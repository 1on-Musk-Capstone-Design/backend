const mediasoup = require("mediasoup");

class RoomManager {
  constructor(config) {
    this.config = config;
    this.worker = null;
    this.rooms = new Map();
  }

  async init() {
    if (this.worker) {
      return this.worker;
    }

    this.worker = await mediasoup.createWorker({
      logLevel: this.config.mediasoup.logLevel,
      rtcMinPort: this.config.mediasoup.worker.rtcMinPort,
      rtcMaxPort: this.config.mediasoup.worker.rtcMaxPort,
    });

    this.worker.on("died", () => {
      console.error("mediasoup worker died unexpectedly");
      process.exit(1);
    });

    return this.worker;
  }

  roomKey(workspaceId, sessionId) {
    return `${workspaceId}:${sessionId}`;
  }

  async getRoom(workspaceId, sessionId) {
    await this.init();

    const key = this.roomKey(workspaceId, sessionId);
    let room = this.rooms.get(key);

    if (!room) {
      room = {
        workspaceId,
        sessionId,
        router: await this.worker.createRouter({
          mediaCodecs: this.config.mediasoup.routerMediaCodecs,
        }),
        peers: new Map(),
      };

      this.rooms.set(key, room);
    }

    return room;
  }

  async getRouterRtpCapabilities(workspaceId, sessionId) {
    const room = await this.getRoom(workspaceId, sessionId);
    return room.router.rtpCapabilities;
  }

  async createWebRtcTransport(workspaceId, sessionId, peerId) {
    const room = await this.getRoom(workspaceId, sessionId);
    const transport = await room.router.createWebRtcTransport({
      listenIps: [{
        ip: this.config.mediasoup.webRtcTransport.listenIp,
        announcedIp: this.config.mediasoup.webRtcTransport.announcedIp,
      }],
      enableUdp: true,
      enableTcp: true,
      preferUdp: true,
      initialAvailableOutgoingBitrate: 1_000_000,
    });

    const peer = this.ensurePeer(room, peerId);
    peer.transports.set(transport.id, transport);

    return {
      id: transport.id,
      iceParameters: transport.iceParameters,
      iceCandidates: transport.iceCandidates,
      dtlsParameters: transport.dtlsParameters,
      sctpParameters: transport.sctpParameters,
    };
  }

  async connectTransport(workspaceId, sessionId, peerId, transportId, dtlsParameters) {
    const room = await this.getRoom(workspaceId, sessionId);
    const peer = this.ensurePeer(room, peerId);
    const transport = peer.transports.get(transportId);

    if (!transport) {
      throw new Error(`Transport not found: ${transportId}`);
    }

    await transport.connect({ dtlsParameters });

    return { connected: true };
  }

  async produce(workspaceId, sessionId, peerId, transportId, kind, rtpParameters, appData = {}) {
    const room = await this.getRoom(workspaceId, sessionId);
    const peer = this.ensurePeer(room, peerId);
    const transport = peer.transports.get(transportId);

    if (!transport) {
      throw new Error(`Transport not found: ${transportId}`);
    }

    // Voice chat only needs one active producer per media kind per peer.
    // Repeated Start Call clicks or refresh/reconnect flows must not leave
    // stale audio producers in the room.
    this.closePeerProducersByKind(room, peer, kind);

    const producer = await transport.produce({ kind, rtpParameters, appData });
    peer.producers.set(producer.id, producer);

    return {
      id: producer.id,
      producerId: producer.id,
      peerId,
      kind: producer.kind,
      rtpParameters: producer.rtpParameters,
    };
  }

  async listProducers(workspaceId, sessionId) {
    const room = await this.getRoom(workspaceId, sessionId);
    const producers = [];

    for (const peer of room.peers.values()) {
      for (const producer of peer.producers.values()) {
        producers.push({
          producerId: producer.id,
          peerId: peer.peerId,
          kind: producer.kind,
          paused: producer.paused,
        });
      }
    }

    return { producers };
  }

  async consume(workspaceId, sessionId, peerId, transportId, producerId, rtpCapabilities) {
    const room = await this.getRoom(workspaceId, sessionId);
    const peer = this.ensurePeer(room, peerId);
    const transport = peer.transports.get(transportId);

    if (!transport) {
      throw new Error(`Transport not found: ${transportId}`);
    }

    const producer = this.findProducer(room, producerId);

    if (!producer) {
      throw new Error(`Producer not found: ${producerId}`);
    }

    if (!room.router.canConsume({ producerId, rtpCapabilities })) {
      throw new Error(`Cannot consume producer: ${producerId}`);
    }

    const consumer = await transport.consume({
      producerId,
      rtpCapabilities,
      paused: true,
    });

    peer.consumers.set(consumer.id, consumer);

    return {
      consumerId: consumer.id,
      producerId: consumer.producerId,
      kind: consumer.kind,
      rtpParameters: consumer.rtpParameters,
      type: consumer.type,
      producerPaused: consumer.producerPaused,
    };
  }

  async resumeConsumer(workspaceId, sessionId, peerId, consumerId) {
    const room = await this.getRoom(workspaceId, sessionId);
    const peer = this.ensurePeer(room, peerId);
    const consumer = peer.consumers.get(consumerId);

    if (!consumer) {
      throw new Error(`Consumer not found: ${consumerId}`);
    }

    await consumer.resume();

    return { resumed: true, consumerId: consumer.id };
  }

  closePeer(workspaceId, sessionId, peerId) {
    const key = this.roomKey(workspaceId, sessionId);
    const room = this.rooms.get(key);

    if (!room) {
      return { closed: false, reason: "room_not_found", roomClosed: false };
    }

    const peer = room.peers.get(peerId);

    if (!peer) {
      return { closed: false, reason: "peer_not_found", roomClosed: false };
    }

    const closed = this.closePeerResources(room, peer);
    room.peers.delete(peerId);

    const roomClosed = this.closeRoomIfEmpty(key, room);

    return {
      closed: true,
      peerId,
      roomClosed,
      ...closed,
    };
  }

  closeRoom(workspaceId, sessionId) {
    const key = this.roomKey(workspaceId, sessionId);
    const room = this.rooms.get(key);

    if (!room) {
      return { closed: false, reason: "room_not_found" };
    }

    let transportsClosed = 0;
    let producersClosed = 0;
    let consumersClosed = 0;

    for (const peer of room.peers.values()) {
      const closed = this.closePeerResources(room, peer);
      transportsClosed += closed.transportsClosed;
      producersClosed += closed.producersClosed;
      consumersClosed += closed.consumersClosed;
    }

    room.peers.clear();
    this.safeClose(room.router);
    this.rooms.delete(key);

    return {
      closed: true,
      roomClosed: true,
      transportsClosed,
      producersClosed,
      consumersClosed,
    };
  }

  ensurePeer(room, peerId) {
    let peer = room.peers.get(peerId);

    if (!peer) {
      peer = {
        peerId,
        transports: new Map(),
        producers: new Map(),
        consumers: new Map(),
      };
      room.peers.set(peerId, peer);
    }

    return peer;
  }

  findProducer(room, producerId) {
    for (const peer of room.peers.values()) {
      const producer = peer.producers.get(producerId);
      if (producer) {
        return producer;
      }
    }
    return null;
  }

  findConsumer(room, consumerId) {
    for (const peer of room.peers.values()) {
      const consumer = peer.consumers.get(consumerId);
      if (consumer) {
        return consumer;
      }
    }
    return null;
  }

  closeProducerConsumers(room, producerId) {
    let consumersClosed = 0;

    for (const peer of room.peers.values()) {
      for (const [consumerId, consumer] of peer.consumers.entries()) {
        if (consumer.producerId === producerId) {
          this.safeClose(consumer);
          peer.consumers.delete(consumerId);
          consumersClosed += 1;
        }
      }
    }

    return consumersClosed;
  }

  closePeerProducersByKind(room, peer, kind) {
    let producersClosed = 0;
    let consumersClosed = 0;

    for (const [producerId, producer] of peer.producers.entries()) {
      if (producer.kind !== kind) {
        continue;
      }

      consumersClosed += this.closeProducerConsumers(room, producerId);
      this.safeClose(producer);
      peer.producers.delete(producerId);
      producersClosed += 1;
    }

    return { producersClosed, consumersClosed };
  }

  closePeerResources(room, peer) {
    const producerIds = new Set(peer.producers.keys());
    let transportsClosed = 0;
    let producersClosed = 0;
    let consumersClosed = 0;

    for (const otherPeer of room.peers.values()) {
      for (const [consumerId, consumer] of otherPeer.consumers.entries()) {
        if (producerIds.has(consumer.producerId)) {
          this.safeClose(consumer);
          otherPeer.consumers.delete(consumerId);
          consumersClosed += 1;
        }
      }
    }

    for (const consumer of peer.consumers.values()) {
      this.safeClose(consumer);
      consumersClosed += 1;
    }
    peer.consumers.clear();

    for (const producer of peer.producers.values()) {
      this.safeClose(producer);
      producersClosed += 1;
    }
    peer.producers.clear();

    for (const transport of peer.transports.values()) {
      this.safeClose(transport);
      transportsClosed += 1;
    }
    peer.transports.clear();

    return { transportsClosed, producersClosed, consumersClosed };
  }

  closeRoomIfEmpty(key, room) {
    if (room.peers.size > 0) {
      return false;
    }

    this.safeClose(room.router);
    this.rooms.delete(key);
    return true;
  }

  safeClose(resource) {
    if (!resource || resource.closed) {
      return;
    }

    try {
      resource.close();
    } catch (error) {
      console.warn("Failed to close mediasoup resource", error);
    }
  }
}

module.exports = RoomManager;

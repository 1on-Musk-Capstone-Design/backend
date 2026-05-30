const cors = require("cors");
const express = require("express");
const RoomManager = require("./roomManager");
const config = require("./config");

async function main() {
  const roomManager = new RoomManager(config);
  await roomManager.init();

  const app = express();
  app.use(cors());
  app.use(express.json({ limit: "1mb" }));

  app.get("/health", (_request, response) => {
    response.json({ status: "ok", service: "capstone-sfu-server" });
  });

  app.get("/rooms/:workspaceId/sessions/:sessionId/router-capabilities", async (request, response, next) => {
    try {
      const capabilities = await roomManager.getRouterRtpCapabilities(
        request.params.workspaceId,
        request.params.sessionId,
      );
      response.json({ routerRtpCapabilities: capabilities });
    } catch (error) {
      next(error);
    }
  });

  app.post("/rooms/:workspaceId/sessions/:sessionId/transports", async (request, response, next) => {
    try {
      const { peerId } = request.body;
      const transport = await roomManager.createWebRtcTransport(
        request.params.workspaceId,
        request.params.sessionId,
        peerId,
      );
      response.status(201).json(transport);
    } catch (error) {
      next(error);
    }
  });

  app.post(
    "/rooms/:workspaceId/sessions/:sessionId/transports/:transportId/connect",
    async (request, response, next) => {
      try {
        const { peerId, dtlsParameters } = request.body;
        const result = await roomManager.connectTransport(
          request.params.workspaceId,
          request.params.sessionId,
          peerId,
          request.params.transportId,
          dtlsParameters,
        );
        response.json(result);
      } catch (error) {
        next(error);
      }
    },
  );

  app.post("/rooms/:workspaceId/sessions/:sessionId/producers", async (request, response, next) => {
    try {
      const { peerId, transportId, kind, rtpParameters, appData } = request.body;
      const result = await roomManager.produce(
        request.params.workspaceId,
        request.params.sessionId,
        peerId,
        transportId,
        kind,
        rtpParameters,
        appData,
      );
      response.status(201).json(result);
    } catch (error) {
      next(error);
    }
  });

  app.get("/rooms/:workspaceId/sessions/:sessionId/producers", async (request, response, next) => {
    try {
      const result = await roomManager.listProducers(
        request.params.workspaceId,
        request.params.sessionId,
      );
      response.json(result);
    } catch (error) {
      next(error);
    }
  });

  app.post("/rooms/:workspaceId/sessions/:sessionId/consumers", async (request, response, next) => {
    try {
      const { peerId, transportId, producerId, rtpCapabilities } = request.body;
      const result = await roomManager.consume(
        request.params.workspaceId,
        request.params.sessionId,
        peerId,
        transportId,
        producerId,
        rtpCapabilities,
      );
      response.status(201).json(result);
    } catch (error) {
      next(error);
    }
  });

  app.post(
    "/rooms/:workspaceId/sessions/:sessionId/consumers/:consumerId/resume",
    async (request, response, next) => {
      try {
        const { peerId } = request.body;
        const result = await roomManager.resumeConsumer(
          request.params.workspaceId,
          request.params.sessionId,
          peerId,
          request.params.consumerId,
        );
        response.json(result);
      } catch (error) {
        next(error);
      }
    },
  );

  app.delete("/rooms/:workspaceId/sessions/:sessionId/peers/:peerId", (request, response, next) => {
    try {
      const result = roomManager.closePeer(
        request.params.workspaceId,
        request.params.sessionId,
        request.params.peerId,
      );
      response.json(result);
    } catch (error) {
      next(error);
    }
  });

  app.delete("/rooms/:workspaceId/sessions/:sessionId", (request, response, next) => {
    try {
      const result = roomManager.closeRoom(
        request.params.workspaceId,
        request.params.sessionId,
      );
      response.json(result);
    } catch (error) {
      next(error);
    }
  });

  app.use((error, _request, response, _next) => {
    const status = error.message && error.message.includes("not found") ? 404 : 500;
    response.status(status).json({
      message: error.message || "Unknown SFU error",
    });
  });

  app.listen(config.http.port, config.http.host, () => {
    console.log(`SFU server listening on http://${config.http.host}:${config.http.port}`);
  });
}

main().catch((error) => {
  console.error("Failed to start SFU server", error);
  process.exit(1);
});

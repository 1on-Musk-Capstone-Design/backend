# Mediasoup SFU Server

This folder contains a standalone mediasoup SFU skeleton for the voice chat transition.

## What it provides

- Worker bootstrapping
- Room and peer state management
- Router capability endpoint
- Transport creation, connect, produce, consume endpoints
- Basic health endpoint

## Environment

Create a `.env` file from `.env.example` and adjust ports as needed.

For LAN testing, set `SFU_ANNOUNCED_IP` to the machine's LAN IP.
For external-network testing, set it to the public IP or the address exposed behind your reverse proxy or load balancer.

If you use coturn, keep the frontend TURN values in `frontend/.env` or `frontend/.env.example` in sync with the TURN host, username, and credential.

## API shape

- `GET /health`
- `GET /rooms/:workspaceId/sessions/:sessionId/router-capabilities`
- `POST /rooms/:workspaceId/sessions/:sessionId/transports`
- `POST /rooms/:workspaceId/sessions/:sessionId/transports/:transportId/connect`
- `POST /rooms/:workspaceId/sessions/:sessionId/producers`
- `POST /rooms/:workspaceId/sessions/:sessionId/consumers`

## Run

```bash
npm install
npm run dev
```

The Java backend can point to this server with the `app.webrtc.sfu-server.*` settings.

## coturn setup

Use coturn when peers may be on different networks, behind strict NATs, or on mobile data.

Minimum checklist:

1. Install coturn on a reachable server or VPS.
2. Copy [turnserver.conf.example](turnserver.conf.example) to `turnserver.conf`.
3. Replace the TURN hostname, credentials, and public IP mapping.
4. Open the TURN ports on the server and firewall.
5. Set the frontend TURN env values to the coturn endpoint.
6. Keep `SFU_ANNOUNCED_IP` pointing to the address clients can actually reach.

Typical ports:

- `3478/tcp` and `3478/udp` for TURN
- `5349/tcp` for TURN over TLS if you enable TLS
- A UDP relay port range for media relay, matching your coturn config

For local first-pass testing, you can still use a public TURN service, but the production setup should point to your own coturn instance.

Example startup:

```bash
turnserver -c ./turnserver.conf
```
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
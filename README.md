# Capstone MSA Backend

Backend-only MSA workspace split from the original `Capstone` Spring Boot backend. The frontend is intentionally excluded, and the original project folder is not modified.

## Services

| Service | Port | Responsibility |
| --- | ---: | --- |
| api-gateway | 8080 | Public entrypoint, CORS, `/api/**` routing |
| auth-service | 8081 | user, Google/Apple OAuth, JWT |
| workspace-service | 8082 | workspace, workspace_user, invite, invitation |
| canvas-service | 8083 | canvas, idea |
| chat-service | 8084 | chat_message, STOMP WebSocket chat |
| voice-service | 8085 | voice_session, voice_session_user |
| prototype-service | 8086 | idea_prototype_job, PRD/UI/React generation, Vercel deployment |
| storage-service | 8087 | uploaded files, thumbnails, artifact file serving |
| postgres | 5432 | shared PostgreSQL database |
| redis | 6379 | cache/session/broker-ready dependency |

The gateway preserves the frontend-facing contract: existing frontend calls to `/api/v1/...` and SockJS/STOMP `/api/ws` can continue to use port `8080`.

## Build

```bash
./gradlew clean build
```

## Run A Service

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/capstone_db \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
./gradlew :services:auth-service:bootRun
```

Use the same pattern for other modules, then run:

```bash
./gradlew :api-gateway:bootRun
```

## Run The Whole MSA Stack

```bash
./scripts/run-msa.sh
```

This builds every backend service jar once, starts Postgres/Redis, and runs all MSA services. The gateway is exposed on `8080` and keeps frontend-facing paths on `/api/**`.

To stop:

```bash
./scripts/stop-msa.sh
```

## Required Environment

Set deployment values through environment variables rather than local config files:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/capstone_db
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>
SPRING_JWT_SECRET=<long-secret>
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
GOOGLE_REDIRECT_URI=https://on-it.kro.kr/auth/callback
OPENAI_API_KEY=<openai-api-key>
APP_FILE_UPLOAD_DIR=/var/lib/capstone/uploads
APP_PROTOTYPE_ARTIFACT_DIR=/var/lib/capstone/prototype-artifacts
APP_FILE_BASE_URL=https://on-it.kro.kr/api
```

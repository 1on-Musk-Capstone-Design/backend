# Capstone Project - 실시간 협업 브레인스토밍 플랫폼 MSA Backend

## 프로젝트 개요

실시간 협업 브레인스토밍을 위한 웹 애플리케이션의 백엔드 MSA 프로젝트입니다. 원본 모놀리식 Spring Boot 백엔드를 프론트엔드 수정 없이 사용할 수 있도록 API Gateway 중심의 마이크로서비스 구조로 분리했습니다.

프론트엔드는 기존과 동일하게 `http://localhost:8080/api` 또는 운영 도메인의 `/api`만 호출하면 됩니다. 내부 서비스 분기는 `api-gateway`가 담당합니다.

## 주요 기능

### 1. 채팅 기능
- 워크스페이스 내 실시간 텍스트 채팅
- 사용자/시간 기반 기록 관리
- STOMP WebSocket 기반 실시간 메시지 전송

### 2. 무한 캔버스
- 아이디어 박스를 자유롭게 생성, 수정, 이동, 삭제
- Figma 스타일의 캔버스 기반 협업
- 팀원 간 아이디어/캔버스 변경 실시간 동기화

### 3. 음성 채팅
- 워크스페이스 내 음성 세션 관리
- 참여자 입장, 퇴장, 이동 기록
- 세션별 활성 사용자 조회

### 4. AI/프로토타입 생성
- 아이디어 기반 PRD 생성
- UI 설계안 생성
- React 프로토타입 생성
- Vercel 배포 연동

### 5. 워크스페이스 관리
- 워크스페이스 생성, 조회, 수정, 삭제
- 멤버 참여/퇴장/제거
- 초대 링크 생성 및 수락

### 6. 파일/스토리지
- 업로드 파일 제공
- 썸네일 파일 관리
- 프로토타입 artifact 파일 관리

## 팀 소개

### 백엔드 개발자

<table>
<tr>
<td align="center">
<img src="https://github.com/qlsl1198.png" width="100" height="100" alt="김수빈">
<br>
<a href="https://github.com/qlsl1198"><strong>김수빈</strong></a>
<br>
<small>백엔드 개발자</small>
</td>
<td align="center">
<img src="https://github.com/jyhyo02.png" width="100" height="100" alt="정양효">
<br>
<a href="https://github.com/jyhyo02"><strong>정양효</strong></a>
<br>
<small>백엔드 개발자</small>
</td>
<td align="center">
<img src="https://github.com/Hyun-jun-Lee0811.png" width="100" height="100" alt="이현준">
<br>
<a href="https://github.com/Hyun-jun-Lee0811"><strong>이현준</strong></a>
<br>
<small>백엔드 개발자</small>
</td>
</tr>
</table>

## 기술 스택

### Backend
- Java 21
- Spring Boot 3.2.0
- Spring Cloud Gateway
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- STOMP WebSocket
- OpenAI API
- Swagger/OpenAPI
- Docker / Docker Compose
- GitHub Actions

### Frontend
- React CSR
- STOMP Client
- SockJS

> 이 저장소에는 프론트엔드 코드가 포함되지 않습니다. 원본 프론트엔드는 기존 API 주소를 그대로 사용할 수 있도록 Gateway 라우팅과 CORS를 유지합니다.

## MSA 구성

| Service | Port | 담당 도메인 |
| --- | ---: | --- |
| api-gateway | 8080 | 외부 진입점, CORS, `/api/**` 라우팅 |
| auth-service | 8081 | user, Google OAuth, Apple OAuth, JWT, 로컬 개발자 로그인 |
| workspace-service | 8082 | workspace, workspace_user, invite, invitation |
| canvas-service | 8083 | canvas, idea |
| chat-service | 8084 | chat_message, STOMP WebSocket chat |
| voice-service | 8085 | voice_session, voice_session_user |
| prototype-service | 8086 | idea_prototype_job, PRD/UI/React 생성, Vercel 배포 |
| storage-service | 8087 | thumbnail, upload file, artifact file |
| postgres | 5432 | 공유 PostgreSQL 데이터베이스 |
| redis | 6379 | Redis 의존 서비스 |

```text
EC2 or Local Docker
├─ api-gateway        : 8080
├─ auth-service       : 8081
├─ workspace-service  : 8082
├─ canvas-service     : 8083
├─ chat-service       : 8084
├─ voice-service      : 8085
├─ prototype-service  : 8086
├─ storage-service    : 8087
├─ postgres           : 5432
└─ redis              : 6379
```

## API Gateway 라우팅

프론트엔드 요청은 모두 `api-gateway:8080`으로 들어오고, Gateway가 내부 서비스로 전달합니다.

| Frontend Path | Target Service |
| --- | --- |
| `/api/v1/health` | auth-service |
| `/api/v1/auth-google/**` | auth-service |
| `/api/v1/auth-apple/**` | auth-service |
| `/api/v1/auth/dev/**` | auth-service |
| `/api/v1/users/**` | auth-service |
| `/api/v1/workspaces/**` | workspace-service |
| `/api/v1/ideas/**` | canvas-service |
| `/api/v1/canvas/**` | canvas-service |
| `/api/v1/*/canvas` | canvas-service |
| `/api/v1/chat/**` | chat-service |
| `/api/ws/**` | chat-service |
| `/api/v1/workspaces/*/voice/**` | voice-service |
| `/api/v1/ideas/*/prototype/**` | prototype-service |
| `/api/v1/workspaces/*/prds/**` | prototype-service |
| `/api/v1/openai/**` | prototype-service |
| `/api/uploads/**` | storage-service |
| `/uploads/**` | storage-service |

## 설치 및 실행

### 사전 요구사항
- Java 21
- Docker Desktop 또는 Docker Engine
- Docker Compose plugin
- Git

### 1. 저장소 클론

```bash
git clone https://github.com/qlsl1198/capstone_msa.git
cd capstone_msa
```

### 2. 전체 MSA 실행

```bash
./scripts/run-msa.sh
```

이 스크립트는 전체 모듈 JAR를 빌드한 뒤 Docker Compose로 PostgreSQL, Redis, Gateway, 7개 서비스를 실행합니다.

로컬 개발 환경에서는 기본적으로 `APP_DEV_BOOTSTRAP_AUTH=true`가 적용되어 기존 프론트 로그인 흐름에서 로컬 개발자 JWT를 받을 수 있습니다.

### 3. 전체 MSA 종료

```bash
./scripts/stop-msa.sh
```

### 4. Docker Compose 직접 실행

```bash
./gradlew clean assemble -x test
docker compose up -d --build
```

종료:

```bash
docker compose down
```

### 5. 단일 서비스 실행

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/capstone_db \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=musk12! \
./gradlew :services:auth-service:bootRun
```

다른 서비스도 같은 방식으로 실행할 수 있습니다.

```bash
./gradlew :api-gateway:bootRun
./gradlew :services:workspace-service:bootRun
./gradlew :services:canvas-service:bootRun
./gradlew :services:chat-service:bootRun
./gradlew :services:voice-service:bootRun
./gradlew :services:prototype-service:bootRun
./gradlew :services:storage-service:bootRun
```

## 접속 정보

- REST API: `http://localhost:8080/api`
- WebSocket STOMP: `ws://localhost:8080/api/ws`
- Health Check: `http://localhost:8080/api/v1/health`
- Gateway Actuator: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/api/v3/api-docs`

## 주요 환경변수

배포 환경에서는 `.env` 또는 GitHub Actions Secrets로 값을 주입합니다.

```bash
POSTGRES_DB=capstone_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<password>

SPRING_JWT_SECRET=<long-secret>

GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
GOOGLE_REDIRECT_URI=https://on-it.kro.kr/auth/callback
GOOGLE_LOGIN_URI=https://accounts.google.com/o/oauth2/v2/auth
GOOGLE_IOS_CLIENT_ID=<google-ios-client-id>
GOOGLE_IOS_CLIENT_SECRET=<google-ios-client-secret>
GOOGLE_IOS_REDIRECT_URI=<google-ios-redirect-uri>

OPENAI_API_KEY=<openai-api-key>
OPENAI_PROTOTYPE_MODEL=gpt-4o-mini
OPENAI_PROTOTYPE_PRD_MAX_TOKENS=8192
OPENAI_PROTOTYPE_PRD_MIN_CHARS=4500
OPENAI_PROTOTYPE_PRD_BRIEF_ENABLED=true
OPENAI_PROTOTYPE_PRD_BRIEF_MIN_CONTENT=180
OPENAI_PROTOTYPE_PRD_EDITOR_PASS=true

WORKSPACE_INVITE_BASE_URL=https://on-it.kro.kr/invite
WORKSPACE_INVITE_EXPIRE_HOURS=168

APP_ALLOWED_ORIGINS_0=http://localhost:*
APP_ALLOWED_ORIGINS_1=http://127.0.0.1:*
APP_ALLOWED_ORIGINS_3=https://on-it.kro.kr
APP_DEFAULT_REDIRECT_URI=http://localhost:3000/auth/callback
APP_OAUTH_REDIRECT_LOCALHOST=http://localhost:3000/auth/callback
APP_OAUTH_REDIRECT_LOOPBACK=http://localhost:3000/auth/callback
APP_OAUTH_REDIRECT_ONIT=https://on-it.kro.kr/auth/callback
APP_PRD_PUBLIC_BASE_URL=https://on-it.kro.kr
APP_LOCAL_DEV_PERMIT_ALL_WORKSPACE_ACCESS=false

APP_FILE_UPLOAD_DIR=/var/lib/capstone/uploads
APP_FILE_THUMBNAIL_DIR=/var/lib/capstone/uploads/thumbnails
APP_FILE_BASE_URL=https://on-it.kro.kr/api

APP_DEV_BOOTSTRAP_AUTH=false

APP_PROTOTYPE_ARTIFACT_DIR=/var/lib/capstone/prototype-artifacts
APP_PROTOTYPE_VERCEL_TOKEN=<vercel-token>
APP_PROTOTYPE_VERCEL_TEAM_ID=<vercel-team-id>
APP_PROTOTYPE_SIMULATED_BASE_URL=https://prototype.example.com

SOCKETIO_HOST=0.0.0.0
SOCKETIO_PORT=9092
SOCKETIO_ALLOW_ORIGIN=*

APPLE_BUNDLE_ID=<apple-bundle-id>
APPLE_PUBLIC_KEY_URL=https://appleid.apple.com/auth/keys
```

## 로컬 개발자 로그인

로컬 개발에서는 프론트엔드 수정 없이 개발자 계정으로 로그인할 수 있도록 dev bootstrap 경로를 제공합니다.

- `APP_DEV_BOOTSTRAP_AUTH=true`일 때 활성화
- 로컬 실행 스크립트 `./scripts/run-msa.sh`는 기본값을 `true`로 설정
- Docker Compose 기본값은 운영 안전성을 위해 `false`

주요 경로:

```text
GET  /api/v1/auth-google/login-uri
POST /api/v1/auth-google?code=local-dev
POST /api/v1/auth/dev/bootstrap
```

운영 배포에서는 반드시 `APP_DEV_BOOTSTRAP_AUTH=false`로 설정해야 합니다.

## API 문서

### Swagger UI

서버 실행 후 브라우저에서 접속합니다.

```text
http://localhost:8080/api/swagger-ui.html
```

Swagger에서 JWT 테스트가 필요한 경우 우측 상단 `Authorize`에 Access Token을 입력합니다.

### 주요 API

#### Health Check
- `GET /api/v1/health`

#### Auth API
- `GET /api/v1/auth-google/login-uri?redirect_uri={uri}`
- `POST /api/v1/auth-google?code={code}&redirect_uri={uri}`
- `POST /api/v1/auth-apple`
- `POST /api/v1/auth/dev/bootstrap`
- `GET /api/v1/users/me`

#### Workspace API
- `GET /api/v1/workspaces`
- `POST /api/v1/workspaces`
- `GET /api/v1/workspaces/{id}`
- `PUT /api/v1/workspaces/{id}`
- `DELETE /api/v1/workspaces/{id}`
- `POST /api/v1/workspaces/{id}/invite-link`
- `POST /api/v1/workspaces/invite/{token}/accept`

#### Workspace Member API
- `POST /api/v1/workspaces/{workspaceId}/join`
- `GET /api/v1/workspaces/{workspaceId}/users`
- `DELETE /api/v1/workspaces/{workspaceId}/users/{userId}`
- `POST /api/v1/workspaces/{workspaceId}/leave`

#### Idea API
- `POST /api/v1/ideas`
- `GET /api/v1/ideas/workspaces/{workspaceId}`
- `GET /api/v1/ideas/{id}`
- `PUT /api/v1/ideas/{id}`
- `DELETE /api/v1/ideas/{id}`

#### Canvas API
- `POST /api/v1/{workspaceId}/canvas`
- `GET /api/v1/{workspaceId}/canvas`
- `GET /api/v1/canvas/{canvasId}`
- `PUT /api/v1/canvas/{canvasId}`
- `DELETE /api/v1/canvas/{canvasId}`

#### Chat Message API
- `POST /api/v1/chat/messages`
- `POST /api/v1/chat/messages/file`
- `GET /api/v1/chat/messages/workspace/{workspaceId}`
- `GET /api/v1/chat/messages/workspace/{workspaceId}/recent`
- `GET /api/v1/chat/messages/user/{userId}`
- `GET /api/v1/chat/messages/workspace/{workspaceId}/count`

#### Voice Session API
- `POST /api/v1/workspaces/{workspaceId}/voice`
- `GET /api/v1/workspaces/{workspaceId}/voice`
- `PATCH /api/v1/workspaces/{workspaceId}/voice/{sessionId}`

#### Voice Session User API
- `POST /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users`
- `DELETE /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users/{userId}`
- `POST /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users/move`
- `GET /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users`
- `GET /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users/all`
- `GET /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users/count`

#### Prototype/PRD API
- `POST /api/v1/ideas/{ideaId}/prototype`
- `GET /api/v1/ideas/{ideaId}/prototype/latest`
- `GET /api/v1/ideas/{ideaId}/prototype/{jobId}`
- `GET /api/v1/workspaces/{workspaceId}/prds`
- `GET /api/v1/workspaces/{workspaceId}/prds/{prdId}`

#### Storage API
- `GET /api/uploads/**`
- `GET /uploads/**`

## STOMP WebSocket 통신

### 연결

- Endpoint: `ws://localhost:8080/api/ws`
- Protocol: STOMP over SockJS
- Gateway route: `/api/ws/**` -> `chat-service`

### 클라이언트 -> 서버

- `/app/chat/message` - 채팅 메시지 전송
- `/app/chat/file` - 파일/이미지 메시지 전송
- `/app/workspace/join` - 워크스페이스 참여 알림
- `/app/workspace/leave` - 워크스페이스 나가기 알림
- `/app/idea/update` - 아이디어 업데이트
- `/app/voice/join` - 음성 채팅 참여
- `/app/voice/leave` - 음성 채팅 나가기

### 서버 -> 클라이언트

- `/topic/workspace/{workspaceId}/messages` - 채팅 메시지 수신
- `/topic/workspace/{workspaceId}/users` - 워크스페이스 사용자 변경 알림
- `/topic/workspace/{workspaceId}/ideas` - 아이디어 업데이트 알림
- `/topic/workspace/{workspaceId}/canvas` - 캔버스 변경 알림
- `/topic/workspace/{workspaceId}/voice` - 음성 채팅 알림
- `/topic/workspace/{workspaceId}/workspace` - 워크스페이스 변경 알림
- `/topic/workspace/{workspaceId}/prototype` - 프로토타입 생성 상태 알림

### 프론트엔드 연결 예제

```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/api/ws'),
  reconnectDelay: 5000,
});

client.activate();

client.onConnect = () => {
  client.subscribe('/topic/workspace/1/messages', (message) => {
    const data = JSON.parse(message.body);
    console.log('메시지 수신:', data);
  });

  client.publish({
    destination: '/app/chat/message',
    body: JSON.stringify({
      workspaceId: 1,
      userId: 1,
      content: '안녕하세요'
    })
  });
};
```

## 프로젝트 구조

```text
capstone_msa/
├── api-gateway/
│   └── src/main/resources/application.yml
├── services/
│   ├── auth-service/
│   ├── workspace-service/
│   ├── canvas-service/
│   ├── chat-service/
│   ├── voice-service/
│   ├── prototype-service/
│   └── storage-service/
├── scripts/
│   ├── run-msa.sh
│   └── stop-msa.sh
├── docs/
│   ├── msa-demo-notes.md
│   └── msa-demo-slide-outline.md
├── .github/workflows/
│   ├── ci-cd.yml
│   └── README.md
├── docker-compose.yml
├── Dockerfile
├── settings.gradle
└── build.gradle
```

### 공통 패키지 구조

각 서비스는 원본 백엔드 코드를 기준으로 필요한 Controller/Service/Domain만 노출하도록 분리되어 있습니다.

```text
com.capstone
├── domain
│   ├── chat
│   ├── idea
│   ├── prototype
│   ├── user
│   ├── voicesession
│   ├── workspace
│   └── workspaceUser
└── global
    ├── config
    ├── controller
    ├── oauth
    ├── service
    └── type
```

## MSA 분리 방식

### 1. Gateway 중심 라우팅

외부 API 계약은 유지하고 내부 서비스만 분리했습니다. 기존 프론트는 계속 `/api/v1/...`를 호출하고, Gateway가 도메인별 서비스로 요청을 전달합니다.

### 2. 서비스별 Controller 노출 제한

각 서비스는 `@ComponentScan` 범위를 제어하여 자기 담당 도메인의 Controller만 노출합니다. 예를 들어 `canvas-service`는 canvas/idea API를 담당하고, `auth-service`는 auth/user API를 담당합니다.

### 3. 공유 DB 기반 단계적 분리

현재는 데모와 기존 동작 보존을 위해 하나의 PostgreSQL을 공유합니다. 서비스별 도메인 책임은 분리되어 있고, 향후 필요하면 DB schema 또는 DB instance 단위로 추가 분리할 수 있습니다.

### 4. 프론트엔드 호환성 유지

CORS, context path, WebSocket endpoint, API path를 기존과 동일하게 유지했습니다. 프론트엔드에서 별도 코드 수정 없이 MSA Gateway만 바라보면 됩니다.

### 5. 배포 단위 분리

각 서비스는 개별 Gradle module/JAR로 빌드되고, Docker Compose에서 독립 컨테이너로 실행됩니다.

## 개발 상태

### 완료된 기능

- [x] 모놀리식 백엔드 MSA 멀티모듈 분리
- [x] API Gateway 라우팅 구성
- [x] CORS 및 기존 프론트엔드 API 계약 유지
- [x] Auth/User 서비스 분리
- [x] Workspace/Invite 서비스 분리
- [x] Canvas/Idea 서비스 분리
- [x] Chat/WebSocket 서비스 분리
- [x] Voice Session 서비스 분리
- [x] Prototype/PRD/UI/React/Vercel 서비스 분리
- [x] Storage/Upload/Thumbnail/Artifact 서비스 분리
- [x] Docker Compose 전체 실행 구성
- [x] GitHub Actions CI/CD 구성
- [x] 로컬 개발자 로그인 지원
- [x] PRD 생성 결과가 캔버스 아이디어로 노출되지 않도록 수정

### 추후 개선 가능 항목

- [ ] 서비스별 DB 분리
- [ ] 서비스 간 REST Client 또는 이벤트 기반 통신 강화
- [ ] Redis Pub/Sub 기반 WebSocket scale-out
- [ ] 운영용 observability 구성
- [ ] 통합 테스트 확대

## CI/CD

GitHub Actions 워크플로우는 `.github/workflows/ci-cd.yml`에 있습니다.

배포 흐름:

1. 전체 MSA 모듈 빌드
2. 서비스별 JAR 생성
3. Dockerfile, docker-compose.yml, scripts 포함 배포 번들 생성
4. EC2 서버로 번들 업로드
5. GitHub Secrets 기반 `.env` 생성
6. Docker Compose로 전체 MSA 재기동
7. Gateway health check 수행

필요한 GitHub Secrets 목록은 `.github/workflows/README.md`를 참고합니다.

## 보안 설정

- JWT 기반 인증
- Google OAuth 로그인
- Apple 로그인
- 로컬 개발자 로그인은 `APP_DEV_BOOTSTRAP_AUTH=true`에서만 활성화
- 운영 환경에서는 `APP_DEV_BOOTSTRAP_AUTH=false` 필수
- CORS origin은 `APP_ALLOWED_ORIGINS_*` 환경변수로 관리
- OpenAI, Vercel, OAuth secret은 저장소에 커밋하지 않고 환경변수로 주입

## 기여 방법

1. 저장소를 포크합니다.
2. 기능 브랜치를 생성합니다.
3. 변경사항을 커밋합니다.
4. 브랜치에 푸시합니다.
5. Pull Request를 생성합니다.

```bash
git checkout -b feat/your-feature
git commit -m "Add your feature"
git push origin feat/your-feature
```

## 문의

프로젝트에 대한 문의사항은 GitHub Issue로 남겨주세요.

---

Happy Brainstorming!

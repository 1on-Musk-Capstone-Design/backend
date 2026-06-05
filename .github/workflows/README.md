# GitHub Actions CI/CD 설정 가이드

## 개요
이 워크플로우는 Capstone MSA 백엔드를 자동으로 빌드, 테스트, 패키징하고 EC2에 Docker Compose 스택으로 배포합니다.

## 워크플로우 트리거
- `develop`, `main`, `feature/aws-sync` 브랜치에 push 시
- `develop`, `main` 브랜치로 Pull Request 생성 시
- GitHub Actions 수동 실행(`workflow_dispatch`)

## 작업 단계

### 1. Build and Test
- Java 21 설정
- 모든 MSA 모듈 JAR 빌드
- 테스트 실행
- `Dockerfile`, `docker-compose.yml`, `scripts`, 서비스별 JAR을 배포 번들로 저장

### 2. Deploy to AWS
- SSH로 EC2 접속
- `~/capstone-msa`에 배포 번들 업로드
- `.env` 생성
- 기존 Docker Compose 스택 종료
- `docker compose up --build -d`로 MSA 전체 재기동
- `http://<AWS_SERVER_HOST>:8080/api/v1/health` 헬스체크

## GitHub Secrets

필수:
- `AWS_SSH_PRIVATE_KEY`: EC2 접속용 SSH 개인 키
- `AWS_SERVER_HOST`: EC2 public host 또는 IP

권장:
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `SPRING_JWT_SECRET`
- `APP_ALLOWED_ORIGINS_0`
- `APP_ALLOWED_ORIGINS_1`
- `APP_ALLOWED_ORIGINS_3`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI`
- `GOOGLE_LOGIN_URI`
- `GOOGLE_IOS_CLIENT_ID`
- `GOOGLE_IOS_CLIENT_SECRET`
- `GOOGLE_IOS_REDIRECT_URI`
- `WORKSPACE_INVITE_BASE_URL`
- `WORKSPACE_INVITE_EXPIRE_HOURS`
- `APP_FILE_BASE_URL`
- `APP_FILE_UPLOAD_DIR`
- `APP_FILE_THUMBNAIL_DIR`
- `APP_PRD_PUBLIC_BASE_URL`
- `APP_PROTOTYPE_ARTIFACT_DIR`
- `APP_PROTOTYPE_SIMULATED_BASE_URL`
- `OPENAI_API_KEY`
- `OPENAI_PROTOTYPE_MODEL`
- `OPENAI_PROTOTYPE_PRD_MAX_TOKENS`
- `OPENAI_PROTOTYPE_PRD_MIN_CHARS`
- `OPENAI_PROTOTYPE_PRD_BRIEF_ENABLED`
- `OPENAI_PROTOTYPE_PRD_BRIEF_MIN_CONTENT`
- `OPENAI_PROTOTYPE_PRD_EDITOR_PASS`
- `APP_PROTOTYPE_VERCEL_TOKEN`
- `APP_PROTOTYPE_VERCEL_TEAM_ID`
- `APPLE_BUNDLE_ID`
- `APPLE_PUBLIC_KEY_URL`
- `SOCKETIO_HOST`
- `SOCKETIO_PORT`
- `SOCKETIO_ALLOW_ORIGIN`
- `APP_WEBRTC_SFU_ENABLED`
- `APP_WEBRTC_SIGNAL_AUTH_SOFT_VALIDATION_ENABLED`
- `APP_WEBRTC_SIGNAL_TOPIC_PREFIX`
- `APP_WEBRTC_VOICE_SESSION_SEGMENT`
- `APP_WEBRTC_SIGNAL_SEGMENT`
- `APP_WEBRTC_SFU_SERVER_BASE_URL`
- `APP_WEBRTC_SFU_SERVER_HTTP_PORT`
- `APP_WEBRTC_SFU_SERVER_HEALTH_PATH`
- `APP_WEBRTC_SFU_SERVER_ROOMS_PATH`
- `APP_WEBRTC_SFU_SERVER_WS_PATH`
- `APP_WEBRTC_SFU_WORKER_MIN_PORT`
- `APP_WEBRTC_SFU_WORKER_MAX_PORT`
- `APP_WEBRTC_SFU_LOG_LEVEL`
- `SFU_HTTP_HOST`
- `SFU_HTTP_PORT`
- `SFU_LISTEN_IP`
- `SFU_ANNOUNCED_IP`
- `SFU_WORKER_MIN_PORT`
- `SFU_WORKER_MAX_PORT`
- `SFU_LOG_LEVEL`

로컬 개발자 로그인:
- `APP_DEV_BOOTSTRAP_AUTH`: 운영에서는 `false` 권장

## 서버 사전 요구사항
- Docker 및 Docker Compose plugin 설치
- 8080-8087, 5432, 6379 포트 정책 확인
- `ec2-user` SSH 접속 가능

## 배포 결과
EC2에서는 다음 스택으로 실행됩니다:
- `api-gateway`: 8080
- `auth-service`: 8081
- `workspace-service`: 8082
- `canvas-service`: 8083
- `chat-service`: 8084
- `voice-service`: 8085
- `prototype-service`: 8086
- `storage-service`: 8087
- `postgres`: 5432
- `redis`: 6379

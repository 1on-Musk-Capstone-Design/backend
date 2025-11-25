# Capstone Project - 실시간 협업 브레인스토밍 플랫폼

## 📋 프로젝트 개요

실시간 협업 브레인스토밍을 위한 웹 애플리케이션입니다. 팀원들이 동시에 아이디어를 공유하고, AI가 자동으로 아이디어를 클러스터링하여 체계적인 브레인스토밍을 지원합니다.

## 🚀 주요 기능

### 1. 채팅 기능
- 세션 내 실시간 텍스트 채팅
- 사용자/시간 기반 기록 관리
- 팀별, 세션별 채널 구분

### 2. 무한 컨버스 (Figma 스타일)
- 아이디어 박스를 자유롭게 생성/위치 조정
- 단순 텍스트 입력 중심의 직관적 인터페이스
- 팀원들이 동시에 편집 가능

### 3. 음성 채팅
- 세션 내 음성 대화 지원
- 참여자 입퇴장 기록
- 세션별 보이스 채널 관리

### 4. AI 클러스터링
- 아이디어 박스들을 자동으로 분류
- 비슷한 주제끼리 그룹핑
- 클러스터 라벨링 기능

### 5. 세션 관리
- 프로젝트/팀 단위로 세션 생성
- 세션별 컨버스, 채팅, 음성, 아이디어 관리
- 진행 상황 저장 및 재접속 가능

## 👥 팀 소개

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

## 🛠 기술 스택

### Backend
- **Java 21**
- **Spring Boot 3.2.0**
- **PostgreSQL** - 데이터베이스
- **STOMP WebSocket** - 실시간 통신 (Socket.IO에서 마이그레이션)
- **OpenAI API** - AI 클러스터링
- **Spring Security** - 보안
- **Spring Data JPA** - ORM
- **Swagger/OpenAPI** - API 문서화

### Frontend
- **React** (CSR)
- **STOMP Client** - 실시간 통신 (@stomp/stompjs, sockjs-client)

## 📦 설치 및 실행

### 사전 요구사항
- Java 21
- Maven 3.6+
- PostgreSQL 12+

### 1. 데이터베이스 설정

```bash
# PostgreSQL에 데이터베이스 생성
createdb capstone_db

# 또는 psql에서
psql -U postgres
CREATE DATABASE capstone_db;
```

### 2. 환경변수 설정 (선택사항)

```bash
# OpenAI API 키 설정
export OPENAI_API_KEY=your-openai-api-key-here
```

### 3. 애플리케이션 실행

#### Maven
```bash
# 의존성 설치
mvn clean install

# 애플리케이션 실행
mvn spring-boot:run
```

#### Gradle (Wrapper 사용 권장)
```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 애플리케이션 실행
./gradlew bootRun

# (포트가 점유 중일 경우) 8080/9092 포트 정리 후 실행
lsof -ti:8080 | xargs -r kill -9; lsof -ti:9092 | xargs -r kill -9; ./gradlew bootRun
```

### 4. 접속 정보

- **REST API**: `http://localhost:8080/api`
- **WebSocket (STOMP)**: `ws://localhost:8080/api/ws`
- **헬스체크**: `http://localhost:8080/api/v1/health`
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/api/v3/api-docs`

### 5. API 문서

#### Swagger UI
서버 실행 후 브라우저에서 `http://localhost:8080/api/swagger-ui.html` 접속
- 모든 API 목록 및 상세 정보 확인
- 직접 API 테스트 가능
- JWT 토큰 입력 가능 (우측 상단 "Authorize" 버튼)

#### Postman 컬렉션
루트의 `postman_collection.json`을 Postman에 임포트하여 API를 바로 테스트할 수 있습니다.

**총 32개 API 포함:**

#### Health Check (1개)
- `GET /api/v1/health` - 서버 상태 확인

#### Workspace API (7개)
- `GET /api/v1/workspaces` - 워크스페이스 목록 조회
- `POST /api/v1/workspaces` - 워크스페이스 생성
- `GET /api/v1/workspaces/{id}` - 워크스페이스 상세 조회
- `PUT /api/v1/workspaces/{id}` - 워크스페이스 이름 변경
- `DELETE /api/v1/workspaces/{id}` - 워크스페이스 삭제
- `POST /api/v1/workspaces/{id}/invite-link` - 초대 링크 생성
- `POST /api/v1/workspaces/invite/{token}/accept` - 초대 링크 수락

#### Workspace Member API (4개)
- `POST /api/v1/workspaces/{workspaceId}/join` - 워크스페이스 참여
- `GET /api/v1/workspaces/{workspaceId}/users` - 멤버 목록 조회
- `DELETE /api/v1/workspaces/{workspaceId}/users/{userId}` - 멤버 제거
- `POST /api/v1/workspaces/{workspaceId}/leave` - 워크스페이스 나가기

#### Idea API (5개)
- `POST /api/v1/ideas` - 아이디어 생성
- `GET /api/v1/ideas/workspaces/{workspaceId}` - 워크스페이스 아이디어 목록 조회
- `GET /api/v1/ideas/{id}` - 아이디어 상세 조회
- `PUT /api/v1/ideas/{id}` - 아이디어 수정
- `DELETE /api/v1/ideas/{id}` - 아이디어 삭제

#### Chat Message API (6개)
- `POST /api/v1/chat/messages` - 채팅 메시지 전송
- `POST /api/v1/chat/messages/file` - 파일/이미지 메시지 생성
- `GET /api/v1/chat/messages/workspace/{workspaceId}` - 워크스페이스별 메시지 조회
- `GET /api/v1/chat/messages/workspace/{workspaceId}/recent` - 최근 메시지 조회
- `GET /api/v1/chat/messages/user/{userId}` - 사용자별 메시지 조회
- `GET /api/v1/chat/messages/workspace/{workspaceId}/count` - 메시지 개수 조회

#### Voice Session API (3개)
- `POST /api/v1/workspaces/{workspaceId}/voice` - 음성 세션 시작
- `GET /api/v1/workspaces/{workspaceId}/voice` - 음성 세션 목록 조회
- `PATCH /api/v1/workspaces/{workspaceId}/voice/{sessionId}` - 음성 세션 종료

#### Voice Session User API (7개)
- `POST /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users` - 세션 참여
- `DELETE /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users/{userId}` - 세션 퇴장
- `POST /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users/move` - 세션 이동
- `GET /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users` - 활성 사용자 목록 조회
- `GET /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users/all` - 모든 사용자 조회
- `GET /api/v1/workspaces/{workspaceId}/voice/{sessionId}/users/count` - 참여자 수 조회

#### Canvas API (5개)
- `POST /api/v1/{workspaceId}/canvas` - 캔버스 생성
- `GET /api/v1/{workspaceId}/canvas` - 워크스페이스별 캔버스 목록 조회
- `GET /api/v1/canvas/{canvasId}` - 캔버스 상세 조회
- `PUT /api/v1/canvas/{canvasId}` - 캔버스 수정
- `DELETE /api/v1/canvas/{canvasId}` - 캔버스 삭제

#### Google OAuth API (2개)
- `GET /api/v1/auth-google/login-uri?redirect_uri={uri}` - Google 로그인 URI 조회 (redirect_uri 파라미터 지원)
- `POST /api/v1/auth-google?code={code}&redirect_uri={uri}` - Google 로그인/회원가입 (redirect_uri 파라미터 지원)

> **참고:** 개발/테스트 환경에서는 Authorization 헤더가 기본적으로 비활성화되어 있습니다. 
> JWT 토큰을 받은 후 사용하려면 Postman의 Variables 탭에서 `authToken` 변수를 설정하고, 
> 원하는 요청의 Authorization 헤더를 활성화하세요.

## 🔧 설정 파일

### application.yml 주요 설정

```yaml
# 서버 설정
server:
  port: 8080
  servlet:
    context-path: /api  # 모든 API의 기본 경로

# 데이터베이스 설정
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/capstone_db
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: create-drop  # 개발용 (프로덕션에서는 validate로 변경)
    show-sql: true

# WebSocket (STOMP) 설정
# WebSocketConfig에서 관리
# 연결 엔드포인트: /api/ws
# 메시지 브로드캐스트: /topic/workspace/{workspaceId}/*
# 클라이언트 전송: /app/*

# OpenAI API 설정 (AI 클러스터링용)
openai:
  api-key: ${OPENAI_API_KEY:your-openai-api-key-here}
  model: text-embedding-ada-002

# OAuth2 (Google) 설정
oauth2:
  google:
    client-id: ${GOOGLE_CLIENT_ID:dummy}
    client-secret: ${GOOGLE_CLIENT_SECRET:dummy}
    redirect-uri: ${GOOGLE_REDIRECT_URI:http://localhost:3000/auth/callback}
```

### 보안 설정 (SecurityConfig)

현재 개발/테스트를 위해 대부분의 API가 인증 없이 접근 가능하도록 설정되어 있습니다.

**허용된 엔드포인트:**
- Health Check, Actuator
- WebSocket (/api/ws)
- Google OAuth
- Workspace API (GET, POST, PUT, DELETE)
- Chat Message API
- Voice Session API

**프로덕션 배포 시:**
- `SecurityConfig.java`의 TODO 주석 참고
- POST, PUT, DELETE 메서드는 `.authenticated()`로 변경 필요

## 📡 STOMP WebSocket 통신

### 연결
- **엔드포인트**: `ws://localhost:8080/api/ws`
- **프로토콜**: STOMP over SockJS

### 클라이언트 → 서버 (메시지 전송)
- `/app/chat/message` - 채팅 메시지 전송
- `/app/chat/file` - 파일/이미지 메시지 전송
- `/app/workspace/join` - 워크스페이스 참여 알림
- `/app/workspace/leave` - 워크스페이스 나가기 알림
- `/app/idea/update` - 아이디어 업데이트
- `/app/voice/join` - 음성 채팅 참여
- `/app/voice/leave` - 음성 채팅 나가기

### 서버 → 클라이언트 (메시지 구독)
- `/topic/workspace/{workspaceId}/messages` - 채팅 메시지 수신
- `/topic/workspace/{workspaceId}/users` - 워크스페이스 사용자 변경 알림
- `/topic/workspace/{workspaceId}/ideas` - 아이디어 업데이트 알림
- `/topic/workspace/{workspaceId}/canvas` - 캔버스 변경 알림
- `/topic/workspace/{workspaceId}/voice` - 음성 채팅 알림
- `/topic/workspace/{workspaceId}/workspace` - 워크스페이스 변경 알림

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
  // 채팅 메시지 구독
  client.subscribe('/topic/workspace/1/messages', (message) => {
    const data = JSON.parse(message.body);
    console.log('메시지 수신:', data);
  });
  
  // 메시지 전송
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

## 🏗 프로젝트 구조

```
src/main/java/com/capstone/
├── CapstoneApplication.java          # 메인 애플리케이션
├── domain/                           # 도메인별 기능
│   ├── chat/                         # 채팅 도메인
│   │   ├── ChatMessage.java         # 채팅 메시지 엔티티
│   │   ├── ChatMessageController.java
│   │   ├── ChatMessageService.java
│   │   └── ChatMessageRepository.java
│   ├── idea/                         # 아이디어 도메인
│   │   ├── Idea.java                 # 아이디어 엔티티
│   │   ├── IdeaController.java       # 아이디어 컨트롤러
│   │   ├── IdeaRepository.java       # 아이디어 리포지토리
│   │   ├── IdeaRequest.java          # 아이디어 요청 DTO
│   │   ├── IdeaResponse.java         # 아이디어 응답 DTO
│   │   └── IdeaService.java   
│   ├── user/                         # 사용자 도메인
│   │   └── entity/
│   │       └── User.java            # 사용자 엔티티
│   ├── voicesession/                 # 음성 세션 도메인
│   │   ├── VoiceSession.java       # 음성 세션 엔티티
│   │   ├── VoiceSessionController.java
│   │   ├── VoiceSessionService.java
│   │   └── VoiceSessionRepository.java
│   ├── workspace/                    # 워크스페이스 도메인
│   │   ├── Workspace.java           # 워크스페이스 엔티티
│   │   ├── WorkspaceController.java # 워크스페이스 API
│   │   ├── WorkspaceDtos.java       # 워크스페이스 DTO
│   │   ├── WorkspaceRepository.java
│   │   └── WorkspaceService.java
│   └── workspaceUser/                # 워크스페이스 멤버 도메인
│       ├── WorkspaceUser.java       # 멤버 엔티티
│       ├── WorkspaceUserController.java
│       └── WorkspaceUserService.java
└── global/                          # 공통 컴포넌트
    ├── config/                       # 설정 클래스들
    │   ├── SecurityConfig.java      # 보안 설정 (HTTP 메서드별 권한 구분)
    │   ├── WebSocketConfig.java     # STOMP WebSocket 설정
    │   └── AppProperties.java       # 애플리케이션 설정 (CORS, OAuth 등)
    ├── controller/                   # 공통 컨트롤러
    │   ├── HealthController.java    # 헬스체크 API
    │   └── OpenAIController.java    # OpenAI API (비활성화)
    ├── oauth/                        # OAuth 인증
    │   ├── JwtProvider.java         # JWT 토큰 생성/검증
    │   ├── TokenDto.java
    │   ├── controller/
    │   │   └── GoogleController.java # Google OAuth API
    │   └── service/
    │       └── GoogleService.java    # Google OAuth 서비스
    ├── service/                      # 공통 서비스
    │   └── WebSocketService.java    # STOMP WebSocket 메시지 처리
    └── type/
        └── Role.java                # 사용자 권한 (OWNER, MEMBER)
```

## 🚧 개발 상태

### ✅ 완료된 기능
- [x] **기본 설정**
  - [x] STOMP WebSocket 서버 설정 및 메시지 핸들러
  - [x] CORS 설정 및 보안 구성 (HTTP 메서드별 권한 구분)
  - [x] PostgreSQL 연결 설정
  - [x] Context-path 설정 (`/api`)
  - [x] 헬스체크 API
  - [x] 환경변수 기반 동적 설정 (CORS, OAuth redirect URI)

- [x] **인증/인가**
  - [x] Google OAuth 2.0 통합
  - [x] JWT 토큰 생성/검증 (Access Token: 24시간, Refresh Token: 7일)
  - [x] Spring Security 설정 (개발/프로덕션 구분)
  - [x] Authorization 헤더 optional 처리 (개발용)
  - [x] OAuth redirect_uri 파라미터 지원 (프론트엔드 동적 설정 연동)

- [x] **Workspace API (7개)**
  - [x] 워크스페이스 생성/조회/수정/삭제
  - [x] 초대 링크 생성/수락
  - [x] 자동 User 생성 (개발용)
  - [x] 초대 수락 API 상세 로깅 및 예외 처리

- [x] **Workspace Member API (4개)**
  - [x] 워크스페이스 참여
  - [x] 멤버 목록 조회
  - [x] 멤버 제거 (OWNER 권한 또는 본인)
  - [x] 워크스페이스 나가기
  - [x] OWNER 나가기 시 자동 소유권 이전 또는 워크스페이스 삭제

- [x] **Idea API (5개)**
  - [x] 워크스페이스 생성/조회/수정/삭제

- [x] **Chat Message API (6개)**
  - [x] 채팅 메시지 전송
  - [x] 파일/이미지 메시지 생성
  - [x] 워크스페이스별/사용자별 메시지 조회
  - [x] 최근 메시지 조회
  - [x] 메시지 개수 조회

- [x] **Voice Session API (3개)**
  - [x] 음성 세션 시작/종료
  - [x] 세션 목록 조회

- [x] **Canvas API (5개)**
  - [x] 캔버스 생성/조회/수정/삭제
  - [x] 워크스페이스별 캔버스 목록 조회

- [x] **Voice Session User API (7개)**
  - [x] 세션 참여/퇴장
  - [x] 세션 이동
  - [x] 활성 사용자 목록 조회
  - [x] 모든 사용자 조회
  - [x] 참여자 수 조회

- [x] **실시간 통신 (STOMP WebSocket)**
  - [x] 채팅 메시지 실시간 전송/수신
  - [x] 파일/이미지 메시지 실시간 전송/수신
  - [x] 워크스페이스 사용자 변경 알림
  - [x] 아이디어 업데이트 실시간 동기화
  - [x] 캔버스 변경 실시간 동기화
  - [x] 워크스페이스 변경 실시간 알림
  - [x] 음성 채팅 참여/나가기 알림

- [x] **API 문서화**
  - [x] Swagger/OpenAPI 통합
  - [x] 34개 API 전체 문서화
  - [x] JWT 인증 테스트 지원
  
- [x] **Postman 컬렉션**
  - [x] 34개 API 전체 포함
  - [x] Authorization 헤더 설정
  - [x] 샘플 응답 포함

- [x] **CI/CD**
  - [x] GitHub Actions 워크플로우
  - [x] 자동 빌드 및 테스트
  - [x] AWS 서버 자동 배포
  - [x] 수동 워크플로우 실행 지원 (workflow_dispatch)
  - [x] 환경변수 자동 설정

### 🔄 진행 예정
- [ ] AI 클러스터링 서비스 (OpenAI 통합)
- [ ] 프로덕션용 보안 강화
- [ ] 테스트 코드 확대
- [ ] 성능 최적화

## 🤝 기여 방법

1. 이 저장소를 포크합니다
2. 새로운 기능 브랜치를 생성합니다 (`git checkout -b feature/amazing-feature`)
3. 변경사항을 커밋합니다 (`git commit -m 'Add some amazing feature'`)
4. 브랜치에 푸시합니다 (`git push origin feature/amazing-feature`)
5. Pull Request를 생성합니다

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요.

---

**Happy Brainstorming! 🧠💡**

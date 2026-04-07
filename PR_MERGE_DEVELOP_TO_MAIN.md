# [Backend] Develop → Main 브랜치 머지

## PR 제목
```
[Backend] Merge develop into main - 백엔드 안정화 및 기능 완성
```

## 개요
develop 브랜치의 백엔드 변경사항을 main 브랜치로 머지합니다.

## 변경 범위
백엔드 관련 파일만 포함:
- `src/main/java/` - Java 소스 코드
- `src/main/resources/` - 설정 파일
- `src/test/` - 테스트 코드
- `build.gradle`, `settings.gradle` - 빌드 설정
- `gradle/`, `gradlew`, `gradlew.bat` - Gradle 관련 파일

## 주요 변경사항

### 백엔드 API
- REST API 엔드포인트 구현 및 개선
- WebSocket 기반 실시간 통신 기능
- 인증/인가 처리 (JWT, OAuth)

### 도메인 모델
- Canvas (무한 캔버스)
- Chat (실시간 채팅)
- Idea (아이디어 관리)
- Workspace (워크스페이스)
- User (사용자 관리)
- VoiceSession (음성 세션)

### 인프라 및 설정
- Spring Boot 3.2.0 기반 설정
- Security 설정
- Swagger/OpenAPI 문서화
- 데이터베이스 설정

### 테스트
- 단위 테스트 및 통합 테스트
- 컨트롤러 테스트
- 서비스 테스트

## 체크리스트
- [ ] 백엔드 빌드 성공 (`./gradlew clean build`)
- [ ] 모든 테스트 통과
- [ ] API 문서화 확인
- [ ] 코드 리뷰 완료

## 참고사항
- 프론트엔드 관련 변경사항은 별도 PR로 진행 예정
- 데이터베이스 마이그레이션이 필요한 경우 별도 안내

## 관련 이슈
- 백엔드 개발 완료 및 안정화


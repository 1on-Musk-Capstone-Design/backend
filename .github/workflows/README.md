# GitHub Actions CI/CD 설정 가이드

## 개요
이 워크플로우는 Spring Boot 애플리케이션을 자동으로 빌드, 테스트, 배포합니다.

## 워크플로우 트리거
- `develop`, `main`, `feature/aws-sync` 브랜치에 push 시
- `develop`, `main` 브랜치로의 Pull Request 생성 시

## 작업 단계

### 1. Build and Test
- Java 21 설정
- Gradle 빌드 실행
- 테스트 실행
- JAR 파일 아티팩트 저장

### 2. Deploy to AWS
- 빌드 성공 시에만 실행
- SSH를 통해 AWS 서버에 JAR 파일 업로드
- 기존 애플리케이션 중지
- 새 버전 배포 및 재시작
- 헬스체크로 배포 검증

## GitHub Secrets 설정

다음 Secrets를 GitHub 저장소에 설정해야 합니다:

1. **AWS_SSH_PRIVATE_KEY**
   - AWS 서버 접속용 SSH 개인 키
   - `capstone.pem` 파일의 전체 내용을 복사하여 설정
   - Settings → Secrets and variables → Actions → New repository secret

2. **AWS_SERVER_HOST**
   - AWS 서버의 호스트 주소
   - 예: `51.20.106.74`
   - Settings → Secrets and variables → Actions → New repository secret

## Secrets 설정 방법

1. GitHub 저장소로 이동
2. Settings → Secrets and variables → Actions 클릭
3. "New repository secret" 클릭
4. 각 Secret 추가:
   - Name: `AWS_SSH_PRIVATE_KEY`
     Value: SSH 키 파일 전체 내용 (-----BEGIN RSA PRIVATE KEY----- 부터 -----END RSA PRIVATE KEY----- 까지)
   - Name: `AWS_SERVER_HOST`
     Value: `51.20.106.74`

## 서버 사전 요구사항

AWS 서버에 다음이 설정되어 있어야 합니다:

1. `~/capstone-app/` 디렉토리 존재
2. Java 21 설치
3. PostgreSQL 실행 중
4. 포트 8080, 9092 열림
5. SSH 키 기반 인증 설정

## 배포 프로세스

1. 코드를 `develop`, `main`, 또는 `feature/aws-sync` 브랜치에 push
2. GitHub Actions가 자동으로 트리거됨
3. 빌드 및 테스트 실행
4. 성공 시 AWS 서버에 자동 배포
5. 헬스체크로 배포 검증

## 문제 해결

### 배포 실패 시
- GitHub Actions 로그 확인
- 서버 SSH 접속 가능 여부 확인
- 서버의 Java 프로세스 상태 확인: `ps aux | grep java`
- 애플리케이션 로그 확인: `cat ~/capstone-app/app.log`

### 헬스체크 실패 시
- 서버 포트 8080이 열려있는지 확인
- 애플리케이션이 정상적으로 시작되었는지 확인
- 방화벽 설정 확인


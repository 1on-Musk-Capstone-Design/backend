# Capstone Project API Documentation

이 디렉토리는 Vercel에 배포되는 Swagger API 문서입니다.

## 배포 방법

### 1. Vercel CLI 사용

```bash
cd swagger-docs
vercel --prod
```

### 2. Vercel 웹 대시보드 사용

1. [Vercel](https://vercel.com)에 로그인
2. 프로젝트 추가 → `swagger-docs` 디렉토리 선택
3. 빌드 설정:
   - Framework Preset: Other
   - Build Command: (비워두기)
   - Output Directory: .
4. 환경변수 설정 (선택사항):
   - `API_URL`: API 서버 URL (예: `https://api.example.com/api`)

## 환경변수

Vercel 대시보드에서 다음 환경변수를 설정하세요:

- `API_URL`: API 서버의 기본 URL (예: `https://api.yourdomain.com/api`)

**중요:** 환경변수를 설정하지 않으면 기본값 `http://localhost:8080/api`이 사용되며, 이는 Vercel에서 접근할 수 없습니다.

## API 서버 URL 설정 방법

### 방법 1: Vercel 환경변수 설정 (권장)
1. Vercel 프로젝트 설정 → Environment Variables
2. `API_URL` 변수 추가 (예: `https://api.yourdomain.com/api`)
3. 재배포

### 방법 2: URL 파라미터 사용
브라우저에서 직접 URL 파라미터로 지정:
```
https://your-vercel-url.vercel.app/?apiUrl=https://api.yourdomain.com/api
```

## 로컬 테스트

```bash
# Python HTTP 서버 사용
python3 -m http.server 3000

# 또는 Node.js http-server 사용
npx http-server -p 3000
```

브라우저에서 `http://localhost:3000` 접속

## API 서버 설정

API 서버가 실행 중이어야 Swagger UI가 정상적으로 작동합니다.

- 로컬: `http://localhost:8080/api`
- 프로덕션: 환경변수 `API_URL`로 설정

## 참고

- Swagger UI는 서버의 `/api/v3/api-docs` 엔드포인트에서 OpenAPI 스펙을 가져옵니다.
- CORS 설정이 올바르게 되어 있어야 합니다.


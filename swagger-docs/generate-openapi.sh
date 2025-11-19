#!/bin/bash
# OpenAPI JSON 생성 스크립트
# 서버를 실행하고 OpenAPI JSON을 다운로드합니다

echo "OpenAPI JSON 생성 중..."
echo "1. 서버를 실행합니다 (백그라운드)"
cd ..
./gradlew bootRun > /dev/null 2>&1 &
SERVER_PID=$!

echo "2. 서버가 시작될 때까지 대기 중..."
sleep 15

echo "3. OpenAPI JSON 다운로드 중..."
curl -s http://localhost:8080/api/v3/api-docs > swagger-docs/openapi.json

if [ $? -eq 0 ]; then
    echo "✅ OpenAPI JSON 생성 완료: swagger-docs/openapi.json"
    echo "4. 서버 종료 중..."
    kill $SERVER_PID 2>/dev/null
    echo "✅ 완료!"
else
    echo "❌ OpenAPI JSON 다운로드 실패"
    echo "서버가 실행 중인지 확인하세요: http://localhost:8080/api/health"
    kill $SERVER_PID 2>/dev/null
    exit 1
fi


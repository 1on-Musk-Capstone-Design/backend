// Vercel 빌드 스크립트
// 정적 OpenAPI JSON 파일이 있으면 그대로 사용
const fs = require('fs');
const path = require('path');

const openApiPath = path.join(__dirname, 'openapi.json');

if (fs.existsSync(openApiPath)) {
    console.log('✅ OpenAPI JSON 파일 발견: openapi.json');
    console.log('정적 문서 모드로 배포됩니다.');
} else {
    console.log('⚠️  OpenAPI JSON 파일이 없습니다.');
    console.log('정적 문서를 사용하려면 openapi.json 파일이 필요합니다.');
    console.log('generate-openapi.sh 스크립트를 실행하여 생성하세요.');
}


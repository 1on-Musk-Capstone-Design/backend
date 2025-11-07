// Vercel 빌드 시 환경변수를 HTML에 주입하는 스크립트
const fs = require('fs');
const path = require('path');

const apiUrl = process.env.API_URL || 'http://localhost:8080/api';
const indexPath = path.join(__dirname, 'index.html');

let html = fs.readFileSync(indexPath, 'utf8');
html = html.replace('API_URL_PLACEHOLDER', apiUrl);

fs.writeFileSync(indexPath, html);
console.log(`API URL injected: ${apiUrl}`);


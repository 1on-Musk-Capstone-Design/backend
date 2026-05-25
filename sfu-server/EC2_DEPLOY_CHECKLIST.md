## SFU EC2 Checklist

mediasoup SFU와 coturn을 EC2에서 함께 운영할 때 사용하는 체크리스트입니다.

현재 이 저장소에서 기준으로 삼는 운영 대상:

- EC2 instance public IP: `3.85.63.196`
- Spring backend and SFU on the same EC2 host: use `http://127.0.0.1:4000` between them
- Browser clients connect to SFU/TURN through `3.85.63.196`

### 1. Backend env

Spring 백엔드용 GitHub Actions secrets에 아래 값을 설정합니다:

```env
APP_WEBRTC_SFU_ENABLED=true
APP_WEBRTC_SFU_SERVER_BASE_URL=http://127.0.0.1:4000
APP_WEBRTC_SFU_SERVER_HEALTH_PATH=/health
APP_WEBRTC_SFU_SERVER_ROOMS_PATH=/rooms
```

나중에 Spring과 SFU를 서로 다른 인스턴스로 분리하면 `APP_WEBRTC_SFU_SERVER_BASE_URL`을 SFU의 private IP 또는 내부 DNS로 변경합니다.

### 2. SFU server env

EC2 호스트에서 `backend/sfu-server/.env` 파일을 생성합니다:

```env
SFU_HTTP_PORT=4000
SFU_HTTP_HOST=0.0.0.0
SFU_LISTEN_IP=0.0.0.0
SFU_ANNOUNCED_IP=3.85.63.196
SFU_WORKER_MIN_PORT=40000
SFU_WORKER_MAX_PORT=49999
SFU_LOG_LEVEL=warn
```

`SFU_ANNOUNCED_IP`는 VPC 외부 브라우저가 실제로 접근할 수 있는 주소여야 합니다.

### 3. TURN env

프론트엔드 env 값은 coturn 호스트 기준으로 설정합니다:

```env
VITE_WEBRTC_TURN_URLS=turn:3.85.63.196:3478?transport=udp,turn:3.85.63.196:3478?transport=tcp
VITE_WEBRTC_TURN_USERNAME=turnuser
VITE_WEBRTC_TURN_CREDENTIAL=turnpass123
VITE_WEBRTC_ICE_TRANSPORT_POLICY=relay
```

### 4. Security group

SFU/TURN EC2 보안 그룹에 아래 인바운드 규칙을 엽니다:

- `4000/tcp` from the Spring backend host or security group
- `40000-49999/udp` from client networks for mediasoup RTP/RTCP
- `3478/udp` and `3478/tcp` from client networks for TURN
- `5349/tcp` from client networks if TURN over TLS is enabled
- `49160-49200/udp` from client networks for coturn relay

### 5. Process check

EC2에서 아래 명령을 실행합니다:

```bash
cd ~/path/to/backend/sfu-server
node -e "require('./src/config')"
npm run dev
```

config 확인 시 `SFU_ANNOUNCED_IP` 경고가 나오면 안 됩니다.

권장 파일 기준:

- SFU env: [`.env.ec2.example`](/Users/jeong-yanghyo/캡스톤_1/backend/sfu-server/.env.ec2.example:1)
- TURN config: [`turnserver.ec2.conf.example`](/Users/jeong-yanghyo/캡스톤_1/backend/sfu-server/turnserver.ec2.conf.example:1)
- Frontend env: [`.env.ec2.example`](/Users/jeong-yanghyo/캡스톤_1/frontend/.env.ec2.example:1)

### 6. Browser check

통화 시작 후 아래 항목을 확인합니다:

- frontend console shows `[useVoiceSFU] ICE config:` with your TURN URLs
- send/recv transport state reaches `connected`
- selected ICE pair is `relay` or a reachable `srflx`/`host`

`transport state`가 `connecting` 또는 `failed`에서 멈추면 먼저 `SFU_ANNOUNCED_IP`, 보안 그룹 규칙, TURN relay 포트를 확인합니다.

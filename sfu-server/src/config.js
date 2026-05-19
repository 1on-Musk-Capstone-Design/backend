const path = require("path");
const dotenv = require("dotenv");

dotenv.config({ path: path.resolve(process.cwd(), ".env") });

function toInt(value, fallback) {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

module.exports = {
  http: {
    host: process.env.SFU_HTTP_HOST || "0.0.0.0",
    port: toInt(process.env.SFU_HTTP_PORT, 4000),
  },
  mediasoup: {
    logLevel: process.env.SFU_LOG_LEVEL || "warn",
    worker: {
      rtcMinPort: toInt(process.env.SFU_WORKER_MIN_PORT, 40000),
      rtcMaxPort: toInt(process.env.SFU_WORKER_MAX_PORT, 49999),
    },
    webRtcTransport: {
      listenIp: process.env.SFU_LISTEN_IP || "0.0.0.0",
      announcedIp: process.env.SFU_ANNOUNCED_IP || "127.0.0.1",
    },
    routerMediaCodecs: [
      {
        kind: "audio",
        mimeType: "audio/opus",
        clockRate: 48000,
        channels: 2,
      },
      {
        kind: "video",
        mimeType: "video/VP8",
        clockRate: 90000,
      },
    ],
  },
};

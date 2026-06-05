#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

export APP_DEV_BOOTSTRAP_AUTH="${APP_DEV_BOOTSTRAP_AUTH:-true}"

./gradlew assemble -x test
docker compose up --build -d
docker compose ps

#!/usr/bin/env bash
# =============================================================================
#  FinTech Compliance AI Orchestrator — VM bootstrap (Yandex Cloud, Ubuntu)
#  ------------------------------------------------------------
#  Выполняет на удалённой VM все шаги развёртывания, требуемые DevOps-регламентом:
#
#    1. Записывает /etc/docker/daemon.json
#       (registry-mirrors -> cr.yandex;  DNS -> 77.88.8.8 / 8.8.8.8)
#    2. Перезапускает службу Docker
#    3. Подтягивает код из Git-репозитория
#    4. Запускает `docker compose up -d --build`
#    5. Если BuildKit блокирует сборку — авто-fallback на DOCKER_BUILDKIT=0
#    6. Показывает `docker compose logs app` для подтверждения, что
#       Spring Boot поднялся и достучался до PostgreSQL / YandexGPT
#
#  Использование:
#      chmod +x deploy/vm-bootstrap.sh
#      sudo ./deploy/vm-bootstrap.sh
#
#  Параметры (опц., через ENV):
#      REPO_URL   — git URL (default: репозиторий проекта)
#      BRANCH     — ветка (default: main)
#      APP_DIR    — путь на VM (default: /opt/fintech-compliance)
# =============================================================================

set -Eeuo pipefail
IFS=$'\n\t'

# ---------- параметры по умолчанию ----------
REPO_URL="${REPO_URL:-https://github.com/markmus11012020-max/FinTech-Compliance-AI-Orchestrator.git}"
BRANCH="${BRANCH:-master}"
APP_DIR="${APP_DIR:-/opt/fintech-compliance}"

DAEMON_JSON_PATH="/etc/docker/daemon.json"
DAEMON_JSON_SOURCE="${DAEMON_JSON_SOURCE:-${APP_DIR}/deploy/daemon.json}"

# ---------- утилитарные функции ----------
log()  { printf '\033[1;34m[%(%H:%M:%S)T] %s\033[0m\n' -1 "$*"; }
ok()   { printf '\033[1;32m[OK] %s\033[0m\n' "$*"; }
err()  { printf '\033[1;31m[ERR] %s\033[0m\n' "$*" >&2; }
die()  { err "$*"; exit 1; }

require_root() {
  if [[ $EUID -ne 0 ]]; then
    die "Скрипт требует root. Запустите: sudo $0"
  fi
}

# ---------- 0. Предварительные проверки ----------
require_root
log "Проверка окружения..."
command -v docker >/dev/null || die "Docker не установлен. Поставьте docker + docker compose plugin."
docker compose version >/dev/null 2>&1 || die "Плагин 'docker compose' недоступен."
ok "Docker / docker compose доступны."

# ---------- 1. /etc/docker/daemon.json ----------
log "1/6  Записываю ${DAEMON_JSON_PATH} (mirror + DNS)..."
mkdir -p /etc/docker
if [[ ! -f "${DAEMON_JSON_SOURCE}" ]]; then
  die "Шаблон ${DAEMON_JSON_SOURCE} не найден. Сначала склонируйте репозиторий."
fi
# Валидируем JSON до записи
python3 -c 'import json,sys; json.load(open("'"${DAEMON_JSON_SOURCE}"'"))' \
  || die "daemon.json содержит невалидный JSON."

cp -f "${DAEMON_JSON_SOURCE}" "${DAEMON_JSON_PATH}"
chmod 0644 "${DAEMON_JSON_PATH}"
ok "daemon.json записан:"
cat "${DAEMON_JSON_PATH}"

# ---------- 2. Перезапуск Docker ----------
log "2/6  Перезапускаю службу Docker..."
systemctl restart docker
# ждём, пока сокет поднимется
for _ in {1..20}; do
  if docker info >/dev/null 2>&1; then break; fi
  sleep 1
done
docker info >/dev/null 2>&1 || die "Docker-демон не отвечает после restart."
ok "Docker работает."

# ---------- 3. Код проекта ----------
log "3/6  Подготовка исходников в ${APP_DIR}..."
mkdir -p "$(dirname "${APP_DIR}")"
if [[ -d "${APP_DIR}/.git" ]]; then
  log "Репозиторий уже склонирован, обновляю..."
  git -C "${APP_DIR}" fetch --all --prune
  git -C "${APP_DIR}" reset --hard "origin/${BRANCH}"
else
  git clone --branch "${BRANCH}" --depth 1 "${REPO_URL}" "${APP_DIR}"
fi
cd "${APP_DIR}"

# .env из .env.example
if [[ ! -f .env && -f .env.example ]]; then
  cp .env.example .env
  log ".env создан из .env.example (заполните секреты перед prod-эксплуатацией)."
fi

# ---------- 4. docker compose up -d --build (с fallback) ----------
log "4/6  Сборка и запуск контейнеров..."
if ! docker compose up -d --build; then
  err "BuildKit/builder блокирует сборку — повторяю с DOCKER_BUILDKIT=0..."
  export DOCKER_BUILDKIT=0
  docker compose up -d --build
fi
ok "Контейнеры подняты."

# ---------- 5. Ожидание готовности приложения ----------
log "5/6  Жду готовности Spring Boot..."
for i in {1..30}; do
  status="$(docker inspect -f '{{.State.Status}}' compliance_app 2>/dev/null || true)"
  health="$(docker inspect -f '{{.State.Health.Status}}' compliance_app 2>/dev/null || true)"
  if [[ "${status}" == "running" ]]; then
    if [[ "${health}" == "healthy" || -z "${health}" ]]; then
      ok "compliance_app: ${status}${health:+ / ${health}} (попытка ${i})"
      break
    fi
  fi
  sleep 2
done

# ---------- 6. Логи ----------
log "6/6  docker compose logs app:"
echo "------------------------------------------------------------"
docker compose logs --tail=200 app || true
echo "------------------------------------------------------------"

# ---------- сводка ----------
cat <<EOF

============================================================
 FinTech Compliance AI Orchestrator — развёрнут
 APP_DIR:    ${APP_DIR}
 Endpoint:   http://<VM-IP>:8080/api/v1/health
 Команды:
   cd ${APP_DIR} && docker compose ps
   cd ${APP_DIR} && docker compose logs -f app
   cd ${APP_DIR} && docker compose down
============================================================
EOF
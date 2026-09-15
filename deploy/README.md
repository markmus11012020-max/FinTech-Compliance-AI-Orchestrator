# Деплой на Yandex Cloud VM

Каталог содержит DevOps-артефакты для развёртывания микросервиса
**FinTech Compliance AI Orchestrator** на удалённой Ubuntu VM
в Yandex Cloud.

## Состав

| Файл | Назначение |
|---|---|
| `daemon.json` | Шаблон `/etc/docker/daemon.json` для VM (mirror + DNS) |
| `vm-bootstrap.sh` | Полный оркестрационный скрипт (все шаги деплоя) |

## Быстрый старт

### Вариант A. С локальной машины через SSH (одной командой)

```bash
# Требуется: установленные sshpass/expect и проброшенный SSH-ключ.
# На локальной Windows-машине из PowerShell/WSL:
ssh ubuntu@<VM-IP> 'bash -s' < deploy/vm-bootstrap.sh
```

### Вариант B. Ручной режим на самой VM

```bash
# 1. Скопировать проект на VM (git clone / scp)
sudo apt-get update && sudo apt-get install -y docker.io docker-compose-plugin
git clone https://github.com/markmus11012020-max/FinTech-Compliance-AI-Orchestrator.git /opt/fintech-compliance
cd /opt/fintech-compliance

# 2. Запустить оркестратор
chmod +x deploy/vm-bootstrap.sh
sudo ./deploy/vm-bootstrap.sh
```

## Что делает `vm-bootstrap.sh`

1. Записывает `/etc/docker/daemon.json` (mirror `cr.yandex`, DNS `77.88.8.8` + `8.8.8.8`).
2. Перезапускает `docker.service` через `systemctl restart docker`.
3. Клонирует/обновляет код из Git-репозитория в `${APP_DIR:-/opt/fintech-compliance}`.
4. Запускает `docker compose up -d --build`.
5. Если BuildKit блокирует сборку — повторяет с `DOCKER_BUILDKIT=0`.
6. Показывает `docker compose logs app` для верификации успешного старта.

## Параметры окружения (опц.)

| ENV | Default | Описание |
|---|---|---|
| `REPO_URL` | URL репозитория проекта | Откуда клонировать код |
| `BRANCH`   | `main` | Ветка |
| `APP_DIR`  | `/opt/fintech-compliance` | Каталог приложения на VM |
| `DAEMON_JSON_SOURCE` | `${APP_DIR}/deploy/daemon.json` | Путь к шаблону |
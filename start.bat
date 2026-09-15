@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM ============================================================
REM  FinTech Compliance AI Orchestrator - деплой-скрипт (Windows)
REM  Оркестрация: stop -> clean -> build -> up
REM ============================================================

set "PROJECT_DIR=%~dp0"
cd /d "%PROJECT_DIR%"

echo.
echo [1/5] Остановка ранее запущенных контейнеров...
docker compose down --remove-orphans 2>nul
docker stop compliance_app 2>nul
docker stop compliance_db 2>nul
docker rm -f compliance_app 2>nul
docker rm -f compliance_db 2>nul

echo.
echo [2/5] Очистка локального кэша сборки...
if exist target rmdir /s /q target
if exist "%USERPROFILE%\.m2\repository\com\fintech\compliance" rmdir /s /q "%USERPROFILE%\.m2\repository\com\fintech\compliance" 2>nul

echo.
echo [3/5] Подготовка .env из .env.example (если отсутствует)...
if not exist .env (
    echo .env не найден — копирую .env.example
    copy /Y .env.example .env >nul
) else (
    echo .env уже существует, оставляю как есть.
)

echo.
echo [4/5] Сборка окружения и установка зависимостей (docker compose build)...
docker compose build --no-cache

echo.
echo [5/5] Запуск проекта в фоне...
docker compose up -d

echo.
echo ============================================================
echo  Сервис запущен: http://localhost:8080
echo  Healthcheck:    http://localhost:8080/api/v1/health
echo  Логи:           docker compose logs -f app
echo ============================================================

endlocal
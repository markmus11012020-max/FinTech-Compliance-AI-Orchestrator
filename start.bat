@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM ============================================================
REM  FinTech Compliance AI Orchestrator - деплой-скрипт (Windows)
REM  Оркестрация: stop -> clean -> build -> up -> healthcheck
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
docker builder prune -f >nul 2>&1

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
if errorlevel 1 (
    echo [WARN] BuildKit заблокировал сборку — повтор с DOCKER_BUILDKIT=0...
    set DOCKER_BUILDKIT=0
    docker compose build --no-cache
)

echo.
echo [5/5] Запуск проекта в фоне и healthcheck...
docker compose up -d
if errorlevel 1 (
    echo [FAIL] docker compose up завершился с ошибкой. Смотрите: docker compose logs
    exit /b 1
)

echo.
echo Жду готовности Spring Boot (health endpoint)...
set /a attempts=0
:wait_health
set /a attempts+=1
if %attempts% gtr 30 (
    echo [WARN] Healthcheck не ответил за 60с. Проверьте логи вручную.
    goto :show_logs
)
for /f "tokens=*" %%s in ('curl -s -o nul -w "%%{http_code}" http://localhost:8080/api/v1/health 2^>nul') do set "HTTP_CODE=%%s"
if /i "%HTTP_CODE%"=="200" (
    echo [OK] /api/v1/health -> 200
    goto :show_logs
)
timeout /t 2 /nobreak >nul
goto :wait_health

:show_logs
echo.
echo ============================================================
echo  Сервис запущен: http://localhost:8080
echo  Healthcheck:    http://localhost:8080/api/v1/health
echo  Логи:           docker compose logs -f app
echo  Остановка:      docker compose down
echo ============================================================
docker compose logs --tail=120 app

endlocal
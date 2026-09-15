# FinTech Compliance AI Orchestrator

> **Production-ready Spring Boot микросервис для AML-комплаенса с локальной анонимизацией данных по 152-ФЗ.**
> Целевая аудитория: российские банки и финтех-компании уровня WMT Group.
>
>  **Живое демо (Yandex Cloud VM):** Swagger UI — <http://158.160.152.145:8080/swagger-ui.html> · OpenAPI — <http://158.160.152.145:8080/api-docs> · Health — <http://158.160.152.145:8080/api/v1/health>

---

## 1. Бизнес-контекст

Финансовый сектор РФ обязан соблюдать сразу два нормативных акта:

| Норматив | Что требует | Как закрыто в проекте |
|---|---|---|
| **115-ФЗ** (противодействие отмыванию доходов) | AML-скоринг каждой подозрительной транзакции | LLM-агент возвращает `risk_level` и `reason` |
| **152-ФЗ** (персональные данные) | Запрет на передачу ПДн в третьи страны без согласия | Локальный pipeline анонимизации + суверенные LLM (YandexGPT / GigaChat) |

Никакие данные клиентов **не покидают периметр РФ**, а персональная информация **не передаётся внешним LLM в открытом виде** — её заменяют токены `[TOKEN_N]`.

---

## 2. Архитектурные решения

### 2.1. Технологический стек

| Слой | Технология | Почему |
|---|---|---|
| Язык | **Java 17** | LTS, банковский стандарт, многопоточность |
| Framework | **Spring Boot 3.2.5** | Production-ready экосистема (Data, Security, Actuator) |
| ORM | **Spring Data JPA + Hibernate** | Зрелость, миграции, аудит |
| БД | **PostgreSQL 15** | ACID, расширяемость, сертифицирован в реестре отечественного ПО |
| HTTP | **Spring `RestTemplate`** | Минимальные зависимости, стабильные таймауты |
| Контейнеризация | **Docker + docker-compose** | Воспроизводимый деплой на Yandex Cloud VM |
| Облако | **Yandex Cloud (Ubuntu VM)** | Российская подсеть, соответствие 152-ФЗ |

### 2.2. Принципы ООП и масштабируемости

* **Strategy** — `LlmProvider` интерфейс с реализациями `MockLlmProvider`, `YandexGptProvider`, `GigaChatProvider`. Добавление нового провайдера = +1 класс без правок оркестратора.
* **Factory** — `LlmProviderFactory` инкапсулирует выбор активного провайдера по конфигу.
* **Single Responsibility** — `DataMaskingService` отвечает только за анонимизацию, `AiOrchestratorService` — только за пайплайн.
* **Dependency Injection** — все зависимости внедряются через Spring-контейнер.
* **Repository** — `AuditLogRepository` инкапсулирует работу с БД.

### 2.3. Структура проекта

```
fintech-compliance-orchestrator/
├── pom.xml
├── Dockerfile                          # multi-stage build (Maven -> JRE)
├── docker-compose.yml                  # PostgreSQL + app
├── start.bat                           # автодеплой для Windows
├── .env.example                        # пресет переменных окружения
├── deploy/
│   ├── vm-bootstrap.sh                 # оркестратор деплоя на Yandex Cloud VM
│   ├── daemon.json                     # шаблон /etc/docker/daemon.json
│   └── README.md
├── README.md
└── src/
    ├── main/
    │   ├── java/com/fintech/compliance/
    │   │   ├── ComplianceOrchestratorApplication.java
    │   │   ├── config/
    │   │   │   ├── AppConfig.java              # RestTemplate + таймауты
    │   │   │   └── ComplianceProperties.java   # @ConfigurationProperties
    │   │   ├── controller/
    │   │   │   ├── TransactionController.java  # POST /api/v1/compliance/check
    │   │   │   └── HealthController.java
    │   │   ├── service/
    │   │   │   ├── AiOrchestratorService.java  # пайплайн Anonymize -> LLM -> Deanonymize
    │   │   │   ├── DataMaskingService.java     # 152-ФЗ: токенизация
    │   │   │   ├── LlmProviderFactory.java
    │   │   │   └── LlmProvider.java            # контракт провайдера
    │   │   ├── provider/
    │   │   │   ├── MockLlmProvider.java
    │   │   │   ├── YandexGptProvider.java      # Foundation Models API
    │   │   │   └── GigaChatProvider.java       # Sber API
    │   │   ├── dto/
    │   │   │   ├── CheckRequest.java
    │   │   │   ├── ComplianceReport.java
    │   │   │   ├── MaskedPayload.java
    │   │   │   ├── YandexGptDto.java
    │   │   │   └── GigaChatDto.java
    │   │   ├── entity/AuditLog.java
    │   │   ├── repository/AuditLogRepository.java
    │   │   └── exception/
    │   │       ├── ComplianceException.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       └── application.yml
    └── test/java/com/fintech/compliance/
        └── ComplianceOrchestratorApplicationTests.java
```

---

## 3. Pipeline работы

```
                  ┌─────────────────────────────────────┐
 HTTP POST        │ DataMaskingService.anonymize()     │
 (транзакция) ──► │ - телефоны, карты, паспорта, ФИО   │
                  │   заменяются на [TOKEN_N]          │
                  └───────────────┬─────────────────────┘
                                  │ маскированный текст
                                  ▼
                  ┌─────────────────────────────────────┐
                  │ LlmProvider (yandexgpt/gigachat/   │
                  │                 mock)              │
                  │ system prompt по правилам ЦБ РФ    │
                  │ JSON-layout: is_suspicious,        │
                  │ risk_level, reason                 │
                  └───────────────┬─────────────────────┘
                                  │ сырой JSON
                                  ▼
                  ┌─────────────────────────────────────┐
                  │ DataMaskingService.deanonymize()   │
                  │ - обратная подстановка токенов     │
                  └───────────────┬─────────────────────┘
                                  │
                                  ▼
                  ┌─────────────────────────────────────┐
                  │ AuditLogRepository.save()          │
                  │ - метаданные без ПДн               │
                  └───────────────┬─────────────────────┘
                                  │
                                  ▼
                          ComplianceReport (JSON)
```

---

## 4. Контракт API

### 4.1. POST `/api/v1/compliance/check`

Запрос:
```json
{
  "transactionId": "tx-2026-0001",
  "payload": "Клиент Иван Иванов, тел +79991234567, перевод 500 000 RUB за границу"
}
```

Ответ:
```json
{
  "is_suspicious": true,
  "risk_level": "HIGH",
  "reason": "Клиент Иван Иванов, тел +79991234567, совершает крупный перевод за рубеж без подтверждающих документов."
}
```

### 4.2. POST `/api/v1/compliance/check-raw`

Принимает plain-text тело. Удобен для `curl`:
```bash
curl -X POST http://localhost:8080/api/v1/compliance/check-raw \
     -H "Content-Type: text/plain" \
     --data "Клиент Иван Иванов, тел +79991234567, перевод 500000 RUB за границу"
```

### 4.3. POST `/api/v1/compliance/info`

Возвращает активного провайдера и список доступных.

### 4.4. GET `/api/v1/health`

Состояние сервиса для liveness/readiness probe Kubernetes.

---

## 5. Развёртывание на Yandex Cloud

### 5.1. Подготовка VM

1. Создать Ubuntu 22.04 VM в Yandex Cloud.
2. Установить Docker + docker compose plugin.
3. Открыть порты `8080` (приложение) и `5432` (БД, опционально).

### 5.2. Клонирование и настройка

```bash
git clone https://github.com/markmus11012020-max/FinTech-Compliance-AI-Orchestrator.git
cd FinTech-Compliance-AI-Orchestrator
cp .env.example .env
nano .env   # заполнить YANDEXGPT_API_KEY / GIGACHAT_API_KEY
```

### 5.3. Получение ключей

| Провайдер | Где получить |
|---|---|
| **YandexGPT** | https://cloud.yandex.ru/docs/iam/concepts/authorization/api-key — `YANDEXGPT_API_KEY`, `YANDEXGPT_FOLDER_ID` |
| **GigaChat** | https://developers.sber.ru/docs/ru/gigachat/quickstart/quick-start-acquiring-access-token — `GIGACHAT_API_KEY` |

### 5.4. Быстрый запуск на VM (рекомендуемый путь)

В каталоге `deploy/` лежит полный оркестрационный скрипт, который выполняет
все DevOps-шаги одной командой:

```bash
# На самой VM:
chmod +x deploy/vm-bootstrap.sh
sudo ./deploy/vm-bootstrap.sh
```

Или удалённо с локальной машины:

```bash
ssh ubuntu@<VM-IP> 'bash -s' < deploy/vm-bootstrap.sh
```

Что делает `vm-bootstrap.sh`:

1. Записывает `/etc/docker/daemon.json` (registry-mirror `https://cr.yandex`, DNS `77.88.8.8` + `8.8.8.8`).
2. Перезапускает службу `docker` через `systemctl restart docker`.
3. Клонирует/обновляет код из Git-репозитория.
4. Запускает `docker compose up -d --build`.
5. Если `BuildKit` блокирует сборку — автоматически повторяет с `DOCKER_BUILDKIT=0`.
6. Выводит `docker compose logs app` для подтверждения, что Spring Boot подключился
   к PostgreSQL и YandexGPT без ошибок.

Параметры (через ENV): `REPO_URL`, `BRANCH`, `APP_DIR`, `DAEMON_JSON_SOURCE`.

### 5.5. Ручной запуск (fallback)

```bash
sudo tee /etc/docker/daemon.json >/dev/null <<'EOF'
{
  "registry-mirrors": ["https://cr.yandex"],
  "dns": ["77.88.8.8", "8.8.8.8"]
}
EOF
sudo systemctl restart docker

docker compose up -d --build
# если BuildKit мешает:
DOCKER_BUILDKIT=0 docker compose up -d --build

docker compose logs app
```

Проверка:
```bash
curl http://localhost:8080/api/v1/health
```

Остановка:
```bash
docker compose down
```

Альтернатива на Windows (локальная разработка):
```bat
start.bat
```

---

## 6. Безопасность и 152-ФЗ

* **Локальная анонимизация** — ПДн (телефоны, карты, паспорта, ФИО) заменяются на токены **до** любого сетевого вызова.
* **Карта замен** хранится только в памяти JVM, никуда не пишется.
* **Аудит-лог** содержит только метаданные (`transactionId`, `provider`, `risk_level`) — без исходных данных.
* **Суверенные LLM** — YandexGPT и GigaChat физически расположены в РФ, что соответствует требованиям 152-ФЗ о хранении и обработке ПДн.
* **`open-in-view: false`** — JPA-сессия закрывается до сериализации ответа (минимизация утечки persistence context).

---

## 7. Расширение

Чтобы добавить нового LLM-провайдера:

1. Реализовать интерфейс `LlmProvider` (например, `MistralProvider`).
2. Добавить `@Component` Spring-бин.
3. Добавить секцию конфигурации в `ComplianceProperties`.
4. Зарегистрировать в `LlmProviderFactory` (через DI — без правок существующего кода).

---

## 8. Лицензия

Внутренний демо-проект. Свободное использование для целей оценки архитектуры.

---

## 9. Известные проблемы и исправления (2026-09-15)

### Исправление HTTP 500 при старте приложения

**Проблема:** При запуске приложения с `LLM_PROVIDER=yandexgpt` и отсутствующими/пустыми API ключами возникала HTTP 500 ошибка.

**Причина:** 
- Отсутствовала зависимость springdoc-openapi для Swagger UI в Spring Boot 3.x
- При выборе провайдера yandexgpt/gigachat с пустыми ключами приложение не могло корректно инициализироваться

**Решение (commits e2b908c, 85e98f4):**

1. **Добавлена зависимость springdoc-openapi-starter-webmvc-ui** (версия 2.3.0) в pom.xml
2. **Настроен ResourceHandler** для Swagger UI в AppConfig.java (Spring Boot 3.x совместимость)
3. **Добавлен метод `canWork()`** в интерфейс `LlmProvider`:
   - Проверяет наличие и валидность API ключей
   - Возвращает `true` если провайдер может работать, `false` иначе
4. **Реализован graceful fallback** в `LlmProviderFactory`:
   - При отсутствии ключей автоматически переключается на mock-провайдер
   - Записывает предупреждение в лог: `"Провайдер X не может работать (отсутствуют ключи). Используется mock."`
5. **Обновлен .env по умолчанию**: `LLM_PROVIDER=mock` для стабильной работы без внешних зависимостей
6. **Добавлена конфигурация springdoc** в application.yml:
   ```yaml
   springdoc:
     api-docs:
       path: /api-docs
     swagger-ui:
       path: /swagger-ui.html
       enabled: true
   ```

### Исправление имени ветки в vm-bootstrap.sh

**Проблема:** Скрипт деплоя использовал ветку `main` по умолчанию, хотя репозиторий использует `master`.

**Решение (commit e2b908c):** Изменена переменная `BRANCH` по умолчанию с `main` на `master` в deploy/vm-bootstrap.sh

---

## 10. Быстрое обновление на VM

После получения изменений из репозитория:

```bash
# Вариант 1: Автоматический (рекомендуется)
cd /opt/fintech-compliance
sudo ./deploy/vm-bootstrap.sh

# Вариант 2: Ручной
cd /opt/fintech-compliance
git pull origin master
DOCKER_BUILDKIT=0 docker compose up -d --build
docker compose logs --tail=50 app
```

Проверка работоспособности:
```bash
curl http://localhost:8080/api/v1/health
# Ожидаемый ответ: {"status":"UP","provider":"mock",...}
```

Живой деплой на Yandex Cloud VM (публичный адрес `158.160.152.145`):
```
http://158.160.152.145:8080/swagger-ui.html
http://158.160.152.145:8080/api-docs
http://158.160.152.145:8080/api/v1/health
```

Swagger UI локально доступен по адресу:
```
http://localhost:8080/swagger-ui.html
http://localhost:8080/api-docs
```
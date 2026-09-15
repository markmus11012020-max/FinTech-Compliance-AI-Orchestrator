Act as a Senior Java Developer and Cloud DevOps Engineer. We are creating a production-ready, fully complete Java Spring Boot microservice called "fintech-compliance-orchestrator". It acts as an AML compliance agent with local data masking to ensure strict compliance with Russian 152-FZ personal data laws.

The user is a Product Owner / Orchestrator and does not write code manually. You must create ALL files, write 100% of the working code, configure the database layer, and prepare the project for immediate deployment via Docker to a Yandex Cloud Ubuntu Virtual Machine.

CRITICAL SECURITY REQUIREMENT: Do NOT use RouterAI, OpenRouter, or DeepSeek APIs. The service must directly support Russian compliant LLM providers: YandexGPT, Sber GigaChat, and a local 'mock' provider for safe testing.

Your Tasks:

1. Update or create 'src/main/resources/application.yml':
Set application port to 8080. Configure Spring Data JPA for PostgreSQL. Add clear configuration sections for Russian LLMs:
spring:
  datasource:
    url: jdbc:postgresql://db:5432/compliance_db
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

compliance:
  llm:
    provider: ${LLM_PROVIDER:-mock} # yandexgpt | gigachat | mock
    timeout-seconds: ${LLM_TIMEOUT_SECONDS:-10}
    temperature: ${LLM_TEMPERATURE:-0.2}
  yandexgpt:
    api-key: ${YANDEXGPT_API_KEY:-}
    folder-id: ${YANDEXGPT_FOLDER_ID:-}
    model: ${YANDEXGPT_MODEL:-yandexgpt/latest}
  gigachat:
    api-key: ${GIGACHAT_API_KEY:-}
    scope: ${GIGACHAT_SCOPE:-GIGACHAT_API_PERS}
    model: ${GIGACHAT_MODEL:-GigaChat-Pro}

2. Create 'src/main/java/com/fintech/compliance/service/AiOrchestratorService.java':
- Use RestTemplate or WebClient to process requests to the chosen provider based on `compliance.llm.provider`.
- If 'mock': instantly return a safe sample JSON compliance report without external calls.
- If 'yandexgpt': implement OAuth/API-key authorization header, construct the correct Yandex Cloud API body format (including folderId and messages array).
- If 'gigachat': implement their required Authorization and Content-Type headers for Sber API.
- System prompt instructs LLM to check the transaction data (which contains tokens like [TOKEN_XXXX]) based on Central Bank of Russia compliance rules and return a strict JSON layout: {"is_suspicious": boolean, "risk_level": "LOW/MEDIUM/HIGH", "reason": "string"}.

3. Create 'src/main/java/com/fintech/compliance/controller/TransactionController.java':
- Create a POST route `/api/v1/compliance/check` accepting raw transactional text.
- Execute full pipeline: Anonymize -> Send to chosen domestic LLM -> Deanonymize the resulting report to restore real entity names -> Return clean final report.

4. Create 'Dockerfile' in the project root (Multi-stage production build):
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/compliance-orchestrator-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

5. Create 'docker-compose.yml' in the project root:
version: '3.8'
services:
  db:
    image: postgres:15-alpine
    container_name: compliance_db
    restart: always
    environment:
      POSTGRES_DB: compliance_db
      POSTGRES_USER: ${DB_USER:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-secure_pass}
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  app:
    build: .
    container_name: compliance_app
    restart: always
    ports:
      - "8080:8080"
    depends_on:
      - db
    environment:
      DB_USER: ${DB_USER:-postgres}
      DB_PASSWORD: ${DB_PASSWORD:-secure_pass}
      LLM_PROVIDER: ${LLM_PROVIDER:-mock}
      LLM_TIMEOUT_SECONDS: ${LLM_TIMEOUT_SECONDS:-10}
      LLM_TEMPERATURE: ${LLM_TEMPERATURE:-0.2}
      YANDEXGPT_API_KEY: ${YANDEXGPT_API_KEY}
      YANDEXGPT_FOLDER_ID: ${YANDEXGPT_FOLDER_ID}
      YANDEXGPT_MODEL: ${YANDEXGPT_MODEL}
      GIGACHAT_API_KEY: ${GIGACHAT_API_KEY}
      GIGACHAT_SCOPE: ${GIGACHAT_SCOPE}
      GIGACHAT_MODEL: ${GIGACHAT_MODEL}

volumes:
  pgdata:

6. Create a highly professional, enterprise-grade 'README.md' in Russian:
- Position the project as a showcase for enterprise architectural standards in Russian FinTech (WMT Group target).
- Emphasize total compliance with 152-FZ: Data Masking Pipeline runs locally, and cloud infrastructure relies entirely on sovereign Russian LLMs (YandexGPT / GigaChat) officially allowed for corporate architectures.
- Detail why Java 17/Spring Boot + PostgreSQL was chosen (scalability, multi-threading, banking standard).
- Provide a simple deployment guide for Yandex Cloud: cloning, setting up `.env` with Yandex/Sber credentials, and running `docker compose up -d`.

Generate all configurations cleanly without missing packages or imports.

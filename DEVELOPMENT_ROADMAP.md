# Roadmap разработки: XML Invoice Validation Backend

Детальный пошаговый план разработки платформы импорта и валидации электронных счетов-фактур.

---

## Фаза 0: Подготовка инфраструктуры (Infrastructure)

### 0.1 Зависимости и конфигурация
- [ ] **0.1.1** Добавить в `pom.xml`:
  - `spring-boot-starter-amqp` (RabbitMQ)
  - `jackson-dataformat-xml`
  - `springdoc-openapi-starter-webmvc-ui` (Swagger UI)
  - `lombok` (optional, для сокращения boilerplate)
  - `junit-jupiter`, `mockito` (если не входят в spring-boot-starter-test)
- [ ] **0.1.2** Проверить/исправить test-зависимости (возможно `spring-boot-starter-test` вместо отдельных test-starters)
- [ ] **0.1.3** Создать профили в `application.yaml`:
  - `dev` — localhost Postgres, localhost RabbitMQ, H2 для тестов (опционально)
  - `docker` — host.docker.internal / имена сервисов из docker-compose

### 0.2 Docker и окружение
- [ ] **0.2.1** Создать `docker-compose.yml`:
  - сервис `postgres` (image: postgres:16, порт 5432, volume для данных)
  - сервис `rabbitmq` (image: rabbitmq:3-management, порты 5672, 15672)
  - переменные окружения для postgres (POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD)
- [ ] **0.2.2** Создать `Dockerfile` для приложения (multi-stage build, Java 21)
- [ ] **0.2.3** Добавить сервис `app` в docker-compose (зависимости от postgres и rabbitmq)
- [ ] **0.2.4** Добавить `.env.example` с переменными для локальной разработки

### 0.3 Структура пакетов (Modular Monolith)
- [ ] **0.3.1** Создать базовую структуру пакетов:
  ```
  com.vpvpteam.xmlinvoicevalidationbackend/
  ├── auth/
  ├── imports/
  ├── messaging/
  ├── formats/
  ├── canonical/
  ├── validation/
  ├── rules/
  └── reporting/
  ```
- [ ] **0.3.2** В каждом модуле при необходимости: `api`, `domain`, `infra` (или аналогичная внутренняя структура)

---

## Фаза 1: База данных и базовые сущности (Database & Core Entities)

### 1.1 JPA Entity — users и organizations
- [ ] **1.1.1** `Organization` entity (id, name, createdAt, и т.п.)
- [ ] **1.1.2** `User` entity (id, email, passwordHash, role, organizationId, createdAt)
- [ ] **1.1.3** Enum `UserRole`: ADMIN, ACCOUNTANT
- [ ] **1.1.4** Repository интерфейсы: `UserRepository`, `OrganizationRepository`

### 1.2 JPA Entity — import session и invoice metadata
- [ ] **1.2.1** `ImportSession` entity:
  - id (UUID), createdByUserId, createdAt
  - status (enum: RECEIVED, QUEUED, PROCESSING, COMPLETED, FAILED_PARTIAL, FAILED)
  - invoicesTotal, invoicesProcessed, errorsCount, warningsCount
  - finishedAt
- [ ] **1.2.2** `InvoiceMetadata` entity:
  - id (UUID), importSessionId (FK)
  - format, formatVersion, invoiceNumber, issueDate
  - sellerVatId, buyerVatId, currency, totalGross
  - invoiceFingerprint (для дедупликации)
  - createdAt, updatedAt
- [ ] **1.2.3** Enum `ImportSessionStatus`
- [ ] **1.2.4** `ImportSessionRepository`, `InvoiceMetadataRepository`

### 1.3 JPA Entity — validation
- [ ] **1.3.1** `ValidationRun` entity:
  - id (UUID), invoiceMetadataId, importSessionId
  - validationProfileId, status (OK, WARN, ERROR, FAILED_TECHNICAL)
  - startedAt, finishedAt, durationMs
- [ ] **1.3.2** `ValidationIssue` entity:
  - id, validationRunId (FK)
  - severity (ERROR, WARNING, INFO)
  - stage (XSD, PARSING, MAPPING, TECHNICAL, ARITHMETIC, BUSINESS)
  - ruleKey, message, fieldPath, code
- [ ] **1.3.3** Enum `ValidationSeverity`, `ValidationStage`, `ValidationRunStatus`
- [ ] **1.3.4** `ValidationRunRepository`, `ValidationIssueRepository`

### 1.4 JPA Entity — rules и профили
- [ ] **1.4.1** `ValidationProfile` entity (id, name, ownerOrganizationId, status: ACTIVE/INACTIVE)
- [ ] **1.4.2** `ValidationRuleDefinition` entity (id, ruleKey, engine, severityDefault, description)
- [ ] **1.4.3** `ValidationProfileRule` entity (profileId, ruleKey, enabled, severityOverride, paramsJson)
- [ ] **1.4.4** `AuditEvent` entity (id, entityType, entityId, action, userId, changedAt, diffJson)
- [ ] **1.4.5** Repository: `ValidationProfileRepository`, `ValidationRuleDefinitionRepository`, `ValidationProfileRuleRepository`, `AuditEventRepository`

### 1.5 Flyway/Liquibase миграции
- [ ] **1.5.1** Добавить Flyway (или Liquibase) в pom.xml
- [ ] **1.5.2** Создать V1__init.sql (или эквивалент) со всеми таблицами
- [ ] **1.5.3** Индексы: по invoiceFingerprint, по sellerVatId, buyerVatId, по датам, по importSessionId
- [ ] **1.5.4** Уникальные ограничения где нужно (например, ruleKey в validation_rule_definition)

### 1.6 Seed-данные (опционально)
- [ ] **1.6.1** V2__seed_data.sql: тестовые организации, пользователи, базовые rule definitions
- [ ] **1.6.2** Либо `CommandLineRunner`/`ApplicationRunner` для dev-профиля

---

## Фаза 2: Auth и Security (Authentication & Authorization)

### 2.1 Базовая аутентификация
- [ ] **2.1.1** Сервис `UserDetailsService` (загрузка User по email)
- [ ] **2.1.2** Сервис `PasswordEncoder` (BCrypt)
- [ ] **2.1.3** DTO: `LoginRequest`, `LoginResponse` (JWT token)

### 2.2 JWT
- [ ] **2.2.1** Добавить `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (или spring-boot-starter-oauth2-resource-server)
- [ ] **2.2.2** Конфигурация: секрет, время жизни токена (application.yaml)
- [ ] **2.2.3** Сервис `JwtService` (generate, parse, validate)
- [ ] **2.2.4** `JwtAuthenticationFilter` (или аналогичный фильтр)
- [ ] **2.2.5** В JWT claims: userId, email, role, organizationId

### 2.3 Spring Security конфигурация
- [ ] **2.3.1** SecurityFilterChain:
  - `/api/auth/**` — permitAll
  - `/api/**` — authenticated
  - `/swagger-ui/**`, `/v3/api-docs/**` — permitAll (или защищённые, по желанию)
- [ ] **2.3.2** Method security: `@PreAuthorize` для ролей (ADMIN vs ACCOUNTANT)
- [ ] **2.3.3** Кастомный `AuthenticationPrincipal` для текущего User

### 2.4 Auth API
- [ ] **2.4.1** `POST /api/auth/login` → JWT
- [ ] **2.4.2** `POST /api/auth/register` (опционально, только для ADMIN или открыто в dev)

---

## Фаза 3: Каноническая модель (Canonical Model)

### 3.1 Доменные классы (in-memory, не JPA)
- [ ] **3.1.1** `CanonicalInvoice` (header, lines, totals)
- [ ] **3.1.2** `InvoiceHeader` (invoiceNumber, issueDate, dueDate, seller, buyer, currency)
- [ ] **3.1.3** `Party` (name, vatId, address)
- [ ] **3.1.4** `Address` (street, city, postalCode, country)
- [ ] **3.1.5** `InvoiceLine` (description, quantity, unitPrice, netAmount, vatAmount, grossAmount, vatRate)
- [ ] **3.1.6** `InvoiceTotals` (netTotal, vatTotal, grossTotal)
- [ ] **3.1.7** `MoneyAmount` (amount: BigDecimal, currency)
- [ ] **3.1.8** `VatRate` (enum или value object с процентом)
- [ ] **3.1.9** Immutability где возможно (records или final fields)

### 3.2 Модель валидации (in-memory)
- [ ] **3.2.1** `ValidationIssue` (record/class: severity, stage, ruleKey, message, fieldPath, code)
- [ ] **3.2.2** `ValidationContext` (накопление List&lt;ValidationIssue&gt;, методы addIssue, hasErrors)
- [ ] **3.2.3** Интерфейс `ValidationStage` (или `PipelineStage`): `void validate(ValidationContext ctx, CanonicalInvoice invoice)` — или с общим контекстом, включающим raw XML/DTO

---

## Фаза 4: Форматы и парсинг (Formats & Parsing)

### 4.1 Реестр форматов
- [ ] **4.1.1** Интерфейс `InvoiceFormat` (или `FormatDescriptor`): getFormatId(), getSupportedNamespaces(), getParser(), getMapper()
- [ ] **4.1.2** `InvoiceFormatDetector`: по root element, namespace определяет формат
- [ ] **4.1.3** `FormatRegistry` (map formatId → InvoiceFormat)

### 4.2 KSeF (Польша)
- [ ] **4.2.1** Изучить структуру KSeF XML (root, namespace, версии)
- [ ] **4.2.2** Создать Jackson XML аннотированные DTO (или JAXB) для KSeF:
  - Root-элемент, header, lines, totals
  - Минимальный набор полей для MVP
- [ ] **4.2.3** `KsefInvoiceParser` реализует `InvoiceParser<KsefInvoiceDto>`
- [ ] **4.2.4** `KsefToCanonicalMapper` реализует `InvoiceMapper<KsefInvoiceDto>`
- [ ] **4.2.5** Зарегистрировать KSeF в FormatRegistry
- [ ] **4.2.6** Создать тестовые XML fixture'ы (валидный, невалидный, edge cases)

### 4.3 XSD (опционально на MVP)
- [ ] **4.3.1** Найти/скачать XSD для KSeF (если публично доступна)
- [ ] **4.3.2** XSD Validation Stage — валидация XML по схеме до парсинга

---

## Фаза 5: Messaging (RabbitMQ)

### 5.1 Конфигурация RabbitMQ
- [ ] **5.1.1** В application.yaml: host, port, username, password (по профилю)
- [ ] **5.1.2** `@Configuration` класс: объявление Queue, Exchange, Binding
  - Очередь: `invoice.import`
  - DLQ: `invoice.import.dlq`
  - Retry policy (при необходимости — через spring-amqp или кастомно)

### 5.2 Контракты сообщений
- [ ] **5.2.1** DTO `InvoiceImportRequested` (importSessionId, uploadedByUserId, xmlPayload, optional metadata)
- [ ] **5.2.2** Сериализация/десериализация (Jackson JSON или XML — JSON предпочтительнее для очередей)

### 5.3 Publisher
- [ ] **5.3.1** `InvoiceImportPublisher` (или `ImportMessagePublisher`):
  - метод `publish(InvoiceImportRequested msg)`
  - отправка в `invoice.import`

### 5.4 Listener (Worker)
- [ ] **5.4.1** `InvoiceImportListener` с `@RabbitListener` на `invoice.import`
- [ ] **5.4.2** Десериализация сообщения
- [ ] **5.4.3** Заглушка обработки (пока логировать) — полная логика в Фазе 6–7
- [ ] **5.4.4** Обработка исключений → retry / send to DLQ

### 5.5 Профили api vs worker
- [ ] **5.5.1** `api` profile: listener отключён (`@Profile("worker")` на listener)
- [ ] **5.5.2** `worker` profile: listener включён, API может быть отключён или ограничен
- [ ] **5.5.3** Обновить docker-compose: два сервиса app (api и worker) с разными профилями

---

## Фаза 6: Pipeline валидации (Validation Pipeline)

### 6.1 Базовая инфраструктура pipeline
- [ ] **6.1.1** `ValidationPipeline` (список стадий, последовательное выполнение)
- [ ] **6.1.2** Расширенный `ValidationContext` (содержит: raw xml, format-specific DTO, CanonicalInvoice, issues, metadata)
- [ ] **6.1.3** Контракт стадии: `ValidationStage.process(ValidationContext)`

### 6.2 Стадия Parsing
- [ ] **6.2.1** `ParsingValidationStage`:
  - использует FormatDetector → Parser
  - при ошибке парсинга — addIssue(PARSING, ...)
  - при успехе — кладёт DTO в контекст

### 6.3 Стадия Mapping
- [ ] **6.3.1** `MappingValidationStage`:
  - DTO → CanonicalInvoice через Mapper
  - при ошибке маппинга — addIssue(MAPPING, ...)
  - при успехе — кладёт CanonicalInvoice в контекст

### 6.4 Стадия Technical Validation
- [ ] **6.4.1** `TechnicalValidationStage`:
  - обязательные поля (invoiceNumber, issueDate, seller.vatId, buyer.vatId, currency)
  - формат NIP/VAT (польский NIP — 10 цифр, checksum)
  - форматы дат
  - валидность валюты (ISO 4217)
  - длины строк, диапазоны
- [ ] **6.4.2** Вынести проверки в отдельные методы/классы (Single Responsibility)

### 6.5 Стадия Arithmetic Validation
- [ ] **6.5.1** `ArithmeticValidationStage`:
  - сумма lines[].netAmount ≈ totals.netTotal (с допуском, например 0.01)
  - сумма lines[].vatAmount ≈ totals.vatTotal
  - сумма lines[].grossAmount ≈ totals.grossTotal
  - для каждой линии: netAmount ≈ quantity * unitPrice (с округлением)
  - netAmount + vatAmount ≈ grossAmount
- [ ] **6.5.2** Утилита для сравнения BigDecimal с допуском

### 6.6 Стадия Business Rules (заглушка)
- [ ] **6.6.1** `BusinessRulesValidationStage`:
  - загрузка ValidationProfile (по умолчанию или из контекста)
  - вызов RuleEngine (пока пустой или один тестовый rule)
- [ ] **6.6.2** Полная реализация — в Фазе 7

### 6.7 Стадия Persistence
- [ ] **6.7.1** `PersistenceValidationStage`:
  - сохранение/обновление InvoiceMetadata (по fingerprint — update or insert)
  - создание ValidationRun
  - сохранение ValidationIssue для каждого issue
  - обновление ImportSession (invoicesProcessed, errorsCount, warningsCount)

### 6.8 Сборка pipeline
- [ ] Открыть XSD stage (если XSD есть)
- [ ] Parsing → Mapping → Technical → Arithmetic → Business → Persistence
- [ ] Обработка: при критических ошибках (PARSING, MAPPING) — ранний выход, Persistence не выполняется

---

## Фаза 7: Rules Engine (Движок правил)

### 7.1 Интерфейсы
- [ ] **7.1.1** `Rule` interface: `List<ValidationIssue> evaluate(CanonicalInvoice invoice, RuleConfig config)`
- [ ] **7.1.2** `RuleConfig` (params из paramsJson, severityOverride)
- [ ] **7.1.3** `RuleRegistry`: map ruleKey → Rule implementation
- [ ] **7.1.4** `RuleKey` — константы или enum для стабильных идентификаторов

### 7.2 Реализации правил
- [ ] **7.2.1** `DuplicateInvoiceRule`: проверка invoiceFingerprint в периоде (по issueDate ± N дней)
- [ ] **7.2.2** `AllowedCurrencyRule`: допустимые валюты из config
- [ ] **7.2.3** `AmountLimitRule`: лимит суммы (grossTotal &lt;= maxAmount из config)
- [ ] **7.2.4** Зарегистрировать в RuleRegistry

### 7.3 Интеграция с ValidationProfile
- [ ] **7.3.1** `BusinessRulesValidationStage` переписать:
  - загрузка ValidationProfile по id из контекста (или default)
  - загрузка ValidationProfileRule (enabled)
  - для каждого правила: RuleRegistry.get(ruleKey) → evaluate с RuleConfig из paramsJson
  - сбор всех ValidationIssue

### 7.4 Seed правил в БД
- [ ] **7.4.1** ValidationRuleDefinition для DuplicateInvoice, AllowedCurrency, AmountLimit
- [ ] **7.4.2** Дефолтный ValidationProfile с этими правилами

### 7.5 API управления профилями и правилами
- [ ] **7.5.1** `GET /api/validation-profiles`
- [ ] **7.5.2** `POST /api/validation-profiles`
- [ ] **7.5.3** `GET /api/validation-profiles/{id}`
- [ ] **7.5.4** `PUT /api/validation-profiles/{id}`
- [ ] **7.5.5** `GET /api/validation-profiles/{id}/rules`
- [ ] **7.5.6** `PUT /api/validation-profiles/{id}/rules/{ruleKey}` (enable/disable, params, severity)

### 7.6 Аудит
- [ ] **7.6.1** При изменении профиля/правила — запись в AuditEvent (entityType, entityId, action, userId, diffJson)
- [ ] **7.6.2** Опционально: `GET /api/audit?entityType=ValidationProfile&entityId=...`

---

## Фаза 8: Imports API и оркестрация

### 8.1 Import Session API
- [ ] **8.1.1** `POST /api/import-sessions`:
  - вход: multipart/form-data (1..N XML файлов)
  - опционально: validationProfileId в query/body
  - создание ImportSession (status RECEIVED)
  - для каждого файла: формирование InvoiceImportRequested, публикация в RabbitMQ
  - обновление status → QUEUED
  - возврат importSessionId
- [ ] **8.1.2** `GET /api/import-sessions/{id}`:
  - статус, createdBy, createdAt
  - invoicesTotal, invoicesProcessed, errorsCount, warningsCount
  - finishedAt

### 8.2 Связка Worker → Pipeline
- [ ] **8.2.1** В `InvoiceImportListener`:
  - вызов FormatDetector → Parser → Mapper (или через pipeline)
  - запуск ValidationPipeline с контекстом (xml, importSessionId, userId, profileId)
  - Pipeline сам делает Parsing, Mapping, ..., Persistence
- [ ] **8.2.2** Обновление ImportSession после каждого обработанного инвойса (атомарно в Persistence stage)
- [ ] **8.2.3** При завершении всех инвойсов сессии — обновить status на COMPLETED / FAILED_PARTIAL / FAILED

### 8.3 Invoice Fingerprint
- [ ] **8.3.1** Алгоритм: hash(invoiceNumber + issueDate + sellerVatId + buyerVatId + grossTotal + currency)
- [ ] **8.3.2** Использование в Persistence: проверка дубликата, в DuplicateInvoiceRule

---

## Фаза 9: Validation Results API

### 9.1 Invoices API
- [ ] **9.1.1** `GET /api/invoices/{invoiceId}` — метаданные инвойса (из InvoiceMetadata)
- [ ] **9.1.2** `GET /api/invoices/{invoiceId}/validation-runs` — список ValidationRun
- [ ] **9.1.3** Фильтрация по organizationId (multi-tenancy): только инвойсы из импортов пользователей своей организации

### 9.2 Validation Runs API
- [ ] **9.2.1** `GET /api/validation-runs/{runId}/issues` — список ValidationIssue
- [ ] **9.2.2** Опционально: `GET /api/validation-runs/{runId}` — полная информация о run

### 9.3 Пагинация и фильтры
- [ ] **9.3.1** `GET /api/import-sessions` — список сессий (пагинация, фильтр по userId, дате)
- [ ] **9.3.2** `GET /api/invoices` — список инвойсов (пагинация, фильтр по sessionId, sellerVatId, дате)

---

## Фаза 10: Reporting

### 10.1 Summary Report
- [ ] **10.1.1** `GET /api/reports/summary`:
  - query params: from, to, sellerVatId, buyerVatId, organizationId
  - агрегация: количество инвойсов, по статусам (OK/WARN/ERROR), количество ошибок/предупреждений
- [ ] **10.1.2** Repository-методы или @Query для агрегации
- [ ] **10.1.3** DTO: `ReportSummary`

### 10.2 Top Issues Report
- [ ] **10.2.1** `GET /api/reports/top-issues`:
  - query params: from, to, limit
  - топ ruleKey по количеству срабатываний (группировка ValidationIssue по ruleKey)
- [ ] **10.2.2** DTO: `TopIssueDto` (ruleKey, count, severity)

### 10.3 Специализированные индексы
- [ ] **10.3.1** Индексы для отчётов (по дате, sellerVatId, ruleKey в validation_issue)
- [ ] **10.3.2** При необходимости — материализованные представления (на потом)

---

## Фаза 11: Доработки, тесты, документация

### 11.1 Unit-тесты
- [ ] **11.1.1** Canonical model: тесты маппинга (KSeF DTO → CanonicalInvoice)
- [ ] **11.1.2** Validation stages: техническая, арифметическая, бизнес-правила
- [ ] **11.1.3** Rules: DuplicateInvoiceRule, AllowedCurrencyRule, AmountLimitRule
- [ ] **11.1.4** FormatDetector: определение KSeF по XML

### 11.2 Integration-тесты
- [ ] **11.2.1** Auth: login, JWT validation
- [ ] **11.2.2** Import API: POST import-sessions, GET status (с Testcontainers для Postgres/RabbitMQ или @MockBean)
- [ ] **11.2.3** Validation Pipeline: полный цикл от XML до сохранения (с тестовой БД)
- [ ] **11.2.4** RabbitMQ: публикация и потребление (интеграционный тест)

### 11.3 Обработка ошибок
- [ ] **11.3.1** Глобальный @ControllerAdvice для REST исключений
- [ ] **11.3.2** Стандартизированный формат ошибок (code, message, field)
- [ ] **11.3.3** Обработка разбора XML (MalformedXmlException и т.п.)

### 11.4 Swagger/OpenAPI
- [ ] **11.4.1** Аннотации @Operation, @ApiResponse на контроллерах
- [ ] **11.4.2** Описание DTO (@Schema)
- [ ] **11.4.3** Проверка доступности Swagger UI по /swagger-ui.html

### 11.5 README и документация
- [ ] **11.5.1** README: описание проекта, как запустить (docker-compose), основные endpoints
- [ ] **11.5.2** Краткое описание архитектуры (модули, pipeline, роли api/worker)
- [ ] **11.5.3** Postman collection (экспорт из Swagger или ручной)

---

## Фаза 12: Продвинутые доработки (опционально)

### 12.1 Масштабирование worker-ов
- [ ] Настройка concurrency у @RabbitListener
- [ ] Метрики (Micrometer): время обработки, количество сообщений
- [ ] Ограничение размера XML в сообщении (или переход на S3/minio для больших файлов)

### 12.2 Дополнительные форматы
- [ ] Исследование формата UBL (EU) или другой страны
- [ ] Реализация второго формата по образцу KSeF

### 12.3 Idempotency
- [ ] Проверка дубликатов сообщений RabbitMQ (messageId, idempotency key)
- [ ] Уникальные ограничения в БД для предотвращения двойной записи

### 12.4 Outbox pattern (если нужно)
- [ ] При необходимости — outbox таблица для надёжной доставки сообщений в RabbitMQ

---

## Порядок выполнения (краткая последовательность)

| # | Фаза | Зависимости |
|---|------|-------------|
| 1 | 0 — Инфраструктура | — |
| 2 | 1 — БД и сущности | 0 |
| 3 | 2 — Auth | 1 |
| 4 | 3 — Canonical модель | — |
| 5 | 4 — Форматы (KSeF) | 3 |
| 6 | 5 — Messaging | 0 |
| 7 | 6 — Validation Pipeline | 3, 4, 5 |
| 8 | 7 — Rules Engine | 1, 6 |
| 9 | 8 — Imports API | 2, 5, 6 |
| 10 | 9 — Results API | 1, 2 |
| 11 | 10 — Reporting | 1, 2 |
| 12 | 11 — Тесты, док-ция | все |

---

## Рекомендуемый порядок итераций

**Итерация 1 (MVP Core):** 0 → 1 → 3 → 4 (minimal KSeF) → 5 → 6 (до Persistence) → 8 (базовый импорт)
→ ручной тест: загрузить XML, получить importSessionId, проверить в БД.

**Итерация 2 (Auth + Full Pipeline):** 2 → 6 (полный pipeline) → 7 (базовые правила) → 9.

**Итерация 3 (Profiles + Reporting):** 7 (API профилей) → 10 → 11.

**Итерация 4 (Polish):** 11 (тесты, документация) → 12 (по необходимости).

---

*Документ создан как живой roadmap. Обновляйте чеклисты по мере выполнения.*

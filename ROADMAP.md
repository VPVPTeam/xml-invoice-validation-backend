# Roadmap разработки: XML Invoice Validation Backend

Детальный поэтапный план разработки backend-платформы для импорта и валидации электронных счетов-фактур (e-invoices) в формате XML.

---

## Легенда

- **[MVP]** — минимально необходимый объём для работающего MVP
- **[POLISH]** — улучшения и доводка после MVP
- **[OPT]** — опционально, по мере времени
- **Оценка**: S (small, 1–2 дня) / M (medium, 3–5 дней) / L (large, 1+ неделя)

---

## Фаза 0: Подготовка инфраструктуры (до начала разработки)

### 0.1 Структура проекта и зависимости
**Оценка: S**

1. **Структура пакетов (modular monolith)**:
   - `com.vpvpteam.xmlinvoicevalidationbackend.auth`
   - `com.vpvpteam.xmlinvoicevalidationbackend.imports`
   - `com.vpvpteam.xmlinvoicevalidationbackend.messaging`
   - `com.vpvpteam.xmlinvoicevalidationbackend.formats`
   - `com.vpvpteam.xmlinvoicevalidationbackend.canonical`
   - `com.vpvpteam.xmlinvoicevalidationbackend.validation`
   - `com.vpvpteam.xmlinvoicevalidationbackend.rules`
   - `com.vpvpteam.xmlinvoicevalidationbackend.reporting`
   - В каждом модуле: `api` (controllers), `domain` (entities, services), `infra` (repositories, config)

2. **Добавить в `pom.xml`**:
   - `spring-boot-starter-amqp` (RabbitMQ)
   - `jackson-dataformat-xml`
   - `springdoc-openapi-starter-webmvc-ui` (Swagger UI)
   - Исправить тестовые зависимости: `spring-boot-starter-test` (вместо разрозненных *-test)
   - Удалить некорректные артефакты типа `spring-boot-starter-data-jpa-test` (если есть)

3. **Профили Spring**:
   - `dev` — H2 или локальный PostgreSQL, локальный RabbitMQ
   - `docker` — подключение к контейнерам postgres/rabbitmq

### 0.2 Docker и окружение
**Оценка: S**

1. **`docker-compose.yml`**:
   - `postgres:16` — порт 5432, volume, переменные `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
   - `rabbitmq:3-management` — порты 5672 (AMQP), 15672 (management UI)
   - `app` — Spring Boot (пока один образ; позже — `api`/`worker`)

2. **`application-docker.yaml`** (или `application.yaml` с профилем):
   - `spring.datasource.url` → `jdbc:postgresql://postgres:5432/...`
   - `spring.rabbitmq.host` → `rabbitmq`
   - `spring.rabbitmq.port` → `5672`

3. **Проверка**: `docker-compose up -d` → приложение поднимается и подключается к БД и RabbitMQ.

---

## Фаза 1: База данных и доменные модели

### 1.1 Миграции и основные таблицы
**Оценка: M**

1. **Flyway** (или Liquibase):
   - Добавить зависимость `flywaydb` / `flyway-core`
   - Каталог миграций: `src/main/resources/db/migration/`

2. **Миграция V1** — базовые таблицы:
   - `organizations` — `id`, `name`, `created_at`
   - `users` — `id`, `email` (unique), `password_hash`, `role`, `organization_id`, `created_at`
   - `import_session` — `id`, `created_by_user_id`, `created_at`, `status`, `invoices_total`, `invoices_processed`, `errors_count`, `warnings_count`, `finished_at`
   - `invoice_metadata` — `id`, `import_session_id`, `format`, `format_version`, `invoice_number`, `issue_date`, `seller_vat_id`, `buyer_vat_id`, `currency`, `total_gross`, `invoice_fingerprint`, `created_at`
   - `validation_run` — `id`, `invoice_metadata_id`, `import_session_id`, `validation_profile_id`, `status`, `started_at`, `finished_at`, `duration_ms`
   - `validation_issue` — `id`, `validation_run_id`, `severity`, `stage`, `rule_key`, `message`, `field_path`, `code`
   - `validation_profile` — `id`, `name`, `owner_organization_id`, `status`, `created_at`
   - `validation_rule_definition` — `id`, `rule_key` (unique), `engine`, `severity_default`, `description`
   - `validation_profile_rule` — `profile_id`, `rule_key`, `enabled`, `severity_override`, `params_json` (jsonb)
   - `audit_event` — `id`, `entity_type`, `entity_id`, `action`, `user_id`, `diff_json` (jsonb), `created_at`

3. **Индексы**:
   - `invoice_metadata.invoice_fingerprint` (unique или для поиска дублей)
   - `invoice_metadata.import_session_id`, `issue_date`, `seller_vat_id`, `buyer_vat_id`
   - `validation_issue.validation_run_id`, `rule_key`
   - `validation_profile_rule(profile_id, rule_key)` — unique

### 1.2 JPA-сущности
**Оценка: M**

1. **Auth**:
   - `Organization`, `User` (с `@ManyToOne` к `Organization`)

2. **Imports**:
   - `ImportSession` (enum `ImportSessionStatus`: RECEIVED, QUEUED, PROCESSING, COMPLETED, FAILED_PARTIAL, FAILED)
   - `InvoiceMetadata`

3. **Validation**:
   - `ValidationRun` (enum `ValidationRunStatus`: OK, WARN, ERROR, FAILED_TECHNICAL)
   - `ValidationIssue` (enum `ValidationSeverity`, `ValidationStage`)

4. **Rules**:
   - `ValidationProfile`, `ValidationRuleDefinition`, `ValidationProfileRule`

5. **Audit**:
   - `AuditEvent`

---

## Фаза 2: Каноническая модель и модуль `canonical`

### 2.1 Канонические доменные классы (не JPA)
**Оценка: M**

1. **Классы**:
   - `CanonicalInvoice` — header, lines, totals
   - `InvoiceHeader` — invoiceNumber, issueDate, dueDate, seller, buyer, currency
   - `InvoiceLine` — description, quantity, unitPrice, netAmount, vatAmount, grossAmount, vatRate
   - `InvoiceTotals` — netTotal, vatTotal, grossTotal
   - `Party` — name, vatId, address (опционально)
   - `Address` — street, city, postalCode, country
   - `MoneyAmount` — amount (BigDecimal), currency
   - `VatRate` — enum (STANDARD, REDUCED, ZERO, EXEMPT) + optional percent

2. **Иммутабельность** — где уместно: `record` или `@Value` (Lombok), либо `class` с final-полями.

3. **Вспомогательные классы**:
   - `Currency` — использовать `java.util.Currency` или обёртку над кодом валюты (PLN, EUR и т.д.)

---

## Фаза 3: Модуль `auth`

### 3.1 Базовая аутентификация и авторизация
**Оценка: M**

1. **UserService**:
   - Регистрация (если включена; иначе — сид данных для dev)
   - Загрузка пользователя по email

2. **JWT**:
   - Зависимость `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (или spring-security-oauth2-resource-server)
   - `JwtTokenProvider` — генерация и парсинг токена
   - В токене: `sub` (userId), `email`, `organizationId`, `roles`

3. **Spring Security**:
   - `SecurityFilterChain` — разрешить `/api/auth/login`, `/api/auth/register`, `/swagger-ui/**`, `/v3/api-docs/**`
   - Остальные `/api/**` — аутентификация по JWT
   - `JwtAuthenticationFilter` — извлечение токена из `Authorization: Bearer ...`

4. **Auth API**:
   - `POST /api/auth/login` — email + password → JWT
   - `POST /api/auth/register` (опционально)

5. **Роли**:
   - `ADMIN`, `ACCOUNTANT` — через `@PreAuthorize` на эндпоинтах

6. **Сид данных** (для dev):
   - Создать организацию и пользователя-админа при старте (если БД пуста)

---

## Фаза 4: Модуль `formats` (KSeF)

### 4.1 Определение формата и парсинг
**Оценка: L**

1. **Интерфейсы**:
   - `InvoiceFormatDetector` — `Optional<InvoiceFormat> detect(String xml)` (root element, namespace)
   - `InvoiceParser<T>` — `T parse(String xml)` (формат-специфичный DTO)
   - `InvoiceMapper<T>` — `CanonicalInvoice toCanonical(T dto)`

2. **Реестр форматов**:
   - `FormatRegistry` — мапа `InvoiceFormat` → (Parser, Mapper)
   - Enum `InvoiceFormat`: KSEF (и позже DE, FR и т.д.)

3. **KSeF**:
   - Изучить структуру KSeF XML (FA, FA2 и т.д.), выбрать минимальный поднабор полей
   - Создать DTO-классы для Jackson XML: root element, header, lines, parties
   - `KsefFormatDetector` — по namespace / root
   - `KsefInvoiceParser` — парсинг в `KsefInvoiceDto`
   - `KsefInvoiceMapper` — маппинг в `CanonicalInvoice`
   - Тестовые fixture'ы: 1–2 валидных XML, 1–2 с типичными ошибками

4. **Обработка ошибок**:
   - При нераспознанном формате — возвращать понятную ошибку (stage = PARSING или MAPPING)

---

## Фаза 5: Модуль `messaging` (RabbitMQ)

### 5.1 Конфигурация RabbitMQ
**Оценка: S**

1. **Конфигурация**:
   - Очередь `invoice.import`
   - DLQ `invoice.import.dlq`
   - Exchange (direct или default)
   - Retry: maxAttempts=3, backoff

2. **Контракт сообщения**:
   - `InvoiceImportRequested` — `importSessionId`, `uploadedByUserId`, `xmlPayload` (String), `fileName` (опц.)

3. **Publisher**:
   - `InvoiceImportPublisher` — `publish(InvoiceImportRequested message)`

4. **Listener** (заглушка):
   - `InvoiceImportListener` — пока только логирует получение; в следующей фазе подключим pipeline

---

## Фаза 6: Модуль `imports` и API импорта

### 6.1 Import Session и оркестрация
**Оценка: M**

1. **ImportSessionService**:
   - `createImportSession(userId, files)` — создаёт session (RECEIVED), публикует N сообщений в RabbitMQ, переводит в QUEUED
   - Синхронная валидация: хотя бы проверить, что файлы — XML (по content-type или magic bytes)

2. **REST API**:
   - `POST /api/import-sessions` — multipart/form-data, 1..N XML файлов
   - Опционально: `validationProfileId` в query/body
   - Ответ: `{ "importSessionId": "...", "invoicesTotal": N }`

3. **GET /api/import-sessions/{id}`**:
   - Статус, createdBy, invoicesTotal, invoicesProcessed, errorsCount, warningsCount, finishedAt

4. **Организация**:
   - Фильтрация по `organizationId` пользователя (multi-tenancy)

---

## Фаза 7: Pipeline валидации

### 7.1 Контекст и структура pipeline
**Оценка: M**

1. **ValidationContext**:
   - `String xmlPayload`
   - `CanonicalInvoice canonicalInvoice` (nullable до mapping)
   - `List<ValidationIssue> issues`
   - `ImportSession session`, `InvoiceMetadata metadata` (для persistence)
   - Методы: `addIssue(...)`, `hasErrors()`, `hasWarnings()`

2. **ValidationIssue** (in-memory, не только JPA):
   - severity, stage, ruleKey, message, fieldPath, code

3. **ValidationPipeline**:
   - Интерфейс `ValidationStage` — `void validate(ValidationContext context)`
   - `ValidationPipeline` — список стадий, выполнение по порядку; при критических ошибках (например, PARSING failed) — early exit
   - Реализации стадий:
     - `XsdValidationStage` (если есть XSD для формата; для MVP можно заглушить)
     - `ParsingValidationStage` — вызов FormatDetector + Parser, запись ошибок в context
     - `MappingValidationStage` — вызов Mapper, запись ошибок
     - `TechnicalValidationStage` — required поля, NIP/VAT, даты, валюта
     - `ArithmeticValidationStage` — суммы, VAT, округление (BigDecimal, допустимая погрешность)
     - `BusinessRulesValidationStage` — выбор профиля, RuleRegistry, применение правил
     - `PersistenceValidationStage` — сохранение InvoiceMetadata, ValidationRun, ValidationIssue, обновление ImportSession

4. **Утилиты**:
   - Класс для работы с BigDecimal и округлением (например, `RoundingUtils` с HALF_UP, масштаб 2)
   - Допустимая погрешность для арифметики: например, 0.01

### 7.2 Technical и Arithmetic stages
**Оценка: M**

1. **TechnicalValidationStage**:
   - Обязательные поля: invoiceNumber, issueDate, seller.vatId, buyer.vatId, currency
   - NIP (Польша): 10 цифр, контрольная сумма
   - VAT ID: базовый формат (страна + номер)
   - Дата: не в будущем, разумный диапазон
   - Валюта: код из ISO 4217

2. **ArithmeticValidationStage**:
   - Сумма по линиям: sum(lines.netAmount) ≈ totals.netTotal
   - Аналогично vatTotal, grossTotal
   - Проверка на каждой линии: net + vat ≈ gross
   - UnitPrice * quantity ≈ netAmount (в пределах погрешности)

---

## Фаза 8: Модуль `rules`

### 8.1 Rule Engine и профили
**Оценка: M**

1. **RuleRegistry**:
   - `Map<String, Rule>` — ruleKey → реализация
   - Метод `apply(CanonicalInvoice, RuleConfig) → List<ValidationIssue>`

2. **Интерфейс Rule**:
   - `List<ValidationIssue> evaluate(CanonicalInvoice invoice, RuleConfig config)`

3. **RuleConfig**:
   - Параметры из `paramsJson` (Map или POJO)

4. **Базовые правила (Java)**:
   - `DuplicateInvoiceRule` — поиск по `invoiceFingerprint` в периоде (например, ±30 дней)
   - `AllowedCurrenciesRule` — список допустимых валют из config
   - `MaxAmountRule` — лимит суммы (опц.)

5. **ValidationProfileService**:
   - CRUD для профилей
   - Получение активного профиля по организации (или default)
   - Управление правилами профиля: enabled, severityOverride, paramsJson

6. **REST API**:
   - `GET/POST /api/validation-profiles`
   - `GET/PUT /api/validation-profiles/{id}`
   - `GET/PUT /api/validation-profiles/{id}/rules/{ruleKey}`

7. **Сид правил**:
   - При старте — создать `ValidationRuleDefinition` для каждого реализованного Rule
   - Создать default `ValidationProfile` с набором базовых правил

---

## Фаза 9: Интеграция Worker и полный цикл

### 9.1 Worker — обработка сообщений
**Оценка: M**

1. **InvoiceImportListener** (реализация):
   - Получить сообщение → `InvoiceImportRequested`
   - Определить формат → парсить → маппить
   - Вызвать `ValidationPipeline` с полным набором стадий
   - Persistence stage сохраняет всё в БД
   - Обновить счётчики `ImportSession` (invoicesProcessed, errorsCount, warningsCount)
   - По завершении батча — перевести session в COMPLETED / FAILED_PARTIAL
   - При необработанной ошибке — отправить в DLQ (retry exhausted)

2. **Idempotency**:
   - По `invoiceFingerprint` — проверить, не обработан ли уже в этой session (или в другой)
   - Логика: update or skip / merge для дублей

3. **Профили `api` и `worker`**:
   - `api` — включает REST controllers, не включает RabbitListener (или условно)
   - `worker` — включает RabbitListener, можно отключить часть HTTP (или оставить health)
   - Через `spring.profiles.include` или отдельные main-классы

### 9.2 API результатов валидации
**Оценка: S**

1. **REST**:
   - `GET /api/invoices/{invoiceId}` — метаданные (из `InvoiceMetadata`)
   - `GET /api/invoices/{invoiceId}/validation-runs` — список runs
   - `GET /api/validation-runs/{runId}/issues` — список `ValidationIssue`
   - Фильтрация по организации (multi-tenancy)

---

## Фаза 10: Модуль `reporting` и отчётное API

### 10.1 Отчётные эндпоинты
**Оценка: M**

1. **Summary**:
   - `GET /api/reports/summary?from=&to=&sellerVatId=&buyerVatId=`
   - Агрегаты: количество инвойсов, по статусам (OK/WARN/ERROR), суммы

2. **Top issues**:
   - `GET /api/reports/top-issues?from=&to=&limit=20`
   - Топ rule_key по количеству срабатываний (ERROR/WARNING)

3. **Специализированные запросы**:
   - Репозиторий с кастомными `@Query` (JPQL/Native) для агрегации
   - Индексы под частые фильтры (issue_date, seller_vat_id, rule_key)

---

## Фаза 11: Аудит и доводка

### 11.1 Аудит изменений
**Оценка: S** [POLISH]

1. **AuditEvent**:
   - При изменении `ValidationProfile`, `ValidationProfileRule` — писать `AuditEvent`
   - Поля: entity_type, entity_id, action (CREATE/UPDATE/DELETE), user_id, diff_json, created_at

2. **Сервис аудита**:
   - `AuditService.record(entityType, entityId, action, userId, oldState, newState)`
   - Diff в JSON (опционально — только изменённые поля)

### 11.2 Swagger / OpenAPI
**Оценка: S** [POLISH]

1. Настроить `springdoc-openapi`
2. Аннотации `@Operation`, `@ApiResponse` на контроллерах
3. Доступ: `/swagger-ui.html` (или `/swagger-ui/index.html`)

### 11.3 XSD-валидация (если применимо)
**Оценка: M** [OPT]

1. Найти/получить XSD для KSeF
2. Реализовать `XsdValidationStage` — валидация XML по схеме до парсинга
3. Ошибки XSD → stage = XSD, severity = ERROR

### 11.4 Тестирование и стабильность
**Оценка: M** [POLISH]

1. **Unit-тесты**:
   - Валидаторы (Technical, Arithmetic)
   - Rules (DuplicateInvoice, AllowedCurrencies)
   - Mapper KSeF → Canonical
   - Парсер KSeF

2. **Интеграционные тесты**:
   - `@SpringBootTest` — полный цикл: POST import → сообщение в тестовую очередь → обработка → проверка БД
   - Testcontainers для Postgres и RabbitMQ

3. **Исправление pom.xml**:
   - Корректные test-зависимости (`spring-boot-starter-test`)
   - Убрать несуществующие артефакты

---

## Сводная таблица фаз

| Фаза | Описание | Оценка |
|------|----------|--------|
| 0 | Инфраструктура, Docker, зависимости | S |
| 1 | БД, миграции, JPA-сущности | M |
| 2 | Каноническая модель | M |
| 3 | Auth (JWT, Spring Security) | M |
| 4 | Форматы (KSeF: detector, parser, mapper) | L |
| 5 | Messaging (RabbitMQ) | S |
| 6 | Imports API | M |
| 7 | Validation pipeline (все стадии) | M |
| 8 | Rules (RuleRegistry, профили, API) | M |
| 9 | Worker + полный цикл | M |
| 10 | Reporting API | M |
| 11 | Аудит, Swagger, тесты, XSD | M (частично OPT) |

**Общая оценка MVP (фазы 0–9)**: примерно 6–10 недель при работе вдвоём.  
**С учётом фазы 10–11**: +2–3 недели.

---

## Рекомендуемый порядок коммитов

1. Структура пакетов + зависимости + docker-compose
2. Flyway V1 + JPA entities
3. Canonical model
4. Auth module (JWT, Security, login API)
5. Formats: интерфейсы + KSeF (detector, parser, mapper)
6. Messaging: RabbitMQ config + publisher + listener (заглушка)
7. Imports API + оркестрация
8. Validation pipeline (stages 1–6)
9. Rules module + persistence stage
10. Worker implementation + full flow
11. Invoices & validation-runs API
12. Reporting API
13. Audit, Swagger, tests

---

## Риски и митигация

| Риск | Митигация |
|------|-----------|
| KSeF XML сложный | Начать с минимального подмножества полей, расширять постепенно |
| Дублирование при retry | Idempotency через `invoiceFingerprint`, проверка перед сохранением |
| Транзакции API + очередь | Создать session в одной транзакции; публикация — после commit; при сбое — помечать session FAILED |
| Производительность worker'ов | Ограничить concurrency (prefetch, consumer count); мониторить duration_ms |

---

*Документ создан как живой план. Обновляйте по мере продвижения проекта.*

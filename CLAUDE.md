# CLAUDE.md — XML Invoice Validation Backend

## Что это за проект

Бэкенд-система для валидации электронных фактур (XML). Принимает пакет XML-файлов через REST API, проводит техническую и бизнес-валидацию, сохраняет результаты в PostgreSQL.

## Стек

- Java 21, Spring Boot 4.0.1, Maven
- Spring Data JPA + PostgreSQL
- Jackson XML (tools.jackson.dataformat:jackson-dataformat-xml:3.1.0)
- Lombok 1.18.44
- Таблицы создаются вручную через SQL (НЕ Flyway, НЕ Liquibase)
- hibernate.ddl-auto: validate (Hibernate только проверяет схему)

Доменные модели (model/) отделены от JPA-сущностей (entity/).
Маппинг между ними — через ValidationPersistenceMapper.
Контроллер возвращает доменные модели, НЕ entity.

## Пакетная структура

```
com.vpvpteam.xmlinvoicevalidationbackend/
├── api/                          — HTTP-инфраструктура (общая для всех контроллеров)
│   ├── ApiExceptionHandler       — @RestControllerAdvice, ловит все исключения
│   ├── ListResponse<T>           — обёртка для списковых ответов (data, count, message)
│   └── exceptions/
│       └── ApiBadRequestException — 400
├── exceptions/                   — общие доменные исключения
│   ├── EntityNotFoundException   — 404
│   └── EntityAlreadyExistsException — 409
├── canonical/                    — внутренняя модель фактуры (формато-независимая)
│   ├── CanonicalInvoice          — header + lines + totals
│   ├── InvoiceHeader             — invoiceNumber, issueDate, seller, buyer
│   ├── Party                     — taxId, name, address
│   ├── Address                   — countryCode, addressLine1
│   ├── InvoiceLine               — lineNumber, product, quantity, prices
│   └── InvoiceTotals             — currencyCode, totalNet, totalTax, totalGross
├── formats/                      — парсинг конкретных XML-форматов
│   ├── InvoiceXmlDto             — маркерный интерфейс для всех DTO
│   └── ksef/
│       ├── KsefInvoiceParser     — XML → KsefInvoiceXmlDto (Jackson XmlMapper)
│       ├── KsefInvoiceMapper     — KsefInvoiceXmlDto → CanonicalInvoice
│       ├── dto/KsefInvoiceXmlDto — зеркало KSeF XML-структуры
│       └── exceptions/KsefMappingException
├── validation/
│   ├── controller/InvoiceValidationController — REST API эндпоинты
│   ├── service/
│   │   ├── ValidationService              — оркестратор валидации батча
│   │   ├── ValidationPersistenceService   — CRUD для сохранения (write)
│   │   └── ValidationQueryService         — чтение из БД (read-only)
│   ├── validator/
│   │   ├── TechnicalValidator<T>          — интерфейс (generic, для разных форматов)
│   │   ├── KsefTechnicalValidator         — реализация для KSeF
│   │   ├── TechnicalValidationOutput      — результат техвалидации (CanonicalInvoice + issues)
│   │   ├── BusinessValidator              — валидация по правилам из БД (один на все форматы)
│   │   ├── FieldValueExtractor            — извлекает значение из CanonicalInvoice по field_path
│   │   └── OperatorEvaluator              — сравнивает значения по оператору (EQUALS, IN, BETWEEN...)
│   ├── entity/                            — JPA-сущности (только для persistence)
│   ├── model/                             — доменные модели (для логики и API-ответов)
│   ├── repository/                        — Spring Data JPA интерфейсы
│   ├── mapper/ValidationPersistenceMapper — model ↔ entity конвертация
│   ├── message/                           — классы с human-readable сообщениями об ошибках
│   │   ├── TechnicalIssueMessages
│   │   ├── BusinessIssueMessages
│   │   └── DuplicateIssueMessages
│   ├── enums/                             — Severity, ValidationStage, RuleOperator
│   └── dao/ValidationBatchDao             — нативные SQL-запросы
└── util/                                  — ExceptionUtils, FieldCheck
```

## Поток валидации

```
POST /api/invoices/validate (multipart XML files)
│
▼
ValidationService.validateBatch()
│
├── 1. Создаёт ValidationBatch + ValidationOutput
├── 2. Для каждого XML-файла:
│     ├── Парсинг: KsefInvoiceParser → KsefInvoiceXmlDto
│     ├── Извлечение sellerTaxId, invoiceNumber
│     ├── Проверка дупликатов внутри батча (WARNING)
│     ├── Техническая валидация: KsefTechnicalValidator → TechnicalValidationOutput
│     └── Бизнес-валидация: BusinessValidator (если техника прошла)
├── 3. prepareFinalReport() — статус (OK/WARNING/ERROR), счётчики
└── 4. Сохранение в БД через ValidationPersistenceService
```

## Бизнес-валидация — как работает

Правила хранятся в таблице `business_rule`, привязаны к конкретному vendor.
Одно активное правило на одно поле (vendor_id + field_path = UNIQUE).

## База данных — PostgreSQL

Таблицы создаются ВРУЧНУЮ (не Flyway). Hibernate ddl-auto: validate.

```sql
CREATE TABLE validation_batch (
                                  id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                  batch_id   VARCHAR(36) NOT NULL UNIQUE,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE batch_vendor (
                              id        BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              validation_batch_id  BIGINT      NOT NULL,
                              vendor_id VARCHAR(50) NOT NULL,
                              CONSTRAINT fk_batch_vendor_batch
                                  FOREIGN KEY (validation_batch_id) REFERENCES validation_batch (id)
                                      ON DELETE CASCADE,
                              CONSTRAINT uq_batch_vendor UNIQUE (validation_batch_id, vendor_id)
);

CREATE TABLE batch_invoice (
                               id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               validation_batch_id   BIGINT       NOT NULL,
                               invoice_id VARCHAR(100) NOT NULL,
                               CONSTRAINT fk_batch_invoice_batch
                                   FOREIGN KEY (validation_batch_id) REFERENCES validation_batch (id)
                                       ON DELETE CASCADE,
                               CONSTRAINT uq_batch_invoice UNIQUE (validation_batch_id, invoice_id)
);

CREATE TABLE validation_output (
                                   id                   BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   validation_batch_id             BIGINT      NOT NULL UNIQUE,
                                   status               VARCHAR(20) NOT NULL DEFAULT 'OK',
                                   total_invoices       INT         NOT NULL DEFAULT 0,
                                   valid_invoices       INT         NOT NULL DEFAULT 0,
                                   invoices_with_issues INT         NOT NULL DEFAULT 0,
                                   duplicate_invoices   INT         NOT NULL DEFAULT 0,

                                   CONSTRAINT fk_output_batch
                                       FOREIGN KEY (validation_batch_id) REFERENCES validation_batch (id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT chk_status
                                       CHECK (status IN ('OK', 'WARNING', 'ERROR'))
);

CREATE TABLE validation_issue (
                                  id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                  validation_output_id      BIGINT       NOT NULL,
                                  file_name      VARCHAR(255),
                                  invoice_number VARCHAR(50),
                                  seller_tax_id  VARCHAR(50),
                                  severity       VARCHAR(20)  NOT NULL,
                                  stage          VARCHAR(20)  NOT NULL,
                                  rule_key       VARCHAR(100) NOT NULL,
                                  field_path     VARCHAR(100),

                                  CONSTRAINT fk_issue_output
                                      FOREIGN KEY (validation_output_id) REFERENCES validation_output (id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT chk_severity
                                      CHECK (severity IN ('ERROR', 'WARNING')),

                                  CONSTRAINT chk_stage
                                      CHECK (stage IN ('TECHNICAL', 'BUSINESS'))
);

CREATE INDEX idx_validation_issue_validation_output_id ON validation_issue (validation_output_id);

CREATE INDEX idx_batch_invoice_invoice_id ON batch_invoice (invoice_id);
CREATE INDEX idx_batch_vendor_vendor_id   ON batch_vendor  (vendor_id);

CREATE TABLE vendor (
                        id     BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        tax_id VARCHAR(50) NOT NULL UNIQUE,
                        name   VARCHAR(255)
);

CREATE TABLE business_rule (
                               id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               vendor_id      BIGINT       NOT NULL,
                               rule_key       VARCHAR(100) NOT NULL,
                               field_path     VARCHAR(100) NOT NULL,
                               operator       VARCHAR(30)  NOT NULL,
                               expected_value VARCHAR(255) NOT NULL,
                               created_by     VARCHAR(100) NOT NULL DEFAULT 'system',

                               CONSTRAINT fk_business_rule_vendor
                                   FOREIGN KEY (vendor_id) REFERENCES vendor (id)
                                       ON DELETE CASCADE,

                               CONSTRAINT uq_business_rule_vendor_field
                                   UNIQUE (vendor_id, field_path),

                               CONSTRAINT uq_business_rule_vendor_key
                                   UNIQUE (vendor_id, rule_key),

                               CONSTRAINT chk_business_rule_operator
                                   CHECK (operator IN (
                                                       'EQUALS',
                                                       'NOT_EQUALS',
                                                       'GREATER_THAN',
                                                       'LESS_THAN',
                                                       'BETWEEN',
                                                       'IN',
                                                       'NOT_IN',
                                                       'CONTAINS'
                                       ))
);
```

## Правила кода

### Общие правила написания кода
- При написании кода используй лучшие практики из книжек: "Чистый код" и "Чистая архитектура"

### SOLID
- **S**: Один класс/метод — одна ответственность. save() и update() — отдельные методы.
- **O**: TechnicalValidator<T> — новый формат = новый класс, без изменения существующего.
- **L**: Реализации TechnicalValidator / FormatProcessor взаимозаменяемы: тот же контракт validate/process, без ломания ожиданий вызывающего кода.
- **I**: Узкие интерфейсы (TechnicalValidator — один метод). Не объединять техвалидацию, бизнес-валидацию и CRUD в один интерфейс. Read (QueryService) и write (PersistenceService) разделены.
- **D**: Сервисы бросают доменные исключения (EntityNotFoundException), НЕ API-исключения. ApiExceptionHandler маппит их в HTTP-коды.

### Именование
- boolean: `vendorExists`, `isActive`, `hasErrors` (не `isExistingInDatabase`)
- Метод возвращает Optional: `find...` (findByTaxId)
- Метод бросает исключение если не найдено: `get...OrThrow` (getVendorEntityOrThrow)
- Метод-проверка: `check...` или `ensure...` (checkVendorNotExistsOrThrow)

### JPA
- Entity и доменные модели РАЗДЕЛЕНЫ. Entity — только для persistence, модели — для логики и API.
- MANAGED-объекты (загруженные из БД внутри @Transactional) обновляются автоматически (dirty checking). НЕ вызывать save() на MANAGED-объектах.
- Lombok: @Getter, @Setter, @NoArgsConstructor. НЕ @Data (плохо для JPA entity).

### Исключения
- api/exceptions/ — ошибки HTTP-слоя (ApiBadRequestException → 400)
- exceptions/ — общие доменные (EntityNotFoundException → 404, EntityAlreadyExistsException → 409)
- formats/ksef/exceptions/ — специфичные для формата
- Сервисный слой НЕ должен бросать API-исключения

### Сообщения об ошибках
- Human-readable сообщения вынесены в отдельные классы: TechnicalIssueMessages, BusinessIssueMessages, DuplicateIssueMessages
- Эти классы — final, private constructor, только static методы

### Общее
- Списковые GET-эндпоинты возвращают ListResponse<T> (data + count + message). Пустой результат — не ошибка, а message "No data found"
- Конкретный ресурс по ID — если не найден, бросаем EntityNotFoundException (404)
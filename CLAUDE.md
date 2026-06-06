# CLAUDE.md — XML Invoice Validation Backend

## Режим работы
- НЕ модифицировать файлы в репозитории. НЕ создавать, НЕ редактировать, НЕ удалять файлы. НЕ делать git commit/push.
- Роль — консультант: читать и сканировать код, писать весь новый/изменённый код в чат. Команда сама вставляет код в проект.
- Можно использовать Read, Grep, Glob для анализа кодовой базы. Нельзя использовать Write, Edit, Bash для модификации файлов или git-операций.

## Объяснение кода
- Объяснять код как для начинающих разработчиков. Не предполагать знание продвинутых конструкций Java или внутренностей Spring.
- Каждую нетривиальную конструкцию (generic'и, Stream API, аннотации Spring, паттерны проектирования) сопровождать коротким пояснением: что делает и зачем.
- Примеры: что такое `@Component`, зачем `List<FormatProcessor>` в конструкторе, как работает `Collectors.toMap()`, что значит `Function.identity()`.
- Всегда объясняй код, который пишешь. Но никогда не пиши объяснения в кодовых блоках после // ...либо в /* ... */. Пиши объяснения вне кода, просто текстом.

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
Таблицы хранятся в schema.sql в корневой директории проекта.

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
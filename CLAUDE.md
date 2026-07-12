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

## Проактивность и оспаривание решений
- Проактивно предлагай решения и улучшения, даже если прямо не просили — коротко, без навязывания.
- Если наше решение/подход выглядит неоптимальным — аргументированно оспаривай, а не соглашайся по умолчанию. Предлагай альтернативу.
- Для нетривиальных развилок разбирай варианты: плюсы/минусы каждого и почему выбран конкретный. Мы учимся разработке — объяснение выбора для нас ценнее готового ответа.
- Глубину соразмеряй весу решения: архитектурная развилка — подробно; мелочь с очевидным дефолтом — коротко, без воды.
- Не оспаривай ради оспаривания и не превращай каждый ответ в эссе. Если наш выбор нормальный — так и скажи и двигайся дальше.
- Всегда давай рекомендацию, а не просто список опций. Финальное слово за нами.

## Что это за проект

Бэкенд-система для валидации электронных фактур (XML). Принимает пакет XML-файлов через REST API, проводит техническую и бизнес-валидацию, сохраняет результаты в PostgreSQL.

## Стек

- Java 21, Spring Boot 4.0.1, Maven
- Spring Data JPA + PostgreSQL
- Jackson XML (tools.jackson.dataformat:jackson-dataformat-xml:3.1.0 — Jackson 3.x, НЕ com.fasterxml)
- Lombok 1.18.44
- Bean Validation (spring-boot-starter-validation)
- Таблицы создаются вручную через SQL (НЕ Flyway, НЕ Liquibase)
- hibernate.ddl-auto: validate (Hibernate только проверяет схему, таблицы не создаёт)
- Spring Security пока НЕ подключён (закомментирован в pom) — появится в Auth

Доменные модели (model/) отделены от JPA-сущностей (entity/).
Маппинг между ними — через ValidationPersistenceMapper.
Контроллер возвращает доменные модели, НЕ entity.

## Пакетная структура

```
com.vpvpteam.xmlinvoicevalidationbackend/
├── api/                          — HTTP-инфраструктура (общая для всех контроллеров)
│   ├── ApiExceptionHandler       — @RestControllerAdvice, ловит все исключения, маппит в HTTP
│   ├── ListResponse<T>           — обёртка списковых ответов (data, count, message)
│   └── exceptions/ApiBadRequestException — 400
├── exceptions/                   — общие доменные исключения
│   ├── EntityNotFoundException           — 404
│   ├── EntityAlreadyExistsException      — 409
│   ├── UnsupportedFieldPathException     — 400 (field_path нет в CanonicalFieldRegistry)
│   └── InvalidRuleExpectedValueException — 400 (expected_value не подходит оператору)
├── canonical/                    — внутренняя формато-независимая модель фактуры
│   ├── CanonicalInvoice          — header + lines + totals + optionalFields
│   ├── InvoiceHeader, Party, Address, InvoiceLine, InvoiceTotals, InvoiceOptionalFields
│   └── CanonicalFieldRegistry    — реестр field_path → экстрактор (ЕДИНСТВЕННЫЙ источник правды по полям)
├── formats/                      — парсинг конкретных XML-форматов
│   ├── InvoiceXmlDto             — маркерный интерфейс для всех DTO
│   ├── FormatProcessor           — интерфейс: getFormatName() + process() (формато-независимость)
│   └── ksef/
│       ├── KsefFormatProcessor   — реализация FormatProcessor для KSeF
│       ├── KsefInvoiceParser     — XML → KsefInvoiceXmlDto (Jackson XmlMapper)
│       ├── KsefInvoiceMapper     — KsefInvoiceXmlDto → CanonicalInvoice (чистый копировщик, не валидирует)
│       └── dto/KsefInvoiceXmlDto — зеркало KSeF XML-структуры
├── validation/
│   ├── controller/
│   │   ├── InvoiceValidationController — /api/validation (валидация + отчёты)
│   │   ├── VendorController            — /api/vendors (CRUD)
│   │   └── BusinessRuleController      — /api/rules (CRUD)
│   ├── service/
│   │   ├── ValidationService              — оркестратор валидации батча
│   │   ├── ValidationPersistenceService   — запись/CRUD (write)
│   │   └── ValidationQueryService         — чтение из БД (read-only)
│   ├── validator/
│   │   ├── technical/
│   │   │   ├── TechnicalValidator<T>       — интерфейс (generic, под разные форматы)
│   │   │   ├── KsefTechnicalValidator      — реализация для KSeF
│   │   │   └── TechnicalValidationOutput   — CanonicalInvoice + issues + sellerTaxId/invoiceNumber
│   │   └── business/
│   │       ├── BusinessValidator           — валидация по правилам из БД (один на все форматы)
│   │       ├── BusinessValidationOutput    — обёртка над списком issues
│   │       ├── FieldValueExtractor         — значение из CanonicalInvoice по field_path (делегирует реестру)
│   │       └── OperatorEvaluator           — сравнение по оператору (EQUALS, IN, BETWEEN...)
│   ├── entity/                            — JPA-сущности (только для persistence)
│   ├── model/                             — доменные модели (логика + API-ответы; BatchSummary, BusinessRuleSaveResult...)
│   ├── repository/                        — Spring Data JPA интерфейсы
│   ├── mapper/ValidationPersistenceMapper — model ↔ entity конвертация
│   ├── message/                           — human-readable сообщения (TechnicalIssueMessages, BusinessIssueMessages, DuplicateIssueMessages)
│   ├── enums/                             — Severity, ValidationStage, RuleOperator
│   └── dao/ValidationBatchDao             — нативные SQL-запросы (кросс-батч дубликаты)
└── util/                          — ExceptionUtils, FieldCheck
```

## Поток валидации

```
POST /api/validation/validate (multipart: XML-файлы + параметр format)
│
▼
ValidationService.validateBatch(files, format)
│
├── 1. Выбор FormatProcessor по format; создание ValidationBatch + ValidationOutput
├── 2. Для каждого XML-файла:
│     ├── Парсинг + техвалидация: FormatProcessor.process → TechnicalValidationOutput
│     ├── invoiceId = sellerTaxId|invoiceNumber
│     ├── Дубликат внутри батча (Set) → WARNING, continue
│     ├── Дубликат в прошлых батчах (ValidationBatchDao) → WARNING (БЕЗ continue)
│     ├── Если техника вернула ERROR → добавить issues, continue
│     └── Бизнес-валидация (только активные правила) → BusinessValidationOutput
├── 3. prepareFinalReport() — статус (OK/WARNING/ERROR) + счётчики
└── 4. Сохранение в БД через ValidationPersistenceService (entityManager.persist)
```

## Бизнес-валидация — как работает

Правила хранятся в таблице `business_rule`, привязаны к конкретному vendor.
Запускается ТОЛЬКО если техническая валидация прошла (нужен CanonicalInvoice).
Бизнес-issue всегда Severity.WARNING (не должна ронять фактуру).

Текущая модель: одно правило на поле у вендора (`vendor_id + field_path` — UNIQUE).
Целевая модель (несколько правил на поле + активация/версионирование, RuleGroup) — см. `devplan.md`.

## База данных — PostgreSQL

Таблицы создаются ВРУЧНУЮ (не Flyway). Hibernate ddl-auto: validate.
Таблицы хранятся в schema.sql в корневой директории проекта.
`validate` НЕ проверяет строго nullability — `@Column(nullable=false)` в entity держать синхронно с `NOT NULL` в БД руками.

## Планы развития и целевая архитектура

Роадмап, дизайн будущих фич (Auth, RuleGroup), а также технические грабли окружения
и наши доменные решения — в `devplan.md`. **Сверяться с ним при добавлении новых фич:**
учитывать будущие направления и не закладывать решения, которые придётся переделывать.
Ключевое: атомарный движок валидации держать переиспользуемым; auth — фундамент для
всех per-user фич, поэтому идёт первым.

## Правила кода

### Общие правила написания кода
- При написании кода используй лучшие практики из книжек: "Чистый код" и "Чистая архитектура"

### SOLID
- **S**: Один класс/метод — одна ответственность. save() и update() — отдельные методы.
- **O**: TechnicalValidator<T> / FormatProcessor — новый формат = новый класс, без изменения существующего.
- **L**: Реализации TechnicalValidator / FormatProcessor взаимозаменяемы: тот же контракт validate/process, без ломания ожиданий вызывающего кода.
- **I**: Узкие интерфейсы (TechnicalValidator — один метод). Не объединять техвалидацию, бизнес-валидацию и CRUD в один интерфейс. Read (QueryService) и write (PersistenceService) разделены.
- **D**: Сервисы бросают доменные исключения (EntityNotFoundException), НЕ API-исключения. ApiExceptionHandler маппит их в HTTP-коды.

### Именование
- boolean: `vendorExists`, `isActive`, `hasErrors` (не `isExistingInDatabase`)
- boolean-поле entity называть `active` (не `isActive`) — Lombok даст чистый `isActive()`
- Метод возвращает Optional: `find...` (findByTaxId)
- Метод бросает исключение если не найдено: `get...OrThrow` (getVendorEntityOrThrow)
- Метод-проверка: `check...` или `ensure...` (checkVendorNotExistsOrThrow)

### JPA
- Entity и доменные модели РАЗДЕЛЕНЫ. Entity — только для persistence, модели — для логики и API.
- MANAGED-объекты (загруженные из БД внутри @Transactional) обновляются автоматически (dirty checking). НЕ вызывать save() на MANAGED-объектах.
- Lombok: @Getter, @Setter, @NoArgsConstructor. НЕ @Data (плохо для JPA entity).
- Валидацию на входе держать в три слоя: @NotBlank (модель) + @Column(nullable=false) (entity) + NOT NULL (БД). @NotBlank строже — режет null/пусто/пробелы с 400.

### Исключения
- api/exceptions/ — ошибки HTTP-слоя (ApiBadRequestException → 400)
- exceptions/ — общие доменные (EntityNotFoundException → 404, EntityAlreadyExistsException → 409, UnsupportedFieldPathException / InvalidRuleExpectedValueException → 400)
- Сервисный слой НЕ должен бросать API-исключения
- handleUnexpected (500) ОБЯЗАН логировать исключение (log.error("...", ex)) — иначе ошибки немые

### Сообщения об ошибках
- Human-readable сообщения вынесены в отдельные классы: TechnicalIssueMessages, BusinessIssueMessages, DuplicateIssueMessages
- Эти классы — final, private constructor, только static методы
- message — одна чистая фраза: БЕЗ `\n`, БЕЗ префикса severity, БЕЗ дублирования полей, которые уже есть отдельно (severity/sellerTaxId/ruleKey/fieldPath). Форматирование — забота фронта.

### Общее
- Списковые/коллекционные GET-эндпоинты возвращают ListResponse<T> с record-DTO (data + count + message). Пустой результат — не ошибка, а message "No data found". НЕ отдавать сырые Map с динамическими ключами.
- POST/PUT возвращают созданный/изменённый ресурс; DELETE → 204 No Content.
- Конкретный ресурс по ID — если не найден, бросаем EntityNotFoundException (404)
- Уникальность проверять в коде (чистый 409 до похода в БД) + констрейнт БД как страховка
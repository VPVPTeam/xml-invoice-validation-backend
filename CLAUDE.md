# CLAUDE.md — XML Invoice Validation Backend

## Режим работы
- НЕ модифицировать файлы в репозитории. НЕ создавать, НЕ редактировать, НЕ удалять файлы. НЕ делать git commit/push.
- Роль — консультант: читать и сканировать код, писать весь новый/изменённый код в чат. Команда сама вставляет код в проект.
- Можно использовать Read, Grep, Glob для анализа кодовой базы. Нельзя использовать Write, Edit, Bash для модификации файлов или git-операций.

## Процесс работы

- **Git-поток:** ветка на этап → PR в `dev` → `dev` в `main`. Ветка следующего этапа отпочковывается от свежего `dev`. Префиксы: `feature/`, `test/`, `refactor/`.
- **При множественных правках отдавать класс целиком**, а не фрагменты — так меньше шансов собрать его неправильно. Одна точечная вставка — можно фрагментом, но с явным указанием, куда.
- **Пути к файлам указывать полностью**, без сокращений вида `.../ClassName.java`.
- **Сверяться с актуальным кодом, а не с памятью.** Перед утверждением о версии библиотеки, расположении класса, сигнатуре метода или содержимом файла — прочитать файл или проверить артефакт. Ошибки в этом месте дорого обходятся: неверно названный модуль или «управляемая» версия зависимости роняют сборку и съедают время.
- **Ожидаемые ответы для новых тестов брать из фактических ответов приложения**, а не выдумывать. Для новых или изменённых эндпоинтов команда прогоняет запрос в Postman и присылает реальный JSON. Для уже покрытых эндпоинтов источник правды — существующие тесты.
- Если тест упал — присылается полный вывод; в отчёте MockMvc поле `Body` показывает фактически отправленное тело запроса, это первое место для диагностики.

## Объяснение кода
- Объяснять код как для начинающих разработчиков. Не предполагать знание продвинутых конструкций Java или внутренностей Spring.
- Каждую нетривиальную конструкцию (generic'и, Stream API, аннотации Spring, паттерны проектирования, термины вроде «fixture» или «статический блок инициализации») сопровождать коротким пояснением: что делает и зачем.
- Всегда объяснять написанный код. Но никогда не писать объяснения внутри кодовых блоков — ни через `//`, ни через `/* */`. Пояснения идут отдельным текстом вне кода.
- Тон формальный, без панибратства и сленга.

## Проактивность и оспаривание решений
- Проактивно предлагай решения и улучшения, даже если прямо не просили — коротко, без навязывания.
- Если наше решение/подход выглядит неоптимальным — аргументированно оспаривай, а не соглашайся по умолчанию. Предлагай альтернативу.
- Для нетривиальных развилок разбирай варианты: плюсы/минусы каждого и почему выбран конкретный. Мы учимся разработке — объяснение выбора для нас ценнее готового ответа.
- Глубину соразмеряй весу решения: архитектурная развилка — подробно; мелочь с очевидным дефолтом — коротко, без воды.
- Не оспаривай ради оспаривания и не превращай каждый ответ в эссе. Если наш выбор нормальный — так и скажи и двигайся дальше.
- Всегда давай рекомендацию, а не просто список опций. Финальное слово за нами.

## Что это за проект

Бэкенд-система для валидации электронных фактур (XML). Принимает пакет XML-файлов через REST API, проводит техническую и бизнес-валидацию, сохраняет результаты в PostgreSQL. Доступ — по JWT, с ролями.

## Стек

- Java 21, Spring Boot 4.0.1, Maven
- Spring Data JPA + PostgreSQL 17, Hibernate 7.2
- Spring Security 7 + JWT (jjwt 0.13.0: `jjwt-api` / `jjwt-impl` / `jjwt-gson`)
- Jackson XML (`tools.jackson.dataformat:jackson-dataformat-xml:3.1.0` — Jackson 3.x, НЕ `com.fasterxml`)
- Lombok 1.18.44
- Bean Validation (spring-boot-starter-validation)
- Тесты: JUnit 5, MockMvc, Testcontainers 1.21.4 (версия задаётся свойством `testcontainers.version` — BOM Spring Boot её не покрывает)
- Таблицы создаются вручную через SQL (НЕ Flyway, НЕ Liquibase); `schema.sql` в корне
- `hibernate.ddl-auto: validate` — Hibernate только проверяет схему, таблицы не создаёт

Доменные модели (`model/`) отделены от JPA-сущностей (`entity/`).
Маппинг между ними — через мапперы. Контроллеры возвращают доменные модели, НЕ entity.

## Пакетная структура

```
com.vpvpteam.xmlinvoicevalidationbackend/
├── api/                          — HTTP-инфраструктура (общая)
│   ├── ApiExceptionHandler       — @RestControllerAdvice, все исключения → HTTP + ErrorResponse
│   ├── ListResponse<T>           — обёртка списковых ответов (data, count, message)
│   └── exceptions/ApiBadRequestException — 400
├── exceptions/                   — общие доменные исключения
│   ├── EntityNotFoundException           — 404
│   ├── EntityAlreadyExistsException      — 409
│   ├── UnsupportedFieldPathException     — 400 (+ список доступных field_path в сообщении)
│   └── InvalidRuleExpectedValueException — 400
├── auth/                         — аутентификация и пользователи
│   ├── controller/AuthController — /api/auth (login, me)
│   ├── controller/UserController — /api/users (создание, только ADMIN)
│   ├── dto/                      — LoginRequest, LoginResponse, CreateUserRequest
│   ├── entity/UserEntity         — таблица app_user
│   ├── enums/Role                — USER, ADMIN
│   ├── model/User                — доменная модель БЕЗ пароля и хеша
│   ├── mapper/UserMapper         — entity → безопасная модель
│   ├── repository/UserRepository
│   ├── service/UserService       — создание пользователя, хеширование
│   └── security/
│       ├── SecurityConfig            — фильтр-чейн, правила доступа, BCrypt, method security
│       ├── JwtService                — выпуск и проверка токена
│       ├── JwtAuthenticationFilter   — читает Bearer-заголовок, грузит юзера из БД
│       ├── UserPrincipal             — адаптер UserEntity → UserDetails
│       ├── AppUserDetailsService     — поиск пользователя по email
│       └── AdminSeeder               — создаёт первого администратора при старте
├── canonical/                    — внутренняя формато-независимая модель фактуры
│   ├── CanonicalInvoice, InvoiceHeader, Party, Address, InvoiceLine, InvoiceTotals
│   └── CanonicalFieldRegistry    — реестр field_path → экстрактор (ЕДИНСТВЕННЫЙ источник правды по полям)
├── formats/                      — парсинг конкретных XML-форматов
│   ├── InvoiceXmlDto             — маркерный интерфейс
│   ├── FormatProcessor           — getFormatName() + process() (формато-независимость)
│   └── ksef/                     — KsefFormatProcessor, KsefInvoiceParser, KsefInvoiceMapper, dto/
├── validation/
│   ├── controller/
│   │   ├── InvoiceValidationController — /api/validation (валидация + отчёты)
│   │   ├── VendorController            — /api/vendors (CRUD; изменяющие операции только ADMIN)
│   │   └── BusinessRuleController      — /api/rules (CRUD)
│   ├── service/
│   │   ├── ValidationService              — оркестратор валидации батча
│   │   ├── ValidationPersistenceService   — запись/CRUD (write)
│   │   └── ValidationQueryService         — чтение из БД (read-only)
│   ├── validator/technical/      — TechnicalValidator<T>, KsefTechnicalValidator, TechnicalValidationOutput
│   ├── validator/business/       — BusinessValidator, BusinessValidationOutput, FieldValueExtractor, OperatorEvaluator
│   ├── entity/                   — JPA-сущности
│   ├── model/                    — доменные модели (BatchSummary, BusinessRuleSaveResult...)
│   ├── repository/               — Spring Data JPA интерфейсы
│   ├── mapper/ValidationPersistenceMapper
│   ├── message/                  — TechnicalIssueMessages, BusinessIssueMessages, DuplicateIssueMessages
│   ├── enums/                    — Severity, ValidationStage, RuleOperator
│   └── dao/ValidationBatchDao    — нативные SQL-запросы (кросс-батч дубликаты)
└── util/                         — ExceptionUtils, FieldCheck
```

## Поток валидации

```
POST /api/validation/validate (multipart: XML-файлы + параметр format, Bearer-токен)
│
▼
ValidationService.validateBatch(files, format)
│
├── 1. Выбор FormatProcessor по format; создание ValidationBatch + ValidationOutput
├── 2. Для каждого XML-файла:
│     ├── Парсинг + техвалидация: FormatProcessor.process → TechnicalValidationOutput
│     │     └── ошибка разбора → issue TECH_XML_PARSE_ERROR, continue (батч не падает)
│     ├── invoiceId = sellerTaxId|invoiceNumber (при отсутствии номера продавца — UNKNOWN)
│     ├── Дубликат внутри батча (Set) → WARNING, continue
│     ├── Дубликат в прошлых батчах (ValidationBatchDao) → WARNING (БЕЗ continue)
│     ├── Если техника вернула ERROR → добавить issues, continue (бизнес НЕ запускается)
│     └── Бизнес-валидация → BusinessValidationOutput
├── 3. prepareFinalReport() — статус (OK/WARNING/ERROR) + счётчики
└── 4. Сохранение в БД через ValidationPersistenceService
```

## Аутентификация

JWT, один access-токен (~1 час), stateless. Вход — `POST /api/auth/login`, публичный;
всё остальное требует токена. Пароли — BCrypt. Роли `USER` / `ADMIN` одной колонкой.
Фильтр на каждом запросе подгружает пользователя из БД, поэтому деактивированный
не пройдёт даже с ещё живым токеном. Первый администратор создаётся сеятелем
из настроек при старте приложения.

## Бизнес-валидация

Правила хранятся в таблице `business_rule`, привязаны к вендору. Запускается ТОЛЬКО
если техническая валидация прошла (нужен `CanonicalInvoice`). Бизнес-issue всегда
`Severity.WARNING` — не должна ронять фактуру.

Текущая модель: одно правило на поле у вендора (`vendor_id + field_path` — UNIQUE).
Целевая модель (RuleGroup) — см. `DevPlan.md`.

## База данных

Таблицы создаются ВРУЧНУЮ (не Flyway), лежат в `schema.sql` в корне проекта.
`ddl-auto: validate` НЕ проверяет строго nullability — `@Column(nullable=false)`
в entity держать синхронно с `NOT NULL` в БД руками.

## Тесты

Интеграционные тесты (Testcontainers + PostgreSQL в Docker) покрывают все эндпоинты.
Устройство тестовой инфраструктуры, правила написания тестов и список того, что
покрывается юнит-тестами, — в `DevPlan.md`.

При изменении кода: тесты не должны зависеть от внутренней структуры классов,
поэтому рефакторинг их ломать не должен. Если сломались — сначала проверить,
не утверждает ли тест деталь реализации вместо поведения.

## Планы развития и целевая архитектура

Роадмап, порядок работ, дизайн будущих фич (RuleGroup), устройство тестов,
технические грабли окружения и наши доменные решения — в `DevPlan.md`.
**Сверяться с ним при добавлении новых фич.**

## Правила кода

### Общие
- Использовать лучшие практики из «Чистого кода» и «Чистой архитектуры».

### SOLID
- **S**: Один класс/метод — одна ответственность. `save()` и `update()` — отдельные методы.
- **O**: `TechnicalValidator<T>` / `FormatProcessor` — новый формат = новый класс, без изменения существующего.
- **L**: Реализации взаимозаменяемы: тот же контракт, без ломания ожиданий вызывающего кода.
- **I**: Узкие интерфейсы. Не объединять техвалидацию, бизнес-валидацию и CRUD. Read (QueryService) и write (PersistenceService) разделены.
- **D**: Сервисы бросают доменные исключения, НЕ API-исключения. `ApiExceptionHandler` маппит их в HTTP-коды.

### Именование
- boolean: `vendorExists`, `isActive`, `hasErrors`
- boolean-поле entity называть `active` (не `isActive`) — Lombok даст чистый `isActive()`
- Метод возвращает Optional: `find...`
- Метод бросает исключение если не найдено: `get...OrThrow`
- Метод-проверка: `check...` или `ensure...`

### JPA
- Entity и доменные модели РАЗДЕЛЕНЫ.
- MANAGED-объекты (загруженные внутри `@Transactional`) обновляются автоматически (dirty checking). НЕ вызывать `save()` на них.
- Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`. НЕ `@Data` (плохо для JPA entity).
- Бины с `@Transactional` / `@PreAuthorize` / `@Async` НЕ должны быть `final` — CGLIB создаёт прокси подклассом.
- Валидация в три слоя: `@NotBlank` (модель) + `@Column(nullable=false)` (entity) + `NOT NULL` (БД). Своё сообщение задавать у каждого ограничения, которое пользователь реально нарушает.

### Исключения
- `api/exceptions/` — ошибки HTTP-слоя; `exceptions/` — общие доменные.
- Сервисный слой НЕ должен бросать API-исключения.
- Все ошибки отдаются единым форматом — `ErrorResponse` с полем `message`.
- `handleUnexpected` (500) ОБЯЗАН логировать исключение (`log.error("...", ex)`).

### Сообщения об ошибках
- Human-readable сообщения вынесены в отдельные классы (`TechnicalIssueMessages`, `BusinessIssueMessages`, `DuplicateIssueMessages`): final, приватный конструктор, только статические методы.
- `message` — одна чистая фраза: БЕЗ `\n`, БЕЗ префикса severity, БЕЗ дублирования полей, которые есть отдельно. Форматирование — забота фронта.

### API
- Коллекционные GET-эндпоинты возвращают `ListResponse<T>` с record-DTO. Пустой результат — не ошибка. НЕ отдавать сырые `Map` с динамическими ключами.
- POST/PUT возвращают созданный/изменённый ресурс; DELETE → 204.
- Конкретный ресурс по ID не найден — `EntityNotFoundException` (404).
- Уникальность проверять в коде (чистый 409 до похода в БД) + констрейнт БД как страховка.
- Пароли: доменная модель наружу пароль и хеш НЕ отдаёт; ввод пароля — отдельный request-DTO.
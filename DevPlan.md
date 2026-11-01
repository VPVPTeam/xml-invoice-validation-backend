# Dev Plan — XML Invoice Validation Backend

Форвард-лукинг роадмап. **Сверяться перед добавлением любой новой фичи** —
чтобы архитектура закладывалась под будущие направления и не переделывалась.

## Подход к разработке (резюме-ориентированный)

Приоритет — фичи, которые **отдельной строкой идут в резюме и обсуждаются на
собеседовании**: тесты, Docker, CI/CD, облако, многопоточность, очереди сообщений.
Это переносимые технические навыки и инструменты, востребованные повсеместно.

Доменные фичи (RuleGroup, common/private правила и т.п.) marketable-навыком не
являются — их нельзя показать в CV или на собесе, поэтому они **отложены** в раздел
«ХЗ когда» и делаются только после основной, резюме-значимой части.

**Рабочий цикл для каждой фичи:**
1. Реализовать фичу.
2. Написать под неё тесты.
3. Смерджить в `dev`.
4. Убедиться, что pipeline зелёный и версия задеплоена.

## Статус

- **Аутентификация — сделана.** Пользователи, роли, JWT, вход, `/me`, создание
  пользователей администратором, ограничения доступа.
- **Глобальные (интеграционные) тесты — сделаны.** Testcontainers (PostgreSQL +
  Kafka в Docker). Покрыты все эндпоинты всех контроллеров.
- **Рефакторинг — сделан** (ветка `refactor/cleanup`). Итоги — ниже.
- **Точечные юнит-тесты — сделаны** (ветка `test/unit-tests`). Состав — ниже.
- **Docker — сделан** (ветка `feature/docker`).
- **CI/CD — сделан** (ветка `feature/ci`): GitHub Actions, сборка + весь набор
  тестов на push в `dev`/`main` и на каждый PR.
- **Cloud — сделан** (ветка `feature/cloud`): AWS — EC2 (Docker) + ECR + RDS,
  регион eu-central-1. Деплой ручной. Справка — ниже и в `docs/DEPLOY.md`.
- **Многопоточность — сделана** (ветка `feature/multithreading`).
- **Юнит-тест `BusinessValidator` — сделан** (ветка `test/business-validator`).
- **Kafka / асинхронная обработка — сделана локально** (ветка `feature/kafka`):
  приём батча → Kafka → консьюмер → отчёт; Claim Check; статусы батча;
  идемпотентность; тесты (весь набор — 215, все зелёные). Итог — ниже.
- **Текущий этап — деплой Kafka на EC2.** Последний шаг фичи и последняя
  резюме-значимая работа; после неё основная часть закрыта. Главная проблема —
  память `t3.micro` (см. «Дизайн: Cloud/AWS» → «Kafka на EC2»).

---

# Рефакторинг — принципы и итоги

## Принципы (пригодятся при следующем заходе)

- **Тесты — страховочная сеть.** Они не зависят от внутренней структуры классов,
  поэтому рефакторинг их ломать не должен. Если сломались — сначала проверить,
  не утверждает ли тест деталь реализации вместо поведения.
- **Прогон всех тестов после каждого шага**, а не в конце.
- **Отдельный коммит на каждый шаг** — неудачный шаг откатывается один.
- **Размер метода — симптом, а не диагноз.** Настоящий критерий — **смешение
  уровней абстракции** и **несколько причин для изменения**.
- **Абстракции выделять по необходимости, а не ради выделения.** Оправданные
  абстракции: `FormatProcessor` и `TechnicalValidator<T>` (несколько форматов).
- **Резать класс по независимым причинам для изменения, а не по объёму.**
- **Порядок работ:** сначала приводить в порядок содержимое строк, потом
  переставлять строки по методам.
- **Разделять IO и CPU.** Оптовая загрузка из базы отдельно, вычисления отдельно.
- **Разделять «вычислить» и «сохранить».** `ValidationService` только считает
  отчёт и возвращает его; сохраняет вызывающий (`BatchProcessingService` через
  `ValidationPersistenceService`). Так движок переиспользуем и не держит
  транзакцию во время долгой CPU-работы.

## Как теперь устроен код (итоги)

- **Идентичность фактуры — тип `InvoiceId`** (`sellerTaxId` + `invoiceNumber`,
  метод `value()`); `InvoiceId.NONE` для неопознанной фактуры.
- **`ValidationIssue` создаётся только фабриками** `technicalError` и
  `businessWarning` — они держат связку этап/severity.
- **Неизменяемые результаты с фабриками.** `ValidationOutput` (`of` / `restored`),
  `ValidationBatch` (только `restored` — идентичность батча всегда приходит из БД,
  её выдаёт приём), `TechnicalValidationOutput` (`success` / `failure`).
- **`BatchTotals`** — record со счётчиками и статическим `compute`.
- **`FormatProcessorRegistry`** (пакет `formats`) — единственное место, знающее
  список форматов: `getProcessorOrThrow`, `ensureFormatSupported`. Им пользуются
  приём (проверка формата → синхронный 400) и `ValidationService`.
- **`ValidationService.validate(batchId, createdAt, format, files)` — три фазы,
  ничего не сохраняет:**
  1. **параллельно** разобрать + технически провалидировать все файлы
     (`parallelStream`, чистый CPU) → список `TechnicalResult`;
  2. **оптом** прочитать из базы — **два запроса на весь батч** (кросс-батч
     дубликаты, бизнес-правила);
  3. **последовательно** собрать итог в `BatchAccumulator`;
     возвращает `ValidationOutput`. Не `@Transactional` намеренно.
- **`BusinessValidator` — чистая функция** (правила приходят списком).
- **`BatchAccumulator`** (package-private) — собирающий параметр;
  `toOutput(batchId, createdAt)` получает идентичность батча снаружи.
- **`TechnicalIssueCollector`**, **`RuleKeys`**, **`XmlFileDataReader`** — как раньше.
- **`XmlFileData(fileName, byte[] content)`** — хранит байты (поток читается один
  раз, а байты нужны дважды: в `batch_file` и парсеру); `xmlInputStream()` создаёт
  новый поток на каждый вызов.
- **Сервисы записи разделены по сущностям:** `ValidationPersistenceService`
  (`complete` — отчёт + `COMPLETED` в одной транзакции), `VendorPersistenceService`,
  `BusinessRulePersistenceService`, `BatchStatusService` (переходы статусов).
  Чтение — `ValidationQueryService`.
- **`PersistenceMapper`** — один маппер на слой персистентности.
- **Контроллеры не ловят `Exception`.**

---

## MVP+ — очередь работ

### 1. Точечные юнит-тесты — сделано

Покрыто стабильное ядро. Классы работают без Spring и без базы.

- **`OperatorEvaluatorTest`**, **`BatchTotalsTest`**, **`KsefTechnicalValidatorTest`**,
  **`FieldValueExtractorTest`**, **`XmlFileDataReaderTest`**, **`UserServiceTest`**,
  **`BusinessValidatorTest`** — как раньше.
- **`PersistenceMapperTest`** — полнота маппинга (включая `message` у нарушений),
  обратная ссылка `addIssue`, восстановление счётчиков «как сохранено».
- **Kafka-фича (Mockito):**
  - **`BatchProcessingServiceTest`** — батча нет → ничего; `COMPLETED`/`FAILED` →
    пропуск (идемпотентность, `@EnumSource`); `PENDING`/`PROCESSING` → порядок
    «в работе → ремонт → готово» (`InOrder`), файлы в порядке загрузки
    (`ArgumentCaptor`).
  - **`BatchValidationConsumerTest`** — падение обработки → `FAILED`.
  - **`BatchSubmittedProducerTest`** — отправка `batchId` ключом и значением;
    Kafka недоступна (`failedFuture`) → `FAILED`.

**Не покрываем сознательно:** `ValidationService` (держат интеграционные тесты),
маппинг правил (перепишет RuleGroup), классы сообщений, `RuleKeys`,
`CanonicalFieldRegistry`, записи и DTO.

**Осталось непокрытым осознанно:** ветка `TECH_CANONICAL_MAPPING_FAILED` в
`KsefTechnicalValidator` (нужен мок `final`-маппера).

### 2. Docker — сделано

- **Multi-stage `Dockerfile`:** сборка `jar` в `maven:3.9-eclipse-temurin-21`,
  запуск на `eclipse-temurin:21-jre` от непривилегированного пользователя.
- **`docker-compose.yml`:** `db` (PostgreSQL 17) + **`kafka`** (`apache/kafka:4.1.0`,
  KRaft, heap 256–512 МБ, healthcheck) + `app`. Приложение ходит к сервисам по
  именам (`db`, `kafka:29092`), не по `localhost`.
- **Локально у нас** PostgreSQL установлен на машине (не в Docker), а Kafka
  запускается только из compose: `docker compose up -d kafka`.
- **Схему накатывает база, а не приложение** (`/docker-entrypoint-initdb.d/`).

### 3. CI/CD — сделано

- **GitHub Actions**, `.github/workflows/ci.yml`: `build-and-test`
  (`mvn -B verify`) и `build-image` (гейт `needs: build-and-test`).
- **Триггеры:** push в `dev`/`main` и любой pull request.
- **Флейк (известный):** `ContainerFetchException: Can't get Docker image` — лимит
  анонимных загрузок Docker Hub на общих IP раннеров. Теперь тесты тянут **два**
  образа (`postgres`, `apache/kafka`) — флейк вероятнее. Лечение: **Re-run failed
  jobs**; насовсем — `docker/login-action` (бэклог, поднять в приоритете).

### 4. Cloud — сделано

EC2 (`invoice-server`, Docker) + ECR + RDS PostgreSQL 18. Деплой ручной. Детали —
«Дизайн: Cloud/AWS» и `docs/DEPLOY.md`. **Kafka на сервер ещё не вынесена.**

### 5. Многопоточность — сделано

- Три фазы: параллельный разбор → два оптовых запроса → последовательная сборка.
- Убран N+1: было `2×N` запросов на батч, стало **2**.
- Параллельность только там, где нет общего изменяемого состояния.
- На `t3.micro` ускорение от потоков скромное; главный выигрыш — устранение N+1.

### 6. Kafka / асинхронная обработка — сделано локально (ветка `feature/kafka`)

**Поток:**
1. `POST /api/validation/validate` → `BatchSubmissionService.submit` (одна
   транзакция): проверки (батч не пустой, формат поддерживается → иначе 400),
   `validation_batch` в `PENDING` + файлы в `batch_file`, публикация события
   `BatchSubmittedEvent`.
2. **После коммита** `BatchSubmittedProducer`
   (`@TransactionalEventListener(AFTER_COMMIT)`) отправляет `batchId` в топик
   `validation-batch-submitted` и **ждёт подтверждения** (`join`). Kafka
   недоступна → батч `FAILED` (`BatchStatusService.markFailed`, `REQUIRES_NEW`).
3. Клиенту — **`202 Accepted`** `{ batchId, status: PENDING, createdAt }`.
4. `BatchValidationConsumer` (`@KafkaListener`, группа `invoice-validation`) →
   `BatchProcessingService.process`: нет батча → лог и пропуск; `COMPLETED`/`FAILED`
   → пропуск; иначе `PROCESSING` → файлы из `batch_file` (по `id`, порядок загрузки)
   → `ValidationService.validate` → `ValidationPersistenceService.complete`
   (отчёт + `COMPLETED` одной транзакцией). Неожиданная ошибка → `FAILED`.
5. `GET /api/validation/batches/{batchId}/status` — статус;
   `GET /api/validation/report/{batchId}` — отчёт.

**Ключевые решения:**
- **Одно сообщение = один батч** (единица смысла — батч).
- **Claim Check:** в сообщении только `batchId`; файлы — в БД (`batch_file`,
  `BYTEA`). Сообщения крошечные, лимит размера Kafka (1 МБ) не мешает, файлы не
  теряются по retention, формат и время приёма консьюмер берёт из БД.
- **Байты, а не колонки:** битый XML тоже надо сохранить (консьюмер вернёт
  `TECH_XML_PARSE_ERROR`), колонки привязали бы таблицу к формату KSeF. `BYTEA`, а
  не `TEXT` — не трогаем кодировку файла.
- **Статусы батча (`BatchStatus`):** `PENDING → PROCESSING → COMPLETED | FAILED`.
  Отдельно от `Severity`: батч может быть `COMPLETED` с результатом `ERROR`.
  **`FAILED` = сломалась система**; битая фактура — это `COMPLETED` + `ERROR`.
- **At-least-once + идемпотентность:** сообщение может прийти повторно; консьюмер
  пропускает `COMPLETED`/`FAILED`, а `PENDING`/`PROCESSING` делает (заново).
- **Сначала коммит, потом Kafka** (dual write). Окно «упали между коммитом и
  отправкой» → батч навсегда `PENDING`; полностью закрывает Transactional Outbox
  (бэклог).
- **Топик назван по событию** (прошедшее время), ключ = значение = `batchId`,
  **1 партиция** (батчи по одному; параллельность — внутри батча), `acks: all`,
  String-сериализаторы (JSON не нужен).
- **Таймауты producer'а:** `max.block.ms 5000`, `request.timeout.ms 5000`,
  `delivery.timeout.ms 10000` — при упавшей Kafka HTTP ждёт ≤ ~10 с, не минуту.
- **Лимиты загрузки:** `max-file-size 1MB`, `max-request-size 20MB`; превышение →
  **413** (`HttpStatus.CONTENT_TOO_LARGE`).
- **`message` у нарушений теперь хранится в БД** (`validation_issue.message TEXT`):
  с асинхронностью отчёт отдаётся только из базы, без этого текст ошибок терялся.

**Осталось по фиче:** деплой Kafka на EC2 + обновить `docs/DEPLOY.md`.

## ХЗ когда (после резюме-части)

### В первую очередь
- **RuleGroup** (дизайн ниже) + юнит-тесты на новый `BusinessValidator`.
- Таблица правил в разрезе юзеров: common / private.

### Kafka — доработки
- **Чистка `batch_file` раз в 7 дней** (по `validation_batch.created_at`; отчёты
  остаются). Индекс `idx_batch_file_validation_batch_id` под это уже есть.
- **Dead Letter Topic** — куда уходят сообщения, которые не удалось обработать
  после N попыток (сейчас `DefaultErrorHandler` логирует и пропускает).
- **Transactional Outbox** — гарантия «запись в БД + сообщение» без окна потери.
- **`GET /report` для незавершённого батча** отвечает `404 Batch not found`, хотя
  батч есть. Отдавать понятный ответ («отчёт ещё не готов», статус).
- Ответ `202` собирается до попытки отправки → при упавшей Kafka тело говорит
  `PENDING`, а статус уже `FAILED`. Приемлемо (клиент опрашивает статус).

### Cloud / CI — доработки
- **Автодеплой при пуше** (GitHub Actions → ECR → redeploy на EC2).
- **Elastic IP**, **swap-файл на EC2** (нужен и для Kafka), **HTTPS + домен**,
  **секреты в SSM / Secrets Manager**, **Actuator + мониторинг**.
- **CI: `docker/login-action`** — теперь приоритетнее (два образа в тестах).
- **CI: бампнуть экшены** (Node 20 deprecation).
- **Mockito как агент** в `maven-surefire-plugin` — сейчас Mockito «самоприцепляется»
  (предупреждение `self-attaching`); в будущих JDK это запретят.

### Доп. фичи и «когда весь код готов»
- **Документация кода** — в самом конце.
- **Кастомные аннотации** — отложено.
- Полное покрытие юнит-тестами остальных классов; больше GET-эндпоинтов; логи
  (structured logging); refresh-токен; soft delete для Vendor.
- Мапперы → MapStruct (?) — при переходе `PersistenceMapper` разделить по сущностям.
- `PersistenceMapper.toEntity(ValidationBatch)` больше не используется в коде
  (только в `PersistenceMapperTest`) — удалить вместе с тестом.
- **Именование в `ValidationQueryService`:** `getFullReport`, `getVendorByTaxId`
  возвращают `Optional`, но названы `get...` (правило — `find...`). Новый метод уже
  `findBatchByBatchId`. Переименовать остальные.
- Счётчик нечитаемых файлов (`unreadableFiles`): файл с ошибкой разбора не попадает
  ни в один счётчик. Поведение зафиксировано тестами
  `BatchTotalsTest.compute_withIssueOnUnknownInvoice_ignoresIt` и
  `ValidationIntegrationTest.validate_withMalformedXml_completesBatchWithParseErrorIssue`.
- `FieldValueExtractor` ловит `NullPointerException` от лямбд реестра — осознанная
  «безопасная навигация».
- `JwtAuthenticationFilter.authenticate` глотает `UsernameNotFoundException` без лога.
- `show_sql: true` шумит в логах — вынести в dev-профиль.
- `open-in-view` включён и load-bearing (`getReport` маппит ленивые сущности вне
  транзакции). Не трогать, пока не перенесём маппинг чтения под `@Transactional`.
- **Оптовый `IN (:ids)` при огромных батчах** — при необходимости бить на пачки.

## Дизайн: RuleGroup (целевая модель)

- Rule = группа условий: `(vendor, name/key, is_active, combinator AND|OR, conditions[])`.
- Condition = `(field_path, operator, expected_value)` — нынешнее атомарное правило.
- Верхний уровень: все активные группы через AND; внутри группы — её комбинатор.
  **Один уровень вложенности.**
- Активация и версионирование — на уровне группы.
- **DRY: атомарный движок (`OperatorEvaluator`, `FieldValueExtractor`,
  `CanonicalFieldRegistry`) переиспользуется без изменений.**
- Репортинг: одно нарушение на упавшую группу.
- Овнер у RuleGroup; уникальность → `(owner, vendor_id, field_path)`.
- Переписываются `BusinessRulePersistenceService` и подготовка списка правил;
  `BusinessValidator` (чистая функция) можно не ломать.

## Дизайн: Auth (реализовано, справочно)

- `app_user`, роли одной колонкой, BCrypt; JWT access-токен, stateless.
- Первый администратор — `AdminSeeder` из конфигурации.
- Vendor: создание/изменение/удаление — только ADMIN.

## Дизайн: Cloud/AWS (реализовано, справочно)

Полная шпаргалка — `docs/DEPLOY.md`. Регион `eu-central-1`.

- **Аккаунт:** AWS **Free plan** (кредиты до 6 месяцев). **App Runner недоступен.**
- **RDS PostgreSQL 18** — `invoice-db`, `db.t4g.micro`, база
  `xml_invoice_validation_backend_db`. Схема заливается вручную через `psql`.
  **С фичей Kafka схема изменилась** (`validation_batch.format/status`,
  `batch_file`, `validation_issue.message`) — перед деплоем перекатить
  `schema.sql` на RDS (данные удалятся).
- **ECR** — репозиторий `xml-invoice-validation-backend`.
- **EC2** — `invoice-server`, `t3.micro` (**1 ГБ RAM**), Amazon Linux 2023, x86_64.
  IP меняется при Stop/Start. SG: 22 с двух IP, 8080 миру. IAM-роль `ec2-ecr-read`.
  Контейнер `invoice-app`, переменные из `app.env`, `--restart unless-stopped`.
- **Деплой (ручной):** `docker build --platform linux/amd64` → push в ECR →
  SSH → `pull` → пересоздать контейнер.

### Kafka на EC2 (следующий шаг)

Проблема: на 1 ГБ RAM уже живёт Spring Boot (впритык), а обычная Kafka — JVM с
кучей минимум ~256–512 МБ. Варианты:
- **`apache/kafka-native`** — Kafka, собранная GraalVM в нативный бинарь:
  стартует за доли секунды, потребляет в разы меньше памяти. **Фаворит.**
- **Swap-файл** — страховка от OOM в любом варианте.
- **`t3.small` (2 ГБ)** — за кредиты Free plan.
  Kafka и приложение на EC2 — в одной Docker-сети; приложению
  `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092`; наружу порт Kafka не открывать.

## Тестовая инфраструктура (как устроена)

- **Где лежат тесты.** Юнит-тест — в пакете тестируемого класса (package-private
  классы иначе не видны). Интеграционные — в пакете фичи. `AbstractIntegrationTest`
  — в корне тестового дерева.
- **Один PostgreSQL и одна Kafka на весь прогон** — стартуют параллельно в
  статическом блоке `AbstractIntegrationTest` (`Startables.deepStart(...).join()`).
  `@Testcontainers` / `@Container` не используются (гасят контейнер после класса).
- **Схема — единственный источник правды, корневой `schema.sql`:**
  `withCopyFileToContainer(MountableFile.forHostPath("schema.sql"),
  "/docker-entrypoint-initdb.d/01-schema.sql")`. Отдельной копии в
  `src/test/resources` больше нет (она отстала от основной и уронила все тесты).
- **Kafka:** `org.testcontainers.kafka.KafkaContainer("apache/kafka:4.1.0")` на
  случайном порту; адрес отдаётся Spring через `@DynamicPropertySource`
  (`spring.kafka.bootstrap-servers`). PostgreSQL — через `@ServiceConnection`.
- **Асинхронные тесты:** отправили → **Awaitility** ждёт завершения батча
  (опрос статуса каждые 200 мс, максимум 30 с; ждём «любой завершённый», потом
  проверяем `COMPLETED` — понятная ошибка вместо таймаута) → `GET /report`.
  Не `Thread.sleep` — он либо ждёт зря, либо мигает.
- **Правило: не оставлять работу в полёте.** Каждый тест, отправивший батч,
  дожидается его обработки — иначе фоновая обработка пересекается с `TRUNCATE`
  следующего теста.
- **Синтетические тестовые фактуры** в `src/test/resources/invoices/`.
- **Очистка между тестами:** `TRUNCATE` таблиц валидации (включая `batch_file`),
  правил и вендоров + удаление пользователей, кроме администратора. Сообщения в
  Kafka между тестами остаются — консьюмер их уже обработал (offset сдвинут).
- **Юнит-тесты Spring не поднимают:** `new` + настоящие дешёвые зависимости
  (`PersistenceMapper`, `FieldValueExtractor`, …) или моки. Mockito 5 мокает и
  `final`-классы.

### Правила написания тестов

- **Тесты не привязаны к внутренней структуре** — только к API и ответам.
- **Достаточность:** основной сценарий + все ошибки, которые код обрабатывает явно.
- **`401`:** один тест на контроллер. **`403`:** тест на каждый метод с `@PreAuthorize`.
- **Утверждения:** проверять итог сценария, а не всё подряд.
- **Record сравнивать целиком.**
- **Подготовка данных — через репозитории**, а не через API.
- **Порядок элементов не утверждать** — `containsInAnyOrder`.
- **DAMP важнее DRY.**
- **Тест, фиксирующий известную дыру, называть так, чтобы это было видно** — когда
  дыру закроют, он упадёт и напомнит о себе (так и вышло с `message`).
- **`eq(...)` не годится для объектов с массивами** (`XmlFileData`) — record
  сравнивает `byte[]` по ссылке; перехватывать `ArgumentCaptor` и сравнивать поля.

## Технические нюансы — окружение и фреймворк (грабли)

- **Spring Boot 4 / Spring MVC 7:** маршруты без трейлинг-слеша.
- **Jackson XML — `tools.jackson.*` (3.x), НЕ `com.fasterxml.jackson.*`.**
- **`@AutoConfigureMockMvc` в Boot 4** — модуль `spring-boot-webmvc-test`.
- **Kafka в Boot 4 — только через `spring-boot-starter-kafka`.** Одна библиотека
  `spring-kafka` без стартера автоконфигурацию не включает (`KafkaTemplate` не
  создастся).
- **Testcontainers не управляется BOM Spring Boot** — версия через
  `testcontainers.version` (модули `postgresql`, `kafka`).
- **Testcontainers требует запущенный Docker.** Ошибка `Could not find a valid
  Docker environment` + все интеграционные тесты `ignored` = Docker Desktop не
  запущен (проверка: `docker ps`).
- **`ddl-auto: validate`:** Hibernate таблицы не создаёт и nullability не проверяет.
  Локально PostgreSQL у нас **не в Docker** → схему накатывать вручную:
  `psql -v ON_ERROR_STOP=1 -h localhost -U postgres -d xml_invoice_validation_backend_db -f schema.sql`
  (в IntelliJ — выполнять **весь** файл, а не выражение под курсором).
- **Init-скрипты PostgreSQL в Docker выполняются один раз** (пустой том).
- **Hibernate явно вставляет `NULL`** для пустого поля → `DEFAULT` в БД **не
  срабатывает**. Значение по умолчанию дублировать в Java
  (`status = BatchStatus.PENDING`).
- **`@Lob byte[]` на PostgreSQL = Large Object (`oid`), не `BYTEA`.** Для `BYTEA` —
  обычный `byte[]` без `@Lob`.
- **PostgreSQL не создаёт индекс на внешний ключ сам** — создавать вручную.
- **`@ManyToOne` по умолчанию EAGER** — для файлов батча ставим `LAZY`.
- **`@TransactionalEventListener(AFTER_COMMIT)`** срабатывает, только если событие
  опубликовано **внутри** транзакции (иначе молча не вызывается) и только после
  успешного коммита (при откате — никогда).
- **Запись в БД из `AFTER_COMMIT`-слушателя — только `REQUIRES_NEW`**: старая
  транзакция уже закоммичена, правка через `REQUIRED` молча не сохранится.
- **`KafkaTemplate.send` асинхронный** (возвращает `CompletableFuture`). Ждём
  `join()`; работу с БД в callback'е не делать (это сетевой поток Kafka).
- **Таймауты producer'а по умолчанию огромные** (`max.block.ms` 60 с,
  `delivery.timeout.ms` 2 мин) — при синхронном ожидании ограничивать.
- **Kafka в Docker — два listener'а:** `EXTERNAL localhost:9092` (для приложения из
  IDE) и `INTERNAL kafka:29092` (для контейнеров). Advertised-адрес обязан быть
  достижим клиентом. В KRaft на одной ноде — `*_REPLICATION_FACTOR: 1`.
- **`auto-offset-reset` срабатывает только когда у группы нет offset'а.**
- **Не запускать консольный консьюмер с `--group invoice-validation`** — он съест
  offset'ы приложения. Для просмотра топика — без `--group`.
- **`MaxUploadSizeExceededException` → 413**, константа `HttpStatus.CONTENT_TOO_LARGE`
  (`PAYLOAD_TOO_LARGE` устарел).
- **Переменные окружения перекрывают `application.yaml`** (`SPRING_KAFKA_BOOTSTRAP_SERVERS`
  → `spring.kafka.bootstrap-servers`).
- **GitHub Actions:** push в feature-ветку workflow не запускает; проверка — через PR.
- **AWS:** образ для EC2 — `--platform linux/amd64`. App Runner закрыт.
- **Многопоточность:** `parallelStream` — только для чистого CPU без разделяемого
  изменяемого состояния; `toList()` сохраняет порядок.
- **N+1 при пакетной обработке:** оптом `WHERE ... IN (:ids)` + `JOIN FETCH`.
- **`TIMESTAMPTZ` в нативном запросе приходит как `Instant`.**
- **Порядок flush у Hibernate:** INSERT → UPDATE → DELETE.
- **Spring Data никогда не возвращает `null`** из методов по имени.
- **`@ExceptionHandler` принимает массив классов.**
- **`new BigDecimal(null)` → `NullPointerException`.**
- **Lombok + boolean:** поле `active`, не `isActive`.
- **AOP-прокси:** бины с `@Transactional` / `@PreAuthorize` / `@Async` не `final`.
  Слушатели `@TransactionalEventListener` / `@KafkaListener` прокси не требуют.
- **`@AllArgsConstructor` несовместим с `@PersistenceContext`-полем.**
- **Spring Security 6+:** `@PreAuthorize`-отказ → 403 через `ApiExceptionHandler`;
  для 401 нужен entry point.
- **JWT (jjwt 0.13):** `jjwt-gson`; секрет ≥ 32 символов.
- **`List.of` не принимает `null`-элементы.**
- **`MockitoExtension` в строгом режиме**; `@InjectMocks` подставляет только моки.
  Для `void` — `doThrow(...).when(mock).method(...)`.
- **Record с массивом:** `equals`/`hashCode` сравнивают массив по ссылке.
- **Локальная копия репозитория может отставать от ветки.**

## Доменные решения и конвенции

- **`invoiceId` = `sellerTaxId + "|" + invoiceNumber`** — только через `InvoiceId.value()`.
- **`CanonicalFieldRegistry` — единственный источник правды по `field_path`.**
- **Системные ключи правил — только в `RuleKeys`.**
- **Операторы:** `BETWEEN` = `;`, `IN`/`NOT_IN` = `,`; `OperatorEvaluator` никогда
  не бросает.
- **Двухэтапная валидация:** бизнес-этап только после успешной технической.
- **Обработка батча асинхронная:** приём → Kafka → консьюмер; внутри — три фазы.
- **Ошибка разбора XML не роняет батч** — нарушение `TECH_XML_PARSE_ERROR`, батч
  `COMPLETED` с результатом `ERROR`.
- **Пустой пакет и неизвестный формат — ошибки вызывающего, не отчёт.** Проверяются
  **при приёме** (`BatchSubmissionService`) → синхронный 400, до Kafka.
- **`BatchStatus` (жизненный цикл) ≠ `Severity` (качество результата).**
- **`FAILED` — только поломка системы** (Kafka недоступна при отправке, неожиданная
  ошибка обработки).
- **Severity бизнес-issue = WARNING**, никогда ERROR.
- **Дубликаты:** in-batch и cross-batch, оба WARNING; отдельная ось, в
  `invoicesWithIssues` не попадают.
- **Счётчики `ValidationOutput`** — от уникальных фактур; инвариант
  `validInvoices + invoicesWithIssues = totalInvoices`.
- **`ValidationBatch.createdAt` — момент приёма батча** (выдаётся при `submit`).
- **`ValidationIssue.message`** — одна чистая фраза; хранится в БД.
- **API-ответы:** коллекции → `ListResponse<T>`; POST/PUT возвращают ресурс;
  DELETE → 204; асинхронный приём → **202** с квитанцией; ошибки — `ErrorResponse`.
- **Веб-слой не пропускает свои типы вглубь:** `MultipartFile` дальше
  `XmlFileDataReader` не идёт.
- **Контроллеры не перехватывают `Exception`.** Широкий `catch (RuntimeException)`
  допустим только на границе с внешней системой (producer/консьюмер Kafka), где
  любая ошибка ведёт к одному действию — лог + `FAILED`.
- **Магические числа-смещения не хардкодить.**
- **Тестовые данные — синтетические.**
- **Уникальность проверяем в коде** + констрейнты БД как страховка.
- **Пароли — только хеш (BCrypt).**
- **`handleUnexpected` обязан логировать** исключение.
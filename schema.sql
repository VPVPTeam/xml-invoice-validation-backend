TRUNCATE TABLE
    validation_issue,
    validation_output,
    batch_invoice,
    batch_vendor,
    validation_batch,
    business_rule,
    vendor
    RESTART IDENTITY CASCADE;

DROP TABLE IF EXISTS validation_issue;
DROP TABLE IF EXISTS validation_output;
DROP TABLE IF EXISTS batch_invoice;
DROP TABLE IF EXISTS batch_vendor;
DROP TABLE IF EXISTS validation_batch;
DROP TABLE IF EXISTS business_rule;
DROP TABLE IF EXISTS vendor;
DROP TABLE IF EXISTS app_user;

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
                        name   VARCHAR(255) NOT NULL
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

CREATE TABLE app_user (
                          id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          email         VARCHAR(255) NOT NULL UNIQUE,
                          password_hash VARCHAR(255) NOT NULL,
                          role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
                          is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                          created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

                          CONSTRAINT chk_app_user_role CHECK (role IN ('USER', 'ADMIN'))
);

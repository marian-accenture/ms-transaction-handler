-- Banking schema: tables and indexes for App 1 - Ingestion Service

CREATE SCHEMA IF NOT EXISTS banking;

-- -----------------------------------------------------------------------
-- accounts
-- -----------------------------------------------------------------------
CREATE TABLE banking.accounts (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number   VARCHAR(20)  UNIQUE,
    cbu              VARCHAR(22)  NOT NULL UNIQUE,
    cuit             VARCHAR(11)  NOT NULL UNIQUE,
    holder_name      VARCHAR(40)  NOT NULL,
    holder_type      VARCHAR(10),
    email            VARCHAR(100),
    phone            VARCHAR(30),
    bank_code        VARCHAR(20),
    branch_code      VARCHAR(10),
    default_currency CHAR(3),
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------
-- ingested_files
-- -----------------------------------------------------------------------
CREATE TABLE banking.ingested_files (
    id               UUID         PRIMARY KEY,
    file_name        VARCHAR(255) NOT NULL,
    file_format      VARCHAR(10)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    checksum         VARCHAR(64)  NOT NULL UNIQUE,
    file_size_bytes  BIGINT,
    total_rows       INT,
    success_rows     INT,
    failed_rows      INT,
    uploaded_by      VARCHAR(30),
    uploaded_at      TIMESTAMPTZ,
    processed_at     TIMESTAMPTZ
);

-- -----------------------------------------------------------------------
-- transactions
-- -----------------------------------------------------------------------
CREATE TABLE banking.transactions (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    external_ref     VARCHAR(30)    NOT NULL UNIQUE,
    transaction_at   TIMESTAMPTZ    NOT NULL,
    ingested_at      TIMESTAMPTZ    NOT NULL,
    type             VARCHAR(10)    NOT NULL,
    status           VARCHAR(10)    NOT NULL,
    amount           NUMERIC(19, 0) NOT NULL,
    currency         CHAR(3)        NOT NULL,
    benefactor_id    UUID           NOT NULL REFERENCES banking.accounts(id),
    beneficiary_id   UUID           NOT NULL REFERENCES banking.accounts(id),
    description      TEXT,
    file_id          UUID           NOT NULL REFERENCES banking.ingested_files(id),
    created_by       VARCHAR(30),
    flagged          BOOLEAN        NOT NULL DEFAULT false,
    flag_reason      TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------
-- transaction_validation_warnings
-- -----------------------------------------------------------------------
CREATE TABLE banking.transaction_validation_warnings (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID        REFERENCES banking.transactions(id),
    warning_code   VARCHAR(50) NOT NULL,
    warning_message TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------
-- Indexes
-- -----------------------------------------------------------------------

-- App 2: bilateral CBU search
CREATE INDEX idx_tx_benefactor_id  ON banking.transactions(benefactor_id);
CREATE INDEX idx_tx_beneficiary_id ON banking.transactions(beneficiary_id);
CREATE INDEX idx_accounts_cbu      ON banking.accounts(cbu);
CREATE INDEX idx_accounts_cuit     ON banking.accounts(cuit);

-- App 2: date range queries
CREATE INDEX idx_tx_transaction_at ON banking.transactions(transaction_at);
CREATE INDEX idx_tx_ingested_at    ON banking.transactions(ingested_at);

-- App 2: file filter
CREATE INDEX idx_tx_file_id        ON banking.transactions(file_id);

-- Flagged filter (partial index)
CREATE INDEX idx_tx_flagged        ON banking.transactions(flagged) WHERE flagged = true;

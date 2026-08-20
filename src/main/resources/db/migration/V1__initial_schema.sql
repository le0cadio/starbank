CREATE TABLE invoices (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    recipient_name VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    recipient_cpf VARCHAR(14) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    fee BIGINT NULL,
    integrity_hash VARCHAR(64) NULL
);

CREATE INDEX idx_invoice_status ON invoices (status);
CREATE INDEX idx_invoice_created_at ON invoices (created_at);

CREATE TABLE transfers (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    source_invoice_id VARCHAR(36) NOT NULL,
    recipient_bank_code VARCHAR(20) NOT NULL,
    recipient_branch VARCHAR(20) NOT NULL,
    recipient_account VARCHAR(30) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    recipient_tax_id VARCHAR(14) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL
);

CREATE INDEX idx_transfer_invoice ON transfers (source_invoice_id);
CREATE INDEX idx_transfer_status ON transfers (status);

CREATE TABLE webhook_events (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    invoice_id VARCHAR(36) NOT NULL,
    payload CLOB NOT NULL,
    status VARCHAR(20) NOT NULL,
    outcome VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    rejection_reason VARCHAR(255) NULL,
    CONSTRAINT uq_webhook_invoice_id UNIQUE (invoice_id)
);

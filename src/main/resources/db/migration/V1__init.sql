CREATE TABLE wallet_transaction (
                                    id               UUID PRIMARY KEY,
                                    reference        VARCHAR(64)   NOT NULL UNIQUE,
                                    user_id          UUID          NOT NULL,
                                    idempotency_key  VARCHAR(64)   NOT NULL,
                                    type             VARCHAR(10)   NOT NULL CHECK (type IN ('TOPUP','WITHDRAW')),
                                    amount           NUMERIC(19,2) NOT NULL CHECK (amount > 0),
                                    fee              NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (fee >= 0),
                                    status           VARCHAR(10)   NOT NULL CHECK (status IN ('PENDING','SUCCESS','FAILED','REVERSED')),
                                    step             VARCHAR(30)   NOT NULL,
                                    failure_reason   VARCHAR(40),
                                    bank_account     CHAR(12)      NOT NULL,
                                    request_hash     CHAR(64)      NOT NULL,
                                    version          BIGINT        NOT NULL DEFAULT 0,
                                    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                    UNIQUE (user_id, idempotency_key)
);
CREATE INDEX idx_tx_user_date ON wallet_transaction(user_id, created_at DESC);
CREATE INDEX idx_tx_pending   ON wallet_transaction(status, updated_at) WHERE status = 'PENDING';

CREATE TABLE transaction_step_log (
                                      id              BIGSERIAL PRIMARY KEY,
                                      transaction_id  UUID        NOT NULL REFERENCES wallet_transaction(id),
                                      step            VARCHAR(30) NOT NULL,
                                      outcome         VARCHAR(20) NOT NULL,
                                      detail          TEXT,
                                      created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_step_log_tx ON transaction_step_log(transaction_id, created_at);
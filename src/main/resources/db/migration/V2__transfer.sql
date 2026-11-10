ALTER TABLE wallet_transaction DROP CONSTRAINT IF EXISTS wallet_transaction_type_check;
ALTER TABLE wallet_transaction
    ADD CONSTRAINT wallet_transaction_type_check
        CHECK (type IN ('TOPUP', 'WITHDRAW', 'TRANSFER'));

ALTER TABLE wallet_transaction ALTER COLUMN bank_account DROP NOT NULL;

ALTER TABLE wallet_transaction
    ADD COLUMN IF NOT EXISTS counterparty_user_id UUID NULL;

ALTER TABLE wallet_transaction
    ADD COLUMN IF NOT EXISTS counterparty_phone VARCHAR(20) NULL;

CREATE INDEX IF NOT EXISTS idx_tx_counterparty_date
    ON wallet_transaction (counterparty_user_id, created_at DESC)
    WHERE counterparty_user_id IS NOT NULL;

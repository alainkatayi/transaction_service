ALTER TABLE wallet_transaction
    ADD COLUMN IF NOT EXISTS sender_phone VARCHAR(20) NULL;

ALTER TABLE wallet_transaction
    ADD COLUMN IF NOT EXISTS sender_name VARCHAR(201) NULL;

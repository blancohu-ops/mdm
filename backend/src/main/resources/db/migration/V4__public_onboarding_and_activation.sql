ALTER TABLE enterprise_submission_records
    ALTER COLUMN submitted_by DROP NOT NULL;

CREATE TABLE IF NOT EXISTS account_activation_tokens (
    id uuid PRIMARY KEY,
    enterprise_id uuid NOT NULL REFERENCES enterprises(id),
    account varchar(128) NOT NULL,
    phone varchar(32) NOT NULL,
    email varchar(128) NOT NULL,
    token_value varchar(128) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at timestamptz NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_account_activation_tokens_enterprise_id
    ON account_activation_tokens(enterprise_id);

CREATE INDEX IF NOT EXISTS idx_account_activation_tokens_expires_at
    ON account_activation_tokens(expires_at);

CREATE TABLE coupons (
    code VARCHAR(32) PRIMARY KEY,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('PERCENT', 'FIXED')),
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    minimum NUMERIC(12,2) NOT NULL CHECK (minimum >= 0),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (ends_at > starts_at),
    CHECK (kind <> 'PERCENT' OR amount <= 100)
);

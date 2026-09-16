CREATE TABLE carts (
    user_id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    coupon VARCHAR(32),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE cart_items (
    user_id UUID NOT NULL REFERENCES carts(user_id),
    product_id UUID NOT NULL,
    sku VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    PRIMARY KEY (user_id, sku)
);
CREATE TABLE cart_commands (
    user_id UUID NOT NULL REFERENCES carts(user_id),
    command_id UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, command_id)
);

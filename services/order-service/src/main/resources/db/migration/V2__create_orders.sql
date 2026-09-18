CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('CREATED','PAYMENT_PENDING','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED','REFUNDED')),
    subtotal NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    discount NUMERIC(12,2) NOT NULL CHECK (discount >= 0),
    shipping NUMERIC(12,2) NOT NULL CHECK (shipping >= 0),
    total NUMERIC(12,2) NOT NULL CHECK (total >= 0),
    currency VARCHAR(3) NOT NULL,
    coupon VARCHAR(32),
    reservation_id UUID,
    failure_code VARCHAR(64),
    correlation_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE order_items (
    order_id UUID NOT NULL REFERENCES orders(id), position INTEGER NOT NULL, product_id UUID NOT NULL,
    sku VARCHAR(64) NOT NULL, name VARCHAR(160) NOT NULL, slug VARCHAR(180) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0), unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    line_total NUMERIC(12,2) NOT NULL CHECK (line_total >= 0), PRIMARY KEY (order_id, position), UNIQUE (order_id, sku)
);
CREATE TABLE checkout_commands (
    customer_id UUID NOT NULL, command_id UUID NOT NULL, request_hash VARCHAR(64) NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id), created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (customer_id, command_id)
);
CREATE INDEX idx_orders_customer_created ON orders(customer_id, created_at DESC, id);
CREATE INDEX idx_orders_status_created ON orders(status, created_at DESC, id);

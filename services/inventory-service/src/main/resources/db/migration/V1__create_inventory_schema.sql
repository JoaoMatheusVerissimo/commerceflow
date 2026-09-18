CREATE TABLE stock_items (
    sku VARCHAR(64) PRIMARY KEY,
    physical INTEGER NOT NULL CHECK (physical >= 0),
    reserved INTEGER NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    minimum INTEGER NOT NULL DEFAULT 0 CHECK (minimum >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (reserved <= physical)
);
CREATE TABLE reservations (
    order_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','RELEASED','EXPIRED')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE reservation_items (
    order_id UUID NOT NULL REFERENCES reservations(order_id),
    sku VARCHAR(64) NOT NULL REFERENCES stock_items(sku),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, sku)
);
CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    sku VARCHAR(64) NOT NULL REFERENCES stock_items(sku),
    kind VARCHAR(24) NOT NULL CHECK (kind IN ('IN','OUT','ADJUSTMENT','RESERVATION','RELEASE')),
    quantity INTEGER NOT NULL,
    physical_before INTEGER NOT NULL,
    physical_after INTEGER NOT NULL,
    reserved_before INTEGER NOT NULL,
    reserved_after INTEGER NOT NULL,
    actor_id UUID NOT NULL,
    reference_id UUID,
    reason VARCHAR(240) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_stock_threshold ON stock_items(minimum, physical, reserved);
CREATE INDEX idx_reservations_status_expiry ON reservations(status, expires_at);
CREATE INDEX idx_movements_sku_time ON stock_movements(sku, occurred_at DESC);

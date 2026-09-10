CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES categories(id),
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    description VARCHAR(5000) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    min_price NUMERIC(12,2) NOT NULL CHECK (min_price > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_products_status_category ON products(status, category_id);
CREATE INDEX idx_products_status_price ON products(status, min_price, id);
CREATE TABLE product_variants (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    sku VARCHAR(64) NOT NULL UNIQUE,
    color VARCHAR(60) NOT NULL,
    size VARCHAR(40) NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK (price > 0),
    promotional_price NUMERIC(12,2),
    PRIMARY KEY (product_id, position),
    UNIQUE (product_id, color, size),
    CHECK (promotional_price IS NULL OR (promotional_price > 0 AND promotional_price < price))
);
CREATE TABLE product_images (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    url VARCHAR(1000) NOT NULL,
    alt VARCHAR(160) NOT NULL,
    PRIMARY KEY (product_id, position)
);
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    author_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (product_id, author_id)
);
CREATE INDEX idx_reviews_product_status_created ON reviews(product_id, status, created_at);
CREATE TABLE catalog_audit (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    resource_id UUID NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

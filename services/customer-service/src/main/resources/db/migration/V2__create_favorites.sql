CREATE TABLE favorites (
    user_id UUID NOT NULL REFERENCES customer_profiles(user_id),
    product_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, product_id)
);

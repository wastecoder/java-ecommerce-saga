CREATE TABLE stock_items (
    product_id  UUID PRIMARY KEY,
    available   INTEGER NOT NULL,
    reserved    INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE stock_reservations (
    id          UUID PRIMARY KEY,
    order_id    UUID        NOT NULL,
    product_id  UUID        NOT NULL,
    quantity    INTEGER     NOT NULL,
    status      VARCHAR(20) NOT NULL
);

CREATE INDEX idx_stock_reservations_order_id ON stock_reservations (order_id);

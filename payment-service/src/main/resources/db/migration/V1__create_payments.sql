CREATE TABLE payments (
    id           UUID PRIMARY KEY,
    order_id     UUID           NOT NULL UNIQUE,
    amount       NUMERIC(19, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    provider_ref VARCHAR(100)   NOT NULL
);

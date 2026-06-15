CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    order_id    UUID         NOT NULL,
    customer_id UUID         NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    message     VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notifications_order_id ON notifications (order_id);

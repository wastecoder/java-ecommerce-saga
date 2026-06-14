CREATE TABLE processed_messages (
    event_id     UUID         NOT NULL,
    consumer     VARCHAR(64)  NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (event_id, consumer)
);

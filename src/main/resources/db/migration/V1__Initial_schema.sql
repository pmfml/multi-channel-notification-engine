CREATE TABLE IF NOT EXISTS notification_log (
    id UUID PRIMARY KEY,
    recipient VARCHAR(100) NOT NULL,
    message VARCHAR(300),
    channel VARCHAR(255),
    status VARCHAR(255),
    created_at TIMESTAMP(6) WITH TIME ZONE
);

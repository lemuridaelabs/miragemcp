SET SCHEMA PUBLIC;

CREATE TABLE IF NOT EXISTS "honey_events"
(
    id         VARCHAR(64) NOT NULL,
    remote_ip  VARCHAR(64),
    uri        VARCHAR(255),
    event_type VARCHAR(32),
    is_mcp     BOOLEAN,
    message    VARCHAR(255),
    score      INT DEFAULT 0,
    data       VARCHAR(16384),
    timestamp  TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS honey_events_idx_001 ON "honey_events" (remote_ip);
CREATE INDEX IF NOT EXISTS honey_events_idx_002 ON "honey_events" (timestamp);
CREATE INDEX IF NOT EXISTS honey_events_idx_003 ON "honey_events" (event_type);
CREATE INDEX IF NOT EXISTS honey_events_idx_004 ON "honey_events" (remote_ip, timestamp);

CREATE TABLE IF NOT EXISTS "honey_alerts"
(
    id        VARCHAR(64) NOT NULL,
    remote_ip VARCHAR(64),
    message   VARCHAR(1024),
    timestamp TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS honey_alerts_idx_001 ON "honey_alerts" (remote_ip, timestamp);

CREATE TABLE IF NOT EXISTS "vapid_keys"
(
    id          BIGINT PRIMARY KEY,
    public_key  VARCHAR(1024) NOT NULL,
    private_key VARCHAR(1024) NOT NULL,
    created_at  TIMESTAMP     NOT NULL
);

CREATE TABLE IF NOT EXISTS "push_subscription"
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    endpoint   VARCHAR(1024) NOT NULL UNIQUE,
    p256dh     VARCHAR(1024) NOT NULL,
    auth       VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP     NOT NULL
);

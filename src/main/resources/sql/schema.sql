SET SCHEMA PUBLIC;

create table "honey_events"
(
    id         varchar(64) not null,
    remote_ip  varchar(64),
    uri        varchar(255),
    event_type varchar(32),
    is_mcp     boolean,
    message    varchar(255),
    score      int default 0,
    data       varchar(16384),
    timestamp  timestamp,
    primary key (id)
);

create index if not exists honey_events_idx_001 on "honey_events" (remote_ip);

create table "honey_alerts"
(
    id        varchar(64) not null,
    remote_ip varchar(64),
    message   varchar(1024),
    timestamp timestamp,
    primary key (id)
);

create index if not exists honey_alerts_idx_001 on "honey_alerts" (remote_ip, timestamp);

--

CREATE TABLE "vapid_keys"
(
    id          BIGINT PRIMARY KEY,
    public_key  VARCHAR(1024) NOT NULL,
    private_key VARCHAR(1024) NOT NULL,
    created_at  TIMESTAMP     NOT NULL
);

CREATE TABLE "push_subscription"
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    endpoint   VARCHAR(1024) NOT NULL UNIQUE,
    p256dh     VARCHAR(1024) NOT NULL,
    auth       VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP     NOT NULL
);
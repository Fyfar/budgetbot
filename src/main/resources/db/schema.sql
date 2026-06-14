-- Preserved from original Hibernate schema (data must survive migrations)
CREATE TABLE IF NOT EXISTS balance_history (
    uuid        VARCHAR(36) PRIMARY KEY,
    account_id  VARCHAR(255),
    balance     INTEGER,
    penny       INTEGER,
    time        TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS user_entity (
    id          INTEGER PRIMARY KEY,
    username    VARCHAR(255),
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    chat_id     BIGINT
);

-- New tables replacing Hibernate TABLE_PER_CLASS inheritance + @ElementCollection
CREATE TABLE IF NOT EXISTS budget_config (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_day  INTEGER NOT NULL,
    budget_limit INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS budget_account (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    budget_config_id BIGINT NOT NULL,
    account          VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS security_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS security_user (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    security_config_id BIGINT NOT NULL,
    user_id            INTEGER NOT NULL
);


-- Indexes for the app's access patterns.
-- balance_history is always filtered by account_id and ordered by time
-- (findTop1..., findLastBalanceBeforeTime, findByAccountIdAndTimeBetween).
-- NOTE: keep this index ASCENDING. A DESC index on a TIMESTAMP WITH TIME ZONE
-- column triggers an H2 range-scan bug that returns wrong rows for
-- "WHERE time < ? ORDER BY time DESC" queries. H2 scans an ASC index backwards
-- for DESC ordering anyway, so there is no performance loss.
CREATE INDEX IF NOT EXISTS idx_balance_history_account_time
    ON balance_history (account_id, time);
-- Foreign-key columns used by the config delete/join paths.
CREATE INDEX IF NOT EXISTS idx_budget_account_config
    ON budget_account (budget_config_id);
CREATE INDEX IF NOT EXISTS idx_security_user_config
    ON security_user (security_config_id);

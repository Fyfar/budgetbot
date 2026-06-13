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

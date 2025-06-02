CREATE TABLE IF NOT EXISTS patients  (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    age INT,
    gender VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255),
    password VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS roles (
    name VARCHAR(100) PRIMARY KEY,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS permissions (
    name VARCHAR(100) PRIMARY KEY,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS account_roles (
    account_id VARCHAR(36) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (account_id, role_name),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (role_name) REFERENCES roles(name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_name VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_name, permission_name),
    FOREIGN KEY (role_name) REFERENCES roles(name) ON DELETE CASCADE,
    FOREIGN KEY (permission_name) REFERENCES permissions(name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS invalidated_tokens (
    id VARCHAR(100) PRIMARY KEY,
    expire_time TIMESTAMP
);

INSERT INTO roles (name, description)
VALUES ('USER', 'User role')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO roles (name, description)
VALUES ('ADMIN', 'Admin role')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO accounts (id, username, password)
VALUES ('123e4567-e89b-12d3-a456-426614174000', 'admin', '$2a$10$jmjiQYFQ/.4rf6ruJNPnUOYPIoGBiurHq2Y3BRG1Zg0RiAsd/neqy')
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO account_roles (account_id, role_name)
VALUES ('123e4567-e89b-12d3-a456-426614174000', 'ADMIN')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);



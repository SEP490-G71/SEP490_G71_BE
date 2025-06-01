CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(50) PRIMARY KEY,
    db_name VARCHAR(100),
    db_url VARCHAR(255),
    db_username VARCHAR(100),
    db_password VARCHAR(100)
);

INSERT INTO tenants (id, db_name, db_url, db_username, db_password) VALUES
    ('tenant1', 'tenant1', 'jdbc:mysql://localhost:3308/tenant1', 'root', 'root'),
    ('tenant2', 'tenant2', 'jdbc:mysql://localhost:3309/tenant2', 'root', 'root')

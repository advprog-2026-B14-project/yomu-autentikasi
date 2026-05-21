-- Create schema for testing (mirrors production auth_mod schema)
DROP SCHEMA IF EXISTS auth_mod CASCADE;
CREATE SCHEMA auth_mod;

CREATE TABLE auth_mod.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'USER',
    username VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

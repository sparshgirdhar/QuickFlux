-- Password: "password123" (BCrypt hash)
INSERT INTO users (id, email, password_hash, name, created_at) VALUES
('00000000-0000-0000-0000-000000000001',
 'test@quickflux.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 'Test User',
 NOW());
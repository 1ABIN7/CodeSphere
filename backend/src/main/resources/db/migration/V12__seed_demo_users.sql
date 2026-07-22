-- V12: Seed demo users for development
-- ============================================================================
-- Password for all demo users: password123
-- BCrypt hash: $2b$10$VZbsdlEFG6WZ7Ty73S15ZeX9uMIDV.87YYAQVFqTqyca4tFQRUxaK
-- NOTE: BCrypt $2b$ prefix is fully compatible with Spring Security's BCryptPasswordEncoder.

-- Create a default organization
INSERT INTO organizations (name) VALUES ('CodeSphere Demo Org')
    ON CONFLICT (name) DO NOTHING;

-- Seed demo users (using the 'password' column that V7 added)
INSERT INTO users (email, username, password, role, organization_id, enabled, email_verified)
VALUES
    ('admin@demo.com',     'admin',     '$2b$10$VZbsdlEFG6WZ7Ty73S15ZeX9uMIDV.87YYAQVFqTqyca4tFQRUxaK', 'ROLE_SUPER_ADMIN', 1, TRUE, TRUE),
    ('candidate@demo.com', 'candidate', '$2b$10$VZbsdlEFG6WZ7Ty73S15ZeX9uMIDV.87YYAQVFqTqyca4tFQRUxaK', 'ROLE_CANDIDATE',    1, TRUE, TRUE),
    ('instructor@demo.com','instructor','$2b$10$VZbsdlEFG6WZ7Ty73S15ZeX9uMIDV.87YYAQVFqTqyca4tFQRUxaK', 'ROLE_INSTRUCTOR',   1, TRUE, TRUE),
    ('examiner@demo.com',  'examiner',  '$2b$10$VZbsdlEFG6WZ7Ty73S15ZeX9uMIDV.87YYAQVFqTqyca4tFQRUxaK', 'ROLE_EXAMINER',     1, TRUE, TRUE)
ON CONFLICT (email) DO NOTHING;

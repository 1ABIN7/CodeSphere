-- Align audit_logs table with AuditLog JPA entity
-- Entity expects: id, user_id, action, resource, timestamp, ip_address
-- V1 migration has: id, user_id, action, resource_type, resource_id, ip_address, user_agent, occurred_at
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS resource VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

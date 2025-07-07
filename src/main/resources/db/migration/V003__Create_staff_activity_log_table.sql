CREATE TABLE staff_activity_log (
                                    id BIGSERIAL PRIMARY KEY,
                                    staff_id VARCHAR(36) NOT NULL,
                                    action_type VARCHAR(50) NOT NULL,
                                    action_description TEXT,
                                    resource_type VARCHAR(50),
                                    resource_id VARCHAR(36),
                                    ip_address VARCHAR(45),
                                    user_agent TEXT,
                                    request_method VARCHAR(10),
                                    request_url TEXT,
                                    response_status INTEGER,
                                    execution_time_ms BIGINT,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_staff_activity_log_staff_id
                                        FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
);

CREATE INDEX idx_staff_activity_staff_id ON staff_activity_log (staff_id);
CREATE INDEX idx_staff_activity_action_type ON staff_activity_log (action_type);
CREATE INDEX idx_staff_activity_created_at ON staff_activity_log (created_at);

CREATE INDEX idx_staff_activity_staff_action_date ON staff_activity_log (staff_id, action_type, created_at);

CREATE INDEX idx_staff_activity_recent ON staff_activity_log (created_at, staff_id)
    WHERE created_at > (CURRENT_TIMESTAMP - INTERVAL '30 days');

COMMENT ON TABLE staff_activity_log IS 'Audit trail for all staff actions in the system';
COMMENT ON COLUMN staff_activity_log.action_type IS 'Type of action performed (LOGIN, CREATE_VEHICLE, etc.)';
COMMENT ON COLUMN staff_activity_log.execution_time_ms IS 'Time taken to execute the action in milliseconds';

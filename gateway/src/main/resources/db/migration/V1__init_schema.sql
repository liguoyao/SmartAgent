-- Gateway database schema
CREATE TABLE IF NOT EXISTS gateway_rate_limit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    request_count BIGINT DEFAULT 0,
    window_start DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(api_key, endpoint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS gateway_access_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id VARCHAR(64),
    user_id BIGINT,
    username VARCHAR(255),
    client_ip VARCHAR(64),
    method VARCHAR(10),
    path VARCHAR(500),
    status_code INTEGER,
    response_time_ms BIGINT,
    user_agent TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE INDEX idx_access_log_user_id ON gateway_access_log(user_id);
CREATE INDEX idx_access_log_created_at ON gateway_access_log(created_at);
CREATE INDEX idx_access_log_trace_id ON gateway_access_log(trace_id);

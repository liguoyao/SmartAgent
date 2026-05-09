-- Gateway database schema
CREATE TABLE IF NOT EXISTS gateway_rate_limit (
    id BIGSERIAL PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    request_count BIGINT DEFAULT 0,
    window_start TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(api_key, endpoint)
);

CREATE TABLE IF NOT EXISTS gateway_access_log (
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(64),
    user_id BIGINT,
    username VARCHAR(255),
    client_ip VARCHAR(64),
    method VARCHAR(10),
    path VARCHAR(500),
    status_code INTEGER,
    response_time_ms BIGINT,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_access_log_user_id ON gateway_access_log(user_id);
CREATE INDEX idx_access_log_created_at ON gateway_access_log(created_at);
CREATE INDEX idx_access_log_trace_id ON gateway_access_log(trace_id);

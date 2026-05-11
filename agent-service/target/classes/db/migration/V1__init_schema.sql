-- Agent Service Database Schema

-- Agents table
CREATE TABLE agents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    system_prompt TEXT,
    capabilities TEXT,
    keywords TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 0,
    max_conversations INTEGER DEFAULT 100,
    current_conversations INTEGER DEFAULT 0,
    success_rate DECIMAL(5,2) DEFAULT 0.0,
    avg_response_time_ms BIGINT DEFAULT 0,
    total_handled BIGINT DEFAULT 0,
    config JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT chk_agent_type CHECK (type IN ('SUPERVISOR', 'PRODUCT', 'COMPLAINT', 'REFUND', 'ORDER', 'TECHNICAL', 'BILLING', 'ESCALATION'))
);

-- Agent conversation context table
CREATE TABLE agent_conversation_context (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    agent_id BIGINT,
    agent_type VARCHAR(50),
    current_state VARCHAR(50),
    context_data JSONB,
    message_history JSONB,
    metadata JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE
);

-- Agent message log table
CREATE TABLE agent_message_log (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    agent_id BIGINT,
    agent_type VARCHAR(50),
    message_type VARCHAR(20) NOT NULL, -- 'USER', 'AGENT', 'SYSTEM'
    content TEXT NOT NULL,
    intent VARCHAR(50),
    confidence DECIMAL(5,2),
    processing_time_ms BIGINT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Agent statistics table
CREATE TABLE agent_statistics (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    date DATE NOT NULL,
    total_messages BIGINT DEFAULT 0,
    successful_messages BIGINT DEFAULT 0,
    escalated_messages BIGINT DEFAULT 0,
    avg_response_time_ms BIGINT DEFAULT 0,
    avg_confidence DECIMAL(5,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agent_id, date)
);

-- Indexes
CREATE INDEX idx_agents_type ON agents(type);
CREATE INDEX idx_agents_active ON agents(is_active);
CREATE INDEX idx_agents_priority ON agents(priority DESC);

CREATE INDEX idx_context_session_id ON agent_conversation_context(session_id);
CREATE INDEX idx_context_user_id ON agent_conversation_context(user_id);
CREATE INDEX idx_context_active ON agent_conversation_context(is_active);

CREATE INDEX idx_message_log_session ON agent_message_log(session_id);
CREATE INDEX idx_message_log_user ON agent_message_log(user_id);
CREATE INDEX idx_message_log_created ON agent_message_log(created_at);
CREATE INDEX idx_message_log_agent ON agent_message_log(agent_id);

CREATE INDEX idx_stats_agent_date ON agent_statistics(agent_id, date);

-- Insert default agents
INSERT INTO agents (name, type, description, system_prompt, priority, is_active) VALUES
('Supervisor Agent', 'SUPERVISOR', 'Main orchestrator that routes customer requests to appropriate specialists', 
'You are the Supervisor Agent for SmartAgent customer service platform. Your role is to:\n1. Receive customer messages\n2. Classify the intent using AI\n3. Route to appropriate specialist agents\n4. Handle escalations when needed', 100, true),
('Product Agent', 'PRODUCT', 'Handles product inquiries, specifications, pricing', 
'Product Agent system prompt...', 50, true),
('Complaint Agent', 'COMPLAINT', 'Handles customer complaints and negative feedback', 
'Complaint Agent system prompt...', 50, true),
('Refund Agent', 'REFUND', 'Processes refund requests and tracking', 
'Refund Agent system prompt...', 50, true),
('Order Agent', 'ORDER', 'Manages order status, modifications, cancellations', 
'Order Agent system prompt...', 50, true),
('Technical Agent', 'TECHNICAL', 'Handles technical support and troubleshooting', 
'Technical Agent system prompt...', 50, true),
('Billing Agent', 'BILLING', 'Manages billing inquiries and payment issues', 
'Billing Agent system prompt...', 50, true);

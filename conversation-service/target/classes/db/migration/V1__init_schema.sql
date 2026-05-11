-- Conversation Service Database Schema

-- Conversations table
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    agent_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    last_message_at TIMESTAMP,
    message_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'CLOSED', 'TRANSFERRED', 'ESCALATED'))
);

-- Messages table
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT,
    session_id VARCHAR(100) NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    sender_id BIGINT,
    content TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    content_type VARCHAR(20) DEFAULT 'TEXT',
    agent_type VARCHAR(50),
    intent VARCHAR(50),
    confidence DECIMAL(5,2),
    processing_time_ms BIGINT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT chk_sender_type CHECK (sender_type IN ('USER', 'AGENT', 'SYSTEM')),
    CONSTRAINT chk_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'AUDIO', 'VIDEO', 'CAROUSEL', 'QUICK_REPLY', 'SYSTEM'))
);

-- Indexes
CREATE INDEX idx_conversations_session_id ON conversations(session_id);
CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_status ON conversations(status);
CREATE INDEX idx_conversations_active ON conversations(is_active);
CREATE INDEX idx_conversations_last_message ON conversations(last_message_at DESC);

CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_sender_type ON messages(sender_type);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX idx_messages_intent ON messages(intent);

-- Unique constraint for active session
CREATE UNIQUE INDEX idx_conversations_active_session ON conversations(session_id) WHERE is_active = true;

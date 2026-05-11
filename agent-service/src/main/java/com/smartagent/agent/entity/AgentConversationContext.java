package com.smartagent.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartagent.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Conversation context for agent conversations
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_conversation_context")
public class AgentConversationContext extends BaseEntity {

    @TableField("session_id")
    private String sessionId;

    @TableField("user_id")
    private Long userId;

    @TableField("agent_id")
    private Long agentId;

    @TableField("agent_type")
    private String agentType;

    @TableField("current_state")
    private String currentState;

    @TableField("context_data")
    private String contextData;

    @TableField("message_history")
    private String messageHistory;

    @TableField("metadata")
    private String metadata;

    @TableField("is_active")
    private Boolean isActive = true;

    @TableField("last_message_at")
    private java.time.LocalDateTime lastMessageAt;
}

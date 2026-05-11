package com.smartagent.conversation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartagent.common.entity.BaseEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Conversation entity representing a chat session
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@TableName("conversations")
public class Conversation extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("user_id")
    private Long userId;

    @TableField("agent_id")
    private Long agentId;

    @TableField("status")
    private ConversationStatus status;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("ended_at")
    private LocalDateTime endedAt;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    @TableField("message_count")
    private Integer messageCount = 0;

    @TableField("is_active")
    private Boolean isActive = true;

    @TableField("metadata")
    private String metadata;

    public enum ConversationStatus {
        ACTIVE, CLOSED, TRANSFERRED, ESCALATED
    }
}

package com.smartagent.conversation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartagent.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental_superbuilder.ExperimentalSuperBuilder;

/**
 * Message entity representing a chat message
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ExperimentalSuperBuilder
@TableName("messages")
public class Message extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("session_id")
    private String sessionId;

    @TableField("sender_type")
    private SenderType senderType;

    @TableField("sender_id")
    private Long senderId;

    @TableField("content")
    private String content;

    @TableField("message_type")
    private MessageType messageType;

    @TableField("content_type")
    private ContentType contentType = ContentType.TEXT;

    @TableField("agent_type")
    private String agentType;

    @TableField("intent")
    private String intent;

    @TableField("confidence")
    private Double confidence;

    @TableField("processing_time_ms")
    private Long processingTimeMs;

    @TableField("metadata")
    private String metadata;

    public enum SenderType {
        USER, AGENT, SYSTEM
    }

    public enum MessageType {
        TEXT, IMAGE, FILE, AUDIO, VIDEO, CAROUSEL, QUICK_REPLY, SYSTEM
    }

    public enum ContentType {
        TEXT, HTML, MARKDOWN
    }
}

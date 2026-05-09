package com.smartagent.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartagent.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental_superbuilder.ExperimentalSuperBuilder;

/**
 * Agent entity representing an AI agent in the system
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ExperimentalSuperBuilder
@TableName("agents")
public class Agent extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("type")
    @Enumerated(EnumType.STRING)
    private AgentType type;

    @TableField("description")
    private String description;

    @TableField("system_prompt")
    private String systemPrompt;

    @TableField("capabilities")
    private String capabilities;

    @TableField("keywords")
    private String keywords;

    @TableField("is_active")
    private Boolean isActive = true;

    @TableField("priority")
    private Integer priority = 0;

    @TableField("max_conversations")
    private Integer maxConversations = 100;

    @TableField("current_conversations")
    private Integer currentConversations = 0;

    @TableField("success_rate")
    private Double successRate = 0.0;

    @TableField("avg_response_time_ms")
    private Long avgResponseTimeMs = 0L;

    @TableField("total_handled")
    private Long totalHandled = 0L;

    @TableField("config")
    private String config;
}

package com.smartagent.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String category;
    private String tags;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

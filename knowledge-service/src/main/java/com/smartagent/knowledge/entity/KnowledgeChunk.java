package com.smartagent.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String content;
    private Integer chunkIndex;
    private String embeddingVector;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.smartagent.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Base entity with common fields for all domain objects
 */
@Data
public abstract class BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Long version;

    @TableLogic
    private Boolean deleted = false;
}

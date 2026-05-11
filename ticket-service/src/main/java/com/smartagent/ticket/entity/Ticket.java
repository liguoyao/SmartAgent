package com.smartagent.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ticket")
public class Ticket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ticketNumber;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String category;
    private Long customerId;
    private String customerName;
    private Long assigneeId;
    private String assigneeName;
    private String resolution;
    private LocalDateTime closedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

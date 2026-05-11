package com.smartagent.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ticket_comment")
public class TicketComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Long userId;
    private String userName;
    private String content;
    private String isInternal;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.smartagent.ticket.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartagent.ticket.entity.TicketComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketCommentRepository extends BaseMapper<TicketComment> {
}

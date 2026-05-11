package com.smartagent.ticket.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartagent.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketRepository extends BaseMapper<Ticket> {
}

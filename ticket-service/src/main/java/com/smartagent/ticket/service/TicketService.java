package com.smartagent.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartagent.ticket.entity.Ticket;
import com.smartagent.ticket.entity.TicketComment;
import com.smartagent.ticket.repository.TicketCommentRepository;
import com.smartagent.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService extends ServiceImpl<TicketRepository, Ticket> {

    private final TicketCommentRepository commentRepository;

    @Transactional
    public Ticket createTicket(String title, String description, String priority, String category,
                               Long customerId, String customerName) {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("TK" + System.currentTimeMillis());
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setPriority(priority != null ? priority : "MEDIUM");
        ticket.setCategory(category);
        ticket.setStatus("OPEN");
        ticket.setCustomerId(customerId);
        ticket.setCustomerName(customerName);
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(ticket);
        log.info("创建工单: {} by {}", ticket.getTicketNumber(), customerName);
        return ticket;
    }

    public IPage<Ticket> listTickets(String status, String priority, int page, int size) {
        Page<Ticket> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Ticket::getStatus, status)
               .eq(priority != null, Ticket::getPriority, priority)
               .orderByDesc(Ticket::getCreateTime);
        return page(pageParam, wrapper);
    }

    @Transactional
    public void assignTicket(Long ticketId, Long assigneeId, String assigneeName) {
        Ticket ticket = baseMapper.selectById(ticketId);
        if (ticket != null) {
            ticket.setAssigneeId(assigneeId);
            ticket.setAssigneeName(assigneeName);
            ticket.setUpdateTime(LocalDateTime.now());
            baseMapper.updateById(ticket);
            log.info("工单 {} 已分配给 {}", ticket.getTicketNumber(), assigneeName);
        }
    }

    @Transactional
    public void closeTicket(Long ticketId, String resolution) {
        Ticket ticket = baseMapper.selectById(ticketId);
        if (ticket != null) {
            ticket.setStatus("CLOSED");
            ticket.setResolution(resolution);
            ticket.setClosedAt(LocalDateTime.now());
            ticket.setUpdateTime(LocalDateTime.now());
            baseMapper.updateById(ticket);
            log.info("工单 {} 已关闭", ticket.getTicketNumber());
        }
    }

    public List<TicketComment> getComments(Long ticketId) {
        return commentRepository.selectList(
            new LambdaQueryWrapper<TicketComment>()
                .eq(TicketComment::getTicketId, ticketId)
                .orderByAsc(TicketComment::getCreateTime)
        );
    }

    @Transactional
    public TicketComment addComment(Long ticketId, Long userId, String userName, String content, boolean isInternal) {
        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setUserId(userId);
        comment.setUserName(userName);
        comment.setContent(content);
        comment.setIsInternal(isInternal ? "Y" : "N");
        comment.setCreateTime(LocalDateTime.now());
        commentRepository.insert(comment);
        return comment;
    }
}

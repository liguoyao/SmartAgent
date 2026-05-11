package com.smartagent.ticket.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartagent.ticket.entity.Ticket;
import com.smartagent.ticket.entity.TicketComment;
import com.smartagent.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ticketService.createTicket(
            (String) request.get("title"),
            (String) request.get("description"),
            (String) request.get("priority"),
            (String) request.get("category"),
            ((Number) request.get("customerId")).longValue(),
            (String) request.get("customerName")
        ));
    }

    @GetMapping
    public ResponseEntity<IPage<Ticket>> listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ticketService.listTickets(status, priority, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<Void> assignTicket(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        ticketService.assignTicket(id, ((Number) request.get("assigneeId")).longValue(), (String) request.get("assigneeName"));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<Void> closeTicket(@PathVariable Long id, @RequestBody Map<String, String> request) {
        ticketService.closeTicket(id, request.get("resolution"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<TicketComment>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketComment> addComment(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ticketService.addComment(
            id,
            ((Number) request.get("userId")).longValue(),
            (String) request.get("userName"),
            (String) request.get("content"),
            Boolean.parseBoolean((String) request.get("isInternal"))
        ));
    }
}

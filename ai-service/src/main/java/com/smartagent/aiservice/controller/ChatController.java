package com.smartagent.aiservice.controller;

import com.smartagent.aiservice.model.dto.ChatRequest;
import com.smartagent.aiservice.model.dto.ChatResponse;
import com.smartagent.aiservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request: sessionId={}, messagePreview={}", 
            request.getSessionId(), 
            request.getMessage().substring(0, Math.min(100, request.getMessage().length())));
        
        ChatService.ChatResult result = chatService.processChat(
            request.getMessage(),
            request.getSessionId(),
            request.isUseTools(),
            request.getSpecialty()
        );
        
        ChatResponse response = ChatResponse.builder()
            .message(result.message())
            .sessionId(result.sessionId())
            .toolUsed(result.toolUsed())
            .toolResult(result.toolResult() != null ? Map.class.cast(result.toolResult()) : null)
            .shouldTransfer(result.shouldTransfer())
            .intent(result.intent())
            .metadata(result.metadata() != null ? Map.class.cast(result.metadata()) : Map.of())
            .build();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("Stream chat request: sessionId={}", request.getSessionId());
        
        String sessionId = request.getSessionId() != null ? request.getSessionId() : java.util.UUID.randomUUID().toString();
        
        // For streaming, we return a simple SSE flux
        // In production, this would integrate with actual streaming LLM responses
        ChatService.ChatResult result = chatService.processChat(
            request.getMessage(),
            sessionId,
            request.isUseTools(),
            request.getSpecialty()
        );
        
        final String[] chunks = result.message().split(" ");
        return Flux.generate(sink -> {
            for (String chunk : chunks) {
                sink.next("data: " + chunk + " \n\n");
            }
            sink.next("data: [DONE]\n\n");
            sink.complete();
        });
    }
}
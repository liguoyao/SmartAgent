package com.smartagent.aiservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private String sessionId;
    
    @Builder.Default
    private boolean useTools = true;
    
    private String specialty;
    
    @Builder.Default
    private boolean stream = false;
}
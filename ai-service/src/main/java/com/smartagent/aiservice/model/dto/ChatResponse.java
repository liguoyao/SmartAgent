package com.smartagent.aiservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String sessionId;
    private String toolUsed;
    private Map<String, Object> toolResult;
    private boolean shouldTransfer;
    private String intent;
    private Map<String, Object> metadata;
}
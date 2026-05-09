package com.smartagent.aiservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {
    private String message;
    private String toolUsed;
    private Map<String, Object> toolResult;
    private boolean shouldTransfer;
    private Intent intent;
    private Map<String, Object> metadata;
}
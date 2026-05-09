package com.smartagent.aiservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String role;
    private String content;
    private List<Map<String, Object>> toolCalls;
    private String toolCallId;
    private String name;
    
    @Builder.Default
    private String timestamp = Instant.now().toString();
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("role", role);
        map.put("content", content);
        map.put("timestamp", timestamp);
        if (toolCalls != null) map.put("tool_calls", toolCalls);
        if (toolCallId != null) map.put("tool_call_id", toolCallId);
        if (name != null) map.put("name", name);
        return map;
    }
}
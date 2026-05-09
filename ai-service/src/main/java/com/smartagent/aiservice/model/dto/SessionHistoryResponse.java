package com.smartagent.aiservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionHistoryResponse {
    private String sessionId;
    private List<Map<String, Object>> messages;
    private int count;
}
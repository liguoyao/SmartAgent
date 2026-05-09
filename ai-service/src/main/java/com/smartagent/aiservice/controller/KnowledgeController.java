package com.smartagent.aiservice.controller;

import com.smartagent.aiservice.model.Document;
import com.smartagent.aiservice.model.dto.DocumentRequest;
import com.smartagent.aiservice.model.dto.SearchRequest;
import com.smartagent.aiservice.model.dto.SearchResponse;
import com.smartagent.aiservice.service.RAGService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final RAGService ragService;

    public KnowledgeController(RAGService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addDocuments(@RequestBody List<DocumentRequest> requests) {
        log.info("Adding {} documents", requests.size());
        
        List<Document> documents = requests.stream()
            .map(r -> Document.builder()
                .content(r.getContent())
                .metadata(r.getMetadata())
                .build())
            .collect(Collectors.toList());
        
        List<String> ids = ragService.addDocuments(documents);
        
        return ResponseEntity.ok(Map.of(
            "status", "added",
            "count", ids.size(),
            "ids", ids
        ));
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        log.info("Searching knowledge base: query={}", request.getQuery());
        
        List<Document> results = ragService.search(request.getQuery(), request.getTopK());
        
        List<Map<String, Object>> formattedResults = results.stream()
            .map(doc -> Map.<String, Object>of(
                "content", doc.getContent(),
                "source", doc.getMetadata() != null ? doc.getMetadata().getOrDefault("source", "unknown") : "unknown",
                "distance", doc.getMetadata() != null ? doc.getMetadata().getOrDefault("distance", 0) : 0
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(SearchResponse.builder()
            .results(formattedResults)
            .count(formattedResults.size())
            .build());
    }

    @PostMapping("/context")
    public ResponseEntity<Map<String, Object>> getContext(
            @RequestParam String query,
            @RequestParam(required = false) Integer topK,
            @RequestParam(defaultValue = "3000") int maxChars) {
        
        log.info("Getting context for query: {}", query);
        
        String context = ragService.getRelevantContext(query, topK, maxChars);
        
        return ResponseEntity.ok(Map.of(
            "context", context,
            "length", context.length()
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "status", ragService.healthCheck() ? "healthy" : "unhealthy",
            "documentCount", ragService.getDocumentCount()
        ));
    }
}
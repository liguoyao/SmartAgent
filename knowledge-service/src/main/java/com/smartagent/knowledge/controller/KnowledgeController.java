package com.smartagent.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartagent.knowledge.entity.KnowledgeDocument;
import com.smartagent.knowledge.entity.KnowledgeChunk;
import com.smartagent.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/documents")
    public ResponseEntity<KnowledgeDocument> createDocument(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");
        String category = request.get("category");
        String tags = request.get("tags");
        return ResponseEntity.ok(knowledgeService.createDocument(title, content, category, tags));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<KnowledgeDocument> getDocument(@PathVariable Long id) {
        knowledgeService.incrementViewCount(id);
        return ResponseEntity.ok(knowledgeService.getById(id));
    }

    @GetMapping("/documents")
    public ResponseEntity<IPage<KnowledgeDocument>> searchDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(knowledgeService.searchDocuments(keyword, page, size));
    }

    @GetMapping("/documents/{id}/chunks")
    public ResponseEntity<List<KnowledgeChunk>> getChunks(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeService.findRelevantChunks(id));
    }

    @PostMapping("/documents/upload")
    public ResponseEntity<KnowledgeDocument> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam String category) throws IOException {
        return ResponseEntity.ok(knowledgeService.uploadDocument(file, title, category));
    }
}

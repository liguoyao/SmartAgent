package com.smartagent.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartagent.knowledge.entity.KnowledgeChunk;
import com.smartagent.knowledge.entity.KnowledgeDocument;
import com.smartagent.knowledge.repository.KnowledgeChunkRepository;
import com.smartagent.knowledge.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService extends ServiceImpl<KnowledgeDocumentRepository, KnowledgeDocument> {

    private final KnowledgeChunkRepository chunkRepository;

    @Transactional
    public KnowledgeDocument createDocument(String title, String content, String category, String tags) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setCategory(category);
        doc.setTags(tags);
        doc.setStatus("ACTIVE");
        doc.setViewCount(0);
        doc.setLikeCount(0);
        doc.setCreateTime(LocalDateTime.now());
        doc.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(doc);

        // 分块存储
        splitAndStoreChunks(doc.getId(), content);
        log.info("创建知识文档成功: id={}, title={}", doc.getId(), title);
        return doc;
    }

    private void splitAndStoreChunks(Long documentId, String content) {
        // 按段落分块，每500字符一块
        int chunkSize = 500;
        for (int i = 0; i < content.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, content.length());
            String chunkContent = content.substring(i, end);

            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(documentId);
            chunk.setContent(chunkContent);
            chunk.setChunkIndex(i / chunkSize);
            chunk.setCreateTime(LocalDateTime.now());
            chunkRepository.insert(chunk);
        }
    }

    public IPage<KnowledgeDocument> searchDocuments(String keyword, int page, int size) {
        Page<KnowledgeDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, KnowledgeDocument::getTitle, keyword)
               .or().like(keyword != null, KnowledgeDocument::getContent, keyword)
               .orderByDesc(KnowledgeDocument::getCreateTime);
        return page(pageParam, wrapper);
    }

    public List<KnowledgeChunk> findRelevantChunks(Long documentId) {
        return chunkRepository.selectList(
            new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocumentId, documentId)
                .orderByAsc(KnowledgeChunk::getChunkIndex)
        );
    }

    public void incrementViewCount(Long id) {
        KnowledgeDocument doc = baseMapper.selectById(id);
        if (doc != null) {
            doc.setViewCount(doc.getViewCount() + 1);
            baseMapper.updateById(doc);
        }
    }

    @Transactional
    public KnowledgeDocument uploadDocument(MultipartFile file, String title, String category) throws IOException {
        String content = new String(file.getBytes());
        return createDocument(title, content, category, "");
    }
}

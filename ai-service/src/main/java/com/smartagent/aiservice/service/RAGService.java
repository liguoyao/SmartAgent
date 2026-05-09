package com.smartagent.aiservice.service;

import com.smartagent.aiservice.config.AppProperties;
import com.smartagent.aiservice.model.Document;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RAGService {

    private final AppProperties appProperties;
    
    // In-memory vector store
    private final List<Document> documents = new ArrayList<>();
    private final Map<String, float[]> embeddings = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> metadata = new ConcurrentHashMap<>();
    
    // Simple embedding dimension (using average of word vectors concept)
    private static final int EMBEDDING_DIM = 384;

    public RAGService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        // Seed sample knowledge if empty
        if (documents.isEmpty()) {
            addSampleKnowledge();
        }
        log.info("RAG service initialized with {} documents", documents.size());
    }

    private void addSampleKnowledge() {
        List<Document> sampleDocs = List.of(
            Document.builder()
                .content("Our return policy allows returns within 30 days of purchase with original receipt.")
                .metadata(Map.of("source", "return_policy", "category", "policy"))
                .build(),
            Document.builder()
                .content("We offer free shipping on orders over $50. Standard shipping takes 5-7 business days.")
                .metadata(Map.of("source", "shipping_info", "category", "policy"))
                .build(),
            Document.builder()
                .content("For technical support, first try restarting the device. If issues persist, contact support.")
                .metadata(Map.of("source", "tech_support", "category", "support"))
                .build(),
            Document.builder()
                .content("You can track your order status using your order ID. Orders are processed within 1-2 business days.")
                .metadata(Map.of("source", "order_tracking", "category", "orders"))
                .build(),
            Document.builder()
                .content("We accept Visa, MasterCard, and PayPal. Refunds are processed within 5-7 business days.")
                .metadata(Map.of("source", "payment_info", "category", "billing"))
                .build()
        );
        
        for (Document doc : sampleDocs) {
            addDocument(doc);
        }
    }

    public List<String> addDocuments(List<Document> docs) {
        List<String> ids = new ArrayList<>();
        for (Document doc : docs) {
            ids.add(addDocument(doc));
        }
        log.info("Added {} documents", docs.size());
        return ids;
    }

    public String addDocument(Document document) {
        String docId = document.getDocId() != null ? document.getDocId() : UUID.randomUUID().toString();
        Document newDoc = Document.builder()
            .content(document.getContent())
            .metadata(document.getMetadata())
            .docId(docId)
            .build();
        
        documents.add(newDoc);
        embeddings.put(docId, computeEmbedding(document.getContent()));
        metadata.put(docId, document.getMetadata() != null ? document.getMetadata() : new HashMap<>());
        
        return docId;
    }

    public List<Document> search(String query, Integer topK) {
        int k = topK != null ? topK : appProperties.getVectorStore().getTopK();
        float[] queryEmbedding = computeEmbedding(query);
        
        // Calculate similarities
        List<Map.Entry<String, Float>> similarities = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : embeddings.entrySet()) {
            float similarity = cosineSimilarity(queryEmbedding, entry.getValue());
            similarities.add(Map.entry(entry.getKey(), similarity));
        }
        
        // Sort by similarity (descending) and take top k
        return similarities.stream()
            .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
            .limit(k)
            .map(entry -> {
                int idx = -1;
                for (int i = 0; i < documents.size(); i++) {
                    if (documents.get(i).getDocId().equals(entry.getKey())) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0) {
                    Document doc = documents.get(idx);
                    Map<String, Object> docMeta = new HashMap<>(doc.getMetadata() != null ? doc.getMetadata() : new HashMap<>());
                    docMeta.put("distance", 1 - entry.getValue()); // Convert similarity to distance
                    return Document.builder()
                        .content(doc.getContent())
                        .metadata(docMeta)
                        .docId(doc.getDocId())
                        .build();
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public String getRelevantContext(String query, Integer topK, int maxChars) {
        List<Document> docs = search(query, topK);
        if (docs.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        for (Document doc : docs) {
            String text = "[Source: " + doc.getMetadata().getOrDefault("source", "unknown") + "]\n" + doc.getContent();
            if (context.length() + text.length() <= maxChars) {
                context.append(text).append("\n\n---\n\n");
            } else {
                break;
            }
        }
        return context.toString();
    }

    public void reset() {
        documents.clear();
        embeddings.clear();
        metadata.clear();
        addSampleKnowledge();
        log.info("Vector store reset");
    }

    // Simple embedding computation using word frequency vectors
    private float[] computeEmbedding(String text) {
        float[] embedding = new float[EMBEDDING_DIM];
        String[] words = text.toLowerCase().split("\\s+");
        
        for (String word : words) {
            int seed = word.hashCode();
            Random rand = new Random(seed);
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                embedding[i] += (rand.nextFloat() * 2 - 1) * 0.1f;
            }
        }
        
        // Normalize
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                embedding[i] /= norm;
            }
        }
        
        return embedding;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0;
        float normA = 0;
        float normB = 0;
        int dim = Math.min(a.length, b.length);
        
        for (int i = 0; i < dim; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        normA = (float) Math.sqrt(normA);
        normB = (float) Math.sqrt(normB);
        
        if (normA > 0 && normB > 0) {
            return dot / (normA * normB);
        }
        return 0;
    }

    public int getDocumentCount() {
        return documents.size();
    }

    public boolean healthCheck() {
        return !documents.isEmpty();
    }
}
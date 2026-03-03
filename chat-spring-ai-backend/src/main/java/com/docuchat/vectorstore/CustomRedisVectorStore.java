package com.docuchat.vectorstore;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CustomRedisVectorStore implements VectorStore {

    private static final String VECTOR_KEY_PREFIX = "vector:";
    private static final String INDEX_KEY = "vector:index";

    private final RedisTemplate<String, VectorEntry> redisTemplate;
    private final RedisTemplate<String, String> redisIndexTemplate;
    private final EmbeddingModel embeddingModel;

    public CustomRedisVectorStore(
            RedisTemplate<String, VectorEntry> redisTemplate,
            RedisTemplate<String, String> redisIndexTemplate,
            EmbeddingModel embeddingModel) {
        this.redisTemplate = redisTemplate;
        this.redisIndexTemplate = redisIndexTemplate;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void add(List<Document> documents) {
        for (Document document : documents) {
            try {
                List<Double> embedding = generateEmbedding(document.getContent());

                VectorEntry entry = new VectorEntry();
                entry.setId(document.getId());
                entry.setContent(document.getContent());
                entry.setMetadata(document.getMetadata());
                entry.setEmbedding(embedding);

                String key = VECTOR_KEY_PREFIX + document.getId();
                redisTemplate.opsForValue().set(key, entry);

                // Store ID in index set
                redisIndexTemplate.opsForSet().add(INDEX_KEY, document.getId());

            } catch (Exception e) {
                log.error("Error adding document {}: {}", document.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        List<Document> results = new ArrayList<>();
        try {
            List<Double> queryEmbedding = generateEmbedding(request.getQuery());

            Set<String> vectorIds = redisIndexTemplate.opsForSet().members(INDEX_KEY);
            if (vectorIds == null || vectorIds.isEmpty()) return results;

            for (String id : vectorIds) {
                VectorEntry entry = redisTemplate.opsForValue().get(VECTOR_KEY_PREFIX + id);
                if (entry == null) continue;

                if (request.getFilterExpression() != null) {
                    String sessionId = extractSessionId(String.valueOf(request.getFilterExpression()));
                    String entrySessionId = (String) entry.getMetadata().get("sessionId");
                    if (sessionId != null && !sessionId.equals(entrySessionId)) continue;
                }

                double similarity = cosineSimilarity(queryEmbedding, entry.getEmbedding());
                if (similarity >= request.getSimilarityThreshold()) {
                    Document doc = new Document(entry.getId(), entry.getContent(), entry.getMetadata());
                    doc.getMetadata().put("distance", 1.0 - similarity);
                    results.add(doc);
                }
            }

            results.sort((d1, d2) -> Double.compare(
                    (Double) d2.getMetadata().get("distance"),
                    (Double) d1.getMetadata().get("distance")
            ));

            return results.size() > request.getTopK() ? results.subList(0, request.getTopK()) : results;

        } catch (Exception e) {
            log.error("Error during similarity search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Document> similaritySearch(String query) {
        return similaritySearch(SearchRequest.query(query).withTopK(5).withSimilarityThreshold(0.3));
    }

    @Override
    public Optional<Boolean> delete(List<String> idList) {
        try {
            for (String id : idList) {
                redisTemplate.delete(VECTOR_KEY_PREFIX + id);
                redisIndexTemplate.opsForSet().remove(INDEX_KEY, id);
            }
            return Optional.of(true);
        } catch (Exception e) {
            log.error("Error deleting documents: {}", e.getMessage(), e);
            return Optional.of(false);
        }
    }

    public void deleteBySession(String sessionId) {
        Set<String> vectorIds = redisIndexTemplate.opsForSet().members(INDEX_KEY);
        if (vectorIds == null || vectorIds.isEmpty()) return;

        List<String> toDelete = vectorIds.stream()
                .filter(id -> {
                    VectorEntry entry = redisTemplate.opsForValue().get(VECTOR_KEY_PREFIX + id);
                    return entry != null && sessionId.equals(entry.getMetadata().get("sessionId"));
                })
                .toList();

        delete(toDelete);
    }

    private List<Double> generateEmbedding(String text) {
        EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
        EmbeddingResponse response = embeddingModel.call(request);
        float[] floatArray = response.getResults().get(0).getOutput();
        List<Double> embedding = new ArrayList<>(floatArray.length);
        for (float v : floatArray) embedding.add((double) v);
        return embedding;
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        double dot = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < v1.size(); i++) {
            dot += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }
        if (norm1 == 0 || norm2 == 0) return 0;
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private String extractSessionId(String filterExpression) {
        if (filterExpression != null && filterExpression.contains("sessionId")) {
            int start = filterExpression.indexOf("'"), end = filterExpression.lastIndexOf("'");
            if (start != -1 && end != -1 && end > start) return filterExpression.substring(start + 1, end);
        }
        return null;
    }

    @Data
    public static class VectorEntry implements java.io.Serializable {
        private String id;
        private String content;
        private Map<String, Object> metadata;
        private List<Double> embedding;
    }
}

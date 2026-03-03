package com.docuchat.service;

import com.docuchat.config.AppProperties;
import com.docuchat.model.ChatMessage;
import com.docuchat.model.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementing RAG (Retrieval Augmented Generation) pipeline.
 *
 * <p>This service handles:</p>
 * <ul>
 *   <li>Document storage in vector database</li>
 *   <li>Similarity search for relevant contexts</li>
 *   <li>Query augmentation with retrieved context</li>
 *   <li>LLM-based answer generation</li>
 * </ul>
 *
 * 
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final AppProperties appProperties;

    private static final String RAG_PROMPT_TEMPLATE = """
            You are a helpful AI assistant that answers questions based ONLY on the provided context.
            
            IMPORTANT RULES:
            1. Answer ONLY using information from the context below
            2. If the context doesn't contain the answer, say: "I couldn't find that information in the uploaded documents."
            3. Do NOT use your general knowledge or make assumptions
            4. Be concise and accurate
            5. If you're quoting from the context, mention the document name
            
            CONTEXT:
            {context}
            
            QUESTION: {question}
            
            ANSWER:
            """;

    /**
     * Stores document chunks in vector database.
     *
     * @param documents list of document chunks to store
     * @param sessionId user session identifier
     */
    public void storeDocuments(List<Document> documents, String sessionId) {
        log.info("Storing {} document chunks for session: {}", documents.size(), sessionId);

        // Add session ID to metadata for filtering
        documents.forEach(doc -> {
            Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
            metadata.put("sessionId", sessionId);
            doc.getMetadata().putAll(metadata);
        });

        vectorStore.add(documents);
        log.info("Successfully stored documents in vector database");
    }

    /**
     * Queries documents using RAG pipeline.
     *
     * @param question user question
     * @param sessionId user session identifier
     * @return chat response with answer and sources
     */
    public ChatResponse queryDocuments(String question, String sessionId) {
        log.info("Processing query for session {}: {}", sessionId, question);

        try {
            // Retrieve relevant documents
            List<Document> relevantDocs = retrieveRelevantDocuments(question, sessionId);

            if (relevantDocs.isEmpty()) {
                log.warn("No relevant documents found for query: {}", question);
                return ChatResponse.builder()
                        .answer("I couldn't find relevant information in the uploaded documents to answer your question.")
                        .foundInDocuments(false)
                        .confidence(0.0)
                        .timestamp(LocalDateTime.now())
                        .sources(Collections.emptyList())
                        .build();
            }

            // Prepare context from retrieved documents
            String context = prepareContext(relevantDocs);
            log.debug("Retrieved context length: {} characters", context.length());

            // Generate answer using LLM
            String answer = generateAnswer(question, context);

            // Extract sources
            List<String> sources = extractSources(relevantDocs);

            // Calculate confidence based on similarity scores
            double confidence = calculateConfidence(relevantDocs);

            return ChatResponse.builder()
                    .answer(answer)
                    .foundInDocuments(true)
                    .confidence(confidence)
                    .sources(sources)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error processing query: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .answer("I encountered an error while processing your question. Please try again.")
                    .foundInDocuments(false)
                    .confidence(0.0)
                    .timestamp(LocalDateTime.now())
                    .sources(Collections.emptyList())
                    .build();
        }
    }

    /**
     * Retrieves relevant document chunks based on similarity search.
     *
     * @param query user query
     * @param sessionId user session identifier
     * @return list of relevant documents
     */
    private List<Document> retrieveRelevantDocuments(String query, String sessionId) {

        SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(appProperties.getRag().getTopK())
                .withSimilarityThreshold(appProperties.getRag().getSimilarityThreshold());

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        // Filter by session AFTER retrieval
        return results.stream()
                .filter(doc -> sessionId.equals(doc.getMetadata().get("sessionId")))
                .toList();
    }


    /**
     * Prepares context from retrieved documents.
     *
     * @param documents retrieved documents
     * @return formatted context string
     */
    private String prepareContext(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    String fileName = (String) doc.getMetadata().get("fileName");
                    String content = doc.getContent();
                    return String.format("[From: %s]\n%s", fileName, content);
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * Generates answer using LLM with RAG context.
     *
     * @param question user question
     * @param context retrieved context
     * @return generated answer
     */
    private String generateAnswer(String question, String context) {
        PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);

        Map<String, Object> params = new HashMap<>();
        params.put("context", context);
        params.put("question", question);

        Prompt prompt = promptTemplate.create(params);

        ChatClient chatClient = chatClientBuilder.build();
        String answer = chatClient.prompt(prompt).call().content();

        log.debug("Generated answer: {}", answer);
        return answer;
    }

    /**
     * Extracts unique source file names from documents.
     *
     * @param documents retrieved documents
     * @return list of source file names
     */
    private List<String> extractSources(List<Document> documents) {
        return documents.stream()
                .map(doc -> (String) doc.getMetadata().get("fileName"))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Calculates confidence score based on document similarities.
     *
     * @param documents retrieved documents
     * @return confidence score (0.0 to 1.0)
     */
    private double calculateConfidence(List<Document> documents) {
        if (documents.isEmpty()) {
            return 0.0;
        }

        // Average similarity scores if available
        double avgScore = documents.stream()
                .filter(doc -> doc.getMetadata().containsKey("distance"))
                .mapToDouble(doc -> {
                    Object distance = doc.getMetadata().get("distance");
                    return distance instanceof Number ?
                            1.0 - ((Number) distance).doubleValue() : 0.5;
                })
                .average()
                .orElse(0.5);

        return Math.min(1.0, Math.max(0.0, avgScore));
    }

    /**
     * Deletes all documents for a session from vector store.
     *
     * @param sessionId user session identifier
     */
    public void clearSessionDocuments(String sessionId) {
        log.info("Clearing documents for session: {}", sessionId);
        try {
            // Use custom method if available
            if (vectorStore instanceof com.docuchat.vectorstore.CustomRedisVectorStore) {
                ((com.docuchat.vectorstore.CustomRedisVectorStore) vectorStore)
                        .deleteBySession(sessionId);
            }
            log.info("Successfully cleared documents for session: {}", sessionId);
        } catch (Exception e) {
            log.error("Error clearing session documents: {}", e.getMessage(), e);
        }
    }
}

package com.docuchat.controller;

import com.docuchat.model.ChatMessage;
import com.docuchat.model.UserSession;
import com.docuchat.model.dto.ApiResponse;
import com.docuchat.model.dto.ChatRequest;
import com.docuchat.model.dto.ChatResponse;
import com.docuchat.service.LoggingService;
import com.docuchat.service.RagService;
import com.docuchat.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for chat operations.
 *
 * 
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "APIs for chatting with documents")
public class ChatController {

    private final RagService ragService;
    private final SessionService sessionService;
    private final LoggingService loggingService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Processes chat query.
     *
     * @param request chat request
     * @return API response with answer
     */
    @PostMapping("/query")
    @Operation(summary = "Query documents", description = "Ask questions about uploaded documents")
    public ResponseEntity<ApiResponse<ChatResponse>> query(@Valid @RequestBody ChatRequest request) {
        log.info("Processing query for session: {}", request.getSessionId());

        Optional<UserSession> sessionOpt = sessionService.getSession(request.getSessionId());
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Session not found"));
        }

        UserSession session = sessionOpt.get();

        // Log user message
        ChatMessage userMessage = ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sessionId(session.getSessionId())
                .type(ChatMessage.MessageType.TEXT)
                .content(request.getQuestion())
                .timestamp(LocalDateTime.now())
                .fromUser(true)
                .build();
        sessionService.addChatMessage(session.getSessionId(), userMessage);

        // Send user message via WebSocket
        messagingTemplate.convertAndSend(
                "/topic/chat/" + session.getSessionId(),
                userMessage
        );

        try {
            // Get AI response
            ChatResponse response = ragService.queryDocuments(
                    request.getQuestion(),
                    request.getSessionId()
            );

            // Log AI response
            ChatMessage aiMessage = ChatMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .sessionId(session.getSessionId())
                    .type(ChatMessage.MessageType.TEXT)
                    .content(response.getAnswer())
                    .timestamp(response.getTimestamp())
                    .fromUser(false)
                    .sources(response.getSources() != null ?
                            String.join(", ", response.getSources()) : null)
                    .build();
            sessionService.addChatMessage(session.getSessionId(), aiMessage);

            // Send AI response via WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + session.getSessionId(),
                    aiMessage
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error processing query: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process query: " + e.getMessage()));
        }
    }

    /**
     * Exports chat history for a session.
     *
     * @param sessionId session identifier
     * @return chat history as text file
     */
    @GetMapping("/{sessionId}/export")
    @Operation(summary = "Export chat history", description = "Downloads chat history as text file")
    public ResponseEntity<String> exportHistory(@PathVariable String sessionId) {
        Optional<UserSession> sessionOpt = sessionService.getSession(sessionId);

        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String history = loggingService.exportChatHistory(sessionOpt.get());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment",
                "chat-history-" + sessionId + ".txt");

        return ResponseEntity.ok()
                .headers(headers)
                .body(history);
    }

    /**
     * Gets chat history for a session.
     *
     * @param sessionId session identifier
     * @return API response with chat history
     */
    @GetMapping("/{sessionId}/history")
    @Operation(summary = "Get chat history", description = "Retrieves all messages for a session")
    public ResponseEntity<ApiResponse<java.util.List<ChatMessage>>> getHistory(
            @PathVariable String sessionId) {

        Optional<UserSession> session = sessionService.getSession(sessionId);

        return session.map(s -> ResponseEntity.ok(
                        ApiResponse.success(s.getChatHistory())
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Session not found")));
    }
}
package com.docuchat.controller;

import com.docuchat.model.ChatMessage;
import com.docuchat.model.UserSession;
import com.docuchat.model.dto.*;
import com.docuchat.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for user session management.
 *
 * 
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
@Tag(name = "Session Management", description = "APIs for managing user sessions")
public class SessionController {

    private final SessionService sessionService;

    /**
     * Creates a new user session.
     *
     * @param userInfo user information
     * @param request HTTP request
     * @return API response with session details
     */
    @PostMapping("/create")
    @Operation(summary = "Create new session", description = "Creates a new user session with provided details")
    public ResponseEntity<ApiResponse<UserSession>> createSession(
            @Valid @RequestBody UserInfoRequest userInfo,
            HttpServletRequest request) {

        try {
            UserSession session = sessionService.createSession(userInfo, request);
            return ResponseEntity.ok(
                    ApiResponse.success("Session created successfully", session)
            );
        } catch (Exception e) {
            log.error("Error creating session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create session: " + e.getMessage()));
        }
    }

    /**
     * Retrieves session by ID.
     *
     * @param sessionId session identifier
     * @return API response with session details
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session", description = "Retrieves session details by ID")
    public ResponseEntity<ApiResponse<UserSession>> getSession(@PathVariable String sessionId) {
        Optional<UserSession> session = sessionService.getSession(sessionId);

        return session.map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Session not found")));
    }

    /**
     * Ends a user session.
     *
     * @param sessionId session identifier
     * @return API response
     */
    @PostMapping("/{sessionId}/end")
    @Operation(summary = "End session", description = "Ends an active user session")
    public ResponseEntity<ApiResponse<Void>> endSession(@PathVariable String sessionId) {
        try {
            sessionService.endSession(sessionId);
            return ResponseEntity.ok(ApiResponse.success("Session ended successfully", null));
        } catch (Exception e) {
            log.error("Error ending session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to end session"));
        }
    }
}
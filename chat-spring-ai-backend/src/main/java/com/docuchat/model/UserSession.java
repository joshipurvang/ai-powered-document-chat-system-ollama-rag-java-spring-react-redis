package com.docuchat.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user session with uploaded documents and chat history.
 *
 * 
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonTypeName("UserSession")   // ← required when using Id.NAME
public class UserSession {
    private String sessionId;
    private String userName;
    private String phoneNumber;
    private String ipAddress;
    private String location;
    private String deviceInfo;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;

    @Builder.Default
    private List<DocumentMetadata> documents = new ArrayList<>();

    @Builder.Default
    private List<ChatMessage> chatHistory = new ArrayList<>();

    private boolean active;
}
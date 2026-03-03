
package com.docuchat.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a chat message in the conversation.
 *
 * 
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
// OR shorter version (cleaner JSON):
// @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
@JsonTypeName("ChatMessage")
public class ChatMessage {
    private String messageId;
    private String sessionId;
    private MessageType type;
    private String content;
    private LocalDateTime timestamp;
    private boolean fromUser;
    private String sources;

    /**
     * Type of chat message.
     */
    public enum MessageType {
        TEXT,
        SYSTEM,
        ERROR,
        INFO
    }
}
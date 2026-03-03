package com.docuchat.service;

import com.docuchat.config.AppProperties;
import com.docuchat.model.ChatMessage;
import com.docuchat.model.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for logging user information and conversations.
 *
 * 
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {

    private final AppProperties appProperties;
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs user information to file.
     *
     * @param session user session
     */
    public void logUserInfo(UserSession session) {
        try {
            ensureLogDirectoryExists();

            try (PrintWriter writer = new PrintWriter(
                    new FileWriter(appProperties.getLogging().getUserInfoFile(), true))) {

                writer.println("=".repeat(80));
                writer.println("Session ID: " + session.getSessionId());
                writer.println("Name: " + session.getUserName());
                writer.println("Phone: " + session.getPhoneNumber());
                writer.println("IP Address: " + session.getIpAddress());
                writer.println("Location: " + session.getLocation());
                writer.println("Device: " + session.getDeviceInfo());
                writer.println("Created At: " + session.getCreatedAt().format(formatter));
                writer.println("=".repeat(80));
                writer.println();
            }

            log.debug("Logged user info for session: {}", session.getSessionId());

        } catch (IOException e) {
            log.error("Error logging user info: {}", e.getMessage(), e);
        }
    }

    /**
     * Logs conversation message to file.
     *
     * @param session user session
     * @param message chat message
     */
    public void logConversation(UserSession session, ChatMessage message) {
        try {
            ensureLogDirectoryExists();

            try (PrintWriter writer = new PrintWriter(
                    new FileWriter(appProperties.getLogging().getConversationsFile(), true))) {

                writer.printf("[%s] Session: %s | User: %s%n",
                        message.getTimestamp().format(formatter),
                        session.getSessionId(),
                        session.getUserName());

                writer.printf("%s: %s%n",
                        message.isFromUser() ? "USER" : "AI",
                        message.getContent());

                if (message.getSources() != null && !message.getSources().isEmpty()) {
                    writer.printf("Sources: %s%n", message.getSources());
                }

                writer.println("-".repeat(80));
                writer.println();
            }

            log.debug("Logged conversation for session: {}", session.getSessionId());

        } catch (IOException e) {
            log.error("Error logging conversation: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensures log directory exists.
     *
     * @throws IOException if directory creation fails
     */
    private void ensureLogDirectoryExists() throws IOException {
        Path logDir = Paths.get("./logs");
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }
    }

    /**
     * Exports chat history for a session.
     *
     * @param session user session
     * @return chat history as formatted string
     */
    public String exportChatHistory(UserSession session) {
        StringBuilder export = new StringBuilder();

        export.append("Chat History Export\n");
        export.append("=".repeat(80)).append("\n");
        export.append("Session ID: ").append(session.getSessionId()).append("\n");
        export.append("User: ").append(session.getUserName()).append("\n");
        export.append("Date: ").append(LocalDateTime.now().format(formatter)).append("\n");
        export.append("=".repeat(80)).append("\n\n");

        for (ChatMessage message : session.getChatHistory()) {
            export.append(String.format("[%s] %s:\n%s\n\n",
                    message.getTimestamp().format(formatter),
                    message.isFromUser() ? "USER" : "AI",
                    message.getContent()));
        }

        return export.toString();
    }
}
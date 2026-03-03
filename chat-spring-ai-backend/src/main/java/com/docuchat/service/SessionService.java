package com.docuchat.service;

import com.docuchat.config.AppProperties;
import com.docuchat.model.ChatMessage;
import com.docuchat.model.UserSession;
import com.docuchat.model.dto.UserInfoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing user sessions.
 *
 * <p>Handles session creation, retrieval, and cleanup using Redis.</p>
 *
 * 
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    //private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, UserSession> userSessionRedisTemplate;
    private final AppProperties appProperties;
    private final LoggingService loggingService;

    private static final String SESSION_KEY_PREFIX = "session:";

    /**
     * Creates a new user session.
     *
     * @param userInfo user information
     * @param request HTTP request for IP and device details
     * @return created user session
     */
    public UserSession createSession(UserInfoRequest userInfo, HttpServletRequest request) {
        String sessionId = UUID.randomUUID().toString();

        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userName(userInfo.getName())
                .phoneNumber(userInfo.getPhoneNumber())
                .ipAddress(getClientIp(request))
                .deviceInfo(userInfo.getDeviceInfo() != null ?
                        userInfo.getDeviceInfo() :
                        request.getHeader("User-Agent"))
                .location(getLocationFromIp(getClientIp(request)))
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .active(true)
                .documents(new ArrayList<>())
                .chatHistory(new ArrayList<>())
                .build();

        saveSession(session);
        loggingService.logUserInfo(session);

        log.info("Created new session: {} for user: {}", sessionId, userInfo.getName());
        return session;
    }


    /**
     * Retrieves session by ID.
     *
     * @param sessionId session identifier
     * @return user session or empty if not found
     */
    public Optional<UserSession> getSession(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        UserSession session = (UserSession) userSessionRedisTemplate.opsForValue().get(key);

        if (session != null) {
            session.setLastActivityAt(LocalDateTime.now());
            saveSession(session);
        }

        return Optional.ofNullable(session);
    }

    /**
     * Saves or updates session in Redis.
     *
     * @param session user session to save
     */
    public void saveSession(UserSession session) {
        String key = SESSION_KEY_PREFIX + session.getSessionId();
        userSessionRedisTemplate.opsForValue().set(
                key,
                session,
                appProperties.getSession().getTimeoutMinutes(),
                TimeUnit.MINUTES
        );
    }

    /**
     * Adds chat message to session history.
     *
     * @param sessionId session identifier
     * @param message chat message
     */
    public void addChatMessage(String sessionId, ChatMessage message) {
        getSession(sessionId).ifPresent(session -> {
            session.getChatHistory().add(message);
            saveSession(session);
            loggingService.logConversation(session, message);
        });
    }

    /**
     * Ends session and clears data.
     *
     * @param sessionId session identifier
     */
    public void endSession(String sessionId) {
        getSession(sessionId).ifPresent(session -> {
            session.setActive(false);
            saveSession(session);
            log.info("Ended session: {}", sessionId);
        });
    }

    /**
     * Deletes session from Redis.
     *
     * @param sessionId session identifier
     */
    public void deleteSession(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        userSessionRedisTemplate.delete(key);
        log.info("Deleted session: {}", sessionId);
    }

    /**
     * Extracts client IP address from request.
     *
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Gets approximate location from IP address.
     *
     * @param ip IP address
     * @return location string
     */
    private String getLocationFromIp(String ip) {
        // Simplified implementation - in production, use GeoIP database
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            return "localhost";
        }
        return "Unknown";
    }
}
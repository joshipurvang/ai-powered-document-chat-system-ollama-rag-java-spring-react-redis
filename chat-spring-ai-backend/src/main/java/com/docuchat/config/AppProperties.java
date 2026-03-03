package com.docuchat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Application-specific configuration properties.
 *
 * <p>Binds properties from application.yml with 'app' prefix.</p>
 *
 * 
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Upload upload = new Upload();
    private Rag rag = new Rag();
    private Session session = new Session();
    private Logging logging = new Logging();

    /**
     * File upload configuration.
     */
    @Data
    public static class Upload {
        private String directory;
        private List<String> allowedTypes;
        private Integer maxFiles;
        private Integer maxFileSizeMb;
    }

    /**
     * RAG pipeline configuration.
     */
    @Data
    public static class Rag {
        private Integer chunkSize;
        private Integer chunkOverlap;
        private Double similarityThreshold;
        private Integer topK;
    }

    /**
     * Session management configuration.
     */
    @Data
    public static class Session {
        private Integer timeoutMinutes;
    }

    /**
     * Logging configuration.
     */
    @Data
    public static class Logging {
        private String conversationsFile;
        private String userInfoFile;
    }
}
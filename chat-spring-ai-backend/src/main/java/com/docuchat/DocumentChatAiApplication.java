package com.docuchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Document Chat AI System.
 *
 * <p>This application provides a RAG (Retrieval Augmented Generation) based
 * document chat system that allows users to upload documents and query them
 * using AI-powered natural language processing.</p>
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Document upload and processing (PDF, DOCX, PPTX, TXT)</li>
 *   <li>Vector-based document storage using Redis</li>
 *   <li>Real-time chat using WebSocket</li>
 *   <li>Ollama-based LLM integration</li>
 *   <li>Session management and conversation logging</li>
 * </ul>
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DocumentChatAiApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(DocumentChatAiApplication.class, args);
    }

}

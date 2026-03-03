
package com.docuchat.controller;

import com.docuchat.config.AppProperties;
import com.docuchat.model.DocumentMetadata;
import com.docuchat.model.UserSession;
import com.docuchat.model.dto.ApiResponse;
import com.docuchat.model.dto.UploadResponse;
import com.docuchat.service.DocumentProcessingService;
import com.docuchat.service.RagService;
import com.docuchat.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for document upload and management.
 *
 * 
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "APIs for uploading and managing documents")
public class DocumentController {

    private final DocumentProcessingService documentProcessingService;
    private final RagService ragService;
    private final SessionService sessionService;
    private final AppProperties appProperties;

    /**
     * Uploads documents for a session.
     *
     * @param sessionId session identifier
     * @param files uploaded files
     * @return API response with upload status
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload documents", description = "Uploads up to 3 documents for processing")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadDocuments(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("files") MultipartFile[] files) {

        log.info("Uploading {} files for session: {}", files.length, sessionId);

        Optional<UserSession> sessionOpt = sessionService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Session not found"));
        }

        UserSession session = sessionOpt.get();

        // Validate file count
        if (files.length > appProperties.getUpload().getMaxFiles()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "Maximum " + appProperties.getUpload().getMaxFiles() + " files allowed"
                    ));
        }

        List<UploadResponse.DocumentStatus> documentStatuses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String documentId = UUID.randomUUID().toString();

                // Process document
                List<Document> chunks = documentProcessingService.processDocument(file, sessionId);

                // Store in vector database
                ragService.storeDocuments(chunks, sessionId);

                // Create metadata
                DocumentMetadata metadata = documentProcessingService.createMetadata(
                        file, documentId, chunks.size()
                );
                session.getDocuments().add(metadata);

                documentStatuses.add(UploadResponse.DocumentStatus.builder()
                        .fileName(file.getOriginalFilename())
                        .processed(true)
                        .status("SUCCESS")
                        .build());

            } catch (Exception e) {
                log.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                documentStatuses.add(UploadResponse.DocumentStatus.builder()
                        .fileName(file.getOriginalFilename())
                        .processed(false)
                        .status("FAILED")
                        .error(e.getMessage())
                        .build());
            }
        }

        sessionService.saveSession(session);

        UploadResponse response = UploadResponse.builder()
                .sessionId(sessionId)
                .documents(documentStatuses)
                .message("Documents processed")
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Gets documents for a session.
     *
     * @param sessionId session identifier
     * @return API response with document list
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get documents", description = "Retrieves all documents for a session")
    public ResponseEntity<ApiResponse<List<DocumentMetadata>>> getDocuments(
            @PathVariable String sessionId) {

        Optional<UserSession> session = sessionService.getSession(sessionId);

        return session.map(s -> ResponseEntity.ok(
                        ApiResponse.success(s.getDocuments())
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Session not found")));
    }

    /**
     * Clears all documents for a session.
     *
     * @param sessionId session identifier
     * @return API response
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Clear documents", description = "Removes all documents for a session")
    public ResponseEntity<ApiResponse<Void>> clearDocuments(@PathVariable String sessionId) {
        try {
            ragService.clearSessionDocuments(sessionId);

            Optional<UserSession> sessionOpt = sessionService.getSession(sessionId);
            sessionOpt.ifPresent(session -> {
                session.getDocuments().clear();
                sessionService.saveSession(session);
            });

            return ResponseEntity.ok(ApiResponse.success("Documents cleared successfully", null));
        } catch (Exception e) {
            log.error("Error clearing documents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to clear documents"));
        }
    }
}
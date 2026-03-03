package com.docuchat.service;

import com.docuchat.config.AppProperties;
import com.docuchat.model.DocumentMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for processing and extracting text from various document formats.
 *
 * <p>Supports: PDF, DOCX, PPTX, TXT</p>
 *
 * 
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService{

    private final AppProperties appProperties;
    private final TokenTextSplitter textSplitter;

    /**
     * Processes uploaded file and extracts text content.
     *
     * @param file uploaded multipart file
     * @param sessionId user session identifier
     * @return list of document chunks with metadata
     * @throws IOException if file processing fails
     */
    public List<Document> processDocument(MultipartFile file, String sessionId) throws IOException {
        log.info("Processing document: {} for session: {}", file.getOriginalFilename(), sessionId);

        validateFile(file);

        String fileName = file.getOriginalFilename();
        String fileType = file.getContentType();
        String extractedText = extractText(file);

        log.debug("Extracted {} characters from {}", extractedText.length(), fileName);

        // Create metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("fileType", fileType);
        metadata.put("sessionId", sessionId);
        metadata.put("uploadTime", LocalDateTime.now().toString());
        metadata.put("fileSize", file.getSize());

        // Create document and split into chunks
        Document document = new Document(extractedText, metadata);
        List<Document> chunks = textSplitter.split(document);

        log.info("Document {} split into {} chunks", fileName, chunks.size());

        return chunks;
    }

    /**
     * Extracts text from uploaded file based on file type.
     *
     * @param file multipart file
     * @return extracted text content
     * @throws IOException if extraction fails
     */
    private String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        return switch (contentType) {
            case "application/pdf" -> extractFromPdf(file);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    extractFromDocx(file);
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    extractFromPptx(file);
            case "text/plain" -> extractFromText(file);
            default -> throw new IllegalArgumentException("Unsupported file type: " + contentType);
        };
    }

    /**
     * Extracts text from PDF file.
     *
     * @param file PDF file
     * @return extracted text
     * @throws IOException if extraction fails
     */
    private String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Extracts text from DOCX file.
     *
     * @param file DOCX file
     * @return extracted text
     * @throws IOException if extraction fails
     */
    private String extractFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * Extracts text from PPTX file.
     *
     * @param file PPTX file
     * @return extracted text
     * @throws IOException if extraction fails
     */
    private String extractFromPptx(MultipartFile file) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow(file.getInputStream())) {
            StringBuilder text = new StringBuilder();
            for (XSLFSlide slide : ppt.getSlides()) {
                for (var shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        text.append(textShape.getText()).append("\n");
                    }
                }
            }
            return text.toString();
        }
    }

    /**
     * Extracts text from plain text file.
     *
     * @param file text file
     * @return extracted text
     * @throws IOException if extraction fails
     */
    private String extractFromText(MultipartFile file) throws IOException {
        return new String(file.getBytes());
    }

    /**
     * Validates uploaded file against size and type constraints.
     *
     * @param file multipart file to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        long maxSize = appProperties.getUpload().getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed size of " +
                            appProperties.getUpload().getMaxFileSizeMb() + "MB"
            );
        }

        if (!appProperties.getUpload().getAllowedTypes().contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "File type not allowed: " + file.getContentType()
            );
        }
    }

    /**
     * Creates document metadata from multipart file.
     *
     * @param file multipart file
     * @param documentId unique document identifier
     * @param chunkCount number of chunks created
     * @return document metadata
     */
    public DocumentMetadata createMetadata(MultipartFile file, String documentId, int chunkCount) {
        return DocumentMetadata.builder()
                .documentId(documentId)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .chunkCount(chunkCount)
                .status("PROCESSED")
                .build();
    }
}
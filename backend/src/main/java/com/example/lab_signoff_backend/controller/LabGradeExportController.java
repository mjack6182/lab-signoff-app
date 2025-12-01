package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.service.LabGradeExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

/**
 * REST controller exposing CSV exports for labs.
 */
@RestController
@RequestMapping("/api/labs")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "http://localhost:5002",
                "https://lab-signoff-app.web.app",
                "https://lab-signoff-app.firebaseapp.com"
        },
        allowCredentials = "true"
)
public class LabGradeExportController {

    private static final Logger log = LoggerFactory.getLogger(LabGradeExportController.class);
    private final LabGradeExportService exportService;

    public LabGradeExportController(LabGradeExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Generate and download a Canvas-compatible CSV for a single lab.
     */
    @GetMapping("/{labId}/grades/export")
    public ResponseEntity<byte[]> exportLabGrades(@PathVariable String labId) {
        try {
            LabGradeExportService.ExportResult result = exportService.generateCsv(labId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(result.getFileName())
                    .build());
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return new ResponseEntity<>(result.getContent(), headers, HttpStatus.OK);
        } catch (NoSuchElementException notFound) {
            log.warn("Lab export not found for {}: {}", labId, notFound.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(notFound.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("Failed to generate CSV for lab {}", labId, ex);
            String message = "There was a problem generating the CSV. Please try again or contact support.";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(message.getBytes(StandardCharsets.UTF_8));
        }
    }
}

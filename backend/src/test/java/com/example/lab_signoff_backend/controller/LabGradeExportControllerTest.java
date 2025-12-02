package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.service.LabGradeExportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class LabGradeExportControllerTest {

    @Test
    void exportSuccess_returnsCsvBytes() {
        LabGradeExportService exportService = mock(LabGradeExportService.class);
        LabGradeExportService.ExportResult result = new LabGradeExportService.ExportResult("file.csv", "a,b,c\n".getBytes());
        when(exportService.generateCsv("lab-1")).thenReturn(result);

        LabGradeExportController controller = new LabGradeExportController(exportService);
        ResponseEntity<byte[]> resp = controller.exportLabGrades("lab-1");

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("file.csv", resp.getHeaders().getContentDisposition().getFilename());
        assertTrue(new String(resp.getBody(), StandardCharsets.UTF_8).startsWith("a,b"));
    }

    @Test
    void exportNotFound_returns404() {
        LabGradeExportService exportService = mock(LabGradeExportService.class);
        when(exportService.generateCsv("missing")).thenThrow(new NoSuchElementException("no lab"));

        LabGradeExportController controller = new LabGradeExportController(exportService);
        ResponseEntity<byte[]> resp = controller.exportLabGrades("missing");

        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void exportFailure_returns500() {
        LabGradeExportService exportService = mock(LabGradeExportService.class);
        when(exportService.generateCsv("fail")).thenThrow(new IllegalStateException("boom"));

        LabGradeExportController controller = new LabGradeExportController(exportService);
        ResponseEntity<byte[]> resp = controller.exportLabGrades("fail");

        assertEquals(500, resp.getStatusCode().value());
    }
}

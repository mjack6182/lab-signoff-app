package com.example.lab_signoff_backend.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WideWithPointsExporterTest {

    @TempDir
    Path tmp;

    @Test
    void exportsWideCsvWithPointsPossibleRow() throws Exception {
        var rows = List.of(
                new CanvasLabsImporter.StudentLabScore("Student One", "1001", "Lab A", new BigDecimal("4"), new BigDecimal("5")),
                new CanvasLabsImporter.StudentLabScore("Student Two", "1002", "Lab A", null, new BigDecimal("5"))
        );
        var exporter = new WideWithPointsExporter();
        Path out = tmp.resolve("wide.csv");

        exporter.export(out, rows, List.of("Lab A"), Map.of("Lab A", new BigDecimal("5")));

        var lines = Files.readAllLines(out);
        assertTrue(lines.get(0).startsWith("Student,ID,Lab A"));
        assertTrue(lines.get(1).contains("Points Possible"));
        assertEquals(4, lines.size()); // header + points possible + one row per student
        assertTrue(lines.get(2).contains("Student One"));
        assertTrue(lines.get(3).contains("Student Two"));
    }

    @Test
    void exportsMultipleLabsPreservesOrdering() throws Exception {
        var rows = List.of(
                new CanvasLabsImporter.StudentLabScore("A", "1", "Lab B", new BigDecimal("2"), new BigDecimal("5")),
                new CanvasLabsImporter.StudentLabScore("A", "1", "Lab A", new BigDecimal("3"), new BigDecimal("5"))
        );
        var exporter = new WideWithPointsExporter();
        Path out = tmp.resolve("multi.csv");

        exporter.export(out, rows, List.of("Lab A", "Lab B"), Map.of("Lab A", new BigDecimal("5"), "Lab B", new BigDecimal("5")));

        var lines = Files.readAllLines(out);
        assertTrue(lines.get(0).contains("Lab A,Lab B"));
        assertTrue(lines.get(2).endsWith("3,2")); // scores follow lab order
    }
}

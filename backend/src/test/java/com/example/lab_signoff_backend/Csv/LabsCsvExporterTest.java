package com.example.lab_signoff_backend.csv;

import com.example.lab_signoff_backend.csv.CanvasLabsImporter;
import com.example.lab_signoff_backend.csv.LabsCsvExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LabsCsvExporterTest {

    @TempDir
    Path tmp;

    @Test
    void exportsLongFormatCsv() throws Exception {
        var rows = List.of(
                new CanvasLabsImporter.StudentLabScore("Student One", "1001", "Lab A", new BigDecimal("3"), new BigDecimal("5")),
                new CanvasLabsImporter.StudentLabScore("Student Two", "1002", "Lab B", null, new BigDecimal("4"))
        );
        Path out = tmp.resolve("long.csv");
        new LabsCsvExporter().new WideWithPointsExporter()
                .export(out, rows, List.of("Lab A", "Lab B"), Map.of("Lab A", new BigDecimal("5"), "Lab B", new BigDecimal("4")));

        var lines = Files.readAllLines(out);
        assertTrue(lines.get(0).startsWith("Student,ID,Lab A"));
        assertTrue(lines.get(1).contains("Points Possible"));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Student One")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Student Two")));
    }

    @Test
    void writesEmptyScoreCells() throws Exception {
        List<CanvasLabsImporter.StudentLabScore> rows = List.of(
                new CanvasLabsImporter.StudentLabScore("Student One", "1001", "Lab A", null, new BigDecimal("5")),
                new CanvasLabsImporter.StudentLabScore("Student One", "1001", "Lab B", new BigDecimal("4"), new BigDecimal("10"))
        );
        Path out = tmp.resolve("labs-empty.csv");
        new LabsCsvExporter().new WideWithPointsExporter()
                .export(out, rows, List.of("Lab A", "Lab B"), Map.of("Lab A", new BigDecimal("5"), "Lab B", new BigDecimal("10")));

        String content = Files.readString(out);
        String[] lines = content.split("\\R");
        assertTrue(lines[1].startsWith("Points Possible"));
        // first student row has blank for Lab A, score for Lab B
        assertTrue(lines[2].contains("Student One,1001,,4"));
    }
}

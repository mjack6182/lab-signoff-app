package com.example.lab_signoff_backend.csv;

import com.example.lab_signoff_backend.csv.CanvasLabsImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CanvasLabsImporterTest {

    @TempDir
    Path tmp;

    @Test
    void normalize_handlesPointsPossibleAndBlankScores() throws Exception {
        String csv = String.join("\n",
                "Student,ID,Laboratory for Module 01 (12345)",
                "Points Possible,,5",
                "Student One,1001,",
                "Student Two,1002,4"
        );
        Path file = tmp.resolve("grades.csv");
        Files.writeString(file, csv);

        var result = new CanvasLabsImporter().normalize(file);

        assertEquals(2, result.rows.size());
        assertFalse(result.labColumns.isEmpty());
        assertTrue(result.pointsPossibleMap.values().stream().anyMatch(v -> v.intValue() == 5));
        // One blank score and one numeric
        assertNull(result.rows.get(0).score());
        assertNotNull(result.rows.get(1).score());
    }

    @Test
    void normalize_skipsEmptyRowsAndCollectsMultipleLabs() throws Exception {
        String csv = String.join("\n",
                "Student,ID,SIS User ID,SIS Login ID,Laboratory for Lab A (111),Laboratory for Lab B (222)",
                "Points Possible,,, ,10,",
                ",,, , ,",
                "Alice,1001,,alice@x,9,",
                "Bob,,SID2,, ,8"
        );
        Path file = tmp.resolve("multi.csv");
        Files.writeString(file, csv);

        var result = new CanvasLabsImporter().normalize(file);

        assertEquals(2, result.labColumns.size());
        // 2 students x 2 labs = 4 rows (plus header skip)
        assertEquals(4, result.rows.size());
        assertEquals("Alice", result.rows.getFirst().student());
        assertEquals("Bob", result.rows.get(2).student());
        assertEquals("Laboratory for Lab B (222)", result.rows.get(3).labTitle());
        assertEquals(new java.math.BigDecimal("10"), result.pointsPossibleMap.get("Laboratory for Lab A (111)"));
    }
}

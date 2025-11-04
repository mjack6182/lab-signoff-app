package com.example.lab_signoff_backend.Csv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.lab_signoff_backend.csv.CanvasLabsImporter;
import com.example.lab_signoff_backend.csv.NormalizedLabsExporter;
import com.example.lab_signoff_backend.csv.WideWithPointsExporter;

import java.net.URISyntaxException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CanvasLabsRoundTripTest {

  private static Path resource(String name) {
    try { return Paths.get(CanvasLabsRoundTripTest.class.getClassLoader().getResource(name).toURI()); }
    catch (URISyntaxException e) { throw new RuntimeException("Missing test resource: "+name, e); }
  }

  @TempDir Path tmp;

  @Test
  void importThenExport_allStudentsAllLabs_evenIfEmpty() throws Exception {
    Path csv = resource("2025-10-30T1841_Grades-LabSignoffProject.SANDBOX.csv");

    // Import (normalize; includes blank scores by design)
    var norm = new CanvasLabsImporter().normalize(csv);

    // Export normalized (long)
    Path outLong = tmp.resolve("normalized.csv");
    new NormalizedLabsExporter().export(outLong, norm.rows);
    assertTrue(Files.exists(outLong));

    // Export wide with Points Possible row
    Path outWide = tmp.resolve("wide-with-pp.csv");
    new WideWithPointsExporter().export(outWide, norm.rows, norm.labColumns, norm.pointsPossibleMap);
    assertTrue(Files.exists(outWide));

    // sanity checks
    var longHead = Files.readAllLines(outLong).get(0);
    assertEquals("student,ID,labTitle,score,pointsPossible", longHead);

    var wideHead = Files.readAllLines(outWide).get(0);
    assertTrue(wideHead.startsWith("Student,ID,")); // followed by lab columns

    System.out.println("==== Normalized Rows (student x lab) ====");
    for (var r : norm.rows) {
      System.out.printf("%s | %s | %s | %s | %s%n",
          r.student(), r.studentId(), r.labTitle(),
          r.score() == null ? "" : r.score().toPlainString(),
          r.pointsPossible() == null ? "" : r.pointsPossible().toPlainString()
      );
    }
}
}
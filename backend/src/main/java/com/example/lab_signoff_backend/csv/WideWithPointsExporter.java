package com.example.lab_signoff_backend.csv;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.Writer;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WideWithPointsExporter {
  public void export(Path path, List<CanvasLabsImporter.StudentLabScore> rows,
                     List<String> labColumns, Map<String, BigDecimal> pointsPossible) throws IOException {
    if (path.getParent()!=null) Files.createDirectories(path.getParent());

    // Group scores by student → (lab → score)
    record Key(String student, String id){}
    Map<Key, Map<String, BigDecimal>> byStudent = new LinkedHashMap<>();
    for (var r: rows) {
      var key = new Key(nz(r.student()), nz(r.studentId()));
      byStudent.computeIfAbsent(key, k -> new HashMap<>())
               .put(r.labTitle(), r.score());
    }

    try (Writer out = Files.newBufferedWriter(path);
         CSVPrinter csv = new CSVPrinter(out, CSVFormat.RFC4180)) {

      // Header
      List<String> header = new ArrayList<>();
      header.add("Student"); header.add("ID");
      header.addAll(labColumns);
      csv.printRecord(header);

      // Points Possible row
      List<String> ppRow = new ArrayList<>();
      ppRow.add("Points Possible"); ppRow.add("");
      for (String lab : labColumns) {
        var pp = pointsPossible.get(lab);
        ppRow.add(pp == null ? "" : pp.stripTrailingZeros().toPlainString());
      }
      csv.printRecord(ppRow);

      // Student rows (blank scores stay blank)
      for (var e : byStudent.entrySet()) {
        List<String> line = new ArrayList<>();
        line.add(e.getKey().student());
        line.add(e.getKey().id());
        var scores = e.getValue();
        for (String lab : labColumns) {
          var s = scores.get(lab);
          line.add(s == null ? "" : s.stripTrailingZeros().toPlainString());
        }
        csv.printRecord(line);
      }
    }
  }

  private static String nz(String s){ return s==null? "": s; }
}

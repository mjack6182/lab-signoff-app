package com.example.lab_signoff_backend.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.Writer;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NormalizedLabsExporter {
  private static String fmt(BigDecimal d){ return d==null? "": d.stripTrailingZeros().toPlainString(); }
  public void export(Path path, List<CanvasLabsImporter.StudentLabScore> rows) throws IOException {
    if (path.getParent()!=null) Files.createDirectories(path.getParent());
    try (Writer out = Files.newBufferedWriter(path);
         CSVPrinter csv = new CSVPrinter(out,
             CSVFormat.RFC4180.builder().setHeader("student","ID","labTitle","score","pointsPossible").build())) {
      for (var r: rows) {
        csv.printRecord(
          nz(r.student()), nz(r.studentId()), nz(r.labTitle()),
          fmt(r.score()), fmt(r.pointsPossible()));
      }
    }
  }
  private static String nz(String s){ return s==null? "": s; }
}
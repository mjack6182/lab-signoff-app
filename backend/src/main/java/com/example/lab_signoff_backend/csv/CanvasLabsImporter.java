package com.example.lab_signoff_backend.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class CanvasLabsImporter {
private static final Pattern LAB_COL = Pattern.compile("^Laboratory\\s+.*\\(\\d+\\)$");

  public record StudentLabScore(
      String student, String studentId, String labTitle,
      BigDecimal score, BigDecimal pointsPossible) {}

  public static class Result {
    public final List<StudentLabScore> rows;
    public final List<String> labColumns;                    // all labs detected
    public final Map<String, BigDecimal> pointsPossibleMap;  // lab -> max points
    public Result(List<StudentLabScore> rows, List<String> labColumns,
                  Map<String, BigDecimal> pointsPossibleMap) {
      this.rows = rows; this.labColumns = labColumns; this.pointsPossibleMap = pointsPossibleMap;
    }
  }

  public Result normalize(Path csvPath) throws IOException {
    // Parse once, keep all records
    List<CSVRecord> all;
    List<String> headers;
    try (Reader reader = Files.newBufferedReader(csvPath)) {
      CSVParser p = CSVFormat.RFC4180.builder()
          .setHeader()
          .setSkipHeaderRecord(true)
          .setTrim(true)
          .setIgnoreEmptyLines(true)
          .get()
          .parse(reader);
      headers = p.getHeaderNames();
      all = p.getRecords();
    }

    // Detect lab columns
    List<String> labs = headers.stream().filter(h -> LAB_COL.matcher(h).matches()).toList();

    // Collect “Points Possible”
    Map<String, BigDecimal> pp = new HashMap<>();
    for (CSVRecord r : all) {
      if ("Points Possible".equalsIgnoreCase(get(r, "Student"))) {
        for (String lab : labs) pp.put(lab, dec(get(r, lab)));
        break;
      }
    }

    // Emit EVERY student × lab, even when score is blank
    List<StudentLabScore> out = new ArrayList<>();
    for (CSVRecord r : all) {
      String student = get(r, "Student");
      if (student.equalsIgnoreCase("Points Possible")) continue;

      String id = firstNonBlank(get(r,"ID"), get(r,"SIS User ID"), get(r,"SIS Login ID"));
      // skip truly empty rows
      if (isBlank(student) && isBlank(id)) continue;

      for (String lab : labs) {
        BigDecimal score = dec(get(r, lab)); // may be null
        out.add(new StudentLabScore(student, id, lab, score, pp.get(lab)));
      }
    }

    return new Result(out, labs, pp);
  }

  // helpers
  private static String get(CSVRecord r, String col) {
    return (r != null && r.isMapped(col) && r.get(col) != null) ? r.get(col).trim() : "";
  }
  private static boolean isBlank(String s){ return s==null || s.isBlank(); }
  private static String firstNonBlank(String... vals){ for (var v: vals) if(!isBlank(v)) return v; return ""; }
  private static BigDecimal dec(String s){ if (isBlank(s)) return null; try { return new BigDecimal(s); } catch(Exception e){ return null; } }
}
package com.example.lab_signoff_backend.ags;

import com.example.lab_signoff_backend.ags.dto.CheckpointDto;
// ScorePublishRequest DTO is provided as an inner static class below to avoid an external dependency.
import com.example.lab_signoff_backend.ags.dto.ScorePublishResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/mock/ags/grades")
public class MockAgsController {

    private static final Logger log = LoggerFactory.getLogger(MockAgsController.class);

    @Value("${app.mocks.ags.enabled:true}")
    private boolean mockEnabled;

    @Value("${app.mocks.ags.retention-seconds:3600}")
    private long retentionSeconds;

    @Value("${app.mocks.ags.redact-comments:true}")
    private boolean redactComments;

    private static final Deque<Stored> HISTORY = new ConcurrentLinkedDeque<>();
    private record Stored(ScorePublishResponse resp, long expiresAtMs) {}
    private static final int MAX_HISTORY = 50;
    private static final AtomicLong SEQ = new AtomicLong(1);

    private static final Pattern EMAIL = Pattern.compile(".+@.+\\..+");

    private static boolean looksLikeEmail(String s) {
        return s != null && EMAIL.matcher(s).matches();
    }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static void bad(String msg) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }

    @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE,
                                  produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScorePublishResponse> sync(@RequestBody ScorePublishRequest req) {
        if (!mockEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        // Basic fields
        if (isBlank(req.courseId)) bad("courseId is required");
        if (isBlank(req.checkpointId)) bad("checkpointId is required");

        boolean hasUsers = req.userIds != null && !req.userIds.isEmpty();
        boolean hasGroup = !isBlank(req.groupId);
        if (!hasUsers && !hasGroup) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Provide groupId or userIds[]");
        }

        // PII guard
        if (hasUsers) for (String uid : req.userIds) if (looksLikeEmail(uid)) bad("userIds must not contain emails (PII)");
        if (hasGroup && looksLikeEmail(req.groupId)) bad("groupId must not be an email (PII)");

        // Redact comments by default
        if (redactComments) req.comment = null;
        else if (req.comment != null && req.comment.length() > 120) req.comment = req.comment.substring(0, 120);
        if (req.comment != null && looksLikeEmail(req.comment)) req.comment = null;

        // Timestamp & line item
        String ts = isBlank(req.timestamp) ? Instant.now().toString() : req.timestamp;
        String lineItemId = isBlank(req.lineItemId) ? "mock-lineitem-" + req.checkpointId : req.lineItemId;

        // === Compute score if checkpoints provided; otherwise use provided score fields ===
        ScoreFormulaService.Result res;
        boolean willCompute = req.checkpoints != null && !req.checkpoints.isEmpty();
        if (willCompute) {
            // validate each checkpoint minimally
            for (CheckpointDto cp : req.checkpoints) {
                if (cp == null || cp.state == null) bad("each checkpoint requires a state");
            }
            res = new ScoreFormulaService().computeFrom(req);
        } else {
            // Back-compat: validate supplied scores/progress
            if (req.scoreMaximum == null || req.scoreMaximum <= 0) bad("scoreMaximum must be > 0");
            if (req.scoreGiven == null || req.scoreGiven < 0 || req.scoreGiven > req.scoreMaximum)
                bad("scoreGiven must be between 0 and scoreMaximum");
            String ap = req.activityProgress != null ? req.activityProgress.name() : "InProgress";
            String gp = req.gradingProgress  != null ? req.gradingProgress.name()  : "Pending";
            res = new ScoreFormulaService.Result(req.scoreGiven, req.scoreMaximum, ap, gp);
        }

        // Result IDs
        List<String> resultIds = new ArrayList<>();
        if (hasUsers) for (String uid : req.userIds) resultIds.add("mock-result-" + uid);
        else resultIds.add("mock-result-" + req.groupId);

        // Build response
        ScorePublishResponse resp = new ScorePublishResponse();
        resp.mockSyncId = "mock-sync-" + SEQ.getAndIncrement();
        resp.courseId = req.courseId;
        resp.lineItemId = lineItemId;
        resp.resultIds = resultIds;
        resp.syncedCount = resultIds.size();
        resp.checkpointId = req.checkpointId;
        // resp.scoreGiven = res.scoreGiven();
        // resp.scoreMaximum = res.scoreMaximum();
        resp.activityProgress = ActivityOrDefault(res.activityProgress());
        resp.gradingProgress  = GradingOrDefault(res.gradingProgress());
        resp.timestamp = ts;

        // Store with TTL
        pruneExpired();
        HISTORY.addFirst(new Stored(resp, System.currentTimeMillis() + retentionSeconds * 1000));
        while (HISTORY.size() > MAX_HISTORY) HISTORY.removeLast();

        // Redacted log (no PII)
        log.info("MOCK AGS SYNC courseId={} checkpointId={} count={}",
                req.courseId, req.checkpointId, resp.syncedCount);

        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/last", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScorePublishResponse> last(@RequestParam String courseId) {
        if (!mockEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        pruneExpired();
        for (Stored s : HISTORY) {
            if (Objects.equals(s.resp.courseId, courseId)) {
                return ResponseEntity.ok(s.resp);
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No mock sync found for courseId=" + courseId);
    }

    private void pruneExpired() {
        long now = System.currentTimeMillis();
        HISTORY.removeIf(s -> s.expiresAtMs < now);
    }

    private static com.example.lab_signoff_backend.ags.ActivityProgress ActivityOrDefault(String ap) {
        try {
            return ap == null ? com.example.lab_signoff_backend.ags.ActivityProgress.InProgress
                              : com.example.lab_signoff_backend.ags.ActivityProgress.valueOf(ap);
        } catch (IllegalArgumentException e) {
            return com.example.lab_signoff_backend.ags.ActivityProgress.InProgress;
        }
    }

    private static com.example.lab_signoff_backend.ags.GradingProgress GradingOrDefault(String gp) {
        try {
            return gp == null ? com.example.lab_signoff_backend.ags.GradingProgress.Pending
                              : com.example.lab_signoff_backend.ags.GradingProgress.valueOf(gp);
        } catch (IllegalArgumentException e) {
            return com.example.lab_signoff_backend.ags.GradingProgress.Pending;
        }
    }

    // Local ScoreFormulaService to avoid depending on an external DTO; computes a simple result based on checkpoints.
    public static class ScoreFormulaService {
        public static record Result(double scoreGiven, double scoreMaximum, String activityProgress, String gradingProgress) {}

        public Result computeFrom(ScorePublishRequest req) {
            if (req == null || req.checkpoints == null || req.checkpoints.isEmpty()) {
                return new Result(0.0, 100.0, "InProgress", "Pending");
            }
            int total = req.checkpoints.size();
            int completed = 0;
            for (CheckpointDto cp : req.checkpoints) {
                if (cp == null || cp.state == null) continue;
                // attempt to treat state named "complete" (case-insensitive) as completed
                if ("complete".equalsIgnoreCase(cp.state.toString())) completed++;
            }
            double max = total > 0 ? (double) total : 100.0;
            double given = completed;
            return new Result(given, max, "InProgress", "Pending");
        }
    }

    // Minimal local DTO to avoid depending on external package during compile.
    public static class ScorePublishRequest {
        public String courseId;
        public String checkpointId;
        public List<String> userIds;
        public String groupId;
        public String comment;
        public String timestamp;
        public String lineItemId;
        public List<CheckpointDto> checkpoints;
        public Double scoreMaximum;
        public Double scoreGiven;
        public ActivityProgress activityProgress;
        public GradingProgress gradingProgress;
    }
}


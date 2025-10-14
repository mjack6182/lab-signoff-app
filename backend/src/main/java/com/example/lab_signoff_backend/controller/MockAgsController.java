package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.ags.dto.ScorePublishRequest;
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

    // ===== Feature flags / safety knobs =====
    @Value("${app.mocks.ags.enabled:true}")
    private boolean mockEnabled;

    @Value("${app.mocks.ags.retention-seconds:3600}") // 1 hour default
    private long retentionSeconds;

    @Value("${app.mocks.ags.redact-comments:true}")
    private boolean redactComments;

    // ===== Minimal in-memory store with TTL =====
    private static final Deque<Stored> HISTORY = new ConcurrentLinkedDeque<>();
    private record Stored(ScorePublishResponse resp, long expiresAtMs) {}
    private static final int MAX_HISTORY = 50;
    private static final AtomicLong SEQ = new AtomicLong(1);

    // ===== Simple PII guards =====
    private static final Pattern EMAIL = Pattern.compile(".+@.+\\..+");

    private static boolean looksLikeEmail(String s) {
        return s != null && EMAIL.matcher(s).matches();
    }

    @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScorePublishResponse> sync(@RequestBody ScorePublishRequest req) {
        if (!mockEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        // --- Validate required fields (no Bean Validation needed) ---
        if (isBlank(req.courseId)) bad("courseId is required");
        if (isBlank(req.checkpointId)) bad("checkpointId is required");
        if (req.scoreMaximum == null || req.scoreMaximum <= 0) bad("scoreMaximum must be > 0");
        if (req.scoreGiven == null || req.scoreGiven < 0 || req.scoreGiven > req.scoreMaximum)
            bad("scoreGiven must be between 0 and scoreMaximum");
        if (req.activityProgress == null) bad("activityProgress is required");
        if (req.gradingProgress == null) bad("gradingProgress is required");

        boolean hasUsers = req.userIds != null && !req.userIds.isEmpty();
        boolean hasGroup = !isBlank(req.groupId);
        if (!hasUsers && !hasGroup) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Provide groupId or userIds[]");
        }

        // --- FERPA: reject obvious PII (emails) in identifiers ---
        if (hasUsers) {
            for (String uid : req.userIds) {
                if (looksLikeEmail(uid)) bad("userIds must not contain emails (PII)");
            }
        }
        if (hasGroup && looksLikeEmail(req.groupId)) {
            bad("groupId must not be an email (PII)");
        }

        // --- FERPA: redact comment by default (or truncate safely) ---
        if (redactComments) {
            req.comment = null; // drop entirely
        } else if (req.comment != null && req.comment.length() > 120) {
            req.comment = req.comment.substring(0, 120);
        }
        // Also block emails in comment if provided
        if (req.comment != null && looksLikeEmail(req.comment)) {
            req.comment = null;
        }

        // --- Deterministic fields ---
        String ts = isBlank(req.timestamp) ? Instant.now().toString() : req.timestamp;
        String lineItemId = isBlank(req.lineItemId)
                ? "mock-lineitem-" + req.checkpointId
                : req.lineItemId;

        List<String> resultIds = new ArrayList<>();
        if (hasUsers) {
            for (String uid : req.userIds) {
                resultIds.add("mock-result-" + uid);
            }
        } else {
            resultIds.add("mock-result-" + req.groupId);
        }

        // --- Build sanitized response ---
        ScorePublishResponse resp = new ScorePublishResponse();
        resp.mockSyncId = "mock-sync-" + SEQ.getAndIncrement();
        resp.courseId = req.courseId;
        resp.lineItemId = lineItemId;
        resp.resultIds = resultIds;
        resp.syncedCount = resultIds.size();
        resp.checkpointId = req.checkpointId;
        resp.gradingProgress = req.gradingProgress;
        resp.activityProgress = req.activityProgress;
        resp.timestamp = ts;
        // NOTE: we do not echo back comment or any PII

        // --- Store with TTL and prune ---
        pruneExpired();
        HISTORY.addFirst(new Stored(resp, System.currentTimeMillis() + retentionSeconds * 1000));
        while (HISTORY.size() > MAX_HISTORY) HISTORY.removeLast();

        // --- Redacted logging (no userIds/groupId/comment) ---
        log.info("MOCK AGS SYNC courseId={} checkpointId={} count={} gp={} ap={}",
                req.courseId, req.checkpointId, resp.syncedCount,
                req.gradingProgress, req.activityProgress);

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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void bad(String msg) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}

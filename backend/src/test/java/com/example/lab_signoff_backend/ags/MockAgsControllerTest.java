package com.example.lab_signoff_backend.ags;

import com.example.lab_signoff_backend.ags.dto.CheckpointDto;
import com.example.lab_signoff_backend.ags.MockAgsController.ScorePublishRequest;
import com.example.lab_signoff_backend.ags.dto.ScorePublishResponse;
import com.example.lab_signoff_backend.ags.CheckpointState;
import com.example.lab_signoff_backend.ags.ActivityProgress;
import com.example.lab_signoff_backend.ags.GradingProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockAgsControllerTest {

    private MockAgsController controller;

    @BeforeEach
    void setup() throws Exception {
        controller = new MockAgsController();
        ReflectionTestUtils.setField(controller, "mockEnabled", true);
        ReflectionTestUtils.setField(controller, "retentionSeconds", 1L);
        ReflectionTestUtils.setField(controller, "redactComments", true);

        // Clear static history between tests
        Field f = MockAgsController.class.getDeclaredField("HISTORY");
        f.setAccessible(true);
        ((Deque<?>) f.get(null)).clear();
    }

    @Test
    void sync_withScores_returnsResponseAndStoresHistory() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.userIds = List.of("u1", "u2");
        req.scoreMaximum = 10.0;
        req.scoreGiven = 5.0;
        req.comment = "should be redacted"; // will be nulled

        var resp = controller.sync(req);
        assertEquals(200, resp.getStatusCode().value());
        ScorePublishResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(2, body.syncedCount);
        assertNull(req.comment); // redacted

        var last = controller.last("course1");
        assertEquals(body.mockSyncId, last.getBody().mockSyncId);
    }

    @Test
    void sync_withInvalidUserEmail_throwsBadRequest() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.userIds = List.of("user@example.com");
        req.scoreMaximum = 5.0;
        req.scoreGiven = 4.0;

        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_withCheckpointsComputesScore() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course2";
        req.checkpointId = "cp2";
        req.groupId = "group-1";
        CheckpointDto cp = new CheckpointDto();
        cp.state = CheckpointState.Passed;
        req.checkpoints = List.of(cp);

        var resp = controller.sync(req);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(0, resp.getBody().scoreGiven); // local formula only counts literal 'complete'
    }

    @Test
    void sync_whenMockDisabled_returns404() {
        ReflectionTestUtils.setField(controller, "mockEnabled", false);
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.groupId = "g1";
        req.scoreMaximum = 10.0;
        req.scoreGiven = 5.0;

        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void last_whenNoneFound_returns404() {
        assertThrows(ResponseStatusException.class, () -> controller.last("no-course"));
    }

    @Test
    void sync_withNoTargets_returnsUnprocessable() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.scoreMaximum = 1.0;
        req.scoreGiven = 1.0;

        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_groupIdEmail_throwsBadRequest() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.groupId = "email@example.com";
        req.scoreMaximum = 1.0;
        req.scoreGiven = 1.0;

        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_commentTruncatedWhenRedactFalse() {
        ReflectionTestUtils.setField(controller, "redactComments", false);
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.groupId = "g1";
        req.scoreMaximum = 5.0;
        req.scoreGiven = 4.0;
        req.comment = "x".repeat(150);

        var resp = controller.sync(req);
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().timestamp.length() > 0);
    }

    @Test
    void sync_scoreGivenOutOfRangeThrows() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.userIds = List.of("u1");
        req.scoreMaximum = 5.0;
        req.scoreGiven = 10.0; // too high
        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_missingScoreMaximumThrows() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.userIds = List.of("u1");
        req.scoreGiven = 1.0;
        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_missingCourseIdThrows() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.checkpointId = "cp1";
        req.userIds = List.of("u1");
        req.scoreMaximum = 1.0;
        req.scoreGiven = 1.0;
        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_missingCheckpointIdThrows() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.userIds = List.of("u1");
        req.scoreMaximum = 1.0;
        req.scoreGiven = 1.0;
        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_nullCheckpointOrStateThrows() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.groupId = "g1";
        CheckpointDto withNullState = new CheckpointDto();
        withNullState.state = null;
        req.checkpoints = List.of(new CheckpointDto(), withNullState);
        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_commentEmailRedactedEvenWhenNotGloballyRedacting() {
        ReflectionTestUtils.setField(controller, "redactComments", false);
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.groupId = "g1";
        req.scoreMaximum = 5.0;
        req.scoreGiven = 4.0;
        req.comment = "user@example.com";

        controller.sync(req);
        assertNull(req.comment);
    }

    @Test
    void sync_redactFalseKeepsNonEmailComment() {
        ReflectionTestUtils.setField(controller, "redactComments", false);
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.userIds = List.of("u1");
        req.scoreMaximum = 2.0;
        req.scoreGiven = 1.0;
        req.comment = "Looks good";

        controller.sync(req);
        assertEquals("Looks good", req.comment);
    }

    @Test
    void sync_withProvidedProgressUsesValues() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.userIds = List.of("u1");
        req.scoreMaximum = 5.0;
        req.scoreGiven = 5.0;
        req.activityProgress = ActivityProgress.Completed;
        req.gradingProgress = GradingProgress.FullyGraded;

        var resp = controller.sync(req).getBody();
        assertEquals(ActivityProgress.Completed, resp.activityProgress);
        assertEquals(GradingProgress.FullyGraded, resp.gradingProgress);
    }

    @Test
    void sync_withNullCheckpointEntryThrows() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.groupId = "g1";
        req.checkpoints = java.util.Collections.singletonList(null);
        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_scoreMaximumZeroThrows() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.userIds = List.of("u1");
        req.scoreMaximum = 0.0;
        req.scoreGiven = 0.0;
        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_negativeScoreGivenThrows() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.userIds = List.of("u1");
        req.scoreMaximum = 5.0;
        req.scoreGiven = -1.0;
        assertThrows(ResponseStatusException.class, () -> controller.sync(req));
    }

    @Test
    void sync_respectsProvidedTimestampAndLineItem() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.groupId = "g1";
        req.scoreMaximum = 3.0;
        req.scoreGiven = 2.0;
        req.timestamp = "2024-01-01T00:00:00Z";
        req.lineItemId = "provided-line";

        var resp = controller.sync(req);
        assertEquals("2024-01-01T00:00:00Z", resp.getBody().timestamp);
        assertEquals("provided-line", resp.getBody().lineItemId);
    }

    @Test
    void sync_trimsHistoryToMaxSize() throws Exception {
        ReflectionTestUtils.setField(controller, "retentionSeconds", 1000L);
        for (int i = 0; i < 55; i++) {
            ScorePublishRequest req = new ScorePublishRequest();
            req.courseId = "course-" + i;
            req.checkpointId = "cp";
            req.groupId = "g" + i;
            req.scoreMaximum = 1.0;
            req.scoreGiven = 1.0;
            controller.sync(req);
        }
        Field f = MockAgsController.class.getDeclaredField("HISTORY");
        f.setAccessible(true);
        Deque<?> history = (Deque<?>) f.get(null);
        assertTrue(history.size() <= 50);
    }

    @Test
    void innerScoreFormula_handlesNullAndEmpty() {
        MockAgsController.ScoreFormulaService sfs = new MockAgsController.ScoreFormulaService();
        MockAgsController.ScoreFormulaService.Result nullRes = sfs.computeFrom(null);
        assertEquals(0.0, nullRes.scoreGiven());
        assertEquals(100.0, nullRes.scoreMaximum());

        MockAgsController.ScorePublishRequest emptyReq = new MockAgsController.ScorePublishRequest();
        emptyReq.checkpoints = List.of();
        MockAgsController.ScoreFormulaService.Result emptyRes = sfs.computeFrom(emptyReq);
        assertEquals(0.0, emptyRes.scoreGiven());
        assertEquals(100.0, emptyRes.scoreMaximum());

        MockAgsController.ScorePublishRequest req = new MockAgsController.ScorePublishRequest();
        CheckpointDto cp = new CheckpointDto();
        cp.state = CheckpointState.Passed;
        cp.required = true;
        cp.weight = 1.0;
        req.checkpoints = List.of(cp);
        MockAgsController.ScoreFormulaService.Result res = sfs.computeFrom(req);
        assertEquals(1.0, res.scoreMaximum());
        assertTrue(res.scoreGiven() >= 0.0);
    }

    @Test
    void innerScoreFormula_handlesOptionalAndExemptStates() {
        MockAgsController.ScoreFormulaService sfs = new MockAgsController.ScoreFormulaService();
        MockAgsController.ScorePublishRequest req = new MockAgsController.ScorePublishRequest();
        CheckpointDto cpRequired = new CheckpointDto();
        cpRequired.state = CheckpointState.Exempt;
        cpRequired.required = true;
        CheckpointDto cpOptional = new CheckpointDto();
        cpOptional.state = CheckpointState.Returned;
        cpOptional.required = false;
        req.checkpoints = List.of(cpRequired, cpOptional);

        MockAgsController.ScoreFormulaService.Result res = sfs.computeFrom(req);
        assertEquals("InProgress", res.activityProgress());
        assertEquals("Pending", res.gradingProgress());
        assertEquals(0.0, res.scoreGiven());
    }

    @Test
    void innerScoreFormula_ignoresNullCheckpointEntries() {
        MockAgsController.ScoreFormulaService sfs = new MockAgsController.ScoreFormulaService();
        MockAgsController.ScorePublishRequest req = new MockAgsController.ScorePublishRequest();
        CheckpointDto completed = new CheckpointDto();
        completed.state = CheckpointState.Passed; // not counted as complete
        req.checkpoints = java.util.Arrays.asList(null, completed);

        MockAgsController.ScoreFormulaService.Result res = sfs.computeFrom(req);
        assertEquals(2.0, res.scoreMaximum());
        assertEquals(0.0, res.scoreGiven());
    }

    @Test
    void innerScoreFormula_handlesNullState() {
        MockAgsController.ScoreFormulaService sfs = new MockAgsController.ScoreFormulaService();
        MockAgsController.ScorePublishRequest req = new MockAgsController.ScorePublishRequest();
        CheckpointDto cp = new CheckpointDto();
        cp.state = null;
        req.checkpoints = java.util.Arrays.asList(cp);

        MockAgsController.ScoreFormulaService.Result res = sfs.computeFrom(req);
        assertEquals(1.0, res.scoreMaximum());
        assertEquals(0.0, res.scoreGiven());
    }

    @Test
    void activityAndGradingProgressDefaultsHandleInvalidValues() {
        ActivityProgress ap = org.springframework.test.util.ReflectionTestUtils.invokeMethod(controller, "ActivityOrDefault", "bad-value");
        GradingProgress gp = org.springframework.test.util.ReflectionTestUtils.invokeMethod(controller, "GradingOrDefault", "bad-value");
        assertEquals(ActivityProgress.InProgress, ap);
        assertEquals(GradingProgress.Pending, gp);
    }

    @Test
    void last_whenMockDisabled_returns404() {
        ReflectionTestUtils.setField(controller, "mockEnabled", false);
        assertThrows(ResponseStatusException.class, () -> controller.last("course1"));
    }

    @Test
    void last_prunesExpiredEntries() throws Exception {
        ReflectionTestUtils.setField(controller, "retentionSeconds", 0L);
        ScorePublishRequest req = new ScorePublishRequest();
        req.courseId = "course1";
        req.checkpointId = "cp1";
        req.groupId = "g1";
        req.scoreMaximum = 1.0;
        req.scoreGiven = 1.0;

        controller.sync(req);
        Thread.sleep(5);
        assertThrows(ResponseStatusException.class, () -> controller.last("course1"));
    }
}

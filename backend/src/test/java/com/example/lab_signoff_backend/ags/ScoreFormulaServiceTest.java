package com.example.lab_signoff_backend.ags;

import com.example.lab_signoff_backend.ags.ScoreFormulaService.ScorePublishRequest;
import com.example.lab_signoff_backend.ags.dto.CheckpointDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScoreFormulaServiceTest {

    private final ScoreFormulaService service = new ScoreFormulaService();

    @Test
    void computeFrom_whenRequestIsNull_returnsDefaults() {
        ScoreFormulaService.Result result = service.computeFrom(null);

        assertNotNull(result);
        assertEquals(0.0, result.scoreGiven());
        assertEquals(0.0, result.scoreMaximum());
        assertEquals("InProgress", result.activityProgress());
        assertEquals("Pending", result.gradingProgress());
    }

    @Test
    void computeFrom_whenCheckpointsEmpty_usesProvidedScoresAndProgress() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.checkpoints = List.of();
        req.scoreGiven = 7.5;
        req.scoreMaximum = 10.0;
        req.activityProgress = ScorePublishRequest.ActivityProgress.Completed;
        req.gradingProgress = ScorePublishRequest.GradingProgress.FullyGraded;

        ScoreFormulaService.Result result = service.computeFrom(req);

        assertEquals(7.5, result.scoreGiven());
        assertEquals(10.0, result.scoreMaximum());
        assertEquals("Completed", result.activityProgress());
        assertEquals("FullyGraded", result.gradingProgress());
    }

    @Test
    void computeFrom_whenScoresMissingFallBackToZero() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.checkpoints = List.of();
        ScoreFormulaService.Result result = service.computeFrom(req);
        assertEquals(0.0, result.scoreGiven());
        assertEquals(0.0, result.scoreMaximum());
        assertEquals("InProgress", result.activityProgress());
        assertEquals("Pending", result.gradingProgress());
    }

    @Test
    void computeFrom_withMixedCheckpointStates_calculatesEarnedScoreWithLatePenalty() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.lateMultiplier = 0.5; // apply 50% penalty
        req.checkpoints = List.of(
                checkpoint(CheckpointState.Passed, null, null),          // required, weight defaults to 1
                checkpoint(CheckpointState.Returned, 2.0, true),         // contributes to max but not earned
                checkpoint(CheckpointState.Exempt, 5.0, true)            // ignored for max/earned
        );

        ScoreFormulaService.Result result = service.computeFrom(req);

        assertEquals(0.5, result.scoreGiven()); // 1 earned * 0.5 penalty
        assertEquals(3.0, result.scoreMaximum()); // 1 + 2 weights counted
        assertEquals("InProgress", result.activityProgress()); // returned item keeps activity in progress
        assertEquals("Pending", result.gradingProgress());
    }

    @Test
    void computeFrom_whenAllRequiredPassed_marksCompletedAndFullyGraded() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.checkpoints = List.of(
                checkpoint(CheckpointState.Passed, 1.0, true),
                checkpoint(CheckpointState.Passed, 2.0, true)
        );

        ScoreFormulaService.Result result = service.computeFrom(req);

        assertEquals(3.0, result.scoreGiven());
        assertEquals(3.0, result.scoreMaximum());
        assertEquals("Completed", result.activityProgress());
        assertEquals("FullyGraded", result.gradingProgress());
    }

    @Test
    void computeFrom_lateMultiplierClampedAndOptionalCheckpoint() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.lateMultiplier = 2.0; // should clamp to 1
        req.checkpoints = List.of(
                checkpoint(CheckpointState.Passed, 1.0, false), // optional -> adds to earned but not max
                checkpoint(CheckpointState.Passed, 0.0, true)    // weight defaults to 1
        );

        ScoreFormulaService.Result result = service.computeFrom(req);
        assertEquals(1.0, result.scoreMaximum());
        assertEquals(1.0, result.scoreGiven());
    }

    @Test
    void computeFrom_clampsNegativeLateMultiplier() {
        ScorePublishRequest req = new ScorePublishRequest();
        req.lateMultiplier = -1.0; // clamps to 0
        req.checkpoints = List.of(checkpoint(CheckpointState.Passed, null, true));

        ScoreFormulaService.Result result = service.computeFrom(req);
        assertEquals(0.0, result.scoreGiven());
        assertEquals(1.0, result.scoreMaximum());
    }

    @Test
    void computeFrom_skipsNullCheckpointsAndDefaultsRequired() {
        ScorePublishRequest req = new ScorePublishRequest();
        CheckpointDto withNullRequired = new CheckpointDto();
        withNullRequired.state = CheckpointState.Passed;
        withNullRequired.required = null; // defaults to required
        withNullRequired.weight = null;   // defaults weight to 1
        req.checkpoints = java.util.Arrays.asList(null, withNullRequired);

        ScoreFormulaService.Result result = service.computeFrom(req);
        assertEquals(1.0, result.scoreMaximum());
        assertEquals(1.0, result.scoreGiven());
        assertEquals("Completed", result.activityProgress());
        assertEquals("FullyGraded", result.gradingProgress());
    }

    private static CheckpointDto checkpoint(CheckpointState state, Double weight, Boolean required) {
        CheckpointDto cp = new CheckpointDto();
        cp.state = state;
        cp.weight = weight;
        cp.required = required;
        return cp;
    }
}

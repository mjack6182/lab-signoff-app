package com.example.lab_signoff_backend.ags;

import com.example.lab_signoff_backend.ags.dto.CheckpointDto;

import java.util.List;

public class ScoreFormulaService {

    public static final double DEFAULT_WEIGHT = 1.0;

    public static record Result(
            double scoreGiven, double scoreMaximum,
            String activityProgress, String gradingProgress
    ) {}

    public Result computeFrom(ScorePublishRequest req) {
        if (req == null) {
            // Not computing; caller should use provided scores (all defaulted)
            double given = 0.0;
            double max   = 0.0;
            String ap = "InProgress";
            String gp = "Pending";
            return new Result(given, max, ap, gp);
        }
        List<CheckpointDto> cps = req.checkpoints;
        if (cps == null || cps.isEmpty()) {
            // Not computing; caller should use provided scores
            double given = safe(req.scoreGiven);
            double max   = safe(req.scoreMaximum);
            String ap = req.activityProgress != null ? req.activityProgress.name() : "InProgress";
            String gp = req.gradingProgress  != null ? req.gradingProgress.name()  : "Pending";
            return new Result(given, max, ap, gp);
        }

        double max = 0.0;
        double earned = 0.0;

        boolean allDone = true;

        for (CheckpointDto cp : cps) {
            if (cp == null || cp.state == null) continue;

            boolean required = cp.required == null ? true : cp.required.booleanValue();
            double weight = cp.weight == null || cp.weight <= 0 ? DEFAULT_WEIGHT : cp.weight;

            if (cp.state != CheckpointState.Exempt && required) {
                max += weight;
            }

            switch (cp.state) {
                case Passed -> earned += (required ? weight : 0.0);
                case Returned, InProgress, NotStarted -> allDone = false;
                case Exempt -> { /* contributes nothing to max or earned */ }
            }
        }

        double lm = clamp01(req.lateMultiplier == null ? 1.0 : req.lateMultiplier);
        earned = round2(earned * lm);

        String ap = allDone ? "Completed" : "InProgress";
        String gp = allDone ? "FullyGraded" : "Pending";

        return new Result(earned, max, ap, gp);
    }

    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static double round2(double v)  { return Math.round(v * 100.0) / 100.0; }
    private static double safe(Double v)    { return v == null ? 0.0 : v.doubleValue(); }

    // Local DTO used by this service to avoid an unresolved external import; fields are public
    // because ScoreFormulaService accesses them directly.
    public static class ScorePublishRequest {
        public List<CheckpointDto> checkpoints;
        public Double scoreGiven;
        public Double scoreMaximum;
        public ActivityProgress activityProgress;
        public GradingProgress gradingProgress;
        public Double lateMultiplier;

        public enum ActivityProgress { InProgress, Completed }
        public enum GradingProgress { Pending, FullyGraded }
    }
}

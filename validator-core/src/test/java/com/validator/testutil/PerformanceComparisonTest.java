package com.validator.testutil;

import com.validator.testutil.PerformanceComparison.Line;
import com.validator.testutil.PerformanceComparison.Status;
import com.validator.testutil.PerformanceComparison.Threshold;
import com.validator.testutil.PerformanceHistory.Row;
import com.validator.testutil.PerformanceHistory.Run;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.validator.testutil.PerformanceHistoryTest.model;
import static com.validator.testutil.PerformanceHistoryTest.row;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for comparing a run with the performance history ({@link PerformanceComparison}): the regression threshold,
 * which rows compare, and the printed table.
 */
@DisplayName("Performance Comparison Tests")
class PerformanceComparisonTest {

    private static final Threshold THRESHOLD = new Threshold(2.0, 1.0, 2.0);
    private static final String THEN = "2026-01-01T00:00:00Z";
    private static final String NOW = "2026-02-01T00:00:00Z";

    @Test
    @DisplayName("Should count as a regression only a ratio over both the factor and the minimum increase")
    void regressionNeedsFactorAndIncrease() {
        assertTrue(THRESHOLD.isRegression(10, 20.1), "2.01 x and +10.1");
        assertFalse(THRESHOLD.isRegression(10, 20), "exactly the factor is not over it");
        assertFalse(THRESHOLD.isRegression(10, 19), "1.9 x is within the noise");
        assertFalse(THRESHOLD.isRegression(0.3, 0.9), "3 x, but a few milliseconds: +0.6 is under the minimum");
        assertTrue(THRESHOLD.isRegression(0.3, 1.5), "5 x and +1.2");
        assertFalse(THRESHOLD.isRegression(20, 10), "faster is never a regression");
    }

    @Test
    @DisplayName("Should count as an improvement a ratio clearly lower by the same margins")
    void improvementIsTheMirror() {
        assertTrue(THRESHOLD.isImprovement(20.1, 10));
        assertFalse(THRESHOLD.isImprovement(19, 10));
        assertFalse(THRESHOLD.isImprovement(0.9, 0.3));
    }

    @Test
    @DisplayName("Should call a machine busy when its load average is over the limit per processor")
    void detectsBusyMachine() {
        assertTrue(THRESHOLD.isBusy(24.5, 12));
        assertFalse(THRESHOLD.isBusy(24.0, 12), "exactly the limit is not over it");
        assertFalse(THRESHOLD.isBusy(3.0, 12));
        assertFalse(THRESHOLD.isBusy(Double.NaN, 12), "no load average (Windows) is not busy");
        assertFalse(THRESHOLD.isBusy(50, 0), "no processor count is not busy");
    }

    @Test
    @DisplayName("Should load the threshold recorded in perf-reference.properties")
    void loadsThreshold() {
        Threshold loaded = Threshold.load();

        assertTrue(loaded.factor() > 1.0, "a factor of at most 1 would fail on noise: " + loaded.factor());
        assertTrue(loaded.minRatioIncrease() > 0.0, "a model of a few milliseconds needs a floor");
        assertTrue(loaded.busyLoadPerCpu() >= 1.0, "a machine is not busy below one runnable thread per processor");
        assertTrue(loaded.describe().contains(String.format(java.util.Locale.ROOT, "%.1f x", loaded.factor())));
    }

    @Test
    @DisplayName("Should compare each subject with the same subject timed the same way in the baseline")
    void comparesSubjectBySubject() {
        Run baseline = new Run(List.of(
            model(THEN, "0.1", "steady.sysml", 10),
            model(THEN, "0.1", "slower.sysml", 10),
            model(THEN, "0.1", "faster.sysml", 10),
            model(THEN, "0.1", "remeasured.sysml", 10),
            row(THEN, "0.1", "suite:positive", PerformanceHistory.FIRST_BUILDS_TOTAL, 49, 100)));
        List<Row> current = List.of(
            model(NOW, "0.2", "steady.sysml", 15),
            model(NOW, "0.2", "slower.sysml", 25),
            model(NOW, "0.2", "faster.sysml", 4),
            row(NOW, "0.2", "remeasured.sysml", "warm-median-of-5", 1, 0.3),
            row(NOW, "0.2", "suite:positive", PerformanceHistory.FIRST_BUILDS_TOTAL, 51, 300),
            model(NOW, "0.2", "added.sysml", 50));

        List<Line> lines = PerformanceComparison.compare(current, Optional.of(baseline), THRESHOLD);

        assertEquals(List.of(Status.WITHIN_NOISE, Status.REGRESSION, Status.FASTER, Status.NOT_COMPARABLE,
            Status.NOT_COMPARABLE, Status.NEW), lines.stream().map(Line::status).toList());
        assertEquals("+50%", lines.get(0).change());
        assertEquals("+150%", lines.get(1).change());
        assertEquals("-60%", lines.get(2).change());
        assertEquals("", lines.get(3).change(), "first build against warm median does not compare");
        assertEquals("", lines.get(4).change(), "a suite with more files does not compare");
        assertEquals(Optional.empty(), lines.get(5).baseline());
        assertEquals(List.of("slower.sysml"),
            PerformanceComparison.regressions(lines).stream().map(l -> l.now().subject()).toList());
    }

    @Test
    @DisplayName("Should find nothing to compare when there is no recorded run")
    void comparesWithNothing() {
        List<Line> lines = PerformanceComparison.compare(List.of(model(NOW, "0.1", "a.sysml", 999)),
            Optional.empty(), THRESHOLD);

        assertEquals(Status.NEW, lines.get(0).status());
        assertTrue(PerformanceComparison.regressions(lines).isEmpty());
    }

    @Test
    @DisplayName("Should print a table against the previous version and the last run, naming each baseline")
    void printsTable() {
        Run previousVersion = new Run(List.of(model(THEN, "0.1", "a.sysml", 10), model(THEN, "0.1", "b.sysml", 10)));
        Run lastRun = new Run(List.of(model("2026-01-15T00:00:00Z", "0.2", "a.sysml", 12),
            model("2026-01-15T00:00:00Z", "0.2", "b.sysml", 4)));
        List<Row> current = List.of(model(NOW, "0.2", "a.sysml", 13), model(NOW, "0.2", "b.sysml", 9));

        String table = PerformanceComparison.table(current, "0.2", Optional.of("0.1"), Optional.of(previousVersion),
            Optional.of(lastRun), THRESHOLD);

        assertTrue(table.contains("for version 0.2"), table);
        assertTrue(table.contains("previous version: 0.1 at abc1234, " + THEN), table);
        assertTrue(table.contains("last run:         0.2 at abc1234, 2026-01-15T00:00:00Z"), table);
        String[] lines = table.split("\n");
        String a = List.of(lines).stream().filter(l -> l.trim().startsWith("a.sysml")).findFirst().orElseThrow();
        String b = List.of(lines).stream().filter(l -> l.trim().startsWith("b.sysml")).findFirst().orElseThrow();
        assertTrue(a.matches(".*13\\.0 +10\\.0 +\\+30% +12\\.0 +\\+8%"), a);
        assertTrue(b.matches(".*9\\.00 +10\\.0 +-10% +4\\.00 +\\+125% +REGRESSION"), b);
    }

    @Test
    @DisplayName("Should distrust the ratios of a run on a busy machine or with a Java agent")
    void distrustsBusyOrInstrumentedRuns() {
        BuildInfo quiet = new BuildInfo("0.2", "abc1234", "25", "Mac OS X 26.5", "x86_64", 12, 3.0, List.of());
        BuildInfo busy = new BuildInfo("0.2", "abc1234", "25", "Mac OS X 26.5", "x86_64", 12, 34.84, List.of());
        BuildInfo covered = new BuildInfo("0.2", "abc1234", "25", "Mac OS X 26.5", "x86_64", 12, 3.0,
            List.of("org.jacoco.agent-0.8.15-runtime.jar"));

        assertEquals(Optional.empty(), PerformanceComparison.untrusted(quiet, THRESHOLD));
        assertEquals(Optional.of("the machine is busy (load 34.8 on 12 CPUs, over 2.0 per CPU)"),
            PerformanceComparison.untrusted(busy, THRESHOLD));
        assertEquals(Optional.of("a Java agent instruments the code (org.jacoco.agent-0.8.15-runtime.jar; run with"
            + " -Djacoco.skip=true)"), PerformanceComparison.untrusted(covered, THRESHOLD));
    }

    @Test
    @DisplayName("Should say when nothing is recorded yet")
    void printsTableWithoutHistory() {
        String table = PerformanceComparison.table(List.of(model(NOW, "0.1", "a.sysml", 1)), "0.1", Optional.empty(),
            Optional.empty(), Optional.empty(), THRESHOLD);

        assertTrue(table.contains("previous version: none recorded"), table);
        assertTrue(table.contains("last run:         none recorded"), table);
    }

    @Test
    @DisplayName("Should print ratios with three significant digits")
    void formatsRatios() {
        assertEquals("992", PerformanceComparison.ratio(992.095));
        assertEquals("64.3", PerformanceComparison.ratio(64.281));
        assertEquals("1.23", PerformanceComparison.ratio(1.234));
        assertEquals("0.364", PerformanceComparison.ratio(0.364));
        assertEquals("-", PerformanceComparison.ratio(Double.NaN));
    }
}

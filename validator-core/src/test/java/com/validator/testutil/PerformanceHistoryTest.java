package com.validator.testutil;

import com.validator.testutil.PerformanceHistory.Decision;
import com.validator.testutil.PerformanceHistory.RecordMode;
import com.validator.testutil.PerformanceHistory.Row;
import com.validator.testutil.PerformanceHistory.Run;
import com.validator.testutil.PerformanceHistory.Timing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the performance history file and its recording policy ({@link PerformanceHistory}), without timing
 * anything.
 */
@DisplayName("Performance History Tests")
class PerformanceHistoryTest {

    static Row row(String utc, String version, String subject, String measure, int files, double ratio) {
        return new Row(utc, version, "abc1234", "25.0.2 Homebrew", "Mac OS X 26.5", "x86_64", 12, 1.5, 22.8, 1.0,
            subject, measure, files, 100, ratio * 22.8, ratio, "");
    }

    static Row model(String utc, String version, String subject, double ratio) {
        return row(utc, version, subject, PerformanceHistory.FIRST_BUILD, 1, ratio);
    }

    static BuildInfo build(String version) {
        return new BuildInfo(version, "abc1234", "25.0.2 Homebrew", "Mac OS X 26.5", "x86_64", 12, 3.25, List.of());
    }

    /** Three versions, the second recorded twice: 0.1 (one run), 0.2 (two runs), 0.3 (one run). */
    static PerformanceHistory threeVersions() {
        return new PerformanceHistory(List.of(
            model("2026-01-01T00:00:00Z", "0.1", "a.sysml", 10),
            model("2026-01-01T00:00:00Z", "0.1", "b.sysml", 20),
            model("2026-02-01T00:00:00Z", "0.2", "a.sysml", 11),
            model("2026-02-01T00:00:00Z", "0.2", "b.sysml", 21),
            model("2026-02-15T00:00:00Z", "0.2", "a.sysml", 12),
            model("2026-03-01T00:00:00Z", "0.3", "a.sysml", 13)));
    }

    @Test
    @DisplayName("Should write and read back rows unchanged, quoting notes with commas and quotes")
    void roundTripsRows() {
        Row tricky = new Row("2026-10-25T15:19:03Z", "0.1.0-SNAPSHOT", "9775388-dirty", "25.0.2 Homebrew",
            "Mac OS X 26.5", "x86_64", 12, 16.53, 26.4, 1.16, "suite:positive", "first-builds-total", 51, 7149,
            37738.0, 1429.477, "busy, 4 parallel \"mvn verify\" builds");

        PerformanceHistory read = PerformanceHistory.parse(new PerformanceHistory(List.of(tricky)).toCsv());

        assertEquals(List.of(tricky), read.rows());
        assertTrue(PerformanceHistory.formatRows(List.of(tricky))
            .endsWith(",\"busy, 4 parallel \"\"mvn verify\"\" builds\"\n"));
    }

    @Test
    @DisplayName("Should read columns by name, so older files without newer columns still parse")
    void readsColumnsByName() {
        String csv = "subject,ratio,version,recorded_utc,timed_ms\n"
            + "a.sysml,1.5,0.1,2026-01-01T00:00:00Z,34.2\n";

        Row r = PerformanceHistory.parse(csv).rows().get(0);

        assertEquals("a.sysml", r.subject());
        assertEquals(1.5, r.ratio());
        assertEquals("0.1", r.version());
        assertEquals(34.2, r.timedMs());
        assertTrue(Double.isNaN(r.loadAverage()), "a missing number reads as NaN");
        assertEquals("", r.note());
        assertEquals(0, r.cpus());
    }

    @Test
    @DisplayName("Should reject a header without the columns a comparison needs, and name a malformed line")
    void rejectsMalformedHistory() {
        IllegalArgumentException noRatio = assertThrows(IllegalArgumentException.class,
            () -> PerformanceHistory.parse("recorded_utc,version,subject\nx,0.1,a.sysml\n"));
        assertTrue(noRatio.getMessage().contains("ratio"), noRatio.getMessage());

        IllegalArgumentException badNumber = assertThrows(IllegalArgumentException.class,
            () -> PerformanceHistory.parse("recorded_utc,version,subject,ratio\nx,0.1,a.sysml,1.0\ny,0.1,b,fast\n"));
        assertTrue(badNumber.getMessage().contains("line 3"), badNumber.getMessage());

        assertThrows(IllegalArgumentException.class,
            () -> PerformanceHistory.parse("recorded_utc,version,subject,ratio,note\nx,0.1,a,1.0,\"open\n"));
    }

    @Test
    @DisplayName("Should split CSV with CRLF line ends, blank lines and quoted line breaks")
    void splitsCsvRecords() {
        List<List<String>> records = PerformanceHistory.parseCsv("a,b\r\n\"x,1\",\"two\nlines\"\r\n\r\nlast,");

        assertEquals(List.of(List.of("a", "b"), List.of("x,1", "two\nlines"), List.of(""), List.of("last", "")),
            records);
        assertEquals(0, PerformanceHistory.parse("").rows().size());
        assertEquals(0, PerformanceHistory.parse(PerformanceHistory.header() + "\n\n").rows().size());
    }

    @Test
    @DisplayName("Should build a run's rows from timings: ratio to the reference, CSV precision, one-line note")
    void buildsRunRows() {
        BuildInfo noLoad = new BuildInfo("0.2.0", "def5678-dirty", "25 X", "Linux 6", "amd64", 4, -1.0, List.of());
        List<Row> rows = PerformanceHistory.newRun(noLoad, Instant.parse("2026-10-25T15:19:03.789Z"), 25.0, 1.0964,
            List.of(new Timing("m.sysml", "warm-median-of-5", 1, 348, 9.6449),
                new Timing("suite:positive", PerformanceHistory.FIRST_BUILDS_TOTAL, 51, 7149, 37738.04)),
            "machine busy\nwith builds");

        Row model = rows.get(0);
        assertEquals("2026-10-25T15:19:03Z", model.recordedUtc(), "recorded to the second");
        assertEquals("def5678-dirty", model.commit());
        assertEquals(9.6, model.timedMs());
        assertEquals(0.386, model.ratio(), "9.6449 / 25 rounded to 3 decimals");
        assertEquals(1.1, model.slowdown());
        assertTrue(Double.isNaN(model.loadAverage()), "a negative load average means the OS has none");
        assertEquals("machine busy with builds", model.note());
        assertEquals(51, rows.get(1).files());
        assertEquals(1509.522, rows.get(1).ratio());
        assertEquals(rows, PerformanceHistory.parse(PerformanceHistory.header()
            + PerformanceHistory.formatRows(rows)).rows(), "the rows read back exactly as built");
    }

    @Test
    @DisplayName("Should create the file with a header, append to it, and rewrite a file with an older header")
    void appendsToFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("docs/performance/history.csv");
        Row first = model("2026-01-01T00:00:00Z", "0.1", "a.sysml", 10);
        Row second = model("2026-02-01T00:00:00Z", "0.2", "a.sysml", 11);

        PerformanceHistory.append(file, List.of(first));
        PerformanceHistory.append(file, List.of(second));

        String text = Files.readString(file);
        assertTrue(text.startsWith(PerformanceHistory.header()));
        assertEquals(1, text.split(PerformanceHistory.COLUMNS.get(0), -1).length - 1, "one header line");
        assertEquals(List.of(first, second), PerformanceHistory.read(file).rows());

        Path old = dir.resolve("old.csv");
        Files.writeString(old, "recorded_utc,version,subject,measure,files,ratio\n"
            + "2026-01-01T00:00:00Z,0.1,a.sysml,first-build,1,10.0");
        PerformanceHistory.append(old, List.of(second));
        PerformanceHistory rewritten = PerformanceHistory.read(old);
        assertTrue(Files.readString(old).startsWith(PerformanceHistory.header()), "rewritten with the current columns");
        assertEquals(2, rewritten.rows().size());
        assertEquals(10.0, rewritten.rows().get(0).ratio());
        assertEquals(second, rewritten.rows().get(1));
    }

    @Test
    @DisplayName("Should treat a missing history file as an empty history")
    void readsMissingFileAsEmpty(@TempDir Path dir) {
        PerformanceHistory history = PerformanceHistory.read(dir.resolve("none.csv"));

        assertTrue(history.rows().isEmpty());
        assertEquals(Optional.empty(), history.lastRun());
        assertEquals(Optional.empty(), history.previousVersion("0.1"));
    }

    @Test
    @DisplayName("Should group adjacent rows of one recording into runs and list versions in recorded order")
    void groupsRunsAndVersions() {
        PerformanceHistory history = threeVersions();

        List<Run> runs = history.runs();
        assertEquals(4, runs.size());
        assertEquals(2, runs.get(0).rows().size());
        assertEquals("2026-02-15T00:00:00Z", runs.get(2).recordedUtc());
        assertEquals(List.of("0.1", "0.2", "0.3"), history.versions());
        assertEquals(List.of(1, 2, 1), List.copyOf(history.runCounts().values()));
        assertEquals(Optional.of(13.0), runs.get(3).row("a.sysml").map(Row::ratio));
        assertEquals(Optional.empty(), runs.get(3).row("b.sysml"));
    }

    @Test
    @DisplayName("Should find the previous version by recording order, and the latest run of a version")
    void findsPreviousVersionAndLatestRun() {
        PerformanceHistory history = threeVersions();

        assertEquals(Optional.of("0.3"), history.previousVersion("0.4"), "an unrecorded version follows the last one");
        assertEquals(Optional.of("0.2"), history.previousVersion("0.3"));
        assertEquals(Optional.of("0.1"), history.previousVersion("0.2"));
        assertEquals(Optional.empty(), history.previousVersion("0.1"), "the first version has none");

        Run latest = history.latestRun("0.2").orElseThrow();
        assertEquals("2026-02-15T00:00:00Z", latest.recordedUtc(), "the later of the two 0.2 runs");
        assertEquals(1, latest.rows().size());
        assertEquals(Optional.empty(), history.latestRun("9.9"));
        assertEquals("0.3", history.lastRun().orElseThrow().version());
    }

    @Test
    @DisplayName("Should order versions by when they were first recorded, not by their numbers")
    void ordersVersionsByRecording() {
        PerformanceHistory history = new PerformanceHistory(List.of(
            model("2026-01-01T00:00:00Z", "1.0", "a.sysml", 10),
            model("2026-02-01T00:00:00Z", "0.9-hotfix", "a.sysml", 11),
            model("2026-03-01T00:00:00Z", "1.0", "a.sysml", 12)));

        assertEquals(List.of("1.0", "0.9-hotfix"), history.versions());
        assertEquals(Optional.of("1.0"), history.previousVersion("0.9-hotfix"));
        assertEquals(Optional.empty(), history.previousVersion("1.0"));
        assertEquals("2026-03-01T00:00:00Z", history.latestRun("1.0").orElseThrow().recordedUtc());
    }

    @Test
    @DisplayName("Should read perf.record as auto by default, true to always record and false to never")
    void readsRecordMode() {
        assertEquals(RecordMode.AUTO, RecordMode.fromProperty(null));
        assertEquals(RecordMode.AUTO, RecordMode.fromProperty(""));
        assertEquals(RecordMode.AUTO, RecordMode.fromProperty("auto"));
        assertEquals(RecordMode.ALWAYS, RecordMode.fromProperty("TRUE"));
        assertEquals(RecordMode.NEVER, RecordMode.fromProperty("false"));
        assertThrows(IllegalArgumentException.class, () -> RecordMode.fromProperty("yes"));
    }

    /** The decision for a run whose ratios can be trusted. */
    static Decision decide(RecordMode mode, String version, PerformanceHistory history, boolean failedChecks) {
        return PerformanceHistory.decide(mode, build(version), history, failedChecks, Optional.empty());
    }

    @Test
    @DisplayName("Should record automatically only the first passing run of a version")
    void decidesWhenToRecord() {
        PerformanceHistory history = threeVersions();

        Decision firstOfNewVersion = decide(RecordMode.AUTO, "0.4", history, false);
        assertTrue(firstOfNewVersion.append(), firstOfNewVersion.reason());
        assertTrue(firstOfNewVersion.reason().contains("first run of version 0.4"));

        assertFalse(decide(RecordMode.AUTO, "0.3", history, false).append(),
            "a recorded version is not recorded again by default");
        Decision failed = decide(RecordMode.AUTO, "0.4", history, true);
        assertFalse(failed.append(), "a run that failed a check does not become the baseline by itself");
        assertTrue(failed.reason().contains("perf.record=true"), failed.reason());

        assertTrue(decide(RecordMode.ALWAYS, "0.3", history, false).append());
        Decision accepted = decide(RecordMode.ALWAYS, "0.3", history, true);
        assertTrue(accepted.append(), "perf.record=true records even a failing run");
        assertTrue(accepted.reason().contains("baseline"), accepted.reason());

        assertFalse(decide(RecordMode.NEVER, "0.4", history, false).append());
        assertFalse(decide(RecordMode.ALWAYS, BuildInfo.UNKNOWN, history, false).append(),
            "a run outside Maven has no version to record under");
        assertTrue(decide(RecordMode.AUTO, "0.1", new PerformanceHistory(List.of()), false).append(),
            "the first run ever is recorded");
    }

    @Test
    @DisplayName("Should wait for a run whose ratios can be trusted before recording a version automatically")
    void waitsForTrustworthyRun() {
        PerformanceHistory history = threeVersions();
        Optional<String> busy = Optional.of("the machine is busy (load 34.8 on 12 CPUs, over 2.0 per CPU)");

        Decision decision = PerformanceHistory.decide(RecordMode.AUTO, build("0.4"), history, false, busy);
        assertFalse(decision.append(), "inflated ratios would hide the next regression");
        assertEquals(busy.get() + "; version 0.4 is recorded by its first run without that", decision.reason());
        assertTrue(PerformanceHistory.decide(RecordMode.ALWAYS, build("0.4"), history, false, busy).append(),
            "perf.record=true records whatever the conditions");
    }

    @Test
    @DisplayName("Should record one run per version across version bumps, compared with the previous version")
    void recordsEachVersionOnce(@TempDir Path dir) {
        Path file = dir.resolve("history.csv");
        String[] builds = {"0.1", "0.1", "0.2", "0.2", "0.2", "0.3"};
        int month = 1;
        for (String version : builds) {
            PerformanceHistory history = PerformanceHistory.read(file);
            Decision decision = decide(RecordMode.AUTO, version, history, false);
            if (decision.append()) {
                PerformanceHistory.append(file, List.of(model("2026-0" + month++ + "-01T00:00:00Z", version,
                    "a.sysml", 10)));
            }
        }

        PerformanceHistory history = PerformanceHistory.read(file);
        assertEquals(List.of("0.1", "0.2", "0.3"), history.versions());
        assertEquals(3, history.runs().size(), "one automatic run per version, however many builds");
        assertEquals(Optional.of("0.2"), history.previousVersion("0.3"));
    }

    @Test
    @DisplayName("Should mark a commit measured with uncommitted changes as dirty")
    void marksDirtyCommits() {
        assertEquals("9775388", BuildInfo.describeCommit("9775388", false));
        assertEquals("9775388-dirty", BuildInfo.describeCommit("9775388", true));
        assertTrue(build("0.1").versionKnown());
        assertFalse(build(BuildInfo.UNKNOWN).versionKnown());
    }

    @Test
    @DisplayName("Should name the Java agents among the JVM arguments")
    void findsJavaAgents() {
        assertEquals(List.of("org.jacoco.agent-0.8.15-runtime.jar", "profiler.jar"), BuildInfo.javaAgents(List.of(
            "-Xmx1g", "-javaagent:/Users/me/.m2/org.jacoco.agent-0.8.15-runtime.jar=destfile=/t/jacoco.exec",
            "-Dx=-javaagent:not-an-option", "-javaagent:C:\\tools\\profiler.jar")));
        assertEquals(List.of(), BuildInfo.javaAgents(List.of("-Xshare:auto")));
    }

    @Test
    @DisplayName("Should describe this checkout: the version from surefire and a short SHA from git")
    void capturesThisBuild() {
        BuildInfo info = BuildInfo.capture(TestPaths.repositoryRoot());

        if (System.getProperty(BuildInfo.VERSION_PROPERTY) != null) {
            assertEquals(System.getProperty(BuildInfo.VERSION_PROPERTY), info.version());
        }
        assertTrue(info.commit().equals(BuildInfo.UNKNOWN) || info.commit().matches("[0-9a-f]{7,}(-dirty)?"),
            info.commit());
        assertTrue(info.cpus() > 0);
        assertFalse(info.jdk().isBlank());
    }
}

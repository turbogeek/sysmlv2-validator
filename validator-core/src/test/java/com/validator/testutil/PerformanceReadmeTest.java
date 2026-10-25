package com.validator.testutil;

import com.validator.testutil.PerformanceHistory.Row;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.validator.testutil.PerformanceHistoryTest.model;
import static com.validator.testutil.PerformanceHistoryTest.row;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the generated tables of docs/performance/README.md ({@link PerformanceReadme}), including that the
 * committed README agrees with the committed history.
 */
@DisplayName("Performance README Tests")
class PerformanceReadmeTest {

    private static final String PROSE_BEFORE = "# Performance history\n\nHand-written prose.\n\n";
    private static final String PROSE_AFTER = "\n\nMore prose.\n";

    private static PerformanceHistory history() {
        return new PerformanceHistory(List.of(
            model("2026-01-01T00:00:00Z", "0.1", "a.sysml", 10),
            model("2026-01-01T00:00:00Z", "0.1", "b.sysml", 20),
            model("2026-02-01T00:00:00Z", "0.2", "a.sysml", 11),
            model("2026-02-01T00:00:00Z", "0.2", "b.sysml", 21),
            model("2026-02-15T00:00:00Z", "0.2", "a.sysml", 12.345),
            row("2026-02-15T00:00:00Z", "0.2", "b.sysml", "warm-median-of-5", 1, 0.3)));
    }

    @Test
    @DisplayName("Should tabulate the latest run of each version, one ratio column per subject and measure")
    void rendersLatestRunPerVersion() {
        String md = PerformanceReadme.render(history());

        assertTrue(md.contains("| 0.1 | 1 | 2026-01-01T00:00:00Z | abc1234 |"), md);
        assertTrue(md.contains("| 0.2 | 2 | 2026-02-15T00:00:00Z | abc1234 |"), "0.2 was recorded twice: " + md);
        assertFalse(md.contains("2026-02-01T00:00:00Z"), "only the latest run of a version is tabulated");
        assertTrue(md.contains(
            "| Version | a.sysml (first-build) | b.sysml (first-build) | b.sysml (warm-median-of-5) |"), md);
        assertTrue(md.contains("| 0.1 | 10.0 | 20.0 | - |"), md);
        assertTrue(md.contains("| 0.2 | 12.3 | - | 0.300 |"), md);
    }

    @Test
    @DisplayName("Should escape pipes in notes and say when nothing is recorded")
    void rendersEdgeCases() {
        Row noted = new Row("2026-01-01T00:00:00Z", "0.1", "abc1234", "25", "Linux", "amd64", 4, Double.NaN, 20.0,
            1.0, "a.sysml", PerformanceHistory.FIRST_BUILD, 1, 10, 200.0, 10.0, "busy | 4 builds");

        String md = PerformanceReadme.render(new PerformanceHistory(List.of(noted)));

        assertTrue(md.contains("| 4 | - | 20.0 | 1.00 | busy \\| 4 builds |"), md);
        assertEquals("No runs recorded yet.\n", PerformanceReadme.render(new PerformanceHistory(List.of())));
    }

    @Test
    @DisplayName("Should replace only the generated section, the same way every time")
    void updatesOnlyGeneratedSection() {
        String readme = PROSE_BEFORE + PerformanceReadme.BEGIN + "\nstale table\n" + PerformanceReadme.END
            + PROSE_AFTER;

        String updated = PerformanceReadme.update(readme, history());

        assertTrue(updated.startsWith(PROSE_BEFORE + PerformanceReadme.BEGIN + "\n### Latest run of each version"));
        assertTrue(updated.endsWith(PerformanceReadme.END + PROSE_AFTER));
        assertFalse(updated.contains("stale table"));
        assertEquals(updated, PerformanceReadme.update(updated, history()), "regenerating changes nothing");
        assertEquals(PerformanceReadme.render(history()), PerformanceReadme.generatedPart(updated).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> PerformanceReadme.update(PROSE_BEFORE, history()));
    }

    @Test
    @DisplayName("Should read a README checked out with CRLF line ends (core.autocrlf) like one with LF")
    void readsCrlfReadme() {
        String lf = PerformanceReadme.update(PROSE_BEFORE + PerformanceReadme.BEGIN + "\n" + PerformanceReadme.END
            + PROSE_AFTER, history());
        String crlf = lf.replace("\n", "\r\n");

        assertEquals(PerformanceReadme.render(history()), PerformanceReadme.generatedPart(crlf).orElseThrow());
        assertEquals(lf, PerformanceReadme.update(crlf, history()));
    }

    @Test
    @DisplayName("Should regenerate a README file from a history file")
    void regeneratesFiles(@TempDir Path dir) throws IOException {
        Path csv = dir.resolve("history.csv");
        Path readme = dir.resolve("README.md");
        PerformanceHistory.append(csv, history().rows());
        Files.writeString(readme, PROSE_BEFORE + PerformanceReadme.BEGIN + "\n" + PerformanceReadme.END);

        PerformanceReadme.regenerate(csv, readme);

        assertEquals(PerformanceReadme.render(history()),
            PerformanceReadme.generatedPart(Files.readString(readme)).orElseThrow());
    }

    @Test
    @DisplayName("Should keep the committed README in step with the committed history")
    void committedReadmeMatchesHistory() throws IOException {
        Path root = TestPaths.repositoryRoot();
        PerformanceHistory history = PerformanceHistory.read(root.resolve(PerformanceReadme.HISTORY_FILE));
        String readme = Files.readString(root.resolve(PerformanceReadme.README_FILE));

        assertEquals(PerformanceReadme.render(history), PerformanceReadme.generatedPart(readme).orElseThrow(),
            "docs/performance/README.md is out of date with history.csv; regenerate it with:"
                + " java validator-core/src/test/java/com/validator/testutil/PerformanceReadme.java");
    }
}

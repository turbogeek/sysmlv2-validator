package com.validator.testutil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Locates the test corpora on any machine, so tests that need them run instead of silently skipping.
 *
 * <p>The repository's test-suite folder is found by walking up from the working directory (Maven runs
 * module tests from the module folder, an IDE often from the repository root). The OMG SysML-v2-Release
 * checkout is optional and found from, in order: the system property {@code sysml.release.path}, the
 * environment variable {@code SYSML_RELEASE_PATH}, a {@code SysML-v2-Release} folder next to the
 * repository, or the original Windows location.</p>
 */
public final class TestPaths {

    private static final String RELEASE_PROPERTY = "sysml.release.path";
    private static final String RELEASE_ENV = "SYSML_RELEASE_PATH";
    private static final String LEGACY_RELEASE = "E:/_Documents/git/SysML-v2-Release";

    private TestPaths() {
    }

    /** The repository root: the nearest ancestor of the working directory that holds test-suite. */
    public static Path repositoryRoot() {
        for (Path dir = Paths.get("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve("test-suite")) && Files.isDirectory(dir.resolve("validator-core"))) {
                return dir;
            }
        }
        throw new IllegalStateException("No repository root with test-suite above " + Paths.get("").toAbsolutePath());
    }

    /** The repository's test-suite folder. */
    public static Path testSuiteRoot() {
        return repositoryRoot().resolve("test-suite");
    }

    /** The OMG SysML-v2-Release checkout (with sysml/, kerml/ and sysml.library/), if one is available. */
    public static Optional<Path> sysmlReleaseRoot() {
        List<String> candidates = new ArrayList<>();
        candidates.add(System.getProperty(RELEASE_PROPERTY));
        candidates.add(System.getenv(RELEASE_ENV));
        candidates.add(repositoryRoot().resolveSibling("SysML-v2-Release").toString());
        candidates.add(LEGACY_RELEASE);
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isEmpty() && Files.isDirectory(Paths.get(candidate, "sysml"))) {
                return Optional.of(Paths.get(candidate).toAbsolutePath().normalize());
            }
        }
        return Optional.empty();
    }
}

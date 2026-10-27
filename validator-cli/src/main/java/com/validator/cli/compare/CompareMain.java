package com.validator.cli.compare;

import com.validator.library.LibraryConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Compares this validator with other tools. See conformance/README.md and perf/README.md.
 * <pre>
 * java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain conformance run
 * java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain conformance compare
 * java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain perf run
 * java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain perf compare
 * </pre>
 */
@Command(name = "compare", mixinStandardHelpOptions = true,
    description = "Compare this validator with the pilot implementation and Cameo",
    subcommands = {CompareMain.Conformance.class, CompareMain.Perf.class})
public final class CompareMain implements Callable<Integer> {

    /** The conformance folder, relative to the repository root. */
    static final String CONFORMANCE = "conformance";
    /** The performance corpus, relative to the repository root. */
    static final String PERF_CORPUS = "perf/corpus";
    /** Where performance runs are written, relative to the repository root. */
    static final String PERF_RESULTS = "perf/results";

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * Entry point.
     *
     * @param args the subcommand and its options
     */
    public static void main(String[] args) {
        // Only warnings from the validator while it runs
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.exit(new CommandLine(new CompareMain()).execute(args));
    }

    /** Options common to the subcommands. */
    abstract static class Common {
        @Option(names = "--repo", description = "Repository root (default: current directory)")
        private Path repo = Path.of(".");

        @Option(names = "--release", description = "SysML-v2-Release checkout (default: $SYSML_RELEASE_PATH)")
        private Path release;

        Path repo() {
            return repo;
        }

        Path release() {
            if (release != null) {
                return release;
            }
            String env = System.getenv("SYSML_RELEASE_PATH");
            return env == null || env.isBlank() ? null : Path.of(env);
        }
    }

    /** Options of a validator run. */
    abstract static class RunOptions extends Common {
        @Option(names = "--only", split = ",", description = "Only these model ids")
        private Set<String> only = new LinkedHashSet<>();

        @Option(names = "--library", description = "SysML standard library (default: <release>/sysml.library, "
            + "else as the validator finds it)")
        private Path library;

        @Option(names = "--allow-no-library", description = "Run without the SysML standard library")
        private boolean allowNoLibrary;

        @Option(names = "--out", description = "Result directory (default: <results>/validator_<host>_<time>)")
        private Path out;

        Set<String> only() {
            return only;
        }

        /** Point the validator at the standard library; false (after a message) when there is none. */
        boolean useLibrary() {
            Path lib = library;
            if (lib == null && release() != null && Files.isDirectory(release().resolve("sysml.library"))) {
                lib = release().resolve("sysml.library");
            }
            if (lib != null) {
                System.setProperty("sysml.library.path", lib.toString());
            }
            LibraryConfig libraries = new LibraryConfig();
            if (!libraries.hasLibraryPaths() && !allowNoLibrary) {
                System.err.println("No SysML standard library: give --library or --release (or set "
                    + "SYSML_LIBRARY_PATH); --allow-no-library runs anyway.");
                return false;
            }
            if (libraries.getLibraryPaths().size() > 1) {
                // The paths of all sources are used together: --library adds to SYSML_LIBRARY_PATH
                System.err.println("More than one SysML standard library is used: " + libraries.getLibraryPaths()
                    + ". Unset SYSML_LIBRARY_PATH (or the one in validator.properties) to use only one.");
            }
            return true;
        }

        Path out(Path results) {
            return out != null ? out : results.resolve("validator_" + ValidatorRunner.host() + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        }
    }

    // -----------------------------------------------------------------------------------------------------------

    /** The conformance test: what this validator, the pilot and Cameo report for the same files. */
    @Command(name = "conformance", mixinStandardHelpOptions = true,
        description = "What this validator, the pilot and Cameo report for the same test files",
        subcommands = {ConformanceCorpusCommand.class, ConformanceRun.class, ConformanceCompare.class})
    static final class Conformance implements Callable<Integer> {
        @Override
        public Integer call() {
            CommandLine.usage(this, System.out);
            return 0;
        }
    }

    /** Write conformance/manifest.csv and conformance/kpar. */
    @Command(name = "corpus", mixinStandardHelpOptions = true,
        description = "Write conformance/manifest.csv and the KPARs of conformance/kpar")
    static final class ConformanceCorpusCommand extends Common implements Callable<Integer> {
        @Override
        public Integer call() throws IOException {
            List<Manifest.Entry> entries = ConformanceCorpus.generate(repo(), repo().resolve(CONFORMANCE));
            System.out.println("Wrote " + entries.size() + " entries to " + repo().resolve(CONFORMANCE)
                .resolve("manifest.csv"));
            return 0;
        }
    }

    /** Run this validator on the conformance corpus. */
    @Command(name = "run", mixinStandardHelpOptions = true,
        description = "Run this validator on the conformance corpus")
    static final class ConformanceRun extends RunOptions implements Callable<Integer> {
        @Override
        public Integer call() throws IOException {
            if (!useLibrary()) {
                return 2;
            }
            Path dir = out(repo().resolve(CONFORMANCE).resolve("results"));
            Path manifest = repo().resolve(CONFORMANCE).resolve("manifest.csv");
            new ValidatorRunner(repo(), release(), 1, Integer.MAX_VALUE, only()).validateOnly().manifest(manifest)
                .run(Manifest.read(manifest), dir);
            System.out.println("Wrote " + dir);
            return 0;
        }
    }

    /** Compare the newest runs of the three tools. */
    @Command(name = "compare", mixinStandardHelpOptions = true,
        description = "Compare the newest validator, pilot and Cameo runs of the conformance corpus")
    static final class ConformanceCompare extends Common implements Callable<Integer> {
        @Option(names = "--validator", description = "Validator result directory (default: the newest)")
        private Path validator;

        @Option(names = "--pilot", description = "Pilot result directory (default: the newest)")
        private Path pilot;

        @Option(names = "--cameo", description = "Cameo result directory (default: the newest)")
        private Path cameo;

        @Option(names = "--out", description = "Report (default: conformance/results/comparison.md, with "
            + "verdicts.csv next to it)")
        private Path out;

        @Override
        public Integer call() throws IOException {
            Path results = repo().resolve(CONFORMANCE).resolve("results");
            List<Path> dirs = new ArrayList<>();
            for (Object[] tool : new Object[][] {{validator, ConformanceComparison.VALIDATOR},
                    {pilot, ConformanceComparison.PILOT}, {cameo, ConformanceComparison.CAMEO}}) {
                Path dir = tool[0] != null ? (Path) tool[0] : newest(results, (String) tool[1], null);
                if (dir != null) {
                    dirs.add(dir);
                }
            }
            if (dirs.size() < 2) {
                System.err.println("Need runs of at least two tools in " + results + " (found " + dirs.size()
                    + "); give them with --validator, --pilot and --cameo");
                return 2;
            }
            Path manifestFile = repo().resolve(CONFORMANCE).resolve("manifest.csv");
            List<Manifest.Entry> manifest = Manifest.read(manifestFile);
            String manifestSha = Manifest.sha256(Files.readAllBytes(manifestFile));
            Path report = out != null ? out : results.resolve("comparison.md");
            Files.createDirectories(report.toAbsolutePath().getParent());
            Files.writeString(report, ConformanceComparison.compare(manifest, manifestSha, dirs),
                StandardCharsets.UTF_8);
            Files.writeString(report.resolveSibling("verdicts.csv"), ConformanceComparison.verdicts(manifest, dirs),
                StandardCharsets.UTF_8);
            System.out.println("Compared " + dirs + ": " + report);
            return 0;
        }
    }

    // -----------------------------------------------------------------------------------------------------------

    /** Performance: this validator and the pilot on generated models of growing size. */
    @Command(name = "perf", mixinStandardHelpOptions = true,
        description = "Time this validator and the pilot on the generated models of perf/corpus",
        subcommands = {PerfCorpusCommand.class, PerfRun.class, PerfCompare.class})
    static final class Perf implements Callable<Integer> {
        @Override
        public Integer call() {
            CommandLine.usage(this, System.out);
            return 0;
        }
    }

    /** Regenerate the generated models and the manifest. */
    @Command(name = "corpus", mixinStandardHelpOptions = true,
        description = "Write the generated models and manifest.csv into perf/corpus")
    static final class PerfCorpusCommand extends Common implements Callable<Integer> {
        @Override
        public Integer call() throws IOException {
            List<Manifest.Entry> entries = PerfCorpus.generate(repo(), repo().resolve(PERF_CORPUS), release());
            System.out.println("Wrote " + entries.size() + " entries to " + repo().resolve(PERF_CORPUS)
                .resolve("manifest.csv"));
            return 0;
        }
    }

    /** Time this validator on the performance corpus. */
    @Command(name = "run", mixinStandardHelpOptions = true,
        description = "Time this validator on perf/corpus and write a result directory")
    static final class PerfRun extends RunOptions implements Callable<Integer> {
        @Option(names = "--reps", description = "Repetitions per model (default: ${DEFAULT-VALUE})")
        private int reps = 3;

        @Option(names = "--large-lines", description = "Models with more lines run once, as in the pilot runner "
            + "(default: ${DEFAULT-VALUE})")
        private int largeLines = 10000;

        @Override
        public Integer call() throws IOException {
            if (!useLibrary()) {
                return 2;
            }
            Path dir = out(repo().resolve(PERF_RESULTS));
            Path manifest = repo().resolve(PERF_CORPUS).resolve("manifest.csv");
            new ValidatorRunner(repo(), release(), reps, largeLines, only()).manifest(manifest)
                .run(Manifest.read(manifest), dir);
            System.out.println("Wrote " + dir);
            return 0;
        }
    }

    /** Compare a validator run with a pilot run. */
    @Command(name = "compare", mixinStandardHelpOptions = true,
        description = "Compare the newest validator and pilot runs of perf/corpus")
    static final class PerfCompare extends Common implements Callable<Integer> {
        @Option(names = "--validator", description = "Validator result directory (default: the newest, preferring "
            + "a run on the pilot run's machine)")
        private Path validator;

        @Option(names = "--pilot", description = "Pilot result directory (default: the newest)")
        private Path pilot;

        @Option(names = "--out", description = "Report file (default: perf/results/comparison.md)")
        private Path out;

        @Override
        public Integer call() throws IOException {
            Path results = repo().resolve(PERF_RESULTS);
            Path p = pilot != null ? pilot : newest(results, ConformanceComparison.PILOT, null);
            String pilotHost = p == null ? null : Results.readResults(p.resolve("results.csv")).host();
            Path v = validator;
            if (v == null) {
                v = newest(results, ValidatorRunner.TOOL, pilotHost);
                v = v != null ? v : newest(results, ValidatorRunner.TOOL, null);
            }
            if (v == null || p == null) {
                System.err.println("No " + (v == null ? "validator" : "pilot") + " result directory in " + results
                    + "; give it with --" + (v == null ? "validator" : "pilot"));
                return 2;
            }
            Path report = out != null ? out : results.resolve("comparison.md");
            Files.createDirectories(report.toAbsolutePath().getParent());
            Files.writeString(report, PerfComparison.compare(Manifest.read(repo().resolve(PERF_CORPUS)
                .resolve("manifest.csv")), v, p), StandardCharsets.UTF_8);
            System.out.println("Compared " + v + " with " + p + ": " + report);
            return 0;
        }
    }

    /**
     * The newest result directory of a tool, by the run id (a UTC time) in its results.csv.
     *
     * @param results the results directory
     * @param tool the tool name in results.csv
     * @param host only runs on this machine, or null for any
     * @return the directory, or null if there is none
     * @throws IOException if the directory cannot be listed
     */
    static Path newest(Path results, String tool, String host) throws IOException {
        if (!Files.isDirectory(results)) {
            return null;
        }
        List<Path> dirs;
        try (var stream = Files.list(results)) {
            dirs = stream.filter(p -> Files.isRegularFile(p.resolve("results.csv"))).toList();
        }
        Path best = null;
        String bestId = null;
        for (Path dir : dirs) {
            List<List<String>> records = Results.parseCsv(Files.readString(dir.resolve("results.csv"),
                StandardCharsets.UTF_8));
            if (records.size() < 2 || records.get(1).size() < 4 || !tool.equals(records.get(1).get(0))) {
                continue;
            }
            if (host != null && !host.equals(records.get(1).get(2))) {
                continue;
            }
            String runId = records.get(1).get(3);
            if (bestId == null || runId.compareTo(bestId) > 0) {
                best = dir;
                bestId = runId;
            }
        }
        return best;
    }
}

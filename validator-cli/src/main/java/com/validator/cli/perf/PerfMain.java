package com.validator.cli.perf;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * The performance comparison with Cameo. See perf/cameo/README.md.
 * <pre>
 * java -cp validator-cli/target/sysml-validator.jar com.validator.cli.perf.PerfMain generate
 * java -cp validator-cli/target/sysml-validator.jar com.validator.cli.perf.PerfMain run
 * java -cp validator-cli/target/sysml-validator.jar com.validator.cli.perf.PerfMain compare --cameo DIR
 * </pre>
 */
@Command(name = "perf", mixinStandardHelpOptions = true,
    description = "Performance comparison of this validator with Cameo on the models of perf/cameo/corpus",
    subcommands = {PerfMain.Generate.class, PerfMain.Run.class, PerfMain.Compare.class})
public final class PerfMain implements Callable<Integer> {

    /** The corpus, relative to the repository root. */
    static final String CORPUS = "perf/cameo/corpus";
    /** Where runs are written, relative to the repository root. */
    static final String RESULTS = "perf/cameo/results";

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
        // Only warnings from the validator while timing it
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.exit(new CommandLine(new PerfMain()).execute(args));
    }

    /** Options common to the subcommands. */
    abstract static class Common {
        @Option(names = "--repo", description = "Repository root (default: current directory)")
        private Path repo = Path.of(".");

        @Option(names = "--release", description = "SysML-v2-Release checkout, for the release entries "
            + "(default: $SYSML_RELEASE_PATH)")
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

        Path manifest() {
            return repo.resolve(CORPUS).resolve("manifest.csv");
        }
    }

    /** Regenerate the generated models and the manifest. */
    @Command(name = "generate", mixinStandardHelpOptions = true,
        description = "Write the generated models and manifest.csv into perf/cameo/corpus")
    static final class Generate extends Common implements Callable<Integer> {
        @Override
        public Integer call() throws IOException {
            List<PerfManifest.Entry> entries = PerfCorpusGenerator.generate(repo(), repo().resolve(CORPUS), release());
            System.out.println("Wrote " + entries.size() + " entries to " + manifest());
            return 0;
        }
    }

    /** Time this validator on the manifest. */
    @Command(name = "run", mixinStandardHelpOptions = true,
        description = "Time this validator on the manifest and write a result directory")
    static final class Run extends Common implements Callable<Integer> {
        @Option(names = "--reps", description = "Repetitions per model (default: ${DEFAULT-VALUE})")
        private int reps = 3;

        @Option(names = "--large-lines", description = "Models with more lines run once, as in the Cameo macro "
            + "(default: ${DEFAULT-VALUE})")
        private int largeLines = 10000;

        @Option(names = "--allow-no-library", description = "Run without the SysML standard library (only the "
            + "parse is then timed as validate)")
        private boolean allowNoLibrary;

        @Option(names = "--only", split = ",", description = "Only these model ids")
        private Set<String> only = new LinkedHashSet<>();

        @Option(names = "--library", description = "SysML standard library (default: <release>/sysml.library, "
            + "else as the validator finds it)")
        private Path library;

        @Option(names = "--out", description = "Result directory (default: perf/cameo/results/validator_<host>_<time>)")
        private Path out;

        @Override
        public Integer call() throws IOException {
            Path lib = library;
            if (lib == null && release() != null && Files.isDirectory(release().resolve("sysml.library"))) {
                lib = release().resolve("sysml.library");
            }
            if (lib != null) {
                System.setProperty("sysml.library.path", lib.toString());
            }
            if (!new LibraryConfig().hasLibraryPaths() && !allowNoLibrary) {
                System.err.println("No SysML standard library: give --library or --release (or set "
                    + "SYSML_LIBRARY_PATH). Without it only the parse is timed; --allow-no-library runs anyway.");
                return 2;
            }
            Path dir = out != null ? out : repo().resolve(RESULTS).resolve("validator_" + ValidatorPerfRunner.host()
                + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            new ValidatorPerfRunner(repo(), release(), reps, largeLines, only).run(PerfManifest.read(manifest()), dir);
            System.out.println("Wrote " + dir);
            return 0;
        }
    }

    /** Compare a validator run with a Cameo run. */
    @Command(name = "compare", mixinStandardHelpOptions = true,
        description = "Compare a validator result directory with a Cameo result directory")
    static final class Compare extends Common implements Callable<Integer> {
        @Option(names = "--validator", description = "Validator result directory (default: the newest, preferring "
            + "a run on the Cameo run's machine)")
        private Path validator;

        @Option(names = "--cameo", description = "Cameo result directory (default: the newest)")
        private Path cameo;

        @Option(names = "--out", description = "Report file (default: comparison.md in the Cameo directory)")
        private Path out;

        @Override
        public Integer call() throws IOException {
            Path c = cameo != null ? cameo : newest(repo().resolve(RESULTS), "cameo_", null);
            String cameoHost = c == null ? null : PerfResults.readResults(c.resolve("results.csv")).host();
            Path v = validator;
            if (v == null) {
                v = newest(repo().resolve(RESULTS), "validator_", cameoHost);
                v = v != null ? v : newest(repo().resolve(RESULTS), "validator_", null);
            }
            if (v == null || c == null) {
                System.err.println("No " + (v == null ? "validator" : "Cameo") + " result directory in "
                    + repo().resolve(RESULTS) + "; give it with --" + (v == null ? "validator" : "cameo"));
                return 2;
            }
            Path report = out != null ? out : c.resolve("comparison.md");
            Files.writeString(report, PerfComparison.compare(PerfManifest.read(manifest()), v, c),
                StandardCharsets.UTF_8);
            System.out.println("Compared " + v + " with " + c + ": " + report);
            return 0;
        }
    }

    /**
     * The newest result directory with the given prefix, by the run id (a UTC time) in its results.csv.
     *
     * @param results the results directory
     * @param prefix validator_ or cameo_
     * @param host only runs on this machine, or null for any
     * @return the directory, or null if there is none
     * @throws IOException if the directory cannot be listed
     */
    static Path newest(Path results, String prefix, String host) throws IOException {
        if (!Files.isDirectory(results)) {
            return null;
        }
        List<Path> dirs;
        try (var stream = Files.list(results)) {
            dirs = stream.filter(p -> Files.isDirectory(p) && p.getFileName().toString().startsWith(prefix)
                && Files.isRegularFile(p.resolve("results.csv"))).toList();
        }
        Path best = null;
        String bestId = null;
        for (Path dir : dirs) {
            List<List<String>> records = PerfResults.parseCsv(Files.readString(dir.resolve("results.csv"),
                StandardCharsets.UTF_8));
            if (records.size() < 2 || records.get(1).size() < 4) {
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

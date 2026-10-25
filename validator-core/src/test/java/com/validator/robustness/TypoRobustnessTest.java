package com.validator.robustness;

import com.validator.SysMLv2ValidatorImpl;
import com.validator.ValidationError;
import com.validator.ValidationResult;
import com.validator.refactor.RefactoringEngine;
import com.validator.robustness.TypoMutator.Mutant;
import com.validator.robustness.TypoMutator.Operator;
import com.validator.suggestions.KeywordProvider;
import com.validator.suggestions.SpellingSuggestionEngine;
import com.validator.suggestions.SuggestionResult;
import com.validator.testutil.PerformanceReference;
import com.validator.testutil.TestPaths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Typo robustness harness: feeds deterministic typos and other small mutations of real SysML v2 files to the
 * validator's public entry points, with the standard library loaded as the CLI loads it, and checks the contract
 * that must hold for any input, however broken:
 * <ol>
 *   <li>no exception or Error escapes, and no input hangs the validator;</li>
 *   <li>every reported position is inside the file: 1 &lt;= line &lt;= number of lines (the last line holds the
 *       end of the file), 0 &lt;= column &lt;= length of that line;</li>
 *   <li>{@code validate(File)} and {@code validate(String, String)} report the same syntax errors;</li>
 *   <li>a mutant that is invalid whatever its seed (an unbalanced brace, an unterminated string or comment, a
 *       misspelled keyword at the start of a declaration, a character SysML does not allow in code, nesting
 *       deeper than the parser can follow) gets at least one ERROR;</li>
 *   <li>a mutant that only changes layout (line endings, a byte order mark, a tab for a space, one long line, a
 *       long name) gets the seed's syntax errors, on the seed's lines where lines are kept;</li>
 *   <li>a misspelled keyword is offered back by the suggestion engine, and by an error the validator reports for
 *       all but a small share of them;</li>
 *   <li>no syntax error message is longer than {@value #MAX_SYNTAX_MESSAGE} characters.</li>
 * </ol>
 *
 * <p>A dropped ';' is not a guaranteed error here: the grammar makes the ';' after most usages optional
 * ({@code usageBody?}, {@code SEMICOLON?}), so the report only shows how many dropped semicolons got an error.</p>
 *
 * <p>The seeds are every file of test-suite/positive and an evenly spaced sample of the OMG release's sysml/src
 * examples when the release is available. The default run makes one mutant for one seed-operator pair in four
 * (rotating from seed to seed, so each operator runs on a quarter of the seeds), leaves out the seeds that are slow
 * to parse with a syntax error, and runs the entry points of {@link #observe}. {@code -Dtypo.mutants=N} makes N
 * mutants for every pair, includes the slow seeds and runs every entry point. {@code -Dtypo.seed=S} changes the
 * random seed, and {@code -Dtypo.only=text} runs only the mutants whose id contains the text, to reproduce one
 * failure ({@code -Dtypo.trace=true} prints each mutant and its time). A summary is written to
 * target/robustness/typo-report.json.</p>
 */
@DisplayName("Typo robustness harness")
class TypoRobustnessTest {

    private static final long DEFAULT_RANDOM_SEED = 20_261_025L;
    /**
     * The default run takes one seed-operator pair in this many. All pairs took 100 s on a loaded machine; a third
     * of them took 53 s with the coverage agent attached, plus about 20 s to warm up the parser on the seeds.
     */
    private static final int DEFAULT_SAMPLING = 4;
    private static final int OFFICIAL_SAMPLE = 30;
    /** Per-mutant budget at full machine speed for all entry points together; scaled by the machine's slowdown. */
    private static final long MUTANT_BUDGET_NOMINAL_MS = 20_000;
    private static final int MAX_REPORTED_FAILURES = 60;
    /**
     * Longest syntax error message allowed. The longest legitimate ones list the tokens ANTLR expected, about 2,100
     * characters; quoting the input whole made messages of 200,000 characters.
     */
    private static final int MAX_SYNTAX_MESSAGE = 5_000;
    /**
     * Least share of misspelled keywords that must come back as a suggestion on an error. On 2026-10-25 it was 159
     * of 160 with -Dtypo.mutants=2; before the misspelled name itself was suggested, 1 of 76 (every pair once).
     */
    private static final double MIN_SUGGESTED_ON_ERROR = 0.95;
    /**
     * Seeds whose parse with full LL prediction, which every mutant with a syntax error gets, takes 0.4 s or more
     * (warm, 2026-10-25: 2.1 s, 1.2 s, 0.9 s, 0.6 s and 0.4 s; most seeds take under 0.1 s). Their mutants would take
     * most of the default run, so only deeper runs include them. Models known to be slow in perf-reference.properties
     * are left out of every run; the performance test tracks them.
     */
    private static final Set<String> SLOW_SEEDS = Set.of(
        "positive/17_use_case.sysml",
        "positive/13_actions_drone_delivery.sysml",
        "release/examples/State Space Representation Examples/EVSample.sysml",
        "release/examples/Interaction Sequencing Examples/ServerSequenceRealization-3.sysml",
        "positive/15_states.sysml");

    private static SysMLv2ValidatorImpl validator;
    private static Duration mutantBudget;

    @BeforeAll
    static void setUp() {
        validator = new SysMLv2ValidatorImpl();
        PerformanceReference.Measurement reference = PerformanceReference.measure();
        double slowdown = PerformanceReference.loadCalibration().slowdown(reference);
        long nominal = Long.getLong("typo.budgetMs", MUTANT_BUDGET_NOMINAL_MS);
        mutantBudget = Duration.ofMillis((long) (nominal * slowdown * Math.max(1.0, reference.spread())));
    }

    /** What the entry points said about one mutant, or what they threw. */
    private record Observed(ValidationResult file, ValidationResult string, ValidationResult all,
                            Throwable thrown, String stage) {
    }

    /** Per-operator counts for the report. */
    private static final class Tally {
        int mutants;
        int withErrors;
        int failures;
    }

    @Test
    @DisplayName("Typos and mutations of real models keep the validator's contract")
    void mutantsKeepTheValidatorsContract(@TempDir Path dir) throws IOException {
        boolean deep = System.getProperty("typo.mutants") != null;
        int perOperator = Integer.getInteger("typo.mutants", 1);
        long randomSeed = Long.getLong("typo.seed", DEFAULT_RANDOM_SEED);
        String only = System.getProperty("typo.only", "");
        boolean trace = Boolean.getBoolean("typo.trace");

        long prepStart = System.nanoTime();
        Map<String, String> seeds = seeds(deep || !only.isEmpty());
        List<Mutant> mutants = new ArrayList<>();
        Map<String, List<Integer>> seedSyntaxLines = new LinkedHashMap<>();
        int seedIndex = 0;
        for (Map.Entry<String, String> seed : seeds.entrySet()) {
            TypoMutator mutator = new TypoMutator(seed.getKey(), seed.getValue(), randomSeed);
            for (Operator op : Operator.values()) {
                // The default run takes one seed-operator pair in four, rotating from seed to seed
                boolean sampled = deep || !only.isEmpty() || (seedIndex + op.ordinal()) % DEFAULT_SAMPLING == 0;
                if (!op.isWholeFile() && sampled) {
                    mutants.addAll(mutator.mutate(op, perOperator));
                }
            }
            seedSyntaxLines.put(seed.getKey(), syntaxLines(validator.validate(seed.getValue(), "seed.sysml")));
            seedIndex++;
        }
        TypoMutator synthetic = new TypoMutator("synthetic", "", randomSeed);
        for (Operator op : Operator.values()) {
            if (op.isWholeFile()) {
                mutants.addAll(synthetic.mutate(op, 1));
            }
        }
        if (!only.isEmpty()) {
            mutants.removeIf(m -> !m.id().contains(only));
        }

        long prepMs = (System.nanoTime() - prepStart) / 1_000_000;
        SpellingSuggestionEngine engine = new SpellingSuggestionEngine();
        List<String> failures = new ArrayList<>();
        Map<Operator, Tally> tallies = new TreeMap<>();
        Map<String, Long> times = new LinkedHashMap<>();
        Set<String> keywordsNotSuggested = new TreeSet<>();
        List<String> notSuggestedOnError = new ArrayList<>();
        int misspellings = 0;
        int longestMessage = 0;
        String longestMessageMutant = "";
        long start = System.nanoTime();
        for (int i = 0; i < mutants.size(); i++) {
            Mutant m = mutants.get(i);
            Tally tally = tallies.computeIfAbsent(m.operator(), k -> new Tally());
            tally.mutants++;
            int before = failures.size();
            Path file = dir.resolve("mutant" + i + ".sysml");
            Files.writeString(file, m.source(), StandardCharsets.UTF_8);
            long t0 = System.nanoTime();
            Observed observed = null;
            try {
                observed = assertTimeoutPreemptively(mutantBudget, () -> observe(file, m.source(), deep));
            } catch (AssertionFailedError timeout) {
                failures.add("HANG (over " + mutantBudget.toMillis() + " ms): " + m.id());
            }
            times.put(m.id(), (System.nanoTime() - t0) / 1_000_000);
            Files.deleteIfExists(file);
            if (trace) {
                System.out.println("mutant " + (i + 1) + "/" + mutants.size() + " " + times.get(m.id()) + " ms: " + m.id());
            }
            if (observed != null) {
                check(m, observed, seedSyntaxLines.get(m.seed()), failures);
                if (m.operator() == Operator.MISSPELL_KEYWORD && observed.thrown() == null) {
                    misspellings++;
                    checkSuggestions(m, observed, engine, failures, keywordsNotSuggested, notSuggestedOnError);
                }
                if (observed.file() != null) {
                    tally.withErrors += observed.file().getErrorCount() > 0 ? 1 : 0;
                    for (String e : syntaxErrors(observed.file())) {
                        if (e.length() > longestMessage) {
                            longestMessage = e.length();
                            longestMessageMutant = m.id();
                        }
                    }
                }
            }
            tally.failures += failures.size() - before;
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("seeds", seeds.size());
        report.put("mutants", mutants.size());
        report.put("mutantsPerSeedAndOperator", perOperator);
        report.put("randomSeed", randomSeed);
        report.put("preparationMs", prepMs);
        report.put("elapsedMs", elapsedMs);
        report.put("mutantBudgetMs", mutantBudget.toMillis());
        report.put("keywords", KeywordProvider.getKeywordCount());
        report.put("longestSyntaxErrorMessage", Map.of("length", longestMessage, "mutant", longestMessageMutant));
        Map<String, Object> operators = new LinkedHashMap<>();
        tallies.forEach((op, t) -> operators.put(op.name(),
            Map.of("mutants", t.mutants, "withErrors", t.withErrors, "failures", t.failures)));
        report.put("operators", operators);
        Map<String, Object> slowest = new LinkedHashMap<>();
        times.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(10)
            .forEach(e -> slowest.put(e.getKey(), e.getValue()));
        report.put("slowestMs", slowest);
        report.put("keywordsNotSuggested", keywordsNotSuggested);
        report.put("misspellings", misspellings);
        report.put("keywordNotSuggestedOnError", notSuggestedOnError);
        if (misspellings > 0 && notSuggestedOnError.size() > misspellings * (1 - MIN_SUGGESTED_ON_ERROR)) {
            failures.add(notSuggestedOnError.size() + " of " + misspellings + " misspelled keywords were not suggested"
                + " on any error (at most " + Math.round(100 * (1 - MIN_SUGGESTED_ON_ERROR)) + "% may be): "
                + notSuggestedOnError.subList(0, Math.min(5, notSuggestedOnError.size())));
        }
        report.put("failures", failures);
        PerformanceReference.writeReport(Path.of("target", "robustness", "typo-report.json"), report);
        System.out.printf("Typo robustness: %d mutants of %d seeds in %d ms, %d contract failures, "
            + "%d of %d misspelled keywords not suggested on an error%n", mutants.size(), seeds.size(), elapsedMs,
            failures.size(), notSuggestedOnError.size(), misspellings);

        assertTrue(failures.isEmpty(), failures.size() + " of " + mutants.size() + " mutants broke the contract:\n  "
            + String.join("\n  ", failures.subList(0, Math.min(MAX_REPORTED_FAILURES, failures.size()))));
    }

    // ------------------------------------------------------------------------------------------------- running

    /**
     * Runs the mutant through the public entry points: validate(File) (what the CLI does per file), validate(String,
     * String) and the refactoring analysis with errorFreeOnly off, which builds the symbol table from the tree that
     * error recovery left. Deeper runs add validateAll (the CLI's multi-file path) and the default refactoring
     * analysis, which stops at the parse errors; each entry point parses again, and a mutant with a syntax error is
     * parsed with full LL prediction, which takes up to seconds per parse.
     */
    private static Observed observe(Path file, String source, boolean everyEntryPoint) {
        String stage = "validate(File)";
        ValidationResult byFile = null;
        ValidationResult byString = null;
        ValidationResult byAll = null;
        try {
            byFile = validator.validate(file.toFile());
            stage = "validate(String, String)";
            byString = validator.validate(source, file.getFileName().toString());
            if (everyEntryPoint) {
                stage = "validateAll(List<File>)";
                byAll = validator.validateAll(List.of(file.toFile())).get(0);
            }
            for (boolean errorFreeOnly : everyEntryPoint ? new boolean[] {true, false} : new boolean[] {false}) {
                stage = "RefactoringEngine.analyzeContent(errorFreeOnly=" + errorFreeOnly + ")";
                RefactoringEngine refactoring = new RefactoringEngine();
                refactoring.setErrorFreeOnly(errorFreeOnly);
                RefactoringEngine.RefactoringPlan plan = refactoring.analyzeContent(source, file.toString());
                for (RefactoringEngine.OutputFormat format : RefactoringEngine.OutputFormat.values()) {
                    stage = "RefactoringEngine.formatOutput(" + format + ")";
                    refactoring.setOutputFormat(format);
                    refactoring.formatOutput(plan);
                }
            }
            return new Observed(byFile, byString, byAll, null, null);
        } catch (Throwable t) {
            return new Observed(byFile, byString, byAll, t, stage);
        }
    }

    // ------------------------------------------------------------------------------------------------- checks

    private static void check(Mutant m, Observed o, List<Integer> seedLines, List<String> failures) {
        if (o.thrown() != null) {
            failures.add("CRASH in " + o.stage() + ": " + describe(o.thrown()) + ": " + m.id());
            return;
        }
        int[] lineLengths = lineLengths(m.source());
        for (ValidationResult result : Stream.of(o.file(), o.string(), o.all()).filter(Objects::nonNull).toList()) {
            for (ValidationError issue : issues(result)) {
                if (!hasPosition(issue)) {
                    continue;
                }
                int line = issue.getLine();
                if (line < 1 || line > lineLengths.length) {
                    failures.add("BAD LINE " + line + " of " + lineLengths.length + " (" + issue.getErrorCode() + " "
                        + issue.getMessage() + "): " + m.id());
                } else if (issue.getColumn() < 0 || issue.getColumn() > lineLengths[line - 1]) {
                    failures.add("BAD COLUMN " + line + ":" + issue.getColumn() + " of " + lineLengths[line - 1]
                        + " (" + issue.getErrorCode() + " " + issue.getMessage() + "): " + m.id());
                }
            }
        }
        List<String> fileSyntax = syntaxErrors(o.file());
        for (String error : fileSyntax) {
            if (error.length() > MAX_SYNTAX_MESSAGE) {
                failures.add("MESSAGE OF " + error.length() + " CHARACTERS " + error.substring(0, 80) + "...: " + m.id());
            }
        }
        if (!fileSyntax.equals(syntaxErrors(o.string()))) {
            failures.add("FILE AND STRING DISAGREE " + fileSyntax + " vs " + syntaxErrors(o.string()) + ": " + m.id());
        }
        switch (m.expectation()) {
            case ERROR -> {
                if (o.file().getErrors().stream().noneMatch(e -> e.getSeverity() == ValidationError.Severity.ERROR)) {
                    failures.add("MISSED ERROR: " + m.id());
                }
            }
            case NO_SYNTAX_ERROR -> {
                if (!fileSyntax.isEmpty()) {
                    failures.add("FALSE SYNTAX ERROR " + fileSyntax.get(0) + ": " + m.id());
                }
            }
            case SAME_AS_SEED -> {
                List<Integer> lines = syntaxLines(o.file());
                boolean same = m.sameLines() ? lines.equals(seedLines) : lines.size() == seedLines.size();
                if (!same) {
                    failures.add("LAYOUT CHANGED THE VERDICT: seed syntax errors on lines " + seedLines
                        + ", mutant " + (fileSyntax.size() > 3 ? fileSyntax.subList(0, 3) + "..." : fileSyntax)
                        + ": " + m.id());
                }
            }
            case ANY -> {
            }
        }
    }

    /**
     * A misspelled keyword must come back as a suggestion from the suggestion engine for the misspelled word. It
     * should also come back on an error the validator reports, but the parser can report the error far from the
     * typo (full LL prediction of a declaration can fail at its header because of a typo in its body), so those
     * misses are collected and only their share is bounded, by {@link #MIN_SUGGESTED_ON_ERROR}.
     */
    private static void checkSuggestions(Mutant m, Observed o, SpellingSuggestionEngine engine, List<String> failures,
                                         Set<String> keywordsNotSuggested, List<String> notSuggestedOnError) {
        List<String> offered = engine.suggest(m.misspelling()).stream().map(SuggestionResult::getSuggestion).toList();
        if (!offered.contains(m.intendedKeyword())) {
            keywordsNotSuggested.add(m.intendedKeyword());
            failures.add("NOT SUGGESTED by the engine: " + m.intendedKeyword() + " for " + m.misspelling() + " (offered "
                + offered + "): " + m.id());
        }
        boolean onError = o.file().getErrors().stream().anyMatch(e -> e.getSuggestions().contains(m.intendedKeyword()));
        if (!onError) {
            String first = syntaxErrors(o.file()).stream().findFirst().orElse("no syntax error");
            notSuggestedOnError.add(m.id() + " (first error " + (first.length() > 80 ? first.substring(0, 80) + "..." : first)
                + ")");
        }
    }

    // ------------------------------------------------------------------------------------------------- seeds

    /**
     * Seed name to source: test-suite/positive, then a sample of the OMG release's examples. Known slow models are
     * left out, and so are the {@link #SLOW_SEEDS} unless {@code includeSlow}.
     */
    static Map<String, String> seeds(boolean includeSlow) throws IOException {
        PerformanceReference.Calibration calibration = PerformanceReference.loadCalibration();
        Map<String, String> seeds = new LinkedHashMap<>();
        Path positive = TestPaths.testSuiteRoot().resolve("positive");
        for (Path p : sysmlFiles(positive)) {
            seeds.put("positive/" + positive.relativize(p).toString().replace('\\', '/'), read(p));
        }
        TestPaths.sysmlReleaseRoot().ifPresent(release -> {
            Path examples = release.resolve("sysml").resolve("src");
            try {
                List<Path> all = sysmlFiles(examples);
                int n = Math.min(OFFICIAL_SAMPLE, all.size());
                for (int i = 0; i < n; i++) {
                    Path p = all.get(i * all.size() / n);
                    seeds.put("release/" + examples.relativize(p).toString().replace('\\', '/'), read(p));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        seeds.keySet().removeIf(name -> calibration.isKnownSlow(name.substring(name.lastIndexOf('/') + 1))
            || !includeSlow && SLOW_SEEDS.contains(name));
        return seeds;
    }

    private static List<Path> sysmlFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(p -> p.toString().endsWith(".sysml"))
                .sorted(Comparator.comparing(p -> root.relativize(p).toString().replace('\\', '/')))
                .toList();
        }
    }

    private static String read(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------------------------------------- helpers

    private static List<ValidationError> issues(ValidationResult result) {
        List<ValidationError> issues = new ArrayList<>(result.getErrors());
        issues.addAll(result.getWarnings());
        return issues;
    }

    /** Syntax errors always have a position; other issues have one unless they are at 0:0 (no position). */
    private static boolean hasPosition(ValidationError issue) {
        return "SYNTAX_ERROR".equals(issue.getErrorCode()) || issue.getLine() != 0 || issue.getColumn() != 0;
    }

    private static List<String> syntaxErrors(ValidationResult result) {
        return result.getErrors().stream().filter(e -> "SYNTAX_ERROR".equals(e.getErrorCode()))
            .map(e -> e.getLine() + ":" + e.getColumn() + " " + e.getMessage()).toList();
    }

    private static List<Integer> syntaxLines(ValidationResult result) {
        return result.getErrors().stream().filter(e -> "SYNTAX_ERROR".equals(e.getErrorCode()))
            .map(ValidationError::getLine).toList();
    }

    /**
     * Length in code points of every line, as an editor counts lines: a line ends at CR LF, LF or a lone CR. The
     * text after the last line break is the last line, even when it is empty, since that is where the end of the
     * file is.
     */
    static int[] lineLengths(String text) {
        List<Integer> lengths = new ArrayList<>();
        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                lengths.add(length);
                length = 0;
            } else if (!Character.isLowSurrogate(c)) {
                length++;
            }
        }
        lengths.add(length);
        return lengths.stream().mapToInt(Integer::intValue).toArray();
    }

    private static String describe(Throwable t) {
        StringBuilder s = new StringBuilder(t.getClass().getSimpleName());
        if (t.getMessage() != null) {
            String msg = t.getMessage();
            s.append("(").append(msg.length() > 80 ? msg.substring(0, 80) + "..." : msg).append(")");
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if (e.getClassName().startsWith("com.validator")) {
                s.append(" at ").append(e.getClassName().substring(e.getClassName().lastIndexOf('.') + 1))
                    .append('.').append(e.getMethodName()).append(':').append(e.getLineNumber());
                break;
            }
        }
        return s.toString();
    }
}

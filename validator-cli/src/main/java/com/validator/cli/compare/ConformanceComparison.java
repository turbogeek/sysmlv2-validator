package com.validator.cli.compare;

import com.validator.ErrorCodes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares what this validator, the pilot implementation and Cameo report for the models of the conformance
 * corpus, as Markdown (and a CSV of the verdicts). Any two or all three runs can be compared.
 *
 * <p>For every model, each tool's <b>verdict</b> is <i>accepted</i> (no errors), <i>rejected</i> (errors), or
 * <i>failed</i> (the tool could not load it, which for a user is a rejection). The tools agree on a model when
 * their verdicts agree; where all reject it, their <b>error places</b> (member and line) are compared too.
 * Warnings are counted, with the warnings of the checks this validator adds (lint, name collisions, diamonds)
 * apart, since the other tools do not have them.</p>
 *
 * <p>The most important finding is a model this validator accepts and Cameo rejects: the validator is meant to
 * tell whether a file is usable in Cameo.</p>
 */
public final class ConformanceComparison {

    /** The tool names in results.csv. */
    static final String VALIDATOR = ValidatorRunner.TOOL;
    static final String PILOT = "pilot";
    static final String CAMEO = "cameo";
    /** How many diagnostics a section lists per model. */
    private static final int LIST = 5;
    /** What a row's detail says when the tool ran other bytes than the manifest describes. */
    private static final String CHANGED = "differs from the manifest";
    /** Environment entries the report shows for each run. */
    private static final List<String> SETTINGS = List.of("library", "library.extra", "library.dassault", "kpar",
        "conf.undocumented", "conf.only", "conf.skip", "only");

    private ConformanceComparison() {
    }

    /** What a tool made of a model. */
    enum Verdict {
        ACCEPTED, REJECTED, FAILED,
        /** Cameo imported the model but did not check its text (the build step did not run): no verdict. */
        UNCHECKED,
        /** The tool did not run the model (file not found, the run failed to start). */
        SKIPPED,
        /** The run has no row for the model (for example a run of some models only). */
        MISSING;

        boolean known() {
            return this == ACCEPTED || this == REJECTED || this == FAILED;
        }

        /** Accepted, or not: a model that failed to load is rejected as far as a user is concerned. */
        boolean accepted() {
            return this == ACCEPTED;
        }

        String label() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * One tool's run.
     *
     * @param dir its result directory
     * @param run its results
     * @param env its environment
     * @param diagnostics its diagnostics
     */
    record ToolRun(Path dir, Results.Run run, Map<String, String> env, List<Results.Diagnostic> diagnostics) {

        static ToolRun read(Path dir) throws IOException {
            return new ToolRun(dir, Results.readResults(dir.resolve("results.csv")),
                Results.readEnvironment(dir.resolve("environment.properties")),
                Results.readDiagnostics(dir.resolve("diagnostics.csv")));
        }

        String tool() {
            return run.tool();
        }

        String name() {
            return switch (run.tool()) {
                case VALIDATOR -> "Validator";
                case PILOT -> "Pilot";
                case CAMEO -> "Cameo";
                default -> run.tool();
            };
        }

        /** The phases whose rows decide the verdict. */
        List<String> phases() {
            return switch (run.tool()) {
                case PILOT -> List.of("process");
                case CAMEO -> List.of("import", "count", "build", "validate");
                default -> List.of("validate");
            };
        }

        /** True when the run itself failed (loading the library, creating Cameo's project): no model ran. */
        boolean broken() {
            for (Results.Row r : run.rows()) {
                if (r.fileId().equals(Results.RUN) && r.status().equals("failed")) {
                    return true;
                }
            }
            return false;
        }

        Verdict verdict(String id) {
            if (broken()) {
                return Verdict.SKIPPED;
            }
            boolean any = false;
            long errors = 0;
            // Cameo's import does not report errors in the text: only its build step does
            boolean checked = !run.tool().equals(CAMEO);
            for (Results.Row r : run.rows()) {
                if (!r.fileId().equals(id)) {
                    continue;
                }
                any = true;
                if (r.status().equals("skipped")) {
                    return Verdict.SKIPPED;
                }
                if (!phases().contains(r.phase()) || r.rep() != 1) {
                    continue;
                }
                if (r.status().equals("failed")) {
                    return Verdict.FAILED;
                }
                if (r.status().equals("ok")) {
                    errors += Math.max(0, r.errors());
                    checked |= r.phase().equals("build");
                }
            }
            if (!any) {
                return Verdict.MISSING;
            }
            if (errors > 0) {
                return Verdict.REJECTED;
            }
            return checked ? Verdict.ACCEPTED : Verdict.UNCHECKED;
        }

        /** The reason a model failed, from the first failed row. */
        String failure(String id) {
            for (Results.Row r : run.rows()) {
                if (r.fileId().equals(id) && r.status().equals("failed")) {
                    return r.phase() + ": " + r.detail();
                }
            }
            return "";
        }

        /** True when the tool ran other bytes than the manifest describes for the model. */
        boolean changed(String id) {
            for (Results.Row r : run.rows()) {
                if (r.fileId().equals(id) && r.detail().contains(CHANGED)) {
                    return true;
                }
            }
            return false;
        }

        List<Results.Diagnostic> of(String id, String severity) {
            List<Results.Diagnostic> result = new ArrayList<>();
            for (Results.Diagnostic d : diagnostics) {
                if (d.fileId().equals(id) && d.severity().equals(severity)) {
                    result.add(d);
                }
            }
            return result;
        }

        Set<String> errorPlaces(String id) {
            Set<String> places = new TreeSet<>();
            for (Results.Diagnostic d : of(id, "error")) {
                if (d.place() != null) {
                    places.add(d.place());
                }
            }
            return places;
        }

        /**
         * True when the pilot reports an unresolved name of a Dassault library (DS_Views, ...): the pilot does not
         * resolve the short name of a top-level package declared in another file, which Cameo and this validator do.
         */
        boolean dassaultNames(String id) {
            if (!run.tool().equals(PILOT)) {
                return false;
            }
            for (Results.Diagnostic d : of(id, "error")) {
                if (d.source().equals("linking") && d.message().contains("'DS_")) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * True for a warning of a check this validator adds to the language rules, which the pilot and Cameo do not
     * have: lint, name collisions, diamond inheritance, and line comments that Cameo does not keep.
     *
     * @param code the validator's error code
     * @return true for an added check
     */
    static boolean addedCheck(String code) {
        if (code == null) {
            return false;
        }
        String category = ErrorCodes.getCategoryDescription(code);
        return category.equals("Lint Warning") || category.equals("Name Collision Warning")
            || category.equals("Inheritance Warning") || code.equals("LINE_COMMENT_NOT_PERSISTED");
    }

    /**
     * Compare result directories.
     *
     * @param manifest the conformance manifest
     * @param dirs two or three result directories (of this validator, the pilot and Cameo)
     * @return the Markdown report
     * @throws IOException if a result file cannot be read
     */
    public static String compare(List<Manifest.Entry> manifest, List<Path> dirs) throws IOException {
        return compare(manifest, null, dirs);
    }

    /**
     * Compare result directories, warning of runs of another version of the manifest.
     *
     * @param manifest the conformance manifest
     * @param manifestSha256 the SHA-256 of the manifest file compared, or null if not known
     * @param dirs two or three result directories (of this validator, the pilot and Cameo)
     * @return the Markdown report
     * @throws IOException if a result file cannot be read
     */
    public static String compare(List<Manifest.Entry> manifest, String manifestSha256, List<Path> dirs)
            throws IOException {
        List<ToolRun> runs = new ArrayList<>();
        for (String tool : List.of(VALIDATOR, PILOT, CAMEO)) {
            for (Path dir : dirs) {
                ToolRun r = ToolRun.read(dir);
                if (r.tool().equals(tool)) {
                    runs.add(r);
                    break;
                }
            }
        }
        StringBuilder sb = new StringBuilder("# Conformance: ");
        sb.append(String.join(", ", runs.stream().map(ToolRun::name).toList())).append("\n\n");
        header(sb, runs);
        consistency(sb, manifest, manifestSha256, runs);
        List<Manifest.Entry> models = manifest.stream().filter(e -> runs.stream()
            .anyMatch(r -> r.verdict(e.id()).known())).toList();
        summary(sb, models, runs);
        ToolRun validator = find(runs, VALIDATOR);
        ToolRun pilot = find(runs, PILOT);
        ToolRun cameo = find(runs, CAMEO);
        if (validator != null && cameo != null) {
            disagreements(sb, models, validator, cameo, true);
            disagreements(sb, models, validator, cameo, false);
        }
        if (pilot != null && cameo != null) {
            pairDisagreements(sb, models, pilot, cameo);
        }
        if (validator != null && pilot != null) {
            pairDisagreements(sb, models, validator, pilot);
        }
        placeDifferences(sb, models, runs);
        table(sb, models, runs);
        sb.append("""

            ## Notes

            - **Verdict**: *accepted*, no errors; *rejected*, at least one error; *failed*, the tool could not load the
              model (counted as a rejection). Cameo's verdict covers its import, its text diagnostics (the build
              step) and its validation. *Unchecked*: Cameo imported the model but its build step did not run, so
              its text was not checked; *skipped*: the tool did not run the model; *missing*: the run has no row for
              it. These three are left out of the agreements and disagreements.
            - **Error places** are member and line: the line in a plain file, `member:line` in a KPAR. Cameo's
              validation reports elements, not lines, so only its text errors have places; a tool with no places
              for a model is left out of that model's comparison. Columns are not compared: the tools count them
              differently.
            - **Added checks**: warnings of checks this validator adds to the language rules (lint, name collisions,
              diamond inheritance, line comments Cameo does not keep). The other tools do not have them, so they are
              counted apart.
            - The pilot does not resolve the short name of a top-level package declared in another file
              (`DS_Views::…` for Dassault's `DassaultSystemesViews`), which Cameo and this validator do. Models that
              use Dassault's libraries through those names are rejected by the pilot for that reason; the
              disagreements mark them.
            - The pilot reads no KPARs, and Cameo's KPAR import is not used yet: their runners read the archives,
              and skip one they cannot read rather than count it as rejected by the tool.
            """);
        return sb.toString();
    }

    /**
     * One line per model and tool: the verdicts, as CSV.
     *
     * @param manifest the conformance manifest
     * @param dirs the result directories
     * @return the CSV text
     * @throws IOException if a result file cannot be read
     */
    public static String verdicts(List<Manifest.Entry> manifest, List<Path> dirs) throws IOException {
        List<ToolRun> runs = new ArrayList<>();
        for (Path dir : dirs) {
            runs.add(ToolRun.read(dir));
        }
        StringBuilder sb = new StringBuilder("id,family,expect");
        for (ToolRun r : runs) {
            sb.append(',').append(r.tool()).append(',').append(r.tool()).append("_errors,").append(r.tool())
                .append("_warnings");
        }
        sb.append('\n');
        for (Manifest.Entry e : manifest) {
            List<String> fields = new ArrayList<>(List.of(e.id(), e.family(), e.expect()));
            for (ToolRun r : runs) {
                fields.add(r.verdict(e.id()).label());
                fields.add(Integer.toString(r.of(e.id(), "error").size()));
                fields.add(Integer.toString(r.of(e.id(), "warning").size()));
            }
            sb.append(Results.csv(fields.toArray(String[]::new))).append('\n');
        }
        return sb.toString();
    }

    private static void header(StringBuilder sb, List<ToolRun> runs) {
        sb.append("| Tool | Version | Machine | Run | Directory |\n|---|---|---|---|---|\n");
        for (ToolRun r : runs) {
            sb.append("| ").append(r.name()).append(" | ").append(r.run().version()).append(" | ")
                .append(r.run().host()).append(" | ").append(r.env().getOrDefault("run.id", "")).append(" | `")
                .append(r.dir().getFileName()).append("` |\n");
        }
        sb.append('\n');
        for (ToolRun r : runs) {
            List<String> settings = new ArrayList<>();
            for (String key : SETTINGS) {
                String value = r.env().getOrDefault(key, "");
                if (!value.isBlank()) {
                    settings.add(key + " `" + value + "`");
                }
            }
            if (!settings.isEmpty()) {
                sb.append("- ").append(r.name()).append(": ").append(String.join("; ", settings)).append('\n');
            }
            if (r.broken()) {
                sb.append("- **").append(r.name()).append(": the run failed to start (")
                    .append(r.failure(Results.RUN)).append("), so it ran no model.**\n");
            }
        }
        sb.append('\n');
    }

    /** A warning when the runs used different manifests, or ran files that differ from it. */
    private static void consistency(StringBuilder sb, List<Manifest.Entry> manifest, String manifestSha256,
            List<ToolRun> runs) {
        Map<String, List<String>> byManifest = new LinkedHashMap<>();
        List<String> older = new ArrayList<>();
        for (ToolRun r : runs) {
            String sha = r.env().getOrDefault("manifest.sha256", "");
            if (!sha.isBlank()) {
                byManifest.computeIfAbsent(sha, k -> new ArrayList<>()).add(r.name());
                if (manifestSha256 != null && !sha.equalsIgnoreCase(manifestSha256)) {
                    older.add(r.name());
                }
            }
        }
        if (!older.isEmpty()) {
            sb.append("**").append(String.join(", ", older))
                .append(" ran another version of the manifest than the one compared: its ids and expected outcomes may"
                    + " not fit. Run it again on this commit.**\n\n");
        }
        if (byManifest.size() > 1) {
            sb.append("**The runs used different versions of the manifest (");
            List<String> groups = new ArrayList<>();
            byManifest.forEach((sha, names) -> groups.add(String.join(" and ", names) + " " + sha.substring(0,
                Math.min(12, sha.length()))));
            sb.append(String.join("; ", groups)).append("): a model may not be the same file in each. Run them again"
                + " on the same commit.**\n\n");
        }
        for (ToolRun r : runs) {
            List<String> changed = manifest.stream().map(Manifest.Entry::id).filter(r::changed).toList();
            if (!changed.isEmpty()) {
                sb.append("**").append(r.name()).append(" ran files that differ from the manifest: ")
                    .append(String.join(", ", changed)).append(".**\n\n");
            }
        }
    }

    private static void summary(StringBuilder sb, List<Manifest.Entry> models, List<ToolRun> runs) {
        sb.append("## Summary\n\n").append(models.size()).append(" models.\n\n");
        int all = 0;
        int allKnown = 0;
        for (Manifest.Entry e : models) {
            if (runs.stream().allMatch(r -> r.verdict(e.id()).known())) {
                allKnown++;
                if (runs.stream().map(r -> r.verdict(e.id()).accepted()).distinct().count() == 1) {
                    all++;
                }
            }
        }
        if (runs.size() > 2) {
            sb.append("- All ").append(runs.size()).append(" tools give the same verdict on ").append(all)
                .append(" of the ").append(allKnown).append(" models all of them judged.\n");
        }
        for (int i = 0; i < runs.size(); i++) {
            for (int j = i + 1; j < runs.size(); j++) {
                ToolRun a = runs.get(i);
                ToolRun b = runs.get(j);
                int same = 0;
                int aOnly = 0;
                int bOnly = 0;
                int notCompared = 0;
                for (Manifest.Entry e : models) {
                    Verdict va = a.verdict(e.id());
                    Verdict vb = b.verdict(e.id());
                    if (!va.known() || !vb.known()) {
                        notCompared++;
                    } else if (va.accepted() == vb.accepted()) {
                        same++;
                    } else if (va.accepted()) {
                        aOnly++;
                    } else {
                        bOnly++;
                    }
                }
                sb.append("- ").append(a.name()).append(" and ").append(b.name()).append(": the same verdict on ")
                    .append(same).append("; ").append(a.name()).append(" accepts and ").append(b.name())
                    .append(" rejects ").append(aOnly).append("; ").append(a.name()).append(" rejects and ")
                    .append(b.name()).append(" accepts ").append(bOnly);
                if (notCompared > 0) {
                    sb.append("; not compared ").append(notCompared);
                }
                sb.append(".\n");
            }
        }
        for (ToolRun r : runs) {
            Map<Verdict, Integer> unknown = new LinkedHashMap<>();
            for (Manifest.Entry e : models) {
                Verdict v = r.verdict(e.id());
                if (!v.known()) {
                    unknown.merge(v, 1, Integer::sum);
                }
            }
            if (!unknown.isEmpty()) {
                List<String> counts = new ArrayList<>();
                unknown.forEach((v, n) -> counts.add(v.label() + " " + n));
                sb.append("- ").append(r.name()).append(" has no verdict on ").append(String.join(", ", counts))
                    .append(".\n");
            }
        }
        sb.append('\n');
    }

    /** The two directions of validator-versus-Cameo disagreement, with what the rejecting tool reported. */
    private static void disagreements(StringBuilder sb, List<Manifest.Entry> models, ToolRun validator,
            ToolRun cameo, boolean validatorAccepts) {
        ToolRun rejecting = validatorAccepts ? cameo : validator;
        List<String> lines = new ArrayList<>();
        for (Manifest.Entry e : models) {
            Verdict v = validator.verdict(e.id());
            Verdict c = cameo.verdict(e.id());
            if (!v.known() || !c.known() || v.accepted() != validatorAccepts || c.accepted() == validatorAccepts) {
                continue;
            }
            StringBuilder item = new StringBuilder("- **").append(e.id()).append("** (`").append(e.path())
                .append("`, expected ").append(e.expect()).append("): ").append(rejecting.name()).append(' ')
                .append(rejecting.verdict(e.id()).label());
            String failure = rejecting.failure(e.id());
            if (!failure.isEmpty()) {
                item.append(" (").append(failure).append(')');
            }
            item.append('\n');
            List<Results.Diagnostic> errors = rejecting.of(e.id(), "error");
            for (Results.Diagnostic d : errors.subList(0, Math.min(LIST, errors.size()))) {
                item.append("  - ").append(d.place() == null ? "" : d.place() + ": ").append(d.source())
                    .append(d.code().isEmpty() ? "" : " " + d.code()).append(": ").append(oneLine(d.message()))
                    .append('\n');
            }
            if (errors.size() > LIST) {
                item.append("  - … ").append(errors.size() - LIST).append(" more\n");
            }
            lines.add(item.toString());
        }
        sb.append(validatorAccepts ? "## Validator accepts, Cameo rejects\n\n"
            : "## Validator rejects, Cameo accepts\n\n");
        if (validatorAccepts) {
            sb.append("These models pass this validator but cannot be used in Cameo as they are: the validator misses"
                + " what Cameo reports here.\n\n");
        }
        sb.append(lines.isEmpty() ? "None.\n" : String.join("", lines)).append('\n');
    }

    private static void pairDisagreements(StringBuilder sb, List<Manifest.Entry> models, ToolRun a, ToolRun b) {
        sb.append("## ").append(a.name()).append(" and ").append(b.name()).append(" disagree\n\n");
        List<String> lines = new ArrayList<>();
        int dassault = 0;
        for (Manifest.Entry e : models) {
            Verdict va = a.verdict(e.id());
            Verdict vb = b.verdict(e.id());
            if (va.known() && vb.known() && va.accepted() != vb.accepted()) {
                boolean names = a.dassaultNames(e.id()) || b.dassaultNames(e.id());
                dassault += names ? 1 : 0;
                lines.add("- " + e.id() + ": " + a.name() + " " + va.label() + ", " + b.name() + " " + vb.label()
                    + (names ? " (the pilot: unresolved Dassault library names)" : "") + "\n");
            }
        }
        if (dassault > 0) {
            sb.append(dassault).append(" of these ").append(lines.size()).append(" are models in which the pilot"
                + " cannot resolve the names of Dassault's libraries (see the notes).\n\n");
        }
        sb.append(lines.isEmpty() ? "None.\n" : String.join("", lines)).append('\n');
    }

    /**
     * Models every tool rejects whose error places differ, among the tools that report places for the model:
     * first those whose tools have no error line in common.
     */
    private static void placeDifferences(StringBuilder sb, List<Manifest.Entry> models, List<ToolRun> runs) {
        List<String> disjoint = new ArrayList<>();
        List<String> overlapping = new ArrayList<>();
        for (Manifest.Entry e : models) {
            if (!runs.stream().allMatch(r -> r.verdict(e.id()) == Verdict.REJECTED)) {
                continue;
            }
            List<ToolRun> located = runs.stream().filter(r -> !r.errorPlaces(e.id()).isEmpty()).toList();
            if (located.size() < 2) {
                continue;
            }
            List<Set<String>> places = located.stream().map(r -> r.errorPlaces(e.id())).toList();
            if (places.stream().distinct().count() <= 1) {
                continue;
            }
            Set<String> common = new LinkedHashSet<>(places.get(0));
            places.forEach(common::retainAll);
            StringBuilder item = new StringBuilder("- **").append(e.id()).append("**: ");
            item.append(common.isEmpty() ? "none in common" : "in common " + abbreviate(common));
            for (int i = 0; i < located.size(); i++) {
                Set<String> own = new TreeSet<>(places.get(i));
                own.removeAll(common);
                item.append("; only ").append(located.get(i).name()).append(' ').append(abbreviate(own));
            }
            for (ToolRun r : runs) {
                if (!located.contains(r)) {
                    item.append("; ").append(r.name()).append(" reports no lines");
                }
            }
            (common.isEmpty() ? disjoint : overlapping).add(item.append('\n').toString());
        }
        sb.append("## Rejected by all, with errors in different places\n\n")
            .append("### No error line in common\n\n")
            .append(disjoint.isEmpty() ? "None.\n" : String.join("", disjoint)).append('\n')
            .append("### Some error lines in common\n\n")
            .append(overlapping.isEmpty() ? "None.\n" : String.join("", overlapping)).append('\n');
    }

    private static void table(StringBuilder sb, List<Manifest.Entry> models, List<ToolRun> runs) {
        sb.append("## All models\n\nErrors and warnings: counts; for this validator, warnings of added checks in"
            + " parentheses.\n\n| Model | Expected |");
        for (ToolRun r : runs) {
            sb.append(' ').append(r.name()).append(" |");
        }
        for (ToolRun r : runs) {
            sb.append(' ').append(r.name()).append(" errors / warnings |");
        }
        sb.append("\n|---|---|").append("---|".repeat(runs.size() * 2)).append('\n');
        for (Manifest.Entry e : models) {
            sb.append("| ").append(e.id()).append(" | ").append(e.expect()).append(" |");
            for (ToolRun r : runs) {
                sb.append(' ').append(r.verdict(e.id()).label()).append(" |");
            }
            for (ToolRun r : runs) {
                List<Results.Diagnostic> warnings = r.of(e.id(), "warning");
                long added = r.tool().equals(VALIDATOR) ? warnings.stream().filter(d -> addedCheck(d.code())).count()
                    : 0;
                sb.append(' ').append(r.of(e.id(), "error").size()).append(" / ").append(warnings.size() - added)
                    .append(added > 0 ? " (+" + added + ")" : "").append(" |");
            }
            sb.append('\n');
        }
    }

    private static ToolRun find(List<ToolRun> runs, String tool) {
        return runs.stream().filter(r -> r.tool().equals(tool)).findFirst().orElse(null);
    }

    private static String abbreviate(Set<String> places) {
        if (places.isEmpty()) {
            return "none";
        }
        List<String> list = new ArrayList<>(places);
        return list.size() <= 8 ? String.join(", ", list)
            : String.join(", ", list.subList(0, 8)) + " … (" + list.size() + ")";
    }

    private static String oneLine(String message) {
        String text = message == null ? "" : message.replaceAll("\\s+", " ").strip();
        return text.length() > 200 ? text.substring(0, 199) + "…" : text;
    }
}

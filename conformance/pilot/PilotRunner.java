/*
 * PilotRunner: runs the models of a conformance manifest through the OMG SysML v2 Pilot Implementation and writes
 * the shared result format. See conformance/pilot/README.md.
 */

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.Issue;
import org.omg.sysml.interactive.SysMLInteractive;
import org.omg.sysml.io.SysMLUtil;
import org.omg.sysml.lang.sysml.Element;
import org.omg.sysml.lang.sysml.Membership;
import org.omg.sysml.lang.sysml.Namespace;
import org.omg.sysml.lang.sysml.Relationship;

/**
 * Runs the models of a conformance manifest through the OMG SysML v2 Pilot Implementation with
 * {@link SysMLInteractive}, as the pilot's Jupyter kernel does, and writes the shared result format into the result
 * directory: results.csv, diagnostics.csv, environment.properties, report.md and run-log.txt.
 *
 * <pre>
 * java -cp &lt;pilot&gt;/sysml/jupyter-sysml-kernel-&lt;version&gt;-all.jar conformance/pilot/PilotRunner.java
 *     --library &lt;pilot&gt;/sysml/sysml.library --manifest &lt;manifest.csv&gt; --repo &lt;repository root&gt;
 *     --out &lt;result dir&gt; [--extra-library &lt;dir&gt;]... [--reps N] [--large-lines N]
 *     [--release &lt;SysML-v2-Release checkout&gt;]
 *     [--only id,id]
 * </pre>
 *
 * <p>A model is processed with the steps of {@code SysMLInteractive.process}: a new resource ({@code next}),
 * {@code parse}, and validation with all checks. The steps are called one by one rather than through
 * {@code process}, because {@code process}</p>
 * <ul>
 *   <li>adds an error-free model to the pilot's index, so that every later model resolves names against it. Here
 *       nothing of a plain file is indexed, and after every model all resources but the library's are unloaded and
 *       removed from the resource set, and the index gets back its content of right after the library loaded;</li>
 *   <li>always parses SysML, while a {@code .kerml} file needs a KerML resource;</li>
 *   <li>drops a model with errors with {@code Resource.delete}, which also deletes a file of the resource's name
 *       ("1.sysml", ...) in the working directory.</li>
 * </ul>
 *
 * <p>The {@code .sysml} and {@code .kerml} members of a KPAR are parsed into one resource each, indexed together
 * and then validated, so that they resolve each other as the files of one project do. Each issue keeps its member
 * and its line within that member. While a KPAR is indexed, its top-level names shadow library packages of the
 * same name, as later cells shadow earlier ones in the Jupyter kernel; what the pilot then works out lazily for the
 * library can keep pointing at the KPAR, so such a KPAR runs in a newly loaded pilot, and the pilot is loaded again
 * before the next model.</p>
 */
public final class PilotRunner {

    /** The tool name in the results. */
    static final String TOOL = "pilot";
    static final String RUN = "*";
    static final String PROCESS = "process";
    static final String INVALID_KPAR = "invalid KPAR";
    static final String PROJECT_JSON = ".project.json";
    static final String SYNTAX_CODE = "org.eclipse.xtext.diagnostics.Diagnostic.Syntax";
    static final String LINKING_CODE = "org.eclipse.xtext.diagnostics.Diagnostic.Linking";
    static final String KERNEL_POM = "/META-INF/maven/org.omg.sysml/jupyter-sysml-kernel/pom.properties";
    static final Pattern JAR_VERSION = Pattern.compile("-(\\d+(?:\\.\\d+)+(?:-SNAPSHOT)?)(?:-all)?\\.jar$");
    static final DateTimeFormatter RUN_ID = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    static final String USAGE = "Usage: java -cp <pilot>/sysml/jupyter-sysml-kernel-<version>-all.jar PilotRunner.java"
        + " --library <pilot>/sysml/sysml.library --manifest <manifest.csv> --repo <repository root>"
        + " --out <result dir> [--extra-library <dir>]... [--reps N] [--large-lines N]"
        + " [--release <SysML-v2-Release checkout>] [--only id,id]";

    /** The command line. */
    record Options(Path library, Path manifest, Path repo, Path out, int reps, int largeLines, Path release,
            Set<String> only, List<Path> extraLibraries) {
    }

    /** A manifest entry (the fields the runner needs). */
    record Entry(String id, String path, long lines, String sha256) {
    }

    /** A model text: a plain file (member "") or a member of a KPAR; extension ".sysml" or ".kerml". */
    record Member(String name, String extension, String text) {
    }

    /** A results.csv row without the run columns. */
    record Row(String fileId, int rep, String phase, String ms, long elements, long errors, long warnings,
            String status, String detail) {
    }

    /** A diagnostics.csv row without the tool column. */
    record Diagnostic(String fileId, String member, String source, String severity, int line, int column,
            String code, String message) {
    }

    /**
     * A KPAR this runner cannot read: not a zip archive, a corrupted entry, no .project.json holding a JSON object,
     * a member that is not UTF-8 text, or no .sysml or .kerml member.
     */
    static final class InvalidKparException extends Exception {
        private static final long serialVersionUID = 1L;

        InvalidKparException(String message) {
            super(message);
        }
    }

    /** Collects what the pilot logs (with log4j) at WARN and above, such as exceptions inside a validation check. */
    final class PilotLog extends AppenderSkeleton {
        private int errors;

        @Override
        protected synchronized void append(LoggingEvent event) {
            boolean error = event.getLevel().isGreaterOrEqual(Level.ERROR);
            if (error) {
                errors++;
            }
            StringBuilder text = new StringBuilder("pilot log ").append(event.getLevel()).append(' ')
                .append(event.getLoggerName()).append(": ").append(event.getRenderedMessage());
            String[] trace = event.getThrowableStrRep();
            if (trace != null) {
                for (int i = 0; i < Math.min(trace.length, 12); i++) {
                    text.append("\n    ").append(trace[i].strip());
                }
            }
            log(text.toString());
        }

        synchronized int takeErrors() {
            int n = errors;
            errors = 0;
            return n;
        }

        @Override
        public void close() {
            // nothing to release
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

    private final Options options;
    private final List<Row> rows = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final StringBuilder runLog = new StringBuilder();
    private final PilotLog pilotLog = new PilotLog();
    private SysMLInteractive pilot;
    private Set<Resource> library;
    /** The names of the library's top-level elements (its packages). */
    private Set<String> libraryNames;
    /** No model was validated since the library was loaded. */
    private boolean pristine;
    /** A KPAR redeclared library packages: the pilot is loaded again before the next model. */
    private boolean contaminated;
    /** The maps of the pilot's index (names, descriptions), each with its content right after the library loaded. */
    private final Map<Map<Object, Object>, Map<Object, Object>> indexSnapshot = new IdentityHashMap<>();
    private String indexProblem;
    /** The manifest being run. */
    private volatile List<Entry> manifest = List.of();
    /** The result files were written (at the end, or by the shutdown hook). */
    private final AtomicBoolean written = new AtomicBoolean();

    PilotRunner(Options options) {
        this.options = options;
    }

    /**
     * Run the manifest and write the result files.
     *
     * @param args the command line, see the class comment
     */
    public static void main(String[] args) {
        Options options;
        try {
            options = parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(USAGE);
            System.exit(2);
            return;
        }
        // The pilot may leave non-daemon threads behind
        System.exit(new PilotRunner(options).run());
    }

    static Options parse(String[] args) {
        Set<String> known = Set.of("--library", "--manifest", "--repo", "--out", "--reps", "--large-lines",
            "--release", "--only", "--extra-library");
        Map<String, String> values = new HashMap<>();
        List<Path> extraLibraries = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (!known.contains(args[i])) {
                throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("No value for " + args[i]);
            }
            if (args[i].equals("--extra-library")) {
                extraLibraries.add(Path.of(args[++i]));
            } else {
                values.put(args[i], args[++i]);
            }
        }
        for (String required : List.of("--library", "--manifest", "--repo", "--out")) {
            if (!values.containsKey(required)) {
                throw new IllegalArgumentException("Missing " + required);
            }
        }
        String release = values.getOrDefault("--release", System.getenv("SYSML_RELEASE_PATH"));
        Set<String> only = new LinkedHashSet<>();
        for (String id : values.getOrDefault("--only", "").split(",")) {
            if (!id.isBlank()) {
                only.add(id.strip());
            }
        }
        return new Options(Path.of(values.get("--library")), Path.of(values.get("--manifest")),
            Path.of(values.get("--repo")), Path.of(values.get("--out")), positive(values, "--reps", 1),
            positive(values, "--large-lines", 10000), release == null || release.isBlank() ? null : Path.of(release),
            only, List.copyOf(extraLibraries));
    }

    private static int positive(Map<String, String> values, String name, int fallback) {
        String value = values.get(name);
        if (value == null) {
            return fallback;
        }
        try {
            int n = Integer.parseInt(value.strip());
            if (n >= 1) {
                return n;
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        throw new IllegalArgumentException(name + " needs a positive number: " + value);
    }

    /** Run everything; the result files are written even when the run fails. Returns the exit status. */
    int run() {
        String loadAtStart = loadAverage();
        String runId = RUN_ID.format(Instant.now());
        String host = host();
        Path jar = pilotJar();
        String version = pilotVersion(jar);
        int status = 0;
        // Ctrl-C or a stopped run still writes what was done
        Thread interrupted = new Thread(() -> {
            synchronized (this) {
                if (written.compareAndSet(false, true)) {
                    log("interrupted: writing the results of the models run so far");
                    try {
                        write(manifest, version, jar, host, runId, loadAtStart);
                    } catch (IOException | RuntimeException e) {
                        System.err.println("Could not write the results to " + options.out() + ": " + describe(e));
                    }
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(interrupted);
        try {
            Files.createDirectories(options.out());
            log("pilot " + version + " (" + jar + ") on " + host + ", run " + runId);
            manifest = readManifest(options.manifest());
            log("manifest " + options.manifest() + ": " + manifest.size() + " entries");
            Set<String> unknown = new LinkedHashSet<>(options.only());
            manifest.forEach(e -> unknown.remove(e.id()));
            if (!unknown.isEmpty()) {
                log("--only names ids that are not in the manifest: " + unknown);
            }
            startup();
            for (Entry entry : manifest) {
                if (options.only().isEmpty() || options.only().contains(entry.id())) {
                    processEntry(entry);
                }
            }
            if (pilot == null) {
                status = 1;
            }
        } catch (IOException | RuntimeException | Error e) {
            log("run failed: " + describe(e) + trace(e));
            status = 1;
        } finally {
            synchronized (this) {
                if (written.compareAndSet(false, true)) {
                    try {
                        write(manifest, version, jar, host, runId, loadAtStart);
                    } catch (IOException | RuntimeException e) {
                        System.err.println("Could not write the results to " + options.out() + ": " + describe(e));
                        status = 1;
                    }
                }
            }
            try {
                Runtime.getRuntime().removeShutdownHook(interrupted);
            } catch (IllegalStateException e) {
                // The JVM is shutting down: the hook has run
            }
        }
        return status;
    }

    /** Create the pilot and load the standard library: the startup row. */
    private void startup() {
        Logger root = Logger.getRootLogger();
        root.addAppender(pilotLog);
        root.setLevel(Level.WARN);
        long t0 = System.nanoTime();
        try {
            load();
            String ms = ms(t0);
            long elements = countElements(library);
            addRow(new Row(RUN, 1, "startup", ms, elements, -1, -1, "ok",
                library.size() + " library resources; " + heap()));
            log("startup " + ms + " ms: " + library.size() + " library resources, " + elements + " elements");
        } catch (IOException | RuntimeException | Error e) {
            addRow(new Row(RUN, 1, "startup", "-1", -1, -1, -1, "failed", describe(e)));
            log("startup failed: " + describe(e) + trace(e));
        }
    }

    /** A new SysMLInteractive with the standard library loaded (not the shared instance, which is never freed). */
    private void load() throws IOException {
        pilot = null;
        library = null;
        indexSnapshot.clear();
        indexProblem = null;
        if (!Files.isDirectory(options.library())) {
            throw new IOException("no library directory " + options.library());
        }
        SysMLInteractive created = SysMLInteractive.createInstance();
        created.setVerbose(false);
        created.loadLibrary(options.library().toString());
        if (created.getResourceSet().getResources().isEmpty()) {
            throw new IOException("no .sysml or .kerml files in the library " + options.library());
        }
        // More libraries the models use, such as Dassault's for Cameo: read as library resources, so that they
        // are indexed and kept like the standard library's
        for (Path extra : options.extraLibraries()) {
            if (!Files.isDirectory(extra)) {
                throw new IOException("no library directory " + extra);
            }
            // Absolute: the pilot does not find a relative path with a space in it
            created.readAll(extra.toAbsolutePath().toString(), false, ".kerml");
            created.readAll(extra.toAbsolutePath().toString(), false, ".sysml");
        }
        Set<Resource> loaded = Collections.newSetFromMap(new IdentityHashMap<>());
        loaded.addAll(created.getResourceSet().getResources());
        Set<String> names = new HashSet<>();
        loaded.forEach(r -> names.addAll(topLevelNames(r)));
        try {
            snapshotIndex(created);
        } catch (ReflectiveOperationException | RuntimeException e) {
            indexSnapshot.clear();
            indexProblem = "cannot reach the pilot's index to restore it after a KPAR: " + describe(e);
            log(indexProblem + "; KPARs with more than one member are skipped");
        }
        libraryNames = names;
        library = loaded;
        pilot = created;
        pristine = true;
        contaminated = false;
    }

    private boolean reload(String id, String why) {
        log(id + ": loading the pilot again, " + why);
        pilot = null;
        library = null;
        System.gc();
        long t0 = System.nanoTime();
        try {
            load();
            log(id + ": loaded the pilot again in " + ms(t0) + " ms");
            return true;
        } catch (IOException | RuntimeException | Error e) {
            log(id + ": loading the pilot again failed: " + describe(e) + trace(e));
            return false;
        }
    }

    private void processEntry(Entry entry) {
        Path file = resolve(entry);
        if (file == null) {
            skipped(entry.id(), "no SysML-v2-Release checkout for a release: path (--release)");
            return;
        }
        if (!Files.isRegularFile(file)) {
            skipped(entry.id(), "file not found: " + file);
            return;
        }
        if (pilot == null) {
            skipped(entry.id(), "the pilot is not loaded");
            return;
        }
        List<Member> members;
        List<String> notes = new ArrayList<>();
        try {
            byte[] content = Files.readAllBytes(file);
            if (!entry.sha256().isEmpty() && !sha256(content).equalsIgnoreCase(entry.sha256())) {
                notes.add("the file differs from the manifest (sha256)");
            }
            members = members(file, content);
        } catch (InvalidKparException e) {
            // This runner's check, not the pilot's verdict: the pilot has no KPAR reader
            notes.add(0, INVALID_KPAR + " (checked by this runner; the pilot reads no KPARs): " + e.getMessage());
            skipped(entry.id(), String.join("; ", notes));
            return;
        } catch (IOException | RuntimeException e) {
            log(entry.id() + ": " + describe(e));
            notes.add(0, describe(e));
            failed(entry.id(), 1, String.join("; ", notes));
            return;
        }
        // Only the members of a KPAR are indexed, and only indexed names can shadow the library's
        Set<String> redeclared = members.size() > 1 ? redeclaredLibraryNames(entry.id(), members) : Set.of();
        if (!redeclared.isEmpty()) {
            notes.add("redeclares the library's " + String.join(", ", redeclared) + ": run in a newly loaded pilot");
        }
        notes.forEach(n -> log(entry.id() + ": " + n));
        if (members.size() > 1 && indexSnapshot.isEmpty()) {
            notes.add(0, indexProblem);
            skipped(entry.id(), String.join("; ", notes));
            return;
        }
        int reps = entry.lines() > options.largeLines() ? 1 : options.reps();
        for (int rep = 1; rep <= reps; rep++) {
            if (contaminated && !reload(entry.id(), "a KPAR before redeclared library packages")
                    || !redeclared.isEmpty() && !pristine && !reload(entry.id(), "it redeclares library packages")) {
                if (rep == 1) {
                    // Not a verdict of the pilot on the model: it did not run it
                    notes.add(0, "the pilot could not be loaded again (run-log.txt)");
                    skipped(entry.id(), String.join("; ", notes));
                }
                return;
            }
            System.gc();
            log(entry.id() + " repetition " + rep + " of " + reps);
            processOnce(entry.id(), members, kpar(file), rep, String.join("; ", notes));
            contaminated |= !redeclared.isEmpty();
        }
    }

    /**
     * The library's top-level names that the members declare too. While the members are indexed, such a name
     * shadows the library package, and what the pilot then works out lazily for the library (as implicit
     * specializations) can keep pointing at the members: later models would get other results.
     */
    private Set<String> redeclaredLibraryNames(String id, List<Member> members) {
        Set<String> names = new TreeSet<>();
        try {
            for (Member member : members) {
                pilot.next(member.extension());
                pilot.parse(member.text());
                names.addAll(topLevelNames(pilot.getResource()));
            }
            names.retainAll(libraryNames);
        } catch (IOException | RuntimeException | Error e) {
            log(id + ": reading the top-level names failed: " + describe(e));
            names.clear();
        } finally {
            cleanUp(id);
        }
        return names;
    }

    /** The declared names and short names of the elements a resource declares at the top level. */
    static Set<String> topLevelNames(Resource resource) {
        Set<String> names = new HashSet<>();
        for (EObject root : resource.getContents()) {
            if (root instanceof Namespace namespace) {
                for (Relationship relationship : namespace.getOwnedRelationship()) {
                    for (Element element : relationship.getOwnedRelatedElement()) {
                        names.add(element.getDeclaredName());
                        names.add(element.getDeclaredShortName());
                    }
                    if (relationship instanceof Membership alias && relationship.getOwnedRelatedElement().isEmpty()) {
                        names.add(alias.getMemberName());
                        names.add(alias.getMemberShortName());
                    }
                }
            }
        }
        names.remove(null);
        return names;
    }

    /** One repetition of one model: the process row, and the diagnostics of the first repetition. */
    private void processOnce(String id, List<Member> members, boolean kpar, int rep, String note) {
        List<Resource> resources = new ArrayList<>();
        pilotLog.takeErrors();
        pristine = false;
        try {
            if (members.size() > 1 && indexSnapshot.isEmpty()) {
                throw new IllegalStateException(indexProblem);
            }
            long t0 = System.nanoTime();
            for (Member member : members) {
                pilot.next(member.extension());
                pilot.parse(member.text());
                resources.add(pilot.getResource());
            }
            long parsing = System.nanoTime() - t0;
            // The model as written: validation adds a few derived elements
            long elements = countElements(resources);
            t0 = System.nanoTime() - parsing;
            if (resources.size() > 1) {
                for (Resource resource : resources) {
                    pilot.addResourceToIndex(resource);
                }
            }
            List<List<Issue>> issues = new ArrayList<>();
            for (Resource resource : resources) {
                if (!(resource instanceof XtextResource xtext)) {
                    throw new IllegalStateException("not an Xtext resource: " + resource.getURI());
                }
                issues.add(xtext.getResourceServiceProvider().getResourceValidator()
                    .validate(resource, CheckMode.ALL, CancelIndicator.NullImpl));
            }
            String ms = ms(t0);

            Map<URI, String> memberOf = new HashMap<>();
            for (int i = 0; i < resources.size(); i++) {
                memberOf.put(resources.get(i).getURI(), members.get(i).name());
            }
            long errors = 0;
            long warnings = 0;
            for (int i = 0; i < resources.size(); i++) {
                for (Issue issue : issues.get(i)) {
                    if (issue.getSeverity() == Severity.ERROR) {
                        errors++;
                    } else if (issue.getSeverity() == Severity.WARNING) {
                        warnings++;
                    }
                    if (rep == 1) {
                        URI where = issue.getUriToProblem();
                        String member = where == null ? null : memberOf.get(where.trimFragment());
                        addDiagnostic(diagnostic(id, member == null ? members.get(i).name() : member, issue));
                    }
                }
            }
            List<String> detail = new ArrayList<>();
            if (!note.isEmpty()) {
                detail.add(note);
            }
            if (kpar) {
                detail.add(members.size() + " members");
            }
            int logged = pilotLog.takeErrors();
            if (logged > 0) {
                detail.add(logged + " errors logged by the pilot (run-log.txt)");
            }
            detail.add(heap());
            addRow(new Row(id, rep, PROCESS, ms, elements, errors, warnings, "ok",
                String.join("; ", detail)));
            log(id + " repetition " + rep + ": " + ms + " ms, " + errors + " errors, " + warnings + " warnings");
        } catch (IOException | RuntimeException | Error e) {
            failed(id, rep, note.isEmpty() ? describe(e) : describe(e) + "; " + note);
            log(id + " repetition " + rep + " failed: " + describe(e) + trace(e));
        } finally {
            cleanUp(id);
        }
    }

    /**
     * Unload and remove every resource that is not the library's, and give the index back its content of right
     * after the library loaded, so that nothing of this model can resolve a name of the next one.
     */
    private void cleanUp(String id) {
        try {
            List<Resource> all = pilot.getResourceSet().getResources();
            for (Resource resource : new ArrayList<>(all)) {
                if (!library.contains(resource)) {
                    resource.unload();
                    all.remove(resource);
                    pilot.getInputResources().remove(resource);
                }
            }
            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> e : indexSnapshot.entrySet()) {
                if (!e.getKey().equals(e.getValue())) {
                    e.getKey().clear();
                    e.getKey().putAll(e.getValue());
                }
            }
            if (all.size() != library.size()) {
                log(id + ": ISOLATION: after the clean-up the pilot holds " + all.size() + " resources, the library "
                    + library.size());
            }
        } catch (RuntimeException | Error e) {
            log(id + ": ISOLATION: the clean-up failed, later models may see this one: " + describe(e) + trace(e));
        }
    }

    /**
     * Remember the content of the pilot's index after the library loaded. The pilot's index lets the description
     * added last shadow a name, and removing that description does not bring back the shadowed one: a KPAR that
     * declares a library package would otherwise leave that package unresolvable for every later model.
     */
    @SuppressWarnings("unchecked")
    private void snapshotIndex(SysMLInteractive created) throws ReflectiveOperationException {
        Field field = SysMLUtil.class.getDeclaredField("index");
        field.setAccessible(true);
        Object index = field.get(created);
        for (Class<?> c = index.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Map<Object, Object> map = (Map<Object, Object>) f.get(index);
                    if (map != null) {
                        indexSnapshot.putIfAbsent(map, new LinkedHashMap<>(map));
                    }
                }
            }
        }
        if (indexSnapshot.isEmpty()) {
            throw new NoSuchFieldException("no maps in " + index.getClass().getName());
        }
    }

    private Path resolve(Entry entry) {
        String release = "release:";
        if (entry.path().startsWith(release)) {
            return options.release() == null ? null : options.release().resolve(entry.path().substring(
                release.length()));
        }
        return options.repo().resolve(entry.path());
    }

    /** The model texts of a file: itself, or the .sysml and .kerml members of a KPAR. */
    static List<Member> members(Path file, byte[] content) throws IOException, InvalidKparException {
        if (kpar(file)) {
            return readKpar(file);
        }
        String name = file.getFileName().toString();
        String extension = extension(name);
        if (extension == null) {
            throw new IllegalArgumentException("unsupported file type (not .sysml, .kerml or .kpar): " + name);
        }
        return List.of(new Member("", extension, text(content)));
    }

    static boolean kpar(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".kpar");
    }

    /** The .sysml and .kerml members of a KPAR, in archive order. */
    static List<Member> readKpar(Path file) throws IOException, InvalidKparException {
        ZipFile opened;
        try {
            opened = new ZipFile(file.toFile(), StandardCharsets.UTF_8);
        } catch (ZipException e) {
            throw new InvalidKparException("not a zip archive: " + e.getMessage());
        }
        try (ZipFile zip = opened) {
            // The faults the validator's reader and the Cameo macro reject too
            ZipEntry project = zip.getEntry(PROJECT_JSON);
            if (project == null || project.isDirectory()) {
                throw new InvalidKparException("no " + PROJECT_JSON);
            }
            String json;
            try (InputStream in = zip.getInputStream(project)) {
                json = strictText(in.readAllBytes(), PROJECT_JSON);
            }
            if (!jsonObject(json)) {
                throw new InvalidKparException(PROJECT_JSON + " is not a JSON object");
            }
            List<Member> members = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String extension = entry.isDirectory() ? null : extension(entry.getName());
                if (extension != null) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        members.add(new Member(entry.getName(), extension,
                            text(strictText(in.readAllBytes(), entry.getName()))));
                    }
                }
            }
            if (members.isEmpty()) {
                throw new InvalidKparException("no .sysml or .kerml member");
            }
            return members;
        } catch (ZipException | EOFException e) {
            throw new InvalidKparException("corrupted zip entry (" + e.getMessage() + ")");
        }
    }

    /** True when the text is a JSON object, as the validator's KPAR reader requires (Gson is in the pilot jar). */
    static boolean jsonObject(String text) {
        String json = text.startsWith("\ufeff") ? text.substring(1) : text;
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            return new Gson().getAdapter(JsonElement.class).read(reader).isJsonObject();
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    static String extension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".sysml")) {
            return ".sysml";
        }
        return lower.endsWith(".kerml") ? ".kerml" : null;
    }

    static String text(byte[] content) {
        return text(new String(content, StandardCharsets.UTF_8));
    }

    /** The text without a byte order mark. */
    static String text(String text) {
        return text.startsWith("﻿") ? text.substring(1) : text;
    }

    /** UTF-8 bytes as text; an archive with a member that is not UTF-8 is invalid. */
    static String strictText(byte[] content, String name) throws InvalidKparException {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException e) {
            throw new InvalidKparException(name + " is not UTF-8 text");
        }
    }

    static Diagnostic diagnostic(String id, String member, Issue issue) {
        String code = issue.getCode() == null ? "" : issue.getCode();
        String source = issue.isSyntaxError() || SYNTAX_CODE.equals(code) ? "syntax"
            : LINKING_CODE.equals(code) ? "linking" : "validation";
        String severity = issue.getSeverity() == null ? "" : issue.getSeverity().name().toLowerCase(Locale.ROOT);
        return new Diagnostic(id, member, source, severity, orZero(issue.getLineNumber()), orZero(issue.getColumn()),
            code, issue.getMessage() == null ? "" : issue.getMessage());
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    static long countElements(Iterable<Resource> resources) {
        long n = 0;
        for (Resource resource : resources) {
            for (Iterator<EObject> it = resource.getAllContents(); it.hasNext();) {
                if (it.next() instanceof Element) {
                    n++;
                }
            }
        }
        return n;
    }

    /** The manifest entries; lines starting with # and blank lines are ignored, and the header is skipped. */
    List<Entry> readManifest(Path file) throws IOException {
        List<Entry> entries = new ArrayList<>();
        boolean first = true;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            boolean header = first && line.startsWith("id,");
            first = false;
            if (header) {
                continue;
            }
            // The description is last and may hold commas
            String[] f = line.split(",", 9);
            if (f.length < 2 || f[0].isBlank() || f[1].isBlank()) {
                log("manifest: ignoring a line without an id and a path: " + line);
                continue;
            }
            long lines = -1;
            try {
                lines = f.length > 5 ? Long.parseLong(f[5].strip()) : -1;
            } catch (NumberFormatException e) {
                log("manifest: " + f[0] + " has no line count: " + f[5]);
            }
            entries.add(new Entry(f[0].strip(), f[1].strip(), lines, f.length > 7 ? f[7].strip() : ""));
        }
        return entries;
    }

    /** A model that was not run: one row with phase "all", as the validator and the Cameo macro write it. */
    private void skipped(String id, String detail) {
        addRow(new Row(id, 1, "all", "-1", -1, -1, -1, "skipped", detail));
        log(id + ": skipped, " + detail);
    }

    /** Rows and diagnostics change under the runner's lock, which the shutdown hook takes to write them. */
    private synchronized void addRow(Row row) {
        rows.add(row);
    }

    private synchronized void addDiagnostic(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    private void failed(String id, int rep, String detail) {
        addRow(new Row(id, rep, PROCESS, "-1", -1, -1, -1, "failed", detail));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Result files

    private void write(List<Entry> manifest, String version, Path jar, String host, String runId, String loadAtStart)
            throws IOException {
        Path out = options.out();
        Files.createDirectories(out);
        StringBuilder results = new StringBuilder(
            "tool,tool_version,host,run_id,file_id,rep,phase,ms,elements,errors,warnings,status,detail\n");
        for (Row r : rows) {
            results.append(csv(TOOL, version, host, runId, r.fileId(), r.rep(), r.phase(), r.ms(), r.elements(),
                r.errors(), r.warnings(), r.status(), r.detail())).append('\n');
        }
        Files.writeString(out.resolve("results.csv"), results, StandardCharsets.UTF_8);

        StringBuilder diag = new StringBuilder("tool,file_id,member,source,severity,line,column,code,message\n");
        for (Diagnostic d : diagnostics) {
            diag.append(csv(TOOL, d.fileId(), d.member(), d.source(), d.severity(), d.line(), d.column(), d.code(),
                d.message())).append('\n');
        }
        Files.writeString(out.resolve("diagnostics.csv"), diag, StandardCharsets.UTF_8);

        Runtime rt = Runtime.getRuntime();
        Map<String, String> env = new TreeMap<>();
        env.put("tool", TOOL);
        env.put("tool.version", version);
        env.put("host", host);
        env.put("run.id", runId);
        env.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " "
            + System.getProperty("os.arch"));
        env.put("cpus", Integer.toString(rt.availableProcessors()));
        env.put("java", System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
        env.put("heap.max.mb", Long.toString(rt.maxMemory() / (1024 * 1024)));
        env.put("load.average.start", loadAtStart);
        env.put("load.average.end", loadAverage());
        env.put("pilot.jar", String.valueOf(jar));
        env.put("library", options.library().toString());
        env.put("library.extra", options.extraLibraries().stream().map(Path::toString)
            .collect(Collectors.joining(", ")));
        env.put("manifest", options.manifest().toString());
        env.put("manifest.sha256", Files.isRegularFile(options.manifest())
            ? sha256(Files.readAllBytes(options.manifest())) : "");
        env.put("repetitions", options.reps() + " (1 above " + options.largeLines() + " lines)");
        env.put("release", options.release() == null ? "" : options.release().toString());
        env.put("only", String.join(",", options.only()));
        env.put("pilot.kpar", "members parsed into one resource each, indexed together, then validated");
        StringBuilder props = new StringBuilder();
        env.forEach((k, v) -> props.append(k).append('=').append(v.replace("\\", "\\\\").replace("\n", " "))
            .append('\n'));
        Files.writeString(out.resolve("environment.properties"), props, StandardCharsets.UTF_8);

        Files.writeString(out.resolve("report.md"), report(manifest, version, host), StandardCharsets.UTF_8);
        log("wrote " + out + " (" + rows.size() + " rows, " + diagnostics.size() + " diagnostics)");
        Files.writeString(out.resolve("run-log.txt"), runLog, StandardCharsets.UTF_8);
    }

    /** One table: per model the process time (first repetition / median of the others), errors and warnings. */
    String report(List<Entry> manifest, String version, String host) {
        StringBuilder sb = new StringBuilder("# SysML v2 Pilot Implementation ").append(version).append(" on ")
            .append(host).append("\n\nMilliseconds: first repetition / median of the others. `process` is parsing ")
            .append("and validating the model with SysMLInteractive (the members of a KPAR together). ")
            .append("Errors and warnings are from the first repetition.\n\n");
        for (Row r : rows) {
            if (RUN.equals(r.fileId()) && "ok".equals(r.status())) {
                sb.append(String.format(Locale.ROOT, "Startup (standard library loaded): %.0f ms\n\n",
                    Double.parseDouble(r.ms())));
            }
        }
        sb.append("| Model | Lines | process | Errors | Warnings |\n|---|---:|---:|---:|---:|\n");
        Set<String> done = new LinkedHashSet<>();
        for (Entry e : manifest) {
            List<Row> mine = rows.stream().filter(r -> r.fileId().equals(e.id())).toList();
            if (mine.isEmpty() || !done.add(e.id())) {
                continue;
            }
            List<Double> ok = mine.stream().filter(r -> "ok".equals(r.status()))
                .map(r -> Double.parseDouble(r.ms())).toList();
            String cell;
            if (ok.isEmpty()) {
                cell = mine.get(0).status();
            } else if (ok.size() == 1) {
                cell = String.format(Locale.ROOT, "%.1f", ok.get(0));
            } else {
                cell = String.format(Locale.ROOT, "%.1f / %.1f", ok.get(0), median(ok.subList(1, ok.size())));
            }
            Row first = mine.get(0);
            boolean counted = "ok".equals(first.status());
            sb.append("| ").append(e.id()).append(" | ").append(e.lines() < 0 ? "" : Long.toString(e.lines()))
                .append(" | ").append(cell).append(" | ").append(counted ? Long.toString(first.errors()) : "–")
                .append(" | ").append(counted ? Long.toString(first.warnings()) : "–").append(" |\n");
        }
        return sb.toString();
    }

    static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }

    /** A CSV record: a field is quoted when it holds a comma, a double quote, CR or LF; quotes are doubled. */
    static String csv(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            String s = fields[i] == null ? "" : String.valueOf(fields[i]);
            if (i > 0) {
                sb.append(',');
            }
            boolean quote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0
                || s.indexOf('\r') >= 0;
            sb.append(quote ? '"' + s.replace("\"", "\"\"") + '"' : s);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------------------------------------------
    // Environment

    /** The jar that SysMLInteractive was loaded from, or null. */
    static Path pilotJar() {
        try {
            return Path.of(SysMLInteractive.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            return null;
        }
    }

    /** The pilot version: from the jar file name, else the jar's manifest or Maven properties. */
    static String pilotVersion(Path jar) {
        if (jar != null) {
            Matcher m = JAR_VERSION.matcher(jar.getFileName().toString());
            if (m.find()) {
                return m.group(1);
            }
        }
        String version = SysMLInteractive.class.getPackage().getImplementationVersion();
        if (version != null) {
            return version;
        }
        try (InputStream in = SysMLInteractive.class.getResourceAsStream(KERNEL_POM)) {
            if (in != null) {
                Properties pom = new Properties();
                pom.load(in);
                return pom.getProperty("version", "unknown");
            }
        } catch (IOException e) {
            // fall through
        }
        return "unknown";
    }

    static String host() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            String name = System.getenv("COMPUTERNAME") != null ? System.getenv("COMPUTERNAME")
                : System.getenv("HOSTNAME");
            return name == null ? "unknown" : name;
        }
    }

    /** The system load average of the last minute (-1 where the platform has none): other work skews times. */
    static String loadAverage() {
        return String.format(Locale.ROOT, "%.2f",
            ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
    }

    static String heap() {
        Runtime rt = Runtime.getRuntime();
        return "heap " + (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024) + " MB";
    }

    static String ms(long startNanos) {
        return String.format(Locale.ROOT, "%.3f", (System.nanoTime() - startNanos) / 1_000_000.0);
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The exception and its root cause on one line. */
    static String describe(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String text = t + (root == t ? "" : " (caused by " + root + ")");
        text = text.replaceAll("[\\r\\n]+", " ");
        return text.length() > 500 ? text.substring(0, 500) + "..." : text;
    }

    private static String trace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] frames = t.getStackTrace();
        for (int i = 0; i < Math.min(frames.length, 15); i++) {
            sb.append("\n    at ").append(frames[i]);
        }
        return sb.toString();
    }

    synchronized void log(String message) {
        String line = LocalTime.now().format(CLOCK) + " " + message;
        runLog.append(line).append('\n');
        System.out.println(line);
    }
}

/*
 * CameoPerfRunner.groovy: times Cameo / CATIA Magic (SysML v2) on the models of perf/cameo/corpus and writes a
 * result directory, perf/cameo/results/cameo_<host>_<time>/, to commit and push. See perf/cameo/README.md.
 *
 * Run it as a Groovy macro: Tools > Macros > Organize Macros > Add (a name, language Groovy, File: this file) >
 * OK > Run. It asks for the sysmlv2-validator checkout unless PERF_REPO is set, creates a scratch SysML v2 project
 * in the local repository, and shows a progress dialog that can be cancelled. Nothing is saved to the project.
 *
 * For every model and repetition, in this order, it times:
 *   import    SysMLTextualNotationService.importTextual, the documented textual import, in a session
 *   count     counting the imported elements (documented API)
 *   validate  Cameo's SysML v2 validation of the imported model, active and passive suites (undocumented API)
 *   export    SysMLTextualNotationService.exportTextual of the imported roots (documented API)
 *   dispose   deleting the imported model again, in a session (documented API); the undo history is then
 *             cleared, so that deleted models do not stay in memory
 *   build     parse and name resolution into a transient model, with the text diagnostics (undocumented API)
 * The import comes first, so that its first repetition is the model's first run in Cameo. An undocumented step
 * that fails is recorded as "unavailable" and the run goes on.
 *
 * Checked against MSOSA / CATIA Magic 2026x Refresh1 (build 2026.1.0-597), SysML v2 plugin 2026x Refresh1.
 * All tool classes are looked up by name, so the script also compiles outside Cameo: validator-cli's
 * CameoPerfRunnerScriptTest runs it with a stand-in for Cameo (the binding variable perfApi).
 */

import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.text.SimpleDateFormat

// ---------------------------------------------------------------------------------------------------------------
// Settings. Each can also be given as a Java system property (-DPERF_REPO=...) or an environment variable.

/** The sysmlv2-validator checkout; asked for when empty. */
String PERF_REPO = setting('PERF_REPO', '')
/** Repetitions per model. */
int PERF_REPETITIONS = setting('PERF_REPETITIONS', '3') as int
/** Models with more lines than this run once. */
int PERF_LARGE_LINES = setting('PERF_LARGE_LINES', '10000') as int
/** Only these model ids, comma-separated (empty: all). */
String PERF_ONLY = setting('PERF_ONLY', '')
/** Skip these model ids, comma-separated. */
String PERF_SKIP = setting('PERF_SKIP', '')
/** The OMG SysML-v2-Release checkout, for the release: entries (empty: SYSML_RELEASE_PATH, else skipped). */
String PERF_RELEASE = setting('PERF_RELEASE', setting('SYSML_RELEASE_PATH', ''))
/** Run the undocumented steps (build, validate). */
boolean PERF_UNDOCUMENTED = Boolean.parseBoolean(setting('PERF_UNDOCUMENTED', 'true'))

// ---------------------------------------------------------------------------------------------------------------

String setting(String name, String fallback) {
    if (binding.hasVariable(name)) {
        return String.valueOf(binding.getVariable(name))
    }
    String value = System.getProperty(name)
    if (value == null || value.isEmpty()) {
        value = System.getenv(name)
    }
    return value == null || value.isEmpty() ? fallback : value
}

/** A class of the modeling tool, by name. */
static Class tool(String name) {
    ClassLoader context = Thread.currentThread().contextClassLoader
    try {
        return Class.forName(name, true, context)
    } catch (ClassNotFoundException e) {
        return Class.forName(name)
    }
}

static def application() {
    tool('com.nomagic.magicdraw.core.Application').getInstance()
}

/** A CSV record: fields quoted when they hold a comma, a quote or a line break (as PerfResults.csv in Java). */
static String csv(List fields) {
    fields.collect { f ->
        String s = f == null ? '' : String.valueOf(f)
        boolean quote = s.contains(',') || s.contains('"') || s.contains('\n') || s.contains('\r')
        quote ? '"' + s.replace('"', '""') + '"' : s
    }.join(',')
}

static String sha256(byte[] content) {
    MessageDigest.getInstance('SHA-256').digest(content).collect { String.format('%02x', it & 0xff) }.join('')
}

static String ms(long startNanos) {
    String.format(Locale.ROOT, '%.3f', (System.nanoTime() - startNanos) / 1_000_000.0d)
}

static String heap() {
    Runtime rt = Runtime.getRuntime()
    'heap ' + (rt.totalMemory() - rt.freeMemory()).intdiv(1024 * 1024) + ' MB'
}

/** The system load average of the last minute (-1 where the platform has none): other work skews times. */
static String loadAverage() {
    String.format(Locale.ROOT, '%.2f',
        java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
}

static String shortError(Throwable t) {
    Throwable root = t
    while (root.cause != null && root.cause != root) {
        root = root.cause
    }
    String text = root.getClass().simpleName + (root.message ? ': ' + root.message : '')
    text.replaceAll('[\\r\\n]+', ' ').take(300)
}

/**
 * The Cameo side. Classes of the SysML v2 plugin are looked up through its class loader, so that a class that
 * is missing or renamed in another Cameo build fails only the step that uses it.
 */
class CameoApi {
    static final String SYSML_PLUGIN = 'com.dassault_systemes.modeler.magic.sysml'
    final List<ClassLoader> loaders = []

    CameoApi() {
        for (def plugin : plugins()) {
            if (plugin.descriptor?.getID() == SYSML_PLUGIN) {
                loaders.add(plugin.getClass().getClassLoader())
            }
        }
        loaders.add(Thread.currentThread().contextClassLoader)
        loaders.add(CameoApi.classLoader)
    }

    static Collection plugins() {
        Class.forName('com.nomagic.magicdraw.plugins.PluginUtils', true,
            Thread.currentThread().contextClassLoader).getPlugins()
    }

    Class cls(String name) {
        Throwable last = null
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue
            }
            try {
                return Class.forName(name, true, loader)
            } catch (Throwable t) {
                last = t
            }
        }
        throw new ClassNotFoundException(name, last)
    }

    def app() {
        cls('com.nomagic.magicdraw.core.Application').getInstance()
    }

    Map environment() {
        Map env = [:]
        for (def plugin : plugins()) {
            def d = plugin.descriptor
            if (d?.getID() == SYSML_PLUGIN) {
                env['cameo.sysml2.plugin'] = d.getName() + ' ' + d.getVersion()
            }
        }
        env['cameo.openapi.version'] = app().getOpenAPIVersion()
        env['cameo.install'] = cls('com.nomagic.magicdraw.core.ApplicationEnvironment').getInstallRoot()
        env
    }

    def createProject(String name) {
        def project = cls('com.dassault_systemes.modeler.magic.sysml.core.SysMLProjectHelper').createUPSProject(name)
        if (project == null) {
            project = app().getProjectsManager().getActiveProject()
            if (project == null || project.getName() != name) {
                throw new IllegalStateException('SysMLProjectHelper.createUPSProject(' + name + ') returned null')
            }
        }
        project
    }

    /** The root namespaces of the project's own model (not of the libraries). */
    List roots(def project) {
        new ArrayList(cls('com.dassault_systemes.modeler.kerml.model.RootNamespaces')
            .getRoots(project.getPrimaryProject()))
    }

    /** Parse and resolve into a transient model; returns [errors, warnings, diagnostics]. */
    List build(def project, String text) {
        def allRoots = cls('com.dassault_systemes.modeler.kerml.model.RootNamespaces').getAllRoots(project)
        // As SysMLTextualNotationService.importTextual builds the model before copying it into the project
        def builder = cls('com.dassault_systemes.modeler.sysml.textual.project.SysMLTransientModelBuilder')
            .newInstance(project)
        def unit = builder.build(text, allRoots)
        List diagnostics = []
        for (def d : unit.getDiagnostics()) {
            diagnostics.add(['text', d.getSeverity().name(), d.getLine(), d.getColumn(), '', d.getMessage()])
        }
        for (def d : unit.getValidationDiagnostics()) {
            diagnostics.add(['text-validation', d.getSeverity().name(), d.getLine(), d.getColumn(), '',
                             d.getMessage()])
        }
        long errors = diagnostics.count { it[1] == 'ERROR' }
        long warnings = diagnostics.count { it[1] == 'WARNING' }
        [errors, warnings, diagnostics]
    }

    void importText(def project, String text, String label) {
        def service = cls('com.dassault_systemes.modeler.magic.sysml.textual.SysMLTextualNotationService')
        inSession(project, label) { service.importTextual(project, text) }
    }

    long count(List roots) {
        long n = 0
        ArrayDeque stack = new ArrayDeque(roots)
        while (!stack.isEmpty()) {
            def e = stack.pop()
            n++
            for (def child : e.getOwnedElement()) {
                stack.push(child)
            }
        }
        n
    }

    /** Validate with the active and passive suites; returns [errors, warnings, diagnostics, suites]. */
    List validate(def project, List roots) {
        def service = cls('com.dassault_systemes.modeler.kerml.validation.ValidationService')
            .getInstanceByProject(project)
        def progress = cls('com.nomagic.task.EmptyProgressStatus').getDefault()
        def params = service.createParams(roots).addActiveSuites().addPassiveSuites().setProgressStatus(progress)
        def failures = cls('com.dassault_systemes.modeler.kerml.validation.ValidationUtils').validateRecursively(params)
        List diagnostics = []
        for (def f : failures) {
            def element = f.getElement()
            String where
            try {
                where = element?.getQualifiedName() ?: ''
            } catch (Throwable ignored) {
                where = String.valueOf(element)
            }
            diagnostics.add(['validation', f.getSeverityLevel().name(), 0, 0, f.getValidationRuleName(),
                             (where ? where + ': ' : '') + f.getMessage()])
        }
        long errors = diagnostics.count { it[1] == 'ERROR' || it[1] == 'FATAL' }
        long warnings = diagnostics.count { it[1] == 'WARNING' }
        [errors, warnings, diagnostics, params.getSuites().size()]
    }

    long export(List roots) {
        def service = cls('com.dassault_systemes.modeler.magic.sysml.textual.SysMLTextualNotationService')
        long chars = 0
        for (def root : roots) {
            chars += String.valueOf(service.exportTextual(root)).length()
        }
        chars
    }

    void dispose(def project, List roots, String label) {
        def elements = cls('com.dassault_systemes.modeler.kerml.model.Elements')
        inSession(project, label) { elements.dispose(roots) }
    }

    /** Forget the undo history, which otherwise keeps every imported and deleted model in memory. */
    void clearUndo(def project) {
        cls('com.dassault_systemes.modeler.foundation.editing.Editing').getInstance(project).clearHistory()
    }

    void close(def project) {
        app().getProjectsManager().closeProjectNoSave(project)
    }

    /**
     * Run a model change in a session, as the Open API's SysML v2 examples do. A failure propagates, so that the
     * session is cancelled rather than committed.
     */
    private void inSession(def project, String label, Closure change) {
        cls('com.nomagic.magicdraw.openapi.uml.SessionManager').getInstance()
            .executeInsideSession(project, label, change as Runnable)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// The run

boolean headless = binding.hasVariable('perfApi')
StringBuilder runLog = new StringBuilder()
Closure log = { String message ->
    String line = new SimpleDateFormat('HH:mm:ss').format(new Date()) + ' ' + message
    runLog.append(line).append('\n')
    if (!headless) {
        application().getGUILog().log('[perf] ' + message)
    }
}

Path repo = resolveRepo(PERF_REPO, headless)
if (repo == null) {
    return 'cancelled: no repository selected'
}
Path manifestFile = repo.resolve('perf/cameo/corpus/manifest.csv')
if (!Files.isRegularFile(manifestFile)) {
    String message = 'No manifest at ' + manifestFile + '. Select the root of the sysmlv2-validator checkout.'
    if (!headless) {
        application().getGUILog().showError(message)
    }
    return 'failed: ' + message
}
Path release = PERF_RELEASE ? Paths.get(PERF_RELEASE) : null
Set<String> only = PERF_ONLY.split(',').collect { it.trim() }.findAll { it } as Set
Set<String> skip = PERF_SKIP.split(',').collect { it.trim() }.findAll { it } as Set

List<Map> manifest = []
for (String line : Files.readAllLines(manifestFile, StandardCharsets.UTF_8).drop(1)) {
    if (!line.trim() || line.startsWith('#')) {
        continue
    }
    String[] f = line.split(',', 9)
    manifest.add([id: f[0], path: f[1], family: f[2], scale: f[3], expect: f[4], lines: f[5] as long,
                  bytes: f[6] as long, sha256: f[7], description: f[8]])
}

String host
try {
    host = InetAddress.getLocalHost().getHostName()
} catch (IOException e) {
    host = System.getenv('COMPUTERNAME') ?: System.getenv('HOSTNAME') ?: 'unknown'
}
String loadAtStart = loadAverage()
String stamp = new SimpleDateFormat('yyyyMMdd-HHmmss').format(new Date())
SimpleDateFormat utc = new SimpleDateFormat('yyyyMMdd-HHmmss')
utc.setTimeZone(TimeZone.getTimeZone('UTC'))
String runId = utc.format(new Date())
Path outDir = repo.resolve('perf/cameo/results')
    .resolve('cameo_' + host.replaceAll('[^A-Za-z0-9.-]', '-') + '_' + stamp)
Files.createDirectories(outDir)

def api = headless ? binding.getVariable('perfApi') : new CameoApi()
List<List> rows = []
List<List> diagnostics = []
Map env = [:]
String toolVersion = 'unknown'

Closure row = { String fileId, int rep, String phase, String msText, long elements, long errors, long warnings,
                String status, String detail ->
    rows.add([fileId, rep, phase, msText, elements, errors, warnings, status, detail])
}

Closure runAll = { progress ->
    try {
        env.putAll(api.environment())
    } catch (Throwable t) {
        log('environment: ' + shortError(t))
    }
    toolVersion = String.valueOf(env['cameo.sysml2.plugin'] ?: 'unknown')

    long t0 = System.nanoTime()
    def project
    try {
        project = api.createProject('SysMLv2Perf_' + stamp)
        row('*', 1, 'create_project', ms(t0), -1, -1, -1, 'ok', heap())
    } catch (Throwable t) {
        row('*', 1, 'create_project', '-1', -1, -1, -1, 'failed', shortError(t))
        log('could not create the scratch project: ' + shortError(t))
        return
    }

    List<Map> todo = manifest.findAll { (only.isEmpty() || only.contains(it.id)) && !skip.contains(it.id) }
    long steps = todo.sum(0L) { (it.lines as long) > PERF_LARGE_LINES ? 1L : (long) PERF_REPETITIONS } as long
    if (progress != null) {
        progress.setMax(steps)
        progress.setCurrent(0)
    }
    try {
        for (Map entry : todo) {
            if (progress != null && progress.isCancel()) {
                log('cancelled')
                break
            }
            Path file = entry.path.startsWith('release:')
                ? (release == null ? null : release.resolve(entry.path.substring('release:'.length())))
                : repo.resolve(entry.path)
            if (file == null || !Files.isRegularFile(file)) {
                row(entry.id, 1, 'all', '-1', -1, -1, -1, 'skipped', 'file not found: ' + (file ?: entry.path))
                log(entry.id + ': skipped, file not found')
                continue
            }
            byte[] content = Files.readAllBytes(file)
            String note = sha256(content) == entry.sha256 ? '' : 'the file differs from the manifest (sha256)'
            if (note) {
                log(entry.id + ': ' + note)
            }
            String text = new String(content, StandardCharsets.UTF_8)
            int reps = (entry.lines as long) > PERF_LARGE_LINES ? 1 : PERF_REPETITIONS
            for (int rep = 1; rep <= reps; rep++) {
                if (progress != null) {
                    if (progress.isCancel()) {
                        break
                    }
                    progress.setDescription(entry.id + ' (' + rep + '/' + reps + ')')
                }
                System.gc()
                log(entry.id + ' repetition ' + rep + ' of ' + reps)
                timeModel(api, project, entry, text, rep, note, PERF_UNDOCUMENTED, row, diagnostics, log)
                if (progress != null) {
                    progress.increase()
                }
            }
        }
    } finally {
        try {
            api.close(project)
        } catch (Throwable t) {
            log('closing the scratch project: ' + shortError(t))
        }
    }
}

try {
    if (headless) {
        runAll(null)
    } else {
        Closure task = { progress -> runAll(progress) }
        tool('com.nomagic.ui.ProgressStatusRunner').runWithProgressStatus(
            task.asType(tool('com.nomagic.task.RunnableWithProgress')), 'SysML v2 performance run', true, 0)
    }
} catch (Throwable t) {
    log('run failed: ' + shortError(t))
} finally {
    env['load.average.start'] = loadAtStart
    env['load.average.end'] = loadAverage()
    env['perf.only'] = PERF_ONLY
    env['perf.skip'] = PERF_SKIP
    env['perf.release'] = PERF_RELEASE
    env['perf.undocumented'] = String.valueOf(PERF_UNDOCUMENTED)
    writeResults(outDir, rows, diagnostics, env, toolVersion, host, runId, manifest, manifestFile,
        PERF_REPETITIONS, PERF_LARGE_LINES, runLog)
}
String done = 'Wrote ' + outDir + ' (' + rows.size() + ' rows). Commit and push it.'
log(done)
Files.writeString(outDir.resolve('run-log.txt'), runLog.toString(), StandardCharsets.UTF_8)
if (!headless) {
    application().getGUILog().showMessage(done)
}
return done

// ---------------------------------------------------------------------------------------------------------------

/** One repetition of one model: import, count, validate, export, dispose, then build. */
void timeModel(def api, def project, Map entry, String text, int rep, String note, boolean undocumented,
               Closure row, List<List> diagnostics, Closure log) {
    String id = entry.id
    List before
    try {
        before = api.roots(project)
    } catch (Throwable e) {
        row(id, rep, 'import', '-1', -1, -1, -1, 'failed', 'reading the root namespaces: ' + shortError(e))
        log(id + ' roots: ' + shortError(e))
        return
    }
    Closure added = { api.roots(project).findAll { r -> !before.any { it.is(r) } } }

    long t = System.nanoTime()
    try {
        api.importText(project, text, 'Import ' + id)
        row(id, rep, 'import', ms(t), -1, -1, -1, 'ok', note ?: heap())
    } catch (Throwable e) {
        row(id, rep, 'import', '-1', -1, -1, -1, 'failed', shortError(e))
        log(id + ' import: ' + shortError(e))
        removeQuietly(api, project, added, id, log)
        return
    }
    List imported
    try {
        imported = added()
    } catch (Throwable e) {
        log(id + ' roots after the import: ' + shortError(e))
        imported = []
    }
    if (imported.isEmpty()) {
        // Nothing to measure: say so rather than recording zeros
        row(id, rep, 'count', '-1', -1, -1, -1, 'failed', 'the import added no root namespace')
        log(id + ': the import added no root namespace')
        return
    }

    t = System.nanoTime()
    try {
        long elements = api.count(imported)
        row(id, rep, 'count', ms(t), elements, -1, -1, 'ok', imported.size() + ' roots')
    } catch (Throwable e) {
        row(id, rep, 'count', '-1', -1, -1, -1, 'failed', shortError(e))
        log(id + ' count: ' + shortError(e))
    }

    if (undocumented) {
        t = System.nanoTime()
        try {
            List checked = api.validate(project, imported)
            String took = ms(t)
            if ((checked[3] as int) == 0) {
                row(id, rep, 'validate', '-1', -1, -1, -1, 'unavailable', 'no validation suites')
            } else {
                row(id, rep, 'validate', took, -1, checked[0] as long, checked[1] as long, 'ok',
                    checked[3] + ' suites')
                if (rep == 1) {
                    checked[2].each { d -> diagnostics.add([id] + d) }
                }
            }
        } catch (Throwable e) {
            row(id, rep, 'validate', '-1', -1, -1, -1, 'unavailable', shortError(e))
            log(id + ' validate: ' + shortError(e))
        }
    }

    t = System.nanoTime()
    try {
        long chars = api.export(imported)
        row(id, rep, 'export', ms(t), -1, -1, -1, 'ok', chars + ' characters')
    } catch (Throwable e) {
        row(id, rep, 'export', '-1', -1, -1, -1, 'failed', shortError(e))
        log(id + ' export: ' + shortError(e))
    }

    t = System.nanoTime()
    try {
        api.dispose(project, imported, 'Remove ' + id)
        String took = ms(t)
        clearUndo(api, project, id, log)
        row(id, rep, 'dispose', took, -1, -1, -1, 'ok', heap() + ' after clearing the undo history')
    } catch (Throwable e) {
        row(id, rep, 'dispose', '-1', -1, -1, -1, 'failed', shortError(e))
        log(id + ' dispose: ' + shortError(e))
        clearUndo(api, project, id, log)
    }

    // The transient build last, on the project as it was before the import
    if (undocumented) {
        t = System.nanoTime()
        try {
            List built = api.build(project, text)
            row(id, rep, 'build', ms(t), -1, built[0] as long, built[1] as long, 'ok', '')
            if (rep == 1) {
                built[2].each { d -> diagnostics.add([id] + d) }
            }
        } catch (Throwable e) {
            row(id, rep, 'build', '-1', -1, -1, -1, 'unavailable', shortError(e))
            log(id + ' build: ' + shortError(e))
        }
    }
}

/** Delete whatever a failed import left in the project, so that it cannot affect the next models. */
void removeQuietly(def api, def project, Closure added, String id, Closure log) {
    try {
        List leftover = added()
        if (!leftover.isEmpty()) {
            api.dispose(project, leftover, 'Remove ' + id)
            log(id + ': removed ' + leftover.size() + ' root namespaces left by the failed import')
        }
    } catch (Throwable e) {
        log(id + ' removing what the failed import left: ' + shortError(e))
    }
    clearUndo(api, project, id, log)
}

void clearUndo(def api, def project, String id, Closure log) {
    try {
        api.clearUndo(project)
    } catch (Throwable e) {
        log(id + ' clearing the undo history: ' + shortError(e))
    }
}

Path resolveRepo(String configured, boolean headless) {
    if (configured) {
        return Paths.get(configured)
    }
    if (headless) {
        return null
    }
    Path chosen = null
    Runnable ask = {
        JFileChooser chooser = new JFileChooser()
        chooser.setDialogTitle('Select the sysmlv2-validator checkout (the folder that holds perf/cameo)')
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        if (chooser.showOpenDialog(application().getMainFrame()) == JFileChooser.APPROVE_OPTION) {
            chosen = chooser.getSelectedFile().toPath()
        }
    }
    if (SwingUtilities.isEventDispatchThread()) {
        ask.run()
    } else {
        SwingUtilities.invokeAndWait(ask)
    }
    return chosen
}

void writeResults(Path outDir, List<List> rows, List<List> diagnostics, Map env, String toolVersion, String host,
                  String runId, List<Map> manifest, Path manifestFile, int repetitions, int largeLines,
                  StringBuilder runLog) {
    StringBuilder results = new StringBuilder(
        'tool,tool_version,host,run_id,file_id,rep,phase,ms,elements,errors,warnings,status,detail\n')
    for (List r : rows) {
        results.append(csv(['cameo', toolVersion, host, runId] + r)).append('\n')
    }
    Files.writeString(outDir.resolve('results.csv'), results.toString(), StandardCharsets.UTF_8)

    StringBuilder diag = new StringBuilder('tool,file_id,source,severity,line,column,code,message\n')
    for (List d : diagnostics) {
        diag.append(csv(['cameo'] + d)).append('\n')
    }
    Files.writeString(outDir.resolve('diagnostics.csv'), diag.toString(), StandardCharsets.UTF_8)

    Runtime rt = Runtime.getRuntime()
    Map all = new TreeMap(env)
    all['tool'] = 'cameo'
    all['tool.version'] = toolVersion
    all['host'] = host
    all['run.id'] = runId
    all['os'] = System.getProperty('os.name') + ' ' + System.getProperty('os.version') + ' ' +
        System.getProperty('os.arch')
    all['cpus'] = String.valueOf(rt.availableProcessors())
    all['java'] = System.getProperty('java.vendor') + ' ' + System.getProperty('java.version')
    all['heap.max.mb'] = String.valueOf(rt.maxMemory().intdiv(1024 * 1024))
    all['groovy'] = GroovySystem.getVersion()
    all['repetitions'] = repetitions + ' (1 above ' + largeLines + ' lines)'
    all['manifest.sha256'] = sha256(Files.readAllBytes(manifestFile))
    StringBuilder props = new StringBuilder()
    all.each { k, v ->
        props.append(k).append('=').append(String.valueOf(v).replace('\\', '\\\\').replace('\n', ' ')).append('\n')
    }
    Files.writeString(outDir.resolve('environment.properties'), props.toString(), StandardCharsets.UTF_8)

    StringBuilder report = new StringBuilder('# Cameo ' + toolVersion + ' on ' + host + '\n\n')
    report.append('Milliseconds: first repetition / median of the others. ')
    report.append('Errors and warnings are from the first repetition.\n\n')
    rows.findAll { it[0] == '*' && it[7] == 'ok' }.each {
        report.append('Scratch project created in ').append(it[3]).append(' ms\n\n')
    }
    report.append('| Model | Lines | build | import | Elements | validate | export | dispose |')
    report.append(' Text errors / warnings | Validation errors / warnings |\n')
    report.append('|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n')
    for (Map e : manifest) {
        List mine = rows.findAll { it[0] == e.id }
        if (mine.isEmpty()) {
            continue
        }
        Closure cell = { String phase ->
            List ok = mine.findAll { it[2] == phase && it[7] == 'ok' }.sort { it[1] }
            if (ok.isEmpty()) {
                List other = mine.findAll { it[2] == phase }
                return other ? other[0][7] : '–'
            }
            double first = ok[0][3] as double
            if (ok.size() == 1) {
                return String.format(Locale.ROOT, '%.1f', first)
            }
            List<Double> warm = ok.drop(1).collect { it[3] as double }.sort()
            int n = warm.size()
            double median = n % 2 == 1 ? warm[n.intdiv(2)] : (warm[n.intdiv(2) - 1] + warm[n.intdiv(2)]) / 2
            String.format(Locale.ROOT, '%.1f / %.1f', first, median)
        }
        Closure counts = { String phase ->
            List r = mine.findAll { it[2] == phase && it[1] == 1 }
            r ? (r[0][7] == 'ok' ? r[0][5] + ' / ' + r[0][6] : r[0][7]) : '–'
        }
        List countRow = mine.find { it[2] == 'count' && it[7] == 'ok' }
        report.append("| ${e.id} | ${e.lines} | ${cell('build')} | ${cell('import')} | ")
        report.append("${countRow ? countRow[4] : ''} | ${cell('validate')} | ${cell('export')} | ")
        report.append("${cell('dispose')} | ${counts('build')} | ${counts('validate')} |\n")
    }
    Files.writeString(outDir.resolve('report.md'), report.toString(), StandardCharsets.UTF_8)
    Files.writeString(outDir.resolve('run-log.txt'), runLog.toString(), StandardCharsets.UTF_8)
}

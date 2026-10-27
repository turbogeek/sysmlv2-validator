/*
 * CameoConformanceRunner.groovy: loads every test file and KPAR of conformance/manifest.csv into Cameo / CATIA Magic
 * (SysML v2) and records what Cameo reports for it: whether it imports, its text errors and warnings with their
 * lines, and Cameo's validation results. It writes conformance/results/cameo_<host>_<time>/ to commit and push.
 * The comparison with this validator and the pilot implementation is then made with
 *   java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain conformance compare
 * See conformance/README.md.
 *
 * Run it as a Groovy macro: Tools > Macros > Organize Macros > Add (a name, language Groovy, File: this file) >
 * OK > Run. It asks for the sysmlv2-validator checkout unless CONF_REPO is set, creates a scratch SysML v2 project
 * in the local repository, and shows a progress dialog that can be cancelled. Nothing is saved to the project.
 *
 * For every model, in this order:
 *   import    SysMLTextualNotationService.importTextual, the documented textual import, in a session
 *   count     counting the imported elements (documented API)
 *   validate  Cameo's SysML v2 validation of the imported model, active and passive suites (undocumented API)
 *   dispose   deleting the imported model again, in a session (documented API); the undo history is then cleared
 *   build     parse and name resolution into a transient model: the text errors and warnings with their lines
 *             (undocumented API)
 * Until Cameo's KPAR import is released, the .sysml members of a KPAR are imported and built together as Cameo's
 * own Import of several files does (TextualModelHelper, undocumented API), each diagnostic in its member; where that
 * API is missing, the members are joined into one text and the lines mapped back to the members. Give CameoApi an
 * importKpar method and it is used for the import. A KPAR this macro cannot read (see readModel) is skipped, since
 * that is the macro's check, not Cameo's; with importKpar, Cameo is given it. An undocumented step that fails is
 * recorded as "unavailable" and the run goes on; without the build step a model's text is not checked, and the
 * comparison gives it no verdict. Times are recorded for information only.
 *
 * Checked against MSOSA / CATIA Magic 2026x Refresh1 (build 2026.1.0-597), SysML v2 plugin 2026x Refresh1.
 * All tool classes are looked up by name, so the script also compiles outside Cameo: validator-cli's
 * CameoConformanceRunnerScriptTest runs it with a stand-in for Cameo (the binding variable confApi).
 */

import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

// ---------------------------------------------------------------------------------------------------------------
// Settings. Each can also be given as a macro argument, a Java system property or an environment variable.

/** The sysmlv2-validator checkout; asked for when empty. */
String CONF_REPO = setting('CONF_REPO', '')
/** Only these model ids, comma-separated (empty: all). */
String CONF_ONLY = setting('CONF_ONLY', '')
/** Skip these model ids, comma-separated. */
String CONF_SKIP = setting('CONF_SKIP', '')
/** Run the undocumented steps (validate, build), which give the validation results and the text diagnostics. */
boolean CONF_UNDOCUMENTED = Boolean.parseBoolean(setting('CONF_UNDOCUMENTED', 'true'))

// ---------------------------------------------------------------------------------------------------------------

String setting(String name, String fallback) {
    // A macro argument with Null ticked is bound to null, one left blank to an empty string: unset
    def bound = binding.hasVariable(name) ? binding.getVariable(name) : null
    if (bound != null && String.valueOf(bound).trim()) {
        return String.valueOf(bound).trim()
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

/** A CSV record: fields quoted when they hold a comma, a quote or a line break (as Results.csv in Java). */
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

/** The system load average of the last minute (-1 where the platform has none). */
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

/** Cameo's severities as the comparison reads them: error, warning or info. */
static String severity(String name) {
    switch (String.valueOf(name).toUpperCase(Locale.ROOT)) {
        case 'FATAL':
        case 'ERROR':
            return 'error'
        case 'WARNING':
            return 'warning'
        default:
            return 'info'
    }
}

/**
 * A model to load. A plain file: its text. A KPAR: its .sysml members (path, text, and the line each starts at in
 * their joined text), the joined text, and how many .kerml members it has, which Cameo's textual import does not
 * read. A KPAR that cannot be read gives an error message instead, for the faults the validator's reader rejects.
 */
static Map readModel(Path file) {
    String name = file.fileName.toString().toLowerCase(Locale.ROOT)
    if (!name.endsWith('.kpar')) {
        return [text: new String(Files.readAllBytes(file), StandardCharsets.UTF_8), members: [], kerml: 0]
    }
    List members = []
    int kerml = 0
    ZipFile zip
    try {
        zip = new ZipFile(file.toFile())
    } catch (ZipException e) {
        return [invalid: 'invalid KPAR: not a zip archive (' + shortError(e) + ')', kpar: true]
    }
    try {
        ZipEntry project = zip.getEntry('.project.json')
        if (project == null || project.directory) {
            return [invalid: 'invalid KPAR: no .project.json', kpar: true]
        }
        if (!jsonObject(utf8(zip.getInputStream(project).bytes))) {
            return [invalid: 'invalid KPAR: .project.json is not a JSON object', kpar: true]
        }
        for (ZipEntry entry : zip.entries().toList()) {
            String path = entry.name
            String lower = path.toLowerCase(Locale.ROOT)
            if (entry.directory || !(lower.endsWith('.sysml') || lower.endsWith('.kerml'))) {
                continue
            }
            String text = utf8(zip.getInputStream(entry).bytes)
            if (text == null) {
                return [invalid: 'invalid KPAR: ' + path + ' is not UTF-8 text', kpar: true]
            }
            if (lower.endsWith('.kerml')) {
                kerml++
            } else {
                members.add([path: path, text: text.startsWith('\uFEFF') ? text.substring(1) : text])
            }
        }
    } catch (ZipException | EOFException e) {
        return [invalid: 'invalid KPAR: corrupted zip entry (' + shortError(e) + ')', kpar: true]
    } finally {
        zip.close()
    }
    if (members.isEmpty() && kerml == 0) {
        return [invalid: 'invalid KPAR: no .sysml or .kerml member', kpar: true]
    }
    StringBuilder joined = new StringBuilder()
    int line = 1
    for (Map m : members) {
        String text = m.text.endsWith('\n') ? m.text : m.text + '\n'
        m.start = line
        line += text.count('\n')
        joined.append(text)
    }
    [text: joined.toString(), members: members, kerml: kerml, kpar: true]
}

/**
 * True when the text is a JSON object, as the validator's KPAR reader requires: parsed with groovy-json, which
 * Cameo's macro engine has, else (without it) by its braces.
 */
static boolean jsonObject(String text) {
    if (text == null) {
        return false
    }
    String json = text.startsWith('\uFEFF') ? text.substring(1) : text
    try {
        def slurper = Class.forName('groovy.json.JsonSlurper').getDeclaredConstructor().newInstance()
        return slurper.parseText(json) instanceof Map
    } catch (ClassNotFoundException e) {
        String trimmed = json.trim()
        return trimmed.startsWith('{') && trimmed.endsWith('}')
    } catch (Exception e) {
        return false
    }
}

/** The text of UTF-8 bytes, or null if they are not UTF-8. */
static String utf8(byte[] bytes) {
    try {
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
    } catch (CharacterCodingException e) {
        return null
    }
}

/**
 * The .sysml members of a KPAR written into a new temporary folder, for Cameo's import of several files:
 * [dir, files, member: the member path of each file's absolute and canonical path].
 */
static Map extract(List members) {
    Path dir = Files.createTempDirectory('conformance-kpar-')
    List<File> files = []
    Map<String, String> member = [:]
    try {
        for (Map m : members) {
            Path target = dir.resolve(m.path).normalize()
            if (!target.startsWith(dir) || target == dir) {
                throw new IOException('member path outside the archive: ' + m.path)
            }
            Files.createDirectories(target.parent)
            Files.writeString(target, m.text, StandardCharsets.UTF_8)
            File f = target.toFile()
            files.add(f)
            member[f.absolutePath] = m.path
            member[f.canonicalPath] = m.path
        }
    } catch (Throwable t) {
        deleteTree(dir)
        throw t
    }
    [dir: dir, files: files, member: member]
}

/** The member a diagnostic of Cameo's build of several files is in, from the file it names. */
static String memberOf(Map extracted, String file) {
    if (file == null) {
        return ''
    }
    String path = file
    if (file.startsWith('file:')) {
        try {
            path = Paths.get(new URI(file)).toFile().absolutePath
        } catch (Exception ignored) {
            path = file
        }
    }
    String member = extracted.member[path] ?: extracted.member[file]
    if (member == null) {
        // A path Cameo wrote another way: the longest member path it ends with
        String slashed = path.replace('\\', '/')
        member = extracted.member.values().findAll { slashed.endsWith('/' + it) }.max { it.length() }
    }
    member ?: ''
}

/** Delete a temporary folder; what cannot be deleted now is deleted when Cameo exits. */
static void deleteTree(Path dir) {
    if (dir == null || !Files.exists(dir)) {
        return
    }
    try {
        Files.walk(dir).withCloseable { paths -> paths.sorted(Comparator.reverseOrder()).toList() }
            .each { Files.deleteIfExists(it) }
    } catch (Exception e) {
        Files.walk(dir).withCloseable { paths -> paths.toList() }.each { it.toFile().deleteOnExit() }
    }
}

/** Member and line in the member for a line of a KPAR's joined text: [member, line]. */
static List place(List members, int line) {
    if (members.isEmpty() || line <= 0) {
        return ['', line]
    }
    Map member = members.findAll { it.start <= line }.last()
    [member.path, line - member.start + 1]
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

    static final String TEXTUAL_HELPER = 'com.dassault_systemes.modeler.magic.sysml.core.TextualModelHelper'
    private Boolean multiFile

    /**
     * True when this Cameo build has the textual import of several files that its own Import action uses
     * (TextualModelHelper.buildFromFiles, then importTransientModelsIntoProject; undocumented API).
     */
    boolean multiFileImport() {
        if (multiFile == null) {
            try {
                Class helper = cls(TEXTUAL_HELPER)
                Class project = cls('com.dassault_systemes.modeler.foundation.project.ModelElementProject')
                Class progress = cls('com.nomagic.task.ProgressStatus')
                helper.getMethod('buildFromFiles', project, List, progress)
                helper.getMethod('importTransientModelsIntoProject', project, List, progress)
                multiFile = true
            } catch (Throwable t) {
                multiFile = false
            }
        }
        multiFile
    }

    /** Build files together into transient models; returns [errors, warnings, diagnostics], each with its file. */
    List buildFiles(def project, List<File> files) {
        def progress = cls('com.nomagic.task.EmptyProgressStatus').getDefault()
        def units = cls(TEXTUAL_HELPER).buildFromFiles(project, files, progress)
        List diagnostics = []
        for (def unit : units) {
            String file = unit.getUri()
            for (def d : unit.getDiagnostics()) {
                diagnostics.add(['text', d.getSeverity().name(), d.getLine(), d.getColumn(), '', d.getMessage(),
                                 file])
            }
            for (def d : unit.getValidationDiagnostics()) {
                diagnostics.add(['text-validation', d.getSeverity().name(), d.getLine(), d.getColumn(), '',
                                 d.getMessage(), file])
            }
        }
        long errors = diagnostics.count { it[1] == 'ERROR' }
        long warnings = diagnostics.count { it[1] == 'WARNING' }
        [errors, warnings, diagnostics]
    }

    /**
     * Import files as Cameo's Import action does: build them together, then copy their models into the project.
     * importTransientModelsIntoProject makes the model change itself (Editing.supply), so no session is opened here.
     */
    void importFiles(def project, List<File> files, String label) {
        def helper = cls(TEXTUAL_HELPER)
        def progress = cls('com.nomagic.task.EmptyProgressStatus').getDefault()
        def units = helper.buildFromFiles(project, files, progress)
        helper.importTransientModelsIntoProject(project, units.collect { it.getTransientRootNs() }, progress)
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

boolean headless = binding.hasVariable('confApi')
StringBuilder runLog = new StringBuilder()
Closure log = { String message ->
    String line = new SimpleDateFormat('HH:mm:ss').format(new Date()) + ' ' + message
    runLog.append(line).append('\n')
    if (!headless) {
        application().getGUILog().log('[conformance] ' + message)
    }
}

Path repo = resolveRepo(CONF_REPO, headless)
if (repo == null) {
    return 'cancelled: no repository selected'
}
Path manifestFile = repo.resolve('conformance/manifest.csv')
if (!Files.isRegularFile(manifestFile)) {
    String message = 'No manifest at ' + manifestFile + '. Select the root of the sysmlv2-validator checkout.'
    if (!headless) {
        application().getGUILog().showError(message)
    }
    return 'failed: ' + message
}
Set<String> only = CONF_ONLY.split(',').collect { it.trim() }.findAll { it } as Set
Set<String> skip = CONF_SKIP.split(',').collect { it.trim() }.findAll { it } as Set

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
Path outDir = repo.resolve('conformance/results')
    .resolve('cameo_' + host.replaceAll('[^A-Za-z0-9.-]', '-') + '_' + stamp)
Files.createDirectories(outDir)

def api = null
List<List> rows = []
List<List> diagnostics = []
Map env = [:]
String toolVersion = 'unknown'

Closure row = { String fileId, String phase, String msText, long elements, long errors, long warnings,
                String status, String detail ->
    rows.add([fileId, 1, phase, msText, elements, errors, warnings, status, detail])
}

List<String> changedFiles = []
int projects = 0

Closure runAll = { progress ->
    try {
        env.putAll(api.environment())
    } catch (Throwable t) {
        log('environment: ' + shortError(t))
    }
    toolVersion = String.valueOf(env['cameo.sysml2.plugin'] ?: 'unknown')

    long t0 = System.nanoTime()
    def project
    List baseline
    try {
        project = api.createProject('SysMLv2Conformance_' + stamp)
        baseline = api.roots(project)
        row('*', 'create_project', ms(t0), -1, -1, -1, 'ok', '')
    } catch (Throwable t) {
        row('*', 'create_project', '-1', -1, -1, -1, 'failed', shortError(t))
        log('could not create the scratch project: ' + shortError(t))
        return
    }

    List<Map> todo = manifest.findAll { (only.isEmpty() || only.contains(it.id)) && !skip.contains(it.id) }
    if (progress != null) {
        progress.setMax(todo.size())
        progress.setCurrent(0)
    }
    try {
        for (Map entry : todo) {
            if (progress != null) {
                if (progress.isCancel()) {
                    log('cancelled')
                    break
                }
                progress.setDescription(entry.id)
            }
            Path file = repo.resolve(entry.path)
            if (entry.path.startsWith('release:') || !Files.isRegularFile(file)) {
                row(entry.id, 'all', '-1', -1, -1, -1, 'skipped', 'file not found: ' + entry.path)
                log(entry.id + ': skipped, file not found')
                continue
            }
            boolean changed = sha256(Files.readAllBytes(file)) != entry.sha256
            if (changed) {
                changedFiles.add(entry.id)
                log(entry.id + ': the file differs from the manifest (sha256)')
            }
            log(entry.id)
            loadModel(api, project, entry, file, CONF_UNDOCUMENTED, changed, row, diagnostics, log)
            // What a model leaves in the scratch project would change the results of the next ones
            List left = leftover(api, project, baseline, entry.id, log)
            if (!left.isEmpty()) {
                try {
                    api.close(project)
                } catch (Throwable t) {
                    log('closing the scratch project: ' + shortError(t))
                }
                try {
                    project = api.createProject('SysMLv2Conformance_' + stamp + '_' + (++projects))
                    baseline = api.roots(project)
                } catch (Throwable t) {
                    project = null
                    log('could not create a new scratch project, so the run stops: ' + shortError(t))
                    row(entry.id, 'cleanup', '-1', -1, -1, -1, 'unavailable', left.size() + ' root namespaces '
                        + 'could not be deleted, and no new scratch project could be created: ' + shortError(t))
                    todo.drop(todo.indexOf(entry) + 1).each { Map e ->
                        row(e.id, 'all', '-1', -1, -1, -1, 'skipped', 'no scratch project: ' + shortError(t))
                    }
                    break
                }
                row(entry.id, 'cleanup', '-1', -1, -1, -1, 'ok', left.size() + ' root namespaces could not be '
                    + 'deleted: the next models run in a new scratch project')
                log(entry.id + ': the next models run in a new scratch project')
            }
            if (progress != null) {
                progress.increase()
            }
        }
    } finally {
        try {
            if (project != null) {
                api.close(project)
            }
        } catch (Throwable t) {
            log('closing the scratch project: ' + shortError(t))
        }
    }
}

try {
    api = headless ? binding.getVariable('confApi') : new CameoApi()
    if (headless) {
        runAll(null)
    } else {
        Closure task = { progress -> runAll(progress) }
        tool('com.nomagic.ui.ProgressStatusRunner').runWithProgressStatus(
            task.asType(tool('com.nomagic.task.RunnableWithProgress')), 'SysML v2 conformance run', true, 0)
    }
} catch (Throwable t) {
    log('run failed: ' + shortError(t))
    // So that the comparison sees the failure: no model ran (or the run stopped after some did)
    row('*', 'start', '-1', -1, -1, -1, rows.any { it[0] != '*' } ? 'unavailable' : 'failed', shortError(t))
} finally {
    env['load.average.start'] = loadAtStart
    env['load.average.end'] = loadAverage()
    env['conf.only'] = CONF_ONLY
    env['conf.skip'] = CONF_SKIP
    env['conf.undocumented'] = String.valueOf(CONF_UNDOCUMENTED)
    env['files.changed'] = changedFiles.join(',')
    env['kpar'] = api == null ? '' : api.respondsTo('importKpar') ? 'Cameo KPAR import'
        : api.respondsTo('multiFileImport') && api.multiFileImport()
        ? 'as files: the .sysml members imported together, as Cameo imports several files'
        : 'as joined text: the .sysml members joined in archive order'
    writeResults(outDir, rows, diagnostics, env, toolVersion, host, runId, manifest, manifestFile, runLog)
}
String done = 'Wrote ' + outDir + ' (' + rows.findAll { it[0] != '*' }.collect { it[0] }.unique().size() +
    ' models). Commit and push it.'
log(done)
Files.writeString(outDir.resolve('run-log.txt'), runLog.toString(), StandardCharsets.UTF_8)
if (!headless) {
    application().getGUILog().showMessage(done)
}
return done

// ---------------------------------------------------------------------------------------------------------------

/** Load one model: import, count, validate, dispose, then build; record rows and diagnostics. */
void loadModel(def api, def project, Map entry, Path file, boolean undocumented, boolean changed, Closure row,
               List<List> diagnostics, Closure log) {
    String id = entry.id
    String note = changed ? 'the file differs from the manifest (sha256)' : ''
    Map model
    try {
        model = readModel(file)
    } catch (Throwable e) {
        model = [invalid: 'cannot read the file: ' + shortError(e)]
    }
    // Cameo's own KPAR import, once it is released: give CameoApi an importKpar(project, Path, label)
    boolean nativeKpar = model.kpar && api.respondsTo('importKpar')
    if (model.invalid && nativeKpar) {
        // Cameo decides about the archive; its text cannot be built here
        model = [kpar: true, members: [], kerml: 0, unreadable: model.invalid]
    } else if (model.invalid) {
        // This macro's check, not Cameo's verdict
        row(id, 'all', '-1', -1, -1, -1, 'skipped', join(model.invalid + ' (checked by this macro; Cameo\'s KPAR '
            + 'import is not used)', note))
        log(id + ': skipped, ' + model.invalid)
        return
    }
    List members = model.members
    if (model.kpar && members.isEmpty() && !nativeKpar) {
        String why = 'no .sysml member: Cameo\'s textual import reads .sysml files only'
        row(id, 'all', '-1', -1, -1, -1, 'skipped', join(why, note))
        log(id + ': skipped, ' + why)
        return
    }
    boolean multiFile = model.kpar && !nativeKpar && api.respondsTo('multiFileImport') && api.multiFileImport()
    if (multiFile) {
        try {
            model.files = extract(members)
        } catch (Throwable e) {
            row(id, 'all', '-1', -1, -1, -1, 'skipped', join('invalid KPAR: ' + shortError(e)
                + ' (checked by this macro)', note))
            log(id + ': skipped, ' + shortError(e))
            return
        }
    }
    String how = !model.kpar ? '' : (nativeKpar ? 'KPAR import' : multiFile ? 'KPAR as files' : 'KPAR as joined text') +
        ', ' + members.size() + ' members' + (model.kerml ? ', ' + model.kerml + ' .kerml left out' : '')
    try {
        importAndCheck(api, project, id, file, model, nativeKpar, join(how, note), undocumented, row, diagnostics,
            log)
    } finally {
        try {
            deleteTree(model.files?.dir)
        } catch (Throwable e) {
            log(id + ' deleting the extracted members: ' + shortError(e))
        }
    }
}

void importAndCheck(def api, def project, String id, Path file, Map model, boolean nativeKpar, String how,
                    boolean undocumented, Closure row, List<List> diagnostics, Closure log) {
    List before
    try {
        before = api.roots(project)
    } catch (Throwable e) {
        row(id, 'import', '-1', -1, -1, -1, 'failed', 'reading the root namespaces: ' + shortError(e))
        log(id + ' roots: ' + shortError(e))
        return
    }
    Closure added = { api.roots(project).findAll { r -> !before.any { it.is(r) } } }

    long t = System.nanoTime()
    try {
        if (nativeKpar) {
            api.importKpar(project, file, 'Import ' + id)
        } else if (model.files) {
            api.importFiles(project, model.files.files, 'Import ' + id)
        } else {
            api.importText(project, model.text, 'Import ' + id)
        }
        row(id, 'import', ms(t), -1, 0, 0, 'ok', how)
    } catch (Throwable e) {
        row(id, 'import', '-1', -1, 1, 0, 'failed', join(shortError(e), how))
        diagnostics.add([id, '', 'import', 'error', 0, 0, '', shortError(e)])
        log(id + ' import: ' + shortError(e))
        removeQuietly(api, project, added, id, log)
        build(api, project, id, model, undocumented, row, diagnostics, log)
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
        row(id, 'count', '-1', -1, -1, -1, 'failed', 'the import added no root namespace')
        log(id + ': the import added no root namespace')
    } else {
        t = System.nanoTime()
        try {
            long elements = api.count(imported)
            row(id, 'count', ms(t), elements, -1, -1, 'ok', imported.size() + ' roots')
        } catch (Throwable e) {
            row(id, 'count', '-1', -1, -1, -1, 'failed', shortError(e))
            log(id + ' count: ' + shortError(e))
        }
        if (undocumented) {
            t = System.nanoTime()
            try {
                List checked = api.validate(project, imported)
                String took = ms(t)
                if ((checked[3] as int) == 0) {
                    row(id, 'validate', '-1', -1, -1, -1, 'unavailable', 'no validation suites')
                } else {
                    row(id, 'validate', took, -1, checked[0] as long, checked[1] as long, 'ok', checked[3] + ' suites')
                    checked[2].each { d -> diagnostics.add([id, '', d[0], severity(d[1]), d[2], d[3], d[4], d[5]]) }
                }
            } catch (Throwable e) {
                row(id, 'validate', '-1', -1, -1, -1, 'unavailable', shortError(e))
                log(id + ' validate: ' + shortError(e))
            }
        }
        t = System.nanoTime()
        try {
            api.dispose(project, imported, 'Remove ' + id)
            row(id, 'dispose', ms(t), -1, -1, -1, 'ok', '')
        } catch (Throwable e) {
            row(id, 'dispose', '-1', -1, -1, -1, 'failed', shortError(e))
            log(id + ' dispose: ' + shortError(e))
        }
        clearUndo(api, project, id, log)
    }
    build(api, project, id, model, undocumented, row, diagnostics, log)
}

static String join(String a, String b) {
    a && b ? a + '; ' + b : a ?: b ?: ''
}

/**
 * The root namespaces a model left in the scratch project, after trying once more to delete them: empty when the
 * project is as it was created.
 */
List leftover(def api, def project, List baseline, String id, Closure log) {
    try {
        List left = api.roots(project).findAll { r -> !baseline.any { it.is(r) } }
        if (left.isEmpty()) {
            return []
        }
        log(id + ': ' + left.size() + ' root namespaces left in the scratch project; deleting them')
        api.dispose(project, left, 'Clean up after ' + id)
        clearUndo(api, project, id, log)
        return api.roots(project).findAll { r -> !baseline.any { it.is(r) } }
    } catch (Throwable e) {
        log(id + ' cleaning up the scratch project: ' + shortError(e))
        return ['unknown']
    }
}

/** The transient build, on the project as it was before the import: the text diagnostics with their lines. */
void build(def api, def project, String id, Map model, boolean undocumented, Closure row, List<List> diagnostics,
           Closure log) {
    if (!undocumented) {
        row(id, 'build', '-1', -1, -1, -1, 'unavailable', 'CONF_UNDOCUMENTED=false: the text was not checked')
        return
    }
    if (model.unreadable) {
        row(id, 'build', '-1', -1, -1, -1, 'unavailable', 'the members could not be read: ' + model.unreadable)
        return
    }
    long t = System.nanoTime()
    try {
        if (model.files) {
            List built = api.buildFiles(project, model.files.files)
            row(id, 'build', ms(t), -1, built[0] as long, built[1] as long, 'ok', 'as files')
            built[2].each { d ->
                diagnostics.add([id, memberOf(model.files, d[6] as String), d[0], severity(d[1]), d[2], d[3], d[4],
                                 d[5]])
            }
        } else {
            List built = api.build(project, model.text)
            row(id, 'build', ms(t), -1, built[0] as long, built[1] as long, 'ok', model.kpar ? 'as joined text' : '')
            built[2].each { d ->
                List where = place(model.members, d[2] as int)
                diagnostics.add([id, where[0], d[0], severity(d[1]), where[1], d[3], d[4], d[5]])
            }
        }
    } catch (Throwable e) {
        row(id, 'build', '-1', -1, -1, -1, 'unavailable', shortError(e))
        log(id + ' build: ' + shortError(e))
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
        chooser.setDialogTitle('Select the sysmlv2-validator checkout (the folder that holds conformance)')
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
                  String runId, List<Map> manifest, Path manifestFile, StringBuilder runLog) {
    StringBuilder results = new StringBuilder(
        'tool,tool_version,host,run_id,file_id,rep,phase,ms,elements,errors,warnings,status,detail\n')
    for (List r : rows) {
        results.append(csv(['cameo', toolVersion, host, runId] + r)).append('\n')
    }
    Files.writeString(outDir.resolve('results.csv'), results.toString(), StandardCharsets.UTF_8)

    StringBuilder diag = new StringBuilder('tool,file_id,member,source,severity,line,column,code,message\n')
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
    all['manifest.sha256'] = sha256(Files.readAllBytes(manifestFile))
    StringBuilder props = new StringBuilder()
    all.each { k, v ->
        props.append(k).append('=').append(String.valueOf(v).replace('\\', '\\\\').replace('\n', ' ')).append('\n')
    }
    Files.writeString(outDir.resolve('environment.properties'), props.toString(), StandardCharsets.UTF_8)

    StringBuilder report = new StringBuilder('# Cameo ' + toolVersion + ' on ' + host + '\n\n')
    report.append('What Cameo reports for each model. Compare it with this validator and the pilot with ')
    report.append('`CompareMain conformance compare`.\n\n')
    report.append('| Model | Verdict | Import (ms) | Elements | Text errors / warnings | ')
    report.append('Validation errors / warnings |\n')
    report.append('|---|---|---:|---:|---:|---:|\n')
    for (Map e : manifest) {
        List mine = rows.findAll { it[0] == e.id }
        if (mine.isEmpty()) {
            continue
        }
        Closure phase = { String name -> mine.find { it[2] == name } }
        Closure counts = { String name ->
            List r = phase(name)
            r ? (r[7] == 'ok' ? r[5] + ' / ' + r[6] : r[7]) : '–'
        }
        boolean failed = mine.any { it[7] == 'failed' && it[2] in ['import', 'count'] }
        long errors = mine.findAll { it[7] == 'ok' && it[2] in ['build', 'validate'] }.sum(0L) { it[5] as long }
        // The import does not report errors in the text: without the build step there is no verdict
        boolean checked = mine.any { it[2] == 'build' && it[7] == 'ok' }
        String verdict = mine.any { it[7] == 'skipped' } ? 'skipped' : failed ? 'failed' : errors > 0 ? 'rejected'
            : checked ? 'accepted' : 'unchecked'
        List imported = phase('import')
        List counted = phase('count')
        report.append("| ${e.id} | ${verdict} | ${imported && imported[7] == 'ok' ? imported[3] : '–'} | ")
        report.append("${counted && counted[7] == 'ok' ? counted[4] : ''} | ${counts('build')} | ")
        report.append("${counts('validate')} |\n")
    }
    Files.writeString(outDir.resolve('report.md'), report.toString(), StandardCharsets.UTF_8)
    Files.writeString(outDir.resolve('run-log.txt'), runLog.toString(), StandardCharsets.UTF_8)
}

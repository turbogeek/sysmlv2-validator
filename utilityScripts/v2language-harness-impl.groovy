// v2language-harness-impl.groovy - what the SysML v2 test harness answers. The bootstrap
// (start-v2language-test-harness.groovy) loads this file in a class loader of its own and can replace it while the
// server keeps running (POST /reload), so this file is edited and picked up without restarting MagicDraw.
//
//   GET  /status      version, port, uptime, the open project, the last runs and the last error
//   GET  /scripts     the Groovy scripts the harness can run, with their size and time of change
//   POST /run-script   {"scriptName": "x.groovy" | "scriptText": "...", "args": {...}, "keepClasses": false}
//                      runs it in a class loader of its own, which is closed afterwards unless keepClasses is true,
//                      so the next run never sees the classes of this one. Returns the script's value as "result",
//                      everything it printed as "output", and writes both to a log file under logs/.
//   POST /load-sysml   {"filePath": "..." | "sysmlText": "...", "persist": true}
//                      parses, links and validates; with persist (the default) the model is copied into the open
//                      project, without it nothing is changed and only the diagnostics come back.
//   POST /reset        {"windows": true, "sessions": true, "classes": true}
//                      closes the windows that scripts registered, cancels a session a script left open and drops
//                      the class loaders of previous runs. This is what to call between tests instead of stopping
//                      the harness.
//
// A script that opens a window registers it with harness.registerWindow(frame) so that /reset can close it.

import com.sun.net.httpserver.HttpExchange
import com.nomagic.magicdraw.core.Application
import java.util.stream.Collectors
import java.util.concurrent.locks.ReentrantLock
import com.dassault_systemes.modeler.sysml.textual.project.ISysMLTransientModelBuilder
import com.dassault_systemes.modeler.sysml.textual.project.SysMLTransientModelBuilder
import com.dassault_systemes.modeler.sysml.textual.project.SysMLTextualProjectModelBasedHelper
import com.dassault_systemes.modeler.kerml.model.kerml.Element
import com.dassault_systemes.modeler.kerml.model.kerml.Namespace
import com.dassault_systemes.modeler.kerml.esi.feature.KerMLProjectFeature
import com.nomagic.magicdraw.openapi.uml.SessionManager

class HarnessImpl {

    static final String VERSION = "2.0"

    Map context = [:]
    File scriptsDir
    File logsDir
    final ReentrantLock runLock = new ReentrantLock()
    final List openLoaders = Collections.synchronizedList(new ArrayList())
    int runCounter = 0
    Map lastError = null

    /** The bootstrap calls this once per load; context survives reloads, so windows and runs are not lost. */
    void init(Map ctx) {
        context = ctx
        scriptsDir = (File) ctx.get("scriptsDir")
        logsDir = new File(scriptsDir, "logs")
        try {
            logsDir.mkdirs()
        } catch (Throwable ignored) {
        }
    }

    void log(String message, Throwable t = null) {
        Closure logger = (Closure) context.get("log")
        if (logger != null) {
            logger.call(message, t)
        } else {
            println(message)
        }
    }

    /** Scripts call this so that /reset can close what they opened. */
    void registerWindow(Object window) {
        ((List) context.get("windows")).add(window)
    }

    void handle(String path, HttpExchange exchange) {
        if ("/status".equals(path)) {
            status(exchange)
        } else if ("/scripts".equals(path)) {
            scripts(exchange)
        } else if ("/run-script".equals(path)) {
            runScript(exchange)
        } else if ("/load-sysml".equals(path)) {
            loadSysml(exchange)
        } else if ("/reset".equals(path)) {
            reset(exchange)
        } else {
            respond(exchange, 404, [success: false, error: "no endpoint " + path,
                                    endpoints: ["/status", "/scripts", "/run-script", "/load-sysml", "/reset",
                                                "/reload", "/ping", "/shutdown"]])
        }
    }

    // --- endpoints ----------------------------------------------------------------------------------------------

    void status(HttpExchange exchange) {
        Date startedAt = (Date) context.get("startedAt")
        def project = null
        try {
            project = Application.getInstance().getProject()
        } catch (Throwable ignored) {
        }
        Closure loadedAt = (Closure) context.get("implLoadedAt")
        List runs = (List) context.get("runs")
        respond(exchange, 200, [
            success        : true,
            status         : "running",
            version        : VERSION,
            port           : context.get("port"),
            startedAt      : String.valueOf(startedAt),
            uptimeSeconds  : startedAt == null ? 0 : (long) ((System.currentTimeMillis() - startedAt.getTime()) / 1000),
            implementation : String.valueOf(loadedAt == null ? null : loadedAt.call()),
            scriptsDir     : scriptsDir.getAbsolutePath(),
            implFile       : new File(scriptsDir, "v2language-harness-impl.groovy").getAbsolutePath(),
            implFileChanged: new Date(new File(scriptsDir, "v2language-harness-impl.groovy").lastModified()).toString(),
            project        : project == null ? null : String.valueOf(project.getName()),
            openWindows    : ((List) context.get("windows")).size(),
            openLoaders    : openLoaders.size(),
            runs           : runs.size() <= 5 ? runs : runs.subList(runs.size() - 5, runs.size()),
            lastError      : lastError
        ])
    }

    void scripts(HttpExchange exchange) {
        List found = []
        File[] files = scriptsDir.listFiles()
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".groovy")) {
                    found.add([name: f.getName(), bytes: f.length(), modified: new Date(f.lastModified()).toString()])
                }
            }
        }
        found.sort { it.get("name") }
        respond(exchange, 200, [success: true, scriptsDir: scriptsDir.getAbsolutePath(), count: found.size(), scripts: found])
    }

    void runScript(HttpExchange exchange) {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, [success: false, error: "POST only"])
            return
        }
        Map request = Json.decode(new String(exchange.getRequestBody().readAllBytes(), "UTF-8"))
        String scriptName = (String) request.get("scriptName")
        String scriptText = (String) request.get("scriptText")
        if (scriptName == null && scriptText == null) {
            respond(exchange, 400, [success: false, error: "give scriptName or scriptText"])
            return
        }
        File scriptFile = null
        if (scriptName != null) {
            scriptFile = new File(scriptsDir, scriptName)
            if (!scriptFile.exists()) {
                respond(exchange, 404, [success: false, error: "no script " + scriptName + " in " + scriptsDir.getAbsolutePath()])
                return
            }
        }
        boolean keepClasses = Boolean.TRUE.equals(request.get("keepClasses"))
        Map args = (request.get("args") instanceof Map) ? (Map) request.get("args") : [:]

        runLock.lock()
        String runId = null
        long startedAt = System.currentTimeMillis()
        StringBuilder output = new StringBuilder()
        GroovyClassLoader loader = null
        try {
            runCounter++
            runId = String.format("%03d", runCounter) + "-" + (scriptName == null ? "inline" : scriptName.replace(".groovy", ""))
            log("Run " + runId + (scriptName == null ? " (inline script)" : "") + " starting")
            loader = new GroovyClassLoader(HarnessImpl.class.getClassLoader())
            openLoaders.add(loader)
            Class scriptClass = scriptFile == null ? loader.parseClass(scriptText, "inline-" + runId + ".groovy")
                                                   : loader.parseClass(scriptFile)
            Script script = (Script) scriptClass.getDeclaredConstructor().newInstance()
            Binding binding = new Binding()
            binding.setVariable("harness", this)
            binding.setVariable("args", args)
            binding.setVariable("runId", runId)
            script.setBinding(binding)

            Object result = null
            Throwable failure = null
            PrintStream oldOut = System.out
            PrintStream oldErr = System.err
            ByteArrayOutputStream captured = new ByteArrayOutputStream()
            PrintStream capture = new PrintStream(captured, true, "UTF-8")
            try {
                System.setOut(capture)
                System.setErr(capture)
                result = script.run()
            } catch (Throwable t) {
                failure = t
            } finally {
                System.setOut(oldOut)
                System.setErr(oldErr)
                capture.flush()
                output.append(captured.toString("UTF-8"))
            }

            long durationMs = System.currentTimeMillis() - startedAt
            File runLog = writeRunLog(runId, scriptName, args, output.toString(), result, failure, durationMs)
            Map summary = [runId: runId, script: scriptName == null ? "inline" : scriptName, durationMs: durationMs,
                           success: failure == null, at: new Date().toString()]
            ((List) context.get("runs")).add(summary)
            if (failure == null) {
                log("Run " + runId + " finished in " + durationMs + " ms")
                respond(exchange, 200, [success: true, result: result == null ? null : result.toString(),
                                        output: output.toString(), runId: runId, durationMs: durationMs,
                                        logFile: runLog == null ? null : runLog.getAbsolutePath()])
            } else {
                lastError = [runId: runId, error: failure.toString(), at: new Date().toString()]
                log("Run " + runId + " failed: " + failure, failure)
                respond(exchange, 500, [success: false, error: failure.getMessage() == null ? failure.toString() : failure.getMessage(),
                                        stackTrace: stackTrace(failure), output: output.toString(), runId: runId,
                                        durationMs: durationMs, logFile: runLog == null ? null : runLog.getAbsolutePath()])
            }
        } catch (Throwable t) {
            lastError = [runId: runId, error: t.toString(), at: new Date().toString()]
            log("Run " + runId + " could not start: " + t, t)
            respond(exchange, 500, [success: false, error: t.toString(), stackTrace: stackTrace(t), runId: runId])
        } finally {
            if (loader != null && !keepClasses) {
                openLoaders.remove(loader)
                try {
                    loader.close()
                } catch (Throwable ignored) {
                }
            }
            runLock.unlock()
        }
    }

    void loadSysml(HttpExchange exchange) {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, [success: false, error: "POST only"])
            return
        }
        def project = Application.getInstance().getProject()
        if (project == null) {
            respond(exchange, 500, [success: false, error: "No active MagicDraw project!"])
            return
        }
        Map request = Json.decode(new String(exchange.getRequestBody().readAllBytes(), "UTF-8"))
        String sysmlText = (String) request.get("sysmlText")
        String filePath = (String) request.get("filePath")
        if (sysmlText == null && filePath != null) {
            File f = new File(filePath)
            if (f.exists()) {
                sysmlText = f.getText("UTF-8")
            }
        }
        if (sysmlText == null) {
            respond(exchange, 400, [success: false, error: "Missing sysmlText or valid filePath param"])
            return
        }
        boolean persist = !Boolean.FALSE.equals(request.get("persist"))
        long startedAt = System.currentTimeMillis()
        log("Loading SysML v2" + (filePath == null ? " (raw text)" : " from " + filePath) + (persist ? "" : " (parse only)"))

        SessionManager.getInstance().createSession(project, "SysMLv2TestHarness: REST Load SysML")
        try {
            ISysMLTransientModelBuilder modelBuilder = new SysMLTransientModelBuilder(project)
            List allRoots = com.dassault_systemes.modeler.kerml.model.RootNamespaces.getAllRoots(project)
                .stream().filter { it instanceof Namespace }.map { (Namespace) it }.collect(Collectors.toList())
            def buildResult = modelBuilder.build(sysmlText, allRoots)
            Element transientNs = buildResult.getTransientRootNs()
            def diags = buildResult.getDiagnostics() ?: []
            List diagnostics = []
            for (Object d : diags) {
                diagnostics.add([severity: String.valueOf(d.getSeverity()), line: d.getLine(), message: String.valueOf(d.getMessage())])
            }
            List errors = diagnostics.findAll { "ERROR".equals(it.get("severity")) }
            long durationMs = System.currentTimeMillis() - startedAt

            if (!errors.isEmpty()) {
                cancelSession(project)
                String errorList = errors.collect { "[" + it.get("severity") + "] line " + it.get("line") + ": " + it.get("message") }
                    .join('\\n').replace('"', "'").replace("\r", "")
                respond(exchange, 400, [success: false, error: "Semantic errors found:\\n" + errorList,
                                        diagnostics: diagnostics, durationMs: durationMs, persisted: false])
                return
            }
            if (transientNs == null) {
                cancelSession(project)
                respond(exchange, 400, [success: false, error: "Failed to parse SysML (returned null root)",
                                        diagnostics: diagnostics, durationMs: durationMs, persisted: false])
                return
            }
            if (!persist) {
                cancelSession(project)
                respond(exchange, 200, [success: true, message: "SysML parsed and validated, nothing changed",
                                        diagnostics: diagnostics, durationMs: durationMs, persisted: false])
                return
            }
            Element persistentNs = SysMLTextualProjectModelBasedHelper.copyTransientModelToPersistent(project, transientNs)
            KerMLProjectFeature projectFeature = KerMLProjectFeature.getPrimaryProjectFeature(project)
            projectFeature.addCommonData(persistentNs)
            SessionManager.getInstance().closeSession(project)
            respond(exchange, 200, [success: true, message: "SysML loaded successfully", diagnostics: diagnostics,
                                    durationMs: System.currentTimeMillis() - startedAt, persisted: true])
        } catch (Throwable t) {
            cancelSession(project)
            lastError = [endpoint: "/load-sysml", error: t.toString(), at: new Date().toString()]
            log("Load failed: " + t, t)
            respond(exchange, 400, [success: false, error: t.getMessage() == null ? t.toString() : t.getMessage(),
                                    stackTrace: stackTrace(t), persisted: false])
        } finally {
            cancelSession(project)
        }
    }

    void reset(HttpExchange exchange) {
        Map request = [:]
        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                request = Json.decode(new String(exchange.getRequestBody().readAllBytes(), "UTF-8"))
            }
        } catch (Throwable ignored) {
        }
        boolean doWindows = !Boolean.FALSE.equals(request.get("windows"))
        boolean doSessions = !Boolean.FALSE.equals(request.get("sessions"))
        boolean doClasses = !Boolean.FALSE.equals(request.get("classes"))

        int closedWindows = 0
        List windows = (List) context.get("windows")
        if (doWindows) {
            List copy = new ArrayList(windows)
            for (Object w : copy) {
                try {
                    if (w != null) {
                        w.setVisible(false)
                        w.dispose()
                        closedWindows++
                    }
                } catch (Throwable ignored) {
                }
            }
            windows.clear()
        }
        boolean cancelledSession = false
        if (doSessions) {
            try {
                def project = Application.getInstance().getProject()
                if (project != null && SessionManager.getInstance().isSessionCreated(project)) {
                    SessionManager.getInstance().cancelSession(project)
                    cancelledSession = true
                }
            } catch (Throwable ignored) {
            }
        }
        int closedLoaders = 0
        if (doClasses) {
            List copy = new ArrayList(openLoaders)
            for (Object l : copy) {
                try {
                    ((GroovyClassLoader) l).close()
                    closedLoaders++
                } catch (Throwable ignored) {
                }
            }
            openLoaders.clear()
            System.gc()
        }
        log("Reset: " + closedWindows + " window(s) closed, session cancelled " + cancelledSession + ", " +
            closedLoaders + " class loader(s) closed")
        respond(exchange, 200, [success: true, closedWindows: closedWindows, cancelledSession: cancelledSession,
                                closedLoaders: closedLoaders])
    }

    // --- helpers ------------------------------------------------------------------------------------------------

    void cancelSession(Object project) {
        try {
            if (project != null && SessionManager.getInstance().isSessionCreated(project)) {
                SessionManager.getInstance().cancelSession(project)
            }
        } catch (Throwable ignored) {
        }
    }

    File writeRunLog(String runId, String scriptName, Map args, String output, Object result, Throwable failure, long durationMs) {
        try {
            File file = new File(logsDir, "run-" + runId + ".log")
            StringBuilder sb = new StringBuilder()
            sb.append(new Date().toString()).append(" run ").append(runId).append("\n")
            sb.append("script: ").append(scriptName == null ? "(inline)" : scriptName).append("\n")
            sb.append("args: ").append(String.valueOf(args)).append("\n")
            sb.append("duration: ").append(durationMs).append(" ms\n")
            sb.append("--- output ---\n").append(output == null ? "" : output).append("\n")
            sb.append("--- result ---\n").append(result == null ? "(null)" : result.toString()).append("\n")
            if (failure != null) {
                sb.append("--- failure ---\n").append(stackTrace(failure)).append("\n")
            }
            file.setText(sb.toString(), "UTF-8")
            return file
        } catch (Throwable t) {
            log("Could not write the log of run " + runId + ": " + t)
            return null
        }
    }

    String stackTrace(Throwable t) {
        if (t == null) {
            return ""
        }
        StringWriter sw = new StringWriter()
        t.printStackTrace(new PrintWriter(sw))
        return sw.toString()
    }

    void respond(HttpExchange exchange, int status, Object body) {
        byte[] bytes = Json.encode(body).getBytes("UTF-8")
        exchange.getResponseHeaders().set("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.length)
        exchange.getResponseBody().write(bytes)
        exchange.getResponseBody().close()
    }
}

/** JSON without groovy.json, which MagicDraw's GroovyShell cannot load (it crashes on the FastStringService SPI). */
class Json {

    static String encode(Object o) {
        if (o == null) {
            return "null"
        }
        if (o instanceof String || o instanceof GString) {
            return '"' + o.toString().replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')
                .replace('\r', '').replace('\t', '\\t') + '"'
        }
        if (o instanceof Number || o instanceof Boolean) {
            return o.toString()
        }
        if (o instanceof Map) {
            List parts = []
            ((Map) o).each { k, v -> parts.add(encode(String.valueOf(k)) + ":" + encode(v)) }
            return "{" + parts.join(",") + "}"
        }
        if (o instanceof Collection || o.getClass().isArray()) {
            List parts = []
            o.each { parts.add(encode(it)) }
            return "[" + parts.join(",") + "]"
        }
        return encode(o.toString())
    }

    static Map decode(String json) {
        if (json == null || json.trim().isEmpty()) {
            return [:]
        }
        Parser parser = new Parser(json)
        Object value = parser.value()
        return (value instanceof Map) ? (Map) value : [value: value]
    }

    /** A small recursive parser, so that nested objects and arrays (script arguments) survive. */
    static class Parser {
        String text
        int at = 0

        Parser(String text) {
            this.text = text
        }

        Object value() {
            skip()
            char c = text.charAt(at)
            if (c == '{' as char) {
                return object()
            }
            if (c == '[' as char) {
                return array()
            }
            if (c == '"' as char) {
                return string()
            }
            return literal()
        }

        Map object() {
            Map map = [:]
            at++
            skip()
            if (text.charAt(at) == '}' as char) {
                at++
                return map
            }
            while (true) {
                skip()
                String key = string()
                skip()
                at++   // ':'
                map.put(key, value())
                skip()
                char c = text.charAt(at++)
                if (c == '}' as char) {
                    return map
                }
            }
        }

        List array() {
            List list = []
            at++
            skip()
            if (text.charAt(at) == ']' as char) {
                at++
                return list
            }
            while (true) {
                list.add(value())
                skip()
                char c = text.charAt(at++)
                if (c == ']' as char) {
                    return list
                }
            }
        }

        String string() {
            StringBuilder sb = new StringBuilder()
            at++
            while (true) {
                char c = text.charAt(at++)
                if (c == '"' as char) {
                    return sb.toString()
                }
                if (c == '\\' as char) {
                    char e = text.charAt(at++)
                    if (e == 'n' as char) {
                        sb.append('\n')
                    } else if (e == 't' as char) {
                        sb.append('\t')
                    } else if (e == 'r' as char) {
                        sb.append('\r')
                    } else if (e == 'u' as char) {
                        sb.append((char) Integer.parseInt(text.substring(at, at + 4), 16))
                        at += 4
                    } else {
                        sb.append(e)
                    }
                } else {
                    sb.append(c)
                }
            }
        }

        Object literal() {
            int start = at
            while (at < text.length() && !",}] \t\n\r".contains(String.valueOf(text.charAt(at)))) {
                at++
            }
            String raw = text.substring(start, at)
            if ("true".equals(raw)) {
                return Boolean.TRUE
            }
            if ("false".equals(raw)) {
                return Boolean.FALSE
            }
            if ("null".equals(raw)) {
                return null
            }
            if (raw.contains(".")) {
                return Double.parseDouble(raw)
            }
            return Long.parseLong(raw)
        }

        void skip() {
            while (at < text.length() && " \t\n\r".contains(String.valueOf(text.charAt(at)))) {
                at++
            }
        }
    }
}

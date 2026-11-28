// start-v2language-test-harness.groovy - the SysML v2 test harness of CATIA Magic / Cameo, launched once from
// MagicDraw (Tools > Macros or the Groovy console). It serves REST on port 8771 by default (8770 is taken by
// macOS sharingd/AirDrop on some Macs); if that port is busy it takes the next free one up to 8780, which GET /ping
// and the log report. Set -Dsysmlv2.harness.port to choose another starting port.
//
// This script is only the bootstrap: it owns the port and the HTTP server and keeps running. Everything a caller
// asks for is handled by v2language-harness-impl.groovy, which the bootstrap loads in a class loader of its own and
// can replace while the server keeps running (POST /reload). So the harness is changed without restarting it by
// hand, and a script that a caller runs gets a fresh class loader that is closed afterwards, which is what used to
// force a restart between tests.
//
//   GET  /ping      is the harness there, and which version
//   POST /reload    re-read v2language-harness-impl.groovy (the answer says whether it worked; on failure the
//                   previous implementation keeps serving)
//   GET  /shutdown  stop the server (rarely needed; /reset is usually what is wanted)
//   everything else is handled by the implementation: /status, /scripts, /run-script, /load-sysml, /reset
//
// Launching it again in the same MagicDraw stops the previous instance first, so relaunching is safe.

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpExchange
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.nomagic.magicdraw.core.Application

class HarnessBootstrap {

    static final String VERSION = "2.0"
    static final int DEFAULT_PORT = 8771
    static final int LAST_PORT = 8780          // stop-v2language-test-harness.groovy scans 8770-8780
    static final String IMPL_FILE = "v2language-harness-impl.groovy"
    static final String REGISTRY_KEY = "sysmlv2.harness.instance"   // survives class loaders: see stopPrevious

    static File harnessDir
    static File logFile
    static HttpServer server
    static Map current = [:]          // impl, loader, loadedAt, error
    static Map context = [:]          // what the implementation is given; survives reloads

    static void log(String msg, Throwable t = null) {
        String line = new Date().toString() + " - " + msg
        try {
            Application.getInstance().getGUILog().log(msg)
        } catch (Throwable ignored) {
        }
        println(line)
        try {
            if (logFile != null) {
                logFile.append(line + "\n")
                if (t != null) {
                    logFile.append(stackTrace(t) + "\n")
                }
            }
        } catch (Throwable ignored) {
        }
    }

    static String stackTrace(Throwable t) {
        if (t == null) {
            return ""
        }
        StringWriter sw = new StringWriter()
        t.printStackTrace(new PrintWriter(sw))
        return sw.toString()
    }

    /** The directory with the harness implementation and the scripts that callers run. */
    static File findHarnessDir() {
        List candidates = []
        String property = System.getProperty("sysmlv2.harness.dir")
        if (property != null && !property.trim().isEmpty()) {
            candidates.add(property)
        }
        String home = System.getProperty("user.home")
        for (String rel : ["Documents/GitHub/sysmlv2-validator", "Desktop/git/sysmlv2-validator", "git/sysmlv2-validator",
                           "Documents/git/sysmlv2-validator", "Desktop/GitHub/sysmlv2-validator", "GitHub/sysmlv2-validator"]) {
            candidates.add(new File(home, rel + "/utilityScripts").getAbsolutePath())
        }
        candidates.add("E:/_Documents/git/sysml-validator/utilityScripts")
        for (Object c : candidates) {
            File dir = new File(c.toString())
            if (new File(dir, IMPL_FILE).exists()) {
                return dir
            }
        }
        for (Object c : candidates) {
            File dir = new File(c.toString())
            if (dir.isDirectory()) {
                return dir
            }
        }
        return new File(System.getProperty("user.home"))
    }

    /**
     * Stops a harness that is already running, so that relaunching does not leave two servers or move the port.
     * A previous bootstrap of this version is found through the system properties, which every class loader shares;
     * an older harness is asked over HTTP instead.
     */
    static void stopPrevious(int port) {
        Object previous = System.getProperties().get(REGISTRY_KEY)
        if (previous instanceof Map && previous.get("server") != null) {
            try {
                ((HttpServer) previous.get("server")).stop(0)
                log("Stopped the previous harness on port " + previous.get("port"))
            } catch (Throwable t) {
                log("Could not stop the previous harness: " + t, t)
            }
            System.getProperties().remove(REGISTRY_KEY)
            return
        }
        // An older harness may sit anywhere in the port sequence (the wanted port, the next ones up to LAST_PORT, or
        // 8770 where earlier versions listened). Only a port whose /ping answers like a harness is asked to shut down.
        List ports = []
        for (int p = port; p <= Math.max(port, LAST_PORT); p++) {
            ports.add(p)
        }
        if (!ports.contains(8770)) {
            ports.add(8770)
        }
        boolean stoppedAny = false
        for (Object p : ports) {
            try {
                URLConnection ping = new URL("http://localhost:" + p + "/ping").openConnection()
                ping.setConnectTimeout(300)
                ping.setReadTimeout(1000)
                String answer = ping.getInputStream().getText("UTF-8")
                if (!answer.contains('"version"')) {
                    continue
                }
                URLConnection connection = new URL("http://localhost:" + p + "/shutdown").openConnection()
                connection.setConnectTimeout(700)
                connection.setReadTimeout(2000)
                connection.getInputStream().getText("UTF-8")
                log("Asked the harness already on port " + p + " to shut down")
                stoppedAny = true
            } catch (Throwable ignored) {
                // nothing (or something other than a harness) was listening, which is the normal case
            }
        }
        if (stoppedAny) {
            Thread.sleep(1500)
        }
    }

    /** Loads the implementation in a class loader of its own; the caller decides what to do with the result. */
    static Map loadImpl() {
        File implFile = new File(harnessDir, IMPL_FILE)
        if (!implFile.exists()) {
            return [ok: false, error: "missing " + implFile.getAbsolutePath()]
        }
        GroovyClassLoader loader = new GroovyClassLoader(HarnessBootstrap.class.getClassLoader())
        try {
            Class implClass = loader.parseClass(implFile)
            Object impl = implClass.getDeclaredConstructor().newInstance()
            impl.init(context)
            return [ok: true, impl: impl, loader: loader, loadedAt: new Date(), file: implFile]
        } catch (Throwable t) {
            try {
                loader.close()
            } catch (Throwable ignored) {
            }
            return [ok: false, error: t.toString(), stack: stackTrace(t)]
        }
    }

    /** Replaces the running implementation. The previous one keeps serving when the new one does not load. */
    static String reload() {
        Map loaded = loadImpl()
        if (!loaded.get("ok")) {
            log("Reload failed, keeping the implementation loaded at " + current.get("loadedAt") + ": " + loaded.get("error"))
            return '{"success": false, "error": ' + quote(String.valueOf(loaded.get("error"))) +
                ', "stack": ' + quote(String.valueOf(loaded.get("stack"))) + '}'
        }
        Object oldLoader = current.get("loader")
        current = loaded
        if (oldLoader != null) {
            try {
                ((GroovyClassLoader) oldLoader).close()
            } catch (Throwable ignored) {
            }
        }
        log("Implementation reloaded from " + loaded.get("file"))
        return '{"success": true, "loadedAt": ' + quote(String.valueOf(loaded.get("loadedAt"))) +
            ', "file": ' + quote(String.valueOf(loaded.get("file"))) + '}'
    }

    static String quote(String s) {
        if (s == null) {
            return "null"
        }
        return '"' + s.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '').replace('\t', '\\t') + '"'
    }

    static void respond(HttpExchange exchange, int status, String body) {
        byte[] bytes = body.getBytes("UTF-8")
        exchange.getResponseHeaders().set("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.length)
        exchange.getResponseBody().write(bytes)
        exchange.getResponseBody().close()
    }

    static void start(int wantedPort) {
        harnessDir = findHarnessDir()
        logFile = new File(harnessDir, "SysMLv2TestHarness.log")
        log("=== SysML v2 test harness " + VERSION + " starting, directory " + harnessDir.getAbsolutePath() + " ===")
        stopPrevious(wantedPort)

        // The wanted port gets a few retries (a previous harness may still be letting go of it); after that each
        // following port up to LAST_PORT is tried once.
        server = null
        int chosen = wantedPort
        BindException lastBindFailure = null
        int lastPort = Math.max(wantedPort, LAST_PORT)
        for (int candidate = wantedPort; candidate <= lastPort && server == null; candidate++) {
            int attempts = (candidate == wantedPort) ? 12 : 1
            for (int attempt = 0; attempt < attempts; attempt++) {
                try {
                    server = HttpServer.create(new InetSocketAddress(candidate), 0)
                    chosen = candidate
                    lastBindFailure = null
                    break
                } catch (BindException e) {
                    lastBindFailure = e
                    if (attempt + 1 < attempts) {
                        Thread.sleep(500)
                    }
                }
            }
        }
        if (server == null) {
            log("Ports " + wantedPort + "-" + lastPort + " are all in use; stop the process holding them, or set -Dsysmlv2.harness.port")
            throw lastBindFailure
        }
        int port = chosen    // assigned once: the request handler below captures it
        if (port != wantedPort) {
            log("Port " + wantedPort + " is in use, so the harness takes port " + port)
        }
        server.setExecutor(Executors.newFixedThreadPool(4))

        context = [
            version       : VERSION,
            port          : port,
            harnessDir    : harnessDir,
            scriptsDir    : harnessDir,
            startedAt     : new Date(),
            logFile       : logFile,
            windows       : Collections.synchronizedList(new ArrayList()),
            runs          : Collections.synchronizedList(new ArrayList()),
            log           : { String m, Throwable t = null -> log(m, t) },
            implLoadedAt  : { -> current.get("loadedAt") },
            reload        : { -> reload() }
        ]
        current = loadImpl()
        if (!current.get("ok")) {
            log("The implementation did not load: " + current.get("error") + "\n" + current.get("stack"))
        }

        server.createContext("/", new HttpHandler() {
            void handle(HttpExchange exchange) {
                String path = exchange.getRequestURI().getPath()
                try {
                    if ("/ping".equals(path)) {
                        respond(exchange, 200, '{"success": true, "version": ' + quote(VERSION) + ', "port": ' + port +
                            ', "implementation": ' + (current.get("ok") ? "true" : "false") + '}')
                        return
                    }
                    if ("/reload".equals(path)) {
                        String body = reload()
                        respond(exchange, body.startsWith('{"success": true') ? 200 : 500, body)
                        return
                    }
                    if ("/shutdown".equals(path)) {
                        respond(exchange, 200, '{"success": true, "message": "shutting down"}')
                        new Timer().schedule(new TimerTask() {
                            void run() {
                                try {
                                    server.stop(0)
                                } catch (Throwable ignored) {
                                }
                                System.getProperties().remove(REGISTRY_KEY)
                                log("Server stopped.")
                            }
                        }, 800)
                        return
                    }
                    Object impl = current.get("impl")
                    if (impl == null) {
                        respond(exchange, 503, '{"success": false, "error": "no implementation loaded: ' +
                            String.valueOf(current.get("error")).replace('"', "'") + '", "hint": "fix ' + IMPL_FILE +
                            ' and POST /reload"}')
                        return
                    }
                    impl.handle(path, exchange)
                } catch (Throwable t) {
                    log("Request " + path + " failed: " + t, t)
                    try {
                        respond(exchange, 500, '{"success": false, "error": ' + quote(t.toString()) +
                            ', "stackTrace": ' + quote(stackTrace(t)) + '}')
                    } catch (Throwable ignored) {
                    }
                }
            }
        })

        server.start()
        System.getProperties().put(REGISTRY_KEY, [server: server, port: port, version: VERSION, startedAt: new Date()])
        log("Harness " + VERSION + " listening on port " + port + "; implementation " +
            (current.get("ok") ? ("loaded from " + current.get("file")) : "NOT loaded"))
    }
}

int port = Integer.parseInt(System.getProperty("sysmlv2.harness.port", String.valueOf(HarnessBootstrap.DEFAULT_PORT)))
HarnessBootstrap.start(port)

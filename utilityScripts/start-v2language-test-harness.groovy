import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpExchange
// Hand-rolled JSON parser to avoid FastStringService SPI crash in Cameo
class Jzon {
    static String encode(Object obj) {
        if (obj == null) return "null"
        if (obj instanceof String) return '"' + obj.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t') + '"'
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString()
        if (obj instanceof List || obj.getClass().isArray()) {
            return "[" + obj.collect { encode(it) }.join(",") + "]"
        }
        if (obj instanceof Map) {
            return "{" + obj.collect { k, v -> encode(k.toString()) + ":" + encode(v) }.join(",") + "}"
        }
        return '"' + obj.toString() + '"'
    }

    static Map decode(String json) {
        json = json.trim()
        if (!json.startsWith("{") || !json.endsWith("}")) throw new IllegalArgumentException("Expected JSON object")
        // Very basic JSON parser for flat objects (good enough for our simple requests)
        Map result = [:]
        String inner = json.substring(1, json.length() - 1).trim()
        if (!inner) return result
        
        // This regex correctly splits top-level key-value pairs separated by commas, 
        // ignoring commas inside string values
        def pairs = inner.split(/,(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)/)
        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':')
            if (colonIdx > 0) {
                String key = pair.substring(0, colonIdx).trim()
                String val = pair.substring(colonIdx + 1).trim()
                if (key.startsWith('"') && key.endsWith('"')) key = key.substring(1, key.length() - 1)
                if (val.startsWith('"') && val.endsWith('"')) val = val.substring(1, val.length() - 1)
                else if (val == "true") val = true
                else if (val == "false") val = false
                else if (val == "null") val = null
                else if (val.isNumber()) {
                    if (val.contains('.')) val = val.toDouble()
                    else val = val.toInteger()
                }
                result[key] = val
            }
        }
        return result
    }
}
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.GUILog
import java.io.File
import java.io.StringWriter
import java.io.PrintWriter
import java.net.BindException
import java.util.stream.Collectors
import com.dassault_systemes.modeler.sysml.textual.project.ISysMLTransientModelBuilder
import com.dassault_systemes.modeler.sysml.textual.project.SysMLTransientModelBuilder
import com.dassault_systemes.modeler.sysml.textual.project.SysMLTextualProjectModelBasedHelper
import com.dassault_systemes.modeler.kerml.model.kerml.Element
import com.dassault_systemes.modeler.kerml.model.kerml.Namespace
import com.dassault_systemes.modeler.kerml.esi.feature.KerMLProjectFeature
import com.nomagic.magicdraw.openapi.uml.SessionManager

/**
 * SysMLv2 Test Harness - Run this FROM WITHIN Cameo/MagicDraw.
 * 
 * Provides a REST API server running within the MagicDraw context
 * to allow external testers (and LLMs) to trigger Groovy scripts and interact with models.
 */
class SysMLv2TestHarness {
    
    static final int DEFAULT_PORT = 8770
    static final String SCRIPTS_DIR = "E:\\_Documents\\git\\SysMLv2ClientAPI\\scripts"
    
    static HttpServer server
    static boolean serverRunning = false

    static void logMsg(String msg, Throwable t = null) {
        Application.getInstance().getGUILog().log(msg)
        println(msg)
        try {
            File logFile = new File(SCRIPTS_DIR, "SysMLv2TestHarness.log")
            def dateStr = new java.util.Date().toString()
            logFile.append(dateStr + " - " + msg + "\n")
            if (t != null) {
                def sw = new StringWriter()
                t.printStackTrace(new PrintWriter(sw))
                logFile.append(sw.toString() + "\n")
                Application.getInstance().getGUILog().log(sw.toString())
            }
        } catch (Exception e) {
            // Silently ignore log file writing errors to prevent crash
        }
    }

    static void main(String[] args) {
        logMsg("=== SysMLv2 Test Harness Starting (MagicDraw Context) ===")
        try {
            startServer(DEFAULT_PORT)
        } catch (Exception e) {
            logMsg("Failed to start SysMLv2 harness: " + e.getMessage(), e)
        }
    }

    static void startServer(int port) {
        if (serverRunning) {
            logMsg("Existing server detected. Shutting down first...")
            server.stop(0)
            serverRunning = false
        }

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0)
            server.setExecutor(Executors.newFixedThreadPool(4))
            serverRunning = true
        } catch (BindException e) {
            logMsg("Port " + port + " is in use. Finding an alternative...")
            for (int altPort = port + 1; altPort <= port + 10; altPort++) {
                try {
                    server = HttpServer.create(new InetSocketAddress(altPort), 0)
                    server.setExecutor(Executors.newFixedThreadPool(4))
                    serverRunning = true
                    port = altPort
                    String altMsg = "✅ Using alternative port " + altPort
                    logMsg(altMsg)
                    break
                } catch (BindException altE) {
                    // ignore
                }
            }
            if (!serverRunning) {
                throw new Exception("Could not find available port near " + port)
            }
        }

        // --- REST ENDPOINTS ---

        server.createContext("/status", new HttpHandler() {
            void handle(HttpExchange exchange) {
                def response = "{ \"status\": \"running\", \"port\": " + port + " }"
                sendResponse(exchange, 200, response)
            }
        })

        server.createContext("/run-script", new HttpHandler() {
            void handle(HttpExchange exchange) {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method not allowed")
                    return
                }
                
                try {
                    String requestBody = new String(exchange.getRequestBody().readAllBytes())
                    def request = Jzon.decode(requestBody)
                    String scriptName = request.scriptName
                    
                    if (!scriptName) {
                        sendResponse(exchange, 400, "{\"success\": false, \"error\": \"Missing scriptName param\"}")
                        return
                    }
                    
                    File scriptFile = new File(SCRIPTS_DIR, scriptName)
                    if (!scriptFile.exists()) {
                        sendResponse(exchange, 404, "{\"success\": false, \"error\": \"Script not found: " + scriptName + "\"}")
                        return
                    }

                    logMsg("Executing script remotely via REST: " + scriptName)
                    GroovyShell shell = new GroovyShell()
                    Script script = shell.parse(scriptFile)
                    Object result = script.run()
                    
                    sendResponse(exchange, 200, Jzon.encode([success: true, result: result?.toString()]))
                    
                } catch (Exception e) {
                    logMsg("Error executing script via REST: " + e.getMessage(), e)
                    sendResponse(exchange, 500, Jzon.encode([
                        success: false, 
                        error: e.message, 
                        stackTrace: getStackTrace(e)
                    ]))
                }
            }
        })
        
        server.createContext("/load-sysml", new HttpHandler() {
            void handle(HttpExchange exchange) {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method not allowed")
                    return
                }
                
                def project = Application.getInstance().getProject()
                if (project == null) {
                    sendResponse(exchange, 500, Jzon.encode([success: false, error: "No active MagicDraw project!"]))
                    return
                }

                try {
                    String requestBody = new String(exchange.getRequestBody().readAllBytes())
                    def request = Jzon.decode(requestBody)
                    
                    String sysmlText = request.sysmlText
                    if (!sysmlText && request.filePath) {
                        File f = new File(request.filePath)
                        if (f.exists()) sysmlText = f.text
                    }
                    
                    if (!sysmlText) {
                        sendResponse(exchange, 400, Jzon.encode([success: false, error: "Missing sysmlText or valid filePath param"]))
                        return
                    }
                    
                    String fileNameInfo = request.filePath ? " from " + request.filePath : " (raw text)"
                    logMsg("Parsing and loading SysMLv2 text via REST" + fileNameInfo + "...")
                    
                    SessionManager.getInstance().createSession(project, "SysMLv2TestHarness: REST Load SysML")
                    boolean success = false
                    try {
                        ISysMLTransientModelBuilder modelBuilder = new SysMLTransientModelBuilder(project)
                        List<Namespace> allRoots = com.dassault_systemes.modeler.kerml.model.RootNamespaces.getAllRoots(project)
                            .stream().filter { it instanceof Namespace }.map { (Namespace) it }.collect(Collectors.toList())
                        
                        def buildResult = modelBuilder.build(sysmlText, allRoots)
                        Element transientNs = buildResult.getTransientRootNs()
                        
                        def diags = buildResult.getDiagnostics() ?: []
                        def errors = diags.findAll { it.getSeverity().toString() == "ERROR" }
                        
                        if (!errors.isEmpty()) {
                            String errorList = errors.collect { "[" + it.getSeverity() + "] line " + it.getLine() + ": " + it.getMessage() }.join("\\n")
                            // Replace characters that could break JSON
                            errorList = errorList.replace("\"", "'").replace("\r", "")
                            if (SessionManager.getInstance().isSessionCreated(project)) {
                                SessionManager.getInstance().cancelSession(project)
                            }
                            sendResponse(exchange, 400, Jzon.encode([success: false, error: "Semantic errors found:\\n" + errorList]))
                        } else if (transientNs != null) {
                            Element persistentNs = SysMLTextualProjectModelBasedHelper.copyTransientModelToPersistent(project, transientNs)
                            KerMLProjectFeature projectFeature = KerMLProjectFeature.getPrimaryProjectFeature(project)
                            projectFeature.addCommonData(persistentNs)
                            SessionManager.getInstance().closeSession(project)
                            success = true
                            sendResponse(exchange, 200, Jzon.encode([success: true, message: "SysML loaded successfully"]))
                        } else {
                            if (SessionManager.getInstance().isSessionCreated(project)) {
                                SessionManager.getInstance().cancelSession(project)
                            }
                            sendResponse(exchange, 400, Jzon.encode([success: false, error: "Failed to parse SysML (returned null root)"]))
                        }
                    } catch (Exception parseEx) {
                        sendResponse(exchange, 400, Jzon.encode([success: false, error: parseEx.message, stackTrace: getStackTrace(parseEx)]))
                    } finally {
                        if (SessionManager.getInstance().isSessionCreated(project)) {
                            SessionManager.getInstance().cancelSession(project)
                        }
                    }
                } catch (Exception e) {
                    logMsg("Error processing /load-sysml request: " + e.getMessage(), e)
                    sendResponse(exchange, 500, Jzon.encode([success: false, error: e.message, stackTrace: getStackTrace(e)]))
                }
            }
        })
        
        server.createContext("/shutdown", new HttpHandler() {
            void handle(HttpExchange exchange) {
                sendResponse(exchange, 200, Jzon.encode([success: true, message: "Shutting down..."]))
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    void run() {
                        server.stop(0)
                        serverRunning = false
                        logMsg("Server stopped.")
                    }
                }, 1000)
            }
        })

        server.start()
        String successMsg = "✅ All REST endpoints registered successfully. Server active on port " + port + "."
        logMsg(successMsg)
    }

    static void sendResponse(HttpExchange exchange, int statusCode, String response) {
        byte[] bytes = response.getBytes("UTF-8")
        exchange.getResponseHeaders().set("Content-Type", "application/json")
        exchange.sendResponseHeaders(statusCode, bytes.length)
        exchange.getResponseBody().write(bytes)
        exchange.getResponseBody().close()
    }

    static String getStackTrace(Throwable t) {
        if (t == null) return ""
        def sw = new StringWriter()
        t.printStackTrace(new PrintWriter(sw))
        return sw.toString()
    }
}

// Execute the harness since this script might be run directly from the console
SysMLv2TestHarness.main(null)

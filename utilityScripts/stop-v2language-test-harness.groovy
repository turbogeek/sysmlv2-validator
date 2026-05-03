import com.nomagic.magicdraw.core.Application

/**
 * stop-v2language-test-harness.groovy
 * 
 * Scans ports 8770-8780 and sends a shutdown command to any running SysMLv2 Test Harnesses.
 */

int stoppedCount = 0
for (int port = 8770; port <= 8780; port++) {
    try {
        def url = new java.net.URL("http://localhost:" + port + "/shutdown")
        def connection = (java.net.HttpURLConnection) url.openConnection()
        connection.setRequestMethod("GET")
        connection.setConnectTimeout(500)
        connection.setReadTimeout(500)
        
        int responseCode = connection.getResponseCode()
        if (responseCode == 200) {
            Application.getInstance().getGUILog().log("✅ Successfully sent shutdown command to test harness on port " + port)
            println "✅ Successfully sent shutdown command to test harness on port " + port
            stoppedCount++
        }
    } catch (Exception e) {
        // Port not active, ignore
    }
}

if (stoppedCount == 0) {
    Application.getInstance().getGUILog().log("❌ Could not find any running harnesses on ports 8770-8780.")
    println "❌ Could not find any running harnesses on ports 8770-8780."
}

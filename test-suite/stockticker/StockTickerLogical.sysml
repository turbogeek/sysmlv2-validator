package StockTickerLogical {
    doc /*
     * Stock Ticker Device - Logical Design
     *
     * Behavioral models, interfaces, flows, and allocations
     * Platform-independent logical design showing how the system operates
     */

    private import ScalarValues::*;
    private import SI::*;
    private import StockTickerSystem::*;

    // ========================================
    // SIGNAL DEFINITIONS
    // ========================================

    package Signals {
        doc /*
         * Signal definitions for state machine transitions
         * Signals represent events that trigger state changes
         */

        /* Device Lifecycle Signals */
        attribute def BootComplete {
            doc /* Boot sequence completed successfully */
        }

        attribute def WiFiConnected {
            doc /* WiFi connection established */
        }

        attribute def WiFiTimeout {
            doc /* WiFi connection attempt timed out */
        }

        attribute def RefreshTimer {
            doc /* Timer expired, time to refresh stock data */
        }

        attribute def DataReceived {
            doc /* Stock data received from API */
        }

        attribute def NetworkError {
            doc /* Network communication error occurred */
        }

        attribute def DataTimeout {
            doc /* API request timed out */
        }

        attribute def DisplayComplete {
            doc /* Display update completed */
        }

        attribute def ButtonPress {
            doc /* Physical button was pressed */
            attribute buttonType : ButtonType;
        }

        attribute def RetryTimer {
            doc /* Retry timer expired */
        }

        attribute def InactivityTimeout {
            doc /* User inactivity timeout */
        }

        attribute def WakeupTimer {
            doc /* Wake from sleep timer */
        }

        attribute def PowerButtonLongPress {
            doc /* Power button held for long press */
        }

        /* Network Connection Signals */
        attribute def ScanRequest {
            doc /* Request to scan for WiFi networks */
        }

        attribute def NetworksFound {
            doc /* WiFi networks discovered */
            attribute networkCount : Integer;
        }

        attribute def AuthenticationSuccess {
            doc /* WiFi authentication successful */
        }

        attribute def AuthenticationFailed {
            doc /* WiFi authentication failed */
            attribute errorCode : Integer;
        }

        attribute def ConnectionLost {
            doc /* WiFi connection lost */
        }

        attribute def ReconnectionSuccess {
            doc /* WiFi reconnection successful */
        }

        attribute def ReconnectionFailed {
            doc /* WiFi reconnection failed */
        }
    }

    // ========================================
    // INTERFACE DEFINITIONS
    // ========================================

    /* --- Communication Interfaces --- */

    interface def SPIInterface {
        doc /* Serial Peripheral Interface for display communication */

        end master : SPIMasterPort;
        end slave : SPISlavePort;

        attribute clockFrequency : Real {
            doc /* MHz */
        }
        attribute dataMode : Integer {
            doc /* SPI Mode 0-3 */
        }
        attribute bitOrder : String {
            doc /* "MSB" or "LSB" first */
        }
    }

    port def SPIMasterPort {
        doc /* SPI master side (ESP32) */

        out mosi : DigitalSignal;
        out sclk : DigitalSignal;
        out cs : DigitalSignal;
        in miso : DigitalSignal;
    }

    port def SPISlavePort {
        doc /* SPI slave side (Display) */

        in mosi : DigitalSignal;
        in sclk : DigitalSignal;
        in cs : DigitalSignal;
        out miso : DigitalSignal;
    }

    attribute def DigitalSignal {
        doc /* Digital signal (HIGH/LOW) */

        attribute logicLevel : Real {
            doc /* Voltage level */
        }
        attribute logicState : Boolean;
    }

    interface def RESTAPIInterface {
        doc /* REST API for stock data retrieval */

        end client : HTTPClientPort;
        end server : HTTPServerPort;

        attribute protocol : String = "HTTPS";
        attribute apiVersion : String = "HTTP/1.1";
        attribute authMethod : String {
            doc /* "API_KEY", "OAuth", "None" */
        }
    }

    port def HTTPClientPort {
        doc /* HTTP client port */

        out request : HTTPRequest;
        in response : HTTPResponse;
    }

    port def HTTPServerPort {
        doc /* HTTP server port */

        in request : HTTPRequest;
        out response : HTTPResponse;
    }

    attribute def HTTPRequest {
        doc /* HTTP request message */

        attribute requestMethod : String {
            doc /* "GET", "POST", etc. */
        }
        attribute url : String;
        attribute headers[*] : String;
        attribute body : String;
    }

    attribute def HTTPResponse {
        doc /* HTTP response message */

        attribute statusCode : Integer;
        attribute headers[*] : String;
        attribute body : String;
    }

    interface def I2CInterface {
        doc /* Inter-Integrated Circuit bus for expansion */

        end master : I2CMasterPort;
        end slave : I2CSlavePort;

        attribute clockSpeed : Real {
            doc /* kHz */
        }
        attribute addressing : String {
            doc /* "7-bit" or "10-bit" */
        }
    }

    port def I2CMasterPort {
        inout sda : DigitalSignal;
        out scl : DigitalSignal;
    }

    port def I2CSlavePort {
        inout sda : DigitalSignal;
        in scl : DigitalSignal;
    }

    // ========================================
    // STATE MACHINES
    // ========================================

    state def DeviceOperationalStates {
        doc /*
         * High-level operational states of the stock ticker device
         * Models the complete device lifecycle
         */

        /* State variables used in guards */
        attribute connectionSuccessful : Boolean {
            doc /* True if WiFi connection was established successfully */
        }

        attribute wifiConnected : Boolean {
            doc /* True if currently connected to WiFi */
        }

        attribute dataValid : Boolean {
            doc /* True if received stock data passed validation */
        }

        attribute button : ButtonType {
            doc /* Which button was pressed */
        }

        entry;
        then poweringOn;

        state poweringOn {
            doc /* Initial power-on and boot sequence */
            entry action initializeHardware;
            do action runBootSequence;
        }

        state idle {
            doc /* Device is ready but not actively fetching */
            entry action displayIdleScreen;
            do action monitorButtons;
        }

        state connectingWiFi {
            doc /* Establishing WiFi connection */
            entry action scanNetworks;
            do action attemptConnection;
        }

        state fetchingData {
            doc /* Retrieving stock data from API */
            entry action prepareAPIRequest;
            do action sendHTTPRequest;
        }

        state displayingData {
            doc /* Rendering stock information on screen */
            entry action updateDisplay;
            do action handleButtonInput;
        }

        state errorState {
            doc /* Error condition - display error message */
            entry action showErrorMessage;
            do action blinkErrorIndicator;
        }

        state deepSleep {
            doc /* Low power mode to conserve battery */
            entry action dimDisplay;
            do action sleepUntilWakeup;
        }

        /* Transitions */

        transition boot_to_wifi
            first poweringOn
            accept Signals::BootComplete
            then connectingWiFi;

        transition wifi_to_idle
            first connectingWiFi
            accept Signals::WiFiConnected
            if connectionSuccessful
            then idle;

        transition wifi_failed
            first connectingWiFi
            accept Signals::WiFiTimeout
            then errorState;

        transition idle_to_fetch
            first idle
            accept Signals::RefreshTimer
            if wifiConnected
            then fetchingData;

        transition fetch_to_display
            first fetchingData
            accept Signals::DataReceived
            if dataValid
            then displayingData;

        transition fetch_to_error_network
            first fetchingData
            accept Signals::NetworkError
            then errorState;

        transition fetch_to_error_timeout
            first fetchingData
            accept Signals::DataTimeout
            then errorState;

        transition display_to_idle
            first displayingData
            accept Signals::DisplayComplete
            then idle;

        transition button_navigate
            first displayingData
            accept Signals::ButtonPress
            if button == ButtonType::next or button == ButtonType::previous
            then displayingData;

        transition error_recovery
            first errorState
            accept Signals::RetryTimer
            then connectingWiFi;

        transition idle_to_sleep
            first idle
            accept Signals::InactivityTimeout
            then deepSleep;

        transition displaying_to_sleep
            first displayingData
            accept Signals::InactivityTimeout
            then deepSleep;

        transition sleep_to_idle_button
            first deepSleep
            accept Signals::ButtonPress
            then idle;

        transition sleep_to_idle_timer
            first deepSleep
            accept Signals::WakeupTimer
            then idle;

        transition idle_to_poweroff
            first idle
            accept Signals::PowerButtonLongPress
            then poweringOn;

        transition displaying_to_poweroff
            first displayingData
            accept Signals::PowerButtonLongPress
            then poweringOn;
    }

    state def NetworkConnectionStates {
        doc /* Detailed WiFi connection state machine */

        entry; then disconnected;

        state disconnected {
            doc /* Not connected to WiFi */
        }

        state scanning {
            doc /* Scanning for available networks */
        }

        state authenticating {
            doc /* Connecting and authenticating to network */
        }

        state connected {
            doc /* Successfully connected */
            entry action updateConnectionStatus;
        }

        state reconnecting {
            doc /* Lost connection, attempting to reconnect */
        }

        transition start_scan
            first disconnected
            accept Signals::ScanRequest
            then scanning;

        transition scan_complete
            first scanning
            accept Signals::NetworksFound
            then authenticating;

        transition auth_success
            first authenticating
            accept Signals::AuthenticationSuccess
            then connected;

        transition auth_fail
            first authenticating
            accept Signals::AuthenticationFailed
            then disconnected;

        transition connection_lost
            first connected
            accept Signals::ConnectionLost
            then reconnecting;

        transition reconnect_success
            first reconnecting
            accept Signals::ReconnectionSuccess
            then connected;

        transition reconnect_fail
            first reconnecting
            accept Signals::ReconnectionFailed
            then disconnected;
    }

    // ========================================
    // ACTION DEFINITIONS
    // ========================================

    action def FetchStockData {
        doc /*
         * Complete action sequence to fetch stock data from REST API
         * and return parsed stock price information
         */

        in stockSymbol : StockSymbol;
        out stockData : StockPriceData;

        /* Sub-actions */
        action buildURL {
            in symbol = stockSymbol;
            out apiURL : String;
        }
        first buildURL then sendRequest;

        action sendRequest : SendHTTPRequest {
            in url = buildURL.apiURL;
            out response : HTTPResponse;
        }
        first sendRequest then validateResponse;

        action validateResponse {
            in response = sendRequest.response;
            out isValid : Boolean;
        }
        first validateResponse then decision1;

        decide decision1;
        if validateResponse.isValid then parseJSON;
        if not validateResponse.isValid then handleError;

        action parseJSON {
            in jsonData = sendRequest.response.body;
            out parsedData : StockPriceData;
        }
        first parseJSON then merge1;

        action handleError {
            doc /* Log error and use cached data */
            out fallbackData : StockPriceData;
        }
        first handleError then merge1;

        merge merge1;
        first merge1 then assignResult;

        action assignResult {
            in data;
            assign stockData := data;
        }
    }

    action def SendHTTPRequest {
        doc /* Send HTTP request and receive response */

        in url : String;
        out response : HTTPResponse;

        action createSocket;
        first createSocket then establishConnection;

        action establishConnection;
        first establishConnection then sendHeaders;

        action sendHeaders;
        first sendHeaders then receiveResponse;

        action receiveResponse {
            out httpResponse : HTTPResponse;
        }
        first receiveResponse then closeSocket;

        action closeSocket;
    }

    action def UpdateDisplay {
        doc /*
         * Update display to show current stock information
         * Renders symbol, price, change, and timestamp
         */

        in stockData : StockPriceData;

        action clearDisplay {
            doc /* Clear previous content */
        }
        first clearDisplay then drawBackground;

        action drawBackground {
            doc /* Draw background and borders */
        }
        first drawBackground then drawSymbol;

        action drawSymbol {
            in symbol = stockData.symbol;
            doc /* Display stock ticker symbol at top */
        }
        first drawSymbol then drawPrice;

        action drawPrice {
            in price = stockData.price;
            doc /* Display current price prominently */
        }
        first drawPrice then drawChange;

        action drawChange {
            in change = stockData.change;
            in percentChange = stockData.percentChange;
            doc /* Display price change with color (red/green) */
        }
        first drawChange then drawTimestamp;

        action drawTimestamp {
            in timestamp = stockData.timestamp;
            doc /* Display last update time */
        }
        first drawTimestamp then updateBacklight;

        action updateBacklight {
            doc /* Adjust brightness if needed */
        }
    }

    action def HandleButtonPress {
        doc /*
         * Process button press and take appropriate action
         * Supports short press, long press, and double press
         */

        in button : ButtonType;
        in pressDuration : Real {
            doc /* milliseconds */
        }

        action debounceButton {
            doc /* Wait for button signal to stabilize */
        }
        first debounceButton then detectPressType;

        action detectPressType {
            in duration = pressDuration;
            out pressType : String {
                doc /* "short", "long", "double" */
            }
        }
        first detectPressType then routeAction;

        action routeAction {
            in buttonInput = button;
            in pressTypeInput = detectPressType.pressType;
        }
        first routeAction then decision1;

        decide decision1;
        if button == ButtonType::next and detectPressType.pressType == "short" then selectNextStock;
        if button == ButtonType::previous and detectPressType.pressType == "short" then selectPreviousStock;
        if button == ButtonType::select and detectPressType.pressType == "short" then toggleFavorite;
        if button == ButtonType::power and detectPressType.pressType == "long" then enterSleepMode;

        action selectNextStock {
            doc /* Increment stock index and refresh display */
        }
        first selectNextStock then merge1;

        action selectPreviousStock {
            doc /* Decrement stock index and refresh display */
        }
        first selectPreviousStock then merge1;

        action toggleFavorite {
            doc /* Mark/unmark current stock as favorite */
        }
        first toggleFavorite then merge1;

        action enterSleepMode {
            doc /* Enter deep sleep to save power */
        }
        first enterSleepMode then merge1;

        merge merge1;
    }

    action def ManageWiFiConnection {
        doc /* Manage WiFi connection lifecycle */

        action scanForNetworks {
            out networks[*] : String;
        }
        first scanForNetworks then selectNetwork;

        action selectNetwork {
            in networks = scanForNetworks.networks;
            out selectedSSID : String;
        }
        first selectNetwork then authenticate;

        action authenticate {
            in ssid = selectNetwork.selectedSSID;
            in password : String;
            out authResult : Boolean;
        }
        first authenticate then decision1;

        decide decision1;
        if authenticate.authResult == true then maintainConnection;
        if authenticate.authResult == false then retryConnection;

        action maintainConnection {
            doc /* Monitor connection and handle keepalive */
        }
        first maintainConnection then merge1;

        action retryConnection {
            doc /* Retry with exponential backoff */
        }
        first retryConnection then merge1;

        merge merge1;
    }

    action def ValidateStockData {
        doc /* Validate received stock data for correctness */

        in rawData : StockPriceData;
        out validData : StockPriceData;
        out isValid : Boolean;

        action checkMandatoryFields {
            doc /* Verify symbol, price fields exist */
            out fieldsPresent : Boolean;
        }
        first checkMandatoryFields then validateRanges;

        action validateRanges {
            doc /* Check price > 0, change is reasonable */
            out rangesValid : Boolean;
        }
        first validateRanges then checkTimestamp;

        action checkTimestamp {
            doc /* Verify timestamp is recent */
            out timestampValid : Boolean;
        }
        first checkTimestamp then computeValidity;

        action computeValidity {
            assign isValid := (
                checkMandatoryFields.fieldsPresent and
                validateRanges.rangesValid and
                checkTimestamp.timestampValid
            );
        }
        first computeValidity then decision1;

        decide decision1;
        if isValid then passthrough;
        if not isValid then useCachedData;

        action passthrough {
            assign validData := rawData;
        }
        first passthrough then merge1;

        action useCachedData {
            doc /* Return last known good value */
        }
        first useCachedData then merge1;

        merge merge1;
    }

    // ========================================
    // FLOW DEFINITIONS
    // ========================================

    /* Flow connection definitions specify the type of data transfer */

    flow def StockDataFlow {
        doc /*
         * Message flow carrying stock price data
         * Used to transfer stock information from API to display system
         */

        in source : StockPriceData;
        out sink : StockPriceData;
    }

    flow def ButtonEventFlow {
        doc /*
         * Event flow carrying button press events
         * Transfers user input from button hardware to processor
         */

        in source : ButtonType;
        out sink : ButtonType;
    }

    flow def DisplayCommandFlow {
        doc /*
         * Streaming flow for continuous display updates
         * Carries rendering commands from processor to display
         */

        in source : StockPriceData;
        out sink : StockPriceData;
    }

    // ========================================
    // ALLOCATIONS
    // ========================================

    /* Note: Allocations map action/part USAGES (instances) to hardware, not definitions.
     * Since we have action DEFINITIONS (FetchStockData, UpdateDisplay, etc.) but no
     * specific action USAGES to allocate, we document the software-to-hardware mapping
     * here as comments:
     *
     * Core 0 (Network operations):
     * - FetchStockData action
     * - ManageWiFiConnection action
     * - SendHTTPRequest action
     *
     * Core 1 (Display and UI):
     * - UpdateDisplay action
     * - HandleButtonPress action
     * - ValidateStockData action
     *
     * To create actual allocations, we would need to define action usages
     * within a system context, then allocate those usages to processor cores.
     */

    /* Note: Memory, power, and timing budgets are better modeled as
     * analysis cases or part attributes, not allocations.
     * Allocations are for mapping between model elements.
     *
     * Memory budget: 520 KB ESP32 SRAM
     * - Network buffer: 32 KB
     * - Display buffer: 64 KB
     * - Stock cache: 16 KB
     * - System reserved: 128 KB
     * - Available: 280 KB
     *
     * Power budget: ~595 mW average
     * - ESP32 core: 40 mW
     * - ESP32 memory: 20 mW
     * - Display LCD: 65 mW
     * - Display backlight: 100 mW
     * - WiFi average: 55 mW (20 idle + 350 active @ 10% duty)
     *
     * Timing allocation:
     * - Stock fetch: 5.0 s
     * - Button scan: 10 ms
     * - Display update: 100 ms
     * - Battery check: 60 s
     * - WiFi keepalive: 30 s
     */

    // ========================================
    // CONCERNS AND VIEWPOINTS
    // ========================================

    concern def EndUserConcern {
        doc /* Concerns from end user perspective */

        stakeholder user : Person;

        require constraint {
            doc /* Device must be readable from 1 meter distance */
        }

        require constraint {
            doc /* Setup must complete in under 5 minutes */
        }

        require constraint {
            doc /* Device must work reliably for 6+ months */
        }

        require constraint {
            doc /* Updates should not interrupt viewing */
        }
    }

    concern def DeveloperConcern {
        doc /* Concerns from developer perspective */

        stakeholder developer : Person;

        require constraint {
            doc /* Code must fit in 4MB flash with OTA capability */
        }

        require constraint {
            doc /* Must support debugging via USB serial */
        }

        require constraint {
            doc /* Memory usage must stay under 80% to prevent crashes */
        }

        require constraint {
            doc /* WiFi credentials must be configurable without recompile */
        }
    }

    concern def ManufacturabilityConcer {
        doc /* Concerns related to manufacturing and production */

        stakeholder manufacturer : Person;

        require constraint {
            doc /* Components must be readily available */
        }

        require constraint {
            doc /* Assembly must not require specialized tools */
        }

        require constraint {
            doc /* Unit cost must be under $30 at quantity 100 */
        }
    }

    viewpoint def UserManualViewpoint {
        doc /* Viewpoint for user-facing documentation */

        frame concern userConcerns : EndUserConcern;

        require constraint {
            doc /*
             * Manual must include:
             * - Setup instructions with WiFi configuration
             * - Button function reference
             * - Troubleshooting guide for common issues
             * - Display symbols and indicators explanation
             */
        }
    }

    viewpoint def TechnicalDocumentationViewpoint {
        doc /* Viewpoint for technical documentation */

        frame concern devConcerns : DeveloperConcern;

        require constraint {
            doc /*
             * Documentation must include:
             * - Pin mapping and GPIO assignments
             * - Memory map and allocation
             * - Power consumption profile
             * - API endpoints and data formats
             * - State machine diagrams
             */
        }
    }
}

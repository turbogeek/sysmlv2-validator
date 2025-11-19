package StockTickerSystem {
    doc /*
     * Stock Ticker Device - Black Box Design
     *
     * A portable stock ticker device that displays real-time stock prices
     * using WiFi connectivity and a REST API interface.
     */

    private import ScalarValues::*;
    private import SI::*;

    // ========================================
    // EXTERNAL SYSTEMS
    // ========================================

    /**
     * External stock price data service accessible via REST API
     */
    part def StockPriceService {
        doc /* External service providing real-time stock price data via REST API */

        port restAPI : StockDataPort;

        attribute serviceURL : String;
        attribute updateInterval : Real; // seconds
    }

    port def StockDataPort {
        doc /* REST API port for stock price data exchange */

        out stockData : StockPriceData;
        in stockRequest : StockRequest;
    }

    // ========================================
    // DATA TYPES
    // ========================================

    attribute def StockPriceData {
        doc /* Real-time stock price information */

        attribute symbol : String;
        attribute price : Real;
        attribute change : Real;
        attribute percentChange : Real;
        attribute timestamp : String;
    }

    attribute def StockRequest {
        doc /* Request for specific stock data */

        attribute symbol : String;
        attribute requestTime : String;
    }

    attribute def StockSymbol {
        doc /* Stock ticker symbol (e.g., AAPL, MSFT) */

        attribute symbol : String;
    }

    enum def ButtonType {
        doc /* Types of buttons on the device */

        enum next;
        enum previous;
        enum select;
        enum power;
    }

    // ========================================
    // HARDWARE COMPONENTS
    // ========================================

    part def Computer {
        doc /* Main computing unit with processor, memory, and wireless capability */

        part processor : Processor;
        part memory : Memory;
        part wifiModule : WiFiModule;
        part storage : Storage;

        port displayInterface : DisplayPort;
        port buttonInterface : ButtonPort;
        port networkInterface : NetworkPort;
        port powerInput : PowerPort;

        attribute operatingVoltage : Real = 5.0; // volts
    }

    part def Processor {
        doc /* CPU for running the stock ticker software */

        attribute clockSpeed : Real {
            doc /* MHz */
        }
        attribute cores : Integer;
    }

    part def Memory {
        doc /* RAM for program execution */

        attribute capacity : Real {
            doc /* MB */
        }
        attribute memoryType : String;
    }

    part def Storage {
        doc /* Non-volatile storage for stock symbols and configuration */

        attribute capacity : Real {
            doc /* MB */
        }
        attribute storageType : String {
            doc /* e.g., "Flash", "EEPROM" */
        }

        attribute preloadedStocks[*] : StockSymbol;
    }

    part def WiFiModule {
        doc /* WiFi radio for internet connectivity */

        port networkPort : NetworkPort;

        attribute communicationStandard : String {
            doc /* e.g., "802.11n", "802.11ac" */
        }
        attribute frequency : Real {
            doc /* GHz */
        }
        attribute signalRange : Real {
            doc /* meters */
        }
    }

    port def NetworkPort {
        doc /* Network communication port */

        out networkData : String;
        in networkData : String;
    }

    part def Display {
        doc /* Visual display for showing stock information */

        port displayPort : DisplayPort;
        port powerInput : PowerPort;

        attribute width : Real {
            doc /* pixels */
        }
        attribute height : Real {
            doc /* pixels */
        }
        attribute displayType : String {
            doc /* e.g., "LCD", "OLED", "E-Ink" */
        }
        attribute diagonal : Real {
            doc /* inches */
        }
        attribute currentStock : StockPriceData;
    }

    port def DisplayPort {
        doc /* Interface for sending display data */

        in displayData : StockPriceData;
        in command : String;
    }

    part def ButtonArray {
        doc /* Collection of user input buttons */

        port buttonPort : ButtonPort;

        part nextButton : Button {
            :>> buttonType = ButtonType::next;
        }
        part previousButton : Button {
            :>> buttonType = ButtonType::previous;
        }
        part selectButton : Button {
            :>> buttonType = ButtonType::select;
        }
        part powerButton : Button {
            :>> buttonType = ButtonType::power;
        }
    }

    part def Button {
        doc /* Single push button for user input */

        attribute buttonType : ButtonType;
        attribute pressed : Boolean;
    }

    port def ButtonPort {
        doc /* Interface for button input signals */

        out buttonPress : ButtonType;
        out buttonState : Boolean;
    }

    part def PowerSource {
        doc /* Power supply for the device */

        port powerOutput : PowerPort;

        attribute voltage : Real {
            doc /* volts */
        }
        attribute currentRating : Real {
            doc /* amperes */
        }
        attribute capacity : Real {
            doc /* mAh (if battery) */
        }
        attribute powerSourceType : String {
            doc /* e.g., "Battery", "AC Adapter", "USB" */
        }
    }

    port def PowerPort {
        doc /* Power delivery port */

        out voltage : Real;
        out current : Real;
    }

    part def Stand {
        doc /* Physical stand for holding the display unit and power supply */

        attribute material : String {
            doc /* e.g., "Plastic", "Metal", "Wood" */
        }
        attribute viewingAngle : Real {
            doc /* degrees (viewing angle) */
        }
        attribute standHeight : Real {
            doc /* cm */
        }
        attribute standWidth : Real {
            doc /* cm */
        }
        attribute standDepth : Real {
            doc /* cm */
        }
    }

    part def Enclosure {
        doc /* Housing for the electronics */

        attribute material : String;
        attribute dimensions : String {
            doc /* e.g., "10cm x 5cm x 2cm" */
        }
        attribute color : String;
        attribute weight : Real {
            doc /* grams */
        }
    }

    // ========================================
    // MAIN SYSTEM ASSEMBLY
    // ========================================

    /**
     * Complete Stock Ticker Device System
     */
    part def StockTickerDevice {
        doc /*
         * Complete stock ticker device with computer, display, buttons,
         * power source, and physical stand. Connects to external stock
         * price service via WiFi.
         */

        // Hardware components
        part computer : Computer {
            :>> operatingVoltage = 5.0;

            :>> storage {
                :>> capacity = 256.0;
                :>> storageType = "Flash";
                /* Preloaded stocks (can be modified via configuration) */
                :>> preloadedStocks[1] = new StockSymbol (symbol = "AAPL");
                :>> preloadedStocks[2] = new StockSymbol (symbol = "MSFT");
                :>> preloadedStocks[3] = new StockSymbol (symbol = "GOOGL");
                :>> preloadedStocks[4] = new StockSymbol (symbol = "AMZN");
                :>> preloadedStocks[5] = new StockSymbol (symbol = "TSLA");
            }

            :>> wifiModule {
                :>> communicationStandard = "802.11n";
                :>> frequency = 2.4;
                :>> signalRange = 50.0;
            }

            :>> processor {
                :>> clockSpeed = 1000.0;
                :>> cores = 2;
            }

            :>> memory {
                :>> capacity = 512.0;
                :>> memoryType = "DDR3";
            }
        }

        part display : Display {
            :>> width = 320.0;
            :>> height = 240.0;
            :>> displayType = "LCD";
            :>> diagonal = 3.5;
        }

        part buttons : ButtonArray;

        part powerSource : PowerSource {
            :>> voltage = 5.0;
            :>> currentRating = 2.0;
            :>> capacity = 5000.0;
            :>> powerSourceType = "USB Power Adapter";
        }

        part stand : Stand {
            :>> material = "Plastic";
            :>> viewingAngle = 45.0;
            :>> standHeight = 8.0;
            :>> standWidth = 12.0;
            :>> standDepth = 10.0;
        }

        part enclosure : Enclosure {
            :>> material = "ABS Plastic";
            :>> dimensions = "12cm x 8cm x 2cm";
            :>> color = "Black";
            :>> weight = 150.0;
        }

        // External reference to stock price service
        ref part stockService : StockPriceService {
            :>> serviceURL = "https://api.stockprice.com/v1";
            :>> updateInterval = 5.0; // 5 seconds
        }

        /* Internal connections */
        connection computerToDisplay : DeviceConnection
            connect computer.displayInterface to display.displayPort;

        connection computerToButtons : DeviceConnection
            connect computer.buttonInterface to buttons.buttonPort;

        connection powerToComputer : DeviceConnection
            connect powerSource.powerOutput to computer.powerInput;

        connection powerToDisplay : DeviceConnection
            connect powerSource.powerOutput to display.powerInput;

        /* External network connection via WiFi */
        interface wifiConnection : NetworkInterface
            connect computer.wifiModule.networkPort to stockService.restAPI;

        /* Configuration attributes */
        attribute currentStockIndex : Integer := 0;
        attribute refreshInterval : Real := 5.0 {
            doc /* seconds */
        }
        attribute networkConnected : Boolean;
        attribute displayBrightness : Real := 80.0 {
            doc /* percent */
        }
    }

    interface def NetworkInterface {
        doc /* Network interface connecting device WiFi to external service */

        end computer : NetworkPort;
        end service : StockDataPort;
    }

    connection def DeviceConnection {
        doc /* Generic connection between components */
    }

    // ========================================
    // SYSTEM INSTANCE
    // ========================================

    /**
     * Instance of the stock ticker device
     */
    part stockTicker : StockTickerDevice {
        doc /* The actual stock ticker device instance */
    }

    // ========================================
    // USE CASES
    // ========================================

    /**
     * User viewing stock prices
     */
    use case def ViewStockPrice {
        doc /*
         * User views current stock price information on the display.
         * The device retrieves and displays real-time stock data.
         */

        subject device : StockTickerDevice;
        actor user : Person;
        actor stockAPI : StockPriceService;

        objective {
            doc /* User successfully views current stock price with accurate, up-to-date information. */
        }

        /* Use case steps would be defined in detailed behavior models */
        include use case retrieveStockData;
        include use case displayStockInfo;
    }

    /**
     * User navigating between stocks
     */
    use case def NavigateBetweenStocks {
        doc /*
         * User presses next/previous buttons to cycle through
         * the preloaded list of stock symbols.
         */

        subject device : StockTickerDevice;
        actor user : Person;

        objective {
            doc /* User successfully navigates to desired stock in the preloaded list. */
        }

        include use case detectButtonPress;
        include use case updateDisplayedStock;
    }

    /**
     * User selecting a stock
     */
    use case def SelectStock {
        doc /*
         * User presses the select button to mark a stock as favorite
         * or trigger additional information display.
         */

        subject device : StockTickerDevice;
        actor user : Person;

        objective {
            doc /* User successfully selects a stock for additional actions. */
        }
    }

    /**
     * Device retrieving stock data from external service
     */
    use case def RetrieveStockData {
        doc /*
         * Device connects to external REST API via WiFi and retrieves
         * current stock price data at regular intervals.
         */

        subject device : StockTickerDevice;
        actor stockAPI : StockPriceService;

        objective {
            doc /* Device successfully retrieves and parses stock data from external API. */
        }

        include use case connectToWiFi;
        include use case sendAPIRequest;
        include use case parseAPIResponse;
    }

    /**
     * Device displaying stock information
     */
    use case def DisplayStockInfo {
        doc /*
         * Device formats and displays stock price information on the screen,
         * including symbol, price, and change percentage.
         */

        subject device : StockTickerDevice;

        objective {
            doc /* Stock information is clearly displayed and readable by the user. */
        }
    }

    /**
     * Device connecting to WiFi network
     */
    use case def ConnectToWiFi {
        doc /*
         * Device WiFi module establishes connection to configured
         * wireless network for internet access.
         */

        subject device : StockTickerDevice;

        objective {
            doc /* Device successfully connects to WiFi and can access the internet. */
        }
    }

    /**
     * Device powering on/off
     */
    use case def PowerControl {
        doc /*
         * User presses power button to turn device on or off.
         * Device manages power state transitions.
         */

        subject device : StockTickerDevice;
        actor user : Person;

        objective {
            doc /* Device successfully transitions between power states as requested. */
        }
    }

    /**
     * Detecting button presses
     */
    use case def DetectButtonPress {
        doc /* Device detects and responds to button press events. */

        subject device : StockTickerDevice;
        actor user : Person;

        objective {
            doc /* Button presses are reliably detected and processed. */
        }
    }

    /**
     * Sending API request
     */
    use case def SendAPIRequest {
        doc /* Device sends HTTP request to stock price API. */

        subject device : StockTickerDevice;
        actor stockAPI : StockPriceService;

        objective {
            doc /* API request is properly formatted and successfully transmitted. */
        }
    }

    /**
     * Parsing API response
     */
    use case def ParseAPIResponse {
        doc /* Device parses JSON response from stock price API. */

        subject device : StockTickerDevice;

        objective {
            doc /* API response is successfully parsed and stock data extracted. */
        }
    }

    /**
     * Updating displayed stock
     */
    use case def UpdateDisplayedStock {
        doc /* Device updates the display to show different stock from the list. */

        subject device : StockTickerDevice;

        objective {
            doc /* Display is updated to show the newly selected stock. */
        }
    }

    // ========================================
    // REQUIREMENTS
    // ========================================

    /**
     * Base data type for person actor
     */
    part def Person {
        attribute name : String;
    }

    // --- Functional Requirements ---

    requirement def <'FR-1'> DisplayStockPriceRequirement {
        doc /*
         * The device shall display the current stock price for the selected stock symbol.
         */

        subject device : StockTickerDevice;

        require constraint {
            doc /* Display shows current price, symbol, change, and percent change. */
        }
    }

    requirement def <'FR-2'> RefreshIntervalRequirement {
        doc /*
         * The device shall retrieve updated stock data at a configurable interval,
         * with a default of 5 seconds.
         */

        subject device : StockTickerDevice;

        attribute actualInterval : Real;
        attribute requiredInterval : Real;

        assume constraint { requiredInterval > 0.0 }
        require constraint { actualInterval == requiredInterval }
    }

    requirement def <'FR-3'> PreloadedStocksRequirement {
        doc /*
         * The device shall come preloaded with a configurable list of stock symbols.
         */

        subject device : StockTickerDevice;

        attribute minStocks : Integer;

        require constraint {
            doc /* Device storage contains at least minStocks preloaded symbols. */
        }
    }

    requirement def <'FR-4'> ButtonNavigationRequirement {
        doc /*
         * The device shall allow users to navigate between stocks using
         * next and previous buttons.
         */

        subject device : StockTickerDevice;
        actor user : Person;

        require constraint {
            doc /* Next button advances to next stock, previous button goes to previous stock. */
        }
    }

    requirement def <'FR-5'> WiFiConnectivityRequirement {
        doc /*
         * The device shall connect to the internet via built-in WiFi module.
         */

        subject device : StockTickerDevice;

        require constraint {
            doc /* WiFi module successfully connects to configured network. */
        }
    }

    requirement def <'FR-6'> RESTAPIRequirement {
        doc /*
         * The device shall retrieve stock data from an external REST API service.
         */

        subject device : StockTickerDevice;
        actor stockAPI : StockPriceService;

        require constraint {
            doc /* Device successfully sends HTTP requests and receives JSON responses. */
        }
    }

    requirement def <'FR-7'> PowerControlRequirement {
        doc /*
         * The device shall provide a power button to turn the device on and off.
         */

        subject device : StockTickerDevice;
        actor user : Person;

        require constraint {
            doc /* Power button controls device power state. */
        }
    }

    // --- Performance Requirements ---

    requirement def <'PERF-1'> DisplayUpdateLatencyRequirement {
        doc /*
         * The device shall update the display within 500ms of receiving new stock data.
         */

        subject device : StockTickerDevice;

        attribute actualLatency : Real {
            doc /* milliseconds */
        }
        attribute maxLatency : Real := 500.0 {
            doc /* milliseconds */
        }

        require constraint { actualLatency <= maxLatency }
    }

    requirement def <'PERF-2'> APIResponseTimeRequirement {
        doc /*
         * The device shall handle API response times up to 3 seconds without freezing.
         */

        subject device : StockTickerDevice;

        attribute maxResponseTime : Real := 3000.0 {
            doc /* milliseconds */
        }

        require constraint {
            doc /* Device remains responsive during API requests. */
        }
    }

    requirement def <'PERF-3'> ButtonResponseRequirement {
        doc /*
         * The device shall respond to button presses within 100ms.
         */

        subject device : StockTickerDevice;

        attribute actualResponseTime : Real {
            doc /* milliseconds */
        }
        attribute maxResponseTime : Real := 100.0 {
            doc /* milliseconds */
        }

        require constraint { actualResponseTime <= maxResponseTime }
    }

    // --- Interface Requirements ---

    requirement def <'INT-1'> DisplaySizeRequirement {
        doc /*
         * The device display shall be at least 3 inches diagonal for readability.
         */

        subject device : StockTickerDevice;

        attribute actualSize : Real;
        attribute minSize : Real := 3.0 {
            doc /* inches */
        }

        require constraint { actualSize >= minSize }
    }

    requirement def <'INT-2'> ButtonCountRequirement {
        doc /*
         * The device shall have at least 4 buttons: next, previous, select, and power.
         */

        subject device : StockTickerDevice;

        attribute actualButtons : Integer;
        attribute requiredButtons : Integer := 4;

        require constraint { actualButtons >= requiredButtons }
    }

    requirement def <'INT-3'> StandStabilityRequirement {
        doc /*
         * The stand shall hold the device at a viewing angle between 30 and 60 degrees.
         */

        subject device : StockTickerDevice;

        attribute actualAngle : Real;
        attribute minAngle : Real := 30.0;
        attribute maxAngle : Real := 60.0;

        require constraint { actualAngle >= minAngle and actualAngle <= maxAngle }
    }

    // --- Connectivity Requirements ---

    requirement def <'CONN-1'> WiFiStandardRequirement {
        doc /*
         * The device WiFi module shall support at least 802.11n standard.
         */

        subject device : StockTickerDevice;

        require constraint {
            doc /* WiFi module supports 802.11n or newer standard. */
        }
    }

    requirement def <'CONN-2'> WiFiRangeRequirement {
        doc /*
         * The device WiFi module shall have a range of at least 30 meters in open space.
         */

        subject device : StockTickerDevice;

        attribute actualRange : Real {
            doc /* meters */
        }
        attribute minRange : Real := 30.0 {
            doc /* meters */
        }

        require constraint { actualRange >= minRange }
    }

    requirement def <'CONN-3'> NetworkSecurityRequirement {
        doc /*
         * The device shall support WPA2 or better WiFi security protocols.
         */

        subject device : StockTickerDevice;

        require constraint {
            doc /* WiFi connection supports secure encryption. */
        }
    }

    // --- Power Requirements ---

    requirement def <'PWR-1'> PowerVoltageRequirement {
        doc /*
         * The device shall operate on 5V DC power supply.
         */

        subject device : StockTickerDevice;

        attribute actualVoltage : Real;
        attribute nominalVoltage : Real := 5.0;
        attribute voltageTolerance : Real := 0.25 {
            doc /* +/- 0.25V */
        }

        require constraint {
            actualVoltage >= (nominalVoltage - voltageTolerance) and
            actualVoltage <= (nominalVoltage + voltageTolerance)
        }
    }

    requirement def <'PWR-2'> PowerConsumptionRequirement {
        doc /*
         * The device shall consume no more than 10 watts during normal operation.
         */

        subject device : StockTickerDevice;

        attribute actualPower : Real {
            doc /* watts */
        }
        attribute maxPower : Real := 10.0 {
            doc /* watts */
        }

        require constraint { actualPower <= maxPower }
    }

    // --- Reliability Requirements ---

    requirement def <'REL-1'> NetworkRetryRequirement {
        doc /*
         * The device shall automatically retry failed API requests up to 3 times
         * before displaying an error.
         */

        subject device : StockTickerDevice;

        attribute maxRetries : Integer := 3;

        require constraint {
            doc /* Device implements retry logic for failed network requests. */
        }
    }

    requirement def <'REL-2'> DataValidationRequirement {
        doc /*
         * The device shall validate received stock data before displaying it.
         */

        subject device : StockTickerDevice;

        require constraint {
            doc /* Stock data is validated for completeness and correctness. */
        }
    }

    // --- Error Handling Requirements ---

    requirement def <'ERR-1'> WiFiDisconnectionHandling {
        doc /*
         * The device shall detect WiFi disconnection within 10 seconds
         * and display a connection error indicator.
         */

        subject device : StockTickerDevice;

        attribute detectionTime : Real {
            doc /* seconds */
        }
        attribute maxDetectionTime : Real := 10.0 {
            doc /* seconds */
        }

        require constraint {
            detectionTime <= maxDetectionTime
        }
    }

    requirement def <'ERR-2'> APIErrorHandling {
        doc /*
         * The device shall retry failed API requests up to 3 times
         * with exponential backoff before displaying an error.
         */

        subject device : StockTickerDevice;

        attribute maxRetries : Integer := 3;
        attribute currentRetry : Integer;
        attribute backoffMultiplier : Real := 2.0;
        attribute baseRetryDelay : Real := 1.0 {
            doc /* seconds */
        }

        require constraint {
            currentRetry <= maxRetries
        }
    }

    requirement def <'ERR-3'> DisplayTimeoutProtection {
        doc /*
         * The device shall reduce display brightness after 5 minutes
         * of inactivity to prevent screen burn-in.
         */

        subject device : StockTickerDevice;

        attribute inactivityTimeout : Real := 300.0 {
            doc /* seconds */
        }
        attribute dimmedBrightness : Real := 20.0 {
            doc /* percent */
        }
        attribute normalBrightness : Real := 80.0 {
            doc /* percent */
        }

        require constraint {
            inactivityTimeout > 0.0
        }
    }

    requirement def <'ERR-4'> LowBatteryWarning {
        doc /*
         * The device shall display a low battery warning when battery
         * voltage drops below 3.3V.
         */

        subject device : StockTickerDevice;

        attribute batteryVoltage : Real;
        attribute warningVoltage : Real := 3.3 {
            doc /* volts */
        }

        require constraint {
            doc /* Display warning icon when batteryVoltage < warningVoltage */
        }
    }

    requirement def <'ERR-5'> InvalidDataHandling {
        doc /*
         * The device shall validate received stock data and display
         * the last known good value if current data is invalid.
         */

        subject device : StockTickerDevice;

        attribute dataValidationEnabled : Boolean := true;

        require constraint {
            dataValidationEnabled == true
        }
    }

    requirement def <'ERR-6'> NetworkTimeoutHandling {
        doc /*
         * The device shall timeout network requests after 10 seconds
         * and handle the timeout gracefully.
         */

        subject device : StockTickerDevice;

        attribute networkTimeout : Real := 10.0 {
            doc /* seconds */
        }
        attribute actualResponseTime : Real;

        require constraint {
            doc /* System remains responsive even if actualResponseTime > networkTimeout */
        }
    }

    // --- Storage Requirements ---

    requirement def <'STOR-1'> MinimumStorageRequirement {
        doc /*
         * The device shall have at least 256MB of storage for software and configuration.
         */

        subject device : StockTickerDevice;

        attribute actualStorage : Real {
            doc /* MB */
        }
        attribute minStorage : Real := 256.0 {
            doc /* MB */
        }

        require constraint { actualStorage >= minStorage }
    }

    requirement def <'STOR-2'> StockListCapacityRequirement {
        doc /*
         * The device shall support storing at least 20 stock symbols in the preloaded list.
         */

        subject device : StockTickerDevice;

        attribute maxStocks : Integer;
        attribute minCapacity : Integer := 20;

        require constraint { maxStocks >= minCapacity }
    }

    // --- Top-Level System Requirements ---

    requirement <'SYS-1'> systemRequirements : StockTickerSystemRequirements {
        doc /* Top-level system requirements for the Stock Ticker Device */

        subject stockTicker;

        /* Functional requirements */
        requirement displayPrice : DisplayStockPriceRequirement {
            subject redefines stockTicker;
        }

        requirement refreshInterval : RefreshIntervalRequirement {
            subject redefines stockTicker;
            :>> requiredInterval = 5.0;
        }

        requirement preloadedStocks : PreloadedStocksRequirement {
            subject redefines stockTicker;
            :>> minStocks = 5;
        }

        requirement buttonNav : ButtonNavigationRequirement {
            subject redefines stockTicker;
        }

        requirement wifi : WiFiConnectivityRequirement {
            subject redefines stockTicker;
        }

        requirement restAPI : RESTAPIRequirement {
            subject redefines stockTicker;
        }

        requirement powerControl : PowerControlRequirement {
            subject redefines stockTicker;
        }

        /* Performance requirements */
        requirement displayLatency : DisplayUpdateLatencyRequirement {
            subject redefines stockTicker;
        }

        requirement apiResponse : APIResponseTimeRequirement {
            subject redefines stockTicker;
        }

        requirement buttonResponse : ButtonResponseRequirement {
            subject redefines stockTicker;
        }

        /* Interface requirements */
        requirement displaySize : DisplaySizeRequirement {
            subject redefines stockTicker;
            :>> minSize = 3.0;
        }

        requirement buttonCount : ButtonCountRequirement {
            subject redefines stockTicker;
        }

        requirement standAngle : StandStabilityRequirement {
            subject redefines stockTicker;
        }

        /* Connectivity requirements */
        requirement wifiStandard : WiFiStandardRequirement {
            subject redefines stockTicker;
        }

        requirement wifiRange : WiFiRangeRequirement {
            subject redefines stockTicker;
        }

        requirement networkSecurity : NetworkSecurityRequirement {
            subject redefines stockTicker;
        }

        /* Power requirements */
        requirement powerVoltage : PowerVoltageRequirement {
            subject redefines stockTicker;
        }

        requirement powerConsumption : PowerConsumptionRequirement {
            subject redefines stockTicker;
        }

        /* Reliability requirements */
        requirement networkRetry : NetworkRetryRequirement {
            subject redefines stockTicker;
        }

        requirement dataValidation : DataValidationRequirement {
            subject redefines stockTicker;
        }

        /* Storage requirements */
        requirement minStorage : MinimumStorageRequirement {
            subject redefines stockTicker;
        }

        requirement stockCapacity : StockListCapacityRequirement {
            subject redefines stockTicker;
        }
    }

    requirement def StockTickerSystemRequirements {
        doc /* Container for all stock ticker system requirements */

        subject device : StockTickerDevice;
    }

    // ========================================
    // VERIFICATION CASES
    // ========================================

    verification def RefreshRateVerification {
        doc /*
         * Verify 5-second refresh rate requirement
         * Test measures actual time between display updates
         */

        subject device : StockTickerDevice;

        /* Test setup */
        action setupTest {
            doc /* Connect device to test WiFi network with mock API server */
        }

        /* Test execution */
        action measureRefreshInterval {
            doc /* Record timestamps of display updates over 5 minutes */
            out timestamps[*] : Real;
        }

        action calculateAverageInterval {
            in timestamps[*] : Real;
            out averageInterval : Real;
        }

        constraint {
            calculateAverageInterval.averageInterval <= 5.0
        }
    }

    verification def ButtonResponseVerification {
        doc /*
         * Verify button press response time < 100ms
         * Uses oscilloscope or high-speed camera
         */

        subject device : StockTickerDevice;

        action setupMeasurement {
            doc /* Connect oscilloscope to button GPIO and display update signal */
        }

        action pressButton {
            in buttonToPress : ButtonType;
        }

        action measureResponseTime {
            doc /* Measure time from button press to visible display change */
            out responseTime : Real {
                doc /* milliseconds */
            }
        }

        constraint {
            measureResponseTime.responseTime <= 100.0
        }
    }

    verification def PowerConsumptionVerification {
        doc /*
         * Verify power consumption < 10W during normal operation
         * Uses USB power meter or benchtop power supply with metering
         */

        subject device : StockTickerDevice;

        action connectPowerMeter {
            doc /* Use USB power meter (e.g., USB-C power monitor) */
        }

        action runNormalOperation {
            doc /* Let device run through normal fetch/display cycle for 10 minutes */
        }

        action measurePower {
            out voltage : Real;
            out currentDraw : Real;
            out powerWatts : Real;
        }

        constraint {
            measurePower.powerWatts <= 10.0
        }
    }

    verification def WiFiConnectivityVerification {
        doc /*
         * Verify device can connect to WiFi network and maintain connection
         */

        subject device : StockTickerDevice;

        action configureTestNetwork {
            doc /* Set up test WiFi access point with known credentials */
        }

        action attemptConnection {
            doc /* Power on device and monitor connection attempt */
            out connectionEstablished : Boolean;
            out connectionTime : Real {
                doc /* seconds to establish connection */
            }
        }

        action monitorStability {
            doc /* Monitor connection for 1 hour */
            out disconnectionCount : Integer;
        }

        constraint {
            monitorStability.disconnectionCount == 0
        }
    }

    verification def DisplayReadabilityVerification {
        doc /*
         * Verify display is readable from 1 meter distance
         * Subjective test with multiple users
         */

        subject device : StockTickerDevice;

        action setupViewingDistance {
            doc /* Position device at viewing angle on stand, measure 1m distance */
        }

        action conductUserTest {
            in testSubjects[*] : Person;
            doc /* Ask users to read stock price from 1 meter */
            out successRate : Real {
                doc /* Percentage of users who can read correctly */
            }
        }

        constraint {
            conductUserTest.successRate >= 90.0
        }
    }

    verification def ErrorRecoveryVerification {
        doc /*
         * Verify device handles network errors gracefully
         */

        subject device : StockTickerDevice;

        action simulateNetworkError {
            doc /* Disconnect WiFi or block API access */
        }

        action observeBehavior {
            doc /* Verify device shows error message and retries */
            out errorDisplayed : Boolean;
            out retryAttempted : Boolean;
            out systemHangs : Boolean;
        }

        constraint {
            observeBehavior.errorDisplayed == true and
            observeBehavior.retryAttempted == true and
            observeBehavior.systemHangs == false
        }
    }

    verification def DataValidationVerification {
        doc /*
         * Verify device validates received data before display
         */

        subject device : StockTickerDevice;

        action sendInvalidData {
            doc /* Use mock API to send malformed JSON or invalid values */
        }

        action observeResponse {
            out displaysInvalidData : Boolean;
            out displaysError : Boolean;
            out displaysLastGoodValue : Boolean;
        }

        constraint {
            observeResponse.displaysInvalidData == false and
            (observeResponse.displaysError == true or observeResponse.displaysLastGoodValue == true)
        }
    }
}

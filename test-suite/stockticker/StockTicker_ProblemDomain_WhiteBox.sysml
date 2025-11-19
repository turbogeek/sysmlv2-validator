package StockTicker_ProblemDomain_WhiteBox {
    doc /* Problem Domain - White Box Perspective */

    private import SysML::*;
    private import ScalarValues::*;

    // ========================================================================
    // WHITE BOX PERSPECTIVE - Internal View
    // ========================================================================

    package '2 White Box' {
        doc /*
        White Box Perspective: Functional decomposition of the system

        Focuses on:
        - What functions the system must perform (Functional Analysis)
        - How to group functions into subsystems (Conceptual Subsystems)
        - Which subsystem performs which function (Function Allocation)
        - Effectiveness measures for subsystems (MoEs for Subsystems)
        - Traceability back to stakeholder needs
        */

        private import SysML::*;
        private import ScalarValues::*;

        // ====================================================================
        // CELL 2.1 - FUNCTIONAL ANALYSIS (Behavior Pillar)
        // ====================================================================

        package '1 Functional Analysis' {
            doc /*
            Functional Analysis - Decomposition of system behavior into functions

            Top-Level System Function: Provide Real-Time Stock Information

            Major Functions:
            1. Acquire Stock Data
            2. Process User Input
            3. Update Display
            4. Manage Connectivity
            5. Manage Power
            */

            private import SysML::*;
            private import ScalarValues::*;

            // Data type definitions for function inputs/outputs
            attribute def StockSymbol {
                attribute symbol : String;
            }

            attribute def StockDataRecord {
                attribute symbol : String;
                attribute price : Real;
                attribute change : Real;
                attribute percentChange : Real;
                attribute timestamp : String;
                attribute marketStatus : String;
            }

            attribute def ButtonEvent {
                attribute buttonType : String;   // Next, Previous, Select, Power
                attribute pressType : String;     // Short, Long, Double
                attribute timestamp : String;
            }

            attribute def UserCommand {
                attribute commandType : String;  // NextStock, PrevStock, ToggleFavorite, EnterSleep, etc.
            }

            attribute def DisplayContent {
                attribute layout : String;
                attribute dataElements : String;
            }

            attribute def ConnectionState {
                attribute status : String;  // Disconnected, Scanning, Connecting, Connected
                attribute ssid : String;
                attribute signalStrength : Real;
            }

            attribute def PowerState {
                attribute mode : String;  // Active, LowPower, Sleep, DeepSleep
                attribute batteryLevel : Real;
                attribute estimatedRuntime : Real;
            }

            attribute def HTTPResponse {
                attribute statusCode : Integer;
                attribute headers : String;
                attribute body : String;
            }

            // =====================
            // TOP-LEVEL SYSTEM FUNCTION
            // =====================

            action def 'Provide Real-Time Stock Information' {
                doc /*
                Root function: Continuously provide up-to-date stock price
                information to the user through a portable display device.

                This top-level function decomposes into 5 major functions.
                */

                // Sub-functions (major functional decomposition)
                action acquireStockData;
                action processUserInput;
                action updateDisplay;
                action manageConnectivity;
                action managePower;

                // These functions operate concurrently (parallel composition)
                fork;
                then acquireStockData;
                then processUserInput;
                then manageConnectivity;
                then managePower;
                join;

                // Display updates based on data availability
                then updateDisplay;
            }

            // =====================
            // FUNCTION 1: ACQUIRE STOCK DATA
            // =====================

            action def AcquireStockData {
                doc /*
                Obtain real-time stock price data from external API

                Inputs: Stock symbol
                Outputs: Stock data record
                Frequency: On-demand and periodic (every 5-15 seconds)

                Decomposes into:
                - Build API request
                - Send HTTP request
                - Receive response
                - Parse JSON data
                - Validate data
                */

                in stockSymbol : StockSymbol;
                out stockData : StockDataRecord;

                // Sub-functions
                action buildAPIRequest {
                    doc /* Construct HTTP GET request URL with stock symbol and API key */
                    in symbol : StockSymbol;
                    out requestURL : String;
                }

                action sendHTTPRequest {
                    doc /* Transmit HTTP request over network connection */
                    in url : String;
                    out response : HTTPResponse;
                }

                action receiveResponse {
                    doc /* Wait for and receive HTTP response from server */
                    in httpConnection : ConnectionState;
                    out response : HTTPResponse;
                }

                action parseJSON {
                    doc /* Extract stock data fields from JSON response */
                    in jsonResponse : String;
                    out parsedData : StockDataRecord;
                }

                action validateData {
                    doc /* Verify data completeness and reasonableness */
                    in data : StockDataRecord;
                    out validatedData : StockDataRecord;
                    out isValid : Boolean;
                }

                action handleAPIError {
                    doc /* Respond to HTTP errors or invalid responses */
                    in errorResponse : HTTPResponse;
                    out errorMessage : String;
                }

                // Control flow
                first start;
                then buildAPIRequest;
                then sendHTTPRequest;
                then receiveResponse;

                // Branch on response validity (HTTP 200 vs error)
                then parseJSON;
                then validateData;
                // On validation success, flow completes
                // On error, handleAPIError is invoked
                then done;

                action handleAPIError {
                    doc /* Handle API errors and retry logic */
                }
                then done;
            }

            // =====================
            // FUNCTION 2: PROCESS USER INPUT
            // =====================

            action def ProcessUserInput {
                doc /*
                Interpret physical button presses and generate commands

                Inputs: Button events (hardware interrupts)
                Outputs: User commands
                Frequency: Event-driven (on button press)

                Decomposes into:
                - Detect button press
                - Debounce button
                - Determine press type (short/long/double)
                - Map button to command
                - Execute command
                */

                in buttonPress : ButtonEvent;
                out command : UserCommand;

                action detectButtonPress {
                    doc /* Hardware interrupt handler for button GPIO pins */
                    out rawButtonEvent : ButtonEvent;
                }

                action debounceButton {
                    doc /* Filter out mechanical bounce noise */
                    in rawEvent : ButtonEvent;
                    out cleanEvent : ButtonEvent;
                }

                action determinePressType {
                    doc /*
                    Analyze timing to classify press:
                    - Short: < 500ms
                    - Long: > 1000ms
                    - Double: Two short presses within 500ms
                    */
                    in buttonEvent : ButtonEvent;
                    out pressType : String;
                }

                action mapButtonToCommand {
                    doc /*
                    Map button + press type to user command:
                    - Next + Short → NextStock
                    - Previous + Short → PrevStock
                    - Select + Short → ToggleFavorite
                    - Select + Long → EnterConfigMode
                    - Power + Long → EnterSleepMode
                    */
                    in button : String;
                    in pressType : String;
                    out command : UserCommand;
                }

                action routeCommand {
                    doc /* Send command to appropriate subsystem */
                    in command : UserCommand;
                }

                // Control flow
                first detectButtonPress;
                then debounceButton;
                then determinePressType;
                then mapButtonToCommand;
                then routeCommand;
                then done;
            }

            // =====================
            // FUNCTION 3: UPDATE DISPLAY
            // =====================

            action def UpdateDisplay {
                doc /*
                Render stock information and status on LCD screen

                Inputs: Stock data, system status
                Outputs: Visual display
                Frequency: On data change, periodic refresh

                Decomposes into:
                - Clear display buffer
                - Draw background
                - Render stock symbol
                - Render price
                - Render change indicator
                - Render timestamp
                - Update backlight
                - Transfer buffer to display
                */

                in displayData : StockDataRecord;
                in systemStatus : String;
                out visualOutput : DisplayContent;

                action clearDisplayBuffer {
                    doc /* Reset framebuffer to blank state */
                }

                action drawBackground {
                    doc /* Draw background color/pattern */
                }

                action renderStockSymbol {
                    doc /* Draw stock symbol in large font at top */
                    in symbol : String;
                }

                action renderPrice {
                    doc /* Draw current price in large font (center) */
                    in price : Real;
                }

                action renderChangeIndicator {
                    doc /*
                    Draw price change with color coding:
                    - Green if positive
                    - Red if negative
                    - Gray if unchanged
                    */
                    in change : Real;
                    in percentChange : Real;
                }

                action renderTimestamp {
                    doc /* Draw last update time in small font (bottom) */
                    in timestamp : String;
                }

                action renderStatusIndicators {
                    doc /* Draw WiFi status, battery level icons */
                    in connectionState : ConnectionState;
                    in powerState : PowerState;
                }

                action adjustBacklight {
                    doc /* Set backlight brightness based on ambient light */
                    in ambientLight : Real;
                    out brightness : Real;
                }

                action transferBufferToDisplay {
                    doc /* Send framebuffer data to display via SPI */
                }

                // Control flow
                first clearDisplayBuffer;
                then drawBackground;
                then fork;
                    action renderStockSymbol;
                    action renderPrice;
                    action renderChangeIndicator;
                    action renderTimestamp;
                    action renderStatusIndicators;
                then join;
                then adjustBacklight;
                then transferBufferToDisplay;
                then done;
            }

            // =====================
            // FUNCTION 4: MANAGE CONNECTIVITY
            // =====================

            action def ManageConnectivity {
                doc /*
                Establish and maintain WiFi network connection

                Inputs: Network credentials, connection state
                Outputs: Active network connection
                Frequency: Continuous monitoring, event-driven reconnection

                Decomposes into:
                - Scan for networks
                - Select network
                - Authenticate
                - Maintain connection
                - Monitor signal strength
                - Handle disconnection
                - Retry connection
                */

                in networkSSID : String;
                in networkPassword : String;
                out connectionState : ConnectionState;

                action scanForNetworks {
                    doc /* Discover available WiFi access points */
                    out availableNetworks : String;
                }

                action selectNetwork {
                    doc /* Choose configured network from scan results */
                    in availableNetworks : String;
                    in configuredSSID : String;
                    out selectedNetwork : String;
                }

                action authenticate {
                    doc /* Complete WPA2/WPA3 authentication handshake */
                    in ssid : String;
                    in password : String;
                    out authResult : Boolean;
                }

                action obtainIPAddress {
                    doc /* Request IP address via DHCP */
                    out ipAddress : String;
                }

                action monitorConnection {
                    doc /* Continuously check connection status */
                    out isConnected : Boolean;
                    out signalStrength : Real;
                }

                action detectDisconnection {
                    doc /* Identify when connection is lost */
                    in connectionState : ConnectionState;
                    out disconnected : Boolean;
                }

                action retryConnection {
                    doc /* Attempt to re-establish connection with exponential backoff */
                    in attemptNumber : Integer;
                    out retryDelay : Real;  // seconds
                }

                // Control flow
                first scanForNetworks;
                then selectNetwork;
                then authenticate;

                // On success: obtain IP and monitor
                // On failure: retry connection
                then obtainIPAddress;
                then monitorConnection;

                // Connection monitoring continues until disconnect
                // On disconnect, retryConnection is invoked
                then done;

                action retryConnection {
                    doc /* Retry WiFi connection after failure or disconnect */
                }
                then scanForNetworks;  // Retry from beginning
            }

            // =====================
            // FUNCTION 5: MANAGE POWER
            // =====================

            action def ManagePower {
                doc /*
                Control power consumption and battery management

                Inputs: User activity, battery level
                Outputs: Power mode, estimated runtime
                Frequency: Continuous monitoring

                Decomposes into:
                - Monitor battery level
                - Detect user activity
                - Determine power mode
                - Transition to sleep mode
                - Wake from sleep mode
                - Control peripheral power
                */

                in batteryVoltage : Real;
                in lastUserActivity : String;  // timestamp
                out powerMode : PowerState;

                action monitorBattery {
                    doc /* Read battery voltage via ADC */
                    out voltage : Real;
                    out percentRemaining : Real;
                }

                action calculateRuntime {
                    doc /* Estimate remaining runtime based on current consumption */
                    in batteryPercent : Real;
                    in currentDraw : Real;
                    out estimatedMinutes : Real;
                }

                action detectInactivity {
                    doc /* Check time since last button press */
                    in lastActivity : String;
                    out inactiveDuration : Real;  // seconds
                }

                action determinePowerMode {
                    doc /*
                    Select power mode based on activity:
                    - Active: Normal operation
                    - LowPower: Dimmed display, reduced refresh
                    - Sleep: Display off, periodic wake
                    - DeepSleep: All peripherals off, wake on button only
                    */
                    in inactiveDuration : Real;
                    in batteryLevel : Real;
                    out mode : String;
                }

                action enterSleepMode {
                    doc /* Transition to low-power state */
                    action saveCurrentState;
                    action disablePeripherals;
                    action configurewakeSource;
                    action enterLowPowerState;
                }

                action wakeFromSleep {
                    doc /* Restore from low-power state */
                    action exitLowPowerState;
                    action enablePeripherals;
                    action restoreState;
                }

                action controlPeripheralPower {
                    doc /*
                    Enable/disable power to:
                    - Display backlight
                    - WiFi module
                    - Unused GPIO pins
                    */
                    in mode : String;
                }

                // Control flow
                first monitorBattery;
                then calculateRuntime;
                then fork;
                    action detectInactivity;
                    action monitorBattery;  // Continuous monitoring
                then join;
                then determinePowerMode;

                // Route to appropriate power control based on mode
                // Active/LowPower: controlPeripheralPower
                // Sleep/DeepSleep: enterSleepMode
                then controlPeripheralPower;
                then done;

                action enterSleepMode {
                    doc /*
                    Enter sleep mode to conserve power.
                    Wake triggered by external event (button press)
                    */
                }
                then wakeFromSleep;
                then determinePowerMode;  // Re-evaluate after wake
            }
        }

        // ====================================================================
        // CELL 2.2 - CONCEPTUAL SUBSYSTEMS (Structure Pillar)
        // ====================================================================

        package '2 Conceptual Subsystems' {
            doc /*
            Conceptual Subsystems - Logical grouping of related functions

            Subsystems:
            1. Stock Data Processor - Data acquisition and processing
            2. Display Controller - Visual rendering
            3. Network Manager - WiFi connectivity
            4. User Input Handler - Button input processing
            5. Power Manager - Power and battery management
            */

            private import SysML::*;
            private import ScalarValues::*;
            private import '1 Functional Analysis'::*;

            // =====================
            // CONCEPTUAL SUBSYSTEM BASE
            // =====================

            part def ConceptualSubsystem {
                doc /* Base definition for all conceptual subsystems */
            }

            // =====================
            // SUBSYSTEM 1: STOCK DATA PROCESSOR
            // =====================

            part def StockDataProcessor :> ConceptualSubsystem {
                doc /*
                Responsible for acquiring and processing stock market data

                Allocated Functions:
                - Acquire Stock Data (build request, send HTTP, parse JSON, validate)

                Conceptual Interfaces:
                - API Port: External connection to stock data provider
                - Data Output Port: Processed stock data to display controller
                - Command Input Port: Commands from user input handler

                Key Responsibilities:
                - Build HTTP requests with stock symbols
                - Communicate with REST API
                - Parse JSON responses
                - Validate data integrity
                - Cache recent queries
                - Handle API errors
                */

                // Allocated functions (references)
                ref action acquireStockData : '1 Functional Analysis'::AcquireStockData;

                // Conceptual interfaces
                port apiPort : APIInterface {
                    doc /* Connection to external stock data service */
                }

                port dataOutputPort : StockDataInterface {
                    doc /* Provides processed stock data to other subsystems */
                }

                port commandInputPort : CommandInterface {
                    doc /* Receives commands (NextStock, PrevStock, etc.) */
                }

                // Internal state (conceptual)
                attribute currentSymbol : String;
                attribute watchlist : String[0..20];  // Up to 20 stocks
                attribute cachedData : '1 Functional Analysis'::StockDataRecord[0..10];  // Cache last 10 queries
                attribute refreshInterval : Real = 5.0;  // seconds
            }

            // =====================
            // SUBSYSTEM 2: DISPLAY CONTROLLER
            // =====================

            part def DisplayController :> ConceptualSubsystem {
                doc /*
                Responsible for rendering visual information on screen

                Allocated Functions:
                - Update Display (clear, draw, render, adjust backlight)

                Conceptual Interfaces:
                - Data Input Port: Receives stock data to display
                - Display Hardware Port: Controls physical LCD
                - Power Control Port: Backlight and display power

                Key Responsibilities:
                - Manage framebuffer
                - Render text and graphics
                - Control backlight brightness
                - Update screen at appropriate refresh rate
                - Display system status icons
                */

                ref action updateDisplay : '1 Functional Analysis'::UpdateDisplay;

                port dataInputPort : StockDataInterface;
                port displayHardwarePort : DisplayHardwareInterface;
                port powerControlPort : PowerControlInterface;

                attribute brightness : Real = 80.0;  // Percent
                attribute refreshRate : Real = 2.0;   // Hz
                attribute currentScreen : String = "StockView";
            }

            // =====================
            // SUBSYSTEM 3: NETWORK MANAGER
            // =====================

            part def NetworkManager :> ConceptualSubsystem {
                doc /*
                Responsible for establishing and maintaining WiFi connectivity

                Allocated Functions:
                - Manage Connectivity (scan, authenticate, monitor, retry)

                Conceptual Interfaces:
                - Network Interface Port: Physical WiFi radio
                - Status Output Port: Connection status to other subsystems

                Key Responsibilities:
                - Scan for WiFi networks
                - Authenticate to access point
                - Monitor connection health
                - Detect and recover from disconnection
                - Implement retry with exponential backoff
                */

                ref action manageConnectivity : '1 Functional Analysis'::ManageConnectivity;

                port networkInterfacePort : WiFiHardwareInterface;
                port statusOutputPort : ConnectionStatusInterface;

                attribute ssid : String;
                attribute password : String;
                attribute connectionState : String = "Disconnected";
                attribute signalStrength : Real = 0.0;  // dBm
                attribute ipAddress : String;
                attribute retryAttempts : Integer = 0;
            }

            // =====================
            // SUBSYSTEM 4: USER INPUT HANDLER
            // =====================

            part def UserInputHandler :> ConceptualSubsystem {
                doc /*
                Responsible for processing user button interactions

                Allocated Functions:
                - Process User Input (detect, debounce, classify, route)

                Conceptual Interfaces:
                - Button Input Port: Physical button connections
                - Command Output Port: User commands to other subsystems

                Key Responsibilities:
                - Detect button press events
                - Debounce mechanical switches
                - Classify press types (short, long, double)
                - Map buttons to commands
                - Route commands to appropriate subsystem
                */

                ref action processUserInput : '1 Functional Analysis'::ProcessUserInput;

                port buttonInputPort : ButtonHardwareInterface;
                port commandOutputPort : CommandInterface;

                attribute lastButtonPress : String;  // timestamp
                attribute currentMode : String = "Normal";  // Normal, Config
                attribute pressHistory : String[0..5];  // For double-press detection
            }

            // =====================
            // SUBSYSTEM 5: POWER MANAGER
            // =====================

            part def PowerManager :> ConceptualSubsystem {
                doc /*
                Responsible for power consumption management and battery monitoring

                Allocated Functions:
                - Manage Power (monitor battery, control modes, sleep/wake)

                Conceptual Interfaces:
                - Battery Monitor Port: ADC connection to battery
                - Power Control Port: Control power to peripherals
                - Activity Input Port: User activity notifications

                Key Responsibilities:
                - Monitor battery voltage and estimate remaining capacity
                - Calculate estimated runtime
                - Detect user inactivity
                - Transition between power modes
                - Control peripheral power states
                - Manage sleep/wake cycles
                */

                ref action managePower : '1 Functional Analysis'::ManagePower;

                port batteryMonitorPort : BatteryInterface;
                port powerControlPort : PowerControlInterface[0..5];  // To multiple subsystems
                port activityInputPort : ActivityNotificationInterface;

                attribute batteryVoltage : Real;
                attribute batteryPercent : Real;
                attribute powerMode : String = "Active";
                attribute estimatedRuntime : Real;  // minutes
                attribute inactivityTimeout : Real = 120.0;  // seconds (2 min)
            }

            // =====================
            // CONCEPTUAL INTERFACE DEFINITIONS
            // =====================

            interface def APIInterface {
                doc /* HTTP/HTTPS connection to stock data provider */
            }

            interface def StockDataInterface {
                doc /* Transfer of stock price data between subsystems */
            }

            interface def CommandInterface {
                doc /* User commands (NextStock, EnterSleep, etc.) */
            }

            interface def DisplayHardwareInterface {
                doc /* SPI connection to LCD display */
            }

            interface def PowerControlInterface {
                doc /* Enable/disable power to components */
            }

            interface def WiFiHardwareInterface {
                doc /* WiFi radio hardware interface */
            }

            interface def ConnectionStatusInterface {
                doc /* Network connection status notifications */
            }

            interface def ButtonHardwareInterface {
                doc /* GPIO connections to physical buttons */
            }

            interface def BatteryInterface {
                doc /* ADC connection for battery voltage monitoring */
            }

            interface def ActivityNotificationInterface {
                doc /* Notifications of user activity for power management */
            }

            // =====================
            // CONCEPTUAL SYSTEM COMPOSITION
            // =====================

            part 'Conceptual Stock Ticker System' {
                doc /*
                Complete conceptual architecture showing subsystems and connections

                This represents the logical decomposition before committing to
                hardware/software allocation or physical implementation.
                */

                part dataProcessor : StockDataProcessor;
                part displayController : DisplayController;
                part networkManager : NetworkManager;
                part inputHandler : UserInputHandler;
                part powerManager : PowerManager;

                // Conceptual connections
                connect dataProcessor.dataOutputPort to displayController.dataInputPort;
                connect inputHandler.commandOutputPort to dataProcessor.commandInputPort;
                connect networkManager.networkInterfacePort to dataProcessor.apiPort;
                connect powerManager.powerControlPort to displayController.powerControlPort;
                connect powerManager.powerControlPort to networkManager;
            }
        }

        // ====================================================================
        // CELL 2.3 - FUNCTION ALLOCATION
        // ====================================================================

        package '3 Function Allocation' {
            doc /*
            Function Allocation - Explicit mapping of functions to subsystems

            Allocation Matrix:
            - Acquire Stock Data → Stock Data Processor
            - Process User Input → User Input Handler
            - Update Display → Display Controller
            - Manage Connectivity → Network Manager
            - Manage Power → Power Manager
            */

            private import SysML::*;
            private import ScalarValues::*;
            private import '1 Functional Analysis'::*;
            private import '2 Conceptual Subsystems'::*;

            // Allocation definition
            allocation def FunctionToSubsystemAllocation {
                doc /* Maps a function (action) to a subsystem (part) */
                end source;
                end target;
            }

            // Specific allocations using allocate statements
            doc /*
            Function-to-Subsystem Allocation Traceability:

            The following allocations map functional requirements to conceptual subsystems:
            - Acquire Stock Data function → Stock Data Processor subsystem
            - Process User Input function → User Input Handler subsystem
            - Update Display function → Display Controller subsystem
            - Manage Connectivity function → Network Manager subsystem
            - Manage Power function → Power Manager subsystem

            These allocations are realized through the ref action features within each
            part definition (e.g., StockDataProcessor.acquireStockData : AcquireStockData)
            */
        }

        // ====================================================================
        // CELL 2.4 - MoEs FOR SUBSYSTEMS (Parameters Pillar)
        // ====================================================================

        package '4 MoEs for Subsystems' {
            doc /*
            Measures of Effectiveness for Conceptual Subsystems

            Each subsystem has performance targets that contribute to
            system-level MoEs.
            */

            private import SysML::*;
            private import ScalarValues::*;

            attribute def SubsystemMoE {
                attribute name : String;
                attribute targetValue : Real;
                attribute threshold : Real;
                attribute unit : String;
            }

            // Stock Data Processor MoEs
            attribute dataProcessorLatency : SubsystemMoE {
                doc /* Time to complete API request and parsing */
                attribute :>> name = "Data Processor Latency";
                attribute :>> targetValue = 1.5;
                attribute :>> threshold = 3.0;
                attribute :>> unit = "seconds";
            }

            attribute dataValidationAccuracy : SubsystemMoE {
                doc /* Percentage of malformed responses caught */
                attribute :>> name = "Data Validation Accuracy";
                attribute :>> targetValue = 99.9;
                attribute :>> threshold = 95.0;
                attribute :>> unit = "percent";
            }

            // Display Controller MoEs
            attribute displayRefreshRate : SubsystemMoE {
                doc /* Screen update frequency */
                attribute :>> name = "Display Refresh Rate";
                attribute :>> targetValue = 10.0;
                attribute :>> threshold = 5.0;
                attribute :>> unit = "Hz";
            }

            attribute displayRenderTime : SubsystemMoE {
                doc /* Time to render complete screen */
                attribute :>> name = "Display Render Time";
                attribute :>> targetValue = 0.3;
                attribute :>> threshold = 1.0;
                attribute :>> unit = "seconds";
            }

            // Network Manager MoEs
            attribute networkReconnectTime : SubsystemMoE {
                doc /* Time to re-establish lost connection */
                attribute :>> name = "Network Reconnect Time";
                attribute :>> targetValue = 5.0;
                attribute :>> threshold = 15.0;
                attribute :>> unit = "seconds";
            }

            attribute connectionStability : SubsystemMoE {
                doc /* Percentage of time connected (uptime) */
                attribute :>> name = "Connection Stability";
                attribute :>> targetValue = 99.0;
                attribute :>> threshold = 95.0;
                attribute :>> unit = "percent";
            }

            // User Input Handler MoEs
            attribute buttonResponseTime : SubsystemMoE {
                doc /* Latency from button press to command */
                attribute :>> name = "Button Response Time";
                attribute :>> targetValue = 0.1;
                attribute :>> threshold = 0.3;
                attribute :>> unit = "seconds";
            }

            attribute inputAccuracy : SubsystemMoE {
                doc /* Correct classification of press types */
                attribute :>> name = "Input Classification Accuracy";
                attribute :>> targetValue = 99.5;
                attribute :>> threshold = 95.0;
                attribute :>> unit = "percent";
            }

            // Power Manager MoEs
            attribute powerEfficiency : SubsystemMoE {
                doc /* Percentage of battery capacity utilized effectively */
                attribute :>> name = "Power Efficiency";
                attribute :>> targetValue = 90.0;
                attribute :>> threshold = 75.0;
                attribute :>> unit = "percent";
            }

            attribute sleepTransitionTime : SubsystemMoE {
                doc /* Time to enter sleep mode */
                attribute :>> name = "Sleep Transition Time";
                attribute :>> targetValue = 2.0;
                attribute :>> threshold = 5.0;
                attribute :>> unit = "seconds";
            }

            attribute wakeLatency : SubsystemMoE {
                doc /* Time from button press to full wake */
                attribute :>> name = "Wake Latency";
                attribute :>> targetValue = 1.0;
                attribute :>> threshold = 3.0;
                attribute :>> unit = "seconds";
            }
        }

        // ====================================================================
        // CELL 2.5 - TRACEABILITY TO STAKEHOLDER NEEDS
        // ====================================================================

        package '5 Traceability to Stakeholder Needs' {
            doc /*
            Traceability Matrix: White Box Elements → Stakeholder Needs

            Documents how functions and conceptual subsystems address
            stakeholder needs captured in the Black Box perspective.
            */

            private import SysML::*;
            private import ScalarValues::*;

            doc /*
            TRACEABILITY NOTES
            ==================

            Example traceability relationships
            (In practice, these would be modeled as explicit trace relationships)

            FUNCTION TO STAKEHOLDER NEED TRACEABILITY:

            Acquire Stock Data Function addresses:
            - SN-001: See current stock prices
            - SN-002: Track multiple stocks
            - SN-004: Know last update time
            - SN-006: Work anywhere with WiFi
            - SN-008: Quick updates (performance)

            Process User Input Function addresses:
            - SN-003: Switch stocks quickly
            - SN-007: Configure stock list
            - SN-011: Simple button controls

            Update Display Function addresses:
            - SN-001: See current prices
            - SN-005: See price change indicator
            - SN-009: Readable display
            - SN-012: Compact but readable
            - SN-013: Connectivity status indication

            Manage Connectivity Function addresses:
            - SN-006: Work with WiFi
            - SN-014: Auto-reconnect
            - SN-015: Use home/office WiFi
            - SN-018: Handle network errors

            Manage Power Function addresses:
            - SN-010: Full workday battery life
            - SN-016: USB recharge
            - SN-017: Sleep mode when inactive

            SUBSYSTEM TO STAKEHOLDER NEED TRACEABILITY:

            Stock Data Processor addresses:
            - SN-001, SN-002, SN-004, SN-006, SN-008, SN-018, SN-020

            Display Controller addresses:
            - SN-001, SN-005, SN-009, SN-012, SN-013, SN-019

            Network Manager addresses:
            - SN-006, SN-014, SN-015, SN-018, SN-021

            User Input Handler addresses:
            - SN-003, SN-007, SN-011

            Power Manager addresses:
            - SN-010, SN-016, SN-017
            */
        }
    }
}

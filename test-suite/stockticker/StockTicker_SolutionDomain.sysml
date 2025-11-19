package StockTicker_SolutionDomain {
    doc /*
    Solution Domain - How the system meets stakeholder needs

    This domain progresses from problem definition to solution design across
    three levels:
    - System Level: Overall logical architecture and requirements
    - Subsystem Level: Detailed subsystem design
    - Configuration Level: Product variants and configurations
    */

    private import SysML::*;
    private import ScalarValues::*;
    private import StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'1 Stakeholder Needs'::*;
    private import StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'2 Conceptual Subsystems'::*;

    // ========================================================================
    // SYSTEM LEVEL - Logical Architecture
    // ========================================================================

    package '1 System Level' {
        doc /*
        System Level: Logical solution architecture

        Defines:
        - System Requirements (derived from stakeholder needs)
        - Logical Subsystem Architecture (refined from conceptual)
        - System Behavior Models (state machines, detailed actions)
        - System Parameters & Budgets (allocated to subsystems)
        - System Verification (test cases and analyses)
        */

        private import SysML::*;
        private import ScalarValues::*;
        private import StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'1 Stakeholder Needs'::*;
        private import StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'4 Measures of Effectiveness'::*;
        private import StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'1 Functional Analysis'::*;
        private import StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'2 Conceptual Subsystems'::*;

        // ====================================================================
        // CELL 3.1 - SYSTEM REQUIREMENTS (Requirements Pillar)
        // ====================================================================

        package '1 System Requirements' {
            doc /*
            System Requirements - Formal, verifiable requirements

            Derived from stakeholder needs through refinement process.
            Each requirement is:
            - Numbered with category prefix (FR-, PERF-, INT-, etc.)
            - Verifiable (testable or analyzable)
            - Traceable to stakeholder needs
            - Assigned verification method
            */

            private import SysML::*;
    private import ScalarValues::*;

            // System requirement base definition
            requirement def SystemRequirement {
                doc /* Base definition for all system requirements */
                attribute reqId : String;
                attribute category : String;
                attribute priority : String;   // High, Medium, Low
                attribute verificationMethod : String;  // Test, Analysis, Inspection, Demonstration
                attribute rationale : String;
            }

            // =====================
            // FUNCTIONAL REQUIREMENTS
            // =====================

            requirement FR_001 : SystemRequirement {
                doc /* The system shall retrieve stock price data via WiFi connection */
                attribute :>> reqId = "FR-001";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-001, SN-006: Users need current prices via WiFi";

                // Traces to stakeholder needs (refine relationship)
                // refine SN_001;
                // refine SN_006;
            }

            requirement FR_002 : SystemRequirement {
                doc /* The system shall support a watchlist of at least 5 stock symbols */
                attribute :>> reqId = "FR-002";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-002: Track multiple stocks";
            }

            requirement FR_003 : SystemRequirement {
                doc /* The system shall allow user to cycle through watchlist using physical buttons */
                attribute :>> reqId = "FR-003";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Demonstration";
                attribute :>> rationale = "Derived from SN-003, SN-011: Quick switching with simple controls";
            }

            requirement FR_004 : SystemRequirement {
                doc /* The system shall display stock symbol, current price, price change, and last update timestamp */
                attribute :>> reqId = "FR-004";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Inspection";
                attribute :>> rationale = "Derived from SN-001, SN-004, SN-005: Essential price information";
            }

            requirement FR_005 : SystemRequirement {
                doc /* The system shall persist user's watchlist in non-volatile memory */
                attribute :>> reqId = "FR-005";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-020: Remember stock list after power off";
            }

            requirement FR_006 : SystemRequirement {
                doc /* The system shall store WiFi credentials securely in non-volatile memory */
                attribute :>> reqId = "FR-006";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Inspection";
                attribute :>> rationale = "Derived from SN-021: Secure credential storage";
            }

            requirement FR_007 : SystemRequirement {
                doc /* The system shall provide visual indication of network connectivity status */
                attribute :>> reqId = "FR-007";
                attribute :>> category = "Functional";
                attribute :>> priority = "Medium";
                attribute :>> verificationMethod = "Demonstration";
                attribute :>> rationale = "Derived from SN-013: Connectivity status";
            }

            // =====================
            // PERFORMANCE REQUIREMENTS
            // =====================

            requirement PERF_001 : SystemRequirement {
                doc /* The system shall update displayed stock price within 2.0 seconds of user request */
                attribute :>> reqId = "PERF-001";
                attribute :>> category = "Performance";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-008: Quick updates; Linked to updateLatency MoE";

                constraint {
                    doc /* Measured update latency must not exceed target */
                    // updateLatency <= 2.0
                }
            }

            requirement PERF_002 : SystemRequirement {
                doc /* The system shall refresh stock data automatically every 5 to 15 seconds */
                attribute :>> reqId = "PERF-002";
                attribute :>> category = "Performance";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Keeps prices current without excessive API calls";

                constraint {
                    doc /* Refresh interval within acceptable range */
                    // refreshInterval >= 5.0 and refreshInterval <= 15.0
                }
            }

            requirement PERF_003 : SystemRequirement {
                doc /* The system shall render display updates within 0.3 seconds */
                attribute :>> reqId = "PERF-003";
                attribute :>> category = "Performance";
                attribute :>> priority = "Medium";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Linked to displayResponseTime MoE";

                constraint {
                    doc /* Display rendering latency */
                    // displayRenderTime <= 0.3
                }
            }

            // =====================
            // INTERFACE REQUIREMENTS
            // =====================

            requirement INT_001 : SystemRequirement {
                doc /* The system shall provide a color LCD display of at least 1.14 inches diagonal */
                attribute :>> reqId = "INT-001";
                attribute :>> category = "Interface";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Inspection";
                attribute :>> rationale = "Derived from SN-012: Compact but readable display";
            }

            requirement INT_002 : SystemRequirement {
                doc /* The display shall be readable at a distance of at least 30 cm in 500 lux ambient light */
                attribute :>> reqId = "INT-002";
                attribute :>> category = "Interface";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-009: Readable in normal lighting; Linked to displayReadability MoE";
            }

            requirement INT_003 : SystemRequirement {
                doc /* The system shall provide at least 2 physical buttons for user input */
                attribute :>> reqId = "INT-003";
                attribute :>> category = "Interface";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Inspection";
                attribute :>> rationale = "Derived from SN-011: Simple button controls (Next/Previous minimum)";
            }

            // =====================
            // CONNECTIVITY REQUIREMENTS
            // =====================

            requirement CONN_001 : SystemRequirement {
                doc /* The system shall support WiFi 802.11 b/g/n connectivity */
                attribute :>> reqId = "CONN-001";
                attribute :>> category = "Connectivity";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-015: Use home/office WiFi";
            }

            requirement CONN_002 : SystemRequirement {
                doc /* The system shall automatically attempt to reconnect to WiFi network upon connection loss */
                attribute :>> reqId = "CONN-002";
                attribute :>> category = "Connectivity";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-014: Auto-reconnect";
            }

            requirement CONN_003 : SystemRequirement {
                doc /* The system shall re-establish WiFi connection within 15 seconds of connection loss */
                attribute :>> reqId = "CONN-003";
                attribute :>> category = "Connectivity";
                attribute :>> priority = "Medium";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Linked to networkReconnectTime MoE (threshold value)";

                constraint {
                    doc /* Reconnection time constraint */
                    // reconnectTime <= 15.0
                }
            }

            // =====================
            // POWER REQUIREMENTS
            // =====================

            requirement PWR_001 : SystemRequirement {
                doc /* The system shall operate for at least 4 hours on a single battery charge under typical usage */
                attribute :>> reqId = "PWR-001";
                attribute :>> category = "Power";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-010: Full workday battery (threshold: 4 hrs, target: 8 hrs)";

                constraint {
                    doc /* Minimum battery life */
                    // batteryRuntime >= 4.0  // hours
                }
            }

            requirement PWR_002 : SystemRequirement {
                doc /* The system shall support USB charging at 5V */
                attribute :>> reqId = "PWR-002";
                attribute :>> category = "Power";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-016: Standard USB recharge";
            }

            requirement PWR_003 : SystemRequirement {
                doc /* The system shall enter low-power sleep mode after 2 minutes of user inactivity */
                attribute :>> reqId = "PWR-003";
                attribute :>> category = "Power";
                attribute :>> priority = "Medium";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-017: Sleep mode when inactive";
            }

            // =====================
            // RELIABILITY REQUIREMENTS
            // =====================

            requirement REL_001 : SystemRequirement {
                doc /* The system shall handle network errors without crashing or requiring user intervention */
                attribute :>> reqId = "REL-001";
                attribute :>> category = "Reliability";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Derived from SN-018: Graceful error handling";
            }

            requirement REL_002 : SystemRequirement {
                doc /* The system shall display user-friendly error messages for common failure conditions */
                attribute :>> reqId = "REL-002";
                attribute :>> category = "Reliability";
                attribute :>> priority = "Medium";
                attribute :>> verificationMethod = "Inspection";
                attribute :>> rationale = "Derived from SN-019: Explain what went wrong";
            }

            requirement REL_003 : SystemRequirement {
                doc /* The system shall implement watchdog timer to recover from software hangs */
                attribute :>> reqId = "REL-003";
                attribute :>> category = "Reliability";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Analysis";
                attribute :>> rationale = "Mitigation for HAZ-004: Infinite loop hazard";
            }

            // =====================
            // SECURITY REQUIREMENTS
            // =====================

            requirement SEC_001 : SystemRequirement {
                doc /* The system shall use HTTPS/TLS for all communication with stock data provider API */
                attribute :>> reqId = "SEC-001";
                attribute :>> category = "Security";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Inspection";
                attribute :>> rationale = "Mitigation for HAZ-007: Man-in-the-middle attack";
            }

            requirement SEC_002 : SystemRequirement {
                doc /* The system shall validate all API responses for buffer overflow prevention */
                attribute :>> reqId = "SEC-002";
                attribute :>> category = "Security";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Test";
                attribute :>> rationale = "Mitigation for HAZ-006: Buffer overflow hazard";
            }

            // =====================
            // COST/BUSINESS REQUIREMENTS
            // =====================

            requirement BUS_001 : SystemRequirement {
                doc /* The system bill of materials cost shall not exceed $25 for production quantities of 1000+ units */
                attribute :>> reqId = "BUS-001";
                attribute :>> category = "Business";
                attribute :>> priority = "High";
                attribute :>> verificationMethod = "Analysis";
                attribute :>> rationale = "Derived from SN-022, SN-023: Affordability (threshold from MoE)";

                constraint {
                    doc /* Manufacturing cost constraint */
                    // bomCost <= 25.0
                }
            }
        }

        // ====================================================================
        // CELL 3.2 - LOGICAL SUBSYSTEM ARCHITECTURE (Structure Pillar)
        // ====================================================================

        package '2 Logical Subsystem Architecture' {
            doc /*
            Logical Subsystem Architecture (LSA) - Refined system architecture

            Refines the conceptual subsystems into logical architecture with:
            - Detailed port and interface definitions
            - Internal state and attributes
            - Behavior references (state machines, actions)
            - Interface connections
            */

            private import SysML::*;
            private import ScalarValues::*;
            private import StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'2 Conceptual Subsystems'::*;

            // =====================
            // LOGICAL SUBSYSTEM BASE
            // =====================

            part def LogicalSubsystem {
                doc /* Base definition for all logical subsystems */
            }

            // =====================
            // LOGICAL SUBSYSTEM 1: STOCK DATA PROCESSOR
            // =====================

            part def StockDataProcessorLogical :> LogicalSubsystem {
                doc /*
                Logical subsystem for stock data acquisition and processing

                Refines: StockDataProcessor (conceptual subsystem)

                Responsibilities:
                - Build HTTP requests for stock data API
                - Send requests and receive responses
                - Parse JSON responses
                - Validate data integrity
                - Cache recent queries
                - Handle API errors and rate limiting

                Interfaces:
                - apiPort: External HTTP/HTTPS connection to stock data service
                - dataOutputPort: Provides processed stock data to Display Controller
                - commandInputPort: Receives user commands (NextStock, PrevStock, etc.)
                - statusOutputPort: Publishes status to other subsystems
                */

                // Ports (logical interfaces)
                port apiPort : HTTPInterface {
                    doc /* External API communication via HTTPS */
                    attribute protocol : String = "HTTPS";
                    attribute portNumber : Integer = 443;
                }

                port dataOutputPort : StockDataOutputInterface {
                    doc /* Processed stock data to Display Controller */
                }

                port commandInputPort : UserCommandInterface {
                    doc /* Control commands from User Input Handler */
                }

                port statusOutputPort : SubsystemStatusInterface {
                    doc /* Status notifications (data fetching, error, etc.) */
                }

                // State attributes
                attribute currentSymbol : String = "AAPL";  // Default stock
                attribute watchlist : String[1..20] {
                    doc /* User's stock watchlist (1 to 20 symbols) */
                }
                attribute currentIndex : Integer = 0;  // Current position in watchlist
                attribute cachedData : StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'1 Functional Analysis'::StockDataRecord[0..10] {
                    doc /* Cache of last 10 queries */
                }
                attribute refreshInterval : Real = 5.0 {
                    doc /* Auto-refresh interval in seconds */
                }
                attribute apiKey : String {
                    doc /* API authentication key (stored securely) */
                }
                attribute lastUpdateTime : String;
                attribute errorCount : Integer = 0;

                // Behavior reference
                // perform action acquireStockData;
            }

            // =====================
            // LOGICAL SUBSYSTEM 2: DISPLAY CONTROLLER
            // =====================

            part def DisplayControllerLogical :> LogicalSubsystem {
                doc /*
                Logical subsystem for display rendering and management

                Refines: DisplayController (conceptual subsystem)

                Responsibilities:
                - Manage framebuffer
                - Render text and graphics (stock symbol, price, change, timestamp)
                - Control backlight brightness (auto-adjust based on ambient light)
                - Update screen at appropriate refresh rate
                - Display system status icons (WiFi, battery)

                Interfaces:
                - dataInputPort: Receives stock data to render
                - displayHardwarePort: SPI connection to physical LCD
                - powerControlPort: Backlight control and display power
                - statusInputPort: System status for icons
                */

                port dataInputPort : StockDataInputInterface;
                port displayHardwarePort : SPIDisplayInterface {
                    doc /* SPI connection to ST7789 or similar LCD controller */
                    attribute clockSpeed : Real = 40.0 {
                        doc /* SPI clock in MHz */
                    }
                }
                port powerControlPort : DisplayPowerInterface;
                port statusInputPort : SystemStatusInterface;

                // State attributes
                attribute brightness : Real = 80.0 {
                    doc /* Backlight brightness (0-100%) */
                }
                attribute refreshRate : Real = 2.0 {
                    doc /* Screen refresh rate in Hz */
                }
                attribute currentScreen : String = "StockView" {
                    doc /* Current display mode: StockView, ConfigMenu, ErrorScreen */
                }
                attribute framebufferSize : Integer = 32400 {
                    doc /* Bytes: 135 x 240 pixels x RGB565 */
                }
                attribute displayOn : Boolean = true;

                // Behavior reference
                // perform action updateDisplay;
                // perform state DisplayBrightnessStateMachine;
            }

            // =====================
            // LOGICAL SUBSYSTEM 3: NETWORK MANAGER
            // =====================

            part def NetworkManagerLogical :> LogicalSubsystem {
                doc /*
                Logical subsystem for WiFi network management

                Refines: NetworkManager (conceptual subsystem)

                Responsibilities:
                - Scan for WiFi networks
                - Authenticate to configured access point
                - Monitor connection health
                - Detect disconnection
                - Attempt automatic reconnection with exponential backoff
                - Manage connection state machine

                Interfaces:
                - wifiHardwarePort: WiFi radio hardware interface
                - statusOutputPort: Connection status notifications
                - configInputPort: WiFi configuration (SSID, password)
                */

                port wifiHardwarePort : WiFiRadioInterface {
                    doc /* ESP32 WiFi radio interface */
                }
                port statusOutputPort : NetworkStatusOutputInterface;
                port configInputPort : WiFiConfigInterface;

                // State attributes
                attribute ssid : String {
                    doc /* Configured network SSID */
                }
                attribute password : String {
                    doc /* Encrypted WiFi password */
                }
                attribute connectionState : String = "Disconnected" {
                    doc /* Current state: Disconnected, Scanning, Connecting, Connected, Reconnecting */
                }
                attribute signalStrength : Real = 0.0 {
                    doc /* WiFi signal strength in dBm */
                }
                attribute ipAddress : String = "0.0.0.0";
                attribute retryAttempts : Integer = 0;
                attribute maxRetries : Integer = 5;
                attribute backoffDelay : Real = 1.0 {
                    doc /* Current exponential backoff delay in seconds */
                }

                // Behavior reference
                // perform state NetworkConnectionStateMachine;
            }

            // =====================
            // LOGICAL SUBSYSTEM 4: USER INPUT HANDLER
            // =====================

            part def UserInputHandlerLogical :> LogicalSubsystem {
                doc /*
                Logical subsystem for processing user button input

                Refines: UserInputHandler (conceptual subsystem)

                Responsibilities:
                - Detect button press events (GPIO interrupts)
                - Debounce mechanical switch noise
                - Classify press types (short < 500ms, long > 1000ms, double within 500ms)
                - Map buttons to commands
                - Route commands to appropriate subsystem

                Interfaces:
                - buttonInputPort: GPIO connections to physical buttons
                - commandOutputPort: User commands to other subsystems
                - activityOutputPort: Activity notifications for power management
                */

                port buttonInputPort : GPIOButtonInterface[2..4] {
                    doc /* 2-4 physical buttons (GPIO pins) */
                }
                port commandOutputPort : UserCommandOutputInterface;
                port activityOutputPort : UserActivityInterface;

                // State attributes
                attribute lastButtonPress : String {
                    doc /* Timestamp of last button event */
                }
                attribute currentMode : String = "Normal" {
                    doc /* Input mode: Normal, Configuration */
                }
                attribute pressHistory : String[0..5] {
                    doc /* Recent button events for double-press detection */
                }
                attribute debounceDelay : Real = 0.05 {
                    doc /* Debounce delay in seconds (50ms) */
                }
                attribute longPressThreshold : Real = 1.0 {
                    doc /* Long press threshold in seconds */
                }
                attribute doublePressWindow : Real = 0.5 {
                    doc /* Double-press detection window in seconds */
                }

                // Behavior reference
                // perform action processButtonInput;
            }

            // =====================
            // LOGICAL SUBSYSTEM 5: POWER MANAGER
            // =====================

            part def PowerManagerLogical :> LogicalSubsystem {
                doc /*
                Logical subsystem for power and battery management

                Refines: PowerManager (conceptual subsystem)

                Responsibilities:
                - Monitor battery voltage via ADC
                - Calculate remaining capacity and estimated runtime
                - Detect user inactivity
                - Transition between power modes
                - Control peripheral power (display, WiFi, unused GPIO)
                - Manage sleep/wake cycles

                Power Modes:
                - Active: All systems operational
                - LowPower: Reduced refresh rate, dimmed display
                - Sleep: Display off, periodic wake for data
                - DeepSleep: All off except wake-on-button

                Interfaces:
                - batteryMonitorPort: ADC for battery voltage sensing
                - powerControlPorts: Enable/disable power to peripherals
                - activityInputPort: User activity notifications
                */

                port batteryMonitorPort : ADCInterface {
                    doc /* Analog-to-digital converter for battery voltage */
                    attribute resolution : Integer = 12 {
                        doc /* 12-bit ADC resolution */
                    }
                }
                port powerControlPort : PowerSwitchInterface[0..5] {
                    doc /* Control power to multiple subsystems */
                }
                port activityInputPort : UserActivityInputInterface;

                // State attributes
                attribute batteryVoltage : Real {
                    doc /* Current battery voltage (V) */
                }
                attribute batteryPercent : Real {
                    doc /* Remaining capacity (0-100%) */
                }
                attribute powerMode : String = "Active" {
                    doc /* Current power mode */
                }
                attribute estimatedRuntime : Real {
                    doc /* Estimated remaining runtime in minutes */
                }
                attribute inactivityTimeout : Real = 120.0 {
                    doc /* Timeout for entering sleep mode (seconds) */
                }
                attribute lowBatteryThreshold : Real = 3.3 {
                    doc /* Voltage threshold for low battery warning */
                }
                attribute currentConsumption : Real {
                    doc /* Current draw in mA */
                }

                // Behavior reference
                // perform state PowerStateMachine;
            }

            // =====================
            // INTERFACE DEFINITIONS
            // =====================

            interface def HTTPInterface {
                doc /* HTTP/HTTPS protocol interface for API communication */
            }

            interface def StockDataOutputInterface {
                doc /* Interface for sending processed stock data */
            }

            interface def StockDataInputInterface {
                doc /* Interface for receiving stock data to display */
            }

            interface def UserCommandInterface {
                doc /* Interface for user commands */
            }

            interface def UserCommandOutputInterface {
                doc /* Interface for outputting user commands */
            }

            interface def SubsystemStatusInterface {
                doc /* Status notifications from subsystems */
            }

            interface def SystemStatusInterface {
                doc /* Aggregated system status */
            }

            interface def SPIDisplayInterface {
                doc /* SPI bus connection to LCD display controller */
            }

            interface def DisplayPowerInterface {
                doc /* Control for display power and backlight */
            }

            interface def WiFiRadioInterface {
                doc /* WiFi hardware radio interface */
            }

            interface def NetworkStatusOutputInterface {
                doc /* Network connection status notifications */
            }

            interface def WiFiConfigInterface {
                doc /* WiFi configuration parameters */
            }

            interface def GPIOButtonInterface {
                doc /* GPIO pin connection for button input */
            }

            interface def UserActivityInterface {
                doc /* User activity event notifications */
            }

            interface def UserActivityInputInterface {
                doc /* Receiving user activity events */
            }

            interface def ADCInterface {
                doc /* Analog-to-digital converter interface */
            }

            interface def PowerSwitchInterface {
                doc /* Power enable/disable control */
            }

            // =====================
            // LOGICAL SYSTEM COMPOSITION
            // =====================

            part 'Logical Stock Ticker System' {
                doc /*
                Complete logical architecture with all subsystems and connections

                This is the formal logical architecture that will be mapped to
                physical implementation.
                */

                part dataProcessor : StockDataProcessorLogical;
                part displayController : DisplayControllerLogical;
                part networkManager : NetworkManagerLogical;
                part inputHandler : UserInputHandlerLogical;
                part powerManager : PowerManagerLogical;

                // Interface connections
                connect dataProcessor.dataOutputPort to displayController.dataInputPort {
                    doc /* Stock data flow from processor to display */
                }

                connect inputHandler.commandOutputPort to dataProcessor.commandInputPort {
                    doc /* User commands routed to data processor */
                }

                connect networkManager.statusOutputPort to dataProcessor {
                    doc /* Network status for API availability */
                }

                connect powerManager.powerControlPort to displayController.powerControlPort {
                    doc /* Power control for display backlight */
                }

                connect powerManager.powerControlPort to networkManager {
                    doc /* Power control for WiFi radio */
                }

                connect inputHandler.activityOutputPort to powerManager.activityInputPort {
                    doc /* User activity notifications for sleep management */
                }

                connect dataProcessor.statusOutputPort to displayController.statusInputPort {
                    doc /* Data fetch status for display indicators */
                }
            }
        }

        // ====================================================================
        // CELL 3.3 - SYSTEM BEHAVIOR MODELS (Behavior Pillar)
        // ====================================================================

        package '3 System Behavior Models' {
            doc /*
            System Behavior Models - State machines and detailed action flows

            Defines:
            - Device operational state machine
            - Network connection state machine
            - Power management state machine
            - Display brightness state machine
            - Detailed action decompositions with control flows
            */

            private import SysML::*;
            private import ScalarValues::*;

            // =====================
            // SIGNALS PACKAGE - Event Definitions
            // =====================

            package Signals {
                doc /* Event signal definitions for state machine transitions */

                // Device Operational State Signals
                attribute def PowerOnComplete {
                    doc /* Power-on sequence completed */
                }

                attribute def InitializationComplete {
                    doc /* System initialization completed */
                }

                attribute def UserRequest {
                    doc /* User initiated a request */
                }

                attribute def DataFetchComplete {
                    doc /* Stock data fetch completed */
                }

                attribute def InactivityTimeout {
                    doc /* User inactivity timeout expired */
                }

                attribute def ExtendedInactivity {
                    doc /* Extended inactivity detected */
                }

                attribute def ButtonPress {
                    doc /* User pressed a button */
                }

                attribute def CriticalError {
                    doc /* Critical system error occurred */
                }

                attribute def ErrorRecovered {
                    doc /* Error condition resolved */
                }

                // Network Connection State Signals
                attribute def StartConnection {
                    doc /* Begin network connection attempt */
                }

                attribute def NetworkFound {
                    doc /* WiFi network SSID found */
                }

                attribute def NetworkNotFound {
                    doc /* WiFi network SSID not found in scan */
                }

                attribute def AuthSuccess {
                    doc /* WiFi authentication successful */
                }

                attribute def AuthFailure {
                    doc /* WiFi authentication failed */
                }

                attribute def ConnectionLost {
                    doc /* WiFi connection lost */
                }

                attribute def RetryReady {
                    doc /* Ready to retry connection */
                }

                attribute def MaxRetriesExceeded {
                    doc /* Maximum retry attempts exceeded */
                }

                // Power Management State Signals
                attribute def LowBattery {
                    doc /* Battery level below threshold */
                }

                attribute def BatteryRecovered {
                    doc /* Battery level recovered */
                }

                attribute def ShortInactivity {
                    doc /* Short inactivity period (dimming threshold) */
                }

                attribute def LongInactivity {
                    doc /* Long inactivity period (sleep threshold) */
                }

                // Display Brightness State Signals
                attribute def AmbientLightIncrease {
                    doc /* Ambient light level increased */
                }

                attribute def AmbientLightDecrease {
                    doc /* Ambient light level decreased */
                }

                attribute def HighAmbientLight {
                    doc /* Very bright ambient conditions */
                }

                attribute def AmbientLightNormal {
                    doc /* Normal ambient light conditions */
                }
            }

            // Import signals for state machine use
            private import Signals::*;

            // =====================
            // STATE MACHINE 1: DEVICE OPERATIONAL STATES
            // =====================

            state def DeviceOperationalStates {
                doc /*
                Overall device lifecycle state machine

                States:
                - poweringOn: Initial boot sequence
                - initializing: Loading config, establishing connections
                - ready: Idle, waiting for user interaction
                - active: Fetching and displaying stock data
                - error: Error condition
                - sleep: Low-power mode
                - deepSleep: Ultra-low power

                Transitions triggered by:
                - System events (boot complete, initialization complete)
                - User events (button press, inactivity timeout)
                - Error events (critical failure)
                - Timer events (auto-refresh, timeout)
                */

                entry; then poweringOn;

                state poweringOn {
                    doc /* Power-on self-test and hardware initialization */
                    entry action {
                        doc /* Initialize hardware peripherals */
                    }
                }

                transition poweringOn_to_initializing
                    first poweringOn
                    accept PowerOnComplete
                    then initializing;

                state initializing {
                    doc /* Load configuration, connect to WiFi, fetch initial data */
                    entry action {
                        doc /* Load watchlist from storage, initiate WiFi connection */
                    }
                }

                transition initializing_to_ready
                    first initializing
                    accept InitializationComplete
                    then ready;

                state ready {
                    doc /* Idle state - displaying current stock, waiting for input */
                }

                transition ready_to_active
                    first ready
                    accept UserRequest
                    then active;

                state active {
                    doc /* Actively processing user request (fetching new data) */
                    entry action {
                        doc /* Trigger stock data fetch */
                    }
                }

                transition active_to_ready
                    first active
                    accept DataFetchComplete
                    then ready;

                transition ready_to_sleep
                    first ready
                    accept InactivityTimeout
                    then sleep;

                state sleep {
                    doc /* Low-power sleep mode - display dimmed or off */
                    entry action {
                        doc /* Save state, reduce power */
                    }
                }

                transition sleep_to_ready
                    first sleep
                    accept ButtonPress
                    then ready;

                transition sleep_to_deepSleep
                    first sleep
                    accept ExtendedInactivity
                    then deepSleep;

                state deepSleep {
                    doc /* Ultra-low power - only wake-on-button active */
                    entry action {
                        doc /* Disable all peripherals except wake source */
                    }
                }

                transition deepSleep_to_ready
                    first deepSleep
                    accept ButtonPress
                    then ready;

                state errorState {
                    doc /* Error display and recovery */
                    entry action {
                        doc /* Display error message, log error */
                    }
                }

                transition error_to_ready
                    first errorState
                    accept ErrorRecovered
                    then ready;
            }

            // =====================
            // STATE MACHINE 2: NETWORK CONNECTION STATES
            // =====================

            state def NetworkConnectionStates {
                doc /*
                WiFi connection lifecycle state machine

                States:
                - disconnected: No network connection
                - scanning: Scanning for available networks
                - authenticating: WPA2/WPA3 handshake in progress
                - connected: Successfully connected with IP address
                - reconnecting: Attempting to re-establish lost connection

                Transitions driven by:
                - Network events (scan complete, auth success/fail, connection lost)
                - Timer events (retry timeout, keep-alive)
                */

                entry; then disconnected;

                state disconnected {
                    doc /* No active WiFi connection */
                }

                transition disconnected_to_scanning
                    first disconnected
                    accept StartConnection
                    then scanning;

                state scanning {
                    doc /* Scanning for available WiFi networks */
                    entry action {
                        doc /* Initiate WiFi scan */
                    }
                }

                transition scanning_to_authenticating
                    first scanning
                    accept NetworkFound
                    then authenticating;

                transition scanning_to_disconnected
                    first scanning
                    accept NetworkNotFound
                    then disconnected;

                state authenticating {
                    doc /* WPA2/WPA3 authentication in progress */
                    entry action {
                        doc /* Send authentication credentials */
                    }
                }

                transition authenticating_to_connected
                    first authenticating
                    accept AuthSuccess
                    then connected;

                transition authenticating_to_reconnecting
                    first authenticating
                    accept AuthFailure
                    then reconnecting;

                state connected {
                    doc /* Successfully connected - normal operation */
                    entry action {
                        doc /* Obtain IP address via DHCP */
                    }
                }

                transition connected_to_reconnecting
                    first connected
                    accept ConnectionLost
                    then reconnecting;

                state reconnecting {
                    doc /* Attempting to re-establish connection */
                    entry action {
                        doc /* Exponential backoff retry */
                    }
                }

                transition reconnecting_to_scanning
                    first reconnecting
                    accept RetryReady
                    then scanning;

                transition reconnecting_to_disconnected
                    first reconnecting
                    accept MaxRetriesExceeded
                    then disconnected;
            }

            // =====================
            // STATE MACHINE 3: POWER MANAGEMENT STATES
            // =====================

            state def PowerManagementStates {
                doc /*
                Power mode state machine

                States:
                - active: Full power - all systems operational
                - lowPower: Reduced power - dimmed display, slower refresh
                - sleep: Display off, periodic wake for refresh
                - deepSleep: All off except button wake interrupt

                Transitions based on:
                - User activity (button press)
                - Battery level
                - Inactivity timers
                */

                entry; then active;

                state active {
                    doc /* Full performance mode */
                }

                transition active_to_lowPower
                    first active
                    accept LowBattery
                    then lowPower;

                transition active_to_sleep
                    first active
                    accept ShortInactivity
                    then sleep;

                state lowPower {
                    doc /* Reduced refresh and dimmed display */
                    entry action {
                        doc /* Dim display, reduce refresh rate */
                    }
                }

                transition lowPower_to_active
                    first lowPower
                    accept BatteryRecovered
                    then active;

                transition lowPower_to_sleep
                    first lowPower
                    accept ShortInactivity
                    then sleep;

                state sleep {
                    doc /* Display off, periodic wake */
                    entry action {
                        doc /* Turn off display, suspend WiFi */
                    }
                }

                transition sleep_to_active
                    first sleep
                    accept ButtonPress
                    then active;

                transition sleep_to_deepSleep
                    first sleep
                    accept LongInactivity
                    then deepSleep;

                state deepSleep {
                    doc /* Minimum power consumption */
                    entry action {
                        doc /* Disable all peripherals */
                    }
                }

                transition deepSleep_to_active
                    first deepSleep
                    accept ButtonPress
                    then active;
            }

            // =====================
            // STATE MACHINE 4: DISPLAY BRIGHTNESS STATES
            // =====================

            state def DisplayBrightnessStates {
                doc /*
                Display backlight control state machine

                States based on ambient light conditions:
                - lowBrightness: < 100 lux (60% brightness)
                - mediumBrightness: 100-500 lux (80% brightness)
                - highBrightness: > 500 lux (100% brightness)

                Transitions triggered by ambient light sensor readings
                */

                entry; then mediumBrightness;

                state lowBrightness {
                    doc /* 60% backlight for dim environments */
                }

                transition low_to_medium
                    first lowBrightness
                    accept AmbientLightIncrease
                    then mediumBrightness;

                state mediumBrightness {
                    doc /* 80% backlight for normal indoor lighting */
                }

                transition medium_to_low
                    first mediumBrightness
                    accept AmbientLightDecrease
                    then lowBrightness;

                transition medium_to_high
                    first mediumBrightness
                    accept HighAmbientLight
                    then highBrightness;

                state highBrightness {
                    doc /* 100% backlight for bright environments */
                }

                transition high_to_medium
                    first highBrightness
                    accept AmbientLightNormal
                    then mediumBrightness;
            }
        }

        // ====================================================================
        // CELL 3.4 - SYSTEM PARAMETERS & BUDGETS (Parameters Pillar)
        // ====================================================================

        package '4 System Parameters and Budgets' {
            doc /*
            System Parameters and Budgets

            Allocates system-level budgets to subsystems:
            - Power budget (total power consumption)
            - Timing budget (latency components)
            - Memory budget (RAM allocation)
            - Cost budget (BOM cost allocation)
            */

            private import SysML::*;
    private import ScalarValues::*;

            attribute def ParameterBudget {
                attribute allocated : Real;
                attribute consumed : Real;
                attribute margin : Real;
                attribute unit : String;

                constraint budgetConstraint {
                    doc /* Consumed must not exceed allocated */
                    consumed <= allocated
                }

                constraint marginCalculation {
                    doc /* Margin is allocated minus consumed */
                    margin == allocated - consumed
                }
            }

            // =====================
            // POWER BUDGET
            // =====================

            attribute powerBudget : ParameterBudget {
                doc /*
                Total power consumption budget for active mode

                Allocated: 500 mW (derived from battery life requirement)
                Subsystem allocations ensure 8-hour runtime on 500mAh battery
                */

                attribute :>> allocated = 500.0;
                attribute :>> unit = "milliwatts";

                // Subsystem power allocations
                attribute displayPower : Real = 200.0 {
                    doc /* Display controller + LCD backlight */
                }
                attribute processorPower : Real = 150.0 {
                    doc /* ESP32 processor at 240MHz */
                }
                attribute networkPower : Real = 100.0 {
                    doc /* WiFi radio (average with duty cycling) */
                }
                attribute miscPower : Real = 50.0 {
                    doc /* Voltage regulators, GPIO, ADC */
                }

                constraint totalPower {
                    doc /* Sum of subsystem allocations must not exceed total budget */
                    displayPower + processorPower + networkPower + miscPower <= allocated
                }
            }

            // =====================
            // TIMING/LATENCY BUDGET
            // =====================

            attribute updateLatencyBudget : ParameterBudget {
                doc /*
                End-to-end update latency budget

                Allocated: 2.0 seconds (from PERF-001)
                Breakdown across subsystems
                */

                attribute :>> allocated = 2.0;
                attribute :>> unit = "seconds";

                attribute networkLatency : Real = 0.8 {
                    doc /* Network Manager: WiFi connection + HTTP round-trip */
                }
                attribute processingLatency : Real = 0.5 {
                    doc /* Data Processor: JSON parsing + validation */
                }
                attribute displayLatency : Real = 0.3 {
                    doc /* Display Controller: Rendering + SPI transfer */
                }
                attribute marginTime : Real = 0.4 {
                    doc /* Reserve margin for variability */
                }

                constraint totalLatency {
                    networkLatency + processingLatency + displayLatency <= allocated
                }
            }

            // =====================
            // MEMORY BUDGET (RAM)
            // =====================

            attribute memoryBudget : ParameterBudget {
                doc /*
                RAM usage budget

                Allocated: 64,000 bytes (64KB available on ESP32, 32KB reserved for system)
                Subsystem allocations
                */

                attribute :>> allocated = 64000.0;
                attribute :>> unit = "bytes";

                attribute dataProcessorMemory : Real = 24000.0 {
                    doc /* Cache (10 stock records × 200 bytes), HTTP buffers */
                }
                attribute displayBufferMemory : Real = 32400.0 {
                    doc /* Framebuffer: 135 × 240 × RGB565 = 64,800 bytes (off-chip) */
                }
                attribute networkBufferMemory : Real = 8000.0 {
                    doc /* WiFi stack and HTTP client buffers */
                }
                attribute inputHandlerMemory : Real = 1000.0 {
                    doc /* Button state and event queues */
                }
                attribute powerManagerMemory : Real = 500.0 {
                    doc /* Power state and battery monitoring */
                }
                attribute systemOverhead : Real = 10000.0 {
                    doc /* OS (FreeRTOS) and stack overhead */
                }

                constraint totalMemory {
                    dataProcessorMemory + networkBufferMemory + inputHandlerMemory + powerManagerMemory + systemOverhead <= allocated
                }
            }

            // =====================
            // COST BUDGET (BOM)
            // =====================

            attribute costBudget : ParameterBudget {
                doc /*
                Bill of Materials cost budget

                Allocated: $25 (from BUS-001 threshold)
                Target: $15 (from MoE)
                */

                attribute :>> allocated = 25.0;
                attribute :>> unit = "USD";

                attribute mcuCost : Real = 12.50 {
                    doc /* ESP32 TTGO T-Display module */
                }
                attribute batteryCost : Real = 3.50 {
                    doc /* 500mAh LiPo battery */
                }
                attribute enclosureCost : Real = 4.00 {
                    doc /* Plastic case and buttons */
                }
                attribute pcbAndAssembly : Real = 3.00 {
                    doc /* PCB, connectors, assembly */
                }
                attribute miscComponents : Real = 2.00 {
                    doc /* Resistors, capacitors, voltage regulator */
                }

                constraint totalCost {
                    mcuCost + batteryCost + enclosureCost + pcbAndAssembly + miscComponents <= allocated
                }
            }
        }

        // ====================================================================
        // CELL 3.5 - SYSTEM VERIFICATION (Safety & Reliability Pillar)
        // ====================================================================

        package '5 System Verification' {
            doc /*
            System Verification - Test cases and analysis for requirements

            Verification methods:
            - Test: Execute and measure
            - Analysis: Calculate or simulate
            - Inspection: Visual examination
            - Demonstration: Observe operation
            */

            private import SysML::*;
    private import ScalarValues::*;

            // =====================
            // VERIFICATION DEFINITION BASE
            // =====================

            verification def VerifySystemRequirement {
                doc /* Base verification definition */
                subject system;
                attribute testMethod : String;
                attribute testProcedure : String;
                attribute successCriteria : String;
            }

            // =====================
            // FUNCTIONAL REQUIREMENT VERIFICATIONS
            // =====================

            verification 'Verify FR-001: WiFi Data Retrieval' : VerifySystemRequirement {
                doc /* Verify system retrieves stock data via WiFi */
                attribute :>> testMethod = "Test";
                attribute :>> testProcedure = "1. Power on device. 2. Verify WiFi connection. 3. Request stock data for AAPL. 4. Verify data received and displayed.";
                attribute :>> successCriteria = "Stock data (symbol, price, change, timestamp) displayed on screen.";

                subject stockTickerSystem;

                constraint {
                    doc /* Data must be successfully retrieved */
                    // stockTickerSystem.dataProcessor.cachedData->size() > 0
                }

                // Verifies requirement
                // verify FR_001;
            }

            verification 'Verify FR-002: Watchlist Capacity' : VerifySystemRequirement {
                doc /* Verify support for at least 5 stocks in watchlist */
                attribute :>> testMethod = "Test";
                attribute :>> testProcedure = "1. Configure watchlist with 5 stock symbols. 2. Verify all symbols stored. 3. Cycle through all stocks. 4. Verify each displays correctly.";
                attribute :>> successCriteria = "Device successfully stores and displays 5 different stocks.";

                subject stockTickerSystem;

                constraint {
                    doc /* Watchlist must support at least 5 symbols */
                    // stockTickerSystem.dataProcessor.watchlist->size() >= 5
                }

                // verify FR_002;
            }

            verification 'Verify FR-005: Persist Watchlist' : VerifySystemRequirement {
                doc /* Verify watchlist persists across power cycles */
                attribute :>> testMethod = "Test";
                attribute :>> testProcedure = "1. Configure watchlist with specific symbols. 2. Power off device. 3. Power on device. 4. Verify watchlist restored.";
                attribute :>> successCriteria = "Watchlist identical before and after power cycle.";

                subject stockTickerSystem;

                // verify FR_005;
            }

            // =====================
            // PERFORMANCE REQUIREMENT VERIFICATIONS
            // =====================

            verification 'Verify PERF-001: Update Latency' : VerifySystemRequirement {
                doc /* Verify 2-second update latency requirement */
                attribute :>> testMethod = "Test";
                attribute :>> testProcedure = "1. Press Next button. 2. Measure time from button press to display update. 3. Repeat 10 times. 4. Calculate average and maximum.";
                attribute :>> successCriteria = "Average latency ≤ 2.0s, maximum latency ≤ 3.0s";

                subject stockTickerSystem;

                attribute measuredLatency : Real;

                constraint {
                    doc /* Measured latency within requirement */
                    measuredLatency <= 2.0
                }

                // verify PERF_001;
            }

            verification 'Verify PWR-001: Battery Life' : VerifySystemRequirement {
                doc /* Verify 4-hour minimum battery life */
                attribute :>> testMethod = "Test";
                attribute :>> testProcedure = "1. Fully charge battery. 2. Operate device with typical usage (stock cycling every 30s). 3. Measure time until battery depletion.";
                attribute :>> successCriteria = "Runtime ≥ 4.0 hours";

                subject stockTickerSystem;

                attribute measuredRuntime : Real;

                constraint {
                    doc /* Measured runtime meets minimum requirement */
                    measuredRuntime >= 4.0
                }

                // verify PWR_001;
            }

            // =====================
            // ANALYSIS CASES
            // =====================

            analysis def BatteryLifeAnalysis {
                doc /*
                Analytical verification of battery life requirement

                Inputs:
                - Battery capacity (500mAh)
                - Subsystem power consumption estimates
                - Usage profile (% time in each power mode)

                Output:
                - Calculated runtime

                Success: Runtime ≥ 4.0 hours (threshold)
                */

                subject stockTickerSystem;

                attribute batteryCapacity : Real = 500.0 {
                    doc /* mAh */
                }
                attribute batteryVoltage : Real = 3.7 {
                    doc /* Volts */
                }
                attribute activeCurrent : Real = 135.0 {
                    doc /* mA in active mode (500mW @ 3.7V) */
                }
                attribute lowPowerCurrent : Real = 80.0 {
                    doc /* mA in low power mode */
                }
                attribute sleepCurrent : Real = 10.0 {
                    doc /* mA in sleep mode */
                }

                attribute activePercent : Real = 60.0 {
                    doc /* Typical usage: 60% active */
                }
                attribute lowPowerPercent : Real = 20.0 {
                    doc /* 20% low power */
                }
                attribute sleepPercent : Real = 20.0 {
                    doc /* 20% sleep */
                }

                attribute averageCurrent : Real;
                attribute calculatedRuntime : Real;

                constraint averageCurrentCalculation {
                    doc /* Weighted average of current draw */
                    averageCurrent == (activeCurrent * activePercent / 100.0) +
                                      (lowPowerCurrent * lowPowerPercent / 100.0) +
                                      (sleepCurrent * sleepPercent / 100.0)
                }

                constraint runtimeCalculation {
                    doc /* Runtime = Capacity / Average Current */
                    calculatedRuntime == batteryCapacity / averageCurrent
                }

                constraint meetsRequirement {
                    doc /* Must meet 4-hour minimum */
                    calculatedRuntime >= 4.0
                }

                // verify PWR_001;
            }

            analysis def WiFiRangeAnalysis {
                doc /*
                Analytical verification of WiFi range requirement

                Uses Friis transmission equation and WiFi sensitivity specs
                */

                subject stockTickerSystem;

                attribute transmitPower : Real = 20.0 {
                    doc /* dBm (ESP32 WiFi transmit power) */
                }
                attribute receiverSensitivity : Real = -90.0 {
                    doc /* dBm (ESP32 WiFi receive sensitivity at 802.11n MCS0) */
                }
                attribute frequency : Real = 2400.0 {
                    doc /* MHz (2.4 GHz WiFi) */
                }
                attribute antennaGain : Real = 2.0 {
                    doc /* dBi (integrated antenna) */
                }

                attribute calculatedRange : Real;

                constraint rangeEstimate {
                    doc /* Friis formula for free-space range */
                    // Simplified: assumes ~10-15m indoor range for 802.11n
                    calculatedRange >= 10.0
                }

                constraint meetsRequirement {
                    doc /* Must meet minimum range MoE */
                    calculatedRange >= 5.0
                }
            }
        }
    }
}

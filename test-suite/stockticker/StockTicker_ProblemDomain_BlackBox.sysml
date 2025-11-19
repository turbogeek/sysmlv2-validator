package StockTicker_ProblemDomain_BlackBox {
    doc /*
    Problem Domain - Understanding what stakeholders need

    This domain captures the problem space from two perspectives:
    - Black Box: External view (stakeholder needs, context, use cases)
    - White Box: Internal view (functional decomposition, conceptual subsystems)
    */

    private import SysML::*;

    // ========================================================================
    // BLACK BOX PERSPECTIVE - External View
    // ========================================================================

    package '1 Black Box' {
        doc /*
        Black Box Perspective: System from stakeholder viewpoint

        Focuses on:
        - What stakeholders need (Stakeholder Needs)
        - Operating environment (System Context)
        - How users interact with system (Use Cases)
        - How to measure success (Measures of Effectiveness)
        - What could go wrong (Risks & Hazards)
        */

        private import SysML::*;

        // ====================================================================
        // CELL 1.1 - STAKEHOLDER NEEDS (Requirements Pillar)
        // ====================================================================

        package '1 Stakeholder Needs' {
            doc /*
            Stakeholder Needs - Raw requirements from stakeholders

            Captures verbatim statements from stakeholders about what they
            need the system to do. These are refined into formal system
            requirements in the Solution Domain.

            Stakeholder Groups:
            - End User: Individual investors monitoring personal portfolios
            - Day Trader: Professional traders needing quick data access
            - System Administrator: IT staff maintaining devices
            */

            private import SysML::*;
            private import ScalarValues::*;

            // Stakeholder need base definition
            requirement def StakeholderNeed {
                doc /* Base definition for all stakeholder needs */
                attribute id : String;
                attribute source : String;  // Which stakeholder
                attribute category : String;
                attribute priority : String;  // High, Medium, Low
            }

            // ===================
            // FUNCTIONAL NEEDS
            // ===================

            requirement SN_001 : StakeholderNeed {
                doc /* "I want to see current stock prices at a glance" */
                attribute :>> id = "SN-001";
                attribute :>> source = "End User";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
            }

            requirement SN_002 : StakeholderNeed {
                doc /* "I need to track multiple stocks simultaneously" */
                attribute :>> id = "SN-002";
                attribute :>> source = "End User";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
            }

            requirement SN_003 : StakeholderNeed {
                doc /* "I want to switch between different stocks quickly" */
                attribute :>> id = "SN-003";
                attribute :>> source = "Day Trader";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
            }

            requirement SN_004 : StakeholderNeed {
                doc /* "I need to know when a stock price was last updated" */
                attribute :>> id = "SN-004";
                attribute :>> source = "Day Trader";
                attribute :>> category = "Functional";
                attribute :>> priority = "Medium";
            }

            requirement SN_005 : StakeholderNeed {
                doc /* "I want to see price change (up/down) at a glance" */
                attribute :>> id = "SN-005";
                attribute :>> source = "End User";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
            }

            requirement SN_006 : StakeholderNeed {
                doc /* "I need the device to work anywhere with WiFi" */
                attribute :>> id = "SN-006";
                attribute :>> source = "End User";
                attribute :>> category = "Functional";
                attribute :>> priority = "High";
            }

            requirement SN_007 : StakeholderNeed {
                doc /* "I want to configure which stocks to monitor" */
                attribute :>> id = "SN-007";
                attribute :>> source = "End User";
                attribute :>> category = "Functional";
                attribute :>> priority = "Medium";
            }

            // ===================
            // PERFORMANCE NEEDS
            // ===================

            requirement SN_008 : StakeholderNeed {
                doc /* "Stock prices should update quickly (within seconds)" */
                attribute :>> id = "SN-008";
                attribute :>> source = "Day Trader";
                attribute :>> category = "Performance";
                attribute :>> priority = "High";
            }

            requirement SN_009 : StakeholderNeed {
                doc /* "The display should be readable in normal lighting" */
                attribute :>> id = "SN-009";
                attribute :>> source = "End User";
                attribute :>> category = "Performance";
                attribute :>> priority = "High";
            }

            requirement SN_010 : StakeholderNeed {
                doc /* "The device should run for a full workday on battery" */
                attribute :>> id = "SN-010";
                attribute :>> source = "Day Trader";
                attribute :>> category = "Performance";
                attribute :>> priority = "High";
            }

            // ===================
            // INTERFACE NEEDS
            // ===================

            requirement SN_011 : StakeholderNeed {
                doc /* "I want simple button controls (no touchscreen complexity)" */
                attribute :>> id = "SN-011";
                attribute :>> source = "End User";
                attribute :>> category = "Interface";
                attribute :>> priority = "Medium";
            }

            requirement SN_012 : StakeholderNeed {
                doc /* "The display should be compact but readable" */
                attribute :>> id = "SN-012";
                attribute :>> source = "End User";
                attribute :>> category = "Interface";
                attribute :>> priority = "High";
            }

            requirement SN_013 : StakeholderNeed {
                doc /* "I need visual indication of connectivity status" */
                attribute :>> id = "SN-013";
                attribute :>> source = "Day Trader";
                attribute :>> category = "Interface";
                attribute :>> priority = "Medium";
            }

            // ===================
            // CONNECTIVITY NEEDS
            // ===================

            requirement SN_014 : StakeholderNeed {
                doc /* "The device should automatically reconnect if WiFi drops" */
                attribute :>> id = "SN-014";
                attribute :>> source = "Day Trader";
                attribute :>> category = "Connectivity";
                attribute :>> priority = "High";
            }

            requirement SN_015 : StakeholderNeed {
                doc /* "I want to use my home/office WiFi network" */
                attribute :>> id = "SN-015";
                attribute :>> source = "End User";
                attribute :>> category = "Connectivity";
                attribute :>> priority = "High";
            }

            // ===================
            // POWER NEEDS
            // ===================

            requirement SN_016 : StakeholderNeed {
                doc /* "I want to recharge via standard USB cable" */
                attribute :>> id = "SN-016";
                attribute :>> source = "End User";
                attribute :>> category = "Power";
                attribute :>> priority = "High";
            }

            requirement SN_017 : StakeholderNeed {
                doc /* "The device should enter sleep mode when inactive" */
                attribute :>> id = "SN-017";
                attribute :>> source = "End User";
                attribute :>> category = "Power";
                attribute :>> priority = "Medium";
            }

            // ===================
            // RELIABILITY NEEDS
            // ===================

            requirement SN_018 : StakeholderNeed {
                doc /* "The device should handle network errors gracefully" */
                attribute :>> id = "SN-018";
                attribute :>> source = "Day Trader";
                attribute :>> category = "Reliability";
                attribute :>> priority = "High";
            }

            requirement SN_019 : StakeholderNeed {
                doc /* "I need error messages that explain what went wrong" */
                attribute :>> id = "SN-019";
                attribute :>> source = "End User";
                attribute :>> category = "Reliability";
                attribute :>> priority = "Medium";
            }

            // ===================
            // STORAGE NEEDS
            // ===================

            requirement SN_020 : StakeholderNeed {
                doc /* "The device should remember my stock list after power off" */
                attribute :>> id = "SN-020";
                attribute :>> source = "End User";
                attribute :>> category = "Storage";
                attribute :>> priority = "High";
            }

            requirement SN_021 : StakeholderNeed {
                doc /* "WiFi credentials should be stored securely" */
                attribute :>> id = "SN-021";
                attribute :>> source = "System Administrator";
                attribute :>> category = "Storage";
                attribute :>> priority = "High";
            }

            // ===================
            // COST/AFFORDABILITY NEEDS
            // ===================

            requirement SN_022 : StakeholderNeed {
                doc /* "The device should be affordable for individual users" */
                attribute :>> id = "SN-022";
                attribute :>> source = "End User";
                attribute :>> category = "Business";
                attribute :>> priority = "High";
            }

            requirement SN_023 : StakeholderNeed {
                doc /* "I want a device under $50 for basic features" */
                attribute :>> id = "SN-023";
                attribute :>> source = "End User";
                attribute :>> category = "Business";
                attribute :>> priority = "Medium";
            }

            // ===================
            // PORTABILITY NEEDS
            // ===================

            requirement SN_024 : StakeholderNeed {
                doc /* "The device should be small enough to fit on my desk" */
                attribute :>> id = "SN-024";
                attribute :>> source = "End User";
                attribute :>> category = "Physical";
                attribute :>> priority = "Medium";
            }

            requirement SN_025 : StakeholderNeed {
                doc /* "I want to carry it in my bag if needed" */
                attribute :>> id = "SN-025";
                attribute :>> source = "Day Trader";
                attribute :>> category = "Physical";
                attribute :>> priority = "Low";
            }
        }

        // ====================================================================
        // CELL 1.2 - SYSTEM CONTEXT (Structure Pillar)
        // ====================================================================

        package '2 System Context' {
            doc /*
            System Context - Operating environment and external participants

            Defines:
            - System of Interest (Stock Ticker Device)
            - External Participants (users, services, networks)
            - Operating Environments
            - Interactions between system and environment
            - Item flows (data exchanged)
            */

            private import SysML::*;
            private import ScalarValues::*;

            // System of Interest
            part def StockTickerDevice {
                doc /* The portable stock ticker device (system being developed) */
            }

            // =====================
            // EXTERNAL PARTICIPANTS
            // =====================

            part def ExternalParticipant {
                doc /* Base definition for actors outside the system */
            }

            part def EndUser :> ExternalParticipant {
                doc /* Individual investor monitoring personal stocks */
                attribute userName : String;
                attribute watchlistSize : Integer;
            }

            part def DayTrader :> ExternalParticipant {
                doc /* Professional trader requiring rapid updates */
                attribute tradingFrequency : String = "High";
                attribute requiresLowLatency : Boolean = true;
            }

            part def StockDataProvider :> ExternalParticipant {
                doc /* External REST API service providing real-time stock data */
                attribute apiURL : String;
                attribute apiKey : String;
                attribute updateInterval : Real = 1.0;  // seconds
            }

            part def WiFiNetwork :> ExternalParticipant {
                doc /* Wireless network infrastructure */
                attribute networkSSID : String;
                attribute securityType : String;  // WPA2, WPA3
                attribute signalStrength : Real;
            }

            part def PowerGrid :> ExternalParticipant {
                doc /* USB power source for charging */
                attribute voltage : Real = 5.0;  // volts
                attribute current : Real = 1.0;  // amperes
            }

            // =====================
            // OPERATING ENVIRONMENTS
            // =====================

            part def OperatingEnvironment {
                doc /* Context in which the stock ticker operates */
            }

            part 'Stock Monitoring Environment' : OperatingEnvironment {
                doc /*
                Primary operating environment:
                - Home or office desk setting
                - WiFi network available
                - USB power for charging
                - User actively monitoring stocks during trading hours
                */

                part device : StockTickerDevice;
                part user : EndUser;
                part dataProvider : StockDataProvider;
                part network : WiFiNetwork;
                part usbPower : PowerGrid;

                // Interactions
                connect user to device;
                connect device to dataProvider;
                connect device to network;
                connect device to usbPower;
            }

            part 'Mobile Monitoring Environment' : OperatingEnvironment {
                doc /*
                Secondary environment:
                - Day trader using device on the go
                - Public WiFi networks
                - Battery-powered operation
                - Frequent stock switching
                */

                part device : StockTickerDevice;
                part trader : DayTrader;
                part dataProvider : StockDataProvider;
                part publicWiFi : WiFiNetwork;

                connect trader to device;
                connect device to dataProvider;
                connect device to publicWiFi;
            }

            part 'Degraded Network Environment' : OperatingEnvironment {
                doc /*
                Edge case environment:
                - Weak or intermittent WiFi signal
                - Network reconnection scenarios
                - Error handling situations
                */

                part device : StockTickerDevice;
                part user : EndUser;
                part dataProvider : StockDataProvider;
                part unreliableNetwork : WiFiNetwork {
                    attribute :>> signalStrength = 20.0;  // Weak signal
                }

                connect user to device;
                connect device to dataProvider;
                connect device to unreliableNetwork;
            }

            // =====================
            // ITEM FLOWS
            // =====================

            item def UserInput {
                doc /* Physical button presses from user */
                attribute buttonType : String;  // Next, Previous, Select
                attribute pressType : String;    // Short, Long, Double
                attribute timestamp : String;
            }

            item def StockDataRequest {
                doc /* Request for stock price information */
                attribute stockSymbol : String;
                attribute requestTime : String;
            }

            item def StockDataResponse {
                doc /* Real-time stock price data */
                attribute symbol : String;
                attribute currentPrice : Real;
                attribute priceChange : Real;
                attribute percentChange : Real;
                attribute timestamp : String;
                attribute marketStatus : String;  // Open, Closed, Pre-market
            }

            item def VisualDisplay {
                doc /* Information rendered on screen */
                attribute displayContent : String;
                attribute screenLayout : String;
            }

            item def NetworkConnection {
                doc /* WiFi network connection */
                attribute ssid : String;
                attribute connectionStatus : String;
                attribute ipAddress : String;
            }

            item def PowerFlow {
                doc /* Electrical power */
                attribute voltage : Real;
                attribute current : Real;
            }

            item def ErrorNotification {
                doc /* Error messages displayed to user */
                attribute errorCode : String;
                attribute errorMessage : String;
                attribute severity : String;  // Warning, Error, Critical
            }

            // Flow definitions
            flow def UserInputFlow {
                doc /* Flow of user commands to device */
                item : UserInput;
            }

            flow def StockDataRequestFlow {
                doc /* Flow of stock data requests */
                item : StockDataRequest;
            }

            flow def StockDataResponseFlow {
                doc /* Flow of stock data from provider */
                item : StockDataResponse;
            }

            flow def DisplayOutputFlow {
                doc /* Flow of visual information to user */
                item : VisualDisplay;
            }
        }

        // ====================================================================
        // CELL 1.3 - USE CASES (Behavior Pillar)
        // ====================================================================

        package '3 Use Cases' {
            doc /*
            Use Cases - User interactions with the system (black-box behavior)

            Primary use cases:
            - View Current Stock Price
            - View Multiple Stocks
            - Configure Stock List
            - Respond to Price Changes
            - Handle Network Issues
            */

            private import SysML::*;
            private import ScalarValues::*;

            // =====================
            // PRIMARY USE CASES
            // =====================

            use case 'View Current Stock Price' {
                doc /*
                User requests and views current price for a specific stock

                Precondition: Device is powered on and connected to WiFi
                Postcondition: Current stock price is displayed on screen

                Basic Flow:
                1. User powers on device (or wakes from sleep)
                2. Device connects to WiFi network
                3. Device requests data for default/selected stock
                4. Stock Data Provider returns current price information
                5. Device displays stock symbol, price, change on screen
                6. User views the displayed information

                Swimlanes:
                - End User: Power on, view display
                - Stock Ticker Device: Connect WiFi, request data, render display
                - Stock Data Provider: Return stock data
                */

                objective viewPriceObjective {
                    doc /* Enable user to see current stock price with minimal interaction */
                }

                include use case 'Establish Network Connection';
                include use case 'Fetch Stock Data';
                include use case 'Display Stock Information';
            }

            use case 'View Multiple Stocks' {
                doc /*
                User cycles through watchlist of multiple stocks

                Precondition: Watchlist is configured with 2+ stocks
                Postcondition: User has viewed prices for multiple stocks

                Basic Flow:
                1. User presses "Next" button to advance to next stock
                2. Device fetches data for next stock in watchlist
                3. Device updates display with new stock information
                4. User views updated display
                5. Repeat steps 1-4 for each stock
                6. User can press "Previous" to go back

                Alternate Flow:
                - If at end of list, "Next" wraps to first stock
                - If at beginning of list, "Previous" wraps to last stock
                */

                objective multipleStocksObjective {
                    doc /* Enable quick switching between monitored stocks */
                }

                include use case 'Fetch Stock Data';
                include use case 'Display Stock Information';
                include use case 'Handle Button Input';
            }

            use case 'Configure Stock List' {
                doc /*
                User adds or removes stocks from watchlist

                Precondition: Device is in configuration mode
                Postcondition: Watchlist is updated with user's selections

                Basic Flow:
                1. User presses "Select" button (long press) to enter config mode
                2. Device displays configuration menu
                3. User navigates to "Add Stock" option
                4. User enters stock symbol (via buttons or companion app)
                5. Device validates stock symbol with provider
                6. If valid, stock is added to watchlist
                7. Device displays confirmation message
                8. User exits configuration mode

                Alternate Flow (Remove Stock):
                3. User navigates to "Remove Stock" option
                4. User selects stock to remove from list
                5. Device removes stock and displays confirmation
                */

                objective configureWatchlistObjective {
                    doc /* Allow user to customize which stocks to monitor */
                }

                include use case 'Validate Stock Symbol';
                include use case 'Display Configuration Menu';
                include use case 'Handle Button Input';
            }

            use case 'Monitor Price Changes' {
                doc /*
                Device continuously updates stock prices at regular intervals

                Precondition: Device is connected and displaying a stock
                Postcondition: Price information is kept current

                Basic Flow:
                1. Device displays current stock price
                2. After update interval (e.g., 5 seconds), device requests new data
                3. Stock Data Provider returns updated price
                4. Device compares new price with previous price
                5. Device updates display with new price and change indicator
                6. Repeat steps 2-5 continuously

                Alternate Flow (Price Alert):
                - If price change exceeds threshold, visual indication shown
                */

                objective priceMonitoringObjective {
                    doc /* Keep displayed stock information current */
                }

                include use case 'Fetch Stock Data';
                include use case 'Display Stock Information';
            }

            use case 'Handle Network Disconnection' {
                doc /*
                Device responds to WiFi network loss

                Precondition: Device was connected to WiFi
                Postcondition: Device attempts reconnection or displays error

                Basic Flow:
                1. WiFi network signal is lost
                2. Device detects connection loss
                3. Device displays "Disconnected" status
                4. Device attempts automatic reconnection
                5. If reconnection successful, resume normal operation
                6. If reconnection fails after timeout, display error message

                Alternate Flow:
                - User can manually initiate reconnection
                - User can configure different WiFi network
                */

                objective networkRecoveryObjective {
                    doc /* Maintain service despite network issues */
                }

                include use case 'Establish Network Connection';
                include use case 'Display Error Message';
            }

            use case 'Conserve Battery Power' {
                doc /*
                Device enters low-power mode when inactive

                Precondition: Device is idle (no button presses)
                Postcondition: Device is in sleep mode to save battery

                Basic Flow:
                1. User stops interacting with device
                2. After inactivity timeout (e.g., 2 minutes), device prepares for sleep
                3. Device saves current state to memory
                4. Device dims display to minimum brightness
                5. After additional timeout, device enters deep sleep mode
                6. Network connection is suspended
                7. Display is turned off

                Wake Flow:
                1. User presses any button
                2. Device wakes from sleep mode
                3. Device restores previous state
                4. Device re-establishes network connection
                5. Device refreshes stock data
                6. Device displays restored information
                */

                objective powerConservationObjective {
                    doc /* Extend battery life during periods of inactivity */
                }

                include use case 'Save Device State';
                include use case 'Restore Device State';
            }

            // =====================
            // INCLUDED USE CASES
            // =====================

            use case 'Establish Network Connection' {
                doc /*
                Connect to WiFi network

                1. Device scans for available networks
                2. Device selects configured network (SSID)
                3. Device authenticates using stored credentials
                4. WiFi Network grants connection
                5. Device receives IP address
                6. Connection status is "Connected"
                */
            }

            use case 'Fetch Stock Data' {
                doc /*
                Retrieve stock data from provider

                1. Device builds HTTP request with stock symbol
                2. Device sends GET request to Stock Data Provider API
                3. Stock Data Provider queries database
                4. Stock Data Provider returns JSON response
                5. Device parses JSON data
                6. Device validates data completeness
                */
            }

            use case 'Display Stock Information' {
                doc /*
                Render stock information on screen

                1. Device formats stock data for display
                2. Device clears previous display content
                3. Device renders stock symbol (large text)
                4. Device renders current price (large text)
                5. Device renders price change with color (green/red)
                6. Device renders percent change
                7. Device renders timestamp
                8. Device updates display backlight based on ambient light
                */
            }

            use case 'Handle Button Input' {
                doc /*
                Process user button presses

                1. User presses physical button
                2. Device detects button press (hardware interrupt)
                3. Device determines button type (Next, Previous, Select)
                4. Device determines press type (Short, Long, Double)
                5. Device routes command to appropriate handler
                6. Device provides visual feedback (brief flash or beep)
                */
            }

            use case 'Validate Stock Symbol' {
                doc /*
                Verify that stock symbol is valid

                1. User enters stock symbol
                2. Device sends validation request to Stock Data Provider
                3. Stock Data Provider checks if symbol exists
                4. Provider returns validation result
                5. Device informs user of result (valid/invalid)
                */
            }

            use case 'Display Configuration Menu' {
                doc /*
                Show configuration options on screen

                1. Device enters configuration mode
                2. Device displays menu options (Add Stock, Remove Stock, WiFi Settings, etc.)
                3. User navigates menu with buttons
                4. Device highlights selected option
                5. User confirms selection
                */
            }

            use case 'Display Error Message' {
                doc /*
                Show error information to user

                1. Error condition is detected
                2. Device determines error type and severity
                3. Device formats error message for display
                4. Device displays error icon and message
                5. Device suggests corrective action if applicable
                6. Device logs error for diagnostics
                */
            }

            use case 'Save Device State' {
                doc /*
                Persist current state to non-volatile memory

                1. Device prepares state data (current stock, watchlist, settings)
                2. Device serializes data to storage format
                3. Device writes data to flash memory
                4. Device verifies write success
                */
            }

            use case 'Restore Device State' {
                doc /*
                Load previously saved state from memory

                1. Device reads state data from flash memory
                2. Device deserializes data
                3. Device validates data integrity
                4. Device restores settings and watchlist
                5. Device resumes from last viewed stock
                */
            }
        }

        // ====================================================================
        // CELL 1.4 - MEASURES OF EFFECTIVENESS (Parameters Pillar)
        // ====================================================================

        package '4 Measures of Effectiveness' {
            doc /*
            Measures of Effectiveness (MoEs) - Quantifiable system characteristics

            These are stakeholder-visible metrics that measure how well the
            system meets their needs.
            */

            private import SysML::*;
            private import ScalarValues::*;

            attribute def MeasureOfEffectiveness {
                doc /* Base definition for all MoEs */
                attribute name : String;
                attribute description : String;
                attribute targetValue : Real;
                attribute threshold : Real;  // Minimum acceptable value
                attribute unit : String;
            }

            // =====================
            // PERFORMANCE MoEs
            // =====================

            attribute updateLatency : MeasureOfEffectiveness {
                doc /* Time from user request to display update */
                attribute :>> name = "Update Latency";
                attribute :>> description = "Time from button press to display refresh";
                attribute :>> targetValue = 2.0;
                attribute :>> threshold = 5.0;
                attribute :>> unit = "seconds";
            }

            attribute dataRefreshRate : MeasureOfEffectiveness {
                doc /* Frequency of automatic stock data updates */
                attribute :>> name = "Data Refresh Rate";
                attribute :>> description = "How often stock prices are updated";
                attribute :>> targetValue = 5.0;
                attribute :>> threshold = 15.0;
                attribute :>> unit = "seconds";
            }

            attribute displayResponseTime : MeasureOfEffectiveness {
                doc /* Time for display to render new information */
                attribute :>> name = "Display Response Time";
                attribute :>> description = "Screen refresh latency";
                attribute :>> targetValue = 0.3;
                attribute :>> threshold = 1.0;
                attribute :>> unit = "seconds";
            }

            // =====================
            // BATTERY LIFE MoEs
            // =====================

            attribute batteryLife : MeasureOfEffectiveness {
                doc /* Operating time on single charge */
                attribute :>> name = "Battery Life";
                attribute :>> description = "Continuous operation time on full charge";
                attribute :>> targetValue = 480.0;  // 8 hours
                attribute :>> threshold = 240.0;     // 4 hours minimum
                attribute :>> unit = "minutes";
            }

            attribute standbyTime : MeasureOfEffectiveness {
                doc /* Time in sleep mode before battery depletes */
                attribute :>> name = "Standby Time";
                attribute :>> description = "Duration in low-power mode";
                attribute :>> targetValue = 2880.0;  // 48 hours
                attribute :>> threshold = 1440.0;     // 24 hours minimum
                attribute :>> unit = "minutes";
            }

            // =====================
            // USABILITY MoEs
            // =====================

            attribute displayReadability : MeasureOfEffectiveness {
                doc /* Viewing distance in ambient light */
                attribute :>> name = "Display Readability";
                attribute :>> description = "Maximum viewing distance with 500 lux ambient light";
                attribute :>> targetValue = 60.0;  // cm
                attribute :>> threshold = 30.0;
                attribute :>> unit = "centimeters";
            }

            attribute buttonResponseTime : MeasureOfEffectiveness {
                doc /* Time from button press to visual feedback */
                attribute :>> name = "Button Response Time";
                attribute :>> description = "Latency of button press acknowledgment";
                attribute :>> targetValue = 0.1;
                attribute :>> threshold = 0.3;
                attribute :>> unit = "seconds";
            }

            attribute watchlistCapacity : MeasureOfEffectiveness {
                doc /* Number of stocks that can be monitored */
                attribute :>> name = "Watchlist Capacity";
                attribute :>> description = "Maximum stocks in watchlist";
                attribute :>> targetValue = 10.0;
                attribute :>> threshold = 5.0;
                attribute :>> unit = "stocks";
            }

            // =====================
            // CONNECTIVITY MoEs
            // =====================

            attribute networkReconnectTime : MeasureOfEffectiveness {
                doc /* Time to re-establish lost WiFi connection */
                attribute :>> name = "Network Reconnect Time";
                attribute :>> description = "Duration to recover from connection loss";
                attribute :>> targetValue = 5.0;
                attribute :>> threshold = 15.0;
                attribute :>> unit = "seconds";
            }

            attribute wifiRange : MeasureOfEffectiveness {
                doc /* Maximum distance from WiFi access point */
                attribute :>> name = "WiFi Range";
                attribute :>> description = "Effective range for reliable connection";
                attribute :>> targetValue = 15.0;  // meters
                attribute :>> threshold = 5.0;
                attribute :>> unit = "meters";
            }

            // =====================
            // RELIABILITY MoEs
            // =====================

            attribute meanTimeBetweenFailures : MeasureOfEffectiveness {
                doc /* Average operating time before failure */
                attribute :>> name = "Mean Time Between Failures";
                attribute :>> description = "Reliability metric (MTBF)";
                attribute :>> targetValue = 4380.0;  // ~6 months of continuous operation
                attribute :>> threshold = 720.0;      // 1 month minimum
                attribute :>> unit = "hours";
            }

            attribute errorRecoveryTime : MeasureOfEffectiveness {
                doc /* Time to recover from error condition */
                attribute :>> name = "Error Recovery Time";
                attribute :>> description = "Duration to return to normal operation after error";
                attribute :>> targetValue = 10.0;
                attribute :>> threshold = 30.0;
                attribute :>> unit = "seconds";
            }

            // =====================
            // COST MoEs
            // =====================

            attribute manufacturingCost : MeasureOfEffectiveness {
                doc /* Bill of materials cost */
                attribute :>> name = "Manufacturing Cost";
                attribute :>> description = "Total component and assembly cost";
                attribute :>> targetValue = 15.0;
                attribute :>> threshold = 25.0;
                attribute :>> unit = "USD";
            }

            attribute retailPrice : MeasureOfEffectiveness {
                doc /* Consumer purchase price */
                attribute :>> name = "Retail Price";
                attribute :>> description = "Target selling price";
                attribute :>> targetValue = 45.0;
                attribute :>> threshold = 60.0;
                attribute :>> unit = "USD";
            }
        }

        // ====================================================================
        // CELL 1.5 - RISK & HAZARDS (Safety & Reliability Pillar)
        // ====================================================================

        package '5 Risk and Hazards' {
            doc /*
            Risk and Hazards - Potential problems from stakeholder perspective

            Identifies hazards, assesses risks, and defines mitigations
            */

            private import SysML::*;
            private import ScalarValues::*;

            requirement def Hazard {
                attribute hazardId : String;
                attribute severity : String;     // Catastrophic, Critical, Marginal, Negligible
                attribute likelihood : String;   // Frequent, Probable, Occasional, Remote, Improbable
                attribute riskLevel : String;    // High, Medium, Low
                attribute mitigation : String;
            }

            // =====================
            // HARDWARE HAZARDS
            // =====================

            requirement HAZ_001 : Hazard {
                doc /* Battery overheating during charging */
                attribute :>> hazardId = "HAZ-001";
                attribute :>> severity = "Critical";
                attribute :>> likelihood = "Remote";
                attribute :>> riskLevel = "Medium";
                attribute :>> mitigation = "Implement temperature monitoring with automatic shutoff at 60°C";
            }

            requirement HAZ_002 : Hazard {
                doc /* USB port electrical short */
                attribute :>> hazardId = "HAZ-002";
                attribute :>> severity = "Marginal";
                attribute :>> likelihood = "Remote";
                attribute :>> riskLevel = "Low";
                attribute :>> mitigation = "Use overcurrent protection circuit on USB power input";
            }

            requirement HAZ_003 : Hazard {
                doc /* Display screen physical damage */
                attribute :>> hazardId = "HAZ-003";
                attribute :>> severity = "Marginal";
                attribute :>> likelihood = "Occasional";
                attribute :>> riskLevel = "Medium";
                attribute :>> mitigation = "Include protective screen cover or tempered glass";
            }

            // =====================
            // SOFTWARE HAZARDS
            // =====================

            requirement HAZ_004 : Hazard {
                doc /* Infinite loop causing device hang */
                attribute :>> hazardId = "HAZ-004";
                attribute :>> severity = "Marginal";
                attribute :>> likelihood = "Remote";
                attribute :>> riskLevel = "Low";
                attribute :>> mitigation = "Implement watchdog timer to reset device if frozen";
            }

            requirement HAZ_005 : Hazard {
                doc /* Memory leak causing eventual crash */
                attribute :>> hazardId = "HAZ-005";
                attribute :>> severity = "Marginal";
                attribute :>> likelihood = "Probable";
                attribute :>> riskLevel = "Medium";
                attribute :>> mitigation = "Regular memory leak testing and static code analysis";
            }

            requirement HAZ_006 : Hazard {
                doc /* Buffer overflow from malformed API response */
                attribute :>> hazardId = "HAZ-006";
                attribute :>> severity = "Critical";
                attribute :>> likelihood = "Remote";
                attribute :>> riskLevel = "Medium";
                attribute :>> mitigation = "Validate all API response lengths before parsing";
            }

            // =====================
            // NETWORK HAZARDS
            // =====================

            requirement HAZ_007 : Hazard {
                doc /* Man-in-the-middle attack on WiFi */
                attribute :>> hazardId = "HAZ-007";
                attribute :>> severity = "Critical";
                attribute :>> likelihood = "Occasional";
                attribute :>> riskLevel = "High";
                attribute :>> mitigation = "Use HTTPS/TLS for all API communication; validate certificates";
            }

            requirement HAZ_008 : Hazard {
                doc /* Denial of service from API rate limiting */
                attribute :>> hazardId = "HAZ-008";
                attribute :>> severity = "Marginal";
                attribute :>> likelihood = "Probable";
                attribute :>> riskLevel = "Medium";
                attribute :>> mitigation = "Implement exponential backoff and local caching";
            }

            // =====================
            // USER EXPERIENCE HAZARDS
            // =====================

            requirement HAZ_009 : Hazard {
                doc /* Incorrect stock data leading to bad trading decisions */
                attribute :>> hazardId = "HAZ-009";
                attribute :>> severity = "Critical";
                attribute :>> likelihood = "Remote";
                attribute :>> riskLevel = "Medium";
                attribute :>> mitigation = "Display data timestamp; add disclaimer about data accuracy";
            }

            requirement HAZ_010 : Hazard {
                doc /* Device displays stale data after network loss */
                attribute :>> hazardId = "HAZ-010";
                attribute :>> severity = "Marginal";
                attribute :>> likelihood = "Occasional";
                attribute :>> riskLevel = "Medium";
                attribute :>> mitigation = "Clear indication of 'disconnected' status; timestamp freshness check";
            }
        }
    }
}

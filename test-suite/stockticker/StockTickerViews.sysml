package StockTickerViews {
    doc /*
    Comprehensive View Definitions for Stock Ticker System

    This package contains tabular and symbolic views for analyzing and
    visualizing the stock ticker system from multiple perspectives.

    View Categories:
    - Requirements Tables
    - Parts/Structure Tables
    - States Tables
    - Actions Tables
    - Use Cases Tables
    - State Machine Diagrams
    - Action Flow Diagrams
    - Parts Tree Diagrams
    */

    private import StockTickerSystem::*;
    private import StockTickerLogical::*;
    private import StockTickerPhysical::*;
    private import StockTickerVariants::*;
    private import DS_Views::*;
    private import KerML::*;
    private import SysML::*;

    // ================================================================
    // TABULAR VIEWS (Tables)
    // ================================================================

    // Requirements Table - All System Requirements
    view 'Requirements Table' : DS_Views::TabularViews::rt,
                                 DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Complete table of all requirements across all files:
        - Functional Requirements (FR-1 through FR-7)
        - Performance Requirements (PERF-1 through PERF-3)
        - Interface Requirements (INT-1 through INT-3)
        - Connectivity Requirements (CONN-1 through CONN-3)
        - Power Requirements (PWR-1 through PWR-2)
        - Reliability Requirements (REL-1 through REL-2)
        - Storage Requirements (STOR-1 through STOR-2)
        - Error Handling Requirements (ERR-1 through ERR-6)
        */
        expose StockTickerSystem::*;  // Namespace exposition - exposes direct children only, not imports
    }

    // Parts Table - All Hardware Components
    view 'Parts and Components Table' : DS_Views::TabularViews::gt {
        doc /*
        Table showing all hardware parts and components:
        - Physical hardware (ESP32, Display, Buttons, Battery)
        - Logical components
        - Product configurations
        - Part relationships
        */
        expose StockTickerPhysical::*;
        expose StockTickerLogical::*;
        expose StockTickerVariants::*;
        filter @SysML::Systems::PartUsage | @SysML::Systems::PartDefinition;
    }

    // States Table - All State Machine States
    view 'States Table' : DS_Views::TabularViews::gt,
                          DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of all states across state machines:
        - Device Operational States
        - Network Connection States
        - Display States (from Variants)
        */
        expose StockTickerLogical::*;
        expose StockTickerVariants::*;
        // Filter will expose state elements naturally through expose
    }

    // Actions Table - All Actions and Behaviors
    view 'Actions Table' : DS_Views::TabularViews::gt,
                           DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of all actions defined in the system:
        - FetchStockData and sub-actions
        - SendHTTPRequest and sub-actions
        - UpdateDisplay and sub-actions
        - HandleButtonPress and sub-actions
        - ManageWiFiConnection and sub-actions
        - ValidateStockData
        - Parallel actions
        */
        expose StockTickerLogical::*;
        expose StockTickerVariants::*;
        // Filter will expose action elements naturally through expose
    }

    // Use Cases Table
    view 'Use Cases Table' : DS_Views::TabularViews::gt,
                             DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of all use cases:
        - ViewCurrentPrice
        - ViewMultipleStocks
        - ConfigureStockList
        - ViewHistoricalTrend
        - ReceivePriceAlert
        - ViewMarketStatus
        - SelectStock
        - UpdateDisplay
        - ConnectToWiFi
        - HandleError
        - EnterSleepMode
        */
        expose StockTickerSystem::*;
        // Filter will expose use case elements naturally through expose
    }

    // Verification Cases Table
    view 'Verification Cases Table' : DS_Views::TabularViews::gt,
                                      DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of all verification test cases from System and Physical files
        */
        expose StockTickerSystem::*;
        expose StockTickerPhysical::*;
        // Filter will expose verification case elements naturally through expose
    }

    // Analysis Cases Table
    view 'Analysis Cases Table' : DS_Views::TabularViews::gt,
                                  DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of all analysis cases:
        - BatteryLifeAnalysis
        - WiFiRangeAnalysis
        - ThermalAnalysis
        - MemoryUtilizationAnalysis
        - DisplayPowerOptimization
        - SPICommunicationAnalysis
        */
        expose StockTickerPhysical::*;
        // Filter will expose analysis case elements naturally through expose
    }

    // ================================================================
    // SYMBOLIC VIEWS - STATE MACHINES
    // ================================================================

    // Device Operational States - State Transition View
    view 'Device Operational States View' : DS_Views::SymbolicViews::stv,
                                             DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        State machine diagram showing complete device lifecycle:
        - poweringOn
        - idle
        - connectingWiFi
        - fetchingData
        - displayingData
        - errorState
        - deepSleep

        Shows all 15 transitions with guards and signals.
        */
        expose StockTickerLogical::DeviceOperationalStates::*;
    }

    // Network Connection States - State Transition View
    view 'Network Connection States View' : DS_Views::SymbolicViews::stv,
                                             DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        State machine diagram for WiFi connection management:
        - disconnected
        - scanning
        - authenticating
        - connected
        - reconnecting

        Shows all 7 transitions for connection lifecycle.
        */
        expose StockTickerLogical::NetworkConnectionStates::*;
    }

    // Display States - State Transition View
    view 'Display States View' : DS_Views::SymbolicViews::stv,
                                  DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        State machine for display brightness states from StockTickerVariants:
        - lowBrightness
        - mediumBrightness
        - highBrightness

        Part of StockTickerWithExhibits exhibit states.
        */
        expose StockTickerVariants::StockTickerWithExhibits::*;
        // Filter will expose state elements naturally through expose
    }

    // ================================================================
    // SYMBOLIC VIEWS - ACTIONS
    // ================================================================

    // Fetch Stock Data - Action Flow View
    view 'Fetch Stock Data Action View' : DS_Views::SymbolicViews::afv,
                                           DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Action flow diagram for complete API fetch sequence:
        - buildURL
        - sendRequest
        - validateResponse
        - parseJSON
        - handleError

        Shows decision nodes and control flow.
        */
        expose StockTickerLogical::FetchStockData::*;
    }

    // Send HTTP Request - Action Flow View
    view 'Send HTTP Request Action View' : DS_Views::SymbolicViews::afv,
                                            DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Action flow for HTTP communication:
        - createSocket
        - establishConnection
        - sendHeaders
        - receiveResponse
        - closeSocket
        */
        expose StockTickerLogical::SendHTTPRequest::*;
    }

    // Update Display - Action Flow View
    view 'Update Display Action View' : DS_Views::SymbolicViews::afv,
                                         DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Action flow for rendering stock information:
        - clearDisplay
        - drawBackground
        - drawSymbol
        - drawPrice
        - drawChange
        - drawTimestamp
        - updateBacklight
        */
        expose StockTickerLogical::UpdateDisplay::*;
    }

    // Handle Button Press - Action Flow View
    view 'Handle Button Press Action View' : DS_Views::SymbolicViews::afv,
                                              DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Action flow for processing user input:
        - Supports short, long, double press detection
        - Routes to selectNextStock, selectPreviousStock, toggleFavorite, enterSleepMode
        */
        expose StockTickerLogical::HandleButtonPress::*;
    }

    // Manage WiFi Connection - Action Flow View
    view 'Manage WiFi Connection Action View' : DS_Views::SymbolicViews::afv,
                                                 DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        WiFi lifecycle management actions:
        - scanForNetworks
        - selectNetwork
        - authenticate
        - maintainConnection
        - retryConnection
        */
        expose StockTickerLogical::ManageWiFiConnection::*;
    }

    // Parallel Data Fetch - Action Flow View
    view 'Parallel Data Fetch View' : DS_Views::SymbolicViews::afv,
                                       DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Fork/join parallel actions for fetching 3 stocks concurrently.
        From StockTickerVariants.sysml
        */
        expose StockTickerVariants::ParallelDataFetch::*;
    }

    // ================================================================
    // SYMBOLIC VIEWS - PARTS STRUCTURE (TREE VIEWS)
    // ================================================================

    // Physical Hardware - Parts Tree View
    view 'Physical Hardware Tree' : DS_Views::SymbolicViewsByExpression::PartsTreeView {
        doc /*
        Tree view of physical hardware structure:
        - PhysicalStockTicker
          - ESP32_TTGO_T_Display
          - ST7789_Display
          - GPIO_Button_0
          - GPIO_Button_35
          - LiPo_Battery_500mAh
        */
        expose StockTickerPhysical::*;
    }

    // Logical Components - Parts Tree View
    view 'Logical Components Tree' : DS_Views::SymbolicViewsByExpression::PartsTreeView {
        doc /*
        Tree view of logical architecture:
        - LogicalStockTicker
          - StockDataProcessor
          - DisplayController
          - NetworkManager
          - UserInputHandler
        */
        expose StockTickerLogical::*;
    }

    // Product Configurations - Parts Tree View
    view 'Product Configurations Tree' : DS_Views::SymbolicViewsByExpression::PartsTreeView {
        doc /*
        Tree view of product variants:
        - BasicConfiguration ($15)
        - PremiumConfiguration ($45)
        - PortableConfiguration ($35)
        */
        expose StockTickerVariants::*;
        filter @SysML::Systems::PartUsage | @SysML::Systems::PartDefinition;
    }

    // ================================================================
    // SYMBOLIC VIEWS - REQUIREMENTS (TREE VIEWS)
    // ================================================================

    // All Requirements - Requirements Tree View
    view 'Requirements Tree View' : DS_Views::SymbolicViewsByExpression::RequirementsTreeView {
        doc /*
        Tree view of all requirements organized by category:
        - Functional Requirements (FR-1 to FR-7)
        - Performance Requirements (PERF-1 to PERF-3)
        - Interface Requirements (INT-1 to INT-3)
        - Connectivity Requirements (CONN-1 to CONN-3)
        - Power Requirements (PWR-1 to PWR-2)
        - Reliability Requirements (REL-1 to REL-2)
        - Storage Requirements (STOR-1 to STOR-2)
        - Error Handling Requirements (ERR-1 to ERR-6)
        */
        expose StockTickerSystem::*;
    }

    // ================================================================
    // SYMBOLIC VIEWS - ACTIONS (TREE VIEWS)
    // ================================================================

    // All Actions - Actions Tree View
    view 'Actions Tree View' : DS_Views::SymbolicViewsByExpression::ActionsTreeView {
        doc /*
        Tree view of all actions showing hierarchical decomposition:
        - FetchStockData (with 5 sub-actions)
        - SendHTTPRequest (with 5 sub-actions)
        - UpdateDisplay (with 7 sub-actions)
        - HandleButtonPress (with 4 routing actions)
        - ManageWiFiConnection (with 5 sub-actions)
        - ValidateStockData
        */
        expose StockTickerLogical::*;
        expose StockTickerVariants::*;
    }

    // ================================================================
    // SYMBOLIC VIEWS - STATES (NESTED VIEWS)
    // ================================================================

    // All States - States Nested View
    view 'States Nested View' : DS_Views::SymbolicViewsByExpression::StatesNestedView {
        doc /*
        Nested view showing all states and sub-states:
        - DeviceOperationalStates (7 states)
        - NetworkConnectionStates (5 states)
        - Display brightness states (3 states)
        */
        expose StockTickerLogical::*;
        expose StockTickerVariants::*;
    }

    // ================================================================
    // SYMBOLIC VIEWS - USE CASES WITH SWIMLANES
    // ================================================================

    // System Context - General View with Use Cases
    view 'System Context View' : DS_Views::SymbolicViews::gv,
                                  DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        General view showing system context:
        - Stock Ticker System
        - Actors (EndUser, StockDataProvider, WiFiNetwork)
        - 11 use cases with include/extend relationships

        NOTE: To see swimlanes for include use cases:
        1. Right-click on an include use case element
        2. Select Display → Display Swimlanes
        3. Swimlanes will show subjects and actors as performers
        */
        expose StockTickerSystem::*;
    }

    // ================================================================
    // SYMBOLIC VIEWS - INTERCONNECTIONS
    // ================================================================

    // Hardware Interconnections - Interconnection View
    view 'Hardware Interconnections View' : DS_Views::SymbolicViews::iv,
                                             DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Shows physical connections between hardware components:
        - ESP32 SPI connections to Display
        - ESP32 GPIO connections to Buttons
        - Power connections to Battery
        */
        expose StockTickerPhysical::PhysicalStockTicker::*;
    }

    // Logical Interconnections - Interconnection View
    view 'Logical Interconnections View' : DS_Views::SymbolicViews::iv,
                                            DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Shows logical connections and data flows:
        - StockDataFlow (API to Display)
        - ButtonEventFlow (Button to Processor)
        - DisplayCommandFlow (Processor to Display)
        */
        expose StockTickerLogical::*;
    }

    // Product Line Connections - Interconnection View
    view 'Product Line Connections View' : DS_Views::SymbolicViews::iv,
                                            DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Shows connections in ConfiguredStockTickerSystem:
        - Connection bindings between stock service and device
        - Port mappings
        */
        expose StockTickerVariants::ConfiguredStockTickerSystem::*;
    }

    // ================================================================
    // COMPOSITE VIEWS - COMPLETE SYSTEM
    // ================================================================

    // Complete System Overview - General View
    view 'Complete System Overview' : DS_Views::SymbolicViews::gv,
                                       DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Comprehensive view exposing all major elements from all files:
        - Requirements
        - Use Cases
        - Parts (Physical and Logical)
        - States
        - Actions
        - Verifications
        - Analysis Cases

        Use this as a starting point for exploring the entire system.
        */
        expose StockTickerSystem::*;
        expose StockTickerLogical::*;
        expose StockTickerPhysical::*;
        expose StockTickerVariants::*;
    }
}

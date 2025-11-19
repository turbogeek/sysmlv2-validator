package StockTicker_Views_MagicGrid {
    doc /*
    View Definitions for MagicGrid-Structured Stock Ticker Model

    This package provides comprehensive views for analyzing the MagicGrid-structured
    model from multiple perspectives.

    View Organization:
    - Problem Domain Views (Stakeholder Needs, Context, Use Cases, Functions)
    - Solution Domain Views (Requirements, Architecture, Behavior, Verification)
    - Implementation Views (Hardware, Software, Integration)
    - Cross-Domain Views (Traceability, Allocation matrices)

    IMPORTANT: All exposition uses ::* (namespace) NOT ::** (recursive membership)
    to avoid exposing standard library imports.
    */

    private import SysML::*;
    private import ScalarValues::*;
    private import DS_Views::*;
    private import StockTicker_MagicGrid::*;
    private import StockTicker_ProblemDomain_BlackBox::*;
    private import StockTicker_ProblemDomain_WhiteBox::*;
    private import StockTicker_SolutionDomain::*;
    private import StockTicker_ImplementationDomain::*;

    // ========================================================================
    // PROBLEM DOMAIN VIEWS - BLACK BOX
    // ========================================================================

    // ====================================================================
    // Stakeholder Needs Views
    // ====================================================================

    view 'Stakeholder Needs Table' : DS_Views::TabularViews::rt,
                                      DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of all 25 stakeholder needs

        Displays:
        - SN-001 through SN-025
        - Source (End User, Day Trader, System Admin)
        - Category (Functional, Performance, Interface, etc.)
        - Priority (High, Medium, Low)
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'1 Stakeholder Needs'::*;
    }

    view 'Stakeholder Needs by Category' : DS_Views::TabularViews::gt,
                                            DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Stakeholder needs organized by category

        Categories:
        - Functional (7 needs)
        - Performance (3 needs)
        - Interface (3 needs)
        - Connectivity (2 needs)
        - Power (2 needs)
        - Reliability (2 needs)
        - Storage (2 needs)
        - Business (2 needs)
        - Physical (2 needs)
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'1 Stakeholder Needs'::*;
    }

    // ====================================================================
    // System Context Views
    // ====================================================================

    view 'System Context Diagram' : DS_Views::SymbolicViews::gv,
                                     DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        General view showing Stock Ticker system in its operating environment

        Shows:
        - Stock Ticker Device (system of interest)
        - External Participants: End User, Day Trader, Stock Data Provider, WiFi Network, Power Grid
        - 3 Operating Environments (Stock Monitoring, Mobile, Degraded Network)
        - Interactions and connections
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'2 System Context'::*;
    }

    view 'External Participants Table' : DS_Views::TabularViews::gt,
                                          DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of external actors and systems

        Lists:
        - End User (attributes: userName, watchlistSize)
        - Day Trader (tradingFrequency, requiresLowLatency)
        - Stock Data Provider (apiURL, updateInterval)
        - WiFi Network (networkSSID, securityType, signalStrength)
        - Power Grid (voltage, current)
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'2 System Context'::*;
    }

    view 'Item Flows Diagram' : DS_Views::SymbolicViews::gv,
                                 DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Shows data flows between system and environment

        Item Flows:
        - UserInput (button presses)
        - StockDataRequest
        - StockDataResponse
        - VisualDisplay
        - NetworkConnection
        - PowerFlow
        - ErrorNotification
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'2 System Context'::*;
    }

    // ====================================================================
    // Use Case Views
    // ====================================================================

    view 'Use Cases Diagram' : DS_Views::SymbolicViews::gv,
                                DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Use case diagram showing user interactions

        Actors:
        - End User
        - Day Trader
        - Stock Data Provider
        - WiFi Network

        Use Cases:
        - 6 primary use cases
        - 9 include use cases
        - include/extend relationships
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'3 Use Cases'::*;
    }

    view 'Use Cases Table' : DS_Views::TabularViews::gt,
                              DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Tabular listing of all use cases with objectives

        Primary Use Cases:
        1. View Current Stock Price
        2. View Multiple Stocks
        3. Configure Stock List
        4. Monitor Price Changes
        5. Handle Network Disconnection
        6. Conserve Battery Power

        Include Use Cases:
        - Establish Network Connection
        - Fetch Stock Data
        - Display Stock Information
        - etc.
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'3 Use Cases'::*;
    }

    // ====================================================================
    // Measures of Effectiveness Views
    // ====================================================================

    view 'Measures of Effectiveness Table' : DS_Views::TabularViews::gt,
                                              DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        All 13 system-level MoEs with targets and thresholds

        Performance MoEs:
        - updateLatency (target: 2.0s, threshold: 5.0s)
        - dataRefreshRate (5s / 15s)
        - displayResponseTime (0.3s / 1.0s)

        Battery MoEs:
        - batteryLife (480 min / 240 min)
        - standbyTime (2880 min / 1440 min)

        Usability, Connectivity, Reliability, Cost MoEs
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'4 Measures of Effectiveness'::*;
    }

    // ====================================================================
    // Risk & Hazards Views
    // ====================================================================

    view 'Hazards Table' : DS_Views::TabularViews::gt,
                            DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        All 10 identified hazards with risk assessment

        Hazards:
        - HAZ-001 through HAZ-010
        - Severity (Catastrophic, Critical, Marginal, Negligible)
        - Likelihood (Frequent, Probable, Occasional, Remote, Improbable)
        - Risk Level (High, Medium, Low)
        - Mitigation strategies
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'5 Risk and Hazards'::*;
    }

    // ========================================================================
    // PROBLEM DOMAIN VIEWS - WHITE BOX
    // ========================================================================

    // ====================================================================
    // Functional Analysis Views
    // ====================================================================

    view 'Top-Level Function Decomposition' : DS_Views::SymbolicViews::afv,
                                               DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Action flow view of top-level system function

        Shows:
        - Provide Real-Time Stock Information (root function)
        - 5 major functions in parallel composition:
          * Acquire Stock Data
          * Process User Input
          * Update Display
          * Manage Connectivity
          * Manage Power
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'1 Functional Analysis'::*;
    }

    view 'Acquire Stock Data Action Flow' : DS_Views::SymbolicViews::afv,
                                             DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Detailed action flow for stock data acquisition

        Sub-functions:
        - buildAPIRequest
        - sendHTTPRequest
        - receiveResponse
        - parseJSON
        - validateData
        - handleAPIError

        Shows decision nodes and control flow
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'1 Functional Analysis'::'AcquireStockData'::*;
    }

    view 'Process User Input Action Flow' : DS_Views::SymbolicViews::afv,
                                             DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Action flow for button input processing

        Sub-functions:
        - detectButtonPress
        - debounceButton
        - determinePressType
        - mapButtonToCommand
        - routeCommand
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'1 Functional Analysis'::'ProcessUserInput'::*;
    }

    view 'Update Display Action Flow' : DS_Views::SymbolicViews::afv,
                                         DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Action flow for display rendering

        Sub-functions (parallel composition):
        - clearDisplayBuffer
        - drawBackground
        - renderStockSymbol
        - renderPrice
        - renderChangeIndicator
        - renderTimestamp
        - renderStatusIndicators
        - adjustBacklight
        - transferBufferToDisplay
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'1 Functional Analysis'::'UpdateDisplay'::*;
    }

    view 'Manage Connectivity Action Flow' : DS_Views::SymbolicViews::afv,
                                              DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        WiFi connection management action flow

        Sub-functions:
        - scanForNetworks
        - selectNetwork
        - authenticate
        - obtainIPAddress
        - monitorConnection
        - detectDisconnection
        - retryConnection (with exponential backoff)

        Shows retry loop
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'1 Functional Analysis'::'ManageConnectivity'::*;
    }

    view 'Manage Power Action Flow' : DS_Views::SymbolicViews::afv,
                                        DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Power management action flow

        Sub-functions:
        - monitorBattery
        - calculateRuntime
        - detectInactivity
        - determinePowerMode
        - enterSleepMode (composite)
        - wakeFromSleep (composite)
        - controlPeripheralPower

        Shows decision nodes for power mode selection
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'1 Functional Analysis'::'ManagePower'::*;
    }

    // ====================================================================
    // Conceptual Subsystems Views
    // ====================================================================

    view 'Conceptual Subsystems Tree' : DS_Views::SymbolicViewsByExpression::PartsTreeView {
        doc /*
        Tree view of conceptual system architecture

        Shows:
        - Conceptual Stock Ticker System
          - Stock Data Processor
          - Display Controller
          - Network Manager
          - User Input Handler
          - Power Manager

        With conceptual interfaces
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'2 Conceptual Subsystems'::*;
    }

    view 'Conceptual Architecture Diagram' : DS_Views::SymbolicViews::gv,
                                              DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Architecture diagram showing subsystems and connections

        Shows:
        - 5 conceptual subsystems
        - 10 conceptual interfaces
        - Connections between subsystems
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'2 Conceptual Subsystems'::'Conceptual Stock Ticker System'::*;
    }

    view 'Conceptual Subsystems Table' : DS_Views::TabularViews::gt,
                                          DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of conceptual subsystems with responsibilities

        Columns:
        - Subsystem name
        - Allocated functions
        - Interfaces
        - Key state attributes
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'2 Conceptual Subsystems'::*;
    }

    // ====================================================================
    // Function Allocation Views
    // ====================================================================

    view 'Function Allocation Matrix' : DS_Views::TabularViews::gt,
                                         DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Matrix showing function-to-subsystem allocations

        Allocations:
        - Acquire Stock Data → Stock Data Processor
        - Process User Input → User Input Handler
        - Update Display → Display Controller
        - Manage Connectivity → Network Manager
        - Manage Power → Power Manager
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'3 Function Allocation'::*;
    }

    // ====================================================================
    // Subsystem MoEs Views
    // ====================================================================

    view 'Subsystem MoEs Table' : DS_Views::TabularViews::gt,
                                   DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of subsystem-level performance measures

        11 Subsystem MoEs:
        - Data Processor: latency, validation accuracy
        - Display Controller: refresh rate, render time
        - Network Manager: reconnect time, stability
        - Input Handler: response time, accuracy
        - Power Manager: efficiency, sleep/wake times

        Shows how subsystem MoEs contribute to system MoEs
        */
        expose StockTicker_ProblemDomain_WhiteBox::'2 White Box'::'4 MoEs for Subsystems'::*;
    }

    // ========================================================================
    // SOLUTION DOMAIN VIEWS - SYSTEM LEVEL
    // ========================================================================

    // ====================================================================
    // System Requirements Views
    // ====================================================================

    view 'System Requirements Table' : DS_Views::TabularViews::rt,
                                        DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        All formal system requirements

        Requirements:
        - FR-001 through FR-007 (Functional)
        - PERF-001 through PERF-003 (Performance)
        - INT-001 through INT-003 (Interface)
        - CONN-001 through CONN-003 (Connectivity)
        - PWR-001 through PWR-003 (Power)
        - REL-001 through REL-003 (Reliability)
        - SEC-001 through SEC-002 (Security)
        - BUS-001 (Business/Cost)

        Shows verification method and rationale
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'1 System Requirements'::*;
    }

    view 'Requirements by Category' : DS_Views::TabularViews::gt,
                                       DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        System requirements organized by category

        Categories:
        - Functional (7 requirements)
        - Performance (3)
        - Interface (3)
        - Connectivity (3)
        - Power (3)
        - Reliability (3)
        - Security (2)
        - Business (1)
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'1 System Requirements'::*;
    }

    // ====================================================================
    // Logical Subsystem Architecture Views
    // ====================================================================

    view 'Logical Architecture Tree' : DS_Views::SymbolicViewsByExpression::PartsTreeView {
        doc /*
        Tree view of logical subsystem architecture

        Shows:
        - Logical Stock Ticker System
          - Stock Data Processor Logical
          - Display Controller Logical
          - Network Manager Logical
          - User Input Handler Logical
          - Power Manager Logical

        With ports and interfaces
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'2 Logical Subsystem Architecture'::*;
    }

    view 'Logical Architecture Diagram' : DS_Views::SymbolicViews::gv,
                                           DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Logical architecture with interface connections

        Shows:
        - 5 logical subsystems
        - Detailed ports (HTTP, SPI, GPIO, ADC, etc.)
        - Interface connections with documentation
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'2 Logical Subsystem Architecture'::'Logical Stock Ticker System'::*;
    }

    view 'Logical Subsystem Interfaces Table' : DS_Views::TabularViews::gt,
                                                 DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of all logical interfaces

        Lists 14 interface definitions:
        - HTTPInterface
        - StockDataOutputInterface
        - SPIDisplayInterface
        - GPIOButtonInterface
        - ADCInterface
        - PowerSwitchInterface
        - etc.
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'2 Logical Subsystem Architecture'::*;
    }

    view 'Logical Subsystems Detail Table' : DS_Views::TabularViews::gt,
                                              DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Detailed table of logical subsystems

        For each subsystem shows:
        - Ports and interfaces
        - State attributes
        - Behavior references
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'2 Logical Subsystem Architecture'::*;
    }

    // ====================================================================
    // System Behavior Views
    // ====================================================================

    view 'Device Operational States Diagram' : DS_Views::SymbolicViews::stv,
                                                DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        State transition diagram for device lifecycle

        States:
        - poweringOn
        - initializing
        - ready
        - active
        - error
        - sleep
        - deepSleep

        Shows all 15+ transitions with triggers
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'3 System Behavior Models'::'DeviceOperationalStates'::*;
    }

    view 'Network Connection States Diagram' : DS_Views::SymbolicViews::stv,
                                                 DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        WiFi connection state machine

        States:
        - disconnected
        - scanning
        - authenticating
        - connected
        - reconnecting

        Shows authentication flow and retry logic
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'3 System Behavior Models'::'NetworkConnectionStates'::*;
    }

    view 'Power Management States Diagram' : DS_Views::SymbolicViews::stv,
                                              DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Power mode state machine

        States:
        - active
        - lowPower
        - sleep
        - deepSleep

        Transitions based on battery level and inactivity
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'3 System Behavior Models'::'PowerManagementStates'::*;
    }

    view 'Display Brightness States Diagram' : DS_Views::SymbolicViews::stv,
                                                 DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Display backlight control state machine

        States:
        - lowBrightness (60%)
        - mediumBrightness (80%)
        - highBrightness (100%)

        Transitions based on ambient light
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'3 System Behavior Models'::'DisplayBrightnessStates'::*;
    }

    // ====================================================================
    // System Parameters & Budgets Views
    // ====================================================================

    view 'Power Budget Table' : DS_Views::TabularViews::gt,
                                 DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Power consumption budget allocation

        Total: 500 mW
        - Display Controller: 200 mW
        - Processor: 150 mW
        - Network: 100 mW
        - Misc: 50 mW

        Shows constraints
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'4 System Parameters and Budgets'::'powerBudget'::*;
    }

    view 'Timing Budget Table' : DS_Views::TabularViews::gt,
                                  DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Update latency budget breakdown

        Total: 2.0 seconds
        - Network: 0.8s
        - Processing: 0.5s
        - Display: 0.3s
        - Margin: 0.4s
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'4 System Parameters and Budgets'::'updateLatencyBudget'::*;
    }

    view 'Memory Budget Table' : DS_Views::TabularViews::gt,
                                  DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        RAM allocation budget

        Total: 64,000 bytes
        - Data Processor: 24,000
        - Display Buffer: 32,400 (off-chip)
        - Network Buffer: 8,000
        - Input Handler: 1,000
        - Power Manager: 500
        - System Overhead: 10,000
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'4 System Parameters and Budgets'::'memoryBudget'::*;
    }

    view 'Cost Budget Table' : DS_Views::TabularViews::gt,
                                DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Bill of Materials cost budget

        Total: $25 (threshold), $15 (target)
        - MCU: $12.50
        - Battery: $3.50
        - Enclosure: $4.00
        - PCB/Assembly: $3.00
        - Misc: $2.00
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'4 System Parameters and Budgets'::'costBudget'::*;
    }

    // ====================================================================
    // System Verification Views
    // ====================================================================

    view 'Verification Cases Table' : DS_Views::TabularViews::gt,
                                       DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        All verification definitions

        Test Cases:
        - Verify FR-001: WiFi Data Retrieval
        - Verify FR-002: Watchlist Capacity
        - Verify FR-005: Persist Watchlist
        - Verify PERF-001: Update Latency
        - Verify PWR-001: Battery Life

        Analysis Cases:
        - Battery Life Analysis
        - WiFi Range Analysis

        Shows test method, procedure, success criteria
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'5 System Verification'::*;
    }

    // ========================================================================
    // IMPLEMENTATION DOMAIN VIEWS
    // ========================================================================

    // ====================================================================
    // Hardware Implementation Views
    // ====================================================================

    view 'Physical Hardware Tree' : DS_Views::SymbolicViewsByExpression::PartsTreeView {
        doc /*
        Tree view of physical hardware assembly

        Shows:
        - Physical Stock Ticker Device
          - ESP32 TTGO T-Display
          - LiPo Battery 500mAh
          - Plastic Enclosure

        With BOM cost breakdown
        */
        expose StockTicker_ImplementationDomain::'1 Hardware Implementation'::'Physical Stock Ticker Device'::*;
    }

    view 'Hardware Components Table' : DS_Views::TabularViews::gt,
                                        DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of physical components

        Lists:
        - ESP32 TTGO T-Display ($12.50)
        - LiPo Battery ($3.50)
        - Enclosure ($4.00)

        With manufacturer, part number, cost, lead time
        */
        expose StockTicker_ImplementationDomain::'1 Hardware Implementation'::*;
    }

    view 'Hardware Interconnections Diagram' : DS_Views::SymbolicViews::iv,
                                                DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Physical connections between hardware components

        Shows:
        - ESP32 GPIO pins to buttons
        - SPI bus to display controller
        - Battery connector
        - USB-C power
        - WiFi antenna

        With pin assignments
        */
        expose StockTicker_ImplementationDomain::'1 Hardware Implementation'::'Physical Stock Ticker Device'::*;
    }

    // ====================================================================
    // Software Implementation Views
    // ====================================================================

    view 'Software Architecture Tree' : DS_Views::SymbolicViewsByExpression::PartsTreeView {
        doc /*
        Tree view of firmware architecture

        Shows:
        - Stock Ticker Firmware
          - Data Processor Software
          - Display Controller Software
          - Network Manager Software
          - Input Handler Software
          - Power Manager Software
          - Storage Manager Software
          - Main Application

        With code size estimates
        */
        expose StockTicker_ImplementationDomain::'2 Software Implementation'::'Stock Ticker Firmware'::*;
    }

    view 'Software Modules Table' : DS_Views::TabularViews::gt,
                                     DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Table of software components

        For each module shows:
        - Language (C++)
        - Framework (Arduino/FreeRTOS)
        - Code size (LOC)
        - Libraries used
        - Source files
        */
        expose StockTicker_ImplementationDomain::'2 Software Implementation'::*;
    }

    // ========================================================================
    // CROSS-DOMAIN VIEWS - TRACEABILITY & ALLOCATION
    // ========================================================================

    // ====================================================================
    // Traceability Views
    // ====================================================================

    view 'Stakeholder Needs to System Requirements Trace' : DS_Views::TabularViews::gt,
                                                             DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Traceability matrix: Stakeholder Needs → System Requirements

        Shows which system requirements refine which stakeholder needs

        Example traces:
        - SN-001, SN-006 → FR-001 (WiFi data retrieval)
        - SN-002 → FR-002 (Watchlist capacity)
        - SN-008 → PERF-001 (Update latency)
        - SN-010 → PWR-001 (Battery life)
        */
        expose StockTicker_ProblemDomain_BlackBox::'1 Black Box'::'1 Stakeholder Needs'::*;
        expose StockTicker_SolutionDomain::'1 System Level'::'1 System Requirements'::*;
    }

    view 'Requirements to Verification Trace' : DS_Views::TabularViews::gt,
                                                 DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Traceability matrix: System Requirements → Verification Cases

        Shows verification coverage for each requirement

        Example:
        - FR-001 → Verify FR-001: WiFi Data Retrieval (Test)
        - PERF-001 → Verify PERF-001: Update Latency (Test)
        - PWR-001 → Verify PWR-001: Battery Life (Test)
        - PWR-001 → Battery Life Analysis (Analysis)
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'1 System Requirements'::*;
        expose StockTicker_SolutionDomain::'1 System Level'::'5 System Verification'::*;
    }

    view 'Logical to Physical Allocation Matrix' : DS_Views::TabularViews::gt,
                                                    DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Allocation matrix: Logical Subsystems → Physical Components

        Shows hardware/software allocation:

        Logical → Hardware:
        - Stock Data Processor → ESP32 WiFi + CPU
        - Display Controller → ST7789 LCD Controller
        - Network Manager → ESP32 WiFi Radio
        - Input Handler → GPIO 0, 35 (buttons)
        - Power Manager → ESP32 + TP4054 + Battery

        Logical → Software:
        - Stock Data Processor → Data Processor Software
        - Display Controller → Display Controller Software
        - Network Manager → Network Manager Software
        - Input Handler → Input Handler Software
        - Power Manager → Power Manager Software
        */
        expose StockTicker_SolutionDomain::'1 System Level'::'2 Logical Subsystem Architecture'::*;
        expose StockTicker_ImplementationDomain::'1 Hardware Implementation'::*;
        expose StockTicker_ImplementationDomain::'2 Software Implementation'::*;
    }

    // ====================================================================
    // Complete System Overview
    // ====================================================================

    view 'MagicGrid Complete System Overview' : DS_Views::SymbolicViews::gv,
                                                  DS_Views::SymbolicViewsByExpression::NonStandardLibraryElementFilter {
        doc /*
        Comprehensive view showing all major elements across domains

        Use as entry point for exploring the complete MagicGrid model

        Exposed elements from:
        - Problem Domain (Stakeholder Needs, Use Cases, Functions, Conceptual Subsystems)
        - Solution Domain (System Requirements, Logical Architecture, Behavior, Verification)
        - Implementation Domain (Physical Hardware, Software Modules)

        Note: This is a high-level overview. Navigate to specific views for details.
        */
        expose StockTicker_ProblemDomain_BlackBox::*;
        expose StockTicker_SolutionDomain::*;
        expose StockTicker_ImplementationDomain::*;
    }
}

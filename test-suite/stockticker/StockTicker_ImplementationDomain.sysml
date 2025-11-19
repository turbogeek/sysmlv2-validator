package StockTicker_ImplementationDomain {
    doc /*
    Implementation Domain - Physical realization of the solution

    Maps logical architecture to physical implementation:
    - Hardware components with actual part numbers
    - Software components and code allocation
    - Integration strategy
    - Deployment configuration

    Implementation allocation:
    - Logical Subsystems → Physical Hardware + Software
    - Interfaces → Physical connections (SPI, GPIO, WiFi, USB)
    */

    private import SysML::*;
    private import ScalarValues::*;
    private import StockTicker_SolutionDomain::'1 System Level'::'2 Logical Subsystem Architecture'::*;

    // ========================================================================
    // HARDWARE IMPLEMENTATION
    // ========================================================================

    package '1 Hardware Implementation' {
        doc /*
        Hardware Implementation - Physical components and connections

        Hardware Architecture:
        - ESP32 TTGO T-Display (integrated MCU + display module)
        - Additional buttons (GPIO 0 and 35 used)
        - LiPo battery (500mAh)
        - USB-C power/charging
        */

        private import SysML::*;
    private import ScalarValues::*;

        // =====================
        // PHYSICAL COMPONENT BASE
        // =====================

        part def PhysicalComponent {
            doc /* Base definition for all physical hardware */
            attribute manufacturer : String;
            attribute partNumber : String;
            attribute cost : Real {
                doc /* Unit cost in USD */
            }
            attribute leadTime : Integer {
                doc /* Lead time in days */
            }
        }

        // =====================
        // MAIN MODULE: ESP32 TTGO T-Display
        // =====================

        part def ESP32_TTGO_T_Display :> PhysicalComponent {
            doc /*
            LIL YGO TTGO T-Display ESP32 Module

            Integrated module combining:
            - ESP32-PICO-D4 SiP (System-in-Package)
            - ST7789V 1.14" TFT LCD (135 x 240 pixels, 65K colors)
            - USB-C connector
            - Battery charging circuit (TP4054)
            - Voltage regulators
            - Two physical buttons (GPIO 0, GPIO 35)

            Technical Specifications:
            - Processor: Xtensa LX6 dual-core @ 240 MHz
            - RAM: 520 KB SRAM (internal)
            - Flash: 4 MB SPI Flash
            - WiFi: 802.11 b/g/n (2.4 GHz)
            - Bluetooth: BLE 4.2
            - Display: 1.14" ST7789, 135x240, RGB565
            - Power: 3.3V logic, 5V USB input
            - Dimensions: 51.52mm × 25.04mm
            */

            attribute :>> manufacturer = "LILYGO";
            attribute :>> partNumber = "TTGO-T-Display";
            attribute :>> cost = 12.50;
            attribute :>> leadTime = 14;

            // Processor specifications
            attribute processorType : String = "ESP32-PICO-D4";
            attribute clockSpeed : Real = 240.0 {
                doc /* MHz */
            }
            attribute cores : Integer = 2;
            attribute ramSize : Integer = 520 {
                doc /* KB SRAM */
            }
            attribute flashSize : Integer = 4096 {
                doc /* KB (4 MB) */
            }

            // Connectivity
            attribute wifiProtocol : String = "802.11 b/g/n";
            attribute wifiFrequency : Real = 2400.0 {
                doc /* MHz */
            }
            attribute bluetoothVersion : String = "BLE 4.2";

            // Display
            attribute displayController : String = "ST7789V";
            attribute displaySize : Real = 1.14 {
                doc /* inches diagonal */
            }
            attribute displayResolution : String = "135 x 240";
            attribute displayColors : Integer = 65536 {
                doc /* 16-bit RGB565 */
            }

            // Power
            attribute operatingVoltage : Real = 3.3 {
                doc /* Volts (logic level) */
            }
            attribute inputVoltage : Real = 5.0 {
                doc /* Volts (USB input) */
            }
            attribute activeCurrent : Real = 135.0 {
                doc /* mA (typical active mode) */
            }
            attribute sleepCurrent : Real = 10.0 {
                doc /* mA (deep sleep) */
            }

            // Physical ports (GPIO assignments)
            port gpio0 : GPIO_Port {
                doc /* Button 0 (built-in, active low) */
                attribute pinNumber = 0;
                attribute pinMode = "INPUT_PULLUP";
            }

            port gpio35 : GPIO_Port {
                doc /* Button 1 (built-in, active low) */
                attribute pinNumber = 35;
                attribute pinMode = "INPUT";
            }

            port spi : SPI_Port {
                doc /* SPI connection to ST7789 display controller */
                attribute sckPin = 18;
                attribute mosiPin = 19;
                attribute csPin = 5;
                attribute dcPin = 16;
                attribute rstPin = 23;
                attribute blPin = 4 {
                    doc /* Backlight control */
                }
                attribute clockSpeed = 40.0 {
                    doc /* MHz */
                }
            }

            port usb : USB_C_Port {
                doc /* USB-C port for power and serial communication */
            }

            port batteryConnector : Battery_Connector {
                doc /* JST 1.25mm 2-pin connector for LiPo battery */
            }

            port wifiAntenna : WiFi_Antenna {
                doc /* Integrated PCB antenna */
                attribute antennaGain = 2.0 {
                    doc /* dBi */
                }
            }
        }

        // =====================
        // BATTERY
        // =====================

        part def LiPo_Battery_500mAh :> PhysicalComponent {
            doc /*
            Lithium Polymer rechargeable battery

            Specifications:
            - Capacity: 500 mAh
            - Nominal voltage: 3.7V
            - Fully charged: 4.2V
            - Cutoff voltage: 3.0V
            - Dimensions: ~30mm × 20mm × 5mm
            - Connector: JST 1.25mm 2-pin
            */

            attribute :>> manufacturer = "Generic";
            attribute :>> partNumber = "LP502030";
            attribute :>> cost = 3.50;
            attribute :>> leadTime = 7;

            attribute capacity : Real = 500.0 {
                doc /* mAh */
            }
            attribute nominalVoltage : Real = 3.7 {
                doc /* Volts */
            }
            attribute fullyChargedVoltage : Real = 4.2 {
                doc /* Volts */
            }
            attribute cutoffVoltage : Real = 3.0 {
                doc /* Volts */
            }
            attribute maxChargeCurrent : Real = 500.0 {
                doc /* mA (1C) */
            }

            port powerOutput : Power_Port;
        }

        // =====================
        // ENCLOSURE
        // =====================

        part def PlasticEnclosure :> PhysicalComponent {
            doc /*
            3D-printed or injection-molded plastic case

            Features:
            - Screen cutout for display
            - Button access holes
            - USB-C port cutout
            - Battery compartment
            - Snap-fit or screw assembly
            */

            attribute :>> manufacturer = "Custom";
            attribute :>> partNumber = "STK-CASE-001";
            attribute :>> cost = 4.00;
            attribute :>> leadTime = 10;

            attribute material : String = "ABS Plastic";
            attribute color : String = "Black";
            attribute dimensions : String = "60mm × 35mm × 15mm";
        }

        // =====================
        // PORT DEFINITIONS
        // =====================

        port def GPIO_Port {
            doc /* General Purpose Input/Output pin */
            attribute pinNumber : Integer;
            attribute pinMode : String;  // INPUT, OUTPUT, INPUT_PULLUP
            attribute voltage : Real = 3.3 {
                doc /* Volts (logic level) */
            }
        }

        port def SPI_Port {
            doc /* Serial Peripheral Interface bus */
            attribute sckPin : Integer;   // Clock
            attribute mosiPin : Integer;  // Master Out Slave In
            attribute misoPin : Integer;  // Master In Slave Out (not used for display)
            attribute csPin : Integer;    // Chip Select
            attribute dcPin : Integer;    // Data/Command
            attribute rstPin : Integer;   // Reset
            attribute blPin : Integer;    // Backlight
            attribute clockSpeed : Real;  // MHz
        }

        port def USB_C_Port {
            doc /* USB Type-C connector */
            attribute dataLines : Boolean = true;
            attribute powerDelivery : Boolean = false;
            attribute maxCurrent : Real = 1000.0 {
                doc /* mA (1A for charging) */
            }
        }

        port def Battery_Connector {
            doc /* JST connector for battery */
            attribute connectorType : String = "JST 1.25mm 2-pin";
        }

        port def WiFi_Antenna {
            doc /* WiFi antenna (PCB trace or external) */
            attribute antennaType : String;  // PCB, Ceramic, External
            attribute antennaGain : Real;    // dBi
        }

        port def Power_Port {
            doc /* Power supply connection */
            attribute voltage : Real;
            attribute maxCurrent : Real;
        }

        // =====================
        // ALLOCATION: LOGICAL TO PHYSICAL
        // =====================

        comment HardwareAllocation /*
        HARDWARE ALLOCATION MATRIX

        Logical Subsystem → Physical Hardware Component

        1. Stock Data Processor → ESP32 Processor (WiFi + CPU)
           - WiFi radio for API communication
           - Dual-core CPU for JSON parsing and data processing

        2. Display Controller → ST7789 Display Controller (on TTGO module)
           - Integrated LCD controller
           - SPI interface from ESP32

        3. Network Manager → ESP32 WiFi Radio
           - 802.11 b/g/n radio
           - TCP/IP stack in firmware

        4. User Input Handler → GPIO 0 and GPIO 35 (physical buttons)
           - Built-in buttons on TTGO module
           - GPIO interrupt capability

        5. Power Manager → ESP32 + TP4054 Charge Controller + Battery
           - ESP32 deep sleep modes
           - ADC for battery voltage monitoring
           - TP4054 for LiPo charging management

        INTERFACE ALLOCATION

        Logical Interface → Physical Connection

        - HTTPInterface → WiFi Radio (2.4GHz 802.11n)
        - StockDataOutputInterface → Internal (memory/function call)
        - SPIDisplayInterface → SPI bus (pins 18, 19, 5, 16, 23, 4)
        - GPIOButtonInterface → GPIO 0, GPIO 35 (input with pullup)
        - ADCInterface → ADC1_CH6 (for battery voltage sensing)
        - PowerSwitchInterface → GPIO outputs (backlight, WiFi enable)
        */

        // =====================
        // COMPLETE PHYSICAL DEVICE
        // =====================

        part def 'Physical Stock Ticker Device' :> ESP32_TTGO_T_Display {
            doc /*
            Complete physical implementation of the Stock Ticker device

            This is the assembled product integrating:
            - ESP32 TTGO T-Display module (main component)
            - LiPo battery (500mAh)
            - Enclosure/housing
            - All necessary connections and wiring

            Bill of Materials:
            1. ESP32 TTGO T-Display module - $12.50
            2. LiPo Battery 500mAh - $3.50
            3. Plastic enclosure - $4.00
            4. Misc hardware (screws, standoffs) - $0.50
            ----------------------------------------
            Total BOM Cost: $20.50 (within $25 budget)
            */

            attribute productName : String = "Stock Ticker Display Device";
            attribute version : String = "1.0";

            part mainModule : ESP32_TTGO_T_Display;
            part battery : LiPo_Battery_500mAh;
            part enclosure : PlasticEnclosure;

            // Physical connections
            connect mainModule.batteryConnector to battery.powerOutput {
                doc /* Battery provides power to module */
            }

            // Attributes
            attribute totalCost : Real = 20.50 {
                doc /* Sum of component costs */
            }

            attribute weight : Real = 35.0 {
                doc /* grams */
            }

            attribute dimensions : String = "60mm × 35mm × 15mm (with enclosure)";
        }
    }

    // ========================================================================
    // SOFTWARE IMPLEMENTATION
    // ========================================================================

    package '2 Software Implementation' {
        doc /*
        Software Implementation - Firmware architecture and code allocation

        Platform: ESP32 Arduino Framework
        Language: C++
        RTOS: FreeRTOS (built-in)
        Libraries: WiFi, HTTPClient, TFT_eSPI, ArduinoJson, SPIFFS/LittleFS
        */

        private import SysML::*;
    private import ScalarValues::*;

        // =====================
        // SOFTWARE COMPONENT BASE
        // =====================

        part def SoftwareComponent {
            doc /* Base definition for software modules */
            attribute programmingLanguage : String;
            attribute framework : String;
            attribute codeSize : Integer {
                doc /* Estimated lines of code */
            }
            attribute libraries : String;
        }

        // =====================
        // SOFTWARE MODULE 1: DATA PROCESSOR
        // =====================

        part def StockDataProcessorSoftware :> SoftwareComponent {
            doc /*
            Software implementation of Stock Data Processor subsystem

            Responsibilities:
            - HTTP/HTTPS client for API communication
            - JSON parsing of stock data responses
            - Data validation and error handling
            - Watchlist management
            - Local caching

            Libraries Used:
            - WiFiClientSecure (HTTPS/TLS)
            - HTTPClient (HTTP requests)
            - ArduinoJson (JSON parsing)
            */

            attribute :>> programmingLanguage = "C++";
            attribute :>> framework = "Arduino (ESP32)";
            attribute :>> codeSize = 800;
            attribute :>> libraries = "WiFiClientSecure, HTTPClient, ArduinoJson";

            // Software modules (classes/functions)
            attribute apiClientModule : String = "StockAPIClient.cpp";
            attribute jsonParserModule : String = "StockDataParser.cpp";
            attribute cacheManagerModule : String = "DataCache.cpp";
            attribute validatorModule : String = "DataValidator.cpp";

            // Allocates to logical subsystem
            // allocate to StockDataProcessorLogical;
        }

        // =====================
        // SOFTWARE MODULE 2: DISPLAY CONTROLLER
        // =====================

        part def DisplayControllerSoftware :> SoftwareComponent {
            doc /*
            Software implementation of Display Controller subsystem

            Responsibilities:
            - TFT display driver (ST7789)
            - Framebuffer management
            - Rendering text and graphics
            - Backlight PWM control
            - UI layout management

            Libraries Used:
            - TFT_eSPI (optimized display driver for ESP32)
            */

            attribute :>> programmingLanguage = "C++";
            attribute :>> framework = "Arduino (ESP32)";
            attribute :>> codeSize = 600;
            attribute :>> libraries = "TFT_eSPI";

            attribute displayDriverModule : String = "DisplayDriver.cpp";
            attribute uiRendererModule : String = "UIRenderer.cpp";
            attribute backlightControlModule : String = "BacklightControl.cpp";

            // allocate to DisplayControllerLogical;
        }

        // =====================
        // SOFTWARE MODULE 3: NETWORK MANAGER
        // =====================

        part def NetworkManagerSoftware :> SoftwareComponent {
            doc /*
            Software implementation of Network Manager subsystem

            Responsibilities:
            - WiFi connection management
            - Network scanning and authentication
            - Connection monitoring and recovery
            - Retry logic with exponential backoff

            Libraries Used:
            - WiFi (ESP32 WiFi library)
            */

            attribute :>> programmingLanguage = "C++";
            attribute :>> framework = "Arduino (ESP32)";
            attribute :>> codeSize = 500;
            attribute :>> libraries = "WiFi";

            attribute wifiManagerModule : String = "WiFiManager.cpp";
            attribute connectionMonitorModule : String = "ConnectionMonitor.cpp";
            attribute retryLogicModule : String = "RetryLogic.cpp";

            // allocate to NetworkManagerLogical;
        }

        // =====================
        // SOFTWARE MODULE 4: INPUT HANDLER
        // =====================

        part def InputHandlerSoftware :> SoftwareComponent {
            doc /*
            Software implementation of User Input Handler subsystem

            Responsibilities:
            - GPIO interrupt handling
            - Button debouncing
            - Press type classification (short/long/double)
            - Command routing

            Libraries Used:
            - Arduino GPIO (built-in)
            */

            attribute :>> programmingLanguage = "C++";
            attribute :>> framework = "Arduino (ESP32)";
            attribute :>> codeSize = 300;
            attribute :>> libraries = "Arduino Core (GPIO)";

            attribute buttonHandlerModule : String = "ButtonHandler.cpp";
            attribute debounceModule : String = "Debounce.cpp";
            attribute commandMapperModule : String = "CommandMapper.cpp";

            // allocate to UserInputHandlerLogical;
        }

        // =====================
        // SOFTWARE MODULE 5: POWER MANAGER
        // =====================

        part def PowerManagerSoftware :> SoftwareComponent {
            doc /*
            Software implementation of Power Manager subsystem

            Responsibilities:
            - ADC battery voltage monitoring
            - Power mode state machine
            - Inactivity detection
            - Deep sleep control
            - Peripheral power control

            Libraries Used:
            - ESP32 ADC
            - ESP32 Sleep library
            */

            attribute :>> programmingLanguage = "C++";
            attribute :>> framework = "Arduino (ESP32)";
            attribute :>> codeSize = 400;
            attribute :>> libraries = "ESP32 ADC, esp_sleep";

            attribute batteryMonitorModule : String = "BatteryMonitor.cpp";
            attribute powerModeManagerModule : String = "PowerModeManager.cpp";
            attribute sleepControlModule : String = "SleepControl.cpp";

            // allocate to PowerManagerLogical;
        }

        // =====================
        // SOFTWARE MODULE 6: STORAGE MANAGER
        // =====================

        part def StorageManagerSoftware :> SoftwareComponent {
            doc /*
            Persistent storage management

            Responsibilities:
            - Save/load watchlist
            - Save/load WiFi credentials (encrypted)
            - Save/load user preferences
            - SPIFFS or LittleFS filesystem

            Libraries Used:
            - SPIFFS or LittleFS
            */

            attribute :>> programmingLanguage = "C++";
            attribute :>> framework = "Arduino (ESP32)";
            attribute :>> codeSize = 250;
            attribute :>> libraries = "SPIFFS or LittleFS";

            attribute fileSystemModule : String = "StorageManager.cpp";
            attribute watchlistPersistenceModule : String = "WatchlistStorage.cpp";
            attribute credentialsStorageModule : String = "SecureStorage.cpp";
        }

        // =====================
        // MAIN APPLICATION
        // =====================

        part def MainApplication :> SoftwareComponent {
            doc /*
            Main application entry point and coordination

            Responsibilities:
            - System initialization
            - Task scheduling (FreeRTOS)
            - Inter-module communication
            - State machine coordination

            Files:
            - main.cpp (setup() and loop())
            - TaskScheduler.cpp
            */

            attribute :>> programmingLanguage = "C++";
            attribute :>> framework = "Arduino (ESP32) + FreeRTOS";
            attribute :>> codeSize = 400;
            attribute :>> libraries = "FreeRTOS (built-in)";

            attribute mainModule : String = "main.cpp";
            attribute taskSchedulerModule : String = "TaskScheduler.cpp";
        }

        // =====================
        // SOFTWARE ARCHITECTURE
        // =====================

        part 'Stock Ticker Firmware' {
            doc /*
            Complete firmware architecture

            Total estimated code size: ~3,250 lines of C++

            Task Architecture (FreeRTOS):
            - DataFetchTask: Periodic stock data updates (priority 2)
            - DisplayUpdateTask: Screen rendering (priority 3)
            - ButtonInputTask: Button event handling (priority 4 - highest)
            - NetworkMonitorTask: Connection health monitoring (priority 1)
            - PowerManagementTask: Battery monitoring and sleep control (priority 1)
            */

            part dataProcessorSW : StockDataProcessorSoftware;
            part displayControllerSW : DisplayControllerSoftware;
            part networkManagerSW : NetworkManagerSoftware;
            part inputHandlerSW : InputHandlerSoftware;
            part powerManagerSW : PowerManagerSoftware;
            part storageSW : StorageManagerSoftware;
            part mainApp : MainApplication;

            attribute totalCodeSize : Integer = 3250 {
                doc /* Lines of C++ code */
            }

            attribute compiledSize : Integer = 350000 {
                doc /* Estimated compiled binary size in bytes (350 KB) */
            }

            attribute flashUtilization : Real = 8.5 {
                doc /* Percent of 4MB flash used */
            }

            attribute ramUtilization : Real = 45.0 {
                doc /* Percent of 520KB RAM used */
            }
        }
    }

    // ========================================================================
    // INTEGRATION & TEST
    // ========================================================================

    package '3 Integration and Test' {
        doc /*
        Integration and Test Strategy

        Integration Levels:
        1. Unit Testing (individual modules)
        2. Subsystem Integration (combine related modules)
        3. System Integration (full system)
        4. System Test (end-to-end verification)
        */

        private import SysML::*;
        private import ScalarValues::*;
        private import '1 Hardware Implementation'::*;

        // =====================
        // INTEGRATION STRATEGY
        // =====================

        comment IntegrationStrategy /*
        INTEGRATION SEQUENCE

        Phase 1: Hardware Bring-Up
        - Power-on test (battery, USB, voltage regulators)
        - GPIO test (buttons functional)
        - Display test (SPI communication, pixel rendering)
        - WiFi radio test (scan for networks)

        Phase 2: Subsystem Integration
        - Data Processor + Network Manager
          → Test: Fetch data from API, verify JSON parsing
        - Display Controller
          → Test: Render stock data on screen
        - Input Handler + Data Processor
          → Test: Button navigation through watchlist
        - Power Manager + Battery Monitor
          → Test: Sleep/wake cycles

        Phase 3: System Integration
        - Integrate all subsystems
        - End-to-end flow: Button → Fetch → Display
        - Error handling paths
        - State machine transitions

        Phase 4: System Testing
        - Execute all verification cases from Solution Domain
        - Performance testing (latency, refresh rate)
        - Battery life testing
        - Stress testing (extended operation, many stock queries)
        - Edge case testing (network loss, invalid data, etc.)
        */

        // =====================
        // TEST SETUP
        // =====================

        part def TestSetup {
            doc /* Equipment and configuration for testing */

            attribute testEnvironment : String = "Lab Environment";
            attribute wifiNetwork : String = "Test-AP-2.4GHz";
            attribute apiEndpoint : String = "https://api.example.com/stock";
            attribute oscilloscope : String = "For power consumption measurement";
            attribute multimeter : String = "For voltage/current measurement";
        }

        // =====================
        // INTEGRATION TEST CASES
        // =====================

        verification def 'Integration Test: WiFi + API Fetch' {
            doc /*
            Test WiFi connection and API data fetch

            Steps:
            1. Power on device
            2. Monitor WiFi connection process (scan, auth, DHCP)
            3. Trigger API fetch for AAPL
            4. Verify JSON response received
            5. Verify data parsed correctly

            Pass Criteria:
            - WiFi connects within 10 seconds
            - API response received within 2 seconds of request
            - JSON parsed with no errors
            */

            subject system : 'Physical Stock Ticker Device';
        }

        verification def 'Integration Test: Display Rendering' {
            doc /*
            Test display controller with live data

            Steps:
            1. Fetch stock data (AAPL)
            2. Trigger display update
            3. Visually inspect rendered output
            4. Measure SPI transfer time with oscilloscope

            Pass Criteria:
            - Stock symbol displayed (large, top)
            - Price displayed (large, center)
            - Change displayed with correct color (green/red)
            - Timestamp displayed (small, bottom)
            - Rendering complete within 0.3 seconds
            */

            subject system : 'Physical Stock Ticker Device';
        }

        verification def 'Integration Test: Button Navigation' {
            doc /*
            Test button input and stock cycling

            Steps:
            1. Configure watchlist with 5 stocks
            2. Press Next button
            3. Verify display updates to next stock
            4. Measure latency from button press to display update
            5. Repeat for all stocks in watchlist

            Pass Criteria:
            - Each button press advances to next stock
            - Latency < 2.0 seconds per press
            - Wrap-around at end of list
            */

            subject system : 'Physical Stock Ticker Device';
        }

        verification def 'Integration Test: Sleep/Wake Cycle' {
            doc /*
            Test power management and deep sleep

            Steps:
            1. Device in active mode
            2. Wait for inactivity timeout (2 minutes)
            3. Verify display dims/turns off
            4. Measure current consumption (should drop to ~10mA)
            5. Press button to wake
            6. Verify device wakes and restores state

            Pass Criteria:
            - Device enters sleep after 2 minutes
            - Current drops to < 20mA in sleep
            - Wake latency < 3 seconds
            - State restored correctly (same stock displayed)
            */

            subject system : 'Physical Stock Ticker Device';
        }
    }

    // ========================================================================
    // DEPLOYMENT
    // ========================================================================

    package '4 Deployment' {
        doc /*
        Deployment Configuration and Procedures

        Deployment activities:
        - Firmware flashing
        - Initial configuration (WiFi credentials, default watchlist)
        - Quality assurance checks
        - Packaging and shipping
        */

        private import SysML::*;
        private import ScalarValues::*;
        private import '1 Hardware Implementation'::*;

        // =====================
        // FIRMWARE DEPLOYMENT
        // =====================

        comment FirmwareDeployment /*
        FIRMWARE FLASHING PROCEDURE

        Equipment:
        - USB-C cable
        - Computer with Arduino IDE or PlatformIO

        Steps:
        1. Connect device to computer via USB-C
        2. Select board: "ESP32 Dev Module" or "TTGO T-Display"
        3. Select port: (detect USB serial port)
        4. Compile firmware (verify no errors)
        5. Flash firmware to device
        6. Open serial monitor (115200 baud)
        7. Verify boot messages (successful initialization)

        Flash Layout:
        - 0x1000: Bootloader
        - 0x8000: Partition table
        - 0x10000: Application firmware (350 KB)
        - 0xE0000: SPIFFS filesystem (1 MB for storage)

        Total Flash Usage: ~1.35 MB / 4 MB (33%)
        */

        // =====================
        // INITIAL CONFIGURATION
        // =====================

        comment InitialConfiguration /*
        DEVICE CONFIGURATION PROCEDURE

        First-Time Setup:
        1. Power on device
        2. Device displays "WiFi Setup Required"
        3. Enter configuration mode (hold Select button on boot)
        4. Device displays available WiFi networks
        5. Use buttons to select network
        6. Use companion app or web interface to enter password
        7. Device saves credentials to secure storage
        8. Device connects to WiFi
        9. Configure default watchlist:
           - Default: AAPL, MSFT, GOOGL, AMZN, TSLA
        10. Save configuration

        Configuration is persisted in SPIFFS:
        - /config/wifi.json (encrypted)
        - /config/watchlist.json
        - /config/preferences.json
        */

        // =====================
        // QUALITY ASSURANCE
        // =====================

        verification def 'QA Check: Power-On Test' {
            doc /*
            Basic power-on functionality check before shipping

            Checklist:
            [ ] Device powers on from USB
            [ ] Device powers on from battery
            [ ] Display shows boot screen
            [ ] WiFi scan completes
            [ ] Both buttons respond to presses
            [ ] Battery charges from USB
            [ ] No physical defects (screen, case, buttons)

            Duration: ~5 minutes per unit
            */

            subject device : 'Physical Stock Ticker Device';
        }

        // =====================
        // PACKAGING
        // =====================

        comment PackagingSpecification /*
        PACKAGING CONTENTS

        Box includes:
        1. Stock Ticker Device (assembled)
        2. USB-C charging cable (1m)
        3. Quick Start Guide (printed card)
        4. Safety information

        Box dimensions: 100mm × 80mm × 40mm
        Shipping weight: 60g (device + packaging)

        Quick Start Guide Contents:
        - How to power on
        - How to configure WiFi (QR code to setup web page)
        - How to navigate stocks (button operation)
        - How to charge battery
        - Troubleshooting tips
        - Support contact information
        */

        // =====================
        // USER DOCUMENTATION
        // =====================

        comment UserDocumentation /*
        USER MANUAL TOPICS

        1. Getting Started
           - Unboxing
           - First power-on
           - WiFi configuration
           - Setting up watchlist

        2. Daily Use
           - Viewing stock prices
           - Cycling through stocks
           - Understanding display elements
           - Battery charging

        3. Configuration
           - Adding/removing stocks
           - Changing WiFi network
           - Adjusting refresh rate
           - Setting sleep timeout

        4. Troubleshooting
           - WiFi won't connect
           - Stock data not updating
           - Display issues
           - Battery problems

        5. Technical Specifications
           - Supported WiFi networks
           - Power consumption and battery life
           - Dimensions and weight

        6. Safety and Regulatory
           - FCC compliance
           - Battery safety
           - Disposal instructions

        Available formats:
        - PDF (downloadable from website)
        - Web-based (responsive HTML)
        - Video tutorials (YouTube)
        */

        // =====================
        // FIELD UPDATES
        // =====================

        comment FieldUpdateStrategy /*
        OVER-THE-AIR (OTA) FIRMWARE UPDATES

        Capability:
        - ESP32 supports OTA updates via WiFi
        - User-initiated from device menu
        - Automatic update checks (optional)

        Update Procedure:
        1. Device checks server for new firmware version
        2. If available, prompts user to update
        3. User confirms update
        4. Device downloads new firmware
        5. Validates firmware signature/checksum
        6. Writes to alternate flash partition
        7. Reboots to new firmware
        8. If successful, marks partition as boot partition
        9. If failure, rolls back to previous firmware

        Update Server:
        - HTTPS endpoint: https://updates.example.com/stock-ticker
        - Firmware binary + version manifest
        - Digital signature for validation

        Fallback:
        - If OTA fails, device remains functional on old firmware
        - User can perform manual USB flash if needed
        */
    }
}

package StockTickerVariants {
    doc /*
     * Stock Ticker Device - Product Variants
     *
     * This model demonstrates product line variants using only
     * features currently supported by Cameo Systems Modeler.
     *
     * Features demonstrated:
     * - Metadata definitions
     * - Part definitions with configurations
     * - Connection bindings
     * - Constraint definitions
     */

    private import ScalarValues::*;
    private import SI::*;
    private import StockTickerSystem::*;
    private import StockTickerPhysical::*;

    // ========================================
    // METADATA DEFINITIONS
    // ========================================

    metadata def SecurityClassification {
        doc /* Security classification for requirements */
        attribute level : String;
        attribute clearanceRequired : Boolean;
    }

    metadata def Traceability {
        doc /* Traceability to external documents */
        attribute documentId : String;
        attribute section : String;
        attribute version : String;
    }

    metadata def ModelingAssumptions {
        doc /* Assumptions made during modeling */
        attribute description : String;
        attribute validUntil : String;
    }

    // ========================================
    // PRODUCT CONFIGURATIONS
    // ========================================

    part def BasicConfiguration :> PhysicalStockTicker {
        doc /*
         * Basic stock ticker configuration
         * Minimal features for cost-sensitive applications
         * Target price: $15 USD
         * Market: Education
         */

        attribute targetPrice : Real = 15.0 {
            doc /* USD */
        }

        attribute marketSegment : String = "Education";

        attribute powerSource : String = "USB Only";
        attribute displaySize : String = "1.14 inch ST7789";
        attribute connectivity : String = "WiFi Only";
    }

    part def PremiumConfiguration :> PhysicalStockTicker {
        doc /*
         * Premium stock ticker with enhanced features
         * For professional trading desks
         * Target price: $45 USD
         * Market: Professional
         */

        attribute targetPrice : Real = 45.0 {
            doc /* USD */
        }

        attribute marketSegment : String = "Professional";

        attribute powerSource : String = "Battery + USB Charging";
        attribute displaySize : String = "2.0 inch ST7789";
        attribute connectivity : String = "WiFi + Bluetooth";
        attribute batteryCapacity : Real = 1000.0 {
            doc /* mAh */
        }
    }

    part def PortableConfiguration :> PhysicalStockTicker {
        doc /*
         * Battery-powered portable configuration
         * Optimized for mobile users with solar assistance
         * Target price: $35 USD
         * Market: Consumer
         */

        attribute targetPrice : Real = 35.0 {
            doc /* USD */
        }

        attribute marketSegment : String = "Consumer";

        attribute powerSource : String = "Solar Assisted Battery";
        attribute displaySize : String = "2.9 inch E-Ink";
        attribute connectivity : String = "WiFi Only";
        attribute batteryLifeDays : Real = 7.0;
    }

    // ========================================
    // EXHIBIT STATES
    // ========================================

    part def StockTickerWithExhibits :> PhysicalStockTicker {
        doc /*
         * Stock ticker demonstrating exhibit states
         * Shows part-state relationships
         */

        /* Display component with its own states */
        part display {
            state displayStates {
                entry; then off;

                state off {
                    doc /* Display is powered off */
                }

                state dimmed {
                    doc /* Display is dimmed to save power */
                }

                state normal {
                    doc /* Display at normal brightness */
                }

                state bright {
                    doc /* Display at maximum brightness */
                }

                transition off_to_normal
                    first off
                    then normal;

                transition normal_to_dimmed
                    first normal
                    then dimmed;

                transition dimmed_to_normal
                    first dimmed
                    then normal;

                transition normal_to_bright
                    first normal
                    then bright;

                transition bright_to_normal
                    first bright
                    then normal;
            }
        }
    }

    // ========================================
    // PARALLEL ACTIONS WITH FORK/JOIN
    // ========================================

    action def ParallelDataFetch {
        doc /*
         * Fetch multiple stock symbols in parallel
         * Demonstrates fork/join parallelism
         */

        action initiateRequests {
            doc /* Prepare all requests */
        }
        first initiateRequests;

        fork;
        then fetch1;
        then fetch2;
        then fetch3;

        action fetch1 {
            doc /* Fetch first stock symbol */
        }

        action fetch2 {
            doc /* Fetch second stock symbol */
        }

        action fetch3 {
            doc /* Fetch third stock symbol */
        }

        first fetch1 then joinResults;
        first fetch2 then joinResults;
        first fetch3 then joinResults;

        join joinResults;
        then consolidateData;

        action consolidateData {
            doc /* Merge all fetched data */
        }
    }

    // ========================================
    // CONNECTION BINDINGS
    // ========================================

    part def ConfiguredStockTickerSystem {
        doc /*
         * Complete system with explicit connection bindings
         * Demonstrates connection patterns
         */

        part stockService {
            port apiEndpoint;
        }

        part tickerDevice : StockTickerWithExhibits {
            port networkInterface;
            port displayOutput;
        }

        part userInterface {
            port buttonInput;
            port visualOutput;
        }

        /* Connect stock service to ticker device */
        connection apiConnection
            connect stockService.apiEndpoint to tickerDevice.networkInterface;

        /* Bind display output to user interface */
        binding displayBinding
            bind tickerDevice.displayOutput = userInterface.visualOutput;
    }

    // ========================================
    // REQUIREMENTS WITH METADATA
    // ========================================

    requirement def PriceUpdateRequirement {
        doc /*
         * Stock prices must update within acceptable timeframe
         */

        subject device : StockTickerWithExhibits;

        attribute maxUpdateInterval : Real = 5.0 {
            doc /* seconds */
        }

        attribute minUpdateInterval : Real = 1.0 {
            doc /* seconds - avoid API rate limiting */
        }

        require constraint {
            maxUpdateInterval > minUpdateInterval
        }
    }

    // ========================================
    // CONCERNS AND VIEWPOINTS
    // ========================================

    concern def UserExperienceConcern {
        doc /* User experience and usability concerns */

        require constraint {
            doc /* Interface must be intuitive and responsive */
        }
    }

    concern def AssemblyConcern {
        doc /* Manufacturing and assembly concerns */

        require constraint {
            doc /* Assembly must be possible without specialized tools */
        }
    }

    // ========================================
    // PRODUCT LINE MATRIX
    // ========================================

    part def ProductLineMatrix {
        doc /*
         * Complete product line showing all configurations
         * Documents the variant space
         *
         * Product Configuration Matrix:
         *
         * | Model     | Power Source    | Display      | Connectivity | Price |
         * |-----------|----------------|--------------|--------------|-------|
         * | Basic     | USB Only       | 1.14" LCD    | WiFi         | $15   |
         * | Premium   | Battery+USB    | 2.0" LCD     | WiFi+BT      | $45   |
         * | Portable  | Solar+Battery  | 2.9" E-Ink   | WiFi         | $35   |
         *
         * Target Markets:
         * - Basic: Education, hobbyists, prototyping
         * - Premium: Trading desks, professional use
         * - Portable: Mobile users, outdoor applications
         */

        part basicModel : BasicConfiguration;
        part premiumModel : PremiumConfiguration;
        part portableModel : PortableConfiguration;

        constraint {
            doc /* Premium must cost more than basic */
            premiumModel.targetPrice > basicModel.targetPrice
        }

        constraint {
            doc /* Portable should be priced between basic and premium */
            portableModel.targetPrice > basicModel.targetPrice and
            portableModel.targetPrice < premiumModel.targetPrice
        }
    }

    // ========================================
    // VERIFICATION
    // ========================================

    verification def ConfigurationValidation {
        doc /*
         * Verify that product configurations meet constraints
         */

        subject system : ProductLineMatrix;

        action verifyBasicConfig {
            doc /* Validate basic configuration */
        }

        action verifyPremiumConfig {
            doc /* Validate premium configuration */
        }

        action verifyPortableConfig {
            doc /* Validate portable configuration */
        }

        constraint {
            doc /* All prices must be positive */
            system.basicModel.targetPrice > 0.0 and
            system.premiumModel.targetPrice > 0.0 and
            system.portableModel.targetPrice > 0.0
        }
    }

    // ========================================
    // SUMMARY DOCUMENTATION
    // ========================================

    doc /*
     * SysML v2 Features Demonstrated in This Model:
     *
     * 1. Metadata Definitions (SecurityClassification, Traceability, ModelingAssumptions)
     * 2. Part Definitions with Inheritance (:> PhysicalStockTicker)
     * 3. Product Configurations (Basic, Premium, Portable)
     * 4. Exhibit States (display component with nested states)
     * 5. Fork/Join Parallelism (ParallelDataFetch action)
     * 6. Connection Bindings (ConfiguredStockTickerSystem)
     * 7. Requirements with Constraints (PriceUpdateRequirement)
     * 8. Concerns (UserExperienceConcern, AssemblyConcern)
     * 9. Product Line Matrix with Constraints
     * 10. Verification Definitions (ConfigurationValidation)
     *
     * This model complements the existing stock ticker models by demonstrating
     * product variants and system configurations using features supported by
     * Cameo Systems Modeler.
     */
}

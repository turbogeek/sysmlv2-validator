library package <DS_Simulation> DassaultSystemesSimulation {
    package SimulationMetadata {
        private import ScalarValues::**;
        private import Metaobjects::**;
        item simConfigs [0..*] nonunique;
        item def simConfigDefs;
        metadata def <'sim config'> SimulationConfigurationMetadata :> SemanticMetadata {
            :> annotatedElement : SysML::Systems::ItemDefinition;
            :> annotatedElement : SysML::Systems::ItemUsage;
            :>> baseType = if annotatedElement istype SysML::Systems::ItemDefinition ? simConfigDefs meta SysML::Systems::ItemDefinition else
                           simConfigs meta SysML::Systems::ItemUsage;

            executionTarget : Base::Anything {
                doc /* Selects the root element for simulation execution.*/
            }
            UI [*] : Base::Anything {
                doc /* Links UI mockup or time-series config to interact with the simulation while it is running.*/
            }
            attribute autoStart : Boolean [1] default true {
                doc /* Determines whether the simulation starts automatically upon initialization (true) or waits for manual trigger (false).*/
            }
            dataExport [*] : Base::Anything {
                doc /* Links export configurations to log and save simulation outputs to external files.*/
            }
            attribute speed : Integer [0..1] default 1 {
                doc /* Adjusts the execution speed to accelerate simulation runs or slow them down for observation.*/
            }
            startupView : Views::View [0..1] {
                doc /* Specifies the view that opens automatically when the simulation begins.*/
            }
            attribute decimalPlaces : Integer [0..1] {
                doc /* Sets how many decimal points to display for numbers; otherwise, the project default will be applied.*/
            }
            attribute startTime : Integer [0..1] {
                doc /* The time mark where the simulation begins running.*/
            }
            attribute endTime : Integer [0..1] {
                doc /* The time mark where the simulation automatically stops running. */
            }
        }
        metadata def <'time series chart'> TimeSeriesChartConfigurationMetadata :> SemanticMetadata {
            :> annotatedElement : SysML::Systems::ItemDefinition;
            :> annotatedElement : SysML::Systems::ItemUsage;
            :>> baseType = if annotatedElement istype SysML::Systems::ItemDefinition ? simConfigDefs meta SysML::Systems::ItemDefinition else
                           simConfigs meta SysML::Systems::ItemUsage;

            values : Base::Anything [*] {
                doc /* References the specific model variables to plot on the chart over time. */
            }
            attribute title : String [0..1] {
                doc /* Specifies the main header text displayed at the top of the time-series chart. */
            }
            attribute subtitle : String [0..1] {
                doc /* Provides secondary text shown beneath the main title to add context to the chart. */
            }
            attribute isInterpolated : Boolean [1] default false {
                doc /* Controls curve rendering; when true, data points are smoothly interpolated, otherwise displayed as discrete steps/lines. */
            }
            attribute showValues : Boolean [1] default false {
                doc /* Toggles explicit numerical data value labels on the visual chart interface. */
            }
            attribute showDataPoints : Boolean [1] default false {
                doc /* Toggles visible markers for individual sampled data points along plotted curves. */
            }
        }
        metadata def <'data export'> DataExportConfigurationMetadata :> SemanticMetadata {
            :> annotatedElement : SysML::Systems::ItemDefinition;
            :> annotatedElement : SysML::Systems::ItemUsage;
            :>> baseType = if annotatedElement istype SysML::Systems::ItemDefinition ? simConfigDefs meta SysML::Systems::ItemDefinition else
                           simConfigs meta SysML::Systems::ItemUsage;

            values : Base::Anything [*] {
                doc /* Identifies the specific model attributes targeted for external logging. */
            }
            attribute fileName : String [0..1] {
                doc /* Specifies the target CSV file name or path for exported results. */
            }
        }
    }
}

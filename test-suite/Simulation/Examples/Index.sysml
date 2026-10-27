package Index {
    view IndexView : StandardViewDefinitions::gv {
        render Views::asInterconnectionDiagram;
        view WaterLevelControlSystemView : DiagramInterchange::NodeView {
            expose WaterTankSystemPkg::WaterTankDomain::WaterLevelControlSystem;
            render DiagramInterchange::asRegularNode {
                attribute :>> x = 648;
                attribute :>> y = 203;
                attribute :>> height = 39;
                attribute :>> width = 145;
            }
        }
        view FlashingLightView : DiagramInterchange::NodeView {
            expose FlashingLightSystemPkg::FlashingLightDomain::FlashingLight;
            render DiagramInterchange::asRegularNode {
                attribute :>> x = 681;
                attribute :>> y = 322;
                attribute :>> height = 39;
                attribute :>> width = 80;
            }
        }
        view TrafficLightSystemViewView : DiagramInterchange::NodeView {
            expose TrafficLightSystemPkg::TrafficLightDomain::TrafficLightSystemView;
            render DiagramInterchange::asRegularNode {
                attribute :>> x = 653;
                attribute :>> y = 448;
                attribute :>> height = 39;
                attribute :>> width = 135;
            }
        }
        view SpacecraftPowerRollupView : DiagramInterchange::NodeView {
            expose DynamicPowerRollupPkg::DynamicPowerRollup::MissionScenario::SpacecraftPowerRollup;
            render DiagramInterchange::asRegularNode {
                attribute :>> x = 656;
                attribute :>> y = 77;
                attribute :>> height = 39;
                attribute :>> width = 130;
            }
        }
        view FlightControlSystemViewView : DiagramInterchange::NodeView {
            expose FlightControlSystemsPkg::FlightControlSystems::FlightControlSystemView;
            render DiagramInterchange::asRegularNode {
                attribute :>> x = 651;
                attribute :>> y = 567;
                attribute :>> height = 39;
                attribute :>> width = 140;
            }
        }
        view AutonomousVehicleSystemViewView : DiagramInterchange::NodeView {
            expose VehiclePerceptionSystem::AutonomousVehicleSystem::AutonomousVehicleSystemView;
            render DiagramInterchange::asRegularNode {
                attribute :>> x = 616;
                attribute :>> y = 693;
                attribute :>> height = 39;
                attribute :>> width = 176;
            }
        }
    }
}

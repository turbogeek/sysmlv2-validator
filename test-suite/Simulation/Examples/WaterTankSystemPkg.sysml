private import ScalarValues::**;
private import SI::**;
package WaterTankSystemPkg {
    package WaterTankDomain {
        part WaterTankSystem {
            interface __unnamed114 : InterfaceDefs::ControlToTankInterface connect controller.valvePort to waterTank.valvePort;
            interface __unnamed118 : InterfaceDefs::TankToControlInterface connect waterTank.levelPort to controller.levelPort;
            part controller : Controller;
            part waterTank : WaterTank;
        }
        part def WaterTank {
            port levelPort : ~PortDefs::LevelPort;
            port valvePort : PortDefs::ValvePort;
            attribute flowRate : Real default := 0.2;
            attribute level : Real;
            exhibit state WaterTankSTM {
                state adjustingLevel {
                    entry assign level := level + flowRate - flowRate * valvePort.valve * 1.2;
                }
                // "Feature Membership -> Transition" is incompatible with textual.
                transition __unnamed116 first start then adjustingLevel;
            }
            binding __unnamed117 bind level = levelPort.level;
            satisfy requirement satisfyLevelReq ::> Requirement::waterLevel;
        }
        part def Controller {
            port levelPort : PortDefs::LevelPort;
            port valvePort : ~PortDefs::ValvePort;
            attribute minlevel : Real default := 1.0;
            attribute maxlevel : Real default := 3.0;
            exhibit state ControllerSTM {
                state closeValve {
                    entry assign valvePort.valve := 0;
                }
                state openValve {
                    entry assign valvePort.valve := 1;
                }
                transition __unnamed111 first closeValve accept when levelPort.level >= maxlevel then openValve;
                transition __unnamed113 first openValve accept when levelPort.level <= minlevel then closeValve;
                transition __unnamed112 first start then closeValve;
            }
        }
        view WaterLevelControlSystem : StandardViewDefinitions::gv {
            render Views::asInterconnectionDiagram;
            view WaterTankSystemView : DiagramInterchange::NodeView {
                expose WaterTankSystem;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 5;
                    attribute :>> y = 5;
                    attribute :>> height = 394;
                    attribute :>> width = 1383;
                }
                view controllerView : DiagramInterchange::NodeView {
                    expose WaterTankSystem::controller;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 70;
                        attribute :>> y = 51;
                        attribute :>> height = 245;
                        attribute :>> width = 632;
                    }
                    view ControllerSTMView : DiagramInterchange::NodeView {
                        expose Controller::ControllerSTM;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 21;
                            attribute :>> y = 63;
                            attribute :>> height = 162;
                            attribute :>> width = 591;
                        }
                        view closeValveView : DiagramInterchange::NodeView {
                            expose ControllerSTM::closeValve;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 45;
                                attribute :>> y = 70;
                                attribute :>> height = 72;
                                attribute :>> width = 172;
                            }
                        }
                        view openValveView : DiagramInterchange::NodeView {
                            expose ControllerSTM::openValve;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 402;
                                attribute :>> y = 70;
                                attribute :>> height = 72;
                                attribute :>> width = 172;
                            }
                        }
                        view startView : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 10;
                                attribute :>> y = 91;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView3 : DiagramInterchange::EdgeView {
                            expose ControllerSTM::__unnamed111;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 51;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 51;
                            }
                            ref view :>> sourceView ::> closeValveView;
                            ref view :>> targetView ::> openValveView;
                        }
                        view unnamedView4 : DiagramInterchange::EdgeView {
                            expose ControllerSTM::__unnamed112;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 30;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 9;
                            }
                            ref view :>> sourceView ::> startView;
                            ref view :>> targetView ::> closeValveView;
                        }
                        view unnamedView5 : DiagramInterchange::EdgeView {
                            expose ControllerSTM::__unnamed113;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::right;
                                attribute :>> yt = 15;
                                attribute :>> sourceOnBorder = sourceOnBorder::left;
                                attribute :>> ys = 15;
                            }
                            ref view :>> sourceView ::> openValveView;
                            ref view :>> targetView ::> closeValveView;
                        }
                    }
                    view valvePortView : DiagramInterchange::NodeView {
                        expose Controller::valvePort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::right;
                            attribute :>> yc = 84;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view levelPortView : DiagramInterchange::NodeView {
                        expose Controller::levelPort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 159;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view unnamedView : DiagramInterchange::EdgeView {
                        expose WaterTankSystem::__unnamed114;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 4;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = ((19, 0), 0, 116);
                        }
                        ref view :>> sourceView ::> valvePortView;
                        ref view :>> targetView ::> waterTankView::valvePortView1;
                    }
                }
                view waterTankView : DiagramInterchange::NodeView {
                    expose WaterTankSystem::waterTank;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 798;
                        attribute :>> y = 51;
                        attribute :>> height = 309;
                        attribute :>> width = 510;
                    }
                    view WaterTankSTMView : DiagramInterchange::NodeView {
                        expose WaterTank::WaterTankSTM;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 21;
                            attribute :>> y = 112;
                            attribute :>> height = 177;
                            attribute :>> width = 469;
                        }
                        view adjustingLevelView : DiagramInterchange::NodeView {
                            expose WaterTankSTM::adjustingLevel;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 56;
                                attribute :>> y = 63;
                                attribute :>> height = 72;
                                attribute :>> width = 358;
                            }
                        }
                        view startView1 : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 86;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView2 : DiagramInterchange::EdgeView {
                            expose WaterTankSTM::__unnamed115;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::right;
                                attribute :>> yt = 33;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 315;
                                attribute :>> wayPoints = ((0, 24), (74, 0), 0, -63);
                            }
                            ref view :>> sourceView ::> adjustingLevelView;
                            ref view :>> targetView ::> adjustingLevelView;
                        }
                        view unnamedView7 : DiagramInterchange::EdgeView {
                            expose WaterTankSTM::__unnamed116;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 32;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 9;
                            }
                            ref view :>> sourceView ::> startView1;
                            ref view :>> targetView ::> adjustingLevelView;
                        }
                    }
                    view valvePortView1 : DiagramInterchange::NodeView {
                        expose WaterTank::valvePort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 203;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view levelPortView1 : DiagramInterchange::NodeView {
                        expose WaterTank::levelPort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::right;
                            attribute :>> yc = 77;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view levelView : DiagramInterchange::NodeView {
                        expose WaterTank::level;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 329;
                            attribute :>> y = 56;
                            attribute :>> height = 39;
                            attribute :>> width = 73;
                        }
                    }
                    view satisfyLevelReqView : DiagramInterchange::NodeView {
                        expose WaterTank::satisfyLevelReq;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 21;
                            attribute :>> y = 56;
                            attribute :>> height = 39;
                            attribute :>> width = 172;
                        }
                    }
                    view unnamedView1 : DiagramInterchange::EdgeView {
                        expose WaterTank::__unnamed117;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 21;
                        }
                        ref view :>> sourceView ::> levelView;
                        ref view :>> targetView ::> levelPortView1;
                    }
                    view unnamedView6 : DiagramInterchange::EdgeView {
                        expose WaterTankSystem::__unnamed118;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 9;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = ((19, 0), (0, 245), (-1302, 0), 0, -161);
                        }
                        ref view :>> sourceView ::> levelPortView1;
                        ref view :>> targetView ::> controllerView::levelPortView;
                    }
                }
            }
        }
    }
    package Requirement {
        requirement waterLevel {
            doc /* Water level must be between 1 and 3 */
            subject container :> WaterTankDomain::WaterTankSystem.waterTank;
            require constraint requireLevelConstraint {
                container.level <= 3 & container.level >= 1
            }
        }
    }
    package InterfaceDefs {
        interface def ControlToTankInterface {
            end p1 : ~PortDefs::ValvePort;
            end p2 : PortDefs::ValvePort;
            flow p1.valve to p2.valve;
        }
        interface def TankToControlInterface {
            end p1 : ~PortDefs::LevelPort;
            end p2 : PortDefs::LevelPort;
            flow p1.level to p2.level;
        }
    }
    package PortDefs {
        port def LevelPort {
            in attribute level : Real;
        }
        port def ValvePort {
            in attribute valve : Real;
        }
    }
    package SimConfig {
        item WaterTank {
            @DassaultSystemesSimulation::SimulationMetadata::'sim config' {
                executionTarget = WaterTankSystemPkg::WaterTankDomain::WaterTankSystem;
                UI = WaterTank_TimeSeriesConfig;
                autoStart = false;
                speed = 10;
                startupView = WaterTankSystemPkg::WaterTankDomain::WaterLevelControlSystem;
                dataExport = WaterTank_DataExportConfig;
            }
        }
        item WaterTank_WithUI {
            @DassaultSystemesSimulation::SimulationMetadata::'sim config' {
                executionTarget = WaterTankSystemPkg::WaterTankDomain::WaterTankSystem;
                UI = 'HTML UI'::WaterTankUI_HTML;
                autoStart = false;
                speed = 3;
                startupView = WaterTankSystemPkg::WaterTankDomain::WaterLevelControlSystem;
                dataExport = WaterTank_DataExportConfig;
            }
        }
        item WaterTank_TimeSeriesConfig {
            @DassaultSystemesSimulation::SimulationMetadata::'time series chart' {
                values = (WaterTankSystemPkg::WaterTankDomain::WaterTankSystem.waterTank.level, WaterTankSystemPkg::WaterTankDomain::WaterTankSystem.controller.minlevel,
                          WaterTankSystemPkg::WaterTankDomain::WaterTankSystem.controller.maxlevel);

                title = "Water Tank System";
                subtitle = "WaTer Tank Level";
                isInterpolated = false;
                showValues = false;
                showDataPoints = false;
            }
        }
        item WaterTank_DataExportConfig {
            @DassaultSystemesSimulation::SimulationMetadata::'data export' {
                values = (WaterTankSystemPkg::WaterTankDomain::WaterTankSystem.waterTank.level,
                          WaterTankSystemPkg::WaterTankDomain::WaterTankSystem.waterTank.satisfyLevelReq.requireLevelConstraint);

                fileName = "WaterTankSystemOutputResult.csv";
            }
        }
        view SimConfigView : StandardViewDefinitions::gv {
            render Views::asInterconnectionDiagram;
            view WaterTankView : DiagramInterchange::NodeView {
                expose WaterTank;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 7;
                    attribute :>> y = 28;
                    attribute :>> height = 154;
                    attribute :>> width = 227;
                }
            }
            view WaterTank_WithUIView : DiagramInterchange::NodeView {
                expose WaterTank_WithUI;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 252;
                    attribute :>> y = 28;
                    attribute :>> height = 154;
                    attribute :>> width = 227;
                }
            }
            view WaterTank_TimeSeriesConfigView : DiagramInterchange::NodeView {
                expose WaterTank_TimeSeriesConfig;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 7;
                    attribute :>> y = 196;
                    attribute :>> height = 154;
                    attribute :>> width = 633;
                }
            }
        }
    }
}

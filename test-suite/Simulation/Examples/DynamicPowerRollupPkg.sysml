package DynamicPowerRollupPkg {
    package DynamicPowerRollup {
        public import NumericalFunctions::*;
        private import SI::**;
        item def turnOn;
        item def turnOff;
        part def PowerConsumer {
            attribute power : Real default := 0;
            attribute onPower : Real;
            exhibit state Operating {
                state Off {
                    entry assign power := 0;
                }
                state On {
                    entry assign power := onPower;
                }
                transition __unnamed3 first start then Off;
                transition __unnamed2 first Off accept turnOn then On;
                transition __unnamed1 first On accept turnOff then Off;
            }
        }
        part def compositeThing :> PowerConsumer {
            attribute :>> power = sum(subConsumers.power);
            state :>> Operating [0];
            abstract part subConsumers : PowerConsumer [*];
        }
        part spaceCraftStructure : compositeThing {
            part telecom : compositeThing :> subConsumers {
                attribute :>> onPower := 35;
                part amplifier :> subConsumers {
                    attribute :>> onPower := 10;
                }
                part antenna :> subConsumers {
                    attribute :>> onPower := 20;
                }
            }
            part propulsion : compositeThing :> subConsumers {
                attribute :>> onPower := 80;
                part tank :> subConsumers {
                    attribute :>> onPower := 44;
                }
                part thruster :> subConsumers {
                    attribute :>> onPower := 29;
                }
            }
            view spaceCraftView1 : StandardViewDefinitions::gv {
                render Views::asInterconnectionDiagram;
                view spaceCraftStructureView : DiagramInterchange::NodeView {
                    expose spaceCraftStructure;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 5;
                        attribute :>> y = 5;
                        attribute :>> height = 1207;
                        attribute :>> width = 1398;
                    }
                    view propulsionView : DiagramInterchange::NodeView {
                        expose propulsion;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 716;
                            attribute :>> y = 51;
                            attribute :>> height = 792;
                            attribute :>> width = 672;
                        }
                        view thrusterView : DiagramInterchange::NodeView {
                            expose propulsion::thruster;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 427;
                                attribute :>> height = 334;
                                attribute :>> width = 533;
                            }
                            view unnamedView : DiagramInterchange::NodeView {
                                expose thruster::onPower;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 126;
                                    attribute :>> y = 266;
                                    attribute :>> height = 39;
                                    attribute :>> width = 74;
                                }
                            }
                            view powerView : DiagramInterchange::NodeView {
                                expose PowerConsumer::power;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 266;
                                    attribute :>> height = 39;
                                    attribute :>> width = 79;
                                }
                            }
                            view OperatingView : DiagramInterchange::NodeView {
                                expose PowerConsumer::Operating;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 70;
                                    attribute :>> height = 171;
                                    attribute :>> width = 493;
                                }
                                view OffView : DiagramInterchange::NodeView {
                                    expose Operating::Off;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 63;
                                        attribute :>> y = 70;
                                        attribute :>> height = 72;
                                        attribute :>> width = 128;
                                    }
                                }
                                view OnView : DiagramInterchange::NodeView {
                                    expose Operating::On;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 308;
                                        attribute :>> y = 70;
                                        attribute :>> height = 72;
                                        attribute :>> width = 165;
                                    }
                                }
                                view startView : DiagramInterchange::NodeView {
                                    expose States::StateAction::start;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 21;
                                        attribute :>> y = 91;
                                        attribute :>> height = 18;
                                        attribute :>> width = 18;
                                    }
                                }
                                view unnamedView8 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed1;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::right;
                                        attribute :>> yt = 51;
                                        attribute :>> sourceOnBorder = sourceOnBorder::left;
                                        attribute :>> ys = 51;
                                    }
                                    ref view :>> sourceView ::> OnView;
                                    ref view :>> targetView ::> OffView;
                                }
                                view unnamedView18 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed2;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 15;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 15;
                                    }
                                    ref view :>> sourceView ::> OffView;
                                    ref view :>> targetView ::> OnView;
                                }
                                view unnamedView23 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed3;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 30;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 9;
                                    }
                                    ref view :>> sourceView ::> startView;
                                    ref view :>> targetView ::> OffView;
                                }
                            }
                        }
                        view tankView : DiagramInterchange::NodeView {
                            expose propulsion::tank;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 70;
                                attribute :>> height = 334;
                                attribute :>> width = 533;
                            }
                            view powerView1 : DiagramInterchange::NodeView {
                                expose PowerConsumer::power;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 259;
                                    attribute :>> height = 39;
                                    attribute :>> width = 79;
                                }
                            }
                            view unnamedView1 : DiagramInterchange::NodeView {
                                expose tank::onPower;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 126;
                                    attribute :>> y = 259;
                                    attribute :>> height = 39;
                                    attribute :>> width = 74;
                                }
                            }
                            view OperatingView1 : DiagramInterchange::NodeView {
                                expose PowerConsumer::Operating;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 63;
                                    attribute :>> height = 171;
                                    attribute :>> width = 493;
                                }
                                view OffView1 : DiagramInterchange::NodeView {
                                    expose Operating::Off;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 63;
                                        attribute :>> y = 70;
                                        attribute :>> height = 72;
                                        attribute :>> width = 128;
                                    }
                                }
                                view OnView1 : DiagramInterchange::NodeView {
                                    expose Operating::On;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 308;
                                        attribute :>> y = 70;
                                        attribute :>> height = 72;
                                        attribute :>> width = 165;
                                    }
                                }
                                view startView1 : DiagramInterchange::NodeView {
                                    expose States::StateAction::start;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 21;
                                        attribute :>> y = 98;
                                        attribute :>> height = 18;
                                        attribute :>> width = 18;
                                    }
                                }
                                view unnamedView15 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed1;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::right;
                                        attribute :>> yt = 56;
                                        attribute :>> sourceOnBorder = sourceOnBorder::left;
                                        attribute :>> ys = 56;
                                    }
                                    ref view :>> sourceView ::> OnView1;
                                    ref view :>> targetView ::> OffView1;
                                }
                                view unnamedView22 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed2;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 20;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 20;
                                    }
                                    ref view :>> sourceView ::> OffView1;
                                    ref view :>> targetView ::> OnView1;
                                }
                                view unnamedView24 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed3;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 37;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 9;
                                    }
                                    ref view :>> sourceView ::> startView1;
                                    ref view :>> targetView ::> OffView1;
                                }
                            }
                        }
                        view unnamedView2 : DiagramInterchange::NodeView {
                            expose propulsion::onPower;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 581;
                                attribute :>> y = 427;
                                attribute :>> height = 39;
                                attribute :>> width = 74;
                            }
                        }
                    }
                    view unnamedView3 : DiagramInterchange::NodeView {
                        expose Operating;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 576;
                            attribute :>> y = 863;
                            attribute :>> height = 171;
                            attribute :>> width = 493;
                        }
                        view OffView2 : DiagramInterchange::NodeView {
                            expose Operating::Off;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 63;
                                attribute :>> y = 70;
                                attribute :>> height = 72;
                                attribute :>> width = 128;
                            }
                        }
                        view OnView2 : DiagramInterchange::NodeView {
                            expose Operating::On;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 308;
                                attribute :>> y = 70;
                                attribute :>> height = 72;
                                attribute :>> width = 165;
                            }
                        }
                        view startView2 : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 98;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView11 : DiagramInterchange::EdgeView {
                            expose Operating::__unnamed3;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 37;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 9;
                            }
                            ref view :>> sourceView ::> startView2;
                            ref view :>> targetView ::> OffView2;
                        }
                        view unnamedView16 : DiagramInterchange::EdgeView {
                            expose Operating::__unnamed1;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::right;
                                attribute :>> yt = 56;
                                attribute :>> sourceOnBorder = sourceOnBorder::left;
                                attribute :>> ys = 56;
                            }
                            ref view :>> sourceView ::> OnView2;
                            ref view :>> targetView ::> OffView2;
                        }
                        view unnamedView17 : DiagramInterchange::EdgeView {
                            expose Operating::__unnamed2;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 20;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 20;
                            }
                            ref view :>> sourceView ::> OffView2;
                            ref view :>> targetView ::> OnView2;
                        }
                    }
                    view telecomView : DiagramInterchange::NodeView {
                        expose telecom;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 23;
                            attribute :>> y = 51;
                            attribute :>> height = 792;
                            attribute :>> width = 672;
                        }
                        view unnamedView4 : DiagramInterchange::NodeView {
                            expose telecom::onPower;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 574;
                                attribute :>> y = 427;
                                attribute :>> height = 39;
                                attribute :>> width = 74;
                            }
                        }
                        view antennaView : DiagramInterchange::NodeView {
                            expose telecom::antenna;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 14;
                                attribute :>> y = 427;
                                attribute :>> height = 334;
                                attribute :>> width = 533;
                            }
                            view unnamedView5 : DiagramInterchange::NodeView {
                                expose antenna::onPower;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 126;
                                    attribute :>> y = 266;
                                    attribute :>> height = 39;
                                    attribute :>> width = 74;
                                }
                            }
                            view powerView2 : DiagramInterchange::NodeView {
                                expose PowerConsumer::power;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 266;
                                    attribute :>> height = 39;
                                    attribute :>> width = 79;
                                }
                            }
                            view OperatingView2 : DiagramInterchange::NodeView {
                                expose PowerConsumer::Operating;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 70;
                                    attribute :>> height = 171;
                                    attribute :>> width = 493;
                                }
                                view OffView3 : DiagramInterchange::NodeView {
                                    expose Operating::Off;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 63;
                                        attribute :>> y = 70;
                                        attribute :>> height = 72;
                                        attribute :>> width = 128;
                                    }
                                }
                                view OnView3 : DiagramInterchange::NodeView {
                                    expose Operating::On;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 308;
                                        attribute :>> y = 70;
                                        attribute :>> height = 72;
                                        attribute :>> width = 165;
                                    }
                                }
                                view startView3 : DiagramInterchange::NodeView {
                                    expose States::StateAction::start;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 21;
                                        attribute :>> y = 91;
                                        attribute :>> height = 18;
                                        attribute :>> width = 18;
                                    }
                                }
                                view unnamedView9 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed3;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 30;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 9;
                                    }
                                    ref view :>> sourceView ::> startView3;
                                    ref view :>> targetView ::> OffView3;
                                }
                                view unnamedView19 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed1;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::right;
                                        attribute :>> yt = 51;
                                        attribute :>> sourceOnBorder = sourceOnBorder::left;
                                        attribute :>> ys = 51;
                                    }
                                    ref view :>> sourceView ::> OnView3;
                                    ref view :>> targetView ::> OffView3;
                                }
                                view unnamedView20 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed2;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 15;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 15;
                                    }
                                    ref view :>> sourceView ::> OffView3;
                                    ref view :>> targetView ::> OnView3;
                                }
                            }
                        }
                        view amplifierView : DiagramInterchange::NodeView {
                            expose telecom::amplifier;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 14;
                                attribute :>> y = 70;
                                attribute :>> height = 334;
                                attribute :>> width = 533;
                            }
                            view powerView3 : DiagramInterchange::NodeView {
                                expose PowerConsumer::power;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 259;
                                    attribute :>> height = 39;
                                    attribute :>> width = 79;
                                }
                            }
                            view unnamedView6 : DiagramInterchange::NodeView {
                                expose amplifier::onPower;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 126;
                                    attribute :>> y = 259;
                                    attribute :>> height = 39;
                                    attribute :>> width = 74;
                                }
                            }
                            view OperatingView3 : DiagramInterchange::NodeView {
                                expose PowerConsumer::Operating;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 63;
                                    attribute :>> height = 171;
                                    attribute :>> width = 493;
                                }
                                view OffView4 : DiagramInterchange::NodeView {
                                    expose Operating::Off;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 63;
                                        attribute :>> y = 70;
                                        attribute :>> height = 72;
                                        attribute :>> width = 128;
                                    }
                                }
                                view OnView4 : DiagramInterchange::NodeView {
                                    expose Operating::On;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 308;
                                        attribute :>> y = 70;
                                        attribute :>> height = 72;
                                        attribute :>> width = 165;
                                    }
                                }
                                view startView4 : DiagramInterchange::NodeView {
                                    expose States::StateAction::start;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 21;
                                        attribute :>> y = 98;
                                        attribute :>> height = 18;
                                        attribute :>> width = 18;
                                    }
                                }
                                view unnamedView10 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed2;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 20;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 20;
                                    }
                                    ref view :>> sourceView ::> OffView4;
                                    ref view :>> targetView ::> OnView4;
                                }
                                view unnamedView14 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed1;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::right;
                                        attribute :>> yt = 56;
                                        attribute :>> sourceOnBorder = sourceOnBorder::left;
                                        attribute :>> ys = 56;
                                    }
                                    ref view :>> sourceView ::> OnView4;
                                    ref view :>> targetView ::> OffView4;
                                }
                                view unnamedView21 : DiagramInterchange::EdgeView {
                                    expose Operating::__unnamed3;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 37;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 9;
                                    }
                                    ref view :>> sourceView ::> startView4;
                                    ref view :>> targetView ::> OffView4;
                                }
                            }
                        }
                    }
                    view unnamedView7 : DiagramInterchange::NodeView {
                        expose power;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 1213;
                            attribute :>> y = 863;
                            attribute :>> height = 39;
                            attribute :>> width = 66;
                        }
                    }
                    view subConsumersView : DiagramInterchange::NodeView {
                        expose subConsumers;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 23;
                            attribute :>> y = 863;
                            attribute :>> height = 334;
                            attribute :>> width = 533;
                        }
                        view powerView4 : DiagramInterchange::NodeView {
                            expose PowerConsumer::power;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 14;
                                attribute :>> y = 266;
                                attribute :>> height = 39;
                                attribute :>> width = 79;
                            }
                        }
                        view OperatingView4 : DiagramInterchange::NodeView {
                            expose PowerConsumer::Operating;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 14;
                                attribute :>> y = 70;
                                attribute :>> height = 171;
                                attribute :>> width = 493;
                            }
                            view OffView5 : DiagramInterchange::NodeView {
                                expose Operating::Off;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 63;
                                    attribute :>> y = 70;
                                    attribute :>> height = 72;
                                    attribute :>> width = 128;
                                }
                            }
                            view OnView5 : DiagramInterchange::NodeView {
                                expose Operating::On;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 308;
                                    attribute :>> y = 70;
                                    attribute :>> height = 72;
                                    attribute :>> width = 165;
                                }
                            }
                            view startView5 : DiagramInterchange::NodeView {
                                expose States::StateAction::start;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 98;
                                    attribute :>> height = 18;
                                    attribute :>> width = 18;
                                }
                            }
                            view unnamedView12 : DiagramInterchange::EdgeView {
                                expose Operating::__unnamed1;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::right;
                                    attribute :>> yt = 55;
                                    attribute :>> sourceOnBorder = sourceOnBorder::left;
                                    attribute :>> ys = 55;
                                }
                                ref view :>> sourceView ::> OnView5;
                                ref view :>> targetView ::> OffView5;
                            }
                            view unnamedView13 : DiagramInterchange::EdgeView {
                                expose Operating::__unnamed2;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 19;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 19;
                                }
                                ref view :>> sourceView ::> OffView5;
                                ref view :>> targetView ::> OnView5;
                            }
                            view unnamedView25 : DiagramInterchange::EdgeView {
                                expose Operating::__unnamed3;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 37;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 9;
                                }
                                ref view :>> sourceView ::> startView5;
                                ref view :>> targetView ::> OffView5;
                            }
                        }
                    }
                    view onPowerView : DiagramInterchange::NodeView {
                        expose onPower;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 1094;
                            attribute :>> y = 863;
                            attribute :>> height = 39;
                            attribute :>> width = 92;
                        }
                    }
                }
            }
        }
        part MissionScenario {
            part spaceCraft :> spaceCraftStructure;
            view SpacecraftPowerRollup : StandardViewDefinitions::gv {
                render Views::asInterconnectionDiagram;
                view MissionScenarioView : DiagramInterchange::NodeView {
                    expose MissionScenario;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 5;
                        attribute :>> y = 5;
                        attribute :>> height = 834;
                        attribute :>> width = 1188;
                    }
                    view spaceCraftView : DiagramInterchange::NodeView {
                        expose spaceCraft;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 11;
                            attribute :>> y = 96;
                            attribute :>> height = 451;
                            attribute :>> width = 1138;
                        }
                        view telecomView : DiagramInterchange::NodeView {
                            expose spaceCraftStructure::telecom;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 577;
                                attribute :>> y = 94;
                                attribute :>> height = 331;
                                attribute :>> width = 541;
                            }
                            view antennaView : DiagramInterchange::NodeView {
                                expose telecom::antenna;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 10;
                                    attribute :>> y = 49;
                                    attribute :>> height = 258;
                                    attribute :>> width = 514;
                                }
                                view OperatingView : DiagramInterchange::NodeView {
                                    expose PowerConsumer::Operating;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 21;
                                        attribute :>> y = 112;
                                        attribute :>> height = 136;
                                        attribute :>> width = 483;
                                    }
                                    view OnView : DiagramInterchange::NodeView {
                                        expose Operating::On;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 308;
                                            attribute :>> y = 49;
                                            attribute :>> height = 72;
                                            attribute :>> width = 165;
                                        }
                                    }
                                    view OffView : DiagramInterchange::NodeView {
                                        expose Operating::Off;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 63;
                                            attribute :>> y = 49;
                                            attribute :>> height = 72;
                                            attribute :>> width = 128;
                                        }
                                    }
                                    view startView : DiagramInterchange::NodeView {
                                        expose States::StateAction::start;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 21;
                                            attribute :>> y = 77;
                                            attribute :>> height = 18;
                                            attribute :>> width = 18;
                                        }
                                    }
                                    view unnamedView : DiagramInterchange::EdgeView {
                                        expose Operating::__unnamed3;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 37;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 9;
                                        }
                                        ref view :>> sourceView ::> startView;
                                        ref view :>> targetView ::> OffView;
                                    }
                                    view unnamedView2 : DiagramInterchange::EdgeView {
                                        expose Operating::__unnamed2;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 54;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 54;
                                        }
                                        ref view :>> sourceView ::> OffView;
                                        ref view :>> targetView ::> OnView;
                                    }
                                    view unnamedView5 : DiagramInterchange::EdgeView {
                                        expose Operating::__unnamed1;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::right;
                                            attribute :>> yt = 18;
                                            attribute :>> sourceOnBorder = sourceOnBorder::left;
                                            attribute :>> ys = 18;
                                        }
                                        ref view :>> sourceView ::> OnView;
                                        ref view :>> targetView ::> OffView;
                                    }
                                }
                            }
                        }
                        view propulsionView : DiagramInterchange::NodeView {
                            expose spaceCraftStructure::propulsion;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 10;
                                attribute :>> y = 96;
                                attribute :>> height = 335;
                                attribute :>> width = 554;
                            }
                            view thrusterView : DiagramInterchange::NodeView {
                                expose propulsion::thruster;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 10;
                                    attribute :>> y = 49;
                                    attribute :>> height = 266;
                                    attribute :>> width = 524;
                                }
                                view OperatingView1 : DiagramInterchange::NodeView {
                                    expose PowerConsumer::Operating;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 21;
                                        attribute :>> y = 110;
                                        attribute :>> height = 136;
                                        attribute :>> width = 483;
                                    }
                                    view OnView1 : DiagramInterchange::NodeView {
                                        expose Operating::On;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 308;
                                            attribute :>> y = 49;
                                            attribute :>> height = 72;
                                            attribute :>> width = 165;
                                        }
                                    }
                                    view OffView1 : DiagramInterchange::NodeView {
                                        expose Operating::Off;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 63;
                                            attribute :>> y = 49;
                                            attribute :>> height = 72;
                                            attribute :>> width = 128;
                                        }
                                    }
                                    view startView1 : DiagramInterchange::NodeView {
                                        expose States::StateAction::start;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 21;
                                            attribute :>> y = 77;
                                            attribute :>> height = 18;
                                            attribute :>> width = 18;
                                        }
                                    }
                                    view unnamedView1 : DiagramInterchange::EdgeView {
                                        expose Operating::__unnamed3;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 37;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 9;
                                        }
                                        ref view :>> sourceView ::> startView1;
                                        ref view :>> targetView ::> OffView1;
                                    }
                                    view unnamedView4 : DiagramInterchange::EdgeView {
                                        expose Operating::__unnamed1;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::right;
                                            attribute :>> yt = 18;
                                            attribute :>> sourceOnBorder = sourceOnBorder::left;
                                            attribute :>> ys = 18;
                                        }
                                        ref view :>> sourceView ::> OnView1;
                                        ref view :>> targetView ::> OffView1;
                                    }
                                    view unnamedView8 : DiagramInterchange::EdgeView {
                                        expose Operating::__unnamed2;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 54;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 54;
                                        }
                                        ref view :>> sourceView ::> OffView1;
                                        ref view :>> targetView ::> OnView1;
                                    }
                                }
                            }
                        }
                    }
                    view scenarioView : DiagramInterchange::NodeView {
                        expose scenario;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 10;
                            attribute :>> y = 565;
                            attribute :>> height = 249;
                            attribute :>> width = 916;
                        }
                        view 'mission scenarioView' : DiagramInterchange::NodeView {
                            expose scenario::'mission scenario';
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 77;
                                attribute :>> height = 162;
                                attribute :>> width = 875;
                            }
                            view waitingView : DiagramInterchange::NodeView {
                                expose 'mission scenario'::waiting;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 63;
                                    attribute :>> y = 84;
                                    attribute :>> height = 39;
                                    attribute :>> width = 49;
                                }
                            }
                            view startView2 : DiagramInterchange::NodeView {
                                expose States::StateAction::start;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 98;
                                    attribute :>> height = 18;
                                    attribute :>> width = 18;
                                }
                            }
                            view antennaOnView : DiagramInterchange::NodeView {
                                expose 'mission scenario'::antennaOn;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 189;
                                    attribute :>> y = 70;
                                    attribute :>> height = 72;
                                    attribute :>> width = 291;
                                }
                            }
                            view thrusterOnView : DiagramInterchange::NodeView {
                                expose 'mission scenario'::thrusterOn;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 553;
                                    attribute :>> y = 70;
                                    attribute :>> height = 72;
                                    attribute :>> width = 302;
                                }
                            }
                            view unnamedView3 : DiagramInterchange::EdgeView {
                                expose 'mission scenario'::__unnamed4;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 35;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 35;
                                }
                                ref view :>> sourceView ::> antennaOnView;
                                ref view :>> targetView ::> thrusterOnView;
                            }
                            view unnamedView6 : DiagramInterchange::EdgeView {
                                expose 'mission scenario'::__unnamed5;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 23;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 9;
                                }
                                ref view :>> sourceView ::> startView2;
                                ref view :>> targetView ::> waitingView;
                            }
                            view unnamedView7 : DiagramInterchange::EdgeView {
                                expose 'mission scenario'::__unnamed6;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 35;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 21;
                                }
                                ref view :>> sourceView ::> waitingView;
                                ref view :>> targetView ::> antennaOnView;
                            }
                        }
                    }
                    view maxPowerView : DiagramInterchange::NodeView {
                        expose maxPower;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 933;
                            attribute :>> y = 562;
                            attribute :>> height = 144;
                            attribute :>> width = 235;
                        }
                    }
                }
            }
            attribute maxPowerRestriction = 45;
            attribute time = localClock.currentTime;
            satisfy requirement maxPower {
                doc /* Total power draw of the spacecraft must always remain below max power restriction value */
                require constraint {
                    spaceCraft.power <= maxPowerRestriction
                }
            }
            part scenario {
                exhibit state 'mission scenario' {
                    state waiting;
                    transition __unnamed5 first start then waiting;
                    transition __unnamed6 first waiting accept after 5 [s] then antennaOn;
                    state antennaOn {
                        entry send new turnOn() to spaceCraft.telecom.antenna;
                    }
                    // "Feature Membership -> Transition" is incompatible with textual.
                    state thrusterOn {
                        entry send new turnOn() to spaceCraft.propulsion.thruster;
                    }
                }
            }
        }
    }
    package SimConfig {
        item DynamicPowerRollup {
            @DS_Simulation::SimulationMetadata::'sim config' {
                executionTarget = DynamicPowerRollupPkg::DynamicPowerRollup::MissionScenario;
                autoStart = true;
                startupView = DynamicPowerRollupPkg::DynamicPowerRollup::MissionScenario.SpacecraftPowerRollup;
            }
        }
        item DynamicPowerRollup_WithUI {
            @DS_Simulation::SimulationMetadata::'sim config' {
                executionTarget = DynamicPowerRollupPkg::DynamicPowerRollup::MissionScenario;
                autoStart = false;
                UI = 'HTML UI'::RollpupUI_HTML;
                startupView = DynamicPowerRollupPkg::DynamicPowerRollup::MissionScenario.SpacecraftPowerRollup;
            }
        }
        view SimConfigView : StandardViewDefinitions::gv {
            render Views::asInterconnectionDiagram;
            view DynamicPowerRollupView : DiagramInterchange::NodeView {
                expose DynamicPowerRollup;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 7;
                    attribute :>> y = 28;
                    attribute :>> height = 126;
                    attribute :>> width = 292;
                }
            }
            view DynamicPowerRollup_WithUIView : DiagramInterchange::NodeView {
                expose DynamicPowerRollup_WithUI;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 7;
                    attribute :>> y = 182;
                    attribute :>> height = 140;
                    attribute :>> width = 292;
                }
            }
        }
    }
}

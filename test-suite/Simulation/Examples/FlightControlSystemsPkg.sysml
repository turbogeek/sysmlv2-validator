package FlightControlSystemsPkg {
    package FlightControlSystems {
        private import ScalarValues::**;
        private import SI::**;
        private import MessageItemDefs::*;
        part droneFlightLRU {
            in port commandPort : PortDefinitions::CommPort;
        }
        part FlightControlSystem {
            attribute time = localClock.currentTime;
            attribute batteryMass = powerControlSystem.battery_capacity_wh / 250;
            attribute mass = batteryMass / 0.4;
            satisfy requirement battery_soc {
                doc /* can’t land with less than 10% of battery */
                require constraint {
                    powerControlSystem.SOC >= 10
                }
            }
            satisfy requirement maxMass {
                doc /* Maximum weight of the drone is 1320 lbs */
                require constraint {
                    mass <= 600
                }
            }
            part flightController {
                attribute altitude : Real;
                attribute minCruiseTime : Integer := 20 * 60;
                bind altitude = altPort.altitude;
                exhibit state flightControlLogic {
                    state TakeOff {
                        entry send new TakeOffMsg() via altCommPort;
                    }
                    transition __unnamed28 first idle accept TakeOffMsg then TakeOff;
                    state Climb {
                        entry send new ClimbMsg() via altCommPort;
                        exit send new HoldMsg() via altCommPort;
                    }
                    state Hold {
                        exit send new DescendMsg() via altCommPort;
                    }
                    transition __unnamed33 first TakeOff accept after 20 [s] then Climb;
                    transition __unnamed31 first Climb accept when altitude >= 5000 then Hold;
                    transition __unnamed30 first Hold accept after minCruiseTime [s] then Descend;
                    state Descend {
                        exit send new LandMsg() via altCommPort;
                    }
                    state Land {
                        exit send new ShutDownMsg() via altCommPort;
                    }
                    transition __unnamed26 first Descend accept when altitude < 20 then Land;
                    state ShutDown;
                    transition __unnamed22 first Land then ShutDown;
                    transition __unnamed23 first Descend accept HoldMsg then Hold;
                    transition __unnamed24 first Descend accept ClimbMsg then Climb;
                    transition __unnamed25 first Hold accept DescendMsg then Descend;
                    transition __unnamed32 first Hold accept ClimbMsg then Climb;
                    transition __unnamed27 first Hold accept when powerControlSystem.SOC < 15 then Descend;
                    state idle;
                    transition __unnamed21 first start then idle;
                    transition __unnamed29 first ShutDown then done;
                }
                in port altCtrlPort : PortDefinitions::AltitudePort;
                in port altPort : ~PortDefinitions::AltitudePort2;
                out port altCommPort : PortDefinitions::CommPort;
            }
            part altitudeSensor : AltitudeSensor {
                connection __unnamed42 connect airSpeedSensor.dynPressPort to altitudeProcessor.dynamicPressPort;
                connection __unnamed41 connect airSpeedSensor.staticPressPort to altitudeProcessor.staticPressPort;
            }
            connection __unnamed43 connect altitudeSensor.altitudePort to flightController.altCtrlPort;
            interface __unnamed40 : PortDefinitions::SensorToControllerInterface connect altitudeSensor.altitudeProcessor.altPort to flightController.altPort;
            out port commPort : PortDefinitions::CommPort;
            connection __unnamed34 connect flightController.altCommPort to powerControlSystem.commPort;
            comment about commPort /* interface to HW*/
            part powerControlSystem {
                in port commPort : PortDefinitions::CommPort;
                attribute power : Real := 0;
                attribute highPowerMode : Real := 160000;
                attribute lowPowerMode : Real := 120000;
                attribute battery_capacity_wh : Integer default := 50000;
                attribute SOC : Real default := 80;
                attribute SOC2 : Real = power / (battery_capacity_wh * 36) + time * 0;
                exhibit state Operating {
                    state Off {
                        entry assign power := 0;
                    }
                    state On parallel {
                        state flightPhases {
                            state lowPower {
                                entry assign power := lowPowerMode;
                            }
                            state highPower {
                                entry assign power := highPowerMode;
                            }
                            transition __unnamed48 first start then highPower;
                            transition __unnamed47 first lowPower accept ClimbMsg then highPower;
                            transition __unnamed46 first highPower accept when OccurrenceFunctions::isDuring(flightController.flightControlLogic.Hold) then lowPower;
                        }
                        state usingPower {
                            state updateSOC {
                                entry assign SOC := SOC - power / (battery_capacity_wh * 36);
                            }
                            transition __unnamed45 first start then updateSOC;
                            transition __unnamed44 first updateSOC accept after 1 [s] then updateSOC;
                        }
                    }
                    transition __unnamed50 first start then Off;
                    transition __unnamed49 first Off accept TakeOffMsg then On;
                    transition __unnamed51 first On accept ShutDownMsg then Off;
                }
            }
        }
        part def AltitudeSensor {
            out port altitudePort : PortDefinitions::AltitudePort;
            part airSpeedSensor : AirSpeedSensor;
            part altitudeProcessor : AltitudeProcessor {
                in port dynamicPressPort : PortDefinitions::DynamicPressurePort;
                in port staticPressPort : ~PortDefinitions::StaticPressPort;
                out port altPort : PortDefinitions::AltitudePort2;
                exhibit state AltitudeProcessor_STM {
                    state updateAltitude {
                        entry assign altitude := 3.28084 * (44330.8 * (1.0 - (pressure / 101.325) ^ 0.190263));
                    }
                    // "Feature Membership -> Transition" is incompatible with textual.
                    state Off;
                    transition __unnamed35 first start then Off;
                    transition __unnamed36 first Off accept TakeOffMsg then updateAltitude;
                    transition __unnamed37 first updateAltitude accept LandMsg then Off;
                }
                attribute altitude : Real;
                attribute pressure : Real default := 101;
                binding __unnamed39 bind altitude = altPort.altitude;
            }
        }
        part def AirSpeedSensor {
            out port staticPressPort : PortDefinitions::StaticPressPort;
            out port dynPressPort : PortDefinitions::DynamicPressurePort;
            exhibit state AirSpeedSensor_STM {
                state Off;
                transition start then Off;
                state On {
                    entry send new StaticPressure() via staticPressPort;
                    do send new DynamicPressure() via dynPressPort;
                }
                transition Off accept TakeOffMsg then On;
                transition On accept LandMsg then Off;
            }
        }
        part def AltitudeProcessor {
            part controlThread : ControlLaw_Thread;
        }
        part def ControlLaw_Thread {
            attribute period : Real = 0.02 [s];
            attribute dispatch_protocol : String = "Periodic";
        }
        view Overview : StandardViewDefinitions::gv {
            render Views::asInterconnectionDiagram;
            view FlightControlSystemView : DiagramInterchange::NodeView {
                expose FlightControlSystem;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 105;
                    attribute :>> y = 119;
                    attribute :>> height = 39;
                    attribute :>> width = 171;
                }
                view commPortView : DiagramInterchange::NodeView {
                    expose FlightControlSystem::commPort;
                    render DiagramInterchange::asBorderNode {
                        attribute :>> onBorder = onBorder::right;
                        attribute :>> yc = 14;
                        attribute :>> height = 15;
                        attribute :>> width = 15;
                    }
                }
                view unnamedView : DiagramInterchange::EdgeView {
                    expose __unnamed19;
                    render DiagramInterchange::asEdge {
                        attribute :>> targetOnBorder = sourceOnBorder::left;
                        attribute :>> yt = 8;
                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                        attribute :>> ys = 11;
                    }
                    ref view :>> sourceView ::> commPortView;
                    ref view :>> targetView ::> droneFlightLRUView::commandPortView;
                }
                view unnamedView1 : DiagramInterchange::EdgeView {
                    expose __unnamed20;
                    render DiagramInterchange::asEdge {
                        attribute :>> targetOnBorder = sourceOnBorder::bottom;
                        attribute :>> xt = 7;
                        attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                        attribute :>> xs = 9;
                        attribute :>> wayPoints = ((0, 94), 318, 0);
                    }
                    ref view :>> sourceView ::> commPortView;
                    ref view :>> targetView ::> droneFlightLRUView::commandPortView;
                }
            }
            view droneFlightLRUView : DiagramInterchange::NodeView {
                expose droneFlightLRU;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 595;
                    attribute :>> y = 119;
                    attribute :>> height = 39;
                    attribute :>> width = 144;
                }
                view commandPortView : DiagramInterchange::NodeView {
                    expose droneFlightLRU::commandPort;
                    render DiagramInterchange::asBorderNode {
                        attribute :>> onBorder = onBorder::left;
                        attribute :>> yc = 17;
                        attribute :>> height = 15;
                        attribute :>> width = 15;
                    }
                }
            }
        }
        connection __unnamed19 connect FlightControlSystem.commPort to droneFlightLRU.commandPort;
        view FlightControlSystemView : StandardViewDefinitions::gv {
            render Views::asInterconnectionDiagram;
            view FlightControlSystemView : DiagramInterchange::NodeView {
                expose FlightControlSystem;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 5;
                    attribute :>> y = 5;
                    attribute :>> height = 1165;
                    attribute :>> width = 3171;
                }
                view flightControllerView : DiagramInterchange::NodeView {
                    expose FlightControlSystem::flightController;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 1185;
                        attribute :>> y = 114;
                        attribute :>> height = 370;
                        attribute :>> width = 1966;
                    }
                    view flightControlLogicView : DiagramInterchange::NodeView {
                        expose flightController::flightControlLogic;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 25;
                            attribute :>> y = 96;
                            attribute :>> height = 254;
                            attribute :>> width = 1921;
                        }
                        view HoldView : DiagramInterchange::NodeView {
                            expose flightControlLogic::Hold;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 815;
                                attribute :>> y = 86;
                                attribute :>> height = 72;
                                attribute :>> width = 244;
                            }
                        }
                        view LandView : DiagramInterchange::NodeView {
                            expose flightControlLogic::Land;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 1529;
                                attribute :>> y = 93;
                                attribute :>> height = 72;
                                attribute :>> width = 251;
                            }
                        }
                        view TakeOffView : DiagramInterchange::NodeView {
                            expose flightControlLogic::TakeOff;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 136;
                                attribute :>> y = 86;
                                attribute :>> height = 72;
                                attribute :>> width = 249;
                            }
                        }
                        view ClimbView : DiagramInterchange::NodeView {
                            expose flightControlLogic::Climb;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 458;
                                attribute :>> y = 86;
                                attribute :>> height = 86;
                                attribute :>> width = 237;
                            }
                        }
                        view DescendView : DiagramInterchange::NodeView {
                            expose flightControlLogic::Descend;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 1200;
                                attribute :>> y = 93;
                                attribute :>> height = 72;
                                attribute :>> width = 225;
                            }
                        }
                        view startView : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 31;
                                attribute :>> y = 51;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view ShutDownView : DiagramInterchange::NodeView {
                            expose flightControlLogic::ShutDown;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 1830;
                                attribute :>> y = 93;
                                attribute :>> height = 72;
                                attribute :>> width = 63;
                            }
                        }
                        view idleView : DiagramInterchange::NodeView {
                            expose flightControlLogic::idle;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 17;
                                attribute :>> y = 86;
                                attribute :>> height = 72;
                                attribute :>> width = 49;
                            }
                        }
                        view doneView : DiagramInterchange::NodeView {
                            expose States::StateAction::done;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 1851;
                                attribute :>> y = 184;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView1 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed21;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 23;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 9;
                            }
                            ref view :>> sourceView ::> startView;
                            ref view :>> targetView ::> idleView;
                        }
                        view unnamedView2 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed22;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 35;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 35;
                            }
                            ref view :>> sourceView ::> LandView;
                            ref view :>> targetView ::> ShutDownView;
                        }
                        view unnamedView3 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed23;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                attribute :>> xt = 130;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 112;
                                attribute :>> wayPoints = ((0, 26), -367, 0);
                            }
                            ref view :>> sourceView ::> DescendView;
                            ref view :>> targetView ::> HoldView;
                        }
                        view unnamedView4 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed24;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                attribute :>> xt = 95;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 172;
                                attribute :>> wayPoints = ((0, 68), -819, 0);
                            }
                            ref view :>> sourceView ::> DescendView;
                            ref view :>> targetView ::> ClimbView;
                        }
                        view unnamedView5 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed25;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                attribute :>> xt = 144;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 63;
                                attribute :>> wayPoints = ((0, 54), 466, 0);
                            }
                            ref view :>> sourceView ::> HoldView;
                            ref view :>> targetView ::> DescendView;
                        }
                        view unnamedView7 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed26;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 35;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 35;
                            }
                            ref view :>> sourceView ::> DescendView;
                            ref view :>> targetView ::> LandView;
                        }
                        view unnamedView8 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed27;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 95;
                                attribute :>> sourceOnBorder = sourceOnBorder::top;
                                attribute :>> xs = 123;
                                attribute :>> wayPoints = ((0, -17), 357, 0);
                            }
                            ref view :>> sourceView ::> HoldView;
                            ref view :>> targetView ::> DescendView;
                        }
                        view unnamedView10 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed28;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 35;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 35;
                            }
                            ref view :>> sourceView ::> idleView;
                            ref view :>> targetView ::> TakeOffView;
                        }
                        view unnamedView12 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed29;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 9;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 30;
                            }
                            ref view :>> sourceView ::> ShutDownView;
                            ref view :>> targetView ::> doneView;
                        }
                        view unnamedView15 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed30;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 32;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 39;
                            }
                            ref view :>> sourceView ::> HoldView;
                            ref view :>> targetView ::> DescendView;
                        }
                        view unnamedView24 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed31;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 42;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 42;
                            }
                            ref view :>> sourceView ::> ClimbView;
                            ref view :>> targetView ::> HoldView;
                        }
                        view unnamedView25 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed32;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                attribute :>> xt = 168;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 32;
                                attribute :>> wayPoints = ((0, 33), -221, 0);
                            }
                            ref view :>> sourceView ::> HoldView;
                            ref view :>> targetView ::> ClimbView;
                        }
                        view unnamedView30 : DiagramInterchange::EdgeView {
                            expose flightControlLogic::__unnamed33;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 39;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 39;
                            }
                            ref view :>> sourceView ::> TakeOffView;
                            ref view :>> targetView ::> ClimbView;
                        }
                    }
                    view altCtrlPortView : DiagramInterchange::NodeView {
                        expose flightController::altCtrlPort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 119;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view altCommPortView : DiagramInterchange::NodeView {
                        expose flightController::altCommPort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::bottom;
                            attribute :>> xc = 728;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view altPortView : DiagramInterchange::NodeView {
                        expose flightController::altPort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 217;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view unnamedView21 : DiagramInterchange::EdgeView {
                        expose FlightControlSystem::__unnamed34;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::top;
                            attribute :>> xt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                            attribute :>> xs = 7;
                            attribute :>> wayPoints = ((0, 36), 77, 0);
                        }
                        ref view :>> sourceView ::> altCommPortView;
                        ref view :>> targetView ::> powerControlSystemView::commPortView1;
                    }
                }
                view altitudeSensorView : DiagramInterchange::NodeView {
                    expose FlightControlSystem::altitudeSensor;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 16;
                        attribute :>> y = 121;
                        attribute :>> height = 450;
                        attribute :>> width = 992;
                    }
                    view altitudeProcessorView : DiagramInterchange::NodeView {
                        expose AltitudeSensor::altitudeProcessor;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 343;
                            attribute :>> y = 62;
                            attribute :>> height = 368;
                            attribute :>> width = 622;
                        }
                        view dynamicPressPortView : DiagramInterchange::NodeView {
                            expose altitudeProcessor::dynamicPressPort;
                            render DiagramInterchange::asBorderNode {
                                attribute :>> onBorder = onBorder::left;
                                attribute :>> yc = 89;
                                attribute :>> height = 15;
                                attribute :>> width = 15;
                            }
                        }
                        view staticPressPortView : DiagramInterchange::NodeView {
                            expose altitudeProcessor::staticPressPort;
                            render DiagramInterchange::asBorderNode {
                                attribute :>> onBorder = onBorder::left;
                                attribute :>> yc = 211;
                                attribute :>> height = 15;
                                attribute :>> width = 15;
                            }
                        }
                        view AltitudeProcessor_STMView : DiagramInterchange::NodeView {
                            expose altitudeProcessor::AltitudeProcessor_STM;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 18;
                                attribute :>> y = 96;
                                attribute :>> height = 252;
                                attribute :>> width = 444;
                            }
                            view OffView : DiagramInterchange::NodeView {
                                expose AltitudeProcessor_STM::Off;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 171;
                                    attribute :>> y = 84;
                                    attribute :>> height = 39;
                                    attribute :>> width = 49;
                                }
                            }
                            view startView1 : DiagramInterchange::NodeView {
                                expose States::StateAction::start;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 185;
                                    attribute :>> y = 49;
                                    attribute :>> height = 18;
                                    attribute :>> width = 18;
                                }
                            }
                            view updateAltitudeView : DiagramInterchange::NodeView {
                                expose AltitudeProcessor_STM::updateAltitude;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 10;
                                    attribute :>> y = 154;
                                    attribute :>> height = 75;
                                    attribute :>> width = 406;
                                }
                            }
                            view unnamedView : DiagramInterchange::EdgeView {
                                expose AltitudeProcessor_STM::__unnamed35;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::top;
                                    attribute :>> xt = 23;
                                    attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                    attribute :>> xs = 9;
                                }
                                ref view :>> sourceView ::> startView1;
                                ref view :>> targetView ::> OffView;
                            }
                            view unnamedView13 : DiagramInterchange::EdgeView {
                                expose AltitudeProcessor_STM::__unnamed36;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::top;
                                    attribute :>> xt = 186;
                                    attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                    attribute :>> xs = 25;
                                }
                                ref view :>> sourceView ::> OffView;
                                ref view :>> targetView ::> updateAltitudeView;
                            }
                            view unnamedView18 : DiagramInterchange::EdgeView {
                                expose AltitudeProcessor_STM::__unnamed37;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::right;
                                    attribute :>> yt = 18;
                                    attribute :>> sourceOnBorder = sourceOnBorder::top;
                                    attribute :>> xs = 245;
                                    attribute :>> wayPoints = (0, -52);
                                }
                                ref view :>> sourceView ::> updateAltitudeView;
                                ref view :>> targetView ::> OffView;
                            }
                            view unnamedView26 : DiagramInterchange::EdgeView {
                                expose AltitudeProcessor_STM::__unnamed38;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::right;
                                    attribute :>> yt = 11;
                                    attribute :>> sourceOnBorder = sourceOnBorder::top;
                                    attribute :>> xs = 378;
                                    attribute :>> wayPoints = ((0, -14), (46, 0), 0, 25);
                                }
                                ref view :>> sourceView ::> updateAltitudeView;
                                ref view :>> targetView ::> updateAltitudeView;
                            }
                        }
                        view altitudeView : DiagramInterchange::NodeView {
                            expose altitudeProcessor::altitude;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 483;
                                attribute :>> y = 131;
                                attribute :>> height = 42;
                                attribute :>> width = 119;
                            }
                        }
                        view altPortView1 : DiagramInterchange::NodeView {
                            expose altitudeProcessor::altPort;
                            render DiagramInterchange::asBorderNode {
                                attribute :>> onBorder = onBorder::right;
                                attribute :>> yc = 110;
                                attribute :>> height = 15;
                                attribute :>> width = 15;
                            }
                        }
                        view unnamedView16 : DiagramInterchange::EdgeView {
                            expose altitudeProcessor::__unnamed39;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 7;
                                attribute :>> sourceOnBorder = sourceOnBorder::top;
                                attribute :>> xs = 53;
                                attribute :>> wayPoints = (0, -21);
                            }
                            ref view :>> sourceView ::> altitudeView;
                            ref view :>> targetView ::> altPortView1;
                        }
                        view unnamedView29 : DiagramInterchange::EdgeView {
                            expose FlightControlSystem::__unnamed40;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 4;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 7;
                                attribute :>> wayPoints = ((110, 0), 0, 35);
                            }
                            ref view :>> sourceView ::> altPortView1;
                            ref view :>> targetView ::> flightControllerView::altPortView;
                        }
                    }
                    view altitudePortView : DiagramInterchange::NodeView {
                        expose AltitudeSensor::altitudePort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::right;
                            attribute :>> yc = 56;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view airSpeedSensorView : DiagramInterchange::NodeView {
                        expose AltitudeSensor::airSpeedSensor;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 14;
                            attribute :>> y = 67;
                            attribute :>> height = 84;
                            attribute :>> width = 203;
                        }
                        view dynPressPortView : DiagramInterchange::NodeView {
                            expose AirSpeedSensor::dynPressPort;
                            render DiagramInterchange::asBorderNode {
                                attribute :>> onBorder = onBorder::right;
                                attribute :>> yc = 17;
                                attribute :>> height = 15;
                                attribute :>> width = 15;
                            }
                        }
                        view staticPressPortView1 : DiagramInterchange::NodeView {
                            expose AirSpeedSensor::staticPressPort;
                            render DiagramInterchange::asBorderNode {
                                attribute :>> onBorder = onBorder::bottom;
                                attribute :>> xc = 98;
                                attribute :>> height = 15;
                                attribute :>> width = 15;
                            }
                        }
                        view unnamedView11 : DiagramInterchange::EdgeView {
                            expose altitudeSensor::__unnamed41;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 8;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 7;
                                attribute :>> wayPoints = (0, 116);
                            }
                            ref view :>> sourceView ::> staticPressPortView1;
                            ref view :>> targetView ::> altitudeProcessorView::staticPressPortView;
                        }
                        view unnamedView19 : DiagramInterchange::EdgeView {
                            expose altitudeSensor::__unnamed42;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 7;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 8;
                                attribute :>> wayPoints = ((11, 0), 0, 66);
                            }
                            ref view :>> sourceView ::> dynPressPortView;
                            ref view :>> targetView ::> altitudeProcessorView::dynamicPressPortView;
                        }
                    }
                    view unnamedView6 : DiagramInterchange::EdgeView {
                        expose FlightControlSystem::__unnamed43;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = ((48, 0), 0, 56);
                        }
                        ref view :>> sourceView ::> altitudePortView;
                        ref view :>> targetView ::> flightControllerView::altCtrlPortView;
                    }
                }
                view commPortView : DiagramInterchange::NodeView {
                    expose FlightControlSystem::commPort;
                    render DiagramInterchange::asBorderNode {
                        attribute :>> onBorder = onBorder::bottom;
                        attribute :>> xc = 1122;
                        attribute :>> height = 15;
                        attribute :>> width = 15;
                    }
                }
                view powerControlSystemView : DiagramInterchange::NodeView {
                    expose FlightControlSystem::powerControlSystem;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 1178;
                        attribute :>> y = 555;
                        attribute :>> height = 590;
                        attribute :>> width = 984;
                    }
                    view commPortView1 : DiagramInterchange::NodeView {
                        expose powerControlSystem::commPort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::top;
                            attribute :>> xc = 812;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view OperatingView : DiagramInterchange::NodeView {
                        expose powerControlSystem::Operating;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 14;
                            attribute :>> y = 69;
                            attribute :>> height = 501;
                            attribute :>> width = 950;
                        }
                        view OnView : DiagramInterchange::NodeView {
                            expose Operating::On;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 287;
                                attribute :>> y = 49;
                                attribute :>> height = 432;
                                attribute :>> width = 643;
                            }
                            view usingPowerView : DiagramInterchange::NodeView {
                                expose On::usingPower;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 239;
                                    attribute :>> height = 176;
                                    attribute :>> width = 436;
                                }
                                view updateSOCView : DiagramInterchange::NodeView {
                                    expose usingPower::updateSOC;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 49;
                                        attribute :>> y = 84;
                                        attribute :>> height = 72;
                                        attribute :>> width = 367;
                                    }
                                }
                                view startView2 : DiagramInterchange::NodeView {
                                    expose States::StateAction::start;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 14;
                                        attribute :>> y = 112;
                                        attribute :>> height = 18;
                                        attribute :>> width = 18;
                                    }
                                }
                                view unnamedView27 : DiagramInterchange::EdgeView {
                                    expose usingPower::__unnamed44;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::right;
                                        attribute :>> yt = 11;
                                        attribute :>> sourceOnBorder = sourceOnBorder::top;
                                        attribute :>> xs = 336;
                                        attribute :>> wayPoints = ((0, -10), (42, 0), 0, 21);
                                    }
                                    ref view :>> sourceView ::> updateSOCView;
                                    ref view :>> targetView ::> updateSOCView;
                                }
                                view unnamedView28 : DiagramInterchange::EdgeView {
                                    expose usingPower::__unnamed45;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 37;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 9;
                                    }
                                    ref view :>> sourceView ::> startView2;
                                    ref view :>> targetView ::> updateSOCView;
                                }
                            }
                            view flightPhasesView : DiagramInterchange::NodeView {
                                expose On::flightPhases;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 14;
                                    attribute :>> y = 64;
                                    attribute :>> height = 161;
                                    attribute :>> width = 609;
                                }
                                view lowPowerView : DiagramInterchange::NodeView {
                                    expose flightPhases::lowPower;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 343;
                                        attribute :>> y = 49;
                                        attribute :>> height = 72;
                                        attribute :>> width = 198;
                                    }
                                }
                                view highPowerView : DiagramInterchange::NodeView {
                                    expose flightPhases::highPower;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 63;
                                        attribute :>> y = 49;
                                        attribute :>> height = 72;
                                        attribute :>> width = 202;
                                    }
                                }
                                view startView3 : DiagramInterchange::NodeView {
                                    expose States::StateAction::start;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 21;
                                        attribute :>> y = 84;
                                        attribute :>> height = 18;
                                        attribute :>> width = 18;
                                    }
                                }
                                view unnamedView9 : DiagramInterchange::EdgeView {
                                    expose flightPhases::__unnamed46;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                        attribute :>> xt = 119;
                                        attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                        attribute :>> xs = 74;
                                        attribute :>> wayPoints = ((0, 26), 325, 0);
                                    }
                                    ref view :>> sourceView ::> highPowerView;
                                    ref view :>> targetView ::> lowPowerView;
                                }
                                view unnamedView17 : DiagramInterchange::EdgeView {
                                    expose flightPhases::__unnamed47;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::right;
                                        attribute :>> yt = 35;
                                        attribute :>> sourceOnBorder = sourceOnBorder::left;
                                        attribute :>> ys = 35;
                                    }
                                    ref view :>> sourceView ::> lowPowerView;
                                    ref view :>> targetView ::> highPowerView;
                                }
                                view unnamedView23 : DiagramInterchange::EdgeView {
                                    expose flightPhases::__unnamed48;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 44;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 9;
                                    }
                                    ref view :>> sourceView ::> startView3;
                                    ref view :>> targetView ::> highPowerView;
                                }
                            }
                        }
                        view OffView1 : DiagramInterchange::NodeView {
                            expose Operating::Off;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 63;
                                attribute :>> y = 59;
                                attribute :>> height = 72;
                                attribute :>> width = 128;
                            }
                        }
                        view startView4 : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 87;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView14 : DiagramInterchange::EdgeView {
                            expose Operating::__unnamed49;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 33;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 23;
                            }
                            ref view :>> sourceView ::> OffView1;
                            ref view :>> targetView ::> OnView;
                        }
                        view unnamedView20 : DiagramInterchange::EdgeView {
                            expose Operating::__unnamed50;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 37;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 9;
                            }
                            ref view :>> sourceView ::> startView4;
                            ref view :>> targetView ::> OffView1;
                        }
                        view unnamedView22 : DiagramInterchange::EdgeView {
                            expose Operating::__unnamed51;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::right;
                                attribute :>> yt = 58;
                                attribute :>> sourceOnBorder = sourceOnBorder::left;
                                attribute :>> ys = 68;
                            }
                            ref view :>> sourceView ::> OnView;
                            ref view :>> targetView ::> OffView1;
                        }
                    }
                }
                view battery_socView : DiagramInterchange::NodeView {
                    expose FlightControlSystem::battery_soc;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 219;
                        attribute :>> y = 611;
                        attribute :>> height = 102;
                        attribute :>> width = 211;
                    }
                }
                view maxMassView : DiagramInterchange::NodeView {
                    expose FlightControlSystem::maxMass;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 51;
                        attribute :>> y = 611;
                        attribute :>> height = 114;
                        attribute :>> width = 122;
                    }
                }
            }
        }
        interface __unnamed20 connect FlightControlSystem.commPort to droneFlightLRU.commandPort {
            flow {
                end ::> source;
                end ::> target {
                    inout type;
                }
            }
        }
    }
    package PortDefinitions {
        private import ScalarValues::**;
        private import SI::**;
        port def CommPort {
            item msg;
        }
        port def StaticPressPort {
            item staticPress : MessageItemDefs::StaticPressure;
        }
        port def DynamicPressurePort {
            item dynPress : MessageItemDefs::DynamicPressure;
        }
        port def AltitudePort {
            attribute alt : MessageItemDefs::Altitude;
        }
        port def AltitudePort2 {
            out attribute altitude : Real;
        }
        port def SpeedPort {
            in attribute speed : Real;
        }
        interface def SensorToControllerInterface {
            end p1 : AltitudePort2;
            end p2 : ~AltitudePort2;
            flow p1.altitude to p2.altitude;
        }
    }
    package MessageItemDefs {
        item def TakeOffMsg;
        item def ClimbMsg;
        item def HoldMsg;
        item def LandMsg;
        item def DescendMsg;
        item def ShutDownMsg;
        item def DynamicPressure;
        item def StaticPressure;
        item def Altitude;
    }
    package SimConfig {
        item FlightConfig {
            @DS_Simulation::SimulationMetadata::'sim config' {
                executionTarget = FlightControlSystems::FlightControlSystem;
                autoStart := true;
                speed := 1;
                startupView = FlightControlSystems::FlightControlSystemView;
                decimalPlaces = 5;
                UI = 'HTML UI'::FlightControlUI_HTML;
            }
        }
    }
}

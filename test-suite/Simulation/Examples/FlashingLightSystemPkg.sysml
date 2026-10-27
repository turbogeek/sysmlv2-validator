package FlashingLightSystemPkg {
    package FlashingLightDomain {
        private import ScalarValues::**;
        private import SI::**;
        private import InterfaceDefs::**;
        private import Payloads::**;
        private import PortDefs::**;
        view FlashingLight : StandardViewDefinitions::iv {
            render Views::asInterconnectionDiagram;
            view FlashingLightSystemView : DiagramInterchange::NodeView {
                expose FlashingLightSystem;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 5;
                    attribute :>> y = 5;
                    attribute :>> height = 407;
                    attribute :>> width = 1058;
                }
                view lightView : DiagramInterchange::NodeView {
                    expose FlashingLightSystem::light;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 870;
                        attribute :>> y = 51;
                        attribute :>> height = 336;
                        attribute :>> width = 168;
                    }
                    view Light_STMView : DiagramInterchange::NodeView {
                        expose Light::Light_STM;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 21;
                            attribute :>> y = 56;
                            attribute :>> height = 259;
                            attribute :>> width = 119;
                        }
                        view OffView : DiagramInterchange::NodeView {
                            expose Light_STM::Off;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 91;
                                attribute :>> height = 39;
                                attribute :>> width = 77;
                            }
                        }
                        view OnView : DiagramInterchange::NodeView {
                            expose Light_STM::On;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 168;
                                attribute :>> height = 39;
                                attribute :>> width = 77;
                            }
                        }
                        view startView : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 49;
                                attribute :>> y = 56;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView : DiagramInterchange::EdgeView {
                            expose Light_STM::__unnamed7;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 37;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 9;
                            }
                            ref view :>> sourceView ::> startView;
                            ref view :>> targetView ::> OffView;
                        }
                        view unnamedView4 : DiagramInterchange::EdgeView {
                            expose Light_STM::__unnamed8;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 56;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 56;
                            }
                            ref view :>> sourceView ::> OffView;
                            ref view :>> targetView ::> OnView;
                        }
                        view unnamedView10 : DiagramInterchange::EdgeView {
                            expose Light_STM::__unnamed9;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                attribute :>> xt = 14;
                                attribute :>> sourceOnBorder = sourceOnBorder::top;
                                attribute :>> xs = 14;
                            }
                            ref view :>> sourceView ::> OnView;
                            ref view :>> targetView ::> OffView;
                        }
                    }
                    view lightInView : DiagramInterchange::NodeView {
                        expose Light::lightIn;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 161;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                }
                view timerView : DiagramInterchange::NodeView {
                    expose FlashingLightSystem::timer;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 471;
                        attribute :>> y = 51;
                        attribute :>> height = 335;
                        attribute :>> width = 300;
                    }
                    view Timer_STMView : DiagramInterchange::NodeView {
                        expose Timer::Timer_STM;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 21;
                            attribute :>> y = 56;
                            attribute :>> height = 259;
                            attribute :>> width = 259;
                        }
                        view IdleView : DiagramInterchange::NodeView {
                            expose Timer_STM::Idle;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 91;
                                attribute :>> height = 39;
                                attribute :>> width = 177;
                            }
                        }
                        view TickingView : DiagramInterchange::NodeView {
                            expose Timer_STM::Ticking;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 168;
                                attribute :>> height = 72;
                                attribute :>> width = 186;
                            }
                        }
                        view startView1 : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 101;
                                attribute :>> y = 56;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView5 : DiagramInterchange::EdgeView {
                            expose Timer_STM::__unnamed10;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                attribute :>> xt = 35;
                                attribute :>> sourceOnBorder = sourceOnBorder::top;
                                attribute :>> xs = 35;
                            }
                            ref view :>> sourceView ::> TickingView;
                            ref view :>> targetView ::> IdleView;
                        }
                        view unnamedView6 : DiagramInterchange::EdgeView {
                            expose Timer_STM::__unnamed11;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 89;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 9;
                            }
                            ref view :>> sourceView ::> startView1;
                            ref view :>> targetView ::> IdleView;
                        }
                        view unnamedView8 : DiagramInterchange::EdgeView {
                            expose Timer_STM::__unnamed12;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 129;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 129;
                            }
                            ref view :>> sourceView ::> IdleView;
                            ref view :>> targetView ::> TickingView;
                        }
                        view unnamedView9 : DiagramInterchange::EdgeView {
                            expose Timer_STM::__unnamed13;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::right;
                                attribute :>> yt = 63;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 32;
                                attribute :>> wayPoints = ((40, 0), 0, 31);
                            }
                            ref view :>> sourceView ::> TickingView;
                            ref view :>> targetView ::> TickingView;
                        }
                    }
                    view timerInView : DiagramInterchange::NodeView {
                        expose Timer::timerIn;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 168;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view timerOutView : DiagramInterchange::NodeView {
                        expose Timer::timerOut;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::right;
                            attribute :>> yc = 70;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view unnamedView1 : DiagramInterchange::EdgeView {
                        expose FlashingLightSystem::__unnamed14;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = ((29, 0), 0, 91);
                        }
                        ref view :>> sourceView ::> timerOutView;
                        ref view :>> targetView ::> lightView::lightInView;
                    }
                }
                view buttonView : DiagramInterchange::NodeView {
                    expose FlashingLightSystem::button;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 10;
                        attribute :>> y = 51;
                        attribute :>> height = 336;
                        attribute :>> width = 359;
                    }
                    view Button_STMView : DiagramInterchange::NodeView {
                        expose Button::Button_STM;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 14;
                            attribute :>> y = 56;
                            attribute :>> height = 260;
                            attribute :>> width = 325;
                        }
                        view ReleasedView : DiagramInterchange::NodeView {
                            expose Button_STM::Released;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 111;
                                attribute :>> y = 91;
                                attribute :>> height = 39;
                                attribute :>> width = 194;
                            }
                        }
                        view PressedView : DiagramInterchange::NodeView {
                            expose Button_STM::Pressed;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 111;
                                attribute :>> y = 168;
                                attribute :>> height = 72;
                                attribute :>> width = 194;
                            }
                        }
                        view startView2 : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 195;
                                attribute :>> y = 56;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView2 : DiagramInterchange::EdgeView {
                            expose Button_STM::__unnamed15;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 93;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 9;
                            }
                            ref view :>> sourceView ::> startView2;
                            ref view :>> targetView ::> ReleasedView;
                        }
                        view unnamedView3 : DiagramInterchange::EdgeView {
                            expose Button_STM::__unnamed16;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                attribute :>> xt = 67;
                                attribute :>> sourceOnBorder = sourceOnBorder::top;
                                attribute :>> xs = 67;
                            }
                            ref view :>> sourceView ::> PressedView;
                            ref view :>> targetView ::> ReleasedView;
                        }
                        view unnamedView7 : DiagramInterchange::EdgeView {
                            expose Button_STM::__unnamed17;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 133;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 133;
                            }
                            ref view :>> sourceView ::> ReleasedView;
                            ref view :>> targetView ::> PressedView;
                        }
                    }
                    view buttonOutView : DiagramInterchange::NodeView {
                        expose Button::buttonOut;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::right;
                            attribute :>> yc = 77;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view unnamedView11 : DiagramInterchange::EdgeView {
                        expose FlashingLightSystem::__unnamed18;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = ((29, 0), 0, 91);
                        }
                        ref view :>> sourceView ::> buttonOutView;
                        ref view :>> targetView ::> timerView::timerInView;
                    }
                }
            }
        }
        part FlashingLightSystem {
            part timer : Timer;
            part button : Button;
            part light : Light;
            interface __unnamed18 : ButtonToTimerInterface connect button.buttonOut to timer.timerIn;
            interface __unnamed14 : TimerToLightInterface connect timer.timerOut to light.lightIn;
        }
        part def Timer {
            exhibit state Timer_STM {
                state Idle;
                state Ticking {
                    entry send new Tick() via timerOut;
                }
                transition __unnamed11 first start then Idle;
                transition __unnamed12 first Idle accept Start then Ticking;
                transition __unnamed10 first Ticking accept Stop then Idle;
                transition __unnamed13 first Ticking accept after 1 [s] then Ticking;
            }
            port timerIn : TimerInPort;
            port timerOut : TimerOutPort;
        }
        part def Light {
            exhibit state Light_STM {
                state Off;
                state On;
                transition __unnamed7 first start then Off;
                transition __unnamed8 first Off accept Tick then On;
                transition __unnamed9 first On accept Tick then Off;
            }
            port lightIn : LightPort;
        }
        part def Button {
            exhibit state Button_STM {
                state Released;
                state Pressed {
                    entry send new Start() via buttonOut;
                }
                transition __unnamed15 first start then Released;
                transition __unnamed17 first Released accept Push then Pressed;
                transition __unnamed16 first Pressed accept Release do send new Stop() via buttonOut then Released;
            }
            port buttonOut : ButtonPort;
        }
    }
    package Payloads {
        item def Push;
        item def Release;
        item def Start;
        item def Stop;
        item def Terminate;
        item def Tick;
    }
    package InterfaceDefs {
        private import PortDefs::**;
        private import Payloads::**;
        interface def ButtonToTimerInterface {
            end p1 : ButtonPort;
            end p2 : TimerInPort;
            flow of Start from p1.start to p2.start;
            flow of Stop from p1.start to p2.start;
        }
        interface def TimerToLightInterface {
            end p1 : TimerOutPort;
            end p2 : LightPort;
            flow of Tick from p1.tick to p2.tick;
        }
    }
    package PortDefs {
        private import Payloads::**;
        port def ButtonPort {
            out item start : Start;
            out item stop : Stop;
        }
        port def TimerOutPort {
            out item tick : Tick;
        }
        port def LightPort {
            in item tick : Tick;
        }
        port def TimerInPort {
            in item start : Start;
            in item stop : Stop;
        }
    }
    package SimConfig {
        item FlashingLight {
            @DS_Simulation::SimulationMetadata::'sim config' {
                executionTarget = FlashingLightSystemPkg::FlashingLightDomain::FlashingLightSystem;
                autoStart = true;
                startupView = FlashingLightSystemPkg::FlashingLightDomain::FlashingLight;
            }
        }
        item FlashingLight_WithUI {
            @DS_Simulation::SimulationMetadata::'sim config' {
                executionTarget = FlashingLightSystemPkg::FlashingLightDomain::FlashingLightSystem;
                UI = 'HTML UI'::FlashingLightUI_HTML;
                autoStart = true;
                startupView = FlashingLightSystemPkg::FlashingLightDomain::FlashingLight;
            }
        }
        view SimConfigView : StandardViewDefinitions::gv {
            render Views::asInterconnectionDiagram;
            view FlashingLightView : DiagramInterchange::NodeView {
                expose FlashingLight;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 7;
                    attribute :>> y = 28;
                    attribute :>> height = 140;
                    attribute :>> width = 218;
                }
            }
            view FlashingLight_WithUIView : DiagramInterchange::NodeView {
                expose FlashingLight_WithUI;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 252;
                    attribute :>> y = 28;
                    attribute :>> height = 140;
                    attribute :>> width = 214;
                }
            }
        }
    }
}

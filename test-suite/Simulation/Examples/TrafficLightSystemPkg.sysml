package TrafficLightSystemPkg {
    package TrafficLightDomain {
        private import ScalarValues::**;
        private import SI::**;
        private import Payloads::**;
        part TrafficLightSystem {
            part signalHead : SignalHead {
                interface __unnamed55 connect headPortGreen to GREEN.lampPort;
                interface __unnamed57 connect headPortAmber to AMBER.lampPort;
                interface __unnamed56 connect headPortRed to RED.lampPort;
            }
            part controller : Controller {
                exhibit state LightLogic {
                    state RedOn {
                        entry send new On() via ctrlPortRed;
                        exit send new Off() via ctrlPortRed;
                    }
                    transition __unnamed64 first States::StateAction::start then RedOn;
                    state AmberOn {
                        entry send new On() via ctrlPortAmber;
                        exit send new Off() via ctrlPortAmber;
                    }
                    state GreenOn {
                        entry send new On() via ctrlPortGreen;
                    }
                    transition __unnamed65 first RedOn accept after 5 [s] then GreenOn;
                    transition __unnamed62 first GreenOn accept after 5 [s] then GreenBlink;
                    transition __unnamed61 first AmberOn accept after 2 [s] then RedOn;
                    state GreenBlink {
                        state GreenOff {
                            entry send new Off() via ctrlPortGreen;
                        }
                        state GreenOn {
                            entry send new On() via ctrlPortGreen;
                        }
                        transition __unnamed59 first GreenOff accept after 1 [s] then GreenOn;
                        transition __unnamed60 first start then GreenOff;
                        transition __unnamed58 first GreenOn accept after 1 [s] then GreenOff;
                    }
                    // "Feature Membership -> Transition" is incompatible with textual.
                }
            }
            interface __unnamed68 connect controller.ctrlPortRed to signalHead.headPortRed;
            interface __unnamed66 connect controller.ctrlPortAmber to signalHead.headPortAmber;
            interface __unnamed67 connect controller.ctrlPortGreen to signalHead.headPortGreen;
        }
        part def SignalHead {
            port headPortRed : PortDefs::HeadPort;
            port headPortAmber : PortDefs::HeadPort;
            port headPortGreen : PortDefs::HeadPort;
            part RED : Lamp;
            part AMBER : Lamp;
            part GREEN : Lamp;
        }
        part def Lamp {
            port lampPort : PortDefs::LampPort;
            exhibit state Lamp_STM {
                state Off;
                transition __unnamed52 first States::StateAction::start then Off;
                state On;
                transition __unnamed53 first Off accept Payloads::On then On;
                transition __unnamed54 first On accept Payloads::Off then Off;
            }
            view LampView : StandardViewDefinitions::stv {
                render Views::asInterconnectionDiagram;
                view LampView : DiagramInterchange::NodeView {
                    expose Lamp;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 5;
                        attribute :>> y = 5;
                        attribute :>> height = 220;
                        attribute :>> width = 325;
                    }
                    view Lamp_STMView : DiagramInterchange::NodeView {
                        expose Lamp_STM;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 23;
                            attribute :>> y = 72;
                            attribute :>> height = 138;
                            attribute :>> width = 285;
                        }
                        view OffView : DiagramInterchange::NodeView {
                            expose Lamp_STM::Off;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 63;
                                attribute :>> y = 70;
                                attribute :>> height = 39;
                                attribute :>> width = 49;
                            }
                        }
                        view OnView : DiagramInterchange::NodeView {
                            expose Lamp_STM::On;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 210;
                                attribute :>> y = 70;
                                attribute :>> height = 39;
                                attribute :>> width = 49;
                            }
                        }
                        view startView : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 14;
                                attribute :>> y = 84;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView : DiagramInterchange::EdgeView {
                            expose Lamp_STM::__unnamed52;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 23;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 9;
                            }
                            ref view :>> sourceView ::> startView;
                            ref view :>> targetView ::> OffView;
                        }
                        view unnamedView1 : DiagramInterchange::EdgeView {
                            expose Lamp_STM::__unnamed53;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 30;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 30;
                                attribute :>> wayPoints = ((22, 0), (0, 8), (54, 0), 0, -8);
                            }
                            ref view :>> sourceView ::> OffView;
                            ref view :>> targetView ::> OnView;
                        }
                        view unnamedView2 : DiagramInterchange::EdgeView {
                            expose Lamp_STM::__unnamed54;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::right;
                                attribute :>> yt = 10;
                                attribute :>> sourceOnBorder = sourceOnBorder::left;
                                attribute :>> ys = 10;
                                attribute :>> wayPoints = ((-22, 0), (0, -8), (-54, 0), 0, 8);
                            }
                            ref view :>> sourceView ::> OnView;
                            ref view :>> targetView ::> OffView;
                        }
                    }
                    view lampPortView : DiagramInterchange::NodeView {
                        expose lampPort;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::right;
                            attribute :>> yc = 58;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                }
            }
        }
        part def Controller {
            port ctrlPortRed : PortDefs::CtrlPort;
            port ctrlPortAmber : PortDefs::CtrlPort;
            port ctrlPortGreen : PortDefs::CtrlPort;
        }
        view TrafficLightSystemView : StandardViewDefinitions::gv {
            part : DS_Styles::StyleSheets::BlackAndWhiteStyleSheet :> DS_Views::CoreViews::sv::explicitlyAppliedStyleSheets;
            render Views::asInterconnectionDiagram;
            view TrafficLightSystemView : DiagramInterchange::NodeView {
                expose TrafficLightSystem;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 5;
                    attribute :>> y = 5;
                    attribute :>> height = 795;
                    attribute :>> width = 1556;
                }
                view signalHeadView : DiagramInterchange::NodeView {
                    expose TrafficLightSystem::signalHead;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 130;
                        attribute :>> y = 49;
                        attribute :>> height = 346;
                        attribute :>> width = 1106;
                    }
                    view REDView : DiagramInterchange::NodeView {
                        expose SignalHead::RED;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 47;
                            attribute :>> y = 121;
                            attribute :>> height = 214;
                            attribute :>> width = 326;
                        }
                        view Lamp_STMView : DiagramInterchange::NodeView {
                            expose Lamp::Lamp_STM;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 56;
                                attribute :>> height = 138;
                                attribute :>> width = 285;
                            }
                            view OffView : DiagramInterchange::NodeView {
                                expose Lamp_STM::Off;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 63;
                                    attribute :>> y = 70;
                                    attribute :>> height = 39;
                                    attribute :>> width = 49;
                                }
                            }
                            view OnView : DiagramInterchange::NodeView {
                                expose Lamp_STM::On;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 210;
                                    attribute :>> y = 70;
                                    attribute :>> height = 39;
                                    attribute :>> width = 49;
                                }
                            }
                            view startView : DiagramInterchange::NodeView {
                                expose States::StateAction::start;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 14;
                                    attribute :>> y = 77;
                                    attribute :>> height = 18;
                                    attribute :>> width = 18;
                                }
                            }
                            view unnamedView7 : DiagramInterchange::EdgeView {
                                expose Lamp_STM::__unnamed53;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 27;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 27;
                                    attribute :>> wayPoints = ((22, 0), (0, 8), (54, 0), 0, -8);
                                }
                                ref view :>> sourceView ::> OffView;
                                ref view :>> targetView ::> OnView;
                            }
                            view unnamedView16 : DiagramInterchange::EdgeView {
                                expose Lamp_STM::__unnamed54;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::right;
                                    attribute :>> yt = 7;
                                    attribute :>> sourceOnBorder = sourceOnBorder::left;
                                    attribute :>> ys = 7;
                                    attribute :>> wayPoints = ((-22, 0), (0, -8), (-54, 0), 0, 8);
                                }
                                ref view :>> sourceView ::> OnView;
                                ref view :>> targetView ::> OffView;
                            }
                            view unnamedView19 : DiagramInterchange::EdgeView {
                                expose Lamp_STM::__unnamed52;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 16;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 9;
                                }
                                ref view :>> sourceView ::> startView;
                                ref view :>> targetView ::> OffView;
                            }
                        }
                        view lampPortView : DiagramInterchange::NodeView {
                            expose Lamp::lampPort;
                            render DiagramInterchange::asBorderNode {
                                attribute :>> onBorder = onBorder::top;
                                attribute :>> xc = 154;
                                attribute :>> height = 15;
                                attribute :>> width = 15;
                            }
                        }
                    }
                    view AMBERView : DiagramInterchange::NodeView {
                        expose SignalHead::AMBER;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 404;
                            attribute :>> y = 122;
                            attribute :>> height = 214;
                            attribute :>> width = 326;
                        }
                        view Lamp_STMView1 : DiagramInterchange::NodeView {
                            expose Lamp::Lamp_STM;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 56;
                                attribute :>> height = 138;
                                attribute :>> width = 285;
                            }
                            view OffView1 : DiagramInterchange::NodeView {
                                expose Lamp_STM::Off;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 63;
                                    attribute :>> y = 70;
                                    attribute :>> height = 39;
                                    attribute :>> width = 49;
                                }
                            }
                            view OnView1 : DiagramInterchange::NodeView {
                                expose Lamp_STM::On;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 210;
                                    attribute :>> y = 70;
                                    attribute :>> height = 39;
                                    attribute :>> width = 49;
                                }
                            }
                            view startView1 : DiagramInterchange::NodeView {
                                expose States::StateAction::start;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 14;
                                    attribute :>> y = 84;
                                    attribute :>> height = 18;
                                    attribute :>> width = 18;
                                }
                            }
                            view unnamedView2 : DiagramInterchange::EdgeView {
                                expose Lamp_STM::__unnamed53;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 31;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 31;
                                    attribute :>> wayPoints = ((22, 0), (0, 8), (54, 0), 0, -8);
                                }
                                ref view :>> sourceView ::> OffView1;
                                ref view :>> targetView ::> OnView1;
                            }
                            view unnamedView9 : DiagramInterchange::EdgeView {
                                expose Lamp_STM::__unnamed52;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 23;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 9;
                                }
                                ref view :>> sourceView ::> startView1;
                                ref view :>> targetView ::> OffView1;
                            }
                            view unnamedView17 : DiagramInterchange::EdgeView {
                                expose Lamp_STM::__unnamed54;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::right;
                                    attribute :>> yt = 11;
                                    attribute :>> sourceOnBorder = sourceOnBorder::left;
                                    attribute :>> ys = 11;
                                    attribute :>> wayPoints = ((-22, 0), (0, -8), (-54, 0), 0, 8);
                                }
                                ref view :>> sourceView ::> OnView1;
                                ref view :>> targetView ::> OffView1;
                            }
                        }
                        view lampPortView1 : DiagramInterchange::NodeView {
                            expose Lamp::lampPort;
                            render DiagramInterchange::asBorderNode {
                                attribute :>> onBorder = onBorder::top;
                                attribute :>> xc = 161;
                                attribute :>> height = 15;
                                attribute :>> width = 15;
                            }
                        }
                    }
                    view GREENView : DiagramInterchange::NodeView {
                        expose SignalHead::GREEN;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 754;
                            attribute :>> y = 121;
                            attribute :>> height = 214;
                            attribute :>> width = 326;
                        }
                        view Lamp_STMView2 : DiagramInterchange::NodeView {
                            expose Lamp::Lamp_STM;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 21;
                                attribute :>> y = 56;
                                attribute :>> height = 138;
                                attribute :>> width = 285;
                            }
                            view OffView2 : DiagramInterchange::NodeView {
                                expose Lamp_STM::Off;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 63;
                                    attribute :>> y = 70;
                                    attribute :>> height = 39;
                                    attribute :>> width = 49;
                                }
                            }
                            view OnView2 : DiagramInterchange::NodeView {
                                expose Lamp_STM::On;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 210;
                                    attribute :>> y = 70;
                                    attribute :>> height = 39;
                                    attribute :>> width = 49;
                                }
                            }
                            view startView2 : DiagramInterchange::NodeView {
                                expose States::StateAction::start;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 14;
                                    attribute :>> y = 77;
                                    attribute :>> height = 18;
                                    attribute :>> width = 18;
                                }
                            }
                            view unnamedView3 : DiagramInterchange::EdgeView {
                                expose Lamp_STM::__unnamed53;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 28;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 28;
                                    attribute :>> wayPoints = ((22, 0), (0, 8), (54, 0), 0, -8);
                                }
                                ref view :>> sourceView ::> OffView2;
                                ref view :>> targetView ::> OnView2;
                            }
                            view unnamedView20 : DiagramInterchange::EdgeView {
                                expose Lamp_STM::__unnamed52;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 16;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 9;
                                }
                                ref view :>> sourceView ::> startView2;
                                ref view :>> targetView ::> OffView2;
                            }
                            view unnamedView21 : DiagramInterchange::EdgeView {
                                expose Lamp_STM::__unnamed54;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::right;
                                    attribute :>> yt = 8;
                                    attribute :>> sourceOnBorder = sourceOnBorder::left;
                                    attribute :>> ys = 8;
                                    attribute :>> wayPoints = ((-22, 0), (0, -8), (-54, 0), 0, 8);
                                }
                                ref view :>> sourceView ::> OnView2;
                                ref view :>> targetView ::> OffView2;
                            }
                        }
                        view lampPortView2 : DiagramInterchange::NodeView {
                            expose Lamp::lampPort;
                            render DiagramInterchange::asBorderNode {
                                attribute :>> onBorder = onBorder::top;
                                attribute :>> xc = 133;
                                attribute :>> height = 15;
                                attribute :>> width = 15;
                            }
                        }
                    }
                    view headPortRedView : DiagramInterchange::NodeView {
                        expose SignalHead::headPortRed;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 107;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view headPortAmberView : DiagramInterchange::NodeView {
                        expose SignalHead::headPortAmber;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 79;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view headPortGreenView : DiagramInterchange::NodeView {
                        expose SignalHead::headPortGreen;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 51;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view unnamedView10 : DiagramInterchange::EdgeView {
                        expose signalHead::__unnamed55;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::top;
                            attribute :>> xt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 4;
                            attribute :>> wayPoints = (879, 0);
                        }
                        ref view :>> sourceView ::> headPortGreenView;
                        ref view :>> targetView ::> GREENView::lampPortView2;
                    }
                    view unnamedView18 : DiagramInterchange::EdgeView {
                        expose signalHead::__unnamed56;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::top;
                            attribute :>> xt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = (193, 0);
                        }
                        ref view :>> sourceView ::> headPortRedView;
                        ref view :>> targetView ::> REDView::lampPortView;
                    }
                    view unnamedView22 : DiagramInterchange::EdgeView {
                        expose signalHead::__unnamed57;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::top;
                            attribute :>> xt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = (557, 0);
                        }
                        ref view :>> sourceView ::> headPortAmberView;
                        ref view :>> targetView ::> AMBERView::lampPortView1;
                    }
                }
                view controllerView : DiagramInterchange::NodeView {
                    expose TrafficLightSystem::controller;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 130;
                        attribute :>> y = 429;
                        attribute :>> height = 356;
                        attribute :>> width = 1406;
                    }
                    view LightLogicView : DiagramInterchange::NodeView {
                        expose controller::LightLogic;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 21;
                            attribute :>> y = 49;
                            attribute :>> height = 287;
                            attribute :>> width = 1365;
                        }
                        view GreenBlinkView : DiagramInterchange::NodeView {
                            expose LightLogic::GreenBlink;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 532;
                                attribute :>> y = 68;
                                attribute :>> height = 181;
                                attribute :>> width = 536;
                            }
                            view GreenOffView : DiagramInterchange::NodeView {
                                expose GreenBlink::GreenOff;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 28;
                                    attribute :>> y = 86;
                                    attribute :>> height = 72;
                                    attribute :>> width = 203;
                                }
                            }
                            view GreenOnView : DiagramInterchange::NodeView {
                                expose GreenBlink::GreenOn;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 315;
                                    attribute :>> y = 89;
                                    attribute :>> height = 72;
                                    attribute :>> width = 201;
                                }
                            }
                            view startView3 : DiagramInterchange::NodeView {
                                expose States::StateAction::start;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 91;
                                    attribute :>> y = 56;
                                    attribute :>> height = 18;
                                    attribute :>> width = 18;
                                }
                            }
                            view unnamedView5 : DiagramInterchange::EdgeView {
                                expose GreenBlink::__unnamed58;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::right;
                                    attribute :>> yt = 14;
                                    attribute :>> sourceOnBorder = sourceOnBorder::left;
                                    attribute :>> ys = 11;
                                }
                                ref view :>> sourceView ::> GreenOnView;
                                ref view :>> targetView ::> GreenOffView;
                            }
                            view unnamedView13 : DiagramInterchange::EdgeView {
                                expose GreenBlink::__unnamed59;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::left;
                                    attribute :>> yt = 57;
                                    attribute :>> sourceOnBorder = sourceOnBorder::right;
                                    attribute :>> ys = 60;
                                }
                                ref view :>> sourceView ::> GreenOffView;
                                ref view :>> targetView ::> GreenOnView;
                            }
                            view unnamedView15 : DiagramInterchange::EdgeView {
                                expose GreenBlink::__unnamed60;
                                render DiagramInterchange::asEdge {
                                    attribute :>> targetOnBorder = sourceOnBorder::top;
                                    attribute :>> xt = 72;
                                    attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                    attribute :>> xs = 9;
                                }
                                ref view :>> sourceView ::> startView3;
                                ref view :>> targetView ::> GreenOffView;
                            }
                        }
                        view GreenOnView1 : DiagramInterchange::NodeView {
                            expose LightLogic::GreenOn;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 259;
                                attribute :>> y = 131;
                                attribute :>> height = 72;
                                attribute :>> width = 201;
                            }
                        }
                        view RedOnView : DiagramInterchange::NodeView {
                            expose LightLogic::RedOn;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 14;
                                attribute :>> y = 122;
                                attribute :>> height = 86;
                                attribute :>> width = 190;
                            }
                        }
                        view AmberOnView : DiagramInterchange::NodeView {
                            expose LightLogic::AmberOn;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 1141;
                                attribute :>> y = 126;
                                attribute :>> height = 86;
                                attribute :>> width = 204;
                            }
                        }
                        view startView4 : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 147;
                                attribute :>> y = 68;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView : DiagramInterchange::EdgeView {
                            expose LightLogic::__unnamed61;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                attribute :>> xt = 133;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 140;
                                attribute :>> wayPoints = ((0, 58), -1134, 0);
                            }
                            ref view :>> sourceView ::> AmberOnView;
                            ref view :>> targetView ::> RedOnView;
                        }
                        view unnamedView6 : DiagramInterchange::EdgeView {
                            expose LightLogic::__unnamed62;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 95;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 32;
                            }
                            ref view :>> sourceView ::> GreenOnView1;
                            ref view :>> targetView ::> GreenBlinkView;
                        }
                        view unnamedView8 : DiagramInterchange::EdgeView {
                            expose LightLogic::__unnamed63;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 63;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 121;
                            }
                            ref view :>> sourceView ::> GreenBlinkView;
                            ref view :>> targetView ::> AmberOnView;
                        }
                        view unnamedView11 : DiagramInterchange::EdgeView {
                            expose LightLogic::__unnamed64;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 142;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 9;
                            }
                            ref view :>> sourceView ::> startView4;
                            ref view :>> targetView ::> RedOnView;
                        }
                        view unnamedView14 : DiagramInterchange::EdgeView {
                            expose LightLogic::__unnamed65;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 39;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 48;
                            }
                            ref view :>> sourceView ::> RedOnView;
                            ref view :>> targetView ::> GreenOnView1;
                        }
                    }
                    view ctrlPortRedView : DiagramInterchange::NodeView {
                        expose Controller::ctrlPortRed;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 98;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view ctrlPortAmberView : DiagramInterchange::NodeView {
                        expose Controller::ctrlPortAmber;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 154;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view ctrlPortGreenView : DiagramInterchange::NodeView {
                        expose Controller::ctrlPortGreen;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 208;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view unnamedView1 : DiagramInterchange::EdgeView {
                        expose TrafficLightSystem::__unnamed66;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::left;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = ((-75, 0), 0, -455);
                        }
                        ref view :>> sourceView ::> ctrlPortAmberView;
                        ref view :>> targetView ::> signalHeadView::headPortAmberView;
                    }
                    view unnamedView4 : DiagramInterchange::EdgeView {
                        expose TrafficLightSystem::__unnamed67;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::left;
                            attribute :>> ys = 6;
                            attribute :>> wayPoints = ((-113, 0), 0, -536);
                        }
                        ref view :>> sourceView ::> ctrlPortGreenView;
                        ref view :>> targetView ::> signalHeadView::headPortGreenView;
                    }
                    view unnamedView12 : DiagramInterchange::EdgeView {
                        expose TrafficLightSystem::__unnamed68;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::left;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = ((-30, 0), 0, -371);
                        }
                        ref view :>> sourceView ::> ctrlPortRedView;
                        ref view :>> targetView ::> signalHeadView::headPortRedView;
                    }
                }
            }
        }
    }
    package PortDefs {
        port def HeadPort {
            in item onSignal : Payloads::On;
            in item offSignal : Payloads::Off;
        }
        port def LampPort {
            in item onSignal : Payloads::On;
            in item offSignal : Payloads::Off;
        }
        port def CtrlPort {
            out item onSignal : Payloads::On;
            out item offSignal : Payloads::Off;
        }
    }
    package Payloads {
        item def On;
        item def Off;
    }
    package SimConfig {
        item TrafficLight {
            @DS_Simulation::SimulationMetadata::'sim config' {
                executionTarget = TrafficLightSystemPkg::TrafficLightDomain::TrafficLightSystem;
                autoStart = true;
                startupView = TrafficLightSystemPkg::TrafficLightDomain::TrafficLightSystemView;
            }
        }
        item TrafficLight_WithUI {
            @DS_Simulation::SimulationMetadata::'sim config' {
                executionTarget = TrafficLightSystemPkg::TrafficLightDomain::TrafficLightSystem;
                autoStart = true;
                UI = 'HTML UI'::TrafficLightUI_HTML;
                startupView = TrafficLightSystemPkg::TrafficLightDomain::TrafficLightSystemView;
            }
        }
        view SimConfigView : StandardViewDefinitions::gv {
            render Views::asInterconnectionDiagram;
            view TrafficLightView : DiagramInterchange::NodeView {
                expose TrafficLight;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 7;
                    attribute :>> y = 28;
                    attribute :>> height = 126;
                    attribute :>> width = 209;
                }
            }
            view TrafficLight_WithUIView : DiagramInterchange::NodeView {
                expose TrafficLight_WithUI;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 7;
                    attribute :>> y = 182;
                    attribute :>> height = 140;
                    attribute :>> width = 209;
                }
            }
        }
    }
}

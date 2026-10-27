package ElectricVehicleModel {
    part electricVehicle {
        part batteryPack {
            port powerPortA;
        }
        part engine {
            port powerPortB;
        }
        interface powerSupply connect batteryPack.powerPortA to engine.powerPortB;
    }
    view example : StandardViewDefinitions::gv {
        render Views::asInterconnectionDiagram;
        view electricVehicleView : DiagramInterchange::NodeView {
            expose electricVehicle;
            render DiagramInterchange::asRegularNode {
                attribute :>> x = 0;
                attribute :>> y = 0;
                attribute :>> height = 539;
                attribute :>> width = 273;
            }
            view batteryPackView : DiagramInterchange::NodeView {
                expose electricVehicle::batteryPack;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 94;
                    attribute :>> y = 84;
                    attribute :>> height = 98;
                    attribute :>> width = 154;
                }
                view powerPortAView : DiagramInterchange::NodeView {
                    expose batteryPack::powerPortA;
                    render DiagramInterchange::asBorderNode {
                        attribute :>> onBorder = onBorder::bottom;
                        attribute :>> xc = 88;
                        attribute :>> height = 15;
                        attribute :>> width = 65;
                    }
                }
                view powerSupplyView : DiagramInterchange::EdgeView {
                    expose electricVehicle::powerSupply;
                    render DiagramInterchange::asEdge {
                        attribute :>> targetOnBorder = sourceOnBorder::top;
                        attribute :>> xt = 43;
                        attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                        attribute :>> xs = 43;
                    }
                    ref view :>> sourceView ::> powerPortAView;
                    ref view :>> targetView ::> engineView::powerPortBView;
                }
            }
            view engineView : DiagramInterchange::NodeView {
                expose electricVehicle::engine;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 94;
                    attribute :>> y = 350;
                    attribute :>> height = 98;
                    attribute :>> width = 154;
                }
                view powerPortBView : DiagramInterchange::NodeView {
                    expose engine::powerPortB;
                    render DiagramInterchange::asBorderNode {
                        attribute :>> onBorder = onBorder::top;
                        attribute :>> xc = 88;
                        attribute :>> height = 15;
                        attribute :>> width = 65;
                    }
                }
            }
        }
    }
}

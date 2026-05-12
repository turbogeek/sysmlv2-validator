            @Rationale about engineTradeOffAnalysis::vehicle_b_engine4cyl{
                explanation = VehicleAnalysis::VehicleTradeOffAnalysis::engineTradeOffAnalysis;          
                text = "the engine4cyl was evaluated to have a higher objective function compared to the engine6cyl based on the trade-off analyiss"; 
            }
            
            // The following risk for the engine4cyl could have been included as part of the objective evaluaiton criteria
            
            @Risk about engineTradeOffAnalysis::vehicle_b_engine4cyl {
                totalRisk = medium;
                technicalRisk = medium;
                scheduleRisk = medium;
                costRisk = RiskLevelEnum::low;
            }
            @Risk about engineTradeOffAnalysis::vehicle_b_engine4cyl::engine::fuelEfficiency {
                technicalRisk {
                    probability = 0.3;
                    impact = 0.5;
                }
            }
            
                
            public import TradeStudies::*;
            //evaluation function with criterion engine mass, engine power, and engine cost
            calc def EngineEvaluation {
                in engineMass:>ISQ::mass;

package VehiclePerceptionSystem {
    private import ScalarValues::*;
    private import ISQ::*;
    item def TargetDetectionPayload {
        attribute frameId : Integer;
        attribute timestampMs : Real;
        attribute position : Real;
        attribute velocity : Real;
        attribute confidence : Real;
    }
    item def SensorFaultSignal;
    item def EnableRadarCommand;
    port def RadarDataOutputPort {
        out item detectionStream : TargetDetectionPayload;
    }
    port def PerceptionDataInputPort {
        in item detectionStream : TargetDetectionPayload;
    }
    part def RadarSensor {
        port dataOut : RadarDataOutputPort;
    }
    part def AdasPerception {
        port dataIn : PerceptionDataInputPort;
        exhibit state PerceptionTrackerStateMachine {
            state Standby;
            state TrackingActive {
                do action executeKalmanTrackFilter {
                    attribute trackCount : Integer := 0;
                    attribute maxTrackCapacity : Integer := 64;
                    attribute filterGain : Real := 0.85;
                    attribute isCovarianceDivergent : Boolean := false;
                    first initializeMatrix;
                    succession __unnamed95 first initializeMatrix then updateTrackState;
                    action initializeMatrix {
                        out attribute initTrackCount : Integer;
                        out attribute initDivergent : Boolean;
                        action counterInitialization {
                            out attribute initialCount : Integer;
                            out attribute initialDivFlag : Boolean;
                            assign initialCount := 0;
                            assign initialDivFlag := false;
                            assign trackCount := initialCount;
                            assign isCovarianceDivergent := initialDivFlag;
                            action wait {
                                assert constraint {
                                    Time::DurationOf(that) == 1
                                }
                            }
                        }
                        bind counterInitialization.initialCount = initTrackCount;
                        bind counterInitialization.initialDivFlag = initDivergent;
                    }
                    action updateTrackState {
                        in attribute currentCount : Integer;
                        in attribute currentDivergent : Boolean;
                        out attribute finalCount : Integer;
                        out attribute finalDivergent : Boolean;
                        assert constraint {
                            Time::DurationOf(that) == 1
                        }
                        action seedState {
                            in attribute inputCount : Integer;
                            in attribute crntDiv : Boolean;
                            assign trackCount := inputCount;
                            assign isCovarianceDivergent := crntDiv;
                            assign incrementCount.newCount := 0;
                        }
                        merge loopStart;
                        succession __unnamed76 first start then seedState;
                        decide checkCapacity;
                        // "Feature Membership -> Succession" is incompatible with textual.
                        succession __unnamed78 first loopStart then checkCapacity;
                        decide checkDivergence;
                        succession __unnamed75 first incrementCount then checkDivergence;
                        succession __unnamed80 first checkDivergence if trackCount > 50 then markDivergence;
                        merge endIf;
                        succession __unnamed87 first checkCapacity if trackCount >= maxTrackCapacity then endIf;
                        succession __unnamed81 first endIf then done;
                        succession __unnamed83 first checkDivergence if trackCount <= 50 then loopStart;
                        succession __unnamed79 first markDivergence then endIf;
                        action incrementCount {
                            in attribute inputCount : Integer;
                            out attribute newCount : Integer;
                            assign newCount := newCount + 1;
                            assign trackCount := newCount;
                            action wait {
                                assert constraint {
                                    Time::DurationOf(that) == 1
                                }
                            }
                        }
                        action markDivergence {
                            out attribute flagState : Boolean;
                            assign flagState := true;
                            assign isCovarianceDivergent := flagState;
                        }
                        binding __unnamed73 bind incrementCount.newCount = finalCount;
                        succession __unnamed82 first seedState then loopStart;
                        binding __unnamed77 bind currentDivergent = seedState.crntDiv;
                        binding __unnamed85 bind currentCount = seedState.inputCount;
                        binding __unnamed74 bind markDivergence.flagState = finalDivergent;
                    }
                    action checkFilterDivergence {
                        in attribute divergenceStatus : Boolean;
                        decide __unnamed69;
                        // "Feature Membership -> Succession" is incompatible with textual.
                        // "Feature Membership -> Succession" is incompatible with textual.
                        action setNominalGain assign filterGain := 0.85;
                        action setLowGain assign filterGain := 0.1;
                        succession __unnamed91 first start then __unnamed69;
                        merge __unnamed70;
                        succession __unnamed89 first setLowGain then __unnamed70;
                        succession __unnamed90 first setNominalGain then __unnamed70;
                        succession __unnamed93 first __unnamed70 then done;
                    }
                    succession __unnamed96 first start then initializeMatrix;
                    succession __unnamed94 first updateTrackState then checkFilterDivergence;
                    succession flow __unnamed71 from initializeMatrix.initTrackCount to updateTrackState.currentCount;
                    succession flow __unnamed72 from initializeMatrix.initDivergent to updateTrackState.currentDivergent;
                    succession flow __unnamed86 from updateTrackState.finalDivergent to checkFilterDivergence.divergenceStatus;
                }
            }
            state FaultRecovery;
            transition __unnamed98 first start then Standby;
            transition __unnamed100 first Standby accept TargetDetectionPayload then TrackingActive;
            transition __unnamed99 first TrackingActive accept SensorFaultSignal then FaultRecovery;
            transition __unnamed97 first TrackingActive then Standby;
        }
    }
    item VehiclePerceptionConfig {
        @DS_Simulation::SimulationMetadata::'sim config' {
            executionTarget = AutonomousVehicleSystem;
            autoStart := true;
            speed := 1;
            startupView = AutonomousVehicleSystem.AutonomousVehicleSystemView;
        }
    }
    part AutonomousVehicleSystem {
        part frontRadar : RadarSensor {
            exhibit state RadarSensorStateMachine {
                state ActiveTransmitting {
                    entry action sendDetectionStream {
                        action emitDetection {
                            out attribute frameId : Integer;
                            out attribute timestampMs : Real;
                            out attribute position : Real;
                            out attribute velocity : Real;
                            out attribute confidence : Real;
                            assign frameId := 101;
                            assign timestampMs := 1000.0;
                            assign position := 45.2;
                            assign velocity := 12.5;
                            assign confidence := 0.98;
                        }
                        action constructPayload {
                            in attribute fId : Integer;
                            in attribute tMs : Real;
                            in attribute pos : Real;
                            in attribute vel : Real;
                            in attribute conf : Real;
                            action transmitPayload send new TargetDetectionPayload(frameId = fId, timestampMs = tMs, position = pos, velocity = vel,
                                                                                   confidence = conf) via dataOut;
                        }
                        succession flow __unnamed105 from emitDetection.velocity to constructPayload.vel;
                        succession flow __unnamed102 from emitDetection.confidence to constructPayload.conf;
                        succession flow __unnamed104 from emitDetection.frameId to constructPayload.fId;
                        succession flow __unnamed103 from emitDetection.position to constructPayload.pos;
                        succession flow __unnamed101 from emitDetection.timestampMs to constructPayload.tMs;
                        succession __unnamed106 first start then emitDetection;
                    }
                }
                state Idle;
                transition __unnamed109 first start then Idle;
                transition __unnamed107 first Idle accept EnableRadarCommand then ActiveTransmitting;
                transition __unnamed108 first ActiveTransmitting then Idle;
            }
        }
        part perceptionUnit : AdasPerception;
        flow __unnamed110 of TargetDetectionPayload from frontRadar.dataOut.detectionStream to perceptionUnit.dataIn.detectionStream;
        view AutonomousVehicleSystemView : StandardViewDefinitions::gv {
            render Views::asInterconnectionDiagram;
            view AutonomousVehicleSystemView : DiagramInterchange::NodeView {
                expose AutonomousVehicleSystem;
                render DiagramInterchange::asRegularNode {
                    attribute :>> x = 5;
                    attribute :>> y = 5;
                    attribute :>> height = 858;
                    attribute :>> width = 2762;
                }
                view perceptionUnitView : DiagramInterchange::NodeView {
                    expose perceptionUnit;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 730;
                        attribute :>> y = 79;
                        attribute :>> height = 759;
                        attribute :>> width = 2022;
                    }
                    view PerceptionTrackerStateMachineView : DiagramInterchange::NodeView {
                        expose AdasPerception::PerceptionTrackerStateMachine;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 21;
                            attribute :>> y = 70;
                            attribute :>> height = 669;
                            attribute :>> width = 1981;
                        }
                        view FaultRecoveryView : DiagramInterchange::NodeView {
                            expose PerceptionTrackerStateMachine::FaultRecovery;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 1876;
                                attribute :>> y = 378;
                                attribute :>> height = 39;
                                attribute :>> width = 85;
                            }
                        }
                        view TrackingActiveView : DiagramInterchange::NodeView {
                            expose PerceptionTrackerStateMachine::TrackingActive;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 98;
                                attribute :>> y = 98;
                                attribute :>> height = 551;
                                attribute :>> width = 1653;
                            }
                            view executeKalmanTrackFilterView : DiagramInterchange::NodeView {
                                expose TrackingActive::executeKalmanTrackFilter;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 105;
                                    attribute :>> height = 426;
                                    attribute :>> width = 1612;
                                }
                                view initializeMatrixView : DiagramInterchange::NodeView {
                                    expose executeKalmanTrackFilter::initializeMatrix;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 20;
                                        attribute :>> y = 161;
                                        attribute :>> height = 120;
                                        attribute :>> width = 89;
                                    }
                                    view initTrackCountView : DiagramInterchange::NodeView {
                                        expose initializeMatrix::initTrackCount;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::right;
                                            attribute :>> yc = 98;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view initDivergentView : DiagramInterchange::NodeView {
                                        expose initializeMatrix::initDivergent;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::right;
                                            attribute :>> yc = 13;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view unnamedView29 : DiagramInterchange::EdgeView {
                                        expose executeKalmanTrackFilter::__unnamed71;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 7;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 7;
                                            attribute :>> wayPoints = ((13, 0), 0, 84);
                                        }
                                        ref view :>> sourceView ::> initTrackCountView;
                                        ref view :>> targetView ::> updateTrackStateView::currentCountView;
                                    }
                                    view unnamedView34 : DiagramInterchange::EdgeView {
                                        expose executeKalmanTrackFilter::__unnamed72;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 7;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 8;
                                            attribute :>> wayPoints = ((72, 0), 0, -35);
                                        }
                                        ref view :>> sourceView ::> initDivergentView;
                                        ref view :>> targetView ::> updateTrackStateView::currentDivergentView;
                                    }
                                }
                                view updateTrackStateView : DiagramInterchange::NodeView {
                                    expose executeKalmanTrackFilter::updateTrackState;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 231;
                                        attribute :>> y = 91;
                                        attribute :>> height = 315;
                                        attribute :>> width = 805;
                                    }
                                    view loopStartView : DiagramInterchange::NodeView {
                                        expose updateTrackState::loopStart;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 161;
                                            attribute :>> y = 161;
                                            attribute :>> height = 12;
                                            attribute :>> width = 24;
                                        }
                                    }
                                    view checkDivergenceView : DiagramInterchange::NodeView {
                                        expose updateTrackState::checkDivergence;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 517;
                                            attribute :>> y = 154;
                                            attribute :>> height = 12;
                                            attribute :>> width = 24;
                                        }
                                    }
                                    view checkCapacityView : DiagramInterchange::NodeView {
                                        expose updateTrackState::checkCapacity;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 202;
                                            attribute :>> y = 161;
                                            attribute :>> height = 12;
                                            attribute :>> width = 24;
                                        }
                                    }
                                    view endIfView : DiagramInterchange::NodeView {
                                        expose updateTrackState::endIf;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 699;
                                            attribute :>> y = 238;
                                            attribute :>> height = 12;
                                            attribute :>> width = 24;
                                        }
                                    }
                                    view doneView : DiagramInterchange::NodeView {
                                        expose Actions::Action::done;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 699;
                                            attribute :>> y = 273;
                                            attribute :>> height = 18;
                                            attribute :>> width = 18;
                                        }
                                    }
                                    view startView : DiagramInterchange::NodeView {
                                        expose Actions::Action::start;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 90;
                                            attribute :>> y = 56;
                                            attribute :>> height = 18;
                                            attribute :>> width = 18;
                                        }
                                    }
                                    view incrementCountView : DiagramInterchange::NodeView {
                                        expose updateTrackState::incrementCount;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 405;
                                            attribute :>> y = 126;
                                            attribute :>> height = 80;
                                            attribute :>> width = 93;
                                        }
                                        view newCountView : DiagramInterchange::NodeView {
                                            expose incrementCount::newCount;
                                            render DiagramInterchange::asBorderNode {
                                                attribute :>> onBorder = onBorder::top;
                                                attribute :>> xc = 22;
                                                attribute :>> height = 15;
                                                attribute :>> width = 15;
                                            }
                                        }
                                        view unnamedView40 : DiagramInterchange::EdgeView {
                                            expose updateTrackState::__unnamed73;
                                            render DiagramInterchange::asEdge {
                                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                                attribute :>> yt = 8;
                                                attribute :>> sourceOnBorder = sourceOnBorder::top;
                                                attribute :>> xs = 7;
                                                attribute :>> wayPoints = (0, -58);
                                            }
                                            ref view :>> sourceView ::> newCountView;
                                            ref view :>> targetView ::> finalCountView;
                                        }
                                    }
                                    view markDivergenceView : DiagramInterchange::NodeView {
                                        expose updateTrackState::markDivergence;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 636;
                                            attribute :>> y = 126;
                                            attribute :>> height = 80;
                                            attribute :>> width = 95;
                                        }
                                        view flagStateView : DiagramInterchange::NodeView {
                                            expose markDivergence::flagState;
                                            render DiagramInterchange::asBorderNode {
                                                attribute :>> onBorder = onBorder::right;
                                                attribute :>> yc = 35;
                                                attribute :>> height = 15;
                                                attribute :>> width = 15;
                                            }
                                        }
                                        view unnamedView32 : DiagramInterchange::EdgeView {
                                            expose updateTrackState::__unnamed74;
                                            render DiagramInterchange::asEdge {
                                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                                attribute :>> yt = 7;
                                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                                attribute :>> ys = 7;
                                                attribute :>> wayPoints = ((17, 0), 0, -56);
                                            }
                                            ref view :>> sourceView ::> flagStateView;
                                            ref view :>> targetView ::> finalDivergentView;
                                        }
                                    }
                                    view finalCountView : DiagramInterchange::NodeView {
                                        expose updateTrackState::finalCount;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::right;
                                            attribute :>> yc = 52;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view finalDivergentView : DiagramInterchange::NodeView {
                                        expose updateTrackState::finalDivergent;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::right;
                                            attribute :>> yc = 105;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view currentDivergentView : DiagramInterchange::NodeView {
                                        expose updateTrackState::currentDivergent;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::left;
                                            attribute :>> yc = 49;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view currentCountView : DiagramInterchange::NodeView {
                                        expose updateTrackState::currentCount;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::left;
                                            attribute :>> yc = 252;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view seedStateView : DiagramInterchange::NodeView {
                                        expose updateTrackState::seedState;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 77;
                                            attribute :>> y = 105;
                                            attribute :>> height = 120;
                                            attribute :>> width = 64;
                                        }
                                        view crntDivView : DiagramInterchange::NodeView {
                                            expose seedState::crntDiv;
                                            render DiagramInterchange::asBorderNode {
                                                attribute :>> onBorder = onBorder::left;
                                                attribute :>> yc = 21;
                                                attribute :>> height = 15;
                                                attribute :>> width = 15;
                                            }
                                        }
                                        view inputCountView : DiagramInterchange::NodeView {
                                            expose seedState::inputCount;
                                            render DiagramInterchange::asBorderNode {
                                                attribute :>> onBorder = onBorder::left;
                                                attribute :>> yc = 84;
                                                attribute :>> height = 15;
                                                attribute :>> width = 15;
                                            }
                                        }
                                    }
                                    view unnamedView6 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed75;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 6;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 34;
                                        }
                                        ref view :>> sourceView ::> incrementCountView;
                                        ref view :>> targetView ::> checkDivergenceView;
                                    }
                                    view unnamedView7 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed76;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::top;
                                            attribute :>> xt = 22;
                                            attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                            attribute :>> xs = 9;
                                        }
                                        ref view :>> sourceView ::> startView;
                                        ref view :>> targetView ::> seedStateView;
                                    }
                                    view unnamedView8 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed77;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 7;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 7;
                                            attribute :>> wayPoints = ((18, 0), 0, 77);
                                        }
                                        ref view :>> sourceView ::> currentDivergentView;
                                        ref view :>> targetView ::> seedStateView::crntDivView;
                                    }
                                    view unnamedView9 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed78;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 6;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 6;
                                        }
                                        ref view :>> sourceView ::> loopStartView;
                                        ref view :>> targetView ::> checkCapacityView;
                                    }
                                    view unnamedView10 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed79;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::top;
                                            attribute :>> xt = 12;
                                            attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                            attribute :>> xs = 75;
                                        }
                                        ref view :>> sourceView ::> markDivergenceView;
                                        ref view :>> targetView ::> endIfView;
                                    }
                                    view unnamedView11 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed80;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 34;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 6;
                                        }
                                        ref view :>> sourceView ::> checkDivergenceView;
                                        ref view :>> targetView ::> markDivergenceView;
                                    }
                                    view unnamedView16 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed81;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::top;
                                            attribute :>> xt = 9;
                                            attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                            attribute :>> xs = 9;
                                        }
                                        ref view :>> sourceView ::> endIfView;
                                        ref view :>> targetView ::> doneView;
                                    }
                                    view unnamedView17 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed82;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 6;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 62;
                                        }
                                        ref view :>> sourceView ::> seedStateView;
                                        ref view :>> targetView ::> loopStartView;
                                    }
                                    view unnamedView20 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed83;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::top;
                                            attribute :>> xt = 12;
                                            attribute :>> sourceOnBorder = sourceOnBorder::top;
                                            attribute :>> xs = 12;
                                            attribute :>> wayPoints = ((0, -73), -356, 0);
                                        }
                                        ref view :>> sourceView ::> checkDivergenceView;
                                        ref view :>> targetView ::> loopStartView;
                                    }
                                    view unnamedView24 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed84;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 41;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 6;
                                        }
                                        ref view :>> sourceView ::> checkCapacityView;
                                        ref view :>> targetView ::> incrementCountView;
                                    }
                                    view unnamedView25 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed85;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 7;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 7;
                                            attribute :>> wayPoints = ((31, 0), 0, -63);
                                        }
                                        ref view :>> sourceView ::> currentCountView;
                                        ref view :>> targetView ::> seedStateView::inputCountView;
                                    }
                                    view unnamedView31 : DiagramInterchange::EdgeView {
                                        expose executeKalmanTrackFilter::__unnamed86;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 7;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 7;
                                            attribute :>> wayPoints = ((20, 0), 0, 56);
                                        }
                                        ref view :>> sourceView ::> finalDivergentView;
                                        ref view :>> targetView ::> checkFilterDivergenceView::divergenceStatusView;
                                    }
                                    view unnamedView38 : DiagramInterchange::EdgeView {
                                        expose updateTrackState::__unnamed87;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 6;
                                            attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                            attribute :>> xs = 12;
                                            attribute :>> wayPoints = (0, 71);
                                        }
                                        ref view :>> sourceView ::> checkCapacityView;
                                        ref view :>> targetView ::> endIfView;
                                    }
                                }
                                view checkFilterDivergenceView : DiagramInterchange::NodeView {
                                    expose executeKalmanTrackFilter::checkFilterDivergence;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 1196;
                                        attribute :>> y = 119;
                                        attribute :>> height = 235;
                                        attribute :>> width = 396;
                                    }
                                    view unnamedView : DiagramInterchange::NodeView {
                                        expose checkFilterDivergence::__unnamed69;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 15;
                                            attribute :>> y = 105;
                                            attribute :>> height = 12;
                                            attribute :>> width = 24;
                                        }
                                    }
                                    view setNominalGainView : DiagramInterchange::NodeView {
                                        expose checkFilterDivergence::setNominalGain;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 175;
                                            attribute :>> y = 161;
                                            attribute :>> height = 54;
                                            attribute :>> width = 96;
                                        }
                                    }
                                    view setLowGainView : DiagramInterchange::NodeView {
                                        expose checkFilterDivergence::setLowGain;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 175;
                                            attribute :>> y = 84;
                                            attribute :>> height = 54;
                                            attribute :>> width = 90;
                                        }
                                    }
                                    view unnamedView1 : DiagramInterchange::NodeView {
                                        expose checkFilterDivergence::__unnamed70;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 309;
                                            attribute :>> y = 147;
                                            attribute :>> height = 12;
                                            attribute :>> width = 24;
                                        }
                                    }
                                    view doneView1 : DiagramInterchange::NodeView {
                                        expose Actions::Action::done;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 358;
                                            attribute :>> y = 140;
                                            attribute :>> height = 18;
                                            attribute :>> width = 18;
                                        }
                                    }
                                    view startView1 : DiagramInterchange::NodeView {
                                        expose Actions::Action::start;
                                        render DiagramInterchange::asRegularNode {
                                            attribute :>> x = 15;
                                            attribute :>> y = 70;
                                            attribute :>> height = 18;
                                            attribute :>> width = 18;
                                        }
                                    }
                                    view divergenceStatusView : DiagramInterchange::NodeView {
                                        expose checkFilterDivergence::divergenceStatus;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::left;
                                            attribute :>> yc = 133;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view unnamedView3 : DiagramInterchange::EdgeView {
                                        expose checkFilterDivergence::__unnamed88;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 27;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 6;
                                        }
                                        ref view :>> sourceView ::> unnamedView;
                                        ref view :>> targetView ::> setLowGainView;
                                    }
                                    view unnamedView4 : DiagramInterchange::EdgeView {
                                        expose checkFilterDivergence::__unnamed89;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::top;
                                            attribute :>> xt = 12;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 28;
                                            attribute :>> wayPoints = (56, 0);
                                        }
                                        ref view :>> sourceView ::> setLowGainView;
                                        ref view :>> targetView ::> unnamedView1;
                                    }
                                    view unnamedView12 : DiagramInterchange::EdgeView {
                                        expose checkFilterDivergence::__unnamed90;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::bottom;
                                            attribute :>> xt = 12;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 28;
                                            attribute :>> wayPoints = (50, 0);
                                        }
                                        ref view :>> sourceView ::> setNominalGainView;
                                        ref view :>> targetView ::> unnamedView1;
                                    }
                                    view unnamedView13 : DiagramInterchange::EdgeView {
                                        expose checkFilterDivergence::__unnamed91;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::top;
                                            attribute :>> xt = 12;
                                            attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                            attribute :>> xs = 12;
                                        }
                                        ref view :>> sourceView ::> startView1;
                                        ref view :>> targetView ::> unnamedView;
                                    }
                                    view unnamedView33 : DiagramInterchange::EdgeView {
                                        expose checkFilterDivergence::__unnamed92;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 28;
                                            attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                            attribute :>> xs = 12;
                                            attribute :>> wayPoints = (0, 72);
                                        }
                                        ref view :>> sourceView ::> unnamedView;
                                        ref view :>> targetView ::> setNominalGainView;
                                    }
                                    view unnamedView41 : DiagramInterchange::EdgeView {
                                        expose checkFilterDivergence::__unnamed93;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 9;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 6;
                                            attribute :>> wayPoints = ((11, 0), 0, -4);
                                        }
                                        ref view :>> sourceView ::> unnamedView1;
                                        ref view :>> targetView ::> doneView1;
                                    }
                                }
                                view startView2 : DiagramInterchange::NodeView {
                                    expose Actions::Action::start;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 50;
                                        attribute :>> y = 105;
                                        attribute :>> height = 18;
                                        attribute :>> width = 18;
                                    }
                                }
                                view unnamedView14 : DiagramInterchange::EdgeView {
                                    expose executeKalmanTrackFilter::__unnamed94;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 196;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 224;
                                    }
                                    ref view :>> sourceView ::> updateTrackStateView;
                                    ref view :>> targetView ::> checkFilterDivergenceView;
                                }
                                view unnamedView35 : DiagramInterchange::EdgeView {
                                    expose executeKalmanTrackFilter::__unnamed95;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 130;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 60;
                                    }
                                    ref view :>> sourceView ::> initializeMatrixView;
                                    ref view :>> targetView ::> updateTrackStateView;
                                }
                                view unnamedView36 : DiagramInterchange::EdgeView {
                                    expose executeKalmanTrackFilter::__unnamed96;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::top;
                                        attribute :>> xt = 39;
                                        attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                        attribute :>> xs = 9;
                                    }
                                    ref view :>> sourceView ::> startView2;
                                    ref view :>> targetView ::> initializeMatrixView;
                                }
                            }
                        }
                        view StandbyView : DiagramInterchange::NodeView {
                            expose PerceptionTrackerStateMachine::Standby;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 14;
                                attribute :>> y = 105;
                                attribute :>> height = 39;
                                attribute :>> width = 54;
                            }
                        }
                        view startView3 : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 34;
                                attribute :>> y = 63;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view unnamedView22 : DiagramInterchange::EdgeView {
                            expose PerceptionTrackerStateMachine::__unnamed97;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::right;
                                attribute :>> yt = 25;
                                attribute :>> sourceOnBorder = sourceOnBorder::left;
                                attribute :>> ys = 32;
                            }
                            ref view :>> sourceView ::> TrackingActiveView;
                            ref view :>> targetView ::> StandbyView;
                        }
                        view unnamedView26 : DiagramInterchange::EdgeView {
                            expose PerceptionTrackerStateMachine::__unnamed98;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 29;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 9;
                            }
                            ref view :>> sourceView ::> startView3;
                            ref view :>> targetView ::> StandbyView;
                        }
                        view unnamedView27 : DiagramInterchange::EdgeView {
                            expose PerceptionTrackerStateMachine::__unnamed99;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 18;
                                attribute :>> sourceOnBorder = sourceOnBorder::right;
                                attribute :>> ys = 298;
                            }
                            ref view :>> sourceView ::> TrackingActiveView;
                            ref view :>> targetView ::> FaultRecoveryView;
                        }
                        view unnamedView37 : DiagramInterchange::EdgeView {
                            expose PerceptionTrackerStateMachine::__unnamed100;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 249;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 28;
                                attribute :>> wayPoints = (0, 203);
                            }
                            ref view :>> sourceView ::> StandbyView;
                            ref view :>> targetView ::> TrackingActiveView;
                        }
                    }
                    view dataInView : DiagramInterchange::NodeView {
                        expose AdasPerception::dataIn;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::left;
                            attribute :>> yc = 357;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                }
                view frontRadarView : DiagramInterchange::NodeView {
                    expose frontRadar;
                    render DiagramInterchange::asRegularNode {
                        attribute :>> x = 44;
                        attribute :>> y = 79;
                        attribute :>> height = 709;
                        attribute :>> width = 603;
                    }
                    view RadarSensorStateMachineView : DiagramInterchange::NodeView {
                        expose frontRadar::RadarSensorStateMachine;
                        render DiagramInterchange::asRegularNode {
                            attribute :>> x = 21;
                            attribute :>> y = 70;
                            attribute :>> height = 619;
                            attribute :>> width = 572;
                        }
                        view ActiveTransmittingView : DiagramInterchange::NodeView {
                            expose RadarSensorStateMachine::ActiveTransmitting;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 49;
                                attribute :>> y = 168;
                                attribute :>> height = 431;
                                attribute :>> width = 513;
                            }
                            view sendDetectionStreamView : DiagramInterchange::NodeView {
                                expose ActiveTransmitting::sendDetectionStream;
                                render DiagramInterchange::asRegularNode {
                                    attribute :>> x = 21;
                                    attribute :>> y = 105;
                                    attribute :>> height = 299;
                                    attribute :>> width = 482;
                                }
                                view constructPayloadView : DiagramInterchange::NodeView {
                                    expose sendDetectionStream::constructPayload;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 371;
                                        attribute :>> y = 63;
                                        attribute :>> height = 200;
                                        attribute :>> width = 101;
                                    }
                                    view posView : DiagramInterchange::NodeView {
                                        expose constructPayload::pos;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::left;
                                            attribute :>> yc = 187;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view fIdView : DiagramInterchange::NodeView {
                                        expose constructPayload::fId;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::left;
                                            attribute :>> yc = 147;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view confView : DiagramInterchange::NodeView {
                                        expose constructPayload::conf;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::left;
                                            attribute :>> yc = 103;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view velView : DiagramInterchange::NodeView {
                                        expose constructPayload::vel;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::left;
                                            attribute :>> yc = 59;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view tMsView : DiagramInterchange::NodeView {
                                        expose constructPayload::tMs;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::left;
                                            attribute :>> yc = 15;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                }
                                view emitDetectionView : DiagramInterchange::NodeView {
                                    expose sendDetectionStream::emitDetection;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 49;
                                        attribute :>> y = 63;
                                        attribute :>> height = 200;
                                        attribute :>> width = 84;
                                    }
                                    view frameIdView : DiagramInterchange::NodeView {
                                        expose emitDetection::frameId;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::right;
                                            attribute :>> yc = 147;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view timestampMsView : DiagramInterchange::NodeView {
                                        expose emitDetection::timestampMs;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::right;
                                            attribute :>> yc = 15;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view confidenceView : DiagramInterchange::NodeView {
                                        expose emitDetection::confidence;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::right;
                                            attribute :>> yc = 103;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view velocityView : DiagramInterchange::NodeView {
                                        expose emitDetection::velocity;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::right;
                                            attribute :>> yc = 59;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view positionView : DiagramInterchange::NodeView {
                                        expose emitDetection::position;
                                        render DiagramInterchange::asBorderNode {
                                            attribute :>> onBorder = onBorder::right;
                                            attribute :>> yc = 187;
                                            attribute :>> height = 15;
                                            attribute :>> width = 15;
                                        }
                                    }
                                    view unnamedView15 : DiagramInterchange::EdgeView {
                                        expose sendDetectionStream::__unnamed101;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 7;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 7;
                                        }
                                        ref view :>> sourceView ::> timestampMsView;
                                        ref view :>> targetView ::> constructPayloadView::tMsView;
                                    }
                                    view unnamedView18 : DiagramInterchange::EdgeView {
                                        expose sendDetectionStream::__unnamed102;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 7;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 7;
                                        }
                                        ref view :>> sourceView ::> confidenceView;
                                        ref view :>> targetView ::> constructPayloadView::confView;
                                    }
                                    view unnamedView28 : DiagramInterchange::EdgeView {
                                        expose sendDetectionStream::__unnamed103;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 11;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 11;
                                        }
                                        ref view :>> sourceView ::> positionView;
                                        ref view :>> targetView ::> constructPayloadView::posView;
                                    }
                                    view unnamedView30 : DiagramInterchange::EdgeView {
                                        expose sendDetectionStream::__unnamed104;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 7;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 7;
                                        }
                                        ref view :>> sourceView ::> frameIdView;
                                        ref view :>> targetView ::> constructPayloadView::fIdView;
                                    }
                                    view unnamedView39 : DiagramInterchange::EdgeView {
                                        expose sendDetectionStream::__unnamed105;
                                        render DiagramInterchange::asEdge {
                                            attribute :>> targetOnBorder = sourceOnBorder::left;
                                            attribute :>> yt = 7;
                                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                                            attribute :>> ys = 7;
                                        }
                                        ref view :>> sourceView ::> velocityView;
                                        ref view :>> targetView ::> constructPayloadView::velView;
                                    }
                                }
                                view startView4 : DiagramInterchange::NodeView {
                                    expose Actions::Action::start;
                                    render DiagramInterchange::asRegularNode {
                                        attribute :>> x = 14;
                                        attribute :>> y = 154;
                                        attribute :>> height = 18;
                                        attribute :>> width = 18;
                                    }
                                }
                                view unnamedView23 : DiagramInterchange::EdgeView {
                                    expose sendDetectionStream::__unnamed106;
                                    render DiagramInterchange::asEdge {
                                        attribute :>> targetOnBorder = sourceOnBorder::left;
                                        attribute :>> yt = 100;
                                        attribute :>> sourceOnBorder = sourceOnBorder::right;
                                        attribute :>> ys = 9;
                                    }
                                    ref view :>> sourceView ::> startView4;
                                    ref view :>> targetView ::> emitDetectionView;
                                }
                            }
                        }
                        view startView5 : DiagramInterchange::NodeView {
                            expose States::StateAction::start;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 274;
                                attribute :>> y = 56;
                                attribute :>> height = 18;
                                attribute :>> width = 18;
                            }
                        }
                        view IdleView : DiagramInterchange::NodeView {
                            expose RadarSensorStateMachine::Idle;
                            render DiagramInterchange::asRegularNode {
                                attribute :>> x = 260;
                                attribute :>> y = 91;
                                attribute :>> height = 39;
                                attribute :>> width = 49;
                            }
                        }
                        view unnamedView2 : DiagramInterchange::EdgeView {
                            expose RadarSensorStateMachine::__unnamed107;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 235;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 24;
                            }
                            ref view :>> sourceView ::> IdleView;
                            ref view :>> targetView ::> ActiveTransmittingView;
                        }
                        view unnamedView5 : DiagramInterchange::EdgeView {
                            expose RadarSensorStateMachine::__unnamed108;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::left;
                                attribute :>> yt = 18;
                                attribute :>> sourceOnBorder = sourceOnBorder::top;
                                attribute :>> xs = 137;
                                attribute :>> wayPoints = (0, -59);
                            }
                            ref view :>> sourceView ::> ActiveTransmittingView;
                            ref view :>> targetView ::> IdleView;
                        }
                        view unnamedView21 : DiagramInterchange::EdgeView {
                            expose RadarSensorStateMachine::__unnamed109;
                            render DiagramInterchange::asEdge {
                                attribute :>> targetOnBorder = sourceOnBorder::top;
                                attribute :>> xt = 25;
                                attribute :>> sourceOnBorder = sourceOnBorder::bottom;
                                attribute :>> xs = 9;
                                attribute :>> wayPoints = ((0, 7), 2, 0);
                            }
                            ref view :>> sourceView ::> startView5;
                            ref view :>> targetView ::> IdleView;
                        }
                    }
                    view dataOutView : DiagramInterchange::NodeView {
                        expose RadarSensor::dataOut;
                        render DiagramInterchange::asBorderNode {
                            attribute :>> onBorder = onBorder::right;
                            attribute :>> yc = 182;
                            attribute :>> height = 15;
                            attribute :>> width = 15;
                        }
                    }
                    view unnamedView19 : DiagramInterchange::EdgeView {
                        expose __unnamed110;
                        render DiagramInterchange::asEdge {
                            attribute :>> targetOnBorder = sourceOnBorder::left;
                            attribute :>> yt = 7;
                            attribute :>> sourceOnBorder = sourceOnBorder::right;
                            attribute :>> ys = 7;
                            attribute :>> wayPoints = ((20, 0), 0, 175);
                        }
                        ref view :>> sourceView ::> dataOutView;
                        ref view :>> targetView ::> perceptionUnitView::dataInView;
                    }
                }
            }
        }
    }
}

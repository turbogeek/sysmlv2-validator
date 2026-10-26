package PerfSystem_xs {
    doc /* Generated performance-test model (PerfCorpusGenerator): 2 subsystems. */
    private import ScalarValues::*;
    private import ISQ::*;
    private import SI::*;

    package Common {
        enum def Health { enum ok; enum degraded; enum failed; }
        item def Packet {
            attribute size : Integer;
            attribute priority : Integer default 0;
        }
        port def PowerPort {
            out attribute supplyVoltage : Real;
        }
        port def DataPort {
            in item packet : Packet;
        }
        interface def PowerLink {
            end source : PowerPort;
            end sink : ~PowerPort;
        }
        abstract part def Component {
            attribute mass :> ISQ::mass;
            attribute cost : Real;
            attribute health : Health default Health::ok;
            port power : ~PowerPort;
        }
        action def Process {
            in input : Packet;
            out output : Packet;
        }
        calc def TotalMass {
            in masses : MassValue[0..*];
            return total : MassValue = NumericalFunctions::sum(masses);
        }
        requirement def MassLimit {
            subject component : Component;
            attribute limit : MassValue;
            require constraint { component.mass <= limit }
        }
        state def Lifecycle {
            entry; then off;
            state off;
            state on;
            transition powerOn first off then on;
            transition powerOff first on then off;
        }
    }

    package Subsystem_0001 {
        part def Sensor_0001 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 101.0;
            port data : Common::DataPort;
        }
        part def Controller_0001 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 201.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0001[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0001 : Common::MassLimit {
            subject component : Controller_0001;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0002 {
        part def Sensor_0002 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 102.0;
            port data : Common::DataPort;
        }
        part def Controller_0002 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 202.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0002[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0002 : Common::MassLimit {
            subject component : Controller_0002;
            attribute :>> limit = 2.0 [kg];
        }
    }

    part def System {
        part controller_0001 : Subsystem_0001::Controller_0001;
        part controller_0002 : Subsystem_0002::Controller_0002;
        interface link_0001_0002 : Common::PowerLink connect
            source ::> controller_0001.supply to sink ::> controller_0002.power;
        attribute totalMass : MassValue = Common::TotalMass((controller_0001.mass, controller_0002.mass));
    }
    part system : System;
}

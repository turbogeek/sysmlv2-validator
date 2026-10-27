package PerfSystem_s {
    doc /* Generated performance-test model (PerfCorpus): 20 subsystems. */
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

    package Subsystem_0003 {
        part def Sensor_0003 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 103.0;
            port data : Common::DataPort;
        }
        part def Controller_0003 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 203.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0003[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0003 : Common::MassLimit {
            subject component : Controller_0003;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0004 {
        part def Sensor_0004 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 104.0;
            port data : Common::DataPort;
        }
        part def Controller_0004 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 204.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0004[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0004 : Common::MassLimit {
            subject component : Controller_0004;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0005 {
        part def Sensor_0005 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 105.0;
            port data : Common::DataPort;
        }
        part def Controller_0005 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 205.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0005[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0005 : Common::MassLimit {
            subject component : Controller_0005;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0006 {
        part def Sensor_0006 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 106.0;
            port data : Common::DataPort;
        }
        part def Controller_0006 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 206.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0006[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0006 : Common::MassLimit {
            subject component : Controller_0006;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0007 {
        part def Sensor_0007 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 107.0;
            port data : Common::DataPort;
        }
        part def Controller_0007 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 207.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0007[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0007 : Common::MassLimit {
            subject component : Controller_0007;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0008 {
        part def Sensor_0008 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 108.0;
            port data : Common::DataPort;
        }
        part def Controller_0008 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 208.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0008[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0008 : Common::MassLimit {
            subject component : Controller_0008;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0009 {
        part def Sensor_0009 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 109.0;
            port data : Common::DataPort;
        }
        part def Controller_0009 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 209.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0009[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0009 : Common::MassLimit {
            subject component : Controller_0009;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0010 {
        part def Sensor_0010 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 110.0;
            port data : Common::DataPort;
        }
        part def Controller_0010 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 210.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0010[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0010 : Common::MassLimit {
            subject component : Controller_0010;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0011 {
        part def Sensor_0011 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 111.0;
            port data : Common::DataPort;
        }
        part def Controller_0011 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 211.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0011[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0011 : Common::MassLimit {
            subject component : Controller_0011;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0012 {
        part def Sensor_0012 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 112.0;
            port data : Common::DataPort;
        }
        part def Controller_0012 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 212.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0012[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0012 : Common::MassLimit {
            subject component : Controller_0012;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0013 {
        part def Sensor_0013 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 113.0;
            port data : Common::DataPort;
        }
        part def Controller_0013 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 213.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0013[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0013 : Common::MassLimit {
            subject component : Controller_0013;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0014 {
        part def Sensor_0014 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 114.0;
            port data : Common::DataPort;
        }
        part def Controller_0014 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 214.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0014[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0014 : Common::MassLimit {
            subject component : Controller_0014;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0015 {
        part def Sensor_0015 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 115.0;
            port data : Common::DataPort;
        }
        part def Controller_0015 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 215.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0015[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0015 : Common::MassLimit {
            subject component : Controller_0015;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0016 {
        part def Sensor_0016 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 116.0;
            port data : Common::DataPort;
        }
        part def Controller_0016 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 216.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0016[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0016 : Common::MassLimit {
            subject component : Controller_0016;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0017 {
        part def Sensor_0017 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 117.0;
            port data : Common::DataPort;
        }
        part def Controller_0017 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 217.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0017[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0017 : Common::MassLimit {
            subject component : Controller_0017;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0018 {
        part def Sensor_0018 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 118.0;
            port data : Common::DataPort;
        }
        part def Controller_0018 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 218.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0018[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0018 : Common::MassLimit {
            subject component : Controller_0018;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0019 {
        part def Sensor_0019 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 119.0;
            port data : Common::DataPort;
        }
        part def Controller_0019 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 219.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0019[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0019 : Common::MassLimit {
            subject component : Controller_0019;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0020 {
        part def Sensor_0020 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 120.0;
            port data : Common::DataPort;
        }
        part def Controller_0020 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 220.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0020[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0020 : Common::MassLimit {
            subject component : Controller_0020;
            attribute :>> limit = 2.0 [kg];
        }
    }

    part def System {
        part controller_0001 : Subsystem_0001::Controller_0001;
        part controller_0002 : Subsystem_0002::Controller_0002;
        part controller_0003 : Subsystem_0003::Controller_0003;
        part controller_0004 : Subsystem_0004::Controller_0004;
        part controller_0005 : Subsystem_0005::Controller_0005;
        part controller_0006 : Subsystem_0006::Controller_0006;
        part controller_0007 : Subsystem_0007::Controller_0007;
        part controller_0008 : Subsystem_0008::Controller_0008;
        part controller_0009 : Subsystem_0009::Controller_0009;
        part controller_0010 : Subsystem_0010::Controller_0010;
        part controller_0011 : Subsystem_0011::Controller_0011;
        part controller_0012 : Subsystem_0012::Controller_0012;
        part controller_0013 : Subsystem_0013::Controller_0013;
        part controller_0014 : Subsystem_0014::Controller_0014;
        part controller_0015 : Subsystem_0015::Controller_0015;
        part controller_0016 : Subsystem_0016::Controller_0016;
        part controller_0017 : Subsystem_0017::Controller_0017;
        part controller_0018 : Subsystem_0018::Controller_0018;
        part controller_0019 : Subsystem_0019::Controller_0019;
        part controller_0020 : Subsystem_0020::Controller_0020;
        interface link_0001_0002 : Common::PowerLink connect
            source ::> controller_0001.supply to sink ::> controller_0002.power;
        interface link_0002_0003 : Common::PowerLink connect
            source ::> controller_0002.supply to sink ::> controller_0003.power;
        interface link_0003_0004 : Common::PowerLink connect
            source ::> controller_0003.supply to sink ::> controller_0004.power;
        interface link_0004_0005 : Common::PowerLink connect
            source ::> controller_0004.supply to sink ::> controller_0005.power;
        interface link_0005_0006 : Common::PowerLink connect
            source ::> controller_0005.supply to sink ::> controller_0006.power;
        interface link_0006_0007 : Common::PowerLink connect
            source ::> controller_0006.supply to sink ::> controller_0007.power;
        interface link_0007_0008 : Common::PowerLink connect
            source ::> controller_0007.supply to sink ::> controller_0008.power;
        interface link_0008_0009 : Common::PowerLink connect
            source ::> controller_0008.supply to sink ::> controller_0009.power;
        interface link_0009_0010 : Common::PowerLink connect
            source ::> controller_0009.supply to sink ::> controller_0010.power;
        interface link_0010_0011 : Common::PowerLink connect
            source ::> controller_0010.supply to sink ::> controller_0011.power;
        interface link_0011_0012 : Common::PowerLink connect
            source ::> controller_0011.supply to sink ::> controller_0012.power;
        interface link_0012_0013 : Common::PowerLink connect
            source ::> controller_0012.supply to sink ::> controller_0013.power;
        interface link_0013_0014 : Common::PowerLink connect
            source ::> controller_0013.supply to sink ::> controller_0014.power;
        interface link_0014_0015 : Common::PowerLink connect
            source ::> controller_0014.supply to sink ::> controller_0015.power;
        interface link_0015_0016 : Common::PowerLink connect
            source ::> controller_0015.supply to sink ::> controller_0016.power;
        interface link_0016_0017 : Common::PowerLink connect
            source ::> controller_0016.supply to sink ::> controller_0017.power;
        interface link_0017_0018 : Common::PowerLink connect
            source ::> controller_0017.supply to sink ::> controller_0018.power;
        interface link_0018_0019 : Common::PowerLink connect
            source ::> controller_0018.supply to sink ::> controller_0019.power;
        interface link_0019_0020 : Common::PowerLink connect
            source ::> controller_0019.supply to sink ::> controller_0020.power;
        attribute totalMass : MassValue = Common::TotalMass((controller_0001.mass, controller_0002.mass, controller_0003.mass, controller_0004.mass, controller_0005.mass, controller_0006.mass, controller_0007.mass, controller_0008.mass));
    }
    part system : System;
}

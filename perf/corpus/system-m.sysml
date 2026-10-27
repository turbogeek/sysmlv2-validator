package PerfSystem_m {
    doc /* Generated performance-test model (PerfCorpus): 200 subsystems. */
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

    package Subsystem_0021 {
        part def Sensor_0021 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 121.0;
            port data : Common::DataPort;
        }
        part def Controller_0021 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 221.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0021[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0021 : Common::MassLimit {
            subject component : Controller_0021;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0022 {
        part def Sensor_0022 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 122.0;
            port data : Common::DataPort;
        }
        part def Controller_0022 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 222.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0022[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0022 : Common::MassLimit {
            subject component : Controller_0022;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0023 {
        part def Sensor_0023 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 123.0;
            port data : Common::DataPort;
        }
        part def Controller_0023 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 223.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0023[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0023 : Common::MassLimit {
            subject component : Controller_0023;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0024 {
        part def Sensor_0024 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 124.0;
            port data : Common::DataPort;
        }
        part def Controller_0024 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 224.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0024[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0024 : Common::MassLimit {
            subject component : Controller_0024;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0025 {
        part def Sensor_0025 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 125.0;
            port data : Common::DataPort;
        }
        part def Controller_0025 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 225.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0025[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0025 : Common::MassLimit {
            subject component : Controller_0025;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0026 {
        part def Sensor_0026 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 126.0;
            port data : Common::DataPort;
        }
        part def Controller_0026 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 226.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0026[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0026 : Common::MassLimit {
            subject component : Controller_0026;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0027 {
        part def Sensor_0027 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 127.0;
            port data : Common::DataPort;
        }
        part def Controller_0027 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 227.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0027[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0027 : Common::MassLimit {
            subject component : Controller_0027;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0028 {
        part def Sensor_0028 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 128.0;
            port data : Common::DataPort;
        }
        part def Controller_0028 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 228.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0028[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0028 : Common::MassLimit {
            subject component : Controller_0028;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0029 {
        part def Sensor_0029 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 129.0;
            port data : Common::DataPort;
        }
        part def Controller_0029 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 229.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0029[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0029 : Common::MassLimit {
            subject component : Controller_0029;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0030 {
        part def Sensor_0030 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 130.0;
            port data : Common::DataPort;
        }
        part def Controller_0030 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 230.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0030[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0030 : Common::MassLimit {
            subject component : Controller_0030;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0031 {
        part def Sensor_0031 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 131.0;
            port data : Common::DataPort;
        }
        part def Controller_0031 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 231.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0031[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0031 : Common::MassLimit {
            subject component : Controller_0031;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0032 {
        part def Sensor_0032 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 132.0;
            port data : Common::DataPort;
        }
        part def Controller_0032 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 232.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0032[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0032 : Common::MassLimit {
            subject component : Controller_0032;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0033 {
        part def Sensor_0033 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 133.0;
            port data : Common::DataPort;
        }
        part def Controller_0033 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 233.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0033[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0033 : Common::MassLimit {
            subject component : Controller_0033;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0034 {
        part def Sensor_0034 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 134.0;
            port data : Common::DataPort;
        }
        part def Controller_0034 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 234.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0034[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0034 : Common::MassLimit {
            subject component : Controller_0034;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0035 {
        part def Sensor_0035 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 135.0;
            port data : Common::DataPort;
        }
        part def Controller_0035 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 235.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0035[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0035 : Common::MassLimit {
            subject component : Controller_0035;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0036 {
        part def Sensor_0036 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 136.0;
            port data : Common::DataPort;
        }
        part def Controller_0036 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 236.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0036[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0036 : Common::MassLimit {
            subject component : Controller_0036;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0037 {
        part def Sensor_0037 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 137.0;
            port data : Common::DataPort;
        }
        part def Controller_0037 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 237.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0037[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0037 : Common::MassLimit {
            subject component : Controller_0037;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0038 {
        part def Sensor_0038 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 138.0;
            port data : Common::DataPort;
        }
        part def Controller_0038 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 238.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0038[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0038 : Common::MassLimit {
            subject component : Controller_0038;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0039 {
        part def Sensor_0039 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 139.0;
            port data : Common::DataPort;
        }
        part def Controller_0039 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 239.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0039[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0039 : Common::MassLimit {
            subject component : Controller_0039;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0040 {
        part def Sensor_0040 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 140.0;
            port data : Common::DataPort;
        }
        part def Controller_0040 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 240.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0040[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0040 : Common::MassLimit {
            subject component : Controller_0040;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0041 {
        part def Sensor_0041 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 141.0;
            port data : Common::DataPort;
        }
        part def Controller_0041 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 241.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0041[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0041 : Common::MassLimit {
            subject component : Controller_0041;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0042 {
        part def Sensor_0042 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 142.0;
            port data : Common::DataPort;
        }
        part def Controller_0042 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 242.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0042[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0042 : Common::MassLimit {
            subject component : Controller_0042;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0043 {
        part def Sensor_0043 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 143.0;
            port data : Common::DataPort;
        }
        part def Controller_0043 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 243.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0043[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0043 : Common::MassLimit {
            subject component : Controller_0043;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0044 {
        part def Sensor_0044 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 144.0;
            port data : Common::DataPort;
        }
        part def Controller_0044 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 244.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0044[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0044 : Common::MassLimit {
            subject component : Controller_0044;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0045 {
        part def Sensor_0045 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 145.0;
            port data : Common::DataPort;
        }
        part def Controller_0045 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 245.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0045[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0045 : Common::MassLimit {
            subject component : Controller_0045;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0046 {
        part def Sensor_0046 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 146.0;
            port data : Common::DataPort;
        }
        part def Controller_0046 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 246.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0046[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0046 : Common::MassLimit {
            subject component : Controller_0046;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0047 {
        part def Sensor_0047 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 147.0;
            port data : Common::DataPort;
        }
        part def Controller_0047 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 247.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0047[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0047 : Common::MassLimit {
            subject component : Controller_0047;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0048 {
        part def Sensor_0048 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 148.0;
            port data : Common::DataPort;
        }
        part def Controller_0048 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 248.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0048[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0048 : Common::MassLimit {
            subject component : Controller_0048;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0049 {
        part def Sensor_0049 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 149.0;
            port data : Common::DataPort;
        }
        part def Controller_0049 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 249.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0049[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0049 : Common::MassLimit {
            subject component : Controller_0049;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0050 {
        part def Sensor_0050 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 100.0;
            port data : Common::DataPort;
        }
        part def Controller_0050 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 250.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0050[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0050 : Common::MassLimit {
            subject component : Controller_0050;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0051 {
        part def Sensor_0051 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 101.0;
            port data : Common::DataPort;
        }
        part def Controller_0051 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 251.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0051[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0051 : Common::MassLimit {
            subject component : Controller_0051;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0052 {
        part def Sensor_0052 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 102.0;
            port data : Common::DataPort;
        }
        part def Controller_0052 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 252.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0052[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0052 : Common::MassLimit {
            subject component : Controller_0052;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0053 {
        part def Sensor_0053 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 103.0;
            port data : Common::DataPort;
        }
        part def Controller_0053 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 253.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0053[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0053 : Common::MassLimit {
            subject component : Controller_0053;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0054 {
        part def Sensor_0054 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 104.0;
            port data : Common::DataPort;
        }
        part def Controller_0054 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 254.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0054[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0054 : Common::MassLimit {
            subject component : Controller_0054;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0055 {
        part def Sensor_0055 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 105.0;
            port data : Common::DataPort;
        }
        part def Controller_0055 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 255.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0055[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0055 : Common::MassLimit {
            subject component : Controller_0055;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0056 {
        part def Sensor_0056 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 106.0;
            port data : Common::DataPort;
        }
        part def Controller_0056 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 256.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0056[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0056 : Common::MassLimit {
            subject component : Controller_0056;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0057 {
        part def Sensor_0057 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 107.0;
            port data : Common::DataPort;
        }
        part def Controller_0057 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 257.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0057[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0057 : Common::MassLimit {
            subject component : Controller_0057;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0058 {
        part def Sensor_0058 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 108.0;
            port data : Common::DataPort;
        }
        part def Controller_0058 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 258.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0058[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0058 : Common::MassLimit {
            subject component : Controller_0058;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0059 {
        part def Sensor_0059 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 109.0;
            port data : Common::DataPort;
        }
        part def Controller_0059 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 259.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0059[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0059 : Common::MassLimit {
            subject component : Controller_0059;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0060 {
        part def Sensor_0060 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 110.0;
            port data : Common::DataPort;
        }
        part def Controller_0060 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 260.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0060[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0060 : Common::MassLimit {
            subject component : Controller_0060;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0061 {
        part def Sensor_0061 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 111.0;
            port data : Common::DataPort;
        }
        part def Controller_0061 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 261.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0061[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0061 : Common::MassLimit {
            subject component : Controller_0061;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0062 {
        part def Sensor_0062 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 112.0;
            port data : Common::DataPort;
        }
        part def Controller_0062 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 262.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0062[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0062 : Common::MassLimit {
            subject component : Controller_0062;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0063 {
        part def Sensor_0063 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 113.0;
            port data : Common::DataPort;
        }
        part def Controller_0063 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 263.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0063[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0063 : Common::MassLimit {
            subject component : Controller_0063;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0064 {
        part def Sensor_0064 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 114.0;
            port data : Common::DataPort;
        }
        part def Controller_0064 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 264.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0064[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0064 : Common::MassLimit {
            subject component : Controller_0064;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0065 {
        part def Sensor_0065 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 115.0;
            port data : Common::DataPort;
        }
        part def Controller_0065 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 265.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0065[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0065 : Common::MassLimit {
            subject component : Controller_0065;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0066 {
        part def Sensor_0066 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 116.0;
            port data : Common::DataPort;
        }
        part def Controller_0066 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 266.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0066[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0066 : Common::MassLimit {
            subject component : Controller_0066;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0067 {
        part def Sensor_0067 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 117.0;
            port data : Common::DataPort;
        }
        part def Controller_0067 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 267.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0067[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0067 : Common::MassLimit {
            subject component : Controller_0067;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0068 {
        part def Sensor_0068 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 118.0;
            port data : Common::DataPort;
        }
        part def Controller_0068 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 268.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0068[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0068 : Common::MassLimit {
            subject component : Controller_0068;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0069 {
        part def Sensor_0069 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 119.0;
            port data : Common::DataPort;
        }
        part def Controller_0069 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 269.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0069[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0069 : Common::MassLimit {
            subject component : Controller_0069;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0070 {
        part def Sensor_0070 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 120.0;
            port data : Common::DataPort;
        }
        part def Controller_0070 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 270.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0070[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0070 : Common::MassLimit {
            subject component : Controller_0070;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0071 {
        part def Sensor_0071 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 121.0;
            port data : Common::DataPort;
        }
        part def Controller_0071 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 271.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0071[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0071 : Common::MassLimit {
            subject component : Controller_0071;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0072 {
        part def Sensor_0072 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 122.0;
            port data : Common::DataPort;
        }
        part def Controller_0072 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 272.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0072[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0072 : Common::MassLimit {
            subject component : Controller_0072;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0073 {
        part def Sensor_0073 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 123.0;
            port data : Common::DataPort;
        }
        part def Controller_0073 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 273.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0073[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0073 : Common::MassLimit {
            subject component : Controller_0073;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0074 {
        part def Sensor_0074 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 124.0;
            port data : Common::DataPort;
        }
        part def Controller_0074 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 274.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0074[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0074 : Common::MassLimit {
            subject component : Controller_0074;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0075 {
        part def Sensor_0075 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 125.0;
            port data : Common::DataPort;
        }
        part def Controller_0075 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 275.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0075[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0075 : Common::MassLimit {
            subject component : Controller_0075;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0076 {
        part def Sensor_0076 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 126.0;
            port data : Common::DataPort;
        }
        part def Controller_0076 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 276.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0076[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0076 : Common::MassLimit {
            subject component : Controller_0076;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0077 {
        part def Sensor_0077 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 127.0;
            port data : Common::DataPort;
        }
        part def Controller_0077 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 277.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0077[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0077 : Common::MassLimit {
            subject component : Controller_0077;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0078 {
        part def Sensor_0078 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 128.0;
            port data : Common::DataPort;
        }
        part def Controller_0078 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 278.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0078[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0078 : Common::MassLimit {
            subject component : Controller_0078;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0079 {
        part def Sensor_0079 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 129.0;
            port data : Common::DataPort;
        }
        part def Controller_0079 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 279.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0079[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0079 : Common::MassLimit {
            subject component : Controller_0079;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0080 {
        part def Sensor_0080 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 130.0;
            port data : Common::DataPort;
        }
        part def Controller_0080 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 280.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0080[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0080 : Common::MassLimit {
            subject component : Controller_0080;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0081 {
        part def Sensor_0081 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 131.0;
            port data : Common::DataPort;
        }
        part def Controller_0081 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 281.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0081[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0081 : Common::MassLimit {
            subject component : Controller_0081;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0082 {
        part def Sensor_0082 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 132.0;
            port data : Common::DataPort;
        }
        part def Controller_0082 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 282.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0082[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0082 : Common::MassLimit {
            subject component : Controller_0082;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0083 {
        part def Sensor_0083 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 133.0;
            port data : Common::DataPort;
        }
        part def Controller_0083 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 283.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0083[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0083 : Common::MassLimit {
            subject component : Controller_0083;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0084 {
        part def Sensor_0084 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 134.0;
            port data : Common::DataPort;
        }
        part def Controller_0084 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 284.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0084[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0084 : Common::MassLimit {
            subject component : Controller_0084;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0085 {
        part def Sensor_0085 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 135.0;
            port data : Common::DataPort;
        }
        part def Controller_0085 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 285.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0085[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0085 : Common::MassLimit {
            subject component : Controller_0085;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0086 {
        part def Sensor_0086 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 136.0;
            port data : Common::DataPort;
        }
        part def Controller_0086 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 286.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0086[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0086 : Common::MassLimit {
            subject component : Controller_0086;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0087 {
        part def Sensor_0087 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 137.0;
            port data : Common::DataPort;
        }
        part def Controller_0087 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 287.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0087[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0087 : Common::MassLimit {
            subject component : Controller_0087;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0088 {
        part def Sensor_0088 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 138.0;
            port data : Common::DataPort;
        }
        part def Controller_0088 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 288.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0088[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0088 : Common::MassLimit {
            subject component : Controller_0088;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0089 {
        part def Sensor_0089 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 139.0;
            port data : Common::DataPort;
        }
        part def Controller_0089 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 289.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0089[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0089 : Common::MassLimit {
            subject component : Controller_0089;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0090 {
        part def Sensor_0090 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 140.0;
            port data : Common::DataPort;
        }
        part def Controller_0090 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 290.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0090[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0090 : Common::MassLimit {
            subject component : Controller_0090;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0091 {
        part def Sensor_0091 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 141.0;
            port data : Common::DataPort;
        }
        part def Controller_0091 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 291.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0091[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0091 : Common::MassLimit {
            subject component : Controller_0091;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0092 {
        part def Sensor_0092 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 142.0;
            port data : Common::DataPort;
        }
        part def Controller_0092 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 292.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0092[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0092 : Common::MassLimit {
            subject component : Controller_0092;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0093 {
        part def Sensor_0093 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 143.0;
            port data : Common::DataPort;
        }
        part def Controller_0093 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 293.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0093[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0093 : Common::MassLimit {
            subject component : Controller_0093;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0094 {
        part def Sensor_0094 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 144.0;
            port data : Common::DataPort;
        }
        part def Controller_0094 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 294.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0094[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0094 : Common::MassLimit {
            subject component : Controller_0094;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0095 {
        part def Sensor_0095 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 145.0;
            port data : Common::DataPort;
        }
        part def Controller_0095 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 295.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0095[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0095 : Common::MassLimit {
            subject component : Controller_0095;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0096 {
        part def Sensor_0096 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 146.0;
            port data : Common::DataPort;
        }
        part def Controller_0096 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 296.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0096[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0096 : Common::MassLimit {
            subject component : Controller_0096;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0097 {
        part def Sensor_0097 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 147.0;
            port data : Common::DataPort;
        }
        part def Controller_0097 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 297.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0097[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0097 : Common::MassLimit {
            subject component : Controller_0097;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0098 {
        part def Sensor_0098 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 148.0;
            port data : Common::DataPort;
        }
        part def Controller_0098 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 298.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0098[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0098 : Common::MassLimit {
            subject component : Controller_0098;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0099 {
        part def Sensor_0099 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 149.0;
            port data : Common::DataPort;
        }
        part def Controller_0099 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 299.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0099[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0099 : Common::MassLimit {
            subject component : Controller_0099;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0100 {
        part def Sensor_0100 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 100.0;
            port data : Common::DataPort;
        }
        part def Controller_0100 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 200.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0100[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0100 : Common::MassLimit {
            subject component : Controller_0100;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0101 {
        part def Sensor_0101 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 101.0;
            port data : Common::DataPort;
        }
        part def Controller_0101 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 201.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0101[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0101 : Common::MassLimit {
            subject component : Controller_0101;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0102 {
        part def Sensor_0102 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 102.0;
            port data : Common::DataPort;
        }
        part def Controller_0102 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 202.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0102[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0102 : Common::MassLimit {
            subject component : Controller_0102;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0103 {
        part def Sensor_0103 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 103.0;
            port data : Common::DataPort;
        }
        part def Controller_0103 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 203.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0103[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0103 : Common::MassLimit {
            subject component : Controller_0103;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0104 {
        part def Sensor_0104 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 104.0;
            port data : Common::DataPort;
        }
        part def Controller_0104 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 204.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0104[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0104 : Common::MassLimit {
            subject component : Controller_0104;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0105 {
        part def Sensor_0105 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 105.0;
            port data : Common::DataPort;
        }
        part def Controller_0105 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 205.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0105[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0105 : Common::MassLimit {
            subject component : Controller_0105;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0106 {
        part def Sensor_0106 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 106.0;
            port data : Common::DataPort;
        }
        part def Controller_0106 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 206.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0106[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0106 : Common::MassLimit {
            subject component : Controller_0106;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0107 {
        part def Sensor_0107 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 107.0;
            port data : Common::DataPort;
        }
        part def Controller_0107 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 207.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0107[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0107 : Common::MassLimit {
            subject component : Controller_0107;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0108 {
        part def Sensor_0108 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 108.0;
            port data : Common::DataPort;
        }
        part def Controller_0108 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 208.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0108[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0108 : Common::MassLimit {
            subject component : Controller_0108;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0109 {
        part def Sensor_0109 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 109.0;
            port data : Common::DataPort;
        }
        part def Controller_0109 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 209.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0109[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0109 : Common::MassLimit {
            subject component : Controller_0109;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0110 {
        part def Sensor_0110 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 110.0;
            port data : Common::DataPort;
        }
        part def Controller_0110 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 210.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0110[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0110 : Common::MassLimit {
            subject component : Controller_0110;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0111 {
        part def Sensor_0111 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 111.0;
            port data : Common::DataPort;
        }
        part def Controller_0111 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 211.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0111[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0111 : Common::MassLimit {
            subject component : Controller_0111;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0112 {
        part def Sensor_0112 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 112.0;
            port data : Common::DataPort;
        }
        part def Controller_0112 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 212.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0112[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0112 : Common::MassLimit {
            subject component : Controller_0112;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0113 {
        part def Sensor_0113 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 113.0;
            port data : Common::DataPort;
        }
        part def Controller_0113 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 213.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0113[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0113 : Common::MassLimit {
            subject component : Controller_0113;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0114 {
        part def Sensor_0114 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 114.0;
            port data : Common::DataPort;
        }
        part def Controller_0114 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 214.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0114[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0114 : Common::MassLimit {
            subject component : Controller_0114;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0115 {
        part def Sensor_0115 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 115.0;
            port data : Common::DataPort;
        }
        part def Controller_0115 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 215.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0115[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0115 : Common::MassLimit {
            subject component : Controller_0115;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0116 {
        part def Sensor_0116 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 116.0;
            port data : Common::DataPort;
        }
        part def Controller_0116 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 216.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0116[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0116 : Common::MassLimit {
            subject component : Controller_0116;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0117 {
        part def Sensor_0117 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 117.0;
            port data : Common::DataPort;
        }
        part def Controller_0117 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 217.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0117[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0117 : Common::MassLimit {
            subject component : Controller_0117;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0118 {
        part def Sensor_0118 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 118.0;
            port data : Common::DataPort;
        }
        part def Controller_0118 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 218.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0118[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0118 : Common::MassLimit {
            subject component : Controller_0118;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0119 {
        part def Sensor_0119 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 119.0;
            port data : Common::DataPort;
        }
        part def Controller_0119 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 219.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0119[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0119 : Common::MassLimit {
            subject component : Controller_0119;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0120 {
        part def Sensor_0120 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 120.0;
            port data : Common::DataPort;
        }
        part def Controller_0120 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 220.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0120[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0120 : Common::MassLimit {
            subject component : Controller_0120;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0121 {
        part def Sensor_0121 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 121.0;
            port data : Common::DataPort;
        }
        part def Controller_0121 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 221.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0121[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0121 : Common::MassLimit {
            subject component : Controller_0121;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0122 {
        part def Sensor_0122 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 122.0;
            port data : Common::DataPort;
        }
        part def Controller_0122 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 222.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0122[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0122 : Common::MassLimit {
            subject component : Controller_0122;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0123 {
        part def Sensor_0123 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 123.0;
            port data : Common::DataPort;
        }
        part def Controller_0123 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 223.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0123[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0123 : Common::MassLimit {
            subject component : Controller_0123;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0124 {
        part def Sensor_0124 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 124.0;
            port data : Common::DataPort;
        }
        part def Controller_0124 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 224.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0124[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0124 : Common::MassLimit {
            subject component : Controller_0124;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0125 {
        part def Sensor_0125 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 125.0;
            port data : Common::DataPort;
        }
        part def Controller_0125 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 225.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0125[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0125 : Common::MassLimit {
            subject component : Controller_0125;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0126 {
        part def Sensor_0126 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 126.0;
            port data : Common::DataPort;
        }
        part def Controller_0126 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 226.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0126[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0126 : Common::MassLimit {
            subject component : Controller_0126;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0127 {
        part def Sensor_0127 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 127.0;
            port data : Common::DataPort;
        }
        part def Controller_0127 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 227.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0127[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0127 : Common::MassLimit {
            subject component : Controller_0127;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0128 {
        part def Sensor_0128 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 128.0;
            port data : Common::DataPort;
        }
        part def Controller_0128 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 228.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0128[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0128 : Common::MassLimit {
            subject component : Controller_0128;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0129 {
        part def Sensor_0129 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 129.0;
            port data : Common::DataPort;
        }
        part def Controller_0129 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 229.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0129[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0129 : Common::MassLimit {
            subject component : Controller_0129;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0130 {
        part def Sensor_0130 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 130.0;
            port data : Common::DataPort;
        }
        part def Controller_0130 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 230.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0130[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0130 : Common::MassLimit {
            subject component : Controller_0130;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0131 {
        part def Sensor_0131 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 131.0;
            port data : Common::DataPort;
        }
        part def Controller_0131 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 231.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0131[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0131 : Common::MassLimit {
            subject component : Controller_0131;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0132 {
        part def Sensor_0132 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 132.0;
            port data : Common::DataPort;
        }
        part def Controller_0132 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 232.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0132[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0132 : Common::MassLimit {
            subject component : Controller_0132;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0133 {
        part def Sensor_0133 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 133.0;
            port data : Common::DataPort;
        }
        part def Controller_0133 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 233.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0133[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0133 : Common::MassLimit {
            subject component : Controller_0133;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0134 {
        part def Sensor_0134 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 134.0;
            port data : Common::DataPort;
        }
        part def Controller_0134 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 234.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0134[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0134 : Common::MassLimit {
            subject component : Controller_0134;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0135 {
        part def Sensor_0135 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 135.0;
            port data : Common::DataPort;
        }
        part def Controller_0135 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 235.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0135[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0135 : Common::MassLimit {
            subject component : Controller_0135;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0136 {
        part def Sensor_0136 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 136.0;
            port data : Common::DataPort;
        }
        part def Controller_0136 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 236.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0136[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0136 : Common::MassLimit {
            subject component : Controller_0136;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0137 {
        part def Sensor_0137 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 137.0;
            port data : Common::DataPort;
        }
        part def Controller_0137 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 237.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0137[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0137 : Common::MassLimit {
            subject component : Controller_0137;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0138 {
        part def Sensor_0138 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 138.0;
            port data : Common::DataPort;
        }
        part def Controller_0138 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 238.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0138[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0138 : Common::MassLimit {
            subject component : Controller_0138;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0139 {
        part def Sensor_0139 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 139.0;
            port data : Common::DataPort;
        }
        part def Controller_0139 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 239.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0139[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0139 : Common::MassLimit {
            subject component : Controller_0139;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0140 {
        part def Sensor_0140 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 140.0;
            port data : Common::DataPort;
        }
        part def Controller_0140 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 240.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0140[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0140 : Common::MassLimit {
            subject component : Controller_0140;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0141 {
        part def Sensor_0141 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 141.0;
            port data : Common::DataPort;
        }
        part def Controller_0141 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 241.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0141[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0141 : Common::MassLimit {
            subject component : Controller_0141;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0142 {
        part def Sensor_0142 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 142.0;
            port data : Common::DataPort;
        }
        part def Controller_0142 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 242.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0142[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0142 : Common::MassLimit {
            subject component : Controller_0142;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0143 {
        part def Sensor_0143 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 143.0;
            port data : Common::DataPort;
        }
        part def Controller_0143 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 243.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0143[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0143 : Common::MassLimit {
            subject component : Controller_0143;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0144 {
        part def Sensor_0144 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 144.0;
            port data : Common::DataPort;
        }
        part def Controller_0144 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 244.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0144[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0144 : Common::MassLimit {
            subject component : Controller_0144;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0145 {
        part def Sensor_0145 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 145.0;
            port data : Common::DataPort;
        }
        part def Controller_0145 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 245.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0145[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0145 : Common::MassLimit {
            subject component : Controller_0145;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0146 {
        part def Sensor_0146 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 146.0;
            port data : Common::DataPort;
        }
        part def Controller_0146 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 246.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0146[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0146 : Common::MassLimit {
            subject component : Controller_0146;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0147 {
        part def Sensor_0147 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 147.0;
            port data : Common::DataPort;
        }
        part def Controller_0147 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 247.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0147[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0147 : Common::MassLimit {
            subject component : Controller_0147;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0148 {
        part def Sensor_0148 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 148.0;
            port data : Common::DataPort;
        }
        part def Controller_0148 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 248.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0148[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0148 : Common::MassLimit {
            subject component : Controller_0148;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0149 {
        part def Sensor_0149 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 149.0;
            port data : Common::DataPort;
        }
        part def Controller_0149 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 249.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0149[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0149 : Common::MassLimit {
            subject component : Controller_0149;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0150 {
        part def Sensor_0150 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 100.0;
            port data : Common::DataPort;
        }
        part def Controller_0150 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 250.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0150[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0150 : Common::MassLimit {
            subject component : Controller_0150;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0151 {
        part def Sensor_0151 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 101.0;
            port data : Common::DataPort;
        }
        part def Controller_0151 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 251.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0151[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0151 : Common::MassLimit {
            subject component : Controller_0151;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0152 {
        part def Sensor_0152 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 102.0;
            port data : Common::DataPort;
        }
        part def Controller_0152 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 252.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0152[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0152 : Common::MassLimit {
            subject component : Controller_0152;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0153 {
        part def Sensor_0153 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 103.0;
            port data : Common::DataPort;
        }
        part def Controller_0153 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 253.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0153[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0153 : Common::MassLimit {
            subject component : Controller_0153;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0154 {
        part def Sensor_0154 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 104.0;
            port data : Common::DataPort;
        }
        part def Controller_0154 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 254.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0154[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0154 : Common::MassLimit {
            subject component : Controller_0154;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0155 {
        part def Sensor_0155 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 105.0;
            port data : Common::DataPort;
        }
        part def Controller_0155 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 255.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0155[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0155 : Common::MassLimit {
            subject component : Controller_0155;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0156 {
        part def Sensor_0156 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 106.0;
            port data : Common::DataPort;
        }
        part def Controller_0156 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 256.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0156[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0156 : Common::MassLimit {
            subject component : Controller_0156;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0157 {
        part def Sensor_0157 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 107.0;
            port data : Common::DataPort;
        }
        part def Controller_0157 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 257.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0157[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0157 : Common::MassLimit {
            subject component : Controller_0157;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0158 {
        part def Sensor_0158 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 108.0;
            port data : Common::DataPort;
        }
        part def Controller_0158 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 258.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0158[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0158 : Common::MassLimit {
            subject component : Controller_0158;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0159 {
        part def Sensor_0159 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 109.0;
            port data : Common::DataPort;
        }
        part def Controller_0159 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 259.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0159[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0159 : Common::MassLimit {
            subject component : Controller_0159;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0160 {
        part def Sensor_0160 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 110.0;
            port data : Common::DataPort;
        }
        part def Controller_0160 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 260.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0160[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0160 : Common::MassLimit {
            subject component : Controller_0160;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0161 {
        part def Sensor_0161 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 111.0;
            port data : Common::DataPort;
        }
        part def Controller_0161 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 261.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0161[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0161 : Common::MassLimit {
            subject component : Controller_0161;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0162 {
        part def Sensor_0162 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 112.0;
            port data : Common::DataPort;
        }
        part def Controller_0162 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 262.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0162[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0162 : Common::MassLimit {
            subject component : Controller_0162;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0163 {
        part def Sensor_0163 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 113.0;
            port data : Common::DataPort;
        }
        part def Controller_0163 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 263.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0163[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0163 : Common::MassLimit {
            subject component : Controller_0163;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0164 {
        part def Sensor_0164 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 114.0;
            port data : Common::DataPort;
        }
        part def Controller_0164 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 264.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0164[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0164 : Common::MassLimit {
            subject component : Controller_0164;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0165 {
        part def Sensor_0165 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 115.0;
            port data : Common::DataPort;
        }
        part def Controller_0165 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 265.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0165[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0165 : Common::MassLimit {
            subject component : Controller_0165;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0166 {
        part def Sensor_0166 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 116.0;
            port data : Common::DataPort;
        }
        part def Controller_0166 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 266.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0166[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0166 : Common::MassLimit {
            subject component : Controller_0166;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0167 {
        part def Sensor_0167 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 117.0;
            port data : Common::DataPort;
        }
        part def Controller_0167 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 267.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0167[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0167 : Common::MassLimit {
            subject component : Controller_0167;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0168 {
        part def Sensor_0168 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 118.0;
            port data : Common::DataPort;
        }
        part def Controller_0168 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 268.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0168[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0168 : Common::MassLimit {
            subject component : Controller_0168;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0169 {
        part def Sensor_0169 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 119.0;
            port data : Common::DataPort;
        }
        part def Controller_0169 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 269.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0169[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0169 : Common::MassLimit {
            subject component : Controller_0169;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0170 {
        part def Sensor_0170 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 120.0;
            port data : Common::DataPort;
        }
        part def Controller_0170 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 270.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0170[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0170 : Common::MassLimit {
            subject component : Controller_0170;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0171 {
        part def Sensor_0171 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 121.0;
            port data : Common::DataPort;
        }
        part def Controller_0171 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 271.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0171[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0171 : Common::MassLimit {
            subject component : Controller_0171;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0172 {
        part def Sensor_0172 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 122.0;
            port data : Common::DataPort;
        }
        part def Controller_0172 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 272.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0172[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0172 : Common::MassLimit {
            subject component : Controller_0172;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0173 {
        part def Sensor_0173 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 123.0;
            port data : Common::DataPort;
        }
        part def Controller_0173 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 273.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0173[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0173 : Common::MassLimit {
            subject component : Controller_0173;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0174 {
        part def Sensor_0174 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 124.0;
            port data : Common::DataPort;
        }
        part def Controller_0174 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 274.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0174[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0174 : Common::MassLimit {
            subject component : Controller_0174;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0175 {
        part def Sensor_0175 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 125.0;
            port data : Common::DataPort;
        }
        part def Controller_0175 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 275.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0175[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0175 : Common::MassLimit {
            subject component : Controller_0175;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0176 {
        part def Sensor_0176 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 126.0;
            port data : Common::DataPort;
        }
        part def Controller_0176 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 276.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0176[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0176 : Common::MassLimit {
            subject component : Controller_0176;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0177 {
        part def Sensor_0177 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 127.0;
            port data : Common::DataPort;
        }
        part def Controller_0177 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 277.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0177[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0177 : Common::MassLimit {
            subject component : Controller_0177;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0178 {
        part def Sensor_0178 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 128.0;
            port data : Common::DataPort;
        }
        part def Controller_0178 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 278.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0178[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0178 : Common::MassLimit {
            subject component : Controller_0178;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0179 {
        part def Sensor_0179 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 129.0;
            port data : Common::DataPort;
        }
        part def Controller_0179 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 279.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0179[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0179 : Common::MassLimit {
            subject component : Controller_0179;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0180 {
        part def Sensor_0180 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 130.0;
            port data : Common::DataPort;
        }
        part def Controller_0180 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 280.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0180[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0180 : Common::MassLimit {
            subject component : Controller_0180;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0181 {
        part def Sensor_0181 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 31.0;
            attribute range : Real = 131.0;
            port data : Common::DataPort;
        }
        part def Controller_0181 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 281.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0181[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0181 : Common::MassLimit {
            subject component : Controller_0181;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0182 {
        part def Sensor_0182 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 32.0;
            attribute range : Real = 132.0;
            port data : Common::DataPort;
        }
        part def Controller_0182 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 282.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0182[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0182 : Common::MassLimit {
            subject component : Controller_0182;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0183 {
        part def Sensor_0183 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 33.0;
            attribute range : Real = 133.0;
            port data : Common::DataPort;
        }
        part def Controller_0183 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 283.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0183[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0183 : Common::MassLimit {
            subject component : Controller_0183;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0184 {
        part def Sensor_0184 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 34.0;
            attribute range : Real = 134.0;
            port data : Common::DataPort;
        }
        part def Controller_0184 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 284.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0184[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0184 : Common::MassLimit {
            subject component : Controller_0184;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0185 {
        part def Sensor_0185 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 35.0;
            attribute range : Real = 135.0;
            port data : Common::DataPort;
        }
        part def Controller_0185 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 285.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0185[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0185 : Common::MassLimit {
            subject component : Controller_0185;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0186 {
        part def Sensor_0186 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 36.0;
            attribute range : Real = 136.0;
            port data : Common::DataPort;
        }
        part def Controller_0186 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 286.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0186[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0186 : Common::MassLimit {
            subject component : Controller_0186;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0187 {
        part def Sensor_0187 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 37.0;
            attribute range : Real = 137.0;
            port data : Common::DataPort;
        }
        part def Controller_0187 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 287.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0187[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0187 : Common::MassLimit {
            subject component : Controller_0187;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0188 {
        part def Sensor_0188 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 38.0;
            attribute range : Real = 138.0;
            port data : Common::DataPort;
        }
        part def Controller_0188 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 288.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0188[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0188 : Common::MassLimit {
            subject component : Controller_0188;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0189 {
        part def Sensor_0189 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 39.0;
            attribute range : Real = 139.0;
            port data : Common::DataPort;
        }
        part def Controller_0189 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 289.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0189[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0189 : Common::MassLimit {
            subject component : Controller_0189;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0190 {
        part def Sensor_0190 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 40.0;
            attribute range : Real = 140.0;
            port data : Common::DataPort;
        }
        part def Controller_0190 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 290.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0190[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0190 : Common::MassLimit {
            subject component : Controller_0190;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0191 {
        part def Sensor_0191 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 41.0;
            attribute range : Real = 141.0;
            port data : Common::DataPort;
        }
        part def Controller_0191 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 291.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0191[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0191 : Common::MassLimit {
            subject component : Controller_0191;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0192 {
        part def Sensor_0192 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 42.0;
            attribute range : Real = 142.0;
            port data : Common::DataPort;
        }
        part def Controller_0192 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 292.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0192[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0192 : Common::MassLimit {
            subject component : Controller_0192;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0193 {
        part def Sensor_0193 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 43.0;
            attribute range : Real = 143.0;
            port data : Common::DataPort;
        }
        part def Controller_0193 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 293.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0193[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0193 : Common::MassLimit {
            subject component : Controller_0193;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0194 {
        part def Sensor_0194 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 44.0;
            attribute range : Real = 144.0;
            port data : Common::DataPort;
        }
        part def Controller_0194 :> Common::Component {
            attribute :>> mass = 1.50 [kg];
            attribute :>> cost = 294.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0194[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0194 : Common::MassLimit {
            subject component : Controller_0194;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0195 {
        part def Sensor_0195 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 45.0;
            attribute range : Real = 145.0;
            port data : Common::DataPort;
        }
        part def Controller_0195 :> Common::Component {
            attribute :>> mass = 1.60 [kg];
            attribute :>> cost = 295.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0195[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0195 : Common::MassLimit {
            subject component : Controller_0195;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0196 {
        part def Sensor_0196 :> Common::Component {
            attribute :>> mass = 0.21 [kg];
            attribute :>> cost = 46.0;
            attribute range : Real = 146.0;
            port data : Common::DataPort;
        }
        part def Controller_0196 :> Common::Component {
            attribute :>> mass = 1.00 [kg];
            attribute :>> cost = 296.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0196[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0196 : Common::MassLimit {
            subject component : Controller_0196;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0197 {
        part def Sensor_0197 :> Common::Component {
            attribute :>> mass = 0.22 [kg];
            attribute :>> cost = 47.0;
            attribute range : Real = 147.0;
            port data : Common::DataPort;
        }
        part def Controller_0197 :> Common::Component {
            attribute :>> mass = 1.10 [kg];
            attribute :>> cost = 297.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0197[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0197 : Common::MassLimit {
            subject component : Controller_0197;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0198 {
        part def Sensor_0198 :> Common::Component {
            attribute :>> mass = 0.23 [kg];
            attribute :>> cost = 48.0;
            attribute range : Real = 148.0;
            port data : Common::DataPort;
        }
        part def Controller_0198 :> Common::Component {
            attribute :>> mass = 1.20 [kg];
            attribute :>> cost = 298.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0198[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0198 : Common::MassLimit {
            subject component : Controller_0198;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0199 {
        part def Sensor_0199 :> Common::Component {
            attribute :>> mass = 0.24 [kg];
            attribute :>> cost = 49.0;
            attribute range : Real = 149.0;
            port data : Common::DataPort;
        }
        part def Controller_0199 :> Common::Component {
            attribute :>> mass = 1.30 [kg];
            attribute :>> cost = 299.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0199[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0199 : Common::MassLimit {
            subject component : Controller_0199;
            attribute :>> limit = 2.0 [kg];
        }
    }

    package Subsystem_0200 {
        part def Sensor_0200 :> Common::Component {
            attribute :>> mass = 0.20 [kg];
            attribute :>> cost = 30.0;
            attribute range : Real = 100.0;
            port data : Common::DataPort;
        }
        part def Controller_0200 :> Common::Component {
            attribute :>> mass = 1.40 [kg];
            attribute :>> cost = 200.0;
            port supply : Common::PowerPort;
            part sensors : Sensor_0200[2];
            action smooth : Common::Process;
            action forward : Common::Process;
            succession first smooth then forward;
            exhibit state lifecycle : Common::Lifecycle;
        }
        requirement controllerMass_0200 : Common::MassLimit {
            subject component : Controller_0200;
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
        part controller_0021 : Subsystem_0021::Controller_0021;
        part controller_0022 : Subsystem_0022::Controller_0022;
        part controller_0023 : Subsystem_0023::Controller_0023;
        part controller_0024 : Subsystem_0024::Controller_0024;
        part controller_0025 : Subsystem_0025::Controller_0025;
        part controller_0026 : Subsystem_0026::Controller_0026;
        part controller_0027 : Subsystem_0027::Controller_0027;
        part controller_0028 : Subsystem_0028::Controller_0028;
        part controller_0029 : Subsystem_0029::Controller_0029;
        part controller_0030 : Subsystem_0030::Controller_0030;
        part controller_0031 : Subsystem_0031::Controller_0031;
        part controller_0032 : Subsystem_0032::Controller_0032;
        part controller_0033 : Subsystem_0033::Controller_0033;
        part controller_0034 : Subsystem_0034::Controller_0034;
        part controller_0035 : Subsystem_0035::Controller_0035;
        part controller_0036 : Subsystem_0036::Controller_0036;
        part controller_0037 : Subsystem_0037::Controller_0037;
        part controller_0038 : Subsystem_0038::Controller_0038;
        part controller_0039 : Subsystem_0039::Controller_0039;
        part controller_0040 : Subsystem_0040::Controller_0040;
        part controller_0041 : Subsystem_0041::Controller_0041;
        part controller_0042 : Subsystem_0042::Controller_0042;
        part controller_0043 : Subsystem_0043::Controller_0043;
        part controller_0044 : Subsystem_0044::Controller_0044;
        part controller_0045 : Subsystem_0045::Controller_0045;
        part controller_0046 : Subsystem_0046::Controller_0046;
        part controller_0047 : Subsystem_0047::Controller_0047;
        part controller_0048 : Subsystem_0048::Controller_0048;
        part controller_0049 : Subsystem_0049::Controller_0049;
        part controller_0050 : Subsystem_0050::Controller_0050;
        part controller_0051 : Subsystem_0051::Controller_0051;
        part controller_0052 : Subsystem_0052::Controller_0052;
        part controller_0053 : Subsystem_0053::Controller_0053;
        part controller_0054 : Subsystem_0054::Controller_0054;
        part controller_0055 : Subsystem_0055::Controller_0055;
        part controller_0056 : Subsystem_0056::Controller_0056;
        part controller_0057 : Subsystem_0057::Controller_0057;
        part controller_0058 : Subsystem_0058::Controller_0058;
        part controller_0059 : Subsystem_0059::Controller_0059;
        part controller_0060 : Subsystem_0060::Controller_0060;
        part controller_0061 : Subsystem_0061::Controller_0061;
        part controller_0062 : Subsystem_0062::Controller_0062;
        part controller_0063 : Subsystem_0063::Controller_0063;
        part controller_0064 : Subsystem_0064::Controller_0064;
        part controller_0065 : Subsystem_0065::Controller_0065;
        part controller_0066 : Subsystem_0066::Controller_0066;
        part controller_0067 : Subsystem_0067::Controller_0067;
        part controller_0068 : Subsystem_0068::Controller_0068;
        part controller_0069 : Subsystem_0069::Controller_0069;
        part controller_0070 : Subsystem_0070::Controller_0070;
        part controller_0071 : Subsystem_0071::Controller_0071;
        part controller_0072 : Subsystem_0072::Controller_0072;
        part controller_0073 : Subsystem_0073::Controller_0073;
        part controller_0074 : Subsystem_0074::Controller_0074;
        part controller_0075 : Subsystem_0075::Controller_0075;
        part controller_0076 : Subsystem_0076::Controller_0076;
        part controller_0077 : Subsystem_0077::Controller_0077;
        part controller_0078 : Subsystem_0078::Controller_0078;
        part controller_0079 : Subsystem_0079::Controller_0079;
        part controller_0080 : Subsystem_0080::Controller_0080;
        part controller_0081 : Subsystem_0081::Controller_0081;
        part controller_0082 : Subsystem_0082::Controller_0082;
        part controller_0083 : Subsystem_0083::Controller_0083;
        part controller_0084 : Subsystem_0084::Controller_0084;
        part controller_0085 : Subsystem_0085::Controller_0085;
        part controller_0086 : Subsystem_0086::Controller_0086;
        part controller_0087 : Subsystem_0087::Controller_0087;
        part controller_0088 : Subsystem_0088::Controller_0088;
        part controller_0089 : Subsystem_0089::Controller_0089;
        part controller_0090 : Subsystem_0090::Controller_0090;
        part controller_0091 : Subsystem_0091::Controller_0091;
        part controller_0092 : Subsystem_0092::Controller_0092;
        part controller_0093 : Subsystem_0093::Controller_0093;
        part controller_0094 : Subsystem_0094::Controller_0094;
        part controller_0095 : Subsystem_0095::Controller_0095;
        part controller_0096 : Subsystem_0096::Controller_0096;
        part controller_0097 : Subsystem_0097::Controller_0097;
        part controller_0098 : Subsystem_0098::Controller_0098;
        part controller_0099 : Subsystem_0099::Controller_0099;
        part controller_0100 : Subsystem_0100::Controller_0100;
        part controller_0101 : Subsystem_0101::Controller_0101;
        part controller_0102 : Subsystem_0102::Controller_0102;
        part controller_0103 : Subsystem_0103::Controller_0103;
        part controller_0104 : Subsystem_0104::Controller_0104;
        part controller_0105 : Subsystem_0105::Controller_0105;
        part controller_0106 : Subsystem_0106::Controller_0106;
        part controller_0107 : Subsystem_0107::Controller_0107;
        part controller_0108 : Subsystem_0108::Controller_0108;
        part controller_0109 : Subsystem_0109::Controller_0109;
        part controller_0110 : Subsystem_0110::Controller_0110;
        part controller_0111 : Subsystem_0111::Controller_0111;
        part controller_0112 : Subsystem_0112::Controller_0112;
        part controller_0113 : Subsystem_0113::Controller_0113;
        part controller_0114 : Subsystem_0114::Controller_0114;
        part controller_0115 : Subsystem_0115::Controller_0115;
        part controller_0116 : Subsystem_0116::Controller_0116;
        part controller_0117 : Subsystem_0117::Controller_0117;
        part controller_0118 : Subsystem_0118::Controller_0118;
        part controller_0119 : Subsystem_0119::Controller_0119;
        part controller_0120 : Subsystem_0120::Controller_0120;
        part controller_0121 : Subsystem_0121::Controller_0121;
        part controller_0122 : Subsystem_0122::Controller_0122;
        part controller_0123 : Subsystem_0123::Controller_0123;
        part controller_0124 : Subsystem_0124::Controller_0124;
        part controller_0125 : Subsystem_0125::Controller_0125;
        part controller_0126 : Subsystem_0126::Controller_0126;
        part controller_0127 : Subsystem_0127::Controller_0127;
        part controller_0128 : Subsystem_0128::Controller_0128;
        part controller_0129 : Subsystem_0129::Controller_0129;
        part controller_0130 : Subsystem_0130::Controller_0130;
        part controller_0131 : Subsystem_0131::Controller_0131;
        part controller_0132 : Subsystem_0132::Controller_0132;
        part controller_0133 : Subsystem_0133::Controller_0133;
        part controller_0134 : Subsystem_0134::Controller_0134;
        part controller_0135 : Subsystem_0135::Controller_0135;
        part controller_0136 : Subsystem_0136::Controller_0136;
        part controller_0137 : Subsystem_0137::Controller_0137;
        part controller_0138 : Subsystem_0138::Controller_0138;
        part controller_0139 : Subsystem_0139::Controller_0139;
        part controller_0140 : Subsystem_0140::Controller_0140;
        part controller_0141 : Subsystem_0141::Controller_0141;
        part controller_0142 : Subsystem_0142::Controller_0142;
        part controller_0143 : Subsystem_0143::Controller_0143;
        part controller_0144 : Subsystem_0144::Controller_0144;
        part controller_0145 : Subsystem_0145::Controller_0145;
        part controller_0146 : Subsystem_0146::Controller_0146;
        part controller_0147 : Subsystem_0147::Controller_0147;
        part controller_0148 : Subsystem_0148::Controller_0148;
        part controller_0149 : Subsystem_0149::Controller_0149;
        part controller_0150 : Subsystem_0150::Controller_0150;
        part controller_0151 : Subsystem_0151::Controller_0151;
        part controller_0152 : Subsystem_0152::Controller_0152;
        part controller_0153 : Subsystem_0153::Controller_0153;
        part controller_0154 : Subsystem_0154::Controller_0154;
        part controller_0155 : Subsystem_0155::Controller_0155;
        part controller_0156 : Subsystem_0156::Controller_0156;
        part controller_0157 : Subsystem_0157::Controller_0157;
        part controller_0158 : Subsystem_0158::Controller_0158;
        part controller_0159 : Subsystem_0159::Controller_0159;
        part controller_0160 : Subsystem_0160::Controller_0160;
        part controller_0161 : Subsystem_0161::Controller_0161;
        part controller_0162 : Subsystem_0162::Controller_0162;
        part controller_0163 : Subsystem_0163::Controller_0163;
        part controller_0164 : Subsystem_0164::Controller_0164;
        part controller_0165 : Subsystem_0165::Controller_0165;
        part controller_0166 : Subsystem_0166::Controller_0166;
        part controller_0167 : Subsystem_0167::Controller_0167;
        part controller_0168 : Subsystem_0168::Controller_0168;
        part controller_0169 : Subsystem_0169::Controller_0169;
        part controller_0170 : Subsystem_0170::Controller_0170;
        part controller_0171 : Subsystem_0171::Controller_0171;
        part controller_0172 : Subsystem_0172::Controller_0172;
        part controller_0173 : Subsystem_0173::Controller_0173;
        part controller_0174 : Subsystem_0174::Controller_0174;
        part controller_0175 : Subsystem_0175::Controller_0175;
        part controller_0176 : Subsystem_0176::Controller_0176;
        part controller_0177 : Subsystem_0177::Controller_0177;
        part controller_0178 : Subsystem_0178::Controller_0178;
        part controller_0179 : Subsystem_0179::Controller_0179;
        part controller_0180 : Subsystem_0180::Controller_0180;
        part controller_0181 : Subsystem_0181::Controller_0181;
        part controller_0182 : Subsystem_0182::Controller_0182;
        part controller_0183 : Subsystem_0183::Controller_0183;
        part controller_0184 : Subsystem_0184::Controller_0184;
        part controller_0185 : Subsystem_0185::Controller_0185;
        part controller_0186 : Subsystem_0186::Controller_0186;
        part controller_0187 : Subsystem_0187::Controller_0187;
        part controller_0188 : Subsystem_0188::Controller_0188;
        part controller_0189 : Subsystem_0189::Controller_0189;
        part controller_0190 : Subsystem_0190::Controller_0190;
        part controller_0191 : Subsystem_0191::Controller_0191;
        part controller_0192 : Subsystem_0192::Controller_0192;
        part controller_0193 : Subsystem_0193::Controller_0193;
        part controller_0194 : Subsystem_0194::Controller_0194;
        part controller_0195 : Subsystem_0195::Controller_0195;
        part controller_0196 : Subsystem_0196::Controller_0196;
        part controller_0197 : Subsystem_0197::Controller_0197;
        part controller_0198 : Subsystem_0198::Controller_0198;
        part controller_0199 : Subsystem_0199::Controller_0199;
        part controller_0200 : Subsystem_0200::Controller_0200;
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
        interface link_0020_0021 : Common::PowerLink connect
            source ::> controller_0020.supply to sink ::> controller_0021.power;
        interface link_0021_0022 : Common::PowerLink connect
            source ::> controller_0021.supply to sink ::> controller_0022.power;
        interface link_0022_0023 : Common::PowerLink connect
            source ::> controller_0022.supply to sink ::> controller_0023.power;
        interface link_0023_0024 : Common::PowerLink connect
            source ::> controller_0023.supply to sink ::> controller_0024.power;
        interface link_0024_0025 : Common::PowerLink connect
            source ::> controller_0024.supply to sink ::> controller_0025.power;
        interface link_0025_0026 : Common::PowerLink connect
            source ::> controller_0025.supply to sink ::> controller_0026.power;
        interface link_0026_0027 : Common::PowerLink connect
            source ::> controller_0026.supply to sink ::> controller_0027.power;
        interface link_0027_0028 : Common::PowerLink connect
            source ::> controller_0027.supply to sink ::> controller_0028.power;
        interface link_0028_0029 : Common::PowerLink connect
            source ::> controller_0028.supply to sink ::> controller_0029.power;
        interface link_0029_0030 : Common::PowerLink connect
            source ::> controller_0029.supply to sink ::> controller_0030.power;
        interface link_0030_0031 : Common::PowerLink connect
            source ::> controller_0030.supply to sink ::> controller_0031.power;
        interface link_0031_0032 : Common::PowerLink connect
            source ::> controller_0031.supply to sink ::> controller_0032.power;
        interface link_0032_0033 : Common::PowerLink connect
            source ::> controller_0032.supply to sink ::> controller_0033.power;
        interface link_0033_0034 : Common::PowerLink connect
            source ::> controller_0033.supply to sink ::> controller_0034.power;
        interface link_0034_0035 : Common::PowerLink connect
            source ::> controller_0034.supply to sink ::> controller_0035.power;
        interface link_0035_0036 : Common::PowerLink connect
            source ::> controller_0035.supply to sink ::> controller_0036.power;
        interface link_0036_0037 : Common::PowerLink connect
            source ::> controller_0036.supply to sink ::> controller_0037.power;
        interface link_0037_0038 : Common::PowerLink connect
            source ::> controller_0037.supply to sink ::> controller_0038.power;
        interface link_0038_0039 : Common::PowerLink connect
            source ::> controller_0038.supply to sink ::> controller_0039.power;
        interface link_0039_0040 : Common::PowerLink connect
            source ::> controller_0039.supply to sink ::> controller_0040.power;
        interface link_0040_0041 : Common::PowerLink connect
            source ::> controller_0040.supply to sink ::> controller_0041.power;
        interface link_0041_0042 : Common::PowerLink connect
            source ::> controller_0041.supply to sink ::> controller_0042.power;
        interface link_0042_0043 : Common::PowerLink connect
            source ::> controller_0042.supply to sink ::> controller_0043.power;
        interface link_0043_0044 : Common::PowerLink connect
            source ::> controller_0043.supply to sink ::> controller_0044.power;
        interface link_0044_0045 : Common::PowerLink connect
            source ::> controller_0044.supply to sink ::> controller_0045.power;
        interface link_0045_0046 : Common::PowerLink connect
            source ::> controller_0045.supply to sink ::> controller_0046.power;
        interface link_0046_0047 : Common::PowerLink connect
            source ::> controller_0046.supply to sink ::> controller_0047.power;
        interface link_0047_0048 : Common::PowerLink connect
            source ::> controller_0047.supply to sink ::> controller_0048.power;
        interface link_0048_0049 : Common::PowerLink connect
            source ::> controller_0048.supply to sink ::> controller_0049.power;
        interface link_0049_0050 : Common::PowerLink connect
            source ::> controller_0049.supply to sink ::> controller_0050.power;
        interface link_0050_0051 : Common::PowerLink connect
            source ::> controller_0050.supply to sink ::> controller_0051.power;
        interface link_0051_0052 : Common::PowerLink connect
            source ::> controller_0051.supply to sink ::> controller_0052.power;
        interface link_0052_0053 : Common::PowerLink connect
            source ::> controller_0052.supply to sink ::> controller_0053.power;
        interface link_0053_0054 : Common::PowerLink connect
            source ::> controller_0053.supply to sink ::> controller_0054.power;
        interface link_0054_0055 : Common::PowerLink connect
            source ::> controller_0054.supply to sink ::> controller_0055.power;
        interface link_0055_0056 : Common::PowerLink connect
            source ::> controller_0055.supply to sink ::> controller_0056.power;
        interface link_0056_0057 : Common::PowerLink connect
            source ::> controller_0056.supply to sink ::> controller_0057.power;
        interface link_0057_0058 : Common::PowerLink connect
            source ::> controller_0057.supply to sink ::> controller_0058.power;
        interface link_0058_0059 : Common::PowerLink connect
            source ::> controller_0058.supply to sink ::> controller_0059.power;
        interface link_0059_0060 : Common::PowerLink connect
            source ::> controller_0059.supply to sink ::> controller_0060.power;
        interface link_0060_0061 : Common::PowerLink connect
            source ::> controller_0060.supply to sink ::> controller_0061.power;
        interface link_0061_0062 : Common::PowerLink connect
            source ::> controller_0061.supply to sink ::> controller_0062.power;
        interface link_0062_0063 : Common::PowerLink connect
            source ::> controller_0062.supply to sink ::> controller_0063.power;
        interface link_0063_0064 : Common::PowerLink connect
            source ::> controller_0063.supply to sink ::> controller_0064.power;
        interface link_0064_0065 : Common::PowerLink connect
            source ::> controller_0064.supply to sink ::> controller_0065.power;
        interface link_0065_0066 : Common::PowerLink connect
            source ::> controller_0065.supply to sink ::> controller_0066.power;
        interface link_0066_0067 : Common::PowerLink connect
            source ::> controller_0066.supply to sink ::> controller_0067.power;
        interface link_0067_0068 : Common::PowerLink connect
            source ::> controller_0067.supply to sink ::> controller_0068.power;
        interface link_0068_0069 : Common::PowerLink connect
            source ::> controller_0068.supply to sink ::> controller_0069.power;
        interface link_0069_0070 : Common::PowerLink connect
            source ::> controller_0069.supply to sink ::> controller_0070.power;
        interface link_0070_0071 : Common::PowerLink connect
            source ::> controller_0070.supply to sink ::> controller_0071.power;
        interface link_0071_0072 : Common::PowerLink connect
            source ::> controller_0071.supply to sink ::> controller_0072.power;
        interface link_0072_0073 : Common::PowerLink connect
            source ::> controller_0072.supply to sink ::> controller_0073.power;
        interface link_0073_0074 : Common::PowerLink connect
            source ::> controller_0073.supply to sink ::> controller_0074.power;
        interface link_0074_0075 : Common::PowerLink connect
            source ::> controller_0074.supply to sink ::> controller_0075.power;
        interface link_0075_0076 : Common::PowerLink connect
            source ::> controller_0075.supply to sink ::> controller_0076.power;
        interface link_0076_0077 : Common::PowerLink connect
            source ::> controller_0076.supply to sink ::> controller_0077.power;
        interface link_0077_0078 : Common::PowerLink connect
            source ::> controller_0077.supply to sink ::> controller_0078.power;
        interface link_0078_0079 : Common::PowerLink connect
            source ::> controller_0078.supply to sink ::> controller_0079.power;
        interface link_0079_0080 : Common::PowerLink connect
            source ::> controller_0079.supply to sink ::> controller_0080.power;
        interface link_0080_0081 : Common::PowerLink connect
            source ::> controller_0080.supply to sink ::> controller_0081.power;
        interface link_0081_0082 : Common::PowerLink connect
            source ::> controller_0081.supply to sink ::> controller_0082.power;
        interface link_0082_0083 : Common::PowerLink connect
            source ::> controller_0082.supply to sink ::> controller_0083.power;
        interface link_0083_0084 : Common::PowerLink connect
            source ::> controller_0083.supply to sink ::> controller_0084.power;
        interface link_0084_0085 : Common::PowerLink connect
            source ::> controller_0084.supply to sink ::> controller_0085.power;
        interface link_0085_0086 : Common::PowerLink connect
            source ::> controller_0085.supply to sink ::> controller_0086.power;
        interface link_0086_0087 : Common::PowerLink connect
            source ::> controller_0086.supply to sink ::> controller_0087.power;
        interface link_0087_0088 : Common::PowerLink connect
            source ::> controller_0087.supply to sink ::> controller_0088.power;
        interface link_0088_0089 : Common::PowerLink connect
            source ::> controller_0088.supply to sink ::> controller_0089.power;
        interface link_0089_0090 : Common::PowerLink connect
            source ::> controller_0089.supply to sink ::> controller_0090.power;
        interface link_0090_0091 : Common::PowerLink connect
            source ::> controller_0090.supply to sink ::> controller_0091.power;
        interface link_0091_0092 : Common::PowerLink connect
            source ::> controller_0091.supply to sink ::> controller_0092.power;
        interface link_0092_0093 : Common::PowerLink connect
            source ::> controller_0092.supply to sink ::> controller_0093.power;
        interface link_0093_0094 : Common::PowerLink connect
            source ::> controller_0093.supply to sink ::> controller_0094.power;
        interface link_0094_0095 : Common::PowerLink connect
            source ::> controller_0094.supply to sink ::> controller_0095.power;
        interface link_0095_0096 : Common::PowerLink connect
            source ::> controller_0095.supply to sink ::> controller_0096.power;
        interface link_0096_0097 : Common::PowerLink connect
            source ::> controller_0096.supply to sink ::> controller_0097.power;
        interface link_0097_0098 : Common::PowerLink connect
            source ::> controller_0097.supply to sink ::> controller_0098.power;
        interface link_0098_0099 : Common::PowerLink connect
            source ::> controller_0098.supply to sink ::> controller_0099.power;
        interface link_0099_0100 : Common::PowerLink connect
            source ::> controller_0099.supply to sink ::> controller_0100.power;
        interface link_0100_0101 : Common::PowerLink connect
            source ::> controller_0100.supply to sink ::> controller_0101.power;
        interface link_0101_0102 : Common::PowerLink connect
            source ::> controller_0101.supply to sink ::> controller_0102.power;
        interface link_0102_0103 : Common::PowerLink connect
            source ::> controller_0102.supply to sink ::> controller_0103.power;
        interface link_0103_0104 : Common::PowerLink connect
            source ::> controller_0103.supply to sink ::> controller_0104.power;
        interface link_0104_0105 : Common::PowerLink connect
            source ::> controller_0104.supply to sink ::> controller_0105.power;
        interface link_0105_0106 : Common::PowerLink connect
            source ::> controller_0105.supply to sink ::> controller_0106.power;
        interface link_0106_0107 : Common::PowerLink connect
            source ::> controller_0106.supply to sink ::> controller_0107.power;
        interface link_0107_0108 : Common::PowerLink connect
            source ::> controller_0107.supply to sink ::> controller_0108.power;
        interface link_0108_0109 : Common::PowerLink connect
            source ::> controller_0108.supply to sink ::> controller_0109.power;
        interface link_0109_0110 : Common::PowerLink connect
            source ::> controller_0109.supply to sink ::> controller_0110.power;
        interface link_0110_0111 : Common::PowerLink connect
            source ::> controller_0110.supply to sink ::> controller_0111.power;
        interface link_0111_0112 : Common::PowerLink connect
            source ::> controller_0111.supply to sink ::> controller_0112.power;
        interface link_0112_0113 : Common::PowerLink connect
            source ::> controller_0112.supply to sink ::> controller_0113.power;
        interface link_0113_0114 : Common::PowerLink connect
            source ::> controller_0113.supply to sink ::> controller_0114.power;
        interface link_0114_0115 : Common::PowerLink connect
            source ::> controller_0114.supply to sink ::> controller_0115.power;
        interface link_0115_0116 : Common::PowerLink connect
            source ::> controller_0115.supply to sink ::> controller_0116.power;
        interface link_0116_0117 : Common::PowerLink connect
            source ::> controller_0116.supply to sink ::> controller_0117.power;
        interface link_0117_0118 : Common::PowerLink connect
            source ::> controller_0117.supply to sink ::> controller_0118.power;
        interface link_0118_0119 : Common::PowerLink connect
            source ::> controller_0118.supply to sink ::> controller_0119.power;
        interface link_0119_0120 : Common::PowerLink connect
            source ::> controller_0119.supply to sink ::> controller_0120.power;
        interface link_0120_0121 : Common::PowerLink connect
            source ::> controller_0120.supply to sink ::> controller_0121.power;
        interface link_0121_0122 : Common::PowerLink connect
            source ::> controller_0121.supply to sink ::> controller_0122.power;
        interface link_0122_0123 : Common::PowerLink connect
            source ::> controller_0122.supply to sink ::> controller_0123.power;
        interface link_0123_0124 : Common::PowerLink connect
            source ::> controller_0123.supply to sink ::> controller_0124.power;
        interface link_0124_0125 : Common::PowerLink connect
            source ::> controller_0124.supply to sink ::> controller_0125.power;
        interface link_0125_0126 : Common::PowerLink connect
            source ::> controller_0125.supply to sink ::> controller_0126.power;
        interface link_0126_0127 : Common::PowerLink connect
            source ::> controller_0126.supply to sink ::> controller_0127.power;
        interface link_0127_0128 : Common::PowerLink connect
            source ::> controller_0127.supply to sink ::> controller_0128.power;
        interface link_0128_0129 : Common::PowerLink connect
            source ::> controller_0128.supply to sink ::> controller_0129.power;
        interface link_0129_0130 : Common::PowerLink connect
            source ::> controller_0129.supply to sink ::> controller_0130.power;
        interface link_0130_0131 : Common::PowerLink connect
            source ::> controller_0130.supply to sink ::> controller_0131.power;
        interface link_0131_0132 : Common::PowerLink connect
            source ::> controller_0131.supply to sink ::> controller_0132.power;
        interface link_0132_0133 : Common::PowerLink connect
            source ::> controller_0132.supply to sink ::> controller_0133.power;
        interface link_0133_0134 : Common::PowerLink connect
            source ::> controller_0133.supply to sink ::> controller_0134.power;
        interface link_0134_0135 : Common::PowerLink connect
            source ::> controller_0134.supply to sink ::> controller_0135.power;
        interface link_0135_0136 : Common::PowerLink connect
            source ::> controller_0135.supply to sink ::> controller_0136.power;
        interface link_0136_0137 : Common::PowerLink connect
            source ::> controller_0136.supply to sink ::> controller_0137.power;
        interface link_0137_0138 : Common::PowerLink connect
            source ::> controller_0137.supply to sink ::> controller_0138.power;
        interface link_0138_0139 : Common::PowerLink connect
            source ::> controller_0138.supply to sink ::> controller_0139.power;
        interface link_0139_0140 : Common::PowerLink connect
            source ::> controller_0139.supply to sink ::> controller_0140.power;
        interface link_0140_0141 : Common::PowerLink connect
            source ::> controller_0140.supply to sink ::> controller_0141.power;
        interface link_0141_0142 : Common::PowerLink connect
            source ::> controller_0141.supply to sink ::> controller_0142.power;
        interface link_0142_0143 : Common::PowerLink connect
            source ::> controller_0142.supply to sink ::> controller_0143.power;
        interface link_0143_0144 : Common::PowerLink connect
            source ::> controller_0143.supply to sink ::> controller_0144.power;
        interface link_0144_0145 : Common::PowerLink connect
            source ::> controller_0144.supply to sink ::> controller_0145.power;
        interface link_0145_0146 : Common::PowerLink connect
            source ::> controller_0145.supply to sink ::> controller_0146.power;
        interface link_0146_0147 : Common::PowerLink connect
            source ::> controller_0146.supply to sink ::> controller_0147.power;
        interface link_0147_0148 : Common::PowerLink connect
            source ::> controller_0147.supply to sink ::> controller_0148.power;
        interface link_0148_0149 : Common::PowerLink connect
            source ::> controller_0148.supply to sink ::> controller_0149.power;
        interface link_0149_0150 : Common::PowerLink connect
            source ::> controller_0149.supply to sink ::> controller_0150.power;
        interface link_0150_0151 : Common::PowerLink connect
            source ::> controller_0150.supply to sink ::> controller_0151.power;
        interface link_0151_0152 : Common::PowerLink connect
            source ::> controller_0151.supply to sink ::> controller_0152.power;
        interface link_0152_0153 : Common::PowerLink connect
            source ::> controller_0152.supply to sink ::> controller_0153.power;
        interface link_0153_0154 : Common::PowerLink connect
            source ::> controller_0153.supply to sink ::> controller_0154.power;
        interface link_0154_0155 : Common::PowerLink connect
            source ::> controller_0154.supply to sink ::> controller_0155.power;
        interface link_0155_0156 : Common::PowerLink connect
            source ::> controller_0155.supply to sink ::> controller_0156.power;
        interface link_0156_0157 : Common::PowerLink connect
            source ::> controller_0156.supply to sink ::> controller_0157.power;
        interface link_0157_0158 : Common::PowerLink connect
            source ::> controller_0157.supply to sink ::> controller_0158.power;
        interface link_0158_0159 : Common::PowerLink connect
            source ::> controller_0158.supply to sink ::> controller_0159.power;
        interface link_0159_0160 : Common::PowerLink connect
            source ::> controller_0159.supply to sink ::> controller_0160.power;
        interface link_0160_0161 : Common::PowerLink connect
            source ::> controller_0160.supply to sink ::> controller_0161.power;
        interface link_0161_0162 : Common::PowerLink connect
            source ::> controller_0161.supply to sink ::> controller_0162.power;
        interface link_0162_0163 : Common::PowerLink connect
            source ::> controller_0162.supply to sink ::> controller_0163.power;
        interface link_0163_0164 : Common::PowerLink connect
            source ::> controller_0163.supply to sink ::> controller_0164.power;
        interface link_0164_0165 : Common::PowerLink connect
            source ::> controller_0164.supply to sink ::> controller_0165.power;
        interface link_0165_0166 : Common::PowerLink connect
            source ::> controller_0165.supply to sink ::> controller_0166.power;
        interface link_0166_0167 : Common::PowerLink connect
            source ::> controller_0166.supply to sink ::> controller_0167.power;
        interface link_0167_0168 : Common::PowerLink connect
            source ::> controller_0167.supply to sink ::> controller_0168.power;
        interface link_0168_0169 : Common::PowerLink connect
            source ::> controller_0168.supply to sink ::> controller_0169.power;
        interface link_0169_0170 : Common::PowerLink connect
            source ::> controller_0169.supply to sink ::> controller_0170.power;
        interface link_0170_0171 : Common::PowerLink connect
            source ::> controller_0170.supply to sink ::> controller_0171.power;
        interface link_0171_0172 : Common::PowerLink connect
            source ::> controller_0171.supply to sink ::> controller_0172.power;
        interface link_0172_0173 : Common::PowerLink connect
            source ::> controller_0172.supply to sink ::> controller_0173.power;
        interface link_0173_0174 : Common::PowerLink connect
            source ::> controller_0173.supply to sink ::> controller_0174.power;
        interface link_0174_0175 : Common::PowerLink connect
            source ::> controller_0174.supply to sink ::> controller_0175.power;
        interface link_0175_0176 : Common::PowerLink connect
            source ::> controller_0175.supply to sink ::> controller_0176.power;
        interface link_0176_0177 : Common::PowerLink connect
            source ::> controller_0176.supply to sink ::> controller_0177.power;
        interface link_0177_0178 : Common::PowerLink connect
            source ::> controller_0177.supply to sink ::> controller_0178.power;
        interface link_0178_0179 : Common::PowerLink connect
            source ::> controller_0178.supply to sink ::> controller_0179.power;
        interface link_0179_0180 : Common::PowerLink connect
            source ::> controller_0179.supply to sink ::> controller_0180.power;
        interface link_0180_0181 : Common::PowerLink connect
            source ::> controller_0180.supply to sink ::> controller_0181.power;
        interface link_0181_0182 : Common::PowerLink connect
            source ::> controller_0181.supply to sink ::> controller_0182.power;
        interface link_0182_0183 : Common::PowerLink connect
            source ::> controller_0182.supply to sink ::> controller_0183.power;
        interface link_0183_0184 : Common::PowerLink connect
            source ::> controller_0183.supply to sink ::> controller_0184.power;
        interface link_0184_0185 : Common::PowerLink connect
            source ::> controller_0184.supply to sink ::> controller_0185.power;
        interface link_0185_0186 : Common::PowerLink connect
            source ::> controller_0185.supply to sink ::> controller_0186.power;
        interface link_0186_0187 : Common::PowerLink connect
            source ::> controller_0186.supply to sink ::> controller_0187.power;
        interface link_0187_0188 : Common::PowerLink connect
            source ::> controller_0187.supply to sink ::> controller_0188.power;
        interface link_0188_0189 : Common::PowerLink connect
            source ::> controller_0188.supply to sink ::> controller_0189.power;
        interface link_0189_0190 : Common::PowerLink connect
            source ::> controller_0189.supply to sink ::> controller_0190.power;
        interface link_0190_0191 : Common::PowerLink connect
            source ::> controller_0190.supply to sink ::> controller_0191.power;
        interface link_0191_0192 : Common::PowerLink connect
            source ::> controller_0191.supply to sink ::> controller_0192.power;
        interface link_0192_0193 : Common::PowerLink connect
            source ::> controller_0192.supply to sink ::> controller_0193.power;
        interface link_0193_0194 : Common::PowerLink connect
            source ::> controller_0193.supply to sink ::> controller_0194.power;
        interface link_0194_0195 : Common::PowerLink connect
            source ::> controller_0194.supply to sink ::> controller_0195.power;
        interface link_0195_0196 : Common::PowerLink connect
            source ::> controller_0195.supply to sink ::> controller_0196.power;
        interface link_0196_0197 : Common::PowerLink connect
            source ::> controller_0196.supply to sink ::> controller_0197.power;
        interface link_0197_0198 : Common::PowerLink connect
            source ::> controller_0197.supply to sink ::> controller_0198.power;
        interface link_0198_0199 : Common::PowerLink connect
            source ::> controller_0198.supply to sink ::> controller_0199.power;
        interface link_0199_0200 : Common::PowerLink connect
            source ::> controller_0199.supply to sink ::> controller_0200.power;
        attribute totalMass : MassValue = Common::TotalMass((controller_0001.mass, controller_0002.mass, controller_0003.mass, controller_0004.mass, controller_0005.mass, controller_0006.mass, controller_0007.mass, controller_0008.mass));
    }
    part system : System;
}

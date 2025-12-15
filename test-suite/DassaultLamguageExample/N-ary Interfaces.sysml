package 'N-ary Interfaces' {
    private import ScalarValues::*;
    private import ISQ::*;
    port def CANBusPort {
        inout attribute id : Integer;
        inout attribute data : String;
    }
    part def eVehicle {
        part batteryPack {
            part cell [100];
            port temperatureSensorPort;
            port voltageOutput;
            port currentOutput;
        }
        part motorController [4] {
            port motorPowerInput;
            port motorControlSignal;
            port canInterface : CANBusPort;
        }
        part wheelMotor [4] {
            port powerInput;
            port controlInput;
            port canInterface : CANBusPort;
        }
        part centralControlUnit {
            port controlOutput [4];
            port batteryStatusInput;
            port motorStatusInput [4];
            port canInterface : CANBusPort;
        }
        part temperatureSensor [20] {
            port sensorOutput;
            port canInterface : CANBusPort;
        }
        part voltageSensor [4] {
            port sensorOutput;
            port canInterface : CANBusPort;
        }
        part currentSensor [4] {
            port sensorOutput;
            port canInterface : CANBusPort;
        }
        part infotainmentSystem {
            port userInterface;
            port canInterface : CANBusPort;
        }
        part hvacSystem {
            port hvacControl;
            port temperatureInput;
            port canInterface : CANBusPort;
        }
        interface vehicleCANBus
                connect (centralControlUnit.canInterface, motorController.canInterface, wheelMotor.canInterface, temperatureSensor.canInterface, voltageSensor.canInterface, currentSensor.canInterface, infotainmentSystem.canInterface, hvacSystem.canInterface);

        interface batteryPack.voltageOutput to centralControlUnit.batteryStatusInput;
        interface centralControlUnit.controlOutput to motorController.motorControlSignal;
        interface motorController.motorPowerInput to wheelMotor.powerInput;
        interface temperatureSensor.sensorOutput to batteryPack.temperatureSensorPort;
        interface voltageSensor.sensorOutput to centralControlUnit.batteryStatusInput;
        interface currentSensor.sensorOutput to centralControlUnit.batteryStatusInput;
        interface hvacSystem.hvacControl to [0] centralControlUnit.controlOutput;
        view 'n-ary interfaces' : DS_Views::SymbolicViews::gv;
        /* The CANBusInterface serves as an n-ary interface that interconnects all subsystems equipped with a CANBusPort, enabling multipoint
         * communications throughout the vehicle. This setup allows for the centralized control unit, motor controllers, sensors, and other 
         * components to exchange information efficiently, enhancing the vehicle's operational efficiency and responsiveness.*/
    }
    attribute def VoltageValue;
    attribute def CurrentValue;
    attribute def Binary;
}

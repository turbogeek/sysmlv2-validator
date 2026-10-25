package OccurrenceTests;

private import ScalarValues::*;

// Test occurrence, individual, snapshot, and timeslice

occurrence def Event {
    attribute timestamp : Real;
}

individual def SpecificVehicle :> Vehicle;

part def Vehicle {
    attribute vin : String;
}

// SysML v2 has 'snapshot' and 'timeslice' usages (portions) but no 'snapshot def' or 'timeslice def';
// the portions below are typed by part definitions.
part def VehicleSnapshot :> Vehicle {
    attribute captureTime : Real;
}

part def VehicleTimeslice :> Vehicle {
    attribute startTime : Real;
    attribute endTime : Real;
}

// Individual instance
individual myCarIndividual : SpecificVehicle {
    attribute vin = "1HGBH41JXMN109186";
}

// Snapshot at specific time
snapshot myCar_t1 : VehicleSnapshot {
    attribute vin = "1HGBH41JXMN109186";
    attribute captureTime = 100.0;
}

// Timeslice over period
timeslice myCar_t1_t2 : VehicleTimeslice {
    attribute vin = "1HGBH41JXMN109186";
    attribute startTime = 100.0;
    attribute endTime = 200.0;
}

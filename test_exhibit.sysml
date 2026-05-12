package Test {
    part def Vehicle {
        attribute mass;
    }
    part vehicle_a:Vehicle{
        port vehicleToRoadPort redefines vehicleToRoadPort{
            port wheelToRoadPort1:WheelToRoadPort;
        }
        perform ActionTree::providePower redefines providePower;
    }
}

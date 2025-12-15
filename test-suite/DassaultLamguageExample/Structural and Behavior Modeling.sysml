package 'Structural and Behavior Modeling' {
    public import ScalarValues::*;
    part 'vehicle in use' {
        part user {
            port setSpeedPort : SetSpeedPort;
            perform 'Structural and Behavior Modeling'::activateAutopilot.setSpeed;
            ref bind setSpeed.speed = setSpeedPort.speed;
        }
        part vehicle {
            perform 'Structural and Behavior Modeling'::activateAutopilot.adjustSpeed;
            port setSpeedPort : SetSpeedPort;
            ref bind adjustSpeed.speed = setSpeedPort.speed;
        }
        interface user.setSpeedPort to vehicle.setSpeedPort;
        perform activateAutopilot;
        view 'structural and behavior in one view' : DS_Views::SymbolicViews::gv;
    }
    action activateAutopilot {
        action setSpeed { out speed : Integer; }
        action adjustSpeed { in speed : Integer; }
        ref succession flow setSpeed.speed to adjustSpeed.speed;
    }
    port def SetSpeedPort {
        attribute speed : Integer;
    }
}

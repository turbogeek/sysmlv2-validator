package Import {
    package '00 Membership Visibility' {
        public import 'Drone Parts'::*;
        part drone_v1 {
            part :> battery;
        }
    }
    package '01 Membership Import' {
        public import 'Drone Parts';
    }
    package '02 Namespace Import' {
        public import 'Drone Parts'::*;
    }
    package '03 Recursive Membership Import' {
        public import 'Drone Parts'::**;
    }
    package '04 Recursive Namespace Import' {
        public import 'Drone Parts'::*::**;
    }
    package '05 Import All' {
        public import all 'Drone Parts'::*;
    }
    package '06 Private Import' {
        private import 'Flight Control and Navigation'::*;
    }
    package '07 Public Import' {
        private import Communication::*;
    }
    package 'Drone Parts' {
        part battery {
            attribute voltage;
            protected attribute capacity;
            private attribute weight;
        }
        protected part body;
        private part motor;
    }
    package Communication {
        public import 'Camera and Sensing'::*;
        part transmitter;
        part antenna;
        part receiver;
    }
    package 'Camera and Sensing' {
        part camera;
        part sensor;
        part gimbals;
    }
    package 'Flight Control and Navigation' {
        private import 'Camera and Sensing'::*;
        part 'flight controller';
        part 'GPS module';
    }
    view 'import modelling' : DS_Views::SymbolicViews::gv;
}

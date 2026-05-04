package Actions_8Trigger {
    private import ScalarValues::*;
    private import SysML::*;
    action def Main {     
        accept tick : Tick via clock;
        accept when t > 10.0;
        doc /* Event trigger with guard */
    }
    view ExposeExample { expose Main; }
}

package Actions_8Trigger {
	import MyLibrary;
    private import ScalarValues::*;
    private import SysML::*;
    action def Main {
        action def Tick;
        part clock{
		attribute time:Real;
		attribute part :Real;
		}
        attribute t : Real;
        accept tick : Tick via clock;
        accept when t > 10.0;
        doc /* Event trigger with guard */
    }
    view ExposeExample { expose Main; }
}
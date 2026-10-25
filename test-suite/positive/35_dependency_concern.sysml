package DependencyTests;

package LibraryPackage {
    part def LibraryComponent;
}

package ApplicationPackage {
    private import LibraryPackage::*;
    
    part def Application {
        part lib : LibraryComponent;
    }
}

dependency ApplicationPackage to LibraryPackage;

concern 'Maintainability' {
    doc /* System must be easy to maintain and extend */
}

concern 'Testability' {
    subject app : ApplicationPackage::Application;
    doc /* Application must be testable in isolation */
}

viewpoint def MaintenanceViewpoint {
    concern 'Maintainability';
    concern 'Testability';
}

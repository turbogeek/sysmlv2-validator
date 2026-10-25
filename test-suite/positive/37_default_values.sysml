package DefaultValueTests;

private import ScalarValues::*;

part def Component {
    // Bound value with = (a redefinition cannot override it)
    attribute count : Integer = 10;
    
    // Default value with default keyword
    attribute name : String default "Component";
    
    // Default with :=
    attribute status : String := "Active";
}

part def System {
    part component : Component {
        // Override the default
        attribute :>> name = "Subcomponent";
    }
}

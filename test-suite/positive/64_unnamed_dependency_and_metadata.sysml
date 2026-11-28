package UnnamedDependencyAndMetadata {
    // SysML v2 8.2.2.3: a dependency's name is optional, and a name needs 'from'
    part a;
    part b;
    part c;
    dependency a to b;
    dependency a, c to b;
    dependency uses from a to b, c;
    dependency <u2> from c to a;

    // SysML v2 8.2.2.27 / 7.27.2: a metadata usage's name and ':' may be omitted
    metadata def Approval {
        attribute approved : ScalarValues::Boolean;
    }
    part lab {
        part fridge;
    }
    metadata Approval about lab::fridge {
        :>> approved = true;
    }
    metadata check : Approval about lab {
        :>> approved = false;
    }
    metadata check2 typed by Approval about a;
}

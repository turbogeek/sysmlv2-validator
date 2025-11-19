/**
 * NEGATIVE TEST: Incompatible port connection
 * EXPECTED ERROR: Cannot connect incompatible port types
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    port def IntegerPort {
        attribute value : Integer;
    }

    port def StringPort {
        attribute text : String;
    }

    part def TestComponent {
        port p1 : IntegerPort;
        port p2 : StringPort;

        connect p1 to p2;  // ERROR: Incompatible port types
    }
}

/**
 * NEGATIVE TEST: Reference to undefined port
 * EXPECTED ERROR: Cannot resolve port reference
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    part def Component {
        port input;

        connect input to undefinedPort;  // ERROR: undefinedPort not defined
    }
}

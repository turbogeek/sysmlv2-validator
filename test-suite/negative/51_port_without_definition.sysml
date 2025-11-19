/**
 * NEGATIVE TEST: Port usage without port definition
 * EXPECTED ERROR: Port type not defined
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    part def Component {
        port dataPort : UndefinedPortType;  // ERROR: UndefinedPortType not defined
    }
}

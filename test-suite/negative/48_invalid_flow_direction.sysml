/**
 * NEGATIVE TEST: Invalid flow direction (out to in)
 * EXPECTED ERROR: Invalid flow direction
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    action def InvalidFlow {
        action producer {
            in input;  // This is an input
        }

        action consumer {
            out output;  // This is an output
        }

        flow from consumer.output to producer.input;  // ERROR: Cannot flow from out to in
    }
}

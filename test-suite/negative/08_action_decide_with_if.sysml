/**
 * NEGATIVE TEST: Combined 'action decide' and 'action if'
 * EXPECTED ERROR: Both "action decide" and "action if" errors
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def DecisionFlow {
        in choice : String;

        action decide selection {  // ERROR
            action if (choice == "A") optionA {  // ERROR
                out result1;
            }
            action if (choice == "B") optionB {  // ERROR
                out result2;
            }
        }
    }
}

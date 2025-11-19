/**
 * NEGATIVE TEST: All four invalid action modifiers in one file
 * EXPECTED ERROR: 4 separate invalid action syntax errors
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def AllInvalidModifiers {
        action parallel p1 {  // ERROR
            out x;
        }
        action then t1 {  // ERROR
            in y;
        }
        action decide d1 {  // ERROR
            in z;
        }
        action if (true) i1 {  // ERROR
            out w;
        }
    }
}

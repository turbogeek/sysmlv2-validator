/**
 * NEGATIVE TEST: Original BreakfastExample with all invalid action syntax
 * EXPECTED ERRORS: 7 errors total
 *   - 3x "Invalid syntax: 'action parallel'"
 *   - 1x "Invalid syntax: 'action then'"
 *   - 1x "Invalid syntax: 'action decide'"
 *   - 2x "Invalid syntax: 'action if'"
 * CATEGORY: Invalid Action Syntax (Real-world example)
 *
 * This is the original BreakfastExample.sysml that demonstrated
 * all the common invalid action syntax patterns.
 */

/**
 * Breakfast Preparation Example
 * Demonstrates SysML v2 compact flow notation in action definitions
 */

package BreakfastExample {
    private import ScalarValues::*;
    private import Time::*;

    /**
     * Simple data type definitions
     */
    attribute def Ingredient {
        doc /* Represents a food ingredient */
    }

    attribute def Meal {
        doc /* Represents a complete prepared meal */
    }

    attribute def Temperature {
        doc /* Temperature in degrees */
    }

    attribute def Duration {
        doc /* Time duration */
    }

    /**
     * Basic breakfast preparation with compact flow notation
     */
    action def MakeBreakfast {
        doc /* Prepare a complete breakfast with eggs, toast, and coffee */

        // Input items
        in eggs : Ingredient;
        in bread : Ingredient;
        in coffeeGrounds : Ingredient;
        in water : Ingredient;

        // Output
        out breakfast : Meal;

        // Sub-actions with compact flow notation
        action crackEggs {
            in rawEggs = eggs;
            out crackedEggs : Ingredient;
        }

        action cookEggs {
            in preparedEggs = crackEggs.crackedEggs;  // ← Compact flow!
            out scrambledEggs : Ingredient;
        }

        action toastBread {
            in slicedBread = bread;
            out toast : Ingredient;
        }

        action brewCoffee {
            in grounds = coffeeGrounds;
            in hotWater = water;
            out coffee : Ingredient;
        }

        action plateFood {
            in eggs = cookEggs.scrambledEggs;        // ← Compact flow!
            in toastedBread = toastBread.toast;      // ← Compact flow!
            in brewedCoffee = brewCoffee.coffee;     // ← Compact flow!
            out completeMeal = breakfast;            // ← Compact flow!
        }
    }

    /**
     * Parallel breakfast preparation
     */
    action def MakeBreakfastParallel {
        doc /* Prepare breakfast with parallel cooking processes */

        in ingredients : Ingredient[4];
        out meal : Meal;

        // These actions can happen in parallel
        action parallel cookProtein {  // ERROR: 'action parallel' invalid
            in eggs = ingredients#(0);
            out protein : Ingredient;
        }

        action parallel cookCarbs {  // ERROR: 'action parallel' invalid
            in bread = ingredients#(1);
            out carbs : Ingredient;
        }

        action parallel makeBeverage {  // ERROR: 'action parallel' invalid
            in coffee = ingredients#(2);
            in water = ingredients#(3);
            out beverage : Ingredient;
        }

        // Final action combines results
        action then combine {  // ERROR: 'action then' invalid
            in p = cookProtein.protein;     // ← Compact flow
            in c = cookCarbs.carbs;         // ← Compact flow
            in b = makeBeverage.beverage;   // ← Compact flow
            out completeMeal = meal;
        }
    }

    /**
     * Breakfast with cooking choices based on preferences
     */
    action def MakeBreakfastWithChoices {
        doc /* Breakfast with cooking choices based on preferences */

        in preference : String;
        in ingredients : Ingredient[5];
        out meal : Meal;

        // First action
        action checkPreference {
            in pref = preference;
            out eggStyle : String;
            out breadType : String;
        }

        // Decision: scrambled or fried?
        action decide scrambledOrFried {  // ERROR: 'action decide' invalid
            in style = checkPreference.eggStyle;  // ← Compact flow

            action if (style == "scrambled") scrambleEggs {  // ERROR: 'action if' invalid
                in rawEggs = ingredients#(0);
                out cookedEggs : Ingredient;
            }

            action if (style == "fried") fryEggs {  // ERROR: 'action if' invalid
                in rawEggs = ingredients#(0);
                out cookedEggs : Ingredient;
            }
        }

        // Toast preparation
        action toastBread {
            in bread = ingredients#(1);
            in toastLevel = checkPreference.breadType;  // ← Compact flow
            out toast : Ingredient;
        }

        // Combine results
        action serveMeal {
            in eggs = scrambledOrFried.cookedEggs;  // ← Compact flow
            in toast = toastBread.toast;            // ← Compact flow
            out completeMeal = meal;
        }
    }

    /**
     * Ultra-compact example with minimal syntax
     */
    action def QuickBreakfast {
        doc /* Quick breakfast preparation with one-liner flows */

        in eggs : Ingredient;
        in bread : Ingredient;
        out meal : Meal;

        action crack {
            in e = eggs;
            out c : Ingredient;
        }

        action cook {
            in c = crack.c;
            out s : Ingredient;
        }

        action toast {
            in b = bread;
            out t : Ingredient;
        }

        action serve {
            in s = cook.s;
            in t = toast.t;
            out m = meal;
        }
    }

    /**
     * Example usage of breakfast actions
     */
    package BreakfastUsageExamples {

        // Usage instance 1: Basic breakfast
        action basicBreakfast : MakeBreakfast {
            doc /* Prepare a standard breakfast */
        }

        // Usage instance 2: Parallel cooking
        action parallelBreakfast : MakeBreakfastParallel {
            doc /* Prepare breakfast with parallel tasks */
        }

        // Usage instance 3: Customized breakfast
        action customBreakfast : MakeBreakfastWithChoices {
            doc /* Prepare breakfast based on user preferences */
        }

        // Usage instance 4: Quick breakfast
        action quickBreakfast : QuickBreakfast {
            doc /* Prepare a quick breakfast */
        }
    }
}

/**
 * POSITIVE TEST: State machine with transitions
 * EXPECTED: No errors
 * CATEGORY: State Machines
 */
package PositiveTests {
    private import ScalarValues::*;
    private import SI::s;

    state def TrafficLightStates {
        entry; then red;

        state red;
        state yellow;
        state green;

        transition red_to_green
            first red
            accept after 30 [s]
            then green;

        transition green_to_yellow
            first green
            accept after 45 [s]
            then yellow;

        transition yellow_to_red
            first yellow
            accept after 5 [s]
            then red;
    }
}

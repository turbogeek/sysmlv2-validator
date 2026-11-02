package com.validator.semantic.index;

import com.validator.ValidationError;
import com.validator.parser.SysMLv2ParserFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Signal names in state machines: 'accept S' types the payload by S, 'send new S() via p' names the signal type and
 * the port. All three were unchecked: 'accept S' declared a payload named S, and 'new S()' and 'via p' were not
 * reference sites.
 */
@DisplayName("Signal and port references")
class SignalReferenceTest {

    private static final SysMLv2ParserFacade PARSER = new SysMLv2ParserFacade();

    @Test
    @DisplayName("Misspelled signals and ports are reported, correct ones are not")
    void signalsAndPorts() {
        List<ValidationError> errors = check("""
            package P {
                item def Start;
                port def Out { out item start : Start; }
                part def Timer {
                    port timerOut : Out;
                    exhibit state s {
                        state idle;
                        state ticking { entry send new Start() via timerOut; }
                        transition first start then idle;
                        transition first idle accept Start then ticking;
                        transition first ticking accept Stopp then idle;
                        transition first idle accept after 1 [s] do send new Startt() via timerOutt then idle;
                    }
                }
            }
            """);
        assertEquals(3, errors.size(), errors.toString());
        String all = errors.toString();
        assertTrue(all.contains("Stopp") && all.contains("Startt") && all.contains("timerOutt"), all);
    }

    @Test
    @DisplayName("'exhibit state s { }' declares s; 'exhibit s;' references it")
    void exhibitDeclaresWithKeyword() {
        List<ValidationError> errors = check("""
            package P {
                state def S;
                part def A { exhibit state s : S { state x; } }
                part def B :> A { exhibit s; }
                part def C { exhibit missingState; }
            }
            """);
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).getMessage().contains("missingState"), errors.toString());
    }

    private static List<ValidationError> check(String text) {
        ModelIndexBuilder.Result built = ModelIndexBuilder.build(PARSER.parseString(text, "M.sysml").getParseTree(), "M.sysml", true);
        ModelIndex index = new ModelIndex();
        index.addAll(built.getElements());
        return ReferenceChecker.check(built.getSites(), index, "M.sysml");
    }
}

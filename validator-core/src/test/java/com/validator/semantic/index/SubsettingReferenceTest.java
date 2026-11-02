package com.validator.semantic.index;

import com.validator.ValidationError;
import com.validator.parser.SysMLv2ParserFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A usage's subsetting (':>', 'subsets') and reference subsetting ('::>', 'references') name a feature. They were
 * not checked, so a '#derivation connection' whose end referenced a misspelled requirement validated. A name a
 * library declares is not reported (the usage may inherit it from its implicit base type, e.g. a flow's source).
 */
@DisplayName("Subsetted and referenced features")
class SubsettingReferenceTest {

    private static final SysMLv2ParserFacade PARSER = new SysMLv2ParserFacade();

    @Test
    @DisplayName("Derivation ends and subsettings that do not resolve are reported")
    void derivationEnds() {
        List<ValidationError> errors = check("""
            package D {
                requirement a;
                requirement <'b-1'> bb;
                part parts[*];
                part p :> parts;
                connection { end ::> a; end ::> 'b-1'; }
                connection { end ::> aX; end ::> 'b-9'; }
                part q :> partz;
            }
            """);
        assertEquals(3, errors.size(), errors.toString());
        String all = errors.toString();
        assertTrue(all.contains("aX") && all.contains("b-9") && all.contains("partz"), all);
    }

    private static List<ValidationError> check(String text) {
        ModelIndexBuilder.Result built = ModelIndexBuilder.build(PARSER.parseString(text, "M.sysml").getParseTree(), "M.sysml", true);
        ModelIndex index = new ModelIndex();
        index.addAll(built.getElements());
        return ReferenceChecker.check(built.getSites(), index, "M.sysml");
    }
}

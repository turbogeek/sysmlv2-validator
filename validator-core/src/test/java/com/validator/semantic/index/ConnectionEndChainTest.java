package com.validator.semantic.index;

import com.validator.ValidationError;
import com.validator.parser.SysMLv2ParserFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Connection and interface ends: 'connect a.p to b.q' names a feature 'a' and then features of its type. The
 * names after the first were unchecked. They are checked only where the type is certain: the leading feature is
 * declared in an enclosing namespace (not inherited, where an unnamed ':>> a : T' may narrow its type) and every
 * element of the chain is typed by plain names. Succession ends ('first start then x') are not checked here.
 */
@DisplayName("Connection end feature chains")
class ConnectionEndChainTest {

    private static final SysMLv2ParserFacade PARSER = new SysMLv2ParserFacade();

    @Test
    @DisplayName("A misspelled port in a connection end is reported")
    void misspelledPorts() {
        List<ValidationError> errors = check("""
            package B {
                port def P;
                part def X { port iA : P; }
                part def Lab { part s : X; }
                part sys {
                    part lab : Lab;
                    part x : X;
                    connect lab.sX to x.iY;
                    connect lab.s.iA to x.iA;
                }
            }
            """);
        assertEquals(2, errors.size(), errors.toString());
        String all = errors.toString();
        assertTrue(all.contains("'sX' is not a feature of 'lab'") && all.contains("'iY' is not a feature of 'x'"), all);
    }

    @Test
    @DisplayName("Uncertain types are not reported: redefinitions, untyped usages, successions")
    void uncertainTypes() {
        List<ValidationError> errors = check("""
            package C {
                port def P;
                part def IC { port bIn : P; }
                part def Base { part powerSource; part bat { port o : P; } }
                part def Sys :> Base {
                    part :>> powerSource : IC;
                    connect bat.o to powerSource.bIn;
                }
                part def Car { part eng; part w { port wp : P; } interface eng.ep to w.wp; }
                action def A { action a1; first start then a1; first a1 then done; }
            }
            """);
        assertEquals(0, errors.size(), errors.toString());
    }

    private static List<ValidationError> check(String text) {
        ModelIndexBuilder.Result built = ModelIndexBuilder.build(PARSER.parseString(text, "M.sysml").getParseTree(), "M.sysml", true);
        ModelIndex index = new ModelIndex();
        index.addAll(built.getElements());
        return ReferenceChecker.check(built.getSites(), index, "M.sysml");
    }
}

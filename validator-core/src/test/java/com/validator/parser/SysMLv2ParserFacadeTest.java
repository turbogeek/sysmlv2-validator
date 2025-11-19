package com.validator.parser;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SysMLv2ParserFacade.
 */
public class SysMLv2ParserFacadeTest {

    private final SysMLv2ParserFacade facade = new SysMLv2ParserFacade();

    @Test
    public void testParseSimplePackage() {
        String source = "package TestPackage;";

        SysMLv2ParserFacade.ParseResult result = facade.parseString(source, "test.sysml");

        assertNotNull(result);
        assertNotNull(result.getParseTree());
        assertTrue(result.isSuccess(), "Should parse without errors");
        assertEquals(0, result.getSyntaxErrors().size());
    }

    @Test
    public void testParsePartDefinition() {
        String source = """
            package TestPackage;

            part def Vehicle {
                part engine;
                part wheels[4];
            }
            """;

        SysMLv2ParserFacade.ParseResult result = facade.parseString(source, "test.sysml");

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should parse without errors");
    }

    @Test
    public void testParseActionDefinition() {
        String source = """
            package TestPackage;

            action def MakeBreakfast {
                action cookFood;
                action serveMeal;

                first cookFood then serveMeal;
            }
            """;

        SysMLv2ParserFacade.ParseResult result = facade.parseString(source, "test.sysml");

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should parse without errors");
    }

    @Test
    public void testParseStateDefinition() {
        String source = """
            package TestPackage;

            state def VehicleStates {
                state off;
                state running;

                transition first off then running;
            }
            """;

        SysMLv2ParserFacade.ParseResult result = facade.parseString(source, "test.sysml");

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should parse without errors");
    }

    @Test
    public void testParseRequirement() {
        String source = """
            package TestPackage;

            requirement def PerformanceRequirement {
                subject vehicle;
                require constraint speed > 100;
            }
            """;

        SysMLv2ParserFacade.ParseResult result = facade.parseString(source, "test.sysml");

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should parse without errors");
    }

    @Test
    public void testParseView() {
        String source = """
            package TestPackage;

            view 'Test View' : SomeViewType::ViewClass {
                expose TestPackage::**;
                render asDefault;
            }
            """;

        SysMLv2ParserFacade.ParseResult result = facade.parseString(source, "test.sysml");

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should parse without errors");
    }

    @Test
    public void testParseSyntaxError() {
        String source = "package ;"; // Missing package name

        SysMLv2ParserFacade.ParseResult result = facade.parseString(source, "test.sysml");

        assertNotNull(result);
        assertFalse(result.isSuccess(), "Should have syntax errors");
        assertTrue(result.hasErrors());
        assertTrue(result.getSyntaxErrors().size() > 0);
    }

    @Test
    public void testIsSysMLFile() {
        assertTrue(SysMLv2ParserFacade.isSysMLFile(new File("test.sysml")));
        assertTrue(SysMLv2ParserFacade.isSysMLFile(new File("Test.SYSML")));
        assertFalse(SysMLv2ParserFacade.isSysMLFile(new File("test.txt")));
    }

    @Test
    public void testIsKerMLFile() {
        assertTrue(SysMLv2ParserFacade.isKerMLFile(new File("test.kerml")));
        assertTrue(SysMLv2ParserFacade.isKerMLFile(new File("Test.KERML")));
        assertFalse(SysMLv2ParserFacade.isKerMLFile(new File("test.txt")));
    }

    @Test
    public void testParseComplexModel() {
        String source = """
            package VehicleModel;

            public import ScalarValues::*;

            part def Engine {
                attribute horsepower : Integer;
                attribute displacement : Real;
            }

            part def Vehicle {
                part engine : Engine;
                part wheels[4];

                attribute speed : Real;
                attribute weight : Real;
            }

            action def StartVehicle {
                action checkFuel;
                action startEngine;
                action engage;

                first checkFuel;
                then startEngine;
                then engage;
            }

            requirement def SafetyRequirement {
                subject vehicle : Vehicle;
                require constraint vehicle.speed <= 120;
            }
            """;

        SysMLv2ParserFacade.ParseResult result = facade.parseString(source, "vehicle.sysml");

        assertNotNull(result);
        if (!result.isSuccess()) {
            for (SysMLv2ParserFacade.SyntaxError error : result.getSyntaxErrors()) {
                System.err.println(error);
            }
        }
        assertTrue(result.isSuccess(), "Should parse complex model without errors");
    }
}

package com.validator.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorCLITest {

    @Test
    void testSuggestionsEnabledByDefault() {
        ValidatorCLI cli = new ValidatorCLI();
        CommandLine cmd = new CommandLine(cli);
        // By default suggestions should be true
        // Let's pass a file so it parses correctly without throwing missing required param
        cmd.parseArgs("dummy.sysml");
        
        // Unfortunately picocli populates private fields. We can check via reflection
        try {
            java.lang.reflect.Field f = ValidatorCLI.class.getDeclaredField("suggestions");
            f.setAccessible(true);
            boolean suggestions = (boolean) f.get(cli);
            assertTrue(suggestions, "Smart Compiler (suggestions) should be enabled by default");
        } catch (Exception e) {
            fail("Failed to read suggestions field: " + e.getMessage());
        }
    }

    @Test
    void testNoSuggestionsFlag() {
        ValidatorCLI cli = new ValidatorCLI();
        CommandLine cmd = new CommandLine(cli);
        cmd.parseArgs("--no-suggestions", "dummy.sysml");
        
        try {
            java.lang.reflect.Field f = ValidatorCLI.class.getDeclaredField("suggestions");
            f.setAccessible(true);
            boolean suggestions = (boolean) f.get(cli);
            assertFalse(suggestions, "--no-suggestions should disable the Smart Compiler (suggestions)");
        } catch (Exception e) {
            fail("Failed to read suggestions field: " + e.getMessage());
        }
    }
    
    @Test
    void testExplicitSuggestionsFlag() {
        ValidatorCLI cli = new ValidatorCLI();
        CommandLine cmd = new CommandLine(cli);
        cmd.parseArgs("--suggestions", "dummy.sysml");
        
        try {
            java.lang.reflect.Field f = ValidatorCLI.class.getDeclaredField("suggestions");
            f.setAccessible(true);
            boolean suggestions = (boolean) f.get(cli);
            assertTrue(suggestions, "--suggestions should explicitly enable the Smart Compiler (suggestions)");
        } catch (Exception e) {
            fail("Failed to read suggestions field: " + e.getMessage());
        }
    }
}

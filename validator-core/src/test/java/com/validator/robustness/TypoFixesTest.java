package com.validator.robustness;

import com.validator.SysMLv2ValidatorImpl;
import com.validator.ValidationError;
import com.validator.ValidationResult;
import com.validator.ValidationWarning;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.ParseResult;
import com.validator.parser.SysMLv2ParserFacade.SyntaxError;
import com.validator.refactor.RefactoringEngine;
import com.validator.suggestions.SpellingSuggestionEngine;
import com.validator.suggestions.SuggestionResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the defects the typo robustness harness ({@link TypoRobustnessTest}) found in the handling of
 * layout, nesting and long input, each on the smallest input that showed it. Misspelled keywords are in
 * {@link MisspelledKeywordTest}.
 */
@DisplayName("Layout, nesting and long-input defects found by the typo robustness harness")
class TypoFixesTest {

    private static SysMLv2ValidatorImpl validator;
    private final SysMLv2ParserFacade parser = new SysMLv2ParserFacade();

    @BeforeAll
    static void setUp() {
        validator = new SysMLv2ValidatorImpl();
    }

    private static List<SyntaxError> errors(ParseResult result) {
        return result.getSyntaxErrors();
    }

    /** An attribute whose value is a literal in {@code depth} pairs of parentheses, on line 2 from column 18. */
    private static String deepParentheses(int depth) {
        return "package DeepExpression {\n    attribute x = " + "(".repeat(depth) + "1" + ")".repeat(depth) + ";\n}\n";
    }

    /** {@code depth} packages, each nested in the one before. */
    private static String deepPackages(int depth) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            text.append("package P").append(i).append(" {\n");
        }
        return text.append("part def Innermost;\n").append("}\n".repeat(depth)).toString();
    }

    // ---------------------------------------------------------------------------------------- layout

    @Test
    @DisplayName("A UTF-8 byte order mark is not a syntax error")
    void byteOrderMarkIsIgnored(@TempDir Path dir) throws IOException {
        String model = "package P {\n    part def A;\n}\n";
        assertTrue(errors(parser.parseString("\uFEFF" + model, "bom.sysml")).isEmpty(),
            "a file saved with a BOM must parse like the same file without one");

        // The validator reads files with Files.readString, which keeps the BOM
        Path file = dir.resolve("bom.sysml");
        Files.writeString(file, "\uFEFF" + model);
        assertEquals(0, validator.validate(file.toFile()).getErrorCount());
    }

    @Test
    @DisplayName("Errors in a file with old Mac (CR) line ends are on their own line")
    void loneCarriageReturnsAreLineBreaks() {
        String model = "package P {\r    part def A;\r    part def B {\r}\r";
        List<SyntaxError> errors = errors(parser.parseString(model, "cr.sysml"));
        assertFalse(errors.isEmpty(), "the unclosed package must be reported");
        // ANTLR counts only LF as a line break: every error was on line 1, at a column past its end
        assertEquals(5, errors.get(0).getLine(), "the missing '}' is reported at the end of the file, line 5");

        String withErrorOnLine3 = "package P {\r    part def A;\r    part def B :> ;\r}\r";
        assertEquals(3, errors(parser.parseString(withErrorOnLine3, "cr.sysml")).get(0).getLine());
    }

    @Test
    @DisplayName("Line comment warnings in a CR file are on their own line")
    void lineCommentWarningsOnCrLines() {
        ValidationResult result = validator.validate("package P {\r    // note\r    part def A;\r}\r", "cr.sysml");
        List<ValidationWarning> comments = result.getWarnings().stream()
            .filter(w -> "LINE_COMMENT_NOT_PERSISTED".equals(w.getErrorCode())).toList();
        assertEquals(1, comments.size());
        assertEquals(2, comments.get(0).getLine());
        assertEquals(4, comments.get(0).getColumn());
    }

    @Test
    @DisplayName("Layout normalization keeps every offset: CR LF is kept, a lone CR becomes LF, a BOM a space")
    void layoutNormalizationKeepsOffsets() throws Exception {
        java.lang.reflect.Method normalize = SysMLv2ParserFacade.class.getDeclaredMethod("normalizeLayout", String.class);
        normalize.setAccessible(true);
        assertEquals(" a\r\nb\nc\n", normalize.invoke(null, "\uFEFFa\r\nb\rc\r"));
        assertEquals("plain\n", normalize.invoke(null, "plain\n"));
    }

    // ---------------------------------------------------------------------------------------- nesting

    @Test
    @DisplayName("Nesting deeper than the parser's stack is a syntax error, not a StackOverflowError")
    void deepNestingIsReportedNotThrown(@TempDir Path dir) throws IOException {
        String model = deepParentheses(5_000);
        Path file = dir.resolve("deep.sysml");
        Files.writeString(file, model);

        ValidationResult byFile = assertTimeoutPreemptively(Duration.ofSeconds(60), () -> validator.validate(file.toFile()));
        ValidationResult byString = validator.validate(model, "deep.sysml");
        ValidationError error = byFile.getErrors().get(0);
        assertTrue(error.getMessage().startsWith("nesting too deep to parse"), error.getMessage());
        // Reported where the brackets are deepest, which does not depend on the stack or the JIT
        assertEquals(2, error.getLine());
        assertEquals(18 + 5_000 - 1, error.getColumn());
        assertEquals(error.getMessage(), byString.getErrors().get(0).getMessage());
        assertEquals(error.getColumn(), byString.getErrors().get(0).getColumn());

        assertNull(parser.parseString(model, "deep.sysml").getParseTree(), "there is no tree to analyze");
        RefactoringEngine refactoring = new RefactoringEngine();
        refactoring.setErrorFreeOnly(false);
        assertFalse(refactoring.analyzeContent(model, "deep.sysml").canApply(),
            "the refactoring analysis must stop at the missing tree instead of throwing");
    }

    @Test
    @DisplayName("Nesting that fits on the stack still parses")
    void moderateNestingParses() {
        assertTrue(errors(parser.parseString(deepParentheses(50), "x.sysml")).isEmpty());
        assertTrue(errors(parser.parseString(deepPackages(100), "x.sysml")).isEmpty());
    }

    // ---------------------------------------------------------------------------------------- long input

    @Test
    @DisplayName("Suggestions for a very long name skip the edit-distance table")
    void suggestionsForVeryLongNameAreFast() {
        String name = "x".repeat(1_000_000);
        // Before: a (length x candidate) table per candidate, seconds per error and gigabytes of garbage
        List<SuggestionResult> suggestions = assertTimeoutPreemptively(Duration.ofSeconds(10),
            () -> new SpellingSuggestionEngine().suggest(name));
        assertTrue(suggestions.isEmpty());
        assertTrue(new SpellingSuggestionEngine().suggest("Intger").stream()
            .anyMatch(s -> s.getSuggestion().equals("Integer")), "short names still get suggestions");
    }

    @Test
    @DisplayName("Error messages quote at most the start and end of a long input")
    void longQuotedInputIsAbbreviated() {
        String name = "Typo" + "x".repeat(100_000);
        List<SyntaxError> errors = errors(parser.parseString(
            "package P {\n    part def A :> " + name + " " + name + ";\n    part p = = " + name + ";\n}\n", "long.sysml"));
        assertEquals(2, errors.size(), errors.toString());
        assertTrue(errors.get(1).getMessage().contains("'package', 'import'"),
            "the expected-token list is left whole: " + errors.get(1).getMessage());
        assertFalse(errors.isEmpty());
        for (SyntaxError e : errors) {
            // Before: the message quoted both names whole, 200,000 characters
            assertTrue(e.getMessage().length() < 5_000, "message of " + e.getMessage().length() + " characters");
        }
        assertTrue(errors.get(0).getMessage().contains("Typoxxx"), errors.get(0).getMessage());
        assertTrue(errors.get(0).getMessage().contains("..."), errors.get(0).getMessage());
    }
}

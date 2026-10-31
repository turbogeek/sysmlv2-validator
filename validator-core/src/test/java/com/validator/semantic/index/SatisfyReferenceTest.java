package com.validator.semantic.index;

import com.validator.ValidationError;
import com.validator.parser.SysMLv2ParserFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 'satisfy r;' references the requirement r (SysML v2 8.2.2.21: only 'satisfy requirement r' declares one). It was
 * parsed as a new usage named r, so r was never resolved and an import that supplied only r looked unused.
 */
@DisplayName("satisfy references")
class SatisfyReferenceTest {

    private static final SysMLv2ParserFacade PARSER = new SysMLv2ParserFacade();

    private static final String LIBRARY = """
        package Reqs {
            requirement def Q;
            requirement environmentalQualification : Q;
        }
        """;

    @Test
    @DisplayName("An unresolved satisfy target is reported")
    void unresolvedTargetReported() {
        ModelIndex index = new ModelIndex(library());
        ModelIndexBuilder.Result built = ModelIndexBuilder.build(PARSER.parseString("""
            package M {
                private import Reqs::environmentalQualification;
                part a { satisfy environmentalQualification; }
                part b { satisfy missingRequirement; }
            }
            """, "M.sysml").getParseTree(), "M.sysml", true);
        index.addAll(built.getElements());
        List<ValidationError> errors = ReferenceChecker.check(built.getSites(), index, "M.sysml");
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).getMessage().contains("missingRequirement"), errors.get(0).getMessage());
    }

    @Test
    @DisplayName("The import that supplies a satisfy target is kept by the minimizer")
    void minimizerKeepsSatisfyImport(@TempDir Path dir) throws IOException {
        File model = dir.resolve("M.sysml").toFile();
        Files.writeString(model.toPath(), """
            package M {
                private import Reqs::*;
                part a { satisfy environmentalQualification; }
            }
            """, StandardCharsets.UTF_8);
        ImportMinimizer.Result r = ImportMinimizer.minimize(List.of(model), library(), PARSER);
        assertTrue(r.getMismatches().isEmpty(), r.getMismatches().toString());
        String text = r.getRewritten().values().iterator().next();
        assertTrue(text.contains("private import Reqs::environmentalQualification;"), text);
    }

    private static ModelIndex library() {
        ModelIndex library = new ModelIndex();
        library.addAll(ModelIndexBuilder.build(PARSER.parseString(LIBRARY, "Lib.sysml").getParseTree(), "Lib.sysml", false)
            .getElements());
        return library;
    }
}

package com.validator.semantic.index;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ImportMinimizer}: private wildcard imports become element imports of the names used,
 * unused ones are removed, public ones are kept, and every name resolves as before.
 */
@DisplayName("Import minimizer")
class ImportMinimizerTest {

    private static final SysMLv2ParserFacade PARSER = new SysMLv2ParserFacade();

    private static final String LIBRARY = """
        package Base {
            attribute def MassValue;
            attribute def LengthValue;
            attribute height;
        }
        package Quantities {
            public import Base::*;
            attribute def SpeedValue;
        }
        package Units {
            attribute kg;
            attribute 'm/s';
        }
        package Unused {
            attribute def Nothing;
        }
        package Views {
            rendering def NodeRendering { attribute height; attribute width; }
            rendering asNode : NodeRendering;
        }
        """;

    private static final String MODEL = """
        package Model {
            private import Quantities::*;
            private import Units::*;
            private import Unused::*;
            public import Base::*;
            private import Views::*;
            part def Box {
                attribute m : MassValue = 2 [kg];
                attribute v : SpeedValue;
                attribute s = 3 ['m/s'];
            }
            view v1 {
                render asNode { attribute :>> height = 10; }
            }
        }
        """;

    @Test
    @DisplayName("Wildcards become the element imports that are used")
    void minimizes(@TempDir Path dir) throws IOException {
        ImportMinimizer.Result r = run(dir, false);
        assertTrue(r.getMismatches().isEmpty(), r.getMismatches().toString());
        String text = r.getRewritten().values().iterator().next();
        assertTrue(text.contains("private import Quantities::MassValue;\n    private import Quantities::SpeedValue;"),
            text);
        assertTrue(text.contains("private import Units::'m/s';\n    private import Units::kg;"), text);
        assertFalse(text.contains("Unused"), "an unused wildcard import is removed");
        assertTrue(text.contains("public import Base::*;"), "public imports are kept");
        assertTrue(text.contains("private import Views::asNode;"), text);
        assertFalse(text.contains("Quantities::height"),
            "a redefinition in a render body is inherited from the rendering, not imported");
        assertEquals(1, r.getWildcardsRemoved());
        assertEquals(3, r.getWildcardsReplaced());
        assertEquals(1, r.getPublicWildcardsKept());
    }

    @Test
    @DisplayName("With owningNamespace, elements are imported from their owner")
    void importsFromOwner(@TempDir Path dir) throws IOException {
        ImportMinimizer.Result r = run(dir, true);
        assertTrue(r.getMismatches().isEmpty(), r.getMismatches().toString());
        String text = r.getRewritten().values().iterator().next();
        assertTrue(text.contains("private import Base::MassValue;"), text);
        assertTrue(text.contains("private import Quantities::SpeedValue;"), text);
    }

    private static ImportMinimizer.Result run(Path dir, boolean owner) throws IOException {
        ModelIndex library = new ModelIndex();
        library.addAll(ModelIndexBuilder.build(PARSER.parseString(LIBRARY, "Lib.sysml").getParseTree(), "Lib.sysml",
            false).getElements());
        File model = dir.resolve("Model.sysml").toFile();
        Files.writeString(model.toPath(), MODEL, StandardCharsets.UTF_8);
        return ImportMinimizer.minimize(List.of(model), library, PARSER, owner);
    }
}

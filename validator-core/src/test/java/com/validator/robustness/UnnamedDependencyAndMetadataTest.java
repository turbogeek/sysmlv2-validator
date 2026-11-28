package com.validator.robustness;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.SyntaxError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unnamed dependencies (SysML v2 8.2.2.3: {@code ( Identification 'from' )?}) and unnamed metadata usages
 * (8.2.2.27: {@code ( Identification ( ':' | 'typed' 'by' ) )? OwnedFeatureTyping}). The grammar read the first name
 * after 'dependency' or 'metadata' as the element's name, so {@code dependency a, c to b;} failed and
 * {@code metadata Approval about x { ... }} became a usage named Approval.
 */
@DisplayName("Unnamed dependencies and metadata usages")
class UnnamedDependencyAndMetadataTest {

    private final SysMLv2ParserFacade parser = new SysMLv2ParserFacade();

    private List<SyntaxError> errors(String body) {
        return parser.parseString("package P {\n    " + body + "\n}\n", "p.sysml").getSyntaxErrors();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "dependency a to b;",
        "dependency a, c to b;",
        "dependency a to b, c;",
        "dependency d from a to b;",
        "dependency <d1> from a to b;",
        "dependency from a to b;",
        "metadata Approval;",
        "metadata Approval about x;",
        "metadata Approval about x, y { approved = true; }",
        "metadata Approval about x { :>> approved = true; }",
        "metadata Approval about x { ref redefines approved = true; }",
        "metadata m : Approval about x;",
        "metadata m typed by Approval about x;",
        "metadata : Approval about x;"
    })
    @DisplayName("Valid declarations parse")
    void valid(String body) {
        List<SyntaxError> errors = errors(body);
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "dependency to b;",
        "dependency d a to b;",
        "metadata m Approval about x;"
    })
    @DisplayName("A dependency without a client, or a name without 'from' or ':', is an error")
    void invalid(String body) {
        assertFalse(errors(body).isEmpty(), body);
    }
}

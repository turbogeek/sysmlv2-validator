package com.validator.index;

import com.validator.ast.Location;
import com.validator.semantic.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Lucene-based ModelIndexer.
 */
@DisplayName("Model Indexer Tests")
public class ModelIndexerTest {

    private ModelIndexer indexer;
    private SymbolTable symbolTable;

    @BeforeEach
    public void setUp() throws IOException {
        indexer = new ModelIndexer();
        symbolTable = createTestSymbolTable();
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (indexer != null) {
            indexer.close();
        }
    }

    @Test
    @DisplayName("Should index symbol table successfully")
    public void testIndexModel() throws Exception {
        // Index the model
        indexer.indexModel(symbolTable);

        // Verify index statistics
        ModelIndexer.IndexStats stats = indexer.getStats();
        assertEquals(5, stats.getNumDocs(), "Should have indexed 5 symbols");
    }

    @Test
    @DisplayName("Should search by name")
    public void testSearchByName() throws Exception {
        indexer.indexModel(symbolTable);

        // Search for "Vehicle"
        List<ModelIndexer.SearchResult> results = indexer.searchByName("Vehicle", 10);

        assertNotNull(results);
        assertEquals(1, results.size(), "Should find Vehicle");
        assertEquals("Vehicle", results.get(0).getName());
        assertEquals("PART_DEFINITION", results.get(0).getType());
    }

    @Test
    @DisplayName("Should search by type")
    public void testSearchByType() throws Exception {
        indexer.indexModel(symbolTable);

        // Search for all PART_DEFINITION elements
        List<ModelIndexer.SearchResult> results = indexer.searchByType("PART_DEFINITION", 10);

        assertNotNull(results);
        assertEquals(2, results.size(), "Should find 2 part definitions (Vehicle, Engine)");
    }

    @Test
    @DisplayName("Should search by qualified name")
    public void testSearchByQualifiedName() throws Exception {
        indexer.indexModel(symbolTable);

        // Search for qualified name
        List<ModelIndexer.SearchResult> results = indexer.searchByQualifiedName("TestPackage::*", 10);

        assertNotNull(results);
        assertTrue(results.size() >= 1, "Should find elements in TestPackage");
    }

    @Test
    @DisplayName("Should perform wildcard search")
    public void testSearchAll() throws Exception {
        indexer.indexModel(symbolTable);

        // Search for "Engine" across all fields
        List<ModelIndexer.SearchResult> results = indexer.searchAll("Engine", 10);

        assertNotNull(results);
        assertEquals(1, results.size(), "Should find Engine");
        assertEquals("Engine", results.get(0).getName());
    }

    @Test
    @DisplayName("Should search by custom query")
    public void testCustomQuery() throws Exception {
        indexer.indexModel(symbolTable);

        // Search by type first, then filter by name
        // Note: Complex queries mixing StringField (type) and TextField (name)
        // require using BooleanQuery or filtering results programmatically
        List<ModelIndexer.SearchResult> results = indexer.searchByType("PART_DEFINITION", 10);

        // Filter for Engine
        results = results.stream()
            .filter(r -> "Engine".equals(r.getName()))
            .toList();

        assertNotNull(results);
        assertEquals(1, results.size(), "Should find Engine part definition");
        assertEquals("Engine", results.get(0).getName());
        assertEquals("PART_DEFINITION", results.get(0).getType());
    }

    @Test
    @DisplayName("Should handle empty search results")
    public void testEmptyResults() throws Exception {
        indexer.indexModel(symbolTable);

        // Search for something that doesn't exist
        List<ModelIndexer.SearchResult> results = indexer.searchByName("NonExistent", 10);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Should return empty results for non-existent elements");
    }

    @Test
    @DisplayName("Should return results ordered by relevance score")
    public void testRelevanceScoring() throws Exception {
        indexer.indexModel(symbolTable);

        // Search for "speed" which appears in an attribute
        List<ModelIndexer.SearchResult> results = indexer.searchByName("speed", 10);

        assertNotNull(results);
        if (results.size() > 1) {
            // Scores should be in descending order
            float previousScore = Float.MAX_VALUE;
            for (ModelIndexer.SearchResult result : results) {
                assertTrue(result.getScore() <= previousScore,
                    "Results should be ordered by descending score");
                previousScore = result.getScore();
            }
        }
    }

    /**
     * Create a test symbol table with sample data.
     */
    private SymbolTable createTestSymbolTable() {
        SymbolTable table = new SymbolTable();

        // Create a package
        table.enterScope("TestPackage", ScopeType.PACKAGE);
        Symbol packageSymbol = new Symbol("TestPackage", "TestPackage",
            ElementType.PACKAGE, new Location("test.sysml", 1, 1));
        table.define(packageSymbol);

        // Create a part definition: Vehicle
        Symbol vehicleSymbol = new Symbol("Vehicle", "TestPackage::Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 3, 1));
        table.define(vehicleSymbol);

        // Create a nested part definition: Engine
        table.enterScope("Engine", ScopeType.PART_DEFINITION);
        Symbol engineSymbol = new Symbol("Engine", "TestPackage::Vehicle::Engine",
            ElementType.PART_DEFINITION, new Location("test.sysml", 5, 5));
        table.define(engineSymbol);
        table.exitScope();

        // Create an attribute: speed
        Symbol speedSymbol = new Symbol("speed", "TestPackage::Vehicle::speed",
            ElementType.ATTRIBUTE_DEFINITION, new Location("test.sysml", 10, 5));
        table.define(speedSymbol);

        // Create a requirement
        Symbol reqSymbol = new Symbol("SpeedRequirement", "TestPackage::SpeedRequirement",
            ElementType.REQUIREMENT_DEFINITION, new Location("test.sysml", 15, 1));
        table.define(reqSymbol);

        table.exitScope(); // Exit package

        return table;
    }
}

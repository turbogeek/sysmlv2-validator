package com.validator.suggestions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CameoLibraryProvider.
 */
@DisplayName("Cameo Library Provider Tests")
class CameoLibraryProviderTest {

    private CameoLibraryProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CameoLibraryProvider();
    }

    @Test
    @DisplayName("Should have correct source name")
    void testSourceName() {
        assertEquals("cameo", provider.getSourceName());
    }

    @Test
    @DisplayName("Should have non-empty candidates")
    void testHasCandidates() {
        Set<String> candidates = provider.getCandidates();
        assertFalse(candidates.isEmpty());
        assertTrue(candidates.size() > 50, "Should have at least 50 symbols");
    }

    @Test
    @DisplayName("Should have SysML v1 Block stereotype")
    void testHasBlock() {
        assertTrue(provider.getCandidates().contains("Block"));
    }

    @Test
    @DisplayName("Should have ValueType stereotype")
    void testHasValueType() {
        assertTrue(provider.getCandidates().contains("ValueType"));
    }

    @Test
    @DisplayName("Should have ConstraintBlock stereotype")
    void testHasConstraintBlock() {
        assertTrue(provider.getCandidates().contains("ConstraintBlock"));
    }

    @Test
    @DisplayName("Should have requirement stereotypes")
    void testHasRequirementStereotypes() {
        Set<String> candidates = provider.getCandidates();
        assertTrue(candidates.contains("DeriveReqt"));
        assertTrue(candidates.contains("Satisfy"));
        assertTrue(candidates.contains("Verify"));
        assertTrue(candidates.contains("Refine"));
        assertTrue(candidates.contains("Trace"));
    }

    @Test
    @DisplayName("Should have state machine stereotypes")
    void testHasStateMachineStereotypes() {
        Set<String> candidates = provider.getCandidates();
        assertTrue(candidates.contains("StateMachine"));
        assertTrue(candidates.contains("FinalState"));
        assertTrue(candidates.contains("InitialPseudostate"));
        assertTrue(candidates.contains("ChoicePseudostate"));
    }

    @Test
    @DisplayName("Should have diagram types")
    void testHasDiagramTypes() {
        Set<String> candidates = provider.getCandidates();
        assertTrue(candidates.contains("BlockDefinitionDiagram"));
        assertTrue(candidates.contains("InternalBlockDiagram"));
        assertTrue(candidates.contains("ParametricDiagram"));
        assertTrue(candidates.contains("RequirementDiagram"));
    }

    @Test
    @DisplayName("Should have QUDV elements")
    void testHasQUDVElements() {
        Set<String> candidates = provider.getCandidates();
        assertTrue(candidates.contains("Unit"));
        assertTrue(candidates.contains("QuantityKind"));
    }

    @Test
    @DisplayName("Should have safety profile elements")
    void testHasSafetyElements() {
        Set<String> candidates = provider.getCandidates();
        assertTrue(candidates.contains("Hazard"));
        assertTrue(candidates.contains("SafetyMechanism"));
        assertTrue(candidates.contains("SafetyRequirement"));
    }

    @Test
    @DisplayName("Static symbol count should be accurate")
    void testStaticSymbolCount() {
        assertEquals(provider.getCandidates().size(), CameoLibraryProvider.getSymbolCount());
    }

    @Test
    @DisplayName("Should return immutable set")
    void testImmutableSet() {
        Set<String> candidates = provider.getCandidates();
        assertThrows(UnsupportedOperationException.class, () -> candidates.add("NewSymbol"));
    }
}

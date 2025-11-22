package com.validator.suggestions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the KparLibraryProvider.
 */
@DisplayName("KPAR Library Provider Tests")
class KparLibraryProviderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should have correct source name")
    void testSourceName() {
        KparLibraryProvider provider = new KparLibraryProvider();
        assertEquals("kpar", provider.getSourceName());
    }

    @Test
    @DisplayName("Empty provider should have no candidates")
    void testEmptyProvider() {
        KparLibraryProvider provider = new KparLibraryProvider();
        assertTrue(provider.getCandidates().isEmpty());
        assertEquals(0, provider.getCandidateCount());
    }

    @Test
    @DisplayName("Should load symbols from KPAR file")
    void testLoadKparFile() throws IOException {
        String sysmlContent = """
            package TestLibrary {
                part def Vehicle;
                part def Engine;
                part def Wheel;
            }
            """;
        File kparFile = createKparFile("test.kpar", "model/test.sysml", sysmlContent);

        KparLibraryProvider provider = new KparLibraryProvider(kparFile);

        assertTrue(provider.getCandidateCount() > 0);
        // Should have at least the package and part defs
        Set<String> candidates = provider.getCandidates();
        assertTrue(candidates.contains("TestLibrary") || candidates.contains("Vehicle")
            || candidates.contains("Engine") || candidates.contains("Wheel"));
    }

    @Test
    @DisplayName("Should load symbols from multiple KPAR files")
    void testLoadMultipleKparFiles() throws IOException {
        String sysml1 = "package Lib1 { part def A; }";
        String sysml2 = "package Lib2 { part def B; }";

        File kpar1 = createKparFile("lib1.kpar", "model/a.sysml", sysml1);
        File kpar2 = createKparFile("lib2.kpar", "model/b.sysml", sysml2);

        KparLibraryProvider provider = new KparLibraryProvider(List.of(kpar1, kpar2));

        assertTrue(provider.getCandidateCount() >= 2);
    }

    @Test
    @DisplayName("Should add individual candidates")
    void testAddCandidate() {
        KparLibraryProvider provider = new KparLibraryProvider();
        provider.addCandidate("CustomSymbol");

        assertTrue(provider.getCandidates().contains("CustomSymbol"));
        assertEquals(1, provider.getCandidateCount());
    }

    @Test
    @DisplayName("Should add multiple candidates")
    void testAddCandidates() {
        KparLibraryProvider provider = new KparLibraryProvider();
        provider.addCandidates(Set.of("Symbol1", "Symbol2", "Symbol3"));

        assertEquals(3, provider.getCandidateCount());
        assertTrue(provider.getCandidates().containsAll(Set.of("Symbol1", "Symbol2", "Symbol3")));
    }

    @Test
    @DisplayName("Should clear all candidates")
    void testClear() {
        KparLibraryProvider provider = new KparLibraryProvider();
        provider.addCandidates(Set.of("A", "B", "C"));
        assertEquals(3, provider.getCandidateCount());

        provider.clear();

        assertEquals(0, provider.getCandidateCount());
        assertTrue(provider.getCandidates().isEmpty());
    }

    @Test
    @DisplayName("Should return immutable copy of candidates")
    void testImmutableCandidates() {
        KparLibraryProvider provider = new KparLibraryProvider();
        provider.addCandidate("Test");

        Set<String> candidates = provider.getCandidates();

        assertThrows(UnsupportedOperationException.class,
            () -> candidates.add("NewSymbol"));
    }

    @Test
    @DisplayName("Should handle KPAR with nested directories")
    void testNestedDirectories() throws IOException {
        String sysmlContent = """
            package Nested {
                part def DeepPart;
            }
            """;
        File kparFile = createKparFile("nested.kpar",
            "library/domain/subsystem/deep.sysml", sysmlContent);

        KparLibraryProvider provider = new KparLibraryProvider(kparFile);

        assertTrue(provider.getCandidateCount() > 0);
    }

    @Test
    @DisplayName("Should load via loadKparFile method")
    void testLoadKparFileMethod() throws IOException {
        String sysmlContent = "package LoadTest { part def Item; }";
        File kparFile = createKparFile("loadtest.kpar", "model/test.sysml", sysmlContent);

        KparLibraryProvider provider = new KparLibraryProvider();
        assertEquals(0, provider.getCandidateCount());

        provider.loadKparFile(kparFile);

        assertTrue(provider.getCandidateCount() > 0);
    }

    @Test
    @DisplayName("Should implement SuggestionProvider interface")
    void testImplementsInterface() {
        KparLibraryProvider provider = new KparLibraryProvider();

        assertTrue(provider instanceof SuggestionProvider);
        assertNotNull(provider.getCandidates());
        assertNotNull(provider.getSourceName());
    }

    // Helper method for creating test KPAR files

    private File createKparFile(String name, String entryPath, String content) throws IOException {
        File kparFile = tempDir.resolve(name).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(kparFile))) {
            ZipEntry entry = new ZipEntry(entryPath);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return kparFile;
    }
}

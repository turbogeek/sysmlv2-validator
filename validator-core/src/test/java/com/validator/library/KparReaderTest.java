package com.validator.library;

import com.validator.library.KparReader.KparReadResult;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the KparReader class.
 */
@DisplayName("KPAR Reader Tests")
class KparReaderTest {

    private KparReader reader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        reader = new KparReader();
    }

    @Test
    @DisplayName("Should throw exception for non-existent file")
    void testNonExistentFile() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent.kpar");
        assertThrows(IOException.class, () -> reader.read(nonExistent));
    }

    @Test
    @DisplayName("Should throw exception for non-KPAR file")
    void testNonKparFile() throws IOException {
        File textFile = tempDir.resolve("test.txt").toFile();
        assertTrue(textFile.createNewFile());

        assertThrows(IOException.class, () -> reader.read(textFile));
    }

    @Test
    @DisplayName("Should read empty KPAR file")
    void testEmptyKparFile() throws IOException {
        File kparFile = createKparFile("empty.kpar");

        KparReadResult result = reader.read(kparFile);

        assertEquals("empty.kpar", result.getSourceName());
        assertEquals(0, result.getFilesProcessed());
        assertEquals(0, result.getSymbolCount());
        assertTrue(result.getSymbols().isEmpty());
        assertFalse(result.hasErrors());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should read KPAR with single SysML file")
    void testSingleSysMLFile() throws IOException {
        String sysmlContent = """
            package TestPackage {
                part def Vehicle;
                part def Engine;
            }
            """;
        File kparFile = createKparFile("single.kpar", "model/test.sysml", sysmlContent);

        KparReadResult result = reader.read(kparFile);

        assertEquals(1, result.getFilesProcessed());
        assertTrue(result.getSymbolCount() > 0);
        assertFalse(result.getSymbols().isEmpty());
    }

    @Test
    @DisplayName("Should read KPAR with multiple model files")
    void testMultipleModelFiles() throws IOException {
        String sysml1 = """
            package Domain {
                part def Sensor;
            }
            """;
        String sysml2 = """
            package Systems {
                part def Controller;
            }
            """;

        File kparFile = createKparFileMultiple("multi.kpar",
            "library/Domain/sensors.sysml", sysml1,
            "library/Systems/controllers.sysml", sysml2
        );

        KparReadResult result = reader.read(kparFile);

        assertEquals(2, result.getFilesProcessed());
        assertTrue(result.getSymbolCount() >= 2, "Should have at least 2 symbols");
    }

    @Test
    @DisplayName("Should handle KerML files in KPAR")
    void testKerMLFiles() throws IOException {
        String kermlContent = """
            package KerML {
                datatype String;
            }
            """;
        File kparFile = createKparFile("kerml.kpar", "library/base.kerml", kermlContent);

        KparReadResult result = reader.read(kparFile);

        assertEquals(1, result.getFilesProcessed());
    }

    @Test
    @DisplayName("Should skip non-model files")
    void testSkipsNonModelFiles() throws IOException {
        String sysmlContent = """
            package Test {
                part def Item;
            }
            """;
        File kparFile = createKparFileMultiple("mixed.kpar",
            "manifest.json", "{\"name\": \"test\"}",
            "README.md", "# Test Library",
            "model/test.sysml", sysmlContent
        );

        KparReadResult result = reader.read(kparFile);

        assertEquals(1, result.getFilesProcessed(), "Should only process .sysml file");
    }

    @Test
    @DisplayName("Should list model files in KPAR")
    void testListModelFiles() throws IOException {
        String sysmlContent = "package Test {}";
        File kparFile = createKparFileMultiple("list.kpar",
            "manifest.json", "{}",
            "library/domain.sysml", sysmlContent,
            "library/systems.kerml", sysmlContent
        );

        List<String> modelFiles = reader.listModelFiles(kparFile);

        assertEquals(2, modelFiles.size());
        assertTrue(modelFiles.stream().anyMatch(f -> f.endsWith(".sysml")));
        assertTrue(modelFiles.stream().anyMatch(f -> f.endsWith(".kerml")));
    }

    @Test
    @DisplayName("Should extract KPAR to temp directory")
    void testExtractToTemp() throws IOException {
        String sysmlContent = "package Extracted {}";
        File kparFile = createKparFile("extract.kpar", "model/test.sysml", sysmlContent);

        Path extracted = reader.extractToTemp(kparFile);

        assertTrue(extracted.toFile().exists());
        assertTrue(extracted.toFile().isDirectory());

        // Clean up
        deleteRecursively(extracted.toFile());
    }

    @Test
    @DisplayName("Should read multiple KPAR files")
    void testReadAllKparFiles() throws IOException {
        String sysml1 = "package Lib1 { part def A; }";
        String sysml2 = "package Lib2 { part def B; }";

        File kpar1 = createKparFile("lib1.kpar", "model/a.sysml", sysml1);
        File kpar2 = createKparFile("lib2.kpar", "model/b.sysml", sysml2);

        KparReadResult result = reader.readAll(List.of(kpar1, kpar2));

        assertEquals("combined", result.getSourceName());
        assertEquals(2, result.getFilesProcessed());
        assertTrue(result.getSymbolCount() >= 2);
    }

    @Test
    @DisplayName("Should report errors for malformed content")
    void testMalformedContent() throws IOException {
        String badContent = "this is not valid SysML {{{{";
        File kparFile = createKparFile("bad.kpar", "model/bad.sysml", badContent);

        KparReadResult result = reader.read(kparFile);

        // File should still be processed even with errors
        assertEquals(1, result.getFilesProcessed());
    }

    @Test
    @DisplayName("KparReadResult toString should be informative")
    void testResultToString() throws IOException {
        String sysmlContent = "package Test { part def Item; }";
        File kparFile = createKparFile("toString.kpar", "model/test.sysml", sysmlContent);

        KparReadResult result = reader.read(kparFile);
        String str = result.toString();

        assertTrue(str.contains("KparReadResult"));
        assertTrue(str.contains("toString.kpar"));
    }

    // Helper methods for creating test KPAR files

    private File createKparFile(String name) throws IOException {
        File kparFile = tempDir.resolve(name).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(kparFile))) {
            // Empty zip file
        }
        return kparFile;
    }

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

    private File createKparFileMultiple(String name, String... pathsAndContents) throws IOException {
        if (pathsAndContents.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide pairs of path and content");
        }

        File kparFile = tempDir.resolve(name).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(kparFile))) {
            for (int i = 0; i < pathsAndContents.length; i += 2) {
                String entryPath = pathsAndContents[i];
                String content = pathsAndContents[i + 1];
                ZipEntry entry = new ZipEntry(entryPath);
                zos.putNextEntry(entry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return kparFile;
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}

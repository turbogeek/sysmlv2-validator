package com.validator.kpar;

import com.validator.kpar.KparArchive.InvalidKparException;
import com.validator.kpar.KparArchive.Member;
import com.validator.testutil.Kpars;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading a model KPAR: the project, the model files in archive order, and the archives that are not KPARs.
 */
@DisplayName("KPAR reader")
class KparArchiveTest {

    @Test
    @DisplayName("Reads the project and the model files in archive order, at any path, and nothing else")
    void readsProjectAndMembers(@TempDir Path dir) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("Second.sysml", "package Second;");
        entries.put(".project.json", Kpars.project("Vehicle Model", "2.1.0"));
        entries.put(".meta.json", "{\"index\":{\"First\":\"sub/First.kerml\"}}");
        entries.put("sub/", "");
        entries.put("sub/First.kerml", "package First { classifier C; }");
        entries.put("README.md", "# not a model file");
        entries.put("docs/Third.SysML", "package Third; // ünïcode");
        Path kpar = Kpars.write(dir.resolve("Vehicle.kpar"), entries);

        KparArchive archive = KparArchive.read(kpar);

        assertEquals("Vehicle Model", archive.getName());
        assertEquals("2.1.0", archive.getVersion());
        assertEquals("Test model", archive.getDescription());
        assertEquals(List.of(
            new Member("Second.sysml", "package Second;"),
            new Member("sub/First.kerml", "package First { classifier C; }"),
            new Member("docs/Third.SysML", "package Third; // ünïcode")), archive.getMembers());
        assertEquals("First.kerml", archive.getMembers().get(1).fileName());
        assertEquals("Second.sysml", archive.getMembers().get(0).fileName());
        assertThrows(UnsupportedOperationException.class, () -> archive.getMembers().clear());
    }

    @Test
    @DisplayName("A project without a name, version or description reads them as null")
    void projectWithoutFields(@TempDir Path dir) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(".project.json", "{\"version\":2,\"usage\":[]}");
        entries.put("A.sysml", "package A;");
        KparArchive archive = KparArchive.read(Kpars.write(dir.resolve("A.kpar"), entries));

        assertNull(archive.getName());
        assertNull(archive.getVersion());
        assertNull(archive.getDescription());
        assertEquals(1, archive.getMembers().size());
    }

    @Test
    @DisplayName("A file that is not a zip is not a KPAR")
    void notAZip(@TempDir Path dir) throws IOException {
        Path text = Files.writeString(dir.resolve("Text.kpar"), "package NotAnArchive;");
        Path empty = Files.writeString(dir.resolve("Empty.kpar"), "");

        assertInvalid(text, "not a zip archive");
        assertInvalid(empty, "not a zip archive");
    }

    @Test
    @DisplayName("A zip without .project.json at its root is not a KPAR")
    void noProject(@TempDir Path dir) throws IOException {
        Path none = Kpars.write(dir.resolve("None.kpar"), Map.of("A.sysml", "package A;"));
        Map<String, String> nested = new LinkedHashMap<>();
        nested.put("model/.project.json", Kpars.project("Nested", "1.0"));
        nested.put("model/A.sysml", "package A;");
        Path inFolder = Kpars.write(dir.resolve("Nested.kpar"), nested);

        assertInvalid(none, "no .project.json at the root of the archive");
        assertInvalid(inFolder, "no .project.json at the root of the archive");
    }

    @Test
    @DisplayName("A .project.json that is not a JSON object makes the archive invalid")
    void badProject(@TempDir Path dir) throws IOException {
        for (String project : List.of("{\"name\": ", "[\"name\"]", "")) {
            Map<String, String> entries = new LinkedHashMap<>();
            entries.put(".project.json", project);
            entries.put("A.sysml", "package A;");
            Path kpar = Kpars.write(dir.resolve("Bad.kpar"), entries);

            InvalidKparException e = assertThrows(InvalidKparException.class, () -> KparArchive.read(kpar));
            assertTrue(e.getMessage().startsWith(".project.json is not"), project + ": " + e.getMessage());
        }
    }

    @Test
    @DisplayName("A zip without a .sysml or .kerml file is not a model KPAR")
    void noModelFile(@TempDir Path dir) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(".project.json", Kpars.project("Empty", "1.0"));
        entries.put(".meta.json", "{\"index\":{}}");
        entries.put("sysml/", "");
        entries.put("notes.txt", "package A;");
        Path kpar = Kpars.write(dir.resolve("Empty.kpar"), entries);

        assertInvalid(kpar, "no model file (.sysml or .kerml) in the archive");
    }

    @Test
    @DisplayName("A model file that is not UTF-8 text makes the archive invalid")
    void notUtf8(@TempDir Path dir) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put(".project.json", Kpars.project("Latin1", "1.0").getBytes(StandardCharsets.UTF_8));
        entries.put("sub/Latin1.sysml", "package Caf\u00e9;".getBytes(StandardCharsets.ISO_8859_1));
        Path kpar = Kpars.writeBytes(dir.resolve("Latin1.kpar"), entries);

        assertInvalid(kpar, "sub/Latin1.sysml is not UTF-8 text");
    }

    @Test
    @DisplayName("A model file whose compressed data is corrupted makes the archive invalid")
    void corruptedMember(@TempDir Path dir) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(".project.json", Kpars.project("Corrupted", "1.0"));
        entries.put("Model.sysml", "package Model { part def A; part def B; part def C; }");
        Path kpar = Kpars.write(dir.resolve("Corrupted.kpar"), entries);
        byte[] zip = Files.readAllBytes(kpar);
        int name = indexOf(zip, "Model.sysml".getBytes(StandardCharsets.UTF_8));
        int header = name - 30;
        int extra = (zip[header + 28] & 0xff) | (zip[header + 29] & 0xff) << 8;
        // A deflate block header with the reserved block type 3
        zip[name + "Model.sysml".length() + extra] = (byte) 0xff;
        Files.write(kpar, zip);

        InvalidKparException e = assertThrows(InvalidKparException.class, () -> KparArchive.read(kpar));
        assertTrue(e.getMessage().startsWith("corrupted zip entry"), e.getMessage());
    }

    @Test
    @DisplayName("A member's file path is the archive's path, '!' and the member's path, and splits back")
    void memberPaths() {
        String path = KparArchive.memberPath("/models/StockTicker.kpar", "sub/Logical.sysml");

        assertEquals("/models/StockTicker.kpar!sub/Logical.sysml", path);
        assertEquals("sub/Logical.sysml", KparArchive.memberOf(path));
        assertEquals("/models/StockTicker.kpar", KparArchive.archiveOf(path));
        assertEquals("A.sysml", KparArchive.memberOf("C:\\x!y\\M.KPAR!A.sysml"));
        assertEquals("C:\\x!y\\M.KPAR", KparArchive.archiveOf("C:\\x!y\\M.KPAR!A.sysml"));
        assertNull(KparArchive.memberOf("/models/Logical.sysml"));
        assertEquals("/models/Logical.sysml", KparArchive.archiveOf("/models/Logical.sysml"));
        assertNull(KparArchive.memberOf("!kpar!A.sysml"));
    }

    private static void assertInvalid(Path kpar, String message) {
        InvalidKparException e = assertThrows(InvalidKparException.class, () -> KparArchive.read(kpar));
        assertTrue(e.getMessage().startsWith(message), e.getMessage());
    }

    private static int indexOf(byte[] data, byte[] part) {
        for (int i = 0; i + part.length <= data.length; i++) {
            boolean match = true;
            for (int j = 0; j < part.length && match; j++) {
                match = data[i + j] == part[j];
            }
            if (match) {
                return i;
            }
        }
        throw new AssertionError("not found");
    }
}

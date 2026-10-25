package com.validator.library;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.validator.semantic.StandardLibraryManager;
import com.validator.semantic.index.IndexedElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that library cache entries written by older validator code are not reused: an entry without the
 * index fields that the inheritance check needs is parsed again and rewritten in the current format.
 */
@DisplayName("Library cache format")
class LibraryCacheFormatTest {

    private static final String LIBRARY = """
        package Lib {
            part def A { attribute f; }
            part def B :> A { attribute :>> f : Real [1] = 1; }
        }
        """;

    @Test
    @DisplayName("An entry from an older cache format (without redefinitions) is parsed again, then reused")
    void staleEntryIsRebuilt(@TempDir Path tmp) throws IOException {
        Path libraryDir = Files.createDirectories(tmp.resolve("lib"));
        Files.writeString(libraryDir.resolve("Lib.sysml"), LIBRARY);
        Path cacheDir = tmp.resolve("cache");

        LibraryCache first = new LibraryCache(cacheDir);
        assertNotNull(redefinition(load(first, libraryDir)), "parsed on the first load");
        assertTrue(first.getMisses() >= 1);

        // Rewrite the entry as format 3 wrote it: another cache key, no inheritance fields
        Path entry = entryFor(cacheDir, "Lib.sysml");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = (ObjectNode) mapper.readTree(entry.toFile());
        json.put("cacheKey", "0123456789abcdef");
        for (JsonNode element : json.get("elements")) {
            ((ObjectNode) element).remove(List.of("redefines", "parameter", "isPrivate", "line", "column",
                "typeText", "multiplicityText", "valueText"));
        }
        mapper.writeValue(entry.toFile(), json);

        LibraryCache second = new LibraryCache(cacheDir);
        IndexedElement rebuilt = redefinition(load(second, libraryDir));
        assertEquals(1, second.getMisses(), "only the stale entry is parsed again");
        assertNotNull(rebuilt, "the redefinition is back after the rebuild");
        assertEquals(List.of(List.of("f")), rebuilt.redefines);

        LibraryCache third = new LibraryCache(cacheDir);
        IndexedElement cached = redefinition(load(third, libraryDir));
        assertEquals(0, third.getMisses(), "the rewritten entry is reused");
        assertNotNull(cached);
        assertEquals(List.of(List.of("f")), cached.redefines, "fields survive the JSON round trip");
        assertEquals("Real", cached.typeText);
        assertEquals("[1]", cached.multiplicityText);
        assertEquals("= 1", cached.valueText);
        assertEquals(3, cached.line);
        assertFalse(cached.parameter);
    }

    // ------------------------------------------------------------------------------------------

    /** Load only the given directory (and the validator's bundled metadata library). */
    private static LibraryLoader load(LibraryCache cache, Path libraryDir) {
        LibraryConfig config = new LibraryConfig() {
            @Override
            public List<Path> getLibraryPaths() {
                return List.of(libraryDir);
            }

            @Override
            public List<Path> getDSViewsPaths() {
                return List.of();
            }
        };
        LibraryLoader loader = new LibraryLoader(cache, new StandardLibraryManager());
        loader.loadLibraries(config);
        return loader;
    }

    /** The unnamed redefinition ':>> f' in Lib::B, or null. */
    private static IndexedElement redefinition(LibraryLoader loader) {
        for (LibraryCacheData data : loader.getLoaded()) {
            for (IndexedElement e : data.getElements()) {
                if ("Lib::B".equals(e.parentName) && "f".equals(e.effectiveName())) {
                    return e;
                }
            }
        }
        return null;
    }

    private static Path entryFor(Path cacheDir, String fileName) throws IOException {
        try (Stream<Path> files = Files.list(cacheDir)) {
            return files.filter(p -> p.getFileName().toString().startsWith(fileName + "_")
                    && p.getFileName().toString().endsWith(".json"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no cache entry for " + fileName));
        }
    }
}

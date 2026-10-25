package com.validator.library;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LibraryCache reuses an entry when the cache key and the source's size match and either its modification time or
 * its checksum match; anything else is a miss.
 */
@DisplayName("Library cache")
class LibraryCacheTest {

    private static final byte[] CONTENT = "package Lib { part def P; }".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path cacheDir;

    private LibraryCache cache;
    private Path source;

    @BeforeEach
    void setUp() {
        cache = new LibraryCache(cacheDir);
        source = cacheDir.resolveSibling("Lib.sysml");
    }

    private LibraryCacheData entry(long lastModified) {
        LibraryCacheData data = new LibraryCacheData("Lib.sysml", lastModified);
        data.setSize(CONTENT.length);
        data.setSha256(LibraryCache.sha256Hex(CONTENT));
        LibraryCacheData.CachedSymbol symbol = new LibraryCacheData.CachedSymbol();
        symbol.setName("P");
        symbol.setQualifiedName("Lib::P");
        data.getSymbols().add(symbol);
        return data;
    }

    private List<Path> entries() throws IOException {
        try (Stream<Path> files = Files.list(cacheDir)) {
            return files.filter(p -> p.toString().endsWith(".json")).toList();
        }
    }

    @Test
    @DisplayName("Without an entry the lookup is a miss")
    void testMissWithoutEntry() {
        assertNull(cache.lookup(source, 1000L, CONTENT.length, () -> CONTENT));
        assertEquals(1, cache.getMisses());
        assertEquals(0, cache.getHits());
        assertEquals(cacheDir, cache.getCacheDir());
    }

    @Test
    @DisplayName("An entry with the same size and modification time is reused without reading the source")
    void testHitByTime() {
        cache.saveCache(source, entry(1000L));
        LibraryCacheData data = cache.lookup(source, 1000L, CONTENT.length, () -> {
            throw new AssertionError("The source must not be read");
        });
        assertNotNull(data);
        assertEquals("Lib::P", data.getSymbols().get(0).getQualifiedName());
        assertEquals(LibraryCache.getCacheKey(), data.getCacheKey(), "Saving stamps the cache key");
        assertEquals(1, cache.getHits());
    }

    @Test
    @DisplayName("A new modification time with the same checksum is a hit and the time is refreshed")
    void testHitByChecksum() {
        cache.saveCache(source, entry(1000L));
        assertNotNull(cache.lookup(source, 2000L, CONTENT.length, () -> CONTENT));
        assertEquals(1, cache.getRehashedHits());
        assertNotNull(cache.lookup(source, 2000L, CONTENT.length, () -> {
            throw new AssertionError("The refreshed time makes this a plain hit");
        }));
        assertEquals(1, cache.getHits());
    }

    @Test
    @DisplayName("A different size, different content or an unreadable source is a miss")
    void testMisses() {
        cache.saveCache(source, entry(1000L));
        assertNull(cache.lookup(source, 1000L, CONTENT.length + 1, () -> CONTENT));
        byte[] changed = CONTENT.clone();
        changed[0] = 'P';
        assertNull(cache.lookup(source, 2000L, CONTENT.length, () -> changed));
        assertNull(cache.lookup(source, 3000L, CONTENT.length, () -> {
            throw new IOException("unreadable");
        }));
        assertEquals(3, cache.getMisses());
        assertEquals(0, cache.getHits() + cache.getRehashedHits());
    }

    @Test
    @DisplayName("An entry written by other validator code or not readable as JSON is a miss")
    void testInvalidEntries() throws IOException {
        cache.saveCache(source, entry(1000L));
        Path file = entries().get(0);
        String json = Files.readString(file);
        Files.writeString(file, json.replace(LibraryCache.getCacheKey(), "0000000000000000"));
        assertNull(cache.lookup(source, 1000L, CONTENT.length, () -> CONTENT));
        assertNull(cache.loadCache(source, 1000L), "The legacy lookup checks the cache key too");

        Files.writeString(file, "{ not json");
        assertNull(cache.lookup(source, 1000L, CONTENT.length, () -> CONTENT));
        assertNull(cache.loadCache(source, 1000L));
        assertEquals(2, cache.getMisses());
    }

    @Test
    @DisplayName("The legacy lookup matches the modification time only")
    void testLegacyLookup() {
        assertNull(cache.loadCache(source, 1000L));
        cache.saveCache(source, entry(1000L));
        assertNotNull(cache.loadCache(source, 1000L));
        assertNull(cache.loadCache(source, 1001L));
    }

    @Test
    @DisplayName("Entries of different sources with the same file name do not collide")
    void testDistinctEntriesPerPath() throws IOException {
        Path other = cacheDir.resolveSibling("other").resolve("Lib.sysml");
        cache.saveCache(source, entry(1000L));
        cache.saveCache(other, entry(5000L));
        assertEquals(2, entries().size());
        assertNotNull(cache.lookup(other, 5000L, CONTENT.length, () -> CONTENT));
        assertNotNull(cache.lookup(source, 1000L, CONTENT.length, () -> CONTENT));
    }

    @Test
    @DisplayName("Clearing deletes the entries and resets the counters")
    void testClear() throws IOException {
        cache.saveCache(source, entry(1000L));
        cache.lookup(source, 1000L, CONTENT.length, () -> CONTENT);
        cache.lookup(source, 1000L, 1, () -> CONTENT);
        Files.writeString(cacheDir.resolve("keep.txt"), "not a cache entry");

        assertEquals(1, cache.clear());
        assertTrue(entries().isEmpty());
        assertTrue(Files.exists(cacheDir.resolve("keep.txt")), "Only cache entries are deleted");
        assertEquals(0, cache.getHits());
        assertEquals(0, cache.getMisses());
        assertNull(cache.lookup(source, 1000L, CONTENT.length, () -> CONTENT));
    }

    @Test
    @DisplayName("The cache directory comes from the system property, else the user's home")
    void testDefaultCacheDir() {
        String original = System.getProperty("sysml.validator.cache.dir");
        try {
            System.setProperty("sysml.validator.cache.dir", cacheDir.toString());
            assertEquals(cacheDir, LibraryCache.defaultCacheDir());
            System.setProperty("sysml.validator.cache.dir", " ");
            assertEquals(Path.of(System.getProperty("user.home"), ".sysml-validator/cache"),
                LibraryCache.defaultCacheDir());
        } finally {
            if (original == null) {
                System.clearProperty("sysml.validator.cache.dir");
            } else {
                System.setProperty("sysml.validator.cache.dir", original);
            }
        }
    }

    @Test
    @DisplayName("Checksums are SHA-256 in lower-case hex and the cache key is stable")
    void testChecksumAndKey() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            LibraryCache.sha256Hex("abc".getBytes(StandardCharsets.US_ASCII)));
        String key = LibraryCache.getCacheKey();
        assertTrue(key.matches("[0-9a-f]{16}"), key);
        assertEquals(key, LibraryCache.getCacheKey());
    }

    @Test
    @DisplayName("A cache directory that cannot be created leaves the cache usable as all misses")
    void testUncreatableDirectory() throws IOException {
        Path file = Files.writeString(cacheDir.resolve("a-file"), "x");
        LibraryCache broken = new LibraryCache(file.resolve("cache"));
        broken.saveCache(source, entry(1000L));
        assertNull(broken.lookup(source, 1000L, CONTENT.length, () -> CONTENT));
        assertEquals(0, broken.clear());
    }
}

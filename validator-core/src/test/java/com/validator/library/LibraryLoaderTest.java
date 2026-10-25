package com.validator.library;

import com.validator.semantic.ElementType;
import com.validator.semantic.StandardLibraryManager;
import com.validator.semantic.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LibraryLoader parses library directories and .kpar archives, registers their symbols, caches what it parsed and
 * reports the syntax errors of library files on every load.
 */
@DisplayName("Library loader")
class LibraryLoaderTest {

    @TempDir
    Path dir;

    private Path libraryDir;
    private LibraryCache cache;

    /** A configuration with exactly the given paths, whatever the environment configures. */
    private static LibraryConfig config(List<Path> libraries, List<Path> dsViews) {
        return new LibraryConfig() {
            @Override
            public List<Path> getLibraryPaths() {
                return libraries;
            }

            @Override
            public List<Path> getDSViewsPaths() {
                return dsViews;
            }
        };
    }

    @BeforeEach
    void setUp() throws IOException {
        libraryDir = Files.createDirectory(dir.resolve("lib"));
        Files.writeString(libraryDir.resolve("Vehicles.sysml"), """
            package Vehicles {
                part def Vehicle;
                part def Car :> Vehicle;
            }
            """);
        Files.createDirectory(libraryDir.resolve("kernel"));
        Files.writeString(libraryDir.resolve("kernel/Base.kerml"), "package Base { classifier Thing; }");
        Files.writeString(libraryDir.resolve("README.md"), "not a library file");
        cache = new LibraryCache(dir.resolve("cache"));
    }

    @Test
    @DisplayName("A library directory's .sysml and .kerml files are parsed and their symbols registered")
    void testLoadsDirectory() {
        StandardLibraryManager manager = new StandardLibraryManager();
        LibraryLoader loader = new LibraryLoader(cache, manager);
        loader.loadLibraries(config(List.of(libraryDir), List.of()));

        List<String> sources = loader.getLoaded().stream().map(LibraryCacheData::getSourceName).toList();
        assertEquals(List.of("LintConfig.sysml", "Vehicles.sysml", "Base.kerml"), sources,
            "The bundled metadata library first, then the directory's library files in path order");
        Symbol car = manager.resolveSymbol("Vehicles::Car");
        assertNotNull(car);
        assertEquals(ElementType.PART_DEFINITION, car.getType());
        assertNotNull(manager.resolveSymbol("Base::Thing"));
        assertTrue(loader.getDiagnostics().isEmpty());
        assertEquals(3, cache.getMisses(), "Everything was parsed the first time");
    }

    @Test
    @DisplayName("A second load reuses the cache and still reports library syntax errors")
    void testReusesCacheAndKeepsDiagnostics() throws IOException {
        Files.writeString(libraryDir.resolve("Broken.sysml"), "package Broken {\n    part def P : ;\n}\n");
        LibraryLoader first = new LibraryLoader(cache, new StandardLibraryManager());
        first.loadLibraries(config(List.of(libraryDir), List.of()));
        assertEquals(1, first.getDiagnostics().size(), first.getDiagnostics().toString());

        LibraryCache sameCache = new LibraryCache(dir.resolve("cache"));
        LibraryLoader second = new LibraryLoader(sameCache, new StandardLibraryManager());
        second.loadLibraries(config(List.of(libraryDir), List.of()));
        assertEquals(4, sameCache.getHits(), "Every file comes from the cache");
        assertEquals(0, sameCache.getMisses());

        LibraryLoader.LibraryDiagnostic diagnostic = second.getDiagnostics().get(0);
        assertEquals(libraryDir.resolve("Broken.sysml").toString(), diagnostic.getSource());
        assertEquals(2, diagnostic.getLine());
        assertTrue(diagnostic.getColumn() >= 0);
        assertTrue(diagnostic.toString().startsWith(diagnostic.getSource() + ":2:"), diagnostic.toString());
        assertEquals(first.getDiagnostics().get(0).getMessage(), diagnostic.getMessage());
    }

    @Test
    @DisplayName("The library files in a .kpar archive are loaded")
    void testLoadsKpar() throws IOException {
        Path kpar = dir.resolve("Extra.kpar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(kpar))) {
            zip.putNextEntry(new ZipEntry("Extra.sysml"));
            zip.write("package Extra { attribute def Speed; }".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("docs/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(".project.json"));
            zip.write("{}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        StandardLibraryManager manager = new StandardLibraryManager();
        LibraryLoader loader = new LibraryLoader(cache, manager);
        loader.loadLibraries(config(List.of(kpar), List.of()));

        assertEquals(2, loader.getLoaded().size(), "The bundled library and the archive's one .sysml file");
        assertEquals(ElementType.ATTRIBUTE_DEFINITION, manager.resolveSymbol("Extra::Speed").getType());

        LibraryLoader again = new LibraryLoader(cache, new StandardLibraryManager());
        again.loadLibraries(config(List.of(kpar), List.of()));
        assertTrue(cache.getHits() >= 2, "The archive entry is cached as well");
    }

    @Test
    @DisplayName("DS_Views paths are loaded like library paths")
    void testLoadsDSViews() {
        StandardLibraryManager manager = new StandardLibraryManager();
        LibraryLoader loader = new LibraryLoader(cache, manager);
        loader.loadLibraries(config(List.of(), List.of(libraryDir)));
        assertNotNull(manager.resolveSymbol("Vehicles::Vehicle"));
    }

    @Test
    @DisplayName("Missing paths, other files and unreadable archives are skipped")
    void testSkipsUnusablePaths() throws IOException {
        Path notKpar = Files.writeString(dir.resolve("notes.txt"), "text");
        Path badKpar = Files.writeString(dir.resolve("Bad.kpar"), "not a zip archive");
        StandardLibraryManager manager = new StandardLibraryManager();
        LibraryLoader loader = new LibraryLoader(cache, manager);
        loader.loadLibraries(config(List.of(dir.resolve("missing"), notKpar, badKpar), List.of()));

        assertEquals(1, loader.getLoaded().size(), "Only the bundled metadata library");
        assertNull(manager.resolveSymbol("Vehicles::Vehicle"));
        assertTrue(loader.getDiagnostics().isEmpty());
    }

    @Test
    @DisplayName("The loaded files and diagnostics cannot be changed from outside")
    void testUnmodifiableViews() {
        LibraryLoader loader = new LibraryLoader(cache, new StandardLibraryManager());
        loader.loadLibraries(config(List.of(), List.of()));
        assertEquals(1, loader.getLoaded().size());
        try {
            loader.getLoaded().clear();
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, loader.getLoaded().size());
            return;
        }
        throw new AssertionError("getLoaded() should be unmodifiable");
    }

    /** Keeps OutputStream imported for the zip writer's type in older javac versions. */
    @SuppressWarnings("unused")
    private static OutputStream unused() {
        return OutputStream.nullOutputStream();
    }
}

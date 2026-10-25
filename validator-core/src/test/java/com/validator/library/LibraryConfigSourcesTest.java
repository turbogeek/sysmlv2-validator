package com.validator.library;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Where LibraryConfig takes library paths from: the system properties, the environment and validator.properties in
 * the working directory, and which DS_Views paths it accepts.
 */
@DisplayName("Library config sources")
class LibraryConfigSourcesTest {

    private static final String LIBRARY_PROPERTY = "sysml.library.path";
    private static final String DS_VIEWS_PROPERTY = "ds.views.library.path";
    private static final Path CONFIG_FILE = Path.of("validator.properties");

    @TempDir
    Path dir;

    private String originalLibrary;
    private String originalDSViews;

    @BeforeEach
    void saveProperties() {
        originalLibrary = System.getProperty(LIBRARY_PROPERTY);
        originalDSViews = System.getProperty(DS_VIEWS_PROPERTY);
    }

    @AfterEach
    void restoreProperties() {
        restore(LIBRARY_PROPERTY, originalLibrary);
        restore(DS_VIEWS_PROPERTY, originalDSViews);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    @DisplayName("The library path of the system property is used, absolute and normalized")
    void testLibraryFromProperty() throws IOException {
        Path library = Files.createDirectory(dir.resolve("sysml.library"));
        System.setProperty(LIBRARY_PROPERTY, library.resolve("../sysml.library").toString());
        LibraryConfig config = new LibraryConfig();
        assertTrue(config.hasLibraryPaths());
        assertEquals(library.toAbsolutePath().normalize(), config.getLibraryPaths().get(0));
        config.getLibraryPaths().clear();
        assertEquals(library.toAbsolutePath().normalize(), config.getLibraryPaths().get(0),
            "The getter returns a copy");
    }

    @Test
    @DisplayName("The same library path from the property and the environment is listed once")
    void testLibraryPathNotDuplicated() {
        String environment = System.getenv("SYSML_LIBRARY_PATH");
        assumeTrue(environment != null && !environment.isEmpty(), "SYSML_LIBRARY_PATH is not set");
        System.setProperty(LIBRARY_PROPERTY, environment);
        List<Path> paths = new LibraryConfig().getLibraryPaths();
        assertEquals(paths.stream().distinct().count(), paths.size(), paths.toString());
        assertTrue(paths.contains(Path.of(environment).toAbsolutePath().normalize()));
    }

    @Test
    @DisplayName("A DS_Views directory from the system property is used")
    void testDSViewsFromProperty() throws IOException {
        Path views = Files.createDirectory(dir.resolve("DS_Views"));
        System.setProperty(DS_VIEWS_PROPERTY, views.toString());
        LibraryConfig config = new LibraryConfig();
        assertTrue(config.hasDSViewsPaths());
        assertEquals(List.of(views.toAbsolutePath().normalize()), config.getDSViewsPaths());
    }

    @Test
    @DisplayName("A DS_Views property naming a missing path or a file configures no DS_Views")
    void testDSViewsPropertyNotADirectory() throws IOException {
        System.setProperty(DS_VIEWS_PROPERTY, dir.resolve("missing").toString());
        assertFalse(new LibraryConfig().hasDSViewsPaths(), "A missing path is skipped, not searched for elsewhere");

        Path file = Files.writeString(dir.resolve("views.sysml"), "package V;");
        System.setProperty(DS_VIEWS_PROPERTY, file.toString());
        assertTrue(new LibraryConfig().getDSViewsPaths().isEmpty(), "A file is not a DS_Views directory");
    }

    @Test
    @DisplayName("validator.properties in the working directory adds a library path and a DS_Views path")
    void testConfigFile() throws IOException {
        assumeFalse(Files.exists(CONFIG_FILE), "A validator.properties already exists in the working directory");
        assumeTrue(System.getenv("DS_VIEWS_LIBRARY_PATH") == null, "DS_VIEWS_LIBRARY_PATH takes precedence");
        System.clearProperty(DS_VIEWS_PROPERTY);
        Path library = Files.createDirectory(dir.resolve("from-file-library"));
        Path views = Files.createDirectory(dir.resolve("from-file-views"));
        Files.writeString(CONFIG_FILE, LIBRARY_PROPERTY + "=" + library.toString().replace('\\', '/') + "\n"
            + DS_VIEWS_PROPERTY + "=" + views.toString().replace('\\', '/') + "\n");
        try {
            LibraryConfig config = new LibraryConfig();
            assertTrue(config.getLibraryPaths().contains(library.toAbsolutePath().normalize()),
                config.getLibraryPaths().toString());
            assertEquals(List.of(views.toAbsolutePath().normalize()), config.getDSViewsPaths());
        } finally {
            Files.deleteIfExists(CONFIG_FILE);
        }
    }
}

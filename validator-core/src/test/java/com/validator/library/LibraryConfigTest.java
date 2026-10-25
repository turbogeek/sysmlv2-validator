package com.validator.library;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for library path discovery in {@link LibraryConfig}.
 */
@DisplayName("Library Config Tests")
class LibraryConfigTest {

    @Test
    @DisplayName("Should locate the bundled Dassault libraries from the class location")
    void findsBundledDSLibrary() {
        Path bundled = LibraryConfig.findBundledDSLibrary();

        assertNotNull(bundled, "bundled CATIA_SysML Libraries/26xR2 folder not found");
        assertTrue(bundled.resolve("DassaultSystemesViews.sysml").toFile().isFile());
    }

    @Test
    @DisplayName("Should configure DS_Views from the bundled libraries when nothing else is set")
    void defaultsDSViewsToBundledLibrary() {
        if (System.getProperty("ds.views.library.path") != null
                || System.getenv("DS_VIEWS_LIBRARY_PATH") != null) {
            return;
        }

        LibraryConfig config = new LibraryConfig();

        assertTrue(config.hasDSViewsPaths());
        assertTrue(config.getDSViewsPaths().stream()
            .anyMatch(p -> p.resolve("DassaultSystemesViews.sysml").toFile().isFile()));
    }
}

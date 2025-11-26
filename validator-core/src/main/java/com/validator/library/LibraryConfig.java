package com.validator.library;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuration for SysML v2 standard library paths.
 *
 * Supports multiple configuration methods:
 * 1. System property: -Dsysml.library.path=/path/to/sysml.library
 * 2. Environment variable: SYSML_LIBRARY_PATH
 * 3. Configuration file: validator.properties
 * 4. Default search paths
 */
public class LibraryConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryConfig.class);

    private static final String SYSML_LIBRARY_PROPERTY = "sysml.library.path";
    private static final String SYSML_LIBRARY_ENV = "SYSML_LIBRARY_PATH";
    private static final String CONFIG_FILE = "validator.properties";

    private final List<Path> libraryPaths = new ArrayList<>();

    public LibraryConfig() {
        loadConfiguration();
    }

    private void loadConfiguration() {
        // 1. Check system property
        String sysProp = System.getProperty(SYSML_LIBRARY_PROPERTY);
        if (sysProp != null && !sysProp.isEmpty()) {
            addLibraryPath(sysProp);
            LOGGER.info("Using library path from system property: {}", sysProp);
        }

        // 2. Check environment variable
        String envVar = System.getenv(SYSML_LIBRARY_ENV);
        if (envVar != null && !envVar.isEmpty()) {
            addLibraryPath(envVar);
            LOGGER.info("Using library path from environment: {}", envVar);
        }

        // 3. Check configuration file
        loadFromConfigFile();

        // 4. Try default search paths
        if (libraryPaths.isEmpty()) {
            tryDefaultPaths();
        }

        if (libraryPaths.isEmpty()) {
            LOGGER.warn("No SysML v2 library paths configured. "
                + "Import validation will be limited.");
            LOGGER.info("To configure library path, use one of:");
            LOGGER.info("  - System property: -D{}=/path/to/sysml.library",
                SYSML_LIBRARY_PROPERTY);
            LOGGER.info("  - Environment variable: {}=/path/to/sysml.library",
                SYSML_LIBRARY_ENV);
            LOGGER.info("  - Config file: {} with property: {}",
                CONFIG_FILE, SYSML_LIBRARY_PROPERTY);
        }
    }

    private void loadFromConfigFile() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                Properties prop = new Properties();
                prop.load(input);
                String path = prop.getProperty(SYSML_LIBRARY_PROPERTY);
                if (path != null && !path.isEmpty()) {
                    addLibraryPath(path);
                    LOGGER.info("Using library path from config file: {}", path);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load config file {}: {}",
                    CONFIG_FILE, e.getMessage());
            }
        }
    }

    private void tryDefaultPaths() {
        // Common locations for SysML v2 Release
        List<String> defaultPaths = List.of(
            "../SysML-v2-Release/sysml.library",
            "../../SysML-v2-Release/sysml.library",
            "../sysml.library",
            "./sysml.library",
            System.getProperty("user.home") + "/SysML-v2-Release/sysml.library"
        );

        for (String pathStr : defaultPaths) {
            File dir = new File(pathStr);
            if (dir.exists() && dir.isDirectory()) {
                addLibraryPath(pathStr);
                LOGGER.info("Found library at default path: {}", pathStr);
                return;
            }
        }
    }

    private void addLibraryPath(String pathStr) {
        Path path = Paths.get(pathStr).toAbsolutePath().normalize();
        if (!libraryPaths.contains(path)) {
            libraryPaths.add(path);
        }
    }

    public List<Path> getLibraryPaths() {
        return new ArrayList<>(libraryPaths);
    }

    public boolean hasLibraryPaths() {
        return !libraryPaths.isEmpty();
    }
}

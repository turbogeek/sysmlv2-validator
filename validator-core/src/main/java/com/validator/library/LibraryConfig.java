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
 * Configuration for SysML v2 standard library paths and optional DS_Views library.
 *
 * Supports multiple configuration methods:
 * 1. System property: -Dsysml.library.path=/path/to/sysml.library
 * 2. Environment variable: SYSML_LIBRARY_PATH
 * 3. Configuration file: validator.properties
 * 4. Default search paths
 *
 * DS_Views (Cameo Systems Modeler proprietary library):
 * 1. System property: -Dds.views.library.path=/path/to/DS_Views
 * 2. Environment variable: DS_VIEWS_LIBRARY_PATH
 * 3. Configuration file: validator.properties (ds.views.library.path=...)
 * 4. Default search paths (Cameo installation directories)
 */
public class LibraryConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryConfig.class);

    private static final String SYSML_LIBRARY_PROPERTY = "sysml.library.path";
    private static final String SYSML_LIBRARY_ENV = "SYSML_LIBRARY_PATH";
    private static final String DS_VIEWS_LIBRARY_PROPERTY = "ds.views.library.path";
    private static final String DS_VIEWS_LIBRARY_ENV = "DS_VIEWS_LIBRARY_PATH";
    private static final String CONFIG_FILE = "validator.properties";

    private final List<Path> libraryPaths = new ArrayList<>();
    private final List<Path> dsViewsPaths = new ArrayList<>();

    public LibraryConfig() {
        loadConfiguration();
    }

    private void loadConfiguration() {
        // Load standard SysML v2 library
        loadStandardLibrary();

        // Load optional DS_Views library (Cameo)
        loadDSViewsLibrary();
    }

    private void loadStandardLibrary() {
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

    private void loadDSViewsLibrary() {
        // 1. Check system property
        String sysProp = System.getProperty(DS_VIEWS_LIBRARY_PROPERTY);
        if (sysProp != null && !sysProp.isEmpty()) {
            addDSViewsPath(sysProp);
            LOGGER.info("Using DS_Views library from system property: {}", sysProp);
            return;
        }

        // 2. Check environment variable
        String envVar = System.getenv(DS_VIEWS_LIBRARY_ENV);
        if (envVar != null && !envVar.isEmpty()) {
            addDSViewsPath(envVar);
            LOGGER.info("Using DS_Views library from environment: {}", envVar);
            return;
        }

        // 3. Check configuration file
        loadDSViewsFromConfigFile();

        // 4. Try default search paths (Cameo installation)
        if (dsViewsPaths.isEmpty()) {
            tryDefaultDSViewsPaths();
        }

        if (dsViewsPaths.isEmpty()) {
            LOGGER.info("DS_Views library not configured (optional). "
                + "Cameo-specific view imports will show warnings.");
            LOGGER.debug("To enable DS_Views validation:");
            LOGGER.debug("  - System property: -D{}=/path/to/DS_Views",
                DS_VIEWS_LIBRARY_PROPERTY);
            LOGGER.debug("  - Environment variable: {}=/path/to/DS_Views",
                DS_VIEWS_LIBRARY_ENV);
        } else {
            LOGGER.info("DS_Views library configured: {} paths", dsViewsPaths.size());
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

    private void loadDSViewsFromConfigFile() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                Properties prop = new Properties();
                prop.load(input);
                String path = prop.getProperty(DS_VIEWS_LIBRARY_PROPERTY);
                if (path != null && !path.isEmpty()) {
                    addDSViewsPath(path);
                    LOGGER.info("Using DS_Views library from config file: {}", path);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load DS_Views from config file {}: {}",
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
            System.getProperty("user.home") + "/SysML-v2-Release/sysml.library",
            "../SysML-v2-Release/sysml.library.kpar",
            "../../SysML-v2-Release/sysml.library.kpar",
            "../sysml.library.kpar",
            "./sysml.library.kpar",
            System.getProperty("user.home") + "/SysML-v2-Release/sysml.library.kpar"
        );

        for (String pathStr : defaultPaths) {
            File file = new File(pathStr);
            if (file.exists() && (file.isDirectory() || (file.isFile() && file.getName().endsWith(".kpar")))) {
                addLibraryPath(pathStr);
                LOGGER.info("Found library at default path: {}", pathStr);
                return;
            }
        }
    }

    private void tryDefaultDSViewsPaths() {
        // Common locations for Cameo DS_Views library
        List<String> defaultPaths = new ArrayList<>();

        // Add Cameo installation paths (Windows)
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            defaultPaths.add(programFiles + "/NoMagic/Cameo Systems Modeler/plugins/DS_Views");
            defaultPaths.add(programFiles + "/Dassault Systemes/Cameo/plugins/DS_Views");
        }

        // Add user workspace
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            defaultPaths.add(userHome + "/.cameo/libraries/DS_Views");
            defaultPaths.add(userHome + "/CameoWorkspace/libraries/DS_Views");
        }

        // Check current project
        defaultPaths.add("./DS_Views");
        defaultPaths.add("../DS_Views");

        for (String pathStr : defaultPaths) {
            File dir = new File(pathStr);
            if (dir.exists() && dir.isDirectory()) {
                addDSViewsPath(pathStr);
                LOGGER.info("Found DS_Views at default path: {}", pathStr);
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

    private void addDSViewsPath(String pathStr) {
        Path path = Paths.get(pathStr).toAbsolutePath().normalize();
        File dir = path.toFile();
        if (!dir.exists()) {
            LOGGER.warn("DS_Views path does not exist: {}", pathStr);
            return;
        }
        if (!dir.isDirectory()) {
            LOGGER.warn("DS_Views path is not a directory: {}", pathStr);
            return;
        }
        if (!dsViewsPaths.contains(path)) {
            dsViewsPaths.add(path);
        }
    }

    public List<Path> getLibraryPaths() {
        return new ArrayList<>(libraryPaths);
    }

    public List<Path> getDSViewsPaths() {
        return new ArrayList<>(dsViewsPaths);
    }

    public boolean hasLibraryPaths() {
        return !libraryPaths.isEmpty();
    }

    public boolean hasDSViewsPaths() {
        return !dsViewsPaths.isEmpty();
    }
}

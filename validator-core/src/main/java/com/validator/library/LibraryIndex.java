package com.validator.library;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Indexes SysML v2 standard library packages and elements.
 * Scans .sysml and .kerml files to build a map of available packages and types.
 */
public class LibraryIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryIndex.class);

    // Pattern to extract package declarations
    private static final Pattern PACKAGE_PATTERN =
        Pattern.compile("(?:standard\\s+library\\s+)?package\\s+(\\w+(?:::\\w+)*)\\s*\\{");

    // Pattern to extract type definitions (comprehensive)
    private static final Pattern TYPE_PATTERN = Pattern.compile(
        "(?:abstract\\s+)?(?:datatype|class|struct|assoc\\s+struct|"
        + "part\\s+def|attribute\\s+def|requirement\\s+def|action\\s+def|"
        + "state\\s+def|constraint\\s+def|calc\\s+def|analysis\\s+def|"
        + "case\\s+def|verification\\s+def|view\\s+def|viewpoint\\s+def|"
        + "connection\\s+def|interface\\s+def|port\\s+def|item\\s+def|"
        + "allocation\\s+def|enum\\s+def|metadata\\s+def|flow\\s+def|"
        + "rendering\\s+def|concern\\s+def|occurrence\\s+def|individual\\s+def|"
        + "use\\s+case\\s+def)\\s+(\\w+)");

    // Map: Package name -> Set of defined elements
    private final Map<String, Set<String>> packageElements = new HashMap<>();

    // Set of all indexed packages
    private final Set<String> indexedPackages = new HashSet<>();

    // Track library paths
    private final Set<Path> indexedPaths = new HashSet<>();

    /**
     * Index all library files from configured paths.
     * Indexes both standard SysML v2 library and optional DS_Views library.
     */
    public void indexLibraries(LibraryConfig config) {
        // Index standard SysML v2 library
        for (Path libraryPath : config.getLibraryPaths()) {
            indexLibraryPath(libraryPath);
        }

        // Index optional DS_Views library (Cameo)
        if (config.hasDSViewsPaths()) {
            for (Path dsViewsPath : config.getDSViewsPaths()) {
                LOGGER.info("Indexing DS_Views library: {}", dsViewsPath);
                indexLibraryPath(dsViewsPath);
            }
        }

        LOGGER.info("Library indexing complete: {} packages, {} elements total",
            indexedPackages.size(), countTotalElements());
    }

    /**
     * Index a single library directory.
     */
    private void indexLibraryPath(Path libraryPath) {
        if (!Files.exists(libraryPath) || !Files.isDirectory(libraryPath)) {
            LOGGER.warn("Library path does not exist or is not a directory: {}",
                libraryPath);
            return;
        }

        if (indexedPaths.contains(libraryPath)) {
            LOGGER.debug("Already indexed: {}", libraryPath);
            return;
        }

        LOGGER.info("Indexing library path: {}", libraryPath);
        indexedPaths.add(libraryPath);

        try (Stream<Path> paths = Files.walk(libraryPath)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".sysml")
                    || p.toString().endsWith(".kerml"))
                .forEach(this::indexFile);
        } catch (IOException e) {
            LOGGER.error("Failed to index library path {}: {}",
                libraryPath, e.getMessage());
        }
    }

    /**
     * Index a single library file.
     */
    private void indexFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            indexContent(content, filePath);
        } catch (IOException e) {
            LOGGER.warn("Failed to read library file {}: {}",
                filePath, e.getMessage());
        }
    }

    /**
     * Extract package and type information from file content.
     */
    private void indexContent(String content, Path filePath) {
        // Extract package name
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
        if (!packageMatcher.find()) {
            LOGGER.debug("No package declaration found in: {}", filePath);
            return;
        }

        String packageName = packageMatcher.group(1);
        indexedPackages.add(packageName);

        // Index nested package paths if qualified (e.g., SysML::Actions)
        if (packageName.contains("::")) {
            String[] parts = packageName.split("::");
            StringBuilder path = new StringBuilder(parts[0]);
            indexedPackages.add(parts[0]);

            for (int i = 1; i < parts.length; i++) {
                // Add parent package as having this child element
                Set<String> parentElements = packageElements.computeIfAbsent(
                    path.toString(), k -> new HashSet<>());
                parentElements.add(parts[i]);

                path.append("::").append(parts[i]);
                indexedPackages.add(path.toString());
            }
        }

        // Extract type definitions
        Set<String> elements = packageElements.computeIfAbsent(packageName,
            k -> new HashSet<>());

        Matcher typeMatcher = TYPE_PATTERN.matcher(content);
        while (typeMatcher.find()) {
            String typeName = typeMatcher.group(1);
            elements.add(typeName);
            LOGGER.trace("Indexed {}.{}", packageName, typeName);
        }

        LOGGER.debug("Indexed package {} from {} ({} elements)",
            packageName, filePath.getFileName(), elements.size());
    }

    /**
     * Check if a package is available in the library.
     */
    public boolean hasPackage(String packageName) {
        return indexedPackages.contains(packageName);
    }

    /**
     * Check if an element exists in a package.
     */
    public boolean hasElement(String packageName, String elementName) {
        Set<String> elements = packageElements.get(packageName);
        return elements != null && elements.contains(elementName);
    }

    /**
     * Check if a qualified name (Package::Element or Package::Sub::Element) is available.
     * Supports deep path resolution for multi-level qualified names.
     */
    public boolean hasQualifiedName(String qualifiedName) {
        if (!qualifiedName.contains("::")) {
            // Check if it's a package
            return hasPackage(qualifiedName);
        }

        String[] parts = qualifiedName.split("::");

        if (parts.length == 2) {
            // Simple case: Package::Element
            return hasElement(parts[0], parts[1]) || hasPackage(qualifiedName);
        }

        // Multi-level: SysML::Actions::Action::start (4+ levels)
        // Strategy: Try progressively deeper package paths

        // Strategy 1: Check if full path is a package
        if (hasPackage(qualifiedName)) {
            return true;
        }

        // Strategy 2: Try Package::Element combinations at each level
        StringBuilder packagePath = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String element = parts[i];

            // Check if current packagePath has this element
            if (hasElement(packagePath.toString(), element)) {
                // Found element - if this is the last part, we're done
                if (i == parts.length - 1) {
                    return true;
                }
                // Otherwise, continue checking nested elements
                // For nested features like Action::start, we accept if parent exists
                return true;
            }

            // Check if packagePath::element is itself a package
            String nestedPackage = packagePath + "::" + element;
            if (hasPackage(nestedPackage)) {
                packagePath = new StringBuilder(nestedPackage);
                continue;
            }

            // Check if this could be a nested element (feature of a type)
            // For paths like SysML::Actions::Action::start
            // where Action is a type and start is a feature
            if (i > 1) {
                // Accept if we've already found a valid intermediate element
                return hasNestedElement(parts, i);
            }
        }

        // Fallback: check if root package exists
        return hasPackage(parts[0]);
    }

    /**
     * Check if nested element path exists.
     * For: SysML::Actions::Action::start
     * Verifies that intermediate elements exist.
     */
    private boolean hasNestedElement(String[] parts, int currentIndex) {
        // Build all possible package::element combinations
        for (int pkgEnd = 1; pkgEnd < parts.length; pkgEnd++) {
            StringBuilder pkgPath = new StringBuilder(parts[0]);
            for (int j = 1; j < pkgEnd; j++) {
                pkgPath.append("::").append(parts[j]);
            }

            String elementName = parts[pkgEnd];
            if (hasElement(pkgPath.toString(), elementName)) {
                // Found a valid package::element combination
                // Accept remaining parts as nested features
                return true;
            }
        }
        return false;
    }

    /**
     * Get all elements in a package.
     */
    public Set<String> getPackageElements(String packageName) {
        Set<String> elements = packageElements.get(packageName);
        return elements != null ? new HashSet<>(elements) : new HashSet<>();
    }

    /**
     * Get all indexed package names.
     */
    public Set<String> getIndexedPackages() {
        return new HashSet<>(indexedPackages);
    }

    private int countTotalElements() {
        return packageElements.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * Check if any libraries have been indexed.
     */
    public boolean hasLibraries() {
        return !indexedPackages.isEmpty();
    }
}

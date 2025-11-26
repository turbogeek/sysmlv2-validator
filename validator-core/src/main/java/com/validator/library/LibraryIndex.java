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

    // Pattern to extract type definitions (simplified)
    private static final Pattern TYPE_PATTERN = Pattern.compile(
        "(?:abstract\\s+)?(?:datatype|class|part\\s+def|attribute\\s+def|"
        + "requirement\\s+def)\\s+(\\w+)");

    // Map: Package name -> Set of defined elements
    private final Map<String, Set<String>> packageElements = new HashMap<>();

    // Set of all indexed packages
    private final Set<String> indexedPackages = new HashSet<>();

    // Track library paths
    private final Set<Path> indexedPaths = new HashSet<>();

    /**
     * Index all library files from configured paths.
     */
    public void indexLibraries(LibraryConfig config) {
        for (Path libraryPath : config.getLibraryPaths()) {
            indexLibraryPath(libraryPath);
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
     * Check if a qualified name (Package::Element) is available.
     */
    public boolean hasQualifiedName(String qualifiedName) {
        if (!qualifiedName.contains("::")) {
            // Check if it's a package
            return hasPackage(qualifiedName);
        }

        String[] parts = qualifiedName.split("::");
        if (parts.length == 2) {
            return hasElement(parts[0], parts[1]);
        }

        // Handle multi-level: Foo::Bar::Baz
        // For now, just check the root package exists
        return hasPackage(parts[0]);
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

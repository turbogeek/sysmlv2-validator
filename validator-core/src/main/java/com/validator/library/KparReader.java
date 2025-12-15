package com.validator.library;

import com.validator.ast.Location;
import com.validator.semantic.ElementType;
import com.validator.semantic.Symbol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads KPAR (KerML/SysML Package Archive) files.
 * KPAR files are ZIP archives containing .sysml and .kerml model files.
 */
public final class KparReader {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "(?:standard\\s+)?(?:library\\s+)?package\\s+(\\w+)");
    private static final Pattern PART_DEF_PATTERN = Pattern.compile(
        "part\\s+def\\s+(\\w+)");
    private static final Pattern ACTION_DEF_PATTERN = Pattern.compile(
        "action\\s+def\\s+(\\w+)");
    private static final Pattern REQUIREMENT_DEF_PATTERN = Pattern.compile(
        "requirement\\s+def\\s+(\\w+)");
    private static final Pattern ATTRIBUTE_DEF_PATTERN = Pattern.compile(
        "attribute\\s+def\\s+(\\w+)");
    private static final Pattern DATA_TYPE_PATTERN = Pattern.compile(
        "datatype\\s+(\\w+)");

    /**
     * Read symbols from a KPAR file.
     *
     * @param kparFile the KPAR file to read
     * @return the read result with extracted symbols
     * @throws IOException if the file cannot be read
     */
    public KparReadResult read(File kparFile) throws IOException {
        if (!kparFile.exists()) {
            throw new IOException("File does not exist: " + kparFile.getAbsolutePath());
        }
        if (!kparFile.getName().endsWith(".kpar")) {
            throw new IOException("Not a KPAR file: " + kparFile.getName());
        }

        KparReadResult result = new KparReadResult(kparFile.getName());

        try (ZipFile zipFile = new ZipFile(kparFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (isModelFile(name) && !entry.isDirectory()) {
                    result.incrementFilesProcessed();
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        extractSymbols(content, name, result);
                    } catch (Exception e) {
                        result.addError("Error parsing " + name + ": " + e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Read symbols from multiple KPAR files.
     *
     * @param kparFiles the KPAR files to read
     * @return combined read result
     */
    public KparReadResult readAll(List<File> kparFiles) throws IOException {
        KparReadResult combined = new KparReadResult("combined");

        for (File kparFile : kparFiles) {
            KparReadResult result = read(kparFile);
            combined.merge(result);
        }

        return combined;
    }

    /**
     * List all model files in a KPAR archive.
     *
     * @param kparFile the KPAR file
     * @return list of model file paths within the archive
     */
    public List<String> listModelFiles(File kparFile) throws IOException {
        List<String> modelFiles = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(kparFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (isModelFile(entry.getName()) && !entry.isDirectory()) {
                    modelFiles.add(entry.getName());
                }
            }
        }

        return modelFiles;
    }

    /**
     * Extract KPAR contents to a temporary directory.
     *
     * @param kparFile the KPAR file to extract
     * @return path to the temporary directory
     */
    public Path extractToTemp(File kparFile) throws IOException {
        Path tempDir = Files.createTempDirectory("kpar-extract-");

        try (ZipFile zipFile = new ZipFile(kparFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path targetPath = tempDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, targetPath);
                    }
                }
            }
        }

        return tempDir;
    }

    private boolean isModelFile(String name) {
        return name.endsWith(".sysml") || name.endsWith(".kerml");
    }

    private void extractSymbols(String content, String fileName, KparReadResult result) {
        String currentPackage = "";

        // Extract package names
        Matcher pkgMatcher = PACKAGE_PATTERN.matcher(content);
        while (pkgMatcher.find()) {
            currentPackage = pkgMatcher.group(1);
            result.addSymbol(new Symbol(currentPackage, currentPackage,
                ElementType.PACKAGE, new Location(fileName, 1, 1)));
        }

        // Extract part definitions
        extractDefinitions(content, fileName, currentPackage, PART_DEF_PATTERN,
            ElementType.PART_DEFINITION, result);

        // Extract action definitions
        extractDefinitions(content, fileName, currentPackage, ACTION_DEF_PATTERN,
            ElementType.ACTION_DEFINITION, result);

        // Extract requirement definitions
        extractDefinitions(content, fileName, currentPackage, REQUIREMENT_DEF_PATTERN,
            ElementType.REQUIREMENT_DEFINITION, result);

        // Extract attribute definitions
        extractDefinitions(content, fileName, currentPackage, ATTRIBUTE_DEF_PATTERN,
            ElementType.ATTRIBUTE_DEFINITION, result);

        // Extract datatypes
        extractDefinitions(content, fileName, currentPackage, DATA_TYPE_PATTERN,
            ElementType.DATA_TYPE, result);
    }

    private void extractDefinitions(String content, String fileName, String packageName,
                                    Pattern pattern, ElementType type, KparReadResult result) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(1);
            String qualifiedName = packageName.isEmpty() ? name : packageName + "::" + name;
            result.addSymbol(new Symbol(name, qualifiedName, type,
                new Location(fileName, 1, 1)));
        }
    }

    /**
     * Result of reading a KPAR file.
     */
    public static class KparReadResult {
        private final String sourceName;
        private int filesProcessed = 0;
        private final Map<String, Symbol> symbols = new LinkedHashMap<>();
        private final List<String> errors = new ArrayList<>();

        KparReadResult(String sourceName) {
            this.sourceName = sourceName;
        }

        void incrementFilesProcessed() {
            filesProcessed++;
        }

        void addSymbol(Symbol symbol) {
            symbols.put(symbol.getQualifiedName(), symbol);
        }

        void addError(String error) {
            errors.add(error);
        }

        void merge(KparReadResult other) {
            filesProcessed += other.filesProcessed;
            symbols.putAll(other.symbols);
            errors.addAll(other.errors);
        }

        /**
         * Get the source name.
         */
        public String getSourceName() {
            return sourceName;
        }

        /**
         * Get the number of files processed.
         */
        public int getFilesProcessed() {
            return filesProcessed;
        }

        /**
         * Get the number of symbols extracted.
         */
        public int getSymbolCount() {
            return symbols.size();
        }

        /**
         * Get all extracted symbols.
         */
        public Collection<Symbol> getSymbols() {
            return Collections.unmodifiableCollection(symbols.values());
        }

        /**
         * Get all errors encountered.
         */
        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        /**
         * Check if there were errors.
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /**
         * Check if the read was successful (no errors).
         */
        public boolean isSuccess() {
            return errors.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("KparReadResult{source='%s', files=%d, symbols=%d, errors=%d}",
                sourceName, filesProcessed, symbols.size(), errors.size());
        }
    }
}

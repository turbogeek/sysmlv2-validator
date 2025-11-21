package com.validator.library;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads KPAR (KerML/SysML Package Archive) files.
 * KPAR files are ZIP archives containing SysML v2 library models.
 *
 * <p>Structure of a KPAR file:
 * <pre>
 * library.kpar
 * ├── manifest.json (optional metadata)
 * ├── library/
 * │   ├── Domain/
 * │   │   └── *.sysml
 * │   ├── Systems/
 * │   │   └── *.sysml
 * │   └── *.kerml
 * └── ...
 * </pre>
 */
public class KparReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(KparReader.class);

    private final SysMLv2ParserFacade parserFacade;

    public KparReader() {
        this.parserFacade = new SysMLv2ParserFacade();
    }

    /**
     * Reads a KPAR file and extracts all symbols from contained model files.
     *
     * @param kparFile the KPAR file to read
     * @return result containing extracted symbols and any errors
     * @throws IOException if the file cannot be read
     */
    public KparReadResult read(File kparFile) throws IOException {
        if (!kparFile.exists()) {
            throw new IOException("KPAR file not found: " + kparFile.getAbsolutePath());
        }

        if (!SysMLv2ParserFacade.isKparFile(kparFile)) {
            throw new IOException("Not a KPAR file: " + kparFile.getName());
        }

        LOGGER.info("Reading KPAR file: {}", kparFile.getAbsolutePath());
        List<Symbol> symbols = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int filesProcessed = 0;

        try (ZipFile zipFile = new ZipFile(kparFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();

                // Process SysML and KerML files
                if (entryName.endsWith(".sysml") || entryName.endsWith(".kerml")) {
                    try {
                        String content = readEntryContent(zipFile, entry);
                        List<Symbol> entrySymbols = parseAndExtractSymbols(content, entryName);
                        symbols.addAll(entrySymbols);
                        filesProcessed++;
                        LOGGER.debug("Processed {}: {} symbols", entryName, entrySymbols.size());
                    } catch (Exception e) {
                        String error = String.format("Error processing %s: %s", entryName, e.getMessage());
                        errors.add(error);
                        LOGGER.warn(error);
                    }
                }
            }
        }

        LOGGER.info("KPAR read complete: {} files, {} symbols, {} errors",
            filesProcessed, symbols.size(), errors.size());

        return new KparReadResult(kparFile.getName(), symbols, errors, filesProcessed);
    }

    /**
     * Reads the content of a ZIP entry as a string.
     */
    private String readEntryContent(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream is = zipFile.getInputStream(entry);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Parses content and extracts symbols using the symbol table builder.
     */
    private List<Symbol> parseAndExtractSymbols(String content, String fileName) {
        SysMLv2ParserFacade.ParseResult parseResult = parserFacade.parseString(content, fileName);

        if (parseResult.hasErrors()) {
            // Log but don't fail - extract what we can
            LOGGER.debug("Parse errors in {}: {}", fileName, parseResult.getSyntaxErrors().size());
        }

        // Build symbol table from parse tree using static factory method
        SymbolTable symbolTable = SymbolTableBuilder.build(parseResult.getParseTree(), fileName);

        return new ArrayList<>(symbolTable.getAllSymbols());
    }

    /**
     * Reads multiple KPAR files and merges their symbols.
     *
     * @param kparFiles list of KPAR files to read
     * @return combined result from all files
     */
    public KparReadResult readAll(List<File> kparFiles) throws IOException {
        List<Symbol> allSymbols = new ArrayList<>();
        List<String> allErrors = new ArrayList<>();
        int totalFiles = 0;

        for (File kparFile : kparFiles) {
            KparReadResult result = read(kparFile);
            allSymbols.addAll(result.getSymbols());
            allErrors.addAll(result.getErrors());
            totalFiles += result.getFilesProcessed();
        }

        return new KparReadResult("combined", allSymbols, allErrors, totalFiles);
    }

    /**
     * Extracts KPAR contents to a temporary directory.
     *
     * @param kparFile the KPAR file to extract
     * @return path to the extracted directory
     * @throws IOException if extraction fails
     */
    public Path extractToTemp(File kparFile) throws IOException {
        Path tempDir = Files.createTempDirectory("kpar_" + kparFile.getName());

        try (ZipFile zipFile = new ZipFile(kparFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = tempDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, entryPath);
                    }
                }
            }
        }

        LOGGER.info("Extracted KPAR to: {}", tempDir);
        return tempDir;
    }

    /**
     * Lists all model files in a KPAR archive.
     *
     * @param kparFile the KPAR file
     * @return list of model file paths within the archive
     * @throws IOException if the file cannot be read
     */
    public List<String> listModelFiles(File kparFile) throws IOException {
        List<String> modelFiles = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(kparFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!entry.isDirectory()
                    && (name.endsWith(".sysml") || name.endsWith(".kerml"))) {
                    modelFiles.add(name);
                }
            }
        }

        return modelFiles;
    }

    /**
     * Result of reading a KPAR file.
     */
    public static class KparReadResult {
        private final String sourceName;
        private final List<Symbol> symbols;
        private final List<String> errors;
        private final int filesProcessed;

        public KparReadResult(String sourceName, List<Symbol> symbols,
                             List<String> errors, int filesProcessed) {
            this.sourceName = sourceName;
            this.symbols = new ArrayList<>(symbols);
            this.errors = new ArrayList<>(errors);
            this.filesProcessed = filesProcessed;
        }

        public String getSourceName() {
            return sourceName;
        }

        public List<Symbol> getSymbols() {
            return new ArrayList<>(symbols);
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public int getFilesProcessed() {
            return filesProcessed;
        }

        public int getSymbolCount() {
            return symbols.size();
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean isSuccess() {
            return errors.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("KparReadResult[%s: %d files, %d symbols, %d errors]",
                sourceName, filesProcessed, symbols.size(), errors.size());
        }
    }
}

package com.validator.library;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.ParseResult;
import com.validator.semantic.StandardLibraryManager;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;
import com.validator.semantic.index.ModelIndexBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads external libraries by fully parsing them via ANTLR.
 * Utilizes LibraryCache to avoid re-parsing on subsequent runs.
 *
 * <p>All library files are registered first and their relationships wired afterwards, so a
 * specialization that refers to a type in another library file resolves. Syntax errors found in a
 * library file are kept in its cache entry and reported through {@link #getDiagnostics()} on every
 * load, not only on the run that parsed it.</p>
 */
public class LibraryLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryLoader.class);

    private final LibraryCache cache;
    private final SysMLv2ParserFacade parserFacade;
    private final StandardLibraryManager standardLibraryManager;
    private final List<LibraryCacheData> loaded = new ArrayList<>();
    private final List<LibraryDiagnostic> diagnostics = new ArrayList<>();

    /** A syntax error in a library file. */
    public static final class LibraryDiagnostic {
        private final String source;
        private final int line;
        private final int column;
        private final String message;

        public LibraryDiagnostic(String source, int line, int column, String message) {
            this.source = source;
            this.line = line;
            this.column = column;
            this.message = message;
        }

        public String getSource() { return source; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return source + ":" + line + ":" + column + ": " + message;
        }
    }

    public LibraryLoader(LibraryCache cache, StandardLibraryManager standardLibraryManager) {
        this.cache = cache;
        this.parserFacade = new SysMLv2ParserFacade();
        this.standardLibraryManager = standardLibraryManager;
    }

    /**
     * Loads all configured libraries into the provided StandardLibraryManager.
     */
    public void loadLibraries(LibraryConfig config) {
        loadBundled(BUNDLED_LINT_LIBRARY);
        for (Path path : config.getLibraryPaths()) {
            loadPath(path);
        }
        for (Path path : config.getDSViewsPaths()) {
            loadPath(path);
        }
        for (LibraryCacheData data : loaded) {
            standardLibraryManager.wireRelationships(data);
        }
        LOGGER.info("Library cache: {} hits, {} hits by checksum, {} parsed",
            cache.getHits(), cache.getRehashedHits(), cache.getMisses());
        if (!diagnostics.isEmpty()) {
            LOGGER.warn("{} syntax error(s) in library files; their symbols may be incomplete", diagnostics.size());
        }
    }

    /** The cache data of every loaded library file, in load order. */
    public List<LibraryCacheData> getLoaded() {
        return Collections.unmodifiableList(loaded);
    }

    /** Syntax errors found in the loaded library files. */
    public List<LibraryDiagnostic> getDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    /** The validator's own metadata library ({@code package Validation}: SuppressWarning, ...). */
    private static final String BUNDLED_LINT_LIBRARY = "metadata/LintConfig.sysml";

    /** Load a library file shipped inside the validator jar. */
    private void loadBundled(String resource) {
        byte[] bytes;
        try (InputStream in = LibraryLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                LOGGER.debug("Bundled library {} not found", resource);
                return;
            }
            bytes = in.readAllBytes();
        } catch (IOException e) {
            LOGGER.warn("Failed to read bundled library {}: {}", resource, e.getMessage());
            return;
        }
        Path virtualPath = Path.of("classpath", resource);
        // The resource has no modification time; its content hash stands in for one.
        long stamp = Arrays.hashCode(bytes);
        LibraryCacheData data = cache.lookup(virtualPath, stamp, bytes.length, () -> bytes);
        if (data == null) {
            String name = virtualPath.getFileName().toString();
            data = parseAndCache(bytes, name, virtualPath, stamp);
        }
        register(data, "classpath:" + resource);
    }

    private void loadPath(Path libraryPath) {
        if (!Files.exists(libraryPath)) {
            LOGGER.warn("Library path does not exist: {}", libraryPath);
            return;
        }

        if (Files.isRegularFile(libraryPath) && libraryPath.toString().endsWith(".kpar")) {
            loadKpar(libraryPath);
        } else if (Files.isDirectory(libraryPath)) {
            loadDirectory(libraryPath);
        } else {
            LOGGER.warn("Unsupported library path type: {}", libraryPath);
        }
    }

    private void loadDirectory(Path dirPath) {
        try (Stream<Path> paths = Files.walk(dirPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".sysml") || p.toString().endsWith(".kerml"))
                 .sorted()
                 .forEach(this::loadFile);
        } catch (IOException e) {
            LOGGER.error("Failed to read directory {}: {}", dirPath, e.getMessage());
        }
    }

    private void loadFile(Path filePath) {
        File file = filePath.toFile();
        long lastModified = file.lastModified();
        long size = file.length();

        LibraryCacheData data = cache.lookup(filePath, lastModified, size, () -> Files.readAllBytes(filePath));
        if (data == null) {
            try {
                byte[] bytes = Files.readAllBytes(filePath);
                data = parseAndCache(bytes, filePath.getFileName().toString(), filePath, lastModified);
            } catch (IOException e) {
                LOGGER.error("Failed to read file {}: {}", filePath, e.getMessage());
                return;
            }
        }

        register(data, filePath.toString());
    }

    private void loadKpar(Path kparPath) {
        File kparFile = kparPath.toFile();
        long kparLastModified = kparFile.lastModified();

        // The cache path for a zip entry will be the kparPath + "!" + entryName
        try (ZipFile zipFile = new ZipFile(kparFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if ((name.endsWith(".sysml") || name.endsWith(".kerml")) && !entry.isDirectory()) {
                    // Create a virtual path for caching purposes
                    Path virtualPath = kparPath.resolve(name);
                    long entryTime = entry.getTime() > 0 ? entry.getTime() : kparLastModified;

                    LibraryCacheData data = cache.lookup(virtualPath, entryTime, entry.getSize(), () -> {
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            return is.readAllBytes();
                        }
                    });
                    if (data == null) {
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            data = parseAndCache(is.readAllBytes(), name, virtualPath, entryTime);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to read {} from {}: {}", name, kparPath, e.getMessage());
                            continue;
                        }
                    }

                    register(data, kparPath + "!" + name);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read KPAR file {}: {}", kparPath, e.getMessage());
        }
    }

    private LibraryCacheData parseAndCache(byte[] bytes, String fileName, Path cacheKeyPath, long lastModified) {
        LOGGER.debug("Parsing library file: {}", fileName);

        String content = new String(bytes, StandardCharsets.UTF_8);
        ParseResult result = parserFacade.parseString(content, fileName);

        LibraryCacheData data = new LibraryCacheData(fileName, lastModified);
        data.setSourcePath(cacheKeyPath.toString());
        data.setSize(bytes.length);
        data.setSha256(LibraryCache.sha256Hex(bytes));

        for (SysMLv2ParserFacade.SyntaxError error : result.getSyntaxErrors()) {
            data.getParseErrors().add(new LibraryCacheData.CachedDiagnostic(
                error.getLine(), error.getCharPositionInLine(), error.getMessage()));
        }

        if (result.getParseTree() != null) {
            // A fresh StandardLibraryManager fulfils the method signature; built-ins are not needed
            // to extract a file's own symbols and the names of what they specialize.
            SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), fileName, new StandardLibraryManager());

            data.setElements(ModelIndexBuilder
                .build(result.getParseTree(), cacheKeyPath.toString(), false).getElements());

            for (Symbol symbol : symbolTable.getAllSymbols()) {
                LibraryCacheData.CachedSymbol cachedSymbol = new LibraryCacheData.CachedSymbol();
                cachedSymbol.setName(symbol.getName());
                cachedSymbol.setQualifiedName(symbol.getQualifiedName());
                cachedSymbol.setType(symbol.getType());
                cachedSymbol.setVisibility(symbol.getVisibility());

                for (Symbol spec : symbol.getSpecializations()) {
                    cachedSymbol.getSpecializations().add(spec.getQualifiedName());
                }
                for (Symbol redef : symbol.getRedefinitions()) {
                    cachedSymbol.getRedefinitions().add(redef.getQualifiedName());
                }
                for (Symbol sub : symbol.getSubsettings()) {
                    cachedSymbol.getSubsettings().add(sub.getQualifiedName());
                }

                data.getSymbols().add(cachedSymbol);
            }
        }

        cache.saveCache(cacheKeyPath, data);
        return data;
    }

    private void register(LibraryCacheData data, String source) {
        if (data == null) {
            return;
        }
        standardLibraryManager.registerSymbols(data);
        loaded.add(data);
        for (LibraryCacheData.CachedDiagnostic error : data.getParseErrors()) {
            LibraryDiagnostic diagnostic =
                new LibraryDiagnostic(source, error.getLine(), error.getColumn(), error.getMessage());
            diagnostics.add(diagnostic);
            LOGGER.warn("Library syntax error {}", diagnostic);
        }
    }
}

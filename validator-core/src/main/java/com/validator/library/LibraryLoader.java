package com.validator.library;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.ParseResult;
import com.validator.semantic.ElementType;
import com.validator.semantic.StandardLibraryManager;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads external libraries by fully parsing them via ANTLR.
 * Utilizes LibraryCache to avoid re-parsing on subsequent runs.
 */
public class LibraryLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryLoader.class);

    private final LibraryCache cache;
    private final SysMLv2ParserFacade parserFacade;
    private final StandardLibraryManager standardLibraryManager;

    public LibraryLoader(LibraryCache cache, StandardLibraryManager standardLibraryManager) {
        this.cache = cache;
        this.parserFacade = new SysMLv2ParserFacade();
        this.standardLibraryManager = standardLibraryManager;
    }

    /**
     * Loads all configured libraries into the provided StandardLibraryManager.
     */
    public void loadLibraries(LibraryConfig config) {
        for (Path path : config.getLibraryPaths()) {
            loadPath(path);
        }
        for (Path path : config.getDSViewsPaths()) {
            loadPath(path);
        }
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
                 .forEach(this::loadFile);
        } catch (IOException e) {
            LOGGER.error("Failed to read directory {}: {}", dirPath, e.getMessage());
        }
    }

    private void loadFile(Path filePath) {
        File file = filePath.toFile();
        long lastModified = file.lastModified();

        LibraryCacheData data = cache.loadCache(filePath, lastModified);
        if (data == null) {
            try {
                String content = Files.readString(filePath, StandardCharsets.UTF_8);
                data = parseAndCache(content, filePath.getFileName().toString(), filePath, lastModified);
            } catch (IOException e) {
                LOGGER.error("Failed to read file {}: {}", filePath, e.getMessage());
                return;
            }
        }
        
        registerCacheData(data);
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
                    
                    LibraryCacheData data = cache.loadCache(virtualPath, kparLastModified);
                    if (data == null) {
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                            data = parseAndCache(content, name, virtualPath, kparLastModified);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to read {} from {}: {}", name, kparPath, e.getMessage());
                            continue;
                        }
                    }
                    
                    registerCacheData(data);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read KPAR file {}: {}", kparPath, e.getMessage());
        }
    }

    private LibraryCacheData parseAndCache(String content, String fileName, Path cacheKeyPath, long lastModified) {
        LOGGER.debug("Parsing library file: {}", fileName);
        
        ParseResult result = parserFacade.parseString(content, fileName);
        
        LibraryCacheData data = new LibraryCacheData(fileName, lastModified);
        
        if (result.getParseTree() != null) {
            // Note: We use a dummy StandardLibraryManager just to fulfill the method signature, 
            // since we don't need it to resolve built-ins for extracting basic symbols and specializations
            SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), fileName, new StandardLibraryManager());
            
            for (Symbol symbol : symbolTable.getAllSymbols()) {
                // Skip generic package definitions that match filename if they have no contents or are redundant
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

    private void registerCacheData(LibraryCacheData data) {
        if (data == null) return;
        
        for (LibraryCacheData.CachedSymbol cs : data.getSymbols()) {
            Symbol symbol = new Symbol(cs.getName(), cs.getQualifiedName(), cs.getType(), null, cs.getVisibility());
            
            // Register it directly with the manager
            standardLibraryManager.registerSymbolInstance(symbol);
        }
    }
}

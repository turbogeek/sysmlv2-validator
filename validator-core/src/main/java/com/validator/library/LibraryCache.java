package com.validator.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles caching of parsed SysML v2 external libraries to speed up subsequent validations.
 */
public class LibraryCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryCache.class);
    private static final String CACHE_DIR_NAME = ".sysml-validator/cache";
    private final Path cacheDir;
    private final ObjectMapper objectMapper;

    public LibraryCache() {
        this.objectMapper = new ObjectMapper();
        
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            this.cacheDir = Paths.get(userHome, CACHE_DIR_NAME);
        } else {
            this.cacheDir = Paths.get(".", CACHE_DIR_NAME);
        }

        try {
            if (!Files.exists(this.cacheDir)) {
                Files.createDirectories(this.cacheDir);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to create cache directory {}: {}", cacheDir, e.getMessage());
        }
    }

    /**
     * Get the cache directory path.
     */
    public Path getCacheDir() {
        return cacheDir;
    }

    /**
     * Tries to load cached data for a given file. Returns null if the cache is missing or stale.
     *
     * @param sourcePath the path to the original source file (e.g., .sysml or .kpar)
     * @param currentLastModified the current last modified timestamp of the source file
     * @return the LibraryCacheData if valid, or null if missing/stale
     */
    public LibraryCacheData loadCache(Path sourcePath, long currentLastModified) {
        File cacheFile = getCacheFile(sourcePath);
        if (!cacheFile.exists()) {
            return null;
        }

        try {
            LibraryCacheData data = objectMapper.readValue(cacheFile, LibraryCacheData.class);
            if (data.getLastModified() == currentLastModified) {
                LOGGER.debug("Cache hit for {}", sourcePath.getFileName());
                return data;
            } else {
                LOGGER.debug("Cache stale for {}", sourcePath.getFileName());
                return null;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read cache file {}: {}", cacheFile, e.getMessage());
            return null;
        }
    }

    /**
     * Saves parsed library data to the cache.
     *
     * @param sourcePath the path to the original source file
     * @param data the parsed cache data to save
     */
    public void saveCache(Path sourcePath, LibraryCacheData data) {
        File cacheFile = getCacheFile(sourcePath);
        try {
            objectMapper.writeValue(cacheFile, data);
            LOGGER.debug("Saved cache for {} to {}", sourcePath.getFileName(), cacheFile.getName());
        } catch (IOException e) {
            LOGGER.warn("Failed to write cache file {}: {}", cacheFile, e.getMessage());
        }
    }

    private File getCacheFile(Path sourcePath) {
        // Create a unique hash for the path to use as the cache filename
        int pathHash = sourcePath.toAbsolutePath().normalize().toString().hashCode();
        String safeName = sourcePath.getFileName().toString().replaceAll("[^a-zA-Z0-9.-]", "_");
        String cacheFileName = safeName + "_" + Integer.toHexString(pathHash) + ".json";
        return cacheDir.resolve(cacheFileName).toFile();
    }
}

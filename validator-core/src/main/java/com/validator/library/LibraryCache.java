package com.validator.library;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validator.parser.SysMLv2Lexer;
import com.validator.parser.SysMLv2Parser;
import com.validator.semantic.SymbolTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Handles caching of parsed SysML v2 external libraries to speed up subsequent validations.
 *
 * <p>Validity rules:</p>
 * <ol>
 *   <li>The entry's cache key must equal {@link #getCacheKey()}. The key covers the cache format, the
 *       generated lexer and parser (so any grammar change) and the symbol-extraction classes, so a
 *       validator upgrade never reuses symbols produced by older code.</li>
 *   <li>Same size and modification time as recorded: reused without reading the source.</li>
 *   <li>Same size but a different modification time (copy, unzip, checkout): the source is hashed; if the
 *       SHA-256 matches, the entry is reused and its recorded time refreshed.</li>
 *   <li>Anything else is a miss and the source is parsed again.</li>
 * </ol>
 *
 * <p>Entries are written to a temporary file and moved into place, so concurrent validator processes
 * never read a half-written entry. The cache directory defaults to {@code ~/.sysml-validator/cache} and
 * can be overridden with the system property {@code sysml.validator.cache.dir}.</p>
 */
public class LibraryCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryCache.class);
    private static final String CACHE_DIR_NAME = ".sysml-validator/cache";
    private static final String CACHE_DIR_PROPERTY = "sysml.validator.cache.dir";

    /** Bump when the layout of {@link LibraryCacheData} or its meaning changes. */
    public static final int FORMAT_VERSION = 2;

    private static volatile String cacheKey;

    private final Path cacheDir;
    private final ObjectMapper objectMapper;

    private int hits;
    private int rehashedHits;
    private int misses;

    public LibraryCache() {
        this(defaultCacheDir());
    }

    public LibraryCache(Path cacheDir) {
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.cacheDir = cacheDir;
        try {
            Files.createDirectories(this.cacheDir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create cache directory {}: {}", cacheDir, e.getMessage());
        }
    }

    /** The default cache directory (system property, else user home, else working directory). */
    public static Path defaultCacheDir() {
        String override = System.getProperty(CACHE_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        String userHome = System.getProperty("user.home");
        return userHome != null ? Paths.get(userHome, CACHE_DIR_NAME) : Paths.get(".", CACHE_DIR_NAME);
    }

    /**
     * Get the cache directory path.
     */
    public Path getCacheDir() {
        return cacheDir;
    }

    /**
     * Key that every valid entry must carry: cache format, grammar and symbol-extraction code.
     */
    public static String getCacheKey() {
        String key = cacheKey;
        if (key == null) {
            MessageDigest md = sha256Digest();
            md.update(("format=" + FORMAT_VERSION + "\n").getBytes(StandardCharsets.UTF_8));
            md.update(SysMLv2Lexer._serializedATN.getBytes(StandardCharsets.UTF_8));
            md.update(SysMLv2Parser._serializedATN.getBytes(StandardCharsets.UTF_8));
            updateWithClassBytes(md, SymbolTableBuilder.class);
            updateWithClassBytes(md, LibraryLoader.class);
            key = HexFormat.of().formatHex(md.digest()).substring(0, 16);
            cacheKey = key;
        }
        return key;
    }

    private static void updateWithClassBytes(MessageDigest md, Class<?> type) {
        String resource = type.getSimpleName() + ".class";
        try (InputStream in = type.getResourceAsStream(resource)) {
            if (in != null) {
                md.update(in.readAllBytes());
            }
        } catch (IOException e) {
            LOGGER.debug("Could not read {} for the cache key: {}", resource, e.getMessage());
        }
    }

    /** Supplies the source content only when a checksum is actually needed. */
    @FunctionalInterface
    public interface ContentSupplier {
        byte[] get() throws IOException;
    }

    /**
     * Looks up a cached entry for a source.
     *
     * @param sourcePath the source file, or the virtual path of an entry inside a .kpar
     * @param lastModified the source's current modification time
     * @param size the source's current size in bytes
     * @param content supplies the source bytes when the checksum must be compared
     * @return the valid entry, or null when the source must be parsed again
     */
    public LibraryCacheData lookup(Path sourcePath, long lastModified, long size, ContentSupplier content) {
        Path cacheFile = getCacheFile(sourcePath);
        if (!Files.exists(cacheFile)) {
            misses++;
            return null;
        }
        LibraryCacheData data;
        try {
            data = objectMapper.readValue(cacheFile.toFile(), LibraryCacheData.class);
        } catch (IOException e) {
            LOGGER.warn("Discarding unreadable cache entry {}: {}", cacheFile, e.getMessage());
            misses++;
            return null;
        }
        if (!getCacheKey().equals(data.getCacheKey())) {
            LOGGER.debug("Cache entry for {} was written by different validator code", sourcePath.getFileName());
            misses++;
            return null;
        }
        if (data.getSize() != size) {
            misses++;
            return null;
        }
        if (data.getLastModified() == lastModified) {
            hits++;
            return data;
        }
        try {
            String sha = sha256Hex(content.get());
            if (sha.equals(data.getSha256())) {
                data.setLastModified(lastModified);
                save(sourcePath, data);
                rehashedHits++;
                LOGGER.debug("Cache hit by checksum for {} (modification time changed)", sourcePath.getFileName());
                return data;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read {} for checksum: {}", sourcePath, e.getMessage());
        }
        misses++;
        return null;
    }

    /**
     * Legacy lookup by modification time only. Prefer {@link #lookup}.
     */
    public LibraryCacheData loadCache(Path sourcePath, long currentLastModified) {
        Path cacheFile = getCacheFile(sourcePath);
        if (!Files.exists(cacheFile)) {
            return null;
        }
        try {
            LibraryCacheData data = objectMapper.readValue(cacheFile.toFile(), LibraryCacheData.class);
            if (getCacheKey().equals(data.getCacheKey()) && data.getLastModified() == currentLastModified) {
                return data;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read cache file {}: {}", cacheFile, e.getMessage());
        }
        return null;
    }

    /**
     * Saves parsed library data to the cache atomically. The cache key is stamped on the entry.
     *
     * @param sourcePath the path to the original source file
     * @param data the parsed cache data to save
     */
    public void saveCache(Path sourcePath, LibraryCacheData data) {
        save(sourcePath, data);
    }

    private void save(Path sourcePath, LibraryCacheData data) {
        data.setCacheKey(getCacheKey());
        Path cacheFile = getCacheFile(sourcePath);
        Path tmp = null;
        try {
            tmp = Files.createTempFile(cacheDir, cacheFile.getFileName().toString(), ".tmp");
            objectMapper.writeValue(tmp.toFile(), data);
            try {
                Files.move(tmp, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, cacheFile, StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.debug("Saved cache for {} to {}", sourcePath.getFileName(), cacheFile.getFileName());
        } catch (IOException e) {
            LOGGER.warn("Failed to write cache file {}: {}", cacheFile, e.getMessage());
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    LOGGER.debug("Could not delete {}", tmp);
                }
            }
        }
    }

    /**
     * Deletes every entry in this cache directory.
     *
     * @return the number of entries deleted
     */
    public int clear() {
        int deleted = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(cacheDir, "*.{json,tmp}")) {
            for (Path entry : entries) {
                Files.deleteIfExists(entry);
                deleted++;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to clear cache {}: {}", cacheDir, e.getMessage());
        }
        hits = 0;
        rehashedHits = 0;
        misses = 0;
        return deleted;
    }

    public int getHits() { return hits; }
    public int getRehashedHits() { return rehashedHits; }
    public int getMisses() { return misses; }

    /** SHA-256 of the given bytes as lower-case hex. */
    public static String sha256Hex(byte[] bytes) {
        return HexFormat.of().formatHex(sha256Digest().digest(bytes));
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private Path getCacheFile(Path sourcePath) {
        String absolute = sourcePath.toAbsolutePath().normalize().toString();
        String pathHash = sha256Hex(absolute.getBytes(StandardCharsets.UTF_8)).substring(0, 16);
        String safeName = sourcePath.getFileName().toString().replaceAll("[^a-zA-Z0-9.-]", "_");
        return cacheDir.resolve(safeName + "_" + pathHash + ".json");
    }
}

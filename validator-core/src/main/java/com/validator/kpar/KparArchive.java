package com.validator.kpar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * A model packaged as a KerML Project Archive (KPAR): a zip file with the project description
 * {@code .project.json} at its root, usually a {@code .meta.json} (index and checksums), and the model files
 * ({@code .sysml}, {@code .kerml}) at any path inside it.
 *
 * <p>{@link #read(Path)} keeps the project's name, version and description and the model files, in archive
 * order, with their text. Other entries, {@code .meta.json} included, are not read. Diagnostics of a member
 * carry the file path {@code <archive path>!<member path>} ({@link #memberPath(String, String)}).</p>
 */
public final class KparArchive {

    /** The project description every KPAR has at its root. */
    public static final String PROJECT_FILE = ".project.json";

    /** Separates the archive from the member in the file path of a member. */
    public static final String MEMBER_SEPARATOR = "!";

    private static final String EXTENSION = ".kpar";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String name;
    private final String version;
    private final String description;
    private final List<Member> members;

    /**
     * A model file in an archive.
     *
     * @param path the path of the file inside the archive, with '/' between folders
     * @param text the file's text, decoded as UTF-8
     */
    public record Member(String path, String text) {

        /** The member's file name, without the folders it is in inside the archive. */
        public String fileName() {
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }

    /** The file is not a KPAR: not a zip, no {@code .project.json}, no model file, or unreadable content. */
    public static final class InvalidKparException extends IOException {
        private static final long serialVersionUID = 1L;

        /**
         * An invalid archive.
         *
         * @param message what is wrong with it
         */
        public InvalidKparException(String message) {
            super(message);
        }

        /**
         * An invalid archive, found by an exception.
         *
         * @param message what is wrong with it
         * @param cause the exception that found it
         */
        public InvalidKparException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private KparArchive(String name, String version, String description, List<Member> members) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.members = Collections.unmodifiableList(members);
    }

    /**
     * Read a KPAR.
     *
     * @param archive the archive file
     * @return the project and its model files
     * @throws InvalidKparException if the file is not a zip, has no {@code .project.json} at its root, its
     *     {@code .project.json} is not a JSON object, it has no {@code .sysml} or {@code .kerml} file, or a
     *     model file is not UTF-8 text
     * @throws IOException if the file cannot be read
     */
    public static KparArchive read(Path archive) throws IOException {
        ZipFile zip;
        try {
            zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8);
        } catch (ZipException e) {
            throw new InvalidKparException("not a zip archive (" + e.getMessage() + ")", e);
        }
        try (zip) {
            return read(zip);
        } catch (ZipException | EOFException e) {
            throw new InvalidKparException("corrupted zip entry (" + e.getMessage() + ")", e);
        }
    }

    private static KparArchive read(ZipFile zip) throws IOException {
        byte[] project = null;
        List<Member> models = new ArrayList<>();
        for (ZipEntry entry : Collections.list(zip.entries())) {
            String path = entry.getName();
            if (entry.isDirectory()) {
                continue;
            }
            if (PROJECT_FILE.equals(path)) {
                project = bytes(zip, entry);
            } else if (isModelFile(path)) {
                models.add(new Member(path, text(path, bytes(zip, entry))));
            }
        }
        if (project == null) {
            throw new InvalidKparException("no " + PROJECT_FILE + " at the root of the archive");
        }
        JsonNode json = project(project);
        if (models.isEmpty()) {
            throw new InvalidKparException("no model file (.sysml or .kerml) in the archive");
        }
        return new KparArchive(field(json, "name"), field(json, "version"), field(json, "description"), models);
    }

    private static boolean isModelFile(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".sysml") || lower.endsWith(".kerml");
    }

    private static byte[] bytes(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }

    /** Decode strictly, as {@code Files.readString} does for a model file outside an archive. */
    private static String text(String path, byte[] bytes) throws InvalidKparException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException e) {
            throw new InvalidKparException(path + " is not UTF-8 text", e);
        }
    }

    private static JsonNode project(byte[] bytes) throws InvalidKparException {
        JsonNode json;
        try {
            json = JSON.readTree(bytes);
        } catch (IOException e) {
            throw new InvalidKparException(PROJECT_FILE + " is not valid JSON", e);
        }
        if (json == null || !json.isObject()) {
            throw new InvalidKparException(PROJECT_FILE + " is not a JSON object");
        }
        return json;
    }

    private static String field(JsonNode json, String key) {
        JsonNode value = json.get(key);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    /** The project's name from {@code .project.json}, or null if it has none. */
    public String getName() {
        return name;
    }

    /** The project's version from {@code .project.json}, or null if it has none. */
    public String getVersion() {
        return version;
    }

    /** The project's description from {@code .project.json}, or null if it has none. */
    public String getDescription() {
        return description;
    }

    /** The model files ({@code .sysml}, {@code .kerml}) in archive order; never empty. */
    public List<Member> getMembers() {
        return members;
    }

    /**
     * The file path the diagnostics of a member carry.
     *
     * @param archive the archive's path
     * @param member the member's path inside the archive
     * @return {@code <archive>!<member>}
     */
    public static String memberPath(String archive, String member) {
        return archive + MEMBER_SEPARATOR + member;
    }

    /**
     * The member part of a file path made by {@link #memberPath(String, String)}.
     *
     * @param filePath a diagnostic's file path
     * @return the member's path inside the archive, or null if the path is not in a {@code .kpar}
     */
    public static String memberOf(String filePath) {
        int at = separator(filePath);
        return at < 0 ? null : filePath.substring(at + MEMBER_SEPARATOR.length());
    }

    /**
     * The archive part of a file path made by {@link #memberPath(String, String)}.
     *
     * @param filePath a diagnostic's file path
     * @return the archive's path, or the path itself if it is not in a {@code .kpar}
     */
    public static String archiveOf(String filePath) {
        int at = separator(filePath);
        return at < 0 ? filePath : filePath.substring(0, at);
    }

    /** The index of the first '!' that follows ".kpar" (any case), or -1. */
    private static int separator(String filePath) {
        int at = filePath.indexOf(MEMBER_SEPARATOR);
        while (at >= 0) {
            int start = at - EXTENSION.length();
            if (start >= 0 && filePath.regionMatches(true, start, EXTENSION, 0, EXTENSION.length())) {
                return at;
            }
            at = filePath.indexOf(MEMBER_SEPARATOR, at + 1);
        }
        return -1;
    }
}

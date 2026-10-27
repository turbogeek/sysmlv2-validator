package com.validator.testutil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes KPARs (KerML Project Archives) for tests, so no binary archive is kept in the repository.
 */
public final class Kpars {

    private Kpars() {
    }

    /** A {@code .project.json} for a project with the given name and version. */
    public static String project(String name, String version) {
        return "{\"name\":\"" + name + "\",\"description\":\"Test model\",\"version\":\"" + version
            + "\",\"usage\":[]}";
    }

    /**
     * Write a KPAR with a {@code .project.json} and the given model files.
     *
     * @param file the archive to write
     * @param members path inside the archive to text, in archive order
     * @return the archive
     */
    public static Path model(Path file, Map<String, String> members) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(".project.json", project("Test", "1.0.0"));
        entries.putAll(members);
        return write(file, entries);
    }

    /**
     * Write a zip with the given entries as UTF-8 text; a name ending in '/' is a folder.
     *
     * @param file the archive to write
     * @param entries path inside the archive to text, in archive order
     * @return the archive
     */
    public static Path write(Path file, Map<String, String> entries) throws IOException {
        Map<String, byte[]> bytes = new LinkedHashMap<>();
        entries.forEach((name, text) -> bytes.put(name, text.getBytes(StandardCharsets.UTF_8)));
        return writeBytes(file, bytes);
    }

    /**
     * Write a zip with the given entries.
     *
     * @param file the archive to write
     * @param entries path inside the archive to content, in archive order
     * @return the archive
     */
    public static Path writeBytes(Path file, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return file;
    }
}

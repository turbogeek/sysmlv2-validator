package com.validator.cli.compare;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * A list of models run by several tools (conformance/manifest.csv, perf/corpus/manifest.csv): one row per model,
 * with its size and checksum so that every machine can tell it ran the same file.
 *
 * <p>A path is relative to the repository root, or starts with {@code release:} for a file of the OMG
 * SysML-v2-Release checkout (not copied into this repository); such entries are skipped where the release is
 * not available.</p>
 */
public final class Manifest {

    /** The manifest's header row. */
    public static final String HEADER = "id,path,family,scale,expect,lines,bytes,sha256,description";
    /** Prefix of a path inside the SysML-v2-Release checkout. */
    public static final String RELEASE_PREFIX = "release:";

    private Manifest() {
    }

    /**
     * One model.
     *
     * @param id short unique name, used in the results
     * @param path where the model is (see the class comment)
     * @param family what the model exercises: system, inheritance, imports, errors, real
     * @param scale its size class or depth, e.g. "m" or "200"
     * @param expect "clean", "warnings" or "errors": what the reference implementation reports for it
     * @param lines number of lines
     * @param bytes size in bytes
     * @param sha256 SHA-256 of the file, hex
     * @param description one sentence, without commas
     */
    public record Entry(String id, String path, String family, String scale, String expect, long lines, long bytes,
            String sha256, String description) {

        /** True for a file of the SysML-v2-Release checkout. */
        public boolean isRelease() {
            return path.startsWith(RELEASE_PREFIX);
        }

        /**
         * The file on this machine.
         *
         * @param repoRoot the repository root
         * @param releaseRoot the SysML-v2-Release checkout, or null if there is none
         * @return the file, or null for a release file without a release checkout
         */
        public Path resolve(Path repoRoot, Path releaseRoot) {
            if (isRelease()) {
                return releaseRoot == null ? null : releaseRoot.resolve(path.substring(RELEASE_PREFIX.length()));
            }
            return repoRoot.resolve(path);
        }

        String toCsv() {
            return String.join(",", id, path, family, scale, expect, Long.toString(lines), Long.toString(bytes),
                sha256, description);
        }
    }

    /**
     * Read a manifest.
     *
     * @param manifest the CSV file
     * @return its entries, in file order
     * @throws IOException if it cannot be read or a row is malformed
     */
    public static List<Entry> read(Path manifest) throws IOException {
        List<Entry> entries = new ArrayList<>();
        List<String> lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // The description is last and may hold commas
            String[] f = line.split(",", 9);
            if (f.length != 9) {
                throw new IOException(manifest + ":" + (i + 1) + ": expected 9 columns, found " + f.length);
            }
            entries.add(new Entry(f[0], f[1], f[2], f[3], f[4], Long.parseLong(f[5]), Long.parseLong(f[6]), f[7],
                f[8]));
        }
        return entries;
    }

    /**
     * Write a manifest.
     *
     * @param manifest the CSV file
     * @param entries the entries
     * @throws IOException if it cannot be written
     */
    public static void write(Path manifest, List<Entry> entries) throws IOException {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (Entry e : entries) {
            sb.append(e.toCsv()).append('\n');
        }
        Files.writeString(manifest, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * An entry for an existing file, with its size and checksum.
     *
     * @param file the file
     * @param id the entry's id
     * @param path the entry's path as written in the manifest
     * @param family the model family
     * @param scale the size class
     * @param expect clean, warnings or errors
     * @param description one sentence, without commas
     * @return the entry
     * @throws IOException if the file cannot be read
     */
    public static Entry describe(Path file, String id, String path, String family, String scale, String expect,
            String description) throws IOException {
        byte[] content = Files.readAllBytes(file);
        return new Entry(id, path, family, scale, expect, countLines(content), content.length, sha256(content),
            description.replace(',', ';'));
    }

    static long countLines(byte[] content) {
        long lines = 0;
        for (byte b : content) {
            if (b == '\n') {
                lines++;
            }
        }
        return content.length > 0 && content[content.length - 1] != '\n' ? lines + 1 : lines;
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

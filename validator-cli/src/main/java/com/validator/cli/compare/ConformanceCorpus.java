package com.validator.cli.compare;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes the conformance corpus: conformance/manifest.csv, which lists the repository's test files, and the KPARs
 * of conformance/kpar. Cameo, the pilot implementation and this validator load every entry, and
 * {@link ConformanceComparison} compares what they report.
 *
 * <p>The KPARs are written uncompressed with fixed timestamps, so the same input gives the same bytes on every
 * machine and the committed archives can be checked against this generator.</p>
 */
public final class ConformanceCorpus {

    /** Test folders and the family, and expected outcome, of their files. */
    static final String[][] FOLDERS = {
        {"test-suite/positive", "positive", "clean"},
        {"test-suite/negative", "negative", "errors"},
        {"test-suite/lint", "lint", "-"},
        {"test-suite/CheatSheet", "cheatsheet", "-"},
        {"test-suite/DSLinSysML", "dsl", "-"},
        {"test-suite/DassaultLamguageExample", "dassault", "-"},
        {"examples/name-collision/pilot-repro/models", "pilot-repro", "-"},
    };
    /** Single test files: path, id, expected outcome. */
    static final String[][] FILES = {
        {"test-suite/test_extend.sysml", "misc/test_extend", "-"},
        {"examples/name-collision/NameCollisionExamples.sysml", "examples/NameCollisionExamples", "warnings"},
        {"examples/name-collision/NameCollisionErrorForm.sysml", "examples/NameCollisionErrorForm", "errors"},
        {"examples/name-collision/MassTypedByMass.sysml", "examples/MassTypedByMass", "errors"},
        {"examples/16_medical_device_stability_v2.sysml", "examples/16_medical_device_stability_v2", "-"},
        {"examples/lint_metadata_examples.sysml", "examples/lint_metadata_examples", "-"},
        {"examples/lint_test.sysml", "examples/lint_test", "-"},
    };
    /** Files whose expected outcome is not their folder's, by id. */
    static final Map<String, String> EXPECT = Map.of(
        // An older copy of the specification's vehicle model (real-vehicle-legacy in perf/corpus)
        "positive/12_simple_vehicle_model", "errors");
    /** The multi-file StockTicker model, packaged as one KPAR. */
    static final String STOCKTICKER = "test-suite/stockticker";

    static final String VEHICLE = """
        package VehicleDefinitions {
            private import ScalarValues::*;
            part def Vehicle {
                attribute mass :> ISQ::mass;
                attribute wheels : Integer;
            }
        }
        """;
    static final String CAR = """
        package Cars {
            private import VehicleDefinitions::*;
            part car : Vehicle {
                attribute :>> mass = 1500 [SI::kg];
                attribute :>> wheels = 4;
            }
        }
        """;
    /**
     * A fixed time for archive entries. Not 1980-01-01 00:00, the earliest DOS time: for that one ZipEntry also
     * writes the time in the machine's time zone, so the bytes would differ between machines.
     */
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final String CREATED = "2026-10-27T00:00:00Z";
    private static final Pattern TOP_PACKAGE = Pattern.compile(
        "(?m)^(?:standard\\s+)?(?:library\\s+)?package\\s+('(?:[^'\\\\]|\\\\.)*'|[A-Za-z_][A-Za-z0-9_]*)");

    private ConformanceCorpus() {
    }

    /**
     * Write conformance/kpar/*.kpar and conformance/manifest.csv.
     *
     * @param repoRoot the repository root
     * @param conformanceDir the conformance folder (inside the repository)
     * @return the manifest entries
     * @throws IOException if a file cannot be read or written
     */
    public static List<Manifest.Entry> generate(Path repoRoot, Path conformanceDir) throws IOException {
        Path root = repoRoot.toAbsolutePath().normalize();
        List<Manifest.Entry> entries = new ArrayList<>();
        for (String[] folder : FOLDERS) {
            Path dir = root.resolve(folder[0]);
            for (Path file : sysmlFiles(dir)) {
                String relative = dir.relativize(file).toString().replace('\\', '/');
                String id = folder[1] + "/" + relative.substring(0, relative.length() - ".sysml".length());
                entries.add(Manifest.describe(file, id, path(root, file), folder[1], "-",
                    EXPECT.getOrDefault(id, folder[2]), "A test file of " + folder[0]));
            }
        }
        for (String[] single : FILES) {
            Path file = root.resolve(single[0]);
            entries.add(Manifest.describe(file, single[1], single[0], single[1].substring(0, single[1].indexOf('/')),
                "-", single[2], "A test file"));
        }

        Path kparDir = conformanceDir.resolve("kpar");
        Files.createDirectories(kparDir);
        Map<String, String> stockticker = new LinkedHashMap<>();
        for (Path file : sysmlFiles(root.resolve(STOCKTICKER))) {
            stockticker.put(file.getFileName().toString(), Files.readString(file, StandardCharsets.UTF_8));
        }
        entries.add(kpar(kparDir, "stockticker", "StockTicker", stockticker, "-",
            "The 12 files of the StockTicker model of test-suite/stockticker as one KPAR"));
        entries.add(kpar(kparDir, "name-collision-examples", "Name collision examples",
            Map.of("NameCollisionExamples.sysml", Files.readString(
                root.resolve("examples/name-collision/NameCollisionExamples.sysml"), StandardCharsets.UTF_8)),
            "warnings", "The name-collision examples as a KPAR with one member"));
        Map<String, String> twoMembers = new LinkedHashMap<>();
        twoMembers.put("Vehicle.sysml", VEHICLE);
        twoMembers.put("Car.sysml", CAR);
        entries.add(kpar(kparDir, "two-members", "Two members", twoMembers, "clean",
            "A car in one member typed by a vehicle defined in the other"));
        Map<String, String> withError = new LinkedHashMap<>(twoMembers);
        withError.put("Car.sysml", CAR.replace(": Vehicle {", ": Vehicel {"));
        entries.add(kpar(kparDir, "two-members-error", "Two members with an error", withError, "errors",
            "As two-members with a misspelled type in the second member (Car.sysml line 3)"));

        Path noProject = kparDir.resolve("invalid-no-project.kpar");
        Files.write(noProject, zip(new LinkedHashMap<>(Map.of("Vehicle.sysml", VEHICLE))));
        entries.add(Manifest.describe(noProject, "kpar/invalid-no-project", kparPath(noProject), "kpar", "-",
            "invalid", "A zip with a model file but no .project.json"));
        Path notZip = kparDir.resolve("invalid-not-a-zip.kpar");
        Files.writeString(notZip, VEHICLE, StandardCharsets.UTF_8);
        entries.add(Manifest.describe(notZip, "kpar/invalid-not-a-zip", kparPath(notZip), "kpar", "-",
            "invalid", "A text file named .kpar"));

        Manifest.write(conformanceDir.resolve("manifest.csv"), entries);
        return entries;
    }

    private static Manifest.Entry kpar(Path dir, String name, String title, Map<String, String> members,
            String expect, String description) throws IOException {
        Path file = dir.resolve(name + ".kpar");
        Files.write(file, kpar(title, members));
        Manifest.Entry e = Manifest.describe(file, "kpar/" + name, kparPath(file), "kpar", "-", expect,
            description);
        // The lines of a KPAR are those of its members
        long lines = 0;
        for (String text : members.values()) {
            lines += Manifest.countLines(text.getBytes(StandardCharsets.UTF_8));
        }
        return new Manifest.Entry(e.id(), e.path(), e.family(), e.scale(), e.expect(), lines, e.bytes(), e.sha256(),
            e.description());
    }

    /**
     * A KPAR: .project.json, .meta.json (index of top-level packages and SHA-256 of each member), then the members.
     *
     * @param title the project name
     * @param members member path to text, in archive order
     * @return the archive's bytes
     * @throws IOException never, in practice (in-memory)
     */
    static byte[] kpar(String title, Map<String, String> members) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".project.json", "{\"name\":" + json(title) + ",\"version\":\"1.0.0\",\"usage\":[]}");
        StringBuilder index = new StringBuilder();
        StringBuilder checksum = new StringBuilder();
        for (Map.Entry<String, String> m : members.entrySet()) {
            Matcher top = TOP_PACKAGE.matcher(m.getValue());
            while (top.find()) {
                String packageName = top.group(1).startsWith("'")
                    ? top.group(1).substring(1, top.group(1).length() - 1) : top.group(1);
                index.append(index.length() == 0 ? "" : ",").append(json(packageName)).append(':')
                    .append(json(m.getKey()));
            }
            checksum.append(checksum.length() == 0 ? "" : ",").append(json(m.getKey())).append(":{\"value\":")
                .append(json(Manifest.sha256(m.getValue().getBytes(StandardCharsets.UTF_8))))
                .append(",\"algorithm\":\"SHA256\"}");
        }
        files.put(".meta.json", "{\"index\":{" + index + "},\"created\":\"" + CREATED
            + "\",\"metamodel\":\"https://www.omg.org/spec/SysML/20250201\",\"checksum\":{" + checksum + "}}");
        files.putAll(members);
        return zip(files);
    }

    /** An uncompressed zip with fixed entry times: the same bytes for the same files everywhere. */
    static byte[] zip(Map<String, String> files) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> f : files.entrySet()) {
                byte[] content = f.getValue().getBytes(StandardCharsets.UTF_8);
                ZipEntry entry = new ZipEntry(f.getKey());
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(content.length);
                entry.setCompressedSize(content.length);
                CRC32 crc = new CRC32();
                crc.update(content);
                entry.setCrc(crc.getValue());
                entry.setTimeLocal(ENTRY_TIME);
                zip.putNextEntry(entry);
                zip.write(content);
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static List<Path> sysmlFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(p -> Files.isRegularFile(p)
                    && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sysml"))
                .sorted((a, b) -> a.toString().replace('\\', '/').compareTo(b.toString().replace('\\', '/')))
                .toList();
        }
    }

    /** The manifest path of a KPAR: in conformance/kpar of the repository, wherever it was written. */
    private static String kparPath(Path file) {
        return CompareMain.CONFORMANCE + "/kpar/" + file.getFileName();
    }

    private static String path(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String json(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}

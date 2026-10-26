package com.validator.cli.perf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the generated models of the performance corpus (perf/cameo/corpus) and its manifest. The models are
 * deterministic, use only mainstream SysML v2 constructs, and are clean in the SysML v2 Pilot Implementation,
 * so that a slow or failing import measures the tool, not the model.
 *
 * <ul>
 *   <li>{@code system-*}: subsystems of parts, ports, actions, states and requirements, wired into one system,
 *       from 2 to 2,500 subsystems;</li>
 *   <li>{@code inheritance-200}: a chain of 200 part definitions, each redefining the first one's attribute;</li>
 *   <li>{@code imports-100}: 100 packages, each publicly importing the previous one;</li>
 *   <li>{@code errors-m}: the 200-subsystem system with an unresolved type, a re-declared inherited attribute and
 *       a misspelled redefinition in every tenth subsystem.</li>
 * </ul>
 */
public final class PerfCorpusGenerator {

    /** Subsystems per size class of the system family. */
    static final String[][] SYSTEM_SIZES = {{"xs", "2"}, {"s", "20"}, {"m", "200"}, {"l", "1000"}, {"xl", "2500"}};
    /** Depth of the inheritance chain. */
    static final int INHERITANCE_DEPTH = 200;
    /** Number of packages in the import chain. */
    static final int IMPORT_LAYERS = 100;
    /** Every n-th subsystem of the errors model has errors. */
    static final int ERROR_EVERY = 10;

    /** Models of the repository and of the SysML-v2-Release that are timed too: id, path, expect, description. */
    static final String[][] EXISTING = {
        {"real-cameo-constructs", "test-suite/positive/60_cameo_26xR2_constructs.sysml", "clean",
            "Constructs used by the CATIA Magic 26xR2 libraries and Cameo-exported models"},
        {"real-name-collisions", "examples/name-collision/NameCollisionExamples.sysml", "warnings",
            "Name-collision pitfalls next to their fixes (warnings only)"},
        {"real-mass-typed-by-mass", "examples/name-collision/MassTypedByMass.sysml", "errors",
            "A car mass typed by a usage: three typing errors"},
        {"real-vehicle-legacy", "test-suite/positive/12_simple_vehicle_model.sysml", "errors",
            "An older copy of the specification's vehicle model; the pilot reports 88 errors"},
        {"release-annex-a-vehicle", PerfManifest.RELEASE_PREFIX
            + "sysml/src/examples/Vehicle Example/SysML v2 Spec Annex A SimpleVehicleModel.sysml", "clean",
            "The specification's Annex A vehicle model (needs a SysML-v2-Release checkout)"},
    };

    private PerfCorpusGenerator() {
    }

    /**
     * Write the generated models into {@code corpusDir} and the manifest of all models.
     *
     * @param repoRoot the repository root
     * @param corpusDir where the generated models go (inside the repository)
     * @param releaseRoot the SysML-v2-Release checkout, or null; release entries need it for their checksum
     * @return the manifest entries
     * @throws IOException if a file cannot be written or read
     */
    public static List<PerfManifest.Entry> generate(Path repoRoot, Path corpusDir, Path releaseRoot)
            throws IOException {
        Files.createDirectories(corpusDir);
        String prefix = repoRoot.toAbsolutePath().normalize().relativize(corpusDir.toAbsolutePath().normalize())
            .toString().replace('\\', '/') + "/";
        List<PerfManifest.Entry> entries = new ArrayList<>();
        for (String[] size : SYSTEM_SIZES) {
            int n = Integer.parseInt(size[1]);
            String id = "system-" + size[0];
            entries.add(write(corpusDir, prefix, id, system("PerfSystem_" + size[0], n, false), "system", size[0],
                "clean", n + " subsystems of parts; ports; actions; states and requirements wired into one system"));
        }
        entries.add(write(corpusDir, prefix, "inheritance-" + INHERITANCE_DEPTH, inheritance(INHERITANCE_DEPTH),
            "inheritance", Integer.toString(INHERITANCE_DEPTH), "clean",
            "A chain of " + INHERITANCE_DEPTH + " part definitions each redefining the first one's attribute"));
        entries.add(write(corpusDir, prefix, "imports-" + IMPORT_LAYERS, imports(IMPORT_LAYERS), "imports",
            Integer.toString(IMPORT_LAYERS), "clean",
            IMPORT_LAYERS + " packages each publicly importing the previous one"));
        // Keep the size and checksum of a model that is not on this machine (a release file without a checkout)
        Path manifestFile = corpusDir.resolve("manifest.csv");
        List<PerfManifest.Entry> previous = Files.isRegularFile(manifestFile) ? PerfManifest.read(manifestFile)
            : List.of();
        int errorSize = Integer.parseInt(SYSTEM_SIZES[2][1]);
        entries.add(write(corpusDir, prefix, "errors-m", system("PerfErrors_m", errorSize, true), "errors", "m",
            "errors", "The 200-subsystem system with three errors in every tenth subsystem"));
        for (String[] existing : EXISTING) {
            PerfManifest.Entry probe = new PerfManifest.Entry(existing[0], existing[1], "real", "-", existing[2], 0,
                0, "-", existing[3]);
            Path file = probe.resolve(repoRoot, releaseRoot);
            if (file != null && Files.isRegularFile(file)) {
                entries.add(PerfManifest.describe(file, existing[0], existing[1], "real", "-", existing[2],
                    existing[3]));
            } else {
                entries.add(previous.stream().filter(p -> p.id().equals(probe.id()) && p.path().equals(probe.path()))
                    .findFirst().orElse(probe));
            }
        }
        PerfManifest.write(manifestFile, entries);
        return entries;
    }

    private static PerfManifest.Entry write(Path dir, String prefix, String id, String text, String family,
            String scale, String expect, String description) throws IOException {
        Path file = dir.resolve(id + ".sysml");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return PerfManifest.describe(file, id, prefix + id + ".sysml", family, scale, expect, description);
    }

    /**
     * A system of {@code subsystems} subsystems: a sensor and a controller definition each, a mass requirement,
     * and one usage of every controller in the system, power-linked to the next.
     *
     * @param name the root package's name
     * @param subsystems how many subsystems, at least 2
     * @param withErrors put three errors into every tenth subsystem
     * @return the model text
     */
    static String system(String name, int subsystems, boolean withErrors) {
        StringBuilder sb = new StringBuilder(subsystems * 1100 + 4000);
        sb.append("package ").append(name).append(" {\n");
        sb.append("    doc /* Generated performance-test model (PerfCorpusGenerator): ").append(subsystems)
            .append(" subsystems").append(withErrors ? ", three errors in every " + ERROR_EVERY + "th." : ".")
            .append(" */\n");
        sb.append("""
                private import ScalarValues::*;
                private import ISQ::*;
                private import SI::*;

                package Common {
                    enum def Health { enum ok; enum degraded; enum failed; }
                    item def Packet {
                        attribute size : Integer;
                        attribute priority : Integer default 0;
                    }
                    port def PowerPort {
                        out attribute supplyVoltage : Real;
                    }
                    port def DataPort {
                        in item packet : Packet;
                    }
                    interface def PowerLink {
                        end source : PowerPort;
                        end sink : ~PowerPort;
                    }
                    abstract part def Component {
                        attribute mass :> ISQ::mass;
                        attribute cost : Real;
                        attribute health : Health default Health::ok;
                        port power : ~PowerPort;
                    }
                    action def Process {
                        in input : Packet;
                        out output : Packet;
                    }
                    calc def TotalMass {
                        in masses : MassValue[0..*];
                        return total : MassValue = NumericalFunctions::sum(masses);
                    }
                    requirement def MassLimit {
                        subject component : Component;
                        attribute limit : MassValue;
                        require constraint { component.mass <= limit }
                    }
                    state def Lifecycle {
                        entry; then off;
                        state off;
                        state on;
                        transition powerOn first off then on;
                        transition powerOff first on then off;
                    }
                }
            """);
        for (int i = 1; i <= subsystems; i++) {
            subsystem(sb, i, withErrors && i % ERROR_EVERY == 0);
        }
        sb.append("\n    part def System {\n");
        for (int i = 1; i <= subsystems; i++) {
            sb.append(String.format(Locale.ROOT,
                "        part controller_%1$04d : Subsystem_%1$04d::Controller_%1$04d;\n", i));
        }
        for (int i = 1; i < subsystems; i++) {
            sb.append(String.format(Locale.ROOT, "        interface link_%1$04d_%2$04d : Common::PowerLink connect\n"
                + "            source ::> controller_%1$04d.supply to sink ::> controller_%2$04d.power;\n", i, i + 1));
        }
        int summed = Math.min(subsystems, 8);
        sb.append("        attribute totalMass : MassValue = Common::TotalMass((");
        for (int i = 1; i <= summed; i++) {
            sb.append(i > 1 ? ", " : "").append(String.format(Locale.ROOT, "controller_%04d.mass", i));
        }
        sb.append("));\n    }\n    part system : System;\n}\n");
        return sb.toString();
    }

    private static void subsystem(StringBuilder sb, int i, boolean withErrors) {
        String mass = String.format(Locale.ROOT, "%.2f", 1.0 + (i % 7) * 0.1);
        String sensorMass = String.format(Locale.ROOT, "%.2f", 0.2 + (i % 5) * 0.01);
        sb.append(String.format(Locale.ROOT, """

                package Subsystem_%1$04d {
                    part def Sensor_%1$04d :> Common::Component {
                        attribute :>> mass = %3$s [kg];
                        attribute :>> cost = %4$d.0;
                        attribute range : Real = %5$d.0;
                        port data : Common::%6$s;
                    }
                    part def Controller_%1$04d :> Common::Component {
                        attribute %7$s = %2$s [kg];
                        attribute :>> %8$s = %9$d.0;
                        port supply : Common::PowerPort;
                        part sensors : Sensor_%1$04d[2];
                        action smooth : Common::Process;
                        action forward : Common::Process;
                        succession first smooth then forward;
                        exhibit state lifecycle : Common::Lifecycle;
                    }
                    requirement controllerMass_%1$04d : Common::MassLimit {
                        subject component : Controller_%1$04d;
                        attribute :>> limit = 2.0 [kg];
                    }
                }
            """, i, mass, sensorMass, 30 + i % 20, 100 + i % 50,
            withErrors ? "DataPrt" : "DataPort",
            withErrors ? "mass" : ":>> mass",
            withErrors ? "costt" : "cost",
            200 + i % 100));
    }

    /**
     * A chain of part definitions, each specializing the previous one, redefining the first one's attribute
     * and adding its own, and a usage of the last one.
     *
     * @param depth the number of definitions
     * @return the model text
     */
    static String inheritance(int depth) {
        StringBuilder sb = new StringBuilder(depth * 160 + 500);
        sb.append("package PerfInheritance_").append(depth).append(" {\n");
        sb.append("    doc /* Generated performance-test model (PerfCorpusGenerator): an inheritance chain of ")
            .append(depth).append(" part definitions. */\n");
        sb.append("    private import ScalarValues::*;\n\n");
        sb.append("    part def Level_0000 {\n        attribute value_0000 : Real default 0.0;\n    }\n");
        for (int i = 1; i < depth; i++) {
            sb.append(String.format(Locale.ROOT, "    part def Level_%04d :> Level_%04d {\n"
                + "        attribute :>> value_0000 default %d.0;\n"
                + "        attribute value_%04d : Real default 0.0;\n    }\n", i, i - 1, i, i));
        }
        sb.append(String.format(Locale.ROOT, "\n    part deepest : Level_%04d {\n"
            + "        attribute :>> value_0000 = 42.0;\n"
            + "        attribute :>> value_%04d = 7.0;\n    }\n}\n", depth - 1, Math.max(depth - 2, 0)));
        return sb.toString();
    }

    /**
     * A chain of packages, each publicly importing the previous one, defining attribute definitions that
     * specialize the previous package's, and a part definition typed by types of the previous package and of
     * the package ten layers down (reached only through the chain of imports).
     *
     * @param layers the number of packages
     * @return the model text
     */
    static String imports(int layers) {
        StringBuilder sb = new StringBuilder(layers * 700 + 500);
        sb.append("package PerfImports_").append(layers).append(" {\n");
        sb.append("    doc /* Generated performance-test model (PerfCorpusGenerator): ").append(layers)
            .append(" packages; each publicly imports the previous one. */\n");
        sb.append("    private import ScalarValues::*;\n");
        for (int k = 0; k < layers; k++) {
            sb.append(String.format(Locale.ROOT, "\n    package Layer_%04d {\n", k));
            if (k > 0) {
                sb.append(String.format(Locale.ROOT, "        public import Layer_%04d::*;\n", k - 1));
            }
            for (int j = 0; j < 10; j++) {
                sb.append(String.format(Locale.ROOT, "        attribute def Quantity_%04d_%02d", k, j));
                sb.append(k > 0 ? String.format(Locale.ROOT, " :> Quantity_%04d_%02d", k - 1, j) : " :> Real");
                sb.append(";\n");
            }
            sb.append(String.format(Locale.ROOT, "        part def Unit_%04d {\n", k));
            sb.append(String.format(Locale.ROOT, "            attribute own : Quantity_%04d_00;\n", k));
            if (k > 0) {
                sb.append(String.format(Locale.ROOT, "            attribute previous : Quantity_%04d_05;\n", k - 1));
            }
            if (k >= 10) {
                sb.append(String.format(Locale.ROOT, "            attribute distant : Quantity_%04d_09;\n", k - 10));
            }
            sb.append("        }\n    }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}

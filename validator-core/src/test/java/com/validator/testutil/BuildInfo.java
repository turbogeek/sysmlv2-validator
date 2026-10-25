package com.validator.testutil;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * What was measured and where, for a row of the performance history: the project version, the git commit, the JDK,
 * the operating system and how busy the machine was.
 *
 * <p>The project version comes from the system property {@value #VERSION_PROPERTY}, which the surefire configuration
 * in the root pom sets to {@code ${project.version}}; run outside Maven it is {@value #UNKNOWN}. The commit is the
 * short SHA of HEAD, with {@value #DIRTY_SUFFIX} when tracked files differ from it, so a measurement of uncommitted
 * code is not attributed to the commit. Changes under docs/performance do not count: the timing test writes the
 * history there itself, and those files are not inputs to what is measured.</p>
 *
 * @param version     the Maven project version, or {@value #UNKNOWN}
 * @param commit      the short SHA of HEAD, with {@value #DIRTY_SUFFIX} for a modified working tree, or
 *                    {@value #UNKNOWN}
 * @param jdk         the Java runtime version and vendor
 * @param os          the operating system name and version
 * @param arch        the architecture the JVM runs on (x86_64 on an Apple silicon Mac means the JVM runs under Rosetta)
 * @param cpus        the processors available to the JVM
 * @param loadAverage the system load average over the last minute, or a negative number where the OS has none
 * @param javaAgents  the jar names of the Java agents in this JVM, such as JaCoCo's coverage agent, which slows the
 *                    validator's instrumented code but not the JDK code of the reference workload
 */
public record BuildInfo(String version, String commit, String jdk, String os, String arch, int cpus,
                        double loadAverage, List<String> javaAgents) {

    public BuildInfo {
        javaAgents = List.copyOf(javaAgents);
    }

    /** System property with the Maven project version, set by surefire. */
    public static final String VERSION_PROPERTY = "validator.version";
    public static final String UNKNOWN = "unknown";
    public static final String DIRTY_SUFFIX = "-dirty";

    /** Files the timing test writes itself; changes to them do not make the working tree dirty. */
    static final String OWN_OUTPUT_PATHSPEC = ":(exclude)docs/performance";

    /** Describes this JVM and the repository's checkout at {@code repositoryRoot}. */
    public static BuildInfo capture(Path repositoryRoot) {
        String version = System.getProperty(VERSION_PROPERTY);
        if (version == null || version.isBlank() || version.startsWith("${")) {
            version = UNKNOWN;
        }
        Optional<String> sha = git(repositoryRoot, "rev-parse", "--short", "HEAD");
        Optional<String> status = git(repositoryRoot, "status", "--porcelain", "--untracked-files=no", "--", ".",
            OWN_OUTPUT_PATHSPEC);
        String commit = sha.isPresent() && status.isPresent()
            ? describeCommit(sha.get(), !status.get().isEmpty()) : UNKNOWN;
        return new BuildInfo(version, commit,
            System.getProperty("java.version") + " " + System.getProperty("java.vendor"),
            System.getProperty("os.name") + " " + System.getProperty("os.version"),
            System.getProperty("os.arch"),
            Runtime.getRuntime().availableProcessors(),
            ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(),
            javaAgents(ManagementFactory.getRuntimeMXBean().getInputArguments()));
    }

    /** The commit as recorded: the short SHA, marked when the working tree has uncommitted changes. */
    static String describeCommit(String shortSha, boolean dirty) {
        return shortSha + (dirty ? DIRTY_SUFFIX : "");
    }

    /** The jar names of the -javaagent options among JVM arguments. */
    static List<String> javaAgents(List<String> jvmArguments) {
        List<String> agents = new ArrayList<>();
        for (String argument : jvmArguments) {
            if (argument.startsWith("-javaagent:")) {
                String jar = argument.substring("-javaagent:".length()).split("=", 2)[0];
                agents.add(jar.substring(Math.max(jar.lastIndexOf('/'), jar.lastIndexOf('\\')) + 1));
            }
        }
        return agents;
    }

    /** True when the project version is known, that is, the test runs under Maven. */
    public boolean versionKnown() {
        return !UNKNOWN.equals(version);
    }

    /**
     * Runs git in the repository and returns its trimmed output, or nothing if git is missing, fails or takes more
     * than ten seconds (a locked index, say). The output goes to a file, so waiting cannot block on a full pipe.
     */
    private static Optional<String> git(Path repositoryRoot, String... args) {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Path out = null;
        try {
            out = Files.createTempFile("perf-git", ".txt");
            Process process = new ProcessBuilder(command).directory(repositoryRoot.toFile())
                .redirectOutput(out.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            return process.exitValue() == 0
                ? Optional.of(Files.readString(out, StandardCharsets.UTF_8).trim()) : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (out != null) {
                try {
                    Files.deleteIfExists(out);
                } catch (IOException ignored) {
                    // a leftover temp file is harmless
                }
            }
        }
    }
}

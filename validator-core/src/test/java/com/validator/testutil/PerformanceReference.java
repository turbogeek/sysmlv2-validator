package com.validator.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SplittableRandom;

/**
 * A known reference for timing tests. The reference workload is a fixed amount of CPU and memory work that does not
 * use the validator's code, so its time says how fast the machine is right now: battery power throttles the CPU and
 * other programs compete for it. A timing budget then scales with the reference measured in the same run instead
 * of failing because the machine, not the code, is slow.
 *
 * Because the workload does not use the validator, a slower parser cannot hide behind a slower reference: the ratio
 * of a model's build time to the reference time stays about the same when the whole machine slows down, and it
 * grows when the validator does. The allowed ratio and the observations it was derived from are recorded in
 * perf-reference.properties.
 */
public final class PerformanceReference {

    /** Test resource with the calibration: the nominal reference, the budget and the known slow models. */
    public static final String CALIBRATION_RESOURCE = "/perf-reference.properties";

    private static final int WARM_UP_RUNS = 5;
    private static final int TIMED_RUNS = 7;
    private static volatile long sink;

    private PerformanceReference() {
    }

    /** The reference times of one run: median, fastest and slowest of the timed repetitions. */
    public record Measurement(double medianMs, double minMs, double maxMs, int runs) {
        /** Slowest over median: how much the machine's speed varied while measuring; 1.0 means steady. */
        public double spread() {
            return maxMs / medianMs;
        }
    }

    /**
     * The calibration of perf-reference.properties.
     *
     * @param nominalReferenceMs the reference workload's median time on the development machine at full speed
     * @param budgetNominalMs    the build-time budget of one model at full speed
     * @param knownSlowRatioMax  per model known to exceed the budget, the largest allowed ratio of its build time to
     *                           the reference time
     */
    public record Calibration(double nominalReferenceMs, double budgetNominalMs, Map<String, Double> knownSlowRatioMax) {
        /** How much slower than at full speed the machine is now: the measured over the nominal reference, at least 1. */
        public double slowdown(Measurement reference) {
            return Math.max(1.0, reference.medianMs() / nominalReferenceMs);
        }

        /**
         * The budget of one model's build time now: the nominal budget scaled by the slowdown and by the spread of
         * the reference measurements in this run, which estimates how much the machine's speed varies right now.
         */
        public double budgetMs(Measurement reference, double spread) {
            return budgetNominalMs * slowdown(reference) * Math.max(1.0, spread);
        }

        /** True when the model is known to exceed the budget. */
        public boolean isKnownSlow(String file) {
            return knownSlowRatioMax.containsKey(file);
        }

        /** The limit for a known slow model: it may not get slower relative to the reference than recorded. */
        public double knownSlowLimitMs(String file, Measurement reference) {
            return knownSlowRatioMax.get(file) * reference.medianMs();
        }
    }

    /** Runs the reference workload after warm-up and returns the median, fastest and slowest timed run. */
    public static Measurement measure() {
        for (int i = 0; i < WARM_UP_RUNS; i++) {
            sink += workload();
        }
        double[] times = new double[TIMED_RUNS];
        for (int i = 0; i < TIMED_RUNS; i++) {
            long start = System.nanoTime();
            sink += workload();
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        Arrays.sort(times);
        return new Measurement(times[TIMED_RUNS / 2], times[0], times[TIMED_RUNS - 1], TIMED_RUNS);
    }

    /** Reads perf-reference.properties from the test classpath. */
    public static Calibration loadCalibration() {
        Properties p = new Properties();
        try (InputStream in = PerformanceReference.class.getResourceAsStream(CALIBRATION_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(CALIBRATION_RESOURCE + " is missing from the test classpath");
            }
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + CALIBRATION_RESOURCE, e);
        }
        Map<String, Double> knownSlow = new LinkedHashMap<>();
        for (String key : p.stringPropertyNames()) {
            if (key.startsWith("knownSlow.")) {
                knownSlow.put(key.substring("knownSlow.".length()), Double.parseDouble(p.getProperty(key)));
            }
        }
        return new Calibration(Double.parseDouble(p.getProperty("reference.nominalMs")),
            Double.parseDouble(p.getProperty("budget.nominalMs")), knownSlow);
    }

    /**
     * Prints three measurements of the reference workload, for recording reference.nominalMs on the development
     * machine at full speed (on AC power, with no other heavy program running):
     * java validator-core/src/test/java/com/validator/testutil/PerformanceReference.java
     */
    public static void main(String[] args) {
        for (int i = 1; i <= 3; i++) {
            Measurement m = measure();
            System.out.printf("reference run %d: median %.1f ms, min %.1f ms, max %.1f ms, spread %.2f%n",
                i, m.medianMs(), m.minMs(), m.maxMs(), m.spread());
        }
    }

    /** Writes a timing report, one JSON object, so that a run's numbers can be inspected after the fact. */
    public static void writeReport(Path file, Map<String, Object> fields) {
        StringBuilder json = new StringBuilder("{\n");
        int i = 0;
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            json.append("  \"").append(e.getKey()).append("\": ").append(toJson(e.getValue()));
            json.append(++i < fields.size() ? ",\n" : "\n");
        }
        json.append("}\n");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write " + file, e);
        }
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder s = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                s.append(i++ > 0 ? ", " : "").append(toJson(String.valueOf(e.getKey()))).append(": ").append(toJson(e.getValue()));
            }
            return s.append("}").toString();
        }
        if (value instanceof Iterable<?> list) {
            StringBuilder s = new StringBuilder("[");
            int i = 0;
            for (Object o : list) {
                s.append(i++ > 0 ? ", " : "").append(toJson(o));
            }
            return s.append("]").toString();
        }
        return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * The reference workload: sorting, string building, hashing and map updates, the kinds of work a parser and a
     * symbol table do, on fixed data. It returns a checksum so that the JIT cannot drop the work.
     */
    static long workload() {
        SplittableRandom random = new SplittableRandom(20260921L);
        double[] values = new double[200_000];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextDouble();
        }
        Arrays.sort(values);
        Map<String, Integer> counts = new HashMap<>();
        StringBuilder text = new StringBuilder();
        long checksum = Double.doubleToLongBits(values[values.length / 2]);
        for (int i = 0; i < 100_000; i++) {
            text.setLength(0);
            text.append("Package").append(i % 997).append("::element").append(i);
            String name = text.toString();
            counts.merge(name.substring(0, name.indexOf(':')), 1, Integer::sum);
            checksum = checksum * 31 + name.hashCode();
        }
        return checksum + counts.size();
    }
}

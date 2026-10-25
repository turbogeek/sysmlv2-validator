# Performance history

[`history.csv`](history.csv) records how fast the validator builds the repository's largest models, version by
version. The timing test `SemanticAnalysisIntegrationTest#testPerformance` (validator-core) writes it, and the tables
at the end of this page are generated from it.

## What is measured

Each recorded run has one row per *subject*:

| Subject | Measure | What the time is |
|---|---|---|
| each of the five largest models in `test-suite/` (by symbols) | `warm-median-of-5` | the median of five builds on the warm JVM, after the test's own first build |
| each of those five that is known to be slow (`knownSlow.*` in `perf-reference.properties`) | `first-build` | its first build in the JVM, which includes JIT compilation and filling the parser's caches |
| `suite:stockticker`, `suite:positive` | `first-builds-total` | the sum of the first builds of every model in that `test-suite/` folder |

A *build* is what the test does with a model: parse it, build the symbol table and resolve the imports. It is not
the whole CLI validation (which also loads the standard library), and the official SysML-v2-Release corpus is not
timed here: `OfficialExamplesTest` already parses it and takes about two minutes, so timing it again would double that.

### The ratio, not the milliseconds

Milliseconds depend on the machine and on what else it is doing. The figure to compare is the **ratio**: the time
divided by the time of a fixed reference workload (`com.validator.testutil.PerformanceReference`, sorting, hashing and
string building that do not use the validator) measured in the same run. When the whole machine is slower (battery
power, other builds) both times grow and the ratio stays about the same; when the validator is slower, only the
ratio grows. Lower is faster.

The ratio has two limits. It is only roughly comparable between different machines, operating systems or JVM
architectures (for example an x86_64 JDK running under Rosetta on Apple silicon against a native one), because the
validator and the reference workload need not speed up equally on different hardware: compare rows of the same `os`,
`arch` and `cpus` first. And it grows when the machine is heavily oversubscribed: the reference's short bursts still
get a processor, but a first build, which needs the JIT compiler and garbage collector threads, slows down more. On
2026-10-25 the vehicle model's ratio was 884-1076 at load averages of 13-24 on 12 CPUs and 1377 at about 35. The
`load_avg` column shows how busy the machine was.

### A JVM of its own

The timing test runs in a fresh JVM just before the other tests (the `performance-jvm` profile in
`validator-core/pom.xml`), because the tests before it in a shared JVM warm the JIT and ANTLR's static prediction
cache: in a shared JVM the vehicle model's first build took 15 s instead of 27 s, so the times depended on the order
of the test classes. With
`-Dtest=SemanticAnalysisIntegrationTest` it runs alone, which is the same; a `-Dtest` pattern that also selects other
test classes gives first-build times that do not compare with the history.

That JVM also runs without JaCoCo's coverage agent, so the timing test adds no coverage. The agent's probes slow the
validator's code but not the JDK code of the reference workload: with it, the ratios were 1.1-1.5 x those without
it. A run with any `-javaagent` (for example `-Dtest=SemanticAnalysisIntegrationTest` without
`-Djacoco.skip=true`) names the agent in its note and is treated like a run on a busy machine (below).

### Columns of history.csv

| Column | Meaning |
|---|---|
| `recorded_utc` | when the run was recorded (UTC); the rows of one run share it |
| `version` | the Maven `${project.version}` |
| `commit` | the short git SHA of HEAD; `-dirty` when tracked files had uncommitted changes (changes under `docs/performance/` do not count) |
| `jdk`, `os`, `arch`, `cpus` | the Java runtime, operating system, JVM architecture and processors |
| `load_avg` | the system load average over the last minute at the end of the run; more than `cpus` means other work competed for the processors |
| `reference_ms` | the reference workload's median time in this run |
| `slowdown` | `reference_ms` over its nominal time on the development machine at full speed (at least 1) |
| `subject`, `measure` | see the table above |
| `files`, `symbols` | how many model files were timed and the symbols built from them |
| `timed_ms` | the time |
| `ratio` | `timed_ms / reference_ms`: the figure to compare |
| `note` | free text from `-Dperf.note=...`, and the Java agent if the JVM had one |

## When a run is recorded

Every run of the timing test writes the rows it would record to `validator-core/target/perf/history-entry.csv` and
prints them against the history. It appends them to `history.csv` (and regenerates this page) only:

- **automatically** for the first run of a project version that has no rows yet, passed its checks and has ratios
  that can be trusted (no busy machine, no Java agent; see below), so every version is recorded once without
  ordinary builds modifying the file;
- **always** with `-Dperf.record=true`;
- **never** with `-Dperf.record=false` (for CI or measurements that should leave the tree alone).

A run outside Maven (from an IDE without the surefire configuration) has no project version and is never recorded.

### How to record a run

Record on a quiet machine on AC power, and describe the conditions in the note:

```sh
mvn -B clean verify -Djacoco.haltOnFailure=false -Dperf.record=true -Dperf.note="quiet, AC power"
```

or, to run only the timing test (about a minute):

```sh
mvn -B -pl validator-core test -Djacoco.skip=true -Dtest=SemanticAnalysisIntegrationTest -Dperf.record=true -Dperf.note="quiet, AC power"
```

Then commit `history.csv` and `README.md` together. To remove a bad run, delete its rows from `history.csv` and
regenerate the tables from the repository root:

```sh
java validator-core/src/test/java/com/validator/testutil/PerformanceReadme.java
```

`PerformanceReadmeTest` fails when the committed tables and `history.csv` disagree.

## The regression check

Each run prints its ratios next to the latest run of the **previous version** (the version recorded before this one)
and the **last recorded run**; for the first run of a new version the two are the same run. The test fails when a
subject's ratio is clearly worse than in the last recorded run: more than **2.0 x** that ratio **and** more than
**1.0** above it. The factor covers the run-to-run noise measured on the development machine; the minimum
increase keeps the models that take a few milliseconds warm (ratios well below 1) from failing on a few milliseconds
of scheduling or garbage collection. Both, and the noise they come from, are in
`validator-core/src/test/resources/perf-reference.properties`. A subject timed another way than in the baseline
(first build against warm median) or a suite with a different number of files is shown but not compared.

A run on a **busy** machine, with a load average above **2.0 per CPU** at its end, or with a **Java agent**, is
compared and printed but does not fail, and is not recorded automatically: its ratios are inflated (see above), so
it could neither show a regression reliably nor serve as a baseline. Run the test again on a quieter machine or
without the agent.

When a slowdown is intended, record the run with `-Dperf.record=true`: it becomes the baseline the next runs compare
with.

The per-model budgets in `perf-reference.properties` (`budget.nominalMs`, `knownSlow.*`) are a separate, absolute
check made by the same test.

## Per version

<!-- BEGIN GENERATED from history.csv by com.validator.testutil.PerformanceReadme; do not edit by hand -->
No runs recorded yet.
<!-- END GENERATED -->

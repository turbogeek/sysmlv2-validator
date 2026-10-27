# Pilot runner

`PilotRunner.java` runs the models of a conformance manifest through the OMG reference implementation, the
[SysML v2 Pilot Implementation](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation), the way its
Jupyter kernel does (`org.omg.sysml.interactive.SysMLInteractive`). It writes the shared result format, so its
accept/reject verdicts and error lines can be compared with this validator's and Cameo's.

## Get the pilot

Download the Jupyter kernel zip of a pilot release from
<https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/releases>. Release tag `2026-09` is version
0.63.0:

```bash
mkdir -p ~/pilot && cd ~/pilot
curl -LO https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/releases/download/2026-09/jupyter-sysml-kernel-0.63.0.zip
unzip jupyter-sysml-kernel-0.63.0.zip -d kernel-0.63.0
```

The runner needs only two things from it: `sysml/jupyter-sysml-kernel-0.63.0-all.jar` and the standard library
`sysml/sysml.library`. Jupyter itself is not needed.

## Run

Java 21 or newer, from the repository root. For the conformance test, once per model:

```bash
PILOT=~/pilot/kernel-0.63.0/sysml
java -Xmx4g -cp $PILOT/jupyter-sysml-kernel-0.63.0-all.jar conformance/pilot/PilotRunner.java \
    --library $PILOT/sysml.library --extra-library "CATIA_SysML Libraries/26xR2" \
    --manifest conformance/manifest.csv --repo . \
    --out conformance/results/pilot_$(hostname)_$(date +%Y%m%d-%H%M%S)
```

For the performance comparison ([perf/README.md](../../perf/README.md)), timed three times per model:

```bash
PILOT=~/pilot/kernel-0.63.0/sysml
java -Xmx4g -cp $PILOT/jupyter-sysml-kernel-0.63.0-all.jar conformance/pilot/PilotRunner.java \
    --library $PILOT/sysml.library --manifest perf/corpus/manifest.csv --repo . --release <SysML-v2-Release> \
    --out perf/results/pilot_$(hostname)_$(date +%Y%m%d-%H%M%S) --reps 3
```

`CompareMain conformance compare` and `CompareMain perf compare` find the newest pilot run in those folders.

| Option | |
|---|---|
| `--library` | the pilot's `sysml.library` (required) |
| `--manifest` | the manifest CSV (required) |
| `--repo` | the repository root that manifest paths are relative to (required) |
| `--out` | the result directory, created (required) |
| `--extra-library DIR` | more library files the models use, such as Dassault's (`CATIA_SysML Libraries/26xR2`); may be repeated |
| `--reps N` | repetitions per model (default 1) |
| `--large-lines N` | models with more lines run once (default 10000) |
| `--release DIR` | a SysML-v2-Release checkout for `release:` paths (default `$SYSML_RELEASE_PATH`; else they are skipped) |
| `--only id,id` | run only these manifest ids |

It writes `results.csv` (a `startup` row for loading the library, a `process` row per model and repetition, and
one row with phase `all` and status `skipped` for a model it did not run), `diagnostics.csv` (first repetition),
`environment.properties`, `report.md` and `run-log.txt`, also when a model or the run fails or the run is stopped
(Ctrl-C). The exit status is 0 when the run completed (models may have failed), 1 when the pilot could not be
loaded, the manifest not read or the results not written, 2 for a wrong command line.

On Java 24 and newer the pilot's Guice prints `sun.misc.Unsafe` warnings; they are harmless
(`--sun-misc-unsafe-memory-access=allow` silences them). A model nested deeper than the parser's stack allows is a
`failed` row with a `StackOverflowError`; `-Xss` raises the limit.

## How models are processed

- The `--extra-library` folders are read after the standard library, as library resources. The pilot does not
  resolve the short name of a top-level package declared in another file (`DS_Views::…` for
  `DassaultSystemesViews`; in a KPAR, a member's package by its short name), so models that use Dassault's
  libraries through those names still have errors in the pilot. In the current corpus every Dassault reference
  uses a short name, so `--extra-library` changes no verdict there; it does for models that use the full names.
- A `.sysml` or `.kerml` file is parsed into a new resource of its language and validated with all checks, the steps
  of `SysMLInteractive.process` called one by one.
- The `.sysml` and `.kerml` members of a `.kpar` are parsed into one resource each, added to the pilot's index together
  and then validated, so they resolve each other as the files of one project do. Each diagnostic names its member and
  the line within it. A KPAR that is not a zip, has a corrupted entry, has no `.project.json` holding a JSON
  object, has a member that is not UTF-8 text or has no model file is a `skipped` row with detail `invalid KPAR …` (the faults this
  validator and the Cameo macro reject too): the pilot reads no KPARs, so that is this runner's check, not the
  pilot's verdict. A model the runner could not run (the pilot could not be loaded again, or its index cannot be
  restored for a KPAR) is `skipped` too.
- Isolation: `process` itself indexes every error-free model, so later models resolve names against it. The runner
  never indexes a plain file; after every model it unloads and removes all resources but the library's and puts the
  index back as it was after the library loaded (the pilot's index does not restore a library package that a KPAR
  shadowed). A KPAR that redeclares a library package (a library KPAR, say) changes what the pilot works out lazily
  for the library, so it runs in a newly loaded pilot, and the pilot is loaded again before the next model.
- Source of a diagnostic: `syntax` and `linking` for the Xtext syntax and linking diagnostics, `validation` for the
  rest; `code` is the pilot's issue code. Errors the pilot only logs (an exception inside a check) are counted in the
  row's detail and written to `run-log.txt`.

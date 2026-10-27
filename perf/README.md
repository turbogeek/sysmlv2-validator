# Performance: this validator and the pilot implementation

This folder times this validator and the OMG SysML v2 Pilot Implementation (the reference implementation) on the
same SysML v2 models, from 105 to 65,053 lines. Each tool writes a result directory in the same format, and a
report compares the two.

```
perf/
  corpus/     the models, and manifest.csv (size and SHA-256 of each)
  results/    one directory per run: validator_<host>_<time>/ or pilot_<host>_<time>/, and comparison.md
```

Whether the tools *agree* on the models, and whether Cameo does, is the conformance test: see
[conformance/README.md](../conformance/README.md). The Cameo macro there records Cameo's import times too, for
information only.

## The models

| Id | Lines | What it exercises |
|---|---:|---|
| `system-xs`, `-s`, `-m`, `-l`, `-xl` | 105 to 65,053 | 2 to 2,500 subsystems (parts, ports, interfaces, actions, states, requirements, quantities with units), wired into one system |
| `inheritance-200` | 809 | a chain of 200 part definitions, each redefining an inherited attribute: name resolution through redefinitions |
| `imports-100` | 1,892 | 100 packages, each publicly importing the previous one: name resolution through imports |
| `errors-m` | 5,253 | `system-m` with an unresolved type, a re-declared inherited attribute and a misspelled redefinition in every tenth subsystem |
| `real-cameo-constructs` | 89 | constructs of the CATIA Magic 26xR2 libraries and Cameo-exported models |
| `real-name-collisions` | 323 | the name-collision examples (warnings only) |
| `real-mass-typed-by-mass` | 47 | three typing errors |
| `real-vehicle-legacy` | 1,493 | an older copy of the specification's vehicle model, with errors |
| `release-annex-a-vehicle` | 1,581 | the specification's Annex A vehicle model, taken from a SysML-v2-Release checkout (skipped without one) |

The generated models (`system-*`, `inheritance-200`, `imports-100`) are clean in the pilot implementation 0.63.0,
so that a slow or failing run measures the tool, not the model. To regenerate them, from the checkout root:

```bash
java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain perf corpus --release <SysML-v2-Release>
```

A test fails if `manifest.csv` no longer matches the generator or the committed files. `.gitattributes` keeps the
models' LF line endings on every checkout, so that both tools time the same bytes.

## 1. Run this validator

This needs a JDK 25 and a build of this repository (`mvn clean package`). From the checkout root:

```bash
java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain perf run --release <SysML-v2-Release>
```

It uses `<SysML-v2-Release>/sysml.library` as the standard library (give a different one with `--library`),
together with any in `$SYSML_LIBRARY_PATH`; it warns if that makes more than one. It stops if no standard
library is found, because without one only the parse would be timed.

It writes `perf/results/validator_<host>_<time>/`. For every model and repetition, after a garbage collection, it
times:

- `startup`: loading the standard library, once;
- `validate`: the whole validation of the file, parse included, as the CLI does it;
- `parse`: the parse alone;
- `index`: the name-resolution index.

Options: `--reps N` (3); `--large-lines N`, models with more lines run once (10000); `--only id,id`; `--out DIR`.

## 2. Run the pilot implementation

See [conformance/pilot/README.md](../conformance/pilot/README.md). The same runner times the pilot; give it this
folder's manifest:

```bash
PILOT=~/pilot/kernel-0.63.0/sysml
java -Xmx4g -cp $PILOT/jupyter-sysml-kernel-0.63.0-all.jar conformance/pilot/PilotRunner.java --library $PILOT/sysml.library --manifest perf/corpus/manifest.csv --repo . --release <SysML-v2-Release> --reps 3 --out perf/results/pilot_$(hostname)_$(date +%Y%m%d-%H%M%S)
```

It times `startup` (loading the library) and `process` for every model: parse, name resolution and validation of
one model by `SysMLInteractive`, as the pilot's Jupyter kernel does.

## 3. Compare

```bash
java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain perf compare
```

This compares the newest pilot run with the newest validator run, preferring a validator run from the same
machine, and writes `perf/results/comparison.md`. Use `--validator DIR` and `--pilot DIR` to choose others, and
`--out FILE` to write elsewhere. The report has:

- the two environments, including the machines' load during the runs;
- startup times;
- per model: this validator's `validate` next to the pilot's `process`, each as the first repetition, then the
  median of the others, and the ratio (the pilot's time over this validator's). For a model either side ran only
  once (above 10,000 lines), the ratio compares the first runs and is marked "(1 run)";
- per model: the errors and warnings of each tool.

If the two runs come from different machines, the report says so, and the ratios then compare machines as well
as tools. The result files are described in [conformance/README.md](../conformance/README.md#result-files).

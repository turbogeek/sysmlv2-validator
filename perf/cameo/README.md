# Performance comparison with Cameo (CATIA Magic SysML v2)

This folder times this validator and Cameo on the same SysML v2 models. Each tool writes a result directory in
the same format, and a comparison report is generated from the two.

```
perf/cameo/
  corpus/                   the models, and manifest.csv (size and SHA-256 of each)
  CameoPerfRunner.groovy    the Cameo macro: imports, validates, exports each model and times it
  results/                  one directory per run: cameo_<host>_<time>/ or validator_<host>_<time>/
```

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

The generated models (`system-*`, `inheritance-200`, `imports-100`) are clean in the SysML v2 Pilot
Implementation 0.63.0, so that a slow or failing import measures the tool, not the model. To regenerate them,
from the checkout root:

```bash
java -cp validator-cli/target/sysml-validator.jar com.validator.cli.perf.PerfMain generate --release <SysML-v2-Release>
```

A test fails if `manifest.csv` no longer matches the generator or the committed files. `.gitattributes` keeps
the models' LF line endings on every checkout, so that both machines time the same bytes.

## 1. Run Cameo (on the Cameo machine)

Requirements: Cameo / CATIA Magic 2026x with the SysML v2 plugin. The macro was written against
2026x Refresh1, build 2026.1.0-597.

1. Get `main` of `https://github.com/turbogeek/sysmlv2-validator.git`:

   ```bash
   git clone https://github.com/turbogeek/sysmlv2-validator.git
   cd sysmlv2-validator
   ```

   In an existing clone, run `git switch main`, then `git pull`.
2. In Cameo, choose **Tools > Macros > Organize Macros** and click **Add**. In the macro information:
   - set **Name** (for example `CameoPerfRunner`) and **Macro Language** to **Groovy**;
   - set **File** to `perf/cameo/CameoPerfRunner.groovy` in the checkout;
   - leave **Add macro to model** unchecked, so that the file is read again on every run;
   - click **OK**.

   Select the macro, click **Run**, and answer **Yes** to the security warning.
3. When asked, select the checkout folder: the one that holds `perf/cameo`.
4. A progress dialog shows the model being timed. **Cancel** stops after the current repetition of the current
   model; the large models can take minutes. The results so far are still written.
5. When it finishes, the macro reports the result directory, `perf/cameo/results/cameo_<host>_<time>/`. Commit
   and push it:

   ```bash
   git add perf/cameo/results
   git commit -m "Cameo performance run"
   git push
   ```

The macro creates a scratch SysML v2 project in the local repository, named `SysMLv2Perf_<time>`. Each model is
imported into it, measured and deleted again, and the undo history is cleared after each model, so that deleted
models do not stay in memory. The project is closed without saving. Delete it from the local projects if you do
not want to keep it.

**Settings.** There are three ways to change one:

1. **Macro arguments** (easiest). In Organize Macros, select the macro, click **Edit**, then **Arguments** >
   **Add**. Give the setting's name as **Name**, for example `PERF_ONLY`, with **Type** String and a **Default
   Value** such as `system-xs,system-s`. Leave **Null** unticked. Unless **Automatically run with default
   values** is ticked, Cameo asks for the values on each run.
2. Edit the default in the settings block at the top of the script, for example `setting('PERF_ONLY', 'system-xs')`.
3. Add `-DPERF_ONLY=system-xs` to the `JAVA_ARGS=` line of `<Cameo install>/bin/<product>.properties` and restart
   Cameo. Environment variables reach Cameo only if it is started from that environment.

| Setting | Default | |
|---|---|---|
| `PERF_REPO` | ask | the checkout |
| `PERF_REPETITIONS` | 3 | repetitions per model |
| `PERF_LARGE_LINES` | 10000 | models with more lines run once (`system-l`, `system-xl`) |
| `PERF_ONLY`, `PERF_SKIP` | | model ids, comma-separated |
| `PERF_RELEASE` | `$SYSML_RELEASE_PATH` | a SysML-v2-Release checkout, for `release-annex-a-vehicle` |
| `PERF_UNDOCUMENTED` | true | also run the steps that use undocumented API (`validate`, `build`) |

**A first, short run.** The large models take a while. Try the macro first with the argument `PERF_ONLY` set to
`system-xs,system-s`, then remove it for the full run.

**The Annex A model.** To include `release-annex-a-vehicle`, clone
`https://github.com/Systems-Modeling/SysML-v2-Release` on the Cameo machine and set `PERF_RELEASE` to it.
Otherwise that model is skipped.

**If something goes wrong.** A row with status `unavailable` or `failed` is a result too. Commit and push the
directory anyway: the reason is in the `detail` column of `results.csv` and in `run-log.txt`.

If the macro does not start at all (no progress dialog and no result directory), copy the message from the
Notification window and the end of Cameo's log file into an issue. The script will then need adjusting for that
Cameo build.

### What the macro times

For every model and repetition, after a garbage collection, in this order:

| Phase | What it does | API |
|---|---|---|
| `import` | the textual import into the project, in a session | `SysMLTextualNotationService.importTextual` (documented) |
| `count` | count the imported elements | `RootNamespaces`, `Element.getOwnedElement` (documented) |
| `validate` | Cameo's SysML v2 validation of the imported model, active and passive suites | `ValidationService`, `ValidationUtils.validateRecursively`, as the Validate action (undocumented) |
| `export` | the textual export of the imported model | `SysMLTextualNotationService.exportTextual` (documented) |
| `dispose` | delete the imported model, in a session | `Elements.dispose` (documented) |
| `build` | parse and resolve names into a transient model; text errors and warnings | `SysMLTransientModelBuilder.build`, the first half of the import (undocumented) |

The import comes first, so its first repetition is the model's first run in Cameo. Once per run,
`create_project` times the creation of the scratch project (`SysMLProjectHelper.createUPSProject`, documented).

Undocumented API can change between Cameo builds. If a step fails, the run records it as `unavailable`, with
the reason, and goes on. All Cameo classes are looked up by name, so a missing class cannot stop the macro from
starting.

## 2. Run this validator

Run it on the Cameo machine as well if you can, so that both tools are timed on the same hardware. That needs a
JDK 25 and a build of this repository (`mvn clean package`). On one line:

```bash
java -cp validator-cli/target/sysml-validator.jar com.validator.cli.perf.PerfMain run --release <SysML-v2-Release>
```

It uses `<SysML-v2-Release>/sysml.library` as the standard library. Give a different one with `--library`. It
stops if no standard library is found, because without one only the parse would be timed.

It writes `perf/cameo/results/validator_<host>_<time>/`. For every model and repetition, after a garbage
collection, it times:

- `startup`: loading the standard library, once;
- `validate`: the whole validation of the file, parse included, as the CLI does it (first, as Cameo's `import`);
- `parse`: the parse alone;
- `index`: the name-resolution index.

Options:

- `--reps N` (3);
- `--large-lines N`: models with more lines run once, as in the macro (10000);
- `--only id,id`;
- `--out DIR`.

## 3. Compare

```bash
java -cp validator-cli/target/sysml-validator.jar com.validator.cli.perf.PerfMain compare
```

This compares the newest Cameo run with the newest validator run, preferring a validator run from the same
machine. It writes `comparison.md` into the Cameo directory. Use `--validator DIR` and `--cameo DIR` to choose
others. The report has:

- the two environments, including the machines' load during the runs;
- startup times;
- per model: this validator's `validate` next to Cameo's `build`, `import`, `validate` and `import` + `validate`
  (text in, model checked). Each is given as the first repetition, then the median of the others, with a ratio.
  For a model either side ran only once (above 10,000 lines), the ratio compares the first runs and is marked
  "(1 run)";
- per model: the errors and warnings of each tool, and the lines both flag with an error.

If the two runs come from different machines, the report says so, and the ratios then compare machines as well
as tools.

## Result files

Both tools write the same files.

- `results.csv`: one row per model, repetition and phase:
  `tool,tool_version,host,run_id,file_id,rep,phase,ms,elements,errors,warnings,status,detail`.
  The status is `ok`, `failed`, `unavailable` or `skipped`.
- `diagnostics.csv`: what the tool reported for each model, from the first repetition:
  `tool,file_id,source,severity,line,column,code,message`. Cameo's validation failures are on elements, not
  lines. Their element is in the message.
- `environment.properties`: the tool and plugin versions, the machine and its load, the JVM, the repetitions,
  and (Cameo) the `PERF_*` settings.
- `report.md`: a summary of the run.
- `run-log.txt` (Cameo only): what the macro did, with the reason for every step that failed.

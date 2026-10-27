# Conformance: this validator, the pilot implementation and Cameo

This validator is meant to tell whether a SysML v2 file can be used in Cameo / CATIA Magic. This folder checks
that it does: the test files of `test-suite` and `examples` (277 models, six of them KPARs) are loaded by three
tools, and what they report is compared:

- this validator;
- the OMG SysML v2 Pilot Implementation, the reference implementation;
- Cameo / CATIA Magic with the SysML v2 plugin.

For each file, the tools should give the same verdict: a file this validator accepts should import into Cameo
without errors, and a file with errors should be rejected by all three. The one expected difference is the checks
this validator adds to the language rules (lint, name collisions, diamond inheritance). Their warnings are counted
apart, because the other tools do not have them.

```
conformance/
  manifest.csv                        the models: id, path, expected outcome, lines, bytes, SHA-256
  kpar/                               KPARs (KerML project archives) built from test files
  cameo/CameoConformanceRunner.groovy the Cameo macro
  pilot/PilotRunner.java              runs the pilot implementation
  results/                            one directory per run (validator_*, pilot_*, cameo_*),
                                      comparison.md and verdicts.csv
```

Performance is compared separately, between this validator and the pilot: see [perf/README.md](../perf/README.md).

## The models

| Family | From | Files | Expected |
|---|---|---:|---|
| `positive` | `test-suite/positive` | 53 | clean |
| `negative` | `test-suite/negative` | 57 | errors |
| `lint` | `test-suite/lint` | 7 | – |
| `cheatsheet` | `test-suite/CheatSheet` | 110 | – |
| `dsl` | `test-suite/DSLinSysML` | 1 | – |
| `dassault` | `test-suite/DassaultLamguageExample` | 13 | – |
| `pilot-repro` | `examples/name-collision/pilot-repro/models`: name-collision cases checked in the pilot | 23 | – |
| `misc`, `examples` | `test-suite/test_extend.sysml`, `examples/*.sysml`, `examples/name-collision` | 7 | as marked |
| `kpar` | `conformance/kpar` | 6 | as below |

"–" means the expected outcome is not fixed: the tools' verdicts are compared with each other only. The
expected outcome is the test suite's intent, except for `positive/12_simple_vehicle_model`, an older copy of the
specification's vehicle model, which is expected to have errors. The lint files are written to trigger this
validator's lint rules, and five of them use `Real` or `Integer` without importing `ScalarValues`, so they are
not clean.

The KPARs:

| Id | Members | Expected |
|---|---|---|
| `kpar/stockticker` | the 12 files of `test-suite/stockticker`, which refer to each other | – |
| `kpar/name-collision-examples` | `examples/name-collision/NameCollisionExamples.sysml` | warnings |
| `kpar/two-members` | `Vehicle.sysml` defines `Vehicle`; `Car.sysml` types a part by it | clean |
| `kpar/two-members-error` | as `two-members`, with `Vehicle` misspelled in `Car.sysml` line 3 | errors |
| `kpar/invalid-no-project` | a zip with a model file but no `.project.json` | invalid |
| `kpar/invalid-not-a-zip` | a text file named `.kpar` | invalid |

Each KPAR has a `.project.json` and a `.meta.json` (an index of its top-level packages and the SHA-256 of each
member). They are written uncompressed with fixed times, so that the same members give the same bytes on any
machine.

To regenerate the manifest and the KPARs after adding or changing test files, from the checkout root:

```bash
java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain conformance corpus
```

A test fails if `manifest.csv` or a KPAR no longer matches the generator. `.gitattributes` keeps the models' LF
line endings on every checkout, so that their checksums hold on Windows too.

## 1. Run this validator

This needs a JDK 25 and a build of this repository (`mvn clean package`). From the checkout root:

```bash
java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain conformance run
```

It validates every model once, as the CLI does, and writes `conformance/results/validator_<host>_<time>/`. The
members of a KPAR are validated together, so that names resolve across them, and each diagnostic names its
member; an invalid KPAR gives one `IO_INVALID_KPAR` error. Dassault's libraries (`CATIA_SysML Libraries/26xR2`)
are loaded as well, as Cameo has them.

The standard library comes from `--library <sysml.library>` (or `<release>/sysml.library` of `--release` or
`$SYSML_RELEASE_PATH`), together with any in `$SYSML_LIBRARY_PATH` and `validator.properties`; if none of these is
set, a few usual places are searched (`~/SysML-v2-Release/sysml.library`, ...). It stops if it finds no library
(`--allow-no-library` runs without one), and warns if it uses more than one. To compare like with like, give it
the library the pilot uses (`--library $PILOT/sysml.library` below) and leave `$SYSML_LIBRARY_PATH` unset. The
libraries used are recorded as `library` in `environment.properties`. Other options: `--only id,id` (the ids are the first column of `manifest.csv`; unknown
ones are reported) and `--out DIR`.

## 2. Run the pilot implementation

See [pilot/README.md](pilot/README.md) for getting the pilot. With its Jupyter kernel unzipped in
`~/pilot/kernel-0.63.0`, from the checkout root:

```bash
PILOT=~/pilot/kernel-0.63.0/sysml
java -Xmx4g -cp $PILOT/jupyter-sysml-kernel-0.63.0-all.jar conformance/pilot/PilotRunner.java --library $PILOT/sysml.library --extra-library "CATIA_SysML Libraries/26xR2" --manifest conformance/manifest.csv --repo . --out conformance/results/pilot_$(hostname)_$(date +%Y%m%d-%H%M%S)
```

It needs Java 21 or later (the JDK 25 of this repository works). `--extra-library` gives the pilot Dassault's
libraries, as Cameo and this validator have them. The pilot does not resolve the short name of a top-level package
declared in another file, though: `DassaultSystemesViews::SymbolicViews::gv` resolves, `DS_Views::SymbolicViews::gv`
does not. So models that use Dassault's libraries through those names (as the corpus's models made in Cameo do)
are rejected by the pilot for that reason, and the comparison marks them. The pilot has no KPAR reader: the
runner reads a KPAR's members and indexes them together, and an invalid KPAR is *skipped* by the pilot, not
rejected.

## 3. Run Cameo

Requirements: Cameo / CATIA Magic 2026x with the SysML v2 plugin. The macro was written against 2026x Refresh1,
build 2026.1.0-597.

1. Get `main` of `https://github.com/turbogeek/sysmlv2-validator.git`:

   ```bash
   git clone https://github.com/turbogeek/sysmlv2-validator.git
   cd sysmlv2-validator
   ```

   In an existing clone, run `git switch main`, then `git pull`.
2. In Cameo, choose **Tools > Macros > Organize Macros** and click **Add**. In the macro information:
   - set **Name** (for example `CameoConformanceRunner`) and **Macro Language** to **Groovy**;
   - set **File** to `conformance/cameo/CameoConformanceRunner.groovy` in the checkout;
   - leave **Add macro to model** unchecked, so that the file is read again on every run;
   - click **OK**.

   Select the macro, click **Run**, and answer **Yes** to the security warning.
3. When asked, select the checkout folder: the one that holds `conformance`.
4. A progress dialog shows the model being loaded. **Cancel** stops after the current model; the results so far
   are still written.
5. When it finishes, the macro reports the result directory, `conformance/results/cameo_<host>_<time>/`. Commit
   and push it:

   ```bash
   git add conformance/results
   git commit -m "Cameo conformance run"
   git push
   ```

The macro creates a scratch SysML v2 project in the local repository, named `SysMLv2Conformance_<time>`. Each
model is imported into it, checked and deleted again, and the undo history is cleared after each model, so that
one model cannot affect the next. The project is closed without saving. Delete it from the local projects if you
do not want to keep it.

**Settings.** There are three ways to change one:

1. **Macro arguments** (easiest). In Organize Macros, select the macro, click **Edit**, then **Arguments** >
   **Add**. Give the setting's name as **Name**, for example `CONF_ONLY`, with **Type** String and a **Default
   Value** such as `positive/01_simple_part_def,kpar/two-members`. Leave **Null** unticked. Unless
   **Automatically run with default values** is ticked, Cameo asks for the values on each run.
2. Edit the default in the settings block at the top of the script, for example `setting('CONF_ONLY', 'kpar/two-members')`.
3. Add `-DCONF_ONLY=kpar/two-members` to the `JAVA_ARGS=` line of `<Cameo install>/bin/<product>.properties` and
   restart Cameo. Environment variables reach Cameo only if it is started from that environment.

| Setting | Default | |
|---|---|---|
| `CONF_REPO` | ask | the checkout |
| `CONF_ONLY`, `CONF_SKIP` | | model ids, comma-separated |
| `CONF_UNDOCUMENTED` | true | also run the steps that use undocumented API (`validate`, `build`). Without the build step the text of a model is not checked, so the comparison gives Cameo no verdict (*unchecked*) |

**If something goes wrong.** A row with status `unavailable` or `failed` is a result too. Commit and push the
directory anyway: the reason is in the `detail` column of `results.csv` and in `run-log.txt`.

If the macro fails before it can load any model, its result directory has a `results.csv` whose only row says
why (`*`, `start`, `failed`), and the comparison reports that the Cameo run failed to start. If it does not start
at all (no progress dialog and no result directory), copy the message from the Notification window and the end
of Cameo's log file into an issue. The log file is in the product's folder in your home folder, for example
`~/.magic.systems.of.systems.architect/<version>/msosa.log` for Magic Systems of Systems Architect. The script will
then need adjusting for that Cameo build.

### What the macro does

For every model, in this order:

| Phase | What it does | API |
|---|---|---|
| `import` | the textual import into the project, in a session | `SysMLTextualNotationService.importTextual` (documented) |
| `count` | count the imported elements; an import that adds nothing fails here | `RootNamespaces`, `Element.getOwnedElement` (documented) |
| `validate` | Cameo's SysML v2 validation of the imported model, active and passive suites | `ValidationService`, `ValidationUtils.validateRecursively`, as the Validate action (undocumented) |
| `dispose` | delete the imported model, in a session | `Elements.dispose` (documented) |
| `build` | parse and resolve names into a transient model: the text errors and warnings, with their lines | `SysMLTransientModelBuilder.build`, the first half of the import (undocumented) |

Cameo's verdict on a model comes from `import`, `count`, `build` and `validate`. The import does not report
errors in the text: `build` gives the text errors with their lines, whether or not the import itself failed, and
without it Cameo has no verdict on the model (*unchecked*). `validate` reports on elements, not lines.

After each model, the macro checks that the scratch project is as it was created. If a model could not be
deleted, a `cleanup` row says so and the next models run in a new scratch project.

Undocumented API can change between Cameo builds. If a step fails, the run records it as `unavailable`, with the
reason, and goes on. All Cameo classes are looked up by name, so a missing class cannot stop the macro from
starting. The times of the phases are recorded for information only.

### KPARs in Cameo

Cameo 2026x Refresh2 loads KPARs, but its API for that is not released yet. Until it is, the macro imports the
`.sysml` members of a KPAR the way Cameo's own Import of several files does: they are written to a temporary
folder, built together (`TextualModelHelper.buildFromFiles`) and copied into the project
(`TextualModelHelper.importTransientModelsIntoProject`), and each diagnostic names its member. Both calls are
undocumented; if a Cameo build lacks them, the members are joined in archive order and imported as one text, and
each diagnostic line is mapped back to its member. `environment.properties` records which way was used (`kpar`),
and the import row of each KPAR says so too.

Cameo's textual import reads `.sysml` files only, so `.kerml` members are left out (the import row says how
many). A KPAR the macro cannot read is *skipped*, since that is the macro's check, not Cameo's verdict: as in this
validator, one that is not a zip archive, has a corrupted entry, has no `.project.json` file holding a JSON
object, has a member that is not UTF-8 text, or has no model file; and one with no `.sysml` member.

When the KPAR API is available, give the macro's `CameoApi` class a method `importKpar(project, Path kpar,
String label)` that imports the archive. The macro then uses it for the import of every KPAR, invalid ones
included (Cameo then decides about them), and records "Cameo KPAR import" in `environment.properties`. The other
phases stay as they are.

## 4. Compare

Get the Cameo run first (`git pull`), then:

```bash
java -cp validator-cli/target/sysml-validator.jar com.validator.cli.compare.CompareMain conformance compare
```

This takes the newest run of each tool in `conformance/results` (at least two of the three), by the run id in its
`results.csv`, and writes `conformance/results/comparison.md` and `verdicts.csv`. Use `--validator DIR`,
`--pilot DIR` and `--cameo DIR` to choose runs, and `--out FILE` to write elsewhere.

Each tool's **verdict** on a model is *accepted* (no errors), *rejected* (errors) or *failed* (it could not load
the model, which for a user is a rejection too). A model also can have no verdict: *unchecked* (Cameo imported it
but did not check its text), *skipped* (the tool did not run it: a file not found, a KPAR the pilot runner or the
macro cannot read, the run failed to start) or *missing* (the run has no row for it, for example a run with
`--only`). Those are left out of the
agreements and disagreements and counted in the summary. The report has:

- the runs, with their settings, and a warning if a run used another version of the manifest than the one
  compared, or ran files that differ from it (run them on the same commit);
- a summary: on how many models all tools agree, and each pair's agreements and disagreements;
- **Validator accepts, Cameo rejects**: the most important list. These files pass this validator but are not
  usable in Cameo as they are, with what Cameo reported;
- **Validator rejects, Cameo accepts**: errors this validator reports that Cameo does not;
- the disagreements of the pilot and Cameo, and of this validator and the pilot;
- **Rejected by all, with errors in different places**: the error lines (`member:line` in a KPAR) each tool
  reports that the others do not, first the models whose tools have no error line in common. A tool with no
  lines for a model (Cameo's validation reports elements) is left out. Columns are not compared: the tools count
  them differently;
- every model with each tool's verdict, errors and warnings; this validator's warnings of added checks are in
  parentheses.

`verdicts.csv` has one line per model with each tool's verdict and counts, for a spreadsheet.

## Result files

All three tools write the same files, which the comparisons read.

- `results.csv`: one row per model and phase:
  `tool,tool_version,host,run_id,file_id,rep,phase,ms,elements,errors,warnings,status,detail`.
  The status is `ok`, `failed`, `unavailable` or `skipped`. A row with `file_id` `*` is about the whole run
  (loading the library, creating Cameo's project). A model that was not run has one row with phase `all` and
  status `skipped`.
- `diagnostics.csv`: what the tool reported for each model:
  `tool,file_id,member,source,severity,line,column,code,message`. `member` is the file inside a KPAR, empty for a
  plain file. The severity is `error`, `warning` or `info`. Cameo's validation failures are on elements, not
  lines (line 0); their element is in the message.
- `environment.properties`: the tool and library versions, the machine and its load, the JVM, and the run's
  settings.
- `report.md`: a summary of the run.
- `run-log.txt` (Cameo and pilot): what the run did, with the reason for every step that failed.

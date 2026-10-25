# Name-collision examples

Examples for the name-collision pitfalls analyzed in [docs/NAME_COLLISION_ANALYSIS.md](../../docs/NAME_COLLISION_ANALYSIS.md):
declaring a feature with an inherited feature's name instead of redefining it (`:>>`), and the related cases where a
name silently resolves to a different element than intended.

## For Cameo / CATIA Magic

- **`NameCollisionExamples.sysml`** is one package, `NameCollisionExamples`, with a sub-package per pitfall. In each
  sub-package, elements named `…Pitfall` show the mistake and elements named `…Fixed` show the correct form. Each
  pitfall's `doc` comment says what is wrong and what the reference implementation reports for it. `doc` comments are
  used instead of `//` comments because only `doc` comments are kept in the model.
  The file has no errors, so it imports as a whole.
- **`NameCollisionErrorForm.sysml`** shows that the correct form can be the one that is rejected. When the general
  type binds a value with `=`, redefining it is an error, while re-declaring it is only a warning. It is a separate
  file because it intentionally contains an error.

Import the files into a SysML v2 project as SysML v2 textual notation, or paste the packages into the textual-notation
editor. Then run the model validation and compare its results with the table below. These files were checked with the
SysML v2 Pilot Implementation and with this validator, not in Cameo itself.

| Sub-package | Pitfall | Reported by the pilot (0.62.0 and 0.63.0) |
|---|---|---|
| `V1_RedeclareInDefinition` | `CarPitfall::mass` | warning: Duplicate of inherited member name 'mass' from Vehicle |
| `V2_ValueOnUsage` | `myCarPitfall::mass`, `yourCarPitfall::mass` | warning (same) on each |
| `V3_DuplicateInOneScope` | `VehiclePitfall` (two `mass`), `TruckPitfall` (`load` twice) | warning: Duplicate of other owned member name, on all four |
| `V4_MultipleInheritance` | `TruckPitfall` | warning: Duplicate of inherited member name 'rating' from Insured, Powered |
| `V4_MultipleInheritance` | `DiamondPitfall::p` | **nothing** |
| `V5_Typo` | `myCarPitfall::masss` | **nothing** |
| `V6_NameResolution` | `OwnedHidesImported::VehiclePitfall::mass` (subsets itself) | **nothing** |
| `V6_NameResolution` | `RedefinitionFallsThroughToImport::CarPitfall::mass` (redefines `ISQ::mass`) | **nothing** |
| `V7_ParametersRedefineByPosition` | `AuditedTransferPitfall` (parameters swapped) | **nothing** |
| `V7_ParametersRedefineByPosition` | `RefundPitfall::target` (redefines two parameters) | **nothing** |
| `V9_SpecificationExample` | `TestWithCutoffPitfall::cutoff` (the SysML spec's own example, 7.13.4) | warning: Duplicate of inherited member name 'cutoff' from TestRecordAsPublished |
| `NameCollisionErrorForm` | `carCorrectForm` / `carPitfall` | **error** on the correct form; only a warning on the pitfall |

None of the `…Fixed` elements produces a diagnostic.

This repository's validator reports every pitfall above, including the five the pilot misses, and none of
the fixed forms (`NameCollisionCheckerTest.cameoExample` checks this on every build):

```bash
java -jar validator-cli/target/sysml-validator.jar examples/name-collision/NameCollisionExamples.sysml
```

## Reproducing the pilot results (`pilot-repro/`)

`PilotProbe.java` runs SysML files through `org.omg.sysml.interactive.SysMLInteractive`, the engine of the pilot's
Jupyter kernel. It prints every diagnostic and what each redefinition, subsetting and reference resolved to,
including computed implicit redefinitions. `models/` holds one model per variant of the analysis, and
`output-0.62.0.txt` / `output-0.63.0.txt` hold the captured results, which are identical.

1. Download `jupyter-sysml-kernel-<version>.zip` from the pilot's GitHub release (tag `2026-08` is 0.62.0 and tag
   `2026-09` is 0.63.0) and unzip it.
2. From `pilot-repro/`, with Java 21 or later:

```bash
java -cp <kernel>/sysml/jupyter-sysml-kernel-0.63.0-all.jar PilotProbe.java <kernel>/sysml/sysml.library models/*.sysml
```

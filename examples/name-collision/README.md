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
- **`MassTypedByMass.sysml`** is the car whose mass "is of type mass", written the ways the language rejects:
  `attribute mass : mass;` (typed by itself), `: ISQ::mass` (a library usage) and `: kg` (a unit). It sits next to the
  correct forms, `attribute :>> mass : MassValue;` and `attribute :>> mass :> ISQ::mass;`. It is a separate file because
  the pitfalls are errors. The forms that are only warnings are in `V11_QuantityTypedMass`.

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
| `V10_Aliases` | `AliasOrRename::CarPitfall::curbMass` (a renaming redefinition meant as a second name) | **nothing**; an error only where `mass` is then used |
| `V10_Aliases` | `AliasHidesInheritedName::CarPitfall` (`alias mass for ISQ::mass`) | warning: Duplicate of inherited member name 'mass' from Vehicle |
| `V10_Aliases` | `FeatureNamedLikeInheritedAlias::myCarPitfall::weight` | warning: Duplicate of inherited member name 'weight' from Vehicle |
| `V10_Aliases` | `RedefinitionDropsAlias::myCarPitfall` (`:>> weight` drops the alias `weight`) | **nothing**; an error only where `weight` is then used |
| `V10_Aliases` | `AliasFromTwoGenerals::WeighedCarPitfall` | warning: Duplicate of inherited member name 'weight' from Metric, Scale |
| `V11_QuantityTypedMass` | `CarPitfall::mass : MassValue`, `myCarPitfall::mass = 1500 [kg]` | warning: Duplicate of inherited member name 'mass' from Vehicle, on each |
| `NameCollisionErrorForm` | `carCorrectForm` / `carPitfall` | **error** on the correct form; only a warning on the pitfall |
| `MassTypedByMass` | `mass : mass`, `mass : ISQ::mass`, `mass : kg` | **error** "An attribute must be typed by attribute definitions", and the duplicate warning, on each |

None of the `…Fixed` elements produces a diagnostic.

This repository's validator reports every pitfall above except `AliasOrRename`. A renaming redefinition is legal, so
it cannot be told from a mistake. The validator also reports none of the fixed forms, and it warns at
`RedefinitionDropsAlias::myCarPitfall`, where the pilot is silent until the alias name is used.
`NameCollisionCheckerTest.cameoExample` checks all of this on every build:

```bash
java -jar validator-cli/target/sysml-validator.jar examples/name-collision/NameCollisionExamples.sysml
```

## Reproducing the pilot results (`pilot-repro/`)

`PilotProbe.java` runs SysML files through `org.omg.sysml.interactive.SysMLInteractive`, the engine of the pilot's
Jupyter kernel. It prints every diagnostic and what each redefinition, subsetting, typing by a feature and reference
resolved to, including computed implicit redefinitions. It also prints each type's inherited memberships, with an
alias shown as `element as aliasName`. A `REF` line gives the referenced element's own name, so a reference
through an alias shows the aliased feature's name. `models/` holds one model per variant of the analysis, and
`output-0.62.0.txt` / `output-0.63.0.txt` hold the captured results, which are identical.

1. Download `jupyter-sysml-kernel-<version>.zip` from the pilot's GitHub release (tag `2026-08` is 0.62.0 and tag
   `2026-09` is 0.63.0) and unzip it.
2. From `pilot-repro/`, with Java 21 or later:

```bash
java -cp <kernel>/sysml/jupyter-sysml-kernel-0.63.0-all.jar PilotProbe.java <kernel>/sysml/sysml.library models/*.sysml
```

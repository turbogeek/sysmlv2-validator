# User Guide

## KPAR models

A KPAR (KerML Project Archive, `.kpar`) is a zip file with the project description `.project.json` at its
root, usually a `.meta.json` (index and checksums), and the model files (`.sysml`, `.kerml`) at any path
inside it. Cameo 26xR2 and the SysML v2 reference implementation load models packaged this way, and so does
the validator:

```
java -jar validator-cli/target/sysml-validator.jar StockTicker.kpar
java -jar validator-cli/target/sysml-validator.jar -r models/      # finds .sysml, .kerml and .kpar files
```

- The model files of an archive are validated together, so a name in one resolves against the others, just
  as when the files are given together on the command line. The archive gives one result, and the CLI counts
  it as one file.
- Each error and warning names the model file it is in and keeps the line and column within that file. The
  CLI prints `StockTicker.kpar!StockTickerLogical.sysml:154:16`. Through the API
  (`ValidationError.getFilePath()`) the path is `<archive path>!<member path>`, and
  `KparArchive.memberOf` and `KparArchive.archiveOf` split it.
- Archives given together, or with plain files, are validated together (`validateAll`): names resolve
  across all of them.
- The `usage` list of `.project.json` (the projects this one depends on) is not followed: the standard
  libraries come from the library path, and other projects must be given on the same command line.
  `.meta.json` is not checked.
- An archive that is not a KPAR gives one `IO_INVALID_KPAR` error at line 0: a file that is not a zip, a zip
  without `.project.json` at its root or whose `.project.json` is not a JSON object, a zip without any
  `.sysml` or `.kerml` file, a model file that is not UTF-8 text, or corrupted zip data.

## Inheritance warnings

When a definition or usage has two or more general types, it inherits every feature of each of them,
except the features it redefines itself. The validator warns when that leaves a contradiction the
inheriting element does not resolve. Both checks are warnings: the file still validates.

General types are the specializations of a definition (`part def D :> B, C;`) and the typings, subsettings
and redefinitions of a usage (`part d : B, C;`, `part :>> engine : V8;`). Library types count as ancestors.
Only the elements of the files being validated are checked, never the libraries themselves.

### DIAMOND_INHERITANCE_CONFLICT

Two generals of D share an ancestor A, and a feature `f` of A is redefined differently along the two
branches. D then inherits both redefinitions.

```sysml
part def Vehicle { attribute range : Real; }
part def ElectricVehicle :> Vehicle { attribute :>> range default 400.0; }
part def CombustionVehicle :> Vehicle { attribute :>> range default 700.0; }
part def HybridVehicle :> ElectricVehicle, CombustionVehicle;   // warning: range is 400.0 and 700.0
```

The message names D, the feature, the common ancestor, each branch with its redefinition, and whether the
redefinitions:

- **contradict**: different literal values (numbers with or without units, strings, Booleans) or
  enumeration literals (`= Color::red` and `= Color::green`), types where neither specializes the other,
  multiplicities that share no value (`[1]` and `[2..*]`), or bodies that redefine a nested feature in
  contradicting ways;
- **are duplicates**: written the same way on both branches (`= 4` and `= 4`), so the model repeats itself.
  Numbers are compared by value, so `= 4` and `= 4.0` are duplicates too.

A redefinition without a value of its own carries the values of the features it redefines, so two bare
redefinitions (`attribute :>> dim;`) contradict when the values they inherit differ.

Redefinitions that only refine each other compatibly are not reported: one branch narrows the type to a
subtype, adds a value or a multiplicity the other leaves open, or redefines different nested features.
Values given by expressions (`= mRef.dimensions` and `= 3`) are not compared, since they may agree.

To resolve the conflict, redefine **every** branch's feature in D, by qualified name, to state which value
applies, or move the redefinition into a common supertype so it is inherited once:

```sysml
part def HybridVehicle :> ElectricVehicle, CombustionVehicle {
    attribute range :>> ElectricVehicle::range, CombustionVehicle::range = 1000.0;
}
```

Redefining the common ancestor's feature (`attribute value :>> Vehicle::range = 1000.0;`) resolves it too,
because a feature that redefines what D redefines is no longer inherited (KerML `removeRedefinedFeatures`).

These do **not** resolve it, and are still reported:

- `attribute :>> range = 1000.0;` (or `redefines range`, `range :>> range`): a redefinition by simple name
  names *one* feature, that of the first general in declaration order. `CombustionVehicle::range` stays
  inherited, so D has two features named `range`. The SysML v2 reference implementation reports
  "Duplicate of inherited member name 'range' from CombustionVehicle" for this (see OMG issue KERML11-72).
- Redefining one branch's redefinition (`:>> CombustionVehicle::range`, or renamed:
  `attribute value :>> ElectricVehicle::range`): the other branch's value still constrains `Vehicle::range`.
- Redefining a feature of the same name in an unrelated type (`:>> Other::range`).

If the branches give their values with `=` (a bound value) rather than `default`, D cannot override them at
all: that is an error, `SEMANTIC_BOUND_VALUE_OVERRIDDEN` (see below). Make the branch values defaults first.

### MULTIPLE_INHERITANCE_NAME_CONFLICT

Two generals contribute different features with the same name, and neither redefines the other, so D has
two features with that name.

```sysml
part def ElectricVehicle { attribute serviceInterval : Real; }
part def CombustionVehicle { attribute serviceInterval : Real; }
part def HybridVehicle :> ElectricVehicle, CombustionVehicle;   // warning: two serviceIntervals
```

Resolve it by redefining both in D (`attribute serviceInterval :>> ElectricVehicle::serviceInterval,
CombustionVehicle::serviceInterval;`), by renaming one of them, or by moving the feature into a common
supertype. Redefining only one of them under the same name (`attribute :>> ElectricVehicle::serviceInterval;`)
is not enough: the other one remains beside D's new `serviceInterval`.

### Not reported

- A feature inherited along two paths without being redefined: it is the same feature.
- Redundant specialization, `D :> B, A` where `B :> A`: everything A has, D already inherits through B.
- A conflict that one general already has by itself (or settled where it arose): it is reported on that
  general.
- A conflict that another inherited feature resolves by redefining both branches
  (`ref self : SuccessionFlow :>> Flow::self, FlowTransferBefore::self;`).
- Parameters, `subject`, `objective`, actors, stakeholders and connector `end` features: they redefine the
  general's parameters by position.
- Private features: specializations do not inherit them.
- The implicit library supertypes (`Parts::Part`, `Items::Item`, ...) that are not written in the model.
- Elements whose general types or redefined features do not resolve.

See `test-suite/positive/62_diamond_unresolved_warnings.sysml` and its refactored version
`test-suite/positive/61_diamond_resolved.sysml`.

## Name-collision warnings

Declaring a feature is so easy that it is easy to create a new one where an existing one was meant:
`attribute mass = 1500.0;` in a usage of `Vehicle` declares a *second* `mass` instead of giving the
inherited one a value. These checks report such names. The analysis behind them, with the specification
rules and what the SysML v2 reference implementation does, is in
[NAME_COLLISION_ANALYSIS.md](NAME_COLLISION_ANALYSIS.md); `examples/name-collision/` has each case next to
its fix.

| Code | Example | Fix |
|---|---|---|
| `INHERITED_NAME_REDECLARED` | `part def Car :> Vehicle { attribute mass : Real; }` | `attribute :>> mass : Real;` |
| `DUPLICATE_MEMBER_NAME` | `attribute mass; attribute mass;` in one definition | remove or rename one |
| `RENAMED_INHERITED_NAME_REUSED` | `A1 :> A { p1 :>> p; }`, then `B :> A1, A2 { part p; }` | `part :>> p1;` |
| `NAME_SIMILAR_TO_INHERITED` | `attribute masss = 1500.0;` or `:>> masss` beside an inherited `mass` | `attribute :>> mass = 1500.0;` |
| `REDEFINITION_NOT_INHERITED` | `attribute :>> mass = 1500 [kg];` where no general has `mass` and `ISQ::*` is imported: it redefines `ISQ::mass` | redefine an inherited feature, or qualify the target |
| `FEATURE_REFERS_TO_ITSELF` | `attribute mass :> mass;` (the owner's own members are found first) | `attribute mass :> ISQ::mass;` |
| `PARAMETER_POSITION_MISMATCH` | `in target; in source;` under `Transfer { in source; in target; }` | declare parameters in the general's order |
| `PARAMETER_REDEFINES_TWO` | `in target :>> target;` as parameter 1 of a `Transfer` specialization | declare parameters in the general's order |
| `ALIAS_NAME_COLLISION` | `part def Car :> Vehicle { alias mass for ISQ::mass; }`: `mass` in `Car` and its specializations now means `ISQ::mass`, and `:>> mass` in a specialization redefines it. Also an alias and a different member with one name inherited from two generals. | `alias isqMass for ISQ::mass;` |
| `INHERITED_ALIAS_DROPPED` | `part myCar : Vehicle { attribute :>> mass = 1500.0; }` where `Vehicle` declares `alias weight for mass;`: a redefinition drops the inherited aliases of the feature, so `weight` no longer resolves in `myCar` | `attribute :>> mass = 1500.0; alias weight for mass;` |

`INHERITED_NAME_REDECLARED` also covers a new feature with the name of an inherited alias
(`attribute weight : Real;` where `Vehicle` declares `alias weight for mass;`). An alias of the inherited
feature itself, such as `alias weight for mass;` in `Car :> Vehicle`, is not reported. It adds a name without
adding a feature, so it is the way to give an inherited feature another name. Note that
`attribute weight :>> mass;` instead renames the feature, and `mass` then no longer resolves.

All are warnings except two errors:

- `SEMANTIC_BOUND_VALUE_OVERRIDDEN`: a redefinition gives a value to a feature whose value is bound (`=`)
  or initial (`:=`), possibly through a chain of redefinitions. Only a `default` value may be overridden
  (KerML `validateFeatureValueOverriding`; the reference implementation reports "Cannot override a binding
  feature value"). Declaring a new feature with the same name instead is not a fix: that is the
  `INHERITED_NAME_REDECLARED` pitfall.
- `SEMANTIC_TYPED_BY_USAGE`: a usage typed by a usage, which is a feature, where a classifier such as a
  definition belongs. The usual cases are
  `attribute mass : mass;` (the owner's own members are found first, so `mass` is the attribute itself),
  `attribute mass : ISQ::mass;` (a library quantity) and `attribute mass : kg;` (a unit). Type it by a
  definition (`: MassValue`), subset the library quantity (`:> ISQ::mass`), or redefine the inherited feature
  (`attribute :>> mass : MassValue;`). The reference implementation reports these as errors too, for example
  "An attribute must be typed by attribute definitions". A typed-by-itself attribute that also has the name of
  an inherited feature gets `INHERITED_NAME_REDECLARED` as well, as in the reference implementation. KerML
  files are not checked: there a feature may type a feature.

Not reported as name collisions:

- Redefinitions, by simple or qualified name, and an owned feature that redefines what the inherited one
  redefines (it replaces it).
- References that look like declarations: `event e;`, `satisfy r by p;`, `metadata M about x;`,
  `perform a;`, `then join j;`, `dependency a to b;`, the cross feature of an `end`.
- Parameters, `subject`, `objective`, actors, stakeholders and `end` features under the re-declaration
  check: they redefine by position (the two parameter checks cover them), and features in metadata bodies,
  which redefine by name.
- For typos: names shorter than four characters, names that differ only in digits (`port1`, `port2`), and a
  new feature that subsets the similar one on purpose (`fraction1 :> fractions`).
- `REDEFINITION_NOT_INHERITED` only when the target is a library feature and everything the owner inherits
  is in the validated files; redefining a package-level feature of your own model is a deliberate pattern.

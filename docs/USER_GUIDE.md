# User Guide

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
part def ElectricVehicle :> Vehicle { attribute :>> range = 400.0; }
part def CombustionVehicle :> Vehicle { attribute :>> range = 700.0; }
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

To resolve the conflict, redefine the feature in D to state which value applies, or move the redefinition
into a common supertype so it is inherited once:

```sysml
part def HybridVehicle :> ElectricVehicle, CombustionVehicle {
    attribute :>> range = 1000.0;
}
```

Redefining `f` by name (`attribute :>> range ...`, `attribute range :>> range ...`), redefining either
branch's redefinition (`:>> ElectricVehicle::range`), or redefining the ancestor's feature
(`:>> Vehicle::range`) all count as resolving it. Redefining a feature of the same name in an unrelated
type (`:>> Other::range`) does not.

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

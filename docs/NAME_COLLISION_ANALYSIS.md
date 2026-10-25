# Accidental re-declaration instead of redefinition: name-collision hazards in KerML / SysML v2

**Analysis and proposed resolution for the OMG KerML 1.1 RTF, the SysML 2.1 RTF and the SysML v2 Pilot Implementation**

| | |
|---|---|
| Submitted by | *[name, organization: to be completed by the submitter]* |
| Date | 2026-10-25 |
| Specifications analyzed | *Kernel Modeling Language v1.1, Beta 2* (`1-Kernel_Modeling_Language.pdf`) and *Systems Modeling Language v2.1, Beta 2, Part 1* (`2a-OMG_Systems_Modeling_Language.pdf`), as shipped in SysML-v2-Release tag `2026-08` (commit `fb97b754f29588b8e9c7a35f370880cd15eb29e7`) |
| Grammar | `bnf/KerML-textual-bnf.kebnf`, `bnf/SysML-textual-bnf.kebnf` (same release) |
| Confirmed by execution | Pilot **0.62.0** (tag `2026-08`) and **0.63.0** (tag `2026-09`, released 2026-10-22, the latest release): identical results; see §2.10 |
| Working branch reviewed | `master` at `850d39f` (2026-10-23, version 0.64.0 in development) and the active branches `ST6RI-904` (validation refactoring, head `aa83e7d`) and `ST6RI-969` (implicit-specialization refactoring): none changes the behavior analyzed here; see §2.11 |
| Reference implementation (source read) | SysML-v2-Pilot-Implementation tag `2026-08`, commit [`692170b71867353b8f90341e61556f49a5beb0e5`](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/tree/692170b71867353b8f90341e61556f49a5beb0e5), bundle version `0.62.0` ([MANIFEST.MF](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/META-INF/MANIFEST.MF)) |

**Page numbers** are the printed page numbers of the spec PDFs. For KerML the PDF page is the printed page + 26; for SysML Part 1 it is the printed page + 32.

**Evidence key for claims about the pilot implementation.** Claims were first derived from the source at the commit above, then every model in §2 was run in pilot 0.62.0 and 0.63.0 (§2.10).

- **[SRC]**: verified by reading the cited source lines.
- **[TEST]**: verified by reading an Xpect test in the pilot repository that asserts this diagnostic. The test was read, not run.
- **[RUN]**: confirmed by running the model in pilot 0.62.0 and 0.63.0 (§2.10).
- **[INF]**: inferred by tracing the cited source. Not confirmed by execution (only one claim remains in this category).

Appendix A lists every pilot claim with its evidence level.

---

## 1. Summary

**The problem in six lines**

1. In KerML and SysML v2, any named feature or usage declaration creates a **new** feature. It is related to an inherited feature only through `:>>`/`redefines`, or through the few positional implicit redefinitions (parameters, ends).
2. So `part def Car :> Vehicle { attribute mass : Real; }` and `part myCar : Car { attribute mass = 1500.0; }` each add *another* feature named `mass`. Constraints, roll-ups and requirements written against `Vehicle::mass` never see the value.
3. KerML already forbids the same-name case (inherited names must be distinct from owned names). But the rule is only formalized indirectly, has no stated severity, has a metaclass loophole, and is violated by the standard libraries and by a SysML spec example. The pilot reports it only as a **warning**.
4. Several closely related variants get **no diagnostic at all**:
   - typos (`masss`);
   - diamonds where one path renamed the feature;
   - unqualified `:>> mass` silently resolving to an imported library feature such as `ISQ::mass`;
   - swapped parameter order, because parameters redefine by position and not by name;
   - an explicit by-name parameter redefinition that the pilot also pairs by position, making it redefine two parameters (confirmed by running the pilot).
5. There is also an incentive problem. Overriding a bound value with the correct form (`:>> mass = …`) is an **error**, while the incorrect form is only a **warning**.
6. **Aliases** are part of the remedy and add two hazards (V10):
   - `alias weight for mass;` gives an inherited feature another name without creating a feature, so an alias is the right tool when the intent is only to name it;
   - but an alias can take an inherited feature's name and redirect it, `:>>` included, with only a warning;
   - and redefining a feature silently drops the aliases a general declared for it. The alias name then fails to resolve, an error reported only where the name is used, far from the cause.

**Proposed resolution (detail in §5)**

- **Specification**
  - Add a dedicated validation constraint, reported as an **error**, in two parts:
    - an owned feature whose (effective) name or short name equals that of an inheritable feature, **or of an inheritable alias of a feature**, must redefine that feature;
    - an owned **alias** whose name or short name equals that of an inheritable member must stand for the same element.

    Aliasing the inherited feature, under its own name or another, stays valid: it creates no feature (V10a).
    Two OCL fixes make that true of the formal rules too: a same-element case in `isDistinguishableFrom` (K2), and
    a rule-1 fix in `removeRedefinedFeatures`, without which an alias and its feature remove each other (K10).
  - Specify what happens to a general's aliases of a feature that a specialization redefines. Today they are silently dropped (K9).
  - Remove the metaclass escape from `Membership::isDistinguishableFrom`.
  - Fix the text/OCL inconsistencies: imported-membership hiding, `resolveLocal` ordering, parameter redefinition text vs. OCL, and the unformalized "redefined features must be inheritable" rule.
  - Add non-normative advisory checks (warnings) for near-miss names, renamed diamonds, positional mismatches and redefinition targets that are not inherited.
  - Correct the spec example and the colliding library models.
- **Pilot implementation**
  - Make the check an error with a specific message and a **quick fix that inserts `:>>`**.
  - Add the advisory checks, including a "did you mean `:>> mass`?" hint for typos.
  - Report the alias cases as specifically, and make the linking error for a dropped alias name its cause.
  - Make the two name-resolution algorithms in the pilot agree.

---

## 2. Minimal reproducing models

Each variant gives:

- what the modeler **intended**;
- what the language **actually means** (spec citations);
- what the pilot **reports today** (source citations, evidence level).

Unless stated otherwise, models are SysML textual notation and import `ScalarValues::*` for `Real`.

### V1. Re-declaring an inherited feature in a specialized definition (the canonical case)

```sysml
package V1 {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Car :> Vehicle {
        attribute mass : Real;          // intended: redefine Vehicle::mass
    }
    // correct:  attribute :>> mass : Real;   or   attribute mass redefines mass : Real;
}
```

**Intended.** `Car::mass` is `Vehicle::mass`, perhaps with a narrower type or multiplicity.

**Actual semantics.**

- `Car::mass` has no owned redefinition. So `Vehicle::mass` is not removed by `Type::removeRedefinedFeatures` (KerML 8.3.3.1.10, p.147) and stays in `Car.inheritedMembership`.
- `inheritedMembership` subsets `membership` (KerML 8.3.3.1.10, p.144).
- `Car` therefore has two memberships named `mass` whose member elements are both `AttributeUsage`s, so `Membership::isDistinguishableFrom` is false (KerML 8.3.2.4.3, p.127). This violates `validateNamespaceDistinguishibility` (KerML 8.3.2.4.5, p.132).
- The prose states the rule directly (KerML 7.3.2.1, p.26; SysML 7.6.1, p.32). The KerML text requires inherited member names to be "distinct from each other and from the member names of all owned memberships".
- A model that violates a validation constraint is not valid, and the semantics are only defined for valid models (KerML 8.3.1, p.109; 8.4.2, p.240).
- If a tool processes the model anyway, `Car` instances carry two independent `mass` values. `Car::mass` does not subset `Vehicle::mass`.

**Pilot today.**

- Emits a **warning** `Duplicate of inherited member name 'mass' from Vehicle` on the declared name of `Car::mass` **[SRC][TEST]**:
  - `KerMLValidator.checkNamespace`, type branch: [L440-L446](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L440-L446);
  - per-member check: [L476-L503](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L476-L503);
  - message constant: [L135](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L135);
  - message format: [L512-L518](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L512-L518).
- The same message is asserted for `part b` in [`Membership_distingushability_invalid.sysml.xt` L35-L46](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xpect.tests/src/org/omg/sysml/xpect/tests/validation/invalid/Membership_distingushability_invalid.sysml.xt#L35-L46).
- `SysMLValidator` extends `KerMLValidator` and does not override `checkNamespace` ([L142](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xtext/src/org/omg/sysml/xtext/validation/SysMLValidator.xtend#L142)) **[SRC]**.
- The check was introduced in PR [#133](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/pull/133) "ST6RI-245 Membership Distinguishability Validation", merged 2020-08-10, whose description already calls it a warning **[SRC: PR text]**.
- There is **no quick fix**. Both quick-fix providers contain only a commented-out template:
  - [KerMLQuickfixProvider L15-L30](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext.ui/src/org/omg/kerml/xtext/ui/quickfix/KerMLQuickfixProvider.xtend#L15-L30);
  - [SysMLQuickfixProvider L15-L30](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xtext.ui/src/org/omg/sysml/xtext/ui/quickfix/SysMLQuickfixProvider.xtend#L15-L30) **[SRC]**.
- The same applies to non-feature members: a nested `classifier D` in `D :> C` that hides `C::D` gives `Duplicate of inherited member name 'D' from C` ([`MembershipTests_Distinguishability.kerml.xt` L31-L43](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xpect.tests/src/org/omg/kerml/xpect/tests/validation/MembershipTests_Distinguishability.kerml.xt#L31-L43)) **[TEST]**.

### V2. Giving an inherited feature a value on a usage

```sysml
package V2 {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Car :> Vehicle { attribute :>> mass; }      // correct redefinition
    part myCar : Car {
        attribute mass = 1500.0;   // (a) intended: value for the inherited mass
    }
    part yourCar : Car {
        mass = 1500.0;             // (b) keyword-less form: also a NEW feature
    }
    // correct:  attribute :>> mass = 1500.0;    or    :>> mass = 1500.0;
}
```

**Intended.** Bind the inherited `mass` of `myCar` to 1500.0.

**Actual semantics.**

- A usage inherits from its definitions as a definition inherits from its general definitions (SysML 7.6.1, p.32). `FeatureTyping` is a `Specialization`, and `Type::supertypes` uses all owned specializations (KerML 8.3.3.1.10, p.148).
- (a) declares a new `AttributeUsage` `myCar::mass`, whose feature value binds only that new feature (SysML 7.13.4, p.72). `Car::mass` and `Vehicle::mass` stay unvalued for `myCar`.
- The new usage also silently gets the default multiplicity `[1..1]`, because it has no owned subsetting or redefinition (SysML 7.6.3, p.41).
- (b) is a `DefaultReferenceUsage`: `RefPrefix Usage`, SysML BNF 8.2.2.6.3, `SysML-textual-bnf.kebnf` L348-L349. It is a new reference usage named `mass`.
- The **same characters** inside a metadata body are a redefinition: in `MetadataBodyUsage`, `:>>` is optional and the name is the *redefined* feature (`SysML-textual-bnf.kebnf` L1706-L1709; SysML 7.27.2, pp.158-159; KerML `MetadataBodyFeature` L1435-L1438, KerML 7.4.13, p.69).
- Both forms violate the distinctness rule in the same way as V1.

**Pilot today.**

- (a) and (b): warning `Duplicate of inherited member name 'mass' from Car` on the owned `mass` (same code path as V1) **[RUN]**.
- If `Car` itself has the V1 defect, `myCar` inherits two `mass` memberships (`Car::mass` and `Vehicle::mass`). Expected diagnostics, both warnings **[RUN]**:
  - `… 'mass' from Car, Vehicle` on the owned feature;
  - a second namespace-level warning on `myCar` from [L505-L510](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L505-L510).

### V3. Two owned members with the same name in one namespace

```sysml
package V3 {
    private import ScalarValues::*;
    part def Vehicle {
        attribute mass : Real;
        attribute mass : Real;        // (a) copy/paste duplicate, same metaclass
    }
    part def Truck {
        attribute def load;           // (b) definition named 'load' ...
        attribute load : Real;        //     ... and a usage named 'load' (different metaclass)
        attribute payload :> load;    //     which 'load' does this subset?
    }
}
```

**Actual semantics.**

- (a) violates `validateNamespaceDistinguishibility` (KerML 8.3.2.4.5, p.132; prose in KerML 7.2.5.2, p.23).
- (b) does **not** violate it. `isDistinguishableFrom` returns true when neither member element's metaclass conforms to the other's (KerML 8.3.2.4.3, p.127), and `AttributeDefinition` and `AttributeUsage` do not conform.
- So (b) is a "valid" namespace that maps one name to two memberships. That contradicts the note in KerML 8.2.3.5.3 (p.85) that a well-formed namespace has at most one local resolution per name.
- Resolving `load` in `:> load` is then under-determined:
  - `Namespace::resolveLocal` returns `first()` of the matching memberships (KerML 8.3.2.4.5, p.130), and the order of the derived union `membership` is not specified;
  - if the definition is picked, KerML 8.2.3.5.1 (p.83) says resolution **fails**, because the element has the wrong metaclass for its context.

**Pilot today.**

- (a): warning `Duplicate of other owned member name` on both declarations ([L426-L433](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L426-L433); message [L132](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L132)) **[SRC][TEST]**.
- (b): **also warns.** The pilot ignores the metaclass:
  - the validator compares names only (L482, L493) **[SRC]**;
  - `Membership_isDistinguishableFrom_InvocationDelegate` has `// TODO: Add member element metaclass check` ([L42](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/delegate/invocation/Membership_isDistinguishableFrom_InvocationDelegate.java#L37-L59)) **[SRC]**;
  - the KerML test expects warnings for a classifier, two packages and a feature all named `A` ([`MembershipTests_Distinguishability.kerml.xt` L17-L29](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xpect.tests/src/org/omg/kerml/xpect/tests/validation/MembershipTests_Distinguishability.kerml.xt#L17-L29)) **[TEST]**.
- So the reference implementation is *stricter* than the specification.
- For `:> load`, the pilot does not fail as the spec does:
  - `KerMLScope.addName` drops candidates that are not instances of the expected reference type (`Feature`) and keeps searching ([L233-L236](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScope.xtend#L233-L253)) **[SRC]**;
  - so it silently resolves to the usage **[RUN]**.

### V4. Same name inherited from two general types (multiple inheritance / diamond)

**(a) Unrelated features with the same name.**

```sysml
package V4a {
    private import ScalarValues::*;
    part def Powered { attribute rating : Real; }    // power rating
    part def Insured { attribute rating : Real; }    // insurance rating
    part def Truck :> Powered, Insured {
        assert constraint { rating > 0.0 }            // which 'rating'?
    }
}
```

**Actual semantics.**

- `Truck` has two inherited memberships named `rating`. This violates `validateNamespaceDistinguishibility` (KerML 7.3.2.1, p.26; SysML 7.6.1, p.32, which says such conflicts are resolved by redefining one of the features).
- Resolution falls back to the unspecified `first()` of KerML 8.3.2.4.5 (p.130).

**Pilot today.**

- A namespace-level warning on `Truck`, expected text `Duplicate of inherited member name 'rating' from Insured, Powered` ([L446](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L446), [L505-L518](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L505-L518)) **[SRC for the check; RUN for the exact text]**.
- The reference `rating` resolves to **`Powered::rating`**, because it is first in specialization order **[RUN]**:
  - `KerMLScope.gen` visits `ownedSpecialization` in declaration order ([L382-L393](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScope.xtend#L361-L414));
  - `addName` keeps the first feature found unless a later one redefines it ([L241-L247](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScope.xtend#L241-L247)).
- Writing `:> Insured, Powered` silently changes what the constraint checks.

**(b) One path renamed the feature, so a new same-named feature passes every rule.** This is the pilot's own test model.

```sysml
package RedefinitionDiamond {
    part A  { part p[*]; }
    part A1 :> A { p1 :>> p; }      // A::p renamed p1 along this path
    part A2 :> A { }                // A::p inherited unchanged along this path
    part B :> A1, A2 {
        part p;                     // intended (likely): constrain the inherited p; actual: new, unrelated feature
    }
}
```

**Actual semantics.**

- `removeRedefinedFeatures` rule 1 (KerML 8.3.3.1.10, p.147) removes `A::p` (inherited via `A2`) from `B`'s inherited memberships, because `A1::p1` redefines it.
- `B::p` therefore collides with nothing. Yet `B` instances still have `A::p` (equal to `p1`) as a feature, distinct from `B::p`.
- No rule flags this.

**Pilot today.**

- `XPECT noErrors` ([`RedefinitionDiamond0.sysml.xt` L27-L43](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xpect.tests/src/org/omg/sysml/xpect/tests/validation/valid/RedefinitionDiamond0.sysml.xt#L27-L43)) **[TEST]**.
- No distinguishability warning either, because `TypeAdapter.removeRedefinedFeatures` removes `A::p` before the check ([L149-L169](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/TypeAdapter.java#L149-L169)) **[RUN]**.
- If `A2` instead declares `p :>> p`, the pilot warns `Duplicate of inherited member name 'p' from A2` ([`RedefinitionDiamond_Invalid.sysml.xt` L27-L43](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xpect.tests/src/org/omg/sysml/xpect/tests/validation/invalid/RedefinitionDiamond_Invalid.sysml.xt#L27-L43)) **[TEST]**.

**(c) Redefinition inheritance anomaly.** Two redefinitions of the same feature can both be inherited, so that even correctly written models violate distinguishability. This is already reported as [KERML11-72](https://issues.omg.org/issues/KERML11-72) and, for SysML substates and `this`, [SYSML21-311](https://issues.omg.org/issues/SYSML21-311). It is not repeated here, but it blocks raising the severity of the rule (see §5).

### V5. A typo silently creates a new feature

```sysml
package V5 {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part myCar : Vehicle {
        attribute masss = 1500.0;   // intended: mass
    }
}
```

**Actual semantics.** A legal new feature `masss`. `Vehicle::mass` stays unvalued. No rule applies.

**Pilot today.**

- **No diagnostic.** Name comparison is exact equality (`nameMap`, [L451-L474](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L451-L474)). Neither validator contains similarity or edit-distance logic **[SRC]**.
- The correct form with a typo, `attribute :>> masss = 1500.0;`, does fail with an unresolved-reference error. But no "did you mean" proposal is offered, because both quick-fix providers override `queryScope` to return an empty set ([KerML L17-L19](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext.ui/src/org/omg/kerml/xtext/ui/quickfix/KerMLQuickfixProvider.xtend#L17-L19), [SysML L17-L19](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xtext.ui/src/org/omg/sysml/xtext/ui/quickfix/SysMLQuickfixProvider.xtend#L17-L19)) **[SRC]**.
- Xtext's `DefaultQuickfixProvider` uses `queryScope` to propose "Change to …" fixes for linking errors **[INF: Xtext framework behavior; IDE-only, not run]**.
- If the misspelled name happens to exist in an enclosing or imported scope, there is no error at all (V6b).

### V6. A reference resolves to a different element than intended (name-resolution precedence)

**(a) An owned member hides an imported one, including the feature being declared.**

```sysml
package V6a {
    private import ISQ::*;
    part def Vehicle {
        attribute mass :> mass;     // intended: subset the library quantity ISQ::mass
    }
}
```

**Actual semantics.**

- For an owned `Subsetting`, the local namespace is the owning namespace of the subsetting feature, here `Vehicle` (KerML 8.2.3.5.2, p.84).
- `Vehicle`'s local resolution of `mass` finds the declaration itself (KerML 8.2.3.5.3–8.2.3.5.4, p.85). The import in the outer package is never consulted.
- The feature subsets **itself**.
- Owned members hiding imported ones is intended (KerML 7.2.5.4, p.23), but nothing warns that the hidden name was the one the modeler meant.

**Pilot today.**

- Searches owned memberships first ([KerMLScope L224-L231](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScope.xtend#L224-L231)) **[SRC]**.
- Deliberately matches the element being declared by its *declared* name ([L278-L281](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScope.xtend#L278-L281)) **[SRC]**.
- Hence a self-subsetting, with **no diagnostic** for the cycle **[RUN]**.
- The release examples consistently avoid this by qualifying, even where `ISQ::*` is already imported. For example, `sysml/src/examples/Vehicle Example/VehicleDefinitions.sysml` L10 imports `ISQ::*` and L16 still writes `attribute mass :> ISQ::mass;`. This is evidence that the idiom is needed, not proof of the failure.

**(b) An unqualified redefinition target falls through to an imported library feature (no error at all).**

```sysml
package V6b {
    private import ISQ::*;
    private import SI::*;
    part def Vehicle { attribute totalMass :> ISQ::mass; }
    part def Car :> Vehicle {
        attribute :>> mass = 1500 [kg];   // intended: Vehicle's mass (actually named totalMass)
    }
}
```

**Actual semantics.**

- For the target of an owned redefinition, basic resolution is repeated with each general type as the local namespace (KerML 8.2.3.5.1, p.83; 7.3.4.5, p.37). Basic resolution of a simple name is *full* resolution, which proceeds outward from `Vehicle` to the enclosing package and its imports (8.2.3.5.4, p.85).
- `mass` therefore resolves to `ISQ::mass`, a package-level feature. KerML 7.3.4.5 (p.36) allows a feature featured by `Anything` to be redefined by a feature with another featuring type.
- `Car::mass` (effective name `mass`) redefines the library quantity, and `Vehicle::totalMass` stays unvalued.
- KerML 7.3.4.5 (p.37) says redefined features of owned redefinitions must be features that would otherwise be inherited. That sentence has **no OCL counterpart**: `Redefinition` (KerML 8.3.3.3.8, pp.176–177) has only `validateRedefinitionDirectionConformance`, `validateRedefinitionEndConformance` and `validateRedefinitionFeaturingTypes`.

**Pilot today.**

- For a redefinition target, the local `KerMLScope` searches only inherited members ([L212-L214](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScope.xtend#L208-L222)). But `KerMLScopeProvider.scopeFor` still chains the enclosing namespaces and the global scope as outer scopes ([L161-L173](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScopeProvider.xtend#L161-L173)) **[SRC]**.
- `checkRedefinition` reports a featuring-type error only when the *redefining* feature is package-level (`A package-level feature cannot be redefined`, [L215](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L215), [L704-L725](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L704-L725)) **[SRC]**.
- The pilot's own test accepts a definition redefining a package-level feature with `noErrors` ([`Redefinition_OwningType.sysml.xt` L28-L38](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xpect.tests/src/org/omg/sysml/xpect/tests/validation/valid/Redefinition_OwningType.sysml.xt#L28-L38)) **[TEST]**.
- Outcome for V6b: **no error and no warning**; `Car::mass` redefines `ISQBase::mass` **[RUN]**.
- Related open issue: [KERML11-140](https://issues.omg.org/issues/KERML11-140) asks for *broader* resolution of redefinition targets in a time-slice case, so any fix here must be coordinated with it (see §5.1 K5).

**(c) Order dependence under multiple inheritance.** See V4a: the first general type in declaration order wins in the pilot **[RUN]**. The spec leaves the choice to `first()` over an unspecified order (p.130).

**(d) The pilot has two resolution algorithms that disagree.**

```sysml
package V6d {
    private import ISQ::*;
    attribute mass : ScalarValues::Real;   // owned; hides imported ISQ::mass per KerML 7.2.5.4
}
```

- **Editor/linker** (`KerMLScope.resolve`): owned, then inherited, then imported ([L224-L231](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScope.xtend#L224-L231)) **[SRC]**.
- **Abstract-syntax operation** `Namespace::resolveLocal`, as exposed to API and OCL clients:
  - the delegate ([L40-L49](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/delegate/invocation/Namespace_resolveLocal_InvocationDelegate.java#L40-L49)) takes the first name match in `getMembership()` ([`NamespaceUtil.getNamedMembershipFor` L104-L111](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/util/NamespaceUtil.java#L104-L111));
  - the derived-union order of `getMembership()` is imported, then owned ([`NamespaceImpl` L138](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.model/src/main/java/org/omg/sysml/lang/sysml/impl/NamespaceImpl.java#L138)). For `Type` it is imported, owned, inherited ([`TypeImpl` L378](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.model/src/main/java/org/omg/sysml/lang/sysml/impl/TypeImpl.java#L378)) **[SRC]**;
  - imported memberships are not filtered for collisions ([`NamespaceImportAdapter` L45-L54](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/NamespaceImportAdapter.java#L45-L54), [`MembershipImportAdapter` L43-L58](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/MembershipImportAdapter.java#L43-L58)). This is consistent with the OCL defect [KERML11-75](https://issues.omg.org/issues/KERML11-75) **[SRC]**.
- Result: in the editor, `mass` inside `V6d` links to `V6d::mass`, but `V6d.resolveLocal('mass')` returns the imported `ISQBase::mass` membership **[RUN]**.

**(e) Global namespace.** If two root namespaces define the same top-level name, KerML 8.2.3.5.4 (p.85) explicitly leaves the choice undetermined. This is out of scope here but is the same class of hazard. See Sensmetry's *Unspecified Behavior in KerML Name Resolution* (attachment on issues.omg.org, linked to KERML_-43).

**(f) A usage typed by its own name: "the car's mass is of type mass".**

```sysml
package V6f {
    private import ISQ::*;
    private import SI::*;
    part def Vehicle { attribute mass : MassValue; }
    part def CarTypedByItself :> Vehicle {
        attribute mass : mass;              // intended: Vehicle's mass, typed as a mass quantity
    }
    part def CarTypedByLibraryUsage :> Vehicle {
        attribute mass : ISQ::mass;         // intended: typed by the library quantity 'mass'
    }
    part def CarTypedByUnit :> Vehicle {
        attribute mass : kg;                // intended: a mass in kilograms
    }
}
```

- As in (a), `mass` after `:` is resolved in `CarTypedByItself` first, where it finds the attribute being declared. The attribute is typed by **itself** **[RUN]**.
- `ISQ::mass` is an attribute *usage* (the library's mass quantity, typed `MassValue`), not a definition. So is the unit `kg` (`SI::kilogram`, typed `MassUnit`).
- A usage's types are classifiers: `Usage::/definition : Classifier` (SysML 8.3.6.4, p.270), nominally definitions, though kernel classifiers such as `ScalarValues::Real` are allowed too. An attribute usage's types are data types: `AttributeUsage::/attributeDefinition : DataType` (8.3.7.3, p.280). A usage is a feature, not a classifier, so it cannot be a usage's type.
- The pilot reports an **error** for each of the three forms, `An attribute must be typed by attribute definitions`, plus the V1 warning `Duplicate of inherited member name 'mass' from Vehicle`, because each declaration is also a second `mass` **[RUN]**. Its messages are stricter than the specification's wording (definitions only).
- The pilot reports the same kind of error for part, item, port, action, occurrence and `ref` usages and a subject typed by a usage (`V6f.sysml`, `Truck` and `EngineCheck`) **[RUN]**.
- Unlike (a), this case is caught. It is listed because the error names neither the cause (the name found the feature itself, or a library usage) nor the intended forms:
  - `attribute :>> mass : MassValue;` redefines `Vehicle::mass` with the quantity type;
  - `attribute :>> mass :> ISQ::mass;` redefines it and subsets the library quantity, which brings its type.

### V7. Implicit redefinition the language already performs, by position and not by name

**(a) Swapped parameters.**

```sysml
package V7a {
    item def Account;
    action def Transfer { in source : Account; in target : Account; }
    action def AuditedTransfer :> Transfer {
        in target : Account;    // position 1: implicitly redefines Transfer::source
        in source : Account;    // position 2: implicitly redefines Transfer::target
    }
}
```

**Actual semantics.**

- Owned parameters redefine the supertype parameter at the same position (KerML 7.4.7.2, p.52; `checkFeatureParameterRedefinition`, KerML 8.3.3.3.4, p.168; SysML 7.17.2, p.100; SysML 8.4.13.1, p.423).
- Names play no role. `AuditedTransfer::target` is `Transfer::source`.
- Both inherited parameters are redefined and therefore not inherited, so no distinguishability rule fires. Every use of `Transfer`'s body, flows and bindings is silently crossed.

**Pilot today.**

- `FeatureAdapter.addRedefinitions` pairs by index only ([L626-L643](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/FeatureAdapter.java#L626-L643), [L665-L672](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/FeatureAdapter.java#L665-L672), [L720-L725](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/FeatureAdapter.java#L720-L725)) **[SRC]**.
- The only check on implicit redefinitions is direction conformance (`KerMLValidator.checkFeature` [L616-L619](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L616-L619)) **[SRC]**.
- **No diagnostic** **[RUN]**.

**(b) An explicit by-name redefinition at a different position.**

```sysml
package V7b {
    item def Account;
    action def Transfer { in source : Account; in target : Account; }
    action def Refund :> Transfer {
        in target :>> target;   // intended: specialize only 'target' (it is at position 1 here)
    }
}
```

**Actual semantics: the text and the OCL disagree.**

- The prose says implicit redefinitions are supplied only if no redefinitions are given explicitly (KerML 7.4.7.2, p.52; SysML 7.17.2, p.100).
- The OCL of `checkFeatureParameterRedefinition` (p.168) requires the position-1 parameter to redefine `Transfer::source` regardless. It exempts only `InvocationExpression` parameters that have explicit redefinitions.
- When a semantic constraint is violated, a tool may insert the implied relationship, report the violation, or ignore it (KerML 8.3.1, p.109; 8.4.2, p.240).
- The meaning of `Refund` is therefore **tool-dependent**: `Refund::target` may redefine *both* `Transfer::target` and `Transfer::source`, which equates them, or the model may be flagged, or nothing may happen.

**Pilot today.**

- `FeatureAdapter.isComputeRedefinitions` does not skip features that have explicit owned redefinitions, except invocation and constructor-result features ([L520-L526](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/FeatureAdapter.java#L520-L526)) **[SRC]**.
- `TypeAdapter.removeUnnecessaryImplicitGeneralTypes` removes only implicit redefinitions that are *identical* to explicit ones ([L390-L422](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/TypeAdapter.java#L390-L422)) **[SRC]**.
- Result: `Refund::target` redefines both `Transfer::target` (explicit) and `Transfer::source` (implicit), with no diagnostic **[RUN]**. The two parameters of `Transfer` are thereby equated in `Refund`.

**(c) Where positional or by-name implicit redefinition is legitimate (to be exempted, see §5.5).**

- Connection ends:
  - `connection def SingleProductSelection :> ProductSelection { end item cart …; end [0..1] item selectedProduct …; }` (`sysml/src/examples/Association Examples/ProductSelection_UnownedEnds.sysml` L18-L21);
  - SysML 7.13.2, p.71; `checkFeatureEndRedefinition`, KerML p.166.
- Subject parameters (SysML 7.21.2, p.134).
- Metadata body features, which redefine by name (SysML 7.27.2, pp.158–159).
- The practitioner-facing asymmetry is real:
  - the training deck says subject and parameters are redefined automatically, so explicit redefinition is unnecessary (*Intro to the SysML v2 Language – Textual Notation*, slide 94 "Analysis Case Usages");
  - the same slide needs `attribute :>> fuelEconomy_city = …` for an ordinary attribute.
- `sysml/src/examples/Analysis Examples/Dynamics.sysml` L53-L66 mixes both styles in one usage: positional for the `in` parameters, explicit `:>>` for the `out` parameters.

### V8. The diagnostics reward the wrong form

```sysml
package V8 {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real = 1000.0; }   // bound (non-default) value
    part car1 : Vehicle { attribute :>> mass = 1500.0; }   // correct form
    part car2 : Vehicle { attribute mass = 1500.0; }       // pitfall form
}
```

**Spec.**

- `car1` violates `validateFeatureValueOverriding` (KerML 8.3.4.10.2, p.228): features redefined by a feature with a value must have only default values.
- `car2` violates only distinguishability.

**Pilot today.**

- `car1`: **error** `Cannot override a binding feature value` ([L360-L361](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L360-L361), [L1315-L1323](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L1315-L1323)) **[SRC]**.
- `car2`: **warning** `Duplicate of inherited member name 'mass' from Vehicle` **[RUN]**.
- A modeler "fixing" the error by deleting `:>>` produces a model with two masses (1000.0 and 1500.0).
- Related: [SYSML21-646](https://issues.omg.org/issues/SYSML21-646), "Attributes with Quantity Values are not redefinable."

### V9. The specification's own example contains the pitfall

SysML 7.13.4 *Feature Values*:

- `item def TestRecord { … attribute cutoff : Integer default = 0.75 * averageScore; }` (p.72).
- `item def TestWithCutoff :> TestRecord { attribute cutoff : Rational default = 0.75 * averageScore; }` (p.73).

`TestWithCutoff::cutoff` does not redefine `TestRecord::cutoff`. It is a second feature named `cutoff`, which is invalid under KerML 7.3.2.1 and SysML 7.6.1. The pilot would warn `Duplicate of inherited member name 'cutoff' from TestRecord` **[RUN]**.

Separately, `TestRecord::cutoff : Integer` is given a Rational-valued default expression.

### V10. Aliases: another name for an inherited feature, and its hazards

An alias is a non-owning membership. `alias weight for mass;` makes `weight` another name for `mass` in the aliasing
namespace without creating an element (KerML 7.2.5.2, p.22). A feature aliased in a type is related to the type by a
plain membership, not a feature membership, so it is not an owned feature of the type (KerML 7.3.2.6, p.30). Alias
memberships are inherited like any other visible membership (R4).

**(a) The intended use: another name for the inherited feature, not a new feature.**

```sysml
package V10a {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def CarSameName :> Vehicle { alias mass for Vehicle::mass; }
    part def CarNewName :> Vehicle { alias weight for mass; }
    part myCar : CarNewName {
        attribute m = mass;
        attribute w = weight;
    }
}
```

- No diagnostic. By the prose of 7.2.5.2, distinguishability compares names only between memberships of *different*
  elements (the OCL lacks this case; S12, K2). The pilot filters with `m.memberElement != memElement` ([L482, L493](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L478-L501)),
  so even an alias that repeats the inherited name for the same feature is valid **[SRC, RUN]**.
- `myCar` inherits `Vehicle::mass` under both names, and both references resolve to it **[RUN]**. As formalized,
  rule 1 of `removeRedefinedFeatures` would remove both memberships from `myCar`, because they share an element
  (S16, K10).
- **So the name can be aliased instead of redefined when the intent is only to name the inherited feature.**
  - An alias carries no value, type or multiplicity. When the modeler means to *change* the feature (V1, V2), a
    redefinition is still required.
  - An alias is not robust to specialization; see (d).

**(b) An owned alias takes an inherited feature's name and redirects it.**

```sysml
package V10b {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Car :> Vehicle {
        alias mass for ISQ::mass;           // intended: a shorthand for the library quantity
        attribute probe = mass;
    }
    part def SportsCar :> Car { attribute :>> mass; }
}
```

- Two memberships of `Car` named `mass` stand for different elements, which violates distinguishability. The pilot
  warns `Duplicate of inherited member name 'mass' from Vehicle` on the alias **[RUN]**.
- Owned memberships are found before inherited ones (V6a), so `mass` in `Car` now means `ISQBase::mass`, and `probe`
  is bound to the library quantity **[RUN]**.
- In `SportsCar`, `:>> mass` resolves through `Car`'s alias and redefines `ISQBase::mass`, not `Vehicle::mass`. The
  pilot warns `Duplicate of inherited member name 'mass' from Vehicle` there too **[RUN]**.
- The first draft of K1 could not catch this: it constrains owned *features*, and an alias is not a feature.

**(c) An owned feature takes the name of an inherited alias.**

```sysml
package V10c {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; alias weight for mass; }
    part def Car :> Vehicle { attribute weight : Real; }          // intended: Vehicle's mass, by its alias
    part def CarShortName :> Vehicle { attribute <weight> w : Real; }
}
```

- This is V1 reached through an alias name: `Car::weight` is a second, unrelated feature. The pilot warns
  `Duplicate of inherited member name 'weight' from Vehicle` on both declarations **[RUN]**.
- The first draft of K1 would miss it too. It selected the inherited memberships with
  `selectByKind(FeatureMembership)`, and an inherited alias is a plain `Membership`.

**(d) A redefinition drops the general's aliases of the feature.**

```sysml
package V10d {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; alias weight for mass; }
    part def CarViaName :> Vehicle { attribute :>> mass = 1500.0; }
    part def CarViaAlias :> Vehicle { attribute :>> weight = 1500.0; }
    part c1 : CarViaName { attribute w = weight; }
    part c2 : CarViaAlias { attribute w = weight; }
    part c3 : Vehicle {
        attribute :>> weight = 1500.0;
        attribute w = weight;
    }
}
```

- `removeRedefinedFeatures` (R12) removes every inheritable membership whose member element is a redefined feature.
  It does not distinguish owning memberships from aliases, so the alias `weight` goes with `mass`. The pilot does the
  same ([`TypeAdapter.removeRedefinedFeatures` L149-L168](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/TypeAdapter.java#L149-L168)) **[SRC]**.
- `:>> weight` resolves through the alias and redefines `Vehicle::mass`. The redefining feature's effective name is
  `mass`, the name of the redefined feature (KerML 7.3.4.5, p.37), not `weight` **[RUN]**.
- In `c1`, in `c2`, and even in `c3`, where the name was written one line earlier, `weight` does not resolve:
  `Couldn't resolve reference to Element 'weight'` **[RUN]**.
  - The error appears at each use, not at the redefinition that caused it.
  - Without a use there is no diagnostic at all.
- The specification does not say whether this is intended. A modeler who declares an alias in a general definition
  can reasonably expect it to name the feature in every specialization, redefined or not.
- Work-around: declare the alias again after the redefinition
  (`attribute :>> mass = 1500.0; alias weight for mass;`). It then names the redefining feature **[RUN]**. (In
  further specializations, both names survive in the pilot; as formalized, they need K10.)

**(e) Rename or alias: a named redefinition removes the old name.**

```sysml
package V10e {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def CarRenamed :> Vehicle { attribute weight :>> mass; }   // intended: a second name for mass
    part def CarAliased :> Vehicle { alias weight for mass; }
    part r : CarRenamed { attribute rm = mass; attribute rw = weight; }
    part a : CarAliased { attribute am = mass; attribute aw = weight; }
}
```

- `CarRenamed` *renames* the feature. `mass` no longer resolves in `CarRenamed` or its specializations
  (`r::rm`: `Couldn't resolve reference to Element 'mass'`). In `CarAliased`, both names resolve to `Vehicle::mass`
  **[RUN]**.
- Renaming by redefinition is legitimate, and the libraries use it, so it cannot be reported in general. It is listed
  because modelers reach for it when they mean an alias. To rename and keep the old name, add an alias:
  `attribute weight :>> mass; alias mass for weight;` (no diagnostic).

**(f) An alias and a different member with one name, from two generals.**

```sysml
package V10f {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Metric :> Vehicle { alias weight for mass; }
    part def Imperial :> Vehicle { alias weight for mass; }
    part def Scale { attribute weight : Real; }
    part def BothAliases :> Metric, Imperial { attribute p = weight; }
    part def MetricFirst :> Metric, Scale { attribute q = weight; }
    part def ScaleFirst :> Scale, Metric { attribute q = weight; }
}
```

- `BothAliases` inherits two aliases of the same feature. There is no diagnostic **[RUN]**. This is valid by the prose,
  and needs K2 and K10 to be valid as formalized (S12, S16).
- `MetricFirst` and `ScaleFirst` each get the warning `Duplicate of inherited member name 'weight' from Metric, Scale`.
  As in V4a, `weight` resolves to the member from the general listed first: `Vehicle::mass` in `MetricFirst`, and
  `Scale::weight` in `ScaleFirst` **[RUN]**.

**Fixed forms** (`pilot-repro/models/V10g.sysml`, no diagnostic **[RUN]**). `Vehicle` is as in (d):

```sysml
part def CarFixed :> Vehicle {
    attribute :>> mass : Real;
    alias weight for mass;              // declared again: names CarFixed::mass
}
part def CarAliasFixed :> Vehicle { alias isqMass for ISQ::mass; }
```

### 2.10 Confirmation by execution (pilot 0.62.0 and 0.63.0)

**Method.** Each model above was run through `org.omg.sysml.interactive.SysMLInteractive`, the engine behind the
pilot's Jupyter kernel, from the official release artifacts `jupyter-sysml-kernel-0.62.0.zip` (tag `2026-08`) and
`jupyter-sysml-kernel-0.63.0.zip` (tag `2026-09`), each with the standard library shipped in the same archive. A small
harness printed every diagnostic, and what each redefinition, subsetting, typing by a feature and feature reference
resolved to, including computed implicit redefinitions (`FeatureUtil.getRedefinedFeaturesWithComputedOf`) and inherited
memberships, aliases included (`TypeUtil.getInheritedMembershipOf`). The harness, the models and the captured output are in
`examples/name-collision/pilot-repro/`. **Both versions produced identical output.**

| Variant | Diagnostics reported by the pilot | What the model actually means (observed) |
|---|---|---|
| V1 | warning `Duplicate of inherited member name 'mass' from Vehicle` | `Car` inherits `Vehicle::mass` and owns a second `mass` |
| V1 fixed (`:>> mass`) | none | `Car::mass` redefines `Vehicle::mass` |
| V2 (a), (b) | warning `… 'mass' from Car` on each | `myCar` and `yourCar` inherit `Car::mass` and own a second `mass` |
| V2 when `Car` has the V1 defect | warnings `… 'mass' from Vehicle` (on `Car::mass`), `… 'mass' from Car, Vehicle` (on `myCar::mass`) and the same on `myCar` itself | `myCar` inherits both `Car::mass` and `Vehicle::mass`, and owns a third |
| V3 (a), (b) | warning `Duplicate of other owned member name` on all four declarations | `payload :> load` subsets the usage `Truck::load` |
| V4a | warning `Duplicate of inherited member name 'rating' from Insured, Powered` on the type | `rating` resolves to `Powered::rating` in `Truck :> Powered, Insured`, and to `Insured::rating` in `Truck2 :> Insured, Powered` |
| V4b | none | `B` inherits only `A1::p1`; `B::p` is a new, unrelated feature |
| V5 | none | new feature `masss` |
| V6a | none | `Vehicle::mass` subsets itself |
| V6b | none | `Car::mass` redefines `ISQBase::mass`; `Vehicle::totalMass` is not redefined |
| V6d | none | the linker resolves `mass` to `V6d::mass`; `V6d.resolveLocal('mass')` returns `ISQBase::mass` |
| V7a | none | `AuditedTransfer::target` redefines `Transfer::source`; `AuditedTransfer::source` redefines `Transfer::target` |
| V7b | none | `Refund::target` redefines **both** `Transfer::target` and `Transfer::source` |
| V8 | `car1`: **error** `Cannot override a binding feature value`; `car2`: warning `… 'mass' from Vehicle` | `car2` has two masses |
| V9 | warning `Duplicate of inherited member name 'cutoff' from TestRecord` | `TestWithCutoff` inherits `TestRecord::cutoff` and owns a second `cutoff` |
| V6f | on each of the three `mass` pitfalls: **error** `An attribute must be typed by attribute definitions` and warning `… 'mass' from Vehicle`; on `Truck`'s and `EngineCheck`'s features: **errors** such as `An occurrence, item or part must be typed by occurrence definitions`, `A port must be typed by port definitions`, `A usage must be typed by definitions`; the fixed forms: none | `CarTypedByItself::mass` is typed by itself, `CarTypedByLibraryUsage::mass` by `ISQBase::mass`, `CarTypedByUnit::mass` by `SI::kilogram` |
| V10a | none | `myCar` inherits `Vehicle::mass` as `mass` and as `weight`; both resolve to it |
| V10b | warning `… 'mass' from Vehicle` on the alias and on `SportsCar::mass` | `mass` in `Car` is `ISQBase::mass`; `SportsCar::mass` redefines `ISQBase::mass` |
| V10c | warning `… 'weight' from Vehicle` on both declarations | each type inherits `Vehicle::mass` twice (as `mass` and as `weight`), and owns a feature named `weight` (`Car`) or short-named `weight` (`CarShortName`) |
| V10d | **error** `Couldn't resolve reference to Element 'weight'` at each use, with the follow-on warning `Bound features should have conforming types`; nothing at the redefinitions | `c1` and `c2` inherit only `CarViaName::mass` and `CarViaAlias::mass`; the alias is no longer inherited |
| V10e | **error** `Couldn't resolve reference to Element 'mass'` in `r`, with the follow-on warning `Bound features should have conforming types`; nothing in `a` | `r` inherits only `CarRenamed::weight`; `a` inherits `Vehicle::mass` under both names |
| V10f | warning `… 'weight' from Metric, Scale` on `MetricFirst` and on `ScaleFirst` | `weight` is `Vehicle::mass` in `MetricFirst` and `Scale::weight` in `ScaleFirst` |
| V10g (fixed forms) | none | the re-declared alias names the redefining feature; `attribute weight :>> mass; alias mass for weight;` keeps both names; `attribute scaleWeight :>> Scale::weight;` resolves V10f |

### 2.11 Status on the current working branch

The pilot's working branch is `master`, at `850d39f` (2026-10-23, version **0.64.0** in development), 28 commits after
the 0.62.0 tag. Its changes to the files cited here (`KerMLValidator.xtend`, `SysMLValidator.xtend`,
`TypeAdapter.java`) concern owned multiplicities, metadata base types and connection/flow ends; the
distinguishability check, the scope provider, `isDistinguishableFrom` (including its `TODO: Add member element
metaclass check`) and implicit redefinition are unchanged. Of the branches pushed since 2026-09:

- **`ST6RI-904`** (head `aa83e7d`, 51 commits ahead of `master`) moves validation out of the Xtend validators into a
  new `org.omg.kerml.validation` module. The check becomes `NamespaceValidationChecker.validateNamespaceDistinguishability`,
  with messages in `KerMLValidationMessages.properties` (`validateNamespaceDistinguishablity_1` … `_4`). It keeps the
  same logic: every collision is still a **warning**, imported memberships are still not checked, and the metaclass
  TODO is still open. **Proposals for the pilot (§5.4) should be made against this new structure.**
- **`ST6RI-969`** extracts implicit specializations into a rule-based service. Its new tests assert that a parameter
  "must redefine the parameter at the same position of each direct general … inferred purely from position", so the
  V7 behavior is intended and kept.
- No other recently pushed branch touches the validators, the scope provider, or the redefinition logic.

### 2.12 The hazards in the official models

Running a validator that implements §6.1 over the 252 SysML files of the release (`sysml/src`, tag `2026-08`)
reports three cases, each confirmed by running the pilot:

| File | Line | Case | What the pilot computes (0.63.0) |
|---|---|---|---|
| `examples/State Space Representation Examples/CartSample.sysml` | 45 | V7b: `calc :>> getOutput { in :>> stateSpace : CartState; ... }` puts `stateSpace` at position 1 | `getOutput::stateSpace` redefines **both** `GetOutput::stateSpace` and `GetOutput::input`, silently equating the cart's input and state. No diagnostic. |
| `examples/.../SysML v2 Spec Annex A SimpleVehicleModel.sysml` | 1473 | V6b: `attribute :>> position3dVector = (0,0,0) [spatialCF];` in `vehicle_b_1`, whose generals have no `position3dVector` | It redefines the library quantity `ISQSpaceTime::position3dVector`. No diagnostic. |
| `examples/Simple Tests/PartTest.sysml` | 55 | V6a: `part p4 :> p4;` | `p4` subsets itself. No diagnostic. (A deliberate cycle test, kept for completeness.) |

The specification's own Annex A model and a published example therefore contain two of the silent cases.

### 2.13 Resolving a diamond: the obvious fix is still a collision

When two general types each redefine a feature of a common ancestor, the textbook fix is to redefine it once
more in the specialization:

```sysml
part def Vehicle { attribute range : Real; }
part def ElectricVehicle :> Vehicle { attribute :>> range default 400.0; }
part def CombustionVehicle :> Vehicle { attribute :>> range default 700.0; }
part def HybridVehicle :> ElectricVehicle, CombustionVehicle {
    attribute :>> range = 1000.0;   // intended: the hybrid's range
}
```

Run in pilot 0.63.0, `HybridVehicle::range` redefines only `ElectricVehicle::range` (the first general in
declaration order), `CombustionVehicle::range` stays inherited, and the pilot warns
`Duplicate of inherited member name 'range' from CombustionVehicle` **[RUN]**. The form the pilot accepts
names every branch: `attribute range :>> ElectricVehicle::range, CombustionVehicle::range = 1000.0;`
(redefining the ancestor's `Vehicle::range` also works, because each branch's redefinition then redefines a
feature the specialization redefines, and is removed from the inherited memberships). If the branches bind
their values with `=` instead of `default`, every resolution is additionally an error
(`Cannot override a binding feature value`) **[RUN]**. This is the practical face of KERML11-72; the
specification should state the rule for multiple inheritance explicitly (§5.1), and the training material
should show the qualified form.

### Summary of variants

| Variant | Spec status | Pilot diagnostic today | Evidence |
|---|---|---|---|
| V1 re-declare inherited (definition) | invalid (distinguishability) | warning "Duplicate of inherited member name" | SRC, TEST, RUN |
| V2 value via re-declaration (usage), incl. keyword-less | invalid | warning (same) | RUN |
| V3a duplicate owned, same metaclass | invalid | warning "Duplicate of other owned member name" | SRC, TEST, RUN |
| V3b duplicate owned, different metaclass | **valid** (metaclass escape) | warning; resolution type-filtered | SRC, TEST, RUN |
| V4a diamond, unrelated same names | invalid | namespace-level warning; first general wins | SRC, RUN |
| V4b diamond, one path renamed | valid, no rule | none (`noErrors`) | TEST, RUN |
| V5 typo | valid | none | SRC, RUN |
| V6a owned hides imported / self-reference | valid (hiding intended) | none; self-subsetting | SRC, RUN |
| V6b `:>>` target falls through to an import | valid as formalized (prose says otherwise) | none | SRC, TEST, RUN |
| V6d resolver disagreement | n/a (spec order unspecified) | editor and `resolveLocal` differ | SRC, RUN |
| V7a swapped parameters | valid (positional) | none | SRC, RUN |
| V7b explicit and positional parameter redefinition | text vs OCL conflict | double redefinition, silent | SRC, RUN |
| V8 correct form errors, wrong form warns | as specified | error vs. warning | SRC, RUN |
| V9 spec example | invalid | warning | RUN |
| V6f usage typed by itself, a library usage or a unit (`mass : mass`) | invalid | error, but the message names neither cause nor fix | RUN |
| V10a alias of the inherited feature | **valid** by the prose (the remedy when only a name is wanted); the OCL needs K2, K10 | none | SRC, RUN |
| V10b alias takes an inherited feature's name | invalid (distinguishability) | warning; the alias wins resolution | SRC, RUN |
| V10c feature takes an inherited alias's name | invalid (distinguishability) | warning | RUN |
| V10d redefinition drops the general's aliases | valid as formalized; intent unstated | none at the cause; linking error at each use | SRC, RUN |
| V10e named redefinition used where an alias was meant | valid (renaming) | none at the cause; linking error at each use of the old name | RUN |
| V10f alias and feature with one name from two generals | invalid | warning; first general wins | RUN |

---

## 3. Specification analysis

### 3.1 Relevant rules

The rules are paraphrased. The only direct quotation in this document is the KerML 7.3.2.1 phrase in V1.

| # | Source | Rule |
|---|---|---|
| R1 | KerML 7.2.5.2, p.23 | The names (including short names and aliases) provided for one member of a namespace must be disjoint from those of every other member. This is called "distinguishibility" [sic]. |
| R2 | KerML 7.2.5.4, p.23 | An imported membership whose name conflicts with an owned member, or with another import, is hidden. Namespaces that add memberships (e.g. inherited) keep all member names distinct. |
| R3 | KerML 7.3.2.1, p.26 | Inherited member names must differ from each other and from all owned member names. Inherited members hide same-named imports. Note: conflicts can be resolved by redefinition. |
| R4 | KerML 7.3.2.3, p.28 | A type inherits visible and protected (not private) memberships of its general types. |
| R5 | KerML 7.3.4.5, pp.36–37 | Redefinition replaces the redefined feature, so redefined features are not inherited and may reuse the name. Redefined features of owned redefinitions must be otherwise-inheritable features. Resolution of redefined-feature names starts in the supertypes. A redefining feature with no name takes the name of the first redefined feature. Features featured only by `Anything` may be redefined. |
| R6 | KerML 7.4.7.2, p.52; SysML 7.17.2, p.100; SysML 8.4.13.1, p.423 | Owned parameters redefine, in order, the parameters at the same position of general behaviors. If no redefinitions are given explicitly, implicit ones are supplied. |
| R7 | KerML 8.2.3.5.1, pp.82–83 | Resolution yields a Membership. If the element has the wrong metaclass for its context, the name has no resolution (error). Redefined-feature names are resolved with each general type as the local namespace. |
| R8 | KerML 8.2.3.5.3–.4, p.85 | Local resolution covers owned, imported and inherited memberships. Note: in a well-formed namespace at most one membership resolves a name. Global duplicates are resolved arbitrarily. |
| R9 | KerML 8.3.1, p.109 | A valid model satisfies all validation constraints. Conformant tools *should report* violations. Semantics are required only for valid models. No severity levels are defined. |
| R10 | KerML 8.3.2.4.3, p.127 | `isDistinguishableFrom`: true if names do not overlap **or if neither member element's metaclass conforms to the other's**. |
| R11 | KerML 8.3.2.4.5, pp.129–132 | `importedMemberships` description excludes colliding memberships (OCL does not, KERML11-75). `resolveLocal` takes `first()` of the matches. `validateNamespaceDistinguishibility` requires all memberships to be pairwise distinguishable. |
| R12 | KerML 8.3.3.1.10, pp.143–149 | `inheritedMembership` subsets `membership`. `inheritedMemberships()` = `removeRedefinedFeatures(inheritableMemberships())`. Removal rule 1 drops a membership redefined by *another inheritable* membership. Rule 2 drops one redefined by an owned feature. |
| R13 | KerML 8.3.3.3.4, pp.166–169 | `checkFeatureEndRedefinition`, `checkFeatureParameterRedefinition` and `checkFeatureResultRedefinition` are positional and name-independent. |
| R14 | KerML 8.3.4.10.2, p.228 | `validateFeatureValueOverriding`: features redefined by a valued feature must have only default values. |
| R15 | KerML 8.4.2, p.240; SysML 8.4.1, pp.397–398, Table 33, p.403 | Tools *may* insert implied relationships to satisfy semantic constraints. Lists the implied redefinitions (objective, state actions, rendering, assignment). |
| R16 | SysML 7.5.1, p.24; 7.6.1, p.32 | Inherited feature names must be distinct from each other and from owned features. Conflicts are resolved by redefining. A usage inherits from its definitions under the same rule. |
| R17 | SysML 7.6.3, pp.40–41; 7.6.5, pp.42–43 | Redefinitions are declared with `redefines`/`:>>`. Default `[1..1]` applies to attribute, item and port usages without explicit subsetting or redefinition. Effective names come from the first redefined feature. |
| R18 | SysML 7.13.2, p.71; 7.21.2, p.134; 7.27.2, pp.158–159 | Connection ends and subject parameters redefine by position. Metadata-body features redefine by name, and `:>>` may be omitted. |
| R19 | SysML BNF 8.2.2.6.2–8.2.2.6.5 (`SysML-textual-bnf.kebnf` L38, L314, L348-L349, L361, L488-L497); 8.2.2.27 (L1706-L1709) | `REDEFINES = ':>>' \| 'redefines'`. `UsageDeclaration = Identification FeatureSpecializationPart?`. A name alone declares a new usage. `DefaultReferenceUsage = RefPrefix Usage`. `MetadataBodyUsage` makes `:>>` optional. |
| R20 | KerML 7.2.5.2, p.22; 7.3.2.6, p.30 | An alias is a non-owning membership that gives an element another name in a namespace. A feature aliased in a type is related to it by a plain membership, not a feature membership, and is not an owned feature of the type. |
| R21 | SysML 8.3.6.4, p.270; 8.3.7.3, p.280 | `Usage::/definition : Classifier` and, for example, `AttributeUsage::/attributeDefinition : DataType`: a usage's types are classifiers (nominally definitions; kernel classifiers are also allowed), so a usage, which is a feature, cannot be one. |

### 3.2 Findings: where the specification is silent, ambiguous, inconsistent or allows the hazard

- **S1. The prohibition exists, but only generically.**
  - V1/V2 are forbidden by prose (R3, R16).
  - Formally they are forbidden only because `membership` includes `inheritedMembership` (R12), which makes `validateNamespaceDistinguishibility` (R11) apply.
  - No constraint states the actual rule that matters to modelers: *a same-named owned feature must redefine*. So tools emit generic "duplicate name" messages and have no defined place to attach a fix.
- **S2. No severity.**
  - R9 defines validity, but not whether a violation is an error.
  - The pilot reports this validation constraint as a *warning*, while most of its other validation constraints are errors. For example, 76 `error(` versus 2 `warning(` calls in `SysMLValidator.xtend` **[SRC]**.
  - Downstream analysis tools that act on "no errors" will process invalid models whose semantics are undefined.
- **S3. The metaclass escape contradicts other rules.**
  - The R10 escape makes same-name members of non-conforming metaclasses legal (V3b).
  - That contradicts the R8 note ("at most one membership"), and turns R7's "wrong metaclass ⇒ no resolution" into an order-dependent failure.
  - The reference implementation does not implement the escape (TODO comment; tests expect warnings). Spec and reference implementation disagree.
- **S4. Unspecified resolution order for ill-formed namespaces.**
  - `resolveLocal` uses `first()` over the ordered derived union `membership` (R11), whose union order is not specified.
  - The pilot's own order (imported, owned, inherited) contradicts the hiding rules R2/R3 (V6d).
- **S5. Import hiding exists in prose but not in OCL** ([KERML11-75](https://issues.omg.org/issues/KERML11-75), open).
  - Read literally, `private import ISQ::*;` plus an owned `mass` violates `validateNamespaceDistinguishibility`; read by the prose, it is valid.
  - The pilot follows the prose for validation ("Does not check distinguishibility for imported Memberships", [L425](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L419-L449)), but not in the derived `importedMembership`.
- **S6. Redefinition-target resolution is too broad, and the R5 restriction is not formalized.**
  - The R7 special rule restarts *full* resolution from each general type. Combined with R5's permission to redefine `Anything`-featured features, an unqualified `:>> mass` can bind to an imported library feature (V6b).
  - R5's "must be otherwise inheritable" has no OCL constraint.
  - [KERML11-140](https://issues.omg.org/issues/KERML11-140) pulls in the opposite direction (wider resolution for time slices). The RTF should resolve both together.
- **S7. Parameter redefinition: text and OCL disagree** (R6 vs R13).
  - Combined with the R15 permissive implied-relationship policy, the same model (V7b) has different meanings in different conformant tools.
  - Nothing detects name/position mismatches (V7a), although names are the only thing a reader sees.
  - The OCL already contains the needed precedent: explicit redefinitions on `InvocationExpression` parameters switch positional matching off (KerML p.168; cf. KERML_-58).
- **S8. Rule-1 removal hides a feature that is still present** (R12, V4b).
  - A feature redefined along one inheritance path disappears from the namespace even though instances still have it along another path. A same-named new feature then passes every rule.
  - Related: [KERML11-72](https://issues.omg.org/issues/KERML11-72) (open), where the same removal logic lets two redefinitions both be inherited.
- **S9. The standard libraries violate the rule.**
  - [KERML11-76](https://issues.omg.org/issues/KERML11-76) (Kernel libraries) and [SYSML21-322](https://issues.omg.org/issues/SYSML21-322) (19 SysML library models, from Actions, Items, Ports, Views to SI and USCustomaryUnits) are open.
  - This blocks raising the severity and trains users to ignore the warning.
- **S10. A spec example violates the rule** (V9, SysML 7.13.4, p.73).
- **S11. The concrete syntax makes the wrong form the shortest one.**
  - `attribute mass = 1500.0;` reads naturally; the correct form needs `:>>`.
  - The keyword-less `mass = 1500.0;` is a redefinition in a metadata body but a new reference usage everywhere else (R19).
  - UML 2.5, and hence SysML v1, allowed *implicit* redefinition by same name (see [UML25-315](https://issues.omg.org/issues/UML25-315)). Migrating users bring that expectation, and neither SysML 7.6.1 nor 7.6.3 warns about the difference.
- **S12. OCL errata in the relevant definitions** (some may already be covered by [KERML11-198](https://issues.omg.org/issues/KERML11-198)):
  - `isDistinguishableFrom` (p.127) uses non-existent `shortMemberName` (the property is `memberShortName`) and `oclKindOf` (should be `oclIsKindOf`), and has an unbalanced closing parenthesis.
  - 8.2.3.5.4 (p.85) also says `shortMemberName`.
  - `resolveLocal` (p.130) types the collection `memberships` as `Membership`.
  - The prose of 7.2.5.2 (p.23) allows several names to identify the same element, but the `isDistinguishableFrom`
    body has no same-element case. Read literally, an alias that
    repeats a member's name for the same element (V10a `CarSameName`), or two inherited aliases of one feature (V10f
    `BothAliases`), violate `validateNamespaceDistinguishibility`. The pilot follows the prose (`m.memberElement !=
    memElement`). K2 adds the case.
  - The constraint name is spelled `validateNamespaceDistinguishibility` (p.132; prose "distinguishibility", p.23). The pilot's issue code is `validateNamespaceDistinguishablity` ([L131](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L131)), a third spelling. That hampers matching diagnostics to constraints in conformance tests.
- **S13. Interaction with value overriding.** R14 makes the correct form an error for bound values, while the hiding form escapes R14 entirely (V8, [SYSML21-646](https://issues.omg.org/issues/SYSML21-646)).
- **S14. Aliases are names too, but the feature-oriented rules forget them** (R1, R20; V10b, V10c).
  - Distinguishability (R1, R11) covers alias memberships, so V10b and V10c are invalid. But a rule phrased in terms
    of feature memberships, as K1 was first drafted, misses both, because an inherited alias is a plain `Membership`.
  - Nothing states which member wins when an owned alias and an inherited feature share a name. In the pilot the
    alias wins, so one alias declaration silently redirects every use of the name in the type and its
    specializations, including `:>>` targets.
- **S15. Redefinition drops inherited aliases without saying so** (R12, V10d).
  - `removeRedefinedFeatures` removes inheritable memberships by member element, so an alias that a general declares
    for a feature disappears from every specialization that redefines that feature. This happens even when the
    redefinition is written through the alias (`:>> weight`).
  - Neither 7.2.5.2 nor 7.3.4.5 mentions this. The only symptom is a linking error wherever the alias name is used,
    which points away from the cause.

- **S16. Rule 1 of `removeRedefinedFeatures` removes every membership that shares an element with another** (R12).
  - Rule 1 removes a membership whose element is among the redefined features of *another* membership, and the
    redefined features of a membership include its own element (`allRedefinedFeatures()` prepends `self`, KerML
    8.3.3.3.4, p.162; `removeRedefinedFeatures`, 8.3.3.1.10, p.147).
  - Two memberships of the same element therefore remove each other. As formalized, `alias weight for mass;` in
    `CarNewName` makes **both** `weight` and `mass` disappear from `myCar : CarNewName` (V10a). The same happens to an
    alias declared again after a redefinition (V10d), and to the two aliases of V10f `BothAliases`.
  - The pilot does not do this. It leaves each membership's own element out of the redefined features it collects
    ([`TypeAdapter` L162-L168](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/TypeAdapter.java#L162-L168))
    **[SRC]**, which is why every name in V10a resolves **[RUN]**. K10 aligns the OCL with it.

**Can the name be aliased instead of redefined?** Yes, when the intent is only to give the inherited feature a name.
An alias creates no feature, so it does not collide (V10a). That is what the prose says and what the reference
implementation does; the formal rules need K2 and K10 to agree (S12, S16). It is not a substitute for redefinition,
though:

- it cannot change the feature's value, type or multiplicity;
- an alias with an inherited member's name must stand for that member's element, or it redirects the name (V10b);
- under the current rules it does not survive a redefinition of the feature in a specialization (V10d).

K1, K2, K9 and K10 below make these rules explicit.

---

## 4. Impact

1. **Silent model errors.**
   - The model "contains" the value, the diagram shows it, and a reviewer sees `mass = 1500.0`, but it is attached to a different feature.
   - In SysML graphical notation an inherited usage is shown with a `^` prefix (SysML Table 4, p.35), so a compartment would show both `mass` and `^mass`. Text views show only the owned one.
2. **Analysis and verification on the wrong feature.**
   - Constraints, calculations, mass roll-ups (`sum(parts.mass)`) and requirement checks written in `Vehicle` or library contexts read `Vehicle::mass`, which is unbound for `myCar`.
   - Results are vacuous or computed from defaults, and satisfaction can be falsely asserted or falsely refuted.
   - With V7a, a behavior's inputs are swapped without any visible change in names.
   - With V6b, a library quantity (`ISQ::mass`) is constrained instead of the design feature.
3. **Tool interchange differences.** For the same text, conforming tools can legitimately differ on:
   - severity (S2);
   - whether V3b is valid (S3, where the spec and the reference implementation already differ);
   - which member a name resolves to (S4, V4a, V6d);
   - whether an import collision is an error (S5);
   - whether a parameter redefines one or two features (S7, V7b).

   Interchange via the textual notation or the API can therefore change the meaning of a model, not just its presentation.
4. **Consumers that key by name** collide or pick arbitrarily. Examples: exports to spreadsheets, parameter tables, FMI/Modelica bindings, or API queries by `declaredName`.
5. **Warning fatigue.** Because the library models already trigger the same diagnostic (S9) and the pilot reports it as a warning, users learn to ignore exactly the message that flags this defect.
6. **Teaching and migration.** SysML v1 habits (implicit redefinition by name) plus a spec example that contains the defect (V9) make the pitfall likely for new adopters.

---

## 5. Proposed resolution

### 5.1 KerML specification changes (KerML 1.1 RTF)

**K1. New validation constraints on `Type` (KerML 8.3.3.1.10), severity error.**

The inherited name may come from a feature membership or from an alias membership (R20), so the constraint is
stated on member elements, not on `FeatureMembership`; a first draft that selected `FeatureMembership` missed
V10b and V10c (S14).

```
validateTypeOwnedFeatureRedefinesHomonym
An ownedFeature of a Type that has the same memberName or memberShortName as an
inheritedMembership of the Type whose memberElement is a Feature, whether that
Membership is a FeatureMembership or an alias Membership, must directly or
indirectly redefine that Feature. (A same-named owned Feature that does not
redefine it declares a second, distinct Feature and is not allowed; to declare a
distinct Feature, redefine the inherited Feature to give it a different name, see
7.3.4.5.)

ownedFeatureMembership->forAll(own |
    inheritedMembership->select(memberElement.oclIsKindOf(Feature))->forAll(inh |
        inh.memberElement <> own.ownedMemberFeature and own.namesOverlap(inh) implies
            own.ownedMemberFeature.allRedefinedFeatures()->
                includes(inh.memberElement.oclAsType(Feature))))

validateTypeAliasNamesInheritedElement
An alias Membership of a Type (an ownedMembership that is not an OwningMembership)
that has the same memberName or memberShortName as an inheritedMembership of the
Type must have the same memberElement. (An alias may repeat the name of an
inherited member for the same element, or give it another name, but must not take
the name of an inherited member for a different element.)

ownedMembership->reject(oclIsKindOf(OwningMembership))->forAll(alias |
    inheritedMembership->forAll(inh |
        alias.namesOverlap(inh) implies alias.memberElement = inh.memberElement))
```

- **Helper.** Add `Membership::namesOverlap(other : Membership) : Boolean`, true if a non-null `memberShortName` or `memberName` of either equals a `memberShortName` or `memberName` of the other. `Feature::allRedefinedFeatures()` already exists (KerML 8.3.3.3.4, p.162).
- **Aliasing is the sanctioned alternative when only a name is wanted.**
  - Neither constraint forbids an alias of the inherited feature, under its own name (`alias mass for Vehicle::mass;`) or another (`alias weight for mass;`).
  - Such an alias creates no feature, so it is not a second `mass`, and the pilot accepts it (V10a) **[RUN]**.
  - It cannot give the feature a value, type or multiplicity, so K1's "must redefine" stays the rule whenever the modeler changes the feature.
- **Why separate constraints, when `validateNamespaceDistinguishibility` already implies them?**
  - They name the modeler's mistake.
  - They give tools a precise hook for the message and the quick fix (`:>>` for a feature, rename for an alias).
  - They work regardless of the metaclass escape (K2).
  - They are written, like all validation constraints, presuming that implied redefinitions have been added (KerML 8.3.1, p.109). So positional parameter and end redefinitions are automatically exempt.
- **Prose.** Add matching notes to KerML 7.3.2.1 and 7.3.4.5 with a two-line example: wrong `feature mass;`, right `feature redefines mass;`. Mention that `alias weight for mass;` adds a name without adding a feature.

**K2. Remove the metaclass escape from `Membership::isDistinguishableFrom` (KerML 8.3.2.4.3, p.127)** and fix its OCL (S12).

```
isDistinguishableFrom(other : Membership) : Boolean
body: memberElement = other.memberElement or not namesOverlap(other)
```

- **Rationale.**
  - Adds the same-element case that the 7.2.5.2 prose has and the OCL lacks (S12), so an alias of a member's own
    element (V10a) is valid as formalized.
  - Makes the KerML 8.2.3.5.3 note true.
  - Makes local resolution unambiguous before metaclass checking (8.2.3.5.1).
  - Matches the reference implementation's behavior and tests.
- **Fallback.** If the RTF wants to keep a metaclass escape for some construct, restrict it to pairs where *neither* member element is a `Feature`, and state the resolution precedence explicitly in 8.2.3.5.

**K3. Severity (KerML 8.3.1, p.109; mirror in SysML 8.3.1).**

- State that a conformant tool shall report a violated validation constraint as an **error**, meaning the model is not valid.
- Add a non-normative annex of **advisory checks**, which tools should report as warnings:
  - **A1 near-miss names** (§6.2);
  - **A2 same-named owned feature vs. an inheritable feature hidden by another path's redefinition** (V4b). The OCL is K1 with `inheritableMemberships(Set{}, Set{}, false)` in place of `inheritedMembership`;
  - **A3 parameter or end paired by position with a differently named feature while a same-named one exists at another position** (V7a);
  - **A4 a redefinition target not inheritable from the owning type, resolved through an enclosing namespace or import** (V6b);
  - **A5 an owned member hides an imported member of the same name** (V6a), informational;
  - **A6 a redefinition drops an alias that a general declares for the redefined feature** (V10d), until K9 is decided.

**K4. Imports and resolution order (KerML 8.3.2.4.5).**

- Fix the `importedMemberships` OCL to exclude collisions, as the prose says (KERML11-75).
- Specify that `resolveLocal` considers owned, then inherited, then imported memberships, and only then outer namespaces. This makes the operation agree with R2/R3 even for ill-formed namespaces.
- Fix the `memberships : Membership` typing.

**K5. Formalize KerML 7.3.4.5 (p.37) and narrow fall-through for redefined features.**

Add to `Redefinition` (8.3.3.3.8):

```
validateRedefinitionRedefinedFeatureInheritable
If the redefiningFeature has an owningType and this Redefinition is an
ownedRedefinition of it, then the redefinedFeature must either have no
featuringTypes (be implicitly featured by Anything) or be the memberFeature, or be
redefined by the memberFeature, of a FeatureMembership in
redefiningFeature.owningType.inheritableMemberships(Set{}, Set{}, false).
```

- This keeps the existing allowance for redefining package-level features (KerML 7.3.4.5, p.36, and the pilot's `Redefinition_OwningType` test).
- Make A4 a warning for the remaining hazardous case: an *unqualified* redefined-feature name that resolves, via an enclosing namespace or import, to a package-level feature.
- The abstract syntax does not record whether a name was qualified, so A4 is a concrete-syntax check (8.2.3.5.1).
- Coordinate with KERML11-140. Its time-slice case resolves to a feature of a *featuring* type, which this rule can add as a second allowed source.

**K6. Parameter redefinition (KerML 7.4.7.2, p.52, and `checkFeatureParameterRedefinition`, p.168).**

- Align the OCL with the prose: the positional requirement applies only to parameters with no non-implied owned redefinition. This generalizes the `InvocationExpression` exemption that already exists.
- Add `validateTypeParameterRedefinitionUnique`, severity error:
  - no parameter of a general behavior is redefined by more than one owned parameter;
  - no owned parameter redefines two parameters of the same general behavior;
  - with more than one general behavior, every general parameter is redefined (already required by the prose).
- This makes V7b either well-defined (by name) or an error, instead of tool-dependent.

**K7. Library and anomaly prerequisites.** Resolve KERML11-72 and KERML11-76 (and SysML SYSML21-311 and SYSML21-322) before or together with K1/K3, so that conformant tools can report K1 as an error without flagging the standard libraries.

**K8. Errata.** Correct the S12 items. Spell the constraint `validateNamespaceDistinguishability`, and keep the old name as an alias for one revision.

**K9. Inherited aliases of a redefined feature (KerML 7.2.5.2, 7.3.4.5 and `Type::removeRedefinedFeatures`, 8.3.3.1.10).**

Today a general's alias of a feature disappears from every specialization that redefines the feature (S15, V10d).
The RTF should decide this explicitly. Two options:

- **(a) Recommended: the alias follows the redefinition.**
  - When `removeRedefinedFeatures` removes an alias membership because its member element is redefined in the type,
    the alias name denotes the redefining feature in that type and its specializations.
  - Specify it in name resolution (8.2.3.5.3). An implied alias membership for the redefining feature, like the
    implied relationships of KerML 8.4.2, would also do, but only together with K10: under the current rule 1 it
    would share its element with the redefining feature's own membership, and the two would remove each other in
    every specialization (S16).
  - If two branches of a diamond redefine the feature, the alias denotes two features, and the multiple-inheritance
    rule applies to it as to the feature (KERML11-72).
  - This keeps the meaning a modeler expects from declaring the alias in the general. It is what the work-around,
    declaring the alias again, does by hand.
- **(b) Minimum: document the current behavior.**
  - Add a note to 7.2.5.2 and 7.3.4.5: redefining a feature removes the aliases inherited for it; declare the alias
    again in the redefining type if it is needed.
  - Add advisory check A6.

Either way, the name should not silently stop resolving in a model with no diagnostic at the redefinition.

**K10. Rule 1 of `Type::removeRedefinedFeatures` (KerML 8.3.3.1.10, p.147): ignore memberships of the same element.**

```
body: let reducedMemberships : Sequence(Membership) =
    memberships->reject(mem1 |
        memberships->excluding(mem1)->
            exists(mem2 | mem2.memberElement <> mem1.memberElement and
                allRedefinedFeaturesOf(mem2)->includes(mem1.memberElement))) in
...   -- rule 2 unchanged
```

- A membership is removed by rule 1 only when a membership of a *different* feature redefines its element. Two
  names for one feature, an owning membership and an alias, then survive together (S16).
- This is what the reference implementation does (TypeAdapter L162-L168) **[SRC]**.

### 5.2 SysML specification changes (SysML 2.1 RTF)

- **S-a. Fix the 7.13.4 example (p.73):** `item def TestWithCutoff :> TestRecord { attribute redefines cutoff : Rational default = 0.75 * averageScore; }`. Also make `TestRecord::cutoff` `Rational` (p.72).
- **S-b. Add a Note with an example to 7.6.1 (Specialization, p.32) and 7.6.3 (p.40).**
  - A usage declared with the same name as an inherited usage does not redefine it; unlike UML/SysML v1, redefinition is never implied by name.
  - Show `part myCar : Car { attribute :>> mass = 1500.0; }` against the invalid `attribute mass = 1500.0;`.
- **S-c. Add a Note to 7.17.2 (p.100), 7.13.2 (p.71) and 7.21.2 (p.134).**
  - Positional pairing ignores names.
  - Recommend explicit `:>>` whenever an owned parameter or end has a different name than, or a different order from, the general one.
  - Refer to K6.
- **S-d.** Resolve SYSML21-311 and SYSML21-322 (library name collisions).

### 5.3 Alternatives considered

| Option | Description | Assessment |
|---|---|---|
| **A (recommended)** | Keep explicit redefinition. K1 is an error with a `:>>` quick fix, plus advisory checks. | No change for valid models; it only makes existing invalidity explicit. Typos remain advisory (A1). |
| B | Implicit redefinition by name, as in UML and SysML v2 metadata bodies: a same-named owned feature without explicit specialization redefines the inheritable feature. | Changes only models that are invalid today, so it is backward compatible for valid models, and matches v1 intuition. **But:** in diamonds (V4a) it would silently *merge* unrelated features; it conflicts with positional parameter pairing (two candidate targets); it hides intent in review; and typos stay undetected. Not recommended. A future RTF could revisit a narrow form (single candidate, non-parameter, non-end). |
| C | Explicit "hiding" marker (e.g. `new` or `hides`) to allow a distinct same-named feature. | Contradicts R1/R3 (names must stay unique, otherwise resolution is ambiguous). Not needed, because KerML already supports the intent by renaming the inherited feature, e.g. `attribute vehicleMass :>> Vehicle::mass; attribute mass : Real;`. Not recommended. The `^` notation proposed in [KERML11-201](https://issues.omg.org/issues/KERML11-201) for *referring to* inherited members is complementary and harmless. |
| D | Status quo (warning). | Leaves S2–S7 tool-dependent and keeps the incentive problem (V8). |

### 5.4 Reference (pilot) implementation changes

On the current working branch `ST6RI-904` the distinguishability check lives in `org.omg.kerml.validation` (`NamespaceValidationChecker`, messages in `KerMLValidationMessages.properties`). The changes below name the `KerMLValidator.xtend` locations of the released 0.62.0/0.63.0 code; they apply unchanged to the new checker (§2.11).

Paths are relative to the repository at commit `692170b`.

- **P1. Error for owned-feature vs. inherited-feature collisions.**
  - In `KerMLValidator.checkNamespace` ([L440-L447](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L440-L447)), split out the case where the owned member is a `FeatureMembership` and the colliding inherited membership is a `FeatureMembership`.
  - Report it as `error(…)` with a new issue code `validateTypeOwnedFeatureRedefinesHomonym`. Until KERML11-72 and the library collisions are fixed, make the severity configurable through an Xtext `ConfigurableIssueCodesProvider`, defaulting to error for user models and warning for library resources.
  - **Message:** `Feature 'mass' does not redefine inherited feature 'Vehicle::mass' and has the same name, so it declares a second, distinct feature. Use ':>> mass' to redefine it.`
  - Pass the redefinable feature's qualified name, plus the offset and length of the declared name, as issue data.
- **P2. Keep the other duplicate cases as they are, but make the messages actionable.** These are inherited vs. inherited (diamond) and non-feature members.
  - Diamond: `Inherited features 'Powered::rating' and 'Insured::rating' have the same name in 'Truck'; redefine at least one to give it a distinct name.`
  - Raise to error when KERML11-72 is resolved.
- **P3. Quick fixes** in `SysMLQuickfixProvider` and `KerMLQuickfixProvider`:
  - `@Fix(validateTypeOwnedFeatureRedefinesHomonym)` **"Redefine inherited 'Vehicle::mass'"**: replace the declared-name token `mass` with `:>> mass`, keeping type, multiplicity and value (`attribute :>> mass : Real;`). For KerML, use `feature :>> mass`.
  - **"Rename to 'mass1'"**: invokes the rename refactoring.
  - Diamond: **"Redefine and rename each inherited 'rating'"**: inserts `attribute rating_Powered :>> Powered::rating;` and similar.
  - For unresolved *redefinition targets* only, restore "Change to …" proposals limited to the inherited feature names of the owning type. This is cheap, unlike the global `queryScope` that is currently disabled.
- **P4. Near-miss hint (A1).** Warning code `possibleMisspelledRedefinition` in `KerMLValidator.checkFeature`:
  - `Feature 'masss' is new; did you mean to redefine inherited 'mass' (':>> mass')?`
  - It comes with the P3 quick fix. The algorithm is in §6.2.
- **P5. Positional checks (A3, K6).** After the direction check at [L616-L619](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L616-L619), for each implicit (positional) redefined feature `r` of a parameter or end `f`:
  - **Warning** if `r`'s effective name differs from `f`'s and a general parameter or end named `f` exists at another position: `Parameter 'target' redefines 'Transfer::source' by position; 'Transfer::target' is at position 2. Reorder, or redefine explicitly.`
  - **Error** (K6) if `f` also has an explicit redefinition of a different feature of the same general type: `Parameter 'target' redefines both 'Transfer::target' (explicitly) and 'Transfer::source' (by position).`
  - Better still, implement K6 in `FeatureAdapter.isComputeRedefinitions` ([L520-L526](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/FeatureAdapter.java#L520-L526)) by skipping positional redefinition when explicit owned redefinitions exist, as is already done for invocation expressions.
- **P6. Redefinition-target fall-through (A4, K5).**
  - In `checkRedefinition` ([L704ff.](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L704-L725)), warn when the redefining feature has an owning type, the redefined feature is not among that type's inheritable features (closure over redefinitions), and the reference text is a single unqualified segment. Use `NodeModelUtils.findNodesForFeature(redef, redefinition_RedefinedFeature)` to check whether the reference was qualified.
  - **Message:** `'mass' resolves to 'ISQ::mass', which is not inherited by 'Car'; qualify the name if this is intended.`
- **P7. One resolution order.**
  - Make `Namespace::resolveLocal` (`NamespaceUtil.getNamedMembershipFor`) use owned, then inherited, then imported, like `KerMLScope`.
  - Filter colliding imports in `NamespaceAdapter.getImportedMembership` ([L104-L113](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/NamespaceAdapter.java#L104-L113)), consistent with KERML11-75.
- **P8. Metaclass.** Keep the stricter name-only check. Remove the TODO in `Membership_isDistinguishableFrom_InvocationDelegate` once K2 is adopted. If K2 is not adopted, the pilot is non-conformant and should document it.
- **P9.** Correct the issue-code string `validateNamespaceDistinguishablity` ([L131](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L131)).
- **P10. Tests.**
  - Add Xpect tests for V1–V9: one `invalid/` file per variant asserting the new message, plus `valid/` counterparts using `:>>`.
  - Change `RedefinitionDiamond0.sysml.xt` to expect the A2 warning.
  - Fix the library collisions so that the library resources validate clean.
- **P11. Aliases (K1, K9).**
  - In `checkNamespace`, the check of owned memberships against inherited ones ([L443-L445](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L441-L447)) already covers alias memberships on both sides.
  - Report the two K1 cases as errors with specific messages.
  - For an owned feature with the name of an inherited alias: `Feature 'weight' does not redefine 'Vehicle::mass', which is inherited under the alias name 'weight', so it declares a second, distinct feature. Use ':>> mass' to redefine it.`
  - For an owned alias with the name of an inherited member: `Alias 'mass' stands for 'ISQ::mass', but 'Vehicle::mass' is inherited under the same name; owned members are found first, so 'mass' now means 'ISQ::mass' here and in specializations.` The quick fix is a rename.
  - Until K9 is decided, give the linking error for a dropped alias its cause: `'weight' is an alias of 'Vehicle::mass', which 'myCar' redefines; a redefinition drops the inherited aliases of the feature it redefines. Declare 'alias weight for mass;' in 'myCar'.` Add the A6 warning at the redefinition.
- **P12. Usage typed by a usage (V6f).** When the type of a usage is itself a usage, say so in the `validate…UsageType` errors.
  - If the name resolved to the feature being declared, say that too.
  - Offer the quick fixes `: <type of the usage>` and `:> <usage>`.
  - For a unit, offer `: <quantity value type>`, for example `: MassValue` for `kg`.

### 5.5 Edge cases the rule must exempt or treat specially

| Case | Treatment | Why |
|---|---|---|
| Implicit redefinitions: parameters by position, results/`return`, `subject`, actor and stakeholder parameters, objective, connection/interface/allocation/flow ends, state `entry`/`do`/`exit`, transition and accept payloads, assignment `startingAt`/`accessedFeature`, view rendering, constructor-result features | Exempt from K1 automatically. K1 is evaluated after implied redefinitions are added, and redefined features are not inherited. A3 still applies to positional pairs. | KerML 8.3.1 (p.109), R12, R13; SysML Table 33 (p.403) |
| Invocation arguments by name (`F(q = 1, p = a)`) | Exempt; already name-based | `checkFeatureParameterRedefinition` exemption (p.168); KERML_-58 |
| Metadata body features | Exempt; they redefine by name | SysML 7.27.2 (pp.158–159); KerML 7.4.13 (p.69) |
| Anonymous features (no declared and no effective name) | Exempt (no name). Note KERML11-47: they cannot be specialized textually. | – |
| Features whose name is an **effective** name (redefinition without a name; perform, exhibit and include usages referencing another action) | Compare effective names. The message should say the name was derived and from where. | KerML 7.3.4.5 (p.37); SysML 7.6.5 (pp.42–43), 7.17.6 (p.104); SYSML21-7 |
| Short names (`<m>`) | Included: short and regular names are compared crosswise | R1, R10; the pilot already does this |
| Alias memberships | Included on both sides. An owned feature must redefine a feature whose inherited alias shares its name (K1, first constraint). An owned alias must stand for the element of any inherited member that shares its name (K1, second constraint). An alias of the *same* element, under its own name or another, is not a collision. | R1, R20; the pilot filters `memberElement !=`; V10a–V10c, V10f |
| Inherited aliases of a redefined feature | Dropped today; see K9 (and A6 until it is decided) | R12; V10d |
| Usage typed by a usage (`: mass`, `: ISQ::mass`, `: kg`) | Not a name collision, but its `: mass` form is the V6a self-reference; report it with its cause | R21; V6f |
| Same element inherited along several paths | Not a collision | Pilot `dups.map[memberElement].toSet.size > 1` (L506) |
| Private members of general types | Exempt (not inherited). Exclude from A1 hints. | KerML 7.3.2.3 (p.28) |
| Protected members | Included (inherited) | KerML 7.3.2.3 |
| Imported names | Not a collision (hidden by owned and inherited). A5 is informational only. | R2, R3 |
| Non-feature members (nested definitions, packages) | K1 does not apply (it cannot be fixed by `:>>`); distinguishability (K2) applies; quick fix is rename only | R1 |
| Enumerated values (`enum`) of an enumeration definition that specializes an attribute definition with nested usages | Distinguishability applies; quick fix is rename only (enumerated values are not meant to redefine) | SysML 7.8.1 (p.47) |
| Variant usages in a variation | Distinguishability applies; quick fix is rename only | SysML 7.6.7 (p.43) |
| Library-originated collisions, including `self`/`that`/`this` | Report on the library, or suppress for library resources, until KERML11-76, SYSML21-311 and SYSML21-322 are fixed | S9 |
| Conjugated types and ports | Same rules on the conjugated type's inherited memberships | KerML 7.3.2.4 (p.28); SYSML21-658 |
| Automatically constructed expressions and binding connectors | Exempt (as in the pilot, L422-L424) | Performance; no user names |
| Global-namespace duplicates | Out of scope; see KERML_-43 and the Sensmetry note | KerML 8.2.3.5.4 (p.85) |

---

## 6. Detecting the pitfall today, and fixing each variant

### 6.1 Diagnostics a validator should emit

The SysML v2 validator in this repository implements these checks.

- **Warnings:** `INHERITED_NAME_REDECLARED`, `DUPLICATE_MEMBER_NAME`, `RENAMED_INHERITED_NAME_REUSED`,
  `NAME_SIMILAR_TO_INHERITED`, `REDEFINITION_NOT_INHERITED`, `FEATURE_REFERS_TO_ITSELF`,
  `PARAMETER_POSITION_MISMATCH`, `PARAMETER_REDEFINES_TWO`, `ALIAS_NAME_COLLISION` and `INHERITED_ALIAS_DROPPED`.
- **Errors:** `SEMANTIC_BOUND_VALUE_OVERRIDDEN` and `SEMANTIC_TYPED_BY_USAGE`.

See `docs/USER_GUIDE.md`. Every hit it reports on the release's 252 SysML files is listed in §2.12, and was
confirmed with the pilot. The alias and typing checks add no hits there.

| ID | Trigger | Severity | Message (template) | Quick fix |
|---|---|---|---|---|
| D1 (K1) | owned feature's name or short name equals an inherited feature's, and it does not redefine it | **error** | `Feature '{n}' does not redefine inherited feature '{Q}' and has the same name, so it declares a second, distinct feature. Use ':>> {n}' to redefine it.` | insert `:>>`; rename |
| D2 (K2) | two owned members with overlapping names (any metaclass) | **error** | `Duplicate of other owned member name '{n}'` | rename; delete |
| D3 | two inherited, unrelated features with the same name | error (warning until KERML11-72) | `Inherited features '{Q1}' and '{Q2}' have the same name in '{T}'; redefine at least one to give it a distinct name.` | generate renaming redefinitions |
| D4 (A2) | owned feature named like an inheritable feature hidden by another path's redefinition | warning | `'{n}' is a new feature; '{Q}' (inherited via '{T2}') is redefined as '{m}' along '{T1}'. Did you mean ':>> {Q}'?` | insert `:>> {Q}` |
| D5 (A1) | near-miss name (see 6.2) | warning | `'{n}' is a new feature; did you mean to redefine inherited '{m}' (':>> {m}')?` | replace with `:>> {m}` |
| D6 (A3) | positional pairing with a differently named feature while a same-named one exists elsewhere | warning | `Parameter '{n}' redefines '{Q}' by position; '{Q2}' is at position {k}.` | reorder; add explicit `:>>` |
| D7 (K6) | explicit and positional redefinition of different features of one general | **error** | `Parameter '{n}' redefines both '{Q1}' (explicitly) and '{Q2}' (by position).` | remove one |
| D8 (A4) | unqualified redefinition target resolves outside the inheritable features | warning | `'{n}' resolves to '{Q}', which is not inherited by '{T}'; qualify the name if this is intended.` | qualify; choose an inherited feature |
| D9 (A5) | owned member hides an imported one | info | `'{n}' hides imported '{Q}'.` | qualify references |
| D10 | a feature's subsetting or redefinition target resolves to the feature itself | warning | `'{n}' specializes itself; did you mean '{Q}'?` | qualify |
| D11 (K1) | owned feature's name or short name equals an inherited **alias** of a feature, and it does not redefine that feature | **error** | `Feature '{n}' does not redefine '{Q}', which is inherited under the alias name '{n}', so it declares a second, distinct feature. Use ':>> {m}' to redefine it.` | insert `:>> {m}` and re-declare the alias; rename |
| D12 (K1) | owned alias's name equals an inherited member's name, for a different element | **error** | `Alias '{n}' stands for '{A}', but '{Q}' is inherited under the same name; '{n}' now means '{A}' here and in specializations.` | rename the alias |
| D13 (A6, K9) | a redefinition drops an inherited alias of the redefined feature | warning | `Redefining '{Q}' drops the alias '{n}' declared in '{T}'; '{n}' no longer resolves in '{U}'.` | insert `alias {n} for {m};` |
| D14 (D3) | an inherited alias and a different inherited member with one name, from two generals | error (warning until KERML11-72) | `'{T}' inherits the alias '{n}' for '{Q1}' and '{Q2}' under the same name; '{n}' resolves to the one from the general listed first.` | rename; redefine under a distinct name |
| D15 (R21) | a usage typed by a usage: itself (`: mass`), a library quantity (`: ISQ::mass`) or a unit (`: kg`) | **error** | `'{n}' is typed by itself / by '{Q}', which is a usage, not a definition.` | `: {type of Q}` or `:> {Q}`; for a unit, its quantity type |

### 6.2 Near-miss ("did you mean `:>> mass`") algorithm

Run it for each owned, non-parameter, non-end feature `f` that has a declared name, no explicit owned subsetting or redefinition, and an owning type with at least one non-implicit general type.

1. **Candidates** are the inherited feature memberships `m` such that:
   - the member feature is not private and is not already redefined by any owned feature of the owning type;
   - `m.memberName ≠ f.name`;
   - and any one of these holds:
     - case-insensitive equality;
     - equality after removing `_`, `-` and spaces;
     - Damerau–Levenshtein distance ≤ 1 for names of 4 characters or fewer, or ≤ 2 for longer names.
2. **Raise the warning** only if one of these also holds:
   - `f` has a feature value or no explicit typing;
   - `f`'s type conforms to `m`'s type.

   This keeps false positives low for genuinely new features such as `massFlow`.
3. **Report the single best candidate**: smallest distance, then nearest general type.
4. **Suppress** when the user marks intent with an explicit `:>`, `:` to a different type, or a documented suppression metadata annotation.
5. **Unresolved `:>>` targets.** Apply the same ranking to unresolved `:>> {n}` targets, so the linking error comes with a "Change to `mass`" fix (P3).
6. **Performance.** Inherited memberships are already cached per type (`TypeAdapter.getInheritedMembership`, [L189-L192](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/TypeAdapter.java#L189-L192)). Candidate sets are small.

### 6.3 What practitioners can do with the current pilot (0.62.0)

- **Treat these warnings as errors** in review and CI:
  - `Duplicate of inherited member name …`
  - `Duplicate of other owned member name …`
  - `Duplicate of other alias name …`
  - `Duplicate of owned member name …`

  They are the only signals for V1–V4a and V9.
- **Know what is not caught**, and check these by review or a custom lint:
  - V4b (renamed diamond);
  - V5 (typos);
  - V6a/V6b (self-reference, fall-through to imports);
  - V7a/V7b (positional pairing).
- **House rules that make mistakes loud:**
  - assign values to inherited features only with `:>>`, never with a bare name;
  - qualify redefinition targets of important features (`attribute :>> Vehicle::mass = 1500.0;`). Qualified targets are resolved through `Vehicle`'s visible memberships, so a typo or a missing feature is an error rather than a fall-through (KerML 8.2.3.5.1, step 3);
  - qualify library subsetting targets (`attribute mass :> ISQ::mass;`), as the release examples do;
  - in specialized actions and calculations, keep the general parameter order, and use explicit `:>>` whenever a parameter's name differs from the general one;
  - prefer `default` values in general definitions that are meant to be specialized (V8);
  - to give an inherited feature another name, use an alias (`alias weight for mass;`), not a named redefinition (`attribute weight :>> mass;` renames, and `mass` stops resolving; V10e);
  - never give an alias the name of an inherited member (V10b);
  - after redefining a feature that a general aliases, declare the alias again (V10d);
  - type a quantity by its value definition (`attribute mass : MassValue;`) or subset the library quantity (`attribute mass :> ISQ::mass;`), never `: mass`, `: ISQ::mass` or `: kg` (V6f).
- **Also not caught, or caught far from the cause:** V10d (a dropped alias fails only where its name is used) and V10e (a renamed feature's old name likewise).

### 6.4 How a modeler fixes each variant

| Variant | Wrong | Right |
|---|---|---|
| V1 | `part def Car :> Vehicle { attribute mass : Real; }` | `attribute :>> mass : Real;` or `attribute mass redefines mass : Real;` |
| V2 | `part myCar : Car { attribute mass = 1500.0; }` or `mass = 1500.0;` | `attribute :>> mass = 1500.0;` or `:>> mass = 1500.0;` |
| V3a | two `attribute mass` | delete one, or rename (`dryMass`) |
| V3b | `attribute def load; attribute load : Real;` | rename one (e.g. `attribute def LoadValue;`) |
| V4a | `part def Truck :> Powered, Insured { … rating … }` | `attribute powerRating :>> Powered::rating; attribute insuranceRating :>> Insured::rating;`, then refer to the new names |
| V4b | `part B :> A1, A2 { part p; }` | `part :>> A2::p;` to constrain it, or `part q;` for a genuinely new feature |
| V5 | `attribute masss = 1500.0;` | `attribute :>> mass = 1500.0;` (a typo now fails resolution) |
| V6a | `attribute mass :> mass;` | `attribute mass :> ISQ::mass;` |
| V6b | `attribute :>> mass = 1500 [kg];` (supertype has `totalMass`) | `attribute :>> totalMass = 1500 [kg];` or `attribute :>> Vehicle::totalMass …` |
| V7a | `in target …; in source …;` (swapped) | keep the general order, or `in target :>> Transfer::target; in source :>> Transfer::source;` (and see K6 for how tools treat this today) |
| V7b | `in target :>> target;` at position 1 | declare `in source;` first (positional), then `in target :>> target;` |
| V8 | delete `:>>` to silence the overriding error | make the general value `default` (`attribute mass : Real default 1000.0;`), then `:>> mass = 1500.0` |
| V9 | `attribute cutoff : Rational default = …` in `TestWithCutoff` | `attribute redefines cutoff : Rational default = …` |
| V6f | `part def Car :> Vehicle { attribute mass : mass; }` (or `: ISQ::mass`, `: kg`) | `attribute :>> mass : MassValue;` or `attribute :>> mass :> ISQ::mass;`; values in a unit: `attribute :>> mass = 1500 [kg];` |
| V10a | (a name for the inherited feature is wanted) | `alias weight for mass;` |
| V10b | `alias mass for ISQ::mass;` in a specialization of `Vehicle` | `alias isqMass for ISQ::mass;` |
| V10c | `attribute weight : Real;` where `weight` is an inherited alias | `attribute :>> mass : Real;` (and `alias weight for mass;` if the name is still used) |
| V10d | `attribute :>> mass = 1500.0;` (or `:>> weight`), then a use of `weight` | `attribute :>> mass = 1500.0; alias weight for mass;` |
| V10e | `attribute weight :>> mass;` meant as a second name | `alias weight for mass;` (or `attribute weight :>> mass; alias mass for weight;` to rename and keep the old name) |
| V10f | `part def P :> Metric, Scale;` with alias `Metric::weight` and feature `Scale::weight` | `attribute scaleWeight :>> Scale::weight;` in `P`, or rename one in its general |

---

## 7. References

### 7.1 Specifications

These are the SysML-v2-Release `2026-08` documents. Page numbers are printed pages.

- **KerML v1.1 Beta 2**
  - 7.2.5.2 Namespace Declaration (pp.21–23)
  - 7.2.5.4 Imports (p.23)
  - 7.3.2.1 Types Overview (p.26)
  - 7.3.2.3 Specialization (pp.27–28)
  - 7.3.2.4 Conjugation (p.28)
  - 7.3.4.5 Redefinition (pp.36–37)
  - 7.4.5 Associations (p.45)
  - 7.4.7.2 Behavior Declaration (p.52)
  - 7.4.13 Metadata (p.69)
  - 8.2.3.5.1–8.2.3.5.4 Name Resolution (pp.82–85)
  - 8.3.1 Abstract Syntax Overview (p.109)
  - 8.3.2.4.3 Membership, `isDistinguishableFrom` (pp.126–127)
  - 8.3.2.4.5 Namespace: `importedMemberships` (p.129), `resolve`/`resolveLocal` (pp.129–130), `validateNamespaceDistinguishibility` (p.132)
  - 8.3.3.1.10 Type: `inheritedMembership` (p.144), `allRedefinedFeaturesOf` (p.145), `inheritableMemberships`/`inheritedMemberships` (p.146), `removeRedefinedFeatures` (p.147), `deriveTypeInheritedMembership` (p.149)
  - 8.3.3.3.4 Feature: `allRedefinedFeatures` (p.162), `checkFeatureEndRedefinition` (p.166), `checkFeatureParameterRedefinition` (p.168), `checkFeatureResultRedefinition` (p.169)
  - 8.3.3.3.8 Redefinition constraints (pp.176–177)
  - 8.3.4.10.2 FeatureValue, `validateFeatureValueOverriding` (p.228)
  - 8.4.2 Semantic Constraints and Implied Relationships (pp.239–240)
- **SysML v2.1 Beta 2 Part 1**
  - 7.5.1 (pp.23–24)
  - 7.6.1 (pp.31–33)
  - Table 4 (p.35)
  - 7.6.3 (pp.40–41)
  - 7.6.5 (pp.42–43)
  - 7.6.7 (p.43)
  - 7.8.1 (p.47)
  - 7.13.2 (p.71)
  - 7.13.4 (pp.72–73)
  - 7.17.2 (p.100)
  - 7.17.6 (p.104)
  - 7.21.2 (p.134)
  - 7.27.2 (pp.158–159)
  - 8.4.1 and Tables 31–33 (pp.397–403)
  - 8.4.13.1 (p.423)

### 7.2 Grammar (`SysML-v2-Release/bnf`, tag 2026-08)

- **`SysML-textual-bnf.kebnf`**
  - `REDEFINES` L38
  - `UsageDeclaration` L314
  - `ValuePart`/`FeatureValue` L323-L330
  - `DefaultReferenceUsage` L348-L349
  - `ReferenceUsage` L351
  - `NonOccurrenceUsageElement` L361
  - `Redefinitions`/`Redefines`/`OwnedRedefinition` L488-L497
  - `AttributeUsage` L529
  - `EnumeratedValue` L547
  - `MetadataBodyUsage` L1706-L1709 (clause 8.2.2.27)
- **`KerML-textual-bnf.kebnf`**
  - `REDEFINES` L144
  - `Feature` L562
  - `FeatureDeclaration` L601
  - `Redefinitions` L663
  - `OwnedRedefinition` L708-L709
  - `MetadataBodyFeature` L1435-L1438

### 7.3 Examples and training (SysML-v2-Release tag 2026-08)

- `sysml/src/training/05. Redefinition/Redefinition Example.sysml`
- `sysml/src/examples/Vehicle Example/VehicleDefinitions.sysml` L10, L16
- `sysml/src/examples/Mass Roll-up Example/MassRollup.sysml` L4-L6
- `sysml/src/examples/Analysis Examples/Dynamics.sysml` L26-L67
- `sysml/src/examples/Analysis Examples/Vehicle Analysis Demo.sysml` L173-L177, L267-L277
- `sysml/src/examples/Association Examples/ProductSelection_UnownedEnds.sysml` L11-L21
- *Intro to the SysML v2 Language – Textual Notation*, slide 12 "Redefinition" and slide 94 "Analysis Case Usages"
- A heuristic scan of `sysml/src` for definitions or usages that re-declare a same-named feature of a same-file general type without `:>>` found only positional parameter and end re-declarations (V7c-style), and no instance of V1/V2. The scan is a regex heuristic, not a parser.

### 7.4 Pilot implementation source

All links are at commit `692170b71867353b8f90341e61556f49a5beb0e5` (tag 2026-08, v0.62.0).

- **`KerMLValidator.xtend`**
  - [constants L131-L135](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L131-L135)
  - [checkNamespace L419-L449](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L419-L449)
  - [nameMap/checkDistinguishibility/identifyDuplicates L451-L518](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L451-L518)
  - [checkFeature implicit-redefinition check L616-L619](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L616-L619)
  - [checkRedefinition L704-L725](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L704-L725)
  - [checkFeatureValue L1315-L1324](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/validation/KerMLValidator.xtend#L1315-L1324)
- [`SysMLValidator.xtend` L142](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xtext/src/org/omg/sysml/xtext/validation/SysMLValidator.xtend#L142)
- **Scoping**
  - [`KerMLScope.xtend` L208-L253, L259-L305, L361-L436](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScope.xtend#L208-L436)
  - [`KerMLScopeProvider.xtend` L86-L173](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext/src/org/omg/kerml/xtext/scoping/KerMLScopeProvider.xtend#L86-L173)
- **Model logic**
  - [`Membership_isDistinguishableFrom_InvocationDelegate.java` L37-L59](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/delegate/invocation/Membership_isDistinguishableFrom_InvocationDelegate.java#L37-L59)
  - [`TypeAdapter.java` L122-L204, L390-L422](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/TypeAdapter.java#L122-L204)
  - [`TypeUtil.java` L103-L108](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/util/TypeUtil.java#L103-L108)
  - [`FeatureAdapter.java` L520-L526, L573-L582, L626-L725](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/FeatureAdapter.java#L520-L725)
  - [`NamespaceAdapter.java` L104-L113](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/NamespaceAdapter.java#L104-L113)
  - [`NamespaceImportAdapter.java` L45-L54](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/NamespaceImportAdapter.java#L45-L54)
  - [`MembershipImportAdapter.java` L43-L58](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/adapter/MembershipImportAdapter.java#L43-L58)
  - [`Namespace_resolveLocal_InvocationDelegate.java` L40-L49](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/delegate/invocation/Namespace_resolveLocal_InvocationDelegate.java#L40-L49)
  - [`NamespaceUtil.java` L104-L111](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.logic/src/main/java/org/omg/sysml/util/NamespaceUtil.java#L104-L111)
  - [`NamespaceImpl.java` L138](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.model/src/main/java/org/omg/sysml/lang/sysml/impl/NamespaceImpl.java#L138)
  - [`TypeImpl.java` L378](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.model/src/main/java/org/omg/sysml/lang/sysml/impl/TypeImpl.java#L378)
- **Quick fixes**
  - [`KerMLQuickfixProvider.xtend` L15-L30](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xtext.ui/src/org/omg/kerml/xtext/ui/quickfix/KerMLQuickfixProvider.xtend#L15-L30)
  - [`SysMLQuickfixProvider.xtend` L15-L30](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xtext.ui/src/org/omg/sysml/xtext/ui/quickfix/SysMLQuickfixProvider.xtend#L15-L30)
- **Tests**
  - [`MembershipTests_Distinguishability.kerml.xt`](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.kerml.xpect.tests/src/org/omg/kerml/xpect/tests/validation/MembershipTests_Distinguishability.kerml.xt#L17-L45)
  - [`Membership_distingushability_invalid.sysml.xt`](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xpect.tests/src/org/omg/sysml/xpect/tests/validation/invalid/Membership_distingushability_invalid.sysml.xt#L35-L46)
  - [`RedefinitionDiamond_Invalid.sysml.xt`](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xpect.tests/src/org/omg/sysml/xpect/tests/validation/invalid/RedefinitionDiamond_Invalid.sysml.xt#L27-L43)
  - [`RedefinitionDiamond0.sysml.xt`](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xpect.tests/src/org/omg/sysml/xpect/tests/validation/valid/RedefinitionDiamond0.sysml.xt#L27-L43)
  - [`Redefinition_OwningType.sysml.xt`](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xpect.tests/src/org/omg/sysml/xpect/tests/validation/valid/Redefinition_OwningType.sysml.xt#L28-L38)
  - [`Redefinition_OwningType_Invalid.sysml.xt`](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/blob/692170b71867353b8f90341e61556f49a5beb0e5/org.omg.sysml.xpect.tests/src/org/omg/sysml/xpect/tests/validation/invalid/Redefinition_OwningType_Invalid.sysml.xt)
- PR [#133](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/pull/133), "ST6RI-245 Membership Distinguishability Validation", merged 2020-08-10.

### 7.5 Existing issues

Status is as shown on issues.omg.org on 2026-10-25. None of them reports the re-declaration pitfall itself, so this submission does not duplicate any of them.

| Key | Title | Status | Relation |
|---|---|---|---|
| [KERML11-72](https://issues.omg.org/issues/KERML11-72) | Redefinition inheritance anomaly | open | prerequisite for raising severity (V4c, S8) |
| [KERML11-75](https://issues.omg.org/issues/KERML11-75) | OCL for importedMemberships is incorrect | open | S5, K4, V6d |
| [KERML11-76](https://issues.omg.org/issues/KERML11-76) | Library models have inherited member name collisions | open | S9, K7 |
| [KERML11-140](https://issues.omg.org/issues/KERML11-140) | Name resolution for the target of a redefinition | open | coordinate with K5/A4 (V6b) |
| [KERML11-198](https://issues.omg.org/issues/KERML11-198) | Names in KerML OCL that do not resolve | open | may cover parts of S12 |
| [KERML11-6](https://issues.omg.org/issues/KERML11-6) | Error in constraint checkFeatureParameterRedefinition | open | same constraint as K6 (OCL `indexOf` and parentheses) |
| [KERML11-47](https://issues.omg.org/issues/KERML11-47) | Impossible to specialize (redefine, subset) unnamed features in textual syntax | open | exemption for anonymous features |
| [KERML11-201](https://issues.omg.org/issues/KERML11-201) | Textual notation does not expose inherited members | open | `^` notation, complementary to option C |
| KERML_-58 | checkFeatureParameterRedefinition fails for named-argument invocations | closed | precedent for name-based parameter binding |
| [SYSML21-311](https://issues.omg.org/issues/SYSML21-311) | Substates may potentially inherit "this" twice | open | S8/S9 |
| [SYSML21-322](https://issues.omg.org/issues/SYSML21-322) | Library models have inherited member name collisions | open | S9 |
| [SYSML21-318](https://issues.omg.org/issues/SYSML21-318) | Diamond inheritance problem with TradeStudy | open | diamond parameter redefinitions (V4/V7) |
| [SYSML21-645](https://issues.omg.org/issues/SYSML21-645) | redefinition of directed action features | open | wording of SysML 7.17.2 (same paragraph as S-c) |
| [SYSML21-646](https://issues.omg.org/issues/SYSML21-646) | Attributes with Quantity Values are not redefinable | open | V8/S13 |
| SYSML21-7 | Missing descriptions of specialized effective naming | open | effective-name exemption |
| [UML25-315](https://issues.omg.org/issues/UML25-315) | Redefinition does not allow for overloading | closed (UML 2.5) | background: UML allowed implicit redefinition by name |
| [Sensmetry note](https://issues.omg.org/secure/attachment/22610/Unspecified%20Behavior%20in%20SysML%20v2%20Name%20Resolution.pdf) | Unspecified Behavior in KerML Name Resolution | attachment (links KERML_-43) | V6e (global namespace) |

The pilot GitHub tracker was searched for "distinguishability", "Duplicate of", "redefin", "name", "inherited", "shadow" and "hiding". The only hits were PR #133 and unrelated issues, such as [#819](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation/issues/819) on default multiplicity.

---

## Appendix A: Evidence ledger for pilot-implementation claims

Claims were first derived from the source and test files, then the model-level claims were confirmed by running pilot 0.62.0 and 0.63.0 (§2.10).

**Verified from source or test files ([SRC]/[TEST])**

1. Distinguishability diagnostics are `warning(...)`, never `error(...)` (KerMLValidator L486-L508).
2. The messages are "Duplicate of other owned member name", "Duplicate of owned member name", "Duplicate of other alias name" and "Duplicate of inherited member name" (L132-L135). The inherited message appends `'name' from A, B` (L512-L518).
3. Owned vs. inherited and inherited vs. inherited checks exist for `Type` namespaces; imported memberships are not checked (L425, L440-L446).
4. Automatically constructed expressions and binding connectors are skipped (L422-L424).
5. Name comparison is exact; there is no metaclass check (L482, L493; delegate TODO L42). The tests expect warnings for same-named classifier, package and feature.
6. Xpect tests assert the inherited-duplicate warning for SysML `part b` and for nested classifiers. `RedefinitionDiamond0` expects `noErrors`. `Redefinition_OwningType` accepts redefining a package-level feature.
7. Inherited memberships remove redefined features, including computed or implicit redefinitions (TypeAdapter L149-L169, L198-L204; TypeUtil L103-L108).
8. Linker precedence is owned, then inherited, then imported, then outer scopes. Candidates of the wrong metaclass are skipped. The first general type wins among non-redefining same-named features (KerMLScope L224-L253, L361-L414; ScopeProvider L161-L173). Redefinition targets search inherited members locally but keep the outer scopes.
9. The element being declared is matched by its declared name (KerMLScope L278-L281).
10. `resolveLocal` takes the first match in `getMembership()`. The union order is imported, owned (Namespace) or imported, owned, inherited (Type). Imports are not filtered for collisions.
11. Positional implicit redefinition pairs by index without name comparison (FeatureAdapter L626-L725). It is computed even with explicit redefinitions, except for invocation and constructor features (L520-L526). Only identical implicit redefinitions are removed (TypeAdapter L390-L422). The validator checks only direction for implicit redefinitions (L616-L619).
12. Overriding a bound value is an error, "Cannot override a binding feature value" (L1315-L1323).
13. There are no quick fixes. `queryScope` returns an empty set in both quick-fix providers.
14. The issue code string is `validateNamespaceDistinguishablity` (L131).
15. `SysMLValidator` extends `KerMLValidator` and does not override `checkNamespace`.
16. The bundle version at the tag is 0.62.0.
17. Alias memberships are checked like owning ones. Owned aliases are checked against owning members (L436-L437), against each other (L438), and, with all owned memberships, against inherited ones (L443-L445). Inherited memberships, aliases included, are checked against each other (L446). Members of the same element are never duplicates (L482, L493, L506).
18. `removeRedefinedFeatures` removes inheritable memberships by member element, whatever the kind of membership, so a general's alias of a redefined feature is removed with it (TypeAdapter L149-L168).
19. Unlike rule 1 of the specification, it leaves each membership's own element out of the redefined features it collects, so two memberships of one element do not remove each other (TypeAdapter L162-L168).

**Confirmed by running pilot 0.62.0 and 0.63.0 ([RUN], §2.10)**

- The exact diagnostics for V2, including the additional warnings when `Car` has the V1 defect.
- V3b resolution to the usage.
- V4a message text, and resolution to whichever general type is listed first.
- V4b producing no warning.
- V5 producing no diagnostic for `masss`.
- V6a self-subsetting, with no diagnostic.
- V6b silent resolution to `ISQBase::mass`, with no diagnostic.
- V6d `resolveLocal` returning the imported membership while the linker resolves to the owned one.
- V7a swapped redefinitions, with no diagnostic.
- V7b redefining two parameters at once, with no diagnostic.
- V8 `car1` producing an error and `car2` only a warning.
- V9 producing a warning.
- V6f: each attribute typed by itself, by `ISQ::mass` or by `kg` producing an error plus the duplicate warning; part, item, port, action, occurrence and `ref` usages and a subject typed by a usage producing errors.
- V10a: aliases of the inherited feature producing no diagnostic, and both names resolving to it.
- V10b: the owned alias winning resolution in the type and redirecting `:>> mass` in a specialization, with warnings on both.
- V10c: warnings on features named like an inherited alias, by name and by short name.
- V10d: aliases dropped by redefinition, via the name and via the alias, with linking errors only at the uses.
- V10e: the old name failing to resolve after a renaming redefinition, but not after an alias.
- V10f: warnings for an alias and a feature with one name from two generals, the general listed first winning.
- V10g: the fixed forms producing no diagnostic, including the rename that keeps the old name as an alias and the V10f fix.

**Still inferred ([INF])**

- Xtext's default "Change to …" linking quick fixes being disabled by the empty `queryScope`. This is IDE behavior
  and rests on the Xtext framework; it was not run.

## Appendix B: Suggested Xpect test cases for the pilot

```sysml
// invalid/RedeclaredInheritedFeature.sysml.xt
part def Vehicle { attribute mass : ScalarValues::Real; }
part def Car :> Vehicle {
    // XPECT errors --> "Feature 'mass' does not redefine inherited feature 'Vehicle::mass' and has the same name, so it declares a second, distinct feature. Use ':>> mass' to redefine it." at "mass"
    attribute mass : ScalarValues::Real;
}
part myCar : Car {
    // XPECT warnings --> "'masss' is a new feature; did you mean to redefine inherited 'mass' (':>> mass')?" at "masss"
    attribute masss = 1500.0;
}

// invalid/ParameterPositionMismatch.sysml.xt
item def Account;
action def Transfer { in source : Account; in target : Account; }
action def AuditedTransfer :> Transfer {
    // XPECT warnings --> "Parameter 'target' redefines 'Transfer::source' by position; 'Transfer::target' is at position 2. Reorder, or redefine explicitly." at "target"
    in target : Account;
    in source : Account;
}

// invalid/RedefinitionFallThrough.sysml.xt
private import ISQ::*;
part def Vehicle { attribute totalMass :> ISQ::mass; }
part def Car :> Vehicle {
    // XPECT warnings --> "'mass' resolves to 'ISQ::mass', which is not inherited by 'Car'; qualify the name if this is intended." at "mass"
    attribute :>> mass;
}

// invalid/AliasNameCollision.sysml.xt
part def Vehicle { attribute mass : ScalarValues::Real; alias weight for mass; }
part def Car :> Vehicle {
    // XPECT errors --> "Alias 'mass' stands for 'ISQ::mass', but 'Vehicle::mass' is inherited under the same name; owned members are found first, so 'mass' now means 'ISQ::mass' here and in specializations." at "mass"
    alias mass for ISQ::mass;
}
part def Truck :> Vehicle {
    // XPECT errors --> "Feature 'weight' does not redefine 'Vehicle::mass', which is inherited under the alias name 'weight', so it declares a second, distinct feature. Use ':>> mass' to redefine it." at "weight"
    attribute weight : ScalarValues::Real;
}

// valid/AliasOfInheritedFeature.sysml.xt
// XPECT noErrors ---> ""
part def Vehicle { attribute mass : ScalarValues::Real; }
part def Car :> Vehicle { alias weight for mass; alias mass for Vehicle::mass; }
part def Truck :> Vehicle {
    attribute :>> mass = 1500.0;
    alias weight for mass;              // re-declared after the redefinition (K9)
}
```

# Positive-suite syntax errors, categorized

> **Status (2026-10-25): resolved.** G1 and G2 are fixed in `SysMLv2Parser.g4`, and the T1-T11 test-file lines
> are corrected. `test-suite/positive` now validates with 0 errors (51 files). Of the follow-ups in section 3,
> `defined by` and filtered imports are fixed (with the other gaps found by the official SysML v2 examples: all 252
> files of `sysml/src` now parse, see `OfficialExamplesTest`); the validator-leniency items remain open.

The 41 reported syntax errors in 15 files come from **28 root errors** and **13 cascades**.

- **Validator bugs:** only 3 roots are real grammar gaps. They fall into 2 categories.
- **Bad test files:** the other 25 roots are test files that are not valid SysML v2 under the OMG 0.62.0 BNF.

Fixing everything takes 2 grammar changes and about 25 test-line edits. The scratch runs show each category clears. The exceptions are flagged in section 4.

BNF references are `SysML-textual-bnf.kebnf:<line>` unless marked KerML. Validator references are `SysMLv2Parser.g4:<line>`. File names are shortened to their number prefix, for example "46" means `46_loop_conditional.sysml`. "dup" means a second message that ANTLR's recovery reports on the same token.

## 1. Summary

| ID | Category | Type | Roots | Cascades | Files (line:col) |
|---|---|---|---|---|---|
| G1 | Transition source written as a qualified name or chain, with no `first` | GRAMMAR_GAP | 2 | 0 | 12_simple_vehicle_model:59:41, 73:41 |
| G2 | Action usages or parameters inside `while`/`loop`/`for` bodies | GRAMMAR_GAP | 1 | 0 | 46_loop_conditional:20:12 |
| T1 | `return <expr>;` used as a calc result | INVALID_TEST | 3 | 0 | 47_expression_operators:60:17; 48_library_package:35:15, 41:19 |
| T2 | Transition trigger placed after the target: `then X when after(n);` | INVALID_TEST | 3 | 1 | 03_state_machine:19:12 (+dup), 24:12, 29:12 |
| T3 | Made-up `signal X;` declaration | INVALID_TEST | 3 | 0 | 41_state_entry_exit:57:7, 58:7, 59:7 |
| T4 | Malformed loops: `action loop name {`, `} over values;`, `loop while` | INVALID_TEST | 3 | 2 | 46:11:8 (fault is the line-10 header), 13:11, 19:13; cascades 11:17, 45:0 |
| T5 | Clauses that don't exist in usage declarations: `satisfies`, `about`, `typed by` | INVALID_TEST | 3 | 2 | 24_rendering_definitions:33:46 (cascades 34:4, 34:22); 35_dependency_concern:19:22; 42_feature_typing:13:25 |
| T6 | Reserved keyword used as an element name (`send`, `library`) | INVALID_TEST | 2 | 1 | 26_message_accept:30:10; 32_reference_subsetting:16:17 (cascade 19:0) |
| T7 | `->` operations that don't exist (`->all`, `->first`), named with reserved words and no argument list | INVALID_TEST | 2 | 2 | 36_sequence_collections:7:34 (+dup), 13:35 (+dup) |
| T8 | Requirement-only members outside a requirement body | INVALID_TEST | 2 | 2 | 20_verification_cases:36:0 (+dup); 34_constraint_assertions:12:4 (+dup) |
| T9 | `dependency import X::*;` (a dependency written as an import) | INVALID_TEST | 2 | 0 | 35_dependency_concern:8:15, 8:36 (one line) |
| T10 | Grouped import `import P::{A, B};` | INVALID_TEST | 1 | 2 | 45_import_membership:12:22 (cascades 12:40 x2) |
| T11 | Made-up infix operator `dot` | INVALID_TEST | 1 | 1 | 47_expression_operators:42:35 (+dup) |
| | **Total** | 3 gap roots / 25 invalid-test roots | **28** | **13** | 15 files |

## 2. Categories

### G1. Transition source is a qualified name or chain (GRAMMAR_GAP)
- **Example (valid SysML):** `transition States::StateAction::start then off;` and `transition a.done then b;`
- **Official BNF:**
  - `:1293` `TransitionUsage = 'transition' (UsageDeclaration 'first')? FeatureChainMember ... 'then' TransitionSuccessionMember ActionBody`
  - `:1123` `FeatureChainMember = [QualifiedName] | OwnedFeatureChainMember`
- **Validator rule at fault:** `transitionUsage` (g4:639-643).
  - Alternative 1, `TRANSITION usageName? transitionRelationships? transitionBody`, reads `States` as the transition's name and then fails on `::`.
  - Alternative 2 needs `FIRST`.
- **Fix (grammar only, test file unchanged):** add `| TRANSITION featureChain transitionBody` (`featureChain` is at g4:942).
  - This was checked in a scratch ANTLR 4.13.2 build: file 12 gives 0 syntax errors, and file 03 is still correctly rejected.
  - No Java code reads `TransitionUsageContext`, so the change is low-risk.
  - Optional cleanup closer to the BNF: `TRANSITION (usageName? featureRelationships? FIRST)? featureChain transitionBody`. Today alternative 1 also reads `transition initial then off;` with `initial` as the transition's name instead of its source. That is harmless only because nothing consumes the parse tree.

### G2. Usages inside loop bodies (GRAMMAR_GAP)
- **Example (valid SysML):** `while count < 10 { action increment { } }`
  - The official `training/17. Control/Control Structures Example.sysml:14-15` (`loop action charging { action monitor : MonitorBattery {`) fails the same way.
- **Official BNF:**
  - `:1152` `ActionBodyParameter = ('action' UsageDeclaration?)? '{' ActionBodyItem* '}'`. `ActionBodyItem` includes `ActionUsage` and parameter usages with a direction.
  - `WhileLoopNode` is at `:1159` and `ForLoopNode` at `:1167`.
- **Validator rule at fault:** five bodies are `LBRACE statement* RBRACE`:
  - `whileStatement` g4:1448-1450
  - `loopStatement` g4:1452-1454
  - `forStatement` g4:1456-1458
  - two `thenStatement` alternatives, g4:1399 and g4:1405

  `statement` (g4:1372) has no usages and no parameters.
- **Fix:**
  - Change those five bodies to `LBRACE usageBodyElement* RBRACE`. `usageBodyElement` (g4:1126) already includes `statement`, and `ifStatement`, `elseIfClause` and `elseClause` (g4:1435-1446) already use this form.
  - Change the loop header to `LOOP (ACTION usageName? featureRelationships?)? LBRACE usageBodyElement* RBRACE (UNTIL expression SEMICOLON)?` so the official `loop action name {` parses.
  - That header change also drops the non-standard `expression?` after `LOOP`. Only test 46 relies on it.
  - Give `while` and `for` the same optional `ACTION usageName?` before `{`.

### T1. `return <expr>;` (INVALID_TEST)
- **Invalid → valid:** `return width * height;` → `return : Real = width * height;`
  - The valid form is the official one (`training/30. Calculations/Calculation Definitions.sysml:9`).
  - A bare last expression `width * height` with no `;` also works.
- **Official BNF:**
  - `:1386` `ReturnParameterMember = MemberPrefix? 'return' UsageElement`. This is a declaration, so `return width` declares a result named `width`, and a literal or `*` or `(` cannot follow.
  - `:1389` `ResultExpressionMember = MemberPrefix? OwnedExpression`.
- **Validator:** `returnFeature` (g4:1096) matches the BNF, so the rejection is correct.
- **Fix:** rewrite 47:60, 48:35 and 48:41. For example, 48:41 becomes `return : Real = sqrt(a * a + b * b);`.

### T2. Transition trigger after the target (INVALID_TEST)
- **Invalid → valid:** `transition red_to_green first red then green when after(30);` → `transition red_to_green first red accept after 30 [s] then green;`, plus `private import SI::s;` in the package.
- **Official BNF:**
  - `:1293`: the trigger must come before `then`, and only `;` or `{...}` may follow the target.
  - `:1321` `TriggerActionMember = 'accept' TriggerAction`
  - `:1044` `TriggerExpression = ('at'|'after') ArgumentMember | 'when' ...`
  - `when` is a reserved keyword.
- **Official usage:** `training/25. Transitions/Change and Time Triggers.sysml` writes `accept after 48 [h] then normal;`.
- **Fix:** rewrite 03:19, 24 and 29. Each one is its own root error.

### T3. `signal` declaration (INVALID_TEST)
- **Invalid → valid:** `signal StartSignal;` → `attribute def StartSignal;` (as in the official `training/25. Transitions/Transition Actions.sysml`). `item def StartSignal;` also works.
- **Official BNF:** the word `signal` appears nowhere in the SysML or KerML BNF. `signal X` would be two names in a row, which nothing allows.
- **Fix:** rewrite 41:57-59. Each line is independent.

### T4. Malformed loops in file 46 (INVALID_TEST)
- **Line 10, `action loop processEach {`** (reported at 11:8, on `in`):
  - `loop` is reserved.
  - Per `:1159` and `:1152`, only `action` or `{` may follow `loop`.
  - The official form is `loop action charging {`.
- **Line 13, `} over values;`:**
  - `over` is in neither BNF.
  - In `ForLoopNode` (`:1167`) the collection comes before the body.
- **Line 19, `loop while (count < 10) {`:** in `WhileLoopNode`, `while` and `loop` are alternatives, not a combination.
- **Fix:**
  - Lines 10-13 → `action processEach for value : Real in values { out result : Real; }`. This needs G2 to parse.
  - What the validator accepts today is `action processEach loop { in value : Real; out result : Real; }`.
  - Line 19 → `while count < 10 {`, or `loop { ... } until count >= 10;` (official `examples/Simple Tests/StructuredControlTest.sysml:20` and `:28`).
  - The cascades at 11:17 and 45:0 (lost brace depth) clear with the line-10 fix.

### T5. Clauses that don't exist in declarations (INVALID_TEST)
- **24:33** `view 'Detailed View' : SymbolicView satisfies StructuralViewpoint {`
  - Fix: drop `satisfies StructuralViewpoint` and put `satisfy requirement sv : StructuralViewpoint;` inside the view body.
  - BNF: `ViewUsage` (`:1622`) has no such clause. `SatisfyRequirementUsage` is at `:1482-1489`. Annex A `SimpleVehicleModel.sysml:1574` shows the in-body form.
  - The cascades at 34:4 and 34:22 clear with this fix.
- **35:19** `concern 'Testability' about Application {`
  - Fix: `concern 'Testability' { subject app : ApplicationPackage::Application; ... }`
  - BNF: `ConcernUsage` is at `:1509`. `about` exists only in `Comment` (`:84`) and `MetadataUsage` (`:1685`).
- **42:13** `part sensor : Sensor typed by TemperatureSensor;`
  - Fix: `part sensor : Sensor, TemperatureSensor;` or just `: TemperatureSensor`.
  - BNF: `typed by` is KerML-only (KerML `:134`). SysML uses `TypedBy` (`:450`) with `DEFINED_BY = ':' | 'defined' 'by'` (`:28`).

### T6. Reserved keyword as a name (INVALID_TEST)
- **26:30** `first send then receive;`
  - Line 18, `action send {`, is not an action named `send`. It parses as an anonymous send action.
  - Fix: rename to `sendMsg` on both line 18 and line 30.
  - Also change line 19 `out message : Message;` to `out outMsg : Message;`, because `message` is reserved too.
- **32:16** `part library : Library;` → `part lib : Library;` (or quote it as `'library'`).
  - The real offending token is at column 9. The error points at column 17 because the parser falls into the library-package rule.
- **Official BNF:** `RESERVED_KEYWORD` is at `:13-26`, and a name must be a NAME, which excludes reserved words.
- **Validator:** the `name` rule (g4:894) correctly excludes these keywords.

### T7. `->` operations that don't exist (INVALID_TEST)
- **Invalid → valid:**
  - `values->all` → `values`, or `values->forAll {in v : Real; v > 0.0}` if a check was meant.
  - `values->first` → `values#(1)` or `values->head()`.
- **Official BNF:** KerML `FunctionOperationExpression` (KerML `:1134-1140`) makes the argument list or body mandatory. `all` and `first` are reserved words, and the library defines no functions with those names.

### T8. Requirement-only members outside a requirement body (INVALID_TEST)
- **20:36** `verify safetyReq by SpeedTest;` at package level.
  - Fix: `verification speedVerification : SpeedTest { subject testSystem; objective { verify safetyReq; } }`
  - BNF: `RequirementVerificationMember` (`:1563`) is allowed only as a `RequirementBodyItem` (`:1423-1430`), and it has no `by` clause. `by` appears only in `SatisfyRequirementUsage` (`:1488`).
- **34:12** `assume constraint { value != 50.0 }` inside a part def.
  - Fix: `assert constraint { value != 50.0 }` (`:1402`), or move it into the requirement def.
  - BNF: `RequirementKind` (`:1442`) is reachable only from requirement bodies.

### T9. `dependency import LibraryPackage::*;` (INVALID_TEST)
- **Fix:** `private import LibraryPackage::*;`. Optionally add `dependency ApplicationPackage to LibraryPackage;` if a real dependency is also wanted.
- **Official BNF:** `Dependency` (`:51`) and `DependencyDeclaration` (`:56`) require client `to` supplier, with no wildcard.
- One edit clears both reports on line 8.

### T10. Grouped import `import SensorLibrary::{TemperatureSensor, PressureSensor};` (INVALID_TEST)
- **Fix:** two lines, `private import SensorLibrary::TemperatureSensor;` and `private import SensorLibrary::PressureSensor;`.
- **Official BNF:** `Import` (`:149`), `MembershipImport` (`:158`) and `NamespaceImport` (`:162`) have no brace list.
- **Line 47 must be fixed at the same time:** `import ScalarValues::RealValue as Real;` → `alias Real for ScalarValues::RealValue;`. It is invalid too, but recovery hides it until line 12 is fixed.

### T11. `dot` operator (INVALID_TEST)
- **Invalid → valid:** `vector1 dot vector2` → delete the line, or use `VectorFunctions::inner(vector1, vector2)`.
- **Official BNF:** KerML `BinaryOperator` and `ConditionalBinaryOperator` have no `dot`.

## 3. Prioritized fix list (biggest error reduction first)

| Priority | Fix | Kind | Errors removed |
|---|---|---|---|
| 1 | File 46: rewrite lines 10, 13 and 19 (T4), plus the G2 grammar body change | test + grammar | 6 |
| 2 | T5: one-line edits at 24:33, 35:19 and 42:13 | test | 5 |
| 3 | T2: three transitions in 03, plus `private import SI::s;` | test | 4 |
| 4 | T7: 36:7 and 36:13 | test | 4 |
| 5 | T8: 20:36 and 34:12 | test | 4 |
| 6 | T1: 47:60, 48:35 and 48:41 | test | 3 |
| 7 | T3: 41:57-59 | test | 3 |
| 8 | T6: 26:18/19/30 and 32:16 | test | 3 |
| 9 | T10: 45:12 together with the line-47 alias | test | 3 (only 2 if line 47 is left alone) |
| 10 | G1: add the `transitionUsage` alternative | grammar | 2 |
| 11 | T9: 35:8 | test | 2 |
| 12 | T11: 47:42 | test | 2 |

If the goal is validator correctness rather than error count, do G1 and G2 first. They are the only real validator bugs, they are small, and they also make official examples parse.

**Follow-up: the validator accepts invalid syntax.** Tighten these only after the test files are fixed:
- `&&`, `||`, `!` and `?:` are accepted (34:8, 47:23-25, 47:29). They are not KerML operators.
- `->name` without `()` is accepted (36:10, 14, 15) because `sequenceSuffix` (g4:1549) makes the parentheses optional. OCL `|` lambdas are also accepted (36:20, 23).
- `expressionStatement` (`expression SEMICOLON?`, g4:1431) accepts names side by side, such as `foo bar baz;` and `over values;`.
- `require constraint expr;` without braces (20:7) and the `method` keyword (20:22) are accepted.
- `loop <expr> {` is accepted. Item 1 removes this.
- `importDeclaration` has `visibility?` (g4:77), but the BNF requires a visibility on every import (`:149-150`).

**Follow-up: valid SysML the validator rejects (not among the 41 errors):** both fixed.
- `: X defined by Y`: the `DEFINED_BY` token (Lexer.g4:52) is never used by the parser, and `typingClause` (g4:1002) only takes `:`.
  Fixed: `typingClause` takes `:`, `defined by` and KerML's `typed by`.
- Filtered import `import P::*[@Meta];`: `importDeclaration` (g4:76-78) has no `[expr]` part.
  Fixed: `importDeclaration` takes filter members and a relationship body.

## 4. Uncertain items
- **G2 was not tested in a scratch build; G1 was.** There is a safe precedent: `ifStatement` already uses `usageBodyElement*`. Run the full suite after the change. Only test 46 uses `loop <expr> {`, so dropping `expression?` affects nothing else in `test-suite/`.
- **46:13 (`over`) is counted as a root.** The validator only reports it because of recovery from line 10. If only line 10 is fixed, the validator silently accepts `over values;`. The text is still invalid and must be changed.
- **35:8:36 is counted as a second root.** Removing just `import` still fails (scratch test 35e), but one edit clears both, so you could also count it as one.
- **6 of the 13 cascades are duplicate messages on the same token** (03:19, 20:36, 34:12, 36:7, 36:13, 47:42). The analyst data labelled them INVALID_TEST with the cascade line set to their own line. I counted them as cascades. Dropping duplicates by line and column in the error listener would cut the count from 41 to 35 with no grammar change.
- **Overlap with the missing Real/String/Integer task:**
  - 03 needs `private import SI::s;`.
  - 20, 34, 36, 42, 47 and 48 need `private import ScalarValues::*;`.
- **File 24 will still have semantic errors after the syntax fix.** `SymbolicView` and `asDefault` are not in `sysml.library` `Views.sysml`.
- **Imports without a visibility are a policy decision.** The strict 0.62.0 BNF makes bare `import` invalid. Two positive files use it on 4 lines in total. Making the validator strict would add errors there.
- **47:42:** the `VectorFunctions::inner(...)` replacement parses, but it may fail type-checking because `inner` takes `NumericalVectorValue`.
- **20:** the existing `subject safetyReq;` is semantically wrong: the subject should be the system under test.

No repo files were edited. The cited sources are:
- `/Users/turbogeek/Desktop/git/sysmlv2-validator/validator-core/src/main/antlr4/com/validator/parser/SysMLv2Parser.g4`
- `/Users/turbogeek/Desktop/git/sysmlv2-validator/validator-core/src/main/antlr4/com/validator/parser/SysMLv2Lexer.g4`
- `/Users/turbogeek/Desktop/git/SysML-v2-Release/bnf/SysML-textual-bnf.kebnf`
- `/Users/turbogeek/Desktop/git/SysML-v2-Release/bnf/KerML-textual-bnf.kebnf`
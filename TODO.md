# sysml-validator TODO

## Known Issues & Bugs (Identified during Cheat Sheet Generation)

### 1. Parser/Tokenizer Issues
- [ ] **Whitespace Concatenation Bug**: The validator appears to strip whitespace *between* tokens in certain constructs, leading to concatenation errors.
    - **Example**: `state A; transition t1` parses as `state Atransition t1` or similar, triggering syntax errors.
    - **Affected Constructs**: `transition` statements in State definitions, `send` actions in Action definitions, and `via` clauses in flows.
    - **Workaround**: Requires multi-line formatting or excessive spacing to sometimes mitigate, but is persistent in Windows environments.

### 2. Cameo Syntax Compatibility
- [ ] **Sequence Action Syntax**: Support the "shorthand" sequence syntax used by Cameo.
    - **Missing Syntax**: `first <action_name> then <next_action_name>;`
    - **Current Error**: `SYNTAX_ERROR: no viable alternative at input 'first ...'`.
    - **Context**: Cameo allows dropping the `succession` keyword in favor of a direct `first ... then` chain which isn't fully supported by the current standard grammar/parser implementation used here.

- [ ] **Standard Library Resolution**: Improve deep reference resolution for standard library elements.
    - **Issue**: References like `SysML::Actions::Action::start` or `SysML::Actions::terminate` fail with `no viable alternative` or `mismatched input`.
    - **Constraint**: The validator may not be indexing the implicit standard library dependencies deep enough or visibility rules are too strict compared to Cameo's internal web of definitions.

- [ ] **Type Casts & Filters**:
    - **Issue**: `filter` keyword usage in embedded views (`filter ConfiguredPart`) caused casting errors.
    - **Analysis**: Verify correct SysML v2 syntax for casts vs filters in views.

## Feature Requests

- [ ] **Better Error Messages**:
    - Errors like "extraneous input ':'" when using `perform : Action;` could be clearer (e.g., "Anonymous perform actions are not supported, use named usage").

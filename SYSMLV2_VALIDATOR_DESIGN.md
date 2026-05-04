# SysML v2 Semantic Validator - Design & Status

**Version**: 0.1.0-SNAPSHOT
**Last Updated**: December 2025
**Status**: Phase 2 - Semantic Validation with Lint Rules
**Tests**: 413 passing

---

## Executive Summary

A comprehensive ANTLR 4-based semantic validator for SysML v2 with intelligent error reporting, lint-style warnings, and refactoring capabilities. Features a custom ANTLR 4 grammar (first standalone implementation), symbol table with reference tracking, and extensive lint rules for code quality.

---

## Architecture Overview

```
+------------------------------------------------------------------+
|                        ValidatorCLI                               |
|                    (Picocli command-line)                         |
+---------------------------+--------------------------------------+
                            |
                            v
+------------------------------------------------------------------+
|                   SysMLv2ValidatorImpl                            |
|              (Main validation orchestrator)                       |
+---------------------------+--------------------------------------+
                            |
          +-----------------+-----------------+
          |                 |                 |
          v                 v                 v
+----------------+  +----------------+  +----------------+
| ParserFacade   |  | SemanticValid  |  | LintAnalyzer   |
| (ANTLR parse)  |  | (type/scope)   |  | (code quality) |
+----------------+  +----------------+  +----------------+
          |                 |                 |
          v                 v                 v
+----------------+  +----------------+  +----------------+
| ANTLR4 Lexer/  |  | SymbolTable    |  | Lint Rules     |
| Parser (.g4)   |  | ImportResolver |  | RefactorEngine |
+----------------+  +----------------+  +----------------+
```

---

## Completed Features

### Phase 1: Parser Integration [COMPLETE]

| Feature | Status | Description |
|---------|--------|-------------|
| Custom ANTLR 4 Grammar | DONE | First standalone SysML v2 grammar (80+ keywords) |
| Lexer (SysMLv2Lexer.g4) | DONE | Tokenization for all SysML v2 constructs |
| Parser (SysMLv2Parser.g4) | DONE | Full syntax parsing with error recovery |
| Parser Facade | DONE | Clean API wrapping ANTLR parser |
| CLI Integration | DONE | Picocli-based CLI with batch validation |
| Unit Tests | DONE | 34 parser tests passing |

### Phase 2: Semantic Validation [COMPLETE]

| Feature | Status | Description |
|---------|--------|-------------|
| Symbol Table | DONE | Tracks all definitions with qualified names |
| SymbolTableBuilder | DONE | Walks parse tree to build symbol table |
| Scope Management | DONE | Nested scopes for packages, definitions |
| Import Resolution | DONE | Resolves qualified names, aliases |
| Import Statement Model | DONE | Public/private, wildcard, recursive |
| Reference Tracking | DONE | Counts references to each symbol |
| Standard Library Manager | DONE | Provides standard library symbols (110 symbols) |
| Semantic Validator | DONE | Validates types, references, imports |
| Element Types | DONE | 30+ SysML v2 element types (ElementType enum) |
| KerML Support | DONE | Struct, assoc, behavior, connector handling |

### Phase 2.5: Lint Rules [COMPLETE]

| Rule | Error Code | Description |
|------|------------|-------------|
| Unused Definitions | LINT001 | Part/action def declared but never used |
| Unused Imports | LINT002 | Import statement bringing unused symbols |
| Missing Definition | LINT003 | Usage references undefined type |
| Untyped Attribute | LINT004 | Attribute without type annotation |
| Naming - Definition | LINT005 | Definitions should be PascalCase |
| Naming - Usage | LINT006 | Usages should be camelCase |
| Naming - Package | LINT007 | Packages should be PascalCase |
| Wildcard Import | LINT017 | `import Pkg::*` causes scope pollution |
| Recursive Wildcard | LINT018 | `import Pkg::**` even more problematic |
| Public Import | LINT019 | `public import` re-exports to importers |

### Phase 2.5: Refactoring Infrastructure [COMPLETE]

| Component | Status | Description |
|-----------|--------|-------------|
| RefactoringEngine | DONE | Orchestrates refactoring operations |
| RefactoringEdit | DONE | Represents source code edits |
| ImportUsageAnalyzer | DONE | Tracks which imported symbols are used |
| ImportRefactoring | DONE | Converts wildcard to specific imports |
| Output Formats | DONE | JSON, Monaco/LSP, plain text |

### Phase 2.5: User Dictionary [COMPLETE]

| Component | Status | Description |
|-----------|--------|-------------|
| UserDictionary | DONE | Loads/saves project-specific terms |
| UserDictionaryProvider | DONE | Integrates with spelling suggestions |
| Dictionary File Format | DONE | `.sysml-dictionary` file support |

### Phase 2.5: Constraint Analysis [PARTIAL]

| Component | Status | Description |
|-----------|--------|-------------|
| ConstraintAnalyzer | DONE | Basic constraint validation |
| ConstraintScope | DONE | Variable scoping in constraints |
| ConstraintType | DONE | Type enumeration for constraints |
| ExpressionTypeChecker | STUB | Type inference (needs completion) |

---

## Test Suite Statistics

| Category | Tests | Status |
|----------|-------|--------|
| Parser Tests | 34 | PASS |
| Semantic Tests | 68 | PASS |
| Symbol Table Tests | 66 | PASS |
| Import Tests | 30 | PASS |
| Lint Rule Tests | 45 | PASS |
| Constraint Tests | 8 | PASS |
| Dictionary Tests | 31 | PASS |
| Integration Tests | 131 | PASS |
| **Total** | **413** | **100%** |

---

## File Structure

```
sysml-validator/
├── validator-core/
│   └── src/main/java/com/validator/
│       ├── parser/
│       │   ├── SysMLv2Lexer.g4        # ANTLR lexer grammar
│       │   ├── SysMLv2Parser.g4       # ANTLR parser grammar
│       │   └── SysMLv2ParserFacade.java
│       ├── semantic/
│       │   ├── Symbol.java            # Symbol with reference tracking
│       │   ├── SymbolTable.java       # Symbol lookup
│       │   ├── SymbolTableBuilder.java
│       │   ├── Scope.java             # Scope management
│       │   ├── ImportStatement.java   # Import model
│       │   ├── ImportResolver.java    # Import resolution
│       │   ├── SemanticValidator.java
│       │   ├── StandardLibraryManager.java
│       │   └── ElementType.java       # 30+ element types
│       ├── lint/
│       │   ├── LintAnalyzer.java      # Lint orchestrator
│       │   ├── LintRule.java          # Rule interface
│       │   ├── LintConfig.java        # Configuration
│       │   ├── WildcardImportRule.java
│       │   ├── PublicImportRule.java
│       │   ├── NamingConventionRule.java
│       │   ├── UnusedElementRule.java
│       │   └── UsageWithoutDefinitionRule.java
│       ├── refactor/
│       │   ├── RefactoringEngine.java
│       │   ├── RefactoringEdit.java
│       │   ├── ImportUsageAnalyzer.java
│       │   └── ImportRefactoring.java
│       ├── constraint/
│       │   ├── ConstraintAnalyzer.java
│       │   ├── ConstraintScope.java
│       │   ├── ConstraintType.java
│       │   └── ExpressionTypeChecker.java
│       └── suggestions/
│           ├── SpellingSuggestionEngine.java
│           ├── UserDictionary.java
│           └── UserDictionaryProvider.java
├── validator-cli/
│   └── src/main/java/com/validator/cli/
│       └── ValidatorCLI.java          # CLI with refactoring options
├── test-suite/
│   ├── lint/                          # Lint test files (6 files)
│   ├── CheatSheet/                    # Syntax examples
│   └── DassaultLanguageExample/       # Cameo exports
└── examples/
    └── *.sysml                        # Example models
```

---

## Known Issues & TODOs

### Parser/Tokenizer Issues

| Issue | Priority | Status |
|-------|----------|--------|
| Whitespace concatenation in some constructs | Medium | INVESTIGATING |
| `first...then` shorthand syntax (Cameo) | Low | WONTFIX (non-standard) |
| Deep standard library references | Medium | PARTIAL |
| Filter keyword in views | Low | INVESTIGATING |

### Semantic Validation TODOs

| Task | Priority | Description |
|------|----------|-------------|
| Type inference for expressions | HIGH | Complete ExpressionTypeChecker |
| Cross-file validation | HIGH | Multi-file symbol resolution |
| Circular dependency detection | MEDIUM | Import cycle warnings |
| Unit compatibility checking | MEDIUM | ISQ/SI unit validation |
| Feature redefinition validation | MEDIUM | `:>>` semantics |

### Lint Rule TODOs

| Rule | Priority | Description |
|------|----------|-------------|
| Documentation warnings | LOW | Missing `doc` annotations |
| Complexity warnings | LOW | Deep nesting, large packages |
| Empty definition warnings | LOW | `part def X;` without body |
| Shadow definition warnings | MEDIUM | Local shadows imported name |

### Refactoring TODOs

| Task | Priority | Description |
|------|----------|-------------|
| Auto-apply refactoring (`--yes`) | MEDIUM | CLI option to apply edits |
| LSP server mode | LOW | Full language server protocol |
| Watch mode | LOW | Auto-validate on file change |

---

## CLI Usage

```bash
# Basic validation
java -jar sysml-validator.jar model.sysml

# Recursive directory validation
java -jar sysml-validator.jar -r ./models/

# With suggestions
java -jar sysml-validator.jar -s model.sysml

# Import refactoring analysis
java -jar sysml-validator.jar --refactor-imports model.sysml

# JSON output format
java -jar sysml-validator.jar --refactor-format json model.sysml

# Verbose mode
java -jar sysml-validator.jar -v model.sysml
```

---

## Test SysML Files for Training

Located in `test-suite/lint/`:

| File | Purpose | Lint Rules Demonstrated |
|------|---------|-------------------------|
| `wildcard_imports.sysml` | Wildcard import warnings | LINT017, LINT018 |
| `public_imports.sysml` | Public import warnings | LINT019 |
| `naming_conventions.sysml` | Naming convention checks | LINT005, LINT006, LINT007 |
| `unused_elements.sysml` | Unused element detection | LINT001, LINT002 |
| `good_practices.sysml` | Correct patterns (no warnings) | - |
| `refactoring_example.sysml` | Refactoring CLI usage | - |

---

## Build & Test

```bash
# Build
./build-with-portable-maven.cmd compile -DskipCheckstyle=true

# Run tests
./build-with-portable-maven.cmd test -DskipCheckstyle=true

# Run specific test
./build-with-portable-maven.cmd test -Dtest=LintTestFilesTest -pl validator-core

# Package
./build-with-portable-maven.cmd package -DskipTests -DskipCheckstyle=true
```

---

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| ANTLR 4 Runtime | 4.13.2 | Parser runtime |
| Apache Commons Text | 1.14.0 | Spelling suggestions |
| Jackson Databind | 2.15.3 | JSON processing |
| Picocli | 4.7.5 | CLI framework |
| JUnit 5 | 5.10.1 | Testing |
| SLF4J | 2.0.9 | Logging |

---

## Contributing

1. Run tests before committing: `./build-with-portable-maven.cmd test -DskipCheckstyle=true`
2. All 413 tests must pass
3. Follow naming conventions (PascalCase for classes, camelCase for methods)
4. Add tests for new features

---

## Roadmap

### Completed

- [x] Phase 1: ANTLR 4 parser integration
- [x] Phase 2: Symbol table and semantic validation
- [x] Phase 2.5: Lint rules and refactoring infrastructure

### In Progress

- [ ] Phase 3: Enhanced constraint expression validation
- [ ] Phase 3: Cross-file validation

### Planned

- [ ] Phase 4: Full KerML support
- [ ] Phase 5: LSP server for IDE integration
- [ ] Phase 6: Maven/Gradle plugins
- [ ] Phase 7: Sireum HAMR Integration for AADL Generation

---

## References

- [SysML v2 Specification](https://www.omg.org/spec/SysML/)
- [Official SysML v2 Release](https://github.com/Systems-Modeling/SysML-v2-Release)
- [ANTLR 4 Documentation](https://www.antlr.org/)

---

**Last Commit**: `f9a9199` - Add lint rules infrastructure with test SysML files
**Branch**: `phase2-semantic-validation`

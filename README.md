# SysML v2 Semantic Validator

ANTLR-based semantic validator for SysML v2 and KerML with intelligent error reporting and suggestions.

## Features

### Current (v0.1.0-SNAPSHOT)

- Maven multi-module project structure
- Native ANTLR 4.13.2 runtime integration (Standard Library loading supported via AST caching)
- Semantic validation engine (Type matching, resolving scopes, dependencies)
- Value/Unit correctness and completeness checking (LINT020-LINT022)
- Multi-layer intelligent error reporting (Spelling, unreferenced elements, etc.)

### Planned Features

#### Layer 1: Syntax Validation

- Full ANTLR-based parsing
- Complete grammar coverage
- Parse tree generation

#### Layer 2: Semantic Validation

- Symbol table and scope management
- Type system with inference
- Reference resolution
- Feature validation (redefinition, visibility, multiplicity)
- Constraint validation
- Cross-file dependency checking

#### Layer 3: Intelligent Error Reporting

- Spelling suggestions (Levenshtein distance)
- Import recommendations with standard library index
- Type error explanations
- Color-coded output with caret pointers
- Multi-line context display

#### KerML Support

- Unified SysML v2 / KerML validation
- Cross-language validation
- Shared symbol tables

#### Build Integration

- Maven plugin
- Gradle plugin
- CI/CD support (GitHub Actions)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Validator CLI                           │
│                   (validator-cli)                           │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Validator Core                            │
│                  (validator-core)                           │
├─────────────────────────────────────────────────────────────┤
│  Layer 3: Error Reporting & Suggestions                    │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  │
│  │   Spelling    │  │    Import     │  │     Type      │  │
│  │  Suggestions  │  │  Suggestions  │  │  Explainer    │  │
│  └───────────────┘  └───────────────┘  └───────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  Layer 2: Semantic Validation                              │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  │
│  │    Symbol     │  │     Type      │  │  Reference    │  │
│  │     Table     │  │    System     │  │  Resolution   │  │
│  └───────────────┘  └───────────────┘  └───────────────┘  │
│  ┌───────────────┐  ┌───────────────┐                     │
│  │   Feature     │  │  Constraint   │                     │
│  │  Validation   │  │  Validation   │                     │
│  └───────────────┘  └───────────────┘                     │
├─────────────────────────────────────────────────────────────┤
│  Layer 1: Syntax Parsing                                   │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  │
│  │     ANTLR     │  │   Parse Tree  │  │      AST      │  │
│  │    Parser     │  │   Visitors    │  │  Representation│ │
│  └───────────────┘  └───────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Project Structure

```
sysml-validator/
├── validator-core/              # Core validation library
│   ├── src/main/java/com/validator/
│   │   ├── parser/             # ANTLR integration
│   │   ├── ast/                # AST representation
│   │   ├── symbols/            # Symbol table
│   │   ├── types/              # Type system
│   │   ├── semantic/           # Semantic validators
│   │   ├── suggestions/        # Error suggestions
│   │   └── reporting/          # Error formatting
│   └── pom.xml
├── validator-cli/              # CLI tool
│   ├── src/main/java/com/validator/cli/
│   └── pom.xml
├── validator-plugins/          # Maven/Gradle plugins (planned)
├── docs/                       # Documentation
├── test-suite/                 # Test files
│   ├── positive/              # Valid SysML v2 files
│   └── negative/              # Invalid SysML v2 files
├── pom.xml                     # Parent POM
└── README.md
```

## Building

Requires:

- Java 11+
- Maven 3.8+

```bash
# Build all modules
mvn clean install

# Build only core library
mvn clean install -pl validator-core

# Build CLI tool
mvn clean install -pl validator-cli

# Run tests
mvn test
```

## Usage (Planned)

```bash
# Validate single file
java -jar validator-cli/target/sysml-validator.jar validate BreakfastExample.sysml

# Validate multiple files
java -jar validator-cli/target/sysml-validator.jar validate *.sysml

# Validate with suggestions
java -jar validator-cli/target/sysml-validator.jar validate --suggestions BreakfastExample.sysml

# Validate directory recursively
java -jar validator-cli/target/sysml-validator.jar validate --recursive src/

# Output JSON report
java -jar validator-cli/target/sysml-validator.jar validate --format json --output report.json *.sysml
```

## Development Roadmap

### Phase 1: Parser + Symbol Table (Completed)

- [x] Project structure
- [x] Maven setup with ANTLR dependencies
- [x] Custom ANTLR parsing of SysMLv2 and robust Standard Library loading
- [x] Build AST visitor framework
- [x] Implement symbol table and scope management
- [x] Create basic CLI interface
- [x] Write 50+ initial test cases

### Phase 2: Semantic Validation (Completed)

- [x] Implement type system
- [x] Build reference resolution engine
- [x] Add feature validation (redefinition, visibility)
- [x] Implement multiplicity and constraint validation
- [x] Write 100+ semantic test cases

### Phase 3: Smart Error Reporting (Completed)

- [x] Build spelling suggestion system
- [x] Create import suggestion engine
- [x] Implement type error explainer
- [x] Enhanced error formatting (with specification references e.g. LINT020)
- [x] Common Correctness and Completeness Checks (Unit mismatches, missing values)

#### Correctness and Completeness Patterns Identified

1. **Value/Unit Compatibility**: Warning when typed values don't match provided units (e.g., `LengthValue = 4[kg]`).
2. **Missing Units**: Warning when dimensional types are given raw scalars.
3. **Missing Typing**: Warning when generic types are given specific units.
4. **Conflicting Multiplicities**: E.g., a part usage `[1..3]` redefined to `[4..5]`.
5. **Circular Redefinitions/Subsettings**: Element A redefines B, and B redefines A.
6. **Invalid Feature Directions**: Connecting `out` to `in` with incompatible payloads.
7. **Action/State Parameter Mismatches**: Invoking an action with parameters that don't match the target `action def`.
8. **Dangling/Unsatisfied Requirements**: `satisfy` pointing to non-requirements, or instantiated requirements lacking `satisfy` relations.
9. **Disconnected Ports**: Interfaces modeled but never connected.
10. **State Machines without Transitions**: States with no valid pathways.
11. **Missing Allocations**: Behavioral actions with no structural component allocated to execute them.

### Phase 4: HAMR Integration & AADL (In Progress)

- [ ] Integrate Sireum HAMR models and architecture.
- [ ] Implement robust AADL code generation.
- [ ] Map SysMLv2 structural components to AADL systems.

### Phase 4: KerML Support (Weeks 11-13)

- [ ] Integrate KerML validator
- [ ] Build unified validation engine
- [ ] Cross-language validation
- [ ] Write 50+ KerML test cases

### Phase 5: Build Integration (Weeks 14-15)

- [ ] Create Maven plugin
- [ ] Create Gradle plugin
- [ ] Setup GitHub Actions CI/CD
- [ ] Release packaging

## Dependencies

### Core Libraries

- ANTLR 4.13.2 - Parser generator and runtime
- Sireum HAMR - SysML v2 ANTLR parser
- Apache Commons Text 1.14.0 - Spelling suggestions (Levenshtein distance)
- Jackson 2.15.3 - JSON processing
- SLF4J 2.0.9 - Logging

### CLI

- Picocli 4.7.5 - Command-line interface framework

### Testing

- JUnit 5.10.1 - Unit testing

## Contributing

This project is in active development.

## License

MIT License

## Related Projects

- [SysML v2 Release](https://github.com/Systems-Modeling/SysML-v2-Release) - Official OMG SysML v2 repository
- [Sireum](https://github.com/sireum/kekinian) - Sireum verification framework with SysML v2 parser
- Pattern-based validator at `E:\_Documents\git\Claude4v2\sysml-validator` - Lightweight validator detecting specific syntax errors

## Status

**Current Version**: 0.1.0-SNAPSHOT
**Status**: Active Development - Phase 4 (HAMR Integration / AADL) in progress
**Last Updated**: May 2026

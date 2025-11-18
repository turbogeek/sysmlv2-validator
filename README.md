# SysML v2 Semantic Validator

ANTLR-based semantic validator for SysML v2 and KerML with intelligent error reporting and suggestions.

## Features

### Current (v0.1.0-SNAPSHOT)
- Maven multi-module project structure
- ANTLR 4.13.2 runtime integration
- Sireum HAMR parser integration (planned)
- Multi-layer validation architecture (planned)

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

### Phase 1: Parser + Symbol Table (Weeks 1-3)
- [x] Project structure
- [x] Maven setup with ANTLR dependencies
- [ ] Integrate Sireum HAMR ANTLR parser
- [ ] Build AST visitor framework
- [ ] Implement symbol table and scope management
- [ ] Create basic CLI interface
- [ ] Write 50+ initial test cases

### Phase 2: Semantic Validation (Weeks 4-7)
- [ ] Implement type system
- [ ] Build reference resolution engine
- [ ] Add feature validation (redefinition, visibility)
- [ ] Implement multiplicity and constraint validation
- [ ] Write 100+ semantic test cases

### Phase 3: Smart Error Reporting (Weeks 8-10)
- [ ] Build spelling suggestion system
- [ ] Create import suggestion engine
- [ ] Implement type error explainer
- [ ] Add enhanced error formatting
- [ ] Write error reporting test cases

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

This is currently in early development (v0.1.0-SNAPSHOT). Contributions welcome once Phase 1 is complete.

## License

TBD

## Related Projects

- [SysML v2 Release](https://github.com/Systems-Modeling/SysML-v2-Release) - Official OMG SysML v2 repository
- [Sireum](https://github.com/sireum/kekinian) - Sireum verification framework with SysML v2 parser
- Pattern-based validator at `E:\_Documents\git\Claude4v2\sysml-validator` - Lightweight validator detecting specific syntax errors

## Status

**Current Version**: 0.1.0-SNAPSHOT
**Status**: Early Development - Phase 1 in progress
**Last Updated**: November 18, 2025

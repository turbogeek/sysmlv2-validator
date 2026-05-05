# Getting Started with SysML v2 Semantic Validator

## Quick Start

### What We Have Now ✅

Successfully created a new GitHub repository at `E:\_Documents\git\sysml-validator` with complete project structure for an ANTLR-based semantic validator.

**Status**: Phase 1 - Initial setup complete, ready for parser integration

### Repository Structure

```
E:\_Documents\git\sysml-validator/
├── README.md                   # Full project documentation
├── PROJECT_STATUS.md           # Detailed status and next steps
├── pom.xml                     # Parent Maven POM
├── validator-core/             # Core validation library
│   ├── pom.xml
│   └── src/main/java/com/validator/
│       ├── Validator.java              # Main interface
│       ├── ValidationResult.java       # Results container
│       ├── ValidationError.java        # Errors with suggestions
│       └── ValidationWarning.java      # Warnings
└── validator-cli/              # CLI tool
    ├── pom.xml
    └── src/main/java/com/validator/cli/
        └── ValidatorCLI.java           # Command-line interface
```

### What's Configured

#### Dependencies Ready
- ✅ ANTLR 4.13.2 runtime
- ✅ Sireum HAMR parser (via JitPack)
- ✅ Apache Commons Text (spelling suggestions)
- ✅ Jackson (JSON output)
- ✅ Picocli (CLI framework)
- ✅ JUnit 5 (testing)

#### Core Classes Created
- ✅ `Validator` - Main validation interface
- ✅ `ValidationResult` - Holds errors, warnings, metadata
- ✅ `ValidationError` - Error with location, message, suggestions (Builder pattern)
- ✅ `ValidationWarning` - Warning subclass
- ✅ `ValidatorCLI` - Command-line interface (placeholder)

### Git Status

**Repository**: Initialized with 2 commits
- `dd7c4db` - Initial project structure
- `2b43163` - Added PROJECT_STATUS.md

**Branch**: master

---

## Next Steps

### 1. Verify Maven Setup

```bash
cd E:\_Documents\git\sysml-validator
mvn --version
mvn clean compile
```

If Maven is not installed, you can:
- Download from https://maven.apache.org/download.cgi
- Use an IDE with Maven support (IntelliJ IDEA, Eclipse, VS Code)
- Add Maven wrapper: `mvn -N wrapper:wrapper`

### 2. Begin Parser Integration (Next Task)

The next phase is integrating the Sireum HAMR ANTLR parser:

1. Research Sireum HAMR parser API
2. Create parser wrapper in `validator-core/src/main/java/com/validator/parser/`
3. Implement file parsing
4. Generate AST from parse tree
5. Write initial tests

### 3. Import Test Suite

Copy existing test files to `test-suite/`:

```bash
# Negative tests (55 files)
cp E:\_Documents\git\Claude4v2\test-cases\negative\*.sysml test-suite\negative\

# Positive tests (11 files)
cp E:\_Documents\git\Claude4v2\test-cases\positive\*.sysml test-suite\positive\

# Official examples can be added as needed
```

---

## Project Goals

### Layer 1: Syntax Validation (Phase 1 - Weeks 1-3)
- Full ANTLR-based parsing
- Complete grammar coverage
- Parse tree generation and AST

### Layer 2: Semantic Validation (Phase 2 - Weeks 4-7)
- Symbol table and scope management
- Type system with inference
- Reference resolution
- Feature validation (redefinition, visibility)
- Constraint validation

### Layer 3: Intelligent Error Reporting (Phase 3 - Weeks 8-10)
- Spelling suggestions (Levenshtein distance)
- Import recommendations
- Type error explanations
- Color-coded output with caret pointers

### KerML Support (Phase 4 - Weeks 11-13)
- Unified SysML v2 / KerML validation
- Cross-language validation

### Build Integration (Phase 5 - Weeks 14-15)
- Maven and Gradle plugins
- CI/CD with GitHub Actions

---

## Usage (Planned)

Once complete, the validator will work like this:

```bash
# Validate single file
java -jar validator-cli/target/sysml-validator.jar validate BreakfastExample.sysml

# Validate with suggestions
java -jar validator-cli/target/sysml-validator.jar validate --suggestions BreakfastExample.sysml

# Validate directory recursively
java -jar validator-cli/target/sysml-validator.jar validate --recursive src/

# Output JSON report
java -jar validator-cli/target/sysml-validator.jar validate --format json --output report.json *.sysml
```

### Example Error Output (Planned)

```
[ERROR] BreakfastExample.sysml:15:5
  UNDEFINED_TYPE: Type 'Breafast' is not defined

  13 | package Breakfast {
  14 |     part def Meal {
  15 |         part breakfast : Breafast;
     |                          ^^^^^^^^ undefined type
  16 |     }
  17 | }

  Suggestions:
    - Did you mean 'Breakfast'? (in package Breakfast)
    - Add import: public import Breakfast::*;
```

---

## Comparison: Pattern-Based vs Semantic Validator

### Pattern-Based Validator (Current)
**Location**: `E:\_Documents\git\Claude4v2\sysml-validator\SysMLv2Validator.java`

✅ **Pros**:
- Very fast (milliseconds)
- No dependencies
- 100% accuracy for targeted patterns
- Great for quick checks

❌ **Cons**:
- Only ~5-10% language coverage
- Detects only 4 specific invalid patterns
- No semantic validation
- No type checking

### Semantic Validator (This Project)
**Location**: `E:\_Documents\git\sysml-validator`

✅ **Pros**:
- 95-100% language coverage (target)
- Full semantic validation
- Type checking and inference
- Intelligent error suggestions
- Professional-grade validation

❌ **Cons**:
- More complex
- Requires dependencies (ANTLR, etc.)
- Slower (but still fast enough)
- Longer development time

**Recommendation**: Use both!
- Pattern validator for quick pre-checks
- Semantic validator for production builds

---

## Documentation Files

### In This Repository
- `README.md` - Project overview, architecture, roadmap
- `PROJECT_STATUS.md` - Detailed status, next steps, design decisions
- `GETTING_STARTED.md` - This file (quick start guide)

### Related Documentation
- `E:\_Documents\git\Claude4v2\VALIDATOR_LANGUAGE_COVERAGE_ANALYSIS.md` - Pattern validator coverage analysis
- `E:\_Documents\git\Claude4v2\OFFICIAL_SYSML_V2_RELEASE_VALIDATION_RESULTS.md` - Validation results from official repo
- `E:\_Documents\git\Claude4v2\SYSMLV2_VALIDATOR_README.md` - Pattern validator documentation

---

## Resources

### Official SysML v2
- Repository: https://github.com/Systems-Modeling/SysML-v2-Release
- Local clone: `E:\_Documents\git\SysML-v2-Release`
- Examples: `E:\_Documents\git\SysML-v2-Release\sysml\src\examples`

### Test Files
- Pattern validator tests: `E:\_Documents\git\Claude4v2\test-cases\`
  - 55 negative tests
  - 11 positive tests
- Cameo samples: `E:\_Documents\_SysMLV2\26xHF1 Samples\` (14 files)
- Official examples: `E:\_Documents\git\SysML-v2-Release\sysml\src\` (100+ files)

### Parsers
- Sireum HAMR: https://github.com/sireum/kekinian
- Official SysML v2 grammar: https://github.com/Systems-Modeling/SysML-v2-Release/tree/master/org.omg.sysml.xtext

---

## Development Workflow

### 1. Make Changes
Edit files in `validator-core/` or `validator-cli/`

### 2. Build
```bash
mvn clean compile
```

### 3. Run Tests
```bash
mvn test
```

### 4. Run CLI
```bash
mvn package
java -jar validator-cli/target/sysml-validator.jar [options] files...
```

### 5. Commit
```bash
git add .
git commit -m "Description of changes"
```

---

## Current Status

**Phase**: 1 - Parser Integration
**Week**: 1
**Completed**:
- ✅ Repository structure
- ✅ Maven configuration
- ✅ Core classes
- ✅ CLI placeholder
- ✅ Documentation

**Next**: Integrate Sireum HAMR ANTLR parser

**Timeline to MVP**: 7 weeks (Phases 1-2)
**Timeline to Production**: 10 weeks (Phases 1-3)
**Timeline to Complete**: 14-15 weeks (All phases)

---

## Questions?

See:
- `README.md` for architecture and full roadmap
- `PROJECT_STATUS.md` for detailed status and design decisions
- Pattern validator at `E:\_Documents\git\Claude4v2\sysml-validator\` for current working validator

---

**Created**: November 18, 2025
**Status**: ✅ Ready for development

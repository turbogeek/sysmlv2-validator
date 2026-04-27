# SysML v2 Validator - Architecture Guide

## System Overview

The SysML v2 Validator is a multi-phase validation system for SysML v2 and KerML models. It provides syntax validation, semantic analysis, and lint-style quality checks.

```
                    +------------------+
                    |   Source File    |
                    +--------+---------+
                             |
                    +--------v---------+
                    |  ANTLR4 Parser   |
                    | (SysMLv2Parser)  |
                    +--------+---------+
                             |
                    +--------v---------+
                    |    Parse Tree    |
                    +--------+---------+
                             |
          +------------------+------------------+
          |                  |                  |
+---------v--------+ +-------v--------+ +-------v--------+
| Symbol Table     | | Import         | | Lint           |
| Builder          | | Resolver       | | Analyzer       |
+--------+---------+ +-------+--------+ +-------+--------+
         |                   |                  |
         +-------------------+------------------+
                             |
                    +--------v---------+
                    | Validation       |
                    | Result           |
                    +------------------+
```

## Module Structure

```
sysml-validator/
├── validator-core/                 # Core validation library
│   └── src/main/java/com/validator/
│       ├── SysMLv2ValidatorImpl.java    # Main validator
│       ├── ValidationError.java         # Error representation
│       ├── ValidationResult.java        # Result container
│       ├── ErrorCodes.java              # Error code constants
│       ├── parser/                      # ANTLR parser facade
│       │   └── SysMLv2ParserFacade.java
│       ├── semantic/                    # Semantic analysis
│       │   ├── Symbol.java
│       │   ├── SymbolTable.java
│       │   ├── SymbolTableBuilder.java
│       │   ├── Scope.java
│       │   ├── ImportResolver.java
│       │   └── SemanticValidator.java
│       ├── lint/                        # Lint analysis
│       │   ├── LintAnalyzer.java
│       │   ├── LintRule.java
│       │   ├── LintConfig.java
│       │   └── LintContext.java
│       ├── constraint/                  # Constraint validation
│       │   ├── ConstraintAnalyzer.java
│       │   └── ExpressionTypeChecker.java
│       ├── suggestions/                 # Spelling suggestions
│       │   ├── SpellingSuggestionEngine.java
│       │   └── UserDictionary.java
│       └── library/                     # Standard library
│           ├── LibraryIndex.java
│           └── StandardLibraryManager.java
└── validator-cli/                  # Command line interface
```

## Core Components

### 1. Parser Layer

**SysMLv2ParserFacade** (`parser/SysMLv2ParserFacade.java`)

Wraps ANTLR4-generated lexer and parser:

```java
public class SysMLv2ParserFacade {
    public ParseResult parseFile(File file);
    public ParseResult parseString(String source, String fileName);

    public static class ParseResult {
        ParseTree getParseTree();
        List<SyntaxError> getSyntaxErrors();
        List<LineComment> getLineComments();
        boolean hasErrors();
    }
}
```

**Key Features**:
- Collects syntax errors with line/column info
- Provides spelling suggestions for typos
- Detects single-line comments (warning: not persisted)

### 2. Symbol Table

**Symbol** (`semantic/Symbol.java`)

Represents a named model element:

```java
public class Symbol {
    String name;
    String qualifiedName;
    ElementType type;
    Location location;
    Visibility visibility;

    // Reference tracking
    List<Location> references;
    boolean usedAsType;
    boolean usedInExpression;

    // Relationships
    List<Symbol> specializations;   // :>
    List<Symbol> redefinitions;     // :>>
}
```

**SymbolTable** (`semantic/SymbolTable.java`)

Manages symbols with scope hierarchy:

```java
public class SymbolTable {
    void enterScope(String name, ScopeType type);
    void exitScope();
    void define(Symbol symbol);
    Symbol resolve(String name);
    Symbol resolveQualified(String qualifiedName);
}
```

**SymbolTableBuilder** (`semantic/SymbolTableBuilder.java`)

ANTLR listener that builds symbol table from parse tree:

```java
public class SymbolTableBuilder extends SysMLv2ParserBaseListener {
    public static SymbolTable build(ParseTree tree, String filePath);

    @Override
    public void enterPackageDeclaration(PackageDeclarationContext ctx);
    @Override
    public void enterPartDefinition(PartDefinitionContext ctx);
    // ... etc for all definition types
}
```

### 3. Import Resolution

**ImportResolver** (`semantic/ImportResolver.java`)

Resolves import statements against standard library and model:

```java
public class ImportResolver {
    void resolveAllImports();
    ImportResolutionStats getStats();
}
```

**Supported Import Types**:
- `import ISQ::*` - Wildcard
- `import SI::kg` - Specific
- `import Pkg::{A, B}` - Filtered
- `import Pkg::Elem as Alias` - Aliased

### 4. Lint Analysis

**LintAnalyzer** (`lint/LintAnalyzer.java`)

Orchestrates lint rules:

```java
public class LintAnalyzer {
    void registerRule(LintRule rule);
    List<ValidationWarning> analyze(ParseTree tree, SymbolTable symbols);
}
```

**LintRule** (`lint/LintRule.java`)

Interface for lint checks:

```java
public interface LintRule {
    String getRuleId();
    String getCategory();
    List<ValidationWarning> analyze(LintContext context);
}
```

**LintConfig** (`lint/LintConfig.java`)

Configuration with cascading precedence:

1. CLI overrides (highest)
2. Project config (`.sysml-lint.json`)
3. User config (`~/.sysml-validator/.sysml-lint.json`)
4. Defaults (lowest)

### 5. Constraint Validation

**ConstraintAnalyzer** (`constraint/ConstraintAnalyzer.java`)

Validates OCL-like constraint expressions:

- Variable scope checking
- Type inference and checking
- Operator compatibility
- Unit compatibility

### 6. Spelling Suggestions

**SpellingSuggestionEngine** (`suggestions/SpellingSuggestionEngine.java`)

Provides spelling suggestions using Levenshtein distance:

```java
public class SpellingSuggestionEngine {
    List<SuggestionResult> suggest(String input, int maxSuggestions);
}
```

**Suggestion Sources** (priority order):
1. User model symbols
2. KPAR library symbols
3. Cameo/MagicDraw symbols
4. Standard library types
5. Language keywords

## Validation Flow

### Phase 1: Parsing

```java
SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
ParseResult result = parser.parseFile(file);

if (result.hasErrors()) {
    // Collect syntax errors with suggestions
    for (SyntaxError error : result.getSyntaxErrors()) {
        errors.add(convertToValidationError(error));
    }
}
```

### Phase 2: Symbol Table Construction

```java
ParseTree tree = result.getParseTree();
SymbolTable symbolTable = SymbolTableBuilder.build(tree, filePath);
```

### Phase 3: Import Resolution

```java
ImportResolver resolver = new ImportResolver(symbolTable, standardLibrary);
resolver.resolveAllImports();
```

### Phase 4: Semantic Validation

```java
SemanticValidator semanticValidator = new SemanticValidator(symbolTable);
List<ValidationError> semanticErrors = semanticValidator.validate(tree);
```

### Phase 5: Lint Analysis

```java
LintAnalyzer lintAnalyzer = LintAnalyzer.withDefaultRules(lintConfig);
List<ValidationWarning> warnings = lintAnalyzer.analyze(tree, symbolTable);
```

### Phase 6: Result Aggregation

```java
ValidationResult result = new ValidationResult.Builder()
    .addErrors(syntaxErrors)
    .addErrors(semanticErrors)
    .addWarnings(warnings)
    .build();
```

## Error Handling

### Error Hierarchy

```
ValidationError (abstract)
├── SyntaxError
├── ImportError
├── SemanticError
├── TypeError
└── ValidationWarning (subclass with WARNING severity)
```

### Error Code Categories

| Prefix | Category | Fatal |
|--------|----------|-------|
| SYNTAX_ | Parsing errors | Yes |
| IMPORT_ | Import resolution | No |
| SEMANTIC_ | Symbol resolution | No |
| TYPE_ | Type checking | No |
| IO_ | File operations | Yes |
| LINT* | Code quality | No |
| CONST* | Constraint validation | No |

## Extension Points

### Adding New Lint Rules

1. Implement `LintRule` interface:

```java
public class MyRule implements LintRule {
    @Override
    public String getRuleId() { return "my-rule"; }

    @Override
    public String getCategory() { return "custom"; }

    @Override
    public List<ValidationWarning> analyze(LintContext ctx) {
        // Analyze ctx.getSymbolTable() and ctx.getParseTree()
        // Return list of warnings
    }
}
```

2. Register with analyzer:

```java
lintAnalyzer.registerRule(new MyRule());
```

### Adding Suggestion Providers

1. Implement `SuggestionProvider`:

```java
public class MyProvider implements SuggestionProvider {
    @Override
    public String getName() { return "custom"; }

    @Override
    public Collection<String> getSymbols() {
        return mySymbols;
    }
}
```

2. Add to suggestion engine:

```java
suggestionEngine.addProvider(new MyProvider());
```

## Performance Considerations

1. **Lazy parsing**: Only parse when needed
2. **Symbol table caching**: Reuse for same file
3. **Incremental updates**: Track changes for re-validation
4. **Parallel validation**: Multiple files in parallel

## Testing Strategy

### Unit Tests
- Parser tests for grammar coverage
- Symbol table tests for scope handling
- Import resolver tests
- Lint rule tests

### Integration Tests
- Full validation pipeline tests
- Real-world model tests (StockTicker, CheatSheet)
- Standard library tests

### Test Suites
- `test-suite/positive/` - Valid models (should pass)
- `test-suite/negative/` - Invalid models (should fail with specific errors)
- `test-suite/stockticker/` - Real-world complex models

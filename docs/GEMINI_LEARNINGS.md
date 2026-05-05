# SysMLv2 Validator - Gemini Learnings & Project Guidelines

This file captures the operational knowledge, build commands, and architectural insights for the SysMLv2 Semantic Validator project, transitioning the context previously stored in `.claude/settings.local.json` to be accessible for Gemini.

## 1. Build and Test Commands

The project uses a portable Maven wrapper (`build-with-portable-maven.cmd`) for consistent execution across environments.

**Common Commands:**
- Clean and Compile: `./build-with-portable-maven.cmd clean compile`
- Clean and Test: `./build-with-portable-maven.cmd clean test`
- Compile and Test: `./build-with-portable-maven.cmd compile test`
- Compile and Test (Skip Checkstyle): `./build-with-portable-maven.cmd compile test -DskipCheckstyle=true`
- Run Specific Test: `./build-with-portable-maven.cmd test -Dtest=SymbolTest,ScopeTest,SymbolTableTest,ImportStatementTest -DskipCheckstyle=true`
- Run CLI on example: `mvn -q exec:java -pl validator-core -Dexec.mainClass="com.validator.Main" -Dexec.args="test-suite/CheatSheet/pastableExamples/Actions_1ActionDefinition.sysml"`

## 2. Architectural Insights

### Lexer and Comments
- **Line Comments (`//`)**: SysML v2 single-line comments are not persisted to the model. The lexer routes `LINE_COMMENT` tokens to hidden channel 1 instead of skipping them. This allows the parser facade and validator to detect them and generate warnings encouraging users to use block comments (`/* */`) or the `doc` keyword.
- **Location Validation**: The `StandardLibraryManager` uses line=1 (not 0) to satisfy Location validation constraints.

### Semantic Validation Components (Phase 2)
The project's Phase 2 infrastructure introduced several key semantic analysis components:
- **ModelIndexer**: For cross-file model indexing (built with Apache Lucene).
- **KparReader**: To read `.kpar` library archives.
- **ImportResolver, ImportStatement, ImportType**: For handling `import` statements.
- **Symbol, SymbolTable, SymbolTableBuilder**: For robust symbol management.
- **Scope, ScopeType, Visibility**: For enforcing scoping rules.
- **ElementType**: An enumeration of SysML v2 element types.
- **KparLibraryProvider, StandardLibraryProvider**: For providing library suggestions.
- **SpellingSuggestionEngine**: For intelligent typo correction.

## 3. Development Workflow

- **Grammar Updates**: When updating `SysMLv2Lexer.g4` or `SysMLv2Parser.g4`, the ANTLR Maven plugin will automatically regenerate the lexer/parser classes during the `compile` phase.
- **Bug Fixes**: Focus on the issues outlined in `TODO.md` (whitespace concatenation, sequence action syntax, standard library resolution). Always write unit tests to verify fixes.

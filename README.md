# SysMLv2 Validator

This project provides a robust validation and parsing engine for SysMLv2 and KerML.

## Features
- **ANTLR4 Grammar Support:** Up-to-date with SysMLv2 specifications.
- **Validation Engine:** Analyzes models for structural and semantic correctness.
- **Language Server Backend:** Core engine powering the VSCode Extension and LSP.

## Build Instructions
Requirements:
- Java 25 (JDK 25+)
- Maven

Run `mvn clean install` to build the parser and core libraries.

## IDE Setup (VS Code)
To develop and use this project within Visual Studio Code, it is recommended to install the following extensions:
- **vscjava** (Extension Pack for Java)
- **Red Hat** (Language Support for Java by Red Hat)

## Usage
The validator can be run from the CLI or integrated as a library in other tools, such as the `SysMLv2-Editor-for-VSCode`.

### Minimizing imports
A wildcard import (`private import ISQ::*;`) makes every member of the package visible, and ISQ, SI and other
libraries publicly import further packages, so a tool such as Cameo has to load all of them. A wildcard also lets a
name bind to an unintended element when the intended one is missing (for example `height` binding to
`ISQSpaceTime::height` when DiagramInterchange is not loaded).

`--minimize-imports` replaces each private wildcard import with element imports of the names the file uses
through it, and removes private wildcard imports that supply no name. It resolves every name in the files again
after the rewrite and writes nothing unless every name resolves to the same element. Public imports are kept,
because other models may rely on what they re-export. It runs only when validation finds no errors.

```
sysml-validator --minimize-imports [--import-from-owner] [--write] -r <folders or files>
```

- Without `--write`, it lists the changes.
- `--import-from-owner` imports each element from the package that owns it (`ISQBase::MassValue`) instead of
  through the re-export (`ISQ::MassValue`).
- Give all the files that refer to each other in one run: names are resolved across them.

## Comparing with the pilot implementation and Cameo
- [conformance/](conformance/README.md) checks that this validator, the OMG SysML v2 Pilot Implementation and
  Cameo / CATIA Magic give the same verdict on the test files of `test-suite` and `examples` and on KPARs, so that
  a file this validator accepts can be used in Cameo.
- [perf/](perf/README.md) times this validator and the pilot implementation on models of up to 65,000 lines.

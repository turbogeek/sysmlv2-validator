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

## Comparing with the pilot implementation and Cameo
- [conformance/](conformance/README.md) checks that this validator, the OMG SysML v2 Pilot Implementation and
  Cameo / CATIA Magic give the same verdict on the test files of `test-suite` and `examples` and on KPARs, so that
  a file this validator accepts can be used in Cameo.
- [perf/](perf/README.md) times this validator and the pilot implementation on models of up to 65,000 lines.

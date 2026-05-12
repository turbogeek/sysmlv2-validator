# SysMLv2 Validator

This project provides a robust validation and parsing engine for SysMLv2 and KerML.

## Features
- **ANTLR4 Grammar Support:** Up-to-date with SysMLv2 specifications.
- **Validation Engine:** Analyzes models for structural and semantic correctness.
- **Language Server Backend:** Core engine powering the VSCode Extension and LSP.

## Build Instructions
Requirements:
- Java 17+
- Maven

Run `mvn clean install` to build the parser and core libraries.

## IDE Setup (VS Code)
To develop and use this project within Visual Studio Code, it is recommended to install the following extensions:
- **vscjava** (Extension Pack for Java)
- **Red Hat** (Language Support for Java by Red Hat)

## Usage
The validator can be run from the CLI or integrated as a library in other tools, such as the `SysMLv2-Editor-for-VSCode`.

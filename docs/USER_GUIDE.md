# SysML v2 Validator - User Guide

## Overview

The SysML v2 Validator is a comprehensive validation tool for SysML v2 and KerML models. It provides syntax validation, semantic analysis, import resolution, and lint-style warnings to help you write better, more maintainable models.

## Quick Start

### Command Line Usage

```bash
# Validate a single file
java -jar sysml-validator.jar model.sysml

# Validate multiple files
java -jar sysml-validator.jar *.sysml

# Validate a directory
java -jar sysml-validator.jar src/models/

# Enable verbose output
java -jar sysml-validator.jar --verbose model.sysml
```

### API Usage

```java
import com.validator.SysMLv2Validator;
import com.validator.SysMLv2ValidatorImpl;
import com.validator.ValidationResult;

// Create validator
SysMLv2Validator validator = new SysMLv2ValidatorImpl();

// Validate a file
ValidationResult result = validator.validate(new File("model.sysml"));

// Check results
if (result.isSuccess()) {
    System.out.println("Validation passed!");
} else {
    for (ValidationError error : result.getErrors()) {
        System.out.println(error.getMessage());
    }
}
```

## Validation Features

### 1. Syntax Validation

The validator uses an ANTLR4-based parser that supports the full SysML v2 grammar:

- Package declarations
- Part, action, state, requirement definitions and usages
- Imports (wildcard, specific, filtered, aliased)
- Constraints and expressions
- Comments and documentation

### 2. Semantic Validation

Beyond syntax, the validator performs semantic analysis:

- **Symbol Resolution**: Verifies that all referenced names exist
- **Import Validation**: Checks that imports reference valid packages/elements
- **Type Checking**: Validates type compatibility in usages
- **Visibility**: Enforces public/private/protected access

### 3. Lint Warnings

The validator provides configurable lint-style warnings:

| Category | Description |
|----------|-------------|
| `unused` | Unused definitions and imports |
| `missing-types` | Missing type definitions |
| `naming` | Naming convention violations |
| `documentation` | Missing or incomplete documentation |
| `complexity` | Overly complex model structures |
| `best-practices` | Best practice violations |

## Configuration

### Configuration File

Create a `.sysml-lint.json` file in your project root:

```json
{
  "rules": {
    "unused": "warn",
    "missing-types": "warn",
    "naming": "warn",
    "documentation": "off",
    "complexity": "warn",
    "best-practices": "info"
  },
  "thresholds": {
    "maxDepth": 5,
    "maxPackageSize": 50,
    "maxParameters": 7
  },
  "dictionary": ".sysml-dictionary"
}
```

### Severity Levels

- `off` - Disable the rule
- `info` - Informational message only
- `warn` - Warning (does not fail validation)
- `error` - Error (fails validation)

### User Dictionary

Create a `.sysml-dictionary` file to add project-specific terms:

```
# Project-specific terms
VehicleECU
BrakeActuator
ThrottleController

# Acronyms
ECU
CAN
LIN
```

## Error Codes

### Syntax Errors (SYNTAX_*)

| Code | Description |
|------|-------------|
| SYNTAX_ERROR | Generic parsing error |
| SYNTAX_UNEXPECTED_TOKEN | Unexpected token in input |
| SYNTAX_MISSING_ELEMENT | Required element missing |

### Import Errors (IMPORT_*)

| Code | Description |
|------|-------------|
| IMPORT_PACKAGE_NOT_FOUND | Package not found for wildcard import |
| IMPORT_ELEMENT_NOT_FOUND | Element not found in package |
| IMPORT_ACCESS_DENIED | Element not accessible (visibility) |
| IMPORT_INVALID_SYNTAX | Invalid import syntax |

### Semantic Errors (SEMANTIC_*)

| Code | Description |
|------|-------------|
| SEMANTIC_DUPLICATE_SYMBOL | Duplicate name in same scope |
| SEMANTIC_UNRESOLVED_REFERENCE | Name not found |
| SEMANTIC_INVALID_SPECIALIZATION | Invalid :> relationship |
| SEMANTIC_CIRCULAR_DEPENDENCY | Circular dependency detected |

### Lint Warnings (LINT*)

| Code | Description |
|------|-------------|
| LINT001 | Unused definition |
| LINT002 | Unused import |
| LINT003 | Missing definition for usage |
| LINT004 | Untyped attribute |
| LINT005 | Definition not PascalCase |
| LINT006 | Usage not camelCase |
| LINT007 | Missing documentation |
| LINT008 | Deep nesting (>5 levels) |
| LINT009 | Empty definition |

### Constraint Errors (CONST*)

| Code | Description |
|------|-------------|
| CONST001 | Undefined variable in constraint |
| CONST002 | Type mismatch in expression |
| CONST003 | Constraint not boolean |
| CONST004 | Unreachable constraint |
| CONST005 | Invalid operator for types |
| CONST006 | Unit mismatch |
| CONST007 | Potential divide by zero |

## Examples

### Example 1: Valid Model

```sysml
package VehicleSystem {
    import ISQ::*;
    import SI::*;

    part def Vehicle {
        doc /* A vehicle with engine and transmission */

        attribute mass : MassValue;

        part engine : Engine;
        part transmission : Transmission;
    }

    part def Engine {
        attribute power : PowerValue;
    }

    part def Transmission {
        attribute gears : Integer;
    }
}
```

### Example 2: Model with Errors

```sysml
package BadExample {
    // Warning: Single-line comment not persisted

    part def vehicle {  // Warning: Should be PascalCase
        attribute mass;  // Warning: Missing type

        part engine : UndefinedType;  // Error: Unresolved reference
    }
}
```

## Troubleshooting

### Common Issues

1. **"Extraneous input" errors**: Check for missing semicolons or braces
2. **"Mismatched input" errors**: Verify keyword spelling
3. **Import not found**: Check package path and standard library configuration
4. **Type mismatch**: Ensure imported types match usage

### Getting Help

- Check the error message and suggestions
- Review the SysML v2 specification
- Consult the examples in `test-suite/positive/`

## License

Apache License 2.0

# CI/CD Setup and Quality Gates

## Overview

This document describes the continuous integration and deployment pipeline for the SysML v2 Validator, including code quality gates, testing requirements, and build automation.

## Quality Gates

All code must pass these automated quality gates before merging:

### 1. Unit Testing (JUnit 5)
- **Goal**: Verify individual components work correctly
- **Command**: `mvn test`
- **Requirements**:
  - All tests must pass
  - No flaky tests tolerated
  - Test execution time < 30 seconds

### 2. Code Coverage (JaCoCo 0.8.11)
- **Goal**: Ensure comprehensive test coverage
- **Command**: `mvn verify`
- **Requirements**:
  - **Line Coverage**: ≥ 95%
  - **Branch Coverage**: ≥ 90%
  - Report location: `target/site/jacoco/index.html`

**Example Output**:
```
[INFO] --- jacoco-maven-plugin:0.8.11:check (check) @ validator-core ---
[INFO] Loading execution data file: target/jacoco.exec
[INFO] Analyzed bundle 'SysML v2 Validator - Core Library' with 42 classes
[INFO] All coverage checks passed successfully.
```

### 3. Mutation Testing (PITest 1.15.3)
- **Goal**: Validate test effectiveness (tests that kill mutants)
- **Command**: `mvn org.pitest:pitest-maven:mutationCoverage`
- **Requirements**:
  - **Mutation Score**: ≥ 80%
  - **Line Coverage**: ≥ 90%
  - Report location: `target/pit-reports/index.html`

**What PITest Does**:
- Introduces small code mutations (changes)
- Runs tests against mutated code
- If tests fail, mutation is "killed" (good)
- If tests pass, mutation "survived" (bad - test gap)

**Example Mutators**:
- Conditional boundary mutations: `<` → `<=`
- Negate conditionals: `if (x)` → `if (!x)`
- Math mutations: `+` → `-`
- Return value mutations: `return true` → `return false`

### 4. Static Analysis (SpotBugs 4.8.3)
- **Goal**: Detect potential bugs and code smells
- **Command**: `mvn spotbugs:check`
- **Requirements**:
  - Zero HIGH or MEDIUM severity bugs
  - All warnings must be reviewed
  - Report location: `target/spotbugs.html`

**Bug Categories Detected**:
- Null pointer dereferences
- Resource leaks (unclosed streams)
- Incorrect synchronization
- Bad practice (equals without hashCode)
- Performance issues
- Security vulnerabilities

### 5. Code Style (Checkstyle 3.3.1)
- **Goal**: Enforce consistent code style
- **Command**: `mvn checkstyle:check`
- **Requirements**:
  - Zero checkstyle violations
  - All public APIs must have Javadoc
  - Max line length: 120 characters
  - Max method length: 150 lines

**Style Rules Enforced**:
- Naming conventions (camelCase, PascalCase)
- Whitespace and indentation
- Import order and no wildcards
- Javadoc presence for public methods
- No unused imports/variables

### 6. Security Check (OWASP Dependency-Check 9.0.9)
- **Goal**: Identify known CVEs in dependencies
- **Command**: `mvn org.owasp:dependency-check-maven:check`
- **Requirements**:
  - Zero HIGH or CRITICAL CVEs (CVSS ≥ 7.0)
  - All MEDIUM CVEs must be documented/accepted
  - Report location: `target/dependency-check-report.html`

## Build Pipeline

### Local Development Build
```bash
# Quick build with tests
mvn clean verify

# Full quality check (includes mutation testing)
mvn clean verify org.pitest:pitest-maven:mutationCoverage

# Security audit (run weekly)
mvn org.owasp:dependency-check-maven:check -Dskip=false
```

### CI/CD Build Stages

#### Stage 1: Compile and Unit Test (< 2 minutes)
```bash
mvn clean compile
mvn test
```

**Gates**:
- Compilation succeeds
- All unit tests pass
- Test execution < 30 seconds

#### Stage 2: Code Quality (< 3 minutes)
```bash
mvn verify
mvn checkstyle:check
mvn spotbugs:check
```

**Gates**:
- JaCoCo coverage ≥ 95%/90%
- Zero checkstyle violations
- Zero spotbugs issues

#### Stage 3: Mutation Testing (< 5 minutes)
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

**Gates**:
- Mutation score ≥ 80%
- No survived mutations in critical paths

#### Stage 4: Integration Tests (< 2 minutes)
```bash
mvn verify -Pintegration-tests
```

**Gates**:
- All integration tests pass
- Real SysML files validate correctly

#### Stage 5: Package and Deploy
```bash
mvn package
mvn deploy -Prelease
```

**Artifacts**:
- `validator-core-0.1.0-SNAPSHOT.jar`
- `validator-cli-0.1.0-SNAPSHOT-standalone.jar`

## Quality Metrics Dashboard

### Target Metrics (Production Quality)
| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Line Coverage | ≥ 95% | 85%* | 🔄 In Progress |
| Branch Coverage | ≥ 90% | 75%* | 🔄 In Progress |
| Mutation Score | ≥ 80% | TBD | 🔄 Pending |
| SpotBugs Issues | 0 | 0 | ✅ Pass |
| Checkstyle Violations | 0 | TBD | 🔄 Pending |
| Security CVEs | 0 HIGH | 0 | ✅ Pass |

*Current estimates based on existing tests (41). Target will be achieved with new unit tests (42 additional).

## Error Handling Standards

All code must follow these error handling patterns to ensure "no exceptions are lost and unreported":

### 1. Error Collection Pattern
```java
public class MyBuilder {
    private final List<String> errors = new ArrayList<>();

    public void process() {
        try {
            // Processing logic
        } catch (Exception e) {
            logger.error("Processing failed", e);
            errors.add(formatError(e));
            // Continue processing - don't throw
        }
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
```

### 2. Try-Finally for Scope Management
```java
public void processElement() {
    symbolTable.enterScope("ElementName", ScopeType.PART_DEFINITION);
    try {
        // Process children
        visitChildren(ctx);
    } finally {
        // ALWAYS exit scope, even on exception
        symbolTable.exitScope();
    }
}
```

### 3. Null Checks at Construction
```java
public Symbol(String name, String qualifiedName, ElementType type) {
    this.name = Objects.requireNonNull(name, "Symbol name cannot be null");
    this.qualifiedName = Objects.requireNonNull(qualifiedName,
        "Qualified name cannot be null");
    this.type = Objects.requireNonNull(type, "Element type cannot be null");
}
```

### 4. Logging All Exceptions
```java
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

public void operation() {
    try {
        // Operation
    } catch (IOException e) {
        logger.error("I/O error during operation", e);
        throw new ValidationException("Failed to read file", e);
    } catch (Exception e) {
        logger.error("Unexpected error", e);
        throw new RuntimeException("Operation failed", e);
    }
}
```

## Continuous Integration Configuration

### GitHub Actions (Recommended)
```yaml
name: CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'

    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

    - name: Build and Test
      run: mvn clean verify

    - name: Mutation Testing
      run: mvn org.pitest:pitest-maven:mutationCoverage

    - name: Upload Coverage Reports
      uses: codecov/codecov-action@v3
      with:
        files: ./target/site/jacoco/jacoco.xml

    - name: Upload PITest Report
      uses: actions/upload-artifact@v3
      with:
        name: pitest-report
        path: target/pit-reports/
```

### Jenkins Pipeline
```groovy
pipeline {
    agent any

    tools {
        maven 'Maven 3.9'
        jdk 'JDK 11'
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Quality Gates') {
            parallel {
                stage('Coverage') {
                    steps {
                        sh 'mvn jacoco:check'
                    }
                }
                stage('Static Analysis') {
                    steps {
                        sh 'mvn spotbugs:check checkstyle:check'
                    }
                }
            }
        }

        stage('Mutation Testing') {
            steps {
                sh 'mvn org.pitest:pitest-maven:mutationCoverage'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
    }

    post {
        always {
            publishHTML([
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html',
                reportName: 'Coverage Report'
            ])
            publishHTML([
                reportDir: 'target/pit-reports',
                reportFiles: 'index.html',
                reportName: 'PITest Report'
            ])
        }
    }
}
```

## Local Pre-Commit Hooks

### Install Git Pre-Commit Hook
```bash
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
set -e

echo "Running pre-commit checks..."

# Quick compile and test
mvn clean test

# Check code style
mvn checkstyle:check

# Check for CVEs (quick)
# mvn org.owasp:dependency-check-maven:check -Dskip=false

echo "Pre-commit checks passed!"
EOF

chmod +x .git/hooks/pre-commit
```

## Troubleshooting

### Coverage Falling Below Threshold
```bash
# Generate detailed coverage report
mvn jacoco:report

# Open report
open target/site/jacoco/index.html

# Identify uncovered classes/methods
# Write unit tests for gaps
```

### Mutation Testing Failures
```bash
# Run PITest
mvn org.pitest:pitest-maven:mutationCoverage

# Open report
open target/pit-reports/index.html

# For each surviving mutant:
# 1. Understand what mutation was made
# 2. Write test that would fail with that mutation
# 3. Verify test kills the mutant
```

### SpotBugs Violations
```bash
# Run SpotBugs
mvn spotbugs:check

# View HTML report
open target/spotbugs.html

# For each violation:
# 1. Understand the issue
# 2. Fix the code
# 3. Re-run check
```

### Checkstyle Violations
```bash
# Check style
mvn checkstyle:check

# View report
open target/site/checkstyle.html

# Common fixes:
# - Add missing Javadoc
# - Fix line length
# - Remove unused imports
# - Fix naming conventions
```

## Best Practices

### Writing Testable Code
1. **Small Methods**: Max 150 lines, ideally < 50
2. **Single Responsibility**: One class does one thing
3. **Dependency Injection**: Pass dependencies via constructor
4. **Avoid Static State**: Makes testing harder
5. **Return Values**: Easier to test than side effects

### Writing Effective Tests
1. **Arrange-Act-Assert**: Clear test structure
2. **Test Behavior**: Not implementation details
3. **Descriptive Names**: `shouldThrowNPEWhenNameIsNull()`
4. **One Assertion**: Per logical test (can have multiple physical asserts)
5. **Test Edge Cases**: Null, empty, boundary values

### Maintaining High Coverage
1. **Test-Driven Development**: Write tests first
2. **Cover All Paths**: Use branch coverage to find gaps
3. **Defensive Copies**: Test that collections are unmodifiable
4. **Exception Handling**: Test all error paths
5. **Parameterized Tests**: For testing many similar cases

## Release Checklist

Before releasing a new version:

- [ ] All tests pass (`mvn clean verify`)
- [ ] Coverage ≥ 95%/90%
- [ ] Mutation score ≥ 80%
- [ ] Zero SpotBugs issues
- [ ] Zero Checkstyle violations
- [ ] Zero HIGH/CRITICAL CVEs
- [ ] Integration tests pass with real files
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version number bumped
- [ ] Git tag created

## References

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [PITest Documentation](https://pitest.org/)
- [SpotBugs Bug Descriptions](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html)
- [Checkstyle Checks](https://checkstyle.sourceforge.io/checks.html)
- [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)

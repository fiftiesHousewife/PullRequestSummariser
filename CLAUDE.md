
# Claude Code Session Notes

This document captures the best practices and patterns established during development. It is the authoritative reference for code style and the pre-push checklist.

## Project Setup Best Practices

### 1. Gradle Conventions

- Always use Kotlin DSL (`build.gradle.kts`, not `build.gradle`)
- Always use submodules, even if there is only one module — no root `build.gradle.kts`
- Root project has only `settings.gradle.kts` with `include("ModuleName")`
- All build configuration lives in submodule `build.gradle.kts` files

### 2. Use Version Catalogs (TOML)

Always centralise dependency versions in `gradle/libs.versions.toml`:

```toml
[versions]
gson = "2.10.1"

[libraries]
gson = { module = "com.google.code.gson:gson", version.ref = "gson" }

[plugins]
versions = { id = "com.github.ben-manes.versions", version.ref = "versions" }
```

Benefits:
- Single source of truth for all versions
- No hardcoded versions in build files
- Easy to update dependencies
- Type-safe accessors in IDE

Usage in `build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.gson)
}
```

### 3. Condense JUnit Dependencies

Before (5 lines):
```kotlin
testImplementation(platform("org.junit:junit-bom:6.0.3"))
testImplementation(libs.junit.jupiter.api)
testImplementation(libs.junit.jupiter.params)
testRuntimeOnly(libs.junit.jupiter.engine)
testRuntimeOnly(libs.junit.platform.launcher)
```

After (2 lines):
```kotlin
testImplementation(libs.junit.jupiter)        // aggregates api, params, engine
testRuntimeOnly(libs.junit.platform.launcher)
```

### 4. Add JaCoCo for Test Coverage

Include the JaCoCo plugin in every submodule and enforce a minimum coverage threshold:

```kotlin
plugins {
    id("jacoco")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
```

Usage:
```bash
./gradlew :ModuleName:jacocoTestReport        # generate HTML report
./gradlew :ModuleName:check                    # build + verify 80% coverage
```

### 5. Add Ben-Manes Versions Plugin

Essential for keeping dependencies up to date:

```kotlin
plugins {
    alias(libs.plugins.versions)
}
```

Usage:
```bash
./gradlew dependencyUpdates  # shows available updates
```

---

## Code Quality Standards

### Use `final` on Local Variables and Fields

Declare local variables and fields `final` wherever they are not reassigned. Apply to parameters too unless the verbosity clearly outweighs the benefit (e.g. long parameter lists).

```java
// Good
public void listPets(final String status, final String pageToken) {
    final PetFilter filter = PetFilter.builder().status(status).build();
}

// Bad — variables that are never reassigned should be final
public void listPets(String status, String pageToken) {
    PetFilter filter = PetFilter.builder().status(status).build();
}
```

### No Comments in Tests

Test method names must be self-documenting. Inline comments are noise.

### No Spurious Comments in Production Code

A comment is spurious if it restates what the code already clearly says. Comments are appropriate only when explaining *why* something non-obvious is done, or documenting a known gotcha.

### Package-Private Methods for Testing

Make helper methods package-private (no access modifier) instead of `private` so they can be tested directly from the same package in the test source tree.

### Always Use Curly Braces

All `if`, `else`, `for`, `while`, and `do-while` statements must use curly braces, even for single-line bodies.

### Class Size Limit — 150 Lines Maximum

No class should exceed 150 lines. A class approaching this limit is a signal to split it by responsibility. Name the resulting classes after what they do, not after the original class.

### Small, Focused Classes (~50 Lines)

Prefer classes of around 50 lines. A class that grows past roughly 100 lines is a signal to split it.

### Break Down Complex Logic

Prefer many small, named methods over a few large ones. Every extracted method is a candidate for a direct unit test.

### Prefer Immutable Objects

Use Java records, `final` fields, and immutable collections. Avoid setters and mutable state.

### Prefer Non-Static Code

Avoid static utility methods scattered across the codebase. Prefer instance methods on well-named classes that can be injected and tested.

### Static Imports for Readability

Use static imports when they make call sites clearer, particularly for constants, assertions, and factory methods. Do not use them when the originating class provides important context.

### No Magic Hard-Coded Strings

Extract repeated string literals into named constants. A string that appears more than once, or whose meaning is not self-evident, must be a constant. Name constants after the value they represent — do not add type prefixes like `VAR_`, `STR_`, `KEY_`.

### Meaningful Variable Names — No Abbreviations

Names must communicate intent clearly to a human reader. Abbreviations are only acceptable when they are universally understood in context (e.g. `id`, `url`, `csv`).

### Class and Method Naming

Names must be clear to a human reader. Avoid jargon terms that describe role rather than purpose:

- No `*Helper`, `*Util`, `*Manager`, `*Processor` — name classes after what they represent or produce
- No method names that start with `handle`, `process`, `do`, `perform` — name methods after what they return or what they change

### Logging

Never use `System.out.println` or `e.printStackTrace()`. Use `java.util.logging.Logger` or an appropriate logging framework.

---

## Test Standards

### No Underscores in Test Names, No Word "test"

Test method names are plain English camelCase sentences describing the behaviour under test.

```java
// Bad
@Test void test_fetchPage_returnsResults() {}
@Test void testFetchPageReturnsResults() {}

// Good
@Test void fetchesPageOfPets() {}
@Test void returnsEmptyPageWhenNoPetsMatch() {}
```

### Use `assertAll` for Multiple Assertions

When asserting multiple properties of the same object, wrap them in `assertAll` so all failures are reported together.

### Tests Are Self-Documenting

A reader must understand what behaviour is being tested without reading the implementation. Structure tests as: arrange, act, assert — no comments needed.

### No Disabled Tests or Commented-Out Code

A disabled test is a lie — it looks like coverage but provides none. Delete it or fix it. Commented-out code belongs in git history, not in source files.

---

## Apply to Any Cloned Repo

When setting up a new or cloned repository, apply these changes in order:

1. Add `gradle/libs.versions.toml` and move all versions into it
2. Replace hardcoded version strings in `build.gradle.kts` with catalog references
3. Add the Ben-Manes versions plugin
4. Condense JUnit dependencies to 2 lines
5. Remove spurious comments
6. Fix all compiler warnings (`-Xlint:all`)

---

## Pre-Push Checklist

Work through this checklist before pushing or opening a pull request.

### Dead Code

- [ ] No unused methods — every non-private method has at least one caller outside its own class (or is a tested package-private helper)
- [ ] No unused imports
- [ ] No unused fields or constants

### Build and Dependencies

- [ ] All dependency versions are declared in `gradle/libs.versions.toml` — no hardcoded versions in `build.gradle.kts`
- [ ] `./gradlew dependencyUpdates` shows no outdated stable dependencies
- [ ] Ben-Manes versions plugin is present in all modules that manage dependencies
- [ ] JUnit dependencies condensed to 2 lines (`libs.junit.jupiter` + `junit.platform.launcher`)
- [ ] No compiler warnings — `./gradlew build` is clean under `-Xlint:all`

### Code Quality

- [ ] No spurious inline comments — every comment explains *why*, not *what*
- [ ] No magic hard-coded strings — repeated or opaque literals are extracted into named constants
- [ ] No abbreviations in variable, field, or parameter names
- [ ] Class and method names are plain English business terms — no `Helper`, `Util`, `Manager`, `Processor`
- [ ] `final` is applied to all local variables, fields, and parameters that are not reassigned
- [ ] All non-private helper methods are package-private (no modifier), not `private`
- [ ] No static utility methods that should be instance methods
- [ ] Static imports used only where they improve readability
- [ ] All `if`/`else`/`for`/`while` statements use curly braces — no braceless single-line bodies
- [ ] No class exceeds 150 lines — split by responsibility if approaching the limit
- [ ] No deprecated API calls

### Design and Architecture

- [ ] Classes are focused and small (target ~50 lines; investigate anything over 100)
- [ ] Immutable objects are preferred — records, `final` fields, no unnecessary setters
- [ ] `System.out` and `e.printStackTrace()` are absent — use a logging framework

### Tests

- [ ] All tests pass: `./gradlew test`
- [ ] Unit tests exist for all package-private helper methods
- [ ] No test method names contain underscores or the word "test"
- [ ] Multiple assertions use `assertAll` so all failures are visible at once
- [ ] No `@Disabled` tests and no commented-out test code
- [ ] No inline comments inside test methods — names and structure carry all meaning

### Documentation

- [ ] README is in the project root — not inside a submodule directory
- [ ] Only one README exists — no duplicates in submodules
- [ ] README examples are consistent with actual code — endpoints, method signatures, and library usage all match the implementation

### Security

- [ ] No secrets, tokens, or credentials in source files

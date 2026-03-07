Run all checks from the CLAUDE.md pre-push checklist before committing. Work through each section and fix any violations found.

## Steps

1. **Build and Coverage**
   - Run `./gradlew :PullRequestSummariser:check` — this runs tests, JaCoCo coverage verification (75% minimum), and compilation
   - Run `./gradlew :PullRequestSummariser:dependencyUpdates` — check for outdated stable dependencies and flag any with later major versions

2. **Dead Code**
   - Check for unused methods — every non-private method should have at least one caller outside its own class
   - Check for unused imports
   - Check for unused fields or constants

3. **Code Quality**
   - No spurious inline comments — every comment explains *why*, not *what*
   - No magic hard-coded strings — repeated or opaque literals are extracted into named constants
   - No abbreviations in variable, field, or parameter names
   - Class and method names are plain English — no `Helper`, `Util`, `Manager`, `Processor`; no methods starting with `handle`, `process`, `do`, `perform`
   - `final` on all local variables, fields, and parameters that are not reassigned
   - All non-private helper methods are package-private (no modifier), not `private`
   - No static utility methods that should be instance methods
   - Static imports used only where they improve readability
   - All `if`/`else`/`for`/`while` use curly braces
   - No class exceeds 150 lines — split by responsibility if approaching the limit
   - No deprecated API calls
   - No `System.out` or `e.printStackTrace()` — use `@Log4j2`

4. **Tests**
   - All tests pass
   - No test method names contain underscores or the word "test"
   - Multiple assertions use `assertAll`
   - No `@Disabled` tests or commented-out test code
   - No inline comments inside test methods

5. **Documentation**
   - README is in the project root, not inside a submodule
   - Only one README exists
   - README examples match actual code

6. **Security**
   - No secrets, tokens, or credentials in source files

## Output

Report each check as PASS or FAIL with details. Fix all FAILs before committing. After all checks pass, show `git status` and `git diff --stat` so the user can review before committing.

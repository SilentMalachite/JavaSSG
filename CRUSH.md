## Build, Lint, and Test Commands

* Build: mvn clean package
* Lint: checkstyle:check
* Test: mvn test
* Test (single): mvn test -Dtest=TestClassName#testMethod

## Code Style Guidelines

* Import order: static imports, then alphabetical order
* Indentation: 4 spaces
* Line length: 120 characters
* Types: Use type inference where possible
* Naming conventions: camelCase for variables, PascalCase for classes
* Error handling: Use try-catch blocks and log errors

## Cursor and Copilot Rules
If present, these can be found in `.cursor/rules/` or `.github/copilot-instructions.md`

Make sure to run `javassg serve` for development server and `javassg build` for production build.

Remember to check `.gitignore` for ignored files and directories.
# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/javassg/`: Source code organized by package:
  - `build/` (build pipeline, HTML generation), `cli/` (entry + commands), `server/` (dev server + live reload), `parser/`, `template/`, `plugin/`, `cache/`, `config/`, `model/`, `security/`.
- `src/test/java/com/javassg/`: Tests mirror the same packages (e.g., `.../server/LiveReloadServiceTest.java`).
- Build tool: Maven (`pom.xml`). Java 21, UTF‑8 encoding.

## Build, Test, and Development Commands
- `mvn clean package`: Compile and create shaded JAR in `target/`.
- `mvn test`: Run unit tests (JUnit 5, Mockito, AssertJ).
- `mvn exec:java -Dexec.mainClass="com.javassg.JavaSSG" -Dexec.args="serve"`: Run dev server.
- `java -jar target/javassg-1.0.0.jar serve`: Run from built JAR. Use `build --production` to build the site.
- `mvn install`: Build and install to local Maven repo.
- `mvn jacoco:report`: Generate coverage report (if Jacoco available).

## Coding Style & Naming Conventions
- Java 21, 4‑space indentation, UTF‑8. Keep methods small and focused.
- Packages: `com.javassg.<module>`; classes: PascalCase; methods/fields: camelCase; constants: UPPER_SNAKE_CASE.
- Tests named `*Test.java` under the mirrored package path.
- Prefer SLF4J logging; avoid `System.out` in production code.

## Testing Guidelines
- Frameworks: JUnit Jupiter, Mockito, AssertJ.
- Place tests in `src/test/java/...` mirroring the class under test.
- Name tests descriptively (e.g., `SecurityValidatorTest#shouldDetectXssAttack`).
- Run fast unit tests locally with `mvn test`; aim to keep coverage reasonable for new/changed code.

## Commit & Pull Request Guidelines
- Commits: Imperative mood, concise subject, add context in body when needed (e.g., rationale, perf/security notes). Example: `Fix: sanitize Markdown code blocks`.
- Link issues in PR descriptions (e.g., `Closes #123`).
- PRs: clear summary, scoped changes, passing tests, and any CLI samples or screenshots for user‑facing changes (e.g., `serve` output).

## Configuration & Security Tips
- Default config loads from `config.yaml` at repo root. Validate inputs through `security/` utilities; avoid introducing unsafe file or HTML handling.

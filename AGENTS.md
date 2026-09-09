# AGENTS.md

This repository is the AWS SDK for Java v2. Keep changes narrow, compatible with Java 8, and aligned with the existing Maven multi-module build.

## Repository shape

- Root build: `pom.xml`
- Core/runtime modules: `core`, `utils`, `utils-lite`, `http-client-spi`, `http-clients`, `metric-publishers`
- Service modules: `services`, `services-custom`
- Code generation: `codegen`, `codegen-lite`, `codegen-maven-plugin`, `codegen-lite-maven-plugin`
- Test/support modules: `test/*`
- Release notes: `.changes`, `scripts/new-change`
- Contributor docs: `CONTRIBUTING.md`, `docs/GettingStarted.md`

## Working rules

- Use the Maven wrapper from the repo root: `./mvnw.cmd` on Windows, `./mvnw` on Unix.
- Preserve Java 8 source compatibility unless the task explicitly says otherwise. Avoid newer language features and APIs.
- Prefer minimal module-scoped builds over full-repo builds when iterating, for example `./mvnw.cmd -pl :s3 -am test`.
- Add or update tests for any behavior change. Unit tests are the default; integration tests require AWS credentials and may incur cost.
- For user-facing or release-worthy changes, add a changelog entry with `scripts/new-change` and commit the file created under `.changes/next-release` if that directory is used by the script.

## Generated code caution

- Much of the service client code is generated.
- Do not hand-edit generated service output unless the task is explicitly about regenerating it and you understand the generation path.
- When behavior appears to come from generated clients or models, inspect `codegen*` modules and any `codegen-resources` inputs before patching service classes directly.

## Validation

- Default verification: `./mvnw.cmd test` for affected modules or `./mvnw.cmd package` before finishing larger changes.
- Integration tests use the `integration-tests` profile and require AWS test credentials in `$HOME/.aws/awsTestAccount.properties`.
- If you change code generation or shared build logic, prefer `install` over earlier phases so downstream modules use the updated local artifacts.

## Editing guidance

- Follow existing style and license header conventions. The repo includes IntelliJ project settings and checkstyle configuration under `.idea` and `build-tools`.
- Avoid broad refactors unless they are required for the task.
- Do not update versions, release scripts, or generated artifacts as incidental cleanup.
- Check for existing issue or PR expectations in `CONTRIBUTING.md` when preparing a substantial change.

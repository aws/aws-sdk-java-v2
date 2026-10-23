# Building and Testing

Maven builds this repository (more than 500 modules). A full build with every check takes about an hour in CI, so
verify a change with the smallest build that covers it. CI still builds and tests everything.

## Commands

`<module>` is a Maven artifactId, for example `s3`, `sdk-core` or `dynamodb-enhanced`.

| goal | command |
|---|---|
| Build the dependencies of `<module>` (first build, after pulling or switching branches, after changing a module it depends on) | `mvn install -pl :<module> -am -P quick -T 1C -Dmaven.test.skip=true` |
| Verify `<module>` while iterating (Checkstyle and all unit tests, in parallel forks) | `mvn verify -pl :<module> -P dev` |
| Run one test class or method | `mvn test -pl :<module> -P dev -Dtest=MyClassTest` (or `-Dtest='MyClassTest#myMethod'`) |
| Changed module `A`, verify `A` and a module `B` that depends on it | `mvn verify -pl :A,:B -P dev` |
| Changed module `A`, test only `B` | `mvn install -pl :A -P quick -Dmaven.test.skip=true`, then `mvn verify -pl :B -P dev` |
| Changed the code generator | `mvn install -pl :codegen,:codegen-maven-plugin -P dev`, then `mvn clean verify -pl :codegen-generated-classes-test -P dev` (add `:protocol-tests` for protocol changes, and any service whose generated code changed) |
| Final check before opening or updating a PR (every check CI runs, for the modules you touched) | `mvn clean verify -pl :<module1>,:<module2>` (add `-T 1C` for several modules) |
| Work in several checkouts or worktrees at once | Add `-Dmaven.repo.local=<path>` to every command (see Gotchas) |

## Rules

1. Run the dependency build only for a first build, after pulling or switching branches, or after changing a
   module that `<module>` depends on. If nothing upstream changed, `-pl :<module>` alone is enough.
2. Do not use `clean` while iterating: it discards compiler and Checkstyle work. Use it only (a) after changing the
   code generator or a service model ("Attempted to clobber existing file" means you need it), (b) after switching
   to a branch with a different code generator, and (c) in the final check.
3. Use `-P dev` for every iteration, including `-Dtest` runs. Without it, SpotBugs, Javadoc, japicmp, dependency
   analysis and JaCoCo also run. Run the default build (no profile) once, as the final check.
4. Never combine `-am` with tests: it runs the tests of every upstream module. Build upstream with `-P quick`, then
   test only the modules you changed.
5. After changing public or protected signatures in a module that other modules use, re-run the dependency build
   before verifying downstream modules. `-pl :A,:B` skips the modules between `A` and `B`, and their installed jars
   were compiled against the old `A`. For changes inside method bodies, `-pl :A,:B` is enough.
6. `verify` does not install. If a later command builds `B` without `A` in the reactor, run `install` for `A` first.
7. With `-Dtest` and more than one module in the reactor, add `-Dsurefire.failIfNoSpecifiedTests=false`.
8. Run one Maven build at a time per checkout and per local repository. Concurrent builds share `target/` and the
   local repository, and make tests flaky.

## For agents

- Redirect output to a log and read only what you need:
  `mvn ... > /tmp/build-<module>.log 2>&1; echo EXIT=$?; tail -40 /tmp/build-<module>.log`.
  Find failures with `grep -E '^\[ERROR\]|<<< (FAILURE|ERROR)' /tmp/build-<module>.log`. Per-class results are in
  `target/surefire-reports/` under the module's directory.
- Run builds in the foreground with a timeout of about twice the expected time. Expected times with `-P dev`:
  sdk-core 30 s, s3 90 s, codegen-generated-classes-test 155 s. With the default profile: s3 about 4 minutes,
  codegen-generated-classes-test about 8 minutes. Do not poll a background build every minute.
- Re-run only what failed: fix it, re-run the failing class with `-Dtest`, then the module with `-P dev`, and run
  the final check once at the end.
- Checkstyle runs first (in `validate`), so a style error fails the build within seconds. Keep Maven's default
  fail-fast while iterating. Use `-fae` only for a final check of several modules.

## Gotchas

- If `AWS_SDK_UA_APP_ID` is set, `AppIdResolutionTest` and `AppIdUserAgentTest` fail. Run tests with
  `env -u AWS_SDK_UA_APP_ID mvn ...`.
- If `-P dev` fails with "add -XX:+AllowRedefinitionToAddDeleteMethods", the local repository has an old
  `service-test-utils`. Re-run the dependency build.
- `-Dmaven.test.skip=true` also skips compiling tests. It is safe in dependency builds only because no module
  depends on another module's `test-jar`. If a module ever does, drop the flag from the dependency build.
- Checkouts or worktrees on different branches overwrite each other's SNAPSHOT jars in `~/.m2`, including
  `build-tools`, which holds the Checkstyle and SpotBugs rules. Give each one its own local repository with
  `-Dmaven.repo.local=<path>`. Seed it once with
  `rsync -a --exclude '/software/amazon/awssdk/' ~/.m2/repository/ <path>/`, then run the dependency build in it.
- Some WireMock-based tests fail now and then with `NoHttpResponseException` or "Connection reset", more often
  when the machine is busy. Re-run the failing class before debugging it.
- mvnd (the Maven daemon) is optional. It saves 1-3 s on small runs such as `-Dtest`, and nothing on test-heavy
  runs. Run `mvnd --stop` after changing the code generator or a Maven plugin.

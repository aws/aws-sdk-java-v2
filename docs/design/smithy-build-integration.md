# Proposal: `smithy-build.json`-driven code generation for the AWS SDK for Java v2

Status: **Prototype implemented** — Phases 1-3 are built and verified for `services/account`.
See §10 for what the implementation changed relative to this design.

## 1. Goal

Let an AWS-owned service module under `services/` be generated from a Smithy model
described by a `smithy-build.json`, so that `mvn install -pl :s3` performs a real
Smithy build. C2J-based services must keep working with zero changes to their
build configuration.

## 2. Where the POC is today (verified)

The branch is a single grafted commit ("Update codegen mojo to use Smithy model"),
so there is no base diff to read; the findings below come from reading the code.

**Generation is bound in one place.** `services/pom.xml:617-628` declares
`codegen-maven-plugin:generate` at the `generate-sources` phase in
`pluginManagement`, and the `generated-service` profile activates it based on the
existence of `src/main/resources/codegen-resources`. Individual service POMs such
as `services/s3/pom.xml` do not mention codegen at all. That single binding point
is what makes this migration tractable.

**The mojo already forks between C2J and Smithy.**
`GenerationMojo` walks `codegen-resources` to a depth of 10 collecting anything
ending in `service-2.json` or `model.json`. Per model root, if `model.json`
exists it takes the Smithy path
(`SmithyModelWithCustomizations` → `SmithyIntermediateModelBuilder` →
`IntermediateModel`), otherwise the C2J path
(`C2jModels` → `IntermediateModelBuilder`). Both converge on the same
`CodeGenerator`.

**`services/account` is the pilot.** It contains `model.json` (117 KB, `"smithy": "2.0"`),
`customization.config`, and `paginators-1.json` — and no `service-2.json`, so it is
already Smithy-only. No `smithy-build.json` exists anywhere in the repository yet.

**Output layout is fixed by the root POM.** Generation writes to
`target/generated-sources/sdk`, `target/generated-resources/sdk-resources`, and
`target/generated-test-sources/sdk-tests`. The root `pom.xml` (~lines 350-372)
wires the first two into the build with `build-helper-maven-plugin`
(`add-source` / `add-resource`) at `generate-sources`. The mojo itself calls
`addCompileSourceRoot` and `addTestCompileSourceRoot`, but not `addResource`.

**Smithy dependencies are partly in place.** `codegen/pom.xml` depends on
`smithy-model`, `smithy-rules-engine`, `smithy-aws-traits`, and
`smithy-aws-endpoints` at `${smithy.version}` = `1.65.0` (`pom.xml:108`).
`smithy-build` is **not** yet a dependency.

### 2.1 Gaps in the POC worth naming up front

- **The model never goes through Smithy's build pipeline.** `SmithyModelWithCustomizations`
  assembles the model directly with
  `Model.assembler().addUnparsedModel(...).discoverModels(...).disableValidation()`.
  There are no projections, no transforms, and no validation.
- **Paginators and waiters are dropped.** `translatePaginators()` and
  `translateWaiters()` are `TODO` stubs returning `Paginators.none()` /
  `Waiters.none()`. `services/account` ships a `paginators-1.json` that the Smithy
  path silently ignores. This is a live regression, independent of this proposal,
  and it will mask itself as "Smithy codegen works" until someone diffs the output.
- **Single service only.** `SmithyIntermediateModelBuilder.getServiceShape` throws
  unless the model has exactly one service shape.
- **Debug output.** `GenerationMojo` contains several leftover
  `System.out.println("WARBLEGARBLE...")` statements.

## 3. The constraint that shapes the design

**Smithy publishes no official Maven plugin.** The only first-party build
integrations are the Gradle `smithy-base` and `smithy-jar` plugins. The SDK builds
with Maven.

So the choice is not "which Smithy Maven plugin do we adopt" but rather: we embed
the `smithy-build` library into the existing `codegen-maven-plugin`. That library
is designed for this — verified against `smithy-build-1.73.0` with `javap`:

```
SmithyBuildConfig.load(Path)                      // parses smithy-build.json
SmithyBuild.config(..).outputDirectory(..).build() // runs projections + plugins
SmithyBuildPlugin { getName(); execute(PluginContext); requiresValidModel(); }
PluginContext   { getModel(); getSettings(); getFileManifest(); getProjectionName(); }
```

This keeps Maven as the driver while making `smithy-build.json` the real source of
truth. It also means we do **not** introduce Gradle into the SDK build.

## 4. Proposed architecture

Two separable changes, matching the split you anticipated.

### Change 1 — Register SDK codegen as a Smithy build plugin (SPI)

Add to the `codegen` module (which already has the Smithy dependencies):

- `software.amazon.awssdk.codegen.smithy.build.AwsSdkJavaCodegenPlugin`
  implementing `software.amazon.smithy.build.SmithyBuildPlugin`.
- `codegen/src/main/resources/META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin`
  naming that class. This is the entire discovery mechanism — Smithy resolves
  plugin names from `smithy-build.json` through `ServiceLoader`.
- A typed settings class parsing `PluginContext.getSettings()` (an `ObjectNode`).
- Add `smithy-build` at `${smithy.version}` to `codegen/pom.xml`.

Proposed plugin name: **`aws-sdk-java-v2-codegen`**. It must not be `java-codegen`,
which is what `smithy-java` uses; colliding names on a shared classpath would be
ambiguous.

`execute(PluginContext)` does only this:

1. Read settings → resolve the target service `ShapeId`.
2. Take `context.getModel()` — already projected and transformed by Smithy.
3. Build the `IntermediateModel` via the existing `SmithyIntermediateModelBuilder`.
4. Invoke the existing `CodeGenerator`, pointing its three output directories at
   subdirectories of `context.getFileManifest()`'s base directory.

Because `CodeGenerator.builder()` already accepts `sourcesDirectory`,
`resourcesDirectory`, and `testsDirectory` as plain strings, step 4 needs **no
change to `CodeGenerator`**. The plugin writes:

```
<projection>/aws-sdk-java-v2-codegen/java/       -> compile sources
<projection>/aws-sdk-java-v2-codegen/resources/  -> resources
<projection>/aws-sdk-java-v2-codegen/tests/      -> test sources
```

The `java/` + `resources/` convention matches `smithy-java`, so the layout will look
familiar to anyone who has wired up a Smithy codegen plugin before.

Note that `SmithyIntermediateModelBuilder` currently takes a
`SmithyModelWithCustomizations`. It should gain a path that accepts an
already-assembled `Model` plus a service `ShapeId`, because under `smithy-build`
the model arrives pre-assembled and the "exactly one service shape" assumption no
longer holds (a projection can legitimately retain several).

### Change 2 — Teach `GenerationMojo` to run a Smithy build

Replace the current `model.json`-sniffing with a decision made once per module:

```
if (${basedir}/smithy-build.json exists) -> Smithy build path
else                                     -> existing C2J path, untouched
```

Detection at the module root rather than inside `codegen-resources` matters: it is
an explicit, reviewable opt-in per service, and it is the file the Smithy
ecosystem already expects.

The Smithy path:

1. `SmithyBuildConfig.load(basedir/smithy-build.json)`.
2. Override `outputDirectory` to `${project.build.directory}/smithyprojections`,
   so Smithy's default `build/` directory never appears and `mvn clean` stays
   correct.
3. Run `SmithyBuild` with the mojo's own classloader, which is where the SPI
   descriptor from Change 1 lives (`codegen-maven-plugin` depends on `codegen`).
   No classpath gymnastics required.
4. For each projection result, register the plugin's output directories:
   `addCompileSourceRoot(.../java)`, `addTestCompileSourceRoot(.../tests)`, and
   `project.addResource(.../resources)`.

Step 4 is the one genuinely new piece of Maven wiring. The mojo already registers
source roots; resources are currently handled by the root POM's hardcoded
`build-helper` path, so the Smithy path must register its resource root itself.
Registering during `generate-sources` is safe because `process-resources` runs later.

C2J modules are unaffected: they keep writing to the canonical directories that
`build-helper` already knows about. Nothing in the root POM changes, and the four
hardcoded references to `generated-sources/sdk` (in `core/sdk-core` and three
C2J-only test modules) stay valid.

### 4.1 What a migrated service looks like

`services/account/smithy-build.json`:

```json
{
    "version": "1.0",
    "sources": ["src/main/resources/codegen-resources/model.json"],
    "plugins": {
        "aws-sdk-java-v2-codegen": {
            "service": "com.amazonaws.account#Account",
            "customizationConfig": "src/main/resources/codegen-resources/customization.config"
        }
    }
}
```

`mvn install -pl :account` then runs a genuine Smithy build. Keeping the model under
`codegen-resources` (rather than moving it to Smithy's conventional `model/`
directory) is deliberate for now: the `generated-service` profile activates on that
directory's existence, and Maven profile activation cannot express "either of two
files." Moving models later means extending that activation.

## 5. Design decisions and alternatives

**Write through the `FileManifest` rather than straight to the canonical SDK
directories.** The tempting shortcut is to have the plugin write directly to
`target/generated-sources/sdk`, which would require no new Maven wiring at all.
I recommend against it: a plugin that writes outside its manifest breaks the
Smithy contract, so `smithy build` from the CLI would produce nothing usable and
the `smithy-build.json` would be decorative. Honouring the manifest costs one
`addResource` call and gives us CLI and Gradle compatibility for free.

**Resolve setting paths against the module directory.** `customizationConfig` is a
relative path, and under `mvn -pl :s3` the process working directory is the repo
root, not the module. The mojo should absolutize relative setting paths against
`${basedir}` before invoking `SmithyBuild`. Keeping this in the mojo leaves
`smithy-build.json` clean and declarative.

**Longer term, customizations should move into the model.** Referencing
`customization.config` from plugin settings is a migration bridge, not the
destination. Traits and projection transforms are the Smithy-native expression of
most of what `customization.config` does today.

## 6. Phased plan

| Phase | Scope | Outcome |
|---|---|---|
| 1 | `AwsSdkJavaCodegenPlugin` + SPI descriptor + settings parsing + `smithy-build` dependency | Codegen is a real Smithy plugin, usable from the Smithy CLI |
| 2 | `GenerationMojo` detection, embedded `SmithyBuild`, source/resource/test root registration | `mvn install -pl :account` builds via `smithy-build.json` |
| 3 | `services/account/smithy-build.json`; remove the `model.json` sniffing path; strip debug `println`s | One service fully migrated, one code path per module |
| 4 | Close the fidelity gaps: paginators, waiters, validation posture | Smithy output verifiably matches C2J output |
| 5 | Migrate a hard service (S3) to exercise heavy customizations | Confidence the approach generalises |

Phase 1 is independently reviewable and testable without touching the Maven build
at all, which is a good property for the first CR.

## 7. Risks and open questions

**Validation posture.** The POC calls `disableValidation()`. `smithy-build`
validates by default and `SmithyBuildPlugin.requiresValidModel()` defaults to
`true`. C2J-converted models may well emit validation errors, so Phase 2 needs a
decision: fix the models, add suppressions, or relax `requiresValidModel()`. I would
not relax it silently — validation is a large part of what Smithy buys us.

**The `maven` block in `smithy-build.json` will not work.** Dependency resolution
from that block is a Smithy *CLI* feature. Embedded `SmithyBuild` ignores it, so
anything a service model needs must be a Maven dependency of `codegen`. This needs
documenting, and possibly a hard error if the block is present, so the failure is
not silent.

**Version alignment.** `${smithy.version}` is `1.65.0`; `smithy-build` must be pinned
to the same version as `smithy-model` to avoid a split-version classpath.

**Output-diffing strategy.** The highest-value safety net is a golden-file
comparison of Smithy-generated versus C2J-generated output for the same service.
The dropped paginators are exactly the class of bug such a check catches
immediately. Worth deciding whether this lands in Phase 3 or gates Phase 4.

**Multi-model modules.** The C2J path supports several model roots per module plus
`shareModelConfig`. Under `smithy-build.json` the natural expression is one
projection per service. Whether any AWS-owned module actually needs this is an open
question I have not yet answered.

## 8. External consumers: customer model + Smithy CLI, outside this repo

Short answer: **the SPI design in Change 1 enables this and the plumbing is
essentially free — but the generator itself would reject almost any customer model
today.** The blockers are in codegen, not in the build integration.

### 8.1 The plumbing already works

- **The artifact is already published.** `software.amazon.awssdk:codegen` is on
  Maven Central (latest `2.54.12` at time of writing), as is
  `codegen-maven-plugin`. No new publishing pipeline is required; the SPI descriptor
  from Change 1 would simply travel inside the jar we already ship.
- **The Smithy CLI resolves the plugin itself.** The `maven` block in
  `smithy-build.json` is documented as the mechanism for pulling in "build plugins",
  and plugin resolution is plain `ServiceLoader`. A customer writes:

  ```json
  {
      "version": "1.0",
      "sources": ["model"],
      "maven": {
          "dependencies": ["software.amazon.awssdk:codegen:2.41.29"]
      },
      "plugins": {
          "aws-sdk-java-v2-codegen": {
              "service": "com.example#MyService"
          }
      }
  }
  ```

  and `smithy build` works. Note the pleasing asymmetry: the `maven` block that does
  **not** work for our embedded-Maven path (§7) is exactly the mechanism that
  **does** work for CLI consumers.
- **Java version is fine.** `codegen` targets Java 8 bytecode
  (`codegen/pom.xml` `jre.version` = `1.8`), which runs happily on the JDK 17+ that
  current Smithy tooling requires.
- **Output lands somewhere usable.** Because the plugin writes through the
  `FileManifest` (§5), a CLI build produces
  `build/smithyprojections/<projection>/aws-sdk-java-v2-codegen/{java,resources,tests}`
  which the customer compiles with their own build. This is the concrete payoff of
  rejecting the "write straight to `target/generated-sources/sdk`" shortcut — that
  version would make CLI use produce nothing at all.

So Change 1 buys Smithy CLI **and** Gradle support at no additional cost, purely as
a consequence of implementing the SPI correctly.

### 8.2 What would actually fail today

These are pre-existing POC limitations, surfaced by thinking about the external case:

1. **Only `restJson1` is understood.**
   `ProtocolUtils.resolveProtocol(ServiceIndex, ServiceShape)` maps `restJson1` and
   throws `IllegalArgumentException("Unable to translate protocol trait")` for
   everything else. Worse, it throws on the *first* unrecognised protocol even when a
   supported one is also present. This blocks most external models — and it blocks
   migrating S3 (`restXml`) internally, so it is on the critical path regardless.
2. **`aws.api#service` is mandatory.**
   `DefaultSmithyNamingStrategy.serviceId()` throws
   `IllegalStateException("ServiceId is missing in the Smithy model.")`, and it is
   reached via `getServiceName()` from `constructMetadata()` — the first step of
   building the intermediate model. A non-AWS model fails immediately, with an
   exception rather than a clear "unsupported" message.
3. **The output is an AWS SDK client.** Generated code depends on SDK runtime
   modules and assumes AWS-shaped concerns: SigV4 auth, regions, endpoint
   resolution, AWS protocol modules. A customer service that is not AWS-shaped may
   generate but not cohere.
4. **`customizationConfig` path resolution.** §5 has the mojo absolutise relative
   setting paths against `${basedir}`; there is no mojo under the CLI. Fix: make the
   setting optional (most external users have no customizations) and support
   `${SMITHY_ROOT_DIR}`, which `smithy-build.json` interpolates natively.

### 8.3 The real question is support posture, not feasibility

`codegen` is published but is de facto internal — its own POM describes it as holding
what is "required to generate the AWS Java SDK clients for AWS services," and it
carries no API stability contract. Advertising it as a Smithy build plugin turns
both the settings schema and the generated API surface into a public, supported
contract. That is a product commitment, not a technical one. Three coherent
positions:

- **(a) Unsupported / experimental.** Works if you can make it work; no guarantees.
  Costs nothing beyond what Change 1 already does.
- **(b) Supported for AWS-shaped services only.** Requires fixing (1) and replacing
  (2)'s exception with a clear diagnostic.
- **(c) Fully supported for arbitrary Smithy services.** Requires (1) through (3),
  which is a substantially larger programme.

**Recommendation:** treat external consumption as an explicit non-goal for Phases
1-3, while deliberately not foreclosing it — the SPI design keeps the door open for
free. Add it as Phase 6, gated on protocol mapping. Critically, do **not** document
or advertise the plugin until (1) and (2) are addressed, because today's failure
modes are raw exceptions rather than actionable messages, which would generate
support load out of proportion to the feature's maturity.

One cheap, high-value item worth pulling into Phase 1 regardless: replace the
`serviceId()` `IllegalStateException` and the `"Unable to translate protocol trait"`
throw with messages that name the offending shape and state what is supported. Those
same messages are what we will want internally when migrating S3.

## 9. Questions for you

1. Plugin name — `aws-sdk-java-v2-codegen`, or something shorter?
2. Should Phase 1 ship as its own CR, given it is inert until Phase 2 lands?
3. Do you want the paginators/waiters gap fixed before or after `smithy-build.json`
   wiring? It is currently a silent output regression on `services/account`.
4. Is `services/account` the intended long-term pilot, or a throwaway?
5. Which support posture from §8.3 do we want to aim at — and do we want the plugin
   name and settings schema to be chosen now with (c) in mind, even if we only
   commit to (a)?

## 10. Implementation notes (prototype)

Phases 1-3 are implemented. `mvn install -pl :account` now generates through
`services/account/smithy-build.json`, and the output is byte-for-byte identical to
what the previous `model.json` path produced (114 sources, 1 test source).

### 10.1 What changed relative to the design above

**The embedded library does not read `sources` itself at Smithy 1.65.0.** This is the
one place the design was wrong. §4 assumed `SmithyBuild` would assemble the model
from the config's `sources`. At the version this repo pins (`${smithy.version}` =
1.65.0), `SmithyBuildConfig` has no `toModelAssembler` method at all — that arrived
later (it exists in 1.73.0). Reading `sources`/`imports` is the responsibility of
whatever drives the library, normally the Smithy CLI.

So `SmithyBuildGenerator` assembles the model itself and passes it in:

```java
SmithyBuild.create(classLoader)
           .config(config)
           .model(model)                 // assembled from config sources/imports
           .registerSources(sources)      // so the built-in `sources` plugin works
           .outputDirectory(outputDirectory)
           .build();
```

A useful side effect: because we assemble the model, we control validation. The
prototype leaves validation **enabled** and `account` passes cleanly, which retires
the open question from §7 for this service. The POC's previous
`disableValidation()` call is gone.

**Plugin settings needed a `baseDir`.** As anticipated in §5, relative paths in
plugin settings cannot resolve against the process working directory, which is the
repo root under `mvn -pl :account`. The mojo rewrites each
`aws-sdk-java-v2-codegen` settings node to add an absolute `baseDir` before running
the build, handling both top-level `plugins` and per-projection `plugins`. The
plugin resolves `customizationConfig` against it, falling back to the working
directory when absent so CLI use still behaves sensibly.

**Only source and test roots were registered for `account`.** `addResource` is
implemented, but the codegen plugin produced no resources for this service, and
empty directories are deliberately skipped so the build does not gain roots that
generate nothing.

### 10.2 Verified

- `mvn clean install -pl :bom-internal,:codegen,:codegen-maven-plugin` — BUILD
  SUCCESS with checkstyle, spotbugs, dependency analysis, and all 567 tests.
- `mvn clean install -pl :account` — generates via `smithy-build.json`; output
  diffed identical to the pre-change baseline.
- `mvn clean install -pl :pinpointsmsvoice` — a C2J service still takes the C2J
  path, still writes to `target/generated-sources/sdk`, and creates no
  `smithyprojections` directory.
- SPI discovery resolves `aws-sdk-java-v2-codegen` from the **published
  `codegen` jar** via `ServiceLoader`, with a negative control for an unknown name.
  This is the mechanism §8 depends on for Smithy CLI use.
- Error paths exercised: a `maven` block, an unknown `service` shape ID, and a
  missing `sources` entry each fail with an actionable message.

### 10.3 Incidental fixes

The prototype also fixed pre-existing problems that were invisible because this
branch had only ever been built with `-P quick`, which skips checkstyle, spotbugs,
and dependency analysis:

- Six unused imports and one import-ordering violation (checkstyle).
- A non-localized `toLowerCase()` when deriving `uid` (spotbugs `DM_CONVERT_CASE`);
  now `toLowerCase(Locale.US)`.
- `CodeGeneratorTest` asserted on an error message the POC had already changed.
- `smithy-aws-endpoints` is flagged as an unused declared dependency because no Java
  code imports it, but it is genuinely required at runtime so model discovery can
  resolve `aws.endpoints` traits — `account`'s model uses
  `aws.endpoints#standardPartitionalEndpoints`. It is now explicitly ignored with
  that rationale recorded, appended to the inherited list rather than replacing it.
- `codegen-maven-plugin` now declares `smithy-build`, `smithy-model`, and
  `maven-model` explicitly instead of relying on them transitively.

Debug `System.out.println` statements were removed, and the `model.json` sniffing in
`GenerationMojo` was deleted so each module has exactly one generation path.

### 10.4 Still outstanding

- **Paginators and waiters are still dropped** (§2.1). `services/account` ships a
  `paginators-1.json` that the Smithy path ignores. This is the highest-value
  remaining gap and it is invisible in the identical-output check, because the
  baseline was itself produced by the Smithy path.
- **Only `restJson1` translates.** The error message is now actionable, but S3
  (`restXml`) still cannot migrate.
- **No tests were added** for the new plugin or the mojo path. The prototype is
  verified end-to-end by build and output diffing, not by unit tests. Worth adding
  before this becomes a real CR: settings parsing, service resolution failure modes,
  and the `baseDir` injection.

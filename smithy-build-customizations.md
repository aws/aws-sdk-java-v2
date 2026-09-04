# Migrating a service to Smithy: `smithy-build.json` and customizations as transforms

Audience: AWS SDK for Java v2 team members. This describes a working prototype on the
`feature/master/smithy-poc` branch, using `services/account` as the pilot. The design
document with rationale and open questions is
[`docs/design/smithy-build-integration.md`](docs/design/smithy-build-integration.md).

The short version: a service opts in by adding a `smithy-build.json`. `mvn install -pl
:account` then runs a real Smithy build — projections, transforms, and all — instead of
loading a C2J model. Services without a `smithy-build.json` are completely unaffected.
Service-specific customizations can move out of `customization.config` and into Java
`ProjectionTransformer`s that live beside the service.

## Before and after

A C2J service today, `services/cloudwatch`:

```
services/cloudwatch/
├── pom.xml
└── src/main/resources/codegen-resources/
    ├── service-2.json          <- the model
    ├── customization.config
    ├── endpoint-rule-set.json
    ├── endpoint-tests.json
    ├── paginators-1.json
    ├── waiters-2.json
    └── examples-1.json
```

The migrated service:

```
services/account/
├── pom.xml
├── smithy-build.json                          <- opts the module into the Smithy path
├── src/main/resources/codegen-resources/
│   ├── model.json                             <- one Smithy model replaces the C2J files
│   ├── customization.config                   <- shrinks; see "Migrating customizations"
│   └── paginators-1.json
└── customization/                              <- separate Maven module
    ├── pom.xml
    └── src/main/
        ├── java/.../AddCustomOperationTransformer.java
        └── resources/META-INF/services/software.amazon.smithy.build.ProjectionTransformer
```

Note that endpoint rules and tests are no longer separate files. They arrive as the
`smithy.rules#endpointRuleSet` and `smithy.rules#endpointTests` traits on the service
shape in `model.json`.

Generated code moves too. C2J writes to `target/generated-sources/sdk`; the Smithy path
writes to `target/smithyprojections/<projection>/aws-sdk-java-v2-codegen/{java,resources,tests}`
and the mojo registers those as source, resource, and test roots. Nothing else in the
build needed changing for that.

## How the pieces fit

Three parts, in the order the build touches them.

**1. `smithy-build.json` in the module root.** This is the switch. `GenerationMojo`
checks for it and takes the Smithy path if present, otherwise the existing C2J path
runs untouched:

```java
Path smithyBuildConfig = project.getBasedir().toPath().resolve(SMITHY_BUILD_FILE);
if (Files.exists(smithyBuildConfig)) {
    new SmithyBuildGenerator(project, getLog()).generate(smithyBuildConfig, ...);
    return;
}
generateFromC2jModels();
```

**2. Codegen is a Smithy build plugin.** `AwsSdkJavaCodegenPlugin` in the `codegen`
module implements `SmithyBuildPlugin` under the name `aws-sdk-java-v2-codegen`, and is
found via `META-INF/services/software.amazon.smithy.build.SmithyBuildPlugin`. It takes
the already-projected model and hands it to the existing `CodeGenerator`:

```java
@Override
public void execute(PluginContext context) {
    AwsSdkJavaCodegenSettings settings = AwsSdkJavaCodegenSettings.fromNode(context.getSettings());
    Path baseDir = context.getFileManifest().getBaseDir();
    ...
    SmithyModelWithCustomizations model =
        SmithyModelWithCustomizations.builder()
                                     .smithyModel(context.getModel())   // projected + transformed
                                     .service(settings.service())
                                     .customizationConfig(loadCustomizationConfig(settings))
                                     .build();

    CodeGenerator.builder()
                 .intermediateModel(new SmithyIntermediateModelBuilder(model).build())
                 .sourcesDirectory(baseDir.resolve("java").toString())
                 .resourcesDirectory(baseDir.resolve("resources").toString())
                 .testsDirectory(baseDir.resolve("tests").toString())
                 .build()
                 .execute();
}
```

`CodeGenerator` itself is unchanged. Because this is plain SPI, the same plugin works
from the Smithy CLI and the Smithy Gradle plugins, not just our Maven build.

**3. The mojo embeds `smithy-build`.** Smithy publishes no Maven plugin, so
`SmithyBuildGenerator` drives the `smithy-build` library directly, which keeps
generation inside the Maven lifecycle.

## `services/account/smithy-build.json`

```json
{
    "version": "1.0",
    "sources": ["src/main/resources/codegen-resources/model.json"],
    "maven": {
        "dependencies": [
            "software.amazon.awssdk:codegen:${AWS_SDK_JAVA_VERSION}",
            "software.amazon.awssdk:account-codegen-customization:${AWS_SDK_JAVA_VERSION}"
        ]
    },
    "projections": {
        "client": {
            "transforms": [
                { "name": "addAccountCustomOperation",
                  "args": { "service": "com.amazonaws.account#Account" } }
            ],
            "plugins": {
                "aws-sdk-java-v2-codegen": {
                    "service": "com.amazonaws.account#Account",
                    "customizationConfig": "src/main/resources/codegen-resources/customization.config"
                }
            }
        }
    }
}
```

Four things worth knowing about this file:

- **Codegen sits inside the named projection, not at the top level.** Top-level plugins
  run for *every* projection, so putting it there generates the client twice — once for
  the implicit `source` projection and once for `client`.
- **The `maven` block is only for the Smithy CLI.** Under Maven it is ignored with an
  informational message; the classpath comes from the plugin's own dependencies plus
  this module's `provided`-scope dependencies.
- **`${AWS_SDK_JAVA_VERSION}` is injected by the mojo** from `project.getVersion()`, so
  no version is hard-coded. Smithy expands `${...}` from system properties and
  environment variables at load time, and *fails the load* if a variable is unresolved,
  so only use placeholders the mojo guarantees.
- **`customizationConfig` is still supported**, so a migration does not have to move all
  customizations at once.

## A customization as a transform

`services/account/customization` is an ordinary Maven module holding a
`ProjectionTransformer`. The demo one synthesizes an operation; the shape of the code is
what matters:

```java
@SdkInternalApi
public final class AddCustomOperationTransformer implements ProjectionTransformer {
    static final String NAME = "addAccountCustomOperation";   // the name used in smithy-build.json

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Model transform(TransformContext context) {
        Model model = context.getModel();
        ServiceShape service = resolveService(context, model);   // optional "service" arg

        StructureShape input  = structure(namespace, "MyCustomOperationRequest", true);
        StructureShape output = structure(namespace, "MyCustomOperationResponse", false);

        OperationShape operation =
            OperationShape.builder()
                          .id(ShapeId.fromParts(namespace, "MyCustomOperation"))
                          .input(input.getId())
                          .output(output.getId())
                          .addTrait(HttpTrait.builder()
                                             .method("POST")
                                             .uri(UriPattern.parse("/myCustomOperation"))
                                             .code(200)
                                             .build())
                          .build();

        return model.toBuilder()
                    .addShapes(input, output, operation)
                    .addShape(service.toBuilder().addOperation(operation.getId()).build())
                    .build();
    }
}
```

Registered by one line in
`src/main/resources/META-INF/services/software.amazon.smithy.build.ProjectionTransformer`:

```
software.amazon.awssdk.services.account.customization.AddCustomOperationTransformer
```

The result is ordinary generated code: `myCustomOperation` appears on `AccountClient`
and `AccountAsyncClient`, with `MyCustomOperationRequest`, `MyCustomOperationResponse`,
and `MyCustomOperationRequestMarshaller` alongside every other model class — 117
generated sources instead of 114.

### Wiring it so `mvn install -pl :account` works

This is the part with a non-obvious answer.

The intuitive approach is to list the customization under the codegen plugin's
`<dependencies>`. **That cannot work**: Maven resolves plugin dependencies from
repositories only, never from the reactor, so a module sitting next to the service could
never be built and consumed in the same invocation.

What works is a normal `provided`-scope dependency:

```xml
<!-- services/account/pom.xml -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>account-codegen-customization</artifactId>
    <version>${awsjavasdk.version}</version>
    <scope>provided</scope>
</dependency>
```

and a mojo that layers those artifacts onto the Smithy build class loader:

```java
// SmithyBuildGenerator
ClassLoader parent = AwsSdkJavaCodegenPlugin.class.getClassLoader();
for (Artifact artifact : resolvedArtifacts()) {
    if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) && artifact.getFile() != null) {
        urls.add(artifact.getFile().toURI().toURL());
    }
}
return new URLClassLoader(urls.toArray(new URL[0]), parent);
```

This is the Maven counterpart to the Smithy Gradle plugin's dedicated `smithyBuild`
configuration. `provided` is the right scope on the merits: visible at build time, not
transitive to consumers of the published `account` artifact, and already excluded from
`dependency:analyze` because the root POM sets `ignoreNonCompile`.

Two requirements come with it. The mojo needs
`@Mojo(requiresDependencyResolution = ResolutionScope.COMPILE)`, or
`project.getArtifacts()` is empty. And the customization module must be in the reactor
(`services/pom.xml` lists `<module>account/customization</module>`), which makes Maven
order it immediately before `account`.

One caveat: plain `mvn install -pl :account` resolves the *installed* customization
rather than rebuilding it — the same as it already does for `aws-core` and every other
SDK dependency. Use `--am`, or a prior full build, after changing a transform.

## Migrating existing customizations

There are 390 `customization.config` files using 52 distinct keys. They split cleanly,
and the split is encouraging: the high-volume keys are generator settings that stay put,
while the awkward long tail is model-shaped and moves to transforms.

**Stays as configuration** — these describe generator or runtime behaviour, not the
model, and there is no reason to express them as transforms:

| Key | Services |
|---|---|
| `enableGenerateCompiledEndpointRules` | 350 |
| `verifiedSimpleMethods` / `excludedSimpleMethods` | 121 / 66 |
| `interceptors` | 13 |
| `generateEndpointClientTests` | 13 |
| `serviceSpecificHttpConfig`, `utilitiesMethod`, `syncClientDecorator`, `multipartCustomization`, ... | 1-4 each |

**Moves to transforms** — these edit the model, which is exactly what a
`ProjectionTransformer` does:

| Key | Services | Notes |
|---|---|---|
| `shapeModifiers` | 27 | member excludes, injects, type changes |
| `renameShapes` | 8 | Smithy has an identically named built-in transform |
| `deprecatedOperations` | 8 | apply `@deprecated` |
| `deprecatedShapes` | 4 | apply `@deprecated` |
| `operationModifiers` | 2 | |
| `shapeSubstitutions` | 2 | |
| `customSdkShapes` | 1 | add shapes, as in the demo above |
| `attachPayloadTraitToMember` | 1 | apply `@httpPayload` |

Some need no code at all, because Smithy ships the transform. S3's rename is the
clearest case:

```jsonc
// today, in customization.config
"renameShapes": { "Error": "S3Error", "Object": "S3Object" }
```

```json
// as a built-in Smithy transform, no Java required
{ "name": "renameShapes",
  "args": { "renamed": { "com.amazonaws.s3#Error":  "com.amazonaws.s3#S3Error",
                         "com.amazonaws.s3#Object": "com.amazonaws.s3#S3Object" } } }
```

Others need a custom transform. S3's `shapeModifiers` is the harder shape of problem —
changing `S3Object$Size` to emit as `long`, injecting `GetObjectOutput$ExpiresString`,
deprecating `Expires` — but it is all model editing, which is a good fit. Smithy's
built-in `changeTypes` covers part of it; member injection needs Java.

A practical consequence: because a transform is real code with tests, these
customizations become debuggable and unit-testable in a way that JSON config entries
interpreted deep inside the generator are not.

## Running with the Smithy CLI

The same `smithy-build.json` builds outside Maven, which is useful for iterating on a
model or a transform without a full Maven cycle:

```
cd services/account
AWS_SDK_JAVA_VERSION=2.41.29-SNAPSHOT smithy build
```

Run it from the module directory, since there is no mojo to inject the base directory
for relative paths in plugin settings.

**One gotcha.** The packaged `smithy` launcher fails:

```
NoClassDefFoundError: javax/lang/model/type/TypeVisitor
```

The Homebrew `smithy-cli` hardcodes its own `JAVA_HOME` and ships a jlink-trimmed
runtime with six modules; `java.compiler`, which provides `javax.lang.model`, is not one
of them, and our generator is built on JavaPoet. This is a property of how the CLI is
packaged, not something the plugin can work around. Run the same CLI on a full JDK:

```
cd services/account
AWS_SDK_JAVA_VERSION=2.41.29-SNAPSHOT "$JAVA_HOME/bin/java" \
  -cp "/opt/homebrew/Cellar/smithy-cli/1.73.0/libexec/lib/*" \
  software.amazon.smithy.cli.SmithyCli build
```

That reports `Validated 663 shapes`, builds both projections, and produces output
identical to Maven's.

## Status and known gaps

Verified: `account` builds from `smithy-build.json` under Maven and the CLI with
identical output; the transform is applied by both; a C2J service
(`pinpointsmsvoice`) is byte-for-byte unaffected; the full non-`quick` build passes
checkstyle, spotbugs, dependency analysis, and 567 tests.

Before this is more than a prototype:

- **Paginators and waiters are dropped** on the Smithy path.
  `SmithyIntermediateModelBuilder.translatePaginators()` and `translateWaiters()` are
  `TODO` stubs returning `none()`. `account` ships a `paginators-1.json` that is
  currently ignored. This is the biggest gap and it is invisible to output diffing
  against another Smithy-generated baseline.
- **Only `restJson1` translates.** `ProtocolUtils.resolveProtocol` maps that one
  protocol trait; anything else fails with a clear message. This blocks S3 (`restXml`),
  so it is on the critical path for any real migration.
- **No unit tests** for the new plugin, settings parsing, or the mojo path yet.
- **Validation is now on.** The prototype dropped the POC's `disableValidation()` and
  `account` passes cleanly, but other services may surface model errors that need
  fixing or suppressing.

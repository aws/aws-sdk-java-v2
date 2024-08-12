# AWS SDK for Java v2 Migration Tool

## Description
This modules contains [OpenRewrite][open-rewrite] recipes to automate migration from the AWS SDK for Java v1 to the AWS SDK for Java v2.

## Usage

You can use [OpenRewrite Maven plugin][open-rewrite-plugin] to start the migration. See [Running OpenRewrite Recipes][open-rewrite-usage] for more information.
To get started, you can either perform a dry run or run directly.

- Dry Run 

With this mode, it generates diff logs in the console as well as diff file in the `target` folder.
Note that you need to replace `{sdkversion}` with the actual SDK version. See [Maven Central][maven-central] to 
find the latest version.

```
mvn org.openrewrite.maven:rewrite-maven-plugin:dryRun \
  -Drewrite.recipeArtifactCoordinates=software.amazon.awssdk:v2-migration:{sdkversion} \
  -Drewrite.activeRecipes=software.amazon.awssdk.v2migration.AwsSdkJavaV1ToV2
```

- Run

With this mode, it runs the SDK recipes and applies the changes locally.

```
mvn org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=software.amazon.awssdk:v2-migration:{sdkversion} \
  -Drewrite.activeRecipes=software.amazon.awssdk.v2migration.AwsSdkJavaV1ToV2
```


## Development

To build this module locally for fast development, run the following command.

```
mvn clean install -pl :bom-internal,:bom,:v2-migration -P quick --am
```

### Testing

There are two types of tests available: unit tests and end-to-end functional tests.
- Unit tests

Unit tests reside in the test folder in this module. They use [RewriteTest][rewrite-test] interface

- End-to-end functional tests

End-to-end functional tests are in [v2-migration-tests module][v2-migration-tests]. It contains
sample applications using the AWS SDK for Java v1 and compares the transformed code with the expected v2 
code and ensures it compiles.

### Documentation

For further resources on performing the migration, see our [Developer Guide][developer-guide].

[open-rewrite]: https://docs.openrewrite.org/
[open-rewrite-usage]: https://docs.openrewrite.org/running-recipes
[open-rewrite-plugin]: https://docs.openrewrite.org/reference/rewrite-maven-plugin
[maven-central]: https://central.sonatype.com/artifact/software.amazon.awssdk/v2-migration
[rewrite-test]:https://docs.openrewrite.org/authoring-recipes/recipe-testing#rewritetest-interface
[v2-migration-tests]:../test/v2-migration-tests
[developer-guide]: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/migration.html
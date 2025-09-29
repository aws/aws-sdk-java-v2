# AWS SDK for Java v2 Migration Tool

## Description
This module contains [OpenRewrite][open-rewrite] recipes to automate migration from the AWS SDK for Java v1 to the 
AWS SDK for Java v2.

While the majority of v1 code is supported by recipes that transform to the v2 equivalent, there are some classes and 
methods not covered by the migration tool. For these classes and methods, refer to our 
[Developer Guide][developer-guide-steps] and [API Reference][api-reference-v2] to manually migrate your code.

## Usage

For detailed steps on using the tool, see our [Developer Guide][developer-guide].

### Maven Project

To transform a Maven project, run the following command from your project root directory:

```
mvn org.openrewrite.maven:rewrite-maven-plugin:6.17.0 \
  -Drewrite.recipeArtifactCoordinates=software.amazon.awssdk:v2-migration:2.34.0 \
  -Drewrite.activeRecipes=software.amazon.awssdk.v2migration.AwsSdkJavaV1ToV2
```

**Note:** Newer OpenRewrite versions may not be compatible. If errors occur during the transforms, specify the 
[SDK supported version][maven-plugin-version], e.g., `6.17.0`, and run the command again.

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

[open-rewrite]: https://docs.openrewrite.org/
[rewrite-test]: https://docs.openrewrite.org/authoring-recipes/recipe-testing#rewritetest-interface
[v2-migration-tests]: ../test/v2-migration-tests
[developer-guide]: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/migration-tool.html
[developer-guide-steps]: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/migration-steps.html
[api-reference-v2]: https://sdk.amazonaws.com/java/api/latest/index.html
[maven-plugin-version]: https://github.com/aws/aws-sdk-java-v2/blob/master/test/v2-migration-tests/src/test/java/software/amazon/awssdk/v2migrationtests/MavenTestBase.java#L54
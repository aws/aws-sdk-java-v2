# AWS SDK for Java v2 Migration Tool

## Description
This modules contains [OpenRewrite][open-rewrite] recipes to automate migration from the AWS SDK for Java v1 to the AWS SDK for Java v2.

## Usage

For steps on performing the migration, see our [Developer Guide][developer-guide].

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
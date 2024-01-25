# AWS SDK for Java v2 Migration Tool

## Description
This is a migration tool to automate migration from AWS SDK for Java v1 to AWS SDK for Java v2. 
It uses [OpenRewrite][open-rewrite].

## Usage

You can use [OpenRewrite Maven plugin][open-rewrite-plugin] to start the migration. See [Running OpenRewrite Recipes][open-rewrite-usage] for more information.
To get started, you can either perform a dry run or run directly.

- Dry Run 

With this mode, it generates diff logs in the console as well as diff file in the `target` folder.
Note that you need to replace `{sdkversion}` with the actual SDK version. See [Maven Central][maven-central] to 
find the latest version.

```
mvn org.openrewrite.maven:rewrite-maven-plugin:dryRun \
  -Drewrite.recipeArtifactCoordinates=software.amazon.awssdk:migration-tool:{sdkversion}
  -Drewrite.activeRecipes=software.amazon.awssdk.UpgradeJavaSdk2
```

- Run

With this mode, it runs the SDK recipes and applies the changes locally.

```
mvn org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=software.amazon.awssdk:migration-tool:{sdkversion}
  -Drewrite.activeRecipes=software.amazon.awssdk.UpgradeJavaSdk2
```

[open-rewrite]: https://docs.openrewrite.org/
[open-rewrite-usage]: https://docs.openrewrite.org/running-recipes
[open-rewrite-plugin]: https://docs.openrewrite.org/reference/rewrite-maven-plugin
[maven-central]: https://central.sonatype.com/artifact/software.amazon.awssdk/migration-tool
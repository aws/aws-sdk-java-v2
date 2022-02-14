# AWS SDK Native Image Test

## Description
This module contains a sample application using AWS SDK for Java 2.x, and it
is used to test GraalVM Native Image compilation.

## Prerequisites

To run the tests, you need to have GraalVM and Native Image set up properly in your workspace.
See [Setting up GraalVM with native-image support](https://graalvm.github.io/native-build-tools/latest/graalvm-setup.html)

## How to run

You can run the integration tests by using the following commands.

```
mvn clean install -pl :sdk-native-image-test -P quick --am

mvn clean install -pl :bom-internal,:bom

cd test/sdk-native-image-test

# build the image
mvn clean package -P native-image

# execute the image
target/sdk-native-image-test
```



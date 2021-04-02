# AWS SDK Native Image Test

## Description
This module contains a sample application using AWS SDK for Java 2.x, and it
is used to test GraalVM Native Image compilation.

## How to run
```
mvn clean install -pl :sdk-native-image-test -P quick --am

mvn clean install -pl :bom-internal,:bom

cd test/sdk-native-image-test

# build the image
mvn clean package -P native-image

# execute the image
target/sdk-native-image-test
```



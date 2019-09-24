# SDK Stability Regression Tests

## Description
This module contains stability regression tests.

We define "stable" to mean that the SDK does not encounter any client-side errors for identified simple expected load 
scenarios, and the number of transient service or network I/O related errors are relatively small.

## Test Case Acceptance

As these tests will be running against live, running services, we can expect some subset of requests to fail due to transient 
failures, network disruptions, throttling, etc. We cannot expect that all requests will always succeed. Instead, we will establish 
that 5% or less of the total number of requests sent for a test case are allowed to fail. We specify that only errors that extend 
from SdkServiceException, or a form of network error such as IOException or ReadTimeoutException may be counted towards the 5%. 
Any other error type, such as SdkClientException will fail the test.


## How to run

- Run from your IDE

- Run from maven command line

```
mvn clean install -P stability-tests -pl :stability-tests
```

- Build JAR and use the executable JAR

First add tests to TestRunner Class, then run the following command.

```
mvn clean install -pl :stability-tests --am -P quick
mvn clean install -pl :bom-inernal
cd test/stability-tests
mvn package -P test-jar
java -jar target/stability-tests-uber.jar
```

## Adding New Tests

- The tests are built using [JUnit 5](https://junit.org/junit5/). Make sure you are using the correct APIs and mixing of
Junit 4 and Junit 5 APIs on the same test can have unexpected results.

- All tests should have the suffix of `StabilityTests`, eg: `S3StabilityTests`




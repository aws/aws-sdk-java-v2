# SDK Regression Tests for Amazon S3

## Description
This module contains SDK regression tests for Amazon S3 streaming operations with various SDK configurations.


## How to run

### Credentials

The tests require valid AWS credentials to be available in the default credential file under the `aws-test-account` profile.

### Run the tests

- Run from your IDE

- Run from maven command. Include the class you want to run with the `regression.test` property 

```
mvn clean install -P s3-regression-tests -pl :s3-tests -am -T1C -Dregression.test=DownloadStreamingRegressionTesting
```

## Adding New Tests

- The tests are built using [JUnit 5](https://junit.org/junit5/). Make sure you are using the correct APIs and mixing of
  Junit 4 and Junit 5 APIs on the same test can have unexpected results.

- All tests should have the suffix of `RegressionTesting`, eg: `DownloadStreamingRegressionTesting`




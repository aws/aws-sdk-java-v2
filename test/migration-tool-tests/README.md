# AWS SDK Migration Tool Tests

## Description
This module is used to test `migration-tool`. It contains an application that uses the AWS SDK for Java v1
and performs `mvn open:rewrite` to migrate it to the AWS SDK for Java v2 and compares the transformed code
with the expected code for verification. The test code is in [run-test](./src/test/resources/run-test).

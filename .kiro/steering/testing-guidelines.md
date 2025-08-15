---
title: Testing Guidelines for AWS SDK v2
inclusion: fileMatch
fileMatchPattern: "**/{test,it}/**/*.java"
---

# Testing Guidelines for AWS SDK v2

## General Guidelines

- **Prefer JUnit 5** for new test classes
- **Prefer AssertJ** over Hamcrest for assertions
- Create dedicated test method for each test scenario
- Prefer parameterized tests for testing multiple similar cases
- Test names **SHOULD** follow `methodToTest_when_expectedBehavior` or `given_when_then` pattern
- When testing a module with dependencies, creating a constructor for dependency injection is preferred over exposing getter/setter APIs just for testing
- Test-only methods/constructors **MUST NOT** be public
- While test coverage is not a strict requirement, we **SHOULD** aim to cover 80% of the code

## Test Naming Conventions

Test names **SHOULD** follow `methodToTest_when_expectedBehavior` pattern:

- Example: `close_withCustomExecutor_shouldNotCloseCustomExecutor`
- Example: `uploadDirectory_withDelimiter_filesSentCorrectly`

This naming convention makes it clear:
1. What method is being tested
2. Under what conditions it's being tested
3. What behavior is expected

## Unit Testing Best Practices

- Each test should focus on a single behavior or aspect
- Use descriptive test names that explain the test's purpose
- Arrange-Act-Assert (AAA) pattern:
  - Arrange: Set up the test data and conditions
  - Act: Perform the action being tested
  - Assert: Verify the expected outcome
- Use appropriate assertions for the specific condition being tested
- Avoid test interdependencies - tests should be able to run in any order
- Clean up resources in @After or @AfterEach methods
- Unit tests **MUST** be added using EqualsVerifier to ensure all fields are properly included in equals and hashCode implementations:
  ```java
  @Test
  public void equalsHashCodeTest() {
      EqualsVerifier.forClass(YourClass.class)
                    .withNonnullFields("requiredFields")
                    .verify();
  }
  ```
- See example implementation in `core/regions/src/test/java/software/amazon/awssdk/regions/PartitionEndpointKeyTest.java`

## Mocking Guidelines

- Mock external dependencies, not the class under test
- Only mock what you need to control for the test
- Prefer constructor injection to make dependencies mockable
- Use Mockito's verify() to ensure interactions with mocks are as expected
- Avoid excessive stubbing - it can make tests brittle and hard to maintain
- [Wiremock](https://wiremock.org/) is commonly used to mock server responses in functional tests

## Testing Asynchronous Code

- Use appropriate mechanisms to wait for async operations to complete
- Set reasonable timeouts to prevent tests from hanging
- For CompletableFuture:
  - Use `join()` or `get()` with timeout in tests to wait for completion
  - Test both successful completion and exceptional completion paths
- For Reactive Streams:
  - Use StepVerifier or similar tools to test reactive flows
  - Test backpressure handling
  - Test cancellation scenarios
  - Test error propagation
  - For Publisher/Subscriber implementations, use TCK tests (see [Reactive Streams TCK Tests](#reactive-streams-tck-tests) section)

## Test Suites

### Unit Tests

Unit tests verify the behaviors of an individual unit of source code.

- **Goal**: Verify behaviors of a specific component and catch regressions
- **Location**: Under `src/test` directory in each module
- **When to add**: New unit tests **SHOULD** be added for any new changes
- **Naming Convention**: ClassNameTest, MethodNameTest
- **Test Automation**: Run for every PR and before release
- **Example**: [S3TransferManagerTest](https://github.com/aws/aws-sdk-java-v2/blob/master/services-custom/s3-transfer-manager/src/test/java/software/amazon/awssdk/transfer/s3/internal/S3TransferManagerTest.java)

### Functional Tests

Functional tests verify end-to-end behaviors and functionalities of an SDK feature.

- **Goal**: Verify end-to-end behaviors of an SDK feature
- **Location**:
  - Generated SDK common functionalities: In [codegen-generated-classes-test](https://github.com/aws/aws-sdk-java-v2/tree/master/test/codegen-generated-classes-test) module and [protocol-test](https://github.com/aws/aws-sdk-java-v2/tree/master/test/protocol-tests) module
  - Service-specific functionalities: Under `src/test` directory in that service module
  - High-level libraries (HLL): Under `src/test` directory in that HLL module
- **When to add**: Functional tests **SHOULD** be added for new SDK features or critical bug fixes
- **Naming Convention**: BehaviorTest, OperationTest
- **Test Automation**: Run for every PR and before release
- **Examples**: [WaitersSyncFunctionalTest](https://github.com/aws/aws-sdk-java-v2/blob/2532ec4f8ab36bab545689a2406d6a61d1696650/test/codegen-generated-classes-test/src/test/java/software/amazon/awssdk/services/waiters/WaitersSyncFunctionalTest.java), [SelectObjectContentTest](https://github.com/aws/aws-sdk-java-v2/blob/master/services/s3/src/test/java/software/amazon/awssdk/services/s3/functionaltests/SelectObjectContentTest.java)

### Reactive Streams TCK Tests

[Reactive Streams TCK tests](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck) verify Reactive Streams implementations against the rules defined in [the Reactive Streams Specification](https://github.com/reactive-streams/reactive-streams-jvm).

- **Goal**: Ensure implementations are compliant with the spec
- **Location**: Under `src/test` directory in each module. The tests always have `TckTest` suffix
- **When to add**: New reactive streams TCK tests **MUST** be added when a new Subscriber/Publisher implementation is added
- **Naming Convention**: ClassNameTckTest
- **Test Automation**: Run for every PR and before release
- **Example**: [FileAsyncRequestPublisherTckTest](https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/test/java/software/amazon/awssdk/core/async/FileAsyncRequestPublisherTckTest.java)

### Integration Tests

Integration tests verify end-to-end behaviors of the SDK with real AWS services.

- **Goal**: Verify end-to-end behaviors of an SDK feature with real services
- **Location**: Under `src/it` directory in each module. The tests always have `IntegrationTest` suffix
- **When to add**: Integration tests **MAY** be added for new SDK features (functional tests are preferred)
- **Naming Convention**: ClassNameIntegrationTest, OperationIntegrationTest, BehaviorIntegrationTest
- **Important Notes**: Resources **MUST** be cleaned up after each test
- **Test Automation**: Run before release
- **Example**: [S3TransferManagerDownloadDirectoryIntegrationTest](https://github.com/aws/aws-sdk-java-v2/blob/master/services-custom/s3-transfer-manager/src/it/java/software/amazon/awssdk/transfer/s3/S3TransferManagerDownloadDirectoryIntegrationTest.java)

### Stability Tests

[Stability regression tests](https://github.com/aws/aws-sdk-java-v2/tree/master/test/stability-tests) detect stability regressions by sending high concurrent requests to AWS services.

- **Goal**: Detect SDK errors in high-concurrency environment
- **Location**: Under `src/it` directory in `stability-tests` module. The tests always have `StabilityTest` suffix
- **When to add**: Stability tests **SHOULD** be added when a critical feature is being developed such as an HTTP Client
- **Naming Convention**: ClassNameStabilityTest
- **Test Automation**: Run before release
- **Example**: [KinesisStabilityTest](https://github.com/aws/aws-sdk-java-v2/blob/master/test/stability-tests/src/it/java/software/amazon/awssdk/stability/tests/kinesis/KinesisStabilityTest.java)

### Long Running Canaries

Long-running canaries continuously send requests to real or mock services at a constant rate, collecting resource usage, error rate, and latency metrics.

- **Goal**: Detect resource leaks, latency increases, and performance issues
- **Location**: In a separate internal repository
- **When to add**: Canaries **SHOULD** be added when a critical feature is being developed such as an HTTP Client
- **Naming Convention**: FeatureTxnCreator
- **Test Automation**: Always running, deployed weekly

### Performance Tests

Performance tests are built using [Java Microbenchmark Harness (JMH)](https://github.com/openjdk/jmh) and measure throughput and latency.

- **Goal**: Detect performance regression
- **Location**: In [sdk-benchmarks](https://github.com/aws/aws-sdk-java-v2/tree/master/test/sdk-benchmarks) module
- **When to add**: Benchmark code **SHOULD** be added for critical features or when verifying SDK performance impact
- **Naming Convention**: FeatureBenchmark
- **Test Automation**: No automation, manually triggered
- **Example**: [ApacheHttpClientBenchmark](https://github.com/aws/aws-sdk-java-v2/blob/master/test/sdk-benchmarks/src/main/java/software/amazon/awssdk/benchmark/apicall/httpclient/sync/ApacheHttpClientBenchmark.java)

### Protocol Tests

Protocol tests verify SDK behavior with different [protocols](https://smithy.io/2.0/aws/protocols/index.html) including rest-json, rest-xml, json, xml, and query protocols.

- **Goal**: Verify marshalling/unmarshalling with different protocols
- **Location**: In [protocol-tests](https://github.com/aws/aws-sdk-java-v2/tree/master/test/protocol-tests) module and [protocol-tests-core](https://github.com/aws/aws-sdk-java-v2/tree/master/test/protocol-tests-core) module
- **When to add**: Protocol tests **MUST** be added if we are adding support for a new structure
- **Naming Convention**: XmlProtocolTest
- **Test Automation**: Run for every PR and before release
- **Example**: [RestJsonProtocolTest](https://github.com/aws/aws-sdk-java-v2/blob/master/test/protocol-tests/src/test/java/software/amazon/awssdk/protocol/tests/RestJsonProtocolTest.java)

## Test Coverage

- Aim for high test coverage, especially for critical paths
- Don't focus solely on line coverage - consider branch and path coverage
- Identify and test edge cases and boundary conditions
- Test error handling and exception paths
- While test coverage is not a strict requirement, we **SHOULD** aim to cover 80% of the code

## References

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Wiremock Documentation](https://wiremock.org/)
- [AWS SDK for Java Developer Guide - Testing](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/testing.html)
- [Reactive Streams Specification](https://github.com/reactive-streams/reactive-streams-jvm)
- [Reactive Streams TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck)
# Business Metrics Implementation Guidelines

## Table of Contents
- [Overview](#overview)
- [Core Principles](#core-principles)
- [Implementation Patterns](#implementation-patterns)
- [Performance Considerations](#performance-considerations)
- [Versioning and Backward Compatibility](#versioning-and-backward-compatibility)
- [Testing Requirements](#testing-requirements)
- [Examples and References](#examples-and-references)

## Overview

Business metrics are short identifiers added to the User-Agent header for telemetry tracking. They help AWS understand feature usage patterns across the SDK. This document provides guidelines for implementing business metrics in the AWS SDK for Java v2, based on team architectural decisions and performance considerations.

**Key Concepts:**
- **Business Metrics**: Short string identifiers (e.g., "S", "A", "B") that represent feature usage
- **User-Agent Header**: HTTP header where business metrics are included for telemetry

## Core Principles

### Feature-Centric Placement

**MUST** add business metrics when we finalize/know for sure that the feature is being used. To account for cases where features can be overridden, add business metrics at the point where feature usage is confirmed and finalized.

**Rationale:** Based on team discussion, this approach was chosen over centralized placement in `ApplyUserAgentStage` because:
- **Better separation of concerns**: `ApplyUserAgentStage` remains ignorant of internal feature implementation details
- **Easier maintenance**: Feature refactoring doesn't require updating multiple places
- **Reduced coupling**: Avoids tight coupling between stages and feature implementations


## Implementation Patterns

For GZIP compression, we know that the request is compressed in `CompressRequestStage`, so we add the business metric there. For checksums, we know that checksum is resolved in `HttpChecksumStage`, so we add the business metric there.

```java
// Example from CompressRequestStage
private void updateContentEncodingHeader(SdkHttpFullRequest.Builder input,
                                         Compressor compressor,
                                         ExecutionAttributes executionAttributes) {
    // Record business metric when compression is actually applied
    executionAttributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS)
                       .addMetric(BusinessMetricFeatureId.GZIP_REQUEST_COMPRESSION.value());
    
    if (input.firstMatchingHeader(COMPRESSION_HEADER).isPresent()) {
        input.appendHeader(COMPRESSION_HEADER, compressor.compressorType());
    } else {
        input.putHeader(COMPRESSION_HEADER, compressor.compressorType());
    }
}

// Example from HttpChecksumStage - showing where business metrics are recorded
@Override
public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, 
                                          RequestExecutionContext context) throws Exception {
    // ... feature resolution logic ...
    
    SdkHttpFullRequest.Builder result = processChecksum(request, context);
    
    // Record business metrics after feature is finalized
    recordChecksumBusinessMetrics(context.executionAttributes());
    
    return result;
}
```

## Performance Considerations

### Avoid Request Mutation for Business Metrics

**SHOULD NOT** use request mutation (`.toBuilder().build()`) for adding business metrics as it creates unnecessary object copies and performance overhead.

**Avoid This Pattern** (Used in waiter/paginator implementations):
```java
// Creates new objects (performance overhead)
Consumer<AwsRequestOverrideConfiguration.Builder> userAgentApplier = 
    b -> b.addApiName(ApiName.builder().name("sdk-metrics").version("B").build());

AwsRequestOverrideConfiguration overrideConfiguration =
    request.overrideConfiguration().map(c -> c.toBuilder().applyMutation(userAgentApplier).build())
    .orElse(AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build());

return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
```

 **Prefer This ExecutionAttributes Pattern**:
```java
// Direct business metrics collection (no object creation)
private void recordFeatureBusinessMetric(ExecutionAttributes executionAttributes) {
    BusinessMetricCollection businessMetrics = 
        executionAttributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
    
    if (businessMetrics != null) {
        businessMetrics.addMetric(BusinessMetricFeatureId.FEATURE_ID.value());
    }
}
```

In cases of high-level features (for example, Transfer Manager, Batch Manager, Cross-Region operations) that are resolved before ExecutionContext is built and we don't have access to ExecutionAttributes, prefer using `AwsExecutionContextBuilder.resolveUserAgentBusinessMetrics()` if the feature can be detected from client configuration or execution parameters (for example, retry mode from client config or RPC v2 CBOR protocol from execution parameters). If there is no option then request mutation is acceptable.

## Versioning and Backward Compatibility

### Business Metrics Changes Are Backward Compatible

In general, changes to existing business metrics can be treated as backward compatible since business metrics don't affect SDK functionality or customer code behavior, so customer applications remain unaffected. Business metrics are purely observational telemetry, so changes like modifying an existing business metric are safe changes. If we are making changes to existing business metrics, then discuss with the team and do a minor version bump if needed so that teams can identify the new metric from that version.


## Testing Requirements

### Functional Testing with Mock HTTP Clients

**MUST** use functional testing with mock HTTP clients instead of interceptor-based testing.

**Why Mock HTTP Clients:**
- **Reliability**: Tests are not affected by interceptor ordering changes or SDK internal modifications
- **End-to-end verification**: Tests verify the complete flow from feature usage to User-Agent header inclusion
- **Simplicity**: Direct access to the final HTTP request without interceptor setup
- **Maintainability**: Tests remain stable even when internal pipeline stages are refactored

**Testing Pattern:**
1. Create a mock HTTP client and stub the response
2. Build the SDK client with the mock HTTP client
3. Execute the operation that should trigger the business metric
4. Extract the User-Agent header from the captured request
5. Verify the business metric is present using pattern matching

```java
@Test
void testBusinessMetric_withMockHttpClient() {
    MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
    mockHttpClient.stubNextResponse(HttpExecuteResponse.builder()
                                   .response(SdkHttpResponse.builder()
                                            .statusCode(200)
                                            .build())
                                   .build());
    
    // Create client with mock HTTP client and make request
    S3Client client = S3Client.builder()
                              .httpClient(mockHttpClient)
                              .build();
    
    client.listBuckets();
    
    // Extract User-Agent from the last request
    SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
    String userAgent = lastRequest.firstMatchingHeader("User-Agent").orElse("");
    
    // Verify business metric is present
    assertThat(userAgent).matches(METRIC_SEARCH_PATTERN.apply("A"));
}
```

**For Async Clients:**
Use `MockAsyncHttpClient` with the same pattern for testing async operations.

### Reference Test Files
- `test/auth-tests/src/it/java/software/amazon/awssdk/auth/source/UserAgentProviderTest.java`
- `test/codegen-generated-classes-test/src/test/java/software/amazon/awssdk/services/rpcv2cbor/RpcV2CborUserAgentTest.java`


## Examples and References

Here are some example implementations:

### Key Files and Classes
- **BusinessMetricFeatureId**: `core/sdk-core/src/main/java/software/amazon/awssdk/core/useragent/BusinessMetricFeatureId.java`
- **BusinessMetricsUtils**: `core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/useragent/BusinessMetricsUtils.java`
- **ApplyUserAgentStage**: `core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/http/pipeline/stages/ApplyUserAgentStage.java`
- **HttpChecksumStage**: `core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/http/pipeline/stages/HttpChecksumStage.java`
- **CompressRequestStage**: `core/sdk-core/src/main/java/software/amazon/awssdk/core/internal/http/pipeline/stages/CompressRequestStage.java`
- **AuthSchemeInterceptorSpec**: `codegen/src/main/java/software/amazon/awssdk/codegen/poet/auth/scheme/AuthSchemeInterceptorSpec.java`

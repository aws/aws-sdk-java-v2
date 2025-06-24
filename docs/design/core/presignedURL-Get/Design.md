# Design Document (S3 Pre-signed URL GET)

## Introduction

This design introduces S3 object downloads using pre-signed URLs in AWS SDK Java v2, providing feature parity with [v1](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/transfer/PresignedUrlDownload.html). Some customers have described a need for downloading S3 objects through pre-signed URLs while maintaining Client side SDK benefits like automatic retries, metrics collection, and typed response objects.

This document proposes how this functionality should be implemented in the Java SDK v2, addressing customer-requested features ([GitHub Issue #2731](https://github.com/aws/aws-sdk-java-v2/issues/2731), [GitHub Issue #181](https://github.com/aws/aws-sdk-java-v2/issues/181)) by reducing complexity and improving usability for temporary access scenarios.

## Design Review

Look at decision log here: [Decision Log Section](DecisionLog.md)

The Java SDK team has decided to implement a separate `PresignedUrlManager`. The team chose the helper API pattern over direct `S3Client` integration to maintain clean separation of concerns while preserving SDK functionality.

## Overview

The design introduces new helper APIs `AsyncPresignedUrlManager` and `PresignedUrlManager` which can be instantiated via the existing `S3AsyncClient` and `S3Client` respectively. These managers provide a clean abstraction layer that preserves SDK benefits while handling the unique requirements of pre-signed URL requests.

This design will implement only the GET /download function for presigned URLs.



## Proposed APIs

The v2 SDK will support a presigned URL manager for both sync and async clients that can leverage pre-signed URL downloads.

### Instantiation
Instantiating from existing client:

```java
// Async Presigned URL Manager
S3AsyncClient s3Client = S3AsyncClient.create();
AsyncPresignedUrlManager presignManager = s3Client.presignedManager();

// Sync Presigned URL Manager  
S3Client s3Client = S3Client.create();
PresignedUrlManager presignManager = s3Client.presignedManager();
```

### General Usage Examples

```java
// Create presigned URL request
PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
                                                .presignedUrl(presignedUrl)
                                                .rangeStart(0L)
                                                .rangeEnd(1024L)
                                                .build();

// Async usage
S3AsyncClient s3Client = S3AsyncClient.create();
AsyncPresignedUrlManager presignManager = s3Client.presignedManager();
CompletableFuture<GetObjectResponse> response = presignManager.getObject(request);

// Sync usage
S3Client s3Client = S3Client.create();
PresignedUrlManager presignManager = s3Client.presignedManager();
GetObjectResponse response = presignManager.getObject(request);
```

### AsyncPresignedUrlManager Interface

```java
/**
 * Presigned URL Manager class that implements presigned URL features for an async client.
 */
@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public interface AsyncPresignedUrlManager {

    /**
     * Downloads S3 objects using pre-signed URLs. Bypasses normal authentication
     * and endpoint resolution while maintaining SDK benefits like retries and metrics.
     *
     * @param request the presigned URL request containing URL and optional range parameters.
     * @return a CompletableFuture of the corresponding GetObjectResponse.
     */
    CompletableFuture<GetObjectResponse> getObject(PresignedUrlGetObjectRequest request);
    
    /**
     * Downloads S3 objects using pre-signed URLs with custom response transformation.
     *
     * @param request the presigned URL request.
     * @param responseTransformer custom transformer for processing the response.
     * @return a CompletableFuture of the transformed response.
     */
    <T> CompletableFuture<T> getObject(PresignedUrlGetObjectRequest request,
                                      AsyncResponseTransformer<GetObjectResponse, T> responseTransformer);
    
    // Additional getObject() overloads for file downloads, byte arrays, etc.
    // Standard Builder interface with client() and overrideConfiguration() methods
}
```

### PresignedUrlManager Interface

```java
/**
 * Presigned URL Manager class that implements presigned URL features for a sync client.
 */
@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public interface PresignedUrlManager {

    /**
     * Downloads S3 objects using pre-signed URLs. Bypasses normal authentication
     * and endpoint resolution while maintaining SDK benefits like retries and metrics.
     *
     * @param request the presigned URL request containing URL and optional range parameters.
     * @return the GetObjectResponse.
     */
    GetObjectResponse getObject(PresignedUrlGetObjectRequest request);
    
    /**
     * Downloads S3 objects using pre-signed URLs with custom response transformation.
     *
     * @param request the presigned URL request.
     * @param responseTransformer custom transformer for processing the response.
     * @return the transformed response.
     */
    <T> T getObject(PresignedUrlGetObjectRequest request,
                   ResponseTransformer<GetObjectResponse, T> responseTransformer);
    
    // Additional getObject() overloads for file downloads, byte arrays, etc.
    // Standard Builder interface with client() and overrideConfiguration() methods
}
```

### PresignedUrlGetObjectRequest

```java
/**
 * Request object for presigned URL GET operations.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class PresignedUrlGetObjectRequest 
        implements ToCopyableBuilder<PresignedUrlGetObjectRequest.Builder, PresignedUrlGetObjectRequest> {
    
    private final String presignedUrl;
    private final Long rangeStart;
    private final Long rangeEnd;
    
    // Standard getters: presignedUrl(), rangeStart(), rangeEnd()
    // Standard builder methods: builder(), toBuilder()
    // Standard Builder class with presignedUrl(), rangeStart(), rangeEnd() setter methods
}
```

## FAQ

### Why don't we implement presigned URL download/GET feature directly on the S3Client?

Three approaches were considered:

1. **Dedicated PresignedUrlManager (CHOSEN)**: Separate manager accessed via `s3Client.presignedManager()`
   - **Pros**: Clean separation, preserves SDK features, follows v2 patterns
   - **Cons**: New API surface for users to learn

2. **Direct S3Client Integration**: Add presigned URL methods directly to S3Client
   - **Pros**: Familiar interface, direct migration path from v1
   - **Cons**: Requires core interceptor changes, complex integration

3. **S3Presigner Extension**: Extend existing S3Presigner to execute URLs
   - **Pros**: Logical extension of presigner concept
   - **Cons**: Breaks current stateless presigner patterns

**Decision**: Option 1 provides clean separation while preserving SDK benefits and following established v2 utility patterns.cutePresignedGetObject(presignedRequest);

### Why doesn't PresignedUrlGetObjectRequest extend S3Request?

While extending S3Request would provide access to RequestOverrideConfiguration, many of these configurations (like credentials provider, signers) are not supported with presigned URL execution and could cause conflicts. Instead, we use a standalone request with only essential parameters (presignedUrl, rangeStart, rangeEnd). Internally, this gets wrapped in an encapsulated class that extends S3Request for use with ClientHandler.


## References

**GitHub feature requests:**
- [S3 Presigned URL Support #2731](https://github.com/aws/aws-sdk-java-v2/issues/2731)
- [Presigned URL GET Support #181](https://github.com/aws/aws-sdk-java-v2/issues/181)

**AWS Documentation:**
- [S3 Pre-signed URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/presigned-urls.html)

**SDK Documentation:**
- [AWS SDK for Java v1 implementation](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html)
- [S3 Client architecture patterns](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html)


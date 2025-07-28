# Design Document (S3 Pre-signed URL GET)

## Introduction

This design introduces S3 object downloads using pre-signed URLs in AWS SDK Java v2, providing feature parity with [v1](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/transfer/PresignedUrlDownload.html). Some customers have described a need for downloading S3 objects through pre-signed URLs while maintaining Client side SDK benefits like automatic retries, metrics collection, and typed response objects.

This document proposes how this functionality should be implemented in the Java SDK v2, addressing customer-requested features ([GitHub Issue #2731](https://github.com/aws/aws-sdk-java-v2/issues/2731), [GitHub Issue #181](https://github.com/aws/aws-sdk-java-v2/issues/181)) by reducing complexity and improving usability for temporary access scenarios.

## Design Review

Look at decision log here: [Decision Log Section](DecisionLog.md)

The Java SDK team has decided to implement a separate `AsyncPresignedUrlExtension`. The team chose the helper API pattern over direct `S3AsyncClient` integration to maintain clean separation of concerns while preserving SDK functionality.

## Overview

The design introduces a new helper API `AsyncPresignedUrlExtension` which can be instantiated via the existing `S3AsyncClient`. This extension provides a clean abstraction layer that preserves SDK benefits while handling the unique requirements of pre-signed URL requests.

This design will implement only the GET /download function for presigned URLs for the S3AsyncClient. The synchronous S3Client implementation is deferred to future work.



## Proposed APIs

The v2 SDK will support a presigned URL extension for the async client that can leverage pre-signed URL downloads.

### Instantiation
Instantiating from existing client:

```java
// Async Presigned URL Extension
S3AsyncClient s3Client = S3AsyncClient.create();
AsyncPresignedUrlExtension presignExtension = s3Client.presignedUrlExtension();
```

### General Usage Examples

```java
// Create presigned URL request
PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                .presignedUrl(presignedUrl)
                                                .range("range=0-1024")
                                                .build();

// Async usage
S3AsyncClient s3Client = S3AsyncClient.create();
AsyncPresignedUrlExtension presignedUrlExtension = s3Client.presignedUrlExtension();
CompletableFuture<GetObjectResponse> response = presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());
```

### AsyncPresignedUrlExtension Interface

```java
/**
 * Interface for presigned URL operations used by Async clients.
 */
@SdkPublicApi
public interface AsyncPresignedUrlExtension {
    
    /**
     * Downloads S3 objects using pre-signed URLs with custom response transformation.
     *
     * @param request the presigned URL request.
     * @param responseTransformer custom transformer for processing the response.
     * @return a CompletableFuture of the transformed response.
     */
    <T> CompletableFuture<T> getObject(PresignedUrlDownloadRequest request,
                                      AsyncResponseTransformer<GetObjectResponse, T> responseTransformer);
    
    // Additional getObject() overloads for file downloads, byte arrays, etc.
    // Standard Builder interface with client() and overrideConfiguration() methods
}
```

### PresignedUrlDownloadRequest

```java
/**
 * Request object for presigned URL GET operations.
 */
@SdkPublicApi
@ThreadSafe
public final class PresignedUrlDownloadRequest 
        implements ToCopyableBuilder<PresignedUrlDownloadRequest.Builder, PresignedUrlDownloadRequest> {
    
    private final URL presignedUrl;
    private final String range;
    
    // Standard getters: presignedUrl(), range()
    // Standard builder methods: builder(), toBuilder()
    // Standard Builder class with presignedUrl(), range() setter methods
}
```

## FAQ

### Why don't we implement presigned URL download/GET feature directly on the S3AsyncClient?

Three approaches were considered:

1. **Dedicated AsyncPresignedUrlExtension (CHOSEN)**: Separate extension accessed via `s3AsyncClient.presignedUrlExtension()`
   - **Pros**: Clean separation, preserves SDK features, follows v2 patterns
   - **Cons**: New API surface for users to learn

2. **Direct S3Client Integration**: Add presigned URL methods directly to S3Client
   - **Pros**: Familiar interface, direct migration path from v1
   - **Cons**: Requires core interceptor changes, complex integration, could confuse users by mixing presigned URL APIs with standard service-generated APIs

3. **S3Presigner Extension**: Extend existing S3Presigner to execute URLs
   - **Pros**: Logical extension of presigner concept
   - **Cons**: Breaks current stateless presigner patterns

**Decision**: Option 1 provides clean separation while preserving SDK benefits and following established v2 utility patterns.

### What about synchronous S3Client and S3 CRT Client support?

The synchronous S3Client implementation has been deferred to future work due to complexities around multipart download requirements. Support for S3CrtAsyncClient will also be added in future work once the AWS CRT team addresses current limitations with pre-signed URL handling.

### Why doesn't PresignedUrlDownloadRequest extend S3Request?

While extending S3Request would provide access to RequestOverrideConfiguration, many of these configurations (like credentials provider, signers) are not supported with presigned URL execution. Instead, we use a standalone request with only essential parameters (presignedUrl, range). Internally, this gets wrapped in an encapsulated class that extends S3Request for use with ClientHandler.

## References

**GitHub feature requests:**
- [S3 Presigned URL Support #2731](https://github.com/aws/aws-sdk-java-v2/issues/2731)
- [Presigned URL GET Support #181](https://github.com/aws/aws-sdk-java-v2/issues/181)

**AWS Documentation:**
- [S3 Pre-signed URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/presigned-urls.html)

**SDK Documentation:**
- [AWS SDK for Java v1 implementation](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html)
- [S3 Client architecture patterns](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html)


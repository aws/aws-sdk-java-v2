**Design:** New Feature, **Status:** [In Development](../README.md)

# Cross-Region S3 Client Design

## Overview

The [cross-region S3 client](https://github.com/aws/aws-sdk-java-v2/issues/52) is a feature supported in the AWS SDK for Java 1.x that automatically routes requests to the correct bucket region. This is useful for customers who do not know the region of the bucket beforehand. Without this support, customers would need to write their own logic to retrieve the region of a specific bucket for each request and redirect the request accordingly, which could be complex and error prone. This documentation proposes the design to implement it in the AWS SDK for Java 2.x.

## Specification 

The SDK 2.x will support this feature in the S3 sync client, S3 async client and the AWS-CRT based S3 client.

### Usage Examples

#### Example 1: enabling cross-region access in the S3 sync client

```
S3ClientBuilder s3ClientBuilder = S3Client.builder()
                                          .crossRegionAcccessEnabled(true)
                                          .build();
```

#### Example 2: enabling cross-region access in the S3 async client

```
// Java S3 async client
S3AsyncClient s3Client = S3AsyncClient.builder()
                                      .crossRegionAcccessEnabled(true)
                                      .build();

// AWS CRT-based S3 async client
S3AsyncClient s3CrtClient = S3AsyncClient.crtBuilder()
                                         .crossRegionAcccessEnabled(true)
                                         .build();
```

### Client Configuration

Users can enable this feature through a client configuration on the client builder, `crossRegionAccessEnabled`. It is default to false.

### Implementation Notes

The region retrieval and redirect logic is implemented in a customized `S3EndPointProvider`. If `crossRegionAccessEnabled` is true, the SDK will add this customized endpoint provider to the client, which will first attempt to retrieve the region of the bucket by sending a HeadObject request and then configure the region for that request. It uses an [LRU cache](https://github.com/aws/aws-sdk-java-v2/blob/master/utils/src/main/java/software/amazon/awssdk/utils/cache/lru/LruCache.java) to bypass HeadObject API call for frequently used buckets and access points for performance optimization.  

## Alternatives Considered

The alternative is to create a standalone S3 client that maintains a pool of S3 client instances configured with different regions. This approach is not recommended because 1) using multiple S3 clients means more resources will be used, which could confuse users 2) the implementation is more complex.

## Appendix: Java SDK v1 customer experience 

```
AmazonS3ClientBuilder.standard()
                     .withRegion(region)
                     .withForceGlobalBucketAccessEnabled(enableGlobalBucketAccess)
                     .build();
```


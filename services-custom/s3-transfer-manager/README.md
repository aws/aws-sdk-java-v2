## Overview

This project provides a much improved experience for S3 customers needing to easily perform uploads and downloads of
objects to and from S3 by providing the S3 S3TransferManager, a high level
library built on the [AWS Common Runtime S3 Client](https://github.com/awslabs/aws-crt-java).

## Getting Started

### Add a dependency for the transfer manager 

First, you need to include the dependency in your project.

```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3-transfer-manager</artifactId>
  <version>${awsjavasdk.version}-PREVIEW</version>
</dependency>
```

Note that you need to replace `${awsjavasdk.version}` with the latest
SDK version

### Instantiate the transfer manager
You can instantiate the transfer manager easily using the default settings

```java

S3TransferManager transferManager = S3TransferManager.create();
    
```

If you wish to configure settings, we recommend using the builder instead:
```java
S3TransferManager transferManager = 
    S3TransferManager.builder()
                     .s3ClientConfiguration(b -> b.credentialsProvider(credentialProvider)
                                                  .region(Region.US_WEST_2)
                                                  .targetThroughputInGbps(20.0)
                                                  .minimumPartSizeInBytes(10 * MB))
                     .build();
```

### Download an S3 object to a file
To download an object, you just need to provide the destion file path and the `GetObjectRequest` that should be used for the download.

```java
Download download = 
    transferManager.download(b -> b.destination(path)
                                   .getObjectRequest(r -> r.bucket("bucket")
                                                           .key("key")));
download.completionFuture().join();
```

### Upload a file to S3
To upload a file to S3, you just need to provide the source file path and the `PutObjectRequest` that should be used for the upload.

```java
Upload upload = transferManager.upload(b -> b.source(path)
                                             .putObjectRequest(r -> r.bucket("bucket")
                                                                     .key("key")));

upload.completionFuture().join();

```
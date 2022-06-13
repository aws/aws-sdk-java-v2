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
SDK version.

### Instantiate the transfer manager

You can instantiate the transfer manager easily using the default settings:

```java
S3TransferManager tm = S3TransferManager.create();
```

If you wish to configure settings, we recommend using the builder instead:


```java
S3TransferManager tm =
    S3TransferManager.builder()
                     .s3AsyncClient(S3CrtAsyncClient.builder()
                                                    .credentialsProvider(credentialProvider)
                                                    .region(Region.US_WEST_2)
                                                    .targetThroughputInGbps(20.0)
                                                    .minimumPartSizeInBytes(8 * MB))
                     .build();
```

### Upload a file to S3

To upload a file to S3, you just need to provide the source file path and the `PutObjectRequest` that should be used for the upload:

```java
FileUpload upload =
    tm.uploadFile(u -> u.source(Paths.get("myFile.txt"))
                        .putObjectRequest(p -> p.bucket("bucket").key("key")));
upload.completionFuture().join();
```

### Download an S3 object to a file

To download an object, you just need to provide the destination file path and the `GetObjectRequest` that should be used for the download:

```java
FileDownload download =
    tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("bucket").key("key"))
                          .destination(Paths.get("myFile.txt")));
download.completionFuture().join();
```

### Upload any content to S3

You may upload any arbitrary content to S3 by providing an `AsyncRequestBody`:

```java
Upload upload =
    tm.upload(u -> u.requestBody(AsyncRequestBody.fromString("Hello world"))
                    .putObjectRequest(p -> p.bucket("bucket").key("key")));
upload.completionFuture().join();
```

Refer to the static factory methods available in `AsyncRequestBody` for other content sources.

### Download an S3 object to a custom destination

You may download an object from S3 to a custom destination by providing an `AsyncResponseTransformer`:

*(This example buffers the entire object in memory and is not suitable for large objects.)*

```java
Download<ResponseBytes<GetObjectResponse>> download =
    tm.download(d -> d.getObjectRequest(g -> g.bucket("bucket").key("key"))
                      .responseTransformer(AsyncResponseTransformer.toBytes()));
download.completionFuture().join();
```

Refer to the static factory methods available in `AsyncResponseTransformer` for other destinations.

### Attach a TransferListener

To monitor a transfer's progress, you can include a `TransferListener` with your transfer request:

```java
FileUpload upload =
    tm.uploadFile(u -> u.source(Paths.get("myFile.txt"))
                        .putObjectRequest(p -> p.bucket("bucket").key("key"))
                        .overrideConfiguration(o -> o.addListener(LoggingTransferListener.create())));
upload.completionFuture().join();
```

You can provide your own implementation of a `TransferListener` to implement progress-bar-type functionality.
## Overview

The S3 Transfer Manager is a high-level transfer utility built on top of the asynchronous S3 client. 
It provides a simple API to allow you to transfer files and directories between your application 
and Amazon S3. The S3 Transfer Manager also enables you to monitor a transfer's progress in real-time, 
as well as pause the transfer for execution at a later time.

## Getting Started

### Add a dependency for the S3 Transfer Manager 

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

### Instantiate the S3 Transfer Manager

You can instantiate the transfer manager easily using the default settings:

```java
S3TransferManager transferManager = S3TransferManager.create();
```

If you wish to configure settings, or use an underlying CRT-based S3 client you have already constructed, 
we recommend using the builder instead:


```java
S3AsyncClient s3AsyncClient =
    S3AsyncClient.crtBuilder()
                 .credentialsProvider(DefaultCredentialsProvider.create())
                 .region(Region.US_WEST_2)
                 .targetThroughputInGbps(20.0)
                 .minimumPartSizeInBytes(8 * MB)
                 .build();

S3TransferManager transferManager =
    S3TransferManager.builder()
                     .s3Client(s3AsyncClient)
                     .build();
```

### Transfer a single object

#### Upload a file to S3 and log the upload’s process with a TransferListener
To upload a file to Amazon S3, you need to provide the source file path and a PutObjectRequest specifying the target bucket and key.

```java
S3TransferManager transferManager = S3TransferManager.create();

UploadFileRequest uploadFileRequest =
    UploadFileRequest.builder()
                     .putObjectRequest(req -> req.bucket("bucket").key("key"))
                     .addTransferListener(LoggingTransferListener.create())
                     .source(Paths.get("myFile.txt"))
                     .build();

FileUpload upload = transferManager.uploadFile(uploadFileRequest);

    // Wait for the transfer to complete
    upload.completionFuture().join();
```

#### Download an S3 object to a local file and log the download’s progress with a TransferListener

To download an object, you need to provide the destination file path and a GetObjectRequest specifying the source bucket and key.

```java
S3TransferManager transferManager = S3TransferManager.create();

DownloadFileRequest downloadFileRequest =
DownloadFileRequest.builder()
.getObjectRequest(req -> req.bucket("bucket").key("key"))
.destination(Paths.get("myFile.txt"))
.addTransferListener(LoggingTransferListener.create())
.build();

FileDownload download = transferManager.downloadFile(downloadFileRequest);

// Wait for the transfer to complete
download.completionFuture().join();
```

#### Copy an S3 object from one location to another
To copy an object, you need to provide a CopyObjectRequest with source and destination location.

```
S3TransferManager transferManager = S3TransferManager.create();
CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                                                       .sourceBucket("source_bucket")
                                                       .sourceKey("source_key")
                                                       .destinationBucket("dest_bucket")
                                                       .destinationKey("dest_key")
                                                       .build();
CopyRequest copyRequest = CopyRequest.builder()
                                     .copyObjectRequest(copyObjectRequest)
                                     .build();

Copy copy = transferManager.copy(copyRequest);

// Wait for the transfer to complete
CompletedCopy completedCopy = copy.completionFuture().join();
```

### Transfer multiple objects in the same directory

#### Upload a local directory to an S3 bucket

To upload a local directory recursively to an S3 bucket, you need to provide the source directory and the target bucket.

```java
S3TransferManager transferManager = S3TransferManager.create();
DirectoryUpload directoryUpload = transferManager.uploadDirectory(UploadDirectoryRequest.builder()
                                                 .sourceDirectory(Paths.get("source/directory"))
                                                 .bucket("bucket")
                                                 .build());

// Wait for the transfer to complete
CompletedDirectoryUpload completedDirectoryUpload = directoryUpload.completionFuture().join();

// Print out the failed uploads
completedDirectoryUpload.failedTransfers().forEach(System.out::println);
```

#### Download S3 objects within the same bucket to a local directory

To download all S3 objects within the same bucket, you need to provide the destination directory and the source bucket.

```java
S3TransferManager transferManager = S3TransferManager.create();
         DirectoryDownload directoryDownload =
             transferManager.downloadDirectory(
                  DownloadDirectoryRequest.builder()
                                          .destination(Paths.get("destination/directory"))
                                          .bucket("bucket")
                                          .listObjectsV2RequestTransformer(l -> l.prefix("prefix"))
                                          .build());
// Wait for the transfer to complete
CompletedDirectoryDownload completedDirectoryDownload = directoryDownload.completionFuture().join();

// Print out the failed downloads
completedDirectoryDownload.failedTransfers().forEach(System.out::println);
```

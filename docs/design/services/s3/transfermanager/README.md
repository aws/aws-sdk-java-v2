**Design:** New Feature, **Status:**
[In Development](../../../README.md)

# Project Tenets (unless you know better ones)

1. Meeting customers in their problem space allows them to deliver value
   quickly.
2. Meeting customer expectations drives usability.
3. Discoverability drives usage.

# Introduction

This project provides a much improved experience for S3 customers needing to
easily perform uploads and downloads of objects to and from S3 by providing the
S3 `S3TransferManager`, a high level library built on the S3 client.

# Project Goals

1. For the use cases it addresses, i.e. the transfer of objects to and from S3,
   S3TransferManager is the preferred solution. It is easier and more intuitive
   than using the S3 client. In the majority of situations, it is more
   performant.
1. S3TransferManager provides a truly asynchronous, non-blocking API that
   conforms to the norms present in the rest of the SDK.
1. S3TransferManager makes efficient use of system resources.
1. S3TransferManager supplements rather than replaces the lower level S3 client.

# Non Project Goals

1. Ability to use the blocking, synchronous client.

   Using a blocking client would severely impede the ability to deliver on goals
   #2 and #3.

# Customer-Requested Changes from 1.11.x

* S3TransferManager supports progress listeners that are easier to use.

  Ref: https://github.com/aws/aws-sdk-java-v2/issues/37#issuecomment-316218667

* S3TransferManager provides bandwidth limiting of uploads and downloads.

  Ref: https://github.com/aws/aws-sdk-java/issues/1103

* The size of resources used by Transfermanager and configured by the user
  should not affect its stability.

  For example, the configured size of a threadpool should be irellevant to its
  ability to successfuly perform an operation.

  Ref: https://github.com/aws/aws-sdk-java/issues/939

* S3TransferManager supports parallel downloads of any object.

  Any object stored in S3 should be downloadable in multiple parts
  simultaneously, not just those uploaded using the Multipart API.

* S3TransferManager has the ability to upload to and download from a pre-signed
  URL.

* S3TransferManager allows uploads and downloads from and to memory.

  Ref: https://github.com/aws/aws-sdk-java/issues/474

* Ability to easily use canned ACL policies with all transfers to S3.

  Ref: https://github.com/aws/aws-sdk-java/issues/1207

* Trailing checksums for parallel uploads and downloads.

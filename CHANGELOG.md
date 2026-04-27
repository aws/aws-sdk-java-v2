 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.43.0__ __2026-04-27__
## __AWS Glue__
  - ### Features
    - Addition of AdditionalAuditContext to GetPartition, GetPartitions, GetTableVersion, and GetTableVersions

## __AWS Key Management Service__
  - ### Features
    - KMS GetKeyLastUsage API provides information on the last successful cryptographic operation performed on KMS keys. This new API provides KMS customers with the last timestamp, CloudTrail eventId, and the cryptographic operation that was performed on the key.

## __AWS SDK for Java v2__
  - ### Features
    - Add support for overriding authSchemeProvider per request.
    - Optimize ExecutionAttributes to reduce resizes and reduce hash computation cost.

## __AWSBillingConductor__
  - ### Features
    - Add support for Passthrough pricing plan

## __Amazon CloudWatch Application Signals__
  - ### Features
    - Application Signals now supports creating composite Service Level Objectives on Service Operations. Users can now create service SLO on multiple operations.

## __Amazon CloudWatch Logs__
  - ### Features
    - Adds support for selecting all logs sources and types in a single association.

## __Amazon GameLift Streams__
  - ### Features
    - Adds Proton 10.0-4 to the list of runtime environment options available when creating an Amazon GameLift Streams application

## __Amazon Interactive Video Service__
  - ### Features
    - Adds tags parameter to the CreateAdConfiguration operation

## __Amazon Omics__
  - ### Features
    - Enable Public Internet or VPC configuration to BatchRun

## __Amazon OpenSearch Service__
  - ### Features
    - Amazon OpenSearch Service now supports JWKS URL configuration for JWT authentication

## __Amazon S3__
  - ### Features
    - Add configurable `expectContinueThresholdInBytes` to S3Configuration (default 1 MB). The Expect: 100-continue header is now only added to PutObject and UploadPart requests when the content-length meets or exceeds the threshold, reducing latency overhead for small uploads.

## __Amazon SageMaker Service__
  - ### Features
    - Updated API documentation for endpoint MetricsConfig. Added details on supported metric publish frequencies and clarified how EnableEnhancedMetrics controls utilization and invocation metric behavior.

## __Amazon WorkSpaces__
  - ### Features
    - Added support for Protocol as modified resource and added update failure as modification state

## __Application Migration Service__
  - ### Features
    - Added network modernization support, enabling customers to edit, resize, merge, and split VPCs and subnets during migration while retaining functional, non-conflicting IP addresses.

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fix TransferListener callbacks (bytesTransferred, transferComplete) not firing for unknown-content-length uploads via S3TransferManager when the data fits in a single chunk.


 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.48.0__ __2026-07-14__
## __AWS Cloud Map__
  - ### Features
    - Fixed Cloud Map endpoint resolution to correctly route to the dualstack endpoint when dualstack is enabled.

## __AWS Lambda__
  - ### Features
    - AWS Lambda now returns a new DependencyError value in StateReasonCode and LastUpdateStatusReasonCode to provide more actionable information when a function reaches a failed state due to an error from an upstream dependency or service.

## __AWS SecurityHub__
  - ### Features
    - AWS Security Hub now provides an AI inventory, giving central security teams a continuously updated, organization-wide view of AI assets and their security posture

## __Amazon Connect Service__
  - ### Features
    - This release adds SearchRules API which can be used to search for rules within an Amazon Connect instance.

## __Amazon EMR Containers__
  - ### Features
    - Introduced 5 new fields across 3 APIs as part of Spark Connect server launch for EMR on EKS. The fields added are sessionIdleTimeoutInMinutes, sessionEnabled, endpointToken, authProxyUrl and encryptionKeyArn.

## __Amazon S3__
  - ### Features
    - Add presigned URL download support to S3AsyncClient and S3 Transfer Manager. Customers can now download S3 objects using pre-signed URLs through the SDK's async client pipeline without needing AWS credentials configured.

  - ### Bugfixes
    - Fix bug where S3 PutObject would hang indefinitely when using AwsCrtAsyncHttpClient with chunkedEncodingEnabled(false)

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Update AWS Systems Manager Automation Targets to be correct max value.

## __AmazonMQ__
  - ### Features
    - This release adds storage size parameter for Amazon MQ for RabbitMQ cluster deployment broker on engine version RabbitMQ 4.2. You can now set a configurable storage size within a range of sizes dependent on broker instance size.

## __Elastic Disaster Recovery Service__
  - ### Features
    - Fast recovery of EC2 based drs workloads by skipping the conversion step


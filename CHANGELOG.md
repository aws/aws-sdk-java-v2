 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.48.1__ __2026-07-15__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed an issue where `ContainerCredentialsProvider` rejected the EKS Pod Identity IPv6 endpoint unless `AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE` was set to `IPv6`.
        - Contributed by: [@jtuglu1](https://github.com/jtuglu1)

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Fix HarnessEndpointArn pattern to match the actual service-emitted ARN format ('harness-endpoint' instead of 'endpoint'). Add additionalParams to Gemini model configuration for passing provider-specific parameters through to the model unchanged.

## __Amazon HealthLake__
  - ### Features
    - AWS HealthLake now offers data transformation in Preview to convert CSV and C-CDA data to FHIR R4. Customers can maintain reusable mapping profiles, run sync or async jobs with provenance tracking and drift detection, and use an AI agent to build and edit mapping logic from natural language.

## __Amazon Relational Database Service__
  - ### Features
    - Adds support for modifying EngineLifecycleSupport on DB instances and DB clusters through ModifyDBInstance and ModifyDBCluster.

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for the IpAddressType field on SourceIpConfig, enabling Network Load Balancer listener rules to match traffic based on whether the source IP is IPv4 or IPv6.

## __Payment Cryptography Data Plane__
  - ### Features
    - Adds support for UnionPay session key derivation to the GenerateAuthRequestCryptogram, VerifyAuthRequestCryptogram, GenerateMac, and VerifyMac APIs.

## __S3 Event Notification__
  - ### Features
    - Added `awsGeneratedTags` field to `S3Bucket` in the S3 Event Notifications module. Amazon S3 emits AWS-generated system tags on the `bucket` portion of event notifications when system tags are enabled on the source bucket. SDK consumers on the SNS/SQS/Lambda delivery paths can now access these tags via `S3Bucket#getAwsGeneratedTags()`.

## __Contributors__
Special thanks to the following contributors to this release: 

[@jtuglu1](https://github.com/jtuglu1)
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


 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.43.2__ __2026-04-30__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Improved error message when `ResponseTransformer.toFile()` or `AsyncResponseTransformer.toFile()` fails because the parent directory does not exist. The error now indicates that the parent directory must be created before calling the method.

## __AWS Single Sign-On Admin__
  - ### Features
    - Add InstanceArn and IdentityStoreArn in the response of CreateApplication API and IdentityStoreArn in the response of DescribeApplication API

## __Amazon Bedrock AgentCore__
  - ### Features
    - AgentCore Identity now supports on-behalf-of token exchange OAuth2. AgentCore Memory now supports metadata for LongTerm Memory Records.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - AgentCore Identity now supports on-behalf-of token exchange OAuth2. AgentCore Memory now supports metadata for LongTerm Memory Records.

## __Amazon DataZone__
  - ### Features
    - Adds support for asynchronous notebook runs

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Vended logs update param for capability vended logs feature

## __Amazon Route 53 Global Resolver__
  - ### Features
    - Adds support for regions in the UpdateGlobalResolver input.

## __Amazon SageMaker Service__
  - ### Features
    - Add InstancePools support to Endpoint for flexible provisioning across a prioritized list of instance types. Add Specifications support to InferenceComponent for per-instance-type model configurations.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Observability Admin enablement launch for AWS Kafka, Bedrock Agent Core Workload Identity and OTel metric enablement.

## __Managed Streaming for Kafka__
  - ### Features
    - Adds support for ZookeeperAccess field to control the Client-Zookeeper connectivity.

## __Payment Cryptography Control Plane__
  - ### Features
    - Adds support for resource-based policies on AWS Payment Cryptography keys, enabling cross-account key sharing. Also adds Multi-Party Approval (MPA) team association APIs for protecting sensitive import root public key operations.

## __Url Connection Client__
  - ### Bugfixes
    - Allow retry when URL Connection HTTP Client encounters a NullPointerException wrapped in a RuntimeException

# __2.43.1__ __2026-04-29__
## __AWS Account__
  - ### Features
    - Adds AccountState in the response for the GetAccountInformation API. Each state represents a specific phase in the account lifecycle. Use this information to manage account access, automate workflows, or trigger actions based on account state changes.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This feature adds configuration for specifying SCTE marker handling and allow greater control over generated manifest and segment URIs

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed deserialization failure for JSON responses containing field names longer than 50,000 characters. Services like DynamoDB allow attribute names up to 65,535 bytes, which exceeded Jackson's default `maxNameLength` limit.

## __AWS Transfer Family__
  - ### Features
    - This launch will increase the limits for customers to list the contents from the remote directories from 10k to 200k.

## __AWSDeadlineCloud__
  - ### Features
    - Adds support for rtx-pro-server-6000 GPU accelerator for service-managed fleets.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds batch evaluation for running evaluators against multiple agent sessions with server-side orchestration, AI-powered recommendations for optimizing system prompts and tool descriptions, and AB testing with controlled traffic splitting and statistical significance reporting

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds configuration bundles for versioned, immutable agent configuration snapshots with branch-based lineage

## __Amazon CloudFront__
  - ### Features
    - Amazon CloudFront now supports cache tag. Tag objects via response headers and invalidate all matching objects in a single request, replacing manual URL tracking and broad wildcards.

## __Amazon Elastic Container Registry__
  - ### Features
    - Removes support for registry policy V1

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers adds a new DescribeContainerGroupPortMappings API for container fleets, making it easy to discover which connection ports map to your container ports without needing to remotely access the compute.

## __Amazon S3__
  - ### Bugfixes
    - Add custom 503 throttling detection for S3 head operations

## __Amazon WorkSpaces Web__
  - ### Features
    - Allow admins to configure IPv6 ranges on IP Access Settings.

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fixed an issue where cancelling a directory transfer did not fully stop the operation.

## __S3TransferManager__
  - ### Features
    - Support MRAP buckets in S3 TransferManager.

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


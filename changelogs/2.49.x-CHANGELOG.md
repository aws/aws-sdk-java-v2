 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.49.6__ __2026-07-29__
## __AWS Database Migration Service__
  - ### Features
    - Updated documentation for various DMS Schema Conversion operations

## __AWS Glue__
  - ### Features
    - Adding filtering, partitioning, and VPC support to AWS Glue REST API connector

## __AWS IoT SiteWise__
  - ### Features
    - We have released a new set of APIs in support of a major new feature within AWS IoT SiteWise called Scenario Discover. Please see user guide about the feature and the API guide in public documentation for new APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue where duplicate signing-region endpoint keys in service metadata caused ServiceMetadata static initialization to fail with "Duplicate keys are provided". The generated SIGNING_REGIONS_BY_REGION map now tolerates duplicate keys.

## __AWS WAFV2__
  - ### Features
    - AWS WAF now supports pre-parse text transformations, letting you normalize raw query strings before parsing, available on rule statements that use SingleQueryArgument or AllQueryArguments as the FieldToMatch. AWS WAF also added 10 new text transformations, including ModSecurity v3 parity options.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for policy-based routing on AWS Transit Gateway, enabling you to route traffic based on 5-tuple matching (source IP, destination IP, source port, destination port, and protocol) using new policy table entry APIs that direct matching traffic to a target route table.

## __Amazon GameLift Streams__
  - ### Features
    - Adds ListApplicationShaderCaches API to retrieve shader cache metadata for applications and adds stream URLs, which give end users temporary, unauthenticated access to a stream session in their browser. Includes CreateStreamUrl, GetStreamUrl, ListStreamUrls, and RevokeStreamUrl operations.

# __2.49.5__ __2026-07-28__
## __AWS DataSync__
  - ### Features
    - Adds Enhanced mode support for EFS and FSx Lustre locations without an agent, and for HDFS (TDE), Azure Blob, and object storage locations with an agent. HDFS Enhanced mode supports multiple NameNodes for High Availability. Enhanced mode agents can now be deployed on Microsoft Hyper-V.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - AgentCore Identity now supports Private Key JWT client authentication for OAuth 2.0 credential providers. Agents can authenticate to identity provider token endpoints with a JWT client assertion signed by a customer-managed AWS KMS asymmetric key, eliminating the need for client secrets.

## __Amazon Connect Service__
  - ### Features
    - Documentation updates for SearchRules, AssociateRoutingProfileQueues, CreateRoutingProfile, AssociateContactWithUser CreateTaskTemplate, and UpdateTaskTemplate

## __IAM Roles Anywhere__
  - ### Features
    - Increases certificate string length for trust anchor source data to support new adjustable trust anchor limits.

## __TrustedAdvisor Public API__
  - ### Features
    - Adds ListRecommendationsForResource API and four CheckSummary fields (resourceArnQueryable, awsResourceTypes, checkGranularity, recommendationId) to retrieve recommendations for a given resource ARN.

# __2.49.4__ __2026-07-27__
## __AWS Account__
  - ### Features
    - This release adds support for the GetPrimaryEmailUpdateStatus API operation, which allows customers to retrieve the current status of a primary email address update request for an AWS account. The operation returns status information including whether the update is pending, completed, or failed.

## __AWS Billing and Cost Management Data Exports__
  - ### Features
    - With this release, customers can configure their data exports to deliver CSV reports in ZIP compressed format.

## __AWS Clean Rooms ML__
  - ### Features
    - This release adds support for the CR.8X worker type for SQL (32 vCPU)

## __AWS Clean Rooms Service__
  - ### Features
    - This release adds support for the CR.8X worker type for SQL (32 vCPU)

## __AWS Glue__
  - ### Features
    - Adds BatchGetDataQualityRulesetEvaluationRun API to retrieve multiple runs in one call, ObservationScope and ObservationMode parameters for anomaly detection, writing evaluation results to Data Catalog tables, and custom log group paths for recommendation runs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Security Agent__
  - ### Features
    - AWS Security Agent adds a new task hours field that reflects the active work done for a task.

## __Amazon EMR Containers__
  - ### Features
    - With this launch, you can now set concurrent job limits on a virtual cluster, giving you fine-grained control over how many job runs execute at once and how many can wait in queue.

## __Amazon QuickSight__
  - ### Features
    - Added new Governance fields to Custom Permissions API to support Deny By Default functionality.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds LoRA adapters, training plans, and new instance types to SageMaker inference optimization. CreateAIRecommendationJob accepts optional AdapterSource and CreateOptimizationJob accepts optional TrainingPlanArns and the ml.g7e and ml.p6-b200 families.

## __Partner Central Account API__
  - ### Features
    - Adds optional headquarters location to StartProfileUpdateTask, letting partners record their headquarters as an ISO 3166 country and subdivision code on their profile. When headquarters is provided, both the country and subdivision codes are required.

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fix race condition when invoking pausing upload that can cause a `NullPointerException`.

# __2.49.3__ __2026-07-24__
## __AWS Artifact__
  - ### Features
    - Added the PutComplianceInquiryFeedback API, enabling customers to submit feedback on compliance inquiry responses. Customers can rate responses as helpful or not helpful and provide optional reason codes and comments.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed several delegating AsyncRequestBody implementations (including those used by S3 Transfer Manager uploads) that reported the request-body type as unknown in the user-agent business metric instead of the actual type (file, bytes, or stream).
    - Update `ResponseInputStream`, to only return `0` from `available()` if the stream is closed; in other cases, if the wrapped stream returns 0 from `available()`, `1` is returned instead. This is to avoid cases where other classes such as [`java.util.zip.GZIPInputStream`](https://docs.oracle.com/javase/8/docs/api/java/util/zip/GZIPInputStream.html) misinterpret this as meaning the stream is closed.

## __Amazon CloudWatch Application Insights__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Amazon Cognito user pools now support the AdminGetUserAuthFactors operation, which lets administrators retrieve the configured authentication factors (such as password, SMS, email, and TOTP) available for a specific user in a user pool.

## __Amazon DynamoDB__
  - ### Features
    - Endpoint test standardizations

## __Amazon Neptune Graph__
  - ### Features
    - Update validations for Tag Keys and KMS Key ARNs.

## __RTBFabric__
  - ### Features
    - The deprecated inboundLinksCount field has been removed from the GetResponderGateway API response. Customers who previously relied on this field should use linksRequestedCount instead.

## __odb__
  - ### Features
    - Documentation-only update to clarify the operation-specific valid values for the externalIdType field.

# __2.49.2__ __2026-07-23__
## __AWS Backup Gateway__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.0. The SDK will prioritize its most performant protocol.

## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.0. The SDK will prioritize its most performant protocol.

## __AWS Billing and Cost Management Recommended Actions__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.0. The SDK will prioritize its most performant protocol.

## __AWS CRT HTTP Client__
  - ### Features
    - Add `numEventLoopThreads(Integer)` to `AwsCrtAsyncHttpClient.Builder` and `AwsCrtHttpClient.Builder` to configure the number of CRT event-loop (IO) threads. When set, the client owns a private `EventLoopGroup` of that size (must be greater than 1); when unset, it shares the process-wide default group.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for non-epoch-locked CMAF ingest in MediaPackageV2 channels.

## __Amazon AppStream__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds support for the Bring Your Own Storage(BYOS) feature in AgentCore Browser and Code Interpreter. Enables mounting S3Files and EFS File Systems via Access points.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds support for the Bring Your Own Storage(BYOS) feature in AgentCore Browser and Code Interpreter. Enables mounting S3Files and EFS File Systems via Access points.

## __Amazon DataZone__
  - ### Features
    - Adds support for notebook sync with S3 ipynb files

## __Amazon GameLift Streams__
  - ### Features
    - GameLift Streams now supports configuring a custom aspect ratio per stream session to accommodate different player devices. Supported aspect ratios include landscape, portrait, and square - delivering a full-screen experience without letterboxing or cropping.

## __Amazon Kendra Intelligent Ranking__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.0. The SDK will prioritize its most performant protocol.

## __Amazon QuickSight__
  - ### Features
    - Added new capabilities to custom permissions profiles to control access to Amazon Quick through the browser extension and Microsoft Word, Outlook, Excel, and PowerPoint add-ins.

## __Amazon SageMaker Service__
  - ### Features
    - Release support for c6a, m6a, m6g, m7g, m8g instance types for SageMaker HyperPod

## __Amazon Workspaces Instances__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.0. The SDK will prioritize its most performant protocol.

## __Redshift Data API Service__
  - ### Features
    - This release include long polling provids a new parameter wait-time-seconds to 5 API operations, new API ListSessions, and a new parameter execution-mode to BatchExecuteStatement

# __2.49.1__ __2026-07-22__
## __ARC - Region switch__
  - ### Features
    - Adds support for a client token in StartPlanExecution to make plan execution requests idempotent for safe retries.

## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Do not set the Content-Length header on a request that already carries Transfer-Encoding. Emitting both violates RFC 7230 and was rejected by the underlying CRT layer; this aligns the CRT client with the Netty client's existing behavior for chunked requests.

## __AWS Parallel Computing Service__
  - ### Features
    - AWS PCS Node Lifecycle Actions provides a structured way to run custom scripts at defined points in a compute node's lifecycle directly through the AWS PCS compute node group API.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch__
  - ### Features
    - Adds documented value constraints for CloudWatch Log Alarm scheduled query configuration fields, and makes LogGroupIdentifiers optional for log alarms.

## __Amazon GuardDuty__
  - ### Features
    - Amazon GuardDuty now returns filter lifecycle metadata in GetFilter responses. The response includes createdAt and updatedAt timestamps and a version number that increments on each update, giving you visibility into when a filter was created and last modified.

## __Amazon Prometheus Service__
  - ### Features
    - Add CloudWatch dataset destinations for Amazon Managed Service for Prometheus collectors.

## __Amazon S3__
  - ### Bugfixes
    - Fix a race condition in multipart upload with low maxInFlightParts where CompleteMultipartUpload could be initiated while the final part was still uploading.

## __Amazon Simple Email Service__
  - ### Features
    - Launching DEED and MREP in US GOV

## __CloudWatch Observability Admin Service__
  - ### Features
    - Enablement for ALB and Bedrock Knowledge Base logs via Observability Admin Telemetry Rule for account and organization level

## __Elastic Load Balancing__
  - ### Features
    - This adds CLI examples for the IpAddressType field on SourceIpConfig, enabling Network Load Balancer listener rules to match traffic based on whether the source IP is IPv4 or IPv6.

## __Partner Central Account API__
  - ### Features
    - Adds Qualifications Association APIs that enable partners to associate a subsidiary account's qualifications with a primary account. Once associated, qualifications are shared across all connected accounts and scorecards are consolidated. Partners can start and track association and disassociation.

# __2.49.0__ __2026-07-21__
## __AWS EntityResolution__
  - ### Features
    - Add support for real time matching with AWS Entity Resolution matching workflows with advanced rule sets.

## __AWS Invoicing__
  - ### Features
    - Added the SendProcurementPortalValidation and VerifyProcurementPortalValidation APIs. You can use the AWS SDKs to self-service activate your Procurement Portal Preferences created on the Billing Preferences page with a one-time-passcode (OTP) delivered to your portal.

## __AWS SDK for Java v2__
  - ### Features
    - Update Netty to 4.1.136

  - ### Bugfixes
    - Eliminate per-operation lambdas for endpoint and auth scheme resolution in generated clients, permanently fixing constant pool overflow for large services like EC2 Internal.

## __Amazon EMR Containers__
  - ### Features
    - Added support for the DeleteSecurityConfiguration API, which allows customers to delete security configurations in Amazon EMR on EKS. Also added authenticationConfiguration in securityConfigurationdata structure.

## __Amazon Redshift__
  - ### Features
    - Amazon Redshift - Added support for managing Query Editor V2 IAM Identity Center applications via new CreateQev2IdcApplication, DescribeQev2IdcApplications, ModifyQev2IdcApplication, and DeleteQev2IdcApplication API operations.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Added a WarningMessage field to Automation along with corresponding public documentation.

## __Inspector2__
  - ### Features
    - GA date - July 21st 2026, remove Tags field from ListCodeSecurityIntegration and ListCodeSecurityScanConfiguration.

## __Redshift Data API Service__
  - ### Features
    - update the workgroupArn to include EUSC partition, tests in THF Gamma and Prod no issue

## __Timestream InfluxDB__
  - ### Features
    - This release adds support for custom plugins in Amazon Timestream for InfluxDB. InfluxDB 3 Core and Enterprise DB parameter groups now accept a plugin repository URL and optional AWS Secrets Manager secret ARN, so the Processing Engine loads your Python plugins from a public or private repository.


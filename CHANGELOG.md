 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.50.2__ __2026-07-31__
## __AWS Billing__
  - ### Features
    - Adds GetEnterpriseSupportChargeSummary, GetEnterpriseSupportContractDetails, and ListEnterpriseSupportLinkedAccountCharges. These APIs provide first-time programmatic access to billing data for Enterprise Support usage previously only available upon request through AWS Concierge or Support.

## __AWS CloudFormation__
  - ### Features
    - Adding enum for sensitive property to DriftIgnoredReason

## __AWS Elemental Inference__
  - ### Features
    - AWS Elemental Inference now supports graphic composition on cropped video outputs, enabling branded graphics and other visual elements to be overlaid as part of the inference workflow.

## __AWS Marketplace Catalog Service__
  - ### Features
    - This release enhances the ListEntities API to support TargetAgreementId, TargetAgreementIntent, and CreatedBySource filters for the Offer entity type.

## __AWS Network Firewall__
  - ### Features
    - Doc Updates for Container Attributes

## __AWS Outposts__
  - ### Features
    - Adds the "EKS" value to the AWSServiceName enum and marks the Address field as sensitive.

## __AWS Resilience Hub V2__
  - ### Features
    - Adding support for new testing capability in AWS Resilience Hub.

## __Amazon Bedrock Runtime__
  - ### Features
    - Added support for mid-conversation tool changes in the Amazon Bedrock Converse and ConverseStream APIs

## __Amazon CloudWatch Logs__
  - ### Features
    - Amazon CloudWatch Logs now lets you create and update lookup tables directly from CloudWatch Logs query results by passing a queryId, and configure a lookup table as a scheduled query destination so it refreshes automatically with the latest query results on each run.

## __Amazon DataZone__
  - ### Features
    - Adding support for enhanced Git experience in Sagemaker Unified Studio.

## __Amazon Prometheus Service__
  - ### Features
    - Amazon Managed Service for Prometheus adds support for an Amazon OpenSearch Service exporter for managed collectors.

## __Amazon QuickSight__
  - ### Features
    - Adding TopicV2 management APIs, adding possibility to use Topics in Analysis

## __Amazon Relational Database Service__
  - ### Features
    - Adds StorageOperationStatus and StorageOperationPercentProgress to DescribeDBInstances, letting you monitor RDS storage initialization and optimization progress.

## __Amazon Transcribe Streaming Service__
  - ### Features
    - This release adds a new optional TranscriptFormat parameter to the Amazon Transcribe streaming API, letting customers select spoken or written form for numeric and formatted output.

## __AmazonConnectCampaignServiceV2__
  - ### Features
    - Launching feature for abandonment rate pacing control for outbound campaigns.

# __2.50.1__ __2026-07-30__
## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Removing Smithy RPC v2 CBOR support that was added in previous SDK release.

## __AWS Billing and Cost Management Recommended Actions__
  - ### Features
    - Removing Smithy RPC v2 CBOR support that was added in previous SDK release.

## __AWS SDK for Java v2__
  - ### Features
    - Add the `@SdkAdvancedApi` annotation, which marks APIs that are error-prone to implement, override, call, or configure so that using them incorrectly compiles cleanly but can fail or misbehave at runtime. The annotation records structured guidance (the risky usage kind, an explanation of the contract to uphold, a safer alternative, and a documentation link) and is applied to several streaming and interceptor extension points, including AsyncRequestBody, AsyncResponseTransformer, ContentStreamProvider, the mutating ExecutionInterceptor content hooks, and the FUTURE_COMPLETION_EXECUTOR advanced client option.
    - Updated endpoint and partition metadata.

# __2.50.0__ __2026-07-30__
## __AWS Identity and Access Management__
  - ### Features
    - Improved IAM Policy Simulator accuracy. Simulator now evaluates SCP conditions and resource scoping, returns explicitDeny for explicit SCP denials, and reports accurate cross-account decisions.

## __AWS Lambda__
  - ### Features
    - Add Python3.15 (python3.15) and NodeJs 26 (nodejs26.x) support to AWS Lambda

## __AWS Network Firewall__
  - ### Features
    - Adds UPDATING field to Container Association Status

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Improve endpoint resolution performance by replacing URI.create with a lightweight EndpointUrl.

## __AWS Security Agent__
  - ### Features
    - Adds support for providing a branch override when configured integrated repositories

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds support for configuring models through the OpenResponses API for custom evaluators. CreateEvaluator and UpdateEvaluator now accept an OpenResponses model configuration for LLM-as-a-Judge evaluations.

## __Amazon S3__
  - ### Bugfixes
    - Honor an explicit `S3Configuration.expectContinueEnabled(false)` when cross region access is enabled.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for g7 family instance types for SageMaker Studio JupyterLab and CodeEditor apps for IAD (us-east-1), PDX (us-west-2), CMH (us-east-2).

## __Managed Streaming for Kafka__
  - ### Features
    - Amazon MSK Express brokers now support streaming tables for Apache Iceberg, continuously materializing Apache Kafka topics as Iceberg tables in Amazon S3 Tables. Express brokers also now support data delivery to Amazon S3 general purpose buckets.

## __PricingPlanManager__
  - ### Features
    - Adds support for Public PricingPlanManager SDK


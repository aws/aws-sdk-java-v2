 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.48.4__ __2026-07-20__
## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Preserve the underlying CRT HttpException (including the CRT error code) as the cause of the SSLHandshakeException and ConnectException surfaced for TLS negotiation failures and socket timeouts, so callers can differentiate transient failures from persistent ones by inspecting the exception cause chain.

## __AWS MediaTailor__
  - ### Features
    - This change adds api support for configuring ad decision server timeouts and concurrency fields on MediaTailor playback configurations

## __AWS Organizations__
  - ### Features
    - Updated InvalidInputException error documentation to clarify that the service validates free-text field values against common cross-site scripting (XSS) patterns.

## __AWSMarketplace Metering__
  - ### Features
    - For new SaaS product integrations, CustomerIdentifier is not populated in ResolveCustomer responses and is not supported in BatchMeterUsage. Use CustomerAWSAccountId and LicenseArn instead.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Add W3C trace context headers (traceparent, tracestate, baggage) and X-Amzn-Trace-Id to InvokeHarness request for end-to-end observability propagation. Add toolResultMetadata to the streaming content block delta for MCP tool result meta delivery without oversized SSE frames.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - This release adds support for specifying a connector version on Gateway targets to pin the connector's tool schema. It also introduces web-search connector version 1.2.0, which adds agent-side domain filtering, published date range filtering, and admin-side domain allowlisting.

## __Amazon QuickSight__
  - ### Features
    - Adds support for custom permissions for Triggers, allowing administrators to control user access to Schedule, Inbound Email and Quick Event triggers.

## __Amazon Simple Email Service__
  - ### Features
    - Amazon SES introduces three new Pricing Plans (Essentials, Pro, Enterprise), which bundle SES features under one pricing umbrella.  The new PutAccountPricingAttributes API lets the user set the account's plan, while current plan retrievalif done through the new PricingAttributes field on GetAccount.

## __Inspector2__
  - ### Features
    - Adds Windows path support for deep inspection. Fixes tag propagation for connector CloudFormation stack operations.

# __2.48.3__ __2026-07-17__
## __AWS CRT HTTP Client__
  - ### Features
    - Add a minTlsVersion(TlsVersion) builder option on AwsCrtHttpClient and AwsCrtAsyncHttpClient that enforces a minimum TLS protocol version for outbound connections. Currently supports TlsVersion.TLS_1_3 and TlsVersion.SYSTEM_DEFAULT (the default). Fixes [#5619](https://github.com/aws/aws-sdk-java-v2/issues/5619).

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Amazon Cognito user pools now support sending SMS via AWS End User Messaging. A new EumsSms object in SmsConfigurationType lets you deliver MFA and verification texts through AWS End User Messaging, alongside the existing Amazon SNS option.

## __Amazon GameLift Streams__
  - ### Features
    - Amazon GameLift Streams now supports assigning an IAM role to a stream session, enabling your application to securely access resources in your AWS account, such as Amazon S3 buckets and DynamoDB tables.

## __Amazon Kinesis Analytics__
  - ### Features
    - Support for Flink 2.3 in Managed Service for Apache Flink

## __Amazon Relational Database Service__
  - ### Features
    - Adds the AssociatedRoles parameter to CreateDBCluster, RestoreDBClusterFromSnapshot, RestoreDBClusterToPointInTime, and RestoreDBClusterFromS3, letting customers associate IAM roles with an Aurora DB cluster at create or restore time instead of calling AddRoleToDBCluster afterward.

## __odb__
  - ### Features
    - Adds support for sourcing Autonomous Database admin and wallet passwords from customer-managed AWS Secrets Manager secrets, including password source configuration and summaries, and enabling or disabling the OCI IAM service role for Secrets Manager integration via InitializeService.

# __2.48.2__ __2026-07-16__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Sustainability__
  - ### Features
    - Adds support for retrieving estimated water allocation data.

## __Amazon Chime SDK Voice__
  - ### Features
    - Marked CreateProxySession, DeleteProxySession, GetProxySession, ListProxySessions, UpdateProxySession, PutVoiceConnectorProxy, DeleteVoiceConnectorProxy, and GetVoiceConnectorProxy as deprecated.

## __Amazon EMR__
  - ### Features
    - Amazon EMR updates the Session object returned by GetSession API

## __Amazon Omics__
  - ### Features
    - Adds support for returning the task UUID (universally unique identifier) in GetRunTask and ListRunTasks responses

## __Amazon Redshift__
  - ### Features
    - Amazon Redshift - Added support for rg.large and rg.12xlarge node types in CreateCluster, ModifyCluster, and ResizeCluster API operations.

## __Amazon SageMaker Service__
  - ### Features
    - Release support for g7 instance type for SageMaker inference endpoints.

## __Amazon Simple Storage Service__
  - ### Features
    - Documentation update for removing the 30 day minimum restriction for transition to Standard-IA or OneZone-IA storage classes

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


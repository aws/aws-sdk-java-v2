 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.55.5__ __2026-09-24__
## __AWS IoT__
  - ### Features
    - Fixed ListV2LoggingLevels and DeleteV2LoggingLevel documentation to include all supported target-types

## __AWS Marketplace Discovery__
  - ### Features
    - AWS Marketplace Discovery API now supports localized responses and SigV4a request signing. It returns new fulfillment details, including AMI architecture, EBS volume and security group information, SaaS quick-launch status, and SageMaker input and output MIME types.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Security Agent__
  - ### Features
    - Added support for Confluence export, enabling customers to publish security findings to Confluence pages.

## __Amazon CloudWatch__
  - ### Features
    - This release adds Create, Get, Update, and DeleteResourceMetricsConfiguration to enable detailed metric collection for an AWS resource, and adds UpdateOTelEnrichment plus include and exclude filters on StartOTelEnrichment so you can choose which metric namespaces CloudWatch enriches.

## __Amazon DataZone__
  - ### Features
    - Amazon DataZone now supports the TOOLING blueprint category on CreateEnvironmentBlueprint, UpdateEnvironmentBlueprint, GetEnvironmentBlueprint, and ListEnvironmentBlueprints, for custom tooling blueprints. CreateConnection now accepts roleArn in iamProperties.

## __Amazon ElastiCache__
  - ### Features
    - Added tagging support for ElastiCache Global DataStore.

## __Amazon EventBridge__
  - ### Features
    - Adds a ManagedBy field to the DescribeEventBus and ListEventBuses responses, identifying the AWS service that created an event bus on your behalf.

## __Amazon EventBridgeV2__
  - ### Features
    - Introducing Amazon EventBridge enhanced Custom event bus, a new shareable event bus for organizational-scale event-driven applications feature ordered delivery, deduplication, open event formats, and cross-account bus sharing.

## __Amazon Route 53 Resolver__
  - ### Features
    - Documentation updates for Route 53 Resolver. Clarifies which Outpost Resolver operations apply to first-generation AWS Outposts and that Resolver is managed automatically on second-generation Outposts. Adds Local Network Interface subnet compatibility notes for Resolver endpoints.

## __Redshift Data API Service__
  - ### Features
    - Updates to the ListDatabases and WorkgroupName validation

# __2.55.4__ __2026-09-23__
## __AWS Billing__
  - ### Features
    - Added the ListBillingViewSegments API, which returns billing view segment information for a specified billing view ARN and time range. This API enables customers and integrated tools to programmatically determine the billing context of their accounts.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds support for SMPTE 337M audio passthrough, compositing up to five motion graphic overlays in a single output, and controlling how passthrough video is segmented in ABR outputs. It also adds 3GP, 3G2, AAC, AC-3, and E-AC-3 as supported input containers for the Probe operation.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for signalling start and end in the ContentKeyPeriod element in key request from MediaPackageV2

## __AWS Network Security Manager Customer API__
  - ### Features
    - AWS Network Security Manager is a new service that helps you centrally configure, deploy, and continuously enforce security policies on network security services across the accounts and resources in your AWS Organization.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Performance Improvements
    - Fix unnecessary allocation and request latency during endpoint resolution for operations that derive endpoint parameters from request data, including DynamoDB batch and transaction operations and Amazon S3 DeleteObjects. These bindings now use generated typed getters and iterate request maps directly instead of using reflection and copying maps. The order of derived key lists may change, but current DynamoDB endpoint routing behavior is unchanged.

## __Amazon Kinesis__
  - ### Features
    - Amazon Kinesis Data Streams now supports service managed record distribution for on demand streams. Set the record distribution strategy to AUTO to evenly distribute records across shards. Configure it at stream creation with CreateStream or update anytime with UpdateStreamRecordDistributionStrategy

## __Amazon Lex Model Building V2__
  - ### Features
    - Adds support for speaker diarization on Amazon Lex V2 bot locales. Speaker diarization keeps your bot on the primary (loudest) speaker during a streaming voice conversation, so background voices do not start a turn or interrupt a prompt.

## __Connect Health__
  - ### Features
    - Multi language support with code switching, custom template sectionHeader now allows underscores.

## __EC2 Image Builder__
  - ### Features
    - Documentation update for EC2 Image Builder - adds API request and response examples for all operations, improves descriptions throughout, and corrects response field patterns for image versions and workflow ARNs.

## __Payment Cryptography Data Plane__
  - ### Features
    - Adds asymmetric key support to ReEncryptData for re-encrypting data between RSA and symmetric data encryption keys.

# __2.55.3__ __2026-09-22__
## __AWS Glue__
  - ### Features
    - Adding two new fields for Glue Materialized Views feature - (1) SubObjectsStatistics and (2) SparkPipelineInfo.

## __AWS SDK for Java v2__
  - ### Performance Improvements
    - Migrate all services to BDD based endpoints. Endpoint resolution behavior is unchanged but performance is improved.

## __AWS Single Sign-On Admin__
  - ### Features
    - AWS IAM Identity Center now returns PrimaryRegion and Regions in the DescribeInstance response, providing information about replicated instances, and returns IdentityStoreArn in both the ListInstances and DescribeInstance responses.

## __Amazon API Gateway__
  - ### Features
    - API Gateway now supports two new security policies for REST APIs and custom domain names, SecurityPolicy-TLS13-1-2-Ext2-PQ-2025-09 (TLS 1.3 1.2 with post-quantum cryptography) and SecurityPolicy-TLS13-1-2-Ext2-FIPS-PQ-2025-09 (adds FIPS). Both retain legacy algorithms for backward compatibility.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 now supports quote-based start date changes for future-dated Capacity Reservations

## __Amazon QuickSight__
  - ### Features
    - Adds support for granular custom permissions on 28 action connectors, including Gmail, Google Drive, Google Sheets, Airtable, and Dropbox. Administrators can now allow or deny individual connector operations instead of all action connectors at once.

## __Amazon S3__
  - ### Bugfixes
    - `PresignedUrlDownloadRequest#toString()` and the presigned URL marshaller's exception messages render the presigned URL without its query string. `PresignedUrlDownloadRequest#presignedUrl()` still returns the full URL.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Amazon CloudWatch Omni is now generally available, an AI-powered unified observability for AI agents, applications, and infrastructure. Centralization now supports context graph for multi-account resource discovery, and dataset integrations makes logs available in CloudWatch datasets.

## __CloudWatch Omni__
  - ### Features
    - Amazon CloudWatch Omni is now generally available, an AI-powered unified observability for AI agents, applications, and infrastructure. As part of it, organization centralization rules now support cross-account context graph centralization.

# __2.55.2__ __2026-09-21__
## __AWSBillingConductor__
  - ### Features
    - Launching Auto Billing Transfer Billing Group Creation Preference feature

## __Amazon Bedrock AgentCore__
  - ### Features
    - Amazon Bedrock AgentCore Harness now supports lifecycle hooks for invocations and tool calls, with Lambda, SNS, and EventBridge targets. This release also adds apiBase for custom OpenAI-compatible endpoints.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Amazon Bedrock AgentCore Harness now supports lifecycle hooks for invocations and tool calls, with Lambda, SNS, and EventBridge targets. This release also adds apiBase for custom OpenAI-compatible endpoints

## __Amazon DocumentDB with MongoDB compatibility__
  - ### Features
    - Add support for CopyTagsToSnapshot field in CreateDbCluster, ModifyDbCluster, RestoreDbClusterFromSnapshot and RestoreDbClusterToPointInTime for DocumentDB.

## __Amazon SageMaker Service__
  - ### Features
    - Add support for r6i, m8i, c8i, r8i instance types in Training and Processing

# __2.55.1__ __2026-09-18__
## __AWS Glue__
  - ### Features
    - Introducing AWS Glue Data Quality advanced rule recommendations for faster recommendations. This capability uses Amazon Athena to analyze a sample of table data and Amazon Bedrock to recommend DQDL rules.

## __AWS SDK for Java v2 Codegen__
  - ### Bugfixes
    - Resolve endpoint parameters up front and ensure all parts of codegen reference them consistently.

## __Amazon AppIntegrations Service__
  - ### Features
    - This release adds support for A2A servers via the ApplicationType and AuthConfig fields, allowing customers to register their agent-to-agent servers with API key authentication.

## __Amazon Connect Service__
  - ### Features
    - This release adds the ListSecurityProfileAIAgents API and updates the CreateSecurityProfile and UpdateSecurityProfile APIs to support the AllowedAIAgents field on security profiles, allowing customers to manage the 3P AI agents associated with a security profile for Agent-to-Agent interactions.

## __Amazon DataZone__
  - ### Features
    - Adds support for specifying Notebook type

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds documentation for the T8i instance family to the EC2 ModifyDefaultCreditSpecification and GetDefaultCreditSpecification APIs.

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - GetParticipant, ListParticipantEvents, ListParticipantReplicas, StartParticipantReplication, and StopParticipantReplication now accept participant IDs containing underscores.

## __Amazon Q Connect__
  - ### Features
    - Amazon Connect AI Agents now support multi-agent orchestration and structured JSON input and output messaging for orchestration agents.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for the hub content resource in SageMaker Search.

## __Amazon Transcribe Service__
  - ### Features
    - Amazon Transcribe now lets you encrypt your custom vocabularies, custom vocabulary filters, and custom language models with a customer managed AWS KMS key instead of an AWS owned key, and adds a new UpdateLanguageModel operation to transition CLM encryption to a different KMS key.

# __2.55.0__ __2026-09-17__
## __AWS CRT HTTP Client__
  - ### Features
    - Added an opt-in read/write (socket) inactivity timeout for the CRT-based HTTP clients (`AwsCrtHttpClient` and `AwsCrtAsyncHttpClient`). It is not enabled unless you set the `AWS_ENABLE_DEFAULT_SOCKET_TIMEOUT_2026` environment variable or the `aws.enableDefaultSocketTimeout2026` system property. Once enabled, an SDK-managed CRT HTTP client that has no explicit `connectionHealthConfiguration` set receives a per-service read/write timeout; a connection that makes no read or write progress within that window is closed and the in-flight request fails with a retryable `IOException`. The timeout is not applied to an HTTP client you build and supply yourself via `httpClient(...)`.

## __AWS End User Messaging Social__
  - ### Features
    - Add support for WhatsApp Calling APIs.

## __AWS IoT Wireless__
  - ### Features
    - Adds Multi-frame GNSS support to the AWS IoT Core Device Location GetPositionEstimate API. The new GnssMultiFrame measurement type improves location accuracy by combining multiple GNSS signal captures (2, 4, 8, 16, or 32) from the same device to estimate its position.

## __AWS User Notifications__
  - ### Features
    - Added support for attachments on managed notification events. Added support to access and subscribe sensitive managed notification events.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Batch evaluation now supports evaluating specific traces within a session. Each session can specify up to 100 trace IDs to evaluate.

## __Amazon Connect Service__
  - ### Features
    - Made the replicaAlias attribute optional in the ReplicateInstance API to support Global routing for Amazon Connect Global Resiliency (ACGR) instances. This change maintains backward compatibility. When onboarding to ACGR without Global routing, you must specify a custom replicaAlias in your API call

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adding support for "Tunnel" VPC Endpoint

## __Amazon GuardDuty__
  - ### Features
    - This change surfaces AI Protection resources on existing public IAM attack sequences. Customers will now see which model was accessed and whether a guardrail intervened as part of the credential-compromise sequence.

## __Amazon Simple Email Service__
  - ### Features
    - Added support to query the tenant name for BatchGetMetricData and CreateExportJob APIs to filter metrics and messages at the tenant level.

## __Amazon Simple Notification Service__
  - ### Features
    - SNS API reference documentation update

## __Amazon VPC Lattice__
  - ### Features
    - Adding support for CIDR Resource Configuration


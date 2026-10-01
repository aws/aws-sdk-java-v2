 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.55.10__ __2026-10-01__
## __AWS End User Messaging__
  - ### Features
    - AWS End User Messaging now supports Brand profiles and Notify code configurations. Brand profiles capture your sender details once to reuse across phone number registrations. Notify code configurations let you define your OTP policy and delivery settings to send passcodes in minutes.

## __AWS Health APIs and Notifications__
  - ### Features
    - Adds DescribeServiceLifecycle operation returning lifecycle information for AWS services, including end-of-support dates, version recommendations, and lifecycle events.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Adds GetRemediationsV2 and ListExposuresByRemediationV2 APIs. This feature allows customers to see their highest priority remediations for their Exposure findings. Remediations target key changes customers can make to resources to drive finding resolution.

## __AWS Transfer Family__
  - ### Features
    - AWS Transfer Family Workflows adds support for the structuredLogDestinations option, enabling customers to specify a custom Amazon CloudWatch Logs log group for managed workflow execution logs.

## __Agents for Amazon Bedrock__
  - ### Features
    - Adds an optional textReadyAt field to ListIngestionJobs and GetIngestionJob for Managed Knowledge Bases data source sync jobs. The field denotes the timestamp at which all the documents in the scope of a sync job had their text content indexed and are available for retrieval.

## __Amazon CloudFront__
  - ### Features
    - Added always-amz-auth as a supported signing behavior for Origin Access Control (OAC), enabling CloudFront to authenticate requests to Lambda-Web origins.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release launches the AMI tag sharing feature, which lets AMI owners share tags alongside their AMIs, eliminating the need to build and maintain custom tag replication workflows.

## __Amazon QuickSight__
  - ### Features
    - This release adds HierarchyFilter support for Amazon QuickSight analysis and dashboard and 2 legged OAuth for databricks datasources.

## __Amazon SageMaker Service__
  - ### Features
    - Release support for c8a.16xlarge and m8a.16xlarge instance types for SageMaker HyperPod

## __Lambda Web__
  - ### Features
    - Lambda Web Functions GA launch. Lambda Web Functions enable customers to run web applications and API backends

# __2.55.9__ __2026-09-30__
## __AWS Account__
  - ### Features
    - This release adds support for verifying an AWS account's primary contact phone number. SendPhoneNumberVerification sends a one-time code by SMS, VerifyPhoneNumber validates it, and GetContactInformation now returns the verification status.

## __AWS Batch__
  - ### Features
    - AWS Batch adds support for Amazon EKS access entries on EKS compute environments through the new accessEntry setting in CreateComputeEnvironment and UpdateComputeEnvironment.

## __AWS Global Accelerator__
  - ### Features
    - IpSets now include the Network Zone for each Static IP address.

## __AWS Glue__
  - ### Features
    - Enable Catalog ID for crawler, column statistics and materialized views.

## __AWS Organizations__
  - ### Features
    - Add support for policy operations on the GUARDDUTY POLICY policy type.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agent Registry__
  - ### Features
    - Minor doc update for the AWS Agent Registry Custom metadata SearchDiscoverableRegistryRecords API

## __Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Automated Reasoning policies now accept Unicode letters in identifier names such as type names, type value names, and variable names. You can now author policies in non-English languages using accented or non-Latin characters.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - This release adds support for private certificate authorities on Amazon Bedrock AgentCore Gateway targets. The new certificateConfigurations parameter on CreateGatewayTarget and UpdateGatewayTarget references a PEM-encoded CA certificate in Amazon S3 or AWS Secrets Manager.

## __Amazon CloudWatch Logs__
  - ### Features
    - Amazon CloudWatch Logs now supports an optional roleArn parameter on PutDeliveryDestination for X-Ray trace delivery destinations, specifying the IAM role to assume when delivering traces.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect Rules can now trigger in-app notifications to users as a rule action. Notification character limit was increased to 500 visible characters.

## __Amazon DataZone__
  - ### Features
    - Support for setting notebook run notification configurations

## __Amazon DynamoDB__
  - ### Features
    - Adds support for filtering exported table data using FilterExpression, ProjectionExpression and KeyConditionExpression with ExportTableToPointInTime.

## __Amazon EC2 Container Service__
  - ### Features
    - Releasing VPCL for BlueGreen ecs deployments.

## __Amazon GuardDuty__
  - ### Features
    - GuardDuty AWS Organizations policy integration. GetDetector and GetMemberDetectors now show whether a GuardDuty policy manages a feature.

## __Amazon S3 Vectors__
  - ### Features
    - Amazon S3 Vectors now supports metadata prefiltering, providing higher recall on filtered queries.

## __Amazon SageMaker Service__
  - ### Features
    - This feature enables customers to modify their accounting database via API.

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon S3 adds a new optional S3 Inventory field, IntelligentTieringReferenceDate, reporting the reference date S3 Intelligent-Tiering uses to evaluate an object's tier-transition eligibility. The value is populated for objects in the Intelligent-Tiering storage class and left blank for others.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Enablement for Bedrock PaymentManager logs via Observability Admin Telemetry Rule

# __2.55.8__ __2026-09-29__
## __AWS Elemental Inference__
  - ### Features
    - Adds an extendedAnalysis setting to contextual metadata outputs to control detection of people, environments, brands, and on-screen text, and updates the summaryGeneration documentation.

## __AWS Glue__
  - ### Features
    - Add support for Glue system-managed materialized views.

## __AWS MediaTailor__
  - ### Features
    - AWS Elemental MediaTailor now supports beaconing configuration on playback configurations. In Insights reporting mode, MediaTailor will now gather client side beaconing metrics. Set the reporting mode to Disabled to turn this off.

## __AWS SSO Identity Store__
  - ### Features
    - Add support for network access controls to restrict Identity Store API and SCIM access to trusted networks, optimistic locking for users and groups via resource revisions, and resource ARNs as identifiers in requests.

## __AWS Security Agent__
  - ### Features
    - Adds support for Azure DevOps and Bitbucket Data Center integration providers.

## __AWS Transfer Family__
  - ### Features
    - AWS Transfer Family now supports configuring up to three custom ports on public SFTP servers, instead of the single default port 22. You can also set each port's communication mode (server-talk-first or client-talk-first) so legacy and modern SFTP clients connect reliably.

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud now supports Docker software add-ons on service-managed fleets. Adds support for Open Job Description EXPR and Feature Bundle 1 job templates with typed job parameters and job, step, and parameter names up to 512 characters.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Amazon Bedrock Agentic Retrieve now supports the Bedrock Mantle (OpenAI Responses) endpoint via a new MantleFoundationModel configuration with an optional projectId.

## __Amazon AppStream__
  - ### Features
    - Add support for NVIDIA GRID driver version metadata in Workspace Applications image responses through the new ImageSoftwareMetadata field.

## __Amazon ElastiCache__
  - ### Features
    - Amazon ElastiCache Serverless now supports public endpoints for Valkey caches. With the new Connection Type parameter, you can create a serverless cache accessible over the internet without any VPC configuration. Public endpoint caches require IAM authentication.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds the LaunchStatus field to CapacityReservation in the DescribeCapacityReservations response. This field indicates whether you can currently launch instances into an UltraServer.

## __Amazon OpenSearch Service__
  - ### Features
    - Amazon OpenSearch Service now supports advisory pre-validations for domain config changes. Non-critical checks now surface as warnings you can acknowledge (via the new AcceptedWarnings parameter) and proceed, instead of hard-blocking. Severity is reported in change-progress and dry-run results.

## __Amazon Relational Database Service__
  - ### Features
    - Adds the TargetResourceConfigurations parameter to CreateBlueGreenDeployment, letting you specify a target KMS key for each resource in the green environment.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for cpu flex type instances on SageMaker Training and Processing. Also contains minor updates to DescribeTrainingPlan to support ARN inputs.

## __Amazon Simple Email Service__
  - ### Features
    - Added Filter support for ListTenants, ListEmailIdentities, and ListConfigurationSets APIs.

## __Inspector2__
  - ### Features
    - The ListFindingAggregations API now includes Low, Informational, and Untriaged counts alongside the existing severity counts in SeverityCounts.

# __2.55.7__ __2026-09-28__
## __AWS Billing__
  - ### Features
    - Adds support for (a) listing Business Support account charges via ListBusinessSupportAccountCharges and (b) subscription history via ListBusinessSupportSubscriptionHistory through the AWS Billing API.

## __AWS Glue__
  - ### Features
    - Added a new exception to several batch APIs

## __AWS Security Agent__
  - ### Features
    - Run automated penetration tests directly from your CI-CD pipeline to scan code changes before they ship, gating deployments on the findings

## __Agent Registry__
  - ### Features
    - AWS Agent Registry adds support for custom metadata. Discovery APIs now return custom metadata on registry records and support filtering by metadata fields. Semantic search includes custom metadata for improved relevance. Filter customMetadata fields using eq, ne, and in operators.

## __Agent Registry Control__
  - ### Features
    - AWS Agent Registry adds support for custom metadata. Define a typed metadata schema on your registry and attach structured key-value metadata to registry records. Schemas are additive only. Enforcement is progressive. Records show a compliance status computed against the current schema.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Amazon Bedrock AgentCore Gateway now supports returning the complete MCP tools list in a single response by disabling pagination for the tools list operation. This feature is available in limited preview.

## __Amazon Connect Service__
  - ### Features
    - This release adds ConnectionTypes and ChatStreamingConfiguration to StartChatContact, and ConnectionCredentials, Websocket, and StreamingId to its response, so customers can request connection information and chat streaming in the same call that starts the chat.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - API changes to AWS Client VPN to support device posture assessment and Cedar authorization policies

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - An optional customer provided prefix used to construct the hostname of the Argo CD server endpoint for EKS Argo CD Capability.

## __Amazon FSx__
  - ### Features
    - Amazon FSx has expanded the model-level maximum on the ThroughputCapacity, ThroughputCapacityPerHAPair, and Iops API parameters. Actual supported values are unchanged and depend on file system type and configuration.

## __Amazon GuardDuty__
  - ### Features
    - Adding awsServiceName field to GuardDuty Findings

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Add support for sharing SSM documents with organizations and OUs using RAM.

# __2.55.6__ __2026-09-25__
## __ARC - Region switch__
  - ### Features
    - Adds a service quota checker to Region switch to verify quota parity between your primary and standby Region, and automatically submit quota limit increases. Adds an optional EC2 Auto Scaling and ECS setting that waits for instances or tasks in the scaled-up Region to be healthy in target groups.

## __AWS Glue__
  - ### Features
    - add support for table level federation

## __AWS MediaConnect__
  - ### Features
    - This release adds support for RTMP push router outputs in AWS Elemental MediaConnect.

## __AWS Security Agent__
  - ### Features
    - This release adds the ListActorMessages operation, which returns the multi-factor authentication messages received at an actor's server-generated email address

## __AWS Well-Architected Tool__
  - ### Features
    - This change releases the Well-Architected Agent, a generative AI service that analyzes a customer's AWS environment and delivers personalized, prioritized recommendations across cost, security, performance, and resilience.

## __Agents for Amazon Bedrock__
  - ### Features
    - Adds support for calling VPC configuration API's in Bedrock. These configurations allow the use of On Prem connectors in Bedrock Managed Knowledge bases

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Amazon Bedrock AgentCore Payments now supports credential rotation for payment connectors, letting you rotate API and wallet secrets for Quick Create payment auths from the console. This release also adds Type and Creation type columns to the payment managers views.

## __Amazon Connect Service__
  - ### Features
    - Agent Privacy During Hold is a new privacy capability for Amazon Connect Voice that prevents agent audio from being captured in call recordings or Contact Lens conversational analytics during hold. When enabled, agents are automatically muted on entering hold and unmuted on resuming the contact

## __Amazon Neptune Graph__
  - ### Features
    - Add GraphIdentifier filter for ListImportTasks

## __Amazon Q Connect__
  - ### Features
    - Release shapes for the proactive agentic recommendations and the multi-knowledge base search features. Increases the maximum length of QuickResponseContent.

## __Amazon Rekognition__
  - ### Features
    - This release adds support for Feedback and Metadata in the GetFaceLivenessSessionResults response. Feedback returns codes explaining why a Face Liveness check produced its result. Metadata includes the client SDK type.

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


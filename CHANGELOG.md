 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.57.6__ __2026-10-28__
## __AWS Artifact__
  - ### Features
    - Update support mode for Assurance Assistance

## __AWS Certificate Manager__
  - ### Features
    - AWS Certificate Manager now supports the Automatic Certificate Management Environment (ACME) protocol in the AWS GovCloud (US-East) and AWS GovCloud (US-West) Regions.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports SMPTE 2022-7 seamless protection switching for multicast inputs on MediaLive Anywhere. Support for MulticastSettings sources to add a redundant copy of the stream received on a second network interface.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed an issue where `ProfileCredentialsProvider` could keep returning credentials from the previous profile file after a reload, either to concurrent callers or after building credentials from the new file failed.
        - Contributed by: [@gibkim](https://github.com/gibkim)

## __AWS Security Agent__
  - ### Features
    - Adds support for attaching client certificates for mutual TLS (mTLS) authentication to penetration test target endpoints via the new ClientCertificates field on Assets, enabling pentesting of endpoints that require client certificate authentication.

## __Amazon Connect Participant Service__
  - ### Features
    - Amazon Connect Participant Service now supports the optional UrlContentDisposition parameter on GetAttachment, which returns a pre-signed URL that renders a chat attachment inline in the browser instead of forcing a download.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect APIs - CreateTaskTemplate and UpdateTaskTemplate accept a new ContactHooks parameter called WrapUpFlowId to identify the flow module that runs when an agent attempts to disconnect from a task created with the template. Use this to verify an agent's work or update external case repos.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 Capacity Manager adds supportability checks and capacity fulfillment metrics. Use GetCapacityManagerSupportabilityScores to score how well instance types and Availability Zones can support a capacity request, and GetCapacityManagerMetricData to retrieve fulfillment metrics.

## __Amazon Interactive Video Service__
  - ### Features
    - InsertAdBreak now allows setting a customized adBreakId on a request and also adds a new field targetStartAt for a delayed ad insertion

## __Amazon NeptuneData__
  - ### Features
    - Get Storage API analyzes storage usage to identify database bloat and your storage footprint. Index Rebuild API triggers on-demand index reconstruction to eliminate bloat and restore query performance.

## __Amazon OpenSearch Service__
  - ### Features
    - This release adds APIs to simplify setting up a replication relationship between OpenSearch clusters. The APIs include new Switchover and Failover actions for planned switchovers and unplanned failovers to ensure high availability for OpenSearch in the event of disaster in a single region.

## __Managed Streaming for Kafka Connect__
  - ### Features
    - Amazon MSK Connect now supports updating a connector's custom plugin and worker configuration via the UpdateConnector API. MSK Connect checks connector and task health after the update and rolls back on failure. Each call returns an operation ARN to track with DescribeConnectorOperation.

## __Contributors__
Special thanks to the following contributors to this release: 

[@gibkim](https://github.com/gibkim)
# __2.57.5__ __2026-10-27__
## __AWS License Manager__
  - ### Features
    - AWS License Manager now supports delegate grants for read-only visibility into license distributions. CreateGrant accepts a new GrantType (BuyerGrant or DelegateGrant) and a desired status at distribution time.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds the ResetTokenVaultKeyMaterial API, which restores a token vault whose customer managed key was deleted by moving it to an AWS owned key. The reset permanently deletes the stored OAuth 2.0 tokens, users must consent again. Adds on-behalf-of token exchange client authentication override option.

## __Amazon EC2 Container Service__
  - ### Features
    - ECS now supports GPU sharing via NVIDIA Multi-Process Service (MPS) on ECS Managed Instances and ECS EC2, allowing multiple ECS tasks to share a single Nvidia GPU for improved utilization.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - EC2 AMIs can now show vulnerability data and be targeted directly for scanning.

## __Amazon Lightsail__
  - ### Features
    - Added the attributeUpdates parameter to the Lightsail StartInstance API, which allows customers to update the attributes of a stopped instance when starting it.

## __Inspector2__
  - ### Features
    - Adding support for Machine Image scan

# __2.57.4__ __2026-10-26__
## __AWS Billing__
  - ### Features
    - Adds support for managing private pricing contracts and contractual investments including listing and associating contracts, retrieving contractual investments and their summaries, and creating, listing, and canceling contractual investment attestations

## __AWS CRT HTTP Client__
  - ### Features
    - Add `useTrustStoreSystemProperties` to `AwsCrtHttpClient.Builder` and `AwsCrtAsyncHttpClient.Builder`. When enabled, the client trusts the certificates in the trust store specified by the `javax.net.ssl.trustStore*` JSSE system properties instead of the OS trust store. Disabled by default.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now lets you control whether explicit weighted prediction is applied to H.264 outputs. It also introduces Perceptual Filter, a new noise reduction filter designed to improve video quality by increasing VMAF scores, now with Auto, None, and strength options.

## __AWS Key Management Service__
  - ### Features
    - This release adds support for ML-KEM (Module-Lattice-Based Key-Encapsulation Mechanism) keys in AWS KMS. ML-KEM, as specified in NIST FIPS 203, is a post-quantum key encapsulation mechanism that can be used to establish a shared secret between two parties.

## __AWS Marketplace Catalog Service__
  - ### Features
    - This release adds support for the SubsidiaryAuthorization entity type in the ListEntities operation, including entity-type filters with partial name search, sort options, and a summary returned for each subsidiary authorization.

## __AWS MediaConnect__
  - ### Features
    - This release adds support for managing and sharing endpoint across accounts in AWS Elemental MediaConnect. You can now register endpoints to represent external or service-managed sources and destinations, and share them across AWS accounts.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Sign-In Service__
  - ### Features
    - Adds support for the OAuth 2.0 Token Exchange grant (RFC 8693), enabling applications to exchange identity provider issued JSON Web Tokens (JWTs) for short-lived OAuth access tokens to access supported AWS services without managing separate AWS credentials.

## __AWS Single Sign-On Admin__
  - ### Features
    - Added support for the RegionScope parameter on ListInstances, allowing you to list IAM Identity Center instances in the current Region or across all Regions.

## __AWSDeadlineCloud__
  - ### Features
    - fix the sdk documenation issue only

## __Amazon AppStream__
  - ### Features
    - Add Windows 11 to PlatformType enum

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect now automatically saves agents' in-progress email drafts preserving the message, recipients, and subject during browser or network interruptions so agents can return to the mail and continue instead of starting over.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Added strategy-based UUID generation to DynamoDbAutoGeneratedUuid with ALWAYS (default) and CREATE modes.

## __Amazon Elastic Container Registry__
  - ### Features
    - Add support for ECR replication backfill

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - This release adds support for EKS management of Gateway API CRDs (Custom Resource Definitions)

## __Amazon Interactive Video Service__
  - ### Features
    - The release adds failover slate configurations, which show viewers a slate during temporary broadcaster network disruptions to reduce viewer drop-off and refresh storms.

## __Amazon Lightsail__
  - ### Features
    - This release adds Web Application Firewall (WAF) support for Lightsail distribution.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker HyperPod adds the MultiNodeTrainingValidation deep health check for clusters orchestrated by Amazon EKS. It validates NVIDIA GPU instances by running a short multi-node training workload and flags instances that produce numerical errors or fall behind expected training time.

## __EC2 Metadata Client__
  - ### Bugfixes
    - Validate that the metadata path passed to `Ec2MetadataClient#get` and `Ec2MetadataAsyncClient#get` cannot change the scheme, host, or port of the configured IMDS endpoint.

## __Tax Settings__
  - ### Features
    - Add Mexico Additional Info changes

## __odb__
  - ### Features
    - Adds support for Exascale storage with Oracle Exadata Database Service on Dedicated Infrastructure

# __2.57.3__ __2026-10-22__
## __AWS Continuum__
  - ### Features
    - Releasing Continuum for Code Vulnerabilities APIs for SDKs

## __AWS Elemental Inference__
  - ### Features
    - Elemental Inference now supports feed scheduling, letting you apply feed configuration changes at a time you choose instead of building your own automation.

## __AWS Glue__
  - ### Features
    - Add support for large worker types in AWS Glue

## __AWS Lambda__
  - ### Features
    - AWS Lambda durable functions now supports payloads up to 1 MB for checkpoints, callback completion, and error details.

## __AWS Network Manager__
  - ### Features
    - AWS Cloud WAN now provides managed one-to-one NAT directly on it's core network attachments, through integration with Regional Private NAT Gateway. Use UpdateAttachmentNatConfiguration to enable NAT and configure NAT rules, and ListAttachmentNatConfigurations to view them.

## __AWS SDK for Java v2__
  - ### Features
    - Port upstream changes to eventstream serde into the `eventstreams` module.
    - Updated endpoint and partition metadata.

## __AWS Telco Network Builder__
  - ### Removals
    - Remove end-of-life service client: `tnb`

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds the dimension name to evaluation results and batch evaluation summaries. For multi-dimensional LLM-as-a-Judge evaluators, each result and summary now identifies which dimension it corresponds to.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds model-based routing and fallback to AgentCore Gateway rules. Rules can match the requested model, route to a specific model, and fall back across inference targets with an optional circuit breaker. Adds multi-dimensional rating scales for AgentCore Evaluations LLM-as-a-Judge evaluators.

## __Amazon Connect Service__
  - ### Features
    - Adds support for customer queryable replication errors.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - AWS Transit Gateway now provides managed one-to-one NAT directly on its attachments, through integration with Regional Private NAT Gateway. Use ModifyTransitGatewayAttachmentNatConfiguration to enable NAT and configure NAT rules, and GetTransitGatewayAttachmentNatConfiguration to view them.

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - Adds RevokeConnectionToken operation for Amazon IVS Real-Time Streaming.

## __Amazon S3__
  - ### Bugfixes
    - Fixed an issue where presigned multipart downloads could hang when the server returned a Content-Range value outside the long range.

## __FinSpace Public API__
  - ### Removals
    - Remove end-of-life service client: `finspacedata`

## __FinSpace User Environment Management service__
  - ### Removals
    - Remove end-of-life service client: `finspace`

## __Managed Streaming for Kafka Connect__
  - ### Features
    - Amazon MSK Connect now supports pausing and resuming connectors via the asynchronous PauseConnector and ResumeConnector APIs. Paused connectors keep their configuration and committed offsets and resume where they left off. Each call returns an operation ARN to track with DescribeConnectorOperation.

## __OpenSearch Service Serverless__
  - ### Features
    - Adds APIs to create, get, list, and cancel ingestion jobs, and to create, get, list, and delete ingestion streams, for ingesting data from Amazon S3 into OpenSearch Serverless collections.

# __2.57.2__ __2026-10-21__
## __AWS CRT-based S3 Client__
  - ### Bugfixes
    - Improved the `UnsupportedOperationException` message thrown by `presignedUrlExtension()` on the AWS CRT-based S3 client to explain that presigned URL downloads are not supported and to suggest using a non-CRT `S3AsyncClient`, such as one built with `S3AsyncClient.builder().multipartEnabled(true)`.

## __AWS DevOps Agent Service__
  - ### Features
    - Adds support for Microsoft Teams as a notification service. Associations can now be created and updated with a Microsoft Teams team and channel configuration using the DevOps Agent API and SDKs.

## __AWS Elemental MediaConvert__
  - ### Features
    - Adds AV1 CBR rate control and scene change detection. New sliding window peak bitrate setting for all codecs. DASH adaptation set grouping update. Option to disable the AV1 perceptual quality filter. Support for vertical (portrait) AV1 encoding.

## __AWS Network Firewall__
  - ### Features
    - Adds support for Network Firewall Resolved IP validation.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.
    - Upgrade Jackson (jackson-core, jackson-databind, jackson-dataformat-cbor) to 2.21.8

## __AWS Step Functions__
  - ### Features
    - Release minor updates to API model

## __Amazon Connect Service__
  - ### Features
    - Added support for AWS-managed security profiles.

## __Amazon DataZone__
  - ### Features
    - Added custom designations support with new CRUD APIs. The designation field in project membership and policy grant APIs supports a new enum for custom designations. These APIs also support a new field for custom designation id.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds new response fields to DescribeInstanceStatus and Network Insights analysis results.

## __Amazon HealthLake__
  - ### Features
    - Update the UpdateProfileWithAgent API to return a streaming response to provide more frequent updates on what the agent is doing.

## __Amazon Lex Model Building V2__
  - ### Features
    - Adds support for Assisted NLU - Dialog Manager mode allowing usage of LLMs as the default NLU system and dialog management

## __Amazon Lightsail__
  - ### Features
    - This release adds support for metrics related to system-disk performance.

## __Amazon Polly__
  - ### Features
    - Added support for the new voices - Kenneth (en-US), Nate (en-US), Tina (de-DE), Carlos (es-US), Carolina (pt-BR), Leo (pt-BR), Curtis (en-AU), Kiara (en-IN), Arjun (en-IN) and Hyunduk (ko-KR). They are available as a Generative voices only.

## __Amazon QuickSight__
  - ### Features
    - Adds support for SharePoint and OneDrive two-legged OAuth certificate credentials and Google Workspace service account credentials when creating data sources

## __Auto Scaling__
  - ### Features
    - EC2 Auto Scaling now supports account settings, letting you manage account-level configuration for Auto Scaling.

## __Redshift Data API Service__
  - ### Features
    - Add Redshift QueryUUID field to DescribeStatement response

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fixed how `S3TransferManager#resumeDownloadFile` handles an object that changes during a paused download when using the Java-based S3 async client.

  - ### Documentations
    - Corrected the `S3TransferManager` Javadoc: `downloadFileWithPresignedUrl` and `downloadWithPresignedUrl` do not currently support AWS CRT-based S3 clients, and `S3TransferManager.create()` falls back to a multipart-enabled `S3AsyncClient` when AWS CRT is not on the classpath.

# __2.57.1__ __2026-10-20__
## __AWS Clean Rooms Service__
  - ### Features
    - This release allows customers and their partners to easily collaborate with data stored in Databricks, without having to move or share their underlying data among collaborators.

## __AWS Marketplace Catalog Service__
  - ### Features
    - Adds ListEntities filter, sort, and summary support for the ExpressPrivateOfferConfiguration entity type, including filtering by ProductId.

## __Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Guardrails now supports a configurable tier for the contextual grounding policy via a new tier option. Select the Standard tier for grounding that better supports conversations and reasoning, with expanded language coverage.

## __Amazon CloudWatch__
  - ### Features
    - Amazon CloudWatch now supports two new anomaly detector settings. AdaptationSpeed (FAST, NORMAL, SLOW) controls how fast the model adapts to baseline changes, and OutlierFilteringStrength (MINIMAL, MODERATE, STRICT) controls how aggressively outliers are filtered.

## __Amazon DynamoDB__
  - ### Features
    - Adds support for disassociating a replica from an Amazon DynamoDB multi-Region eventual consistency global table, so you can keep it as a single-Region table with its existing data. Also adds disassociation protection, on by default, to help prevent accidental disassociation.

## __Amazon EC2 Container Service__
  - ### Features
    - ECS Service Gateway makes it easier for developers to deploy ECS services without having to worry about complex ingress networking constructs.

## __Amazon ElastiCache__
  - ### Features
    - LiveMigration enables customers to migrate from self-hosted EC2 Redis and Valkey clusters to ElastiCache Serverless with near-zero downtime. Customers control the migration through three API-driven phases (prepare, start, complete) that handle traffic proxying, data replication, and cutover.

## __Amazon Relational Database Service__
  - ### Features
    - Amazon RDS now supports On-Demand Capacity Reservations for DB instances.

## __Amazon S3 Files__
  - ### Features
    - S3 Files adds support for the "Misconfigured" file system status.

## __Amazon Workspaces Instances__
  - ### Features
    - Documentation update release for AWS service

## __CloudWatch Omni__
  - ### Features
    - Updating max value for maxResults in SearchPrincipals API

# __2.57.0__ __2026-10-19__
## __AWS Amplify__
  - ### Features
    - Documentation update release for AWS service

## __AWS CodeCommit__
  - ### Features
    - Documentation update release for AWS service

## __AWS CodeConnections__
  - ### Features
    - Documentation update release for AWS service

## __AWS CodeDeploy__
  - ### Features
    - Adds support for suspending Amazon EC2 Auto Scaling processes during CodeDeploy EC2 in-place deployments. Use ec2DeploymentOptions to choose which processes are suspended and resumed.

## __AWS CodePipeline__
  - ### Features
    - Documentation update release for AWS service

## __AWS CodeStar Notifications__
  - ### Features
    - Documentation update release for AWS service

## __AWS Control Tower__
  - ### Features
    - Documentation update release for AWS service

## __AWS DevOps Agent Service__
  - ### Features
    - Documentation update release for AWS service

## __AWS End User Messaging Social__
  - ### Features
    - Adding support for WhatsApp Direct Send.

## __AWS Glue DataBrew__
  - ### Features
    - Documentation update release for AWS service

## __AWS IoT Wireless__
  - ### Features
    - Documentation update release for AWS service

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Improve the performance of parsing profile files with many property continuation lines.
    - Releases HTTP response resources when an execution interceptor fails during response processing, preventing connection-pool leaks in synchronous and asynchronous clients.

  - ### Removals
    - Remove the `software.amazon.eventstream:eventstream` dependency.

## __AWS X-Ray__
  - ### Features
    - Adds support for encrypting the Transaction Search spans log group with a customer managed AWS KMS key. Pass the key ARN in the new CloudWatchLogsConfiguration.SpansLogGroupKmsKeyId field of UpdateTraceSegmentDestination. X-Ray applies the key when it creates the log group.

## __Agents for Amazon Bedrock__
  - ### Features
    - Adds Retrieval Instructions for Amazon Bedrock Knowledge Bases. Create, get, update, delete, list, and version customer-authored instructions that guide how a knowledge base retrieves and answers, with content review before use.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Adds support for applying a Retrieval Instruction, optionally by version, to AgenticRetrieveStream requests for Amazon Bedrock Knowledge Bases, and reports the applied instruction and version on the trace.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds the Builtin.FailureAnalysis evaluator for batch evaluations. GetBatchEvaluation now returns insightResults that group session failures into insights with root causes and suggested fixes.

## __Amazon CloudFront__
  - ### Bugfixes
    - Fixed an issue where `CloudFrontClient` and `CloudFrontAsyncClient` had no paginator methods

## __Amazon CloudWatch__
  - ### Features
    - This release adds ActionConfiguration to PutMetricAlarm, PutCompositeAlarm and PutLogAlarm. It allows customers to include an alarm's tags in its notifications and alarm state change events

## __Amazon CloudWatch Logs__
  - ### Features
    - Amazon CloudWatch Logs now supports filtering DescribeDeliverySources by resourceArns, DescribeDeliveryDestinations by names, and DescribeDeliveries by deliverySourceNames.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect GetAttachedFile API now supports an optional UrlContentDisposition parameter. Set it to INLINE to get a pre-signed URL that displays the file in the browser, so agents can preview email attachments without downloading them. ATTACHMENT (download) remains the default.

## __Amazon DataZone__
  - ### Features
    - Adds SDK and CLI support for the Iceberg REST catalog connection types already available in Amazon SageMaker Unified Studio - ICEBERGRESTCATALOG, SNOWFLAKEICEBERGRESTCATALOG, and DATABRICKSICEBERGRESTCATALOG.

## __Amazon DynamoDB__
  - ### Features
    - Enable account endpoint generation for existing CN and GovCloud regions

## __Amazon DynamoDB Enhanced Client__
  - ### Documentations
    - Clarified in the UpdateItemEnhancedRequest Javadoc that ignoreNulls and IgnoreNullsMode don't apply to the version attribute.
        - Contributed by: [@adev-code](https://github.com/adev-code)

## __Amazon EC2 Container Service__
  - ### Features
    - Adds support for configurable envoy application log levels. Customers can now debug network connectivity issues with their task's sidecar envoys.

## __Amazon EMR Containers__
  - ### Features
    - Documentation update release for AWS service

## __Amazon GuardDuty__
  - ### Features
    - Adding a new Expected Bucket Owner optional field for GuardDuty publishing destination APIs

## __Amazon Managed Grafana__
  - ### Features
    - Documentation update release for AWS service

## __Amazon OpenSearch Service__
  - ### Features
    - Amazon OpenSearch Service adds a new TLS security policy to support FIPS policy and post-quantum cryptography

## __Amazon WorkSpaces__
  - ### Features
    - Adds optional imageValidationMode enum to ImportCustomWorkspaceImageRequest. This allows customers to opt-in to having prevalidation run in the service account instead of customer account.

## __Amazon Workspaces Instances__
  - ### Features
    - ModifyVolume is a new public API for WorkSpaces Core Managed Instances. Customers can change the size, type, IOPS, or throughput of an existing managed volume online, with no stop required. Customers track progress by calling EC2 DescribeVolumesModifications directly.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Adding support for EKS Container Insights Telemetry Rules

## __Elastic Disaster Recovery Service__
  - ### Features
    - Documentation update release for AWS service

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fixed an issue where `S3TransferManager#downloadDirectory` failed the entire directory download when an object's key mapped to a path outside the destination directory or to a path that is not valid on the local file system. Such objects are now skipped and reported in `CompletedDirectoryDownload#failedTransfers()`, and the remaining objects are still downloaded.
    - Fixed an issue where `S3TransferManager#uploadDirectory` with a custom `s3Delimiter` silently uploaded two local files to the same key when the delimiter appeared in a file or directory name, for example `a/b` and `a-b` with the delimiter `-`. Only the first file is now uploaded to that key, and each other file that maps to it is reported in `CompletedDirectoryUpload#failedTransfers()`.

## __Timestream InfluxDB__
  - ### Features
    - Adds four new parameters to the InfluxDBv3Enterprise parameter group for Amazon Timestream for InfluxDB. The new parameters are upgradePachaTree, walBufferSize, snapshotSize, and snapshotDuration.

## __Contributors__
Special thanks to the following contributors to this release: 

[@adev-code](https://github.com/adev-code)

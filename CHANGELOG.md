 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.46.20__ __2026-07-01__
## __AWS Artifact__
  - ### Features
    - Add support for Assurance Assistant APIs for managing compliance inquiries along with tagging features.

## __AWS Cloud9__
  - ### Features
    - Since Amazon Linux 2 (AL2) will reach its end-of-life (EOL) and stop receiving security updates on June 30, 2026, Cloud9 will remove AL2 from AMI options in public API create-environment-ec2.

## __AWS Elemental MediaConvert__
  - ### Features
    - Adds support for integer-second duration normalization and the option to disable explicit weighted prediction.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSMarketplace Metering__
  - ### Features
    - The usage reporting window for the BatchMeterUsage API has been extended from 6 hours to 24 hours. Sellers can now submit usage records for up to 24 hours after a metered event occurs.

## __Amazon Connect Service__
  - ### Features
    - Adds a new Amazon Connect Service API, SendOutboundWebNotification, that delivers web notifications to end-customer chat widget sessions. Callable only by the Amazon Connect Outbound Campaigns service principal.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Use declarative policies to enable VPC Encryption Controls across your organization or select accounts. Added AMD SEV-SNP support for EC2 Dedicated Hosts. Managed resource visibility settings control whether AWS-provisioned resources in your account appear in console views and API list operations.

## __Amazon GameLift Streams__
  - ### Features
    - Added CreateStreamSessionAdminShell API operation to enable customers to establish secure terminal connections to the live runtime environment of streaming sessions for troubleshooting purposes.

## __Amazon OpenSearch Service__
  - ### Features
    - To create a Mustang domain via the AWS CLI, you must pass EngineMode OPTIMIZED (along with UseCase OBSERVABILITY or MIXED)  without it, the domain defaults to a regular (GENERAL) domain. Also this release includes Insights Feedback API which user can use to provide feedback for Insight API.

## __Amazon QuickSight__
  - ### Features
    - Adding support for FileSource PhysicalTables.  This adds support for datasets with file sources.

# __2.46.19__ __2026-06-30__
## __AWS Certificate Manager__
  - ### Features
    - AWS Certificate Manager now supports the Automatic Certificate Management Environment (ACME) protocol to issue public certificates. ACME is an industry-standard protocol for automating certificate lifecycle on customer-managed infrastructure such as on-premises servers and Kubernetes clusters.

## __AWS Clean Rooms Service__
  - ### Features
    - Adds support for intermediate tables in AWS Clean Rooms collaborations.

## __AWS CloudFormation__
  - ### Features
    - AWS CloudFormation adds a DeploymentConfig parameter to enable Express mode, which completes stack operations as soon as resource configuration is applied. Also adds a DisableValidation parameter to skip pre-deployment validation, which now runs automatically on CreateStack and UpdateStak.

## __AWS CodeBuild__
  - ### Features
    - Adds support for host kernel selection for on-demand builds.

## __AWS Network Firewall__
  - ### Features
    - AWS Network Firewall now supports container associations for monitoring ECS and EKS workloads. You can create container associations to dynamically track the IP addresses of running containers in your Amazon ECS and Amazon EKS clusters.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Single Sign-On Admin__
  - ### Features
    - AWS IAM Identity Center now returns PrimaryRegion and Regions in the ListInstances response, providing information about replicated instances.

## __Amazon CloudWatch__
  - ### Features
    - Customers can configure alarms with wall-clock-aligned evaluation windows instead of sliding windows, with optional timezone support for daily or weekly periods

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect - Added CreateAttachedFile and StartContactConversationalAnalyticsJob APIs to import call recordings and run conversational analytics.

## __Amazon DataZone__
  - ### Features
    - Amazon DataZone now supports SNOWFLAKE as a connection type in the CreateConnection API, enabling metadata and lineage retrieval from Snowflake databases. Specify snowflakeProperties with connection details, a Secrets Manager secret, an Athena spill bucket, and an identity mapping for Snowflake.

## __Amazon EC2 Container Service__
  - ### Features
    - Updated threshold configuration documentation.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds ModifyVpcEndpointPayerResponsibility API, which enables VPC endpoint service owners to modify the billing account for VPC endpoint usage charges at the individual endpoint level

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds Kubernetes version rollback support, including the CancelUpdate operation to cancel an in-progress VersionRollback update, the RollbackConfig structure with a timeoutMinutes field, and the Cancellation structure surfaced via the new cancellation field on the Update object.

## __Auto Scaling__
  - ### Features
    - This release adds support for a new reservations-then-balanced capacity distribution strategy, which first attempts to launch instances into your Capacity Reservations and then balances remaining capacity across healthy Availability Zones.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Organization and account level telemetry rule via Observability Admin and CloudWatch pipelines for metrics

## __Partner Central Selling API__
  - ### Features
    - This release adds AwsMarketplaceSolutions and AwsMarketplaceProducts entity types to the Associate and Disassociate APIs, returns them in GetOpportunity, and adds AwsMarketplaceSolutionArn to ListSolutions ,letting partners link Marketplace listings directly to opportunities.

## __SupportAuthZ__
  - ### Features
    - New SDK release for SupportAuthZ.

# __2.46.18__ __2026-06-29__
## __AWS Glue__
  - ### Features
    - Added the UpdateAsset operation to set the business name and description for an existing AWS Glue Data Catalog asset.

## __AWS Lambda__
  - ### Features
    - Lambda now supports self-managed S3 buckets for Lambda code storage giving you the option for Lambda to reference a copy of your source code from your own S3 buckets. This allows you to maintain a single copy of your source code and manage your own code storage limits.

## __AWS Parallel Computing Service__
  - ### Features
    - Add support for in-place Slurm version upgrades on existing clusters by accepting scheduler.version in UpdateCluster.

## __AWS RDS DataService__
  - ### Features
    - Updated documentation to remove Aurora Serverless V1 references.

## __AWS Resource Explorer__
  - ### Features
    - Added CFN resource type fields for Search and ListSupportedResourceTypes responses. Added SLRec field for ServiceView

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Codegen now fails with a clear model validation error identifying the offending shape, member, and unresolved target when a shape member references a shape that does not exist in the model, instead of a NullPointerException during code generation.
    - Include the sdk-core mime.types resource in the native-image resource configuration so default MIME type detection works in GraalVM native images. Fixes [#7063](https://github.com/aws/aws-sdk-java-v2/issues/7063)
        - Contributed by: [@sjh9714](https://github.com/sjh9714)

## __AWS SDK for Java v2 Codegen__
  - ### Bugfixes
    - Add support for generating endpoint tests with integer parameter values.
    - Validate paginators before generating

## __AWS WAFV2__
  - ### Features
    - AWS WAF added support for associating AWS WAF web ACLs with Amazon Bedrock AgentCore Gateway resources. You can now use AssociateWebACL, DisassociateWebACL, GetWebACLForResource, and ListResourcesForWebACL to protect your AgentCore Gateways with AWS WAF.

## __Amazon AppConfig__
  - ### Features
    - AWS AppConfig introduces Experimentation tools - enhanced capabilities within AWS AppConfig that enable you to run AB tests, multivariate tests, and gradual feature rollouts across your application stack.

## __Amazon CloudWatch__
  - ### Features
    - This release adds the API (PutLogAlarm) to manage a new CloudWatch resource, Log Based Alarms. Log Based Alarms allows customers to alarm directly on CloudWatch Logs query results.

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS now supports customizable deployment circuit breaker configurations. Customers can now define the failure threshold or control the failure counting mechanism.

## __Amazon ElastiCache__
  - ### Features
    - Updated documentation for the ApplyImmediately parameter in ModifyCacheCluster and ModifyReplicationGroup to clarify modification behavior.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds support for the precision time strategy and a parentGroupId parameter on CreatePlacementGroup and DescribePlacementGroups. Precision time placement groups and cluster placement groups with a parent precision time placement group ensure instances launch on precision time capable hardware.

## __Amazon Elastic VMware Service__
  - ### Features
    - Amazon EVS introduces a VMware Cloud Foundation (VCF) self-deployed mode, along with new connectors to VCF components such as the Operations and SDDC managers to monitor coverage and usage.

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - This launch is an expansion of our Q1 RCS for business launch where we will release an API that supports rich media and interactive messaging elements.

## __Amazon SageMaker Feature Store Runtime__
  - ### Features
    - Add support for ListRecords and BatchWriteRecord APIs to Feature Store.

## __Amazon VPC Lattice__
  - ### Features
    - Amazon VPC Lattice now supports mutable idle timeout configuration on VPC Lattice Services

## __AmazonConnectCampaignServiceV2__
  - ### Features
    - Adding new attributes to PutProfileOutboundRequest API that will create an outbound request call for the customer's Web Notification outbound campaign.

## __Apache 5 HTTP Client__
  - ### Bugfixes
    - Update `httpcore5` to `5.4.3` to fix an issue where early 3xx responses to requests with `Expect: 100-continue` does not cause the client to terminate the request. Fixes [#7047](https://github.com/aws/aws-sdk-java-v2/issues/7047).

## __Connect Health__
  - ### Features
    - Expand input validation to support Unicode characters and markdown table syntax.

## __EC2 Image Builder__
  - ### Features
    - Adds support for AMI watermarks in Image Builder.

## __Contributors__
Special thanks to the following contributors to this release: 

[@sjh9714](https://github.com/sjh9714)
# __2.46.17__ __2026-06-22__
## __Amazon S3__
  - ### Bugfixes
    - Always set 'Expect: 100-continue' when using PUT operations across regions; this enables the correct redirect behavior when the initial request goes to an incorrect region.

## __Managed Streaming for Kafka__
  - ### Features
    - Amazon MSK Replicator now supports mTLS authentication when connecting to external Apache Kafka clusters, enabling customers to replicate data from clusters that require mutual TLS for client authentication. This capability is supported when replicating to Amazon MSK Express brokers.

# __2.46.16__ __2026-06-22__
## __AWS Direct Connect__
  - ### Features
    - Added VIF rate limiting support for AWS Direct Connect, allowing customers to set bandwidth allocations on virtual interfaces to manage traffic on dedicated connections.

## __AWS Lambda__
  - ### Features
    - Add support for tagging Network Connector resources in AWS Lambda.

## __AWS Lambda Core__
  - ### Features
    - Initial release of the AWS Lambda Core SDK with APIs to create, manage, and tag network connectors that enable Lambda compute resources to access private resources in your Amazon VPC.

## __AWS MediaConnect__
  - ### Features
    - AWS MediaConnect now supports Content Quality Analysis for Router Inputs, enabling detection of black frames, frozen frames, and silent audio with configurable thresholds.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds an optional extractionMode field to CreateEvent. SKIP retains the event in short-term memory but excludes it from long-term memory extraction.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - Application Signals now supports dynamic instrumentation and Service Events telemetry. Add instrumentation at runtime without restarts, and use fine-grained profiling data to quickly pinpoint latency and error root causes.

## __Amazon CloudWatch Logs__
  - ### Features
    - CloudWatch Logs Updates - New APIs introduced to support syslog ingestion to a log group. For more information, see CloudWatch Logs API documentation.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for AMI Watermark and Allowed AMIs integration

## __Amazon GuardDuty__
  - ### Features
    - Added AI-powered investigations that automatically analyze security findings, correlate related activity, and produce structured summaries with risk assessment, confidence scoring, MITRE technique classification, and actionable next steps.

## __Amazon Omics__
  - ### Features
    - Adds support for scratch ephemeral storage mounted at tmp

## __Amazon QuickSight__
  - ### Features
    - Updated the Amazon Quick Spaces API to remove unsupported SPACE and ARTIFACT values from the SpaceQuickSightResourceType enum.

## __Lambda MicroVMs__
  - ### Features
    - Lambda MicroVMs GA launch. Lambda MicroVMs enable isolated and highly responsive execution of user-supplied or LLM-generated code.

## __Managed Streaming for Kafka__
  - ### Features
    - Amazon MSK Replicator now supports mTLS authentication when connecting to external Apache Kafka clusters, enabling customers to replicate data from clusters that require mutual TLS for client authentication. This capability is supported when replicating to Amazon MSK Express brokers.

# __2.46.15__ __2026-06-19__
## __AWS Glue__
  - ### Features
    - Adds the SearchAssets operation for discovering assets in the AWS Glue Data Catalog using full-text search and filters. Minor naming refinements across the Glossary Terms and Attachment APIs for consistency.

## __Agents for Amazon Bedrock__
  - ### Features
    - Add support for metadata-only retrieval on GetFlow, GetFlowVersion, and GetPrompt APIs.

## __Amazon AppStream__
  - ### Features
    - Amazon WorkSpaces Agent Access now supports domain-joined fleets for enterprise identity integration, real-time agent observation with instant stop controls, and MCP tool forwarding for lower-latency, cost-effective desktop tool access.

## __Amazon Connect Service__
  - ### Features
    - This is the release for point based scoring system and the evaluation form validation project

## __Amazon OpenSearch Service__
  - ### Features
    - This release introduces data source attachment APIs, enabling users to attach and detach Amazon OpenSearch Service domains and Amazon OpenSearch Serverless collections to an OpenSearch application.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Decorate streaming response publisher failures so S3AsyncClient getObject can retry Netty read timeouts.
        - Contributed by: [@goutamadwant](https://github.com/goutamadwant)

## __Contributors__
Special thanks to the following contributors to this release: 

[@goutamadwant](https://github.com/goutamadwant)
# __2.46.14__ __2026-06-18__
## __AWS Batch__
  - ### Features
    - Adds Support for ordered allocation strategies- BEST-FIT-PROGRESSIVE-ORDERED or SPOT-CAPACITY-OPTIMIZED-PRIORITIZED

## __AWS Compute Optimizer__
  - ### Features
    - This release surfaces two new metrics Volume IOPS Exceeded and Volume Throughput Exceeded into EBS volume rightsizing recommendations.

## __AWS Lambda__
  - ### Features
    - Converging and fixing existing documentation gaps in Lambda SDK

## __Amazon CloudWatch Logs__
  - ### Features
    - Added optional startFromHead parameter to FilterLogEvents enabling descending timestamp order (newest first) when set to false. Default true preserves existing ascending order. Reverse sorting requires a startTime on or after Jan 1, 2024.

## __Amazon Cognito Identity Provider__
  - ### Features
    - In order to support the new TLS Self-Service feature, this change adds SecurityPolicyType to CustomDomainConfigType. During CreateUserPoolDomain and UpdateUserPoolDomain this is used to select a custom domain's TLS enforcement, and for DescribeUserPoolDomain it informs users about the current TLS.

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS services now support high resolution (20 second) CloudWatch metrics for CPUUtilization and MemoryUtilization. Use these metrics for faster service auto scaling.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates clarifying CancelCapacityReservation cancellable states

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds support for configurable control plane egress routing in Amazon EKS, allowing you to route control plane egress traffic through your VPC and control how the control plane reaches resources in your network such as webhook servers and OIDC providers.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers has launched support for customizing Linux capabilities in container fleets. You can now specify additional Linux capabilities for containers in a container group definition, giving you finer control over the default Docker capabilities available to your containers.

## __Amazon HealthLake__
  - ### Features
    - Adding New Configurations to the FHIR Create Datastore. The new configurations include NLP Configuration, AnalyticsConfiguration, ProfileConfiguration

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for automatic AMI patching on HyperPod clusters. Customers can configure patching strategies to automatically apply security patch with zero job termination. Customers can also specify an AMI version at instance group level and update cluster software to a certain AMI version.

## __Application Auto Scaling__
  - ### Features
    - Adds support for ECS high-resolution predefined scaling metrics (ECSServiceAverageCPUUtilizationHighResolution, ECSServiceAverageMemoryUtilizationHighResolution) enabling 20-second metric periods for faster scaling

## __Synthetics__
  - ### Features
    - CloudWatch Synthetics adds support for multi-location canaries. Customers can now monitor their endpoints from multiple locations with centralized management from a primary location. The SDK includes new parameters for configuring multiple locations and tracking their state.

# __2.46.13__ __2026-06-17__
## __AWS DevOps Agent Service__
  - ### Features
    - Adds support for Remote A2A (Agent-to-Agent) agent registration and management. Adds new Release Readiness Review and Release Testing capabilities. Adds support for Git managed skills in AWS DevOps Agent.

## __AWS Glue__
  - ### Features
    - This release adds support for Search and Discovery in AWS Glue, letting you and your applications search Data Catalog assets such as table and enrich them with business context and glossary terms.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix excessive memory usage in AsyncRequestBody.fromInputStream() when reading from streams that return small chunks (e.g., PipedInputStream) by right-sizing the read buffer based on InputStream.available() and trimming oversized backing arrays before they're held by downstream pipelines.

## __AWS Security Agent__
  - ### Features
    - Updated AWS Security Agent SDK model with new APIs for threat modeling, code review, security requirements, and additional integration providers.

## __Agents for Amazon Bedrock__
  - ### Features
    - Launching Bedrock Managed Knowledge Bases. Added support for resource-based policies on Knowledge Base resources, enabling cross-account access for Managed Knowledge Bases.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Adds new AgenticRetrieveStream API for managed knowledge bases to use conversation history and autonomously plan for multi-hop multi-KB reasoning with built-in evaluation and access-control. Updates Retrieve API for access-control-based filtering for managed knowledge bases.

## __Amazon Bedrock AgentCore__
  - ### Features
    - AgentCore Harness service will be Generally Available at NYS 2026 with this Treb release. Harness will support invoking specific endpoints via the qualifier parameter, AWS Skills for pre-built agent capabilities, and improved validation for skill git source URLs.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - AgentCore Gateway now supports inference targets to LLM providers (direct config or built-in connectors), HTTP passthrough targets with session stickiness, runtime target API schemas, AWS WAF web ACL association with configurable fail-open or fail-close modes, and interceptor payload filtering.

## __Amazon EC2 Container Service__
  - ### Features
    - Releasing the ability to bring-your-own task-definition for CreateExpressGatewayService and UpdateGatewayExpressService

## __Amazon OpenSearch Service__
  - ### Features
    - Adds support for configuring IAM Identity Center options on existing OpenSearch applications via the UpdateApplication API.

## __AmazonMQ__
  - ### Features
    - This release adds private networking support for Amazon MQ for RabbitMQ. You can now associate AWS RAM resource shares with your broker and retrieve shared resource details using the new DescribeSharedResources API.

## __Compute Optimizer Automation__
  - ### Features
    - This launch adds IfExists comparison operators to Compute Optimizer Automation rule criteria, so a rule can include recommended actions whose specified attribute isn't present.

## __Partner Central Selling API__
  - ### Features
    - Cosell Resonate AND Prospecing API Launch with ARN correction

## __S3 Event Notification__
  - ### Features
    - Add support for `ObjectAnnotation`.
        - Contributed by: [@parasparani1](https://github.com/parasparani1)

## __Contributors__
Special thanks to the following contributors to this release: 

[@parasparani1](https://github.com/parasparani1)
# __2.46.12__ __2026-06-16__
## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Wrap connection pool acquire timeout and other transient HTTP errors in IOException so the SDK retry layer treats them as retryable.

## __AWS Direct Connect__
  - ### Features
    - Added VIF rate limiting support for AWS Direct Connect, allowing customers to set bandwidth allocations on virtual interfaces to manage traffic on dedicated connections.

## __AWS Outposts__
  - ### Features
    - Adds support for creating an order from quotes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Route 53 Resolver__
  - ### Features
    - Adds supports for PartnerManagedRules

## __Amazon S3 Vectors__
  - ### Features
    - Amazon S3 Vectors now supports paginated QueryVectors requests, returning up to 10,000 results per query.

## __Amazon SageMaker Service__
  - ### Features
    - Add EnableDetailedObservability to Endpoint MetricsConfig. Publishes GPU, host, and framework-native inference metrics to CloudWatch with per-inference-component, availability-zone, and instance dimensions. Adds Inference Component provisioning lifecycle and multi-AZ placement metrics.

## __Amazon Simple Storage Service__
  - ### Features
    - Added support for annotations. You can now attach up to 1000 annotations (up to 1 MB each) directly to objects and create, retrieve, list, and delete them using new annotation APIs. Also added support for configuring an annotation table in S3 Metadata.

## __Partner Central Selling API__
  - ### Features
    - Added Prospecting APIs to convert engagements into AI-enriched leads with scoring insights. Extended Engagement APIs with ProspectingResult and Lead contexts. Added CoSell Scoring to GetAwsOpportunitySummary- quality score, trend, agent-driven recommendations, and engagement classification.

# __2.46.11__ __2026-06-15__
## __AWS WAFV2__
  - ### Features
    - AWS WAF now supports AI traffic monetization for CloudFront. Configure payment networks and pricing on your web ACL, use the new Monetize rule action to charge AI agents via x402, and monitor revenue with new GetRevenueStatisticsSummary, GetRevenueStatistics, and ListSettlementRecords APIs.

## __Amazon Bedrock Runtime__
  - ### Features
    - InvokeGuardrailChecks API evaluates prompts and responses against safety checks (content filters, prompt attacks, sensitive info) without creating guardrail resources. It's a detect-only API, returning numeric scores so you can build adaptive logic as per your application.

## __Amazon CloudWatch Logs__
  - ### Features
    - Added endTimeOffset parameter to Scheduled Queries APIs (Create, Update, Get) enabling bounded time window configuration. Introduced scheduleType filter (CUSTOMER MANAGED, AWS MANAGED) for ListScheduledQueries and exposed it in Get and Update responses.

## __Amazon DataZone__
  - ### Features
    - Adds support for deleting lineage events in Amazon DataZone.

## __Amazon Relational Database Service__
  - ### Features
    - Adding support for RDS SQL Server BYOM and DB2 Community Edition

## __Amazon S3__
  - ### Features
    - Added BufferedSplittableAsyncRequestBody.builder() with bufferBeforeSend option that fully buffers each multipart upload part before sending, fixing NonRetryableException when retrying parts from slow streaming sources.

## __Amazon WorkSpaces__
  - ### Features
    - Added a validation for null check for ImageIds in DescribeWorkspaceImages API request parameters.

## __Application Migration Service__
  - ### Features
    - AWS Transform for VMware now supports Amazon FSx for NetApp ONTAP as a target storage. Customers can migrate source server disks directly to FSx for NetApp ONTAP iSCSI LUNs. Target storage is configurable per source server, and compute, network, and storage migrate together in coordinated waves.

# __2.46.10__ __2026-06-12__
## __AWS Certificate Manager__
  - ### Features
    - Certificate transparency logging opt-out is no longer available. Per compliance requirements, all public ACM certificates are automatically recorded in certificate transparency logs. The CertificateTransparencyLoggingPreference option is deprecated.

## __AWS DevOps Agent Service__
  - ### Features
    - Adds support for Trigger CRUD APIs (CreateTrigger, GetTrigger, UpdateTrigger, DeleteTrigger, ListTriggers) for managing schedule-based automation triggers in DevOps Agent agent spaces.

## __AWS Glue__
  - ### Features
    - Adds support for retrieving Apache Iceberg table metadata via GetTable. Use the new AttributesToGet parameter with LATEST ICEBERG METADATA to receive schema, partition specs, sort orders, and table properties in the response.

## __AWS Identity and Access Management__
  - ### Features
    - Updating documentation for select service-specific credential APIs

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Added tagging and CMK support across optimization, an explanation field in recommendation output, and an insights feature to identify failure patterns, extract user intents, and summarize execution behavior

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Added tagging and CMK support for optimizations and an insights feature to identify failure patterns, extract user intents, and summarize execution behavior

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Patches missing enum values for EKS updates

## __Amazon Kinesis Firehose__
  - ### Features
    - Update KeyARN in DeliveryStreamEncryptionConfigurationInput to accept KMS key ARNs only (not alias ARNs), matching service behavior.

## __Amazon SageMaker Runtime__
  - ### Features
    - Added support for inline request payloads to the InvokeEndpointAsync operation to allow users to provide the inference payload directly in the request Body (up to 128,000 bytes) as an alternative to uploading the payload to Amazon S3 and passing InputLocation.

# __2.46.9__ __2026-06-11__
## __AWS Support__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds support to perform cross account data plane actions on an AgentCore Memory resource

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Supports deterministic metadata for AgentCore Memory

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Introduce new CreateCluster parameters for Amazon EKS local clusters on AWS Outposts. Added etcdInstanceType for configuring the EC2 instance type for dedicated etcd instances, and spreadLevel for configuring the placement group spread level for Kubernetes control plane and etcd instances.

## __Amazon HealthLake__
  - ### Features
    - Adds the UpdateFHIRDatastore API and adds analytics, NLP, and profile configuration support to CreateFHIRDatastore and DescribeFHIRDatastore.

## __Amazon Neptune__
  - ### Features
    - Amazon Neptune now supports IPv6 dual-stack networking. You can create and manage Neptune DB clusters accessible over both IPv4 and IPv6 by specifying NetworkType as DUAL in CreateDBCluster, ModifyDBCluster, RestoreDBClusterFromSnapshot, and RestoreDBClusterToPointInTime API operations

## __Amazon Omics__
  - ### Features
    - Adds support for workflowName in the ListRuns API response.

# __2.46.8__ __2026-06-10__
## __AWS Elemental MediaLive__
  - ### Features
    - Adding premixer settings to pid and track audio inputs in MediaLIve to allow greater control over mixing audio from multiple source streams including support for AudioPidSelectors made up of multiple audio PIDs.

## __AWS Sign-In Service__
  - ### Features
    - AWS Sign-In now allows customers to control access to the AWS Management Console using resource-based policies. With this release customers can restrict console access based on network perimeters such as VPC IDs, VPC endpoints, and IP addresses.

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS Managed Daemon task definitions now support pidMode and ipcMode parameters. Set shared to allow daemons to share PID or IPC namespaces with co-located tasks on Managed Instances, enabling process tracing and shared memory communication.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for AMI Watermark which a structured identifier that helps in tracking AMI provenance

## __Amazon Lightsail__
  - ### Features
    - This release adds support for Asia Pacific (Hong Kong) (ap-east-1), Europe (Spain) (eu-south-2) and South America (Sao Paulo) (sa-east-1) Regions.

## __Amazon Prometheus Service__
  - ### Features
    - Adds supports for out-of-order sample ingestion (default 1-minute window) and a configurable rule query offset to reduce data loss and improve alerting accuracy.

## __Amazon S3__
  - ### Bugfixes
    - Fixed an issue where S3 multipart uploads with unknown content length could hang indefinitely when apiCallBufferSizeInBytes was less than twice minimumPartSizeInBytes. The SDK now validates this at request time and fails fast with a descriptive error instead of deadlocking

## __Amazon SageMaker Service__
  - ### Features
    - Add support for G6e instances (ml.g6e.xlarge through ml.g6e.48xlarge) on Amazon SageMaker Notebook Instances.

## __Connect Health__
  - ### Features
    - Add support for MedicalScribeBinaryAudioEvent in the Medical Scribe streaming input. This new event type lets you send audio as a raw binary payload instead of a base64-encoded value

# __2.46.7__ __2026-06-09__
## __AWS Outposts__
  - ### Features
    - Added AWS Outposts APIs for self-service Outposts quoting and ordering. New operations include CreateQuote, GetQuote, UpdateQuote, DeleteQuote, ListQuotes, and ListOrderableInstanceTypes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Adds support for the Amazon Bedrock account-level data retention APIs PutAccountDataRetention and GetAccountDataRetention.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Add RetryableConflictException (HTTP 409) to InvokeAgentRuntimeCommand and GetAgentCard to prevent orphaned VMs during concurrent session access. The SDK automatically retries this exception with backoff. Enforcement is not yet active and will be enabled in a future service update.

## __Amazon CloudWatch__
  - ### Features
    - This release adds the APIs (AssociateDatasetKmsKey, DisassociateDatasetKmsKey, GetDataset) to manage encryption at rest for OpenTelemetry metrics in CloudWatch using AWS KMS customer managed keys.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added TagFieldSpecifications to CreateFlowLogs and DescribeFlowLogs APIs. Customers can now specify tag keys in their Flow Logs subscriptions to capture associated EC2 resource tag values in their logs, enabling tag-based visibility.

## __Amazon SNS Message Manager__
  - ### Bugfixes
    - Fixed `SnsMessageManager` rejecting valid SNS messages whose signature timestamp falls on a whole second (zero milliseconds): the canonical string was rebuilt with `Instant#toString()`, which drops the `.000` fraction and no longer matched the value Amazon SNS signed.
        - Contributed by: [@henricook](https://github.com/henricook)

## __odb__
  - ### Features
    - Releases Autonomous Database Serverless APIs, autonomousDatabaseOciIntegrationIamRoles, linkedOciTenancyId, linkedOciCompartmentId, and subscriptionErrors fields in GetOciOnboardingStatus API response.

## __Contributors__
Special thanks to the following contributors to this release: 

[@henricook](https://github.com/henricook)
# __2.46.6__ __2026-06-08__
## __AWS Compute Optimizer__
  - ### Features
    - Adds new Idle Recommendation Resource types in the AWS Compute Optimizer API

## __AWS DevOps Agent Service__
  - ### Features
    - Add Asset APIs for managing versioned assets and asset files in AWS DevOps Agent agent spaces.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - Adds support for DASH Audio Timeline Patternization. This enables your DASH manifests to templatize the repeating patterns that emerge in audio segment timelines. This compacts the total timeline length, utilizing the repeat notation, such that manifests don't grow indefinitely long.

## __AWSDeadlineCloud__
  - ### Features
    - Added optional identityCenterRegion parameter to AssociateMember APIs to allow managing memberships for users and groups in other regions.

## __Amazon Omics__
  - ### Features
    - StartRunBatch API - Add EngineSettings

## __Application Migration Service__
  - ### Features
    - AWS Transform discovery tool now supported as network migration input source. You can now use the AWS Transform Discovery tool as a source for network migration alongside modelizeIT, enabling hybrid network migrations for environments running both VMware and non-VMware workloads.

## __CloudWatch Observability Admin Service__
  - ### Features
    - CloudWatch Observability Admin extends CentralizationRuleForOrganization APIs to support metrics, enabling centralization of metrics across accounts and Regions alongside logs.

## __Cost Optimization Hub__
  - ### Features
    - Adds new Idle Recommendation types in the Cost Optimization Hub API

## __Tax Settings__
  - ### Features
    - Adds support for additional tax information fields for Philippines, Belgium, Chile, France, Poland, and Italy in the Tax Settings API.

# __2.46.5__ __2026-06-05__
## __AWS Elemental MediaConvert__
  - ### Features
    - Adds support for configurable number of Clear Lead segments at the beginning of encrypted output. Adds support for multiple trickplay variants.

## __AWS SDK for Java v2__
  - ### Features
    - Update Netty to 4.1.135
    - Updated endpoint and partition metadata.

## __Amazon QuickSight__
  - ### Features
    - Adds support for Knowledge Base APIs and Index Capacity API

## __Amazon SageMaker Service__
  - ### Features
    - This release adds support for MLflow experiment tracking in SageMaker inference optimization. CreateAIRecommendationJob and CreateAIBenchmarkJob now accept an optional OutputConfig.MlflowConfig (MLflow App ARN, experiment, run name) to stream benchmark metrics and artifacts to your own MLflow App.

## __EMR Serverless__
  - ### Features
    - Adds support for updating max capacity and custom fields while application is started

## __Payment Cryptography Control Plane__
  - ### Features
    - Adds CloudFormation support for resource-based policies on AWS Payment Cryptography keys.

# __2.46.4__ __2026-06-04__
## __AWS Audit Manager__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Fix connection pool exhaustion in the CRT HTTP client where connections were not released after a request abort or timeout.

## __AWS CloudFormation__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __AWS Config__
  - ### Features
    - AWS Config now supports internal service-linked rules, allowing AWS service partners to deploy Config rules for customers and use the evaluation results to build enhanced features.

## __AWS Glue__
  - ### Features
    - AWS Glue Interactive Sessions now supports Apache Spark Connect, enabling remote Spark execution over gRPC with minimal client-side dependencies. Adds GetSessionEndpoint and GetDashboardUrl APIs. Modifies CreateSession now accepts SPARK CONNECT session type.

## __AWS Wickr Admin API__
  - ### Features
    - AWS Wickr now allows network administrators to configure a maximum session duration for non-SSO users in security groups, and display customizable consent popups to users at login for terms of use or compliance acknowledgements.

## __AWSKendraFrontendService__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon AppIntegrations Service__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon Appflow__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon Chime SDK Voice__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon Connect Participant Service__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon EMR__
  - ### Features
    - Added support for Spark Connect interactive sessions on Amazon EMR on EC2 with new APIs - StartSession, GetSession, GetSessionEndpoint, ListSessions, and TerminateSession. Added sessionEnabled field in RunJobFlow and DescribeCluster to enable Spark Connect endpoints on EMR clusters.

## __Amazon Elastic File System__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon GuardDuty__
  - ### Features
    - Remove unsupported RDS field for filter

## __Amazon Interactive Video Service__
  - ### Features
    - adds UpdateAdConfiguration operation to AWS IVS low-latency APIs

## __Amazon SageMaker Service__
  - ### Features
    - Adds the IncludedData parameter to DescribeModelCard and DescribeModelPackage. Set it to MetadataOnly to retrieve a model card without decrypt permission on the customer managed AWS KMS key (default AllData returns full content). Adds support for the MTRL Job resource in SageMaker Search.

## __Amazon Simple Notification Service__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon WorkDocs__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon WorkSpaces__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Tax Settings__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

# __2.46.3__ __2026-06-03__
## __ARC - Region switch__
  - ### Features
    - ARC Region Switch now supports three new execution blocks for multi-Region database workloads-Amazon Aurora Serverless scaling, Amazon Aurora Provisioned scaling, and Amazon Neptune Global Database failover.

## __AWS Compute Optimizer__
  - ### Features
    - This release lets customers extend the lookback period for Amazon EBS volume and Amazon ECS rightsizing recommendations to 32 days.

## __AWS Cost Explorer Service__
  - ### Features
    - Added support for target-coverage-based Savings Plans purchase analysis. The StartCommitmentPurchaseAnalysis API now accepts a new TARGET AVERAGE COVERAGE value for AnalysisType, as well as an optional SavingsPlansTargetCoverage field in SavingsPlansPurchaseAnalysisConfiguration

## __AWS End User Messaging Social__
  - ### Features
    - Adding support for WhatsApp flow APIs and adding AccessDeniedByMetaException for Template APIs

## __Amazon Connect Service__
  - ### Features
    - SearchContacts Connect API now supports filtering contacts by the AI Agents involved in handling them

## __Inspector2__
  - ### Features
    - Inspector support for enhanced scanning

# __2.46.2__ __2026-06-02__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Location Service Routes V2__
  - ### Features
    - Add "standardRegionalEndpoints" back to fix 'Could not connect to the endpoint URL'

# __2.46.1__ __2026-06-02__
## __AWS IoT__
  - ### Features
    - Fleet indexing documentation update

## __AWS Lambda__
  - ### Features
    - Adds configuration for tag propagation to Lambda-managed resources.

## __Amazon ElastiCache__
  - ### Features
    - Amazon ElastiCache for Valkey now supports durability. This new capability is enabled through a Multi-AZ transactional log, enabling fast recovery and restart during failures.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 now supports self-service cancellation of future-dated Capacity Reservations. A cancellation charge applies based on remaining commitment. Customers can generate a cancellation quote to review charges before confirming.

## __Amazon GuardDuty__
  - ### Features
    - Amazon GuardDuty Runtime Monitoring now supports 3 new SensitiveFileModified finding types (Persistence, PrivilegeEscalation, DefenseEvasion) that detect when security-sensitive system files are modified on EC2 instances or containers, indicating potential compromise through file tampering.

## __Amazon Keyspaces Streams__
  - ### Features
    - Added iterator description to the GetRecords API response for Amazon Keyspaces Change Data Capture (CDC) streams, enabling consumers to track their current position within the stream.

## __Amazon Location Service Routes V2__
  - ### Features
    - Added Transit and Intermodal travel modes to CalculateRoutes. Plan routes using public transit (bus, subway, train, ferry) or combine transit with driving, taxi, and rental car segments in a single multi-modal route.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker Job is a new service to help you manage various workloads related to model fine tuning, evaluation etc. Two job categories are supported today, AgentRFT for multi-turn agentic reinforcement fine tuning, and AgentRFTEvaluation for evaluating base model or trained model from AgentRFT.

## __Amazon Transcribe Service__
  - ### Features
    - Release new Language locales including am-ET, es-MX, fa-AF, ht-HT, jv-ID, km-KH, my-MM, sq-AL, ne-NP. The commit shows past locales that have already been release which include cy-gb, ga-ie, gd-gb.

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fix a resource leak in `downloadDirectory` where cancelling the download could still allow file transfers that arrive late to recreate the destination directory. Added a guard in `DownloadDirectoryHelper` to skip file downloads after the operation is cancelled.

## __Sagemaker Job Runtime Service__
  - ### Features
    - Amazon SageMaker Job Runtime is a new service for managing trajectory data during multi-turn customization jobs. It provides APIs to send inference requests to models during job execution, mark rollouts as complete, and submit reward values for training trajectories.

# __2.46.0__ __2026-06-01__
## __AWS Marketplace Agreement Service__
  - ### Features
    - Adding Entitlements in SearchAgreements Response

## __AWS SDK for Java v2__
  - ### Features
    - This update replaces the default `apache-client` runtime dependency of service clients with the new `apache5-client`. This means that service clients will now use the `Apache5HttpClient` by default if no HTTP client is explicitly configured on the service client builder.\n Notable changes:\n - Apache 5 uses different logger names than Apache 4\n - Expect: 100-Continue is disabled by default\n - TCP keep-alive socket options require `jdk.net.NetworkPermission` when SecurityManager is active

## __Amazon Cognito Identity Provider__
  - ### Features
    - Add support for multi-region replication, enabling synchronization of user data and configurations to a secondary user pool in a standby Region. Add support for customer managed keys (CMK) in AWS KMS for encrypting user pool data at rest.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Increase code coverage on dynamodb-enhanced module

## __Amazon QuickSight__
  - ### Features
    - This release adds public APIs for Amazon QuickSight Spaces, Agents, and Flows. Spaces APIs enable management of curated resource collections. Agents APIs provide lifecycle control over AI-powered agents that leverage Spaces. Flows APIs add CRUDL APIs for automated workflows.

## __Apache HTTP Client 5__
  - ### Features
    - Upgrade httpcomponents.client5 to 5.6.1


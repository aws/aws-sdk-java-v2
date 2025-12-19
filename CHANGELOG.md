 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.40.13__ __2025-12-19__
## __ARC - Region switch__
  - ### Features
    - Automatic Plan Execution Reports allow customers to maintain a concise record of their Region switch Plan executions. This enables customer SREs and leadership to have a clear view of their recovery posture based on the generated reports for their Plan executions.

## __AWS IoT__
  - ### Features
    - This release adds event-based logging feature that enables granular event logging controls for AWS IoT logs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix credential reloading in defaults when shared credential/config files are modified.
    - Gracefully handle missing file in ProfileFileSupplier.reloadWhenModified.
    - Optimize endpoint rule standard functions

## __AWS Wickr Admin API__
  - ### Features
    - AWS Wickr now provides a suite of admin APIs to allow you to programmatically manage secure communication for Wickr networks at scale. These APIs enable you to automate administrative workflows including user lifecycle management, network configuration, and security group administration.

## __Amazon CloudFront__
  - ### Features
    - Add support for ECDSA signed URLs.

## __Amazon Connect Service__
  - ### Features
    - Adding support for Custom Metrics and Pre-Defined Attributes to GetCurrentMetricData API.

## __Amazon WorkSpaces Web__
  - ### Features
    - Add support for WebAuthn under user settings.

## __EMR Serverless__
  - ### Features
    - Added JobLevelCostAllocationConfiguration field to enable cost allocation reporting at the job level, providing more granular visibility into EMR Serverless charges

## __QBusiness__
  - ### Features
    - It is a internal bug fix for region expansion

# __2.40.12__ __2025-12-18__
## __ARC - Region switch__
  - ### Features
    - New API to list Route 53 health checks created by ARC region switch for a plan in a specific AWS Region using the Region switch Regional data plane.

## __AWS Artifact__
  - ### Features
    - Add support for ListReportVersions API for the calling AWS account.

## __AWS Clean Rooms Service__
  - ### Features
    - Adding support for collaboration change requests requiring an approval workflow. Adding support for change requests that grant or revoke results receiver ability and modifying auto approved change types in an existing collaboration.

## __AWS IoT__
  - ### Features
    - This release adds message batching for the IoT Rules Engine HTTP action.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Systems Manager for SAP__
  - ### Features
    - Added "Stopping" for the HANA Database Status.

## __Amazon AppStream__
  - ### Features
    - Added support for new operating systems (1) Ubuntu 24.04 Pro LTS on Elastic fleets, and (2) Microsoft Server 2025 on Always-On and On-Demand fleets

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Feature to support header exchanges between Bedrock AgentCore Gateway Targets and client, along with propagating query parameter to the configured targets.

## __Amazon EC2 Container Service__
  - ### Features
    - Adding support for Event Windows via a new ECS account setting "fargateEventWindows". When enabled, ECS Fargate will use the configured event window for patching tasks. Introducing "CapacityOptionType" for CreateCapacityProvider API, allowing support for Spot capacity for ECS Managed Instances.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds AvailabilityZoneId support for CreateFleet, ModifyFleet, DescribeFleets, RequestSpotFleet, ModifySpotFleetRequests and DescribeSpotFleetRequests APIs.

## __Amazon Elastic Container Registry__
  - ### Features
    - Adds support for ECR Create On Push

## __Amazon OpenSearch Service__
  - ### Features
    - Amazon OpenSearch Service adds support for warm nodes, enabling new multi-tier architecture.

## __Amazon Simple Email Service__
  - ### Features
    - Amazon SES introduces Email Validation feature which checks email addresses for syntax errors, domain validity, and risky addresses to help maintain deliverability and protect sender reputation. SES also adds resource tagging and ABAC support for EmailTemplates and CustomVerificationEmailTemplates.

## __Data Automation for Amazon Bedrock__
  - ### Features
    - Blueprint Optimization (BPO) is a new Amazon Bedrock Data Automation (BDA) capability that improves blueprint inference accuracy using example content assets and ground truth data. BPO works by generating better instructions for fields in the Blueprint using provided data.

# __2.40.11__ __2025-12-17__
## __AWS Elemental MediaConvert__
  - ### Features
    - Adds support for tile encoding in HEVC and audio for video overlays.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for SPEKE V2 content key encryption in MediaPackage v2 Origin Endpoints.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SDK for Java v2 Migration Tool__
  - ### Bugfixes
    - Add support for S3Object getObjectMetadata transform

## __Amazon CloudFront__
  - ### Features
    - Add support for ECDSA signed URLs.

## __Amazon GameLift Streams__
  - ### Features
    - Added new stream group operation parameters for scale-on-demand capacity with automatic prewarming. Added new Gen6 stream classes based on the EC2 G6 instance family. Added new StartStreamSession parameter for exposure of real-time performance stats to clients.

## __Amazon GuardDuty__
  - ### Features
    - Add support for dbiResourceId in finding.

## __Amazon SageMaker Service__
  - ### Features
    - Adding the newly launched p6-b300.48xlarge ec2 instance support in Sagemaker(Hyperpod,Training and Sceptor)

## __Inspector Scan__
  - ### Features
    - Adds an additional OutputFormat

## __Managed Streaming for Kafka Connect__
  - ### Features
    - Support dual-stack network connectivity for connectors via NetworkType field.

## __Payment Cryptography Control Plane__
  - ### Features
    - Support for AS2805 standard. Modifications to import-key and export-key to support AS2805 variants.

## __Payment Cryptography Data Plane__
  - ### Features
    - Support for AS2805 standard. New API GenerateAs2805KekValidation and changes to translate pin, GenerateMac and VerifyMac to support AS2805 key variants.

# __2.40.10__ __2025-12-16__
## __AWS IoT__
  - ### Features
    - Add support for dynamic payloads in IoT Device Management Commands

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Timestream InfluxDB__
  - ### Features
    - This release adds support for rebooting InfluxDB DbInstances and DbClusters

# __2.40.9__ __2025-12-15__
## __AWS EntityResolution__
  - ### Features
    - Support Customer Profiles Integration for AWS Entity Resolution

## __AWS Health APIs and Notifications__
  - ### Features
    - Updating Health API endpoint generation for dualstack only regions

## __AWS MediaTailor__
  - ### Features
    - Added support for Ad Decision Server Configuration enabling HTTP POST requests with custom bodies, headers, GZIP compression, and dynamic variables. No changes required for existing GET request configurations.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - This release updates broken links for AgentCore Policy APIs in the AWS CLI and SDK resources.

## __Amazon CloudWatch Logs__
  - ### Features
    - This release allows you to import your historical CloudTrail Lake data into CloudWatch with a few steps, enabling you to easily consolidate operational, security, and compliance data in one place.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect now supports outbound WhatsApp contacts via the Send message block or StartOutboundChatContact API. Send proactive messages for surveys, reminders, and updates. Offer customers the option to switch to WhatsApp while in queue, eliminating hold time.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - EC2 Capacity Manager now supports SpotTotalCount, SpotTotalInterruptions and SpotInterruptionRate metrics for both vCPU and instance units.

## __Amazon Glacier__
  - ### Features
    - Documentation updates for Amazon Glacier's maintenance mode

## __Amazon Route 53 Resolver__
  - ### Features
    - Adds support for enabling detailed metrics on Route 53 Resolver endpoints using RniEnhancedMetricsEnabled and TargetNameServerMetricsEnabled in the CreateResolverEndpoint and UpdateResolverEndpoint APIs, providing enhanced visibility into Resolver endpoint and target name server performance.

## __Amazon Simple Storage Service__
  - ### Features
    - This release adds support for the new optional field 'LifecycleExpirationDate' in S3 Inventory configurations.

## __Service Quotas__
  - ### Features
    - Add support for SQ Dashboard Api

# __2.40.8__ __2025-12-12__
## __AWS Billing and Cost Management Recommended Actions__
  - ### Features
    - Added new freetier action types to RecommendedAction.type.

## __AWS DataSync__
  - ### Features
    - Adds Enhanced mode support for NFS and SMB locations. SMB credentials are now managed via Secrets Manager, and may be encrypted with service or customer managed keys. Increases AgentArns maximum count to 8 (max 4 per TaskMode). Adds folder counters to DescribeTaskExecution for Enhanced mode tasks.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect now offers automated post-chat surveys triggered when customers end conversations. This captures timely feedback while experience is fresh, using either a no-code form builder or Amazon Lex-powered interactive surveys.

## __Amazon WorkSpaces Web__
  - ### Features
    - Adds support for portal branding customization, enabling administrators to personalize end-user portals with custom assets.

# __2.40.7__ __2025-12-11__
## __AWS Lambda__
  - ### Features
    - Add Dotnet 10 (dotnet10) support to AWS Lambda.

## __AWS Organizations__
  - ### Features
    - Add support for policy operations on the NETWORK SECURITY DIRECTOR POLICY policy type.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Secrets Manager__
  - ### Features
    - Add SortBy parameter to ListSecrets

## __Amazon QuickSight__
  - ### Features
    - This release adds new GetIdentityContext API, Dashboard customization options for tables and pivot tables, Visual styling options- borders and decals, map GeocodingPreferences, KeyPairCredentials for DataSourceCredentials. Snapshot APIs now support registered users. Parameters limit increased to 400

## __Amazon Simple Email Service__
  - ### Features
    - Update GetEmailIdentity and CreateEmailIdentity response to include SigningHostedZone in DkimAttributes. Updated PutEmailIdentityDkimSigningAttributes Response to include SigningHostedZone.

# __2.40.6__ __2025-12-10__
## __AWS Signer__
  - ### Features
    - Adds support for Signer GetRevocationStatus with updated endpoints

## __AWSBillingConductor__
  - ### Features
    - Launch itemized custom line item and service line item filter

## __Amazon Bedrock__
  - ### Features
    - Automated Reasoning checks in Amazon Bedrock Guardrails is capable of generating policy scenarios to validate policies. The GetAutomatedReasoningPolicyBuildWorkflowResultAssets API now adds POLICY SCENARIO asset type, allowing customers to retrieve scenarios generated by the build workflow.

## __Amazon CloudWatch__
  - ### Features
    - This release introduces two additional protocols AWS JSON 1.1 and Smithy RPC v2 CBOR, replacing the currently utilized one, AWSQuery. AWS SDKs will prioritize the protocol that is the most performant for each language.

## __Amazon OpenSearch Service__
  - ### Features
    - The CreateApplication API now supports an optional kms key arn parameter to allow customers to specify a CMK for application encryption.

## __Partner Central Selling API__
  - ### Features
    - Adds support for the new Project.AwsPartition field on Opportunity and AWS Opportunity Summary. Use this field to specify the AWS partition where the opportunity will be deployed.

## __odb__
  - ### Features
    - The following APIs now return CloudExadataInfrastructureArn and OdbNetworkArn fields for improved resource identification and AWS service integration - GetCloudVmCluster, ListCloudVmClusters, GetCloudAutonomousVmCluster, and ListCloudAutonomousVmClusters.

# __2.40.5__ __2025-12-09__
## __AWS Account__
  - ### Features
    - This release adds a new API (GetGovCloudAccountInformation) used to retrieve information about a linked GovCloud account from the standard AWS partition.

## __AWS AppSync__
  - ### Features
    - Update Event API to require EventConfig parameter in creation and update requests.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon GuardDuty__
  - ### Features
    - Adding support for Ec2LaunchTemplate Version field

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - Token Exchange introduces seamless token exchange capabilities for IVS RTX, enabling customers to upgrade or downgrade token capabilities and update token attributes within the IVS client SDK without forcing clients to disconnect and reconnect.

## __Amazon Route 53__
  - ### Features
    - Amazon Route 53 now supports the EU (Germany) Region (eusc-de-east-1) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region

## __Application Migration Service__
  - ### Features
    - Added parameters encryption, IPv4/IPv6 protocol configuration, and enhanced tagging support for replication operations.

# __2.40.4__ __2025-12-08__
## __AWS Cost Explorer Service__
  - ### Features
    - Add support for Cost Category resource associations including filtering by resource type on ListCostCategoryDefinitions and new ListCostCategoryResourceAssociations API.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - ApplyUserAgentStage will not overwrite the custom User-Agent

## __AWS SSO Identity Store__
  - ### Features
    - Updating AWS Identity Store APIs to support Attribute Extensions capability, with the first release adding Enterprise Attributes. This launch aligns Identity Store APIs with SCIM for enterprise attributes, reducing cases when customers are forced to use SCIM due to lack of SigV4 API support.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 P6-B300 instances provide 8x NVIDIA Blackwell Ultra GPUs with 2.1 TB high bandwidth GPU memory, 6.4 Tbps EFA networking, 300 Gbps dedicated ENA throughput, and 4 TB of system memory. Amazon EC2 C8a instances are powered by 5th Gen AMD EPYC processors with a maximum frequency of 4.5 GHz.

## __Amazon Relational Database Service__
  - ### Features
    - Adding support for tagging RDS Instance/Cluster Automated Backups

## __Amazon Simple Email Service__
  - ### Features
    - Update Mail Manager Archive ARN validation

## __IAM Roles Anywhere__
  - ### Features
    - Increases certificate string length for trust anchor source data to support ML-DSA certificates.

## __Partner Central Selling API__
  - ### Features
    - Deal Sizing Service for AI-based deal size estimation with AWS service-level breakdown, supporting Expansion and Migration deals across Technology, and Reseller partner cohorts, including Pricing Calculator AddOn for MAP deals and funding incentives.

## __Redshift Serverless__
  - ### Features
    - Added GetIdentityCenterAuthToken API to retrieve encrypted authentication tokens for Identity Center integrated serverless workgroups. This API enables programmatic access to secure Identity Center tokens with proper error handling and parameter validation across supported SDK languages.

# __2.40.3__ __2025-12-05__
## __AWS Identity and Access Management__
  - ### Features
    - Adding the ExpirationTime attribute to the delegation request resource.

## __AWS SDK for Java v2 Codegen__
  - ### Features
    - Automatically enable `AwsV4HttpSigner.CHUNK_ENCODING_ENABLED` signer property for input streaming operations that support checksums in generated auth scheme provider class

## __Amazon EC2 Container Service__
  - ### Features
    - Updating stop-task API to encapsulate containers with custom stop signal

## __Amazon Simple Email Service__
  - ### Features
    - Updating the desired url for `PutEmailIdentityDkimSigningAttributes` from v1 to v2

## __Inspector2__
  - ### Features
    - This release adds a new ScanStatus called "Unsupported Code Artifacts". This ScanStatus will be returned when a Lambda function was not code scanned because it has unsupported code artifacts.

## __Partner Central Account API__
  - ### Features
    - Adding Verification API's to Partner Central Account SDK.

# __2.40.2__ __2025-12-04__
## __AWS Lambda__
  - ### Features
    - Add DisallowedByVpcEncryptionControl to the LastUpdateStatusReasonCode and StateReasonCode enums to represent failures caused by VPC Encryption Controls.

## __Apache 5 HTTP Client (Preview)__
  - ### Bugfixes
    - Ignore negative values set `connectionTimeToLive`. There is no behavior change on the client as negative values have no meaning for Apache 5.

# __2.40.1__ __2025-12-03__
## __Amazon Bedrock__
  - ### Features
    - Adding support in Amazon Bedrock to customize models with reinforcement fine-tuning (RFT) and support for updating the existing Custom Model Deployments.

## __Amazon S3__
  - ### Bugfixes
    - Fix NPE issue thrown when using multipart S3 client to upload an object containing empty content without supplying a content length. See [#6464](https://github.com/aws/aws-sdk-java-v2/issues/6464)

## __Amazon SageMaker Service__
  - ### Features
    - Introduces Serverless training: A fully managed compute infrastructure that abstracts away all infrastructure complexity, allowing you to focus purely on model development. Added AI model customization assets used to train, refine, and evaluate custom models during the model customization process.

# __2.40.0__ __2025-12-02__
## __AWS Cost Explorer Service__
  - ### Features
    - This release updates existing Savings Plans Purchase Analyzer and Recommendations APIs to support Database Savings Plans.

## __AWS Lambda__
  - ### Features
    - Launching Lambda durable functions - a new feature to build reliable multi-step applications and AI workflows natively within the Lambda developer experience.

## __AWS S3 Control__
  - ### Features
    - Add support for S3 Storage Lens Advanced Performance Metrics, Expanded Prefixes metrics report, and export to S3 Tables.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Savings Plans__
  - ### Features
    - Added support for Amazon Database Savings Plans

## __AWS SecurityHub__
  - ### Features
    - ITSM enhancements: DRYRUN mode for testing ticket creation, ServiceNow now uses AWS Secrets Manager for credentials, ConnectorRegistrationsV2 renamed to RegisterConnectorV2, added ServiceQuotaExceededException error, and ConnectorStatus visibility in CreateConnectorV2.

## __Amazon Bedrock__
  - ### Features
    - Adds the audioDataDeliveryEnabled boolean field to the Model Invocation Logging Configuration.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Support for AgentCore Evaluations and Episodic memory strategy for AgentCore Memory.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Supports AgentCore Evaluations, Policy, Episodic Memory Strategy, Resource Based Policy for Runtime and Gateway APIs, API Gateway Rest API Targets and enhances JWT authorizer.

## __Amazon Bedrock Runtime__
  - ### Features
    - Adds support for Audio Blocks and Streaming Image Output plus new Stop Reasons of malformed_model_output and malformed_tool_use.

## __Amazon CloudWatch Logs__
  - ### Features
    - CloudWatch Logs adds managed S3 Tables integration to access logs using other analytical tools, as well as facets and field indexing to simplify log analytics in CloudWatch Logs Insights.

## __Amazon DataZone__
  - ### Features
    - Amazon DataZone now supports exporting Catalog datasets as Amazon S3 tables, and provides automatic business glossary term suggestions for data assets.

## __Amazon FSx__
  - ### Features
    - S3 Access Points support for FSx for NetApp ONTAP

## __Amazon GuardDuty__
  - ### Features
    - Adding support for extended threat detection for Amazon EC2 and Amazon ECS. Adding support for wild card suppression rules.

## __Amazon OpenSearch Service__
  - ### Features
    - GPU-acceleration helps you build large-scale vector databases faster and more efficiently. You can enable this feature on new OpenSearch domains and OpenSearch Serverless collections. This feature uses GPU-acceleration to reduce the time needed to index data into vector indexes.

## __Amazon Relational Database Service__
  - ### Features
    - RDS Oracle and SQL Server: Add support for adding, modifying, and removing additional storage volumes, offering up to 256TiB storage; RDS SQL Server: Support Developer Edition via custom engine versions for development and testing purposes; M7i/R7i instances with Optimize CPU for cost savings.

## __Amazon S3 Tables__
  - ### Features
    - Add storage class, replication, and table record expiration features to S3 Tables.

## __Amazon S3 Vectors__
  - ### Features
    - Amazon S3 Vectors provides cost-effective, elastic, and durable vector storage for queries based on semantic meaning and similarity.

## __Amazon SageMaker Service__
  - ### Features
    - Added support for serverless MLflow Apps. Added support for new HubContentTypes (DataSet and JsonDoc) in Private Hub for AI model customization assets, enabling tracking and management of training datasets and evaluators (reward functions/prompts) throughout the ML lifecycle.

## __Amazon Simple Storage Service__
  - ### Features
    - New S3 Storage Class FSX_ONTAP

## __CloudWatch Observability Admin Service__
  - ### Features
    - CloudWatch Observability Admin adds pipelines configuration for third party log ingestion and transformation of all logs ingested, integration of CloudWatch logs with S3 Tables, and AWS account or organization level enablement for 7 AWS services.

## __Nova Act Service__
  - ### Features
    - Initial release of Nova Act SDK. The Nova Act service enables customers to build and manage fleets of agents for automating production UI workflows with high reliability, fastest time-to-value, and ease of implementation at scale.

## __OpenSearch Service Serverless__
  - ### Features
    - GPU-acceleration helps you build large-scale vector databases faster and more efficiently. You can enable this feature on new OpenSearch domains and OpenSearch Serverless collections. This feature uses GPU-acceleration to reduce the time needed to index data into vector indexes.

## __S3__
  - ### Features
    - Add support for parallel download for individual part-get for multipart GetObject in s3 async client and Transfer Manager


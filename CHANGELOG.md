 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.31.67__ __2025-06-19__
## __AWS Lambda__
  - ### Features
    - Support Schema Registry feature for Kafka Event Source Mapping. Customers can now configure a Schema Registry to enable schema validation and filtering for Avro, Protobuf, and JSON-formatted events in Lambda for Kafka Event Source.

## __Amazon Bedrock__
  - ### Features
    - This release of the SDK has the API and documentation for the createcustommodel API. This feature lets you copy a trained model into Amazon Bedrock for inference.

## __Amazon SageMaker Service__
  - ### Features
    - This release introduces alternative support for utilizing CFN templates from S3 for SageMaker Projects.

## __EMR Serverless__
  - ### Features
    - This release adds the capability to enable IAM IdentityCenter Trusted Identity Propagation for users running Interactive Sessions on EMR Serverless Applications.

## __Payment Cryptography Control Plane__
  - ### Features
    - Additional support for managing HMAC keys that adheres to changes documented in X9.143-2021 and provides better interoperability for key import/export

## __Payment Cryptography Data Plane__
  - ### Features
    - Additional support for managing HMAC keys that adheres to changes documented in X9.143-2021 and provides better interoperability for key import/export

# __2.31.66__ __2025-06-18__
## __AWS AI Ops__
  - ### Features
    - This is the initial SDK release for Amazon AI Operations (AIOps). AIOps is a generative AI-powered assistant that helps you respond to incidents in your system by scanning your system's telemetry and quickly surface suggestions that might be related to your issue.

## __AWS S3__
  - ### Features
    - Adds the ability to presign HeadObject and HeadBucket requests with the S3 Presigner
        - Contributed by: [@tmccombs](https://github.com/tmccombs)

## __AWS SDK for Java v2__
  - ### Features
    - Adding a new method of constructing ARNs without exceptions as control flow
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Logs__
  - ### Features
    - Added CloudWatch Logs Transformer support for converting CloudTrail, VPC Flow, EKS Audit, AWS WAF and Route53 Resolver logs to OCSF v1.1 format.

## __Amazon SageMaker Service__
  - ### Features
    - Add support for p6-b200 instance type for SageMaker Hyperpod

## __Amazon Simple Storage Service__
  - ### Features
    - Added support for renaming objects within the same bucket using the new RenameObject API.

## __Auto Scaling__
  - ### Features
    - Add IncludeInstances parameter to DescribeAutoScalingGroups API

## __Contributors__
Special thanks to the following contributors to this release: 

[@tmccombs](https://github.com/tmccombs)
# __2.31.65__ __2025-06-17__
## __AWS Backup__
  - ### Features
    - AWS Backup is adding support for integration of its logically air-gapped vaults with the AWS Organizations Multi-party approval capability.

## __AWS Certificate Manager__
  - ### Features
    - Adds support for Exportable Public Certificates

## __AWS Database Migration Service__
  - ### Features
    - Add "Virtual" field to Data Provider as well as "S3Path" and "S3AccessRoleArn" fields to DataProvider settings

## __AWS Multi-party Approval__
  - ### Features
    - This release enables customers to create Multi-party approval teams and approval requests to protect supported operations.

## __AWS Network Firewall__
  - ### Features
    - Release of Active Threat Defense in Network Firewall

## __AWS Organizations__
  - ### Features
    - Add support for policy operations on the SECURITYHUB_POLICY policy type.

## __AWS SDK for Java V2__
  - ### Bugfixes
    - Fix a bug in ConstructorCache when classes are GC'ed but not removed from cache

## __AWS Security Token Service__
  - ### Features
    - The AWS Security Token Service APIs AssumeRoleWithSAML and AssumeRoleWithWebIdentity can now be invoked without pre-configured AWS credentials in the SDK configuration.

## __AWS SecurityHub__
  - ### Features
    - Adds operations, structures, and exceptions required for public preview release of Security Hub V2.

## __AWS WAFV2__
  - ### Features
    - AWS WAF can now suggest protection packs for you based on the application information you provide when you create a webACL.

## __Access Analyzer__
  - ### Features
    - We are launching a new analyzer type, internal access analyzer. The new analyzer will generate internal access findings, which help customers understand who within their AWS organization or AWS Account has access to their critical AWS resources.

## __Amazon Bedrock__
  - ### Features
    - This release of the SDK has the API and documentation for the createcustommodel API. This feature lets you copy a trained model into Amazon Bedrock for inference.

## __Amazon GuardDuty__
  - ### Features
    - Adding support for extended threat detection for EKS Audit Logs and EKS Runtime Monitoring.

## __Inspector2__
  - ### Features
    - Add Code Repository Scanning as part of AWS InspectorV2

# __2.31.64__ __2025-06-16__
## __AWS Network Firewall__
  - ### Features
    - You can now create firewalls using a Transit Gateway instead of a VPC, resulting in a TGW attachment.

## __AWS SDK for Java v2__
  - ### Features
    - Add tracking of RequestBody/ResponseTransfromer implementations used in UserAgent.

## __Amazon Bedrock__
  - ### Features
    - This release of the SDK has the API and documentation for the createcustommodel API. This feature lets you copy a Amazon SageMaker trained Amazon Nova model into Amazon Bedrock for inference.

## __Amazon Elastic Container Registry__
  - ### Features
    - The `DescribeImageScanning` API now includes `lastInUseAt` and `InUseCount` fields that can be used to prioritize vulnerability remediation for images that are actively being used.

## __Amazon SageMaker Service__
  - ### Features
    - This release 1) adds a new S3DataType Converse for SageMaker training 2)adds C8g R7gd M8g C6in P6 P6e instance type for SageMaker endpoint 3) adds m7i, r7i, c7i instance type for SageMaker Training and Processing.

# __2.31.63__ __2025-06-12__
## __AWS IoT FleetWise__
  - ### Features
    - Add new status READY_FOR_CHECKIN used for vehicle synchronisation

## __AWS Key Management Service__
  - ### Features
    - AWS KMS announces the support of ML-DSA key pairs that creates post-quantum safe digital signatures.

## __AWS Parallel Computing Service__
  - ### Features
    - Fixed regex patterns for ARN fields.

## __Amazon API Gateway__
  - ### Features
    - Documentation updates for Amazon API Gateway

## __Amazon EC2 Container Service__
  - ### Features
    - This Amazon ECS release supports updating the capacityProviderStrategy parameter in update-service.

## __AmazonApiGatewayV2__
  - ### Features
    - Documentation updates for Amazon API Gateway

## __AmazonConnectCampaignServiceV2__
  - ### Features
    - Added PutInstanceCommunicationLimits and GetInstanceCommunicationLimits APIs

## __EMR Serverless__
  - ### Features
    - This release adds support for retrieval of the optional executionIamPolicy field in the GetJobRun API response.

# __2.31.62__ __2025-06-11__
## __AWS Control Catalog__
  - ### Features
    - Introduced ListControlMappings API that retrieves control mappings. Added control aliases and governed resources fields in GetControl and ListControls APIs. New filtering capability in ListControls API, with implementation identifiers and implementation types.

## __AWS Network Manager__
  - ### Features
    - Add support for public DNS hostname resolution to private IP addresses across Cloud WAN-managed VPCs. Add support for security group referencing across Cloud WAN-managed VPCs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Deprecated DefaultCredentialsProvider.create() since it creates Singleton instance
        - Contributed by: [@jencymaryjoseph](https://github.com/jencymaryjoseph)

## __AWS WAFV2__
  - ### Features
    - WAF now provides two DDoS protection options: resource-level monitoring for Application Load Balancers and the AWSManagedRulesAntiDDoSRuleSet managed rule group for CloudFront distributions.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Release for EKS Pod Identity Cross Account feature and disableSessionTags flag.

## __Amazon Lex Model Building V2__
  - ### Features
    - Add support for the Assisted NLU feature to improve bot performance

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation for Amazon RDS for Db2 cross-Region replicas in standby mode.

## __Contributors__
Special thanks to the following contributors to this release: 

[@jencymaryjoseph](https://github.com/jencymaryjoseph)
# __2.31.61__ __2025-06-10__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon GameLift Streams__
  - ### Features
    - Documentation updates for Amazon GameLift Streams to address formatting errors, correct resource ID examples, and update links to other guides

# __2.31.60__ __2025-06-09__
## __AWS AppSync__
  - ### Features
    - Deprecate `atRestEncryptionEnabled` and `transitEncryptionEnabled` attributes in `CreateApiCache` action. Encryption is always enabled for new caches.

## __AWS Cost Explorer Service__
  - ### Features
    - Support dual-stack endpoints for ce api

## __AWS Marketplace Catalog Service__
  - ### Features
    - The ListEntities API now supports the EntityID, LastModifiedDate, ProductTitle, and Visibility filters for machine learning products. You can also sort using all of those filters.

## __AWS SDK for Java v2__
  - ### Features
    - Adds support for configuring bearer auth using a token sourced from the environment for services with the `enableEnvironmentBearerToken` customization flag.
    - Updated Region class generation to use Partitions.json instead of the Endpoints.json and removed the hardcoded global regions.
    - Updated endpoint and partition metadata.

## __Amazon Connect Customer Profiles__
  - ### Features
    - This release introduces capability of Profile Explorer, using correct ingestion timestamp & using historical data for computing calculated attributes, and new standard objects for T&H as part of Amazon Connect Customer Profiles service.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Release to support Elastic VMware Service (Amazon EVS) Subnet and Amazon EVS Network Interface Types.

## __Amazon Elastic File System__
  - ### Features
    - Added support for Internet Protocol Version 6 (IPv6) on EFS Service APIs and mount targets.

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Add ConflictException to UpdateEnvironment API

# __2.31.59__ __2025-06-06__
## __AWS Key Management Service__
  - ### Features
    - Remove unpopulated KeyMaterialId from Encrypt Response

## __AWS SDK for Java v2__
  - ### Features
    - Add support for protocols field in service model
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix expiration in past warning during profile credential loading.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This release introduces the `PromptCreationConfigurations` input parameter, which includes fields to control prompt population for `InvokeAgent` or `InvokeInlineAgent` requests.

## __Amazon Rekognition__
  - ### Features
    - Adds support for defining an ordered preference list of different Rekognition Face Liveness challenge types when calling CreateFaceLivenessSession.

## __Amazon Relational Database Service__
  - ### Features
    - Include Global Cluster Identifier in DBCluster if the DBCluster is a Global Cluster Member.

## __Amazon Route 53__
  - ### Features
    - Amazon Route 53 now supports the Asia Pacific (Taipei) Region (ap-east-2) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region.

## __Amazon S3 Tables__
  - ### Features
    - S3 Tables now supports getting details about a table via its table ARN.

# __2.31.58__ __2025-06-05__
## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Updating the minimum for List APIs to be 1 (instead of 0)

## __AWS CloudFormation__
  - ### Features
    - Add new warning type 'EXCLUDED_PROPERTIES'

## __AWS Key Management Service__
  - ### Features
    - AWS KMS announces the support for on-demand rotation of symmetric-encryption KMS keys with imported key material (EXTERNAL origin).

## __AWS SDK for Java v2__
  - ### Features
    - Added ability to configure preferred authentication schemes when multiple auth options are available.
    - Updated endpoint and partition metadata.

## __AWS WAFV2__
  - ### Features
    - AWS WAF adds support for ASN-based traffic filtering and support for ASN-based rate limiting.

## __Amazon DynamoDB Enhanced Client__
  - ### Bugfixes
    - Fixed DynamoDbEnhancedClient DefaultDynamoDbAsyncTable::createTable() to create secondary indices that are defined on annotations of the POJO class, similar to DefaultDynamoDbTable::createTable().

# __2.31.57__ __2025-06-04__
## __AWS Amplify__
  - ### Features
    - Update documentation for cacheConfig in CreateApp API

## __AWS Elemental MediaConvert__
  - ### Features
    - This release includes support for embedding and signing C2PA content credentials in MP4 outputs.

## __AWS Invoicing__
  - ### Features
    - Added new Invoicing ListInvoiceSummaries API Operation

## __AWS MediaConnect__
  - ### Features
    - This release updates the DescribeFlow API to show peer IP addresses. You can now identify the peer IP addresses of devices connected to your sources and outputs. This helps you to verify and troubleshoot your flow's active connections.

## __AWS Network Firewall__
  - ### Features
    - You can now monitor flow and alert log metrics from the Network Firewall console.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic VMware Service__
  - ### Features
    - Amazon Elastic VMware Service (Amazon EVS) allows you to run VMware Cloud Foundation (VCF) directly within your Amazon VPC including simplified self-managed migration experience with guided workflow in AWS console or via AWS CLI, get full access to their VCF deployment and VCF license portability.

## __Amazon SageMaker Service__
  - ### Features
    - Added support for p6-b200 instance type in SageMaker Training Jobs and Training Plans.

## __Amazon Transcribe Service__
  - ### Features
    - AWS Healthscribe now supports new templates for the clinical note summary: BIRP, SIRP, DAP, BEHAVIORAL_SOAP, and PHYSICAL_SOAP

## __Amazon Transcribe Streaming Service__
  - ### Features
    - AWS Healthscribe now supports new templates for the clinical note summary: BIRP, SIRP, DAP, BEHAVIORAL_SOAP, and PHYSICAL_SOAP

## __S3 Transfer Manager__
  - ### Bugfixes
    - DownloadFilter type incompatability methods overriden from extended interface
        - Contributed by: [@jencymaryjoseph](https://github.com/jencymaryjoseph)

## __Contributors__
Special thanks to the following contributors to this release: 

[@jencymaryjoseph](https://github.com/jencymaryjoseph)
# __2.31.56__ __2025-06-03__
## __AWS S3 Event Notifications__
  - ### Bugfixes
    - Fixed parsing of S3 event notifications to allow eventTime to be null when eventName is not
        - Contributed by: [@reifiedbeans](https://github.com/reifiedbeans)

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix NPE in `ProfileFileSupplier.defaultSupplier` when both credentials and config files do not exist.

## __Amazon API Gateway__
  - ### Features
    - Adds support to set the routing mode for a custom domain name.

## __AmazonApiGatewayV2__
  - ### Features
    - Adds support to create routing rules and set the routing mode for a custom domain name.

## __EMR Serverless__
  - ### Features
    - AWS EMR Serverless: Adds a new option in the CancelJobRun API in EMR 7.9.0+, to cancel a job with grace period. This feature is enabled by default with a 120-second grace period for streaming jobs and is not enabled by default for batch jobs.

## __Contributors__
Special thanks to the following contributors to this release: 

[@reifiedbeans](https://github.com/reifiedbeans)
# __2.31.55__ __2025-06-02__
## __AWS Backup__
  - ### Features
    - You can now subscribe to Amazon SNS notifications and Amazon EventBridge events for backup indexing. You can now receive notifications when a backup index is created, deleted, or fails to create, enhancing your ability to monitor and track your backup operations.

## __AWS Compute Optimizer__
  - ### Features
    - This release enables AWS Compute Optimizer to analyze Amazon Aurora database clusters and generate Aurora I/O-Optimized recommendations.

## __AWS EntityResolution__
  - ### Features
    - Add support for generating match IDs in near real-time.

## __AWS Parallel Computing Service__
  - ### Features
    - Introduces SUSPENDING and SUSPENDED states for clusters, compute node groups, and queues.

## __AWS SDK for Java v2__
  - ### Features
    - Improve the endpoint rules performance by directly passing the needed params instead of using a POJO to keep track of them.
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - This release adds the Agent Lifecycle Paused State feature to Amazon Bedrock agents. By using an agent's alias, you can temporarily suspend agent operations during maintenance, updates, or other situations.

## __Amazon Athena__
  - ### Features
    - Add support for the managed query result in the workgroup APIs. The managed query result configuration enables users to store query results to Athena owned storage.

## __Amazon EC2 Container Service__
  - ### Features
    - Updates Amazon ECS documentation to include note for upcoming default log driver mode change.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Add support for filtering ListInsights API calls on MISCONFIGURATION insight category

## __Cost Optimization Hub__
  - ### Features
    - Support recommendations for Aurora instance and Aurora cluster storage.

## __Synthetics__
  - ### Features
    - Support for Java runtime handler pattern.

# __2.31.54__ __2025-05-30__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon SageMaker Service__
  - ### Features
    - Release new parameter CapacityReservationConfig in ProductionVariant

## __EMR Serverless__
  - ### Features
    - This release adds the capability for users to specify an optional Execution IAM policy in the StartJobRun action. The resulting permissions assumed by the job run is the intersection of the permissions in the Execution Role and the specified Execution IAM Policy.

# __2.31.53__ __2025-05-29__
## __AWS Amplify__
  - ### Features
    - Add support for customizable build instance sizes. CreateApp and UpdateApp operations now accept a new JobConfig parameter composed of BuildComputeType.

## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Add AFTER_DISCOUNTS_AND_COMMITMENTS to Workload Estimate Rate Type. Set ListWorkLoadEstimateUsage maxResults range to minimum of 0 and maximum of 300.

## __AWS CloudTrail__
  - ### Features
    - CloudTrail Feature Release: Support for Enriched Events with Configurable Context for Event Data Store

## __AWS Data Exchange__
  - ### Features
    - This release adds Tag support for Event Action resource, through which customers can create event actions with Tags and retrieve event actions with Tags.

## __AWS DataSync__
  - ### Features
    - AgentArns field is made optional for Object Storage and Azure Blob location create requests. Location credentials are now managed via Secrets Manager, and may be encrypted with service managed or customer managed keys. Authentication is now optional for Azure Blob locations.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect Service Feature: Email Recipient Limit Increase

## __Amazon FSx__
  - ### Features
    - FSx API changes to support the public launch of new Intelligent Tiering storage class on Amazon FSx for Lustre

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - IVS Real-Time now offers customers the participant replication that allow customers to copy a participant from one stage to another.

## __Amazon SageMaker Service__
  - ### Features
    - Add maintenance status field to DescribeMlflowTrackingServer API response

## __Amazon Simple Storage Service__
  - ### Features
    - Adding checksum support for S3 PutBucketOwnershipControls API.

## __AmazonMWAA__
  - ### Features
    - Amazon MWAA now lets you choose a worker replacement strategy when updating an environment. This release adds two worker replacement strategies: FORCED (default), which stops workers immediately, and GRACEFUL, which allows workers to finish current tasks before shutting down.

## __Auto Scaling__
  - ### Features
    - Add support for "apple" CpuManufacturer in ABIS

# __2.31.52__ __2025-05-28__
## __AWS Network Firewall__
  - ### Features
    - You can now use VPC endpoint associations to create multiple firewall endpoints for a single firewall.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Enable the option to automatically delete underlying Amazon EBS snapshots when deregistering Amazon Machine Images (AMIs)

## __Amazon EventBridge__
  - ### Features
    - Allow for more than 2 characters for location codes in EventBridge ARNs

## __Cost Optimization Hub__
  - ### Features
    - This release allows customers to modify their preferred commitment term and payment options.

## __Synthetics__
  - ### Features
    - Add support to change ephemeral storage. Add a new field "TestResult" under CanaryRunStatus.

# __2.31.51__ __2025-05-27__
## __AWS Cost Explorer Service__
  - ### Features
    - This release introduces Cost Comparison feature (GetCostAndUsageComparisons, GetCostComparisonDrivers) allowing you find cost variations across multiple dimensions and identify key drivers of spending changes.

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud service-managed fleets now support storage profiles. With storage profiles, you can map file paths between a workstation and the worker hosts running the job.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds three features - option to store AWS Site-to-Site VPN pre-shared keys in AWS Secrets Manager, GetActiveVpnTunnelStatus API to check the in-use VPN algorithms, and SampleType option in GetVpnConnectionDeviceSampleConfiguration API to get recommended sample configs for VPN devices.

# __2.31.50__ __2025-05-23__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix CompletableFuture hanging when RetryStrategy/MetricsCollector raise errors

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for the C7i-flex, M7i-flex, I7i, I7ie, I8g, P6-b200, Trn2, C8gd, M8gd and R8gd instances

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Enable Netty HTTP header validation when connecting with proxy

## __Security Incident Response__
  - ### Features
    - Update PrincipalId pattern documentation to reflect what user should receive back from the API call

# __2.31.49__ __2025-05-22__
## __AWS Audit Manager__
  - ### Features
    - With this release, the AssessmentControl description field has been deprecated, as of May 19, 2025. Additionally, the UpdateAssessment API can now return a ServiceQuotaExceededException when applicable service quotas are exceeded.

## __AWS Glue__
  - ### Features
    - This release supports additional ConversionSpec parameter as part of IntegrationPartition Structure in CreateIntegrationTableProperty API. This parameter is referred to apply appropriate column transformation for columns that are used for timestamp based partitioning

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Update non-streaming error unmarshalling to properly unmarshall exceptions to their expected types.

## __Amazon Aurora DSQL__
  - ### Features
    - Features: support for customer managed encryption keys

## __Amazon Prometheus Service__
  - ### Features
    - Add QueryLoggingConfiguration APIs for Amazon Managed Prometheus

# __2.31.48__ __2025-05-21__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Amazon Bedrock introduces asynchronous flows (in preview), which let you run flows for longer durations and yield control so that your application can perform other tasks and you don't have to actively monitor the flow's progress.

## __Amazon CloudWatch__
  - ### Features
    - Adds support for setting up Contributor Insight rules on logs transformed via Logs Transformation feature.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Release of Dualstack and Ipv6-only EC2 Public DNS hostnames

## __Application Auto Scaling__
  - ### Features
    - Doc only update that addresses a customer reported issue.

## __Partner Central Selling API__
  - ### Features
    - Modified validation to allow expectedCustomerSpend array with zero elements in Partner Opportunity operations.

# __2.31.47__ __2025-05-20__
## __AWS DataSync__
  - ### Features
    - Remove Discovery APIs from the DataSync service

## __AWS Glue__
  - ### Features
    - Enhanced AWS Glue ListConnectionTypes API Model with additional metadata fields.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release expands the ModifyInstanceMaintenanceOptions API to enable or disable instance migration during customer-initiated reboots for EC2 Scheduled Reboot Events.

## __Amazon Relational Database Service__
  - ### Features
    - This release introduces the new DescribeDBMajorEngineVersions API for describing the properties of specific major versions of database engines.

## __CloudWatch Observability Access Manager__
  - ### Features
    - Add IncludeTags field to GetLink, GetSink and UpdateLink API

## __Inspector2__
  - ### Features
    - This release adds GetClustersForImage API and filter updates as part of the mapping of container images to running containers feature.

# __2.31.46__ __2025-05-19__
## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for DVB-DASH, EBU-TT-D subtitle format, and non-compacted manifests for DASH in MediaPackage v2 Origin Endpoints.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Aurora DSQL__
  - ### Features
    - CreateMultiRegionCluster and DeleteMultiRegionCluster APIs removed

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release includes new APIs for System Integrity Protection (SIP) configuration and automated root volume ownership delegation for EC2 Mac instances.

# __2.31.45__ __2025-05-16__
## __AWS CodePipeline__
  - ### Features
    - CodePipeline now supports new API ListDeployActionExecutionTargets that lists the deployment target details for deploy action executions.

## __AWS Glue__
  - ### Features
    - Changes include (1) Excel as S3 Source type and XML and Tableau's Hyper as S3 Sink types, (2) targeted number of partitions parameter in S3 sinks and (3) new compression types in CSV/JSON and Parquet S3 sinks.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix a regression for the JSON REST protocol for which an structure explicit payload member was set to the empty object instead of null

## __Amazon EC2 Container Service__
  - ### Features
    - This is an Amazon ECs documentation only release to support the change of the container exit "reason" field from 255 characters to 1024 characters.

## __Amazon EMR__
  - ### Features
    - Added APIs for managing Application UIs: Access Persistent (serverless) UIs via CreatePersistentAppUI DescribePersistentAppUI & GetPersistentAppUIPresignedURL, and Cluster-based UIs through GetOnClusterAppUIPresignedURL. Supports Yarn, Spark History, and TEZ interfaces.

## __Amazon Neptune__
  - ### Features
    - This release adds Global Cluster Switchover capability which enables you to change your global cluster's primary AWS Region, the region that serves writes, while preserving the replication between all regions in the global cluster.

## __Data Automation for Amazon Bedrock__
  - ### Features
    - Add support for VIDEO modality to BlueprintType enum.

## __Runtime for Amazon Bedrock Data Automation__
  - ### Features
    - Add AssetProcessingConfiguration for video segment to InputConfiguration

## __Service Quotas__
  - ### Features
    - This release introduces CreateSupportCase operation to SDK.

# __2.31.44__ __2025-05-15__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports Docker Server capability

## __AWS Control Tower__
  - ### Features
    - Updated the descriptions for the AWS Control Tower Baseline APIs to make them more intuitive.

## __AWS Database Migration Service__
  - ### Features
    - Introduces Data Resync feature to describe-table-statistics and IAM database authentication for MariaDB, MySQL, and PostgreSQL.

## __AWS Parallel Computing Service__
  - ### Features
    - This release adds support for Slurm accounting. For more information, see the Slurm accounting topic in the AWS PCS User Guide. Slurm accounting is supported for Slurm 24.11 and later. This release also adds 24.11 as a valid value for the version parameter of the Scheduler data type.

## __AWS SDK for Java v2__
  - ### Features
    - Small optimization for endpoint rules. Lazily compile the region pattern instead of parsing it every time. This will pay the penalty of parsing it just once at the cost of using a bit more of memory to keep the parsed pattern.

## __Agents for Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Flows introduces DoWhile loops nodes, parallel node executions, and enhancements to knowledge base nodes.

## __Amazon WorkSpaces__
  - ### Features
    - Added the new AlwaysOn running mode for WorkSpaces Pools. Customers can now choose between AlwaysOn (for instant access, with hourly usage billing regardless of connection status), or AutoStop (to optimize cost, with a brief startup delay) for their pools.

# __2.31.43__ __2025-05-14__
## __AWS Elemental MediaConvert__
  - ### Features
    - This update enables cropping for video overlays and adds a new STL to Teletext upconversion toggle to preserve styling.

## __Amazon CloudWatch Logs__
  - ### Features
    - This release adds a new API "ListLogGroups" and an improvement in API "DescribeLogGroups"

## __Amazon Cognito Identity Provider__
  - ### Features
    - Add exceptions to WebAuthn operations.

## __Amazon Kinesis Firehose__
  - ### Features
    - This release adds catalogARN support for s3 tables multi-catalog catalogARNs.

# __2.31.42__ __2025-05-13__
## __AWS Control Tower__
  - ### Features
    - AWS Control Tower now reports the inheritance drift status for EnabledBaselines through the GetEnabledBaseline and ListEnabledBaselines APIs. You can now filter EnabledBaselines by their enablement and drift status using the ListEnabledBaselines API to view accounts and OUs that require attention.

## __AWS License Manager__
  - ### Features
    - Add Tagging feature to resources in the Managed Entitlements service. License and Grant resources can now be tagged.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Changes for enhanced metadata in trace

## __Amazon Aurora DSQL__
  - ### Features
    - CreateMultiRegionClusters and DeleteMultiRegionClusters APIs marked as deprecated. Introduced new multi-Region clusters creation experience through multiRegionProperties parameter in CreateCluster API.

## __Amazon Bedrock__
  - ### Features
    - Enable cross-Region inference for Amazon Bedrock Guardrails by using the crossRegionConfig parameter when calling the CreateGuardrail or UpdateGuardrail operation.

## __Amazon EC2 Container Service__
  - ### Features
    - This release extends functionality for Amazon EBS volumes attached to Amazon ECS tasks by adding support for the new EBS volumeInitializationRate parameter in ECS RunTask/StartTask/CreateService/UpdateService APIs.

# __2.31.41__ __2025-05-12__
## __AWS Elemental MediaLive__
  - ### Features
    - Add support to the AV1 rate control mode

## __AWS Identity and Access Management__
  - ### Features
    - Updating the endpoint list for the Identity and access management (IAM) service

## __AWS MediaTailor__
  - ### Features
    - Documenting that EnabledLoggingStrategies is always present in responses of PlaybackConfiguration read operations.

## __AWS S3 Control__
  - ### Features
    - Updates to support S3 Express zonal endpoints for directory buckets in AWS CLI

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Add `@Mutable` and `@NotThreadSafe` to model Builders

## __AWS Supply Chain__
  - ### Features
    - Launch new AWS Supply Chain public APIs for DataIntegrationEvent, DataIntegrationFlowExecution and DatasetNamespace. Also add more capabilities to existing public APIs to support direct dataset event publish, data deduplication in DataIntegrationFlow, partition specification of custom datasets.

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud service-managed fleets now support configuration scripts. Configuration scripts make it easy to install additional software, like plugins and packages, onto a worker.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - EC2 - Adding support for AvailabilityZoneId

## __Amazon SageMaker Service__
  - ### Features
    - No API changes from previous release. This release migrated the model to Smithy keeping all features unchanged.

# __2.31.40__ __2025-05-09__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Athena__
  - ### Features
    - Minor API documentation updates

## __Amazon CloudWatch Logs__
  - ### Features
    - We are pleased to announce limit increases to our grok processor logs transformation feature. Now you can define 20 Grok patterns in their configurations, with an expanded total pattern matching limit of 512 characters.

## __Amazon WorkSpaces__
  - ### Features
    - Remove parameter EnableWorkDocs from WorkSpacesServiceModel due to end of support of Amazon WorkDocs service.

## __Synthetics__
  - ### Features
    - Add support to retry a canary automatically after schedule run failures. Users can enable this feature by configuring the RetryConfig field when calling the CreateCanary or UpdateCanary API. Also includes changes in GetCanary and GetCanaryRuns to support retrieving retry configurations.

# __2.31.39__ __2025-05-08__
## __AWS CodePipeline__
  - ### Features
    - Add support for Secrets Manager and Plaintext environment variable types in Commands action

## __AWS Glue__
  - ### Features
    - This new release supports customizable RefreshInterval for all Saas ZETL integrations from 15 minutes to 6 days.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Single Sign-On Admin__
  - ### Features
    - Update PutPermissionBoundaryToPermissionSet API's managedPolicyArn pattern to allow valid ARN only. Update ApplicationName to allow white spaces.

## __Amazon CloudFront__
  - ### Features
    - Doc-only update for CloudFront. These changes include customer-reported issues.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Launching the feature to support ENA queues offering flexibility to support multiple queues per Enhanced Network Interface (ENI)

## __Amazon GuardDuty__
  - ### Features
    - Updated description of a data structure.

# __2.31.38__ __2025-05-07__
## __AWS Elemental MediaLive__
  - ### Features
    - Enables Updating Anywhere Settings on a MediaLive Anywhere Channel.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds API support for Path Component Exclusion (Filter Out ARN) for Reachability Analyzer

## __Amazon SageMaker Service__
  - ### Features
    - SageMaker AI Studio users can now migrate to SageMaker Unified Studio, which offers a unified web-based development experience that integrates AWS data, analytics, artificial intelligence (AI), and machine learning (ML) services, as well as additional tools and resource

## __EC2 Image Builder__
  - ### Features
    - Updated the CreateImageRecipeRequest ParentImage description to include all valid values as updated with the SSM Parameters project.

## __Synthetics__
  - ### Features
    - Add support to test a canary update by invoking a dry run of a canary. This behavior can be used via the new StartCanaryDryRun API along with new fields in UpdateCanary to apply dry run changes. Also includes changes in GetCanary and GetCanaryRuns to support retrieving dry run configurations.

# __2.31.37__ __2025-05-06__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix Smithy RPC v2 CBOR URI resolution allowing custom URIs.
        - Contributed by: [@kstich](https://github.com/kstich)

## __AWS Service Catalog__
  - ### Features
    - ServiceCatalog's APIs (DeleteServiceAction, DisassociateServiceActionFromProvisioningArtifact, AssociateServiceActionWithProvisioningArtifact) now throw InvalidParametersException when IdempotencyToken is invalid.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for Amazon EBS Provisioned Rate for Volume Initialization, which lets you specify a volume initialization rate to ensure that your EBS volumes are initialized in a predictable amount of time.

## __Amazon Timestream Query__
  - ### Features
    - Add dualstack endpoints support and correct us-gov-west-1 FIPS endpoint.

## __Amazon Timestream Write__
  - ### Features
    - Add dualstack endpoints support.

## __Contributors__
Special thanks to the following contributors to this release: 

[@kstich](https://github.com/kstich)
# __2.31.36__ __2025-05-05__
## __AWS CRT-based S3 Client__
  - ### Bugfixes
    - Fixed "Connection pool shut down" error thrown when a default AWS CRT-based S3 client is created and closed per request. See [#5881](https://github.com/aws/aws-sdk-java-v2/issues/5881)

## __AWS Device Farm__
  - ### Features
    - Add an optional parameter to the GetDevicePoolCompatibility API to pass in project information to check device pool compatibility.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds an optional sidecar per-frame video quality metrics report and an ALL_PCM option for audio selectors. It also changes the data type for Probe API response fields related to video and audio bitrate from integer to double.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Allow default values for non-builtIn endpoint params

## __Amazon DataZone__
  - ### Features
    - This release adds a new authorization policy to control the usage of custom AssetType when creating an Asset. Customer can now add new grant(s) of policyType USE_ASSET_TYPE for custom AssetTypes to apply authorization policy to projects members and domain unit owners.

## __Amazon EC2 Container Service__
  - ### Features
    - Add support to roll back an In_Progress ECS Service Deployment

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This update introduces API operations to manage and create local gateway VIF and VIF groups. It also includes API operations to describe Outpost LAGs and service link VIFs.

# __2.31.35__ __2025-05-02__
## __AWS Directory Service__
  - ### Features
    - Doc only update - fixed typos.

## __AWS SDK for Java V2__
  - ### Bugfixes
    - Add synchronization around use of JDT code formatter to prevent NPE/race condition during code generation.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Kinesis__
  - ### Features
    - Marking ResourceARN as required for Amazon Kinesis Data Streams APIs TagResource, UntagResource, and ListTagsForResource.

## __Data Automation for Amazon Bedrock__
  - ### Features
    - Added support for Custom output and blueprints for AUDIO data types.

# __2.31.34__ __2025-05-01__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon AppConfig__
  - ### Features
    - Adding waiter support for deployments and environments; documentation updates

## __Amazon Connect Service__
  - ### Features
    - This release adds the following fields to DescribeContact: DisconnectReason, AgentInitiatedHoldDuration, AfterContactWorkStartTimestamp, AfterContactWorkEndTimestamp, AfterContactWorkDuration, StateTransitions, Recordings, ContactDetails, ContactEvaluations, Attributes

## __Amazon SageMaker Service__
  - ### Features
    - Feature - Adding support for Scheduled and Rolling Update Software in Sagemaker Hyperpod.

## __Amazon Verified Permissions__
  - ### Features
    - Amazon Verified Permissions / Features : Adds support for tagging policy stores.

# __2.31.33__ __2025-04-30__
## __AWS Clean Rooms Service__
  - ### Features
    - This release adds support for ProtectedQuery results to be delivered to more than one collaboration member via the new distribute output configuration in StartProtectedQuery.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSDeadlineCloud__
  - ### Features
    - Adds support for tag management on workers and tag inheritance from fleets to their associated workers.

## __Agents for Amazon Bedrock__
  - ### Features
    - Features: Add inline code node to prompt flow

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Support for Custom Orchestration within InlineAgents

## __Amazon Bedrock__
  - ### Features
    - You can now specify a cross region inference profile as a teacher model for the CreateModelCustomizationJob API. Additionally, the GetModelCustomizationJob API has been enhanced to return the sub-task statuses of a customization job within the StatusDetails response field.

## __Amazon CloudWatch Logs__
  - ### Features
    - CloudWatch Logs supports "DELIVERY" log class. This log class is used only for delivering AWS Lambda logs to Amazon S3 or Amazon Data Firehose.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Launch of cost distribution feature for IPAM owners to distribute costs to internal teams.

## __Amazon Elastic Container Registry__
  - ### Features
    - Adds dualstack support for Amazon Elastic Container Registry (Amazon ECR).

## __Amazon Elastic Container Registry Public__
  - ### Features
    - Adds dualstack support for Amazon Elastic Container Registry Public (Amazon ECR Public).

## __MailManager__
  - ### Features
    - Introducing new RuleSet rule PublishToSns action, which allows customers to publish email notifications to an Amazon SNS topic. New PublishToSns action enables customers to easily integrate their email workflows via Amazon SNS, allowing them to notify other systems about important email events.

# __2.31.32__ __2025-04-29__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix timestamp unmarshalling off-by-one errors
        - Contributed by: [@brandondahler](https://github.com/brandondahler)

## __AWS SSM-GUIConnect__
  - ### Features
    - This release adds API support for the connection recording GUI Connect feature of AWS Systems Manager

## __Amazon Connect Cases__
  - ### Features
    - Introduces CustomEntity as part of the UserUnion data type. This field is used to indicate the entity who is performing the API action.

## __Amazon Kinesis__
  - ### Features
    - Amazon KDS now supports tagging and attribute-based access control (ABAC) for enhanced fan-out consumers.

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - AWS End User Messaging has added MONITOR and FILTER functionality to SMS Protect.

## __Amazon SageMaker Metrics Service__
  - ### Features
    - SageMaker Metrics Service now supports FIPS endpoint in all US and Canada Commercial regions.

## __Amazon SageMaker Service__
  - ### Features
    - Introduced support for P5en instance types on SageMaker Studio for JupyterLab and CodeEditor applications.

## __Amazon Simple Storage Service__
  - ### Features
    - Added LegacyMd5Plugin to perform MD5 checksums for operations that require checksum

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release adds support for just-In-time node access in AWS Systems Manager. Just-in-time node access enables customers to move towards zero standing privileges by requiring operators to request access and obtain approval before remotely connecting to nodes managed by the SSM Agent.

## __QBusiness__
  - ### Features
    - Add support for anonymous user access for Q Business applications

## __Contributors__
Special thanks to the following contributors to this release: 

[@brandondahler](https://github.com/brandondahler)
# __2.31.31__ __2025-04-28__
## __AWS Certificate Manager__
  - ### Features
    - Add support for file-based HTTP domain control validation, available through Amazon CloudFront.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock Runtime__
  - ### Features
    - This release adds native h2 support for the bedrock runtime API, the support is only limited to SDKs that support h2 requests natively.

## __Amazon CloudFront__
  - ### Features
    - Add distribution tenant, connection group, and multi-tenant distribution APIs to the CloudFront SDK.

## __Amazon DynamoDB__
  - ### Features
    - Doc only update for GSI descriptions.

## __EC2 Image Builder__
  - ### Features
    - Add integration with SSM Parameter Store to Image Builder.

# __2.31.30__ __2025-04-25__
## __AWS Marketplace Deployment Service__
  - ### Features
    - Doc only update for the AWS Marketplace Deployment Service that fixes several customer-reported issues.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock Runtime__
  - ### Features
    - You can now reference images and documents stored in Amazon S3 when using InvokeModel and Converse APIs with Amazon Nova Lite and Nova Pro. This enables direct integration of S3-stored multimedia assets in your model requests without manual downloading or base64 encoding.

## __Amazon EC2 Container Service__
  - ### Features
    - Documentation only release for Amazon ECS.

# __2.31.29__ __2025-04-24__
## __AWS App Runner__
  - ### Features
    - AWS App Runner adds Node.js 22 runtime.

## __AWS AppSync__
  - ### Features
    - Add data source support to Event APIs

## __AWS CodeBuild__
  - ### Features
    - Remove redundant validation check.

## __AWS Parallel Computing Service__
  - ### Features
    - Documentation-only update: added valid values for the version property of the Scheduler and SchedulerRequest data types.

## __Amazon DynamoDB__
  - ### Features
    - Add support for ARN-sourced account endpoint generation for TransactWriteItems. This will generate account endpoints for DynamoDB TransactWriteItems requests using ARN-sourced account ID when available.

## __Amazon EC2 Container Service__
  - ### Features
    - Documentation only release for Amazon ECS

## __Amazon Relational Database Service__
  - ### Features
    - This Amazon RDS release adds support for managed master user passwords for Oracle CDBs.

## __Data Automation for Amazon Bedrock__
  - ### Features
    - Added support for modality routing and modality enablement on CreateDataAutomationProject and UpdateDataAutomationProject APIs

# __2.31.28__ __2025-04-23__
## __AWS CodeBuild__
  - ### Features
    - Add support for custom instance type for reserved capacity fleets

## __AWS Resource Explorer__
  - ### Features
    - Documentation-only update for CreateView option correction

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon EC2 Container Service__
  - ### Features
    - Add support to roll back an In_Progress ECS Service Deployment

# __2.31.27__ __2025-04-22__
## __AWS Account__
  - ### Features
    - AWS Account Management now supports account name update via IAM principals.

## __AWS EntityResolution__
  - ### Features
    - To expand support for matching records using digital identifiers with TransUnion

## __AWS S3 Control__
  - ### Features
    - Fix endpoint resolution test cases

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release adds refresh token rotation.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added support for ClientRouteEnforcementOptions flag in CreateClientVpnEndpoint and ModifyClientVpnEndpoint requests and DescribeClientVpnEndpoints responses

## __Amazon S3 Transfer Manager__
  - ### Features
    - Add support for etag validation in resumableFileDownload: restart paused downloads when etag does not match

## __AmazonMQ__
  - ### Features
    - You can now delete Amazon MQ broker configurations using the DeleteConfiguration API. For more information, see Configurations in the Amazon MQ API Reference.

## __Redshift Serverless__
  - ### Features
    - Provides new and updated API members to support the Redshift Serverless reservations feature.

# __2.31.26__ __2025-04-21__
## __AWS ARC - Zonal Shift__
  - ### Features
    - Updates to documentation and exception types for Zonal Autoshift

## __AWS Budgets__
  - ### Features
    - Releasing the new Budget FilterExpression and Metrics fields to support more granular filtering options. These new fields are intended to replace CostFilters and CostTypes, which are deprecated as of 2025/18/04.

## __AWS MediaTailor__
  - ### Features
    - Added support for Recurring Prefetch and Traffic Shaping on both Single and Recurring Prefetch. ListPrefetchSchedules now return single prefetchs by default and can be provided scheduleType of SINGLE, RECURRING, AND ALL.

## __AWS SecurityHub__
  - ### Features
    - Minor documentation update for the GetConfigurationPolicyAssociation example

## __Amazon Kinesis Firehose__
  - ### Features
    - Documentation update regarding the number of streams you can create using the CreateDeliveryStream API.

## __QBusiness__
  - ### Features
    - The CheckDocumentAccess API for Amazon Q Business is a self-service debugging API that allows administrators to verify document access permissions and review Access Control List (ACL) configurations.

# __2.31.25__ __2025-04-18__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Q Connect__
  - ### Features
    - This release adds support for the following capabilities: Chunking generative answer replies from Amazon Q in Connect. Integration support for the use of additional LLM models with Amazon Q in Connect.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds a new Neuron driver option in InferenceAmiVersion parameter for ProductionVariant. Additionally, it adds support for fetching model lifecycle status in the ListModelPackages API. Users can now use this API to view the lifecycle stage of models that have been shared with them.

## __Service Quotas__
  - ### Features
    - Add new optional SupportCaseAllowed query parameter to the RequestServiceQuotaIncrease API

# __2.31.24__ __2025-04-17__
## __AWS IoT FleetWise__
  - ### Features
    - We've added stricter parameter validations to AWS IoT FleetWise signal catalog, model manifest, and decoder manifest APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Access Analyzer__
  - ### Features
    - Added new resource types to evaluate for public access in resource policies and added support for S3 directory bucket access points.

## __Amazon Bedrock__
  - ### Features
    - With this release, Bedrock Evaluation will now support custom metrics for evaluation.

## __Amazon Connect Service__
  - ### Features
    - This release adds following capabilities to Contact Lens Rules APIs 1/ 'ASSIGN_SLA' action and '$.Case.TemplateId' comparison value for 'OnCaseCreate' and 'OnCaseUpdate' event sources 2/ 'OnSlaBreach' Cases event source which supports '$.RelatedItem.SlaConfiguration.Name' comparison value

## __Amazon EC2 Container Service__
  - ### Features
    - Adds a new AccountSetting - defaultLogDriverMode for ECS.

## __Amazon MemoryDB__
  - ### Features
    - Added support for IPv6 and dual stack for Valkey and Redis clusters. Customers can now launch new Valkey and Redis clusters with IPv6 and dual stack networking support.

## __Amazon Omics__
  - ### Features
    - Add versioning for HealthOmics workflows

## __Amazon Prometheus Service__
  - ### Features
    - Add Workspace Configuration APIs for Amazon Prometheus

## __Auto Scaling__
  - ### Features
    - Doc only update for EC2 Auto Scaling.

# __2.31.23__ __2025-04-16__
## __AWS Resource Groups__
  - ### Features
    - Resource Groups: TagSyncTasks can be created with ResourceQuery

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Compile v2 migration OpenRewrite recipes with `-parameters`
        - Contributed by: [@greg-at-moderne](https://github.com/greg-at-moderne)

## __AWS Service Catalog__
  - ### Features
    - Updated default value for the access-level-filter in SearchProvisionedProducts API to Account. For access to userLevel or roleLevel, the user must provide access-level-filter parameter.

## __Amazon Aurora DSQL__
  - ### Features
    - Added GetClusterEndpointService API. The new API allows retrieving endpoint service name specific to a cluster.

## __Amazon Connect Cases__
  - ### Features
    - This feature provides capabilities to help track and meet service level agreements (SLAs) on cases programmatically. It allows configuring a new related item of type `Sla` on a case using CreateRelatedItem API and provides the ability to search for this new related item using SearchRelatedItems API.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support for new AL2023 ARM64 NVIDIA AMIs to the supported AMITypes.

## __Amazon EventBridge__
  - ### Features
    - Adding support for KmsKeyIdentifer in CreateConnection, UpdateConnection and DescribeConnection APIs

## __Amazon S3 Tables__
  - ### Features
    - S3 Tables now supports setting encryption configurations on table buckets and tables. Encryption configurations can use server side encryption using AES256 or KMS customer-managed keys.

## __Contributors__
Special thanks to the following contributors to this release: 

[@greg-at-moderne](https://github.com/greg-at-moderne)
# __2.31.22__ __2025-04-14__
## __AWS EntityResolution__
  - ### Features
    - This is to add new metrics to our GetIdMappingJob API and also update uniqueId naming for batchDeleteUniqueIds API to be more accurate

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Restored the 2.13.x changelog file.

## __Tax Settings__
  - ### Features
    - Indonesia SOR Tax Registration Launch

# __2.31.21__ __2025-04-11__
## __AWS Marketplace Entitlement Service__
  - ### Features
    - Add support for Marketplace Entitlement Service dual-stack endpoints for CN and GOV regions

## __AWS Parallel Computing Service__
  - ### Features
    - Changed the minimum length of clusterIdentifier, computeNodeGroupIdentifier, and queueIdentifier to 3.

## __AWS SDK for Java v2__
  - ### Features
    - Update `aws-crt` to `0.38.1`.
    - Updated endpoint and partition metadata.

## __AWSMarketplace Metering__
  - ### Features
    - Add support for Marketplace Metering Service dual-stack endpoints for CN regions

## __Amazon Connect Contact Lens__
  - ### Features
    - Making sentiment optional for ListRealtimeContactAnalysisSegments Response depending on conversational analytics configuration

## __Amazon DataZone__
  - ### Features
    - Raise hard limit of authorized principals per SubscriptionTarget from 10 to 20.

## __Amazon Detective__
  - ### Features
    - Add support for Detective DualStack endpoints

## __Amazon DynamoDB__
  - ### Features
    - Doc only update for API descriptions.

## __Amazon Verified Permissions__
  - ### Features
    - Adds deletion protection support to policy stores. Deletion protection is disabled by default, can be enabled via the CreatePolicyStore or UpdatePolicyStore APIs, and is visible in GetPolicyStore.

# __2.31.20__ __2025-04-10__
## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive / Features : Add support for CMAF Ingest CaptionLanguageMappings, TimedMetadataId3 settings, and Link InputResolution.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSMainframeModernization__
  - ### Features
    - Introduce three new APIs: CreateDataSetExportTask, GetDataSetExportTask and ListDataSetExportHistory. Add support for batch restart for Blu Age applications.

## __Amazon ElastiCache__
  - ### Features
    - AWS ElastiCache SDK now supports using MemcachedUpgradeConfig parameter with ModifyCacheCluster API to enable updating Memcached cache node types. Please refer to updated AWS ElastiCache public documentation for detailed information on API usage and implementation.

## __Amazon QuickSight__
  - ### Features
    - Add support to analysis and sheet level highlighting in QuickSight.

## __Application Auto Scaling__
  - ### Features
    - Application Auto Scaling now supports horizontal scaling for Elasticache Memcached self-designed clusters using target tracking scaling policies and scheduled scaling.

## __QBusiness__
  - ### Features
    - Adds functionality to enable/disable a new Q Business Hallucination Reduction feature. If enabled, Q Business will detect and attempt to remove Hallucinations from certain Chat requests.

# __2.31.19__ __2025-04-09__
## __AWS Control Catalog__
  - ### Features
    - The GetControl API now surfaces a control's Severity, CreateTime, and Identifier for a control's Implementation. The ListControls API now surfaces a control's Behavior, Severity, CreateTime, and Identifier for a control's Implementation.

## __AWS Glue__
  - ### Features
    - The TableOptimizer APIs in AWS Glue now return the DpuHours field in each TableOptimizerRun, providing clients visibility to the DPU-hours used for billing in managed Apache Iceberg table compaction optimization.

## __AWS Ground Station__
  - ### Features
    - Support tagging Agents and adjust input field validations

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Transfer Family__
  - ### Features
    - This launch includes 2 enhancements to SFTP connectors user-experience: 1) Customers can self-serve concurrent connections setting for their connectors, and 2) Customers can discover the public host key of remote servers using their SFTP connectors.

## __Amazon DynamoDB__
  - ### Features
    - Documentation update for secondary indexes and Create_Table.

# __2.31.18__ __2025-04-08__
## __AWS Cost Explorer Service__
  - ### Features
    - This release supports Pagination traits on Cost Anomaly Detection APIs.

## __AWS IoT FleetWise__
  - ### Features
    - This release adds the option to update the strategy of state templates already associated to a vehicle, without the need to remove and re-add them.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Documentation updates for AWS Security Hub.

## __AWS Storage Gateway__
  - ### Features
    - Added new ActiveDirectoryStatus value, ListCacheReports paginator, and support for longer pagination tokens.

## __Amazon Bedrock Runtime__
  - ### Features
    - This release introduces our latest bedrock runtime API, InvokeModelWithBidirectionalStream. The API supports both input and output streams and is supported by only HTTP2.0.

## __Cost Optimization Hub__
  - ### Features
    - This release adds resource type "MemoryDbReservedInstances" and resource type "DynamoDbReservedCapacity" to the GetRecommendation, ListRecommendations, and ListRecommendationSummaries APIs to support new MemoryDB and DynamoDB RI recommendations.

## __Tax Settings__
  - ### Features
    - Uzbekistan Launch on TaxSettings Page

# __2.31.17__ __2025-04-07__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now offers an enhanced debugging experience.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports SDI inputs to MediaLive Anywhere Channels in workflows that use AWS SDKs.

## __AWS Glue__
  - ### Features
    - Add input validations for multiple Glue APIs

## __AWS Transfer Family__
  - ### Features
    - This launch enables customers to manage contents of their remote directories, by deleting old files or moving files to archive folders in remote servers once they have been retrieved. Customers will be able to automate the process using event-driven architecture.

## __Amazon Bedrock__
  - ### Features
    - New options for how to handle harmful content detected by Amazon Bedrock Guardrails.

## __Amazon Bedrock Runtime__
  - ### Features
    - New options for how to handle harmful content detected by Amazon Bedrock Guardrails.

## __Amazon Personalize__
  - ### Features
    - Add support for eventsConfig for CreateSolution, UpdateSolution, DescribeSolution, DescribeSolutionVersion. Add support for GetSolutionMetrics to return weighted NDCG metrics when eventsConfig is enabled for the solution.

# __2.31.16__ __2025-04-04__
## __AWS Directory Service Data__
  - ### Features
    - Doc only update - fixed broken links.

## __AWS S3 Control__
  - ### Features
    - Updated max size of Prefixes parameter of Scope data type.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Doc-only updates for Amazon EC2

## __Amazon EventBridge__
  - ### Features
    - Amazon EventBridge adds support for customer-managed keys on Archives and validations for two fields: eventSourceArn and kmsKeyIdentifier.

# __2.31.15__ __2025-04-03__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - Added optional "customMetadataField" for Amazon Aurora knowledge bases, allowing single-column metadata. Also added optional "textIndexName" for MongoDB Atlas knowledge bases, enabling hybrid search support.

## __Amazon Chime SDK Voice__
  - ### Features
    - Added FOC date as an attribute of PhoneNumberOrder, added AccessDeniedException as a possible return type of ValidateE911Address

## __Amazon OpenSearch Service__
  - ### Features
    - Improve descriptions for various API commands and data types.

## __Amazon Route 53__
  - ### Features
    - Added us-gov-east-1 and us-gov-west-1 as valid Latency Based Routing regions for change-resource-record-sets.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for i3en, m7i, r7i instance types for SageMaker Hyperpod

## __Amazon Simple Email Service__
  - ### Features
    - This release enables customers to provide attachments in the SESv2 SendEmail and SendBulkEmail APIs.

## __Amazon Transcribe Service__
  - ### Features
    - This Feature Adds Support for the "zh-HK" Locale for Batch Operations

## __MailManager__
  - ### Features
    - Add support for Dual_Stack and PrivateLink types of IngressPoint. For configuration requests, SES Mail Manager will now accept both IPv4/IPv6 dual-stack endpoints and AWS PrivateLink VPC endpoints for email receiving.

# __2.31.14__ __2025-04-02__
## __AWS CodeBuild__
  - ### Features
    - This release adds support for environment type WINDOWS_SERVER_2022_CONTAINER in ProjectEnvironment

## __AWS Elemental MediaLive__
  - ### Features
    - Added support for SMPTE 2110 inputs when running a channel in a MediaLive Anywhere cluster. This feature enables ingestion of SMPTE 2110-compliant video, audio, and ancillary streams by reading SDP files that AWS Elemental MediaLive can retrieve from a network source.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - Application Signals now supports creating Service Level Objectives on service dependencies. Users can now create or update SLOs on discovered service dependencies to monitor their standard application metrics.

## __Amazon EC2 Container Service__
  - ### Features
    - This is an Amazon ECS documentation only update to address various tickets.

## __Amazon Elastic Container Registry__
  - ### Features
    - Fix for customer issues related to AWS account ID and size limitation for token.

## __Amazon Lex Model Building V2__
  - ### Features
    - Release feature of errorlogging for lex bot, customer can config this feature in bot version to generate log for error exception which helps debug

# __2.31.13__ __2025-04-01__
## __AWS Clean Rooms Service__
  - ### Features
    - This release adds support for updating the analytics engine of a collaboration.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon SageMaker Service__
  - ### Features
    - Added tagging support for SageMaker notebook instance lifecycle configurations

# __2.31.12__ __2025-03-31__
## __AWS Marketplace Entitlement Service__
  - ### Features
    - Add support for Marketplace Entitlement Service dual-stack endpoints.

## __AWS Outposts__
  - ### Features
    - Enabling Asset Level Capacity Management feature, which allows customers to create a Capacity Task for a single Asset on their active Outpost.

## __AWS S3 Control__
  - ### Features
    - Amazon S3 adds support for S3 Access Points for directory buckets in AWS Dedicated Local Zones

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Transfer Family__
  - ### Features
    - Add WebAppEndpointPolicy support for WebApps

## __AWSDeadlineCloud__
  - ### Features
    - With this release you can use a new field to specify the search term match type. Search term match types currently support fuzzy and contains matching.

## __Amazon Bedrock Runtime__
  - ### Features
    - Add Prompt Caching support to Converse and ConverseStream APIs

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Use String instead of Select enum for ProjectionExpression to support future values

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Release VPC Route Server, a new feature allowing dynamic routing in VPCs.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Add support for updating RemoteNetworkConfig for hybrid nodes on EKS UpdateClusterConfig API

## __Amazon Simple Email Service__
  - ### Features
    - Add dual-stack support to global endpoints.

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon S3 adds support for S3 Access Points for directory buckets in AWS Dedicated Local Zones

# __2.31.11__ __2025-03-28__
## __AWS CRT-based S3 Client__
  - ### Bugfixes
    - Fixed an issue in the AWS CRT-based S3 client where a GetObject request with `AsyncResponseTransformer#toBlockingInputStream` may hang if request failed mid streaming

## __AWS CodeBuild__
  - ### Features
    - This release adds support for cacheNamespace in ProjectCache

## __AWS Network Manager__
  - ### Features
    - Add support for NetworkManager Dualstack endpoints.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSMarketplace Metering__
  - ### Features
    - Add support for Marketplace Metering Service dual-stack endpoints.

## __Amazon API Gateway__
  - ### Features
    - Adds support for setting the IP address type to allow dual-stack or IPv4 address types to invoke your APIs or domain names.

## __Amazon Bedrock Runtime__
  - ### Features
    - Launching Multi-modality Content Filter for Amazon Bedrock Guardrails.

## __Amazon EC2 Container Service__
  - ### Features
    - This is an Amazon ECS documentation only release that addresses tickets.

## __Amazon QuickSight__
  - ### Features
    - RLS permission dataset with userAs: RLS_RULES flag, Q in QuickSight/Threshold Alerts/Schedules/Snapshots in QS embedding, toggle dataset refresh email alerts via API, transposed table with options: column width, type and index, toggle Q&A on dashboards, Oracle Service Name when creating data source.

## __Amazon SageMaker Service__
  - ### Features
    - TransformAmiVersion for Batch Transform and SageMaker Search Service Aggregate Search API Extension

## __AmazonApiGatewayV2__
  - ### Features
    - Adds support for setting the IP address type to allow dual-stack or IPv4 address types to invoke your APIs or domain names.

## __Payment Cryptography Control Plane__
  - ### Features
    - The service adds support for transferring AES-256 and other keys between the service and other service providers and HSMs. This feature uses ECDH to derive a one-time key transport key to enable these secure key exchanges.

# __2.31.10__ __2025-03-27__
## __AWS Batch__
  - ### Features
    - This release will enable two features: Firelens log driver, and Execute Command on Batch jobs on ECS. Both features will be passed through to ECS.

## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Added standaloneAccountRateTypeSelections for GetPreferences and UpdatePreferences APIs. Added STALE enum value to status attribute in GetBillScenario and UpdateBillScenario APIs.

## __AWS CloudFormation__
  - ### Features
    - Adding support for the new parameter "ScanFilters" in the CloudFormation StartResourceScan API. When this parameter is included, the StartResourceScan API will initiate a scan limited to the resource types specified by the parameter.

## __AWS Identity and Access Management__
  - ### Features
    - Update IAM dual-stack endpoints for BJS, IAD and PDT partitions

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SSO OIDC__
  - ### Features
    - This release adds AwsAdditionalDetails in the CreateTokenWithIAM API response.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - bedrock flow now support node action trace.

## __Amazon DataZone__
  - ### Features
    - This release adds new action type of Create Listing Changeset for the Metadata Enforcement Rule feature.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support for BOTTLEROCKET FIPS AMIs to AMI types in US regions.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers add support for additional instance types.

## __Amazon SageMaker Service__
  - ### Features
    - add: recovery mode for SageMaker Studio apps

# __2.31.9__ __2025-03-26__
## __AWS ARC - Zonal Shift__
  - ### Features
    - Add new shiftType field for ARC zonal shifts.

## __AWS Direct Connect__
  - ### Features
    - With this release, AWS Direct Connect allows you to tag your Direct Connect gateways. Tags are metadata that you can create and use to manage your Direct Connect gateways. For more information about tagging, see AWS Tagging Strategies.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds a configurable Quality Level setting for the top rendition of Auto ABR jobs

## __AWS MediaTailor__
  - ### Features
    - Add support for log filtering which allow customers to filter out selected event types from logs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS WAFV2__
  - ### Features
    - This release adds the ability to associate an AWS WAF v2 web ACL with an AWS Amplify App.

## __Amazon Polly__
  - ### Features
    - Added support for the new voice - Jihye (ko-KR). Jihye is available as a Neural voice only.

## __Amazon Relational Database Service__
  - ### Features
    - Add note about the Availability Zone where RDS restores the DB cluster for the RestoreDBClusterToPointInTime operation.

# __2.31.8__ __2025-03-25__
## __AWS Marketplace Entitlement Service__
  - ### Features
    - This release enhances the GetEntitlements API to support new filter CUSTOMER_AWS_ACCOUNT_ID in request and CustomerAWSAccountId field in response.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSMarketplace Metering__
  - ### Features
    - This release enhances the BatchMeterUsage API to support new field CustomerAWSAccountId in request and response and making CustomerIdentifier optional. CustomerAWSAccountId or CustomerIdentifier must be provided in request but not both.

## __Agents for Amazon Bedrock__
  - ### Features
    - Adding support for Amazon OpenSearch Managed clusters as a vector database in Knowledge Bases for Amazon Bedrock

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support to override upgrade-blocking readiness checks via force flag when updating a cluster.

## __Amazon GameLift Streams__
  - ### Features
    - Minor updates to improve developer experience.

## __Amazon Keyspaces__
  - ### Features
    - Removing replication region limitation for Amazon Keyspaces Multi-Region Replication APIs.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds support for customer-managed KMS keys in Amazon SageMaker Partner AI Apps

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Deprecate tags field in Get API responses

## __EC2 Metadata Client__
  - ### Features
    - Added new Ec2MetadataClientException extending SdkClientException for IMDS unsuccessful responses that captures HTTP status codes, headers, and raw response content for improved error handling. See [#5786](https://github.com/aws/aws-sdk-java-v2/issues/5786)

# __2.31.7__ __2025-03-24__
## __AWS IoT Wireless__
  - ### Features
    - Mark EutranCid under LteNmr optional.

## __AWS Parallel Computing Service__
  - ### Features
    - ClusterName/ClusterIdentifier, ComputeNodeGroupName/ComputeNodeGroupIdentifier, and QueueName/QueueIdentifier can now have 10 characters, and a minimum of 3 characters. The TagResource API action can now return ServiceQuotaExceededException.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Q Connect__
  - ### Features
    - Provides the correct value for supported model ID.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release adds the AvailableSecurityUpdatesComplianceStatus field to patch baseline operations, as well as the AvailableSecurityUpdateCount and InstancesWithAvailableSecurityUpdates to patch state operations. Applies to Windows Server managed nodes only.

# __2.31.6__ __2025-03-21__
## __AWS Route53 Recovery Control Config__
  - ### Features
    - Adds dual-stack (IPv4 and IPv6) endpoint support for route53-recovery-control-config operations, opt-in dual-stack addresses for cluster endpoints, and UpdateCluster API to update the network-type of clusters between IPv4 and dual-stack.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - A CustomModelUnit(CMU) is an abstract view of the hardware utilization that Bedrock needs to host a a single copy of your custom imported model. Bedrock determines the number of CMUs that a model copy needs when you import the custom model. You can use CMUs to estimate the cost of Inference's.

## __Amazon DataZone__
  - ### Features
    - Add support for overriding selection of default AWS IAM Identity Center instance as part of Amazon DataZone domain APIs.

## __Amazon SageMaker Service__
  - ### Features
    - This release does the following: 1.) Adds DurationHours as a required field to the SearchTrainingPlanOfferings action in the SageMaker AI API; 2.) Adds support for G6e instance types for SageMaker AI inference optimization jobs.

# __2.31.5__ __2025-03-20__
## __AWS Amplify__
  - ### Features
    - Added appId field to Webhook responses

## __AWS Control Catalog__
  - ### Features
    - Add ExemptAssumeRoot parameter to adapt for new AWS AssumeRoot capability.

## __AWS Network Firewall__
  - ### Features
    - You can now use flow operations to either flush or capture traffic monitored in your firewall's flow table.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - With this release, Bedrock Evaluation will now support bring your own inference responses.

## __MailManager__
  - ### Features
    - Amazon SES Mail Manager. Extended rule string and boolean expressions to support analysis in condition evaluation. Extended ingress point string expression to support analysis in condition evaluation

# __2.31.4__ __2025-03-19__
## __AWS Lambda__
  - ### Features
    - Add Ruby 3.4 (ruby3.4) support to AWS Lambda.

## __AWS MediaConnect__
  - ### Features
    - This release adds support for NDI flow outputs in AWS Elemental MediaConnect. You can now send content from your MediaConnect transport streams directly to your NDI environment using the new NDI output type.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Support custom prompt routers for evaluation jobs

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Doc-only updates for EC2 for March 2025.

## __Amazon Neptune Graph__
  - ### Features
    - Update IAM Role ARN Validation to Support Role Paths

## __Amazon SageMaker Service__
  - ### Features
    - Added support for g6, g6e, m6i, c6i instance types in SageMaker Processing Jobs.

# __2.31.3__ __2025-03-18__
## __AWS AppSync__
  - ### Features
    - Providing Tagging support for DomainName in AppSync

## __AWS Clean Rooms Service__
  - ### Features
    - This release adds support for PySpark jobs. Customers can now analyze data by running jobs using approved PySpark analysis templates.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds support for AVC passthrough, the ability to specify PTS offset without padding, and an A/V segment matching feature.

## __AWS SDK for Java v2__
  - ### Features
    - Added functionality to be able to configure an endpoint override through the [services] section in the aws config file for specific services. 
      https://docs.aws.amazon.com/sdkref/latest/guide/feature-ss-endpoints.html
    - Updated endpoint and partition metadata.

## __Amazon Route 53__
  - ### Features
    - Amazon Route 53 now supports the iso-f regions for private DNS Amazon VPCs and cloudwatch healthchecks.

# __2.31.2__ __2025-03-17__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Copy bytes written to OutputStream of BlockingOutputStreamAsyncRequestBody

## __AWS WAFV2__
  - ### Features
    - AWS WAF now lets you inspect fragments of request URIs. You can specify the scope of the URI to inspect and narrow the set of URI fragments.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - This release adds support for adding, removing, and listing SLO time exclusion windows with the BatchUpdateExclusionWindows and ListServiceLevelObjectiveExclusionWindows APIs.

## __Amazon Location Service Maps V2__
  - ### Features
    - Provide support for vector map styles in the GetStaticMap operation.

## __CloudWatch RUM__
  - ### Features
    - CloudWatch RUM now supports unminification of JS error stack traces.

## __Tax Settings__
  - ### Features
    - Adjust Vietnam PaymentVoucherNumber regex and minor API change.

# __2.31.1__ __2025-03-14__
## __AWS CRT HTTP Client__
  - ### Features
    - Map AWS_IO_SOCKET_TIMEOUT to ConnectException when acquiring a connection to improve error handling
        - Contributed by: [@thomasjinlo](https://github.com/thomasjinlo)

## __AWS Glue__
  - ### Features
    - This release added AllowFullTableExternalDataAccess to glue catalog resource.

## __AWS Lake Formation__
  - ### Features
    - This release added "condition" to LakeFormation OptIn APIs, also added WithPrivilegedAccess flag to RegisterResource and DescribeResource.

## __AWS SDK for Java v2__
  - ### Features
    - Made DefaultSdkAutoConstructList and DefaultSdkAutoConstructMap serializable
    - Updated endpoint and partition metadata.

## __Amazon Cognito Identity__
  - ### Features
    - Updated API model build artifacts for identity pools

## __Amazon Cognito Identity Provider__
  - ### Features
    - Minor description updates to API parameters

## __Amazon S3__
  - ### Bugfixes
    - Updated logic for S3MultiPartUpload. Part numbers are now assigned and incremented when parts are read.

## __Contributors__
Special thanks to the following contributors to this release: 

[@thomasjinlo](https://github.com/thomasjinlo)
# __2.31.0__ __2025-03-13__
## __AWS Amplify__
  - ### Features
    - Introduced support for Skew Protection. Added enableSkewProtection field to createBranch and updateBranch API.

## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Private Certificate Authority service now supports P521 and RSA3072 key algorithms.

## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports webhook filtering by organization name

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds the ResetChannelState and ResetOriginEndpointState operation to reset MediaPackage V2 channel and origin endpoint. This release also adds a new field, UrlEncodeChildManifest, for HLS/LL-HLS to allow URL-encoding child manifest query string based on the requirements of AWS SigV4.

## __AWS S3 Control__
  - ### Features
    - Updating GetDataAccess response for S3 Access Grants to include the matched Grantee for the requested prefix

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Updated the SDK to handle error thrown from consumer subscribed to paginator publisher, which caused the request to hang for pagination operations

## __Amazon CloudWatch Logs__
  - ### Features
    - Updated CreateLogAnomalyDetector to accept only kms key arn

## __Amazon DataZone__
  - ### Features
    - This release adds support to update projects and environments

## __Amazon DynamoDB__
  - ### Features
    - Generate account endpoints for DynamoDB requests using ARN-sourced account ID when available

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release changes the CreateLaunchTemplate, CreateLaunchTemplateVersion, ModifyLaunchTemplate CLI and SDKs such that if you do not specify a client token, a randomly generated token is used for the request to ensure idempotency.

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - IVS Real-Time now offers customers the ability to adjust the participant & composition recording segment duration


 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


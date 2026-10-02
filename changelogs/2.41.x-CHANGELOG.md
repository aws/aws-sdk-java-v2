 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.41.34__ __2026-02-20__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Signer Data Plane__
  - ### Features
    - This release introduces AWS Signer Data Plane SDK client supporting GetRevocationStatus API. The new client enables AWS PrivateLink connectivity with both private DNS and VPC endpoint URLs.

## __Amazon AppStream__
  - ### Features
    - Adding new attribute to disable IMDS v1 APIs for fleet, Image Builder and AppBlockBuilder instances.

## __Amazon EC2 Container Service__
  - ### Features
    - Migrated to Smithy. No functional changes

## __Amazon SageMaker Runtime__
  - ### Features
    - Added support for S3OutputPathExtension and Filename parameters to the InvokeEndpointAsync API to allow users to customize the S3 output path and file name for async inference response payloads.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Add support for AssociationDispatchAssumeRole in AWS SSM State Manager.

## __TrustedAdvisor Public API__
  - ### Features
    - Adding a new enum attribute(statusReason) to TrustedAdvisorAPI response. This attribute explains reasoning behind check status for certain specific scenarios.

# __2.41.33__ __2026-02-19__
## __AWS Billing and Cost Management Dashboards__
  - ### Features
    - The Billing and Cost Management GetDashboard API now returns identifier for each widget, enabling users to uniquely identify widgets within their dashboards.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Correctly handle unions with members named "type" by renaming the member variable to avoid conflicts with the existing SDK added "type" field.

## __Amazon Elastic Container Registry__
  - ### Features
    - Adds multiple artifact types filter support in ListImageReferrers API.

## __Private CA Connector for SCEP__
  - ### Features
    - AWS Private CA Connector for SCEP now supports AWS PrivateLink, allowing your clients to request certificates from within your Amazon Virtual Private Cloud (VPC) without traversing the public internet. With this launch, you can create VPC endpoints to connect to your SCEP connector privately.

# __2.41.32__ __2026-02-18__
## __AWS Clean Rooms Service__
  - ### Features
    - This release adds support for federated catalogs in Athena-sourced configured tables.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Updating Lakeformation Access Grants Plugin version to 1.3
        - Contributed by: [@akhilyendluri](https://github.com/akhilyendluri)

## __Amazon Connect Service__
  - ### Features
    - Correcting in-app notifications API documentation.

## __Contributors__
Special thanks to the following contributors to this release: 

[@akhilyendluri](https://github.com/akhilyendluri)
# __2.41.31__ __2026-02-17__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add Operator field to CreatePlacementGroup and DescribePlacementGroup APIs.

## __Amazon Managed Grafana__
  - ### Features
    - This release updates Amazon Managed Grafana's APIs to support customer managed KMS keys.

## __Amazon Relational Database Service__
  - ### Features
    - Adds support for the StorageEncryptionType field to specify encryption type for DB clusters, DB instances, snapshots, automated backups, and global clusters.

## __Amazon WorkSpaces Web__
  - ### Features
    - Adds support for branding customization without requiring a custom wallpaper.

# __2.41.30__ __2026-02-16__
## __ARC - Region switch__
  - ### Features
    - Clarify documentation on ARC Region Switch start-plan-execution operation

## __AWS Key Management Service__
  - ### Features
    - Added support for Decrypt and ReEncrypt API's to use dry run feature without ciphertext for authorization validation

## __AWS SDK for Java v2__
  - ### Features
    - Migrate PartitionMetadata code generation from endpoints.json to partitions.json
    - Migrate PartitionMetadataProvider code generation from endpoints.json to partitions.json
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for EC2 Secondary Networks

## __Amazon Elastic Container Registry__
  - ### Features
    - Adds support for enabling blob mounting, and removes support for Clair based image scanning

## __Amazon Q Connect__
  - ### Features
    - Update MessageType enum to include missing types.

## __Managed Streaming for Kafka__
  - ### Features
    - Amazon MSK now supports dual-stack connectivity (IPv4 and IPv6) for existing MSK clusters. You can enable dual-stack on existing clusters by specifying the NetworkType parameter in updateConnectivity API.

# __2.41.29__ __2026-02-13__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Improve support for operationContextParams with chained index and multi-select expressions and improve support for StringArray endpoint parametes.
    - Updating S3 AccessGrants plugin version to 2.4.1
        - Contributed by: [@prime025](https://github.com/prime025)

## __Amazon CloudWatch__
  - ### Features
    - Adding new evaluation states that provides information about the alarm evaluation process. Evaluation error Indicates configuration errors in alarm setup that require review and correction. Evaluation failure Indicates temporary CloudWatch issues.

## __Amazon Connect Service__
  - ### Features
    - API release for headerr notifications in the admin website. APIs allow customers to publish brief messages (including URLs) to a specified audience, and a new header icon will indicate when unread messages are available.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds geography information to EC2 region and availability zone APIs. DescribeRegions now includes a Geography field, while DescribeAvailabilityZones includes both Geography and SubGeography fields, enabling better geographic classification for AWS regions and zones.

## __Amazon SageMaker Service__
  - ### Features
    - Enable g7e instance type support for SageMaker Processing, and enable single file configuration provisioning for HyperPod Slurm, where customers have the option to use HyperPod API to provide the provisioning parameters.

## __Inspector2__
  - ### Features
    - Added .Net 10 (dotnet10) and Node 24.x (node24.x) runtime support for lambda package scanning

## __Contributors__
Special thanks to the following contributors to this release: 

[@prime025](https://github.com/prime025)
# __2.41.28__ __2026-02-12__
## __AWS SDK for Java v2__
  - ### Features
    - Add `WRITE_THROUGHPUT` metric to measure request body upload speed (bytes/sec). This metric is reported at the API call attempt level for requests with a body.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Launching nested virtualization. This feature allows you to run nested VMs inside virtual (non-bare metal) EC2 instances.

# __2.41.27__ __2026-02-11__
## __AWS Batch__
  - ### Features
    - Add support for listing jobs by share identifier and getting snapshots of active capacity utilization by job queue and share.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - R8i instances powered by custom Intel Xeon 6 processors available only on AWS with sustained all-core 3.9 GHz turbo frequency

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - This release adds support for Windows Server 2025 in Amazon EKS Managed Node Groups.

## __Amazon S3 Tables__
  - ### Features
    - S3 Tables now supports setting partition specifications and sort orders on tables. Partition specs allow users to define how data is organized using transform functions. Sort order configurations enable users to specify sort directions and null ordering preferences for optimized data layout.

## __Managed Streaming for Kafka Connect__
  - ### Features
    - Support configurable upper limits on task count during autoscaling operations via maxAutoscalingTaskCount parameter.

# __2.41.26__ __2026-02-10__
## __Amazon Bedrock AgentCore__
  - ### Features
    - Added AgentCore browser proxy configuration support, allowing routing of browser traffic through HTTP and HTTPS proxy servers with authentication and bypass rules.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect now supports per-channel auto-accept and After Contact Work (ACW) timeouts. Configure agents with auto-accept and ACW timeout settings for chat, tasks, emails, and callbacks. Use the new UpdateUserConfig API to manage these settings.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Introducing an optional policy field, an IAM policy applied to pod identity associations in addition to IAM role policies. When specified, pod permissions are the intersection of IAM role policies and the policy field, ensuring the principle of least privilege.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds backup configuration for RDS and Aurora restores, letting customers set backup retention period and preferred backup window during restore. It also enables viewing backup settings when describing snapshots or automated backups for instances and clusters.

## __Managed Streaming for Kafka__
  - ### Features
    - Amazon MSK adds three new APIs, CreateTopic, UpdateTopic, and DeleteTopic for managing Kafka topics in your MSK clusters.

# __2.41.25__ __2026-02-09__
## __AWS Lake Formation__
  - ### Features
    - Allow cross account v5 in put data lake settings

## __AWS Parallel Computing Service__
  - ### Features
    - Introduces RESUMING state for clusters, compute node groups, and queues.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Transfer Family__
  - ### Features
    - This release adds a documentation update for MdnResponse of type "ASYNC"

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon Secondary Networks is a networking feature that provides high-performance, low-latency connectivity for specialized workloads.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Amazon EKS adds a new DescribeUpdate update type, VendedLogsUpdate, to support an integration between EKS Auto Mode and Amazon CloudWatch Vended Logs.

## __Amazon NeptuneData__
  - ### Features
    - Added edgeOnlyLoad boolean parameter to Neptune bulk load request. When TRUE, files are loaded in order without scanning. When FALSE (default), the loader scans files first, then loads vertex files before edge files automatically.

## __AmazonConnectCampaignServiceV2__
  - ### Features
    - Add the missing event type for WhatsApp

## __EC2 Image Builder__
  - ### Features
    - EC2 Image Builder now supports wildcard patterns in lifecycle policies with recipes and enhances the experience of tag-scoped policies.

# __2.41.24__ __2026-02-06__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Update session credentials builder in HttpsCredentialsLoader to include expiration time so it's set in session credentials for ContainerCredentialsProvider
        - Contributed by: [@carhaz](https://github.com/carhaz)

## __AWSDeadlineCloud__
  - ### Features
    - Adds support for tagging jobs during job creation

## __Amazon SageMaker Service__
  - ### Features
    - Adding g7e instance support in Sagemaker Training

## __Managed integrations for AWS IoT Device Management__
  - ### Features
    - Adding support for Custom(General) Authorization in managed integrations for AWS IoT Device Management cloud connectors.

## __Partner Central Selling API__
  - ### Features
    - Releasing AWS Opportunity Snapshots for SDK release.

## __Runtime for Amazon Bedrock Data Automation__
  - ### Features
    - Add OutputConfiguration to InvokeDataAutomation input and output to support S3 output

## __Contributors__
Special thanks to the following contributors to this release: 

[@carhaz](https://github.com/carhaz)
# __2.41.23__ __2026-02-05__
## __ARC - Region switch__
  - ### Features
    - Updates documentation for ARC Region switch and provides stronger validation for Amazon Aurora Global Database execution block parameters.

## __AWS Elemental MediaLive__
  - ### Features
    - Outputs using the AV1 codec in CMAF Ingest output groups in MediaLive now have the ability to specify a target bit depth of 8 or 10.

## __AWS Glue__
  - ### Features
    - This release adds the capability to easily create custom AWS Glue connections to data sources with REST APIs.

## __AWS Resource Access Manager__
  - ### Features
    - Added ListSourceAssociations API. Allows RAM resource share owners to list source associations that determine which sources can access resources through service principal associations. Supports filtering by resource share ARN, source ID, source type, or status, with pagination.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.
    - Upgrade Jackson to 2.20.2

## __AWS Transfer Family__
  - ### Features
    - Adds support for the customer to send custom HTTP headers and configure an AS2 Connector to receive Asynchronous MDNs from their trading partner

## __Amazon Athena__
  - ### Features
    - Reduces the minimum TargetDpus to create or update capacity reservations from 24 to 4.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Support Browser profile persistence (cookies and local storage) across sessions for AgentCore Browser.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Support Browser profile persistence (cookies and local storage) across sessions for AgentCore Browser.

## __Amazon Neptune Graph__
  - ### Features
    - Minor neptune-graph documentation changes

## __Amazon WorkSpaces__
  - ### Features
    - Added support for 12 new graphics-optimized compute types - Graphics.g6 (xlarge, 2xlarge, 4xlarge, 8xlarge, 16xlarge), Graphics.gr6 (4xlarge, 8xlarge), Graphics.g6f (large, xlarge, 2xlarge, 4xlarge), and Graphics.gr6f (4xlarge).

# __2.41.22__ __2026-02-04__
## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports SRT listener mode for inputs and outputs, in addition to the existing SRT caller mode.

## __Amazon Bedrock Runtime__
  - ### Features
    - Added support for structured outputs to Converse and ConverseStream APIs.

## __Amazon Connect Cases__
  - ### Features
    - Amazon Connect Cases now supports larger, multi-line text fields with up to 4,100 characters. Administrators can use the Admin UI to select the appropriate configuration (single-line or multi-line) on a per-field basis, improving case documentation capabilities.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Update delete cluster description

## __Amazon Redshift__
  - ### Features
    - We have increased the maximum duration for a deferred maintenance window from 45 days to 60 days for Amazon Redshift provisioned clusters. This enhancement provides customers with greater flexibility in scheduling patching and maintenance activities while also maintaining security compliance.

## __Amazon WorkSpaces Web__
  - ### Features
    - Support for configuring and managing custom domain names for WorkSpaces Secure Browser portals.

## __CloudWatch Metric Publisher__
  - ### Features
    - Add `taskQueue` configuration option to `CloudWatchMetricPublisher.Builder` to allow customizing the internal executor queue. This enables high-throughput applications to use a larger queue to prevent dropped metrics.

# __2.41.21__ __2026-02-03__
## __AWS Batch__
  - ### Features
    - AWS Batch Array Job Visibility feature support. Includes new statusSummaryLastUpdatedAt for array job parent DescribeJobs responses for the last time the statusSummary was updated. Includes both statusSummary and statusSummaryLastUpdatedAt in ListJobs responses for array job parents.

## __AWS Marketplace Catalog Service__
  - ### Features
    - Adds support for Catalog API us-east-1 dualstack endpoint catalog-marketplace.us-east-1.api.aws

## __AWS Organizations__
  - ### Features
    - Updated the CloseAccount description.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Single Sign-On Admin__
  - ### Features
    - Added new Region management APIs to support multi-Region replication in IAM Identity Center.

## __Amazon DynamoDB__
  - ### Features
    - This change supports the creation of multi-account global tables. It adds two new arguments to CreateTable, GlobalTableSourceArn and GlobalTableSettingsReplicationMode. DescribeTable is also updated to include information about GlobalTableSettingsReplicationMode.

## __Amazon Kinesis__
  - ### Features
    - Adds StreamId parameter to AWS Kinesis Data Streams APIs that is reserved for future use.

## __Amazon Location Service Maps V2__
  - ### Features
    - Added support for optional style parameters in maps, including 3D terrain and 3D Buildings

# __2.41.20__ __2026-02-02__
## __AWS Multi-party Approval__
  - ### Features
    - Updates to multi-party approval (MPA) service to add support for multi-factor authentication (MFA) for voting operations.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds tagging support for AgentCore Evaluations (evaluator and online evaluation config)

## __Amazon CloudFront__
  - ### Features
    - Add OriginMTLS support to CloudFront Distribution APIs

# __2.41.19__ __2026-01-30__
## __AWS S3 AccessGrants__
  - ### Features
    - Updating S3 AccessGrants plugin version to 2.4.0
        - Contributed by: [@prime025](https://github.com/prime025)

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Connect Service__
  - ### Features
    - This release adds Estimated Wait Time support to the GetContactMetrics API for Amazon Connect.

## __Amazon QuickSight__
  - ### Features
    - Improve SessionTag usage guidelines in the GenerateEmbedURLForAnonymousUser API documentation. Update the GetIdentityContext document with the region support context.

## __Contributors__
Special thanks to the following contributors to this release: 

[@prime025](https://github.com/prime025)
# __2.41.18__ __2026-01-29__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - G7e instances feature up to 8 NVIDIA RTX PRO 6000 Blackwell Server Edition GPUs with 768 GB of memory and 5th generation Intel Xeon Scalable processors. Supporting up to 192 vCPUs, 1600 Gbps networking bandwidth with EFA, up to 2 TiB of system memory, and up to 15.2 TB of local NVMe SSD storage.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers now supports automatic scaling to and from zero instances based on game session activity. Fleets scale down to zero following a defined period of no game session activity and scale up from zero when game sessions are requested, providing an option for cost optimization.

# __2.41.17__ __2026-01-28__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds a follow source mode for audio output channel count, an AES audio frame wrapping option for MXF outputs, and an option to signal DolbyVision compatibility using the SUPPLEMENTAL-CODECS tag in HLS manifests.

## __AWS Lambda__
  - ### Features
    - We are launching ESM Metrics and logging for Kafka ESM to allow customers to monitor Kafka event processing using CloudWatch Metrics and Logs.

## __AWS MediaConnect__
  - ### Features
    - This release adds support for NDI flow sources in AWS Elemental MediaConnect. You can now send content to your MediaConnect transport streams directly from your NDI environment using the new NDI source type. Also adds support for LARGE 4X flow size, which can be used when creating CDI JPEG-XS flows.

## __AWS S3 Control__
  - ### Features
    - Adds support for the UpdateObjectEncryption API to change the server-side encryption type of objects in general purpose buckets.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release adds support for a new lambda trigger to transform federated user attributes during the authentication with external identity providers on Cognito Managed Login.

## __Amazon Connect Service__
  - ### Features
    - Adds support for filtering search results based on tags assigned to contacts.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - SearchTransitGatewayRoutes API response now includes a NextToken field, enabling pagination when retrieving large sets of transit gateway routes. Pass the returned NextToken value in subsequent requests to retrieve the next page of results.

## __Amazon S3__
  - ### Bugfixes
    - Fixed multipart uploads not propagating content-type from AsyncRequestBody when using S3AsyncClient with multipartEnabled(true). See Issue [#6607](https://github.com/aws/aws-sdk-java-v2/issues/6607)

## __Amazon Simple Storage Service__
  - ### Features
    - Adds support for the UpdateObjectEncryption API to change the server-side encryption type of objects in general purpose buckets.

# __2.41.16__ __2026-01-27__
## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive released two new features that allows customers 1) to set Output Timecode for AV1 encoder, 2) to set a Custom Epoch for CMAF Ingest and MediaPackage V2 output groups when using Pipeline Locking or Disabled Locking modes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud now supports editing job names and descriptions after submission.

## __Amazon Connect Service__
  - ### Features
    - Added support for task attachments. The StartTaskContact API now accepts file attachments, enabling customers to include files (.csv, .doc, .docx, .heic, .jfif, .jpeg, .jpg, .mov, .mp4, .pdf, .png, .ppt, .pptx, .rtf, .txt, etc.) when creating Task contacts. Supports up to 5 attachments per task.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Releasing new EC2 instances. C8gb and M8gb with highest EBS performance, M8gn with 600 Gbps network bandwidth, X8aedz and M8azn with 5GHz AMD processors, X8i with Intel Xeon 6 processors and up to 6TB memory, and Mac-m4max with Apple M4 Max chip for 25 percent faster builds.

## __Amazon SageMaker Service__
  - ### Features
    - Idle resource sharing enables teams to borrow unused compute resources in your SageMaker HyperPod cluster. This capability maximizes resource utilization by allowing teams to borrow idle compute capacity beyond their allocated compute quotas.

# __2.41.15__ __2026-01-26__
## __AWS Ground Station__
  - ### Features
    - Adds support for AWS Ground Station Telemetry.

## __Amazon CloudWatch Evidently__
  - ### Features
    - Deprecate all Evidently API for AWS CloudWatch Evidently deprecation

## __Amazon Connect Cases__
  - ### Features
    - Amazon Connect now enables you to use tag-based access controls to define who can access specific cases. You can associate tags with case templates and configure security profiles to determine which users can access cases with those tags.

## __Amazon DynamoDB Enhanced Client__
  - ### Bugfixes
    - Fixed DynamoDbEnhancedClient TableSchema::itemToMap to return a map that contains a consistent representation of null top-level (non-flattened) attributes and flattened attributes when their enclosing member is null and ignoreNulls is set to false.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - DescribeInstanceTypes API response now includes an additionalFlexibleNetworkInterfaces field, the number of interfaces attachable to an instance when using flexible Elastic Network Adapter (ENA) queues in addition to the base number specified by maximumNetworkInterfaces.

# __2.41.14__ __2026-01-23__
## __Amazon Connect Service__
  - ### Features
    - Amazon Connect now offers public APIs to programmatically configure and run automated tests for contact center experiences. Integrate testing into CICD pipelines, run multiple tests at scale, and retrieve results via API to automate validation of voice interactions and workflows.

## __Amazon DataZone__
  - ### Features
    - Added api for deleting data export configuration for a domain

## __Amazon Q Connect__
  - ### Features
    - Fixes incorrect types in the UpdateAssistantAIAgent API request, adds MESSAGE to TargetType enum, and other minor changes.

# __2.41.13__ __2026-01-22__
## __AWS Budgets__
  - ### Features
    - Add Budget FilterExpression and Metrics fields to DescribeBudgetPerformanceHistory to support more granular filtering options.

## __AWS Health APIs and Notifications__
  - ### Features
    - Updates the lower range for the maxResults request property for DescribeAffectedEntities, DescribeAffectedEntitiesForOrganization, DescribeEvents, and DescribeEventsForOrganization API request properties.

## __AWS SDK for Java v2__
  - ### Features
    - Added GraalVM reachability metadata for sso service
        - Contributed by: [@gbaso](https://github.com/gbaso)
    - Updated endpoint and partition metadata.

## __AWSMarketplace Metering__
  - ### Features
    - Customer Identifier parameter deprecation date has been removed. For new implementations, we recommend using the CustomerAWSAccountID. Your current integration will continue to work. When updating your implementation, consider migrating to CustomerAWSAccountID for improved integration.

## __Amazon DynamoDB__
  - ### Features
    - Adds additional waiters to Amazon DynamoDB.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add better support for fractional GPU instances in DescribeInstanceTypes API. The new fields, logicalGpuCount, gpuPartitionSize, and workload array enable better GPU resource selection and filtering for both full and fractional GPU instance types.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers Realtime now supports Node.js 24.x runtime on the Amazon Linux 2023 operating system.

## __Amazon GuardDuty__
  - ### Features
    - Adding new enum value for ScanStatusReason

## __Amazon Verified Permissions__
  - ### Features
    - Adding documentation to user guide and API documentation for how customers can create new encrypted policy stores by passing in their customer managed key during policy store creation.

## __Auto Scaling__
  - ### Features
    - This release adds support for Amazon EC2 Auto Scaling group deletion protection

## __Contributors__
Special thanks to the following contributors to this release: 

[@gbaso](https://github.com/gbaso)
# __2.41.12__ __2026-01-21__
## __AWS Config__
  - ### Features
    - AWS Config Conformance Packs now support tag-on-create through PutConformancePack API.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SDK for Java v2 Bundle__
  - ### Features
    - Include `aws-lakeformation-accessgrants-java-plugin` in the bundle.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Supports custom browser extensions for AgentCore Browser and increased message payloads up to 100KB per message in an Event for AgentCore Memory

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added support of multiple EBS cards. New EbsCardIndex parameter enables attaching volumes to specific EBS cards on supported instance types for improved storage performance.

## __Amazon QuickSight__
  - ### Features
    - Added documentation and model for sheet layout groups - allows sheet elements to be grouped, Added documentation and the feature enables admins to have granular control over connectors under actions, Updated API documentation for PDF Export in Snapshot Export APIs

## __Apache5 HTTP Client__
  - ### Features
    - Update `httpclient5` to 5.6 and `httpcore5` to 5.4.

# __2.41.11__ __2026-01-20__
## __AWS SDK for Java v2__
  - ### Features
    - Make `Apache5HttpClient` the preferred default HTTP client for sync SDK clients. This means that when `apache5-client` is on the classpath, and an HTTP client is *not* explicitly configured on client builder, the SDK client will use `Apache5HttpClient`.
    - Updated endpoint and partition metadata.

## __AWS STS__
  - ### Bugfixes
    - Fix `StsWebIdentityTokenFileCredentialsProvider` not respecting custom `prefetchTime` and `staleTime` configurations.

## __Amazon Bedrock Runtime__
  - ### Features
    - Added support for extended prompt caching with one hour TTL.

## __Amazon Keyspaces__
  - ### Features
    - Adds support for managing table pre-warming in Amazon Keyspaces (for Apache Cassandra)

## __Amazon Verified Permissions__
  - ### Features
    - Amazon Verified Permissions now supports encryption of resources by a customer managed KMS key. Customers can now create new encrypted policy stores by passing in their customer managed key during policy store creation.

## __Amazon Workspaces Instances__
  - ### Features
    - Added billing configuration support for WorkSpaces Instances with monthly and hourly billing modes, including new filtering capabilities for instance type searches.

## __Auto Scaling__
  - ### Features
    - This release adds support for three new filters when describing scaling activities, StartTimeLowerBound, StartTimeUpperBound, and Status.

## __odb__
  - ### Features
    - Adds support for associating and disassociating IAM roles with Autonomous VM cluster resources through the AssociateIamRoleToResource and DisassociateIamRoleFromResource APIs. The GetCloudAutonomousVmCluster and ListCloudAutonomousVmClusters API responses now include the iamRoles field.

# __2.41.10__ __2026-01-16__
## __AWS Launch Wizard__
  - ### Features
    - Added UpdateDeployment, ListDeploymentPatternVersions and GetDeploymentPatternVersion APIs for Launch Wizard

## __AWS Resource Explorer__
  - ### Features
    - Added ViewName to View-related responses and ServiceViewName to GetServiceView response.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Connect Service__
  - ### Features
    - Adds support to allow customers to create form with Dispute configuration

## __Amazon DataZone__
  - ### Features
    - This release adds support for numeric filtering and complex free-text searches cases for the Search and SearchListings APIs.

## __Amazon Glacier__
  - ### Features
    - Documentation updates for Amazon Glacier's maintenance mode

## __Amazon SageMaker Service__
  - ### Features
    - Adding security consideration comments for lcc accessing execution role under root access

# __2.41.9__ __2026-01-15__
## __AWS Clean Rooms Service__
  - ### Features
    - This release adds support for parameters in PySpark analysis templates.

## __AWS Lake Formation__
  - ### Features
    - API Changes for GTCForLocation feature. Includes a new API, GetTemporaryDataLocationCredentials and updates to the APIs RegisterResource and UpdateResource

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Documentations
    - Fix typo in Javadoc snippet of `AsyncRequestBody.forBlockingOutputStream`
        - Contributed by: [@KENNYSOFT](https://github.com/KENNYSOFT)

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud now supports tagging Budget resources with ABAC for permissions management and selecting up to 16 filter values in the monitor and Search API.

## __Amazon EC2 Container Service__
  - ### Features
    - Adds support for configuring FIPS in AWS GovCloud (US) Regions via a new ECS Capacity Provider field fipsEnabled. When enabled, instances launched by the capacity provider will use a FIPS-140 enabled AMI. Instances will use FIPS-140 compliant cryptographic modules and AWS FIPS endpoints.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release includes documentation updates to support up to four Elastic Volume modifications per Amazon EBS volume within a rolling 24-hour period.

## __Amazon Elastic VMware Service__
  - ### Features
    - A new GetVersions API has been added to retrieve VCF, ESX versions, and EC2 instances provided by Amazon EVS. The CreateEnvironment API now allows you to select a VCF version and the CreateEnvironmentHost API introduces a optional esxVersion parameter.

## __Amazon Q Connect__
  - ### Features
    - Fix inference configuration shapes for the CreateAIPrompt and UpdateAIPrompt APIs, Modify Text Length Limit for SendMessage API

## __OpenSearch Service Serverless__
  - ### Features
    - Collection groups in Amazon OpenSearch Serverless enables to organize multiple collections and enable compute resource sharing across collections with different KMS keys. This shared compute model reduces costs by eliminating the need for separate OpenSearch Compute Units (OCUs) for each KMS key.

## __Contributors__
Special thanks to the following contributors to this release: 

[@KENNYSOFT](https://github.com/KENNYSOFT)
# __2.41.8__ __2026-01-14__
## __AWS Cost Explorer Service__
  - ### Features
    - Cost Categories added support to BillingView data filter expressions through the new costCategories parameter, enabling users to filter billing views by AWS Cost Categories for more granular cost management and allocation.

## __AWS End User Messaging Social__
  - ### Features
    - This release clarifies WhatsApp template operations as a resource-authenticated operation via the parent WhatsApp Business Account. It also introduces new parameters for parameter format, CTA URL link tracking, and template body examples, and increases the phone number ID length.

## __AWS SDK for Java v2__
  - ### Features
    - Don't generate the unused files for the service endpoint provider when compiled endpoint rules are enabled (the default behavior). This lowers the overall size of the built JAR.
    - Updated endpoint and partition metadata.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect makes it easier to manage contact center operating hours by enabling automated scheduling for recurring events like holidays and maintenance windows. Set up recurring patterns (weekly, monthly, etc.) or link to another hours of operation to inherit overrides.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support for BOTTLEROCKET NVIDIA FIPS AMIs to AMI types in US regions.

## __Amazon Redshift__
  - ### Features
    - Adds support for enabling extra compute resources for automatic optimization during create and modify operations in Amazon Redshift clusters.

## __Amazon Relational Database Service__
  - ### Features
    - no feature changes. model migrated to Smithy

## __CloudWatch Metric Publisher__
  - ### Features
    - Optimize metric processing by replacing stream-based operations with direct iteration to reduce allocations and GC pressure.

## __Redshift Serverless__
  - ### Features
    - Adds support for enabling extra compute resources for automatic optimization during create and update operations in Amazon Redshift Serverless workgroups.

# __2.41.7__ __2026-01-13__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix an issue where `StackOverflowError` can occur when iterating over large pages from an async paginator. This can manifest as the publisher hanging/never reaching the end of the stream. Fixes [#6411](https://github.com/aws/aws-sdk-java-v2/issues/6411).

## __Amazon Bedrock__
  - ### Features
    - This change will increase TestCase guardContent input size from 1024 to 2028 characters and PolicyBuildDocumentDescription from 2000 to 4000 characters

## __Amazon DataZone__
  - ### Features
    - Adds support for IAM role subscriptions to Glue table listings via CreateSubscriptionRequest API. Also adds owningIamPrincipalArn filter to List APIs and subscriptionGrantCreationMode parameter to subscription target APIs for controlling grant creation behavior.

## __DynamoDB Enhanced Client__
  - ### Features
    - modify VersionedRecordExtension to support updating existing records with version=0 using OR condition

  - ### Bugfixes
    - Allow new records to be initialized with version=0 by supporting startAt=-1 in VersionedRecordExtension

# __2.41.6__ __2026-01-12__
## __AWS Billing__
  - ### Features
    - Cost Categories filtering support to BillingView data filter expressions through the new costCategories parameter, enabling users to filter billing views by AWS Cost Categories for more granular cost management and allocation.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon SageMaker Service__
  - ### Features
    - Added ultraServerType to the UltraServerInfo structure to support server type identification for SageMaker HyperPod

## __IAM Policy Builder__
  - ### Bugfixes
    - Allow integer AWS account IDs and boolean values when reading IAM policies from JSON with `IamPolicyReader`.

## __Managed integrations for AWS IoT Device Management__
  - ### Features
    - This release introduces WiFi Simple Setup (WSS) enabling device provisioning via barcode scanning with automated network discovery, authentication, and credential provisioning. Additionally, it introduces 2P Device Capability Rediscovery for updating hub-managed device capabilities post-onboarding.

# __2.41.5__ __2026-01-09__
## __AWS Elemental MediaLive__
  - ### Features
    - MediaPackage v2 output groups in MediaLive can now accept one additional destination for single pipeline channels and up to two additional destinations for standard channels. MediaPackage v2 destinations now support sending to cross region MediaPackage channels.

## __AWS Glue__
  - ### Features
    - Adding MaterializedViews task run APIs

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix a race condition in aggregate ProfileFileSupplier that could cause credential resolution failures with shared DefaultCredentialsProvider.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds optional field "view" to GetMemory API input to give customers control over whether CMK encrypted data such as strategy decryption or override prompts is returned or not.

## __Amazon CloudFront__
  - ### Features
    - Added EntityLimitExceeded exception handling to the following API operations AssociateDistributionWebACL, AssociateDistributionTenantWebACL, UpdateDistributionWithStagingConfig

## __Amazon Transcribe Service__
  - ### Features
    - Adds waiters to Amazon Transcribe.

# __2.41.4__ __2026-01-07__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon WorkSpaces__
  - ### Features
    - Add StateMessage and ProgressPercentage fields to DescribeCustomWorkspaceImageImport API response.

# __2.41.3__ __2026-01-06__
## __AWS Cost Explorer Service__
  - ### Features
    - This release updates existing reservation recommendations API to support deployment model.

## __EMR Serverless__
  - ### Features
    - Added support for enabling disk encryption using customer managed AWS KMS keys to CreateApplication, UpdateApplication and StartJobRun APIs.

# __2.41.2__ __2026-01-05__
## __AWS Clean Rooms ML__
  - ### Features
    - AWS Clean Rooms ML now supports advanced Spark configurations to optimize SQL performance when creating an MLInputChannel or an audience generation job.

# __2.41.1__ __2026-01-02__
## __AWS Clean Rooms Service__
  - ### Features
    - Added support for publishing detailed metrics to CloudWatch for operational monitoring of collaborations, including query performance and resource utilization.

## __AWS SSO Identity Store__
  - ### Features
    - This change introduces "Roles" attribute for User entities supported by AWS Identity Store SDK.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Handles the edge case in Netty client where HTTP/2 stream gets cleaned up before metrics collection completes, causing NPE to be thrown. See [#6561](https://github.com/aws/aws-sdk-java-v2/issues/6561).

# __2.41.0__ __2025-12-30__
## __AWS SDK for Java V2__
  - ### Bugfixes
    - Ensure rpc 1.0/1.1 error code parsing matches smithy spec: use both __type and code fields and handle uris in body error codes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Don't use the value of AwsQueryError in json rpc/smithy-rpc-v2-cbor protocols.

## __Amazon Connect Service__
  - ### Features
    - Adds support for searching global contacts using the ActiveRegions filter, and pagination support for ListSecurityProfileFlowModules and ListEntitySecurityProfiles.

## __Apache5 HTTP Client__
  - ### Features
    - The Apache5 HTTP Client (`apache5-client`) is out of preview and now generally available.

## __Lambda Maven Archetype__
  - ### Features
    - Various Java Lambda Maven archetype improvements: use Java 25, use platform specific AWS CRT dependency, bump dependency version, and improve README. See [#6115](https://github.com/aws/aws-sdk-java-v2/issues/6115)

## __Managed Streaming for Kafka Connect__
  - ### Features
    - This change sets the KafkaConnect GovCloud FIPS and FIPS DualStack endpoints to use kafkaconnect instead of kafkaconnect-fips as the service name. This is done to match the Kafka endpoints.


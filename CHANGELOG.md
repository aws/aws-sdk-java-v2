 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.26.30__ __2024-08-05__
## __AWS Performance Insights__
  - ### Features
    - Added a description for the Dimension db.sql.tokenized_id on the DimensionGroup data type page.

## __AWS SDK for Java v2__
  - ### Features
    - Adds create() method for ContainerCredentialsProvider
    - Updated endpoint and partition metadata.

## __Amazon DataZone__
  - ### Features
    - This releases Data Product feature. Data Products allow grouping data assets into cohesive, self-contained units for ease of publishing for data producers, and ease of finding and accessing for data consumers.

## __Amazon EC2 Container Registry__
  - ### Features
    - Released two new APIs along with documentation updates. The GetAccountSetting API is used to view the current basic scan type version setting for your registry, while the PutAccountSetting API is used to update the basic scan type version for your registry.

## __Amazon Kinesis Video WebRTC Storage__
  - ### Features
    - Add JoinStorageSessionAsViewer API

# __2.26.29__ __2024-08-02__
## __AWS CRT-based S3 Client__
  - ### Bugfixes
    - Fixed a race condition in AWS CRT-based S3 client that could cause `s3metaRequest is not initialized yet` error to be thrown.

## __AWS Glue__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Resilience Hub__
  - ### Features
    - Customers are presented with the grouping recommendations and can determine if the recommendations are accurate and apply to their case. This feature simplifies onboarding by organizing resources into appropriate AppComponents.

## __AWS WAF Regional__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon CloudWatch__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon EC2 Container Service__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Interactive Video Service__
  - ### Features
    - updates cloudtrail event source for SDKs

## __Amazon Interactive Video Service Chat__
  - ### Features
    - updates cloudtrail event source for SDKs

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - updates cloudtrail event source for SDKs

## __Amazon Kinesis__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Route 53__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

# __2.26.28__ __2024-08-01__
## __AWS Control Catalog__
  - ### Features
    - AWS Control Tower provides two new public APIs controlcatalog:ListControls and controlcatalog:GetControl under controlcatalog service namespace, which enable customers to programmatically retrieve control metadata of available controls.

## __AWS Control Tower__
  - ### Features
    - Updated Control Tower service documentation for controlcatalog control ARN support with existing Control Tower public APIs

## __AWS Identity and Access Management__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed an issue in the SDK that caused `UncheckedIOException` to be thrown instead of `ApiCallTimeoutException`/`ApiCallAttemptTimeoutException` when API call/API call attempt timeout is breached.

## __AWS Support__
  - ### Features
    - Doc only updates to CaseDetails

## __AWS Systems Manager QuickSetup__
  - ### Features
    - This release adds API support for the QuickSetup feature of AWS Systems Manager

## __Amazon Bedrock__
  - ### Features
    - API and Documentation for Bedrock Model Copy feature. This feature lets you share and copy a custom model from one region to another or one account to another.

## __Amazon MemoryDB__
  - ### Features
    - Doc only update for changes to deletion API.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for specifying optional MinACU parameter in CreateDBShardGroup and ModifyDBShardGroup API. DBShardGroup response will contain MinACU if specified.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds support for Amazon EMR Serverless applications in SageMaker Studio for running data processing jobs.

# __2.26.27__ __2024-07-30__
## __AWS CodePipeline__
  - ### Features
    - AWS CodePipeline V2 type pipelines now support stage level conditions to enable development teams to safely release changes that meet quality and compliance requirements.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue in the SDK that caused a generic `RuntimeException` to be thrown instead of `ApiCallTimeoutException`/`ApiCallAttemptTimeoutException` when API call/API call attempt timeout is breached.

## __AWS Telco Network Builder__
  - ### Features
    - This release adds Network Service Update, through which customers will be able to update their instantiated networks to a new network package. See the documentation for limitations. The release also enhances the Get network operation API to return parameter overrides used during the operation.

## __Amazon AppStream__
  - ### Features
    - Added support for Red Hat Enterprise Linux 8 on Amazon AppStream 2.0

## __Amazon CloudWatch Logs__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon ElastiCache__
  - ### Features
    - Doc only update for changes to deletion API.

## __Amazon EventBridge__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Lex Model Building V2__
  - ### Features
    - This release adds new capabilities to the AMAZON.QnAIntent: Custom prompting, Guardrails integration and ExactResponse support for Bedrock Knowledge Base.

## __Amazon WorkSpaces__
  - ### Features
    - Removing multi-session as it isn't supported for pools

## __Auto Scaling__
  - ### Features
    - Increase the length limit for VPCZoneIdentifier from 2047 to 5000

## __Elastic Load Balancing__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __IAM Roles Anywhere__
  - ### Features
    - IAM RolesAnywhere now supports custom role session name on the CreateSession. This release adds the acceptRoleSessionName option to a profile to control whether a role session name will be accepted in a session request with a given profile.

# __2.26.26__ __2024-07-29__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon ElastiCache__
  - ### Features
    - Renaming full service name as it appears in developer documentation.

## __Amazon MemoryDB__
  - ### Features
    - Renaming full service name as it appears in developer documentation.

# __2.26.25__ __2024-07-25__
## __AWS CodeCommit__
  - ### Features
    - CreateRepository API now throws OperationNotAllowedException when the account has been restricted from creating a repository.

## __AWS Network Firewall__
  - ### Features
    - You can now log events that are related to TLS inspection, in addition to the existing alert and flow logging.

## __AWS Outposts__
  - ### Features
    - Adding default vCPU information to GetOutpostSupportedInstanceTypes and GetOutpostInstanceTypes responses

## __AWS SDK for Java v2__
  - ### Features
    - Adds support for Map type to JmesPathRuntime

## __AWS Step Functions__
  - ### Features
    - This release adds support to customer managed KMS key encryption in AWS Step Functions.

## __Amazon Bedrock Runtime__
  - ### Features
    - Provides ServiceUnavailableException error message

## __Amazon CloudWatch Application Signals__
  - ### Features
    - CloudWatch Application Signals now supports application logs correlation with traces and operational health metrics of applications running on EC2 instances. Users can view the most relevant telemetry to troubleshoot application health anomalies such as spikes in latency, errors, and availability.

## __Amazon DataZone__
  - ### Features
    - Introduces GetEnvironmentCredentials operation to SDK

## __Amazon EC2 Container Registry__
  - ### Features
    - API and documentation updates for Amazon ECR, adding support for creating, updating, describing and deleting ECR Repository Creation Template.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - EC2 Fleet now supports using custom identifiers to reference Amazon Machine Images (AMI) in launch requests that are configured to choose from a diversified list of instance types.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - This release adds support for EKS cluster to manage extended support.

## __Application Auto Scaling__
  - ### Features
    - Application Auto Scaling is now more responsive to the changes in demand of your SageMaker Inference endpoints. To get started, create or update a Target Tracking policy based on High Resolution CloudWatch metrics.

## __DynamoDB Enhanced Client__
  - ### Features
    - Adding support for Select in QueryEnhancedRequest
        - Contributed by: [@shetsa-amzn](https://github.com/shetsa-amzn)

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for sharing trust stores across accounts and organizations through integration with AWS Resource Access Manager.

## __Contributors__
Special thanks to the following contributors to this release: 

[@shetsa-amzn](https://github.com/shetsa-amzn)
# __2.26.24__ __2024-07-24__
## __AWS Clean Rooms Service__
  - ### Features
    - Three enhancements to the AWS Clean Rooms: Disallowed Output Columns, Flexible Result Receivers, SQL as a Seed

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for Irdeto DRM encryption in DASH manifests.

## __AWS Health Imaging__
  - ### Features
    - CopyImageSet API adds copying selected instances between image sets, and overriding inconsistent metadata with a force parameter. UpdateImageSetMetadata API enables reverting to prior versions; updates to Study, Series, and SOP Instance UIDs; and updates to private elements, with a force parameter.

## __AWS IoT SiteWise__
  - ### Features
    - Adds support for creating SiteWise Edge gateways that run on a Siemens Industrial Edge Device.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed the issue where request-level signer override provided through ExecutionInterceptor is not honored.

## __Amazon DynamoDB__
  - ### Features
    - DynamoDB doc only update for July

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - Update for rebrand to AWS End User Messaging SMS and Voice.

# __2.26.23__ __2024-07-23__
## __AWS AppSync__
  - ### Features
    - Adding support for paginators in AppSync list APIs

## __AWS Clean Rooms ML__
  - ### Features
    - Adds SQL query as the source of seed audience for audience generation job.

## __AWS Clean Rooms Service__
  - ### Features
    - This release adds AWS Entity Resolution integration to associate ID namespaces & ID mapping workflow resources as part of ID namespace association and ID mapping table in AWS Clean Rooms. It also introduces a new ID_MAPPING_TABLE analysis rule to manage the protection on ID mapping table.

## __AWS EntityResolution__
  - ### Features
    - Support First Party ID Mapping

## __Amazon Connect Contact Lens__
  - ### Features
    - Added PostContactSummary segment type on ListRealTimeContactAnalysisSegments API

## __Amazon Connect Service__
  - ### Features
    - Added PostContactSummary segment type on ListRealTimeContactAnalysisSegmentsV2 API

## __Amazon DataZone__
  - ### Features
    - This release removes the deprecated dataProductItem field from Search API output.

# __2.26.22__ __2024-07-22__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon DataZone__
  - ### Features
    - This release adds 1/ support of register S3 locations of assets in AWS Lake Formation hybrid access mode for DefaultDataLake blueprint. 2/ support of CRUD operations for Asset Filters.

## __Amazon Interactive Video Service__
  - ### Features
    - Documentation update for IVS Low Latency API Reference.

## __Amazon Neptune Graph__
  - ### Features
    - Amazon Neptune Analytics provides new options for customers to start with smaller graphs at a lower cost. CreateGraph, CreaateGraphImportTask, UpdateGraph and StartImportTask APIs will now allow 32 and 64 for `provisioned-memory`

## __Redshift Serverless__
  - ### Features
    - Adds dualstack support for Redshift Serverless workgroup.

# __2.26.21__ __2024-07-18__
## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Fix broken waiters for the acm-pca client. Waiters broke in version 1.13.144 of the Boto3 SDK.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports the SRT protocol via the new SRT Caller input type.

## __AWS SDK for Java v2__
  - ### Features
    - Make Waiters use the new Backoff Strategy
    - The partitions.json that ships with the SDK can now be overridden using one of the following means (in priority order):
      1. Specify a file path using the system property `aws.partitionsFile`
      2. Specify a file path using the environment variable `AWS_PARTITIONS_FILE`
      3. Add a resource to the classpath under the name `software/amazon/awssdk/global/partitions.json`
    - Updated endpoint and partition metadata.

## __AWS Secrets Manager__
  - ### Features
    - Doc only update for Secrets Manager

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect expands search API coverage for additional resources. Search for hierarchy groups by name, ID, tag, or other criteria (new endpoint). Search for agent statuses by name, ID, tag, or other criteria (new endpoint). Search for users by their assigned proficiencies (enhanced endpoint)

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon VPC IP Address Manager (IPAM) now supports Bring-Your-Own-IP (BYOIP) for IP addresses registered with any Internet Registry. This feature uses DNS TXT records to validate ownership of a public IP address range.

## __Amazon Interactive Video Service Chat__
  - ### Features
    - Documentation update for IVS Chat API Reference.

## __Amazon Kinesis Firehose__
  - ### Features
    - This release 1) Add configurable buffering hints for Snowflake as destination. 2) Add ReadFromTimestamp for MSK As Source. Firehose will start reading data from MSK Cluster using offset associated with this timestamp. 3) Gated public beta release to add Apache Iceberg tables as destination.

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation to specify an eventual consistency model for DescribePendingMaintenanceActions.

## __Amazon SageMaker Service__
  - ### Features
    - SageMaker Training supports R5, T3 and R5D instances family. And SageMaker Processing supports G5 and R5D instances family.

## __Amazon Timestream Query__
  - ### Features
    - Doc-only update for TimestreamQuery. Added guidance about the accepted valid value for the QueryPricingModel parameter.

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Documentation update for WorkSpaces Thin Client.

## __Apache HTTP Client__
  - ### Bugfixes
    - Added fix to handle TLS half-close scenarios by throwing an exception. In TLS 1.3, the inbound and outbound close_notify alerts are independent. When the client receives a close_notify alert, it only closes the inbound stream but continues to send data to the server. Previously, the SDK could not detect that the connection was closed on the server side, causing it to get stuck while writing to the socket and eventually timing out. With this bug fix, the SDK will now detect the closed connection and throw an appropriate exception, preventing client hangs and improving overall reliability.

## __Tax Settings__
  - ### Features
    - Set default endpoint for aws partition. Requests from all regions in aws partition will be forward to us-east-1 endpoint.

# __2.26.20__ __2024-07-12__
## __AWS ARC - Zonal Shift__
  - ### Features
    - Adds the option to subscribe to get notifications when a zonal autoshift occurs in a region.

## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Minor refactoring of C2J model for AWS Private CA

## __AWS CodeBuild__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Global Accelerator__
  - ### Features
    - This feature adds exceptions to the Customer API to avoid throwing Internal Service errors

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Do not serialize empty lists for the EC2 variant of the Query protocol. The service returns exceptions if it gets query parameters with no values.
    - fix SigV4a signer incorrectly interpreting query params with '&'
    - prevent defaultRetryMode in customization.config from not being taken into account

## __Amazon DynamoDB__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Pinpoint__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon QuickSight__
  - ### Features
    - Vega ally control options and Support for Reviewed Answers in Topics

## __Amazon Relational Database Service__
  - ### Features
    - Update path for CreateDBCluster resource identifier, and Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Simple Notification Service__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Apache HTTP Client__
  - ### Bugfixes
    - Added fix to handle TLS half-close scenarios by throwing an exception. In TLS 1.3, the inbound and outbound close_notify alerts are independent. When the client receives a close_notify alert, it only closes the inbound stream but continues to send data to the server. Previously, the SDK could not detect that the connection was closed on the server side, causing it to get stuck while writing to the socket and eventually timing out. With this bug fix, the SDK will now detect the closed connection and throw an appropriate exception, preventing client hangs and improving overall reliability.

## __Auto Scaling__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

# __2.26.19__ __2024-07-10__
## __AWS Batch__
  - ### Features
    - This feature allows AWS Batch Jobs with EKS container orchestration type to be run as Multi-Node Parallel Jobs.

## __AWS Glue__
  - ### Features
    - Add recipe step support for recipe node

## __AWS Ground Station__
  - ### Features
    - Documentation update specifying OEM ephemeris units of measurement

## __AWS License Manager Linux Subscriptions__
  - ### Features
    - Add support for third party subscription providers, starting with RHEL subscriptions through Red Hat Subscription Manager (RHSM). Additionally, add support for tagging subscription provider resources, and detect when an instance has more than one Linux subscription and notify the customer.

## __AWS MediaConnect__
  - ### Features
    - AWS Elemental MediaConnect introduces the ability to disable outputs. Disabling an output allows you to keep the output attached to the flow, but stop streaming to the output destination. A disabled output does not incur data transfer costs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - Introduces new data sources and chunking strategies for Knowledge bases, advanced parsing logic using FMs, session summary generation, and code interpretation (preview) for Claude V3 Sonnet and Haiku models. Also introduces Prompt Flows (preview) to link prompts, foundational models, and resources.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Introduces query decomposition, enhanced Agents integration with Knowledge bases, session summary generation, and code interpretation (preview) for Claude V3 Sonnet and Haiku models. Also introduces Prompt Flows (preview) to link prompts, foundational models, and resources for end-to-end solutions.

## __Amazon Bedrock__
  - ### Features
    - Add support for contextual grounding check for Guardrails for Amazon Bedrock.

## __Amazon Bedrock Runtime__
  - ### Features
    - Add support for contextual grounding check and ApplyGuardrail API for Guardrails for Amazon Bedrock.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add parameters to enable provisioning IPAM BYOIPv4 space at a Local Zone Network Border Group level

# __2.26.18__ __2024-07-09__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Update way we build retry strategies to honor the `AWS_MAX_ATTEMPTS` system setting.

## __Amazon DataZone__
  - ### Features
    - This release deprecates dataProductItem field from SearchInventoryResultItem, along with some unused DataProduct shapes

## __Amazon FSx__
  - ### Features
    - Adds support for FSx for NetApp ONTAP 2nd Generation file systems, and FSx for OpenZFS Single AZ HA file systems.

## __Amazon OpenSearch Service__
  - ### Features
    - This release adds support for enabling or disabling Natural Language Query Processing feature for Amazon OpenSearch Service domains, and provides visibility into the current state of the setup or tear-down.

## __Amazon SageMaker Service__
  - ### Features
    - This release 1/ enables optimization jobs that allows customers to perform Ahead-of-time compilation and quantization. 2/ allows customers to control access to Amazon Q integration in SageMaker Studio. 3/ enables AdditionalModelDataSources for CreateModel action.

# __2.26.17__ __2024-07-08__
## __AWS CodeDeploy__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Database Migration Service__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Device Farm__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Elastic Beanstalk__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elasticsearch Service__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon GameLift__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Kinesis Firehose__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Route 53 Resolver__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Simple Email Service__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __QApps__
  - ### Features
    - This is a general availability (GA) release of Amazon Q Apps, a capability of Amazon Q Business. Q Apps leverages data sources your company has provided to enable users to build, share, and customize apps within your organization.

# __2.26.16__ __2024-07-05__
## __AWS Certificate Manager__
  - ### Features
    - Documentation updates, including fixes for xml formatting, broken links, and ListCertificates description.

## __AWS SDK for Java v2__
  - ### Features
    - Add implicit global region to internal endpoint resolution metadata
    - Updated endpoint and partition metadata.

## __Amazon EC2 Container Registry__
  - ### Features
    - This release for Amazon ECR makes change to bring the SDK into sync with the API.

## __Payment Cryptography Data Plane__
  - ### Features
    - Added further restrictions on logging of potentially sensitive inputs and outputs.

## __QBusiness__
  - ### Features
    - Add personalization to Q Applications. Customers can enable or disable personalization when creating or updating a Q application with the personalization configuration.

# __2.26.15__ __2024-07-03__
## __AWS Direct Connect__
  - ### Features
    - This update includes documentation for support of new native 400 GBps ports for Direct Connect.

## __AWS Organizations__
  - ### Features
    - Added a new reason under ConstraintViolationException in RegisterDelegatedAdministrator API to prevent registering suspended accounts as delegated administrator of a service.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Rekognition__
  - ### Features
    - This release adds support for tagging projects and datasets with the CreateProject and CreateDataset APIs.

## __Amazon WorkSpaces__
  - ### Features
    - Fix create workspace bundle RootStorage/UserStorage to accept non null values

## __Application Auto Scaling__
  - ### Features
    - Doc only update for Application Auto Scaling that fixes resource name.

# __2.26.14__ __2024-07-02__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for Elastic Compute Cloud (EC2).

## __Amazon Simple Storage Service__
  - ### Features
    - Added response overrides to Head Object requests.

## __Firewall Management Service__
  - ### Features
    - Increases Customer API's ManagedServiceData length

# __2.26.13__ __2024-07-01__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - The retry strategies implementation was not backwards compatible with the retry policies in regards of throttled exceptions, for these the retry policies had a different backoff strategy that is much more slower. This change retrofits the retry strategies to have also a different backoff strategy for throttling errors that has the same base and max delay values as the legacy retry policy.

## __AWS Step Functions__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS WAFV2__
  - ### Features
    - JSON body inspection: Update documentation to clarify that JSON parsing doesn't include full validation.

## __Amazon API Gateway__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Cognito Identity__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Connect Service__
  - ### Features
    - Authentication profiles are Amazon Connect resources (in gated preview) that allow you to configure authentication settings for users in your contact center. This release adds support for new ListAuthenticationProfiles, DescribeAuthenticationProfile and UpdateAuthenticationProfile APIs.

## __Amazon DocumentDB with MongoDB compatibility__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Updates EKS managed node groups to support EC2 Capacity Blocks for ML

## __Amazon Simple Workflow Service__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Payment Cryptography Control Plane__
  - ### Features
    - Added further restrictions on logging of potentially sensitive inputs and outputs.

## __Payment Cryptography Data Plane__
  - ### Features
    - Adding support for dynamic keys for encrypt, decrypt, re-encrypt and translate pin functions. With this change, customers can use one-time TR-31 keys directly in dataplane operations without the need to first import them into the service.

# __2.26.12__ __2024-06-28__
## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Added CCPC_LEVEL_1_OR_HIGHER KeyStorageSecurityStandard and SM2 KeyAlgorithm and SM3WITHSM2 SigningAlgorithm for China regions.

## __AWS CloudHSM V2__
  - ### Features
    - Added 3 new APIs to support backup sharing: GetResourcePolicy, PutResourcePolicy, and DeleteResourcePolicy. Added BackupArn to the output of the DescribeBackups API. Added support for BackupArn in the CreateCluster API.

## __AWS Glue__
  - ### Features
    - Added AttributesToGet parameter to Glue GetDatabases, allowing caller to limit output to include only the database name.

## __AWS Performance Insights__
  - ### Features
    - Noting that the filter db.sql.db_id isn't available for RDS for SQL Server DB instances.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix a bug on the token bucket, after success we need to deposit back one token to allow it to slowly recover and allow more retries after seeing several successful responses.

## __Amazon Connect Service__
  - ### Features
    - This release supports showing PreferredAgentRouting step via DescribeContact API.

## __Amazon EMR__
  - ### Features
    - This release provides the support for new allocation strategies i.e. CAPACITY_OPTIMIZED_PRIORITIZED for Spot and PRIORITIZED for On-Demand by taking input of priority value for each instance type for instance fleet clusters.

## __Amazon Kinesis Analytics__
  - ### Features
    - Support for Flink 1.19 in Managed Service for Apache Flink

## __Amazon OpenSearch Service__
  - ### Features
    - This release removes support for enabling or disabling Natural Language Query Processing feature for Amazon OpenSearch Service domains.

## __Amazon WorkSpaces__
  - ### Features
    - Added support for Red Hat Enterprise Linux 8 on Amazon WorkSpaces Personal.

# __2.26.11__ __2024-06-27__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Add a new backoff strategy that reassembles
      `EqualJitterBackoffStrategy` and is used to be behavioral backwards
      compatible with the way `RetryPolicy` behaves for the `LEGACY` retry
      mode.
    - Allows overrides of the retry strategy for Kinesis clients. Kinesis has its own RetryPolicy that would take precedence over any retry strategy making it impossible to override using a retry strategy.

## __Amazon Chime SDK Media Pipelines__
  - ### Features
    - Added Amazon Transcribe multi language identification to Chime SDK call analytics. Enabling customers sending single stream audio to generate call recordings using Chime SDK call analytics

## __Amazon CloudFront__
  - ### Features
    - Doc only update for CloudFront that fixes customer-reported issue

## __Amazon DataZone__
  - ### Features
    - This release supports the data lineage feature of business data catalog in Amazon DataZone.

## __Amazon ElastiCache__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Q Connect__
  - ### Features
    - Adds CreateContentAssociation, ListContentAssociations, GetContentAssociation, and DeleteContentAssociation APIs.

## __Amazon QuickSight__
  - ### Features
    - Adding support for Repeating Sections, Nested Filters

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation for TAZ export to S3.

## __Amazon SageMaker Service__
  - ### Features
    - Add capability for Admins to customize Studio experience for the user by showing or hiding Apps and MLTools.

## __Amazon WorkSpaces__
  - ### Features
    - Added support for WorkSpaces Pools.

## __AmazonMQ__
  - ### Features
    - This release makes the EngineVersion field optional for both broker and configuration and uses the latest available version by default. The AutoMinorVersionUpgrade field is also now optional for broker creation and defaults to 'true'.

## __Application Auto Scaling__
  - ### Features
    - Amazon WorkSpaces customers can now use Application Auto Scaling to automatically scale the number of virtual desktops in a WorkSpaces pool.

# __2.26.10__ __2024-06-26__
## __AWS Control Tower__
  - ### Features
    - Added ListLandingZoneOperations API.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - upgrade netty version to 4.1.111.Final
        - Contributed by: [@sullis](https://github.com/sullis)

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support for disabling unmanaged addons during cluster creation.

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - IVS Real-Time now offers customers the ability to upload public keys for customer vended participant tokens.

## __Amazon Kinesis Analytics__
  - ### Features
    - This release adds support for new ListApplicationOperations and DescribeApplicationOperation APIs. It adds a new configuration to enable system rollbacks, adds field ApplicationVersionCreateTimestamp for clarity and improves support for pagination for APIs.

## __Amazon OpenSearch Service__
  - ### Features
    - This release adds support for enabling or disabling Natural Language Query Processing feature for Amazon OpenSearch Service domains, and provides visibility into the current state of the setup or tear-down.

## __DynamoDB Enhanced Client__
  - ### Features
    - Adds support for specifying ReturnValue in UpdateItemEnhancedRequest
        - Contributed by: [@shetsa-amzn](https://github.com/shetsa-amzn)

## __Contributors__
Special thanks to the following contributors to this release: 

[@sullis](https://github.com/sullis), [@shetsa-amzn](https://github.com/shetsa-amzn)
# __2.26.9__ __2024-06-25__
## __AWS Network Manager__
  - ### Features
    - This is model changes & documentation update for the Asynchronous Error Reporting feature for AWS Cloud WAN. This feature allows customers to view errors that occur while their resources are being provisioned, enabling customers to fix their resources without needing external support.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release is for the launch of the new u7ib-12tb.224xlarge, R8g, c7gn.metal and mac2-m1ultra.metal instance types

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - This release adds the deviceCreationTags field to CreateEnvironment API input, UpdateEnvironment API input and GetEnvironment API output.

## __Auto Scaling__
  - ### Features
    - Doc only update for Auto Scaling's TargetTrackingMetricDataQuery

# __2.26.8__ __2024-06-24__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Implement `ApiName.equals`/`.hashCode`
        - Contributed by: [@brettkail-wk](https://github.com/brettkail-wk)

## __Amazon Bedrock Runtime__
  - ### Features
    - Increases Converse API's document name length

## __Amazon Connect Customer Profiles__
  - ### Features
    - This release includes changes to ProfileObjectType APIs, adds functionality top set and get capacity for profile object types.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Fix EC2 multi-protocol info in models.

## __Amazon S3__
  - ### Bugfixes
    - Fixes bug where empty non-final chunk is wrapped with headers and trailers during PutObject when using flexible checksums with S3AsyncClient

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Add sensitive trait to SSM IPAddress property for CloudTrail redaction

## __Amazon WorkSpaces Web__
  - ### Features
    - Added ability to enable DeepLinking functionality on a Portal via UserSettings as well as added support for IdentityProvider resource tagging.

## __QBusiness__
  - ### Features
    - Allow enable/disable Q Apps when creating/updating a Q application; Return the Q Apps enablement information when getting a Q application.

## __Contributors__
Special thanks to the following contributors to this release: 

[@brettkail-wk](https://github.com/brettkail-wk)
# __2.26.7__ __2024-06-20__
## __AWS Compute Optimizer__
  - ### Features
    - This release enables AWS Compute Optimizer to analyze and generate optimization recommendations for Amazon RDS MySQL and RDS PostgreSQL.

## __AWS Glue__
  - ### Features
    - Fix Glue paginators for Jobs, JobRuns, Triggers, Blueprints and Workflows.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Documentation updates for Security Hub

## __Amazon Bedrock Runtime__
  - ### Features
    - This release adds document support to Converse and ConverseStream APIs

## __Amazon DynamoDB__
  - ### Features
    - Doc-only update for DynamoDB. Fixed Important note in 6 Global table APIs - CreateGlobalTable, DescribeGlobalTable, DescribeGlobalTableSettings, ListGlobalTables, UpdateGlobalTable, and UpdateGlobalTableSettings.

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - IVS Real-Time now offers customers the ability to record individual stage participants to S3.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for model references in Hub service, and adds support for cross-account access of Hubs

## __CodeArtifact__
  - ### Features
    - Add support for the Cargo package format.

## __Cost Optimization Hub__
  - ### Features
    - This release enables AWS Cost Optimization Hub to show cost optimization recommendations for Amazon RDS MySQL and RDS PostgreSQL.

# __2.26.6__ __2024-06-19__
## __AWS Artifact__
  - ### Features
    - This release adds an acceptanceType field to the ReportSummary structure (used in the ListReports API response).

## __AWS Cost and Usage Report Service__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Direct Connect__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix a bug that prevented users from overriding retry strategies

## __Amazon Athena__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Elastic Transcoder__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon OpenSearch Service__
  - ### Features
    - This release enables customers to use JSON Web Tokens (JWT) for authentication on their Amazon OpenSearch Service domains.

# __2.26.5__ __2024-06-18__
## __AWS CloudTrail__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Config__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Shield__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Bedrock Runtime__
  - ### Features
    - This release adds support for using Guardrails with the Converse and ConverseStream APIs.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - This release adds support to surface async fargate customer errors from async path to customer through describe-fargate-profile API response.

## __Amazon Import/Export Snowball__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Lightsail__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Polly__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Rekognition__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon SageMaker Service__
  - ### Features
    - Launched a new feature in SageMaker to provide managed MLflow Tracking Servers for customers to track ML experiments. This release also adds a new capability of attaching additional storage to SageMaker HyperPod cluster instances.

# __2.26.4__ __2024-06-17__
## __AWS Batch__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Doc-only update that adds name constraints as an allowed extension for ImportCertificateAuthorityCertificate.

## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports global and organization GitHub webhooks

## __AWS Directory Service__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release includes support for creating I-frame only video segments for DASH trick play.

## __AWS Glue__
  - ### Features
    - This release introduces a new feature, Usage profiles. Usage profiles allow the AWS Glue admin to create different profiles for various classes of users within the account, enforcing limits and defaults for jobs and sessions.

## __AWS Key Management Service__
  - ### Features
    - Updating SDK example for KMS DeriveSharedSecret API.

## __AWS Secrets Manager__
  - ### Features
    - Doc only update for Secrets Manager

## __AWS WAF__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Elastic File System__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

# __2.26.3__ __2024-06-14__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds the ability to search for historical job records within the management console using a search box and/or via the SDK/CLI with partial string matching search on input file name.

## __Amazon DataZone__
  - ### Features
    - This release introduces a new default service blueprint for custom environment creation.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for Amazon EC2.

## __Amazon Macie 2__
  - ### Features
    - This release adds support for managing the status of automated sensitive data discovery for individual accounts in an organization, and determining whether individual S3 buckets are included in the scope of the analyses.

## __Amazon Route 53 Domains__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

# __2.26.2__ __2024-06-13__
## __AWS CloudHSM V2__
  - ### Features
    - Added support for hsm type hsm2m.medium. Added supported for creating a cluster in FIPS or NON_FIPS mode.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for CMAF ingest (DASH-IF live media ingest protocol interface 1)

## __AWS Glue__
  - ### Features
    - This release adds support for configuration of evaluation method for composite rules in Glue Data Quality rulesets.

## __AWS IoT Wireless__
  - ### Features
    - Add RoamingDeviceSNR and RoamingDeviceRSSI to Customer Metrics.

## __AWS Key Management Service__
  - ### Features
    - This feature allows customers to use their keys stored in KMS to derive a shared secret which can then be used to establish a secured channel for communication, provide proof of possession, or establish trust with other parties.

## __Amazon S3__
  - ### Bugfixes
    - Fixes bug where Md5 validation is performed for S3 PutObject even if checksum value is supplied

# __2.26.1__ __2024-06-12__
## __AWS Mainframe Modernization Application Testing__
  - ### Features
    - AWS Mainframe Modernization Application Testing is an AWS Mainframe Modernization service feature that automates functional equivalence testing for mainframe application modernization and migration to AWS, and regression testing.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Secrets Manager__
  - ### Features
    - Introducing RotationToken parameter for PutSecretValue API

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Tagging support for Traffic Mirroring FilterRule resource

## __Amazon OpenSearch Ingestion__
  - ### Features
    - SDK changes for self-managed vpc endpoint to OpenSearch ingestion pipelines.

## __Amazon Redshift__
  - ### Features
    - Updates to remove DC1 and DS2 node types.

## __Amazon Security Lake__
  - ### Features
    - This release updates request validation regex to account for non-commercial aws partitions.

## __Amazon Simple Email Service__
  - ### Features
    - This release adds support for Amazon EventBridge as an email sending events destination.

# __2.26.0__ __2024-06-11__
## __AWS Network Manager__
  - ### Features
    - This is model changes & documentation update for Service Insertion feature for AWS Cloud WAN. This feature allows insertion of AWS/3rd party security services on Cloud WAN. This allows to steer inter/intra segment traffic via security appliances and provide visibility to the route updates.

## __AWS SDK for Java v2__
  - ### Features
    - Adds the new module retries API module
        - Contributed by: [@sugmanue](https://github.com/sugmanue)
    - This release contains a major internal refactor of retries and is part
      of moving the SDK to a standardized AWS SDK architecture. It
      introduces the interface `RetryStrategy` and three subclasses
      `StandardRetryStrategy`, `LegacyRetryStrategy` , and
      `AdaptiveRetryStrategy`. The new interfaces live in the `retry-spi`
      module, and the implementation classes live in the `retries` module.

      Note 1) This change marks RetryPolicy as as deprecated and we
      encourage users to migrate to its replacement, RetryStrategy. However,
      retry policies are, and will for the foreseeable future be fully
      supported. Clients configured to use retry policies will not need any
      code changes and won’t see any behavioral change with this release.

      Note 2) The original implementation of adaptive mode (see
      [#2658](https://github.com/aws/aws-sdk-java-v2/pull/2658)) that was
      released with the retry policy API contains a bug in its rate-limiter
      logic which prevents it from remembering state across requests. In
      this release of the retry strategy API, we introduce
      `RetryMode.ADAPTIVE_V2`, which implements the correct adaptive
      behavior. `RetryMode.ADAPTIVE` is still present in order to maintain
      backwards compatibility, but is now marked as deprecated.

      Note 3) When configuring retry mode through system settings or
      environment variables, users can only choose adaptive mode. This
      setting will map to `RetryMode.ADAPTIVE_V2` instead of
      `RetryMode.ADAPTIVE` with this release, giving users the correct
      behavior and still keeping the settings consistent across all
      SDKs. The list of configuration options are: profile file `retry_mode`
      setting, the `aws.retryMode` system property and the `AWS_RETRY_MODE`
      environment variable.
        - Contributed by: [@sugmanue](https://github.com/sugmanue)
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue in async client where the future would get stuck if there is a server error and the server fails to return response body that matches with the content length specified in the response header. See [#4354](https://github.com/aws/aws-sdk-java-v2/issues/4354)

## __Access Analyzer__
  - ### Features
    - IAM Access Analyzer now provides policy recommendations to help resolve unused permissions for IAM roles and users. Additionally, IAM Access Analyzer now extends its custom policy checks to detect when IAM policies grant public access or access to critical resources ahead of deployments.

## __Amazon GuardDuty__
  - ### Features
    - Added API support for GuardDuty Malware Protection for S3.

## __Amazon SageMaker Service__
  - ### Features
    - Introduced Scope and AuthenticationRequestExtraParams to SageMaker Workforce OIDC configuration; this allows customers to modify these options for their private Workforce IdP integration. Model Registry Cross-account model package groups are discoverable.

## __Private CA Connector for SCEP__
  - ### Features
    - Connector for SCEP allows you to use a managed, cloud CA to enroll mobile devices and networking gear. SCEP is a widely-adopted protocol used by mobile device management (MDM) solutions for enrolling mobile devices. With the connector, you can use AWS Private CA with popular MDM solutions.

## __Contributors__
Special thanks to the following contributors to this release: 

[@sugmanue](https://github.com/sugmanue)

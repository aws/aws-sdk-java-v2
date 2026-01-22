 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


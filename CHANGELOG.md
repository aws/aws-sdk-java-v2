 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.32.15__ __2025-08-04__
## __AWS CodeConnections__
  - ### Features
    - New integration with Azure DevOps provider type.

## __AWS IoT SiteWise__
  - ### Features
    - Support Interface for IoT SiteWise Asset Modeling

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock AgentCore Data Plane Fronting Layer__
  - ### Features
    - Remove superfluous field from API

## __Amazon Elastic VMware Service__
  - ### Features
    - TagResource API now throws ServiceQuotaExceededException when the number of tags on the Amazon EVS resource exceeds the maximum allowed. TooManyTagsException is deprecated.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds the ability for customers to attach and detach their EBS volumes to EKS-orchestrated HyperPod cluster nodes.

# __2.32.14__ __2025-08-01__
## __ARC - Region switch__
  - ### Features
    - This is the initial SDK release for Region switch

## __AWS AI Ops__
  - ### Features
    - This release includes fix for InvestigationGroup timestamp conversion issue.

## __AWS Audit Manager__
  - ### Features
    - Added a note to Framework APIs (CreateAssessmentFramework, GetAssessmentFramework, UpdateAssessmentFramework) clarifying that the Controls object returns a partial response when called through Framework APIs. Added documentation that the Framework's controlSources parameter is no longer supported.

## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Doc-only update to add more information to GetCertificate action.

## __AWS Parallel Computing Service__
  - ### Features
    - Add support for IPv6 Networking for Clusters.

## __AWS SDK for Java v2__
  - ### Features
    - Fixed SigV4a signing to respect pre-existing Host headers to be consistent with existing SigV4 signing behavior.
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Release new resource detail type CodeRepository

## __Amazon Lightsail__
  - ### Features
    - This release adds support for the Asia Pacific (Jakarta) (ap-southeast-3) Region.

## __Amazon S3__
  - ### Bugfixes
    - Add additional validations for multipart upload operations in the Java multipart S3 client.

## __Amazon Simple Notification Service__
  - ### Features
    - Amazon SNS support for Amazon SQS fair queues

## __CloudWatch Observability Admin Service__
  - ### Features
    - CloudWatch Observability Admin adds the ability to enable telemetry on AWS resources such as Amazon VPCs (Flow Logs) in customers AWS Accounts and Organizations. The release introduces new APIs to manage telemetry rules, which define telemetry settings to be applied on AWS resources.

# __2.32.13__ __2025-07-31__
## __AWS EntityResolution__
  - ### Features
    - Add support for creating advanced rule-based matching workflows in AWS Entity Resolution.

## __AWS Glue__
  - ### Features
    - Added support for Route node, S3 Iceberg sources/targets, catalog Iceberg sources, DynamoDB ELT connector, AutoDataQuality evaluation, enhanced PII detection with redaction, Kinesis fan-out support, and new R-series worker types.

## __AWS IoT__
  - ### Features
    - This release allows AWS IoT Core users to use their own AWS KMS keys for data protection

## __AWS S3 Control__
  - ### Features
    - Add Tags field to CreateAccessPoint

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Connect Customer Profiles__
  - ### Features
    - The release updates standard profile with 2 new fields that supports account-level engagement. Updated APIs include CreateProfile, UpdateProfile, MergeProfiles, SearchProfiles, BatchGetProfile, GetSegmentMembership, CreateSegmentDefinition, CreateSegmentEstimate.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added support for the force option for the EC2 instance terminate command. This feature enables customers to recover resources associated with an instance stuck in the shutting-down state as a result of rare issues caused by a frozen operating system or an underlying hardware problem.

## __Amazon OpenSearch Service__
  - ### Features
    - Granular access control support for NEO-SAML with IAMFederation for AOS data source

## __Amazon QuickSight__
  - ### Features
    - Added Impala connector support

## __Amazon Simple Email Service__
  - ### Features
    - This release introduces support for Multi-tenant management

## __Amazon WorkSpaces Web__
  - ### Features
    - Added ability to log session activity on a portal to an S3 bucket.

## __Elastic Load Balancing__
  - ### Features
    - This release enables secondary IP addresses for Network Load Balancers.

## __Inspector2__
  - ### Features
    - Extend usage to include agentless hours and add CODE_REPOSITORY to aggregation resource type

# __2.32.12__ __2025-07-30__
## __AWS Directory Service__
  - ### Features
    - This release adds support for AWS Managed Microsoft AD Hybrid Edition, introducing new operations: StartADAssessment, DescribeADAssessment, ListADAssessments, DeleteADAssessment, CreateHybridAD, UpdateHybridAD, and DescribeHybridADUpdate; and updated existing operation: DescribeDirectories.

## __AWS IoT Wireless__
  - ### Features
    - Added TxPowerIndexMin, TxPowerIndexMax, NbTransMin and NbTransMax params to ServiceProfile.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudFront__
  - ### Features
    - This release adds new origin timeout options: 1) ResponseCompletionTimeout and 2) OriginReadTimeout (for S3 origins)

## __Amazon DocumentDB with MongoDB compatibility__
  - ### Features
    - Add support for setting Serverless Scaling Configuration on clusters.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Release to show the next hop IP address for routes propagated by VPC Route Server into VPC route tables.

# __2.32.11__ __2025-07-29__
## __AWS Batch__
  - ### Features
    - AWS Batch for SageMaker Training jobs feature support. Includes new APIs for service job submission (e.g., SubmitServiceJob) and managing service environments (e.g., CreateServiceEnvironment) that enable queueing SageMaker Training jobs.

## __AWS Clean Rooms Service__
  - ### Features
    - This feature provides the ability to update the table reference and allowed columns on an existing configured table.

## __AWS S3__
  - ### Features
    - Add support for using CRT's response file in the CRT based S3AsyncClient - CRT will directly write to the file when calling getObject with a Path.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSDeadlineCloud__
  - ### Features
    - Adds support for tag management on monitors.

## __Amazon Location Service__
  - ### Features
    - This release 1) adds support for multi-polygon geofences with disconnected territories, and 2) enables polygon exclusion zones within geofences for more accurate representation of real-world boundaries.

## __OpenSearch Service Serverless__
  - ### Features
    - This is to support Granular access control support for SAML with IAMFedraton in AOSS

# __2.32.10__ __2025-07-28__
## __AWS Direct Connect__
  - ### Features
    - Enable MACSec support and features on Interconnects.

## __AWS IoT SiteWise__
  - ### Features
    - Add support for native anomaly detection in IoT SiteWise using new Computation Model APIs

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon OpenSearch Ingestion__
  - ### Features
    - Add Pipeline Role Arn as an optional parameter to the create / update pipeline APIs as an alternative to passing in the pipeline configuration body

# __2.32.9__ __2025-07-25__
## __AWS Budgets__
  - ### Features
    - Adds IPv6 and PrivateLink support for AWS Budgets in IAD.

## __AWS Config__
  - ### Features
    - Documentation improvements have been made to the EvaluationModel and DescribeConfigurationRecorders APIs.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for specifying a preferred input for channels using CMAF ingest.

## __AWS End User Messaging Social__
  - ### Features
    - This release introduces new WhatsApp template management APIs that enable customers to programmatically create and submit templates for approval, monitor approval status, and manage the complete template lifecycle

## __AWS Key Management Service__
  - ### Features
    - Doc only update: fixed grammatical errors.

## __Amazon AppIntegrations Service__
  - ### Features
    - Amazon AppIntegrations introduces new configuration capabilities to enable customers to manage iframe permissions, control application refresh behavior (per contact or per browser/cross-contact), and run background applications (service).

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Transit Gateway native integration with AWS Network Firewall. Adding new enum value for the new Transit Gateway Attachment type.

## __Amazon Simple Queue Service__
  - ### Features
    - Documentation updates for Amazon SQS fair queues feature.

# __2.32.8__ __2025-07-24__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon DataZone__
  - ### Features
    - This release adds support for 1) highlighting relevant text in returned results for Search and SearchListings APIs and 2) returning aggregated counts of values for specified attributes for SearchListings API.

## __Amazon Omics__
  - ### Features
    - Add Git integration and README support for HealthOmics workflows

# __2.32.7__ __2025-07-23__
## __AWS Glue__
  - ### Features
    - AWS Glue now supports dynamic session policies for job executions. This feature allows you to specify custom, fine-grained permissions for each job run without creating multiple IAM roles.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added support for skip-os-shutdown option for the EC2 instance stop and terminate operations. This feature enables customers to bypass the graceful OS shutdown, supporting faster state transitions when instance data preservation isn't critical.

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Added the lastUserId parameter to the ListDevices and GetDevice API.

## __DynamoDB Enhanced Client__
  - ### Features
    - Support for Version Starting at 0 with Configurable Increment
        - Contributed by: [@akiesler](https://github.com/akiesler)

## __Contributors__
Special thanks to the following contributors to this release: 

[@akiesler](https://github.com/akiesler)
# __2.32.6__ __2025-07-22__
## __AWS Lambda__
  - ### Features
    - This release migrated the model to Smithy keeping all features unchanged.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon EMR__
  - ### Features
    - This release adds new parameter 'ExtendedSupport' in AWS EMR RunJobFlow, ModifyCluster and DescribeCluster API.

## __Amazon Elastic Container Registry__
  - ### Features
    - Add support for Image Tag Mutability Exception feature, allowing repositories to define wildcard-based patterns that override the default image tag mutability settings.

## __Amazon NeptuneData__
  - ### Features
    - This release updates the supported regions for Neptune API to include current AWS regions.

# __2.32.5__ __2025-07-21__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSDeadlineCloud__
  - ### Features
    - Add support for VPC resource endpoints in Service Managed Fleets

## __Amazon CloudFront__
  - ### Features
    - Add dualstack endpoint support

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for C8gn, F2 and P6e-GB200 Instance types

## __Amazon SageMaker Service__
  - ### Features
    - This release adds 1/ Support for S3FileSystem in CustomFileSystems 2/ The ability for a customer to select their preferred IpAddressType for use with private Workforces 3/ Support for p4de instance type in SageMaker Training Plans

## __Timestream InfluxDB__
  - ### Features
    - Timestream for InfluxDB adds support for db.influx.24xlarge instance type. This enhancement enables higher compute capacity for demanding workloads through CreateDbInstance, CreateDbCluster, UpdateDbInstance, and UpdateDbCluster APIs.

# __2.32.4__ __2025-07-18__
## __AWS Audit Manager__
  - ### Features
    - Updated error handling for RegisterOrganizationAdminAccount API to properly translate TooManyExceptions to HTTP 429 status code. This enhancement improves error handling consistency and provides clearer feedback when request limits are exceeded.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds support for TAMS server integration with MediaConvert inputs.

## __AWS Outposts__
  - ### Features
    - Add AWS Outposts API to surface customer billing information

## __AWS SDK for Java V2__
  - ### Features
    - Add support for tracking business metrics from resolved endpoints.

## __AWS SDK for Java v2__
  - ### Features
    - Convert codegen exceptions caused by bad customization config or invalid models to ModelInvalidException.
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Logs__
  - ### Features
    - CloudWatchLogs launches GetLogObject API with streaming support for efficient log data retrieval. Logs added support for new AccountPolicy type METRIC_EXTRACTION_POLICY. For more information, see CloudWatch Logs API documentation

## __Amazon Simple Email Service__
  - ### Features
    - Added IP Visibility support for managed dedicated pools. Enhanced GetDedicatedIp and GetDedicatedIps APIs to return managed IP addresses.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - July 2025 doc-only updates for Systems Manager.

# __2.32.3__ __2025-07-17__
## __AWS Clean Rooms ML__
  - ### Features
    - This release introduces Parquet result format support for ML Input Channel models in AWS Clean Rooms ML.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release expands the range of supported audio outputs to include xHE, 192khz FLAC and the deprecation of dual mono for AC3.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix generation of credentialType(bearer) for non-priority bearer operations

## __AWS Step Functions__
  - ### Features
    - Align input with style guidelines.

## __Amazon CloudFront__
  - ### Features
    - Doc only update for CloudFront that fixes some customer-reported issues

## __Amazon Elastic Compute Cloud__
  - ### Features
    - AWS Free Tier Version2 Support

## __Amazon Keyspaces Streams__
  - ### Features
    - Doc only update for the Amazon Keyspaces Streams API.

## __MailManager__
  - ### Features
    - Allow underscores in the local part of the input of the "Email recipients rewrite" action in rule sets.

## __Synthetics__
  - ### Features
    - This feature allows AWS Synthetics customers to provide code dependencies using lambda layer while creating a canary

# __2.32.2__ __2025-07-16__
## __AWS DataSync__
  - ### Features
    - AWS DataSync now supports IPv6 address inputs and outputs in create, update, and describe operations for NFS, SMB, and Object Storage locations

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for CDN Authentication using Static Headers in MediaPackage v2.

## __AWS Glue__
  - ### Features
    - AWS Glue now supports schema, partition and sort management of Apache Iceberg tables using Glue SDK

## __AWS IoT Wireless__
  - ### Features
    - FuotaTaskId is not a valid IdentifierType for EventConfiguration and is being removed from possible IdentifierType values.

## __AWS Step Functions__
  - ### Features
    - Doc-only update to introduction, and edits to clarify input parameter and the set of control characters.

## __Amazon Bedrock__
  - ### Features
    - This release adds support for on-demand custom model inference through CustomModelDeployment APIs for Amazon Bedrock.

## __Amazon Bedrock Agent Core Control Plane Fronting Layer__
  - ### Features
    - Initial release of Amazon Bedrock AgentCore SDK including Runtime, Built-In Tools, Memory, Gateway and Identity.

## __Amazon Bedrock AgentCore Data Plane Fronting Layer__
  - ### Features
    - Initial release of Amazon Bedrock AgentCore SDK including Runtime, Built-In Tools, Memory, Gateway and Identity.

## __Amazon Bedrock Runtime__
  - ### Features
    - document update to support on demand custom model.

## __Amazon CloudWatch Logs__
  - ### Features
    - CloudWatch Logs updates: Added X-Ray tracing for Amazon Bedrock Agent resources. Logs introduced Log Group level resource policies (managed through Put/Delete/Describe Resource Policy APIs). For more information, see CloudWatch Logs API documentation.

## __Amazon GuardDuty__
  - ### Features
    - Add expectedBucketOwner parameter to ThreatIntel and IPSet APIs.

## __Network Flow Monitor__
  - ### Features
    - Introducing 2 new scope status types - DEACTIVATING and DEACTIVATED.

## __Payment Cryptography Data Plane__
  - ### Features
    - Expand length of message data field for Mac generation and validation to 8192 characters.

# __2.32.1__ __2025-07-15__
## __AWS CRT Async HTTP Client__
  - ### Bugfixes
    - Fixed potential connection leak issue when SDK failed to convert the SDK request to CRT request
    - Fixed the issue where AWS CRT HTTP client was eagerly buffering data before the underlying CRT component was able to handle it

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon EC2 Container Service__
  - ### Features
    - This release removes hookDetails for the Amazon ECS native blue/green deployments.

# __2.32.0__ __2025-07-15__
## __AWS Price List Service__
  - ### Features
    - This release adds support for new filter types in GetProducts API, including EQUALS, CONTAINS, ANY_OF, and NONE_OF.

## __AWS SDK for Java v2__
  - ### Features
    - Add support for caching results to URI constructors for account-id based endpoints
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Improve error message for the error case where a request using RequestBody#fromInputStream failed to retry due to lack of mark and reset support. See [#6174](https://github.com/aws/aws-sdk-java-v2/issues/6174)
    - Improve performance for SDK service clients that don't use custom client plugins by optimizing plugin resolution logic

## __AWS re:Post Private__
  - ### Features
    - This release introduces Channels functionality with CreateChannel, GetChannel, ListChannels, and UpdateChannel operations. Channels provide dedicated collaboration spaces where teams can organize discussions and knowledge by projects, business units, or areas of responsibility.

## __Agents for Amazon Bedrock__
  - ### Features
    - Add support for S3 Vectors as a vector store.

## __Amazon DataZone__
  - ### Features
    - Removing restriction of environment profile identifier as required field, S3 feature release

## __Amazon DynamoDB__
  - ### Features
    - Enable caching results to URI constructors for account-id based endpoints

## __Amazon DynamoDB Enhanced Client__
  - ### Documentations
    - Add documentation clarifying usage for ddb enhanced client

## __Amazon DynamoDB Streams__
  - ### Features
    - Added support for optional shard filter parameter in DescribeStream api that allows customers to fetch child shards of a read_only parent shard.

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS supports native blue/green deployments, allowing you to validate new service revisions before directing production traffic to them.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for volume initialization status, which enables you to monitor when the initialization process for an EBS volume is completed. This release also adds IPv6 support to EC2 Instance Connect Endpoints, allowing you to connect to your EC2 Instance via a private IPv6 address.

## __Amazon EventBridge__
  - ### Features
    - Add customer-facing logging for the EventBridge Event Bus, enabling customers to better observe their events and extract insights about their EventBridge usage.

## __Amazon OpenSearch Service__
  - ### Features
    - AWS Opensearch adds support for enabling s3 vector engine options. After enabling this option, customers will be able to create indices with s3 vector engine.

## __Amazon QuickSight__
  - ### Features
    - Introduced custom instructions for topics.

## __Amazon S3 Tables__
  - ### Features
    - Adds table bucket type to ListTableBucket and GetTableBucket API operations

## __Amazon S3 Vectors__
  - ### Features
    - Amazon S3 Vectors provides cost-effective, elastic, and durable vector storage for queries based on semantic meaning and similarity.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds support for a new Restricted instance group type to enable a specialized environment for running Nova customization jobs on SageMaker HyperPod clusters. This release also adds support for SageMaker pipeline versioning.

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon S3 Metadata live inventory tables provide a queryable inventory of all the objects in your general purpose bucket so that you can determine the latest state of your data. To help minimize your storage costs, use journal table record expiration to set a retention period for your records.

## __Apache HTTP Client 5__
  - ### Features
    - Preview Release of AWS SDK Apache5 HttpClient with Apache HttpClient 5.5.x


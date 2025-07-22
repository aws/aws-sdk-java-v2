 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


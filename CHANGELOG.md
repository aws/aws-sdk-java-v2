 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


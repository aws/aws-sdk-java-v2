 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


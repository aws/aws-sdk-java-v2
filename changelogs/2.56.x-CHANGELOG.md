 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.56.0__ __2026-10-14__
## __ARC - Region switch__
  - ### Features
    - Features - Adds support for an optional payload to invoke the Lambda function with as part of the Custom action Lambda execution block

## __AWS Lambda__
  - ### Features
    - AWS Lambda now supports an optional X-Ray trace header for chained invocations in durable executions, enabling child invocations to be linked to the initiating span.

## __AWS Organizations__
  - ### Features
    - With dual-stack enabled, all requests now resolve to the endpoint in the partition's primary Region (for example, organizations.us-east-1.api.aws) and are signed for that Region. Previously, requests from other Regions resolved to a nonexistent endpoint. No API changes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Updated SdkUri TRACE log messages to include only the scheme, host, port and path of the URI.

## __AWS Transfer Family__
  - ### Features
    - AWS Transfer Family now lets you store SFTP server host keys in AWS Secrets Manager in your own AWS account. This gives you greater control over your server identity and helps simplify server migrations and failover.

## __AWSMarketplace Metering__
  - ### Features
    - Documentation update - added a Get started pointer to the AWS Marketplace AI Metering Agent Skill in the BatchMeterUsage API reference. No API or behavior change.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - This release supports scoping Amazon Bedrock AgentCore Gateway rate limits by request header or request body field.

## __Amazon CloudFront__
  - ### Features
    - This release adds a new "IpAddressType" field to "VPCOriginEndpointConfig", allowing customers to specify whether their VPC origin accepts IPv4-only or dualstack connections.

## __Amazon DynamoDB Streams__
  - ### Features
    - Add support for a new iterator type (AT TIMESTAMP) for DynamoDB Streams GetShardIterator API that allows reading from a specific point in time from the stream.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds T8i instance sizes to the InstanceType enum. T8i instances are low-cost burstable instances powered by custom sixth generation Intel Xeon 6 processors, available exclusively on AWS, and built on the latest sixth generation AWS Nitro cards

## __Amazon GuardDuty__
  - ### Features
    - Releasing support for Custom Detection Rule Runtime Monitoring

## __EC2 Image Builder__
  - ### Features
    - This release adds parentInstance to infrastructure configurations, so builds can use an already-running instance from an Amazon EC2 Auto Scaling group. It also adds terminateInstanceOnCompletion, which keeps build and test instances running after image creation ends.


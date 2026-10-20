 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.57.1__ __2026-10-20__
## __AWS Clean Rooms Service__
  - ### Features
    - This release allows customers and their partners to easily collaborate with data stored in Databricks, without having to move or share their underlying data among collaborators.

## __AWS Marketplace Catalog Service__
  - ### Features
    - Adds ListEntities filter, sort, and summary support for the ExpressPrivateOfferConfiguration entity type, including filtering by ProductId.

## __Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Guardrails now supports a configurable tier for the contextual grounding policy via a new tier option. Select the Standard tier for grounding that better supports conversations and reasoning, with expanded language coverage.

## __Amazon CloudWatch__
  - ### Features
    - Amazon CloudWatch now supports two new anomaly detector settings. AdaptationSpeed (FAST, NORMAL, SLOW) controls how fast the model adapts to baseline changes, and OutlierFilteringStrength (MINIMAL, MODERATE, STRICT) controls how aggressively outliers are filtered.

## __Amazon DynamoDB__
  - ### Features
    - Adds support for disassociating a replica from an Amazon DynamoDB multi-Region eventual consistency global table, so you can keep it as a single-Region table with its existing data. Also adds disassociation protection, on by default, to help prevent accidental disassociation.

## __Amazon EC2 Container Service__
  - ### Features
    - ECS Service Gateway makes it easier for developers to deploy ECS services without having to worry about complex ingress networking constructs.

## __Amazon ElastiCache__
  - ### Features
    - LiveMigration enables customers to migrate from self-hosted EC2 Redis and Valkey clusters to ElastiCache Serverless with near-zero downtime. Customers control the migration through three API-driven phases (prepare, start, complete) that handle traffic proxying, data replication, and cutover.

## __Amazon Relational Database Service__
  - ### Features
    - Amazon RDS now supports On-Demand Capacity Reservations for DB instances.

## __Amazon S3 Files__
  - ### Features
    - S3 Files adds support for the "Misconfigured" file system status.

## __Amazon Workspaces Instances__
  - ### Features
    - Documentation update release for AWS service

## __CloudWatch Omni__
  - ### Features
    - Updating max value for maxResults in SearchPrincipals API

# __2.57.0__ __2026-10-19__
## __AWS Amplify__
  - ### Features
    - Documentation update release for AWS service

## __AWS CodeCommit__
  - ### Features
    - Documentation update release for AWS service

## __AWS CodeConnections__
  - ### Features
    - Documentation update release for AWS service

## __AWS CodeDeploy__
  - ### Features
    - Adds support for suspending Amazon EC2 Auto Scaling processes during CodeDeploy EC2 in-place deployments. Use ec2DeploymentOptions to choose which processes are suspended and resumed.

## __AWS CodePipeline__
  - ### Features
    - Documentation update release for AWS service

## __AWS CodeStar Notifications__
  - ### Features
    - Documentation update release for AWS service

## __AWS Control Tower__
  - ### Features
    - Documentation update release for AWS service

## __AWS DevOps Agent Service__
  - ### Features
    - Documentation update release for AWS service

## __AWS End User Messaging Social__
  - ### Features
    - Adding support for WhatsApp Direct Send.

## __AWS Glue DataBrew__
  - ### Features
    - Documentation update release for AWS service

## __AWS IoT Wireless__
  - ### Features
    - Documentation update release for AWS service

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Improve the performance of parsing profile files with many property continuation lines.
    - Releases HTTP response resources when an execution interceptor fails during response processing, preventing connection-pool leaks in synchronous and asynchronous clients.

  - ### Removals
    - Remove the `software.amazon.eventstream:eventstream` dependency.

## __AWS X-Ray__
  - ### Features
    - Adds support for encrypting the Transaction Search spans log group with a customer managed AWS KMS key. Pass the key ARN in the new CloudWatchLogsConfiguration.SpansLogGroupKmsKeyId field of UpdateTraceSegmentDestination. X-Ray applies the key when it creates the log group.

## __Agents for Amazon Bedrock__
  - ### Features
    - Adds Retrieval Instructions for Amazon Bedrock Knowledge Bases. Create, get, update, delete, list, and version customer-authored instructions that guide how a knowledge base retrieves and answers, with content review before use.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Adds support for applying a Retrieval Instruction, optionally by version, to AgenticRetrieveStream requests for Amazon Bedrock Knowledge Bases, and reports the applied instruction and version on the trace.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds the Builtin.FailureAnalysis evaluator for batch evaluations. GetBatchEvaluation now returns insightResults that group session failures into insights with root causes and suggested fixes.

## __Amazon CloudFront__
  - ### Bugfixes
    - Fixed an issue where `CloudFrontClient` and `CloudFrontAsyncClient` had no paginator methods

## __Amazon CloudWatch__
  - ### Features
    - This release adds ActionConfiguration to PutMetricAlarm, PutCompositeAlarm and PutLogAlarm. It allows customers to include an alarm's tags in its notifications and alarm state change events

## __Amazon CloudWatch Logs__
  - ### Features
    - Amazon CloudWatch Logs now supports filtering DescribeDeliverySources by resourceArns, DescribeDeliveryDestinations by names, and DescribeDeliveries by deliverySourceNames.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect GetAttachedFile API now supports an optional UrlContentDisposition parameter. Set it to INLINE to get a pre-signed URL that displays the file in the browser, so agents can preview email attachments without downloading them. ATTACHMENT (download) remains the default.

## __Amazon DataZone__
  - ### Features
    - Adds SDK and CLI support for the Iceberg REST catalog connection types already available in Amazon SageMaker Unified Studio - ICEBERGRESTCATALOG, SNOWFLAKEICEBERGRESTCATALOG, and DATABRICKSICEBERGRESTCATALOG.

## __Amazon DynamoDB__
  - ### Features
    - Enable account endpoint generation for existing CN and GovCloud regions

## __Amazon DynamoDB Enhanced Client__
  - ### Documentations
    - Clarified in the UpdateItemEnhancedRequest Javadoc that ignoreNulls and IgnoreNullsMode don't apply to the version attribute.
        - Contributed by: [@adev-code](https://github.com/adev-code)

## __Amazon EC2 Container Service__
  - ### Features
    - Adds support for configurable envoy application log levels. Customers can now debug network connectivity issues with their task's sidecar envoys.

## __Amazon EMR Containers__
  - ### Features
    - Documentation update release for AWS service

## __Amazon GuardDuty__
  - ### Features
    - Adding a new Expected Bucket Owner optional field for GuardDuty publishing destination APIs

## __Amazon Managed Grafana__
  - ### Features
    - Documentation update release for AWS service

## __Amazon OpenSearch Service__
  - ### Features
    - Amazon OpenSearch Service adds a new TLS security policy to support FIPS policy and post-quantum cryptography

## __Amazon WorkSpaces__
  - ### Features
    - Adds optional imageValidationMode enum to ImportCustomWorkspaceImageRequest. This allows customers to opt-in to having prevalidation run in the service account instead of customer account.

## __Amazon Workspaces Instances__
  - ### Features
    - ModifyVolume is a new public API for WorkSpaces Core Managed Instances. Customers can change the size, type, IOPS, or throughput of an existing managed volume online, with no stop required. Customers track progress by calling EC2 DescribeVolumesModifications directly.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Adding support for EKS Container Insights Telemetry Rules

## __Elastic Disaster Recovery Service__
  - ### Features
    - Documentation update release for AWS service

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fixed an issue where `S3TransferManager#downloadDirectory` failed the entire directory download when an object's key mapped to a path outside the destination directory or to a path that is not valid on the local file system. Such objects are now skipped and reported in `CompletedDirectoryDownload#failedTransfers()`, and the remaining objects are still downloaded.
    - Fixed an issue where `S3TransferManager#uploadDirectory` with a custom `s3Delimiter` silently uploaded two local files to the same key when the delimiter appeared in a file or directory name, for example `a/b` and `a-b` with the delimiter `-`. Only the first file is now uploaded to that key, and each other file that maps to it is reported in `CompletedDirectoryUpload#failedTransfers()`.

## __Timestream InfluxDB__
  - ### Features
    - Adds four new parameters to the InfluxDBv3Enterprise parameter group for Amazon Timestream for InfluxDB. The new parameters are upgradePachaTree, walBufferSize, snapshotSize, and snapshotDuration.

## __Contributors__
Special thanks to the following contributors to this release: 

[@adev-code](https://github.com/adev-code)

 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.28.12__ __2024-09-30__
## __AWS Price List Service__
  - ### Features
    - Add examples for API operations in model.

## __AWS Resource Groups__
  - ### Features
    - This update includes new APIs to support application groups and to allow users to manage resource tag-sync tasks in applications.

## __AWS Supply Chain__
  - ### Features
    - Release DataLakeDataset, DataIntegrationFlow and ResourceTagging APIs for AWS Supply Chain

## __Amazon Bedrock__
  - ### Features
    - Add support for custom models via provisioned throughput for Bedrock Model Evaluation

## __Amazon CloudDirectory__
  - ### Features
    - Add examples for API operations in model.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect introduces StartOutboundChatContact API allowing customers to initiate outbound chat contacts

## __Amazon Verified Permissions__
  - ### Features
    - Add examples for API operations in model.

## __Timestream InfluxDB__
  - ### Features
    - Timestream for InfluxDB now supports port configuration and additional customer-modifiable InfluxDB v2 parameters. This release adds Port to the CreateDbInstance and UpdateDbInstance API, and additional InfluxDB v2 parameters to the CreateDbParameterGroup API.

# __2.28.11__ __2024-09-27__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Documentation updates for AWS Security Hub

## __Amazon Connect Customer Profiles__
  - ### Features
    - Introduces optional RoleArn parameter for PutIntegration request and includes RoleArn in the response of PutIntegration, GetIntegration and ListIntegrations

## __Amazon QuickSight__
  - ### Features
    - Adding personalization in QuickSight data stories. Admins can enable or disable personalization through QuickSight settings.

## __Amazon Simple Email Service__
  - ### Features
    - This release adds support for engagement tracking over Https using custom domains.

# __2.28.10__ __2024-09-26__
## __AWS Chatbot__
  - ### Features
    - Return State and StateReason fields for Chatbot Channel Configurations.

## __AWS Lambda__
  - ### Features
    - Reverting Lambda resource-based policy and block public access APIs.

## __AWS Organizations__
  - ### Features
    - Add support for policy operations on the CHATBOT_POLICY policy type.

## __AWS Parallel Computing Service__
  - ### Features
    - AWS PCS API documentation - Edited the description of the iamInstanceProfileArn parameter of the CreateComputeNodeGroup and UpdateComputeNodeGroup actions; edited the description of the SlurmCustomSetting data type to list the supported parameters for clusters and compute node groups.

## __AWS RDS DataService__
  - ### Features
    - Documentation update for RDS Data API to reflect support for Aurora MySQL Serverless v2 and Provisioned DB clusters.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon SageMaker Service__
  - ### Features
    - Adding `TagPropagation` attribute to Sagemaker API

# __2.28.9__ __2024-09-25__
## __AWS CloudTrail__
  - ### Features
    - Doc-only update for CloudTrail network activity events release (in preview)

## __AWS SDK for Java v2__
  - ### Features
    - Added support for the Smithy RPCv2 CBOR protocol, a new RPC protocol with better performance characteristics than AWS Json.
        - Contributed by: [@sugmanue](https://github.com/sugmanue)
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Updates to documentation for the transit gateway security group referencing feature.

## __Amazon FSx__
  - ### Features
    - Doc-only update to address Lustre S3 hard-coded names.

## __Contributors__
Special thanks to the following contributors to this release: 

[@sugmanue](https://github.com/sugmanue)
# __2.28.8__ __2024-09-24__
## __AWS Budgets__
  - ### Features
    - Releasing minor partitional endpoint updates

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Add support for Cross Region Inference in Bedrock Model Evaluations.

## __Amazon Kinesis__
  - ### Features
    - This release includes support to add tags when creating a stream

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - AWS End User Messaging SMS-Voice V2 has added support for resource policies. Use the three new APIs to create, view, edit, and delete resource policies.

## __Amazon SageMaker Service__
  - ### Features
    - Adding `HiddenInstanceTypes` and `HiddenSageMakerImageVersionAliases` attribute to SageMaker API

# __2.28.7__ __2024-09-23__
## __AWS Glue__
  - ### Features
    - Added AthenaProperties parameter to Glue Connections, allowing Athena to store service specific properties on Glue Connections.

## __AWS Resource Explorer__
  - ### Features
    - AWS Resource Explorer released ListResources feature which allows customers to list all indexed AWS resources within a view.

## __Agents for Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Prompt Flows and Prompt Management now supports using inference profiles to increase throughput and improve resilience.

## __Amazon API Gateway__
  - ### Features
    - Documentation updates for Amazon API Gateway

## __Amazon Athena__
  - ### Features
    - List/Get/Update/Delete/CreateDataCatalog now integrate with AWS Glue connections. Users can create a Glue connection through Athena or use a Glue connection to define their Athena federated parameters.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 G6e instances powered by NVIDIA L40S Tensor Core GPUs are the most cost-efficient GPU instances for deploying generative AI models and the highest performance GPU instances for spatial computing workloads.

## __Amazon Relational Database Service__
  - ### Features
    - Support ComputeRedundancy parameter in ModifyDBShardGroup API. Add DBShardGroupArn in DBShardGroup API response. Remove InvalidMaxAcuFault from CreateDBShardGroup and ModifyDBShardGroup API. Both API will throw InvalidParameterValueException for invalid ACU configuration.

## __EMR Serverless__
  - ### Features
    - This release adds support for job concurrency and queuing configuration at Application level.

# __2.28.6__ __2024-09-20__
## __AWS SDK for Java v2__
  - ### Features
    - Refactoring the user agent string format to be more consistent across SDKs

## __Amazon DynamoDB__
  - ### Features
    - Generate account endpoint for DynamoDB requests when the account ID is available

## __Amazon Neptune__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon SageMaker Metrics Service__
  - ### Features
    - This release introduces support for the SageMaker Metrics BatchGetMetrics API.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker now supports using manifest files to specify the location of uncompressed model artifacts within Model Packages

## __Amazon WorkSpaces__
  - ### Features
    - Releasing new ErrorCodes for SysPrep failures during ImageImport and CreateImage process

# __2.28.5__ __2024-09-19__
## __AWS CodeConnections__
  - ### Features
    - This release adds the PullRequestComment field to CreateSyncConfiguration API input, UpdateSyncConfiguration API input, GetSyncConfiguration API output and ListSyncConfiguration API output

## __AWS Elemental MediaConvert__
  - ### Features
    - This release provides support for additional DRM configurations per SPEKE Version 2.0.

## __AWS Elemental MediaLive__
  - ### Features
    - Adds Bandwidth Reduction Filtering for HD AVC and HEVC encodes, multiplex container settings.

## __AWS Glue__
  - ### Features
    - This change is for releasing TestConnection api SDK model

## __AWS Lambda__
  - ### Features
    - Tagging support for Lambda event source mapping, and code signing configuration resources.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Introduce a new method to transform input to be able to perform update operations on nested DynamoDB object attributes.
        - Contributed by: [@anirudh9391](https://github.com/anirudh9391)

## __Amazon QuickSight__
  - ### Features
    - QuickSight: 1. Add new API - ListFoldersForResource. 2. Commit mode adds visibility configuration of Apply button on multi-select controls for authors.

## __Amazon SageMaker Service__
  - ### Features
    - Introduced support for G6e instance types on SageMaker Studio for JupyterLab and CodeEditor applications.

## __Amazon WorkSpaces Web__
  - ### Features
    - WorkSpaces Secure Browser now enables Administrators to view and manage end-user browsing sessions via Session Management APIs.

## __Contributors__
Special thanks to the following contributors to this release: 

[@anirudh9391](https://github.com/anirudh9391)
# __2.28.4__ __2024-09-18__
## __AWS Cost Explorer Service__
  - ### Features
    - This release extends the GetReservationPurchaseRecommendation API to support recommendations for Amazon DynamoDB reservations.

## __AWS Directory Service__
  - ### Features
    - Added new APIs for enabling, disabling, and describing access to the AWS Directory Service Data API

## __AWS Directory Service Data__
  - ### Features
    - Added new AWS Directory Service Data API, enabling you to manage data stored in AWS Directory Service directories. This includes APIs for creating, reading, updating, and deleting directory users, groups, and group memberships.

## __AWS SDK for Java V2__
  - ### Features
    - Generate and use AWS-account-based endpoints for DynamoDB requests when the account ID is available. The new endpoint URL pattern will be `https://<account-id>.ddb.<region>.amazonaws.com`. See the documentation for details: https://docs.aws.amazon.com/sdkref/latest/guide/feature-account-endpoints.html

## __Amazon GuardDuty__
  - ### Features
    - Add `launchType` and `sourceIPs` fields to GuardDuty findings.

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation with information upgrading snapshots with unsupported engine versions for RDS for MySQL and RDS for PostgreSQL.

## __Amazon Simple Storage Service__
  - ### Features
    - Added SSE-KMS support for directory buckets.

## __MailManager__
  - ### Features
    - Introduce a new RuleSet condition evaluation, where customers can set up a StringExpression with a MimeHeader condition. This condition will perform the necessary validation based on the X-header provided by customers.

# __2.28.3__ __2024-09-17__
## __AWS CodeBuild__
  - ### Features
    - GitLab Enhancements - Add support for Self-Hosted GitLab runners in CodeBuild. Add group webhooks

## __AWS Lambda__
  - ### Features
    - Support for JSON resource-based policies and block public access

## __Amazon EC2 Container Service__
  - ### Features
    - This is a documentation only release to address various tickets.

## __Amazon Elastic Container Registry__
  - ### Features
    - The `DescribeImageScanning` API now includes `fixAvailable`, `exploitAvailable`, and `fixedInVersion` fields to provide more detailed information about the availability of fixes, exploits, and fixed versions for identified image vulnerabilities.

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation with configuration information about the BYOL model for RDS for Db2.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Support for additional levels of cross-account, cross-Region organizational units in Automation. Various documentation updates.

# __2.28.2__ __2024-09-16__
## __AWS Elemental MediaLive__
  - ### Features
    - Removing the ON_PREMISE enum from the input settings field.

## __AWS IoT__
  - ### Features
    - This release adds additional enhancements to AWS IoT Device Management Software Package Catalog and Jobs. It also adds SBOM support in Software Package Version.

## __AWS Organizations__
  - ### Features
    - Doc only update for AWS Organizations that fixes several customer-reported issues

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - This feature adds cross account s3 bucket and VPC support to ModelInvocation jobs. To use a cross account bucket, pass in the accountId of the bucket to s3BucketOwner in the ModelInvocationJobInputDataConfig or ModelInvocationJobOutputDataConfig.

## __Amazon Relational Database Service__
  - ### Features
    - Launching Global Cluster tagging.

## __Private CA Connector for SCEP__
  - ### Features
    - This is a general availability (GA) release of Connector for SCEP, a feature of AWS Private CA. Connector for SCEP links your SCEP-enabled and mobile device management systems to AWS Private CA for digital signature installation and certificate management.

# __2.28.1__ __2024-09-13__
## __AWS Amplify__
  - ### Features
    - Doc only update to Amplify to explain platform setting for Next.js 14 SSG only applications

## __AWS SDK for Java v2__
  - ### Features
    - Add support for specifying endpoint overrides using environment variables, system properties or profile files. More information about this feature is available here: https://docs.aws.amazon.com/sdkref/latest/guide/feature-ss-endpoints.html
    - Updated endpoint and partition metadata.

## __Amazon Interactive Video Service__
  - ### Features
    - Updates to all tags descriptions.

## __Amazon Interactive Video Service Chat__
  - ### Features
    - Updates to all tags descriptions.

## __Amazon S3__
  - ### Bugfixes
    - Fix issue where the `AWS_USE_DUALSTACK_ENDPOINT` environment variable and `aws.useDualstackEndpoint` system property are not resolved during client creation time.

## __Amazon S3 Control__
  - ### Bugfixes
    - Fix issue where the `AWS_USE_DUALSTACK_ENDPOINT` environment variable and `aws.useDualstackEndpoint` system property are not resolved during client creation time.

# __2.28.0__ __2024-09-12__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release includes support for dynamic video overlay workflows, including picture-in-picture and squeezeback

## __AWS Glue__
  - ### Features
    - AWS Glue is introducing two new optimizers for Apache Iceberg tables: snapshot retention and orphan file deletion. Customers can enable these optimizers and customize their configurations to perform daily maintenance tasks on their Iceberg tables based on their specific requirements.

## __AWS Storage Gateway__
  - ### Features
    - The S3 File Gateway now supports DSSE-KMS encryption. A new parameter EncryptionType is added to these APIs: CreateSmbFileShare, CreateNfsFileShare, UpdateSmbFileShare, UpdateNfsFileShare, DescribeSmbFileShares, DescribeNfsFileShares. Also, in favor of EncryptionType, KmsEncrypted is deprecated.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Added email MFA option to user pools with advanced security features.

## __Amazon EMR__
  - ### Features
    - Update APIs to allow modification of ODCR options, allocation strategy, and InstanceTypeConfigs on running InstanceFleet clusters.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for the os-upgrade pending maintenance action for Amazon Aurora DB clusters.

## __Amazon Simple Queue Service__
  - ### Features
    - The AWS SDK for Java now supports a new `BatchManager` for Amazon Simple Queue Service (SQS), allowing for client-side request batching with `SqsAsyncClient`. This feature improves cost efficiency by buffering up to 10 requests before sending them as a batch to SQS. The implementation also supports receive message polling, which further enhances throughput by minimizing the number of individual requests sent. The batched requests help to optimize performance and reduce the costs associated with using Amazon SQS.

## __Elastic Load Balancing__
  - ### Features
    - Correct incorrectly mapped error in ELBv2 waiters

## __Synthetics__
  - ### Features
    - This release introduces two features. The first is tag replication, which allows for the propagation of canary tags onto Synthetics related resources, such as Lambda functions. The second is a limit increase in canary name length, which has now been increased from 21 to 255 characters.


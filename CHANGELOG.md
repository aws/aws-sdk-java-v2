 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.28.26__ __2024-10-18__
## __Amazon Athena__
  - ### Features
    - Removing FEDERATED from Create/List/Delete/GetDataCatalog API

## __Amazon Bedrock__
  - ### Features
    - Adding converse support to CMI API's

## __Amazon Bedrock Runtime__
  - ### Features
    - Added converse support for custom imported models

## __Amazon DataZone__
  - ### Features
    - Adding the following project member designations: PROJECT_CATALOG_VIEWER, PROJECT_CATALOG_CONSUMER and PROJECT_CATALOG_STEWARD in the CreateProjectMembership API and PROJECT_CATALOG_STEWARD designation in the AddPolicyGrant API.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - RequestSpotInstances and RequestSpotFleet feature release.

# __2.28.25__ __2024-10-17__
## __AWS Data Exchange__
  - ### Features
    - This release adds Data Grant support, through which customers can programmatically create data grants to share with other AWS accounts and accept data grants from other AWS accounts.

## __Agents for Amazon Bedrock__
  - ### Features
    - Removing support for topK property in PromptModelInferenceConfiguration object, Making PromptTemplateConfiguration property as required, Limiting the maximum PromptVariant to 1

## __Amazon EC2 Container Service__
  - ### Features
    - This is an Amazon ECS documentation only update to address tickets.

## __Amazon EventBridge Pipes__
  - ### Features
    - This release adds validation to require specifying a SecurityGroup and Subnets in the Vpc object under PipesSourceSelfManagedKafkaParameters. It also adds support for iso-e, iso-f, and other non-commercial partitions in ARN parameters.

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - Added the registrations status of REQUIRES_AUTHENTICATION

## __Amazon QuickSight__
  - ### Features
    - Add StartDashboardSnapshotJobSchedule API. RestoreAnalysis now supports restoring analysis to folders.

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation for TAZ IAM support

## __Amazon WorkSpaces__
  - ### Features
    - Updated the DomainName pattern for Active Directory

# __2.28.24__ __2024-10-16__
## __Amazon Simple Storage Service__
  - ### Features
    - Add support for the new optional bucket-region and prefix query parameters in the ListBuckets API. For ListBuckets requests that express pagination, Amazon S3 will now return both the bucket names and associated AWS regions in the response.

# __2.28.23__ __2024-10-15__
## __AWS Amplify__
  - ### Features
    - Added sourceUrlType field to StartDeployment request

## __AWS CloudFormation__
  - ### Features
    - Documentation update for AWS CloudFormation API Reference.

## __AWS CodeBuild__
  - ### Features
    - Enable proxy for reserved capacity fleet.

## __AWS Resilience Hub__
  - ### Features
    - AWS Resilience Hub now integrates with the myApplications platform, enabling customers to easily assess the resilience of applications defined in myApplications. The new Resiliency widget provides visibility into application resilience and actionable recommendations for improvement.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Interactive Video Service__
  - ### Features
    - On a channel that you own, you can now replace an ongoing stream with a new stream by streaming up with the priority parameter appended to the stream key.

## __Amazon Redshift__
  - ### Features
    - This release launches the CreateIntegration, DeleteIntegration, DescribeIntegrations and ModifyIntegration APIs to create and manage Amazon Redshift Zero-ETL Integrations.

## __Amazon Simple Email Service__
  - ### Features
    - This release adds support for email maximum delivery seconds that allows senders to control the time within which their emails are attempted for delivery.

## __Amazon Transcribe Streaming Service__
  - ### Features
    - We are expanding support for 40 new locales in AWS Transcribe Streaming.

## __QBusiness__
  - ### Features
    - Amazon Q Business now supports embedding the Amazon Q Business web experience on third-party websites.

# __2.28.22__ __2024-10-14__
## __AWS CodePipeline__
  - ### Features
    - AWS CodePipeline V2 type pipelines now support automatically retrying failed stages and skipping stage for failed entry conditions.

## __AWS SDK for Java v2__
  - ### Features
    - Adds an option to set 'appId' metadata to the client builder or to system settings and config files. This metadata string value will be added to the user agent string as `app/somevalue`
    - Updated endpoint and partition metadata.

## __AWS Supply Chain__
  - ### Features
    - This release adds AWS Supply Chain instance management functionality. Specifically adding CreateInstance, DeleteInstance, GetInstance, ListInstances, and UpdateInstance APIs.

## __AWS Transfer Family__
  - ### Features
    - This release enables customers using SFTP connectors to query the transfer status of their files to meet their monitoring needs as well as orchestrate post transfer actions.

## __Amazon Security Lake__
  - ### Features
    - This release updates request validation regex for resource ARNs.

## __MailManager__
  - ### Features
    - Mail Manager support for viewing and exporting metadata of archived messages.

# __2.28.21__ __2024-10-11__
## __AWS RoboMaker__
  - ### Features
    - Documentation update: added support notices to each API action.

## __Amazon Appflow__
  - ### Features
    - Doc only updates for clarification around OAuth2GrantType for Salesforce.

## __Amazon EMR__
  - ### Features
    - This release provides new parameter "Context" in instance fleet clusters.

## __Amazon GuardDuty__
  - ### Features
    - Added a new field for network connection details.

## __Elastic Load Balancing__
  - ### Features
    - Add zonal_shift.config.enabled attribute. Add new AdministrativeOverride construct in the describe-target-health API response to include information about the override status applied to a target.

# __2.28.20__ __2024-10-10__
## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Documentation updates for AWS Private CA.

## __AWS Database Migration Service__
  - ### Features
    - Introduces DescribeDataMigrations, CreateDataMigration, ModifyDataMigration, DeleteDataMigration, StartDataMigration, StopDataMigration operations to SDK. Provides FailedDependencyFault error message.

## __AWS End User Messaging Social__
  - ### Features
    - This release for AWS End User Messaging includes a public SDK, providing a suite of APIs that enable sending WhatsApp messages to end users.

## __AWS IoT FleetWise__
  - ### Features
    - Refine campaign related API validations

## __AWS Outposts__
  - ### Features
    - Adding new "DELIVERED" enum value for Outposts Order status

## __Amazon EC2 Container Service__
  - ### Features
    - This is a documentation only release that updates to documentation to let customers know that Amazon Elastic Inference is no longer available.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for assigning the billing of shared Amazon EC2 On-Demand Capacity Reservations.

## __Amazon Elastic Inference__
  - ### Features
    - Elastic Inference - Documentation update to add service shutdown notice.

## __Amazon Neptune Graph__
  - ### Features
    - Support for 16 m-NCU graphs available through account allowlisting

## __Amazon Route 53 Resolver__
  - ### Features
    - Route 53 Resolver Forwarding Rules can now include a server name indication (SNI) in the target address for rules that use the DNS-over-HTTPS (DoH) protocol. When a DoH-enabled Outbound Resolver Endpoint forwards a request to a DoH server, it will provide the SNI in the TLS handshake.

## __Timestream InfluxDB__
  - ### Features
    - This release updates our regex based validation rules in regards to valid DbInstance and DbParameterGroup name.

# __2.28.19__ __2024-10-09__
## __AWS CodePipeline__
  - ### Features
    - AWS CodePipeline introduces a Compute category

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - This change adds the default retry conditions in the client builder if none are configured to behave similarly to retry policies that are configured behind the scenes without the users having to do that themselves. This will prevent customers using directly the retry strategies builders from `DefaultRetryStrategy` to end up with a no-op strategy.

# __2.28.18__ __2024-10-08__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon ElastiCache__
  - ### Features
    - AWS ElastiCache SDK now supports using APIs with newly launched Valkey engine. Please refer to updated AWS ElastiCache public documentation for detailed information on API usage.

## __Amazon MemoryDB__
  - ### Features
    - Amazon MemoryDB SDK now supports all APIs for newly launched Valkey engine. Please refer to the updated Amazon MemoryDB public documentation for detailed information on API usage.

# __2.28.17__ __2024-10-07__
## __AWS Marketplace Reporting Service__
  - ### Features
    - Documentation-only update for AWS Marketplace Reporting API.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSDeadlineCloud__
  - ### Features
    - Add support for using the template from a previous job during job creation and listing parameter definitions for a job.

## __Amazon Q Connect__
  - ### Features
    - This release adds support for the following capabilities: Configuration of the Gen AI system via AIAgent and AIPrompts. Integration support for Bedrock Knowledge Base.

## __Amazon Redshift__
  - ### Features
    - Add validation pattern to S3KeyPrefix on the EnableLogging API

# __2.28.16__ __2024-10-04__
## __AWS IoT Data Plane__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for Amazon EC2.

# __2.28.15__ __2024-10-03__
## __AWS CodePipeline__
  - ### Features
    - AWS CodePipeline introduces Commands action that enables you to easily run shell commands as part of your pipeline execution.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - Added support for ClipStartTime on the FilterConfiguration object on OriginEndpoint manifest settings objects. Added support for EXT-X-START tags on produced HLS child playlists.

## __AWS IoT__
  - ### Features
    - This release adds support for Custom Authentication with X.509 Client Certificates, support for Custom Client Certificate validation, and support for selecting application protocol and authentication type without requiring TLS ALPN for customer's AWS IoT Domain Configurations.

## __AWS Marketplace Reporting Service__
  - ### Features
    - The AWS Marketplace Reporting service introduces the GetBuyerDashboard API. This API returns a dashboard that provides visibility into your organization's AWS Marketplace agreements and associated spend across the AWS accounts in your organization.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Connect Service__
  - ### Features
    - Public GetMetricDataV2 Grouping increase from 3 to 4

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release includes a new API for modifying instance cpu-options after launch.

## __Amazon QuickSight__
  - ### Features
    - QuickSight: Add support for exporting and importing folders in AssetBundle APIs

# __2.28.14__ __2024-10-02__
## __AWS B2B Data Interchange__
  - ### Features
    - Added and updated APIs to support outbound EDI transformations

## __AWS IoT Core Device Advisor__
  - ### Features
    - Add clientToken attribute and implement idempotency for CreateSuiteDefinition.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Added raw model response and usage metrics to PreProcessing and PostProcessing Trace

## __Amazon AppStream__
  - ### Features
    - Added support for Automatic Time Zone Redirection on Amazon AppStream 2.0

## __Amazon Bedrock Runtime__
  - ### Features
    - Added new fields to Amazon Bedrock Guardrails trace

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - Adds new Stage Health EventErrorCodes applicable to RTMP(S) broadcasts. Bug Fix: Enforces that EncoderConfiguration Video height and width must be even-number values.

## __Amazon SageMaker Service__
  - ### Features
    - releasing builtinlcc to public

## __Amazon Simple Storage Service__
  - ### Features
    - This release introduces a header representing the minimum object size limit for Lifecycle transitions.

## __Amazon WorkSpaces__
  - ### Features
    - WSP is being rebranded to become DCV.

# __2.28.13__ __2024-10-01__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - This release adds support to stop an ongoing ingestion job using the StopIngestionJob API in Agents for Amazon Bedrock.

## __Amazon Relational Database Service__
  - ### Features
    - This release provides additional support for enabling Aurora Limitless Database DB clusters.

## __CodeArtifact__
  - ### Features
    - Add support for the dual stack endpoints.

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


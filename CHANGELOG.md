 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.29.11__ __2024-11-12__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports non-containerized Linux and Windows builds on Reserved Capacity.

## __AWS Control Tower__
  - ### Features
    - Added ResetEnabledControl API.

## __AWS Fault Injection Simulator__
  - ### Features
    - This release adds support for generating experiment reports with the experiment report configuration

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift releases container fleets support for general availability. Deploy Linux-based containerized game server software for hosting on Amazon GameLift.

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation for Amazon RDS Extended Support for Amazon Aurora MySQL.

## __Payment Cryptography Control Plane__
  - ### Features
    - Updated ListAliases API with KeyArn filter.

# __2.29.10__ __2024-11-11__
## __AWS Lambda__
  - ### Features
    - Add Python 3.13 (python3.13) support to AWS Lambda

## __AWS Outposts__
  - ### Features
    - This release updates StartCapacityTask to allow an active Outpost to be modified. It also adds a new API to list all running EC2 instances on the Outpost.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - SigV4: Add host header only when not already provided
        - Contributed by: [@vsudilov](https://github.com/vsudilov)

## __Amazon CloudFront__
  - ### Features
    - No API changes from previous release. This release migrated the model to Smithy keeping all features unchanged.

## __Amazon OpenSearch Service__
  - ### Features
    - Adds Support for new AssociatePackages and DissociatePackages API in Amazon OpenSearch Service that allows association and dissociation operations to be carried out on multiple packages at the same time.

## __Inspector2__
  - ### Features
    - Adds support for filePath filter.

## __Contributors__
Special thanks to the following contributors to this release: 

[@vsudilov](https://github.com/vsudilov)
# __2.29.9__ __2024-11-08__
## __AWS Batch__
  - ### Features
    - This feature allows override LaunchTemplates to be specified in an AWS Batch Compute Environment.

## __AWS Control Catalog__
  - ### Features
    - AWS Control Catalog GetControl public API returns additional data in output, including Implementation and Parameters

## __AWS Lambda__
  - ### Features
    - This release adds support for using AWS KMS customer managed keys to encrypt AWS Lambda .zip deployment packages.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This release adds trace functionality to Bedrock Prompt Flows

## __Amazon Chime SDK Media Pipelines__
  - ### Features
    - Added support for Media Capture Pipeline and Media Concatenation Pipeline for customer managed server side encryption. Now Media Capture Pipeline can use IAM sink role to get access to KMS key and encrypt/decrypt recorded artifacts. KMS key ID can also be supplied with encryption context.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds new error code `Ec2InstanceTypeDoesNotExist` for Amazon EKS managed node groups

## __Amazon Kinesis Firehose__
  - ### Features
    - Amazon Data Firehose / Features : Adds support for a new DeliveryStreamType, DatabaseAsSource. DatabaseAsSource hoses allow customers to stream CDC events from their RDS and Amazon EC2 hosted databases, running MySQL and PostgreSQL database engines, to Iceberg Table destinations.

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - Added the RequiresAuthenticationTimestamp field to the RegistrationVersionStatusHistory data type.

## __QBusiness__
  - ### Features
    - Adds S3 path option to pass group member list for PutGroup API.

# __2.29.8__ __2024-11-07__
## __AWS Clean Rooms ML__
  - ### Features
    - This release introduces support for Custom Models in AWS Clean Rooms ML.

## __AWS Clean Rooms Service__
  - ### Features
    - This release introduces support for Custom Models in AWS Clean Rooms ML.

## __AWS Resource Explorer__
  - ### Features
    - Add GetManagedView, ListManagedViews APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - Add prompt support for chat template configuration and agent generative AI resource. Add support for configuring an optional guardrail in Prompt and Knowledge Base nodes in Prompt Flows. Add API to validate flow definition

## __Amazon Bedrock Runtime__
  - ### Features
    - Add Prompt management support to Bedrock runtime APIs: Converse, ConverseStream, InvokeModel, InvokeModelWithStreamingResponse

## __Amazon QuickSight__
  - ### Features
    - Add Client Credentials based OAuth support for Snowflake and Starburst

## __Auto Scaling__
  - ### Features
    - Auto Scaling groups now support the ability to strictly balance instances across Availability Zones by configuring the AvailabilityZoneDistribution parameter. If balanced-only is configured for a group, launches will always be attempted in the under scaled Availability Zone even if it is unhealthy.

## __Synthetics__
  - ### Features
    - Add support to toggle if a canary will automatically delete provisioned canary resources such as Lambda functions and layers when a canary is deleted. This behavior can be controlled via the new ProvisionedResourceCleanup property exposed in the CreateCanary and UpdateCanary APIs.

# __2.29.7__ __2024-11-06__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now adds additional compute types for reserved capacity fleet.

## __AWS Lake Formation__
  - ### Features
    - API changes for new named tag expressions feature.

## __AWS S3 Control__
  - ### Features
    - Fix ListStorageLensConfigurations and ListStorageLensGroups deserialization for Smithy SDKs.

## __AWS SDK for Java v2__
  - ### Features
    - Improve unmarshalling performance of all JSON protocols by unifying parsing with unmarshalling instead of doing one after the other.

  - ### Bugfixes
    - Moves setting the default backoff strategies for all retry strategies to the builder as per the javadocs instead of only doing it in the `DefaultRetryStrategy` builder methods.

## __Amazon GuardDuty__
  - ### Features
    - GuardDuty RDS Protection expands support for Amazon Aurora PostgreSQL Limitless Databases.

## __Amazon Verified Permissions__
  - ### Features
    - Adding BatchGetPolicy API which supports the retrieval of multiple policies across multiple policy stores within a single request.

## __QApps__
  - ### Features
    - Introduces category apis in AmazonQApps. Web experience users use Categories to tag and filter library items.

# __2.29.6__ __2024-11-01__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Load Checksum classes of CRT from Classloader instead of direct references of CRT classes.

## __Agents for Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Knowledge Bases now supports using application inference profiles to increase throughput and improve resilience.

## __Amazon CloudWatch Logs__
  - ### Features
    - This release introduces an improvement in PutLogEvents

## __Amazon DocumentDB Elastic Clusters__
  - ### Features
    - Amazon DocumentDB Elastic Clusters adds support for pending maintenance actions feature with APIs GetPendingMaintenanceAction, ListPendingMaintenanceActions and ApplyPendingMaintenanceAction

## __Tax Settings__
  - ### Features
    - Add support for supplemental tax registrations via these new APIs: PutSupplementalTaxRegistration, ListSupplementalTaxRegistrations, and DeleteSupplementalTaxRegistration.

# __2.29.5__ __2024-10-31__
## __AWS Batch__
  - ### Features
    - Add `podNamespace` to `EksAttemptDetail` and `containerID` to `EksAttemptContainerDetail`.

## __AWS Glue__
  - ### Features
    - Add schedule support for AWS Glue column statistics

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix an issue where the SDK does not properly unmarshall an evenstream exception to the expected exception type.

## __Amazon Prometheus Service__
  - ### Features
    - Added support for UpdateScraper API, to enable updating collector configuration in-place

## __Amazon SageMaker Service__
  - ### Features
    - SageMaker HyperPod adds scale-down at instance level via BatchDeleteClusterNodes API and group level via UpdateCluster API. SageMaker Training exposes secondary job status in TrainingJobSummary from ListTrainingJobs API. SageMaker now supports G6, G6e, P5e instances for HyperPod and Training.

## __Amazon Simple Email Service__
  - ### Features
    - This release enables customers to provide the email template content in the SESv2 SendEmail and SendBulkEmail APIs instead of the name or the ARN of a stored email template.

## __Auto Scaling__
  - ### Features
    - Adds bake time for Auto Scaling group Instance Refresh

## __Elastic Load Balancing__
  - ### Features
    - Add UDP support for AWS PrivateLink and dual-stack Network Load Balancers

# __2.29.4__ __2024-10-30__
## __AWS AppSync__
  - ### Features
    - This release adds support for AppSync Event APIs.

## __AWS DataSync__
  - ### Features
    - AWS DataSync now supports Enhanced mode tasks. This task mode supports transfer of virtually unlimited numbers of objects with enhanced metrics, more detailed logs, and higher performance than Basic mode. This mode currently supports transfers between Amazon S3 locations.

## __AWS Network Firewall__
  - ### Features
    - AWS Network Firewall now supports configuring TCP idle timeout

## __AWS SDK for Java v2__
  - ### Features
    - Adds support for tracking feature usage in a new user agent metadata section and adds a base set of features. Where features were already a part of the user agent string, they are now converted to the new format where a feature is represented as a Base64 encoded string. For example, using DynamoDb Enhanced Client was previously recorded as 'hll/ddb-enh' in the user agent, but is now a 'd' in the business metrics metadata section 'm/'.
    - Updated endpoint and partition metadata.

## __Amazon Connect Service__
  - ### Features
    - Updated the public documentation for the UserIdentityInfo object to accurately reflect the character limits for the FirstName and LastName fields, which were previously listed as 1-100 characters.

## __Amazon EC2 Container Service__
  - ### Features
    - This release supports service deployments and service revisions which provide a comprehensive view of your Amazon ECS service history.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds two new capabilities to VPC Security Groups: Security Group VPC Associations and Shared Security Groups.

## __Amazon Keyspaces__
  - ### Features
    - Adds support for interacting with user-defined types (UDTs) through the following new operations: Create-Type, Delete-Type, List-Types, Get-Type.

## __Amazon Location Service Maps V2__
  - ### Features
    - Release of Amazon Location Maps API. Maps enables you to build digital maps that showcase your locations, visualize your data, and unlock insights to drive your business

## __Amazon Location Service Places V2__
  - ### Features
    - Release of Amazon Location Places API. Places enables you to quickly search, display, and filter places, businesses, and locations based on proximity, category, and name

## __Amazon Location Service Routes V2__
  - ### Features
    - Release of Amazon Location Routes API. Routes enables you to plan efficient routes and streamline deliveries by leveraging real-time traffic, vehicle restrictions, and turn-by-turn directions.

## __Amazon OpenSearch Service__
  - ### Features
    - This release introduces the new OpenSearch user interface (Dashboards), a new web-based application that can be associated with multiple data sources across OpenSearch managed clusters, serverless collections, and Amazon S3, so that users can gain a comprehensive insights in an unified interface.

## __Amazon Redshift__
  - ### Features
    - This release launches S3 event integrations to create and manage integrations from an Amazon S3 source into an Amazon Redshift database.

## __Amazon Route 53__
  - ### Features
    - This release adds support for TLSA, SSHFP, SVCB, and HTTPS record types.

## __Amazon SageMaker Service__
  - ### Features
    - Added support for Model Registry Staging construct. Users can define series of stages that models can progress through for model workflows and lifecycle. This simplifies tracking and managing models as they transition through development, testing, and production stages.

## __Amazon WorkMail__
  - ### Features
    - This release adds support for Multi-Factor Authentication (MFA) and Personal Access Tokens through integration with AWS IAM Identity Center.

## __OpenSearch Service Serverless__
  - ### Features
    - Neo Integration via IAM Identity Center (IdC)

## __Redshift Serverless__
  - ### Features
    - Adds and updates API members for the Redshift Serverless AI-driven scaling and optimization feature using the price-performance target setting.

# __2.29.3__ __2024-10-29__
## __AWS Clean Rooms Service__
  - ### Features
    - This release adds the option for customers to configure analytics engine when creating a collaboration, and introduces the new SPARK analytics engine type in addition to maintaining the legacy CLEAN_ROOMS_SQL engine type.

## __AWS IoT FleetWise__
  - ### Features
    - Updated BatchCreateVehicle and BatchUpdateVehicle APIs: LimitExceededException has been added and the maximum number of vehicles in a batch has been set to 10 explicitly

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Update Application Inference Profile

## __Amazon Bedrock Runtime__
  - ### Features
    - Update Application Inference Profile

## __Amazon CloudWatch Logs__
  - ### Features
    - Added support for new optional baseline parameter in the UpdateAnomaly API. For UpdateAnomaly requests with baseline set to True, The anomaly behavior is then treated as baseline behavior. However, more severe occurrences of this behavior will still be reported as anomalies.

## __Amazon SageMaker Service__
  - ### Features
    - Adding `notebook-al2-v3` as allowed value to SageMaker NotebookInstance PlatformIdentifier attribute

## __Redshift Data API Service__
  - ### Features
    - Adding a new API GetStatementResultV2 that supports CSV formatted results from ExecuteStatement and BatchExecuteStatement calls.

# __2.29.2__ __2024-10-28__
## __AWS Elemental MediaPackage v2__
  - ### Features
    - MediaPackage V2 Live to VOD Harvester is a MediaPackage V2 feature, which is used to export content from an origin endpoint to a S3 bucket.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Storage Gateway__
  - ### Features
    - Documentation update: Amazon FSx File Gateway will no longer be available to new customers.

## __Amazon OpenSearch Service__
  - ### Features
    - Adds support for provisioning dedicated coordinator nodes. Coordinator nodes can be specified using the new NodeOptions parameter in ClusterConfig.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for Enhanced Monitoring and Performance Insights when restoring Aurora Limitless Database DB clusters. It also adds support for the os-upgrade pending maintenance action.

## __Amazon S3__
  - ### Bugfixes
    - Update the S3 client to correctly handle redirect cases for opt-in regions when crossRegionAccessEnabled is used.

# __2.29.1__ __2024-10-25__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports automatically retrying failed builds

## __AWS Lambda__
  - ### Features
    - Add TagsError field in Lambda GetFunctionResponse. The TagsError field contains details related to errors retrieving tags.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Supply Chain__
  - ### Features
    - API doc updates, and also support showing error message on a failed instance

## __Agents for Amazon Bedrock__
  - ### Features
    - Add support of new model types for Bedrock Agents, Adding inference profile support for Flows and Prompt Management, Adding new field to configure additional inference configurations for Flows and Prompt Management

## __Amazon CloudWatch Logs__
  - ### Features
    - Adding inferred token name for dynamic tokens in Anomalies.

# __2.29.0__ __2024-10-24__
## __AWS Parallel Computing Service__
  - ### Features
    - Documentation update: added the default value of the Slurm configuration parameter scaleDownIdleTimeInSeconds to its description.

## __AWS SDK for Java v2__
  - ### Features
    - The SDK now defaults to Java built-in CRC32 and CRC32C(if it's Java 9+) implementations, resulting in improved performance.
    - Updated endpoint and partition metadata.

  - ### Deprecations
    - Deprecate internal checksum algorithm classes.

## __Amazon AppConfig__
  - ### Features
    - This release improves deployment safety by granting customers the ability to REVERT completed deployments, to the last known good state.In the StopDeployment API revert case the status of a COMPLETE deployment will be REVERTED. AppConfig only allows a revert within 72 hours of deployment completion.

## __Amazon EC2 Container Service__
  - ### Features
    - This release adds support for EBS volumes attached to Amazon ECS Windows tasks running on EC2 instances.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release includes a new API to describe some details of the Amazon Machine Images (AMIs) that were used to launch EC2 instances, even if those AMIs are no longer available for use.

## __QBusiness__
  - ### Features
    - Add a new field in chat response. This field can be used to support nested schemas in array fields


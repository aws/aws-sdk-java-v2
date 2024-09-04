 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.27.19__ __2024-09-04__
## __AWS AppSync__
  - ### Features
    - Adds new logging levels (INFO and DEBUG) for additional log output control

## __AWS Fault Injection Simulator__
  - ### Features
    - This release adds safety levers, a new mechanism to stop all running experiments and prevent new experiments from starting.

## __AWS S3 Control__
  - ### Features
    - Amazon Simple Storage Service /S3 Access Grants / Features : This release launches new Access Grants API - ListCallerAccessGrants.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Introduce a new method to transform input to be able to perform update operations on nested DynamoDB object attributes.
        - Contributed by: [@anirudh9391](https://github.com/anirudh9391)

## __Agents for Amazon Bedrock__
  - ### Features
    - Add support for user metadata inside PromptVariant.

## __Amazon CloudWatch Logs__
  - ### Features
    - Update to support new APIs for delivery of logs from AWS services.

## __DynamoDB Enhanced Client__
  - ### Features
    - Adding support for Select in ScanEnhancedRequest
        - Contributed by: [@shetsa-amzn](https://github.com/shetsa-amzn)

## __FinSpace User Environment Management service__
  - ### Features
    - Updates Finspace documentation for smaller instances.

## __Contributors__
Special thanks to the following contributors to this release: 

[@anirudh9391](https://github.com/anirudh9391), [@shetsa-amzn](https://github.com/shetsa-amzn)
# __2.27.18__ __2024-09-03__
## __AWS Elemental MediaLive__
  - ### Features
    - Added MinQP as a Rate Control option for H264 and H265 encodes.

## __AWS MediaConnect__
  - ### Features
    - AWS Elemental MediaConnect introduces thumbnails for Flow source monitoring. Thumbnails provide still image previews of the live content feeding your MediaConnect Flow allowing you to easily verify that your source is operating as expected.

## __Amazon Connect Service__
  - ### Features
    - Release ReplicaConfiguration as part of DescribeInstance

## __Amazon DataZone__
  - ### Features
    - Add support to let data publisher specify a subset of the data asset that a subscriber will have access to based on the asset filters provided, when accepting a subscription request.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker now supports automatic mounting of a user's home folder in the Amazon Elastic File System (EFS) associated with the SageMaker Studio domain to their Studio Spaces to enable users to share data between their own private spaces.

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for configuring TCP idle timeout on NLB and GWLB listeners.

## __Timestream InfluxDB__
  - ### Features
    - Timestream for InfluxDB now supports compute scaling and deployment type conversion. This release adds the DbInstanceType and DeploymentType parameters to the UpdateDbInstance API.

# __2.27.17__ __2024-08-30__
## __AWS Backup__
  - ### Features
    - The latest update introduces two new attributes, VaultType and VaultState, to the DescribeBackupVault and ListBackupVaults APIs. The VaultState attribute reflects the current status of the vault, while the VaultType attribute indicates the specific category of the vault.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Logs__
  - ### Features
    - This release introduces a new optional parameter: Entity, in PutLogEvents request

## __Amazon DataZone__
  - ### Features
    - Amazon DataZone now adds new governance capabilities of Domain Units for organization within your Data Domains, and Authorization Policies for tighter controls.

## __Redshift Data API Service__
  - ### Features
    - The release include the new Redshift DataAPI feature for session use, customer execute query with --session-keep-alive-seconds parameter and can submit follow-up queries to same sessions with returned`session-id`

# __2.27.16__ __2024-08-29__
## __AWS Step Functions__
  - ### Features
    - This release adds support for static analysis to ValidateStateMachineDefinition API, which can now return optional WARNING diagnostics for semantic errors on the definition of an Amazon States Language (ASL) state machine.

## __AWS WAFV2__
  - ### Features
    - The minimum request rate for a rate-based rule is now 10. Before this, it was 100.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Lifting the maximum length on Bedrock KnowledgeBase RetrievalFilter array

## __Amazon Bedrock Runtime__
  - ### Features
    - Add support for imported-model in invokeModel and InvokeModelWithResponseStream.

## __Amazon Personalize__
  - ### Features
    - This releases ability to update automatic training scheduler for customer solutions

## __Amazon QuickSight__
  - ### Features
    - Increased Character Limit for Dataset Calculation Field expressions

# __2.27.15__ __2024-08-28__
## __AWS Device Farm__
  - ### Features
    - This release removed support for Calabash, UI Automation, Built-in Explorer, remote access record, remote access replay, and web performance profile framework in ScheduleRun API.

## __AWS Parallel Computing Service__
  - ### Features
    - Introducing AWS Parallel Computing Service (AWS PCS), a new service makes it easy to setup and manage high performance computing (HPC) clusters, and build scientific and engineering models at virtually any scale on AWS.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon AppConfig__
  - ### Features
    - This release adds support for deletion protection, which is a safety guardrail to prevent the unintentional deletion of a recently used AWS AppConfig Configuration Profile or Environment. This also includes a change to increase the maximum length of the Name parameter in UpdateConfigurationProfile.

## __Amazon CloudWatch Internet Monitor__
  - ### Features
    - Adds new querying types to show overall traffic suggestion information for monitors

## __Amazon DataZone__
  - ### Features
    - Update regex to include dot character to be consistent with IAM role creation in the authorized principal field for create and update subscription target.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon VPC IP Address Manager (IPAM) now allows customers to provision IPv4 CIDR blocks and allocate Elastic IP Addresses directly from IPAM pools with public IPv4 space

## __Amazon WorkSpaces__
  - ### Features
    - Documentation-only update that clarifies the StartWorkspaces and StopWorkspaces actions, and a few other minor edits.

# __2.27.14__ __2024-08-27__
## __AWS Chatbot__
  - ### Features
    - Update documentation to be consistent with the API docs

## __Amazon Bedrock__
  - ### Features
    - Amazon Bedrock SDK updates for Inference Profile.

## __Amazon Bedrock Runtime__
  - ### Features
    - Amazon Bedrock SDK updates for Inference Profile.

## __Amazon Omics__
  - ### Features
    - Adds data provenance to import jobs from read sets and references

## __Amazon Polly__
  - ### Features
    - Amazon Polly adds 2 new voices: Jitka (cs-CZ) and Sabrina (de-CH).

# __2.27.13__ __2024-08-26__
## __AWS IoT SiteWise__
  - ### Features
    - AWS IoT SiteWise now supports versioning for asset models. It enables users to retrieve active version of their asset model and perform asset model writes with optimistic lock.

## __Amazon WorkSpaces__
  - ### Features
    - This release adds support for creating and managing directories that use AWS IAM Identity Center as user identity source. Such directories can be used to create non-Active Directory domain joined WorkSpaces Personal.Updated RegisterWorkspaceDirectory and DescribeWorkspaceDirectories APIs.

## __s3__
  - ### Bugfixes
    - Added reflect-config.json for S3Client in s3 for native builds
        - Contributed by: [@klopfdreh](https://github.com/klopfdreh)

## __Contributors__
Special thanks to the following contributors to this release: 

[@klopfdreh](https://github.com/klopfdreh)
# __2.27.12__ __2024-08-23__
## __AWS CodeBuild__
  - ### Features
    - Added support for the MAC_ARM environment type for CodeBuild fleets.

## __AWS Organizations__
  - ### Features
    - Releasing minor partitional endpoint updates.

## __AWS Supply Chain__
  - ### Features
    - Update API documentation to clarify the event SLA as well as the data model expectations

## __Agents for Amazon Bedrock__
  - ### Features
    - Releasing the support for Action User Confirmation.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Releasing the support for Action User Confirmation.

## __QBusiness__
  - ### Features
    - Amazon QBusiness: Enable support for SAML and OIDC federation through AWS IAM Identity Provider integration.

# __2.27.11__ __2024-08-22__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Evaluation BatchDeleteEvaluationJob API allows customers to delete evaluation jobs under terminated evaluation job statuses - Stopped, Failed, or Completed. Customers can submit a batch of 25 evaluation jobs to be deleted at once.

## __Amazon EMR Containers__
  - ### Features
    - Correct endpoint for FIPS is configured for US Gov Regions.

## __Amazon QuickSight__
  - ### Features
    - Explicit query for authors and dashboard viewing sharing for embedded users

## __Amazon Route 53__
  - ### Features
    - Amazon Route 53 now supports the Asia Pacific (Malaysia) Region (ap-southeast-5) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region.

## __Auto Scaling__
  - ### Features
    - Amazon EC2 Auto Scaling now provides EBS health check to manage EC2 instance replacement

## __Inspector2__
  - ### Features
    - Add enums for Agentless scan statuses and EC2 enablement error states

# __2.27.10__ __2024-08-21__
## __AWS EntityResolution__
  - ### Features
    - Increase the mapping attributes in Schema to 35.

## __AWS Glue__
  - ### Features
    - Add optional field JobRunQueuingEnabled to CreateJob and UpdateJob APIs.

## __AWS Lambda__
  - ### Features
    - Release FilterCriteria encryption for Lambda EventSourceMapping, enabling customers to encrypt their filter criteria using a customer-owned KMS key.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Security Hub documentation and definition updates

## __Amazon Elastic Compute Cloud__
  - ### Features
    - DescribeInstanceStatus now returns health information on EBS volumes attached to Nitro instances

## __Amazon Simple Email Service__
  - ### Features
    - Enable email receiving customers to provide SES with access to their S3 buckets via an IAM role for "Deliver to S3 Action"

# __2.27.9__ __2024-08-20__
## __Amazon EC2 Container Service__
  - ### Features
    - Documentation only release to address various tickets

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon Simple Storage Service / Features : Add support for conditional writes for PutObject and CompleteMultipartUpload APIs.

## __OpenSearch Service Serverless__
  - ### Features
    - Added FailureCode and FailureMessage to BatchGetCollectionResponse for BatchGetVPCEResponse for non-Active Collection and VPCE.

# __2.27.8__ __2024-08-19__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports creating fleets with macOS platform for running builds.

## __AWS Lambda__
  - ### Features
    - Release Lambda FunctionRecursiveConfig, enabling customers to turn recursive loop detection on or off on individual functions. This release adds two new APIs, GetFunctionRecursionConfig and PutFunctionRecursionConfig.

## __AWS SDK for Java v2__
  - ### Features
    - Update service exception messages to never include the string "null" in the message.
    - Updated endpoint and partition metadata.

## __AWS Systems Manager for SAP__
  - ### Features
    - Add new attributes to the outputs of GetApplication and GetDatabase APIs.

## __AWSDeadlineCloud__
  - ### Features
    - This release adds additional search fields and provides sorting by multiple fields.

## __Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Batch Inference/ Model Invocation is a feature which allows customers to asynchronously run inference on a large set of records/files stored in S3.

## __Amazon S3__
  - ### Features
    - When S3 returns a HEAD response, use the HTTP status text as the errorMessage.

## __DynamoDBEnhancedClient__
  - ### Features
    - This commit introduces ConsumedCapacity in the response of a BatchWrite response
        - Contributed by: [@prateek-vats](https://github.com/prateek-vats)

## __Contributors__
Special thanks to the following contributors to this release: 

[@prateek-vats](https://github.com/prateek-vats)
# __2.27.7__ __2024-08-16__
## __AWS Batch__
  - ### Features
    - Improvements of integration between AWS Batch and EC2.

## __AWS SDK for Java v2__
  - ### Features
    - Add new spotbugs rule to detect blocking call in the async codepath
    - Updated endpoint and partition metadata.

## __Amazon QuickSight__
  - ### Features
    - Amazon QuickSight launches Customer Managed Key (CMK) encryption for Data Source metadata

## __Amazon SageMaker Service__
  - ### Features
    - Introduce Endpoint and EndpointConfig Arns in sagemaker:ListPipelineExecutionSteps API response

## __Amazon Simple Email Service__
  - ### Features
    - Marking use case description field of account details as deprecated.

## __Inspector2__
  - ### Features
    - Update the correct format of key and values for resource tags

# __2.27.6__ __2024-08-15__
## __AWS Identity and Access Management__
  - ### Features
    - Make the LastUsedDate field in the GetAccessKeyLastUsed response optional. This may break customers who only call the API for access keys with a valid LastUsedDate. This fixes a deserialization issue for access keys without a LastUsedDate, because the field was marked as required but could be null.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon DocumentDB with MongoDB compatibility__
  - ### Features
    - This release adds Global Cluster Failover capability which enables you to change your global cluster's primary AWS region, the region that serves writes, during a regional outage. Performing a failover action preserves your Global Cluster setup.

## __Amazon EC2 Container Service__
  - ### Features
    - This release introduces a new ContainerDefinition configuration to support the customer-managed keys for ECS container restart feature.

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon Simple Storage Service / Features : Adds support for pagination in the S3 ListBuckets API.

# __2.27.5__ __2024-08-14__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports using Secrets Manager to store git credentials and using multiple source credentials in a single project.

## __S3 Transfer Manager__
  - ### Features
    - This change enables multipart download for S3 Transfer Manager with the java-based Multipart S3 Async Client.

# __2.27.4__ __2024-08-13__
## __AWS Amplify__
  - ### Features
    - Add a new field "cacheConfig" that enables users to configure the CDN cache settings for an App

## __AWS Fault Injection Simulator__
  - ### Features
    - This release adds support for additional error information on experiment failure. It adds the error code, location, and account id on relevant failures to the GetExperiment and ListExperiment API responses.

## __AWS Glue__
  - ### Features
    - Add AttributesToGet parameter support for Glue GetTables

## __Amazon AppStream__
  - ### Features
    - This release includes following new APIs: CreateThemeForStack, DescribeThemeForStack, UpdateThemeForStack, DeleteThemeForStack to support custom branding programmatically.

## __Amazon Neptune Graph__
  - ### Features
    - Amazon Neptune Analytics provides a new option for customers to load data into a graph using the RDF (Resource Description Framework) NTRIPLES format. When loading NTRIPLES files, use the value `convertToIri` for the `blankNodeHandling` parameter.

# __2.27.3__ __2024-08-12__
## __AWS Compute Optimizer__
  - ### Features
    - Doc only update for Compute Optimizer that fixes several customer-reported issues relating to ECS finding classifications

## __AWS Config__
  - ### Features
    - Documentation update for the OrganizationConfigRuleName regex pattern.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports now supports editing the PID values for a Multiplex.

## __AWS Ground Station__
  - ### Features
    - Updating documentation for OEMEphemeris to link to AWS Ground Station User Guide

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds new capabilities to manage On-Demand Capacity Reservations including the ability to split your reservation, move capacity between reservations, and modify the instance eligibility of your reservation.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support for new AL2023 GPU AMIs to the supported AMITypes.

## __Amazon SageMaker Service__
  - ### Features
    - Releasing large data support as part of CreateAutoMLJobV2 in SageMaker Autopilot and CreateDomain API for SageMaker Canvas.

# __2.27.2__ __2024-08-09__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue where invoking `abort` and then `close` on a `ResponseInputStream` would cause the `close` to fail.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Fixed a description of AdvancedSecurityAdditionalFlows in Amazon Cognito user pool configuration.

## __Amazon Connect Service__
  - ### Features
    - This release supports adding RoutingCriteria via UpdateContactRoutingData public API.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Systems Manager doc-only updates for August 2024.

# __2.27.1__ __2024-08-08__
## __AWS Glue__
  - ### Features
    - This release adds support to retrieve the validation status when creating or updating Glue Data Catalog Views. Also added is support for BasicCatalogTarget partition keys.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Update ResponseTransformer so download attempts to a directory that does not exist or does not have write permissions are not retried

## __AWS SDK for Java v2 Migration Tool__
  - ### Features
    - Introduce the preview release of the AWS SDK for Java v2 migration tool that automatically migrates applications from the AWS SDK for Java v1 to the AWS SDK for Java v2.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Added support for threat protection for custom authentication in Amazon Cognito user pools.

## __Amazon Connect Service__
  - ### Features
    - This release fixes a regression in number of access control tags that are allowed to be added to a security profile in Amazon Connect. You can now add up to four access control tags on a single security profile.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Launch of private IPv6 addressing for VPCs and Subnets. VPC IPAM supports the planning and monitoring of private IPv6 usage.

# __2.27.0__ __2024-08-07__
## __AWS Glue__
  - ### Features
    - Introducing AWS Glue Data Quality anomaly detection, a new functionality that uses ML-based solutions to detect data anomalies users have not explicitly defined rules for.

## __Amazon AppIntegrations Service__
  - ### Features
    - Updated CreateDataIntegration and CreateDataIntegrationAssociation API to support bulk data export from Amazon Connect Customer Profiles to the customer S3 bucket.


 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.23.19__ __2024-02-06__
## __AWS AppSync__
  - ### Features
    - Support for environment variables in AppSync GraphQL APIs

## __AWS WAFV2__
  - ### Features
    - You can now delete an API key that you've created for use with your CAPTCHA JavaScript integration API.

## __Amazon CloudWatch Logs__
  - ### Features
    - This release adds a new field, logGroupArn, to the response of the logs:DescribeLogGroups action.

## __Amazon EC2 Container Service__
  - ### Features
    - This release is a documentation only update to address customer issues.

## __Amazon Elasticsearch Service__
  - ### Features
    - This release adds clear visibility to the customers on the changes that they make on the domain.

## __Amazon OpenSearch Service__
  - ### Features
    - This release adds clear visibility to the customers on the changes that they make on the domain.

# __2.23.18__ __2024-02-05__
## __AWS Glue__
  - ### Features
    - Introduce Catalog Encryption Role within Glue Data Catalog Settings. Introduce SASL/PLAIN as an authentication method for Glue Kafka connections

## __Amazon WorkSpaces__
  - ### Features
    - Added definitions of various WorkSpace states

# __2.23.17__ __2024-02-02__
## __AWS CRT-based S3 Client__
  - ### Features
    - Allow users to configure future completion executor on the AWS CRT-based S3 client via `S3CrtAsyncClientBuilder#futureCompletionExecutor`. See [#4879](https://github.com/aws/aws-sdk-java-v2/issues/4879)

## __AWS SDK for Java v2__
  - ### Features
    - Adds setting to disable making EC2 Instance Metadata Service (IMDS) calls without a token header when prefetching a token does not work. This feature can be configured through environment variables (AWS_EC2_METADATA_V1_DISABLED), system property (aws.disableEc2MetadataV1) or AWS config file (ec2_metadata_v1_disabled). When you configure this setting to true, no calls without token headers will be made to IMDS.
    - Updated endpoint and partition metadata.

## __Amazon DynamoDB__
  - ### Features
    - Any number of users can execute up to 50 concurrent restores (any type of restore) in a given account.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker Canvas adds GenerativeAiSettings support for CanvasAppSettings.

# __2.23.16__ __2024-02-01__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release includes support for broadcast-mixed audio description tracks.

## __AWS SDK for Java v2__
  - ### Features
    - Switching a set of services onto the new SRA (Smithy Reference Architecture) identity and auth logic that was released in v2.21.0. For a list of individual services affected, please check the committed files.
    - Updated endpoint and partition metadata.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Added CreateIdentityProvider and UpdateIdentityProvider details for new SAML IdP features

## __Amazon Interactive Video Service__
  - ### Features
    - This release introduces a new resource Playback Restriction Policy which can be used to geo-restrict or domain-restrict channel stream playback when associated with a channel. New APIs to support this resource were introduced in the form of Create/Delete/Get/Update/List.

## __Amazon Managed Blockchain Query__
  - ### Features
    - This release adds support for transactions that have not reached finality. It also removes support for the status property from the response of the GetTransaction operation. You can use the confirmationStatus and executionStatus properties to determine the status of the transaction.

## __Amazon Neptune Graph__
  - ### Features
    - Adding new APIs in SDK for Amazon Neptune Analytics. These APIs include operations to execute, cancel, list queries and get the graph summary.

# __2.23.15__ __2024-01-31__
## __AWS CloudFormation__
  - ### Features
    - CloudFormation IaC generator allows you to scan existing resources in your account and select resources to generate a template for a new or existing CloudFormation stack.

## __AWS Glue__
  - ### Features
    - Update page size limits for GetJobRuns and GetTriggers APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Reduce how many times input data is copied when writing to chunked encoded operations, like S3's PutObject.
    - Updated endpoint and partition metadata.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release adds an optional Duration parameter to StateManager Associations. This allows customers to specify how long an apply-only-on-cron association execution should run. Once the specified Duration is out all the ongoing cancellable commands or automations are cancelled.

## __Elastic Load Balancing__
  - ### Features
    - This release enables unhealthy target draining intervals for Network Load Balancers.

# __2.23.14__ __2024-01-30__
## __Amazon DataZone__
  - ### Features
    - Add new skipDeletionCheck to DeleteDomain. Add new skipDeletionCheck to DeleteProject which also automatically deletes dependent objects

## __Amazon Route 53__
  - ### Features
    - Update the SDKs for text changes in the APIs.

## __Amazon S3__
  - ### Features
    - Reduce memory usage when request-level plugins aren't used.

# __2.23.13__ __2024-01-29__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Comprehend__
  - ### Features
    - Comprehend PII analysis now supports Spanish input documents.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - EC2 Fleet customers who use attribute based instance-type selection can now intuitively define their Spot instances price protection limit as a percentage of the lowest priced On-Demand instance type.

## __Amazon Import/Export Snowball__
  - ### Features
    - Modified description of createaddress to include direction to add path when providing a JSON file.

## __Amazon Relational Database Service__
  - ### Features
    - Introduced support for the InsufficientDBInstanceCapacityFault error in the RDS RestoreDBClusterFromSnapshot and RestoreDBClusterToPointInTime API methods. This provides enhanced error handling, ensuring a more robust experience.

## __Amazon Simple Storage Service__
  - ### Bugfixes
    - S3 client configured with crossRegionEnabled(true) will now use us-east-1 regional endpoint instead of the global endpoint. See [#4720](https://github.com/aws/aws-sdk-java-v2/issues/4720).

## __AmazonMWAA__
  - ### Features
    - This release adds MAINTENANCE environment status for Amazon MWAA environments.

## __Auto Scaling__
  - ### Features
    - EC2 Auto Scaling customers who use attribute based instance-type selection can now intuitively define their Spot instances price protection limit as a percentage of the lowest priced On-Demand instance type.

# __2.23.12__ __2024-01-26__
## __AWS SDK for Java v2__
  - ### Features
    - Improved performance of chunk-encoded streaming uploads, like S3's PutObject.

  - ### Bugfixes
    - Fixed bug where the ProfileCredentialsProvider would re-read the credentials file with each request by default.

## __Amazon Connect Service__
  - ### Features
    - Update list and string length limits for predefined attributes.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker Automatic Model Tuning now provides an API to programmatically delete tuning jobs.

## __Inspector2__
  - ### Features
    - This release adds ECR container image scanning based on their lastRecordedPullTime.

# __2.23.11__ __2024-01-25__
## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - AWS Private CA now supports an option to omit the CDP extension from issued certificates, when CRL revocation is enabled.

## __Amazon Lightsail__
  - ### Features
    - This release adds support for IPv6-only instance plans.

# __2.23.10__ __2024-01-24__
## __AWS Outposts__
  - ### Features
    - DeviceSerialNumber parameter is now optional in StartConnection API

## __AWS Storage Gateway__
  - ### Features
    - Add DeprecationDate and SoftwareVersion to response of ListGateways.

## __Amazon EC2 Container Service__
  - ### Features
    - Documentation updates for Amazon ECS.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Introduced a new clientToken request parameter on CreateNetworkAcl and CreateRouteTable APIs. The clientToken parameter allows idempotent operations on the APIs.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for Aurora Limitless Database.

# __2.23.9__ __2024-01-23__
## __Inspector2__
  - ### Features
    - This release adds support for CIS scans on EC2 instances.

# __2.23.8__ __2024-01-22__
## __AWS AppConfig Data__
  - ### Features
    - Fix FIPS Endpoints in aws-us-gov.

## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Fixed a thread safety issue that could cause application to crash in the edge case where the SDK attempted to invoke `incrementWindow` after the stream is closed in AWS CRT HTTP Client.

## __AWS Cloud9__
  - ### Features
    - Doc-only update around removing AL1 from list of available AMIs for Cloud9

## __AWS Organizations__
  - ### Features
    - Doc only update for quota increase change

## __Amazon CloudFront KeyValueStore__
  - ### Features
    - This release improves upon the DescribeKeyValueStore API by returning two additional fields, Status of the KeyValueStore and the FailureReason in case of failures during creation of KeyValueStore.

## __Amazon Connect Cases__
  - ### Features
    - This release adds the ability to view audit history on a case and introduces a new parameter, performedBy, for CreateCase and UpdateCase API's.

## __Amazon EC2 Container Service__
  - ### Features
    - This release adds support for Transport Layer Security (TLS) and Configurable Timeout to ECS Service Connect. TLS facilitates privacy and data security for inter-service communications, while Configurable Timeout allows customized per-request timeout and idle timeout for Service Connect services.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for Amazon EC2.

## __Amazon Relational Database Service__
  - ### Features
    - Introduced support for the InsufficientDBInstanceCapacityFault error in the RDS CreateDBCluster API method. This provides enhanced error handling, ensuring a more robust experience when creating database clusters with insufficient instance capacity.

## __FinSpace User Environment Management service__
  - ### Features
    - Allow customer to set zip default through command line arguments.

# __2.23.7__ __2024-01-19__
## __AWS CodeBuild__
  - ### Features
    - Release CodeBuild Reserved Capacity feature

## __AWS SDK for Java v2__
  - ### Features
    - Allowing SDK plugins to read and modify S3's crossRegionEnabled and SQS's checksumValidationEnabled
        - Contributed by: [@anirudh9391](https://github.com/anirudh9391)
    - Updated endpoint and partition metadata.

## __Amazon Athena__
  - ### Features
    - Introducing new NotebookS3LocationUri parameter to Athena ImportNotebook API. Payload is no longer required and either Payload or NotebookS3LocationUri needs to be provided (not both) for a successful ImportNotebook API call. If both are provided, an InvalidRequestException will be thrown.

## __Amazon DynamoDB__
  - ### Features
    - This release adds support for including ApproximateCreationDateTimePrecision configurations in EnableKinesisStreamingDestination API, adds the same as an optional field in the response of DescribeKinesisStreamingDestination, and adds support for a new UpdateKinesisStreamingDestination API.

## __Amazon Q Connect__
  - ### Features
    - Increased Quick Response name max length to 100

## __Contributors__
Special thanks to the following contributors to this release: 

[@anirudh9391](https://github.com/anirudh9391)
# __2.23.6__ __2024-01-18__
## __AWS B2B Data Interchange__
  - ### Features
    - Increasing TestMapping inputFileContent file size limit to 5MB and adding file size limit 250KB for TestParsing input file. This release also includes exposing InternalServerException for Tag APIs.

## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Fixed the issue in the AWS CRT sync HTTP client where the connection was left open after the stream was aborted.

## __AWS CloudTrail__
  - ### Features
    - This release adds a new API ListInsightsMetricData to retrieve metric data from CloudTrail Insights.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Connect Service__
  - ### Features
    - GetMetricDataV2 now supports 3 groupings

## __Amazon Kinesis Firehose__
  - ### Features
    - Allow support for Snowflake as a Kinesis Data Firehose delivery destination.

## __Amazon SageMaker Feature Store Runtime__
  - ### Features
    - Increase BatchGetRecord limits from 10 items to 100 items

## __Elastic Disaster Recovery Service__
  - ### Features
    - Removed invalid and unnecessary default values.

# __2.23.5__ __2024-01-17__
## __AWS Backup Storage, Amazon CodeCatalyst, Amazon Cognito Identity, Amazon Cognito Identity Provider, AWS Identity and Access Management (IAM), Amazon Kinesis, AWS Elemental MediaStore Data Plane, Amazon Transcribe Service, Amazon Transcribe Streaming Service__
  - ### Features
    - Switching a set of services onto the new SRA (Smithy Reference Architecture) identity and auth logic that was released in v2.21.0.

## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Fixed the issue in the AWS CRT HTTP client where the application could crash if stream.incrementWindow was invoked on a closed stream

## __AWS DynamoDB Enhanced Client__
  - ### Features
    - Added support for `@DynamoDBAutoGeneratedUUID` to facilitate the automatic updating of DynamoDB attributes with random UUID.

## __Amazon DynamoDB__
  - ### Features
    - Updating note for enabling streams for UpdateTable.

## __Amazon Keyspaces__
  - ### Features
    - This release adds support for Multi-Region Replication with provisioned tables, and Keyspaces auto scaling APIs

# __2.23.4__ __2024-01-16__
## __AWS IoT__
  - ### Features
    - Revert release of LogTargetTypes

## __AWS IoT FleetWise__
  - ### Features
    - Updated APIs: SignalNodeType query parameter has been added to ListSignalCatalogNodesRequest and ListVehiclesResponse has been extended with attributes field.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Documentation updates for AWS Security Hub

## __Amazon Macie 2__
  - ### Features
    - This release adds support for analyzing Amazon S3 objects that are encrypted using dual-layer server-side encryption with AWS KMS keys (DSSE-KMS). It also adds support for reporting DSSE-KMS details in statistics and metadata about encryption settings for S3 buckets and objects.

## __Amazon Personalize__
  - ### Features
    - Documentation updates for Amazon Personalize.

## __Amazon Personalize Runtime__
  - ### Features
    - Documentation updates for Amazon Personalize

## __Amazon Rekognition__
  - ### Features
    - This release adds ContentType and TaxonomyLevel attributes to DetectModerationLabels and GetMediaAnalysisJob API responses.

## __Amazon S3__
  - ### Features
    - Propagating client apiCallTimeout values to S3Express createSession calls. If existing, this value overrides the default timeout value of 10s when making the nested S3Express session credentials call.

## __Payment Cryptography Control Plane__
  - ### Features
    - Provide an additional option for key exchange using RSA wrap/unwrap in addition to tr-34/tr-31 in ImportKey and ExportKey operations. Added new key usage (type) TR31_M1_ISO_9797_1_MAC_KEY, for use with Generate/VerifyMac dataplane operations with ISO9797 Algorithm 1 MAC calculations.

# __2.23.3__ __2024-01-13__
## __Amazon SageMaker Service__
  - ### Features
    - This release will have ValidationException thrown if certain invalid app types are provided. The release will also throw ValidationException if more than 10 account ids are provided in VpcOnlyTrustedAccounts.

# __2.23.2__ __2024-01-12__
## __AWS S3 Control__
  - ### Features
    - S3 On Outposts team adds dualstack endpoints support for S3Control and S3Outposts API calls.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix shading of artifacts in the `bundle` by not `org.apache.log4j.*` packages. This allows proper binding of `commons-logging` to Log4J and enables dependencies that use commons logging (e.g. Apache HTTP Client) to properly bind to Log4j.

## __AWS Supply Chain__
  - ### Features
    - This release includes APIs CreateBillOfMaterialsImportJob and GetBillOfMaterialsImportJob.

## __AWS Transfer Family__
  - ### Features
    - AWS Transfer Family now supports static IP addresses for SFTP & AS2 connectors and for async MDNs on AS2 servers.

## __Amazon Connect Participant Service__
  - ### Features
    - Introduce new Supervisor participant role

## __Amazon Connect Service__
  - ### Features
    - Supervisor Barge for Chat is now supported through the MonitorContact API.

## __Amazon Location Service__
  - ### Features
    - Location SDK documentation update. Added missing fonts to the MapConfiguration data type. Updated note for the SubMunicipality property in the place data type.

## __AmazonMWAA__
  - ### Features
    - This Amazon MWAA feature release includes new fields in CreateWebLoginToken response model. The new fields IamIdentity and AirflowIdentity will let you match identifications, as the Airflow identity length is currently hashed to 64 characters.

# __2.23.1__ __2024-01-11__
## __AWS IoT__
  - ### Features
    - Add ConflictException to Update APIs of AWS IoT Software Package Catalog

## __AWS IoT FleetWise__
  - ### Features
    - The following dataTypes have been removed: CUSTOMER_DECODED_INTERFACE in NetworkInterfaceType; CUSTOMER_DECODED_SIGNAL_INFO_IS_NULL in SignalDecoderFailureReason; CUSTOMER_DECODED_SIGNAL_NETWORK_INTERFACE_INFO_IS_NULL in NetworkInterfaceFailureReason; CUSTOMER_DECODED_SIGNAL in SignalDecoderType

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix IllegalArgumentException in FullJitterBackoffStrategy when base delay and max backoff time are zero.

## __AWS Secrets Manager__
  - ### Features
    - Doc only update for Secrets Manager

## __Amazon EC2 Container Service__
  - ### Features
    - This release adds support for adding an ElasticBlockStorage volume configurations in ECS RunTask/StartTask/CreateService/UpdateService APIs. The configuration allows for attaching EBS volumes to ECS Tasks.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for adding an ElasticBlockStorage volume configurations in ECS RunTask/StartTask/CreateService/UpdateService APIs. The configuration allows for attaching EBS volumes to ECS Tasks.

## __Amazon EventBridge__
  - ### Features
    - Adding AppSync as an EventBridge Target

## __Amazon WorkSpaces__
  - ### Features
    - Added AWS Workspaces RebootWorkspaces API - Extended Reboot documentation update

# __2.23.0__ __2024-01-10__
## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Fixed the issue where `AWS_ERROR_HTTP_CONNECTION_CLOSED` was not retried by the SDK.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Logs__
  - ### Features
    - Add support for account level subscription filter policies to PutAccountPolicy, DescribeAccountPolicies, and DeleteAccountPolicy APIs. Additionally, PutAccountPolicy has been modified with new optional "selectionCriteria" parameter for resource selection.

## __Amazon Connect Wisdom Service__
  - ### Features
    - QueryAssistant and GetRecommendations will be discontinued starting June 1, 2024. To receive generative responses after March 1, 2024 you will need to create a new Assistant in the Connect console and integrate the Amazon Q in Connect JavaScript library (amazon-q-connectjs) into your applications.

## __Amazon Location Service__
  - ### Features
    - This release adds API support for custom layers for the maps service APIs: CreateMap, UpdateMap, DescribeMap.

## __Amazon Q Connect__
  - ### Features
    - QueryAssistant and GetRecommendations will be discontinued starting June 1, 2024. To receive generative responses after March 1, 2024 you will need to create a new Assistant in the Connect console and integrate the Amazon Q in Connect JavaScript library (amazon-q-connectjs) into your applications.

## __Amazon Route 53__
  - ### Features
    - Route53 now supports geoproximity routing in AWS regions

## __Amazon S3__
  - ### Bugfixes
    - Fixes a bug in DeleteObjects to properly encode the key in the request.

## __AmazonConnectCampaignService__
  - ### Features
    - Minor pattern updates for Campaign and Dial Request API fields.

## __Redshift Serverless__
  - ### Features
    - Updates to ConfigParameter for RSS workgroup, removal of use_fips_ssl


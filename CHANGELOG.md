 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.31.16__ __2025-04-04__
## __AWS Directory Service Data__
  - ### Features
    - Doc only update - fixed broken links.

## __AWS S3 Control__
  - ### Features
    - Updated max size of Prefixes parameter of Scope data type.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Doc-only updates for Amazon EC2

## __Amazon EventBridge__
  - ### Features
    - Amazon EventBridge adds support for customer-managed keys on Archives and validations for two fields: eventSourceArn and kmsKeyIdentifier.

# __2.31.15__ __2025-04-03__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - Added optional "customMetadataField" for Amazon Aurora knowledge bases, allowing single-column metadata. Also added optional "textIndexName" for MongoDB Atlas knowledge bases, enabling hybrid search support.

## __Amazon Chime SDK Voice__
  - ### Features
    - Added FOC date as an attribute of PhoneNumberOrder, added AccessDeniedException as a possible return type of ValidateE911Address

## __Amazon OpenSearch Service__
  - ### Features
    - Improve descriptions for various API commands and data types.

## __Amazon Route 53__
  - ### Features
    - Added us-gov-east-1 and us-gov-west-1 as valid Latency Based Routing regions for change-resource-record-sets.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for i3en, m7i, r7i instance types for SageMaker Hyperpod

## __Amazon Simple Email Service__
  - ### Features
    - This release enables customers to provide attachments in the SESv2 SendEmail and SendBulkEmail APIs.

## __Amazon Transcribe Service__
  - ### Features
    - This Feature Adds Support for the "zh-HK" Locale for Batch Operations

## __MailManager__
  - ### Features
    - Add support for Dual_Stack and PrivateLink types of IngressPoint. For configuration requests, SES Mail Manager will now accept both IPv4/IPv6 dual-stack endpoints and AWS PrivateLink VPC endpoints for email receiving.

# __2.31.14__ __2025-04-02__
## __AWS CodeBuild__
  - ### Features
    - This release adds support for environment type WINDOWS_SERVER_2022_CONTAINER in ProjectEnvironment

## __AWS Elemental MediaLive__
  - ### Features
    - Added support for SMPTE 2110 inputs when running a channel in a MediaLive Anywhere cluster. This feature enables ingestion of SMPTE 2110-compliant video, audio, and ancillary streams by reading SDP files that AWS Elemental MediaLive can retrieve from a network source.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - Application Signals now supports creating Service Level Objectives on service dependencies. Users can now create or update SLOs on discovered service dependencies to monitor their standard application metrics.

## __Amazon EC2 Container Service__
  - ### Features
    - This is an Amazon ECS documentation only update to address various tickets.

## __Amazon Elastic Container Registry__
  - ### Features
    - Fix for customer issues related to AWS account ID and size limitation for token.

## __Amazon Lex Model Building V2__
  - ### Features
    - Release feature of errorlogging for lex bot, customer can config this feature in bot version to generate log for error exception which helps debug

# __2.31.13__ __2025-04-01__
## __AWS Clean Rooms Service__
  - ### Features
    - This release adds support for updating the analytics engine of a collaboration.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon SageMaker Service__
  - ### Features
    - Added tagging support for SageMaker notebook instance lifecycle configurations

# __2.31.12__ __2025-03-31__
## __AWS Marketplace Entitlement Service__
  - ### Features
    - Add support for Marketplace Entitlement Service dual-stack endpoints.

## __AWS Outposts__
  - ### Features
    - Enabling Asset Level Capacity Management feature, which allows customers to create a Capacity Task for a single Asset on their active Outpost.

## __AWS S3 Control__
  - ### Features
    - Amazon S3 adds support for S3 Access Points for directory buckets in AWS Dedicated Local Zones

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Transfer Family__
  - ### Features
    - Add WebAppEndpointPolicy support for WebApps

## __AWSDeadlineCloud__
  - ### Features
    - With this release you can use a new field to specify the search term match type. Search term match types currently support fuzzy and contains matching.

## __Amazon Bedrock Runtime__
  - ### Features
    - Add Prompt Caching support to Converse and ConverseStream APIs

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Use String instead of Select enum for ProjectionExpression to support future values

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Release VPC Route Server, a new feature allowing dynamic routing in VPCs.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Add support for updating RemoteNetworkConfig for hybrid nodes on EKS UpdateClusterConfig API

## __Amazon Simple Email Service__
  - ### Features
    - Add dual-stack support to global endpoints.

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon S3 adds support for S3 Access Points for directory buckets in AWS Dedicated Local Zones

# __2.31.11__ __2025-03-28__
## __AWS CRT-based S3 Client__
  - ### Bugfixes
    - Fixed an issue in the AWS CRT-based S3 client where a GetObject request with `AsyncResponseTransformer#toBlockingInputStream` may hang if request failed mid streaming

## __AWS CodeBuild__
  - ### Features
    - This release adds support for cacheNamespace in ProjectCache

## __AWS Network Manager__
  - ### Features
    - Add support for NetworkManager Dualstack endpoints.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSMarketplace Metering__
  - ### Features
    - Add support for Marketplace Metering Service dual-stack endpoints.

## __Amazon API Gateway__
  - ### Features
    - Adds support for setting the IP address type to allow dual-stack or IPv4 address types to invoke your APIs or domain names.

## __Amazon Bedrock Runtime__
  - ### Features
    - Launching Multi-modality Content Filter for Amazon Bedrock Guardrails.

## __Amazon EC2 Container Service__
  - ### Features
    - This is an Amazon ECS documentation only release that addresses tickets.

## __Amazon QuickSight__
  - ### Features
    - RLS permission dataset with userAs: RLS_RULES flag, Q in QuickSight/Threshold Alerts/Schedules/Snapshots in QS embedding, toggle dataset refresh email alerts via API, transposed table with options: column width, type and index, toggle Q&A on dashboards, Oracle Service Name when creating data source.

## __Amazon SageMaker Service__
  - ### Features
    - TransformAmiVersion for Batch Transform and SageMaker Search Service Aggregate Search API Extension

## __AmazonApiGatewayV2__
  - ### Features
    - Adds support for setting the IP address type to allow dual-stack or IPv4 address types to invoke your APIs or domain names.

## __Payment Cryptography Control Plane__
  - ### Features
    - The service adds support for transferring AES-256 and other keys between the service and other service providers and HSMs. This feature uses ECDH to derive a one-time key transport key to enable these secure key exchanges.

# __2.31.10__ __2025-03-27__
## __AWS Batch__
  - ### Features
    - This release will enable two features: Firelens log driver, and Execute Command on Batch jobs on ECS. Both features will be passed through to ECS.

## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Added standaloneAccountRateTypeSelections for GetPreferences and UpdatePreferences APIs. Added STALE enum value to status attribute in GetBillScenario and UpdateBillScenario APIs.

## __AWS CloudFormation__
  - ### Features
    - Adding support for the new parameter "ScanFilters" in the CloudFormation StartResourceScan API. When this parameter is included, the StartResourceScan API will initiate a scan limited to the resource types specified by the parameter.

## __AWS Identity and Access Management__
  - ### Features
    - Update IAM dual-stack endpoints for BJS, IAD and PDT partitions

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SSO OIDC__
  - ### Features
    - This release adds AwsAdditionalDetails in the CreateTokenWithIAM API response.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - bedrock flow now support node action trace.

## __Amazon DataZone__
  - ### Features
    - This release adds new action type of Create Listing Changeset for the Metadata Enforcement Rule feature.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support for BOTTLEROCKET FIPS AMIs to AMI types in US regions.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers add support for additional instance types.

## __Amazon SageMaker Service__
  - ### Features
    - add: recovery mode for SageMaker Studio apps

# __2.31.9__ __2025-03-26__
## __AWS ARC - Zonal Shift__
  - ### Features
    - Add new shiftType field for ARC zonal shifts.

## __AWS Direct Connect__
  - ### Features
    - With this release, AWS Direct Connect allows you to tag your Direct Connect gateways. Tags are metadata that you can create and use to manage your Direct Connect gateways. For more information about tagging, see AWS Tagging Strategies.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds a configurable Quality Level setting for the top rendition of Auto ABR jobs

## __AWS MediaTailor__
  - ### Features
    - Add support for log filtering which allow customers to filter out selected event types from logs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS WAFV2__
  - ### Features
    - This release adds the ability to associate an AWS WAF v2 web ACL with an AWS Amplify App.

## __Amazon Polly__
  - ### Features
    - Added support for the new voice - Jihye (ko-KR). Jihye is available as a Neural voice only.

## __Amazon Relational Database Service__
  - ### Features
    - Add note about the Availability Zone where RDS restores the DB cluster for the RestoreDBClusterToPointInTime operation.

# __2.31.8__ __2025-03-25__
## __AWS Marketplace Entitlement Service__
  - ### Features
    - This release enhances the GetEntitlements API to support new filter CUSTOMER_AWS_ACCOUNT_ID in request and CustomerAWSAccountId field in response.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSMarketplace Metering__
  - ### Features
    - This release enhances the BatchMeterUsage API to support new field CustomerAWSAccountId in request and response and making CustomerIdentifier optional. CustomerAWSAccountId or CustomerIdentifier must be provided in request but not both.

## __Agents for Amazon Bedrock__
  - ### Features
    - Adding support for Amazon OpenSearch Managed clusters as a vector database in Knowledge Bases for Amazon Bedrock

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support to override upgrade-blocking readiness checks via force flag when updating a cluster.

## __Amazon GameLift Streams__
  - ### Features
    - Minor updates to improve developer experience.

## __Amazon Keyspaces__
  - ### Features
    - Removing replication region limitation for Amazon Keyspaces Multi-Region Replication APIs.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds support for customer-managed KMS keys in Amazon SageMaker Partner AI Apps

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Deprecate tags field in Get API responses

## __EC2 Metadata Client__
  - ### Features
    - Added new Ec2MetadataClientException extending SdkClientException for IMDS unsuccessful responses that captures HTTP status codes, headers, and raw response content for improved error handling. See [#5786](https://github.com/aws/aws-sdk-java-v2/issues/5786)

# __2.31.7__ __2025-03-24__
## __AWS IoT Wireless__
  - ### Features
    - Mark EutranCid under LteNmr optional.

## __AWS Parallel Computing Service__
  - ### Features
    - ClusterName/ClusterIdentifier, ComputeNodeGroupName/ComputeNodeGroupIdentifier, and QueueName/QueueIdentifier can now have 10 characters, and a minimum of 3 characters. The TagResource API action can now return ServiceQuotaExceededException.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Q Connect__
  - ### Features
    - Provides the correct value for supported model ID.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release adds the AvailableSecurityUpdatesComplianceStatus field to patch baseline operations, as well as the AvailableSecurityUpdateCount and InstancesWithAvailableSecurityUpdates to patch state operations. Applies to Windows Server managed nodes only.

# __2.31.6__ __2025-03-21__
## __AWS Route53 Recovery Control Config__
  - ### Features
    - Adds dual-stack (IPv4 and IPv6) endpoint support for route53-recovery-control-config operations, opt-in dual-stack addresses for cluster endpoints, and UpdateCluster API to update the network-type of clusters between IPv4 and dual-stack.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - A CustomModelUnit(CMU) is an abstract view of the hardware utilization that Bedrock needs to host a a single copy of your custom imported model. Bedrock determines the number of CMUs that a model copy needs when you import the custom model. You can use CMUs to estimate the cost of Inference's.

## __Amazon DataZone__
  - ### Features
    - Add support for overriding selection of default AWS IAM Identity Center instance as part of Amazon DataZone domain APIs.

## __Amazon SageMaker Service__
  - ### Features
    - This release does the following: 1.) Adds DurationHours as a required field to the SearchTrainingPlanOfferings action in the SageMaker AI API; 2.) Adds support for G6e instance types for SageMaker AI inference optimization jobs.

# __2.31.5__ __2025-03-20__
## __AWS Amplify__
  - ### Features
    - Added appId field to Webhook responses

## __AWS Control Catalog__
  - ### Features
    - Add ExemptAssumeRoot parameter to adapt for new AWS AssumeRoot capability.

## __AWS Network Firewall__
  - ### Features
    - You can now use flow operations to either flush or capture traffic monitored in your firewall's flow table.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - With this release, Bedrock Evaluation will now support bring your own inference responses.

## __MailManager__
  - ### Features
    - Amazon SES Mail Manager. Extended rule string and boolean expressions to support analysis in condition evaluation. Extended ingress point string expression to support analysis in condition evaluation

# __2.31.4__ __2025-03-19__
## __AWS Lambda__
  - ### Features
    - Add Ruby 3.4 (ruby3.4) support to AWS Lambda.

## __AWS MediaConnect__
  - ### Features
    - This release adds support for NDI flow outputs in AWS Elemental MediaConnect. You can now send content from your MediaConnect transport streams directly to your NDI environment using the new NDI output type.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Support custom prompt routers for evaluation jobs

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Doc-only updates for EC2 for March 2025.

## __Amazon Neptune Graph__
  - ### Features
    - Update IAM Role ARN Validation to Support Role Paths

## __Amazon SageMaker Service__
  - ### Features
    - Added support for g6, g6e, m6i, c6i instance types in SageMaker Processing Jobs.

# __2.31.3__ __2025-03-18__
## __AWS AppSync__
  - ### Features
    - Providing Tagging support for DomainName in AppSync

## __AWS Clean Rooms Service__
  - ### Features
    - This release adds support for PySpark jobs. Customers can now analyze data by running jobs using approved PySpark analysis templates.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds support for AVC passthrough, the ability to specify PTS offset without padding, and an A/V segment matching feature.

## __AWS SDK for Java v2__
  - ### Features
    - Added functionality to be able to configure an endpoint override through the [services] section in the aws config file for specific services. 
      https://docs.aws.amazon.com/sdkref/latest/guide/feature-ss-endpoints.html
    - Updated endpoint and partition metadata.

## __Amazon Route 53__
  - ### Features
    - Amazon Route 53 now supports the iso-f regions for private DNS Amazon VPCs and cloudwatch healthchecks.

# __2.31.2__ __2025-03-17__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Copy bytes written to OutputStream of BlockingOutputStreamAsyncRequestBody

## __AWS WAFV2__
  - ### Features
    - AWS WAF now lets you inspect fragments of request URIs. You can specify the scope of the URI to inspect and narrow the set of URI fragments.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - This release adds support for adding, removing, and listing SLO time exclusion windows with the BatchUpdateExclusionWindows and ListServiceLevelObjectiveExclusionWindows APIs.

## __Amazon Location Service Maps V2__
  - ### Features
    - Provide support for vector map styles in the GetStaticMap operation.

## __CloudWatch RUM__
  - ### Features
    - CloudWatch RUM now supports unminification of JS error stack traces.

## __Tax Settings__
  - ### Features
    - Adjust Vietnam PaymentVoucherNumber regex and minor API change.

# __2.31.1__ __2025-03-14__
## __AWS CRT HTTP Client__
  - ### Features
    - Map AWS_IO_SOCKET_TIMEOUT to ConnectException when acquiring a connection to improve error handling
        - Contributed by: [@thomasjinlo](https://github.com/thomasjinlo)

## __AWS Glue__
  - ### Features
    - This release added AllowFullTableExternalDataAccess to glue catalog resource.

## __AWS Lake Formation__
  - ### Features
    - This release added "condition" to LakeFormation OptIn APIs, also added WithPrivilegedAccess flag to RegisterResource and DescribeResource.

## __AWS SDK for Java v2__
  - ### Features
    - Made DefaultSdkAutoConstructList and DefaultSdkAutoConstructMap serializable
    - Updated endpoint and partition metadata.

## __Amazon Cognito Identity__
  - ### Features
    - Updated API model build artifacts for identity pools

## __Amazon Cognito Identity Provider__
  - ### Features
    - Minor description updates to API parameters

## __Amazon S3__
  - ### Bugfixes
    - Updated logic for S3MultiPartUpload. Part numbers are now assigned and incremented when parts are read.

## __Contributors__
Special thanks to the following contributors to this release: 

[@thomasjinlo](https://github.com/thomasjinlo)
# __2.31.0__ __2025-03-13__
## __AWS Amplify__
  - ### Features
    - Introduced support for Skew Protection. Added enableSkewProtection field to createBranch and updateBranch API.

## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Private Certificate Authority service now supports P521 and RSA3072 key algorithms.

## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports webhook filtering by organization name

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds the ResetChannelState and ResetOriginEndpointState operation to reset MediaPackage V2 channel and origin endpoint. This release also adds a new field, UrlEncodeChildManifest, for HLS/LL-HLS to allow URL-encoding child manifest query string based on the requirements of AWS SigV4.

## __AWS S3 Control__
  - ### Features
    - Updating GetDataAccess response for S3 Access Grants to include the matched Grantee for the requested prefix

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Updated the SDK to handle error thrown from consumer subscribed to paginator publisher, which caused the request to hang for pagination operations

## __Amazon CloudWatch Logs__
  - ### Features
    - Updated CreateLogAnomalyDetector to accept only kms key arn

## __Amazon DataZone__
  - ### Features
    - This release adds support to update projects and environments

## __Amazon DynamoDB__
  - ### Features
    - Generate account endpoints for DynamoDB requests using ARN-sourced account ID when available

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release changes the CreateLaunchTemplate, CreateLaunchTemplateVersion, ModifyLaunchTemplate CLI and SDKs such that if you do not specify a client token, a randomly generated token is used for the request to ensure idempotency.

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - IVS Real-Time now offers customers the ability to adjust the participant & composition recording segment duration


 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


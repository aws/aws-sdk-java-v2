 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


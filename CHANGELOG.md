 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.30.0__ __2025-01-15__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SDK for Java v2 Migration Tool__
  - ### Bugfixes
    - Transform the getter methods on the service model classes that return SdkBytes to return ByteBuffer to be compatible with v1 style getters

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Now supports streaming for inline agents.

## __Amazon API Gateway__
  - ### Features
    - Documentation updates for Amazon API Gateway

## __Amazon Cognito Identity__
  - ### Features
    - corrects the dual-stack endpoint configuration

## __Amazon Simple Email Service__
  - ### Features
    - This release introduces a new recommendation in Virtual Deliverability Manager Advisor, which detects elevated complaint rates for customer sending identities.

## __Amazon Simple Storage Service__
  - ### Features
    - This change enhances integrity protections for new SDK requests to S3. S3 SDKs now support the CRC64NVME checksum algorithm, full object checksums for multipart S3 objects, and new default integrity protections for S3 requests.

## __Amazon WorkSpaces__
  - ### Features
    - Added GeneralPurpose.4xlarge & GeneralPurpose.8xlarge ComputeTypes.

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Mark type in MaintenanceWindow as required.

## __Partner Central Selling API__
  - ### Features
    - Add Tagging support for ResourceSnapshotJob resources

## __S3 Event Notification__
  - ### Bugfixes
    - add static modifier to fromJson(InputStream) method of S3EventNotification

## __Security Incident Response__
  - ### Features
    - Increase minimum length of Threat Actor IP 'userAgent' to 1.


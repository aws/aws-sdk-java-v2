 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.30.2__ __2025-01-17__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS User Notifications__
  - ### Features
    - Added support for Managed Notifications, integration with AWS Organization and added aggregation summaries for Aggregate Notifications

## __Amazon Bedrock Runtime__
  - ### Features
    - Allow hyphens in tool name for Converse and ConverseStream APIs

## __Amazon Detective__
  - ### Features
    - Doc only update for Detective documentation.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Release u7i-6tb.112xlarge, u7i-8tb.112xlarge, u7inh-32tb.480xlarge, p5e.48xlarge, p5en.48xlarge, f2.12xlarge, f2.48xlarge, trn2.48xlarge instance types.

## __Amazon SageMaker Service__
  - ### Features
    - Correction of docs for "Added support for ml.trn1.32xlarge instance type in Reserved Capacity Offering"

## __Amazon Simple Storage Service__
- ### Bugfixes
    - Fixed contentLength mismatch issue thrown from putObject when multipartEnabled is true and a contentLength is provided in PutObjectRequest. See [#5807](https://github.com/aws/aws-sdk-java-v2/issues/5807)

# __2.30.1__ __2025-01-16__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon EC2 Container Service__
  - ### Features
    - The release addresses Amazon ECS documentation tickets.

## __Amazon SageMaker Service__
  - ### Features
    - Added support for ml.trn1.32xlarge instance type in Reserved Capacity Offering

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
    - S3 client behavior is updated to always calculate a checksum by default for operations that support it (such as PutObject or UploadPart), or require it (such as DeleteObjects). The checksum algorithm used by default is CRC32. The S3 client attempts to validate response checksums for all S3 API operations that support checksums. However, if the SDK has not implemented the specified checksum algorithm then this validation is skipped. See [Dev Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/s3-checksums.html) for more information
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


 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.30.9__ __2025-01-29__
## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Added ConflictException error type in DeleteBillScenario, BatchDeleteBillScenarioCommitmentModification, BatchDeleteBillScenarioUsageModification, BatchUpdateBillScenarioUsageModification, and BatchUpdateBillScenarioCommitmentModification API operations.

## __Amazon Elastic Container Registry__
  - ### Features
    - Add support for Dualstack and Dualstack-with-FIPS Endpoints

## __Amazon Elastic Container Registry Public__
  - ### Features
    - Add support for Dualstack Endpoints

## __Amazon Simple Storage Service__
  - ### Features
    - Change the type of MpuObjectSize in CompleteMultipartUploadRequest from int to long.

## __Amazon Transcribe Streaming Service__
  - ### Features
    - This release adds support for AWS HealthScribe Streaming APIs within Amazon Transcribe.

## __MailManager__
  - ### Features
    - This release includes a new feature for Amazon SES Mail Manager which allows customers to specify known addresses and domains and make use of those in traffic policies and rules actions to distinguish between known and unknown entries.

# __2.30.8__ __2025-01-28__
## __AWS AppSync__
  - ### Features
    - Add stash and outErrors to EvaluateCode/EvaluateMappingTemplate response

## __AWS DataSync__
  - ### Features
    - AWS DataSync now supports the Kerberos authentication protocol for SMB locations.

## __AWS SDK for Java v2__
  - ### Features
    - Buffer input data from ContentStreamProvider to avoid the need to reread the stream after calculating its length.

## __AWSDeadlineCloud__
  - ### Features
    - feature: Deadline: Add support for limiting the concurrent usage of external resources, like floating licenses, using limits and the ability to constrain the maximum number of workers that work on a job

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release changes the CreateFleet CLI and SDK's such that if you do not specify a client token, a randomly generated token is used for the request to ensure idempotency.

## __Amazon Kinesis Firehose__
  - ### Features
    - For AppendOnly streams, Firehose will automatically scale to match your throughput.

## __Timestream InfluxDB__
  - ### Features
    - Adds 'allocatedStorage' parameter to UpdateDbInstance API that allows increasing the database instance storage size and 'dbStorageType' parameter to UpdateDbInstance API that allows changing the storage type of the database instance

# __2.30.7__ __2025-01-27__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds support for dynamic audio configuration and the ability to disable the deblocking filter for h265 encodes.

## __AWS IoT__
  - ### Features
    - Raised the documentParameters size limit to 30 KB for AWS IoT Device Management - Jobs.

## __AWS S3 Control__
  - ### Features
    - Minor fix to ARN validation for Lambda functions passed to S3 Batch Operations

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - Add support for the prompt caching feature for Bedrock Prompt Management

# __2.30.6__ __2025-01-24__
## __AWS CloudTrail__
  - ### Features
    - This release introduces the SearchSampleQueries API that allows users to search for CloudTrail Lake sample queries.

## __AWS SSO OIDC__
  - ### Features
    - Fixed typos in the descriptions.

## __AWS Transfer Family__
  - ### Features
    - Added CustomDirectories as a new directory option for storing inbound AS2 messages, MDN files and Status files.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds support for UpdateStrategies in EKS Managed Node Groups.

## __Amazon HealthLake__
  - ### Features
    - Added new authorization strategy value 'SMART_ON_FHIR' for CreateFHIRDatastore API to support Smart App 2.0

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Systems Manager doc-only update for January, 2025.

# __2.30.5__ __2025-01-23__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added "future" allocation type for future dated capacity reservation

## __Netty NIO HTTP Client__
  - ### Features
    - Adds ALPN H2 support for Netty client

# __2.30.4__ __2025-01-22__
## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Reuse connections that receive a 5xx service response.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive adds a new feature, ID3 segment tagging, in CMAF Ingest output groups. It allows customers to insert ID3 tags into every output segment, controlled by a newly added channel schedule action Id3SegmentTagging.

## __AWS Glue__
  - ### Features
    - Docs Update for timeout changes

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Adds multi-turn input support for an Agent node in an Amazon Bedrock Flow

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Rename WorkSpaces Web to WorkSpaces Secure Browser

## __Apache HTTP Client__
  - ### Bugfixes
    - Reuse connections that receive a 5xx service response.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Reuse connections that receive a 5xx service response.

# __2.30.3__ __2025-01-21__
## __AWS Batch__
  - ### Features
    - Documentation-only update: clarified the description of the shareDecaySeconds parameter of the FairsharePolicy data type, clarified the description of the priority parameter of the JobQueueDetail data type.

## __AWS IoT SiteWise__
  - ### Features
    - AWS IoT SiteWise now supports ingestion and querying of Null (all data types) and NaN (double type) values of bad or uncertain data quality. New partial error handling prevents data loss during ingestion. Enabled by default for new customers; existing customers can opt-in.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Logs__
  - ### Features
    - Documentation-only update to address doc errors

## __Amazon Cognito Identity Provider__
  - ### Features
    - corrects the dual-stack endpoint configuration for cognitoidp

## __Amazon Connect Service__
  - ### Features
    - Added DeleteContactFlowVersion API and the CAMPAIGN flow type

## __Amazon QuickSight__
  - ### Features
    - Added `DigitGroupingStyle` in ThousandsSeparator to allow grouping by `LAKH`( Indian Grouping system ) currency. Support LAKH and `CRORE` currency types in Column Formatting.

## __Amazon Simple Notification Service__
  - ### Features
    - This release adds support for the topic attribute FifoThroughputScope for SNS FIFO topics. For details, see the documentation history in the Amazon Simple Notification Service Developer Guide.

## __EMR Serverless__
  - ### Features
    - Increasing entryPoint in SparkSubmit to accept longer script paths. New limit is 4kb.

## __Emf Metric Logging Publisher__
  - ### Features
    - Added a new EmfMetricLoggingPublisher class that transforms SdkMetricCollection to emf format string and logs it, which will be automatically collected by cloudwatch.

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


 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.28.3__ __2024-09-17__
## __AWS CodeBuild__
  - ### Features
    - GitLab Enhancements - Add support for Self-Hosted GitLab runners in CodeBuild. Add group webhooks

## __AWS Lambda__
  - ### Features
    - Support for JSON resource-based policies and block public access

## __Amazon EC2 Container Service__
  - ### Features
    - This is a documentation only release to address various tickets.

## __Amazon Elastic Container Registry__
  - ### Features
    - The `DescribeImageScanning` API now includes `fixAvailable`, `exploitAvailable`, and `fixedInVersion` fields to provide more detailed information about the availability of fixes, exploits, and fixed versions for identified image vulnerabilities.

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation with configuration information about the BYOL model for RDS for Db2.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Support for additional levels of cross-account, cross-Region organizational units in Automation. Various documentation updates.

# __2.28.2__ __2024-09-16__
## __AWS Elemental MediaLive__
  - ### Features
    - Removing the ON_PREMISE enum from the input settings field.

## __AWS IoT__
  - ### Features
    - This release adds additional enhancements to AWS IoT Device Management Software Package Catalog and Jobs. It also adds SBOM support in Software Package Version.

## __AWS Organizations__
  - ### Features
    - Doc only update for AWS Organizations that fixes several customer-reported issues

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - This feature adds cross account s3 bucket and VPC support to ModelInvocation jobs. To use a cross account bucket, pass in the accountId of the bucket to s3BucketOwner in the ModelInvocationJobInputDataConfig or ModelInvocationJobOutputDataConfig.

## __Amazon Relational Database Service__
  - ### Features
    - Launching Global Cluster tagging.

## __Private CA Connector for SCEP__
  - ### Features
    - This is a general availability (GA) release of Connector for SCEP, a feature of AWS Private CA. Connector for SCEP links your SCEP-enabled and mobile device management systems to AWS Private CA for digital signature installation and certificate management.

# __2.28.1__ __2024-09-13__
## __AWS Amplify__
  - ### Features
    - Doc only update to Amplify to explain platform setting for Next.js 14 SSG only applications

## __AWS SDK for Java v2__
  - ### Features
    - Add support for specifying endpoint overrides using environment variables, system properties or profile files. More information about this feature is available here: https://docs.aws.amazon.com/sdkref/latest/guide/feature-ss-endpoints.html
    - Updated endpoint and partition metadata.

## __Amazon Interactive Video Service__
  - ### Features
    - Updates to all tags descriptions.

## __Amazon Interactive Video Service Chat__
  - ### Features
    - Updates to all tags descriptions.

## __Amazon S3__
  - ### Bugfixes
    - Fix issue where the `AWS_USE_DUALSTACK_ENDPOINT` environment variable and `aws.useDualstackEndpoint` system property are not resolved during client creation time.

## __Amazon S3 Control__
  - ### Bugfixes
    - Fix issue where the `AWS_USE_DUALSTACK_ENDPOINT` environment variable and `aws.useDualstackEndpoint` system property are not resolved during client creation time.

# __2.28.0__ __2024-09-12__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release includes support for dynamic video overlay workflows, including picture-in-picture and squeezeback

## __AWS Glue__
  - ### Features
    - AWS Glue is introducing two new optimizers for Apache Iceberg tables: snapshot retention and orphan file deletion. Customers can enable these optimizers and customize their configurations to perform daily maintenance tasks on their Iceberg tables based on their specific requirements.

## __AWS Storage Gateway__
  - ### Features
    - The S3 File Gateway now supports DSSE-KMS encryption. A new parameter EncryptionType is added to these APIs: CreateSmbFileShare, CreateNfsFileShare, UpdateSmbFileShare, UpdateNfsFileShare, DescribeSmbFileShares, DescribeNfsFileShares. Also, in favor of EncryptionType, KmsEncrypted is deprecated.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Added email MFA option to user pools with advanced security features.

## __Amazon EMR__
  - ### Features
    - Update APIs to allow modification of ODCR options, allocation strategy, and InstanceTypeConfigs on running InstanceFleet clusters.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for the os-upgrade pending maintenance action for Amazon Aurora DB clusters.

## __Amazon Simple Queue Service__
  - ### Features
    - The AWS SDK for Java now supports a new `BatchManager` for Amazon Simple Queue Service (SQS), allowing for client-side request batching with `SqsAsyncClient`. This feature improves cost efficiency by buffering up to 10 requests before sending them as a batch to SQS. The implementation also supports receive message polling, which further enhances throughput by minimizing the number of individual requests sent. The batched requests help to optimize performance and reduce the costs associated with using Amazon SQS.

## __Elastic Load Balancing__
  - ### Features
    - Correct incorrectly mapped error in ELBv2 waiters

## __Synthetics__
  - ### Features
    - This release introduces two features. The first is tag replication, which allows for the propagation of canary tags onto Synthetics related resources, such as Lambda functions. The second is a limit increase in canary name length, which has now been increased from 21 to 255 characters.


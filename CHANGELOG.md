 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.41.6__ __2026-01-12__
## __AWS Billing__
  - ### Features
    - Cost Categories filtering support to BillingView data filter expressions through the new costCategories parameter, enabling users to filter billing views by AWS Cost Categories for more granular cost management and allocation.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon SageMaker Service__
  - ### Features
    - Added ultraServerType to the UltraServerInfo structure to support server type identification for SageMaker HyperPod

## __IAM Policy Builder__
  - ### Bugfixes
    - Allow integer AWS account IDs and boolean values when reading IAM policies from JSON with `IamPolicyReader`.

## __Managed integrations for AWS IoT Device Management__
  - ### Features
    - This release introduces WiFi Simple Setup (WSS) enabling device provisioning via barcode scanning with automated network discovery, authentication, and credential provisioning. Additionally, it introduces 2P Device Capability Rediscovery for updating hub-managed device capabilities post-onboarding.

# __2.41.5__ __2026-01-09__
## __AWS Elemental MediaLive__
  - ### Features
    - MediaPackage v2 output groups in MediaLive can now accept one additional destination for single pipeline channels and up to two additional destinations for standard channels. MediaPackage v2 destinations now support sending to cross region MediaPackage channels.

## __AWS Glue__
  - ### Features
    - Adding MaterializedViews task run APIs

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix a race condition in aggregate ProfileFileSupplier that could cause credential resolution failures with shared DefaultCredentialsProvider.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds optional field "view" to GetMemory API input to give customers control over whether CMK encrypted data such as strategy decryption or override prompts is returned or not.

## __Amazon CloudFront__
  - ### Features
    - Added EntityLimitExceeded exception handling to the following API operations AssociateDistributionWebACL, AssociateDistributionTenantWebACL, UpdateDistributionWithStagingConfig

## __Amazon Transcribe Service__
  - ### Features
    - Adds waiters to Amazon Transcribe.

# __2.41.4__ __2026-01-07__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon WorkSpaces__
  - ### Features
    - Add StateMessage and ProgressPercentage fields to DescribeCustomWorkspaceImageImport API response.

# __2.41.3__ __2026-01-06__
## __AWS Cost Explorer Service__
  - ### Features
    - This release updates existing reservation recommendations API to support deployment model.

## __EMR Serverless__
  - ### Features
    - Added support for enabling disk encryption using customer managed AWS KMS keys to CreateApplication, UpdateApplication and StartJobRun APIs.

# __2.41.2__ __2026-01-05__
## __AWS Clean Rooms ML__
  - ### Features
    - AWS Clean Rooms ML now supports advanced Spark configurations to optimize SQL performance when creating an MLInputChannel or an audience generation job.

# __2.41.1__ __2026-01-02__
## __AWS Clean Rooms Service__
  - ### Features
    - Added support for publishing detailed metrics to CloudWatch for operational monitoring of collaborations, including query performance and resource utilization.

## __AWS SSO Identity Store__
  - ### Features
    - This change introduces "Roles" attribute for User entities supported by AWS Identity Store SDK.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Handles the edge case in Netty client where HTTP/2 stream gets cleaned up before metrics collection completes, causing NPE to be thrown. See [#6561](https://github.com/aws/aws-sdk-java-v2/issues/6561).

# __2.41.0__ __2025-12-30__
## __AWS SDK for Java V2__
  - ### Bugfixes
    - Ensure rpc 1.0/1.1 error code parsing matches smithy spec: use both __type and code fields and handle uris in body error codes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Don't use the value of AwsQueryError in json rpc/smithy-rpc-v2-cbor protocols.

## __Amazon Connect Service__
  - ### Features
    - Adds support for searching global contacts using the ActiveRegions filter, and pagination support for ListSecurityProfileFlowModules and ListEntitySecurityProfiles.

## __Apache5 HTTP Client__
  - ### Features
    - The Apache5 HTTP Client (`apache5-client`) is out of preview and now generally available.

## __Lambda Maven Archetype__
  - ### Features
    - Various Java Lambda Maven archetype improvements: use Java 25, use platform specific AWS CRT dependency, bump dependency version, and improve README. See [#6115](https://github.com/aws/aws-sdk-java-v2/issues/6115)

## __Managed Streaming for Kafka Connect__
  - ### Features
    - This change sets the KafkaConnect GovCloud FIPS and FIPS DualStack endpoints to use kafkaconnect instead of kafkaconnect-fips as the service name. This is done to match the Kafka endpoints.


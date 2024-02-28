 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.24.13__ __2024-02-28__
## __AWS Batch__
  - ### Features
    - This release adds Batch support for configuration of multicontainer jobs in ECS, Fargate, and EKS. This support is available for all types of jobs, including both array jobs and multi-node parallel jobs.

## __AWS Cost Explorer Service__
  - ### Features
    - This release introduces the new API 'GetApproximateUsageRecords', which retrieves estimated usage records for hourly granularity or resource-level data at daily granularity.

## __AWS IoT__
  - ### Features
    - This release reduces the maximum results returned per query invocation from 500 to 100 for the SearchIndex API. This change has no implications as long as the API is invoked until the nextToken is NULL.

## __AWS SDK for Java v2__
  - ### Features
    - Switching remaining AWS service clients onto the new SRA (Smithy Reference Architecture) identity and auth logic that was released in v2.21.0. For a list of individual services affected, please check the committed files.

## __AWS WAFV2__
  - ### Features
    - AWS WAF now supports configurable time windows for request aggregation with rate-based rules. Customers can now select time windows of 1 minute, 2 minutes or 10 minutes, in addition to the previously supported 5 minutes.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This release adds support to override search strategy performed by the Retrieve and RetrieveAndGenerate APIs for Amazon Bedrock Agents

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release increases the range of MaxResults for GetNetworkInsightsAccessScopeAnalysisFindings to 1,000.

## __Amazon S3__
  - ### Bugfixes
    - Fixed an issue in S3 multipart client that could cause `BlockingInputStreamAsyncRequestBody#writeInputStream` to get stuck if any of the multipart request fails. See [#4801](https://github.com/aws/aws-sdk-java-v2/issues/4801)

# __2.24.12__ __2024-02-27__
## __AWS Amplify UI Builder__
  - ### Features
    - We have added the ability to tag resources after they are created

## __AWS SDK for Java v2__
  - ### Bugfixes
    - upgrade netty version to 4.1.107.Final
        - Contributed by: [@sullis](https://github.com/sullis)

## __Amazon S3__
  - ### Features
    - Enable TransferListener when uploading with TransferManager with Java-based S3Client with multipart enabled

## __Contributors__
Special thanks to the following contributors to this release: 

[@sullis](https://github.com/sullis)
# __2.24.11__ __2024-02-26__
## __Amazon API Gateway__
  - ### Features
    - Documentation updates for Amazon API Gateway.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for gp3 data volumes for Multi-AZ DB Clusters.

## __Elastic Disaster Recovery Service__
  - ### Features
    - Added volume status to DescribeSourceServer replicated volumes.

## __Managed Streaming for Kafka Connect__
  - ### Features
    - Adds support for tagging, with new TagResource, UntagResource and ListTagsForResource APIs to manage tags and updates to existing APIs to allow tag on create. This release also adds support for the new DeleteWorkerConfiguration API.

# __2.24.10__ __2024-02-23__
## __AWS AppSync__
  - ### Features
    - Documentation only updates for AppSync

## __Amazon QLDB__
  - ### Features
    - Clarify possible values for KmsKeyArn and EncryptionDescription.

## __Amazon Relational Database Service__
  - ### Features
    - Add pattern and length based validations for DBShardGroupIdentifier

## __CloudWatch RUM__
  - ### Features
    - Doc-only update for new RUM metrics that were added

## __S3 Transfer Manager__
  - ### Features
    - Enable multipart configuration by default when creating a new S3TranferManager instance using the .create() method
    - Make Transfer Manager work by default with S3AsyncClient when multipart configuration is enabled.

# __2.24.9__ __2024-02-22__
## __AWS CRT-based S3 client__
  - ### Bugfixes
    - Fixed memory leak issue when a request was cancelled in the AWS CRT-based S3 client.

## __Amazon CloudWatch Internet Monitor__
  - ### Features
    - This release adds IPv4 prefixes to health events

## __Amazon Kinesis Video Streams__
  - ### Features
    - Increasing NextToken parameter length restriction for List APIs from 512 to 1024.

# __2.24.8__ __2024-02-21__
## __AWS Elemental MediaLive__
  - ### Features
    - MediaLive now supports the ability to restart pipelines in a running channel.

## __AWS IoT Events__
  - ### Features
    - Increase the maximum length of descriptions for Inputs, Detector Models, and Alarm Models

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Add content-length header in Json and  Xml Protocol Marshaller for String and Binary explicit Payloads.

## __Amazon Lookout for Equipment__
  - ### Features
    - This release adds a field exposing model quality to read APIs for models. It also adds a model quality field to the API response when creating an inference scheduler.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release adds support for sharing Systems Manager parameters with other AWS accounts.

# __2.24.7__ __2024-02-20__
## __AWS Lambda__
  - ### Features
    - Add .NET 8 (dotnet8) Runtime support to AWS Lambda.

## __Amazon DynamoDB__
  - ### Features
    - Publishing quick fix for doc only update.

## __Amazon Kinesis Firehose__
  - ### Features
    - This release updates a few Firehose related APIs.

# __2.24.6__ __2024-02-19__
## __AWS Amplify__
  - ### Features
    - This release contains API changes that enable users to configure their Amplify domains with their own custom SSL/TLS certificate.

## __AWS Config__
  - ### Features
    - Documentation updates for the AWS Config CLI

## __AWS MediaTailor__
  - ### Features
    - MediaTailor: marking #AdBreak.OffsetMillis as required.

## __Amazon Interactive Video Service__
  - ### Features
    - Changed description for latencyMode in Create/UpdateChannel and Channel/ChannelSummary.

## __Amazon Keyspaces__
  - ### Features
    - Documentation updates for Amazon Keyspaces

## __Amazon S3__
  - ### Features
    - Add support for pause/resume upload for TransferManager with Java-based S3Client that has multipart enabled

## __chatbot__
  - ### Features
    - This release adds support for AWS Chatbot. You can now monitor, operate, and troubleshoot your AWS resources with interactive ChatOps using the AWS SDK.

# __2.24.5__ __2024-02-16__
## __AWS Lambda__
  - ### Features
    - Documentation-only updates for Lambda to clarify a number of existing actions and properties.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Connect Participant Service__
  - ### Features
    - Doc only update to GetTranscript API reference guide to inform users about presence of events in the chat transcript.

## __Amazon EMR__
  - ### Features
    - adds fine grained control over Unhealthy Node Replacement to Amazon ElasticMapReduce

## __Amazon Kinesis Firehose__
  - ### Features
    - This release adds support for Data Message Extraction for decompressed CloudWatch logs, and to use a custom file extension or time zone for S3 destinations.

## __Amazon Relational Database Service__
  - ### Features
    - Doc only update for a valid option in DB parameter group

## __Amazon Simple Notification Service__
  - ### Features
    - This release marks phone numbers as sensitive inputs.

# __2.24.4__ __2024-02-15__
## __AWS Artifact__
  - ### Features
    - This is the initial SDK release for AWS Artifact. AWS Artifact provides on-demand access to compliance and third-party compliance reports. This release includes access to List and Get reports, along with their metadata. This release also includes access to AWS Artifact notifications settings.

## __AWS CodePipeline__
  - ### Features
    - Add ability to override timeout on action level.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Secrets Manager__
  - ### Features
    - Doc only update for Secrets Manager

## __Amazon Detective__
  - ### Features
    - Doc only updates for content enhancement

## __Amazon GuardDuty__
  - ### Features
    - Marked fields IpAddressV4, PrivateIpAddress, Email as Sensitive.

## __Amazon HealthLake__
  - ### Features
    - This release adds a new response parameter, JobProgressReport, to the DescribeFHIRImportJob and ListFHIRImportJobs API operation. JobProgressReport provides details on the progress of the import job on the server.

## __Amazon OpenSearch Service__
  - ### Features
    - Adds additional supported instance types.

## __Amazon Polly__
  - ### Features
    - Amazon Polly adds 1 new voice - Burcu (tr-TR)

## __Amazon SageMaker Service__
  - ### Features
    - This release adds a new API UpdateClusterSoftware for SageMaker HyperPod. This API allows users to patch HyperPod clusters with latest platform softwares.

# __2.24.3__ __2024-02-14__
## __AWS Control Tower__
  - ### Features
    - Adds support for new Baseline and EnabledBaseline APIs for automating multi-account governance.

## __AWS SDK for Java v2__
  - ### Features
    - Switching half of the AWS service clients onto the new SRA (Smithy Reference Architecture) identity and auth logic that was released in v2.21.0. For a list of individual services affected, please check the committed files.
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue where NPE would be thrown if there was an empty event in the input for an event streaming operation.

## __Amazon Lookout for Equipment__
  - ### Features
    - This feature allows customers to see pointwise model diagnostics results for their models.

## __Amazon Simple Storage Service__
  - ### Bugfixes
    - Fix for Issue [#4912](https://github.com/aws/aws-sdk-java-v2/issues/4912) where client region with AWS_GLOBAL calls failed for cross region access.

## __QBusiness__
  - ### Features
    - This release adds the metadata-boosting feature, which allows customers to easily fine-tune the underlying ranking of retrieved RAG passages in order to optimize Q&A answer relevance. It also adds new feedback reasons for the PutFeedback API.

# __2.24.2__ __2024-02-13__
## __AWS Marketplace Catalog Service__
  - ### Features
    - AWS Marketplace Catalog API now supports setting intent on requests

## __AWS Resource Explorer__
  - ### Features
    - Resource Explorer now uses newly supported IPv4 'amazonaws.com' endpoints by default.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon DynamoDB__
  - ### Features
    - Add additional logical operator ('and' and 'or') methods to DynamoDB Expression
        - Contributed by: [@akiesler](https://github.com/akiesler)

## __Amazon Lightsail__
  - ### Features
    - This release adds support to upgrade the major version of a database.

## __Amazon S3__
  - ### Features
    - Automatically trim object metadata keys of whitespace for `PutObject` and `CreateMultipartUpload`.

## __Amazon Security Lake__
  - ### Features
    - Documentation updates for Security Lake

## __URL Connection Client__
  - ### Bugfixes
    - Fix a bug where headers with multiple values don't have all values for that header sent on the wire. This leads to signature mismatch exceptions.

      Fixes [#4746](https://github.com/aws/aws-sdk-java-v2/issues/4746).

## __Contributors__
Special thanks to the following contributors to this release: 

[@akiesler](https://github.com/akiesler)
# __2.24.1__ __2024-02-12__
## __AWS AppSync__
  - ### Features
    - Adds support for new options on GraphqlAPIs, Resolvers and Data Sources for emitting Amazon CloudWatch metrics for enhanced monitoring of AppSync APIs.

## __Amazon CloudWatch__
  - ### Features
    - This release enables PutMetricData API request payload compression by default.

## __Amazon Neptune Graph__
  - ### Features
    - Adding a new option "parameters" for data plane api ExecuteQuery to support running parameterized query via SDK.

## __Amazon Route 53 Domains__
  - ### Features
    - This release adds bill contact support for RegisterDomain, TransferDomain, UpdateDomainContact and GetDomainDetail API.

# __2.24.0__ __2024-02-09__
## __AWS Batch__
  - ### Features
    - This feature allows Batch to support configuration of repository credentials for jobs running on ECS

## __AWS IoT__
  - ### Features
    - This release allows AWS IoT Core users to enable Online Certificate Status Protocol (OCSP) Stapling for TLS X.509 Server Certificates when creating and updating AWS IoT Domain Configurations with Custom Domain.

## __AWS Price List Service__
  - ### Features
    - Add Throttling Exception to all APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.
    - Updated internal core logic for signing properties with non-default values to be codegen based instead of set at runtime.

## __Amazon EC2 Container Service__
  - ### Features
    - Documentation only update for Amazon ECS.

## __Amazon Prometheus Service__
  - ### Features
    - Overall documentation updates.

## __Amazon S3__
  - ### Features
    - Overriding signer properties for S3 through the deprecated non-public execution attributes in S3SignerExecutionAttribute no longer works with this release. The recommended approach is to use plugins in order to change these settings.

  - ### Bugfixes
    - Fix bug where PUT fails when using SSE-C with Checksum when using S3AsyncClient with multipart enabled. Enable CRC32 for putObject when using multipart client if checksum validation is not disabled and checksum is not set by user

## __Braket__
  - ### Features
    - Creating a job will result in DeviceOfflineException when using an offline device, and DeviceRetiredException when using a retired device.

## __Cost Optimization Hub__
  - ### Features
    - Adding includeMemberAccounts field to the response of ListEnrollmentStatuses API.


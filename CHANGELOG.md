 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.54.4__ __2026-08-25__
## __AWS DevOps Agent Service__
  - ### Features
    - Adds the UpdateApprovalAction API for resolving agent action approvals in AWS DevOps Agent agent spaces.

## __AWS IoT__
  - ### Features
    - As part of this release, we are extending capability of AWS IoT Rules Engine to support IoT InfluxDB Action. The IoT InfluxDB action lets customers send messages from IoT sensors and applications to InfluxDB.

## __AWS SDK for Java v2__
  - ### Features
    - Extract the duplicated resolveMetricPublishers generator into ClientClassUtils, removing  verbatim duplication across the sync and async client generators. No change to generated client code or SDK behavior.
        - Contributed by: [@Se3do](https://github.com/Se3do)

  - ### Bugfixes
    - Fix an issue where futures from the async SDK clients don't complete with an exception when the client or its [scheduled executor service](https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/core/client/config/ClientOverrideConfiguration.html#scheduledExecutorService()) is closed. See [#7313](https://github.com/aws/aws-sdk-java-v2/issues/7313) for more details.

## __AWSMarketplace Metering__
  - ### Features
    - Updated documentation to clarify duplicate-billing prevention and BatchMeterUsage retry guidance

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Fleet feature to support Capacity Reservation Resource Groups with Amazon EC2 Capacity Blocks and interruptible Capacity Reservations

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - This feature would give customers the ability to tune TerminatedPodGcThreshold configuration in an Amazon EKS cluster.

## __Amazon Elastic VMware Service__
  - ### Features
    - EVS now supports i7i.metal-48xl EC2 bare metal instance type, delivering high random IOPS performance with real-time latency, ideal for IO intensive and latency-sensitive workloads such as transactional databases, real-time analytics, and AI ML pre-processing.

## __Amazon SQS__
  - ### Bugfixes
    - Fixed `SqsAsyncBatchManager.close()` re-sending the same buffered batch in a busy loop. On close, each buffered batch (including partial batches) is now flushed exactly once, and close waits a bounded timeout (approximately 5 seconds) for in-flight batch sends to complete so their callers receive the real result instead of a cancellation.

## __Auto Scaling__
  - ### Features
    - Adds support for Distribution Segments in mixed instances policies, providing ordered prioritization across On-Demand Capacity Reservations, Capacity Blocks, interruptible Capacity Reservations, and On-Demand capacity.

## __IAM Toolbox (Preview)__
  - ### Features
    - AWS Identity and Access Management (IAM) announces access troubleshooter, helping you debug access denied errors faster. Supported error messages now include an identifier you can use to retrieve detailed evaluations of the policies considered and their results. Preview in US East (N. Virginia).

## __Contributors__
Special thanks to the following contributors to this release: 

[@Se3do](https://github.com/Se3do)
# __2.54.3__ __2026-08-24__
## __AWS Batch__
  - ### Features
    - Doc Update, Add note that UpdatePolicy applies only to EC2 managed compute environments

## __AWS Elemental Inference__
  - ### Features
    - Added support for the GetFixture API, enabling customers to retrieve the details of a fixture from its fixture ID, and added the access role ARN to the CreateFeed, GetFeed, and UpdateFeed responses.

## __AWS Launch Wizard__
  - ### Features
    - Added accountConstraints and patternType to GetWorkload, ListWorkloads, GetWorkloadDeploymentPattern and ListWorkloadDeploymentPatterns for Launch Wizard

## __AWS Security Agent__
  - ### Features
    - Adding private and self-signed certificate configuration support for penetration tests

## __Amazon Aurora DSQL__
  - ### Features
    - Corrected the validation pattern on the ServiceName response field in the GetVpcEndpointServiceName API to match the values Amazon Aurora DSQL actually returns.

## __Amazon Bedrock__
  - ### Features
    - Adds support for specifying an inference profile ID or ARN, or an application inference profile ARN as the target model in CreateAdvancedPromptOptimizationJob.

## __Amazon Connect Contact Lens__
  - ### Features
    - This release adds the ExtractedInformation segment to the ListRealtimeContactAnalysisSegments API, enabling customers to retrieve information extracted from real-time contact analysis.

## __Amazon Connect Service__
  - ### Features
    - This release adds the ExtractedInformation segment to the ListRealtimeContactAnalysisSegmentsV2 API, enabling customers to retrieve information extracted from real-time contact analysis.

## __Managed Streaming for Kafka__
  - ### Features
    - Amazon MSK Replicator now supports OAuth authentication when connecting to external Apache Kafka clusters, enabling customers to replicate data from clusters that require OAuth for client authentication. This new capability is supported in all AWS Regions where MSK Express brokers are available.

## __Timestream InfluxDB__
  - ### Features
    - Service-managed parameter groups now only apply optimized defaults to DB Clusters automatically. New field effectiveDbParameterGroupIdentifier surfaces the parameter group actually applied.

# __2.54.2__ __2026-08-21__
## __AWS Backup__
  - ### Features
    - Updating CLI Docs for Backup Audit Manager List Job Summaries APIs.

## __AWS Device Farm__
  - ### Features
    - Added support to CreateRemoveAccessSession for selecting a server version on the mobile WebDriver endpoint.

## __AWS WAFV2__
  - ### Features
    - DataProtectionConfig field Key Documentation Update

## __Amazon Bedrock AgentCore__
  - ### Features
    - Increase spans count from 1k to 20k

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Update Dataset schema to THIRDPARTYEVALUATIONV1

## __Amazon CloudWatch__
  - ### Features
    - Allows customers to specify an initial warm up period to wait for metrics to arrive when creating metric or log alarms

## __Amazon Kinesis__
  - ### Features
    - Generate account endpoint for Kinesis Data Streams requests when the account ID is available

## __Netty NIO HTTP Client__
  - ### Features
    - Add support for Kerberos (SPNEGO) proxy authentication via the new `proxyAuthScheme` option on the Netty client's `ProxyConfiguration`. Setting `ProxyAuthScheme.NEGOTIATE` authenticates proxy CONNECT tunnels using the Kerberos ticket cache in the environment; a valid ticket-granting ticket must already exist (for example via `kinit`), and no password or keytab is read. `ProxyAuthScheme.BASIC` may also be set to select Basic authentication explicitly. See [#7033](https://github.com/aws/aws-sdk-java-v2/issues/7033).

  - ### Bugfixes
    - Fix `NettyNioAsyncHttpClient` errors when making requests in GraalVM native images, which can lead to timeout exceptions.
    - Fixed a `NullPointerException` in `HandlerSubscriber` that could intermittently fail async requests (such as S3 `PutObject`/`UploadPart`) when a channel writability change occurred during the `Expect: 100-continue` window before the request body subscription was established. See [#7271](https://github.com/aws/aws-sdk-java-v2/issues/7271).

# __2.54.1__ __2026-08-20__
## __ARC - Region switch__
  - ### Features
    - Adds support for Rds switchover read replica for Oracle databases in Region switch plans

## __AWS Amplify__
  - ### Features
    - Increased the maximum allowed length from 255 to 4,096 characters to support longer access tokens.

## __AWS Batch__
  - ### Features
    - AWS Batch now supports a new compute environment type that provides fully managed EC2 capacity with broader compute flexibility than Fargate, including GPU instances, bare metal, and specific instance type selection, without infrastructure management overhead.

## __AWS Direct Connect__
  - ### Features
    - This release adds custom route prefix pool allocations for Direct Connect. You can set IPv4 and IPv6 route prefix counts on private and transit virtual interfaces, and view pool size and unallocated counts on connections and LAGs, plus direct connect gateway attachment prefix allocation totals.

## __AWS Lambda__
  - ### Features
    - Adds support for full JSON resource-based policies, enabling customers to create, retrieve, update, and delete function resource policies as complete JSON documents.

## __Amazon CloudFront__
  - ### Features
    - Added SigV4a as a supported signing protocol for Origin Access Control (OAC), enabling CloudFront to sign requests to Amazon S3 Multi-Region Access Point (S3-MRAP) origins.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - EC2 marks UEFI instance metadata field as sensitive.

## __Amazon SageMaker Service__
  - ### Features
    - Added IAM Identity Center (IdC) support to CreatePartnerApp and UpdatePartnerApp APIs. Added Customer Managed Key (CMK) support to CreateMlflowApp and DescribeMlflowApp.

## __Amazon Simple Email Service__
  - ### Features
    - Amazon SES now supports per-message tracking overrides. You can use the new ConfigurationOverrides parameter in SendEmail and SendBulkEmail to enable or disable open and click tracking for individual messages without changing your account-level or configuration set settings.

## __PricingPlanManager__
  - ### Features
    - Documentation update for the CreateSubscription API to correct the default value of the approval mode parameter. The default value for paid subscriptions is MANUAL, not IMMEDIATE as previously documented. The default value remains IMMEDIATE for FREE tier subscriptions.

# __2.54.0__ __2026-08-19__
## __AWS Batch__
  - ### Features
    - AWS Batch now supports managing CloudWatch Container Insights on compute environments via CreateComputeEnvironment and UpdateComputeEnvironment.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports video cropping and output positioning. Use cropRectangle and outputPositionRectangle to position the encoded video within the output frame, with the surrounding area filled with black.

## __AWS SDK for Java v2__
  - ### Features
    - Add `SdkWarmUp` to warm up SDK request paths before a Coordinated Restore at Checkpoint (CRaC) checkpoint, including AWS Lambda SnapStart. `SdkWarmUp.warmUp()` warms every service client on the classpath. `SdkWarmUp.warmUp(Class...)` warms the service clients you name.
    - Cache auth scheme resolution results per operation
    - Enable compiled endpoint rules for all services by default, with a fix for region parameter handling in the generated endpoint providers.

  - ### Documentations
    - Add the @SdkAdvancedApi annotation to generated, operation event stream response handlers.

## __Account Access__
  - ### Features
    - Adds throttling exceptions to operation outputs that were previously inconsistent with other operations.

## __Amazon Bedrock AgentCore__
  - ### Features
    - AgentCore Memory now supports Flexible Namespaces and Non-Conversational Payloads in CreateEvent API

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - AgentCore Memory now supports Flexible Namespaces

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds support for EKS cluster certificate authorities (CA)

## __Amazon Redshift__
  - ### Features
    - Amazon Redshift enhanced System Table retention that allows customers to store their system table data directly in S3 Tables in customer's account instead of Redshift Managed Storage

## __Amazon VPC Lattice__
  - ### Features
    - Amazon VPC Lattice now supports modification of private DNS options on Service Network VPC Associations

## __Redshift Serverless__
  - ### Features
    - Amazon Redshift Enhanced System Table Retention that allows customers to store their system table data directly in S3 Tables in customer's account instead of Redshift Managed Storage


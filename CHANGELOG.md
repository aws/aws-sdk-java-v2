 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


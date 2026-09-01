 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.54.10__ __2026-09-01__
## __AWS Elemental MediaConvert__
  - ### Features
    - Adds support for AAC passthrough. Adds ManifestCues option to support HLS manifest Cue marker passthrough. Adds playback device compatibility mode for DASH H.265 outputs. Adds TTML caption styling options. Adds interlace mode support for XAVC HD Intra CBG profile.

## __AWS IoT SiteWise__
  - ### Features
    - AWS IoT SiteWise Scenario Discovery now supports mounting Amazon S3 data directly into pipeline task containers via S3 Access Points, and configuring additional ephemeral storage per task. Mount configurations can be overridden at execution time. See the API guide for details.

## __AWS Lambda__
  - ### Features
    - AWS Lambda now provides configurable control over S3 direct access, allowing you to explicitly enable or disable how functions stream file reads directly from S3 buckets. This gives you flexibility to tune data access behavior based on your workload requirements, independent of memory size.

## __AWS Marketplace Agreement Service__
  - ### Features
    - This release adds renewal support for AWS Marketplace private offers. Agreements report whether they renew and, if not, why. Renewal terms add price increases, renewal limits, renewal decision deadlines, and payment schedule templates. SearchAgreements adds filters.

## __AWS Marketplace Discovery__
  - ### Features
    - GetOfferTerms now returns renewalTerm for offers with pre-authorized renewals, exposing maxRenewals, lockoutPeriod, adjustmentDeadline, priceIncrease (fixed percentage or percentage range), and termTemplates (renewal payment schedules). Enables buyers to view renewal pricing and terms.

## __AWS SDK for Java v2__
  - ### Features
    - Removed the legacy interpreted endpoint rules code generation path. Compiled endpoint rules are now the only supported path. This includes removing the `enableGenerateCompiledEndpointRules` customization flag from all services and deleting the old interpreted runtime resources.
    - Updated endpoint and partition metadata.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Online evaluation configurations now support up to 25 evaluators. CloudWatch Logs data sources for online evaluation now support up to 10 log groups.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Update UserData and UploadPolicy shapes to use SecureBlob

## __Amazon GuardDuty__
  - ### Features
    - Amazon GuardDuty now supports custom detection rules, including APIs to manage rule associations and organization-level configurations.

## __Amazon Kinesis__
  - ### Features
    - Amazon Kinesis Data Streams now supports a dry run feature for data-plane APIs to validate the permissions and request parameters. If all checks complete successfully, the API returns a 'DryRunOperationException', confirming the request would have succeeded without the 'DryRun' parameter.

## __Amazon Lightsail__
  - ### Features
    - This release adds support for the Amazon Lightsail GetProfile API, which returns the profile for the specified account.

## __Amazon Simple Email Service__
  - ### Features
    - Added support for managing SMIME signing certificates for email identities, including associating, listing, and disassociating certificates. Added the UpdateConfigurationSet operation to configure message security options such as signing scheme.

## __Tax Settings__
  - ### Features
    - France and Monaco Additional Info changes

# __2.54.9__ __2026-08-31__
## __Agent Registry__
  - ### Features
    - Release HTTP and AGUI descriptors to the dataplane model

# __2.54.8__ __2026-08-31__
## __AWS Control Tower__
  - ### Features
    - Updated the descriptions for the AWS Control Tower ListEnabledControls API parameters to make them more accurate and intuitive.

## __AWS DevOps Agent Service__
  - ### Features
    - Adds support for Slack bidirectional communication configuration in AWS DevOps Agent agent spaces.

## __AWS Support__
  - ### Features
    - AWS Support now allows up to 10 attachments (150 MB each) per case correspondence, up from 3 at 5 MB. Customers can share large diagnostic logs, heap dumps, and packet captures directly in cases to reduce back-and-forth and speed up resolution. Available in US East, US West, and Europe (Ireland).

## __Agent Registry__
  - ### Features
    - AWS Agent Registry becomes Generally Available

## __Agent Registry Control__
  - ### Features
    - AWS Agent Registry becomes Generally Available

## __Amazon Connect Customer Profiles__
  - ### Features
    - This release introduces new APIs for segment membership events allowing segment definition membership events to be exported to a kinesis stream for downstream processing. Additionally, includes new calculated attribute statistic and 2 new segment dimension types.

## __Amazon Connect Service__
  - ### Features
    - Added support for global routing on Amazon Connect Global Resiliency instances. New APIs GetCrossRegionRouting and UpdateCrossRegionRouting allow you to view and control cross-region contact routing between linked instances, so both Regions are active at all times.

## __Amazon Kinesis__
  - ### Features
    - Adds support for data delivery to Amazon S3 Tables (Apache Iceberg) and general purpose Amazon S3 buckets with new CreateChannel, UpdateChannel, DeleteChannel, DescribeChannel, and ListChannels APIs for Amazon Kinesis Data Streams.

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - AWS End User Messaging SMS now returns ConditionalBehavior on DescribeRegistrationFieldDefinitions, allowing you to programmatically discover which registration fields are required, optional, or disallowed based on the values of other fields in the same form.

## __Amazon QuickSight__
  - ### Features
    - This release adds support for managing apps in Amazon QuickSight with ListApps, SearchApps, DescribeApp, DescribeAppPermissions, UpdateAppPermissions, and DeleteApp

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker Batch Transform now supports G6e instances, powered by NVIDIA L40S Tensor Core GPUs. G6e instances are the most cost-efficient GPU instances for deploying generative AI models and the highest-performance GPU instances for spatial computing workloads.

## __Amazon Workspaces Instances__
  - ### Features
    - Amazon WorkSpaces Core managed instances now support nested virtualization. Customers can enable nested virtualization with supported instance types at launch via CpuOptions.NestedVirtualization in CreateWorkspaceInstance to run hypervisors and virtual machines inside their WorkSpaces Instance.

## __Managed Streaming for Kafka Connect__
  - ### Features
    - Amazon MSK Connect now supports restarting newly created connectors via the asynchronous RestartConnector API. Restart all tasks or only failed tasks, while preserving configuration and committed offsets. This returns a connector operation ARN that you can track with DescribeConnectorOperation.

# __2.54.7__ __2026-08-28__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Reduce memory allocations for non-streaming requests made with asycn clients.

## __Agents for Amazon Bedrock__
  - ### Features
    - Adds an optional syncSchedule field to CreateDataSource and UpdateDataSource for Managed Knowledge Bases data source connectors, so a data source can sync automatically on a daily, weekly, or monthly schedule.

## __Amazon Bedrock AgentCore__
  - ### Features
    - AgentCore Memory now supports direct ingestion into long-term memory via IngestData API

## __Amazon Cognito Identity Provider__
  - ### Features
    - Adds two new operations - GetClientToken which allows M2M auth through the SDK, and DescribeTermsByClient to find which Terms are associated with a user-pool client without knowing the Terms resource id.

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon Elastic Container Service - This release adds support for early success criteria on ECS rolling deployments, letting deployment complete once a configurable percentage of tasks are healthy, with configurable BLOCKING (required) or DEFERRED (asynchronous) cleanup of previous service revisions.

## __Amazon HealthLake__
  - ### Features
    - New HealthLake API, RestoreFHIRDatastore, providing the capability to restore active datastores to a point in time within the last 30 days or recover a deleted datastore from the delete snapshot.

## __Partner Central Selling API__
  - ### Features
    - Releasing PARC, new APN Program that lets sellers add solftware revenue details to aws opportunity summary

# __2.54.6__ __2026-08-27__
## __AWS CodeDeploy__
  - ### Features
    - Added a deploymentMode parameter to CreateDeployment. Set it to RESTART to restart an EC2 and on-premises fleet, using the last successful revision, honoring Deployment Configuration.

## __Amazon CloudWatch Logs__
  - ### Features
    - Added resultCount to QueryStatistics in GetQueryResults. This field returns the total number of output rows in the final result set, helping customers programmatically determine whether a query produced results after all operations including post-aggregation filters.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Adds the AdminDeleteSoftwareToken API operation, enabling administrators to remove a user's registered TOTP (software token) MFA configuration from a user pool.

## __Amazon DataZone__
  - ### Features
    - Add cascadeDelete to DeleteDomain. When specified, DataZone recursively deletes all projects, environments, subscriptions, and their underlying AWS resources before removing the domain. Deletion progress is reported via deleteProgress and resource failures via failureReasons on GetDomain.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - EC2 allows AMI owners to define compatible instance types on their AMIs, blocking RunInstances calls automatically for launches on non-permitted instance types.

## __Amazon OpenSearch Service__
  - ### Features
    - Updating SDK and CLI documentation for AttachDataSource API.

## __Amazon Relational Database Service__
  - ### Features
    - Adding support for the full snapshot size, in bytes, of DB instance snapshots.

## __Lambda MicroVMs__
  - ### Features
    - Added InsufficientCapacityException to RunMicrovm for capacity-related failures. Added lifecycle status field (AVAILABLE, DEPRECATED) to ListManagedMicrovmImageVersions. Added ConflictException to CreateMicrovmAuthToken and CreateMicrovmShellAuthToken for unregistered MicroVMs.

# __2.54.5__ __2026-08-26__
## __AWS CRT Async HTTP Client__
  - ### Bugfixes
    - Fixed an issue where an error signaled by a request body publisher was printed to stderr on a CRT event loop thread instead of failing the request with the original error.

## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Fixed an issue where an error thrown while reading the request body stream was printed to stderr on a CRT event loop thread instead of failing the request with the original error. See [#6715](https://github.com/aws/aws-sdk-java-v2/issues/6715).

## __AWS CRT-based S3 Client__
  - ### Bugfixes
    - Fixed an issue where an error signaled by a request body publisher (for example via `AsyncRequestBody.fromPublisher`) was printed to stderr on a CRT event loop thread instead of failing the operation; the operation's future now completes with the original publisher error. See [#6715](https://github.com/aws/aws-sdk-java-v2/issues/6715).

## __AWS DevOps Agent Service__
  - ### Features
    - AWS DevOps Agent now supports trigger filter groups for Release Readiness Review, letting you control when the capability auto-triggers based on webhook events and target branches.

## __AWS License Manager User Subscriptions__
  - ### Features
    - Released support for License Expiry field in ListProductSubscriptions API

## __AWS Network Firewall__
  - ### Features
    - Adding new status enum for Firewalls.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds deleting state to possible VPC States.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker AI now supports ml.g7 instances for model optimization. You can now run model optimization jobs on ml.g7 instances, in supported AWS Regions.

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


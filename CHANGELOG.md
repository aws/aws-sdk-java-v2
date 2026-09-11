 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.54.17__ __2026-09-11__
## __AWS Batch__
  - ### Features
    - Added new bulk job APIs (CancelJobs, TerminateJobs, TerminateServiceJobs) and new fields on ListJobs and ListServiceJobs responses. This allows customers to cancel or terminate multiple jobs in a single request. ListJobs and ListServiceJobs responses now include isCancelled and isTerminated fields.

## __AWS Elemental MediaConvert__
  - ### Features
    - Adds Dolby Vision metadata to Probe results, including profile, level, and presence of the RPU, base layer, and enhancement layer. Adds video sample and display aspect ratios. Adds the UnprocessableEntityException (HTTP 422) error to Probe for recognized but malformed or corrupt inputs.

## __AWS Invoicing__
  - ### Features
    - Add ListProcurementPortals and ListProcurementPortalSuppliers APIs to retrieve AWS-supported 3rd party procurement portals and their suppliers for e-invoice delivery and purchase order retrieval.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.
    - Upgrade Netty to 4.1.138

## __Amazon EC2 Container Service__
  - ### Features
    - This feature adds support for setting the cpu architecture type that should be used to launch tasks for an Express Gateway Service.

## __Amazon Lightsail__
  - ### Features
    - Amazon Lightsail now lets you serve website content from a private Lightsail bucket through a Lightsail distribution. This release adds enablePrivateOriginAccess to the CreateDistribution and UpdateDistribution actions, plus new defaultRootObject and customErrorResponses options.

## __Amazon S3__
  - ### Bugfixes
    - Prevent the CRT-based S3 async client from replacing a pre-existing destination when using `getObject(request, Path)`. Failed or cancelled downloads now delete files they create. Existing destinations surface `FileAlreadyExistsException`; callers that require replacement can use `AsyncResponseTransformer.toFile` with `FileTransformerConfiguration.defaultCreateOrReplaceExisting()`. This fixes an inconsistency introduced in 2.32.11.

## __Amazon Simple Storage Service__
  - ### Features
    - Updated S3 Object Lock Default Retention documentation.

# __2.54.16__ __2026-09-10__
## __AWS Outposts__
  - ### Features
    - Added fields to identify Outpost generation and rack scaling configuration on Outpost and CatalogItem resources.

## __AWS Resilience Hub V2__
  - ### Features
    - This release adds the ListTestRunSourceEvents and ListTestRunDependencies APIs, which return the alarm state changes during a test run and the dependencies the run blocked.

## __AWS SDK for Java v2__
  - ### Features
    - Enable BDD Endpoints for a small subset of services.
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - TwelveLabs Marengo 3.0 is now an embedding model option in Amazon Bedrock Managed Knowledge Base. Create multimodal embeddings for video, audio, and image content that capture visual scenes, speech, and video cues, not just transcribed text.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - The CreateImage API now supports a BootModeOverride parameter to explicitly set UEFI boot mode on a new AMI, overriding the source instance's inherited boot mode.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds the ability for customers to attach customer owned Elastic Network Interfaces (ENIs) to HyperPod cluster nodes.

## __RTBFabric__
  - ### Features
    - AWS RTB Fabric now lets you control how traffic is routed to your responder gateway across Availability Zones. Set the new clientRoutingPolicy parameter  to keep traffic within the same Availability Zone or distribute traffic across all Availability Zones.

# __2.54.15__ __2026-09-09__
## __AWS Elemental Inference__
  - ### Features
    - This release adds contextual metadata, a feed output type that generates a descriptive summary of your media content along with IAB taxonomy and GARM suitability classifications. It also adds feed resource policies for granting cross-account access to a feed.

## __AWS Elemental MediaLive__
  - ### Features
    - MediaLive now supports Manual Style Control for vertical caption positioning in TTML, WebVTT, and Embedded captions, Contextual Metadata Enrichment via Elemental Inference, and an Output Usage field on MediaPackage v2 for Dynamic Multiview validation.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - Dynamic Multiview enables viewers to watch multiple live video streams in a single combined output. Viewers can select from 6 preset tiled layouts. Create MediaPackage channels with Input Type MULTIVIEW and configure Available Layouts and Available Sources. See the API Documentation for details.

## __AWS Lambda__
  - ### Features
    - Updates documentation for lambda function timeout.

## __AWS MediaTailor__
  - ### Features
    - Added the AWS Service Request function type for MediaTailor Functions, enabling authenticated requests to AWS Elemental Inference for contextual ad targeting during ad insertion.

## __AWS Parallel Computing Service__
  - ### Features
    - This release adds support for custom Gres.conf configuration and Slurm version 26.05 in AWS PCS. Customers can now specify generic resource (GRES) settings to control how GPUs and other resources are configured and shared on their compute node groups.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed an intermittent NullPointerException when downloading a single-part object with a multipart-enabled async S3 client. A race between the emit loop and subscription cancellation could dereference a cleared subscriber reference; the reference is now stable and reads are null-safe.
        - Contributed by: [@cthiebault](https://github.com/cthiebault)

## __Amazon Connect Service__
  - ### Features
    - Add metric configuration field to evaluation forms and ListEvaluationFormAIVersions API for retrieving AI-generated evaluation form versions

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for sharing Amazon EBS volumes across AWS accounts using AWS Resource Access Manager (RAM). Consuming accounts can view shared volume metadata and create copies of shared volumes within the same Availability Zone, with optional re-encryption using their own KMS key.

## __Apache HTTP Client__
  - ### Deprecations
    - `apache-client` is now deprecated in favor of `apache5-client`. [Apache HttpClient 4.x](https://hc.apache.org/status.html) is in maintenance mode, receiving fixes only for major defects and security issues.

## __Contributors__
Special thanks to the following contributors to this release: 

[@cthiebault](https://github.com/cthiebault)
# __2.54.14__ __2026-09-08__
## __AWS CloudTrail__
  - ### Features
    - Adds support for the RecursiveLogging trail setting, which suppresses recursive events generated when CloudTrail delivers logs to a trail's destinations.

## __AWS S3 Control__
  - ### Features
    - Adds support for Amazon S3 Object Lock variable retention.  Existing S3 APIs that support S3 Object Lock parameters now support two new parameters EventHold and EventHoldDuration at the object level, and DefaultEventHoldDuration at the bucket level.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Prevent `AsyncResponseTransformer.toFile` from deleting a pre-existing destination when a request fails before the SDK opens the file. Calls using `CREATE_NEW` now preserve the existing file and surface `FileAlreadyExistsException` instead of allowing a retry to overwrite it.

## __Amazon Appflow__
  - ### Features
    - Amazon AppFlow now supports key pair (RSA private key) authentication for the Snowflake connector. You can provide a privateKey in SnowflakeConnectorProfileCredentials, and password is no longer required. This is a non-breaking, additive change available via the AWS SDK and CLI.

## __Amazon Connect Service__
  - ### Features
    - Releasing workload types feature. A proper launch announcement or details will follow up.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds the InterfaceTypes field to NetworkCardInfo in the DescribeInstanceTypes response. This field identifies the network interface types supported by each network card.

## __Amazon Omics__
  - ### Features
    - Added support for session policies in AWS HealthOmics Workflows, allowing customers to scope down IAM permissions for individual workflow runs without modifying the service role.

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - This feature will allow customers to specify an area-code when requesting a 10DLC number. Why it matters- Customers can now select a number that matches where their business is located.

## __Amazon SageMaker Service__
  - ### Features
    - Add support for InstancePreferences list for multiple instance type input support on SageMaker Training and Processing

## __Amazon Simple Storage Service__
  - ### Features
    - Adds support for Amazon S3 Object Lock variable retention.  Existing S3 APIs that support S3 Object Lock parameters now support two new parameters EventHold and EventHoldDuration at the object level, and DefaultEventHoldDuration at the bucket level.

## __Application Migration Service__
  - ### Features
    - This release adds support for configuring the EBS volume initialization rate and delete on termination behavior in launch configuration template

## __S3 Event Notification__
  - ### Features
    - Add support for ObjectRetention to S3EventNotifications.

## __URL Connection HTTP Client__
  - ### Bugfixes
    - Allow retries when the URL Connection HTTP Client encounters an IOException or NullPointerException while accessing request or response body streams.

# __2.54.13__ __2026-09-04__
## __AWS MediaTailor__
  - ### Features
    - Elemental MediaTailor now supports two new Monetization Functions lifecycle hooks, Post Ads Response and Pre Manifest Insertion, and a VAST Request function type that calls a VAST or VMAP ad server. This release also adds Yield Optimization with demand from Amazon Publisher Services.

## __AWS SDK for Java v2__
  - ### Features
    - Add support for code generating Endpoint Resolvers based on BDD rather than rules. Adds single element endpoint cache to generated BDD based resolvers. BDD based Endpoint Resolvers are generated only when the BDD model is present on a service.

## __Amazon Bedrock__
  - ### Features
    - New AWS REVIEW mode as supported data retention mode for Bedrock models

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds support for ValidateSecurityGroupQuotasForInterface, an API that specifically authorized AWS services use to validate security group rule quotas before creating an elastic network interface.

## __Service Quotas__
  - ### Features
    - Service Quotas adds the AdjustableAtLevel property to QuotaContext, indicating whether a quota is adjustable at the account or resource level.

# __2.54.12__ __2026-09-03__
## __AWS End User Messaging Social__
  - ### Features
    - Adding support for WhatsApp Flows with endpoints.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix `ApiCallDuration` so that it measures the whole API call. It previously started after marshalling had already completed, and on asynchronous clients it also started after endpoint resolution, auth scheme resolution, request compression and checksum computation, and so understated the reported duration. The metric is now measured identically for synchronous and asynchronous clients and matches the documented formula. Reported `ApiCallDuration` values will increase but do not reflect changes in actual round-trip latency.

## __AWS Step Functions__
  - ### Features
    - Updates Step Functions API documentation around CloudTrail, Execution name reuse and sort order of ListExecutions API

## __AWS Transfer Family__
  - ### Features
    - AWS Transfer Family SFTP Connectors now support specifying an ordered list of AWS Secrets Manager version stages for secret retrieval. This enables seamless credential rotation workflows where external partners may take time to update their systems with new credentials.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds log group name prefix trace source selection, custom or source log group result destinations, and metrics namespace customization

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - AgentCore Identity adds Consent Portal APIs to manage portals that let end users grant OAuth authorization for agents to access resources. AgentCore Evaluation adds trace source selection by log group prefix, custom or source log group result destinations, and metrics namespace customization.

## __Amazon Connect Service__
  - ### Features
    - This release enables TagOnCreate for Rule resource on CreateRule API. It also introduces a new field called PreEvaluationFilters to Rule resource, thereby impacting all Create, Update, Describe and Search APIs for Rules

## __Amazon EC2 Container Service__
  - ### Features
    - Adds a critical parameter to the Amazon ECS managed daemon APIs that controls whether a daemon task failure drains the container instance. Non-critical daemon failures no longer drain the instance or block instance registration.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Deprecate EncryptionConfig resources field. Amazon EKS encrypts all Kubernetes API data with envelope encryption by default for clusters running Kubernetes version 1.28 or higher, so this field no longer affects which resources are encrypted.

## __Amazon Elastic VMware Service__
  - ### Features
    - Amazon EVS now allows users to set, update, and retrieve values for parameters that apply across all EVS Environments in their account at a regional level, such as the VCF License portability core count.

## __Amazon GuardDuty__
  - ### Features
    - Adding support for Sequence Activities in GuardDuty Findings

## __Amazon Transcribe Service__
  - ### Features
    - Amazon Transcribe now supports specifying up to 29 PII entity types in the ContentRedaction configuration of a StartTranscriptionJob request, allowing all supported entity types to be redacted in a single batch transcription job.

## __Elastic Disaster Recovery Service__
  - ### Features
    - AWS Elastic Disaster Recovery now includes source server architecture in SourceProperties to identify x86 and ARM64 systems.

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for sending TCP resets for Gateway Load Balancer when a flow's idle timeout expires, or when a target becomes unhealthy or is deregistered. This adds updates the CLI documentation.

# __2.54.11__ __2026-09-02__
## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports AB forensic video watermarking

## __Amazon AppIntegrations Service__
  - ### Features
    - This release adds a force parameter to DeleteApplication and a ConflictException to UpdateApplication, letting customers delete applications with existing associations in one call and get a clear error when an update conflicts with the application's current state.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Batch evaluation now supports up to 10 CloudWatch log groups per CloudWatchLogsSource

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support to retain interruptible Capacity Reservations in an active state when all capacity is reclaimed.

## __Amazon SageMaker Feature Store Runtime__
  - ### Features
    - Amazon SageMaker Feature Store now supports the UpdateRecord API, enabling partial updates to individual feature values in an existing Online Store record without rewriting the entire record. This reduces write payloads and latency for high-frequency feature-level writes .

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker Feature Store now supports the Standard V2 online store type, which enables feature-level writes to feature groups. You can select Standard V2 when creating a feature group, and update the storage type of an existing feature group via UpdateFeatureGroup.

## __AmazonMWAA__
  - ### Features
    - Enabled customers to clear optional S3 paths (plugins, requirements, and startup script) for their Amazon MWAA environments by accepting empty strings for the associated fields in UpdateEnvironment requests.

## __Application Migration Service__
  - ### Features
    - AWS Transform for migrations adds a second network migration option - apply your source security posture to existing VPCs. Upload a source network file with firewall rules, tag the in-scope VPCs, and AWS Transform matches source subnets to them by CIDR and generates the security groups.

## __odb__
  - ### Features
    - Adds the ListFlexComponents API for listing the flex components available for a given DB system shape.

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


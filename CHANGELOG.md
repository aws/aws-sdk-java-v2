 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.44.8__ __2026-05-18__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Access Analyzer__
  - ### Features
    - Services manage service-linked analyzers through dedicated APIs - CreateServiceLinkedAnalyzer and DeleteServiceLinkedAnalyzer that separate service-linked specific operations from customer-managed operations. It also shows up in ListAnalyzers and GetAnalyzer responses.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect Cases now supports SLA durations of up to 2 years (1,051,200 minutes), increased from the previous maximum of 90 days (129,600 minutes). This enables you to track long-running service level agreements for cases that require extended resolution timelines.

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS now supports Pause lifecycle hooks for service deployments, allowing customers to automatically pause deployments at specified stages and use the new ContinueServiceDeployment API to continue or roll back with confidence.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon VPC IP Address Manager (IPAM) now supports tags on IPAM pool allocations, enabling all standard tagging features for allocations including tag-on-create.

## __Amazon Elastic VMware Service__
  - ### Features
    - Amazon EVS now supports up to 32 hosts per EVS environment, increasing the previous host limit to allow a larger scale of VMware workload deployments and reduce operational overhead.

## __Amazon Interactive Video Service__
  - ### Features
    - Adds support for up to 3 mediaTailorPlaybackConfiguration objects in an ad configuration resource

## __Amazon QuickSight__
  - ### Features
    - Support for dataset enrichment and geo spatial in new data preparation experience

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Don't force Netty's unpooled ByteBuffer allocator when using the JDK SSL provider. The underlying Netty issue (netty/netty#9768) has been fixed in later versions.
        - Contributed by: [@olivergillespie](https://github.com/olivergillespie)

## __Contributors__
Special thanks to the following contributors to this release: 

[@olivergillespie](https://github.com/olivergillespie)
# __2.44.7__ __2026-05-15__
## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for AvailabilityStartTimeConfiguration in MediaPackageV2 DASH manifests

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Logs__
  - ### Features
    - Service Release Notes

## __Partner Central Selling API__
  - ### Features
    - Service Release Notes

# __2.44.6__ __2026-05-14__
## __AWS Data Exchange__
  - ### Features
    - Add support for SendApiAsset operation.

## __AWS Database Migration Service__
  - ### Features
    - Service Release Notes

## __AWS Glue__
  - ### Features
    - Release --has-databases parameter for AWS Glue get-catalogs API, which filters catalog responses to include only those capable of containing databases, excluding parent catalogs that hold only other catalogs. Remove model-level validation on partition index list size for AWS Glue tables.

## __AWS SDK for Java v2__
  - ### Features
    - Support prefix headers (header maps) in Rest-Json
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Optimized GC usage (specifically G1GC humongous allocations) in JSON marshalling

## __Amazon Bedrock__
  - ### Features
    - Advanced Prompt Optimization (AdvPO) allows you to optimize and migrate your prompts for any model on Bedrock by automatically evaluating responses and rewriting prompts to improve performance. This release provides a programmatic way to create, get, list, stop, and delete AdvPO jobs.

## __Amazon CloudFront__
  - ### Features
    - Adding a new boolean for OCSP Revocations in Viewer mTLS Create and Update APIs, and adding a new 'Passthrough' option for TrustStore modes

## __Amazon DataZone__
  - ### Features
    - Adds support for SageMaker Unified Studio notebook operations, including notebook import and export

## __Amazon Managed Grafana__
  - ### Features
    - Adds support for dual-stack (IPv4 and IPv6) connectivity to Amazon Managed Grafana workspaces. Customers can configure the ipAddressType parameter when creating or updating a workspace to choose between IPv4-only or dual-stack (IPv4 and IPv6) access.

## __Amazon Q Connect__
  - ### Features
    - ListModels is an API that returns the available AI models for a Connect Assistant based on its region and AI prompt type.

## __Application Migration Service__
  - ### Features
    - Introducing new option for security groups mapping - with MAP-DHCP the service translates security rules from your source environment with DHCP compatibility.

# __2.44.5__ __2026-05-13__
## __ARC - Region switch__
  - ### Features
    - Service Release Notes

## __AWS Batch__
  - ### Features
    - Adds a billing callout to docs regarding using the CE Scale Down Delay feature

## __AWS End User Messaging Social__
  - ### Features
    - Adds parameters to call the GetWhatsAppMessageTemplate and UpdateWhatsAppMessageTemplate APIs with a template name and language code in place of the template ID. Linked WhatsApp accounts also describe whether the WABA is onboarded to Meta's Marketing Messages API.

## __AWS Glue__
  - ### Features
    - AWS Glue now defaults the job timeout to 480 minutes for Glue version 5.0 and later when no timeout value is specified. The default remains 2,880 minutes for Glue version 4.0 and earlier.

## __AWS Parallel Computing Service__
  - ### Features
    - Add support for Amazon EC2 Interruptible-ODCR

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Upgrade Jackson to 2.21.3

## __AWS Security Agent__
  - ### Features
    - Add support for code reviews, a new resource type that enables automated security-focused static analysis of source code repositories.

## __AWS Step Functions__
  - ### Features
    - Service Release Notes

## __AWSBillingConductor__
  - ### Features
    - Add ConflictException to UpdateCustomLineItem operation.

## __Amazon Aurora DSQL__
  - ### Features
    - Added support for Amazon Aurora DSQL change data capture (CDC) streams that deliver row-level database changes to Amazon Kinesis in JSON format. Includes CreateStream, GetStream, ListStreams, and DeleteStream operations.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Service Release Notes

## __Amazon Connect Cases__
  - ### Features
    - Amazon Connect Cases now supports SLA durations of up to 2 years (1,051,200 minutes), increased from the previous maximum of 90 days (129,600 minutes). This enables you to track long-running service level agreements for cases that require extended resolution timelines.

## __Amazon Connect Service__
  - ### Features
    - This change added three new EventSourceName for schedule notification feature

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Include length limits in the SDK and documentation for text fields in Image (AMI) APIs such as the image name and description

## __Amazon Elasticsearch Service__
  - ### Features
    - Service Release Notes

## __Amazon Lightsail__
  - ### Features
    - Added OriginIpAddressTypeEnum (ipv4, ipv6, dualstack) and ipAddressType field to Origin and InputOrigin structures for Lightsail CDN distributions. Allows customers to specify how the distribution connects to origins, using IPv4, IPv6, or dualstack networking

## __Amazon OpenSearch Service__
  - ### Features
    - Adds support for AutomatedSnapshotPauseOptions.

## __Amazon QuickSight__
  - ### Features
    - Adds five new custom permission option for Quick Apps so that these capabilities can be controlled by public SDK and CLI.

## __Amazon Redshift__
  - ### Features
    - Added rg.xlarge and rg.4xlarge to valid NodeType values and updated documentation for CreateCluster, ModifyCluster, ResizeCluster, and RestoreFromClusterSnapshot APIs to reflect RG node type support.

## __Amazon S3__
  - ### Bugfixes
    - Fixed request-level override configuration not being propagated to sub-requests during multipart uploads and copies in the S3 AsyncClient.
        - Contributed by: [@dbadaya1](https://github.com/dbadaya1)

## __Amazon SageMaker Service__
  - ### Features
    - Adds execution role session name mode to reflect user identity in Studio. Adds Flexible Training Plans on Studio apps. Adds restricted model packages to control access to proprietary model artifacts via IAM. Fixed instance type parity between inference endpoints and managed shadow tests.

## __AmazonConnectCampaignServiceV2__
  - ### Features
    - This release added support for Outbound Campaign timezone detection using all available contact methods

## __Partner Central Account API__
  - ### Features
    - Service Release Notes

## __RTBFabric__
  - ### Features
    - Customers can now configure custom domain names for their RTB Fabric gateways. This enables partners to use their own branded domain for RTB traffic instead of the default rtbfabric endpoint

## __Contributors__
Special thanks to the following contributors to this release: 

[@dbadaya1](https://github.com/dbadaya1)
# __2.44.4__ __2026-05-07__
## __AWS Billing and Cost Management Data Exports__
  - ### Features
    - With this release, customers can configure their data exports to generate additional integration artifacts for Athena and Redshift.

## __AWS Invoicing__
  - ### Features
    - Updated ListInvoiceSummaries API to add new ReceiverRole filter in Request and Response

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Launching AgentCore payments - a capability that provides secure, instant microtransaction payments for AI agents to access paid APIs, MCP servers, and content. It handles payment processing for x402 protocol, payment limits, and 3P wallet integrations with Coinbase CDP and Stripe (Privy).

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Launching AgentCore payments - a capability that provides secure, instant microtransaction payments for AI agents to access paid APIs, MCP servers, and content. It handles payment processing for x402 protocol, payment limits, and 3P wallet integrations with Coinbase CDP and Stripe (Privy).

## __Amazon Elastic Compute Cloud__
  - ### Features
    - DescribeInstanceTypes now accepts an IncludeUnsupportedInRegion parameter. When set, the response also lists instance types that are not available in the current Region. Each instance type includes a SupportedInRegion field indicating its regional availability.

## __Amazon GuardDuty__
  - ### Features
    - This is a documentation update

## __Amazon Route 53 Resolver__
  - ### Features
    - Adds supports for DNS64 on inbound endpoints and IPv6 forwarding through the internet gateway (IGW) on outbound endpoints, making it easier to manage hybrid DNS across IPv4 and IPv6 networks.

# __2.44.3__ __2026-05-06__
## __AWS Glue__
  - ### Features
    - Adds support for a CustomLogGroupPrefix parameter in StartDataQualityRulesetEvaluationRun to specify custom CloudWatch log group paths, and a RulesetName filter in ListDataQualityRulesetEvaluationRuns to filter evaluation runs by ruleset name.

## __AWS SDK for Java v2__
  - ### Features
    - Update Netty to 4.1.133

## __AWS SecurityHub__
  - ### Features
    - Release GenerateRecommendedPolicyV2 and GetRecommendedPolicyV2 APIs. This supports generating and retrieving policy recommendations to remediate unused permissions findings that are now being supported on Security Hub.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds support for bring-your-own file system in AgentCore Runtime. Developers can mount Amazon S3 Files and Amazon EFS access points directly into agent sessions using filesystemConfigurations.

## __Amazon Lex Model Building V2__
  - ### Features
    - Amazon Lex V2 introduces audio filler support for speech-to-speech bots. Configure melody or typing sounds that play during backend processing to reduce perceived latency and maintain a natural conversational experience for callers.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker HyperPod now returns ImageVersionStatus in DescribeCluster, DescribeClusterNode, and ListClusterNodes responses, indicating whether cluster instances are running the latest available image version.

## __Amazon Simple Storage Service__
  - ### Features
    - Validate outpost access point resource name

## __AmazonMWAA__
  - ### Features
    - Amazon MWAA now supports a PublicAndPrivate webserver access mode. The Airflow web server is accessible over both public and private endpoints, enabling workers in VPCs without internet access to reach the Task API privately while retaining public access to the Airflow UI.

## __EC2 Image Builder__
  - ### Features
    - The ImportDiskImage API now enforces a maximum character limit of 128 characters on the image name field.

# __2.44.2__ __2026-05-05__
## __AWS Clean Rooms ML__
  - ### Features
    - Increase max configurable output limits in the Clean Rooms ML configured model algorithm association resource.

## __AWS Health Imaging__
  - ### Features
    - Add support for DICOM Json Metadata Override features in startDICOMImportJob API

## __AWS Marketplace Agreement Service__
  - ### Features
    - With this release, Agreements API provides a programmatic way to generate quotes, accept offers, track charges and entitlements, manage renewals and cancellations, and streamline operations entirely through APIs without navigating to the AWS Marketplace website or AWS Management Console.

## __AWS MediaTailor__
  - ### Features
    - Added support for Monetization Functions. Monetization Functions let you enrich ad requests with external data and transform session parameters using JSONata expressions, without deploying custom infrastructure.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix an issue in the async SDK clients where a retry can lead to a `NullPointerException` if the exception that the SDK encountered did not originate from the service, such as a connection exception.

## __Amazon CloudFront__
  - ### Features
    - Adds support for tagging CloudFront Functions and KeyValueStores resources.

## __Amazon OpenSearch Service__
  - ### Features
    - Amazon OpenSearch Service now supports VPC egress, enabling outbound traffic from your OpenSearch domain to route privately through your VPC instead of the public internet.

## __Amazon Route 53 Domains__
  - ### Features
    - This release adds the TLDInMaintenance exception.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for ml.p5.4xlarge instance type for SageMaker Studio JupyterLab and CodeEditor apps for IAD (us-east-1), NRT (ap-northeast-1), BOM (ap-south-1), CGK (ap-southeast-3), GRU (sa-east-1), PDX (us-west-2), CMH (us-east-2).

# __2.44.1__ __2026-05-04__
## __AWS Elemental MediaLive__
  - ### Features
    - Updates the type of the MediaLiveRouterOutputConnectionMap.

## __AWS SDK for Java v2__
  - ### Features
    - Optimized JSON marshalling performance for JSON RPC, REST JSON and RPCv2 Cbor protocols.

## __AWS Security Agent__
  - ### Features
    - AWS Security Agent is adding a new target domain verification method for private VPC penetration testing. Additionally, the target domain resource will now have a verification status reason field to surface additional details about domain verification

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Amazon Bedrock AgentCore gateways now support MCP Sessions and response streaming from MCP targets. Session timeouts can be set between 15 minutes and 8 hours, and response streaming enables forwarding stream events sent by MCP targets to gateway users.

## __Amazon CloudWatch Logs__
  - ### Features
    - Adding an additional optional deliverySourceConfiguration field to PutDeliverySource API. This enables customers to pass service-specific configurations through IngestionHub such as tracing enablement or sampling rates that will be propagated to the source resource.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This feature allows customers to change the tunnel bandwidth on existing VPN connections using the ModifyVpnConnectionOptions API

## __Amazon Lex Model Building Service__
  - ### Features
    - Lex V1 is deprecated, use Lex V2 instead

## __Amazon Location Service Routes V2__
  - ### Features
    - Added support for TravelTimeExceedsDriverWorkHours, ViolatedBlockedRoad, and ViolatedVehicleRestriction notice codes to the CalculateRoutes API response.

## __Amazon VPC Lattice__
  - ### Features
    - Amazon VPC Lattice now supports privately resolvable DNS resources

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fix S3 Transfer Manager progress tracking overshoot when a multipart download part-get is retried after partial data delivery

# __2.44.0__ __2026-05-01__
## __AWS EntityResolution__
  - ### Features
    - Add support for transitive matching in AWS Entity Resolution rule-based matching workflows. When enabled, records that match through different rules are grouped together into the same match group, allowing related records to be connected across rule levels.

## __AWS Identity and Access Management__
  - ### Features
    - Added guidance for CreateOpenIDConnectProvider to include multiple thumbprints when OIDC discovery and JWKS endpoints use different hosts or certificates

## __AWS IoT__
  - ### Features
    - AWS IoT HTTP rule actions now support cross-topic batching, combining messages from different MQTT topics into single HTTP requests.

## __AWS SDK for Java v2__
  - ### Features
    - Added support for v2.1 retry behavior behind the `AWS_NEW_RETRIES_2026` feature gate. When enabled via environment variable `AWS_NEW_RETRIES_2026=true` or system property `-Daws.newRetries2026=true`, the SDK applies the following changes:

      - Uses `STANDARD` as the default retry mode
      - Reduces base backoff delay from 100ms to 50ms
      - Differentiates token costs between transient and throttling errors
      - Honors the `max_attempts` profile property
      - Uses the `x-amz-retry-after` response header for server-suggested delays
      - Retries on `LimitExceededException` as a throttling error
      - Retries on STS `IdpCommunicationErrorException`
      - Reduces DynamoDB default max attempts from 9 to 4
      - Backs off before failing long-polling operations (e.g., SQS `ReceiveMessage`) when the retry token bucket is exhausted, instead of failing immediately

      Example usage:
      ```java
      System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "true");
      DynamoDbClient ddb = DynamoDbClient.create();
      ```
    - Updated endpoint and partition metadata.

## __Amazon AppStream__
  - ### Features
    - Amazon WorkSpaces Applications now enables AI agents to securely operate desktop applications. Administrators configure stacks to provide agents access to WorkSpaces. Agents can click, type, and take screenshots. Agents authenticate with AWS IAM credentials with activity logged in AWS CloudTrail.

## __Amazon CloudWatch__
  - ### Features
    - This release adds tag support for CloudWatch Dashboards. The PutDashboard API now accepts a Tags parameter, allowing you to tag dashboards at creation time. Additionally, the TagResource, UntagResource, and ListTagsForResource APIs now support dashboard ARNs as resources.

## __Amazon CloudWatch Logs__
  - ### Features
    - Adds support for filtering log groups by tags in the ListLogGroups API via the new logGroupTags parameter.

## __Amazon Q Connect__
  - ### Features
    - Added reasoning details, statusDescription, and timeToFirstTokenMs fields to the ListSpans response in Amazon Q in Connect to provide visibility into model thinking, error diagnostics, and inference latency metrics.

## __Amazon QuickSight__
  - ### Features
    - Add IdentityProviderCACertificatesBundleS3Uri for private CA certs with OAuth datasources. 256-char limit for FontFamily in themes. ControlTitleFormatText on all 13 filters. ControlTitleFontConfiguration. ContextRegion for cross-region identity context. Story,scenario in CreateCustomCapability API.


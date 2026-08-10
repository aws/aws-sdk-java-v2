 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.51.4__ __2026-08-10__
## __AWS Elemental Inference__
  - ### Features
    - Added support for the SearchFixtures API and DataSourceConfiguration, enabling customers to map fixture event data onto clipping outputs for improved feature accuracy.

## __AWS Elemental MediaLive__
  - ### Features
    - Added VirtualSourceAddress to multicast output destinations for MediaLive Anywhere channels. Specifies the source IP address for outbound multicast packets when downstream networks enforce source-IP filtering.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Reduce generated client source and bytecode size for JSON, CBOR, and Smithy RPC v2 protocol services by generating the error-metadata mapper once per client instead of once per operation.

## __Amazon Connect Service__
  - ### Features
    - Added Malay language option to use AI to automatically fill evaluation forms in Malay

## __Amazon SageMaker Runtime__
  - ### Features
    - Added the PrefixAwareId header to InvokeEndpoint and InvokeEndpointWithResponseStream. This optional parameter serves as a routing hint for endpoints configured with prefix-aware routing, differentiating routing decisions for requests that share the same prompt prefix.

## __Amazon SageMaker Service__
  - ### Features
    - Added PREFIX AWARE routing strategy and PrefixAwareRoutingConfig to CreateEndpointConfig. Configure PrefixLength and ConcurrencyThreshold to route requests that share the same prompt prefix to the same instance.

# __2.51.3__ __2026-08-07__
## __AWS Amplify__
  - ### Features
    - Increased the maximum allowed length of the oauthToken parameter in the CreateApp and UpdateApp APIs to support longer OAuth tokens issued by third-party Git providers.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - StreamNameOutputMode - a new optional field on MediaPackageV2 OriginEndpoints that lets customers choose whether egress manifests use numeric stream indices (default) or encoder-assigned stream names from the input

## __AWS MediaTailor__
  - ### Features
    - Added support for inserting ads via the VAST Ad Buffet standard. You can now configure MediaTailor to insert ads in sequence order using the AdSequencingMode setting in your playback configuration. Standalone ads are used as fallbacks when a sequenced ad is unavailable.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Correctly support endpoint expressions with multiple chained assigns.

## __AWS Security Agent__
  - ### Features
    - Added enableEmailMfa input field on Actor to enable email-based MFA during penetration tests. When enabled, a server-generated mfaForwardingAddress is returned. Set up a forwarding rule in your email provider to forward MFA emails to this address so the agent can complete email-based MFA login flows

## __Amazon Connect Service__
  - ### Features
    - Supports updating the task template associated with in-progress task contacts using the new UpdateContactTaskTemplate API. This enables supervisors and developers to dynamically reassign task templates without creating a new task.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for BGP route protection in Amazon VPC IP Address Manager (IPAM), including route discovery, RPKI route protection findings, and delegated RPKI (Internet Registry Associations, routing policy registrations, and ROA management) for BYOIP prefixes.

## __Amazon HealthLake__
  - ### Features
    - Adds provenanceEnabled to StartFHIRImportJob

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker adds maintenance lifecycle statuses for Notebook Instances

# __2.51.2__ __2026-08-06__
## __AWS Backup__
  - ### Features
    - AWS Backup now lets you create read-only access points for Amazon S3 recovery points, enabling you to access backup data using S3 APIs without initiating a restore.

## __AWS Device Farm__
  - ### Features
    - Adds support for service generated insights across runs, jobs, and tests.

## __AWS End User Messaging Social__
  - ### Features
    - Add support for WhatsApp Conversions APIs.

## __AWS Marketplace Agreement Service__
  - ### Features
    - GetAgreementTerms now returns a new term variant in AcceptedTerm, netPaymentTerm, with a paymentDuePeriod field (example "P30D").

## __AWS Marketplace Discovery__
  - ### Features
    - GetOfferTerms now returns netPaymentTerm in offerTerms, specifying payment due period after invoice date. The paymentDuePeriod field uses ISO 8601 duration format (e.g., "P30D" for net 30 days). This is a backward-compatible addition. See API documentation for full structure and examples.

## __AWS MediaTailor__
  - ### Features
    - AWS Elemental MediaTailor now supports concurrent function execution. The new Concurrent Executor function type runs multiple independent child functions in parallel within a single lifecycle hook, reducing pipeline latency to the duration of the slowest call instead of the sum of all calls.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Security Hub is adding a new public API, ListFreeTrialStatusesV2 to describe the free trial statuses of the Security Hub service and its opt-in features.

## __Agent Registry__
  - ### Features
    - Agent Registry's Public Preview release

## __Agent Registry Control__
  - ### Features
    - Agent Registry's Public Preview release

## __Amazon Bedrock AgentCore__
  - ### Features
    - Add support for capacity provider sessions in Amazon Bedrock AgentCore. Customers can now delete an active session running on a runtime instance launched through their capacity provider.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Add support for Gateway rate limits and Runtime instances in Amazon Bedrock AgentCore. Customers can now configure rate limits scoped to control request rates, token consumption rates, and active connection rates. Customers can now create capacity providers to launch runtimes on their EC2 instances.

## __Amazon CloudWatch Logs__
  - ### Features
    - This release adds index category support to the CloudWatch Logs DescribeFieldIndexes API. Customers can filter and identify DEFAULT, CUSTOM, AUTO, and INACTIVE field indexes.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds a new optional IncludeLocalZones parameter to the Spot Placement Score API that defaults to false. When set to true, the Spot Placement Score API will consider the relevant Local Zones with Spot capacity when computing the Spot Placement Score.

## __Amazon GameLift__
  - ### Features
    - Adds support for C8a, C8i, C9g, M8a, M8i, and M9g EC2 instance type families for managed EC2 and container fleets. Also adds explicit anchors on most string regexes.

## __Amazon SageMaker Service__
  - ### Features
    - Releases new Model Customization SequenceLength parameter for Training and g7 instance types for Training and Processing.

## __Amazon Simple Storage Service__
  - ### Features
    - AWS Backup now lets you create read-only access points for Amazon S3 recovery points, enabling you to access backup data using S3 APIs without initiating a restore.

## __Auto Scaling__
  - ### Features
    - EC2 Auto Scaling now supports being managed by other AWS services via the operator field.

## __Managed Streaming for Kafka__
  - ### Features
    - MSK Clusters can now deliver authorizer logs alongside broker logs to the destinations defined by you

# __2.51.1__ __2026-08-05__
## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Private Certificate Authority service now supports RSASSA-PSS signing algorithm.

## __AWS Glue__
  - ### Features
    - Added the PutDataCatalogExportConfiguration to export Glue Data Catalog metadata to systems tables stored in S3 Tables.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Eliminate per-operation metric-publishing lambdas in generated clients by hoisting them into shared helper methods, reducing constant pool usage and class size for large service clients approaching the JVM per-class constant pool limit.

## __AWS Sign-In Service__
  - ### Bugfixes
    - Fixed an issue where credentials loaded from a login_session profile resolved the Signin client's configuration, such as region and endpoint_url, from the default profile instead of the profile that requested the credentials.

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud now reports persistent volume costs alongside compute and license costs. Customers can view per-fleet storage costs in Usage Explorer by selecting the Usage Type grouping, helping them better understand the costs of their infrastructure.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adding support for fine-grained access control for AgentCore Memory through managed AgentCore Gateway HTTP Connectors.

## __Amazon EC2 Container Service__
  - ### Features
    - New enum values added for Agent Connectivity issues

# __2.51.0__ __2026-08-04__
## __AWS Identity and Access Management__
  - ### Features
    - Updating endpoint generation logic

## __AWS Organizations__
  - ### Features
    - Improved accuracy of CloudTrail event documentation for AWS Organizations membership operations.

## __AWS Single Sign-On Admin__
  - ### Features
    - AWS IAM Identity Center now lets you create organization-level instances without enabling multi-account permissions. You can enable multi-account permissions during instance creation or later via console or API, which then provisions the necessary service-linked roles.

## __Amazon Aurora DSQL__
  - ### Features
    - UpdateCluster now checks the RemovePeerCluster permission on the specific cluster being removed, not a wildcard and docs now clarify how to set kmsEncryptionKey so the cluster uses the AWS-owned key.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect Customer now supports up to 50 attachments per email, increased from the previous limit of 10. The individual maximum attachment size limit of 20 MB and the total email size limit of 25 MB still hold true.

## __Amazon DynamoDB__
  - ### Features
    - Vector indexes are a type of index in Amazon DynamoDB that enable similarity search on vector embedding stored in your table items. Vector indexes use approximate nearest neighbor search to find items whose vectors are most similar to a query vector that you provide.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Adds SearchVectors and vector index support to the DynamoDB Enhanced Client, including vector index table operations, table.vectorIndex() search handles, and enhanced search request/response types.
    - Adds annotation-driven vector index support to the DynamoDB Enhanced Client via @DynamoDbVectorAttribute, @DynamoDbSearchVectorsHashKey, and @DynamoDbSearchVectorsInlineFilterKey on bean and immutable schemas, enabling no-arg createTable() and vector search through annotation-derived table.vectorIndex() handles.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 now supports Application Status Checks, a new status check that monitors your application's health through configurable HTTP(S) paths and ports, so you can detect and automatically respond to application-level impairments.

## __Amazon WorkSpaces__
  - ### Features
    - Added ClientExperiencePolicy to ClientProperties object for ModifyClientProperties and DescribeClientProperties APIs.

## __Inspector2__
  - ### Features
    - Adding Azure SBOM export capability.

## __Partner Central Selling API__
  - ### Features
    - Partners can now create leads with only 5 required fields and free-text values for all other fields, reducing import friction. Engagement invitations now include enrichment data (propensity scores, lead readiness) directly in the response.


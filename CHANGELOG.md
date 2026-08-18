 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.53.2__ __2026-08-17__
## __AWS Organizations__
  - ### Features
    - Add new Transfer Responsibility error codes and document related CloudTrail events for accepting and terminating a Transfer Responsibility.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - AgenticRetrieveStream API now supports Amazon Bedrock AgentCore Memory. Use the new memoryConfiguration parameter to continue a session from short-term memory and retrieve from long-term memory.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds implementations of third-party evaluators, both managed-as-a-service and as templates within custom evaluators.

## __Amazon Connect Service__
  - ### Features
    - This release adds new APIs to create, describe, update, delete, and list extraction definitions, enabling customers to manage lifecycle of extraction definition resources. Additionally, this release adds new event sources for Rules related to ACW and new action to Extract Information.

## __Amazon Elastic Container Registry__
  - ### Features
    - Documentation update for the ECR PutReplicationConfiguration API to increase the replication rule limit from 10 to 25

## __Amazon Location Service Maps V2__
  - ### Features
    - Amazon Location Service now supports POI density and category filtering on dynamic maps. The GetStyleDescriptor API adds two optional parameters. PoiDensity (Off to VeryDense) controls POI volume, and PoiCategories filters by up to nine categories. Available on HERE and Grab map styles.

## __Elastic Disaster Recovery Service__
  - ### Features
    - AWS Elastic Disaster Recovery (AWS DRS) now offers Recovery Plans to recover multi-server applications in the right order in one action. Define the launch sequence once, with ordered steps and wait times, and DRS runs it automatically. Validate with non-disruptive drills and monitor in real time.

# __2.53.1__ __2026-08-14__
## __AWS Glue__
  - ### Features
    - Added support for associating glossary terms with iterable form items, such as table columns.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Adds CheckIngestedDocumentAcl and GetIngestedDocumentAcl APIs to Amazon Bedrock Knowledge Bases. Customers can verify user access to documents based on ingested ACLs and retrieve full ACL details including allow and deny entries, enabling validation of ACL ingestion without test retrievals.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Add support for the Machine Payments Protocol (MPP) and x402 upto scheme payments protocol in Amazon Bedrock AgentCore Payments. Customers can now pay for MPP-gated resources and also pay services which requires upto scheme in x402

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds AgentCore Payments support for CMK, Marketplace Subscriptions and QuickCreate

## __Amazon Redshift__
  - ### Features
    - Amazon Redshift now unlocks a locked admin user account and resets the failed-login counter when you update the admin password using the ModifyCluster API. This option is available only when account lockout security is enabled.

## __Amazon SageMaker Service__
  - ### Features
    - Release support for g7.2xlarge, g7.4xlarge, g7.8xlarge, g7.12xlarge, g7.24xlarge, and g7.48xlarge instance types for SageMaker HyperPod

## __AmazonMWAAServerless__
  - ### Features
    - Adds support for Consuming code for MWAA Serverless

## __CloudWatch Observability Admin Service__
  - ### Features
    - CloudWatch Logs centralization rules now support tag propagation. You can configure a TagPropagationConfiguration on your centralization rule to automatically sync resource tags from source to destination log groups, with configurable conflict resolution strategies.

## __Redshift Serverless__
  - ### Features
    - Amazon Redshift now unlocks a locked admin user account and resets the failed-login counter when you update the admin password using the UpdateNamespace API. This option is available only when account lockout security is enabled.

# __2.53.0__ __2026-08-13__
## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Honor wildcard `nonProxyHosts` entries in CRT-based HTTP clients (`AwsCrtHttpClient`, `AwsCrtAsyncHttpClient`, and the S3/CRT client). Previously a `*.suffix` wildcard or a bare `*` from `http.nonProxyHosts`/`no_proxy` was silently ignored, so matching hosts were routed through the proxy instead of bypassing it. Supported forms are an exact host, a `*.suffix` wildcard (matches the domain and its subdomains), a single `*` for all hosts, and CIDR ranges. Entries must not contain surrounding whitespace.

## __AWS Certificate Manager__
  - ### Features
    - This change allows customers to update their existing email-validated certificates to use the DNS validation method.

## __AWS Clean Rooms Service__
  - ### Features
    - This release adds support for minimum aggregation thresholds and comparison controls to the Custom analysis rule type.

## __AWS CodeCommit__
  - ### Features
    - Added the GetBlobDifferences API operation, which returns line-level diffs between two blob versions without requiring a local clone. Returns structured hunks with context, additions, and deletions. Supports pagination for large diffs.

## __AWS SDK for Java v2__
  - ### Features
    - Removed ServiceMetadata usages from client creation and request processing code paths to improve cold start performance. Endpoint and signing region resolution now uses Endpoints 2.0 directly instead of triggering eager initialization of all service metadata classes. Service client artifacts must be version 2.28.1 or later to be compatible with this change.

## __AWS Security Agent__
  - ### Features
    - Add support for setting a maximum task-hour budget cap on penetration tests and code reviews, and for revalidating previously reported findings via a new REVALIDATION job type.

## __Amazon CloudFront__
  - ### Features
    - Adds SHA256 support to CloudFrontUtilities

## __Amazon Connect Service__
  - ### Features
    - Adds the StartAssistantContact API to start chat contacts handled by an AI agent. Adds SegmentAttributes to StartWebRTCContact, and corrects its error response to now receive AccessDeniedException (previously returned as an internal server error due to a missing error declaration).

## __Amazon DynamoDB Enhanced Client__
  - ### Bugfixes
    - Fix AutoGeneratedTimestampRecordExtension failing when TableSchema.converterForAttribute throws UnsupportedOperationException for custom schemas; expand dynamodb-enhanced functional test coverage

## __Apache 5 HTTP Client__
  - ### Bugfixes
    - Trimmed surrounding whitespace from `nonProxyHosts` / `no_proxy` entries so comma- or pipe-separated values with spaces (e.g. `no_proxy=a.com, *.foo.com`) are honored. Previously an entry with a leading or trailing space was treated as part of the host name and never matched, so the host was routed through the proxy instead of bypassing it.

## __Apache HTTP Client__
  - ### Bugfixes
    - Trimmed surrounding whitespace from `nonProxyHosts` / `no_proxy` entries so comma- or pipe-separated values with spaces (e.g. `no_proxy=a.com, *.foo.com`) are honored. Previously an entry with a leading or trailing space was treated as part of the host name and never matched, so the host was routed through the proxy instead of bypassing it.

## __Apache HTTP Client 5__
  - ### Features
    - Upgrade httpcomponents.client5 to 5.6.4 to address CVE-2026-71290

## __Auto Scaling__
  - ### Features
    - Amazon EC2 Auto Scaling now supports terminating multiple instances in a single TerminateInstanceInAutoScalingGroup call via the new InstanceIds parameter, returning an Activities list. LaunchInstances now returns IdempotentCallInProgressFault for duplicate client tokens.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Trimmed surrounding whitespace from `nonProxyHosts` / `no_proxy` entries so comma- or pipe-separated values with spaces (e.g. `no_proxy=a.com, *.foo.com`) are honored. Previously an entry with a leading or trailing space was treated as part of the host name and never matched, so the host was routed through the proxy instead of bypassing it.

## __URL Connection HTTP Client__
  - ### Bugfixes
    - Trimmed surrounding whitespace from `nonProxyHosts` / `no_proxy` entries so comma- or pipe-separated values with spaces (e.g. `no_proxy=a.com, *.foo.com`) are honored. Previously an entry with a leading or trailing space was treated as part of the host name and never matched, so the host was routed through the proxy instead of bypassing it.


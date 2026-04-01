 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.42.25__ __2026-03-31__
## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Enabled default connection health monitoring for the AWS CRT HTTP client. Connections that remain stalled below 1 byte per second for the duration the read/write timeout (default 30 seconds) are now automatically terminated. This behavior can be overridden via ConnectionHealthConfiguration.

## __AWS Certificate Manager__
  - ### Features
    - Adds support for searching for ACM certificates using the new SearchCertificates API.

## __AWS Data Exchange__
  - ### Features
    - Support Tags for AWS Data Exchange resource Assets

## __AWS Database Migration Service__
  - ### Features
    - To successfully connect to the IBM DB2 LUW database server, you may need to specify additional security parameters that are passed to the JDBC driver. These parameters are EncryptionAlgorithm and SecurityMechanism. Both parameters accept integer values.

## __AWS DevOps Agent Service__
  - ### Features
    - AWS DevOps Agent service General Availability release.

## __AWS Marketplace Agreement Service__
  - ### Features
    - This release adds 8 new APIs for AWS Marketplace sellers. 4 APIs for Cancellations (Send, List, Get, Cancel action on AgreementCancellationRequest), 3 APIs for Billing Adjustments (BatchCreate, List, Get action on BillingAdjustmentRequest), and 1 API to List Invoices (ListAgreementInvoiceLineItems)

## __AWS Organizations__
  - ### Features
    - Added Path field to Account and OrganizationalUnit objects in AWS Organizations API responses.

## __AWS S3 Control__
  - ### Features
    - Adding an optional auditContext parameter to S3 Access Grants credential vending API GetDataAccess to enable job-level audit correlation in S3 CloudTrail logs

## __AWS SDK for Java v2__
  - ### Features
    - Update Netty to 4.1.132
        - Contributed by: [@mrdziuban](https://github.com/mrdziuban)

## __AWS SDK for Java v2 Migration Tool__
  - ### Bugfixes
    - Fix bug for v1 getUserMetaDataOf transform

## __AWS Security Agent__
  - ### Features
    - AWS Security Agent is a service that proactively secures applications throughout the development lifecycle with automated security reviews and on-demand penetration testing.

## __AWS Sustainability__
  - ### Features
    - This is the first release of the AWS Sustainability SDK, which enables customers to access their sustainability impact data via API.

## __Amazon CloudFront__
  - ### Features
    - This release adds bring your own IP (BYOIP) IPv6 support to CloudFront's CreateAnycastIpList and UpdateAnycastIpList API through the IpamCidrConfigs field.

## __Amazon DataZone__
  - ### Features
    - Adds environmentConfigurationName field to CreateEnvironmentInput and UpdateEnvironmentInput, so that Domain Owners can now recover orphaned environments by recreating deleted configurations with the same name, and will auto-recover orphaned environments

## __Amazon DynamoDB Enhanced Client__
  - ### Bugfixes
    - Returning correct operation name for DeleteTableOperation

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release updates the examples in the documentation for DescribeRegions and DescribeAvailabilityZones.

## __Amazon Kinesis Analytics__
  - ### Features
    - Support for Flink 2.2 in Managed Service for Apache Flink

## __Amazon Location Service Maps V2__
  - ### Features
    - This release expands map customization options with adjustable contour line density, dark mode support for Hybrid and Satellite views, enhanced traffic information across multiple map styles, and transit and truck travel modes for Monochrome and Hybrid map styles.

## __Amazon OpenSearch Service__
  - ### Features
    - Support RegisterCapability, GetCapability, DeregisterCapability API for AI Assistant feature management for OpenSearch UI Applications

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - This release adds RCS for Business messaging and Notify support. RCS lets you create and manage agents, send and receive messages in the US and Canada via SendTextMessage API, and configure SMS fallback. Notify lets you send templated OTP messages globally in minutes with no phone number required.

## __Amazon QuickSight__
  - ### Features
    - Adds StartAutomationJob and DescribeAutomationJob APIs for automation jobs. Adds three custom permission capabilities that allow admins to control whether users can manage Spaces and chat agents. Adds an OAuthClientCredentials structure to provide OAuth 2.0 client credentials inline to data sources.

## __Amazon S3 Tables__
  - ### Features
    - S3 Tables now supports nested types when creating tables. Users can define complex column schemas using struct, list, and map types. These types can be composed together to model complex, hierarchical data structures within table schemas.

## __Amazon Simple Storage Service__
  - ### Features
    - Add Bucket Metrics configuration support to directory buckets

## __CloudWatch Observability Admin Service__
  - ### Features
    - This release adds the Bedrock and Security Hub resource types for Omnia Enablement launch for March 31.

## __MailManager__
  - ### Features
    - Amazon SES Mail Manager now supports optional TLS policy for accepting unencrypted connections and mTLS authentication for ingress endpoints with configurable trust stores. Two new rule actions are available, Bounce for sending non-delivery reports and Lambda invocation for custom email processing.

## __Partner Central Selling API__
  - ### Features
    - Adding EURO Currency for MRR Amount

## __odb__
  - ### Features
    - Adds support for EC2 Placement Group integration with ODB Network. The GetOdbNetwork and ListOdbNetworks API responses now include the ec2PlacementGroupIds field.

## __Contributors__
Special thanks to the following contributors to this release: 

[@mrdziuban](https://github.com/mrdziuban)
# __2.42.24__ __2026-03-30__
## __AWS DevOps Agent Service__
  - ### Features
    - AWS DevOps Agent General Availability.

## __AWS Lake Formation__
  - ### Features
    - Add setSourceIdentity to DataLakeSettings Parameters

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Optimized JSON serialization by skipping null field marshalling for payload fields

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud now supports three new fleet auto scaling settings. With scale out rate, you can configure how quickly workers launch. With worker idle duration, you can set how long workers wait before shutting down. With standby worker count, you can keep idle workers ready for fast job start.

## __Amazon AppStream__
  - ### Features
    - Add support for URL Redirection

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds Ground Truth support for AgentCore Evaluations (Evaluate)

## __Amazon CloudWatch Logs__
  - ### Features
    - Adds Lookup Tables to CloudWatch Logs for log enrichment using CSV key-value data with KMS encryption support.

## __Amazon DynamoDB Enhanced Client__
  - ### Bugfixes
    - Improved performance by caching partition and sort key name lookups in StaticTableMetadata.

## __Amazon EC2 Container Service__
  - ### Features
    - Adding Local Storage support for ECS Managed Instances by introducing a new field "localStorageConfiguration" for CreateCapacityProvider and UpdateCapacityProvider APIs.

## __Amazon GameLift__
  - ### Features
    - Update CreateScript API documentation.

## __Amazon OpenSearch Service__
  - ### Features
    - Added Cluster Insights API's In OpenSearch Service SDK.

## __Amazon SageMaker Service__
  - ### Features
    - Added support for placement strategy and consolidation for SageMaker inference component endpoints. Customers can now configure how inference component copies are distributed across instances and availability zones (AZs), and enable automatic consolidation to optimizes resource utilization.

## __Auto Scaling__
  - ### Features
    - Adds support for new instance lifecycle states introduced by the instance lifecycle policy and replace root volume features.

## __Partner Central Account API__
  - ### Features
    - KYB Supplemental Form enables partners who fail business verification to submit additional details and supporting documentation through a self-service form, triggering an automated re-verification without requiring manual intervention from support teams.

# __2.42.23__ __2026-03-27__
## __Amazon Bedrock AgentCore__
  - ### Features
    - Adding AgentCore Code Interpreter Node.js Runtime Support with an optional runtime field

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds support for custom code-based evaluators using customer-managed Lambda functions.

## __Amazon NeptuneData__
  - ### Features
    - Minor formatting changes to remove unnecessary symbols.

## __Amazon Omics__
  - ### Features
    - AWS HealthOmics now supports VPC networking, allowing users to connect runs to external resources with NAT gateway, AWS VPC resources, and more. New Configuration APIs support configuring VPC settings. StartRun API now accepts networkingMode and configurationName parameters to enable VPC networking.

## __Apache 5 HTTP Client__
  - ### Bugfixes
    - Fixed an issue in the Apache 5 HTTP client where requests could fail with `"Endpoint not acquired / already released"`. These failures are now converted to retryable I/O errors.

# __2.42.22__ __2026-03-26__
## __AWS Billing and Cost Management Data Exports__
  - ### Features
    - With this release we are providing an option to accounts to have their export delivered to an S3 bucket that is not owned by the account.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch EMF Metric Publisher__
  - ### Features
    - Add `PropertiesFactory` and `propertiesFactory` to `EmfMetricLoggingPublisher.Builder`, enabling users to enrich EMF records with custom key-value properties derived from the metric collection or ambient context, searchable in CloudWatch Logs Insights. See [#6595](https://github.com/aws/aws-sdk-java-v2/issues/6595).
        - Contributed by: [@humanzz](https://github.com/humanzz)

## __Amazon CloudWatch Logs__
  - ### Features
    - This release adds parameter support to saved queries in CloudWatch Logs Insights. Define reusable query templates with named placeholders, invoke them using start query. Available in Console, CLI and SDK

## __Amazon EMR__
  - ### Features
    - Add StepExecutionRoleArn to RunJobFlow API

## __Amazon SageMaker Service__
  - ### Features
    - Release support for ml.r5d.16xlarge instance types for SageMaker HyperPod

## __Timestream InfluxDB__
  - ### Features
    - Timestream for InfluxDB adds support for customer defined maintenance windows. This allows customers to define maintenance schedule during resource creation and updates

## __Contributors__
Special thanks to the following contributors to this release: 

[@humanzz](https://github.com/humanzz)
# __2.42.21__ __2026-03-25__
## __AWS Batch__
  - ### Features
    - Documentation-only update for AWS Batch.

## __AWS Marketplace Agreement Service__
  - ### Features
    - The Variable Payments APIs enable AWS Marketplace Sellers to perform manage their payment requests (send, get, list, cancel).

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix bug in CachedSupplier that retries aggressively after many consecutive failures

## __AWS User Experience Customization__
  - ### Features
    - GA release of AccountCustomizations, used to manage account color, visible services, and visible regions settings in the AWS Management Console.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - This release adds support for creating SLOs on RUM appMonitors, Synthetics canaries and services.

## __Amazon Polly__
  - ### Features
    - Add support for Mu-law and A-law codecs for output format

## __Amazon S3__
  - ### Features
    - Add support for maxInFlightParts to multipart upload (PutObject) in MultipartS3AsyncClient.

## __AmazonApiGatewayV2__
  - ### Features
    - Added DISABLE IN PROGRESS and DISABLE FAILED Portal statuses.

# __2.42.20__ __2026-03-24__
## __AWS Elemental MediaPackage v2__
  - ### Features
    - Reduces the minimum allowed value for startOverWindowSeconds from 60 to 0, allowing customers to effectively disable the start-over window.

## __AWS Parallel Computing Service__
  - ### Features
    - This release adds support for custom slurmdbd and cgroup configuration in AWS PCS. Customers can now specify slurmdbd and cgroup settings to configure database accounting and reporting for their HPC workloads, and control resource allocation and limits for compute jobs.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds SDK support for 1) Persist session state in AgentCore Runtime via filesystemConfigurations in CreateAgentRuntime, UpdateAgentRuntime, and GetAgentRuntime APIs, 2) Optional name-based filtering on AgentCore ListBrowserProfiles API.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers launches UDP ping beacons in the Beijing and Ningxia (China) Regions to help measure real-time network latency for multiplayer games. The ListLocations API is now available in these regions to provide endpoint domain and port information as part of the locations list.

## __Amazon Relational Database Service__
  - ### Features
    - Adds support in Aurora PostgreSQL serverless databases for express configuration based creation through WithExpressConfiguration in CreateDbCluster API, and for restoring clusters using RestoreDBClusterToPointInTime and RestoreDBClusterFromSnapshot APIs.

## __OpenSearch Service Serverless__
  - ### Features
    - Adds support for updating the vector options field for existing collections.

# __2.42.19__ __2026-03-23__
## __AWS Batch__
  - ### Features
    - AWS Batch AMI Visibility feature support. Adds read-only batchImageStatus to Ec2Configuration to provide visibility on the status of Batch-vended AMIs used by Compute Environments.

## __Amazon Connect Cases__
  - ### Features
    - You can now use the UpdateRelatedItem API to update the content of comments and custom related items associated with a case.

## __Amazon Lightsail__
  - ### Features
    - Add support for tagging of ContactMethod resource type

## __Amazon Omics__
  - ### Features
    - Adds support for batch workflow runs in Amazon Omics, enabling users to submit, manage, and monitor multiple runs as a single batch. Includes APIs to create, cancel, and delete batches, track submission statuses and counts, list runs within a batch, and configure default settings.

## __Amazon S3__
  - ### Features
    - Added support of Request-level credentials override in DefaultS3CrtAsyncClient. See [#5354](https://github.com/aws/aws-sdk-java-v2/issues/5354).

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fixed an issue where requests with `Expect: 100-continue` over TLS could hang indefinitely when no response is received, because the read timeout handler was prematurely removed by TLS handshake data.

# __2.42.18__ __2026-03-20__
## __AWS Backup__
  - ### Features
    - Fix Typo for S3Backup Options ( S3BackupACLs to BackupACLs)

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix bug in CachedSupplier that disabled InstanceProfileCredentialsProvider credential refreshing after 58 consecutive failures.

## __Amazon DynamoDB__
  - ### Features
    - Adding ReplicaArn to ReplicaDescription of a global table replica

## __Amazon OpenSearch Service__
  - ### Features
    - Added support for Amazon Managed Service for Prometheus (AMP) as a connected data source in OpenSearch UI. Now users can analyze Prometheus metrics in OpenSearch UI without data copy.

## __Amazon Verified Permissions__
  - ### Features
    - Adds support for Policy Store Aliases, Policy Names, and Policy Template Names. These are customizable identifiers that can be used in place of Policy Store ids, Policy ids, and Policy Template ids respectively in Amazon Verified Permissions APIs.

# __2.42.17__ __2026-03-19__
## __AWS Batch__
  - ### Features
    - AWS Batch now supports quota management, enabling administrators to allocate shared compute resources across teams and projects through quota shares with capacity limits, resource-sharing strategies, and priority-based preemption - currently available for SageMaker Training job queues.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock AgentCore__
  - ### Features
    - This release includes SDK support for the following new features on AgentCore Built In Tools. 1. Enterprise Policies for AgentCore Browser Tool. 2. Root CA Configuration Support for AgentCore Browser Tool and Code Interpreter. 3. API changes to AgentCore Browser Profile APIs

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds support for the following new features. 1. Enterprise Policies support for AgentCore Browser Tool. 2. Root CA Configuration support for AgentCore Browser Tool and Code Interpreter.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 Fleet instant mode now supports launching instances into Interruptible Capacity Reservations, enabling customers to use spare capacity shared by Capacity Reservation owners within their AWS Organization.

## __Amazon Polly__
  - ### Features
    - Added bi-directional streaming functionality through a new API, StartSpeechSynthesisStream. This API allows streaming input text through inbound events and receiving audio as part of an output stream simultaneously.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Adding a new field in the CreateCentralizationRuleForOrganization, UpdateCentralizationRuleForOrganization API and updating the GetCentralizationRuleForOrganization API response to include the new field

# __2.42.16__ __2026-03-18__
## __AWS Elemental MediaConvert__
  - ### Features
    - This update adds additional bitrate options for Dolby AC-4 audio outputs.

## __Amazon DynamoDB Enhanced Client__
  - ### Bugfixes
    - Fix NullPointerException in `EnhancedType.hashCode()`, `EnhancedType.equals()`, and `EnhancedType.toString()` when using wildcard types.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - The DescribeInstanceTypes API now returns default connection tracking timeout values for TCP, UDP, and UDP stream via the new connectionTrackingConfiguration field on NetworkInfo.

# __2.42.15__ __2026-03-17__
## __AWS Glue__
  - ### Features
    - Provide approval to overwrite existing Lake Formation permissions on all child resources with the default permissions specified in 'CreateTableDefaultPermissions' and 'CreateDatabaseDefaultPermissions' when updating catalog. Allowed values are ["Accept","Deny"] .

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Deprecating namespaces field and adding namespaceTemplates.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Improved performance of UpdateExpression conversion by replacing Stream.concat chains and String.format with direct iteration and StringJoiner.

## __Amazon EMR__
  - ### Features
    - Add S3LoggingConfiguration to Control LogUploads

# __2.42.14__ __2026-03-16__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - You can now generate policy scenarios on demand using the new GENERATE POLICY SCENARIOS build workflow type. Scenarios will no longer be automatically generated during INGEST CONTENT, REFINE POLICY, and IMPORT POLICY workflows, resulting in faster completion times for these operations.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Provide support to perform deterministic operations on agent runtime through shell command executions via the new InvokeAgentRuntimeCommand API

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Supporting hosting of public ECR Container Images in AgentCore Runtime

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS now supports configuring whether tags are propagated to the EC2 Instance Metadata Service (IMDS) for instances launched by the Managed Instances capacity provider. This gives customers control over tag visibility in IMDS when using ECS Managed Instances.

# __2.42.13__ __2026-03-13__
## __AWS Config__
  - ### Features
    - Fix pagination support for DescribeConformancePackCompliance, and update OrganizationConfigRule InputParameters max length to match ConfigRule.

## __AWS Elemental MediaConvert__
  - ### Features
    - This update adds support for Dolby AC-4 audio output, frame rate conversion between non-Dolby Vision inputs to Dolby Vision outputs, and clear lead CMAF HLS output.

## __AWS Elemental MediaLive__
  - ### Features
    - Documents the VideoDescription.ScalingBehavior.SMART(underscore)CROP enum value.

## __AWS Glue__
  - ### Features
    - Add QuerySessionContext to BatchGetPartitionRequest

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - upgrade maven-compiler-plugin to 3.14.1
        - Contributed by: [@sullis](https://github.com/sullis)

## __Amazon API Gateway__
  - ### Features
    - API Gateway now supports an additional security policy "SecurityPolicy-TLS13-1-2-FIPS-PFS-PQ-2025-09" for REST APIs and custom domain names. The new policy is compliant with TLS 1.3, Federal Information Processing Standards (FIPS), Perfect Forward Secrecy (PFS), and post-quantum (PQ) cryptography

## __Amazon Connect Service__
  - ### Features
    - Deprecating PredefinedNotificationID field

## __Amazon GameLift Streams__
  - ### Features
    - Feature launch that enables customers to connect streaming sessions to their own VPCs running in AWS.

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - Updates maximum reconnect window seconds from 60 to 300 for participant replication

## __Amazon QuickSight__
  - ### Features
    - The change adds a new capability named ManageSharedFolders in Custom Permissions

## __Amazon S3__
  - ### Features
    - Added `expectContinueEnabled` to `S3Configuration` to control the `Expect: 100-continue` header on PutObject and UploadPart requests. When set to `false`, the SDK stops adding the header. For Apache HTTP client users, to have `ApacheHttpClient.builder().expectContinueEnabled()` fully control the header, set `expectContinueEnabled(false)` on `S3Configuration`.

## __Application Migration Service__
  - ### Features
    - Network Migration APIs are now publicly available for direct programmatic access. Customers can now call Network Migration APIs directly without going through AWS Transform (ATX), enabling automation, integration with existing tools, and self-service migration workflows.

## __Contributors__
Special thanks to the following contributors to this release: 

[@sullis](https://github.com/sullis)
# __2.42.12__ __2026-03-12__
## __AWS DataSync__
  - ### Features
    - DataSync's 3 location types, Hadoop Distributed File System (HDFS), FSx for Windows File Server (FSx Windows), and FSx for NetApp ONTAP (FSx ONTAP) now have credentials managed via Secrets Manager, which may be encrypted with service keys or be configured to use customer-managed keys or secret.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Updating Lakefromation Access Grants Plugin version to 1.4.1
        - Contributed by: [@akhilyendluri](https://github.com/akhilyendluri)

## __Amazon Cloudfront__
  - ### Features
    - Add support for resourceUrlPattern to `CloudFrontUtilities.getCookiesForCustomPolicy`.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Added dynamoDbClient() and dynamoDbAsyncClient() default methods to DynamoDbEnhancedClient and DynamoDbEnhancedAsyncClient interfaces to allow access to the underlying low-level client. Fixes [#6654](https://github.com/aws/aws-sdk-java-v2/issues/6654)

## __Amazon Elastic Container Registry__
  - ### Features
    - Add Chainguard to PTC upstreamRegistry enum

## __Amazon Simple Storage Service__
  - ### Features
    - Adds support for account regional namespaces for general purpose buckets. The account regional namespace is a reserved subdivision of the global bucket namespace where only your account can create general purpose buckets.

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fix inaccurate progress tracking for in-memory uploads in the Java-based S3TransferManager.

## __Contributors__
Special thanks to the following contributors to this release: 

[@akhilyendluri](https://github.com/akhilyendluri)
# __2.42.11__ __2026-03-11__
## __AWS CRT-based S3 Client__
  - ### Bugfixes
    - Only log `SSL Certificate verification is disabled` warning if trustAllCertificatesEnabled is set to true.
        - Contributed by: [@bsmelo](https://github.com/bsmelo)

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Updating Lakeformation Access Grants Plugin version to 1.4

## __AWS SDK for Java v2 Migration Tool__
  - ### Bugfixes
    - Strip quotes in getETag response

## __Amazon Connect Customer Profiles__
  - ### Features
    - Today, Amazon Connect is announcing the ability to filter (include or exclude) recommendations based on properties of items and interactions.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Improved performance by adding a fast path avoiding wrapping of String and Byte types

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds support for a new tier in controlPlaneScalingConfig on EKS Clusters.

## __Amazon Polly__
  - ### Features
    - Added support for the new voices - Ambre (fr-FR), Beatrice (it-IT), Florian (fr-FR), Lennart (de-DE), Lorenzo (it-IT) and Tiffany (en-US). They are available as a Generative voices only.

## __Amazon S3__
  - ### Bugfixes
    - Fixed misleading checksum mismatch error message for S3 GetObject that incorrectly referenced uploading. See [#6324](https://github.com/aws/aws-sdk-java-v2/issues/6324).

## __Amazon SageMaker Service__
  - ### Features
    - SageMaker training plans allow you to extend your existing training plans to avoid workload interruptions without workload reconfiguration. When a training plan is approaching expiration, you can extend it directly through the SageMaker AI console or programmatically using the API or AWS CLI.

## __Amazon SimpleDB v2__
  - ### Features
    - Introduced Amazon SimpleDB export functionality enabling domain data export to S3 in JSON format. Added three new APIs StartDomainExport, GetExport, and ListExports via SimpleDBv2 service. Supports cross-region exports and KMS encryption.

## __Amazon WorkSpaces__
  - ### Features
    - Added WINDOWS SERVER 2025 OperatingSystemName.

## __Contributors__
Special thanks to the following contributors to this release: 

[@bsmelo](https://github.com/bsmelo)
# __2.42.10__ __2026-03-10__
## __AWS Database Migration Service__
  - ### Features
    - Not need to include to any release notes. The only change is to correct LoadTimeout unit from milliseconds to seconds in RedshiftSettings

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SDK for Java v2 Code Generator__
  - ### Features
    - Improve model validation error message for operations missing request URI.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adding first class support for AG-UI protocol in AgentCore Runtime.

## __Amazon Connect Cases__
  - ### Features
    - Added functionality for the Required and Hidden case rule types to be conditionally evaluated on up to 5 conditions.

## __Amazon Lex Model Building V2__
  - ### Features
    - This release introduces a new generative AI feature called Lex Bot Analyzer. This feature leverage AI to analyze the bot configuration against AWS Lex best practices to identify configuration issues and provides recommendations.

## __Managed Streaming for Kafka__
  - ### Features
    - Add dual stack endpoint to SDK

# __2.42.9__ __2026-03-09__
## __AWS Identity and Access Management__
  - ### Features
    - Added support for CloudWatch Logs long-term API keys, currently available in Preview

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Implement reset() for XxHashChecksum to allow checksum reuse.

## __Amazon OpenSearch Service__
  - ### Features
    - This change enables cross-account and cross-region access for DataSources. Customers can now define access policies on their datasources to allow other AWS accounts to access and query their data.

## __Amazon Route 53 Global Resolver__
  - ### Features
    - Adds support for dual stack Global Resolvers and Dictionary-based Domain Generation Firewall Advanced Protection.

## __Application Migration Service__
  - ### Features
    - Adds support for new storeSnapshotOnLocalZone field in ReplicationConfiguration and updateReplicationConfiguration

# __2.42.8__ __2026-03-06__
## __AWS Billing and Cost Management Data Exports__
  - ### Features
    - Fixed wrong endpoint resolutions in few regions. Added AWS CFN resource schema for BCM Data Exports. Added max value validation for pagination parameter. Fixed ARN format validation for BCM Data Exports resources. Updated size constraints for table properties. Added AccessDeniedException error.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud now supports cost scale factors for farms, enabling studios to adjust reported costs to reflect their actual rendering economics. Adjusted costs are reflected in Deadline Cloud's Usage Explorer and Budgets.

## __Amazon AppIntegrations Service__
  - ### Features
    - This release adds support for webhooks, allowing customers to create an Event Integration with a webhook source.

## __Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Guardrails account-level enforcement APIs now support lists for model inclusion and exclusion from guardrail enforcement.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds support for streaming memory records in AgentCore Memory

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect now supports the ability to programmatically configure and run automated tests for contact center experiences for Chat. Integrate testing into CICD pipelines, run multiple tests at scale, and retrieve results via API to automate validation of chat interactions and workflows.

## __Amazon GameLift Streams__
  - ### Features
    - Added new Gen6 stream classes based on the EC2 G6f instance family. These stream classes provide cost-optimized options for streaming well-optimized or lower-fidelity games on Windows environments.

## __Amazon Simple Email Service__
  - ### Features
    - Adds support for longer email message header values, increasing the maximum length from 870 to 995 characters for RFC 5322 compliance.

# __2.42.7__ __2026-03-05__
## __AWS Multi-party Approval__
  - ### Features
    - Updates to multi-party approval (MPA) service to add support for approval team baseline operations.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed a thread leak in ResponseInputStream and ResponsePublisher where the internal timeout scheduler thread persisted for the lifetime of the JVM, even when no streams were active. The thread now terminates after being idle for 60 seconds.

## __AWS Savings Plans__
  - ### Features
    - Added support for OpenSearch and Neptune Analytics to Database Savings Plans.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added metadata field to CapacityAllocation.

## __Amazon GuardDuty__
  - ### Features
    - Added MALICIOUS FILE to IndicatorType enum in MDC Sequence

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for S3 Bucket Ownership validation for SageMaker Managed MLflow.

## __Connect Health__
  - ### Features
    - Connect-Health SDK is AWS's unified SDK for the Amazon Connect Health offering. It allows healthcare developers to integrate purpose-built agents - such as patient insights, ambient documentation, and medical coding - into their existing applications, including EHRs, telehealth, and revenue cycle.

# __2.42.6__ __2026-03-04__
## __AWS Elastic Beanstalk__
  - ### Features
    - As part of this release, Beanstalk introduce a new info type - analyze for request environment info and retrieve environment info operations. When customers request an Al analysis, Elastic Beanstalk runs a script on an instance in their environment and returns an analysis of events, health and logs.

## __Amazon Connect Service__
  - ### Features
    - Added support for configuring additional email addresses on queues in Amazon Connect. Agents can now select an outbound email address and associate additional email addresses for replying to or initiating emails.

## __Amazon Elasticsearch Service__
  - ### Features
    - Adds support for DeploymentStrategyOptions.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers now offers DDoS protection for Linux-based EC2 and Container Fleets on SDKv5. The player gateway proxy relay network provides traffic validation, per-player rate limiting, and game server IP address obfuscation all with negligible added latency and no additional cost.

## __Amazon OpenSearch Service__
  - ### Features
    - Adding support for DeploymentStrategyOptions

## __Amazon QuickSight__
  - ### Features
    - Added several new values for Capabilities, increased visual limit per sheet from previous limit to 75, renamed Quick Suite to Quick in several places.

# __2.42.5__ __2026-03-03__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Support for AgentCore Policy GA

## __Amazon CloudWatch Logs__
  - ### Features
    - CloudWatch Logs updates- Added support for the PutBearerTokenAuthentication API to enable or disable bearer token authentication on a log group. For more information, see CloudWatch Logs API documentation.

## __Amazon DataZone__
  - ### Features
    - Adding QueryGraph operation to DataZone SDK

## __Amazon SageMaker Service__
  - ### Features
    - This release adds b300 and g7e instance types for SageMaker inference endpoints.

## __Partner Central Channel API__
  - ### Features
    - Adds the Resold Unified Operations support plan and removes the Resold Business support plan in the CreateRelationship and UpdateRelationship APIs

# __2.42.4__ __2026-02-27__
## __ARC - Region switch__
  - ### Features
    - Post-Recovery Workflows enable customers to maintain comprehensive disaster recovery automation. This allows customer SREs and leadership to have complete recovery orchestration from failover through post-recovery preparation, ensuring Regions remain ready for subsequent recovery events.

## __AWS Batch__
  - ### Features
    - This feature allows customers to specify the minimum time (in minutes) that AWS Batch keeps instances running in a compute environment after all jobs on the instance complete

## __AWS Health APIs and Notifications__
  - ### Features
    - Updates the regex for validating availabilityZone strings used in the describe events filters.

## __AWS Resource Access Manager__
  - ### Features
    - Resource owners can now specify ResourceShareConfiguration request parameter for CreateResourceShare API including RetainSharingOnAccountLeaveOrganization boolean parameter

## __Amazon Bedrock__
  - ### Features
    - Added four new model lifecycle date fields, startOfLifeTime, endOfLifeTime, legacyTime, and publicExtendedAccessTime. Adds support for using the Converse API with Bedrock Batch inference jobs.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Cognito is introducing a two-secret rotation model for app clients, enabling seamless credential rotation without downtime. Dedicated APIs support passing in a custom secret. Custom secrets need to be at least 24 characters. This eliminates reconfiguration needs and reduces security risks.

## __Amazon Connect Customer Profiles__
  - ### Features
    - This release introduces an optional SourcePriority parameter to the ProfileObjectType APIs, allowing you to control the precedence of object types when ingesting data from multiple sources. Additionally, WebAnalytics and Device have been added as new StandardIdentifier values.

## __Amazon Connect Service__
  - ### Features
    - Deprecate EvaluationReviewMetadata's CreatedBy and CreatedTime, add EvaluationReviewMetadata's RequestedBy and RequestedTime

## __Amazon Keyspaces Streams__
  - ### Features
    - Added support for Change Data Capture (CDC) streams with Duration DataType.

## __Amazon Transcribe Streaming Service__
  - ### Features
    - AWS Transcribe Streaming now supports specifying a resumption window for the stream through the SessionResumeWindow parameter, allowing customers to reconnect to their streams for a longer duration beyond stream start time.

## __odb__
  - ### Features
    - ODB Networking Route Management is a feature improvement which allows for implicit creation and deletion of EC2 Routes in the Peer Network Route Table designated by the customer via new optional input. This feature release is combined with Multiple App-VPC functionality for ODB Network Peering(s).

# __2.42.3__ __2026-02-26__
## __AWS Backup Gateway__
  - ### Features
    - This release updates GetGateway API to include deprecationDate and softwareVersion in the response, enabling customers to track gateway software versions and upcoming deprecation dates.

## __AWS Marketplace Entitlement Service__
  - ### Features
    - Added License Arn as a new optional filter for GetEntitlements and LicenseArn field in each entitlement in the response.

## __AWS SecurityHub__
  - ### Features
    - Security Hub added EXTENDED PLAN integration type to DescribeProductsV2 and added metadata.product.vendor name GroupBy support to GetFindingStatisticsV2

## __AWSMarketplace Metering__
  - ### Features
    - Added LicenseArn to ResolveCustomer response and BatchMeterUsage usage records. BatchMeterUsage now accepts LicenseArn in each UsageRecord to report usage at the license level. Added InvalidLicenseException error response for invalid license parameters.

## __Amazon EC2 Container Service__
  - ### Features
    - Adding support for Capacity Reservations for ECS Managed Instances by introducing a new "capacityOptionType" value of "RESERVED" and new field "capacityReservations" for CreateCapacityProvider and UpdateCapacityProvider APIs.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add c8id, m8id and hpc8a instance types.

## __Apache 5 HTTP Client__
  - ### Features
    - Update `httpcore5` to `5.4.1`.

# __2.42.2__ __2026-02-25__
## __AWS Batch__
  - ### Features
    - AWS Batch documentation update for service job capacity units.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS WAFV2__
  - ### Features
    - AWS WAF now supports GetTopPathStatisticsByTraffic that provides aggregated statistics on the top URI paths accessed by bot traffic. Use this operation to see which paths receive the most bot traffic, identify the specific bots accessing them, and filter by category, organization, or bot name.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add support for EC2 Capacity Blocks in Local Zones.

## __Amazon Elastic Container Registry__
  - ### Features
    - Update repository name regex to comply with OCI Distribution Specification

## __Amazon Neptune__
  - ### Features
    - Neptune global clusters now supports tags

# __2.42.1__ __2026-02-24__
## __AWS Elemental Inference__
  - ### Features
    - Initial GA launch for AWS Elemental Inference including capabilities of Smart Crop and Live Event Clipping

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive - Added support for Elemental Inference for Smart Cropping and Clipping features for MediaLive.

## __Amazon CloudWatch__
  - ### Features
    - This release adds the APIs (PutAlarmMuteRule, ListAlarmMuteRules, GetAlarmMuteRule and DeleteAlarmMuteRule) to manage a new Cloudwatch resource, AlarmMuteRules. AlarmMuteRules allow customers to temporarily mute alarm notifications during expected downtime periods.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds httpTokensEnforced property to ModifyInstanceMetadataDefaults API. Set per account or manage organization-wide using declarative policies to prevent IMDSv1-enabled instance launch and block attempts to enable IMDSv1 on existing IMDSv2-only instances.

## __Amazon Elasticsearch Service__
  - ### Features
    - Fixed HTTP binding for DescribeDomainAutoTunes API to correctly pass request parameters as query parameters in the HTTP request.

## __Amazon OpenSearch Service__
  - ### Features
    - Fixed HTTP binding for DescribeDomainAutoTunes API to correctly pass request parameters as query parameters in the HTTP request.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Adding a new field in the CreateCentralizationRuleForOrganization, UpdateCentralizationRuleForOrganization API and updating the GetCentralizationRuleForOrganization API response to include the new field

## __Partner Central Selling API__
  - ### Features
    - Added support for filtering opportunities by target close date in the ListOpportunities API. You can now filter results to return opportunities with a target close date before or after a specified date, enabling more precise opportunity searches based on expected closure timelines.

# __2.42.0__ __2026-02-23__
## __AWS Control Catalog__
  - ### Features
    - Updated ExemptedPrincipalArns parameter documentation for improved accuracy

## __AWS MediaTailor__
  - ### Features
    - Updated endpoint rule set for dualstack endpoints. Added a new opt-in option to log raw ad decision server requests for Playback Configurations.

## __AWS SDK for Java v2__
  - ### Features
    - Add support for additional checksum algorithms: XXHASH64, XXHASH3, XXHASH128, SHA512.
    - Updated endpoint and partition metadata.

## __AWS Wickr Admin API__
  - ### Features
    - AWS Wickr now provides APIs to manage your Wickr OpenTDF integration. These APIs enable you to test and save your OpenTDF configuration allowing you to manage rooms based on Trusted Data Format attributes.

## __Amazon Bedrock__
  - ### Features
    - Automated Reasoning checks in Amazon Bedrock Guardrails now support fidelity report generation. The new workflow type assesses policy coverage and accuracy against customer documents. The GetAutomatedReasoningPolicyBuildWorkflowResultAssets API adds support for the three new asset types.

## __Amazon Connect Cases__
  - ### Features
    - SearchCases API can now accept 25 fields in the request and response as opposed to the previous limit of 10. DeleteField's hard limit of 100 fields per domain has been lifted.

## __Amazon DataZone__
  - ### Features
    - Add workflow properties support to connections APIs

## __Amazon DynamoDB__
  - ### Features
    - This change supports the creation of multi-account global tables. It adds one new arguments to UpdateTable, GlobalTableSettingsReplicationMode.

## __Amazon QuickSight__
  - ### Features
    - Adds support for SEMISTRUCT to InputColumn Type


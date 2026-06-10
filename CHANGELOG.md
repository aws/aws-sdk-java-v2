 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.46.8__ __2026-06-10__
## __AWS Elemental MediaLive__
  - ### Features
    - Adding premixer settings to pid and track audio inputs in MediaLIve to allow greater control over mixing audio from multiple source streams including support for AudioPidSelectors made up of multiple audio PIDs.

## __AWS Sign-In Service__
  - ### Features
    - AWS Sign-In now allows customers to control access to the AWS Management Console using resource-based policies. With this release customers can restrict console access based on network perimeters such as VPC IDs, VPC endpoints, and IP addresses.

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS Managed Daemon task definitions now support pidMode and ipcMode parameters. Set shared to allow daemons to share PID or IPC namespaces with co-located tasks on Managed Instances, enabling process tracing and shared memory communication.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for AMI Watermark which a structured identifier that helps in tracking AMI provenance

## __Amazon Lightsail__
  - ### Features
    - This release adds support for Asia Pacific (Hong Kong) (ap-east-1), Europe (Spain) (eu-south-2) and South America (Sao Paulo) (sa-east-1) Regions.

## __Amazon Prometheus Service__
  - ### Features
    - Adds supports for out-of-order sample ingestion (default 1-minute window) and a configurable rule query offset to reduce data loss and improve alerting accuracy.

## __Amazon S3__
  - ### Bugfixes
    - Fixed an issue where S3 multipart uploads with unknown content length could hang indefinitely when apiCallBufferSizeInBytes was less than twice minimumPartSizeInBytes. The SDK now validates this at request time and fails fast with a descriptive error instead of deadlocking

## __Amazon SageMaker Service__
  - ### Features
    - Add support for G6e instances (ml.g6e.xlarge through ml.g6e.48xlarge) on Amazon SageMaker Notebook Instances.

## __Connect Health__
  - ### Features
    - Add support for MedicalScribeBinaryAudioEvent in the Medical Scribe streaming input. This new event type lets you send audio as a raw binary payload instead of a base64-encoded value

# __2.46.7__ __2026-06-09__
## __AWS Outposts__
  - ### Features
    - Added AWS Outposts APIs for self-service Outposts quoting and ordering. New operations include CreateQuote, GetQuote, UpdateQuote, DeleteQuote, ListQuotes, and ListOrderableInstanceTypes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Adds support for the Amazon Bedrock account-level data retention APIs PutAccountDataRetention and GetAccountDataRetention.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Add RetryableConflictException (HTTP 409) to InvokeAgentRuntimeCommand and GetAgentCard to prevent orphaned VMs during concurrent session access. The SDK automatically retries this exception with backoff. Enforcement is not yet active and will be enabled in a future service update.

## __Amazon CloudWatch__
  - ### Features
    - This release adds the APIs (AssociateDatasetKmsKey, DisassociateDatasetKmsKey, GetDataset) to manage encryption at rest for OpenTelemetry metrics in CloudWatch using AWS KMS customer managed keys.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added TagFieldSpecifications to CreateFlowLogs and DescribeFlowLogs APIs. Customers can now specify tag keys in their Flow Logs subscriptions to capture associated EC2 resource tag values in their logs, enabling tag-based visibility.

## __Amazon SNS Message Manager__
  - ### Bugfixes
    - Fixed `SnsMessageManager` rejecting valid SNS messages whose signature timestamp falls on a whole second (zero milliseconds): the canonical string was rebuilt with `Instant#toString()`, which drops the `.000` fraction and no longer matched the value Amazon SNS signed.
        - Contributed by: [@henricook](https://github.com/henricook)

## __odb__
  - ### Features
    - Releases Autonomous Database Serverless APIs, autonomousDatabaseOciIntegrationIamRoles, linkedOciTenancyId, linkedOciCompartmentId, and subscriptionErrors fields in GetOciOnboardingStatus API response.

## __Contributors__
Special thanks to the following contributors to this release: 

[@henricook](https://github.com/henricook)
# __2.46.6__ __2026-06-08__
## __AWS Compute Optimizer__
  - ### Features
    - Adds new Idle Recommendation Resource types in the AWS Compute Optimizer API

## __AWS DevOps Agent Service__
  - ### Features
    - Add Asset APIs for managing versioned assets and asset files in AWS DevOps Agent agent spaces.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - Adds support for DASH Audio Timeline Patternization. This enables your DASH manifests to templatize the repeating patterns that emerge in audio segment timelines. This compacts the total timeline length, utilizing the repeat notation, such that manifests don't grow indefinitely long.

## __AWSDeadlineCloud__
  - ### Features
    - Added optional identityCenterRegion parameter to AssociateMember APIs to allow managing memberships for users and groups in other regions.

## __Amazon Omics__
  - ### Features
    - StartRunBatch API - Add EngineSettings

## __Application Migration Service__
  - ### Features
    - AWS Transform discovery tool now supported as network migration input source. You can now use the AWS Transform Discovery tool as a source for network migration alongside modelizeIT, enabling hybrid network migrations for environments running both VMware and non-VMware workloads.

## __CloudWatch Observability Admin Service__
  - ### Features
    - CloudWatch Observability Admin extends CentralizationRuleForOrganization APIs to support metrics, enabling centralization of metrics across accounts and Regions alongside logs.

## __Cost Optimization Hub__
  - ### Features
    - Adds new Idle Recommendation types in the Cost Optimization Hub API

## __Tax Settings__
  - ### Features
    - Adds support for additional tax information fields for Philippines, Belgium, Chile, France, Poland, and Italy in the Tax Settings API.

# __2.46.5__ __2026-06-05__
## __AWS Elemental MediaConvert__
  - ### Features
    - Adds support for configurable number of Clear Lead segments at the beginning of encrypted output. Adds support for multiple trickplay variants.

## __AWS SDK for Java v2__
  - ### Features
    - Update Netty to 4.1.135
    - Updated endpoint and partition metadata.

## __Amazon QuickSight__
  - ### Features
    - Adds support for Knowledge Base APIs and Index Capacity API

## __Amazon SageMaker Service__
  - ### Features
    - This release adds support for MLflow experiment tracking in SageMaker inference optimization. CreateAIRecommendationJob and CreateAIBenchmarkJob now accept an optional OutputConfig.MlflowConfig (MLflow App ARN, experiment, run name) to stream benchmark metrics and artifacts to your own MLflow App.

## __EMR Serverless__
  - ### Features
    - Adds support for updating max capacity and custom fields while application is started

## __Payment Cryptography Control Plane__
  - ### Features
    - Adds CloudFormation support for resource-based policies on AWS Payment Cryptography keys.

# __2.46.4__ __2026-06-04__
## __AWS Audit Manager__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Fix connection pool exhaustion in the CRT HTTP client where connections were not released after a request abort or timeout.

## __AWS CloudFormation__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __AWS Config__
  - ### Features
    - AWS Config now supports internal service-linked rules, allowing AWS service partners to deploy Config rules for customers and use the evaluation results to build enhanced features.

## __AWS Glue__
  - ### Features
    - AWS Glue Interactive Sessions now supports Apache Spark Connect, enabling remote Spark execution over gRPC with minimal client-side dependencies. Adds GetSessionEndpoint and GetDashboardUrl APIs. Modifies CreateSession now accepts SPARK CONNECT session type.

## __AWS Wickr Admin API__
  - ### Features
    - AWS Wickr now allows network administrators to configure a maximum session duration for non-SSO users in security groups, and display customizable consent popups to users at login for terms of use or compliance acknowledgements.

## __AWSKendraFrontendService__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon AppIntegrations Service__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon Appflow__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon Chime SDK Voice__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon Connect Participant Service__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon EMR__
  - ### Features
    - Added support for Spark Connect interactive sessions on Amazon EMR on EC2 with new APIs - StartSession, GetSession, GetSessionEndpoint, ListSessions, and TerminateSession. Added sessionEnabled field in RunJobFlow and DescribeCluster to enable Spark Connect endpoints on EMR clusters.

## __Amazon Elastic File System__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon GuardDuty__
  - ### Features
    - Remove unsupported RDS field for filter

## __Amazon Interactive Video Service__
  - ### Features
    - adds UpdateAdConfiguration operation to AWS IVS low-latency APIs

## __Amazon SageMaker Service__
  - ### Features
    - Adds the IncludedData parameter to DescribeModelCard and DescribeModelPackage. Set it to MetadataOnly to retrieve a model card without decrypt permission on the customer managed AWS KMS key (default AllData returns full content). Adds support for the MTRL Job resource in SageMaker Search.

## __Amazon Simple Notification Service__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon WorkDocs__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Amazon WorkSpaces__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

## __Tax Settings__
  - ### Features
    - Adding new BDD representation of endpoint ruleset

# __2.46.3__ __2026-06-03__
## __ARC - Region switch__
  - ### Features
    - ARC Region Switch now supports three new execution blocks for multi-Region database workloads-Amazon Aurora Serverless scaling, Amazon Aurora Provisioned scaling, and Amazon Neptune Global Database failover.

## __AWS Compute Optimizer__
  - ### Features
    - This release lets customers extend the lookback period for Amazon EBS volume and Amazon ECS rightsizing recommendations to 32 days.

## __AWS Cost Explorer Service__
  - ### Features
    - Added support for target-coverage-based Savings Plans purchase analysis. The StartCommitmentPurchaseAnalysis API now accepts a new TARGET AVERAGE COVERAGE value for AnalysisType, as well as an optional SavingsPlansTargetCoverage field in SavingsPlansPurchaseAnalysisConfiguration

## __AWS End User Messaging Social__
  - ### Features
    - Adding support for WhatsApp flow APIs and adding AccessDeniedByMetaException for Template APIs

## __Amazon Connect Service__
  - ### Features
    - SearchContacts Connect API now supports filtering contacts by the AI Agents involved in handling them

## __Inspector2__
  - ### Features
    - Inspector support for enhanced scanning

# __2.46.2__ __2026-06-02__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Location Service Routes V2__
  - ### Features
    - Add "standardRegionalEndpoints" back to fix 'Could not connect to the endpoint URL'

# __2.46.1__ __2026-06-02__
## __AWS IoT__
  - ### Features
    - Fleet indexing documentation update

## __AWS Lambda__
  - ### Features
    - Adds configuration for tag propagation to Lambda-managed resources.

## __Amazon ElastiCache__
  - ### Features
    - Amazon ElastiCache for Valkey now supports durability. This new capability is enabled through a Multi-AZ transactional log, enabling fast recovery and restart during failures.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 now supports self-service cancellation of future-dated Capacity Reservations. A cancellation charge applies based on remaining commitment. Customers can generate a cancellation quote to review charges before confirming.

## __Amazon GuardDuty__
  - ### Features
    - Amazon GuardDuty Runtime Monitoring now supports 3 new SensitiveFileModified finding types (Persistence, PrivilegeEscalation, DefenseEvasion) that detect when security-sensitive system files are modified on EC2 instances or containers, indicating potential compromise through file tampering.

## __Amazon Keyspaces Streams__
  - ### Features
    - Added iterator description to the GetRecords API response for Amazon Keyspaces Change Data Capture (CDC) streams, enabling consumers to track their current position within the stream.

## __Amazon Location Service Routes V2__
  - ### Features
    - Added Transit and Intermodal travel modes to CalculateRoutes. Plan routes using public transit (bus, subway, train, ferry) or combine transit with driving, taxi, and rental car segments in a single multi-modal route.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker Job is a new service to help you manage various workloads related to model fine tuning, evaluation etc. Two job categories are supported today, AgentRFT for multi-turn agentic reinforcement fine tuning, and AgentRFTEvaluation for evaluating base model or trained model from AgentRFT.

## __Amazon Transcribe Service__
  - ### Features
    - Release new Language locales including am-ET, es-MX, fa-AF, ht-HT, jv-ID, km-KH, my-MM, sq-AL, ne-NP. The commit shows past locales that have already been release which include cy-gb, ga-ie, gd-gb.

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fix a resource leak in `downloadDirectory` where cancelling the download could still allow file transfers that arrive late to recreate the destination directory. Added a guard in `DownloadDirectoryHelper` to skip file downloads after the operation is cancelled.

## __Sagemaker Job Runtime Service__
  - ### Features
    - Amazon SageMaker Job Runtime is a new service for managing trajectory data during multi-turn customization jobs. It provides APIs to send inference requests to models during job execution, mark rollouts as complete, and submit reward values for training trajectories.

# __2.46.0__ __2026-06-01__
## __AWS Marketplace Agreement Service__
  - ### Features
    - Adding Entitlements in SearchAgreements Response

## __AWS SDK for Java v2__
  - ### Features
    - This update replaces the default `apache-client` runtime dependency of service clients with the new `apache5-client`. This means that service clients will now use the `Apache5HttpClient` by default if no HTTP client is explicitly configured on the service client builder.\n Notable changes:\n - Apache 5 uses different logger names than Apache 4\n - Expect: 100-Continue is disabled by default\n - TCP keep-alive socket options require `jdk.net.NetworkPermission` when SecurityManager is active

## __Amazon Cognito Identity Provider__
  - ### Features
    - Add support for multi-region replication, enabling synchronization of user data and configurations to a secondary user pool in a standby Region. Add support for customer managed keys (CMK) in AWS KMS for encrypting user pool data at rest.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Increase code coverage on dynamodb-enhanced module

## __Amazon QuickSight__
  - ### Features
    - This release adds public APIs for Amazon QuickSight Spaces, Agents, and Flows. Spaces APIs enable management of curated resource collections. Agents APIs provide lifecycle control over AI-powered agents that leverage Spaces. Flows APIs add CRUDL APIs for automated workflows.

## __Apache HTTP Client 5__
  - ### Features
    - Upgrade httpcomponents.client5 to 5.6.1


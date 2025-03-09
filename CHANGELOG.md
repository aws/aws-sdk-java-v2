 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.30.36__ __2025-03-07__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - Introduces support for Neptune Analytics as a vector data store and adds Context Enrichment Configurations, enabling use cases such as GraphRAG.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Support Multi Agent Collaboration within Inline Agents

## __Amazon CloudFront__
  - ### Features
    - Documentation updates for Amazon CloudFront.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add serviceManaged field to DescribeAddresses API response.

## __Amazon Neptune Graph__
  - ### Features
    - Several small updates to resolve customer requests.

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for assigning IP addresses to Application Load Balancers from VPC IP Address Manager pools.

# __2.30.35__ __2025-03-06__
## __AWS CloudTrail__
  - ### Features
    - Doc-only update for CloudTrail.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS WAFV2__
  - ### Features
    - You can now perform an exact match or rate limit aggregation against the web request's JA4 fingerprint.

## __Amazon Bedrock__
  - ### Features
    - This releases adds support for Custom Prompt Router

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - IVS Real-Time now offers customers the ability to merge fragmented recordings in the event of a participant disconnect.

## __Amazon WorkSpaces__
  - ### Features
    - Added a new ModifyEndpointEncryptionMode API for managing endpoint encryption settings.

## __Network Flow Monitor__
  - ### Features
    - This release contains 2 changes. 1: DeleteScope/GetScope/UpdateScope operations now return 404 instead of 500 when the resource does not exist. 2: Expected string format for clientToken fields of CreateMonitorInput/CreateScopeInput/UpdateMonitorInput have been updated to be an UUID based string.

## __Redshift Data API Service__
  - ### Features
    - This release adds support for ListStatements API to filter statements by ClusterIdentifier, WorkgroupName, and Database.

# __2.30.34__ __2025-03-05__
## __AWS DataSync__
  - ### Features
    - AWS DataSync now supports modifying ServerHostname while updating locations SMB, NFS, and ObjectStorage.

## __AWS IoT FleetWise__
  - ### Features
    - This release adds floating point support for CAN/OBD signals and adds support for signed OBD signals.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SDK for Java v2 Migration Tool__
  - ### Bugfixes
    - Transform the setter methods on the service model classes that take SdkBytes to take ByteBuffer to be compatible with v1 style setters

## __Amazon Bedrock Runtime__
  - ### Features
    - This releases adds support for Custom Prompt Router ARN

## __Amazon GameLift Streams__
  - ### Features
    - New Service: Amazon GameLift Streams delivers low-latency game streaming from AWS global infrastructure to virtually any device with a browser at up to 1080p resolution and 60 fps.

## __Amazon WorkSpaces__
  - ### Features
    - Added DeviceTypeWorkSpacesThinClient type to allow users to access their WorkSpaces through a WorkSpaces Thin Client.

# __2.30.33__ __2025-03-04__
## __AWS IoT SiteWise__
  - ### Features
    - AWS IoT SiteWise now supports MQTT-enabled, V3 gateways. Configure data destinations for real-time ingestion into AWS IoT SiteWise or buffered ingestion using Amazon S3 storage. You can also use path filters for precise data collection from specific MQTT topics.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon ElastiCache__
  - ### Features
    - Doc only update, listing 'valkey7' and 'valkey8' as engine options for parameter groups.

## __Amazon Relational Database Service__
  - ### Features
    - Note support for Database Insights for Amazon RDS.

## __Managed integrations for AWS IoT Device Management__
  - ### Features
    - Adding managed integrations APIs for IoT Device Management to setup and control devices across different manufacturers and connectivity protocols. APIs include managedthing operations, credential and provisioning profile management, notification configuration, and OTA update.

# __2.30.32__ __2025-03-03__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Improving codegen error message when shapes have lowercased names.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Added the capacity to return available challenges in admin authentication and to set version 3 of the pre token generation event for M2M ATC.

## __Amazon DynamoDB Enhanced Client__
  - ### Bugfixes
    - Fixed DynamoDbEnhancedClient TableSchema::itemToMap to return a map that contains a consistent representation of null top-level (non-flattened) attributes and flattened attributes when their enclosing member is null and ignoreNulls is set to false.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Update the DescribeVpcs response

## __Amazon SageMaker Service__
  - ### Features
    - Add DomainId to CreateDomainResponse

## __Amazon Transcribe Service__
  - ### Features
    - Updating documentation for post call analytics job queueing.

## __CloudWatch RUM__
  - ### Features
    - Add support for PutResourcePolicy, GetResourcePolicy and DeleteResourcePolicy to support resource based policies for AWS CloudWatch RUM

## __QBusiness__
  - ### Features
    - Adds support for the ingestion of audio and video files by Q Business, which can be configured with the mediaExtractionConfiguration parameter.

# __2.30.31__ __2025-02-28__
## __AWS Database Migration Service__
  - ### Features
    - Add skipped status to the Result Statistics of an Assessment Run

## __AWS Elemental MediaConvert__
  - ### Features
    - The AWS MediaConvert Probe API allows you to analyze media files and retrieve detailed metadata about their content, format, and structure.

## __AWS Price List Service__
  - ### Features
    - Update GetProducts and DescribeServices API request input validations.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - This release lets Amazon Bedrock Flows support newer models by increasing the maximum length of output in a prompt configuration. This release also increases the maximum number of prompt variables to 20 and the maximum number of node inputs to 20.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adding licenses to EKS Anywhere Subscription operations response.

## __Amazon Simple Queue Service__
  - ### Bugfixes
    - Fixed memory leak in SqsBatch Manager: Resolved an issue where pendingResponses and pendingBatchResponses collections in RequestBatchManager retained references to completed futures, causing memory accumulation over time.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Systems Manager doc-only updates for Feb. 2025.

## __Data Automation for Amazon Bedrock__
  - ### Features
    - Renamed and added new StandardConfiguration enums. Added support to update EncryptionConfiguration in UpdateBlueprint and UpdateDataAutomation APIs. Changed HttpStatus code for DeleteBlueprint and DeleteDataAutomationProject APIs to 200 from 204. Added APIs to support tagging.

## __Runtime for Amazon Bedrock Data Automation__
  - ### Features
    - Added a mandatory parameter DataAutomationProfileArn to support for cross region inference for InvokeDataAutomationAsync API. Renamed DataAutomationArn to DataAutomationProjectArn. Added APIs to support tagging.

# __2.30.30__ __2025-02-27__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue in AwsServiceException#getMessage() where it returned an empty string instead of null when the message is null.
    - Handle SecurityException for ProfileFileLocation access checks while accessing aws shared credentials file.

## __AWS Storage Gateway__
  - ### Features
    - This release adds support to invoke a process that cleans the specified file share's cache of file entries that are failing upload to Amazon S3.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Introduces Sessions (preview) to enable stateful conversations in GenAI applications.

## __Amazon EMR__
  - ### Features
    - Definition update for EbsConfiguration.

## __Amazon SageMaker Service__
  - ### Features
    - SageMaker HubService is introducing support for creating Training Jobs in Curated Hub (Private Hub). Additionally, it is introducing two new APIs: UpdateHubContent and UpdateHubContentReference.

## __QBusiness__
  - ### Features
    - This release supports deleting attachments from conversations.

## __Redshift Serverless__
  - ### Features
    - Add track support for Redshift Serverless workgroup.

# __2.30.29__ __2025-02-26__
## __AWS Batch__
  - ### Features
    - AWS Batch: Resource Aware Scheduling feature support

## __AWS IoT FleetWise__
  - ### Features
    - This release adds an optional listResponseScope request parameter in certain list API requests to limit the response to metadata only.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Chime__
  - ### Features
    - Removes the Amazon Chime SDK APIs from the "chime" namespace. Amazon Chime SDK APIs continue to be available in the AWS SDK via the dedicated Amazon Chime SDK namespaces: chime-sdk-identity, chime-sdk-mediapipelines, chime-sdk-meetings, chime-sdk-messaging, and chime-sdk-voice.

## __Amazon CloudFront__
  - ### Features
    - Documentation update for VPC origin config.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - This release adds API support for reading Service Level Objectives and Services from monitoring accounts, from SLO and Service-scoped operations, including ListServices and ListServiceLevelObjectives.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Add support in BeanTableSchema for beans that use fluent setters.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 Fleet customers can now override the Block Device Mapping specified in the Launch Template when creating a new Fleet request, saving the effort of creating and associating new Launch Templates to customize the Block Device Mapping.

## __Amazon SageMaker Service__
  - ### Features
    - AWS SageMaker InferenceComponents now support rolling update deployments for Inference Components.

## __CloudWatch Observability Access Manager__
  - ### Features
    - This release adds support for sharing AWS::ApplicationSignals::Service and AWS::ApplicationSignals::ServiceLevelObjective resources.

# __2.30.28__ __2025-02-25__
## __AWS CodeBuild__
  - ### Features
    - Adding "reportArns" field in output of BatchGetBuildBatches API. "reportArns" is an array that contains the ARNs of reports created by merging reports from builds associated with the batch build.

## __AWS Device Farm__
  - ### Features
    - Add an optional configuration to the ScheduleRun and CreateRemoteAccessSession API to set a device level http/s proxy.

## __AWS IoT__
  - ### Features
    - AWS IoT - AWS IoT Device Defender adds support for a new Device Defender Audit Check that monitors device certificate age and custom threshold configurations for both the new device certificate age check and existing device certificate expiry check.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds support for time-based EBS-backed AMI copy operations. Time-based copy ensures that EBS-backed AMIs are copied within and across Regions in a specified timeframe.

## __Tax Settings__
  - ### Features
    - PutTaxRegistration API changes for Egypt, Greece, Vietnam countries

# __2.30.27__ __2025-02-24__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - This release improves support for newer models in Amazon Bedrock Flows.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Adding support for ReasoningContent fields in Pre-Processing, Post-Processing and Orchestration Trace outputs.

## __Amazon Bedrock Runtime__
  - ### Features
    - This release adds Reasoning Content support to Converse and ConverseStream APIs

## __Amazon ElastiCache__
  - ### Features
    - Documentation update, adding clarity and rephrasing.

# __2.30.26__ __2025-02-21__
## __AWS CRT HTTP Client__
  - ### Features
    - Allow users to configure the number of TCP keep alive probes in the AWS CRT HTTP client through `keepAliveProbes`.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Transfer-Encoding headers will no longer be signed during SigV4 authentication

## __Agents for Amazon Bedrock__
  - ### Features
    - Introduce a new parameter which represents the user-agent header value used by the Bedrock Knowledge Base Web Connector.

## __Amazon AppStream__
  - ### Features
    - Added support for Certificate-Based Authentication on AppStream 2.0 multi-session fleets.

## __DynamoDB Enhanced Client__
  - ### Features
    - Add ability to provide a custom `MethodHandles.Lookup` object when using either `BeanTableSchema` or `ImmutableTableSchema`. By setting a custom `MethodHandles.Lookup` it allows these schemas to be used in applications where the item class and the SDK are loaded by different `ClassLoader`s.

# __2.30.25__ __2025-02-20__
## __AWS CodeBuild__
  - ### Features
    - Add webhook status and status message to AWS CodeBuild webhooks

## __AWS License Manager User Subscriptions__
  - ### Features
    - Updates entity to include Microsoft RDS SAL as a valid type of user subscription.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon GuardDuty__
  - ### Features
    - Reduce the minimum number of required attack sequence signals from 2 to 1

## __Amazon Relational Database Service__
  - ### Features
    - CloudWatch Database Insights now supports Amazon RDS.

## __Amazon S3__
  - ### Features
    - Allow users to configure disableS3ExpressSessionAuth for S3CrtAsyncClient

## __Amazon SageMaker Service__
  - ### Features
    - Added new capability in the UpdateCluster operation to remove instance groups from your SageMaker HyperPod cluster.

## __Amazon WorkSpaces Web__
  - ### Features
    - Add support for toolbar configuration under user settings.

# __2.30.24__ __2025-02-19__
## __AWS CodePipeline__
  - ### Features
    - Add environment variables to codepipeline action declaration.

## __AWS Network Firewall__
  - ### Features
    - This release introduces Network Firewall's Automated Domain List feature. New APIs include UpdateFirewallAnalysisSettings, StartAnalysisReport, GetAnalysisReportResults, and ListAnalysisReports. These allow customers to enable analysis on firewalls to identify and report frequently accessed domain.

## __AWS SDK for Java v2__
  - ### Features
    - Add retry attempt count to exception messages to improve debugging visibility.

## __Amazon EC2 Container Service__
  - ### Features
    - This is a documentation only release for Amazon ECS that supports the CPU task limit increase.

## __Amazon Lightsail__
  - ### Features
    - Documentation updates for Amazon Lightsail.

## __Amazon Location Service__
  - ### Features
    - Adds support for larger property maps for tracking and geofence positions changes. It increases the maximum number of items from 3 to 4, and the maximum value length from 40 to 150.

## __Amazon SageMaker Service__
  - ### Features
    - Adds r8g instance type support to SageMaker Realtime Endpoints

## __Amazon Simple Email Service__
  - ### Features
    - This release adds the ability for outbound email sent with SES to preserve emails to a Mail Manager archive.

## __MailManager__
  - ### Features
    - This release adds additional metadata fields in Mail Manager archive searches to show email source and details about emails that were archived when being sent with SES.

# __2.30.23__ __2025-02-18__
## __AWS Batch__
  - ### Features
    - This documentation-only update corrects some typos.

## __AWS Elemental MediaLive__
  - ### Features
    - Adds support for creating CloudWatchAlarmTemplates for AWS Elemental MediaTailor Playback Configuration resources.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Cloudfront__
  - ### Bugfixes
    - Allow users to specify resource URL pattern in `CloudFrontUtilities#getSignedUrlWithCustomPolicy`. See [#5577](https://github.com/aws/aws-sdk-java-v2/issues/5577)

## __Amazon EMR Containers__
  - ### Features
    - EMR on EKS StartJobRun Api will be supporting the configuration of log storage in AWS by using "managedLogs" under "MonitoringConfiguration".

# __2.30.22__ __2025-02-17__
## __AWS Amplify__
  - ### Features
    - Add ComputeRoleArn to CreateApp, UpdateApp, CreateBranch, and UpdateBranch, allowing caller to specify a role to be assumed by Amplify Hosting for server-side rendered applications.

## __AWS CRT HTTP Client__
  - ### Features
    - Add support for ML-KEM in Post-Quantum TLS Config
        - Contributed by: [@alexw91](https://github.com/alexw91)

## __AWS Database Migration Service__
  - ### Features
    - Support replicationConfigArn in DMS DescribeApplicableIndividualAssessments API.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Timestream InfluxDB__
  - ### Features
    - This release introduces APIs to manage DbClusters and adds support for read replicas

## __Contributors__
Special thanks to the following contributors to this release: 

[@alexw91](https://github.com/alexw91)
# __2.30.21__ __2025-02-14__
## __AWS CodeBuild__
  - ### Features
    - Added test suite names to test case metadata

## __AWS Database Migration Service__
  - ### Features
    - Introduces premigration assessment feature to DMS Serverless API for start-replication and describe-replications

## __AWS RDS DataService__
  - ### Features
    - Add support for Stop DB feature.

## __AWS WAFV2__
  - ### Features
    - The WAFv2 API now supports configuring data protection in webACLs.

## __Amazon Connect Service__
  - ### Features
    - Release Notes: 1) Analytics API enhancements: Added new ListAnalyticsDataLakeDataSets API. 2) Onboarding API Idempotency: Adds ClientToken to instance creation and management APIs to support idempotency.

## __Amazon Simple Storage Service__
  - ### Features
    - Added support for Content-Range header in HeadObject response.

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Update Environment and Device name field definitions

# __2.30.20__ __2025-02-13__
## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Private Certificate Authority (PCA) documentation updates

## __AWS Fault Injection Simulator__
  - ### Features
    - Adds auto-pagination for the following operations: ListActions, ListExperimentTemplates, ListTargetAccountConfigurations, ListExperiments, ListExperimentResolvedTargets, ListTargetResourceTypes. Reduces length constraints of prefixes for logConfiguration and experimentReportConfiguration.

## __AWS Storage Gateway__
  - ### Features
    - This release adds support for generating cache reports on S3 File Gateways for files that fail to upload.

## __Access Analyzer__
  - ### Features
    - This release introduces the getFindingsStatistics API, enabling users to retrieve aggregated finding statistics for IAM Access Analyzer's external access and unused access analysis features. Updated service API and documentation.

## __Amazon EC2 Container Service__
  - ### Features
    - This is a documentation only release to support migrating Amazon ECS service ARNs to the long ARN format.

## __Amazon SageMaker Service__
  - ### Features
    - Adds additional values to the InferenceAmiVersion parameter in the ProductionVariant data type.

## __Apache Http Client__
  - ### Features
    - Allow users to configure authSchemeProviderRegistry for ApacheHttpClient

# __2.30.19__ __2025-02-12__
## __AWS B2B Data Interchange__
  - ### Features
    - Allow spaces in the following fields in the Partnership resource: ISA 06 - Sender ID, ISA 08 - Receiver ID, GS 02 - Application Sender Code, GS 03 - Application Receiver Code

## __AWS CodeBuild__
  - ### Features
    - Add note for the RUNNER_BUILDKITE_BUILD buildType.

## __AWS Elemental MediaLive__
  - ### Features
    - Adds a RequestId parameter to all MediaLive Workflow Monitor create operations. The RequestId parameter allows idempotent operations.

## __Agents for Amazon Bedrock__
  - ### Features
    - This releases adds the additionalModelRequestFields field to the CreateAgent and UpdateAgent operations. Use additionalModelRequestFields to specify additional inference parameters for a model beyond the base inference parameters.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This releases adds the additionalModelRequestFields field to the InvokeInlineAgent operation. Use additionalModelRequestFields to specify additional inference parameters for a model beyond the base inference parameters.

## __Amazon FSx__
  - ### Features
    - Support for in-place Lustre version upgrades

## __Amazon Polly__
  - ### Features
    - Added support for the new voice - Jasmine (en-SG). Jasmine is available as a Neural voice only.

## __Amazon S3__
  - ### Bugfixes
    - Fixed an issue in the S3 client where it skipped checksum calculation for operations that use SigV4a signing and require checksums. See [#5878](https://github.com/aws/aws-sdk-java-v2/issues/5878).

## __OpenSearch Service Serverless__
  - ### Features
    - Custom OpenSearchServerless Entity ID for SAML Config.

# __2.30.18__ __2025-02-11__
## __AWS AppSync__
  - ### Features
    - Add support for operation level caching

## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Private Certificate Authority service now supports Partitioned CRL as a revocation configuration option.

## __AWS Performance Insights__
  - ### Features
    - Documentation only update for RDS Performance Insights dimensions for execution plans and locking analysis.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adding support for the new fullSnapshotSizeInBytes field in the response of the EC2 EBS DescribeSnapshots API. This field represents the size of all the blocks that were written to the source volume at the time the snapshot was created.

# __2.30.17__ __2025-02-10__
## __AWS CRT-based S3 Client__
  - ### Bugfixes
    - Fixed an issue in AWS CRT-based S3 client where checksums are not calculated for operations that require checksums when RequestChecksumCalculation.WHEN_REQUIRED is configured, resulting error.

## __AWS Database Migration Service__
  - ### Features
    - New vendors for DMS Data Providers: DB2 LUW and DB2 for z/OS

## __AWS SDK for Java v2__
  - ### Features
    - The SDK now does not buffer input data from ContentStreamProvider in cases where content length is known.
    - The SDK now does not buffer input data from `RequestBody#fromInputStream` in cases where the InputStream does not support mark and reset.
    - The SDK now throws exception for input streaming operation if the stream has fewer bytes (i.e. reaches EOF) before the expected length is reached.
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Remove unnecessary invocation of `ContentStreamProvider#newStream` when content-length is known for requests that use AWS chunked encoding.

## __Amazon CloudFront__
  - ### Features
    - Doc-only update that adds defaults for CloudFront VpcOriginEndpointConfig values.

## __Amazon Connect Service__
  - ### Features
    - Updated the CreateContact API documentation to indicate that it only applies to EMAIL contacts.

## __AmazonApiGatewayV2__
  - ### Features
    - Documentation updates for Amazon API Gateway

# __2.30.16__ __2025-02-07__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds support for Animated GIF output, forced chroma sample positioning metadata, and Extensible Wave Container format

## __AWS Performance Insights__
  - ### Features
    - Adds documentation for dimension groups and dimensions to analyze locks for Database Insights.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Container Registry__
  - ### Features
    - Adds support to handle the new basic scanning daily quota.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Introduce versionStatus field to take place of status field in EKS DescribeClusterVersions API

## __Amazon Transcribe Service__
  - ### Features
    - This release adds support for the Clinical Note Template Customization feature for the AWS HealthScribe APIs within Amazon Transcribe.

## __Amazon Transcribe Streaming Service__
  - ### Features
    - This release adds support for the Clinical Note Template Customization feature for the AWS HealthScribe Streaming APIs within Amazon Transcribe.

# __2.30.15__ __2025-02-06__
## __AWS CRT HTTP Client__
  - ### Features
    - Allow users to configure connectionAcquisitionTimeout for AwsCrtHttpClient and AwsCrtAsyncHttpClient

## __AWS CloudFormation__
  - ### Features
    - We added 5 new stack refactoring APIs: CreateStackRefactor, ExecuteStackRefactor, ListStackRefactors, DescribeStackRefactor, ListStackRefactorActions.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Connect Cases__
  - ### Features
    - This release adds the ability to conditionally require fields on a template. Check public documentation for more information.

## __Amazon Simple Storage Service__
  - ### Features
    - Updated list of the valid AWS Region values for the LocationConstraint parameter for general purpose buckets.

## __Cost Optimization Hub__
  - ### Features
    - This release enables AWS Cost Optimization Hub to show cost optimization recommendations for Amazon Auto Scaling Groups, including those with single and mixed instance types.

# __2.30.14__ __2025-02-05__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix an issue where the trailing checksum of a request body is not sent when the `Content-Length` header is explicitly set to `0`.

## __Amazon Relational Database Service__
  - ### Features
    - Documentation updates to clarify the description for the parameter AllocatedStorage for the DB cluster data type, the description for the parameter DeleteAutomatedBackups for the DeleteDBCluster API operation, and removing an outdated note for the CreateDBParameterGroup API operation.

## __Netty NIO HTTP Client__
  - ### Features
    - Fallback to prior knowledge if default client setting is ALPN and request has HTTP endpoint

# __2.30.13__ __2025-02-04__
## __AWS DataSync__
  - ### Features
    - Doc-only update to provide more information on using Kerberos authentication with SMB locations.

## __AWS Database Migration Service__
  - ### Features
    - Introduces TargetDataSettings with the TablePreparationMode option available for data migrations.

## __AWS Identity and Access Management__
  - ### Features
    - This release adds support for accepting encrypted SAML assertions. Customers can now configure their identity provider to encrypt the SAML assertions it sends to IAM.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue in the SDK where it unnecessarily buffers the entire content for streaming operations, causing OOM error. See [#5850](https://github.com/aws/aws-sdk-java-v2/issues/5850).

## __Amazon Neptune Graph__
  - ### Features
    - Added argument to `list-export` to filter by graph ID

## __Amazon SageMaker Service__
  - ### Features
    - IPv6 support for Hyperpod clusters

## __QBusiness__
  - ### Features
    - Adds functionality to enable/disable a new Q Business Chat orchestration feature. If enabled, Q Business can orchestrate over datasources and plugins without the need for customers to select specific chat modes.

# __2.30.12__ __2025-02-03__
## __AWS MediaTailor__
  - ### Features
    - Add support for CloudWatch Vended Logs which allows for delivery of customer logs to CloudWatch Logs, S3, or Firehose.

# __2.30.11__ __2025-01-31__
## __AWS CodeBuild__
  - ### Features
    - Added support for CodeBuild self-hosted Buildkite runner builds

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed an issue in SdkHttpUtils used in SdkHttpFullRequest where constructing with a query string consisting of a single "=" would throw an ArrayIndexOutOfBoundsException.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This change is to deprecate the existing citation field under RetrieveAndGenerateStream API response in lieu of GeneratedResponsePart and RetrievedReferences

## __Amazon Location Service Routes V2__
  - ### Features
    - The OptimizeWaypoints API now supports 50 waypoints per request (20 with constraints like AccessHours or AppointmentTime). It adds waypoint clustering via Clustering and ClusteringIndex for better optimization. Also, total distance validation is removed for greater flexibility.

## __Amazon Prometheus Service__
  - ### Features
    - Add support for sending metrics to cross account and CMCK AMP workspaces through RoleConfiguration on Create/Update Scraper.

## __Amazon Relational Database Service__
  - ### Features
    - Updates to Aurora MySQL and Aurora PostgreSQL API pages with instance log type in the create and modify DB Cluster.

## __Amazon S3__
  - ### Bugfixes
    - Stopped populating SessionMode by default for the SDK-created S3 express sessions. This value already matched the service-side default, and was already not sent by most SDK languages.

## __Amazon SageMaker Service__
  - ### Features
    - This release introduces a new valid value in InstanceType parameter: p5en.48xlarge, in ProductionVariant.

# __2.30.10__ __2025-01-30__
## __AWS MediaTailor__
  - ### Features
    - Adds options for configuring how MediaTailor conditions ads before inserting them into the content stream. Based on the new settings, MediaTailor will either transcode ads to match the content stream as it has in the past, or it will insert ads without first transcoding them.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Add a 'reason' field to InternalServerException

## __Amazon AppStream__
  - ### Features
    - Add support for managing admin consent requirement on selected domains for OneDrive Storage Connectors in AppStream2.0.

## __Amazon Elastic Container Registry__
  - ### Features
    - Temporarily updating dualstack endpoint support

## __Amazon Elastic Container Registry Public__
  - ### Features
    - Temporarily updating dualstack endpoint support

## __Amazon S3 Tables__
  - ### Features
    - You can now use the CreateTable API operation to create tables with schemas by adding an optional metadata argument.

## __Amazon Verified Permissions__
  - ### Features
    - Adds Cedar JSON format support for entities and context data in authorization requests

## __QBusiness__
  - ### Features
    - Added APIs to manage QBusiness user subscriptions

# __2.30.9__ __2025-01-29__
## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Added ConflictException error type in DeleteBillScenario, BatchDeleteBillScenarioCommitmentModification, BatchDeleteBillScenarioUsageModification, BatchUpdateBillScenarioUsageModification, and BatchUpdateBillScenarioCommitmentModification API operations.

## __AWS SDK for Java v2__
  - ### Features
    - Buffer input data from ContentStreamProvider in cases where content length is known.

## __Amazon Elastic Container Registry__
  - ### Features
    - Add support for Dualstack and Dualstack-with-FIPS Endpoints

## __Amazon Elastic Container Registry Public__
  - ### Features
    - Add support for Dualstack Endpoints

## __Amazon S3__
  - ### Bugfixes
    - Fixed an issue that could cause checksum mismatch errors when performing parallel uploads with the async S3 client and the SHA1 or SHA256 checksum algorithms selected.

## __Amazon Simple Storage Service__
  - ### Features
    - Change the type of MpuObjectSize in CompleteMultipartUploadRequest from int to long.

## __Amazon Transcribe Streaming Service__
  - ### Features
    - This release adds support for AWS HealthScribe Streaming APIs within Amazon Transcribe.

## __MailManager__
  - ### Features
    - This release includes a new feature for Amazon SES Mail Manager which allows customers to specify known addresses and domains and make use of those in traffic policies and rules actions to distinguish between known and unknown entries.

# __2.30.8__ __2025-01-28__
## __AWS AppSync__
  - ### Features
    - Add stash and outErrors to EvaluateCode/EvaluateMappingTemplate response

## __AWS DataSync__
  - ### Features
    - AWS DataSync now supports the Kerberos authentication protocol for SMB locations.

## __AWS SDK for Java v2__
  - ### Features
    - Buffer input data from ContentStreamProvider to avoid the need to reread the stream after calculating its length.

## __AWSDeadlineCloud__
  - ### Features
    - feature: Deadline: Add support for limiting the concurrent usage of external resources, like floating licenses, using limits and the ability to constrain the maximum number of workers that work on a job

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release changes the CreateFleet CLI and SDK's such that if you do not specify a client token, a randomly generated token is used for the request to ensure idempotency.

## __Amazon Kinesis Firehose__
  - ### Features
    - For AppendOnly streams, Firehose will automatically scale to match your throughput.

## __Timestream InfluxDB__
  - ### Features
    - Adds 'allocatedStorage' parameter to UpdateDbInstance API that allows increasing the database instance storage size and 'dbStorageType' parameter to UpdateDbInstance API that allows changing the storage type of the database instance

# __2.30.7__ __2025-01-27__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds support for dynamic audio configuration and the ability to disable the deblocking filter for h265 encodes.

## __AWS IoT__
  - ### Features
    - Raised the documentParameters size limit to 30 KB for AWS IoT Device Management - Jobs.

## __AWS S3 Control__
  - ### Features
    - Minor fix to ARN validation for Lambda functions passed to S3 Batch Operations

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - Add support for the prompt caching feature for Bedrock Prompt Management

# __2.30.6__ __2025-01-24__
## __AWS CloudTrail__
  - ### Features
    - This release introduces the SearchSampleQueries API that allows users to search for CloudTrail Lake sample queries.

## __AWS SSO OIDC__
  - ### Features
    - Fixed typos in the descriptions.

## __AWS Transfer Family__
  - ### Features
    - Added CustomDirectories as a new directory option for storing inbound AS2 messages, MDN files and Status files.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds support for UpdateStrategies in EKS Managed Node Groups.

## __Amazon HealthLake__
  - ### Features
    - Added new authorization strategy value 'SMART_ON_FHIR' for CreateFHIRDatastore API to support Smart App 2.0

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Systems Manager doc-only update for January, 2025.

# __2.30.5__ __2025-01-23__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added "future" allocation type for future dated capacity reservation

## __Netty NIO HTTP Client__
  - ### Features
    - Adds ALPN H2 support for Netty client

# __2.30.4__ __2025-01-22__
## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Reuse connections that receive a 5xx service response.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive adds a new feature, ID3 segment tagging, in CMAF Ingest output groups. It allows customers to insert ID3 tags into every output segment, controlled by a newly added channel schedule action Id3SegmentTagging.

## __AWS Glue__
  - ### Features
    - Docs Update for timeout changes

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Adds multi-turn input support for an Agent node in an Amazon Bedrock Flow

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Rename WorkSpaces Web to WorkSpaces Secure Browser

## __Apache HTTP Client__
  - ### Bugfixes
    - Reuse connections that receive a 5xx service response.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Reuse connections that receive a 5xx service response.

# __2.30.3__ __2025-01-21__
## __AWS Batch__
  - ### Features
    - Documentation-only update: clarified the description of the shareDecaySeconds parameter of the FairsharePolicy data type, clarified the description of the priority parameter of the JobQueueDetail data type.

## __AWS IoT SiteWise__
  - ### Features
    - AWS IoT SiteWise now supports ingestion and querying of Null (all data types) and NaN (double type) values of bad or uncertain data quality. New partial error handling prevents data loss during ingestion. Enabled by default for new customers; existing customers can opt-in.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Logs__
  - ### Features
    - Documentation-only update to address doc errors

## __Amazon Cognito Identity Provider__
  - ### Features
    - corrects the dual-stack endpoint configuration for cognitoidp

## __Amazon Connect Service__
  - ### Features
    - Added DeleteContactFlowVersion API and the CAMPAIGN flow type

## __Amazon QuickSight__
  - ### Features
    - Added `DigitGroupingStyle` in ThousandsSeparator to allow grouping by `LAKH`( Indian Grouping system ) currency. Support LAKH and `CRORE` currency types in Column Formatting.

## __Amazon Simple Notification Service__
  - ### Features
    - This release adds support for the topic attribute FifoThroughputScope for SNS FIFO topics. For details, see the documentation history in the Amazon Simple Notification Service Developer Guide.

## __EMR Serverless__
  - ### Features
    - Increasing entryPoint in SparkSubmit to accept longer script paths. New limit is 4kb.

## __Emf Metric Logging Publisher__
  - ### Features
    - Added a new EmfMetricLoggingPublisher class that transforms SdkMetricCollection to emf format string and logs it, which will be automatically collected by cloudwatch.

# __2.30.2__ __2025-01-17__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS User Notifications__
  - ### Features
    - Added support for Managed Notifications, integration with AWS Organization and added aggregation summaries for Aggregate Notifications

## __Amazon Bedrock Runtime__
  - ### Features
    - Allow hyphens in tool name for Converse and ConverseStream APIs

## __Amazon Detective__
  - ### Features
    - Doc only update for Detective documentation.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Release u7i-6tb.112xlarge, u7i-8tb.112xlarge, u7inh-32tb.480xlarge, p5e.48xlarge, p5en.48xlarge, f2.12xlarge, f2.48xlarge, trn2.48xlarge instance types.

## __Amazon SageMaker Service__
  - ### Features
    - Correction of docs for "Added support for ml.trn1.32xlarge instance type in Reserved Capacity Offering"

## __Amazon Simple Storage Service__
  - ### Bugfixes
    - Fixed contentLength mismatch issue thrown from putObject when multipartEnabled is true and a contentLength is provided in PutObjectRequest. See [#5807](https://github.com/aws/aws-sdk-java-v2/issues/5807)

# __2.30.1__ __2025-01-16__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon EC2 Container Service__
  - ### Features
    - The release addresses Amazon ECS documentation tickets.

## __Amazon SageMaker Service__
  - ### Features
    - Added support for ml.trn1.32xlarge instance type in Reserved Capacity Offering

# __2.30.0__ __2025-01-15__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SDK for Java v2 Migration Tool__
  - ### Bugfixes
    - Transform the getter methods on the service model classes that return SdkBytes to return ByteBuffer to be compatible with v1 style getters

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Now supports streaming for inline agents.

## __Amazon API Gateway__
  - ### Features
    - Documentation updates for Amazon API Gateway

## __Amazon Cognito Identity__
  - ### Features
    - corrects the dual-stack endpoint configuration

## __Amazon Simple Email Service__
  - ### Features
    - This release introduces a new recommendation in Virtual Deliverability Manager Advisor, which detects elevated complaint rates for customer sending identities.

## __Amazon Simple Storage Service__
  - ### Features
    - S3 client behavior is updated to always calculate a checksum by default for operations that support it (such as PutObject or UploadPart), or require it (such as DeleteObjects). The checksum algorithm used by default is CRC32. The S3 client attempts to validate response checksums for all S3 API operations that support checksums. However, if the SDK has not implemented the specified checksum algorithm then this validation is skipped. See [Dev Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/s3-checksums.html) for more information
    - This change enhances integrity protections for new SDK requests to S3. S3 SDKs now support the CRC64NVME checksum algorithm, full object checksums for multipart S3 objects, and new default integrity protections for S3 requests.

## __Amazon WorkSpaces__
  - ### Features
    - Added GeneralPurpose.4xlarge & GeneralPurpose.8xlarge ComputeTypes.

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Mark type in MaintenanceWindow as required.

## __Partner Central Selling API__
  - ### Features
    - Add Tagging support for ResourceSnapshotJob resources

## __S3 Event Notification__
  - ### Bugfixes
    - add static modifier to fromJson(InputStream) method of S3EventNotification

## __Security Incident Response__
  - ### Features
    - Increase minimum length of Threat Actor IP 'userAgent' to 1.


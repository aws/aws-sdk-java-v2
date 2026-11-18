 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.59.4__ __2026-11-18__
## __AWS Abuse Signal API__
  - ### Features
    - This release introduces the AWS Abuse Signal API, enabling customers to programmatically submit and track AWS abuse reports.

## __AWS Clean Rooms Service__
  - ### Features
    - Adds support for populating intermediate tables with results from PySpark analysis templates.

## __AWS CloudFormation__
  - ### Features
    - AWS CloudFormation StackSets now provides stage-by-stage progress for deployments that span thousands of accounts and AWS Regions, which enables you to see where each deployment stands while it runs

## __AWS Health APIs and Notifications__
  - ### Features
    - Adds resource tags functionality to the AWS Health service

## __AWS Identity and Access Management__
  - ### Features
    - Adds MaxSignInSessionDuration to IAM Roles and Login Profiles, allowing customers to configure the maximum Console sign-in session duration per principal. Affects CreateRole, UpdateRole, GetRole and LoginProfile APIs.

## __AWS Resilience Hub V2__
  - ### Features
    - This release includes support for adding new field for the effective policy used in test runs and documentation updates.

## __AWS SDK for Java v2__
  - ### Features
    - Split the `afterTransmission` and `modifyHttpResponse` interceptor hooks into separate steps. If `modifyHttpResponse` throws, `modifyException` and `onExecutionFailure` now receive the HTTP response.
    - Updated endpoint and partition metadata.

## __AWS Security Agent__
  - ### Features
    - This release adds support for resuming a paused penetration test job from its last saved checkpoint.

## __AWS Sign-In Service__
  - ### Features
    - These APIs enable customers and administrators to list, view, and revoke AWS Sign-In Sessions programmatically using the AWS CLI and SDKs, providing greater visibility and control over authenticated access.

## __AWS Support__
  - ### Features
    - Updated endpoint resolution for AWS Support, including FIPS and dual-stack endpoints.

## __AWS WAFV2__
  - ### Features
    - Adds the AnalyticsConfig parameter to CreateWebACL and UpdateWebACL to enable or disable cross-Region security analytics for a WebACL. When you enable security analytics, you authorize AWS to transfer and store full request logs outside the AWS Region where the WebACL is hosted.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds s3BucketMount to mount an Amazon S3 bucket as a file system in AgentCore Runtime, with an optional per-session key prefix and adds operatingSystemConfiguration with a Linux privilegeMode (STANDARD or PRIVILEGED) to the capacity provider configuration

## __Amazon Cognito Identity Provider__
  - ### Features
    - Add support for restoring user pool's user data to point in time.

## __Amazon Neptune__
  - ### Features
    - Neptune Database Serverless support for graviton3 based r8g instances.

## __Amazon Simple Email Service__
  - ### Features
    - This release adds support for Spillover Routing in Amazon SES.

## __Amazon Simple Storage Service__
  - ### Features
    - Adds exclusions and Multi-Region key resolution to S3 Replication. Rule filters support a Not element to exclude prefixes and object tags. Destination EncryptionConfiguration supports UseSourceMultiRegionKey to encrypt replicas with the related replica KMS multi-Region key in the destination Region.

## __Braket__
  - ### Features
    - Supporting a new PAUSED device status and a DeviceStatusReason.

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for the keyExchange field in CloudTrail tlsDetails, enabling customers to distinguish classical from post-quantum hybrid key exchange for TLS connections on Application Load Balancers.

# __2.59.3__ __2026-11-17__
## __AWS Billing__
  - ### Features
    - Adds the SplitCredit and TransferCredit APIs to split or transfer credits, and adds benefit level sharing for Reserved Instances and Savings Plans to GetBillingPreferences and UpdateBillingPreferences.

## __AWS Config__
  - ### Features
    - Adds support for AWS Config managed conformance packs, with new Source and Scope parameters on PutConformancePack, a new Type field and type Filters on DescribeConformancePacks and DescribeConformancePackStatus, and an optional StackArn in DescribeConformancePackStatus.

## __AWS Control Tower__
  - ### Features
    - AWS Control Tower now reports source drift when an enabled baseline no longer matches the landing zone's configuration.

## __AWS DevOps Agent Service__
  - ### Features
    - AWS DevOps Agent adds Code Change Risk Analysis capability and task types, and GetChat and UpdateChat operations to rename, archive, and unarchive chats and filter ListChats by archive status.

## __AWS Elemental Inference__
  - ### Features
    - AWS Elemental Inference now supports functions, for running your own analysis on feed media, and question sets, for custom decisions on contextual metadata shots and scenes.

## __AWS Lambda__
  - ### Features
    - AWS Lambda durable functions now supports pausing, resuming, and deleting durable executions with the new PauseDurableExecution, ResumeDurableExecution, and DeleteDurableExecution APIs.

## __AWS Marketplace Agreement Service__
  - ### Features
    - Adds buyer agreement request and approval APIs. Buyers can get, list and cancel agreement requests and start approvals, which organization administrators approve or reject. Buyers can share a contact when they create an agreement request, and sellers can retrieve it with GetAgreementContacts.

## __AWS Network Manager__
  - ### Features
    - Adds support for Multipath Resilience change values to be delivered when reviewing a core network change set.

## __AWS S3 Control__
  - ### Features
    - Adds support for S3 Intelligent Tiering Access Tier and publishing to AWS Billing and Cost Management in S3 Storage Lens

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Security Hub now supports Google Cloud. Use connector APIs to connect GCP organizations or projects and monitor their security posture. It also infers resource criticality from tags, CloudWatch metrics, and SBOM data to help you prioritize findings

## __Amazon API Gateway__
  - ### Features
    - API Gateway now adds support for JWT authorizers on REST APIs.

## __Amazon AppStream__
  - ### Features
    - Adds support for In-Session SmartCard Redirection for Windows Multi-Session fleets

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adds support for continuous insights and automatic dataset generation

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds support for agent insight stores that help frame insight evaluators, and dataset source support for batch evaluation

## __Amazon Connect Cases__
  - ### Features
    - PutCaseEventConfiguration now supports an optional autoIncludeFields attribute on case event included data. Set it to All, System, or Custom to automatically include a category of case fields in case event data, combined as a union with any fields you list explicitly.

## __Amazon Connect Contact Lens__
  - ### Features
    - Amazon Connect Customer now allows conversational analytics for voice contacts to be analyzed in more than one language. The languages detected will be present in the output.

## __Amazon Connect Service__
  - ### Features
    - Evaluation forms can now adapt to each contact. Questions and sections can be shown or hidden, and single-select answers can be set automatically, based on answers to other questions, contact data (Contact Lens categories, sentiment, agent, queue, contact attributes and more).

## __Amazon ElastiCache__
  - ### Features
    - Enables creation of a serverless Valkey cache that supports write-through caching for DynamoDB table data.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds multipath resilience for inter-Region VPC peering connections and Transit Gateway peering attachments. Adds support for quantum-resistant key exchanges to Site-to-Site VPN. Allows querying multiple instance types in a single DescribeCapacityBlockOfferings request.

## __Amazon Elastic Container Registry__
  - ### Features
    - Adds a new image scan status for images that exceed the maximum size supported by Amazon ECR enhanced scanning.

## __Amazon Kinesis__
  - ### Features
    - Adds support for queues on on-demand streams with new CreateQueue, UpdateQueue, DeleteQueue, DescribeQueue, ListQueues, ReceiveQueueRecords, AcknowledgeQueueRecords, and UpdateQueueRecordsVisibility APIs, plus four dead-letter APIs, for Amazon Kinesis Data Streams.

## __Amazon Route 53 Global Resolver__
  - ### Features
    - Adds AWS Cloud WAN support to Route 53 Global Resolver via new core network endpoint APIs to connect a global resolver to a core network, segment-based access sources, and a publicIpStatus setting to disable public IPs so a resolver is reachable only over Cloud WAN.

## __Amazon Simple Storage Service__
  - ### Features
    - For the S3 operations AbortMultipartUpload, CreateMultipartUpload, ListParts, UploadPart, UploadPartCopy, the Object Version ID is returned as a response header, if the bucket has versioning enabled. For ListMultipartUploads, it will be returned in the response body.

## __Amazon WorkSpaces__
  - ### Features
    - Added the NestedVirtualization and EnableEndUserAdvisor self-service permissions. Use them with ModifySelfservicePermissions to turn nested virtualization and End User Advisor on or off for a directory, and view the settings with DescribeWorkspaceDirectories. End User Advisor is on by default.

## __Application Migration Service__
  - ### Features
    - AWS Transform adds Express Migration for MAP, remote source actions, per-server default launch settings, F5 BIG-IP to Elastic Load Balancing migration, AWS Outposts support, greenfield network design, and replication of volumes up to 64 TB.

## __CloudWatch Observability Admin Service__
  - ### Features
    - This release adds Microsoft Azure connectors, third-party telemetry enrichment and StartTelemetryPipelineRefresh, to discover Azure resources, enrich their telemetry and refresh external-cloud pipelines.

## __CloudWatch RUM__
  - ### Features
    - Add SessionMetadata to PutRumEvents for attributes that apply to every event in a batch, such as browser, device, and operating system.

## __Inspector2__
  - ### Features
    - This release extends vulnerability management to Google Cloud Platform (GCP) Compute Engine virtual machine (VM) instances, container images in Google Artifact Registry (GAR), and Cloud Run functions.

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fixed how `S3TransferManager#resumeDownloadFile` handles an object that changes during a paused download when using the CRT-based S3 client.

# __2.59.2__ __2026-11-16__
## __AWS Health APIs and Notifications__
  - ### Features
    - Adds resource tags functionality to the AWS Health service

## __AWS IoT Wireless__
  - ### Features
    - Updated CreateDeviceProfile to accept MaxEirp values up to 36 dBm in LoRaWANDeviceProfile. Previously the maximum value was 15.

## __AWS Key Management Service__
  - ### Features
    - AWS KMS now supports sharing KMS keys across AWS accounts using AWS Resource Access Manager (AWS RAM). This release adds the optional ResourceOwners parameter to ListKeys, which lists keys owned by the caller's account (SELF), keys shared with the account through AWS RAM, or both.

## __AWS Lambda__
  - ### Features
    - Adds support for GPU instances on Lambda Managed Instances in the CreateFunction and UpdateFunctionConfiguration APIs. Lambda Event Functions on Lambda Managed Instances can now set the accelerator memory using the new AcceleratorConfig parameter and run on capacity providers with GPU instances.

## __AWS Lambda Core__
  - ### Features
    - Lambda network connectors now support service-managed Elastic IP addresses. This enables traffic routed through a connector to reach the public internet without a NAT gateway.

## __AWS Network Firewall__
  - ### Features
    - Adds support for context-aware stateful rule groups, letting rules reference AWS resource IDs through ContextSets sourced from a Network Manager context store.

## __AWS Network Manager__
  - ### Features
    - Context Sharing with Firewalls

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Add path validation to EC2MetadataUtils.

## __AWS Well-Architected Tool__
  - ### Features
    - This release adds AWS Organizations integration, on-demand reports, recommendation and profile sharing, and automated basic profiles to the Well-Architected Agent.

## __Amazon Athena__
  - ### Features
    - Adds an optional AttributesToGet parameter to ListDatabases and ListTableMetadata to return only requested database or table names (and table types) from the AWS Glue Data Catalog, reducing response time for large catalogs.

## __Amazon Aurora DSQL__
  - ### Features
    - Added support for Amazon Aurora DSQL advanced monitoring metric groups, which publish detailed database metrics and logs to Amazon CloudWatch Database Insights. Adds the metricGroups parameter to CreateCluster, UpdateCluster, and GetCluster.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Implement OAuth 2.0 Token Exchange (RFC 8693) for Cognito User Pools, enabling services to exchange a user's existing token for a new token that is appropriately scoped and audience-bound for a specific downstream service.

## __Amazon Connect Scheduling__
  - ### Features
    - This release adds APIs for Amazon Connect Scheduling. Use them to create, get, approve, reject, and cancel agent time off requests, and to set agent time off balances.

## __Amazon Connect Service__
  - ### Features
    - Adds the DeleteAllEntitySessions API, which signs a user out of all of their active sessions in an Amazon Connect instance.

## __Amazon DynamoDB Streams__
  - ### Features
    - Generate account-based endpoints for DynamoDB Streams requests when the account ID is available

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS gateway connectors now support a configurable deregistration delay (0-3600 seconds, default 300 seconds).

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds a new Transit Gateway Attachment type called Network Function. Launches associated KMS Keys for EBS Snapshots - associated KMS keys enable customers to share and copy snapshots encrypted with AWS Managed Keys across AWS accounts.

## __Amazon Elastic Container Registry__
  - ### Features
    - ECR feature support for replicating image deletion and untagging.

## __Amazon OpenSearch Memory__
  - ### Features
    - Initial release of Amazon OpenSearch Memory, which gives AI agents persistent memory. Store conversation and document events in a Space, have memories extracted from them automatically, and retrieve them with semantic, lexical, or hybrid search.

## __Amazon S3 Vectors__
  - ### Features
    - Amazon S3 Vectors now supports hybrid search, providing metadata text search along with vector search.

## __Amazon SageMaker Inference Router Runtime__
  - ### Features
    - Amazon SageMaker adds cross-region inference routing with new runtime APIs that intelligently route inference requests across endpoints in multiple AWS regions.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker adds Synthetic Data Generation, Auto-labeling, Data Quality Evaluation, and Data Transformation capabilities, Cross-Region Inference Routing, KV Aware routing for realtime inference endpoints.

## __Amazon Simple Queue Service__
  - ### Bugfixes
    - Limit the number of receive buffers the SQS batch manager keeps and release them when the batch manager is closed. A `receiveMessage` call on a queue that does not exist now fails with `QueueDoesNotExistException`, including when `waitTimeSeconds` is set. After the batch manager is closed, every `receiveMessage` call fails, including calls that are not batched.

## __Amazon WorkSpaces__
  - ### Features
    - Adds the WorkspaceOsUserMode parameter to RegisterWorkspaceDirectory and DescribeWorkspaceDirectories, so customers can register an IAM Identity Center directory whose WorkSpaces use local operating system users instead of joining a domain.

## __Connect Health__
  - ### Features
    - Adds dataset APIs and async medical coding jobs that can use a customer dataset (preview). Ambient listening sessions add medical coding job details and real-time periodontal chart and checklist events (preview). SubmitFeedback compares human-approved and AI-generated artifacts (preview).

## __EMR Serverless__
  - ### Features
    - Amazon EMR Serverless now supports Flex, a new execution option for running data processing jobs on spare compute capacity at a discount.

## __IAM Toolbox (Preview)__
  - ### Features
    - AWS IAM adds the GetRequestAuthorizationPolicy operation, per-check authorization results (checks) and extra policy metadata for resource-based policies (policySource) in GetRequestAuthorizationDetails for Access Troubleshooter.

## __Network Flow Monitor__
  - ### Features
    - This release adds Performance Monitors to Amazon CloudWatch Network Monitoring. Without installing an agent, they track traffic in the VPCs you select and report an impact score (None, Low, Medium, or High) showing whether the AWS network is degrading your workloads.

## __S3 Transfer Manager__
  - ### Performance Improvements
    - Improve transfer throughput when `maxInFlightParts` is high by removing unnecessary CPU contention in transfer progress updates.

# __2.59.1__ __2026-11-13__
## __AWS CloudFormation__
  - ### Features
    - AWS CloudFormation introduces stack aliases, which let you refer to an existing stack by a new name without deleting and recreating it. Manage aliases with SetAlias and RemoveAlias, and use an alias in place of the stack name in operations such as UpdateStack.

## __AWS DevOps Agent Service__
  - ### Features
    - AWS DevOps Agent adds bidirectional communication for Microsoft Teams associations. Set msteams.bidirectional with an IAM role so the agent answers mentions in your Teams channels.

## __AWS Glue__
  - ### Features
    - Field description update for auto upgrade summaries

## __AWS Identity and Access Management__
  - ### Features
    - Minor documentation updates only

## __AWS Lambda__
  - ### Features
    - Adds support for customer-managed runtimes in the CreateFunction and UpdateFunctionConfiguration APIs. Functions can now use a customer-managed runtime and receive its updates automatically, or pin to a specific runtime version using the existing runtime controls.

## __AWS Lambda Core__
  - ### Features
    - Initial release of the customer-managed runtime APIs. Runtime publishers can now create, version, and manage custom runtimes, and use aliases to roll out new runtime versions to the functions that use them.

## __AWS Network Firewall__
  - ### Features
    - Releasing Firewall Proxy API and deprecating Egress Proxy API.

## __AWS Organizations__
  - ### Features
    - Add support for policy operations on the SECURITYHUB CSPM POLICY policy type.

## __AWS SecurityHub__
  - ### Features
    - New DISABLED status for Security Hub CSPM Central Configuration.

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud now supports the NVIDIA RTX PRO 4500 GPU accelerator (rtx-pro-4500) for service-managed fleets running Amazon EC2 G7 instances.

## __Amazon AppStream__
  - ### Features
    - Amazon WorkSpaces Agent Access now supports image builders, so AI agents can install, configure, and test applications before you create an image. New Replay Session delivers a video of screenshots, a transcript, and a report for each agent session, optionally encrypted with your own KMS key.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Adding support for dynamic client registration.

## __Amazon DocumentDB with MongoDB compatibility__
  - ### Features
    - Update the documentation for the --engine-version flag for cluster creation

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds support for Networking Flexible Cost Allocation APIs, enabling Network Admin, Architect, and Engineers to break down shared networking costs by the resources that generated the traffic.

## __Amazon EventBridge Scheduler__
  - ### Features
    - Adds EventBridgePutEventsV2Parameters and EventBridgePutRawEventsParameters to Target so schedules can publish to Amazon EventBridge event bus v2 with PutEvents or PutRawEvents.

## __Amazon Q Connect__
  - ### Features
    - Amazon Q in Connect now supports additional message template attributes. You can personalize message templates using case fields, Amazon Connect Customer Profiles calculated attributes, and product recommendations.

## __Amazon SageMaker Service__
  - ### Features
    - New feature for Sagemaker Hyperpod Slurm clusters to install Extensions and view cluster operations.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Adding Google Cloud Platform (GCP) support to SSM Cloud Connector to enable onboarding of GCP Compute Engine instances to AWS Systems Manager. Customers can now optionally create an SSM Cloud Connector without an AWS Config connector.

## __Amazon VPC Lattice__
  - ### Features
    - Amazon VPC Lattice now supports modification of resource configuration DNS resolution on Resource Gateways

## __AmazonConnectCampaignServiceV2__
  - ### Features
    - Adds LastModifiedTime to DescribeCampaign and ListCampaigns responses.

## __EC2 Image Builder__
  - ### Features
    - Add documentation clarification around List API behavior, and add image available waiter

## __IAM Roles Anywhere__
  - ### Features
    - This release adds resource aliases for IAM Roles Anywhere trust anchors, profiles, and certificate revocation lists (CRLs). Assign an optional, unique alias name to a resource and use its alias ARN wherever an identifier is accepted, including CreateSession.

# __2.59.0__ __2026-11-12__
## __AWS Backup__
  - ### Features
    - Adding new notification types for SNS notifications

## __AWS CRT-based S3 Client__
  - ### Features
    - Add support for direct buffer pool downloads to the AWS CRT-based S3 client. Configure a pool with `S3CrtAsyncClientBuilder#directBufferPoolConfiguration`, then use `S3CrtResponseTransformerProvider.toBlockingInputStreamFromDirectBufferPool()` to read a `GetObject` response as an InputStream backed by the CRT direct buffer pool, without the extra copy into Java heap buffers.

## __AWS Clean Rooms Service__
  - ### Features
    - Adds a retry signal to protected query and protected job errors that indicates whether running the same request again might succeed, and defines error codes that identify the category of the failure.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now adds a caption language setting for MediaPackage V2 output groups that signals a stream carries no closed captions.

## __AWS MediaConnect__
  - ### Features
    - MediaConnect adds four features. (1) RTMP Push on Router Input and Flows (RTMP, RTMPS, eRTMP). (2) PID Remapping on Router rewrites PIDs inline without transcoding. (3) Key Frame Switching transitions router inputs on video boundaries. (4) SRT Merge Input bonds redundant SRT streams.

## __AWS MediaTailor__
  - ### Features
    - Add post origin manifest hook, add HLS tag processor

## __AWS Secrets Manager__
  - ### Features
    - Adds support for cross-account replication with the new AssociateCrossAccountReplica, DisassociateCrossAccountReplica, and ListResourcePolicies APIs, CrossAccountPrimaryARN in CreateSecret, cross-account replication status in DescribeSecret, and PolicyName in the resource policy APIs.

## __AWS Security Agent__
  - ### Features
    - Adds support for cleanup strategy in CreatePentestInput and UpdatePentestInput.

## __AWS Single Sign-On Admin__
  - ### Features
    - Adds support for Network Controls on IAM Identity Center instances. You can now require that access to your instance and AWS managed applications originate from VPC endpoints in your organization, and optionally narrow that perimeter to specific VPCs, IP ranges, or application exceptions.

## __Amazon CloudWatch__
  - ### Features
    - Adds support for saving, retrieving, listing, and deleting CloudWatch metric data query definitions.

## __Amazon Connect Service__
  - ### Features
    - This release adds a LastModifiedTime and LastModifiedRegion field to ListIntegrationAssociations so customers can see at what time and in which Region an IntegrationAssociation was last modified in.

## __Amazon EMR Containers__
  - ### Features
    - This release updates range of maxInQueueDepth and maxConcurrency limit from 10000 to 1000.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for multiple features, including EC2 Image Builder image recipes and container recipes in AMI usage reports, Amazon EC2 Detailed Monitoring adds opt-in metrics, and Global Accelerator and API Gateway V2 as resource exclusions for VPC Encryption Control.

## __Amazon GameLift Streams__
  - ### Features
    - Adds support for ApplicationStartedAt on stream sessions, a timestamp that indicates when the application launched for the stream session, returned by GetStreamSession and StartStreamSession. Also added streamingStartedAt and streamingEndedAt.

## __Amazon GuardDuty__
  - ### Features
    - Adds the ListInvestigationRecords API, which returns the step-by-step records of a completed investigation. Adds investigation scope details and an optional structured summary to GetInvestigation. Adds filter criteria and investigation type to ListInvestigations.

## __Amazon Lightsail__
  - ### Features
    - Amazon Lightsail now offers web addresses, so you can quickly make a website or application reachable at a name like mysite.amazonlightsail.com. A web address is free while attached to a Lightsail CDN, with no domain to register and no SSL TLS certificate to create or renew.

## __Amazon Route 53__
  - ### Features
    - Amazon Route 53 now supports Record Set Protection, which blocks changes to protected records in a hosted zone.

## __Amazon SageMaker Service__
  - ### Features
    - Adds CreatedBy and LastModifiedBy to the DescribeHub and DescribeHubContent responses and to HubContent records returned by the Search API, identifying the IAM identity or SageMaker Studio user profile that created or last modified a hub or hub content.

## __AmazonMWAA__
  - ### Features
    - Amazon MWAA now supports Airflow environments with IAM Identity Center (IDC) integration and exposes OSS Airflow Multi-Team feature. Users can configure individual IDC-backed identities and fine-grained authorization, and define Dags on per-team basis.

## __AmazonMWAAServerless__
  - ### Features
    - Adds support for Airflow UI in MWAA Serverless

## __Auto Scaling__
  - ### Features
    - Amazon EC2 Auto Scaling now supports ASGAverageCPUUtilizationHighResolution as a predefined metric type for target tracking scaling policies.

## __CloudWatch Omni__
  - ### Features
    - Add aggregationTemporality and monotonicity to GetContextGraph MetricMetadata

## __S3 Transfer Manager__
  - ### Features
    - Add support for direct buffer pool downloads to S3 Transfer Manager with the CRT-based S3 client.

  - ### Bugfixes
    - Escape control characters in the S3 Transfer Manager log messages that include object keys, file paths or requests, for directory transfers and for paused and resumed transfers.

## __odb__
  - ### Features
    - ODB now supports scaling for Exadata Dedicated offerings. Customers can scale database and storage servers on Exadata infrastructure, resize VM clusters and Autonomous VM clusters, and add or remove virtual machines in a VM cluster.


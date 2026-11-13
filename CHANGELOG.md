 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


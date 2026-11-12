 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


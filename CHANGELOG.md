 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.34.8__ __2025-10-01__
## __AWS Clean Rooms ML__
  - ### Features
    - This release introduces data access budgets to view how many times an input channel can be used for ML jobs in a collaboration.

## __AWS Clean Rooms Service__
  - ### Features
    - This release introduces data access budgets to control how many times a table can be used for queries and jobs in a collaboration.

## __AWS Database Migration Service__
  - ### Features
    - This is a doc-only update, revising text for kms-key-arns.

## __AWS Parallel Computing Service__
  - ### Features
    - Added the UpdateCluster API action to modify cluster configurations, and Slurm custom settings for queues.

## __AWS SDK for Java v2__
  - ### Features
    - This update enables reusing the initially computed payload checksum of a request across all request attempts. This ensures that even if the content is changed from one attempt to the next, the checksum included in the request will remain the same and the request will be rejected by the service.

## __Amazon Chime SDK Meetings__
  - ### Features
    - Add support to receive dual stack MediaPlacement URLs in Chime Meetings SDK

## __Amazon EC2 Container Service__
  - ### Features
    - This is a documentation only Amazon ECS release that adds additional information for health checks.

## __Amazon Interactive Video Service RealTime__
  - ### Features
    - Remove incorrect ReadOnly trait on IVS RealTime ImportPublicKey API

# __2.34.7__ __2025-09-30__
## __AWS DataSync__
  - ### Features
    - Added support for FIPS VPC endpoints in FIPS-enabled AWS Regions.

## __AWS Directory Service__
  - ### Features
    - AWS Directory service now supports IPv6-native and dual-stack configurations for AWS Managed Microsoft AD, AD Connector, and Simple AD (dual-stack only). Additionally, AWS Managed Microsoft AD Standard Edition directories can be upgraded to Enterprise Edition directories through a single API call.

## __AWS MediaTailor__
  - ### Features
    - Adding TPS Traffic Shaping to Prefetch Schedules

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Transfer Family__
  - ### Features
    - Add support for updating server identity provider type

## __Amazon Bedrock Agent Core Control Plane Fronting Layer__
  - ### Features
    - Tagging support for AgentCore Gateway

## __Amazon Chime SDK Voice__
  - ### Features
    - Added support for IPv4-only and dual-stack network configurations for VoiceConnector and CreateVoiceConnector API.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - Amazon CloudWatch Application Signals is introducing the Application Map to give users a more comprehensive view of their service health. Users will now be able to group services, track their latest deployments, and view automated audit findings concerning service performance.

## __Amazon Connect Cases__
  - ### Features
    - This release adds support for two new related item types: ConnectCase for linking Amazon Connect cases and Custom for user-defined related items with configurable fields.

## __Amazon Connect Customer Profiles__
  - ### Features
    - This release introduces ListProfileHistoryRecords and GetProfileHistoryRecord APIs for comprehensive profile history tracking with complete audit trails of creation, updates, merges, deletions, and data ingestion events.

## __Amazon DataZone__
  - ### Features
    - This release adds support for creation of EMR on EKS Connections in Amazon DataZone.

## __Amazon EC2 Container Service__
  - ### Features
    - This release adds support for Managed Instances on Amazon ECS.

## __Amazon FSx__
  - ### Features
    - Add Dual-Stack support for Amazon FSx for NetApp ONTAP and Windows File Server

## __Amazon QuickSight__
  - ### Features
    - added warnings to a few CLI pages

## __Amazon Relational Database Service__
  - ### Features
    - Enhanced RDS error handling: Added DBProxyEndpointNotFoundFault, DBShardGroupNotFoundFault, KMSKeyNotAccessibleFault for snapshots/restores/backups, NetworkTypeNotSupported, StorageTypeNotSupportedFault for restores, and granular state validation faults. Changed DBInstanceNotReadyFault to HTTP 400.

# __2.34.6__ __2025-09-29__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SDK for Java v2 Bundle__
  - ### Bugfixes
    - Remove unused `META-INF/jandex.idx`. This file was shaded into the `bundle` JAR from a transitive dependency of the bundle, which also does not use this file.

## __Amazon Bedrock__
  - ### Features
    - Release for fixing GetFoundationModel API behavior. Imported and custom models have their own exclusive API and GetFM should not accept those ARNS as input

## __Amazon Bedrock Runtime__
  - ### Features
    - New stop reason for Converse and ConverseStream

## __Amazon VPC Lattice__
  - ### Features
    - Adds support for specifying the number of IPv4 addresses in each ENI for the resource gateway for VPC Lattice.

## __EC2 Image Builder__
  - ### Features
    - This release introduces several new features and improvements to enhance pipeline management, logging, and resource configuration.

# __2.34.5__ __2025-09-26__
## __AWS Billing__
  - ### Features
    - Add ability to combine custom billing views to create new consolidated views.

## __AWS Cost Explorer Service__
  - ### Features
    - Support for payer account dimension and billing view health status.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This release enhances the information provided through Flow Traces. New information includes source/next node tracking, execution chains for complex nodes, dependency action (operation) details, and dependency traces.

## __Amazon Connect Service__
  - ### Features
    - Adds supports for manual contact picking (WorkList) operations on Routing Profiles, Agent Management and SearchContacts APIs.

## __Amazon DynamoDB Enhanced Client__
  - ### Bugfixes
    - Optimizations for DDB EnhancedDocument.toJson()

## __Amazon DynamoDB Streams__
  - ### Features
    - Added support for IPv6 compatible endpoints for DynamoDB Streams.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release includes documentation updates for Amazon EBS General Purpose SSD (gp3) volumes with larger size and higher IOPS and throughput.

## __Amazon Redshift__
  - ### Features
    - Support tagging and tag propagation to IAM Identity Center for Redshift Idc Applications

## __Amazon Simple Queue Service__
  - ### Bugfixes
    - Fix SqsAsyncBatchManager excessive batch flushing under heavy load. Fixes [#6374](https://github.com/aws/aws-sdk-java-v2/issues/6374).
        - Contributed by: [@thornhillcody](https://github.com/thornhillcody)

## __Data Automation for Amazon Bedrock__
  - ### Features
    - Added support for configurable Speaker Labeling and Channel Labeling features for Audio modality.

## __Contributors__
Special thanks to the following contributors to this release: 

[@thornhillcody](https://github.com/thornhillcody)
# __2.34.4__ __2025-09-25__
## __AWS Glue__
  - ### Features
    - Update GetConnection(s) API to return KmsKeyArn & Add 63 missing connection types

## __AWS Network Firewall__
  - ### Features
    - Network Firewall now introduces Reject and Alert action support for stateful domain list rule groups, providing customers with more granular control over their network traffic.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Lightsail__
  - ### Features
    - Attribute HTTP binding update for Get/Delete operations

# __2.34.3__ __2025-09-24__
## __AWS Key Management Service__
  - ### Features
    - Documentation only updates for KMS.

## __AWS SDK for Java v2 Migration Tool__
  - ### Bugfixes
    - Fix OpenRewrite recipe MethodMatcher
        - Contributed by: [@MBoegers](https://github.com/MBoegers)

## __Amazon AppStream__
  - ### Features
    - G6f instance support for AppStream 2.0

## __Amazon CloudWatch__
  - ### Features
    - Fix default dualstack FIPS endpoints in AWS GovCloud(US) regions

## __Amazon DynamoDB Accelerator (DAX)__
  - ### Features
    - This release adds support for IPv6-only, DUAL_STACK DAX instances

## __Amazon Neptune__
  - ### Features
    - Doc-only update to address customer use.

## __Contributors__
Special thanks to the following contributors to this release: 

[@MBoegers](https://github.com/MBoegers)
# __2.34.2__ __2025-09-23__
## __AWS Clean Rooms Service__
  - ### Features
    - Added support for running incremental ID mapping for rule-based workflows.

## __AWS EntityResolution__
  - ### Features
    - Support incremental id mapping workflow for AWS Entity Resolution

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SSO OIDC__
  - ### Features
    - This release includes exception definition and documentation updates.

## __AWS Single Sign-On Admin__
  - ### Features
    - Add support for encryption at rest with Customer Managed KMS Key in AWS IAM Identity Center

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add Amazon EC2 R8gn instance types

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Added Dualstack support to GetDeployablePatchSnapshotForInstance

# __2.34.1__ __2025-09-22__
## __AWS Batch__
  - ### Features
    - Starting in JAN 2026, AWS Batch will change the default AMI for new Amazon ECS compute environments from Amazon Linux 2 to Amazon Linux 2023. We recommend migrating AWS Batch Amazon ECS compute environments to Amazon Linux 2023 to maintain optimal performance and security.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds support for RepairConfig overrides and configurations in EKS Managed Node Groups.

## __EC2 Image Builder__
  - ### Features
    - Version ARNs are no longer required for the EC2 Image Builder list-image-build-version, list-component-build-version, and list-workflow-build-version APIs. Calling these APIs without the ARN returns all build versions for the given resource type in the requesting account.

# __2.34.0__ __2025-09-19__
## __AWS Config__
  - ### Features
    - Add UNKNOWN state to RemediationExecutionState and add IN_PROGRESS/EXITED/UNKNOWN states to RemediationExecutionStepState.

## __AWS Elemental MediaLive__
  - ### Features
    - Add MinBitrate for QVBR mode under H264/H265/AV1 output codec. Add GopBReference, GopNumBFrames, SubGopLength fields under H265 output codec.

## __AWS License Manager User Subscriptions__
  - ### Features
    - Added support for cross-account Active Directories.

## __AWS SDK for Java v2__
  - ### Features
    - Add a utils-lite package that wraps threadlocal meant for internal shared usage across multiple applications across multiple applications
    - Updated endpoint and partition metadata.

## __AWS SDK for Java v2 Migration Tool__
  - ### Features
    - General availability release of the AWS SDK for Java v2 migration tool that automatically migrates applications from the AWS SDK for Java v1 to the AWS SDK for Java v2.

## __Amazon Bedrock Agent Core Control Plane Fronting Layer__
  - ### Features
    - Add tagging and VPC support to AgentCore Runtime, Code Interpreter, and Browser resources. Add support for configuring request headers in Runtime. Fix AgentCore Runtime shape names.

## __Amazon Connect Service__
  - ### Features
    - This release adds a persistent connection field to UserPhoneConfig that maintains agent's softphone media connection for faster call connections.

## __Amazon Kendra Intelligent Ranking__
  - ### Features
    - Model whitespace change - no client difference

## __Amazon Simple Queue Service__
  - ### Features
    - Update invalid character handling documentation for SQS SendMessage API


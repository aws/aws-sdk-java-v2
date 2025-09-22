 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


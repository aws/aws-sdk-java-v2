 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


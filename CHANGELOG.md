 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.29.3__ __2024-10-29__
## __AWS Clean Rooms Service__
  - ### Features
    - This release adds the option for customers to configure analytics engine when creating a collaboration, and introduces the new SPARK analytics engine type in addition to maintaining the legacy CLEAN_ROOMS_SQL engine type.

## __AWS IoT FleetWise__
  - ### Features
    - Updated BatchCreateVehicle and BatchUpdateVehicle APIs: LimitExceededException has been added and the maximum number of vehicles in a batch has been set to 10 explicitly

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Update Application Inference Profile

## __Amazon Bedrock Runtime__
  - ### Features
    - Update Application Inference Profile

## __Amazon CloudWatch Logs__
  - ### Features
    - Added support for new optional baseline parameter in the UpdateAnomaly API. For UpdateAnomaly requests with baseline set to True, The anomaly behavior is then treated as baseline behavior. However, more severe occurrences of this behavior will still be reported as anomalies.

## __Amazon SageMaker Service__
  - ### Features
    - Adding `notebook-al2-v3` as allowed value to SageMaker NotebookInstance PlatformIdentifier attribute

## __Redshift Data API Service__
  - ### Features
    - Adding a new API GetStatementResultV2 that supports CSV formatted results from ExecuteStatement and BatchExecuteStatement calls.

# __2.29.2__ __2024-10-28__
## __AWS Elemental MediaPackage v2__
  - ### Features
    - MediaPackage V2 Live to VOD Harvester is a MediaPackage V2 feature, which is used to export content from an origin endpoint to a S3 bucket.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Storage Gateway__
  - ### Features
    - Documentation update: Amazon FSx File Gateway will no longer be available to new customers.

## __Amazon OpenSearch Service__
  - ### Features
    - Adds support for provisioning dedicated coordinator nodes. Coordinator nodes can be specified using the new NodeOptions parameter in ClusterConfig.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for Enhanced Monitoring and Performance Insights when restoring Aurora Limitless Database DB clusters. It also adds support for the os-upgrade pending maintenance action.

## __Amazon S3__
  - ### Bugfixes
    - Update the S3 client to correctly handle redirect cases for opt-in regions when crossRegionAccessEnabled is used.

# __2.29.1__ __2024-10-25__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports automatically retrying failed builds

## __AWS Lambda__
  - ### Features
    - Add TagsError field in Lambda GetFunctionResponse. The TagsError field contains details related to errors retrieving tags.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Supply Chain__
  - ### Features
    - API doc updates, and also support showing error message on a failed instance

## __Agents for Amazon Bedrock__
  - ### Features
    - Add support of new model types for Bedrock Agents, Adding inference profile support for Flows and Prompt Management, Adding new field to configure additional inference configurations for Flows and Prompt Management

## __Amazon CloudWatch Logs__
  - ### Features
    - Adding inferred token name for dynamic tokens in Anomalies.

# __2.29.0__ __2024-10-24__
## __AWS Parallel Computing Service__
  - ### Features
    - Documentation update: added the default value of the Slurm configuration parameter scaleDownIdleTimeInSeconds to its description.

## __AWS SDK for Java v2__
  - ### Features
    - The SDK now defaults to Java built-in CRC32 and CRC32C(if it's Java 9+) implementations, resulting in improved performance.
    - Updated endpoint and partition metadata.

  - ### Deprecations
    - Deprecate internal checksum algorithm classes.

## __Amazon AppConfig__
  - ### Features
    - This release improves deployment safety by granting customers the ability to REVERT completed deployments, to the last known good state.In the StopDeployment API revert case the status of a COMPLETE deployment will be REVERTED. AppConfig only allows a revert within 72 hours of deployment completion.

## __Amazon EC2 Container Service__
  - ### Features
    - This release adds support for EBS volumes attached to Amazon ECS Windows tasks running on EC2 instances.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release includes a new API to describe some details of the Amazon Machine Images (AMIs) that were used to launch EC2 instances, even if those AMIs are no longer available for use.

## __QBusiness__
  - ### Features
    - Add a new field in chat response. This field can be used to support nested schemas in array fields


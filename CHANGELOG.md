 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.40.3__ __2025-12-05__
## __AWS Identity and Access Management__
  - ### Features
    - Adding the ExpirationTime attribute to the delegation request resource.

## __AWS SDK for Java v2 Codegen__
  - ### Features
    - Automatically enable `AwsV4HttpSigner.CHUNK_ENCODING_ENABLED` signer property for input streaming operations that support checksums in generated auth scheme provider class

## __Amazon EC2 Container Service__
  - ### Features
    - Updating stop-task API to encapsulate containers with custom stop signal

## __Amazon Simple Email Service__
  - ### Features
    - Updating the desired url for `PutEmailIdentityDkimSigningAttributes` from v1 to v2

## __Inspector2__
  - ### Features
    - This release adds a new ScanStatus called "Unsupported Code Artifacts". This ScanStatus will be returned when a Lambda function was not code scanned because it has unsupported code artifacts.

## __Partner Central Account API__
  - ### Features
    - Adding Verification API's to Partner Central Account SDK.

# __2.40.2__ __2025-12-04__
## __AWS Lambda__
  - ### Features
    - Add DisallowedByVpcEncryptionControl to the LastUpdateStatusReasonCode and StateReasonCode enums to represent failures caused by VPC Encryption Controls.

## __Apache 5 HTTP Client (Preview)__
  - ### Bugfixes
    - Ignore negative values set `connectionTimeToLive`. There is no behavior change on the client as negative values have no meaning for Apache 5.

# __2.40.1__ __2025-12-03__
## __Amazon Bedrock__
  - ### Features
    - Adding support in Amazon Bedrock to customize models with reinforcement fine-tuning (RFT) and support for updating the existing Custom Model Deployments.

## __Amazon S3__
  - ### Bugfixes
    - Fix NPE issue thrown when using multipart S3 client to upload an object containing empty content without supplying a content length. See [#6464](https://github.com/aws/aws-sdk-java-v2/issues/6464)

## __Amazon SageMaker Service__
  - ### Features
    - Introduces Serverless training: A fully managed compute infrastructure that abstracts away all infrastructure complexity, allowing you to focus purely on model development. Added AI model customization assets used to train, refine, and evaluate custom models during the model customization process.

# __2.40.0__ __2025-12-02__
## __AWS Cost Explorer Service__
  - ### Features
    - This release updates existing Savings Plans Purchase Analyzer and Recommendations APIs to support Database Savings Plans.

## __AWS Lambda__
  - ### Features
    - Launching Lambda durable functions - a new feature to build reliable multi-step applications and AI workflows natively within the Lambda developer experience.

## __AWS S3 Control__
  - ### Features
    - Add support for S3 Storage Lens Advanced Performance Metrics, Expanded Prefixes metrics report, and export to S3 Tables.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Savings Plans__
  - ### Features
    - Added support for Amazon Database Savings Plans

## __AWS SecurityHub__
  - ### Features
    - ITSM enhancements: DRYRUN mode for testing ticket creation, ServiceNow now uses AWS Secrets Manager for credentials, ConnectorRegistrationsV2 renamed to RegisterConnectorV2, added ServiceQuotaExceededException error, and ConnectorStatus visibility in CreateConnectorV2.

## __Amazon Bedrock__
  - ### Features
    - Adds the audioDataDeliveryEnabled boolean field to the Model Invocation Logging Configuration.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Support for AgentCore Evaluations and Episodic memory strategy for AgentCore Memory.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Supports AgentCore Evaluations, Policy, Episodic Memory Strategy, Resource Based Policy for Runtime and Gateway APIs, API Gateway Rest API Targets and enhances JWT authorizer.

## __Amazon Bedrock Runtime__
  - ### Features
    - Adds support for Audio Blocks and Streaming Image Output plus new Stop Reasons of malformed_model_output and malformed_tool_use.

## __Amazon CloudWatch Logs__
  - ### Features
    - CloudWatch Logs adds managed S3 Tables integration to access logs using other analytical tools, as well as facets and field indexing to simplify log analytics in CloudWatch Logs Insights.

## __Amazon DataZone__
  - ### Features
    - Amazon DataZone now supports exporting Catalog datasets as Amazon S3 tables, and provides automatic business glossary term suggestions for data assets.

## __Amazon FSx__
  - ### Features
    - S3 Access Points support for FSx for NetApp ONTAP

## __Amazon GuardDuty__
  - ### Features
    - Adding support for extended threat detection for Amazon EC2 and Amazon ECS. Adding support for wild card suppression rules.

## __Amazon OpenSearch Service__
  - ### Features
    - GPU-acceleration helps you build large-scale vector databases faster and more efficiently. You can enable this feature on new OpenSearch domains and OpenSearch Serverless collections. This feature uses GPU-acceleration to reduce the time needed to index data into vector indexes.

## __Amazon Relational Database Service__
  - ### Features
    - RDS Oracle and SQL Server: Add support for adding, modifying, and removing additional storage volumes, offering up to 256TiB storage; RDS SQL Server: Support Developer Edition via custom engine versions for development and testing purposes; M7i/R7i instances with Optimize CPU for cost savings.

## __Amazon S3 Tables__
  - ### Features
    - Add storage class, replication, and table record expiration features to S3 Tables.

## __Amazon S3 Vectors__
  - ### Features
    - Amazon S3 Vectors provides cost-effective, elastic, and durable vector storage for queries based on semantic meaning and similarity.

## __Amazon SageMaker Service__
  - ### Features
    - Added support for serverless MLflow Apps. Added support for new HubContentTypes (DataSet and JsonDoc) in Private Hub for AI model customization assets, enabling tracking and management of training datasets and evaluators (reward functions/prompts) throughout the ML lifecycle.

## __Amazon Simple Storage Service__
  - ### Features
    - New S3 Storage Class FSX_ONTAP

## __CloudWatch Observability Admin Service__
  - ### Features
    - CloudWatch Observability Admin adds pipelines configuration for third party log ingestion and transformation of all logs ingested, integration of CloudWatch logs with S3 Tables, and AWS account or organization level enablement for 7 AWS services.

## __Nova Act Service__
  - ### Features
    - Initial release of Nova Act SDK. The Nova Act service enables customers to build and manage fleets of agents for automating production UI workflows with high reliability, fastest time-to-value, and ease of implementation at scale.

## __OpenSearch Service Serverless__
  - ### Features
    - GPU-acceleration helps you build large-scale vector databases faster and more efficiently. You can enable this feature on new OpenSearch domains and OpenSearch Serverless collections. This feature uses GPU-acceleration to reduce the time needed to index data into vector indexes.

## __S3__
  - ### Features
    - Add support for parallel download for individual part-get for multipart GetObject in s3 async client and Transfer Manager


 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.25.22__ __2024-04-01__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSDeadlineCloud__
  - ### Features
    - AWS Deadline Cloud is a new fully managed service that helps customers set up, deploy, and scale rendering projects in minutes, so they can improve the efficiency of their rendering pipelines and take on more projects.

## __Amazon CloudWatch__
  - ### Features
    - This release adds support for CloudWatch Anomaly Detection on cross-account metrics. SingleMetricAnomalyDetector and MetricDataQuery inputs to Anomaly Detection APIs now take an optional AccountId field.

## __Amazon DataZone__
  - ### Features
    - This release supports the feature of AI recommendations for descriptions to enrich the business data catalog in Amazon DataZone.

## __Amazon EMR__
  - ### Features
    - This release fixes a broken link in the documentation.

## __Amazon Lightsail__
  - ### Features
    - This release adds support to upgrade the TLS version of the distribution.

# __2.25.21__ __2024-03-29__
## __AWS B2B Data Interchange__
  - ### Features
    - Supporting new EDI X12 transaction sets for X12 versions 4010, 4030, and 5010.

## __AWS CodeBuild__
  - ### Features
    - Add new fleet status code for Reserved Capacity.

## __AWS CodeConnections__
  - ### Features
    - Duplicating the CodeStar Connections service into the new, rebranded AWS CodeConnections service.

## __AWS IoT Wireless__
  - ### Features
    - Add support for retrieving key historical and live metrics for LoRaWAN devices and gateways

## __AWS Marketplace Catalog Service__
  - ### Features
    - This release enhances the ListEntities API to support ResaleAuthorizationId filter and sort for OfferEntity in the request and the addition of a ResaleAuthorizationId field in the response of OfferSummary.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch Internet Monitor__
  - ### Features
    - This release adds support to allow customers to track cross account monitors through ListMonitor, GetMonitor, ListHealthEvents, GetHealthEvent, StartQuery APIs.

## __Amazon Neptune Graph__
  - ### Features
    - Add the new API Start-Import-Task for Amazon Neptune Analytics.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds support for custom images for the CodeEditor App on SageMaker Studio

# __2.25.20__ __2024-03-28__
## __AWS Compute Optimizer__
  - ### Features
    - This release enables AWS Compute Optimizer to analyze and generate recommendations with a new customization preference, Memory Utilization.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CodeCatalyst__
  - ### Features
    - This release adds support for understanding pending changes to subscriptions by including two new response parameters for the GetSubscription API for Amazon CodeCatalyst.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 C7gd, M7gd and R7gd metal instances with up to 3.8 TB of local NVMe-based SSD block-level storage have up to 45% improved real-time NVMe storage performance than comparable Graviton2-based instances.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Add multiple customer error code to handle customer caused failure when managing EKS node groups

## __Amazon GuardDuty__
  - ### Features
    - Add EC2 support for GuardDuty Runtime Monitoring auto management.

## __Amazon Neptune Graph__
  - ### Features
    - Update ImportTaskCancelled waiter to evaluate task state correctly and minor documentation changes.

## __Amazon QuickSight__
  - ### Features
    - Amazon QuickSight: Adds support for setting up VPC Endpoint restrictions for accessing QuickSight Website.

## __CloudWatch Observability Access Manager__
  - ### Features
    - This release adds support for sharing AWS::InternetMonitor::Monitor resources.

# __2.25.19__ __2024-03-27__
## __AWS Batch__
  - ### Features
    - This feature allows AWS Batch to support configuration of imagePullSecrets and allowPrivilegeEscalation for jobs running on EKS

## __AWS SDK For Java v2__
  - ### Bugfixes
    - Remediate CVE-2024-29025 by updating netty version
        - Contributed by: [@FatalEnigma](https://github.com/FatalEnigma)

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Secrets Manager__
  - ### Features
    - Documentation updates for Secrets Manager

## __Agents for Amazon Bedrock__
  - ### Features
    - This changes introduces metadata documents statistics and also updates the documentation for bedrock agent.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This release introduces filtering support on Retrieve and RetrieveAndGenerate APIs.

## __Amazon ElastiCache__
  - ### Features
    - Added minimum capacity to Amazon ElastiCache Serverless. This feature allows customer to ensure minimum capacity even without current load

## __S3 Transfer Manager__
  - ### Bugfixes
    - Set a limit on the number of concurrent upload file requests for upload directory. This fixes the OOM issue that could surface when users try to upload a directory that has millions of small files. See [#5023](https://github.com/aws/aws-sdk-java-v2/issues/5023).

## __Contributors__
Special thanks to the following contributors to this release: 

[@FatalEnigma](https://github.com/FatalEnigma)
# __2.25.18__ __2024-03-26__
## __AWS Cost Explorer Service__
  - ### Features
    - Adds support for backfill of cost allocation tags, with new StartCostAllocationTagBackfill and ListCostAllocationTagBackfillHistory API.

## __AWS S3__
  - ### Features
    - allow user to configure subscriber timeout for input stream
        - Contributed by: [@benarnao](https://github.com/benarnao)

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This release adds support to customize prompts sent through the RetrieveAndGenerate API in Agents for Amazon Bedrock.

## __Amazon EC2 Container Service__
  - ### Features
    - This is a documentation update for Amazon ECS.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for Elastic Compute Cloud (EC2).

## __FinSpace User Environment Management service__
  - ### Features
    - Add new operation delete-kx-cluster-node and add status parameter to list-kx-cluster-node operation.

## __Contributors__
Special thanks to the following contributors to this release: 

[@benarnao](https://github.com/benarnao)
# __2.25.17__ __2024-03-25__
## __AWS CodeBuild__
  - ### Features
    - Supporting GitLab and GitLab Self Managed as source types in AWS CodeBuild.

## __AWS Elemental MediaLive__
  - ### Features
    - Exposing TileMedia H265 options

## __AWS Global Accelerator__
  - ### Features
    - AWS Global Accelerator now supports cross-account sharing for bring your own IP addresses.

## __AWS SDK for Java v2__
  - ### Features
    - Support creating an `SdkPublisher` from an `Iterable`
    - Updated endpoint and partition metadata.

## __Amazon EC2 Container Service__
  - ### Features
    - Documentation only update for Amazon ECS.

## __Amazon EMR Containers__
  - ### Features
    - This release increases the number of supported job template parameters from 20 to 100.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added support for ModifyInstanceMetadataDefaults and GetInstanceMetadataDefaults to set Instance Metadata Service account defaults

## __Amazon SageMaker Service__
  - ### Features
    - Introduced support for the following new instance types on SageMaker Studio for JupyterLab and CodeEditor applications: m6i, m6id, m7i, c6i, c6id, c7i, r6i, r6id, r7i, and p5

# __2.25.16__ __2024-03-22__
## __AWS Price List Service__
  - ### Features
    - Add ResourceNotFoundException to ListPriceLists and GetPriceListFileUrl APIs

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Improve HTTP2/PING timeout logic. Improvement avoids premature timeouts due to delays in scheduling the write of PING frame to the channel and/or flushing it to the socket.
        - Contributed by: [@akidambisrinivasan](https://github.com/akidambisrinivasan)

## __AWS SecurityHub__
  - ### Features
    - Added new resource detail object to ASFF, including resource for LastKnownExploitAt

## __AWSKendraFrontendService__
  - ### Features
    - Documentation update, March 2024. Corrects some docs for Amazon Kendra.

## __Amazon Kinesis Firehose__
  - ### Features
    - Updates Amazon Firehose documentation for message regarding Enforcing Tags IAM Policy.

## __IAM Roles Anywhere__
  - ### Features
    - This release relaxes constraints on the durationSeconds request parameter for the *Profile APIs that support it. This parameter can now take on values that go up to 43200.

## __Contributors__
Special thanks to the following contributors to this release: 

[@akidambisrinivasan](https://github.com/akidambisrinivasan)
# __2.25.15__ __2024-03-21__
## __CodeArtifact__
  - ### Features
    - This release adds Package groups to CodeArtifact so you can more conveniently configure package origin controls for multiple packages.

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fix normalize key method to not strip extra character when prefix is not immediately followed by separator in the key.
    - Fixed OOM issue that could surface when users try to download objects from an S3 bucket that has millions of small files. See [#4987](https://github.com/aws/aws-sdk-java-v2/issues/4987)

# __2.25.14__ __2024-03-20__
## __AWS CodeBuild__
  - ### Features
    - This release adds support for new webhook events (RELEASED and PRERELEASED) and filter types (TAG_NAME and RELEASE_NAME).

## __AWS Savings Plans__
  - ### Features
    - Introducing the Savings Plans Return feature enabling customers to return their Savings Plans within 7 days of purchase.

## __Access Analyzer__
  - ### Features
    - This release adds support for policy validation and external access findings for DynamoDB tables and streams. IAM Access Analyzer helps you author functional and secure resource-based policies and identify cross-account access. Updated service API, documentation, and paginators.

## __Amazon Connect Service__
  - ### Features
    - This release updates the *InstanceStorageConfig APIs to support a new ResourceType: REAL_TIME_CONTACT_ANALYSIS_CHAT_SEGMENTS. Use this resource type to enable streaming for real-time analysis of chat contacts and to associate a Kinesis stream where real-time analysis chat segments will be published.

## __Amazon DynamoDB__
  - ### Features
    - This release introduces 3 new APIs ('GetResourcePolicy', 'PutResourcePolicy' and 'DeleteResourcePolicy') and modifies the existing 'CreateTable' API for the resource-based policy support. It also modifies several APIs to accept a 'TableArn' for the 'TableName' parameter.

## __Amazon Managed Blockchain Query__
  - ### Features
    - AMB Query: update GetTransaction to include transactionId as input

# __2.25.13__ __2024-03-19__
## __AWS CloudFormation__
  - ### Features
    - Documentation update, March 2024. Corrects some formatting.

## __Amazon CloudWatch Logs__
  - ### Features
    - Update LogSamples field in Anomaly model to be a list of LogEvent

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds the new DescribeMacHosts API operation for getting information about EC2 Mac Dedicated Hosts. Users can now see the latest macOS versions that their underlying Apple Mac can support without needing to be updated.

## __Amazon Managed Blockchain Query__
  - ### Features
    - Introduces a new API for Amazon Managed Blockchain Query: ListFilteredTransactionEvents.

## __FinSpace User Environment Management service__
  - ### Features
    - Adding new attributes readWrite and onDemand to dataview models for Database Maintenance operations.

# __2.25.12__ __2024-03-18__
## __AWS CloudFormation__
  - ### Features
    - This release supports for a new API ListStackSetAutoDeploymentTargets, which provider auto-deployment configuration as a describable resource. Customers can now view the specific combinations of regions and OUs that are being auto-deployed.

## __AWS Key Management Service__
  - ### Features
    - Adds the ability to use the default policy name by omitting the policyName parameter in calls to PutKeyPolicy and GetKeyPolicy

## __AWS MediaTailor__
  - ### Features
    - This release adds support to allow customers to show different content within a channel depending on metadata associated with the viewer.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Relational Database Service__
  - ### Features
    - This release launches the ModifyIntegration API and support for data filtering for zero-ETL Integrations.

## __Amazon Simple Storage Service__
  - ### Features
    - Fix two issues with response root node names.

## __Amazon Timestream Query__
  - ### Features
    - Documentation updates, March 2024

# __2.25.11__ __2024-03-15__
## __AWS Backup__
  - ### Features
    - This release introduces a boolean attribute ManagedByAWSBackupOnly as part of ListRecoveryPointsByResource api to filter the recovery points based on ownership. This attribute can be used to filter out the recovery points protected by AWSBackup.

## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports overflow behavior on Reserved Capacity.

## __AWS SDK for Java v2__
  - ### Features
    - Add the model for S3 Event Notifications and json parsers for them
    - Updated endpoint and partition metadata.

## __Amazon Connect Service__
  - ### Features
    - This release adds Hierarchy based Access Control fields to Security Profile public APIs and adds support for UserAttributeFilter to SearchUsers API.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add media accelerator and neuron device information on the describe instance types API.

## __Amazon Kinesis Analytics__
  - ### Features
    - Support for Flink 1.18 in Managed Service for Apache Flink

## __Amazon SageMaker Service__
  - ### Features
    - Adds m6i, m6id, m7i, c6i, c6id, c7i, r6i r6id, r7i, p5 instance type support to Sagemaker Notebook Instances and miscellaneous wording fixes for previous Sagemaker documentation.

## __Amazon Simple Storage Service__
  - ### Features
    - Documentation updates for Amazon S3.

## __Amazon WorkSpaces Thin Client__
  - ### Features
    - Removed unused parameter kmsKeyArn from UpdateDeviceRequest

# __2.25.10__ __2024-03-14__
## __AWS Amplify__
  - ### Features
    - Documentation updates for Amplify. Identifies the APIs available only to apps created using Amplify Gen 1.

## __AWS EC2 Instance Connect__
  - ### Features
    - This release includes a new exception type "SerialConsoleSessionUnsupportedException" for SendSerialConsoleSSHPublicKey API.

## __AWS Fault Injection Simulator__
  - ### Features
    - This release adds support for previewing target resources before running a FIS experiment. It also adds resource ARNs for actions, experiments, and experiment templates to API responses.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue in `ByteBufferStoringSubscriber` where it could buffer more data than configured, resulting in out of memory error. See [#4999](https://github.com/aws/aws-sdk-java-v2/issues/4999).

## __AWS Secrets Manager__
  - ### Features
    - Doc only update for Secrets Manager

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation for EBCDIC collation for RDS for Db2.

## __Elastic Load Balancing__
  - ### Features
    - This release allows you to configure HTTP client keep-alive duration for communication between clients and Application Load Balancers.

## __Timestream InfluxDB__
  - ### Features
    - This is the initial SDK release for Amazon Timestream for InfluxDB. Amazon Timestream for InfluxDB is a new time-series database engine that makes it easy for application developers and DevOps teams to run InfluxDB databases on AWS for near real-time time-series applications using open source APIs.

# __2.25.9__ __2024-03-13__
## __Amazon Interactive Video Service RealTime__
  - ### Features
    - adds support for multiple new composition layout configuration options (grid, pip)

## __Amazon Kinesis Analytics__
  - ### Features
    - Support new RuntimeEnvironmentUpdate parameter within UpdateApplication API allowing callers to change the Flink version upon which their application runs.

## __Amazon Simple Storage Service__
  - ### Features
    - This release makes the default option for S3 on Outposts request signing to use the SigV4A algorithm when using AWS Common Runtime (CRT).

## __S3 Transfer Manager__
  - ### Bugfixes
    - AWS-CRT based S3 Transfer Manager now relies on CRT to perform file reading for upload directory. Related to [#4999](https://github.com/aws/aws-sdk-java-v2/issues/4999)

# __2.25.8__ __2024-03-12__
## __AWS CloudFormation__
  - ### Features
    - CloudFormation documentation update for March, 2024

## __AWS SDK for Java v2__
  - ### Features
    - Allow users to configure `subscribeTimeout` for BlockingOutputStreamAsyncRequestBody. See [#4893](https://github.com/aws/aws-sdk-java-v2/issues/4893)

## __Amazon Connect Service__
  - ### Features
    - This release increases MaxResults limit to 500 in request for SearchUsers, SearchQueues and SearchRoutingProfiles APIs of Amazon Connect.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for Amazon EC2.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - March 2024 doc-only updates for Systems Manager.

## __Managed Streaming for Kafka__
  - ### Features
    - Added support for specifying the starting position of topic replication in MSK-Replicator.

# __2.25.7__ __2024-03-11__
## __AWS CodeStar connections__
  - ### Features
    - Added a sync configuration enum to disable publishing of deployment status to source providers (PublishDeploymentStatus). Added a sync configuration enum (TriggerStackUpdateOn) to only trigger changes.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release enables customers to safely update their MediaPackage v2 channel groups, channels and origin endpoints using entity tags.

## __Amazon ElastiCache__
  - ### Features
    - Revisions to API text that are now to be carried over to SDK text, changing usages of "SFO" in code examples to "us-west-1", and some other typos.

# __2.25.6__ __2024-03-08__
## __AWS Batch__
  - ### Features
    - This release adds JobStateTimeLimitActions setting to the Job Queue API. It allows you to configure an action Batch can take for a blocking job in front of the queue after the defined period of time. The new parameter applies for ECS, EKS, and FARGATE Job Queues.

## __AWS CloudTrail__
  - ### Features
    - Added exceptions to CreateTrail, DescribeTrails, and ListImportFailures APIs.

## __AWS CodeBuild__
  - ### Features
    - This release adds support for a new webhook event: PULL_REQUEST_CLOSED.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Transfer Family__
  - ### Features
    - Added DES_EDE3_CBC to the list of supported encryption algorithms for messages sent with an AS2 connector.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Documentation update for Bedrock Runtime Agent

## __Amazon Cognito Identity Provider__
  - ### Features
    - Add ConcurrentModificationException to SetUserPoolMfaConfig

## __Amazon GuardDuty__
  - ### Features
    - Add RDS Provisioned and Serverless Usage types

# __2.25.5__ __2024-03-07__
## __AWS Lambda__
  - ### Features
    - Documentation updates for AWS Lambda

## __AWS S3__
  - ### Bugfixes
    - Fixed the issue in S3 multipart client where the list of parts could be out of order in CompleteMultipartRequest, causing `The list of parts was not in ascending order` error to be thrown.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Modify ARN toString to print a valid ARN when there's no region or acountId
        - Contributed by: [@Madrigal](https://github.com/Madrigal)

## __AWS WAFV2__
  - ### Features
    - You can increase the max request body inspection size for some regional resources. The size setting is in the web ACL association config. Also, the AWSManagedRulesBotControlRuleSet EnableMachineLearning setting now takes a Boolean instead of a primitive boolean type, for languages like Java.

## __Amazon AppConfig__
  - ### Features
    - AWS AppConfig now supports dynamic parameters, which enhance the functionality of AppConfig Extensions by allowing you to provide parameter values to your Extensions at the time you deploy your configuration.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds an optional parameter to RegisterImage and CopyImage APIs to support tagging AMIs at the time of creation.

## __Amazon Import/Export Snowball__
  - ### Features
    - Doc-only update for change to EKS-Anywhere ordering.

## __Amazon Managed Grafana__
  - ### Features
    - Adds support for the new GrafanaToken as part of the Amazon Managed Grafana Enterprise plugins upgrade to associate your AWS account with a Grafana Labs account.

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation for io2 storage for Multi-AZ DB clusters

## __Amazon WorkSpaces__
  - ### Features
    - Added note for user decoupling

## __Payment Cryptography Data Plane__
  - ### Features
    - AWS Payment Cryptography EMV Decrypt Feature Release

## __Contributors__
Special thanks to the following contributors to this release: 

[@Madrigal](https://github.com/Madrigal)
# __2.25.4__ __2024-03-06__
## __Amazon DynamoDB__
  - ### Features
    - Doc only updates for DynamoDB documentation

## __Amazon Redshift__
  - ### Features
    - Update for documentation only. Covers port ranges, definition updates for data sharing, and definition updates to cluster-snapshot documentation.

## __Amazon Relational Database Service__
  - ### Features
    - Updated the input of CreateDBCluster and ModifyDBCluster to support setting CA certificates. Updated the output of DescribeDBCluster to show current CA certificate setting value.

## __Amazon Verified Permissions__
  - ### Features
    - Deprecating details in favor of configuration for GetIdentitySource and ListIdentitySources APIs.

## __AmazonMWAA__
  - ### Features
    - Amazon MWAA adds support for Apache Airflow v2.8.1.

## __EC2 Image Builder__
  - ### Features
    - Add PENDING status to Lifecycle Execution resource status. Add StartTime and EndTime to ListLifecycleExecutionResource API response.

# __2.25.3__ __2024-03-05__
## __AWS Chatbot__
  - ### Features
    - Minor update to documentation.

## __AWS Organizations__
  - ### Features
    - This release contains an endpoint addition

## __Amazon API Gateway__
  - ### Features
    - Documentation updates for Amazon API Gateway

## __Amazon Simple Email Service__
  - ### Features
    - Adds support for providing custom headers within SendEmail and SendBulkEmail for SESv2.

# __2.25.2__ __2024-03-04__
## __AWS CloudFormation__
  - ### Features
    - Add DetailedStatus field to DescribeStackEvents and DescribeStacks APIs

## __AWS Organizations__
  - ### Features
    - Documentation update for AWS Organizations

## __Amazon FSx__
  - ### Features
    - Added support for creating FSx for NetApp ONTAP file systems with up to 12 HA pairs, delivering up to 72 GB/s of read throughput and 12 GB/s of write throughput.

# __2.25.1__ __2024-03-01__
## __Access Analyzer__
  - ### Features
    - Fixed a typo in description field.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - With this release, Amazon EC2 Auto Scaling groups, EC2 Fleet, and Spot Fleet improve the default price protection behavior of attribute-based instance type selection of Spot Instances, to consistently select from a wide range of instance types.

## __Auto Scaling__
  - ### Features
    - With this release, Amazon EC2 Auto Scaling groups, EC2 Fleet, and Spot Fleet improve the default price protection behavior of attribute-based instance type selection of Spot Instances, to consistently select from a wide range of instance types.

# __2.25.0__ __2024-02-29__
## __AWS CRT HTTP Client__
  - ### Features
    - Support Non proxy host settings in the ProxyConfiguration for Crt http client.

  - ### Bugfixes
    - Addressing Issue [#4745](https://github.com/aws/aws-sdk-java-v2/issues/4745) , Netty and CRT clients' default proxy settings have been made consistent with the Apache client, now using environment and system property settings by default.\n To disable the use of environment variables and system properties by default, set useSystemPropertyValue(false) and useEnvironmentVariablesValues(false) in ProxyConfigurations.

## __AWS Migration Hub Orchestrator__
  - ### Features
    - Adds new CreateTemplate, UpdateTemplate and DeleteTemplate APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Addressing Issue [#4745](https://github.com/aws/aws-sdk-java-v2/issues/4745) , Netty and CRT clients' default proxy settings have been made consistent with the Apache client, now using environment and system property settings by default.
       To disable the use of environment variables and system properties by default, set useSystemPropertyValue(false) and useEnvironmentVariablesValues(false) in ProxyConfigurations.

## __Amazon DocumentDB Elastic Clusters__
  - ### Features
    - Launched Elastic Clusters Readable Secondaries, Start/Stop, Configurable Shard Instance count, Automatic Backups and Snapshot Copying

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support for new AL2023 AMIs to the supported AMITypes.

## __Amazon Lex Model Building V2__
  - ### Features
    - This release makes AMAZON.QnAIntent generally available in Amazon Lex. This generative AI feature leverages large language models available through Amazon Bedrock to automate frequently asked questions (FAQ) experience for end-users.

## __Amazon QuickSight__
  - ### Features
    - TooltipTarget for Combo chart visuals; ColumnConfiguration limit increase to 2000; Documentation Update

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for ModelDataSource in Model Packages to support unzipped models. Adds support to specify SourceUri for models which allows registration of models without mandating a container for hosting. Using SourceUri, customers can decouple the model from hosting information during registration.

## __Amazon Security Lake__
  - ### Features
    - Add capability to update the Data Lake's MetaStoreManager Role in order to perform required data lake updates to use Iceberg table format in their data lake or update the role for any other reason.

## __Netty NIO Async HTTP Client__
  - ### Bugfixes
    - Addressing Issue [#4745](https://github.com/aws/aws-sdk-java-v2/issues/4745) , Netty and CRT clients' default proxy settings have been made consistent with the Apache client, now using environment and system property settings by default.\n To disable the use of environment variables and system properties by default, set useSystemPropertyValue(false) and useEnvironmentVariablesValues(false) in ProxyConfigurations


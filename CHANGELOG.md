 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.29.25__ __2024-12-02__
## __AWS End User Messaging Social__
  - ### Features
    - Added support for passing role arn corresponding to the supported event destination

## __AWS S3 Control__
  - ### Features
    - It allows customers to pass CRC64NVME as a header in S3 Batch Operations copy requests

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock Runtime__
  - ### Features
    - Add an API parameter that allows customers to set performance configuration for invoking a model.

# __2.29.24__ __2024-12-01__
## __AWS CRT-based S3 client__
  - ### Bugfixes
    - Fixed an issue where an error was not surfaced if request failed halfway for a GetObject operation. See [#5631](https://github.com/aws/aws-sdk-java-v2/issues/5631)

## __AWS Clean Rooms Service__
  - ### Features
    - This release allows customers and their partners to easily collaborate with data stored in Snowflake and Amazon Athena, without having to move or share their underlying data among collaborators.

## __AWS Invoicing__
  - ### Features
    - AWS Invoice Configuration allows you to receive separate AWS invoices based on your organizational needs. You can use the AWS SDKs to manage Invoice Units and programmatically fetch the information of the invoice receiver.

## __AWS Organizations__
  - ### Features
    - Add support for policy operations on the DECLARATIVE_POLICY_EC2 policy type.

## __AWS S3 Control__
  - ### Features
    - Amazon S3 introduces support for AWS Dedicated Local Zones

## __AWS SecurityHub__
  - ### Features
    - Add new Multi Domain Correlation findings.

## __AWS Transfer Family__
  - ### Features
    - AWS Transfer Family now offers Web apps that enables simple and secure access to data stored in Amazon S3.

## __Agents for Amazon Bedrock__
  - ### Features
    - This release introduces APIs to upload documents directly into a Knowledge Base

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This release introduces a new Rerank API to leverage reranking models (with integration into Knowledge Bases); APIs to upload documents directly into Knowledge Base; RetrieveAndGenerateStream API for streaming response; Guardrails on Retrieve API; and ability to automatically generate filters

## __Amazon Bedrock__
  - ### Features
    - Add support for Knowledge Base Evaluations & LLM as a judge

## __Amazon Chime SDK Voice__
  - ### Features
    - This release adds supports for enterprises to integrate Amazon Connect with other voice systems. It supports directly transferring voice calls and metadata without using the public telephone network. It also supports real-time and post-call analytics.

## __Amazon CloudWatch Logs__
  - ### Features
    - Adds PutIntegration, GetIntegration, ListIntegrations and DeleteIntegration APIs. Adds QueryLanguage support to StartQuery, GetQueryResults, DescribeQueries, DescribeQueryDefinitions, and PutQueryDefinition APIs.

## __Amazon Connect Customer Profiles__
  - ### Features
    - This release introduces Event Trigger APIs as part of Amazon Connect Customer Profiles service.

## __Amazon Connect Service__
  - ### Features
    - Adds support for WhatsApp Business messaging, IVR call recording, enabling Contact Lens for existing on-premise contact centers and telephony platforms, and enabling telephony and IVR migration to Amazon Connect independent of their contact center agents.

## __Amazon EC2 Container Service__
  - ### Features
    - This release adds support for Container Insights with Enhanced Observability for Amazon ECS.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds support for declarative policies that allow you to enforce desired configuration across an AWS organization through configuring account attributes. Adds support for Allowed AMIs that allows you to limit the use of AMIs in AWS accounts. Adds support for connectivity over non-HTTP protocols.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support for Auto Mode Clusters, Hybrid Nodes, and specifying computeTypes in the DescribeAddonVersions API.

## __Amazon EventBridge__
  - ### Features
    - Call private APIs by configuring Connections with VPC connectivity through PrivateLink and VPC Lattice

## __Amazon FSx__
  - ### Features
    - FSx API changes to support the public launch of the Amazon FSx Intelligent Tiering for OpenZFS storage class.

## __Amazon GuardDuty__
  - ### Features
    - Add new Multi Domain Correlation findings.

## __Amazon MemoryDB__
  - ### Features
    - Amazon MemoryDB SDK now supports all APIs for Multi-Region. Please refer to the updated Amazon MemoryDB public documentation for detailed information on API usage.

## __Amazon OpenSearch Service__
  - ### Features
    - This feature introduces support for CRUDL APIs, enabling the creation and management of Connected data sources.

## __Amazon Q Connect__
  - ### Features
    - This release adds following capabilities: Configuring safeguards via AIGuardrails for Q in Connect inferencing, and APIs to support Q&A self-service use cases

## __Amazon Relational Database Service__
  - ### Features
    - Amazon RDS supports CloudWatch Database Insights. You can use the SDK to create, modify, and describe the DatabaseInsightsMode for your DB instances and clusters.

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon S3 introduces support for AWS Dedicated Local Zones

## __Amazon VPC Lattice__
  - ### Features
    - Lattice APIs that allow sharing and access of VPC resources across accounts.

## __AmazonConnectCampaignServiceV2__
  - ### Features
    - Amazon Connect Outbound Campaigns V2 / Features : Adds support for Event-Triggered Campaigns.

## __EC2 Image Builder__
  - ### Features
    - Added support for EC2 Image Builder's integration with AWS Marketplace for Marketplace components.

## __Network Flow Monitor__
  - ### Features
    - This release adds documentation for a new feature in Amazon CloudWatch called Network Flow Monitor. You can use Network Flow Monitor to get near real-time metrics, including retransmissions and data transferred, for your actual workloads.

## __QBusiness__
  - ### Features
    - Amazon Q Business now supports capabilities to extract insights and answer questions from visual elements embedded within documents, a browser extension for Google Chrome, Mozilla Firefox, and Microsoft Edge, and attachments across conversations.

## __Security Incident Response__
  - ### Features
    - AWS Security Incident Response is a purpose-built security incident solution designed to help customers prepare for, respond to, and recover from security incidents.

# __2.29.23__ __2024-11-27__
## __AWS Config__
  - ### Features
    - AWS Config adds support for service-linked recorders, a new type of Config recorder managed by AWS services to record specific subsets of resource configuration data and functioning independently from customer managed AWS Config recorders.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - Add support for specifying embeddingDataType, either FLOAT32 or BINARY

## __Amazon FSx__
  - ### Features
    - This release adds EFA support to increase FSx for Lustre file systems' throughput performance to a single client instance. This can be done by specifying EfaEnabled=true at the time of creation of Persistent_2 file systems.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Amazon CloudWatch Observability Admin adds the ability to audit telemetry configuration for AWS resources in customers AWS Accounts and Organizations. The release introduces new APIs to turn on/off the new experience, which supports discovering supported AWS resources and their state of telemetry.

## __DynamoDB Enhanced Client__
  - ### Bugfixes
    - Fix a bug where DurationAttributeConverter was considering any number past the decimal point as a nanosecond during deserialization

# __2.29.22__ __2024-11-26__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix ApacheHttpClient's handling of request bodies on DELETE, GET, HEAD & OPTIONS requests
        - Contributed by: [@Xtansia](https://github.com/Xtansia)

## __Agents for Amazon Bedrock__
  - ### Features
    - Custom Orchestration API release for AWSBedrockAgents.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Custom Orchestration and Streaming configurations API release for AWSBedrockAgents.

## __Amazon Connect Service__
  - ### Features
    - Enables access to ValueMap and ValueInteger types for SegmentAttributes and fixes deserialization bug for DescribeContactFlow in AmazonConnect Public API

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds support for Time-based Copy for EBS Snapshots and Cross Region PrivateLink. Time-based Copy ensures that EBS Snapshots are copied within and across AWS Regions in a specified timeframe. Cross Region PrivateLink enables customers to connect to VPC endpoint services hosted in other AWS Regions.

## __QApps__
  - ### Features
    - Private sharing, file upload and data collection feature support for Q Apps

## __Contributors__
Special thanks to the following contributors to this release: 

[@Xtansia](https://github.com/Xtansia)
# __2.29.21__ __2024-11-25__
## __AWS Direct Connect__
  - ### Features
    - Update DescribeDirectConnectGatewayAssociations API to return associated core network information if a Direct Connect gateway is attached to a Cloud WAN core network.

## __AWS Network Manager__
  - ### Features
    - This release adds native Direct Connect integration on Cloud WAN enabling customers to directly attach their Direct Connect gateways to Cloud WAN without the need for an intermediate Transit Gateway.

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon Simple Storage Service / Features: Add support for ETag based conditional writes in PutObject and CompleteMultiPartUpload APIs to prevent unintended object modifications.

## __v2-migration OpenRewrite recipe__
  - ### Bugfixes
    - This fixes a ConcurrentModificationException - just by replacing a HashMap with ConcurrentHashMap
        - Contributed by: [@sk-br](https://github.com/sk-br)

## __Contributors__
Special thanks to the following contributors to this release: 

[@sk-br](https://github.com/sk-br)
# __2.29.20__ __2024-11-22__
## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Initial release of the AWS Billing and Cost Management Pricing Calculator API.

## __AWS Chatbot__
  - ### Features
    - Adds support for programmatic management of custom actions and aliases which can be associated with channel configurations.

## __AWS CodePipeline__
  - ### Features
    - AWS CodePipeline V2 type pipelines now support ECRBuildAndPublish and InspectorScan actions.

## __AWS Cost Explorer Service__
  - ### Features
    - This release adds the Impact field(contains Contribution field) to the GetAnomalies API response under RootCause

## __AWS Lambda__
  - ### Features
    - Add ProvisionedPollerConfig to Lambda event-source-mapping API.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Step Functions__
  - ### Features
    - Add support for variables and JSONata in TestState, GetExecutionHistory, DescribeStateMachine, and DescribeStateMachineForExecution

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - InvokeInlineAgent API release to help invoke runtime agents without any dependency on preconfigured agents.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Add support for users to sign up and sign in without passwords, using email and SMS OTPs and Passkeys. Add support for Passkeys based on WebAuthn. Add support for enhanced branding customization for hosted authentication pages with Amazon Cognito Managed Login. Add feature tiers with new pricing.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect Service Feature: Add APIs for Amazon Connect Email Channel

## __Amazon EMR__
  - ### Features
    - Advanced Scaling in Amazon EMR Managed Scaling

## __Amazon Neptune Graph__
  - ### Features
    - Add 4 new APIs to support new Export features, allowing Parquet and CSV formats. Add new arguments in Import APIs to support Parquet import. Add a new query "neptune.read" to run algorithms without loading data into database

## __Amazon Omics__
  - ### Features
    - This release adds support for resource policy based cross account S3 access to sequence store read sets.

## __Amazon QuickSight__
  - ### Features
    - This release includes: Update APIs to support Image, Layer Map, font customization, and Plugin Visual. Add Identity center related information in ListNamsespace API. Update API for restrictedFolder support in topics and add API for SearchTopics, Describe/Update DashboardsQA Configration.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds APIs for new features for SageMaker endpoint to scale down to zero instances, native support for multi-adapter inference, and endpoint scaling improvements.

## __Amazon Simple Email Service__
  - ### Features
    - This release adds support for starting email contacts in your Amazon Connect instance as an email receiving action.

## __Amazon Simple Notification Service__
  - ### Features
    - ArchivePolicy attribute added to Archive and Replay feature

## __Amazon WorkSpaces__
  - ### Features
    - While integrating WSP-DCV rebrand, a few mentions were erroneously renamed from WSP to DCV. This release reverts those mentions back to WSP.

## __Auto Scaling__
  - ### Features
    - Now, Amazon EC2 Auto Scaling customers can enable target tracking policies to take quicker scaling decisions, enhancing their application performance and EC2 utilization. To get started, specify target tracking to monitor a metric that is available on Amazon CloudWatch at seconds-level interval.

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for advertising trusted CA certificate names in associated trust stores.

## __Inspector2__
  - ### Features
    - Extend inspector2 service model to include ServiceQuotaExceededException.

## __MailManager__
  - ### Features
    - Added new "DeliverToQBusiness" rule action to MailManager RulesSet for ingesting email data into Amazon Q Business customer applications

# __2.29.19__ __2024-11-21__
## __AWS AppSync__
  - ### Features
    - Add support for the Amazon Bedrock Runtime.

## __AWS CloudTrail__
  - ### Features
    - This release introduces new APIs for creating and managing CloudTrail Lake dashboards. It also adds support for resource-based policies on CloudTrail EventDataStore and Dashboard resource.

## __AWS Cost Explorer Service__
  - ### Features
    - This release introduces three new APIs that enable you to estimate the cost, coverage, and utilization impact of Savings Plans you plan to purchase. The three APIs are StartCommitmentPurchaseAnalysis, GetCommitmentPurchaseAnalysis, and ListCommitmentPurchaseAnalyses.

## __AWS Health APIs and Notifications__
  - ### Features
    - Adds metadata property to an AffectedEntity.

## __AWS IoT__
  - ### Features
    - General Availability (GA) release of AWS IoT Device Management - Commands, to trigger light-weight remote actions on targeted devices

## __AWS IoT FleetWise__
  - ### Features
    - AWS IoT FleetWise now includes campaign parameters to store and forward data, configure MQTT topic as a data destination, and collect diagnostic trouble code data. It includes APIs for network agnostic data collection using custom decoding interfaces, and monitoring the last known state of vehicles.

## __AWS IoT Jobs Data Plane__
  - ### Features
    - General Availability (GA) release of AWS IoT Device Management - Commands, to trigger light-weight remote actions on targeted devices

## __AWS Lambda__
  - ### Features
    - Adds support for metrics for event source mappings for AWS Lambda

## __AWS Resilience Hub__
  - ### Features
    - AWS Resilience Hub's new summary view visually represents applications' resilience through charts, enabling efficient resilience management. It provides a consolidated view of the app portfolio's resilience state and allows data export for custom stakeholder reporting.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Systems Manager QuickSetup__
  - ### Features
    - Add methods that retrieve details about deployed configurations: ListConfigurations, GetConfiguration

## __AWS User Notifications__
  - ### Features
    - This release adds support for AWS User Notifications. You can now configure and view notifications from AWS services in a central location using the AWS SDK.

## __AWS User Notifications Contacts__
  - ### Features
    - This release adds support for AWS User Notifications Contacts. You can now configure and view email contacts for AWS User Notifications using the AWS SDK.

## __AWS X-Ray__
  - ### Features
    - AWS X-Ray introduces Transaction Search APIs, enabling span ingestion into CloudWatch Logs for high-scale trace data indexing. These APIs support span-level queries, trace graph generation, and metric correlation for deeper application insights.

## __Amazon API Gateway__
  - ### Features
    - Added support for custom domain names for private APIs.

## __Amazon CloudFront__
  - ### Features
    - Adds support for Origin Selection between EMPv2 origins based on media quality score.

## __Amazon CloudWatch Logs__
  - ### Features
    - Adds "Create field indexes to improve query performance and reduce scan volume" and "Transform logs during ingestion". Updates documentation for "PutLogEvents with Entity".

## __Amazon ElastiCache__
  - ### Features
    - Added support to modify the engine type for existing ElastiCache Users and User Groups. Customers can now modify the engine type from redis to valkey.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds support for requesting future-dated Capacity Reservations with a minimum commitment duration, enabling IPAM for organizational units within AWS Organizations, reserving EC2 Capacity Blocks that start in 30 minutes, and extending the end date of existing Capacity Blocks.

## __Amazon Simple Storage Service__
  - ### Features
    - Add support for conditional deletes for the S3 DeleteObject and DeleteObjects APIs. Add support for write offset bytes option used to append to objects with the S3 PutObject API.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Added support for providing high-level overviews of managed nodes and previewing the potential impact of a runbook execution.

## __Application Auto Scaling__
  - ### Features
    - Application Auto Scaling now supports Predictive Scaling to proactively increase the desired capacity ahead of predicted demand, ensuring improved availability and responsiveness for customers' applications. This feature is currently only made available for Amazon ECS Service scalable targets.

## __Elastic Load Balancing__
  - ### Features
    - This feature adds support for enabling zonal shift on cross-zone enabled Application Load Balancer, as well as modifying HTTP request and response headers.

# __2.29.18__ __2024-11-20__
## __AWS Application Discovery Service__
  - ### Features
    - Add support to import data from commercially available discovery tools without file manipulation.

## __AWS Compute Optimizer__
  - ### Features
    - This release enables AWS Compute Optimizer to analyze and generate optimization recommendations for Amazon Aurora database instances. It also enables Compute Optimizer to identify idle Amazon EC2 instances, Amazon EBS volumes, Amazon ECS services running on Fargate, and Amazon RDS databases.

## __AWS Control Tower__
  - ### Features
    - Adds support for child enabled baselines which allow you to see the enabled baseline status for individual accounts.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds the ability to reconfigure concurrent job settings for existing queues and create queues with custom concurrent job settings.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - MediaPackage v2 now supports the Media Quality Confidence Score (MQCS) published from MediaLive. Customers can control input switching based on the MQCS and publishing HTTP Headers for the MQCS via the API.

## __AWS Lambda__
  - ### Features
    - Add Node 22.x (node22.x) support to AWS Lambda

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - Releasing new Prompt Optimization to enhance your prompts for improved performance

## __Amazon CloudFront__
  - ### Features
    - Add support for gRPC, VPC origins, and Anycast IP Lists. Allow LoggingConfig IncludeCookies to be set regardless of whether the LoggingConfig is enabled.

## __Amazon DataZone__
  - ### Features
    - This release supports Metadata Enforcement Rule feature for Create Subscription Request action.

## __Amazon EC2 Container Service__
  - ### Features
    - This release adds support for the Availability Zone rebalancing feature on Amazon ECS.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - With this release, customers can express their desire to launch instances only in an ODCR or ODCR group rather than OnDemand capacity. Customers can express their baseline instances' CPU-performance in attribute-based Instance Requirements configuration by referencing an instance family.

## __Amazon Omics__
  - ### Features
    - Enabling call caching feature that allows customers to reuse previously computed results from a set of completed tasks in a new workflow run.

## __Amazon Recycle Bin__
  - ### Features
    - This release adds support for exclusion tags for Recycle Bin, which allows you to identify resources that are to be excluded, or ignored, by a Region-level retention rule.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for scale storage on the DB instance using a Blue/Green Deployment.

## __Amazon Timestream Query__
  - ### Features
    - This release adds support for Provisioning Timestream Compute Units (TCUs), a new feature that allows provisioning dedicated compute resources for your queries, providing predictable and cost-effective query performance.

## __Amazon WorkSpaces__
  - ### Features
    - Added support for Rocky Linux 8 on Amazon WorkSpaces Personal.

## __Amazon WorkSpaces Web__
  - ### Features
    - Added data protection settings with support for inline data redaction.

## __Auto Scaling__
  - ### Features
    - With this release, customers can prioritize launching instances into ODCRs using targets from ASGs or Launch Templates. Customers can express their baseline instances' CPU-performance in attribute-based Instance Requirements configuration by referencing an instance family that meets their needs.

## __Cost Optimization Hub__
  - ### Features
    - This release adds action type "Delete" to the GetRecommendation, ListRecommendations and ListRecommendationSummaries APIs to support new EBS and ECS recommendations with action type "Delete".

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for configuring Load balancer Capacity Unit reservations

# __2.29.17__ __2024-11-19__
## __AWS B2B Data Interchange__
  - ### Features
    - Add new X12 transactions sets and versions

## __AWS Glue__
  - ### Features
    - AWS Glue Data Catalog now enhances managed table optimizations of Apache Iceberg tables that can be accessed only from a specific Amazon Virtual Private Cloud (VPC) environment.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon EC2 Container Service__
  - ### Features
    - This release introduces support for configuring the version consistency feature for individual containers defined within a task definition. The configuration allows to specify whether ECS should resolve the container image tag specified in the container definition to an image digest.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds VPC Block Public Access (VPC BPA), a new declarative control which blocks resources in VPCs and subnets that you own in a Region from reaching or being reached from the internet through internet gateways and egress-only internet gateways.

## __Amazon Elastic File System__
  - ### Features
    - Add support for the new parameters in EFS replication APIs

## __Amazon Keyspaces__
  - ### Features
    - Amazon Keyspaces Multi-Region Replication: Adds support to add new regions to multi and single-region keyspaces.

## __Amazon WorkSpaces__
  - ### Features
    - Releasing new ErrorCodes for Image Validation failure during CreateWorkspaceImage process

## __AmazonMWAA__
  - ### Features
    - Amazon MWAA now supports a new environment class, mw1.micro, ideal for workloads requiring fewer resources than mw1.small. This class supports a single instance of each Airflow component: Scheduler, Worker, and Webserver.

## __Tax Settings__
  - ### Features
    - Release Tax Inheritance APIs, Tax Exemption APIs, and functionality update for some existing Tax Registration APIs

# __2.29.16__ __2024-11-18__
## __AWS CloudFormation__
  - ### Features
    - This release adds a new API, ListHookResults, that allows retrieving CloudFormation Hooks invocation results for hooks invoked during a create change set operation or Cloud Control API operation

## __AWS IoT SiteWise__
  - ### Features
    - The release introduces a generative AI Assistant in AWS IoT SiteWise. It includes: 1) InvokeAssistant API - Invoke the Assistant to get alarm summaries and ask questions. 2) Dataset APIs - Manage knowledge base configuration for the Assistant. 3) Portal APIs enhancement - Manage AI-aware dashboards.

## __AWS RDS DataService__
  - ### Features
    - Add support for the automatic pause/resume feature of Aurora Serverless v2.

## __Amazon AppConfig__
  - ### Features
    - AWS AppConfig has added a new extension action point, AT_DEPLOYMENT_TICK, to support third-party monitors to trigger an automatic rollback during a deployment.

## __Amazon Connect Customer Profiles__
  - ### Features
    - This release introduces Segmentation APIs and new Calculated Attribute Event Filters as part of Amazon Connect Customer Profiles service.

## __Amazon Connect Service__
  - ### Features
    - Adds CreateContactFlowVersion and ListContactFlowVersions APIs to create and view the versions of a contact flow.

## __Amazon EC2 Container Service__
  - ### Features
    - This release adds support for adding VPC Lattice configurations in ECS CreateService/UpdateService APIs. The configuration allows for associating VPC Lattice target groups with ECS Services.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adding request and response elements for managed resources.

## __Amazon Q Connect__
  - ### Features
    - This release introduces MessageTemplate as a resource in Amazon Q in Connect, along with APIs to create, read, search, update, and delete MessageTemplate resources.

## __Amazon Relational Database Service__
  - ### Features
    - Add support for the automatic pause/resume feature of Aurora Serverless v2.

## __Auto Scaling__
  - ### Features
    - Amazon EC2 Auto Scaling now supports Amazon Application Recovery Controller (ARC) zonal shift and zonal autoshift to help you quickly recover an impaired application from failures in an Availability Zone (AZ).

# __2.29.15__ __2024-11-15__
## __AWS DataSync__
  - ### Features
    - Doc-only updates and enhancements related to creating DataSync tasks and describing task executions.

## __AWS IoT__
  - ### Features
    - This release allows AWS IoT Core users to enrich MQTT messages with propagating attributes, to associate a thing to a connection, and to enable Online Certificate Status Protocol (OCSP) stapling for TLS X.509 server certificates through private endpoints.

## __AWS Outposts__
  - ### Features
    - You can now purchase AWS Outposts rack or server capacity for a 5-year term with one of the following payment options: All Upfront, Partial Upfront, and No Upfront.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch__
  - ### Features
    - Adds support for adding related Entity information to metrics ingested through PutMetricData.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Remove non-functional enum variants for FleetCapacityReservationUsageStrategy

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - Use rule overrides to always allow or always block messages to specific phone numbers. Use message feedback to monitor if a customer interacts with your message.

## __Amazon Polly__
  - ### Features
    - Fixes PutLexicon usage example.

## __Amazon Route 53 Resolver__
  - ### Features
    - Route 53 Resolver DNS Firewall Advanced Rules allows you to monitor and block suspicious DNS traffic based on anomalies detected in the queries, such as DNS tunneling and Domain Generation Algorithms (DGAs).

## __AmazonConnectCampaignServiceV2__
  - ### Features
    - Added Amazon Connect Outbound Campaigns V2 SDK.

# __2.29.14__ __2024-11-14__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Partner Central Selling API__
  - ### Features
    - Announcing AWS Partner Central API for Selling: This service launch Introduces new APIs for co-selling opportunity management and related functions. Key features include notifications, a dynamic sandbox for testing, and streamlined validations.

# __2.29.13__ __2024-11-14__
## __AWS Cloud Control API__
  - ### Features
    - Added support for CloudFormation Hooks with Cloud Control API. The GetResourceRequestStatus API response now includes an optional HooksProgressEvent and HooksRequestToken parameter for Hooks Invocation Progress as part of resource operation with Cloud Control.

## __AWS Identity and Access Management__
  - ### Features
    - This release includes support for five new APIs and changes to existing APIs that give AWS Organizations customers the ability to use temporary root credentials, targeted to member accounts in the organization.

## __AWS IoT Wireless__
  - ### Features
    - New FuotaTask resource type to enable logging for your FUOTA tasks. A ParticipatingGatewaysforMulticast parameter to choose the list of gateways to receive the multicast downlink message and the transmission interval between them. Descriptor field which will be sent to devices during FUOTA transfer.

## __AWS License Manager User Subscriptions__
  - ### Features
    - New and updated API operations to support License Included User-based Subscription of Microsoft Remote Desktop Services (RDS).

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Security Token Service__
  - ### Features
    - This release introduces the new API 'AssumeRoot', which returns short-term credentials that you can use to perform privileged tasks.

## __AWSDeadlineCloud__
  - ### Features
    - Adds support for select GPU accelerated instance types when creating new service-managed fleets.

## __Access Analyzer__
  - ### Features
    - Expand analyzer configuration capabilities for unused access analyzers. Unused access analyzer configurations now support the ability to exclude accounts and resource tags from analysis providing more granular control over the scope of analysis.

## __Amazon Interactive Video Service__
  - ### Features
    - IVS now offers customers the ability to stream multitrack video to Channels.

## __Amazon QuickSight__
  - ### Features
    - This release adds APIs for Custom Permissions management in QuickSight, and APIs to support QuickSight Branding.

## __Amazon Redshift__
  - ### Features
    - Adds support for Amazon Redshift S3AccessGrants

## __Amazon SageMaker Service__
  - ### Features
    - Add support for Neuron instance types [ trn1/trn1n/inf2 ] on SageMaker Notebook Instances Platform.

## __Amazon Simple Storage Service__
  - ### Features
    - This release updates the ListBuckets API Reference documentation in support of the new 10,000 general purpose bucket default quota on all AWS accounts. To increase your bucket quota from 10,000 to up to 1 million buckets, simply request a quota increase via Service Quotas.

## __Netty NIO HTTP Client__
  - ### Features
    - Update Netty version to `4.1.115.Final`.

## __Partner Central Selling API__
  - ### Features
    - Announcing AWS Partner Central API for Selling: This service launch Introduces new APIs for co-selling opportunity management and related functions. Key features include notifications, a dynamic sandbox for testing, and streamlined validations.

# __2.29.12__ __2024-11-13__
## __AWS B2B Data Interchange__
  - ### Features
    - This release adds a GenerateMapping API to allow generation of JSONata or XSLT transformer code based on input and output samples.

## __AWS Billing__
  - ### Features
    - Today, AWS announces the general availability of ListBillingViews API in the AWS SDKs, to enable AWS Billing Conductor (ABC) users to create proforma Cost and Usage Reports (CUR) programmatically.

## __AWS CloudTrail__
  - ### Features
    - This release adds a new API GenerateQuery that generates a query from a natural language prompt about the event data in your event data store. This operation uses generative artificial intelligence (generative AI) to produce a ready-to-use SQL query from the prompt.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds support for ARN inputs in the Kantar credentials secrets name field and the MSPR field to the manifests for PlayReady DRM protected outputs.

## __AWS Organizations__
  - ### Features
    - Add support for policy operations on the Resource Control Polices.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Access Analyzer__
  - ### Features
    - This release adds support for policy validation and external access findings for resource control policies (RCP). IAM Access Analyzer helps you author functional and secure RCPs and awareness that a RCP may restrict external access. Updated service API, documentation, and paginators.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - Amazon CloudWatch Application Signals now supports creating Service Level Objectives with burn rates. Users can now create or update SLOs with burn rate configurations to meet their specific business requirements.

## __Amazon CloudWatch Internet Monitor__
  - ### Features
    - Add new query type Routing_Suggestions regarding querying interface

## __Amazon DynamoDB__
  - ### Features
    - This release includes supports the new WarmThroughput feature for DynamoDB. You can now provide an optional WarmThroughput attribute for CreateTable or UpdateTable APIs to pre-warm your table or global secondary index. You can also use DescribeTable to see the latest WarmThroughput value.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds the source AMI details in DescribeImages API

# __2.29.11__ __2024-11-12__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports non-containerized Linux and Windows builds on Reserved Capacity.

## __AWS Control Tower__
  - ### Features
    - Added ResetEnabledControl API.

## __AWS Fault Injection Simulator__
  - ### Features
    - This release adds support for generating experiment reports with the experiment report configuration

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift releases container fleets support for general availability. Deploy Linux-based containerized game server software for hosting on Amazon GameLift.

## __Amazon Relational Database Service__
  - ### Features
    - Updates Amazon RDS documentation for Amazon RDS Extended Support for Amazon Aurora MySQL.

## __Payment Cryptography Control Plane__
  - ### Features
    - Updated ListAliases API with KeyArn filter.

# __2.29.10__ __2024-11-11__
## __AWS Lambda__
  - ### Features
    - Add Python 3.13 (python3.13) support to AWS Lambda

## __AWS Outposts__
  - ### Features
    - This release updates StartCapacityTask to allow an active Outpost to be modified. It also adds a new API to list all running EC2 instances on the Outpost.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - SigV4: Add host header only when not already provided
        - Contributed by: [@vsudilov](https://github.com/vsudilov)

## __Amazon CloudFront__
  - ### Features
    - No API changes from previous release. This release migrated the model to Smithy keeping all features unchanged.

## __Amazon OpenSearch Service__
  - ### Features
    - Adds Support for new AssociatePackages and DissociatePackages API in Amazon OpenSearch Service that allows association and dissociation operations to be carried out on multiple packages at the same time.

## __Inspector2__
  - ### Features
    - Adds support for filePath filter.

## __Contributors__
Special thanks to the following contributors to this release: 

[@vsudilov](https://github.com/vsudilov)
# __2.29.9__ __2024-11-08__
## __AWS Batch__
  - ### Features
    - This feature allows override LaunchTemplates to be specified in an AWS Batch Compute Environment.

## __AWS Control Catalog__
  - ### Features
    - AWS Control Catalog GetControl public API returns additional data in output, including Implementation and Parameters

## __AWS Lambda__
  - ### Features
    - This release adds support for using AWS KMS customer managed keys to encrypt AWS Lambda .zip deployment packages.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock Runtime__
  - ### Features
    - This release adds trace functionality to Bedrock Prompt Flows

## __Amazon Chime SDK Media Pipelines__
  - ### Features
    - Added support for Media Capture Pipeline and Media Concatenation Pipeline for customer managed server side encryption. Now Media Capture Pipeline can use IAM sink role to get access to KMS key and encrypt/decrypt recorded artifacts. KMS key ID can also be supplied with encryption context.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds new error code `Ec2InstanceTypeDoesNotExist` for Amazon EKS managed node groups

## __Amazon Kinesis Firehose__
  - ### Features
    - Amazon Data Firehose / Features : Adds support for a new DeliveryStreamType, DatabaseAsSource. DatabaseAsSource hoses allow customers to stream CDC events from their RDS and Amazon EC2 hosted databases, running MySQL and PostgreSQL database engines, to Iceberg Table destinations.

## __Amazon Pinpoint SMS Voice V2__
  - ### Features
    - Added the RequiresAuthenticationTimestamp field to the RegistrationVersionStatusHistory data type.

## __QBusiness__
  - ### Features
    - Adds S3 path option to pass group member list for PutGroup API.

# __2.29.8__ __2024-11-07__
## __AWS Clean Rooms ML__
  - ### Features
    - This release introduces support for Custom Models in AWS Clean Rooms ML.

## __AWS Clean Rooms Service__
  - ### Features
    - This release introduces support for Custom Models in AWS Clean Rooms ML.

## __AWS Resource Explorer__
  - ### Features
    - Add GetManagedView, ListManagedViews APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Agents for Amazon Bedrock__
  - ### Features
    - Add prompt support for chat template configuration and agent generative AI resource. Add support for configuring an optional guardrail in Prompt and Knowledge Base nodes in Prompt Flows. Add API to validate flow definition

## __Amazon Bedrock Runtime__
  - ### Features
    - Add Prompt management support to Bedrock runtime APIs: Converse, ConverseStream, InvokeModel, InvokeModelWithStreamingResponse

## __Amazon QuickSight__
  - ### Features
    - Add Client Credentials based OAuth support for Snowflake and Starburst

## __Auto Scaling__
  - ### Features
    - Auto Scaling groups now support the ability to strictly balance instances across Availability Zones by configuring the AvailabilityZoneDistribution parameter. If balanced-only is configured for a group, launches will always be attempted in the under scaled Availability Zone even if it is unhealthy.

## __Synthetics__
  - ### Features
    - Add support to toggle if a canary will automatically delete provisioned canary resources such as Lambda functions and layers when a canary is deleted. This behavior can be controlled via the new ProvisionedResourceCleanup property exposed in the CreateCanary and UpdateCanary APIs.

# __2.29.7__ __2024-11-06__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now adds additional compute types for reserved capacity fleet.

## __AWS Lake Formation__
  - ### Features
    - API changes for new named tag expressions feature.

## __AWS S3 Control__
  - ### Features
    - Fix ListStorageLensConfigurations and ListStorageLensGroups deserialization for Smithy SDKs.

## __AWS SDK for Java v2__
  - ### Features
    - Improve unmarshalling performance of all JSON protocols by unifying parsing with unmarshalling instead of doing one after the other.

  - ### Bugfixes
    - Moves setting the default backoff strategies for all retry strategies to the builder as per the javadocs instead of only doing it in the `DefaultRetryStrategy` builder methods.

## __Amazon GuardDuty__
  - ### Features
    - GuardDuty RDS Protection expands support for Amazon Aurora PostgreSQL Limitless Databases.

## __Amazon Verified Permissions__
  - ### Features
    - Adding BatchGetPolicy API which supports the retrieval of multiple policies across multiple policy stores within a single request.

## __QApps__
  - ### Features
    - Introduces category apis in AmazonQApps. Web experience users use Categories to tag and filter library items.

# __2.29.6__ __2024-11-01__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Load Checksum classes of CRT from Classloader instead of direct references of CRT classes.

## __Agents for Amazon Bedrock__
  - ### Features
    - Amazon Bedrock Knowledge Bases now supports using application inference profiles to increase throughput and improve resilience.

## __Amazon CloudWatch Logs__
  - ### Features
    - This release introduces an improvement in PutLogEvents

## __Amazon DocumentDB Elastic Clusters__
  - ### Features
    - Amazon DocumentDB Elastic Clusters adds support for pending maintenance actions feature with APIs GetPendingMaintenanceAction, ListPendingMaintenanceActions and ApplyPendingMaintenanceAction

## __Tax Settings__
  - ### Features
    - Add support for supplemental tax registrations via these new APIs: PutSupplementalTaxRegistration, ListSupplementalTaxRegistrations, and DeleteSupplementalTaxRegistration.

# __2.29.5__ __2024-10-31__
## __AWS Batch__
  - ### Features
    - Add `podNamespace` to `EksAttemptDetail` and `containerID` to `EksAttemptContainerDetail`.

## __AWS Glue__
  - ### Features
    - Add schedule support for AWS Glue column statistics

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix an issue where the SDK does not properly unmarshall an evenstream exception to the expected exception type.

## __Amazon Prometheus Service__
  - ### Features
    - Added support for UpdateScraper API, to enable updating collector configuration in-place

## __Amazon SageMaker Service__
  - ### Features
    - SageMaker HyperPod adds scale-down at instance level via BatchDeleteClusterNodes API and group level via UpdateCluster API. SageMaker Training exposes secondary job status in TrainingJobSummary from ListTrainingJobs API. SageMaker now supports G6, G6e, P5e instances for HyperPod and Training.

## __Amazon Simple Email Service__
  - ### Features
    - This release enables customers to provide the email template content in the SESv2 SendEmail and SendBulkEmail APIs instead of the name or the ARN of a stored email template.

## __Auto Scaling__
  - ### Features
    - Adds bake time for Auto Scaling group Instance Refresh

## __Elastic Load Balancing__
  - ### Features
    - Add UDP support for AWS PrivateLink and dual-stack Network Load Balancers

# __2.29.4__ __2024-10-30__
## __AWS AppSync__
  - ### Features
    - This release adds support for AppSync Event APIs.

## __AWS DataSync__
  - ### Features
    - AWS DataSync now supports Enhanced mode tasks. This task mode supports transfer of virtually unlimited numbers of objects with enhanced metrics, more detailed logs, and higher performance than Basic mode. This mode currently supports transfers between Amazon S3 locations.

## __AWS Network Firewall__
  - ### Features
    - AWS Network Firewall now supports configuring TCP idle timeout

## __AWS SDK for Java v2__
  - ### Features
    - Adds support for tracking feature usage in a new user agent metadata section and adds a base set of features. Where features were already a part of the user agent string, they are now converted to the new format where a feature is represented as a Base64 encoded string. For example, using DynamoDb Enhanced Client was previously recorded as 'hll/ddb-enh' in the user agent, but is now a 'd' in the business metrics metadata section 'm/'.
    - Updated endpoint and partition metadata.

## __Amazon Connect Service__
  - ### Features
    - Updated the public documentation for the UserIdentityInfo object to accurately reflect the character limits for the FirstName and LastName fields, which were previously listed as 1-100 characters.

## __Amazon EC2 Container Service__
  - ### Features
    - This release supports service deployments and service revisions which provide a comprehensive view of your Amazon ECS service history.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds two new capabilities to VPC Security Groups: Security Group VPC Associations and Shared Security Groups.

## __Amazon Keyspaces__
  - ### Features
    - Adds support for interacting with user-defined types (UDTs) through the following new operations: Create-Type, Delete-Type, List-Types, Get-Type.

## __Amazon Location Service Maps V2__
  - ### Features
    - Release of Amazon Location Maps API. Maps enables you to build digital maps that showcase your locations, visualize your data, and unlock insights to drive your business

## __Amazon Location Service Places V2__
  - ### Features
    - Release of Amazon Location Places API. Places enables you to quickly search, display, and filter places, businesses, and locations based on proximity, category, and name

## __Amazon Location Service Routes V2__
  - ### Features
    - Release of Amazon Location Routes API. Routes enables you to plan efficient routes and streamline deliveries by leveraging real-time traffic, vehicle restrictions, and turn-by-turn directions.

## __Amazon OpenSearch Service__
  - ### Features
    - This release introduces the new OpenSearch user interface (Dashboards), a new web-based application that can be associated with multiple data sources across OpenSearch managed clusters, serverless collections, and Amazon S3, so that users can gain a comprehensive insights in an unified interface.

## __Amazon Redshift__
  - ### Features
    - This release launches S3 event integrations to create and manage integrations from an Amazon S3 source into an Amazon Redshift database.

## __Amazon Route 53__
  - ### Features
    - This release adds support for TLSA, SSHFP, SVCB, and HTTPS record types.

## __Amazon SageMaker Service__
  - ### Features
    - Added support for Model Registry Staging construct. Users can define series of stages that models can progress through for model workflows and lifecycle. This simplifies tracking and managing models as they transition through development, testing, and production stages.

## __Amazon WorkMail__
  - ### Features
    - This release adds support for Multi-Factor Authentication (MFA) and Personal Access Tokens through integration with AWS IAM Identity Center.

## __OpenSearch Service Serverless__
  - ### Features
    - Neo Integration via IAM Identity Center (IdC)

## __Redshift Serverless__
  - ### Features
    - Adds and updates API members for the Redshift Serverless AI-driven scaling and optimization feature using the price-performance target setting.

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


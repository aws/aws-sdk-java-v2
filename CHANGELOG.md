# __2.10.68__ __2020-02-19__
## __AWS Lambda__
  - ### Features
    - AWS Lambda now supports Ruby 2.7

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __AWS Service Catalog__
  - ### Features
    - "ListPortfolioAccess" API now has a new optional parameter "OrganizationParentId". When it is provided and if the portfolio with the "PortfolioId" given was shared with an organization or organizational unit with "OrganizationParentId", all accounts in the organization sub-tree under parent which inherit an organizational portfolio share will be listed, rather than all accounts with external shares. To accommodate long lists returned from the new option, the API now supports pagination.

## __Auto Scaling__
  - ### Features
    - Doc update for EC2 Auto Scaling: Add Enabled parameter for PutScalingPolicy

# __2.10.67__ __2020-02-18__
## __Amazon Chime__
  - ### Features
    - Added AudioFallbackUrl to support Chime SDK client.

## __Amazon Relational Database Service__
  - ### Features
    - This release supports Microsoft Active Directory authentication for Amazon Aurora.

## __Auto Scaling__
  - ### Features
    - Amazon EC2 Auto Scaling now supports the ability to enable/disable target tracking, step scaling, and simple scaling policies.

# __2.10.66__ __2020-02-17__
## __AWS Cloud9__
  - ### Features
    - AWS Cloud9 now supports the ability to tag Cloud9 development environments.

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __Amazon DynamoDB__
  - ### Features
    - Amazon DynamoDB enables you to restore your DynamoDB backup or table data across AWS Regions such that the restored table is created in a different AWS Region from where the source table or backup resides. You can do cross-region restores between AWS commercial Regions, AWS China Regions, and AWS GovCloud (US) Regions.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for EC2

## __Amazon Rekognition__
  - ### Features
    - This update adds the ability to detect text in videos and adds filters to image and video text detection.

# __2.10.65__ __2020-02-14__
## __AWS MediaTailor__
  - ### Features
    - AWS Elemental MediaTailor SDK now allows configuration of Personalization Threshold for HLS and DASH streams.

## __AWS SecurityHub__
  - ### Features
    - Security Hub has released a new DescribeStandards API action. This API action allows a customer to list all of the standards available in an account. For each standard, the list provides the customer with the standard name, description, and ARN. Customers can use the ARN as an input to the BatchEnableStandards API action. To learn more, visit our API documentation.

## __AWS Shield__
  - ### Features
    - This release adds support for associating Amazon Route 53 health checks to AWS Shield Advanced protected resources.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - You can now enable Multi-Attach on Provisioned IOPS io1 volumes through the create-volume API.

## __Amazon S3__
  - ### Features
    - Added support for presigning `CreateMultipartUpload`, `UploadPart`, `CompleteMultipartUpload`, and `AbortMultipartUpload` requests.

# __2.10.64__ __2020-02-13__
## __AWS Elemental MediaPackage VOD__
  - ### Features
    - Adds support for DASH with multiple media presentation description periods triggered by presence of SCTE-35 ad markers in the manifest.Also adds optional configuration for DASH SegmentTemplateFormat to refer to segments by Number with Duration, Number with Timeline or Time with Timeline and compact the manifest by combining duplicate SegmentTemplate tags.

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __Amazon DynamoDB Enhanced Client [Preview]__
  - ### Features
    - Improves discoverability by adding operation methods for deleteItem(), getItem(), putItem and updateItem(), as applicable. These methods take a request object as parameter. Execute() methods for the table interface is removed since they are no longer needed.

## __Netty NIO HTTP Client__
  - ### Features
    - When there is an I/O error on an http2 request, the SDK will start shutting down the connection - stopping using the http2 connection for new requests and closing it after all streams are finished.

# __2.10.63__ __2020-02-12__
## __AWS Directory Service__
  - ### Features
    - Release to add the ExpirationDateTime as an output to ListCertificates so as to ease customers to look into their certificate lifetime and make timely decisions about renewing them.

## __AWS Glue__
  - ### Features
    - Adding ability to add arguments that cannot be overridden to AWS Glue jobs

## __Amazon Chime__
  - ### Features
    - Documentation updates for Amazon Chime

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for tagging public IPv4 pools.

## __Amazon Elasticsearch Service__
  - ### Features
    - Amazon Elasticsearch Service now offers fine-grained access control, which adds multiple capabilities to give tighter control over data. New features include the ability to use roles to define granular permissions for indices, documents, or fields and to extend Kibana with read-only views and secure multi-tenant support.

## __Amazon Neptune__
  - ### Features
    - This launch enables Neptune start-db-cluster and stop-db-cluster. Stopping and starting Amazon Neptune clusters helps you manage costs for development and test environments. You can temporarily stop all the DB instances in your cluster, instead of setting up and tearing down all the DB instances each time that you use the cluster.

## __Amazon WorkMail__
  - ### Features
    - This release adds support for access control rules management in Amazon WorkMail.

# __2.10.62__ __2020-02-11__
## __AWS CloudFormation__
  - ### Features
    - This release of AWS CloudFormation StackSets allows you to centrally manage deployments to all the accounts in your organization or specific organizational units (OUs) in AWS Organizations. You will also be able to enable automatic deployments to any new accounts added to your organization or OUs. The permissions needed to deploy across accounts will automatically be taken care of by the StackSets service.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Features:This release adds a new setting for a user pool to allow if customer wants their user signup/signin with case insensitive username. The current default setting is case sensitive, and for our next release we will change it to case insensitive.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 Now Supports Tagging Spot Fleet.

# __2.10.61__ __2020-02-10__
## __AWS Key Management Service__
  - ### Features
    - The ConnectCustomKeyStore API now provides a new error code (SUBNET_NOT_FOUND) for customers to better troubleshoot if their "connect-custom-key-store" operation fails.

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __Amazon DocumentDB with MongoDB compatibility__
  - ### Features
    - Added clarifying information that Amazon DocumentDB shares operational technology with Amazon RDS and Amazon Neptune.

# __2.10.60__ __2020-02-07__
## __AWS RoboMaker__
  - ### Features
    - This release adds support for simulation job batches

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __Amazon DynamoDB Enhanced Client [Preview]__
  - ### Features
    - Improves discoverability by renaming the table and index interfaces to be consistent with the client interface naming, and by adding operation methods for createTable(), scan() and query(), as applicable. These methods take a request object as parameter. Execute() methods for the index interface is removed since they are no longer needed.

## __Amazon Relational Database Service__
  - ### Features
    - Documentation updates for RDS: when restoring a DB cluster from a snapshot, must create DB instances

## __EC2 Image Builder__
  - ### Features
    - This version of the SDK includes bug fixes and documentation updates.

# __2.10.59__ __2020-02-06__
## __AWS AppSync__
  - ### Features
    - AWS AppSync now supports X-Ray

## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild adds support for Amazon Elastic File Systems

## __Amazon DynamoDB Enhanced Client [Preview]__
  - ### Features
    - In order to make operations more easily discoverable by an IDE, specific operation methods have been added to the enhanced client interface. An operation method takes a corresponding request object as parameter. Meanwhile, the generic execute() method is removed. This change affects only batch and transcribe operations at the database level.

## __Amazon EC2 Container Registry__
  - ### Features
    - This release contains updated text for the GetAuthorizationToken API.

## __Amazon Elastic Block Store__
  - ### Features
    - Documentation updates for EBS direct APIs.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds platform details and billing info to the DescribeImages API.

## __Amazon Lex Model Building Service__
  - ### Features
    - Amazon Lex now supports AMAZON.AlphaNumeric with regular expressions.

# __2.10.58__ __2020-02-05__
## __AWS Elemental MediaConvert__
  - ### Features
    - AWS Elemental MediaConvert SDK has added support for fine-tuned QVBR quality level.

## __AWS Ground Station__
  - ### Features
    - Adds dataflowEndpointRegion property to DataflowEndpointConfig. The dateCreated, lastUpdated, and tags properties on GetSatellite have been deprecated.

## __AWS Resource Groups Tagging API__
  - ### Features
    - Documentation-only update that adds services to the list of supported services.

## __AWS SecurityHub__
  - ### Features
    - Additional resource types are now supported in the AWS Security Finding Format (ASFF). The following new resource types are added, each having an accompanying resource details object with fields for security finding providers to populate: AwsCodeBuildProject, AwsEc2NetworkInterface, AwsEc2SecurityGroup, AwsElasticsearchDomain, AwsLambdaLayerVersion, AwsRdsDbInstance, and AwsWafWebAcl. The following resource types are added without an accompanying details object: AutoscalingAutoscalingGroup, AwsDynamoDbTable, AwsEc2Eip, AwsEc2Snapshot, AwsEc2Volume, AwsRdsDbSnapshot, AwsRedshiftCluster, and AwsS3Object. The number of allowed resources per finding is increased from 10 to 32. A new field is added in the Compliance object, RelatedRequirements. To learn more, visit our documentation on the ASFF.

## __Amazon Data Lifecycle Manager__
  - ### Features
    - Updated the maximum number of tags that can be added to a snapshot using DLM to 45.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release provides support for tagging when you create a VPC endpoint, or VPC endpoint service.

## __Amazon Forecast Query Service__
  - ### Features
    - Documentation updates for Amazon Forecast.

# __2.10.57__ __2020-02-04__
## __AWS IoT__
  - ### Features
    - Updated ThrottlingException documentation to report that the error code is 400, and not 429, to reflect actual system behaviour.

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __AWS Storage Gateway__
  - ### Features
    - Adding KVM as a support hypervisor

## __Amazon CloudFront__
  - ### Features
    - Documentation updates for CloudFront

## __Amazon DynamoDB Enhanced Client [Preview]__
  - ### Features
    - Changing usage of typed builders for PutItem, UpdateItem and StaticTableSchema to explicitly provide class type.
    - Renames top level sync/async MappedDatabase interfaces as DynamoDbEnhancedClient interfaces. Also adds builder definitions to the interfaces together with a static method that returns the default implementation of the builder.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon VPC Flow Logs adds support for 1-minute aggregation intervals.

## __Amazon S3__
  - ### Bugfixes
    - Fixed an issue where fields in `ListObjectVersionsResponse` and `ListMultipartUploadsResponse` are not decoded correctly when encodingType is specified as url. See [#1601](https://github.com/aws/aws-sdk-java-v2/issues/1601)

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This feature ensures that an instance is patched up to the available patches on a particular date. It can be enabled by selecting the 'ApproveUntilDate' option as the auto-approval rule while creating the patch baseline. ApproveUntilDate - The cutoff date for auto approval of released patches. Any patches released on or before this date will be installed automatically.

## __Amazon WorkMail__
  - ### Features
    - This release adds support for tagging Amazon WorkMail organizations.

## __Managed Streaming for Kafka__
  - ### Features
    - This release enables AWS MSK customers to list Apache Kafka versions that are supported on AWS MSK clusters. Also includes changes to expose additional details of a cluster's state in DescribeCluster and ListClusters APIs.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Deliver exceptions to stream channels correctly if there's an exception thrown on connection. This also fixes a bug where publisher signals onComplete if the stream is closed as a result of outbound GOAWAY.
    - Throws `IOException` for the race condition where an HTTP2 connection gets reused at the same time it gets inactive so that failed requests can be retried

# __2.10.56__ __2020-01-24__
## __AWS DataSync__
  - ### Features
    - AWS DataSync now supports FSx for Windows File Server Locations

## __AWS OpsWorks CM__
  - ### Features
    - AWS OpsWorks for Chef Automate now supports in-place upgrade to Chef Automate 2. Eligible servers can be updated through the management console, CLI and APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __Amazon EC2__
  - ### Features
    - Adds EC2ThrottledException as a recognized throttling exception to be retried

## __Amazon EC2 Container Service__
  - ### Features
    - This release provides support for tagging Amazon ECS task sets for services using external deployment controllers.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adding new error codes for Nodegroups in EKS

## __Amazon WorkSpaces__
  - ### Features
    - Documentation updates for WorkSpaces

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fix issue where DNS resolution for a host is only made once for the initial request to the host. If the DNS entries change for a hostname, the client will resolve the new address until the client is closed and recreated.

# __2.10.55__ __2020-01-23__
## __AWS Identity and Access Management__
  - ### Features
    - This release enables the Identity and Access Management policy simulator to simulate permissions boundary policies.

## __AWS SDK for Java v2__
  - ### Features
    - Added ServiceMetadata.servicePartitions() to get partition metadata for a specific service
    - Improved error messages on UnknownHostExceptions
    - Updated service endpoint metadata.

## __Amazon DynamoDB Enhanced Client [Preview]__
  - ### Features
    - Support for non-blocking asynchronous calling of all mapper operations

## __Amazon Relational Database Service__
  - ### Features
    - This SDK release introduces APIs that automate the export of Amazon RDS snapshot data to Amazon S3. The new APIs include: StartExportTask, CancelExportTask, DescribeExportTasks. These APIs automate the extraction of data from an RDS snapshot and export it to an Amazon S3 bucket. The data is stored in a compressed, consistent, and query-able format. After the data is exported, you can query it directly using tools such as Amazon Athena or Redshift Spectrum. You can also consume the data as part of a data lake solution. If you archive the data in S3 Infrequent Access or Glacier, you can reduce long term data storage costs by applying data lifecycle policies.

# __2.10.54__ __2020-01-21__
## __AWS Application Discovery Service__
  - ### Features
    - Documentation updates for the AWS Application Discovery Service.

## __AWS CodePipeline__
  - ### Features
    - AWS CodePipeline enables an ability to stop pipeline executions.

## __AWS IoT Events__
  - ### Features
    - Documentation updates for iotcolumbo

## __AWS Marketplace Commerce Analytics__
  - ### Features
    - Remove 4 deprecated data sets, change some data sets available dates to 2017-09-15

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add an enum value to the result of DescribeByoipCidrs to support CIDRs that are not publicly advertisable.

## __Netty NIO Http Client__
  - ### Bugfixes
    - Fixed a bug where an inactive http2 connection without `GOAWAY` frame received might get reused in a new request, causing `ClosedChannelException`

# __2.10.53__ __2020-01-20__
## __AWS Key Management Service__
  - ### Features
    - The ConnectCustomKeyStore operation now provides new error codes (USER_LOGGED_IN and USER_NOT_FOUND) for customers to better troubleshoot if their connect custom key store operation fails. Password length validation during CreateCustomKeyStore now also occurs on the client side.

## __AWS Lambda__
  - ### Features
    - Added reason codes to StateReasonCode (InvalidSubnet, InvalidSecurityGroup) and LastUpdateStatusReasonCode (SubnetOutOfIPAddresses, InvalidSubnet, InvalidSecurityGroup) for functions that connect to a VPC.

## __Alexa For Business__
  - ### Features
    - Add support for CreatedTime and ConnectionStatusUpdatedTime in response of SearchDevices API.

## __Amazon CloudWatch__
  - ### Features
    - Updating DescribeAnomalyDetectors API to return AnomalyDetector Status value in response.

## __Amazon CloudWatch Application Insights__
  - ### Features
    - This release adds support for a list API to retrieve the configuration events logged during periodic updates to an application by Amazon CloudWatch Application Insights.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release provides support for a preview of bringing your own IPv6 addresses (BYOIP for IPv6) for use in AWS.

# __2.10.52__ __2020-01-17__
## __AWS Batch__
  - ### Features
    - This release ensures INACTIVE job definitions are permanently deleted after 180 days.

## __AWS CloudHSM V2__
  - ### Features
    - This release introduces resource-level and tag-based access control for AWS CloudHSM resources. You can now tag CloudHSM backups, tag CloudHSM clusters on creation, and tag a backup as you copy it to another region.

## __AWS Elemental MediaConvert__
  - ### Features
    - AWS Elemental MediaConvert SDK has added support for MP3 audio only outputs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __Amazon EC2 Container Service__
  - ### Features
    - This release provides a public preview for specifying Amazon EFS file systems as volumes in your Amazon ECS task definitions.

## __Amazon Neptune__
  - ### Features
    - This release includes Deletion Protection for Amazon Neptune databases.

## __Amazon Redshift__
  - ### Features
    - Documentation updates for redshift

# __2.10.51__ __2020-01-16__
## __AWS Directory Service__
  - ### Features
    - To reduce the number of errors our customers are facing, we have modified the requirements of input parameters for two of Directory Service APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Client VPN now supports Port Configuration for VPN Endpoints, allowing usage of either port 443 or port 1194.

## __Amazon SageMaker Service__
  - ### Features
    - This release adds two new APIs (UpdateWorkforce and DescribeWorkforce) to SageMaker Ground Truth service for workforce IP whitelisting.

# __2.10.50__ __2020-01-15__
## __AWS Organizations__
  - ### Features
    - Updated description for PolicyID parameter and ConstraintViolationException.

## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __AWS SecurityHub__
  - ### Features
    - Add support for DescribeStandardsControls and UpdateStandardsControl. These new Security Hub API operations are used to track and manage whether a compliance standards control is enabled.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - General Update to EC2 Docs and SDKs

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Document updates for Patch Manager 'NoReboot' feature.

## __Amazon Transcribe Service__
  - ### Bugfixes
    - Fixed an issue where streaming transcriptions would fail with signature validation errors if the date changed during the request.

# __2.10.49__ __2020-01-14__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for partition placement groups and instance metadata option in Launch Templates

# __2.10.48__ __2020-01-13__
## __AWS Backup__
  - ### Features
    - Cross-region backup is a new AWS Backup feature that allows enterprises to copy backups across multiple AWS services to different regions.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for the StopInstances API. You can now stop and start an Amazon EBS-backed Spot Instance at will, instead of relying on the Stop interruption behavior to stop your Spot Instances when interrupted.

## __Amazon Elastic File System__
  - ### Features
    - This release adds support for managing EFS file system policies and EFS Access Points.

## __Amazon S3__
  - ### Bugfixes
    - Fixed bug prevent GetBucketBolicy from ever being successful using the asynchronous S3 client.

# __2.10.47__ __2020-01-10__
## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.
    - Updated service endpoints and added global endpoints for iso and iso-b.

## __AWS Transfer for SFTP__
  - ### Features
    - This release introduces a new endpoint type that allows you to attach Elastic IP addresses from your AWS account with your server's endpoint directly and whitelist access to your server by client's internet IP address(es) using VPC Security Groups.

## __Amazon Chime__
  - ### Features
    - Add shared profile support to new and existing users

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release introduces the ability to tag egress only internet gateways, local gateways, local gateway route tables, local gateway virtual interfaces, local gateway virtual interface groups, local gateway route table VPC association and local gateway route table virtual interface group association. You can use tags to organize and identify your resources for cost allocation.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds an operation that enables users to override the system-default SSL/TLS certificate for new Amazon RDS DB instances temporarily, or remove the customer override.

## __Amazon S3__
  - ### Bugfixes
    - Fix an issue where s3#listObjects incorrectly decoded marker field. See [#1574](https://github.com/aws/aws-sdk-java-v2/issues/1574).

## __Amazon SageMaker Service__
  - ### Features
    - SageMaker ListTrialComponents API filter by TrialName and ExperimentName.

## __Amazon WorkSpaces__
  - ### Features
    - Added the migrate feature to Amazon WorkSpaces.

# __2.10.46__ __2020-01-09__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Increase the priority of the AWS_WEB_IDENTITY_TOKEN_FILE/AWS_ROLE_ARN/AWS_ROLE_SESSION_NAME environment variables when loading credentials so that they are considered before web_identity_token_file/role_arn/role_session_name profile properties. This is consistent with the other AWS SDKs, including the CLI.

## __AWS Security Token Service__
  - ### Features
    - Documentation updates for sts

## __Amazon CloudWatch Logs__
  - ### Features
    - Documentation updates for logs

## __Amazon S3__
  - ### Features
    - Add support for Tagging builder in `CreateMultipartUploadRequest`. See [#1440](https://github.com/aws/aws-sdk-java-v2/issues/1440)

# __2.10.45__ __2020-01-08__
## __AWS Cost Explorer Service__
  - ### Features
    - Documentation updates for CreateCostCategoryDefinition and UpdateCostCategoryDefinition API

## __AWS Step Functions__
  - ### Features
    - Add sfn specific http configurations. See [#1325](https://github.com/aws/aws-sdk-java-v2/issues/1325)

## __Amazon EC2__
  - ### Bugfixes
    - Fix NPE when calling `CopySnapshot`. Fixes [#1564](https://github.com/aws/aws-sdk-java-v2/issues/1564)

## __Amazon Translate__
  - ### Features
    - This release adds a new family of APIs for asynchronous batch translation service that provides option to translate large collection of text or HTML documents stored in Amazon S3 folder. This service accepts a batch of up to 5 GB in size per API call with each document not exceeding 1 MB size and the number of documents not exceeding 1 million per batch. See documentation for more information.

## __Firewall Management Service__
  - ### Features
    - AWS Firewall Manager now supports tagging, and tag-based access control, of policies.

# __2.10.44__ __2020-01-07__
## __AWS CodeBuild__
  - ### Features
    - Add encryption key override to StartBuild API in AWS CodeBuild.

## __AWS Migration Hub__
  - ### Features
    - ListApplicationStates API provides a list of all application migration states

## __AWS X-Ray__
  - ### Features
    - Documentation updates for xray

# __2.10.43__ __2020-01-06__
## __AWS Elemental MediaPackage__
  - ### Features
    - You can now restrict direct access to AWS Elemental MediaPackage by securing requests for live content using CDN authorization. With CDN authorization, content requests require a specific HTTP header and authorization code.

## __AWS SDK for Java v2__
  - ### Features
    - Add `RequestBody.fromRemainingByteBuffer(ByteBuffer)` that copies only the remaining readable bytes of the buffer. See [#1534](https://github.com/aws/aws-sdk-java-v2/issues/1534)

  - ### Bugfixes
    - Reduce ReadTimeout and ConnectTimeout for accessing EC2 metadata instance service

## __Amazon Comprehend__
  - ### Features
    - Amazon Comprehend now supports Multilabel document classification

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release supports service providers configuring a private DNS name for services other than AWS services and services available in the AWS marketplace. This feature allows consumers to access the service using an existing DNS name without making changes to their applications.

## __Amazon S3__
  - ### Bugfixes
    - Requests that return an error response in the body of the HTTP response with a successful (200) status code will now correctly be handled as a failed request by the SDK.

# __2.10.42__ __2020-01-02__
## __AWS Cost Explorer Service__
  - ### Features
    - Documentation updates for GetReservationUtilization for the Cost Explorer API.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix unmarshalling for models with xml attributes. See [#1488](https://github.com/aws/aws-sdk-java-v2/issues/1488).

## __Amazon EC2 Container Registry__
  - ### Features
    - Adds waiters for ImageScanComplete and LifecyclePolicyPreviewComplete

## __Amazon Lex Model Building Service__
  - ### Features
    - Documentation updates for Amazon Lex.

## __Amazon Lightsail__
  - ### Features
    - This release adds support for Certificate Authority (CA) certificate identifier to managed databases in Amazon Lightsail.

## __Netty NIO Http Client__
  - ### Bugfixes
    - Propagate exception properly when an exception is thrown from protocol initialization.

# __2.10.41__ __2019-12-23__
## __AWS Health APIs and Notifications__
  - ### Features
    - With this release, you can now centrally aggregate AWS Health events from all accounts in your AWS organization. Visit AWS Health documentation to learn more about enabling and using this feature: https://docs.aws.amazon.com/health/latest/ug/organizational-view-health.html.

## __Amazon Detective__
  - ### Features
    - Updated the documentation for Amazon Detective.

## __Amazon FSx__
  - ### Features
    - This release adds a new family of APIs (create-data-repository-task, describe-data-repository-task, and cancel-data-repository-task) that allow users to perform operations between their file system and its linked data repository.

# __2.10.40__ __2019-12-20__
## __AWS Device Farm__
  - ### Features
    - Introduced browser testing support through AWS Device Farm

## __AWS SecurityHub__
  - ### Features
    - Additional resource types are now fully supported in the AWS Security Finding Format (ASFF). These resources include AwsElbv2LoadBalancer, AwsKmsKey, AwsIamRole, AwsSqsQueue, AwsLambdaFunction, AwsSnsTopic, and AwsCloudFrontDistribution. Each of these resource types includes an accompanying resource details object with fields for security finding providers to populate. Updates were made to the AwsIamAccessKey resource details object to include information on principal ID and name. To learn more, visit our documentation on the ASFF.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release introduces the ability to tag key pairs, placement groups, export tasks, import image tasks, import snapshot tasks and export image tasks. You can use tags to organize and identify your resources for cost allocation.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Amazon EKS now supports restricting access to the API server public endpoint by applying CIDR blocks

## __Amazon Pinpoint__
  - ### Features
    - This release of the Amazon Pinpoint API introduces versioning support for message templates.

## __Amazon Redshift__
  - ### Features
    - Documentation updates for Amazon Redshift RA3 node types.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds an operation that enables users to specify whether a database is restarted when its SSL/TLS certificate is rotated. Only customers who do not use SSL/TLS should use this operation.

## __Amazon S3__
  - ### Bugfixes
    - Fixed an issue where the SDK would attempt to validate the checksum on a PutObjectRequest when S3 was returning invalid checksums. This would cause all requests to buckets with customer-managed-key service-side encryption to fail.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release updates the attachments support to include AttachmentReference source for Automation documents.

## __Amazon Transcribe Service__
  - ### Features
    - AWS Transcribe now supports vocabulary filtering that allows customers to input words to the service that they don't want to see in the output transcript.

# __2.10.39__ __2019-12-19__
## __AWS CodeStar connections__
  - ### Features
    - Public beta for Bitbucket Cloud support in AWS CodePipeline through integration with AWS CodeStar connections.

## __Amazon Data Lifecycle Manager__
  - ### Features
    - You can now copy snapshots across regions using Data Lifecycle Manager (DLM). You can enable policies which, along with create, can now also copy snapshots to one or more AWS region(s). Copies can be scheduled for up to three regions from a single policy and retention periods are set for each region separately.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - We are updating the supportedRootDevices field to supportedRootDeviceTypes for DescribeInstanceTypes API to ensure that the actual value is returned, correcting a previous error in the model.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift now supports ARNs for all key GameLift resources, tagging for GameLift resource authorization management, and updated documentation that articulates GameLift's resource authorization strategy.

## __Amazon Lex Model Building Service__
  - ### Features
    - Amazon Lex now supports conversation logs and slot obfuscation.

## __Amazon Personalize Runtime__
  - ### Features
    - Add context map to get-recommendations and get-personalized-ranking request objects to provide contextual metadata at inference time

## __Amazon S3__
  - ### Bugfixes
    - Fixed an issue where a 'checksum mismatch' error is raised whenever a PutObject request is retried while using an async client.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release allows customers to add tags to Automation execution, enabling them to sort and filter executions in different ways, such as by resource, purpose, owner, or environment.

## __Amazon Transcribe Service__
  - ### Features
    - Amazon Transcribe supports job queuing for the StartTranscriptionJob API.

## __Netty NIO HTTP Client__
  - ### Features
    - `SETTINGS_INITIAL_WINDOW_SIZE` is now configurable on HTTP/2 connections opened by the Netty client using `Http2Configuration#initialWindowSize(Integer)` along with `NettyNioAsyncHttpClient.Builder#http2Configuration(Http2Configuration)`. See https://tools.ietf.org/html/rfc7540#section-6.5.2 for more information.

# __2.10.38__ __2019-12-18__
## __AWS OpsWorks CM__
  - ### Features
    - AWS OpsWorks CM now supports tagging, and tag-based access control, of servers and backups.

## __AWS Resource Groups Tagging API__
  - ### Features
    - Documentation updates for resourcegroupstaggingapi

## __Amazon CloudFront__
  - ### Features
    - Documentation updates for CloudFront

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release introduces the ability to tag Elastic Graphics accelerators. You can use tags to organize and identify your accelerators for cost allocation.

## __Amazon Simple Storage Service__
  - ### Features
    - Updates Amazon S3 endpoints allowing you to configure your client to opt-in to using S3 with the us-east-1 regional endpoint, instead of global.

# __2.10.37__ __2019-12-17__
## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports HLS ID3 segment tagging, HLS redundant manifests for CDNs that support different publishing/viewing endpoints, fragmented MP4 (fMP4), and frame capture intervals specified in milliseconds.

## __AWS IoT__
  - ### Features
    - Added a new Over-the-Air (OTA) Update feature that allows you to use different, or multiple, protocols to transfer an image from the AWS cloud to IoT devices.

## __Amazon EC2 Container Service__
  - ### Features
    - Documentation updates for Amazon ECS.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for Amazon EC2

## __Amazon Kinesis Analytics__
  - ### Features
    - Kinesis Data Analytics service now supports running Java applications using Flink 1.8.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Added support for Cloud Watch Output and Document Version to the Run Command tasks in Maintenance Windows.

# __2.10.36__ __2019-12-16__
## __AWS Comprehend Medical__
  - ### Features
    - New Ontology linking APIs will provides medication concepts normalization and Diagnoses codes from input text. In this release we will provide two APIs - RxNorm and ICD10-CM.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - You can now configure your EC2 Fleet to preferentially use EC2 Capacity Reservations for launching On-Demand instances, enabling you to fully utilize the available (and unused) Capacity Reservations before launching On-Demand instances on net new capacity.

## __Amazon S3__
  - ### Features
    - CopyObjectRequest now has `destinationBucket` and `destinationKey` properties for clarity.
      The existing names, `bucket` and `key`, are deprecated.

## __AmazonMQ__
  - ### Features
    - Amazon MQ now supports throughput-optimized message brokers, backed by Amazon EBS.

# __2.10.35__ __2019-12-13__
## __AWS CodeBuild__
  - ### Features
    - CodeBuild adds support for cross account

## __Amazon Detective__
  - ### Features
    - This is the initial release of Amazon Detective.

## __Amazon Simple Email Service__
  - ### Features
    - Added the ability to use your own public-private key pair to configure DKIM authentication for a domain identity.

# __2.10.34__ __2019-12-12__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixing exception using `RequestBody.fromInputStream` on non-resettable `InputStreams` by making `reset` conditional on `markSupported`. See [#1544](https://github.com/aws/aws-sdk-java-v2/issues/1544) / [#1545](https://github.com/aws/aws-sdk-java-v2/issues/1545)

## __Access Analyzer__
  - ### Features
    - This release includes improvements and fixes bugs for the IAM Access Analyzer feature.

# __2.10.33__ __2019-12-11__
## __AWS SDK for Java v2__
  - ### Features
    - Adds a `has*` method to requests and responses that have a List or Map property.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release allows customers to attach multiple Elastic Inference Accelerators to a single EC2 instance. It adds support for a Count parameter for each Elastic Inference Accelerator type you specify on the RunInstances and LaunchTemplate APIs.

# __2.10.32__ __2019-12-10__
## __AWSKendraFrontendService__
  - ### Features
    - 1. Adding DocumentTitleFieldName as an optional configuration for SharePoint. 2. updating s3 object pattern to support all s3 keys.

# __2.10.31__ __2019-12-09__
## __AWS Key Management Service__
  - ### Features
    - The Verify operation now returns KMSInvalidSignatureException on invalid signatures. The Sign and Verify operations now return KMSInvalidStateException when a request is made against a CMK pending deletion.

## __Amazon QuickSight__
  - ### Features
    - Documentation updates for QuickSight

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Adds the SSM GetCalendarState API and ChangeCalendar SSM Document type. These features enable the forthcoming Systems Manager Change Calendar feature, which will allow you to schedule events during which actions should (or should not) be performed.

## __Managed Streaming for Kafka__
  - ### Features
    - AWS MSK has added support for Open Monitoring with Prometheus.

## __Netty NIO HTTP Client__
  - ### Features
    - Close HTTP/2 connections if they have had 0 streams for 5 seconds. This can be disabled using `useIdleConnectionReaper(false)` or have the time period adjusted using `connectionMaxIdleTime(...)` on the `NettyNioAsyncHttpClient.Builder`.
    - Periodically ping HTTP/2 connections and close them if the service does not respond. The ping periodicity and timeout time is not currently configurable.

# __2.10.30__ __2019-12-04__
## __Amazon Kinesis Video Signaling Channels__
  - ### Features
    - Announcing support for WebRTC in Kinesis Video Streams, as fully managed capability. You can now use simple APIs to enable your connected devices, web, and mobile apps with real-time two-way media streaming capabilities.

## __Amazon Kinesis Video Streams__
  - ### Features
    - Introduces management of signaling channels for Kinesis Video Streams.

## __AmazonApiGatewayV2__
  - ### Features
    - Amazon API Gateway now supports HTTP APIs (beta), enabling customers to quickly build high performance RESTful APIs that are up to 71% cheaper than REST APIs also available from API Gateway. HTTP APIs are optimized for building APIs that proxy to AWS Lambda functions or HTTP backends, making them ideal for serverless workloads. Using HTTP APIs, you can secure your APIs using OIDC and OAuth 2 out of box, quickly build web applications using a simple CORS experience, and get started immediately with automatic deployment and simple create workflows.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fixed an issue where closing the last stream on a connection that had been closed or received a GOAWAY did not close the connection.
    - Fixed an issue where receiving a GOAWAY that would cause the closing of all streams could cause all outstanding streams to be completed successfully instead of exceptionally.

# __2.10.29__ __2019-12-03__
## __AWS Lambda__
  - ### Features
    - - Added the ProvisionedConcurrency type and operations. Allocate provisioned concurrency to enable your function to scale up without fluctuations in latency. Use PutProvisionedConcurrencyConfig to configure provisioned concurrency on a version of a function, or on an alias.

## __AWS Step Functions__
  - ### Features
    - This release of the AWS Step Functions SDK introduces support for Express Workflows.

## __Amazon Elastic Block Store__
  - ### Features
    - This release introduces the EBS direct APIs for Snapshots: 1. ListSnapshotBlocks, which lists the block indexes and block tokens for blocks in an Amazon EBS snapshot. 2. ListChangedBlocks, which lists the block indexes and block tokens for blocks that are different between two snapshots of the same volume/snapshot lineage. 3. GetSnapshotBlock, which returns the data in a block of an Amazon EBS snapshot.

## __Amazon Rekognition__
  - ### Features
    - This SDK Release introduces APIs for Amazon Rekognition Custom Labels feature (CreateProjects, CreateProjectVersion,DescribeProjects, DescribeProjectVersions, StartProjectVersion, StopProjectVersion and DetectCustomLabels). Also new is AugmentedAI (Human In The Loop) Support for DetectModerationLabels in Amazon Rekognition.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for the Amazon RDS Proxy

## __Amazon S3__
  - ### Bugfixes
    - Interacting with an access point in a different region to the one the S3 client is configured for will no longer result in the request being signed for the wrong region and rejected by S3.

## __Amazon SageMaker Service__
  - ### Features
    - You can now use SageMaker Autopilot for automatically training and tuning candidate models using a combination of various feature engineering, ML algorithms, and hyperparameters determined from the user's input data. SageMaker Automatic Model Tuning now supports tuning across multiple algorithms. With Amazon SageMaker Experiments users can create Experiments, ExperimentTrials, and ExperimentTrialComponents to track, organize, and evaluate their ML training jobs. With Amazon SageMaker Debugger, users can easily debug training jobs using a number of pre-built rules provided by Amazon SageMaker, or build custom rules. With Amazon SageMaker Processing, users can run on-demand, distributed, and fully managed jobs for data pre- or post- processing or model evaluation. With Amazon SageMaker Model Monitor, a user can create MonitoringSchedules to automatically monitor endpoints to detect data drift and other issues and get alerted on them. This release also includes the preview version of Amazon SageMaker Studio with Domains, UserProfiles, and Apps. This release also includes the preview version of Amazon Augmented AI to easily implement human review of machine learning predictions by creating FlowDefinitions, HumanTaskUis, and HumanLoops.

## __Application Auto Scaling__
  - ### Features
    - This release supports auto scaling of provisioned concurrency for AWS Lambda.

# __2.10.28__ __2019-12-03__
## __AWS Compute Optimizer__
  - ### Features
    - Initial release of AWS Compute Optimizer. AWS Compute Optimizer recommends optimal AWS Compute resources to reduce costs and improve performance for your workloads.

## __AWS Network Manager__
  - ### Features
    - This is the initial SDK release for AWS Network Manager.

## __AWS Outposts__
  - ### Features
    - This is the initial release for AWS Outposts, a fully managed service that extends AWS infrastructure, services, APIs, and tools to customer sites. AWS Outposts enables you to launch and run EC2 instances and EBS volumes locally at your on-premises location. This release introduces new APIs for creating and viewing Outposts.

## __AWS S3 Control__
  - ### Features
    - Amazon S3 Access Points is a new S3 feature that simplifies managing data access at scale for shared data sets on Amazon S3. Access Points provide a customizable way to access the objects in a bucket, with a unique hostname and access policy that enforces the specific permissions and network controls for any request made through the access point. This represents a new way of provisioning access to shared data sets.

## __AWSKendraFrontendService__
  - ### Features
    - It is a preview launch of Amazon Kendra. Amazon Kendra is a managed, highly accurate and easy to use enterprise search service that is powered by machine learning.

## __Amazon Augmented AI Runtime__
  - ### Features
    - This release adds support for Amazon Augmented AI, which makes it easy to build workflows for human review of machine learning predictions.

## __Amazon CodeGuru Profiler__
  - ### Features
    - (New Service) Amazon CodeGuru Profiler analyzes application CPU utilization and latency characteristics to show you where you are spending the most cycles in your application. This analysis is presented in an interactive flame graph that helps you easily understand which paths consume the most resources, verify that your application is performing as expected, and uncover areas that can be optimized further.

## __Amazon CodeGuru Reviewer__
  - ### Features
    - This is the preview release of Amazon CodeGuru Reviewer.

## __Amazon EC2 Container Service__
  - ### Features
    - This release supports ECS Capacity Providers, Fargate Spot, and ECS Cluster Auto Scaling. These features enable new ways for ECS to manage compute capacity used by tasks.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for the following features: 1. An option to enable acceleration for Site-to-Site VPN connections; 2. Inf1 instances featuring up to 16 AWS Inferentia chips; 3. The ability to associate route tables with internet gateways and virtual private gateways; 4. AWS Local Zones that place compute, storage, database, and other select services; 5. Launching and viewing EC2 instances and EBS volumes running locally in Outposts; 6. Peering Transit Gateways between regions simplifying creation of secure and private global networks on AWS; 7. Transit Gateway Multicast, enabling multicast routing within and between VPCs using Transit Gateway as a multicast router.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Introducing Amazon EKS with Fargate. Customers can now use Amazon EKS to launch pods directly onto AWS Fargate, the serverless compute engine built for containers on AWS.

## __Amazon Elasticsearch Service__
  - ### Features
    - UltraWarm storage provides a cost-effective way to store large amounts of read-only data on Amazon Elasticsearch Service. Rather than attached storage, UltraWarm nodes use Amazon S3 and a sophisticated caching solution to improve performance. For indices that you are not actively writing to and query less frequently, UltraWarm storage offers significantly lower costs per GiB. In Elasticsearch, these warm indices behave just like any other index. You can query them using the same APIs or use them to create dashboards in Kibana.

## __Amazon Fraud Detector__
  - ### Features
    - Amazon Fraud Detector is a fully managed service that makes it easy to identify potentially fraudulent online activities such as online payment fraud and the creation of fake accounts. Amazon Fraud Detector uses your data, machine learning (ML), and more than 20 years of fraud detection expertise from Amazon to automatically identify potentially fraudulent online activity so you can catch more fraud faster.

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon S3 Access Points is a new S3 feature that simplifies managing data access at scale for shared data sets on Amazon S3. Access Points provide a customizable way to access the objects in a bucket, with a unique hostname and access policy that enforces the specific permissions and network controls for any request made through the access point. This represents a new way of provisioning access to shared data sets.

## __Amazon Textract__
  - ### Features
    - This SDK Release introduces Amazon Augmented AI support for Amazon Textract AnalyzeDocument API. Image byte payloads for synchronous operations have increased from 5 MB to 10 MB.

# __2.10.27__ __2019-12-02__
## __Access Analyzer__
  - ### Features
    - Introducing AWS IAM Access Analyzer, an IAM feature that makes it easy for AWS customers to ensure that their resource-based policies provide only the intended access to resources outside their AWS accounts.

# __2.10.26__ __2019-12-02__
## __AWS License Manager__
  - ### Features
    - AWS License Manager now automates discovery of bring-your-own-license usage across the customers organization. With few simple settings, customers can add bring your own license product information along with licensing rules, which would enable License Manager to automatically track the instances that have the specified products installed. If License Manager detects any violation of licensing rules, it would notify the customers designated license administrator to take corrective action.

## __Amazon DynamoDB Enhanced Client [Preview]__
  - ### Features
    - Write operations (put, get, delete) now support 'conditionExpression'

## __Amazon Elastic Compute Cloud__
  - ### Features
    - AWS now provides a new BYOL experience for software licenses, such as Windows and SQL Server, that require a dedicated physical server. You can now enjoy the flexibility and cost effectiveness of using your own licenses on Amazon EC2 Dedicated Hosts, but with the simplicity, resiliency, and elasticity of AWS. You can specify your Dedicated Host management preferences, such as host allocation, host capacity utilization, and instance placement in AWS License Manager. Once set up, AWS takes care of these administrative tasks on your behalf, so that you can seamlessly launch virtual machines (instances) on Dedicated Hosts just like you would launch an EC2 instance with AWS provided licenses.

## __EC2 Image Builder__
  - ### Features
    - This is the first release of EC2 Image Builder, a service that provides a managed experience for automating the creation of EC2 AMIs.

## __Schemas__
  - ### Features
    - This release introduces support for Amazon EventBridge schema registry, making it easy to discover and write code for events in EventBridge.

# __2.10.25__ __2019-11-26__
## __AWS Directory Service__
  - ### Features
    - This release will introduce optional encryption over LDAP network traffic using SSL certificates between customer's self-managed AD and AWS Directory Services instances. The release also provides APIs for Certificate management.

## __AWS Kinesis__
  - ### Bugfixes
    - Reducing default read timeout and write timeout to 10 seconds for Kinesis client.

## __AWS MediaTailor__
  - ### Features
    - AWS Elemental MediaTailor SDK now allows configuration of the Live Pre-Roll feature for HLS and DASH streams.

## __AWS Organizations__
  - ### Features
    - Introduces the DescribeEffectivePolicy action, which returns the contents of the policy that's in effect for the account.

## __AWS RDS DataService__
  - ### Features
    - Type hints to improve handling of some specific parameter types (date/time, decimal etc) for ExecuteStatement and BatchExecuteStatement APIs

## __AWS Resource Groups Tagging API__
  - ### Features
    - You can use tag policies to help standardize on tags across your organization's resources.

## __AWSServerlessApplicationRepository__
  - ### Features
    - AWS Serverless Application Repository now supports verified authors. Verified means that AWS has made a good faith review, as a reasonable and prudent service provider, of the information provided by the requester and has confirmed that the requester's identity is as claimed.

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release adds a new setting for a user pool to configure which recovery methods a user can use to recover their account via the forgot password operation.

## __Amazon DynamoDB__
  - ### Features
    - 1) Amazon Contributor Insights for Amazon DynamoDB is a diagnostic tool for identifying frequently accessed keys and understanding database traffic trends. 2) Support for displaying new fields when a table's encryption state is Inaccessible or the table have been Archived.

## __Amazon Elastic Inference__
  - ### Features
    - Amazon Elastic Inference allows customers to attach Elastic Inference Accelerators to Amazon EC2 and Amazon ECS tasks, thus providing low-cost GPU-powered acceleration and reducing the cost of running deep learning inference. This release allows customers to add or remove tags for their Elastic Inference Accelerators.

## __Amazon QuickSight__
  - ### Features
    - Documentation updates for QuickSight

## __Amazon WorkSpaces__
  - ### Features
    - For the WorkspaceBundle API, added the image identifier and the time of the last update.

## __Netty NIO HTTP Client__
  - ### Features
    - Detect unhealthy http2 connections when read or write times out by sending PING frames

# __2.10.24__ __2019-11-25__
## __AWS CodeBuild__
  - ### Features
    - CodeBuild adds support for test reporting

## __AWS Cost Explorer Service__
  - ### Features
    - This launch provides customers with access to Cost Category Public Beta APIs.

## __AWS Elemental MediaConvert__
  - ### Features
    - AWS Elemental MediaConvert SDK has added support for 8K outputs and support for QuickTime Animation Codec (RLE) inputs.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports the ability to create a multiple program transport stream (MPTS).

## __AWS Elemental MediaPackage VOD__
  - ### Features
    - Adds a domain name to PackagingGroups, representing the fully qualified domain name for Assets created in the group.

## __AWS Greengrass__
  - ### Features
    - IoT Greengrass supports machine learning resources in 'No container' mode.

## __AWS IoT__
  - ### Features
    - This release adds: 1) APIs for fleet provisioning claim and template, 2) endpoint configuration and custom domains, 3) support for enhanced custom authentication, d) support for 4 additional audit checks: Device and CA certificate key quality checks, IoT role alias over-permissive check and IoT role alias access to unused services check, 5) extended capability of AWS IoT Rules Engine to support IoT SiteWise rule action. The IoT SiteWise rule action lets you send messages from IoT sensors and applications to IoT SiteWise asset properties

## __AWS IoT Secure Tunneling__
  - ### Features
    - This release adds support for IoT Secure Tunneling to remote access devices behind restricted firewalls.

## __AWS Key Management Service__
  - ### Features
    - AWS Key Management Service (KMS) now enables creation and use of asymmetric Customer Master Keys (CMKs) and the generation of asymmetric data key pairs.

## __AWS Lambda__
  - ### Features
    - Added the function state and update status to the output of GetFunctionConfiguration and other actions. Check the state information to ensure that a function is ready before you perform operations on it. Functions take time to become ready when you connect them to a VPC.Added the EventInvokeConfig type and operations to configure error handling options for asynchronous invocation. Use PutFunctionEventInvokeConfig to configure the number of retries and the maximum age of events when you invoke the function asynchronously.Added on-failure and on-success destination settings for asynchronous invocation. Configure destinations to send an invocation record to an SNS topic, an SQS queue, an EventBridge event bus, or a Lambda function.Added error handling options to event source mappings. This enables you to configure the number of retries, configure the maximum age of records, or retry with smaller batches when an error occurs when a function processes a Kinesis or DynamoDB stream.Added the on-failure destination setting to event source mappings. This enables you to send discarded events to an SNS topic or SQS queue when all retries fail or when the maximum record age is exceeded when a function processes a Kinesis or DynamoDB stream.Added the ParallelizationFactor option to event source mappings to increase concurrency per shard when a function processes a Kinesis or DynamoDB stream.

## __AWS Resource Access Manager__
  - ### Features
    - AWS RAM provides new APIs to view the permissions granted to principals in a resource share. This release also creates corresponding resource shares for supported services that use resource policies, as well as an API to promote them to standard shares that can be managed in RAM.

## __AWS WAFV2__
  - ### Features
    - This release introduces new set of APIs ("wafv2") for AWS WAF. Major changes include single set of APIs for creating/updating resources in global and regional scope, and rules are configured directly into web ACL instead of being referenced. The previous APIs ("waf" and "waf-regional") are now referred as AWS WAF Classic. For more information visit: https://docs.aws.amazon.com/waf/latest/APIReference/Welcome.html

## __Alexa For Business__
  - ### Features
    - API update for Alexa for Business: This update enables the use of meeting room configuration that can be applied to a room profile. These settings help improve and measure utilization on Alexa for Business enabled rooms. New features include end meeting reminders, intelligent room release and room utilization analytics report.

## __Amazon AppConfig__
  - ### Features
    - Introducing AWS AppConfig, a new service that enables customers to quickly deploy validated configurations to applications of any size in a controlled and monitored fashion.

## __Amazon Athena__
  - ### Features
    - This release adds additional query lifecycle metrics to the QueryExecutionStatistics object in GetQueryExecution response.

## __Amazon CloudWatch__
  - ### Features
    - This release adds a new feature called "Contributor Insights". "Contributor Insights" supports the following 6 new APIs (PutInsightRule, DeleteInsightRules, EnableInsightRules, DisableInsightRules, DescribeInsightRules and GetInsightRuleReport).

## __Amazon CloudWatch Application Insights__
  - ### Features
    - CloudWatch Application Insights for .NET and SQL Server includes the follwing features: -Tagging Create and manage tags for your applications.-Custom log pattern matching. Define custom log patterns to be detected and monitored.-Resource-level permissions. Specify applications users can access.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Amazon Cognito Userpools now supports Sign in with Apple as an Identity Provider.

## __Amazon Comprehend__
  - ### Features
    - Amazon Comprehend now supports real-time analysis with Custom Classification

## __Amazon Data Lifecycle Manager__
  - ### Features
    - You can now set time based retention policies on Data Lifecycle Manager. With this launch, DLM allows you to set snapshot retention period in the following interval units: days, weeks, months and years.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds two new APIs: 1. ModifyDefaultCreditSpecification, which allows you to set default credit specification at the account level per AWS Region, per burstable performance instance family, so that all new burstable performance instances in the account launch using the new default credit specification. 2. GetDefaultCreditSpecification, which allows you to get current default credit specification per AWS Region, per burstable performance instance family. This release also adds new client exceptions for StartInstances and StopInstances.

## __Amazon Kinesis Analytics__
  - ### Features
    - Kinesis Data Analytics service adds support to configure Java applications to access resources in a VPC. Also releasing support to configure Java applications to set allowNonRestoreState flag through the service APIs.

## __Amazon Lex Runtime Service__
  - ### Features
    - Amazon Lex adds "sessionId" attribute to the PostText and PostContent response.

## __Amazon Redshift__
  - ### Features
    - This release contains changes for 1. Redshift Scheduler 2. Update to the DescribeNodeConfigurationOptions to include a new action type recommend-node-config

## __Amazon Relational Database Service__
  - ### Features
    - Cluster Endpoints can now be tagged by using --tags in the create-db-cluster-endpoint API

## __Amazon Simple Email Service__
  - ### Features
    - This release includes support for automatically suppressing email addresses that result in hard bounce or complaint events at the account level, and for managing addresses on this account-level suppression list.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - AWS Systems Manager Documents now supports more Document Types: ApplicationConfiguration, ApplicationConfigurationSchema and DeploymentStrategy. This release also extends Document Permissions capabilities and introduces a new Force flag for DeleteDocument API.

## __Application Auto Scaling__
  - ### Features
    - This release supports auto scaling of document classifier endpoints for Comprehend; and supports target tracking based on the average capacity utilization metric for AppStream 2.0 fleets.

## __Elastic Load Balancing__
  - ### Features
    - This release of Elastic Load Balancing V2 adds new subnet features for Network Load Balancers and a new routing algorithm for Application Load Balancers.

# __2.10.23__ __2019-11-22__
## __AWS Auto Scaling Plans__
  - ### Features
    - Update default endpoint for AWS Auto Scaling.

## __AWS Certificate Manager__
  - ### Features
    - This release adds support for Tag-Based IAM for AWS Certificate Manager and adding tags to certificates upon creation.

## __AWS CodeBuild__
  - ### Features
    - Add Canonical ARN to LogsLocation.

## __AWS Elemental MediaPackage VOD__
  - ### Features
    - Includes the submission time of Asset ingestion request in the API response for Create/List/Describe Assets.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - The ProcessCredentialsProvider now supports credential files up to 64 KB by default through an increase of the processOutputLimit from 1024 bytes to 64000 bytes.

## __AWS Security Token Service__
  - ### Features
    - Support tagging for STS sessions and tag based access control for the STS APIs

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds two new APIs (DescribeInstanceTypes and DescribeInstanceTypeOfferings) that give customers access to instance type attributes and regional and zonal offerings.

## __Amazon Elastic MapReduce__
  - ### Features
    - Amazon EMR adds support for concurrent step execution and cancelling running steps. Amazon EMR has added a new Outpost ARN field in the ListCluster and DescribeCluster API responses that is populated for clusters launched in an AWS Outpost subnet.

## __Amazon Forecast Service__
  - ### Features
    - This release adds two key updates to existing APIs. 1. Amazon Forecast can now generate forecasts in any quantile using the optional parameter forecastTypes in the CreateForecast API and 2. You can get additional details (metrics and relevant error messages) on your AutoML runs using the DescribePredictor and GetAccuracyMetrics APIs.

## __Amazon Rekognition__
  - ### Features
    - This release adds enhanced face filtering support to the IndexFaces API operation, and introduces face filtering for CompareFaces and SearchFacesByImage API operations.

## __Amazon Simple Notification Service__
  - ### Features
    - Added documentation for the dead-letter queue feature.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Add RebootOption and LastNoRebootInstallOperationTime for DescribeInstancePatchStates and DescribeInstancePatchStatesForPatchGroup API

## __Application Auto Scaling__
  - ### Features
    - Update default endpoint for Application Auto Scaling.

# __2.10.22__ __2019-11-21__
## __AWS Amplify__
  - ### Features
    - This release of AWS Amplify Console introduces support for backend environments. Backend environments are containers for AWS deployments. Each environment is a collection of AWS resources.

## __AWS AppSync__
  - ### Features
    - AppSync: AWS AppSync now supports the ability to add, configure, and maintain caching for your AWS AppSync GraphQL API.

## __AWS Config__
  - ### Features
    - AWS Config launches Custom Configuration Items. A new feature which allows customers to publish resource configuration for third-party resources, custom, or on-premises servers.

## __AWS Glue__
  - ### Features
    - This release adds support for Glue 1.0 compatible ML Transforms.

## __AWSMarketplace Metering__
  - ### Features
    - Documentation updates for the AWS Marketplace Metering Service.

## __Amazon Connect Participant Service__
  - ### Features
    - This release adds 5 new APIs: CreateParticipantConnection, DisconnectParticipant, GetTranscript, SendEvent, and SendMessage. For Amazon Connect chat, you can use them to programmatically perform participant actions on the configured Amazon Connect instance. Learn more here: https://docs.aws.amazon.com/connect-participant/latest/APIReference/Welcome.html

## __Amazon Connect Service__
  - ### Features
    - This release adds a new API: StartChatContact. You can use it to programmatically start a chat on the specified Amazon Connect instance. Learn more here: https://docs.aws.amazon.com/connect/latest/APIReference/Welcome.html

## __Amazon DynamoDB__
  - ### Features
    - With this release, you can convert an existing Amazon DynamoDB table to a global table by adding replicas in other AWS Regions.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for attaching AWS License Manager Configurations to Amazon Machine Image (AMI) using ImportImage API; and adds support for running different instance sizes on EC2 Dedicated Hosts

## __Amazon Lex Model Building Service__
  - ### Features
    - Amazon Lex now supports Sentiment Analysis

## __Amazon Lex Runtime Service__
  - ### Features
    - Amazon Lex now supports Sentiment Analysis

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - The release contains new API and API changes for AWS Systems Manager Explorer product.

## __Amazon Transcribe Service__
  - ### Features
    - With this release, Amazon Transcribe now supports transcriptions from audio sources in Hebrew (he-IL), Swiss German (de-CH), Japanese (ja-JP), Turkish (tr-TR), Arabic-Gulf (ar-AE), Malay (ms-MY), Telugu (te-IN)

# __2.10.21__ __2019-11-20__
## __AWS Application Discovery Service__
  - ### Features
    - New exception type for use with Migration Hub home region

## __AWS CloudTrail__
  - ### Features
    - 1. This release adds two new APIs, GetInsightSelectors and PutInsightSelectors, which let you configure CloudTrail Insights event delivery on a trail. An Insights event is a new type of event that is generated when CloudTrail detects unusual activity in your AWS account. In this release, only "ApiCallRateInsight" is a supported Insights event type. 2. This release also adds the new "ExcludeManagementEventSource" option to the existing PutEventSelectors API. This field currently supports only AWS Key Management Services.

## __AWS CodeCommit__
  - ### Features
    - This release adds support for creating pull request approval rules and pull request approval rule templates in AWS CodeCommit. This allows developers to block merges of pull requests, contingent on the approval rules being satisfiied.

## __AWS DataSync__
  - ### Features
    - Update to configure task to run periodically on a schedule

## __AWS Elemental MediaStore__
  - ### Features
    - This release fixes a broken link in the SDK documentation.

## __AWS Migration Hub__
  - ### Features
    - New exception type for use with Migration Hub home region

## __AWS Migration Hub Config__
  - ### Features
    - AWS Migration Hub Config Service allows you to get and set the Migration Hub home region for use with AWS Migration Hub and Application Discovery Service

## __AWS Storage Gateway__
  - ### Features
    - The new DescribeAvailabilityMonitorTest API provides the results of the most recent High Availability monitoring test. The new StartAvailabilityMonitorTest API verifies the storage gateway is configured for High Availability monitoring. The new ActiveDirectoryStatus response element has been added to the DescribeSMBSettings and JoinDomain APIs to indicate the status of the gateway after the most recent JoinDomain operation. The new TimeoutInSeconds parameter of the JoinDomain API allows for the configuration of the timeout in which the JoinDomain operation must complete.

## __Amazon Chime__
  - ### Features
    - Adds APIs to create and manage meeting session resources for the Amazon Chime SDK

## __Amazon Data Lifecycle Manager__
  - ### Features
    - DLM now supports Fast Snapshot Restore. You can enable Fast Restore on snapshots created by DLM, provide the AZs and the number of snapshots to be enabled with this capability.

## __Amazon EC2 Container Service__
  - ### Features
    - Added support for CPU and memory task-level overrides on the RunTask and StartTask APIs. Added location information to Tasks.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release of Amazon Elastic Compute Cloud (Amazon EC2) introduces support for Amazon Elastic Block Store (Amazon EBS) fast snapshot restores.

## __Amazon FSx__
  - ### Features
    - Announcing a Multi-AZ deployment type for Amazon FSx for Windows File Server, providing fully-managed Windows file storage with high availability and redundancy across multiple AWS Availability Zones.

## __Amazon Kinesis Firehose__
  - ### Features
    - With this release, Amazon Kinesis Data Firehose allows server side encryption with customer managed CMKs. Customer managed CMKs ( "Customer Master Keys") are AWS Key Management Service (KMS) keys that are fully managed by the customer. With customer managed CMKs, customers can establish and maintain their key policies, IAM policies, rotating policies and add tags. For more information about AWS KMS and CMKs, please refer to: https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html. Please refer to the following link to create CMKs: https://docs.aws.amazon.com/kms/latest/developerguide/importing-keys-create-cmk.html

## __Amazon QuickSight__
  - ### Features
    - Amazon QuickSight now supports programmatic creation and management of data sources, data sets, dashboards and templates with new APIs. Templates hold dashboard metadata, and can be used to create copies connected to the same or different dataset as required. Also included in this release are APIs for SPICE ingestions, fine-grained access control over AWS resources using AWS Identity and Access Management (IAM) policies, as well AWS tagging. APIs are supported for both Standard and Enterprise Edition, with edition-specific support for specific functionality.

## __Amazon Simple Storage Service__
  - ### Features
    - This release introduces support for Amazon S3 Replication Time Control, a new feature of S3 Replication that provides a predictable replication time backed by a Service Level Agreement. S3 Replication Time Control helps customers meet compliance or business requirements for data replication, and provides visibility into the replication process with new Amazon CloudWatch Metrics.

## __Amazon Transcribe Service__
  - ### Features
    - With this release Amazon Transcribe enables alternative transcriptions so that you can see different interpretations of transcribed audio.

# __2.10.20__ __2019-11-19__
## __AWS CloudFormation__
  - ### Features
    - This release of AWS CloudFormation StackSets enables users to detect drift on a stack set and the stack instances that belong to that stack set.

## __AWS CodeBuild__
  - ### Features
    - Add support for ARM and GPU-enhanced build environments and a new SSD-backed Linux compute type with additional CPU and memory in CodeBuild

## __AWS Config__
  - ### Features
    - AWSConfig launches support for conformance packs. A conformance pack is a new resource type that allows you to package a collection of Config rules and remediation actions into a single entity. You can create and deploy conformance packs into your account or across all accounts in your organization

## __AWS Identity and Access Management__
  - ### Features
    - IAM reports the timestamp when a role's credentials were last used to make an AWS request. This helps you identify unused roles and remove them confidently from your AWS accounts.

## __AWS IoT__
  - ### Features
    - As part of this release, we are extending the capability of AWS IoT Rules Engine to send messages directly to customer's own web services/applications. Customers can now create topic rules with HTTP actions to route messages from IoT Core directly to URL's that they own. Ownership is proved by creating and confirming topic rule destinations.

## __AWS Lambda__
  - ### Features
    - This release provides three new runtimes to support Node.js 12 (initially 12.13.0), Python 3.8 and Java 11.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for RunInstances to specify the metadata options for new instances; adds a new API, ModifyInstanceMetadataOptions, which lets you modify the metadata options for a running or stopped instance; and adds support for CreateCustomerGateway to specify a device name.

## __Auto Scaling__
  - ### Features
    - Amazon EC2 Auto Scaling now supports Instance Weighting and Max Instance Lifetime. Instance Weighting allows specifying the capacity units for each instance type included in the MixedInstancesPolicy and how they would contribute to your application's performance. Max Instance Lifetime allows specifying the maximum length of time that an instance can be in service. If any instances are approaching this limit, Amazon EC2 Auto Scaling gradually replaces them.

## __Elastic Load Balancing__
  - ### Features
    - This release allows forward actions on Application Load Balancers to route requests to multiple target groups, based on the weight you specify for each target group.

# __2.10.19__ __2019-11-18__
## __AWS CloudFormation__
  - ### Features
    - This release introduces APIs for the CloudFormation Registry, a new service to submit and discover resource providers with which you can manage third-party resources natively in CloudFormation.

## __AWS Cost Explorer Service__
  - ### Features
    - add EstimatedOnDemandCostWithCurrentCommitment to GetSavingsPlansPurchaseRecommendationRequest API

## __Amazon Pinpoint__
  - ### Features
    - This release of the Amazon Pinpoint API introduces support for using and managing message templates for messages that are sent through the voice channel. It also introduces support for specifying default values for message variables in message templates.

## __Amazon Relational Database Service__
  - ### Features
    - Documentation updates for rds

## __Amazon SageMaker Runtime__
  - ### Features
    - Amazon SageMaker Runtime now supports a new TargetModel header to invoke a specific model hosted on multi model endpoints.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker now supports multi-model endpoints to host multiple models on an endpoint using a single inference container.

## __Amazon Simple Storage Service__
  - ### Features
    - Added support for S3 Replication for existing objects. This release allows customers who have requested and been granted access to replicate existing S3 objects across buckets.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - The release contains new API and API changes for AWS Systems Manager Explorer product.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Update default connectionMaxIdleTimeout of NettyNioAsyncClient to 5 seconds

# __2.10.18__ __2019-11-15__
## __AWS Elemental MediaConvert__
  - ### Features
    - AWS Elemental MediaConvert SDK has added support for DolbyVision encoding, and SCTE35 & ESAM insertion to DASH ISO EMSG.

## __AWS SDK for Java v2__
  - ### Features
    - When SdkException or one of its children is created without a 'message', inherit the message from the exception 'cause' (if any). This should reduce the chance of an exception being raised by the SDK with a null message.

## __Amazon Chime__
  - ### Features
    - This release adds support for Chime Room Management APIs

## __Amazon CloudWatch Logs__
  - ### Features
    - Documentation updates for logs

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release adds a new option in the User Pool to allow specifying sender's name in the emails sent by Amazon Cognito. This release also adds support to add SES Configuration Set to the emails sent by Amazon Cognito.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - You can now add tags while copying snapshots. Previously, a user had to first copy the snapshot and then add tags to the copied snapshot manually. Moving forward, you can specify the list of tags you wish to be applied to the copied snapshot as a parameter on the Copy Snapshot API.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Introducing Amazon EKS managed node groups, a new feature that lets you easily provision worker nodes for Amazon EKS clusters and keep them up to date using the Amazon EKS management console, CLI, and APIs.

## __Amazon Elastic MapReduce__
  - ### Features
    - Access to the cluster ARN makes it easier for you to author resource-level permissions policies in AWS Identity and Access Management. To simplify the process of obtaining the cluster ARN, Amazon EMR has added a new field containing the cluster ARN to all API responses that include the cluster ID.

## __Amazon GuardDuty__
  - ### Features
    - This release includes new operations related to findings export, including: CreatePublishingDestination, UpdatePublishingDestination, DescribePublishingDestination, DeletePublishingDestination and ListPublishingDestinations.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release updates AWS Systems Manager Parameter Store documentation for the enhanced search capability.

## __Amazon WorkSpaces__
  - ### Features
    - Added APIs to register your directories with Amazon WorkSpaces and to modify directory details.

## __Elastic Load Balancing__
  - ### Features
    - Documentation-only change to the default value of the routing.http.drop_invalid_header_fields.enabled attribute.

# __2.10.17__ __2019-11-14__
## __AWSMarketplace Metering__
  - ### Features
    - Added CustomerNotEntitledException in MeterUsage API for Container use case.

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release adds a new setting at user pool client to prevent user existence related errors during authentication, confirmation, and password recovery related operations. This release also adds support to enable or disable specific authentication flows for a user pool client.

## __Amazon Connect Service__
  - ### Features
    - This release enhances the existing user management APIs and adds 3 new APIs - TagResource, UntagResource, and ListTagsForResource to support tagging Amazon Connect users, which facilitates more granular access controls for Amazon Connect users within an Amazon Connect instance. You can learn more about the new APIs here: https://docs.aws.amazon.com/connect/latest/APIReference/Welcome.html.

## __Amazon Personalize__
  - ### Features
    - Amazon Personalize: Adds ability to get batch recommendations by creating a batch inference job.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Updates support for adding attachments to Systems Manager Automation documents

# __2.10.16__ __2019-11-13__
## __AWS Data Exchange__
  - ### Features
    - Introducing AWS Data Exchange, a service that makes it easy for AWS customers to securely create, manage, access, and exchange data sets in the cloud.

## __AWS IoT__
  - ### Features
    - This release adds the custom fields definition support in the index definition for AWS IoT Fleet Indexing Service. Custom fields can be used as an aggregation field to run aggregations with both existing GetStatistics API and newly added GetCardinality, GetPercentiles APIs. GetStatistics will return all statistics (min/max/sum/avg/count...) with this release. For more information, please refer to our latest documentation: https://docs.aws.amazon.com/iot/latest/developerguide/iot-indexing.html

## __Amazon CloudSearch__
  - ### Features
    - Amazon CloudSearch domains let you require that all traffic to the domain arrive over HTTPS. This security feature helps you block clients that send unencrypted requests to the domain.

## __Amazon Data Lifecycle Manager__
  - ### Features
    - You can now add tags to a lifecycle policy in Data Lifecycle Manager (DLM). Tags allow you to categorize your policies in different ways, such as by department, purpose or owner. You can also enable resource level permissions based on tags to set access control on ability to modify or delete a tagged policy.

## __Amazon Simple Email Service__
  - ### Features
    - This is the first release of version 2 of the Amazon SES API. You can use this API to configure your Amazon SES account, and to send email. This API extends the functionality that exists in the previous version of the Amazon SES API.

# __2.10.15__ __2019-11-12__
## __AWS CodePipeline__
  - ### Features
    - AWS CodePipeline now supports the use of variables in action configuration.

## __AWS Marketplace Catalog Service__
  - ### Features
    - This is the first release for the AWS Marketplace Catalog service which allows you to list, describe and manage change requests on your published entities on AWS Marketplace.

## __Amazon DynamoDB__
  - ### Features
    - Amazon DynamoDB enables you to restore your data to a new DynamoDB table using a point-in-time or on-demand backup. You now can modify the settings on the new restored table. Specifically, you can exclude some or all of the local and global secondary indexes from being created with the restored table. In addition, you can change the billing mode and provisioned capacity settings.

## __Amazon Transcribe Service__
  - ### Features
    - With this release, Amazon Transcribe now supports transcriptions from audio sources in Welsh English (en-WL), Scottish English(en-AB), Irish English(en-IE), Farsi(fa-IR), Tamil(ta-IN), Indonesian(id-ID), Portuguese (pt-PT), Dutch(nl-NL).

## __Elastic Load Balancing__
  - ### Features
    - You can configure your Application Load Balancer to either drop invalid header fields or forward them to targets.

# __2.10.14__ __2019-11-11__
## __AWS CloudFormation__
  - ### Features
    - The Resource Import feature enables customers to import existing AWS resources into new or existing CloudFormation Stacks.

## __AWS Cost Explorer Service__
  - ### Features
    - This launch provides customers with access to GetCostAndUsageWithResources API.

## __Amazon Polly__
  - ### Features
    - Add `PollyPresigner` which enables support for presigning `SynthesizeSpeech` requests.

# __2.10.13__ __2019-11-08__
## __Amazon Cognito Identity__
  - ### Features
    - This release adds support for disabling classic flow.

## __Amazon EC2 Container Registry__
  - ### Features
    - This release contains ticket fixes for Amazon ECR.

# __2.10.12__ __2019-11-07__
## __AWS S3__
  - ### Features
    - Added support for presignPutObject in S3Presigner.

## __AWS SSO OIDC__
  - ### Features
    - This is an initial release of AWS Single Sign-On OAuth device code authorization service.

## __AWS Single Sign-On__
  - ### Features
    - This is an initial release of AWS Single Sign-On (SSO) end-user access. This release adds support for accessing AWS accounts assigned in AWS SSO using short term credentials.

## __Amazon Comprehend__
  - ### Features
    - This release adds new languages (ar, hi, ko, ja, zh, zh-TW) for Amazon Comprehend's DetectSentiment, DetectEntities, DetectKeyPhrases, BatchDetectSentiment, BatchDetectEntities and BatchDetectKeyPhrases APIs

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - AWS Systems Manager Session Manager target length increased to 400.

## __Netty NIO HTTP Client__
  - ### Features
    - Switch from setting the absolute URI in HTTP requests with no `Host` header to setting the absolute request path and query paramters and a `Host` header.

# __2.10.11__ __2019-11-06__
## __AWS SDK for Java v2__
  - ### Features
    - Added the web identity credentials provider to the default credential chain

## __AWS Savings Plans__
  - ### Features
    - This is the first release of Savings Plans, a new flexible pricing model that offers low prices on Amazon EC2 and AWS Fargate usage.

# __2.10.10__ __2019-11-06__
## __AWS Budgets__
  - ### Features
    - Documentation updates for budgets to track Savings Plans utilization and coverage

## __AWS CodeBuild__
  - ### Features
    - Add support for Build Number, Secrets Manager and Exported Environment Variables.

## __AWS Cost Explorer Service__
  - ### Features
    - This launch provides customers with access to Savings Plans management APIs.

## __AWS Savings Plans__
  - ### Features
    - This is the first release of Savings Plans, a new flexible pricing model that offers low prices on Amazon EC2 and AWS Fargate usage.

## __AWS Signer__
  - ### Features
    - This release adds support for tagging code-signing profiles in AWS Signer.

## __Amazon Elastic File System__
  - ### Features
    - EFS customers can select a lifecycle policy that automatically moves files that have not been accessed for 7 days into the EFS Infrequent Access (EFS IA) storage class. EFS IA provides price/performance that is cost-optimized for files that are not accessed every day.

# __2.10.9__ __2019-11-05__
## __AWS CodeStar Notifications__
  - ### Features
    - This release adds a notification manager for events in repositories, build projects, deployments, and pipelines. You can now configure rules and receive notifications about events that occur for resources. Each notification includes a status message as well as a link to the resource (repository, build project, deployment application, or pipeline) whose event generated the notification.

## __Amazon Relational Database Service__
  - ### Features
    - Documentation updates for Amazon RDS

# __2.10.8__ __2019-11-04__
## __AWS RoboMaker__
  - ### Features
    - RoboMaker Fleet Management launch a feature to verify your robot is ready to download and install the new robot application using a download condition file, which is a script run on the robot prior to downloading the new deployment.

## __Amazon DynamoDB Accelerator (DAX)__
  - ### Features
    - Documentation updates for dax

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for ec2

# __2.10.7__ __2019-11-01__
## __AWS CloudTrail__
  - ### Features
    - This release adds two new APIs, GetTrail and ListTrails, and support for adding tags when you create a trail by using a new TagsList parameter on CreateTrail operations.

## __AWS Database Migration Service__
  - ### Features
    - This release contains task timeline attributes in replication task statistics. This release also adds a note to the documentation for the CdcStartPosition task request parameter. This note describes how to enable the use of native CDC start points for a PostgreSQL source by setting the new slotName extra connection attribute on the source endpoint to the name of an existing logical replication slot.

## __Amazon Pinpoint__
  - ### Features
    - This release of the Amazon Pinpoint API introduces support for using and managing journeys, and querying analytics data for journeys.

# __2.10.6__ __2019-10-31__
## __AWS Amplify__
  - ### Features
    - This release of AWS Amplify Console introduces support for Web Previews. This feature allows user to create ephemeral branch deployments from pull request submissions made to a connected repository. A pull-request preview deploys every pull request made to your Git repository to a unique preview URL.

## __AWS Support__
  - ### Features
    - The status descriptions for TrustedAdvisorCheckRefreshStatus have been updated

## __Amazon Simple Storage Service__
  - ### Features
    - S3 Inventory now supports a new field 'IntelligentTieringAccessTier' that reports the access tier (frequent or infrequent) of objects stored in Intelligent-Tiering storage class.

# __2.10.5__ __2019-10-30__
## __Amazon ElastiCache__
  - ### Features
    - Amazon ElastiCache for Redis 5.0.5 now allows you to modify authentication tokens by setting and rotating new tokens. You can now modify active tokens while in use, or add brand-new tokens to existing encryption-in-transit enabled clusters that were previously setup without authentication tokens. This is a two-step process that allows you to set and rotate the token without interrupting client requests.

# __2.10.4__ __2019-10-29__
## __AWS Cloud9__
  - ### Features
    - Added CREATING and CREATE_FAILED environment lifecycle statuses.

## __Amazon AppStream__
  - ### Features
    - Adds support for providing domain names that can embed streaming sessions

# __2.10.3__ __2019-10-28__
## __Amazon Simple Storage Service__
  - ### Features
    - Adding support in SelectObjectContent for scanning a portion of an object specified by a scan range.

# __2.10.2__ __2019-10-28__
## __AWS Transfer for SFTP__
  - ### Features
    - This release adds logical directories support to your AWS SFTP server endpoint, so you can now create logical directory structures mapped to Amazon Simple Storage Service (Amazon S3) bucket paths for users created and stored within the service. Amazon S3 bucket names and paths can now be hidden from AWS SFTP users, providing an additional level of privacy to meet security requirements. You can lock down your SFTP users' access to designated folders (commonly referred to as 'chroot'), and simplify complex folder structures for data distribution through SFTP without replicating files across multiple users.

## __Amazon EC2 Container Registry__
  - ### Features
    - This release of Amazon Elastic Container Registry Service (Amazon ECR) introduces support for image scanning. This identifies the software vulnerabilities in the container image based on the Common Vulnerabilities and Exposures (CVE) database.

## __Amazon ElastiCache__
  - ### Features
    - Amazon ElastiCache adds support for migrating Redis workloads hosted on Amazon EC2 into ElastiCache by syncing the data between the source Redis cluster and target ElastiCache for Redis cluster in real time. For more information, see https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/migrate-to-elasticache.html.

# __2.10.1__ __2019-10-25__
## __Amazon Transcribe Streaming Service__
  - ### Features
    - With this release, Amazon Transcribe Streaming now supports audio sources in Australian English (en-AU).

# __2.10.0__ __2019-10-24__
## __AWS App Mesh__
  - ### Features
    - This release adds support for the gRPC and HTTP/2 protocols.

## __AWS SDK for Java v2__
  - ### Features
    - Updating to use Jackson 2.10.0 and Netty 4.1.42.Final

## __Amazon Chime__
  - ### Features
    - This release introduces Voice Connector PDX region and defaults previously created Voice Connectors to IAD. You can create Voice Connector Groups and add region specific Voice Connectors to direct telephony traffic across AWS regions in case of regional failures. With this release you can add phone numbers to Voice Connector Groups and can bulk move phone numbers between Voice Connectors, between Voice Connector and Voice Connector Groups and between Voice Connector Groups. Voice Connector now supports additional settings to enable SIP Log capture. This is in addition to the launch of Voice Connector Cloud Watch metrics in this release. This release also supports assigning outbound calling name (CNAM) to AWS account and individual phone numbers assigned to Voice Connectors. * Voice Connector now supports a setting to enable real time audio streaming delivered via Kinesis Audio streams. Please note that recording Amazon Chime Voice Connector calls with this feature maybe be subject to laws or regulations regarding the recording of telephone calls and other electronic communications. AWS Customer and their end users' have the responsibility to comply with all applicable laws regarding the recording, including properly notifying all participants in a recorded session or to a recorded communication that the session or communication is being recorded and obtain their consent.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release updates CreateFpgaImage to support tagging FPGA images on creation

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift offers expanded hardware options for game hosting: Custom game builds can use the Amazon Linux 2 operating system, and fleets for both custom builds and Realtime servers can now use C5, M5, and R5 instance types.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for the new family of Elastic Inference Accelerators (eia2) for SageMaker Hosting and Notebook Services

## __Netty NIO Http Client__
  - ### Bugfixes
    - Fix a race condition where the channel is closed right after all content is buffered, causing `server failed to complete the response` error by adding a flag when `LastHttpContentHandler` is received.

# __2.9.26__ __2019-10-23__
## __AWS Security Token Service__
  - ### Features
    - AWS Security Token Service (STS) now supports a regional configuration flag to make the client respect the region without the need for the endpoint parameter.

## __Amazon Connect Service__
  - ### Features
    - This release adds 4 new APIs ListQueues, ListPhoneNumbers, ListContactFlows, and ListHoursOfOperations, which can be used to programmatically list Queues, PhoneNumbers, ContactFlows, and HoursOfOperations configured for an Amazon Connect instance respectively. You can learn more about the new APIs here: https://docs.aws.amazon.com/connect/latest/APIReference/Welcome.html.

## __Amazon Polly__
  - ### Features
    - Amazon Polly adds new female voices: US Spanish - Lupe and Brazilian Portuguese - Camila; both voices are available in Standard and Neural engine.

# __2.9.25__ __2019-10-22__
## __AWS IoT Events__
  - ### Features
    - Add support for new serial evaluation method for events in a detector model.

## __AWS OpsWorks CM__
  - ### Features
    - AWS OpsWorks for Chef Automate (OWCA) now allows customers to use a custom domain and respective certificate, for their AWS OpsWorks For Chef Automate servers. Customers can now provide a CustomDomain, CustomCertificate and CustomPrivateKey in CreateServer API to configure their Chef Automate servers with a custom domain and certificate.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Better handle `GOAWAY` messages from the remote endpoint.

# __2.9.24__ __2019-10-18__
## __Amazon CloudWatch__
  - ### Features
    - New Period parameter added to MetricDataQuery structure.

## __Netty NIO Http Client__
  - ### Bugfixes
    - Update `HealthCheckedChannelPool` to check `KEEP_ALIVE` when acquiring a channel from the pool to avoid soon-to-be inactive channels being picked up by a new request. This should reduce the frequency of `IOException: Server failed to complete response` errors. See [#1380](https://github.com/aws/aws-sdk-java-v2/issues/1380), [#1466](https://github.com/aws/aws-sdk-java-v2/issues/1466).

# __2.9.23__ __2019-10-17__
## __AWS Batch__
  - ### Features
    - Adding support for Compute Environment Allocation Strategies

## __Amazon Relational Database Service__
  - ### Features
    - Amazon RDS now supports Amazon RDS on VMware with the introduction of APIs related to Custom Availability Zones and Media installation.

# __2.9.22__ __2019-10-16__
## __AWS Marketplace Commerce Analytics__
  - ### Features
    - add 2 more values for the supporting sections - age of past due funds + uncollected funds breakdown

## __AWS RoboMaker__
  - ### Features
    - This release adds support for ROS2 Dashing as a beta feature

## __Managed Streaming for Kafka__
  - ### Features
    - AWS MSK has added support for adding brokers to a cluster.

# __2.9.21__ __2019-10-15__
## __Amazon Kinesis Video Streams Archived Media__
  - ### Features
    - Add ON_DISCONTINUITY mode to the GetHLSStreamingSessionURL API

# __2.9.20__ __2019-10-14__
## __Amazon Personalize__
  - ### Features
    - AWS Personalize: Adds ability to create a solution version using FULL or UPDATE training mode

## __Amazon WorkSpaces__
  - ### Features
    - Documentation updates for WorkSpaces

# __2.9.19__ __2019-10-11__
## __AWS Greengrass__
  - ### Features
    - Greengrass OTA service supports Raspbian/Armv6l platforms.

# __2.9.18__ __2019-10-10__
## __AWS IoT Analytics__
  - ### Features
    - Add `completionTime` to API call ListDatasetContents.

## __AWS SDK for Java v2__
  - ### Features
    - Implement arn parser functions in `arns` module.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - New EC2 M5n, M5dn, R5n, R5dn instances with 100 Gbps network performance and Elastic Fabric Adapter (EFA) for ultra low latency; New A1.metal bare metal instance powered by AWS Graviton Processors

## __Amazon Lex Runtime Service__
  - ### Features
    - Amazon Lex now supports Session API checkpoints

## __Firewall Management Service__
  - ### Features
    - Firewall Manager now supports Amazon VPC security groups, making it easier to configure and manage security groups across multiple accounts from a single place.

# __2.9.17__ __2019-10-09__
## __AWS Elemental MediaConvert__
  - ### Features
    - AWS Elemental MediaConvert SDK has added support for Dolby Atmos encoding, up to 36 outputs, accelerated transcoding with frame capture and preferred acceleration feature.

## __AWS SDK for Java v2__
  - ### Features
    - Expose instance signature through EC2MetadataUtils

  - ### Bugfixes
    - Fix the implementations of `equals(Object)` and `hashCode()` for `DefaultSdkAutoConstructList` and `DefaultSdkAutoConstructMap` so that they follow the Java `equals` and `hashCode` contract. In addition, ensure that these implementations' `toString()` methods return nicely readable results. Fixes [#1445](https://github.com/aws/aws-sdk-java-v2/issues/1445)

## __Amazon ElastiCache__
  - ### Features
    - Amazon ElastiCache now allows you to apply available service updates on demand to your Memcached and Redis Cache Clusters. Features included: (1) Access to the list of applicable service updates and their priorities. (2) Service update monitoring and regular status updates. (3) Recommended apply-by-dates for scheduling the service updates. (4) Ability to stop and later re-apply updates. For more information, see https://docs.aws.amazon.com/AmazonElastiCache/latest/mem-ug/Self-Service-Updates.html and https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/Self-Service-Updates.html

## __Managed Streaming for Kafka__
  - ### Features
    - Updated documentation for Amazon Managed Streaming for Kafka service.

# __2.9.16__ __2019-10-08__
## __AWS DataSync__
  - ### Features
    - Add Sync options to enable/disable TaskQueueing

## __AWS Organizations__
  - ### Features
    - Documentation updates for organizations

## __AWS SDK for Java v2__
  - ### Features
    - EC2MetadataUtils: add marketplaceProductCodes inside InstanceInfo's POJO

## __Amazon EventBridge__
  - ### Features
    - Documentation updates for Amazon EventBridge.

## __Amazon Kinesis Firehose__
  - ### Features
    - With this release, you can use Amazon Kinesis Firehose delivery streams to deliver streaming data to Amazon Elasticsearch Service version 7.x clusters. For technical documentation, look for CreateDeliveryStream operation in Amazon Kinesis Firehose API reference.

## __Amazon S3 Control__
  - ### Features
    - Adds support for the Amazon S3 Control service to the SDK.

# __2.9.15__ __2019-10-07__
## __AWS Direct Connect__
  - ### Features
    - This release adds a service provider field for physical connection creation and provides a list of available partner providers for each Direct Connect location.

## __AWS Glue__
  - ### Features
    - AWS Glue now provides ability to use custom certificates for JDBC Connections.

## __Amazon Import/Export Snowball__
  - ### Features
    - AWS Snowball Edge now allows you to perform an offline update to the software of your Snowball Edge device when your device is not connected to the internet. Previously, updating your Snowball Edge's software required that the device be connected to the internet or be sent back to AWS. Now, you can keep your Snowball Edge software up to date even if your device(s) cannot connect to the internet, or are required to run in an air-gapped environment. To complete offline updates, download the software update from a client machine with connection to the internet using the AWS Command Line Interface (CLI). Then, have the Snowball Edge device download and install the software update using the Snowball Edge device API. For more information about offline updates, visit the Snowball Edge documentation page.

## __Amazon Kinesis Firehose__
  - ### Features
    - Amazon Kinesis Data Firehose now allows delivering data to Elasticsearch clusters set up in a different AWS account than the Firehose AWS account. For technical documentation, look for ElasticsearchDestinationConfiguration in the Amazon Kinesis Firehose API reference.

## __Amazon Pinpoint__
  - ### Features
    - This release of the Amazon Pinpoint API introduces support for using and managing message templates.

## __Amazon Pinpoint Email Service__
  - ### Features
    - This release of the Amazon Pinpoint Email API introduces support for using and managing message templates.

# __2.9.14__ __2019-10-04__
## __AWS Elemental MediaPackage__
  - ### Features
    - New Harvest Job APIs to export segment-accurate content windows from MediaPackage Origin Endpoints to S3. See https://docs.aws.amazon.com/mediapackage/latest/ug/harvest-jobs.html for more info

## __Amazon CloudWatch__
  - ### Bugfixes
    - Add cloudwatch specific http configurations, specifically reducing `connectionMaxIdleTime`. Related to [#1380](https://github.com/aws/aws-sdk-java-v2/issues/1380)

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release adds ClientMetadata input parameter to multiple Cognito User Pools operations, making this parameter available to the customer configured lambda triggers as applicable.

## __Amazon S3__
  - ### Bugfixes
    - Add s3 specific http configurations, specifically reducing `connectionMaxIdleTime`. Related to [#1122](https://github.com/aws/aws-sdk-java-v2/issues/1122)

## __Amazon S3 Control__
  - ### Features
    - Adds support for the Amazon S3 Control service to the SDK.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Documentation updates for Systems Manager / StartSession.

# __2.9.13__ __2019-10-03__
## __AWS Device Farm__
  - ### Features
    - Documentation updates for devicefarm

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release allows customers to purchase regional EC2 RIs on a future date.

## __Amazon Elasticsearch Service__
  - ### Features
    - Amazon Elasticsearch Service now supports configuring additional options for domain endpoint, such as whether to require HTTPS for all traffic.

## __Application Auto Scaling__
  - ### Features
    - Documentation updates for Application Auto Scaling

# __2.9.12__ __2019-10-02__
## __Amazon Lightsail__
  - ### Features
    - This release adds support for the automatic snapshots add-on for instances and block storage disks.

# __2.9.11__ __2019-10-01__
## __Amazon DocumentDB with MongoDB compatibility__
  - ### Features
    - This release provides support for describe and modify CA certificates.

# __2.9.10__ __2019-09-30__
## __AWS WAF__
  - ### Features
    - Lowering the threshold for Rate Based rule from 2000 to 100.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for creating a Read Replica with Active Directory domain information. This release updates RDS API to indicate whether an OrderableDBInstanceOption supports Kerberos Authentication.

## __AmazonMQ__
  - ### Features
    - Amazon MQ now includes the ability to scale your brokers by changing the host instance type. See the hostInstanceType property of UpdateBrokerInput (https://docs.aws.amazon.com/amazon-mq/latest/api-reference/brokers-broker-id.html#brokers-broker-id-model-updatebrokerinput), and pendingHostInstanceType property of DescribeBrokerOutput (https://docs.aws.amazon.com/amazon-mq/latest/api-reference/brokers-broker-id.html#brokers-broker-id-model-describebrokeroutput).

# __2.9.9__ __2019-09-27__
## __AWS Amplify__
  - ### Features
    - This release adds access logs APIs and artifact APIs for AWS Amplify Console.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Update the pool size for default async future completion executor service. See [#1251](https://github.com/aws/aws-sdk-java-v2/issues/1251), [#994](https://github.com/aws/aws-sdk-java-v2/issues/994)

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) removes FirelensConfiguration from the DescribeTask output during the FireLens public preview.

# __2.9.8__ __2019-09-26__
## __AWS CodePipeline__
  - ### Features
    - Documentation updates for CodePipeline

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release updates the AWS Systems Manager Parameter Store PutParameter and LabelParameterVersion APIs to return the "Tier" of parameter created/updated and the "parameter version" labeled respectively.

# __2.9.7__ __2019-09-25__
## __AWS Database Migration Service__
  - ### Features
    - This release adds a new DeleteConnection API to delete the connection between a replication instance and an endpoint. It also adds an optional S3 setting to specify the precision of any TIMESTAMP column values written to an S3 object file in .parquet format.

## __AWS Global Accelerator__
  - ### Features
    - API Update for AWS Global Accelerator to support for DNS aliasing.

## __Amazon SageMaker Service__
  - ### Features
    - Enable G4D and R5 instances in SageMaker Hosting Services

# __2.9.6__ __2019-09-24__
## __AWS Comprehend Medical__
  - ### Features
    - Use Amazon Comprehend Medical to analyze medical text stored in the specified Amazon S3 bucket. Use the console to create and manage batch analysis jobs, or use the batch APIs to detect both medical entities and protected health information (PHI). The batch APIs start, stop, list, and retrieve information about batch analysis jobs. This release also includes DetectEntitiesV2 operation which returns the Acuity and Direction entities as attributes instead of types.

## __AWS DataSync__
  - ### Features
    - Added S3StorageClass, OverwriteMode sync option, and ONLY_FILES_TRANSFERRED setting for the VerifyMode sync option.

## __Amazon Transcribe Service__
  - ### Features
    - With this update Amazon Transcribe enables you to provide an AWS KMS key to encrypt your transcription output.

# __2.9.5__ __2019-09-23__
## __AWS RDS DataService__
  - ### Features
    - RDS Data API now supports Amazon Aurora Serverless PostgreSQL databases.

## __Amazon Redshift__
  - ### Features
    - Adds API operation DescribeNodeConfigurationOptions and associated data structures.

# __2.9.4__ __2019-09-20__
## __AWS Greengrass__
  - ### Features
    - Greengrass OTA service now returns the updated software version in the PlatformSoftwareVersion parameter of a CreateSoftwareUpdateJob response

## __Amazon Elastic Compute Cloud__
  - ### Features
    - G4 instances are Amazon EC2 instances based on NVIDIA T4 GPUs and are designed to provide cost-effective machine learning inference for applications, like image classification, object detection, recommender systems, automated speech recognition, and language translation. G4 instances are also a cost-effective platform for building and running graphics-intensive applications, such as remote graphics workstations, video transcoding, photo-realistic design, and game streaming in the cloud. To get started with G4 instances visit https://aws.amazon.com/ec2/instance-types/g4.

## __Amazon Relational Database Service__
  - ### Features
    - Add a new LeaseID output field to DescribeReservedDBInstances, which shows the unique identifier for the lease associated with the reserved DB instance. AWS Support might request the lease ID for an issue related to a reserved DB instance.

## __Amazon WorkSpaces__
  - ### Features
    - Adds the WorkSpaces restore feature

# __2.9.3__ __2019-09-19__
## __AWS Glue__
  - ### Features
    - AWS Glue DevEndpoints now supports GlueVersion, enabling you to choose Apache Spark 2.4.3 (in addition to Apache Spark 2.2.1). In addition to supporting the latest version of Spark, you will also have the ability to choose between Python 2 and Python 3.

## __AWS MediaConnect__
  - ### Features
    - When you grant an entitlement, you can now specify the percentage of the entitlement data transfer that you want the subscriber to be responsible for.

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces support for container image manifest digests. This enables you to identify all tasks launched using a container image pulled from ECR in order to correlate what was built with where it is running.

# __2.9.2__ __2019-09-18__
## __AWS Resource Access Manager__
  - ### Features
    - AWS RAM provides a new ListPendingInvitationResources API action that lists the resources in a resource share that is shared with you but that the invitation is still pending for

## __AWS WAF Regional__
  - ### Features
    - Lowering the threshold for Rate Based rule from 2000 to 100.

## __Amazon API Gateway__
  - ### Features
    - Amazon API Gateway simplifies accessing PRIVATE APIs by allowing you to associate one or more Amazon Virtual Private Cloud (VPC) Endpoints to a private API. API Gateway will create and manage DNS alias records necessary for easily invoking the private APIs. With this feature, you can leverage private APIs in web applications hosted within your VPCs.

# __2.9.1__ __2019-09-17__
## __AWS Identity and Access Management__
  - ### Features
    - Documentation updates for iam

## __Amazon Athena__
  - ### Features
    - This release adds DataManifestLocation field indicating the location and file name of the data manifest file. Users can get a list of files that the Athena query wrote or intended to write from the manifest file.

## __Amazon Personalize__
  - ### Features
    - [Personalize] Adds trainingHours to solutionVersion properties.

# __2.9.0__ __2019-09-16__
## __AWS Elemental MediaConvert__
  - ### Features
    - AWS Elemental MediaConvert SDK has added support for multi-DRM SPEKE with CMAF outputs, MP3 ingest, and options for improved video quality.

## __AWS SDK for Java v2__
  - ### Features
    - Bump minor version to `2.9.0-SNAPSHOT` because of [#1413](https://github.com/aws/aws-sdk-java-v2/issues/1413).
    - Updating dependencies versions: jackson 2.9.8 -> 2.9.9, slf4j 1.7.35 -> 1.7.38, netty 4.1.39.Final -> 4.1.41.Final (contains the fix for the performance regression in 4.1.39)

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - This release lets customers add tags to an Amazon EKS cluster. These tags can be used to control access to the EKS API for managing the cluster using IAM. The Amazon EKS TagResource API allows customers to associate tags with their cluster. Customers can list tags for a cluster using the ListTagsForResource API and remove tags from a cluster with the UntagResource API. Note: tags are specific to the EKS cluster resource, they do not propagate to other AWS resources used by the cluster.

# __2.8.7__ __2019-09-12__
## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports High Efficiency Video Coding (HEVC) for standard-definition (SD), high-definition (HD), and ultra-high-definition (UHD) encoding with HDR support.Encoding with HEVC offers a number of advantages. While UHD video requires an advanced codec beyond H.264 (AVC), high frame rate (HFR) or High Dynamic Range (HDR) content in HD also benefit from HEVC's advancements. In addition, benefits can be achieved with HD and SD content even if HDR and HFR are not needed.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Fix for FleetActivityStatus and FleetStateCode enum

## __Amazon WorkMail Message Flow__
  - ### Features
    - This release allows customers to access email messages as they flow to and from Amazon WorkMail.

## __Elastic Load Balancing__
  - ### Features
    - Documentation updates for elasticloadbalancingv2: This release adds support for TLS SNI on Network Load Balancers

# __2.8.6__ __2019-09-11__
## __AWS Config__
  - ### Features
    - Adding input validation for the OrganizationConfigRuleName string.

## __AWS MediaConnect__
  - ### Features
    - This release adds support for the RIST protocol on sources and outputs.

## __AWS Step Functions__
  - ### Features
    - Fixing letter case in Map history event details to be small case

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for new data fields and log format in VPC flow logs.

## __Amazon Relational Database Service__
  - ### Features
    - This release allows customers to specify a custom parameter group when creating a Read Replica, for DB engines which support this feature.

## __Amazon Simple Email Service__
  - ### Features
    - Updated API documentation to correct broken links, and to update content based on customer feedback.

# __2.8.5__ __2019-09-10__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix marshalling for models with xml attribute. See [#1182](https://github.com/aws/aws-sdk-java-v2/issues/1182)

## __AWS Storage Gateway__
  - ### Features
    - The CloudWatchLogGroupARN parameter of the UpdateGatewayInformation API allows for configuring the gateway to use a CloudWatch log-group where Storage Gateway health events will be logged.

# __2.8.4__ __2019-09-09__
## __AWS App Mesh__
  - ### Features
    - This release adds support for http retry policies.

## __AWS Marketplace Commerce Analytics__
  - ### Features
    - Add FDP+FPS (monthly_revenue_field_demonstration_usage + monthly_revenue_flexible_payment_schedule) to Marketplace Commerce Analytics Service

## __AWS RoboMaker__
  - ### Features
    - Support for Connectivity to Simulation. When you need to interact with the applications in your simulation job, you can connect to your robot application or simulation application with port forwarding. When you configure port forwarding, traffic will be forwarded from the simulation job port to the application port. Port forwarding makes it easy to connect with tools such as ROS Bridge and other tools. This can be useful when you want to debug or run custom tools to interact with your applications.

## __Amazon AppStream__
  - ### Features
    - IamRoleArn support in CreateFleet, UpdateFleet, CreateImageBuilder APIs

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release expands Site-to-Site VPN tunnel options to allow customers to restrict security algorithms and configure timer settings for VPN connections. Customers can specify these new options while creating new VPN connections, or they can modify the tunnel options on existing connections using a new API.

## __Amazon QLDB__
  - ### Features
    - (New Service) Amazon QLDB is a fully managed ledger database that provides a transparent, immutable, and cryptographically verifiable transaction log owned by a central trusted authority. Amazon QLDB is a new class of serverless database that eliminates the need to engage in the complex development effort of building your own ledger-like applications and it automatically scales to support the demands of your application. Introduces Amazon QLDB API operations needed for managing Amazon QLDB ledgers. This includes the ability to manage Amazon QLDB ledgers, cryptographically verify documents, and export the journal in a ledger.

## __Amazon QLDB Session__
  - ### Features
    - (New Service) Amazon QLDB is a fully managed ledger database that provides a transparent, immutable, and cryptographically verifiable transaction log owned by a central trusted authority. Amazon QLDB is a new class of serverless database that eliminates the need to engage in the complex development effort of building your own ledger-like applications and it automatically scales to support the demands of your application. Introduces Amazon QLDB API operations needed for interacting with data in Amazon QLDB ledgers.

# __2.8.3__ __2019-09-06__
## __Amazon Kinesis Analytics__
  - ### Features
    - Documentation updates for kinesisanalytics

# __2.8.2__ __2019-09-05__
## __AWS Config__
  - ### Features
    - AWS Config now includes the option for marking RemediationConfigurations as automatic, removing the need to call the StartRemediationExecution API. Manual control over resource execution rate is also included, and RemediationConfigurations are now ARN addressable. Exceptions to exclude account resources from being remediated can be configured with the new PutRemediationExceptions, DescribeRemediationExceptions, and DeleteRemediationExceptions APIs.

# __2.8.1__ __2019-09-04__
## __AWS Step Functions__
  - ### Features
    - Added support for new history events

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Amazon EKS DescribeCluster API returns a new OIDC issuer field that can be used to create OIDC identity provider for IAM for Service Accounts feature.

## __Amazon Transcribe Service__
  - ### Features
    - MediaFormat is now optional for StartTranscriptionJob API.

# __2.8.0__ __2019-09-03__
## __AWS Resource Groups Tagging API__
  - ### Features
    - Documentation updates for resourcegroupstaggingapi

## __AWS SDK for Java v2__
  - ### Features
    - Bump minor version to 2.8.0-SNAPSHOT because of [#1391](https://github.com/aws/aws-sdk-java-v2/issues/1391).
    - Upgrade Netty version to 4.1.39.Final, netty reactive streams version to 2.0.3, netty open ssl version to 2.0.25.Final

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces support for attaching Amazon Elastic Inference accelerators to your containers. This enables you to run deep learning inference workloads with hardware acceleration in a more efficient way.

## __Amazon GameLift__
  - ### Features
    - You can now make use of PKI resources to provide more secure connections between your game clients and servers. To learn more, please refer to the public Amazon GameLift documentation.

# __2.7.36__ __2019-08-30__
## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces support for modifying the cluster settings for existing clusters, which enables you to toggle whether Container Insights is enabled or not. Support is also introduced for custom log routing using the ECS FireLens integration.

## __AmazonApiGatewayManagementApi__
  - ### Features
    - You can use getConnection to return information about the connection (when it is connected, IP address, etc) and deleteConnection to disconnect the given connection

## __AmazonMQ__
  - ### Features
    - Adds support for updating security groups selection of an Amazon MQ broker.

# __2.7.35__ __2019-08-29__
## __AWS CodePipeline__
  - ### Features
    - Introducing pipeline execution trigger details in ListPipelineExecutions API.

## __AWS Lambda__
  - ### Features
    - Adds a "MaximumBatchingWindowInSeconds" parameter to event source mapping api's. Usable by Dynamodb and Kinesis event sources.

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces support for including Docker container IDs in the API response when describing and stopping tasks. This enables customers to easily map containers to the tasks they are associated with.

## __Amazon ElastiCache__
  - ### Features
    - Amazon ElastiCache for Redis now supports encryption at rest using customer managed customer master keys (CMKs) in AWS Key Management Service (KMS). Amazon ElastiCache now supports cluster names upto 40 characters for replicationGoups and upto 50 characters for cacheClusters.

## __Application Auto Scaling__
  - ### Features
    - With the current release, you can suspend and later resume any of the following scaling actions in Application Auto Scaling: scheduled scaling actions, dynamic scaling in actions, dynamic scaling out actions.

# __2.7.34__ __2019-08-28__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds the ability to send a job to an on-demand queue while simulating the performance of a job sent to a reserved queue. Use this setting to estimate the number of reserved transcoding slots (RTS) you need for a reserved queue.

## __AWS Global Accelerator__
  - ### Features
    - API Update for AWS Global Accelerator Client IP Preservation

## __Amazon Simple Queue Service__
  - ### Features
    - Added support for message system attributes, which currently lets you send AWS X-Ray trace IDs through Amazon SQS.

# __2.7.33__ __2019-08-27__
## __AWS Organizations__
  - ### Features
    - Documentation updates for organizations

# __2.7.32__ __2019-08-26__
## __AWS SecurityHub__
  - ### Features
    - This release resolves an issue with the DescribeHub action, changes the MasterId and InvitationId parameters for AcceptInvitation to Required, and changes the AccountIds parameter for DeleteInvitations and DeclineInvitations to Required.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This feature adds "default tier" to the AWS Systems Manager Parameter Store for parameter creation and update. AWS customers can now set the "default tier" to one of the following values: Standard (default), Advanced or Intelligent-Tiering. This allows customers to create advanced parameters or parameters in corresponding tiers with one setting rather than code change to specify parameter tiers.

# __2.7.31__ __2019-08-23__
## __AWS Elemental MediaPackage VOD__
  - ### Features
    - Adds optional Constant Initialization Vector (IV) to HLS Encryption for MediaPackage VOD.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release of EC2 VM Import Export adds support for exporting Amazon Machine Image(AMI)s to a VM file

## __Amazon Transcribe Service__
  - ### Features
    - Amazon Transcribe - support transcriptions from audio sources in Russian (ru-RU) and Chinese (zh-CN).

# __2.7.30__ __2019-08-22__
## __AWS DataSync__
  - ### Features
    - This release adds support for SMB location type.

## __Amazon Relational Database Service__
  - ### Features
    - This release allows users to enable RDS Data API while creating Aurora Serverless databases.

# __2.7.29__ __2019-08-21__
## __Amazon DynamoDB__
  - ### Features
    - Public preview version of 'dynamodb-enhanced' that has a new DynamoDb mapper library that can be used with the v2 SDK. See README.md in the module for more detailed information about this module.

## __Amazon ElastiCache__
  - ### Features
    - ElastiCache extends support for Scale down for Redis Cluster-mode enabled and disabled replication groups

## __Amazon Forecast Query Service__
  - ### Features
    - Amazon Forecast is a fully managed machine learning service that makes it easy for customers to generate accurate forecasts using their historical time-series data

## __Amazon Forecast Service__
  - ### Features
    - Amazon Forecast is a fully managed machine learning service that makes it easy for customers to generate accurate forecasts using their historical time-series data

## __Amazon Personalize Runtime__
  - ### Features
    - Increased limits on number of items recommended and reranked: The maximum number of results returned from getRecommendations API has been increased to 200. The maximum number of items which can be reranked via getPersonalizedRanking API has been increased to 200.

## __Amazon Rekognition__
  - ### Features
    - Documentation updates for Amazon Rekognition.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker now supports Amazon EFS and Amazon FSx for Lustre file systems as data sources for training machine learning models. Amazon SageMaker now supports running training jobs on ml.p3dn.24xlarge instance type. This instance type is offered as a limited private preview for certain SageMaker customers. If you are interested in joining the private preview, please reach out to the SageMaker Product Management team via AWS Support."

## __Amazon Simple Queue Service__
  - ### Features
    - This release provides a way to add metadata tags to a queue when it is created. You can use tags to organize and identify your Amazon SQS queues for cost allocation.

## __Apache HTTP Client__
  - ### Features
    - Enable TLS client authentication support for the Apache HTTP Client by allowing customers to specify a `TlsKeyManagersProvider` on the builder. The `KeyManger`s provided will be used when the remote server wants to authenticate the client.

## __HTTP Client SPI__
  - ### Features
    - Add `TlsKeyManagersProvider` interface for supporting TLS client auth in HTTP client implementations.

## __Netty NIO HTTP Client__
  - ### Features
    - Add ability to to use HTTP proxies with the Netty async client.

# __2.7.28__ __2019-08-20__
## __AWS Transfer for SFTP__
  - ### Features
    - New field in response of TestIdentityProvider

## __Alexa For Business__
  - ### Features
    - Adding support for optional locale input in CreateProfile and UpdateProfile APIs

## __Amazon AppStream__
  - ### Features
    - Includes API updates to support streaming through VPC endpoints for image builders and stacks.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker introduces Managed Spot Training. Increases the maximum number of metric definitions to 40 for SageMaker Training and Hyperparameter Tuning Jobs. SageMaker Neo adds support for Acer aiSage and Qualcomm QCS605 and QCS603.

# __2.7.27__ __2019-08-19__
## __AWS App Mesh__
  - ### Features
    - Fix for HttpMethod enum

## __AWS Cost and Usage Report Service__
  - ### Features
    - New IAM permission required for editing AWS Cost and Usage Reports - Starting today, you can allow or deny IAM users permission to edit Cost & Usage Reports through the API and the Billing and Cost Management console. To allow users to edit Cost & Usage Reports, ensure that they have 'cur: ModifyReportDefinition' permission. Refer to the technical documentation (https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_cur_ModifyReportDefinition.html) for additional details.

# __2.7.26__ __2019-08-16__
## __AWS RoboMaker__
  - ### Features
    - Two feature release: 1. AWS RoboMaker introduces log-based simulation. Log-based simulation allows you to play back pre-recorded log data such as sensor streams for testing robotic functions like localization, mapping, and object detection. Use the AWS RoboMaker SDK to test your robotic applications. 2. AWS RoboMaker allow customer to setup a robot deployment timeout when CreateDeploymentJob.

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces support for controlling the usage of swap space on a per-container basis for Linux containers.

## __Amazon Elastic MapReduce__
  - ### Features
    - Amazon EMR has introduced an account level configuration called Block Public Access that allows you to block clusters with ports open to traffic from public IP sources (i.e. 0.0.0.0/0 for IPv4 and ::/0 for IPv6) from launching. Individual ports or port ranges can be added as exceptions to allow public access.

# __2.7.25__ __2019-08-15__
## __AWS App Mesh__
  - ### Features
    - This release adds support for http header based routing and route prioritization.

## __AWS CodeCommit__
  - ### Features
    - This release adds an API, BatchGetCommits, that allows retrieval of metadata for multiple commits in an AWS CodeCommit repository.

## __AWS Glue__
  - ### Features
    - GetJobBookmarks API is withdrawn.

## __AWS Storage Gateway__
  - ### Features
    - CreateSnapshotFromVolumeRecoveryPoint API supports new parameter: Tags (to be attached to the created resource)

## __Amazon Athena__
  - ### Features
    - This release adds support for querying S3 Requester Pays buckets. Users can enable this feature through their Workgroup settings.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds an option to use private certificates from AWS Certificate Manager (ACM) to authenticate a Site-to-Site VPN connection's tunnel endpoints and customer gateway device.

# __2.7.24__ __2019-08-14__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds a new API called SendDiagnosticInterrupt, which allows you to send diagnostic interrupts to your EC2 instance.

# __2.7.23__ __2019-08-13__
## __AWS AppSync__
  - ### Features
    - Adds a configuration option for AppSync GraphQL APIs

# __2.7.22__ __2019-08-12__
## __Amazon CloudWatch__
  - ### Features
    - Documentation updates for monitoring

## __Amazon Rekognition__
  - ### Features
    - Adding new Emotion, Fear

## __Application Auto Scaling__
  - ### Features
    - Documentation updates for Application Auto Scaling

## __Auto Scaling__
  - ### Features
    - Amazon EC2 Auto Scaling now supports a new Spot allocation strategy "capacity-optimized" that fulfills your request using Spot Instance pools that are optimally chosen based on the available Spot capacity.

# __2.7.21__ __2019-08-09__
## __AWS Elemental MediaConvert__
  - ### Features
    - AWS Elemental MediaConvert has added support for multi-DRM SPEKE with CMAF outputs, MP3 ingest, and options for improved video quality.

## __AWS IoT__
  - ### Features
    - This release adds Quality of Service (QoS) support for AWS IoT rules engine republish action.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed the issue where ByteArrayAsyncRequestBody can send duplicate requests when another request comes in at the same time the subscription completes.
    - For APIs that support input event streams, set the `Content-Type` to `application/vnd.amazon.eventstream` on the request.

## __Amazon GuardDuty__
  - ### Features
    - New "evidence" field in the finding model to provide evidence information explaining why the finding has been triggered. Currently only threat-intelligence findings have this field. Some documentation updates.

## __Amazon Lex Runtime Service__
  - ### Features
    - Manage Amazon Lex session state using APIs on the client

## __Amazon Redshift__
  - ### Features
    - Add expectedNextSnapshotScheduleTime and expectedNextSnapshotScheduleTimeStatus to redshift cluster object.

# __2.7.20__ __2019-08-08__
## __AWS CodeBuild__
  - ### Features
    - CodeBuild adds CloudFormation support for SourceCredential

## __AWS Glue__
  - ### Features
    - You can now use AWS Glue to find matching records across dataset even without identifiers to join on by using the new FindMatches ML Transform. Find related products, places, suppliers, customers, and more by teaching a custom machine learning transformation that you can use to identify matching matching records as part of your analysis, data cleaning, or master data management project by adding the FindMatches transformation to your Glue ETL Jobs. If your problem is more along the lines of deduplication, you can use the FindMatches in much the same way to identify customers who have signed up more than ones, products that have accidentally been added to your product catalog more than once, and so forth. Using the FindMatches MLTransform, you can teach a Transform your definition of a duplicate through examples, and it will use machine learning to identify other potential duplicates in your dataset. As with data integration, you can then use your new Transform in your deduplication projects by adding the FindMatches transformation to your Glue ETL Jobs. This release also contains additional APIs that support AWS Lake Formation.

## __AWS Lake Formation__
  - ### Features
    - Lake Formation: (New Service) AWS Lake Formation is a fully managed service that makes it easier for customers to build, secure and manage data lakes. AWS Lake Formation simplifies and automates many of the complex manual steps usually required to create data lakes including collecting, cleaning and cataloging data and securely making that data available for analytics and machine learning.

## __AWS OpsWorks CM__
  - ### Features
    - This release adds support for Chef Automate 2 specific engine attributes.

# __2.7.19__ __2019-08-07__
## __Amazon CloudWatch Application Insights__
  - ### Features
    - CloudWatch Application Insights for .NET and SQL Server now provides integration with AWS Systems Manager OpsCenter. This integration allows you to view and resolve problems and operational issues detected for selected applications.

# __2.7.18__ __2019-08-06__
## __AWS Batch__
  - ### Features
    - Documentation updates for AWS Batch

# __2.7.17__ __2019-08-05__
## __AWS DataSync__
  - ### Features
    - Support VPC endpoints.

## __AWS IoT__
  - ### Features
    - In this release, AWS IoT Device Defender introduces audit mitigation actions that can be applied to audit findings to help mitigate security issues.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 now supports a new Spot allocation strategy "Capacity-optimized" that fulfills your request using Spot Instance pools that are optimally chosen based on the available Spot capacity.

# __2.7.16__ __2019-08-02__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix the issue where the `content-length` set on the request is not honored for streaming operations.

## __AWS Security Token Service__
  - ### Features
    - Documentation updates for sts

# __2.7.15__ __2019-07-30__
## __AWS Elemental MediaConvert__
  - ### Features
    - MediaConvert adds support for specifying priority (-50 to 50) on jobs submitted to on demand or reserved queues

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed the issue where `AsyncResponseHandler#prepare` was not invoked before `#onHeaders`. See [#1343](https://github.com/aws/aws-sdk-java-v2/issues/1343).

## __Amazon Polly__
  - ### Features
    - Amazon Polly adds support for Neural text-to-speech engine.

## __Amazon Route 53__
  - ### Features
    - Amazon Route 53 now supports the Middle East (Bahrain) Region (me-south-1) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region.

# __2.7.14__ __2019-07-29__
## __AWS CodeCommit__
  - ### Features
    - This release supports better exception handling for merges.

## __Netty NIO Http Client__
  - ### Bugfixes
    - Update `HandlerRemovingChannelPool` to only remove per request handlers if the channel is open or registered to avoid the race condition when the DefaultChannelPipeline is trying to removing the handler at the same time, causing `NoSuchElementException`.

# __2.7.13__ __2019-07-26__
## __AWS Batch__
  - ### Features
    - AWS Batch now supports SDK auto-pagination and Job-level docker devices.

## __AWS Cost Explorer Service__
  - ### Features
    - Adds support for resource optimization recommendations.

## __AWS Glue__
  - ### Features
    - This release provides GetJobBookmark and GetJobBookmarks APIs. These APIs enable users to look at specific versions or all versions of the JobBookmark for a specific job. This release also enables resetting the job bookmark to a specific run via an enhancement of the ResetJobBookmark API.

## __AWS Greengrass__
  - ### Features
    - Greengrass OTA service supports openwrt/aarch64 and openwrt/armv7l platforms.

## __AWS MediaConnect__
  - ### Features
    - This release adds support for the Zixi pull protocol on outputs.

## __Amazon CloudWatch Logs__
  - ### Features
    - Allow for specifying multiple log groups in an Insights query, and deprecate storedByte field for LogStreams and interleaved field for FilterLogEventsRequest.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - You can now create EC2 Capacity Reservations using Availability Zone ID or Availability Zone name. You can view usage of Amazon EC2 Capacity Reservations per AWS account.

# __2.7.12__ __2019-07-25__
## __AWS Elemental MediaConvert__
  - ### Features
    - AWS Elemental MediaConvert has added several features including support for: audio normalization using ITU BS.1770-3, 1770-4 algorithms, extension of job progress indicators, input cropping rectangle & output position rectangle filters per input, and dual SCC caption mapping to additional codecs and containers.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive is adding Input Clipping, Immediate Mode Input Switching, and Dynamic Inputs.

## __Amazon EC2 Container Registry__
  - ### Features
    - This release adds support for immutable image tags.

# __2.7.11__ __2019-07-24__
## __AWS Glue__
  - ### Features
    - This release provides GlueVersion option for Job APIs and WorkerType option for DevEndpoint APIs. Job APIs enable users to pick specific GlueVersion for a specific job and pin the job to a specific runtime environment. DevEndpoint APIs enable users to pick different WorkerType for memory intensive workload.

## __AWS Security Token Service__
  - ### Features
    - New STS GetAccessKeyInfo API operation that returns the account identifier for the specified access key ID.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release introduces support for split tunnel with AWS Client VPN, and also adds support for opt-in Regions in DescribeRegions API. In addition, customers can now also tag Launch Templates on creation.

## __Amazon Pinpoint__
  - ### Features
    - This release adds support for programmatic access to many of the same campaign metrics that are displayed on the Amazon Pinpoint console. You can now use the Amazon Pinpoint API to monitor and assess performance data for campaigns, and integrate metrics data with other reporting tools. We update the metrics data continuously, resulting in a data latency timeframe that is limited to approximately two hours.

# __2.7.10__ __2019-07-23__
## __AWS Secrets Manager__
  - ### Features
    - This release increases the maximum allowed size of SecretString or SecretBinary from 7KB to 10KB in the CreateSecret, UpdateSecret, PutSecretValue and GetSecretValue APIs. This release also increases the maximum allowed size of ResourcePolicy from 4KB to 20KB in the GetResourcePolicy and PutResourcePolicy APIs.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - You can now use Maintenance Windows to select a resource group as the target. By selecting a resource group as the target of a Maintenance Window, customers can perform routine tasks across different resources such as Amazon Elastic Compute Cloud (AmazonEC2) instances, Amazon Elastic Block Store (Amazon EBS) volumes, and Amazon Simple Storage Service(Amazon S3) buckets within the same recurring time window.

# __2.7.9__ __2019-07-22__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix model builder enum fluent setters to check null before calling `toString` to avoid NPE.

## __AWS Shield__
  - ### Features
    - Adding new VectorType (HTTP_Reflection) and related top contributor types to describe WordPress Pingback DDoS attacks.

## __AmazonMQ__
  - ### Features
    - Adds support for AWS Key Management Service (KMS) to offer server-side encryption. You can now select your own customer managed CMK, or use an AWS managed CMK in your KMS account.

# __2.7.8__ __2019-07-19__
## __AWS IoT Events__
  - ### Features
    - Adds support for IoT Events, Lambda, SQS and Kinesis Firehose actions.

## __Amazon Simple Queue Service__
  - ### Features
    - This release updates the information about the availability of FIFO queues and includes miscellaneous fixes.

# __2.7.7__ __2019-07-18__
## __AWS CodeDeploy__
  - ### Features
    - Documentation updates for codedeploy

## __Amazon Comprehend__
  - ### Features
    - Amazon Comprehend now supports multiple entities for custom entity recognition

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces support for cluster settings. Cluster settings specify whether CloudWatch Container Insights is enabled or disabled for the cluster.

## __Amazon ElastiCache__
  - ### Features
    - Updates for Elasticache

# __2.7.6__ __2019-07-17__
## __AWS Config__
  - ### Features
    - This release adds more granularity to the status of an OrganizationConfigRule by adding a new status. It also adds an exception when organization access is denied.

## __AWS Database Migration Service__
  - ### Features
    - S3 endpoint settings update: 1) Option to append operation column to full-load files. 2) Option to add a commit timestamp column to full-load and cdc files. Updated DescribeAccountAttributes to include UniqueAccountIdentifier.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Notify error by invoking `AsyncResponseTransformer#exceptionOccurred` for streaming operations for services using xml protocol such as S3 when the request fails and is not retriable.

## __Auto Scaling__
  - ### Features
    - Documentation updates for autoscaling

# __2.7.5__ __2019-07-12__
## __AWS Identity and Access Management__
  - ### Features
    - Removed exception that was indicated but never thrown for IAM GetAccessKeyLastUsed API

## __AWS RoboMaker__
  - ### Features
    - Added Melodic as a supported Robot Software Suite Version

## __AWS SDK for Java v2__
  - ### Features
    - Introduce a new method `equalsBySdkFields` to compare only non-inherited fields for model classes.

  - ### Bugfixes
    - Fix `AwsSessionCredentials#equals` to not compare super because the super is an interface.
    - Fix the bug where `equals` and `hashCode` methods in the AWS service request and response classes were not calling super.

## __Amazon Elasticsearch Service__
  - ### Features
    - Amazon Elasticsearch Service now supports M5, C5, and R5 instance types.

## __AmazonApiGatewayV2__
  - ### Features
    - Bug fix (Add tags field to Update Stage , Api and DomainName Responses )

# __2.7.4__ __2019-07-11__
## __Amazon CloudWatch Events__
  - ### Features
    - Adds APIs for partner event sources, partner event buses, and custom event buses. These new features are managed in the EventBridge service.

## __Amazon EventBridge__
  - ### Features
    - Amazon EventBridge is a serverless event bus service that makes it easy to connect your applications with data from a variety of sources, including AWS services, partner applications, and your own applications.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fix the issue where the SDK can invoke `channel#read` twice per request and buffer content aggressively before the subscriber is able to consume the data. This should fix [#1122](https://github.com/aws/aws-sdk-java-v2/issues/1122).

# __2.7.3__ __2019-07-10__
## __AWS SDK for Java v2__
  - ### Features
    - Automatically retry on CRC32 checksum validation failures when the service returns a CRC32 checksum that differs from the one the SDK calculated. Previously this would just throw an exception.

## __AWS Service Catalog__
  - ### Features
    - This release adds support for Parameters in ExecuteProvisionedProductServiceAction and adds functionality to get the default parameter values for a Self-Service Action execution against a Provisioned Product via DescribeServiceActionExecutionParameters

## __Amazon Glacier__
  - ### Features
    - Documentation updates for glacier

## __Amazon QuickSight__
  - ### Features
    - Amazon QuickSight now supports embedding dashboards for all non-federated QuickSight users. This includes IAM users, AD users and users from the QuickSight user pool. The get-dashboard-embed-url API accepts QUICKSIGHT as identity type with a user ARN to authenticate the embeddable dashboard viewer as a non-federated user.

# __2.7.2__ __2019-07-09__
## __AWS Amplify__
  - ### Features
    - This release adds webhook APIs and manual deployment APIs for AWS Amplify Console.

## __AWS Config__
  - ### Features
    - AWS Config now supports a new set of APIs to manage AWS Config rules across your organization in AWS Organizations. Using this capability, you can centrally create, update, and delete AWS Config rules across all accounts in your organization. This capability is particularly useful if you have a need to deploy a common set of AWS Config rules across all accounts. You can also specify accounts where AWS Config rules should not be created. In addition, you can use these APIs from the master account in AWS Organizations to enforce governance by ensuring that the underlying AWS Config rules are not modifiable by your organization member accounts.These APIs work for both managed and custom AWS Config rules. For more information, see Enabling AWS Config Rules Across all Accounts in Your Organization in the AWS Config Developer Guide.The new APIs are available in all commercial AWS Regions where AWS Config and AWS Organizations are supported. For the full list of supported Regions, see AWS Regions and Endpoints in the AWS General Reference. To learn more about AWS Config, visit the AWS Config webpage. To learn more about AWS Organizations, visit the AWS Organizations webpage.

## __AWS WAF__
  - ### Features
    - Updated SDK APIs to add tags to WAF Resources: WebACL, Rule, Rulegroup and RateBasedRule. Tags can also be added during creation of these resources.

## __AWS WAF Regional__
  - ### Features
    - Updated SDK APIs to add tags to WAF Resources: WebACL, Rule, Rulegroup and RateBasedRule. Tags can also be added during creation of these resources.

## __Amazon CloudWatch__
  - ### Features
    - This release adds three new APIs (PutAnomalyDetector, DeleteAnomalyDetector, and DescribeAnomalyDetectors) to support the new feature, CloudWatch Anomaly Detection. In addition, PutMetricAlarm and DescribeAlarms APIs are updated to support management of Anomaly Detection based alarms.

## __Amazon Elastic File System__
  - ### Features
    - EFS customers can now enable Lifecycle Management for all file systems. You can also now select from one of four Lifecycle Management policies (14, 30, 60 and 90 days), to automatically move files that have not been accessed for the period of time defined by the policy, from the EFS Standard storage class to the EFS Infrequent Access (IA) storage class. EFS IA provides price/performance that is cost-optimized for files that are not accessed every day.

## __Amazon GameLift__
  - ### Features
    - GameLift FlexMatch now supports matchmaking of up to 200 players per game session, and FlexMatch can now automatically backfill your game sessions whenever there is an open slot.

## __Amazon Kinesis Video Streams__
  - ### Features
    - Add "GET_DASH_STREAMING_SESSION_URL" as an API name to the GetDataEndpoint API.

## __Amazon Kinesis Video Streams Archived Media__
  - ### Features
    - Adds support for the GetDASHStreamingSessionURL API. Also adds support for the Live Replay playback mode of the GetHLSStreamingSessionURL API.

## __Netty NIO HTTP Client__
  - ### Features
    - Improved error messaging when a connection is closed. Fixes [#1260](https://github.com/aws/aws-sdk-java-v2/issues/1260).

# __2.7.1__ __2019-07-08__
## __AWS Cost Explorer Service__
  - ### Features
    - This release introduces a new operation called GetUsageForecast, which allows you to programmatically access AWS Cost Explorer's forecasting engine on usage data (running hours, data transfer, etc).

# __2.7.0__ __2019-07-03__
## __AWS SDK for Java v2__
  - ### Features
    - Update Apache http client version to `4.5.9`.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - AssignPrivateIpAddresses response includes two new fields: AssignedPrivateIpAddresses, NetworkInterfaceId

## __Amazon Relational Database Service__
  - ### Features
    - This release supports Cross-Account Cloning for Amazon Aurora clusters.

## __Amazon Simple Storage Service__
  - ### Features
    - Add S3 x-amz-server-side-encryption-context support.

## __Amazon Simple Workflow Service__
  - ### Features
    - This release adds APIs that allow adding and removing tags to a SWF domain, and viewing tags for a domain. It also enables adding tags when creating a domain.

## __Apache Http Client__
  - ### Bugfixes
    - Disable apache normalization to handle breaking change introduced in apache httpclient `4.5.7`. See [aws/aws-sdk-java[#1919](https://github.com/aws/aws-sdk-java-v2/issues/1919)](https://github.com/aws/aws-sdk-java/issues/1919) for more information.

# __2.6.5__ __2019-07-02__
## __AWS Elemental MediaStore__
  - ### Features
    - This release adds support for tagging, untagging, and listing tags for AWS Elemental MediaStore containers.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix a bug in `FileAsyncResponseTransformer` that causes the future from `prepare()` to not be completed if `onError` is called on its `Subscriber` wile consuming the response stream. Fixes [#1279](https://github.com/aws/aws-sdk-java-v2/issues/1279)

## __Amazon AppStream__
  - ### Features
    - Adding ImageBuilderName in Fleet API and Documentation updates for AppStream.

# __2.6.4__ __2019-07-01__
## __AWS Organizations__
  - ### Features
    - Specifying the tag key and tag value is required for tagging requests.

## __Amazon DocumentDB with MongoDB compatibility__
  - ### Features
    - This release provides support for cluster delete protection and the ability to stop and start clusters.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for specifying a maximum hourly price for all On-Demand and Spot instances in both Spot Fleet and EC2 Fleet.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for RDS DB Cluster major version upgrade

# __2.6.3__ __2019-06-28__
## __Alexa For Business__
  - ### Features
    - This release allows developers and customers to add SIP addresses and international phone numbers to contacts.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - You can now launch 8xlarge and 16xlarge instance sizes on the general purpose M5 and memory optimized R5 instance types.

## __Amazon Redshift__
  - ### Features
    - ClusterAvailabilityStatus: The availability status of the cluster for queries. Possible values are the following: Available, Unavailable, Maintenance, Modifying, Failed.

## __Amazon S3__
  - ### Bugfixes
    - Update `ChecksumCalculatingStreamProvider` to comply with `ContentStreamProvider` contract.

## __Amazon WorkSpaces__
  - ### Features
    - Minor API fixes for WorkSpaces.

# __2.6.2__ __2019-06-27__
## __AWS Direct Connect__
  - ### Features
    - Tags will now be included in the API responses of all supported resources (Virtual interfaces, Connections, Interconnects and LAGs). You can also add tags while creating these resources.

## __AWS EC2 Instance Connect__
  - ### Features
    - Amazon EC2 Instance Connect is a simple and secure way to connect to your instances using Secure Shell (SSH). With EC2 Instance Connect, you can control SSH access to your instances using AWS Identity and Access Management (IAM) policies as well as audit connection requests with AWS CloudTrail events. In addition, you can leverage your existing SSH keys or further enhance your security posture by generating one-time use SSH keys each time an authorized user connects.

## __Amazon Pinpoint__
  - ### Features
    - This release includes editorial updates for the Amazon Pinpoint API documentation.

## __Amazon WorkSpaces__
  - ### Features
    - Added support for the WorkSpaces restore feature and copying WorkSpaces Images across AWS Regions.

# __2.6.1__ __2019-06-26__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed issue where specifying a custom presigning time causes a runtime exception.

## __Amazon DynamoDB__
  - ### Features
    - Documentation updates for dynamodb

# __2.6.0__ __2019-06-26__
## __AWS CodeCommit__
  - ### Features
    - This release supports better exception handling for merges.

## __Amazon S3__
  - ### Bugfixes
    - Modify the types of Part#size and ObjectVersion#size from Integer to Long. This is a breaking change for customers who are using the size() method.

## __AmazonApiGatewayV2__
  - ### Features
    - You can now perform tag operations on ApiGatewayV2 Resources (typically associated with WebSocket APIs)

## __Netty NIO Http Client__
  - ### Bugfixes
    - Completes the response normally when subscription is cancelled from the subscriber and not invoke `SdkAsyncHttpResponseHandler#onError` from the publisher.

# __2.5.71__ __2019-06-25__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Only allows a single execution interceptor with the same class name to be included in loaded execution interceptors.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Starting today, you can use Traffic Mirroring to copy network traffic from an elastic network interface of Amazon EC2 instances and then send it to out-of-band security and monitoring appliances for content inspection, threat monitoring, and troubleshooting. These appliances can be deployed as individual instances, or as a fleet of instances behind a Network Load Balancer with a User Datagram Protocol (UDP) listener. Traffic Mirroring supports filters and packet truncation, so that you only extract the traffic of interest to monitor by using monitoring tools of your choice.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Changing Amazon EKS full service name to Amazon Elastic Kubernetes Service.

## __Amazon S3__
  - ### Bugfixes
    - Fixes [#1196](https://github.com/aws/aws-sdk-java-v2/issues/1196) by changing S3 FilterRule enums to correctly model each enum as capitalized

# __2.5.70__ __2019-06-24__
## __AWS Resource Groups Tagging API__
  - ### Features
    - Updated service APIs and documentation.

## __AWS SecurityHub__
  - ### Features
    - This release includes a new Tags parameter for the EnableSecurityHub operation, and the following new operations: DescribeHub, CreateActionTarget, DeleteActionTarget, DescribeActionTargets, UpdateActionTarget, TagResource, UntagResource, and ListTagsforResource. It removes the operation ListProductSubscribers, and makes Title and Description required attributes of AwsSecurityFinding.

## __Amazon API Gateway__
  - ### Features
    - Customers can pick different security policies (TLS version + cipher suite) for custom domains in API Gateway

## __Amazon CloudWatch Application Insights__
  - ### Features
    - CloudWatch Application Insights detects errors and exceptions from logs, including .NET custom application logs, SQL Server logs, IIS logs, and more, and uses a combination of built-in rules and machine learning, such as dynamic baselining, to identify common problems. You can then easily drill into specific issues with CloudWatch Automatic Dashboards that are dynamically generated. These dashboards contain the most recent alarms, a summary of relevant metrics, and log snippets to help you identify root cause.

## __Amazon FSx__
  - ### Features
    - Starting today, you can join your Amazon FSx for Windows File Server file systems to your organization's self-managed Microsoft Active Directory while creating the file system. You can also perform in-place updates of file systems to keep your Active Directory configuration up to date.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - AWS Systems Manager now supports deleting a specific version of a SSM Document.

## __AmazonApiGatewayV2__
  - ### Features
    - Customers can get information about security policies set on custom domain resources in API Gateway

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for UDP on Network Load Balancers

## __Service Quotas__
  - ### Features
    - Service Quotas enables you to view and manage your quotas for AWS services from a central location.

# __2.5.69__ __2019-06-21__
## __AWS Device Farm__
  - ### Features
    - This release includes updated documentation about the default timeout value for test runs and remote access sessions. This release also includes miscellaneous bug fixes for the documentation.

## __AWS Elemental MediaPackage__
  - ### Features
    - Added two new origin endpoint fields for configuring which SCTE-35 messages are treated as advertisements.

## __AWS Identity and Access Management__
  - ### Features
    - We are making it easier for you to manage your permission guardrails i.e. service control policies by enabling you to retrieve the last timestamp when an AWS service was accessed within an account or AWS Organizations entity.

## __Amazon Kinesis Video Streams Media__
  - ### Features
    - Documentation updates for Amazon Kinesis Video Streams.

# __2.5.68__ __2019-06-20__
## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - ACM Private CA is launching Root CAs and hierarchy management, a new feature that expands the scope of ACM Private CA from supporting only subordinate issuing CAs, to now include a full CA hierarchy that includes root CAs - the cryptographic root of trust for an organization.

## __AWS Glue__
  - ### Features
    - Starting today, you can now use workflows in AWS Glue to author directed acyclic graphs (DAGs) of Glue triggers, crawlers and jobs. Workflows enable orchestration of your ETL workloads by building dependencies between Glue entities (triggers, crawlers and jobs). You can visually track status of the different nodes in the workflows on the console making it easier to monitor progress and troubleshoot issues. Also, you can share parameters across entities in the workflow.

## __AWS Health APIs and Notifications__
  - ### Features
    - API improvements for the AWS Health service.

## __AWS IoT Events Data__
  - ### Features
    - "The colon character ':' is now permitted in Detector Model 'key' parameter values.

## __AWS OpsWorks__
  - ### Features
    - Documentation updates for OpsWorks Stacks.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for RDS storage autoscaling

# __2.5.67__ __2019-06-19__
## __Amazon Elastic Container Service for Kubernetes__
  - ### Features
    - Changing Amazon EKS full service name to Amazon Elastic Kubernetes Service.

# __2.5.66__ __2019-06-18__
## __AWS Resource Groups Tagging API__
  - ### Features
    - You can use tag policies to help standardize on tags across your organization's resources.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - You can now launch new 12xlarge, 24xlarge, and metal instance sizes on the Amazon EC2 compute optimized C5 instance types featuring 2nd Gen Intel Xeon Scalable Processors.

# __2.5.65__ __2019-06-17__
## __AWS RoboMaker__
  - ### Features
    - Add the ServiceUnavailableException (503) into CreateSimulationJob API.

## __AWS Service Catalog__
  - ### Features
    - Restrict concurrent calls by a single customer account for CreatePortfolioShare and DeletePortfolioShare when sharing/unsharing to an Organization.

## __Amazon Neptune__
  - ### Features
    - This release adds a feature to configure Amazon Neptune to publish audit logs to Amazon CloudWatch Logs.

# __2.5.64__ __2019-06-14__
## __Amazon AppStream__
  - ### Features
    - Added 2 new values(WINDOWS_SERVER_2016, WINDOWS_SERVER_2019) for PlatformType enum.

## __Amazon CloudFront__
  - ### Features
    - A new datatype in the CloudFront API, AliasICPRecordal, provides the ICP recordal status for CNAMEs associated with distributions. AWS services in China customers must file for an Internet Content Provider (ICP) recordal if they want to serve content publicly on an alternate domain name, also known as a CNAME, that they have added to CloudFront. The status value is returned in the CloudFront response; you cannot configure it yourself. The status is set to APPROVED for all CNAMEs (aliases) in regions outside of China.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Correction to enumerations in EC2 client.

## __Amazon Personalize__
  - ### Features
    - Documentation updates for Amazon Personalize.

# __2.5.63__ __2019-06-13__
## __AWS App Mesh__
  - ### Features
    - This release adds support for AWS Cloud Map as a service discovery method for virtual nodes.

## __Amazon ElastiCache__
  - ### Features
    - This release is to add support for reader endpoint for cluster-mode disabled Amazon ElastiCache for Redis clusters.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - G4 instances are Amazon EC2 instances based on NVIDIA T4 GPUs and are designed to provide cost-effective machine learning inference for applications, like image classification, object detection, recommender systems, automated speech recognition, and language translation. G4 instances are also a cost-effective platform for building and running graphics-intensive applications, such as remote graphics workstations, video transcoding, photo-realistic design, and game streaming in the cloud. To get started with G4 instances visit https://aws.amazon.com/ec2/instance-types/g4.

## __Amazon GuardDuty__
  - ### Features
    - Support for tagging functionality in Create and Get operations for Detector, IP Set, Threat Intel Set, and Finding Filter resources and 3 new tagging APIs: ListTagsForResource, TagResource, and UntagResource.

# __2.5.62__ __2019-06-12__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix a bug in the code generator causing event headers to be incorrectly marshalled and unmarshalled to and from the payload.

## __AWS Service Catalog__
  - ### Features
    - This release adds a new field named Guidance to update provisioning artifact, this field can be set by the administrator to provide guidance to end users about which provisioning artifacts to use.

# __2.5.61__ __2019-06-11__
## __Amazon SageMaker Service__
  - ### Features
    - The default TaskTimeLimitInSeconds of labeling job is increased to 8 hours. Batch Transform introduces a new DataProcessing field which supports input and output filtering and data joining. Training job increases the max allowed input channels from 8 to 20.

# __2.5.60__ __2019-06-10__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild adds support for source version on project level.

## __AWS CodeCommit__
  - ### Features
    - This release adds two merge strategies for merging pull requests: squash and three-way. It also adds functionality for resolving merge conflicts, testing merge outcomes, and for merging branches using one of the three supported merge strategies.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix NPE for streaming APIs in async client if there is a failure before AsyncResponseTransformer#prepare is called for first time. See https://github.com/aws/aws-sdk-java-v2/issues/1268

## __Amazon Personalize__
  - ### Features
    - Amazon Personalize is a machine learning service that makes it easy for developers to create individualized recommendations for customers using their applications.

## __Amazon Personalize Events__
  - ### Features
    - Introducing Amazon Personalize - a machine learning service that makes it easy for developers to create individualized recommendations for customers using their applications.

## __Amazon Personalize Runtime__
  - ### Features
    - Amazon Personalize is a machine learning service that makes it easy for developers to create individualized recommendations for customers using their applications.

# __2.5.59__ __2019-06-07__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Allow customers to disable read and write timeout by setting `Duration.ZERO` to `readTimeout` and `writeTimeout`. See [#1281](https://github.com/aws/aws-sdk-java-v2/issues/1281)

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds DNS entries and NLB ARNs to describe-vpc-endpoint-connections API response. Adds owner ID to describe-vpc-endpoints and create-vpc-endpoint API responses.

# __2.5.58__ __2019-06-06__
## __AWS MediaConnect__
  - ### Features
    - This release adds support for encrypting entitlements using Secure Packager and Encoder Key Exchange (SPEKE).

## __AWS Organizations__
  - ### Features
    - You can tag and untag accounts in your organization and view tags on an account in your organization.

## __Amazon CloudWatch Logs__
  - ### Features
    - Documentation updates for logs

## __Amazon DynamoDB__
  - ### Features
    - Documentation updates for dynamodb

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces support for launching container instances using supported Amazon EC2 instance types that have increased elastic network interface density. Using these instance types and opting in to the awsvpcTrunking account setting provides increased elastic network interface (ENI) density on newly launched container instances which allows you to place more tasks on each container instance.

## __Amazon GuardDuty__
  - ### Features
    - Improve FindingCriteria Condition field names, support long-typed conditions and deprecate old Condition field names.

## __Amazon Simple Email Service__
  - ### Features
    - You can now specify whether the Amazon Simple Email Service must deliver email over a connection that is encrypted using Transport Layer Security (TLS).

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - OpsCenter is a new Systems Manager capability that allows you to view, diagnose, and remediate, operational issues, aka OpsItems, related to various AWS resources by bringing together contextually relevant investigation information. New APIs to create, update, describe, and get OpsItems as well as OpsItems summary API.

# __2.5.57__ __2019-06-05__
## __AWS Glue__
  - ### Features
    - Support specifying python version for Python shell jobs. A new parameter PythonVersion is added to the JobCommand data type.

# __2.5.56__ __2019-06-04__
## __AWS Identity and Access Management__
  - ### Features
    - This release adds validation for policy path field. This field is now restricted to be max 512 characters.

## __AWS Storage Gateway__
  - ### Features
    - AWS Storage Gateway now supports AWS PrivateLink, enabling you to administer and use gateways without needing to use public IP addresses or a NAT/Internet Gateway, while avoiding traffic from going over the internet.

## __Amazon ElastiCache__
  - ### Features
    - Amazon ElastiCache now allows you to apply available service updates on demand. Features included: (1) Access to the list of applicable service updates and their priorities. (2) Service update monitoring and regular status updates. (3) Recommended apply-by-dates for scheduling the service updates, which is critical if your cluster is in ElastiCache-supported compliance programs. (4) Ability to stop and later re-apply updates. For more information, see https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/Self-Service-Updates.html

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for Host Recovery feature which automatically restarts instances on to a new replacement host if failures are detected on Dedicated Host.

## __Amazon Simple Storage Service__
  - ### Features
    - Documentation updates for s3

# __2.5.55__ __2019-06-03__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 I3en instances are the new storage-optimized instances offering up to 60 TB NVMe SSD instance storage and up to 100 Gbps of network bandwidth.

## __Amazon Relational Database Service__
  - ### Features
    - Amazon RDS Data API is generally available. Removing beta notes in the documentation.

# __2.5.54__ __2019-05-30__
## __AWS CodeCommit__
  - ### Features
    - This release adds APIs that allow adding and removing tags to a repository, and viewing tags for a repository. It also enables adding tags when creating a repository.

## __AWS IoT Analytics__
  - ### Features
    - IoT Analytics adds the option to use your own S3 bucket to store channel and data store resources. Previously, only service-managed storage was used.

## __AWS IoT Events__
  - ### Features
    - The AWS IoT Events service allows customers to monitor their IoT devices and sensors to detect failures or changes in operation and to trigger actions when these events occur

## __AWS IoT Events Data__
  - ### Features
    - The AWS IoT Events service allows customers to monitor their IoT devices and sensors to detect failures or changes in operation and to trigger actions when these events occur

## __AWS RDS DataService__
  - ### Features
    - The RDS Data API is generally available for the MySQL-compatible edition of Amazon Aurora Serverless in the US East (N. Virginia and Ohio), US West (Oregon), EU (Ireland), and Asia Pacific (Tokyo) regions. This service enables you to easily access Aurora Serverless clusters with web services-based applications including AWS Lambda and AWS AppSync. The new APIs included in this SDK release are ExecuteStatement, BatchExecuteStatement, BeginTransaction, CommitTransaction, and RollbackTransaction. The ExecuteSql API is deprecated; instead use ExecuteStatement which provides additional functionality including transaction support.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Improved exception messages in credential providers to exclude detailed parse errors that may contain sensitive information.

## __AWS Service Catalog__
  - ### Features
    - Service Catalog ListStackInstancesForProvisionedProduct API enables customers to get details of a provisioned product with type "CFN_STACKSET". By passing the provisioned product id, the API will list account, region and status of each stack instances that are associated with this provisioned product.

## __Amazon Pinpoint Email Service__
  - ### Features
    - You can now specify whether the Amazon Pinpoint Email service must deliver email over a connection that is encrypted using Transport Layer Security (TLS).

## __Amazon Relational Database Service__
  - ### Features
    - This release adds support for Activity Streams for database clusters.

## __Managed Streaming for Kafka__
  - ### Features
    - Updated APIs for Amazon MSK to enable new features such as encryption in transit, client authentication, and scaling storage.

# __2.5.53__ __2019-05-29__
## __AWS IoT Things Graph__
  - ### Features
    - Initial release.

## __AWS SecurityHub__
  - ### Features
    - This update adds the ListProductSubscribers API, DescribeProducts API, removes CONTAINS as a comparison value for the StringFilter, and only allows use of EQUALS instead of CONTAINS in MapFilter.

## __Amazon Data Lifecycle Manager__
  - ### Features
    - Customers can now simultaneously take snapshots of multiple EBS volumes attached to an EC2 instance. With this new capability, snapshots guarantee crash-consistency across multiple volumes by preserving the order of IO operations. This new feature is fully integrated with Amazon Data Lifecycle Manager (DLM) allowing customers to automatically manage snapshots by creating lifecycle policies.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Customers can now simultaneously take snapshots of multiple EBS volumes attached to an EC2 instance. With this new capability, snapshots guarantee crash-consistency across multiple volumes by preserving the order of IO operations. This new feature is fully integrated with Amazon Data Lifecycle Manager (DLM) allowing customers to automatically manage snapshots by creating lifecycle policies.

## __Amazon Relational Database Service__
  - ### Features
    - Documentation updates for rds

## __Amazon S3__
  - ### Bugfixes
    - Allows S3 to be used with object keys that have a leading slash "/myKey"

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Systems Manager - Documentation updates

# __2.5.52__ __2019-05-28__
## __AWS Ground Station__
  - ### Features
    - AWS Ground Station is a fully managed service that enables you to control satellite communications, downlink and process satellite data, and scale your satellite operations efficiently and cost-effectively without having to build or manage your own ground station infrastructure.

## __AWS RoboMaker__
  - ### Features
    - Added support for an additional robot software suite (Gazebo 9) and for cancelling deployment jobs.

## __AWS Security Token Service__
  - ### Features
    - Documentation updates for iam

## __AWS Storage Gateway__
  - ### Features
    - Introduce AssignTapePool operation to allow customers to migrate tapes between pools.

## __AWS WAF__
  - ### Features
    - Documentation updates for waf

## __Amazon Chime__
  - ### Features
    - This release adds the ability to search and order toll free phone numbers for Voice Connectors.

## __Amazon Pinpoint Email Service__
  - ### Features
    - This release adds support for programmatic access to Deliverability dashboard subscriptions and the deliverability data provided by the Deliverability dashboard for domains and IP addresses. The data includes placement metrics for campaigns that use subscribed domains to send email.

## __Amazon Relational Database Service__
  - ### Features
    - Add a new output field Status to DBEngineVersion which shows the status of the engine version (either available or deprecated). Add a new parameter IncludeAll to DescribeDBEngineVersions to make it possible to return both available and deprecated engine versions. These changes enable a user to create a Read Replica of an DB instance on a deprecated engine version.

## __Amazon Transcribe Service__
  - ### Features
    - Amazon Transcribe - support transcriptions from audio sources in Modern Standard Arabic (ar-SA).

# __2.5.51__ __2019-05-24__
## __AWS CodeDeploy__
  - ### Features
    - AWS CodeDeploy now supports tagging for the application and deployment group resources.

## __AWS Elemental MediaStore Data Plane__
  - ### Features
    - MediaStore - This release adds support for chunked transfer of objects, which reduces latency by making an object available for downloading while it is still being uploaded.

## __AWS OpsWorks for Chef Automate__
  - ### Features
    - Documentation updates for OpsWorks for Chef Automate; attribute values updated for Chef Automate 2.0 release.

# __2.5.50__ __2019-05-23__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Updated aws-java-sdk and bom modules to include ALL service modules.

## __AWS WAF Regional__
  - ### Features
    - Documentation updates for waf-regional

## __Amazon Elastic Compute Cloud__
  - ### Features
    - New APIs to enable EBS encryption by default feature. Once EBS encryption by default is enabled in a region within the account, all new EBS volumes and snapshot copies are always encrypted

# __2.5.49__ __2019-05-22__
## __AWS Budgets__
  - ### Features
    - Added new datatype PlannedBudgetLimits to Budget model, and updated examples for AWS Budgets API for UpdateBudget, CreateBudget, DescribeBudget, and DescribeBudgets

## __AWS Device Farm__
  - ### Features
    - This release introduces support for tagging, tag-based access control, and resource-based access control.

## __AWS Service Catalog__
  - ### Features
    - Service Catalog UpdateProvisionedProductProperties API enables customers to manage provisioned product ownership. Administrators can now update the user associated to a provisioned product to another user within the same account allowing the new user to describe, update, terminate and execute service actions in that Service Catalog resource. New owner will also be able to list and describe all past records executed for that provisioned product.

## __Amazon API Gateway__
  - ### Features
    - This release adds support for tagging of Amazon API Gateway resources.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds idempotency support for associate, create route and authorization APIs for AWS Client VPN Endpoints.

## __Amazon Elastic File System__
  - ### Features
    - AWS EFS documentation updated to reflect the minimum required value for ProvisionedThroughputInMibps is 1 from the previously documented 0. The service has always required a minimum value of 1, therefor service behavior is not changed.

## __Amazon Relational Database Service__
  - ### Features
    - Documentation updates for rds

## __Amazon WorkLink__
  - ### Features
    - Amazon WorkLink is a fully managed, cloud-based service that enables secure, one-click access to internal websites and web apps from mobile phones. This release introduces new APIs to associate and manage website authorization providers with Amazon WorkLink fleets.

# __2.5.48__ __2019-05-21__
## __AWS DataSync__
  - ### Features
    - Documentation update and refine pagination token on Datasync List API's

## __Alexa For Business__
  - ### Features
    - This release contains API changes to allow customers to create and manage Network Profiles for their Shared devices

# __2.5.47__ __2019-05-20__
## __AWS Elemental MediaPackage VOD__
  - ### Features
    - AWS Elemental MediaPackage now supports Video-on-Demand (VOD) workflows. These new features allow you to easily deliver a vast library of source video Assets stored in your own S3 buckets using a small set of simple to set up Packaging Configurations and Packaging Groups.

## __AWSMarketplace Metering__
  - ### Features
    - Documentation updates for meteringmarketplace

## __Managed Streaming for Kafka__
  - ### Features
    - Updated APIs for the Managed Streaming for Kafka service that let customers create clusters with custom Kafka configuration.

# __2.5.46__ __2019-05-17__
## __Amazon AppStream__
  - ### Features
    - Includes APIs for managing subscriptions to AppStream 2.0 usage reports and configuring idle disconnect timeouts on AppStream 2.0 fleets.

# __2.5.45__ __2019-05-16__
## __AWS Elemental MediaLive__
  - ### Features
    - Added channel state waiters to MediaLive.

## __Amazon Simple Storage Service__
  - ### Features
    - This release updates the Amazon S3 PUT Bucket replication API to include a new optional field named token, which allows you to add a replication configuration to an S3 bucket that has Object Lock enabled.

# __2.5.44__ __2019-05-15__
## __AWS CodePipeline__
  - ### Features
    - This feature includes new APIs to add, edit, remove and view tags for pipeline, custom action type and webhook resources. You can also add tags while creating these resources.

## __AWS Elemental MediaPackage__
  - ### Features
    - Adds optional configuration for DASH SegmentTemplateFormat to refer to segments by Number with Duration, rather than Number or Time with SegmentTimeline.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix a bug in `EventStreamAsyncResponseTransformer` where the reference to the current stream `Subscriber` is not reset in `prepare`, causing an `IllegalStateException` to be thrown when attemping to subscribe to the event stream upon a retry.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adding tagging support for VPC Endpoints and VPC Endpoint Services.

## __Amazon Relational Database Service__
  - ### Features
    - In the RDS API and CLI documentation, corrections to the descriptions for Boolean parameters to avoid references to TRUE and FALSE. The RDS CLI does not allow TRUE and FALSE values values for Boolean parameters.

## __Amazon Transcribe Service__
  - ### Features
    - Amazon Transcribe - support transcriptions from audio sources in Indian English (en-IN) and Hindi (hi-IN).

# __2.5.43__ __2019-05-14__
## __AWS Storage Gateway__
  - ### Features
    - Add Tags parameter to CreateSnapshot and UpdateSnapshotSchedule APIs, used for creating tags on create for one off snapshots and scheduled snapshots.

## __Amazon Chime__
  - ### Features
    - Amazon Chime private bots GA release.

## __Amazon Comprehend__
  - ### Features
    - With this release AWS Comprehend now supports Virtual Private Cloud for Asynchronous Batch Processing jobs

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Pagination support for ec2.DescribeSubnets, ec2.DescribeDhcpOptions

# __2.5.42__ __2019-05-13__
## __AWS DataSync__
  - ### Features
    - AWS DataSync now enables exclude and include filters to control what files and directories will be copied as part of a task execution.

## __AWS IoT Analytics__
  - ### Features
    - ContentDeliveryRule to support sending dataset to S3 and glue

## __AWS Lambda__
  - ### Features
    - AWS Lambda now supports Node.js v10

# __2.5.41__ __2019-05-10__
## __AWS Glue__
  - ### Features
    - AWS Glue now supports specifying existing catalog tables for a crawler to examine as a data source. A new parameter CatalogTargets is added to the CrawlerTargets data type.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix a bug where events in an event stream were being signed with the request date, and not with the current system time.

## __AWS Security Token Service__
  - ### Features
    - AWS Security Token Service (STS) now supports passing IAM Managed Policy ARNs as session policies when you programmatically create temporary sessions for a role or federated user. The Managed Policy ARNs can be passed via the PolicyArns parameter, which is now available in the AssumeRole, AssumeRoleWithWebIdentity, AssumeRoleWithSAML, and GetFederationToken APIs. The session policies referenced by the PolicyArn parameter will only further restrict the existing permissions of an IAM User or Role for individual sessions.

# __2.5.40__ __2019-05-08__
## __AWS IoT 1-Click Projects Service__
  - ### Features
    - Added automatic pagination support for ListProjects and ListPlacements APIs.

## __AWS Service Catalog__
  - ### Features
    - Adds "Parameters" field in UpdateConstraint API, which will allow Admin user to update "Parameters" in created Constraints.

## __Amazon Elastic Container Service for Kubernetes__
  - ### Features
    - Documentation update for Amazon EKS to clarify allowed parameters in update-cluster-config.

## __Amazon Kinesis Analytics__
  - ### Features
    - Kinesis Data Analytics APIs now support tagging on applications.

## __Amazon SageMaker Service__
  - ### Features
    - Workteams now supports notification configurations. Neo now supports Jetson Nano as a target device and NumberOfHumanWorkersPerDataObject is now included in the ListLabelingJobsForWorkteam response.

# __2.5.39__ __2019-05-07__
## __AWS AppSync__
  - ### Features
    - AWS AppSync now supports the ability to add additional authentication providers to your AWS AppSync GraphQL API as well as the ability to retrieve directives configured against fields or object type definitions during schema introspection.

## __AWS Storage Gateway__
  - ### Features
    - Add optional field AdminUserList to CreateSMBFileShare and UpdateSMBFileShare APIs.

## __Alexa For Business__
  - ### Features
    - This release adds an API allowing authorized users to delete a shared device's history of voice recordings and associated response data.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Patch Manager adds support for Microsoft Application Patching.

# __2.5.38__ __2019-05-06__
## __AWS CodePipeline__
  - ### Features
    - Documentation updates for codepipeline

## __AWS Config__
  - ### Features
    - AWS Config now supports tagging on PutConfigRule, PutConfigurationAggregator and PutAggregationAuthorization APIs.

## __AWS Identity and Access Management__
  - ### Features
    - Documentation updates for iam

## __AWS Security Token Service__
  - ### Features
    - Documentation updates for sts

# __2.5.37__ __2019-05-03__
## __AWS Elemental MediaConvert__
  - ### Features
    - DASH output groups using DRM encryption can now enable a playback device compatibility mode to correct problems with playback on older devices.

## __AWS Elemental MediaLive__
  - ### Features
    - You can now switch the channel mode of your channels from standard to single pipeline and from single pipeline to standard. In order to switch a channel from single pipeline to standard all inputs attached to the channel must support two encoder pipelines.

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release of Amazon Cognito User Pools introduces the new AdminSetUserPassword API that allows administrators of a user pool to change a user's password. The new password can be temporary or permanent.

## __Amazon WorkMail__
  - ### Features
    - Amazon WorkMail is releasing two new actions: 'GetMailboxDetails' and 'UpdateMailboxQuota'. They add insight into how much space is used by a given mailbox (size) and what its limit is (quota). A mailbox quota can be updated, but lowering the value will not influence WorkMail per user charges. For a closer look at the actions please visit https://docs.aws.amazon.com/workmail/latest/APIReference/API_Operations.html

# __2.5.36__ __2019-05-02__
## __AWS Key Management Service__
  - ### Features
    - AWS Key Management Service (KMS) can return an INTERNAL_ERROR connection error code if it cannot connect a custom key store to its AWS CloudHSM cluster. INTERNAL_ERROR is one of several connection error codes that help you to diagnose and fix a problem with your custom key store.

## __Alexa For Business__
  - ### Features
    - This release allows developers and customers to send text and audio announcements to rooms.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fix a bug in the Netty client where the read timeout isn't applied correctly for some requests.

# __2.5.35__ __2019-05-01__
## __AWS X-Ray__
  - ### Features
    - AWS X-Ray now includes Analytics, an interactive approach to analyzing user request paths (i.e., traces). Analytics will allow you to easily understand how your application and its underlying services are performing. With X-Ray Analytics, you can quickly detect application issues, pinpoint the root cause of the issue, determine the severity of the issues, and identify which end users were impacted. With AWS X-Ray Analytics you can explore, analyze, and visualize traces, allowing you to find increases in response time to user requests or increases in error rates. Metadata around peak periods, including frequency and actual times of occurrence, can be investigated by applying filters with a few clicks. You can then drill down on specific errors, faults, and response time root causes and view the associated traces.

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces additional task definition parameters that enable you to define secret options for Docker log configuration, a per-container list contains secrets stored in AWS Systems Manager Parameter Store or AWS Secrets Manager.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds an API for the modification of a VPN Connection, enabling migration from a Virtual Private Gateway (VGW) to a Transit Gateway (TGW), while preserving the VPN endpoint IP addresses on the AWS side as well as the tunnel options.

# __2.5.34__ __2019-04-30__
## __AWS CodePipeline__
  - ### Features
    - This release contains an update to the PipelineContext object that includes the Pipeline ARN, and the Pipeline Execution Id. The ActionContext object is also updated to include the Action Execution Id.

## __AWS Direct Connect__
  - ### Features
    - This release adds support for AWS Direct Connect customers to use AWS Transit Gateway with AWS Direct Connect gateway to route traffic between on-premise networks and their VPCs.

## __AWS Service Catalog__
  - ### Features
    - Admin users can now associate/disassociate aws budgets with a portfolio or product in Service Catalog. End users can see the association by listing it or as part of the describe portfolio/product output. A new optional boolean parameter, "DisableTemplateValidation", is added to ProvisioningArtifactProperties data type. The purpose of the parameter is to enable or disable the CloudFormation template validtion when creating a product or a provisioning artifact.

## __Amazon Managed Blockchain__
  - ### Features
    - (New Service) Amazon Managed Blockchain is a fully managed service that makes it easy to create and manage scalable blockchain networks using popular open source frameworks.

## __Amazon Neptune__
  - ### Features
    - Adds a feature to allow customers to specify a custom parameter group when restoring a database cluster.

# __2.5.33__ __2019-04-29__
## __AWS Transfer for SFTP__
  - ### Features
    - This release adds support for per-server host-key management. You can now specify the SSH RSA private key used by your SFTP server.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds support for Elastic Fabric Adapter (EFA) ENIs.

# __2.5.32__ __2019-04-26__
## __AWS Identity and Access Management__
  - ### Features
    - AWS Security Token Service (STS) enables you to request session tokens from the global STS endpoint that work in all AWS Regions. You can configure the global STS endpoint to vend session tokens that are compatible with all AWS Regions using the new IAM SetSecurityTokenServicePreferences API.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix bug in ChecksumValidatingSubscriber which results in NPE if checksum validation fails.

## __Amazon Simple Notification Service__
  - ### Features
    - With this release AWS SNS adds tagging support for Topics.

# __2.5.31__ __2019-04-25__
## __AWS Batch__
  - ### Features
    - Documentation updates for AWS Batch.

## __AWS Lambda__
  - ### Features
    - AWS Lambda now supports the GetLayerVersionByArn API.

## __Amazon DynamoDB__
  - ### Features
    - This update allows you to tag Amazon DynamoDB tables when you create them. Tags are labels you can attach to AWS resources to make them easier to manage, search, and filter.

## __Amazon GameLift__
  - ### Features
    - This release introduces the new Realtime Servers feature, giving game developers a lightweight yet flexible solution that eliminates the need to build a fully custom game server. The AWS SDK updates provide support for scripts, which are used to configure and customize Realtime Servers.

## __Amazon Inspector__
  - ### Features
    - AWS Inspector - Improve the ListFindings API response time and decreases the maximum number of agentIDs from 500 to 99.

## __Amazon WorkSpaces__
  - ### Features
    - Documentation updates for workspaces

## __Netty NIO Async Http Client__
  - ### Bugfixes
    - Add workaround to await channel pools to be closed before shutting down EventLoopGroup to avoid the race condition between `channelPool.close` and `eventLoopGroup.shutdown`. See [#1109](https://github.com/aws/aws-sdk-java-v2/issues/1109).

# __2.5.30__ __2019-04-24__
## __AWS CloudFormation__
  - ### Features
    - Documentation updates for cloudformation

## __AWS MediaConnect__
  - ### Features
    - Adds support for ListEntitlements pagination.

## __AWS MediaTailor__
  - ### Features
    - AWS Elemental MediaTailor SDK now includes a new parameter to support origin servers that produce single-period DASH manifests.

## __AWS SDK for Java v2__
  - ### Features
    - Make `BytesWrapper`, parent of `SdkBytes` and `ResponseBytes`, public. Fixes [#1208](https://github.com/aws/aws-sdk-java-v2/issues/1208).
    - Support for `credential_source` property in profiles.

  - ### Bugfixes
    - Fixed a bug in asynchronous clients, where a service closing a connection between when a channel is acquired and handlers are attached could lead to response futures never being completed. Fixes [#1207](https://github.com/aws/aws-sdk-java-v2/issues/1207).

## __AWS Storage Gateway__
  - ### Features
    - AWS Storage Gateway now supports Access Control Lists (ACLs) on File Gateway SMB shares, enabling you to apply fine grained access controls for Active Directory users and groups.

## __Alexa For Business__
  - ### Features
    - This release adds support for the Alexa for Business gateway and gateway group APIs.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - You can now launch the new Amazon EC2 general purpose burstable instance types T3a that feature AMD EPYC processors.

## __Amazon Relational Database Service__
  - ### Features
    - A new parameter "feature-name" is added to the add-role and remove-role db cluster APIs. The value for the parameter is optional for Aurora MySQL compatible database clusters, but mandatory for Aurora PostgresQL. You can find the valid list of values using describe db engine versions API.

## __Amazon Route 53__
  - ### Features
    - Amazon Route 53 now supports the Asia Pacific (Hong Kong) Region (ap-east-1) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release updates AWS Systems Manager APIs to allow customers to configure parameters to use either the standard-parameter tier (the default tier) or the advanced-parameter tier. It allows customers to create parameters with larger values and attach parameter policies to an Advanced Parameter.

## __Amazon Textract__
  - ### Features
    - This release adds support for checkbox also known as SELECTION_ELEMENT in Amazon Textract.

# __2.5.29__ __2019-04-19__
## __AWS Resource Groups__
  - ### Features
    - The AWS Resource Groups service increased the query size limit to 4096 bytes.

## __Amazon Transcribe Service__
  - ### Features
    - Amazon Transcribe - support transcriptions from audio sources in Spanish Spanish (es-ES).

## __Amazon WorkSpaces__
  - ### Features
    - Added a new reserved field.

# __2.5.28__ __2019-04-18__
## __AWS Application Discovery Service__
  - ### Features
    - The Application Discovery Service's DescribeImportTasks and BatchDeleteImportData APIs now return additional statuses for error reporting.

## __AWS Organizations__
  - ### Features
    - AWS Organizations is now available in the AWS GovCloud (US) Regions, and we added a new API action for creating accounts in those Regions. For more information, see CreateGovCloudAccount in the AWS Organizations API Reference.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Document updates for Amazon Cognito Identity Provider.

## __Amazon Relational Database Service__
  - ### Features
    - This release adds the TimeoutAction parameter to the ScalingConfiguration of an Aurora Serverless DB cluster. You can now configure the behavior when an auto-scaling capacity change can't find a scaling point.

## __Amazon S3__
  - ### Bugfixes
    - Reduced the frequency of 'server failed to send complete response' exceptions when using S3AsyncClient.

## __Amazon WorkLink__
  - ### Features
    - Amazon WorkLink is a fully managed, cloud-based service that enables secure, one-click access to internal websites and web apps from mobile phones. This release introduces new APIs to link and manage internal websites and web apps with Amazon WorkLink fleets.

## __Amazon WorkSpaces__
  - ### Features
    - Documentation updates for workspaces

## __Managed Streaming for Kafka__
  - ### Features
    - Amazon Kafka - Added tagging APIs

# __2.5.27__ __2019-04-17__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for requester-managed Interface VPC Endpoints (powered by AWS PrivateLink). The feature prevents VPC endpoint owners from accidentally deleting or otherwise mismanaging the VPC endpoints of some AWS VPC endpoint services.

## __Amazon Polly__
  - ### Features
    - Amazon Polly adds Arabic language support with new female voice - "Zeina"

# __2.5.26__ __2019-04-16__
## __AWS Organizations__
  - ### Features
    - Documentation updates for organizations

## __AWS SDK for Java v2__
  - ### Features
    - Enable support for credential_process in an AWS credential profile

## __AWS Storage Gateway__
  - ### Features
    - This change allows you to select either a weekly or monthly maintenance window for your volume or tape gateway. It also allows you to tag your tape and volume resources on creation by adding a Tag value on calls to the respective api endpoints.

## __Amazon CloudWatch__
  - ### Features
    - Documentation updates for monitoring

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release adds support for the new email configuration in Amazon Cognito User Pools. You can now specify whether Amazon Cognito emails your users by using its built-in email functionality or your Amazon SES email configuration.

## __Amazon Redshift__
  - ### Features
    - DescribeResize can now return percent of data transferred from source cluster to target cluster for a classic resize.

## __AmazonMQ__
  - ### Features
    - This release adds the ability to retrieve information about broker engines and broker instance options. See Broker Engine Types and Broker Instance Options in the Amazon MQ REST API Reference.

## __Netty NIO Http Client__
  - ### Bugfixes
    - Update `UnusedChannelExceptionHandler` to check the cause of the exception so that it does emit warn logs if the cause is netty io exception. See [#1171](https://github.com/aws/aws-sdk-java-v2/issues/1171)

# __2.5.25__ __2019-04-05__
## __AWS Elemental MediaConvert__
  - ### Features
    - Rectify incorrect modelling of DisassociateCertificate method

## __AWS Elemental MediaLive__
  - ### Features
    - Today AWS Elemental MediaLive (https://aws.amazon.com/medialive/) adds the option to create "Single Pipeline" channels, which offers a lower-cost option compared to Standard channels. MediaLive Single Pipeline channels have a single encoding pipeline rather than the redundant dual Availability Zone (AZ) pipelines that MediaLive provides with a "Standard" channel.

## __AWS Glue__
  - ### Features
    - AWS Glue now supports workerType choices in the CreateJob, UpdateJob, and StartJobRun APIs, to be used for memory-intensive jobs.

## __AWS IoT 1-Click Devices Service__
  - ### Features
    - Documentation updates for 1-Click: improved descriptions of resource tagging APIs.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Lazily initialize `ApiCallTimeoutException` and `ApiCallAttemptTimeoutException`. This change would improve performance of async api calls.

## __AWS Transcribe Streaming__
  - ### Features
    - Amazon Transcribe now supports GB English, CA French and FR French which expands upon the existing language support for US English and US Spanish.

## __Amazon Comprehend__
  - ### Features
    - With this release AWS Comprehend provides confusion matrix for custom document classifier.

# __2.5.24__ __2019-04-04__
## __AWS Identity and Access Management__
  - ### Features
    - Documentation updates for iam

## __Amazon Elastic Container Service for Kubernetes__
  - ### Features
    - Added support to enable or disable publishing Kubernetes cluster logs in AWS CloudWatch

# __2.5.23__ __2019-04-03__
## __AWS Batch__
  - ### Features
    - Support for GPU resource requirement in RegisterJobDefinition and SubmitJob

## __Amazon Comprehend__
  - ### Features
    - With this release AWS Comprehend adds tagging support for document-classifiers and entity-recognizers.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fix a bug where SNI was not enabled in Netty NIO Async Client for TLS and caused the requests to fail of handshake_failure in some services. See [#1171](https://github.com/aws/aws-sdk-java-v2/issues/1171)

# __2.5.22__ __2019-04-02__
## __AWS Certificate Manager__
  - ### Features
    - Documentation updates for acm

## __AWS SecurityHub__
  - ### Features
    - This update includes 3 additional error codes: AccessDeniedException, InvalidAccessException, and ResourceConflictException. This update also removes the error code ResourceNotFoundException from the GetFindings, GetInvitationsCount, ListInvitations, and ListMembers operations.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add paginators.

# __2.5.21__ __2019-04-01__
## __Amazon Elastic MapReduce__
  - ### Features
    - Amazon EMR adds the ability to modify instance group configurations on a running cluster through the new "configurations" field in the ModifyInstanceGroups API.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - March 2019 documentation updates for Systems Manager.

# __2.5.20__ __2019-03-29__
## __AWS Greengrass__
  - ### Features
    - Greengrass APIs now support tagging operations on resources

## __Amazon API Gateway__
  - ### Bugfixes
    - Fix the SDK cloberring existed 'Accept' headers on marshalled requests.

## __Amazon CloudWatch__
  - ### Features
    - Added 3 new APIs, and one additional parameter to PutMetricAlarm API, to support tagging of CloudWatch Alarms.

## __Amazon Comprehend__
  - ### Features
    - With this release AWS Comprehend supports encryption of output results of analysis jobs and volume data on the storage volume attached to the compute instance that processes the analysis job.

# __2.5.19__ __2019-03-28__
## __AWS Elemental MediaLive__
  - ### Features
    - This release adds a new output locking mode synchronized to the Unix epoch.

## __AWS Service Catalog__
  - ### Features
    - Adds "Tags" field in UpdateProvisionedProduct API. The product should have a new RESOURCE_UPDATE Constraint with TagUpdateOnProvisionedProduct field set to ALLOWED for it to work. See API docs for CreateConstraint for more information

## __Amazon Pinpoint Email Service__
  - ### Features
    - This release adds support for using the Amazon Pinpoint Email API to tag the following types of Amazon Pinpoint resources: configuration sets; dedicated IP pools; deliverability dashboard reports; and, email identities. A tag is a label that you optionally define and associate with these types of resources. Tags can help you categorize and manage these resources in different ways, such as by purpose, owner, environment, or other criteria. A resource can have as many as 50 tags. For more information, see the Amazon Pinpoint Email API Reference.

## __Amazon WorkSpaces__
  - ### Features
    - Amazon WorkSpaces adds tagging support for WorkSpaces Images, WorkSpaces directories, WorkSpaces bundles and IP Access control groups.

# __2.5.18__ __2019-03-27__
## __AWS App Mesh__
  - ### Features
    - This release includes AWS Tagging integration for App Mesh, VirtualNode access logging, TCP routing, and Mesh-wide external traffic egress control. See https://docs.aws.amazon.com/app-mesh/latest/APIReference/Welcome.html for more details.

## __AWS Storage Gateway__
  - ### Features
    - This change allows you to select a pool for archiving virtual tapes. Pools are associated with S3 storage classes. You can now choose to archive virtual tapes in either S3 Glacier or S3 Glacier Deep Archive storage class. CreateTapes API now takes a new PoolId parameter which can either be GLACIER or DEEP_ARCHIVE. Tapes created with this parameter will be archived in the corresponding storage class.

## __AWS Transfer for SFTP__
  - ### Features
    - This release adds PrivateLink support to your AWS SFTP server endpoint, enabling the customer to access their SFTP server within a VPC, without having to traverse the internet. Customers can now can create a server and specify an option whether they want the endpoint to be hosted as public or in their VPC, and with the in VPC option, SFTP clients and users can access the server only from the customer's VPC or from their on-premises environments using DX or VPN. This release also relaxes the SFTP user name requirements to allow underscores and hyphens.

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces support for external deployment controllers for ECS services with the launch of task set management APIs. Task sets are a new primitive for controlled management of application deployments within a single ECS service.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - You can now launch the new Amazon EC2 R5ad and M5ad instances that feature local NVMe attached SSD instance storage (up to 3600 GB). M5ad and R5ad feature AMD EPYC processors that offer a 10% cost savings over the M5d and R5d EC2 instances.

## __Amazon Simple Storage Service__
  - ### Features
    - S3 Glacier Deep Archive provides secure, durable object storage class for long term data archival. This SDK release provides API support for this new storage class.

## __Apache Http Client__
  - ### Features
    - Add the ability to set a custom Apache HttpRoutePlanner and CredentialProvider

## __Elastic Load Balancing__
  - ### Features
    - This release adds support for routing based on HTTP headers, methods, query string or query parameters and source IP addresses in Application Load Balancer.

# __2.5.17__ __2019-03-26__
## __AWS Glue__
  - ### Features
    - This new feature will now allow customers to add a customized csv classifier with classifier API. They can specify a custom delimiter, quote symbol and control other behavior they'd like crawlers to have while recognizing csv files

## __Amazon WorkMail__
  - ### Features
    - Documentation updates for Amazon WorkMail.

# __2.5.16__ __2019-03-25__
## __AWS Direct Connect__
  - ### Features
    - Direct Connect gateway enables you to establish connectivity between your on-premise networks and Amazon Virtual Private Clouds (VPCs) in any commercial AWS Region (except in China) using AWS Direct Connect connections at any AWS Direct Connect location. This release enables multi-account support for Direct Connect gateway, with multi-account support for Direct Connect gateway, you can associate up to ten VPCs from any AWS account with a Direct Connect gateway. The AWS accounts owning VPCs and the Direct Connect gateway must belong to the same AWS payer account ID. This release also enables Direct Connect Gateway owners to allocate allowed prefixes from each associated VPCs.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds support for detailed job progress status and S3 server-side output encryption. In addition, the anti-alias filter will now be automatically applied to all outputs

## __AWS IoT 1-Click Devices Service__
  - ### Features
    - This release adds tagging support for AWS IoT 1-Click Device resources. Use these APIs to add, remove, or list tags on Devices, and leverage the tags for various authorization and billing scenarios. This release also adds the ARN property for DescribeDevice response object.

## __AWS IoT Analytics__
  - ### Features
    - This change allows you to specify the number of versions of IoT Analytics data set content to be retained. Previously, the number of versions was managed implicitly via the setting of the data set's retention period.

## __AWS RoboMaker__
  - ### Features
    - Added additional progress metadata fields for robot deployments

## __Amazon Transcribe Service__
  - ### Features
    - Amazon Transcribe - With this release Amazon Transcribe enhances the custom vocabulary feature to improve accuracy by providing customization on pronunciations and output formatting.

## __Firewall Management Service__
  - ### Features
    - AWS Firewall Manager now allows customer to centrally enable AWS Shield Advanced DDoS protection for their entire AWS infrastructure, across accounts and applications.

## __URL Connection Http Client__
  - ### Bugfixes
    - Bypass ssl validations when `TRUST_ALL_CERTIFICATES` is set to true.

# __2.5.15__ __2019-03-22__
## __AWS IoT 1-Click Projects Service__
  - ### Features
    - This release adds tagging support for AWS IoT 1-Click Project resources. Use these APIs to add, remove, or list tags on Projects, and leverage the tags for various authorization and billing scenarios. This release also adds the ARN property to projects for DescribeProject and ListProject responses.

## __Amazon CloudSearch Domain__
  - ### Bugfixes
    - Use application/x-www-form-urlencoded as Content-Type for search API

## __Amazon Transcribe Service__
  - ### Features
    - Amazon Transcribe - support transcriptions from audio sources in German (de-DE) and Korean (ko-KR).

## __Netty NIO Http Client__
  - ### Features
    - Add sslProvider configuration in `NettyNioAsyncHttpClient.Builder`.

# __2.5.14__ __2019-03-21__
## __AWS IoT__
  - ### Features
    - This release adds the GetStatistics API for the AWS IoT Fleet Indexing Service, which allows customers to query for statistics about registered devices that match a search query. This release only supports the count statistics. For more information about this API, see https://docs.aws.amazon.com/iot/latest/apireference/API_GetStatistics.html

## __AWS SDK for Java v2__
  - ### Features
    - Automatically retry on `RequestThrottledException` error codes.

  - ### Bugfixes
    - Fix bug where the stream returned from a `ContentStreamProvider` is not closed after request execution. See [#1138](https://github.com/aws/aws-sdk-java-v2/issues/1138)

## __Amazon CloudWatch Events__
  - ### Features
    - Added 3 new APIs, and one additional parameter to the PutRule API, to support tagging of CloudWatch Events rules.

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release adds tags and tag-based access control support to Amazon Cognito User Pools.

## __Amazon Lightsail__
  - ### Features
    - This release adds the DeleteKnownHostKeys API, which enables Lightsail's browser-based SSH or RDP clients to connect to the instance after a host key mismatch.

## __Amazon S3__
  - ### Bugfixes
    - Fix bug in `ChecksumCalculatingInputStream` where methods not overridden, such as `close()`, are not called on the wrapped stream. See [#1138](https://github.com/aws/aws-sdk-java-v2/issues/1138).

## __Auto Scaling__
  - ### Features
    - Documentation updates for Amazon EC2 Auto Scaling

# __2.5.13__ __2019-03-20__
## __AWS CodePipeline__
  - ### Features
    - Add support for viewing details of each action execution belonging to past and latest pipeline executions that have occurred in customer's pipeline. The details include start/updated times, action execution results, input/output artifacts information, etc. Customers also have the option to add pipelineExecutionId in the input to filter the results down to a single pipeline execution.

## __AWSMarketplace Metering__
  - ### Features
    - This release increases AWS Marketplace Metering Service maximum usage quantity to 2147483647 and makes parameters usage quantity and dryrun optional.

## __Amazon Cognito Identity__
  - ### Features
    - This release adds tags and tag-based access control support to Amazon Cognito Identity Pools (Federated Identities).

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fix a bug that could pollute non SDK threads with `ThreadLocal`'s when allocating memory. See [#1133](https://github.com/aws/aws-sdk-java-v2/issues/1133)

## __Netty NIO Http Client__
  - ### Bugfixes
    - Fix failed test NettyNioAsyncHttpClientSpiVerificationTest when running with JDK11. See [#1038](https://github.com/aws/aws-sdk-java-v2/issues/1038)

# __2.5.12__ __2019-03-19__
## __AWS Config__
  - ### Features
    - AWS Config adds a new API called SelectResourceConfig to run advanced queries based on resource configuration properties.

## __AWS SDK for Java v2__
  - ### Features
    - Adds the Java vendor the user agent as well as using the updated user agent for all HTTP calls

## __Amazon Elastic Container Service for Kubernetes__
  - ### Features
    - Added support to control private/public access to the Kubernetes API-server endpoint

## __Amazon S3__
  - ### Features
    - Add support for getUrl operation. The API can be used to generate a URL that represents an object in Amazon S3. The url can only be used to download the object content if the object has public read permissions. Original issue: https://github.com/aws/aws-sdk-java-v2/issues/860

  - ### Bugfixes
    - Only set content type of S3 `CreateMultipartUploadRequest` if `Content-Type` header is not present and honor the overridden content type.

# __2.5.11__ __2019-03-18__
## __AWS Database Migration Service__
  - ### Features
    - S3 Endpoint Settings added support for 1) Migrating to Amazon S3 as a target in Parquet format 2) Encrypting S3 objects after migration with custom KMS Server-Side encryption. Redshift Endpoint Settings added support for encrypting intermediate S3 objects during migration with custom KMS Server-Side encryption.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix HeaderUnmarshaller to compare header ignoring cases.

## __Amazon Chime__
  - ### Features
    - This release adds support for the Amazon Chime Business Calling and Voice Connector features.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - DescribeFpgaImages API now returns a new DataRetentionSupport attribute to indicate if the AFI meets the requirements to support DRAM data retention. DataRetentionSupport is a read-only attribute.

# __2.5.10__ __2019-03-14__
## __AWS Certificate Manager__
  - ### Features
    - AWS Certificate Manager has added a new API action, RenewCertificate. RenewCertificate causes ACM to force the renewal of any private certificate which has been exported.

## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - AWS Certificate Manager (ACM) Private CA allows customers to manage permissions on their CAs. Customers can grant or deny AWS Certificate Manager permission to renew exported private certificates.

## __AWS Config__
  - ### Features
    - AWS Config - add ability to tag, untag and list tags for ConfigRule, ConfigurationAggregator and AggregationAuthorization resource types. Tags can be used for various scenarios including tag based authorization.

## __AWS IoT__
  - ### Features
    - In this release, AWS IoT introduces support for tagging OTA Update and Stream resources. For more information about tagging, see the AWS IoT Developer Guide.

## __Amazon CloudWatch__
  - ### Features
    - New Messages parameter for the output of GetMetricData, to support new metric search functionality.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds tagging support for Dedicated Host Reservations.

## __Amazon SageMaker Service__
  - ### Features
    - Amazon SageMaker Automatic Model Tuning now supports random search and hyperparameter scaling.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Backport `FixedChannelPool` fix from https://github.com/netty/netty/pull/7927, which ensures that the operation doesn't become blocked from closing the wrapped pool.
    - Fix a possible `NullPointerException` if `HttpOrHttp2ChannelPool` is closed while the protocol is still being determined. The operation is now done synchronously with other operations on the pool to prevent a data race.

# __2.5.9__ __2019-03-13__
## __AWS Config__
  - ### Features
    - Config released Remediation APIs allowing Remediation of Config Rules

## __Amazon CloudWatch Logs__
  - ### Features
    - Documentation updates for logs

## __Netty Nio HTTP Client__
  - ### Bugfixes
    - Update `SslCompletionEventHandler` to close channel for `SslCloseCompletionEvent` only if the channel is not currently in use. This would fix the race condition in the async clients causing incorrect IOException to be thrown when the service returns error response and closes the connection. See [#1076](https://github.com/aws/aws-sdk-java-v2/issues/1076)

# __2.5.8__ __2019-03-12__
## __AWSServerlessApplicationRepository__
  - ### Features
    - The AWS Serverless Application Repository now supports associating a ZIP source code archive with versions of an application.

## __Netty Nio Http Client__
  - ### Bugfixes
    - Fix a bug where the channel fails to be released if there is an exception thrown.

# __2.5.7__ __2019-03-11__
## __AWS Cost Explorer Service__
  - ### Features
    - The only change in this release is to make TimePeriod a required parameter in GetCostAndUsageRequest.

## __AWS Elastic Beanstalk__
  - ### Features
    - Elastic Beanstalk added support for tagging, and tag-based access control, of all Elastic Beanstalk resources.

## __AWS Glue__
  - ### Features
    - CreateDevEndpoint and UpdateDevEndpoint now support Arguments to configure the DevEndpoint.

## __AWS IoT__
  - ### Features
    - Documentation updates for iot

## __Amazon QuickSight__
  - ### Features
    - Amazon QuickSight user and group operation results now include group principal IDs and user principal IDs. This release also adds "DeleteUserByPrincipalId", which deletes users given their principal ID. The update also improves role session name validation.

## __Amazon Rekognition__
  - ### Features
    - Documentation updates for Amazon Rekognition

## __Amazon S3__
  - ### Bugfixes
    - Set `Content-Type` to `binary/octet-stream` for `S3#createMultipartRequest`. See [#1092](https://github.com/aws/aws-sdk-java-v2/issues/1092)

## __Apache Http Client__
  - ### Bugfixes
    - Updated to not set a default `Content-Type` if the header does not exist. Per [RFC7231](https://tools.ietf.org/html/rfc7231#page-11), we should let the recipient to decide if not known.

# __2.5.6__ __2019-03-08__
## __AWS CodeBuild__
  - ### Features
    - CodeBuild also now supports Git Submodules. CodeBuild now supports opting out of Encryption for S3 Build Logs. By default these logs are encrypted.

## __Amazon SageMaker Service__
  - ### Features
    - SageMaker notebook instances now support enabling or disabling root access for notebook users. SageMaker Neo now supports rk3399 and rk3288 as compilation target devices.

## __Amazon Simple Storage Service__
  - ### Features
    - Documentation updates for s3

# __2.5.5__ __2019-03-07__
## __AWS App Mesh__
  - ### Features
    - This release includes a new version of the AWS App Mesh APIs. You can read more about the new APIs here: https://docs.aws.amazon.com/app-mesh/latest/APIReference/Welcome.html.

## __AWS Elemental MediaLive__
  - ### Features
    - This release adds a MediaPackage output group, simplifying configuration of outputs to AWS Elemental MediaPackage.

## __AWS Greengrass__
  - ### Features
    - Greengrass group UID and GID settings can now be configured to use a provided default via FunctionDefaultConfig. If configured, all Lambda processes in your deployed Greengrass group will by default start with the provided UID and/or GID, rather than by default starting with UID "ggc_user" and GID "ggc_group" as they would if not configured. Individual Lambdas can also be configured to override the defaults if desired via each object in the Functions list of your FunctionDefinitionVersion.

## __AWS SDK For Java v2__
  - ### Bugfixes
    - Fix bug in the generated async clients where cancelling the `CompletableFuture` returned from an async operation does not result in cancelling the underlying HTTP request execution. In some cases, this can lead to unnecesarily keeping resources from being freed until the request execution finishes.

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces additional task definition parameters that enable you to define dependencies for container startup and shutdown, a per-container start and stop timeout value, as well as an AWS App Mesh proxy configuration which eases the integration between Amazon ECS and AWS App Mesh.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift-hosted instances can now securely access resources on other AWS services using IAM roles. See more details at https://aws.amazon.com/releasenotes/amazon-gamelift/.

## __Amazon Relational Database Service__
  - ### Features
    - You can configure your Aurora database cluster to automatically copy tags on the cluster to any automated or manual database cluster snapshots that are created from the cluster. This allows you to easily set metadata on your snapshots to match the parent cluster, including access policies. You may enable or disable this functionality while creating a new cluster, or by modifying an existing database cluster.

## __Auto Scaling__
  - ### Features
    - Documentation updates for autoscaling

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fix a bug where, if the future returned from the `NettyRequestExecutor#execute` is cancelled, the client continues to wait for the `Channel` acquire to complete, which leads to keeping potentially many resources around unnecessarily.

# __2.5.4__ __2019-03-06__
## __AWS Direct Connect__
  - ### Features
    - Exposed a new available port speeds field in the DescribeLocation api call.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix the generated ServiceMetadata classes for services that have PARTITION_OVERRIDDEN_ENDPOINTS

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds pagination support for ec2.DescribeVpcs, ec2.DescribeInternetGateways and ec2.DescribeNetworkAcls APIs

## __Amazon Elastic File System__
  - ### Features
    - Documentation updates for elasticfilesystem adding new examples for EFS Lifecycle Management feature.

# __2.5.3__ __2019-03-05__
## __AWS CodeDeploy__
  - ### Features
    - Documentation updates for codedeploy

## __AWS Elemental MediaLive__
  - ### Features
    - This release adds support for pausing and unpausing one or both pipelines at scheduled times.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Add missing non-service modules to bom. See [#1063](https://github.com/aws/aws-sdk-java-v2/issues/1063)

## __AWS Storage Gateway__
  - ### Features
    - ActivateGateway, CreateNFSFileShare and CreateSMBFileShare APIs support a new parameter: Tags (to be attached to the created resource). Output for DescribeNFSFileShare, DescribeSMBFileShare and DescribeGatewayInformation APIs now also list the Tags associated with the resource. Minimum length of a KMSKey is now 7 characters.

## __Amazon Textract__
  - ### Features
    - This release is intended ONLY for customers that are officially part of the Amazon Textract Preview program. If you are not officially part of the Amazon Textract program THIS WILL NOT WORK. Our two main regions for Amazon Textract Preview are N. Virginia and Dublin. Also some members have been added to Oregon and Ohio. If you are outside of any of these AWS regions, Amazon Textract Preview definitely will not work. If you would like to be part of the Amazon Textract program, you can officially request sign up here - https://pages.awscloud.com/textract-preview.html. To set expectations appropriately, we are aiming to admit new preview participants once a week until General Availability.

# __2.5.2__ __2019-03-04__
## __AWS Elemental MediaPackage__
  - ### Features
    - This release adds support for user-defined tagging of MediaPackage resources. Users may now call operations to list, add and remove tags from channels and origin-endpoints. Users can also specify tags to be attached to these resources during their creation. Describe and list operations on these resources will now additionally return any tags associated with them.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - This release updates AWS Systems Manager APIs to support service settings for AWS customers. A service setting is a key-value pair that defines how a user interacts with or uses an AWS service, and is typically created and consumed by the AWS service team. AWS customers can read a service setting via GetServiceSetting API and update the setting via UpdateServiceSetting API or ResetServiceSetting API, which are introduced in this release. For example, if an AWS service charges money to the account based on a feature or service usage, then the AWS service team might create a setting with the default value of "false". This means the user can't use this feature unless they update the setting to "true" and intentionally opt in for a paid feature.

# __2.5.1__ __2019-03-01__
## __AWS Auto Scaling Plans__
  - ### Features
    - Documentation updates for autoscaling-plans

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for modifying instance event start time which allows users to reschedule EC2 events.

# __2.5.0__ __2019-02-28__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix a bug in the code generator where the enum getter for a structure member is not being generated in some cases. Additionally, fix a bug that generated the wrong code for enum getters where the enum is not at the top level container but is nested, such as `List<List<EnumType>>`. This breaks the interface for affected services so the minor version is increased.
    - Fixed a bug where the request would fail of NoSuchElementException. This bug would affect `TranscribeStreaming#startStreamTranscription` request

## __Alexa For Business__
  - ### Features
    - This release adds the PutInvitationConfiguration API to configure the user invitation email template with custom attributes, and the GetInvitationConfiguration API to retrieve the configured values.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - AWS Systems Manager State Manager now supports associations using documents shared by other AWS accounts.

## __AmazonApiGatewayV2__
  - ### Features
    - Marking certain properties as explicitly required and fixing an issue with the GetApiMappings operation for ApiMapping resources.

## __Application Auto Scaling__
  - ### Features
    - Documentation updates for application-autoscaling

## __Netty Nio HTTP Client__
  - ### Bugfixes
    - Added try-catch blocks to prevent uncompleted future when exception is thrown.

# __2.4.17__ __2019-02-27__
## __AWS WAF__
  - ### Features
    - Documentation updates for waf

## __AWS WAF Regional__
  - ### Features
    - Documentation updates for waf-regional

## __core__
  - ### Bugfixes
    - Netty 4.1.33.Final

# __2.4.16__ __2019-02-26__
## __AWS Application Discovery Service__
  - ### Features
    - Documentation updates for discovery

## __AWS Cost and Usage Report Service__
  - ### Features
    - Adding support for Athena and new report preferences to the Cost and Usage Report API.

## __AWS Elemental MediaConvert__
  - ### Features
    - AWS Elemental MediaConvert SDK has added several features including support for: auto-rotation or user-specified rotation of 0, 90, 180, or 270 degrees; multiple output groups with DRM; ESAM XML documents to specify ad insertion points; Offline Apple HLS FairPlay content protection.

## __AWS OpsWorks for Chef Automate__
  - ### Features
    - Documentation updates for opsworkscm

## __AWS Organizations__
  - ### Features
    - Documentation updates for AWS Organizations

## __AWS Resource Groups__
  - ### Features
    - Documentation updates for Resource Groups API; updating description of Tag API.

## __Amazon Pinpoint__
  - ### Features
    - This release adds support for the Amazon Resource Groups Tagging API to Amazon Pinpoint, which means that you can now add and manage tags for Amazon Pinpoint projects (apps), campaigns, and segments. A tag is a label that you optionally define and associate with Amazon Pinpoint resource. Tags can help you categorize and manage these types of resources in different ways, such as by purpose, owner, environment, or other criteria. For example, you can use tags to apply policies or automation, or to identify resources that are subject to certain compliance requirements. A project, campaign, or segment can have as many as 50 tags. For more information about using and managing tags in Amazon Pinpoint, see the Amazon Pinpoint Developer Guide at https://docs.aws.amazon.com/pinpoint/latest/developerguide/welcome.html. For more information about the Amazon Resource Group Tagging API, see the Amazon Resource Group Tagging API Reference at https://docs.aws.amazon.com/resourcegroupstagging/latest/APIReference/Welcome.html.

## __Amazon S3__
  - ### Bugfixes
    - Fix the issue where NoSuchBucketException was not unmarshalled for `s3#getBucketPolicy` when the bucket doesn't exist. See [#1088](https://github.com/aws/aws-sdk-java-v2/issues/1088)

# __2.4.15__ __2019-02-25__
## __AWS CodeCommit__
  - ### Removals
    - Removing invalid "fips" region

## __AWS Cost Explorer Service__
  - ### Features
    - Added metrics to normalized units.

## __AWS Elasticache__
  - ### Removals
    - Removing invalid "fips" region

## __AWS Elemental MediaStore__
  - ### Features
    - This release adds support for access logging, which provides detailed records for the requests that are made to objects in a container.

## __AWS SDK for Java v2__
  - ### Removals
    - Removes invalid AWS regions that don't match the partition regex

## __Amazon DynamoDB__
  - ### Removals
    - Removing invalid "local" region

## __Amazon MTurk__
  - ### Removals
    - Removing invalid "sandbox" region

## __Amazon S3__
  - ### Removals
    - Removing invalid dualstack regions and s3-external-1

## __Auto Scaling__
  - ### Features
    - Added support for passing an empty SpotMaxPrice parameter to remove a value previously set when updating an Amazon EC2 Auto Scaling group.

## __Elastic Load Balancing__
  - ### Features
    - This release enables you to use the existing client secret when modifying a rule with an action of type authenticate-oidc.

# __2.4.14__ __2019-02-22__
## __AWS Cloud9__
  - ### Features
    - Adding EnvironmentLifecycle to the Environment data type.

## __AWS Glue__
  - ### Features
    - AWS Glue adds support for assigning AWS resource tags to jobs, triggers, development endpoints, and crawlers. Each tag consists of a key and an optional value, both of which you define. With this capacity, customers can use tags in AWS Glue to easily organize and identify your resources, create cost allocation reports, and control access to resources.

## __AWS Step Functions__
  - ### Features
    - This release adds support for tag-on-create. You can now add tags when you create AWS Step Functions activity and state machine resources. For more information about tagging, see AWS Tagging Strategies.

## __Amazon Athena__
  - ### Features
    - This release adds tagging support for Workgroups to Amazon Athena. Use these APIs to add, remove, or list tags on Workgroups, and leverage the tags for various authorization and billing scenarios.

# __2.4.13__ __2019-02-21__
## __AWS CodeBuild__
  - ### Features
    - Add support for CodeBuild local caching feature

## __AWS Organizations__
  - ### Features
    - Documentation updates for organizations

## __AWS Transfer for SFTP__
  - ### Features
    - Bug fix: increased the max length allowed for request parameter NextToken when paginating List operations

## __Amazon CloudWatch__
  - ### Features
    - Documentation updates for monitoring

## __Amazon Kinesis Video Streams__
  - ### Features
    - Documentation updates for Kinesis Video Streams

## __Amazon Kinesis Video Streams Archived Media__
  - ### Features
    - In this release, HLS playback of KVS streams can be configured to output MPEG TS fragments using the ContainerFormat parameter. HLS playback of KVS streams can also be configured to include the EXT-X-PROGRAM-DATE-TIME field using the DisplayFragmentTimestamp parameter.

## __Amazon Kinesis Video Streams Media__
  - ### Features
    - Documentation updates for Kinesis Video Streams

## __Amazon WorkDocs__
  - ### Features
    - Documentation updates for workdocs

# __2.4.12__ __2019-02-20__
## __AWS CodeCommit__
  - ### Features
    - This release adds an API for adding / updating / deleting / copying / moving / setting file modes for one or more files directly to an AWS CodeCommit repository without requiring a Git client.

## __AWS Direct Connect__
  - ### Features
    - Documentation updates for AWS Direct Connect

## __AWS Elemental MediaLive__
  - ### Features
    - This release adds support for VPC inputs, allowing you to push content from your Amazon VPC directly to MediaLive.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed an issue where the SDK could be over-retrying on signature errors.
    - Fixed an issue where the SDK could fail to adjust the local clock under skewed-clock conditions.

# __2.4.11__ __2019-02-19__
## __AWS Directory Service__
  - ### Features
    - This release adds support for tags during directory creation (CreateDirectory, CreateMicrosoftAd, ConnectDirectory).

## __AWS IoT__
  - ### Features
    - AWS IoT - AWS IoT Device Defender adds support for configuring behaviors in a security profile with statistical thresholds. Device Defender also adds support for configuring multiple data-point evaluations before a violation is either created or cleared.

## __Amazon Elastic File System__
  - ### Features
    - Amazon EFS now supports adding tags to file system resources as part of the CreateFileSystem API . Using this capability, customers can now more easily enforce tag-based authorization for EFS file system resources.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - AWS Systems Manager now supports adding tags when creating Activations, Patch Baselines, Documents, Parameters, and Maintenance Windows

# __2.4.10__ __2019-02-18__
## __AWS SDK for Java v2__
  - ### Features
    - Updated service endpoint metadata.

## __AWS Secrets Manager__
  - ### Features
    - This release increases the maximum allowed size of SecretString or SecretBinary from 4KB to 7KB in the CreateSecret, UpdateSecret, PutSecretValue and GetSecretValue APIs.

## __Amazon Athena__
  - ### Features
    - This release adds support for Workgroups to Amazon Athena. Use Workgroups to isolate users, teams, applications or workloads in the same account, control costs by setting up query limits and creating Amazon SNS alarms, and publish query-related metrics to Amazon CloudWatch.

# __2.4.9__ __2019-02-15__
## __AWS IoT__
  - ### Features
    - In this release, IoT Device Defender introduces support for tagging Scheduled Audit resources.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Attach `extendedRequestId` to `AwsResponseMetadata` if available for event streaming operations so that customers can retrieve it from response metadata

## __Amazon Chime__
  - ### Features
    - Documentation updates for Amazon Chime

## __Application Auto Scaling__
  - ### Features
    - Documentation updates for Application Auto Scaling

# __2.4.8__ __2019-02-14__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds tagging and ARN support for AWS Client VPN Endpoints.You can now run bare metal workloads on EC2 M5 and M5d instances. m5.metal and m5d.metal instances are powered by custom Intel Xeon Scalable Processors with a sustained all core frequency of up to 3.1 GHz. m5.metal and m5d.metal offer 96 vCPUs and 384 GiB of memory. With m5d.metal, you also have access to 3.6 TB of NVMe SSD-backed instance storage. m5.metal and m5d.metal instances deliver 25 Gbps of aggregate network bandwidth using Elastic Network Adapter (ENA)-based Enhanced Networking, as well as 14 Gbps of bandwidth to EBS.You can now run bare metal workloads on EC2 z1d instances. z1d.metal instances are powered by custom Intel Xeon Scalable Processors with a sustained all core frequency of up to 4.0 GHz. z1d.metal offers 48 vCPUs, 384 GiB of memory, and 1.8 TB of NVMe SSD-backed instance storage. z1d.metal instances deliver 25 Gbps of aggregate network bandwidth using Elastic Network Adapter (ENA)-based Enhanced Networking, as well as 14 Gbps of bandwidth to EBS.

## __Amazon Kinesis Video Streams__
  - ### Features
    - Adds support for Tag-On-Create for Kinesis Video Streams. A list of tags associated with the stream can be created at the same time as the stream creation.

# __2.4.7__ __2019-02-13__
## __AWS MediaTailor__
  - ### Features
    - This release adds support for tagging AWS Elemental MediaTailor resources.

## __Amazon Elastic File System__
  - ### Features
    - Customers can now use the EFS Infrequent Access (IA) storage class to more cost-effectively store larger amounts of data in their file systems. EFS IA is cost-optimized storage for files that are not accessed every day. You can create a new file system and enable Lifecycle Management to automatically move files that have not been accessed for 30 days from the Standard storage class to the IA storage class.

## __Amazon Rekognition__
  - ### Features
    - GetContentModeration now returns the version of the moderation detection model used to detect unsafe content.

# __2.4.6__ __2019-02-12__
## __AWS Lambda__
  - ### Features
    - Documentation updates for AWS Lambda

## __AWS Transcribe Streaming__
  - ### Features
    - Amazon Transcribe now supports US Spanish, which expands upon the existing language support for US English.

## __Netty Nio HTTP Client__
  - ### Bugfixes
    - Awaits `EventLoopGroup#shutdownGracefully` to complete when closing Netty client.

# __2.4.5__ __2019-02-11__
## __AWS CodeBuild__
  - ### Features
    - Add customized webhook filter support

## __AWS Elemental MediaPackage__
  - ### Features
    - Adds optional configuration for DASH to compact the manifest by combining duplicate SegmentTemplate tags. Adds optional configuration for DASH SegmentTemplate format to refer to segments by "Number" (default) or by "Time".

## __Amazon AppStream__
  - ### Features
    - This update enables customers to find the start time, max expiration time, and connection status associated with AppStream streaming session.

## __Amazon CloudWatch Logs__
  - ### Bugfixes
    - Fix infinite pagination bug in CloudWatchLogsClient.getLogEventsPaginator API. See https://github.com/aws/aws-sdk-java-v2/issues/1045

# __2.4.4__ __2019-02-08__
## __AWS Application Discovery Service__
  - ### Features
    - Documentation updates for the AWS Application Discovery Service.

## __AWS S3__
  - ### Bugfixes
    - Use request header to determine if checksum validation should be enabled for `s3#putObject`

## __AWS SDK for Java v2__
  - ### Features
    - Never initialie the default region provider chain if the region is always specified in the client builder.
    - Never initialize the default credentials provider chain if credentials are always specified in the client builder.

  - ### Bugfixes
    - Defer all errors raised when creating `ProfileCredentialsProvider` to the `resolveCredentials()` call.

## __Amazon Data Lifecycle Manager__
  - ### Features
    - This release is to correct the timestamp format to ISO8601 for the DateCreated and DateModified files in the GetLifecyclePolicy response object.

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS introduces the PutAccountSettingDefault API, an API that allows a user to set the default ARN/ID format opt-in status for all the roles and users in the account. Previously, setting the account's default opt-in status required the use of the root user with the PutAccountSetting API.

# __2.4.3__ __2019-02-07__
## __AWS Elemental MediaLive__
  - ### Features
    - This release adds tagging of channels, inputs, and input security groups.

## __AWS RoboMaker__
  - ### Features
    - Added support for tagging and tag-based access control for AWS RoboMaker resources. Also, DescribeSimulationJob now includes a new failureReason field to help debug simulation job failures

## __Amazon Elasticsearch Service__
  - ### Features
    - Feature: Support for three Availability Zone deployments

## __Amazon GameLift__
  - ### Features
    - This release delivers a new API action for deleting unused matchmaking rule sets. More details are available at https://aws.amazon.com/releasenotes/?tag=releasenotes%23keywords%23amazon-gamelift.

# __2.4.2__ __2019-02-06__
## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add Linux with SQL Server Standard, Linux with SQL Server Web, and Linux with SQL Server Enterprise to the list of allowed instance platforms for On-Demand Capacity Reservations.

## __Amazon FSx__
  - ### Features
    - New optional ExportPath parameter added to the CreateFileSystemLustreConfiguration object for user-defined export paths. Used with the CreateFileSystem action when creating an Amazon FSx for Lustre file system.

# __2.4.1__ __2019-02-05__
## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix an issue where an exception could be raised when configuring the idle connection reaper in the apache HTTP client [#1059](https://github.com/aws/aws-sdk-java-v2/issues/1059).

## __AWS Service Catalog__
  - ### Features
    - Service Catalog Documentation Update for ProvisionedProductDetail

## __AWS Shield__
  - ### Features
    - The DescribeProtection request now accepts resource ARN as valid parameter.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - ec2.DescribeVpcPeeringConnections pagination support

# __2.4.0__ __2019-02-04__
## __AWS CodeCommit__
  - ### Features
    - This release supports a more graceful handling of the error case when a repository is not associated with a pull request ID in a merge request in AWS CodeCommit.

## __AWS SDK for Java v2__
  - ### Features
    - Add support for `connectionTimeToLive`, `connectionMaxIdleTime` and `useIdleConnectionReaper` to the netty HTTP client.
    - Enable `useIdleConnectionReaper` by default for Netty and Apache.
    - Updated service endpoint metadata.

  - ### Bugfixes
    - Added a new handler ([#1041](https://github.com/aws/aws-sdk-java-v2/issues/1041)) to close channels which have triggered an SslCloseCompletionEvent and are no longer usable per [#452](https://github.com/aws/aws-sdk-java-v2/issues/452).
    - Fix the deadlock issue in `EventStreamAsyncResponseTransformer` for event streaming operations triggered in an edge case where customer subscriber signals `Subscription#request` the same time as `SdkPublisher` signals `Subscriber#onComplete`
    - Reduced netty client logging noise, by logging at a DEBUG level (instead of WARN) when encountering IO errors on channels not currently in use and not logging the whole stack trace.
    - Removed broken client methods: `BackupClient#getSupportedResourceTypes()` and `PinpointSmsVoiceClient.listConfigurationSets()`.

## __Amazon EC2 Container Service__
  - ### Features
    - This release of Amazon Elastic Container Service (Amazon ECS) introduces support for GPU workloads by enabling you to create clusters with GPU-enabled container instances.

## __Amazon WorkSpaces__
  - ### Features
    - This release sets ClientProperties as a required parameter.

## __Application Auto Scaling__
  - ### Features
    - Documentation updates for application-autoscaling

## __Netty NIO HTTP Client__
  - ### Features
    - Allows customers to enable wire logging with the Netty client at debug level.

# __2.3.9__ __2019-01-25__
## __AWS CodeCommit__
  - ### Features
    - The PutFile API will now throw new exception FilePathConflictsWithSubmodulePathException when a submodule exists at the input file path; PutFile API will also throw FolderContentSizeLimitExceededException when the total size of any folder on the path exceeds the limit as a result of the operation.

## __AWS Device Farm__
  - ### Features
    - Introduces a new rule in Device Pools - "Availability". Customers can now ensure they pick devices that are available (i.e., not being used by other customers).

## __AWS Elemental MediaLive__
  - ### Features
    - This release adds support for Frame Capture output groups and for I-frame only manifests (playlists) in HLS output groups.

## __AWS MediaConnect__
  - ### Features
    - This release adds support for tagging, untagging, and listing tags for existing AWS Elemental MediaConnect resources.

# __2.3.8__ __2019-01-24__
## __AWS CodeBuild__
  - ### Features
    - This release adds support for cross-account ECR images and private registry authentication.

## __Amazon CloudWatch Logs__
  - ### Features
    - Documentation updates for CloudWatch Logs

## __Amazon EC2 Container Registry__
  - ### Features
    - Amazon ECR updated the default endpoint URL to support AWS Private Link.

## __Amazon Pinpoint SMS and Voice Service__
  - ### Features
    - Added the ListConfigurationSets operation, which returns a list of the configuration sets that are associated with your account.

## __Amazon Relational Database Service__
  - ### Features
    - The Amazon RDS API allows you to add or remove Identity and Access Management (IAM) role associated with a specific feature name with an RDS database instance. This helps with capabilities such as invoking Lambda functions from within a trigger in the database, load data from Amazon S3 and so on

## __Elastic Load Balancing__
  - ### Features
    - Elastic Load Balancing now supports TLS termination on Network Load Balancers. With this launch, you can offload the decryption/encryption of TLS traffic from your application servers to the Network Load Balancer. This enables you to run your backend servers optimally and keep your workloads secure. Additionally, Network Load Balancers preserve the source IP of the clients to the back-end applications, while terminating TLS on the load balancer. When TLS is enabled on an NLB, Access Logs can be enabled for the load balancer, and log entries will be emitted for all TLS connections.

# __2.3.7__ __2019-01-23__
## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Added TagOnCreate parameter to the CreateCertificateAuthority operation, updated the Tag regex pattern to align with AWS tagging APIs, and added RevokeCertificate limit.

## __AWS SDK for Java v2__
  - ### Features
    - Redact potentially-sensitive data from the `toString` of service request and response objects.

  - ### Bugfixes
    - Fixed the time marshalling issue when CBOR is disabled. See [#1023](https://github.com/aws/aws-sdk-java-v2/issues/1023)

## __Amazon WorkLink__
  - ### Features
    - This is the initial SDK release for Amazon WorkLink. Amazon WorkLink is a fully managed, cloud-based service that enables secure, one-click access to internal websites and web apps from mobile phones. With Amazon WorkLink, employees can access internal websites as seamlessly as they access any other website. IT administrators can manage users, devices, and domains by enforcing their own security and access policies via the AWS Console or the AWS SDK.

## __AmazonApiGatewayManagementApi__
  - ### Features
    - Fixes a typo in the 'max' constraint.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Add `OneTimeReadTimeoutHanlder` to requests with `expect: 100-continue` header to avoid unexpected `ReadTimeoutException`. See [#954](https://github.com/aws/aws-sdk-java-v2/issues/954)

# __2.3.6__ __2019-01-21__
## __AWS Application Discovery Service__
  - ### Features
    - The Application Discovery Service's import APIs allow you to import information about your on-premises servers and applications into ADS so that you can track the status of your migrations through the Migration Hub console.

## __AWS Database Migration Service__
  - ### Features
    - Update for DMS TestConnectionSucceeds waiter

## __Amazon AppStream__
  - ### Features
    - This API update includes support for tagging Stack, Fleet, and ImageBuilder resources at creation time.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - AWS Systems Manager State Manager now supports configuration management of all AWS resources through integration with Automation.

## __Firewall Management Service__
  - ### Features
    - This release provides support for cleaning up web ACLs during Firewall Management policy deletion. You can now enable the DeleteAllPolicyResources flag and it will delete all system-generated web ACLs.

# __2.3.5__ __2019-01-18__
## __AWS Glue__
  - ### Features
    - AllocatedCapacity field is being deprecated and replaced with MaxCapacity field

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adjust EC2's available instance types.

# __2.3.4__ __2019-01-17__
## __AWS Lambda__
  - ### Features
    - Documentation updates for AWS Lambda

## __Amazon Lightsail__
  - ### Features
    - This release adds functionality to the CreateDiskSnapshot API that allows users to snapshot instance root volumes. It also adds various documentation updates.

## __Amazon Pinpoint__
  - ### Features
    - This release updates the PutEvents operation. AppPackageName, AppTitle, AppVersionCode, SdkName fields will now be accepted as a part of the event when submitting events.

## __Amazon Rekognition__
  - ### Features
    - GetLabelDetection now returns bounding box information for common objects and a hierarchical taxonomy of detected labels. The version of the model used for video label detection is also returned. DetectModerationLabels now returns the version of the model used for detecting unsafe content.

# __2.3.3__ __2019-01-16__
## __AWS Backup__
  - ### Features
    - AWS Backup is a unified backup service designed to protect AWS services and their associated data. AWS Backup simplifies the creation, migration, restoration, and deletion of backups, while also providing reporting and auditing

## __AWS Cost Explorer Service__
  - ### Features
    - Removed Tags from the list of GroupBy dimensions available for GetReservationCoverage.

## __Amazon DynamoDB__
  - ### Features
    - Amazon DynamoDB now integrates with AWS Backup, a centralized backup service that makes it easy for customers to configure and audit the AWS resources they want to backup, automate backup scheduling, set retention policies, and monitor all recent backup and restore activity. AWS Backup provides a fully managed, policy-based backup solution, simplifying your backup management, and helping you meet your business and regulatory backup compliance requirements. For more information, see the Amazon DynamoDB Developer Guide.

## __URLConnection HTTP Client__
  - ### Bugfixes
    - Fix NullPointer of AbortableInputStream delegate if there is no body within UrlConnectionHttpClient

# __2.3.2__ __2019-01-14__
## __AWS Elemental MediaConvert__
  - ### Features
    - IMF decode from a Composition Playlist for IMF specializations App [#2](https://github.com/aws/aws-sdk-java-v2/issues/2) and App [#2](https://github.com/aws/aws-sdk-java-v2/issues/2)e; up to 99 input clippings; caption channel selection for MXF; and updated rate control for CBR jobs. Added support for acceleration in preview

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fixed the bug where query parameters are incorrectly moved to body in async clients. See [#958](https://github.com/aws/aws-sdk-java-v2/issues/958)

## __AWS Storage Gateway__
  - ### Features
    - JoinDomain API supports two more parameters: organizational unit(OU) and domain controllers. Two new APIs are introduced: DetachVolume and AttachVolume.

# __2.3.1__ __2019-01-11__
## __AWS RDS DataService__
  - ### Features
    - Documentation updates for RDS Data API.

## __AWS SDK for Java v2__
  - ### Features
    - Updated to the latest service models.

## __Amazon Elastic MapReduce__
  - ### Features
    - Documentation updates for Amazon EMR

# __2.3.0__ __2019-01-09__
## __AWS SDK for Java v2__
  - ### Features
    - Updated to the latest service models.

  - ### Bugfixes
    - ChecksumValidatingPublisher deals with any packetization of the incoming data. See https://github.com/aws/aws-sdk-java-v2/issues/965
    - Fix an issue where dates were being unmarshalled incorrectly for the CBOR protocol used by Amazon Kinesis.
    - Make default `asyncFutureCompletionExeuctor` a true multi-threads executor. See [#968](https://github.com/aws/aws-sdk-java-v2/issues/968)

## __AWS STS__
  - ### Bugfixes
    - Changed the region resolution logic for `role_arn`-based profiles: 1. Check for a `region` property in the same profile as the `role_arn` definition. 2. Check the default region chain. 3. Fall back to the global endpoint and `us-east-1` signing. Fixes [#988](https://github.com/aws/aws-sdk-java-v2/issues/988).

## __Amazon S3__
  - ### Bugfixes
    - Fix `SyncChecksumValidationInterceptor` and `AsyncChecksumValidationInterceptor` to use `long` instead of `int` for contentLength`. See [#963](https://github.com/aws/aws-sdk-java-v2/issues/963)

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Preserve interrupted flag if `Http2MultiplexedChannelPool#close()` interrupted.

## __URLConnection HTTP Client__
  - ### Bugfixes
    - Disable following redirects automatically since doing so causes SDK response handling to fail

## __core__
  - ### Features
    - Jackson 2.9.7 -> 2.9.8

# __2.2.0__ __2018-12-14__
## __AWS SDK for Java v2__
  - ### Features
    - Adds the operation name of the calling API to the ExecutionContext class. This exposes a way to get the API name from within an ExecutionInterceptor.
    - Updated to the latest service models.

## __Amazon S3__
  - ### Bugfixes
    - Modify type of S3Object#size member from integer to long. This is a breaking change for customers who are using the size() method currently
    - S3 putObject API using UrlConnectionHttpClient goes into infinite loop. See https://github.com/aws/aws-sdk-java-v2/pull/942 for more details.

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fix a bug where it's possible for an HTTP2 channel pool to be closed while some channels are still being released causing them to be left open and leaked.

## __URLConnection HTTP Client__
  - ### Features
    - Adding a hook to enable custom creation of the initial `HttpURLConnection`. This enables customers to control how a connection is established for a given `URL` including handling any required proxy configuration etc.

# __2.1.4__ __2018-12-07__
## __AWS CodeDeploy__
  - ### Features
    - Supporting AWS CodeDeploy

## __AWS SDK for Java v2__
  - ### Features
    - Add `modifyException` API to `ExecutionInterceptor`.
    - Add application/gzip mime type
    - Update spot bugs version to 3.1.9
    - Updated to the latest service models.

  - ### Bugfixes
    - Fix infinite stream of results bug in auto paginator APIs when the next token is an empty string
    - Fixes nullpointerexception when server responds with null values in map.
    - Use the class loader that loaded the SDK to load the HTTP implementations. See [#56](https://github.com/aws/aws-sdk-java-v2/issues/56)

## __Amazon S3__
  - ### Bugfixes
    - Turns off trailing checksums when using SSE-C or SSE-KMS
    - Update S3 headObject/headBucket operations to throw NoSuchKey/NoSuchException when S3 is returning 404. See [#123](https://github.com/aws/aws-sdk-java-v2/issues/123), [#544](https://github.com/aws/aws-sdk-java-v2/issues/544)

## __Netty NIO Async HTTP Client__
  - ### Bugfixes
    - Close created `ChannelPool`s in `close()` method.
    - Fix the issue where streaming requests with `Expect: 100-continue` header sometimes are hanging because 100Continue response message is not being read automatically. See [#459](https://github.com/aws/aws-sdk-java-v2/issues/459)

## __core__
  - ### Features
    - Netty 4.1.32.Final

# __2.1.3__ __2018-11-29__
## __AWS SDK for Java v2__
  - ### Features
    - Updated to the latest service models.

# __2.1.2__ __2018-11-28__
## __AWS SDK for Java v2__
  - ### Features
    - Updated to the latest service models.

## __core__
  - ### Features
    - Jackson 2.9.6 -> 2.9.7

# __2.1.1__ __2018-11-27__
## __AWS Organizations__
  - ### Bugfixes
    - Add `organizations` to `aws-sdk-java` module.

## __AWS SDK for Java V2__
  - ### Bugfixes
    - Fixes Issue [#864](https://github.com/aws/aws-sdk-java-v2/issues/864) by checking for embedded JSON objects while unmarshalling bytes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated to the latest service models.
    - Updated to the latest service models.

  - ### Bugfixes
    - Fix async pagination javadocs to use the correct method name `SdkPublisher#subscribe`.
    - Fixed an issue where close() and abort() weren't being honored for streaming responses in all cases.
    - Preserve computedChecksum in `ChecksumValidatingInputStream` so that it doesn't throw error if it validates more than once. See [#873](https://github.com/aws/aws-sdk-java-v2/issues/873)

# __2.1.0__ __2018-11-19__
## __AWS SDK for Java v2__
  - ### Features
    - AWS SDK for Java v2 is generally available now. To get started, please see this [blog post](https://aws.amazon.com/blogs/developer/aws-sdk-for-java-2-x-released/).
    - Update Netty version to Netty 4.1.31.Final

  - ### Bugfixes
    - Temporarily removed OSGi support because the Netty HTTP client does not yet support it. See [#726](https://github.com/aws/aws-sdk-java-v2/issues/726)

## __Netty NIO Async HTTP Client__
  - ### Bugfixes
    - Not invoke fireExceptionCaught if the channel is not active. see [#452](https://github.com/aws/aws-sdk-java-v2/issues/452)

# __2.0.0-preview-13__ __2018-11-13__
## __AWS SDK for Java v2__
  - ### Features
    - Add `Automatic-Module-Name` manifest entry.
    - Add `AwsResponseMetadata` support to allow users to retrieve metadata information such as `requestId`, `extendedRequestId` from the response. see [#670](https://github.com/aws/aws-sdk-java-v2/issues/670)
    - Add apiCallTimeout and apiCallAttemptTimeout feature for synchronous calls.
    - Guava 23.0 -> 26.0
    - upgrade maven-bundle-plugin -> 4.0.0

  - ### Bugfixes
    - Attach `SdkHttpResponse` to the responses of event streaming operations.

## __AWS Security Token Service__
  - ### Features
    - Added supplier functionality to StsAssumeRoleWithSamlCredentialProvider.  This allows for the saml assertion to be refreshed before getting new credentials from STS.

## __AWS Step Function__
  - ### Removals
    - Remove AWS Step Function high level library for now. We will add them in the future.

## __Amazon S3__
  - ### Features
    - Add support for automatically decoding URL-encoded parts of the ListObjects and ListObjectsV2 responses. See https://docs.aws.amazon.com/AmazonS3/latest/API/v2-RESTBucketGET.html and https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html.

  - ### Bugfixes
    - Update S3 `EndpointAddressInterceptor` to honor request protocol.

## __Apache HTTP Client__
  - ### Features
    - Add support for idle connection reaping.

## __core__
  - ### Features
    - Apache HttpClient 4.5.5 -> 4.5.6
    - Netty 4.1.28 -> 4.1.30

# __2.0.0-preview-12__ __2018-09-18__
## __AWS SDK for Java v2__
  - ### Features
    - Add mfa_serial to ProfileProperty
    - Allow clients to add Socket Channel Option
    - Implement apiCallAttemptTimeout and apiCallTimeout feature for asynchrounous calls. Customers can specify timeout via `ClientOverrideConfiguaration.Builder#apiCallTimeout(Duration)` or `RequestOverrideConfiguration.Builder#apiCallAttemptTimeout(Duration)`. Note: this feature is only implemented for asynchrounous api calls.
    - Improve logging for debuggability. see `SdkStandardLogger`.
    - Refactored all services to make module names match the service id from the service model
    - Removed sdk-core dependency from the profiles module. This allows reading from profile files without pulling in the rest of the SDK.
    - Replacing legacy `HttpResponse` with `SdkHttpFullResponse`.
    - Update service models to be current as of 2018-09-07.

  - ### Bugfixes
    - Fix Response Fetcher hasNextPage to check if the output token is non null or non empty if it is a collection or map type. Related to [#677](https://github.com/aws/aws-sdk-java-v2/issues/677)
    - RetryPolicy bug fix: adding throttlingBackoffStrategy to `RetryPolicy.Builder`. see [#646](https://github.com/aws/aws-sdk-java-v2/issues/646)

## __AWS STS__
  - ### Features
    - Add the ability to provide a Supplier<AssumeRoleRequest> to StsAssumeRoleCredentialsProvider

## __Aamazon S3__
  - ### Bugfixes
    - Fix NPE for S3 GET request using http protocol. see [#612](https://github.com/aws/aws-sdk-java-v2/issues/612)

## __Amazon SimpleDB__
  - ### Removals
    - Amazon SimpleDB module is removed from the SDK 2.0. To use SimpleDB, use SDK 1.11.x. Note that you can run SDK 1.11 and 2.0 in the same application.

## __runtime__
  - ### Bugfixes
    - Netty 4.1.26.Final -> 4.1.28.Final

# __2.0.0-preview-11__ __2018-07-30__
## __AWS SDK for Java v2__
  - ### Features
    - Accept `SdkBytes` and `byte[]` instead of `ByteBuffer` in generated setters.
    - Add support to disable EC2 instance metadata service usage via environment variable and system property. [#430](https://github.com/aws/aws-sdk-java-v2/issues/430)
    - Caching `XPathFactory` to improve performance of exception handling for services using XML protocol
    - Exceptions use builders and are immutable.
    - Incorporate the [Reactive Streams Technology Compatibility Kit](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck) and ensure current implementations are compliant. [#519](https://github.com/aws/aws-sdk-java-v2/issues/519)
    - Modules (annotations, auth, sdk-core, aws-core, profiles, regions) are refactored under the core module.
    - Refactor signer interfaces to be independent from Amazon/AWS specific classes. Signer interfaces expose a sign method that takes in the request to sign and ExecutionAttributes instance. Parameters needed for signing are to be passed through ExecutionAttributes. SDK signer implementations have overloaded sign methods that can take either generic ExecutionAttributes or modeled params classes as convenience for customers.
    - Region class clean up including the following: - Flattened GovCloud - Renamed `Region.value()` to `Region.id()` - Dropped `get` prefix in the method names. eg: `getRegions()` -> `regions()`
    - Renamed all non-service enums to be singular, not plural.
    - Renaming `SdkBuilder.apply()` -> `SdkBuilder.applyMutation()` and renaming `ResponseTransformer.apply()` to `ResponseTransformer.transform()`.
    - Return `SdkBytes` instead of `ByteBuffer` from generated getters.
    - Update all service models to follow V2 naming convention. eg: `WAFException` -> `WafException`
    - Update service name in clients, requests and exceptions to match 2.0 naming conventions (eg. DynamoDBClient -> DynamoDbClient)
    - Updated `AwsCredentials` to interface implemented by `AwsBasicCredentials` and `AwsSessionCredentials`. Renamed `AwsCredentialsProvider.getCredentials()` to `AwsCredentialsProvider.resolveCredentials()`.
    - Use auto constructed containers for list and map members. [#497](https://github.com/aws/aws-sdk-java-v2/pull/497), [#529](https://github.com/aws/aws-sdk-java-v2/pull/529), [#600](https://github.com/aws/aws-sdk-java-v2/pull/600)
    - Various AsyncClient Refactors:\n - Drop async prefix in `SdkAyncClientBuilder`: `SdkAsyncClientBuilder.asyncHttpClientBuilder() -> SdkAsyncClientBuilder.httpClientBuilder()`\n - Create `SdkEventLoopGroup` to allow users to provide `EventLoopGroup` and `ChannelFactory`.
    - upgrade Netty 4.1.22.Final to Netty 4.1.26.Final

  - ### Deprecations
    - Deprecating `QueryStringSigner` in favor of `Aws4Signer`.

  - ### Removals
    - Make paginators resume method private.(We will re-add the feature in the future)
    - Removing gzipEnabled client configuration.

## __AWS WAF Regional__
  - ### Features
    - AWS Waf Regional clients are now in `software.amazon.awssdk.services.waf.regional` package.

## __Amazon DynamoDB__
  - ### Features
    - Add default DynamoDB specific retry policy.
    - Update DynamoDB default max retry count to 8. Related to [#431](https://github.com/aws/aws-sdk-java-v2/issues/431)

## __Amazon DynamoDB Streams__
  - ### Features
    - Dynamodb Streams clients are now in `software.amazon.awssdk.services.dynamodb.streams` package.

## __Amazon S3__
  - ### Features
    - Move `AWSS3V4Signer` to auth module.

## __Netty NIO Async HTTP Client__
  - ### Bugfixes
    - Fix the Netty async client to stop publishing to the request stream once `Content-Length` is reached.

# __2.0.0-preview-10__ __2018-05-25__
## __AWS SDK for Java v2__
  - ### Features
    - Add [SdkHttpResponse](https://github.com/aws/aws-sdk-java-v2/blob/master/http-client-spi/src/main/java/software/amazon/awssdk/http/SdkHttpResponse.java) to [SdkResponse](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/core/SdkResponse.java) so that customers can retrieve Http data such as headers, status code from the response object.
    - Add a standard User-Agent when making requests to the metadata service.  User-Agent pattern: aws-sdk-java/<version>
    - Added Consumer<Builder>-style methods for all client overloads.
    - Added Consumer<Builder>-style methods for vararg parameters.
    - AsyncResponseTransformer byte array and string methods now match the sync model.
    - Include root causes in the exception message from AWSCredentialsProviderChain to ease troubleshooting.
    - Moved AWS specific retry policies to aws-core module, created AwsServiceException and moved isThrottlingException and isClockSkewException methods to SdkServiceException.
    - Renamed "Bytes" overload for streaming operations to "AsBytes", and "String" overload for enums to "AsString"
    - Renamed AsyncRequestProvider to AsyncRequestBody to better match sync's RequestBody
    - Renamed AsyncResponseHandler to AsyncResponseTransformer and StreamingResponseHandler to ResponseTransformer.
    - Renamed `AdvancedServiceConfiguration` to `ServiceConfiguration`
    - Renamed `RequestOverrideConfig` to `RequestOverrideConfiguration` to match `ClientOverrideConfiguration` naming.
    - Simplified configuration of HTTP clients.
    - Split core module to regions, profiles, auth, aws-core and core modules.[#27](https://github.com/aws/aws-sdk-java-v2/issues/27)
    - Updating default retry policy to include newly added conditions.

  - ### Removals
    - Remove httpRequestTimeout and totalExecutionTimeout features

## __AWS Secrets Manager__
  - ### Features
    - Add AWS Secrets Manager to v2.

## __Amazon S3__
  - ### Features
    - Renamed `S3AdvancedConfiguration` to `S3Configuration`

# __2.0.0-preview-9__ __2018-03-20__
## __AWS Lambda__
  - ### Features
    - Added latest model for new service features.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Fix default user agent to comply with [RFC 7231](https://tools.ietf.org/html/rfc7231#section-5.5.3). Related to [#80](https://github.com/aws/aws-sdk-java-v2/issues/80)
    - Maven artifact software.amazon.awssdk:bom no longer includes non-SDK dependencies.

# __2.0.0-preview-8__ __2018-02-02__
## __AWS SDK for Java v2__
  - ### Features
    - Added Consumer<Builder> methods to multiple locations where they were previously missing.
    - Added `SdkClient` base interface that all service clients implement.
    - Added and standardized `toString` implementations of public data classes.
    - Adding the following services from re:invent 2017:
       - Alexa For Business
       - AWS Migration Hub
       - AWS Cost Explorer
       - AWS Cloud9
       - AWS CloudHSM V2
       - Amazon Comprehend
       - AWS Glue
       - Amazon GuardDuty
       - Amazon Kinesis Video Streams
       - AWS Elemental MediaConvert
       - AWS Elemental MediaLive
       - AWS Elemental MediaPackage
       - AWS Elemental MediaStore
       - AWS Mobile
       - AmazonMQ
       - AWS Price List
       - AWS Resource Groups
       - Amazon SageMaker
       - AWS Serverless Application Repository
       - Amazon Route 53 Auto Naming
       - Amazon Translate
       - Amazon WorkMail
    - Setting `Content-Type` header for streaming requests. Related to [#357](https://github.com/aws/aws-sdk-java-v2/issues/357)
    - upgrade Netty 4.1.17.Final to 4.1.19.Final

  - ### Bugfixes
    - Fixed issue where error message in S3 exceptions could be "null" if the exception did not have a modeled type.

## __Amazon CloudWatch__
  - ### Features
    - Added pagination configuration to CloudWatch

## __Netty NIO Async HTTP Client__
  - ### Bugfixes
    - Fix race condition in the async client causing instability when making multiple concurent requests. Fixes [#202](https://github.com/aws/aws-sdk-java-v2/issues/202)

# __2.0.0-preview-7__ __2017-12-15__
## __AWS SDK for Java v2__
  - ### Features
    - Added `Bytes` methods to all streaming operations. These methods will load the service response into memory and return a `ResponseBytes` object that eases conversion into other types, like strings. eg. `String object = s3.getObjectBytes(request).asUtf8String()`. [#324](https://github.com/aws/aws-sdk-java-v2/pull/324)
    - Added `ProfileCredentialsProvider.create("profile-name")` helper to `ProfileCredentialsProvider` to account for common use-case where only profile name is provided. [#347](https://github.com/aws/aws-sdk-java-v2/pull/347)
    - Adds convenience type overloads to allow easier to use types on modeled objects. [#336](https://github.com/aws/aws-sdk-java-v2/pull/336)
    - Automatically retry streaming downloads to a file if they fail or are interrupted. [#324](https://github.com/aws/aws-sdk-java-v2/pull/324)
    - Implementation of a generic HTTP credential provider used to get credentials from an container metadata service. Replica of v1 [implementation](https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/auth/ContainerCredentialsProvider.java#L108) [#328](https://github.com/aws/aws-sdk-java-v2/pull/328)
    - Refactors the exceptions used by the SDK. [#301](https://github.com/aws/aws-sdk-java-v2/pull/301)
    - Remove the legacy `AmazonWebServiceRequest`, `AmazonWebServiceResult`, and `AmazonWebServiceResponse` classes. They are replaced with `AwsRequest` and `AwsResponse`. [#289](https://github.com/aws/aws-sdk-java-v2/issues/289)
    - Updated profile-based region and credential loading to more closely mirror the behavior in the AWS CLI. Notably, profile names in `~/.aws/config` must be prefixed with "profile " (except for the default profile) and profile names in `~/.aws/credentials` must not be prefixed with "profile ". [#296](https://github.com/aws/aws-sdk-java-v2/pull/296)
    - Upgrade maven-compiler-plugin from 3.6.0 to 3.7.0
    - Upgraded dependencies
       * Wiremock (com.github.tomakehurst:wiremock) 1.55 -> 2.12.0
       * Json Path (com.jayway.jsonpath:json-path) 2.2.0 -> 2.4.0
    - upgrade to Jackson 2.9.3

  - ### Removals
    - Remove easymock as a dependency, mockito should be used for all mocking going forward. [#348](https://github.com/aws/aws-sdk-java-v2/pull/348)
    - Removed the following unused dependencies [#349](https://github.com/aws/aws-sdk-java-v2/issues/349):
       * org.eclipse:text
       * info.cukes:cucumber-java
       * info.cukes:cucumber-junit
       * info.cukes:cucumber-guice
       * com.google.inject:guice
       * org.bouncycastle:bcprov-jdk15on
       * com.google.guava:guava
       * io.burt:jmespath-jackson
       * javax.annotation:javax.annotation-api

## __Amazon S3__
  - ### Bugfixes
    - Fixing exception unmarshalling for S3. [#297](https://github.com/aws/aws-sdk-java-v2/issues/297)

## __Netty NIO Async HTTP Client__
  - ### Bugfixes
    - Fixes Issue [#340](https://github.com/aws/aws-sdk-java-v2/issues/340) where connection acquisition time was calculated incorrectly in the Netty client.

# __2.0.0-preview-6__ __2017-12-06__
## __AWS AppSync__
  - ### Features
    - Adding AWS AppSync based on customer request. [#318](https://github.com/aws/aws-sdk-java-v2/pull/318)

## __AWS Lambda__
  - ### Removals
    - Removed high-level utilities. [#247](https://github.com/aws/aws-sdk-java-v2/pull/247)

## __AWS SDK for Java v2__
  - ### Features
    - Add paginators-1.json file for some services [#298](https://github.com/aws/aws-sdk-java-v2/pull/298)
    - Added a primitive `Waiter` class for simplifying poll-until-condition-is-met behavior. [#300](https://github.com/aws/aws-sdk-java-v2/pull/300)
    - Adding Consumer<Builder> to overrideConfiguration on ClientBuilder [#291](https://github.com/aws/aws-sdk-java-v2/pull/291)
    - Adding helper to Either that allows construction from two possibly null values [#292](https://github.com/aws/aws-sdk-java-v2/pull/292)
    - Adding knownValues static to enum generation [#218](https://github.com/aws/aws-sdk-java-v2/pull/218)
    - Adding validation to Region class [#261](https://github.com/aws/aws-sdk-java-v2/pull/261)
    - Converted all wiremock tests to run as part of the build. [#260](https://github.com/aws/aws-sdk-java-v2/pull/260)
    - Enhanced pagination for synchronous clients[#207](https://github.com/aws/aws-sdk-java-v2/pull/207)
    - Implementing Consumer<Builder> fluent setter pattern on client operations [#280](https://github.com/aws/aws-sdk-java-v2/pull/280)
    - Implementing Consumer<Builder> fluent setters pattern on model builders. [#278](https://github.com/aws/aws-sdk-java-v2/pull/278)
    - Making it easier to supply async http configuration. [#274](https://github.com/aws/aws-sdk-java-v2/pull/274)
    - Refactoring retry logic out to separate class [#177](https://github.com/aws/aws-sdk-java-v2/pull/177)
    - Removing unnecessary javax.mail dependency [#312](https://github.com/aws/aws-sdk-java-v2/pull/312)
    - Replacing constructors with static factory methods [#284](https://github.com/aws/aws-sdk-java-v2/pull/284)
    - Retry policy refactor [#190](https://github.com/aws/aws-sdk-java-v2/pull/190)
    - Update latest models for existing services [#299](https://github.com/aws/aws-sdk-java-v2/pull/299)
    - Upgrade dependencies to support future migration to Java 9. [#271](https://github.com/aws/aws-sdk-java-v2/pull/271)
    - Upgraded dependencies:
      * javapoet 1.8.0 -> 1.9.0 [#311](https://github.com/aws/aws-sdk-java-v2/pull/311)
      * Apache HttpClient 4.5.2 -> 4.5.4 [#308](https://{github.com/aws/aws-sdk-java-v2/pull/308)
      * Jackson 2.9.1 -> 2.9.2 [#310](https://github.com/aws/aws-sdk-java-v2/pull/310)
      * Netty 4.1.13 -> 4.1.17 [#309](https://github.com/{aws/aws-sdk-java-v2/pull/309)
    - Use java.util.Objects to implement equals, hashCode [#294](https://github.com/aws/aws-sdk-java-v2/pull/294)

  - ### Bugfixes
    - Attempting to fix class-loader exception raised on gitter. [#216](https://github.com/aws/aws-sdk-java-v2/pull/216)
    - Call doClose in HttpClientDependencies#close method [#268](https://github.com/aws/aws-sdk-java-v2/pull/268)
    - Fixing bundle exports [#281](https://github.com/aws/aws-sdk-java-v2/pull/281)

  - ### Removals
    - Delete old jmespath AST script [#266](https://github.com/aws/aws-sdk-java-v2/pull/266)
    - Remove current waiter implementation. [#258](https://github.com/aws/aws-sdk-java-v2/pull/258)
    - Removed policy builder. [#259](https://github.com/aws/aws-sdk-java-v2/pull/259)
    - Removed progress listeners until they can be updated to V2 standards. [#285](https://github.com/aws/aws-sdk-java-v2/pull/285)

## __Amazon CloudFront__
  - ### Removals
    - Removed high-level cloudfront utilities. [#242](https://github.com/aws/aws-sdk-java-v2/pull/242)

## __Amazon DynamoDB__
  - ### Features
    - Adding some helpers for being able to create DyanmoDB AttributeValues. [#276](https://github.com/aws/aws-sdk-java-v2/pull/276)

  - ### Bugfixes
    - Fixed TableUtils that broke with enum change. [#235](https://github.com/aws/aws-sdk-java-v2/pull/235)

## __Amazon EC2__
  - ### Removals
    - Removed high-level utilities. [#244](https://github.com/aws/aws-sdk-java-v2/pull/244)

## __Amazon EMR__
  - ### Removals
    - Removed high-level utilities. [#245](https://github.com/aws/aws-sdk-java-v2/pull/245)

## __Amazon Glacier__
  - ### Removals
    - Removed high-level utilities. [#246](https://github.com/aws/aws-sdk-java-v2/pull/246)

## __Amazon Polly__
  - ### Removals
    - Removed polly presigners until they can be updated for V2. [#287](https://github.com/aws/aws-sdk-java-v2/pull/287)

## __Amazon S3__
  - ### Features
    - Adding utility that creates temporary bucket name using user-name  [#234](https://github.com/aws/aws-sdk-java-v2/pull/234)

## __Amazon SES__
  - ### Removals
    - Removed high-level utilities. [#248](https://github.com/aws/aws-sdk-java-v2/pull/248)

## __Amazon SNS__
  - ### Removals
    - Removed high-level utilities. [#255](https://github.com/aws/aws-sdk-java-v2/pull/255)

## __Amazon SQS__
  - ### Bugfixes
    - Porting SQS test to make use of async and hopefully resolve the bug [#240](https://github.com/aws/aws-sdk-java-v2/pull/240)

  - ### Removals
    - Removed high-level utilities and the interceptor that rewrites the endpoint based on the SQS queue. [#238](https://github.com/aws/aws-sdk-java-v2/pull/238)

## __Amazon SimpleDB__
  - ### Removals
    - Removed high-level utilities and unused response metadata handler. [#249](https://github.com/aws/aws-sdk-java-v2/pull/249)

## __Netty NIO Async HTTP Client__
  - ### Features
    - Adding socket resolver helper that will load the appropriate SocketChannel [#293](https://github.com/aws/aws-sdk-java-v2/pull/293)

  - ### Bugfixes
    - Netty spurious timeout error fix [#283](https://github.com/aws/aws-sdk-java-v2/pull/283)
    - Temporarily disable epoll [#254](https://github.com/aws/aws-sdk-java-v2/pull/254)

# __2.0.0-preview-5__ __2017-10-17__
## __AWS SDK for Java v2__
  - ### Features
    - Asynchronous request handler for strings `AsyncRequestProvider.fromString("hello world!!!")` [PR #183](https://github.com/aws/aws-sdk-java-v2/pull/183)
    - General HTTP core clean-up [PR #178](https://github.com/aws/aws-sdk-java-v2/pull/178)
    - Get value from request POJO using member model names `String bucketName = s3PutObjectResponse.getValueForField("Bucket", String.class);` [PR #144](https://github.com/aws/aws-sdk-java-v2/pull/144)
    - Model enums on service POJOs [PR #195](https://github.com/aws/aws-sdk-java-v2/pull/195)
    - Move `core` classes to their own package `software.amazon.awssdk.core` [PR #194](https://github.com/aws/aws-sdk-java-v2/pull/194)

  - ### Bugfixes
    - Resolve potential security issue handling DTD entities [PR #198](https://github.com/aws/aws-sdk-java-v2/pull/198)
    - Serialization/deserialization of complex model objects [PR #128](https://github.com/aws/aws-sdk-java-v2/pull/128) / [Issue #121](https://github.com/aws/aws-sdk-java-v2/issues/121)

## __Amazon S3__
  - ### Features
    - Handle 100-continue header for PUT object [PR #169](https://github.com/aws/aws-sdk-java-v2/pull/169)

## __Netty NIO Async HTTP Client__
  - ### Bugfixes
    - Better handling of event-loop selection for AWS Lambda container [PR #208](https://github.com/aws/aws-sdk-java-v2/pull/208)
    - Data corruption fix in streaming responses and stability fixes [PR #173](https://github.com/aws/aws-sdk-java-v2/pull/173)

# __2.0.0-preview-4__ __2017-09-19__
## __AWS SDK for Java v2__
  - ### Features
    - Added convenience methods for both sync and async streaming operations for file based uploads/downloads.
    - Added some convenience implementation of [AsyncResponseHandler](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/async/AsyncResponseHandler.java) to emit to a byte array or String.
    - Immutable objects can now be modified easily with a newly introduced [copy](https://github.com/aws/aws-sdk-java-v2/blob/master/utils/src/main/java/software/amazon/awssdk/utils/builder/ToCopyableBuilder.java#L42) method that applies a transformation on the builder for the object and returns a new immutable object.
    - Major refactor of RequestHandler interfaces. Newly introduced [ExecutionInterceptors](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/interceptor/ExecutionInterceptor.java) have a cleaner, more consistent API and are much more powerful.
    - S3's CreateBucket no longer requires the location constraint to be specified, it will be inferred from the client region if not present.
    - The [File](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/sync/StreamingResponseHandler.java#L92) and [OutputStream](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/sync/StreamingResponseHandler.java#L107) implementations of StreamingResponseHandler now return the POJO response in onComplete.

  - ### Bugfixes
    - Fixed a bug in default credential provider chain where it would erroneously abort at the ProfileCredentialsProvider. See [Issue #135](https://github.com/aws/aws-sdk-java-v2/issues/135)
    - Many improvements and fixes to the Netty NIO based transport.
    - Several fixes around S3's endpoint resolution, particularly with advanced options like path style addressing and accelerate mode. See [Issue #130](https://github.com/aws/aws-sdk-java-v2/issues/130)
    - Several fixes around serialization and deserialization of immutable objects. See [Issue #122](https://github.com/aws/aws-sdk-java-v2/issues/122)
    - Type parameters are now correctly included for [StreamingResponseHandler](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/sync/StreamingResponseHandler.java) on the client interface.

  - ### Removals
    - Dependency on JodaTime has been dropped in favor of Java 8's APIS.
    - DynamoDBMapper and DynamoDB Document API have been removed.
    - Metrics subsystem has been removed.

# __2.0.0-preview-2__ __2017-07-21__
## __AWS SDK for Java v2__
  - ### Features
    - New pluggable HTTP implementation built on top of Java's HttpUrlConnection. Good choice for simple applications with low throughput requirements. Better cold start latency than the default Apache implementation.
    - Simple convenience methods have been added for operations that require no input parameters.
    - Substantial improvements to start up time and cold start latencies
    - The Netty NIO HTTP client now uses a shared event loop group for better resource management. More options for customizing the event loop group are now available.
    - Using java.time instead of the legacy java.util.Date in generated model classes.
    - Various improvements to the immutability of model POJOs. ByteBuffers are now copied and collections are returned as unmodifiable.

# __2.0.0-preview-1__ __2017-06-28__
## __AWS SDK for Java v2__
  - ### Features
    - Initial release of the AWS SDK for Java v2. See our [blog post](https://aws.amazon.com/blogs/developer/aws-sdk-for-java-2-0-developer-preview) for information about this new major veresion. This release is considered a developer preview and is not intended for production use cases.


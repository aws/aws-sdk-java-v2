 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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


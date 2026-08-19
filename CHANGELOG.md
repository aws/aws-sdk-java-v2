 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.54.0__ __2026-08-19__
## __AWS Batch__
  - ### Features
    - AWS Batch now supports managing CloudWatch Container Insights on compute environments via CreateComputeEnvironment and UpdateComputeEnvironment.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports video cropping and output positioning. Use cropRectangle and outputPositionRectangle to position the encoded video within the output frame, with the surrounding area filled with black.

## __AWS SDK for Java v2__
  - ### Features
    - Add `SdkWarmUp` to warm up SDK request paths before a Coordinated Restore at Checkpoint (CRaC) checkpoint, including AWS Lambda SnapStart. `SdkWarmUp.warmUp()` warms every service client on the classpath. `SdkWarmUp.warmUp(Class...)` warms the service clients you name.
    - Cache auth scheme resolution results per operation
    - Enable compiled endpoint rules for all services by default, with a fix for region parameter handling in the generated endpoint providers.

  - ### Documentations
    - Add the @SdkAdvancedApi annotation to generated, operation event stream response handlers.

## __Account Access__
  - ### Features
    - Adds throttling exceptions to operation outputs that were previously inconsistent with other operations.

## __Amazon Bedrock AgentCore__
  - ### Features
    - AgentCore Memory now supports Flexible Namespaces and Non-Conversational Payloads in CreateEvent API

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - AgentCore Memory now supports Flexible Namespaces

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds support for EKS cluster certificate authorities (CA)

## __Amazon Redshift__
  - ### Features
    - Amazon Redshift enhanced System Table retention that allows customers to store their system table data directly in S3 Tables in customer's account instead of Redshift Managed Storage

## __Amazon VPC Lattice__
  - ### Features
    - Amazon VPC Lattice now supports modification of private DNS options on Service Network VPC Associations

## __Redshift Serverless__
  - ### Features
    - Amazon Redshift Enhanced System Table Retention that allows customers to store their system table data directly in S3 Tables in customer's account instead of Redshift Managed Storage


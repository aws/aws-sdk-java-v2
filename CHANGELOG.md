 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.50.1__ __2026-07-30__
## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Removing Smithy RPC v2 CBOR support that was added in previous SDK release.

## __AWS Billing and Cost Management Recommended Actions__
  - ### Features
    - Removing Smithy RPC v2 CBOR support that was added in previous SDK release.

## __AWS SDK for Java v2__
  - ### Features
    - Add the `@SdkAdvancedApi` annotation, which marks APIs that are error-prone to implement, override, call, or configure so that using them incorrectly compiles cleanly but can fail or misbehave at runtime. The annotation records structured guidance (the risky usage kind, an explanation of the contract to uphold, a safer alternative, and a documentation link) and is applied to several streaming and interceptor extension points, including AsyncRequestBody, AsyncResponseTransformer, ContentStreamProvider, the mutating ExecutionInterceptor content hooks, and the FUTURE_COMPLETION_EXECUTOR advanced client option.
    - Updated endpoint and partition metadata.

# __2.50.0__ __2026-07-30__
## __AWS Identity and Access Management__
  - ### Features
    - Improved IAM Policy Simulator accuracy. Simulator now evaluates SCP conditions and resource scoping, returns explicitDeny for explicit SCP denials, and reports accurate cross-account decisions.

## __AWS Lambda__
  - ### Features
    - Add Python3.15 (python3.15) and NodeJs 26 (nodejs26.x) support to AWS Lambda

## __AWS Network Firewall__
  - ### Features
    - Adds UPDATING field to Container Association Status

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Improve endpoint resolution performance by replacing URI.create with a lightweight EndpointUrl.

## __AWS Security Agent__
  - ### Features
    - Adds support for providing a branch override when configured integrated repositories

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds support for configuring models through the OpenResponses API for custom evaluators. CreateEvaluator and UpdateEvaluator now accept an OpenResponses model configuration for LLM-as-a-Judge evaluations.

## __Amazon S3__
  - ### Bugfixes
    - Honor an explicit `S3Configuration.expectContinueEnabled(false)` when cross region access is enabled.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for g7 family instance types for SageMaker Studio JupyterLab and CodeEditor apps for IAD (us-east-1), PDX (us-west-2), CMH (us-east-2).

## __Managed Streaming for Kafka__
  - ### Features
    - Amazon MSK Express brokers now support streaming tables for Apache Iceberg, continuously materializing Apache Kafka topics as Iceberg tables in Amazon S3 Tables. Express brokers also now support data delivery to Amazon S3 general purpose buckets.

## __PricingPlanManager__
  - ### Features
    - Adds support for Public PricingPlanManager SDK


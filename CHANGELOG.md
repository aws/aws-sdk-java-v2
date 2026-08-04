 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.51.0__ __2026-08-04__
## __AWS Identity and Access Management__
  - ### Features
    - Updating endpoint generation logic

## __AWS Organizations__
  - ### Features
    - Improved accuracy of CloudTrail event documentation for AWS Organizations membership operations.

## __AWS Single Sign-On Admin__
  - ### Features
    - AWS IAM Identity Center now lets you create organization-level instances without enabling multi-account permissions. You can enable multi-account permissions during instance creation or later via console or API, which then provisions the necessary service-linked roles.

## __Amazon Aurora DSQL__
  - ### Features
    - UpdateCluster now checks the RemovePeerCluster permission on the specific cluster being removed, not a wildcard and docs now clarify how to set kmsEncryptionKey so the cluster uses the AWS-owned key.

## __Amazon Connect Service__
  - ### Features
    - Amazon Connect Customer now supports up to 50 attachments per email, increased from the previous limit of 10. The individual maximum attachment size limit of 20 MB and the total email size limit of 25 MB still hold true.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Adds SearchVectors and vector index support to the DynamoDB low-level client (sync and async) and Enhanced Client, including vector index table operations, table.vectorIndex() search handles, and enhanced search request/response types.
    - Adds annotation-driven vector index support to the DynamoDB Enhanced Client via @DynamoDbVectorAttribute, @DynamoDbSearchVectorsHashKey, and @DynamoDbSearchVectorsInlineFilterKey on bean and immutable schemas, enabling no-arg createTable() and vector search through annotation-derived table.vectorIndex() handles.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 now supports Application Status Checks, a new status check that monitors your application's health through configurable HTTP(S) paths and ports, so you can detect and automatically respond to application-level impairments.

## __Amazon WorkSpaces__
  - ### Features
    - Added ClientExperiencePolicy to ClientProperties object for ModifyClientProperties and DescribeClientProperties APIs.

## __Inspector2__
  - ### Features
    - Adding Azure SBOM export capability.

## __Partner Central Selling API__
  - ### Features
    - Partners can now create leads with only 5 required fields and free-text values for all other fields, reducing import friction. Engagement invitations now include enrichment data (propensity scores, lead readiness) directly in the response.


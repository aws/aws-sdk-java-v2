 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.41.0__ __2025-12-30__
## __AWS SDK for Java V2__
  - ### Bugfixes
    - Ensure rpc 1.0/1.1 error code parsing matches smithy spec: use both __type and code fields and handle uris in body error codes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Don't use the value of AwsQueryError in json rpc/smithy-rpc-v2-cbor protocols.

## __Amazon Connect Service__
  - ### Features
    - Adds support for searching global contacts using the ActiveRegions filter, and pagination support for ListSecurityProfileFlowModules and ListEntitySecurityProfiles.

## __Apache5 HTTP Client__
  - ### Features
    - The Apache5 HTTP Client (`apache5-client`) is out of preview and now generally available.

## __Lambda Maven Archetype__
  - ### Features
    - Various Java Lambda Maven archetype improvements: use Java 25, use platform specific AWS CRT dependency, bump dependency version, and improve README. See [#6115](https://github.com/aws/aws-sdk-java-v2/issues/6115)

## __Managed Streaming for Kafka Connect__
  - ### Features
    - This change sets the KafkaConnect GovCloud FIPS and FIPS DualStack endpoints to use kafkaconnect instead of kafkaconnect-fips as the service name. This is done to match the Kafka endpoints.


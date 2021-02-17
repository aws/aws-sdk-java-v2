# Maven Archetype for client applications using the AWS SDK for Java 2.x

## Description
This is an Apache Maven Archetype to create a client application with
a dependency of [AWS Java SDK 2.x][aws-java-sdk-v2].

### Features

The generated application has the following features:

- Uses [Bill of Materials](BOM) to manage SDK dependencies
- Contains the code to create the SDK client
- Out-of-box support of GraalVM Native Image when `nativeImage` is enabled

## Usage

You can use `mvn archetype:generate` to generate a project using this archetype. See [maven archetype usage guidance][maven-archetype-usage] for more information.

- Interactive mode

```
mvn archetype:generate \
  -DarchetypeGroupId=software.amazon.awssdk \
  -DarchetypeArtifactId=archetype-app-quickstart \
  -DarchetypeVersion=2.x
```

- Batch mode

```
mvn archetype:generate \
    -DarchetypeGroupId=software.amazon.awssdk \
    -DarchetypeArtifactId=archetype-app-quickstart \
    -DarchetypeVersion=2.x \
    -DgroupId=com.test \
    -DnativeImage=true \
    -DhttpClient=apache-client \
    -DartifactId=sample-project \
    -Dservice=s3  \
    -DinteractiveMode=false
```

### Parameters
      
Parameter Name | Default Value | Description
---|---|---
`service` (required) | n/a | Specifies the service client to be used in the application, eg: s3, dynamodb. Only one service should be provided. You can find available services [here][java-sdk-v2-services]. 
`groupId`(required) | n/a | Specifies the group ID of the project
`artifactId`(required) | n/a | Specifies the artifact ID of the project
`nativeImage`(required)  | n/a | Specifies whether GraalVM Native Image configuration should be included
`httpClient`(required) | n/a | Specifies the http client to be used by the SDK client. Available options are `url-connection-client` (sync), `apache-client` (sync), `netty-nio-client` (async). See [http clients][sdk-http-clients]
`javaSdkVersion` | Same version as the archetype version | Specifies the version of the AWS Java SDK 2.x to be used
`version` | 1.0-SNAPSHOT | Specifies the version of the project
`package` | ${groupId} | Specifies the package name for the classes


[aws-java-sdk-v2]: https://github.com/aws/aws-sdk-java-v2
[java-sdk-v2-services]: https://github.com/aws/aws-sdk-java-v2/tree/master/services
[sdk-http-clients]: https://github.com/aws/aws-sdk-java-v2/tree/master/http-clients
[maven-archetype-usage]: https://maven.apache.org/archetype/maven-archetype-plugin/usage.html
[graalvm]: https://www.graalvm.org/docs/getting-started/#native-images

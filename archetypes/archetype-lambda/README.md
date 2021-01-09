# Maven Archetype for lambda function using AWS SDK for Java 2.x

## Description
This is an Apache Maven Archetype to create a lambda function template using [AWS Java SDK 2.x][aws-java-sdk-v2]. The generated template
has the optimized configurations and follows the best practices to reduce start up time.

## Usage

You can use `mvn archetype:generate` to generate a project using this archetype. See [maven archetype usage guidance][maven-archetype-usage] for more information.

- Interactive mode

```
mvn archetype:generate \
  -DarchetypeGroupId=software.amazon.awssdk \
  -DarchetypeArtifactId=archetype-lambda \
  -DarchetypeVersion=2.x
```

- Batch mode

```
mvn archetype:generate \
    -DarchetypeGroupId=software.amazon.awssdk \
    -DarchetypeArtifactId=archetype-lambda \
    -DarchetypeVersion=2.x \
    -DgroupId=com.test \
    -DartifactId=sample-project \
    -Dservice=s3  \
    -DinteractiveMode=false
```

### Parameters
      
Parameter Name | Default Value | Description
---|---|---
`service` (required) | n/a | Specifies the service client to be used in the lambda function, eg: s3, dynamodb. You can find available services [here][java-sdk-v2-services].
`groupId`(required) | n/a | Specifies the group ID of the project
`artifactId`(required) | n/a | Specifies the artifact ID of the project
`region` | n/a | Specifies the region to be set for the SDK client in the application
`httpClient` | url-connection-client | Specifies the http client to be used by the SDK client. Available options are `url-connection-client` (sync), `apache-client` (sync), `netty-nio-client` (async). See [http clients][sdk-http-clients]
`handlerClassName` | `"App"`| Specifies the class name of the handler, which will be used as the lambda function name. It should be camel case.
`javaSdkVersion` | Same version as the archetype version | Specifies the version of the AWS Java SDK 2.x to be used
`version` | 1.0-SNAPSHOT | Specifies the version of the project
`package` | ${groupId} | Specifies the package name for the classes

### Deployment

To deploy the lambda function, you can use [SAM CLI][sam-cli]. The generated project contains a default [SAM template][sam-template] file `template.yaml` where you can 
configure different properties of your lambda function such as memory size and timeout.

```
sam deploy --guided
```

Please refer to [deploying lambda apps][deploying-lambda-apps] for more info.

[aws-java-sdk-v2]: https://github.com/aws/aws-sdk-java-v2
[java-sdk-v2-services]: https://github.com/aws/aws-sdk-java-v2/tree/master/services
[sdk-http-clients]: https://github.com/aws/aws-sdk-java-v2/tree/master/http-clients
[deploying-lambda-apps]: https://docs.aws.amazon.com/lambda/latest/dg/deploying-lambda-apps.html
[sam-cli]:https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-getting-started.html
[maven-archetype-usage]: https://maven.apache.org/archetype/maven-archetype-plugin/usage.html
[sam-template]: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-function.html

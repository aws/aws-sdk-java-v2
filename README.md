# AWS SDK for Java 2.0 Developer Preview [![Build Status](https://travis-ci.org/aws/aws-sdk-java.png?branch=master)](https://travis-ci.org/aws/aws-sdk-java)

The **AWS SDK for Java 2.0 Developer Preview** is a rewrite of 1.0 with some great new features. As with version 1.0, 
it enables you to easily work with [Amazon Web Services][aws] but also includes features like non-blocking IO and pluggable
HTTP implementation to further customize your applications. You can get started in minutes using ***Maven*** or any build system that supports MavenCentral as an artifact source.

**NOTE** 2.0 version still a preview and is not recommended for production use yet. 

* [SDK Homepage][sdk-website]
* [API Docs][docs-api]
* [Developer Guide][docs-guide] ([source][docs-guide-source])
* [Issues][sdk-issues]
* [SDK Blog][blog]
* [Giving Feedback](#giving-feedback)

## Getting Started

#### Sign up for AWS ####

Before you begin, you need an AWS account. Please see the [Sign Up for AWS][docs-signup] section of
the developer guide for information about how to create an AWS account and retrieve your AWS
credentials.

#### Minimum requirements ####

To run the SDK you will need **Java 1.8+**. For more information about the requirements and optimum
settings for the SDK, please see the [Installing a Java Development Environment][docs-java-env]
section of the developer guide.

#### Install the SDK ####

The recommended way to use the AWS SDK for Java in your project is to consume it from Maven. Import
the [bom][] and specify the SDK Maven modules that your project needs in the
dependencies.

##### Importing the BOM #####

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>bom</artifactId>
      <version>2.0.0-preview-1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

##### Using the SDK Maven modules #####

```xml
<dependencies>
  <dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>ec2</artifactId>
  </dependency>
  <dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
  </dependency>
  <dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb</artifactId>
  </dependency>
</dependencies>
```

See the [Set up the AWS SDK for Java][docs-setup] section of the developer guide for more
information about installing the SDK through other means.

## New Features for Developer Preview

* Provides a way to plug in your own HTTP implementation.

* Provides first class support for non-blocking IO in Async clients.

## Building From Source

Once you check out the code from GitHub, you can build it using Maven. To disable the GPG-signing
in the build, use:

```sh
mvn clean install -Dgpg.skip=true
```

## Giving Feedback 
We need your help in making this SDK great. Please participate in the community and contribute to this effort by submitting issues, participating in discussion forums and submitting pull requests through the following channels. 

* Come join the AWS Java community chat on [Gitter][gitter].
* Articulate your feature request or upvote existing ones on our [Issues][features] page.
* Submit [issues][sdk-issues].
* Send feedback directly to the team at aws-java-sdk-v2-feedback@amazon.com. 

[aws-iam-credentials]: http://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/java-dg-roles.html
[aws]: http://aws.amazon.com/
[blog]: https://aws.amazon.com/blogs/developer/category/java/
[docs-api]: http://aws-java-sdk-javadoc.s3-website-us-west-2.amazonaws.com/latest/overview-summary.html
[docs-guide]: http://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html
[docs-guide-source]: https://github.com/awsdocs/aws-java-developer-guide-v2
[docs-java-env]: http://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/setup-install.html##java-dg-java-env
[docs-signup]: http://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/signup-create-iam-user.html
[docs-setup]: http://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/setup-install.html
[sdk-issues]: https://github.com/aws/aws-sdk-java-v2/issues
[sdk-license]: http://aws.amazon.com/apache2.0/
[sdk-website]: http://aws.amazon.com/sdkforjava
[aws-java-sdk-bom]: https://github.com/aws/aws-sdk-java-v2/tree/master/bom
[stack-overflow]: http://stackoverflow.com/questions/tagged/aws-java-sdk
[gitter]: https://gitter.im/aws/aws-sdk-java-v2
[features]: https://github.com/aws/aws-sdk-java-v2/issues?q=is%3Aopen+is%3Aissue+label%3A%22Feature+Request%22
[support-center]: https://console.aws.amazon.com/support/
[console]: https://console.aws.amazon.com

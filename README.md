# AWS SDK for Java 2.0
[![Build Status](https://travis-ci.org/aws/aws-sdk-java-v2.svg?branch=master)](https://travis-ci.org/aws/aws-sdk-java-v2) ![Build Status](https://codebuild.us-west-2.amazonaws.com/badges?uuid=eyJlbmNyeXB0ZWREYXRhIjoiTFJSRXBBN1hkU1ZEQzZ4M1hoaWlFUExuNER3WjNpVllSQ09Qam1YdFlTSDNTd3RpZzNia3F0VkJRUTBwZlQwR1BEelpSV2dWVnp4YTBCOFZKRzRUR004PSIsIml2UGFyYW1ldGVyU3BlYyI6ImdHdEp1UHhKckpDRmhmQU4iLCJtYXRlcmlhbFNldFNlcmlhbCI6MX0%3D&branch=master)
[![Maven](https://img.shields.io/maven-central/v/software.amazon.awssdk/s3.svg?label=Maven)](https://search.maven.org/search?q=g:%22software.amazon.awssdk%22%20AND%20a:%22s3%22)
[![Gitter](https://badges.gitter.im/aws/aws-sdk-java-v2.svg)](https://gitter.im/aws/aws-sdk-java-v2?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) 

The **AWS SDK for Java 2.0** is a rewrite of 1.0 with some great new features. As with version 1.0,
it enables you to easily work with [Amazon Web Services][aws] but also includes features like
non-blocking IO and pluggable HTTP implementation to further customize your applications. You can
get started in minutes using ***Maven*** or any build system that supports MavenCentral as an
artifact source.

* [SDK Homepage][sdk-website]
* [1.11 to 2.0 Changelog](docs/LaunchChangelog.md)
* [Sample Code](#sample-code)
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

## Using the SDK

The recommended way to use the AWS SDK for Java in your project is to consume it from Maven. 

#### Whole SDK ####

You can import the whole SDK into your project (includes all services) as follows:

```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>aws-sdk-java</artifactId>
  <version>2.5.11</version>
</dependency>
```

#### Individual Services ####

Alternatively you can add dependencies for the specific services you use only:

```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>ec2</artifactId>
  <version>2.5.11</version>
</dependency>
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3</artifactId>
  <version>2.5.11</version>
</dependency>
```

#### Importing the BOM ####

To automatically manage module versions (currently all modules have the same version, but this may not always be the case) we recommend you use the [Bill of Materials][bom] import as follows:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>bom</artifactId>
      <version>2.5.11</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Then individual models may omit the `version` from their dependency statement:

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

See the [Set up the AWS SDK for Java][docs-setup] section of the developer guide for more usage information.

## New Features for 2.0

* Provides a way to plug in your own HTTP implementation.

* Provides first class support for non-blocking IO in Async clients.

## Building From Source

Once you check out the code from GitHub, you can build it using Maven.

```sh
mvn clean install
```

## Sample Code
You can find sample code for v2 in the following places:

* [aws-doc-sdk-examples] repo.
* Integration tests in this repo. They are located in the `it` directory under each service module, eg: [s3-integration-tests]

## Giving Feedback
We need your help in making this SDK great. Please participate in the community and contribute to this effort by submitting issues, participating in discussion forums and submitting pull requests through the following channels.

* Come join the AWS Java community chat on [Gitter][gitter].
* Articulate your feature request or upvote existing ones on our [Issues][features] page.
* Submit [issues][sdk-issues].
* Send feedback directly to the team at aws-java-sdk-v2-feedback@amazon.com.

[aws-iam-credentials]: http://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/java-dg-roles.html
[aws]: http://aws.amazon.com/
[blog]: https://aws.amazon.com/blogs/developer/category/java/
[docs-api]: https://sdk.amazonaws.com/java/api/latest/overview-summary.html
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
[bom]: http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22software.amazon.awssdk%22%20AND%20a%3A%22bom%22
[aws-doc-sdk-examples]: https://github.com/awsdocs/aws-doc-sdk-examples/tree/master/javav2
[s3-integration-tests]: https://github.com/aws/aws-sdk-java-v2/tree/master/services/s3/src/it/java/software/amazon/awssdk/services/s3
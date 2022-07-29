# AWS SDK for Java 2.0
![Build Status](https://codebuild.us-west-2.amazonaws.com/badges?uuid=eyJlbmNyeXB0ZWREYXRhIjoiTFJSRXBBN1hkU1ZEQzZ4M1hoaWlFUExuNER3WjNpVllSQ09Qam1YdFlTSDNTd3RpZzNia3F0VkJRUTBwZlQwR1BEelpSV2dWVnp4YTBCOFZKRzRUR004PSIsIml2UGFyYW1ldGVyU3BlYyI6ImdHdEp1UHhKckpDRmhmQU4iLCJtYXRlcmlhbFNldFNlcmlhbCI6MX0%3D&branch=master)
[![Maven](https://img.shields.io/maven-central/v/software.amazon.awssdk/s3.svg?label=Maven)](https://search.maven.org/search?q=g:%22software.amazon.awssdk%22%20AND%20a:%22s3%22)
[![Gitter](https://badges.gitter.im/aws/aws-sdk-java-v2.svg)](https://gitter.im/aws/aws-sdk-java-v2?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) 
[![codecov](https://codecov.io/gh/aws/aws-sdk-java-v2/branch/master/graph/badge.svg)](https://codecov.io/gh/aws/aws-sdk-java-v2)
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-81-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

The **AWS SDK for Java 2.0** is a rewrite of 1.0 with some great new features. As with version 1.0,
it enables you to easily work with [Amazon Web Services][aws] but also includes features like
non-blocking IO and pluggable HTTP implementation to further customize your applications. You can
get started in minutes using ***Maven*** or any build system that supports MavenCentral as an
artifact source.

* [SDK Homepage][sdk-website]
* [1.11 to 2.0 Changelog](docs/LaunchChangelog.md)
* [Best Practices](docs/BestPractices.md)
* [Sample Code](#sample-code)
* [API Docs][docs-api]
* [Developer Guide][docs-guide] ([source][docs-guide-source])
* [Maven Archetypes](archetypes/README.md)
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

The recommended way to use the AWS SDK for Java in your project is to consume it from Maven Central. 

#### Importing the BOM ####

To automatically manage module versions (currently all modules have the same version, but this may not always be the case) we recommend you use the [Bill of Materials][bom] import as follows:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>bom</artifactId>
      <version>2.17.243</version>
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
#### Individual Services ####

Alternatively you can add dependencies for the specific services you use only:

```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>ec2</artifactId>
  <version>2.17.243</version>
</dependency>
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3</artifactId>
  <version>2.17.243</version>
</dependency>
```

#### Whole SDK ####

You can import the whole SDK into your project (includes *ALL* services). Please note that it is recommended to only import the modules you need.

```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>aws-sdk-java</artifactId>
  <version>2.17.243</version>
</dependency>
```

See the [Set up the AWS SDK for Java][docs-setup] section of the developer guide for more usage information.

## New Features for 2.0

* Provides a way to plug in your own HTTP implementation.

* Provides first class support for non-blocking IO in Async clients.

## Building From Source

Once you check out the code from GitHub, you can build it using the following commands.

Linux:

```sh
./mvnw clean install

# Skip tests, checkstyles, findbugs, etc for quick build
./mvnw clean install -P quick

# Build a specific service module
./mvnw clean install -pl :s3 -P quick --am
```

Windows:
```sh
./mvnw.cmd clean install
```

## Sample Code
You can find sample code for v2 in the following places:

* [aws-doc-sdk-examples] repo.
* Integration tests in this repo. They are located in the `it` directory under each service module, eg: [s3-integration-tests]

## Maintenance and Support for SDK Major Versions
For information about maintenance and support for SDK major versions and their underlying dependencies, see the following in the AWS SDKs and Tools Shared Configuration and Credentials Reference Guide:

* [AWS SDKs and Tools Maintenance Policy][maintenance-policy]
* [AWS SDKs and Tools Version Support Matrix][version-matrix]

## Giving Feedback
We need your help in making this SDK great. Please participate in the community and contribute to this effort by submitting issues, participating in discussion forums and submitting pull requests through the following channels:

* Submit [issues][sdk-issues] - this is the preferred channel to interact with our team
* Come join the AWS Java community chat on [Gitter][gitter]
* Articulate your feature request or upvote existing ones on our [Issues][features] page
* Send feedback directly to the team at aws-java-sdk-v2-feedback@amazon.com

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
[features]: https://github.com/aws/aws-sdk-java-v2/issues?q=is%3Aopen+is%3Aissue+label%3A%22feature-request%22
[support-center]: https://console.aws.amazon.com/support/
[console]: https://console.aws.amazon.com
[bom]: http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22software.amazon.awssdk%22%20AND%20a%3A%22bom%22
[aws-doc-sdk-examples]: https://github.com/awsdocs/aws-doc-sdk-examples/tree/master/javav2
[s3-integration-tests]: https://github.com/aws/aws-sdk-java-v2/tree/master/services/s3/src/it/java/software/amazon/awssdk/services/s3
[maintenance-policy]: https://docs.aws.amazon.com/credref/latest/refdocs/maint-policy.html
[version-matrix]: https://docs.aws.amazon.com/credref/latest/refdocs/version-support-matrix.html

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://github.com/sullis"><img src="https://avatars.githubusercontent.com/u/30938?v=4?s=100" width="100px;" alt=""/><br /><sub><b>sullis</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=sullis" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/abrooksv"><img src="https://avatars.githubusercontent.com/u/8992246?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Austin Brooks</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=abrooksv" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/ktoso"><img src="https://avatars.githubusercontent.com/u/120979?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Konrad `ktoso` Malawski</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=ktoso" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/andrewhop"><img src="https://avatars.githubusercontent.com/u/41167468?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Andrew Hopkins</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=andrewhop" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/adamthom-amzn"><img src="https://avatars.githubusercontent.com/u/61852529?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Adam Thomas</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=adamthom-amzn" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/sworisbreathing"><img src="https://avatars.githubusercontent.com/u/1486524?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Steven Swor</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=sworisbreathing" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/Carey-AWS"><img src="https://avatars.githubusercontent.com/u/61763083?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Carey Burgess</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=Carey-AWS" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/anuraaga"><img src="https://avatars.githubusercontent.com/u/198344?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Anuraag Agrawal</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=anuraaga" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/jeffalder"><img src="https://avatars.githubusercontent.com/u/49817386?v=4?s=100" width="100px;" alt=""/><br /><sub><b>jeffalder</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=jeffalder" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/dotbg"><img src="https://avatars.githubusercontent.com/u/367403?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Boris</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=dotbg" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/notdryft"><img src="https://avatars.githubusercontent.com/u/2608594?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Guillaume Corré</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=notdryft" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/hyandell"><img src="https://avatars.githubusercontent.com/u/477715?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Henri Yandell</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=hyandell" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/rschmitt"><img src="https://avatars.githubusercontent.com/u/3725049?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Ryan Schmitt</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=rschmitt" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/SomayaB"><img src="https://avatars.githubusercontent.com/u/23043132?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Somaya</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=SomayaB" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/steven-aerts"><img src="https://avatars.githubusercontent.com/u/1381633?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Steven Aerts</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=steven-aerts" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/skwslide"><img src="https://avatars.githubusercontent.com/u/1427510?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Steven Wong</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=skwslide" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/telendt"><img src="https://avatars.githubusercontent.com/u/85191?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Tomasz Elendt</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=telendt" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/Sarev0k"><img src="https://avatars.githubusercontent.com/u/8388574?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Will Erickson</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=Sarev0k" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/madgnome"><img src="https://avatars.githubusercontent.com/u/279528?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Julien Hoarau</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=madgnome" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/SEOKHYOENCHOI"><img src="https://avatars.githubusercontent.com/u/42906668?v=4?s=100" width="100px;" alt=""/><br /><sub><b>SEOKHYOENCHOI</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=SEOKHYOENCHOI" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/adriannistor"><img src="https://avatars.githubusercontent.com/u/3051958?v=4?s=100" width="100px;" alt=""/><br /><sub><b>adriannistor</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=adriannistor" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/alicesun16"><img src="https://avatars.githubusercontent.com/u/56938110?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Xian Sun </b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=alicesun16" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/ascheja"><img src="https://avatars.githubusercontent.com/u/3932118?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Andreas Scheja</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=ascheja" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/antegocanva"><img src="https://avatars.githubusercontent.com/u/43571020?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Anton Egorov</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=antegocanva" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/roexber"><img src="https://avatars.githubusercontent.com/u/7964627?v=4?s=100" width="100px;" alt=""/><br /><sub><b>roexber</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=roexber" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/brharrington"><img src="https://avatars.githubusercontent.com/u/1289028?v=4?s=100" width="100px;" alt=""/><br /><sub><b>brharrington</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=brharrington" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/chrisradek"><img src="https://avatars.githubusercontent.com/u/14189820?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Christopher Radek</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=chrisradek" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/zakkak"><img src="https://avatars.githubusercontent.com/u/1435395?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Foivos</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=zakkak" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/superwese"><img src="https://avatars.githubusercontent.com/u/954116?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Frank Wesemann</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=superwese" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/sperka"><img src="https://avatars.githubusercontent.com/u/157324?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Gergely Varga</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=sperka" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/GuillermoBlasco"><img src="https://avatars.githubusercontent.com/u/1889971?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Guillermo</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=GuillermoBlasco" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/rce"><img src="https://avatars.githubusercontent.com/u/4427896?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Henry Heikkinen</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=rce" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/joschi"><img src="https://avatars.githubusercontent.com/u/43951?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Jochen Schalanda</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=joschi" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/josephlbarnett"><img src="https://avatars.githubusercontent.com/u/13838924?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Joe Barnett</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=josephlbarnett" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/seratch"><img src="https://avatars.githubusercontent.com/u/19658?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Kazuhiro Sera</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=seratch" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/ChaithanyaGK"><img src="https://avatars.githubusercontent.com/u/28896513?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Krishna Chaithanya Ganta</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=ChaithanyaGK" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/leepa"><img src="https://avatars.githubusercontent.com/u/9469?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Lee Packham</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=leepa" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/MatteCarra"><img src="https://avatars.githubusercontent.com/u/11074527?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Matteo Carrara</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=MatteCarra" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/mscharp"><img src="https://avatars.githubusercontent.com/u/1426929?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Michael Scharp</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=mscharp" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/miguelrjim"><img src="https://avatars.githubusercontent.com/u/1420241?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Miguel Jimenez</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=miguelrjim" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/Helmsdown"><img src="https://avatars.githubusercontent.com/u/1689115?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Russell Bolles</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=Helmsdown" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/scheerer"><img src="https://avatars.githubusercontent.com/u/4659?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Russell Scheerer</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=scheerer" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/scotty-g"><img src="https://avatars.githubusercontent.com/u/7861050?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Scott</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=scotty-g" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/ueokande"><img src="https://avatars.githubusercontent.com/u/534166?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Shin'ya Ueoka</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=ueokande" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/sushilamazon"><img src="https://avatars.githubusercontent.com/u/42008398?v=4?s=100" width="100px;" alt=""/><br /><sub><b>sushilamazon</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=sushilamazon" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/tomliu4uber"><img src="https://avatars.githubusercontent.com/u/22459891?v=4?s=100" width="100px;" alt=""/><br /><sub><b>tomliu4uber</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=tomliu4uber" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/musketyr"><img src="https://avatars.githubusercontent.com/u/660405?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Vladimir Orany</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=musketyr" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/Xinyu-Hu"><img src="https://avatars.githubusercontent.com/u/31017838?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Xinyu Hu</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=Xinyu-Hu" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/frosforever"><img src="https://avatars.githubusercontent.com/u/1630422?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Yosef Fertel</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=frosforever" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/denyskonakhevych"><img src="https://avatars.githubusercontent.com/u/5894907?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Denys Konakhevych</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=denyskonakhevych" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/alexw91"><img src="https://avatars.githubusercontent.com/u/3596374?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Alex Weibel</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=alexw91" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/rccarper"><img src="https://avatars.githubusercontent.com/u/51676630?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Ryan Carper</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=rccarper" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/JonathanHenson"><img src="https://avatars.githubusercontent.com/u/3926469?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Jonathan M. Henson</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=JonathanHenson" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/debora-ito"><img src="https://avatars.githubusercontent.com/u/476307?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Debora N. Ito</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=debora-ito" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/bretambrose"><img src="https://avatars.githubusercontent.com/u/341314?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Bret Ambrose</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=bretambrose" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/cenedhryn"><img src="https://avatars.githubusercontent.com/u/26603446?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Anna-Karin Salander</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=cenedhryn" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/joviegas"><img src="https://avatars.githubusercontent.com/u/70235430?v=4?s=100" width="100px;" alt=""/><br /><sub><b>John Viegas</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=joviegas" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/dagnir"><img src="https://avatars.githubusercontent.com/u/261310?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Dongie Agnir</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=dagnir" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/millems"><img src="https://avatars.githubusercontent.com/u/24903526?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Matthew Miller</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=millems" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/bmaizels"><img src="https://avatars.githubusercontent.com/u/36682168?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Benjamin Maizels</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=bmaizels" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/Quanzzzz"><img src="https://avatars.githubusercontent.com/u/51490885?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Quan Zhou</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=Quanzzzz" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/zoewangg"><img src="https://avatars.githubusercontent.com/u/33073555?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Zoe Wang</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=zoewangg" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/varunnvs92"><img src="https://avatars.githubusercontent.com/u/17261531?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Varun Nandi</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=varunnvs92" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/shorea"><img src="https://avatars.githubusercontent.com/u/11096681?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Andrew Shore</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=shorea" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/kiiadi"><img src="https://avatars.githubusercontent.com/u/4661536?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Kyle Thomson</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=kiiadi" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/spfink"><img src="https://avatars.githubusercontent.com/u/20525381?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Sam Fink</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=spfink" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/bondj"><img src="https://avatars.githubusercontent.com/u/4749778?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Jonathan Bond</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=bondj" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/ajs139"><img src="https://avatars.githubusercontent.com/u/9387176?v=4?s=100" width="100px;" alt=""/><br /><sub><b>ajs139</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=ajs139" title="Code">💻</a></td>
    <td align="center"><a href="http://imdewey.com"><img src="https://avatars.githubusercontent.com/u/44629464?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Dewey Nguyen</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=duy3101" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/dleen"><img src="https://avatars.githubusercontent.com/u/1297964?v=4?s=100" width="100px;" alt=""/><br /><sub><b>David Leen</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=dleen" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="http://16lim21.github.io"><img src="https://avatars.githubusercontent.com/u/53011962?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Michael Li</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=16lim21" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/Bennett-Lynch"><img src="https://avatars.githubusercontent.com/u/11811448?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Bennett Lynch</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=Bennett-Lynch" title="Code">💻</a></td>
    <td align="center"><a href="https://bandism.net/"><img src="https://avatars.githubusercontent.com/u/22633385?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Ikko Ashimine</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=eltociear" title="Documentation">📖</a></td>
    <td align="center"><a href="https://jamieliu.me"><img src="https://avatars.githubusercontent.com/u/35614552?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Jamie Liu</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=jamieliu386" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/guillepb10"><img src="https://avatars.githubusercontent.com/u/28654665?v=4?s=100" width="100px;" alt=""/><br /><sub><b>guillepb10</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=guillepb10" title="Code">💻</a></td>
    <td align="center"><a href="https://www.linkedin.com/in/lorenznickel/"><img src="https://avatars.githubusercontent.com/u/29959150?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Lorenz Nickel</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=LorenzNickel" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/erin889"><img src="https://avatars.githubusercontent.com/u/38885911?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Erin Yang</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=erin889" title="Code">💻</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://www.theguardian.com/profile/roberto-tyley"><img src="https://avatars.githubusercontent.com/u/52038?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Roberto Tyley</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=rtyley" title="Code">💻</a></td>
    <td align="center"><a href="https://alvinsee.com/"><img src="https://avatars.githubusercontent.com/u/1531158?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Alvin See</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=alvinsee" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/ron1"><img src="https://avatars.githubusercontent.com/u/1318509?v=4?s=100" width="100px;" alt=""/><br /><sub><b>ron1</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=ron1" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/srsaikumarreddy"><img src="https://avatars.githubusercontent.com/u/24988810?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Sai Kumar Reddy Chandupatla</b></sub></a><br /><a href="https://github.com/aws/aws-sdk-java-v2/commits?author=srsaikumarreddy" title="Code">💻</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!

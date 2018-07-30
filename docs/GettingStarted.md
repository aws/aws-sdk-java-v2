# Working on the SDK

# Things to Know
* The SDK is built on Java 8
* [Maven][maven] is used as the build and dependency management system
* The majority of the service client code is auto-generated using the [code
  generator][codegen]

# Development Environment Setup Tips
If you use IntelliJ IDEA, we include some helpful config files that will make your development experience smoother:
- [intellij-codestyle.xml](https://github.com/aws/aws-sdk-java-v2/blob/master/build-tools/src/main/resources/software/amazon/awssdk/intellij-codestyle.xml)

  This will help ensure your code follows our code style guidelines.

- [intellij-copyright-profile.xml](https://github.com/aws/aws-sdk-java-v2/blob/master/build-tools/src/main/resources/software/amazon/awssdk/intellij-copyright-profile.xml)

  This automatically inserts the license header to the top of source files that you create.

If you have Checkstyle integrated with your IDE, we also recommend configuring it with our [Checkstyle config](https://github.com/aws/aws-sdk-java-v2/blob/master/build-tools/src/main/resources/software/amazon/awssdk/checkstyle.xml) so you can see any violations in line with the code.

# Getting Around
At a high level, the SDK is comprised of two parts: the core runtime, and the
service clients, including their higher level abstractions.

*TODO*

# Building
Since the SDK is a normal Maven project, the usual `mvn package` and `mvn
install` commands are all you need to build the SDK.

One important thing to note is that if you're working on the [code
generator][codegen], be sure to do a `mvn install` rather than a phase that
comes earlier such as `compile` or `test` so that the build uses the uses the
correct code generator JAR (i.e. the one including your changes). When in
doubt, just use `mvn package`.

## Disabling Checkstyle/FindBugs
Normally Checkstyle and FindBugs scans run as part of the build process.
However, this slows down the build significantly so if you need to be able to
iterate quickly locally, you can turn either of them off by setting the
appropriate properties:

```sh
# skips both Checkstyle and FindBugs
$ mvn install -Dfindbugs.skip=true -Dcheckstyle.skip=true
```

# Testing
## Unit Tests
As described in the project structure, tests are split between unit and
integration tests. During the normal `test` lifecycle phase, only the unit
tests are run.

```sh
# runs the unit tests
mvn install
```

## Integration Tests
__Before running the integration tests, be aware that they require active AWS
IAM credentials, and because they will make actual calls to AWS, will incur a
cost to the owner of the account.__

If you're writing an integration test, try to see if it's possible to write it
as a set of unit tests with mocked responses instead.

### Test Credentials

As mentioned above, you will need to have active IAM credentials that the tests
will use to authenticate with AWS, and it will need to have an attached IAM
policy that is allowed to perform the actions the tests will be running.

All integration tests are written to locate these credentials in
`$HOME/.aws/awsTestAccount.properties`:

```
$ cat $HOME/.aws/awsTestAccount.properties

accessKey = ...
secretKey = ...
```

### Running the Integration Tests

In order to run the integration tests along with the unit tests, you must
activate the `integration-tests` profile

```sh
# runs both unit and integration tests
mvn install -P integration-tests
```

[maven]: https://maven.apache.org/
[codegen]: https://github.com/aws/aws-sdk-java-v2/blob/master/codegen

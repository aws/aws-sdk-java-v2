# App

This project contains a maven application with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2) dependencies.

## Prerequisites
- Java 1.8+
- Apache Maven
- GraalVM Native Image (optional)

## Development

Below is the structure of the generated project.

```
├── src
│   ├── main
│   │   ├── java
│   │   │   └── package
│   │   │       ├── App.java
│   │   │       ├── DependencyFactory.java
│   │   │       └── Handler.java
│   │   └── resources
│   │       └── simplelogger.properties
│   └── test
│       └── java
│           └── package
│               └── HandlerTest.java
```

- `App.java`: main entry of the application
- `DependencyFactory.java`: creates the SDK client
- `Handler.java`: you can invoke the api calls using the SDK client here.

#### Building the project
```
mvn clean package
```

#### Building the native image

Note that it requires `native-image` installed in your environment

```
mvn clean package -P native-image
```
After it finishes, you can find the generated native image in the `target
` folder. 

You can run the following command to execute it.

```
target/test-apache-artifact
```

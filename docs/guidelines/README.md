# AWS SDK for Java v2 Development Guidelines

This directory contains comprehensive guidelines for developing with the AWS SDK for Java v2. These guidelines are organized into separate, focused documents to make them more manageable and easier to reference.

## Available Guidelines

### [General Guidelines](aws-sdk-java-v2-general.md)
Core development principles, code style standards, design patterns, and performance considerations that apply to all AWS SDK Java v2 development. Includes naming conventions, proper use of Optional, object method implementations, and exception handling patterns.

### [Asynchronous Programming Guidelines](async-programming-guidelines.md)
Best practices for CompletableFuture usage, thread safety considerations, and proper handling of asynchronous operations. Covers cancellation, exception handling, and testing patterns for async code.

### [Reactive Streams Guidelines](reactive-streams-guidelines.md)
Requirements and patterns for implementing Reactive Streams components. Ensures compliance with the Reactive Streams specification, proper backpressure handling, and mandatory TCK testing for Publisher/Subscriber implementations.

### [Testing Guidelines](testing-guidelines.md)
Comprehensive testing strategies including unit tests, functional tests, integration tests, and specialized test types like TCK tests for reactive streams. Covers test naming conventions, mocking guidelines, and coverage expectations.

### [Javadoc Guidelines](javadoc-guidelines.md)
Documentation standards for public APIs including formatting requirements, proper use of Javadoc tags, code snippet guidelines, and examples for different API classifications (public, protected, internal).

### [Logging Guidelines](logging-guidelines.md)
Logging standards specific to the AWS SDK including proper use of the SDK Logger, log level guidelines, structured logging patterns, and critical rules about avoiding duplicate error reporting when exceptions are thrown.

### [Code Generation Guidelines](code-generation-guidelines.md)
Patterns and standards for JavaPoet-based code generation including ClassSpec implementations, generator task organization, model processing, and fixture-based testing approaches for generated code validation.

### [Naming Conventions](NamingConventions.md)
Specific naming patterns for classes, methods, and tests including service client naming, acronym handling, and test naming conventions that clearly describe the method, conditions, and expected behavior.

### [Use of Optional](UseOfOptional.md)
Guidelines for when and how to use Optional in the SDK, including restrictions on usage in method parameters, member variables, and return types to maintain API clarity and consistency.

### [Static Factory Methods](FavorStaticFactoryMethods.md)
Patterns for preferring static factory methods over constructors, including naming conventions for factory methods and the benefits of this approach for immutable objects and API design.

### [Client Configuration](ClientConfiguration.md)
Structural requirements for configuration objects including immutability patterns, builder interfaces, field naming conventions, and proper handling of collection types in configuration APIs.

### [Business Metrics Guidelines](business-metrics-guidelines.md)
Guidelines for implementing business metrics in the AWS SDK for Java v2. Covers feature-centric placement principles, performance considerations, functional testing approaches, and a few examples of where we added business metrics for various features.

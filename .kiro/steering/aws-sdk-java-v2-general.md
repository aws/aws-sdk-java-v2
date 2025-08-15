---
title: AWS SDK for Java v2 General Guidelines
inclusion: always
---

# AWS SDK for Java v2 General Guidelines

## General Principles

- Write clean, readable, and maintainable code
- Follow the SOLID principles of object-oriented design
- Favor composition over inheritance
- Program to interfaces, not implementations
- Fail fast - detect and report errors as soon as possible
- Maintain backward compatibility when modifying existing APIs

## Code Style Standards

- Follow Java coding conventions and the existing style in the codebase
- Use meaningful variable, method, and class names that clearly indicate purpose
- Include comprehensive Javadoc for public APIs
- Keep methods short and focused on a single responsibility
- Limit the number of method parameters (ideally 3 or fewer)
- Use appropriate access modifiers (private, protected, public)
- Follow consistent indentation and formatting

## Common Design Patterns

- Use builder pattern for object creation
- Follow reactive programming principles for async operations
- Use SdkException hierarchy for error handling
- Prefer immutable objects where appropriate

## Naming Conventions

### Class Naming

#### General Rules
- Prefer singular class names: `SdkSystemSetting`, not `SdkSystemSettings`
- Treat acronyms as a single word: `DynamoDbClient`, not `DynamoDBClient`
  
#### Classes that instantiate other classes

- If the class's primary purpose is to return instances of another class:
  - If the "get" method has no parameters:
    - If the class implements `Supplier`: `{Noun}Supplier` (e.g. `CachedSupplier`)
    - If the class does not implement `Supplier`: `{Noun}Provider` (e.g. `AwsCredentialsProvider`)
  - If the "get" method has parameters: `{Noun}Factory` (e.g. `AwsJsonProtocolFactory`)

#### Service-specific classes

- If the class makes service calls:
  - If the class can be used to invoke *every* data-plane operation:
    - If the class is code generated:
      - If the class uses sync HTTP: `{ServiceName}Client` (e.g. `DynamoDbClient`)
      - If the class uses async HTTP: `{ServiceName}AsyncClient` (e.g. `DynamoDbAsyncClient`)
    - If the class is hand-written:
      - If the class uses sync HTTP: `{ServiceName}EnhancedClient` (e.g. `DynamoDbEnhancedClient`)
      - If the class uses async HTTP: `{ServiceName}EnhancedAsyncClient` (e.g. `DynamoDbEnhancedAsyncClient`)
  - If the class can be used to invoke only *some* data-plane operations:
    - If the class uses sync HTTP: `{ServiceName}{Noun}Manager` (e.g. `SqsBatchManager`)
    - If the class uses async HTTP: `{ServiceName}Async{Noun}Manager` (e.g. `SqsAsyncBatchManager`)
    - Note: If only one implementation exists and it uses async HTTP, `Async` may be excluded. (e.g. `S3TransferManager`)
- If the class does not make service calls:
  - If the class creates presigned URLs: `{ServiceName}Presigner` (e.g. `S3Presigner`)
  - If the class is a collection of various unrelated "helper" methods: `{ServiceName}Utilities` (e.g. `S3Utilities`)

## Class Initialization

- Favor static factory methods over constructors:
  - Static factory methods provide meaningful names compared with constructors
  - They are useful when working with immutable classes as we can reuse the same object
  - Static factory methods can return any subtype of that class

### Naming Conventions for Static Factory Methods
- `create()`, `create(params)` when creating a new instance (e.g., `DynamoDBClient.create()`)
- `defaultXXX()` when returning an instance with default settings (e.g., `BackoffStrategy.defaultStrategy()`)

## Use of Optional

- `Optional` **MUST NOT** be used when the result will never be null
- For return types:
  - `Optional` **SHOULD** be used when it is not obvious to a caller whether a result will be null
  - `Optional` **MUST NOT** be used for "getters" in generated service model classes
- For member variables: `Optional` **SHOULD NOT** be used
- For method parameters: `Optional` **MUST NOT** be used

## Object Methods (toString, equals, hashCode)

- All public POJO classes **MUST** implement `toString()`, `equals()`, and `hashCode()` methods
- When adding new fields to existing POJO classes, these methods **MUST** be updated to include the new fields
- Implementation guidelines:
  - `toString()`: Include class name and all fields with their values
    - **MUST** use the SDK's `ToString` utility class (`utils/src/main/java/software/amazon/awssdk/utils/ToString.java`) for consistent formatting:
      ```java
      @Override
      public String toString() {
          return ToString.builder("YourClassName")
                         .add("fieldName1", fieldValue1)
                         .add("fieldName2", fieldValue2)
                         .build();
      }
      ```
  - `equals()`: Compare all fields for equality, including proper null handling
  - `hashCode()`: Include all fields in the hash code calculation
- Consider using IDE-generated implementations
- For immutable objects, consider caching the hash code value for better performance
- Unit tests **MUST** be added using EqualsVerifier to ensure all fields are properly included in equals and hashCode implementations. Example:
  ```java
  @Test
  public void equalsHashCodeTest() {
      EqualsVerifier.forClass(YourClass.class)
                    .withNonnullFields("requiredFields")
                    .verify();
  }
  ```

## Exception Handling

- Avoid throwing checked exceptions in public APIs
- Don't catch exceptions unless you can handle them properly
- Always include meaningful error messages
- Clean up resources in finally blocks or use try-with-resources
- Don't use exceptions for flow control

## Use of Existing Utility Classes

- Developers **MUST** check for existing utility methods before implementing their own
- Common utility classes are available in the `utils` module and **SHOULD** be used when applicable
- Examples of available utility classes:
  - `Lazy` (`utils/src/main/java/software/amazon/awssdk/utils/Lazy.java`): For thread-safe lazy initialization of singleton objects
  - `ToString` (`utils/src/main/java/software/amazon/awssdk/utils/ToString.java`): For consistent toString() implementations
  - `IoUtils`: For safely closing resources and handling I/O operations
  - `StringUtils`: For common string operations
  - `CollectionUtils`: For common collection operations
  - `ValidationUtils`: For input validation
- Using existing utilities ensures:
  - Consistent behavior across the codebase
  - Thread-safety where applicable
  - Proper handling of edge cases
  - Reduced code duplication
- Example of using `Lazy` for thread-safe singleton initialization:
  ```java
  private static final Lazy<ExpensiveObject> INSTANCE = new Lazy<>(() -> new ExpensiveObject());
  
  public static ExpensiveObject getInstance() {
      return INSTANCE.getValue();
  }
  ```

## Performance

- Avoid premature optimization
- Use appropriate data structures for the task
- Be mindful of memory usage, especially with large collections
- Consider thread safety in concurrent applications
- Use profiling tools to identify actual bottlenecks

## References

- [AWS SDK for Java Developer Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Design Documentation](https://github.com/aws/aws-sdk-java-v2/tree/master/docs/design)
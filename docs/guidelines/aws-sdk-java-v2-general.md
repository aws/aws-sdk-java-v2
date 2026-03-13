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
- Minimize API surface area and prefer internal over public
- Follow consistent indentation and formatting

## Common Design Patterns

- Use builder pattern for object creation
- Follow reactive programming principles for async operations
- Use SdkException hierarchy for error handling
- Prefer immutable objects where appropriate

## SDK Utilities

The AWS SDK for Java v2 provides several utility classes that should be used instead of external libraries or custom implementations:

### JSON Parsing
- **Production Code**: **MUST** use `JsonNodeParser` from the `json-utils` module (`core/json-utils/src/main/java/software/amazon/awssdk/protocols/jsoncore/JsonNodeParser.java`) for parsing JSON content
- **Test Code Exception**: Test code **MAY** use external JSON libraries like Jackson or javax.json for convenience
- **Codegen Exception**: Code generation templates **MAY** use external JSON libraries when appropriate
- **MUST NOT** use external JSON libraries in production SDK code
- Example usage:
  ```java
  JsonNodeParser parser = JsonNodeParser.create();
  JsonNode rootNode = parser.parse(jsonString);
  ```

### Lazy Initialization
- **MUST** use `Lazy` from the `utils` module (`utils/src/main/java/software/amazon/awssdk/utils/Lazy.java`) for thread-safe lazy initialization
- **MUST NOT** implement custom lazy initialization patterns
- Example usage:
  ```java
  private static final Lazy<ExpensiveObject> EXPENSIVE_OBJECT = 
      new Lazy<>(() -> new ExpensiveObject());
  
  public ExpensiveObject getExpensiveObject() {
      return EXPENSIVE_OBJECT.getValue();
  }
  ```

### Caching with TTL
- **MUST** use `CachedSupplier` from the `utils` module (`utils/src/main/java/software/amazon/awssdk/utils/cache/CachedSupplier.java`) for caching values with time-to-live (TTL) functionality
- **MUST NOT** implement custom caching mechanisms with expiration logic
- Use `CachedSupplier` when you need to cache expensive operations that should be refreshed periodically
- Example usage:
  ```java
  private final Supplier<AuthToken> tokenCache = CachedSupplier.builder(() -> 
      RefreshResult.builder(fetchAuthToken())
                   .staleTime(Instant.now().plus(Duration.ofMinutes(5)))
                   .build())
      .cachedValueName("AuthToken")
      .build();
  
  public AuthToken getAuthToken() {
      return tokenCache.get();
  }
  
  private AuthToken fetchAuthToken() {
      // Expensive operation to fetch token
      return callAuthService();
  }
  ```
- **Key Features**:
  - Thread-safe caching with automatic expiration
  - Configurable stale time for cache invalidation
  - Optional prefetch functionality for proactive refresh
  - Built-in logging and debugging support via `cachedValueName`

## Naming Conventions

See [Naming Conventions Guidelines](NamingConventions.md) for detailed information.

## Class Initialization

See [Favor Static Factory Methods Guidelines](FavorStaticFactoryMethods.md) for detailed information.

## Use of Optional

See [Use of Optional Guidelines](UseOfOptional.md) for detailed information.

## Object Methods (toString, equals, hashCode)

- All public POJO classes **MUST** implement `toString()`, `equals()`, and `hashCode()` methods
- When adding new fields to existing POJO classes, these methods **MUST** be updated to include the new fields
- Implementation guidelines:
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
  - **MUST NOT** include fields that could be considered sensitive such as credentials
  - `equals()`: Compare all fields for equality, including proper null handling
  - `hashCode()`: Include all fields in the hash code calculation
- Unit tests **MUST** be added using EqualsVerifier to ensure all fields are properly included in equals and hashCode implementations. Example:
  ```java
  @Test
  public void equalsHashCodeTest() {
      EqualsVerifier.forClass(YourClass.class)
                    .withNonnullFields("requiredFields")
                    .verify();
  }
  ```
- See example implementation in `core/regions/src/test/java/software/amazon/awssdk/regions/PartitionEndpointKeyTest.java`

## Exception Handling

- Avoid throwing checked exceptions in public APIs
- Don't catch exceptions unless you can handle them properly
- Always include meaningful error messages
- Clean up resources in finally blocks or use try-with-resources
- Don't use exceptions for flow control because exceptions are expensive
  ```java
  // BAD: Calling validators expecting them to throw
  public ValidationResult validateRequest(Request request) {
      try {
          validateRequired(request);     // Throws if required fields missing
          validateFormat(request);       // Throws if format invalid
          validatePermissions(request);  // Throws if permissions insufficient
          return ValidationResult.success();
      } catch (ValidationException e) {
          return ValidationResult.failure(e.getMessage());
      }
  }

  // GOOD: Explicit validation with return values
  public ValidationResult validateRequest(Request request) {
      ValidationResult result = validateRequired(request);
      if (!result.isValid()) {
          return result;
      }
    
      result = validateFormat(request);
      if (!result.isValid()) {
          return result;
      }
    
      result = validatePermissions(request);
      return result;
  }
  ```
- **MUST NOT** catch and rethrow the same exception type

```java
// BAD: Catching and rethrowing with the same message
public void processMessage(String message) {
    try {
        parseMessage(message);
    } catch (SnsMessageParsingException e) {
        throw new SnsMessageParsingException(e.getMessage(), e); 
    }
  }
// BAD: Even with additional context, don't catch and rethrow the same exception type
public void processMessage(String message) {
    try {
        parseMessage(message);
    } catch (SnsMessageParsingException e) {
        // WRONG - Don't catch and rethrow SnsMessageParsingException as SnsMessageParsingException
        throw SnsMessageParsingException.builder()
            .message("Failed to process SNS message in batch operation. " + e.getMessage() + 
                    " Message index: " + getCurrentMessageIndex())
            .cause(e)
            .build();
    }
} 
```

### Error Message Guidelines
- **MUST** provide context-specific error messages that explain what went wrong and how to fix it
- **MUST** include relevant details (field names, expected formats, received values) without exposing sensitive information
- **MUST** chain exceptions properly to preserve error context while adding meaningful information
- **SHOULD** include troubleshooting guidance when appropriate


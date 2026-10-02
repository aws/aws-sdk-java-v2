# Logging Guidelines for AWS SDK for Java v2

## Table of Contents
- [General Principles](#general-principles)
- [Log Levels](#log-levels)
- [Structured Logging](#structured-logging)
- [Sensitive Data Handling](#sensitive-data-handling)
- [Error Logging](#error-logging)
- [Testing Logging](#testing-logging)

## General Principles

### Core Logging Standards
- **Use SDK Logger**: Always use `software.amazon.awssdk.utils.Logger`, never use SLF4J or other logging frameworks directly
- **Meaningful messages**: Write clear, actionable log messages that help with debugging
- **Include context**: Add relevant context like request IDs or operation names
- **Be consistent**: Use consistent formatting and terminology across the codebase
- **Consider the audience**: Write logs for the person who will debug the issue (often not you)
  - Include enough context so someone unfamiliar with the code can understand what happened
  - Avoid internal jargon or abbreviations that only the original developer would understand
  - Include business context, not just technical details
  - Think about what information would help during a production incident at 3 AM

### Logger Declaration
- **Static final loggers**: Declare loggers as `private static final`
- **Class-based loggers**: Use the class for logger creation

```java
// Good: Proper logger declaration
private static final Logger logger = Logger.loggerFor(MyClass.class);
```

## Log Levels

### Level Guidelines
Use log levels consistently across the SDK:

**Important**: Do not abuse WARN and ERROR levels. Only use WARN and ERROR for issues that are NOT surfaced to users through exceptions. If an exception will be thrown to the caller, do not also log it as WARN or ERROR - this creates duplicate error reporting and log noise. The exception itself is the error reporting mechanism for the user. When WARN or ERROR conditions affect every request (e.g., deprecated configuration, missing optional features, or suboptimal settings), use a one-time logging pattern to avoid flooding logs with repetitive messages. Log once per JVM startup rather than per request.

**ERROR**: System errors, exceptions that prevent operation completion AND are not thrown to the caller
- Internal system or callback failures that don't result in thrown exceptions
- Background process failures
- Resource cleanup failures

```java
// ERROR: Critical failures that don't throw exceptions to caller
logger.error(() -> "SdkAsyncHttpResponseHandler " + responseHandler + " threw an exception in onError. It will be ignored.", e);
logger.error(() -> "Connection pool health check failed [pool=" + poolName + "]", exception);

// BAD: Don't log ERROR if you're throwing the exception to the caller
public void validateInput(String input) {
    if (input == null) {
        logger.error(() -> "Input validation failed: null input"); // DON'T DO THIS
        throw new IllegalArgumentException("Input cannot be null"); // Exception already tells the user
    }
}
```

**WARN**: Recoverable issues, deprecated usage, performance concerns that do NOT result in exceptions thrown to caller
- Successful fallback operations (user doesn't see the failure)
- Deprecated API usage (operation still succeeds)
- Performance degradation (operation completes but slowly)
- Configuration issues with successful fallbacks

```java
logger.warn(() -> "Primary endpoint failed, falling back to secondary [primary=" + 
                  primaryEndpoint + ", secondary=" + secondaryEndpoint + "]");
logger.warn(() -> "SSL Certificate verification is disabled. This is not a safe setting and should only be used for testing.");

// BAD: Don't log WARN if the failure will be thrown to caller
public Response callService() {
    try {
        return primaryService.call();
    } catch (ServiceException e) {
        logger.warn(() -> "Service call failed", e); // DON'T DO THIS
        throw e; // Exception already informs the caller
    }
}

// GOOD: Log WARN only when you handle the error internally
public Response callServiceWithFallback() {
    try {
        return primaryService.call();
    } catch (ServiceException e) {
        logger.warn(() -> "Primary service failed, using fallback [error=" + e.getMessage() + "]");
        return fallbackService.call(); // User gets successful response
    }
}
```

**INFO**: Do not use INFO level logging in the AWS SDK for Java v2.

Use DEBUG level instead for operational information that might be useful for troubleshooting:

**DEBUG**: Operational information, detailed execution flow, parameter values
- Service startup/shutdown
- Major operation completion
- Configuration changes
- Important state transitions
- Method entry/exit with parameters
- Intermediate processing steps
- Configuration details
- Performance metrics

```java
// DEBUG: Operational and detailed execution information
logger.debug(() -> "Retry attempt [attemptNumber=" + attemptNumber + 
                   ", delay=" + delayMs + "ms]");
```

**TRACE**: Very detailed execution, typically for troubleshooting
- Detailed state information
- Fine-grained timing information

```java
 logger.debug(() -> "Interceptor '" + interceptor + "' modified the message with its " + methodName + " method.");
 // TRACE: detailed execution information
logger.trace(() -> "Old: " + originalMessage + "\nNew: " + newMessage);
```            
## Structured Logging

### Consistent Format
Use consistent key-value formatting for structured data when applicable:

```java
// Good: Consistent structured format
logger.debug(() -> "Operation completed [operation=" + operationName + 
                   ", duration=" + duration + "ms, status=" + status + 
                   ", itemCount=" + itemCount + "]");
```

## Sensitive Data Handling

Never log sensitive information.

**Never Log:**
- HTTP header values
- Authentication credentials
- Members with senstive trait 

**Safe to Log:**
- Request IDs, correlation IDs
- Operation names, status codes
- Timing and performance metrics

### Error Context
Provide sufficient context for debugging:

```java
// Good: Rich error context
s3AsyncClient.abortMultipartUpload(request)
             .exceptionally(throwable -> {
                 logger.warn(() -> String.format("Failed to abort previous multipart upload. You may need to call S3AsyncClient#abortMultiPartUpload to "
                 + "free all storage consumed by all parts. [id=%s]", uploadId), throwable);
                 return null;
            });
```

## Testing Logging

### Using LogCaptor
The AWS SDK provides `software.amazon.awssdk.testutils.LogCaptor` for testing log output:

- **Create LogCaptor**: Use `LogCaptor.create(ClassName.class)` to capture logs for a specific class
- **Set log level**: Use `logCaptor.setLevel(Level.DEBUG)` to capture logs at specific levels
- **Access events**: Use `logCaptor.loggedEvents()` to get captured log events
- **Clean up**: Always call `logCaptor.close()` in `@AfterEach` to prevent memory leaks

### Test Log Levels
Test important log messages in unit tests and verify appropriate log levels are used using `LogCaptor`:

```java
@Test
void processRequest_withFallback_logsAtWarnLevel() {
    Request request = createRequestThatTriggersFailover();
    
    Response response = service.processRequestWithFallback(request);
    
    assertThat(response).isNotNull(); // Operation succeeded via fallback
    
    List<LogEvent> warnEvents = logCaptor.loggedEvents().stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .collect(Collectors.toList());
    
    assertThat(warnEvents).hasSize(1);
    assertThat(warnEvents.get(0).getMessage())
        .contains("Primary service failed, using fallback");
}

@Test
void processRequest_withValidation_logsAtDebugLevel() {
    logCaptor.setLevel(Level.DEBUG);
    Request request = createInvalidRequest();
    
    assertThatThrownBy(() -> service.processRequest(request))
        .isInstanceOf(ValidationException.class);
    
    List<LogEvent> debugEvents = logCaptor.loggedEvents().stream()
        .filter(event -> event.getLevel() == Level.DEBUG)
        .collect(Collectors.toList());
    
    assertThat(debugEvents).isNotEmpty();
    assertThat(debugEvents.get(0).getMessage()).contains("Input validation failed");
}
```
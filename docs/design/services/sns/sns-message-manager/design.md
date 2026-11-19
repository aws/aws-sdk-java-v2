# Design Document

## Overview

The SnsMessageManager feature provides automatic validation of SNS message signatures in AWS SDK for Java v2, following the same architectural pattern as the SqsAsyncBatchManager. This utility will be implemented as a separate manager class within the SNS service module that handles the parsing and cryptographic verification of SNS messages received via HTTP/HTTPS endpoints.

The design follows the established AWS SDK v2 patterns for utility classes, providing a clean API for developers to validate SNS message authenticity without requiring deep knowledge of the underlying cryptographic verification process.

## Usage Examples

### Example 1: Basic Message Validation

```java
// Create the message manager
SnsMessageManager messageManager = SnsMessageManager.builder().build();

// Validate a message from HTTP request body
String messageBody = request.getBody(); // JSON message from SNS
try {
    SnsMessage validatedMessage = messageManager.parseMessage(messageBody);
    
    // Access message content
    String messageContent = validatedMessage.message();
    String topicArn = validatedMessage.topicArn();
    String messageType = validatedMessage.type();
    
    // Process the validated message
    processNotification(messageContent, topicArn);
    
} catch (SnsMessageValidationException e) {
    // Handle validation failure
    logger.error("SNS message validation failed: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
}
```

### Example 2: Custom Configuration

```java
// Configure certificate caching and timeouts
SnsMessageManager messageManager = SnsMessageManager.builder()
    .configuration(config -> config
        .certificateCacheTimeout(Duration.ofHours(1))
        .httpTimeout(Duration.ofSeconds(10))
        .strictCertificateValidation(true))
    .build();

// Validate message with custom configuration
SnsMessage message = messageManager.parseMessage(inputStream);
```

### Example 3: Handling Different Message Types

```java
SnsMessageManager messageManager = SnsMessageManager.builder().build();

try {
    SnsMessage message = messageManager.parseMessage(messageJson);
    
    switch (message.type()) {
        case "Notification":
            handleNotification(message.message(), message.subject());
            break;
        case "SubscriptionConfirmation":
            confirmSubscription(message.token(), message.topicArn());
            break;
        case "UnsubscribeConfirmation":
            handleUnsubscribe(message.token(), message.topicArn());
            break;
        default:
            logger.warn("Unknown message type: {}", message.type());
    }
    
} catch (SnsSignatureValidationException e) {
    logger.error("Invalid signature: {}", e.getMessage());
} catch (SnsMessageParsingException e) {
    logger.error("Malformed message: {}", e.getMessage());
} catch (SnsCertificateException e) {
    logger.error("Certificate error: {}", e.getMessage());
}
```



## Architecture

### Sync vs Async Support Decision

Unlike SqsBatchManager which provides async support for batching operations, SNS message validation is **synchronous by nature** - you receive a message and need to validate it immediately before processing.

We will start with **sync-only support** (`SnsMessageManager`) for the following reasons:
- Most common use case is HTTP endpoint handlers requiring immediate validation
- Simpler implementation and maintenance
- Can add async support later if customer demand emerges
- Follows YAGNI principle - avoid unnecessary complexity

### Package Structure
```
services/sns/src/main/java/software/amazon/awssdk/services/sns/
├── messagemanager/
│   ├── SnsMessageManager.java (public interface)
│   └── MessageManagerConfiguration.java (configuration class)
└── internal/
    └── messagemanager/
        ├── DefaultSnsMessageManager.java (implementation)
        ├── SnsMessageParser.java (message parsing logic)
        ├── SignatureValidator.java (signature validation)
        ├── CertificateRetriever.java (certificate management)
        └── SnsMessageImpl.java (message representation)
```

### Core Components

#### 1. SnsMessageManager (Public Interface)
- Main entry point for developers
- Provides `parseMessage()` methods for validation
- Follows builder pattern similar to other SDK utilities
- Thread-safe and reusable

#### 2. MessageManagerConfiguration
- Configuration class for customizing validation behavior
- Controls certificate caching, timeout settings
- Similar to other SDK configuration classes

#### 3. DefaultSnsMessageManager (Internal Implementation)
- Implements the SnsMessageManager interface
- Coordinates between parser, validator, and certificate retriever
- Manages configuration and lifecycle

#### 4. SnsMessageParser
- Parses JSON message payload
- Extracts signature fields and message content
- Validates message format and required fields

#### 5. SignatureValidator
- Performs cryptographic signature verification using SHA1 (SignatureVersion1) and SHA256 (SignatureVersion2)
- Uses AWS certificate to validate message authenticity
- Handles different signature versions and validates certificate chain of trust

#### 6. CertificateRetriever
- Retrieves and caches SNS certificates using HTTPS only
- Validates certificate URLs against known SNS-signed domains
- Supports different AWS regions and partitions (aws, aws-gov, aws-cn)
- Verifies certificate chain of trust and Amazon SNS issuance

## Components and Interfaces

### SnsMessageManager Interface
```java
@SdkPublicApi
public interface SnsMessageManager extends SdkAutoCloseable {
    
    static Builder builder() {
        return DefaultSnsMessageManager.builder();
    }
    
    /**
     * Parses and validates an SNS message from InputStream
     */
    SnsMessage parseMessage(InputStream messageStream);
    
    /**
     * Parses and validates an SNS message from String
     */
    SnsMessage parseMessage(String messageContent);
    
    interface Builder extends CopyableBuilder<Builder, SnsMessageManager> {
        Builder configuration(MessageManagerConfiguration configuration);
        Builder configuration(Consumer<MessageManagerConfiguration.Builder> configuration);
        SnsMessageManager build();
    }
}
```

### SnsMessage Interface
```java
@SdkPublicApi
public interface SnsMessage {
    String type();
    String messageId();
    String topicArn();
    String subject();
    String message();
    Instant timestamp();
    String signatureVersion();
    String signature();
    String signingCertUrl();
    String unsubscribeUrl();
    String token();
    Map<String, String> messageAttributes();
}
```

### MessageManagerConfiguration
```java
@SdkPublicApi
@Immutable
@ThreadSafe
public final class MessageManagerConfiguration 
        implements ToCopyableBuilder<MessageManagerConfiguration.Builder, MessageManagerConfiguration> {
    
    private final Duration certificateCacheTimeout;
    private final SdkHttpClient httpClient;
    
    // Constructor, getters, toBuilder() implementation
    
    public static Builder builder() {
        return new DefaultMessageManagerConfigurationBuilder();
    }
    
    public Duration certificateCacheTimeout() { return certificateCacheTimeout; }
    public SdkHttpClient httpClient() { return httpClient; }
    
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, MessageManagerConfiguration> {
        Builder certificateCacheTimeout(Duration certificateCacheTimeout);
        Builder httpClient(SdkHttpClient httpClient);
    }
}
```

## Data Models

### Message Types
The manager will support all standard SNS message types:
- **Notification**: Standard SNS notifications
- **SubscriptionConfirmation**: Subscription confirmation messages
- **UnsubscribeConfirmation**: Unsubscribe confirmation messages

### Message Fields
Standard SNS message fields that will be parsed and validated:
- Type (required)
- MessageId (required)
- TopicArn (required)
- Message (required for Notification)
- Timestamp (required)
- SignatureVersion (required)
- Signature (required)
- SigningCertURL (required)
- Subject (optional)
- UnsubscribeURL (optional for Notification)
- Token (required for confirmations)
- MessageAttributes (optional)

### Certificate Management
- Certificate URLs will be validated against known AWS SNS-signed domains only
- Certificates retrieved exclusively via HTTPS to prevent interception attacks
- Certificate chain of trust validation to ensure Amazon SNS issuance
- Certificates will be cached with configurable TTL
- Support for different AWS partitions (aws, aws-gov, aws-cn)
- Rejection of any certificates provided directly in messages without validation

## Error Handling

### Exception Hierarchy
```java
public class SnsMessageValidationException extends SdkException {
    // Base exception for all validation failures
}

public class SnsMessageParsingException extends SnsMessageValidationException {
    // JSON parsing or format errors
}

public class SnsSignatureValidationException extends SnsMessageValidationException {
    // Signature verification failures
}

public class SnsCertificateException extends SnsMessageValidationException {
    // Certificate retrieval or validation errors
}
```

### Error Scenarios
1. **Malformed JSON**: Clear parsing error with details
2. **Missing Required Fields**: Specific field validation errors
3. **Invalid Signature**: Cryptographic verification failure
4. **Certificate Issues**: Certificate retrieval or validation problems
5. **Invalid Certificate URL**: Security validation of certificate source

## Testing Strategy

### Unit Tests
- **SnsMessageParser**: JSON parsing, field extraction, format validation
- **SignatureValidator**: Cryptographic verification with known test vectors
- **CertificateRetriever**: Certificate fetching, caching, URL validation
- **DefaultSnsMessageManager**: Integration of all components

### Integration Tests
- **Real SNS Messages**: Test with actual SNS message samples
- **Different Regions**: Validate messages from various AWS regions
- **Message Types**: Test all supported message types
- **Error Conditions**: Verify proper error handling

### Test Data
- Sample SNS messages for each type (Notification, SubscriptionConfirmation, UnsubscribeConfirmation)
- Invalid messages for error testing
- Test certificates and signatures for validation testing

## Implementation Considerations

### Security
- Certificate URL validation against known AWS SNS-signed domains only
- HTTPS-only certificate retrieval to prevent interception attacks
- Proper certificate chain validation and Amazon SNS issuance verification
- Protection against certificate spoofing attacks
- Rejection of unexpected message fields or formats
- Never trusting certificates provided directly in messages without validation

### Performance
- Certificate caching to avoid repeated HTTP requests
- Efficient JSON parsing
- Thread-safe implementation for concurrent usage

### Compatibility
- Support for SignatureVersion1 (SHA1) and SignatureVersion2 (SHA256) as per AWS SNS standards
- Graceful handling of future signature version updates
- Consistent behavior across different AWS partitions
- API compatibility with AWS SDK v1 SnsMessageManager functionality

### Dependencies
The implementation will require:
- JSON parsing (Jackson, already available in SDK)
- HTTP client for certificate retrieval (SDK's HTTP client)
- Cryptographic libraries (Java standard library)
- No additional external dependencies
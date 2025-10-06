/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.sns.internal.messagemanager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.services.sns.messagemanager.MessageManagerConfiguration;
import software.amazon.awssdk.services.sns.messagemanager.SnsCertificateException;
import software.amazon.awssdk.services.sns.messagemanager.SnsMessage;
import software.amazon.awssdk.services.sns.messagemanager.SnsMessageManager;
import software.amazon.awssdk.services.sns.messagemanager.SnsMessageParsingException;
import software.amazon.awssdk.services.sns.messagemanager.SnsSignatureValidationException;
import software.amazon.awssdk.utils.AttributeMap;


/**
 * Default implementation of {@link SnsMessageManager} that provides comprehensive SNS message validation.
 *
 * <p>This class coordinates between the message parser, signature validator, and certificate retriever
 * to provide complete SNS message validation functionality. It handles the entire validation pipeline
 * including JSON parsing, certificate retrieval and caching, and cryptographic signature verification.
 *
 * <p>The implementation supports:
 * <ul>
 *   <li>Both SignatureVersion1 (SHA1) and SignatureVersion2 (SHA256) signature algorithms</li>
 *   <li>Automatic certificate retrieval and caching from trusted SNS domains</li>
 *   <li>All SNS message types (Notification, SubscriptionConfirmation, UnsubscribeConfirmation)</li>
 *   <li>Configurable HTTP client and certificate cache timeout settings</li>
 *   <li>Thread-safe concurrent usage</li>
 * </ul>
 *
 * <p>This class manages the lifecycle of HTTP resources and implements {@link SdkAutoCloseable}
 * to ensure proper cleanup. When using a custom HTTP client via configuration, the client's
 * lifecycle is managed externally. When using the default HTTP client, this class manages
 * the client's lifecycle and closes it when {@link #close()} is called.
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and can be used concurrently
 * from multiple threads. Certificate caching is implemented using thread-safe collections.
 *
 * <p><strong>Resource Management:</strong> Instances should be closed when no longer needed
 * to free HTTP client resources. Use try-with-resources or explicit close() calls.
 *
 * @see SnsMessageManager
 * @see MessageManagerConfiguration
 * @see SnsMessage
 */
@SdkInternalApi
public final class DefaultSnsMessageManager implements SnsMessageManager {

    /** The configuration settings for this message manager instance. */
    private final MessageManagerConfiguration configuration;
    
    /** Certificate retriever for fetching and caching SNS signing certificates. */
    private final CertificateRetriever certificateRetriever;
    
    /** HTTP client used for certificate retrieval operations. */
    private final SdkHttpClient httpClient;
    
    /** Flag indicating whether this instance should close the HTTP client on cleanup. */
    private final boolean shouldCloseHttpClient;

    private DefaultSnsMessageManager(DefaultBuilder builder) {
        this.configuration = builder.configuration != null 
            ? builder.configuration 
            : MessageManagerConfiguration.builder().build();
        
        // Initialize HTTP client - use provided one or create default
        if (configuration.httpClient() != null) {
            this.httpClient = configuration.httpClient();
            this.shouldCloseHttpClient = false;
        } else {
            this.httpClient = new DefaultSdkHttpClientBuilder().buildWithDefaults(createHttpDefaults());
            this.shouldCloseHttpClient = true;
        }
        
        // Initialize certificate retriever
        this.certificateRetriever = new CertificateRetriever(httpClient, configuration.certificateCacheTimeout());
    }

    /**
     * Creates a new builder for {@link DefaultSnsMessageManager}.
     *
     * @return A new builder instance.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public SnsMessage parseMessage(InputStream messageStream) {
        // Comprehensive input validation
        validateInputStreamParameter(messageStream);
        
        try {
            String messageContent = readInputStreamToString(messageStream);
            return parseMessage(messageContent);
        } catch (IOException e) {
            throw SnsMessageParsingException.builder()
                .message("Failed to read message from InputStream. This may indicate a network issue, " +
                        "stream corruption, or insufficient memory. Error: " + e.getMessage())
                .cause(e)
                .build();
        }
    }

    @Override
    public SnsMessage parseMessage(String messageContent) {
        // Comprehensive input validation with detailed error messages
        validateStringMessageParameter(messageContent);
        
        try {
            // Step 1: Parse the JSON message
            SnsMessage parsedMessage = SnsMessageParser.parseMessage(messageContent);
            
            // Step 2: Retrieve the certificate
            byte[] certificateBytes = certificateRetriever.retrieveCertificate(parsedMessage.signingCertUrl());
            
            // Step 3: Validate the signature
            SignatureValidator.validateSignature(parsedMessage, certificateBytes);
            
            // Return the validated message
            return parsedMessage;
            
        } catch (SnsMessageParsingException | SnsSignatureValidationException | SnsCertificateException e) {
            // Let SNS-specific exceptions propagate as-is with their original detailed messages
            throw e;
        } catch (Exception e) {
            // Only wrap truly unexpected exceptions
            throw SnsMessageParsingException.builder()
                .message("Unexpected error during message validation: " + e.getMessage() + 
                        ". Please check that the message is a valid SNS message and try again.")
                .cause(e)
                .build();
        }
    }

    @Override
    public void close() {
        // Close HTTP client only if we created it
        if (shouldCloseHttpClient && httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                // Log and ignore - we're closing anyway
                // In a real implementation, this would use a logger
            }
        }
    }
    
    /**
     * Validates the InputStream parameter with comprehensive error reporting.
     *
     * @param messageStream The InputStream to validate.
     * @throws SnsMessageParsingException If validation fails.
     */
    private void validateInputStreamParameter(InputStream messageStream) {
        if (messageStream == null) {
            throw SnsMessageParsingException.builder()
                .message("Message InputStream cannot be null. Please provide a valid InputStream containing SNS message data.")
                .build();
        }
        
        // Additional validation could be added here for stream state if needed
    }

    /**
     * Validates the String message parameter with comprehensive error reporting.
     *
     * @param messageContent The message content to validate.
     * @throws SnsMessageParsingException If validation fails.
     */
    private void validateStringMessageParameter(String messageContent) {
        if (messageContent == null) {
            throw SnsMessageParsingException.builder()
                .message("Message content cannot be null. Please provide a valid SNS message JSON string.")
                .build();
        }
        
        if (messageContent.trim().isEmpty()) {
            throw SnsMessageParsingException.builder()
                .message("Message content cannot be empty or contain only whitespace. " +
                        "Please provide a valid SNS message JSON string.")
                .build();
        }
        
        // Check for reasonable message size limits
        if (messageContent.length() > 256 * 1024) { // 256KB limit
            throw SnsMessageParsingException.builder()
                .message("Message content is too large (" + messageContent.length() + " characters). " +
                        "SNS messages should typically be under 256KB. Please verify the message content.")
                .build();
        }
        
        // Basic format validation - should look like JSON
        String trimmed = messageContent.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw SnsMessageParsingException.builder()
                .message("Message content does not appear to be valid JSON. " +
                        "SNS messages must be in JSON format starting with '{' and ending with '}'. " +
                        "Received content starts with: " + 
                        (trimmed.length() > 50 ? trimmed.substring(0, 50) + "..." : trimmed))
                .build();
        }
    }

    /**
     * Reads an InputStream to a String using UTF-8 encoding with enhanced error handling.
     *
     * @param inputStream The InputStream to read.
     * @return The string content.
     * @throws IOException If reading fails.
     */
    private String readInputStreamToString(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            // Read with size limit to prevent memory exhaustion
            byte[] buffer = new byte[8192];
            int totalBytesRead = 0;
            int maxSize = 256 * 1024; // 256KB limit
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                
                if (totalBytesRead > maxSize) {
                    throw new IOException("InputStream content exceeds maximum allowed size of " + maxSize + " bytes. " +
                                        "SNS messages should typically be much smaller.");
                }
                
                result.write(buffer, 0, bytesRead);
            }
            
            if (totalBytesRead == 0) {
                throw new IOException("InputStream is empty. Please provide a valid InputStream containing SNS message data.");
            }
            
            return result.toString(StandardCharsets.UTF_8.name());
        }
    }
    
    /**
     * Creates HTTP defaults for the SNS message manager.
     */
    private static AttributeMap createHttpDefaults() {
        return AttributeMap.builder()
                          .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, java.time.Duration.ofSeconds(10))
                          .put(SdkHttpConfigurationOption.READ_TIMEOUT, java.time.Duration.ofSeconds(30))
                          .build();
    }

    /**
     * Builder implementation for {@link DefaultSnsMessageManager}.
     */
    public static final class DefaultBuilder implements Builder {
        private MessageManagerConfiguration configuration;

        private DefaultBuilder() {
        }

        @Override
        public Builder configuration(MessageManagerConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        @Override
        public SnsMessageManager build() {
            return new DefaultSnsMessageManager(this);
        }
    }
}
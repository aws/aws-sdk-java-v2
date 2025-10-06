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

package software.amazon.awssdk.services.sns.messagemanager;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a validated SNS message with all its attributes.
 *
 * <p>This class provides access to all standard SNS message fields after successful signature validation.
 * The message has been cryptographically verified to be authentic and from Amazon SNS.
 *
 * <p>Supports all SNS message types:
 * <ul>
 *   <li><strong>Notification</strong>: Standard SNS notifications</li>
 *   <li><strong>SubscriptionConfirmation</strong>: Subscription confirmation messages</li>
 *   <li><strong>UnsubscribeConfirmation</strong>: Unsubscribe confirmation messages</li>
 * </ul>
 *
 * <p>This class is immutable and thread-safe. All required fields are validated during construction.
 * Instances are typically created through the {@link SnsMessageManager#parseMessage(String)} method
 * after successful message validation.
 *
 * <p>Example usage:
 * <pre>{@code
 * SnsMessageManager messageManager = SnsMessageManager.builder().build();
 * SnsMessage message = messageManager.parseMessage(jsonMessageBody);
 * 
 * // Access message properties
 * String messageType = message.type();
 * String content = message.message();
 * String topicArn = message.topicArn();
 * 
 * // Handle optional fields
 * message.subject().ifPresent(subject -> 
 *     System.out.println("Subject: " + subject));
 * }</pre>
 *
 * @see SnsMessageManager
 */
@SdkPublicApi
public final class SnsMessage {

    private final String type;
    private final String messageId;
    private final String topicArn;
    private final String subject;
    private final String message;
    private final Instant timestamp;
    private final String signatureVersion;
    private final String signature;
    private final String signingCertUrl;
    private final String unsubscribeUrl;
    private final String token;
    private final Map<String, String> messageAttributes;

    private SnsMessage(Builder builder) {
        this.type = Validate.paramNotNull(builder.type, "type");
        this.messageId = Validate.paramNotNull(builder.messageId, "messageId");
        this.topicArn = Validate.paramNotNull(builder.topicArn, "topicArn");
        this.subject = builder.subject;
        this.message = Validate.paramNotNull(builder.message, "message");
        this.timestamp = Validate.paramNotNull(builder.timestamp, "timestamp");
        this.signatureVersion = Validate.paramNotNull(builder.signatureVersion, "signatureVersion");
        this.signature = Validate.paramNotNull(builder.signature, "signature");
        this.signingCertUrl = Validate.paramNotNull(builder.signingCertUrl, "signingCertUrl");
        this.unsubscribeUrl = builder.unsubscribeUrl;
        this.token = builder.token;
        this.messageAttributes = builder.messageAttributes != null 
            ? Collections.unmodifiableMap(builder.messageAttributes) 
            : Collections.emptyMap();
    }

    /**
     * Creates a new builder for constructing SnsMessage instances.
     *
     * @return A new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the message type.
     * <p>
     * Valid values are:
     * <ul>
     *   <li>"Notification" - Standard SNS notification</li>
     *   <li>"SubscriptionConfirmation" - Subscription confirmation message</li>
     *   <li>"UnsubscribeConfirmation" - Unsubscribe confirmation message</li>
     * </ul>
     *
     * @return The message type (never null).
     */
    public String type() {
        return type;
    }

    /**
     * Returns the unique message identifier.
     *
     * @return The message ID (never null).
     */
    public String messageId() {
        return messageId;
    }

    /**
     * Returns the Amazon Resource Name (ARN) of the topic from which the message was published.
     *
     * @return The topic ARN (never null).
     */
    public String topicArn() {
        return topicArn;
    }

    /**
     * Returns the subject of the message, if provided.
     *
     * <p>This field is optional and may not be present in all message types.
     * It is commonly used in Notification messages to provide a brief description
     * of the message content.
     *
     * @return An Optional containing the subject, or empty if not present
     */
    public Optional<String> subject() {
        return Optional.ofNullable(subject);
    }

    /**
     * Returns the message content.
     * <p>
     * For Notification messages, this contains the actual notification content.
     * For confirmation messages, this may contain confirmation details.
     *
     * @return The message content (never null).
     */
    public String message() {
        return message;
    }

    /**
     * Returns the timestamp when the message was published.
     *
     * @return The message timestamp (never null).
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns the signature version used to sign the message.
     * <p>
     * Valid values are:
     * <ul>
     *   <li>"1" - SignatureVersion1 (SHA1)</li>
     *   <li>"2" - SignatureVersion2 (SHA256)</li>
     * </ul>
     *
     * @return The signature version (never null).
     */
    public String signatureVersion() {
        return signatureVersion;
    }

    /**
     * Returns the cryptographic signature of the message.
     *
     * @return The message signature (never null).
     */
    public String signature() {
        return signature;
    }

    /**
     * Returns the URL of the certificate used to sign the message.
     * <p>
     * This URL has been validated to ensure it comes from a trusted SNS-signed domain.
     *
     * @return The signing certificate URL (never null).
     */
    public String signingCertUrl() {
        return signingCertUrl;
    }

    /**
     * Returns the unsubscribe URL, if present.
     * <p>
     * This field is typically present in Notification messages and allows recipients
     * to unsubscribe from the topic.
     *
     * @return An Optional containing the unsubscribe URL, or empty if not present.
     */
    public Optional<String> unsubscribeUrl() {
        return Optional.ofNullable(unsubscribeUrl);
    }

    /**
     * Returns the token for subscription or unsubscribe confirmation, if present.
     * <p>
     * This field is required for SubscriptionConfirmation and UnsubscribeConfirmation messages.
     *
     * @return An Optional containing the token, or empty if not present.
     */
    public Optional<String> token() {
        return Optional.ofNullable(token);
    }

    /**
     * Returns the message attributes, if any.
     * <p>
     * Message attributes are key-value pairs that provide additional metadata about the message.
     *
     * @return A map of message attributes (never null, but may be empty).
     */
    public Map<String, String> messageAttributes() {
        return messageAttributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SnsMessage that = (SnsMessage) obj;
        return Objects.equals(type, that.type) &&
               Objects.equals(messageId, that.messageId) &&
               Objects.equals(topicArn, that.topicArn) &&
               Objects.equals(subject, that.subject) &&
               Objects.equals(message, that.message) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(signatureVersion, that.signatureVersion) &&
               Objects.equals(signature, that.signature) &&
               Objects.equals(signingCertUrl, that.signingCertUrl) &&
               Objects.equals(unsubscribeUrl, that.unsubscribeUrl) &&
               Objects.equals(token, that.token) &&
               Objects.equals(messageAttributes, that.messageAttributes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(messageId);
        result = 31 * result + Objects.hashCode(topicArn);
        result = 31 * result + Objects.hashCode(subject);
        result = 31 * result + Objects.hashCode(message);
        result = 31 * result + Objects.hashCode(timestamp);
        result = 31 * result + Objects.hashCode(signatureVersion);
        result = 31 * result + Objects.hashCode(signature);
        result = 31 * result + Objects.hashCode(signingCertUrl);
        result = 31 * result + Objects.hashCode(unsubscribeUrl);
        result = 31 * result + Objects.hashCode(token);
        result = 31 * result + Objects.hashCode(messageAttributes);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("SnsMessage")
                      .add("type", type)
                      .add("messageId", messageId)
                      .add("topicArn", topicArn)
                      .add("subject", subject)
                      .add("timestamp", timestamp)
                      .add("signatureVersion", signatureVersion)
                      .add("hasSignature", signature != null)
                      .add("signingCertUrl", signingCertUrl)
                      .add("hasUnsubscribeUrl", unsubscribeUrl != null)
                      .add("hasToken", token != null)
                      .add("messageAttributesCount", messageAttributes.size())
                      .build();
    }

    /**
     * Builder for creating SnsMessage instances.
     */
    public static final class Builder {
        private String type;
        private String messageId;
        private String topicArn;
        private String subject;
        private String message;
        private Instant timestamp;
        private String signatureVersion;
        private String signature;
        private String signingCertUrl;
        private String unsubscribeUrl;
        private String token;
        private Map<String, String> messageAttributes;

        private Builder() {
        }

        /**
         * Sets the message type.
         *
         * @param type The message type.
         * @return This builder for method chaining.
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the message ID.
         *
         * @param messageId The unique message identifier.
         * @return This builder for method chaining.
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the topic ARN.
         *
         * @param topicArn The Amazon Resource Name of the topic.
         * @return This builder for method chaining.
         */
        public Builder topicArn(String topicArn) {
            this.topicArn = topicArn;
            return this;
        }

        /**
         * Sets the message subject.
         *
         * @param subject The message subject (optional).
         * @return This builder for method chaining.
         */
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Sets the message content.
         *
         * @param message The message content.
         * @return This builder for method chaining.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the message timestamp.
         *
         * @param timestamp The timestamp when the message was published.
         * @return This builder for method chaining.
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the signature version.
         *
         * @param signatureVersion The signature version used to sign the message.
         * @return This builder for method chaining.
         */
        public Builder signatureVersion(String signatureVersion) {
            this.signatureVersion = signatureVersion;
            return this;
        }

        /**
         * Sets the message signature.
         *
         * @param signature The cryptographic signature of the message.
         * @return This builder for method chaining.
         */
        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        /**
         * Sets the signing certificate URL.
         *
         * @param signingCertUrl The URL of the certificate used to sign the message.
         * @return This builder for method chaining.
         */
        public Builder signingCertUrl(String signingCertUrl) {
            this.signingCertUrl = signingCertUrl;
            return this;
        }

        /**
         * Sets the unsubscribe URL.
         *
         * @param unsubscribeUrl The unsubscribe URL (optional).
         * @return This builder for method chaining.
         */
        public Builder unsubscribeUrl(String unsubscribeUrl) {
            this.unsubscribeUrl = unsubscribeUrl;
            return this;
        }

        /**
         * Sets the confirmation token.
         *
         * @param token The token for subscription or unsubscribe confirmation (optional).
         * @return This builder for method chaining.
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Sets the message attributes.
         *
         * @param messageAttributes A map of message attributes.
         * @return This builder for method chaining.
         */
        public Builder messageAttributes(Map<String, String> messageAttributes) {
            this.messageAttributes = messageAttributes;
            return this;
        }

        /**
         * Builds a new SnsMessage instance.
         *
         * @return A new SnsMessage with the configured properties.
         * @throws IllegalArgumentException if any required field is null.
         */
        public SnsMessage build() {
            return new SnsMessage(this);
        }
    }
}
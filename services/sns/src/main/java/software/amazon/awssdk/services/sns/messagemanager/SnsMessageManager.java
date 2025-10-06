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

import java.io.InputStream;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.sns.internal.messagemanager.DefaultSnsMessageManager;
import software.amazon.awssdk.utils.SdkAutoCloseable;


/**
 * Message manager for validating SNS message signatures. Create an instance using {@link #builder()}.
 * <p>
 * This manager provides automatic validation of SNS message signatures received via HTTP/HTTPS endpoints,
 * ensuring that messages are genuinely from Amazon SNS and have not been tampered with during transmission.
 * It supports both SignatureVersion1 (SHA1) and SignatureVersion2 (SHA256) as per AWS SNS standards.
 * <p>
 * The manager handles certificate retrieval, caching, and validation automatically, supporting different
 * AWS regions and partitions (aws, aws-gov, aws-cn).
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * SnsMessageManager messageManager = SnsMessageManager.builder().build();
 * 
 * try {
 *     SnsMessage validatedMessage = messageManager.parseMessage(messageBody);
 *     String messageContent = validatedMessage.message();
 *     String topicArn = validatedMessage.topicArn();
 *     // Process the validated message
 * } catch (SnsMessageValidationException e) {
 *     // Handle validation failure
 *     logger.error("SNS message validation failed: {}", e.getMessage());
 * }
 * }
 * </pre>
 */
@SdkPublicApi
public interface SnsMessageManager extends SdkAutoCloseable {

    /**
     * Creates a builder for configuring and creating an {@link SnsMessageManager}.
     *
     * @return A new builder.
     */
    static Builder builder() {
        return DefaultSnsMessageManager.builder();
    }

    /**
     * Parses and validates an SNS message from an InputStream.
     * <p>
     * This method reads the JSON message payload, validates the signature using AWS cryptographic verification,
     * and returns a parsed SNS message object with all message attributes if validation succeeds.
     *
     * @param messageStream The InputStream containing the JSON SNS message payload.
     * @return A validated {@link SnsMessage} object containing all message fields.
     * @throws SnsMessageValidationException If the message signature is invalid, the message format is malformed,
     *                                       or contains unexpected fields.
     * @throws NullPointerException If messageStream is null.
     */
    SnsMessage parseMessage(InputStream messageStream);

    /**
     * Parses and validates an SNS message from a String.
     * <p>
     * This method parses the JSON message payload, validates the signature using AWS cryptographic verification,
     * and returns a parsed SNS message object with all message attributes if validation succeeds.
     *
     * @param messageContent The String containing the JSON SNS message payload.
     * @return A validated {@link SnsMessage} object containing all message fields.
     * @throws SnsMessageValidationException If the message signature is invalid, the message format is malformed,
     *                                       or contains unexpected fields.
     * @throws NullPointerException If messageContent is null.
     */
    SnsMessage parseMessage(String messageContent);

    /**
     * Builder for creating and configuring an {@link SnsMessageManager}.
     */
    interface Builder {

        /**
         * Sets the configuration for the message manager.
         *
         * @param configuration The configuration to use.
         * @return This builder for method chaining.
         */
        Builder configuration(MessageManagerConfiguration configuration);

        /**
         * Sets the configuration for the message manager using a {@link Consumer} to configure the settings.
         *
         * @param configuration A {@link Consumer} to configure the {@link MessageManagerConfiguration}.
         * @return This builder for method chaining.
         */
        default Builder configuration(Consumer<MessageManagerConfiguration.Builder> configuration) {
            return configuration(MessageManagerConfiguration.builder().applyMutation(configuration).build());
        }

        /**
         * Builds an instance of {@link SnsMessageManager} based on the supplied configurations.
         *
         * @return An initialized SnsMessageManager.
         */
        SnsMessageManager build();
    }
}
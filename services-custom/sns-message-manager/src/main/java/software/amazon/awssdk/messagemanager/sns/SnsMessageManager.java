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

package software.amazon.awssdk.messagemanager.sns;

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.messagemanager.sns.internal.DefaultSnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.SdkAutoCloseable;


/**
 * Message manager for validating SNS message signatures. Create an instance using {@link #builder()}.
 *
 * <p>This manager provides automatic validation of SNS message signatures received via HTTP/HTTPS endpoints,
 * ensuring that messages originate from Amazon SNS and have not been modified during transmission.
 * It supports both SignatureVersion1 (SHA1) and SignatureVersion2 (SHA256) as per AWS SNS standards. By default both
 * versions are accepted. Use {@link Builder#minimumSignatureVersion(SignatureVersion)} to reject messages that declare a
 * lower signature version than your SNS topics use.
 *
 * <p>The manager handles certificate retrieval, caching, and validation automatically, supporting different
 * AWS regions and partitions (aws, aws-gov, aws-cn).
 *
 * <p>Basic usage with default configuration:
 * <pre>
 * {@code
 * SnsMessageManager messageManager = SnsMessageManager.builder().build();
 *
 * try {
 *     SnsMessage validatedMessage = messageManager.parseMessage(messageBody);
 *     String messageContent = validatedMessage.message();
 *     String topicArn = validatedMessage.topicArn();
 *     // Process the validated message
 * } catch (SdkClientException e) {
 *     // Handle validation failure
 *     logger.error("SNS message validation failed: {}", e.getMessage());
 * }
 * }
 * </pre>
 *
 * <p>Advanced usage with custom HTTP client:
 * <pre>
 * {@code
 * SnsMessageManager messageManager = SnsMessageManager.builder()
 *     .httpClient(ApacheHttpClient.create())
 *     .build();
 * }
 * </pre>
 *
 * <p>Usage for SNS topics configured with SignatureVersion 2, rejecting messages that declare SignatureVersion 1:
 * <pre>
 * {@code
 * SnsMessageManager messageManager = SnsMessageManager.builder()
 *     .minimumSignatureVersion(SignatureVersion.VERSION_2)
 *     .build();
 * }
 * </pre>
 *
 * @see SnsMessage
 * @see Builder
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
     * Parses and validates an SNS message from a stream.
     * <p>
     * This method reads the JSON message payload, validates the signature, returns a parsed SNS message object with all
     * message attributes if validation succeeds.
     *
     * @param messageStream The binary stream representation of the SNS message.
     * @return The parsed SNS message.
     */
    SnsMessage parseMessage(InputStream messageStream);

    /**
     * Parses and validates an SNS message from a string.
     * <p>
     * This method reads the JSON message payload, validates the signature, returns a parsed SNS message object with all
     * message attributes if validation succeeds.
     *
     * @param messageContent The string representation of the SNS message.
     * @return the parsed SNS message.
     */
    SnsMessage parseMessage(String messageContent);

    /**
     * Close this {@code SnsMessageManager}, releasing any resources it owned.
     * <p>
     * <b>Note:</b> if you provided your own {@link SdkHttpClient}, you must close it separately.
     */
    @Override
    void close();

    interface Builder {

        /**
         * Sets the HTTP client to use for certificate retrieval. The caller is responsible for closing this HTTP client after
         * the {@code SnsMessageManager} is closed.
         *
         * @param httpClient The HTTP client to use for fetching signing certificates.
         * @return This builder for method chaining.
         */
        Builder httpClient(SdkHttpClient httpClient);

        /**
         * Sets the AWS region for certificate validation. This region must match the SNS region where the messages originate.
         * This is a required parameter.
         *
         * @param region The AWS region where the SNS messages originate.
         * @return This builder for method chaining.
         */
        Builder region(Region region);

        /**
         * Sets the minimum SNS signature version this manager accepts. Messages that declare a lower SignatureVersion are
         * rejected with {@link software.amazon.awssdk.core.exception.SdkClientException} before the signing certificate is
         * retrieved or the signature is checked.
         * <p>
         * Defaults to {@link SignatureVersion#VERSION_1}, which accepts both SignatureVersion 1 (SHA1withRSA) and
         * SignatureVersion 2 (SHA256withRSA). If the SNS topics you subscribe to are configured with SignatureVersion 2,
         * set this to {@link SignatureVersion#VERSION_2} so that a message cannot be downgraded to SHA1withRSA by
         * declaring SignatureVersion 1.
         *
         * @param minimumSignatureVersion {@link SignatureVersion#VERSION_1} or {@link SignatureVersion#VERSION_2};
         * {@link SignatureVersion#UNKNOWN} is not allowed.
         * @return This builder for method chaining.
         */
        Builder minimumSignatureVersion(SignatureVersion minimumSignatureVersion);

        /**
         * Builds an instance of {@link SnsMessageManager} based on the supplied configurations.
         *
         * @return An initialized SnsMessageManager ready to validate SNS messages.
         */
        SnsMessageManager build();
    }
}
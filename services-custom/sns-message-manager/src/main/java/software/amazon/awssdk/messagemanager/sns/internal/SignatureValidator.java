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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.StringJoiner;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.messagemanager.sns.model.SnsUnsubscribeConfirmation;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * See
 * <a href="https://docs.aws.amazon.com/sns/latest/dg/sns-verify-signature-of-message-verify-message-signature.html">
 *     The official documentation.</a>
 */
@SdkInternalApi
public final class SignatureValidator {
    private static final Logger LOG = Logger.loggerFor(SignatureValidator.class);

    private static final String MESSAGE = "Message";
    private static final String MESSAGE_ID = "MessageId";
    private static final String SUBJECT = "Subject";
    private static final String SUBSCRIBE_URL = "SubscribeURL";
    private static final String TIMESTAMP = "Timestamp";
    private static final String TOKEN = "Token";
    private static final String TOPIC_ARN = "TopicArn";
    private static final String TYPE = "Type";

    private static final String NEWLINE = "\n";

    public void validateSignature(SnsMessage message, PublicKey publicKey) {
        Validate.paramNotNull(message, "message");
        Validate.paramNotNull(publicKey, "publicKey");

        SdkBytes messageSignature = message.signature();
        if (messageSignature == null) {
            throw SdkClientException.create("Message signature cannot be null");
        }

        SignatureVersion signatureVersion = message.signatureVersion();
        if (signatureVersion == null) {
            throw SdkClientException.create("Message signature version cannot be null");
        }

        if (message.timestamp() == null) {
            throw SdkClientException.create("Message timestamp cannot be null");
        }

        String canonicalMessage = buildCanonicalMessage(message);
        LOG.debug(() -> String.format("Canonical message: %s%n", canonicalMessage));

        Signature signature = getSignature(signatureVersion);

        verifySignature(canonicalMessage, messageSignature, publicKey, signature);
    }

    private static String buildCanonicalMessage(SnsMessage message) {
        switch (message.type()) {
            case NOTIFICATION:
                return buildCanonicalMessage((SnsNotification) message);
            case SUBSCRIPTION_CONFIRMATION:
                return buildCanonicalMessage((SnsSubscriptionConfirmation) message);
            case UNSUBSCRIBE_CONFIRMATION:
                return buildCanonicalMessage((SnsUnsubscribeConfirmation) message);
            default:
                throw new IllegalStateException(String.format("Unsupported SNS message type: %s", message.type()));
        }
    }

    private static String buildCanonicalMessage(SnsNotification notification) {
        StringJoiner joiner = new StringJoiner(NEWLINE, "", NEWLINE);
        joiner.add(MESSAGE).add(notification.message());
        joiner.add(MESSAGE_ID).add(notification.messageId());

        if (notification.subject() != null) {
            joiner.add(SUBJECT).add(notification.subject());
        }

        joiner.add(TIMESTAMP).add(notification.timestamp().toString());
        joiner.add(TOPIC_ARN).add(notification.topicArn());
        joiner.add(TYPE).add(notification.type().toString());

        return joiner.toString();
    }

    // Message, MessageId, SubscribeURL, Timestamp, Token, TopicArn, and Type.
    private static String buildCanonicalMessage(SnsSubscriptionConfirmation message) {
        StringJoiner joiner = new StringJoiner(NEWLINE, "", NEWLINE);
        joiner.add(MESSAGE).add(message.message());
        joiner.add(MESSAGE_ID).add(message.messageId());
        joiner.add(SUBSCRIBE_URL).add(message.subscribeUrl().toString());
        joiner.add(TIMESTAMP).add(message.timestamp().toString());
        joiner.add(TOKEN).add(message.token());
        joiner.add(TOPIC_ARN).add(message.topicArn());
        joiner.add(TYPE).add(message.type().toString());

        return joiner.toString();
    }

    private static String buildCanonicalMessage(SnsUnsubscribeConfirmation message) {
        StringJoiner joiner = new StringJoiner(NEWLINE, "", NEWLINE);
        joiner.add(MESSAGE).add(message.message());
        joiner.add(MESSAGE_ID).add(message.messageId());
        joiner.add(SUBSCRIBE_URL).add(message.subscribeUrl().toString());
        joiner.add(TIMESTAMP).add(message.timestamp().toString());
        joiner.add(TOKEN).add(message.token());
        joiner.add(TOPIC_ARN).add(message.topicArn());
        joiner.add(TYPE).add(message.type().toString());

        return joiner.toString();
    }

    private static void verifySignature(String canonicalMessage, SdkBytes messageSignature, PublicKey publicKey,
                                        Signature signature) {

        try {
            signature.initVerify(publicKey);
            signature.update(canonicalMessage.getBytes(StandardCharsets.UTF_8));

            boolean isValid = signature.verify(messageSignature.asByteArray());

            if (!isValid) {
                throw SdkClientException.create("The computed signature did not match the expected signature");
            }
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("The public key is invalid", e);
        } catch (SignatureException e) {
            throw SdkClientException.create("The signature is invalid", e);
        }
    }

    private static Signature getSignature(SignatureVersion signatureVersion) {
        try {
            switch (signatureVersion) {
                case VERSION_1:
                    return Signature.getInstance("SHA1withRSA");
                case VERSION_2:
                    return Signature.getInstance("SHA256withRSA");
                default:
                    throw new IllegalArgumentException("Unsupported signature version: " + signatureVersion);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to create Signature for " + signatureVersion, e);
        }
    }
}
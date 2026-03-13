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

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessageType;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.messagemanager.sns.model.SnsUnsubscribeConfirmation;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.BinaryUtils;

@SdkInternalApi
public class SnsMessageUnmarshaller {
    private static final String MESSAGE_FIELD = "Message";
    private static final String MESSAGE_ID_FIELD = "MessageId";
    private static final String SIGNATURE_FIELD = "Signature";
    private static final String SIGNATURE_VERSION_FIELD = "SignatureVersion";
    private static final String SIGNING_CERT_URL = "SigningCertURL";
    private static final String SUBJECT_FIELD = "Subject";
    private static final String SUBSCRIBE_URL = "SubscribeURL";
    private static final String TOKEN_FIELD = "Token";
    private static final String TOPIC_ARN_FIELD = "TopicArn";
    private static final String TIMESTAMP_FIELD = "Timestamp";
    private static final String TYPE_FIELD = "Type";
    private static final String UNSUBSCRIBE_URL_FIELD = "UnsubscribeURL";

    private static final JsonNodeParser PARSER = JsonNodeParser.builder()
        .removeErrorLocations(true)
        .build();

    public SnsMessage unmarshall(InputStream stream) {
        JsonNode node = PARSER.parse(stream);

        if (!node.isObject()) {
            throw SdkClientException.create("Expected an JSON object");
        }

        Optional<String> type = stringMember(node, TYPE_FIELD);

        if (!type.isPresent()) {
            throw SdkClientException.create("'Type' field must be present");
        }

        SnsMessageType snsMessageType = SnsMessageType.fromValue(type.get());
        switch (snsMessageType) {
            case NOTIFICATION: {
                SnsNotification.Builder builder = SnsNotification.builder();
                unmarshallNotification(node, builder);
                return builder.build();
            }
            case SUBSCRIPTION_CONFIRMATION: {
                SnsSubscriptionConfirmation.Builder builder = SnsSubscriptionConfirmation.builder();
                unmarshallSubscriptionConfirmation(node, builder);
                return builder.build();
            }
            case UNSUBSCRIBE_CONFIRMATION: {
                SnsUnsubscribeConfirmation.Builder builder = SnsUnsubscribeConfirmation.builder();
                unmarshallUnsubscribeNotification(node, builder);
                return builder.build();
            }
            default:
                throw SdkClientException.create("Unsupported sns message type: " + snsMessageType);
        }
    }

    private Optional<String> stringMember(JsonNode obj, String fieldName) {
        Optional<JsonNode> memberOptional = obj.field(fieldName);

        if (!memberOptional.isPresent()) {
            return Optional.empty();
        }

        JsonNode node = memberOptional.get();
        if (!node.isString()) {
            String msg = String.format("Expected field '%s' to be a string", fieldName);
            throw SdkClientException.create(msg);
        }

        return Optional.of(node.asString());
    }

    private void unmarshallNotification(JsonNode node, SnsNotification.Builder builder) {
        stringMember(node, SUBJECT_FIELD).ifPresent(builder::subject);
        stringMember(node, UNSUBSCRIBE_URL_FIELD)
            .map(SnsMessageUnmarshaller::toUri)
            .ifPresent(builder::unsubscribeUrl);

        unmarshallCommon(node, builder);
    }

    // https://docs.aws.amazon.com/sns/latest/dg/http-subscription-confirmation-json.html
    private void unmarshallSubscriptionConfirmation(JsonNode node, SnsSubscriptionConfirmation.Builder builder) {
        stringMember(node, TOKEN_FIELD).ifPresent(builder::token);
        stringMember(node, SUBSCRIBE_URL).map(SnsMessageUnmarshaller::toUri).ifPresent(builder::subscribeUrl);
        unmarshallCommon(node, builder);
    }

    // https://docs.aws.amazon.com/sns/latest/dg/http-unsubscribe-confirmation-json.html
    private void unmarshallUnsubscribeNotification(JsonNode node, SnsUnsubscribeConfirmation.Builder builder) {
        stringMember(node, TOKEN_FIELD).ifPresent(builder::token);
        stringMember(node, SUBSCRIBE_URL).map(SnsMessageUnmarshaller::toUri).ifPresent(builder::subscribeUrl);
        unmarshallCommon(node, builder);
    }

    private void unmarshallCommon(JsonNode node, SnsMessage.Builder<?> builder) {
        stringMember(node, MESSAGE_ID_FIELD).ifPresent(builder::messageId);
        stringMember(node, MESSAGE_FIELD).ifPresent(builder::message);
        stringMember(node, TOPIC_ARN_FIELD).ifPresent(builder::topicArn);

        Optional<String> timestamp = stringMember(node, TIMESTAMP_FIELD);
        if (timestamp.isPresent()) {
            try {
                Instant instant = Instant.parse(timestamp.get());
                builder.timestamp(instant);
            } catch (DateTimeParseException e) {
                throw SdkClientException.create("Unable to parse timestamp", e);
            }
        }

        Optional<String> signatureField = stringMember(node, SIGNATURE_FIELD);
        if (signatureField.isPresent()) {
            try {
                byte[] decoded = BinaryUtils.fromBase64(signatureField.get());
                builder.signature(SdkBytes.fromByteArray(decoded));
            } catch (IllegalArgumentException e) {
                throw SdkClientException.create("Unable to decode signature", e);
            }
        }

        stringMember(node, SIGNATURE_VERSION_FIELD)
            .map(SignatureVersion::fromValue)
            .ifPresent(builder::signatureVersion);

        stringMember(node, SIGNING_CERT_URL)
            .map(SnsMessageUnmarshaller::toUri)
            .ifPresent(builder::signingCertUrl);
    }

    private static URI toUri(String s) {
        try {
            return URI.create(s);
        } catch (IllegalArgumentException e) {
            throw SdkClientException.create("Unable to parse URI", e);
        }
    }
}
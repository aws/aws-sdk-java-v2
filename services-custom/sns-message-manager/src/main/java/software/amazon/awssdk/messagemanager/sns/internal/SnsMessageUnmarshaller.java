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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessageType;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.messagemanager.sns.model.SnsUnsubscribeConfirmation;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonProcessingException;
import software.amazon.awssdk.thirdparty.jackson.core.JsonToken;
import software.amazon.awssdk.thirdparty.jackson.core.json.JsonReadFeature;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Converts the JSON body of an SNS HTTP(S) delivery into an {@link SnsMessage}.
 *
 * <p>Production code in this repository usually parses JSON with {@code JsonNodeParser}. This class uses the shaded
 * jackson-core streaming API ({@link JsonParser}; no databind, no {@code JsonNode}) directly and reads only the fields
 * used by the message manager; everything else is skipped.
 */
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

    /**
     * Mirrors {@code JsonNodeParser.DEFAULT_JSON_FACTORY} with two deliberate deviations.
     *
     * <ul>
     *     <li>{@code StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION} is not enabled and
     *     {@link JsonProcessingException#clearLocation()} is called before any parser exception is wrapped. Together these
     *     match {@code JsonNodeParser.builder().removeErrorLocations(true)}: exception messages never echo the body.</li>
     *     <li>{@code CANONICALIZE_FIELD_NAMES} is off (which also disables {@code INTERN_FIELD_NAMES}). Field names are
     *     not interned.</li>
     * </ul>
     */
    private static final JsonFactory JSON_FACTORY =
        JsonFactory.builder()
                   .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                   .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
                   .build();

    public SnsMessage unmarshall(InputStream stream) {
        Validate.paramNotNull(stream, "stream");
        try (JsonParser parser = JSON_FACTORY.createParser(stream)
                                             .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)) {
            return unmarshall(parser);
        } catch (IOException e) {
            throw parseFailure(e);
        }
    }

    public SnsMessage unmarshall(String content) {
        Validate.paramNotNull(content, "content");
        try (JsonParser parser = JSON_FACTORY.createParser(content)
                                             .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)) {
            return unmarshall(parser);
        } catch (IOException e) {
            throw parseFailure(e);
        }
    }

    private static SdkClientException parseFailure(IOException e) {
        if (e instanceof JsonProcessingException) {
            ((JsonProcessingException) e).clearLocation();
        }
        return SdkClientException.create("Unable to parse SNS message as JSON", e);
    }

    private SnsMessage unmarshall(JsonParser parser) throws IOException {
        JsonToken first = parser.nextToken();
        if (first == null) {
            throw SdkClientException.create("Expected a JSON object but the message was empty");
        }
        if (first != JsonToken.START_OBJECT) {
            throw SdkClientException.create("Expected a JSON object");
        }

        RawFields raw = new RawFields();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.currentName();
            JsonToken value = parser.nextToken();
            switch (name) {
                case TYPE_FIELD:
                    raw.type = stringValue(parser, value, name);
                    break;
                case MESSAGE_ID_FIELD:
                    raw.messageId = stringValue(parser, value, name);
                    break;
                case TOPIC_ARN_FIELD:
                    raw.topicArn = stringValue(parser, value, name);
                    break;
                case MESSAGE_FIELD:
                    raw.message = stringValue(parser, value, name);
                    break;
                case SUBJECT_FIELD:
                    raw.subject = stringValue(parser, value, name);
                    break;
                case TIMESTAMP_FIELD:
                    raw.timestamp = stringValue(parser, value, name);
                    break;
                case SIGNATURE_VERSION_FIELD:
                    raw.signatureVersion = stringValue(parser, value, name);
                    break;
                case SIGNATURE_FIELD:
                    raw.signature = stringValue(parser, value, name);
                    break;
                case SIGNING_CERT_URL:
                    raw.signingCertUrl = stringValue(parser, value, name);
                    break;
                case UNSUBSCRIBE_URL_FIELD:
                    raw.unsubscribeUrl = stringValue(parser, value, name);
                    break;
                case SUBSCRIBE_URL:
                    raw.subscribeUrl = stringValue(parser, value, name);
                    break;
                case TOKEN_FIELD:
                    raw.token = stringValue(parser, value, name);
                    break;
                default:
                    // Anything not in the model (including MessageAttributes) is skipped. skipChildren() is a no-op for
                    // scalar values.
                    parser.skipChildren();
                    break;
            }
        }
        // Content after the top-level object is not read, matching the previous tree parser.

        return toMessage(raw);
    }

    /**
     * Returns the text of a known field, which must be a JSON string. A JSON {@code null} is rejected like any other
     * non-string value. When a key repeats, the caller stores the last occurrence, matching the previous tree parser.
     */
    private static String stringValue(JsonParser parser, JsonToken value, String fieldName) throws IOException {
        if (value != JsonToken.VALUE_STRING) {
            String msg = String.format("Expected field '%s' to be a string", fieldName);
            throw SdkClientException.create(msg);
        }
        return parser.getText();
    }

    private SnsMessage toMessage(RawFields raw) {
        if (raw.type == null) {
            throw SdkClientException.create("'Type' field must be present");
        }

        SnsMessageType snsMessageType = SnsMessageType.fromValue(raw.type);
        switch (snsMessageType) {
            case NOTIFICATION: {
                SnsNotification.Builder builder = SnsNotification.builder();
                unmarshallNotification(raw, builder);
                return builder.build();
            }
            case SUBSCRIPTION_CONFIRMATION: {
                SnsSubscriptionConfirmation.Builder builder = SnsSubscriptionConfirmation.builder();
                unmarshallSubscriptionConfirmation(raw, builder);
                return builder.build();
            }
            case UNSUBSCRIBE_CONFIRMATION: {
                SnsUnsubscribeConfirmation.Builder builder = SnsUnsubscribeConfirmation.builder();
                unmarshallUnsubscribeNotification(raw, builder);
                return builder.build();
            }
            default:
                throw SdkClientException.create("Unsupported sns message type: " + snsMessageType);
        }
    }

    private void unmarshallNotification(RawFields raw, SnsNotification.Builder builder) {
        if (raw.subject != null) {
            builder.subject(raw.subject);
        }
        if (raw.unsubscribeUrl != null) {
            builder.unsubscribeUrl(toUri(raw.unsubscribeUrl));
        }

        unmarshallCommon(raw, builder);
    }

    // https://docs.aws.amazon.com/sns/latest/dg/http-subscription-confirmation-json.html
    private void unmarshallSubscriptionConfirmation(RawFields raw, SnsSubscriptionConfirmation.Builder builder) {
        if (raw.token != null) {
            builder.token(raw.token);
        }
        if (raw.subscribeUrl != null) {
            builder.subscribeUrl(toUri(raw.subscribeUrl));
        }
        unmarshallCommon(raw, builder);
    }

    // https://docs.aws.amazon.com/sns/latest/dg/http-unsubscribe-confirmation-json.html
    private void unmarshallUnsubscribeNotification(RawFields raw, SnsUnsubscribeConfirmation.Builder builder) {
        if (raw.token != null) {
            builder.token(raw.token);
        }
        if (raw.subscribeUrl != null) {
            builder.subscribeUrl(toUri(raw.subscribeUrl));
        }
        unmarshallCommon(raw, builder);
    }

    private void unmarshallCommon(RawFields raw, SnsMessage.Builder<?> builder) {
        if (raw.messageId != null) {
            builder.messageId(raw.messageId);
        }
        if (raw.message != null) {
            builder.message(raw.message);
        }
        if (raw.topicArn != null) {
            builder.topicArn(raw.topicArn);
        }

        if (raw.timestamp != null) {
            try {
                Instant instant = Instant.parse(raw.timestamp);
                builder.timestamp(instant);
            } catch (DateTimeParseException e) {
                throw SdkClientException.create("Unable to parse timestamp", e);
            }
        }

        if (raw.signature != null) {
            try {
                byte[] decoded = BinaryUtils.fromBase64(raw.signature);
                builder.signature(SdkBytes.fromByteArray(decoded));
            } catch (IllegalArgumentException e) {
                throw SdkClientException.create("Unable to decode signature", e);
            }
        }

        if (raw.signatureVersion != null) {
            builder.signatureVersion(SignatureVersion.fromValue(raw.signatureVersion));
        }

        if (raw.signingCertUrl != null) {
            builder.signingCertUrl(toUri(raw.signingCertUrl));
        }
    }

    private static URI toUri(String s) {
        try {
            return URI.create(s);
        } catch (IllegalArgumentException e) {
            throw SdkClientException.create("Unable to parse URI", e);
        }
    }

    /**
     * The known SNS fields as read from the body, before conversion. Fields irrelevant to the message type (for example
     * {@code Token} on a Notification) are collected but not applied.
     */
    private static final class RawFields {
        private String type;
        private String messageId;
        private String topicArn;
        private String message;
        private String subject;
        private String timestamp;
        private String signatureVersion;
        private String signature;
        private String signingCertUrl;
        private String unsubscribeUrl;
        private String subscribeUrl;
        private String token;
    }
}

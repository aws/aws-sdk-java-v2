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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessageType;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.messagemanager.sns.model.SnsUnsubscribeConfirmation;
import software.amazon.awssdk.utils.BinaryUtils;

public class SnsMessageUnmarshallerTest {
    private static final SnsMessageUnmarshaller UNMARSHALLER = new SnsMessageUnmarshaller();
    private static final byte[] TEST_SIGNATURE = BinaryUtils.fromBase64("k764G/ur2Ng=");
    private static final SignatureVersion TEST_SIGNATURE_VERSION = SignatureVersion.VERSION_1;
    private static final String TEST_TOPIC_ARN = "arn:aws:sns:us-west-2:123456789012:MyTopic";
    private static final String TEST_MESSAGE_ID = "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324";
    private static final String TEST_MESSAGE = "Hello world!";
    private static final String TEST_SUBJECT = "My First Message";
    private static final URI TEST_SIGNING_CERT_URL = URI.create("https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem");
    private static final URI TEST_SUBSCRIBE_URL = URI.create("https://sns.us-west-2.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-west-2:123456789012:MyTopic&Token=token");
    private static final URI TEST_UNSUBSCRIBE_URL = URI.create("https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96");
    private static final Instant TEST_TIMESTAMP = Instant.parse("2012-05-02T00:54:06.655Z");
    private static final String TEST_TOKEN = "token";

    private static final String RESOURCE_ROOT = "/software/amazon/awssdk/messagemanager/sns/internal/";
    private static final String FIXTURE_SIGNING_CERT_URL =
        "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-7506a1e35b36ef5a444dd1a8e7cc3ed8.pem";
    private static final String FIXTURE_TOPIC_ARN = "arn:aws:sns:us-west-2:131990247566:dongie-standard-topic";

    @Test
    void unmarshall_notification_maximal() {
        String json = "{\n"
                      + "  \"Type\" : \"Notification\",\n"
                      + "  \"MessageId\" : \"22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324\",\n"
                      + "  \"TopicArn\" : \"arn:aws:sns:us-west-2:123456789012:MyTopic\",\n"
                      + "  \"Subject\" : \"My First Message\",\n"
                      + "  \"Message\" : \"Hello world!\",\n"
                      + "  \"Timestamp\" : \"2012-05-02T00:54:06.655Z\",\n"
                      + "  \"SignatureVersion\" : \"1\",\n"
                      + "  \"Signature\" : \"k764G/ur2Ng=\",\n"
                      + "  \"SigningCertURL\" : \"https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem\",\n"
                      + "  \"UnsubscribeURL\" : \"https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96\"\n"
                      + "}";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg).isInstanceOf(SnsNotification.class);

        SnsNotification notification = (SnsNotification) msg;

        assertThat(notification.type()).isEqualTo(SnsMessageType.NOTIFICATION);
        assertThat(notification.messageId()).isEqualTo(TEST_MESSAGE_ID);
        assertThat(notification.message()).isEqualTo(TEST_MESSAGE);
        assertThat(notification.topicArn()).isEqualTo(TEST_TOPIC_ARN);
        assertThat(notification.signature().asByteArray()).isEqualTo(TEST_SIGNATURE);
        assertThat(notification.signatureVersion()).isEqualTo(TEST_SIGNATURE_VERSION);
        assertThat(notification.subject()).isEqualTo(TEST_SUBJECT);
        assertThat(notification.timestamp()).isEqualTo(TEST_TIMESTAMP);
        assertThat(notification.signingCertUrl()).isEqualTo(TEST_SIGNING_CERT_URL);
        assertThat(notification.unsubscribeUrl()).isEqualTo(TEST_UNSUBSCRIBE_URL);
    }

    @Test
    void unmarshall_subscriptionNotification_maximal() {
        String json = "{\n"
                      + "  \"Type\" : \"SubscriptionConfirmation\",\n"
                      + "  \"MessageId\" : \"22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324\",\n"
                      + "  \"Token\" : \"token\",\n"
                      + "  \"TopicArn\" : \"arn:aws:sns:us-west-2:123456789012:MyTopic\",\n"
                      + "  \"Message\" : \"Hello world!\",\n"
                      + "  \"SubscribeURL\" : \"https://sns.us-west-2.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-west-2:123456789012:MyTopic&Token=token\",\n"
                      + "  \"Timestamp\" : \"2012-05-02T00:54:06.655Z\",\n"
                      + "  \"SignatureVersion\" : \"1\",\n"
                      + "  \"Signature\" : \"k764G/ur2Ng=\",\n"
                      + "  \"SigningCertURL\" : \"https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem\"\n"
                      + "}";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg).isInstanceOf(SnsSubscriptionConfirmation.class);

        SnsSubscriptionConfirmation subscriptionConfirmation = (SnsSubscriptionConfirmation) msg;

        assertThat(subscriptionConfirmation.type()).isEqualTo(SnsMessageType.SUBSCRIPTION_CONFIRMATION);
        assertThat(subscriptionConfirmation.messageId()).isEqualTo(TEST_MESSAGE_ID);
        assertThat(subscriptionConfirmation.token()).isEqualTo(TEST_TOKEN);
        assertThat(subscriptionConfirmation.topicArn()).isEqualTo(TEST_TOPIC_ARN);
        assertThat(subscriptionConfirmation.message()).isEqualTo(TEST_MESSAGE);
        assertThat(subscriptionConfirmation.subscribeUrl()).isEqualTo(TEST_SUBSCRIBE_URL);
        assertThat(subscriptionConfirmation.timestamp()).isEqualTo(TEST_TIMESTAMP);
        assertThat(subscriptionConfirmation.signatureVersion()).isEqualTo(TEST_SIGNATURE_VERSION);
        assertThat(subscriptionConfirmation.signature().asByteArray()).isEqualTo(TEST_SIGNATURE);
        assertThat(subscriptionConfirmation.signingCertUrl()).isEqualTo(TEST_SIGNING_CERT_URL);
    }

    @Test
    void unmarshall_unsubscribeConfirmation_maximal() {
        String json = "{\n"
                      + "  \"Type\" : \"UnsubscribeConfirmation\",\n"
                      + "  \"MessageId\" : \"22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324\",\n"
                      + "  \"Token\" : \"token\",\n"
                      + "  \"TopicArn\" : \"arn:aws:sns:us-west-2:123456789012:MyTopic\",\n"
                      + "  \"Message\" : \"Hello world!\",\n"
                      + "  \"SubscribeURL\" : \"https://sns.us-west-2.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-west-2:123456789012:MyTopic&Token=token\",\n"
                      + "  \"Timestamp\" : \"2012-05-02T00:54:06.655Z\",\n"
                      + "  \"SignatureVersion\" : \"1\",\n"
                      + "  \"Signature\" : \"k764G/ur2Ng=\",\n"
                      + "  \"SigningCertURL\" : \"https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem\"\n"
                      + "}";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg).isInstanceOf(SnsUnsubscribeConfirmation.class);

        SnsUnsubscribeConfirmation unsubscribeConfirmation = (SnsUnsubscribeConfirmation) msg;

        assertThat(unsubscribeConfirmation.type()).isEqualTo(SnsMessageType.UNSUBSCRIBE_CONFIRMATION);
        assertThat(unsubscribeConfirmation.messageId()).isEqualTo(TEST_MESSAGE_ID);
        assertThat(unsubscribeConfirmation.token()).isEqualTo(TEST_TOKEN);
        assertThat(unsubscribeConfirmation.topicArn()).isEqualTo(TEST_TOPIC_ARN);
        assertThat(unsubscribeConfirmation.message()).isEqualTo(TEST_MESSAGE);
        assertThat(unsubscribeConfirmation.subscribeUrl()).isEqualTo(TEST_SUBSCRIBE_URL);
        assertThat(unsubscribeConfirmation.timestamp()).isEqualTo(TEST_TIMESTAMP);
        assertThat(unsubscribeConfirmation.signatureVersion()).isEqualTo(TEST_SIGNATURE_VERSION);
        assertThat(unsubscribeConfirmation.signature().asByteArray()).isEqualTo(TEST_SIGNATURE);
        assertThat(unsubscribeConfirmation.signingCertUrl()).isEqualTo(TEST_SIGNING_CERT_URL);
    }

    @Test
    void unmarshall_unknownType_throws() {
        String json = "{\n"
                      + "  \"Type\" : \"MyCustomType\"\n"
                      + "}";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unsupported sns message type");
    }

    @Test
    void unmarshall_jsonNotObject_throws() {
        String json = "[]";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected a JSON object");
    }

    @Test
    void unmarshall_topLevelArray_rejectedAfterFirstToken() {
        StringBuilder sb = new StringBuilder("[{}");
        while (sb.length() < 1024 * 1024) {
            sb.append(",{}");
        }
        sb.append(']');
        CountingInputStream stream = new CountingInputStream(asStream(sb.toString()));

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(stream))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected a JSON object");

        // Jackson fills one 8000-byte buffer before producing the first token; nothing beyond that is read.
        assertThat(stream.count).isLessThanOrEqualTo(16 * 1024);
    }

    @Test
    void unmarshall_leadingWhitespace_succeeds() {
        String json = " \n\t{\"Type\":\"Notification\",\"MessageId\":\"id\",\"Message\":\"hello\"}";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg).isInstanceOf(SnsNotification.class);
        assertThat(msg.messageId()).isEqualTo("id");
    }

    @Test
    void unmarshall_emptyInput_throws() {
        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream("")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("message was empty");
    }

    @Test
    void unmarshall_malformedJson_throwsSdkClientException() {
        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream("{\"Type\":")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to parse SNS message as JSON");
    }

    @Test
    void unmarshall_malformedParseError_doesNotEchoBody() {
        String body = "{\"Type\": \"x\" \"oops\"}";

        SdkClientException e = catchThrowableOfType(() -> UNMARSHALLER.unmarshall(asStream(body)),
                                                    SdkClientException.class);

        assertThat(e).hasMessageContaining("Unable to parse SNS message as JSON");
        assertThat(e.getCause()).isInstanceOf(IOException.class);
        assertThat(e.getCause().getMessage()).doesNotContain(body);
        assertThat(e.getCause().getMessage()).doesNotContain("oops");
    }

    @Test
    void unmarshall_unknownFields_ignored() {
        String json = "{\n"
                      + "  \"Type\" : \"Notification\",\n"
                      + "  \"Nested\" : {\"a\":{\"b\":[1,2,{\"c\":null}]}},\n"
                      + "  \"MessageId\" : \"22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324\",\n"
                      + "  \"Array\" : [\"x\", 1, true, null, {}],\n"
                      + "  \"Number\" : 12.5,\n"
                      + "  \"TopicArn\" : \"arn:aws:sns:us-west-2:123456789012:MyTopic\",\n"
                      + "  \"Flag\" : false,\n"
                      + "  \"Nothing\" : null,\n"
                      + "  \"Message\" : \"Hello world!\",\n"
                      + "  \"Extra\" : \"string\",\n"
                      + "  \"MessageAttributes\" : {\"attr\":{\"Type\":\"String\",\"Value\":\"v\"}},\n"
                      + "  \"Subject\" : \"My First Message\"\n"
                      + "}";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg).isInstanceOf(SnsNotification.class);
        SnsNotification notification = (SnsNotification) msg;
        assertThat(notification.messageId()).isEqualTo(TEST_MESSAGE_ID);
        assertThat(notification.topicArn()).isEqualTo(TEST_TOPIC_ARN);
        assertThat(notification.message()).isEqualTo(TEST_MESSAGE);
        assertThat(notification.subject()).isEqualTo(TEST_SUBJECT);
    }

    @Test
    void unmarshall_duplicateKey_lastWins() {
        String json = "{\"Type\":\"Notification\",\"MessageId\":\"first\",\"MessageId\":\"second\"}";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg.messageId()).isEqualTo("second");
    }

    @Test
    void unmarshall_knownFieldNull_throws() {
        String json = "{\"Type\":\"Notification\",\"Subject\":null}";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected field 'Subject' to be a string");
    }

    @Test
    void unmarshall_knownFieldObject_throws() {
        String json = "{\"Type\":\"Notification\",\"Message\":{\"a\":1}}";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected field 'Message' to be a string");
    }

    @Test
    void unmarshall_trailingContentAfterObject_ignored() {
        String json = "{\"Type\":\"Notification\",\"MessageId\":\"id\"} trailing content [";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg.messageId()).isEqualTo("id");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "test-notification-no-subject.json",
        "test-notification-signature-v2.json",
        "test-notification-with-subject.json",
        "test-subscription-confirmation.json",
        "test-unsubscribe-confirmation.json"
    })
    void unmarshall_string_equalsInputStreamResult(String fixture) throws IOException {
        byte[] bytes = readResource(fixture);

        SnsMessage fromStream = UNMARSHALLER.unmarshall(new ByteArrayInputStream(bytes));
        SnsMessage fromString = UNMARSHALLER.unmarshall(new String(bytes, StandardCharsets.UTF_8));

        assertThat(fromString).isEqualTo(fromStream);
        assertThat(fromString.getClass()).isEqualTo(fromStream.getClass());
    }

    @ParameterizedTest
    @CsvSource({
        "test-notification-no-subject.json, NOTIFICATION, c0e5a280-53c9-5a98-93bb-c0a560b3d192, VERSION_1",
        "test-notification-signature-v2.json, NOTIFICATION, 9d2c5580-b61d-550c-8583-37a582870907, VERSION_2",
        "test-notification-with-subject.json, NOTIFICATION, 26858270-545e-5848-82a2-e05566af3dcf, VERSION_1",
        "test-subscription-confirmation.json, SUBSCRIPTION_CONFIRMATION, f3f2fc87-c426-4c2f-9044-b55890c0939c, VERSION_1",
        "test-unsubscribe-confirmation.json, UNSUBSCRIBE_CONFIRMATION, 0b71e34e-42c3-486a-9e32-b1928ec994b5, VERSION_2"
    })
    void unmarshall_signedFixtures_fieldsMatch(String fixture,
                                               SnsMessageType type,
                                               String messageId,
                                               SignatureVersion signatureVersion) throws IOException {
        SnsMessage msg = UNMARSHALLER.unmarshall(new ByteArrayInputStream(readResource(fixture)));

        assertThat(msg.type()).isEqualTo(type);
        assertThat(msg.messageId()).isEqualTo(messageId);
        assertThat(msg.topicArn()).isEqualTo(FIXTURE_TOPIC_ARN);
        assertThat(msg.signingCertUrl()).isEqualTo(URI.create(FIXTURE_SIGNING_CERT_URL));
        assertThat(msg.signatureVersion()).isEqualTo(signatureVersion);
        assertThat(msg.signature()).isNotNull();
        assertThat(msg.timestamp()).isNotNull();
    }

    @Test
    void unmarshall_manyUnknownKeys_completes() {
        String knownFields = "{\"Type\":\"Notification\",\"MessageId\":\"id\",\"TopicArn\":\"arn\",\"Message\":\"hello\"";
        UnknownKeyStream stream = new UnknownKeyStream(knownFields, 1024 * 1024);

        SnsMessage msg = UNMARSHALLER.unmarshall(stream);

        assertThat(msg).isInstanceOf(SnsNotification.class);
        assertThat(msg.messageId()).isEqualTo("id");
        assertThat(msg.topicArn()).isEqualTo("arn");
        assertThat(msg.message()).isEqualTo("hello");
        assertThat(stream.fullyRead()).isTrue();
    }

    private static InputStream asStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] readResource(String name) throws IOException {
        try (InputStream in = SnsMessageUnmarshallerTest.class.getResourceAsStream(RESOURCE_ROOT + name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static final class CountingInputStream extends FilterInputStream {
        private long count;

        CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int b = in.read();
            if (b != -1) {
                count++;
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = in.read(b, off, len);
            if (n > 0) {
                count += n;
            }
            return n;
        }
    }

    /**
     * Lazily emits {@code <prefix>,"k0":{"a":[1,2,{}]},"k1":...} until roughly {@code totalSize} bytes, then closes the
     * object. The filler is never held in memory.
     */
    private static final class UnknownKeyStream extends InputStream {
        private final long totalSize;
        private byte[] pending;
        private int pendingPos;
        private long emitted;
        private long produced;
        private boolean closed;

        UnknownKeyStream(String prefix, long totalSize) {
            this.totalSize = totalSize;
            this.pending = prefix.getBytes(StandardCharsets.UTF_8);
        }

        /**
         * True once every generated byte, including the closing brace, has been handed to the reader.
         */
        boolean fullyRead() {
            return closed && pendingPos >= pending.length;
        }

        private boolean refill() {
            if (closed) {
                return false;
            }
            if (produced >= totalSize) {
                pending = "}".getBytes(StandardCharsets.UTF_8);
                closed = true;
            } else {
                pending = (",\"k" + emitted + "\":{\"a\":[1,2,{}]}").getBytes(StandardCharsets.UTF_8);
                emitted++;
            }
            pendingPos = 0;
            return true;
        }

        @Override
        public int read() {
            if (pendingPos >= pending.length && !refill()) {
                return -1;
            }
            produced++;
            return pending[pendingPos++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (len == 0) {
                return 0;
            }
            int written = 0;
            while (written < len) {
                if (pendingPos >= pending.length && !refill()) {
                    break;
                }
                int n = Math.min(len - written, pending.length - pendingPos);
                System.arraycopy(pending, pendingPos, b, off + written, n);
                pendingPos += n;
                written += n;
                produced += n;
            }
            return written == 0 ? -1 : written;
        }
    }
}

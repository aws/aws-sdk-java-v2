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

import java.net.URI;
import java.security.PrivateKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.regions.Region;

/**
 * End-to-end checks of the field rules applied by {@code parseMessage}: signed fields other than {@code Message} must be
 * single-line.
 * <p>
 * The "SNS service" side (canonicalization and RSA-SHA256 signing) is performed here with a private key from
 * {@link TestSigningCertificates} whose self-signed certificate is served from memory by a {@link RecordingHttpClient};
 * everything else is the real module code.
 */
class DefaultSnsMessageManagerFieldValidationTest {
    private static final String SIGNING_CERT_URL =
        "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-1234567890abcdef.pem";
    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:order-events";
    private static final String TIMESTAMP = "2024-06-15T12:30:45.123Z";
    private static final String REAL_MESSAGE_ID = "8f1c2f2e-1111-4d4d-9e2b-9a0b1c2d3e4f";
    private static final String REAL_UNSUBSCRIBE_URL =
        "https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=" + TOPIC_ARN + ":sub-1";
    private static final String REAL_SUBSCRIBE_URL =
        "https://sns.us-east-1.amazonaws.com/?Action=ConfirmSubscription&TopicArn=" + TOPIC_ARN + "&Token=abc123";
    private static final String TOKEN = "2336412f37fb687f5d51e6e2425929f52e8e353134fe72d55ef8b330217d82da";

    private static final String MESSAGE = "Your order 1001 has shipped.";
    private static final String SUBJECT = "order-shipped";

    private static PrivateKey snsKey;

    private RecordingHttpClient httpClient;
    private SnsMessageManager manager;

    @BeforeAll
    static void loadKey() throws Exception {
        snsKey = TestSigningCertificates.privateKey(TestSigningCertificates.KEY_A_PKCS8_B64);
    }

    @BeforeEach
    void setUp() {
        httpClient = new RecordingHttpClient(TestSigningCertificates.pem(TestSigningCertificates.CERT_A_DER_B64));
        manager = SnsMessageManager.builder()
                                   .region(Region.US_EAST_1)
                                   .httpClient(httpClient)
                                   .build();
    }

    @AfterEach
    void tearDown() {
        manager.close();
    }

    @Test
    void parseMessage_notification_succeeds() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT));
        String json = notificationJson(MESSAGE, REAL_MESSAGE_ID, SUBJECT, REAL_UNSUBSCRIBE_URL, TIMESTAMP, signature);

        SnsMessage parsed = manager.parseMessage(json);

        assertThat(parsed).isInstanceOf(SnsNotification.class);
        SnsNotification notification = (SnsNotification) parsed;
        assertThat(notification.subject()).isEqualTo(SUBJECT);
        assertThat(notification.messageId()).isEqualTo(REAL_MESSAGE_ID);
        assertThat(notification.unsubscribeUrl()).isEqualTo(URI.create(REAL_UNSUBSCRIBE_URL));
        assertThat(httpClient.fetched()).hasSize(1);
    }

    @Test
    void parseMessage_messageIdContainsLineBreak_rejected() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT));
        String body = notificationJson(MESSAGE, REAL_MESSAGE_ID + "\nSubject\n" + SUBJECT, null, REAL_UNSUBSCRIBE_URL,
                                       TIMESTAMP, signature);

        assertThatThrownBy(() -> manager.parseMessage(body))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Signed field 'MessageId' must not contain line breaks");
        assertThat(httpClient.fetched()).isEmpty();
    }

    @Test
    void parseMessage_messageIdContainsLineBreak_messageTailMoved_rejected() throws Exception {
        String signature = sign(canonical("line one\nMessageId\nline two", "id-1", null));
        String body = notificationJson("line one", "line two\nMessageId\nid-1", null, REAL_UNSUBSCRIBE_URL, TIMESTAMP,
                                       signature);

        assertThatThrownBy(() -> manager.parseMessage(body))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Signed field 'MessageId' must not contain line breaks");
        assertThat(httpClient.fetched()).isEmpty();
    }

    /**
     * A Subject containing a line break is rejected before the certificate is fetched.
     */
    @Test
    void parseMessage_subjectContainsLineBreak_rejected() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT));
        String json = notificationJson(MESSAGE, REAL_MESSAGE_ID, SUBJECT + "\nsecond line", REAL_UNSUBSCRIBE_URL, TIMESTAMP,
                                       signature);

        assertThatThrownBy(() -> manager.parseMessage(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Signed field 'Subject' must not contain line breaks");
        assertThat(httpClient.fetched()).isEmpty();
    }

    @Test
    void parseMessage_modifiedBody_rejected() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT));
        String json = notificationJson(MESSAGE + " (edited)", REAL_MESSAGE_ID, SUBJECT, REAL_UNSUBSCRIBE_URL, TIMESTAMP,
                                       signature);

        assertThatThrownBy(() -> manager.parseMessage(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("did not match");
    }

    @Test
    void parseMessage_modifiedTimestamp_rejected() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT));
        String json = notificationJson(MESSAGE, REAL_MESSAGE_ID, SUBJECT, REAL_UNSUBSCRIBE_URL, "2024-06-15T12:31:45.123Z",
                                       signature);

        assertThatThrownBy(() -> manager.parseMessage(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("did not match");
    }

    @Test
    void parseMessage_multilineMessage_succeeds() throws Exception {
        String multilineMessage = "line one\nline two\nline three";
        String signature = sign(canonical(multilineMessage, REAL_MESSAGE_ID, SUBJECT));
        String json = notificationJson(multilineMessage, REAL_MESSAGE_ID, SUBJECT, REAL_UNSUBSCRIBE_URL, TIMESTAMP,
                                       signature);

        SnsMessage parsed = manager.parseMessage(json);

        assertThat(parsed.message()).isEqualTo(multilineMessage);
        assertThat(parsed.messageId()).isEqualTo(REAL_MESSAGE_ID);
    }

    @Test
    void parseMessage_subscriptionConfirmation_succeeds() throws Exception {
        String message = "You have chosen to subscribe to the topic " + TOPIC_ARN + ".\nTo confirm, visit the SubscribeURL.";
        String signature = sign(confirmationCanonical(message, REAL_SUBSCRIBE_URL));
        String json = subscriptionConfirmationJson(message, REAL_SUBSCRIBE_URL, signature);

        SnsMessage parsed = manager.parseMessage(json);

        assertThat(parsed.message()).isEqualTo(message);
    }

    private static String canonical(String message, String messageId, String subject) {
        return TestSigningCertificates.notificationCanonical(message, messageId, subject, TIMESTAMP, TOPIC_ARN);
    }

    private static String confirmationCanonical(String message, String subscribeUrl) {
        return TestSigningCertificates.confirmationCanonical(message, REAL_MESSAGE_ID, subscribeUrl, TIMESTAMP, TOKEN,
                                                             TOPIC_ARN);
    }

    private static String sign(String canonicalMessage) throws Exception {
        return TestSigningCertificates.sign(canonicalMessage, snsKey, "SHA256withRSA");
    }

    private static String notificationJson(String message, String messageId, String subject, String unsubscribeUrl,
                                           String timestamp, String signatureB64) {
        return TestSigningCertificates.notificationJson(message, messageId, subject, timestamp, TOPIC_ARN, signatureB64,
                                                        SIGNING_CERT_URL, unsubscribeUrl);
    }

    private static String subscriptionConfirmationJson(String message, String subscribeUrl, String signatureB64) {
        return TestSigningCertificates.subscriptionConfirmationJson(message, REAL_MESSAGE_ID, TOKEN, TOPIC_ARN, subscribeUrl,
                                                                    TIMESTAMP, signatureB64, SIGNING_CERT_URL);
    }
}

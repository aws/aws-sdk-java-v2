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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.regions.Region;

/**
 * End-to-end checks of the field rules applied by {@code parseMessage}: signed fields other than {@code Message} must be
 * single-line, and {@code UnsubscribeURL} and {@code SubscribeURL} must point at the regional SNS endpoint.
 * <p>
 * The "SNS service" side (canonicalization and RSA-SHA256 signing) is performed here with a private key whose self-signed
 * certificate is served from memory by a stub {@link SdkHttpClient}; everything else is the real module code.
 */
class DefaultSnsMessageManagerFieldValidationTest {

    // Self-signed test certificate, CN/SAN = sns.amazonaws.com (the CN the SDK requires for us-east-1),
    // valid 2020-01-01 .. 2035-01-01, plus its private key. Generated for this test only.
    private static final String CERT_DER_B64 =
        "MIIDNzCCAh+gAwIBAgIUBpBgVH2vE79Hq2hjLplCJBAy9XAwDQYJKoZIhvcNAQELBQAwHDEaMBgG"
        + "A1UEAwwRc25zLmFtYXpvbmF3cy5jb20wHhcNMjAwMTAxMDAwMDAwWhcNMzUwMTAxMDAwMDAwWjAc"
        + "MRowGAYDVQQDDBFzbnMuYW1hem9uYXdzLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC"
        + "ggEBANVfw9G+e5qgHPEyNvHhNZ3ZUz2Q1yMcS5G0HhUxNrEDoBhwBbf/4lFNEMWHfsnCdKzXhxl4"
        + "mHg4WfdEsRYB8T4vSmJc2t54AgIWOgKUHo/Tuf/zXsktHbR7yU7OaSzo/k5+gdEPmWbc9PQE18jN"
        + "Fyrk1JCwGbusFLtMf5UGzxSYMktaDjR/NEA0DnPxi2w5C+0DhKdwfcAp4dn9T4iE7mitPoMvsEgj"
        + "1Y6oPagYYVGvydZo2SZrRkHGaZ/C6VB4zIc9Owb0KjV6W4UsT72jJMVNd3boGTkHLfAyfUWusoDp"
        + "61VkWbiylqYpnwa5xL4Cx1rBP8s9hChap5AkfckWBQkCAwEAAaNxMG8wHQYDVR0OBBYEFDJRBPwW"
        + "0aTtr7C4y/MVYMlD0RYgMB8GA1UdIwQYMBaAFDJRBPwW0aTtr7C4y/MVYMlD0RYgMA8GA1UdEwEB"
        + "/wQFMAMBAf8wHAYDVR0RBBUwE4IRc25zLmFtYXpvbmF3cy5jb20wDQYJKoZIhvcNAQELBQADggEB"
        + "AKxVLqhA2Pvqjzkgw2q6Jcsv8I8MrxzP+L1yGEA7ihH45D4qG9067+cx01mRJ5i57QRXAax7eR3A"
        + "dwbq0N80BZZzHtFUD/5MWViUuS9p2l9blWA0+ofO0WbW0Auy21FnvxWIvsmAAamjyv5ayvRoPA0a"
        + "/Uxj80F4LJvh9MBm+CFUnoAKrHJUoPD7gwj2iyWYABv48h2k0JvE3Ccu6B7PeDNluQDifEaN1C+S"
        + "/rT5F03l9sqSChHH4EB81rhEGUG5gBko9gDv28S+P/f9Vo+/R6Lo4EcCrIrIDeVDOr0nhcNLCHN4"
        + "NXi9KWhELw6VgWeRM79Gu1XheJ19DbafOWSCqNo=";

    private static final String KEY_PKCS8_B64 =
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDVX8PRvnuaoBzxMjbx4TWd2VM9"
        + "kNcjHEuRtB4VMTaxA6AYcAW3/+JRTRDFh37JwnSs14cZeJh4OFn3RLEWAfE+L0piXNreeAICFjoC"
        + "lB6P07n/817JLR20e8lOzmks6P5OfoHRD5lm3PT0BNfIzRcq5NSQsBm7rBS7TH+VBs8UmDJLWg40"
        + "fzRANA5z8YtsOQvtA4SncH3AKeHZ/U+IhO5orT6DL7BII9WOqD2oGGFRr8nWaNkma0ZBxmmfwulQ"
        + "eMyHPTsG9Co1eluFLE+9oyTFTXd26Bk5By3wMn1FrrKA6etVZFm4spamKZ8GucS+AsdawT/LPYQo"
        + "WqeQJH3JFgUJAgMBAAECggEADAB04CpB0DE7krkoP21TZqHcqkTCE4hmzpCRW2maWDuXEjjSDlPE"
        + "ANGzMbRZrKYvVn3rBfZAEUCnyTfhna/8FOtBC/MjU0pWk2DfrQx0aZ+kDmI7wL7oAe8vV0TwXb8F"
        + "quTRW+QjCBbbZyxfwzyYYuiRjxZKSqgDvhzjIj7kGZYM1ir1AkPA4tn1Fu4RzrmsjI2SjnuZMR6r"
        + "OIZSRxoCgtasJ5LEHOv//heCAe0lRxJ/zurXqtSvleu03F4gCxeHywm/EcpIZBbdyxceaVPRUGN2"
        + "+Y995+I//lJno+QZYKe0kHN0EH2A+cZI0bjMRsE/hOZ2cirhNiZu1/Bls8+JZwKBgQD5Pi8j7xC2"
        + "kPTcu2S3cM1W0LhuL1j2q/CkZrI+cUfOhqhMHrYqALMS+KN6gDuPJnekAiHAh6x+9lIPFPsieg+l"
        + "QGOFrf+3yiaMMtdeW47Htobr0+HDESxJ0ToPq426ixu6/SOSqAWtTM58ZZBr5G8N6WCRprMQwJMc"
        + "0D3yDF9HIwKBgQDbKKQbhypIwJIYre9COLjn4X+hVISGgh5B3+IuipcJK6sVciBdA9APb5ZELXvq"
        + "UGcKDnTSGWXLt8/punTNLUMcnKthVfG4EaPx9uXezZLQwT9GA962erxAweRBsQI0v+4ecctO3UmQ"
        + "YDvkTQ+Rl0EM4uADie5A6nQaSIFvobXb4wKBgQCfPTKDA8cqzRwUNomXhbM7Ryi+BPQSzTF258Hz"
        + "kQ5ujavSzkBWP5Ipib9wkaL7x3jiyRXlWwlDKH6u8hmtIk0CqEutaciaFMZiBk2BFH/5as1yuDFi"
        + "HoOsOcedoV5wk1XJNtfWaBEQcqTt5znJJO4q9jLje5ymFFMpMoaRMYRh/wKBgQC/xIYk2Fc1pduw"
        + "4LHVHlEeBgFqJ8x27nCPb9o4o2iZy64+a8jJ20Y524h/S9V8KLKg6BlspnCem87AVDlUN+DbrnOA"
        + "DnYefMEaPtLR7MwlApPdLhG5VxhXCdIzcTHe+nrgBO7DqXpvbOW7VXrkCJgRMIyYTB9AimbhgMRR"
        + "7Ax3xQKBgBVSxSuO7/rSAn7jtKqaNKHx4Bx+k3KMqM1pZmuVA8pEHst0ySXUNPsGvEn3p3gbdRFl"
        + "rgWm60fXWDCo7RJq2CtIIqlcD3X8WM0p7F5UhLQPnwLjqGVv2fCd+QNfMaWxld1qkEU9Dnu/bLrt"
        + "BvUHu4rE0iMkkDCLbQxzb0cKEvK+";

    private static final String SIGNING_CERT_URL =
        "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-1234567890abcdef.pem";
    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:order-events";
    private static final String TIMESTAMP = "2024-06-15T12:30:45.123Z";
    private static final String REAL_MESSAGE_ID = "8f1c2f2e-1111-4d4d-9e2b-9a0b1c2d3e4f";
    private static final String REAL_UNSUBSCRIBE_URL =
        "https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=" + TOPIC_ARN + ":sub-1";
    private static final String OTHER_HOST_UNSUBSCRIBE_URL =
        "https://other.example.com/unsubscribe?topic=order-events";
    private static final String REAL_SUBSCRIBE_URL =
        "https://sns.us-east-1.amazonaws.com/?Action=ConfirmSubscription&TopicArn=" + TOPIC_ARN + "&Token=abc123";
    private static final String OTHER_HOST_SUBSCRIBE_URL = "https://other.example.com/confirm";
    private static final String TOKEN = "2336412f37fb687f5d51e6e2425929f52e8e353134fe72d55ef8b330217d82da";

    private static final String MESSAGE = "Your order 1001 has shipped.";
    private static final String SUBJECT = "order-shipped";

    private static PrivateKey snsKey;

    private InMemoryCertHttpClient httpClient;
    private SnsMessageManager manager;

    @BeforeAll
    static void loadKey() throws Exception {
        snsKey = privateKey();
    }

    @BeforeEach
    void setUp() {
        httpClient = new InMemoryCertHttpClient();
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
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT), snsKey);
        String json = notificationJson(MESSAGE, REAL_MESSAGE_ID, SUBJECT, REAL_UNSUBSCRIBE_URL, TIMESTAMP, signature);

        SnsMessage parsed = manager.parseMessage(json);

        assertThat(parsed).isInstanceOf(SnsNotification.class);
        SnsNotification notification = (SnsNotification) parsed;
        assertThat(notification.subject()).isEqualTo(SUBJECT);
        assertThat(notification.messageId()).isEqualTo(REAL_MESSAGE_ID);
        assertThat(notification.unsubscribeUrl()).isEqualTo(URI.create(REAL_UNSUBSCRIBE_URL));
        assertThat(httpClient.fetches()).isEqualTo(1);
    }

    /**
     * A MessageId containing a line break is rejected before the certificate is fetched.
     */
    @Test
    void parseMessage_messageIdContainsLineBreak_rejected() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT), snsKey);
        String json = notificationJson(MESSAGE, REAL_MESSAGE_ID + "\nsecond line", SUBJECT, REAL_UNSUBSCRIBE_URL, TIMESTAMP,
                                       signature);

        assertThatThrownBy(() -> manager.parseMessage(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Signed field 'MessageId' must not contain line breaks");
        assertThat(httpClient.fetches()).isZero();
    }

    /**
     * A Subject containing a line break is rejected before the certificate is fetched.
     */
    @Test
    void parseMessage_subjectContainsLineBreak_rejected() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT), snsKey);
        String json = notificationJson(MESSAGE, REAL_MESSAGE_ID, SUBJECT + "\nsecond line", REAL_UNSUBSCRIBE_URL, TIMESTAMP,
                                       signature);

        assertThatThrownBy(() -> manager.parseMessage(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Signed field 'Subject' must not contain line breaks");
        assertThat(httpClient.fetches()).isZero();
    }

    @Test
    void parseMessage_unsubscribeUrlOnOtherHost_rejected() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT), snsKey);
        String json = notificationJson(MESSAGE, REAL_MESSAGE_ID, SUBJECT, OTHER_HOST_UNSUBSCRIBE_URL, TIMESTAMP, signature);

        assertThatThrownBy(() -> manager.parseMessage(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("UnsubscribeURL does not match expected host");
        assertThat(httpClient.fetches()).isZero();
    }

    @Test
    void parseMessage_modifiedBody_rejected() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT), snsKey);
        String json = notificationJson(MESSAGE + " (edited)", REAL_MESSAGE_ID, SUBJECT, REAL_UNSUBSCRIBE_URL, TIMESTAMP,
                                       signature);

        assertThatThrownBy(() -> manager.parseMessage(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("did not match");
    }

    @Test
    void parseMessage_modifiedTimestamp_rejected() throws Exception {
        String signature = sign(canonical(MESSAGE, REAL_MESSAGE_ID, SUBJECT), snsKey);
        String json = notificationJson(MESSAGE, REAL_MESSAGE_ID, SUBJECT, REAL_UNSUBSCRIBE_URL, "2024-06-15T12:31:45.123Z",
                                       signature);

        assertThatThrownBy(() -> manager.parseMessage(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("did not match");
    }

    @Test
    void parseMessage_multilineMessage_succeeds() throws Exception {
        String multilineMessage = "line one\nline two\nline three";
        String signature = sign(canonical(multilineMessage, REAL_MESSAGE_ID, SUBJECT), snsKey);
        String json = notificationJson(multilineMessage, REAL_MESSAGE_ID, SUBJECT, REAL_UNSUBSCRIBE_URL, TIMESTAMP,
                                       signature);

        SnsMessage parsed = manager.parseMessage(json);

        assertThat(parsed.message()).isEqualTo(multilineMessage);
        assertThat(parsed.messageId()).isEqualTo(REAL_MESSAGE_ID);
    }

    @Test
    void parseMessage_subscribeUrlOnOtherHost_rejected() throws Exception {
        String message = "You have chosen to subscribe to the topic " + TOPIC_ARN + ".";
        String signature = sign(confirmationCanonical(message, OTHER_HOST_SUBSCRIBE_URL), snsKey);
        String json = subscriptionConfirmationJson(message, OTHER_HOST_SUBSCRIBE_URL, signature);

        assertThatThrownBy(() -> manager.parseMessage(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("SubscribeURL does not match expected host");
        assertThat(httpClient.fetches()).isZero();
    }

    @Test
    void parseMessage_subscriptionConfirmation_succeeds() throws Exception {
        String message = "You have chosen to subscribe to the topic " + TOPIC_ARN + ".\nTo confirm, visit the SubscribeURL.";
        String signature = sign(confirmationCanonical(message, REAL_SUBSCRIBE_URL), snsKey);
        String json = subscriptionConfirmationJson(message, REAL_SUBSCRIBE_URL, signature);

        SnsMessage parsed = manager.parseMessage(json);

        assertThat(parsed.message()).isEqualTo(message);
    }

    // ---- the "SNS service" side: canonicalization per the AWS docs, then RSA-SHA256 ----

    private static String canonical(String message, String messageId, String subject) {
        StringBuilder sb = new StringBuilder();
        sb.append("Message\n").append(message).append('\n');
        sb.append("MessageId\n").append(messageId).append('\n');
        if (subject != null) {
            sb.append("Subject\n").append(subject).append('\n');
        }
        sb.append("Timestamp\n").append(TIMESTAMP).append('\n');
        sb.append("TopicArn\n").append(TOPIC_ARN).append('\n');
        sb.append("Type\nNotification\n");
        return sb.toString();
    }

    private static String confirmationCanonical(String message, String subscribeUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("Message\n").append(message).append('\n');
        sb.append("MessageId\n").append(REAL_MESSAGE_ID).append('\n');
        sb.append("SubscribeURL\n").append(subscribeUrl).append('\n');
        sb.append("Timestamp\n").append(TIMESTAMP).append('\n');
        sb.append("Token\n").append(TOKEN).append('\n');
        sb.append("TopicArn\n").append(TOPIC_ARN).append('\n');
        sb.append("Type\nSubscriptionConfirmation\n");
        return sb.toString();
    }

    private static String sign(String canonicalMessage, PrivateKey key) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(canonicalMessage.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private static String notificationJson(String message, String messageId, String subject, String unsubscribeUrl,
                                           String timestamp, String signatureB64) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"Type\" : \"Notification\",\n");
        sb.append("  \"MessageId\" : \"").append(esc(messageId)).append("\",\n");
        sb.append("  \"TopicArn\" : \"").append(esc(TOPIC_ARN)).append("\",\n");
        if (subject != null) {
            sb.append("  \"Subject\" : \"").append(esc(subject)).append("\",\n");
        }
        sb.append("  \"Message\" : \"").append(esc(message)).append("\",\n");
        sb.append("  \"Timestamp\" : \"").append(esc(timestamp)).append("\",\n");
        sb.append("  \"SignatureVersion\" : \"2\",\n");
        sb.append("  \"Signature\" : \"").append(signatureB64).append("\",\n");
        sb.append("  \"SigningCertURL\" : \"").append(esc(SIGNING_CERT_URL)).append("\",\n");
        sb.append("  \"UnsubscribeURL\" : \"").append(esc(unsubscribeUrl)).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

    private static String subscriptionConfirmationJson(String message, String subscribeUrl, String signatureB64) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"Type\" : \"SubscriptionConfirmation\",\n");
        sb.append("  \"MessageId\" : \"").append(esc(REAL_MESSAGE_ID)).append("\",\n");
        sb.append("  \"Token\" : \"").append(esc(TOKEN)).append("\",\n");
        sb.append("  \"TopicArn\" : \"").append(esc(TOPIC_ARN)).append("\",\n");
        sb.append("  \"Message\" : \"").append(esc(message)).append("\",\n");
        sb.append("  \"SubscribeURL\" : \"").append(esc(subscribeUrl)).append("\",\n");
        sb.append("  \"Timestamp\" : \"").append(esc(TIMESTAMP)).append("\",\n");
        sb.append("  \"SignatureVersion\" : \"2\",\n");
        sb.append("  \"Signature\" : \"").append(signatureB64).append("\",\n");
        sb.append("  \"SigningCertURL\" : \"").append(esc(SIGNING_CERT_URL)).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static PrivateKey privateKey() throws Exception {
        byte[] der = Base64.getDecoder().decode(KEY_PKCS8_B64);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static byte[] certificatePem() {
        StringBuilder sb = new StringBuilder("-----BEGIN CERTIFICATE-----\n");
        for (int i = 0; i < CERT_DER_B64.length(); i += 64) {
            sb.append(CERT_DER_B64, i, Math.min(i + 64, CERT_DER_B64.length())).append('\n');
        }
        sb.append("-----END CERTIFICATE-----\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Stands in for the network: serves the SNS signing certificate from memory and counts the fetches. */
    private static final class InMemoryCertHttpClient implements SdkHttpClient {
        private final AtomicInteger fetches = new AtomicInteger();

        int fetches() {
            return fetches.get();
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            fetches.incrementAndGet();

            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    return HttpExecuteResponse.builder()
                                              .response(SdkHttpFullResponse.builder().statusCode(200).build())
                                              .responseBody(AbortableInputStream.create(
                                                  new ByteArrayInputStream(certificatePem())))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public void close() {
        }

        @Override
        public String clientName() {
            return "InMemoryCertHttpClient";
        }
    }
}

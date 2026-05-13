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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;

class SignatureValidatorTest {
    private static final String RESOURCE_ROOT = "/software/amazon/awssdk/messagemanager/sns/internal/";
    private static final String SIGNING_CERT_RESOURCE = "SimpleNotificationService-7506a1e35b36ef5a444dd1a8e7cc3ed8.pem";
    private static final SignatureValidator VALIDATOR = new SignatureValidator();
    private static X509Certificate signingCertificate;

    @BeforeAll
    static void setup() throws CertificateException {
        InputStream is = resourceAsStream(SIGNING_CERT_RESOURCE);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        signingCertificate = (X509Certificate) factory.generateCertificate(is);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validMessages")
    void validateSignature_signatureValid_doesNotThrow(TestCase tc) {
        SnsMessageUnmarshaller unmarshaller = new SnsMessageUnmarshaller();
        SnsMessage msg = unmarshaller.unmarshall(resourceAsStream(tc.messageJsonResource));
        VALIDATOR.validateSignature(msg, signingCertificate.getPublicKey());
    }

    @Test
    void validateSignature_signatureMismatch_throws() {
        SnsNotification notification = SnsNotification.builder()
                                                      .message("hello world")
                                                      .messageId("message-id")
                                                      .timestamp(Instant.now())
                                                      .signature(SdkBytes.fromByteArray(new byte[256]))
                                                      .signatureVersion(SignatureVersion.VERSION_1)
                                                      .build();

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, signingCertificate.getPublicKey()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("The computed signature did not match the expected signature");
    }

    @Test
    void validateSignature_signatureMissing_throws() {
        SnsNotification notification = SnsNotification.builder()
                                                      .subject("hello world")
                                                      .message("hello world")
                                                      .messageId("message-id")
                                                      .timestamp(Instant.now())
                                                      .unsubscribeUrl(URI.create("https://my-test-service.amazonaws.com"))
                                                      .signingCertUrl(URI.create("https://my-test-service.amazonaws.com/cert"
                                                                                 + ".pem"))
                                                      .signatureVersion(SignatureVersion.VERSION_1)
                                                      .build();

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, signingCertificate.getPublicKey()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Message signature cannot be null");
    }

    @Test
    void validateSignature_timestampMissing_throws() {
        SnsNotification notification = SnsNotification.builder()
                                                      .subject("hello world")
                                                      .message("hello world")
                                                      .messageId("message-id")
                                                      .signature(SdkBytes.fromByteArray(new byte[256]))
                                                      .unsubscribeUrl(URI.create("https://my-test-service.amazonaws.com"))
                                                      .signingCertUrl(URI.create("https://my-test-service.amazonaws.com/cert"
                                                                                 + ".pem"))
                                                      .signatureVersion(SignatureVersion.VERSION_1)
                                                      .build();

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, signingCertificate.getPublicKey()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Message timestamp cannot be null");
    }

    @Test
    void validateSignature_signatureVersionMissing_throws() {
        SnsNotification notification = SnsNotification.builder()
                                                      .subject("hello world")
                                                      .message("hello world")
                                                      .messageId("message-id")
                                                      .signature(SdkBytes.fromByteArray(new byte[256]))
                                                      .timestamp(Instant.now())
                                                      .unsubscribeUrl(URI.create("https://my-test-service.amazonaws.com"))
                                                      .signingCertUrl(URI.create("https://my-test-service.amazonaws.com/cert"
                                                                                 + ".pem"))
                                                      .build();


        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, signingCertificate.getPublicKey()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Message signature version cannot be null");
    }

    @Test
    void validateSignature_certInvalid_throws() throws CertificateException {
        SnsNotification notification = SnsNotification.builder()
                                                      .signature(SdkBytes.fromByteArray(new byte[1]))
                                                      .signatureVersion(SignatureVersion.VERSION_1)
                                                      .timestamp(Instant.now())
                                                      .build();

        PublicKey badKey = mock(PublicKey.class);
        when(badKey.getFormat()).thenReturn("X.509");
        when(badKey.getAlgorithm()).thenReturn("RSA");
        when(badKey.getEncoded()).thenReturn(new byte[1]);

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, badKey))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("The public key is invalid");
    }

    @Test
    void validateSignature_signatureInvalid_throws() throws CertificateException {
        SnsNotification notification = SnsNotification.builder()
                                                      .subject("hello world")
                                                      .message("hello world")
                                                      .messageId("message-id")
                                                      .signature(SdkBytes.fromByteArray(new byte[1]))
                                                      .signatureVersion(SignatureVersion.VERSION_1)
                                                      .timestamp(Instant.now())
                                                      .unsubscribeUrl(URI.create("https://my-test-service.amazonaws.com"))
                                                      .signingCertUrl(URI.create("https://my-test-service.amazonaws.com/cert"
                                                                                 + ".pem"))
                                                      .build();

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, signingCertificate.getPublicKey()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("The signature is invalid");
    }

    private static List<TestCase> validMessages() {
        return Stream.of(
                         new TestCase("Notification - No Subject", "test-notification-no-subject.json"),
                         new TestCase("Notification - Version 2 signature", "test-notification-signature-v2.json"),
                         new TestCase("Notification with subject", "test-notification-with-subject.json"),
                         new TestCase("Subscription confirmation", "test-subscription-confirmation.json"),
                         new TestCase("Unsubscribe confirmation", "test-unsubscribe-confirmation.json")
                     )
                     .collect(Collectors.toList());
    }

    private static InputStream resourceAsStream(String resourceName) {
        return SignatureValidatorTest.class.getResourceAsStream(RESOURCE_ROOT + resourceName);
    }

    private static class TestCase {
        private String desription;
        private String messageJsonResource;

        public TestCase(String desription, String messageJsonResource) {
            this.desription = desription;
            this.messageJsonResource = messageJsonResource;
        }

        @Override
        public String toString() {
            return desription;
        }
    }
}
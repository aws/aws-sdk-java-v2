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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;

public class DefaultSnsMessageManagerTest {
    private static SdkHttpClient.Builder<?> mockHttpClientBuilder;
    private static SdkHttpClient mockHttpClient;

    @BeforeAll
    static void setup() {
        mockHttpClientBuilder = mock(SdkHttpClient.Builder.class);
        mockHttpClient = mock(SdkHttpClient.class);
        when(mockHttpClientBuilder.buildWithDefaults(any(AttributeMap.class))).thenReturn(mockHttpClient);
    }

    @Test
    void close_httpClientConfiguredOnBuilder_httpClientNotClosed() {
        SdkHttpClient mockClient = mock(SdkHttpClient.class);

        SnsMessageManager msgManager = DefaultSnsMessageManager.builder()
                                                               .httpClient(mockClient)
                                                               .region(Region.US_WEST_2)
                                                               .build();

        msgManager.close();

        verifyNoInteractions(mockClient);
    }

    @Test
    void parseMessage_streamNull_throws() {
        SnsMessageManager msgManager =
            DefaultSnsMessageManager.builder().httpClient(mockHttpClient).region(Region.US_WEST_2).build();

        assertThatThrownBy(() -> msgManager.parseMessage((InputStream) null))
            .hasMessage("message cannot be null");
    }

    @Test
    void parseMessage_stringNull_throws() {
        SnsMessageManager msgManager =
            DefaultSnsMessageManager.builder().httpClient(mockHttpClient).region(Region.US_WEST_2).build();

        assertThatThrownBy(() -> msgManager.parseMessage((String) null))
            .hasMessage("message cannot be null");
    }

    @Test
    void close_certRetrieverClosed() {
        CertificateRetriever mockCertRetriever = mock(CertificateRetriever.class);

        SnsMessageManager msgManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(mockCertRetriever)
            .region(Region.US_WEST_2)
            .build();

        msgManager.close();

        verify(mockCertRetriever).close();
    }

    @Test
    void parseMessage_shortSignature_certificateNotRetrieved() {
        CertificateRetriever mockRetriever = mock(CertificateRetriever.class);
        SnsMessageManager msgManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(mockRetriever)
            .region(Region.US_WEST_2)
            .build();

        String message = notification("AAAA", "https://sns.us-west-2.amazonaws.com/cert.pem");

        assertThatThrownBy(() -> msgManager.parseMessage(message))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("too short");
        verifyNoInteractions(mockRetriever);
    }

    @Test
    void parseMessage_missingSigningCertUrl_throwsSdkClientException() {
        CertificateRetriever mockRetriever = mock(CertificateRetriever.class);
        SnsMessageManager msgManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(mockRetriever)
            .region(Region.US_WEST_2)
            .build();

        String message = notification(Base64.getEncoder().encodeToString(new byte[256]), null);

        assertThatThrownBy(() -> msgManager.parseMessage(message))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("SigningCertURL must be present");
        verifyNoInteractions(mockRetriever);
    }

    @Test
    void parseMessage_signingCertUrlOnOtherHost_certificateNotRetrieved() {
        CertificateRetriever mockRetriever = mock(CertificateRetriever.class);
        SnsMessageManager msgManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(mockRetriever)
            .region(Region.US_WEST_2)
            .build();

        String message = notification(Base64.getEncoder().encodeToString(new byte[256]),
                                      "https://sns.us-east-1.amazonaws.com/cert.pem");

        assertThatThrownBy(() -> msgManager.parseMessage(message))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("SigningCertURL does not match expected host: sns.us-west-2.amazonaws.com");
        verifyNoInteractions(mockRetriever);
    }

    @Test
    void parseMessage_minimumVersion2_version1Message_certificateNotRetrieved() {
        CertificateRetriever mockRetriever = mock(CertificateRetriever.class);
        SnsMessageManager msgManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(mockRetriever)
            .region(Region.US_WEST_2)
            .minimumSignatureVersion(SignatureVersion.VERSION_2)
            .build();

        String message = notification(Base64.getEncoder().encodeToString(new byte[256]),
                                      "https://sns.us-west-2.amazonaws.com/cert.pem");

        assertThatThrownBy(() -> msgManager.parseMessage(message))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("below the minimum");
        verifyNoInteractions(mockRetriever);
    }

    @Test
    void parseMessage_validSha1SignedVersion1Message_acceptedByDefaultRejectedWithMinimumVersion2() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String certUrl = "https://sns.us-west-2.amazonaws.com/cert.pem";
        String canonical = TestSigningCertificates.notificationCanonical("hello", "id", null, "2026-08-29T00:00:00.000Z",
                                                                         "arn:aws:sns:us-west-2:1:t");
        String message = notification(TestSigningCertificates.sign(canonical, keyPair.getPrivate(), "SHA1withRSA"), certUrl);

        CertificateRetriever defaultRetriever = mock(CertificateRetriever.class);
        when(defaultRetriever.cachedCertificates(URI.create(certUrl))).thenReturn(Collections.emptyList());
        when(defaultRetriever.retrieveCertificate(URI.create(certUrl))).thenReturn(keyPair.getPublic());
        SnsMessageManager defaultManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(defaultRetriever)
            .region(Region.US_WEST_2)
            .build();

        SnsMessage parsed = defaultManager.parseMessage(message);
        assertThat(parsed.signatureVersion()).isEqualTo(SignatureVersion.VERSION_1);

        CertificateRetriever strictRetriever = mock(CertificateRetriever.class);
        when(strictRetriever.cachedCertificates(URI.create(certUrl))).thenReturn(Collections.emptyList());
        when(strictRetriever.retrieveCertificate(URI.create(certUrl))).thenReturn(keyPair.getPublic());
        SnsMessageManager strictManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(strictRetriever)
            .region(Region.US_WEST_2)
            .minimumSignatureVersion(SignatureVersion.VERSION_2)
            .build();

        assertThatThrownBy(() -> strictManager.parseMessage(message))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("below the minimum");
        verifyNoInteractions(strictRetriever);
    }

    @Test
    void parseMessage_minimumVersion2_version2Message_notRejectedForVersion() throws Exception {
        CertificateRetriever mockRetriever = mock(CertificateRetriever.class);
        when(mockRetriever.retrieveCertificate(any())).thenReturn(
            KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic());
        SnsMessageManager msgManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(mockRetriever)
            .region(Region.US_WEST_2)
            .minimumSignatureVersion(SignatureVersion.VERSION_2)
            .build();

        String message = notification(Base64.getEncoder().encodeToString(new byte[256]),
                                      "https://sns.us-west-2.amazonaws.com/cert.pem")
            .replace("\"SignatureVersion\":\"1\"", "\"SignatureVersion\":\"2\"");

        // The version check passes, so the manager goes on to verify against the retrieved certificate.
        assertThatThrownBy(() -> msgManager.parseMessage(message))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("The computed signature did not match the expected signature");
        verify(mockRetriever).retrieveCertificate(any());
    }

    @Test
    void build_minimumVersionNull_throws() {
        SnsMessageManager.Builder builder = DefaultSnsMessageManager.builder()
                                                                    .httpClient(mockHttpClient)
                                                                    .region(Region.US_WEST_2)
                                                                    .minimumSignatureVersion(null);

        assertThatThrownBy(builder::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("minimumSignatureVersion");
    }

    @Test
    void build_minimumVersionUnknown_throws() {
        SnsMessageManager.Builder builder = DefaultSnsMessageManager.builder()
                                                                    .httpClient(mockHttpClient)
                                                                    .region(Region.US_WEST_2)
                                                                    .minimumSignatureVersion(SignatureVersion.UNKNOWN);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("minimumSignatureVersion");
    }

    @Test
    void build_minimumVersionSet_builds() {
        assertThatCode(() -> DefaultSnsMessageManager.builder()
                                                     .httpClient(mockHttpClient)
                                                     .region(Region.US_WEST_2)
                                                     .minimumSignatureVersion(SignatureVersion.VERSION_2)
                                                     .build())
            .doesNotThrowAnyException();
    }

    private static String notification(String signature, String signingCertUrl) {
        return "{"
               + "\"Type\":\"Notification\","
               + "\"MessageId\":\"id\","
               + "\"TopicArn\":\"arn:aws:sns:us-west-2:1:t\","
               + "\"Message\":\"hello\","
               + "\"Timestamp\":\"2026-08-29T00:00:00.000Z\","
               + "\"SignatureVersion\":\"1\","
               + "\"Signature\":\"" + signature + "\""
               + (signingCertUrl == null ? "" : ",\"SigningCertURL\":\"" + signingCertUrl + "\"")
               + "}";
    }

    @Test
    void build_httpClientNotConfigured_usesDefaultHttpClient() {
        System.setProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property(), TestHttpService.class.getName());
        try {
            DefaultSnsMessageManager.builder().region(Region.US_WEST_2).build();

            ArgumentCaptor<AttributeMap> captor = ArgumentCaptor.forClass(AttributeMap.class);
            verify(mockHttpClientBuilder).buildWithDefaults(captor.capture());

            AttributeMap buildArgs = captor.getValue();
            assertThat(buildArgs.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT)).isEqualTo(Duration.ofSeconds(10));
            assertThat(buildArgs.get(SdkHttpConfigurationOption.READ_TIMEOUT)).isEqualTo(Duration.ofSeconds(30));
        } finally {
            System.clearProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property());
        }
    }

    @Test
    void close_defaultHttpClientUsed_isClosed() {
        System.setProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property(), TestHttpService.class.getName());
        try {
            SnsMessageManager msgManager = DefaultSnsMessageManager.builder()
                                                                   .region(Region.US_WEST_2)
                                                                   .build();
            msgManager.close();

            verify(mockHttpClient).close();
        } finally {
            System.clearProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property());
        }
    }

    // NOTE: needs to be public to work with the service loader
    public static class TestHttpService implements SdkHttpService {

        @Override
        public SdkHttpClient.Builder<?> createHttpClientBuilder() {
            return mockHttpClientBuilder;
        }
    }
}

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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import software.amazon.awssdk.utils.IoUtils;

class CertificateRetrieverTest {
    private static final String RESOURCE_ROOT = "/software/amazon/awssdk/messagemanager/sns/internal/";
    private static final String CERT_COMMON_NAME = "my-test-service.amazonaws.com";
    private static final URI TEST_CERT_URI = URI.create("https://my-test-service.amazonaws.com/cert.pem");
    private SdkHttpClient mockHttpClient;

    private static byte[] validCert;
    private static byte[] expiredCert;
    private static byte[] futureValidCert;

    @BeforeAll
    static void classSetUp() throws IOException {
        validCert = getResourceBytes("valid-cert.pem");
        expiredCert = getResourceBytes("expired-cert.pem");
        futureValidCert = getResourceBytes("valid-in-future-cert.pem");
    }

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(SdkHttpClient.class);
    }

    @Test
    void constructor_nullHttpClient_throwsException() {
        assertThatThrownBy(() -> new CertificateRetriever(null, CERT_COMMON_NAME))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("httpClient must not be null.");
    }

    @Test
    void constructor_nullCertCommonName_throwsException() {
        assertThatThrownBy(() -> new CertificateRetriever(mockHttpClient, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("certCommonName must not be null.");
    }

    @Test
    void retrieveCertificate_nullUrl_throwsException() {
        assertThatThrownBy(() -> new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME)
            .retrieveCertificate(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("certificateUrl must not be null.");
    }

    @Test
    void retrieveCertificate_httpUrl_throwsException() {
        assertThatThrownBy(() -> new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME)
            .retrieveCertificate(URI.create("http://my-service.amazonaws.com/cert.pem")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Certificate URL must use HTTPS");
    }

    @Test
    void retrieveCertificate_httpError_throwsException() throws IOException {
        mockResponse(SdkHttpFullResponse.builder().statusCode(400).build(), null);

        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(TEST_CERT_URI))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Request was unsuccessful. Status Code: 400");
    }

    @Test
    void retrieveCertificate_callThrows_throwsException() throws IOException {
        ExecutableHttpRequest mockExecRequest = mock(ExecutableHttpRequest.class);

        RuntimeException cause = new RuntimeException("oops");
        when(mockExecRequest.call()).thenThrow(cause);

        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(mockExecRequest);

        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        assertThatThrownBy(() -> certificateRetriever
            .retrieveCertificate(TEST_CERT_URI))
            .isInstanceOf(SdkClientException.class)
            .hasCause(cause)
            .hasMessageContaining("Unexpected error while retrieving URL");
    }

    @Test
    void retrieveCertificate_noResponseStream_throwsException() throws IOException {
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), null);

        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(TEST_CERT_URI))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Response body is empty");
    }

    @Test
    void retrieveCertificate_emptyResponseBody_throwsException() throws IOException {
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), new byte[0]);

        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(TEST_CERT_URI))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Certificate does not match expected X509 PEM format.");
    }

    @Test
    void retrieveCertificate_invalidCertificateFormat_throwsException() throws IOException {
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), "this is not a cert".getBytes(StandardCharsets.UTF_8));

        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(TEST_CERT_URI))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Certificate does not match expected X509 PEM format.");
    }

    @Test
    void retrieveCertificate_nonParsableCertificate_throwsException() throws IOException {
        String certificate = "-----BEGIN CERTIFICATE-----\n"
                             + "MIIE+DCCAuCgAwIBAgIJAOCC5W/Vl4AEMA0GCSqGSIb3DQEBDAUAMCgxJjAkBgNV\n"
                             + "-----END CERTIFICATE-----\n";
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), certificate.getBytes(StandardCharsets.UTF_8));

        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(TEST_CERT_URI))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("The certificate could not be parsed");
    }

    @Test
    void retrieveCertificate_certificateExpired_throwsException() throws IOException {
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), expiredCert);
        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(TEST_CERT_URI))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("The certificate is expired");
    }

    @Test
    void retrieveCertificate_certNotYetValid_throwsException() throws IOException {
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), futureValidCert);
        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(TEST_CERT_URI))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("The certificate is not yet valid");
    }

    @Test
    void retrieveCertificate_commonNameMismatch_throwsException() throws IOException {
        String commonName = "my-other-service.amazonaws.com";
        URI certUri = URI.create("https://" + commonName + "/cert.pem");
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), futureValidCert);
        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, commonName);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(certUri))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Certificate does not match expected common name: my-other-service.amazonaws.com");
    }

    @Test
    void retrieveCertificate_validPemCertificate_succeeds() throws IOException {
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), validCert);

        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        assertThat(certificateRetriever.retrieveCertificate(TEST_CERT_URI))
            .hasSizeGreaterThan(0);
    }

    @Test
    void retrieveCertificate_cacheHit_returnsFromCache() throws IOException {
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), validCert);

        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        certificateRetriever.retrieveCertificate(TEST_CERT_URI);
        certificateRetriever.retrieveCertificate(TEST_CERT_URI);

        verify(mockHttpClient, times(1)).prepareRequest(any(HttpExecuteRequest.class));
    }

    @Test
    void retrieveCertificate_differentUrls_cachesIndependently() throws IOException {
        mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), validCert);

        CertificateRetriever certificateRetriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);

        URI cert1Url = URI.create("https://" + CERT_COMMON_NAME + "/cert1.pem");
        certificateRetriever.retrieveCertificate(cert1Url);
        certificateRetriever.retrieveCertificate(cert1Url);

        URI cert2Url = URI.create("https://" + CERT_COMMON_NAME + "/cert2.pem");
        certificateRetriever.retrieveCertificate(cert2Url);
        certificateRetriever.retrieveCertificate(cert2Url);

        verify(mockHttpClient, times(2)).prepareRequest(any(HttpExecuteRequest.class));

    }

    @Test
    void retrieveCertificate_concurrentAccess_threadSafe() throws Exception {
        int threads = 4;
        ExecutorService exec = Executors.newFixedThreadPool(threads);


        for (int i = 0; i < 10_000; ++i) {
            CountDownLatch start = new CountDownLatch(threads);
            CountDownLatch end = new CountDownLatch(threads);

            mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), validCert);

            CertificateRetriever retriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);
            for (int j = 0; j < threads; ++j) {
                exec.submit(() -> {
                    start.countDown();

                    retriever.retrieveCertificate(TEST_CERT_URI);

                    end.countDown();
                });
            }

            end.await();

            verify(mockHttpClient, times(1)).prepareRequest(any(HttpExecuteRequest.class));
            reset(mockHttpClient);
        }
    }

    @Test
    void retrieveCertificate_concurrentDifferentUrls_threadSafe() throws Exception {
        int threads = 4;
        ExecutorService exec = Executors.newFixedThreadPool(threads);


        for (int i = 0; i < 10_000; ++i) {
            CountDownLatch start = new CountDownLatch(threads);
            CountDownLatch end = new CountDownLatch(threads);

            mockResponse(SdkHttpFullResponse.builder().statusCode(200).build(), validCert);

            CertificateRetriever retriever = new CertificateRetriever(mockHttpClient, CERT_COMMON_NAME);
            for (int j = 0; j < threads; ++j) {
                URI uri = URI.create(String.format("https://" + CERT_COMMON_NAME + "/cert%d.pem", j % 2));
                exec.submit(() -> {
                    start.countDown();

                    retriever.retrieveCertificate(uri);

                    end.countDown();
                });
            }

            end.await();

            verify(mockHttpClient, times(2)).prepareRequest(any(HttpExecuteRequest.class));
            reset(mockHttpClient);
        }
    }

    private static byte[] getResourceBytes(String resourcePath) throws IOException {
        return IoUtils.toByteArray(CertificateRetrieverTest.class.getResourceAsStream(RESOURCE_ROOT + resourcePath));
    }

    private void mockResponse(SdkHttpFullResponse httpResponse, byte[] content) throws IOException {
        ExecutableHttpRequest mockExecRequest = mock(ExecutableHttpRequest.class);

        when(mockExecRequest.call()).thenAnswer(i -> {
            AbortableInputStream body = null;
            if (content != null) {
                body = asStream(content);
            }

            return HttpExecuteResponse.builder()
                                      .response(httpResponse)
                                      .responseBody(body)
                                      .build();
        });

        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(mockExecRequest);
    }

    private static AbortableInputStream asStream(byte[] b) {
        return AbortableInputStream.create(new ByteArrayInputStream(b));
    }
}

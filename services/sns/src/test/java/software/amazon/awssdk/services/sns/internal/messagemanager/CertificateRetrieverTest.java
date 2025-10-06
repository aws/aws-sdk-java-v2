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

package software.amazon.awssdk.services.sns.internal.messagemanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sns.messagemanager.SnsCertificateException;

/**
 * Unit tests for {@link CertificateRetriever}.
 * 
 * <p>This test class validates the certificate retrieval and caching functionality
 * of the SNS message manager. It focuses on testing security validations, caching behavior,
 * error handling, and thread-safety.
 * 
 * <p>The test strategy includes:
 * <ul>
 *   <li>Testing certificate URL validation against trusted SNS domains</li>
 *   <li>Testing HTTPS-only certificate retrieval</li>
 *   <li>Testing certificate caching functionality and TTL behavior</li>
 *   <li>Testing error handling for invalid URLs and network failures</li>
 *   <li>Testing thread-safety of cache implementation</li>
 *   <li>Testing certificate content validation and security checks</li>
 * </ul>
 * 
 * @see CertificateRetriever
 */
class CertificateRetrieverTest {

    /** Valid certificate URL for US East 1 region used in tests. */
    private static final String VALID_CERT_URL_US_EAST_1 = "https://sns.us-east-1.amazonaws.com/cert.pem";
    
    /** Valid certificate URL for EU West 1 region used in tests. */
    private static final String VALID_CERT_URL_EU_WEST_1 = "https://sns.eu-west-1.amazonaws.com/cert.pem";
    
    /** Valid certificate URL for US Gov Cloud region used in tests. */
    private static final String VALID_CERT_URL_GOV_CLOUD = "https://sns.us-gov-west-1.amazonaws.com/cert.pem";
    
    /** Valid certificate URL for China region used in tests. */
    private static final String VALID_CERT_URL_CHINA = "https://sns.cn-north-1.amazonaws.com.cn/cert.pem";
    
    /** 
     * Valid PEM-encoded X.509 certificate used for testing certificate parsing and validation.
     * This is a minimal test certificate that passes basic format validation.
     */
    private static final String VALID_PEM_CERTIFICATE = 
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIBkTCB+wIJAKZV5i2qhHcmMA0GCSqGSIb3DQEBBQUAMBQxEjAQBgNVBAMMCWxv\n" +
        "Y2FsaG9zdDAeFw0yMzAxMDEwMDAwMDBaFw0yNDAxMDEwMDAwMDBaMBQxEjAQBgNV\n" +
        "BAMMCWxvY2FsaG9zdDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAuVqVeII=\n" +
        "-----END CERTIFICATE-----";
    
    /** 
     * Valid DER-encoded X.509 certificate used for testing binary certificate format handling.
     * This represents the same certificate as {@link #VALID_PEM_CERTIFICATE} in DER format.
     */
    private static final byte[] VALID_DER_CERTIFICATE = {
        (byte) 0x30, (byte) 0x82, (byte) 0x01, (byte) 0x91, (byte) 0x30, (byte) 0x82, (byte) 0x01, (byte) 0x3A, 
        (byte) 0x02, (byte) 0x09, (byte) 0x00, (byte) 0xA6, (byte) 0x55, (byte) 0xE6, (byte) 0x2D, (byte) 0xAA,
        (byte) 0x84, (byte) 0x77, (byte) 0x26, (byte) 0x30, (byte) 0x0D, (byte) 0x06, (byte) 0x09, (byte) 0x2A, 
        (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x05,
        (byte) 0x05, (byte) 0x00, (byte) 0x30, (byte) 0x14, (byte) 0x31, (byte) 0x12, (byte) 0x30, (byte) 0x10, 
        (byte) 0x06, (byte) 0x03, (byte) 0x55, (byte) 0x04, (byte) 0x03, (byte) 0x0C, (byte) 0x09, (byte) 0x6C,
        (byte) 0x6F, (byte) 0x63, (byte) 0x61, (byte) 0x6C, (byte) 0x68, (byte) 0x6F, (byte) 0x73, (byte) 0x74, 
        (byte) 0x30, (byte) 0x1E, (byte) 0x17, (byte) 0x0D, (byte) 0x32, (byte) 0x33, (byte) 0x30, (byte) 0x31,
        (byte) 0x30, (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, 
        (byte) 0x5A, (byte) 0x17, (byte) 0x0D, (byte) 0x32, (byte) 0x34, (byte) 0x30, (byte) 0x31, (byte) 0x30,
        (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x5A, 
        (byte) 0x30, (byte) 0x14, (byte) 0x31, (byte) 0x12, (byte) 0x30, (byte) 0x10, (byte) 0x06, (byte) 0x03,
        (byte) 0x55, (byte) 0x04, (byte) 0x03, (byte) 0x0C, (byte) 0x09, (byte) 0x6C, (byte) 0x6F, (byte) 0x63, 
        (byte) 0x61, (byte) 0x6C, (byte) 0x68, (byte) 0x6F, (byte) 0x73, (byte) 0x74, (byte) 0x30, (byte) 0x81,
        (byte) 0x9F, (byte) 0x30, (byte) 0x0D, (byte) 0x06, (byte) 0x09, (byte) 0x2A, (byte) 0x86, (byte) 0x48, 
        (byte) 0x86, (byte) 0xF7, (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00,
        (byte) 0x03, (byte) 0x81, (byte) 0x8D, (byte) 0x00, (byte) 0x30, (byte) 0x81, (byte) 0x89, (byte) 0x02, 
        (byte) 0x81, (byte) 0x81, (byte) 0x00, (byte) 0xB9, (byte) 0x5A, (byte) 0x95, (byte) 0x78, (byte) 0x82
    };

    /** Mock HTTP client used for testing certificate retrieval operations. */
    private SdkHttpClient mockHttpClient;
    
    /** Certificate retriever instance under test. */
    private CertificateRetriever certificateRetriever;

    /**
     * Sets up test fixtures before each test method execution.
     * 
     * <p>Initializes a mock HTTP client and creates a CertificateRetriever instance
     * with a 5-minute cache timeout for testing.
     */
    @BeforeEach
    void setUp() {
        mockHttpClient = mock(SdkHttpClient.class);
        certificateRetriever = new CertificateRetriever(mockHttpClient, Duration.ofMinutes(5));
    }



    // ========== Constructor Validation Tests ==========

    /**
     * Tests that CertificateRetriever constructor properly validates null HTTP client parameter.
     * 
     * <p>This test ensures that the constructor performs proper null checking on the httpClient
     * parameter and throws a {@link NullPointerException} with a descriptive error message
     * when null is provided.
     * 
     * <p>This validation is critical for preventing null pointer exceptions during certificate
     * retrieval operations and ensuring that callers receive clear feedback about invalid parameters.
     * 
     * @throws NullPointerException Expected exception when httpClient parameter is null
     */
    @Test
    void constructor_nullHttpClient_throwsException() {
        assertThatThrownBy(() -> new CertificateRetriever(null, Duration.ofMinutes(5)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("httpClient must not be null");
    }

    /**
     * Tests that CertificateRetriever constructor properly validates null cache timeout parameter.
     * 
     * <p>This test ensures proper null checking on the certificateCacheTimeout parameter and verifies
     * that a {@link NullPointerException} is thrown with a descriptive error message when null is provided.
     * 
     * <p>The cache timeout is essential for controlling certificate cache behavior and preventing
     * indefinite caching of potentially compromised certificates.
     * 
     * @throws NullPointerException Expected exception when certificateCacheTimeout parameter is null
     */
    @Test
    void constructor_nullCacheTimeout_throwsException() {
        assertThatThrownBy(() -> new CertificateRetriever(mockHttpClient, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("certificateCacheTimeout must not be null");
    }

    // ========== Certificate URL Validation Tests ==========

    /**
     * Tests that certificate retrieval properly validates null URL parameter.
     * 
     * <p>This test ensures that the {@link CertificateRetriever#retrieveCertificate(String)}
     * method performs proper null checking on the certificateUrl parameter and throws a
     * {@link NullPointerException} with a descriptive error message when null is provided.
     * 
     * <p>This validation is critical for preventing null pointer exceptions during URL
     * processing and ensuring that callers receive clear feedback about invalid parameters.
     * 
     * @throws NullPointerException Expected exception when certificateUrl parameter is null
     */
    @Test
    void retrieveCertificate_nullUrl_throwsException() {
        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("certificateUrl must not be null");
    }

    /**
     * Tests that certificate retrieval rejects empty URL strings.
     * 
     * <p>This test verifies that empty strings are properly detected and rejected
     * with an appropriate {@link SnsCertificateException}. Empty URLs cannot be
     * processed for certificate retrieval.
     * 
     * @throws SnsCertificateException Expected exception when URL is empty
     */
    @Test
    void retrieveCertificate_emptyUrl_throwsException() {
        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(""))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Certificate URL cannot be null or empty");
    }

    /**
     * Tests that certificate retrieval rejects blank URL strings (whitespace only).
     * 
     * <p>This test verifies that URLs containing only whitespace characters are
     * properly detected and rejected. Such URLs are effectively empty and cannot
     * be used for certificate retrieval.
     * 
     * @throws SnsCertificateException Expected exception when URL contains only whitespace
     */
    @Test
    void retrieveCertificate_blankUrl_throwsException() {
        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate("   "))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Certificate URL cannot be null or empty");
    }

    /**
     * Tests that certificate retrieval rejects malformed URLs.
     * 
     * <p>This test verifies that URLs that don't conform to valid URL syntax
     * are properly detected and rejected with an appropriate error message.
     * This prevents attempts to retrieve certificates from invalid locations.
     * 
     * @throws SnsCertificateException Expected exception when URL format is invalid
     */
    @Test
    void retrieveCertificate_invalidUrlFormat_throwsException() {
        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate("not-a-valid-url"))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Certificate URL must use HTTPS");
    }

    /**
     * Tests that certificate retrieval enforces HTTPS-only policy.
     * 
     * <p>This test verifies that HTTP URLs are rejected to ensure certificate
     * retrieval only occurs over secure connections. This is a critical security
     * requirement to prevent man-in-the-middle attacks on certificate retrieval.
     * 
     * @throws SnsCertificateException Expected exception when URL uses HTTP instead of HTTPS
     */
    @Test
    void retrieveCertificate_httpUrl_throwsException() {
        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate("http://sns.us-east-1.amazonaws.com/cert.pem"))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Certificate URL must use HTTPS");
    }

    /**
     * Tests that certificate retrieval rejects URLs from untrusted domains.
     * 
     * <p>This test verifies that only URLs from trusted SNS domains are accepted
     * for certificate retrieval. This prevents attackers from providing certificates
     * from malicious domains that could be used to forge SNS messages.
     * 
     * @throws SnsCertificateException Expected exception when URL is from untrusted domain
     */
    @Test
    void retrieveCertificate_untrustedDomain_throwsException() {
        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate("https://malicious.com/cert.pem"))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Certificate URL is not from a trusted SNS domain");
    }

    /**
     * Tests that certificate retrieval accepts URLs from all trusted SNS domains.
     * 
     * <p>This parameterized test verifies that certificate URLs from legitimate SNS domains
     * across different AWS partitions are accepted for certificate retrieval. The test
     * covers standard AWS regions, GovCloud regions, and China regions.
     * 
     * <p>Trusted domains include:
     * <ul>
     *   <li>Standard AWS regions: *.amazonaws.com</li>
     *   <li>GovCloud regions: *.amazonaws.com</li>
     *   <li>China regions: *.amazonaws.com.cn</li>
     * </ul>
     * 
     * @param validUrl A valid certificate URL from a trusted SNS domain
     * @throws Exception If certificate retrieval fails unexpectedly
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "https://sns.us-east-1.amazonaws.com/cert.pem",
        "https://sns.eu-west-1.amazonaws.com/cert.pem", 
        "https://sns.ap-southeast-2.amazonaws.com/cert.pem",
        "https://sns.us-gov-west-1.amazonaws.com/cert.pem",
        "https://sns.us-gov-east-1.amazonaws.com/cert.pem",
        "https://sns.cn-north-1.amazonaws.com.cn/cert.pem",
        "https://sns.cn-northwest-1.amazonaws.com.cn/cert.pem"
    })
    void retrieveCertificate_validTrustedDomains_acceptsUrl(String validUrl) throws Exception {
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));
        
        byte[] result = certificateRetriever.retrieveCertificate(validUrl);
        
        assertThat(result).isNotNull();
        assertThat(new String(result, StandardCharsets.UTF_8)).contains("-----BEGIN CERTIFICATE-----");
    }

    /**
     * Tests that certificate retrieval rejects URLs from various untrusted domains.
     * 
     * <p>This parameterized test verifies that certificate URLs that appear similar to
     * legitimate SNS domains but are actually malicious or malformed are properly rejected.
     * This includes subdomain spoofing, domain spoofing, and malformed domain patterns.
     * 
     * <p>The test covers various attack vectors:
     * <ul>
     *   <li>Subdomain spoofing (fake-sns.us-east-1.amazonaws.com)</li>
     *   <li>Region spoofing (sns.fake-region.amazonaws.com)</li>
     *   <li>Domain spoofing (sns.us-east-1.fake.com)</li>
     *   <li>TLD spoofing (sns.us-east-1.amazonaws.com.fake)</li>
     *   <li>Malformed domains with extra dots or hyphens</li>
     * </ul>
     * 
     * @param invalidUrl An invalid certificate URL from an untrusted domain
     * @throws SnsCertificateException Expected exception when URL is from untrusted domain
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "https://fake-sns.us-east-1.amazonaws.com/cert.pem",
        "https://sns.us-east-1.fake.com/cert.pem",
        "https://sns.us-east-1.amazonaws.com.fake/cert.pem",
        "https://malicious.amazonaws.com/cert.pem",
        "https://sns..amazonaws.com/cert.pem",
        "https://sns.us-east-1-.amazonaws.com/cert.pem",
        "https://sns.-us-east-1.amazonaws.com/cert.pem"
    })
    void retrieveCertificate_invalidTrustedDomains_throwsException(String invalidUrl) {
        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(invalidUrl))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Certificate URL is not from a trusted SNS domain");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://sns.fake-region.amazonaws.com/cert.pem"
    })
    void retrieveCertificate_validFormatButInvalidRegion_throwsException(String invalidUrl) {
        // These URLs pass the domain pattern validation but fail during HTTP request
        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(invalidUrl))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate");
    }

    // ========== HTTP Response and Network Error Tests ==========

    /**
     * Tests that certificate retrieval handles HTTP error responses appropriately.
     * 
     * <p>This test verifies that when the HTTP request for certificate retrieval
     * returns an error status code (such as 404 Not Found), the retriever throws
     * an appropriate {@link SnsCertificateException} with details about the HTTP error.
     * 
     * <p>This ensures that network-level failures are properly reported to callers
     * with sufficient context for debugging and error handling.
     * 
     * @throws SnsCertificateException Expected exception when HTTP request fails
     */
    @Test
    void retrieveCertificate_httpError_throwsException() throws Exception {
        // Setup HTTP error response
        SdkHttpResponse errorResponse = SdkHttpResponse.builder()
            .statusCode(404)
            .build();
        
        HttpExecuteResponse httpResponse = mock(HttpExecuteResponse.class);
        when(httpResponse.httpResponse()).thenReturn(errorResponse);
        
        ExecutableHttpRequest executableRequest = mock(ExecutableHttpRequest.class);
        when(executableRequest.call()).thenReturn(httpResponse);
        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(executableRequest);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate")
            .hasCauseInstanceOf(SnsCertificateException.class);
    }

    @Test
    void retrieveCertificate_ioException_throwsException() throws Exception {
        ExecutableHttpRequest executableRequest = mock(ExecutableHttpRequest.class);
        when(executableRequest.call()).thenThrow(new IOException("Network error"));
        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(executableRequest);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("IO error while retrieving certificate")
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void retrieveCertificate_unexpectedException_throwsException() throws Exception {
        ExecutableHttpRequest executableRequest = mock(ExecutableHttpRequest.class);
        when(executableRequest.call()).thenThrow(new RuntimeException("Unexpected error"));
        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(executableRequest);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void retrieveCertificate_emptyResponseBody_throwsException() throws Exception {
        SdkHttpResponse successResponse = SdkHttpResponse.builder()
            .statusCode(200)
            .build();
        
        HttpExecuteResponse httpResponse = mock(HttpExecuteResponse.class);
        when(httpResponse.httpResponse()).thenReturn(successResponse);
        when(httpResponse.responseBody()).thenReturn(Optional.empty());
        
        ExecutableHttpRequest executableRequest = mock(ExecutableHttpRequest.class);
        when(executableRequest.call()).thenReturn(httpResponse);
        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(executableRequest);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate");
    }

    @Test
    void retrieveCertificate_emptyCertificate_throwsException() throws Exception {
        setupSuccessfulHttpResponse(new byte[0]);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate");
    }

    // ========== Certificate Content Validation Tests ==========

    /**
     * Tests that certificate retrieval rejects certificates that are too small to be valid.
     * 
     * <p>This test verifies that certificates smaller than the minimum expected size
     * for a valid X.509 certificate are rejected. This helps prevent processing of
     * malformed or truncated certificate data that could cause parsing errors.
     * 
     * <p>Valid X.509 certificates, even minimal ones, should be at least 100 bytes
     * due to the required ASN.1 structure and metadata.
     * 
     * @throws SnsCertificateException Expected exception when certificate is too small
     */
    @Test
    void retrieveCertificate_tooSmallCertificate_throwsException() throws Exception {
        byte[] tooSmallCert = "small".getBytes(StandardCharsets.UTF_8);
        setupSuccessfulHttpResponse(tooSmallCert);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate")
            .hasCauseInstanceOf(SnsCertificateException.class);
    }

    @Test
    void retrieveCertificate_oversizedCertificate_throwsException() throws Exception {
        // Create a certificate larger than 10KB
        byte[] oversizedCert = new byte[11 * 1024];
        // Fill with valid PEM header to pass format validation
        String pemHeader = "-----BEGIN CERTIFICATE-----\n";
        System.arraycopy(pemHeader.getBytes(StandardCharsets.UTF_8), 0, oversizedCert, 0, pemHeader.length());
        
        setupSuccessfulHttpResponse(oversizedCert);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate")
            .hasCauseInstanceOf(SnsCertificateException.class);
    }

    @Test
    void retrieveCertificate_invalidCertificateFormat_throwsException() throws Exception {
        byte[] invalidCert = "This is not a valid certificate format".getBytes(StandardCharsets.UTF_8);
        // Make it large enough to pass size validation
        byte[] paddedInvalidCert = new byte[200];
        System.arraycopy(invalidCert, 0, paddedInvalidCert, 0, invalidCert.length);
        
        setupSuccessfulHttpResponse(paddedInvalidCert);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate")
            .hasCauseInstanceOf(SnsCertificateException.class);
    }

    @Test
    void retrieveCertificate_certificateWithExcessiveNullBytes_throwsException() throws Exception {
        // Create certificate with too many null bytes (over 10% of content)
        byte[] certWithNulls = new byte[1000];
        String pemHeader = "-----BEGIN CERTIFICATE-----\n";
        System.arraycopy(pemHeader.getBytes(StandardCharsets.UTF_8), 0, certWithNulls, 0, pemHeader.length());
        // Fill rest with null bytes (over 10% threshold)
        
        setupSuccessfulHttpResponse(certWithNulls);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate")
            .hasCauseInstanceOf(SnsCertificateException.class);
    }

    @Test
    void retrieveCertificate_certificateWithConsecutiveNullBytes_throwsException() throws Exception {
        // Create certificate with too many consecutive null bytes
        byte[] certWithConsecutiveNulls = new byte[200];
        String pemHeader = "-----BEGIN CERTIFICATE-----\n";
        System.arraycopy(pemHeader.getBytes(StandardCharsets.UTF_8), 0, certWithConsecutiveNulls, 0, pemHeader.length());
        // Add 51 consecutive null bytes starting after the header
        for (int i = pemHeader.length(); i < pemHeader.length() + 51; i++) {
            certWithConsecutiveNulls[i] = 0;
        }
        // Fill rest with non-null data
        for (int i = pemHeader.length() + 51; i < certWithConsecutiveNulls.length; i++) {
            certWithConsecutiveNulls[i] = 'A';
        }
        
        setupSuccessfulHttpResponse(certWithConsecutiveNulls);

        assertThatThrownBy(() -> certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Unexpected error while retrieving certificate")
            .hasCauseInstanceOf(SnsCertificateException.class);
    }

    @Test
    void retrieveCertificate_validPemCertificate_succeeds() throws Exception {
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));

        byte[] result = certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);

        assertThat(result).isNotNull();
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo(VALID_PEM_CERTIFICATE);
    }

    @Test
    void retrieveCertificate_validDerCertificate_succeeds() throws Exception {
        setupSuccessfulHttpResponse(VALID_DER_CERTIFICATE);

        byte[] result = certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(VALID_DER_CERTIFICATE);
    }   
    // ========== Certificate Caching Functionality Tests ==========

    /**
     * Tests that certificate caching works correctly for repeated requests to the same URL.
     * 
     * <p>This test verifies that when the same certificate URL is requested multiple times,
     * the certificate is retrieved from the HTTP endpoint only once and subsequent requests
     * are served from the cache. This improves performance and reduces network traffic.
     * 
     * <p>The test confirms:
     * <ul>
     *   <li>Both requests return identical certificate data</li>
     *   <li>HTTP client is called only once despite multiple requests</li>
     *   <li>Cache hit behavior works as expected</li>
     * </ul>
     * 
     * @throws Exception If certificate retrieval fails unexpectedly
     */
    @Test
    void retrieveCertificate_cacheHit_returnsFromCache() throws Exception {
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));

        // First call should fetch from HTTP
        byte[] result1 = certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);
        
        // Second call should return from cache without HTTP call
        byte[] result2 = certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);

        assertThat(result1).isEqualTo(result2);
        // Verify HTTP client was called only once
        verify(mockHttpClient, times(1)).prepareRequest(any(HttpExecuteRequest.class));
    }

    @Test
    void retrieveCertificate_differentUrls_cachesIndependently() throws Exception {
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));

        // Retrieve certificates from different URLs
        byte[] result1 = certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);
        byte[] result2 = certificateRetriever.retrieveCertificate(VALID_CERT_URL_EU_WEST_1);

        assertThat(result1).isEqualTo(result2); // Same content but different cache entries
        // Verify HTTP client was called twice (once for each URL)
        verify(mockHttpClient, times(2)).prepareRequest(any(HttpExecuteRequest.class));
        
        // Verify cache has both entries
        assertThat(certificateRetriever.getCacheSize()).isEqualTo(2);
    }

    @Test
    void retrieveCertificate_expiredCache_refetchesCertificate() throws Exception {
        // Create retriever with very short cache timeout
        CertificateRetriever shortCacheRetriever = new CertificateRetriever(mockHttpClient, Duration.ofMillis(10));
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));

        // First call
        byte[] result1 = shortCacheRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);
        
        // Wait for cache to expire
        Thread.sleep(20);
        
        // Second call should refetch
        byte[] result2 = shortCacheRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);

        assertThat(result1).isEqualTo(result2);
        // Verify HTTP client was called twice due to cache expiration
        verify(mockHttpClient, times(2)).prepareRequest(any(HttpExecuteRequest.class));
    }

    @Test
    void clearCache_removesAllCachedCertificates() throws Exception {
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));

        // Cache some certificates
        certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);
        certificateRetriever.retrieveCertificate(VALID_CERT_URL_EU_WEST_1);
        
        assertThat(certificateRetriever.getCacheSize()).isEqualTo(2);
        
        // Clear cache
        certificateRetriever.clearCache();
        
        assertThat(certificateRetriever.getCacheSize()).isEqualTo(0);
    }

    @Test
    void getCacheSize_returnsCorrectSize() throws Exception {
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));

        assertThat(certificateRetriever.getCacheSize()).isEqualTo(0);
        
        certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);
        assertThat(certificateRetriever.getCacheSize()).isEqualTo(1);
        
        certificateRetriever.retrieveCertificate(VALID_CERT_URL_EU_WEST_1);
        assertThat(certificateRetriever.getCacheSize()).isEqualTo(2);
        
        // Same URL should not increase cache size
        certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);
        assertThat(certificateRetriever.getCacheSize()).isEqualTo(2);
    }

    // Thread-safety tests
    @Test
    void retrieveCertificate_concurrentAccess_threadSafe() throws Exception {
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));
        
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Submit concurrent tasks
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    byte[] result = certificateRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);
                    if (result != null && result.length > 0) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
        
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(errorCount.get()).isEqualTo(0);
        
        // Verify cache is thread-safe and contains only one entry
        assertThat(certificateRetriever.getCacheSize()).isEqualTo(1);
        
        executor.shutdown();
    }

    @Test
    void retrieveCertificate_concurrentDifferentUrls_threadSafe() throws Exception {
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));
        
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        String[] urls = {
            VALID_CERT_URL_US_EAST_1,
            VALID_CERT_URL_EU_WEST_1,
            VALID_CERT_URL_GOV_CLOUD,
            VALID_CERT_URL_CHINA
        };

        // Submit concurrent tasks with different URLs
        for (int i = 0; i < threadCount; i++) {
            final String url = urls[i % urls.length];
            executor.submit(() -> {
                try {
                    startLatch.await();
                    byte[] result = certificateRetriever.retrieveCertificate(url);
                    if (result != null && result.length > 0) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore for this test
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
        
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        
        // Should have cached all unique URLs
        assertThat(certificateRetriever.getCacheSize()).isEqualTo(urls.length);
        
        executor.shutdown();
    }

    @Test
    void retrieveCertificate_concurrentCacheExpiration_threadSafe() throws Exception {
        // Create retriever with short cache timeout for this test
        CertificateRetriever shortCacheRetriever = new CertificateRetriever(mockHttpClient, Duration.ofMillis(50));
        setupSuccessfulHttpResponse(VALID_PEM_CERTIFICATE.getBytes(StandardCharsets.UTF_8));
        
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit tasks that will run over time to test cache expiration
        for (int i = 0; i < threadCount; i++) {
            final int delay = i * 10; // Stagger the requests
            executor.submit(() -> {
                try {
                    Thread.sleep(delay);
                    byte[] result = shortCacheRetriever.retrieveCertificate(VALID_CERT_URL_US_EAST_1);
                    if (result != null && result.length > 0) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore for this test
                }
            });
        }

        executor.shutdown();
        boolean terminated = executor.awaitTermination(2, TimeUnit.SECONDS);
        
        assertThat(terminated).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    // ========== Test Helper Methods ==========

    /**
     * Sets up a successful HTTP response mock for certificate retrieval testing.
     * 
     * <p>This helper method configures the mock HTTP client to return a successful
     * HTTP 200 response with the provided certificate bytes as the response body.
     * This allows tests to focus on certificate processing logic without dealing
     * with actual HTTP communication.
     * 
     * <p>The mock setup includes:
     * <ul>
     *   <li>HTTP 200 status code response</li>
     *   <li>Certificate bytes wrapped in an AbortableInputStream</li>
     *   <li>Proper mock chaining for HTTP client execution</li>
     * </ul>
     * 
     * @param certificateBytes The certificate data to return in the HTTP response body
     * @throws Exception If there are issues setting up the mock HTTP response
     */
    private void setupSuccessfulHttpResponse(byte[] certificateBytes) throws Exception {
        SdkHttpResponse successResponse = SdkHttpResponse.builder()
            .statusCode(200)
            .build();
        
        HttpExecuteResponse httpResponse = mock(HttpExecuteResponse.class);
        when(httpResponse.httpResponse()).thenReturn(successResponse);
        // Create a new stream for each call to handle concurrent access
        when(httpResponse.responseBody()).thenAnswer(invocation -> 
            Optional.of(AbortableInputStream.create(new ByteArrayInputStream(certificateBytes))));
        
        ExecutableHttpRequest executableRequest = mock(ExecutableHttpRequest.class);
        when(executableRequest.call()).thenReturn(httpResponse);
        
        // Make the mock thread-safe by using lenient stubbing
        lenient().when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class)))
                .thenReturn(executableRequest);
    }
}
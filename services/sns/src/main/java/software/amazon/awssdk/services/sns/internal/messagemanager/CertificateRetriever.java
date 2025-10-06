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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.sns.messagemanager.SnsCertificateException;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal certificate retriever for SNS message validation.
 *
 * <p>This class handles secure retrieval and caching of SNS signing certificates from AWS.
 * It implements comprehensive security validations to ensure certificate authenticity and
 * prevent various attack vectors including certificate spoofing and man-in-the-middle attacks.
 *
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>HTTPS-only certificate retrieval to prevent interception attacks</li>
 *   <li>Certificate URL validation against known SNS-signed domains</li>
 *   <li>Support for different AWS partitions (aws, aws-gov, aws-cn)</li>
 *   <li>Thread-safe certificate caching with configurable TTL</li>
 *   <li>Protection against certificate spoofing attacks</li>
 *   <li>Certificate size validation to prevent resource exhaustion</li>
 * </ul>
 *
 * <p><strong>Trusted Domains:</strong>
 * The retriever only accepts certificates from pre-validated SNS domains including:
 * <ul>
 *   <li>Standard AWS regions: {@code sns.*.amazonaws.com}</li>
 *   <li>AWS GovCloud: {@code sns.*.amazonaws.com}</li>
 *   <li>AWS China: {@code sns.*.amazonaws.com.cn}</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong>
 * This class is thread-safe and can be used concurrently from multiple threads.
 * Certificate caching is implemented using thread-safe collections.
 *
 * <p><strong>Usage:</strong>
 * This class is intended for internal use by the SNS message manager and should not be
 * used directly by client code. Certificates are automatically retrieved and cached
 * during message signature validation.
 *
 * @see SignatureValidator
 * @see DefaultSnsMessageManager
 */
@SdkInternalApi
public final class CertificateRetriever {

    // Trusted SNS domain patterns for different AWS partitions
    private static final Pattern[] TRUSTED_SNS_DOMAIN_PATTERNS = {
        // AWS Standard partition: sns.<region>.amazonaws.com
        Pattern.compile("^sns\\.[a-z0-9][a-z0-9\\-]*[a-z0-9]\\.amazonaws\\.com$"),
        
        // AWS GovCloud partition: sns.us-gov-<region>.amazonaws.com  
        Pattern.compile("^sns\\.us-gov-[a-z0-9][a-z0-9\\-]*[a-z0-9]\\.amazonaws\\.com$"),
        
        // AWS China partition: sns.cn-<region>.amazonaws.com.cn
        Pattern.compile("^sns\\.cn-[a-z0-9][a-z0-9\\-]*[a-z0-9]\\.amazonaws\\.com\\.cn$")
    };

    private static final String HTTPS_SCHEME = "https";
    private static final int MAX_CERTIFICATE_SIZE = 10 * 1024; // 10KB max certificate size
    private static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(10);

    private final SdkHttpClient httpClient;
    private final Duration certificateCacheTimeout;
    private final ConcurrentMap<String, CachedCertificate> certificateCache;

    /**
     * Creates a new certificate retriever with the specified configuration.
     *
     * @param httpClient The HTTP client to use for certificate retrieval.
     * @param certificateCacheTimeout The cache timeout for certificates.
     * @throws NullPointerException If httpClient or certificateCacheTimeout is null.
     */
    public CertificateRetriever(SdkHttpClient httpClient, Duration certificateCacheTimeout) {
        this.httpClient = Validate.paramNotNull(httpClient, "httpClient");
        this.certificateCacheTimeout = Validate.paramNotNull(certificateCacheTimeout, "certificateCacheTimeout");
        this.certificateCache = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves a certificate from the specified URL with security validation.
     * <p>
     * This method performs comprehensive security checks:
     * <ul>
     *   <li>Validates the certificate URL against trusted SNS domains</li>
     *   <li>Ensures HTTPS-only retrieval</li>
     *   <li>Implements certificate caching with TTL</li>
     *   <li>Protects against oversized certificates</li>
     * </ul>
     *
     * @param certificateUrl The URL of the certificate to retrieve.
     * @return The certificate bytes.
     * @throws SnsCertificateException If certificate retrieval or validation fails.
     * @throws NullPointerException If certificateUrl is null.
     */
    public byte[] retrieveCertificate(String certificateUrl) {
        Validate.paramNotNull(certificateUrl, "certificateUrl");
        
        // Check cache first
        CachedCertificate cached = certificateCache.get(certificateUrl);
        if (cached != null && !cached.isExpired()) {
            return cached.getCertificateBytes();
        }
        
        // Validate certificate URL security
        validateCertificateUrl(certificateUrl);
        
        // Retrieve certificate from AWS
        byte[] certificateBytes = fetchCertificateFromUrl(certificateUrl);
        
        // Cache the certificate
        certificateCache.put(certificateUrl, new CachedCertificate(certificateBytes, certificateCacheTimeout));
        
        return certificateBytes;
    }

    /**
     * Validates that the certificate URL is from a trusted SNS domain and uses HTTPS.
     *
     * @param certificateUrl The certificate URL to validate.
     * @throws SnsCertificateException If the URL is not trusted or secure.
     */
    private void validateCertificateUrl(String certificateUrl) {
        if (StringUtils.isBlank(certificateUrl)) {
            throw SnsCertificateException.builder()
                .message("Certificate URL cannot be null or empty")
                .build();
        }

        URI uri;
        try {
            uri = new URI(certificateUrl);
        } catch (URISyntaxException e) {
            throw SnsCertificateException.builder()
                .message("Invalid certificate URL format: " + certificateUrl)
                .cause(e)
                .build();
        }

        // Ensure HTTPS only
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw SnsCertificateException.builder()
                .message("Certificate URL must use HTTPS. Provided URL: " + certificateUrl)
                .build();
        }

        // Validate against trusted SNS domain patterns
        String host = uri.getHost();
        if (host == null || !isTrustedSnsDomain(host)) {
            throw SnsCertificateException.builder()
                .message("Certificate URL is not from a trusted SNS domain. Host: " + host + 
                        ". Expected format: sns.<region>.amazonaws.com, sns.us-gov-<region>.amazonaws.com, " +
                        "or sns.cn-<region>.amazonaws.com.cn")
                .build();
        }
    }

    /**
     * Checks if the given host is a trusted SNS domain using pattern matching.
     * <p>
     * This method validates against known AWS SNS domain patterns for all partitions:
     * <ul>
     *   <li>AWS Standard: sns.&lt;region&gt;.amazonaws.com</li>
     *   <li>AWS GovCloud: sns.us-gov-&lt;region&gt;.amazonaws.com</li>
     *   <li>AWS China: sns.cn-&lt;region&gt;.amazonaws.com.cn</li>
     * </ul>
     * <p>
     * The patterns ensure that:
     * <ul>
     *   <li>Only valid region names are accepted (alphanumeric and hyphens, not starting/ending with hyphen)</li>
     *   <li>The domain structure matches AWS SNS certificate hosting patterns</li>
     *   <li>New regions are automatically supported without code changes</li>
     * </ul>
     *
     * @param host The host to check.
     * @return true if the host matches a trusted SNS domain pattern, false otherwise.
     */
    private boolean isTrustedSnsDomain(String host) {
        if (host == null) {
            return false;
        }
        
        // Convert to lowercase for case-insensitive matching
        String normalizedHost = host.toLowerCase();
        
        // Check against all trusted SNS domain patterns
        for (Pattern pattern : TRUSTED_SNS_DOMAIN_PATTERNS) {
            if (pattern.matcher(normalizedHost).matches()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Fetches the certificate from the specified URL.
     *
     * @param certificateUrl The URL to fetch the certificate from.
     * @return The certificate bytes.
     * @throws SnsCertificateException If certificate retrieval fails.
     */
    private byte[] fetchCertificateFromUrl(String certificateUrl) {
        SdkHttpRequest httpRequest = SdkHttpRequest.builder()
            .method(SdkHttpMethod.GET)
            .uri(URI.create(certificateUrl))
            .build();

        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
            .request(httpRequest)
            .build();

        try {
            HttpExecuteResponse response = httpClient.prepareRequest(executeRequest).call();
            
            if (!response.httpResponse().isSuccessful()) {
                throw SnsCertificateException.builder()
                    .message("Failed to retrieve certificate from URL: " + certificateUrl + 
                            ". HTTP status: " + response.httpResponse().statusCode())
                    .build();
            }

            return readCertificateBytes(response);
            
        } catch (IOException e) {
            throw SnsCertificateException.builder()
                .message("IO error while retrieving certificate from URL: " + certificateUrl)
                .cause(e)
                .build();
        } catch (Exception e) {
            throw SnsCertificateException.builder()
                .message("Unexpected error while retrieving certificate from URL: " + certificateUrl)
                .cause(e)
                .build();
        }
    }

    /**
     * Reads certificate bytes from the HTTP response with comprehensive validation.
     *
     * @param response The HTTP response containing the certificate.
     * @return The certificate bytes.
     * @throws IOException If reading fails.
     * @throws SnsCertificateException If certificate validation fails.
     */
    private byte[] readCertificateBytes(HttpExecuteResponse response) throws IOException {
        try (InputStream inputStream = response.responseBody().orElseThrow(
                () -> SnsCertificateException.builder()
                    .message("Certificate response body is empty")
                    .build())) {
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int totalBytesRead = 0;
            int bytesRead;
            
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                totalBytesRead += bytesRead;
                
                // Protect against oversized certificates
                if (totalBytesRead > MAX_CERTIFICATE_SIZE) {
                    throw SnsCertificateException.builder()
                        .message("Certificate size exceeds maximum allowed size of " + MAX_CERTIFICATE_SIZE + " bytes")
                        .build();
                }
                
                buffer.write(chunk, 0, bytesRead);
            }
            
            byte[] certificateBytes = buffer.toByteArray();
            
            if (certificateBytes.length == 0) {
                throw SnsCertificateException.builder()
                    .message("Retrieved certificate is empty")
                    .build();
            }
            
            // Perform additional security validation on certificate content
            validateCertificateContent(certificateBytes);
            
            return certificateBytes;
        }
    }

    /**
     * Validates the certificate content for security compliance.
     * <p>
     * This method performs additional security checks on the certificate content
     * to ensure it meets security requirements and is not malformed or malicious.
     *
     * @param certificateBytes The certificate bytes to validate.
     * @throws SnsCertificateException If certificate content validation fails.
     */
    private void validateCertificateContent(byte[] certificateBytes) {
        // Check for minimum certificate size (too small indicates potential issues)
        if (certificateBytes.length < 100) {
            throw SnsCertificateException.builder()
                .message("Certificate is too small (" + certificateBytes.length + " bytes). " +
                        "Valid X.509 certificates should be at least 100 bytes")
                .build();
        }
        
        // Validate certificate starts with expected X.509 PEM or DER format markers
        if (!isValidCertificateFormat(certificateBytes)) {
            throw SnsCertificateException.builder()
                .message("Certificate does not appear to be in valid X.509 PEM or DER format")
                .build();
        }
        
        // Check for suspicious content patterns that might indicate tampering
        validateCertificateIntegrity(certificateBytes);
    }

    /**
     * Validates that the certificate is in a recognized X.509 format.
     *
     * @param certificateBytes The certificate bytes to check.
     * @return true if the format appears valid, false otherwise.
     */
    private boolean isValidCertificateFormat(byte[] certificateBytes) {
        if (certificateBytes.length < 10) {
            return false;
        }
        
        // Check for PEM format (starts with "-----BEGIN CERTIFICATE-----")
        String beginPem = "-----BEGIN CERTIFICATE-----";
        if (certificateBytes.length >= beginPem.length()) {
            String start = new String(certificateBytes, 0, beginPem.length(), StandardCharsets.US_ASCII);
            if (beginPem.equals(start)) {
                return true;
            }
        }
        
        // Check for DER format (starts with ASN.1 SEQUENCE tag 0x30)
        if (certificateBytes[0] == 0x30) {
            // Basic DER validation - second byte should indicate length encoding
            if (certificateBytes.length > 1) {
                byte lengthByte = certificateBytes[1];
                // Length byte should be reasonable for certificate size
                return (lengthByte & 0x80) == 0 || (lengthByte & 0x7F) <= 4;
            }
        }
        
        return false;
    }

    /**
     * Validates certificate integrity by checking for suspicious patterns.
     *
     * @param certificateBytes The certificate bytes to validate.
     * @throws SnsCertificateException If suspicious patterns are detected.
     */
    private void validateCertificateIntegrity(byte[] certificateBytes) {
        // Check for excessive null bytes which might indicate padding attacks
        int nullByteCount = 0;
        int consecutiveNullBytes = 0;
        int maxConsecutiveNullBytes = 0;
        
        for (byte b : certificateBytes) {
            if (b == 0) {
                nullByteCount++;
                consecutiveNullBytes++;
                maxConsecutiveNullBytes = Math.max(maxConsecutiveNullBytes, consecutiveNullBytes);
            } else {
                consecutiveNullBytes = 0;
            }
        }
        
        // If more than 10% of the certificate is null bytes, it's suspicious
        if (nullByteCount > certificateBytes.length * 0.1) {
            throw SnsCertificateException.builder()
                .message("Certificate contains excessive null bytes (" + nullByteCount + " out of " + 
                        certificateBytes.length + "), which may indicate tampering")
                .build();
        }
        
        // If there are more than 50 consecutive null bytes, it's suspicious
        if (maxConsecutiveNullBytes > 50) {
            throw SnsCertificateException.builder()
                .message("Certificate contains " + maxConsecutiveNullBytes + 
                        " consecutive null bytes, which may indicate tampering")
                .build();
        }
    }

    /**
     * Clears the certificate cache.
     * <p>
     * This method is primarily intended for testing purposes.
     */
    void clearCache() {
        certificateCache.clear();
    }

    /**
     * Returns the current cache size.
     * <p>
     * This method is primarily intended for testing purposes.
     *
     * @return The number of cached certificates.
     */
    int getCacheSize() {
        return certificateCache.size();
    }

    /**
     * Cached certificate with expiration time.
     */
    private static final class CachedCertificate {
        private final byte[] certificateBytes;
        private final Instant expirationTime;

        CachedCertificate(byte[] certificateBytes, Duration cacheTimeout) {
            this.certificateBytes = certificateBytes.clone();
            this.expirationTime = Instant.now().plus(cacheTimeout);
        }

        byte[] getCertificateBytes() {
            return certificateBytes.clone();
        }

        boolean isExpired() {
            return Instant.now().isAfter(expirationTime);
        }
    }
}
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.HttpClientHostnameVerifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.lru.LruCache;

/**
 * Internal certificate retriever for SNS message validation.
 * <p>
 * This class retrieves the certificate used to sign a message, validates it, and caches them for future use.
 */
@SdkInternalApi
public class CertificateRetriever implements SdkAutoCloseable {
    private static final Lazy<Pattern> X509_FORMAT = new Lazy<>(() ->
        Pattern.compile(
            "^[\\s]*-----BEGIN [A-Z]+-----\\n[A-Za-z\\d+\\/\\n]+[=]{0,2}\\n-----END [A-Z]+-----[\\s]*$"));

    private final HttpClientHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();

    private final SdkHttpClient httpClient;
    private final String certCommonName;
    private final CertificateUrlValidator certUrlValidator;
    private final LruCache<URI, PublicKey> certificateCache;

    public CertificateRetriever(SdkHttpClient httpClient, String certHost, String certCommonName) {
        this(httpClient, certCommonName, new CertificateUrlValidator(certHost));
    }

    CertificateRetriever(SdkHttpClient httpClient, String certCommonName, CertificateUrlValidator certificateUrlValidator) {
        this.httpClient = Validate.paramNotNull(httpClient, "httpClient");
        this.certCommonName = Validate.paramNotNull(certCommonName, "certCommonName");
        this.certificateCache = LruCache.builder(this::fetchCertificate)
                                        .maxSize(10)
                                        .build();
        this.certUrlValidator = Validate.paramNotNull(certificateUrlValidator, "certificateUrlValidator");
    }

    public PublicKey retrieveCertificate(URI certificateUrl) {
        Validate.paramNotNull(certificateUrl, "certificateUrl");
        certUrlValidator.validate(certificateUrl);
        return certificateCache.get(certificateUrl);
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private PublicKey fetchCertificate(URI certificateUrl) {
        byte[] cert = fetchUrl(certificateUrl);
        validateCertificateData(cert);
        return createPublicKey(cert);
    }

    private byte[] fetchUrl(URI certificateUrl) {
        SdkHttpRequest httpRequest = SdkHttpRequest.builder()
                                                   .method(SdkHttpMethod.GET)
                                                   .uri(certificateUrl)
                                                   .build();

        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                                                              .request(httpRequest)
                                                              .build();

        try {
            HttpExecuteResponse response = httpClient.prepareRequest(executeRequest).call();

            if (!response.httpResponse().isSuccessful()) {
                throw SdkClientException.create("Request was unsuccessful. Status Code: " + response.httpResponse().statusCode());
            }

            return readResponse(response);
        } catch (SdkClientException e) {
            throw e;
        } catch (Exception e) {
            throw SdkClientException.create("Unexpected error while retrieving URL: " + certificateUrl, e);
        }
    }

    private byte[] readResponse(HttpExecuteResponse response) throws IOException {
        try (InputStream inputStream = response.responseBody().orElseThrow(
            () -> SdkClientException.create("Response body is empty"))) {

            return IoUtils.toByteArray(inputStream);
        }
    }

    private PublicKey createPublicKey(byte[] cert) {
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            InputStream stream = new ByteArrayInputStream(cert);
            X509Certificate cer = (X509Certificate) fact.generateCertificate(stream);
            validateCertificate(cer, certCommonName);
            return cer.getPublicKey();
        } catch (CertificateExpiredException e) {
            throw SdkClientException.create("The certificate is expired", e);
        } catch (CertificateNotYetValidException e) {
            throw SdkClientException.create("The certificate is not yet valid", e);
        } catch (CertificateException e) {
            throw SdkClientException.create("The certificate could not be parsed", e);
        }
    }

    /**
     * Check that the certificate is valid and that the principal is actually SNS.
     */
    private void validateCertificate(X509Certificate cer, String expectedCommonName) throws CertificateExpiredException,
                                                                     CertificateNotYetValidException {
        verifyHostname(cer, expectedCommonName);
        cer.checkValidity();
    }

    private void verifyHostname(X509Certificate cer, String expectedCertCommonName) {
        try {
            hostnameVerifier.verify(expectedCertCommonName, cer);
        } catch (SSLException e) {
            throw SdkClientException.create("Certificate does not match expected common name: "
                                            + expectedCertCommonName, e);
        }
    }

    private void validateCertificateData(byte[] data) {
        Matcher m = X509_FORMAT.getValue().matcher(new String(data, StandardCharsets.UTF_8));
        if (!m.matches()) {
            throw SdkClientException.create("Certificate does not match expected X509 PEM format.");
        }
    }
}
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.HttpClientHostnameVerifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal certificate retriever for SNS message validation.
 * <p>
 * This class retrieves the certificate used to sign a message, validates it, and caches it for future use.
 * <p>
 * Certificates are cached by the SHA-256 fingerprint of their DER encoding; the {@code SigningCertURL} is used as a lookup
 * hint. Callers check {@link #cachedCertificates(URI)} first and call {@link #retrieveCertificate(URI)} when no cached
 * certificate verifies the message.
 * <p>
 * A fetch requires the URL to pass {@link CertificateUrlValidator}, and at most {@value #DEFAULT_MAX_CONCURRENT_FETCHES}
 * outbound fetches run at the same time. A fetch that cannot get a permit within {@link #DEFAULT_FETCH_PERMIT_TIMEOUT} is
 * refused. Concurrent fetches of the same URL are collapsed into one request; cache hits and waiters never take a permit.
 */
@SdkInternalApi
public class CertificateRetriever implements SdkAutoCloseable {
    static final int DEFAULT_MAX_CONCURRENT_FETCHES = 8;
    static final Duration DEFAULT_FETCH_PERMIT_TIMEOUT = Duration.ofSeconds(1);
    static final int MAX_CACHED_CERTIFICATES = 10;
    static final int MAX_URL_HINTS = 16;

    private static final Logger LOG = Logger.loggerFor(CertificateRetriever.class);

    private static final Lazy<Pattern> X509_FORMAT = new Lazy<>(() ->
        Pattern.compile(
            "^[\\s]*-----BEGIN [A-Z]+-----\\n[A-Za-z\\d+\\/\\n]+[=]{0,2}\\n-----END [A-Z]+-----[\\s]*$"));

    private final HttpClientHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();

    private final SdkHttpClient httpClient;
    private final String certCommonName;
    private final CertificateUrlValidator certUrlValidator;
    private final Semaphore fetchPermits;
    private final long fetchPermitTimeoutNanos;
    private final CertificateCache cache = new CertificateCache(MAX_CACHED_CERTIFICATES, MAX_URL_HINTS);
    private final ConcurrentHashMap<URI, FutureTask<PublicKey>> inFlight = new ConcurrentHashMap<>();

    public CertificateRetriever(SdkHttpClient httpClient, String certHost, String certCommonName) {
        this(httpClient, certCommonName, new CertificateUrlValidator(certHost));
    }

    CertificateRetriever(SdkHttpClient httpClient, String certCommonName, CertificateUrlValidator certificateUrlValidator) {
        this(httpClient, certCommonName, certificateUrlValidator, DEFAULT_MAX_CONCURRENT_FETCHES,
             DEFAULT_FETCH_PERMIT_TIMEOUT);
    }

    @SdkTestInternalApi
    CertificateRetriever(SdkHttpClient httpClient, String certCommonName, CertificateUrlValidator certificateUrlValidator,
                         int maxConcurrentFetches, Duration fetchPermitTimeout) {
        this.httpClient = Validate.paramNotNull(httpClient, "httpClient");
        this.certCommonName = Validate.paramNotNull(certCommonName, "certCommonName");
        this.certUrlValidator = Validate.paramNotNull(certificateUrlValidator, "certificateUrlValidator");
        this.fetchPermits = new Semaphore(Validate.isPositive(maxConcurrentFetches, "maxConcurrentFetches"));
        this.fetchPermitTimeoutNanos = Validate.isNotNegative(
            Validate.paramNotNull(fetchPermitTimeout, "fetchPermitTimeout"), "fetchPermitTimeout").toNanos();
    }

    /**
     * The validated certificates currently cached, ordered so that the one last fetched from {@code certificateUrl} comes
     * first. Makes no network request and takes no fetch permit. The URL is validated so that an invalid URL is rejected
     * with the same error whether or not the cache is empty.
     */
    public List<PublicKey> cachedCertificates(URI certificateUrl) {
        Validate.paramNotNull(certificateUrl, "certificateUrl");
        certUrlValidator.validate(certificateUrl);
        return cache.candidates(certificateUrl);
    }

    /**
     * The certificate this URL resolves to, fetching it if it is not already known.
     */
    public PublicKey retrieveCertificate(URI certificateUrl) {
        Validate.paramNotNull(certificateUrl, "certificateUrl");
        certUrlValidator.validate(certificateUrl);

        PublicKey known = cache.lookup(certificateUrl);
        if (known != null) {
            return known;
        }
        return fetchOnce(certificateUrl);
    }

    @SdkTestInternalApi
    int cachedCertificateCount() {
        return cache.size();
    }

    @Override
    public void close() {
        httpClient.close();
    }

    /**
     * Fetches the URL, collapsing concurrent calls for the same URL into a single request whose result all callers share.
     * The first caller runs the fetch on its own thread; later callers for the same URL wait for that result and never
     * take a fetch permit.
     */
    private PublicKey fetchOnce(URI certificateUrl) {
        FutureTask<PublicKey> mine = new FutureTask<>(() -> fetchAndCache(certificateUrl));
        FutureTask<PublicKey> task = inFlight.putIfAbsent(certificateUrl, mine);
        if (task == null) {
            task = mine;
            try {
                task.run();
            } finally {
                inFlight.remove(certificateUrl, mine);
            }
        }
        return awaitFetch(task, certificateUrl);
    }

    private static PublicKey awaitFetch(FutureTask<PublicKey> task, URI certificateUrl) {
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SdkClientException.create("Interrupted while waiting to retrieve certificate: " + certificateUrl, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw SdkClientException.create("Unexpected error while retrieving URL: " + certificateUrl, cause);
        }
    }

    private PublicKey fetchAndCache(URI certificateUrl) {
        // Re-check now that this caller owns the in-flight slot: another caller may have fetched and cached this URL between
        // the lookup in retrieveCertificate and the claim of the slot.
        PublicKey known = cache.lookup(certificateUrl);
        if (known != null) {
            return known;
        }

        byte[] pem = fetchUrlWithPermit(certificateUrl);
        validateCertificateData(pem);
        X509Certificate certificate = parseAndValidateCertificate(pem);
        return cache.putIfAbsent(certificateUrl, fingerprint(certificate), certificate.getPublicKey());
    }

    private byte[] fetchUrlWithPermit(URI certificateUrl) {
        acquireFetchPermit(certificateUrl);
        try {
            return fetchUrl(certificateUrl);
        } finally {
            fetchPermits.release();
        }
    }

    private void acquireFetchPermit(URI certificateUrl) {
        boolean acquired;
        try {
            acquired = fetchPermits.tryAcquire(fetchPermitTimeoutNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SdkClientException.create("Interrupted while waiting to retrieve certificate: " + certificateUrl, e);
        }
        if (!acquired) {
            throw SdkClientException.create("Too many concurrent certificate retrievals; refusing to fetch " + certificateUrl
                                            + ". Try again later.");
        }
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
                response.responseBody().ifPresent(body -> IoUtils.closeQuietlyV2(body, LOG));
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

    private X509Certificate parseAndValidateCertificate(byte[] cert) {
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            InputStream stream = new ByteArrayInputStream(cert);
            X509Certificate cer = (X509Certificate) fact.generateCertificate(stream);
            validateCertificate(cer, certCommonName);
            return cer;
        } catch (CertificateExpiredException e) {
            throw SdkClientException.create("The certificate is expired", e);
        } catch (CertificateNotYetValidException e) {
            throw SdkClientException.create("The certificate is not yet valid", e);
        } catch (CertificateException e) {
            throw SdkClientException.create("The certificate could not be parsed", e);
        }
    }

    /**
     * The SHA-256 of the certificate's DER encoding, which is the cache key.
     */
    private static String fingerprint(X509Certificate certificate) {
        try {
            return BinaryUtils.toHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
        } catch (CertificateException e) {
            throw SdkClientException.create("The certificate could not be parsed", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
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

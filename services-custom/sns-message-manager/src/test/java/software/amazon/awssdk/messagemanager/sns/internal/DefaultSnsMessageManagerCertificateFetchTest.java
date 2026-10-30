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
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.regions.Region;

/**
 * End-to-end checks of when {@link SnsMessageManager#parseMessage(String)} fetches a signing certificate: messages that fail
 * field validation are rejected without a fetch, a message signed with an already cached certificate validates without a
 * fetch regardless of the {@code SigningCertURL} it names, URLs that do not serve a valid certificate are not cached, and a
 * fetch that has not completed for one URL does not delay a fetch for a different URL.
 * <p>
 * Everything but the network is the real module code: the "SNS service" side is performed with the private keys in
 * {@link TestSigningCertificates}, and a stub {@link SdkHttpClient} serves the certificates and records every fetch.
 */
class DefaultSnsMessageManagerCertificateFetchTest {
    private static final String SNS_HOST = "sns.us-east-1.amazonaws.com";
    private static final String SIGNING_CERT_COMMON_NAME = "sns.amazonaws.com";
    private static final String FIRST_CERT_PATH = "/SimpleNotificationService-0123456789abcdef0123456789abcdef.pem";
    private static final String FIRST_CERT_URL = "https://" + SNS_HOST + FIRST_CERT_PATH;
    private static final String SECOND_CERT_PATH = "/SimpleNotificationService-fedcba9876543210fedcba9876543210.pem";
    private static final String SECOND_CERT_URL = "https://" + SNS_HOST + SECOND_CERT_PATH;

    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:order-events";
    private static final String TIMESTAMP = "2024-06-15T12:30:45.123Z";
    private static final String MESSAGE_ID = "8f1c2f2e-1111-4d4d-9e2b-9a0b1c2d3e4f";
    private static final String MESSAGE = "Your order 1001 has shipped.";
    private static final String ZERO_SIGNATURE = Base64.getEncoder().encodeToString(new byte[256]);

    private static PrivateKey keyA;
    private static PrivateKey keyB;

    private RecordingHttpClient httpClient;
    private CertificateRetriever retriever;
    private SnsMessageManager manager;

    @BeforeAll
    static void loadKeys() throws Exception {
        keyA = TestSigningCertificates.privateKey(TestSigningCertificates.KEY_A_PKCS8_B64);
        keyB = TestSigningCertificates.privateKey(TestSigningCertificates.KEY_B_PKCS8_B64);
    }

    @BeforeEach
    void setUp() {
        httpClient = new RecordingHttpClient(TestSigningCertificates.pem(TestSigningCertificates.CERT_A_DER_B64));
        // The same retriever DefaultSnsMessageManager builds for us-east-1, constructed here so the test can observe the
        // number of distinct certificates it has cached.
        retriever = new CertificateRetriever(httpClient, SNS_HOST, SIGNING_CERT_COMMON_NAME);
        manager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(retriever)
            .region(Region.US_EAST_1)
            .httpClient(httpClient)
            .build();
    }

    @AfterEach
    void tearDown() {
        manager.close();
    }

    @Test
    void parseMessage_shortSignatureDifferentUrls_noCertificateFetch() {
        for (int i = 0; i < 50; i++) {
            String message = unsignedMessage("https://" + SNS_HOST + "/other-" + i + ".pem", "AAAA");
            assertThatThrownBy(() -> manager.parseMessage(message)).isInstanceOf(SdkClientException.class);
        }

        assertThat(httpClient.fetched()).isEmpty();
    }

    @Test
    void parseMessage_repeatedShortSignature_noCertificateFetch() {
        String message = unsignedMessage("https://" + SNS_HOST + "/simple-notification-service/repeated.pem", "AAAA");

        for (int i = 0; i < 50; i++) {
            assertThatThrownBy(() -> manager.parseMessage(message)).isInstanceOf(SdkClientException.class);
        }

        assertThat(httpClient.fetched()).isEmpty();
    }

    @Test
    void parseMessage_differentUrlsSameCertificate_fetchesOnce() throws Exception {
        String signature = TestSigningCertificates.sign(canonical(MESSAGE), keyA, "SHA256withRSA");

        assertThat(manager.parseMessage(signedMessage(signature, FIRST_CERT_URL))).isInstanceOf(SnsNotification.class);
        assertThat(httpClient.fetched()).containsExactly(URI.create(FIRST_CERT_URL));

        // The signature verifies against the cached certificate, so the other URL forms are not fetched.
        for (int i = 0; i < 30; i++) {
            assertThat(manager.parseMessage(signedMessage(signature, urlVariant(FIRST_CERT_PATH, i))))
                .isInstanceOf(SnsNotification.class);
        }
        assertThat(httpClient.fetched()).containsExactly(URI.create(FIRST_CERT_URL));

        assertThat(manager.parseMessage(signedMessage(signature, FIRST_CERT_URL))).isInstanceOf(SnsNotification.class);
        assertThat(httpClient.fetched()).hasSize(1);
    }

    @Test
    void parseMessage_urlVariantsWithBadSignature_certificateCachedOnce() throws Exception {
        String signature = TestSigningCertificates.sign(canonical(MESSAGE), keyA, "SHA256withRSA");
        String firstMessage = signedMessage(signature, FIRST_CERT_URL);

        assertThat(manager.parseMessage(firstMessage)).isInstanceOf(SnsNotification.class);
        assertThat(httpClient.fetched()).hasSize(1);
        assertThat(retriever.cachedCertificateCount()).isEqualTo(1);

        // Nothing cached verifies the signature, so each new URL form is fetched once; all of them resolve to the same
        // certificate, which occupies a single cache slot.
        for (int i = 0; i < 30; i++) {
            String message = unsignedMessage(urlVariant(FIRST_CERT_PATH, i), ZERO_SIGNATURE);
            assertThatThrownBy(() -> manager.parseMessage(message))
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("did not match");
            assertThat(httpClient.fetched()).hasSizeLessThanOrEqualTo(2 + i);
            assertThat(retriever.cachedCertificateCount()).isEqualTo(1);
        }

        // The first certificate is still cached, so the first message validates without a fetch.
        int fetchesBefore = httpClient.fetched().size();
        assertThat(manager.parseMessage(firstMessage)).isInstanceOf(SnsNotification.class);
        assertThat(httpClient.fetched()).hasSize(fetchesBefore);
    }

    @Test
    void parseMessage_newCertificateAtNewUrl_fetchedOnceThenCached() throws Exception {
        httpClient.serve(SECOND_CERT_PATH, TestSigningCertificates.pem(TestSigningCertificates.CERT_B_DER_B64));
        String signatureA = TestSigningCertificates.sign(canonical(MESSAGE), keyA, "SHA256withRSA");
        String signatureB = TestSigningCertificates.sign(canonical(MESSAGE), keyB, "SHA256withRSA");

        assertThat(manager.parseMessage(signedMessage(signatureA, FIRST_CERT_URL))).isInstanceOf(SnsNotification.class);
        assertThat(httpClient.fetched()).containsExactly(URI.create(FIRST_CERT_URL));

        // A second real certificate is fetched from its URL once ...
        assertThat(manager.parseMessage(signedMessage(signatureB, SECOND_CERT_URL))).isInstanceOf(SnsNotification.class);
        assertThat(httpClient.fetched()).containsExactly(URI.create(FIRST_CERT_URL), URI.create(SECOND_CERT_URL));
        assertThat(retriever.cachedCertificateCount()).isEqualTo(2);

        // ... and then served from the cache, even when the message names the other certificate's URL.
        assertThat(manager.parseMessage(signedMessage(signatureB, SECOND_CERT_URL))).isInstanceOf(SnsNotification.class);
        assertThat(manager.parseMessage(signedMessage(signatureB, FIRST_CERT_URL))).isInstanceOf(SnsNotification.class);
        assertThat(httpClient.fetched()).hasSize(2);
        assertThat(retriever.cachedCertificateCount()).isEqualTo(2);
    }

    @Test
    void parseMessage_urlServingNonCertificate_notCached() throws Exception {
        String junkUrl = "https://" + SNS_HOST + "/junk.pem";
        httpClient.serve("/junk.pem", "not a cert".getBytes(StandardCharsets.UTF_8));
        String signature = TestSigningCertificates.sign(canonical(MESSAGE), keyA, "SHA256withRSA");
        String firstMessage = signedMessage(signature, FIRST_CERT_URL);

        assertThat(manager.parseMessage(firstMessage)).isInstanceOf(SnsNotification.class);
        assertThat(httpClient.fetched()).hasSize(1);

        String junkMessage = unsignedMessage(junkUrl, ZERO_SIGNATURE);
        assertThatThrownBy(() -> manager.parseMessage(junkMessage))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Certificate does not match expected X509 PEM format.");
        assertThat(httpClient.fetched()).containsExactly(URI.create(FIRST_CERT_URL), URI.create(junkUrl));
        assertThat(retriever.cachedCertificateCount()).isEqualTo(1);

        assertThat(manager.parseMessage(firstMessage)).isInstanceOf(SnsNotification.class);
        assertThat(httpClient.fetched()).hasSize(2);
    }

    /**
     * A fetch that has not completed for one URL does not delay a fetch for a different URL.
     */
    @Test
    void parseMessage_fetchForOneUrlStalled_doesNotBlockOtherUrl() throws Exception {
        httpClient.serve(SECOND_CERT_PATH, TestSigningCertificates.pem(TestSigningCertificates.CERT_B_DER_B64));
        CountDownLatch firstRequested = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondRequested = new CountDownLatch(1);
        httpClient.countDownWhenRequested(FIRST_CERT_PATH, firstRequested);
        httpClient.holdResponseUntil(FIRST_CERT_PATH, releaseFirst);
        httpClient.countDownWhenRequested(SECOND_CERT_PATH, secondRequested);

        String firstMessage = signedMessage(TestSigningCertificates.sign(canonical(MESSAGE), keyA, "SHA256withRSA"),
                                            FIRST_CERT_URL);
        String secondMessage = signedMessage(TestSigningCertificates.sign(canonical(MESSAGE), keyB, "SHA256withRSA"),
                                             SECOND_CERT_URL);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> manager.parseMessage(firstMessage));
            assertThat(firstRequested.await(2, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> manager.parseMessage(secondMessage));
            assertThat(secondRequested.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(first.isDone()).isFalse();

            releaseFirst.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isInstanceOf(SnsNotification.class);
            assertThat(second.get(5, TimeUnit.SECONDS)).isInstanceOf(SnsNotification.class);
            assertThat(httpClient.fetched()).containsExactly(URI.create(FIRST_CERT_URL), URI.create(SECOND_CERT_URL));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    /**
     * Different spellings of one certificate URL that the URL rules accept.
     */
    private static String urlVariant(String path, int i) {
        switch (i % 5) {
            case 0:
                return "https://" + SNS_HOST + path + "?n=" + i;
            case 1:
                return "https://" + SNS_HOST + "/." + path;
            case 2:
                return "https://" + SNS_HOST + "/" + path;
            case 3:
                return "https://" + SNS_HOST + ":443" + path;
            default:
                return "https://" + SNS_HOST + "/a/.." + path;
        }
    }

    private static String canonical(String message) {
        return TestSigningCertificates.notificationCanonical(message, MESSAGE_ID, null, TIMESTAMP, TOPIC_ARN);
    }

    private static String signedMessage(String signatureB64, String certUrl) {
        return TestSigningCertificates.notificationJson(MESSAGE, MESSAGE_ID, null, TIMESTAMP, TOPIC_ARN, signatureB64,
                                                        certUrl);
    }

    private static String unsignedMessage(String certUrl, String signatureB64) {
        return TestSigningCertificates.notificationJson("x", "00000000-0000-0000-0000-000000000000", null, TIMESTAMP,
                                                        TOPIC_ARN, signatureB64, certUrl);
    }

    /**
     * Records every URI the manager asks to fetch and serves a body per request path, falling back to the default body.
     * A path can signal a latch when it is requested and hold its response until another latch is released.
     */
    private static final class RecordingHttpClient implements SdkHttpClient {
        private final byte[] defaultBody;
        private final Map<String, byte[]> bodiesByPath = new ConcurrentHashMap<>();
        private final Map<String, CountDownLatch> requestedByPath = new ConcurrentHashMap<>();
        private final Map<String, CountDownLatch> holdByPath = new ConcurrentHashMap<>();
        private final List<URI> fetched = Collections.synchronizedList(new ArrayList<>());

        RecordingHttpClient(byte[] defaultBody) {
            this.defaultBody = defaultBody;
        }

        void serve(String path, byte[] body) {
            bodiesByPath.put(path, body);
        }

        void countDownWhenRequested(String path, CountDownLatch latch) {
            requestedByPath.put(path, latch);
        }

        void holdResponseUntil(String path, CountDownLatch latch) {
            holdByPath.put(path, latch);
        }

        List<URI> fetched() {
            synchronized (fetched) {
                return new ArrayList<>(fetched);
            }
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            URI uri = request.httpRequest().getUri();
            fetched.add(uri);
            byte[] body = bodiesByPath.getOrDefault(uri.getRawPath(), defaultBody);
            CountDownLatch requested = requestedByPath.get(uri.getRawPath());
            CountDownLatch hold = holdByPath.get(uri.getRawPath());

            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() throws IOException {
                    if (requested != null) {
                        requested.countDown();
                    }
                    if (hold != null) {
                        awaitRelease(hold);
                    }
                    return HttpExecuteResponse.builder()
                                              .response(SdkHttpFullResponse.builder().statusCode(200).build())
                                              .responseBody(AbortableInputStream.create(new ByteArrayInputStream(body)))
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
            return "RecordingHttpClient";
        }

        private static void awaitRelease(CountDownLatch hold) throws IOException {
            try {
                if (!hold.await(10, TimeUnit.SECONDS)) {
                    throw new IOException("Held response was not released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
        }
    }
}

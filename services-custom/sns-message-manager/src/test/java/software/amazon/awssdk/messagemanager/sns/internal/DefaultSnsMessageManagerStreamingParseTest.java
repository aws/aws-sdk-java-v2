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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.regions.Region;

/**
 * End-to-end checks of how {@link SnsMessageManager#parseMessage(InputStream)} reads its input: a top-level array is
 * rejected after its first token, and a large object-shaped body is streamed to completion with only the known fields
 * retained, in both cases without any certificate fetch.
 */
class DefaultSnsMessageManagerStreamingParseTest {
    private static final String RESOURCE_ROOT = "/software/amazon/awssdk/messagemanager/sns/internal/";
    private static final String SIGNING_CERT_RESOURCE = "SimpleNotificationService-7506a1e35b36ef5a444dd1a8e7cc3ed8.pem";
    private static final long SIXTEEN_MIB = 16L * 1024 * 1024;

    private RecordingHttpClient httpClient;
    private SnsMessageManager manager;

    @BeforeEach
    void setUp() {
        httpClient = new RecordingHttpClient();
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
    void parseMessage_topLevelArray_rejected() {
        LargeGeneratedArrayStream stream = new LargeGeneratedArrayStream();

        assertThatThrownBy(() -> manager.parseMessage(stream))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected a JSON object");

        assertThat(httpClient.invoked).isFalse();
        // Jackson fills one 8000-byte buffer before producing the first token; nothing beyond that is read.
        assertThat(stream.bytesRead()).isLessThanOrEqualTo(16 * 1024);
    }

    @Test
    void parseMessage_largeObjectWithUnknownKeys_parsesWithoutFetch() {
        LargeGeneratedJsonStream stream = new LargeGeneratedJsonStream(SIXTEEN_MIB);

        // The whole body streams through the parser; the known fields are retained and the message is then rejected by the
        // signature length check ("AAAA" decodes to three bytes).
        assertTimeoutPreemptively(Duration.ofSeconds(30), () ->
            assertThatThrownBy(() -> manager.parseMessage(stream))
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("too short"));

        assertThat(httpClient.invoked).isFalse();
        assertThat(stream.bytesRead()).isEqualTo(stream.totalSize());
    }

    @Test
    void parseMessage_signedFixture_succeeds() throws Exception {
        try (SnsMessageManager mockedManager = mockedManager()) {
            SnsMessage msg = mockedManager.parseMessage(resourceAsStream("test-notification-with-subject.json"));

            assertThat(msg).isInstanceOf(SnsNotification.class);
            assertThat(((SnsNotification) msg).subject()).isEqualTo("My message subject");
        }
    }

    @Test
    void parseMessage_signedFixtureAsString_succeeds() throws Exception {
        try (SnsMessageManager mockedManager = mockedManager()) {
            String body = new String(readResource("test-notification-with-subject.json"), StandardCharsets.UTF_8);

            SnsMessage msg = mockedManager.parseMessage(body);

            assertThat(msg).isInstanceOf(SnsNotification.class);
            assertThat(((SnsNotification) msg).subject()).isEqualTo("My message subject");
        }
    }

    @Test
    void parseMessage_emptyBody_throws() {
        assertThatThrownBy(() -> manager.parseMessage(asStream("")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected a JSON object but the message was empty");

        assertThat(httpClient.invoked).isFalse();
    }

    @Test
    void parseMessage_malformedJson_throws() {
        assertThatThrownBy(() -> manager.parseMessage("{\"Type\":"))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to parse SNS message as JSON");

        assertThat(httpClient.invoked).isFalse();
    }

    @Test
    void parseMessage_nonObjectTopLevel_throws() {
        assertThatThrownBy(() -> manager.parseMessage("\"just a string\""))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected a JSON object");

        assertThat(httpClient.invoked).isFalse();
    }

    /**
     * A manager whose certificate retriever is mocked to return the fixture's signing key. The HTTP stub is still supplied
     * so that building the manager never loads (and statically caches) the default HTTP client, which would break the
     * system-property-driven tests in {@link DefaultSnsMessageManagerTest}.
     */
    private SnsMessageManager mockedManager() throws Exception {
        CertificateRetriever mockRetriever = mock(CertificateRetriever.class);
        when(mockRetriever.retrieveCertificate(any())).thenReturn(signingPublicKey());
        return new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(mockRetriever)
            .region(Region.US_WEST_2)
            .httpClient(httpClient)
            .build();
    }

    private static PublicKey signingPublicKey() throws Exception {
        try (InputStream is = resourceAsStream(SIGNING_CERT_RESOURCE)) {
            return CertificateFactory.getInstance("X.509").generateCertificate(is).getPublicKey();
        }
    }

    private static InputStream resourceAsStream(String resourceName) {
        return DefaultSnsMessageManagerStreamingParseTest.class.getResourceAsStream(RESOURCE_ROOT + resourceName);
    }

    private static byte[] readResource(String resourceName) throws IOException {
        try (InputStream in = resourceAsStream(resourceName)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static InputStream asStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * HTTP client stub that never touches the network. It only records whether it was asked to fetch anything.
     */
    private static final class RecordingHttpClient implements SdkHttpClient {
        private volatile boolean invoked;

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            invoked = true;
            throw new IllegalStateException("network disabled in test");
        }

        @Override
        public void close() {
        }

        @Override
        public String clientName() {
            return "RecordingHttpClient";
        }
    }

    /**
     * Lazily emits a sequence of byte chunks, optionally up to a total size after which {@link #closingChunk()} is emitted
     * once and the stream ends. The chunks are produced on demand and never held in memory.
     */
    private abstract static class GeneratedStream extends InputStream {
        private static final long UNBOUNDED = -1;

        private final long totalSize;
        private long bytesRead;
        private byte[] pending;
        private int pendingPos;
        private long emitted;
        private boolean finished;

        GeneratedStream(String first, long totalSize) {
            this.pending = ascii(first);
            this.totalSize = totalSize;
        }

        GeneratedStream(String first) {
            this(first, UNBOUNDED);
        }

        /**
         * The chunk to emit after the {@code emitted}-th one has been consumed. Chunks must be ASCII.
         */
        abstract String nextChunk(long emitted);

        /**
         * The chunk that ends the body once {@code totalSize} is reached; ignored for unbounded streams.
         */
        String closingChunk() {
            return "";
        }

        long bytesRead() {
            return bytesRead;
        }

        long totalSize() {
            return totalSize;
        }

        private static byte[] ascii(String s) {
            return s.getBytes(StandardCharsets.US_ASCII);
        }

        /**
         * Replaces the fully consumed {@code pending} chunk. For a bounded stream, whole filler chunks are emitted while
         * they fit; the body is then padded with JSON whitespace so that the closing chunk ends exactly at
         * {@code totalSize}.
         */
        private boolean refill() {
            if (finished) {
                return false;
            }
            if (totalSize == UNBOUNDED) {
                emitted++;
                pending = ascii(nextChunk(emitted));
            } else {
                byte[] closing = ascii(closingChunk());
                long remaining = totalSize - bytesRead - closing.length;
                byte[] next = ascii(nextChunk(emitted + 1));
                if (next.length <= remaining) {
                    emitted++;
                    pending = next;
                } else {
                    byte[] tail = new byte[(int) remaining + closing.length];
                    Arrays.fill(tail, 0, (int) remaining, (byte) ' ');
                    System.arraycopy(closing, 0, tail, (int) remaining, closing.length);
                    pending = tail;
                    finished = true;
                }
            }
            pendingPos = 0;
            return true;
        }

        @Override
        public int read() {
            if (pendingPos >= pending.length && !refill()) {
                return -1;
            }
            bytesRead++;
            return pending[pendingPos++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (len == 0) {
                return 0;
            }
            int written = 0;
            while (written < len) {
                if (pendingPos >= pending.length && !refill()) {
                    break;
                }
                int n = Math.min(len - written, pending.length - pendingPos);
                System.arraycopy(pending, pendingPos, b, off + written, n);
                pendingPos += n;
                written += n;
                bytesRead += n;
            }
            return written == 0 ? -1 : written;
        }
    }

    /**
     * Top-level array body {@code [{},{},{},...} with no end.
     */
    private static final class LargeGeneratedArrayStream extends GeneratedStream {
        LargeGeneratedArrayStream() {
            super("[{}");
        }

        @Override
        String nextChunk(long emitted) {
            return ",{}";
        }
    }

    /**
     * Object-shaped body that carries the known fields first, then unique unknown keys with nested values until
     * {@code totalSize} bytes have been produced, then {@code }}.
     */
    private static final class LargeGeneratedJsonStream extends GeneratedStream {
        private static final String KNOWN_FIELDS =
            "{\"Type\":\"Notification\","
            + "\"MessageId\":\"22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"hello\","
            + "\"Timestamp\":\"2012-05-02T00:54:06.655Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"AAAA\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/SimpleNotificationService-test.pem\"";

        LargeGeneratedJsonStream(long totalSize) {
            super(KNOWN_FIELDS, totalSize);
        }

        @Override
        String nextChunk(long emitted) {
            return ",\"k" + emitted + "\":{\"a\":[1,2,{}]}";
        }

        @Override
        String closingChunk() {
            return "}";
        }
    }
}

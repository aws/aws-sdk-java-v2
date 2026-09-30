package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Behavioral tests for the streaming path, covering what a wire diff structurally cannot.
 *
 * <p>{@link S3WireDiffTest} compares request text against a golden capture, which leaves three gaps:
 *
 * <ol>
 *   <li>{@code WireFormat} replaces {@code x-amz-content-sha256} with a placeholder (§12.8), so a
 *       regression from {@code UNSIGNED-PAYLOAD} back to hashing the body would compare equal. Since
 *       hashing a streamed body means buffering all of it, that regression is exactly the one worth a
 *       dedicated assertion.</li>
 *   <li>A golden capture only proves the request was right, not that response bytes reached the
 *       caller.</li>
 *   <li>The diffed bodies are 38 bytes, so nothing about them would notice a pipeline that buffers.</li>
 * </ol>
 *
 * <p>The last one is why this class runs with a small heap: the module's surefire configuration sets
 * {@code -Xmx256m}, and {@link #streamsBodiesLargerThanTheHeap()} moves 1 GiB in each direction. A
 * bridge that buffers a body anywhere — to hash it, to measure it, or to hand it to a codec — fails
 * that test with an {@code OutOfMemoryError} instead of passing quietly on a machine with enough RAM.
 */
class S3StreamingTest {

    /** 4x the -Xmx this module's surefire sets, so buffering cannot survive by luck. */
    private static final long OVER_HEAP_BYTES = 1024L * 1024 * 1024;

    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDWIREDIFFEXAMPLE", "wirediff/secret/key"));

    @Test
    void sendsRequestBodyBytesUnchanged() {
        byte[] payload = randomBytes(1024 * 1024);
        CapturingHttpClient transport = CapturingHttpClient.xml("");
        try (S3Client s3 = S3Cases.client(transport)) {
            s3.putObject(r -> r.bucket("b").key("k"), RequestBody.fromBytes(payload));
        }
        assertArrayEquals(payload, transport.only().body(), "PutObject body was altered in transit");
        assertEquals(Optional.of(String.valueOf(payload.length)),
                     transport.only().request().firstMatchingHeader("Content-Length"),
                     "a known-length streaming body must still declare its length");
    }

    /**
     * Locks in {@code UNSIGNED-PAYLOAD} for a streaming request.
     *
     * <p>smithy-java's SigV4 signer will hash the body if this header is absent, which for a streaming
     * body means either {@code DataStream.asByteBuffer()} (the whole object into the heap) or an
     * outright {@code UnsupportedOperationException} when the length is unknown. So this is not a
     * cosmetic assertion about a header value; it is the assertion that the streaming path exists.
     */
    @Test
    void signsStreamingRequestsAsUnsignedPayload() {
        CapturingHttpClient transport = CapturingHttpClient.xml("");
        try (S3Client s3 = S3Cases.client(transport)) {
            s3.putObject(r -> r.bucket("b").key("k"), RequestBody.fromString("x", StandardCharsets.UTF_8));
        }
        SdkHttpFullRequest request = transport.only().request();
        assertEquals(Optional.of("UNSIGNED-PAYLOAD"), request.firstMatchingHeader("x-amz-content-sha256"),
                     "streaming request bodies must not be hashed");
        assertTrue(request.firstMatchingHeader("Authorization")
                          .orElse("")
                          .contains("x-amz-content-sha256"),
                   "x-amz-content-sha256 must be inside SignedHeaders, or the service rejects it");
    }

    @Test
    void deliversResponseBodyBytesUnchanged() {
        byte[] payload = randomBytes(512 * 1024);
        CapturingHttpClient transport = new CapturingHttpClient(200, payload, "application/octet-stream");
        ResponseBytes<GetObjectResponse> result;
        try (S3Client s3 = S3Cases.client(transport)) {
            result = s3.getObject(r -> r.bucket("b").key("k"), ResponseTransformer.toBytes());
        }
        assertArrayEquals(payload, result.asByteArray(), "GetObject body was altered in transit");
        // Taking the body away from the deserializer must not cost the modeled response. Header-bound
        // members are the check: they are deserialized from the same response object the interceptor
        // replaces, so if it replaced too much they would come back null.
        assertEquals((long) payload.length, result.response().contentLength());
        assertEquals("application/octet-stream", result.response().contentType());
        // Not asserted: responseMetadata()/sdkHttpResponse(), which are null on every successful
        // response through this pipeline, streaming or not. That is §1.4, not a streaming finding.
    }

    /**
     * Proves the response body is not drained before the caller sees it.
     *
     * <p>The interceptor hands the deserializer an empty body in place of the real one. If it handed
     * over the real body as well — or if the codec were given the original response — the bytes would be
     * consumed looking for an XML document, and the caller's transformer would see a short or empty
     * stream. A byte count is the cheapest thing that catches that.
     */
    @Test
    void doesNotConsumeTheResponseBodyBeforeTheTransformer() {
        byte[] payload = randomBytes(64 * 1024);
        CapturingHttpClient transport = new CapturingHttpClient(200, payload, "application/octet-stream");
        long counted;
        try (S3Client s3 = S3Cases.client(transport)) {
            counted = s3.getObject(r -> r.bucket("b").key("k"), (response, body) -> drain(body));
        }
        assertEquals(payload.length, counted);
    }

    /**
     * Streams 1 GiB up and 1 GiB down under a 256 MiB heap.
     *
     * <p>Neither body is ever materialized: the request is generated by a {@link ContentStreamProvider}
     * and checksummed as the transport drains it, and the response is generated by the transport and
     * checksummed as the transformer reads it. The CRCs are what make this a correctness test rather
     * than only a memory test — a pipeline that streams the wrong bytes, or drops a chunk, fails here
     * too.
     */
    @Test
    void streamsBodiesLargerThanTheHeap() {
        assertTrue(Runtime.getRuntime().maxMemory() < OVER_HEAP_BYTES,
                   "this test is meaningless above a " + OVER_HEAP_BYTES + " byte heap; surefire should "
                   + "be setting -Xmx256m for this module");

        DrainingHttpClient transport = new DrainingHttpClient(OVER_HEAP_BYTES);
        long received;
        try (S3Client s3 = S3Cases.client(transport)) {
            s3.putObject(r -> r.bucket("b").key("k"),
                         RequestBody.fromContentProvider(() -> new PatternInputStream(OVER_HEAP_BYTES),
                                                         OVER_HEAP_BYTES,
                                                         "application/octet-stream"));
            assertEquals(OVER_HEAP_BYTES, transport.requestBodyBytes, "the whole request body must arrive");
            assertEquals(patternCrc(OVER_HEAP_BYTES), transport.requestBodyCrc, "request body bytes differ");
            assertEquals(Optional.of("UNSIGNED-PAYLOAD"),
                         transport.lastRequest.firstMatchingHeader("x-amz-content-sha256"),
                         "a body this size cannot have been hashed, so the header must say so");

            received = s3.getObject(r -> r.bucket("b").key("k"), (response, body) -> crcOf(body));
        }
        assertEquals(patternCrc(OVER_HEAP_BYTES), received, "response body bytes differ");
    }

    /**
     * A modeled error on a streaming operation still arrives as its v2 exception.
     *
     * <p>The interceptor that moves a response body aside has to leave error responses alone: their body
     * is the XML the deserializer needs to build the exception. Take it away and every failed
     * {@code GetObject} becomes a generic {@code S3Exception} with no error code — a much worse outcome
     * than the one being optimized for, and one no success-path test would notice.
     */
    @Test
    void modeledErrorsSurviveOnStreamingOperations() {
        String error = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                       + "<Error><Code>NoSuchKey</Code><Message>The specified key does not exist.</Message>"
                       + "<Key>k</Key><RequestId>WIREDIFF000000000</RequestId></Error>";
        CapturingHttpClient transport = new CapturingHttpClient(404, error, "application/xml");
        try (S3Client s3 = S3Cases.client(transport)) {
            NoSuchKeyException e = assertThrows(
                NoSuchKeyException.class,
                () -> s3.getObject(r -> r.bucket("b").key("k"), ResponseTransformer.toBytes()));
            assertEquals("NoSuchKey", e.awsErrorDetails().errorCode());
            assertEquals(404, e.statusCode());
        }
    }

    /**
     * Over plain HTTP a streamed body is chunk-signed, as stock v2 does it (§13.2, resolved).
     *
     * <p>This used to assert a refusal: smithy-java 1.6.1 has no chunked signing, so the bridge refused
     * rather than send an unauthenticated body. With v2's own signer bridged ({@code V2SigningAuthScheme})
     * the request goes out as v2 sends it — {@code STREAMING-AWS4-HMAC-SHA256-PAYLOAD}, each chunk
     * carrying its own signature. The byte-level comparison with stock is
     * {@code S3ChecksumWireDiffTest}'s {@code put-object-http}; this checks the property directly, with
     * checksums off so no trailer is involved.
     */
    @Test
    void streamsOverPlainHttpWithChunkedSigning() {
        CapturingHttpClient transport = CapturingHttpClient.xml("");
        try (S3Client s3 = S3Client.builder()
                                   .region(Region.US_EAST_1)
                                   .credentialsProvider(CREDENTIALS)
                                   .endpointOverride(URI.create("http://s3.us-east-1.amazonaws.com"))
                                   .forcePathStyle(true)
                                   .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                                   .httpClient(transport)
                                   .build()) {
            s3.putObject(r -> r.bucket("b").key("k"), RequestBody.fromString("x", StandardCharsets.UTF_8));
        }
        CapturingHttpClient.CapturedRequest sent = transport.only();
        assertEquals("STREAMING-AWS4-HMAC-SHA256-PAYLOAD",
                     sent.request().firstMatchingHeader("x-amz-content-sha256").orElse(null));
        assertTrue(new String(sent.body(), StandardCharsets.UTF_8).contains("chunk-signature="),
                   "the body must be chunk-signed");
    }

    private static long drain(InputStream in) {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        try {
            for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
                total += read;
            }
        } catch (IOException e) {
            throw new AssertionError("failed reading the response body", e);
        }
        return total;
    }

    private static long crcOf(InputStream in) {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[64 * 1024];
        try {
            for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
                crc.update(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new AssertionError("failed reading the response body", e);
        }
        return crc.getValue();
    }

    private static long patternCrc(long length) {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[64 * 1024];
        long produced = 0;
        while (produced < length) {
            int chunk = (int) Math.min(buffer.length, length - produced);
            for (int i = 0; i < chunk; i++) {
                buffer[i] = PatternInputStream.byteAt(produced + i);
            }
            crc.update(buffer, 0, chunk);
            produced += chunk;
        }
        return crc.getValue();
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        // Seeded: a failure has to be reproducible, and these bytes are only here to be unlike
        // anything the pipeline could produce on its own.
        new Random(20260905L).nextBytes(bytes);
        return bytes;
    }

    /**
     * An arbitrarily long stream of a deterministic, non-repeating-per-byte pattern.
     *
     * <p>Position-dependent rather than a repeated block: a pipeline that duplicated or reordered a
     * chunk would produce the same CRC over a repeated block, and this stream exists to catch exactly
     * that class of bug.
     */
    private static final class PatternInputStream extends InputStream {

        private final long length;
        private long position;

        private PatternInputStream(long length) {
            this.length = length;
        }

        static byte byteAt(long position) {
            return (byte) ((position * 31 + (position >> 13)) & 0xFF);
        }

        @Override
        public int read() {
            if (position >= length) {
                return -1;
            }
            return byteAt(position++) & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (position >= length) {
                return -1;
            }
            int count = (int) Math.min(len, length - position);
            for (int i = 0; i < count; i++) {
                b[off + i] = byteAt(position + i);
            }
            position += count;
            return count;
        }
    }

    /**
     * A transport that drains the request body without keeping it, and answers with a generated body of
     * the same size.
     *
     * <p>{@link CapturingHttpClient} cannot be used here: it calls {@code readAllBytes()}, which for a
     * body larger than the heap fails in the harness rather than in the code under test — a failure that
     * looks identical to the bug being hunted.
     */
    private static final class DrainingHttpClient implements SdkHttpClient {

        private final long responseLength;
        private SdkHttpFullRequest lastRequest;
        private long requestBodyBytes;
        private long requestBodyCrc;

        private DrainingHttpClient(long responseLength) {
            this.responseLength = responseLength;
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            lastRequest = (SdkHttpFullRequest) request.httpRequest();
            Optional<ContentStreamProvider> provider = request.contentStreamProvider();
            if (provider.isPresent()) {
                CRC32 crc = new CRC32();
                byte[] buffer = new byte[256 * 1024];
                long total = 0;
                try (InputStream in = provider.get().newStream()) {
                    for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
                        crc.update(buffer, 0, read);
                        total += read;
                    }
                } catch (IOException e) {
                    throw new AssertionError("failed draining the request body", e);
                }
                requestBodyBytes = total;
                requestBodyCrc = crc.getValue();
            }

            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    return HttpExecuteResponse.builder()
                                              .response(SdkHttpResponse.builder()
                                                                       .statusCode(200)
                                                                       .putHeader("Content-Type",
                                                                                  "application/octet-stream")
                                                                       .putHeader("Content-Length",
                                                                                  String.valueOf(responseLength))
                                                                       .putHeader("x-amz-request-id",
                                                                                  "WIREDIFF000000000")
                                                                       .build())
                                              .responseBody(AbortableInputStream.create(
                                                  new PatternInputStream(responseLength)))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public String clientName() {
            return "WireDiffDrain";
        }

        @Override
        public void close() {
        }
    }
}

package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BufferedSplittableAsyncRequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * The generated async clients, bridged onto smithy-java, driving v2's two async HTTP clients over a real
 * socket.
 *
 * <p>Every case runs over Netty and over CRT, and every client is built the way a customer builds one —
 * {@code S3AsyncClient.builder()}, {@code DynamoDbAsyncClient.builder()}, and for multipart
 * {@code multipartEnabled(true)} — so nothing here reaches past the public API. That last point is what
 * retires {@code SyncBackedS3AsyncClient}: the multipart cases in {@link S3MultipartTest} had to reach
 * the internal {@code MultipartS3AsyncClient.create} to point it at the façade, because the public builder
 * produced a stock client. Here the public builder produces a bridged one.
 *
 * <p>S3 runs over HTTPS because the bridge refuses to stream a body over plain HTTP
 * ({@code compatability_issues.md} 13.2); Netty validates the loopback certificate, and CRT, which
 * accepts no custom trust material, is told to skip validation. DynamoDB runs over plain HTTP, like the
 * benchmark harness does.
 */
class BridgedAsyncClientsTest {

    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDWIREDIFFEXAMPLE", "wirediff/secret/key"));
    private static final long PART_SIZE = 5 * 1024 * 1024;
    private static final int OBJECT_BYTES = 20 * 1024 * 1024;
    private static final int EXPECTED_PARTS = 4;

    private LocalHttpServer server;
    private FakeS3 s3;

    @BeforeEach
    void start() {
        server = LocalHttpServer.startTls();
        s3 = new FakeS3();
        server.route(s3::handle);
    }

    @AfterEach
    void stop() {
        server.close();
    }

    static List<Object[]> transports() {
        return List.of(
            new Object[] {"netty", (Supplier<SdkAsyncHttpClient>) () ->
                NettyNioAsyncHttpClient.builder()
                                       .tlsTrustManagersProvider(() -> LocalHttpServer.trustManagers().getTrustManagers())
                                       .build()},
            new Object[] {"crt", (Supplier<SdkAsyncHttpClient>) () ->
                AwsCrtAsyncHttpClient.builder()
                                     .buildWithDefaults(AttributeMap.builder()
                                                                    .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES,
                                                                         true)
                                                                    .build())});
    }

    /** The premise of every other case: these really are the generated clients, and they are bridged. */
    @Test
    void theGeneratedAsyncClientsAreBridged() throws Exception {
        for (String name : List.of("software.amazon.awssdk.services.s3.DefaultS3AsyncClient",
                                   "software.amazon.awssdk.services.dynamodb.DefaultDynamoDbAsyncClient")) {
            boolean bridged = java.util.Arrays.stream(Class.forName(name).getDeclaredFields())
                                              .anyMatch(f -> f.getType().getName().endsWith(".SmithyBridgeClient"));
            assertTrue(bridged, name + " has no SmithyBridgeClient field; these tests would be measuring stock v2");
        }
    }

    // ---- S3: streaming both ways ---------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void putThenGetRoundTripsTheBytes(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        byte[] payload = pattern(4 * 1024 * 1024);

        try (S3AsyncClient client = s3Client(transport.get(), false)) {
            PutObjectResponse put = client.putObject(r -> r.bucket("bucket").key("obj"), AsyncRequestBody.fromBytes(payload))
                                          .get(30, TimeUnit.SECONDS);
            byte[] got = client.getObject(r -> r.bucket("bucket").key("obj"), AsyncResponseTransformer.toBytes())
                               .get(30, TimeUnit.SECONDS)
                               .asByteArray();

            assertEquals("\"single\"", put.eTag());
            assertArrayEquals(payload, s3.objects.get("/bucket/obj"), "the server must receive every byte");
            assertArrayEquals(payload, got, "the caller must receive every byte");
        }
    }

    /**
     * {@code toBlockingInputStream} completes on headers and is read afterwards, from the caller's thread.
     *
     * <p>Under the façade this held a pool thread until the caller closed the stream (14.4). Here the future
     * completes from {@code onStream}, the envelope's virtual thread exits, and the reads drain the
     * transport directly — so reading the body after the future has completed is itself the assertion.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void blockingInputStreamIsReadAfterTheFutureCompletes(String name, Supplier<SdkAsyncHttpClient> transport)
            throws Exception {
        byte[] payload = pattern(2 * 1024 * 1024);
        s3.objects.put("/bucket/obj", payload);

        try (S3AsyncClient client = s3Client(transport.get(), false)) {
            byte[] got;
            try (ResponseInputStream<GetObjectResponse> stream =
                     client.getObject(r -> r.bucket("bucket").key("obj"), AsyncResponseTransformer.toBlockingInputStream())
                           .get(30, TimeUnit.SECONDS)) {
                assertEquals((long) payload.length, stream.response().contentLength());
                got = stream.readAllBytes();
            }
            assertArrayEquals(payload, got);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void aModeledErrorFailsTheFutureTheWayV2Does(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        try (S3AsyncClient client = s3Client(transport.get(), false)) {
            Throwable failure = failureOf(() -> client.getObject(r -> r.bucket("bucket").key("missing"),
                                                                 AsyncResponseTransformer.toBytes()).join());

            // v2 fails its futures with a CompletionException around the SdkException
            // (AsyncExecutionFailureExceptionReportingStage); join() rethrows it as is.
            assertInstanceOf(CompletionException.class, failure);
            NoSuchKeyException cause = assertInstanceOf(NoSuchKeyException.class, failure.getCause());
            assertEquals(404, cause.statusCode());
            assertEquals("NoSuchKey", cause.awsErrorDetails().errorCode());
        }
    }

    /**
     * The caller's callbacks run on v2's completion executor, not on the envelope thread or an event loop.
     *
     * <p>This is the configuration a customer can set with {@code FUTURE_COMPLETION_EXECUTOR}; the default
     * pool's threads are named {@code sdk-async-response-*}. Ignoring the option would silently drop any
     * context propagation a customer's executor does.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void callbacksRunOnTheCompletionExecutor(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        s3.objects.put("/bucket/obj", pattern(1024));
        try (S3AsyncClient client = s3Client(transport.get(), false)) {
            AtomicReference<String> thread = new AtomicReference<>();
            client.headObject(r -> r.bucket("bucket").key("obj"))
                  .whenComplete((r, e) -> thread.set(Thread.currentThread().getName()))
                  .get(30, TimeUnit.SECONDS);
            assertTrue(thread.get().startsWith("sdk-async-response"),
                       "callback ran on " + thread.get() + ", not the completion executor");
        }
    }

    // ---- S3: multipart through the public builder -----------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void multipartUploadSendsEveryByteExactlyOnce(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        byte[] payload = pattern(OBJECT_BYTES);

        try (S3AsyncClient client = s3Client(transport.get(), true)) {
            PutObjectResponse response = client.putObject(r -> r.bucket("bucket").key("big.bin"),
                                                          AsyncRequestBody.fromBytes(payload))
                                               .get(60, TimeUnit.SECONDS);
            assertEquals("\"final\"", response.eTag());
        }

        assertEquals(1, s3.creates.get(), "exactly one CreateMultipartUpload");
        assertEquals(EXPECTED_PARTS, s3.parts.size(), "parts uploaded: " + s3.parts.keySet());
        assertEquals(1, s3.completes.get(), "exactly one CompleteMultipartUpload");
        assertEquals(0, s3.aborts.get(), "a successful upload must not abort");
        assertArrayEquals(payload, s3.reassemble(), "the parts do not reassemble into the object that was sent");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void multipartDownloadReassemblesEveryPart(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        byte[] payload = pattern(OBJECT_BYTES);
        s3.objects.put("/bucket/big.bin", payload);

        byte[] got;
        try (S3AsyncClient client = s3Client(transport.get(), true)) {
            got = client.getObject(r -> r.bucket("bucket").key("big.bin"), AsyncResponseTransformer.toBytes())
                        .get(60, TimeUnit.SECONDS)
                        .asByteArray();
        }
        assertEquals(EXPECTED_PARTS, s3.partGets.get(), "every part must be fetched");
        assertArrayEquals(payload, got);
    }

    /**
     * A retryable failure on a one-shot part is not retried, and the caller sees the real failure.
     *
     * <p>Stock v2 does not retry it either: {@code AsyncRequestBody.split} produces
     * {@code NonRetryableSubAsyncRequestBody} parts, and a second subscription is refused with "Multiple
     * subscribers detected". The difference is what the caller is told. Stock v2 attempts the retry and
     * reports the refused subscription; the bridge declines the retry up front
     * ({@code V2NonReplayableError}) and reports the 500 that actually happened. Either way the upload is
     * aborted, which is the part that matters for not leaking storage.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void aOneShotPartIsNotRetriedAndTheRealErrorSurfaces(String name, Supplier<SdkAsyncHttpClient> transport)
            throws Exception {
        byte[] payload = pattern(OBJECT_BYTES);
        s3.failPart(2, 1);

        S3AsyncClient client = s3Client(transport.get(), true);
        try {
            Throwable failure = failureOf(() -> client.putObject(r -> r.bucket("bucket").key("big.bin"),
                                                                 AsyncRequestBody.fromBytes(payload)).join());
            Throwable cause = failure instanceof CompletionException ? failure.getCause() : failure;
            S3Exception s3Exception = assertInstanceOf(S3Exception.class, cause, "got " + failure);
            assertEquals(500, s3Exception.statusCode());
            assertEquals(1, s3.partAttempts.get(2).get(), "the failed part must not be re-sent");
            assertEquals(0, s3.completes.get());
            assertTrue(waitFor(() -> s3.aborts.get() == 1), "the upload must be aborted");
        } finally {
            client.close();
        }
    }

    /**
     * The same failure on a <em>replayable</em> part is retried, and the upload succeeds.
     *
     * <p>{@code BufferedSplittableAsyncRequestBody} is v2's documented way to make split parts retryable —
     * the fix {@code NonRetryableSubAsyncRequestBody}'s own error message recommends. It is the other half
     * of the principled rule in {@code V2DataStreams.toDataStream(AsyncRequestBody)}: the bridge declines
     * only the retries that cannot work.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void aReplayablePartIsRetriedAndTheUploadSucceeds(String name, Supplier<SdkAsyncHttpClient> transport)
            throws Exception {
        byte[] payload = pattern(OBJECT_BYTES);
        s3.failPart(2, 1);

        try (S3AsyncClient client = s3Client(transport.get(), true)) {
            client.putObject(r -> r.bucket("bucket").key("big.bin"),
                             BufferedSplittableAsyncRequestBody.create(AsyncRequestBody.fromBytes(payload)))
                  .get(60, TimeUnit.SECONDS);
        }
        assertEquals(2, s3.partAttempts.get(2).get(), "part 2 must have been sent twice");
        assertEquals(1, s3.completes.get());
        assertArrayEquals(payload, s3.reassemble());
    }

    // ---- DynamoDB: the envelope alone ------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void dynamoDbGetItemAndAModeledError(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        try (LocalHttpServer ddb = LocalHttpServer.start()) {
            ddb.route(request -> {
                String target = request.header("X-amz-target");
                if ("DynamoDB_20120810.GetItem".equals(target)) {
                    return LocalHttpServer.Response.ok(
                        "{\"Item\":{\"id\":{\"S\":\"a\"},\"n\":{\"N\":\"42\"}}}".getBytes(StandardCharsets.UTF_8),
                        "application/x-amz-json-1.0");
                }
                return LocalHttpServer.Response.of(
                    400,
                    ("{\"__type\":\"com.amazonaws.dynamodb.v20120810#ConditionalCheckFailedException\","
                     + "\"message\":\"The conditional request failed\"}").getBytes(StandardCharsets.UTF_8),
                    "application/x-amz-json-1.0");
            });

            try (DynamoDbAsyncClient client = DynamoDbAsyncClient.builder()
                                                                 .region(Region.US_EAST_1)
                                                                 .credentialsProvider(CREDENTIALS)
                                                                 .endpointOverride(ddb.uri(""))
                                                                 .httpClient(transport.get())
                                                                 .build()) {
                GetItemResponse item = client.getItem(r -> r.tableName("t").key(Map.of("id", AttributeValue.fromS("a"))))
                                             .get(30, TimeUnit.SECONDS);
                assertEquals("42", item.item().get("n").n());

                Throwable failure = failureOf(() -> client.putItem(r -> r.tableName("t")
                                                                         .item(Map.of("id", AttributeValue.fromS("a")))
                                                                         .conditionExpression("attribute_not_exists(id)"))
                                                          .join());
                assertInstanceOf(CompletionException.class, failure);
                ConditionalCheckFailedException cause =
                    assertInstanceOf(ConditionalCheckFailedException.class, failure.getCause());
                assertEquals(400, cause.statusCode());
            }
        }
    }

    // ---- helpers ---------------------------------------------------------------

    private S3AsyncClient s3Client(SdkAsyncHttpClient transport, boolean multipart) {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .credentialsProvider(CREDENTIALS)
                            .endpointOverride(server.uri(""))
                            .forcePathStyle(true)
                            // As in S3Cases: the bridge computes no checksums (12.9), so neither side should.
                            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                            .multipartEnabled(multipart)
                            .multipartConfiguration(c -> c.minimumPartSizeInBytes(PART_SIZE)
                                                          .thresholdInBytes(PART_SIZE)
                                                          .apiCallBufferSizeInBytes(PART_SIZE * 4))
                            .httpClient(transport)
                            .build();
    }

    private static Throwable failureOf(Runnable call) {
        try {
            call.run();
        } catch (Throwable t) {
            return t;
        }
        throw new AssertionError("expected the call to fail");
    }

    private static boolean waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(20);
        }
        return condition.getAsBoolean();
    }

    /** Position-dependent, so a reordered or duplicated part cannot compare equal. */
    private static byte[] pattern(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ((i * 31 + (i >> 13)) & 0xFF);
        }
        return bytes;
    }

    /**
     * Enough of S3 to drive v2's single-object and multipart code over a socket.
     *
     * <p>Routed the way S3 routes: {@code POST ?uploads} creates, {@code PUT ?partNumber&uploadId} uploads a
     * part, {@code POST ?uploadId} completes, {@code DELETE ?uploadId} aborts, {@code GET ?partNumber}
     * returns a part with {@code x-amz-mp-parts-count} so v2's downloader knows there are more.
     */
    private static final class FakeS3 {
        private static final String XML = "application/xml";

        final Map<String, byte[]> objects = new ConcurrentHashMap<>();
        final Map<Integer, byte[]> parts = new ConcurrentSkipListMap<>();
        final Map<Integer, AtomicInteger> partAttempts = new ConcurrentHashMap<>();
        final Map<Integer, AtomicInteger> partFailures = new ConcurrentHashMap<>();
        final AtomicInteger creates = new AtomicInteger();
        final AtomicInteger completes = new AtomicInteger();
        final AtomicInteger aborts = new AtomicInteger();
        final AtomicInteger partGets = new AtomicInteger();

        void failPart(int part, int times) {
            partFailures.put(part, new AtomicInteger(times));
        }

        LocalHttpServer.Response handle(LocalHttpServer.ReceivedRequest request) {
            String path = request.uri().contains("?") ? request.uri().substring(0, request.uri().indexOf('?'))
                                                       : request.uri();
            String partNumber = request.query("partNumber");
            boolean uploadId = request.query("uploadId") != null;

            switch (request.method()) {
                case "POST":
                    if (request.query("uploads") != null) {
                        creates.incrementAndGet();
                        return xml("<InitiateMultipartUploadResult><Bucket>bucket</Bucket><Key>big.bin</Key>"
                                   + "<UploadId>upload-1</UploadId></InitiateMultipartUploadResult>");
                    }
                    completes.incrementAndGet();
                    return xml("<CompleteMultipartUploadResult><Bucket>bucket</Bucket><Key>big.bin</Key>"
                               + "<ETag>&quot;final&quot;</ETag></CompleteMultipartUploadResult>");
                case "DELETE":
                    if (uploadId) {
                        aborts.incrementAndGet();
                    }
                    return LocalHttpServer.Response.of(204, new byte[0], null);
                case "PUT":
                    if (partNumber != null) {
                        int part = Integer.parseInt(partNumber);
                        partAttempts.computeIfAbsent(part, p -> new AtomicInteger()).incrementAndGet();
                        AtomicInteger remaining = partFailures.get(part);
                        if (remaining != null && remaining.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                            return LocalHttpServer.Response.of(
                                500, ("<Error><Code>InternalError</Code><Message>injected</Message></Error>")
                                    .getBytes(StandardCharsets.UTF_8), XML);
                        }
                        parts.put(part, request.body());
                        return LocalHttpServer.Response.ok(new byte[0], XML).header("ETag", "\"part-" + part + "\"");
                    }
                    objects.put(path, request.body());
                    return LocalHttpServer.Response.ok(new byte[0], XML).header("ETag", "\"single\"");
                case "HEAD":
                case "GET":
                    byte[] object = objects.get(path);
                    if (object == null) {
                        return LocalHttpServer.Response.of(
                            404, "<Error><Code>NoSuchKey</Code><Message>missing</Message></Error>"
                                .getBytes(StandardCharsets.UTF_8), XML);
                    }
                    if (partNumber == null) {
                        return LocalHttpServer.Response.ok(request.method().equals("HEAD") ? new byte[0] : object,
                                                           "application/octet-stream")
                                                       .header("ETag", "\"single\"");
                    }
                    partGets.incrementAndGet();
                    int part = Integer.parseInt(partNumber);
                    int offset = (int) ((part - 1) * PART_SIZE);
                    int length = (int) Math.min(PART_SIZE, object.length - offset);
                    byte[] slice = new byte[length];
                    System.arraycopy(object, offset, slice, 0, length);
                    int total = (int) Math.ceil(object.length / (double) PART_SIZE);
                    return LocalHttpServer.Response.ok(slice, "application/octet-stream")
                                                   .header("x-amz-mp-parts-count", String.valueOf(total))
                                                   .header("ETag", "\"multi\"");
                default:
                    throw new IllegalStateException("unexpected " + request.method() + " " + request.uri());
            }
        }

        byte[] reassemble() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] part : parts.values()) {
                out.write(part);
            }
            return out.toByteArray();
        }

        private static LocalHttpServer.Response xml(String body) {
            return LocalHttpServer.Response.ok(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + body)
                                                   .getBytes(StandardCharsets.UTF_8), XML);
        }
    }
}

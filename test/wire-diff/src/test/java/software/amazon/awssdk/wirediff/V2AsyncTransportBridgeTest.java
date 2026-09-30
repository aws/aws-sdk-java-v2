package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.bridge.smithyjava.transport.V2AsyncTransportBridge;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Proves {@link V2AsyncTransportBridge} drives both of v2's async HTTP clients, against a real socket.
 *
 * <p>Every test runs twice, once over Netty and once over CRT, because "the bridge works" is a claim
 * about both: they are independent wire implementations (a Java event loop and a native one), and the
 * bridge depends on a callback ordering and an error-delivery path that each of them could get to
 * differently. A single-transport pass would not be evidence for the other.
 *
 * <p>The tests also pin down the two properties that are the reason for this bridge existing rather
 * than {@code SyncBackedS3AsyncClient}: {@code send} returns on <em>headers</em>, not on a complete
 * body ({@link #returnsBeforeTheBodyIsComplete}), and nothing in the path needs a thread per request
 * ({@link #manyConcurrentCallsOnVirtualThreads} runs more concurrent calls than any pool here has
 * threads). The rest are the ordinary transport obligations: bytes out intact, bytes back intact,
 * status preserved, failures remapped to smithy's exception contract.
 */
class V2AsyncTransportBridgeTest {

    private LocalHttpServer server;

    @BeforeEach
    void startServer() {
        server = LocalHttpServer.start();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    /** The two transports under test, as suppliers so each test gets a fresh client it can close. */
    static List<Object[]> transports() {
        return List.of(
            new Object[] {"netty", (Supplier<SdkAsyncHttpClient>) () -> NettyNioAsyncHttpClient.builder().build()},
            new Object[] {"crt", (Supplier<SdkAsyncHttpClient>) () -> AwsCrtAsyncHttpClient.builder().build()});
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void getReturnsStatusHeadersAndBody(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        byte[] body = "<Result><Ok>yes</Ok></Result>".getBytes(StandardCharsets.UTF_8);
        server.respondWith(LocalHttpServer.Response.ok(body, "application/xml"));

        try (ClientTransport<HttpRequest, HttpResponse> bridge = new V2AsyncTransportBridge(transport.get())) {
            HttpResponse response = bridge.send(Context.create(), get(server.uri("/bucket/key")));

            assertEquals(200, response.statusCode());
            assertEquals("application/xml", response.headers().firstValue("content-type"));
            assertArrayEquals(body, readFully(response.body()));
            assertEquals("GET", server.only().method());
            assertEquals("/bucket/key", server.only().uri());
        }
    }

    /**
     * The response body can be read the way smithy-java's codecs read it, with {@code asByteBuffer()}.
     *
     * <p>Not a formality: {@code DataStream.ofPublisher(..., false)} fails this in smithy-java 1.6.1 — its
     * {@code asByteBuffer()} marks the stream consumed and then trips over its own flag — which is why the
     * bridge returns its own {@code ResponseBodyDataStream}. Every XML and JSON response is read this way,
     * so a regression here fails every non-streaming call. The second read must still fail: the body is
     * one-shot, and pretending otherwise would hand a retry an empty document.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void responseBodyReadsAsAByteBufferExactlyOnce(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        byte[] body = "<Result><Ok>yes</Ok></Result>".getBytes(StandardCharsets.UTF_8);
        server.respondWith(LocalHttpServer.Response.ok(body, "application/xml"));

        try (ClientTransport<HttpRequest, HttpResponse> bridge = new V2AsyncTransportBridge(transport.get())) {
            DataStream received = bridge.send(Context.create(), get(server.uri("/doc"))).body();
            java.nio.ByteBuffer buffer = received.asByteBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            assertArrayEquals(body, bytes);
            assertThrows(IllegalStateException.class, received::asByteBuffer);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void requestHeadersAndBodyReachTheServerIntact(String name, Supplier<SdkAsyncHttpClient> transport)
            throws Exception {
        server.respondWith(LocalHttpServer.Response.ok(new byte[0], null));
        byte[] payload = randomBytes(256 * 1024);

        try (ClientTransport<HttpRequest, HttpResponse> bridge = new V2AsyncTransportBridge(transport.get())) {
            HttpRequest request = HttpRequest.create()
                                             .setMethod("PUT")
                                             .setUri(server.uri("/bucket/object"))
                                             .setHeader("x-amz-meta-test", "bridged")
                                             .setBody(DataStream.ofBytes(payload, "application/octet-stream"));

            HttpResponse response = bridge.send(Context.create(), request);
            readFully(response.body());

            LocalHttpServer.ReceivedRequest seen = server.only();
            assertEquals("PUT", seen.method());
            assertEquals("bridged", seen.header("X-amz-meta-test"));
            // The point of the FlowAdapters path: the body arrives byte for byte, with no copy through a
            // blocking stream and no buffering step in between.
            assertArrayEquals(payload, seen.body());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void unknownLengthRequestBodyIsChunked(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        server.respondWith(LocalHttpServer.Response.ok(new byte[0], null));
        byte[] payload = randomBytes(64 * 1024);

        try (ClientTransport<HttpRequest, HttpResponse> bridge = new V2AsyncTransportBridge(transport.get())) {
            // -1 content length, i.e. hasKnownLength() == false, which is what the bridge reports to v2 as
            // an empty Optional. v2 then has to pick chunked encoding rather than send a Content-Length.
            DataStream unknownLength =
                DataStream.ofInputStream(new java.io.ByteArrayInputStream(payload), "application/octet-stream", -1);

            HttpRequest request = HttpRequest.create()
                                             .setMethod("POST")
                                             .setUri(server.uri("/stream"))
                                             .setBody(unknownLength);

            readFully(bridge.send(Context.create(), request).body());

            LocalHttpServer.ReceivedRequest seen = server.only();
            assertArrayEquals(payload, seen.body());
            // The framing the bridge had to add itself: v2's async clients send no body at all when
            // neither Content-Length nor Transfer-Encoding is settled, and smithy-java's serializer only
            // supplies the first. See V2AsyncTransportBridge#addFraming.
            assertEquals("chunked", seen.header("Transfer-encoding"));
            assertNull(seen.header("Content-length"));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void bodylessRequestGetsNoFramingHeaders(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        server.respondWith(LocalHttpServer.Response.ok(new byte[0], null));

        try (ClientTransport<HttpRequest, HttpResponse> bridge = new V2AsyncTransportBridge(transport.get())) {
            readFully(bridge.send(Context.create(), get(server.uri("/nobody"))).body());

            // The other side of addFraming: an empty body has a known length, so it must not pick up a
            // chunked header. Content-Length on a GET would be a wire difference against stock v2.
            LocalHttpServer.ReceivedRequest seen = server.only();
            assertNull(seen.header("Transfer-encoding"));
            assertEquals(0, seen.body().length);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void errorStatusIsReturnedRatherThanThrown(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        byte[] body = "<Error><Code>NoSuchKey</Code></Error>".getBytes(StandardCharsets.UTF_8);
        server.respondWith(LocalHttpServer.Response.of(404, body, "application/xml"));

        try (ClientTransport<HttpRequest, HttpResponse> bridge = new V2AsyncTransportBridge(transport.get())) {
            // A 404 is a successful exchange as far as a transport is concerned; turning it into an
            // exception is the protocol's job, further up the pipeline.
            HttpResponse response = bridge.send(Context.create(), get(server.uri("/missing")));

            assertEquals(404, response.statusCode());
            assertArrayEquals(body, readFully(response.body()));
        }
    }

    /**
     * The design claim, asserted: the call returns while the body is still arriving.
     *
     * <p>The server sends headers and 1 KiB, stalls for 800 ms, then sends the remaining 63 KiB. If
     * {@code send} returned only on a complete body, it could not come back in materially less than the
     * stall. Measured against half the stall, so the assertion has room for a slow machine without
     * becoming true for the wrong reason.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void returnsBeforeTheBodyIsComplete(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        byte[] body = randomBytes(64 * 1024);
        long stallMillis = 800;
        server.respondWith(LocalHttpServer.Response.ok(body, "application/octet-stream")
                                                   .stallAfter(1024, stallMillis));

        try (ClientTransport<HttpRequest, HttpResponse> bridge = new V2AsyncTransportBridge(transport.get())) {
            long start = System.nanoTime();
            HttpResponse response = bridge.send(Context.create(), get(server.uri("/slow")));
            Duration toHeaders = Duration.ofNanos(System.nanoTime() - start);

            byte[] received = readFully(response.body());
            Duration toBody = Duration.ofNanos(System.nanoTime() - start);

            assertArrayEquals(body, received);
            assertTrue(toHeaders.toMillis() < stallMillis / 2,
                       "send should return on headers, but took " + toHeaders.toMillis() + "ms of a "
                       + stallMillis + "ms stall");
            assertTrue(toBody.toMillis() >= stallMillis / 2,
                       "reading the body should have spanned the stall, but took only " + toBody.toMillis() + "ms");
        }
    }

    /**
     * More in-flight calls than there are platform threads to hold them.
     *
     * <p>This is the property that makes the blocking-envelope design acceptable. Each call parks a
     * virtual thread waiting on headers; the I/O is on the transport's own event loops. 200 concurrent
     * calls against a server that stalls each response for 300 ms would need 200 platform threads under
     * {@code SyncBackedS3AsyncClient}, whose ceiling was its pool size.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void manyConcurrentCallsOnVirtualThreads(String name, Supplier<SdkAsyncHttpClient> transport) throws Exception {
        int calls = 200;
        byte[] body = randomBytes(8 * 1024);
        server.respondWith(LocalHttpServer.Response.ok(body, "application/octet-stream").stallAfter(512, 300));

        // Netty's default maxConcurrency is 50, so this is also a check that exceeding it queues rather
        // than fails -- the natural bound virtual threads remove is the transport's, not the bridge's.
        try (ClientTransport<HttpRequest, HttpResponse> bridge = new V2AsyncTransportBridge(transport.get());
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Callable<byte[]>> tasks = IntStream.range(0, calls)
                    .<Callable<byte[]>>mapToObj(i -> () -> readFully(
                        bridge.send(Context.create(), get(server.uri("/concurrent/" + i))).body()))
                    .toList();

            for (Future<byte[]> result : executor.invokeAll(tasks)) {
                assertArrayEquals(body, result.get());
            }
            assertEquals(calls, server.received().size());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transports")
    void connectFailureIsRemappedToSmithysExceptionContract(String name, Supplier<SdkAsyncHttpClient> transport)
            throws Exception {
        int deadPort = server.port();
        server.close(); // nothing is listening now, so the connect attempt is refused

        try (ClientTransport<HttpRequest, HttpResponse> bridge = new V2AsyncTransportBridge(transport.get())) {
            // The contract a transport owes the pipeline: never leak a raw IOException or a CRT-specific
            // type. What arrives here has been through ClientTransport.remapExceptions.
            RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> bridge.send(Context.create(), get(URI.create("http://localhost:" + deadPort + "/gone"))));

            assertNotNull(thrown);
            assertTrue(thrown.getClass().getName().startsWith("software.amazon.smithy.java"),
                       "expected a smithy-java exception type, got " + thrown.getClass().getName());
            assertTrue(!(thrown instanceof CrtRuntimeException), "CRT's own exception type leaked through");
        }
    }

    // ---- helpers -------------------------------------------------------------

    private static HttpRequest get(URI uri) {
        return HttpRequest.create().setMethod("GET").setUri(uri);
    }

    private static byte[] readFully(DataStream body) throws IOException {
        try (InputStream in = body.asInputStream()) {
            return in.readAllBytes();
        }
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random(length).nextBytes(bytes); // seeded: a failure is reproducible
        return bytes;
    }
}

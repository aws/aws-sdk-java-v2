package software.amazon.awssdk.wirediff;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * A real loopback HTTP server, for the tests that need a socket rather than a stub.
 *
 * <p>{@link CapturingHttpClient} is the right tool for byte diffing, and this is not trying to replace
 * it: a stubbed transport cannot exercise a transport bridge, because the thing under test <em>is</em>
 * the transport. Netty and CRT need a port to connect to, headers that arrive before a body, and a
 * connection that can be refused.
 *
 * <p>Built on the JDK's own {@code com.sun.net.httpserver}, so no dependency and no lifecycle beyond
 * {@link #close()}. Bound to loopback on an ephemeral port, which keeps it usable in parallel runs.
 */
public final class LocalHttpServer implements AutoCloseable {

    /**
     * The benchmark harness's throwaway loopback key pair, reused rather than copied so the repository holds
     * one committed private key and not two. See {@code BenchmarkTls} in {@code standalone-e2e-benchmarks}
     * for its provenance; it authenticates nothing but a loopback test server.
     */
    private static final Path TLS_KEYSTORE =
        Path.of("../standalone-e2e-benchmarks/src/main/resources/benchmark-tls/benchmark.p12");
    private static final char[] TLS_PASSWORD = "benchmark".toCharArray();

    private final HttpServer server;
    private final String scheme;
    private java.util.concurrent.ExecutorService executor;
    private final List<ReceivedRequest> received = new CopyOnWriteArrayList<>();
    private volatile Function<ReceivedRequest, Response> router =
        r -> Response.ok("ok".getBytes(StandardCharsets.UTF_8), "text/plain");

    private LocalHttpServer(HttpServer server, String scheme) {
        this.server = server;
        this.scheme = scheme;
    }

    public static LocalHttpServer start() {
        try {
            return listen(HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0), "http");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * An HTTPS server, for the tests that need one: the bridge refuses to stream a request body over plain
     * HTTP ({@code compatability_issues.md} 13.2), so an S3 upload over a real socket needs TLS.
     */
    public static LocalHttpServer startTls() {
        try {
            HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.setHttpsConfigurator(new HttpsConfigurator(serverTlsContext()));
            return listen(server, "https");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Trust material for a client that should validate this server's certificate rather than skip it. */
    public static TrustManagerFactory trustManagers() {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(keyStore());
            return factory;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SSLContext serverTlsContext() {
        try {
            KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(keyStore(), TLS_PASSWORD);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keys.getKeyManagers(), null, null);
            return context;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyStore keyStore() {
        try (InputStream in = Files.newInputStream(TLS_KEYSTORE)) {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(in, TLS_PASSWORD);
            return store;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("could not load " + TLS_KEYSTORE.toAbsolutePath(), e);
        }
    }

    private static LocalHttpServer listen(HttpServer server, String scheme) {
        LocalHttpServer local = new LocalHttpServer(server, scheme);
        server.createContext("/", local::handle);
        // A small pool rather than the default (null) executor, which serves on the accept thread and
        // would serialize a concurrency test.
        java.util.concurrent.ExecutorService executor = Executors.newFixedThreadPool(16);
        server.setExecutor(executor);
        local.executor = executor;
        server.start();
        return local;
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public URI uri(String path) {
        return URI.create(scheme + "://localhost:" + port() + path);
    }

    /** Sets the reply for every subsequent request. */
    public LocalHttpServer respondWith(Response response) {
        this.router = r -> response;
        return this;
    }

    /** Routes each request to a reply, for a server that has to behave like an API rather than a stub. */
    public LocalHttpServer route(Function<ReceivedRequest, Response> router) {
        this.router = router;
        return this;
    }

    public List<ReceivedRequest> received() {
        return Collections.unmodifiableList(new ArrayList<>(received));
    }

    public ReceivedRequest only() {
        List<ReceivedRequest> snapshot = received();
        if (snapshot.size() != 1) {
            throw new IllegalStateException("expected exactly one request, got " + snapshot.size());
        }
        return snapshot.get(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            ReceivedRequest request = new ReceivedRequest(exchange.getRequestMethod(),
                                                          exchange.getRequestURI().toString(),
                                                          exchange.getRequestHeaders(),
                                                          body);
            received.add(request);
            Response reply;
            try {
                reply = router.apply(request);
            } catch (RuntimeException e) {
                reply = Response.of(500, String.valueOf(e).getBytes(StandardCharsets.UTF_8), "text/plain");
            }

            if (reply.contentType != null) {
                exchange.getResponseHeaders().add("Content-Type", reply.contentType);
            }
            reply.headers.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
            // -1 is the JDK server's spelling of "no body"; 0 would mean chunked.
            exchange.sendResponseHeaders(reply.status, reply.body.length == 0 ? -1 : reply.body.length);

            try (OutputStream out = exchange.getResponseBody()) {
                if (reply.pauseAfterBytes <= 0 || reply.pauseAfterBytes >= reply.body.length) {
                    out.write(reply.body);
                    return;
                }
                // Headers and a first slice, then a stall. This is what lets a test tell the difference
                // between "the transport bridge returns on headers" and "it waits for the whole body".
                out.write(reply.body, 0, reply.pauseAfterBytes);
                out.flush();
                sleep(reply.pauseMillis);
                out.write(reply.body, reply.pauseAfterBytes, reply.body.length - reply.pauseAfterBytes);
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        server.stop(0);
        // HttpServer.stop leaves a caller-supplied executor running, and its threads are not daemons,
        // so a main() that used this server would otherwise never exit.
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /** One request as the server saw it, body fully read. */
    public record ReceivedRequest(String method, String uri, Map<String, List<String>> headers, byte[] body) {
        public String bodyAsString() {
            return new String(body, StandardCharsets.UTF_8);
        }

        /** A query parameter's first value, "" for a valueless one, or null if absent. */
        public String query(String name) {
            String raw = URI.create(uri).getRawQuery();
            if (raw == null) {
                return null;
            }
            for (String pair : raw.split("&")) {
                int eq = pair.indexOf('=');
                String key = eq < 0 ? pair : pair.substring(0, eq);
                if (key.equals(name)) {
                    return eq < 0 ? "" : java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                }
            }
            return null;
        }

        public String header(String name) {
            List<String> values = headers.get(name);
            return values == null || values.isEmpty() ? null : values.get(0);
        }
    }

    /** What the server replies with, including an optional stall part way through the body. */
    public static final class Response {
        private final int status;
        private final byte[] body;
        private final String contentType;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private int pauseAfterBytes;
        private long pauseMillis;

        private Response(int status, byte[] body, String contentType) {
            this.status = status;
            this.body = body;
            this.contentType = contentType;
        }

        public static Response ok(byte[] body, String contentType) {
            return new Response(200, body, contentType);
        }

        public static Response of(int status, byte[] body, String contentType) {
            return new Response(status, body, contentType);
        }

        public Response header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        /** Write {@code bytes} of the body, then stall for {@code millis} before writing the rest. */
        public Response stallAfter(int bytes, long millis) {
            this.pauseAfterBytes = bytes;
            this.pauseMillis = millis;
            return this;
        }
    }
}

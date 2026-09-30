package software.amazon.awssdk.wirediff;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * An {@link SdkHttpClient} that records the request it was handed and replies with a canned response.
 *
 * <p>Capturing here rather than at a mock server is deliberate: this is the last point the SDK
 * controls, so what lands in {@link #captured()} is byte-for-byte what would have gone out — after
 * marshalling, after endpoint resolution, after signing. A mock server would see the same bytes but
 * would also add a socket, a port, and a clock to something that needs to be deterministic enough to
 * diff against a file in git.
 *
 * <p>Not thread-safe and not meant to be: one instance per captured call.
 */
public final class CapturingHttpClient implements SdkHttpClient {

    private final List<CapturedRequest> captured = new ArrayList<>();
    private final int responseStatus;
    private final byte[] responseBody;
    private final String responseContentType;
    private final java.util.Map<String, String> extraHeaders = new java.util.LinkedHashMap<>();

    public CapturingHttpClient(int responseStatus, String responseBody, String responseContentType) {
        this(responseStatus, responseBody.getBytes(StandardCharsets.UTF_8), responseContentType);
    }

    /** For a response body that is not text: an object's bytes, rather than an XML document. */
    private volatile long delayMillis;

    /** Delays every response, for the tests that need a call to be slow (timeouts). */
    public CapturingHttpClient withDelay(long millis) {
        this.delayMillis = millis;
        return this;
    }

    /** Adds a response header, for the tests that need one the canned response does not carry. */
    public CapturingHttpClient withResponseHeader(String name, String value) {
        extraHeaders.put(name, value);
        return this;
    }

    public CapturingHttpClient(int responseStatus, byte[] responseBody, String responseContentType) {
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.responseContentType = responseContentType;
    }

    /** A 200 with an XML body, the shape of nearly every S3 non-streaming response. */
    public static CapturingHttpClient xml(String body) {
        return new CapturingHttpClient(200, body, "application/xml");
    }

    public List<CapturedRequest> captured() {
        return captured;
    }

    public CapturedRequest only() {
        if (captured.size() != 1) {
            throw new IllegalStateException("expected exactly one captured request, got " + captured.size());
        }
        return captured.get(0);
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        SdkHttpFullRequest httpRequest = (SdkHttpFullRequest) request.httpRequest();
        // The body is read here, at capture time, rather than lazily: request.contentStreamProvider()
        // hands out a fresh stream per call, and reading it later would race with the SDK's own
        // (already finished) use of it.
        byte[] body = readBody(request);
        captured.add(new CapturedRequest(httpRequest, body));

        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() {
                if (delayMillis > 0) {
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("interrupted", e);
                    }
                }
                return HttpExecuteResponse.builder()
                                          .response(SdkHttpResponse.builder()
                                                                   .statusCode(responseStatus)
                                                                   .putHeader("Content-Type", responseContentType)
                                                                   .putHeader("Content-Length",
                                                                              String.valueOf(responseBody.length))
                                                                   .putHeader("x-amz-request-id", "WIREDIFF000000000")
                                                                   .putHeader("x-amz-id-2", "wirediff")
                                                                   .applyMutation(b -> extraHeaders.forEach(b::putHeader))
                                                                   .build())
                                          .responseBody(AbortableInputStream.create(
                                              new ByteArrayInputStream(responseBody)))
                                          .build();
            }

            @Override
            public void abort() {
            }
        };
    }

    private static byte[] readBody(HttpExecuteRequest request) {
        Optional<software.amazon.awssdk.http.ContentStreamProvider> provider = request.contentStreamProvider();
        if (provider.isEmpty()) {
            return new byte[0];
        }
        try (InputStream in = provider.get().newStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOExceptionWrapper(e);
        }
    }

    @Override
    public String clientName() {
        return "WireDiffCapture";
    }

    @Override
    public void close() {
    }

    /** Unchecked wrapper; the harness has nothing useful to do with an IO failure on a byte array. */
    static final class UncheckedIOExceptionWrapper extends RuntimeException {
        UncheckedIOExceptionWrapper(IOException cause) {
            super(cause);
        }
    }

    /** One captured request: the SDK's own request object plus the fully-read body. */
    public record CapturedRequest(SdkHttpFullRequest request, byte[] body) {
    }
}

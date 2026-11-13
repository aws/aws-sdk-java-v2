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

package software.amazon.awssdk.s3.mockserver.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.s3.mockserver.helpers.ByteArrays;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.async.SimplePublisher;

public final class InMemoryAsyncHttpClient implements SdkAsyncHttpClient {

    public static final int EVENT_LOOP_THREADS = 2;

    private static final int RESPONSE_CHUNK_BYTES = 64 * 1024;

    private static final byte[] HEAD_END = {'\r', '\n', '\r', '\n'};

    private final S3Wiretap wiretap;
    private final ExecutorService eventLoop;

    private InMemoryAsyncHttpClient(S3Wiretap wiretap) {
        this.wiretap = wiretap;
        AtomicInteger threads = new AtomicInteger();
        this.eventLoop = Executors.newFixedThreadPool(EVENT_LOOP_THREADS, r -> {
            Thread t = new Thread(r, "in-memory-http-" + threads.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    public static Builder builder(S3Wiretap wiretap) {
        return new Builder(wiretap);
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        SdkAsyncHttpResponseHandler handler = request.responseHandler();
        request.requestContentPublisher().subscribe(new Subscriber<ByteBuffer>() {
            private final ByteArrayOutputStream body = new ByteArrayOutputStream();

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                body.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(Throwable t) {
                handler.onError(t);
                done.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                byte[] sent = body.toByteArray();
                try {
                    eventLoop.execute(() -> exchange(request.request(), sent, handler, done));
                } catch (RejectedExecutionException e) {
                    handler.onError(e);
                    done.completeExceptionally(e);
                }
            }
        });
        return done;
    }

    @Override
    public String clientName() {
        return "InMemory";
    }

    @Override
    public void close() {
        eventLoop.shutdownNow();
    }

    private void exchange(SdkHttpRequest head, byte[] body, SdkAsyncHttpResponseHandler handler,
                          CompletableFuture<Void> done) {
        ParsedResponse response;
        try {
            response = parse(wiretap.serveInMemory(head.host(), serialize(head, body)));
        } catch (IOException | RuntimeException e) {
            handler.onError(e);
            done.completeExceptionally(e);
            return;
        }
        handler.onHeaders(SdkHttpResponse.builder()
                .statusCode(response.statusCode())
                .statusText(response.statusText())
                .headers(response.headers())
                .build());
        SimplePublisher<ByteBuffer> stream = new SimplePublisher<>();
        handler.onStream(stream);
        for (int off = 0; off < response.body().length; off += RESPONSE_CHUNK_BYTES) {
            int end = Math.min(response.body().length, off + RESPONSE_CHUNK_BYTES);
            stream.send(ByteBuffer.wrap(Arrays.copyOfRange(response.body(), off, end)));
        }
        stream.complete().whenComplete((ignored, t) -> {
            if (t == null) {
                done.complete(null);
            } else {
                done.completeExceptionally(t);
            }
        });
    }

    private static byte[] serialize(SdkHttpRequest head, byte[] body) {
        StringBuilder text = new StringBuilder();
        String path = head.encodedPath().isEmpty() ? "/" : head.encodedPath();
        text.append(head.method().name()).append(' ').append(path);
        head.encodedQueryParameters().ifPresent(q -> text.append('?').append(q));
        text.append(" HTTP/1.1\r\n");
        if (!head.firstMatchingHeader("Host").isPresent()) {
            boolean defaultPort = head.port() == ("https".equals(head.protocol()) ? 443 : 80);
            text.append("Host: ").append(head.host()).append(defaultPort ? "" : ":" + head.port()).append("\r\n");
        }
        head.forEachHeader((name, values) -> values.forEach(v -> text.append(name).append(": ").append(v).append("\r\n")));
        boolean chunked = head.firstMatchingHeader("Transfer-Encoding")
                .map(v -> v.toLowerCase(java.util.Locale.ROOT).contains("chunked")).orElse(false);
        if (!chunked && !head.firstMatchingHeader("Content-Length").isPresent() && body.length > 0) {
            text.append("Content-Length: ").append(body.length).append("\r\n");
        }
        text.append("\r\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length() + body.length + 32);
        byte[] headBytes = text.toString().getBytes(StandardCharsets.ISO_8859_1);
        out.write(headBytes, 0, headBytes.length);
        if (chunked) {
            byte[] size = (Integer.toHexString(body.length) + "\r\n").getBytes(StandardCharsets.US_ASCII);
            if (body.length > 0) {
                out.write(size, 0, size.length);
                out.write(body, 0, body.length);
                out.write('\r');
                out.write('\n');
            }
            byte[] last = "0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
            out.write(last, 0, last.length);
        } else {
            out.write(body, 0, body.length);
        }
        return out.toByteArray();
    }

    private static ParsedResponse parse(byte[] raw) {
        int start = 0;
        while (true) {
            int end = indexOf(raw, HEAD_END, start);
            if (end < 0) {
                throw new IllegalStateException("the wiretap's in-memory response has no complete head");
            }
            String[] lines = new String(raw, start, end - start, StandardCharsets.ISO_8859_1).split("\r\n");
            String[] status = lines[0].split(" ", 3);
            int code = Integer.parseInt(status[1]);
            start = end + HEAD_END.length;
            if (code >= 100 && code < 200) {
                continue;
            }
            Map<String, List<String>> headers = new LinkedHashMap<>();
            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                headers.computeIfAbsent(lines[i].substring(0, colon).trim(), k -> new ArrayList<>())
                        .add(lines[i].substring(colon + 1).trim());
            }
            if (headers.keySet().stream().anyMatch("Transfer-Encoding"::equalsIgnoreCase)) {
                throw new IllegalStateException("the in-memory transport reads Content-Length framing only;"
                        + " the response carries Transfer-Encoding");
            }
            return new ParsedResponse(code, status.length > 2 ? status[2] : "", headers,
                    Arrays.copyOfRange(raw, start, raw.length));
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        for (int i = from; i <= haystack.length - needle.length; i++) {
            if (regionMatches(haystack, i, needle)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean regionMatches(byte[] haystack, int at, byte[] needle) {
        for (int j = 0; j < needle.length; j++) {
            if (haystack[at + j] != needle[j]) {
                return false;
            }
        }
        return true;
    }

    private static final class ParsedResponse {
        private final int statusCode;
        private final String statusText;
        private final Map<String, List<String>> headers;
        private final byte[] body;

        private ParsedResponse(int statusCode, String statusText, Map<String, List<String>> headers, byte[] body) {
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.headers = headers;
            this.body = body;
        }

        public int statusCode() {
            return statusCode;
        }

        public String statusText() {
            return statusText;
        }

        public Map<String, List<String>> headers() {
            return headers;
        }

        public byte[] body() {
            return body;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ParsedResponse that = (ParsedResponse) o;
            return statusCode == that.statusCode
                && Objects.equals(statusText, that.statusText)
                && Objects.equals(headers, that.headers)
                && body == that.body;
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Integer.hashCode(statusCode);
            result = 31 * result + Objects.hashCode(statusText);
            result = 31 * result + Objects.hashCode(headers);
            result = 31 * result + Objects.hashCode(body);
            return result;
        }

        @Override
        public String toString() {
            return "ParsedResponse[statusCode=" + statusCode
                + ", statusText=" + statusText
                + ", headers=" + headers
                + ", body=" + ByteArrays.describe(body)
                + "]";
        }
    }

    public static final class Builder implements SdkAsyncHttpClient.Builder<Builder> {
        private final S3Wiretap wiretap;

        private Builder(S3Wiretap wiretap) {
            this.wiretap = wiretap;
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return new InMemoryAsyncHttpClient(wiretap);
        }
    }
}

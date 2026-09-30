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

package software.amazon.awssdk.bridge.smithyjava.transport;

import static software.amazon.awssdk.http.Header.CHUNKED;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.TRANSFER_ENCODING;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.bridge.smithyjava.client.V2Timeout;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.client.core.MessageExchange;
import software.amazon.smithy.java.client.http.HttpMessageExchange;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.HttpVersion;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Wraps an AWS SDK for Java <b>v2</b> {@link SdkAsyncHttpClient} as a smithy-java
 * {@link ClientTransport}, so the smithy-java runtime can put bytes on the wire using v2's
 * asynchronous HTTP clients — Netty and CRT — unchanged.
 *
 * <p>This is the async sibling of {@link V2TransportBridge}, and the two differ in one way that
 * matters more than the type signatures suggest: this one does <b>no</b> blocking I/O. It blocks a
 * thread, but only to wait, and the thread it blocks is meant to be a virtual thread.
 *
 * <h2>Why a blocking method over an async client is the right shape</h2>
 *
 * <p>smithy-java's transport contract is synchronous: {@code send} returns a response. v2's async
 * clients are callback-driven. Bridging them means parking somewhere, and the only question is what
 * is parked and for how long.
 *
 * <p>The answer here is: park until the response <b>headers</b> arrive, not until the body is
 * complete. {@code onHeaders} gives the status and headers, {@code onStream} gives a publisher for
 * the body, and this class turns the pair into a smithy {@link HttpResponse} whose body is a
 * {@link DataStream} wrapping that publisher. The body then streams from the event loop to whoever
 * reads the {@code DataStream}, with backpressure intact and nothing buffered. So the parked thread
 * waits for one round trip's first byte, not for the payload — and if that thread is virtual, the
 * wait costs a few hundred bytes of heap instead of a platform thread.
 *
 * <p>The net effect is that a bridged async client has no thread per in-flight request anywhere: the
 * I/O is on Netty's or CRT's event loops, and the call envelope is a parked virtual thread. That is
 * the property {@code SyncBackedS3AsyncClient} could not have, and the reason its caveats 1 and 2
 * (pool size as the real concurrency ceiling; deadlock when a body publisher shares the pool) do not
 * apply here. See {@code compatability_issues.md} sections 14.1 and 14.2.
 *
 * <h2>Both transports satisfy the ordering this relies on</h2>
 *
 * <p>This class completes its wait from {@code onStream}, having stashed the headers from
 * {@code onHeaders}, which assumes {@code onStream} always follows. Both v2 async clients call them
 * back to back and unconditionally, including for a response with no body: Netty in
 * {@code ResponseHandler} (one branch for a full response, one for a streaming one) and CRT in
 * {@code CrtResponseAdapter}. A transport that broke that assumption would hang rather than fail, so
 * the {@code execute} future is also wired to the wait: if the exchange finishes without ever
 * producing a response, the wait fails instead of hanging forever.
 *
 * <h2>Which failures get retried</h2>
 *
 * <p>Same split as the sync bridge, for the same reason. Because {@code send} returns on headers, a
 * failure while reading the <em>body</em> happens later, inside the pipeline's {@code deserialize},
 * and is retried like any other deserialization failure. What this class sees is the earlier set —
 * connect, TLS handshake, request send, response-header read — and smithy-java retries none of it.
 * See {@code compatability_issues.md} section 3.6; it is not fixable from here.
 */
@SdkPublicApi
public final class V2AsyncTransportBridge implements ClientTransport<HttpRequest, HttpResponse> {

    private final SdkAsyncHttpClient v2HttpClient;

    public V2AsyncTransportBridge(SdkAsyncHttpClient v2HttpClient) {
        this.v2HttpClient = v2HttpClient;
    }

    @Override
    public HttpResponse send(Context context, HttpRequest request) {
        try {
            return V2Timeout.attempt(V2Timeout.attemptTimeout(context), () -> sendAttempt(request));
        } catch (RuntimeException e) {
            // Into the retry loop rather than past it; see V2DeferredTransportFailure (ledger 3.6).
            return V2DeferredTransportFailure.defer(context, e);
        }
    }

    private HttpResponse sendAttempt(HttpRequest request) {
        ResponseSignal signal = new ResponseSignal();
        SdkHttpContentPublisher contentPublisher = toContentPublisher(request.body());

        AsyncExecuteRequest executeRequest = AsyncExecuteRequest.builder()
                .request(toV2Request(request, contentPublisher))
                .requestContentPublisher(contentPublisher)
                .responseHandler(signal)
                .build();

        CompletableFuture<Void> exchange;
        try {
            exchange = v2HttpClient.execute(executeRequest);
        } catch (RuntimeException e) {
            throw ClientTransport.remapExceptions(e);
        }

        // Two reasons to wire the exchange future into the wait. A failure before onHeaders (connect,
        // TLS, a rejected request) may arrive here rather than through onError; and an exchange that
        // somehow completes without ever producing a response has to fail the wait rather than leave
        // it parked forever.
        exchange.whenComplete((ignored, error) -> signal.exchangeCompleted(error));
        // A timeout cancels the exchange, which is how v2's async clients abort a request in flight.
        CompletableFuture<Void> inFlight = exchange;
        V2Timeout.registerAbort(() -> inFlight.cancel(true));

        return signal.awaitResponse();
    }

    @Override
    public MessageExchange<HttpRequest, HttpResponse> messageExchange() {
        // Reuse smithy-java's HTTP message exchange -- this bridge is just a different wire impl.
        return HttpMessageExchange.INSTANCE;
    }

    @Override
    public void close() {
        v2HttpClient.close();
    }

    // ---- smithy-java HttpRequest -> v2 ---------------------------------------

    private static SdkHttpFullRequest toV2Request(HttpRequest request, SdkHttpContentPublisher body) {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                .uri(request.uri().toURI())
                .method(SdkHttpMethod.fromValue(request.method()));

        for (Map.Entry<String, List<String>> e : request.headers().map().entrySet()) {
            builder.putHeader(e.getKey(), e.getValue());
        }

        addFraming(builder, body);
        return builder.build();
    }

    /**
     * Supplies the chunked-framing header that smithy-java does not.
     *
     * <p>An HTTP request body has to be framed, by a {@code Content-Length} or by
     * {@code Transfer-Encoding: chunked}, and <b>neither v2 async client synthesizes either one</b>.
     * Netty copies the request's headers verbatim ({@code RequestAdapter#addHeadersToRequest}) and never
     * reads {@code SdkHttpContentPublisher.contentLength()} at all; CRT derives a {@code Content-Length}
     * from it ({@code CrtRequestAdapter:111-115}) but has nothing to fall back on when it is empty. So an
     * unframed body fails silently and differently on each: CRT reports the length as 0
     * ({@code CrtRequestBodyAdapter#getLength}) and never subscribes, while Netty subscribes and writes
     * the bytes with no framing at all, which the peer reads as no body — a protocol violation rather
     * than an empty request.
     *
     * <p>In stock v2 the choice is made much earlier, by the marshaller:
     * {@code AbstractStreamingRequestMarshaller#addHeaders} writes {@code Content-Length} from the body's
     * length and falls back to {@code Transfer-Encoding: chunked} otherwise. smithy-java's serializer
     * does the first half and not the second, so a body of unknown length (an {@code InputStream} with no
     * length, or an {@code AsyncRequestBody.fromPublisher} with no content length) would be lost. This
     * restores the fallback branch.
     *
     * <p>Two parts of v2's version are model-driven and therefore cannot be reproduced here: the
     * {@code requiresLength} trait, which makes v2 fail fast with a clear message rather than send
     * chunked, and the HTTP/2 case, where v2 omits the header. See {@code compatability_issues.md}
     * section 14.6.
     */
    private static void addFraming(SdkHttpFullRequest.Builder builder, SdkHttpContentPublisher body) {
        if (body.contentLength().isPresent() || builder.firstMatchingHeader(CONTENT_LENGTH).isPresent()) {
            return;
        }
        if (builder.firstMatchingHeader(TRANSFER_ENCODING).isEmpty()) {
            builder.putHeader(TRANSFER_ENCODING, CHUNKED);
        }
    }

    /**
     * Presents the smithy request body to v2 as an {@link SdkHttpContentPublisher}.
     *
     * <p>No adaptation of the bytes is needed, only of the publisher interface: a {@link DataStream}
     * <em>is</em> a {@link Flow.Publisher} and v2 wants an {@code org.reactivestreams.Publisher}, which
     * is what {@link FlowAdapters} exists for. Nothing is copied and nothing is buffered.
     *
     * <p>An unknown content length is reported as empty rather than as -1, because v2 reads the
     * {@code Optional} to decide between a {@code Content-Length} header and chunked encoding.
     */
    private static SdkHttpContentPublisher toContentPublisher(DataStream body) {
        DataStream stream = body == null ? DataStream.ofEmpty() : body;
        Publisher<ByteBuffer> adapted = FlowAdapters.toPublisher(stream);
        Optional<Long> length = stream.hasKnownLength()
                                ? Optional.of(stream.contentLength())
                                : Optional.empty();

        return new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return length;
            }

            @Override
            public void subscribe(org.reactivestreams.Subscriber<? super ByteBuffer> subscriber) {
                adapted.subscribe(subscriber);
            }
        };
    }

    // ---- v2 response callbacks -> smithy-java HttpResponse -------------------

    /**
     * Collects v2's response callbacks into the single {@link HttpResponse} that {@code send} returns.
     *
     * <p>Completed from {@code onStream}, because that is the first point at which both halves of a
     * smithy {@code HttpResponse} exist: the status and headers from {@code onHeaders}, and the body
     * publisher from {@code onStream}.
     */
    private static final class ResponseSignal implements SdkAsyncHttpResponseHandler {

        private final CompletableFuture<HttpResponse> response = new CompletableFuture<>();
        private volatile SdkHttpResponse headers;

        @Override
        public void onHeaders(SdkHttpResponse headers) {
            this.headers = headers;
        }

        @Override
        public void onStream(Publisher<ByteBuffer> body) {
            SdkHttpResponse received = headers;
            if (received == null) {
                // Defensive: onStream without onHeaders is a transport contract violation, and
                // guessing a status code here would turn it into a confusing deserialization error.
                response.completeExceptionally(
                    new IllegalStateException("the v2 async HTTP client called onStream without onHeaders"));
                return;
            }
            response.complete(toSmithyResponse(received, body));
        }

        @Override
        public void onError(Throwable error) {
            response.completeExceptionally(error);
        }

        void exchangeCompleted(Throwable error) {
            if (error != null) {
                response.completeExceptionally(error);
            } else {
                // A normal completion is the common case and almost always arrives after the response
                // was handed over, where completing again is a no-op. It only matters when the exchange
                // reported success without ever calling back, which would otherwise park forever.
                response.completeExceptionally(
                    new IllegalStateException("the v2 async HTTP client completed the exchange without "
                                              + "delivering a response"));
            }
        }

        /** Parks until headers arrive. Intended to park a virtual thread; see the class javadoc. */
        HttpResponse awaitResponse() {
            // get(), not join(): a timeout interrupts this wait (V2Timeout), and join() ignores interrupts.
            try {
                return response.get();
            } catch (java.util.concurrent.ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                throw ClientTransport.remapExceptions(cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw ClientTransport.remapExceptions(e);
            } catch (RuntimeException e) {
                throw ClientTransport.remapExceptions(e);
            }
        }

        private static HttpResponse toSmithyResponse(SdkHttpResponse headers, Publisher<ByteBuffer> body) {
            long contentLength = headers.firstMatchingHeader("Content-Length")
                                        .map(Long::parseLong)
                                        .orElse(-1L);
            String contentType = headers.firstMatchingHeader("Content-Type").orElse(null);

            // A response body is never replayable: these bytes arrive once. Not DataStream.ofPublisher,
            // whose asByteBuffer() cannot be called at all in smithy-java 1.6.1; see ResponseBodyDataStream.
            DataStream stream = new ResponseBodyDataStream(body, contentType, contentLength);

            return HttpResponse.of(HttpVersion.HTTP_1_1,
                                   headers.statusCode(),
                                   HttpHeaders.of(headers.headers()),
                                   stream);
        }
    }
}

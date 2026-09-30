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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.bridge.smithyjava.client.V2Timeouts;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
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
 * Wraps an AWS SDK for Java <b>v2</b> {@link SdkHttpClient} as a smithy-java
 * {@link ClientTransport}, so the smithy-java runtime can send requests over any of v2's
 * HTTP clients (URLConnection, Apache, Apache5, Netty, CRT) unchanged.
 *
 * <p>In the "v2 SDK becomes a shell over smithy-java" architecture, this is how a customer's
 * configured {@code httpClient(...)} keeps working: smithy-java drives the call; the v2
 * transport puts bytes on the wire.
 *
 * <p>Scope: synchronous {@link SdkHttpClient} only. An async bridge over
 * {@code SdkAsyncHttpClient} is the analogous follow-up. Bodies are <b>streamed</b>, not
 * buffered: the request body is exposed to v2 as a {@link ContentStreamProvider} that
 * re-opens the smithy {@link DataStream} on demand (so v2 retries get a fresh stream), and
 * the response body is the v2 stream handed straight to {@link DataStream#ofInputStream}.
 *
 * <p>Streaming has a consequence worth stating, because it decides which failures are retried:
 * {@link #send} returns as soon as the response <em>headers</em> arrive, so a failure while reading the
 * body happens outside this class, inside the pipeline's {@code deserialize}, and is retried like any
 * other deserialization failure. What this class sees is only the earlier failures — connect, TLS
 * handshake, request send, response-header read — and smithy-java retries none of them. That is not
 * something this class can change from here; see the comment on its catch clause, and
 * {@code compatability_issues.md} section 3.6.
 */
@SdkPublicApi
public final class V2TransportBridge implements ClientTransport<HttpRequest, HttpResponse> {

    private final SdkHttpClient v2HttpClient;

    public V2TransportBridge(SdkHttpClient v2HttpClient) {
        this.v2HttpClient = v2HttpClient;
    }

    @Override
    public HttpResponse send(Context context, HttpRequest request) {
        try {
            return V2Timeouts.attempt(V2Timeouts.attemptTimeout(context), () -> sendAttempt(request));
        } catch (RuntimeException e) {
            // Into the retry loop rather than past it; see V2TransportFailures (ledger 3.6).
            return V2TransportFailures.defer(context, e);
        }
    }

    private HttpResponse sendAttempt(HttpRequest request) {
        try {
            ExecutableHttpRequest executable = v2HttpClient.prepareRequest(toV2Request(request));
            // What a timeout calls to release the connection, as v2's timeout stages abort the request.
            V2Timeouts.registerAbort(executable::abort);
            HttpExecuteResponse v2Response = executable.call();
            return toSmithyResponse(v2Response);
        } catch (IOException e) {
            // Contract: transports must only throw TransportException/CallException subtypes.
            //
            // Every failure caught here is one v2 would retry -- its retryable set contains IOException,
            // matched against the whole cause chain -- and none of them are retried, no matter what is
            // thrown from here. ClientPipeline drives retries from inside `deserialize`, and
            // `afterIdentity` wraps the send and the deserialize in one try, so a failure in the send is
            // remapped and rethrown having never reached the code that would retry it. Marking the error
            // retry-safe, or substituting a V2RetryableError for it, was tried and measured: inert, for
            // that reason. See compatability_issues.md section 3.6.
            throw ClientTransport.remapExceptions(e);
        }
    }

    @Override
    public MessageExchange<HttpRequest, HttpResponse> messageExchange() {
        // Reuse smithy-java's HTTP message exchange — this bridge is just a different wire impl.
        return HttpMessageExchange.INSTANCE;
    }

    @Override
    public void close() {
        v2HttpClient.close();
    }

    // ---- smithy-java HttpRequest -> v2 SdkHttpFullRequest --------------------

    private static HttpExecuteRequest toV2Request(HttpRequest request) {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                .uri(request.uri().toURI())
                .method(SdkHttpMethod.fromValue(request.method()));

        // Copy every header across. v2 keys headers by name -> list of values.
        for (Map.Entry<String, List<String>> e : request.headers().map().entrySet()) {
            builder.putHeader(e.getKey(), e.getValue());
        }

        ContentStreamProvider bodyProvider = toContentStreamProvider(request.body());
        if (bodyProvider != null) {
            builder.contentStreamProvider(bodyProvider);
        }

        return HttpExecuteRequest.builder()
                .request(builder.build())
                .contentStreamProvider(bodyProvider)
                .build();
    }

    /**
     * Stream the request body to v2 without buffering. A replayable {@link DataStream} is
     * re-opened on each {@code newStream()} (correct for v2 retries); a one-shot stream is
     * handed over once via {@code fromInputStream} (v2 marks/resets within its read limit).
     */
    private static ContentStreamProvider toContentStreamProvider(DataStream body) {
        if (body == null || body.contentLength() == 0) {
            return null;
        }
        if (body.isReplayable()) {
            return ContentStreamProvider.fromInputStreamSupplier(body::asInputStream);
        }
        return ContentStreamProvider.fromInputStream(body.asInputStream());
    }

    // ---- v2 HttpExecuteResponse -> smithy-java HttpResponse ------------------

    private static HttpResponse toSmithyResponse(HttpExecuteResponse v2Response) {
        int status = v2Response.httpResponse().statusCode();
        HttpHeaders headers = HttpHeaders.of(v2Response.httpResponse().headers());

        // Hand v2's response stream straight to smithy-java — no readAllBytes on the hot path.
        DataStream body = v2Response.responseBody()
                .map(V2TransportBridge::toDataStream)
                .orElse(DataStream.ofBytes(new byte[0]));

        return HttpResponse.of(HttpVersion.HTTP_1_1, status, headers, body);
    }

    private static DataStream toDataStream(AbortableInputStream in) {
        long contentLength = -1; // unknown; smithy-java reads until EOF
        return DataStream.ofInputStream(in, null, contentLength);
    }
}

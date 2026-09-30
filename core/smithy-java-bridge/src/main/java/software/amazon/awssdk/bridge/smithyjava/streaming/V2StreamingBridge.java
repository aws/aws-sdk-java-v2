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

package software.amazon.awssdk.bridge.smithyjava.streaming;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.client.core.interceptors.ResponseHook;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Carries streaming request and response bodies around the schema layer.
 *
 * <p>v2's code generator removes a streaming member from the shape entirely: {@code PutObjectRequest} has
 * no {@code Body} field and {@code GetObjectResponse} has no {@code Body} field, because v2 passes the
 * body beside the request ({@code putObject(request, RequestBody)}) and the response body to a
 * {@code ResponseTransformer}. So there is no {@code SdkField} for {@code SdkSchemaFactory} to translate
 * into an {@code @httpPayload @streaming} member, and inventing one would not help: the value it would
 * serialize does not exist on the POJO either.
 *
 * <p>This interceptor therefore does what v2 itself does — moves the body out of band:
 *
 * <ul>
 *   <li><b>Request.</b> {@link #modifyBeforeRetryLoop} replaces the serialized (empty) body with the
 *       caller's {@link DataStream} and sets the payload-hash header, both of which have to happen
 *       before signing; see the payload-hash note below. {@link #readAfterSigning} then rejects the
 *       plain-HTTP case, which cannot be checked any earlier because the endpoint is not on the request
 *       until after {@code modifyBeforeSigning} has run.</li>
 *   <li><b>Response.</b> {@link #modifyBeforeDeserialization} moves the response body into a holder the
 *       caller supplied, and hands the deserializer an empty body in its place, so the XML codec cannot
 *       consume the object's bytes off the socket while looking for a document that is not there.</li>
 * </ul>
 *
 * <h2>Why signing is the hard part</h2>
 *
 * <p>smithy-java's {@code SigV4Signer} resolves the payload hash in three steps: a
 * {@code PAYLOAD_HASH_OVERRIDE} in the context, then an {@code x-amz-content-sha256} header that is
 * already present, then hashing the body. That last step cannot be used for streaming at any size — for an
 * unknown-length body it throws outright, and for a known-length body it calls
 * {@code DataStream.asByteBuffer()}, pulling the entire object into the heap to hash it. A 5 GiB
 * {@code PutObject} would fail on a 2 GiB heap and would double the wall time on a larger one.
 *
 * <p>So this class sets the header itself, to {@code UNSIGNED-PAYLOAD}, which is what v2's S3 signer sends
 * over HTTPS by default. Over plain HTTP v2 does not do that — it switches to chunked (aws-chunked)
 * signing so the body is still authenticated on an unencrypted connection. smithy-java 1.6.1 does not
 * implement chunked signing, so this class refuses rather than silently downgrading the request's
 * integrity protection; {@code compatability_issues.md} §13.2 records that gap.
 */
@SdkProtectedApi
public final class V2StreamingBridge implements ClientInterceptor {

    /**
     * The streaming request body, put in the per-call context by generated code.
     *
     * <p>Absent for every non-streaming operation, which is what makes this interceptor free to install
     * unconditionally: the two hooks below both start with a null check on a context lookup.
     */
    public static final Context.Key<DataStream> REQUEST_BODY =
        Context.key("v2 streaming request body");

    /**
     * Where to leave the streaming response body for the caller to pick up.
     *
     * <p>A holder rather than a return value because {@code Context} is immutable, and the interceptor and
     * the generated client method are on opposite sides of {@code Client#call}. Generated code creates the
     * holder, passes it in, and reads it back after the call returns.
     */
    public static final Context.Key<AtomicReference<DataStream>> RESPONSE_BODY_SINK =
        Context.key("v2 streaming response body sink");

    /**
     * Set in the client config when the client signs with v2's own signer ({@code V2SigningAuthScheme}).
     *
     * <p>Then v2's signer decides the payload hash — {@code UNSIGNED-PAYLOAD} over HTTPS, chunked signing
     * over HTTP, a trailer when a checksum is wanted — and this class must not pre-empt it with a header,
     * nor refuse plain HTTP: both workarounds exist only because smithy-java's signer implements none of
     * that.
     */
    public static final Context.Key<Boolean> V2_SIGNER = Context.key("v2 signer active");

    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    private static final V2StreamingBridge INSTANCE = new V2StreamingBridge();

    private V2StreamingBridge() {
    }

    public static V2StreamingBridge instance() {
        return INSTANCE;
    }

    /**
     * Per-call configuration for an operation with a streaming request body.
     *
     * <p>The interceptor is added per call rather than installed on the client so that a non-streaming
     * operation never walks past it. Generated code calls this and passes the result to
     * {@code SmithyBridgeClient#invoke}.
     *
     * @param requestBody the caller's body.
     * @return a contribution to the call's per-call config, carrying the body and the interceptor that applies it.
     */
    public static Consumer<RequestOverrideConfig.Builder> forRequestBody(RequestBody requestBody) {
        return forRequestBody(V2DataStreams.toDataStream(requestBody));
    }

    /**
     * Per-call configuration for a streaming request body that is already a {@link DataStream}: the
     * async client's case, where {@link V2DataStreams#toDataStream(software.amazon.awssdk.core.async.AsyncRequestBody)}
     * has adapted the caller's {@code AsyncRequestBody}.
     *
     * @param requestBody the body.
     * @return a contribution to the call's per-call config, carrying the body and the interceptor that applies it.
     */
    public static Consumer<RequestOverrideConfig.Builder> forRequestBody(DataStream requestBody) {
        return b -> b.putConfig(REQUEST_BODY, requestBody).addInterceptor(INSTANCE);
    }

    /**
     * Per-call configuration for an operation with a streaming response body.
     *
     * @param sink where the interceptor should leave the response body; generated code reads it after the
     *             call returns and hands it to the caller's {@code ResponseTransformer}.
     * @return a contribution to the call's per-call config, carrying the sink and the interceptor that fills it.
     */
    public static Consumer<RequestOverrideConfig.Builder> forResponseBody(AtomicReference<DataStream> sink) {
        return b -> b.putConfig(RESPONSE_BODY_SINK, sink).addInterceptor(INSTANCE);
    }

    /**
     * Per-call configuration for an operation that streams in both directions.
     *
     * @param requestBody the caller's body.
     * @param sink        where to leave the response body.
     * @return a contribution to the call's per-call config, carrying both, and the interceptor.
     */
    public static Consumer<RequestOverrideConfig.Builder> forBoth(RequestBody requestBody,
                                                                  AtomicReference<DataStream> sink) {
        return forBoth(V2DataStreams.toDataStream(requestBody), sink);
    }

    /**
     * Per-call configuration for an operation that streams in both directions, with the request body
     * already adapted.
     *
     * @param requestBody the body.
     * @param sink        where to leave the response body.
     * @return a contribution to the call's per-call config, carrying both, and the interceptor.
     */
    public static Consumer<RequestOverrideConfig.Builder> forBoth(DataStream requestBody,
                                                                  AtomicReference<DataStream> sink) {
        return b -> b.putConfig(REQUEST_BODY, requestBody).putConfig(RESPONSE_BODY_SINK, sink).addInterceptor(INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <RequestT> RequestT modifyBeforeRetryLoop(RequestHook<?, ?, RequestT> hook) {
        DataStream body = hook.context().get(REQUEST_BODY);
        if (body == null) {
            return hook.request();
        }
        if (!(hook.request() instanceof HttpRequest request)) {
            return hook.request();
        }

        // Before the retry loop, not per attempt, and that choice is about v2 compatibility rather than
        // cost. V2InterceptorBridge runs v2's modifyHttpRequest at modifyBeforeSigning, which is inside
        // the loop; attaching the body there too would mean a v2 interceptor sees a streaming request
        // with no Content-Length, and S3's own StreamingRequestInterceptor decides whether to send
        // Expect: 100-continue from exactly that header. Attaching here puts the body and its length on
        // the request before any v2 interceptor looks, whatever order the interceptors happen to be in.
        //
        // Nothing resets the request between attempts, so both the body and the header below survive a
        // retry, and the replayable DataStream is what makes re-sending them correct.
        ModifiableHttpRequest modifiable = request.toModifiableCopy();
        modifiable.setBody(body);
        if (!Boolean.TRUE.equals(hook.context().get(V2_SIGNER))) {
            modifiable.setHeader("x-amz-content-sha256", UNSIGNED_PAYLOAD);
        }

        if (body.hasKnownLength()) {
            modifiable.setHeader("content-length", Long.toString(body.contentLength()));
        }

        return (RequestT) modifiable;
    }

    /**
     * Refuses a streamed body over plain HTTP.
     *
     * <p>Checked here, and not in {@link #modifyBeforeSigning}, because the endpoint is not on the
     * request yet at that point: {@code ClientPipeline} calls {@code modifyBeforeSigning} first and
     * {@code ClientProtocol.setServiceEndpoint} only afterwards, so {@code uri().getScheme()} is null
     * there. This is the first hook that sees the resolved endpoint, and it still runs before anything is
     * transmitted, so the caller sees the same thing either way — one wasted signature on a request that
     * was never going to be sent.
     */
    @Override
    public void readAfterSigning(RequestHook<?, ?, ?> hook) {
        if (hook.context().get(REQUEST_BODY) == null || Boolean.TRUE.equals(hook.context().get(V2_SIGNER))) {
            return;
        }
        if (!(hook.request() instanceof HttpRequest request)) {
            return;
        }
        String scheme = request.uri().getScheme();
        if (!"https".equalsIgnoreCase(scheme)) {
            throw SdkClientException.create(
                "Cannot stream a request body over " + scheme + ": SigV4 requires either a payload hash, "
                + "which would mean buffering the whole body, or chunked signing, which smithy-java does "
                + "not implement. Use an https endpoint.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ResponseT> ResponseT modifyBeforeDeserialization(ResponseHook<?, ?, ?, ResponseT> hook) {
        AtomicReference<DataStream> sink = hook.context().get(RESPONSE_BODY_SINK);
        if (sink == null) {
            return hook.response();
        }
        if (!(hook.response() instanceof HttpResponse response)) {
            return hook.response();
        }

        // Only a successful response carries the object. An error response carries an XML document that
        // the deserializer must read to build the exception, so it has to be left alone -- taking the body
        // away from a 404 would turn NoSuchKeyException into an unhelpful generic failure.
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return hook.response();
        }

        sink.set(response.body());
        return (ResponseT) HttpResponse.of(response.httpVersion(),
                                           response.statusCode(),
                                           response.headers(),
                                           DataStream.ofEmpty());
    }
}

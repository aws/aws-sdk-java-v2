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

package software.amazon.awssdk.bridge.smithyjava.interceptors;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.bridge.smithyjava.auth.V2SigningAuthScheme;
import software.amazon.awssdk.bridge.smithyjava.client.V2RequestOverrides;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.bridge.smithyjava.transport.ResponseBodyDataStream;
import software.amazon.awssdk.bridge.smithyjava.transport.V2TransportFailures;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.http.SdkHttpUtils;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.InputHook;
import software.amazon.smithy.java.client.core.interceptors.OutputHook;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.client.core.interceptors.ResponseHook;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Runs a customer's v2 {@link ExecutionInterceptor} chain from inside smithy-java's pipeline.
 *
 * <p>This is what lets customer-authored interceptors — and the SDK's own classpath-discovered ones —
 * keep working when the pipeline underneath is smithy-java. The whole v2 chain is driven through
 * {@link ExecutionInterceptorChain} so that v2's ordering rules (forward for {@code before*}, reverse
 * for {@code after*}) and its result validation are preserved rather than re-implemented.
 *
 * <h2>Installed lazily</h2>
 *
 * <p>Bridging is not free: it materializes a v2 {@link InterceptorContext}, an
 * {@link ExecutionAttributes} copy, and — on the signing hook — a v2 {@link SdkHttpRequest} converted
 * from and back to smithy's representation. So the config translator only installs this interceptor
 * when the client actually has interceptors that smithy does not already replace. A default client
 * pays nothing.
 *
 * <h2>Hook mapping</h2>
 *
 * <pre>
 *   smithy-java ClientInterceptor      AWS SDK v2 ExecutionInterceptor
 *   ----------------------------       -------------------------------
 *   readBeforeExecution          --&gt;   beforeExecution
 *   modifyBeforeSerialization    --&gt;   modifyRequest
 *   modifyBeforeSigning          --&gt;   modifyHttpRequest / modifyHttpContent
 *   modifyBeforeDeserialization  --&gt;   afterTransmission, modifyHttpResponse,
 *                                       modifyHttpResponseContent | modifyAsyncHttpResponseContent,
 *                                       beforeUnmarshalling
 *   modifyBeforeCompletion       --&gt;   afterUnmarshalling, modifyResponse
 *   readAfterExecution           --&gt;   afterExecution | onExecutionFailure
 * </pre>
 *
 * <p>The response hooks matter more than they look, because v2 itself uses them: every v2 client carries
 * {@code HttpChecksumValidationInterceptor}, which is how response checksums are validated at all, and S3
 * layers several more on top ({@code modifyResponse} decodes URL-encoded listing keys, for one). They see
 * the attempt's checksum spec because {@link #modifyBeforeDeserialization} adopts the attributes the signer
 * bridge resolved; see {@code compatability_issues.md} 2.1 and 6.1.
 *
 * <p>{@code modifyRequest} maps to {@code modifyBeforeSerialization} and {@code modifyHttpRequest} to
 * {@code modifyBeforeSigning} because that is where each sits relative to serialization and signing in
 * v2: a header added by {@code modifyHttpRequest} must end up inside the signature, as it does in v2.
 *
 * <p>The remaining twelve v2 hooks are not bridged; see {@code compatability_issues.md} section 2.1 for
 * the full list and what depends on each.
 */
@SdkProtectedApi
public final class V2InterceptorBridge implements ClientInterceptor {

    // Per-call state, threaded through the smithy call context because the v2 chain is stateful across
    // hooks (each hook sees the InterceptorContext the previous one produced).
    private static final software.amazon.smithy.java.context.Context.Key<CallState> STATE =
            software.amazon.smithy.java.context.Context.key("v2InterceptorBridgeState");

    private final ExecutionInterceptorChain chain;
    private final ExecutionAttributes template;
    private final URI placeholderEndpoint;

    /**
     * @param interceptors the v2 interceptors to run, in v2 order.
     * @param v2Config     client configuration, read once to seed the execution attributes.
     */
    public V2InterceptorBridge(List<ExecutionInterceptor> interceptors, SdkClientConfiguration v2Config) {
        this.chain = new ExecutionInterceptorChain(interceptors);
        this.template = buildTemplate(v2Config);
        this.placeholderEndpoint = clientEndpoint(v2Config);
    }

    /**
     * The scheme/host/port shown to v2 interceptors, because smithy has not resolved one yet.
     *
     * <p>smithy resolves the endpoint <em>after</em> {@code modifyBeforeSigning}, so the request URI at
     * that point is smithy's unresolved placeholder: no scheme, no host, path {@code "/"}. v2's
     * {@code modifyHttpRequest} is specified to see a complete URI, and a v2 {@code SdkHttpFullRequest}
     * cannot even be built without a protocol. The client endpoint is the closest available answer; it
     * matches the resolved endpoint for a client with an endpoint override, and differs from it whenever
     * the endpoint rules rewrite the host (accounts endpoints, FIPS/dualstack, host prefixes).
     * See {@code compatability_issues.md} section 2.1.
     */
    private static URI clientEndpoint(SdkClientConfiguration v2Config) {
        ClientEndpointProvider provider = v2Config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER);
        if (provider != null && provider.clientEndpoint() != null) {
            return provider.clientEndpoint();
        }
        return URI.create("https://unresolved.invalid");
    }

    // Client-scoped attributes, built once and copied per call. This is not the full set v2 populates —
    // metric collectors, checksum specs, and the auth-scheme attributes are absent. See 2.1.
    private static ExecutionAttributes buildTemplate(SdkClientConfiguration v2Config) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AwsExecutionAttribute.AWS_REGION, v2Config.option(AwsClientOption.AWS_REGION));
        attributes.putAttribute(AwsExecutionAttribute.ENDPOINT_PREFIX,
                                v2Config.option(AwsClientOption.ENDPOINT_PREFIX));
        attributes.putAttribute(SdkExecutionAttribute.SERVICE_NAME, v2Config.option(SdkClientOption.SERVICE_NAME));
        attributes.putAttribute(SdkExecutionAttribute.CLIENT_TYPE, v2Config.option(SdkClientOption.CLIENT_TYPE));
        // The client's checksum modes, which S3's interceptors read on the request side: with response
        // validation at its WHEN_SUPPORTED default, EnableTrailingChecksumInterceptor asks GetObject for a
        // trailing MD5 (x-amz-te: append-md5), and the response half -- stripping and checking those 16 bytes
        // -- runs in the response hooks this class now bridges. See compatability_issues.md 13.6.
        attributes.putAttribute(SdkInternalExecutionAttribute.REQUEST_CHECKSUM_CALCULATION,
                                v2Config.option(SdkClientOption.REQUEST_CHECKSUM_CALCULATION));
        attributes.putAttribute(SdkInternalExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION,
                                v2Config.option(SdkClientOption.RESPONSE_CHECKSUM_VALIDATION));
        // The service's own configuration object (S3Configuration, and its equivalents elsewhere). Worth
        // the line because omitting it does not disable the interceptors that read it -- it silently sends
        // them down their no-configuration path. S3's StreamingRequestInterceptor is the clearest case: it
        // reads expectContinueThresholdInBytes, whose default is 1 MiB, and falls back to a threshold of
        // 0 when there is no config, so every PutObject of any size gets Expect: 100-continue.
        ServiceConfiguration serviceConfiguration = v2Config.option(SdkClientOption.SERVICE_CONFIGURATION);
        if (serviceConfiguration != null) {
            attributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, serviceConfiguration);
        }
        return attributes;
    }

    @Override
    public void readBeforeExecution(InputHook<?, ?> hook) {
        if (!(hook.input() instanceof SdkRequest request)) {
            return;
        }
        ExecutionAttributes attributes = template.copy();
        attributes.putAttribute(SdkExecutionAttribute.OPERATION_NAME,
                                hook.operation().schema().id().getName());
        // A request's own execution attributes, which stock v2 hands to every interceptor (ledger 2.4).
        RequestOverrideConfiguration overrides = hook.context().get(V2RequestOverrides.KEY);
        if (overrides != null && overrides.executionAttributes() != null) {
            attributes = overrides.executionAttributes().merge(attributes);
        }
        CallState state = new CallState(InterceptorContext.builder().request(request).build(), attributes);
        hook.context().put(STATE, state);
        chain.beforeExecution(state.context, state.attributes);
    }

    @Override
    public <I extends SerializableStruct> I modifyBeforeSerialization(InputHook<I, ?> hook) {
        CallState state = hook.context().get(STATE);
        if (state == null) {
            return hook.input();
        }
        InterceptorContext result = chain.modifyRequest(state.context, state.attributes);
        state.context = result;
        SdkRequest modified = result.request();
        // Reference equality: the chain only copies the context when an interceptor actually swapped the
        // request, so an unmodified request costs nothing here.
        return modified == hook.input() ? hook.input() : hook.asInputType((SerializableStruct) modified);
    }

    @Override
    public <RequestT> RequestT modifyBeforeSigning(RequestHook<?, ?, RequestT> hook) {
        CallState state = hook.context().get(STATE);
        if (state == null || !(hook.request() instanceof HttpRequest smithyRequest)) {
            return hook.request();
        }

        SdkHttpRequest before = toV2Request(smithyRequest);
        state.context = state.context.copy(b -> b.httpRequest(before));
        InterceptorContext result = chain.modifyHttpRequestAndHttpContent(state.context, state.attributes);
        state.context = result;

        SdkHttpRequest after = result.httpRequest();
        if (after == before) {
            return hook.request();
        }
        // A RequestBody swapped in by modifyHttpContent is dropped: smithy owns serialization and the
        // body is already a DataStream. See compatability_issues.md 2.1.
        ModifiableHttpRequest modifiable = smithyRequest.toModifiable();
        modifiable.setMethod(after.method().name());
        modifiable.setUri(writeBackUri(after));
        modifiable.setHeaders(after.headers());
        return hook.asRequestType(modifiable);
    }

    /**
     * v2's response-side hooks, run on the HTTP response before smithy deserializes it.
     *
     * <p>The body is handed to the chain in the form the client type expects — an {@code InputStream} for
     * a sync client, a publisher for an async one, as v2 does — and whatever the chain returns becomes the
     * body smithy deserializes, or, for a streaming operation, the body the caller's transformer reads. A
     * validating wrapper therefore sits between the transport and the caller exactly where v2 puts it.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <ResponseT> ResponseT modifyBeforeDeserialization(ResponseHook<?, ?, ?, ResponseT> hook) {
        CallState state = hook.context().get(STATE);
        if (state == null || !(hook.response() instanceof HttpResponse response)
            || V2TransportFailures.isStandIn(response)) {
            // No response exists for a failed transport attempt; v2 runs no response hooks for one.
            return hook.response();
        }
        adoptSigningAttributes(hook.context(), state);

        boolean async = state.attributes.getAttribute(SdkExecutionAttribute.CLIENT_TYPE) == ClientType.ASYNC;
        DataStream body = response.body();
        InputStream in = async || body == null ? null : body.asInputStream();
        Publisher<ByteBuffer> publisher = async && body != null ? FlowAdapters.toPublisher(body) : null;

        SdkHttpResponse v2Response = toV2Response(response);
        state.context = state.context.copy(b -> b.httpResponse(v2Response).responseBody(in).responsePublisher(publisher));
        chain.afterTransmission(state.context, state.attributes);
        InterceptorContext result = chain.modifyHttpResponse(state.context, state.attributes);
        if (async) {
            result = chain.modifyAsyncHttpResponse(result, state.attributes);
        }
        state.context = result;
        chain.beforeUnmarshalling(result, state.attributes);

        DataStream newBody;
        long length = body == null ? -1 : body.contentLength();
        String type = body == null ? null : body.contentType();
        if (async) {
            Publisher<ByteBuffer> after = result.responsePublisher().orElse(publisher);
            newBody = after == publisher ? body : new ResponseBodyDataStream(after, type, length);
        } else {
            // The sync body was opened to show it to the chain, so it is always re-wrapped -- the original
            // DataStream has been consumed either way.
            InputStream after = result.responseBody().orElse(in);
            newBody = after == null ? body : DataStream.ofInputStream(after, type, length);
        }
        SdkHttpResponse headers = result.httpResponse();
        if (headers == v2Response && newBody == body) {
            return hook.response();
        }
        return (ResponseT) HttpResponse.of(response.httpVersion(), headers.statusCode(),
                                           software.amazon.smithy.java.http.api.HttpHeaders.of(headers.headers()),
                                           newBody);
    }

    /** v2's {@code afterUnmarshalling} and {@code modifyResponse}, once per call, on success only. */
    @Override
    public <O extends SerializableStruct> O modifyBeforeCompletion(OutputHook<?, O, ?, ?> hook, RuntimeException error) {
        CallState state = hook.context().get(STATE);
        if (error != null || state == null || !(hook.output() instanceof SdkResponse output)) {
            return error != null ? hook.forward(error) : hook.output();
        }
        state.context = state.context.copy(b -> b.response(output));
        chain.afterUnmarshalling(state.context, state.attributes);
        InterceptorContext result = chain.modifyResponse(state.context, state.attributes);
        state.context = result;
        return result.response() == output ? hook.output() : hook.asOutputType((SerializableStruct) result.response());
    }

    /**
     * The attempt's checksum spec and selected auth scheme, as the signer bridge resolved them.
     *
     * <p>They are resolved per attempt, after this interceptor's per-call attributes were created, and
     * travel on the endpoint ({@code V2SigningAuthScheme.SIGNING_INPUTS}). v2's response checksum
     * validation reads {@code RESOLVED_CHECKSUM_SPECS}, which is a view onto the selected auth scheme, so
     * both have to be here before the response hooks run.
     */
    private static void adoptSigningAttributes(software.amazon.smithy.java.context.Context context, CallState state) {
        Endpoint endpoint = context.get(CallContext.ENDPOINT);
        V2SigningAuthScheme.SigningInputs inputs =
            endpoint == null ? null : endpoint.property(V2SigningAuthScheme.SIGNING_INPUTS);
        if (inputs != null) {
            state.attributes.putAbsentAttributes(inputs.attributes());
        }
    }

    @Override
    public void readAfterExecution(OutputHook<?, ?, ?, ?> hook, RuntimeException error) {
        CallState state = hook.context().get(STATE);
        if (state == null) {
            return;
        }
        if (hook.response() instanceof HttpResponse response) {
            SdkHttpResponse v2Response = toV2Response(response);
            state.context = state.context.copy(b -> b.httpResponse(v2Response));
        }
        if (error != null) {
            chain.onExecutionFailure(new FailedExecution(state.context, error), state.attributes);
            return;
        }
        if (hook.output() instanceof SdkResponse response) {
            state.context = state.context.copy(b -> b.response(response));
        }
        chain.afterExecution(state.context, state.attributes);
    }

    // ---- conversions ---------------------------------------------------------

    /**
     * The URI to hand back to smithy after the v2 chain has run.
     *
     * <p>Whatever host the interceptors saw is discarded: smithy resolves the endpoint after this hook
     * and {@code setServiceEndpoint} overwrites scheme, host and port, so a host change made here would
     * be silently dropped anyway (see {@code compatability_issues.md} 2.1). Only the path and query
     * survive — and the path is made relative again, because {@code setServiceEndpoint} concatenates the
     * endpoint's path in front of it and would otherwise duplicate the prefix.
     */
    private URI writeBackUri(SdkHttpRequest after) {
        String prefix = placeholderEndpoint.getRawPath();
        String path = after.encodedPath();
        if (prefix != null && !prefix.isEmpty() && !prefix.equals("/") && path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        String query = SdkHttpUtils.encodeAndFlattenQueryParameters(after.rawQueryParameters()).orElse(null);
        return URI.create(query == null ? path : path + "?" + query);
    }

    private SdkHttpRequest toV2Request(HttpRequest request) {
        URI uri = request.uri().toURI();
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                                                              .method(SdkHttpMethod.fromValue(request.method()));
        if (uri.getScheme() == null || uri.getHost() == null) {
            // Unresolved placeholder: keep the path smithy serialized, and borrow the scheme, host and
            // port from the client endpoint. Set piecewise rather than through uri(URI), which appends
            // paths in the opposite order. See clientEndpoint().
            builder.protocol(placeholderEndpoint.getScheme())
                   .host(placeholderEndpoint.getHost())
                   .port(placeholderEndpoint.getPort())
                   .encodedPath(SdkHttpUtils.appendUri(placeholderEndpoint.getRawPath(), uri.getRawPath()));
        } else {
            builder.uri(uri);
        }
        // Builder.uri(URI) does not carry query parameters across, so copy them explicitly. Guarded
        // because uriParams throws NPE on a URI with no query string, which is every awsJson request.
        if (uri.getRawQuery() != null) {
            for (Map.Entry<String, List<String>> param : SdkHttpUtils.uriParams(uri).entrySet()) {
                builder.putRawQueryParameter(param.getKey(), param.getValue());
            }
        }
        for (Map.Entry<String, List<String>> header : request.headers().map().entrySet()) {
            builder.putHeader(header.getKey(), header.getValue());
        }
        return builder.build();
    }

    private static SdkHttpResponse toV2Response(HttpResponse response) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(response.statusCode())
                                  .headers(response.headers().map())
                                  .build();
    }

    private static final class CallState {
        private InterceptorContext context;
        private final ExecutionAttributes attributes;

        private CallState(InterceptorContext context, ExecutionAttributes attributes) {
            this.context = context;
            this.attributes = attributes;
        }
    }

    /**
     * A {@link Context.FailedExecution} over whatever the bridge managed to capture before the failure.
     *
     * <p>Implemented here rather than reusing v2's {@code DefaultFailedExecutionContext} to keep the
     * bridge off of {@code sdk-core} internals.
     */
    private static final class FailedExecution implements Context.FailedExecution {
        private final InterceptorContext context;
        private final Throwable exception;

        private FailedExecution(InterceptorContext context, Throwable exception) {
            this.context = context;
            this.exception = exception;
        }

        @Override
        public Throwable exception() {
            return exception;
        }

        @Override
        public SdkRequest request() {
            return context.request();
        }

        @Override
        public Optional<SdkHttpRequest> httpRequest() {
            return Optional.ofNullable(context.httpRequest());
        }

        @Override
        public Optional<SdkHttpResponse> httpResponse() {
            return Optional.ofNullable(context.httpResponse());
        }

        @Override
        public Optional<SdkResponse> response() {
            return Optional.ofNullable(context.response());
        }
    }
}

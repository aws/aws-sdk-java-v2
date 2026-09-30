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

package software.amazon.awssdk.bridge.smithyjava.client;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.bridge.smithyjava.error.V2ModeledError;
import software.amazon.awssdk.bridge.smithyjava.error.V2NonReplayableError;
import software.amazon.awssdk.bridge.smithyjava.error.V2RetryableError;
import software.amazon.awssdk.bridge.smithyjava.error.V2UnmodeledError;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.smithy.java.client.core.Client;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.client.core.error.TransportException;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.endpoints.EndpointResolver;

/**
 * The smithy-java client that a generated AWS SDK v2 client delegates to.
 *
 * <p>Generated v2 clients hold one of these, built by {@link V2ConfigTranslator} from the v2
 * {@code SdkClientConfiguration}, and call {@link #invoke(SerializableStruct, ApiOperation)} once per
 * operation. Everything between that call and the wire — serialization, endpoint resolution, auth,
 * signing, retries, deserialization — is smithy-java's.
 *
 * <p>This class exists for two reasons that {@link Client} alone does not cover:
 *
 * <ol>
 *   <li>{@code Client#call} is {@code protected}, so a subclass is required to expose it.</li>
 *   <li>Exceptions crossing this boundary must look like v2 exceptions. smithy-java throws
 *       {@link CallException} subtypes; v2 callers catch {@code DynamoDbException},
 *       {@code AwsServiceException}, and {@code SdkClientException}. Translating here — rather than in
 *       an interceptor or an {@code ExceptionMapperPlugin} — keeps the translation off the hot path
 *       entirely and keeps this class free of service-specific types.</li>
 * </ol>
 *
 * @see V2ModeledError for how modeled errors survive the round trip as real v2 exception instances
 */
@SdkProtectedApi
public final class SmithyBridgeClient extends Client {

    /**
     * Starts one virtual thread per async call; see {@link #runAsync}. Named so a thread dump taken under
     * load says which threads are parked call envelopes rather than leaving them anonymous.
     */
    private static final ThreadFactory ENVELOPE_THREADS = Thread.ofVirtual().name("sdk-smithy-bridge-call-", 0).factory();

    private final Supplier<? extends AwsServiceException.Builder> baseExceptionBuilder;
    private final Executor completionExecutor;
    private final V2RequestOverride requestOverrides;
    private final Duration apiCallTimeout;

    private SmithyBridgeClient(Builder builder) {
        super(builder);
        this.baseExceptionBuilder = builder.baseExceptionBuilder;
        this.completionExecutor = builder.completionExecutor;
        this.apiCallTimeout = builder.apiCallTimeout;
        this.requestOverrides = builder.v2Configuration == null
                                ? null
                                : new V2RequestOverride(builder.v2Configuration, builder.endpointResolverFactory,
                                                         builder.requestConfigurationUpdater);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Invokes an operation and translates any failure into the v2 exception a caller expects.
     *
     * @param input     operation input; also a v2 {@code SdkRequest}.
     * @param operation the generated operation.
     * @param <I>       input shape.
     * @param <O>       output shape.
     * @return the deserialized output, which is also a v2 {@code SdkResponse}.
     */
    public <I extends SerializableStruct, O extends SerializableStruct> O invoke(I input, ApiOperation<I, O> operation) {
        return invoke(input, operation, null);
    }

    /**
     * Invokes an operation with per-call overrides, translating any failure as {@link #invoke} does.
     *
     * <p>Streaming operations use this: the body cannot travel inside the input shape, because v2's
     * generated shapes have no member for it, so it travels in the override config's context instead. See
     * {@link software.amazon.awssdk.bridge.smithyjava.streaming.V2StreamingBridge}.
     *
     * @param input     operation input; also a v2 {@code SdkRequest}.
     * @param operation the generated operation.
     * @param overrides a contribution to the per-call configuration (streaming bodies), or null for none.
     * @param <I>       input shape.
     * @param <O>       output shape.
     * @return the deserialized output, which is also a v2 {@code SdkResponse}.
     */
    public <I extends SerializableStruct, O extends SerializableStruct> O invoke(
            I input, ApiOperation<I, O> operation, Consumer<RequestOverrideConfig.Builder> overrides) {
        try {
            RequestOverrideConfig effective = requestOverrides == null
                                              ? (overrides == null ? null : build(overrides))
                                              : requestOverrides.apply(input, overrides);
            return V2Timeout.apiCall(apiCallTimeout(input), () -> call(input, operation, effective));
        } catch (RuntimeException e) {
            throw toV2(e);
        }
    }

    /**
     * Invokes an operation asynchronously, the way a generated v2 async client does.
     *
     * @param input     operation input; also a v2 {@code SdkRequest}.
     * @param operation the generated operation.
     * @param <I>       input shape.
     * @param <O>       output shape.
     * @return a future of the deserialized output, failed as v2 fails its futures.
     * @see #runAsync
     */
    public <I extends SerializableStruct, O extends SerializableStruct> CompletableFuture<O> invokeAsync(
            I input, ApiOperation<I, O> operation) {
        return invokeAsync(input, operation, null);
    }

    /**
     * Invokes an operation asynchronously with per-call overrides.
     *
     * @param input     operation input; also a v2 {@code SdkRequest}.
     * @param operation the generated operation.
     * @param overrides a contribution to the per-call configuration (streaming bodies), or null for none.
     * @param <I>       input shape.
     * @param <O>       output shape.
     * @return a future of the deserialized output, failed as v2 fails its futures.
     * @see #runAsync
     */
    public <I extends SerializableStruct, O extends SerializableStruct> CompletableFuture<O> invokeAsync(
            I input, ApiOperation<I, O> operation, Consumer<RequestOverrideConfig.Builder> overrides) {
        return runAsync(() -> CompletableFuture.completedFuture(invoke(input, operation, overrides)));
    }

    /**
     * Runs a blocking call envelope on its own virtual thread and exposes it as a v2-shaped future.
     *
     * <p>This is the whole async model of the bridge, and it is deliberately small. smithy-java's pipeline
     * is synchronous — {@code Client#call} returns its output — so something has to be parked while a
     * call is in flight, and the only choices are what and for how long. Here it is a virtual thread,
     * parked for the call envelope: interceptors, serialization, endpoint and identity resolution,
     * signing, and the wait for response <em>headers</em>. The I/O itself is not on this thread. With
     * {@link software.amazon.awssdk.bridge.smithyjava.transport.V2AsyncTransportBridge} underneath, request
     * and response bodies move on Netty's or CRT's event loops, and a parked virtual thread costs a few
     * hundred bytes of heap rather than a platform thread's stack.
     *
     * <p>Three v2 behaviors are reproduced on purpose:
     *
     * <ul>
     *   <li><b>The completion hop.</b> v2 completes the future a caller holds on
     *       {@code FUTURE_COMPLETION_EXECUTOR}, so that a caller's {@code thenApply} never runs on an I/O
     *       thread. The envelope thread is not an I/O thread, so the hop protects less here than it does in
     *       v2 — but a caller may have configured that executor for context propagation or thread naming,
     *       and skipping it would silently ignore their configuration.</li>
     *   <li><b>How failures look.</b> v2 fails its futures with a {@link CompletionException} whose cause
     *       is the {@code SdkException} ({@code AsyncExecutionFailureExceptionReportingStage}); a
     *       {@code whenComplete} that reads {@code t.getCause()} depends on exactly that.</li>
     *   <li><b>Nothing throws synchronously.</b> An executor that rejects the task still yields a failed
     *       future, never an exception from the call site.</li>
     * </ul>
     *
     * @param body the call envelope; runs on a virtual thread and may block.
     * @param <T>  what the call produces.
     * @return a future completed on the configured completion executor.
     */
    public <T> CompletableFuture<T> runAsync(Supplier<CompletableFuture<T>> body) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            ENVELOPE_THREADS.newThread(() -> {
                CompletableFuture<T> produced;
                try {
                    produced = body.get();
                } catch (Throwable t) {
                    completeOnCompletionExecutor(result, null, t);
                    return;
                }
                produced.whenComplete((value, error) -> completeOnCompletionExecutor(result, value, error));
            }).start();
        } catch (RuntimeException e) {
            // Thread creation failing is an out-of-resources condition, not a bug in the call; surface it
            // through the future like every other failure.
            result.completeExceptionally(asCompletionException(e));
        }
        return result;
    }

    private static RequestOverrideConfig build(Consumer<RequestOverrideConfig.Builder> contribution) {
        RequestOverrideConfig.Builder builder = RequestOverrideConfig.builder();
        contribution.accept(builder);
        return builder.build();
    }

    /** The request's own {@code apiCallTimeout}, else the client's. */
    private Duration apiCallTimeout(SerializableStruct input) {
        if (input instanceof SdkRequest request) {
            Optional<Duration> own = request.overrideConfiguration().flatMap(c -> c.apiCallTimeout());
            if (own.isPresent()) {
                return own.get();
            }
        }
        return apiCallTimeout;
    }

    private <T> void completeOnCompletionExecutor(CompletableFuture<T> result, T value, Throwable error) {
        Runnable complete = error == null
                            ? () -> result.complete(value)
                            : () -> result.completeExceptionally(asCompletionException(error));
        if (completionExecutor == null) {
            complete.run();
            return;
        }
        try {
            completionExecutor.execute(complete);
        } catch (RejectedExecutionException e) {
            // v2's default completion pool has a bounded queue. Completing inline is what v2 falls back to
            // as well (the future still completes, on whatever thread is here), and it is strictly better
            // than the alternative of a future that never completes at all.
            complete.run();
        }
    }

    private static CompletionException asCompletionException(Throwable error) {
        return error instanceof CompletionException completion ? completion : new CompletionException(error);
    }

    /**
     * Translates anything that crosses the client boundary into the v2 exception a caller expects.
     *
     * <p>Public so the streaming invokers, which live in another package, can apply the same translation
     * to failures that happen outside {@link #invoke}.
     */
    public RuntimeException toV2(RuntimeException error) {
        if (error instanceof V2ModeledError e) {
            // A modeled error: the real v2 exception was built by the generated builder and is carried
            // inside the shim. Unwrap and throw it, so `catch (ConditionalCheckFailedException e)` works.
            return e.v2Exception();
        }
        if (error instanceof V2UnmodeledError e) {
            // An error response that matched no modeled shape. V2ErrorEnricher already built the
            // service's base exception from the HTTP response, so this is the same unwrap.
            return e.v2Exception();
        }
        if (error instanceof V2RetryableError e) {
            // A carrier the enricher substituted purely so the retry strategy could see a
            // classification the real failure's type refuses to report. Retries are over by now, so
            // the carrier has done its job and the caller should see the failure it wrapped.
            return unwrapped(e.original());
        }
        if (error instanceof V2NonReplayableError e) {
            // The opposite carrier: substituted so the retry strategy would decline to re-send a body
            // that cannot be re-read. Same reasoning; the caller sees what actually went wrong.
            return unwrapped(e.original());
        }
        if (error instanceof CallException e) {
            return translate(e);
        }
        return error;
    }

    // A carrier's original: a v2 exception passes through as is (an attempt timeout, a modeled error); any
    // other failure is translated like a bare one -- a torn body arrives as an UncheckedIOException or a
    // smithy SerializationException, and a v2 caller must see SdkClientException for either, as on stock.
    private RuntimeException unwrapped(RuntimeException original) {
        RuntimeException v2 = toV2(original);
        return v2 instanceof SdkException ? v2 : translate(v2);
    }

    private RuntimeException translate(RuntimeException e) {
        // Anything the bridge itself threw (transport, credentials, endpoints) already is an
        // SdkException; preserve it rather than re-wrapping so v2 error handling sees the real type.
        for (Throwable cause = e.getCause(); cause != null && cause.getCause() != cause; cause = cause.getCause()) {
            if (cause instanceof SdkException sdkException) {
                return sdkException;
            }
        }
        // A transport failure, or anything that is not a service error at all (a bare
        // SerializationException from a response the codec could not read), maps to the client-side
        // exception v2 uses for the same conditions -- not to the service's exception, which would
        // claim the service reported something it did not.
        if (e instanceof TransportException && e.getCause() != null) {
            // The transport failure itself as the cause, as stock v2 reports it (NoHttpResponseException,
            // ConnectException), not smithy's remapping wrapper around it.
            return SdkClientException.builder().message(e.getCause().getMessage()).cause(e.getCause()).build();
        }
        if (e instanceof TransportException || !(e instanceof CallException) || baseExceptionBuilder == null) {
            return SdkClientException.builder().message(e.getMessage()).cause(e).build();
        }
        return baseExceptionBuilder.get().message(e.getMessage()).cause(e).build();
    }

    /**
     * Builder for {@link SmithyBridgeClient}.
     *
     * <p>Adds {@code service} — which {@link Client.Builder} does not expose, though
     * {@code ClientConfig} requires it — and the fallback error factory.
     */
    public static final class Builder extends Client.Builder<SmithyBridgeClient, Builder> {

        private Supplier<? extends AwsServiceException.Builder> baseExceptionBuilder;
        private Executor completionExecutor;
        private Duration apiCallTimeout;
        private SdkClientConfiguration v2Configuration;
        private Function<SdkClientConfiguration, EndpointResolver> endpointResolverFactory;
        private Function<SdkRequest, SdkClientConfiguration> requestConfigurationUpdater;

        private Builder() {
        }

        /** The client-level {@code apiCallTimeout}; see {@link V2Timeout}. */
        public Builder apiCallTimeout(Duration apiCallTimeout) {
            this.apiCallTimeout = apiCallTimeout;
            return this;
        }

        /**
         * What {@link V2RequestOverride} needs to rebuild per-call components for a request whose plugins
         * change the configuration. Set by {@link V2ConfigTranslator}.
         */
        Builder requestOverrideSupport(SdkClientConfiguration v2Configuration,
                                       Function<SdkClientConfiguration, EndpointResolver> endpointResolverFactory) {
            this.v2Configuration = v2Configuration;
            this.endpointResolverFactory = endpointResolverFactory;
            return this;
        }

        /**
         * The generated client's own per-request configuration update ({@code updateSdkClientConfiguration}),
         * which runs a request's {@code SdkPlugin}s the way stock v2 does.
         */
        public Builder requestConfigurationUpdater(Function<SdkRequest, SdkClientConfiguration> updater) {
            this.requestConfigurationUpdater = updater;
            return this;
        }

        /**
         * Sets the executor that completes the futures {@link #runAsync} returns.
         *
         * <p>{@link V2ConfigTranslator} passes v2's {@code FUTURE_COMPLETION_EXECUTOR}, so a bridged async
         * client completes on the same threads a stock one does. Null completes on the envelope thread.
         *
         * @param completionExecutor the executor, or null.
         * @return this builder.
         */
        public Builder completionExecutor(Executor completionExecutor) {
            this.completionExecutor = completionExecutor;
            return this;
        }

        /**
         * @param service the generated {@code ApiService} for the service being called.
         * @return this builder.
         */
        public Builder service(ApiService service) {
            configBuilder().service(service);
            return this;
        }

        /**
         * Sets the service's base exception, used for any failure with no more specific v2 type.
         *
         * <p>Generated code passes a reference to the generated base exception's builder, e.g.
         * {@code DynamoDbException::builder}, so an unmodeled server error still surfaces as a
         * {@code DynamoDbException} the way v2 does it. The same supplier is handed to
         * {@link software.amazon.awssdk.bridge.smithyjava.error.V2ErrorEnricher}, which populates it
         * from the HTTP response; this class only uses it for failures that never got a response.
         *
         * @param baseExceptionBuilder supplier of the service base exception's builder.
         * @return this builder.
         */
        public Builder baseExceptionBuilder(Supplier<? extends AwsServiceException.Builder> baseExceptionBuilder) {
            this.baseExceptionBuilder = baseExceptionBuilder;
            return this;
        }

        @Override
        public SmithyBridgeClient build() {
            return new SmithyBridgeClient(this);
        }
    }
}

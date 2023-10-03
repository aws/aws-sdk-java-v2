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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.AsyncRequestBodySigner;
import software.amazon.awssdk.core.signer.AsyncSigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class AsyncSigningStage implements RequestPipeline<SdkHttpFullRequest,
        CompletableFuture<SdkHttpFullRequest>> {

    private static final Logger log = Logger.loggerFor(AsyncSigningStage.class);

    private final HttpClientDependencies dependencies;

    public AsyncSigningStage(HttpClientDependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    @Override
    public CompletableFuture<SdkHttpFullRequest> execute(SdkHttpFullRequest request, RequestExecutionContext context)
            throws Exception {

        updateHttpRequestInInterceptorContext(request, context.executionContext());

        // Whether pre / post SRA, if old Signer is setup in context, that's the one to use
        if (context.signer() != null) {
            return signRequest(request, context);
        }
        // else if AUTH_SCHEMES != null (implies SRA), use SelectedAuthScheme
        if (context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES) != null) {
            SelectedAuthScheme<?> selectedAuthScheme =
                context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
            log.debug(() -> String.format("Using SelectedAuthScheme: %s", selectedAuthScheme.authSchemeOption().schemeId()));
            return sraSignRequest(request, context, selectedAuthScheme);
        }
        // else, this implies pre SRA client with authType=None, so don't need to do anything
        return CompletableFuture.completedFuture(request);
    }

    private <T extends Identity> CompletableFuture<SdkHttpFullRequest> sraSignRequest(SdkHttpFullRequest request,
                                                                                      RequestExecutionContext context,
                                                                                      SelectedAuthScheme<T> selectedAuthScheme) {
        adjustForClockSkew(context.executionAttributes());
        CompletableFuture<? extends T> identityFuture = selectedAuthScheme.identity();
        return identityFuture.thenCompose(identity -> {
            CompletableFuture<SdkHttpFullRequest> signedRequestFuture = MetricUtils.reportDuration(
                () -> doSraSign(request, context, selectedAuthScheme, identity),
                context.attemptMetricCollector(),
                CoreMetric.SIGNING_DURATION);

            return signedRequestFuture.thenApply(r -> {
                updateHttpRequestInInterceptorContext(r, context.executionContext());
                return r;
            });
        });
    }

    private <T extends Identity> CompletableFuture<SdkHttpFullRequest> doSraSign(SdkHttpFullRequest request,
                                                                                 RequestExecutionContext context,
                                                                                 SelectedAuthScheme<T> selectedAuthScheme,
                                                                                 T identity) {
        AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();
        HttpSigner<T> signer = selectedAuthScheme.signer();

        if (context.requestProvider() == null) {
            SignRequest.Builder<T> signRequestBuilder = SignRequest
                .builder(identity)
                .putProperty(HttpSigner.SIGNING_CLOCK, signingClock())
                .request(request)
                .payload(request.contentStreamProvider().orElse(null));
            authSchemeOption.forEachSignerProperty(signRequestBuilder::putProperty);

            SignedRequest signedRequest = signer.sign(signRequestBuilder.build());
            return CompletableFuture.completedFuture(toSdkHttpFullRequest(signedRequest));
        }

        AsyncSignRequest.Builder<T> signRequestBuilder = AsyncSignRequest
            .builder(identity)
            .putProperty(HttpSigner.SIGNING_CLOCK, signingClock())
            .request(request)
            .payload(context.requestProvider());
        authSchemeOption.forEachSignerProperty(signRequestBuilder::putProperty);

        CompletableFuture<AsyncSignedRequest> signedRequestFuture = signer.signAsync(signRequestBuilder.build());
        return signedRequestFuture.thenCompose(signedRequest -> {
            SdkHttpFullRequest result = toSdkHttpFullRequest(signedRequest);
            updateAsyncRequestBodyInContexts(context, signedRequest);
            return CompletableFuture.completedFuture(result);
        });
    }

    private static void updateAsyncRequestBodyInContexts(RequestExecutionContext context, AsyncSignedRequest signedRequest) {
        AsyncRequestBody newAsyncRequestBody;
        Optional<Publisher<ByteBuffer>> optionalPayload = signedRequest.payload();
        if (optionalPayload.isPresent()) {
            Publisher<ByteBuffer> signedPayload = optionalPayload.get();
            if (signedPayload instanceof AsyncRequestBody) {
                newAsyncRequestBody = (AsyncRequestBody) signedPayload;
            } else {
                newAsyncRequestBody = AsyncRequestBody.fromPublisher(signedPayload);
            }
        } else {
            newAsyncRequestBody = null;
        }

        context.requestProvider(newAsyncRequestBody);

        ExecutionContext executionContext = context.executionContext();
        executionContext.interceptorContext(executionContext.interceptorContext()
                                                            .copy(b -> b.asyncRequestBody(newAsyncRequestBody)));
    }

    private SdkHttpFullRequest toSdkHttpFullRequest(SignedRequest signedRequest) {
        SdkHttpRequest request = signedRequest.request();
        if (request instanceof SdkHttpFullRequest) {
            return (SdkHttpFullRequest) request;
        }
        return toSdkHttpFullRequestBuilder(signedRequest).contentStreamProvider(signedRequest.payload().orElse(null)).build();
    }

    private SdkHttpFullRequest toSdkHttpFullRequest(AsyncSignedRequest signedRequest) {
        SdkHttpRequest request = signedRequest.request();
        if (request instanceof SdkHttpFullRequest) {
            return (SdkHttpFullRequest) request;
        }
        return toSdkHttpFullRequestBuilder(signedRequest).build();
    }

    private SdkHttpFullRequest.Builder toSdkHttpFullRequestBuilder(BaseSignedRequest<?> baseSignedRequest) {
        SdkHttpRequest request = baseSignedRequest.request();
        return SdkHttpFullRequest.builder()
                                 .protocol(request.protocol())
                                 .method(request.method())
                                 .host(request.host())
                                 .port(request.port())
                                 .encodedPath(request.encodedPath())
                                 .applyMutation(r -> request.forEachHeader(r::putHeader))
                                 .applyMutation(r -> request.forEachRawQueryParameter(r::putRawQueryParameter));
    }

    /**
     * Sign the request if the signer is provided and credentials are present.
     */
    private CompletableFuture<SdkHttpFullRequest> signRequest(SdkHttpFullRequest request,
                                                              RequestExecutionContext context) {
        Signer signer = context.signer();
        MetricCollector metricCollector = context.attemptMetricCollector();

        adjustForClockSkew(context.executionAttributes());

        AsyncSigner asyncSigner = asAsyncSigner(signer, context);

        long signingStart = System.nanoTime();
        CompletableFuture<SdkHttpFullRequest> signedRequestFuture =  asyncSigner.sign(request, context.requestProvider(),
                context.executionAttributes());
        signedRequestFuture.whenComplete((r, t) ->
                    metricCollector.reportMetric(CoreMetric.SIGNING_DURATION,
                            Duration.ofNanos(System.nanoTime() - signingStart)));

        return signedRequestFuture.thenApply(r -> {
            updateHttpRequestInInterceptorContext(r, context.executionContext());
            return r;
        });
    }

    /**
     * TODO: Remove when we stop having two copies of the request.
     */
    private void updateHttpRequestInInterceptorContext(SdkHttpFullRequest request, ExecutionContext executionContext) {
        executionContext.interceptorContext(executionContext.interceptorContext().copy(b -> b.httpRequest(request)));
    }

    /**
     * Returns the {@link Clock} used for signing that already accounts for clock skew when detected by the retryable stage.
     */
    private Clock signingClock() {
        int offsetInSeconds = dependencies.timeOffset();
        return Clock.offset(Clock.systemUTC(), Duration.ofSeconds(-offsetInSeconds));
    }

    /**
     * Always use the client level timeOffset.
     */
    private void adjustForClockSkew(ExecutionAttributes attributes) {
        attributes.putAttribute(SdkExecutionAttribute.TIME_OFFSET, dependencies.timeOffset());
    }

    /**
     * Simple method to adapt a sync Signer to an AsyncSigner if necessary to make the signing code path a little cleaner
     */
    private AsyncSigner asAsyncSigner(Signer signer, RequestExecutionContext context) {
        if (signer instanceof AsyncSigner) {
            return (AsyncSigner) signer;
        }

        return (request, requestBody, executionAttributes) -> {
            SdkHttpFullRequest signedRequest = signer.sign(request, executionAttributes);

            if (signer instanceof AsyncRequestBodySigner) {
                //Transform request body provider with signing operator
                AsyncRequestBody transformedRequestProvider =
                        ((AsyncRequestBodySigner) signer)
                                .signAsyncRequestBody(signedRequest, context.requestProvider(),
                                        context.executionAttributes());
                context.requestProvider(transformedRequestProvider);
            }

            return CompletableFuture.completedFuture(signedRequest);
        };
    }
}

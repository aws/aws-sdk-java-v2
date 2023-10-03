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

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToRequestPipeline;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.AsyncRequestBodySigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * Sign the marshalled request (if applicable).
 */
// TODO how does signing work with a request provider
@SdkInternalApi
public class SigningStage implements RequestToRequestPipeline {

    private static final Logger log = Logger.loggerFor(SigningStage.class);

    private final HttpClientDependencies dependencies;

    public SigningStage(HttpClientDependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    @Override
    public SdkHttpFullRequest execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        InterruptMonitor.checkInterrupted();

        SelectedAuthScheme<?> selectedAuthScheme =
            context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (shouldDoSraSigning(context, selectedAuthScheme)) {
            log.debug(() -> String.format("Using SelectedAuthScheme: %s", selectedAuthScheme.authSchemeOption().schemeId()));
            return sraSignRequest(request, context, selectedAuthScheme);
        }
        return signRequest(request, context);
    }

    private <T extends Identity> SdkHttpFullRequest sraSignRequest(SdkHttpFullRequest request,
                                                                   RequestExecutionContext context,
                                                                   SelectedAuthScheme<T> selectedAuthScheme) {
        updateHttpRequestInInterceptorContext(request, context.executionContext());
        adjustForClockSkew(context.executionAttributes());
        CompletableFuture<? extends T> identityFuture = selectedAuthScheme.identity();
        T identity = CompletableFutureUtils.joinLikeSync(identityFuture);

        Pair<SdkHttpFullRequest, Duration> measuredSign = MetricUtils.measureDuration(
            () -> doSraSign(request, selectedAuthScheme, identity));
        context.attemptMetricCollector().reportMetric(CoreMetric.SIGNING_DURATION, measuredSign.right());

        SdkHttpFullRequest signedRequest = measuredSign.left();
        updateHttpRequestInInterceptorContext(signedRequest, context.executionContext());
        return signedRequest;
    }

    private <T extends Identity> SdkHttpFullRequest doSraSign(SdkHttpFullRequest request,
                                                              SelectedAuthScheme<T> selectedAuthScheme,
                                                              T identity) {
        SignRequest.Builder<T> signRequestBuilder = SignRequest
            .builder(identity)
            .putProperty(HttpSigner.SIGNING_CLOCK, signingClock())
            .request(request)
            .payload(request.contentStreamProvider().orElse(null));
        AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();
        authSchemeOption.forEachSignerProperty(signRequestBuilder::putProperty);

        HttpSigner<T> signer = selectedAuthScheme.signer();
        SignedRequest signedRequest = signer.sign(signRequestBuilder.build());
        return toSdkHttpFullRequest(signedRequest);
    }

    private SdkHttpFullRequest toSdkHttpFullRequest(SignedRequest signedRequest) {
        SdkHttpRequest request = signedRequest.request();

        return SdkHttpFullRequest.builder()
                                 .contentStreamProvider(signedRequest.payload().orElse(null))
                                 .protocol(request.protocol())
                                 .method(request.method())
                                 .host(request.host())
                                 .port(request.port())
                                 .encodedPath(request.encodedPath())
                                 .applyMutation(r -> request.forEachHeader(r::putHeader))
                                 .applyMutation(r -> request.forEachRawQueryParameter(r::putRawQueryParameter))
                                 .build();
    }

    /**
     * Sign the request if the signer if provided and credentials are present.
     */
    private SdkHttpFullRequest signRequest(SdkHttpFullRequest request, RequestExecutionContext context) {
        updateHttpRequestInInterceptorContext(request, context.executionContext());

        Signer signer = context.signer();
        MetricCollector metricCollector = context.attemptMetricCollector();

        if (!shouldSign(context.executionAttributes(), signer)) {
            return request;
        }

        adjustForClockSkew(context.executionAttributes());

        Pair<SdkHttpFullRequest, Duration> measuredSign = MetricUtils.measureDuration(
            () -> signer.sign(request, context.executionAttributes()));

        metricCollector.reportMetric(CoreMetric.SIGNING_DURATION, measuredSign.right());

        SdkHttpFullRequest signedRequest = measuredSign.left();

        // TODO: This case does not apply to SigningStage as event stream operations are not supported by SyncClients that
        //  use this SigningStage. So this is dead code and can be removed.
        if (signer instanceof AsyncRequestBodySigner) {
            //Transform request body provider with signing operator
            AsyncRequestBody transformedRequestProvider =
                ((AsyncRequestBodySigner) signer)
                    .signAsyncRequestBody(signedRequest, context.requestProvider(), context.executionAttributes());
            context.requestProvider(transformedRequestProvider);
        }
        updateHttpRequestInInterceptorContext(signedRequest, context.executionContext());
        return signedRequest;

    }


    /**
     * TODO: Remove when we stop having two copies of the request.
     */
    private void updateHttpRequestInInterceptorContext(SdkHttpFullRequest request, ExecutionContext executionContext) {
        executionContext.interceptorContext(executionContext.interceptorContext().copy(b -> b.httpRequest(request)));
    }

    /**
     * We sign if it isn't auth=none. This attribute is no longer set in the SRA, so this exists only for old clients. In
     * addition to this, old clients only set this to false, never true. So, we have to treat null as true.
     */
    private boolean shouldSign(ExecutionAttributes attributes, Signer signer) {
        return signer != null &&
               !Boolean.FALSE.equals(attributes.getAttribute(SdkInternalExecutionAttribute.IS_NONE_AUTH_TYPE_REQUEST));
    }

    /**
     * Returns true if we should use SRA signing logic.
     */
    private boolean shouldDoSraSigning(RequestExecutionContext context, SelectedAuthScheme<?> selectedAuthScheme) {
        return context.signer() == null && selectedAuthScheme != null;
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
}

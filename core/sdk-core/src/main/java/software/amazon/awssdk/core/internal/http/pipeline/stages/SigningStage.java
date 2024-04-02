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
        // else, this implies pre SRA client, with authType=None, so don't need to do anything
        return request;
    }

    private <T extends Identity> SdkHttpFullRequest sraSignRequest(SdkHttpFullRequest request,
                                                                   RequestExecutionContext context,
                                                                   SelectedAuthScheme<T> selectedAuthScheme) {
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

        // Optimization: don't do any conversion if we can avoid it.
        if (request instanceof SdkHttpFullRequest) {
            SdkHttpFullRequest fullRequest = (SdkHttpFullRequest) request;
            if (signedRequest.payload().orElse(null) == fullRequest.contentStreamProvider().orElse(null)) {
                return fullRequest;
            }

            return fullRequest.toBuilder()
                              .contentStreamProvider(signedRequest.payload().orElse(null))
                              .build();
        }

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
        Signer signer = context.signer();
        MetricCollector metricCollector = context.attemptMetricCollector();

        adjustForClockSkew(context.executionAttributes());

        Pair<SdkHttpFullRequest, Duration> measuredSign = MetricUtils.measureDuration(
            () -> signer.sign(request, context.executionAttributes()));

        metricCollector.reportMetric(CoreMetric.SIGNING_DURATION, measuredSign.right());

        SdkHttpFullRequest signedRequest = measuredSign.left();

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

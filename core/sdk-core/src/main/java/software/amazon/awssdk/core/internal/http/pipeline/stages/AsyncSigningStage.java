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
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.AsyncRequestBodySigner;
import software.amazon.awssdk.core.signer.AsyncSigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SignedRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class AsyncSigningStage implements RequestPipeline<SdkHttpFullRequest,
        CompletableFuture<SdkHttpFullRequest>> {

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
        // TODO: Add unit tests for SRA signing logic.
        if (context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME) != null) {
            return sraSignRequest(request, context);
        }
        return signRequest(request, context);
    }

    private <T extends Identity> CompletableFuture<SdkHttpFullRequest> sraSignRequest(SdkHttpFullRequest request,
                                                                                      RequestExecutionContext context) {
        updateInterceptorContext(request, context.executionContext());

        ExecutionAttributes executionAttributes = context.executionAttributes();
        SelectedAuthScheme<T> selectedAuthScheme =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        if (!shouldSign(selectedAuthScheme)) {
            return CompletableFuture.completedFuture(request);
        }

        Pair<SdkHttpFullRequest, Duration> measuredSign = MetricUtils.measureDuration(
            () -> {
                AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();

                // TODO: Identity resolution should move to before Endpoint resolution interceptor, to support accountId based
                //  endpoints and also to logically separate out identity resolution as its own step.
                ResolveIdentityRequest.Builder identityRequestBuilder = ResolveIdentityRequest.builder();
                authSchemeOption.forEachIdentityProperty(identityRequestBuilder::putProperty);

                IdentityProvider<T> identityProvider = selectedAuthScheme.identityProvider();
                T identity = identityProvider.resolveIdentity(identityRequestBuilder.build()).join();

                HttpSigner<T> signer = selectedAuthScheme.signer();

                SdkHttpFullRequest result;
                if (context.requestProvider() == null) {
                    SyncSignRequest.Builder<T> signRequestBuilder = SyncSignRequest
                        .builder(identity)
                        .request(request)
                        .payload(request.contentStreamProvider().orElse(null));
                    authSchemeOption.forEachSignerProperty(signRequestBuilder::putProperty);

                    SyncSignedRequest signedRequest = signer.sign(signRequestBuilder.build());
                    result = toSdkHttpFullRequest(signedRequest);
                } else {
                    AsyncSignRequest.Builder<T> signRequestBuilder = AsyncSignRequest
                        .builder(identity)
                        .request(request)
                        .payload(context.requestProvider());
                    authSchemeOption.forEachSignerProperty(signRequestBuilder::putProperty);

                    AsyncSignedRequest signedRequest = signer.signAsync(signRequestBuilder.build());
                    result = toSdkHttpFullRequest(signedRequest);

                    if (signedRequest.payload().isPresent()) {
                        context.requestProvider(AsyncRequestBody.fromPublisher(signedRequest.payload().get()));
                    } else {
                        // TODO: to confirm. Seems like it is possible that a request with async payload, can go through
                        //  signAsync and result is no async payload, so better to unset the input async payload than use it
                        //  as-is if the signer returned null payload?
                        context.requestProvider(null);
                    }
                }

                // TODO: the below updates the result in interceptor context, but if signAsync is called the asyncRequestBody
                //  could also be different. Should it also be updated in InterceptorContext?
                //  executionContext.interceptorContext(executionContext.interceptorContext().copy(b -> b.asyncRequestBody(...))).
                //  This is NOT done in current non-SRA code.
                updateInterceptorContext(result, context.executionContext());
                return result;
            });
        context.attemptMetricCollector().reportMetric(CoreMetric.SIGNING_DURATION, measuredSign.right());
        return CompletableFuture.completedFuture(measuredSign.left());
    }

    private SdkHttpFullRequest toSdkHttpFullRequest(SyncSignedRequest signedRequest) {
        return toSdkHttpFullRequestBuilder(signedRequest).contentStreamProvider(signedRequest.payload().orElse(null)).build();
    }

    private SdkHttpFullRequest toSdkHttpFullRequest(AsyncSignedRequest signedRequest) {
        return toSdkHttpFullRequestBuilder(signedRequest).build();
    }

    private SdkHttpFullRequest.Builder toSdkHttpFullRequestBuilder(SignedRequest<?> signedRequest) {
        SdkHttpRequest request = signedRequest.request();
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
        updateInterceptorContext(request, context.executionContext());

        Signer signer = context.signer();
        MetricCollector metricCollector = context.attemptMetricCollector();

        if (!shouldSign(signer)) {
            return CompletableFuture.completedFuture(request);
        }

        adjustForClockSkew(context.executionAttributes());

        AsyncSigner asyncSigner = asAsyncSigner(signer, context);

        long signingStart = System.nanoTime();
        CompletableFuture<SdkHttpFullRequest> signedRequestFuture =  asyncSigner.sign(request, context.requestProvider(),
                context.executionAttributes());
        signedRequestFuture.whenComplete((r, t) ->
                    metricCollector.reportMetric(CoreMetric.SIGNING_DURATION,
                            Duration.ofNanos(System.nanoTime() - signingStart)));

        return signedRequestFuture.thenApply(r -> {
            updateInterceptorContext(r, context.executionContext());
            return r;
        });
    }

    /**
     * TODO: Remove when we stop having two copies of the request.
     */
    private void updateInterceptorContext(SdkHttpFullRequest request, ExecutionContext executionContext) {
        executionContext.interceptorContext(executionContext.interceptorContext().copy(b -> b.httpRequest(request)));
    }

    /**
     * We do not sign if the Auth SchemeId is smithy.api#noAuth.
     *
     * @return True if request should be signed, false if not.
     */
    private boolean shouldSign(SelectedAuthScheme<?> selectedAuthScheme) {
        // TODO: Should this string be a constant somewhere. Similar logic is used in AuthSchemeInterceptors.
        return !"smithy.api#noAuth".equals(selectedAuthScheme.authSchemeOption().schemeId());
    }

    /**
     * We sign if a signer is provided is not null.
     *
     * @return True if request should be signed, false if not.
     */
    private boolean shouldSign(Signer signer) {
        return signer != null;
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

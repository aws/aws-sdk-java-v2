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
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Pair;

/**
 * Sign the marshalled request (if applicable).
 */
// TODO how does signing work with a request provider
@SdkInternalApi
public class SigningStage implements RequestToRequestPipeline {

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
        if (context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME) != null) {
            return sraSignRequest(request, context);
        }
        return signRequest(request, context);
    }

    private <T extends Identity> SdkHttpFullRequest sraSignRequest(SdkHttpFullRequest request, RequestExecutionContext context) {

        ExecutionAttributes executionAttributes = context.executionAttributes();
        SelectedAuthScheme<T> selectedAuthScheme =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();

        // TODO: Identity resolution should move to before Endpoint resolution interceptor, to support accountId based endpoints
        //  and also to logically separate out identity resolution as its own step.
        ResolveIdentityRequest.Builder identityRequestBuilder = ResolveIdentityRequest.builder();
        authSchemeOption.forEachIdentityProperty(identityRequestBuilder::putProperty);

        IdentityProvider<T> identityProvider = selectedAuthScheme.identityProvider();
        T identity = identityProvider.resolveIdentity(identityRequestBuilder.build()).join();

        SyncSignRequest.Builder<T> signRequestBuilder = SyncSignRequest
            .builder(identity)
            .request(request)
            .payload(request.contentStreamProvider().orElse(null));
        authSchemeOption.forEachSignerProperty(signRequestBuilder::putProperty);

        HttpSigner<T> signer = selectedAuthScheme.signer();
        SyncSignedRequest signedRequest = signer.sign(signRequestBuilder.build());
        return toSdkHttpFullRequest(signedRequest);
    }

    private SdkHttpFullRequest toSdkHttpFullRequest(SyncSignedRequest signedRequest) {
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
    private SdkHttpFullRequest signRequest(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        updateInterceptorContext(request, context.executionContext());

        Signer signer = context.signer();
        MetricCollector metricCollector = context.attemptMetricCollector();

        if (shouldSign(signer)) {
            adjustForClockSkew(context.executionAttributes());

            Pair<SdkHttpFullRequest, Duration> measuredSign = MetricUtils.measureDuration(() ->
                    signer.sign(request, context.executionAttributes()));

            metricCollector.reportMetric(CoreMetric.SIGNING_DURATION, measuredSign.right());

            SdkHttpFullRequest signedRequest = measuredSign.left();

            if (signer instanceof AsyncRequestBodySigner) {
                //Transform request body provider with signing operator
                AsyncRequestBody transformedRequestProvider =
                    ((AsyncRequestBodySigner) signer)
                        .signAsyncRequestBody(signedRequest, context.requestProvider(), context.executionAttributes());
                context.requestProvider(transformedRequestProvider);
            }
            updateInterceptorContext(signedRequest, context.executionContext());
            return signedRequest;
        }

        return request;
    }


    /**
     * TODO: Remove when we stop having two copies of the request.
     */
    private void updateInterceptorContext(SdkHttpFullRequest request, ExecutionContext executionContext) {
        executionContext.interceptorContext(executionContext.interceptorContext().copy(b -> b.httpRequest(request)));
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
}

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
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.AsyncRequestBodySigner;
import software.amazon.awssdk.core.signer.AsyncSigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;

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
    public CompletableFuture<SdkHttpFullRequest> execute(SdkHttpFullRequest request, RequestExecutionContext context)
            throws Exception {
        InterruptMonitor.checkInterrupted();
        return signRequest(request, context);
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

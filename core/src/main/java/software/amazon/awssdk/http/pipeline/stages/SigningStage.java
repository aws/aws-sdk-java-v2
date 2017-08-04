/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.pipeline.stages;

import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.CanHandleNullCredentials;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.handlers.AwsExecutionAttributes;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.RequestToRequestPipeline;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.runtime.auth.SignerProviderContext;

/**
 * Sign the marshalled request (if applicable).
 */
// TODO how does signing work with a request provider
public class SigningStage implements RequestToRequestPipeline {

    private final HttpClientDependencies dependencies;

    public SigningStage(HttpClientDependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    public SdkHttpFullRequest execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        AmazonHttpClient.checkInterrupted();
        return signRequest(request, context);
    }

    /**
     * Sign the request if the signer if provided and credentials are present.
     */
    private SdkHttpFullRequest signRequest(SdkHttpFullRequest request, RequestExecutionContext context) {
        final AwsCredentials credentials = context.credentialsProvider().getCredentials();
        updateInterceptorContext(request, context.executionContext());
        Signer signer = newSigner(request, context);
        if (shouldSign(signer, credentials)) {
            context.awsRequestMetrics().startEvent(AwsRequestMetrics.Field.RequestSigningTime);
            try {
                adjustForClockSkew(context.executionAttributes());
                return signer.sign(context.executionContext().interceptorContext(), context.executionAttributes());
            } finally {
                context.awsRequestMetrics().endEvent(AwsRequestMetrics.Field.RequestSigningTime);
            }
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
     * Obtain a signer from the {@link software.amazon.awssdk.runtime.auth.SignerProvider}.
     */
    private Signer newSigner(final SdkHttpFullRequest request, RequestExecutionContext context) {
        final SignerProviderContext.Builder signerProviderContext = SignerProviderContext
                .builder()
                .withRequest(request)
                .withRequestConfig(context.requestConfig());
        return context.signerProvider().getSigner(signerProviderContext.withUri(request.getEndpoint()).build());
    }

    /**
     * We sign if a signer is provided and the credentials are non-null (unless the signer implements the marker interface,
     * {@link
     * CanHandleNullCredentials}).
     *
     * @return True if request should be signed, false if not.
     */
    private boolean shouldSign(Signer signer, AwsCredentials credentials) {
        return signer != null && (credentials != null || signer instanceof CanHandleNullCredentials);
    }

    /**
     * Always use the client level timeOffset.
     */
    private void adjustForClockSkew(ExecutionAttributes attributes) {
        // TODO: Should we allow customers to specify the initial time offset?
        attributes.putAttribute(AwsExecutionAttributes.TIME_OFFSET, dependencies.timeOffset());
    }

}

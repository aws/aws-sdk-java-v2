/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.auth.presign;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.AwsRequestOverrideConfig;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfig;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.auth.Presigner;
import software.amazon.awssdk.core.interceptor.AwsExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.runtime.auth.SignerProvider;
import software.amazon.awssdk.core.runtime.auth.SignerProviderContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Really thin facade over {@link Presigner} to deal with some common concerns like credential resolution, adding custom headers
 * and query params to be included in signing, and conversion to a usable URL.
 */
@Immutable
@SdkProtectedApi
public final class PresignerFacade {

    private final AwsCredentialsProvider credentialsProvider;
    private final SignerProvider signerProvider;

    private PresignerFacade(PresignerParams presignerParams) {
        this.credentialsProvider = presignerParams.credentialsProvider();
        this.signerProvider = presignerParams.signerProvider();
    }

    public static PresignerFacade create(PresignerParams presignerParams) {
        return new PresignerFacade(presignerParams);
    }

    @ReviewBeforeRelease("Can this be cleaned up with the signer refactor?")
    public URL presign(SdkRequest request, SdkHttpFullRequest httpRequest, AwsRequestOverrideConfig requestConfig,
                       Date expirationDate) {
        final Presigner presigner = (Presigner) signerProvider.getSigner(SignerProviderContext.builder()
                                                                                              .withIsRedirect(false)
                                                                                              .withRequest(httpRequest)
                                                                                              .build());
        SdkHttpFullRequest.Builder mutableHttpRequest = httpRequest.toBuilder();
        addCustomQueryParams(mutableHttpRequest, requestConfig);
        addCustomHeaders(mutableHttpRequest, requestConfig);
        AwsCredentialsProvider credentialsProvider = resolveCredentials(requestConfig);

        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS, credentialsProvider.getCredentials());

        SdkHttpFullRequest signed = presigner.presign(InterceptorContext.builder()
                                                                        .request(request)
                                                                        .httpRequest(mutableHttpRequest.build())
                                                                        .build(),
                                                      executionAttributes,
                                                      expirationDate);
        return invokeSafely(() -> signed.getUri().toURL());
    }

    private void addCustomQueryParams(SdkHttpFullRequest.Builder request, SdkRequestOverrideConfig requestConfig) {
        Optional.ofNullable(requestConfig).flatMap(SdkRequestOverrideConfig::rawQueryParameters)
                .ifPresent(queryParameters -> {
                    for (Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
                        request.rawQueryParameter(param.getKey(), param.getValue());
                    }
                });
    }

    private void addCustomHeaders(SdkHttpFullRequest.Builder request, SdkRequestOverrideConfig requestConfig) {
        Optional.ofNullable(requestConfig).flatMap(SdkRequestOverrideConfig::headers)
                .ifPresent(headers -> {
                    for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                        request.header(header.getKey(), header.getValue());
                    }
                });
    }

    private AwsCredentialsProvider resolveCredentials(AwsRequestOverrideConfig requestConfig) {
        return Optional.of(requestConfig).flatMap(AwsRequestOverrideConfig::credentialsProvider)
                .orElse(credentialsProvider);
    }

}

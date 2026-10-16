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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Resolves the identity used to sign a request, both when the auth scheme is first selected and again before each retry
 * attempt.
 */
@SdkInternalApi
public final class IdentityResolutionHelper {

    private IdentityResolutionHelper() {
    }

    /**
     * Resolves the identity again for a retry attempt and replaces the
     * {@link SdkInternalExecutionAttribute#SELECTED_AUTH_SCHEME} with a copy holding the newly resolved identity.
     * The identity is resolved from the {@link SelectedAuthScheme#identityProvider()} using the identity properties of the
     * selected {@link AuthSchemeOption}.
     *
     * @param executionAttributes The execution attributes of the request being retried.
     */
    public static void reResolveIdentityForRetry(ExecutionAttributes executionAttributes) {
        SelectedAuthScheme<?> selectedAuthScheme =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (selectedAuthScheme == null || selectedAuthScheme.identityProvider() == null) {
            return;
        }

        MetricCollector metricCollector = executionAttributes.getAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                         withReResolvedIdentity(selectedAuthScheme, metricCollector));
    }

    private static <T extends Identity> SelectedAuthScheme<T> withReResolvedIdentity(SelectedAuthScheme<T> selectedAuthScheme,
                                                                                     MetricCollector metricCollector) {
        CompletableFuture<? extends T> identity;
        try {
            identity = resolveIdentity(selectedAuthScheme.identityProvider(),
                                       resolveIdentityRequest(selectedAuthScheme.authSchemeOption()),
                                       metricCollector);
        } catch (RuntimeException e) {
            identity = CompletableFutureUtils.failedFuture(e);
        }
        return selectedAuthScheme.toBuilder()
                                 .identity(identity)
                                 .build();
    }

    /**
     * Returns the request used to resolve the identity for the given auth scheme option, carrying the option's identity
     * properties.
     */
    public static ResolveIdentityRequest resolveIdentityRequest(AuthSchemeOption authSchemeOption) {
        ResolveIdentityRequest.Builder identityRequestBuilder = ResolveIdentityRequest.builder();
        authSchemeOption.forEachIdentityProperty(identityRequestBuilder::putProperty);
        return identityRequestBuilder.build();
    }

    /**
     * Resolves an identity from the given provider, reporting the fetch duration to the metric collector when the identity
     * type has a corresponding fetch-duration metric and a metric collector is provided.
     */
    public static <T extends Identity> CompletableFuture<? extends T> resolveIdentity(IdentityProvider<T> identityProvider,
                                                                                      ResolveIdentityRequest request,
                                                                                      MetricCollector metricCollector) {
        SdkMetric<Duration> metric = identityFetchDurationMetric(identityProvider);
        if (metric == null || metricCollector == null) {
            return identityProvider.resolveIdentity(request);
        }
        return MetricUtils.reportDuration(() -> identityProvider.resolveIdentity(request), metricCollector, metric);
    }

    private static SdkMetric<Duration> identityFetchDurationMetric(IdentityProvider<?> identityProvider) {
        Class<?> identityType = identityProvider.identityType();
        if (identityType == AwsCredentialsIdentity.class) {
            return CoreMetric.CREDENTIALS_FETCH_DURATION;
        }
        if (identityType == TokenIdentity.class) {
            return CoreMetric.TOKEN_FETCH_DURATION;
        }
        return null;
    }
}

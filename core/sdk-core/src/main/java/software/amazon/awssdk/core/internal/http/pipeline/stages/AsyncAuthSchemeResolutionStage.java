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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToRequestPipeline;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.spi.identity.IdentityProviderUpdater;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.Logger;

/**
 * Async pipeline stage that resolves the auth scheme and identity for signing.
 * <p>
 * This stage runs after all interceptors have completed, ensuring that any credentials
 * injected via ExecutionInterceptor.modifyRequest() are respected.
 */
@SdkInternalApi
public final class AsyncAuthSchemeResolutionStage implements RequestToRequestPipeline {

    private static final Logger LOG = Logger.loggerFor(AsyncAuthSchemeResolutionStage.class);

    public AsyncAuthSchemeResolutionStage(HttpClientDependencies dependencies) {
    }

    @Override
    public SdkHttpFullRequest execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        ExecutionAttributes executionAttributes = context.executionAttributes();

        // Skip if no auth schemes configured (pre-SRA client or authType=None)
        Map<String, AuthScheme<?>> authSchemes = executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES);
        if (authSchemes == null) {
            return request;
        }

        // Skip if already resolved
        // Note: Check for "unset" scheme ID which is a placeholder created by derived attributes
        // (e.g., SIGNING_REGION) when SELECTED_AUTH_SCHEME is null. See AwsSignerExecutionAttribute.
        // TODO: Consider skipping SIGNING_REGION setup in AwsExecutionContextBuilder when using the new flow,
        //       or setting it after auth resolution, to avoid this placeholder check.
        SelectedAuthScheme<?> existingAuthScheme =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (existingAuthScheme != null && !"unset".equals(existingAuthScheme.authSchemeOption().schemeId())) {
            return request;
        }

        // Get auth options
        List<AuthSchemeOption> authOptions =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS);

        if (authOptions == null || authOptions.isEmpty()) {
            return request;
        }

        // Get base identity providers
        IdentityProviders identityProviders =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);

        // Apply request-level overrides via callback
        IdentityProviderUpdater updater =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDER_UPDATER);
        if (updater != null) {
            identityProviders = updater.update(
                context.executionContext().interceptorContext().request(),
                identityProviders);
        }

        // Select auth scheme and resolve identity
        SelectedAuthScheme<? extends Identity> selectedAuthScheme =
            selectAuthScheme(authOptions, authSchemes, identityProviders, executionAttributes);

        // Store the selected auth scheme
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);

        return request;
    }

    private SelectedAuthScheme<? extends Identity> selectAuthScheme(
            List<AuthSchemeOption> authOptions,
            Map<String, AuthScheme<?>> authSchemes,
            IdentityProviders identityProviders,
            ExecutionAttributes executionAttributes) {

        MetricCollector metricCollector =
            executionAttributes.getAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);
        List<Supplier<String>> discardedReasons = new ArrayList<>();

        for (AuthSchemeOption authOption : authOptions) {
            AuthScheme<?> authScheme = authSchemes.get(authOption.schemeId());
            SelectedAuthScheme<? extends Identity> selectedAuthScheme = trySelectAuthScheme(
                authOption, authScheme, identityProviders, discardedReasons, metricCollector);

            if (selectedAuthScheme != null) {
                if (!discardedReasons.isEmpty()) {
                    LOG.debug(() -> String.format("%s auth will be used, discarded: '%s'",
                        authOption.schemeId(),
                        discardedReasons.stream().map(Supplier::get).collect(Collectors.joining(", "))));
                }
                return selectedAuthScheme;
            }
        }

        throw SdkException.builder()
            .message("Failed to determine how to authenticate the user: " +
                     discardedReasons.stream().map(Supplier::get).collect(Collectors.joining(", ")))
            .build();
    }

    private <T extends Identity> SelectedAuthScheme<T> trySelectAuthScheme(
            AuthSchemeOption authOption,
            AuthScheme<T> authScheme,
            IdentityProviders identityProviders,
            List<Supplier<String>> discardedReasons,
            MetricCollector metricCollector) {

        if (authScheme == null) {
            discardedReasons.add(() -> String.format("'%s' is not enabled for this request.", authOption.schemeId()));
            return null;
        }

        IdentityProvider<T> identityProvider = authScheme.identityProvider(identityProviders);
        if (identityProvider == null) {
            discardedReasons.add(() -> String.format("'%s' does not have an identity provider configured.",
                authOption.schemeId()));
            return null;
        }

        HttpSigner<T> signer;
        try {
            signer = authScheme.signer();
        } catch (RuntimeException e) {
            discardedReasons.add(() -> String.format("'%s' signer could not be retrieved: %s",
                authOption.schemeId(), e.getMessage()));
            return null;
        }

        ResolveIdentityRequest.Builder identityRequestBuilder = ResolveIdentityRequest.builder();
        authOption.forEachIdentityProperty(identityRequestBuilder::putProperty);

        CompletableFuture<? extends T> identity;
        SdkMetric<Duration> metric = getIdentityMetric(identityProvider);
        if (metric == null) {
            identity = identityProvider.resolveIdentity(identityRequestBuilder.build());
        } else {
            identity = MetricUtils.reportDuration(
                () -> identityProvider.resolveIdentity(identityRequestBuilder.build()),
                metricCollector,
                metric);
        }

        return new SelectedAuthScheme<>(identity, signer, authOption);
    }

    private SdkMetric<Duration> getIdentityMetric(IdentityProvider<?> identityProvider) {
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

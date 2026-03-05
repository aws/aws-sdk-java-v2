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

package software.amazon.awssdk.core.internal.http.auth;

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
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
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
 * Shared utility for selecting auth schemes from a list of options.
 */
@SdkInternalApi
public final class AuthSchemeResolver {

    private static final Logger LOG = Logger.loggerFor(AuthSchemeResolver.class);

    private AuthSchemeResolver() {
    }

    /**
     * Select an auth scheme from the given options.
     *
     * @param authOptions List of auth scheme options to try in order
     * @param authSchemes Map of available auth schemes
     * @param identityProviders Identity providers to use for resolving identity
     * @param metricCollector Optional metric collector for recording identity fetch duration
     * @return The selected auth scheme
     * @throws SdkException if no auth scheme could be selected
     */
    public static SelectedAuthScheme<? extends Identity> selectAuthScheme(
            List<AuthSchemeOption> authOptions,
            Map<String, AuthScheme<?>> authSchemes,
            IdentityProviders identityProviders,
            MetricCollector metricCollector) {

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

    /**
     * Merge properties from any pre-existing auth scheme into the selected one.
     */
    public static <T extends Identity> SelectedAuthScheme<T> mergePreExistingAuthSchemeProperties(
            SelectedAuthScheme<T> selectedAuthScheme,
            ExecutionAttributes executionAttributes) {

        SelectedAuthScheme<?> existingAuthScheme =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        if (existingAuthScheme == null) {
            return selectedAuthScheme;
        }

        AuthSchemeOption.Builder mergedOption = selectedAuthScheme.authSchemeOption().toBuilder();
        existingAuthScheme.authSchemeOption().forEachIdentityProperty(mergedOption::putIdentityPropertyIfAbsent);
        existingAuthScheme.authSchemeOption().forEachSignerProperty(mergedOption::putSignerPropertyIfAbsent);

        return new SelectedAuthScheme<>(
            selectedAuthScheme.identity(),
            selectedAuthScheme.signer(),
            mergedOption.build()
        );
    }

    private static <T extends Identity> SelectedAuthScheme<T> trySelectAuthScheme(
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

        CompletableFuture<? extends T> identity = resolveIdentity(
            identityProvider, identityRequestBuilder.build(), metricCollector);

        return new SelectedAuthScheme<>(identity, signer, authOption);
    }

    private static <T extends Identity> CompletableFuture<? extends T> resolveIdentity(
            IdentityProvider<T> identityProvider,
            ResolveIdentityRequest request,
            MetricCollector metricCollector) {

        SdkMetric<Duration> metric = getIdentityMetric(identityProvider);
        if (metric == null || metricCollector == null) {
            return identityProvider.resolveIdentity(request);
        }
        return MetricUtils.reportDuration(() -> identityProvider.resolveIdentity(request), metricCollector, metric);
    }

    private static SdkMetric<Duration> getIdentityMetric(IdentityProvider<?> identityProvider) {
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

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

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.auth.AuthSchemeResolver;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.core.spi.identity.IdentityProviderUpdater;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Pipeline stage that resolves the auth scheme and identity for signing.
 */
@SdkInternalApi
public final class AuthSchemeResolutionStage implements MutableRequestToRequestPipeline {

    public AuthSchemeResolutionStage(HttpClientDependencies dependencies) {
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        ExecutionAttributes executionAttributes = context.executionAttributes();

        Map<String, AuthScheme<?>> authSchemes = executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES);
        if (authSchemes == null) {
            return request;
        }

        SdkRequest sdkRequest = context.executionContext().interceptorContext().request();
        List<AuthSchemeOption> authOptions = resolveAuthSchemeOptions(executionAttributes, sdkRequest);
        if (authOptions == null || authOptions.isEmpty()) {
            return request;
        }

        IdentityProviders identityProviders =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);

        IdentityProviderUpdater updater =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDER_UPDATER);
        if (updater != null) {
            identityProviders = updater.update(sdkRequest, identityProviders);
        }

        MetricCollector metricCollector =
            executionAttributes.getAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);

        SelectedAuthScheme<? extends Identity> selectedAuthScheme =
            AuthSchemeResolver.selectAuthScheme(authOptions, authSchemes, identityProviders, metricCollector);

        selectedAuthScheme = AuthSchemeResolver.mergePreExistingAuthSchemeProperties(selectedAuthScheme, executionAttributes);

        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);

        recordBusinessMetrics(selectedAuthScheme, sdkRequest, executionAttributes);

        return request;
    }

    private List<AuthSchemeOption> resolveAuthSchemeOptions(ExecutionAttributes executionAttributes, SdkRequest request) {
        AuthSchemeOptionsResolver resolver =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER);

        if (resolver == null) {
            return null;
        }
        return resolver.resolve(request);
    }

    private void recordBusinessMetrics(SelectedAuthScheme<? extends Identity> selectedAuthScheme,
                                       SdkRequest request,
                                       ExecutionAttributes executionAttributes) {
        if (selectedAuthScheme == null) {
            return;
        }

        BusinessMetricCollection businessMetrics =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
        if (businessMetrics == null) {
            return;
        }

        String schemeId = selectedAuthScheme.authSchemeOption().schemeId();

        if (AwsV4aAuthScheme.SCHEME_ID.equals(schemeId) && !isSignerOverridden(request, executionAttributes)) {
            businessMetrics.addMetric(BusinessMetricFeatureId.SIGV4A_SIGNING.value());
        }

        if (BearerAuthScheme.SCHEME_ID.equals(schemeId) && selectedAuthScheme.identity().isDone()) {
            Identity identity = selectedAuthScheme.identity().getNow(null);
            if (identity instanceof TokenIdentity) {
                String tokenFromEnv = executionAttributes.getAttribute(SdkInternalExecutionAttribute.TOKEN_CONFIGURED_FROM_ENV);
                if (tokenFromEnv != null && tokenFromEnv.equals(((TokenIdentity) identity).token())) {
                    businessMetrics.addMetric(BusinessMetricFeatureId.BEARER_SERVICE_ENV_VARS.value());
                }
            }
        }
    }

    private boolean isSignerOverridden(SdkRequest request, ExecutionAttributes executionAttributes) {
        boolean isClientSignerOverridden =
            Boolean.TRUE.equals(executionAttributes.getAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN));
        boolean isRequestSignerOverridden = request.overrideConfiguration()
                                                   .flatMap(RequestOverrideConfiguration::signer)
                                                   .isPresent();
        return isClientSignerOverridden || isRequestSignerOverridden;
    }
}

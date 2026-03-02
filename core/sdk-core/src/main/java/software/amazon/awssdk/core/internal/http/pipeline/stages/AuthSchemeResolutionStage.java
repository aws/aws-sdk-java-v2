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
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.auth.AuthSchemeResolver;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToRequestPipeline;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.core.spi.identity.IdentityProviderUpdater;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Pipeline stage that resolves the auth scheme and identity for signing.
 */
@SdkInternalApi
public final class AuthSchemeResolutionStage implements RequestToRequestPipeline {

    public AuthSchemeResolutionStage(HttpClientDependencies dependencies) {
    }

    @Override
    public SdkHttpFullRequest execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
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
}

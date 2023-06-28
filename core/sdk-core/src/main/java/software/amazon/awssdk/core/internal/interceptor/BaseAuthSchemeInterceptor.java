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

package software.amazon.awssdk.core.internal.interceptor;

import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.AUTH_SCHEMES;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.IDENTITY_PROVIDER_CONFIGURATION;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.auth.spi.AuthScheme;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.AuthSchemeProvider;
import software.amazon.awssdk.http.auth.spi.IdentityProviderConfiguration;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.Logger;

/**
 * This is the base interceptor logic that does the Smithy Reference Architecture auth resolution logic. Subclasses need to use
 * the service specific {@link AuthSchemeProvider} to resolve the list of {@link AuthSchemeOption}s. The base auth resolution
 * logic here then selects the auth scheme for this request and is saved as ExecutionAttribute for use later.
 */
@SdkInternalApi
public abstract class BaseAuthSchemeInterceptor implements ExecutionInterceptor {
    private static final Logger log = Logger.loggerFor(BaseAuthSchemeInterceptor.class);

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        List<AuthSchemeOption> authOptions = resolveAuthOptions(executionAttributes);
        SelectedAuthScheme<?> selectedAuthScheme = selectAuthScheme(authOptions, executionAttributes);
        executionAttributes.putAttribute(SELECTED_AUTH_SCHEME, selectedAuthScheme);
    }

    /**
     * Invoke the auth scheme resolver to determine which auth options we should consider for this request.
     */
    protected abstract List<AuthSchemeOption> resolveAuthOptions(ExecutionAttributes executionAttributes);

    /**
     * From a list of possible auth options for this request, determine which auth scheme should be used.
     */
    private SelectedAuthScheme<?> selectAuthScheme(List<AuthSchemeOption> authOptions, ExecutionAttributes executionAttributes) {
        Map<String, AuthScheme<?>> authSchemes = executionAttributes.getAttribute(AUTH_SCHEMES);
        IdentityProviderConfiguration identityResolvers = executionAttributes.getAttribute(IDENTITY_PROVIDER_CONFIGURATION);

        StringBuilder failureReasons = new StringBuilder();
        // Check each option, in the order the auth scheme resolver proposed them.
        for (AuthSchemeOption authOption : authOptions) {
            // If we're using no-auth, don't consider which options are enabled.
            if ("smithy.auth#noAuth".equals(authOption.schemeId())) {
                return new SelectedAuthScheme<>(null, null, authOption);
            }

            AuthScheme<?> authScheme = authSchemes.get(authOption.schemeId());

            SelectedAuthScheme<?> selectedAuthScheme =
                trySelectAuthScheme(authOption, authScheme, identityResolvers, failureReasons);

            // Check to see if selecting this auth option succeeded.
            if (selectedAuthScheme != null) {
                if (failureReasons.length() > 0) {
                    log.debug(() -> authOption.schemeId() + " auth will be used because " + failureReasons);
                }
                return selectedAuthScheme;
            }
        }

        // TODO: Exception type and message?
        throw new IllegalStateException("Failed to determine how to authenticate the user:" + failureReasons);
    }

    /**
     * Try to select the provided auth scheme by ensuring it is non-null and that its identity resolver is configured.
     */
    <T extends Identity> SelectedAuthScheme<T> trySelectAuthScheme(AuthSchemeOption authOption,
                                                                   AuthScheme<T> authScheme,
                                                                   IdentityProviderConfiguration identityProviders,
                                                                   StringBuilder failureReasons) {
        if (authScheme == null) {
            failureReasons.append("\nAuth scheme '" + authOption.schemeId() + "' was not enabled for this request.");
            return null;
        }

        IdentityProvider<T> identityProvider = authScheme.identityProvider(identityProviders);

        if (identityProvider == null) {
            failureReasons.append("\nAuth scheme '" + authOption.schemeId() + "' did not have an identity resolver configured.");
            return null;
        }

        return new SelectedAuthScheme<>(identityProvider, authScheme.signer(), authOption);
    }
}

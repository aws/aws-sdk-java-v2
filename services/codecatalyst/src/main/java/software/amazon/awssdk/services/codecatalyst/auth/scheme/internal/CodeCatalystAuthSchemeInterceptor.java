/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst.auth.scheme.internal;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.auth.spi.AuthScheme;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.IdentityProviderConfiguration;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codecatalyst.auth.scheme.CodeCatalystAuthSchemeParams;
import software.amazon.awssdk.services.codecatalyst.auth.scheme.CodeCatalystAuthSchemeProvider;
import software.amazon.awssdk.utils.Logger;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class CodeCatalystAuthSchemeInterceptor implements ExecutionInterceptor {

    private static final Logger log = Logger.loggerFor(CodeCatalystAuthSchemeInterceptor.class);

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        List<AuthSchemeOption> authOptions = resolveAuthOptions(executionAttributes);
        SelectedAuthScheme<?> selectedAuthScheme = selectAuthScheme(authOptions, executionAttributes);
        executionAttributes.putAttribute(SdkExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);
    }

    /**
     * Invoke the auth scheme resolver to determine which auth options we should consider for this request.
     */
    private List<AuthSchemeOption> resolveAuthOptions(ExecutionAttributes executionAttributes) {
        // Prepare the inputs for the auth scheme resolver. We always include the
        // operationName, and we include the region if the service is modeled with
        // @sigv4.
        String operation = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
        // TODO: Do this only if sigv4
        Region region = executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION);

        // Use the operation-level auth scheme resolver if the customer
        // specified an override, otherwise fall back to the one on the client.
        // TODO: The fallback should happen while setting in execution attributes
        CodeCatalystAuthSchemeProvider authSchemeProvider =
            (CodeCatalystAuthSchemeProvider) executionAttributes.getAttribute(SdkExecutionAttribute.AUTH_SCHEME_RESOLVER);

        return authSchemeProvider.resolveAuthScheme(CodeCatalystAuthSchemeParams.builder()
                                                                       .operation(operation)
                                                                       // .region(region.id())
                                                                       .build());
    }

    /**
     * From a list of possible auth options for this request, determine which auth scheme should be used.
     */
    private SelectedAuthScheme<?> selectAuthScheme(List<AuthSchemeOption> authOptions, ExecutionAttributes executionAttributes) {

        // TODO: This should be "merged" earlier, with request preferred over client
        Map<String, ? extends AuthScheme<?>> authSchemes =
            executionAttributes.getAttribute(SdkExecutionAttribute.AUTH_SCHEMES);

        IdentityProviderConfiguration identityResolvers =
            executionAttributes.getAttribute(SdkExecutionAttribute.IDENTITY_PROVIDERS);

        StringBuilder failureReasons = new StringBuilder();

        // Check each option, in the order the auth scheme resolver proposed them.
        for (AuthSchemeOption authOption : authOptions) {
            // If we're using no-auth, don't consider which options are enabled.
            if (authOption.schemeId().equals("smithy.auth#noAuth")) {
                return new SelectedAuthScheme(null, null, authOption);
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

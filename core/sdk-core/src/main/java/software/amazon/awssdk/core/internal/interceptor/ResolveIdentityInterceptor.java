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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

@SdkInternalApi
public class ResolveIdentityInterceptor implements ExecutionInterceptor {

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        SelectedAuthScheme<?> selectedAuthScheme =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        // This seems unnecessary since this interceptor won't be configured for older clients
        if (selectedAuthScheme == null) {
            return;
        }

        if ("smithy.api#noAuth".equals(selectedAuthScheme.authSchemeOption().schemeId())) {
            return;
        }

        CompletableFuture<? extends Identity> identity = resolveIdentity(selectedAuthScheme);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY, identity);
    }

    private CompletableFuture<? extends Identity> resolveIdentity(SelectedAuthScheme<?> selectedAuthScheme) {
        ResolveIdentityRequest.Builder identityRequestBuilder = ResolveIdentityRequest.builder();

        AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();
        authSchemeOption.forEachIdentityProperty(identityRequestBuilder::putProperty);

        IdentityProvider<?> identityProvider = selectedAuthScheme.identityProvider();
        return identityProvider.resolveIdentity(identityRequestBuilder.build());
    }
}

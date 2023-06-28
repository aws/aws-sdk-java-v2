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

package software.amazon.awssdk.services.codecatalyst.endpoints.internal;

import java.util.List;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.SignerLoader;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.util.SignerOverrideUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.endpoints.Endpoint;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class CodeCatalystEndpointAuthSchemeInterceptor implements ExecutionInterceptor {
    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        Endpoint resolvedEndpoint = executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);
        AwsRequest request = (AwsRequest) context.request();
        if (resolvedEndpoint.headers() != null) {
            request = AwsEndpointProviderUtils.addHeaders(request, resolvedEndpoint.headers());
        }
        List<EndpointAuthScheme> authSchemes = resolvedEndpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
        if (authSchemes == null) {
            return request;
        }
        EndpointAuthScheme chosenAuthScheme = AuthSchemeUtils.chooseAuthScheme(authSchemes);
        Supplier<Signer> signerProvider = signerProvider(chosenAuthScheme);
        AuthSchemeUtils.setSigningParams(executionAttributes, chosenAuthScheme);
        return SignerOverrideUtils.overrideSignerIfNotOverridden(request, executionAttributes, signerProvider);
    }

    private Supplier<Signer> signerProvider(EndpointAuthScheme authScheme) {
        switch (authScheme.name()) {
        case "sigv4":
            return Aws4Signer::create;
        case "sigv4a":
            return SignerLoader::getSigV4aSigner;
        default:
            break;
        }
        throw SdkClientException.create("Don't know how to create signer for auth scheme: " + authScheme.name());
    }
}

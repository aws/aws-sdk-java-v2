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

package software.amazon.awssdk.services.acm.endpoints.internal;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.acm.endpoints.AcmEndpointParams;
import software.amazon.awssdk.services.acm.endpoints.AcmEndpointProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class AcmResolveEndpointInterceptor implements ExecutionInterceptor {
    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        if (AwsEndpointProviderUtils.endpointIsDiscovered(executionAttributes)) {
            return context.request();
        }
        AcmEndpointProvider provider = (AcmEndpointProvider) executionAttributes
                .getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER);
        try {
            Endpoint result = provider.resolveEndpoint(ruleParams(context, executionAttributes)).join();
            if (!AwsEndpointProviderUtils.disableHostPrefixInjection(executionAttributes)) {
                Optional<String> hostPrefix = hostPrefix(executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME),
                        context.request());
                if (hostPrefix.isPresent()) {
                    result = AwsEndpointProviderUtils.addHostPrefix(result, hostPrefix.get());
                }
            }
            executionAttributes.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, result);
            return context.request();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SdkClientException) {
                throw (SdkClientException) cause;
            } else {
                throw SdkClientException.create("Endpoint resolution failed", cause);
            }
        }
    }

    private static AcmEndpointParams ruleParams(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        AcmEndpointParams.Builder builder = AcmEndpointParams.builder();
        builder.region(AwsEndpointProviderUtils.regionBuiltIn(executionAttributes));
        builder.useDualStack(AwsEndpointProviderUtils.dualStackEnabledBuiltIn(executionAttributes));
        builder.useFips(AwsEndpointProviderUtils.fipsEnabledBuiltIn(executionAttributes));
        builder.endpoint(AwsEndpointProviderUtils.endpointBuiltIn(executionAttributes));
        setContextParams(builder, executionAttributes.getAttribute(AwsExecutionAttribute.OPERATION_NAME), context.request());
        setStaticContextParams(builder, executionAttributes.getAttribute(AwsExecutionAttribute.OPERATION_NAME));
        return builder.build();
    }

    private static void setContextParams(AcmEndpointParams.Builder params, String operationName, SdkRequest request) {
    }

    private static void setStaticContextParams(AcmEndpointParams.Builder params, String operationName) {
    }

    private static Optional<String> hostPrefix(String operationName, SdkRequest request) {
        return Optional.empty();
    }
}

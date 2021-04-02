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

package software.amazon.awssdk.services.s3.internal.handlers;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.internal.util.RegionScope;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.ConfiguredS3SdkHttpRequest;
import software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointResolverContext;
import software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointResolverFactory;
import software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointResolverFactoryContext;
import software.amazon.awssdk.services.s3.model.S3Request;

@SdkInternalApi
public final class EndpointAddressInterceptor implements ExecutionInterceptor {

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {

        boolean endpointOverridden =
            Boolean.TRUE.equals(executionAttributes.getAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN));
        URI endpointOverride = endpointOverridden ? executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT)
                                                  : null;

        S3Configuration serviceConfiguration =
            (S3Configuration) executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG);
        boolean disableHostPrefixInjection = isDisableHostPrefixInjection(executionAttributes);
        S3EndpointResolverContext resolverContext =
            S3EndpointResolverContext.builder()
                                     .request(context.httpRequest())
                                     .originalRequest(context.request())
                                     .region(executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION))
                                     .endpointOverride(endpointOverride)
                                     .serviceConfiguration(serviceConfiguration)
                                     .disableHostPrefixInjection(disableHostPrefixInjection)
                                     .build();

        String bucketName = context.request().getValueForField("Bucket", String.class).orElse(null);
        S3EndpointResolverFactoryContext resolverFactoryContext = S3EndpointResolverFactoryContext.builder()
                .bucketName(bucketName)
                .originalRequest((S3Request) context.request())
                .build();
        ConfiguredS3SdkHttpRequest configuredRequest = S3EndpointResolverFactory.getEndpointResolver(resolverFactoryContext)
                                                                                .applyEndpointConfiguration(resolverContext);

        configuredRequest.signingRegionModification()
                         .map(region -> executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, region))
                         .orElseGet(() -> executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE,
                                                                           RegionScope.GLOBAL));

        configuredRequest.signingServiceModification().ifPresent(
            name -> executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, name));

        return configuredRequest.sdkHttpRequest();
    }

    private static boolean isDisableHostPrefixInjection(ExecutionAttributes executionAttributes) {
        return Boolean.TRUE.equals(executionAttributes.getAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION));
    }
}

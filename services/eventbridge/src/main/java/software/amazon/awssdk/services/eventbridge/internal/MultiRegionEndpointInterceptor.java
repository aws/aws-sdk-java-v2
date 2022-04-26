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

package software.amazon.awssdk.services.eventbridge.internal;

import static java.lang.Boolean.TRUE;
import static software.amazon.awssdk.awscore.util.SignerOverrideUtils.overrideSignerIfNotOverridden;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.SignerLoader;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.EndpointTag;
import software.amazon.awssdk.regions.PartitionEndpointKey;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.utils.HostnameValidator;

@SdkInternalApi
public final class MultiRegionEndpointInterceptor implements ExecutionInterceptor {

    private static final ExecutionAttribute<String> MULTI_REGION_ENDPOINT =
        new ExecutionAttribute<>("MultiRegionEndpoint");

    private static final Pattern HOSTNAME_COMPLIANT_PATTERN = Pattern.compile("[A-Za-z0-9\\-.]+");

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        String endpointId = getEndpointId(context.request());
        if (endpointId != null) {
            return handleMultiRegionEndpoint(endpointId, context.request(), executionAttributes);
        }
        return context.request();
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        String endpoint = executionAttributes.getAttribute(MULTI_REGION_ENDPOINT);
        if (endpoint != null) {
            return context.httpRequest().copy(r -> r.host(endpoint));
        }
        return context.httpRequest();
    }

    private static String getEndpointId(SdkRequest request) {
        if (request instanceof PutEventsRequest) {
            return ((PutEventsRequest) request).endpointId();
        }
        return null;
    }

    private static SdkRequest handleMultiRegionEndpoint(String endpointId,
                                                        SdkRequest request,
                                                        ExecutionAttributes executionAttributes) {
        validateEndpointId(endpointId);
        validateClientConfiguration(executionAttributes);
        if (!isEndpointOverridden(executionAttributes)) {
            String endpoint = constructEndpoint(endpointId, executionAttributes);
            executionAttributes.putAttribute(MULTI_REGION_ENDPOINT, endpoint);
        }
        return enableSigV4a(request, executionAttributes);
    }

    private static void validateEndpointId(String endpointId) {
        HostnameValidator.validateHostnameCompliant(endpointId,
                                                    "endpointId",
                                                    "PutEventsRequest",
                                                    HOSTNAME_COMPLIANT_PATTERN);
    }

    private static void validateClientConfiguration(ExecutionAttributes executionAttributes) {
        if (isFipsEnabled(executionAttributes)) {
            throw new IllegalStateException("FIPS is not supported with EventBridge multi-region endpoints");
        }
    }

    private static String constructEndpoint(String endpointId, ExecutionAttributes executionAttributes) {
        Region clientRegion = executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION);
        PartitionEndpointKey partitionEndpointKey = getPartitionEndpointKey(executionAttributes);
        String dnsSuffix = clientRegion.metadata().partition().dnsSuffix(partitionEndpointKey);
        return String.format("%s.endpoint.events.%s", endpointId, dnsSuffix);
    }

    private static PartitionEndpointKey getPartitionEndpointKey(ExecutionAttributes executionAttributes) {
        List<EndpointTag> tags = new ArrayList<>();
        if (isFipsEnabled(executionAttributes)) {
            tags.add(EndpointTag.FIPS);
        }
        if (isDualstackEnabled(executionAttributes)) {
            tags.add(EndpointTag.DUALSTACK);
        }
        return PartitionEndpointKey.builder()
                                   .tags(tags)
                                   .build();
    }

    private static SdkRequest enableSigV4a(SdkRequest request, ExecutionAttributes executionAttributes) {
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE, RegionScope.GLOBAL);
        return overrideSignerIfNotOverridden(request, executionAttributes, SignerLoader::getSigV4aSigner);
    }

    private static boolean isEndpointOverridden(ExecutionAttributes executionAttributes) {
        return TRUE.equals(executionAttributes.getAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN));
    }

    private static boolean isDualstackEnabled(ExecutionAttributes executionAttributes) {
        return TRUE.equals(executionAttributes.getAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED));
    }

    private static boolean isFipsEnabled(ExecutionAttributes executionAttributes) {
        return TRUE.equals(executionAttributes.getAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED));
    }
}

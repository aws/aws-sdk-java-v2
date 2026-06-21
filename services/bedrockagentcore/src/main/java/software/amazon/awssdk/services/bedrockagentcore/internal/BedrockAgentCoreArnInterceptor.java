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

package software.amazon.awssdk.services.bedrockagentcore.internal;

import java.lang.reflect.Method;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * Interceptor that intercepts BedrockAgentCore requests and extracts the short memory ID from
 * a full ARN if passed in the `memoryId` parameter.
 */
@SdkInternalApi
public final class BedrockAgentCoreArnInterceptor implements ExecutionInterceptor {

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest request = context.request();
        try {
            Method getMemoryIdMethod = request.getClass().getMethod("memoryId");
            String memoryId = (String) getMemoryIdMethod.invoke(request);
            if (memoryId != null && memoryId.startsWith("arn:")) {
                String shortMemoryId = extractMemoryId(memoryId);
                if (!memoryId.equals(shortMemoryId)) {
                    Method toBuilderMethod = request.getClass().getMethod("toBuilder");
                    Object builder = toBuilderMethod.invoke(request);
                    Method builderMemoryIdMethod = builder.getClass().getMethod("memoryId", String.class);
                    builderMemoryIdMethod.invoke(builder, shortMemoryId);
                    Method buildMethod = builder.getClass().getMethod("build");
                    return (SdkRequest) buildMethod.invoke(builder);
                }
            }
        } catch (NoSuchMethodException e) {
            // Request doesn't have a memoryId field (expected for some requests)
        } catch (Exception e) {
            // Safe fall-through on unexpected reflection exceptions to avoid blocking request execution
        }
        return request;
    }

    private String extractMemoryId(String memoryId) {
        if (memoryId == null) {
            return null;
        }
        if (memoryId.startsWith("arn:")) {
            // Format: arn:partition:service:region:account-id:resource
            int colonCount = 0;
            int index = -1;
            while (colonCount < 5) {
                index = memoryId.indexOf(':', index + 1);
                if (index == -1) {
                    break;
                }
                colonCount++;
            }
            if (index != -1 && index < memoryId.length() - 1) {
                String resourcePart = memoryId.substring(index + 1);
                // resourcePart is e.g. "memory/my-memory-store-AbCdEf"
                int slashIndex = resourcePart.indexOf('/');
                if (slashIndex != -1 && slashIndex < resourcePart.length() - 1) {
                    return resourcePart.substring(slashIndex + 1);
                }
                return resourcePart;
            }
        }
        return memoryId;
    }
}

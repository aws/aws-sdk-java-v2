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

import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.internal.BucketUtils;
import software.amazon.awssdk.services.s3.rules.S3ClientContextParams;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.StringUtils;

public class ChangeToDnsEndpointInterceptor implements ExecutionInterceptor {

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        String bucketName = context.request().getValueForField("Bucket", String.class).orElse(null);

        if (bucketName == null) {
            return context.httpRequest();
        }

        SdkHttpRequest.Builder mutableRequest = context.httpRequest().toBuilder();
        if (canUseVirtualAddressing(executionAttributes, bucketName)) {
            changeToDnsEndpoint(mutableRequest, bucketName);
        }
        return mutableRequest.build();
    }

    private static boolean canUseVirtualAddressing(ExecutionAttributes executionAttributes, String bucketName) {
        AttributeMap clientContextParams = executionAttributes.getAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS);
        boolean isPathStyleEnabled = clientContextParams.get(S3ClientContextParams.FORCE_PATH_STYLE) != null && clientContextParams.get(S3ClientContextParams.FORCE_PATH_STYLE);

        return !isPathStyleEnabled && BucketUtils.isVirtualAddressingCompatibleBucketName(bucketName, false);
    }

    /**
     * Changes from path style addressing (which the marshallers produce by default), to DNS style/virtual style addressing,
     * where the bucket name is prepended to the host. DNS style addressing is preferred due to the better load balancing
     * qualities it provides; path style is an option mainly for proxy based situations and alternative S3 implementations.
     *
     * @param mutableRequest Marshalled HTTP request we are modifying.
     * @param bucketName     Bucket name for this particular operation.
     */
    private static void changeToDnsEndpoint(SdkHttpRequest.Builder mutableRequest, String bucketName) {
        if (mutableRequest.host().startsWith("s3")) {
            String newHost = StringUtils.replaceOnce(mutableRequest.host(), "s3", bucketName + "." + "s3");
            String newPath = StringUtils.replaceOnce(mutableRequest.encodedPath(), "/" + bucketName, "");
            mutableRequest.host(newHost).encodedPath(newPath);
        }
    }

}

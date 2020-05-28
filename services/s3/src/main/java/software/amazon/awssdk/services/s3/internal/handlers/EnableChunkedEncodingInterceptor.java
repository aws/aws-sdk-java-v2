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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Interceptor to enable chunked encoding on specific upload operations if the option does not already have a value.
 * <p>
 * This affects the following requests:
 * <ul>
 *     <li>{@link PutObjectRequest}</li>
 *     <li>{@link UploadPartRequest}</li>
 * </ul>
 */
@SdkInternalApi
public final class EnableChunkedEncodingInterceptor implements ExecutionInterceptor {

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest sdkRequest = context.request();

        if (sdkRequest instanceof PutObjectRequest || sdkRequest instanceof UploadPartRequest) {
            S3Configuration serviceConfiguration =
                    (S3Configuration) executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG);

            boolean enableChunkedEncoding;
            if (serviceConfiguration != null) {
                enableChunkedEncoding = serviceConfiguration.chunkedEncodingEnabled();
            } else {
                enableChunkedEncoding = true;
            }

            executionAttributes.putAttributeIfAbsent(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING, enableChunkedEncoding);
        }

        return sdkRequest;
    }
}

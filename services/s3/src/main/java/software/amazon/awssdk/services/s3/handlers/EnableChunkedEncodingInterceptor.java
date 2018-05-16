/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.handlers;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.auth.S3ExecutionAttributes;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Interceptor to enable chunked encoding on specific upload operations.
 */
@ReviewBeforeRelease("Check if there are other operations that should have chuncked encoding enabled by default."
                     + "Do we even require this? By default chunked encoding is enabled. See AwsS3V4SignerParams class"
                     + "Maybe we need interceptor for payload signing as its disabled by default. See JAVA-2775")
public class EnableChunkedEncodingInterceptor implements ExecutionInterceptor {

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest sdkRequest = context.request();

        if (sdkRequest instanceof PutObjectRequest || sdkRequest instanceof UploadPartRequest) {
            executionAttributes.putAttribute(S3ExecutionAttributes.ENABLE_CHUNKED_ENCODING, Boolean.TRUE);
        }

        return sdkRequest;
    }
}

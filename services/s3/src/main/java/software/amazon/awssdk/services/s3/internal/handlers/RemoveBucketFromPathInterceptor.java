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
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public class RemoveBucketFromPathInterceptor implements ExecutionInterceptor {

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        String bucketName = context.request().getValueForField("Bucket", String.class).orElse(null);

        if (bucketName == null) {
            return context.httpRequest();
        }

        SdkHttpRequest.Builder mutableRequest = context.httpRequest().toBuilder();
        String encodedBucket = SdkHttpUtils.urlEncode(bucketName);
        String newPath = StringUtils.replaceOnce(mutableRequest.encodedPath(), "/" + encodedBucket, "");

        return mutableRequest.encodedPath(newPath).build();
    }
}

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

package software.amazon.awssdk.services.glacier.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.glacier.model.DescribeJobRequest;
import software.amazon.awssdk.services.glacier.model.GetJobOutputRequest;
import software.amazon.awssdk.services.glacier.model.UploadMultipartPartRequest;

@SdkInternalApi
public final class GlacierExecutionInterceptor implements ExecutionInterceptor {

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpRequest request = context.httpRequest();
        Object originalRequest = context.request();
        return request.toBuilder()
                      .applyMutation(b -> beforeRequest(originalRequest, b))
                      .build();
    }

    private SdkHttpRequest.Builder beforeRequest(Object originalRequest, SdkHttpRequest.Builder mutableRequest) {
        mutableRequest.putHeader("x-amz-glacier-version", "2012-06-01");

        //  "x-amz-content-sha256" header is required for sig v4 for some streaming operations
        mutableRequest.putHeader("x-amz-content-sha256", "required");

        if (originalRequest instanceof UploadMultipartPartRequest) {
            mutableRequest.firstMatchingHeader("Content-Range")
                          .ifPresent(range -> mutableRequest.putHeader("Content-Length",
                                                                       Long.toString(parseContentLengthFromRange(range))));

        } else if (originalRequest instanceof GetJobOutputRequest || originalRequest instanceof DescribeJobRequest) {
            String resourcePath = mutableRequest.encodedPath();
            if (resourcePath != null) {
                String newResourcePath = resourcePath.replace("{jobType}", "archive-retrievals");
                mutableRequest.encodedPath(newResourcePath);
            }
        }
        return mutableRequest;
    }

    private long parseContentLengthFromRange(String range) {
        if (range.startsWith("bytes=") || range.startsWith("bytes ")) {
            range = range.substring(6);
        }

        String start = range.substring(0, range.indexOf('-'));
        String end = range.substring(range.indexOf('-') + 1);

        if (end.contains("/")) {
            end = end.substring(0, end.indexOf("/"));
        }

        return Long.parseLong(end) - Long.parseLong(start) + 1;
    }

}

/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.glacier.model.DescribeJobRequest;
import software.amazon.awssdk.services.glacier.model.GetJobOutputRequest;
import software.amazon.awssdk.services.glacier.model.UploadMultipartPartRequest;

public class GlacierRequestHandler extends RequestHandler {

    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {
        Object originalRequest = request.handlerContext(AwsHandlerKeys.REQUEST_CONFIG).getOriginalRequest();
        return request.toBuilder()
                      .apply(b -> beforeRequest(originalRequest, b))
                      .build();
    }

    private SdkHttpFullRequest.Builder beforeRequest(Object originalRequest, SdkHttpFullRequest.Builder mutableRequest) {
        mutableRequest.header("x-amz-glacier-version", "2012-06-01");

        //  "x-amz-content-sha256" header is required for sig v4 for some streaming operations
        mutableRequest.header("x-amz-content-sha256", "required");

        if (originalRequest instanceof UploadMultipartPartRequest) {
            mutableRequest.getFirstHeaderValue("Content-Range").ifPresent(range -> mutableRequest
                    .header("Content-Length", Long.toString(parseContentLengthFromRange(range))));

        } else if (originalRequest instanceof GetJobOutputRequest || originalRequest instanceof DescribeJobRequest) {
            String resourcePath = mutableRequest.getResourcePath();
            if (resourcePath != null) {
                String newResourcePath = resourcePath.replace("{jobType}", "archive-retrievals");
                mutableRequest.resourcePath(newResourcePath);
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

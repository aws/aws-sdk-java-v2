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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Interceptor for {@link GetObjectRequest} messages.
 */
@SdkInternalApi
public class GetObjectInterceptor implements ExecutionInterceptor {
    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        SdkResponse response = context.response();
        if (!(response instanceof GetObjectResponse)) {
            return response;
        }

        return fixContentRange(response, context.httpResponse());
    }

    /**
     * S3 currently returns content-range in two possible headers: Content-Range or x-amz-content-range based on the x-amz-te
     * in the request. This will check the x-amz-content-range if the modeled header (Content-Range) wasn't populated.
     */
    private SdkResponse fixContentRange(SdkResponse sdkResponse, SdkHttpResponse httpResponse) {
        // Use the modeled content range header, if the service returned it.
        GetObjectResponse getObjectResponse = (GetObjectResponse) sdkResponse;
        if (getObjectResponse.contentRange() != null) {
            return getObjectResponse;
        }

        // If the service didn't use the modeled content range header, check the x-amz-content-range header.
        Optional<String> xAmzContentRange = httpResponse.firstMatchingHeader("x-amz-content-range");
        if (!xAmzContentRange.isPresent()) {
            return getObjectResponse;
        }

        return getObjectResponse.copy(r -> r.contentRange(xAmzContentRange.get()));
    }
}

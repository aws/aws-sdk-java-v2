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

package software.amazon.awssdk.services.apigateway.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;

@SdkInternalApi
public final class AcceptJsonInterceptor implements ExecutionInterceptor {
    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {
        // Some APIG operations marshall to the 'Accept' header to specify the
        // format of the document returned by the service, such as GetExport
        // https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-export-api.html.
        // See the same fix in V1:
        // https://github.com/aws/aws-sdk-java/blob/cd2275c07df8656033bfa9baa665354bfb17a6bf/aws-java-sdk-api-gateway/src/main/java/com/amazonaws/services/apigateway/internal/AcceptJsonRequestHandler.java#L29
        SdkHttpRequest httpRequest = context.httpRequest();
        if (!httpRequest.headers().containsKey("Accept")) {
            return httpRequest
                    .toBuilder()
                    .putHeader("Accept", "application/json")
                    .build();
        }
        return httpRequest;
    }
}

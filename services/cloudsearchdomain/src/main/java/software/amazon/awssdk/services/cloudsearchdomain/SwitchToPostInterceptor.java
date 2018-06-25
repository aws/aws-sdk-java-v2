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

package software.amazon.awssdk.services.cloudsearchdomain;

import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MoveParametersToBodyStage;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.cloudsearchdomain.model.SearchRequest;

/**
 * Ensures that all SearchRequests use <code>POST</code> instead of <code>GET</code>, moving the query parameters to be form data.
 */
public class SwitchToPostInterceptor implements ExecutionInterceptor {
    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();
        Object originalRequest = context.request();
        if (originalRequest instanceof SearchRequest && request.method() == SdkHttpMethod.GET) {
            return request.toBuilder()
                          .method(SdkHttpMethod.POST)
                          .applyMutation(MoveParametersToBodyStage::changeQueryParametersToFormData)
                          .build();
        }
        return request;
    }
}

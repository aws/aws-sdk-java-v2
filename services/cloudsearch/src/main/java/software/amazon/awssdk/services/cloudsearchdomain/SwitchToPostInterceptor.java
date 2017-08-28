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

package software.amazon.awssdk.services.cloudsearchdomain;

import java.io.ByteArrayInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.interceptor.Context;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.interceptor.Priority;
import software.amazon.awssdk.services.cloudsearchdomain.model.SearchRequest;
import software.amazon.awssdk.util.SdkHttpUtils;

/**
 * Ensures that all SearchRequests use <code>POST</code> instead of <code>GET</code>.
 */
public class SwitchToPostInterceptor implements ExecutionInterceptor {
    @Override
    public Priority priority() {
        return Priority.SERVICE;
    }

    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();
        Object originalRequest = context.request();
        if (originalRequest instanceof SearchRequest && request.getHttpMethod() == SdkHttpMethod.GET) {
            final byte[] content = SdkHttpUtils.encodeParameters(request).getBytes();
            return request.toBuilder()
                          .httpMethod(SdkHttpMethod.POST)
                          .content(new ByteArrayInputStream(content))
                          .header("Content-Type", "application/x-www-form-urlencoded")
                          .header("Content-Length", Integer.toString(content.length))
                          .clearQueryParameters()
                          .build();
        }
        return request;
    }
}

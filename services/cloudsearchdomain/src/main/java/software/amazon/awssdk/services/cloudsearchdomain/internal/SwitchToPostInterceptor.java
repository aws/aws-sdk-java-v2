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

package software.amazon.awssdk.services.cloudsearchdomain.internal;

import static java.util.Collections.singletonList;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.cloudsearchdomain.model.SearchRequest;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Ensures that all SearchRequests use <code>POST</code> instead of <code>GET</code>, moving the query parameters to be form data.
 */
@SdkInternalApi
public final class SwitchToPostInterceptor implements ExecutionInterceptor {

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpRequest httpRequest = context.httpRequest();
        if (context.request() instanceof SearchRequest) {
            return httpRequest.toBuilder()
                              .clearQueryParameters()
                              .method(SdkHttpMethod.POST)
                              .putHeader("Content-Type", singletonList("application/x-www-form-urlencoded"))
                              .build();
        }
        return context.httpRequest();
    }

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        if (context.request() instanceof SearchRequest) {
            byte[] params = SdkHttpUtils.encodeAndFlattenFormData(context.httpRequest().rawQueryParameters()).orElse("")
                                        .getBytes(StandardCharsets.UTF_8);
            return Optional.of(RequestBody.fromContentProvider(() -> new ByteArrayInputStream(params),
                                                               params.length,
                                                               "application/x-www-form-urlencoded; charset=" +
                                                               lowerCase(StandardCharsets.UTF_8.toString())));
        }

        return context.requestBody();
    }

}

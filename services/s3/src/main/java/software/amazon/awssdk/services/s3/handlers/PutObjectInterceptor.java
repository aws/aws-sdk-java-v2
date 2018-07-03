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
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Interceptor to add an 'Expect: 100-continue' header to the HTTP Request if it represents a PUT Object request.
 */
@ReviewBeforeRelease("This should be generalized for all streaming requests when we refactor the marshallers.")
public class PutObjectInterceptor implements ExecutionInterceptor {
    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        if (context.request() instanceof PutObjectRequest) {
            return context.httpRequest().toBuilder().putHeader("Expect", "100-continue").build();
        }
        return context.httpRequest();
    }
}

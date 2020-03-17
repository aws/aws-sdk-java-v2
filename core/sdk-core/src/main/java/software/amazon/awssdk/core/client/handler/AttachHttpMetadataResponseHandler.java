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

package software.amazon.awssdk.core.client.handler;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Decorate {@link HttpResponseHandler} to attach {@link SdkHttpResponse} to the response object.
 */
@SdkProtectedApi
public final class AttachHttpMetadataResponseHandler<T extends SdkResponse> implements HttpResponseHandler<T> {

    private final HttpResponseHandler<T> delegate;

    public AttachHttpMetadataResponseHandler(HttpResponseHandler<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        return (T) delegate.handle(response, executionAttributes)
                           .toBuilder()
                           .sdkHttpResponse(response)
                           .build();
    }
}

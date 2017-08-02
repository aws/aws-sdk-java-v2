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

package software.amazon.awssdk.opensdk.internal.protocol;

import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpMetadata;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.opensdk.BaseResult;
import software.amazon.awssdk.opensdk.SdkResponseMetadata;

public class ApiGatewayResponseHandler<T extends BaseResult> implements
                                                             HttpResponseHandler<T> {

    private final HttpResponseHandler<T> delegate;

    public ApiGatewayResponseHandler(HttpResponseHandler<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T handle(HttpResponse httpResponse, ExecutionAttributes executionAttributes) throws Exception {
        T result = delegate.handle(httpResponse, executionAttributes);
        result.sdkResponseMetadata(new SdkResponseMetadata(SdkHttpMetadata.from(httpResponse)));
        return result;
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return delegate.needsConnectionLeftOpen();
    }
}

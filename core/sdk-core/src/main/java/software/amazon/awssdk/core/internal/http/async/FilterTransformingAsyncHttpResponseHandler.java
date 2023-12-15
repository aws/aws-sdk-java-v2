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

package software.amazon.awssdk.core.internal.http.async;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Decorator class that simply delegates to the wrapped {@link TransformingAsyncResponseHandler}. Useful for intercepting the
 * desired callback(s) before passing them on to the delegate.
 */
@SdkInternalApi
public abstract class FilterTransformingAsyncHttpResponseHandler<ResultT> implements TransformingAsyncResponseHandler<ResultT> {
    private final TransformingAsyncResponseHandler<ResultT> delegate;

    public FilterTransformingAsyncHttpResponseHandler(TransformingAsyncResponseHandler<ResultT> delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<ResultT> prepare() {
        return delegate.prepare();
    }

    @Override
    public void onHeaders(SdkHttpResponse headers) {
        delegate.onHeaders(headers);
    }

    @Override
    public void onStream(Publisher<ByteBuffer> stream) {
        delegate.onStream(stream);
    }

    @Override
    public void onError(Throwable error) {
        delegate.onError(error);
    }
}

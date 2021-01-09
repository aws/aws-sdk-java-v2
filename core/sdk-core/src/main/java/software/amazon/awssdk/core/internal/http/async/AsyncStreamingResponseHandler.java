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
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Response handler for asynchronous streaming operations.
 */
@SdkInternalApi
public final class AsyncStreamingResponseHandler<OutputT extends SdkResponse, ReturnT>
    implements TransformingAsyncResponseHandler<ReturnT> {

    private final AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer;
    private volatile HttpResponseHandler<OutputT> responseHandler;

    public AsyncStreamingResponseHandler(AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer) {
        this.asyncResponseTransformer = asyncResponseTransformer;
    }

    public void responseHandler(HttpResponseHandler<OutputT> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    public void onHeaders(SdkHttpResponse response) {
        try {
            // TODO would be better to pass in AwsExecutionAttributes to the async response handler so we can
            // provide them to HttpResponseHandler
            OutputT resp = responseHandler.handle((SdkHttpFullResponse) response, null);

            asyncResponseTransformer.onResponse(resp);
        } catch (Exception e) {
            asyncResponseTransformer.exceptionOccurred(e);
        }
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        asyncResponseTransformer.onStream(SdkPublisher.adapt(publisher));
    }

    @Override
    public void onError(Throwable error) {
        asyncResponseTransformer.exceptionOccurred(error);
    }

    @Override
    public CompletableFuture<ReturnT> prepare() {
        return asyncResponseTransformer.prepare();
    }
}

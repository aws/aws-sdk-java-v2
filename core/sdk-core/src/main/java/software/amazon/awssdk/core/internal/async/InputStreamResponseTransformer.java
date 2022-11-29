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

package software.amazon.awssdk.core.internal.async;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.async.InputStreamSubscriber;

/**
 * A {@link AsyncResponseTransformer} that allows performing blocking reads on the response data.
 * <p>
 * Created with {@link AsyncResponseTransformer#toBlockingInputStream()}.
 */
@SdkInternalApi
public class InputStreamResponseTransformer<ResponseT extends SdkResponse>
    implements AsyncResponseTransformer<ResponseT, ResponseInputStream<ResponseT>> {

    private volatile CompletableFuture<ResponseInputStream<ResponseT>> future;
    private volatile ResponseT response;

    @Override
    public CompletableFuture<ResponseInputStream<ResponseT>> prepare() {
        CompletableFuture<ResponseInputStream<ResponseT>> result = new CompletableFuture<>();
        this.future = result;
        return result;
    }

    @Override
    public void onResponse(ResponseT response) {
        this.response = response;
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        InputStreamSubscriber inputStreamSubscriber = new InputStreamSubscriber();
        publisher.subscribe(inputStreamSubscriber);
        future.complete(new ResponseInputStream<>(response, inputStreamSubscriber));
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        future.completeExceptionally(error);
    }
}

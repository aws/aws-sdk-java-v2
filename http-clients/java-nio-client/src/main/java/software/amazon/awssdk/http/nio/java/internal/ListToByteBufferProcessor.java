/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.java.internal;

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A processor connects the publisher (HttpResponse.BodyHandler) and subscriber (SdkAsyncHttpResponseHandler).
 * Since jdk 11 doesn't support creating HttpResponse.BodyHandler with ByteBuffer, but supports that with
 * List&lt;ByteBuffer&gt;, while SDK expects a ByteBuffer-typed publisher, so this class is to convert List
 * object into ByteBuffer, then the flow is like publisher - processor - subscriber.
 */
@SdkInternalApi
public final class ListToByteBufferProcessor implements Subscriber<List<ByteBuffer>> {

    private PublishProcessor<List<ByteBuffer>> processor;
    private Flowable<ByteBuffer> publisherToSdk;

    private final CompletableFuture<Void> terminated = new CompletableFuture<>();


    ListToByteBufferProcessor() {
        processor = PublishProcessor.create();
        publisherToSdk = processor.map(list -> {
            int bodyPartSize = list.stream().mapToInt(Buffer::remaining).sum();
            ByteBuffer processedByteBuffer = ByteBuffer.allocate(bodyPartSize);
            list.forEach(processedByteBuffer::put);
            return processedByteBuffer;
        });
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        processor.onSubscribe(subscription);
    }

    @Override
    public void onNext(List<ByteBuffer> byteBufferList) {
        processor.onNext(byteBufferList);

    }

    @Override
    public void onError(Throwable throwable) {
        terminated.completeExceptionally(throwable);
        processor.onError(throwable);
    }

    @Override
    public void onComplete() {
        terminated.complete(null);
        processor.onComplete();
    }

    Flowable<ByteBuffer> getPublisherToSdk() {
        return publisherToSdk;
    }

    CompletableFuture<Void> getTerminated() {
        return terminated;
    }

}

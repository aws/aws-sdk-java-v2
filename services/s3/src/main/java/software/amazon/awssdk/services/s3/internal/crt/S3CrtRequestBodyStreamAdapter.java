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

package software.amazon.awssdk.services.s3.internal.crt;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;

/**
 * Adapts an SDK {@link software.amazon.awssdk.core.async.AsyncRequestBody} to CRT's {@link HttpRequestBodyStream}.
 */
@SdkInternalApi
public final class S3CrtRequestBodyStreamAdapter implements HttpRequestBodyStream {
    private final Publisher<ByteBuffer> bodyPublisher;
    private final ByteBufferStoringSubscriber requestBodySubscriber;


    public S3CrtRequestBodyStreamAdapter(Publisher<ByteBuffer> bodyPublisher) {
        this.bodyPublisher = bodyPublisher;
        this.requestBodySubscriber = new ByteBufferStoringSubscriber(16 * 1024 * 1024);
        bodyPublisher.subscribe(requestBodySubscriber);
    }

    @Override
    public boolean sendRequestBody(ByteBuffer outBuffer) {
        return requestBodySubscriber.transferTo(outBuffer) == ByteBufferStoringSubscriber.TransferResult.END_OF_STREAM;
    }
}

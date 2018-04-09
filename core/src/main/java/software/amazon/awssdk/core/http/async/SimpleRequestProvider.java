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

package software.amazon.awssdk.core.http.async;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestClientOptions;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Implementation of {@link SdkHttpRequestProvider} that provides all it's data at once. Useful for
 * non streaming operations that are already marshalled into memory.
 */
@SdkInternalApi
public class SimpleRequestProvider implements SdkHttpRequestProvider {

    private final byte[] content;
    private final int length;

    public SimpleRequestProvider(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        this.content = request.content().map(content -> {
            try {
                content.mark(getReadLimit(executionAttributes));
                return invokeSafely(() -> IoUtils.toByteArray(content));
            } finally {
                invokeSafely(content::reset);
            }
        }).orElseGet(() -> new byte[0]);
        this.length = content.length;
    }

    @ReviewBeforeRelease("Do we still want to make read limit user-configurable as in V1?")
    private int getReadLimit(ExecutionAttributes executionAttributes) {
        return RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE;
    }

    @Override
    public long contentLength() {
        return length;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        s.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                s.onNext(ByteBuffer.wrap(content));
                s.onComplete();
            }

            @Override
            public void cancel() {
            }
        });
    }
}

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

package software.amazon.awssdk.http.async;

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Responsible for handling asynchronous http responses
 *
 * @param <T> Type of result returned in {@link #complete()}. May be {@link Void}.
 */
public interface SdkHttpResponseHandler<T> {

    /**
     * Called when the initial response with headers is received.
     *
     * @param response the {@link SdkHttpResponse}
     */
    void headersReceived(SdkHttpResponse response);

    /**
     * Called when the HTTP client is ready to start sending data to the response handler. Implementations
     * must subscribe to the {@link Publisher} and request data via a {@link org.reactivestreams.Subscription} as
     * they can handle it.
     *
     * <p>
     * If at any time the subscriber wishes to stop receiving data, it may call {@link Subscription#cancel()}. This
     * will be treated as a failure of the response and the {@link #exceptionOccurred(Throwable)} callback will be invoked.
     * </p>
     */
    void onStream(Publisher<ByteBuffer> publisher);

    /**
     * Called when an exception occurs during the request/response.
     *
     * This is a terminal method call, no other method invocations should be expected
     * on the {@link SdkHttpResponseHandler} after this point.
     *
     * @param throwable the exception that occurred.
     */
    void exceptionOccurred(Throwable throwable);

    /**
     * Called when all parts of the response have been received.
     *
     * This is a terminal method call, no other method invocations should be expected
     * on the {@link SdkHttpResponseHandler} after this point.
     *
     * @return Transformed result.
     */
    T complete();

}

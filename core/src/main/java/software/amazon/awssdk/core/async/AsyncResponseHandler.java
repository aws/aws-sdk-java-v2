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

package software.amazon.awssdk.core.async;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * Callback interface to handle a streaming asynchronous response.
 *
 * @param <ResponseT> POJO response type.
 * @param <ReturnT>   Type this response handler produces. I.E. the type you are transforming the response into.
 */
public interface AsyncResponseHandler<ResponseT, ReturnT> extends BaseAsyncResponseHandler<ResponseT, ByteBuffer, ReturnT> {

    /**
     * Called when events are ready to be streamed. Implementations  must subscribe to the {@link Publisher} and request data via
     * a {@link org.reactivestreams.Subscription} as they can handle it.
     *
     * <p>
     * If at any time the subscriber wishes to stop receiving data, it may call {@link Subscription#cancel()}. This
     * will be treated as a failure of the response and the {@link #exceptionOccurred(Throwable)} callback will be invoked.
     * </p>
     *
     * <p>This callback may never be called if the response has no content or if an error occurs.</p>
     *
     * <p>
     * In the event of a retryable error, this callback may be called multiple times with different Publishers.
     * If this method is called more than once, implementation must either reset any state to prepare for another
     * stream of data or must throw an exception indicating they cannot reset. If any exception is thrown then no
     * automatic retry is performed.
     * </p>
     */
    void onStream(Publisher<ByteBuffer> publisher);

    /**
     * Creates an {@link AsyncResponseHandler} that writes all the content to the given file. In the event of an error,
     * the SDK will attempt to delete the file (whatever has been written to it so far). If the file already exists, an
     * exception will be thrown.
     *
     * @param path        Path to file to write to.
     * @param <ResponseT> Pojo Response type.
     * @return AsyncResponseHandler instance.
     */
    static <ResponseT> AsyncResponseHandler<ResponseT, ResponseT> toFile(Path path) {
        return new FileAsyncResponseHandler<>(path);
    }

    /**
     * Creates an {@link AsyncResponseHandler} that writes all content to a byte array.
     *
     * @param <ResponseT> Pojo response type.
     * @return AsyncResponseHandler instance.
     */
    static <ResponseT> AsyncResponseHandler<ResponseT, byte[]> toByteArray() {
        return new ByteArrayAsyncResponseHandler<>();
    }

    /**
     * Creates an {@link AsyncResponseHandler} that writes all content to a string using the specified encoding.
     *
     * @param charset     {@link Charset} to use when constructing the string.
     * @param <ResponseT> Pojo response type.
     * @return AsyncResponseHandler instance.
     */
    static <ResponseT> AsyncResponseHandler<ResponseT, String> toString(Charset charset) {
        return new StringAsyncResponseHandler<>(toByteArray(), charset);
    }

    /**
     * Creates an {@link AsyncResponseHandler} that writes all content to UTF8 encoded string.
     *
     * @param <ResponseT> Pojo response type.
     * @return AsyncResponseHandler instance.
     */
    static <ResponseT> AsyncResponseHandler<ResponseT, String> toUtf8String() {
        return toString(StandardCharsets.UTF_8);
    }

}

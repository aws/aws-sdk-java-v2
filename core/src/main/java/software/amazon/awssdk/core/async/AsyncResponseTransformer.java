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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformer;

/**
 * Callback interface to handle a streaming asynchronous response.
 *
 * @param <ResponseT> POJO response type.
 * @param <ReturnT>   Type this response handler produces. I.E. the type you are transforming the response into.
 */
@SdkPublicApi
public interface AsyncResponseTransformer<ResponseT, ReturnT> {
    /**
     * Called when the initial response (headers/status code) has been received and the POJO response has
     * been unmarshalled. This is guaranteed to be called before {@link #onStream(Publisher)}.
     *
     * <p>In the event of a retryable error, this callback may be called multiple times. It
     * also may never be invoked if the request never succeeds.</p>
     *
     * @param response Unmarshalled POJO containing metadata about the streamed data.
     */
    void responseReceived(ResponseT response);

    /**
     * Called when the HTTP client is ready to start sending data to the response handler. Implementations
     * must subscribe to the {@link Publisher} and request data via a {@link org.reactivestreams.Subscription} as
     * they can handle it.
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
     * Called when an exception occurs while establishing the connection or streaming the response. Implementations
     * should free up any resources in this method. This method may be called multiple times during the lifecycle
     * of a request if automatic retries are enabled.
     *
     * @param throwable Exception that occurred.
     */
    void exceptionOccurred(Throwable throwable);

    /**
     * Called when all data has been successfully published to the {@link org.reactivestreams.Subscriber}. This will
     * only be called once during the lifecycle of the request. Implementors should free up any resources they have
     * opened and do final transformations to produce the return object.
     *
     * @return Transformed object as a result of the streamed data.
     */
    ReturnT complete();

    /**
     * Creates an {@link AsyncResponseTransformer} that writes all the content to the given file. In the event of an error,
     * the SDK will attempt to delete the file (whatever has been written to it so far). If the file already exists, an
     * exception will be thrown.
     *
     * @param path        Path to file to write to.
     * @param <ResponseT> Pojo Response type.
     * @return AsyncResponseTransformer instance.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseT> toFile(Path path) {
        return new FileAsyncResponseTransformer<>(path);
    }

    /**
     * Creates an {@link AsyncResponseTransformer} that writes all the content to the given file. In the event of an error,
     * the SDK will attempt to delete the file (whatever has been written to it so far). If the file already exists, an
     * exception will be thrown.
     *
     * @param file        File to write to.
     * @param <ResponseT> Pojo Response type.
     * @return AsyncResponseTransformer instance.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseT> toFile(File file) {
        return toFile(file.toPath());
    }

    /**
     * Creates an {@link AsyncResponseTransformer} that writes all content to a byte array.
     *
     * @param <ResponseT> Pojo response type.
     * @return AsyncResponseTransformer instance.
     */
    static <ResponseT> AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> toBytes() {
        return new ByteArrayAsyncResponseTransformer<>();
    }
}

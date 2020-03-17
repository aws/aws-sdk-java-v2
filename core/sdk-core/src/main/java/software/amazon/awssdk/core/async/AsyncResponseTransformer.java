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

package software.amazon.awssdk.core.async;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformer;

/**
 * Callback interface to handle a streaming asynchronous response.
 * <p>
 * <h2>Synchronization</h2>
 * <p>
 * All operations, including those called on the {@link org.reactivestreams.Subscriber} of the stream are guaranteed to be
 * synchronized externally; i.e. no two methods on this interface or on the {@link org.reactivestreams.Subscriber} will be
 * invoked concurrently. It is <b>not</b> guaranteed that the methods will being invoked by the same thread.
 * <p>
 * <h2>Invocation Order</h2>
 * <p>
 * The methods are called in the following order:
 * <ul>
 *     <li>
 *     {@link #prepare()}: This method is always called first. Implementations should use this to setup or perform any
 *     cleanup necessary. <b>Note that this will be called upon each request attempt</b>. If the {@link CompletableFuture}
 *     returned from the previous invocation has already been completed, the implementation should return a new instance.
 *     </li>
 *     <li>
 *     {@link #onResponse}: If the response was received successfully, this method is called next.
 *     </li>
 *     <li>
 *     {@link #onStream(SdkPublisher)}: Called after {@code onResponse}. This is always invoked, even if the service
 *     operation response does not contain a body. If the response does not have a body, then the {@link SdkPublisher} will
 *     complete the subscription without signaling any elements.
 *     </li>
 *     <li>
 *     {@link #exceptionOccurred(Throwable)}: If there is an error sending the request. This method is called before {@link
 *     org.reactivestreams.Subscriber#onError(Throwable)}.
 *     </li>
 *     <li>
 *     {@link org.reactivestreams.Subscriber#onError(Throwable)}: If an error is encountered while the {@code Publisher} is
 *     publishing to a {@link org.reactivestreams.Subscriber}.
 *     </li>
 * </ul>
 * <p>
 * <h2>Retries</h2>
 * <p>
 * The transformer has the ability to trigger retries at any time by completing the {@link CompletableFuture} with an
 * exception that is deemed retryable by the configured {@link software.amazon.awssdk.core.retry.RetryPolicy}.
 *
 * @param <ResponseT> POJO response type.
 * @param <ResultT>   Type this response handler produces. I.E. the type you are transforming the response into.
 */
@SdkPublicApi
public interface AsyncResponseTransformer<ResponseT, ResultT> {
    /**
     * Initial call to enable any setup required before the response is handled.
     * <p>
     * Note that this will be called for each request attempt, up to the number of retries allowed by the configured {@link
     * software.amazon.awssdk.core.retry.RetryPolicy}.
     * <p>
     * This method is guaranteed to be called before the request is executed, and before {@link #onResponse(Object)} is
     * signaled.
     *
     * @return The future holding the transformed response.
     */
    CompletableFuture<ResultT> prepare();

    /**
     * Called when the unmarshalled response object is ready.
     *
     * @param response The unmarshalled response.
     */
    void onResponse(ResponseT response);

    /**
     * Called when the response stream is ready.
     *
     * @param publisher The publisher.
     */
    void onStream(SdkPublisher<ByteBuffer> publisher);

    /**
     * Called when an error is encountered while making the request or receiving the response.
     * Implementations should free up any resources in this method. This method may be called
     * multiple times during the lifecycle of a request if automatic retries are enabled.
     *
     * @param error Error that occurred.
     */
    void exceptionOccurred(Throwable error);

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

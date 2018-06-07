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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * Callback interface to handle a streaming asynchronous response that produces a certain type of event.
 *
 * @param <ResponseT> POJO response type.
 * @param <ReturnT> Type this response handler produces. I.E. the type you are transforming the response into.
 */
public interface BaseAsyncResponseTransformer<ResponseT, PublisherT, ReturnT> {

    /**
     * Called when the initial response has been received and the POJO response has
     * been unmarshalled. This is guaranteed to be called before onStream.
     *
     * <p>In the event of a retryable error, this callback may be called multiple times. It
     * also may never be invoked if the request never succeeds.</p>
     *
     * @param response Unmarshalled POJO containing metadata about the streamed data.
     */
    void responseReceived(ResponseT response);

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
    void onStream(PublisherT publisher);


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
}

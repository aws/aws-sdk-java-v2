/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.awssdk.core.flow;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.async.BaseAsyncResponseHandler;

/**
 * Response handler for a flow operation.
 *
 * @param <ResponseT> Intial response type of flow operation.
 * @param <EventT> Event type returned by the flow stream.
 * @param <ReturnT> Transformation type returned in {@link #complete()}.
 */
public interface SdkFlowResponseHandler<ResponseT, EventT, ReturnT> extends BaseAsyncResponseHandler<ResponseT, EventT, ReturnT> {

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
    void onStream(SdkPublisher<EventT> publisher);
}

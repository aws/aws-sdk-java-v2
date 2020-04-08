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

package software.amazon.awssdk.http.async;

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Handles asynchronous HTTP responses.
 */
@SdkProtectedApi
public interface SdkAsyncHttpResponseHandler {
    /**
     * Called when the headers have been received.
     *
     * @param headers The headers.
     */
    void onHeaders(SdkHttpResponse headers);

    /**
     * Called when the streaming body is ready.
     * <p>
     * This method is always called. If the response does not have a body, then the publisher will complete the subscription
     * without signalling any elements.
     *
     * @param stream The streaming body.
     */
    void onStream(Publisher<ByteBuffer> stream);

    /**
     * Called when there is an error making the request or receiving the response. If the error is encountered while
     * streaming the body, then the error is also delivered to the {@link org.reactivestreams.Subscriber}.
     *
     * @param error The error.
     */
    void onError(Throwable error);

}

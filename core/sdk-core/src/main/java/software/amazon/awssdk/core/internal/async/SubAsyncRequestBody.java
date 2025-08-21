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

package software.amazon.awssdk.core.internal.async;

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;

/**
 * Represent a sub {@link AsyncRequestBody} that publishes a portion of the source {@link AsyncRequestBody}
 */
@SdkInternalApi
public interface SubAsyncRequestBody extends CloseableAsyncRequestBody {

    /**
     * Send a byte buffer.
     * <p>
     * This method must not be invoked concurrently.
     */
    void send(ByteBuffer byteBuffer);

    /**
     *  Indicate that no more {@link #send(ByteBuffer)} )} calls will be made,
     *  and that stream of messages is completed successfully.
     */
    void complete();

    /**
     * The maximum length of the content this AsyncRequestBody can hold. If the upstream content length is known, this should be
     * the same as receivedBytesLength
     */
    long maxLength();

    /**
     * The length of the bytes received
     */
    long receivedBytesLength();

    @Override
    default void close() {
        // no op
    }

    /**
     * The part number associated with this SubAsyncRequestBody
     * @return
     */
    int partNumber();
}

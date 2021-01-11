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

package software.amazon.awssdk.custom.s3.transfer;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Progress listener for a Transfer.
 * <p>
 * <b>Implementations must be thread-safe.</b>
 */
@SdkPublicApi
@ThreadSafe
public interface TransferProgressListener {
    /**
     * Called when a new progress event is available for a Transfer.
     *
     * @param ctx The context object for the given transfer event.
     */
    void transferProgressEvent(EventContext ctx);

    interface EventContext {
        /**
         * The transfer this listener of associated with.
         */
        Transfer transfer();
    }

    interface Initiated extends EventContext {
        /**
         * The amount of time that has elapsed since the transfer was
         * initiated.
         */
        Duration elapsedTime();
    }

    interface BytesTransferred extends Initiated {
        /**
         * The transfer request for the object whose bytes were transferred.
         */
        TransferObjectRequest objectRequest();

        /**
         * The number of bytes transferred for this event.
         */
        long bytes();

        /**
         * The total length of the object.
         */
        long objectLength();

        /**
         * If the transfer of the given object is complete.
         */
        boolean complete();
    }

    interface Completed extends Initiated {
    }

    interface Cancelled extends Initiated {
    }

    interface Failed extends Initiated {
        /**
         * The error.
         */
        Throwable error();
    }
}

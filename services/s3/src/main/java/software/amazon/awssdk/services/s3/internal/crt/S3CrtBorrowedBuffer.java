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

package software.amazon.awssdk.services.s3.internal.crt;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
final class S3CrtBorrowedBuffer {
    private final ByteBuffer buffer;
    private final long byteCount;
    private final Runnable releaseAction;
    private final LongConsumer readWindowAction;
    private final AtomicBoolean terminal = new AtomicBoolean();

    S3CrtBorrowedBuffer(ByteBuffer buffer,
                        long byteCount,
                        Runnable releaseAction,
                        LongConsumer readWindowAction) {
        Validate.paramNotNull(buffer, "buffer");
        this.releaseAction = Validate.paramNotNull(releaseAction, "releaseAction");
        this.readWindowAction = Validate.paramNotNull(readWindowAction, "readWindowAction");
        Validate.isTrue(byteCount == buffer.remaining(), "byteCount must match the buffer's remaining bytes");
        // Duplicate rather than store the view CRT handed us. CRT returns the lease's own ByteBuffer instance, so
        // reading through it here would advance the position of a buffer CRT may hand out again after the lease is
        // released. The duplicate shares the same memory, which is the entire point, but keeps our cursor private.
        this.buffer = buffer.duplicate();
        this.byteCount = byteCount;
    }

    /**
     * The readable view of the pooled memory.
     *
     * @throws IllegalStateException if the lease has already been released, because the pooled memory may by then have
     *         been handed to another request or returned to the allocator. Reading it would yield whatever now occupies
     *         it, so this fails loudly instead of returning another request's data or freed memory as object content.
     */
    ByteBuffer buffer() {
        if (terminal.get()) {
            throw new IllegalStateException(
                "Borrowed buffer was read after its lease was released. The pooled memory it referenced is no longer "
                + "owned by this request.");
        }
        return buffer;
    }

    long byteCount() {
        return byteCount;
    }

    void consumed() {
        if (terminal.compareAndSet(false, true)) {
            releaseAction.run();
            readWindowAction.accept(byteCount);
        }
    }

    void discard() {
        if (terminal.compareAndSet(false, true)) {
            releaseAction.run();
        }
    }
}

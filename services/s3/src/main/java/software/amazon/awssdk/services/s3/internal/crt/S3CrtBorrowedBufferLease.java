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

/**
 * A lease of a CRT pooled buffer.
 *
 * <p>{@link #consumed()} releases the buffer and credits its bytes to the CRT read window; {@link #discard()} only releases
 * it. The first terminal action invalidates {@link #buffer()}.
 */
@SdkInternalApi
public final class S3CrtBorrowedBufferLease {
    private final ByteBuffer buffer;
    private final long byteCount;
    private final Runnable releaseAction;
    private final LongConsumer readWindowAction;
    private final AtomicBoolean terminal = new AtomicBoolean();

    S3CrtBorrowedBufferLease(ByteBuffer buffer,
                              Runnable releaseAction,
                              LongConsumer readWindowAction) {
        this.releaseAction = releaseAction;
        this.readWindowAction = readWindowAction;
        this.buffer = buffer;
        this.byteCount = buffer.remaining();
    }

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

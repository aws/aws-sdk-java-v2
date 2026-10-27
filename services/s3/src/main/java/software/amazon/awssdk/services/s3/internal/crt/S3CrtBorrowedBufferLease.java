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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * A lease of a CRT borrowed buffer.
 *
 * <p>{@link #release()} returns the buffer to CRT, and later reads throw.
 */
@SdkInternalApi
public final class S3CrtBorrowedBufferLease {
    private final ByteBuffer buffer;
    private final long byteCount;
    private final Runnable releaseAction;
    private final AtomicBoolean released = new AtomicBoolean();

    S3CrtBorrowedBufferLease(ByteBuffer buffer, Runnable releaseAction) {
        this.releaseAction = releaseAction;
        this.buffer = buffer;
        this.byteCount = buffer.remaining();
    }

    int copyTo(byte[] destination, int offset, int length) {
        Validate.validState(!released.get(), "Borrowed buffer was read after its lease was released.");
        int count = Math.min(buffer.remaining(), length);
        buffer.get(destination, offset, count);
        return count;
    }

    boolean hasRemaining() {
        Validate.validState(!released.get(), "Borrowed buffer was read after its lease was released.");
        return buffer.hasRemaining();
    }

    long byteCount() {
        return byteCount;
    }

    void release() {
        if (released.compareAndSet(false, true)) {
            releaseAction.run();
        }
    }
}

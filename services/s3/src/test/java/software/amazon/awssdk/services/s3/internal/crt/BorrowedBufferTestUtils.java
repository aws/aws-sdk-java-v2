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

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;

final class BorrowedBufferTestUtils {
    private BorrowedBufferTestUtils() {
    }

    /**
     * Returns a mock that, like CRT's buffer, runs releaseAction on the first close only and throws from
     * asByteBuffer() after close.
     */
    static S3BorrowedBuffer borrowedBuffer(ByteBuffer data, Runnable releaseAction) {
        S3BorrowedBuffer buffer = mock(S3BorrowedBuffer.class);
        AtomicBoolean closed = new AtomicBoolean();
        when(buffer.asByteBuffer()).thenAnswer(invocation -> {
            if (closed.get()) {
                throw new IllegalStateException("S3BorrowedBuffer has been closed");
            }
            return data;
        });
        doAnswer(invocation -> {
            if (closed.compareAndSet(false, true)) {
                releaseAction.run();
            }
            return null;
        }).when(buffer).close();
        return buffer;
    }
}

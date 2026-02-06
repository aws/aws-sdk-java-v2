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

package software.amazon.awssdk.core.internal.metrics;

import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.io.SdkFilterInputStream;

/**
 * An input stream that tracks the number of bytes read from it. When the HTTP client reads from this stream to send the request
 * body, we count those bytes as "written" to the service.
 */
@SdkInternalApi
public final class BytesWrittenTrackingInputStream extends SdkFilterInputStream {
    private final RequestBodyMetrics metrics;

    public BytesWrittenTrackingInputStream(InputStream in, RequestBodyMetrics metrics) {
        super(in);
        this.metrics = metrics;
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read >= 0) {
            long now = System.nanoTime();
            recordFirstByteWritten(now);
            metrics.bytesWritten().incrementAndGet();
            metrics.lastByteWrittenNanoTime().set(now);
        }
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read > 0) {
            long now = System.nanoTime();
            recordFirstByteWritten(now);
            metrics.bytesWritten().addAndGet(read);
            metrics.lastByteWrittenNanoTime().set(now);
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        if (skipped > 0) {
            long now = System.nanoTime();
            recordFirstByteWritten(now);
            metrics.bytesWritten().addAndGet(skipped);
            metrics.lastByteWrittenNanoTime().set(now);
        }
        return skipped;
    }

    private void recordFirstByteWritten(long time) {
        metrics.firstByteWrittenNanoTime().compareAndSet(0, time);
    }
}

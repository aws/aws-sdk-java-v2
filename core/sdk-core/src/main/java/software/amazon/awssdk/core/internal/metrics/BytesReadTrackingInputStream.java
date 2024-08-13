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
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.util.ProgressUpdaterInvoker;
import software.amazon.awssdk.core.io.SdkFilterInputStream;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;

@SdkInternalApi
public final class BytesReadTrackingInputStream extends SdkFilterInputStream implements Abortable {
    private final Abortable abortableIs;
    private final AtomicLong bytesRead;
    private final ProgressUpdaterInvoker progressUpdaterInvoker;

    public BytesReadTrackingInputStream(AbortableInputStream in, AtomicLong bytesRead,
                                        ProgressUpdaterInvoker progressUpdaterInvoker) {
        super(in);
        this.abortableIs = in;
        this.bytesRead = bytesRead;
        this.progressUpdaterInvoker = progressUpdaterInvoker;
    }

    public long bytesRead() {
        return bytesRead.get();
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        updateBytesRead(read);
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        updateBytesRead(read);
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        updateBytesRead(skipped);
        return skipped;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int read = super.read(b);
        updateBytesRead(read);
        return read;
    }

    private void updateBytesRead(long read) {
        if (read > 0) {
            bytesRead.addAndGet(read);

            if (progressUpdaterInvoker != null) {
                progressUpdaterInvoker.incrementBytesTransferred(read);
            }
        }
    }

    @Override
    public void abort() {
        abortableIs.abort();
    }
}

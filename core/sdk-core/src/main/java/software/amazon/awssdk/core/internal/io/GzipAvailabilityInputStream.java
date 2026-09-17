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

package software.amazon.awssdk.core.internal.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Wraps a response body so {@code available()} never returns {@code 0} for gzip content while the stream is open.
 * {@link java.util.zip.GZIPInputStream} treats a transient {@code 0} from {@code available()} at a member boundary
 * as end of stream and stops, truncating concatenated gzip. Gzip is detected passively from the leading bytes
 * ({@code 1f 8b 08}); non-gzip streams keep honest {@code available()}.
 */
@SdkInternalApi
public final class GzipAvailabilityInputStream extends FilterInputStream implements Releasable {

    private static final int GZIP_MAGIC_1 = 0x1f;
    private static final int GZIP_MAGIC_2 = 0x8b;
    private static final int GZIP_METHOD_DEFLATE = 0x08;
    private static final int HEADER_LENGTH = 3;

    private final byte[] header = new byte[HEADER_LENGTH];
    private volatile int headerLen;
    private volatile boolean classified;
    private volatile boolean gzipDetected;
    private volatile boolean eof;
    private volatile boolean closed;

    private int markHeaderLen;
    private boolean markClassified;
    private boolean markGzipDetected;
    private boolean markEof;
    private boolean marked;

    public GzipAvailabilityInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b == -1) {
            eof = true;
        } else {
            if (eof) {
                eof = false;
            }
            observe((byte) b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = in.read(b, off, len);
        if (n == -1) {
            eof = true;
        } else if (n > 0) {
            if (eof) {
                eof = false;
            }
            observe(b, off, n);
        }
        return n;
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            return 0;
        }
        int available = in.available();
        return available == 0 && gzipDetected && !eof ? 1 : available;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = in.skip(n);
        if (skipped > 0) {
            classified = true;
        }
        return skipped;
    }

    @Override
    public synchronized void mark(int readlimit) {
        markHeaderLen = headerLen;
        markClassified = classified;
        markGzipDetected = gzipDetected;
        markEof = eof;
        marked = true;
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
        if (marked) {
            headerLen = markHeaderLen;
            classified = markClassified;
            gzipDetected = markGzipDetected;
            eof = markEof;
        } else {
            headerLen = 0;
            classified = false;
            gzipDetected = false;
            eof = false;
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        in.close();
    }

    @Override
    public void release() {
        IoUtils.closeQuietly(this, null);
        if (in instanceof Releasable) {
            ((Releasable) in).release();
        }
    }

    private void observe(byte[] b, int off, int len) {
        if (classified) {
            return;
        }
        for (int i = 0; i < len && !classified; i++) {
            observe(b[off + i]);
        }
    }

    private void observe(byte b) {
        if (classified) {
            return;
        }
        int len = headerLen;
        header[len] = b;
        headerLen = len + 1;
        if (headerLen == HEADER_LENGTH) {
            classified = true;
            gzipDetected = (header[0] & 0xff) == GZIP_MAGIC_1
                           && (header[1] & 0xff) == GZIP_MAGIC_2
                           && (header[2] & 0xff) == GZIP_METHOD_DEFLATE;
        }
    }
}

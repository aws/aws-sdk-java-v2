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

import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.io.SdkInputStream;
import software.amazon.awssdk.utils.Logger;

/**
 * A wrapper of InputStream that implements streaming in chunks.
 */
@SdkInternalApi
public abstract class AwsChunkedInputStream extends SdkInputStream {
    public static final int DEFAULT_CHUNK_SIZE = 128 * 1024;
    protected static final int SKIP_BUFFER_SIZE = 256 * 1024;
    protected static final Logger log = Logger.loggerFor(AwsChunkedInputStream.class);
    protected InputStream is;
    /**
     * Iterator on the current chunk.
     */
    protected ChunkContentIterator currentChunkIterator;

    /**
     * Iterator on the buffer of the underlying stream,
     * Null if the wrapped stream is marksupported,
     * otherwise it will be initialized when this wrapper is marked.
     */
    protected UnderlyingStreamBuffer underlyingStreamBuffer;
    protected boolean isAtStart = true;
    protected boolean isTerminating = false;

    @Override
    public int read() throws IOException {
        byte[] tmp = new byte[1];
        int count = read(tmp, 0, 1);
        if (count > 0) {
            log.debug(() -> "One byte read from the stream.");
            int unsignedByte = (int) tmp[0] & 0xFF;
            return unsignedByte;
        } else {
            return count;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long remaining = n;
        int toskip = (int) Math.min(SKIP_BUFFER_SIZE, n);
        byte[] temp = new byte[toskip];
        while (remaining > 0) {
            int count = read(temp, 0, toskip);
            if (count < 0) {
                break;
            }
            remaining -= count;
        }
        return n - remaining;
    }

    /**
     * @see InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    protected InputStream getWrappedInputStream() {
        return is;
    }
}

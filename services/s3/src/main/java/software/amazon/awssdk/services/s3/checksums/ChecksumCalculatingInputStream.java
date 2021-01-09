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

package software.amazon.awssdk.services.s3.checksums;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.SdkChecksum;

@SdkInternalApi
public class ChecksumCalculatingInputStream extends FilterInputStream {

    private final SdkChecksum checkSum;
    private final InputStream inputStream;
    private boolean endOfStream = false;

    /**
     * Creates an input stream using the specified Checksum.
     *
     * @param in    the input stream to read
     * @param cksum the Checksum implementation to use for computing the checksum
     */
    public ChecksumCalculatingInputStream(final InputStream in, final SdkChecksum cksum) {
        super(in);
        inputStream = in;
        checkSum = cksum;
    }

    /**
     * Reads from the underlying stream. If the end of the stream is reached, the
     * running checksum will be appended a byte at a time (1 per read call).
     *
     * @return byte read, if eos has been reached, -1 will be returned.
     */
    @Override
    public int read() throws IOException {
        int read = -1;

        if (!endOfStream) {
            read = inputStream.read();

            if (read != -1) {
                checkSum.update(read);
            }

            if (read == -1) {
                endOfStream = true;
            }
        }

        return read;
    }

    /**
     * Reads up to len bytes at a time from the input stream, updates the checksum. If the end of the stream has been reached
     * the checksum will be appended to the last 4 bytes.
     *
     * @param buf    buffer to write into
     * @param off offset in the buffer to write to
     * @param len maximum number of bytes to attempt to read.
     * @return number of bytes written into buf, otherwise -1 will be returned to indicate eos.
     */
    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException();
        }

        int read = -1;

        if (!endOfStream) {
            read = inputStream.read(buf, off, len);

            if (read != -1) {
                checkSum.update(buf, off, read);
            }

            if (read == -1) {
                endOfStream = true;
            }
        }

        return read;
    }

    /**
     * Resets stream state, including the running checksum.
     */
    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
        checkSum.reset();
        endOfStream = false;
    }

    public byte[] getChecksumBytes() {
        return checkSum.getChecksumBytes();
    }
}

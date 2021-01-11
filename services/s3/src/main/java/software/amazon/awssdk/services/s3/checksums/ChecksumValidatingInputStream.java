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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.Abortable;

@SdkInternalApi
public class ChecksumValidatingInputStream extends InputStream implements Abortable {
    private static final int CHECKSUM_SIZE = 16;

    private final SdkChecksum checkSum;
    private final InputStream inputStream;
    private long strippedLength;
    private byte[] streamChecksum = new byte[CHECKSUM_SIZE];
    private long lengthRead = 0;
    // Preserve the computed checksum because some InputStream readers (e.g., java.util.Properties) read more than once at the
    // end of the stream.
    private Integer computedChecksum;

    /**
     * Creates an input stream using the specified Checksum, input stream, and length.
     *
     * @param in the input stream
     * @param cksum the Checksum implementation
     * @param streamLength the total length of the expected stream (including the extra 4 bytes on the end).
     */
    public ChecksumValidatingInputStream(InputStream in, SdkChecksum cksum, long streamLength) {
        inputStream = in;
        checkSum = cksum;
        this.strippedLength = streamLength - CHECKSUM_SIZE;
    }

    /**
     * Reads one byte at a time from the input stream, updates the checksum. If the end of the stream has been reached
     * the checksum will be compared to the stream's checksum amd a SdkClientException will be thrown.
     *
     * @return byte read, if a read happened, otherwise -1 will be returned to indicate eos.
     */
    @Override
    public int read() throws IOException {
        int read = inputStream.read();

        if (read != -1 && lengthRead < strippedLength) {
            checkSum.update(read);
        }

        if (read != -1) {
            lengthRead++;
        }

        if (read != -1 && lengthRead == strippedLength) {
            int byteRead = -1;
            byteRead = inputStream.read();

            while (byteRead != -1 && lengthRead < strippedLength + CHECKSUM_SIZE) {
                int index = Math.min((int) (lengthRead - strippedLength), CHECKSUM_SIZE - 1);
                streamChecksum[index] = (byte) byteRead;
                lengthRead++;
                byteRead = inputStream.read();
            }
        }

        if (read == -1) {
            validateAndThrow();
        }

        return read;
    }

    /**
     * Reads up to len bytes at a time from the input stream, updates the checksum. If the end of the stream has been reached
     * the checksum will be compared to the stream's checksum amd a SdkClientException will be thrown.
     *
     * @param buf buffer to write into
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

        if (lengthRead < strippedLength) {
            long maxRead = Math.min(Integer.MAX_VALUE, strippedLength - lengthRead);
            int maxIterRead = (int) Math.min(maxRead, len);

            read = inputStream.read(buf, off, maxIterRead);

            int toUpdate = (int) Math.min(strippedLength - lengthRead, read);

            if (toUpdate > 0) {
                checkSum.update(buf, off, toUpdate);
            }

            lengthRead += read >= 0 ? read : 0;
        }

        if (lengthRead >= strippedLength) {
            int byteRead = 0;

            while ((byteRead = inputStream.read()) != -1) {
                int index = Math.min((int) (lengthRead - strippedLength), CHECKSUM_SIZE - 1);
                streamChecksum[index] = (byte) byteRead;
                lengthRead++;
            }

            if (read == -1) {
                validateAndThrow();
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
        lengthRead = 0;

        for (int i = 0; i < CHECKSUM_SIZE; i++) {
            streamChecksum[i] = 0;
        }
    }

    @Override
    public void abort() {
        if (inputStream instanceof Abortable) {
            ((Abortable) inputStream).abort();
        }
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    /**
     * Gets the stream's checksum as an integer.
     *
     * @return checksum.
     */
    public int getStreamChecksum() {
        ByteBuffer bb = ByteBuffer.wrap(streamChecksum);
        return bb.getInt();
    }

    private void validateAndThrow() {
        int streamChecksumInt = getStreamChecksum();
        if (computedChecksum == null) {
            computedChecksum = ByteBuffer.wrap(checkSum.getChecksumBytes()).getInt();
        }

        if (streamChecksumInt != computedChecksum) {
            throw SdkClientException.builder().message(
                String.format("Data read has a different checksum than expected. Was %d, but expected %d",
                              computedChecksum, streamChecksumInt)).build();
        }
    }

}

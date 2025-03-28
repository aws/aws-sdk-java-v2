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

import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@code InputStream} that is aware of its length. This class enforces that we sent exactly the number of bytes equal to
 * the input length. If the wrapped stream has more bytes than the expected length, it will be truncated to length. If the stream
 * has less bytes (i.e. reaches EOF) before the expected length is reached, it will throw {@code IOException}.
 */
@SdkInternalApi
public class SdkLengthAwareInputStream extends FilterInputStream {
    private static final Logger LOG = Logger.loggerFor(SdkLengthAwareInputStream.class);
    private long length;
    private long remaining;

    public SdkLengthAwareInputStream(InputStream in, long length) {
        super(in);
        this.length = Validate.isNotNegative(length, "length");
        this.remaining = this.length;
    }

    @Override
    public int read() throws IOException {
        if (!hasMoreBytes()) {
            LOG.debug(() -> String.format("Specified InputStream length of %d has been reached. Returning EOF.", length));
            return -1;
        }

        int read = super.read();

        if (read != -1) {
            remaining--;
        }

        // EOF, ensure we've read the number of expected bytes
        if (read == -1 && remaining > 0) {
            throw new IllegalStateException("The request content has fewer bytes than the "
                                            + "specified "
                                            + "content-length: " + length + " bytes.");
        }
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!hasMoreBytes()) {
            LOG.debug(() -> String.format("Specified InputStream length of %d has been reached. Returning EOF.", length));
            return -1;
        }

        int readLen = Math.min(len, saturatedCast(remaining));

        int read = super.read(b, off, readLen);
        if (read != -1) {
            remaining -= read;
        }

        // EOF, ensure we've read the number of expected bytes
        if (read == -1 && remaining > 0) {
            throw new IllegalStateException("The request content has fewer bytes than the "
                                            + "specified "
                                            + "content-length: " + length + " bytes.");
        }

        return read;
    }

    @Override
    public long skip(long requestedBytesToSkip) throws IOException {
        requestedBytesToSkip = Math.min(requestedBytesToSkip, remaining);
        long skippedActual = super.skip(requestedBytesToSkip);
        remaining -= skippedActual;
        return skippedActual;
    }

    @Override
    public int available() throws IOException {
        int streamAvailable = super.available();
        return Math.min(streamAvailable, saturatedCast(remaining));
    }

    @Override
    public void mark(int readlimit) {
        super.mark(readlimit);
        // mark() causes reset() to change the stream's position back to the current position. Therefore, when reset() is called,
        // the new length of the stream will be equal to the current value of 'remaining'.
        length = remaining;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        remaining = length;
    }

    public long remaining() {
        return remaining;
    }

    private boolean hasMoreBytes() {
        return remaining > 0;
    }
}

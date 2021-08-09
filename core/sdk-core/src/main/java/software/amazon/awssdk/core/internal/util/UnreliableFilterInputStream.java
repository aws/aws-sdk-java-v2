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

package software.amazon.awssdk.core.internal.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.util.json.JacksonUtils;

/**
 * An internal class used solely for the purpose of testing via failure
 * injection.
 */
@SdkInternalApi
public class UnreliableFilterInputStream extends FilterInputStream {
    // True to throw a FakeIOException; false to throw a RuntimeException
    private final boolean isFakeIoException;
    /**
     * Max number of errors that can be triggered.
     */
    private int maxNumErrors = 1;
    /**
     * Current number of errors that have been triggered.
     */
    private int currNumErrors;
    private int bytesReadBeforeException = 100;
    private int marked;
    private int position;
    private int resetCount; // number of times the reset method has been called
    /**
     * used to control whether an exception would be thrown based on the reset
     * recurrence; not applicable if set to zero. For example, if
     * resetIntervalBeforeException == n, the exception can only be thrown
     * before the n_th reset (or after the n_th minus 1 reset), 2n_th reset (or
     * after the 2n_th minus 1) reset), etc.
     */
    private int resetIntervalBeforeException;

    public UnreliableFilterInputStream(InputStream in, boolean isFakeIoException) {
        super(in);
        this.isFakeIoException = isFakeIoException;
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read != -1) {
            position++;
        }
        triggerError();
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        triggerError();
        int read = super.read(b, off, len);
        position += read;
        triggerError();
        return read;
    }

    @Override
    public void mark(int readlimit) {
        super.mark(readlimit);
        marked = position;
    }

    @Override
    public void reset() throws IOException {
        resetCount++;
        super.reset();
        position = marked;
    }

    private void triggerError() throws FakeIoException {
        if (currNumErrors >= maxNumErrors) {
            return;
        }

        if (position >= bytesReadBeforeException) {
            if (resetIntervalBeforeException > 0
                && resetCount % resetIntervalBeforeException != (resetIntervalBeforeException - 1)) {
                return;
            }
            currNumErrors++;
            if (isFakeIoException) {
                throw new FakeIoException("Fake IO error " + currNumErrors
                                          + " on UnreliableFileInputStream: " + this);
            } else {
                throw new RuntimeException("Injected runtime error " + currNumErrors
                                           + " on UnreliableFileInputStream: " + this);
            }
        }
    }

    public int getCurrNumErrors() {
        return currNumErrors;
    }

    public int getMaxNumErrors() {
        return maxNumErrors;
    }

    public UnreliableFilterInputStream withMaxNumErrors(int maxNumErrors) {
        this.maxNumErrors = maxNumErrors;
        return this;
    }

    public UnreliableFilterInputStream withBytesReadBeforeException(
            int bytesReadBeforeException) {
        this.bytesReadBeforeException = bytesReadBeforeException;
        return this;
    }

    public int getBytesReadBeforeException() {
        return bytesReadBeforeException;
    }

    /**
     * @param resetIntervalBeforeException
     *            used to control whether an exception would be thrown based on
     *            the reset recurrence; not applicable if set to zero. For
     *            example, if resetIntervalBeforeException == n, the exception
     *            can only be thrown before the n_th reset (or after the n_th
     *            minus 1 reset), 2n_th reset (or after the 2n_th minus 1)
     *            reset), etc.
     */
    public UnreliableFilterInputStream withResetIntervalBeforeException(
            int resetIntervalBeforeException) {
        this.resetIntervalBeforeException = resetIntervalBeforeException;
        return this;
    }

    public int getResetIntervalBeforeException() {
        return resetIntervalBeforeException;
    }

    public int getMarked() {
        return marked;
    }

    public int getPosition() {
        return position;
    }

    public boolean isFakeIoException() {
        return isFakeIoException;
    }

    public int getResetCount() {
        return resetCount;
    }

    @Override
    public String toString() {
        return JacksonUtils.toJsonString(this);
    }
}

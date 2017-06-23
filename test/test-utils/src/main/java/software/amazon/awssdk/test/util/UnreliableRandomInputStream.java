/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.test.util;

import java.io.IOException;
import software.amazon.awssdk.utils.Logger;

/**
 * Subclass of RandomInputStream that, in addition to spitting out a set length
 * of random characters, throws an IOException. Intended for testing error
 * recovery in the client library.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class UnreliableRandomInputStream extends RandomInputStream {
    private static final Logger log = Logger.loggerFor(UnreliableRandomInputStream.class);
    private static final boolean DEBUG = false;
    /** True if this stream has already triggered an exception. */
    private boolean hasTriggeredAnException = false;

    /**
     * Constructs a new unreliable random data input stream of the specified
     * number of bytes.
     *
     * @param lengthInBytes
     *            The number of bytes of data contained in the new stream.
     */
    public UnreliableRandomInputStream(long lengthInBytes) {
        super(lengthInBytes);
    }

    /**
     * @see software.amazon.awssdk.test.util.RandomInputStream#read()
     */
    @Override
    public int read() throws IOException {
        triggerException();
        return super.read();
    }

    /*
     * If we're more than half way through the bogus data, and we
     * haven't fired an exception yet, go ahead and fire one.
     */
    private void triggerException() throws IOException {
        if (remainingBytes <= (lengthInBytes / 2) && !hasTriggeredAnException) {
            hasTriggeredAnException = true;
            final String msg = "UnreliableBogusInputStream fired an IOException after reading " + getBytesRead() + " bytes.";
            if (DEBUG) {
                log.error(() -> msg);
            }
            throw new IOException(msg);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        triggerException();
        int read = super.read(b, off, len);
        if (DEBUG) {
            log.debug(() -> "read=" + read + ", off=" + off + ", len=" + len + ", b.length=" + b.length);
        }
        return read;
    }
}

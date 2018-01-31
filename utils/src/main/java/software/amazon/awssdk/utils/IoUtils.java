/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for IO operations.
 */
public final class IoUtils {

    private static final int BUFFER_SIZE = 1024 * 4;
    private static final Logger DEFAULT_LOG = LoggerFactory.getLogger(IoUtils.class);

    private IoUtils() {
    }

    /**
     * Reads and returns the rest of the given input stream as a byte array.
     * Caller is responsible for closing the given input stream.
     */
    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] b = new byte[BUFFER_SIZE];
            int n = 0;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        } finally {
            output.close();
        }
    }

    /**
     * Reads and returns the rest of the given input stream as a string.
     * Caller is responsible for closing the given input stream.
     */
    public static String toString(InputStream is) throws IOException {
        return new String(toByteArray(is), StandardCharsets.UTF_8);
    }

    /**
     * Closes the given Closeable quietly.
     * @param is the given closeable
     * @param log logger used to log any failure should the close fail
     */
    public static void closeQuietly(AutoCloseable is, Logger log) {
        if (is != null) {
            try {
                is.close();
            } catch (Exception ex) {
                Logger logger = log == null ? DEFAULT_LOG : log;
                if (logger.isDebugEnabled()) {
                    logger.debug("Ignore failure in closing the Closeable", ex);
                }
            }
        }
    }

    /**
     * Closes the given Closeable quietly.
     * @param is the given closeable
     * @param log logger used to log any failure should the close fail
     */
    public static void closeIfCloseable(Object maybeCloseable, Logger log) {
        if (maybeCloseable instanceof AutoCloseable) {
            IoUtils.closeQuietly((AutoCloseable) maybeCloseable, log);
        }
    }

    /**
     * Copies all bytes from the given input stream to the given output stream.
     * Caller is responsible for closing the streams.
     *
     * @throws IOException
     *             if there is any IO exception during read or write.
     */
    public static long copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while ((n = in.read(buf)) > -1) {
            out.write(buf, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Read all remaining data in the stream.
     *
     * @param in InputStream to read.
     */
    public static void drainInputStream(InputStream in) {
        try {
            while (in.read() != -1) {
                // Do nothing.
            }
        } catch (IOException ignored) {
            // Stream may be self closed by HTTP client so we ignore any failures.
        }
    }
}

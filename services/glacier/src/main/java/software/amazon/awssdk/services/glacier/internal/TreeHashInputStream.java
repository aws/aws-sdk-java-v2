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

package software.amazon.awssdk.services.glacier.internal;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.runtime.io.SdkDigestInputStream;
import software.amazon.awssdk.runtime.io.SdkFilterInputStream;
import software.amazon.awssdk.services.glacier.TreeHashGenerator;

/**
 * Filter input stream that enables tree hash computation on the bytes that
 * it streams.
 */
public class TreeHashInputStream extends SdkFilterInputStream {

    private static final int MB = 1024 * 1024;
    /**
     * The private DigestInputStream we use to wrap the source input stream and
     * compute a client-side checksum for every part of the message.
     */
    private final DigestInputStream digestInputStream;

    /**
     * The checksums of megabyte-sized parts of the bytes streamed.
     */
    private final List<byte[]> checksums = new ArrayList<byte[]>();

    private boolean closed = false;
    private int byteOffset = 0;

    public TreeHashInputStream(InputStream in) throws NoSuchAlgorithmException {
        super(new SdkDigestInputStream(in, MessageDigest.getInstance("SHA-256")));
        this.digestInputStream = (DigestInputStream) super.in;
    }

    /**
     * Returns the list of one-megabyte checksums for this input stream. Only
     * valid once the stream has been closed.
     *
     * @throws IOException
     *             If the stream hasn't been closed.
     */
    public List<byte[]> getChecksums() throws IOException {
        if (!closed) {
            throw new IOException("Stream must be closed before getting checksums");
        }
        return Collections.unmodifiableList(checksums);
    }

    public String getTreeHash() throws IOException {
        if (!closed) {
            throw new IOException("Stream must be closed before getting the tree hash");
        }
        return TreeHashGenerator.calculateTreeHash(getChecksums());
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read >= 0) {
            byteOffset++;
        }

        digestPart();
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int totalBytesRead = 0;
        int lastBytesRead = 0;
        while (totalBytesRead < len && lastBytesRead > -1) {
            int bytesToRead = Math.min(MB - byteOffset, len - totalBytesRead);

            lastBytesRead = super.read(b, off + totalBytesRead, bytesToRead);
            if (lastBytesRead > 0) {
                totalBytesRead += lastBytesRead;
                byteOffset += lastBytesRead;
            }

            digestPart();

            if (lastBytesRead == -1 && totalBytesRead == 0) {
                return -1;
            }
        }

        return totalBytesRead;
    }

    /**
     * Digests the current part of the message, if necessary, and resets digest
     * state.
     */
    private void digestPart() {
        if (byteOffset >= MB) {
            byteOffset = 0;
            checksums.add(digestInputStream.getMessageDigest().digest());
            digestInputStream.getMessageDigest().reset();
        }
    }

    @Override
    public void close() throws IOException {
        super.close();

        closed = true;
        if (byteOffset > 0) {
            checksums.add(digestInputStream.getMessageDigest().digest());
        }
    }
}

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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An input-stream that takes a collection of checksums, and updates each checksum when it reads data.
 */
@SdkInternalApi
public class ChecksumInputStream extends FilterInputStream {

    private final Collection<Checksum> checksums = new ArrayList<>();

    public ChecksumInputStream(InputStream stream, Collection<? extends Checksum> checksums) {
        super(stream);
        this.checksums.addAll(checksums);
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int read = read(b, 0, 1);
        if (read > 0) {
            checksums.forEach(checksum -> checksum.update(b, 0, 1));
        }

        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read > 0) {
            checksums.forEach(checksum -> checksum.update(b, off, read));
        }

        return read;
    }
}


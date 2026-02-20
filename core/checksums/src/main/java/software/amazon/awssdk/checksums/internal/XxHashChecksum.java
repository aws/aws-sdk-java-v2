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

package software.amazon.awssdk.checksums.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.crt.checksums.XXHash;
import software.amazon.awssdk.utils.IoUtils;

@SdkInternalApi
public final class XxHashChecksum implements SdkChecksum {

    private final XXHash xxHash;

    public XxHashChecksum(ChecksumAlgorithm algorithm) {
        if (algorithm == DefaultChecksumAlgorithm.XXHASH64) {
            xxHash = XXHash.newXXHash64();
        } else if (algorithm == DefaultChecksumAlgorithm.XXHASH3) {
            xxHash = XXHash.newXXHash3_64();
        } else if (algorithm == DefaultChecksumAlgorithm.XXHASH128) {
            xxHash = XXHash.newXXHash3_128();
        } else {
            throw new UnsupportedOperationException("Unsupported algorithm: " + algorithm.algorithmId());
        }
    }

    @Override
    public void update(byte[] b) {
        xxHash.update(b);
    }

    @Override
    public byte[] getChecksumBytes() {
        try {
            return xxHash.digest();
        } finally {
            IoUtils.closeQuietlyV2(xxHash, null);
        }
    }

    @Override
    public void update(int b) {
        xxHash.update(b);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        xxHash.update(b, off, len);
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("mark and reset is not supported");
    }

    @Override
    public void mark(int readLimit) {
        throw new UnsupportedOperationException("mark and reset is not supported");
    }

    @Override
    public long getValue() {
        throw new UnsupportedOperationException("Use getChecksumBytes() instead.");
    }
}

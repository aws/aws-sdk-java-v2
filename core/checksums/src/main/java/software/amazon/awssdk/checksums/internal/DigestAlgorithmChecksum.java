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
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.internal.DigestAlgorithm.CloseableMessageDigest;

/**
 * An implementation of {@link SdkChecksum} that uses a {@link DigestAlgorithm}.
 */
@SdkInternalApi
public class DigestAlgorithmChecksum implements SdkChecksum {

    private final DigestAlgorithm algorithm;

    private CloseableMessageDigest digest;

    private CloseableMessageDigest digestLastMarked;

    public DigestAlgorithmChecksum(DigestAlgorithm algorithm) {
        this.algorithm = algorithm;
        this.digest = newDigest();
    }

    private CloseableMessageDigest newDigest() {
        return algorithm.getDigest();
    }

    @Override
    public void update(int b) {
        digest.messageDigest().update((byte) b);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        digest.messageDigest().update(b, off, len);
    }

    @Override
    public long getValue() {
        throw new UnsupportedOperationException("Use getChecksumBytes() instead.");
    }

    @Override
    public void reset() {
        digest.close();
        if (digestLastMarked == null) {
            digest = newDigest();
        } else {
            digest = digestLastMarked;
        }
    }

    @Override
    public byte[] getChecksumBytes() {
        return digest.digest();
    }

    @Override
    public void mark(int readLimit) {
        digestLastMarked = digest.clone();
    }
}

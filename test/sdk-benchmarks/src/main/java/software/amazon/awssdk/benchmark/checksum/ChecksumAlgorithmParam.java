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

package software.amazon.awssdk.benchmark.checksum;

import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.internal.CrcCloneOnMarkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.crt.checksums.CRC32C;

public enum ChecksumAlgorithmParam {
    CRC32(DefaultChecksumAlgorithm.CRC32),
    CRC32C(DefaultChecksumAlgorithm.CRC32C),
    CRC32C_CRT(DefaultChecksumAlgorithm.CRC32C) {
        @Override
        public SdkChecksum createChecksum() {
            return new CrcCloneOnMarkChecksum(new CRC32C());
        }
    },
    CRC64NVME(DefaultChecksumAlgorithm.CRC64NVME),
    SHA1(DefaultChecksumAlgorithm.SHA1),
    SHA256(DefaultChecksumAlgorithm.SHA256),
    SHA512(DefaultChecksumAlgorithm.SHA512),
    XXHASH64(DefaultChecksumAlgorithm.XXHASH64),
    XXHASH3(DefaultChecksumAlgorithm.XXHASH3),
    XXHASH128(DefaultChecksumAlgorithm.XXHASH128);

    private final ChecksumAlgorithm checksumAlgorithm;

    ChecksumAlgorithmParam(ChecksumAlgorithm checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public ChecksumAlgorithm checksumAlgorithm() {
        return checksumAlgorithm;
    }

    public SdkChecksum createChecksum() {
        return SdkChecksum.forAlgorithm(checksumAlgorithm);
    }
}

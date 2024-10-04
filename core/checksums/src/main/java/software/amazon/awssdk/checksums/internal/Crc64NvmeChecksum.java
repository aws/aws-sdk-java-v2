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

import static software.amazon.awssdk.utils.DependencyValidate.requireClass;
import static software.amazon.awssdk.utils.NumericUtils.longToByte;

import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.crt.checksums.CRC64NVME;

/**
 * Implementation of {@link SdkChecksum} to calculate an CRC64NVME checksum.
 */
@SdkInternalApi
public final class Crc64NvmeChecksum extends BaseCrcChecksum {
    private static final String CRT_CRC64NVME_PATH = "software.amazon.awssdk.crt.checksums.CRC64NVME";
    private static final String CRT_MODULE = "software.amazon.awssdk.crt:aws-crt";

    public Crc64NvmeChecksum() {
        super(getCrc64Nvme());
    }

    private static CRC64NVME getCrc64Nvme() {
        requireClass(CRT_CRC64NVME_PATH, CRT_MODULE, "CRC64NVME");
        return new CRC64NVME();
    }

    @Override
    public Checksum cloneChecksum(Checksum checksum) {
        if (checksum instanceof CRC64NVME) {
            return (Checksum) ((CRC64NVME) checksum).clone();
        }

        throw new IllegalStateException("Unsupported checksum");
    }

    @Override
    public byte[] getChecksumBytes() {
        return longToByte(getChecksum().getValue());
    }
}

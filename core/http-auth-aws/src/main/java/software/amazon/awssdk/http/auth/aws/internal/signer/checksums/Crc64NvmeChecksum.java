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

package software.amazon.awssdk.http.auth.aws.internal.signer.checksums;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.longToByte;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.OptionalDependencyLoaderUtil.getCrc64Nvme;

import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.checksums.CRC64NVME;

/**
 * Implementation of {@link SdkChecksum} to calculate an CRC64NVME checksum.
 */
@SdkInternalApi
public final class Crc64NvmeChecksum extends BaseCrcChecksum {

    public Crc64NvmeChecksum() {
        super(getCrc64Nvme());
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

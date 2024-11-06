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

import static software.amazon.awssdk.utils.NumericUtils.longToByte;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;

/**
 * Implementation of {@link SdkChecksum} to calculate an CRC64NVME checksum.
 */
@SdkInternalApi
public final class Crc64NvmeChecksum implements SdkChecksum {

    private final SdkChecksum sdkChecksum;

    public Crc64NvmeChecksum() {
        this.sdkChecksum = CrcChecksumProvider.crc64NvmeCrtImplementation();
    }

    @Override
    public byte[] getChecksumBytes() {
        return longToByte(sdkChecksum.getValue());
    }

    @Override
    public void mark(int readLimit) {
        this.sdkChecksum.mark(readLimit);
    }

    @Override
    public void update(int b) {
        this.sdkChecksum.update(b);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        this.sdkChecksum.update(b, off, len);
    }

    @Override
    public long getValue() {
        return sdkChecksum.getValue();
    }

    @Override
    public void reset() {
        sdkChecksum.reset();
    }
}

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

import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;

/**
 * Implementation of {@link SdkChecksum} to provide a constant checksum.
 */
@SdkInternalApi
public class ConstantChecksum implements SdkChecksum {

    private final String value;

    public ConstantChecksum(String value) {
        this.value = value;
    }

    @Override
    public void update(int b) {
    }

    @Override
    public void update(byte[] b, int off, int len) {
    }

    @Override
    public long getValue() {
        throw new UnsupportedOperationException("Use getChecksumBytes() instead.");
    }

    @Override
    public void reset() {
    }

    @Override
    public byte[] getChecksumBytes() {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void mark(int readLimit) {
    }
}

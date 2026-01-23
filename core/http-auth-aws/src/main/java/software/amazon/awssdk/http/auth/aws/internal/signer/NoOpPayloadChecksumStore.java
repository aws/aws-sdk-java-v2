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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;

@SdkInternalApi
public final class NoOpPayloadChecksumStore implements PayloadChecksumStore {
    private NoOpPayloadChecksumStore() {
    }

    @Override
    public byte[] putChecksumValue(ChecksumAlgorithm algorithm, byte[] checksum) {
        return null;
    }

    @Override
    public byte[] getChecksumValue(ChecksumAlgorithm algorithm) {
        return null;
    }

    @Override
    public boolean containsChecksumValue(ChecksumAlgorithm algorithm) {
        return false;
    }

    public static NoOpPayloadChecksumStore create() {
        return new NoOpPayloadChecksumStore();
    }
}

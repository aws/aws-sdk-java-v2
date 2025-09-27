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

package software.amazon.awssdk.http.auth.spi.signer;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.spi.internal.signer.DefaultPayloadChecksumStore;

/**
 * Storage object for storing computed checksums for a request payload.
 */
@SdkProtectedApi
public interface PayloadChecksumStore {
    /**
     * Store the checksum value computed using the given algorithm.
     *
     * @return The previous value stored for this algorithm or {@code null} if not present.
     */
    byte[] putChecksumValue(ChecksumAlgorithm algorithm, byte[] checksum);

    /**
     * Retrieve the stored checksum value for the given algorithm.
     *
     * @return The checksum value for the given algorithm or {@code null} if not present.
     */
    byte[] getChecksumValue(ChecksumAlgorithm algorithm);

    /**
     * Returns {@code true} if the store contains a checksum value for the given algorithm, {@code false} otherwise.
     */
    boolean containsChecksumValue(ChecksumAlgorithm algorithm);

    /**
     * Returns the default implementation of this interface.
     */
    static PayloadChecksumStore create() {
        return new DefaultPayloadChecksumStore();
    }
}

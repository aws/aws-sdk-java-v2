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

package software.amazon.awssdk.http.auth.spi.internal.signer;

import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;

/**
 * Default implementation of {@link PayloadChecksumStore}.
 */
@SdkInternalApi
@ThreadSafe
public class DefaultPayloadChecksumStore implements PayloadChecksumStore {
    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    @Override
    public byte[] putChecksumValue(ChecksumAlgorithm algorithm, byte[] value) {
        return cache.put(algorithm.algorithmId(), value);
    }

    @Override
    public byte[] getChecksumValue(ChecksumAlgorithm algorithm) {
        return cache.get(algorithm.algorithmId());
    }

    @Override
    public boolean containsChecksumValue(ChecksumAlgorithm algorithm) {
        return cache.containsKey(algorithm.algorithmId());
    }
}

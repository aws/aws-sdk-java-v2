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

package software.amazon.awssdk.core.internal.checksums;

import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.Algorithm;

/**
 * Cache for storing computed payload checksums. Only to be used in the legacy signing paths.
 */
@SdkInternalApi
@SuppressWarnings("deprecation")
public class LegacyPayloadChecksumCache {
    private final ConcurrentHashMap<Algorithm, byte[]> cache = new ConcurrentHashMap<>();

    public byte[] putChecksumValue(Algorithm algorithm, byte[] checksumValue) {
        return cache.put(algorithm, checksumValue);
    }

    public byte[] getChecksumValue(Algorithm algorithm) {
        return cache.get(algorithm);
    }
}

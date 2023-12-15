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

package software.amazon.awssdk.checksums.spi;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * An interface for declaring the implementation of a checksum.
 */
@SdkProtectedApi
public interface ChecksumAlgorithm {
    /**
     * The ID of the checksum algorithm. This is matched against algorithm
     * names used in smithy traits
     * (e.g. "CRC32C" from the aws.protocols#HTTPChecksum smithy trait)
     */
    String algorithmId();
}

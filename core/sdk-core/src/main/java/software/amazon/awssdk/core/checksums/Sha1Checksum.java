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

package software.amazon.awssdk.core.checksums;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.internal.checksums.LegacyDelegatingChecksum;

/**
 * Implementation of {@link SdkChecksum} to calculate a Sha-1 checksum.
 *
 * <p>
 * Implementor notes: this should've been an internal API, but we can't change it now since it's not within the internal
 * subpackage.
 *
 * @deprecated this class is deprecated and subject to removal.
 */
@Deprecated
@SdkInternalApi
public final class Sha1Checksum extends LegacyDelegatingChecksum {

    public Sha1Checksum() {
        super(DefaultChecksumAlgorithm.SHA1);
    }
}

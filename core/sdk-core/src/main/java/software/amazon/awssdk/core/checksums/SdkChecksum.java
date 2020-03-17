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

import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Extension of {@link Checksum} to support checksums and checksum validations used by the SDK that
 * are not provided by the JDK.
 */
@SdkPublicApi
public interface SdkChecksum extends Checksum {

    /**
     * Returns the computed checksum in a byte array rather than the long provided by
     * {@link #getValue()}.
     *
     * @return byte[] containing the checksum
     */
    byte[] getChecksumBytes();

    /**
     * Allows marking a checksum for checksums that support the ability to mark and reset.
     *
     * @param readLimit the maximum limit of bytes that can be read before the mark position becomes invalid.
     */
    void mark(int readLimit);
}

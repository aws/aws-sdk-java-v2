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

import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * Enum that indicates all the checksums supported by Flexible checksums in a Service Request/Response Header.
 */
@SdkPublicApi
public enum Algorithm {

    CRC32C("crc32c", 8),
    CRC32("crc32", 8),
    SHA256("sha256", 44),
    SHA1("sha1", 28),
    ;

    private static final Map<String, Algorithm> VALUE_MAP = EnumUtils.uniqueIndex(Algorithm.class, Algorithm::toString);

    private final String value;
    private final int length;

    Algorithm(String value, int length) {
        this.value = value;
        this.length = length;
    }

    public static Algorithm fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = StringUtils.lowerCase(value);
        Algorithm algorithm = VALUE_MAP.get(normalizedValue);
        if (algorithm == null) {
            throw new IllegalArgumentException("The provided value is not a valid algorithm " + value);
        }

        return algorithm;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Length corresponds to Base64Encoded length for a given Checksum.
     * This is always fixed for a checksum.
     * @return length of base64 Encoded checksum.
     */
    public Integer base64EncodedLength() {
        return this.length;
    }


}

/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.util;

import static software.amazon.awssdk.utils.BinaryUtils.copyBytesFrom;

import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Utilities for converting objects to strings.
 */
@SdkProtectedApi
public class StringConversion {
    private StringConversion() {
    }

    public static String fromInteger(Integer value) {
        return Integer.toString(value);
    }

    public static String fromLong(Long value) {
        return Long.toString(value);
    }

    public static String fromString(String value) {
        return value;
    }

    public static String fromBoolean(Boolean value) {
        return Boolean.toString(value);
    }

    public static String fromFloat(Float value) {
        return Float.toString(value);
    }

    /**
     * Converts the specified instant to an ISO 8601 timestamp string and
     * returns it.
     *
     * @param value
     *            The instant to format as an ISO 8601 timestamp string.
     *
     * @return An ISO 8601 timestamp string created from the specified instant.
     */
    public static String fromInstant(Instant value) {
        return DateUtils.formatIso8601Date(value);
    }

    /**
     * Returns the string representation of the specified double.
     *
     * @param d
     *            The double to represent as a string.
     *
     * @return The string representation of the specified double.
     */
    public static String fromDouble(Double d) {
        return Double.toString(d);
    }

    /**
     * Returns the string representation of the specified Byte.
     *
     * @param b
     *            The Byte to represent as a string.
     *
     * @return The string representation of the specified Byte.
     */
    public static String fromByte(Byte b) {
        return Byte.toString(b);
    }

    /**
     * Base64 encodes the data in the specified byte buffer (from the current
     * position to the buffer's limit) and returns it as a base64 encoded
     * string.
     *
     * @param byteBuffer
     *            The data to base64 encode and return as a string; must not be
     *            null.
     *
     * @return The base64 encoded contents of the specified byte buffer.
     */
    public static String fromByteBuffer(ByteBuffer byteBuffer) {
        return BinaryUtils.toBase64(copyBytesFrom(byteBuffer));
    }

    /**
     * Base64 encodes the data in the specified sdk bytes.
     *
     * @param sdkBytes The data to base64 encode and return as a string; must not be null.
     * @return The base64 encoded contents of the specified bytes.
     */
    public static String fromSdkBytes(SdkBytes sdkBytes) {
        return BinaryUtils.toBase64(sdkBytes.asByteArray());
    }
}

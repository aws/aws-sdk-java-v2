/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import software.amazon.awssdk.annotation.SdkProtectedApi;

/**
 * A Base 64 codec API.
 *
 * See http://www.ietf.org/rfc/rfc4648.txt
 *
 * @author Hanson Char
 */
@SdkProtectedApi
public final class Base64Utils {
    private Base64Utils() {}

    /**
     * Returns a base 64 encoded string of the given bytes. Null inputs will result in a null result.
     */
    public static String encodeAsString(byte... bytes) {
        return bytes == null ? null : new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
    }

    /**
     * Returns a 64 encoded byte array of the given bytes. Null inputs will result in a null result.
     */
    public static byte[] encode(byte[] bytes) {
        return bytes == null ? null : Base64.getEncoder().encode(bytes);
    }

    /**
     * Decodes the given base 64 encoded string. Null inputs will result in a null result.
     */
    public static byte[] decode(String string) {
        return string == null ? null : Base64.getDecoder().decode(string);
    }

    /**
     * Decodes the given base 64 encoded bytes. Null inputs will result in a null result.
     */
    public static byte[] decode(byte[] bytes) {
        return bytes == null ? null : Base64.getDecoder().decode(bytes);
    }
}

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

package software.amazon.awssdk.core.protocol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;

/**
 * Enum of known types of marshalling types. This enum is used to use EnumMap's for fast lookup of traits.
 */
public enum MarshallingKnownType {
    NULL,
    STRING,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    BIG_DECIMAL,
    BOOLEAN,
    INSTANT,
    SDK_BYTES,
    SDK_POJO,
    LIST,
    MAP,
    SHORT,
    BYTE,
    DOCUMENT,
    ;

    public static MarshallingKnownType from(Class<?> clazz) {
        if (clazz == Void.class) {
            return NULL;
        }
        if (clazz == String.class) {
            return STRING;
        }
        if (clazz == Integer.class) {
            return INTEGER;
        }
        if (clazz == Long.class) {
            return LONG;
        }
        if (clazz == Float.class) {
            return FLOAT;
        }
        if (clazz == Double.class) {
            return DOUBLE;
        }
        if (clazz == BigDecimal.class) {
            return BIG_DECIMAL;
        }
        if (clazz == Boolean.class) {
            return BOOLEAN;
        }
        if (clazz == Instant.class) {
            return INSTANT;
        }
        if (clazz == SdkBytes.class) {
            return SDK_BYTES;
        }
        if (clazz == SdkPojo.class) {
            return SDK_POJO;
        }
        if (clazz == List.class) {
            return LIST;
        }
        if (clazz == Map.class) {
            return MAP;
        }
        if (clazz == Short.class) {
            return SHORT;
        }
        if (clazz == Byte.class) {
            return BYTE;
        }
        if (clazz == Document.class) {
            return DOCUMENT;
        }
        return null;
    }
}

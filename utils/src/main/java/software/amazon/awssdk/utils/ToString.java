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

package software.amazon.awssdk.utils;

import java.util.Arrays;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A class to standardize implementations of {@link Object#toString()} across the SDK.
 *
 * <pre>{@code
 * ToString.builder("Person")
 *         .add("name", name)
 *         .add("age", age)
 *         .build();
 * }</pre>
 */
@NotThreadSafe
@SdkProtectedApi
public final class ToString {
    private final StringBuilder result;
    private final int startingLength;

    /**
     * @see #builder(String)
     */
    private ToString(String className) {
        this.result = new StringBuilder(className).append("(");
        this.startingLength = result.length();
    }

    /**
     * Create a to-string result for the given class name.
     */
    public static String create(String className) {
        return className + "()";
    }

    /**
     * Create a to-string result builder for the given class name.
     * @param className The name of the class being toString'd
     */
    public static ToString builder(String className) {
        return new ToString(className);
    }

    /**
     * Add a field to the to-string result.
     * @param fieldName The name of the field. Must not be null.
     * @param field The value of the field. Value is ignored if null.
     */
    public ToString add(String fieldName, Object field) {
        if (field != null) {
            String value;

            if (field.getClass().isArray()) {
                if (field instanceof byte[]) {
                    value = "0x" + BinaryUtils.toHex((byte[]) field);
                } else {
                    value = Arrays.toString((Object[]) field);
                }
            } else {
                value = String.valueOf(field);
            }
            result.append(fieldName).append("=").append(value).append(", ");
        }
        return this;
    }

    /**
     * Convert this result to a string. The behavior of calling other methods after this one is undefined.
     */
    public String build() {
        if (result.length() > startingLength) {
            result.setLength(result.length() - 2);
        }
        return result.append(")").toString();
    }
}

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

package software.amazon.awssdk.protocols.core;

import java.math.BigDecimal;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Converts various types to Strings. Used for Query Param/Header/Path marshalling.
 */
@SdkProtectedApi
public final class ValueToStringConverter {

    /**
     * Interface to convert a type to a String.
     *
     * @param <T> Type to convert.
     */
    @FunctionalInterface
    public interface ValueToString<T> {

        /**
         * Converts the value to a string.
         *
         * @param t Value to convert.
         * @param field {@link SdkField} containing metadata about the member being marshalled.
         * @return String value.
         */
        String convert(T t, SdkField<T> field);
    }

    /**
     * Simple interface to convert a type to a String. Useful for implementations that don't need the {@link SdkField}.
     *
     * @param <T> Type to convert.
     */
    @FunctionalInterface
    public interface SimpleValueToString<T> extends ValueToString<T> {

        @Override
        default String convert(T t, SdkField<T> field) {
            return convert(t);
        }

        /**
         * Converts the value to a string.
         *
         * @param t Value to convert.
         * @return String value.
         */
        String convert(T t);
    }

    /**
     * Identity converter.
     */
    public static final SimpleValueToString<String> FROM_STRING = val -> val;

    public static final SimpleValueToString<Integer> FROM_INTEGER = Object::toString;

    public static final SimpleValueToString<Long> FROM_LONG = Object::toString;

    public static final SimpleValueToString<Float> FROM_FLOAT = Object::toString;

    public static final SimpleValueToString<Double> FROM_DOUBLE = Object::toString;

    public static final SimpleValueToString<BigDecimal> FROM_BIG_DECIMAL = Object::toString;

    /**
     * Marshalls boolean as a literal 'true' or 'false' string.
     */
    public static final SimpleValueToString<Boolean> FROM_BOOLEAN = Object::toString;

    /**
     * Marshalls bytes as a Base64 string.
     */
    public static final SimpleValueToString<SdkBytes> FROM_SDK_BYTES = b -> BinaryUtils.toBase64(b.asByteArray());

    private ValueToStringConverter() {
    }

}

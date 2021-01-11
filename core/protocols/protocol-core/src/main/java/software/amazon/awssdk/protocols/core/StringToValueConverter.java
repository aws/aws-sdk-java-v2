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
 * Converter implementations that transform a String to a specified type.
 */
@SdkProtectedApi
public final class StringToValueConverter {

    /**
     * Interface to convert a String into another type.
     *
     * @param <T> Type to convert to.
     */
    @FunctionalInterface
    public interface StringToValue<T> {

        /**
         * Converts the value to a string.
         *
         * @param s Value to convert from.
         * @param sdkField {@link SdkField} containing metadata about the member being unmarshalled.
         * @return Unmarshalled value.
         */
        T convert(String s, SdkField<T> sdkField);
    }

    /**
     * Simple interface to convert a String to another type. Useful for implementations that don't need the {@link SdkField}.
     *
     * @param <T> Type to convert to.
     */
    @FunctionalInterface
    public interface SimpleStringToValue<T> extends StringToValue<T> {

        @Override
        default T convert(String s, SdkField<T> sdkField) {
            return convert(s);
        }

        /**
         * Converts the value to a string.
         *
         * @param s Value to convert from.
         * @return Unmarshalled value.
         */
        T convert(String s);
    }

    /**
     * Identity converter.
     */
    public static final SimpleStringToValue<String> TO_STRING = val -> val;

    public static final SimpleStringToValue<Integer> TO_INTEGER = Integer::parseInt;

    public static final SimpleStringToValue<Long> TO_LONG = Long::parseLong;

    public static final SimpleStringToValue<Float> TO_FLOAT = Float::parseFloat;

    public static final SimpleStringToValue<Double> TO_DOUBLE = Double::parseDouble;

    public static final SimpleStringToValue<BigDecimal> TO_BIG_DECIMAL = BigDecimal::new;

    public static final SimpleStringToValue<Boolean> TO_BOOLEAN = Boolean::parseBoolean;

    public static final SimpleStringToValue<SdkBytes> TO_SDK_BYTES = StringToValueConverter::toSdkBytes;

    private StringToValueConverter() {
    }

    private static SdkBytes toSdkBytes(String s) {
        return SdkBytes.fromByteArray(BinaryUtils.fromBase64(s));
    }

}

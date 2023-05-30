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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DoubleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.FloatAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal utilities that are used by some {@link AttributeConverter}s in the aid
 * of converting to an {@link AttributeValue} and vice-versa.
 */
@SdkInternalApi
public class ConverterUtils {
    private ConverterUtils() {
    }

    /**
     * Validates that a given Double input is a valid double supported by {@link DoubleAttributeConverter}.
     * @param input
     */
    public static void validateDouble(Double input) {
        Validate.isTrue(!Double.isNaN(input), "NaN is not supported by the default converters.");
        Validate.isTrue(Double.isFinite(input), "Infinite numbers are not supported by the default converters.");
    }

    /**
     * Validates that a given Float input is a valid double supported by {@link FloatAttributeConverter}.
     * @param input
     */
    public static void validateFloat(Float input) {
        Validate.isTrue(!Float.isNaN(input), "NaN is not supported by the default converters.");
        Validate.isTrue(Float.isFinite(input), "Infinite numbers are not supported by the default converters.");
    }

    public static String padLeft(int paddingAmount, int valueToPad) {
        String value = Integer.toString(valueToPad);
        int padding = paddingAmount - value.length();
        StringBuilder result = new StringBuilder(paddingAmount);
        for (int i = 0; i < padding; i++) {
            result.append('0');
        }
        result.append(value);
        return result.toString();
    }

    public static String[] splitNumberOnDecimal(String valueToSplit) {
        int i = valueToSplit.indexOf('.');
        if (i == -1) {
            return new String[] { valueToSplit, "0" };
        } else {
            // Ends with '.' is not supported.
            return new String[] { valueToSplit.substring(0, i), valueToSplit.substring(i + 1) };
        }
    }

    public static LocalDateTime convertFromLocalDate(LocalDate localDate) {
        return LocalDateTime.of(localDate, LocalTime.MIDNIGHT);
    }

}

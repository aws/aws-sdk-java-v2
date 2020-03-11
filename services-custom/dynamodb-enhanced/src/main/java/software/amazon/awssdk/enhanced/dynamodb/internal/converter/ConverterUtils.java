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

    public static String padLeft2(int valueToPad) {
        return valueToPad > 10 ? Integer.toString(valueToPad) : "0" + valueToPad;
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

    public static String padRight(int paddingAmount, String valueToPad) {
        StringBuilder result = new StringBuilder(paddingAmount);
        result.append(valueToPad);
        for (int i = result.length(); i < paddingAmount; i++) {
            result.append('0');
        }
        return result.toString();
    }

    public static String trimNumber(String number) {
        int startInclusive = findTrimInclusiveStart(number, '0', 0);

        if (startInclusive >= number.length()) {
            return "0";
        }

        if (!number.contains(".")) {
            return number.substring(startInclusive);
        }

        int endExclusive = findTrimExclusiveEnd(number, '0', number.length());
        endExclusive = findTrimExclusiveEnd(number, '.', endExclusive);

        if (startInclusive >= endExclusive) {
            return "0";
        }

        String result = number.substring(startInclusive, endExclusive);
        if (result.startsWith(".")) {
            return "0" + result;
        }
        return result;
    }

    private static int findTrimInclusiveStart(String string, char characterToTrim, int startingIndex) {
        int startInclusive = startingIndex;

        while (startInclusive < string.length() && string.charAt(startInclusive) == characterToTrim) {
            ++startInclusive;
        }

        return startInclusive;
    }

    private static int findTrimExclusiveEnd(String string, char characterToTrim, int startingIndex) {
        int endExclusive = startingIndex;

        while (endExclusive > 0 && string.charAt(endExclusive - 1) == characterToTrim) {
            --endExclusive;
        }

        return endExclusive;
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

    public static String[] chunk(String valueToChunk, int... splitSizes) {
        String[] result = new String[splitSizes.length + 1];
        int splitStartInclusive = chunkLeft(valueToChunk, result, splitSizes);

        Validate.isTrue(splitStartInclusive == valueToChunk.length(), "Value size does not match expected chunking scheme.");

        return result;
    }

    public static String[] chunkWithRightOverflow(String valueToChunk, int... splitSizesFromLeft) {
        String[] result = new String[splitSizesFromLeft.length + 1];
        int splitStartInclusive = chunkLeft(valueToChunk, result, splitSizesFromLeft);

        result[splitSizesFromLeft.length] = valueToChunk.substring(splitStartInclusive);

        return result;
    }

    public static String[] chunkWithLeftOverflow(String valueToChunk, int... splitSizesFromRight) {
        try {
            String[] result = new String[splitSizesFromRight.length + 1];
            int splitEndExclusive = valueToChunk.length();

            for (int i = splitSizesFromRight.length - 1; i >= 0; i--) {
                int splitStartInclusive = splitEndExclusive - splitSizesFromRight[i];
                result[i + 1] = valueToChunk.substring(splitStartInclusive, splitEndExclusive);
                splitEndExclusive = splitStartInclusive;
            }

            result[0] = valueToChunk.substring(0, splitEndExclusive);

            return result;
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid format for value.", e);
        }
    }

    private static int chunkLeft(String valueToChunk, String[] result, int[] splitSizes) {
        try {
            int splitStartInclusive = 0;

            for (int i = 0; i < splitSizes.length; i++) {
                int splitEndExclusive = splitStartInclusive + splitSizes[i];
                result[i] = valueToChunk.substring(splitStartInclusive, splitEndExclusive);
                splitStartInclusive = splitEndExclusive;
            }
            return splitStartInclusive;
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid format for value.", e);
        }
    }
}

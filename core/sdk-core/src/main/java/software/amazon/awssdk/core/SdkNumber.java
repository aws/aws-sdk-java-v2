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

package software.amazon.awssdk.core;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * An in-memory representation of Number being given to a service or being returned by a service.
 * This is a SDK representation of a Number. This allows conversion to any desired numeric type by providing constructors
 * as below
 * @see #fromBigDecimal(BigDecimal) to create from a BigDecimal.
 * @see #fromBigInteger(BigInteger) to create from a BigInteger.
 * @see #fromDouble(double) to create from a double
 * @see #fromFloat(float) to create from a float.
 * @see #fromLong(long) to create from a long.
 * @see #fromShort(short) to create from a short.
 * @see #fromInteger(int) to create from an integer.
 * @see #fromString(String)  to create from a Stringl,
 * Thus by doing this, this class is able to preserve arbitary precison of any given number.
 *
 * If {@link SdkNumber} is expected in a particular number format then its corresponding getter methods can be used.
 * Example for a {@link SdkNumber} created with {@link BigDecimal} the
 * @see #fromBigDecimal(BigDecimal) can be used.

 */
@SdkPublicApi
@Immutable
public final class SdkNumber extends Number implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Number numberValue;
    private final String stringValue;

    /**
     * @param value Number value as passed in the from COnstructor.
     * @see #fromBigDecimal(BigDecimal)
     * @see #fromBigInteger(BigInteger)
     * @see #fromDouble(double)
     * @see #fromFloat(float)
     * @see #fromLong(long)
     * @see #fromShort(short)
     * @see #fromInteger(int)
     */
    private SdkNumber(Number value) {
        this.numberValue = value;
        this.stringValue = null;
    }

    /**
     * .
     *
     * @param stringValue String value.
     * @see #fromString(String)
     */
    private SdkNumber(String stringValue) {
        this.stringValue = stringValue;
        this.numberValue = null;
    }

    private static boolean isNumberValueNaN(Number numberValue) {
        return (numberValue instanceof Double && Double.isNaN((double) numberValue)) ||
                (numberValue instanceof Float && Float.isNaN((float) numberValue));
    }

    private static boolean isNumberValueInfinite(Number numberValue) {
        return (numberValue instanceof Double && Double.isInfinite((double) numberValue)) ||
                (numberValue instanceof Float && Float.isInfinite((float) numberValue));
    }

    private static Number valueOf(Number numberValue) {
        Number valueOfInfiniteOrNaN = valueOfInfiniteOrNaN(numberValue);
        return valueOfInfiniteOrNaN != null ? valueOfInfiniteOrNaN : valueInBigDecimal(numberValue);
    }

    private static Number valueOfInfiniteOrNaN(Number numberValue) {
        if (numberValue instanceof Double
                && (Double.isInfinite((double) numberValue) || Double.isNaN((double) numberValue))) {
            return Double.valueOf(numberValue.doubleValue());
        } else if ((numberValue instanceof Float
                && (Float.isInfinite((float) numberValue) || Float.isNaN((float) numberValue)))) {
            return Float.valueOf(numberValue.floatValue());
        } else {
            return null;
        }
    }

    /**
     * This function converts a given number to BigDecimal Number where the caller can convert to an primitive number.
     * This is done to keep the precision.
     *
     * @param numberValue The number value.
     * @return Big Decimal value for the given number.
     */
    private static BigDecimal valueInBigDecimal(Number numberValue) {
        if (numberValue instanceof Double) {
            return BigDecimal.valueOf((double) numberValue);
        } else if (numberValue instanceof Float) {
            return BigDecimal.valueOf((float) numberValue);
        } else if (numberValue instanceof Integer) {
            return new BigDecimal((int) numberValue);
        } else if (numberValue instanceof Short) {
            return new BigDecimal((short) numberValue);
        } else if (numberValue instanceof Long) {
            return BigDecimal.valueOf((Long) numberValue);
        } else if (numberValue instanceof BigDecimal) {
            return (BigDecimal) numberValue;
        } else if (numberValue instanceof BigInteger) {
            return new BigDecimal((BigInteger) numberValue);
        } else {
            return new BigDecimal(numberValue.toString());
        }
    }

    /**
     * Create {@link SdkNumber} from a integer value.
     *
     * @param integerValue Integer value.
     * @return new {@link SdkNumber} for the given int value.
     */
    public static SdkNumber fromInteger(int integerValue) {
        return new SdkNumber(integerValue);
    }

    /**
     * Create {@link SdkNumber} from a BigInteger value.
     *
     * @param bigIntegerValue BigInteger value.
     * @return new {@link SdkNumber} for the given BigInteger value.
     */
    public static SdkNumber fromBigInteger(BigInteger bigIntegerValue) {
        return new SdkNumber(bigIntegerValue);
    }

    /**
     * Create {@link SdkNumber} from a BigDecimal value.
     *
     * @param bigDecimalValue BigInteger value.
     * @return new {@link SdkNumber} for the given BigDecimal value.
     */
    public static SdkNumber fromBigDecimal(BigDecimal bigDecimalValue) {
        Validate.notNull(bigDecimalValue, "BigDecimal cannot be null");
        return new SdkNumber(bigDecimalValue);
    }

    /**
     * Create {@link SdkNumber} from a long Value.
     *
     * @param longValue long value.
     * @return new {@link SdkNumber} for the given long value.
     */
    public static SdkNumber fromLong(long longValue) {
        return new SdkNumber(longValue);
    }

    /**
     * Create {@link SdkNumber} from a double Value.
     *
     * @param doubleValue long value.
     * @return new {@link SdkNumber} for the given double value.
     */
    public static SdkNumber fromDouble(double doubleValue) {
        return new SdkNumber(doubleValue);
    }

    /**
     * Create {@link SdkNumber} from a long Value.
     *
     * @param shortValue long value.
     * @return new {@link SdkNumber} for the given long value.
     */
    public static SdkNumber fromShort(short shortValue) {
        return new SdkNumber(shortValue);
    }

    /**
     * Create {@link SdkNumber} from a float Value.
     *
     * @param floatValue float value.
     * @return new {@link SdkNumber} for the given float value.
     */
    public static SdkNumber fromFloat(float floatValue) {
        return new SdkNumber(floatValue);
    }

    /**
     * Create {@link SdkNumber} from a long Value.
     *
     * @param stringValue String value.
     * @return new {@link SdkNumber} for the given stringValue value.
     */
    public static SdkNumber fromString(String stringValue) {
        return new SdkNumber(stringValue);
    }

    /**
     * Gets the integer value of the  {@link SdkNumber}.
     * If we do a intValue() for {@link SdkNumber} constructed
     * from float, double, long, BigDecimal, BigInteger number type then it
     * may result in loss of magnitude and a loss of precision.
     * The result may lose some of the least significant bits of the value.
     * Precision is not lost while getting a {@link SdkNumber} which was constructed as
     * lower precision number type like short, byte, integer.
     *
     * @return integer value of  {@link SdkNumber} .
     */
    @Override
    public int intValue() {
        return numberValue instanceof Integer ? numberValue.intValue() :
                stringValue != null ? new BigDecimal(stringValue).intValue()
                        : valueOf(numberValue).intValue();
    }

    /**
     * Gets the long value of the  {@link SdkNumber}.
     * If we do a longValue() for {@link SdkNumber} constructed from
     * float, double, BigDecimal, BigInteger number type then it
     * may result in loss of magnitude and a loss of precision.
     * Precision is not lost while getting a {@link SdkNumber} which was constructed from
     * lower precision type like short, byte, integer.
     *
     * @return long value of  {@link SdkNumber}.
     */
    @Override
    public long longValue() {
        return numberValue instanceof Long ? numberValue.longValue() :
                stringValue != null ? new BigDecimal(stringValue).longValue() : valueOf(numberValue).longValue();
    }

    /**
     * Gets the float value of the  {@link SdkNumber}.
     * If we do a floatValue() for {@link SdkNumber} constructed from
     * double, BigDecimal, BigInteger number type then it
     * may result in loss of magnitude and a loss of precision.
     * Precision is not lost while getting a {@link SdkNumber} which was constructed from
     * precision type like short, byte, integer, long.
     *
     * @return long value of  {@link SdkNumber}.
     */
    @Override
    public float floatValue() {
        return numberValue instanceof Float ? numberValue.floatValue() :
                numberValue != null ? valueOf(numberValue).floatValue() : new BigDecimal(stringValue).floatValue();
    }

    /**
     * Gets the double value of the  {@link SdkNumber}.
     * If we do a doubleValue() for {@link SdkNumber} constructed from BigDecimal, BigInteger number type then it
     * may result in loss of magnitude and a loss of precision.
     * Precision is not lost while getting a {@link SdkNumber} which was constructed from
     * precision type like short, byte, integer, long, float.
     *
     * @return long value of  {@link SdkNumber}.
     */
    @Override
    public double doubleValue() {
        return numberValue instanceof Double ? numberValue.doubleValue() :
                numberValue != null ? valueOf(numberValue).doubleValue() :
                        new BigDecimal(stringValue).doubleValue();
    }

    /**
     * Gets the bigDecimalValue of the {@link SdkNumber}.
     * Precision is not lost in this case.
     * However bigDecimalValue cannot be performed on
     * a {{@link SdkNumber}} constructed from Float/Double Nan/Infinity.
     *
     * @return BigDecimal value of  {@link SdkNumber}
     * @throws NumberFormatException Exception in thrown if a {@link SdkNumber} was constructed asNan/Infinte number
     *                               of Double/FLoat type.Since we cannot convert NaN/Infinite numbers to BigDecimal.
     */
    public BigDecimal bigDecimalValue() {

        if (stringValue != null) {
            return new BigDecimal(stringValue);
        }
        if (numberValue instanceof BigDecimal) {
            return (BigDecimal) numberValue;
        }
        if (isNumberValueNaN(numberValue) || isNumberValueInfinite(numberValue)) {
            throw new NumberFormatException("Nan or Infinite Number can not be converted to BigDecimal.");
        } else {
            return valueInBigDecimal(numberValue);
        }
    }

    /**
     * Gets the String value of the  {@link SdkNumber}.
     *
     * @return the stringValue
     */
    public String stringValue() {
        return stringValue != null ? stringValue : numberValue.toString();
    }

    @Override
    public String toString() {
        return stringValue != null ? stringValue : numberValue.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SdkNumber)) {
            return false;
        }
        SdkNumber sdkNumber = (SdkNumber) o;
        return Objects.equals(stringValue(), sdkNumber.stringValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(stringValue());
    }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public class SdkNumberTest {


    public static final String THIRTY_TWO_DIGITS = "100000000000000000000000000000000";

    @Test
    public void integerSdkNumber() {
        final SdkNumber sdkNumber = SdkNumber.fromInteger(-100);
        assertThat(sdkNumber.bigDecimalValue()).isEqualTo(BigDecimal.valueOf(-100));
        assertThat(sdkNumber.longValue()).isEqualTo(-100L);
        assertThat(sdkNumber.doubleValue()).isEqualTo(-100.0);
        assertThat(sdkNumber.floatValue()).isEqualTo(-100.0f);
        assertThat(sdkNumber.shortValue()).isEqualTo((short) -100);
        assertThat(sdkNumber.byteValue()).isEqualTo((byte) -100);
        assertThat(sdkNumber).hasToString("-100");
        assertThat(SdkNumber.fromInteger(-100)).isEqualTo(sdkNumber);
        assertThat(Objects.hashCode(sdkNumber)).isEqualTo(Objects.hashCode(SdkNumber.fromInteger(-100)));
    }

    @Test
    public void longSdkNumber() {
        final SdkNumber sdkNumber = SdkNumber.fromLong(-123456789L);
        assertThat(sdkNumber.longValue()).isEqualTo(-123456789L);
        assertThat(sdkNumber.intValue()).isEqualTo(-123456789);
        assertThat(sdkNumber.bigDecimalValue()).isEqualTo(BigDecimal.valueOf(-123456789));
        assertThat(sdkNumber.doubleValue()).isEqualTo(-123456789.0);
        assertThat(sdkNumber.floatValue()).isEqualTo(-123456789.0f);
        assertThat(sdkNumber.shortValue()).isEqualTo((short) -123456789);
        assertThat(sdkNumber.byteValue()).isEqualTo((byte) -123456789);
        assertThat(sdkNumber).hasToString("-123456789");

        assertThat(SdkNumber.fromLong(-123456789L)).isEqualTo(sdkNumber);
        assertThat(Objects.hashCode(sdkNumber)).isEqualTo(Objects.hashCode(SdkNumber.fromLong(-123456789L)));
    }

    @Test
    public void doubleSdkNumber() {
        final SdkNumber sdkNumber = SdkNumber.fromDouble(-123456789.987654321);
        assertThat(sdkNumber.longValue()).isEqualTo(-123456789L);
        assertThat(sdkNumber.intValue()).isEqualTo(-123456789);
        assertThat(sdkNumber.bigDecimalValue()).isEqualTo(BigDecimal.valueOf(-123456789.987654321));
        assertThat(sdkNumber.doubleValue()).isEqualTo(-123456789.987654321);
        assertThat(sdkNumber.floatValue()).isEqualTo(-123456789.987654321f);
        assertThat(sdkNumber.shortValue()).isEqualTo((short) -123456789);
        assertThat(sdkNumber.byteValue()).isEqualTo((byte) -123456789);
        assertThat(sdkNumber).hasToString("-1.2345678998765433E8");
        assertThat(SdkNumber.fromDouble(-123456789.987654321)).isEqualTo(sdkNumber);
        assertThat(Objects.hashCode(sdkNumber)).isEqualTo(Objects.hashCode(SdkNumber.fromDouble(-123456789.987654321)));
    }

    @Test
    public void shortSdkNumber() {
        final SdkNumber sdkNumber = SdkNumber.fromShort((short) 2);

        assertThat(sdkNumber.longValue()).isEqualTo(2L);
        assertThat(sdkNumber.doubleValue()).isEqualTo(2);
        assertThat(sdkNumber.floatValue()).isEqualTo(2);
        assertThat(sdkNumber.shortValue()).isEqualTo((short) 2);
        assertThat(sdkNumber.byteValue()).isEqualTo((byte) 2);
        assertThat(sdkNumber).hasToString("2");
    }

    @Test
    public void floatSdkNumber() {
        final SdkNumber sdkNumber = SdkNumber.fromFloat(-123456789.987654321f);
        assertThat(sdkNumber.longValue()).isEqualTo((long) -123456789.987654321f);
        assertThat(sdkNumber.intValue()).isEqualTo((int) -123456789.987654321f);
        assertThat(sdkNumber.bigDecimalValue()).isEqualTo(BigDecimal.valueOf(-123456789.987654321f));
        assertThat(sdkNumber.doubleValue()).isEqualTo(-123456789.987654321f);
        assertThat(sdkNumber.floatValue()).isEqualTo(-123456789.987654321f);
        assertThat(sdkNumber.shortValue()).isEqualTo((short) -123456789.987654321f);
        assertThat(sdkNumber.byteValue()).isEqualTo((byte) -123456789.987654321f);
        assertThat(sdkNumber.toString()).isEqualTo("-1.23456792E8")
                .isEqualTo(Float.valueOf(-123456789.987654321f).toString());
    }

    @Test
    public void bigDecimalSdkNumber() {
        final BigDecimal bigDecimalValue = new BigDecimal(-123456789.987654321);
        final SdkNumber sdkNumber = SdkNumber.fromBigDecimal(bigDecimalValue);
        assertThat(sdkNumber.longValue()).isEqualTo(bigDecimalValue.longValue());
        assertThat(sdkNumber.intValue()).isEqualTo(bigDecimalValue.intValue());
        assertThat(sdkNumber.bigDecimalValue()).isEqualTo(new BigDecimal(-123456789.987654321));
        assertThat(sdkNumber.doubleValue()).isEqualByComparingTo(-123456789.987654321);
        assertThat(sdkNumber.floatValue()).isEqualTo(-123456789.987654321f);
        assertThat(sdkNumber.shortValue()).isEqualTo(bigDecimalValue.shortValue());
        assertThat(sdkNumber.byteValue()).isEqualTo(bigDecimalValue.byteValue());
        assertThat(sdkNumber).hasToString(bigDecimalValue.toString());

        assertThat(SdkNumber.fromBigDecimal(new BigDecimal(-123456789.987654321))).isEqualTo(sdkNumber);
        assertThat(Objects.hashCode(sdkNumber)).isEqualTo(Objects.hashCode(SdkNumber.fromBigDecimal(new BigDecimal(-123456789.987654321))));
    }


    @Test
    public void numberFromString() {

        final SdkNumber sdkSmallNumber = SdkNumber.fromString("1");
        assertThat(sdkSmallNumber.longValue()).isEqualTo(1L);
        assertThat(sdkSmallNumber.stringValue()).isEqualTo("1");
        assertThat(sdkSmallNumber.bigDecimalValue()).isEqualTo(new BigDecimal(1));
        assertThat(sdkSmallNumber.shortValue()).isEqualTo((short) 1);
        assertThat(sdkSmallNumber.intValue()).isEqualTo(1);
        final SdkNumber sdkBigDecimalNumber = SdkNumber.fromString(THIRTY_TWO_DIGITS +
                ".123456789000000");
        final BigDecimal bigDecimal = new BigDecimal(THIRTY_TWO_DIGITS
                + ".123456789000000");
        assertThat(sdkBigDecimalNumber.bigDecimalValue()).isEqualTo(bigDecimal);
        assertThat(sdkBigDecimalNumber.longValue()).isEqualTo(bigDecimal.longValue());
        assertThat(sdkBigDecimalNumber.bigDecimalValue()).isEqualTo(bigDecimal);
        assertThat(sdkBigDecimalNumber.intValue()).isEqualTo(bigDecimal.intValue());
        assertThat(sdkBigDecimalNumber.shortValue()).isEqualTo(bigDecimal.shortValue());
        assertThat(sdkBigDecimalNumber.intValue()).isEqualTo(bigDecimal.intValue());
        assertThat(sdkBigDecimalNumber.doubleValue()).isEqualTo(bigDecimal.doubleValue());

        assertThat(SdkNumber.fromString("1")).isEqualTo(sdkSmallNumber);
        assertThat(Objects.hashCode(sdkSmallNumber)).isEqualTo(Objects.hashCode(SdkNumber.fromString("1")));

        assertThat(sdkBigDecimalNumber).isEqualTo(SdkNumber.fromBigDecimal(bigDecimal));
        assertThat(Objects.hashCode(sdkBigDecimalNumber)).isEqualTo(Objects.hashCode(SdkNumber.fromBigDecimal(bigDecimal)));
        assertThat(sdkBigDecimalNumber.equals(sdkBigDecimalNumber)).isTrue();
    }

    @Test
    public void numberFromNaNDouble() {
        final SdkNumber sdkNan = SdkNumber.fromDouble(Double.longBitsToDouble(0x7ff8000000000000L));
        final Double nanDouble = Double.longBitsToDouble(0x7ff8000000000000L);
        assertThat(nanDouble.isNaN()).isTrue();
        assertThatThrownBy(() -> sdkNan.bigDecimalValue()).isInstanceOf(NumberFormatException.class);
        assertEqualitySDKNumberWithNumber(sdkNan, nanDouble);

    }

    @Test
    public void numberFromNaNFloat() {
        final SdkNumber sdkNan = SdkNumber.fromFloat(Float.NaN);
        final Float floatNan = Float.NaN;
        assertThat(floatNan.isNaN()).isTrue();
        assertThatThrownBy(() -> sdkNan.bigDecimalValue()).isInstanceOf(NumberFormatException.class);
        assertEqualitySDKNumberWithNumber(sdkNan, floatNan);
    }

    @Test
    public void numberFromPositiveInfinityDouble() {
        final SdkNumber sdkNan = SdkNumber.fromDouble(Double.POSITIVE_INFINITY);
        final Double positiveInfinity = Double.POSITIVE_INFINITY;
        assertThat(positiveInfinity.isInfinite()).isTrue();
        assertThatThrownBy(() -> sdkNan.bigDecimalValue()).isInstanceOf(NumberFormatException.class);
        assertEqualitySDKNumberWithNumber(sdkNan, positiveInfinity);
    }

    @Test
    public void numberFromNegativeInfinityDouble() {
        final SdkNumber sdkNan = SdkNumber.fromDouble(Double.NEGATIVE_INFINITY);
        final Double positiveInfinity = Double.NEGATIVE_INFINITY;
        assertThat(positiveInfinity.isInfinite()).isTrue();
        assertThatThrownBy(() -> sdkNan.bigDecimalValue()).isInstanceOf(NumberFormatException.class);
        assertEqualitySDKNumberWithNumber(sdkNan, positiveInfinity);
    }

    public void numberFromPositiveInfinityFloat() {
        final SdkNumber sdkNan = SdkNumber.fromFloat(Float.POSITIVE_INFINITY);
        final Float positiveInfinity = Float.POSITIVE_INFINITY;
        assertThat(positiveInfinity.isNaN()).isTrue();
        assertThatThrownBy(() -> sdkNan.bigDecimalValue()).isInstanceOf(NumberFormatException.class);
        assertEqualitySDKNumberWithNumber(sdkNan, positiveInfinity);
    }

    @Test
    public void numberFromNegativeInfinityFLoat() {
        final SdkNumber sdkNan = SdkNumber.fromFloat(Float.NEGATIVE_INFINITY);
        final Float positiveInfinity = Float.NEGATIVE_INFINITY;
        assertThat(positiveInfinity.isInfinite()).isTrue();
        assertThatThrownBy(() -> sdkNan.bigDecimalValue()).isInstanceOf(NumberFormatException.class);
        assertEqualitySDKNumberWithNumber(sdkNan, positiveInfinity);
    }

    private void assertEqualitySDKNumberWithNumber(SdkNumber sdkNan, Number nanDouble) {
        assertThat(sdkNan.longValue()).isEqualTo(nanDouble.longValue());
        assertThat(sdkNan.stringValue()).isEqualTo(nanDouble.toString());
        assertThat(sdkNan.shortValue()).isEqualTo(nanDouble.shortValue());
        assertThat(sdkNan.intValue()).isEqualTo(nanDouble.intValue());
        assertThat(sdkNan).hasToString(nanDouble.toString());
        assertThat(sdkNan.byteValue()).isEqualTo(nanDouble.byteValue());
        assertThat(sdkNan.doubleValue()).isEqualTo(nanDouble.doubleValue());
        assertThat(sdkNan.byteValue()).isEqualTo(nanDouble.byteValue());
    }


    @Test
    public void bigIntegerNumber() {
        final SdkNumber sdkNumber = SdkNumber.fromBigInteger(new BigInteger(THIRTY_TWO_DIGITS));
        BigInteger bigIntegerExpected = new BigInteger(THIRTY_TWO_DIGITS);
        assertEqualitySDKNumberWithNumber(sdkNumber, bigIntegerExpected);
    }

    @Test
    public void sdkNumberNotEqualToPrimitive(){
        assertThat(SdkNumber.fromInteger(2)).isNotEqualTo(2);

    }
}

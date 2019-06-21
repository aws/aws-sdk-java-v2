/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.fromAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.toAttributeValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;

public class NumberAttributeConvertersTest {
    private static String TIIIINY_NUMBER = tiiiinyNumber();
    private static String HUUUUGE_NUMBER = huuuugeNumber();

    @Test
    public void atomicIntegerAttributeConverterBehaves() {
        AtomicIntegerAttributeConverter converter = AtomicIntegerAttributeConverter.create();

        assertThat(toAttributeValue(converter, new AtomicInteger(Integer.MIN_VALUE)).asNumber())
                .isEqualTo(Integer.toString(Integer.MIN_VALUE));
        assertThat(toAttributeValue(converter, new AtomicInteger(-42)).asNumber()).isEqualTo("-42");
        assertThat(toAttributeValue(converter, new AtomicInteger(0)).asNumber()).isEqualTo("0");
        assertThat(toAttributeValue(converter, new AtomicInteger(42)).asNumber()).isEqualTo("42");
        assertThat(toAttributeValue(converter, new AtomicInteger(Integer.MAX_VALUE)).asNumber())
                .isEqualTo(Integer.toString(Integer.MAX_VALUE));

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("1.0")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("1.0")));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(Integer.toString(Integer.MIN_VALUE))))
                .hasValue(Integer.MIN_VALUE);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("-42"))).hasValue(-42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-42"))).hasValue(-42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).hasValue(0);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("42"))).hasValue(42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("42"))).hasValue(42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(Integer.toString(Integer.MAX_VALUE))))
                .hasValue(Integer.MAX_VALUE);
    }

    @Test
    public void atomicLongAttributeConverterBehaves() {
        AtomicLongAttributeConverter converter = AtomicLongAttributeConverter.create();

        assertThat(toAttributeValue(converter, new AtomicLong(Long.MIN_VALUE)).asNumber())
                .isEqualTo(Long.toString(Long.MIN_VALUE));
        assertThat(toAttributeValue(converter, new AtomicLong(-42)).asNumber()).isEqualTo("-42");
        assertThat(toAttributeValue(converter, new AtomicLong(0)).asNumber()).isEqualTo("0");
        assertThat(toAttributeValue(converter, new AtomicLong(42)).asNumber()).isEqualTo("42");
        assertThat(toAttributeValue(converter, new AtomicLong(Long.MAX_VALUE)).asNumber())
                .isEqualTo(Long.toString(Long.MAX_VALUE));

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("1.0")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("1.0")));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(Long.toString(Long.MIN_VALUE))))
                .hasValue(Long.MIN_VALUE);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("-42"))).hasValue(-42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-42"))).hasValue(-42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).hasValue(0);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("42"))).hasValue(42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("42"))).hasValue(42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(Long.toString(Long.MAX_VALUE))))
                .hasValue(Long.MAX_VALUE);
    }

    @Test
    public void bigDecimalAttributeConverterBehaves() {
        BigDecimalAttributeConverter converter = BigDecimalAttributeConverter.create();

        assertThat(toAttributeValue(converter, new BigDecimal(TIIIINY_NUMBER)).asNumber()).isEqualTo(TIIIINY_NUMBER);
        assertThat(toAttributeValue(converter, new BigDecimal("43.0")).asNumber()).isEqualTo("43.0");
        assertThat(toAttributeValue(converter, new BigDecimal("-42.42")).asNumber()).isEqualTo("-42.42");
        assertThat(toAttributeValue(converter, new BigDecimal("-42")).asNumber()).isEqualTo("-42");
        assertThat(toAttributeValue(converter, new BigDecimal("0")).asNumber()).isEqualTo("0");
        assertThat(toAttributeValue(converter, new BigDecimal("42")).asNumber()).isEqualTo("42");
        assertThat(toAttributeValue(converter, new BigDecimal("42.42")).asNumber()).isEqualTo("42.42");
        assertThat(toAttributeValue(converter, new BigDecimal("43.0")).asNumber()).isEqualTo("43.0");
        assertThat(toAttributeValue(converter, new BigDecimal(HUUUUGE_NUMBER)).asNumber()).isEqualTo(HUUUUGE_NUMBER);

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("X")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(TIIIINY_NUMBER)).toString()).isEqualTo(TIIIINY_NUMBER);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-43.0")).toString()).isEqualTo("-43.0");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("-42.42")).toString()).isEqualTo("-42.42");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-42")).toString()).isEqualTo("-42");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0")).toString()).isEqualTo("0");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("42")).toString()).isEqualTo("42");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("42.42")).toString()).isEqualTo("42.42");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("43.0")).toString()).isEqualTo("43.0");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(HUUUUGE_NUMBER)).toString()).isEqualTo(HUUUUGE_NUMBER);
    }

    @Test
    public void bigIntegerAttributeConverterBehaves() {
        BigIntegerAttributeConverter converter = BigIntegerAttributeConverter.create();

        assertThat(toAttributeValue(converter, new BigInteger(TIIIINY_NUMBER)).asNumber()).isEqualTo(TIIIINY_NUMBER);
        assertThat(toAttributeValue(converter, new BigInteger("-42")).asNumber()).isEqualTo("-42");
        assertThat(toAttributeValue(converter, new BigInteger("0")).asNumber()).isEqualTo("0");
        assertThat(toAttributeValue(converter, new BigInteger("42")).asNumber()).isEqualTo("42");
        assertThat(toAttributeValue(converter, new BigInteger(HUUUUGE_NUMBER)).asNumber()).isEqualTo(HUUUUGE_NUMBER);

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("X")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("1.0")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(TIIIINY_NUMBER)).toString()).isEqualTo(TIIIINY_NUMBER);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("-42")).toString()).isEqualTo("-42");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-42")).toString()).isEqualTo("-42");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0")).toString()).isEqualTo("0");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("42")).toString()).isEqualTo("42");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("42")).toString()).isEqualTo("42");
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(HUUUUGE_NUMBER)).toString()).isEqualTo(HUUUUGE_NUMBER);
    }

    @Test
    public void floatAttributeConverterBehaves() {
        FloatAttributeConverter converter = FloatAttributeConverter.create();

        assertFails(() -> toAttributeValue(converter, Float.NEGATIVE_INFINITY));
        assertFails(() -> toAttributeValue(converter, Float.POSITIVE_INFINITY));
        assertFails(() -> toAttributeValue(converter, Float.NaN));

        assertThat(toAttributeValue(converter, -Float.MAX_VALUE).asNumber()).isEqualTo("-3.4028235E38");
        assertThat(Float.parseFloat(toAttributeValue(converter, -42.42f).asNumber())).isCloseTo(-42.42f, offset(1E-10f));
        assertThat(toAttributeValue(converter, -Float.MIN_VALUE).asNumber()).isEqualTo("-1.4E-45");
        assertThat(toAttributeValue(converter, 0f).asNumber()).isEqualTo("0.0");
        assertThat(toAttributeValue(converter, Float.MIN_VALUE).asNumber()).isEqualTo("1.4E-45");
        assertThat(Float.parseFloat(toAttributeValue(converter, 42.42f).asNumber())).isCloseTo(42.42f, offset(1E-10f));
        assertThat(toAttributeValue(converter, Float.MAX_VALUE).asNumber()).isEqualTo("3.4028235E38");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("2E308")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("-2E308")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("NaN")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("1.4E-45")))
                .isCloseTo(Float.MIN_VALUE, offset(1E-10f));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-42.42"))).isCloseTo(-42.42f, offset(1E-10f));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo(0f);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("42.42"))).isCloseTo(42.42f, offset(1E-10f));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("3.4028235E38")))
                .isCloseTo(Float.MAX_VALUE, offset(1E-10f));
    }

    @Test
    public void doubleAttributeConverterBehaves() {
        DoubleAttributeConverter converter = DoubleAttributeConverter.create();

        assertFails(() -> toAttributeValue(converter, Double.NEGATIVE_INFINITY));
        assertFails(() -> toAttributeValue(converter, Double.POSITIVE_INFINITY));
        assertFails(() -> toAttributeValue(converter, Double.NaN));

        assertThat(toAttributeValue(converter, -Double.MAX_VALUE).asNumber()).isEqualTo("-1.7976931348623157E308");
        assertThat(Double.parseDouble(toAttributeValue(converter, -42.42d).asNumber())).isCloseTo(-42.42d, offset(1E-10));
        assertThat(toAttributeValue(converter, -Double.MIN_VALUE).asNumber()).isEqualTo("-4.9E-324");
        assertThat(toAttributeValue(converter, 0d).asNumber()).isEqualTo("0.0");
        assertThat(toAttributeValue(converter, Double.MIN_VALUE).asNumber()).isEqualTo("4.9E-324");
        assertThat(Double.parseDouble(toAttributeValue(converter, 42.42).asNumber())).isCloseTo(42.42d, offset(1E-10));
        assertThat(toAttributeValue(converter, Double.MAX_VALUE).asNumber()).isEqualTo("1.7976931348623157E308");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("2E308")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("-2E308")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("NaN")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("4.9E-324")))
                .isCloseTo(Double.MIN_VALUE, offset(1E-10));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-42.42"))).isCloseTo(-42.42d, offset(1E-10));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo(0d);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("42.42"))).isCloseTo(42.42d, offset(1E-10));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("1.7976931348623157E308")))
                .isCloseTo(Double.MAX_VALUE, offset(1E-10));
    }

    @Test
    public void shortAttributeConverterBehaves() {
        ShortAttributeConverter converter = ShortAttributeConverter.create();

        assertThat(toAttributeValue(converter, Short.MIN_VALUE).asNumber()).isEqualTo("-32768");
        assertThat(toAttributeValue(converter, (short) 0).asNumber()).isEqualTo("0");
        assertThat(toAttributeValue(converter, Short.MAX_VALUE).asNumber()).isEqualTo("32767");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("1.0")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("1.0")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-32768"))).isEqualTo(Short.MIN_VALUE);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo((short) 0);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("32767"))).isEqualTo(Short.MAX_VALUE);
    }

    @Test
    public void integerAttributeConverterBehaves() {
        IntegerAttributeConverter converter = IntegerAttributeConverter.create();

        assertThat(toAttributeValue(converter, Integer.MIN_VALUE).asNumber()).isEqualTo(Integer.toString(Integer.MIN_VALUE));
        assertThat(toAttributeValue(converter, -42).asNumber()).isEqualTo("-42");
        assertThat(toAttributeValue(converter, 0).asNumber()).isEqualTo("0");
        assertThat(toAttributeValue(converter, 42).asNumber()).isEqualTo("42");
        assertThat(toAttributeValue(converter, Integer.MAX_VALUE).asNumber()).isEqualTo(Integer.toString(Integer.MAX_VALUE));

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("1.0")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("1.0")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(Integer.toString(Integer.MIN_VALUE))))
                .isEqualTo(Integer.MIN_VALUE);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("-42"))).isEqualTo(-42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-42"))).isEqualTo(-42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo(0);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("42"))).isEqualTo(42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("42"))).isEqualTo(42);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(Integer.toString(Integer.MAX_VALUE))))
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void longAttributeConverterBehaves() {
        LongAttributeConverter converter = LongAttributeConverter.create();

        assertThat(toAttributeValue(converter, Long.MIN_VALUE).asNumber()).isEqualTo(Long.toString(Long.MIN_VALUE));
        assertThat(toAttributeValue(converter, -42L).asNumber()).isEqualTo("-42");
        assertThat(toAttributeValue(converter, 0L).asNumber()).isEqualTo("0");
        assertThat(toAttributeValue(converter, 42L).asNumber()).isEqualTo("42");
        assertThat(toAttributeValue(converter, Long.MAX_VALUE).asNumber()).isEqualTo(Long.toString(Long.MAX_VALUE));

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("1.0")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("1.0")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(Long.toString(Long.MIN_VALUE))))
                .isEqualTo(Long.MIN_VALUE);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("-42"))).isEqualTo(-42L);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-42"))).isEqualTo(-42L);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo(0L);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("42"))).isEqualTo(42L);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("42"))).isEqualTo(42L);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(Long.toString(Long.MAX_VALUE))))
                .isEqualTo(Long.MAX_VALUE);
    }

    private static String tiiiinyNumber() {
        return "-" + huuuugeNumber();
    }

    private static String huuuugeNumber() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 1_000; ++i) {
            result.append("9");
        }
        return result.toString();
    }
}

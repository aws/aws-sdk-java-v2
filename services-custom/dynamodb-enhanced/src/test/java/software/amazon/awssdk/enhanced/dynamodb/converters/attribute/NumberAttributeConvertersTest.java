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

package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.AtomicIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.AtomicLongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BigDecimalAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BigIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DoubleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.FloatAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.IntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ShortAttributeConverter;

public class NumberAttributeConvertersTest {
    private static String TIIIINY_NUMBER = tiiiinyNumber();
    private static String HUUUUGE_NUMBER = huuuugeNumber();

    @Test
    public void atomicIntegerAttributeConverterBehaves() {
        AtomicIntegerAttributeConverter converter = AtomicIntegerAttributeConverter.create();

        assertThat(transformFrom(converter, new AtomicInteger(Integer.MIN_VALUE)).n())
                .isEqualTo(Integer.toString(Integer.MIN_VALUE));
        assertThat(transformFrom(converter, new AtomicInteger(-42)).n()).isEqualTo("-42");
        assertThat(transformFrom(converter, new AtomicInteger(0)).n()).isEqualTo("0");
        assertThat(transformFrom(converter, new AtomicInteger(42)).n()).isEqualTo("42");
        assertThat(transformFrom(converter, new AtomicInteger(Integer.MAX_VALUE)).n())
                .isEqualTo(Integer.toString(Integer.MAX_VALUE));

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1.0")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("1.0")));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(Integer.toString(Integer.MIN_VALUE))))
                .hasValue(Integer.MIN_VALUE);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("-42"))).hasValue(-42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-42"))).hasValue(-42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).hasValue(0);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("42"))).hasValue(42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("42"))).hasValue(42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(Integer.toString(Integer.MAX_VALUE))))
                .hasValue(Integer.MAX_VALUE);
    }

    @Test
    public void atomicLongAttributeConverterBehaves() {
        AtomicLongAttributeConverter converter = AtomicLongAttributeConverter.create();

        assertThat(transformFrom(converter, new AtomicLong(Long.MIN_VALUE)).n())
                .isEqualTo(Long.toString(Long.MIN_VALUE));
        assertThat(transformFrom(converter, new AtomicLong(-42)).n()).isEqualTo("-42");
        assertThat(transformFrom(converter, new AtomicLong(0)).n()).isEqualTo("0");
        assertThat(transformFrom(converter, new AtomicLong(42)).n()).isEqualTo("42");
        assertThat(transformFrom(converter, new AtomicLong(Long.MAX_VALUE)).n())
                .isEqualTo(Long.toString(Long.MAX_VALUE));

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1.0")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("1.0")));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(Long.toString(Long.MIN_VALUE))))
                .hasValue(Long.MIN_VALUE);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("-42"))).hasValue(-42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-42"))).hasValue(-42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).hasValue(0);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("42"))).hasValue(42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("42"))).hasValue(42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(Long.toString(Long.MAX_VALUE))))
                .hasValue(Long.MAX_VALUE);
    }

    @Test
    public void bigDecimalAttributeConverterBehaves() {
        BigDecimalAttributeConverter converter = BigDecimalAttributeConverter.create();

        assertThat(transformFrom(converter, new BigDecimal(TIIIINY_NUMBER)).n()).isEqualTo(TIIIINY_NUMBER);
        assertThat(transformFrom(converter, new BigDecimal("43.0")).n()).isEqualTo("43.0");
        assertThat(transformFrom(converter, new BigDecimal("-42.42")).n()).isEqualTo("-42.42");
        assertThat(transformFrom(converter, new BigDecimal("-42")).n()).isEqualTo("-42");
        assertThat(transformFrom(converter, new BigDecimal("0")).n()).isEqualTo("0");
        assertThat(transformFrom(converter, new BigDecimal("42")).n()).isEqualTo("42");
        assertThat(transformFrom(converter, new BigDecimal("42.42")).n()).isEqualTo("42.42");
        assertThat(transformFrom(converter, new BigDecimal("43.0")).n()).isEqualTo("43.0");
        assertThat(transformFrom(converter, new BigDecimal(HUUUUGE_NUMBER)).n()).isEqualTo(HUUUUGE_NUMBER);

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("X")));

        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(TIIIINY_NUMBER)).toString()).isEqualTo(TIIIINY_NUMBER);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-43.0")).toString()).isEqualTo("-43.0");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("-42.42")).toString()).isEqualTo("-42.42");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-42")).toString()).isEqualTo("-42");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0")).toString()).isEqualTo("0");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("42")).toString()).isEqualTo("42");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("42.42")).toString()).isEqualTo("42.42");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("43.0")).toString()).isEqualTo("43.0");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(HUUUUGE_NUMBER)).toString()).isEqualTo(HUUUUGE_NUMBER);
    }

    @Test
    public void bigIntegerAttributeConverterBehaves() {
        BigIntegerAttributeConverter converter = BigIntegerAttributeConverter.create();

        assertThat(transformFrom(converter, new BigInteger(TIIIINY_NUMBER)).n()).isEqualTo(TIIIINY_NUMBER);
        assertThat(transformFrom(converter, new BigInteger("-42")).n()).isEqualTo("-42");
        assertThat(transformFrom(converter, new BigInteger("0")).n()).isEqualTo("0");
        assertThat(transformFrom(converter, new BigInteger("42")).n()).isEqualTo("42");
        assertThat(transformFrom(converter, new BigInteger(HUUUUGE_NUMBER)).n()).isEqualTo(HUUUUGE_NUMBER);

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("X")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("1.0")));

        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(TIIIINY_NUMBER)).toString()).isEqualTo(TIIIINY_NUMBER);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("-42")).toString()).isEqualTo("-42");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-42")).toString()).isEqualTo("-42");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0")).toString()).isEqualTo("0");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("42")).toString()).isEqualTo("42");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("42")).toString()).isEqualTo("42");
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(HUUUUGE_NUMBER)).toString()).isEqualTo(HUUUUGE_NUMBER);
    }

    @Test
    public void floatAttributeConverterBehaves() {
        FloatAttributeConverter converter = FloatAttributeConverter.create();

        assertFails(() -> transformFrom(converter, Float.NEGATIVE_INFINITY));
        assertFails(() -> transformFrom(converter, Float.POSITIVE_INFINITY));
        assertFails(() -> transformFrom(converter, Float.NaN));

        assertThat(transformFrom(converter, -Float.MAX_VALUE).n()).isEqualTo("-3.4028235E38");
        assertThat(Float.parseFloat(transformFrom(converter, -42.42f).n())).isCloseTo(-42.42f, offset(1E-10f));
        assertThat(transformFrom(converter, -Float.MIN_VALUE).n()).isEqualTo("-1.4E-45");
        assertThat(transformFrom(converter, 0f).n()).isEqualTo("0.0");
        assertThat(transformFrom(converter, Float.MIN_VALUE).n()).isEqualTo("1.4E-45");
        assertThat(Float.parseFloat(transformFrom(converter, 42.42f).n())).isCloseTo(42.42f, offset(1E-10f));
        assertThat(transformFrom(converter, Float.MAX_VALUE).n()).isEqualTo("3.4028235E38");

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("2E308")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("-2E308")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("NaN")));

        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("1.4E-45")))
                .isCloseTo(Float.MIN_VALUE, offset(1E-10f));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-42.42"))).isCloseTo(-42.42f, offset(1E-10f));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).isEqualTo(0f);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("42.42"))).isCloseTo(42.42f, offset(1E-10f));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("3.4028235E38")))
                .isCloseTo(Float.MAX_VALUE, offset(1E-10f));
    }

    @Test
    public void doubleAttributeConverterBehaves() {
        DoubleAttributeConverter converter = DoubleAttributeConverter.create();

        assertFails(() -> transformFrom(converter, Double.NEGATIVE_INFINITY));
        assertFails(() -> transformFrom(converter, Double.POSITIVE_INFINITY));
        assertFails(() -> transformFrom(converter, Double.NaN));

        assertThat(transformFrom(converter, -Double.MAX_VALUE).n()).isEqualTo("-1.7976931348623157E308");
        assertThat(Double.parseDouble(transformFrom(converter, -42.42d).n())).isCloseTo(-42.42d, offset(1E-10));
        assertThat(transformFrom(converter, -Double.MIN_VALUE).n()).isEqualTo("-4.9E-324");
        assertThat(transformFrom(converter, 0d).n()).isEqualTo("0.0");
        assertThat(transformFrom(converter, Double.MIN_VALUE).n()).isEqualTo("4.9E-324");
        assertThat(Double.parseDouble(transformFrom(converter, 42.42).n())).isCloseTo(42.42d, offset(1E-10));
        assertThat(transformFrom(converter, Double.MAX_VALUE).n()).isEqualTo("1.7976931348623157E308");

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("2E308")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("-2E308")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("NaN")));

        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("4.9E-324")))
                .isCloseTo(Double.MIN_VALUE, offset(1E-10));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-42.42"))).isCloseTo(-42.42d, offset(1E-10));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).isEqualTo(0d);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("42.42"))).isCloseTo(42.42d, offset(1E-10));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("1.7976931348623157E308")))
                .isCloseTo(Double.MAX_VALUE, offset(1E-10));
    }

    @Test
    public void shortAttributeConverterBehaves() {
        ShortAttributeConverter converter = ShortAttributeConverter.create();

        assertThat(transformFrom(converter, Short.MIN_VALUE).n()).isEqualTo("-32768");
        assertThat(transformFrom(converter, (short) 0).n()).isEqualTo("0");
        assertThat(transformFrom(converter, Short.MAX_VALUE).n()).isEqualTo("32767");

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1.0")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("1.0")));

        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-32768"))).isEqualTo(Short.MIN_VALUE);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).isEqualTo((short) 0);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("32767"))).isEqualTo(Short.MAX_VALUE);
    }

    @Test
    public void integerAttributeConverterBehaves() {
        IntegerAttributeConverter converter = IntegerAttributeConverter.create();

        assertThat(transformFrom(converter, Integer.MIN_VALUE).n()).isEqualTo(Integer.toString(Integer.MIN_VALUE));
        assertThat(transformFrom(converter, -42).n()).isEqualTo("-42");
        assertThat(transformFrom(converter, 0).n()).isEqualTo("0");
        assertThat(transformFrom(converter, 42).n()).isEqualTo("42");
        assertThat(transformFrom(converter, Integer.MAX_VALUE).n()).isEqualTo(Integer.toString(Integer.MAX_VALUE));

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1.0")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("1.0")));

        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(Integer.toString(Integer.MIN_VALUE))))
                .isEqualTo(Integer.MIN_VALUE);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("-42"))).isEqualTo(-42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-42"))).isEqualTo(-42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).isEqualTo(0);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("42"))).isEqualTo(42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("42"))).isEqualTo(42);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(Integer.toString(Integer.MAX_VALUE))))
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void longAttributeConverterBehaves() {
        LongAttributeConverter converter = LongAttributeConverter.create();

        assertThat(transformFrom(converter, Long.MIN_VALUE).n()).isEqualTo(Long.toString(Long.MIN_VALUE));
        assertThat(transformFrom(converter, -42L).n()).isEqualTo("-42");
        assertThat(transformFrom(converter, 0L).n()).isEqualTo("0");
        assertThat(transformFrom(converter, 42L).n()).isEqualTo("42");
        assertThat(transformFrom(converter, Long.MAX_VALUE).n()).isEqualTo(Long.toString(Long.MAX_VALUE));

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1.0")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("1.0")));

        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(Long.toString(Long.MIN_VALUE))))
                .isEqualTo(Long.MIN_VALUE);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("-42"))).isEqualTo(-42L);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-42"))).isEqualTo(-42L);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).isEqualTo(0L);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("42"))).isEqualTo(42L);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("42"))).isEqualTo(42L);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(Long.toString(Long.MAX_VALUE))))
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void setOfLongsAttributeConverter_ReturnsNSType() {
        SetAttributeConverter<Set<Long>> longSet = SetAttributeConverter.setConverter(LongAttributeConverter.create());
        assertThat(longSet.attributeValueType()).isEqualTo(AttributeValueType.NS);
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

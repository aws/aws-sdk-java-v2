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
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.fromAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.toAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromNumber;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromString;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.nullValue;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

public class OptionalAttributeConvertersTest {
    @Test
    public void optionalConverterWorksCorrectly() {
        OptionalSubtypeAttributeConverter converter = OptionalSubtypeAttributeConverter.create();

        assertThat(toAttributeValue(converter, Optional.empty())).isEqualTo(nullValue());
        assertThat(toAttributeValue(converter, Optional.of("foo"))).isEqualTo(fromString("foo"));
        assertThat(toAttributeValue(converter, Optional.of(1))).isEqualTo(fromNumber("1"));

        assertThat(fromAttributeValue(converter, TypeToken.optionalOf(Void.class), nullValue())).isEmpty();
        assertThat(fromAttributeValue(converter, TypeToken.optionalOf(String.class), fromString("foo"))).hasValue("foo");
        assertThat(fromAttributeValue(converter, TypeToken.optionalOf(Integer.class), fromNumber("1"))).hasValue(1);
    }

    @Test
    public void optionalDoubleConverterWorksCorrectly() {
        OptionalDoubleAttributeConverter converter = OptionalDoubleAttributeConverter.create();

        assertThat(toAttributeValue(converter, OptionalDouble.empty())).isEqualTo(nullValue());
        assertThat(toAttributeValue(converter, OptionalDouble.of(-Double.MAX_VALUE))).isEqualTo(fromNumber("-1.7976931348623157E308"));
        assertThat(toAttributeValue(converter, OptionalDouble.of(-Double.MIN_VALUE))).isEqualTo(fromNumber("-4.9E-324"));
        assertThat(toAttributeValue(converter, OptionalDouble.of(0.0))).isEqualTo(fromNumber("0.0"));
        assertThat(toAttributeValue(converter, OptionalDouble.of(Double.MIN_VALUE))).isEqualTo(fromNumber("4.9E-324"));
        assertThat(toAttributeValue(converter, OptionalDouble.of(Double.MAX_VALUE))).isEqualTo(fromNumber("1.7976931348623157E308"));

        assertThat(fromAttributeValue(converter, nullValue())).isEmpty();
        assertThat(fromAttributeValue(converter, fromNumber("-1.7976931348623157E308"))).hasValue(-Double.MAX_VALUE);
        assertThat(fromAttributeValue(converter, fromNumber("-4.9E-324"))).hasValue(-Double.MIN_VALUE);
        assertThat(fromAttributeValue(converter, fromNumber("0.0"))).hasValue(0.0);
        assertThat(fromAttributeValue(converter, fromNumber("4.9E-324"))).hasValue(Double.MIN_VALUE);
        assertThat(fromAttributeValue(converter, fromNumber("1.7976931348623157E308"))).hasValue(Double.MAX_VALUE);
    }

    @Test
    public void optionalIntConverterWorksCorrectly() {
        OptionalIntAttributeConverter converter = OptionalIntAttributeConverter.create();

        assertThat(toAttributeValue(converter, OptionalInt.empty())).isEqualTo(nullValue());
        assertThat(toAttributeValue(converter, OptionalInt.of(Integer.MIN_VALUE))).isEqualTo(fromNumber("-2147483648"));
        assertThat(toAttributeValue(converter, OptionalInt.of(0))).isEqualTo(fromNumber("0"));
        assertThat(toAttributeValue(converter, OptionalInt.of(Integer.MAX_VALUE))).isEqualTo(fromNumber("2147483647"));

        assertThat(fromAttributeValue(converter, nullValue())).isEmpty();
        assertThat(fromAttributeValue(converter, fromNumber("-2147483648"))).hasValue(Integer.MIN_VALUE);
        assertThat(fromAttributeValue(converter, fromNumber("0"))).hasValue(0);
        assertThat(fromAttributeValue(converter, fromNumber("2147483647"))).hasValue(Integer.MAX_VALUE);
    }

    @Test
    public void optionalLongConverterWorksCorrectly() {
        OptionalLongAttributeConverter converter = OptionalLongAttributeConverter.create();

        assertThat(toAttributeValue(converter, OptionalLong.empty())).isEqualTo(nullValue());
        assertThat(toAttributeValue(converter, OptionalLong.of(Long.MIN_VALUE))).isEqualTo(fromNumber("-9223372036854775808"));
        assertThat(toAttributeValue(converter, OptionalLong.of(0))).isEqualTo(fromNumber("0"));
        assertThat(toAttributeValue(converter, OptionalLong.of(Long.MAX_VALUE))).isEqualTo(fromNumber("9223372036854775807"));

        assertThat(fromAttributeValue(converter, nullValue())).isEmpty();
        assertThat(fromAttributeValue(converter, fromNumber("-9223372036854775808"))).hasValue(Long.MIN_VALUE);
        assertThat(fromAttributeValue(converter, fromNumber("0"))).hasValue(0);
        assertThat(fromAttributeValue(converter, fromNumber("9223372036854775807"))).hasValue(Long.MAX_VALUE);
    }
}

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
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.fromAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.toAttributeValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;

public class LocalDateTimeAttributeConvertersTest {
    @Test
    public void localDateAttributeConverterBehaves() {
        LocalDateAttributeConverter converter = LocalDateAttributeConverter.create();

        assertThat(toAttributeValue(converter, LocalDate.MIN).asNumber()).isEqualTo("-9999999990101000000");
        assertThat(toAttributeValue(converter, LocalDate.of(0, 1, 1)).asNumber()).isEqualTo("00101000000");
        assertThat(toAttributeValue(converter, LocalDate.MAX).asNumber()).isEqualTo("9999999991231000000");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("-99999999990101000000")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("99999999991231000000")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-9999999990101000000")))
                .isEqualTo(LocalDate.MIN);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("00101000000"))).isEqualTo(LocalDate.of(0, 1, 1));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("9999999991231000000")))
                .isEqualTo(LocalDate.MAX);
    }

    @Test
    public void localDateTimeAttributeConverterBehaves() {
        LocalDateTimeAttributeConverter converter = LocalDateTimeAttributeConverter.create();

        assertThat(toAttributeValue(converter, LocalDateTime.MIN).asNumber()).isEqualTo("-9999999990101000000");
        assertThat(toAttributeValue(converter, LocalDateTime.of(0, 1, 1, 0, 0, 0, 0)).asNumber()).isEqualTo("00101000000");
        assertThat(toAttributeValue(converter, LocalDateTime.MAX).asNumber()).isEqualTo("9999999991231235959.999999999");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("-99999999990101000000")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("99999999991231000000")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("99999999991232000000")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("00101000000.9999999999")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-9999999990101000000")))
                .isEqualTo(LocalDateTime.MIN);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("00101000000")))
                .isEqualTo(LocalDateTime.of(0, 1, 1, 0, 0, 0, 0));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("9999999991231235959.999999999")))
                .isEqualTo(LocalDateTime.MAX);
    }

    @Test
    public void localTimeAttributeConverterBehaves() {
        LocalTimeAttributeConverter converter = LocalTimeAttributeConverter.create();

        assertThat(toAttributeValue(converter, LocalTime.MIN).asNumber()).isEqualTo("000000");
        assertThat(toAttributeValue(converter, LocalTime.of(1, 2, 3, 4)).asNumber()).isEqualTo("010203.000000004");
        assertThat(toAttributeValue(converter, LocalTime.MAX).asNumber()).isEqualTo("235959.999999999");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("-1")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("240000")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("000000.9999999999")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("000000")))
                .isEqualTo(LocalTime.MIN);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("010203.000000004")))
                .isEqualTo(LocalTime.of(1, 2, 3, 4));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("235959.999999999")))
                .isEqualTo(LocalTime.MAX);
    }

    @Test
    public void monthDayAttributeConverterBehaves() {
        MonthDayAttributeConverter converter = MonthDayAttributeConverter.create();

        assertThat(toAttributeValue(converter, MonthDay.of(1, 1)).asNumber()).isEqualTo("0101");
        assertThat(toAttributeValue(converter, MonthDay.of(5, 21)).asNumber()).isEqualTo("0521");
        assertThat(toAttributeValue(converter, MonthDay.of(12, 31)).asNumber()).isEqualTo("1231");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("X")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("0230")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0101"))).isEqualTo(MonthDay.of(1, 1));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0521"))).isEqualTo(MonthDay.of(5, 21));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("1231"))).isEqualTo(MonthDay.of(12, 31));
    }
}

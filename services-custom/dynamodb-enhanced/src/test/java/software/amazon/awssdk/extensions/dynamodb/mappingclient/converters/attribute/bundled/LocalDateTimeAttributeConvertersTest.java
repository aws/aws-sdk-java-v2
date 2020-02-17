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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled.ConverterTestUtils.transformTo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import org.junit.Test;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.LocalDateAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.LocalDateTimeAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.LocalTimeAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.MonthDayAttributeConverter;

public class LocalDateTimeAttributeConvertersTest {
    @Test
    public void localDateAttributeConverterBehaves() {
        LocalDateAttributeConverter converter = LocalDateAttributeConverter.create();

        assertThat(transformFrom(converter, LocalDate.MIN).n()).isEqualTo("-9999999990101000000");
        assertThat(transformFrom(converter, LocalDate.of(0, 1, 1)).n()).isEqualTo("00101000000");
        assertThat(transformFrom(converter, LocalDate.MAX).n()).isEqualTo("9999999991231000000");

        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("-99999999990101000000").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("99999999991231000000").toGeneratedAttributeValue()));

        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("-9999999990101000000").toGeneratedAttributeValue()))
                .isEqualTo(LocalDate.MIN);
        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("00101000000").toGeneratedAttributeValue())).isEqualTo(LocalDate.of(0, 1, 1));
        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("9999999991231000000").toGeneratedAttributeValue()))
                .isEqualTo(LocalDate.MAX);
    }

    @Test
    public void localDateTimeAttributeConverterBehaves() {
        LocalDateTimeAttributeConverter converter = LocalDateTimeAttributeConverter.create();

        assertThat(transformFrom(converter, LocalDateTime.MIN).n()).isEqualTo("-9999999990101000000");
        assertThat(transformFrom(converter, LocalDateTime.of(0, 1, 1, 0, 0, 0, 0)).n()).isEqualTo("00101000000");
        assertThat(transformFrom(converter, LocalDateTime.MAX).n()).isEqualTo("9999999991231235959.999999999");

        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("-99999999990101000000").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("99999999991231000000").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("99999999991232000000").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("00101000000.9999999999").toGeneratedAttributeValue()));

        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("-9999999990101000000").toGeneratedAttributeValue()))
                .isEqualTo(LocalDateTime.MIN);
        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("00101000000").toGeneratedAttributeValue()))
                .isEqualTo(LocalDateTime.of(0, 1, 1, 0, 0, 0, 0));
        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("9999999991231235959.999999999").toGeneratedAttributeValue()))
                .isEqualTo(LocalDateTime.MAX);
    }

    @Test
    public void localTimeAttributeConverterBehaves() {
        LocalTimeAttributeConverter converter = LocalTimeAttributeConverter.create();

        assertThat(transformFrom(converter, LocalTime.MIN).n()).isEqualTo("000000");
        assertThat(transformFrom(converter, LocalTime.of(1, 2, 3, 4)).n()).isEqualTo("010203.000000004");
        assertThat(transformFrom(converter, LocalTime.MAX).n()).isEqualTo("235959.999999999");

        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("-1").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("240000").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("000000.9999999999").toGeneratedAttributeValue()));

        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("000000").toGeneratedAttributeValue()))
                .isEqualTo(LocalTime.MIN);
        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("010203.000000004").toGeneratedAttributeValue()))
                .isEqualTo(LocalTime.of(1, 2, 3, 4));
        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("235959.999999999").toGeneratedAttributeValue()))
                .isEqualTo(LocalTime.MAX);
    }

    @Test
    public void monthDayAttributeConverterBehaves() {
        MonthDayAttributeConverter converter = MonthDayAttributeConverter.create();

        assertThat(transformFrom(converter, MonthDay.of(1, 1)).n()).isEqualTo("0101");
        assertThat(transformFrom(converter, MonthDay.of(5, 21)).n()).isEqualTo("0521");
        assertThat(transformFrom(converter, MonthDay.of(12, 31)).n()).isEqualTo("1231");

        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("X").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromNumber("0230").toGeneratedAttributeValue()));

        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("0101").toGeneratedAttributeValue())).isEqualTo(MonthDay.of(1, 1));
        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("0521").toGeneratedAttributeValue())).isEqualTo(MonthDay.of(5, 21));
        assertThat(transformTo(converter, ItemAttributeValue.fromNumber("1231").toGeneratedAttributeValue())).isEqualTo(MonthDay.of(12, 31));
    }
}

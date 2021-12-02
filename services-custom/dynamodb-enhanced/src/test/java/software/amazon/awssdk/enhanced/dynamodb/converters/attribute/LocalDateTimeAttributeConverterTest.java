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
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;

import java.time.LocalDateTime;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalDateTimeAttributeConverter;

public class LocalDateTimeAttributeConverterTest {

    private static LocalDateTimeAttributeConverter converter = LocalDateTimeAttributeConverter.create();

    @Test
    public void localDateTimeAttributeConverterMinTest() {
        verifyTransform(LocalDateTime.MIN, "-999999999-01-01T00:00");
    }

    @Test
    public void localDateTimeAttributeConverterNormalTest() {
        verifyTransform(LocalDateTime.of(0, 1, 1, 0, 0, 0, 0), "0000-01-01T00:00");
    }

    @Test
    public void localDateTimeAttributeConverterMaxTest() {
        verifyTransform(LocalDateTime.MAX, "+999999999-12-31T23:59:59.999999999");
    }


    @Test
    public void localDateTimeAttributeConverterLowerBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("-9999999999-01-01T00:00")
                                                                       .toAttributeValue()));
    }

    @Test
    public void localDateTimeAttributeConverterHigherBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("9999999999-12-31T00:00:00")
                                                                       .toAttributeValue()));
    }

    @Test
    public void localDateTimeAttributeConverterExceedHigherBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("9999999999-12-32T00:00:00")
                                                                       .toAttributeValue()));
    }

    @Test
    public void localDateTimeAttributeConverterInvalidNanoSecondsTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("0-01-01T00:00:00.9999999999")
                                                                       .toAttributeValue()));
    }

    @Test
    public void localDateTimeAttributeConverterNotAcceptInstantTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void localDateTimeAttributeConverterNotAcceptOffsetTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00")
                                                                       .toAttributeValue()));
    }

    @Test
    public void localDateTimeAttributeConverterNotAcceptZonedTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00[Europe/Paris]")
                                                                       .toAttributeValue()));
    }

    @Test
    public void localDateTimeAttributeConverterNotAcceptLocalTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void localDateTimeAttributeConverterNotAcceptMonthDayTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("05-21")
                                                                       .toAttributeValue()));
    }

    @Test
    public void localDateTimeAttributeConverterAdditionallyAcceptLocalDateTest() {
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21").toAttributeValue()))
            .isEqualTo(LocalDateTime.of(1988, 5, 21, 0, 0, 0));
    }

    private void verifyTransform(LocalDateTime objectToTransform, String attributeValueString) {
        assertThat(transformFrom(converter, objectToTransform))
            .isEqualTo(EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue());
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue()))
            .isEqualTo(objectToTransform);
    }
}

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

import java.time.Instant;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;

public class InstantAsStringAttributeConvertersTest {

    private static final InstantAsStringAttributeConverter CONVERTER = InstantAsStringAttributeConverter.create();

    @Test
    public void InstantAsStringAttributeConverterMinTest() {
        verifyTransform(Instant.MIN, "-1000000000-01-01T00:00:00Z");
    }

    @Test
    public void InstantAsStringAttributeConverterEpochMinusOneMilliTest() {
        verifyTransform(Instant.EPOCH.minusMillis(1), "1969-12-31T23:59:59.999Z");
    }

    @Test
    public void InstantAsStringAttributeConverterEpochTest() {
        verifyTransform(Instant.EPOCH, "1970-01-01T00:00:00Z");
    }

    @Test
    public void InstantAsStringAttributeConverterEpochPlusOneMilliTest() {
        verifyTransform(Instant.EPOCH.plusMillis(1), "1970-01-01T00:00:00.001Z");
    }

    @Test
    public void InstantAsStringAttributeConverterMaxTest() {
        verifyTransform(Instant.MAX, "+1000000000-12-31T23:59:59.999999999Z");
    }


    @Test
    public void InstantAsStringAttributeConverterExceedLowerBoundTest() {
        assertFails(() -> transformTo(CONVERTER, EnhancedAttributeValue.fromString("-1000000001-12-31T23:59:59.999999999Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void InstantAsStringAttributeConverterInvalidFormatTest() {
        assertFails(() -> transformTo(CONVERTER, EnhancedAttributeValue.fromString("X")
                                                                       .toAttributeValue()));
    }

    @Test
    public void InstantAsStringAttributeConverterExceedHigherBoundTest() {
        assertFails(() -> transformTo(CONVERTER, EnhancedAttributeValue.fromString("+1000000001-01-01T00:00:00Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void InstantAsStringAttributeConverterNotAcceptLocalDateTimeTest() {
        assertFails(() -> transformTo(CONVERTER, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void InstantAsStringAttributeConverterNotAcceptOffsetTimeTest() {
        assertFails(() -> transformTo(CONVERTER, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00")
                                                                       .toAttributeValue()));
    }

    @Test
    public void InstantAsStringAttributeConverterNotAcceptZonedTimeTest() {
        assertFails(() -> transformTo(CONVERTER, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00[Europe/Paris]")
                                                                       .toAttributeValue()));
    }

    @Test
    public void InstantAsStringAttributeConverterNotAcceptLocalDateTest() {
        assertFails(() -> transformTo(CONVERTER, EnhancedAttributeValue.fromString("1988-05-21")
                                                                       .toAttributeValue()));
    }

    @Test
    public void InstantAsStringAttributeConverterNotAcceptLocalTimeTest() {
        assertFails(() -> transformTo(CONVERTER, EnhancedAttributeValue.fromString("00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void InstantAsStringAttributeConverterNotAcceptMonthDayTest() {
        assertFails(() -> transformTo(CONVERTER, EnhancedAttributeValue.fromString("05-21")
                                                                       .toAttributeValue()));
    }

    private void verifyTransform(Instant objectToTransform, String attributeValueString) {
        assertThat(transformFrom(CONVERTER, objectToTransform))
            .isEqualTo(EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue());
        assertThat(transformTo(CONVERTER, EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue()))
            .isEqualTo(objectToTransform);
    }

}

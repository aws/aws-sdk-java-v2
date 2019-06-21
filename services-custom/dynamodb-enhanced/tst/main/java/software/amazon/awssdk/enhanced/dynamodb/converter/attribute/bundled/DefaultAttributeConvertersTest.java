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

import java.time.Duration;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;

public class DefaultAttributeConvertersTest {
    @Test
    public void identityAttributeConverterBehaves() {
        AttributeAttributeConverter converter = AttributeAttributeConverter.create();

        ItemAttributeValue attributeValue = ItemAttributeValue.fromString("");
        ItemAttributeValue attributeValue2 = ItemAttributeValue.fromString("");
        assertThat(toAttributeValue(converter, attributeValue) == attributeValue).isTrue();
        assertThat(fromAttributeValue(converter, attributeValue) == attributeValue).isTrue();
        assertThat(toAttributeValue(converter, attributeValue) == attributeValue2).isFalse();
        assertThat(fromAttributeValue(converter, attributeValue) == attributeValue2).isFalse();
    }

    @Test
    public void durationAttributeConverterBehaves() {
        DurationAttributeConverter converter = DurationAttributeConverter.create();

        assertThat(toAttributeValue(converter, Duration.ofSeconds(Long.MIN_VALUE)).asNumber())
                .isEqualTo("-9223372036854775808");
        assertThat(toAttributeValue(converter, Duration.ZERO).asNumber()).isEqualTo("0");
        assertThat(toAttributeValue(converter, Duration.ofSeconds(Long.MAX_VALUE, 999_999_999L)).asNumber())
                .isEqualTo("9223372036854775807.999999999");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("-9223372036854775809")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("9223372036854775808")));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-9223372036854775808")))
                .isEqualTo(Duration.ofSeconds(Long.MIN_VALUE));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo(Duration.ZERO);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("9223372036854775807.999999999")))
                .isEqualTo(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999L));
    }
}
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.AtomicBooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;

public class BooleanAttributeConvertersTest {
    @Test
    public void atomicBooleanAttributeConverterBehaves() {
        AtomicBooleanAttributeConverter converter = AtomicBooleanAttributeConverter.create();
        assertThat(transformFrom(converter, new AtomicBoolean(true)).bool()).isEqualTo(true);
        assertThat(transformFrom(converter, new AtomicBoolean(false)).bool()).isEqualTo(false);

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("FALSE").toAttributeValue()));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("TRUE").toAttributeValue()));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("0").toAttributeValue()));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1").toAttributeValue()));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("").toAttributeValue()));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("1").toAttributeValue())).isTrue();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0").toAttributeValue())).isFalse();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("true").toAttributeValue())).isTrue();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("false").toAttributeValue())).isFalse();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromBoolean(true).toAttributeValue())).isTrue();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromBoolean(false).toAttributeValue())).isFalse();
    }

    @Test
    public void booleanAttributeConverterBehaves() {
        BooleanAttributeConverter converter = BooleanAttributeConverter.create();

        assertThat(transformFrom(converter, true).bool()).isEqualTo(true);
        assertThat(transformFrom(converter, false).bool()).isEqualTo(false);

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("FALSE").toAttributeValue()));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("TRUE").toAttributeValue()));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("0").toAttributeValue()));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1").toAttributeValue()));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("").toAttributeValue()));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("1").toAttributeValue())).isTrue();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0").toAttributeValue())).isFalse();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("true").toAttributeValue())).isTrue();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("false").toAttributeValue())).isFalse();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromBoolean(true).toAttributeValue())).isTrue();
        assertThat(transformTo(converter, EnhancedAttributeValue.fromBoolean(false).toAttributeValue())).isFalse();
    }

    @Test
    public void setOfBooleanAttributeConverter_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> SetAttributeConverter.setConverter(BooleanAttributeConverter.create()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SetAttributeConverter cannot be created")
            .hasMessageContaining("Boolean");
    }
}

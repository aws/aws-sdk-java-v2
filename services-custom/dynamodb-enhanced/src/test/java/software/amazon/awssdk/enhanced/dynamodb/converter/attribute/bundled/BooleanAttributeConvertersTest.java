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
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.transformTo;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ItemAttributeValue;

public class BooleanAttributeConvertersTest {
    @Test
    public void atomicBooleanAttributeConverterBehaves() {
        AtomicBooleanAttributeConverter converter = AtomicBooleanAttributeConverter.create();
        assertThat(transformFrom(converter, new AtomicBoolean(true)).bool()).isEqualTo(true);
        assertThat(transformFrom(converter, new AtomicBoolean(false)).bool()).isEqualTo(false);

        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("FALSE").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("TRUE").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("0").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("1").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("").toGeneratedAttributeValue()));
        assertThat(transformTo(converter, ItemAttributeValue.fromString("true").toGeneratedAttributeValue())).isTrue();
        assertThat(transformTo(converter, ItemAttributeValue.fromString("false").toGeneratedAttributeValue())).isFalse();
        assertThat(transformTo(converter, ItemAttributeValue.fromBoolean(true).toGeneratedAttributeValue())).isTrue();
        assertThat(transformTo(converter, ItemAttributeValue.fromBoolean(false).toGeneratedAttributeValue())).isFalse();
    }

    @Test
    public void booleanAttributeConverterBehaves() {
        BooleanAttributeConverter converter = BooleanAttributeConverter.create();

        assertThat(transformFrom(converter, true).bool()).isEqualTo(true);
        assertThat(transformFrom(converter, false).bool()).isEqualTo(false);

        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("FALSE").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("TRUE").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("0").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("1").toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromString("").toGeneratedAttributeValue()));
        assertThat(transformTo(converter, ItemAttributeValue.fromString("true").toGeneratedAttributeValue())).isTrue();
        assertThat(transformTo(converter, ItemAttributeValue.fromString("false").toGeneratedAttributeValue())).isFalse();
        assertThat(transformTo(converter, ItemAttributeValue.fromBoolean(true).toGeneratedAttributeValue())).isTrue();
        assertThat(transformTo(converter, ItemAttributeValue.fromBoolean(false).toGeneratedAttributeValue())).isFalse();
    }
}

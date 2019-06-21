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

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;

public class BooleanAttributeConvertersTest {
    @Test
    public void atomicBooleanAttributeConverterBehaves() {
        AtomicBooleanAttributeConverter converter = AtomicBooleanAttributeConverter.create();
        assertThat(toAttributeValue(converter, new AtomicBoolean(true)).asBoolean()).isEqualTo(true);
        assertThat(toAttributeValue(converter, new AtomicBoolean(false)).asBoolean()).isEqualTo(false);

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("FALSE")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("TRUE")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("0")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("1")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("")));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("true"))).isTrue();
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("false"))).isFalse();
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromBoolean(true))).isTrue();
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromBoolean(false))).isFalse();
    }

    @Test
    public void booleanAttributeConverterBehaves() {
        BooleanAttributeConverter converter = BooleanAttributeConverter.create();

        assertThat(toAttributeValue(converter, true).asBoolean()).isEqualTo(true);
        assertThat(toAttributeValue(converter, false).asBoolean()).isEqualTo(false);

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("FALSE")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("TRUE")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("0")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("1")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("")));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("true"))).isTrue();
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("false"))).isFalse();
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromBoolean(true))).isTrue();
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromBoolean(false))).isFalse();
    }
}

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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ItemAttributeValueTest {
    @Test
    public void simpleFromMethodsCreateCorrectTypes() {
        assertThat(ItemAttributeValue.nullValue()).satisfies(v -> {
            assertThat(v.isNull()).isTrue();
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.NULL);
        });

        assertThat(ItemAttributeValue.fromString("foo")).satisfies(v -> {
            assertThat(v.isString()).isTrue();
            assertThat(v.asString()).isEqualTo("foo");
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.STRING);
        });

        assertThat(ItemAttributeValue.fromNumber("1")).satisfies(v -> {
            assertThat(v.isNumber()).isTrue();
            assertThat(v.asNumber()).isEqualTo("1");
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.NUMBER);
        });

        assertThat(ItemAttributeValue.fromBoolean(true)).satisfies(v -> {
            assertThat(v.isBoolean()).isTrue();
            assertThat(v.asBoolean()).isEqualTo(true);
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.BOOLEAN);
        });

        assertThat(ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo"))).satisfies(v -> {
            assertThat(v.isBytes()).isTrue();
            assertThat(v.asBytes().asUtf8String()).isEqualTo("foo");
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.BYTES);
        });

        assertThat(ItemAttributeValue.fromSetOfStrings(Arrays.asList("a", "b"))).satisfies(v -> {
            assertThat(v.isSetOfStrings()).isTrue();
            assertThat(v.asSetOfStrings()).containsExactly("a", "b");
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.SET_OF_STRINGS);
        });

        assertThat(ItemAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2"))).satisfies(v -> {
            assertThat(v.isSetOfNumbers()).isTrue();
            assertThat(v.asSetOfNumbers()).containsExactly("1", "2");
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.SET_OF_NUMBERS);
        });

        assertThat(ItemAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                   SdkBytes.fromUtf8String("foo2")))).satisfies(v -> {
            assertThat(v.isSetOfBytes()).isTrue();
            assertThat(v.asSetOfBytes().get(0).asUtf8String()).isEqualTo("foo");
            assertThat(v.asSetOfBytes().get(1).asUtf8String()).isEqualTo("foo2");
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.SET_OF_BYTES);
        });

        assertThat(ItemAttributeValue.fromListOfAttributeValues(Arrays.asList(ItemAttributeValue.fromString("foo"),
                                                                              ItemAttributeValue.fromBoolean(true)))).satisfies(v -> {
            assertThat(v.isListOfAttributeValues()).isTrue();
            assertThat(v.asListOfAttributeValues().get(0).asString()).isEqualTo("foo");
            assertThat(v.asListOfAttributeValues().get(1).asBoolean()).isEqualTo(true);
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.LIST_OF_ATTRIBUTE_VALUES);
        });

        Map<String, ItemAttributeValue> map = new LinkedHashMap<>();
        map.put("a", ItemAttributeValue.fromString("foo"));
        map.put("b", ItemAttributeValue.fromBoolean(true));
        assertThat(ItemAttributeValue.fromMap(map)).satisfies(v -> {
            assertThat(v.isMap()).isTrue();
            assertThat(v.asMap().get("a").asString()).isEqualTo("foo");
            assertThat(v.asMap().get("b").asBoolean()).isEqualTo(true);
            assertThat(v.type()).isEqualTo(ItemAttributeValueType.MAP);
        });
    }

    @Test
    public void fromGeneratedTypeMethodsCreateCorrectType() {
        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().nul(true).build()))
                .isEqualTo(ItemAttributeValue.nullValue());

        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().s("foo").build()))
                .isEqualTo(ItemAttributeValue.fromString("foo"));

        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().n("1").build()))
                .isEqualTo(ItemAttributeValue.fromNumber("1"));

        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().bool(true).build()))
                .isEqualTo(ItemAttributeValue.fromBoolean(true));

        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().b(SdkBytes.fromUtf8String("foo")).build()))
                .isEqualTo(ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")));

        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().ss(Arrays.asList("foo", "bar")).build()))
                .isEqualTo(ItemAttributeValue.fromSetOfStrings(Arrays.asList("foo", "bar")));

        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().ns(Arrays.asList("1", "2")).build()))
                .isEqualTo(ItemAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2")));

        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder()
                                                                                .bs(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                                                  SdkBytes.fromUtf8String("foo2")))
                                                                                .build()))
                .isEqualTo(ItemAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                           SdkBytes.fromUtf8String("foo2"))));

        List<AttributeValue> l = Arrays.asList(AttributeValue.builder().s("foo").build(),
                                                  AttributeValue.builder().n("1").build());
        List<ItemAttributeValue> list = Arrays.asList(ItemAttributeValue.fromString("foo"), ItemAttributeValue.fromNumber("1"));
        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().l(l).build()))
                .isEqualTo(ItemAttributeValue.fromListOfAttributeValues(list));

        Map<String, AttributeValue> m = new LinkedHashMap<>();
        m.put("foo", AttributeValue.builder().s("foo").build());
        m.put("bar", AttributeValue.builder().n("1").build());
        Map<String, ItemAttributeValue> map = new LinkedHashMap<>();
        map.put("foo", ItemAttributeValue.fromString("foo"));
        map.put("bar", ItemAttributeValue.fromNumber("1"));

        assertThat(ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().m(m).build()))
                .isEqualTo(ItemAttributeValue.fromMap(map));

        assertThat(ItemAttributeValue.fromGeneratedItem(m))
                .isEqualTo(ItemAttributeValue.fromMap(map));
    }

    @Test
    public void emptyAttributeValuesCannotBeConverted() {
        assertThatThrownBy(() -> ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void conversionToGeneratedIsCorrect() {
        List<String> strings = Arrays.asList("foo", "bar");
        List<SdkBytes> bytes = Arrays.asList(SdkBytes.fromUtf8String("foo"), SdkBytes.fromUtf8String("bar"));

        List<ItemAttributeValue> itemAttributes = Arrays.asList(ItemAttributeValue.fromString("foo"), ItemAttributeValue.fromNumber("1"));
        List<AttributeValue> attributes = Arrays.asList(AttributeValue.builder().s("foo").build(), AttributeValue.builder().n("1").build());

        Map<String, AttributeValue> attributeMap = new LinkedHashMap<>();
        attributeMap.put("foo", AttributeValue.builder().s("foo").build());
        attributeMap.put("bar", AttributeValue.builder().n("1").build());
        Map<String, ItemAttributeValue> itemAttributeMap = new LinkedHashMap<>();
        itemAttributeMap.put("foo", ItemAttributeValue.fromString("foo"));
        itemAttributeMap.put("bar", ItemAttributeValue.fromNumber("1"));

        assertThat(ItemAttributeValue.nullValue().toGeneratedAttributeValue().nul()).isEqualTo(true);
        assertThat(ItemAttributeValue.fromString("foo").toGeneratedAttributeValue().s()).isEqualTo("foo");
        assertThat(ItemAttributeValue.fromNumber("1").toGeneratedAttributeValue().n()).isEqualTo("1");
        assertThat(ItemAttributeValue.fromBoolean(false).toGeneratedAttributeValue().bool()).isEqualTo(false);
        assertThat(ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")).toGeneratedAttributeValue().b().asUtf8String()).isEqualTo("foo");
        assertThat(ItemAttributeValue.fromSetOfStrings(strings).toGeneratedAttributeValue().ss()).isEqualTo(strings);
        assertThat(ItemAttributeValue.fromSetOfNumbers(strings).toGeneratedAttributeValue().ns()).isEqualTo(strings);
        assertThat(ItemAttributeValue.fromSetOfBytes(bytes).toGeneratedAttributeValue().bs()).isEqualTo(bytes);
        assertThat(ItemAttributeValue.fromListOfAttributeValues(itemAttributes).toGeneratedAttributeValue().l()).isEqualTo(attributes);
        assertThat(ItemAttributeValue.fromMap(itemAttributeMap).toGeneratedAttributeValue().m()).isEqualTo(attributeMap);
        assertThat(ItemAttributeValue.fromMap(itemAttributeMap).toGeneratedItem()).isEqualTo(attributeMap);
    }

    @Test
    public void conversionToStringIsCorrect() {
        List<String> strings = Arrays.asList("foo", "bar");
        List<SdkBytes> bytes = Arrays.asList(SdkBytes.fromUtf8String("foo"), SdkBytes.fromUtf8String("bar"));

        List<ItemAttributeValue> itemAttributes = Arrays.asList(ItemAttributeValue.fromString("foo"),
                                                                ItemAttributeValue.fromNumber("1"));

        Map<String, ItemAttributeValue> itemAttributeMap = new LinkedHashMap<>();
        itemAttributeMap.put("foo", ItemAttributeValue.fromString("foo"));
        itemAttributeMap.put("bar", ItemAttributeValue.fromNumber("1"));

        assertThat(ItemAttributeValue.nullValue().toString())
                .isEqualTo("ItemAttributeValue(type=NULL, value=null)");

        assertThat(ItemAttributeValue.fromString("foo").toString())
                .isEqualTo("ItemAttributeValue(type=STRING, value=foo)");

        assertThat(ItemAttributeValue.fromNumber("1").toString())
                .isEqualTo("ItemAttributeValue(type=NUMBER, value=1)");

        assertThat(ItemAttributeValue.fromBoolean(false).toString())
                .isEqualTo("ItemAttributeValue(type=BOOLEAN, value=false)");

        assertThat(ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")).toString())
                .isEqualTo("ItemAttributeValue(type=BYTES, value=SdkBytes(bytes=0x666f6f))");

        assertThat(ItemAttributeValue.fromSetOfStrings(strings).toString())
                .isEqualTo("ItemAttributeValue(type=SET_OF_STRINGS, value=[foo, bar])");

        assertThat(ItemAttributeValue.fromSetOfNumbers(strings).toString())
                .isEqualTo("ItemAttributeValue(type=SET_OF_NUMBERS, value=[foo, bar])");

        assertThat(ItemAttributeValue.fromSetOfBytes(bytes).toString())
                .isEqualTo("ItemAttributeValue(type=SET_OF_BYTES, value=[SdkBytes(bytes=0x666f6f), SdkBytes(bytes=0x626172)])");

        assertThat(ItemAttributeValue.fromListOfAttributeValues(itemAttributes).toString())
                .isEqualTo("ItemAttributeValue(type=LIST_OF_ATTRIBUTE_VALUES, value=[ItemAttributeValue(type=STRING, value=foo), " +
                           "ItemAttributeValue(type=NUMBER, value=1)])");

        assertThat(ItemAttributeValue.fromMap(itemAttributeMap).toString())
                .isEqualTo("ItemAttributeValue(type=MAP, value={foo=ItemAttributeValue(type=STRING, value=foo), " +
                           "bar=ItemAttributeValue(type=NUMBER, value=1)})");
    }
}
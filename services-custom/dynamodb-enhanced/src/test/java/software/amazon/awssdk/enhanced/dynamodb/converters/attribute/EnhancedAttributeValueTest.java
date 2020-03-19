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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;

public class EnhancedAttributeValueTest {
    @Test
    public void simpleFromMethodsCreateCorrectTypes() {
        assertThat(EnhancedAttributeValue.nullValue()).satisfies(v -> {
            assertThat(v.isNull()).isTrue();
            assertThat(v.type()).isEqualTo(AttributeValueType.NULL);
        });

        assertThat(EnhancedAttributeValue.fromString("foo")).satisfies(v -> {
            assertThat(v.isString()).isTrue();
            assertThat(v.asString()).isEqualTo("foo");
            assertThat(v.type()).isEqualTo(AttributeValueType.S);
        });

        assertThat(EnhancedAttributeValue.fromNumber("1")).satisfies(v -> {
            assertThat(v.isNumber()).isTrue();
            assertThat(v.asNumber()).isEqualTo("1");
            assertThat(v.type()).isEqualTo(AttributeValueType.N);
        });

        assertThat(EnhancedAttributeValue.fromBoolean(true)).satisfies(v -> {
            assertThat(v.isBoolean()).isTrue();
            assertThat(v.asBoolean()).isEqualTo(true);
            assertThat(v.type()).isEqualTo(AttributeValueType.BOOL);
        });

        assertThat(EnhancedAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo"))).satisfies(v -> {
            assertThat(v.isBytes()).isTrue();
            assertThat(v.asBytes().asUtf8String()).isEqualTo("foo");
            assertThat(v.type()).isEqualTo(AttributeValueType.B);
        });

        assertThat(EnhancedAttributeValue.fromSetOfStrings(Arrays.asList("a", "b"))).satisfies(v -> {
            assertThat(v.isSetOfStrings()).isTrue();
            assertThat(v.asSetOfStrings()).containsExactly("a", "b");
            assertThat(v.type()).isEqualTo(AttributeValueType.SS);
        });

        assertThat(EnhancedAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2"))).satisfies(v -> {
            assertThat(v.isSetOfNumbers()).isTrue();
            assertThat(v.asSetOfNumbers()).containsExactly("1", "2");
            assertThat(v.type()).isEqualTo(AttributeValueType.NS);
        });

        assertThat(EnhancedAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                   SdkBytes.fromUtf8String("foo2")))).satisfies(v -> {
            assertThat(v.isSetOfBytes()).isTrue();
            assertThat(v.asSetOfBytes().get(0).asUtf8String()).isEqualTo("foo");
            assertThat(v.asSetOfBytes().get(1).asUtf8String()).isEqualTo("foo2");
            assertThat(v.type()).isEqualTo(AttributeValueType.BS);
        });

        assertThat(EnhancedAttributeValue.fromListOfAttributeValues(Arrays.asList(EnhancedAttributeValue.fromString("foo"),
                                                                              EnhancedAttributeValue.fromBoolean(true)))).satisfies(v -> {
            assertThat(v.isListOfAttributeValues()).isTrue();
            assertThat(v.asListOfAttributeValues().get(0).asString()).isEqualTo("foo");
            assertThat(v.asListOfAttributeValues().get(1).asBoolean()).isEqualTo(true);
            assertThat(v.type()).isEqualTo(AttributeValueType.L);
        });

        Map<String, EnhancedAttributeValue> map = new LinkedHashMap<>();
        map.put("a", EnhancedAttributeValue.fromString("foo"));
        map.put("b", EnhancedAttributeValue.fromBoolean(true));
        assertThat(EnhancedAttributeValue.fromMap(map)).satisfies(v -> {
            assertThat(v.isMap()).isTrue();
            assertThat(v.asMap().get("a").asString()).isEqualTo("foo");
            assertThat(v.asMap().get("b").asBoolean()).isEqualTo(true);
            assertThat(v.type()).isEqualTo(AttributeValueType.M);
        });
    }

    @Test
    public void fromGeneratedTypeMethodsCreateCorrectType() {
        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().nul(true).build()))
                .isEqualTo(EnhancedAttributeValue.nullValue());

        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s("foo").build()))
                .isEqualTo(EnhancedAttributeValue.fromString("foo"));

        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n("1").build()))
                .isEqualTo(EnhancedAttributeValue.fromNumber("1"));

        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().bool(true).build()))
                .isEqualTo(EnhancedAttributeValue.fromBoolean(true));

        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().b(SdkBytes.fromUtf8String("foo")).build()))
                .isEqualTo(EnhancedAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")));

        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().ss(Arrays.asList("foo", "bar")).build()))
                .isEqualTo(EnhancedAttributeValue.fromSetOfStrings(Arrays.asList("foo", "bar")));

        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().ns(Arrays.asList("1", "2")).build()))
                .isEqualTo(EnhancedAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2")));

        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                                                                                                               .bs(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                                                                                                 SdkBytes.fromUtf8String("foo2")))
                                                                                                                               .build()))
                .isEqualTo(EnhancedAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                           SdkBytes.fromUtf8String("foo2"))));

        List<software.amazon.awssdk.services.dynamodb.model.AttributeValue> l = Arrays.asList(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s("foo").build(),
                                                                                              software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n("1").build());
        List<EnhancedAttributeValue> list = Arrays.asList(EnhancedAttributeValue.fromString("foo"), EnhancedAttributeValue.fromNumber("1"));
        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().l(l).build()))
                .isEqualTo(EnhancedAttributeValue.fromListOfAttributeValues(list));

        Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> m = new LinkedHashMap<>();
        m.put("foo", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s("foo").build());
        m.put("bar", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n("1").build());
        Map<String, EnhancedAttributeValue> map = new LinkedHashMap<>();
        map.put("foo", EnhancedAttributeValue.fromString("foo"));
        map.put("bar", EnhancedAttributeValue.fromNumber("1"));

        assertThat(EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().m(m).build()))
                .isEqualTo(EnhancedAttributeValue.fromMap(map));

        assertThat(EnhancedAttributeValue.fromAttributeValueMap(m))
                .isEqualTo(EnhancedAttributeValue.fromMap(map));
    }

    @Test
    public void emptyAttributeValuesCannotBeConverted() {
        assertThatThrownBy(() -> EnhancedAttributeValue.fromAttributeValue(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void conversionToGeneratedIsCorrect() {
        List<String> strings = Arrays.asList("foo", "bar");
        List<SdkBytes> bytes = Arrays.asList(SdkBytes.fromUtf8String("foo"), SdkBytes.fromUtf8String("bar"));

        List<EnhancedAttributeValue> itemAttributes = Arrays.asList(EnhancedAttributeValue.fromString("foo"), EnhancedAttributeValue.fromNumber("1"));
        List<software.amazon.awssdk.services.dynamodb.model.AttributeValue> attributes = Arrays.asList(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s("foo").build(), software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n("1").build());

        Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> attributeMap = new LinkedHashMap<>();
        attributeMap.put("foo", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s("foo").build());
        attributeMap.put("bar", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n("1").build());
        Map<String, EnhancedAttributeValue> itemAttributeMap = new LinkedHashMap<>();
        itemAttributeMap.put("foo", EnhancedAttributeValue.fromString("foo"));
        itemAttributeMap.put("bar", EnhancedAttributeValue.fromNumber("1"));

        assertThat(EnhancedAttributeValue.nullValue().toAttributeValue().nul()).isEqualTo(true);
        assertThat(EnhancedAttributeValue.fromString("foo").toAttributeValue().s()).isEqualTo("foo");
        assertThat(EnhancedAttributeValue.fromNumber("1").toAttributeValue().n()).isEqualTo("1");
        assertThat(EnhancedAttributeValue.fromBoolean(false).toAttributeValue().bool()).isEqualTo(false);
        assertThat(EnhancedAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")).toAttributeValue().b().asUtf8String()).isEqualTo("foo");
        assertThat(EnhancedAttributeValue.fromSetOfStrings(strings).toAttributeValue().ss()).isEqualTo(strings);
        assertThat(EnhancedAttributeValue.fromSetOfNumbers(strings).toAttributeValue().ns()).isEqualTo(strings);
        assertThat(EnhancedAttributeValue.fromSetOfBytes(bytes).toAttributeValue().bs()).isEqualTo(bytes);
        assertThat(EnhancedAttributeValue.fromListOfAttributeValues(itemAttributes).toAttributeValue().l()).isEqualTo(attributes);
        assertThat(EnhancedAttributeValue.fromMap(itemAttributeMap).toAttributeValue().m()).isEqualTo(attributeMap);
        assertThat(EnhancedAttributeValue.fromMap(itemAttributeMap).toAttributeValueMap()).isEqualTo(attributeMap);
    }

    @Test
    public void conversionToStringIsCorrect() {
        List<String> strings = Arrays.asList("foo", "bar");
        List<SdkBytes> bytes = Arrays.asList(SdkBytes.fromUtf8String("foo"), SdkBytes.fromUtf8String("bar"));

        List<EnhancedAttributeValue> itemAttributes = Arrays.asList(EnhancedAttributeValue.fromString("foo"),
                                                                EnhancedAttributeValue.fromNumber("1"));

        Map<String, EnhancedAttributeValue> itemAttributeMap = new LinkedHashMap<>();
        itemAttributeMap.put("foo", EnhancedAttributeValue.fromString("foo"));
        itemAttributeMap.put("bar", EnhancedAttributeValue.fromNumber("1"));

        assertThat(EnhancedAttributeValue.nullValue().toString())
                .isEqualTo("EnhancedAttributeValue(type=NULL, value=null)");

        assertThat(EnhancedAttributeValue.fromString("foo").toString())
                .isEqualTo("EnhancedAttributeValue(type=S, value=foo)");

        assertThat(EnhancedAttributeValue.fromNumber("1").toString())
                .isEqualTo("EnhancedAttributeValue(type=N, value=1)");

        assertThat(EnhancedAttributeValue.fromBoolean(false).toString())
                .isEqualTo("EnhancedAttributeValue(type=BOOL, value=false)");

        assertThat(EnhancedAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")).toString())
                .isEqualTo("EnhancedAttributeValue(type=B, value=SdkBytes(bytes=0x666f6f))");

        assertThat(EnhancedAttributeValue.fromSetOfStrings(strings).toString())
                .isEqualTo("EnhancedAttributeValue(type=SS, value=[foo, bar])");

        assertThat(EnhancedAttributeValue.fromSetOfNumbers(strings).toString())
                .isEqualTo("EnhancedAttributeValue(type=NS, value=[foo, bar])");

        assertThat(EnhancedAttributeValue.fromSetOfBytes(bytes).toString())
                .isEqualTo("EnhancedAttributeValue(type=BS, value=[SdkBytes(bytes=0x666f6f), SdkBytes(bytes=0x626172)])");

        assertThat(EnhancedAttributeValue.fromListOfAttributeValues(itemAttributes).toString())
                .isEqualTo("EnhancedAttributeValue(type=L, value=[EnhancedAttributeValue(type=S, value=foo), " +
                           "EnhancedAttributeValue(type=N, value=1)])");

        assertThat(EnhancedAttributeValue.fromMap(itemAttributeMap).toString())
                .isEqualTo("EnhancedAttributeValue(type=M, value={foo=EnhancedAttributeValue(type=S, value=foo), " +
                           "bar=EnhancedAttributeValue(type=N, value=1)})");
    }
}

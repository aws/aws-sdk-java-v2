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

package software.amazon.awssdk.enhanced.dynamodb.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.RequestItem;
import software.amazon.awssdk.enhanced.dynamodb.model.ResponseItem;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

public class DefaultConverterChainTest {
    private static final DefaultConverterChain CHAIN = DefaultConverterChain.create();

    @Test
    public void fromStringConversionWorks() {
        assertThat(toAttributeValue("foo").asString()).isEqualTo("foo");
    }

    @Test
    public void fromIntegerConversionWorks() {
        assertThat(toAttributeValue(1).asNumber()).isEqualTo("1");
    }

    @Test
    public void fromInstantConversionWorks() {
        Instant now = Instant.now();
        assertThat(toAttributeValue(now).asNumber()).isEqualTo(Long.toString(now.toEpochMilli()));
    }

    @Test
    public void fromItemAttributeValueWorks() {
        assertThat(toAttributeValue(ItemAttributeValue.nullValue())).isEqualTo(ItemAttributeValue.nullValue());
    }

    @Test
    public void fromListWorks() {
        List<Object> list = Arrays.asList("foo", 1);
        ItemAttributeValue attributeValue = toAttributeValue(list);
        assertThat(attributeValue.asListOfAttributeValues()).hasSize(2);
        assertThat(attributeValue.asListOfAttributeValues().get(0).asString()).isEqualTo("foo");
        assertThat(attributeValue.asListOfAttributeValues().get(1).asNumber()).isEqualTo("1");
    }

    @Test
    public void fromMapWorks() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("foo", "bar");
        map.put(1, 2);

        ItemAttributeValue attributeValue = toAttributeValue(map);
        assertThat(attributeValue.asMap()).hasSize(2);
        assertThat(attributeValue.asMap().get("foo").asString()).isEqualTo("bar");
        assertThat(attributeValue.asMap().get("1").asNumber()).isEqualTo("2");
    }

    @Test
    public void fromRequestItemWorks() {
        assertThat(toAttributeValue(RequestItem.builder().putAttribute("foo", 1).build()).asMap())
                .hasSize(1)
                .containsEntry("foo", ItemAttributeValue.fromNumber("1"));
    }

    @Test
    public void toStringConversionWorks() {
        assertThat(fromAttributeValue(String.class, ItemAttributeValue.nullValue())).isNull();

        assertThat(fromAttributeValue(String.class, ItemAttributeValue.fromString("foo"))).isEqualTo("foo");

        assertThat(fromAttributeValue(String.class, ItemAttributeValue.fromNumber("1"))).isEqualTo("1");

        assertThat(fromAttributeValue(String.class, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo"))))
                .isEqualTo("0x666f6f");

        assertThat(fromAttributeValue(String.class, ItemAttributeValue.fromBoolean(true))).isEqualTo("true");

        assertThat(fromAttributeValue(String.class, ItemAttributeValue.fromSetOfStrings(Arrays.asList("foo", "bar"))))
                .isEqualTo("[foo, bar]");

        assertThat(fromAttributeValue(String.class, ItemAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2"))))
                .isEqualTo("[1, 2]");

        assertThat(fromAttributeValue(String.class, ItemAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                                                    SdkBytes.fromUtf8String("bar")))))
                .isEqualTo("[0x666f6f, 0x626172]");

        assertThat(fromAttributeValue(String.class, ItemAttributeValue.fromListOfAttributeValues(list())))
                .isEqualTo("[foo, 0x666f6f]");

        assertThat(fromAttributeValue(String.class, ItemAttributeValue.fromMap(map())))
                .isEqualTo("{foo=bar, foo2=0x666f6f}");
    }

    @Test
    public void toIntegerConversionWorks() {
        assertThat(fromAttributeValue(Integer.class, ItemAttributeValue.nullValue())).isNull();

        assertThat(fromAttributeValue(Integer.class, ItemAttributeValue.fromString("2"))).isEqualTo(2);
        assertFromAttributeValueFails(Integer.class, ItemAttributeValue.fromString("1.5"), NumberFormatException.class);

        assertThat(fromAttributeValue(Integer.class, ItemAttributeValue.fromNumber("2"))).isEqualTo(2);
        assertFromAttributeValueFails(Integer.class, ItemAttributeValue.fromNumber("1.5"), NumberFormatException.class);

        assertFromAttributeValueFails(Integer.class, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("")));

        assertFromAttributeValueFails(Integer.class, ItemAttributeValue.fromBoolean(true));

        assertFromAttributeValueFails(Integer.class, ItemAttributeValue.fromSetOfStrings(Arrays.asList("foo", "bar")));

        assertFromAttributeValueFails(Integer.class, ItemAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2")));

        assertFromAttributeValueFails(Integer.class, ItemAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                                                     SdkBytes.fromUtf8String("bar"))));

        assertFromAttributeValueFails(Integer.class, ItemAttributeValue.fromListOfAttributeValues(list()));

        assertFromAttributeValueFails(Integer.class, ItemAttributeValue.fromMap(map()));
    }

    @Test
    public void toInstantConversionWorks() {
        Instant instant = Instant.parse("2007-12-03T10:15:30.00Z");

        assertThat(fromAttributeValue(Instant.class, ItemAttributeValue.nullValue())).isNull();

        assertThat(fromAttributeValue(Instant.class, ItemAttributeValue.fromString(instant.toString()))).isEqualTo(instant);
        assertFromAttributeValueFails(Instant.class, ItemAttributeValue.fromString("foo"), DateTimeParseException.class);

        assertThat(fromAttributeValue(Instant.class, ItemAttributeValue.fromNumber(Long.toString(instant.toEpochMilli()))))
                .isEqualTo(instant);
        assertFromAttributeValueFails(Instant.class, ItemAttributeValue.fromNumber("foo"), NumberFormatException.class);

        assertFromAttributeValueFails(Instant.class, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("")));

        assertFromAttributeValueFails(Instant.class, ItemAttributeValue.fromBoolean(true));

        assertFromAttributeValueFails(Instant.class, ItemAttributeValue.fromSetOfStrings(Arrays.asList("foo", "bar")));

        assertFromAttributeValueFails(Instant.class, ItemAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2")));

        assertFromAttributeValueFails(Instant.class, ItemAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                                                     SdkBytes.fromUtf8String("bar"))));

        assertFromAttributeValueFails(Instant.class, ItemAttributeValue.fromListOfAttributeValues(list()));

        assertFromAttributeValueFails(Instant.class, ItemAttributeValue.fromMap(map()));
    }

    @Test
    public void toItemAttributeValueWorks() {
        assertThat(fromAttributeValue(ItemAttributeValue.class, ItemAttributeValue.nullValue())).isEqualTo(ItemAttributeValue.nullValue());
    }

    @Test
    public void toListConversionWorks() {
        TypeToken<List<String>> targetType = TypeToken.listOf(String.class);

        assertThat(fromAttributeValue(targetType, ItemAttributeValue.nullValue())).isNull();

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromString("foo"));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromNumber("foo"));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("")));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromBoolean(true));

        assertThat(fromAttributeValue(targetType, ItemAttributeValue.fromSetOfStrings(Arrays.asList("foo", "bar"))))
                .containsExactly("foo", "bar");

        assertThat(fromAttributeValue(targetType, ItemAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2"))))
                .containsExactly("1", "2");

        assertThat(fromAttributeValue(targetType,
                                      ItemAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                                      SdkBytes.fromUtf8String("bar")))))
                .containsExactly("0x666f6f", "0x626172");

        assertThat(fromAttributeValue(targetType, ItemAttributeValue.fromListOfAttributeValues(list())))
                .containsExactly("foo", "0x666f6f");

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromMap(map()));
    }

    @Test
    public void toListSubtypeConversionWorks() {

        assertThat(fromAttributeValue(new TypeToken<ArrayList<String>>(){},
                                      ItemAttributeValue.fromSetOfStrings(Arrays.asList("foo", "bar"))))
                .isInstanceOf(ArrayList.class)
                .containsExactly("foo", "bar");
    }

    @Test
    public void toMapConversionWorks() {
        TypeToken<Map<String, String>> targetType = TypeToken.mapOf(String.class, String.class);

        assertThat(fromAttributeValue(targetType, ItemAttributeValue.nullValue())).isNull();

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromString("2"));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromNumber("2"));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("")));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromBoolean(true));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromSetOfStrings(Arrays.asList("foo", "bar")));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2")));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                                                  SdkBytes.fromUtf8String("bar"))));

        assertFromAttributeValueFails(targetType, ItemAttributeValue.fromListOfAttributeValues(list()));

        assertThat(fromAttributeValue(targetType, ItemAttributeValue.fromMap(map())))
                .hasSize(2)
                .containsEntry("foo", "bar")
                .containsEntry("foo2", "0x666f6f");
    }

    @Test
    public void toResponseItemConversionWorks() {
        assertThat(fromAttributeValue(ResponseItem.class, ItemAttributeValue.nullValue())).isNull();

        assertFromAttributeValueFails(ResponseItem.class, ItemAttributeValue.fromString("2"));

        assertFromAttributeValueFails(ResponseItem.class, ItemAttributeValue.fromNumber("2"));

        assertFromAttributeValueFails(ResponseItem.class, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("")));

        assertFromAttributeValueFails(ResponseItem.class, ItemAttributeValue.fromBoolean(true));

        assertFromAttributeValueFails(ResponseItem.class, ItemAttributeValue.fromSetOfStrings(Arrays.asList("foo", "bar")));

        assertFromAttributeValueFails(ResponseItem.class, ItemAttributeValue.fromSetOfNumbers(Arrays.asList("1", "2")));

        assertFromAttributeValueFails(ResponseItem.class, ItemAttributeValue.fromSetOfBytes(Arrays.asList(SdkBytes.fromUtf8String("foo"),
                                                                                                  SdkBytes.fromUtf8String("bar"))));

        assertFromAttributeValueFails(ResponseItem.class, ItemAttributeValue.fromListOfAttributeValues(list()));

        assertThat(fromAttributeValue(ResponseItem.class, ItemAttributeValue.fromMap(map())).attributes()).satisfies(m -> {
            assertThat(m).hasSize(2);
            assertThat(m.get("foo").asString()).isEqualTo("bar");
            assertThat(m.get("foo2").asString()).isEqualTo("0x666f6f");
        });
    }

    @Test
    public void toMapSubtypeConversionWorks() {

        assertThat(fromAttributeValue(new TypeToken<HashMap<String, String>>(){},
                                      ItemAttributeValue.fromMap(map())))
                .isInstanceOf(HashMap.class)
                .hasSize(2)
                .containsEntry("foo", "bar")
                .containsEntry("foo2", "0x666f6f");
    }

    private <T> T fromAttributeValue(Class<T> targetType, ItemAttributeValue value) {
        return fromAttributeValue(TypeToken.from(targetType), value);
    }

    private <T> T fromAttributeValue(TypeToken<T> targetType, ItemAttributeValue value) {
        ConversionContext context = ConversionContext.builder().converter(CHAIN).build();
        return targetType.rawClass().cast(CHAIN.fromAttributeValue(value, targetType, context));
    }

    private void assertFromAttributeValueFails(Class<?> targetType, ItemAttributeValue value) {
        assertFromAttributeValueFails(targetType, value, IllegalStateException.class);
    }

    private void assertFromAttributeValueFails(TypeToken<?> targetType, ItemAttributeValue value) {
        assertFromAttributeValueFails(targetType, value, IllegalStateException.class);
    }

    private void assertFromAttributeValueFails(Class<?> targetType, ItemAttributeValue value, Class<?> exceptionType) {
        assertFromAttributeValueFails(TypeToken.from(targetType), value, exceptionType);
    }

    private void assertFromAttributeValueFails(TypeToken<?> targetType, ItemAttributeValue value, Class<?> exceptionType) {
        assertThatThrownBy(() -> fromAttributeValue(targetType, value)).isInstanceOf(exceptionType);
    }

    private ItemAttributeValue toAttributeValue(Object input) {
        ConversionContext context = ConversionContext.builder().converter(CHAIN).build();
        return CHAIN.toAttributeValue(input, context);
    }

    private List<ItemAttributeValue> list() {
        return Arrays.asList(ItemAttributeValue.fromString("foo"),
                             ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")));
    }

    private Map<String, ItemAttributeValue> map() {
        Map<String, ItemAttributeValue> map = new LinkedHashMap<>();
        map.put("foo", ItemAttributeValue.fromString("bar"));
        map.put("foo2", ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")));
        return map;
    }
}
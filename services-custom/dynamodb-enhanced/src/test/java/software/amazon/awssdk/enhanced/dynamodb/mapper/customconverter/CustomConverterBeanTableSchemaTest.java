/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.mapper.customconverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CustomType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CustomConverterBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class CustomConverterBeanTableSchemaTest {

    @Test
    public void serializeBean_withCustomSet_CorrectlyPerformSerialization() {
        CustomConverterBean customConverterBean = new CustomConverterBean()
            .setId("1")
            .setCustomSet(buildCustomSet());

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(customConverterBean, true);

        //expected result items
        AttributeValue expectedCustomSet = AttributeValue.builder().ss("{\"stringAttribute\":\"test2\","
                                                                       + "\"booleanAttribute\":false,\"integerAttribute\":2,"
                                                                       + "\"doubleAttribute\":200.0,"
                                                                       + "\"localDateAttribute\":[2025,5,5]}",

                                                                       "{\"stringAttribute\":\"test1\","
                                                                       + "\"booleanAttribute\":true,\"integerAttribute\":1,"
                                                                       + "\"doubleAttribute\":100.0,"
                                                                       + "\"localDateAttribute\":[2025,1,1]}").build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("1")));
        assertThat(itemMap, hasEntry("customSet", expectedCustomSet));
    }

    @Test
    public void deserializeBean_withCustomSet_CorrectlyPerformDeserialization() {
        AttributeValue customSetAttribute = AttributeValue.builder().ss("{\"stringAttribute\":\"test1\","
                                                                        + "\"booleanAttribute\":true,\"integerAttribute\":1,"
                                                                        + "\"doubleAttribute\":100.0,"
                                                                        + "\"localDateAttribute\":[2025,1,1]}",

                                                                        "{\"stringAttribute\":\"test2\","
                                                                        + "\"booleanAttribute\":false,\"integerAttribute\":2,"
                                                                        + "\"doubleAttribute\":200.0,"
                                                                        + "\"localDateAttribute\":[2025,5,5]}").build();

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue("1"));
        itemMap.put("customSet", customSetAttribute);

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        CustomConverterBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getCustomSet(), equalTo(buildCustomSet()));
    }

    @Test
    public void serializeBean_withCustomList_CorrectlyPerformSerialization() {
        CustomConverterBean customConverterBean = new CustomConverterBean()
            .setId("1")
            .setCustomList(buildCustomList());

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(customConverterBean, true);

        //expected result items
        AttributeValue expectedCustomList = AttributeValue.builder().l(new ArrayList<>(Arrays.asList(
            AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
                put("booleanAttribute", AttributeValue.builder().bool(Boolean.TRUE).build());
                put("integerAttribute", AttributeValue.builder().n("1").build());
                put("doubleAttribute", AttributeValue.builder().n("100.0").build());
                put("stringAttribute", AttributeValue.builder().s("test1").build());
                put("localDateAttribute", AttributeValue.builder().s("2025-01-01").build());
            }}).build(),

            AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
                put("booleanAttribute", AttributeValue.builder().bool(Boolean.FALSE).build());
                put("integerAttribute", AttributeValue.builder().n("2").build());
                put("doubleAttribute", AttributeValue.builder().n("200.0").build());
                put("stringAttribute", AttributeValue.builder().s("test2").build());
                put("localDateAttribute", AttributeValue.builder().s("2025-05-05").build());
            }}).build()
        ))).build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("1")));
        assertThat(itemMap, hasEntry("customList", expectedCustomList));
    }

    @Test
    public void deserializeBean_withCustomListCorrectlyPerformDeserialization() {
        AttributeValue customListAttribute = AttributeValue.builder().l(new ArrayList<>(Arrays.asList(
            AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
                put("booleanAttribute", AttributeValue.builder().bool(Boolean.TRUE).build());
                put("integerAttribute", AttributeValue.builder().n("1").build());
                put("doubleAttribute", AttributeValue.builder().n("100.0").build());
                put("stringAttribute", AttributeValue.builder().s("test1").build());
                put("localDateAttribute", AttributeValue.builder().s("2025-01-01").build());
            }}).build(),

            AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
                put("booleanAttribute", AttributeValue.builder().bool(Boolean.FALSE).build());
                put("integerAttribute", AttributeValue.builder().n("2").build());
                put("doubleAttribute", AttributeValue.builder().n("200.0").build());
                put("stringAttribute", AttributeValue.builder().s("test2").build());
                put("localDateAttribute", AttributeValue.builder().s("2025-05-05").build());
            }}).build()
        ))).build();

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue("1"));
        itemMap.put("customList", customListAttribute);

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        CustomConverterBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getCustomList(), equalTo(buildCustomList()));
    }

    @Test
    public void serializeBean_withCustomMapKey_CorrectlyPerformSerialization() {
        CustomConverterBean customConverterBean = new CustomConverterBean()
            .setId("1")
            .setCustomKeyMap(buildMapWithCustomKey());

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(customConverterBean, true);

        //expected result items
        AttributeValue expectedCustomKeyMap = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("{\"stringAttribute\":\"test1\",\"booleanAttribute\":true,\"integerAttribute\":1,"
                + "\"doubleAttribute\":100.0,\"localDateAttribute\":[2025,1,1]}",
                AttributeValue.builder().s("mapValue").build());
        }}).build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("1")));
        assertThat(itemMap, hasEntry("customKeyMap", expectedCustomKeyMap));
    }

    @Test
    public void deserializeBean_withCustomMapKey_CorrectlyPerformDeserialization() {
        AttributeValue customKeyMapAttribute = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("{\"stringAttribute\":\"test1\",\"booleanAttribute\":true,\"integerAttribute\":1,"
                + "\"doubleAttribute\":100.0,\"localDateAttribute\":[2025,1,1]}",
                AttributeValue.builder().s("mapValue").build());
        }}).build();

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue("1"));
        itemMap.put("customKeyMap", customKeyMapAttribute);

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        CustomConverterBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getCustomKeyMap(), equalTo(buildMapWithCustomKey()));
    }

    @Test
    public void serializeBean_withCustomMapValue_CorrectlyPerformSerialization() {
        CustomConverterBean customConverterBean = new CustomConverterBean()
            .setId("1")
            .setCustomValueMap(buildMapWithCustomValue());

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(customConverterBean, true);

        //expected result items
        AttributeValue expectedCustomValueMap = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("mapKey",
                AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
                    put("booleanAttribute", AttributeValue.builder().bool(Boolean.TRUE).build());
                    put("integerAttribute", AttributeValue.builder().n("1").build());
                    put("doubleAttribute", AttributeValue.builder().n("100.0").build());
                    put("stringAttribute", AttributeValue.builder().s("test1").build());
                    put("localDateAttribute", AttributeValue.builder().s("2025-01-01").build());
                }}).build());
        }}).build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("1")));
        assertThat(itemMap, hasEntry("customValueMap", expectedCustomValueMap));
    }

    @Test
    public void deserializeBean_withCustomMapValue_CorrectlyPerformDeserialization() {
        AttributeValue customValueMapAttribute = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("mapKey",
                AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
                    put("booleanAttribute", AttributeValue.builder().bool(Boolean.TRUE).build());
                    put("integerAttribute", AttributeValue.builder().n("1").build());
                    put("doubleAttribute", AttributeValue.builder().n("100.0").build());
                    put("stringAttribute", AttributeValue.builder().s("test1").build());
                    put("localDateAttribute", AttributeValue.builder().s("2025-01-01").build());
                }}).build());
        }}).build();

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue("1"));
        itemMap.put("customValueMap", customValueMapAttribute);

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        CustomConverterBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getCustomValueMap(), equalTo(buildMapWithCustomValue()));
    }

    @Test
    public void serializeBean_withStringsMap_CorrectlyPerformSerialization() {
        CustomConverterBean customConverterBean = new CustomConverterBean()
            .setId("1")
            .setLocalDate(LocalDate.of(2025, 1, 1))
            .setStringsMap(buildStringsMap());

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(customConverterBean, true);

        //expected result items
        AttributeValue expectedLocalDate = AttributeValue.builder().s("2025-01-01").build();

        AttributeValue expectedStringsMap = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("stringMapAttribute1", AttributeValue.builder().s("mapValue1").build());
            put("stringMapAttribute2", AttributeValue.builder().s("mapValue2").build());
            put("stringMapAttribute3", AttributeValue.builder().s("mapValue3").build());
        }}).build();

        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("1")));
        assertThat(itemMap, hasEntry("localDate", expectedLocalDate));
        assertThat(itemMap, hasEntry("stringsMap", expectedStringsMap));
    }

    @Test
    public void deserializeBean_withStringsMap_CorrectlyPerformDeserialization() {
        AttributeValue localDateAttribute = AttributeValue.builder().s("2025-01-01").build();
        AttributeValue stringsMapAttribute = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("stringMapAttribute1", AttributeValue.builder().s("mapValue1").build());
            put("stringMapAttribute2", AttributeValue.builder().s("mapValue2").build());
            put("stringMapAttribute3", AttributeValue.builder().s("mapValue3").build());
        }}).build();

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue("1"));
        itemMap.put("localDate", localDateAttribute);
        itemMap.put("stringsMap", stringsMapAttribute);

        BeanTableSchema<CustomConverterBean> beanTableSchema = BeanTableSchema.create(CustomConverterBean.class);
        CustomConverterBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getLocalDate(), is(LocalDate.of(2025, 1,1)));
        assertThat(result.getStringsMap(), equalTo(buildStringsMap()));
    }

    private Set<CustomType> buildCustomSet() {
        return new HashSet<>(Arrays.asList(buildFirstCustomElement(), buildSecondCustomElement()));
    }

    private List<CustomType> buildCustomList() {
        return new ArrayList<>(Arrays.asList(buildFirstCustomElement(), buildSecondCustomElement()));
    }

    private Map<CustomType, String> buildMapWithCustomKey() {
        return new HashMap<CustomType, String>() {{
            put(buildFirstCustomElement(), "mapValue");
        }};
    }

    private Map<String, CustomType> buildMapWithCustomValue() {
        return new HashMap<String, CustomType>() {{
            put("mapKey", buildFirstCustomElement());
        }};
    }

    private Map<String, String> buildStringsMap() {
        return new HashMap<String, String>() {{
            put("stringMapAttribute1", "mapValue1");
            put("stringMapAttribute2", "mapValue2");
            put("stringMapAttribute3", "mapValue3");
        }};
    }

    private CustomType buildFirstCustomElement() {
      return new CustomType()
            .setBooleanAttribute(Boolean.TRUE)
            .setIntegerAttribute(1)
            .setDoubleAttribute(100.0)
            .setStringAttribute("test1")
            .setLocalDateAttribute(LocalDate.of(2025, 1, 1));
    }

    private CustomType buildSecondCustomElement() {
       return new CustomType()
            .setBooleanAttribute(Boolean.FALSE)
            .setIntegerAttribute(2)
            .setDoubleAttribute(200.0)
            .setStringAttribute("test2")
            .setLocalDateAttribute(LocalDate.of(2025, 5, 5));
    }
}

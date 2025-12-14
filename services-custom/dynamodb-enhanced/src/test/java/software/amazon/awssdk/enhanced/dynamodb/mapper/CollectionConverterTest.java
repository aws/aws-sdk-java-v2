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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CustomType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CollectionBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class CollectionConverterTest {

    private static final String ID = "1";
    private BeanTableSchema<CollectionBean> beanTableSchema;

    @Before
    public void setup() {
        beanTableSchema = BeanTableSchema.create(CollectionBean.class);
    }

    @Test
    public void serializeBean_withCustomTypeSet() {
        CollectionBean bean = new CollectionBean()
            .setId(ID)
            .setCustomTypeSet(buildCustomTypeSet());

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(bean, true);

        AttributeValue expectedSet = AttributeValue.builder().ss("{\"stringAttribute\":\"test2\","
                                                                 + "\"booleanAttribute\":false,\"integerAttribute\":2,"
                                                                 + "\"doubleAttribute\":200.0,"
                                                                 + "\"localDateAttribute\":[2025,5,5]}",

                                                                 "{\"stringAttribute\":\"test1\","
                                                                 + "\"booleanAttribute\":true,\"integerAttribute\":1,"
                                                                 + "\"doubleAttribute\":100.0,"
                                                                 + "\"localDateAttribute\":[2025,1,1]}").build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue(ID)));
        assertThat(itemMap, hasEntry("customTypeSet", expectedSet));
    }

    @Test
    public void deserializeBean_withCustomTypeSet() {
        AttributeValue setAttribute = AttributeValue.builder().ss("{\"stringAttribute\":\"test1\","
                                                                  + "\"booleanAttribute\":true,\"integerAttribute\":1,"
                                                                  + "\"doubleAttribute\":100.0,"
                                                                  + "\"localDateAttribute\":[2025,1,1]}",

                                                                  "{\"stringAttribute\":\"test2\","
                                                                  + "\"booleanAttribute\":false,\"integerAttribute\":2,"
                                                                  + "\"doubleAttribute\":200.0,"
                                                                  + "\"localDateAttribute\":[2025,5,5]}").build();

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue(ID));
        itemMap.put("customTypeSet", setAttribute);

        CollectionBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getCustomTypeSet(), equalTo(buildCustomTypeSet()));
    }

    @Test
    public void serializeBean_withCustomTypeList() {
        CollectionBean bean = new CollectionBean()
            .setId(ID)
            .setCustomTypeList(buildCustomTypeList());

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(bean, true);

        AttributeValue expectedList = AttributeValue.builder().l(new ArrayList<>(Arrays.asList(
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
        assertThat(itemMap, hasEntry("id", stringValue(ID)));
        assertThat(itemMap, hasEntry("customTypeList", expectedList));
    }

    @Test
    public void deserializeBean_withCustomTypeList() {
        AttributeValue listAttribute = AttributeValue.builder().l(new ArrayList<>(Arrays.asList(
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
        itemMap.put("id", stringValue(ID));
        itemMap.put("customTypeList", listAttribute);

        CollectionBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getCustomTypeList(), equalTo(buildCustomTypeList()));
    }

    @Test
    public void serializeBean_withCustomMapKey() {
        CollectionBean bean = new CollectionBean()
            .setId(ID)
            .setCustomKeyMap(buildMapWithCustomKey());

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(bean, true);

        AttributeValue expectedMap = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("{\"stringAttribute\":\"test1\",\"booleanAttribute\":true,\"integerAttribute\":1,"
                + "\"doubleAttribute\":100.0,\"localDateAttribute\":[2025,1,1]}",
                AttributeValue.builder().s("mapValue").build());
        }}).build();

        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue(ID)));
        assertThat(itemMap, hasEntry("customKeyMap", expectedMap));
    }

    @Test
    public void deserializeBean_withCustomMapKey() {
        AttributeValue customKeyMapAttribute = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("{\"stringAttribute\":\"test1\",\"booleanAttribute\":true,\"integerAttribute\":1,"
                + "\"doubleAttribute\":100.0,\"localDateAttribute\":[2025,1,1]}",
                AttributeValue.builder().s("mapValue").build());
        }}).build();

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue(ID));
        itemMap.put("customKeyMap", customKeyMapAttribute);

        CollectionBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getCustomKeyMap(), equalTo(buildMapWithCustomKey()));
    }

    @Test
    public void serializeBean_withCustomMapValue() {
        CollectionBean bean = new CollectionBean()
            .setId(ID)
            .setCustomValueMap(buildMapWithCustomValue());

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(bean, true);

        AttributeValue expectedMap = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
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
        assertThat(itemMap, hasEntry("id", stringValue(ID)));
        assertThat(itemMap, hasEntry("customValueMap", expectedMap));
    }

    @Test
    public void deserializeBean_withCustomMapValue() {
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
        itemMap.put("id", stringValue(ID));
        itemMap.put("customValueMap", customValueMapAttribute);

        CollectionBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getCustomValueMap(), equalTo(buildMapWithCustomValue()));
    }

    @Test
    public void serializeBean_withStringsMap() {
        CollectionBean bean = new CollectionBean()
            .setId(ID)
            .setLocalDate(LocalDate.of(2025, 1, 1))
            .setStringsMap(buildStringsMap());

        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(bean, true);

        AttributeValue expectedLocalDate = AttributeValue.builder().s("2025-01-01").build();

        AttributeValue expectedStringsMap = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("stringMapAttribute1", AttributeValue.builder().s("mapValue1").build());
            put("stringMapAttribute2", AttributeValue.builder().s("mapValue2").build());
            put("stringMapAttribute3", AttributeValue.builder().s("mapValue3").build());
        }}).build();

        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue(ID)));
        assertThat(itemMap, hasEntry("localDate", expectedLocalDate));
        assertThat(itemMap, hasEntry("stringsMap", expectedStringsMap));
    }

    @Test
    public void deserializeBean_withStringsMap() {
        AttributeValue localDateAttribute = AttributeValue.builder().s("2025-01-01").build();
        AttributeValue stringsMapAttribute = AttributeValue.builder().m(new HashMap<String, AttributeValue>() {{
            put("stringMapAttribute1", AttributeValue.builder().s("mapValue1").build());
            put("stringMapAttribute2", AttributeValue.builder().s("mapValue2").build());
            put("stringMapAttribute3", AttributeValue.builder().s("mapValue3").build());
        }}).build();

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue(ID));
        itemMap.put("localDate", localDateAttribute);
        itemMap.put("stringsMap", stringsMapAttribute);

        CollectionBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("1"));
        assertThat(result.getLocalDate(), is(LocalDate.of(2025, 1, 1)));
        assertThat(result.getStringsMap(), equalTo(buildStringsMap()));
    }


    private Set<CustomType> buildCustomTypeSet() {
        return new HashSet<>(Arrays.asList(buildFirstCustomElement(), buildSecondCustomElement()));
    }

    private List<CustomType> buildCustomTypeList() {
        return new ArrayList<>(Arrays.asList(buildFirstCustomElement(), buildSecondCustomElement()));
    }

    private Map<CustomType, String> buildMapWithCustomKey() {
        Map<CustomType, String> customKeyMap = new HashMap<>();
        customKeyMap.put(buildFirstCustomElement(), "mapValue");
        return customKeyMap;
    }

    private Map<String, CustomType> buildMapWithCustomValue() {
        Map<String, CustomType> customValueMap = new HashMap<>();
        customValueMap.put("mapKey", buildFirstCustomElement());
        return customValueMap;
    }

    private Map<String, String> buildStringsMap() {
        Map<String, String> stringsMap = new HashMap<>();
        stringsMap.put("stringMapAttribute1", "mapValue1");
        stringsMap.put("stringMapAttribute2", "mapValue2");
        stringsMap.put("stringMapAttribute3", "mapValue3");
        return stringsMap;
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

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

package software.amazon.awssdk.enhanced.dynamodb.mapper.flattenmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CompositeRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FlattenRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NestedRecordWithUpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.flattenmap.FlattenMapAndFlattenRecordBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.flattenmap.FlattenMapInvalidBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.flattenmap.FlattenMapValidBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class FlattenMapBeanTableSchemaTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void serializeBean_withFlattenMapAndIgnoreNulls_successfullyFlattensAndOmitNulls() {
        FlattenMapValidBean bean = new FlattenMapValidBean();
        bean.setRootAttribute1("rootValue1");
        bean.setRootAttribute2("rootValue2");

        bean.setAttributesMap(new HashMap<String, String>() {{
            put("mapAttribute1", "mapValue1");
            put("mapAttribute2", null);
            put("mapAttribute3", null);
        }});

        BeanTableSchema<FlattenMapValidBean> beanTableSchema = BeanTableSchema.create(FlattenMapValidBean.class);
        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(bean, true);

        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("rootAttribute1", stringValue("rootValue1")));
        assertThat(itemMap, hasEntry("rootAttribute2", stringValue("rootValue2")));
        assertThat(itemMap, hasEntry("mapAttribute1", stringValue("mapValue1")));
    }

    @Test
    public void serializeBean_withFlattenMapAndNotIgnoringNulls_successfullyFlattensAndSetNullAttributes() {
        FlattenMapValidBean bean = new FlattenMapValidBean();
        bean.setRootAttribute1("rootValue1");
        bean.setRootAttribute2("rootValue2");

        bean.setAttributesMap(new HashMap<String, String>() {{
            put("mapAttribute1", "mapValue1");
            put("mapAttribute2", null);
            put("mapAttribute3", null);
        }});

        BeanTableSchema<FlattenMapValidBean> beanTableSchema = BeanTableSchema.create(FlattenMapValidBean.class);
        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(bean, false);

        assertThat(itemMap.size(), is(6));
        assertThat(itemMap, hasEntry("rootAttribute1", stringValue("rootValue1")));
        assertThat(itemMap, hasEntry("rootAttribute2", stringValue("rootValue2")));
        assertThat(itemMap, hasEntry("mapAttribute1", stringValue("mapValue1")));
        assertThat(itemMap, hasEntry("mapAttribute2", AttributeValue.builder().build()));
        assertThat(itemMap, hasEntry("mapAttribute3", AttributeValue.builder().build()));
    }

    @Test
    public void serializeBean_withFlattenMapAndFlattenRecord_successfullyFlattens() {
        FlattenMapAndFlattenRecordBean bean = createFlattenMapAndFlattenRecordBean();

        BeanTableSchema<FlattenMapAndFlattenRecordBean> beanTableSchema = BeanTableSchema.create(FlattenMapAndFlattenRecordBean.class);
        Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(bean, true);

        assertThat(itemMap.size(), is(6));
        assertThat(itemMap, hasEntry("rootAttribute1", stringValue("rootValue1")));
        assertThat(itemMap, hasEntry("rootAttribute2", stringValue("rootValue2")));
        assertThat(itemMap, hasEntry("mapAttribute1", stringValue("mapValue1")));
        assertThat(itemMap, hasEntry("mapAttribute2", stringValue("mapValue2")));
        assertThat(itemMap, hasEntry("mapAttribute3", stringValue("mapValue3")));

        AttributeValue resultedNestedRecord = itemMap.get("nestedRecord");
        Assertions.assertThat(resultedNestedRecord).isNotNull();
    }

    @Test
    public void serializeBean_withMultipleAnnotatedMaps_throwsIllegalArgumentException() {
        FlattenMapInvalidBean bean = new FlattenMapInvalidBean();
        bean.setId("idValue");
        bean.setRootAttribute1("rootValue1");
        bean.setRootAttribute2("rootValue2");

        bean.setAttributesMap(new HashMap<String, String>() {{
            put("mapAttribute1", "mapValue1");
            put("mapAttribute2", "mapValue2");
            put("mapAttribute3", "mapValue3");
        }});

        bean.setSecondaryAttributesMap(new HashMap<String, String>() {{
            put("secondaryMapAttribute1", "secondaryMapValue1");
            put("secondaryMapAttribute2", "secondaryMapValue2");
            put("secondaryMapAttribute3", "secondaryMapValue3");
        }});

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("More than one @DynamoDbFlattenMap annotation found on the same record");

        BeanTableSchema<FlattenMapInvalidBean> beanTableSchema = BeanTableSchema.create(FlattenMapInvalidBean.class);
        beanTableSchema.itemToMap(bean, false);
    }

    @Test
    public void deserializeBean_withFlattenMap_successfullyCreatesItem() {
        BeanTableSchema<FlattenMapValidBean> beanTableSchema = BeanTableSchema.create(FlattenMapValidBean.class);
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", stringValue("123"));
        itemMap.put("rootAttribute1", stringValue("rootAttributeValue1"));
        itemMap.put("rootAttribute2", stringValue("rootAttributeValue2"));
        itemMap.put("mapAttribute1", AttributeValue.builder().s("mapValue1").build());
        itemMap.put("mapAttribute2", AttributeValue.builder().s("mapValue2").build());
        itemMap.put("mapAttribute3", AttributeValue.builder().s("mapValue3").build());

        FlattenMapValidBean result = beanTableSchema.mapToItem(itemMap);

        assertThat(result.getId(), is("123"));
        assertThat(result.getRootAttribute1(), is("rootAttributeValue1"));
        assertThat(result.getRootAttribute2(), is("rootAttributeValue2"));
        assertThat(result.getAttributesMap().size(), is(3));
        assertThat(itemMap, hasEntry("mapAttribute1", stringValue("mapValue1")));
        assertThat(itemMap, hasEntry("mapAttribute2", stringValue("mapValue2")));
        assertThat(itemMap, hasEntry("mapAttribute3", stringValue("mapValue3")));
    }

    private static FlattenMapAndFlattenRecordBean createFlattenMapAndFlattenRecordBean() {
        FlattenMapAndFlattenRecordBean bean = new FlattenMapAndFlattenRecordBean();
        bean.setRootAttribute1("rootValue1");
        bean.setRootAttribute2("rootValue2");

        FlattenRecord flattenRecord = new FlattenRecord();
        NestedRecordWithUpdateBehavior updateNestedRecord = new NestedRecordWithUpdateBehavior();
        updateNestedRecord.setNestedCounter(100L);
        CompositeRecord updateCompositeRecord = new CompositeRecord();
        updateCompositeRecord.setNestedRecord(updateNestedRecord);
        flattenRecord.setCompositeRecord(updateCompositeRecord);
        bean.setFlattenRecord(flattenRecord);

        bean.setAttributesMap(new HashMap<String, String>() {{
            put("mapAttribute1", "mapValue1");
            put("mapAttribute2", "mapValue2");
            put("mapAttribute3", "mapValue3");
        }});
        return bean;
    }
}

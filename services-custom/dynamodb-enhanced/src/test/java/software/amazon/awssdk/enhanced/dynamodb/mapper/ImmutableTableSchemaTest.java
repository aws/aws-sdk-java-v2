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

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.AbstractBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.AbstractImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.DocumentImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.FlattenedBeanImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.FlattenedImmutableImmutable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ImmutableTableSchemaTest {
    @Test
    public void documentImmutable_correctlyMapsBeanAttributes() {
        ImmutableTableSchema<DocumentImmutable> documentImmutableTableSchema =
            ImmutableTableSchema.create(DocumentImmutable.class);
        AbstractBean abstractBean = new AbstractBean();
        abstractBean.setAttribute2("two");
        DocumentImmutable documentImmutable = DocumentImmutable.builder().id("id-value")
                                                               .attribute1("one")
                                                               .abstractBean(abstractBean)
                                                               .build();

        AttributeValue expectedDocument = AttributeValue.builder()
                                                        .m(singletonMap("attribute2", stringValue("two")))
                                                        .build();

        Map<String, AttributeValue> itemMap = documentImmutableTableSchema.itemToMap(documentImmutable, true);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("abstractBean", expectedDocument));
    }

    @Test
    public void documentImmutable_list_correctlyMapsBeanAttributes() {
        ImmutableTableSchema<DocumentImmutable> documentImmutableTableSchema =
            ImmutableTableSchema.create(DocumentImmutable.class);
        AbstractBean abstractBean1 = new AbstractBean();
        abstractBean1.setAttribute2("two");
        AbstractBean abstractBean2 = new AbstractBean();
        abstractBean2.setAttribute2("three");
        DocumentImmutable documentImmutable =
            DocumentImmutable.builder()
                             .id("id-value")
                             .attribute1("one")
                             .abstractBeanList(Arrays.asList(abstractBean1, abstractBean2))
                             .build();

        AttributeValue expectedDocument1 = AttributeValue.builder()
                                                         .m(singletonMap("attribute2", stringValue("two")))
                                                         .build();
        AttributeValue expectedDocument2 = AttributeValue.builder()
                                                         .m(singletonMap("attribute2", stringValue("three")))
                                                         .build();
        AttributeValue expectedList = AttributeValue.builder().l(expectedDocument1, expectedDocument2).build();

        Map<String, AttributeValue> itemMap = documentImmutableTableSchema.itemToMap(documentImmutable, true);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("abstractBeanList", expectedList));
    }

    @Test
    public void documentImmutable_map_correctlyMapsBeanAttributes() {
        ImmutableTableSchema<DocumentImmutable> documentImmutableTableSchema =
            ImmutableTableSchema.create(DocumentImmutable.class);
        AbstractBean abstractBean1 = new AbstractBean();
        abstractBean1.setAttribute2("two");
        AbstractBean abstractBean2 = new AbstractBean();
        abstractBean2.setAttribute2("three");
        Map<String, AbstractBean> abstractBeanMap = new HashMap<>();
        abstractBeanMap.put("key1", abstractBean1);
        abstractBeanMap.put("key2", abstractBean2);
        DocumentImmutable documentImmutable =
            DocumentImmutable.builder()
                             .id("id-value")
                             .attribute1("one")
                             .abstractBeanMap(abstractBeanMap)
                             .build();

        AttributeValue expectedDocument1 = AttributeValue.builder()
                                                         .m(singletonMap("attribute2", stringValue("two")))
                                                         .build();
        AttributeValue expectedDocument2 = AttributeValue.builder()
                                                         .m(singletonMap("attribute2", stringValue("three")))
                                                         .build();
        Map<String, AttributeValue> expectedAttributeValueMap = new HashMap<>();
        expectedAttributeValueMap.put("key1", expectedDocument1);
        expectedAttributeValueMap.put("key2", expectedDocument2);
        AttributeValue expectedMap = AttributeValue.builder().m(expectedAttributeValueMap).build();

        Map<String, AttributeValue> itemMap = documentImmutableTableSchema.itemToMap(documentImmutable, true);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("abstractBeanMap", expectedMap));
    }

    @Test
    public void documentImmutable_correctlyMapsImmutableAttributes() {
        ImmutableTableSchema<DocumentImmutable> documentImmutableTableSchema =
            ImmutableTableSchema.create(DocumentImmutable.class);
        AbstractImmutable abstractImmutable = AbstractImmutable.builder().attribute2("two").build();
        DocumentImmutable documentImmutable = DocumentImmutable.builder().id("id-value")
                                                               .attribute1("one")
                                                               .abstractImmutable(abstractImmutable)
                                                               .build();

        AttributeValue expectedDocument = AttributeValue.builder()
                                                        .m(singletonMap("attribute2", stringValue("two")))
                                                        .build();

        Map<String, AttributeValue> itemMap = documentImmutableTableSchema.itemToMap(documentImmutable, true);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("abstractImmutable", expectedDocument));
    }

    @Test
    public void documentImmutable_list_correctlyMapsImmutableAttributes() {
        ImmutableTableSchema<DocumentImmutable> documentImmutableTableSchema =
            ImmutableTableSchema.create(DocumentImmutable.class);
        AbstractImmutable abstractImmutable1 = AbstractImmutable.builder().attribute2("two").build();
        AbstractImmutable abstractImmutable2 = AbstractImmutable.builder().attribute2("three").build();

        DocumentImmutable documentImmutable =
            DocumentImmutable.builder()
                             .id("id-value")
                             .attribute1("one")
                             .abstractImmutableList(Arrays.asList(abstractImmutable1, abstractImmutable2))
                             .build();

        AttributeValue expectedDocument1 = AttributeValue.builder()
                                                         .m(singletonMap("attribute2", stringValue("two")))
                                                         .build();
        AttributeValue expectedDocument2 = AttributeValue.builder()
                                                         .m(singletonMap("attribute2", stringValue("three")))
                                                         .build();
        AttributeValue expectedList = AttributeValue.builder().l(expectedDocument1, expectedDocument2).build();

        Map<String, AttributeValue> itemMap = documentImmutableTableSchema.itemToMap(documentImmutable, true);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("abstractImmutableList", expectedList));
    }

    @Test
    public void documentImmutable_map_correctlyMapsImmutableAttributes() {
        ImmutableTableSchema<DocumentImmutable> documentImmutableTableSchema =
            ImmutableTableSchema.create(DocumentImmutable.class);
        AbstractImmutable abstractImmutable1 = AbstractImmutable.builder().attribute2("two").build();
        AbstractImmutable abstractImmutable2 = AbstractImmutable.builder().attribute2("three").build();
        Map<String, AbstractImmutable> abstractImmutableMap = new HashMap<>();
        abstractImmutableMap.put("key1", abstractImmutable1);
        abstractImmutableMap.put("key2", abstractImmutable2);
        DocumentImmutable documentImmutable =
            DocumentImmutable.builder()
                             .id("id-value")
                             .attribute1("one")
                             .abstractImmutableMap(abstractImmutableMap)
                             .build();

        AttributeValue expectedDocument1 = AttributeValue.builder()
                                                         .m(singletonMap("attribute2", stringValue("two")))
                                                         .build();
        AttributeValue expectedDocument2 = AttributeValue.builder()
                                                         .m(singletonMap("attribute2", stringValue("three")))
                                                         .build();
        Map<String, AttributeValue> expectedAttributeValueMap = new HashMap<>();
        expectedAttributeValueMap.put("key1", expectedDocument1);
        expectedAttributeValueMap.put("key2", expectedDocument2);
        AttributeValue expectedMap = AttributeValue.builder().m(expectedAttributeValueMap).build();

        Map<String, AttributeValue> itemMap = documentImmutableTableSchema.itemToMap(documentImmutable, true);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("abstractImmutableMap", expectedMap));
    }

    @Test
    public void dynamoDbFlatten_correctlyFlattensBeanAttributes() {
        ImmutableTableSchema<FlattenedBeanImmutable> tableSchema =
            ImmutableTableSchema.create(FlattenedBeanImmutable.class);
        AbstractBean abstractBean = new AbstractBean();
        abstractBean.setAttribute2("two");
        FlattenedBeanImmutable flattenedBeanImmutable =
            new FlattenedBeanImmutable.Builder().setId("id-value")
                                                .setAttribute1("one")
                                                .setAbstractBean(abstractBean)
                                                .build();

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(flattenedBeanImmutable, false);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("attribute2", stringValue("two")));
    }

    @Test
    public void dynamoDbFlatten_correctlyFlattensImmutableAttributes() {
        ImmutableTableSchema<FlattenedImmutableImmutable> tableSchema =
            ImmutableTableSchema.create(FlattenedImmutableImmutable.class);
        AbstractImmutable abstractImmutable = AbstractImmutable.builder().attribute2("two").build();
        FlattenedImmutableImmutable FlattenedImmutableImmutable =
            new FlattenedImmutableImmutable.Builder().setId("id-value")
                                                     .setAttribute1("one")
                                                     .setAbstractImmutable(abstractImmutable)
                                                .build();

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(FlattenedImmutableImmutable, false);
        assertThat(itemMap.size(), is(3));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
        assertThat(itemMap, hasEntry("attribute2", stringValue("two")));
    }
}

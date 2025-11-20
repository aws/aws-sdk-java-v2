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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.nullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.ExecutionContext;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.AbstractBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.AbstractImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CompositeMetadataImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CrossIndexImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.DocumentImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.DuplicateOrderImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.FlattenedBeanImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.FlattenedImmutableImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.MixedOrderingImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.NestedImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.NestedImmutableIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.NonSequentialOrderImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.OrderPreservationImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.ToBuilderImmutable;
import software.amazon.awssdk.enhanced.dynamodb.model.ImmutableCompositeKeyRecord;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ImmutableTableSchemaTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @After
    public void tearDown() {
        ImmutableTableSchema.clearSchemaCache();
    }

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

    @Test
    public void dynamodbPreserveEmptyObject_shouldInitializeAsEmptyClass() {
        ImmutableTableSchema<NestedImmutable> tableSchema =
            ImmutableTableSchema.create(NestedImmutable.class);
        AbstractImmutable abstractImmutable = AbstractImmutable.builder().build();

        NestedImmutable nestedImmutable =
            NestedImmutable.builder().integerAttribute(1)
                           .innerBean(abstractImmutable)
                           .build();

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(nestedImmutable, false);
        assertThat(itemMap.size(), is(3));

        NestedImmutable result = tableSchema.mapToItem(itemMap);
        assertThat(result.innerBean(), is(abstractImmutable));
    }

    @Test
    public void dynamoDbIgnoreNulls_shouldOmitNulls() {
        ImmutableTableSchema<NestedImmutableIgnoreNulls> tableSchema =
            ImmutableTableSchema.create(NestedImmutableIgnoreNulls.class);

        NestedImmutableIgnoreNulls nestedImmutable =
            NestedImmutableIgnoreNulls.builder()
                                      .innerBean1(AbstractImmutable.builder().build())
                                      .innerBean2(AbstractImmutable.builder().build())
                                      .build();

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(nestedImmutable, true);
        assertThat(itemMap.size(), is(2));
        AttributeValue expectedMapForInnerBean1 = AttributeValue.builder().m(new HashMap<>()).build();

        assertThat(itemMap, hasEntry("innerBean1", expectedMapForInnerBean1));
        assertThat(itemMap.get("innerBean2").m(), hasEntry("attribute2", nullAttributeValue()));
    }

    @Test
    public void toBuilderImmutable_ignoresToBuilderMethod() {
        ImmutableTableSchema<ToBuilderImmutable> toBuilderImmutableTableSchema =
            ImmutableTableSchema.create(ToBuilderImmutable.class);

        ToBuilderImmutable toBuilderImmutable = ToBuilderImmutable.builder()
                                                                  .id("id-value")
                                                                  .attribute1("one")
                                                                  .build();

        Map<String, AttributeValue> itemMap = toBuilderImmutableTableSchema.itemToMap(toBuilderImmutable, true);
        assertThat(itemMap.size(), is(2));
        assertThat(itemMap, hasEntry("id", stringValue("id-value")));
        assertThat(itemMap, hasEntry("attribute1", stringValue("one")));
    }

    @Test
    public void fromImmutable_constructsTableMetadata_withGSICompositeKeys() {
        ImmutableTableSchema<CompositeMetadataImmutable> schema = ImmutableTableSchema.create(CompositeMetadataImmutable.class);
        TableMetadata metadata = schema.tableMetadata();

        assertThat(metadata.indexPartitionKey(TableMetadata.primaryIndexName()), is("id"));
        assertThat(metadata.indexSortKey(TableMetadata.primaryIndexName()), is(Optional.of("sort")));

        List<String> gsiPartitionKeys = metadata.indexPartitionKeys("gsi1");
        assertThat(gsiPartitionKeys, contains("gsiPk1", "gsiPk2"));

        List<String> gsiSortKeys = metadata.indexSortKeys("gsi1");
        assertThat(gsiSortKeys, contains("gsiSk1", "gsiSk2"));
    }

    @Test
    public void fromImmutable_constructsTableMetadata_withGSICompositePartitionKeys_AndOrderPreserved() {
        ImmutableTableSchema<OrderPreservationImmutable> schema = ImmutableTableSchema.create(OrderPreservationImmutable.class);
        TableMetadata metadata = schema.tableMetadata();

        Optional<IndexMetadata> gsi1Metadata = metadata.indices().stream()
                                                       .filter(index -> "gsi1".equals(index.name()))
                                                       .findFirst();

        assertThat(gsi1Metadata.isPresent(), is(true));

        List<KeyAttributeMetadata> partitionKeysMetadata = gsi1Metadata.get().partitionKeys();
        assertThat(partitionKeysMetadata.size(), is(4));

        assertThat(partitionKeysMetadata.get(0).name(), is("key3"));
        assertThat(partitionKeysMetadata.get(0).order().getIndex(), is(0));
        assertThat(partitionKeysMetadata.get(0).attributeValueType(), is(AttributeValueType.S));

        assertThat(partitionKeysMetadata.get(1).name(), is("key2"));
        assertThat(partitionKeysMetadata.get(1).order().getIndex(), is(1));
        assertThat(partitionKeysMetadata.get(1).attributeValueType(), is(AttributeValueType.S));

        assertThat(partitionKeysMetadata.get(2).name(), is("key4"));
        assertThat(partitionKeysMetadata.get(2).order().getIndex(), is(2));
        assertThat(partitionKeysMetadata.get(2).attributeValueType(), is(AttributeValueType.S));

        assertThat(partitionKeysMetadata.get(3).name(), is("key1"));
        assertThat(partitionKeysMetadata.get(3).order().getIndex(), is(3));
        assertThat(partitionKeysMetadata.get(3).attributeValueType(), is(AttributeValueType.S));
    }

    @Test
    public void fromImmutable_constructsTableMetadata_withGSICompositeKeys_crossIndexConsistency() {
        ImmutableTableSchema<CrossIndexImmutable> schema = ImmutableTableSchema.create(CrossIndexImmutable.class);

        List<String> gsi1PartitionKeys = schema.tableMetadata().indexPartitionKeys("gsi1");
        assertThat(gsi1PartitionKeys.size(), is(2));
        assertThat(gsi1PartitionKeys, contains("attr1", "attr2"));

        List<String> gsi2PartitionKeys = schema.tableMetadata().indexPartitionKeys("gsi2");
        assertThat(gsi2PartitionKeys.size(), is(1));
        assertThat(gsi2PartitionKeys, contains("attr3"));

        List<String> gsi2SortKeys = schema.tableMetadata().indexSortKeys("gsi2");
        assertThat(gsi2SortKeys.size(), is(1));
        assertThat(gsi2SortKeys, contains("attr1"));
    }

    @Test
    public void compositeKeyImmutable_duplicateOrderValues_throwsException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Duplicate partition key order 0 for index 'gsi1'");
        ImmutableTableSchema.create(DuplicateOrderImmutable.class);
    }

    @Test
    public void compositeKeyImmutable_nonSequentialOrders_throwsException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Non-sequential partition key orders for index 'gsi1'. Expected: 0,1,2,3 but got: [0, 2]");
        ImmutableTableSchema.create(NonSequentialOrderImmutable.class);
    }

    @Test
    public void compositeKeyImmutable_mixedExplicitImplicitOrdering_throwsException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Composite partition keys for index 'gsi1' must all have explicit ordering (0,1,2,3)");
        ImmutableTableSchema.create(MixedOrderingImmutable.class);
    }

    @Test
    public void rootSchema_areCached_but_flattenedAreNot() {
        ImmutableTableSchema<ImmutableCompositeKeyRecord> root1 = ImmutableTableSchema.create(ImmutableCompositeKeyRecord.class
            , ExecutionContext.ROOT);
        ImmutableTableSchema<ImmutableCompositeKeyRecord> root2 = ImmutableTableSchema.create(ImmutableCompositeKeyRecord.class
            , ExecutionContext.ROOT);
        assertThat(root1, is(root2));

        ImmutableTableSchema<ImmutableCompositeKeyRecord> flattened =
            ImmutableTableSchema.create(ImmutableCompositeKeyRecord.class, ExecutionContext.FLATTENED);
        assertThat(root1, not(flattened));
    }
}

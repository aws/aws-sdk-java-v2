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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchemaParams;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableTableSchemaParams;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CommonTypesBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CompositeMetadataBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CrossIndexBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.DuplicateOrderBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.ImplicitOrderBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.InvalidBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.MixedOrderingBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.MultiGSIBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.NonSequentialOrderBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.OrderPreservationBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SingleKeyBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.ImmutableMap;

public class TableSchemaTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void builder_constructsStaticTableSchemaBuilder_fromClass() {
        StaticTableSchema.Builder<FakeItem> builder = TableSchema.builder(FakeItem.class);
        assertThat(builder).isNotNull();
    }

    @Test
    public void builder_constructsStaticTableSchemaBuilder_fromEnhancedType() {
        StaticTableSchema.Builder<FakeItem> builder = TableSchema.builder(EnhancedType.of(FakeItem.class));
        assertThat(builder).isNotNull();
    }

    @Test
    public void builder_constructsStaticImmutableTableSchemaBuilder_fromClass() {
        StaticImmutableTableSchema.Builder<SimpleImmutable, SimpleImmutable.Builder> builder =
            TableSchema.builder(SimpleImmutable.class, SimpleImmutable.Builder.class);
        assertThat(builder).isNotNull();
    }

    @Test
    public void builder_constructsStaticImmutableTableSchemaBuilder_fromEnhancedType() {
        StaticImmutableTableSchema.Builder<SimpleImmutable, SimpleImmutable.Builder> builder =
            TableSchema.builder(EnhancedType.of(SimpleImmutable.class), EnhancedType.of(SimpleImmutable.Builder.class));
        assertThat(builder).isNotNull();
    }

    @Test
    public void fromBean_constructsBeanTableSchema() {
        BeanTableSchema<SimpleBean> beanBeanTableSchema = TableSchema.fromBean(SimpleBean.class);
        assertThat(beanBeanTableSchema).isNotNull();
    }

    @Test
    public void fromBean_withParams_constructsBeanTableSchema() {
        BeanTableSchemaParams<SimpleBean> params = BeanTableSchemaParams.builder(SimpleBean.class)
                                                                        .lookup(MethodHandles.lookup())
                                                                        .build();
        BeanTableSchema<SimpleBean> beanTableSchema = TableSchema.fromBean(params);

        assertThat(beanTableSchema).isNotNull();
        assertThat(beanTableSchema.itemType().rawClass()).isEqualTo(SimpleBean.class);
    }

    @Test
    public void fromImmutable_constructsImmutableTableSchema() {
        ImmutableTableSchema<SimpleImmutable> immutableTableSchema =
            TableSchema.fromImmutableClass(SimpleImmutable.class);

        assertThat(immutableTableSchema).isNotNull();
    }

    @Test
    public void fromImmutable_withParams_constructsImmutableTableSchema() {
        ImmutableTableSchemaParams<SimpleImmutable> params = ImmutableTableSchemaParams.builder(SimpleImmutable.class)
                                                                                 .lookup(MethodHandles.lookup())
                                                                                 .build();
        ImmutableTableSchema<SimpleImmutable> immutableTableSchema = TableSchema.fromImmutableClass(params);

        assertThat(immutableTableSchema).isNotNull();
        assertThat(immutableTableSchema.itemType().rawClass()).isEqualTo(SimpleImmutable.class);
    }

    @Test
    public void fromClass_constructsBeanTableSchema() {
        TableSchema<SimpleBean> tableSchema = TableSchema.fromClass(SimpleBean.class);
        assertThat(tableSchema).isInstanceOf(BeanTableSchema.class);
    }

    @Test
    public void fromClass_constructsImmutableTableSchema() {
        TableSchema<SimpleImmutable> tableSchema = TableSchema.fromClass(SimpleImmutable.class);
        assertThat(tableSchema).isInstanceOf(ImmutableTableSchema.class);
    }

    @Test
    public void fromBean_constructsTableMetadata_withGSICompositeKeys() {
        TableSchema<CompositeMetadataBean> schema = TableSchema.fromBean(CompositeMetadataBean.class);
        TableMetadata metadata = schema.tableMetadata();

        assertThat(metadata.indexPartitionKey(TableMetadata.primaryIndexName())).isEqualTo("id");
        assertThat(metadata.indexSortKey(TableMetadata.primaryIndexName())).isEqualTo(Optional.of("sort"));

        List<String> gsiPartitionKeys = metadata.indexPartitionKeys("gsi1");
        assertThat(gsiPartitionKeys).containsExactly("gsiPk1", "gsiPk2");

        List<String> gsiSortKeys = metadata.indexSortKeys("gsi1");
        assertThat(gsiSortKeys).containsExactly("gsiSk1", "gsiSk2");
    }

    @Test
    public void fromBean_constructsTableMetadata_withGSICompositePartitionKeys_AndOrderPreserved() {
        TableSchema<OrderPreservationBean> schema = TableSchema.fromBean(OrderPreservationBean.class);
        TableMetadata metadata = schema.tableMetadata();

        Optional<IndexMetadata> gsi1Metadata = metadata.indices().stream()
                                                       .filter(index -> "gsi1".equals(index.name()))
                                                       .findFirst();

        assertThat(gsi1Metadata.isPresent()).isTrue();

        List<KeyAttributeMetadata> partitionKeysMetadata = gsi1Metadata.get().partitionKeys();
        assertThat(partitionKeysMetadata.size()).isEqualTo(4);

        assertThat(partitionKeysMetadata.get(0).name()).isEqualTo("key3");
        assertThat(partitionKeysMetadata.get(0).order().getIndex()).isEqualTo(0);
        assertThat(partitionKeysMetadata.get(0).attributeValueType()).isEqualTo(AttributeValueType.S);

        assertThat(partitionKeysMetadata.get(1).name()).isEqualTo("key2");
        assertThat(partitionKeysMetadata.get(1).order().getIndex()).isEqualTo(1);
        assertThat(partitionKeysMetadata.get(1).attributeValueType()).isEqualTo(AttributeValueType.S);

        assertThat(partitionKeysMetadata.get(2).name()).isEqualTo("key4");
        assertThat(partitionKeysMetadata.get(2).order().getIndex()).isEqualTo(2);
        assertThat(partitionKeysMetadata.get(2).attributeValueType()).isEqualTo(AttributeValueType.S);

        assertThat(partitionKeysMetadata.get(3).name()).isEqualTo("key1");
        assertThat(partitionKeysMetadata.get(3).order().getIndex()).isEqualTo(3);
        assertThat(partitionKeysMetadata.get(3).attributeValueType()).isEqualTo(AttributeValueType.S);
    }

    @Test
    public void fromBean_constructsTableMetadata_withGSICompositeKeys_crossIndexConsistency() {
        TableSchema<CrossIndexBean> schema = TableSchema.fromBean(CrossIndexBean.class);

        List<String> gsi1PartitionKeys = schema.tableMetadata().indexPartitionKeys("gsi1");
        assertThat(gsi1PartitionKeys.size()).isEqualTo(2);
        assertThat(gsi1PartitionKeys).containsExactly("attr1", "attr2");

        List<String> gsi2PartitionKeys = schema.tableMetadata().indexPartitionKeys("gsi2");
        assertThat(gsi2PartitionKeys.size()).isEqualTo(1);
        assertThat(gsi2PartitionKeys).containsExactly("attr3");

        List<String> gsi2SortKeys = schema.tableMetadata().indexSortKeys("gsi2");
        assertThat(gsi2SortKeys.size()).isEqualTo(1);
        assertThat(gsi2SortKeys).containsExactly("attr1");
    }

    @Test
    public void fromBean_constructsTableMetadata_withGSISingleKeys_backwardCompatibilityMethods() {
        TableSchema<SingleKeyBean> schema = TableSchema.fromBean(SingleKeyBean.class);
        TableMetadata metadata = schema.tableMetadata();

        assertThat(metadata.indexPartitionKey("gsi1")).isEqualTo("gsiPk");
        assertThat(metadata.indexSortKey("gsi1")).isEqualTo(Optional.of("gsiSk"));

        List<String> partitionKeys = metadata.indexPartitionKeys("gsi1");
        assertThat(partitionKeys.size()).isEqualTo(1);
        assertThat(partitionKeys).containsExactly("gsiPk");

        List<String> sortKeys = metadata.indexSortKeys("gsi1");
        assertThat(sortKeys.size()).isEqualTo(1);
        assertThat(sortKeys).containsExactly("gsiSk");
    }

    @Test
    public void fromBean_constructsTableMetadata_withMultipleGSI_differentCompositeStructures() {
        TableSchema<MultiGSIBean> schema = TableSchema.fromBean(MultiGSIBean.class);

        List<String> gsi1PartitionKeys = schema.tableMetadata().indexPartitionKeys("gsi1");
        assertThat(gsi1PartitionKeys.size()).isEqualTo(2);
        assertThat(gsi1PartitionKeys).containsExactly("gsi1Pk1", "gsi1Pk2");

        List<String> gsi1SortKeys = schema.tableMetadata().indexSortKeys("gsi1");
        assertThat(gsi1SortKeys.size()).isEqualTo(1);
        assertThat(gsi1SortKeys).containsExactly("gsi1Sk");

        List<String> gsi2PartitionKeys = schema.tableMetadata().indexPartitionKeys("gsi2");
        assertThat(gsi2PartitionKeys.size()).isEqualTo(1);
        assertThat(gsi2PartitionKeys).containsExactly("gsi2Pk");

        List<String> gsi2SortKeys = schema.tableMetadata().indexSortKeys("gsi2");
        assertThat(gsi2SortKeys.size()).isEqualTo(2);
        assertThat(gsi2SortKeys).containsExactly("gsi2Sk1", "gsi2Sk2");

        List<String> gsi3PartitionKeys = schema.tableMetadata().indexPartitionKeys("gsi3");
        assertThat(gsi3PartitionKeys.size()).isEqualTo(3);
        assertThat(gsi3PartitionKeys).containsExactly("gsi3Pk1", "gsi3Pk2", "gsi3Pk3");

        List<String> gsi3SortKeys = schema.tableMetadata().indexSortKeys("gsi3");
        assertThat(gsi3SortKeys.size()).isEqualTo(0);
    }

    @Test
    public void mapToItem_whenPreserveEmptyObjectTrue_throwsUnsupportedOperationException() {
        exception.expect(UnsupportedOperationException.class);
        exception.expectMessage("preserveEmptyObject is not supported. You can set preserveEmptyObject to "
                                + "false to continue to call this operation. If you wish to enable "
                                + "preserveEmptyObject, please reach out to the maintainers of the "
                                + "implementation class for assistance.");

        TableSchema<CommonTypesBean> schema = new TableSchema<CommonTypesBean>() {
            @Override
            public CommonTypesBean mapToItem(Map<String, AttributeValue> attributeMap) {
                return null;
            }

            @Override
            public Map<String, AttributeValue> itemToMap(CommonTypesBean item, boolean ignoreNulls) {
                return null;
            }

            @Override
            public Map<String, AttributeValue> itemToMap(CommonTypesBean item, Collection<String> attributes) {
                return null;
            }

            @Override
            public AttributeValue attributeValue(CommonTypesBean item, String attributeName) {
                return null;
            }

            @Override
            public TableMetadata tableMetadata() {
                return null;
            }

            @Override
            public EnhancedType<CommonTypesBean> itemType() {
                return null;
            }

            @Override
            public List<String> attributeNames() {
                return null;
            }

            @Override
            public boolean isAbstract() {
                return false;
            }
        };

        schema.mapToItem(ImmutableMap.of("abc", AttributeValue.builder().build()), true);
    }

    @Test
    public void fromClass_invalidClassThrowsException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("InvalidBean");
        TableSchema.fromClass(InvalidBean.class);
    }

    @Test
    public void fromBean_schemaGeneration_GSICompositeKeyImplicitOrdering_throwsException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Composite partition keys for index 'gsi1' must all have explicit ordering (0,1,2,3)");
        TableSchema.fromClass(ImplicitOrderBean.class);
    }

    @Test
    public void fromBean_schemaGeneration_GSICompositeKeyDuplicateOrderValues_throwsException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Duplicate partition key order");
        TableSchema.fromBean(DuplicateOrderBean.class);
    }

    @Test
    public void fromBean_schemaGeneration_GSICompositeKeyNonSequentialOrders_throwsException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Non-sequential partition key orders");
        TableSchema.fromBean(NonSequentialOrderBean.class);
    }

    @Test
    public void fromBean_schemaGeneration_GSICompositeKeyMixedExplicitImplicit_throwsException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("must all have explicit ordering");
        TableSchema.fromBean(MixedOrderingBean.class);
    }
}

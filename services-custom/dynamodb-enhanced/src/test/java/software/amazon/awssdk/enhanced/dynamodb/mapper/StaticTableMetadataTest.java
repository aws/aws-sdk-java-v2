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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.TableMetadata.primaryIndexName;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public class StaticTableMetadataTest {
    private static final String INDEX_NAME = "test_index";
    private static final String ATTRIBUTE_NAME = "test_attribute";
    private static final String ATTRIBUTE_NAME_2 = "test_attribute_2";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void setAndRetrievePrimaryPartitionKey() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(), ATTRIBUTE_NAME, AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.primaryPartitionKey(), is(ATTRIBUTE_NAME));
    }

    @Test
    public void setAndRetrievePrimarySortKey() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
            .addIndexSortKey(primaryIndexName(), ATTRIBUTE_NAME, AttributeValueType.S)
            .build();

        assertThat(tableMetadata.primarySortKey(), is(Optional.of(ATTRIBUTE_NAME)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void retrieveUnsetPrimaryPartitionKey() {
        TableMetadata tableMetadata = StaticTableMetadata.builder().build();

        tableMetadata.primaryPartitionKey();
    }

    @Test(expected = IllegalArgumentException.class)
    public void retrieveUnsetPrimaryPartitionKey_withSortKeySet() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexSortKey(primaryIndexName(),
                                                                          ATTRIBUTE_NAME,
                                                                          AttributeValueType.S)
                                                         .build();

        tableMetadata.primaryPartitionKey();
    }

    @Test
    public void retrieveUnsetPrimarySortKey() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(),
                                                                               ATTRIBUTE_NAME,
                                                                               AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.primarySortKey(), is(Optional.empty()));
    }

    @Test
    public void setAndRetrieveSecondaryPartitionKey() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(INDEX_NAME,
                                                                               ATTRIBUTE_NAME,
                                                                               AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.indexPartitionKey(INDEX_NAME), is(ATTRIBUTE_NAME));
    }

    @Test
    public void setAndRetrieveSecondarySortKey() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexSortKey(INDEX_NAME,
                                                                          ATTRIBUTE_NAME,
                                                                          AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.indexSortKey(INDEX_NAME), is(Optional.of(ATTRIBUTE_NAME)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void retrieveUnsetSecondaryPartitionKey() {
        TableMetadata tableMetadata = StaticTableMetadata.builder().build();

        tableMetadata.indexPartitionKey(INDEX_NAME);
    }

    @Test
    public void retrieveSecondaryPartitionKeyForLocalIndex() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(),
                                                                               ATTRIBUTE_NAME,
                                                                               AttributeValueType.S)
                                                         .addIndexSortKey(INDEX_NAME,
                                                                          ATTRIBUTE_NAME_2,
                                                                          AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.indexPartitionKey(INDEX_NAME), is(ATTRIBUTE_NAME));
    }

    @Test
    public void retrieveUnsetSecondarySortKey() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(INDEX_NAME,
                                                                               ATTRIBUTE_NAME,
                                                                               AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.indexSortKey(INDEX_NAME), is(Optional.empty()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSamePartitionKeyTwice() {
        StaticTableMetadata.builder()
                           .addIndexPartitionKey("idx", "id", AttributeValueType.S)
                           .addIndexPartitionKey("idx", "id", AttributeValueType.S)
                           .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSameSortKeyTwice() {
        StaticTableMetadata.builder()
                           .addIndexSortKey("idx", "id", AttributeValueType.S)
                           .addIndexSortKey("idx", "id", AttributeValueType.S)
                           .build();
    }

    @Test
    public void getPrimaryKeys_partitionAndSort() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
            .addIndexPartitionKey(primaryIndexName(), "primary_id", AttributeValueType.S)
            .addIndexSortKey(primaryIndexName(), "primary_sort", AttributeValueType.S)
            .addIndexPartitionKey(INDEX_NAME, "dummy", AttributeValueType.S)
            .addIndexSortKey(INDEX_NAME, "dummy2", AttributeValueType.S)
            .build();

        assertThat(tableMetadata.primaryKeys(), containsInAnyOrder("primary_id", "primary_sort"));
    }

    @Test
    public void getPrimaryKeys_partition() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(), "primary_id", AttributeValueType.S)
                                                         .addIndexPartitionKey(INDEX_NAME, "dummy", AttributeValueType.S)
                                                         .addIndexSortKey(INDEX_NAME, "dummy2", AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.primaryKeys(), contains("primary_id"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPrimaryKeys_unset() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(INDEX_NAME, "dummy", AttributeValueType.S)
                                                         .addIndexSortKey(INDEX_NAME, "dummy2", AttributeValueType.S)
                                                         .build();

        tableMetadata.primaryKeys();
    }

    @Test
    public void singleKeyImplicitOrdering() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "key1", AttributeValueType.S, Order.UNSPECIFIED);
        builder.addIndexSortKey("gsi1", "sort1", AttributeValueType.S, Order.UNSPECIFIED);

        StaticTableMetadata metadata = builder.build();

        assertThat(metadata.indexPartitionKeys("gsi1"), contains("key1"));
        assertThat(metadata.indexSortKeys("gsi1"), contains("sort1"));
    }

    @Test
    public void singleKeyExplicitOrdering() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "key1", AttributeValueType.S, Order.FIRST);
        builder.addIndexSortKey("gsi1", "sort1", AttributeValueType.S, Order.FIRST);

        StaticTableMetadata metadata = builder.build();

        assertThat(metadata.indexPartitionKeys("gsi1"), contains("key1"));
        assertThat(metadata.indexSortKeys("gsi1"), contains("sort1"));
    }

    @Test
    public void compositeKeysAllExplicit() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "key1", AttributeValueType.S, Order.FIRST);
        builder.addIndexPartitionKey("gsi1", "key2", AttributeValueType.S, Order.SECOND);
        builder.addIndexSortKey("gsi1", "sort1", AttributeValueType.S, Order.FIRST);
        builder.addIndexSortKey("gsi1", "sort2", AttributeValueType.S, Order.SECOND);

        StaticTableMetadata metadata = builder.build();

        assertThat(metadata.indexPartitionKeys("gsi1"), contains("key1", "key2"));
        assertThat(metadata.indexSortKeys("gsi1"), contains("sort1", "sort2"));
    }

    @Test
    public void separatePartitionAndSort() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "pk1", AttributeValueType.S, Order.FIRST);
        builder.addIndexPartitionKey("gsi1", "pk2", AttributeValueType.S, Order.SECOND);

        builder.addIndexSortKey("gsi1", "sk1", AttributeValueType.S, Order.FIRST);
        builder.addIndexSortKey("gsi1", "sk2", AttributeValueType.S, Order.SECOND);
        builder.addIndexSortKey("gsi1", "sk3", AttributeValueType.S, Order.THIRD);

        StaticTableMetadata metadata = builder.build();

        assertThat(metadata.indexPartitionKeys("gsi1"), contains("pk1", "pk2"));
        assertThat(metadata.indexSortKeys("gsi1"), contains("sk1", "sk2", "sk3"));
    }

    @Test
    public void multipleIndicesIndependent() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "key1", AttributeValueType.S, Order.FIRST);
        builder.addIndexPartitionKey("gsi1", "key2", AttributeValueType.S, Order.SECOND);

        builder.addIndexPartitionKey("gsi2", "single_key", AttributeValueType.S, Order.UNSPECIFIED);

        builder.addIndexPartitionKey("gsi3", "keyA", AttributeValueType.S, Order.FIRST);
        builder.addIndexPartitionKey("gsi3", "keyB", AttributeValueType.S, Order.SECOND);
        builder.addIndexPartitionKey("gsi3", "keyC", AttributeValueType.S, Order.THIRD);

        StaticTableMetadata metadata = builder.build();

        assertThat(metadata.indexPartitionKeys("gsi1"), contains("key1", "key2"));
        assertThat(metadata.indexPartitionKeys("gsi2"), contains("single_key"));
        assertThat(metadata.indexPartitionKeys("gsi3"), contains("keyA", "keyB", "keyC"));
    }

    @Test
    public void primaryIndexSkipped() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey(primaryIndexName(), "id", AttributeValueType.S, Order.UNSPECIFIED);
        builder.addIndexSortKey(primaryIndexName(), "sort", AttributeValueType.S, Order.UNSPECIFIED);

        builder.addIndexPartitionKey("gsi1", "gsi_key", AttributeValueType.S, Order.UNSPECIFIED);

        StaticTableMetadata metadata = builder.build();

        assertThat(metadata.indexPartitionKeys(primaryIndexName()), contains("id"));
        assertThat(metadata.indexSortKeys(primaryIndexName()), contains("sort"));
        assertThat(metadata.indexPartitionKeys("gsi1"), contains("gsi_key"));
    }

    @Test
    public void emptyIndex_throwsException() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "key1", AttributeValueType.S, Order.UNSPECIFIED);

        StaticTableMetadata metadata = builder.build();

        assertThatThrownBy(() -> metadata.indexPartitionKeys("empty_index"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to execute an operation that requires a secondary index without defining the index "
                                  + "attributes in the table metadata.");
    }

    @Test
    public void maxFourKeys_partitionKeys() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "key1", AttributeValueType.S, Order.FIRST);
        builder.addIndexPartitionKey("gsi1", "key2", AttributeValueType.S, Order.SECOND);
        builder.addIndexPartitionKey("gsi1", "key3", AttributeValueType.S, Order.THIRD);
        builder.addIndexPartitionKey("gsi1", "key4", AttributeValueType.S, Order.FOURTH);

        StaticTableMetadata metadata = builder.build();

        assertThat(metadata.indexPartitionKeys("gsi1"), hasSize(4));
    }

    @Test
    public void maxFourKeys_sortKeys() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "pk", AttributeValueType.S, Order.UNSPECIFIED);
        builder.addIndexSortKey("gsi1", "sort1", AttributeValueType.S, Order.FIRST);
        builder.addIndexSortKey("gsi1", "sort2", AttributeValueType.S, Order.SECOND);
        builder.addIndexSortKey("gsi1", "sort3", AttributeValueType.S, Order.THIRD);
        builder.addIndexSortKey("gsi1", "sort4", AttributeValueType.S, Order.FOURTH);

        StaticTableMetadata metadata = builder.build();

        assertThat(metadata.indexSortKeys("gsi1"), hasSize(4));
    }

    @Test
    public void orderingPreservation() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "key3", AttributeValueType.S, Order.THIRD);
        builder.addIndexPartitionKey("gsi1", "key1", AttributeValueType.S, Order.FIRST);
        builder.addIndexPartitionKey("gsi1", "key2", AttributeValueType.S, Order.SECOND);

        StaticTableMetadata metadata = builder.build();

        List<String> partitionKeys = metadata.indexPartitionKeys("gsi1");

        assertThat(partitionKeys, hasSize(3));
        assertThat(partitionKeys, contains("key1", "key2", "key3"));
    }

    @Test
    public void builderReuse_independentValidation() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder();

        builder.addIndexPartitionKey("gsi1", "key1", AttributeValueType.S, Order.FIRST);
        StaticTableMetadata metadata1 = builder.build();
        assertThat(metadata1.indexPartitionKeys("gsi1"), contains("key1"));

        builder.addIndexPartitionKey("gsi1", "key2", AttributeValueType.S, Order.SECOND);
        StaticTableMetadata metadata2 = builder.build();
        assertThat(metadata2.indexPartitionKeys("gsi1"), contains("key1", "key2"));
    }

    @Test
    public void getIndexKeys_partitionAndSort() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(), "primary_id", AttributeValueType.S)
                                                         .addIndexSortKey(primaryIndexName(), "primary_sort", AttributeValueType.S)
                                                         .addIndexPartitionKey(INDEX_NAME, "dummy", AttributeValueType.S)
                                                         .addIndexSortKey(INDEX_NAME, "dummy2", AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.indexKeys(INDEX_NAME), containsInAnyOrder("dummy", "dummy2"));
    }

    @Test
    public void getIndexKeys_partition() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(), "primary_id", AttributeValueType.S)
                                                         .addIndexSortKey(primaryIndexName(), "primary_sort", AttributeValueType.S)
                                                         .addIndexPartitionKey(INDEX_NAME, "dummy", AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.indexKeys(INDEX_NAME), contains("dummy"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIndexKeys_unset() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(), "primary_id", AttributeValueType.S)
                                                         .addIndexSortKey(primaryIndexName(), "primary_sort", AttributeValueType.S)
                                                         .build();

        tableMetadata.indexKeys(INDEX_NAME);
    }

    @Test
    public void getIndexKeys_sortOnly() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(), "primary_id", AttributeValueType.S)
                                                         .addIndexSortKey(primaryIndexName(), "primary_sort", AttributeValueType.S)
                                                         .addIndexSortKey(INDEX_NAME, "dummy", AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.indexKeys(INDEX_NAME), containsInAnyOrder("primary_id", "dummy"));
    }

    @Test
    public void getAllKeys() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(), "primary_id", AttributeValueType.S)
                                                         .addIndexSortKey(primaryIndexName(), "primary_sort", AttributeValueType.S)
                                                         .addIndexPartitionKey(INDEX_NAME, "dummy", AttributeValueType.S)
                                                         .addIndexSortKey(INDEX_NAME, "dummy2", AttributeValueType.S)
                                                         .build();

        assertThat(tableMetadata.allKeys(), containsInAnyOrder("primary_id", "primary_sort", "dummy", "dummy2"));
    }

    @Test
    public void getScalarAttributeValueType() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addIndexPartitionKey(primaryIndexName(), "primary_id",
                                                                               AttributeValueType.S)
                                                         .addIndexSortKey(primaryIndexName(), "primary_sort",
                                                                          AttributeValueType.N)
                                                         .addIndexPartitionKey(INDEX_NAME, "dummy",
                                                                               AttributeValueType.B)
                                                         .addIndexSortKey(INDEX_NAME, "dummy2", AttributeValueType.BOOL)
                                                         .build();

        assertThat(tableMetadata.scalarAttributeType("primary_id"), is(Optional.of(ScalarAttributeType.S)));
        assertThat(tableMetadata.scalarAttributeType("primary_sort"), is(Optional.of(ScalarAttributeType.N)));
        assertThat(tableMetadata.scalarAttributeType("dummy"), is(Optional.of(ScalarAttributeType.B)));
        assertThat(tableMetadata.scalarAttributeType("dummy2"), is(Optional.empty()));
    }

    @Test
    public void setAndRetrieveSimpleCustomMetadata() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
            .addCustomMetadataObject("custom-key", 123)
            .build();

        assertThat(tableMetadata.customMetadataObject("custom-key", Integer.class), is(Optional.of(123)));
    }

    @Test
    public void setAndRetrieveCustomMetadataCollection() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addCustomMetadataObject("custom-key", Collections.singleton("123"))
                                                         .addCustomMetadataObject("custom-key", Collections.singleton("456"))
                                                         .build();

        Collection<String> metadataObject = tableMetadata.customMetadataObject("custom-key", Collection.class).orElse(null);
        assertThat(metadataObject.size(), is(2));
        assertThat(metadataObject, contains("123", "456"));
    }

    @Test
    public void setAndRetrieveCustomMetadataMap() {
        TableMetadata tableMetadata =
            StaticTableMetadata.builder()
                               .addCustomMetadataObject("custom-key", Collections.singletonMap("key1", "123"))
                               .addCustomMetadataObject("custom-key", Collections.singletonMap("key2", "456"))
                               .build();

        Map<String, String> metadataObject = tableMetadata.customMetadataObject("custom-key", Map.class).orElse(null);
        assertThat(metadataObject.size(), is(2));
        assertThat(metadataObject.get("key1"), is("123"));
        assertThat(metadataObject.get("key2"), is("456"));
    }

    @Test
    public void retrieveUnsetCustomMetadata() {
        TableMetadata tableMetadata = StaticTableMetadata.builder().build();

        assertThat(tableMetadata.customMetadataObject("custom-key", Integer.class), is(Optional.empty()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAndRetrieveCustomMetadataOfUnassignableType() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addCustomMetadataObject("custom-key", 123.45)
                                                         .build();

        tableMetadata.customMetadataObject("custom-key", Integer.class);
    }

    @Test
    public void setAndRetrieveCustomMetadataOfDifferentButAssignableType() {
        TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                         .addCustomMetadataObject("custom-key", 123.45f)
                                                         .build();

        assertThat(tableMetadata.customMetadataObject("custom-key", Number.class), is(Optional.of(123.45f)));
    }

    @Test
    public void mergeFullIntoEmpty() {
        StaticTableMetadata tableMetadata = StaticTableMetadata.builder()
            .addIndexPartitionKey(primaryIndexName(), "primary_id", AttributeValueType.S)
            .addIndexSortKey(primaryIndexName(), "primary_sort", AttributeValueType.S)
            .addIndexPartitionKey(INDEX_NAME, "dummy", AttributeValueType.S)
            .addIndexSortKey(INDEX_NAME, "dummy2", AttributeValueType.S)
            .addCustomMetadataObject("custom1", "value1")
            .addCustomMetadataObject("custom2", "value2")
            .addCustomMetadataObject("custom-key", Collections.singletonMap("key1", "123"))
            .addCustomMetadataObject("custom-key", Collections.singletonMap("key2", "456"))
            .build();

        StaticTableMetadata mergedTableMetadata = StaticTableMetadata.builder().mergeWith(tableMetadata).build();

        assertThat(mergedTableMetadata, is(tableMetadata));
    }

    @Test
    public void mergeEmptyIntoFull() {
        StaticTableMetadata emptyTableMetadata = StaticTableMetadata.builder().build();

        StaticTableMetadata.Builder tableMetadataBuilder = StaticTableMetadata.builder()
                                                               .addIndexPartitionKey(primaryIndexName(), "primary_id", AttributeValueType.S)
                                                               .addIndexSortKey(primaryIndexName(), "primary_sort", AttributeValueType.S)
                                                               .addIndexPartitionKey(INDEX_NAME, "dummy", AttributeValueType.S)
                                                               .addIndexSortKey(INDEX_NAME, "dummy2", AttributeValueType.S)
                                                               .addCustomMetadataObject("custom1", "value1")
                                                               .addCustomMetadataObject("custom2", "value2")
                                                               .addCustomMetadataObject("custom-key", Collections.singletonMap("key1", "123"))
                                                               .addCustomMetadataObject("custom-key", Collections.singletonMap("key2", "456"));

        StaticTableMetadata original = tableMetadataBuilder.build();
        StaticTableMetadata merged = tableMetadataBuilder.mergeWith(emptyTableMetadata).build();

        assertThat(merged, is(original));
    }

    @Test
    public void mergeWithDuplicateIndexPartitionKey() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder().addIndexPartitionKey(INDEX_NAME, "id", AttributeValueType.S);

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("key");
        exception.expectMessage(INDEX_NAME);

        builder.mergeWith(builder.build()).build();
    }

    @Test
    public void mergeWithDuplicateIndexSortKey() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder().addIndexSortKey(INDEX_NAME, "id", AttributeValueType.S);

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("key");
        exception.expectMessage(INDEX_NAME);

        builder.mergeWith(builder.build()).build();
    }

    @Test
    public void mergeWithDuplicateSingleCustomMetadata() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder().addCustomMetadataObject(INDEX_NAME, "id");

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("custom metadata");
        exception.expectMessage(INDEX_NAME);

        builder.mergeWith(builder.build());
    }

    @Test
    public void mergeWithCustomMetadataCollection() {
        StaticTableMetadata original = StaticTableMetadata.builder()
                                                          .addCustomMetadataObject("custom-key", Collections.singleton("123"))
                                                          .build();
        StaticTableMetadata.Builder mergedBuilder =
            StaticTableMetadata.builder().addCustomMetadataObject("custom-key", Collections.singleton("456"));

        StaticTableMetadata merged = mergedBuilder.mergeWith(original).build();

        Collection<String> metadataObject = merged.customMetadataObject("custom-key", Collection.class).orElse(null);
        assertThat(metadataObject.size(), is(2));
        assertThat(metadataObject, contains("123", "456"));
    }

    @Test
    public void mergeWithCustomMetadataMap() {
        StaticTableMetadata original = StaticTableMetadata.builder()
                                                          .addCustomMetadataObject("custom-key", Collections.singletonMap("key1", "123"))
                                                          .build();
        StaticTableMetadata.Builder mergedBuilder =
            StaticTableMetadata.builder().addCustomMetadataObject("custom-key", Collections.singletonMap("key2", "456"));

        StaticTableMetadata merged = mergedBuilder.mergeWith(original).build();

        Map<String, String> metadataObject = merged.customMetadataObject("custom-key", Map.class).orElse(null);
        assertThat(metadataObject.size(), is(2));
        assertThat(metadataObject.get("key1"), is("123"));
        assertThat(metadataObject.get("key2"), is("456"));
    }
}

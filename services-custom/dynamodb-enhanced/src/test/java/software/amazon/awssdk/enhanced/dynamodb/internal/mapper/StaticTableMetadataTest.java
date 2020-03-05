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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.TableMetadata.primaryIndexName;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeValueType;
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
                                                               .addCustomMetadataObject("custom2", "value2");

        StaticTableMetadata original = tableMetadataBuilder.build();
        StaticTableMetadata merged = tableMetadataBuilder.mergeWith(emptyTableMetadata).build();

        assertThat(merged, is(original));
    }

    @Test
    public void mergeWithDuplicateIndexPartitionKey() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder().addIndexPartitionKey(INDEX_NAME, "id", AttributeValueType.S);

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("partition key");
        exception.expectMessage(INDEX_NAME);

        builder.mergeWith(builder.build());
    }

    @Test
    public void mergeWithDuplicateIndexSortKey() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder().addIndexSortKey(INDEX_NAME, "id", AttributeValueType.S);

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("sort key");
        exception.expectMessage(INDEX_NAME);

        builder.mergeWith(builder.build());
    }

    @Test
    public void mergeWithDuplicateCustomMetadata() {
        StaticTableMetadata.Builder builder = StaticTableMetadata.builder().addCustomMetadataObject(INDEX_NAME, "id");

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("custom metadata");
        exception.expectMessage(INDEX_NAME);

        builder.mergeWith(builder.build());
    }
}

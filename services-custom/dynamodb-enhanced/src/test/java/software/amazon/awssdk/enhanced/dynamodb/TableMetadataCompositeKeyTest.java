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

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;

class TableMetadataCompositeKeyTest {

    private static final TableSchema<FakeItem> SIMPLE_SCHEMA = FakeItem.getTableSchema();
    private static final TableSchema<FakeItemWithIndices> INDEXED_SCHEMA = FakeItemWithIndices.getTableSchema();

    @Test
    void indexPartitionKeys_primaryIndex_returnsSingleKey() {
        TableMetadata metadata = SIMPLE_SCHEMA.tableMetadata();

        List<String> partitionKeys = metadata.indexPartitionKeys(TableMetadata.primaryIndexName());

        assertThat(partitionKeys).containsExactly("id");
    }

    @Test
    void indexSortKeys_primaryIndexNoSort_returnsEmptyList() {
        TableMetadata metadata = SIMPLE_SCHEMA.tableMetadata();

        List<String> sortKeys = metadata.indexSortKeys(TableMetadata.primaryIndexName());

        assertThat(sortKeys).isEmpty();
    }

    @Test
    void indexPartitionKeys_gsiIndex_returnsSingleKey() {
        TableMetadata metadata = INDEXED_SCHEMA.tableMetadata();

        List<String> partitionKeys = metadata.indexPartitionKeys("gsi_1");

        assertThat(partitionKeys).containsExactly("gsi_id");
    }

    @Test
    void backwardCompatibility_deprecatedMethods_stillWork() {
        TableMetadata metadata = INDEXED_SCHEMA.tableMetadata();

        String partitionKey = metadata.indexPartitionKey(TableMetadata.primaryIndexName());
        assertThat(partitionKey).isEqualTo("id");

        Optional<String> sortKey = metadata.indexSortKey(TableMetadata.primaryIndexName());
        assertThat(sortKey).isPresent();
        assertThat(sortKey.get()).isEqualTo("sort");
    }

    @Test
    void backwardCompatibility_newMethodsMatchDeprecated() {
        TableMetadata metadata = INDEXED_SCHEMA.tableMetadata();

        String deprecatedPartitionKey = metadata.indexPartitionKey("gsi_1");
        List<String> newPartitionKeys = metadata.indexPartitionKeys("gsi_1");

        assertThat(newPartitionKeys).hasSize(1);
        assertThat(newPartitionKeys.get(0)).isEqualTo(deprecatedPartitionKey);
    }
}
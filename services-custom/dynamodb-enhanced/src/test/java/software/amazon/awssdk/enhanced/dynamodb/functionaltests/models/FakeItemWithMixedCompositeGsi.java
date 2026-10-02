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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class FakeItemWithMixedCompositeGsi {
    private static final TableSchema<FakeItemWithMixedCompositeGsi> TABLE_SCHEMA =
        TableSchema.fromClass(FakeItemWithMixedCompositeGsi.class);

    public static TableSchema<FakeItemWithMixedCompositeGsi> getTableSchema() {
        return TABLE_SCHEMA;
    }

    private String id;
    private String sort;
    private String rootPartitionKey1;
    private String rootPartitionKey2;
    private String rootSortKey1;
    private String rootSortKey2;
    private FlattenedKeys flattenedKeys;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSortKey
    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"mixed_partition_gsi", "full_mixed_gsi", "mixed_sort_gsi"}, order = Order.FIRST)
    public String getRootPartitionKey1() {
        return rootPartitionKey1;
    }

    public void setRootPartitionKey1(String rootPartitionKey1) {
        this.rootPartitionKey1 = rootPartitionKey1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"mixed_partition_gsi", "full_mixed_gsi", "mixed_sort_gsi"}, order = Order.SECOND)
    public String getRootPartitionKey2() {
        return rootPartitionKey2;
    }

    public void setRootPartitionKey2(String rootPartitionKey2) {
        this.rootPartitionKey2 = rootPartitionKey2;
    }

    @DynamoDbSecondarySortKey(indexNames = {"mixed_sort_gsi", "full_mixed_gsi"}, order = Order.FIRST)
    public String getRootSortKey1() {
        return rootSortKey1;
    }

    public void setRootSortKey1(String rootSortKey1) {
        this.rootSortKey1 = rootSortKey1;
    }

    @DynamoDbSecondarySortKey(indexNames = {"mixed_sort_gsi", "full_mixed_gsi"}, order = Order.SECOND)
    public String getRootSortKey2() {
        return rootSortKey2;
    }

    public void setRootSortKey2(String rootSortKey2) {
        this.rootSortKey2 = rootSortKey2;
    }

    @DynamoDbFlatten
    public FlattenedKeys getFlattenedKeys() {
        return flattenedKeys;
    }

    public void setFlattenedKeys(FlattenedKeys flattenedKeys) {
        this.flattenedKeys = flattenedKeys;
    }

    @DynamoDbBean
    public static class FlattenedKeys {
        private String flattenedPartitionKey1;
        private String flattenedPartitionKey2;
        private String flattenedSortKey1;
        private String flattenedSortKey2;

        @DynamoDbSecondaryPartitionKey(indexNames = {"mixed_partition_gsi", "full_mixed_gsi"}, order = Order.THIRD)
        public String getFlattenedPartitionKey1() {
            return flattenedPartitionKey1;
        }

        public void setFlattenedPartitionKey1(String flattenedPartitionKey1) {
            this.flattenedPartitionKey1 = flattenedPartitionKey1;
        }

        @DynamoDbSecondaryPartitionKey(indexNames = {"mixed_partition_gsi", "full_mixed_gsi"}, order = Order.FOURTH)
        public String getFlattenedPartitionKey2() {
            return flattenedPartitionKey2;
        }

        public void setFlattenedPartitionKey2(String flattenedPartitionKey2) {
            this.flattenedPartitionKey2 = flattenedPartitionKey2;
        }

        @DynamoDbSecondarySortKey(indexNames = {"mixed_sort_gsi", "full_mixed_gsi"}, order = Order.THIRD)
        public String getFlattenedSortKey1() {
            return flattenedSortKey1;
        }

        public void setFlattenedSortKey1(String flattenedSortKey1) {
            this.flattenedSortKey1 = flattenedSortKey1;
        }

        @DynamoDbSecondarySortKey(indexNames = {"mixed_sort_gsi", "full_mixed_gsi"}, order = Order.FOURTH)
        public String getFlattenedSortKey2() {
            return flattenedSortKey2;
        }

        public void setFlattenedSortKey2(String flattenedSortKey2) {
            this.flattenedSortKey2 = flattenedSortKey2;
        }
    }
}
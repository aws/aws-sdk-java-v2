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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class FakeItemWithFlattenedGsi {
    private static final TableSchema<FakeItemWithFlattenedGsi> TABLE_SCHEMA =
        TableSchema.fromClass(FakeItemWithFlattenedGsi.class);

    public static TableSchema<FakeItemWithFlattenedGsi> getTableSchema() {
        return TABLE_SCHEMA;
    }

    private String id;
    private String sort;
    private FlattenedGsiKeys gsiKeys;
    private FlattenedGsiSortKeys gsiSortKeys;

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

    @DynamoDbFlatten
    public FlattenedGsiKeys getGsiKeys() {
        return gsiKeys;
    }

    public void setGsiKeys(FlattenedGsiKeys gsiKeys) {
        this.gsiKeys = gsiKeys;
    }

    @DynamoDbFlatten
    public FlattenedGsiSortKeys getGsiSortKeys() {
        return gsiSortKeys;
    }

    public void setGsiSortKeys(FlattenedGsiSortKeys gsiSortKeys) {
        this.gsiSortKeys = gsiSortKeys;
    }

    @DynamoDbBean
    public static class FlattenedGsiKeys {
        private String gsiPartitionKey;
        private String gsiMixedPartitionKey;

        @DynamoDbSecondaryPartitionKey(indexNames = "flatten_partition_gsi")
        public String getGsiPartitionKey() {
            return gsiPartitionKey;
        }

        public void setGsiPartitionKey(String gsiPartitionKey) {
            this.gsiPartitionKey = gsiPartitionKey;
        }

        @DynamoDbSecondaryPartitionKey(indexNames = "flatten_mixed_gsi")
        public String getGsiMixedPartitionKey() {
            return gsiMixedPartitionKey;
        }

        public void setGsiMixedPartitionKey(String gsiMixedPartitionKey) {
            this.gsiMixedPartitionKey = gsiMixedPartitionKey;
        }
    }

    @DynamoDbBean
    public static class FlattenedGsiSortKeys {
        private String gsiSortKey;
        private String gsiMixedSortKey;
        private String gsiBothSortKey;

        @DynamoDbSecondarySortKey(indexNames = "flatten_sort_gsi")
        public String getGsiSortKey() {
            return gsiSortKey;
        }

        public void setGsiSortKey(String gsiSortKey) {
            this.gsiSortKey = gsiSortKey;
        }

        @DynamoDbSecondarySortKey(indexNames = "flatten_mixed_gsi")
        public String getGsiMixedSortKey() {
            return gsiMixedSortKey;
        }

        public void setGsiMixedSortKey(String gsiMixedSortKey) {
            this.gsiMixedSortKey = gsiMixedSortKey;
        }

        @DynamoDbSecondaryPartitionKey(indexNames = "flatten_both_gsi")
        @DynamoDbSecondarySortKey(indexNames = "flatten_both_gsi")
        public String getGsiBothSortKey() {
            return gsiBothSortKey;
        }

        public void setGsiBothSortKey(String gsiBothSortKey) {
            this.gsiBothSortKey = gsiBothSortKey;
        }
    }
}
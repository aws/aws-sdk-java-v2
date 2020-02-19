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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.primarySortKey;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.secondarySortKey;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.stringAttribute;

import java.util.UUID;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.StaticTableSchema;

public class FakeItemWithIndices {
    private static final StaticTableSchema<FakeItemWithIndices> FAKE_ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemWithIndices.class)
                         .newItemSupplier(FakeItemWithIndices::new)
                         .attributes(
                stringAttribute("id", FakeItemWithIndices::getId, FakeItemWithIndices::setId).as(primaryPartitionKey()),
                stringAttribute("sort", FakeItemWithIndices::getSort, FakeItemWithIndices::setSort).as(primarySortKey()),
                stringAttribute("gsi_id", FakeItemWithIndices::getGsiId, FakeItemWithIndices::setGsiId)
                    .as(secondaryPartitionKey("gsi_1"), secondaryPartitionKey("gsi_2")),
                stringAttribute("gsi_sort", FakeItemWithIndices::getGsiSort, FakeItemWithIndices::setGsiSort)
                    .as(secondarySortKey("gsi_1")),
                stringAttribute("lsi_sort", FakeItemWithIndices::getLsiSort, FakeItemWithIndices::setLsiSort)
                    .as(secondarySortKey("lsi_1")))
                         .build();

    private String id;
    private String sort;
    private String gsiId;
    private String gsiSort;
    private String lsiSort;

    public FakeItemWithIndices() {
    }

    public FakeItemWithIndices(String id, String sort, String gsiId, String gsiSort, String lsiSort) {
        this.id = id;
        this.sort = sort;
        this.gsiId = gsiId;
        this.gsiSort = gsiSort;
        this.lsiSort = lsiSort;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StaticTableSchema<FakeItemWithIndices> getTableSchema() {
        return FAKE_ITEM_MAPPER;
    }

    public static FakeItemWithIndices createUniqueFakeItemWithIndices() {
        return FakeItemWithIndices.builder()
                                  .id(UUID.randomUUID().toString())
                                  .sort(UUID.randomUUID().toString())
                                  .gsiId(UUID.randomUUID().toString())
                                  .gsiSort(UUID.randomUUID().toString())
                                  .lsiSort(UUID.randomUUID().toString())
                                  .build();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getGsiId() {
        return gsiId;
    }

    public void setGsiId(String gsiId) {
        this.gsiId = gsiId;
    }

    public String getGsiSort() {
        return gsiSort;
    }

    public void setGsiSort(String gsiSort) {
        this.gsiSort = gsiSort;
    }

    public String getLsiSort() {
        return lsiSort;
    }

    public void setLsiSort(String lsiSort) {
        this.lsiSort = lsiSort;
    }

    public static class Builder {
        private String id;
        private String sort;
        private String gsiId;
        private String gsiSort;
        private String lsiSort;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public Builder gsiId(String gsiId) {
            this.gsiId = gsiId;
            return this;
        }

        public Builder gsiSort(String gsiSort) {
            this.gsiSort = gsiSort;
            return this;
        }

        public Builder lsiSort(String lsiSort) {
            this.lsiSort = lsiSort;
            return this;
        }

        public FakeItemWithIndices build() {
            return new FakeItemWithIndices(id, sort, gsiId, gsiSort, lsiSort);
        }
    }
}

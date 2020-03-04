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

import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.Attributes.attribute;

import java.util.Objects;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

public class FakeItemWithSort {
    private static final StaticTableSchema<FakeItemWithSort> FAKE_ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemWithSort.class)
                         .newItemSupplier(FakeItemWithSort::new)
                         .attributes(
                             attribute("id", TypeToken.of(String.class), FakeItemWithSort::getId, FakeItemWithSort::setId).as(primaryPartitionKey()),
                             attribute("sort", TypeToken.of(String.class), FakeItemWithSort::getSort, FakeItemWithSort::setSort).as(primarySortKey()),
                             attribute("other_attribute_1", TypeToken.of(String.class), FakeItemWithSort::getOtherAttribute1, FakeItemWithSort::setOtherAttribute1),
                             attribute("other_attribute_2", TypeToken.of(String.class), FakeItemWithSort::getOtherAttribute2, FakeItemWithSort::setOtherAttribute2))
                         .build();

    private String id;
    private String sort;
    private String otherAttribute1;
    private String otherAttribute2;

    public FakeItemWithSort() {
    }

    public FakeItemWithSort(String id, String sort, String otherAttribute1, String otherAttribute2) {
        this.id = id;
        this.sort = sort;
        this.otherAttribute1 = otherAttribute1;
        this.otherAttribute2 = otherAttribute2;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StaticTableSchema<FakeItemWithSort> getTableSchema() {
        return FAKE_ITEM_MAPPER;
    }

    public static TableMetadata getTableMetadata() {
        return FAKE_ITEM_MAPPER.tableMetadata();
    }

    public static FakeItemWithSort createUniqueFakeItemWithSort() {
        return FakeItemWithSort.builder()
                               .id(UUID.randomUUID().toString())
                               .sort(UUID.randomUUID().toString())
                               .build();
    }

    public static FakeItemWithSort createUniqueFakeItemWithoutSort() {
        return FakeItemWithSort.builder()
                               .id(UUID.randomUUID().toString())
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

    public String getOtherAttribute1() {
        return otherAttribute1;
    }

    public void setOtherAttribute1(String otherAttribute1) {
        this.otherAttribute1 = otherAttribute1;
    }

    public String getOtherAttribute2() {
        return otherAttribute2;
    }

    public void setOtherAttribute2(String otherAttribute2) {
        this.otherAttribute2 = otherAttribute2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FakeItemWithSort that = (FakeItemWithSort) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(sort, that.sort) &&
               Objects.equals(otherAttribute1, that.otherAttribute1) &&
               Objects.equals(otherAttribute2, that.otherAttribute2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sort, otherAttribute1, otherAttribute2);
    }

    public static class Builder {
        private String id;
        private String sort;
        private String otherAttribute1;
        private String otherAttribute2;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public Builder otherAttribute1(String otherAttribute1) {
            this.otherAttribute1 = otherAttribute1;
            return this;
        }

        public Builder otherAttribute2(String otherAttribute2) {
            this.otherAttribute2 = otherAttribute2;
            return this;
        }

        public FakeItemWithSort build() {
            return new FakeItemWithSort(id, sort, otherAttribute1, otherAttribute2);
        }
    }
}

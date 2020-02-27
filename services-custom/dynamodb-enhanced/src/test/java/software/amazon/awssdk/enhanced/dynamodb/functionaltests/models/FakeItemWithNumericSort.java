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

import java.util.Random;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

public class FakeItemWithNumericSort {
    private static final Random RANDOM = new Random();

    private static final StaticTableSchema<FakeItemWithNumericSort> FAKE_ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemWithNumericSort.class)
                         .newItemSupplier(FakeItemWithNumericSort::new)
                         .attributes(
                            attribute("id", TypeToken.of(String.class), FakeItemWithNumericSort::getId, FakeItemWithNumericSort::setId)
                                .as(primaryPartitionKey()),
                            attribute("sort", TypeToken.of(Integer.class), FakeItemWithNumericSort::getSort, FakeItemWithNumericSort::setSort)
                                .as(primarySortKey()))
                         .build();

    private String id;
    private Integer sort;

    public FakeItemWithNumericSort() {
    }

    public FakeItemWithNumericSort(String id, Integer sort) {
        this.id = id;
        this.sort = sort;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StaticTableSchema<FakeItemWithNumericSort> getTableSchema() {
        return FAKE_ITEM_MAPPER;
    }

    public static FakeItemWithNumericSort createUniqueFakeItemWithSort() {
        return FakeItemWithNumericSort.builder()
                                      .id(UUID.randomUUID().toString())
                                      .sort(RANDOM.nextInt())
                                      .build();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public static class Builder {
        private String id;
        private Integer sort;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sort(Integer sort) {
            this.sort = sort;
            return this;
        }

        public FakeItemWithNumericSort build() {
            return new FakeItemWithNumericSort(id, sort);
        }
    }
}

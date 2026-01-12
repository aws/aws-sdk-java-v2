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

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

public class CompositeKeyFakeItem {
    private String id;
    private String gsiKey1;
    private String gsiKey2;
    private String gsiSort1;
    private String gsiSort2;

    public CompositeKeyFakeItem() {
    }

    private CompositeKeyFakeItem(Builder builder) {
        this.id = builder.id;
        this.gsiKey1 = builder.gsiKey1;
        this.gsiKey2 = builder.gsiKey2;
        this.gsiSort1 = builder.gsiSort1;
        this.gsiSort2 = builder.gsiSort2;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGsiKey1() {
        return gsiKey1;
    }

    public void setGsiKey1(String gsiKey1) {
        this.gsiKey1 = gsiKey1;
    }

    public String getGsiKey2() {
        return gsiKey2;
    }

    public void setGsiKey2(String gsiKey2) {
        this.gsiKey2 = gsiKey2;
    }

    public String getGsiSort1() {
        return gsiSort1;
    }

    public void setGsiSort1(String gsiSort1) {
        this.gsiSort1 = gsiSort1;
    }

    public String getGsiSort2() {
        return gsiSort2;
    }

    public void setGsiSort2(String gsiSort2) {
        this.gsiSort2 = gsiSort2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompositeKeyFakeItem that = (CompositeKeyFakeItem) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(gsiKey1, that.gsiKey1) &&
               Objects.equals(gsiKey2, that.gsiKey2) &&
               Objects.equals(gsiSort1, that.gsiSort1) &&
               Objects.equals(gsiSort2, that.gsiSort2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gsiKey1, gsiKey2, gsiSort1, gsiSort2);
    }

    public static class Builder {
        private String id;
        private String gsiKey1;
        private String gsiKey2;
        private String gsiSort1;
        private String gsiSort2;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder gsiKey1(String gsiKey1) {
            this.gsiKey1 = gsiKey1;
            return this;
        }

        public Builder gsiKey2(String gsiKey2) {
            this.gsiKey2 = gsiKey2;
            return this;
        }

        public Builder gsiSort1(String gsiSort1) {
            this.gsiSort1 = gsiSort1;
            return this;
        }

        public Builder gsiSort2(String gsiSort2) {
            this.gsiSort2 = gsiSort2;
            return this;
        }

        public CompositeKeyFakeItem build() {
            return new CompositeKeyFakeItem(this);
        }
    }

    public static final TableSchema<CompositeKeyFakeItem> SCHEMA =
        StaticTableSchema.builder(CompositeKeyFakeItem.class)
                         .newItemSupplier(CompositeKeyFakeItem::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(CompositeKeyFakeItem::getId)
                                                           .setter(CompositeKeyFakeItem::setId)
                                                           .tags(StaticAttributeTags.primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("gsiKey1")
                                                           .getter(CompositeKeyFakeItem::getGsiKey1)
                                                           .setter(CompositeKeyFakeItem::setGsiKey1)
                                                           .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.FIRST)))
                         .addAttribute(String.class, a -> a.name("gsiKey2")
                                                           .getter(CompositeKeyFakeItem::getGsiKey2)
                                                           .setter(CompositeKeyFakeItem::setGsiKey2)
                                                           .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.SECOND)))
                         .addAttribute(String.class, a -> a.name("gsiSort1")
                                                           .getter(CompositeKeyFakeItem::getGsiSort1)
                                                           .setter(CompositeKeyFakeItem::setGsiSort1)
                                                           .tags(StaticAttributeTags.secondarySortKey("gsi1", Order.FIRST)))
                         .addAttribute(String.class, a -> a.name("gsiSort2")
                                                           .getter(CompositeKeyFakeItem::getGsiSort2)
                                                           .setter(CompositeKeyFakeItem::setGsiSort2)
                                                           .tags(StaticAttributeTags.secondarySortKey("gsi1", Order.SECOND)))
                         .build();
}
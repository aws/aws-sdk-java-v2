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

import static software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension.AttributeTags.version;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.Attributes.attribute;

import java.util.Objects;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

public class FakeItem extends FakeItemAbstractSubclass {
    private static final StaticTableSchema<FakeItem> FAKE_ITEM_MAPPER =
        StaticTableSchema.builder(FakeItem.class)
                         .newItemSupplier(FakeItem::new)
                         .flatten(FakeItemComposedClass.getTableSchema(),
                                  FakeItem::getComposedObject,
                                  FakeItem::setComposedObject)
                         .extend(getSubclassTableSchema())
                         .attributes(attribute("id", TypeToken.of(String.class), FakeItem::getId, FakeItem::setId).as(primaryPartitionKey()),
                                     attribute("version", TypeToken.of(Integer.class), FakeItem::getVersion, FakeItem::setVersion).as(version()))                         .build();

    private String id;
    private Integer version;
    private FakeItemComposedClass composedObject;

    public FakeItem() {
    }

    public FakeItem(String id, Integer version, FakeItemComposedClass composedObject) {
        this.id = id;
        this.version = version;
        this.composedObject = composedObject;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TableSchema<FakeItem> getTableSchema() {
        return FAKE_ITEM_MAPPER;
    }

    public static TableMetadata getTableMetadata() {
        return FAKE_ITEM_MAPPER.tableMetadata();
    }

    public static FakeItem createUniqueFakeItem() {
        return FakeItem.builder()
                       .id(UUID.randomUUID().toString())
                       .build();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public FakeItemComposedClass getComposedObject() {
        return composedObject;
    }

    public void setComposedObject(FakeItemComposedClass composedObject) {
        this.composedObject = composedObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (! super.equals(o)) return false;
        FakeItem fakeItem = (FakeItem) o;
        return Objects.equals(id, fakeItem.id) &&
               Objects.equals(version, fakeItem.version) &&
               Objects.equals(composedObject, fakeItem.composedObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, version, composedObject);
    }

    public static class Builder {
        private String id;
        private Integer version;
        private FakeItemComposedClass composedObject;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder version(Integer version) {
            this.version = version;
            return this;
        }

        public Builder composedObject(FakeItemComposedClass composedObject) {
            this.composedObject = composedObject;
            return this;
        }

        public FakeItem build() {
            return new FakeItem(id, version, composedObject);
        }
    }
}

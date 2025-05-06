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

import static software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension.AttributeTags.versionAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = FakeVersionedStaticImmutableItem.Builder.class)
public class FakeVersionedStaticImmutableItem {
    private final String id;
    private final String attribute;
    private final long version;

    private FakeVersionedStaticImmutableItem(Builder b) {
        this.id = b.id;
        this.attribute = b.attribute;
        this.version = b.version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String attribute() {
        return attribute;
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    public long version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FakeVersionedStaticImmutableItem that = (FakeVersionedStaticImmutableItem) o;
        return version == that.version && Objects.equals(id, that.id) && Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, attribute, version);
    }

    public static TableSchema<FakeVersionedStaticImmutableItem> getTableSchema() {
        return StaticImmutableTableSchema.builder(FakeVersionedStaticImmutableItem.class, FakeVersionedStaticImmutableItem.Builder.class)
                                         .newItemBuilder(FakeVersionedStaticImmutableItem::builder, FakeVersionedStaticImmutableItem.Builder::build)
                                         .addAttribute(String.class, a -> a.name("id")
                                                                           .getter(FakeVersionedStaticImmutableItem::id)
                                                                           .setter(FakeVersionedStaticImmutableItem.Builder::id)
                                                                           .tags(primaryPartitionKey()))
                                         .addAttribute(Long.class, a -> a.name("version")
                                                                           .getter(FakeVersionedStaticImmutableItem::version)
                                                                           .setter(FakeVersionedStaticImmutableItem.Builder::version)
                                                                           .tags(versionAttribute()))
                                         .addAttribute(String.class, a -> a.name("attribute")
                                                                           .getter(FakeVersionedStaticImmutableItem::attribute)
                                                                           .setter(FakeVersionedStaticImmutableItem.Builder::attribute))
                                         .build();
    }

    public static TableMetadata getTableMetadata() {
        return getTableSchema().tableMetadata();
    }

    public static final class Builder {
        private String id;
        private String attribute;
        private long version;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public Builder attribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        public FakeVersionedStaticImmutableItem build() {
            return new FakeVersionedStaticImmutableItem(this);
        }
    }
}

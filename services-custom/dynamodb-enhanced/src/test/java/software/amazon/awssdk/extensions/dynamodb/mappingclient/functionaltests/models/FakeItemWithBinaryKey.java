/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.binaryAttribute;

import java.nio.ByteBuffer;
import java.util.Objects;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.StaticTableSchema;

public class FakeItemWithBinaryKey {
    private static final StaticTableSchema<FakeItemWithBinaryKey> FAKE_ITEM_WITH_BINARY_KEY_SCHEMA =
        StaticTableSchema.builder()
                         .newItemSupplier(FakeItemWithBinaryKey::new)
                         .attributes(
                            binaryAttribute("id", FakeItemWithBinaryKey::getId, FakeItemWithBinaryKey::setId)
                                .as(primaryPartitionKey()))
                         .build();

    private ByteBuffer id;

    public static StaticTableSchema<FakeItemWithBinaryKey> getTableSchema() {
        return FAKE_ITEM_WITH_BINARY_KEY_SCHEMA;
    }

    public ByteBuffer getId() {
        return id;
    }

    public void setId(ByteBuffer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FakeItemWithBinaryKey that = (FakeItemWithBinaryKey) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

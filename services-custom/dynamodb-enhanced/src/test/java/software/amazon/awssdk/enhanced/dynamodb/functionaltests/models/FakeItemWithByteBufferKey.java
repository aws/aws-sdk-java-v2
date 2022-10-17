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

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

import java.nio.ByteBuffer;
import java.util.Objects;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

public class FakeItemWithByteBufferKey {
    private static final StaticTableSchema<FakeItemWithByteBufferKey> FAKE_ITEM_WITH_BINARY_KEY_SCHEMA =
            StaticTableSchema.builder(FakeItemWithByteBufferKey.class)
                    .newItemSupplier(FakeItemWithByteBufferKey::new)
                    .addAttribute(SdkBytes.class, a -> a.name("id")
                            .getter(FakeItemWithByteBufferKey::getIdAsSdkBytes)
                            .setter(FakeItemWithByteBufferKey::setIdAsSdkBytes)
                            .tags(primaryPartitionKey()))
                    .build();

    private ByteBuffer id;

    public static StaticTableSchema<FakeItemWithByteBufferKey> getTableSchema() {
        return FAKE_ITEM_WITH_BINARY_KEY_SCHEMA;
    }

    public ByteBuffer getId() {
        return id;
    }

    public void setId(ByteBuffer id) {
        this.id = id;
    }


    public SdkBytes getIdAsSdkBytes() {
        return SdkBytes.fromByteBuffer(id);
    }

    public void setIdAsSdkBytes(SdkBytes id) {
        this.id = id.asByteBuffer();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FakeItemWithByteBufferKey that = (FakeItemWithByteBufferKey) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

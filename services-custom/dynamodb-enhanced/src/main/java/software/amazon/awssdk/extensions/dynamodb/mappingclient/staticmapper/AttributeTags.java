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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.AttributeValueType;

@SdkPublicApi
public final class AttributeTags {
    private static final AttributeTag PRIMARY_PARTITION_KEY_SINGLETON =
        new KeyAttribute((tableMetadataBuilder, attribute) ->
            tableMetadataBuilder.addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                      attribute.getAttributeName(),
                                                      attribute.getAttributeValueType()));
    private static final AttributeTag PRIMARY_SORT_KEY_SINGLETON =
        new KeyAttribute((tableMetadataBuilder, attribute) ->
            tableMetadataBuilder.addIndexSortKey(TableMetadata.primaryIndexName(),
                                                 attribute.getAttributeName(),
                                                 attribute.getAttributeValueType()));

    private AttributeTags() {
    }

    public static AttributeTag primaryPartitionKey() {
        return PRIMARY_PARTITION_KEY_SINGLETON;
    }

    public static AttributeTag primarySortKey() {
        return PRIMARY_SORT_KEY_SINGLETON;
    }

    public static AttributeTag secondaryPartitionKey(String indexName) {
        return new KeyAttribute((tableMetadataBuilder, attribute) ->
                                    tableMetadataBuilder.addIndexPartitionKey(indexName,
                                                                              attribute.getAttributeName(),
                                                                              attribute.getAttributeValueType()));
    }

    public static AttributeTag secondarySortKey(String indexName) {
        return new KeyAttribute((tableMetadataBuilder, attribute) ->
                                    tableMetadataBuilder.addIndexSortKey(indexName,
                                                                         attribute.getAttributeName(),
                                                                         attribute.getAttributeValueType()));
    }

    private static class KeyAttribute extends AttributeTag {
        private final BiConsumer<StaticTableMetadata.Builder, AttributeAndType> tableMetadataKeySetter;

        private KeyAttribute(BiConsumer<StaticTableMetadata.Builder, AttributeAndType> tableMetadataKeySetter) {
            this.tableMetadataKeySetter = tableMetadataKeySetter;
        }

        @Override
        protected boolean isKeyAttribute() {
            return true;
        }

        @Override
        public Map<String, Object> customMetadataForAttribute(String attributeName,
                                                              AttributeValueType attributeValueType) {
            return Collections.emptyMap();
        }

        @Override
        public void setTableMetadataForAttribute(String attributeName,
                                                 AttributeValueType attributeValueType,
                                                 StaticTableMetadata.Builder tableMetadataBuilder) {

            if (attributeValueType.scalarAttributeType() == null) {
                throw new IllegalArgumentException(String.format("Attribute '%s' of type %s is not a suitable type to "
                    + "be used as a key.", attributeName, attributeValueType.name()));
            }

            tableMetadataKeySetter.accept(tableMetadataBuilder,
                                          new AttributeAndType(attributeName, attributeValueType));
        }
    }

    private static class AttributeAndType {
        private final String attributeName;
        private final AttributeValueType attributeValueType;

        private AttributeAndType(String attributeName, AttributeValueType attributeValueType) {
            this.attributeName = attributeName;
            this.attributeValueType = attributeValueType;
        }

        private String getAttributeName() {
            return attributeName;
        }

        private AttributeValueType getAttributeValueType() {
            return attributeValueType;
        }
    }
}

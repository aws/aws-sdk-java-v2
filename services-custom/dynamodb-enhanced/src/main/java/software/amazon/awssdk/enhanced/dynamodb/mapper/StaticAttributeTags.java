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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.UpdateBehaviorTag;

/**
 * Common implementations of {@link StaticAttributeTag}. These tags can be used to mark your attributes as primary or
 * secondary keys in your {@link StaticTableSchema} definitions.
 */
@SdkPublicApi
public final class StaticAttributeTags {
    private static final StaticAttributeTag PRIMARY_PARTITION_KEY_SINGLETON =
        new KeyAttributeTag((tableMetadataBuilder, attribute) ->
            tableMetadataBuilder.addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                      attribute.getAttributeName(),
                                                      attribute.getAttributeValueType()));
    private static final StaticAttributeTag PRIMARY_SORT_KEY_SINGLETON =
        new KeyAttributeTag((tableMetadataBuilder, attribute) ->
            tableMetadataBuilder.addIndexSortKey(TableMetadata.primaryIndexName(),
                                                 attribute.getAttributeName(),
                                                 attribute.getAttributeValueType()));

    private StaticAttributeTags() {
    }

    /**
     * Marks an attribute as being the primary partition key of the table it participates in. Only one attribute can
     * be marked this way in a given table schema.
     */
    public static StaticAttributeTag primaryPartitionKey() {
        return PRIMARY_PARTITION_KEY_SINGLETON;
    }

    /**
     * Marks an attribute as being the primary sort key of the table it participates in. Only one attribute can be
     * marked this way in a given table schema.
     */
    public static StaticAttributeTag primarySortKey() {
        return PRIMARY_SORT_KEY_SINGLETON;
    }

    /**
     * Marks an attribute as being a partition key for a secondary index.
     * @param indexName The name of the index this key participates in.
     */
    public static StaticAttributeTag secondaryPartitionKey(String indexName) {
        return new KeyAttributeTag((tableMetadataBuilder, attribute) ->
                                    tableMetadataBuilder.addIndexPartitionKey(indexName,
                                                                              attribute.getAttributeName(),
                                                                              attribute.getAttributeValueType()));
    }

    /**
     * Marks an attribute as being a partition key for multiple secondary indices.
     * @param indexNames The names of the indices this key participates in.
     */
    public static StaticAttributeTag secondaryPartitionKey(Collection<String> indexNames) {
        return new KeyAttributeTag(
            (tableMetadataBuilder, attribute) ->
                indexNames.forEach(
                    indexName -> tableMetadataBuilder.addIndexPartitionKey(indexName,
                                                                           attribute.getAttributeName(),
                                                                           attribute.getAttributeValueType())));
    }

    /**
     * Marks an attribute as being a sort key for a secondary index.
     * @param indexName The name of the index this key participates in.
     */
    public static StaticAttributeTag secondarySortKey(String indexName) {
        return new KeyAttributeTag((tableMetadataBuilder, attribute) ->
                                    tableMetadataBuilder.addIndexSortKey(indexName,
                                                                         attribute.getAttributeName(),
                                                                         attribute.getAttributeValueType()));
    }

    /**
     * Marks an attribute as being a sort key for multiple secondary indices.
     * @param indexNames The names of the indices this key participates in.
     */
    public static StaticAttributeTag secondarySortKey(Collection<String> indexNames) {
        return new KeyAttributeTag(
            (tableMetadataBuilder, attribute) ->
                indexNames.forEach(
                    indexName -> tableMetadataBuilder.addIndexSortKey(indexName,
                                                                      attribute.getAttributeName(),
                                                                      attribute.getAttributeValueType())));
    }

    /**
     * Specifies the behavior when this attribute is updated as part of an 'update' operation such as UpdateItem. See
     * documentation of {@link UpdateBehavior} for details on the different behaviors supported and the default
     * behavior.
     * @param updateBehavior The {@link UpdateBehavior} to be applied to this attribute
     */
    public static StaticAttributeTag updateBehavior(UpdateBehavior updateBehavior) {
        return UpdateBehaviorTag.fromUpdateBehavior(updateBehavior);
    }

    private static class KeyAttributeTag implements StaticAttributeTag {
        private final BiConsumer<StaticTableMetadata.Builder, AttributeAndType> tableMetadataKeySetter;

        private KeyAttributeTag(BiConsumer<StaticTableMetadata.Builder, AttributeAndType> tableMetadataKeySetter) {
            this.tableMetadataKeySetter = tableMetadataKeySetter;
        }

        @Override
        public Consumer<StaticTableMetadata.Builder> modifyMetadata(String attributeName,
                                                                    AttributeValueType attributeValueType) {
            return metadata -> {
                if (attributeValueType.scalarAttributeType() == null) {
                    throw new IllegalArgumentException(
                        String.format("Attribute '%s' of type %s is not a suitable type to be used as a key.",
                                      attributeName, attributeValueType.name()));
                }

                tableMetadataKeySetter.accept(metadata, new AttributeAndType(attributeName, attributeValueType));
            };
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

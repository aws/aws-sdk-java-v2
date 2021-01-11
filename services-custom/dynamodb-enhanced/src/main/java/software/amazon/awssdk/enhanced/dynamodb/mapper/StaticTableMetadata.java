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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticIndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticKeyAttributeMetadata;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Implementation of {@link TableMetadata} that can be constructed directly using literal values for metadata objects.
 * This implementation is used by {@link StaticTableSchema} and associated interfaces such as {@link StaticAttributeTag}
 * and {@link StaticTableTag} which permit manipulation of the table metadata.
 */
@SdkPublicApi
public final class StaticTableMetadata implements TableMetadata {
    private final Map<String, Object> customMetadata;
    private final Map<String, IndexMetadata> indexByNameMap;
    private final Map<String, KeyAttributeMetadata> keyAttributes;

    private StaticTableMetadata(Builder builder) {
        this.customMetadata = Collections.unmodifiableMap(builder.customMetadata);
        this.indexByNameMap = Collections.unmodifiableMap(builder.indexByNameMap);
        this.keyAttributes = Collections.unmodifiableMap(builder.keyAttributes);
    }

    /**
     * Create a new builder for this class
     * @return A newly initialized {@link Builder} for building a {@link StaticTableMetadata} object.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> Optional<T> customMetadataObject(String key, Class<? extends T> objectClass) {
        Object genericObject = customMetadata.get(key);

        if (genericObject == null) {
            return Optional.empty();
        }

        if (!objectClass.isAssignableFrom(genericObject.getClass())) {
            throw new IllegalArgumentException("Attempt to retrieve a custom metadata object as a type that is not "
                                               + "assignable for that object. Custom metadata key: " + key + "; "
                                               + "requested object class: " + objectClass.getCanonicalName() + "; "
                                               + "found object class: " + genericObject.getClass().getCanonicalName());
        }

        return Optional.of(objectClass.cast(genericObject));
    }

    @Override
    public String indexPartitionKey(String indexName) {
        IndexMetadata index = getIndex(indexName);

        if (!index.partitionKey().isPresent()) {
            if (!TableMetadata.primaryIndexName().equals(indexName) && index.sortKey().isPresent()) {
                // Local secondary index, use primary partition key
                return primaryPartitionKey();
            }

            throw new IllegalArgumentException("Attempt to execute an operation against an index that requires a "
                                               + "partition key without assigning a partition key to that index. "
                                               + "Index name: " + indexName);
        }

        return index.partitionKey().get().name();
    }

    @Override
    public Optional<String> indexSortKey(String indexName) {
        IndexMetadata index = getIndex(indexName);

        return index.sortKey().map(KeyAttributeMetadata::name);
    }

    @Override
    public Collection<String> indexKeys(String indexName) {
        IndexMetadata index = getIndex(indexName);

        if (index.sortKey().isPresent()) {
            if (!TableMetadata.primaryIndexName().equals(indexName) && !index.partitionKey().isPresent()) {
                // Local secondary index, use primary index for partition key
                return Collections.unmodifiableList(Arrays.asList(primaryPartitionKey(), index.sortKey().get().name()));
            }
            return Collections.unmodifiableList(Arrays.asList(index.partitionKey().get().name(), index.sortKey().get().name()));
        } else {
            return Collections.singletonList(index.partitionKey().get().name());
        }
    }

    @Override
    public Collection<String> allKeys() {
        return this.keyAttributes.keySet();
    }

    @Override
    public Collection<IndexMetadata> indices() {
        return indexByNameMap.values();
    }

    @Override
    public Map<String, Object> customMetadata() {
        return this.customMetadata;
    }

    @Override
    public Collection<KeyAttributeMetadata> keyAttributes() {
        return this.keyAttributes.values();
    }

    private IndexMetadata getIndex(String indexName) {
        IndexMetadata index = indexByNameMap.get(indexName);

        if (index == null) {
            if (TableMetadata.primaryIndexName().equals(indexName)) {
                throw new IllegalArgumentException("Attempt to execute an operation that requires a primary index "
                                                   + "without defining any primary key attributes in the table "
                                                   + "metadata.");
            } else {
                throw new IllegalArgumentException("Attempt to execute an operation that requires a secondary index "
                                                   + "without defining the index attributes in the table metadata. "
                                                   + "Index name: " + indexName);
            }
        }

        return index;
    }

    @Override
    public Optional<ScalarAttributeType> scalarAttributeType(String keyAttribute) {
        KeyAttributeMetadata key = this.keyAttributes.get(keyAttribute);

        if (key == null) {
            throw new IllegalArgumentException("Key attribute '" + keyAttribute + "' not found in table metadata.");
        }

        return Optional.ofNullable(key.attributeValueType().scalarAttributeType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StaticTableMetadata that = (StaticTableMetadata) o;

        if (customMetadata != null ? ! customMetadata.equals(that.customMetadata) : that.customMetadata != null) {
            return false;
        }
        if (indexByNameMap != null ? ! indexByNameMap.equals(that.indexByNameMap) : that.indexByNameMap != null) {
            return false;
        }
        return keyAttributes != null ? keyAttributes.equals(that.keyAttributes) : that.keyAttributes == null;
    }

    @Override
    public int hashCode() {
        int result = customMetadata != null ? customMetadata.hashCode() : 0;
        result = 31 * result + (indexByNameMap != null ? indexByNameMap.hashCode() : 0);
        result = 31 * result + (keyAttributes != null ? keyAttributes.hashCode() : 0);
        return result;
    }

    /**
     * Builder for {@link StaticTableMetadata}
     */
    public static class Builder {
        private final Map<String, Object> customMetadata = new LinkedHashMap<>();
        private final Map<String, IndexMetadata> indexByNameMap = new LinkedHashMap<>();
        private final Map<String, KeyAttributeMetadata> keyAttributes = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Builds an immutable instance of {@link StaticTableMetadata} from the values supplied to the builder.
         */
        public StaticTableMetadata build() {
            return new StaticTableMetadata(this);
        }

        /**
         * Adds a single custom object to the metadata, keyed by a string. Attempting to add a metadata object with a
         * key that matches one that has already been added will cause an exception to be thrown.
         * @param key a string key that will be used to retrieve the custom metadata
         * @param object an object that will be stored in the custom metadata map
         * @throws  IllegalArgumentException if the custom metadata map already contains an entry with the same key
         */
        public Builder addCustomMetadataObject(String key, Object object) {
            if (customMetadata.containsKey(key)) {
                throw new IllegalArgumentException("Attempt to set a custom metadata object that has already been set. "
                                                   + "Custom metadata object key: " + key);
            }

            customMetadata.put(key, object);
            return this;
        }

        /**
         * Adds information about a partition key associated with a specific index.
         * @param indexName the name of the index to associate the partition key with
         * @param attributeName the name of the attribute that represents the partition key
         * @param attributeValueType the {@link AttributeValueType} of the partition key
         * @throws IllegalArgumentException if a partition key has already been defined for this index
         */
        public Builder addIndexPartitionKey(String indexName, String attributeName, AttributeValueType attributeValueType) {
            IndexMetadata index = indexByNameMap.get(indexName);

            if (index != null && index.partitionKey().isPresent()) {
                throw new IllegalArgumentException("Attempt to set an index partition key that conflicts with an "
                                                   + "existing index partition key of the same name and index. Index "
                                                   + "name: " + indexName + "; attribute name: " + attributeName);
            }

            KeyAttributeMetadata partitionKey = StaticKeyAttributeMetadata.create(attributeName, attributeValueType);
            indexByNameMap.put(indexName,
                               StaticIndexMetadata.builderFrom(index).name(indexName).partitionKey(partitionKey).build());
            markAttributeAsKey(attributeName, attributeValueType);
            return this;
        }

        /**
         * Adds information about a sort key associated with a specific index.
         * @param indexName the name of the index to associate the sort key with
         * @param attributeName the name of the attribute that represents the sort key
         * @param attributeValueType the {@link AttributeValueType} of the sort key
         * @throws IllegalArgumentException if a sort key has already been defined for this index
         */
        public Builder addIndexSortKey(String indexName, String attributeName, AttributeValueType attributeValueType) {
            IndexMetadata index = indexByNameMap.get(indexName);

            if (index != null && index.sortKey().isPresent()) {
                throw new IllegalArgumentException("Attempt to set an index sort key that conflicts with an existing"
                                                   + " index sort key of the same name and index. Index name: "
                                                   + indexName + "; attribute name: " + attributeName);
            }

            KeyAttributeMetadata sortKey = StaticKeyAttributeMetadata.create(attributeName, attributeValueType);
            indexByNameMap.put(indexName,
                               StaticIndexMetadata.builderFrom(index).name(indexName).sortKey(sortKey).build());
            markAttributeAsKey(attributeName, attributeValueType);
            return this;
        }

        /**
         * Declares a 'key-like' attribute that is not an actual DynamoDB key. These pseudo-keys can then be recognized
         * by extensions and treated appropriately, often being protected from manipulations as those would alter the
         * meaning of the record. One example usage of this is a 'versioned record attribute': although the version is
         * not part of the primary key of the record, it effectively serves as such.
         * @param attributeName the name of the attribute to mark as a pseudo-key
         * @param attributeValueType the {@link AttributeValueType} of the pseudo-key
         */
        public Builder markAttributeAsKey(String attributeName, AttributeValueType attributeValueType) {
            KeyAttributeMetadata existing = keyAttributes.get(attributeName);

            if (existing != null && !existing.attributeValueType().equals(attributeValueType)) {
                throw new IllegalArgumentException("Attempt to mark an attribute as a key with a different "
                                                   + "AttributeValueType than one that has already been recorded.");
            }

            if (existing == null) {
                keyAttributes.put(attributeName, StaticKeyAttributeMetadata.create(attributeName, attributeValueType));
            }

            return this;
        }

        /**
         * Package-private method to merge the contents of a constructed {@link TableMetadata} into this builder.
         */
        Builder mergeWith(TableMetadata other) {
            other.indices().forEach(
                index -> {
                    index.partitionKey().ifPresent(
                        partitionKey -> addIndexPartitionKey(index.name(),
                                                             partitionKey.name(),
                                                             partitionKey.attributeValueType()));

                    index.sortKey().ifPresent(
                        sortKey -> addIndexSortKey(index.name(), sortKey.name(), sortKey.attributeValueType())
                    );
                });

            other.customMetadata().forEach(this::addCustomMetadataObject);
            other.keyAttributes().forEach(keyAttribute -> markAttributeAsKey(keyAttribute.name(),
                                                                             keyAttribute.attributeValueType()));
            return this;
        }
    }
}

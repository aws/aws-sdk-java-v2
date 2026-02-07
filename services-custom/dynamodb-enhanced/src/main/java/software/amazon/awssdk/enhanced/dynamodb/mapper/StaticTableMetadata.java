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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
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
@ThreadSafe
public final class StaticTableMetadata implements TableMetadata {
    private final Map<String, Object> customMetadata;
    private final Map<String, IndexMetadata> indexByNameMap;
    private final Map<String, KeyAttributeMetadata> keyAttributes;
    private final ConcurrentHashMap<String, List<String>> partitionKeyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> sortKeyCache = new ConcurrentHashMap<>();

    private StaticTableMetadata(Builder builder) {
        this.customMetadata = Collections.unmodifiableMap(builder.customMetadata);
        Map<String, IndexMetadata> indices = new LinkedHashMap<>();
        builder.indexBuilders.forEach((key, value) -> indices.put(key, value.name(key).build()));
        this.indexByNameMap = Collections.unmodifiableMap(indices);
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
    public List<String> indexPartitionKeys(String indexName) {
        return partitionKeyCache.computeIfAbsent(indexName, this::computePartitionKeys);
    }

    private List<String> computePartitionKeys(String indexName) {
        IndexMetadata index = getIndex(indexName);
        
        List<KeyAttributeMetadata> partitionKeys = index.partitionKeys();
        if (partitionKeys.isEmpty()) {
            if (!TableMetadata.primaryIndexName().equals(indexName) && !index.sortKeys().isEmpty()) {
                // Local secondary index, use primary partition keys
                return indexPartitionKeys(TableMetadata.primaryIndexName());
            }

            throw new IllegalArgumentException("Attempt to execute an operation against an index that requires "
                                               + "partition keys without assigning partition keys to that index. "
                                               + "Index name: " + indexName);
        }

        return Collections.unmodifiableList(partitionKeys.stream()
                           .filter(Objects::nonNull)
                           .map(KeyAttributeMetadata::name)
                           .collect(Collectors.toList()));
    }

    @Override
    public List<String> indexSortKeys(String indexName) {
        return sortKeyCache.computeIfAbsent(indexName, this::computeSortKeys);
    }

    private List<String> computeSortKeys(String indexName) {
        IndexMetadata index = getIndex(indexName);

        List<KeyAttributeMetadata> sortKeys = index.sortKeys();
        return Collections.unmodifiableList(sortKeys.stream()
                       .filter(Objects::nonNull)
                       .map(KeyAttributeMetadata::name)
                       .collect(Collectors.toList()));
    }

    @Override
    public Collection<String> indexKeys(String indexName) {
        IndexMetadata index = getIndex(indexName);
        List<String> allKeys = new ArrayList<>();
        
        List<KeyAttributeMetadata> partitionKeys = index.partitionKeys();
        List<KeyAttributeMetadata> sortKeys = index.sortKeys();
        
        if (!sortKeys.isEmpty()) {
            if (!TableMetadata.primaryIndexName().equals(indexName) && partitionKeys.isEmpty()) {
                // Local secondary index, use primary index for partition keys
                allKeys.addAll(indexPartitionKeys(TableMetadata.primaryIndexName()));
            } else {
                allKeys.addAll(partitionKeys.stream()
                                           .filter(Objects::nonNull)
                                           .map(KeyAttributeMetadata::name)
                                           .collect(Collectors.toList()));
            }
            allKeys.addAll(sortKeys.stream()
                                  .filter(Objects::nonNull)
                                  .map(KeyAttributeMetadata::name)
                                  .collect(Collectors.toList()));
        } else {
            allKeys.addAll(partitionKeys.stream()
                                       .filter(Objects::nonNull)
                                       .map(KeyAttributeMetadata::name)
                                       .collect(Collectors.toList()));
        }
        
        return Collections.unmodifiableList(allKeys);
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
            }
            throw new IllegalArgumentException("Attempt to execute an operation that requires a secondary index "
                                               + "without defining the index attributes in the table metadata. "
                                               + "Index name: " + indexName);
        }

        // Check if primary index is empty (no keys defined)
        if (TableMetadata.primaryIndexName().equals(indexName) && 
            index.partitionKeys().isEmpty() && index.sortKeys().isEmpty()) {
            throw new IllegalArgumentException("Attempt to execute an operation that requires a primary index "
                                               + "without defining any primary key attributes in the table "
                                               + "metadata.");
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
    @NotThreadSafe
    public static class Builder {
        private final Map<String, Object> customMetadata = new LinkedHashMap<>();
        private final Map<String, StaticIndexMetadata.Builder> indexBuilders = new LinkedHashMap<>();
        private final Map<String, KeyAttributeMetadata> keyAttributes = new LinkedHashMap<>();

        private Builder() {
            indexBuilders.put(TableMetadata.primaryIndexName(), StaticIndexMetadata.builder());
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
         * Adds collection of custom objects to the custom metadata, keyed by a string.
         * If a collection is already present then it will append the newly added collection to the existing collection.
         *
         * @param key     a string key that will be used to retrieve the custom metadata
         * @param objects Collection of objects that will be stored in the custom metadata map
         */
        public Builder addCustomMetadataObject(String key, Collection<Object> objects) {
            Object collectionInMetadata = customMetadata.get(key);
            Object customObjectToPut = collectionInMetadata != null
                                       ? Stream.concat(((Collection<Object>) collectionInMetadata).stream(),
                                                       objects.stream()).collect(Collectors.toSet())
                                       : objects;
            customMetadata.put(key, customObjectToPut);
            return this;
        }

        /**
         * Adds map of custom objects to the custom metadata, keyed by a string.
         * If a map is already present then it will merge the new map with the existing map.
         *
         * @param key     a string key that will be used to retrieve the custom metadata
         * @param objectMap Map of objects that will be stored in the custom metadata map
         */
        public Builder addCustomMetadataObject(String key, Map<Object, Object> objectMap) {
            Object collectionInMetadata = customMetadata.get(key);
            Object customObjectToPut = collectionInMetadata != null
                                       ? Stream.concat(((Map<Object, Object>) collectionInMetadata).entrySet().stream(),
                                                       objectMap.entrySet().stream())
                                               .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                                       : objectMap;
            customMetadata.put(key, customObjectToPut);
            return this;
        }

        /**
         * Adds information about a partition key associated with a specific index.
         * @param indexName the name of the index to associate the partition key with
         * @param attributeName the name of the attribute that represents the partition key
         * @param attributeValueType the {@link AttributeValueType} of the partition key
         * @param order the order of this key in composite keys (-1 for implicit, 0-3 for explicit)
         */
        public Builder addIndexPartitionKey(String indexName, String attributeName, 
                                           AttributeValueType attributeValueType, Order order) {
            IndexValidator.validateKeyOrder(order);
            StaticIndexMetadata.Builder indexBuilder = getOrCreateIndexBuilder(indexName);
            IndexValidator.validateNoDuplicateKeys(indexBuilder.getPartitionKeys(), indexName, attributeName);

            KeyAttributeMetadata partitionKey = StaticKeyAttributeMetadata.create(attributeName, attributeValueType, order);
            indexBuilder.addPartitionKey(partitionKey);
            markAttributeAsKey(attributeName, attributeValueType);
            return this;
        }
        
        /**
         * Adds information about a partition key associated with a specific index (backward compatibility).
         * @param indexName the name of the index to associate the partition key with
         * @param attributeName the name of the attribute that represents the partition key
         * @param attributeValueType the {@link AttributeValueType} of the partition key
         */
        public Builder addIndexPartitionKey(String indexName, String attributeName, AttributeValueType attributeValueType) {
            return addIndexPartitionKey(indexName, attributeName, attributeValueType, Order.UNSPECIFIED);
        }

        /**
         * Adds information about a sort key associated with a specific index.
         * @param indexName the name of the index to associate the sort key with
         * @param attributeName the name of the attribute that represents the sort key
         * @param attributeValueType the {@link AttributeValueType} of the sort key
         * @param order the order of this key in composite keys (-1 for implicit, 0-3 for explicit)
         */
        public Builder addIndexSortKey(String indexName, String attributeName, 
                                      AttributeValueType attributeValueType, Order order) {
            IndexValidator.validateKeyOrder(order);
            StaticIndexMetadata.Builder indexBuilder = getOrCreateIndexBuilder(indexName);
            IndexValidator.validateNoDuplicateKeys(indexBuilder.getSortKeys(), indexName, attributeName);

            KeyAttributeMetadata sortKey = StaticKeyAttributeMetadata.create(attributeName, attributeValueType, order);
            indexBuilder.addSortKey(sortKey);
            markAttributeAsKey(attributeName, attributeValueType);
            return this;
        }
        
        /**
         * Adds information about a non-composite sort key associated with a specific index.
         * @param indexName the name of the index to associate the sort key with
         * @param attributeName the name of the attribute that represents the sort key
         * @param attributeValueType the {@link AttributeValueType} of the sort key
         */
        public Builder addIndexSortKey(String indexName, String attributeName, AttributeValueType attributeValueType) {
            return addIndexSortKey(indexName, attributeName, attributeValueType, Order.UNSPECIFIED);
        }
        
        private StaticIndexMetadata.Builder getOrCreateIndexBuilder(String indexName) {
            return indexBuilders.computeIfAbsent(indexName, k -> StaticIndexMetadata.builder());
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

            if (existing != null && existing.attributeValueType() != attributeValueType) {
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
                    for (KeyAttributeMetadata partitionKey : index.partitionKeys()) {
                        addIndexPartitionKey(index.name(),
                                             partitionKey.name(),
                                             partitionKey.attributeValueType(),
                                             partitionKey.order());
                    }
                    for (KeyAttributeMetadata sortKey : index.sortKeys()) {
                        addIndexSortKey(index.name(),
                                        sortKey.name(),
                                        sortKey.attributeValueType(),
                                        sortKey.order());
                    }
                });

            other.customMetadata().forEach(this::mergeCustomMetaDataObject);
            other.keyAttributes().forEach(keyAttribute -> markAttributeAsKey(keyAttribute.name(),
                                                                             keyAttribute.attributeValueType()));
            return this;
        }

        private void mergeCustomMetaDataObject(String key, Object object) {
            if (object instanceof Collection) {
                this.addCustomMetadataObject(key, (Collection<Object>) object);
            } else if (object instanceof Map) {
                this.addCustomMetadataObject(key, (Map<Object, Object>) object);
            } else {
                this.addCustomMetadataObject(key, object);
            }
        }
    }
}

/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static software.amazon.awssdk.services.dynamodb.model.KeyType.HASH;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.RANGE;
import static software.amazon.awssdk.services.dynamodb.model.ProjectionType.KEYS_ONLY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;

/**
 * Table model.
 *
 * @param <T> The object type.
 */
public final class DynamoDbMapperTableModel<T> implements DynamoDbTypeConverter<Map<String, AttributeValue>, T> {

    private final Map<String, GlobalSecondaryIndex> globalSecondaryIndexes;
    private final Map<String, LocalSecondaryIndex> localSecondaryIndexes;
    private final Map<String, DynamoDbMapperFieldModel<T, Object>> versions;
    private final Map<String, DynamoDbMapperFieldModel<T, Object>> fields;
    private final Map<KeyType, DynamoDbMapperFieldModel<T, Object>> keys;
    private final Class<T> targetType;

    /**
     * Constructs a new table model for the specified class.
     * @param builder The builder.
     */
    private DynamoDbMapperTableModel(final DynamoDbMapperTableModel.Builder<T> builder) {
        this.globalSecondaryIndexes = builder.globalSecondaryIndexes();
        this.localSecondaryIndexes = builder.localSecondaryIndexes();
        this.versions = builder.versions();
        this.fields = builder.fields();
        this.keys = builder.keys();
        this.targetType = builder.targetType;
    }

    /**
     * Gets the object type.
     * @return The object type.
     */
    public Class<T> targetType() {
        return this.targetType;
    }

    /**
     * Gets all the field models for the given class.
     * @return The field models.
     */
    public Collection<DynamoDbMapperFieldModel<T, Object>> fields() {
        return fields.values();
    }

    /**
     * Gets the field model for a given attribute.
     * @param <V> The field model's value type.
     * @param attributeName The attribute name.
     * @return The field model.
     */
    @SuppressWarnings("unchecked")
    public <V> DynamoDbMapperFieldModel<T, V> field(final String attributeName) {
        final DynamoDbMapperFieldModel<T, V> field = (DynamoDbMapperFieldModel<T, V>) fields.get(attributeName);
        if (field == null) {
            throw new DynamoDbMappingException(
                    targetType.getSimpleName() + "[" + attributeName + "]; no mapping for attribute by name"
            );
        }
        return field;
    }

    /**
     * Gets all the key field models for the given class.
     * @return The field models.
     */
    public Collection<DynamoDbMapperFieldModel<T, Object>> keys() {
        return keys.values();
    }

    /**
     * Gets the hash key field model for the specified type.
     * @param <H> The hash key type.
     * @return The hash key field model.
     * @throws DynamoDbMappingException If the hash key is not present.
     */
    @SuppressWarnings("unchecked")
    public <H> DynamoDbMapperFieldModel<T, H> hashKey() {
        final DynamoDbMapperFieldModel<T, H> field = (DynamoDbMapperFieldModel<T, H>) keys.get(HASH);
        if (field == null) {
            throw new DynamoDbMappingException(
                    targetType.getSimpleName() + "; no mapping for HASH key"
            );
        }
        return field;
    }

    /**
     * Gets the range key field model for the specified type.
     * @param <R> The range key type.
     * @return The range key field model.
     * @throws DynamoDbMappingException If the range key is not present.
     */
    @SuppressWarnings("unchecked")
    public <R> DynamoDbMapperFieldModel<T, R> rangeKey() {
        final DynamoDbMapperFieldModel<T, R> field = (DynamoDbMapperFieldModel<T, R>) keys.get(RANGE);
        if (field == null) {
            throw new DynamoDbMappingException(
                    targetType.getSimpleName() + "; no mapping for RANGE key"
            );
        }
        return field;
    }

    /**
     * Gets the range key field model for the specified type.
     * @param <R> The range key type.
     * @return The range key field model, or null if not present.
     */
    @SuppressWarnings("unchecked")
    public <R> DynamoDbMapperFieldModel<T, R> rangeKeyIfExists() {
        return (DynamoDbMapperFieldModel<T, R>) keys.get(RANGE);
    }

    /**
     * Gets all the version fields for the given class.
     * @return The field models.
     */
    public Collection<DynamoDbMapperFieldModel<T, Object>> versions() {
        return versions.values();
    }

    /**
     * Indicates if this table has any versioned attributes.
     * @return True if any versioned attributes, false otherwise.
     */
    public boolean versioned() {
        return !versions.isEmpty();
    }

    /**
     * Gets the global secondary indexes for the given class.
     * @return The map of index name to GlobalSecondaryIndexes.
     */
    public Collection<GlobalSecondaryIndex> globalSecondaryIndexes() {
        if (globalSecondaryIndexes.isEmpty()) {
            return null;
        }
        final Collection<GlobalSecondaryIndex> copies = new ArrayList<GlobalSecondaryIndex>(globalSecondaryIndexes.size());
        for (final String indexName : globalSecondaryIndexes.keySet()) {
            copies.add(globalSecondaryIndex(indexName));
        }
        return copies;
    }

    /**
     * Gets the global secondary index.
     * @param indexName The index name.
     * @return The global secondary index or null.
     */
    public GlobalSecondaryIndex globalSecondaryIndex(final String indexName) {
        if (!globalSecondaryIndexes.containsKey(indexName)) {
            return null;
        }
        final GlobalSecondaryIndex gsi = globalSecondaryIndexes.get(indexName);
        final GlobalSecondaryIndex.Builder copyBuilder = GlobalSecondaryIndex.builder()
                .indexName(gsi.indexName())
                .projection(Projection.builder()
                        .projectionType(gsi.projection().projectionType())
                        .build());
        for (final KeySchemaElement key : gsi.keySchema()) {
            copyBuilder.keySchema(KeySchemaElement.builder().attributeName(key.attributeName()).keyType(key.keyType()).build());
        }
        return copyBuilder.build();
    }

    /**
     * Gets the local secondary indexes for the given class.
     * @return The map of index name to LocalSecondaryIndexes.
     */
    public Collection<LocalSecondaryIndex> localSecondaryIndexes() {
        if (localSecondaryIndexes.isEmpty()) {
            return null;
        }
        final Collection<LocalSecondaryIndex> copies = new ArrayList<LocalSecondaryIndex>(localSecondaryIndexes.size());
        for (final String indexName : localSecondaryIndexes.keySet()) {
            copies.add(localSecondaryIndex(indexName));
        }
        return copies;
    }

    /**
     * Gets the local secondary index by name.
     * @param indexName The index name.
     * @return The local secondary index, or null.
     */
    public LocalSecondaryIndex localSecondaryIndex(final String indexName) {
        if (!localSecondaryIndexes.containsKey(indexName)) {
            return null;
        }
        final LocalSecondaryIndex lsi = localSecondaryIndexes.get(indexName);
        final LocalSecondaryIndex.Builder copyBuilder = LocalSecondaryIndex.builder()
                .indexName(lsi.indexName())
                .projection(Projection.builder()
                        .projectionType(lsi.projection().projectionType())
                        .build());
        for (final KeySchemaElement key : lsi.keySchema()) {
            copyBuilder.keySchema(KeySchemaElement.builder()
                    .attributeName(key.attributeName())
                    .keyType(key.keyType())
                    .build());
        }
        return copyBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, AttributeValue> convert(final T object) {
        final Map<String, AttributeValue> map = new LinkedHashMap<String, AttributeValue>();
        for (final DynamoDbMapperFieldModel<T, Object> field : fields()) {
            try {
                final AttributeValue value = field.getAndConvert(object);
                if (value != null) {
                    map.put(field.name(), value);
                }
            } catch (final RuntimeException e) {
                throw new DynamoDbMappingException(
                        targetType.getSimpleName() + "[" + field.name() + "]; could not convert attribute", e
                );
            }
        }
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T unconvert(final Map<String, AttributeValue> object) {
        final T result = StandardBeanProperties.DeclaringReflect.<T>newInstance(targetType);
        if (!object.isEmpty()) {
            for (final DynamoDbMapperFieldModel<T, Object> field : fields()) {
                try {
                    final AttributeValue value = object.get(field.name());
                    if (value != null) {
                        field.unconvertAndSet(result, value);
                    }
                } catch (final RuntimeException e) {
                    throw new DynamoDbMappingException(
                            targetType.getSimpleName() + "[" + field.name() + "]; could not unconvert attribute", e
                    );
                }
            }
        }
        return result;
    }

    /**
     * Creates a new object instance with the keys populated.
     * @param <H> The hash key type.
     * @param <R> The range key type.
     * @param hashKey The hash key.
     * @param rangeKey The range key (optional if not present on table).
     * @return The new instance.
     */
    public <H, R> T createKey(final H hashKey, final R rangeKey) {
        final T key = StandardBeanProperties.DeclaringReflect.<T>newInstance(targetType);
        if (hashKey != null) {
            final DynamoDbMapperFieldModel<T, H> hk = hashKey();
            hk.set(key, hashKey);
        }
        if (rangeKey != null) {
            final DynamoDbMapperFieldModel<T, R> rk = rangeKey();
            rk.set(key, rangeKey);
        }
        return key;
    }

    /**
     * Creates a new key map from the specified object.
     * @param <H> The hash key type.
     * @param <R> The range key type.
     * @return The key map.
     */
    public <H, R> Map<String, AttributeValue> convertKey(final T key) {
        final DynamoDbMapperFieldModel<T, H> hk = this.<H>hashKey();
        final DynamoDbMapperFieldModel<T, R> rk = this.<R>rangeKeyIfExists();
        return this.<H, R>convertKey(hk.get(key), (rk == null ? (R) null : rk.get(key)));
    }

    /**
     * Creates a new key map from the specified hash and range key.
     * @param <H> The hash key type.
     * @param <R> The range key type.
     * @param hashKey The hash key.
     * @param rangeKey The range key (optional if not present on table).
     * @return The key map.
     */
    public <H, R> Map<String, AttributeValue> convertKey(final H hashKey, final R rangeKey) {
        final Map<String, AttributeValue> key = new LinkedHashMap<String, AttributeValue>(4);
        final DynamoDbMapperFieldModel<T, H> hk = this.<H>hashKey();
        final AttributeValue hkValue = hashKey == null ? null : hk.convert(hashKey);
        if (hkValue != null) {
            key.put(hk.name(), hkValue);
        } else {
            throw new DynamoDbMappingException(
                    targetType.getSimpleName() + "[" + hk.name() + "]; no HASH key value present"
            );
        }
        final DynamoDbMapperFieldModel<T, R> rk = this.<R>rangeKeyIfExists();
        final AttributeValue rkValue = rangeKey == null ? null : rk.convert(rangeKey);
        if (rkValue != null) {
            key.put(rk.name(), rkValue);
        } else if (rk != null) {
            throw new DynamoDbMappingException(
                    targetType.getSimpleName() + "[" + rk.name() + "]; no RANGE key value present"
            );
        }
        return key;
    }

    /**
     * The table model properties.
     */
    static interface Properties<T> {
        public String tableName();

        static final class Immutable<T> implements Properties<T> {
            private final String tableName;

            public Immutable(final Properties<T> properties) {
                this.tableName = properties.tableName();
            }

            @Override
            public String tableName() {
                return this.tableName;
            }
        }
    }

    /**
     * {@link DynamoDbMapperTableModel} builder.
     */
    static class Builder<T> {
        private final Map<String, DynamoDbMapperFieldModel<T, Object>> versions;
        private final Map<String, DynamoDbMapperFieldModel<T, Object>> fields;
        private final Map<KeyType, DynamoDbMapperFieldModel<T, Object>> keys;
        private final Properties properties;
        private final Class<T> targetType;

        public Builder(Class<T> targetType, Properties<T> properties) {
            this.versions = new LinkedHashMap<String, DynamoDbMapperFieldModel<T, Object>>(4);
            this.fields = new LinkedHashMap<String, DynamoDbMapperFieldModel<T, Object>>();
            this.keys = new EnumMap<KeyType, DynamoDbMapperFieldModel<T, Object>>(KeyType.class);
            this.properties = properties;
            this.targetType = targetType;
        }

        public Builder<T> with(final DynamoDbMapperFieldModel<T, Object> field) {
            fields.put(field.name(), field);
            if (field.keyType() != null) {
                keys.put(field.keyType(), field);
            }
            if (field.versioned()) {
                versions.put(field.name(), field);
            }
            return this;
        }

        public Map<String, GlobalSecondaryIndex> globalSecondaryIndexes() {
            final Map<String, GlobalSecondaryIndex> map = new LinkedHashMap<String, GlobalSecondaryIndex>();
            for (final DynamoDbMapperFieldModel<T, Object> field : fields.values()) {
                for (final String indexName : field.globalSecondaryIndexNames(HASH)) {
                    final GlobalSecondaryIndex.Builder gsiBuilder = GlobalSecondaryIndex.builder()
                            .indexName(indexName)
                            .projection(Projection.builder()
                                    .projectionType(KEYS_ONLY)
                                    .build())
                            .keySchema(KeySchemaElement.builder()
                                    .attributeName(field.name())
                                    .keyType(HASH).build());
                    if (map.put(indexName, gsiBuilder.build()) != null) {
                        throw new DynamoDbMappingException(
                                targetType.getSimpleName() + "[" + field.name() + "]; must not duplicate GSI " + indexName
                        );
                    }
                }
            }
            for (final DynamoDbMapperFieldModel<T, Object> field : fields.values()) {
                for (final String indexName : field.globalSecondaryIndexNames(RANGE)) {
                    final GlobalSecondaryIndex gsi = map.get(indexName);
                    if (gsi == null) {
                        throw new DynamoDbMappingException(
                                targetType.getSimpleName() + "[" + field.name() + "]; no HASH key for GSI " + indexName
                        );
                    }
                    map.put(indexName,
                            gsi.toBuilder()
                                    .keySchema(KeySchemaElement.builder()
                                            .attributeName(field.name())
                                            .keyType(RANGE).build()).build());
                }
            }
            if (map.isEmpty()) {
                return Collections.<String, GlobalSecondaryIndex>emptyMap();
            }
            return Collections.unmodifiableMap(map);
        }

        public Map<String, LocalSecondaryIndex> localSecondaryIndexes() {
            final Map<String, LocalSecondaryIndex> map = new LinkedHashMap<String, LocalSecondaryIndex>();
            for (final DynamoDbMapperFieldModel<T, Object> field : fields.values()) {
                for (final String indexName : field.localSecondaryIndexNames()) {
                    final LocalSecondaryIndex.Builder lsiBuilder = LocalSecondaryIndex.builder()
                            .indexName(indexName)
                            .projection(Projection.builder()
                                    .projectionType(KEYS_ONLY)
                                    .build())
                            .keySchema(KeySchemaElement.builder()
                                            .attributeName(keys.get(HASH).name())
                                            .keyType(HASH).build(),
                                    KeySchemaElement.builder()
                                            .attributeName(field.name())
                                            .keyType(RANGE).build());
                    if (map.put(indexName, lsiBuilder.build()) != null) {
                        throw new DynamoDbMappingException(
                                targetType.getSimpleName() + "[" + field.name() + "]; must not duplicate LSI " + indexName
                        );
                    }
                }
            }
            if (map.isEmpty()) {
                return Collections.<String, LocalSecondaryIndex>emptyMap();
            }
            return Collections.unmodifiableMap(map);
        }

        private Map<String, DynamoDbMapperFieldModel<T, Object>> versions() {
            if (versions.isEmpty()) {
                return Collections.<String, DynamoDbMapperFieldModel<T, Object>>emptyMap();
            }
            return Collections.unmodifiableMap(versions);
        }

        public Map<String, DynamoDbMapperFieldModel<T, Object>> fields() {
            if (fields.isEmpty()) {
                return Collections.<String, DynamoDbMapperFieldModel<T, Object>>emptyMap();
            }
            return Collections.unmodifiableMap(fields);
        }

        public Map<KeyType, DynamoDbMapperFieldModel<T, Object>> keys() {
            if (keys.isEmpty()) {
                return Collections.<KeyType, DynamoDbMapperFieldModel<T, Object>>emptyMap();
            }
            return Collections.unmodifiableMap(keys);
        }

        public DynamoDbMapperTableModel<T> build() {
            final DynamoDbMapperTableModel<T> result = new DynamoDbMapperTableModel<T>(this);
            if (properties.tableName() != null) {
                result.hashKey(); //<- make sure the hash key is present
            }
            return result;
        }
    }

}

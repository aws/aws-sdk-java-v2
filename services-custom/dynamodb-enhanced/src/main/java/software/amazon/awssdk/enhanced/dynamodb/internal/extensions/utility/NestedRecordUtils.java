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

package software.amazon.awssdk.enhanced.dynamodb.internal.extensions.utility;

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.getNestedSchema;
import static software.amazon.awssdk.enhanced.dynamodb.internal.operations.UpdateItemOperation.NESTED_OBJECT_UPDATE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class NestedRecordUtils {

    private static final Pattern NESTED_OBJECT_PATTERN = Pattern.compile(NESTED_OBJECT_UPDATE);

    private NestedRecordUtils() {
    }

    /**
     * Resolves and returns the {@link TableSchema} for the element type of list attribute from the provided root schema.
     * <p>
     * This method is useful when dealing with lists of nested objects in a DynamoDB-enhanced table schema, particularly in
     * scenarios where the list is part of a flattened nested structure.
     * <p>
     * If the provided key contains the nested object delimiter (e.g., {@code _NESTED_ATTR_UPDATE_}), the method traverses the
     * nested hierarchy based on that path to locate the correct schema for the target attribute. Otherwise, it directly resolves
     * the list element type from the root schema using reflection.
     *
     * @param rootSchema The root {@link TableSchema} representing the top-level entity.
     * @param key        The key representing the list attribute, either flat or nested (using a delimiter).
     * @return The {@link TableSchema} representing the list element type of the specified attribute.
     * @throws IllegalArgumentException If the list element class cannot be found via reflection.
     */
    public static TableSchema<?> getTableSchemaForListElement(TableSchema<?> rootSchema, String key) {
        return getTableSchemaForListElement(rootSchema, key, new HashMap<>());
    }

    /**
     * Same as {@link #getTableSchemaForListElement(TableSchema, String)} but allows callers to provide a shared per-operation
     * cache for nested schema lookups.
     */
    public static TableSchema<?> getTableSchemaForListElement(
        TableSchema<?> rootSchema,
        String key,
        Map<SchemaLookupKey, Optional<? extends TableSchema<?>>> nestedSchemaCache) {
        TableSchema<?> listElementSchema;

        if (!key.contains(NESTED_OBJECT_UPDATE)) {
            Optional<? extends TableSchema<?>> staticSchema = getNestedSchemaCached(nestedSchemaCache, rootSchema, key);
            if (staticSchema.isPresent()) {
                listElementSchema = staticSchema.get();
            } else {
                AttributeConverter<?> converter = rootSchema.converterForAttribute(key);
                if (converter == null) {
                    throw new IllegalArgumentException("No converter found for attribute: " + key);
                }
                List<EnhancedType<?>> rawClassParameters = converter.type().rawClassParameters();
                if (CollectionUtils.isNullOrEmpty(rawClassParameters)) {
                    throw new IllegalArgumentException("No type parameters found for list attribute: " + key);
                }
                listElementSchema = TableSchema.fromClass(rawClassParameters.get(0).rawClass());
            }
        } else {
            String[] parts = NESTED_OBJECT_PATTERN.split(key);
            TableSchema<?> currentSchema = rootSchema;

            for (int i = 0; i < parts.length - 1; i++) {
                Optional<? extends TableSchema<?>> nestedSchema =
                    getNestedSchemaCached(nestedSchemaCache, currentSchema, parts[i]);
                if (nestedSchema.isPresent()) {
                    currentSchema = nestedSchema.get();
                }
            }

            String attributeName = parts[parts.length - 1];
            Optional<? extends TableSchema<?>> nestedListSchema =
                getNestedSchemaCached(nestedSchemaCache, currentSchema, attributeName);

            listElementSchema = nestedListSchema.orElseThrow(
                () -> new IllegalArgumentException("Unable to resolve schema for list element at: " + key));
        }

        return listElementSchema;
    }

    /**
     * Traverses the attribute keys representing flattened nested structures and resolves the corresponding {@link TableSchema}
     * for each nested path.
     * <p>
     * The method constructs a mapping between each unique nested path (represented as dot-delimited strings) and the
     * corresponding {@link TableSchema} object derived from the root schema. It supports resolving schemas for arbitrarily deep
     * nesting, using the {@code _NESTED_ATTR_UPDATE_} pattern as a path delimiter.
     * <p>
     * This is typically used in update or transformation flows where fields from nested objects are represented as flattened keys
     * in the attribute map (e.g., {@code parent_NESTED_ATTR_UPDATE_child}).
     *
     * @param attributesToSet A map of flattened attribute keys to values, where keys may represent paths to nested attributes.
     * @param rootSchema      The root {@link TableSchema} of the top-level entity.
     * @return A map where the key is the nested path (e.g., {@code "parent.child"}) and the value is the {@link TableSchema}
     * corresponding to that level in the object hierarchy.
     */
    public static Map<String, TableSchema<?>> resolveSchemasPerPath(Map<String, AttributeValue> attributesToSet,
                                                                    TableSchema<?> rootSchema) {
        return resolveSchemasPerPath(attributesToSet, rootSchema, new HashMap<>());
    }

    /**
     * Same as {@link #resolveSchemasPerPath(Map, TableSchema)} but allows callers to provide a shared per-operation cache for
     * nested schema lookups.
     */
    public static Map<String, TableSchema<?>> resolveSchemasPerPath(
        Map<String, AttributeValue> attributesToSet,
        TableSchema<?> rootSchema,
        Map<SchemaLookupKey, Optional<? extends TableSchema<?>>> nestedSchemaCache) {

        Map<String, TableSchema<?>> schemaMap = new HashMap<>();
        schemaMap.put("", rootSchema);

        for (String key : attributesToSet.keySet()) {
            String[] parts = NESTED_OBJECT_PATTERN.split(key);

            StringBuilder pathBuilder = new StringBuilder();
            TableSchema<?> currentSchema = rootSchema;

            for (int i = 0; i < parts.length - 1; i++) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append(".");
                }
                pathBuilder.append(parts[i]);

                String path = pathBuilder.toString();

                if (!schemaMap.containsKey(path)) {
                    Optional<? extends TableSchema<?>> nestedSchema =
                        getNestedSchemaCached(nestedSchemaCache, currentSchema, parts[i]);

                    if (nestedSchema.isPresent()) {
                        TableSchema<?> resolved = nestedSchema.get();
                        schemaMap.put(path, resolved);
                        currentSchema = resolved;
                    }
                } else {
                    currentSchema = schemaMap.get(path);
                }
            }
        }

        return schemaMap;
    }

    /**
     * Converts a dot-separated path to a composite key using nested object delimiters. Example:
     * {@code reconstructCompositeKey("parent.child", "attr")} returns
     * {@code "parent_NESTED_ATTR_UPDATE_child_NESTED_ATTR_UPDATE_attr"}
     *
     * @param path          the dot-separated path; may be null or empty
     * @param attributeName the attribute name to append; must not be null
     * @return the composite key with nested object delimiters
     */
    public static String reconstructCompositeKey(String path, String attributeName) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Attribute name cannot be null");
        }

        if (StringUtils.isEmpty(path)) {
            return attributeName;
        }

        return String.join(NESTED_OBJECT_UPDATE, path.split("\\."))
               + NESTED_OBJECT_UPDATE + attributeName;
    }

    /**
     * Cached wrapper around {@link software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils#getNestedSchema}. Cache
     * key is based on (parent schema identity, attribute name).
     */
    public static Optional<? extends TableSchema<?>> getNestedSchemaCached(
        Map<SchemaLookupKey, Optional<? extends TableSchema<?>>> cache,
        TableSchema<?> parentSchema,
        String attributeName) {

        SchemaLookupKey key = new SchemaLookupKey(parentSchema, attributeName);
        return cache.computeIfAbsent(key, k -> getNestedSchema(parentSchema, attributeName));
    }

    /**
     * Cached wrapper for resolving list element schema, storing results (including null) in the provided cache.
     * <p>
     * Note: {@link #getTableSchemaForListElement(TableSchema, String, Map)} does not return null today, but this helper is used
     * by callers that previously cached the list element schema separately, and it keeps the "cache null" behavior.
     */
    public static TableSchema<?> getListElementSchemaCached(
        Map<SchemaLookupKey, TableSchema<?>> cache,
        TableSchema<?> parentSchema,
        String attributeName) {

        SchemaLookupKey key = new SchemaLookupKey(parentSchema, attributeName);

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        TableSchema<?> schema = getTableSchemaForListElement(parentSchema, attributeName, new HashMap<>());
        cache.put(key, schema);
        return schema;
    }

    /**
     * Identity-based cache key for schema lookups: - compares TableSchema by identity (==) to avoid depending on its
     * equals/hashCode semantics - compares attribute name by value
     */
    public static final class SchemaLookupKey {
        private final TableSchema<?> parentSchema;
        private final String attributeName;
        private final int hash;

        public SchemaLookupKey(TableSchema<?> parentSchema, String attributeName) {
            this.parentSchema = parentSchema;
            this.attributeName = attributeName;
            this.hash = 31 * System.identityHashCode(parentSchema) + attributeName.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SchemaLookupKey)) {
                return false;
            }
            SchemaLookupKey other = (SchemaLookupKey) o;
            return this.parentSchema == other.parentSchema && this.attributeName.equals(other.attributeName);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}

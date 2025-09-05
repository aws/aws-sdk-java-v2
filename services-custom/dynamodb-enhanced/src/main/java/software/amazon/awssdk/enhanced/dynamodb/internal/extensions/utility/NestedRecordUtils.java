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
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class NestedRecordUtils {

    private static final Pattern NESTED_OBJECT_PATTERN = Pattern.compile(NESTED_OBJECT_UPDATE);

    private NestedRecordUtils() {
    }

    /**
     * Resolves and returns the {@link TableSchema} for the element type of a list attribute from the provided root schema.
     * <p>
     * This method is useful when dealing with lists of nested objects in a DynamoDB-enhanced table schema,
     * particularly in scenarios where the list is part of a flattened nested structure.
     * <p>
     * If the provided key contains the nested object delimiter (e.g., {@code _NESTED_ATTR_UPDATE_}), the method traverses
     * the nested hierarchy based on that path to locate the correct schema for the target attribute.
     * Otherwise, it directly resolves the list element type from the root schema using reflection.
     *
     * @param rootSchema The root {@link TableSchema} representing the top-level entity.
     * @param key        The key representing the list attribute, either flat or nested (using a delimiter).
     * @return The {@link TableSchema} representing the list element type of the specified attribute.
     * @throws IllegalArgumentException If the list element class cannot be found via reflection.
     */
    public static TableSchema<?> getTableSchemaForListElement(TableSchema<?> rootSchema, String key) {
        TableSchema<?> listElementSchema;
        try {
            if (!key.contains(NESTED_OBJECT_UPDATE)) {
                listElementSchema = TableSchema.fromClass(
                    Class.forName(rootSchema.converterForAttribute(key).type().rawClassParameters().get(0).rawClass().getName()));
            } else {
                String[] parts = NESTED_OBJECT_PATTERN.split(key);
                TableSchema<?> currentSchema = rootSchema;

                for (int i = 0; i < parts.length - 1; i++) {
                    Optional<? extends TableSchema<?>> nestedSchema = getNestedSchema(currentSchema, parts[i]);
                    if (nestedSchema.isPresent()) {
                        currentSchema = nestedSchema.get();
                    }
                }
                String attributeName = parts[parts.length - 1];
                listElementSchema = TableSchema.fromClass(
                    Class.forName(currentSchema.converterForAttribute(attributeName)
                                               .type().rawClassParameters().get(0).rawClass().getName()));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found for field name: " + key, e);
        }
        return listElementSchema;
    }

    /**
     * Traverses the attribute keys representing flattened nested structures and resolves the corresponding
     * {@link TableSchema} for each nested path.
     * <p>
     * The method constructs a mapping between each unique nested path (represented as dot-delimited strings)
     * and the corresponding {@link TableSchema} object derived from the root schema. It supports resolving schemas
     * for arbitrarily deep nesting, using the {@code _NESTED_ATTR_UPDATE_} pattern as a path delimiter.
     * <p>
     * This is typically used in update or transformation flows where fields from nested objects are represented
     * as flattened keys in the attribute map (e.g., {@code parent_NESTED_ATTR_UPDATE_child}).
     *
     * @param attributesToSet A map of flattened attribute keys to values, where keys may represent paths to nested attributes.
     * @param rootSchema      The root {@link TableSchema} of the top-level entity.
     * @return A map where the key is the nested path (e.g., {@code "parent.child"}) and the value is the {@link TableSchema}
     *         corresponding to that level in the object hierarchy.
     */
    public static Map<String, TableSchema<?>> resolveSchemasPerPath(Map<String, AttributeValue> attributesToSet,
                                                              TableSchema<?> rootSchema) {
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
                    Optional<? extends TableSchema<?>> nestedSchema = getNestedSchema(currentSchema, parts[i]);
                    if (nestedSchema.isPresent()) {
                        schemaMap.put(path, nestedSchema.get());
                        currentSchema = nestedSchema.get();
                    }
                } else {
                    currentSchema = schemaMap.get(path);
                }
            }
        }
        return schemaMap;
    }

    public static String reconstructCompositeKey(String path, String attributeName) {
        if (path == null || path.isEmpty()) {
            return attributeName;
        }
        return String.join(NESTED_OBJECT_UPDATE, path.split("\\."))
               + NESTED_OBJECT_UPDATE + attributeName;
    }
}

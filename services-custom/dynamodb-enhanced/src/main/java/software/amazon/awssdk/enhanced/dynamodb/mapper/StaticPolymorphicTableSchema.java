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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

@SdkPublicApi
public final class StaticPolymorphicTableSchema<T> implements TableSchema<T> {

    private final TableSchema<T> rootTableSchema;
    private final String discriminatorAttributeName;
    private final Map<String, StaticSubtype<? extends T>> subtypeByName; // discriminator -> subtype
    private final List<StaticSubtype<? extends T>> subtypes;             // ordered most-specific -> least-specific
    private final boolean allowMissingDiscriminatorFallbackToRoot;

    private StaticPolymorphicTableSchema(TableSchema<T> rootTableSchema,
                                         String discriminatorAttributeName,
                                         Map<String, StaticSubtype<? extends T>> subtypeByName,
                                         List<StaticSubtype<? extends T>> subtypes,
                                         boolean allowMissingDiscriminatorFallbackToRoot) {
        this.rootTableSchema = rootTableSchema;
        this.discriminatorAttributeName = discriminatorAttributeName;
        this.subtypeByName = subtypeByName;
        this.subtypes = subtypes;
        this.allowMissingDiscriminatorFallbackToRoot = allowMissingDiscriminatorFallbackToRoot;
    }

    public static <U> Builder<U> builder(Class<U> itemClass) {
        return new Builder<>(itemClass);
    }

    // Serialization
    @Override
    public Map<String, AttributeValue> itemToMap(T item, boolean ignoreNulls) {
        StaticSubtype<T> subtype = cast(resolveByInstance(item));
        T castItem = subtype.tableSchema().itemType().rawClass().cast(item);

        Map<String, AttributeValue> result = new HashMap<>(subtype.tableSchema().itemToMap(castItem, ignoreNulls));

        result.put(discriminatorAttributeName, AttributeValue.builder().s(subtype.name()).build());
        return result;
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, Collection<String> attributes) {
        StaticSubtype<T> subtype = cast(resolveByInstance(item));
        T castItem = subtype.tableSchema().itemType().rawClass().cast(item);

        Map<String, AttributeValue> result =
            new HashMap<>(subtype.tableSchema().itemToMap(castItem, attributes));

        if (attributes.contains(discriminatorAttributeName)) {
            result.put(discriminatorAttributeName, AttributeValue.builder().s(subtype.name()).build());
        }
        return result;
    }

    // Deserialization
    @Override
    public T mapToItem(Map<String, AttributeValue> attributeMap) {
        String discriminator = Optional.ofNullable(attributeMap.get(discriminatorAttributeName))
                                       .map(AttributeValue::s)
                                       .orElse(null);

        if (discriminator == null) {
            if (allowMissingDiscriminatorFallbackToRoot) {
                // Legacy record (no discriminator) → use root schema
                return rootTableSchema.mapToItem(attributeMap);
            }
            throw new IllegalArgumentException("Missing discriminator '" + discriminatorAttributeName + "' in item map");
        }

        StaticSubtype<? extends T> subtype = subtypeByName.get(discriminator);
        if (subtype == null) {
            throw new IllegalArgumentException("Unknown discriminator '" + discriminator + "'");
        }

        return returnWithSubtypeCast(subtype, ts -> ts.mapToItem(attributeMap));
    }

    @Override
    public AttributeValue attributeValue(T item, String attributeName) {
        if (discriminatorAttributeName.equals(attributeName)) {
            StaticSubtype<? extends T> s = resolveByInstance(item);
            return AttributeValue.builder().s(s.name()).build();
        }

        StaticSubtype<T> subtype = cast(resolveByInstance(item));
        T castItem = subtype.tableSchema().itemType().rawClass().cast(item);
        return subtype.tableSchema().attributeValue(castItem, attributeName);
    }

    @Override
    public TableMetadata tableMetadata() {
        return rootTableSchema.tableMetadata();
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(T itemContext) {
        return resolveByInstance(itemContext).tableSchema();
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(Map<String, AttributeValue> itemContext) {
        String discriminator = Optional.ofNullable(itemContext.get(discriminatorAttributeName))
                                       .map(AttributeValue::s)
                                       .orElse(null);

        if (discriminator == null) {
            if (allowMissingDiscriminatorFallbackToRoot) {
                return rootTableSchema;
            }
            throw new IllegalArgumentException("Missing discriminator '" + discriminatorAttributeName + "' in item map");
        }

        StaticSubtype<? extends T> subtype = subtypeByName.get(discriminator);
        if (subtype == null) {
            throw new IllegalArgumentException("Unknown discriminator '" + discriminator + "'");
        }
        return subtype.tableSchema();
    }

    @Override
    public EnhancedType<T> itemType() {
        return rootTableSchema.itemType();
    }

    @Override
    public List<String> attributeNames() {
        return rootTableSchema.attributeNames();
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public AttributeConverter<T> converterForAttribute(Object key) {
        return rootTableSchema.converterForAttribute(key);
    }

    private StaticSubtype<? extends T> resolveByInstance(T item) {
        for (StaticSubtype<? extends T> s : subtypes) {
            if (s.tableSchema().itemType().rawClass().isInstance(item)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Cannot serialize item of type " + item.getClass().getName());
    }

    private static <T, S extends T> S returnWithSubtypeCast(StaticSubtype<S> subtype, Function<TableSchema<S>, S> fn) {
        S r = fn.apply(subtype.tableSchema());
        return subtype.tableSchema().itemType().rawClass().cast(r);
    }

    @SuppressWarnings("unchecked")
    private static <T> StaticSubtype<T> cast(StaticSubtype<? extends T> s) {
        return (StaticSubtype<T>) s;
    }

    public static final class Builder<T> {
        private TableSchema<T> rootTableSchema;
        private String discriminatorAttributeName;
        private final List<StaticSubtype<? extends T>> staticSubtypes = new ArrayList<>();
        private boolean allowMissingDiscriminatorFallbackToRoot = false;

        private Builder(Class<T> ignored) {
        }

        /**
         * Root (non-polymorphic) schema for the supertype.
         */
        public Builder<T> rootTableSchema(TableSchema<T> root) {
            this.rootTableSchema = root;
            return this;
        }

        /**
         * Discriminator attribute name (defaults to "type").
         */
        public Builder<T> discriminatorAttributeName(String name) {
            this.discriminatorAttributeName = Validate.notEmpty(name, "discriminatorAttributeName");
            return this;
        }

        /**
         * Register one or more subtypes. Order is not required; we will sort most-specific first.
         */
        @SafeVarargs
        public final Builder<T> addStaticSubtype(StaticSubtype<? extends T>... subs) {
            Collections.addAll(this.staticSubtypes, subs);
            return this;
        }

        /**
         * If true, legacy items without a discriminator are deserialized using the root schema. Defaults to false (strict mode).
         */
        public Builder<T> allowMissingDiscriminatorFallbackToRoot(boolean allow) {
            this.allowMissingDiscriminatorFallbackToRoot = allow;
            return this;
        }

        public StaticPolymorphicTableSchema<T> build() {
            // Validate required fields
            Validate.paramNotNull(rootTableSchema, "rootTableSchema");
            Validate.notEmpty(discriminatorAttributeName, "discriminatorAttributeName");
            Validate.notEmpty(staticSubtypes, "A polymorphic TableSchema must have at least one subtype");

            // Each subtype must be assignable to root
            Class<?> root = rootTableSchema.itemType().rawClass();
            for (StaticSubtype<? extends T> s : staticSubtypes) {
                Class<?> sub = s.tableSchema().itemType().rawClass();
                if (!root.isAssignableFrom(sub)) {
                    throw new IllegalArgumentException(
                        "Subtype " + sub.getSimpleName() + " is not assignable to " + root.getSimpleName());
                }
            }

            // Build discriminator map with uniqueness check
            Map<String, StaticSubtype<? extends T>> byName = new LinkedHashMap<>();
            for (StaticSubtype<? extends T> s : staticSubtypes) {
                String key = s.name();
                if (byName.putIfAbsent(key, s) != null) {
                    throw new IllegalArgumentException("Duplicate subtype discriminator: " + key);
                }
            }

            // Sort subtypes so that deeper subclasses (more specific) are checked first by resolveByInstance.
            List<StaticSubtype<? extends T>> ordered = new ArrayList<>(staticSubtypes);
            ordered.sort((first, second) -> Integer.compare(
                inheritanceDepthFromRoot(second.tableSchema().itemType().rawClass(), root),
                inheritanceDepthFromRoot(first.tableSchema().itemType().rawClass(), root)
            ));

            return new StaticPolymorphicTableSchema<>(
                rootTableSchema,
                discriminatorAttributeName,
                Collections.unmodifiableMap(byName),
                Collections.unmodifiableList(ordered),
                allowMissingDiscriminatorFallbackToRoot
            );
        }

        /**
         * Counts how many superclass steps it takes to reach the given root.
         * Example: if Manager extends Employee extends Person (root), then:
         * Manager → depth 2, Employee → depth 1, Person → depth 0.
         */
        private static int inheritanceDepthFromRoot(Class<?> type, Class<?> root) {
            int depth = 0;
            Class<?> current = type;
            while (current != null && !current.equals(root)) {
                current = current.getSuperclass();
                depth++;
            }
            return depth;
        }
    }
}
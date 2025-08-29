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
    private final Map<String, StaticSubtype<? extends T>> subtypeByName;
    private final List<StaticSubtype<? extends T>> subtypes;

    private StaticPolymorphicTableSchema(Builder<T> builder) {
        this.rootTableSchema = Validate.paramNotNull(builder.rootTableSchema, "rootTableSchema");
        this.discriminatorAttributeName = Validate.notEmpty(builder.discriminatorAttributeName, "discriminatorAttributeName");
        Validate.notEmpty(builder.staticSubtypes, "A polymorphic TableSchema must have at least one subtype");

        Map<String, StaticSubtype<? extends T>> map = new LinkedHashMap<>();
        for (StaticSubtype<? extends T> subtype : builder.staticSubtypes) {
            map.compute(subtype.name(), (name, existing) -> {
                if (existing != null) {
                    throw new IllegalArgumentException("Duplicate subtype names are not permitted. [name = \"" + name + "\"]");
                }
                return subtype;
            });
        }

        this.subtypeByName = Collections.unmodifiableMap(map);
        this.subtypes = Collections.unmodifiableList(new ArrayList<>(builder.staticSubtypes));
    }

    public static <T> Builder<T> builder(Class<T> itemClass) {
        return new Builder<>(itemClass);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, boolean ignoreNulls) {
        StaticSubtype<T> subtype = (StaticSubtype<T>) resolveByInstance(item);
        T castItem = subtype.tableSchema()
                            .itemType()
                            .rawClass()
                            .cast(item);

        // copy into a mutable map
        Map<String, AttributeValue> result = new HashMap<>(subtype.tableSchema().itemToMap(castItem, ignoreNulls));

        // inject discriminator
        result.put(discriminatorAttributeName, AttributeValue.builder().s(subtype.name()).build());
        return result;
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, Collection<String> attributes) {
        StaticSubtype<T> subtype = (StaticSubtype<T>) resolveByInstance(item);
        T castItem = subtype.tableSchema()
                            .itemType()
                            .rawClass()
                            .cast(item);

        // Copy into a mutable map so we can inject the discriminator
        Map<String, AttributeValue> result = new HashMap<>(subtype.tableSchema().itemToMap(castItem, attributes));

        // Only inject if they explicitly requested the discriminator field
        if (attributes.contains(discriminatorAttributeName)) {
            result.put(discriminatorAttributeName, AttributeValue.builder().s(subtype.name()).build());
        }

        return result;
    }

    @Override
    public T mapToItem(Map<String, AttributeValue> attributeMap) {
        String discriminator = Optional.ofNullable(attributeMap.get(discriminatorAttributeName))
                              .map(AttributeValue::s)
                              .orElseThrow(() -> new IllegalArgumentException(
                                  "Missing discriminator '" + discriminatorAttributeName + "' in item map"));

        StaticSubtype<? extends T> subtype = subtypeByName.get(discriminator);
        if (subtype == null) {
            throw new IllegalArgumentException("Unknown discriminator '" + discriminator + "'");
        }
        return returnWithSubtypeCast(subtype, ts -> ts.mapToItem(attributeMap));
    }

    @Override
    public AttributeValue attributeValue(T item, String attributeName) {
        // If we want to get the discriminator itself, just return it
        if (discriminatorAttributeName.equals(attributeName)) {
            StaticSubtype<? extends T> raw = resolveByInstance(item);
            return AttributeValue.builder().s(raw.name()).build();
        }

        // Otherwise delegate to the concrete subtype
        StaticSubtype<T> subtype = (StaticSubtype<T>) resolveByInstance(item);

        // Cast the item into the subtype's class
        T castItem = subtype.tableSchema()
                            .itemType()
                            .rawClass()
                            .cast(item);

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
                              .orElseThrow(() -> new IllegalArgumentException(
                                  "Missing discriminator '" + discriminatorAttributeName + "' in item map"));

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

    public static final class Builder<T> {
        private TableSchema<T> rootTableSchema;
        private String discriminatorAttributeName;
        private List<StaticSubtype<? extends T>> staticSubtypes;

        private Builder(Class<T> itemClass) {
        }

        /**
         * The root (monomorphic) schema for the supertype.
         */
        public Builder<T> rootTableSchema(TableSchema<T> root) {
            this.rootTableSchema = root;
            return this;
        }

        /**
         * Optional: override the attribute name used for the discriminator. Defaults to `"type"`.
         */
        public Builder<T> discriminatorAttributeName(String name) {
            this.discriminatorAttributeName = Validate.notEmpty(name, "discriminatorAttributeName");
            return this;
        }

        /**
         * Register one or more (discriminatorValue → subtypeSchema) pairs.
         */
        public Builder<T> addStaticSubtype(StaticSubtype<? extends T>... subs) {
            if (this.staticSubtypes == null) {
                this.staticSubtypes = new ArrayList<>();
            }
            Collections.addAll(this.staticSubtypes, subs);
            return this;
        }

        public StaticPolymorphicTableSchema<T> build() {
            return new StaticPolymorphicTableSchema<>(this);
        }
    }
}

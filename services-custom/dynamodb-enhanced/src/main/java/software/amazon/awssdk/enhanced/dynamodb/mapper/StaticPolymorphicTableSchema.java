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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.SubtypeNameTag;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of {@link TableSchema} that provides polymorphic mapping to and from various subtypes as denoted by
 * a single property of the object that represents the 'subtype name'. In order to build this class, an abstract root
 * {@link TableSchema} must be provided that maps the supertype class, and then a separate concrete {@link TableSchema}
 * that maps each subtype. Each subtype is named, and a string attribute on the root class must be tagged with
 * {@link StaticAttributeTags#subtypeName()} so that any instance of that supertype can have its subtype determined
 * just by looking at the value of that attribute.
 * <p>
 * Example:
 * <p><pre>
 * {@code
 * TableSchema<Animal> ANIMAL_TABLE_SCHEMA =
 *         StaticPolymorphicTableSchema.builder(Animal.class)
 *             .rootTableSchema(ROOT_ANIMAL_TABLE_SCHEMA)
 *             .staticSubtypes(StaticSubtype.builder(Cat.class).names("CAT").tableSchema(CAT_TABLE_SCHEMA).build(),
 *                             StaticSubtype.builder(Snake.class).names("SNAKE").tableSchema(SNAKE_TABLE_SCHEMA).build())
 *             .build();
 * }
 * </pre>
 * @param <T>
 */
@SdkPublicApi
public class StaticPolymorphicTableSchema<T> implements TableSchema<T> {
    private final TableSchema<T> rootTableSchema;
    private final String subtypeAttribute;
    private final Map<String, StaticSubtype<? extends T>> subtypeMap;

    private StaticPolymorphicTableSchema(Builder<T> builder) {
        Validate.notEmpty(builder.staticSubtypes, "A polymorphic TableSchema must have at least one associated subtype");

        this.rootTableSchema = Validate.paramNotNull(builder.rootTableSchema, "rootTableSchema");
        this.subtypeAttribute = SubtypeNameTag.resolve(this.rootTableSchema.tableMetadata()).orElseThrow(
            () -> new IllegalArgumentException("The root TableSchema of a polymorphic TableSchema must tag an attribute to use "
                                               + "as the subtype name so records can be identified as their correct subtype"));

        Map<String, StaticSubtype<? extends T>> subtypeMap = new HashMap<>();

        builder.staticSubtypes.forEach(
            staticSubtype -> staticSubtype.names().forEach(
                name -> subtypeMap.compute(name, (key, existingValue) -> {
                    if (existingValue != null) {
                        throw new IllegalArgumentException("Duplicate subtype names are not permitted. " +
                                                                   "[name = \"" + key + "\"]");
                    }

                    return staticSubtype;
                })));


        this.subtypeMap = Collections.unmodifiableMap(subtypeMap);
    }

    @Override
    public T mapToItem(Map<String, AttributeValue> attributeMap) {
        StaticSubtype<? extends T> subtype = resolveSubtype(attributeMap);
        return returnWithSubtypeCast(subtype, tableSchema -> tableSchema.mapToItem(attributeMap));
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, boolean ignoreNulls) {
        StaticSubtype<? extends T> subtype = resolveSubtype(item);
        return executeWithSubtypeCast(
            item, subtype, (tableSchema, subtypeItem) -> tableSchema.itemToMap(subtypeItem, ignoreNulls));
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, Collection<String> attributes) {
        StaticSubtype<? extends T> subtype = resolveSubtype(item);
        return executeWithSubtypeCast(
            item, subtype, (tableSchema, subtypeItem) -> tableSchema.itemToMap(subtypeItem, attributes));
    }

    @Override
    public AttributeValue attributeValue(T item, String attributeName) {
        StaticSubtype<? extends T> subtype = resolveSubtype(item);
        return executeWithSubtypeCast(
            item, subtype, (tableSchema, subtypeItem) -> tableSchema.attributeValue(subtypeItem, attributeName));
    }

    @Override
    public TableMetadata tableMetadata() {
        return this.rootTableSchema.tableMetadata();
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(T itemContext) {
        StaticSubtype<? extends T> subtype = resolveSubtype(itemContext);
        return subtype.tableSchema();
    }

    @Override
    public TableSchema<? extends T> subtypeTableSchema(Map<String, AttributeValue> itemContext) {
        StaticSubtype<? extends T> subtype = resolveSubtype(itemContext);
        return subtype.tableSchema();
    }

    @Override
    public EnhancedType<T> itemType() {
        return this.rootTableSchema.itemType();
    }

    @Override
    public List<String> attributeNames() {
        return this.rootTableSchema.attributeNames();
    }

    @Override
    public boolean isAbstract() {
        // A polymorphic table schema must always be concrete as Java does not permit multiple class inheritance
        return false;
    }

    private StaticSubtype<? extends T> resolveSubtype(AttributeValue subtypeNameAv) {
        if (subtypeNameAv == null || subtypeNameAv.s() == null || subtypeNameAv.s().isEmpty()) {
            throw new IllegalArgumentException("The subtype name could not be read from the item, either because it is missing "
                                               + "or because it is not a string.");
        }

        String subtypeName = subtypeNameAv.s();
        StaticSubtype<? extends T> subtype = subtypeMap.get(subtypeName);

        if (subtype == null) {
            throw new IllegalArgumentException("The subtype name '" + subtypeName + "' could not be matched to any declared "
                                               + "subtypes of the polymorphic table schema.");
        }

        return subtype;
    }

    private StaticSubtype<? extends T> resolveSubtype(T item) {
        AttributeValue subtypeNameAv = this.rootTableSchema.attributeValue(item, this.subtypeAttribute);
        return resolveSubtype(subtypeNameAv);
    }

    private StaticSubtype<? extends T> resolveSubtype(Map<String, AttributeValue> itemMap) {
        AttributeValue subtypeNameAv = itemMap.get(this.subtypeAttribute);
        return resolveSubtype(subtypeNameAv);
    }

    private static <T, S extends T> S returnWithSubtypeCast(StaticSubtype<S> subtype, Function<TableSchema<S>, S> function) {
        S result = function.apply(subtype.tableSchema());
        return subtype.tableSchema().itemType().rawClass().cast(result);
    }

    private static <T, S extends T, R> R executeWithSubtypeCast(T item,
                                                                StaticSubtype<S> subtype,
                                                                BiFunction<TableSchema<S>, S, R> function) {
        S castItem = subtype.tableSchema().itemType().rawClass().cast(item);
        return function.apply(subtype.tableSchema(), castItem);
    }

    /**
     * Create a builder for a {@link StaticPolymorphicTableSchema}.
     * @param itemClass the class which the {@link StaticPolymorphicTableSchema} will map.
     * @param <T> the type mapped by the table schema.
     * @return A newly initialized builder.
     */
    public static <T> Builder<T> builder(Class<T> itemClass) {
        return new Builder<>();
    }

    /**
     * Builder for a {@link StaticPolymorphicTableSchema}.
     * @param <T> the type that will be mapped by the {@link StaticPolymorphicTableSchema}.
     */
    public static class Builder<T> {
        private List<StaticSubtype<? extends T>> staticSubtypes;
        private TableSchema<T> rootTableSchema;

        private Builder() {
        }

        /**
         * The complete list of subtypes that are mapped by the resulting table schema. Will overwrite any previously
         * specified subtypes.
         */
        @SafeVarargs
        public final Builder<T> staticSubtypes(StaticSubtype<? extends T>... staticSubtypes) {
            this.staticSubtypes = Arrays.asList(staticSubtypes);
            return this;
        }

        /**
         * The complete list of subtypes that are mapped by the resulting table schema. Will overwrite any previously
         * specified subtypes.
         */
        public Builder<T> staticSubtypes(Collection<StaticSubtype<? extends T>> staticSubtypes) {
            this.staticSubtypes = new ArrayList<>(staticSubtypes);
            return this;
        }

        /**
         * Adds a subtype to be mapped by the resulting table schema. Will append to, and not overwrite any previously
         * specified subtypes.
         */
        public Builder<T> addStaticSubtype(StaticSubtype<? extends T> staticSubtype) {
            if (this.staticSubtypes == null) {
                this.staticSubtypes = new ArrayList<>();
            }

            this.staticSubtypes.add(staticSubtype);
            return this;
        }

        /**
         * Specifies the {@link TableSchema} that can be used to map objects of the supertype. It is expected, although
         * not required, that this table schema will be abstract. The root table schema must include a string attribute
         * that is tagged with {@link StaticAttributeTags#subtypeName()} so that the subtype can be determined for any
         * mappable object.
         */
        public Builder<T> rootTableSchema(TableSchema<T> rootTableSchema) {
            this.rootTableSchema = rootTableSchema;
            return this;
        }

        /**
         * Builds an instance of {@link StaticPolymorphicTableSchema} based on the properties of the builder.
         */
        public StaticPolymorphicTableSchema<T> build() {
            return new StaticPolymorphicTableSchema<>(this);
        }
    }
}

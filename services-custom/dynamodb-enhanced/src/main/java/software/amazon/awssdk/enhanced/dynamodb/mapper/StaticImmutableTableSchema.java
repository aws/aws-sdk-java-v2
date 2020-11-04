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

import static java.util.Collections.unmodifiableMap;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.isNullAttributeValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterProviderResolver;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ResolvedImmutableAttribute;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Implementation of {@link TableSchema} that builds a schema for immutable data objects based on directly declared
 * attributes. Just like {@link StaticTableSchema} which is the equivalent implementation for mutable objects, this is
 * the most direct, and thus fastest, implementation of {@link TableSchema}.
 * <p>
 * Example using a fictional 'Customer' immutable data item class that has an inner builder class named 'Builder':-
 * {@code
 * static final TableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
 *      StaticImmutableTableSchema.builder(Customer.class, Customer.Builder.class)
 *        .newItemBuilder(Customer::builder, Customer.Builder::build)
 *        .addAttribute(String.class, a -> a.name("account_id")
 *                                          .getter(Customer::accountId)
 *                                          .setter(Customer.Builder::accountId)
 *                                          .tags(primaryPartitionKey()))
 *        .addAttribute(Integer.class, a -> a.name("sub_id")
 *                                           .getter(Customer::subId)
 *                                           .setter(Customer.Builder::subId)
 *                                           .tags(primarySortKey()))
 *        .addAttribute(String.class, a -> a.name("name")
 *                                          .getter(Customer::name)
 *                                          .setter(Customer.Builder::name)
 *                                          .tags(secondaryPartitionKey("customers_by_name")))
 *        .addAttribute(Instant.class, a -> a.name("created_date")
 *                                           .getter(Customer::createdDate)
 *                                           .setter(Customer.Builder::createdDate)
 *                                           .tags(secondarySortKey("customers_by_date"),
 *                                                 secondarySortKey("customers_by_name")))
 *        .build();
 * }
 */
@SdkPublicApi
public final class StaticImmutableTableSchema<T, B> implements TableSchema<T> {
    private final List<ResolvedImmutableAttribute<T, B>> attributeMappers;
    private final Supplier<B> newBuilderSupplier;
    private final Function<B, T> buildItemFunction;
    private final Map<String, ResolvedImmutableAttribute<T, B>> indexedMappers;
    private final StaticTableMetadata tableMetadata;
    private final EnhancedType<T> itemType;
    private final AttributeConverterProvider attributeConverterProvider;
    private final Map<String, FlattenedMapper<T, B, ?>> indexedFlattenedMappers;
    private final List<String> attributeNames;
    
    private static class FlattenedMapper<T, B, T1> {
        private final Function<T, T1> otherItemGetter;
        private final BiConsumer<B, T1> otherItemSetter;
        private final TableSchema<T1> otherItemTableSchema;

        private FlattenedMapper(Function<T, T1> otherItemGetter,
                                BiConsumer<B, T1> otherItemSetter,
                                TableSchema<T1> otherItemTableSchema) {
            this.otherItemGetter = otherItemGetter;
            this.otherItemSetter = otherItemSetter;
            this.otherItemTableSchema = otherItemTableSchema;


        }

        public TableSchema<T1> getOtherItemTableSchema() {
            return otherItemTableSchema;
        }

        private B mapToItem(B thisBuilder,
                            Supplier<B> thisBuilderConstructor,
                            Map<String, AttributeValue> attributeValues) {
            T1 otherItem = this.otherItemTableSchema.mapToItem(attributeValues);

            if (otherItem != null) {
                if (thisBuilder == null) {
                    thisBuilder = thisBuilderConstructor.get();
                }

                this.otherItemSetter.accept(thisBuilder, otherItem);
            }

            return thisBuilder;
        }

        private Map<String, AttributeValue> itemToMap(T item, boolean ignoreNulls) {
            T1 otherItem = this.otherItemGetter.apply(item);

            if (otherItem == null) {
                return Collections.emptyMap();
            }

            return this.otherItemTableSchema.itemToMap(otherItem, ignoreNulls);
        }

        private AttributeValue attributeValue(T item, String attributeName) {
            T1 otherItem = this.otherItemGetter.apply(item);

            if (otherItem == null) {
                return null;
            }

            AttributeValue attributeValue = this.otherItemTableSchema.attributeValue(otherItem, attributeName);
            return isNullAttributeValue(attributeValue) ? null : attributeValue;
        }
    }
    
    private StaticImmutableTableSchema(Builder<T, B> builder) {
        StaticTableMetadata.Builder tableMetadataBuilder = StaticTableMetadata.builder();

        this.attributeConverterProvider =
                ConverterProviderResolver.resolveProviders(builder.attributeConverterProviders);

        // Resolve declared attributes and find converters for them
        Stream<ResolvedImmutableAttribute<T, B>> attributesStream = builder.attributes == null ?
            Stream.empty() : builder.attributes.stream().map(a -> a.resolve(this.attributeConverterProvider));

        // Merge resolved declared attributes
        List<ResolvedImmutableAttribute<T, B>> mutableAttributeMappers = new ArrayList<>();
        Map<String, ResolvedImmutableAttribute<T, B>>  mutableIndexedMappers = new HashMap<>();
        Set<String> mutableAttributeNames = new LinkedHashSet<>();
        Stream.concat(attributesStream, builder.additionalAttributes.stream()).forEach(
            resolvedAttribute -> {
                String attributeName = resolvedAttribute.attributeName();

                if (mutableAttributeNames.contains(attributeName)) {
                    throw new IllegalArgumentException(
                        "Attempt to add an attribute to a mapper that already has one with the same name. " +
                            "[Attribute name: " + attributeName + "]");
                }

                mutableAttributeNames.add(attributeName);
                mutableAttributeMappers.add(resolvedAttribute);
                mutableIndexedMappers.put(attributeName, resolvedAttribute);

                // Merge in metadata associated with attribute
                tableMetadataBuilder.mergeWith(resolvedAttribute.tableMetadata());
            }
        );

        Map<String, FlattenedMapper<T, B, ?>> mutableFlattenedMappers = new HashMap<>();
        builder.flattenedMappers.forEach(
            flattenedMapper -> {
                flattenedMapper.otherItemTableSchema.attributeNames().forEach(
                    attributeName -> {
                        if (mutableAttributeNames.contains(attributeName)) {
                            throw new IllegalArgumentException(
                                "Attempt to add an attribute to a mapper that already has one with the same name. " +
                                    "[Attribute name: " + attributeName + "]");
                        }

                        mutableAttributeNames.add(attributeName);
                        mutableFlattenedMappers.put(attributeName, flattenedMapper);
                    }
                );

                tableMetadataBuilder.mergeWith(flattenedMapper.getOtherItemTableSchema().tableMetadata());
            }
        );

        // Apply table-tags to table metadata
        if (builder.tags != null) {
            builder.tags.forEach(staticTableTag -> staticTableTag.modifyMetadata().accept(tableMetadataBuilder));
        }

        this.attributeMappers = Collections.unmodifiableList(mutableAttributeMappers);
        this.indexedMappers = Collections.unmodifiableMap(mutableIndexedMappers);
        this.attributeNames = Collections.unmodifiableList(new ArrayList<>(mutableAttributeNames));
        this.indexedFlattenedMappers = Collections.unmodifiableMap(mutableFlattenedMappers);
        this.newBuilderSupplier = builder.newBuilderSupplier;
        this.buildItemFunction = builder.buildItemFunction;
        this.tableMetadata = tableMetadataBuilder.build();
        this.itemType = EnhancedType.of(builder.itemClass);
    }

    /**
     * Creates a builder for a {@link StaticImmutableTableSchema} typed to specific immutable data item class.
     * @param itemClass The immutable data item class object that the {@link StaticImmutableTableSchema} is to map to.
     * @param builderClass The builder class object that can be used to construct instances of the immutable data item.
     * @return A newly initialized builder
     */
    public static <T, B> Builder<T, B> builder(Class<T> itemClass, Class<B> builderClass) {
        return new Builder<>(itemClass, builderClass);
    }

    /**
     * Builder for a {@link StaticImmutableTableSchema}
     * @param <T> The immutable data item class object that the {@link StaticImmutableTableSchema} is to map to.
     * @param <B> The builder class object that can be used to construct instances of the immutable data item.
     */
    public static final class Builder<T, B> {
        private final Class<T> itemClass;
        private final Class<B> builderClass;
        private final List<ResolvedImmutableAttribute<T, B>> additionalAttributes = new ArrayList<>();
        private final List<FlattenedMapper<T, B, ?>> flattenedMappers = new ArrayList<>();

        private List<ImmutableAttribute<T, B, ?>> attributes;
        private Supplier<B> newBuilderSupplier;
        private Function<B, T> buildItemFunction;
        private List<StaticTableTag> tags;
        private List<AttributeConverterProvider> attributeConverterProviders =
            Collections.singletonList(ConverterProviderResolver.defaultConverterProvider());

        private Builder(Class<T> itemClass, Class<B> builderClass) {
            this.itemClass = itemClass;
            this.builderClass = builderClass;
        }

        /**
         * Methods used to construct a new instance of the immutable data object.
         * @param newBuilderMethod A method to create a new builder for the immutable data object.
         * @param buildMethod A method on the builder to build a new instance of the immutable data object.
         */
        public Builder<T, B> newItemBuilder(Supplier<B> newBuilderMethod, Function<B, T> buildMethod) {
            this.newBuilderSupplier = newBuilderMethod;
            this.buildItemFunction = buildMethod;
            return this;
        }

        /**
         * A list of attributes that can be mapped between the data item object and the database record that are to
         * be associated with the schema. Will overwrite any existing attributes.
         */
        @SafeVarargs
        public final Builder<T, B> attributes(ImmutableAttribute<T, B, ?>... immutableAttributes) {
            this.attributes = Arrays.asList(immutableAttributes);
            return this;
        }

        /**
         * A list of attributes that can be mapped between the data item object and the database record that are to
         * be associated with the schema. Will overwrite any existing attributes.
         */
        public Builder<T, B> attributes(Collection<ImmutableAttribute<T, B, ?>> immutableAttributes) {
            this.attributes = new ArrayList<>(immutableAttributes);
            return this;
        }

        /**
         * Adds a single attribute to the table schema that can be mapped between the data item object and the database
         * record.
         */
        public <R> Builder<T, B> addAttribute(EnhancedType<R> attributeType,
                                              Consumer<ImmutableAttribute.Builder<T, B, R>> immutableAttribute) {

            ImmutableAttribute.Builder<T, B, R> builder =
                ImmutableAttribute.builder(itemClass, builderClass, attributeType);
            immutableAttribute.accept(builder);
            return addAttribute(builder.build());
        }

        /**
         * Adds a single attribute to the table schema that can be mapped between the data item object and the database
         * record.
         */
        public <R> Builder<T, B> addAttribute(Class<R> attributeClass,
                                              Consumer<ImmutableAttribute.Builder<T, B, R>> immutableAttribute) {
            return addAttribute(EnhancedType.of(attributeClass), immutableAttribute);
        }

        /**
         * Adds a single attribute to the table schema that can be mapped between the data item object and the database
         * record.
         */
        public Builder<T, B> addAttribute(ImmutableAttribute<T, B, ?> immutableAttribute) {
            if (this.attributes == null) {
                this.attributes = new ArrayList<>();
            }

            this.attributes.add(immutableAttribute);
            return this;
        }

        /**
         * Associate one or more {@link StaticTableTag} with this schema. See documentation on the tags themselves to
         * understand what each one does. This method will overwrite any existing table tags.
         */
        public Builder<T, B> tags(StaticTableTag... staticTableTags) {
            this.tags = Arrays.asList(staticTableTags);
            return this;
        }

        /**
         * Associate one or more {@link StaticTableTag} with this schema. See documentation on the tags themselves to
         * understand what each one does. This method will overwrite any existing table tags.
         */
        public Builder<T, B> tags(Collection<StaticTableTag> staticTableTags) {
            this.tags = new ArrayList<>(staticTableTags);
            return this;
        }

        /**
         * Associates a {@link StaticTableTag} with this schema. See documentation on the tags themselves to understand
         * what each one does. This method will add the tag to the list of existing table tags.
         */
        public Builder<T, B> addTag(StaticTableTag staticTableTag) {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }

            this.tags.add(staticTableTag);
            return this;
        }

        /**
         * Flattens all the attributes defined in another {@link TableSchema} into the database record this schema
         * maps to. Functions to get and set an object that the flattened schema maps to is required.
         */
        public <T1> Builder<T, B> flatten(TableSchema<T1> otherTableSchema,
                                          Function<T, T1> otherItemGetter,
                                          BiConsumer<B, T1> otherItemSetter) {
            if (otherTableSchema.isAbstract()) {
                throw new IllegalArgumentException("Cannot flatten an abstract TableSchema. You must supply a concrete " +
                                                       "TableSchema that is able to create items");
            }

            FlattenedMapper<T, B, T1> flattenedMapper = 
                new FlattenedMapper<>(otherItemGetter, otherItemSetter, otherTableSchema);
            this.flattenedMappers.add(flattenedMapper);
            return this;
        }

        /**
         * Extends the {@link StaticImmutableTableSchema} of a super-class, effectively rolling all the attributes modelled by
         * the super-class into the {@link StaticImmutableTableSchema} of the sub-class. The extended immutable table schema
         * must be using a builder class that is also a super-class of the builder being used for the current immutable
         * table schema.
         */
        public Builder<T, B> extend(StaticImmutableTableSchema<? super T, ? super B> superTableSchema) {
            Stream<ResolvedImmutableAttribute<T, B>> attributeStream =
                upcastingTransformForAttributes(superTableSchema.attributeMappers);
            attributeStream.forEach(this.additionalAttributes::add);
            return this;
        }

        /**
         * Specifies the {@link AttributeConverterProvider}s to use with the table schema.
         * The list of attribute converter providers must provide {@link AttributeConverter}s for all types used
         * in the schema. The attribute converter providers will be loaded in the strict order they are supplied here.
         * <p>
         * Calling this method will override the default attribute converter provider
         * {@link DefaultAttributeConverterProvider}, which provides standard converters for most primitive
         * and common Java types, so that provider must included in the supplied list if it is to be
         * used. Providing an empty list here will cause no providers to get loaded.
         * <p>
         * Adding one custom attribute converter provider and using the default as fallback:
         * {@code
         * builder.attributeConverterProviders(customAttributeConverter, AttributeConverterProvider.defaultProvider())
         * }
         *
         * @param attributeConverterProviders a list of attribute converter providers to use with the table schema
         */
        public Builder<T, B> attributeConverterProviders(AttributeConverterProvider... attributeConverterProviders) {
            this.attributeConverterProviders = Arrays.asList(attributeConverterProviders);
            return this;
        }

        /**
         * Specifies the {@link AttributeConverterProvider}s to use with the table schema.
         * The list of attribute converter providers must provide {@link AttributeConverter}s for all types used
         * in the schema. The attribute converter providers will be loaded in the strict order they are supplied here.
         * <p>
         * Calling this method will override the default attribute converter provider
         * {@link DefaultAttributeConverterProvider}, which provides standard converters
         * for most primitive and common Java types, so that provider must included in the supplied list if it is to be
         * used. Providing an empty list here will cause no providers to get loaded.
         * <p>
         * Adding one custom attribute converter provider and using the default as fallback:
         * {@code
         * List<AttributeConverterProvider> providers = new ArrayList<>(
         *     customAttributeConverter,
         *     AttributeConverterProvider.defaultProvider());
         * builder.attributeConverterProviders(providers);
         * }
         *
         * @param attributeConverterProviders a list of attribute converter providers to use with the table schema
         */
        public Builder<T, B> attributeConverterProviders(List<AttributeConverterProvider> attributeConverterProviders) {
            this.attributeConverterProviders = new ArrayList<>(attributeConverterProviders);
            return this;
        }


        /**
         * Builds a {@link StaticImmutableTableSchema} based on the values this builder has been configured with
         */
        public StaticImmutableTableSchema<T, B> build() {
            return new StaticImmutableTableSchema<>(this);
        }

        private static <T extends T1, T1, B extends B1, B1> Stream<ResolvedImmutableAttribute<T, B>>
            upcastingTransformForAttributes(Collection<ResolvedImmutableAttribute<T1, B1>> superAttributes) {

            return superAttributes.stream().map(attribute -> attribute.transform(x -> x, x -> x));
        }
    }

    @Override
    public StaticTableMetadata tableMetadata() {
        return tableMetadata;
    }

    @Override
    public T mapToItem(Map<String, AttributeValue> attributeMap) {
        // Lazily instantiate the builder once we have an attribute to write
        B builder = null;
        Map<FlattenedMapper<T, B, ?>, Map<String, AttributeValue>> flattenedAttributeValuesMap = new LinkedHashMap<>();
        
        for (Map.Entry<String, AttributeValue> entry : attributeMap.entrySet()) {
            String key = entry.getKey();
            AttributeValue value = entry.getValue();
            
            if (!isNullAttributeValue(value)) {
                ResolvedImmutableAttribute<T, B> attributeMapper = indexedMappers.get(key);

                if (attributeMapper != null) {
                    if (builder == null) {
                        builder = constructNewBuilder();
                    }

                    attributeMapper.updateItemMethod().accept(builder, value);
                } else {
                    FlattenedMapper<T, B, ?> flattenedMapper = this.indexedFlattenedMappers.get(key);

                    if (flattenedMapper != null) {
                        Map<String, AttributeValue> flattenedAttributeValues = 
                            flattenedAttributeValuesMap.get(flattenedMapper);
                        
                        if (flattenedAttributeValues == null) {
                            flattenedAttributeValues = new HashMap<>();
                        }
                        
                        flattenedAttributeValues.put(key, value);
                        flattenedAttributeValuesMap.put(flattenedMapper, flattenedAttributeValues);
                    }
                }
            }
        }

        for (Map.Entry<FlattenedMapper<T, B, ?>, Map<String, AttributeValue>> entry :
                flattenedAttributeValuesMap.entrySet()) {
            builder = entry.getKey().mapToItem(builder, this::constructNewBuilder, entry.getValue());
        }
        
        return builder == null ? null : buildItemFunction.apply(builder);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, boolean ignoreNulls) {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();

        attributeMappers.forEach(attributeMapper -> {
            String attributeKey = attributeMapper.attributeName();
            AttributeValue attributeValue = attributeMapper.attributeGetterMethod().apply(item);

            if (!ignoreNulls || !isNullAttributeValue(attributeValue)) {
                attributeValueMap.put(attributeKey, attributeValue);
            }
        });

        indexedFlattenedMappers.forEach((name, flattenedMapper) -> {
            attributeValueMap.putAll(flattenedMapper.itemToMap(item, ignoreNulls));
        });

        return unmodifiableMap(attributeValueMap);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, Collection<String> attributes) {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();

        attributes.forEach(key -> {
            AttributeValue attributeValue = attributeValue(item, key);

            if (attributeValue == null || !isNullAttributeValue(attributeValue)) {
                attributeValueMap.put(key, attributeValue);
            }
        });

        return unmodifiableMap(attributeValueMap);
    }

    @Override
    public AttributeValue attributeValue(T item, String key) {
        ResolvedImmutableAttribute<T, B> attributeMapper = indexedMappers.get(key);

        if (attributeMapper == null) {
            FlattenedMapper<T, B, ?> flattenedMapper = indexedFlattenedMappers.get(key);

            if (flattenedMapper == null) {
                throw new IllegalArgumentException(String.format("TableSchema does not know how to retrieve requested "
                                                                     + "attribute '%s' from mapped object.", key));
            }

            return flattenedMapper.attributeValue(item, key);
        }

        AttributeValue attributeValue = attributeMapper.attributeGetterMethod().apply(item);

        return isNullAttributeValue(attributeValue) ? null : attributeValue;
    }

    @Override
    public EnhancedType<T> itemType() {
        return this.itemType;
    }

    @Override
    public List<String> attributeNames() {
        return this.attributeNames;
    }

    @Override
    public boolean isAbstract() {
        return this.buildItemFunction == null;
    }

    /**
     * The table schema {@link AttributeConverterProvider}.
     * @see Builder#attributeConverterProvider
     */
    public AttributeConverterProvider attributeConverterProvider() {
        return this.attributeConverterProvider;
    }

    private B constructNewBuilder() {
        if (newBuilderSupplier == null) {
            throw new UnsupportedOperationException("An abstract TableSchema cannot be used to map a database record "
                                                    + "to a concrete object. Add a 'newItemBuilder' to the "
                                                    + "TableSchema to give it the ability to create mapped objects.");
        }

        return newBuilderSupplier.get();
    }
}

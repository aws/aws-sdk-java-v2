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
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ResolvedStaticAttribute;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Implementation of {@link TableSchema} that builds a schema based on directly declared attributes and methods to
 * get and set those attributes. This is the most direct, and thus fastest, implementation of {@link TableSchema}.
 * <p>
 * Example using a fictional 'Customer' data item class:-
 * <pre>{@code
 * static final TableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
 *      StaticTableSchema.builder(Customer.class)
 *        .newItemSupplier(Customer::new)
 *        .addAttribute(String.class, a -> a.name("account_id")
 *                                          .getter(Customer::getAccountId)
 *                                          .setter(Customer::setAccountId)
 *                                          .tags(primaryPartitionKey()))
 *        .addAttribute(Integer.class, a -> a.name("sub_id")
 *                                           .getter(Customer::getSubId)
 *                                           .setter(Customer::setSubId)
 *                                           .tags(primarySortKey()))
 *        .addAttribute(String.class, a -> a.name("name")
 *                                          .getter(Customer::getName)
 *                                          .setter(Customer::setName)
 *                                          .tags(secondaryPartitionKey("customers_by_name")))
 *        .addAttribute(Instant.class, a -> a.name("created_date")
 *                                           .getter(Customer::getCreatedDate)
 *                                           .setter(Customer::setCreatedDate)
 *                                           .tags(secondarySortKey("customers_by_date"),
 *                                                 secondarySortKey("customers_by_name")))
 *        .build();
 * }</pre>
 */
@SdkPublicApi
public final class StaticTableSchema<T> implements TableSchema<T> {
    private final List<ResolvedStaticAttribute<T>> attributeMappers;
    private final Supplier<T> newItemSupplier;
    private final Map<String, ResolvedStaticAttribute<T>> indexedMappers;
    private final StaticTableMetadata tableMetadata;
    private final EnhancedType<T> itemType;
    private final AttributeConverterProvider attributeConverterProvider;

    private StaticTableSchema(Builder<T> builder) {
        StaticTableMetadata.Builder tableMetadataBuilder = StaticTableMetadata.builder();

        this.attributeConverterProvider =
                ConverterProviderResolver.resolveProviders(builder.attributeConverterProviders);

        // Resolve declared attributes and find converters for them
        Stream<ResolvedStaticAttribute<T>> attributesStream = builder.attributes == null ?
            Stream.empty() : builder.attributes.stream().map(a -> a.resolve(this.attributeConverterProvider));

        // Merge resolved declared attributes and additional attributes that were added by extend or flatten
        List<ResolvedStaticAttribute<T>> mutableAttributeMappers = new ArrayList<>();
        Map<String, ResolvedStaticAttribute<T>>  mutableIndexedMappers = new HashMap<>();
        Stream.concat(attributesStream, builder.additionalAttributes.stream()).forEach(
            resolvedAttribute -> {
                String attributeName = resolvedAttribute.attributeName();

                if (mutableIndexedMappers.containsKey(attributeName)) {
                    throw new IllegalArgumentException(
                        "Attempt to add an attribute to a mapper that already has one with the same name. " +
                            "[Attribute name: " + attributeName + "]");
                }

                mutableAttributeMappers.add(resolvedAttribute);
                mutableIndexedMappers.put(attributeName, resolvedAttribute);

                // Merge in metadata associated with attribute
                tableMetadataBuilder.mergeWith(resolvedAttribute.tableMetadata());
            }
        );

        // Apply table-tags to table metadata
        if (builder.tags != null) {
            builder.tags.forEach(staticTableTag -> staticTableTag.modifyMetadata().accept(tableMetadataBuilder));
        }

        this.attributeMappers = Collections.unmodifiableList(mutableAttributeMappers);
        this.indexedMappers = Collections.unmodifiableMap(mutableIndexedMappers);
        this.newItemSupplier = builder.newItemSupplier;
        this.tableMetadata = tableMetadataBuilder.build();
        this.itemType = EnhancedType.of(builder.itemClass);
    }

    /**
     * Creates a builder for a {@link StaticTableSchema} typed to specific data item class.
     * @param itemClass The data item class object that the {@link StaticTableSchema} is to map to.
     * @return A newly initialized builder
     */
    public static <T> Builder<T> builder(Class<T> itemClass) {
        return new Builder<>(itemClass);
    }

    /**
     * Builder for a {@link StaticTableSchema}
     * @param <T> The data item type that the {@link StaticTableSchema} this builder will build is to map to.
     */
    public static final class Builder<T> {
        private final Class<T> itemClass;
        private final List<ResolvedStaticAttribute<T>> additionalAttributes = new ArrayList<>();

        private List<StaticAttribute<T, ?>> attributes;
        private Supplier<T> newItemSupplier;
        private List<StaticTableTag> tags;
        private List<AttributeConverterProvider> attributeConverterProviders =
            Collections.singletonList(ConverterProviderResolver.defaultConverterProvider());

        private Builder(Class<T> itemClass) {
            this.itemClass = itemClass;
        }

        /**
         * A function that can be used to create new instances of the data item class.
         */
        public Builder<T> newItemSupplier(Supplier<T> newItemSupplier) {
            this.newItemSupplier = newItemSupplier;
            return this;
        }

        /**
         * A list of attributes that can be mapped between the data item object and the database record that are to
         * be associated with the schema. Will overwrite any existing attributes.
         */
        @SafeVarargs
        public final Builder<T> attributes(StaticAttribute<T, ?>... staticAttributes) {
            this.attributes = Arrays.asList(staticAttributes);
            return this;
        }

        /**
         * A list of attributes that can be mapped between the data item object and the database record that are to
         * be associated with the schema. Will overwrite any existing attributes.
         */
        public Builder<T> attributes(Collection<StaticAttribute<T, ?>> staticAttributes) {
            this.attributes = new ArrayList<>(staticAttributes);
            return this;
        }

        /**
         * Adds a single attribute to the table schema that can be mapped between the data item object and the database
         * record.
         */
        public <R> Builder<T> addAttribute(EnhancedType<R> attributeType,
                                           Consumer<StaticAttribute.Builder<T, R>> staticAttribute) {

            StaticAttribute.Builder<T, R> builder = StaticAttribute.builder(itemClass, attributeType);
            staticAttribute.accept(builder);
            return addAttribute(builder.build());
        }

        /**
         * Adds a single attribute to the table schema that can be mapped between the data item object and the database
         * record.
         */
        public <R> Builder<T> addAttribute(Class<R> attributeClass,
                                           Consumer<StaticAttribute.Builder<T, R>> staticAttribute) {
            return addAttribute(EnhancedType.of(attributeClass), staticAttribute);
        }

        /**
         * Adds a single attribute to the table schema that can be mapped between the data item object and the database
         * record.
         */
        public Builder<T> addAttribute(StaticAttribute<T, ?> staticAttribute) {
            if (this.attributes == null) {
                this.attributes = new ArrayList<>();
            }

            this.attributes.add(staticAttribute);
            return this;
        }

        /**
         * Flattens all the attributes defined in another {@link StaticTableSchema} into the database record this schema
         * maps to. Functions to get and set an object that the flattened schema maps to is required.
         */
        public <R> Builder<T> flatten(StaticTableSchema<R> otherTableSchema,
                                      Function<T, R> otherItemGetter,
                                      BiConsumer<T, R> otherItemSetter) {
            if (otherTableSchema.newItemSupplier == null) {
                throw new IllegalArgumentException("Cannot flatten an abstract StaticTableSchema. Add a "
                                                   + "'newItemSupplier' to the other StaticTableSchema to make it "
                                                   + "concrete.");
            }

            // Creates a consumer that given a parent object will instantiate the composed object if its value is
            // currently null and call the setter to store it on the parent object.
            Consumer<T> composedObjectConstructor = parentObject -> {
                if (otherItemGetter.apply(parentObject) == null) {
                    R compositeItem = otherTableSchema.newItemSupplier.get();
                    otherItemSetter.accept(parentObject, compositeItem);
                }
            };

            otherTableSchema.attributeMappers.stream()
                                             .map(attribute -> attribute.transform(otherItemGetter,
                                                                                   composedObjectConstructor))
                                             .forEach(this.additionalAttributes::add);
            return this;
        }

        /**
         * Extends the {@link StaticTableSchema} of a super-class, effectively rolling all the attributes modelled by
         * the super-class into the {@link StaticTableSchema} of the sub-class.
         */
        public Builder<T> extend(StaticTableSchema<? super T> superTableSchema) {
            Stream<ResolvedStaticAttribute<T>> attributeStream =
                upcastingTransformForAttributes(superTableSchema.attributeMappers);
            attributeStream.forEach(this.additionalAttributes::add);
            return this;
        }

        /**
         * Associate one or more {@link StaticTableTag} with this schema. See documentation on the tags themselves to
         * understand what each one does. This method will overwrite any existing table tags.
         */
        public Builder<T> tags(StaticTableTag... staticTableTags) {
            this.tags = Arrays.asList(staticTableTags);
            return this;
        }

        /**
         * Associate one or more {@link StaticTableTag} with this schema. See documentation on the tags themselves to
         * understand what each one does. This method will overwrite any existing table tags.
         */
        public Builder<T> tags(Collection<StaticTableTag> staticTableTags) {
            this.tags = new ArrayList<>(staticTableTags);
            return this;
        }

        /**
         * Associates a {@link StaticTableTag} with this schema. See documentation on the tags themselves to understand
         * what each one does. This method will add the tag to the list of existing table tags.
         */
        public Builder<T> addTag(StaticTableTag staticTableTag) {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }

            this.tags.add(staticTableTag);
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
        public Builder<T> attributeConverterProviders(AttributeConverterProvider... attributeConverterProviders) {
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
        public Builder<T> attributeConverterProviders(List<AttributeConverterProvider> attributeConverterProviders) {
            this.attributeConverterProviders = new ArrayList<>(attributeConverterProviders);
            return this;
        }


        /**
         * Builds a {@link StaticTableSchema} based on the values this builder has been configured with
         */
        public StaticTableSchema<T> build() {
            return new StaticTableSchema<>(this);
        }

        private static <T extends R, R> Stream<ResolvedStaticAttribute<T>> upcastingTransformForAttributes(
            Collection<ResolvedStaticAttribute<R>> superAttributes) {
            return superAttributes.stream().map(attribute -> attribute.transform(x -> x, null));
        }
    }

    @Override
    public StaticTableMetadata tableMetadata() {
        return tableMetadata;
    }

    @Override
    public T mapToItem(Map<String, AttributeValue> attributeMap) {
        // Lazily instantiate the item once we have an attribute to write
        T item = null;

        for (Map.Entry<String, AttributeValue> entry : attributeMap.entrySet()) {
            String key = entry.getKey();
            AttributeValue value = entry.getValue();
            if (!isNullAttributeValue(value)) {
                ResolvedStaticAttribute<T> attributeMapper = indexedMappers.get(key);

                if (attributeMapper != null) {
                    if (item == null) {
                        item = constructNewItem();
                    }

                    attributeMapper.updateItemMethod().accept(item, value);
                }
            }
        }

        return item;
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
        ResolvedStaticAttribute<T> attributeMapper = indexedMappers.get(key);

        if (attributeMapper == null) {
            throw new IllegalArgumentException(String.format("TableSchema does not know how to retrieve requested "
                                                             + "attribute '%s' from mapped object.", key));
        }

        AttributeValue attributeValue = attributeMapper.attributeGetterMethod().apply(item);

        return isNullAttributeValue(attributeValue) ? null : attributeValue;
    }

    @Override
    public EnhancedType<T> itemType() {
        return this.itemType;
    }

    /**
     * The table schema {@link AttributeConverterProvider}.
     * @see Builder#attributeConverterProvider
     */
    public AttributeConverterProvider attributeConverterProvider() {
        return this.attributeConverterProvider;
    }

    private T constructNewItem() {
        if (newItemSupplier == null) {
            throw new UnsupportedOperationException("An abstract TableSchema cannot be used to map a database record "
                                                    + "to a concrete object. Add a 'newItemSupplier' to the "
                                                    + "TableSchema to give it the ability to create mapped objects.");
        }

        return newItemSupplier.get();
    }
}

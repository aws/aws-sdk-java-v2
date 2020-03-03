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

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.isNullAttributeValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticTableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Implementation of {@link TableSchema} that builds a schema based on directly declared attributes and methods to
 * get and set those attributes. This is the most direct, and thus fastest, implementation of {@link TableSchema}.
 * <p>
 * Example using a fictional 'Customer' data item class:-
 * {@code
 * static final TableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
 *      StaticTableSchema.builder(Customer.class)
 *        .newItemSupplier(Customer::new)
 *        .attributes(
 *          stringAttribute("account_id",
 *                          Customer::getAccountId,
 *                          Customer::setAccountId)
 *             .as(primaryPartitionKey()),
 *          integerNumberAttribute("sub_id",
 *                                 Customer::getSubId,
 *                                 Customer::setSubId)
 *             .as(primarySortKey()),
 *          stringAttribute("name",
 *                          Customer::getName,
 *                          Customer::setName)
 *             .as(secondaryPartitionKey("customers_by_name")),
 *          stringAttribute("created_date",
 *                          Customer::getCreatedDate,
 *                          Customer::setCreatedDate)
 *             .as(secondarySortKey("customers_by_date"),
 *                 secondarySortKey("customers_by_name")))
 *        .build();
 * }
 */
@SdkPublicApi
public final class StaticTableSchema<T> implements TableSchema<T> {
    private final List<Attribute<T>> attributeMappers;
    private final Supplier<T> newItemSupplier;
    private final Map<String, Attribute<T>> indexedMappers;
    private final StaticTableMetadata tableMetadata;

    private StaticTableSchema(List<Attribute<T>> attributeMappers,
                              Supplier<T> newItemSupplier,
                              StaticTableMetadata tableMetadata) {
        this.attributeMappers = attributeMappers;
        this.newItemSupplier = newItemSupplier;
        this.tableMetadata = tableMetadata;

        indexedMappers =
            unmodifiableMap(
                attributeMappers.stream()
                                .collect(Collectors.toMap(Attribute::attributeName, Function.identity())));
    }

    /**
     * Creates a builder for a {@link StaticTableSchema} typed to specific data item class.
     * @param itemClass The data item class object that the {@link StaticTableSchema} is to map to.
     * @return A newly initialized builder
     */
    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new Builder<>();
    }

    /**
     * Builder for a {@link StaticTableSchema}
     * @param <T> The data item type that the {@link StaticTableSchema} this builder will build is to map to.
     */
    public static final class Builder<T> {
        private Supplier<T> newItemSupplier;
        private StaticTableMetadata.Builder tableMetadataBuilder = StaticTableMetadata.builder();
        private final List<Attribute<T>> mappedAttributes = new ArrayList<>();
        private final Map<String, Attribute<T>> indexedMappers = new HashMap<>();

        private Builder() {
        }

        /**
         * A function that can be used to create new instances of the data item class.
         */
        public Builder<T> newItemSupplier(Supplier<T> newItemSupplier) {
            this.newItemSupplier = newItemSupplier;
            return this;
        }

        /**
         * A list of attributes that can be mapped between the data item object and the database record.
         */
        @SafeVarargs
        public final Builder<T> attributes(Attribute.AttributeSupplier<T>... mappedAttributes) {
            stream(mappedAttributes).map(Supplier::get).forEach(this::mergeAttribute);
            return this;
        }

        /**
         * A list of attributes that can be mapped between the data item object and the database record.
         */
        public Builder<T> attributes(Collection<Attribute.AttributeSupplier<T>> mappedAttributes) {
            mappedAttributes.stream().map(Supplier::get).forEach(this::mergeAttribute);
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
                                             .forEach(this::mergeAttribute);
            return this;
        }

        /**
         * Extends the {@link StaticTableSchema} of a super-class, effectively rolling all the attributes modelled by
         * the super-class into the {@link StaticTableSchema} of the sub-class.
         */
        public Builder<T> extend(StaticTableSchema<? super T> superTableSchema) {
            // Upcast transform and merge attributes
            Stream<Attribute<T>> transformedAttributes =
                upcastingTransformForAttributes(superTableSchema.attributeMappers);
            transformedAttributes.forEach(this::mergeAttribute);

            return this;
        }

        /**
         * Associate one or more {@link TableTag} with this schema. See documentation on the tags themselves to
         * understand what each one does.
         */
        public Builder<T> tagWith(TableTag... tableTags) {
            Arrays.stream(tableTags).forEach(tableTag -> tableTag.setTableMetadata(tableMetadataBuilder));
            return this;
        }

        /**
         * Builds a {@link StaticTableSchema} based on the values this builder has been configured with
         */
        public StaticTableSchema<T> build() {
            return new StaticTableSchema<>(mappedAttributes,
                                           newItemSupplier,
                                           tableMetadataBuilder.build());
        }

        private void mergeAttribute(Attribute<T> attributeToMerge) {
            String attributeName = attributeToMerge.attributeName();

            if (this.indexedMappers.containsKey(attributeName)) {
                throw new IllegalArgumentException("Attempt to add an attribute to a mapper that already has one "
                                                   + "with the same name. [Attribute name: " + attributeName + "]");
            }

            this.mappedAttributes.add(attributeToMerge);
            this.indexedMappers.put(attributeName, attributeToMerge);
            this.tableMetadataBuilder.mergeWith(attributeToMerge.tableMetadata());
        }

        private static <T extends R, R> Stream<Attribute<T>> upcastingTransformForAttributes(
            Collection<Attribute<R>> superAttributes) {
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
        AtomicReference<T> item = new AtomicReference<>();

        attributeMap.forEach((key, value) -> {
            if (!isNullAttributeValue(value)) {
                Attribute<T> attributeMapper = indexedMappers.get(key);

                if (attributeMapper != null) {
                    if (item.get() == null) {
                        item.set(constructNewItem());
                    }

                    attributeMapper.updateItemMethod().accept(item.get(), value);
                }
            }
        });

        return item.get();
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
        Attribute<T> attributeMapper = indexedMappers.get(key);

        if (attributeMapper == null) {
            throw new IllegalArgumentException(String.format("TableSchema does not know how to retrieve requested "
                                                             + "attribute '%s' from mapped object.", key));
        }

        AttributeValue attributeValue = attributeMapper.attributeGetterMethod().apply(item);

        return isNullAttributeValue(attributeValue) ? null : attributeValue;
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

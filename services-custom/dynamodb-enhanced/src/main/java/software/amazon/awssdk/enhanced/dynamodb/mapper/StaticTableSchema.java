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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Implementation of {@link TableSchema} that builds a schema based on directly declared attributes and methods to
 * get and set those attributes. Just like {@link StaticImmutableTableSchema} which is the equivalent implementation for
 * immutable objects, this is the most direct, and thus fastest, implementation of {@link TableSchema}.
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
public final class StaticTableSchema<T> extends WrappedTableSchema<T, StaticImmutableTableSchema<T, T>> {
    private StaticTableSchema(Builder<T> builder) {
        super(builder.delegateBuilder.build());
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
        private final StaticImmutableTableSchema.Builder<T, T> delegateBuilder;
        private final Class<T> itemClass;

        private Builder(Class<T> itemClass) {
            this.delegateBuilder = StaticImmutableTableSchema.builder(itemClass, itemClass);
            this.itemClass = itemClass;
        }

        /**
         * A function that can be used to create new instances of the data item class.
         */
        public Builder<T> newItemSupplier(Supplier<T> newItemSupplier) {
            this.delegateBuilder.newItemBuilder(newItemSupplier, Function.identity());
            return this;
        }

        /**
         * A list of attributes that can be mapped between the data item object and the database record that are to
         * be associated with the schema. Will overwrite any existing attributes.
         */
        @SafeVarargs
        public final Builder<T> attributes(StaticAttribute<T, ?>... staticAttributes) {
            this.delegateBuilder.attributes(Arrays.stream(staticAttributes)
                                                  .map(StaticAttribute::toImmutableAttribute)
                                                  .collect(Collectors.toList()));

            return this;
        }

        /**
         * A list of attributes that can be mapped between the data item object and the database record that are to
         * be associated with the schema. Will overwrite any existing attributes.
         */
        public Builder<T> attributes(Collection<StaticAttribute<T, ?>> staticAttributes) {
            this.delegateBuilder.attributes(staticAttributes.stream()
                                                            .map(StaticAttribute::toImmutableAttribute)
                                                            .collect(Collectors.toList()));
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
            this.delegateBuilder.addAttribute(builder.build().toImmutableAttribute());
            return this;
        }

        /**
         * Adds a single attribute to the table schema that can be mapped between the data item object and the database
         * record.
         */
        public <R> Builder<T> addAttribute(Class<R> attributeClass,
                                           Consumer<StaticAttribute.Builder<T, R>> staticAttribute) {
            StaticAttribute.Builder<T, R> builder = StaticAttribute.builder(itemClass, attributeClass);
            staticAttribute.accept(builder);
            this.delegateBuilder.addAttribute(builder.build().toImmutableAttribute());
            return this;
        }

        /**
         * Adds a single attribute to the table schema that can be mapped between the data item object and the database
         * record.
         */
        public Builder<T> addAttribute(StaticAttribute<T, ?> staticAttribute) {
            this.delegateBuilder.addAttribute(staticAttribute.toImmutableAttribute());
            return this;
        }

        /**
         * Flattens all the attributes defined in another {@link StaticTableSchema} into the database record this schema
         * maps to. Functions to get and set an object that the flattened schema maps to is required.
         */
        public <R> Builder<T> flatten(TableSchema<R> otherTableSchema,
                                      Function<T, R> otherItemGetter,
                                      BiConsumer<T, R> otherItemSetter) {
            this.delegateBuilder.flatten(otherTableSchema, otherItemGetter, otherItemSetter);
            return this;
        }

        /**
         * Extends the {@link StaticTableSchema} of a super-class, effectively rolling all the attributes modelled by
         * the super-class into the {@link StaticTableSchema} of the sub-class.
         */
        public Builder<T> extend(StaticTableSchema<? super T> superTableSchema) {
            this.delegateBuilder.extend(superTableSchema.toImmutableTableSchema());
            return this;
        }

        /**
         * Associate one or more {@link StaticTableTag} with this schema. See documentation on the tags themselves to
         * understand what each one does. This method will overwrite any existing table tags.
         */
        public Builder<T> tags(StaticTableTag... staticTableTags) {
            this.delegateBuilder.tags(staticTableTags);
            return this;
        }

        /**
         * Associate one or more {@link StaticTableTag} with this schema. See documentation on the tags themselves to
         * understand what each one does. This method will overwrite any existing table tags.
         */
        public Builder<T> tags(Collection<StaticTableTag> staticTableTags) {
            this.delegateBuilder.tags(staticTableTags);
            return this;
        }

        /**
         * Associates a {@link StaticTableTag} with this schema. See documentation on the tags themselves to understand
         * what each one does. This method will add the tag to the list of existing table tags.
         */
        public Builder<T> addTag(StaticTableTag staticTableTag) {
            this.delegateBuilder.addTag(staticTableTag);
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
            this.delegateBuilder.attributeConverterProviders(attributeConverterProviders);
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
            this.delegateBuilder.attributeConverterProviders(attributeConverterProviders);
            return this;
        }


        /**
         * Builds a {@link StaticTableSchema} based on the values this builder has been configured with
         */
        public StaticTableSchema<T> build() {
            return new StaticTableSchema<>(this);
        }

    }

    private StaticImmutableTableSchema<T, T> toImmutableTableSchema() {
        return delegateTableSchema();
    }

    /**
     * The table schema {@link AttributeConverterProvider}.
     * @see Builder#attributeConverterProvider
     */
    public AttributeConverterProvider attributeConverterProvider() {
        return delegateTableSchema().attributeConverterProvider();
    }

}

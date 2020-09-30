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
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ResolvedImmutableAttribute;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticAttributeType;
import software.amazon.awssdk.utils.Validate;

/**
 * A class that represents an attribute on an mapped immutable item. A {@link StaticImmutableTableSchema} composes
 * multiple attributes that map to a common immutable item class.
 * <p>
 * The recommended way to use this class is by calling
 * {@link software.amazon.awssdk.enhanced.dynamodb.TableSchema#builder(Class, Class)}.
 * Example:
 * {@code
 * TableSchema.builder(Customer.class, Customer.Builder.class)
 *            .addAttribute(String.class,
 *                          a -> a.name("customer_name").getter(Customer::name).setter(Customer.Builder::name))
 *            // ...
 *            .build();
 * }
 * <p>
 * It's also possible to construct this class on its own using the static builder. Example:
 * {@code
 * ImmutableAttribute<Customer, Customer.Builder, ?> customerNameAttribute =
 *     ImmutableAttribute.builder(Customer.class, Customer.Builder.class, String.class)
 *                       .name("customer_name")
 *                       .getter(Customer::name)
 *                       .setter(Customer.Builder::name)
 *                       .build();
 * }
 * @param <T> the class of the immutable item this attribute maps into.
 * @param <B> the class of the builder for the immutable item this attribute maps into.
 * @param <R> the class that the value of this attribute converts to.
 */
@SdkPublicApi
public final class ImmutableAttribute<T, B, R> {
    private final String name;
    private final Function<T, R> getter;
    private final BiConsumer<B, R> setter;
    private final Collection<StaticAttributeTag> tags;
    private final EnhancedType<R> type;
    private final AttributeConverter<R> attributeConverter;

    private ImmutableAttribute(Builder<T, B, R> builder) {
        this.name = Validate.paramNotNull(builder.name, "name");
        this.getter = Validate.paramNotNull(builder.getter, "getter");
        this.setter = Validate.paramNotNull(builder.setter, "setter");
        this.tags = builder.tags == null ? Collections.emptyList() : Collections.unmodifiableCollection(builder.tags);
        this.type = Validate.paramNotNull(builder.type, "type");
        this.attributeConverter = builder.attributeConverter;
    }

    /**
     * Constructs a new builder for this class using supplied types.
     * @param itemClass The class of the immutable item that this attribute composes.
     * @param builderClass The class of the builder for the immutable item that this attribute composes.
     * @param attributeType A {@link EnhancedType} that represents the type of the value this attribute stores.
     * @return A new typed builder for an attribute.
     */
    public static <T, B, R> Builder<T, B, R> builder(Class<T> itemClass,
                                                     Class<B> builderClass,
                                                     EnhancedType<R> attributeType) {
        return new Builder<>(attributeType);
    }

    /**
     * Constructs a new builder for this class using supplied types.
     * @param itemClass The class of the item that this attribute composes.
     * @param builderClass The class of the builder for the immutable item that this attribute composes.
     * @param attributeClass A class that represents the type of the value this attribute stores.
     * @return A new typed builder for an attribute.
     */
    public static <T, B, R> Builder<T, B, R> builder(Class<T> itemClass,
                                                     Class<B> builderClass,
                                                     Class<R> attributeClass) {
        return new Builder<>(EnhancedType.of(attributeClass));
    }

    /**
     * The name of this attribute
     */
    public String name() {
        return this.name;
    }

    /**
     * A function that can get the value of this attribute from a modelled immutable item it composes.
     */
    public Function<T, R> getter() {
        return this.getter;
    }

    /**
     * A function that can set the value of this attribute on a builder for the immutable modelled item it composes.
     */
    public BiConsumer<B, R> setter() {
        return this.setter;
    }

    /**
     * A collection of {@link StaticAttributeTag} associated with this attribute.
     */
    public Collection<StaticAttributeTag> tags() {
        return this.tags;
    }

    /**
     * A {@link EnhancedType} that represents the type of the value this attribute stores.
     */
    public EnhancedType<R> type() {
        return this.type;
    }

    /**
     * A custom {@link AttributeConverter} that will be used to convert this attribute.
     * If no custom converter was provided, the value will be null.
     * @see Builder#attributeConverter
     */
    public AttributeConverter<R> attributeConverter() {
        return this.attributeConverter;
    }

    /**
     * Converts an instance of this class to a {@link Builder} that can be used to modify and reconstruct it.
     */
    public Builder<T, B, R> toBuilder() {
        return new Builder<T, B, R>(this.type).name(this.name)
                                              .getter(this.getter)
                                              .setter(this.setter)
                                              .tags(this.tags)
                                              .attributeConverter(this.attributeConverter);
    }


    ResolvedImmutableAttribute<T, B> resolve(AttributeConverterProvider attributeConverterProvider) {
        return ResolvedImmutableAttribute.create(this,
                                                 StaticAttributeType.create(converterFrom(attributeConverterProvider)));
    }

    private AttributeConverter<R> converterFrom(AttributeConverterProvider attributeConverterProvider) {
        return (attributeConverter != null) ? attributeConverter : attributeConverterProvider.converterFor(type);
    }

    /**
     * A typed builder for {@link ImmutableAttribute}.
     * @param <T> the class of the item this attribute maps into.
     * @param <R> the class that the value of this attribute converts to.
     */
    public static final class Builder<T, B, R> {
        private final EnhancedType<R> type;
        private String name;
        private Function<T, R> getter;
        private BiConsumer<B, R> setter;
        private List<StaticAttributeTag> tags;
        private AttributeConverter<R> attributeConverter;

        private Builder(EnhancedType<R> type) {
            this.type = type;
        }

        /**
         * The name of this attribute
         */
        public Builder<T, B, R> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * A function that can get the value of this attribute from a modelled item it composes.
         */
        public Builder<T, B, R> getter(Function<T, R> getter) {
            this.getter = getter;
            return this;
        }

        /**
         * A function that can set the value of this attribute on a modelled item it composes.
         */
        public Builder<T, B, R> setter(BiConsumer<B, R> setter) {
            this.setter = setter;
            return this;
        }

        /**
         * A collection of {@link StaticAttributeTag} associated with this attribute. Overwrites any existing tags.
         */
        public Builder<T, B, R> tags(Collection<StaticAttributeTag> tags) {
            this.tags = new ArrayList<>(tags);
            return this;
        }

        /**
         * A collection of {@link StaticAttributeTag} associated with this attribute. Overwrites any existing tags.
         */
        public Builder<T, B, R> tags(StaticAttributeTag... tags) {
            this.tags = Arrays.asList(tags);
            return this;
        }

        /**
         * Associates a single {@link StaticAttributeTag} with this attribute. Adds to any existing tags.
         */
        public Builder<T, B, R> addTag(StaticAttributeTag tag) {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }

            this.tags.add(tag);
            return this;
        }

        /**
         * An {@link AttributeConverter} for the attribute type ({@link EnhancedType}), that can convert this attribute.
         * It takes precedence over any converter for this type provided by the table schema
         * {@link AttributeConverterProvider}.
         */
        public Builder<T, B, R> attributeConverter(AttributeConverter<R> attributeConverter) {
            this.attributeConverter = attributeConverter;
            return this;
        }

        /**
         * Builds a {@link StaticAttributeTag} from the values stored in this builder.
         */
        public ImmutableAttribute<T, B, R> build() {
            return new ImmutableAttribute<>(this);
        }
    }
}

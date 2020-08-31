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

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

/**
 * A class that represents an attribute that can be read from and written to an mapped item. A {@link StaticTableSchema}
 * composes multiple attributes that map to a common item class.
 * <p>
 * The recommended way to use this class is by calling {@link StaticTableSchema.Builder#addAttribute(Class, Consumer)}.
 * Example:
 * <pre>{@code
 * StaticTableSchema.builder()
 *                  .addAttribute(String.class,
 *                                a -> a.name("customer_name").getter(Customer::getName).setter(Customer::setName))
 *                  // ...
 *                  .build();
 * }</pre>
 * <p>
 * It's also possible to construct this class on its own using the static builder. Example:
 * <pre>{@code
 * StaticAttribute<Customer, ?> customerNameAttribute =
 *     StaticAttribute.builder(Customer.class, String.class)
 *                    .name("customer_name")
 *                    .getter(Customer::getName)
 *                    .setter(Customer::setName)
 *                    .build();
 * }
 * </pre>
 * @param <T> the class of the item this attribute maps into.
 * @param <R> the class that the value of this attribute converts to.
 */
@SdkPublicApi
public final class StaticAttribute<T, R> {
    private final ImmutableAttribute<T, T, R> delegateAttribute;

    private StaticAttribute(Builder<T, R> builder) {
        this.delegateAttribute = builder.delegateBuilder.build();
    }

    /**
     * Constructs a new builder for this class using supplied types.
     * @param itemClass The class of the item that this attribute composes.
     * @param attributeType A {@link EnhancedType} that represents the type of the value this attribute stores.
     * @return A new typed builder for an attribute.
     */
    public static <T, R> Builder<T, R> builder(Class<T> itemClass, EnhancedType<R> attributeType) {
        return new Builder<>(itemClass, attributeType);
    }

    /**
     * Constructs a new builder for this class using supplied types.
     * @param itemClass The class of the item that this attribute composes.
     * @param attributeClass A class that represents the type of the value this attribute stores.
     * @return A new typed builder for an attribute.
     */
    public static <T, R> Builder<T, R> builder(Class<T> itemClass, Class<R> attributeClass) {
        return new Builder<>(itemClass, EnhancedType.of(attributeClass));
    }

    /**
     * The name of this attribute
     */
    public String name() {
        return this.delegateAttribute.name();
    }

    /**
     * A function that can get the value of this attribute from a modelled item it composes.
     */
    public Function<T, R> getter() {
        return this.delegateAttribute.getter();
    }

    /**
     * A function that can set the value of this attribute on a modelled item it composes.
     */
    public BiConsumer<T, R> setter() {
        return this.delegateAttribute.setter();
    }

    /**
     * A collection of {@link StaticAttributeTag} associated with this attribute.
     */
    public Collection<StaticAttributeTag> tags() {
        return this.delegateAttribute.tags();
    }

    /**
     * A {@link EnhancedType} that represents the type of the value this attribute stores.
     */
    public EnhancedType<R> type() {
        return this.delegateAttribute.type();
    }

    /**
     * A custom {@link AttributeConverter} that will be used to convert this attribute.
     * If no custom converter was provided, the value will be null.
     * @see Builder#attributeConverter
     */
    public AttributeConverter<R> attributeConverter() {
        return this.delegateAttribute.attributeConverter();
    }

    /**
     * Converts an instance of this class to a {@link Builder} that can be used to modify and reconstruct it.
     */
    public Builder<T, R> toBuilder() {
        return new Builder<>(this.delegateAttribute.toBuilder());
    }

    ImmutableAttribute<T, T, R> toImmutableAttribute() {
        return this.delegateAttribute;
    }

    /**
     * A typed builder for {@link StaticAttribute}.
     * @param <T> the class of the item this attribute maps into.
     * @param <R> the class that the value of this attribute converts to.
     */
    public static final class Builder<T, R> {
        private final ImmutableAttribute.Builder<T, T, R> delegateBuilder;

        private Builder(Class<T> itemClass, EnhancedType<R> type) {
            this.delegateBuilder = ImmutableAttribute.builder(itemClass, itemClass, type);
        }

        private Builder(ImmutableAttribute.Builder<T, T, R> delegateBuilder) {
            this.delegateBuilder = delegateBuilder;
        }

        /**
         * The name of this attribute
         */
        public Builder<T, R> name(String name) {
            this.delegateBuilder.name(name);
            return this;
        }

        /**
         * A function that can get the value of this attribute from a modelled item it composes.
         */
        public Builder<T, R> getter(Function<T, R> getter) {
            this.delegateBuilder.getter(getter);
            return this;
        }

        /**
         * A function that can set the value of this attribute on a modelled item it composes.
         */
        public Builder<T, R> setter(BiConsumer<T, R> setter) {
            this.delegateBuilder.setter(setter);
            return this;
        }

        /**
         * A collection of {@link StaticAttributeTag} associated with this attribute. Overwrites any existing tags.
         */
        public Builder<T, R> tags(Collection<StaticAttributeTag> tags) {
            this.delegateBuilder.tags(tags);
            return this;
        }

        /**
         * A collection of {@link StaticAttributeTag} associated with this attribute. Overwrites any existing tags.
         */
        public Builder<T, R> tags(StaticAttributeTag... tags) {
            this.delegateBuilder.tags(tags);
            return this;
        }

        /**
         * Associates a single {@link StaticAttributeTag} with this attribute. Adds to any existing tags.
         */
        public Builder<T, R> addTag(StaticAttributeTag tag) {
            this.delegateBuilder.addTag(tag);
            return this;
        }

        /**
         * An {@link AttributeConverter} for the attribute type ({@link EnhancedType}), that can convert this attribute.
         * It takes precedence over any converter for this type provided by the table schema
         * {@link AttributeConverterProvider}.
         */
        public Builder<T, R> attributeConverter(AttributeConverter<R> attributeConverter) {
            this.delegateBuilder.attributeConverter(attributeConverter);
            return this;
        }

        /**
         * Builds a {@link StaticAttributeTag} from the values stored in this builder.
         */
        public StaticAttribute<T, R> build() {
            return new StaticAttribute<>(this);
        }
    }
}

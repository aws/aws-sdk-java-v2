/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.UnionAttributeConverter.Visitor;
import software.amazon.awssdk.enhanced.dynamodb.model.AttributeConverterAware;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A chain of converters, invoking the underlying converters based on the precedence defined in the
 * {@link AttributeConverterAware} documentation.
 *
 * <p>
 * Given an input, this will identify a converter that can convert the specific Java type and invoke it. If a converter cannot
 * be found, it will invoke a "parent" converter, which would be expected to be able to convert the value (or throw an exception).
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class ChainAttributeConverter<T> implements SubtypeAttributeConverter<T> {
    private static final Logger log = Logger.loggerFor(ChainAttributeConverter.class);

    private final TypeToken<T> type;

    private final List<SubtypeAttributeConverter<? extends T>> subtypeConverters = new ArrayList<>();

    private final ConcurrentHashMap<Class<? extends T>, UnionAttributeConverter<? extends T>> converterCache =
            new ConcurrentHashMap<>();

    /**
     * The "default converter" to invoke if no converters can be found in this chain supporting a specific type.
     */
    private final SubtypeAttributeConverter<? super T> parent;

    private ChainAttributeConverter(Builder<T> builder) {
        this.type = builder.type;
        this.parent = builder.parent;

        // Subtype converters are used in the REVERSE order of how they were added to the builder.
        for (int i = builder.subtypeConverters.size() - 1; i >= 0; i--) {
            this.subtypeConverters.add(builder.subtypeConverters.get(i));
        }

        // Converters are used in the REVERSE order of how they were added to the builder.
        for (int i = builder.converters.size() - 1; i >= 0; i--) {
            AttributeConverter<? extends T> converter = builder.converters.get(i);
            converterCache.put(converter.type().rawClass(), UnionAttributeConverter.create(converter));
        }
    }

    /**
     * Equivalent to {@code builder(TypeToken.of(Object.class))}.
     */
    public static Builder<Object> builder() {
        return new Builder<>(TypeToken.of(Object.class));
    }

    /**
     * Create a builder for a {@link ChainAttributeConverter}, using the provided type as the upper bound for the types supported
     * by this chain.
     */
    public static <T> Builder<T> builder(TypeToken<T> typeBound) {
        return new Builder<>(typeBound);
    }

    /**
     * A simplified way of invoking {@code builder().addConverters(converters.converters())
     * .addSubtypeConverters(converters.subtypeConverters()).build()}.
     */
    public static ChainAttributeConverter<Object> create(AttributeConverterAware<?> converters) {
        return builder().addConverters(converters.converters())
                        .addSubtypeConverters(converters.subtypeConverters())
                        .build();
    }

    @Override
    public TypeToken<T> type() {
        return type;
    }

    @Override
    public ItemAttributeValue toAttributeValue(T input, ConversionContext context) {
        if (input == null) {
            return ItemAttributeValue.nullValue();
        }

        return findRequiredConverter((Class<T>) input.getClass()).visit(new Visitor<T, ItemAttributeValue>() {
            @Override
            public ItemAttributeValue visit(AttributeConverter<T> converter) {
                return converter.toAttributeValue(input, context);
            }

            @Override
            public ItemAttributeValue visit(SubtypeAttributeConverter<? super T> converter) {
                return converter.toAttributeValue(input, context);
            }
        });
    }

    @Override
    public <U extends T> U fromAttributeValue(ItemAttributeValue input, TypeToken<U> desiredType, ConversionContext context) {
        return findRequiredConverter(desiredType.rawClass()).visit(new Visitor<U, U>() {
            @Override
            public U visit(AttributeConverter<U> converter) {
                return converter.fromAttributeValue(input, context);
            }

            @Override
            public U visit(SubtypeAttributeConverter<? super U> converter) {
                return converter.fromAttributeValue(input, desiredType, context);
            }
        });
    }

    /**
     * Find a converter that matches the provided type. If one cannot be found, throw an exception.
     */
    private <U extends T> UnionAttributeConverter<U> findRequiredConverter(Class<U> type) {
        return findConverter(type).orElseThrow(() -> new IllegalStateException("Converter not found for " + type));
    }

    /**
     * Find a converter that matches the provided type. If one cannot be found, return empty.
     */
    private <U extends T> Optional<UnionAttributeConverter<U>> findConverter(Class<U> type) {
        log.debug(() -> "Loading converter for " + type + ".");

        @SuppressWarnings("unchecked") // We initialized correctly, so this is safe.
                UnionAttributeConverter<U> converter = (UnionAttributeConverter<U>) converterCache.get(type);
        if (converter != null) {
            return Optional.of(converter);
        }

        log.debug(() -> "Converter not cached for " + type + ". Checking for a subtype converter match.");

        converter = findSubtypeConverter(type).orElse(null);

        if (converter == null && parent != null) {
            log.debug(() -> "Converter not found in this chain for " + type + ". Parent will be used.");
            converter = UnionAttributeConverter.create(parent);
        }

        if (converter != null && shouldCache(type)) {
            this.converterCache.put(type, converter);
        }

        return Optional.ofNullable(converter);
    }

    private boolean shouldCache(Class<?> type) {
        // Do not cache anonymous classes, to prevent memory leaks.
        return !type.isAnonymousClass();
    }

    private <U extends T> Optional<UnionAttributeConverter<U>> findSubtypeConverter(Class<U> type) {
        for (SubtypeAttributeConverter<? extends T> subtypeConverter : subtypeConverters) {
            if (subtypeConverter.type().rawClass().isAssignableFrom(type)) {
                SubtypeAttributeConverter<U> result = (SubtypeAttributeConverter<U>) subtypeConverter;
                return Optional.of(UnionAttributeConverter.create(result));
            }
        }

        return Optional.empty();
    }

    /**
     * A builder for configuring and creating {@link ChainAttributeConverter}s.
     */
    public static class Builder<T> implements AttributeConverterAware.Builder<T> {
        private final TypeToken<T> type;
        private List<AttributeConverter<? extends T>> converters = new ArrayList<>();
        private List<SubtypeAttributeConverter<? extends T>> subtypeConverters = new ArrayList<>();
        private SubtypeAttributeConverter<? super T> parent;

        private Builder(TypeToken<T> type) {
            this.type = type;
        }

        @Override
        public Builder<T> addConverters(Collection<? extends AttributeConverter<? extends T>> converters) {
            Validate.paramNotNull(converters, "converters");
            Validate.noNullElements(converters, "Converters must not contain null members.");
            this.converters.addAll(converters);
            return this;
        }

        @Override
        public Builder<T> addConverter(AttributeConverter<? extends T> converter) {
            Validate.paramNotNull(converter, "converter");
            this.converters.add(converter);
            return this;
        }

        @Override
        public Builder<T> addSubtypeConverters(Collection<? extends SubtypeAttributeConverter<? extends T>> converters) {
            Validate.paramNotNull(converters, "converters");
            Validate.noNullElements(converters, "Converters must not contain null members.");
            this.subtypeConverters.addAll(converters);
            return this;
        }

        @Override
        public Builder<T> addSubtypeConverter(SubtypeAttributeConverter<? extends T> converter) {
            Validate.paramNotNull(converter, "converter");
            this.subtypeConverters.add(converter);
            return this;
        }

        @Override
        public Builder<T> clearConverters() {
            this.converters.clear();
            return this;
        }

        @Override
        public Builder<T> clearSubtypeConverters() {
            this.subtypeConverters.clear();
            return this;
        }

        /**
         * Specify the parent converter to which this chain should delegate its conversion when no converters defined in this
         * chain can handle the requested type. This must not be null.
         */
        public Builder<T> parent(SubtypeAttributeConverter<? super T> parent) {
            Validate.paramNotNull(parent, "parent");
            this.parent = parent;
            return this;
        }

        public ChainAttributeConverter<T> build() {
            if (converters.isEmpty() &&
                subtypeConverters.size() == 1 &&
                subtypeConverters.get(0) instanceof ChainAttributeConverter &&
                subtypeConverters.get(0).type().equals(type)) {
                return (ChainAttributeConverter<T>) subtypeConverters.get(0);
            }
            return new ChainAttributeConverter<>(this);
        }
    }
}

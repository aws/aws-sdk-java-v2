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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionCondition;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ConverterAware;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A chain of converters, invoking the underlying converters based on the precedence defined in the
 * {@link ItemAttributeValueConverter} documentation.
 *
 * <p>
 * Given an input, this will identify a converter that can convert the specific Java type and invoke it. If a converter cannot
 * be found, it will invoke a "parent" converter, which would be expected to be able to convert the value (or throw an exception).
 */
@SdkInternalApi
@ThreadSafe
public final class ItemAttributeValueConverterChain implements ItemAttributeValueConverter {
    private static final Logger log = Logger.loggerFor(ItemAttributeValueConverterChain.class);

    /**
     * All converters in this chain that match the {@link ConversionCondition#isInstanceOf(Class)}
     */
    private final List<ItemAttributeValueConverter> instanceOfConverters = new ArrayList<>();

    /**
     * A cache from Java type to the converter for that type. This is pre-populated with all
     * {@link ConversionCondition#isExactInstanceOf(Class)} converters in this chain.
     */
    private final ConcurrentHashMap<Class<?>, ItemAttributeValueConverter> converterCache = new ConcurrentHashMap<>();

    /**
     * The "default converter" to invoke if no converters can be found in this chain supporting a specific type.
     */
    private final ItemAttributeValueConverter parent;

    private ItemAttributeValueConverterChain(Builder builder) {
        for (ItemAttributeValueConverter converter : builder.converters) {
            ConversionCondition condition = converter.defaultConversionCondition();

            if (condition instanceof InstanceOfConversionCondition) {
                this.instanceOfConverters.add(converter);
            }

            if (condition instanceof ExactInstanceOfConversionCondition) {
                // Pre-cache all exact-condition converters
                ExactInstanceOfConversionCondition exactCondition = (ExactInstanceOfConversionCondition) condition;
                this.converterCache.putIfAbsent(exactCondition.convertedClass(), converter);
            }
        }

        this.parent = builder.parent;
    }

    /**
     * Create a builder that can be used to configure and create a {@link ItemAttributeValueConverterChain}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A simplified way of invoking {@code builder().addAll(converters).build()}.
     */
    public static ItemAttributeValueConverterChain create(Collection<? extends ItemAttributeValueConverter> converters) {
        return builder().addConverters(converters).build();
    }

    @Override
    public ConversionCondition defaultConversionCondition() {
        return ConversionCondition.isInstanceOf(Object.class);
    }

    @Override
    public ItemAttributeValue toAttributeValue(Object input, ConversionContext context) {
        return invokeConverter(input.getClass(), c -> c.toAttributeValue(input, context));
    }

    @Override
    public Object fromAttributeValue(ItemAttributeValue input, TypeToken<?> desiredType, ConversionContext context) {
        return invokeConverter(desiredType.rawClass(), c -> c.fromAttributeValue(input, desiredType, context));
    }

    /**
     * Find a converter that matches the provided type. Once the converter is found, invoke the provided invoker to generate
     * a result type.
     */
    private <T> T invokeConverter(Class<?> type, Function<ItemAttributeValueConverter, T> converterInvoker) {
        log.debug(() -> "Loading converter for " + type.getTypeName() + ".");

        ItemAttributeValueConverter converter = converterCache.get(type);
        if (converter != null) {
            return converterInvoker.apply(converter);
        }

        log.debug(() -> "Converter not cached for " + type.getTypeName() + ". " +
                        "Checking for an instanceof converter match.");

        converter = findInstanceOfConverter(type);

        if (converter == null && parent != null) {
            log.debug(() -> "Converter not found in this chain for " + type.getTypeName() + ". Parent will be used.");
            converter = parent;
        }

        if (converter == null) {
            throw new IllegalStateException("Converter not found for " + type.getTypeName() + ".");
        }


        T result = converterInvoker.apply(converter);

        if (shouldCache(type)) {
            // Only cache after successful conversion, to prevent leaking memory.
            this.converterCache.put(type, converter);
        }

        return result;
    }

    private boolean shouldCache(Class<?> type) {
        // Do not cache anonymous classes, to prevent memory leaks.
        return !type.isAnonymousClass();
    }

    private ItemAttributeValueConverter findInstanceOfConverter(Class<?> type) {
        for (ItemAttributeValueConverter converter : instanceOfConverters) {
            InstanceOfConversionCondition condition = (InstanceOfConversionCondition)
                    converter.defaultConversionCondition();

            if (condition.converts(type)) {
                return converter;
            }
        }

        return null;
    }

    public static class Builder implements ConverterAware.Builder {
        private List<ItemAttributeValueConverter> converters = new ArrayList<>();
        private ItemAttributeValueConverter parent;

        private Builder() {}

        @Override
        public Builder addConverters(Collection<? extends ItemAttributeValueConverter> converters) {
            Validate.paramNotNull(converters, "converters");
            Validate.noNullElements(converters, "Converters must not contain null members.");
            this.converters.addAll(converters);
            return this;
        }

        @Override
        public Builder addConverter(ItemAttributeValueConverter converter) {
            Validate.paramNotNull(converter, "converter");
            this.converters.add(converter);
            return this;
        }

        @Override
        public Builder clearConverters() {
            this.converters.clear();
            return this;
        }

        public Builder parent(ItemAttributeValueConverter parent) {
            Validate.paramNotNull(parent, "parent");
            this.parent = parent;
            return this;
        }

        public ItemAttributeValueConverterChain build() {
            if (converters.size() == 1 && converters.get(0) instanceof ItemAttributeValueConverterChain && parent == null) {
                // Optimization: Don't wrap chains in chains
                return (ItemAttributeValueConverterChain) converters.get(0);
            }

            return new ItemAttributeValueConverterChain(this);
        }
    }
}

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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.AtomicBooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.AtomicIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.AtomicLongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BigDecimalAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BigIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharSequenceAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharacterArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharacterAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DocumentAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DoubleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DurationAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnumAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.FloatAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.IntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalDateAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalDateTimeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalTimeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MonthDayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OffsetDateTimeAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OptionalDoubleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OptionalIntAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OptionalLongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.PeriodAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SdkBytesAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ShortAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringBufferAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringBuilderAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UriAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UrlAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UuidAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZoneIdAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZoneOffsetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZonedDateTimeAsStringAttributeConverter;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * This class is the default attribute converter provider in the DDB Enhanced library. When instantiated
 * using the constructor {@link #DefaultAttributeConverterProvider()} or the {@link #create()} method, it's loaded
 * with the currently supported attribute converters in the library.
 * <p>
 * Given an input, the method {@link #converterFor(EnhancedType)} will identify a converter that can convert the
 * specific Java type and invoke it. If a converter cannot be found, it will invoke a "parent" converter,
 * which would be expected to be able to convert the value (or throw an exception).
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class DefaultAttributeConverterProvider implements AttributeConverterProvider {
    private static final Logger log = Logger.loggerFor(DefaultAttributeConverterProvider.class);

    private final ConcurrentHashMap<EnhancedType<?>, AttributeConverter<?>> converterCache =
        new ConcurrentHashMap<>();

    private DefaultAttributeConverterProvider(Builder builder) {
        // Converters are used in the REVERSE order of how they were added to the builder.
        for (int i = builder.converters.size() - 1; i >= 0; i--) {
            AttributeConverter<?> converter = builder.converters.get(i);
            converterCache.put(converter.type(), converter);

            if (converter instanceof PrimitiveConverter) {
                PrimitiveConverter<?> primitiveConverter = (PrimitiveConverter<?>) converter;
                converterCache.put(primitiveConverter.primitiveType(), converter);
            }
        }
    }

    /**
     * Returns an attribute converter provider with all default converters set.
     */
    public DefaultAttributeConverterProvider() {
        this(getDefaultBuilder());
    }

    /**
     * Returns an attribute converter provider with all default converters set.
     */
    public static DefaultAttributeConverterProvider create() {
        return getDefaultBuilder().build();
    }


    /**
     * Equivalent to {@code builder(EnhancedType.of(Object.class))}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Find a converter that matches the provided type. If one cannot be found, throw an exception.
     */
    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> type) {
        return findConverter(type).orElseThrow(() -> new IllegalStateException("Converter not found for " + type));
    }

    /**
     * Find a converter that matches the provided type. If one cannot be found, return empty.
     */
    @SuppressWarnings("unchecked")
    private <T> Optional<AttributeConverter<T>> findConverter(EnhancedType<T> type) {
        log.debug(() -> "Loading converter for " + type + ".");

        AttributeConverter<T> converter = (AttributeConverter<T>) converterCache.get(type);
        if (converter != null) {
            return Optional.of(converter);
        }

        if (type.rawClass().isAssignableFrom(Map.class)) {
            converter = createMapConverter(type);
        } else if (type.rawClass().isAssignableFrom(Set.class)) {
            converter = createSetConverter(type);
        } else if (type.rawClass().isAssignableFrom(List.class)) {
            EnhancedType<T> innerType = (EnhancedType<T>) type.rawClassParameters().get(0);
            AttributeConverter<?> innerConverter = findConverter(innerType)
                .orElseThrow(() -> new IllegalStateException("Converter not found for " + type));
            return Optional.of((AttributeConverter<T>) ListAttributeConverter.create(innerConverter));
        } else if (type.rawClass().isEnum()) {
            return Optional.of(EnumAttributeConverter.create(((EnhancedType<? extends Enum>) type).rawClass()));
        }

        if (type.tableSchema().isPresent()) {
            converter = DocumentAttributeConverter.create(type.tableSchema().get(), type);
        }

        if (converter != null && shouldCache(type.rawClass())) {
            this.converterCache.put(type, converter);
        }

        return Optional.ofNullable(converter);
    }

    private boolean shouldCache(Class<?> type) {
        // Do not cache anonymous classes, to prevent memory leaks.
        return !type.isAnonymousClass();
    }

    @SuppressWarnings("unchecked")
    private <T> AttributeConverter<T> createMapConverter(EnhancedType<T> type) {
        EnhancedType<?> keyType = type.rawClassParameters().get(0);
        EnhancedType<T> valueType = (EnhancedType<T>) type.rawClassParameters().get(1);

        StringConverter<?> keyConverter = StringConverterProvider.defaultProvider().converterFor(keyType);
        AttributeConverter<?> valueConverter = findConverter(valueType)
            .orElseThrow(() -> new IllegalStateException("Converter not found for " + type));

        return (AttributeConverter<T>) MapAttributeConverter.mapConverter(keyConverter, valueConverter);
    }

    @SuppressWarnings("unchecked")
    private <T> AttributeConverter<T> createSetConverter(EnhancedType<T> type) {
        EnhancedType<T> innerType = (EnhancedType<T>) type.rawClassParameters().get(0);
        AttributeConverter<?> innerConverter = findConverter(innerType)
            .orElseThrow(() -> new IllegalStateException("Converter not found for " + type));

        return (AttributeConverter<T>) SetAttributeConverter.setConverter(innerConverter);
    }

    private static Builder getDefaultBuilder() {
        return DefaultAttributeConverterProvider.builder()
                                                .addConverter(AtomicBooleanAttributeConverter.create())
                                                .addConverter(AtomicIntegerAttributeConverter.create())
                                                .addConverter(AtomicLongAttributeConverter.create())
                                                .addConverter(BigDecimalAttributeConverter.create())
                                                .addConverter(BigIntegerAttributeConverter.create())
                                                .addConverter(BooleanAttributeConverter.create())
                                                .addConverter(ByteArrayAttributeConverter.create())
                                                .addConverter(ByteAttributeConverter.create())
                                                .addConverter(CharacterArrayAttributeConverter.create())
                                                .addConverter(CharacterAttributeConverter.create())
                                                .addConverter(CharSequenceAttributeConverter.create())
                                                .addConverter(DoubleAttributeConverter.create())
                                                .addConverter(DurationAttributeConverter.create())
                                                .addConverter(FloatAttributeConverter.create())
                                                .addConverter(InstantAsStringAttributeConverter.create())
                                                .addConverter(IntegerAttributeConverter.create())
                                                .addConverter(LocalDateAttributeConverter.create())
                                                .addConverter(LocalDateTimeAttributeConverter.create())
                                                .addConverter(LocalTimeAttributeConverter.create())
                                                .addConverter(LongAttributeConverter.create())
                                                .addConverter(MonthDayAttributeConverter.create())
                                                .addConverter(OffsetDateTimeAsStringAttributeConverter.create())
                                                .addConverter(OptionalDoubleAttributeConverter.create())
                                                .addConverter(OptionalIntAttributeConverter.create())
                                                .addConverter(OptionalLongAttributeConverter.create())
                                                .addConverter(PeriodAttributeConverter.create())
                                                .addConverter(SdkBytesAttributeConverter.create())
                                                .addConverter(ShortAttributeConverter.create())
                                                .addConverter(StringAttributeConverter.create())
                                                .addConverter(StringBufferAttributeConverter.create())
                                                .addConverter(StringBuilderAttributeConverter.create())
                                                .addConverter(UriAttributeConverter.create())
                                                .addConverter(UrlAttributeConverter.create())
                                                .addConverter(UuidAttributeConverter.create())
                                                .addConverter(ZonedDateTimeAsStringAttributeConverter.create())
                                                .addConverter(ZoneIdAttributeConverter.create())
                                                .addConverter(ZoneOffsetAttributeConverter.create());
    }

    /**
     * A builder for configuring and creating {@link DefaultAttributeConverterProvider}s.
     */
    public static class Builder {
        private List<AttributeConverter<?>> converters = new ArrayList<>();

        private Builder() {
        }

        public Builder addConverter(AttributeConverter<?> converter) {
            Validate.paramNotNull(converter, "converter");
            this.converters.add(converter);
            return this;
        }

        public DefaultAttributeConverterProvider build() {
            return new DefaultAttributeConverterProvider(this);
        }
    }
}

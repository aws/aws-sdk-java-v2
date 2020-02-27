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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.AtomicBooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.AtomicIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.AtomicLongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.BigDecimalAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.BigIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.BooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ByteArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ByteAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.CharSequenceAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.CharacterArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.CharacterAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.DoubleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.DurationAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.FloatAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.InstantAsIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.IntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.LocalDateAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.LocalDateTimeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.LocalTimeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.MonthDayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.OffsetDateTimeAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.OptionalDoubleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.OptionalIntAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.OptionalLongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.PeriodAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.SdkBytesAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ShortAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.StringBufferAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.StringBuilderAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.UriAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.UrlAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.UuidAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ZoneIdAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ZoneOffsetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ZonedDateTimeAsStringAttributeConverter;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 *
 * <p>
 * Given an input, this will identify a converter that can convert the specific Java type and invoke it. If a converter cannot
 * be found, it will invoke a "parent" converter, which would be expected to be able to convert the value (or throw an exception).
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class DefaultAttributeConverterProvider<T> implements AttributeConverterProvider {
    private static final Logger log = Logger.loggerFor(DefaultAttributeConverterProvider.class);

    private final ConcurrentHashMap<TypeToken<?>, AttributeConverter<? extends T>> converterCache =
            new ConcurrentHashMap<>();

    private DefaultAttributeConverterProvider(Builder<T> builder) {
        // Converters are used in the REVERSE order of how they were added to the builder.
        for (int i = builder.converters.size() - 1; i >= 0; i--) {
            AttributeConverter<? extends T> converter = builder.converters.get(i);
            converterCache.put(converter.type(), converter);

            if (converter instanceof PrimitiveConverter) {
                PrimitiveConverter primitiveConverter = (PrimitiveConverter) converter;
                converterCache.put(primitiveConverter.primitiveType(), converter);
            }
        }
    }

    /**
     * Equivalent to {@code builder(TypeToken.of(Object.class))}.
     */
    public static Builder<Object> builder() {
        return new Builder<>(TypeToken.of(Object.class));
    }

    /**
     * Create a builder for a {@link DefaultAttributeConverterProvider}, using the provided type as the upper bound for the types supported
     * by this chain.
     */
    public static <T> Builder<T> builder(TypeToken<T> typeBound) {
        return new Builder<>(typeBound);
    }

    /**
     * Find a converter that matches the provided type. If one cannot be found, throw an exception.
     */
    @Override
    public <T> AttributeConverter<T> converterFor(TypeToken<T> type) {
        return findConverter(type).orElseThrow(() -> new IllegalStateException("Converter not found for " + type));
    }

    /**
     * Find a converter that matches the provided type. If one cannot be found, return empty.
     */
    private <T, U> Optional<AttributeConverter<T>> findConverter(TypeToken<T> type) {
        log.debug(() -> "Loading converter for " + type + ".");

        @SuppressWarnings("unchecked") // We initialized correctly, so this is safe.
                AttributeConverter<T> converter = (AttributeConverter<T>) converterCache.get(type);
        if (converter != null) {
            return Optional.of(converter);
        }

        return Optional.empty();
    }

    public static DefaultAttributeConverterProvider create() {
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
                                                .addConverter(InstantAsIntegerAttributeConverter.create())
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
                                                .addConverter(ZoneOffsetAttributeConverter.create())
                                                .build();
    }

    /**
     * A builder for configuring and creating {@link DefaultAttributeConverterProvider}s.
     */
    public static class Builder<T> {
        private final TypeToken<T> type;
        private List<AttributeConverter<? extends T>> converters = new ArrayList<>();

        private Builder(TypeToken<T> type) {
            this.type = type;
        }

        public Builder<T> addConverters(Collection<? extends AttributeConverter<? extends T>> converters) {
            Validate.paramNotNull(converters, "converters");
            Validate.noNullElements(converters, "Converters must not contain null members.");
            this.converters.addAll(converters);
            return this;
        }

        public Builder<T> addConverter(AttributeConverter<? extends T> converter) {
            Validate.paramNotNull(converter, "converter");
            this.converters.add(converter);
            return this;
        }

        public Builder<T> clearConverters() {
            this.converters.clear();
            return this;
        }

        public DefaultAttributeConverterProvider<T> build() {
            return new DefaultAttributeConverterProvider<>(this);
        }
    }
}

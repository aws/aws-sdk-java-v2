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

package software.amazon.awssdk.enhanced.dynamodb.converter.string;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.AtomicBooleanStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.AtomicIntegerStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.AtomicLongStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.BigDecimalStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.BigIntegerStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.BooleanStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.ByteArrayStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.ByteStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.CharSequenceStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.CharacterArrayStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.CharacterStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.DoubleStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.DurationStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.FloatStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.InstantStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.IntegerStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.LocalDateStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.LocalDateTimeStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.LocalTimeStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.LongStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.MonthDayStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.OffsetDateTimeStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.OffsetTimeStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.OptionalDoubleStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.OptionalIntStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.OptionalLongStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.PeriodStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.SdkBytesStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.ShortStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.StringBufferStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.StringBuilderStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.StringStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.UriStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.UrlStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.UuidStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.YearMonthStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.YearStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.ZoneIdStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.ZoneOffsetStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.ZonedDateTimeStringConverter;
import software.amazon.awssdk.utils.Validate;

/**
 * <p>
 * Included converters:
 * <ul>
 *     <li>{@link AtomicIntegerStringConverter}</li>
 *     <li>{@link AtomicLongStringConverter}</li>
 *     <li>{@link BigDecimalStringConverter}</li>
 *     <li>{@link BigIntegerStringConverter}</li>
 *     <li>{@link DoubleStringConverter}</li>
 *     <li>{@link DurationStringConverter}</li>
 *     <li>{@link FloatStringConverter}</li>
 *     <li>{@link InstantStringConverter}</li>
 *     <li>{@link IntegerStringConverter}</li>
 *     <li>{@link LocalDateStringConverter}</li>
 *     <li>{@link LocalDateTimeStringConverter}</li>
 *     <li>{@link LocalTimeStringConverter}</li>
 *     <li>{@link LongStringConverter}</li>
 *     <li>{@link MonthDayStringConverter}</li>
 *     <li>{@link OptionalDoubleStringConverter}</li>
 *     <li>{@link OptionalIntStringConverter}</li>
 *     <li>{@link OptionalLongStringConverter}</li>
 *     <li>{@link ShortStringConverter}</li>
 *     <li>{@link CharacterArrayStringConverter}</li>
 *     <li>{@link CharacterStringConverter}</li>
 *     <li>{@link CharSequenceStringConverter}</li>
 *     <li>{@link OffsetDateTimeStringConverter}</li>
 *     <li>{@link PeriodStringConverter}</li>
 *     <li>{@link StringStringConverter}</li>
 *     <li>{@link StringBufferStringConverter}</li>
 *     <li>{@link StringBuilderStringConverter}</li>
 *     <li>{@link UriStringConverter}</li>
 *     <li>{@link UrlStringConverter}</li>
 *     <li>{@link UuidStringConverter}</li>
 *     <li>{@link ZonedDateTimeStringConverter}</li>
 *     <li>{@link ZoneIdStringConverter}</li>
 *     <li>{@link ZoneOffsetStringConverter}</li>
 *     <li>{@link ByteArrayStringConverter}</li>
 *     <li>{@link ByteStringConverter}</li>
 *     <li>{@link SdkBytesStringConverter}</li>
 *     <li>{@link AtomicBooleanStringConverter}</li>
 *     <li>{@link BooleanStringConverter}</li>
 * </ul>
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class DefaultStringConverterProvider<T> implements StringConverterProvider {

    private final ConcurrentHashMap<TypeToken<?>, StringConverter<? extends T>> converterCache =
        new ConcurrentHashMap<>();

    private DefaultStringConverterProvider(Builder<T> builder) {
        // Converters are used in the REVERSE order of how they were added to the builder.
        for (int i = builder.converters.size() - 1; i >= 0; i--) {
            StringConverter<? extends T> converter = builder.converters.get(i);
            converterCache.put(converter.type(), converter);

            if (converter instanceof PrimitiveConverter) {
                PrimitiveConverter primitiveConverter = (PrimitiveConverter) converter;
                converterCache.put(primitiveConverter.primitiveType(), converter);
            }
        }
    }

    /**
     * Create a builder for a {@link DefaultStringConverterProvider}.
     */
    public static Builder<Object> builder() {
        return new Builder<>();
    }

    public static DefaultStringConverterProvider create() {
        return DefaultStringConverterProvider.builder()
                                             .addConverter(ByteArrayStringConverter.create())
                                             .addConverter(CharacterArrayStringConverter.create())
                                             .addConverter(BooleanStringConverter.create())
                                             .addConverter(ShortStringConverter.create())
                                             .addConverter(IntegerStringConverter.create())
                                             .addConverter(LongStringConverter.create())
                                             .addConverter(FloatStringConverter.create())
                                             .addConverter(DoubleStringConverter.create())
                                             .addConverter(CharacterStringConverter.create())
                                             .addConverter(ByteStringConverter.create())
                                             .addConverter(StringStringConverter.create())
                                             .addConverter(CharSequenceStringConverter.create())
                                             .addConverter(StringBufferStringConverter.create())
                                             .addConverter(StringBuilderStringConverter.create())
                                             .addConverter(BigIntegerStringConverter.create())
                                             .addConverter(BigDecimalStringConverter.create())
                                             .addConverter(AtomicLongStringConverter.create())
                                             .addConverter(AtomicIntegerStringConverter.create())
                                             .addConverter(AtomicBooleanStringConverter.create())
                                             .addConverter(OptionalIntStringConverter.create())
                                             .addConverter(OptionalLongStringConverter.create())
                                             .addConverter(OptionalDoubleStringConverter.create())
                                             .addConverter(InstantStringConverter.create())
                                             .addConverter(DurationStringConverter.create())
                                             .addConverter(LocalDateStringConverter.create())
                                             .addConverter(LocalTimeStringConverter.create())
                                             .addConverter(LocalDateTimeStringConverter.create())
                                             .addConverter(OffsetTimeStringConverter.create())
                                             .addConverter(OffsetDateTimeStringConverter.create())
                                             .addConverter(ZonedDateTimeStringConverter.create())
                                             .addConverter(YearStringConverter.create())
                                             .addConverter(YearMonthStringConverter.create())
                                             .addConverter(MonthDayStringConverter.create())
                                             .addConverter(PeriodStringConverter.create())
                                             .addConverter(ZoneOffsetStringConverter.create())
                                             .addConverter(ZoneIdStringConverter.create())
                                             .addConverter(UuidStringConverter.create())
                                             .addConverter(UrlStringConverter.create())
                                             .addConverter(UriStringConverter.create())
                                             .build();
    }

    @Override
    public <T> StringConverter<T> converterFor(TypeToken<T> typeToken) {
        @SuppressWarnings("unchecked") // We initialized correctly, so this is safe.
        StringConverter<T> converter = (StringConverter<T>) converterCache.get(typeToken);

        if (converter == null) {
            throw new IllegalArgumentException("No string converter exists for " + typeToken.rawClass());
        }

        return converter;
    }

    /**
     * A builder for configuring and creating {@link DefaultStringConverterProvider}s.
     */
    public static class Builder<T> {
        private List<StringConverter<? extends T>> converters = new ArrayList<>();

        private Builder() {}

        public Builder<T> addConverters(Collection<? extends StringConverter<? extends T>> converters) {
            Validate.paramNotNull(converters, "converters");
            Validate.noNullElements(converters, "Converters must not contain null members.");
            this.converters.addAll(converters);
            return this;
        }

        public Builder<T> addConverter(StringConverter<? extends T> converter) {
            Validate.paramNotNull(converter, "converter");
            this.converters.add(converter);
            return this;
        }

        public Builder<T> clearConverters() {
            this.converters.clear();
            return this;
        }

        public DefaultStringConverterProvider<T> build() {
            return new DefaultStringConverterProvider(this);
        }
    }
}

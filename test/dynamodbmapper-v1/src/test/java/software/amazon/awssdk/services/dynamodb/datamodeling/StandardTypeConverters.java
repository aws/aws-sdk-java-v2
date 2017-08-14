/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.util.DateUtils;

/**
 * Type conversions.
 *
 * @see DynamoDbTypeConverter
 */
@SdkInternalApi
final class StandardTypeConverters extends DynamoDbTypeConverterFactory {

    /**
     * Standard scalar type-converter factory.
     */
    private static final DynamoDbTypeConverterFactory FACTORY = new StandardTypeConverters();

    static DynamoDbTypeConverterFactory factory() {
        return StandardTypeConverters.FACTORY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S, T> DynamoDbTypeConverter<S, T> getConverter(Class<S> sourceType, Class<T> targetType) {
        final Scalar source = Scalar.of(sourceType);
        final Scalar target = Scalar.of(targetType);
        final Converter<S, T> toSource = source.getConverter(sourceType, target.<T>type());
        final Converter<T, S> toTarget = target.getConverter(targetType, source.<S>type());
        return new StandardDynamoDbTypeConverter<>(toSource, toTarget);
    }

    /**
     * Standard scalar types.
     */
    enum Scalar {
        /**
         * {@link BigDecimal}
         */
        BIG_DECIMAL(ScalarAttributeType.N, new ConverterMap(BigDecimal.class, null)
                .with(Number.class, ToBigDecimal.FROM_STRING.join(ToString.FROM_NUMBER))
                .with(String.class, ToBigDecimal.FROM_STRING)
        ),

        /**
         * {@link BigInteger}
         */
        BIG_INTEGER(ScalarAttributeType.N, new ConverterMap(BigInteger.class, null)
                .with(Number.class, ToBigInteger.FROM_STRING.join(ToString.FROM_NUMBER))
                .with(String.class, ToBigInteger.FROM_STRING)
        ),

        /**
         * {@link Boolean}
         */
        BOOLEAN(ScalarAttributeType.N, new ConverterMap(Boolean.class, Boolean.TYPE)
                .with(Number.class, ToBoolean.FROM_STRING.join(ToString.FROM_NUMBER))
                .with(String.class, ToBoolean.FROM_STRING)
        ),

        /**
         * {@link Byte}
         */
        BYTE(ScalarAttributeType.N, new ConverterMap(Byte.class, Byte.TYPE)
                .with(Number.class, ToByte.FROM_NUMBER)
                .with(String.class, ToByte.FROM_STRING)
        ),

        /**
         * {@link Byte} array
         */
        BYTE_ARRAY(ScalarAttributeType.B, new ConverterMap(byte[].class, null)
                .with(ByteBuffer.class, ToByteArray.FROM_BYTE_BUFFER)
                .with(String.class, ToByteArray.FROM_STRING)
        ),

        /**
         * {@link ByteBuffer}
         */
        BYTE_BUFFER(ScalarAttributeType.B, new ConverterMap(ByteBuffer.class, null)
                .with(byte[].class, ToByteBuffer.FROM_BYTE_ARRAY)
                .with(String.class, ToByteBuffer.FROM_BYTE_ARRAY.join(ToByteArray.FROM_STRING))
                .with(java.util.UUID.class, ToByteBuffer.FROM_UUID)
        ),

        /**
         * {@link Calendar}
         */
        CALENDAR(ScalarAttributeType.S, new ConverterMap(Calendar.class, null)
                .with(Date.class, ToCalendar.FROM_DATE)
                .with(Instant.class, ToCalendar.FROM_INSTANT)
                .with(Long.class, ToCalendar.FROM_INSTANT.join(ToInstant.FROM_LONG))
                .with(String.class, ToCalendar.FROM_INSTANT.join(ToInstant.FROM_STRING))
        ),

        /**
         * {@link Character}
         */
        CHARACTER(ScalarAttributeType.S, new ConverterMap(Character.class, Character.TYPE)
                .with(String.class, ToCharacter.FROM_STRING)
        ),

        /**
         * {@link Currency}
         */
        CURRENCY(ScalarAttributeType.S, new ConverterMap(Currency.class, null)
                .with(String.class, ToCurrency.FROM_STRING)
        ),

        /**
         * {@link Instant}
         */
        INSTANT(ScalarAttributeType.S, new ConverterMap(Instant.class, null)
                .with(Calendar.class, ToInstant.FROM_CALENDAR)
                .with(ZonedDateTime.class, ToInstant.FROM_ZONEDDATETIME)
                .with(Long.class, ToInstant.FROM_LONG)
                .with(String.class, ToInstant.FROM_STRING)
        ),
        /**
         * {@link Date}
         */
        DATE(ScalarAttributeType.S, new ConverterMap(Date.class, null)
                .with(Instant.class, ToDate.FROM_INSTANT)
                .with(Calendar.class, ToDate.FROM_CALENDAR)
                .with(ZonedDateTime.class, ToDate.FROM_INSTANT.join(ToInstant.FROM_ZONEDDATETIME))
                .with(Long.class, ToDate.FROM_INSTANT.join(ToInstant.FROM_LONG))
                .with(String.class, ToDate.FROM_STRING)
        ),

        /**
         * {@link ZonedDateTime}
         */
        ZONED_DATE_TIME(/*ScalarAttributeType.S*/null, new ConverterMap(ZonedDateTime.class, null)
                .with(Calendar.class, ToDateTime.FROM_CALENDAR)
                .with(Date.class, ToDateTime.FROM_DATE)
                .with(Long.class, ToDateTime.FROM_INSTANT_AT_UTC.join(ToInstant.FROM_LONG))
                .with(String.class, ToDateTime.FROM_INSTANT_AT_UTC.join(ToInstant.FROM_STRING))
        ),

        /**
         * {@link Double}
         */
        DOUBLE(ScalarAttributeType.N, new ConverterMap(Double.class, Double.TYPE)
                .with(Number.class, ToDouble.FROM_NUMBER)
                .with(String.class, ToDouble.FROM_STRING)
        ),

        /**
         * {@link Float}
         */
        FLOAT(ScalarAttributeType.N, new ConverterMap(Float.class, Float.TYPE)
                .with(Number.class, ToFloat.FROM_NUMBER)
                .with(String.class, ToFloat.FROM_STRING)
        ),

        /**
         * {@link Integer}
         */
        INTEGER(ScalarAttributeType.N, new ConverterMap(Integer.class, Integer.TYPE)
                .with(Number.class, ToInteger.FROM_NUMBER)
                .with(String.class, ToInteger.FROM_STRING)
        ),

        /**
         * {@link Locale}
         */
        LOCALE(ScalarAttributeType.S, new ConverterMap(Locale.class, null)
                .with(String.class, ToLocale.FROM_STRING)
        ),

        /**
         * {@link Long}
         */
        LONG(ScalarAttributeType.N, new ConverterMap(Long.class, Long.TYPE)
                .with(Date.class, ToLong.FROM_DATE)
                .with(ZonedDateTime.class, ToLong.FROM_TEMPORAL_ACCESSOR.join(ToInstant.FROM_ZONEDDATETIME))
                .with(Number.class, ToLong.FROM_NUMBER)
                .with(String.class, ToLong.FROM_STRING)
        ),

        /**
         * {@link S3Link}
         */
        S3_LINK(ScalarAttributeType.S, new ConverterMap(S3Link.class, null)),

        /**
         * {@link Short}
         */
        SHORT(ScalarAttributeType.N, new ConverterMap(Short.class, Short.TYPE)
                .with(Number.class, ToShort.FROM_NUMBER)
                .with(String.class, ToShort.FROM_STRING)
        ),

        /**
         * {@link String}
         */
        STRING(ScalarAttributeType.S, new ConverterMap(String.class, null)
                .with(Boolean.class, ToString.FROM_BOOLEAN)
                .with(byte[].class, ToString.FROM_BYTE_ARRAY)
                .with(ByteBuffer.class, ToString.FROM_BYTE_ARRAY.join(ToByteArray.FROM_BYTE_BUFFER))
                .with(Calendar.class, ToString.FROM_DATE.join(ToDate.FROM_CALENDAR))
                .with(Date.class, ToString.FROM_DATE)
                .with(ZonedDateTime.class, ToString.FROM_DATE_TIME)
                .with(Enum.class, ToString.FROM_ENUM)
                .with(Locale.class, ToString.FROM_LOCALE)
                .with(TimeZone.class, ToString.FROM_TIME_ZONE)
                .with(ZoneId.class, ToString.FROM_ZONE_ID)
                .with(Object.class, ToString.FROM_OBJECT)
        ),

        /**
         * {@link TimeZone}
         */
        TIME_ZONE(ScalarAttributeType.S, new ConverterMap(TimeZone.class, null)
                .with(String.class, ToTimeZone.FROM_STRING)
        ),
        /**
         * {@link ZoneId}
         */
        ZONE_ID(ScalarAttributeType.S, new ConverterMap(ZoneId.class, null)
                .with(String.class, ToZoneId.FROM_STRING)
        ),

        /**
         * {@link java.net.URL}
         */
        URL(ScalarAttributeType.S, new ConverterMap(java.net.URL.class, null)
                .with(String.class, ToUrl.FROM_STRING)
        ),

        /**
         * {@link java.net.URI}
         */
        URI(ScalarAttributeType.S, new ConverterMap(java.net.URI.class, null)
                .with(String.class, ToUri.FROM_STRING)
        ),

        /**
         * {@link java.util.UUID}
         */
        UUID(ScalarAttributeType.S, new ConverterMap(java.util.UUID.class, null)
                .with(ByteBuffer.class, ToUuid.FROM_BYTE_BUFFER)
                .with(String.class, ToUuid.FROM_STRING)
        ),

        /**
         * {@link Object}; default must be last
         */
        DEFAULT(null, new ConverterMap(Object.class, null)) {
            @Override
            <S, T> Converter<S, T> getConverter(Class<S> sourceType, Class<T> targetType) {
                if (sourceType.isEnum() && STRING.map.isAssignableFrom(targetType)) {
                    return (Converter<S, T>) new ToEnum.FromString(sourceType);
                }
                return super.<S, T>getConverter(sourceType, targetType);
            }
        };

        /**
         * The scalar attribute type.
         */
        private final ScalarAttributeType scalarAttributeType;

        /**
         * The mapping of conversion functions for this scalar.
         */
        private final ConverterMap map;

        /**
         * Constructs a new scalar with the specified conversion mappings.
         */
        Scalar(ScalarAttributeType scalarAttributeType, ConverterMap map) {
            this.scalarAttributeType = scalarAttributeType;
            this.map = map;
        }

        /**
         * Returns the first matching scalar, which may be the same as or a
         * supertype of the specified target class.
         */
        static Scalar of(Class<?> type) {
            for (final Scalar scalar : Scalar.values()) {
                if (scalar.is(type)) {
                    return scalar;
                }
            }
            return DEFAULT;
        }

        /**
         * Returns the function to convert from the specified target class to
         * this scalar type.
         */
        <S, T> Converter<S, T> getConverter(Class<S> sourceType, Class<T> targetType) {
            return map.<S, T>getConverter(targetType);
        }

        /**
         * Converts the target instance using the standard type-conversions.
         */
        @SuppressWarnings("unchecked")
        <S> S convert(Object o) {
            return getConverter(this.<S>type(), (Class<Object>) o.getClass()).convert(o);
        }

        /**
         * Determines if the scalar is of the specified scalar attribute type.
         */
        boolean is(final ScalarAttributeType scalarAttributeType) {
            return this.scalarAttributeType == scalarAttributeType;
        }

        /**
         * Determines if the class represented by this scalar is either the
         * same as or a supertype of the specified target type.
         */
        boolean is(final Class<?> type) {
            return this.map.isAssignableFrom(type);
        }

        /**
         * Returns the primary reference type.
         */
        @SuppressWarnings("unchecked")
        <S> Class<S> type() {
            return (Class<S>) this.map.referenceType;
        }
    }

    /**
     * Standard vector types.
     */
    abstract static class Vector {
        /**
         * {@link List}
         */
        static final ToList LIST = new ToList();
        /**
         * {@link Map}
         */
        static final ToMap MAP = new ToMap();
        /**
         * {@link Set}
         */
        static final ToSet SET = new ToSet();

        /**
         * Determines if the class represented by this vector is either the
         * same as or a supertype of the specified target type.
         */
        abstract boolean is(Class<?> type);

        static final class ToList extends Vector {
            <S, T> DynamoDbTypeConverter<List<S>, List<T>> join(final DynamoDbTypeConverter<S, T> scalar) {
                return new ListTypeConverter<>(scalar);
            }

            <S, T> List<S> convert(Collection<T> o, DynamoDbTypeConverter<S, T> scalar) {
                final List<S> vector = new ArrayList<S>(o.size());
                for (final T t : o) {
                    vector.add(scalar.convert(t));
                }
                return vector;
            }

            <S, T> List<T> unconvert(Collection<S> o, DynamoDbTypeConverter<S, T> scalar) {
                final List<T> vector = new ArrayList<T>(o.size());
                for (final S s : o) {
                    vector.add(scalar.unconvert(s));
                }
                return vector;
            }

            @Override
            boolean is(final Class<?> type) {
                return List.class.isAssignableFrom(type);
            }

            private static class ListTypeConverter<S, T> implements DynamoDbTypeConverter<List<S>, List<T>> {
                private final DynamoDbTypeConverter<S, T> scalar;

                ListTypeConverter(DynamoDbTypeConverter<S, T> scalar) {
                    this.scalar = scalar;
                }

                @Override
                public final List<S> convert(final List<T> o) {
                    return LIST.convert(o, scalar);
                }

                @Override
                public final List<T> unconvert(final List<S> o) {
                    return LIST.unconvert(o, scalar);
                }
            }
        }

        static final class ToMap extends Vector {
            <K, S, T> DynamoDbTypeConverter<Map<K, S>, Map<K, T>> join(final DynamoDbTypeConverter<S, T> scalar) {
                return new MapTypeConverter<>(scalar);
            }

            <K, S, T> Map<K, S> convert(Map<K, T> o, DynamoDbTypeConverter<S, T> scalar) {
                final Map<K, S> vector = new LinkedHashMap<K, S>();
                for (final Map.Entry<K, T> t : o.entrySet()) {
                    vector.put(t.getKey(), scalar.convert(t.getValue()));
                }
                return vector;
            }

            <K, S, T> Map<K, T> unconvert(Map<K, S> o, DynamoDbTypeConverter<S, T> scalar) {
                final Map<K, T> vector = new LinkedHashMap<K, T>();
                for (final Map.Entry<K, S> s : o.entrySet()) {
                    vector.put(s.getKey(), scalar.unconvert(s.getValue()));
                }
                return vector;
            }

            boolean is(final Class<?> type) {
                return Map.class.isAssignableFrom(type);
            }

            private static class MapTypeConverter<K, S, T> implements DynamoDbTypeConverter<Map<K, S>, Map<K, T>> {
                private final DynamoDbTypeConverter<S, T> scalar;

                MapTypeConverter(DynamoDbTypeConverter<S, T> scalar) {
                    this.scalar = scalar;
                }

                @Override
                public final Map<K, S> convert(final Map<K, T> o) {
                    return MAP.convert(o, scalar);
                }

                @Override
                public final Map<K, T> unconvert(final Map<K, S> o) {
                    return MAP.unconvert(o, scalar);
                }
            }
        }

        static final class ToSet extends Vector {
            <S, T> DynamoDbTypeConverter<List<S>, Collection<T>> join(final DynamoDbTypeConverter<S, T> target) {
                return new SetTypeConverter<>(target);
            }

            <S, T> Set<T> unconvert(Collection<S> o, DynamoDbTypeConverter<S, T> scalar) {
                final Set<T> vector = new LinkedHashSet<T>();
                for (final S s : o) {
                    if (vector.add(scalar.unconvert(s)) == false) {
                        throw new DynamoDbMappingException("duplicate value (" + s + ")");
                    }
                }
                return vector;
            }

            boolean is(final Class<?> type) {
                return Set.class.isAssignableFrom(type);
            }

            private static class SetTypeConverter<S, T> implements DynamoDbTypeConverter<List<S>, Collection<T>> {
                private final DynamoDbTypeConverter<S, T> target;

                SetTypeConverter(DynamoDbTypeConverter<S, T> target) {
                    this.target = target;
                }

                @Override
                public List<S> convert(final Collection<T> o) {
                    return LIST.convert(o, target);
                }

                @Override
                public Collection<T> unconvert(final List<S> o) {
                    return SET.unconvert(o, target);
                }
            }
        }
    }

    /**
     * Converter map.
     */
    private static class ConverterMap extends LinkedHashMap<Class<?>, Converter<?, ?>> {
        private static final long serialVersionUID = -1L;
        private final Class<?> referenceType;
        private final Class<?> primitiveType;

        private ConverterMap(Class<?> referenceType, Class<?> primitiveType) {
            this.referenceType = referenceType;
            this.primitiveType = primitiveType;
        }

        private <S, T> ConverterMap with(Class<T> targetType, Converter<S, T> converter) {
            put(targetType, converter);
            return this;
        }

        private boolean isAssignableFrom(Class<?> type) {
            return type.isPrimitive() ? primitiveType == type : referenceType.isAssignableFrom(type);
        }

        @SuppressWarnings("unchecked")
        private <S, T> Converter<S, T> getConverter(Class<T> targetType) {
            for (final Map.Entry<Class<?>, Converter<?, ?>> entry : entrySet()) {
                if (entry.getKey().isAssignableFrom(targetType)) {
                    return (Converter<S, T>) entry.getValue();
                }
            }
            if (isAssignableFrom(targetType)) {
                return (Converter<S, T>) ToObject.FROM_OBJECT;
            }
            throw new DynamoDbMappingException(
                    "type [" + targetType + "] is not supported; no conversion from " + referenceType
            );
        }
    }

    /**
     * {@link BigDecimal} conversion functions.
     */
    private abstract static class ToBigDecimal<T> extends Converter<BigDecimal, T> {
        private static final ToBigDecimal<String> FROM_STRING = new ToBigDecimal<String>() {
            @Override
            public BigDecimal convert(final String o) {
                return new BigDecimal(o);
            }
        };
    }

    /**
     * {@link BigInteger} conversion functions.
     */
    private abstract static class ToBigInteger<T> extends Converter<BigInteger, T> {
        private static final ToBigInteger<String> FROM_STRING = new ToBigInteger<String>() {
            @Override
            public BigInteger convert(final String o) {
                return new BigInteger(o);
            }
        };
    }

    /**
     * {@link Boolean} conversion functions.
     */
    private abstract static class ToBoolean<T> extends Converter<Boolean, T> {
        private static final ToBoolean<String> FROM_STRING = new ToBoolean<String>() {
            private final Pattern n0 = Pattern.compile("(?i)[N0]");
            private final Pattern y1 = Pattern.compile("(?i)[Y1]");

            @Override
            public Boolean convert(final String o) {
                return n0.matcher(o).matches() ? Boolean.FALSE : y1.matcher(o).matches() ? Boolean.TRUE : Boolean.valueOf(o);
            }
        };
    }

    /**
     * {@link Byte} conversion functions.
     */
    private abstract static class ToByte<T> extends Converter<Byte, T> {
        private static final ToByte<Number> FROM_NUMBER = new ToByte<Number>() {
            @Override
            public Byte convert(final Number o) {
                return o.byteValue();
            }
        };

        private static final ToByte<String> FROM_STRING = new ToByte<String>() {
            @Override
            public Byte convert(final String o) {
                return Byte.valueOf(o);
            }
        };
    }

    /**
     * {@link byte} array conversion functions.
     */
    private abstract static class ToByteArray<T> extends Converter<byte[], T> {
        private static final ToByteArray<ByteBuffer> FROM_BYTE_BUFFER = new ToByteArray<ByteBuffer>() {
            @Override
            public byte[] convert(final ByteBuffer o) {
                if (o.hasArray()) {
                    return o.array();
                }
                final byte[] value = new byte[o.remaining()];
                o.get(value);
                return value;
            }
        };

        private static final ToByteArray<String> FROM_STRING = new ToByteArray<String>() {
            @Override
            public byte[] convert(final String o) {
                return o.getBytes(Charset.forName("UTF-8"));
            }
        };
    }

    /**
     * {@link ByteBuffer} conversion functions.
     */
    private abstract static class ToByteBuffer<T> extends Converter<ByteBuffer, T> {
        private static final ToByteBuffer<byte[]> FROM_BYTE_ARRAY = new ToByteBuffer<byte[]>() {
            @Override
            public ByteBuffer convert(final byte[] o) {
                return ByteBuffer.wrap(o);
            }
        };

        private static final ToByteBuffer<java.util.UUID> FROM_UUID = new ToByteBuffer<java.util.UUID>() {
            @Override
            public ByteBuffer convert(final java.util.UUID o) {
                final ByteBuffer value = ByteBuffer.allocate(16);
                value.putLong(o.getMostSignificantBits()).putLong(o.getLeastSignificantBits());
                value.position(0);
                return value;
            }
        };
    }

    /**
     * {@link Calendar} conversion functions.
     */
    private abstract static class ToCalendar<T> extends Converter<Calendar, T> {
        private static final ToCalendar<Date> FROM_DATE = new ToCalendar<Date>() {
            @Override
            public Calendar convert(final Date o) {
                final Calendar value = Calendar.getInstance();
                value.setTime(o);
                return value;
            }
        };
        private static final ToCalendar<Instant> FROM_INSTANT = new ToCalendar<Instant>() {
            @Override
            public Calendar convert(Instant o) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(o.toEpochMilli());
                return cal;
            }
        };
    }

    /**
     * {@link Character} conversion functions.
     */
    private abstract static class ToCharacter<T> extends Converter<Character, T> {
        private static final ToCharacter<String> FROM_STRING = new ToCharacter<String>() {
            @Override
            public Character convert(final String o) {
                return Character.valueOf(o.charAt(0));
            }
        };
    }

    /**
     * {@link Currency} conversion functions.
     */
    private abstract static class ToCurrency<T> extends Converter<Currency, T> {
        private static final ToCurrency<String> FROM_STRING = new ToCurrency<String>() {
            @Override
            public Currency convert(final String o) {
                return Currency.getInstance(o);
            }
        };
    }

    private abstract static class ToDate<T> extends Converter<Date, T> {
        private static final ToDate<Instant> FROM_INSTANT = new ToDate<Instant>() {
            @Override
            public Date convert(Instant o) {
                return Date.from(o);
            }
        };
        private static final ToDate<Calendar> FROM_CALENDAR = new ToDate<Calendar>() {
            @Override
            public Date convert(Calendar o) {
                return o.getTime();
            }
        };
        private static final ToDate<String> FROM_STRING = new ToDate<String>() {
            @Override
            public Date convert(String s) {
                return Date.from(DateUtils.parseIso8601Date(s));
            }
        };
    }

    /**
     * {@link Instant} conversion functions.
     */
    private abstract static class ToInstant<T> extends Converter<Instant, T> {
        private static final ToInstant<Calendar> FROM_CALENDAR = new ToInstant<Calendar>() {
            @Override
            public Instant convert(final Calendar o) {
                return o.toInstant();
            }
        };

        private static final ToInstant<ZonedDateTime> FROM_ZONEDDATETIME = new ToInstant<ZonedDateTime>() {
            @Override
            public Instant convert(final ZonedDateTime o) {
                return o.toInstant();
            }
        };

        private static final ToInstant<Long> FROM_LONG = new ToInstant<Long>() {
            @Override
            public Instant convert(final Long o) {
                return Instant.ofEpochMilli(o);
            }
        };

        private static final ToInstant<String> FROM_STRING = new ToInstant<String>() {
            @Override
            public Instant convert(final String o) {
                return DateUtils.parseIso8601Date(o);
            }
        };
    }

    /**
     * {@link java.time.ZonedDateTime} conversion functions.
     */
    private abstract static class ToDateTime<T> extends Converter<ZonedDateTime, T> {
        private static final ToDateTime<Date> FROM_DATE = new ToDateTime<Date>() {
            public ZonedDateTime convert(final Date o) {
                return ZonedDateTime.ofInstant(o.toInstant(), ZoneOffset.UTC);
            }
        };
        private static final ToDateTime<Instant> FROM_INSTANT_AT_UTC = new ToDateTime<Instant>() {
            public ZonedDateTime convert(final Instant o) {
                return ZonedDateTime.ofInstant(o, ZoneOffset.UTC);
            }
        };

        private static final ToDateTime<Calendar> FROM_CALENDAR = new ToDateTime<Calendar>() {
            @Override
            public ZonedDateTime convert(Calendar o) {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(o.getTimeInMillis()), o.getTimeZone().toZoneId());
            }
        };
    }

    /**
     * {@link Double} conversion functions.
     */
    private abstract static class ToDouble<T> extends Converter<Double, T> {
        private static final ToDouble<Number> FROM_NUMBER = new ToDouble<Number>() {
            @Override
            public Double convert(final Number o) {
                return o.doubleValue();
            }
        };

        private static final ToDouble<String> FROM_STRING = new ToDouble<String>() {
            @Override
            public Double convert(final String o) {
                return Double.valueOf(o);
            }
        };
    }

    /**
     * {@link Enum} from {@link String}
     */
    private abstract static class ToEnum<S extends Enum<S>, T> extends Converter<S, T> {
        private static final class FromString<S extends Enum<S>> extends ToEnum<S, String> {
            private final Class<S> sourceType;

            private FromString(final Class<S> sourceType) {
                this.sourceType = sourceType;
            }

            @Override
            public S convert(final String o) {
                return Enum.valueOf(sourceType, o);
            }
        }
    }

    /**
     * {@link Float} conversion functions.
     */
    private abstract static class ToFloat<T> extends Converter<Float, T> {
        private static final ToFloat<Number> FROM_NUMBER = new ToFloat<Number>() {
            @Override
            public Float convert(final Number o) {
                return o.floatValue();
            }
        };

        private static final ToFloat<String> FROM_STRING = new ToFloat<String>() {
            @Override
            public Float convert(final String o) {
                return Float.valueOf(o);
            }
        };
    }

    /**
     * {@link Integer} conversion functions.
     */
    private abstract static class ToInteger<T> extends Converter<Integer, T> {
        private static final ToInteger<Number> FROM_NUMBER = new ToInteger<Number>() {
            @Override
            public Integer convert(final Number o) {
                return o.intValue();
            }
        };

        private static final ToInteger<String> FROM_STRING = new ToInteger<String>() {
            @Override
            public Integer convert(final String o) {
                return Integer.valueOf(o);
            }
        };
    }

    /**
     * {@link Locale} conversion functions.
     */
    private abstract static class ToLocale<T> extends Converter<Locale, T> {
        private static final ToLocale<String> FROM_STRING = new ToLocale<String>() {
            @Override
            public Locale convert(final String o) {
                final String[] value = o.split("-", 3);
                if (value.length == 3) {
                    return new Locale(value[0], value[1], value[2]);
                }
                if (value.length == 2) {
                    return new Locale(value[0], value[1]);
                }
                return new Locale(value[0]); //JDK7+: return Locale.forLanguageTag(o);
            }
        };
    }

    /**
     * {@link Long} conversion functions.
     */
    private abstract static class ToLong<T> extends Converter<Long, T> {
        private static final ToLong<Date> FROM_DATE = new ToLong<Date>() {
            @Override
            public Long convert(final Date o) {
                return o.getTime();
            }
        };
        private static final ToLong<Instant> FROM_TEMPORAL_ACCESSOR = new ToLong<Instant>() {
            @Override
            public Long convert(final Instant o) {
                return o.toEpochMilli();
            }
        };

        private static final ToLong<Number> FROM_NUMBER = new ToLong<Number>() {
            @Override
            public Long convert(final Number o) {
                return o.longValue();
            }
        };

        private static final ToLong<String> FROM_STRING = new ToLong<String>() {
            @Override
            public Long convert(final String o) {
                return Long.valueOf(o);
            }
        };
    }

    /**
     * {@link Short} conversion functions.
     */
    private abstract static class ToShort<T> extends Converter<Short, T> {
        private static final ToShort<Number> FROM_NUMBER = new ToShort<Number>() {
            @Override
            public Short convert(final Number o) {
                return o.shortValue();
            }
        };

        private static final ToShort<String> FROM_STRING = new ToShort<String>() {
            @Override
            public Short convert(final String o) {
                return Short.valueOf(o);
            }
        };
    }

    /**
     * {@link String} conversion functions.
     */
    private abstract static class ToString<T> extends Converter<String, T> {
        private static final ToString<Boolean> FROM_BOOLEAN = new ToString<Boolean>() {
            @Override
            public String convert(final Boolean o) {
                return Boolean.TRUE.equals(o) ? "1" : "0";
            }
        };

        private static final ToString<byte[]> FROM_BYTE_ARRAY = new ToString<byte[]>() {
            @Override
            public String convert(final byte[] o) {
                return new String(o, Charset.forName("UTF-8"));
            }
        };

        private static final ToString<Date> FROM_DATE = new ToString<Date>() {
            @Override
            public String convert(final Date o) {
                return DateUtils.formatIso8601Date(o.toInstant());
            }
        };

        private static final ToString<Enum> FROM_ENUM = new ToString<Enum>() {
            @Override
            public String convert(final Enum o) {
                return o.name();
            }
        };

        private static final ToString<Locale> FROM_LOCALE = new ToString<Locale>() {
            @Override
            public String convert(final Locale o) {
                final StringBuilder value = new StringBuilder(o.getLanguage());
                if (!o.getCountry().isEmpty() || !o.getVariant().isEmpty()) {
                    value.append("-").append(o.getCountry());
                }
                if (!o.getVariant().isEmpty()) {
                    value.append("-").append(o.getVariant());
                }
                return value.toString(); //JDK7+: return o.toLanguageTag();
            }
        };

        private static final ToString<Number> FROM_NUMBER = new ToString<Number>() {
            @Override
            public String convert(final Number o) {
                return o.toString();
            }
        };

        private static final ToString<TimeZone> FROM_TIME_ZONE = new ToString<TimeZone>() {
            @Override
            public String convert(final TimeZone o) {
                return o.getID();
            }
        };
        private static final ToString<ZoneId> FROM_ZONE_ID = new ToString<ZoneId>() {
            @Override
            public String convert(final ZoneId o) {
                return o.getId();
            }
        };

        private static final ToString<Object> FROM_OBJECT = new ToString<Object>() {
            @Override
            public String convert(final Object o) {
                return o.toString();
            }
        };

        private static final ToString<ZonedDateTime> FROM_DATE_TIME = new ToString<ZonedDateTime>() {
            @Override
            public String convert(ZonedDateTime o) {
                return ISO_DATE_TIME.format(o);
            }
        };
    }

    /**
     * {@link TimeZone} conversion functions.
     */
    private abstract static class ToTimeZone<T> extends Converter<TimeZone, T> {
        private static final ToTimeZone<String> FROM_STRING = new ToTimeZone<String>() {
            @Override
            public TimeZone convert(final String o) {
                return TimeZone.getTimeZone(o);
            }
        };
    }

    /**
     * {@link ZoneId} conversion functions.
     */
    private abstract static class ToZoneId<T> extends Converter<ZoneId, T> {
        private static final ToZoneId<String> FROM_STRING = new ToZoneId<String>() {
            @Override
            public ZoneId convert(final String o) {
                return ZoneId.of(o);
            }
        };
    }

    /**
     * {@link java.net.URL} conversion functions.
     */
    private abstract static class ToUrl<T> extends Converter<java.net.URL, String> {
        private static final ToUrl<String> FROM_STRING = new ToUrl<String>() {
            @Override
            public java.net.URL convert(final String o) {
                try {
                    return new java.net.URL(o);
                } catch (final java.net.MalformedURLException e) {
                    throw new IllegalArgumentException("malformed URL", e);
                }
            }
        };
    }

    /**
     * {@link java.net.URI} conversion functions.
     */
    private abstract static class ToUri<T> extends Converter<java.net.URI, T> {
        private static final ToUri<String> FROM_STRING = new ToUri<String>() {
            @Override
            public java.net.URI convert(final String o) {
                try {
                    return new java.net.URI(o);
                } catch (final java.net.URISyntaxException e) {
                    throw new IllegalArgumentException("malformed URI", e);
                }
            }
        };
    }

    /**
     * {@link java.util.UUID} conversion functions.
     */
    private abstract static class ToUuid<T> extends Converter<java.util.UUID, T> {
        private static final ToUuid<ByteBuffer> FROM_BYTE_BUFFER = new ToUuid<ByteBuffer>() {
            @Override
            public java.util.UUID convert(final ByteBuffer o) {
                return new java.util.UUID(o.getLong(), o.getLong());
            }
        };

        private static final ToUuid<String> FROM_STRING = new ToUuid<String>() {
            @Override
            public java.util.UUID convert(final String o) {
                return java.util.UUID.fromString(o);
            }
        };
    }

    /**
     * {@link Object} conversion functions.
     */
    private abstract static class ToObject<T> extends Converter<Object, T> {
        private static final ToObject<Object> FROM_OBJECT = new ToObject<Object>() {
            @Override
            public Object convert(final Object o) {
                return o;
            }
        };
    }

    /**
     * One-way type-converter.
     */
    abstract static class Converter<S, T> {
        final <U> Converter<S, U> join(final Converter<T, U> target) {
            final Converter<S, T> source = this;
            return new Converter<S, U>() {
                @Override
                public S convert(final U o) {
                    return source.convert(target.convert(o));
                }
            };
        }

        public abstract S convert(T o);
    }

    private static class StandardDynamoDbTypeConverter<S, T> implements DynamoDbTypeConverter<S, T> {
        private final Converter<S, T> toSource;
        private final Converter<T, S> toTarget;

        StandardDynamoDbTypeConverter(Converter<S, T> toSource, Converter<T, S> toTarget) {
            this.toSource = toSource;
            this.toTarget = toTarget;
        }

        @Override
        public final S convert(final T o) {
            return toSource.convert(o);
        }

        @Override
        public final T unconvert(final S o) {
            return toTarget.convert(o);
        }
    }
}

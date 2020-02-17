/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute;

import com.sun.jndi.toolkit.url.Uri;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.AtomicBooleanAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.AtomicIntegerAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.AtomicLongAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.BigDecimalAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.BooleanAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.ByteArrayAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.ByteAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.CharSequenceAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.CharacterArrayAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.CharacterAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.CollectionAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.DoubleAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.DurationAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.FloatAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.InstantAsIntegerAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.IntegerAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.LocalDateAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.LocalDateTimeAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.LocalTimeAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.LongAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.MapAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.MonthDayAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.OffsetDateTimeAsStringAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.OptionalDoubleAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.OptionalIntAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.OptionalLongAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.PeriodAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.SdkBytesAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.ShortAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.StringAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.StringBufferAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.StringBuilderAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.UriAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.UrlAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.UuidAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.ZoneIdAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.ZoneOffsetAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.ZonedDateTimeAsStringAttributeConverter;

public class DefaultAttributeConverterProvider implements AttributeConverterProvider {

    private static final Map<TypeToken, AttributeConverter> converters = new HashMap<>();

    static {
        converters.put(TypeToken.of(AtomicBoolean.class), AtomicBooleanAttributeConverter.create());
        converters.put(TypeToken.of(AtomicInteger.class), AtomicIntegerAttributeConverter.create());
        converters.put(TypeToken.of(AtomicLong.class), AtomicLongAttributeConverter.create());
        converters.put(TypeToken.of(BigDecimal.class), BigDecimalAttributeConverter.create());
        converters.put(TypeToken.of(Boolean.class), BooleanAttributeConverter.create());
        converters.put(TypeToken.of(byte[].class), ByteArrayAttributeConverter.create());
        converters.put(TypeToken.of(Byte.class), ByteAttributeConverter.create());
        converters.put(TypeToken.of(char[].class), CharacterArrayAttributeConverter.create());
        converters.put(TypeToken.of(Character.class), CharacterAttributeConverter.create());
        converters.put(TypeToken.of(CharSequence.class), CharSequenceAttributeConverter.create());
        converters.put(TypeToken.of(Double.class), DoubleAttributeConverter.create());
        converters.put(TypeToken.of(Duration.class), DurationAttributeConverter.create());
        converters.put(TypeToken.of(Float.class), FloatAttributeConverter.create());
        converters.put(TypeToken.of(Instant.class), InstantAsIntegerAttributeConverter.create());
        converters.put(TypeToken.of(Integer.class), IntegerAttributeConverter.create());
        converters.put(TypeToken.of(LocalDate.class), LocalDateAttributeConverter.create());
        converters.put(TypeToken.of(LocalDateTime.class), LocalDateTimeAttributeConverter.create());
        converters.put(TypeToken.of(LocalTime.class), LocalTimeAttributeConverter.create());
        converters.put(TypeToken.of(Long.class), LongAttributeConverter.create());
        converters.put(TypeToken.of(MonthDay.class), MonthDayAttributeConverter.create());
        converters.put(TypeToken.of(OffsetDateTime.class), OffsetDateTimeAsStringAttributeConverter.create());
        converters.put(TypeToken.of(OptionalDouble.class), OptionalDoubleAttributeConverter.create());
        converters.put(TypeToken.of(OptionalInt.class), OptionalIntAttributeConverter.create());
        converters.put(TypeToken.of(OptionalLong.class), OptionalLongAttributeConverter.create());
        converters.put(TypeToken.of(Period.class), PeriodAttributeConverter.create());
        converters.put(TypeToken.of(SdkBytes.class), SdkBytesAttributeConverter.create());
        converters.put(TypeToken.of(Short.class), ShortAttributeConverter.create());
        converters.put(TypeToken.of(String.class), StringAttributeConverter.create());
        converters.put(TypeToken.of(StringBuffer.class), StringBufferAttributeConverter.create());
        converters.put(TypeToken.of(StringBuilder.class), StringBuilderAttributeConverter.create());
        converters.put(TypeToken.of(Uri.class), UriAttributeConverter.create());
        converters.put(TypeToken.of(URL.class), UrlAttributeConverter.create());
        converters.put(TypeToken.of(UUID.class), UuidAttributeConverter.create());
        converters.put(TypeToken.of(ZonedDateTime.class), ZonedDateTimeAsStringAttributeConverter.create());
        converters.put(TypeToken.of(ZoneId.class), ZoneIdAttributeConverter.create());
        converters.put(TypeToken.of(ZoneOffset.class), ZoneOffsetAttributeConverter.create());

    }

    @Override
    public <T> AttributeConverter<T> converterFor(TypeToken<T> typeToken) {
        return converters.get(typeToken);
    }
}

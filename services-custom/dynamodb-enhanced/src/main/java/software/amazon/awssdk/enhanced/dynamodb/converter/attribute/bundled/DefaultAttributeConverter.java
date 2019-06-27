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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ChainAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bean.annotation.AnnotatedBeanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

/**
 * A {@link SubtypeAttributeConverter} that includes support for the majority of Java and the SDK's built-in types.
 *
 * <p>
 * This is the default converter included with all {@link DynamoDbEnhancedClient}s and {@link DynamoDbEnhancedAsyncClient}s.
 *
 * <p>
 * Included converters:
 * <ul>
 *     <li><b>Numeric Types</b></li>
 *     <li>{@link AtomicIntegerAttributeConverter}</li>
 *     <li>{@link AtomicLongAttributeConverter}</li>
 *     <li>{@link BigDecimalAttributeConverter}</li>
 *     <li>{@link BigIntegerAttributeConverter}</li>
 *     <li>{@link DoubleAttributeConverter}</li>
 *     <li>{@link DurationAttributeConverter}</li>
 *     <li>{@link FloatAttributeConverter}</li>
 *     <li>{@link InstantAsIntegerAttributeConverter}</li>
 *     <li>{@link IntegerAttributeConverter}</li>
 *     <li>{@link LocalDateAttributeConverter}</li>
 *     <li>{@link LocalDateTimeAttributeConverter}</li>
 *     <li>{@link LocalTimeAttributeConverter}</li>
 *     <li>{@link LongAttributeConverter}</li>
 *     <li>{@link MonthDayAttributeConverter}</li>
 *     <li>{@link OptionalDoubleAttributeConverter}</li>
 *     <li>{@link OptionalIntAttributeConverter}</li>
 *     <li>{@link OptionalLongAttributeConverter}</li>
 *     <li>{@link ShortAttributeConverter}</li>
 *
 *     <li><b>String Types</b></li>
 *     <li>{@link CharacterArrayAttributeConverter}</li>
 *     <li>{@link CharacterAttributeConverter}</li>
 *     <li>{@link CharSequenceAttributeConverter}</li>
 *     <li>{@link OffsetDateTimeAsStringAttributeConverter}</li>
 *     <li>{@link PeriodAttributeConverter}</li>
 *     <li>{@link StringAttributeConverter}</li>
 *     <li>{@link StringBufferAttributeConverter}</li>
 *     <li>{@link StringBuilderAttributeConverter}</li>
 *     <li>{@link UriAttributeConverter}</li>
 *     <li>{@link UrlAttributeConverter}</li>
 *     <li>{@link UuidAttributeConverter}</li>
 *     <li>{@link ZonedDateTimeAsStringAttributeConverter}</li>
 *     <li>{@link ZoneIdAttributeConverter}</li>
 *     <li>{@link ZoneOffsetAttributeConverter}</li>
 *
 *     <li><b>Binary Types</b></li>
 *     <li>{@link ByteArrayAttributeConverter}</li>
 *     <li>{@link ByteAttributeConverter}</li>
 *     <li>{@link SdkBytesAttributeConverter}</li>
 *
 *     <li><b>Boolean Types</b></li>
 *     <li>{@link AtomicBooleanAttributeConverter}</li>
 *     <li>{@link BooleanAttributeConverter}</li>
 *
 *     <li><b>Collection Types</b></li>
 *     <li>{@link CollectionSubtypeAttributeConverter}</li>
 *
 *     <li><b>Map Types</b></li>
 *     <li>{@link RequestItemSubtypeAttributeConverter}</li>
 *     <li>{@link ResponseItemSubtypeAttributeConverter}</li>
 *     <li>{@link MapSubtypeAttributeConverter}</li>
 *
 *     <li><b>Other Types</b></li>
 *     <li>{@link AttributeAttributeConverter}</li>
 *     <li>{@link OptionalSubtypeAttributeConverter}</li>
 *     <li>{@link ConvertibleAttributeConverter}</li>
 *     <li>{@link AnnotatedBeanAttributeConverter}</li>
 * </ul>
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class DefaultAttributeConverter implements SubtypeAttributeConverter<Object> {
    private static final SubtypeAttributeConverter<Object> CHAIN;

    static {
        CHAIN = ChainAttributeConverter.builder()
                                       .addConverter(AtomicBooleanAttributeConverter.create())
                                       .addConverter(AtomicIntegerAttributeConverter.create())
                                       .addConverter(AtomicLongAttributeConverter.create())
                                       .addConverter(AttributeAttributeConverter.create())
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
                                       .addSubtypeConverter(AnnotatedBeanAttributeConverter.create())
                                       .addSubtypeConverter(CollectionSubtypeAttributeConverter.create())
                                       .addSubtypeConverter(ConvertibleAttributeConverter.create())
                                       .addSubtypeConverter(MapSubtypeAttributeConverter.create())
                                       .addSubtypeConverter(OptionalSubtypeAttributeConverter.create())
                                       .addSubtypeConverter(RequestItemSubtypeAttributeConverter.create())
                                       .addSubtypeConverter(ResponseItemSubtypeAttributeConverter.create())
                                       .build();
    }

    private DefaultAttributeConverter() {}

    /**
     * Create a default convert chain that contains all of the converters built into the SDK.
     *
     * <p>
     * This call should never fail with an {@link Exception}.
     */
    public static DefaultAttributeConverter create() {
        return new DefaultAttributeConverter();
    }

    @Override
    public TypeToken<Object> type() {
        return TypeToken.of(Object.class);
    }

    @Override
    public ItemAttributeValue toAttributeValue(Object input, ConversionContext context) {
        return CHAIN.toAttributeValue(input, context);
    }

    @Override
    public <U> U fromAttributeValue(ItemAttributeValue input, TypeToken<U> desiredType, ConversionContext context) {
        return CHAIN.fromAttributeValue(input, desiredType, context);
    }
}

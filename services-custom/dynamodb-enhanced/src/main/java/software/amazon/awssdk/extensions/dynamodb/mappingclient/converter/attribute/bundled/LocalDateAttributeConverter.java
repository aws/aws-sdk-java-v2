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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.AttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link LocalDate} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a number, so that they can be sorted numerically as part of a sort key.
 *
 * <p>
 * LocalDateTimes are stored in the format "[-]YYYYMMDD000000", where:
 * <ol>
 *     <li>Y is a year between {@link Year#MIN_VALUE} and {@link Year#MAX_VALUE} (prefixed with - if it is negative)</li>
 *     <li>M is a 2-character, zero-prefixed month between 01 and 12</li>
 *     <li>D is a 2-character, zero-prefixed day between 01 and 31</li>
 *     <li>0 is a 6-character padding allowing for support with {@link LocalDateTimeAttributeConverter}.</li>
 * </ol>
 *
 * <p>
 * This is format-compatible with the {@link LocalDateTimeAttributeConverter}, allowing values stored as {@link LocalDate} to be
 * retrieved as {@link LocalDateTime}s and vice-versa. The time associated with a value stored as a {@link LocalDate} is the
 * beginning of the day (midnight).
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code LocalDate.of(1988, 5, 21)} is stored as {@code ItemAttributeValueMapper.fromNumber("19880521000000")}</li>
 *     <li>{@code LocalDateTime.of(-1988, 5, 21)} is stored as {@code ItemAttributeValueMapper.fromNumber("-19880521000000")}</li>
 * </ul>
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class LocalDateAttributeConverter implements AttributeConverter<LocalDate> {
    private static final LocalDateTimeAttributeConverter LOCAL_DATE_TIME_ATTRIBUTE_CONVERTER =
        LocalDateTimeAttributeConverter.create();

    private LocalDateAttributeConverter() {
    }

    public static LocalDateAttributeConverter create() {
        return new LocalDateAttributeConverter();
    }

    @Override
    public TypeToken<LocalDate> type() {
        return TypeToken.of(LocalDate.class);
    }

    @Override
    public AttributeValue transformFrom(LocalDate input) {
        return LOCAL_DATE_TIME_ATTRIBUTE_CONVERTER.transformFrom(input.atStartOfDay());
    }

    @Override
    public LocalDate transformTo(AttributeValue input) {
        return LOCAL_DATE_TIME_ATTRIBUTE_CONVERTER.transformTo(input).toLocalDate();
    }
}

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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import java.time.Period;
import java.time.format.DateTimeParseException;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.PeriodStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Period} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a string, according to the format of {@link Period#parse(CharSequence)} and
 * {@link Period#toString()}.
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class PeriodAttributeConverter implements AttributeConverter<Period> {
    private static final Visitor VISITOR = new Visitor();
    private static final PeriodStringConverter STRING_CONVERTER = PeriodStringConverter.create();

    private PeriodAttributeConverter() {
    }

    public static PeriodAttributeConverter create() {
        return new PeriodAttributeConverter();
    }

    @Override
    public TypeToken<Period> type() {
        return TypeToken.of(Period.class);
    }

    @Override
    public AttributeValue transformFrom(Period input) {
        return ItemAttributeValue.fromString(STRING_CONVERTER.toString(input)).toGeneratedAttributeValue();
    }

    @Override
    public Period transformTo(AttributeValue input) {
        try {
            return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(VISITOR);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final class Visitor extends TypeConvertingVisitor<Period> {
        private Visitor() {
            super(Period.class, PeriodAttributeConverter.class);
        }

        @Override
        public Period convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}

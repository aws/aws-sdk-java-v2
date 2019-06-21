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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

public final class ConverterTestUtils {
    private ConverterTestUtils() {}

    public static <T> ItemAttributeValue toAttributeValue(AttributeConverter<T> converter, T value) {
        return converter.toAttributeValue(value, conversionContext());
    }

    public static <T> ItemAttributeValue toAttributeValue(SubtypeAttributeConverter<T> converter, T value) {
        return converter.toAttributeValue(value, conversionContext());
    }

    public static <T> T fromAttributeValue(AttributeConverter<T> converter, ItemAttributeValue value) {
        return converter.fromAttributeValue(value, conversionContext());
    }

    public static <T> T fromAttributeValue(SubtypeAttributeConverter<T> converter, ItemAttributeValue value) {
        return converter.fromAttributeValue(value, converter.type(), conversionContext());
    }

    public static <T, U extends T> U fromAttributeValue(SubtypeAttributeConverter<T> converter,
                                                        TypeToken<U> targetType,
                                                        ItemAttributeValue value) {
        return converter.fromAttributeValue(value, targetType, conversionContext());
    }

    public static void assertFails(ThrowableAssert.ThrowingCallable shouldRaiseThrowable) {
        assertThatThrownBy(shouldRaiseThrowable).isInstanceOf(IllegalArgumentException.class);
    }

    private static ConversionContext conversionContext() {
        return ConversionContext.builder()
                                .attributeConverter(DefaultAttributeConverter.create())
                                .build();
    }
}

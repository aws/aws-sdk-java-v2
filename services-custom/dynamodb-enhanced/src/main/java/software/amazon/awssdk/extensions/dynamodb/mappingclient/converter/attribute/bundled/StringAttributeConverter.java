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

import static java.util.stream.Collectors.toList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.AttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.bundled.BooleanStringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.bundled.ByteArrayStringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeConvertingVisitor;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link String} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string.
 *
 * <p>
 * This supports reading any DynamoDB attribute type into a string type, so it is very useful for logging information stored in
 * DynamoDB.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class StringAttributeConverter implements AttributeConverter<String> {
    public static StringAttributeConverter create() {
        return new StringAttributeConverter();
    }

    @Override
    public TypeToken<String> type() {
        return TypeToken.of(String.class);
    }

    @Override
    public AttributeValue transformFrom(String input) {
        return ItemAttributeValue.fromString(input).toGeneratedAttributeValue();
    }

    @Override
    public String transformTo(AttributeValue input) {
        return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(Visitor.INSTANCE);
    }

    private static final class Visitor extends TypeConvertingVisitor<String> {
        private static final Visitor INSTANCE = new Visitor();

        private Visitor() {
            super(String.class, StringAttributeConverter.class);
        }

        @Override
        public String convertString(String value) {
            return value;
        }

        @Override
        public String convertNumber(String value) {
            return value;
        }

        @Override
        public String convertBytes(SdkBytes value) {
            return ByteArrayStringConverter.create().toString(value.asByteArray());
        }

        @Override
        public String convertBoolean(Boolean value) {
            return BooleanStringConverter.create().toString(value);
        }

        @Override
        public String convertSetOfStrings(List<String> value) {
            return value.toString();
        }

        @Override
        public String convertSetOfNumbers(List<String> value) {
            return value.toString();
        }

        @Override
        public String convertSetOfBytes(List<SdkBytes> value) {
            return value.stream()
                        .map(this::convertBytes)
                        .collect(Collectors.joining(",", "[", "]"));
        }

        @Override
        public String convertMap(Map<String, ItemAttributeValue> value) {
            BinaryOperator<Object> throwingMerger = (l, r) -> {
                // Should not happen: we're converting from map.
                throw new IllegalStateException();
            };

            return value.entrySet().stream()
                        .collect(Collectors.toMap(i -> i.getKey(), i -> convert(i.getValue()),
                                                  throwingMerger, LinkedHashMap::new))
                        .toString();
        }

        @Override
        public String convertListOfAttributeValues(List<ItemAttributeValue> value) {
            return value.stream()
                        .map(this::convert)
                        .collect(toList())
                        .toString();
        }
    }
}

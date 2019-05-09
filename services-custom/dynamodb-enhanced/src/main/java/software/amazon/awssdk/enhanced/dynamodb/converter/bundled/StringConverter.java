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

package software.amazon.awssdk.enhanced.dynamodb.converter.bundled;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ExactInstanceOfConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * A converter between {@link String} and {@link ItemAttributeValue}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class StringConverter extends ExactInstanceOfConverter<String> {
    public StringConverter() {
        super(String.class);
    }

    @Override
    protected ItemAttributeValue convertToAttributeValue(String input, ConversionContext context) {
        return ItemAttributeValue.fromString(input);
    }

    @Override
    protected String convertFromAttributeValue(ItemAttributeValue input, TypeToken<?> desiredType, ConversionContext context) {
        return input.convert(Visitor.INSTANCE);
    }

    private static final class Visitor extends TypeConvertingVisitor<String> {
        private static final Visitor INSTANCE = new Visitor();

        private Visitor() {
            super(String.class, StringConverter.class);
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
            return "0x" + BinaryUtils.toHex(value.asByteArray());
        }

        @Override
        public String convertBoolean(Boolean value) {
            return value.toString();
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
        public String convertListOfAttributeValues(Collection<ItemAttributeValue> value) {
            return value.stream()
                        .map(this::convert)
                        .collect(toList())
                        .toString();
        }
    }
}
